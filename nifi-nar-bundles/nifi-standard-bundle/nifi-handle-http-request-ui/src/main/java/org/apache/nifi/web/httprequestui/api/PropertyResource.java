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
package org.apache.nifi.web.httprequestui.api;

import org.apache.nifi.web.ComponentDetails;
import org.apache.nifi.web.HttpServletConfigurationRequestContext;
import org.apache.nifi.web.HttpServletRequestContext;
import org.apache.nifi.web.NiFiWebConfigurationContext;
import org.apache.nifi.web.NiFiWebConfigurationRequestContext;
import org.apache.nifi.web.NiFiWebRequestContext;
import org.apache.nifi.web.Revision;
import org.apache.nifi.web.UiExtensionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.apache.nifi.web.httprequestui.model.PropertyInfoEntity;
import org.apache.nifi.web.httprequestui.model.ParameterInfo;
import org.apache.nifi.web.httprequestui.model.RespModel;
import org.apache.nifi.web.httprequestui.model.RespInfo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 *
 */
@Path("/property")
public class PropertyResource {

    private static final Logger logger = LoggerFactory.getLogger(PropertyResource.class);

    @Context
    private ServletContext servletContext;

    @Context
    private HttpServletRequest request;

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/info")
    public Response getPropertyInfos(
        @QueryParam("processorId") final String processorId) {

        // get the web context
        final NiFiWebConfigurationContext configurationContext = (NiFiWebConfigurationContext) servletContext.getAttribute("nifi-web-configuration-context");

        // build the web context config
        final NiFiWebRequestContext requestContext = getRequestContext(processorId);

        PropertyInfoEntity propertyInfoEntity = getPropertyInfos(configurationContext, requestContext);

        Gson gson = new Gson();
        String json = gson.toJson(propertyInfoEntity);
        final ResponseBuilder response = Response.ok(json);
        return noCache(response).build();
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/info")
    public Response createPropertyInfos(
            @Context final UriInfo uriInfo,
            final PropertyInfoEntity properties) {

        // get the web context
        final NiFiWebConfigurationContext configurationContext = (NiFiWebConfigurationContext) servletContext.getAttribute("nifi-web-configuration-context");

        // build the request context
        final NiFiWebConfigurationRequestContext requestContext = getConfigurationRequestContext(properties.processorId, properties.revision, properties.clientId);

        savePropertyInfos(requestContext, properties);
        final ResponseBuilder response = Response.ok("保存成功!");
        return noCache(response).build();
    }

    private NiFiWebRequestContext getRequestContext(final String processorId) {
        return new HttpServletRequestContext(UiExtensionType.ProcessorConfiguration, request) {
            @Override
            public String getId() {
                return processorId;
            }
        };
    }

    private PropertyInfoEntity getPropertyInfos(final NiFiWebConfigurationContext configurationContext, final NiFiWebRequestContext requestContext) {
        final ComponentDetails processorDetails;

        try {
            // load the processor configuration
            processorDetails = configurationContext.getComponentDetails(requestContext);
        } catch (final Exception e) {
            final String message = String.format("Unable to get PropertyInfos[id=%s]: %s", requestContext.getId(), e);
            logger.error(message, e);
            throw new WebApplicationException(e, error(message));
        }

        PropertyInfoEntity propertyInfoEntity = null;
        if (processorDetails != null) {
            try {
                String annotation = processorDetails.getAnnotationData();
                Gson gson = new Gson();
                propertyInfoEntity = gson.fromJson(annotation, new TypeToken<PropertyInfoEntity>(){}.getType());
            } catch (final IllegalArgumentException iae) {
                final String message = String.format("Unable to deserialize existing PropertyInfos for HandleHttpRequest[id=%s]. Deserialization error: %s", requestContext.getId(), iae);
                logger.error(message, iae);
                throw new WebApplicationException(iae, error(message));
            }
        }
        // ensure the propertyInfoEntity isn't null
        if (propertyInfoEntity == null) {
            propertyInfoEntity = new PropertyInfoEntity();
            //add 2 default model
            for (int i=0; i<2; i++) {
                RespModel model = new RespModel();
                model.id = UUID.randomUUID().toString();
                model.name = "model" + String.valueOf(i+1);
                ArrayList<String> contentTypeArray = new ArrayList();
                contentTypeArray.add("application/json");
                model.contentType = contentTypeArray;
                model.description = "this is description of " + model.name;
                Map<String, Map<String, String>> properties = new HashMap();
                Map<String, String> property = new HashMap();
                property.put("type", "string");
                property.put("description", "this is description");
                properties.put("name", property);

                if (i == 0) {
                    Map<String, String> property2 = new HashMap();
                    property2.put("type", "integer");
                    properties.put("name2", property2);
                }
                model.properties = properties;
                propertyInfoEntity.respModels.add(model);
            }
            //add default response
            RespInfo respInfo = new RespInfo();
            respInfo.code = "200";
            respInfo.description = "description";
            respInfo.type = "object";
            respInfo.ref = "model1";
            ArrayList<RespInfo> arr= new ArrayList();
            arr.add(respInfo);
            propertyInfoEntity.respInfos.put("get", arr);
            propertyInfoEntity.respInfos.put("post", arr);
            propertyInfoEntity.respInfos.put("delete", arr);
            propertyInfoEntity.respInfos.put("put", arr);
        }

        //get properties
        if (processorDetails != null) {
            Map<String, String> properties = processorDetails.getProperties();
            // String path = properties.get("Allowed Paths");
            // propertyInfoEntity.path = path != null ? path : "";

            String port = properties.get("Listening Port");
            propertyInfoEntity.host = "localhost:" + port;

            propertyInfoEntity.methods.clear();
            Boolean allowed = properties.get("Allow GET").equals("true");
            if (allowed) {
                propertyInfoEntity.methods.add("get");
            }
            allowed = properties.get("Allow POST").equals("true");
            if (allowed) {
                propertyInfoEntity.methods.add("post");
            }
            allowed = properties.get("Allow DELETE").equals("true");
            if (allowed) {
                propertyInfoEntity.methods.add("delete");
            }
            allowed = properties.get("Allow PUT").equals("true");
            if (allowed) {
                propertyInfoEntity.methods.add("put");
            }
            //remove unsupported method's parameter infos
            Iterator<Map.Entry<String, ArrayList<ParameterInfo>>> it = propertyInfoEntity.parameters.entrySet().iterator();
            while(it.hasNext()){  
                Map.Entry<String, ArrayList<ParameterInfo>> entry=it.next();  
                String key=entry.getKey();  
                if(!propertyInfoEntity.methods.contains(key)){ 
                    it.remove();  
                }  
            }
            //remove unsupported method's decription infos
            Iterator<Map.Entry<String, String>> itDesc = propertyInfoEntity.description.entrySet().iterator();
            while(itDesc.hasNext()){  
                Map.Entry<String, String> entry=itDesc.next();  
                String key=entry.getKey();  
                if(!propertyInfoEntity.methods.contains(key)){ 
                    itDesc.remove();  
                }  
            }
            //remove unsupported method's contentType infos
            Iterator<Map.Entry<String, ArrayList<String>>> itContType = propertyInfoEntity.contentType.entrySet().iterator();
            while(itContType.hasNext()){  
                Map.Entry<String, ArrayList<String>> entry=itContType.next();  
                String key=entry.getKey();  
                if(!propertyInfoEntity.methods.contains(key)){ 
                    itContType.remove();  
                }  
            }
        }
        return propertyInfoEntity;
    }

    private void savePropertyInfos(final NiFiWebConfigurationRequestContext requestContext, PropertyInfoEntity parameter) {

        Gson gson = new Gson();
        String annotationData = gson.toJson(parameter);
        // get the web context
        final NiFiWebConfigurationContext configurationContext = (NiFiWebConfigurationContext) servletContext.getAttribute("nifi-web-configuration-context");

        try {
            // save the annotation data
            configurationContext.updateComponent(requestContext, annotationData, null);

        } catch (final Exception e) {
            final String message = String.format("Unable to save parameter[id=%s] in: %s", requestContext.getId(), e);
            logger.error(message, e);
        }
    }

    private NiFiWebConfigurationRequestContext getConfigurationRequestContext(final String processorId, final Long revision, final String clientId) {
        return new HttpServletConfigurationRequestContext(UiExtensionType.ProcessorConfiguration, request) {
            @Override
            public String getId() {
                return processorId;
            }

            @Override
            public Revision getRevision() {
                return new Revision(revision, clientId, processorId);
            }
        };
    }

    private ResponseBuilder noCache(ResponseBuilder response) {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setPrivate(true);
        cacheControl.setNoCache(true);
        cacheControl.setNoStore(true);
        return response.cacheControl(cacheControl);
    }

    private Response error(final String message) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(message).type("text/plain").build();
    }

}
