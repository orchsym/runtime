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

import net.minidev.json.JSONObject;

/**
 * @author GU Guoqiang
 *
 */
public class AboutCollector extends APIBaseCollector {

    @Override
    protected String getUrlPath() {
        return "/nifi-api/flow/about";
    }

    @Override
    protected JSONObject retrieveJSON(JSONObject source) {
        final JSONObject aboutJson = (JSONObject) source.get("about");
       
        aboutJson.remove("contentViewerUrl");
        aboutJson.remove("uri");
        
        return source;
    }

}
