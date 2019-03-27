package com.github.ustc_zzzz.pec;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import com.github.ustc_zzzz.pec.util.Element;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.*;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
public class PECCommandDispatcher implements Supplier<CommandCallable>
{
    private static final String SUBTITLE = ParticleEffectCombinator.DESC;
    private static final String VERSION = ParticleEffectCombinator.VERSION;
    private static final String GITHUB_URL = ParticleEffectCombinator.GITHUB_URL;
    private static final String WEBSITE_URL = ParticleEffectCombinator.WEBSITE_URL;
    private static final String PEC_COMMAND_NAME = ParticleEffectCombinator.PLUGIN_ID; // pec

    private final ParticleEffectCombinator plugin;

    private final CommandCallable listCommand;
    private final CommandCallable reloadCommand;
    private final CommandCallable displayCommand;
    private final CommandCallable versionCommand;

    private final PECTranslation translation;

    private final Random commandRandom = new Random();

    public PECCommandDispatcher(ParticleEffectCombinator plugin)
    {
        this.plugin = plugin;
        this.translation = plugin.getTranslation();

        this.displayCommand = CommandSpec.builder()
                .arguments(GenericArguments.seq(
                        GenericArguments.choices(Text.of("effect-name"), this::getEffectNames, Function.identity()),
                        GenericArguments.firstParsing(
                                GenericArguments.seq(
                                        GenericArguments.literal(Text.of("at"), "at"),
                                        GenericArguments.location(Text.of("at-location")),
                                        GenericArguments.optional(GenericArguments.seq(
                                                GenericArguments.literal(Text.of("for"), "for"),
                                                GenericArguments.player(Text.of("for-players"))))),
                                GenericArguments.seq(
                                        GenericArguments.literal(Text.of("for"), "for"),
                                        GenericArguments.player(Text.of("for-players")),
                                        GenericArguments.optional(GenericArguments.seq(
                                                GenericArguments.literal(Text.of("at"), "at"),
                                                GenericArguments.location(Text.of("at-location"))))),
                                GenericArguments.none())))
                .executor(this::runDisplayCommand).build();

        this.listCommand = CommandSpec.builder()
                .arguments(GenericArguments.none())
                .executor(this::runListCommand).build();

        this.reloadCommand = CommandSpec.builder()
                .permission("pec.reload")
                .arguments(GenericArguments.optional(
                        GenericArguments.literal(Text.of("extract-examples"), "extract-examples")))
                .executor(this::runReloadCommand).build();

        this.versionCommand = CommandSpec.builder()
                .arguments(GenericArguments.none())
                .executor(this::runVersionCommand).build();

        Sponge.getCommandManager().register(plugin, this.get(), PEC_COMMAND_NAME);
    }

    private CommandResult runReloadCommand(CommandSource src, CommandContext args) throws CommandException
    {
        if (src.hasPermission("pec.reload"))
        {
            if (args.getOne("extract-examples").isPresent())
            {
                this.plugin.getElementManager().releaseExample();
            }
            try
            {
                this.plugin.reload(src);
            }
            catch (IOException e)
            {
                throw new CommandException(this.translation.take("pec.reload.errorWhenReloading"), e);
            }
            return CommandResult.success();
        }
        return CommandResult.empty();
    }

    private CommandResult runVersionCommand(CommandSource src, CommandContext args) throws CommandException
    {
        src.sendMessage(Text.of("================================================================"));
        src.sendMessage(this.translation.take("pec.version.description.title", VERSION));
        src.sendMessage(this.translation.take("pec.version.description.subtitle", SUBTITLE));
        src.sendMessage(Text.of("================================================================"));
        try
        {
            String gitCommitHash = "@git_hash@";
            // RFC 3339
            Date releaseDate = ParticleEffectCombinator.RFC3339.parse("@release_date@");

            src.sendMessage(this.translation
                    .take("pec.version.description.line1", releaseDate));
            src.sendMessage(this.translation
                    .take("pec.version.description.line2", gitCommitHash));

            Text urlWebsite = Text.builder(WEBSITE_URL)
                    .color(TextColors.GREEN).style(TextStyles.BOLD)
                    .onClick(TextActions.openUrl(new URL(WEBSITE_URL))).build();
            Text urlGitHub = Text.builder(GITHUB_URL)
                    .color(TextColors.GREEN).style(TextStyles.BOLD)
                    .onClick(TextActions.openUrl(new URL(GITHUB_URL))).build();

            src.sendMessage(Text.join(this.translation
                    .take("pec.version.description.line3", ""), urlWebsite));
            src.sendMessage(Text.join(this.translation
                    .take("pec.version.description.line4", ""), urlGitHub));

            src.sendMessage(Text.of("================================================================"));
        }
        catch (MalformedURLException | ParseException e)
        {
            throw new RuntimeException(e);
        }
        return CommandResult.success();
    }

    private CommandResult runListCommand(CommandSource src, CommandContext args) throws CommandException
    {
        if (!src.hasPermission("pec.list"))
        {
            Text error = this.translation.take("pec.list.noPermission", src.getName());
            throw new CommandPermissionException(error);
        }
        int size = 0;
        StringJoiner joiner = new StringJoiner(", ");
        for (String name : this.plugin.getElementManager().getElements().keySet())
        {
            if (src.hasPermission("pec.send." + name))
            {
                ++size;
                joiner.add(name);
            }
        }
        src.sendMessage(this.translation.take("pec.list.overview", size));
        src.sendMessage(Text.of(joiner.toString()));
        return CommandResult.success();
    }

    private CommandResult runDisplayCommand(CommandSource src, CommandContext args) throws CommandException
    {
        Optional<String> nameOptional = args.getOne(Text.of("effect-name"));

        Map<String, Element> elements = this.plugin.getElementManager().getElements();
        Optional<Element> element = nameOptional.flatMap(name -> Optional.ofNullable(elements.get(name)));

        if (!element.isPresent() || !src.hasPermission("pec.send." + nameOptional))
        {
            Text error = this.translation.take("pec.display.notExists", nameOptional);
            throw new CommandPermissionException(error);
        }

        Optional<Location<World>> locationOptional = args.getOne(Text.of("at-location"));
        Location<World> spawnLocation = locationOptional.orElse(((Locatable) src).getLocation());

        Collection<Player> players = args.getAll(Text.of("for-players"));
        if (!args.hasAny(Text.of("for")))
        {
            players = spawnLocation.getExtent().getPlayers();
        }

        this.displayParticles(nameOptional.get(), element.get(), spawnLocation, players.toArray(new Player[0]));

        return CommandResult.success();
    }

    private void displayParticles(String name, Element element, Location<World> spawnLocation, Player[] players)
    {
        try (Timing ignored = Timings.of(this.plugin, "displayEffect - " + name).startTiming())
        {
            Iterable<Element.Particle> particles = element.getParticles(0, this.commandRandom);
            particles.forEach(particle -> particle.spawn(spawnLocation, this.plugin, players));
        }
        catch (Exception e)
        {
            this.plugin.getLogger().error("Find error when displaying particle: " + name, e);
        }
    }

    private Collection<String> getEffectNames()
    {
        return this.plugin.getElementManager().getElements().keySet();
    }

    @Override
    public CommandCallable get()
    {
        return CommandSpec.builder()
                .child(this.listCommand, "list", "l")
                .child(this.reloadCommand, "reload", "r")
                .child(this.displayCommand, "display", "d")
                .child(this.versionCommand, "version", "v")
                .description(this.translation.take("pec.description")).build();
    }
}
