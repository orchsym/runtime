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
package org.apache.nifi.web.puttcpui.api;

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
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.nifi.web.puttcpui.model.PackingInfoCollectionEntity;

/**
 *
 */
@Path("/puttcp")
public class PutTcpResource {

    private static final Logger logger = LoggerFactory.getLogger(PutTcpResource.class);

    @Context
    private ServletContext servletContext;

    @Context
    private HttpServletRequest request;

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/info")
    public Response getPutTcpInfos(
        @QueryParam("processorId") final String processorId) {

        // get the web context
        final NiFiWebConfigurationContext configurationContext = (NiFiWebConfigurationContext) servletContext.getAttribute("nifi-web-configuration-context");

        // build the web context config
        final NiFiWebRequestContext requestContext = getRequestContext(processorId);

        PackingInfoCollectionEntity packingInfos = getPackingInfos(configurationContext, requestContext);
        Gson gson = new Gson();
        String json = gson.toJson(packingInfos);
        final ResponseBuilder response = Response.ok(json);
        return noCache(response).build();
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/info")
    public Response creaPutTcpInfos(
            @Context final UriInfo uriInfo,
            final PackingInfoCollectionEntity infos) {

        // get the web context
        final NiFiWebConfigurationContext configurationContext = (NiFiWebConfigurationContext) servletContext.getAttribute("nifi-web-configuration-context");

        // build the request context
        final NiFiWebConfigurationRequestContext requestContext = getConfigurationRequestContext(infos.processorId, infos.revision, infos.clientId);

        savePutTcpInfos(requestContext, infos);
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

    private PackingInfoCollectionEntity getPackingInfos(final NiFiWebConfigurationContext configurationContext, final NiFiWebRequestContext requestContext) {
        final ComponentDetails processorDetails;

        try {
            // load the processor configuration
            processorDetails = configurationContext.getComponentDetails(requestContext);
        } catch (final Exception e) {
            final String message = String.format("Unable to get PackingInfos[id=%s]: %s", requestContext.getId(), e);
            logger.error(message, e);
            throw new WebApplicationException(e, error(message));
        }

        PackingInfoCollectionEntity packingInfos = null;
        if (processorDetails != null) {
            try {
                String annotation = processorDetails.getAnnotationData();
                Gson gson = new Gson();
                packingInfos = gson.fromJson(annotation, new TypeToken<PackingInfoCollectionEntity>(){}.getType());
            } catch (final IllegalArgumentException iae) {
                final String message = String.format("Unable to deserialize existing PackingInfos for PutTcp[id=%s]. Deserialization error: %s", requestContext.getId(), iae);
                logger.error(message, iae);
                throw new WebApplicationException(iae, error(message));
            }
        }
        // ensure the packingInfos isn't null
        if (packingInfos == null) {
            packingInfos = new PackingInfoCollectionEntity();
        }

        return packingInfos;
    }

        private void savePutTcpInfos(final NiFiWebConfigurationRequestContext requestContext, PackingInfoCollectionEntity parameter) {

        Gson gson = new Gson();
        String annotationData = gson.toJson(parameter);
        // get the web context
        final NiFiWebConfigurationContext configurationContext = (NiFiWebConfigurationContext) servletContext.getAttribute("nifi-web-configuration-context");

        try {
            // save the annotation data
            configurationContext.updateComponent(requestContext, annotationData, null);

        } catch (final Exception e) {
            final String message = String.format("Unable to save PutTcpInfos[id=%s] in: %s", requestContext.getId(), e);
            logger.error(message, e);
        }
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
