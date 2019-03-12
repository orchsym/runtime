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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.nifi.bundle.Bundle;
import org.apache.nifi.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scans through the classpath to load all i18n resources using the service provider API and running through all classloaders (root, NARs).
 *
 */
class PropertiesExtensionManager {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesExtensionManager.class);
    private static final Object SYN = new Object();
    private static final String UL = "_"; // underline

    static final Map<TypeLocale, PropertyBundle> typeBundleLookup = new HashMap<>();
    static final Map<TypeLocale, ResourceBundle> resBundleLookup = new HashMap<>();
    static final List<URL> foundURLs = new ArrayList<>();

    static void discoverExtensions(final Set<Bundle> narBundles) {
        // get the current context class loader
        ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();

        // consider each nar class loader
        for (final Bundle bundle : narBundles) {
            // Must set the context class loader to the nar classloader itself
            // so that static initialization techniques that depend on the context class loader will work properly
            final ClassLoader ncl = bundle.getClassLoader();
            Thread.currentThread().setContextClassLoader(ncl);

            PropertiesServiceLoader.load(bundle.getClassLoader()).forEach(url -> cachePropertyURL(bundle, url));

        }

        // restore the current context class loader if appropriate
        if (currentContextClassLoader != null) {
            Thread.currentThread().setContextClassLoader(currentContextClassLoader);
        }
    }

    private static void cachePropertyURL(final Bundle bundle, final URL url) {
        if (foundURLs.contains(url)) {
            return;
        }
        List<TypeLocale> tls = createTypeLocales(url);
        if (tls != null && !tls.isEmpty()) {
            foundURLs.add(url);
            for (TypeLocale tl : tls) {
                if (typeBundleLookup.containsKey(tl)) {
                    final PropertyBundle oldTL = typeBundleLookup.get(tl);
                    logger.warn("Have existed the properties: " + oldTL.url + " in " + oldTL.bundle.getBundleDetails());
                } else {
                    typeBundleLookup.put(tl, new PropertyBundle(url, bundle));
                }
            }
        }
    }

    /**
     * in order to compatible for components with versioning, like XXX_0_11_en, also support the lang with country, like XXX_0_11_zh_CN
     */
    static List<TypeLocale> createTypeLocales(final URL url) {
        final String fullPath = url.getFile();
        if (!fullPath.endsWith(MessagesProvider.EXT)) {
            return Collections.emptyList();
        }
        int pathIndex = fullPath.lastIndexOf('/');
        if (-1 == pathIndex) {
            pathIndex = fullPath.lastIndexOf('\\');
        }
        String fullname = fullPath;
        if (-1 != pathIndex) { // remove path
            fullname = fullPath.substring(pathIndex + 1);
        }
        String name = fullname.substring(0, fullname.lastIndexOf(MessagesProvider.EXT));

        if (name.contains(UL)) {

            int _first = name.indexOf(UL);
            int _last = name.lastIndexOf(UL);

            if (Objects.equals(_first, _last)) { // only one lang, like XXX_zh
                String type = name.substring(0, _first);
                String lang = name.substring(_last + 1);
                if (!lang.isEmpty()) {
                    return Arrays.asList(new TypeLocale(type, new Locale(lang, "")));
                }
            } else { // more underline
                List<TypeLocale> tls = new ArrayList<>(2);

                // like for versioning, XXX_v10_en or XXX_0_11_en
                String type = name.substring(0, _last);
                String lang = name.substring(_last + 1);
                tls.add(new TypeLocale(type, new Locale(lang, "")));

                // FIXME, let it after versioning, because don't provide it with country normally.
                String part = name.substring(_first + 1, _last);
                int _partLast = part.lastIndexOf('_');

                String country = name.substring(_last + 1);
                if (-1 == _partLast) { // only 2
                    // like XXX_en_US
                    type = name.substring(0, _first);
                    lang = part;

                } else { // >2
                    // like XXX_0_11_en_US
                    type = name.substring(0, _first) + UL + part.substring(0, _partLast);
                    lang = part.substring(_partLast + 1);
                }
                tls.add(new TypeLocale(type, new Locale(lang, country)));
                return tls;

            }

        } else {
            // return Arrays.asList(new TypeLocale(name, Locale.ROOT));
        }
        return Collections.emptyList();

    }

    static class PropertyBundle {
        URL url;
        Bundle bundle;

        PropertyBundle(URL url, Bundle bundle) {
            this.url = url;
            this.bundle = bundle;
        }
    }

    static class TypeLocale {
        String type;
        Locale locale;

        TypeLocale(String type, Locale locale) {
            this.type = type;
            this.locale = locale;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((locale == null) ? 0 : locale.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            TypeLocale other = (TypeLocale) obj;
            if (locale == null) {
                if (other.locale != null)
                    return false;
            } else if (!locale.equals(other.locale))
                return false;
            if (type == null) {
                if (other.type != null)
                    return false;
            } else if (!type.equals(other.type))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return type + '_' + locale;
        }

    }

    private static ResourceBundle getResourceBundle(TypeLocale tl) {
        if (resBundleLookup.containsKey(tl)) {
            return resBundleLookup.get(tl);
        }

        synchronized (SYN) {
            PropertyBundle propertyBundle = typeBundleLookup.get(tl);
            if (propertyBundle != null) {
                try (final InputStream is = propertyBundle.url.openStream()) {
                    // final ClassLoader classLoader = propertyBundle.bundle.getClassLoader();

                    PropertyResourceBundle resBundle = new PropertyResourceBundle(is);
                    resBundleLookup.put(tl, resBundle);
                    return resBundle;
                    // ResourceBundle resBundle = ResourceBundle.getBundle(tl.type, tl.locale, classLoader);
                } catch (MissingResourceException | IOException e) {
                    // ignore, try another one
                }
            }
        }
        return null;
    }

    /**
     * get the value from properties which is based on the locale.
     */
    static String getValue(final Locale locale, final String type, final String key) {
        if (locale == null) {
            return null; // if no locale, or no special one, will use original value, no i18n
        }
        return getValue(new TypeLocale(type, locale), key);
    }

    private static String getValue(TypeLocale tl, String key) {
        final ResourceBundle resourceBundle = getResourceBundle(tl);
        if (resourceBundle != null) {
            try {
                final String value = resourceBundle.getString(key);
                if (!StringUtils.isBlank(value)) {
                    return value;
                }
            } catch (MissingResourceException e) {
                // ignore
            }
        }
        final Locale locale = tl.locale;
        // if has country, try again language only
        if (locale != null && !StringUtils.isEmpty(locale.getCountry())) {
            return getValue(new TypeLocale(tl.type, new Locale(locale.getLanguage())), key);
        }
        return null;
    }

}
