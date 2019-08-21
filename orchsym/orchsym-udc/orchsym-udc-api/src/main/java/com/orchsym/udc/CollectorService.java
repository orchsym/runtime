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
package com.orchsym.udc;

import java.util.Map;

import net.minidev.json.JSONObject;

/**
 * @author GU Guoqiang
 *
 */
public interface CollectorService {

    default void setValue(JSONObject target, JSONObject source, String key, Object defaultValue) {
        if (null == target || null == source || null == key || key.isEmpty()) {
            return;
        }
        final Object value = source.getOrDefault(key, defaultValue);
        if (null != value) {
            target.put(key, value);
        }
    }

    JSONObject collect(Map<String, Object> parameters);
}
