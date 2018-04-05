package com.github.ustc_zzzz.pec;

import com.google.common.base.Charsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.plugin.meta.version.ComparableVersion;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * @author ustc_zzzz
 */
@Plugin(authors = "ustc_zzzz", dependencies = @Dependency(id = "spongeapi"),
        id = ParticleEffectCombinator.PLUGIN_ID, name = ParticleEffectCombinator.NAME,
        description = ParticleEffectCombinator.DESC, version = ParticleEffectCombinator.VERSION)
public class ParticleEffectCombinator
{
    static final String PLUGIN_ID = "pec";
    static final String VERSION = "@version@";
    static final String NAME = "ParticleEffectCombinator";
    static final String DESC = "A plugin for generating complex particle effects based on a series of basic elements";

    public static final String GITHUB_URL = "https://github.com/ustc-zzzz/ParticleEffectCombinator";
    public static final String WEBSITE_URL = "https://ore.spongepowered.org/zzzz/ParticleEffectCombinator";
    public static final String API_URL = "https://api.github.com/repos/ustc-zzzz/ParticleEffectCombinator/releases";

    public static final SimpleDateFormat RFC3339 = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    public static final SimpleDateFormat ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH);

    @Inject
    private Logger logger;

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDir;

    @Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> config;

    private CommentedConfigurationNode rootConfigurationNode;

    private PECTranslation translation;

    private PECCommandDispatcher commands;

    private PECEffectManager effectManager;

    private boolean doCheckUpdate = true;

    private void checkUpdate()
    {
        try
        {
            URL url = new URL(API_URL);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.getResponseCode();
            InputStreamReader reader = new InputStreamReader(connection.getInputStream(), Charsets.UTF_8);
            JsonObject jsonObject = new JsonParser().parse(reader).getAsJsonArray().get(0).getAsJsonObject();
            String version = jsonObject.get("tag_name").getAsString();
            if (version.startsWith("v"))
            {
                version = version.substring(1);
                String releaseName = jsonObject.get("name").getAsString();
                String releaseUrl = jsonObject.get("html_url").getAsString();
                String releaseDate = RFC3339.format(ISO8601.parse(jsonObject.get("published_at").getAsString()));
                if (new ComparableVersion(version).compareTo(new ComparableVersion(VERSION)) > 0)
                {
                    this.logger.info("================================================================");
                    this.logger.warn("   #   # ##### #   #      #   # ####  ####    #   ##### #####   ");
                    this.logger.warn("   #   # #     # # #      #   # #   #  #  #  # #    #   #       ");
                    this.logger.warn("   ##  # #     # # #      #   # #   #  #  # #   #   #   #       ");
                    this.logger.warn("   # # # ##### # # #      #   # ####   #  # #   #   #   #####   ");
                    this.logger.warn("   #  ## #     ## ##      #   # #      #  # #####   #   #       ");
                    this.logger.warn("   #   # #     #   #      #   # #      #  # #   #   #   #       ");
                    this.logger.warn("   #   # ##### #   #       ###  #     ####  #   #   #   #####   ");
                    this.logger.warn("================================================================");
                    this.logger.warn("An update was found: " + releaseName);
                    this.logger.warn("This new update was released at: " + releaseDate);
                    this.logger.warn("You can get the latest version at: " + releaseUrl);
                    this.logger.info("================================================================");
                }
            }
        }
        catch (Exception e)
        {
            // <strike>do not bother offline users</strike> maybe bothering them is a better choice
            this.logger.warn("Failed to check update", e);
        }
    }

    private void loadConfig() throws IOException
    {
        CommentedConfigurationNode root = this.config.load();

        this.doCheckUpdate = root.getNode(PLUGIN_ID, "check-update").getBoolean(true);
        this.effectManager.loadConfig(root.getNode(PLUGIN_ID, "scan-dirs"));

        this.rootConfigurationNode = root;
    }

    private void saveConfig() throws IOException
    {
        CommentedConfigurationNode root = this.rootConfigurationNode;
        if (Objects.isNull(root)) root = this.config.createEmptyNode();

        root.getNode(PLUGIN_ID, "check-update").setValue(this.doCheckUpdate);
        this.effectManager.saveConfig(root.getNode(PLUGIN_ID, "scan-dirs"));

        this.config.save(root);
    }

    @Listener
    public void on(GameInitializationEvent event)
    {
        this.translation = new PECTranslation(this);
        this.commands = new PECCommandDispatcher(this);
        this.effectManager = new PECEffectManager(this);
    }

    @Listener
    public void on(GameStartedServerEvent event)
    {
        try
        {
            this.translation.info("pec.config.load.start");
            this.loadConfig();
            if (this.doCheckUpdate) new Thread(this::checkUpdate).start();
            this.saveConfig();
            this.translation.info("pec.config.load.finish");
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Listener
    public void on(GameReloadEvent event)
    {
        try
        {
            this.reload(event.getCause().first(CommandSource.class).orElse(Sponge.getServer().getConsole()));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void reload(MessageReceiver receiver) throws IOException
    {
        receiver.sendMessage(this.translation.take("pec.reload.start"));
        this.loadConfig();
        this.saveConfig();
        receiver.sendMessage(this.translation.take("pec.reload.finish"));
    }

    public Logger getLogger()
    {
        return this.logger;
    }

    public Path getConfigDir()
    {
        return this.configDir;
    }

    public PECTranslation getTranslation()
    {
        return this.translation;
    }

    public PECEffectManager getElementManager()
    {
        return this.effectManager;
    }
}
