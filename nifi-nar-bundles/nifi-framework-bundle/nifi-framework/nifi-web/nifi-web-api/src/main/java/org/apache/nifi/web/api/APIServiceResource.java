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
package org.apache.nifi.web.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.nifi.registry.api.APIServicesManager;
import org.apache.nifi.registry.api.APISpec;
import org.apache.nifi.registry.api.ApiInfo;
import org.apache.nifi.registry.api.InfoSpec;
import org.apache.nifi.registry.api.ParamSpec;
import org.apache.nifi.registry.api.PathSpec;
import org.apache.nifi.registry.api.PropertySpec;
import org.apache.nifi.registry.api.RespSpec;
import org.apache.nifi.util.HttpRequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * RESTful endpoint for retrieving api informaitions
 */
@Path(APIServiceResource.PATH)
@Api(value = APIServiceResource.PATH, //
        description = "Endpoint  for retrieving api informaitions")
public class APIServiceResource extends ApplicationResource {
    public static final String PATH = "/apis";

    private static final Logger logger = LoggerFactory.getLogger(APIServiceResource.class);

    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/")
    @ApiOperation(value = "Gets all api informaitions or filter by a group id", //
            response = ArrayList.class)
    @ApiResponses(value = { //
            @ApiResponse(code = 400, message = StatsResource.CODE_MESSAGE_400), //
            @ApiResponse(code = 404, message = StatsResource.CODE_MESSAGE_404), //
            @ApiResponse(code = 409, message = StatsResource.CODE_MESSAGE_409) //
    })
    public Response getApiInformations(//
            @QueryParam("groupid") String groupid) throws InterruptedException {

        if (isReplicateRequest()) {
            return replicate(HttpMethod.GET);
        }
        final List<ApiInfo> apiInfoList = APIServicesManager.getInstance().getInfos();
        ArrayList<ApiInfo> collectApis = new ArrayList<>();
        if (groupid == null) {
            Iterator<ApiInfo> infoItr = apiInfoList.iterator();
            while (infoItr.hasNext()) {
                ApiInfo apiInfo = (ApiInfo) infoItr.next();
                collectApis.add(apiInfo.copy());
            }
        } else {
            Iterator<ApiInfo> infoItr = apiInfoList.iterator();
            while (infoItr.hasNext()) {
                ApiInfo apiInfo = (ApiInfo) infoItr.next();
                String groupID = apiInfo.groupID;
                if (groupID.equals(groupid)) {
                    collectApis.add(apiInfo.copy());
                }
            }
        }
        // generate the response
        HashMap<String, ArrayList<ApiInfo>> apis = new HashMap<>();
        apis.put("apis", collectApis);
        Gson gson = new Gson();
        String json = gson.toJson(apis);
        return Response.ok(json).build();
    }

    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/swagger")
    @ApiOperation(value = "Gets all api informaitions or filter by a group id", //
            response = ArrayList.class)
    @ApiResponses(value = { //
            @ApiResponse(code = 400, message = StatsResource.CODE_MESSAGE_400), //
            @ApiResponse(code = 404, message = StatsResource.CODE_MESSAGE_404), //
            @ApiResponse(code = 409, message = StatsResource.CODE_MESSAGE_409) //
    })
    public Response getApiSwaggerInformations(//
            @QueryParam("id") String processorId) {

        if (isReplicateRequest()) {
            return replicate(HttpMethod.GET);
        }
        if (processorId != null) {

            String body;
            try {
                String url = getHandleHttpRequestUiUrl(processorId);
                body = HttpRequestUtil.getString(url);
            } catch (IOException e) {
                return buildException(e);
            }

            try {
                JsonObject infoObject = new JsonParser().parse(body).getAsJsonObject();

                final List<ApiInfo> apiInfoList = APIServicesManager.getInstance().getInfos();
                String swaggerInfo = getSwaggerSpec(apiInfoList, infoObject, processorId);

                return Response.ok(swaggerInfo).build();
            } catch (JsonSyntaxException e) { // invalid json
                final String message = "Can't parse the value: \n" + body;
                final Exception e2 = new Exception(message, e);
                logger.error(e2.getMessage(), e2);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(message).build();
            }
        }
        return Response.noContent().build();
    }

    private Response buildException(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        if (e.getCause() != null) {
            e.getCause().printStackTrace(pw);
        } else {
            e.printStackTrace(pw);
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(sw.toString()).build();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private String getSwaggerSpec(final List<ApiInfo> apiInfoList, JsonObject infoObject, String apiID) {

        Gson gson = new Gson();

        String spec = null;
        APISpec apiSpec = new APISpec();
        // info
        InfoSpec infoSpec = new InfoSpec();
        infoSpec.title = infoObject.get("title").getAsString();
        infoSpec.version = infoObject.get("version").getAsString();
        apiSpec.info = infoSpec;
        // host
        apiSpec.host = infoObject.get("host").getAsString();
        // schemes
        ArrayList<String> schemes = new ArrayList();
        String scheme = getApiScheme(apiInfoList, apiID);
        schemes.add(scheme);
        apiSpec.schemes = schemes;
        // basePath
        apiSpec.basePath = infoObject.get("basePath").getAsString();
        // methods
        JsonArray jsonMethodsArr = infoObject.getAsJsonArray("methods");
        Map<String, Map<String, PathSpec>> paths = new HashMap();
        Map<String, PathSpec> pathSpecMap = new HashMap();
        for (int i = 0; jsonMethodsArr != null && i < jsonMethodsArr.size(); i++) {
            String method = jsonMethodsArr.get(i).getAsString();
            PathSpec pathSpec = new PathSpec();
            // parameters
            ArrayList<ParamSpec> parameters = new ArrayList();
            JsonObject parametersObject = infoObject.getAsJsonObject("parameters");
            JsonArray jsonParamArr = parametersObject.getAsJsonArray(method);

            String consumeContentType = null;
            String consumeRef = null;
            for (int j = 0; jsonParamArr != null && j < jsonParamArr.size(); j++) {
                JsonObject object = (JsonObject) jsonParamArr.get(j).getAsJsonObject();
                ParamSpec parameterSpec = new ParamSpec();
                parameterSpec.name = object.get("name").getAsString();
                parameterSpec.description = object.get("description").getAsString();
                parameterSpec.required = object.get("required").getAsBoolean();
                if (object.has("type")) {
                    parameterSpec.type = object.get("type").getAsString();
                }
                String position = object.get("position").getAsString();
                if (position.equals("form")) {
                    parameterSpec.in = "formData";
                } else {
                    parameterSpec.in = position;
                }
                if (object.has("format")) {
                    parameterSpec.format = object.get("format").getAsString();
                }
                if (object.has("consumes") && (position.equals("form") || position.equals("body"))) {
                    String consumes = object.get("consumes").getAsString();
                    if (consumes.equals("form-data")) {
                        consumeContentType = "multipart/form-data";
                    } else if (consumes.equals("x-www-form-urlencoded")) {
                        consumeContentType = "application/x-www-form-urlencoded";
                    }
                }
                if (object.has("ref")) {
                    Map<String, String> schema = new HashMap<>();
                    consumeRef = object.get("ref").getAsString();
                    String reference = "#/definitions/" + consumeRef;
                    schema.put("$ref", reference);
                    parameterSpec.schema = schema;
                }
                parameters.add(parameterSpec);
            }
            pathSpec.parameters = parameters;
            pathSpec.consumes = new ArrayList<String>();
            pathSpec.produces = new ArrayList<String>();
            // consumes
            if (consumeContentType != null) {
                pathSpec.consumes.add(consumeContentType);
            }
            // produces
            JsonObject contentTypeObject = infoObject.getAsJsonObject("contentType");
            JsonArray contentTypeObjectArr = contentTypeObject.getAsJsonArray(method);
            for (int x = 0; contentTypeObjectArr != null && x < contentTypeObjectArr.size(); x++) {
                String type = contentTypeObjectArr.get(x).getAsString();
                pathSpec.produces.add(type);
            }

            // responses
            JsonObject respinfosObject = infoObject.getAsJsonObject("respInfos");
            JsonArray jsonRespArr = respinfosObject.getAsJsonArray(method);
            pathSpec.responses = new HashMap<String, RespSpec>();
            for (int k = 0; jsonRespArr != null && k < jsonRespArr.size(); k++) {
                JsonObject object = (JsonObject) jsonRespArr.get(k).getAsJsonObject();
                RespSpec respSpec = new RespSpec();
                respSpec.description = object.get("description").getAsString();
                Map<String, Object> schema = new HashMap<>();
                Map<String, String> items = new HashMap<>();
                String reference = null;
                if (object.has("ref")) {
                    reference = object.get("ref").getAsString();
                    if (object.get("type").getAsString().equals("array")) {
                        schema.put("type", "array");
                        items.put("$ref", "#/definitions/" + reference);
                        schema.put("items", items);
                    } else {
                        schema.put("$ref", "#/definitions/" + reference);
                    }
                    respSpec.schema = schema;
                }
                pathSpec.responses.put(object.get("code").getAsString(), respSpec);
                // handle produces consumes
                JsonArray jsondefArr = infoObject.getAsJsonArray("respModels");
                for (int m = 0; jsondefArr != null && m < jsondefArr.size(); m++) {
                    JsonObject respModelObject = (JsonObject) jsondefArr.get(m).getAsJsonObject();
                    String modelName = respModelObject.get("name").getAsString();
                    if (modelName.equals(reference)) {
                        JsonArray typeArr = respModelObject.getAsJsonArray("contentType");
                        for (int n = 0; typeArr != null && n < typeArr.size(); n++) {
                            String type = typeArr.get(n).getAsString();
                            if (!pathSpec.produces.contains(type)) {
                                pathSpec.produces.add(type);
                            }
                        }
                    }
                    if (consumeRef != null && modelName.equals(consumeRef)) {
                        JsonArray typeArr = respModelObject.getAsJsonArray("contentType");
                        for (int n = 0; typeArr != null && n < typeArr.size(); n++) {
                            String type = typeArr.get(n).getAsString();
                            if (!pathSpec.consumes.contains(type)) {
                                pathSpec.consumes.add(type);
                            }
                        }
                    }
                }
            }
            // description
            JsonObject descriObject = infoObject.getAsJsonObject("description");
            pathSpec.description = descriObject.has(method) ? descriObject.get(method).getAsString() : "";
            pathSpecMap.put(method, pathSpec);
        }
        paths.put(infoObject.get("path").getAsString(), pathSpecMap);
        apiSpec.paths = paths;

        // definitions
        Map<String, Map<String, Map<String, PropertySpec>>> definitions = new HashMap<>();
        JsonArray jsondefArr = infoObject.getAsJsonArray("respModels");
        for (int i = 0; jsondefArr != null && i < jsondefArr.size(); i++) {
            // model level
            JsonObject object = (JsonObject) jsondefArr.get(i).getAsJsonObject();
            String modelName = object.get("name").getAsString();

            String respModelJson = object.get("properties").toString();
            Map<String, Map<String, String>> respModelMap = gson.fromJson(respModelJson, new TypeToken<Map<String, Map<String, String>>>() {
            }.getType());
            Iterator propertyEntries = respModelMap.entrySet().iterator();

            Map<String, Map<String, PropertySpec>> map1 = new HashMap<>();
            Map<String, PropertySpec> map2 = new HashMap<>();
            while (propertyEntries.hasNext()) {
                // property level
                Map.Entry entry = (Map.Entry) propertyEntries.next();
                String propertyName = (String) entry.getKey();
                Map<String, String> propertyDesc = (Map<String, String>) entry.getValue();

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

    private String getHandleHttpRequestUiUrl(String processorID) {
        final UriBuilder uriBuilder = this.uriInfo.getBaseUriBuilder();

        String version = APIServicesManager.getInstance().getClass().getPackage().getImplementationVersion(); // get package version
        uriBuilder.replacePath("/nifi-handle-http-request-ui-" + version + "/");

        return buildResourceUri(uriBuilder, "api", "property", "info").toString() + "?processorId=" + processorID;
    }

    private String getApiScheme(final List<ApiInfo> apiInfoList, String id) {
        String scheme = "";
        Iterator<ApiInfo> infoItr = apiInfoList.iterator();
        while (infoItr.hasNext()) {
            ApiInfo apiInfo = (ApiInfo) infoItr.next();
            String apiId = apiInfo.id;
            if (apiId.equals(id)) {
                scheme = apiInfo.scheme;
            }
        }
        return scheme;
    }
}
