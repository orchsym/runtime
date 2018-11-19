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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /*
     * match a.b.c.X_zh_CN.properties
     */
    private static final Pattern PROP_PATTERN = Pattern.compile("([\\w.-]+?)_(\\w+?)(_(\\w+?))?\\.\\w+$", Pattern.CASE_INSENSITIVE);

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
        TypeLocale tl = createTypeLocale(url);
        if (tl != null) {
            foundURLs.add(url);

            if (typeBundleLookup.containsKey(tl)) {
                final PropertyBundle oldTL = typeBundleLookup.get(tl);
                logger.error("Have existed the properties: " + oldTL.url + " in " + oldTL.bundle.getBundleDetails());
            } else {
                typeBundleLookup.put(tl, new PropertyBundle(url, bundle));
            }
        }
    }

    static TypeLocale createTypeLocale(final URL url) {
        final String fullname = url.getFile();
        if (!fullname.endsWith(MessagesProvider.EXT)) {
            return null;
        }
        final Matcher matcher = PROP_PATTERN.matcher(fullname);
        if (matcher.find() && matcher.groupCount() > 1) {
            final String type = matcher.group(1);
            final String lang = matcher.group(2);
            String country = null;
            if (matcher.groupCount() > 2) {
                country = matcher.group(4);
            }
            if (StringUtils.isEmpty(country)) {
                country = "";
            }
            return new TypeLocale(type, new Locale(lang, country));
        }
        return null;
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
