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
package com.orchsym.udc.data;

import java.util.Iterator;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

/**
 * @author GU Guoqiang
 *
 */
public class ApplicationsCollector extends APIBaseCollector {

    @Override
    protected String getUrlPath() {
        return "/nifi-api/flow/process-groups/root";
    }

    @Override
    protected JSONObject retrieveJSON(JSONObject source) {
        JSONObject data = new JSONObject();

        final JSONObject groupFlowJson = (JSONObject) source.get("processGroupFlow");

        if (null != groupFlowJson) {
            final String rootId = groupFlowJson.getAsString("id"); // root id
            data.put("id", rootId);

            JSONObject flowJson = (JSONObject) groupFlowJson.get("flow");
            if (null != flowJson) {
                JSONObject apps = new JSONObject();
                data.put("applications", apps);

                JSONArray groupsArr = (JSONArray) flowJson.get("processGroups");

                if (null != groupsArr) {
                    apps.put("count", groupsArr.size());

                    JSONArray groupList = new JSONArray();
                    apps.put("list", groupList);

                    final Iterator<Object> iterator = groupsArr.iterator();
                    while (iterator.hasNext()) {
                        JSONObject one = (JSONObject) iterator.next();
                        final String appId = one.getAsString("id");

                        JSONObject app = new JSONObject();

                        groupList.add(app);

                        app.put("appId", appId);
                        JSONObject statusJson = (JSONObject) one.get("status");
                        if (null != statusJson) {
                            final String name = statusJson.getAsString("name");
                            if (null != name) {
                                app.put("appName", name);
                            }
                        }

                        // status
                        final Number running = one.getAsNumber("runningCount");
                        if (null != running) {
                            app.put("running", running.intValue() > 0);
                        }

                        // revision
                        JSONObject revisionJson = (JSONObject) one.get("revision");
                        if (null != revisionJson) {
                            final Number revision = revisionJson.getAsNumber("version");
                            if (null != revision) {
                                app.put("revision", revision.intValue());
                            }
                        }

                    }
                }
            }
        }

        return data;
    }

}
