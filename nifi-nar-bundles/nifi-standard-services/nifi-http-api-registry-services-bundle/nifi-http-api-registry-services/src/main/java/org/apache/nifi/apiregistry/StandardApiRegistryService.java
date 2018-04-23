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
package org.apache.nifi.apiregistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnDisabled;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.reporting.InitializationException;


import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.gson.Gson;

@Tags({ "api registry"})
@CapabilityDescription("This service is for api registry")
public class StandardApiRegistryService extends AbstractControllerService implements ApiRegisterService {

    public static final PropertyDescriptor REQ_PATH = new PropertyDescriptor
            .Builder().name("Req_Path")
            .displayName("Path")
            .description("request path")
            .required(true)
            .defaultValue("/apis")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor REQ_PORT = new PropertyDescriptor
            .Builder().name("Req_Port")
            .displayName("Port")
            .description("request port")
            .required(true)
            .defaultValue("7878")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    private static final List<PropertyDescriptor> properties;
    private String requestPath;
    private int port;
    private Server server;
    final CopyOnWriteArrayList<ApiInfo>apiInfos = new CopyOnWriteArrayList();

    static {
        final List<PropertyDescriptor> props = new ArrayList<>();
        props.add(REQ_PATH);
        props.add(REQ_PORT);
        properties = Collections.unmodifiableList(props);
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

    @OnEnabled
    public void onConfigured(final ConfigurationContext context) {

        requestPath = context.getProperty(REQ_PATH).getValue();
        port = context.getProperty(REQ_PORT).asInteger();
        try {
            final Server server = new Server(port);
            final HttpConfiguration httpConfiguration = new HttpConfiguration();
       
            // create the connector
            final ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpConfiguration));
            http.setPort(port);

            // add this connector
            server.setConnectors(new Connector[]{http});

            server.setHandler(new MyHandler(this));

            this.server = server;
            server.start();
            } catch(Exception exception){
        }
        
    }

    @OnDisabled
    public void onDisabled() throws Exception{

        server.stop();
        server.destroy();
        server.join();
    }

    public class MyHandler extends AbstractHandler { 

        private StandardApiRegistryService service;

        public  MyHandler(StandardApiRegistryService standardApiRegistryService) {
            this.service = standardApiRegistryService;

        } 
  
        @Override
        public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {

            String pathInfo = request.getPathInfo();
            if (pathInfo.equals(this.service.getRequestPath())) {
                HashMap<String, CopyOnWriteArrayList<ApiInfo>> apis = new HashMap();
                apis.put("apis", this.service.apiInfos);

                Gson gson = new Gson();
                String json = gson.toJson(apis);

                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json");
                response.getWriter().write(json);
                response.getWriter().flush();
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().flush();
            }
        }
    }  

    @Override
    public void registerApiInfo(ApiInfo apiInfo) {

        unregisterApiInfo(apiInfo.id);

        this.apiInfos.add(apiInfo);
    }

    @Override
    public void unregisterApiInfo(String id) {

        Iterator<ApiInfo> infoItr = this.apiInfos.iterator();
        while (infoItr.hasNext()) {

            ApiInfo apiInfo = (ApiInfo) infoItr.next();

            String apiId = apiInfo.id;
            if (apiId.equals(id)) {
                this.apiInfos.remove(apiInfo);
            }
        }
    }

    public String getRequestPath() {
        return this.requestPath;
    }

}
