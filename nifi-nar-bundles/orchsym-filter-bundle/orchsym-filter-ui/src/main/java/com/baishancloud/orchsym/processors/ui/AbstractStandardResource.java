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
package com.baishancloud.orchsym.processors.ui;

import org.apache.nifi.web.NiFiWebConfigurationContext;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;


public abstract class AbstractStandardResource {

    @Context
    protected ServletContext servletContext;

    @Context
    protected HttpServletRequest request;

    protected NiFiWebConfigurationContext getWebConfigurationContext() {
        return (NiFiWebConfigurationContext) servletContext.getAttribute("nifi-web-configuration-context");
    }
}
