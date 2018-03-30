package com.github.ustc_zzzz.particlecombinator;

import com.google.common.base.Charsets;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.asset.AssetManager;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializer;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
public class ParticleCombinatorTranslation
{

    private final Logger logger;
    private final ResourceBundle resourceBundle;
    private final TextSerializer textSerializer = TextSerializers.FORMATTING_CODE;

    public ParticleCombinatorTranslation(ParticleCombinator plugin)
    {
        this.logger = plugin.getLogger();
        AssetManager manager = Sponge.getAssetManager();
        try
        {
            String other = "i18n/en_US.properties";
            String one = "i18n/" + Locale.getDefault().toString() + ".properties";
            InputStream inputStream = this.getAsset(plugin, manager, one, other).getUrl().openStream();
            this.resourceBundle = new PropertyResourceBundle(new InputStreamReader(inputStream, Charsets.UTF_8));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private Asset getAsset(ParticleCombinator p, AssetManager manager, String one, String other) throws IOException
    {
        String error = "Cannot find the default i18n file: i18n/en_US.properties";
        return manager.getAsset(p, one).orElse(manager.getAsset(p, other).orElseThrow(() -> new IOException(error)));
    }

    public void info(String key)
    {
        logger.info(getString(key, new Object[0]));
    }

    public void info(String key, Object... values)
    {
        logger.info(getString(key, values));
    }

    public Text take(String key)
    {
        return textSerializer.deserialize(getString(key, new Object[0]));
    }

    public Text take(String key, Object... values)
    {
        return textSerializer.deserialize(getString(key, values));
    }

    private String getString(String key, Object[] values)
    {
        try
        {
            return new MessageFormat(resourceBundle.getString(key)).format(values);
        }
        catch (MissingResourceException | ClassCastException | IllegalArgumentException e)
        {
            return key;
        }
    }
}
