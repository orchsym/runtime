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
package com.orchsym.udc.data;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orchsym.udc.CollectorService;
import com.orchsym.udc.util.HttpRequestUtil;
import com.orchsym.util.LocalResource;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

/**
 * 
 * @author GU Guoqiang
 *
 */
public abstract class APIBaseCollector implements CollectorService {
    private static final Logger logger = LoggerFactory.getLogger(APIBaseCollector.class);

    @Override
    public JSONObject collect(Map<String, Object> parameters) {

        final String url = LocalResource.getLocalUrl(getUrlPath());
        try {
            final String result = HttpRequestUtil.getString(url);
            final JSONObject json = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(result);

            return retrieveJSON(json);

        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }

        return new JSONObject();

    }

    protected abstract String getUrlPath();

    protected JSONObject retrieveJSON(JSONObject source) {
        return source;
    }
}
