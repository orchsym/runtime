/*
 * Licensed to the Orchsym Runtime under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * 
 * this file to You under the Orchsym License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * https://github.com/orchsym/runtime/blob/master/orchsym/LICENSE
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
