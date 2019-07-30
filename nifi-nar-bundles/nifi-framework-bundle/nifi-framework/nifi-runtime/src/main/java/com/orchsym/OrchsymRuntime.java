package com.orchsym;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.apache.nifi.NiFi;
import org.apache.nifi.util.NiFiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orchsym.branding.BrandingExtension;
import com.orchsym.util.OrchsymProperties;

/**
 * 
 * @author GU Guoqiang
 *
 */
public class OrchsymRuntime extends NiFi {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrchsymRuntime.class);

    public OrchsymRuntime(NiFiProperties properties)
            throws ClassNotFoundException, IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        super(properties);
    }

    protected static void setCompatibleProperties() {
        setNifiProperty(OrchsymProperties.PROPERTIES_FILE_PATH, NiFiProperties.PROPERTIES_FILE_PATH);
        setNifiProperty(OrchsymProperties.BOOTSTRAP_LISTEN_PORT, BOOTSTRAP_PORT_PROPERTY);
        setNifiProperty(OrchsymProperties.BOOTSTRAP_LOG_DIR, OrchsymProperties.NIFI_BOOTSTRAP_LOG_DIR);
        setNifiProperty(OrchsymProperties.APP, "app");
    }

    protected static void setNifiProperty(String key, String nifiKey) {
        String propPath = System.getProperty(key);
        if (propPath != null && !propPath.isEmpty()) {
            System.setProperty(nifiKey, propPath);
        }
    }

    protected static NiFiProperties loadSettings(String[] args) {
        try {
            setCompatibleProperties();

            final String runtimeName = BrandingExtension.get().getRuntimeName();
            LOGGER.info("Launching " + runtimeName + "...");

            NiFiProperties properties = convertArgumentsToValidatedNiFiProperties(args);
            return properties;
        } catch (final Throwable t) {
            LOGGER.error("Failure to launch due to " + t, t);
            return null;
        }
    }

    public static void main(String[] args) {
        try {

            NiFiProperties properties = loadSettings(args);
            if (properties != null)
                new OrchsymRuntime(properties);
        } catch (final Throwable t) {
            LOGGER.error("Failure to launch due to " + t, t);
        }

    }
}
