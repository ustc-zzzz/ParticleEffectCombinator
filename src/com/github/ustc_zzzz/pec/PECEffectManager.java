package com.github.ustc_zzzz.pec;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector3d;
import com.github.ustc_zzzz.pec.util.*;
import com.github.ustc_zzzz.pec.util.repackage.net.objecthunter.exp4j.Expression;
import com.github.ustc_zzzz.pec.util.repackage.net.objecthunter.exp4j.ExpressionBuilder;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.asset.AssetManager;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.effect.particle.ParticleType;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
public class PECEffectManager
{
    private static final Pattern PATTERN = Pattern.compile("^[a-zA-z_]\\w*$");

    private final Logger logger;
    private final ParticleEffectCombinator plugin;
    private final PECTranslation translation;
    private final List<String> menuDirs = new ArrayList<>();

    private final Map<String, Element> elementMap = new LinkedHashMap<>();

    public PECEffectManager(ParticleEffectCombinator plugin)
    {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        translation = plugin.getTranslation();
    }

    public void loadConfig(CommentedConfigurationNode node) throws IOException
    {
        try
        {
            this.menuDirs.clear();
            this.elementMap.clear();
            Path configDir = this.plugin.getConfigDir();
            this.menuDirs.addAll(node.getList(TypeToken.of(String.class), this::releaseExample));
            this.menuDirs.stream().map(configDir::resolve).forEach(p -> this.elementMap.putAll(this.scan(p.toFile())));
        }
        catch (ObjectMappingException e)
        {
            throw new IOException(e);
        }
    }

    public void saveConfig(CommentedConfigurationNode node) throws IOException
    {
        Text def = this.translation.take("pec.config.scanDir.comment");
        node.setValue(this.menuDirs).setComment(node.getComment().orElse(def.toPlain()));
    }

    public List<String> releaseExample()
    {
        String defaultElementDir = "effects/";
        File elementDir = this.plugin.getConfigDir().resolve(defaultElementDir).toFile();
        if (elementDir.isDirectory() || elementDir.mkdirs())
        {
            try
            {
                for (String fileName : Arrays.asList("sphere", "cylinder", "combination", "circle-theorem"))
                {
                    String path = "examples/" + fileName + ".conf";
                    AssetManager assetManager = Sponge.getAssetManager();
                    Optional<Asset> example = assetManager.getAsset(this.plugin, path);
                    example.orElseThrow(IOException::new).copyToDirectory(elementDir.toPath());
                }
            }
            catch (IOException e)
            {
                this.logger.warn("Cannot extract default configurations", e);
            }
        }
        return Collections.singletonList(defaultElementDir);
    }

    public Map<String, Element> getElements()
    {
        return Collections.unmodifiableMap(this.elementMap);
    }

    private Map<String, Element> scan(File file)
    {
        Map<String, Element> newElements = new LinkedHashMap<>();
        if (file.isDirectory() || file.mkdirs())
        {
            for (File f : Optional.ofNullable(file.listFiles()).orElse(new File[0]))
            {
                String fileName = f.getName();
                if (fileName.endsWith(".conf"))
                {
                    String particleEffectRealName = fileName.substring(0, fileName.lastIndexOf(".conf"));
                    try
                    {
                        HoconConfigurationLoader loader = HoconConfigurationLoader.builder().setFile(f).build();
                        CommentedConfigurationNode root = loader.load().getNode(ParticleEffectCombinator.PLUGIN_ID);
                        newElements.put(particleEffectRealName, Objects.requireNonNull(this.deserializeElement(root)));
                    }
                    catch (Exception e)
                    {
                        this.logger.warn("Find error when reading a file (" + f.getAbsolutePath() +
                                "). Don't worry, we will skip this one and continue to read others", e);
                    }
                }
            }
        }
        return newElements;
    }

    private Element deserializeElement(ConfigurationNode node) throws InvalidDataException
    {
        return this.deserializeElement(node, new LinkedList<>());
    }

    private Element deserializeElement(ConfigurationNode node, LinkedList<Map<String, Double>> constants) throws InvalidDataException
    {
        if (node.hasListChildren())
        {
            Function<ConfigurationNode, Element> function = n -> this.deserializeElement(n, constants);
            return new ElementConcatenation(node.getChildrenList().stream().map(function).collect(Collectors.toList()));
        }
        if (node.hasMapChildren())
        {
            String type = node.getNode("type").getString("");
            Map<String, Double> constantsFrame = new HashMap<>();
            constants.addLast(constantsFrame);
            try
            {
                Map<Object, ? extends ConfigurationNode> constantMap = node.getNode("constant").getChildrenMap();
                for (Map.Entry<Object, ? extends ConfigurationNode> entry : constantMap.entrySet())
                {
                    String key = entry.getKey().toString(), value = entry.getValue().getString("").replace('-', '_');
                    if (!PATTERN.matcher(key).find()) throw new ObjectMappingException("Invalid constant name: " + key);
                    constantsFrame.put(key, this.evaluate(constants, value));
                }
                switch (type)
                {
                case "particle":
                {
                    List<String> def = Arrays.asList("0", "0", "0");
                    Element.Particle particle = new Element.Particle();

                    particle.effectType = node.getNode("particle-type").getValue(TypeToken.of(ParticleType.class));
                    List<String> velocityList = node.getNode("velocity").getList(TypeToken.of(String.class), def);

                    Vector3d velocity = this.evaluate(constants, velocityList);
                    particle.orientation = GenericMath.normalizeSafe(velocity).toFloat();
                    particle.speed = (float) velocity.length();

                    List<String> offsetList = node.getNode("offset").getList(TypeToken.of(String.class), def);
                    particle.offset = this.evaluate(constants, offsetList).toFloat();

                    return particle.asElement();
                }
                case "line":
                {
                    List<String> fromList = node.getNode("from").getList(TypeToken.of(String.class));
                    Vector3d from = this.evaluate(constants, fromList);

                    List<String> toList = node.getNode("to").getList(TypeToken.of(String.class));
                    Vector3d to = this.evaluate(constants, toList);

                    double rate = this.evaluate(constants, node.getNode("sampling-rate").getString("4"));
                    double phase = this.evaluate(constants, node.getNode("sampling-phase").getString("0"));
                    Element child = this.deserializeElement(node.getNode("child"), constants);
                    return new ElementLine(from, to, child, (float) rate, (float) phase);
                }
                case "circle":
                {
                    float radius = (float) this.evaluate(constants, node.getNode("radius").getString("0"));
                    if (radius <= 0)
                    {
                        throw new ObjectMappingException("Radius should be positive number, while it is: " + radius);
                    }
                    double rate = this.evaluate(constants, node.getNode("sampling-rate").getString("4"));
                    Element child = this.deserializeElement(node.getNode("child"), constants);
                    return new ElementCircle(child, radius, (float) rate);
                }
                case "sphere":
                {
                    float radius = (float) this.evaluate(constants, node.getNode("radius").getString("0"));
                    if (radius <= 0)
                    {
                        throw new ObjectMappingException("Radius should be positive number, while it is: " + radius);
                    }
                    float sampleNumber = node.getNode("sampling-rate").getFloat(4);
                    Element child = this.deserializeElement(node.getNode("child"), constants);
                    return new ElementSphere(child, radius, sampleNumber);
                }
                case "rotation":
                {
                    List<String> def = Arrays.asList("0", "1", "0");
                    double rotation = this.evaluate(constants, node.getNode("rotation-degree").getString("0"));
                    List<String> axisList = node.getNode("rotation-axis").getList(TypeToken.of(String.class), def);
                    Element child = this.deserializeElement(node.getNode("child"), constants);
                    Vector3d rotationAxis = this.evaluate(constants, axisList);
                    return new ElementRotation(child, rotationAxis, rotation);
                }
                case "offset":
                {
                    List<String> def = Arrays.asList("0", "1", "0");
                    Multimap<Float, Double> offsetMultimap = Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);
                    if (node.getNode("interpolation").hasMapChildren())
                    {
                        Map<Object, ? extends ConfigurationNode> map = node.getNode("interpolation").getChildrenMap();
                        for (Map.Entry<Object, ? extends ConfigurationNode> entry : map.entrySet())
                        {
                            float key = (float) this.evaluate(constants, entry.getKey().toString());
                            double value = this.evaluate(constants, entry.getValue().getString(""));
                            offsetMultimap.put(key, value);
                        }
                    }
                    else
                    {
                        double value = this.evaluate(constants, node.getNode("interpolation").getString(""));
                        offsetMultimap.put(0F, value);
                    }
                    // noinspection ConstantConditions
                    ToDoubleFunction<Collection<Double>> f = v -> v.stream().mapToDouble(d -> d).average().getAsDouble();
                    Map<Float, Double> offsetMap = Maps.transformValues(offsetMultimap.asMap(), f::applyAsDouble);
                    List<String> axisList = node.getNode("orientation").getList(TypeToken.of(String.class), def);
                    Element child = this.deserializeElement(node.getNode("child"), constants);
                    Vector3d orientation = this.evaluate(constants, axisList);
                    return new ElementOffset(child, orientation, offsetMap);
                }
                case "time-offset":
                {
                    Multimap<Float, Double> offsetMultimap = Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);
                    if (node.getNode("interpolation").hasMapChildren())
                    {
                        Map<Object, ? extends ConfigurationNode> map = node.getNode("interpolation").getChildrenMap();
                        for (Map.Entry<Object, ? extends ConfigurationNode> entry : map.entrySet())
                        {
                            float key = (float) this.evaluate(constants, entry.getKey().toString());
                            double value = this.evaluate(constants, entry.getValue().getString(""));
                            offsetMultimap.put(key, value);
                        }
                    }
                    else
                    {
                        double value = this.evaluate(constants, node.getNode("interpolation").getString(""));
                        offsetMultimap.put(0F, value);
                    }
                    // noinspection ConstantConditions
                    ToDoubleFunction<Collection<Double>> f = v -> v.stream().mapToDouble(d -> d).average().getAsDouble();
                    Map<Float, Double> offsetMap = Maps.transformValues(offsetMultimap.asMap(), f::applyAsDouble);
                    Element child = this.deserializeElement(node.getNode("child"), constants);
                    return new ElementTimeOffset(child, offsetMap);
                }
                /* case "offset-expression": */ // TODO
                default:
                    throw new ObjectMappingException("Unrecognized type: " + type);
                }
            }
            catch (Exception exception)
            {
                throw new InvalidDataException("Find error when parsing fields for type:" + type, exception);
            }
            finally
            {
                constants.removeLast();
            }
        }
        String reference = node.getString();
        return Objects.isNull(reference) ? new Element.Particle().asElement() : (interpolation, random) ->
        {
            Element e = this.elementMap.get(reference);
            if (Objects.nonNull(e)) return e.getParticles(interpolation, random);
            Text errorMessage = this.translation.take("pec.display.invalidReference", reference);
            throw new IllegalStateException(errorMessage.toPlain());
        };
    }

    private Vector3d evaluate(List<Map<String, Double>> constants, List<String> values) throws ObjectMappingException
    {
        if (values.size() != 3) throw new ObjectMappingException("Invalid list size:" + values.size());
        double x = this.evaluate(constants, values.get(0));
        double y = this.evaluate(constants, values.get(1));
        double z = this.evaluate(constants, values.get(2));
        return Vector3d.from(x, y, z);
    }

    private double evaluate(List<Map<String, Double>> constants, String value) throws ObjectMappingException
    {
        try
        {
            ExpressionBuilder builder = new ExpressionBuilder(value);
            constants.forEach(m -> builder.variables(m.keySet()));
            Expression expression = builder.build();

            constants.forEach(expression::setVariables);
            return expression.evaluate();
        }
        catch (Exception exception)
        {
            String message = "Find error when evaluating \"" + value + "\", constants: " + constants;
            throw new ObjectMappingException(message, exception);
        }
    }
}
