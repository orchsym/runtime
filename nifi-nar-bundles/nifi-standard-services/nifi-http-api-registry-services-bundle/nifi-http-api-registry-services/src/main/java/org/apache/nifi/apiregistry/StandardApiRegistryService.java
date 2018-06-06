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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import java.io.BufferedReader;
import java.io.InputStreamReader;

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
    final CopyOnWriteArrayList<ApiInfo> apiInfos = new CopyOnWriteArrayList();

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
                HashMap<String, ArrayList<ApiInfo>> apis = new HashMap();
                ArrayList<ApiInfo> collectApis = new ArrayList();

                //get groupid, uri:/apis?groupid=123
                String query = request.getQueryString();
                String queryGroupID = null;
                
                try {
                    Map<String, String> query_pairs = splitQuery(query);
                    for (Map.Entry<String, String> entry : query_pairs.entrySet()) {  
                        if (entry.getKey().equals("groupid")) {
                            if (!entry.getValue().equals("")) {
                                queryGroupID = entry.getValue();
                            }
                        }
                    }
                }catch(Exception e) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().flush();
                    return;
                }
                
                if (queryGroupID != null && this.service != null) {
                    Iterator<ApiInfo> infoItr = this.service.apiInfos.iterator();
                    while (infoItr.hasNext()) {
                        ApiInfo apiInfo = (ApiInfo) infoItr.next();
                        String groupID = apiInfo.groupID;
                        if (groupID.equals(queryGroupID)) {
                            collectApis.add(apiInfo);
                        }
                    }
                }          
                apis.put("apis", collectApis);

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
    public void registerApiInfo(ApiInfo apiInfo, Boolean shouldHandleGroupID) {

        unregisterApiInfo(apiInfo.id);

        this.apiInfos.add(apiInfo);

        if (shouldHandleGroupID) {
            String groupID = getProcessorGroupID(apiInfo.id);
            modifyApiInfo(apiInfo.id, "groupID", groupID);
        }
    }

    @Override
    public void modifyApiInfo(String id, String key, String value) {

        ListIterator<ApiInfo> infoItr = this.apiInfos.listIterator();
        while (infoItr.hasNext()) {

            ApiInfo apiInfo = (ApiInfo) infoItr.next();
            String apiId = apiInfo.id;

            if (apiId.equals(id)) {
                //modify key value
                if (key.equals("Hostname")) {
                    apiInfo.host = value;
                } else if (key.equals("Listening Port")) {
                    apiInfo.port = Integer.parseInt(value);
                } else if (key.equals("Allowed Paths")) {
                    apiInfo.path = value;
                } else if (key.equals("Default URL Character Set")) {
                    apiInfo.charset = value;
                } else if (key.equals("Allow GET")) {
                    apiInfo.allowGet = Boolean.valueOf(value);
                } else if (key.equals("Allow POST")) {
                    apiInfo.allowPost = Boolean.valueOf(value);
                } else if (key.equals("Allow PUT")) {
                    apiInfo.allowPut = Boolean.valueOf(value);
                } else if (key.equals("Allow DELETE")) {
                    apiInfo.allowDelete = Boolean.valueOf(value);
                } else if (key.equals("Allow HEAD")) {
                    apiInfo.allowHead = Boolean.valueOf(value);
                } else if (key.equals("Allow OPTIONS")) {
                    apiInfo.allowOptions = Boolean.valueOf(value);
                } else if (key.equals("state")) {
                    apiInfo.state = value;
                } else if (key.equals("groupID")) {
                    apiInfo.groupID = value;
                }else if (key.equals("SSL Context Service")) {
                    if (value.equals("null")) {
                        apiInfo.schema = "http";
                    } else {
                        apiInfo.schema = "https";
                    }
                }
            }
        }
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

    private String getProcessorGroupID(String processorID){

        String groupID = "";
        String url = "http://127.0.0.1:8080/nifi-api/processors/" + processorID; 

        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(url);

        try {

            HttpResponse response = client.execute(request);
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            JsonObject jsonObject = new JsonParser().parse(result.toString()).getAsJsonObject();

            JsonObject statusObj = jsonObject.getAsJsonObject("status");

            for (Entry<String, JsonElement> entry : statusObj.entrySet())
            {
                if (entry.getKey().equals("groupId")) {
                    groupID = ((JsonElement)entry.getValue()).getAsString();
                }
            }
            
        } catch (IOException except){
        } finally {
            request.releaseConnection();
        }
        
        return groupID;
    }

    private Map<String, String> splitQuery(String query) throws UnsupportedEncodingException{
        Map<String, String> query_pairs = new HashMap();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }



    public String getRequestPath() {
        return this.requestPath;
    }

}
