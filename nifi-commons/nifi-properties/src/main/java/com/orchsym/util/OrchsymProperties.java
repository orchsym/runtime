/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
