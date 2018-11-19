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
package org.apache.nifi.web.api.dto.util;

import java.util.Locale;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * XmlAdapter for (un)marshalling a locale.
 */
public class LocaleAdapter extends XmlAdapter<String, Locale> {

    @Override
    public String marshal(Locale locale) throws Exception {
        return locale.toString();
    }

    @Override
    public Locale unmarshal(String locale) throws Exception {
        Locale loc = null;
        if (locale != null && !locale.isEmpty()) {
            final String[] values = locale.split("_");
            if (values.length > 1) {
                loc = new Locale(values[0], values[1]);
            } else if (values.length == 1) {
                loc = new Locale(locale);
            }
        }
        return loc;
    }

}
