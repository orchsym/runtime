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
package org.apache.nifi.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ComponentsContext {
    public static final String CONF_LIST = "components.preview.conf.list";
    public static final String PROP_LIST = "components.preview.list";

    public static final String CONF_ENABLED = "components.preview.enabled";
    public static final String PROP_UPDATED = "components.preview.updated";

    public static final String COMP_SEP = ",";

    public static final Function<String, String> FUN_TYPE_NAME = type -> type.lastIndexOf('.') > 0 ? type.substring(type.lastIndexOf('.') + 1) : type;
    public static final Function<String, List<String>> FUN_COMP_LIST = str -> (null == str || str.trim().isEmpty()) ? Collections.emptyList()
            : new ArrayList<>(Arrays.asList(str.split(COMP_SEP)).stream().filter(c -> c != null && c.trim().length() > 0).map(c -> c.trim()).collect(Collectors.toSet()));

    private static List<String> previewComponents;

    private static List<String> getPreviewList() {
        final List<String> propList = FUN_COMP_LIST.apply(System.getProperty(PROP_LIST, ""));
        final List<String> confList = FUN_COMP_LIST.apply(System.getProperty(CONF_LIST, ""));
        Set<String> compsSet = new HashSet<>(propList);
        compsSet.addAll(confList);
        return Collections.unmodifiableList(new ArrayList<>(compsSet));
    }

    private static List<String> getPreviewComponents() {
        if (null == previewComponents || previewComponents.isEmpty()) {
            synchronized (ComponentsContext.class) {
                if (null == previewComponents || previewComponents.isEmpty()) {
                    previewComponents = getPreviewList();
                }
            }
        }

        if (isPreviewUpdated()) {// maybe the lic changed, need update
            synchronized (ComponentsContext.class) {
                if (isPreviewUpdated()) {
                    previewComponents = getPreviewList();
                    setPriviewUpdated(false);
                }
            }
        }
        return previewComponents;
    }

    private static void setPriviewUpdated(boolean updated) {
        System.setProperty(PROP_UPDATED, Boolean.toString(updated));
    }

    public static boolean isPreviewUpdated() {
        return Boolean.getBoolean(PROP_UPDATED);
    }

    public static boolean isPreviewEnabled() {
        return Boolean.getBoolean(CONF_ENABLED);
    }

    public static boolean isPreviewType(String type) {
        if (null == type || type.isEmpty()) {
            return false;
        }
        final List<String> previewList = getPreviewComponents();
        return previewList.contains(FUN_TYPE_NAME.apply(type)) || previewList.contains(type);
    }

    public static boolean isPreview(Class<?> component) {
        if (null == component) {
            return false;
        }
        return isPreviewType(component.getName());
    }
}
