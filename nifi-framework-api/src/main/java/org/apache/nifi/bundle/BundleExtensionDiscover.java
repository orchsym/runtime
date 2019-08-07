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
package org.apache.nifi.bundle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author GU Guoqiang
 *
 *
 *         TODO need take care the dynamic extension load in future version
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class BundleExtensionDiscover {

    private static final Logger logger = LoggerFactory.getLogger(BundleExtensionDiscover.class);

    private static final Map<BundleCoordinate, Bundle> bundleCoordinateBundleLookup = new LinkedHashMap<>();
    private static final Map<ClassLoader, Bundle> classLoaderBundleLookup = new LinkedHashMap<>();
    // private static Bundle systemBundle;

   public static void set(Map<BundleCoordinate, Bundle> bundleCoordinateMap, Map<ClassLoader, Bundle> classLoaderMap) {
        bundleCoordinateBundleLookup.putAll(bundleCoordinateMap);
        classLoaderBundleLookup.putAll(classLoaderMap);
    }

    public static List discoverExtensions(Class<?> serviceClass) {
        List servicesList = new ArrayList();

        // get the current context class loader
        ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();

        for (Entry<ClassLoader, Bundle> entry : classLoaderBundleLookup.entrySet()) {
            // Must set the context class loader to the nar classloader itself
            // so that static initialization techniques that depend on the context class loader will work properly
            final ClassLoader ncl = entry.getKey();
            Thread.currentThread().setContextClassLoader(ncl);

            final Bundle bundle = entry.getValue();
            final List list = loadExtensions(serviceClass, bundle);
            if (null != list && !list.isEmpty()) {
                servicesList.addAll(list);
            }
        }

        // restore the current context class loader if appropriate
        if (currentContextClassLoader != null) {
            Thread.currentThread().setContextClassLoader(currentContextClassLoader);
        }
        return servicesList;
    }

    private static List loadExtensions(Class<?> serviceClass, final Bundle bundle) {
        List servicesList = new ArrayList();
        final ServiceLoader<?> serviceLoader = ServiceLoader.load(serviceClass, bundle.getClassLoader());
        final Iterator<?> iterator = serviceLoader.iterator();
        while (iterator.hasNext()) {
            Object o = null;
            try {
                o = iterator.next();
            } catch (Throwable e) {
                logger.error(String.format("Unable to create the service %s from %s due to %s", serviceClass.getSimpleName(), bundle.getBundleDetails().toString(), e.getMessage()));
                continue; // try to load another components, so ignore current one
            }

            // only consider extensions discovered directly in this bundle
            boolean registerExtension = bundle.getClassLoader().equals(o.getClass().getClassLoader());
            if (registerExtension) {
                servicesList.add(o);
            }
        }
        return servicesList;
    }
}
