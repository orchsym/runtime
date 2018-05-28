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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.apache.nifi.web.httprequestui.model.PropertyInfoEntity;

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
        System.out.println("1111 getAnnotationData: " + json);
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

        System.out.println("1111 processorId: " + properties.processorId + " datas: " + properties.parameters);
        // get the web context
        final NiFiWebConfigurationContext configurationContext = (NiFiWebConfigurationContext) servletContext.getAttribute("nifi-web-configuration-context");

        // build the request context
        final NiFiWebConfigurationRequestContext requestContext = getConfigurationRequestContext(properties.processorId, properties.revision, properties.clientId);

        savePropertyInfos(requestContext, properties);
        final ResponseBuilder response = Response.ok("{'status':'ok'}");
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
            System.out.println("1111 processorDetails: " + processorDetails.getProperties() + " getDescriptors: " + processorDetails.getDescriptors());
        } catch (final Exception e) {
            final String message = String.format("Unable to get UpdateAttribute[id=%s] criteria: %s", requestContext.getId(), e);
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
                final String message = String.format("Unable to deserialize existing rules for UpdateAttribute[id=%s]. Deserialization error: %s", requestContext.getId(), iae);
                logger.error(message, iae);
                throw new WebApplicationException(iae, error(message));
            }
        }
        // ensure the parameters isn't null
        if (propertyInfoEntity == null) {
            propertyInfoEntity = new PropertyInfoEntity();
        }

        //add path
        propertyInfoEntity.path = processorDetails.getProperties().get("Allowed Paths");

        return propertyInfoEntity;
    }

    private void savePropertyInfos(final NiFiWebConfigurationRequestContext requestContext, PropertyInfoEntity parameter) {

        Gson gson = new Gson();
        String annotationData = gson.toJson(parameter);
        System.out.println("1111 savePropertyInfos annotation: " + annotationData);
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
