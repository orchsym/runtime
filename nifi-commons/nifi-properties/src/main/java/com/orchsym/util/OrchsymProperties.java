package com.orchsym.util;

import java.io.File;

/**
 * @author GU Guoqiang
 *
 */
public class OrchsymProperties {
    public static final String FILE_NAME = "orchsym.properties";

    public static final String CONF_DIR = "./conf";

    /**
     * keys
     */
    public static final String PROPERTIES_FILE_PATH = "orchsym.properties.file.path";
    public static final String BOOTSTRAP_LISTEN_PORT = "orchsym.bootstrap.listen.port";
    public static final String BOOTSTRAP_LOG_DIR = "orchsym.bootstrap.config.log.dir";
    public static final String NIFI_BOOTSTRAP_LOG_DIR = "org.apache.nifi.bootstrap.config.log.dir";
    public static final String LIC_PATH = "orchsym.lic.path";
    public static final String APP = "orchsym.app";

    public static File getConfDir() {
        String configFilename = System.getProperty("org.apache.nifi.bootstrap.config.file"); // same as RunOrchsymRuntime.getDefaultBootstrapConfFile

        final String propPath = System.getProperty(PROPERTIES_FILE_PATH, configFilename);

        final File defaultConfFile = new File(CONF_DIR);

        File confFile = defaultConfFile;
        if (propPath != null && propPath.trim().isEmpty() && new File(propPath).exists()) {
            confFile = new File(propPath).getParentFile();
        }
        if (confFile.exists()) {
            return confFile;
        }

        return defaultConfFile;
    }

}
