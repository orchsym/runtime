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
package com.orchsym.util;

import org.apache.nifi.util.NiFiProperties;

/**
 * 
 * @author GU Guoqiang
 *
 */
public final class LocalResource {

    public static String getLocalUrl(String path) {
        final NiFiProperties properties = NiFiProperties.createBasicNiFiProperties(null, null);

        final String httpHost = properties.getProperty(NiFiProperties.WEB_HTTP_HOST);
        final Integer httpPort = properties.getIntegerProperty(NiFiProperties.WEB_HTTP_PORT, null);

        final String httpsHost = properties.getProperty(NiFiProperties.WEB_HTTPS_HOST);
        final Integer httpsPort = properties.getIntegerProperty(NiFiProperties.WEB_HTTPS_PORT, null);

        String host = "127.0.0.1";
        int port;
        String scheme;
        if (null != httpPort) {// http
            scheme = "http";
            port = httpPort;
            if (null != httpHost && !httpHost.isEmpty()) {
                host = httpHost;
            }
        } else {// https
            scheme = "https";
            port = httpsPort;
            if (null != httpsHost && !httpsHost.isEmpty()) {
                host = httpsHost;
            }
        }

        String url = scheme + "://" + host + ":" + port;
        if (null != path) {
            path = path.trim();
            if (!path.isEmpty()) {
                if (!path.startsWith("/")) {
                    url += '/';
                }
                url += path.trim();
            }
        }

        return url;
    }

}
