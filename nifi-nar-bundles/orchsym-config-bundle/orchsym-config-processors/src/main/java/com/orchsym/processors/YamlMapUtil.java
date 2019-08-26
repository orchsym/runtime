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
package com.orchsym.processors;

import java.util.Map;
import java.util.Set;

/**
 * @author liuxun
 * @apiNote 实现将yaml的对象转为一级Map
 */
public class YamlMapUtil {

    /**
     * @param sourceMap 源Map
     * @param targetMap 目标Map
     * @param parentKey 父key
     */
    public static void toMap(Map sourceMap, Map targetMap, String parentKey) {
        Set keySet = sourceMap.keySet();
        for (Object key : keySet) {
            if (sourceMap.get(key) instanceof Map) {
                toMap((Map) sourceMap.get(key), targetMap, parentKey + (parentKey.isEmpty() ? "" : ".") + key);
            } else {
                targetMap.put(parentKey + (parentKey.isEmpty() ? "" : ".") + key, sourceMap.get(key));
            }
        }
    }

}
