package com.github.ustc_zzzz.particlecombinator;

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
@Plugin(id = ParticleCombinator.PLUGIN_ID, name = "ParticleCombinator", version = ParticleCombinator.VERSION,
        authors = "ustc_zzzz", dependencies = @Dependency(id = "spongeapi"), description = ParticleCombinator.DESC)
public class ParticleCombinator
{
    static final String VERSION = "@version@";
    static final String PLUGIN_ID = "particlecombinator";
    static final String DESC = "A plugin for generating complex particle effects based on a series of basic elements";

    public static final String GITHUB_URL = "https://github.com/ustc-zzzz/ParticleCombinator";
    public static final String WEBSITE_URL = "https://ore.spongepowered.org/zzzz/ParticleCombinator";
    public static final String API_URL = "https://api.github.com/repos/ustc-zzzz/ParticleCombinator/releases";

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

    private ParticleCombinatorTranslation translation;

    private ParticleCombinatorCommands commands;

    private ParticleCombinatorManager manager;

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
        this.manager.loadConfig(root.getNode(PLUGIN_ID, "scan-dirs"));

        this.rootConfigurationNode = root;
    }

    private void saveConfig() throws IOException
    {
        CommentedConfigurationNode root = this.rootConfigurationNode;
        if (Objects.isNull(root)) root = this.config.createEmptyNode();

        root.getNode(PLUGIN_ID, "check-update").setValue(this.doCheckUpdate);
        this.manager.saveConfig(root.getNode(PLUGIN_ID, "scan-dirs"));

        this.config.save(root);
    }

    @Listener
    public void on(GameInitializationEvent event)
    {
        this.translation = new ParticleCombinatorTranslation(this);
        this.commands = new ParticleCombinatorCommands(this);
        this.manager = new ParticleCombinatorManager(this);
    }

    @Listener
    public void on(GameStartedServerEvent event)
    {
        try
        {
            this.translation.info("particlecombinator.config.load.start");
            this.loadConfig();
            if (this.doCheckUpdate) new Thread(this::checkUpdate).start();
            this.saveConfig();
            this.translation.info("particlecombinator.config.load.finish");
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
        receiver.sendMessage(this.translation.take("particlecombinator.reload.start"));
        this.loadConfig();
        this.saveConfig();
        receiver.sendMessage(this.translation.take("particlecombinator.reload.finish"));
    }

    public Logger getLogger()
    {
        return this.logger;
    }

    public Path getConfigDir()
    {
        return this.configDir;
    }

    public ParticleCombinatorManager getElementManager()
    {
        return this.manager;
    }

    public ParticleCombinatorTranslation getTranslation()
    {
        return this.translation;
    }
}
