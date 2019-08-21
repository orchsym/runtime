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
package org.apache.nifi.registry.api;

public class ApiInfo implements Cloneable {
    public enum State {
        init, running, stopped;
    }

    public String name;

    public String id;

    public String groupID = "";

    public String path;

    public String host;

    public String charset = "UTF-8";

    public String scheme = "http";

    public Integer port = 80;

    // 1 min, same as the default value of REQUEST_EXPIRATION in StandardHttpContextMap
    public Long requestTimeout = 1 * 60 * 1000L;

    public Boolean allowGet = false;

    public Boolean allowPost = false;

    public Boolean allowPut = false;

    public Boolean allowDelete = false;

    public Boolean allowHead = false;

    public Boolean allowOptions = false;

    public String state = State.init.name(); // init running stopped

    public transient String controllerServiceId;

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public ApiInfo copy() {
        ApiInfo copied = null;
        try {
            copied = (ApiInfo) this.clone();

            copied.controllerServiceId = null; // remove
        } catch (CloneNotSupportedException e) {
            //
        }

        return copied;
    }

}
