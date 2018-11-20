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
package org.apache.nifi.nar.i18n;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PropertiesServiceLoader implements Iterable<URL> {
    private static final Logger logger = LoggerFactory.getLogger(PropertiesServiceLoader.class);

    private final ClassLoader loader;

    private Iterator<URL> foundPropURLs = null;

    private PropertiesServiceLoader(ClassLoader cl) {
        loader = (cl == null) ? ClassLoader.getSystemClassLoader() : cl;
        reload();
    }

    /**
     * Clear this loader's provider cache so that all providers will be reloaded.
     *
     * <p>
     * After invoking this method, subsequent invocations of the {@link #iterator() iterator} method will lazily look up and instantiate providers from scratch, just as is done by a newly-created
     * loader.
     *
     * <p>
     * This method is intended for use in situations in which new providers can be installed into a running Java virtual machine.
     */
    public void reload() {
        try {
            if (loader == null) {
                return;
            }
            // try to find all i18n folder in jars
            Enumeration<URL> i18nURLs = loader.getResources(MessagesProvider.PATH_I18N);
            if (i18nURLs == null || !i18nURLs.hasMoreElements()) {
                return;
            }
            Set<URL> foundURLs = new HashSet<>();
            while (i18nURLs.hasMoreElements()) {
                final URL url = i18nURLs.nextElement();
                final String urlStr = url.toString();
                final int jarIndex = urlStr.indexOf("!/");
                if (jarIndex > 0) { // in jar
                    String jarPath = urlStr.substring(0, jarIndex + 2); // must with "!/"
                    foundURLs.addAll(getPropertiesURLs(jarPath));
                } else {// in dir
                    File i18nDir = new File(url.getPath());
                    if (i18nDir.exists()) {
                        foundURLs.addAll(getPropertiesURLs(i18nDir));
                    }
                }
            }
            foundPropURLs = foundURLs.iterator();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

    }

    /**
     * Get the properties files from jar.
     *
     */
    private List<URL> getPropertiesURLs(String jarPath) {
        List<URL> propURLs = new ArrayList<>();
        try {
            URL jarURL = new URL(jarPath);
            JarURLConnection jarCon = (JarURLConnection) jarURL.openConnection();
            JarFile jarFile = jarCon.getJarFile();
            Enumeration<JarEntry> jarEntrys = jarFile.entries();
            while (jarEntrys.hasMoreElements()) {
                JarEntry entry = jarEntrys.nextElement();
                String name = entry.getName();
                if (!entry.isDirectory() && name.startsWith(MessagesProvider.PATH_I18N) && name.endsWith(MessagesProvider.EXT)) {
                    final URL resource = loader.getResource(name); // try to find the first one
                    if (resource != null)
                        propURLs.add(resource);
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return propURLs;
    }

    /**
     * 
     * get the properties files from directory.
     */
    private List<URL> getPropertiesURLs(File dir) {
        List<URL> propURLs = new ArrayList<>();
        if (dir != null && dir.exists()) {
            final File[] subFiles = dir.listFiles();
            if (subFiles != null) {
                try {
                    for (File f : subFiles) {
                        if (f.isFile() && f.getName().endsWith(MessagesProvider.EXT)) {
                            propURLs.add(f.toURI().toURL());
                        } else if (f.isDirectory()) {
                            propURLs.addAll(getPropertiesURLs(f));
                        }
                    }
                } catch (MalformedURLException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return propURLs;
    }

    public Iterator<URL> iterator() {
        return new Iterator<URL>() {

            public boolean hasNext() {
                return foundPropURLs != null && foundPropURLs.hasNext();
            }

            public URL next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return foundPropURLs.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }

    public static PropertiesServiceLoader load(ClassLoader loader) {
        return new PropertiesServiceLoader(loader);
    }

}
