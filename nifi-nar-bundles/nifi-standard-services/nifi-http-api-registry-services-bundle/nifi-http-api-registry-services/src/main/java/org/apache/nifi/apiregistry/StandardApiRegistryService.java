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

import java.net.URLDecoder;

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
import org.apache.nifi.util.NiFiProperties;

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
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.nifi.apiregistry.spec.APISpec;
import org.apache.nifi.apiregistry.spec.InfoSpec;
import org.apache.nifi.apiregistry.spec.PathSpec;
import org.apache.nifi.apiregistry.spec.PropertySpec;
import org.apache.nifi.apiregistry.spec.ParamSpec;
import org.apache.nifi.apiregistry.spec.RespSpec;




@Tags({ "api registry"})
@CapabilityDescription("This service is for api registry")
public class StandardApiRegistryService extends AbstractControllerService implements ApiRegistryService {

    private static final String PROPERTIES_NIFI_WEB_HTTP_HOST = "nifi.web.http.host";
    private static final String PROPERTIES_NIFI_WEB_HTTP_PORT = "nifi.web.http.port";
    private static final String PROPERTIES_NIFI_WEB_HTTPS_HOST = "nifi.web.https.host";
    private static final String PROPERTIES_NIFI_WEB_HTTPS_PORT = "nifi.web.https.port";

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
    public void onConfigured(final ConfigurationContext context) throws Exception{

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

            server.setHandler(new ReqHandler(this));

            this.server = server;
            server.start();
        } catch(Exception e){
            getLogger().error("start api resistry serverice failed ", e);
            throw new Exception("start api resistry serverice failed ", e);
        }   
    }

    @OnDisabled
    public void onDisabled() throws Exception{
        server.stop();
        server.destroy();
        server.join();
    }

    public class ReqHandler extends AbstractHandler { 

        private StandardApiRegistryService service;

        public  ReqHandler(StandardApiRegistryService standardApiRegistryService) {
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

                if (query == null) {
                    Iterator<ApiInfo> infoItr = this.service.apiInfos.iterator();
                    while (infoItr.hasNext()) {
                        ApiInfo apiInfo = (ApiInfo) infoItr.next();
                        collectApis.add(apiInfo);
                    }
                } else {
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
                }
                apis.put("apis", collectApis);
                Gson gson = new Gson();
                String json = gson.toJson(apis);

                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json; charset=utf-8");
                response.getWriter().write(json);
                response.getWriter().flush();
                return;

            } else if(pathInfo.equals("/apis/swagger")){
                ///apis/swagger?processorid=123
                String query = request.getQueryString();
                String processorId = null;

                try {
                    Map<String, String> query_pairs = splitQuery(query);
                    for (Map.Entry<String, String> entry : query_pairs.entrySet()) {  
                        if (entry.getKey().equals("processorid")) {
                            processorId = entry.getValue();
                        }
                    }
                    if (processorId != null) {
                        String swaggerInfo = getSwaggerinfo(processorId);
                        if (swaggerInfo != null) {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.setContentType("application/json");
                            response.getWriter().write(swaggerInfo);
                            response.getWriter().flush();
                            return;
                        }
                    }

                } catch(Exception e) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().flush();
                }
            } 
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().flush();
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
                        apiInfo.scheme = "http";
                    } else {
                        apiInfo.scheme = "https";
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

    private String getProcessorGroupID(String processorId){

        String groupID = "";
        String url = getUrlInfo(processorId);
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
            for (Entry<String, JsonElement> entry : statusObj.entrySet()) {
                if (entry.getKey().equals("groupId")) {
                    groupID = ((JsonElement)entry.getValue()).getAsString();
                }
            }
        } catch (IOException e){
            getLogger().error("parse response failed", e);
        } finally {
            request.releaseConnection();
        }
        
        return groupID;
    }
    
    private String getSwaggerinfo(String processorId) throws Exception{

        String url = getPropertyUrl(processorId);

        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(url);
        String swaggerInfo = null;
        try {
            HttpResponse response = client.execute(request);
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

            StringBuffer info = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                info.append(line);
            }
            swaggerInfo = getSwaggerSpec(info.toString(), processorId);
        } catch (IOException except){
            throw new Exception("get processor's property failed ", except);
        } finally {
            request.releaseConnection();
        }
        
        return swaggerInfo;
    }

    private String getSwaggerSpec(String info, String apiID) {

        info = "{\"method\":\"get\",\"version\":\"1.0\",\"title\":\"test tittle\",\"host\":\"localhost\",\"path\":\"/test\",\"basePath\":\"/v1\",\"description\":\"this is description\",\"summary\":\"this is summary\",\"parameters\":[{\"name\":\"longitude\",\"position\":\"query\",\"required\":true,\"type\":\"string\",\"description\":\"Latitude component of location\"},{\"name\":\"latitude\",\"position\":\"query\",\"required\":true,\"type\":\"string\",\"description\":\"Longitude component of location\"}],\"respInfos\":[{\"code\":\"200\",\"description\":\"response description\",\"type\":\"array\",\"ref\":\"#/definitions/Product\"},{\"code\":\"404\",\"description\":\"response description\",\"type\":\"object\",\"ref\":\"#/definitions/Error\"}],\"respModels\":[{\"name\":\"Product\",\"properties\":{\"product_id\":{\"type\":\"string\",\"description\":\"Unique identifier representing a specific product\"},\"url\":{\"type\":\"string\",\"description\":\"url of a specific product\"}}},{\"name\":\"Error\",\"properties\":{\"message\":{\"type\":\"string\",\"description\":\"message of an error\"}}}]}";
        Gson gson = new Gson();
        JsonObject infoObject = new JsonParser().parse(info).getAsJsonObject();

        String spec = null;
        APISpec apiSpec = new APISpec();
        //info
        InfoSpec infoSpec = new InfoSpec();
        infoSpec.title = infoObject.get("title").getAsString();
        infoSpec.version = infoObject.get("version").getAsString();
        apiSpec.info = infoSpec;
        //host
        apiSpec.host = infoObject.get("host").getAsString();
        //schemes
        ArrayList<String> schemes = new ArrayList();
        String scheme = getApiScheme(apiID);
        schemes.add(scheme);
        apiSpec.schemes = schemes;
        //basePath
        apiSpec.basePath = infoObject.get("basePath").getAsString();
        //consumes、produces
        ArrayList<String> consumes = new ArrayList();
        ArrayList<String> produces = new ArrayList();
        consumes.add("application/json");
        produces.add("application/json");
        apiSpec.consumes = consumes;
        apiSpec.produces = produces;

        PathSpec pathSpec = new PathSpec();
        //parameters
        ArrayList<ParamSpec> parameters = new ArrayList();
        JsonArray jsonParamArr = infoObject.getAsJsonArray("parameters");
        for (int i=0; i<jsonParamArr.size(); i++) {
            JsonObject object = (JsonObject) jsonParamArr.get(i).getAsJsonObject();
            ParamSpec parameterSpec = new ParamSpec();
            parameterSpec.name = object.get("name").getAsString();
            parameterSpec.in = object.get("position").getAsString();
            parameterSpec.description = object.get("description").getAsString();
            parameterSpec.type = object.get("type").getAsString();
            parameterSpec.required = object.get("required").getAsBoolean();
            parameters.add(parameterSpec);

        }
        pathSpec.parameters = parameters;
        //responses
        JsonArray jsonRespArr = infoObject.getAsJsonArray("respInfos");
        pathSpec.responses = new HashMap<String, RespSpec>();
        for (int i=0; i<jsonRespArr.size(); i++) {
            JsonObject object = (JsonObject) jsonRespArr.get(i).getAsJsonObject();
            RespSpec respSpec = new RespSpec();
            respSpec.description = object.get("description").getAsString();
            Map<String, Object> schema = new HashMap();
            Map<String,String> items = new HashMap();
            if (object.get("type").getAsString().equals("array")) {
                schema.put("type","array");
                items.put("$ref",object.get("ref").getAsString());
                schema.put("items", items); 
            } else{
                schema.put("$ref", object.get("ref").getAsString());
            }
            respSpec.schema = schema;
            pathSpec.responses.put(object.get("code").getAsString(), respSpec);

        }

        pathSpec.summary = infoObject.get("summary").getAsString();
        pathSpec.description = infoObject.get("description").getAsString();
        Map<String, Map<String, PathSpec>> paths = new HashMap();
        Map<String, PathSpec> pathSpecMap = new HashMap();
        pathSpecMap.put(infoObject.get("method").getAsString(), pathSpec);
        paths.put(infoObject.get("path").getAsString(), pathSpecMap);
        apiSpec.paths = paths;

        //definitions
        Map<String, Map<String, Map<String, PropertySpec>>> definitions = new HashMap();
        JsonArray jsondefArr = infoObject.getAsJsonArray("respModels");
        for (int i=0; i<jsondefArr.size(); i++) {
            //model level
            JsonObject object = (JsonObject) jsondefArr.get(i).getAsJsonObject();
            
            String modelName = object.get("name").getAsString();
            String respModelJson = object.get("properties").toString();

            Map<String, Map<String, String>> respModelMap = gson.fromJson(respModelJson, new TypeToken<Map<String, Map<String, String>>>(){}.getType());

            Iterator propertyEntries = respModelMap.entrySet().iterator(); 

            Map<String, Map<String, PropertySpec>> map1 = new HashMap();
            Map<String, PropertySpec> map2 = new HashMap();
            while (propertyEntries.hasNext()) {
                //property level
                Map.Entry entry = (Map.Entry) propertyEntries.next();  
                String propertyName = (String)entry.getKey();  
                Map<String, String> propertyDesc = (Map<String, String>)entry.getValue();

                PropertySpec propertySpec = new PropertySpec();
                for (Map.Entry<String, String> proEntry : propertyDesc.entrySet()) {
                    if (proEntry.getKey().equals("type")) {
                        propertySpec.type = proEntry.getValue();
                    } else if (proEntry.getKey().equals("description")) {
                        propertySpec.description = proEntry.getValue();
                    }
                }
                map2.put(propertyName, propertySpec);
            }

            map1.put("properties", map2);
            definitions.put(modelName, map1);
        }
        apiSpec.definitions = definitions;

        spec = gson.toJson(apiSpec);

        return spec;
    }

    private String getUrlInfo(String processorID) {

        String host;
        String port;
        String scheme;

        final NiFiProperties properties = NiFiProperties.createBasicNiFiProperties(null, null);
        String httpHost = properties.getProperty(PROPERTIES_NIFI_WEB_HTTP_HOST);
        String httpPort = properties.getProperty(PROPERTIES_NIFI_WEB_HTTP_PORT);
        String httpsHost = properties.getProperty(PROPERTIES_NIFI_WEB_HTTPS_HOST);
        String httpsPort = properties.getProperty(PROPERTIES_NIFI_WEB_HTTPS_PORT);

        if (!httpPort.trim().equals("")) {
            //http
            scheme = "http";
            port = httpPort;
            if (httpHost.trim().equals("")) {
                host = "127.0.0.1";
            } else {
                host = httpHost;
            }
        } else {
            //https
            scheme = "https";
            port = httpsPort;
            if (httpsHost.trim().equals("")) {
                host = "127.0.0.1";
            } else {
                host = httpsHost;
            }
        }
        String url = scheme + "://" + host + ":" + port + "/nifi-api/processors/" + processorID;
        return url;
    }

    private String getPropertyUrl(String processorID) {

        String host;
        String port;
        String scheme;
        String version =  this.getClass().getPackage().getImplementationVersion(); //get package version

        final NiFiProperties properties = NiFiProperties.createBasicNiFiProperties(null, null);
        String httpHost = properties.getProperty(PROPERTIES_NIFI_WEB_HTTP_HOST);
        String httpPort = properties.getProperty(PROPERTIES_NIFI_WEB_HTTP_PORT);
        String httpsHost = properties.getProperty(PROPERTIES_NIFI_WEB_HTTPS_HOST);
        String httpsPort = properties.getProperty(PROPERTIES_NIFI_WEB_HTTPS_PORT);

        if (!httpPort.trim().equals("")) {
            //http
            scheme = "http";
            port = httpPort;
            if (httpHost.trim().equals("")) {
                host = "127.0.0.1";
            } else {
                host = httpHost;
            }
        } else {
            //https
            scheme = "https";
            port = httpsPort;
            if (httpsHost.trim().equals("")) {
                host = "127.0.0.1";
            } else {
                host = httpsHost;
            }
        }
        String url = scheme + "://" + host + ":" + port + "/nifi-handle-http-request-ui-" + version +"/api/property/info?processorId="  + processorID;
        System.out.println("11111 property URL: " + url);
        return url;
    }

    private String getApiScheme(String id) {
        String scheme = "";
        Iterator<ApiInfo> infoItr = this.apiInfos.iterator();
        while (infoItr.hasNext()) {
            ApiInfo apiInfo = (ApiInfo) infoItr.next();
            String apiId = apiInfo.id;
            if (apiId.equals(id)) {
                scheme = apiInfo.scheme;
            }
        }
        return scheme;
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
