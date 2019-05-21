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
package org.apache.nifi.processors.standard;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnRemoved;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.lifecycle.OnStopped;
import org.apache.nifi.apiregistry.ApiRegistryService;
import org.apache.nifi.authentication.APIAuthenticationService;
import org.apache.nifi.components.AllowableValue;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.PropertyValue;
import org.apache.nifi.controller.ControllerServiceLookup;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.BooleanAllowableValues;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.http.HttpContextMap;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.processors.standard.util.HTTPUtils;
import org.apache.nifi.registry.api.APIServicesManager;
import org.apache.nifi.registry.api.ApiInfo;
import org.apache.nifi.ssl.RestrictedSSLContextService;
import org.apache.nifi.ssl.SSLContextService;
import org.apache.nifi.stream.io.StreamUtils;
import org.apache.nifi.util.NiFiProperties;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.web.util.UriTemplate;

@InputRequirement(Requirement.INPUT_FORBIDDEN)
@Tags({ "http", "https", "request", "listen", "ingress", "web service" })
@CapabilityDescription("Starts an HTTP Server and listens for HTTP Requests. For each request, creates a FlowFile and transfers to 'success'. "
        + "This Processor is designed to be used in conjunction with the HandleHttpResponse Processor in order to create a Web Service")
@WritesAttributes({
        @WritesAttribute(attribute = HTTPUtils.HTTP_CONTEXT_ID, description = "An identifier that allows the HandleHttpRequest and HandleHttpResponse "
                + "to coordinate which FlowFile belongs to which HTTP Request/Response."),
        @WritesAttribute(attribute = "mime.type", description = "The MIME Type of the data, according to the HTTP Header \"Content-Type\""),
        @WritesAttribute(attribute = "http.servlet.path", description = "The part of the request URL that is considered the Servlet Path"),
        @WritesAttribute(attribute = "http.context.path", description = "The part of the request URL that is considered to be the Context Path"),
        @WritesAttribute(attribute = "http.method", description = "The HTTP Method that was used for the request, such as GET or POST"),
        @WritesAttribute(attribute = HTTPUtils.HTTP_LOCAL_NAME, description = "IP address/hostname of the server"),
        @WritesAttribute(attribute = HTTPUtils.HTTP_PORT, description = "Listening port of the server"),
        @WritesAttribute(attribute = "http.query.string", description = "The query string portion of the Request URL"),
        @WritesAttribute(attribute = HTTPUtils.HTTP_REMOTE_HOST, description = "The hostname of the requestor"),
        @WritesAttribute(attribute = "http.remote.addr", description = "The hostname:port combination of the requestor"),
        @WritesAttribute(attribute = "http.remote.user", description = "The username of the requestor"),
        @WritesAttribute(attribute = "http.protocol", description = "The protocol used to communicate"), @WritesAttribute(attribute = HTTPUtils.HTTP_REQUEST_URI, description = "The full Request URL"),
        @WritesAttribute(attribute = "http.auth.type", description = "The type of HTTP Authorization used"),
        @WritesAttribute(attribute = "http.principal.name", description = "The name of the authenticated user making the request"),
        @WritesAttribute(attribute = HTTPUtils.HTTP_SSL_CERT, description = "The Distinguished Name of the requestor. This value will not be populated "
                + "unless the Processor is configured to use an SSLContext Service"),
        @WritesAttribute(attribute = "http.issuer.dn", description = "The Distinguished Name of the entity that issued the Subject's certificate. "
                + "This value will not be populated unless the Processor is configured to use an SSLContext Service"),
        @WritesAttribute(attribute = "http.headers.XXX", description = "Each of the HTTP Headers that is received in the request will be added as an "
                + "attribute, prefixed with \"http.headers.\" For example, if the request contains an HTTP Header named \"x-my-header\", then the value "
                + "will be added to an attribute named \"http.headers.x-my-header\"") })
@SeeAlso(value = { HandleHttpResponse.class })
public class HandleHttpRequest extends AbstractProcessor {
    private static final Pattern URL_QUERY_PARAM_DELIMITER = Pattern.compile("&");

    // Allowable values for client auth
    public static final AllowableValue CLIENT_NONE = new AllowableValue("No Authentication", "No Authentication", //
            "Processor will not authenticate clients. Anyone can communicate with this Processor anonymously"); //
    public static final AllowableValue CLIENT_WANT = new AllowableValue("Want Authentication", "Want Authentication", //
            "Processor will try to verify the client but if unable to verify will allow the client to communicate anonymously"); //
    public static final AllowableValue CLIENT_NEED = new AllowableValue("Need Authentication", "Need Authentication", //
            "Processor will reject communications from any client unless the client provides a certificate that is trusted by the TrustStore" //
                    + "specified in the SSL Context Service");

    public static final PropertyDescriptor PORT = new PropertyDescriptor.Builder() //
            .name("Listening Port") //
            .description("The Port to listen on for incoming HTTP requests") //
            .required(true) //
            .addValidator(StandardValidators.createLongValidator(0L, 65535L, true)) //
            .expressionLanguageSupported(ExpressionLanguageScope.NONE) //
            .defaultValue("20001") //
            .build();
    public static final PropertyDescriptor HOSTNAME = new PropertyDescriptor.Builder() //
            .name("Hostname") //
            .description("The Hostname to bind to. If not specified, will bind to all hosts") //
            .required(false) //
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR) //
            .expressionLanguageSupported(ExpressionLanguageScope.NONE) //
            .build();
    public static final PropertyDescriptor HTTP_CONTEXT_MAP = new PropertyDescriptor.Builder() //
            .name("HTTP Context Map") //
            .description("The HTTP Context Map Controller Service to use for caching the HTTP Request Information") //
            .required(true) //
            .identifiesControllerService(HttpContextMap.class) //
            .build();
    public static final PropertyDescriptor SSL_CONTEXT = new PropertyDescriptor.Builder() //
            .name("SSL Context Service") //
            .description("The SSL Context Service to use in order to secure the server. If specified, the server will accept only HTTPS requests; " //
                    + "otherwise, the server will accept only HTTP requests") //
            .required(false) //
            .identifiesControllerService(RestrictedSSLContextService.class) //
            .build();
    public static final PropertyDescriptor URL_CHARACTER_SET = new PropertyDescriptor.Builder() //
            .name("Default URL Character Set") //
            .description("The character set to use for decoding URL parameters if the HTTP Request does not supply one") //
            .required(true) //
            .defaultValue("UTF-8") //
            .addValidator(StandardValidators.CHARACTER_SET_VALIDATOR) //
            .build();
    public static final PropertyDescriptor PATH_REGEX = new PropertyDescriptor.Builder() //
            .name("Allowed Paths") //
            .description("A Regular Expression that specifies the valid HTTP Paths that are allowed in the incoming URL Requests. If this value is " //
                    + "specified and the path of the HTTP Requests does not match this Regular Expression, the Processor will respond with a " //
                    + "404: NotFound") //
            .required(true) //
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR) //
            .expressionLanguageSupported(ExpressionLanguageScope.NONE) //
            .build();
    public static final PropertyDescriptor ALLOW_GET = new PropertyDescriptor.Builder() //
            .name("Allow GET") //
            .description("Allow HTTP GET Method") //
            .required(true) //
            .allowableValues("true", "false") //
            .defaultValue("true") //
            .build();
    public static final PropertyDescriptor ALLOW_POST = new PropertyDescriptor.Builder() //
            .name("Allow POST") //
            .description("Allow HTTP POST Method") //
            .required(true) //
            .allowableValues("true", "false") //
            .defaultValue("true") //
            .build();
    public static final PropertyDescriptor ALLOW_PUT = new PropertyDescriptor.Builder() //
            .name("Allow PUT") //
            .description("Allow HTTP PUT Method") //
            .required(true) //
            .allowableValues("true", "false") //
            .defaultValue("true") //
            .build();
    public static final PropertyDescriptor ALLOW_DELETE = new PropertyDescriptor.Builder() //
            .name("Allow DELETE") //
            .description("Allow HTTP DELETE Method") //
            .required(true) //
            .allowableValues("true", "false") //
            .defaultValue("true") //
            .build();
    public static final PropertyDescriptor ALLOW_HEAD = new PropertyDescriptor.Builder() //
            .name("Allow HEAD") //
            .description("Allow HTTP HEAD Method") //
            .required(true) //
            .allowableValues("true", "false") //
            .defaultValue("false") //
            .build();
    public static final PropertyDescriptor ALLOW_OPTIONS = new PropertyDescriptor.Builder() //
            .name("Allow OPTIONS") //
            .description("Allow HTTP OPTIONS Method") //
            .required(true) //
            .allowableValues("true", "false") //
            .defaultValue("false") //
            .build();
    public static final PropertyDescriptor ADDITIONAL_METHODS = new PropertyDescriptor.Builder() //
            .name("Additional HTTP Methods") //
            .description("A comma-separated list of non-standard HTTP Methods that should be allowed") //
            .required(false) //
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR) //
            .expressionLanguageSupported(ExpressionLanguageScope.NONE) //
            .build();
    public static final PropertyDescriptor CLIENT_AUTH = new PropertyDescriptor.Builder() //
            .name("Client Authentication") //
            .description("Specifies whether or not the Processor should authenticate clients. This value is ignored if the <SSL Context Service> " //
                    + "Property is not specified or the SSL Context provided uses only a KeyStore and not a TrustStore.") //
            .required(true) //
            .allowableValues(CLIENT_NONE, CLIENT_WANT, CLIENT_NEED) //
            .defaultValue(CLIENT_NONE.getValue()) //
            .build();
    public static final PropertyDescriptor CONTAINER_QUEUE_SIZE = new PropertyDescriptor.Builder() //
            .name("container-queue-size") //
            .displayName("Container Queue Size") //
            .description("The size of the queue for Http Request Containers") //
            .required(true) //
            .addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR) //
            .defaultValue("50") //
            .build();
    public static final PropertyDescriptor SUPPORT_API_REGISTRY = new PropertyDescriptor.Builder() //
            .name("allow-api-registry") //
            .displayName("Allow API Registry")//
            .description("Specifies whether or not this Processor should do registry to API Manager for the HTTP Service.") //
            .required(true) //
            .allowableValues(BooleanAllowableValues.list()) //
            .defaultValue(BooleanAllowableValues.FALSE.value()) //
            .addValidator(BooleanAllowableValues.validator()) //
            .build();
    public static final PropertyDescriptor HTTP_API_REGISTRY = new PropertyDescriptor.Builder() //
            .name("HTTP API Registry Service") //
            .description("this service is for api registry, will be deprecated, should try to use <" + SUPPORT_API_REGISTRY.getDisplayName() + "> instead") //
            .required(false) //
            .identifiesControllerService(ApiRegistryService.class) //
            .build();

    public static final PropertyDescriptor HTTP_AUTHENTICATION = new PropertyDescriptor.Builder() //
            .name("HTTP Authentication") //
            .description("this service is for request authentication") //
            .required(false) //
            .identifiesControllerService(APIAuthenticationService.class) //
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder() //
            .name("success") //
            .description("All content that is received is routed to the 'success' relationship") //
            .build();

    private static final List<PropertyDescriptor> propertyDescriptors;

    static {
        List<PropertyDescriptor> descriptors = new ArrayList<>();
        descriptors.add(PORT);
        descriptors.add(HOSTNAME);
        descriptors.add(SSL_CONTEXT);
        descriptors.add(HTTP_CONTEXT_MAP);
        descriptors.add(HTTP_API_REGISTRY);
        descriptors.add(HTTP_AUTHENTICATION);
        descriptors.add(SUPPORT_API_REGISTRY);
        descriptors.add(PATH_REGEX);
        descriptors.add(URL_CHARACTER_SET);
        descriptors.add(ALLOW_GET);
        descriptors.add(ALLOW_POST);
        descriptors.add(ALLOW_PUT);
        descriptors.add(ALLOW_DELETE);
        descriptors.add(ALLOW_HEAD);
        descriptors.add(ALLOW_OPTIONS);
        descriptors.add(ADDITIONAL_METHODS);
        descriptors.add(CLIENT_AUTH);
        descriptors.add(CONTAINER_QUEUE_SIZE);
        propertyDescriptors = Collections.unmodifiableList(descriptors);
    }

    private volatile Server server;
    private AtomicBoolean initialized = new AtomicBoolean(false);
    private volatile BlockingQueue<HttpRequestContainer> containerQueue;

    private volatile boolean loaded = false;
    private volatile boolean hasAPIService = false;
    private volatile boolean supportApiRegistry = false;
    private volatile HttpContextMap httpContextService;
    private Map<String, String> allProperties = new HashMap<>();

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return propertyDescriptors;
    }

    @Override
    public Set<Relationship> getRelationships() {
        return Collections.singleton(REL_SUCCESS);
    }

    @Override
    protected void init(ProcessorInitializationContext context) {
        super.init(context);

        // init all default value
        for (PropertyDescriptor desc : getSupportedPropertyDescriptors()) {
            allProperties.put(desc.getName(), desc.getDefaultValue());
        }
    }

    @Override
    public void onPropertyModified(final PropertyDescriptor descriptor, final String oldValue, final String newValue) {
        // put all properties
        if (newValue == null) { // when delete property
            allProperties.remove(descriptor.getName());
            if (descriptor.equals(SSL_CONTEXT)) {
                // update scheme
                allProperties.put(descriptor.getName(), "null");
            } else if (descriptor.equals(HTTP_API_REGISTRY)) {
                hasAPIService = false;
            } else if (descriptor.equals(HTTP_CONTEXT_MAP)) {
                httpContextService = null;
            }
        } else {// set new value or modify
            if (descriptor.equals(HTTP_API_REGISTRY)) {
                ControllerServiceLookup serviceLookup = getControllerServiceLookup();
                if (newValue != null) {
                    ApiRegistryService apiService = (ApiRegistryService) serviceLookup.getControllerService(newValue);
                    if (apiService != null) {
                        hasAPIService = true;
                    }
                }
            }
            if (descriptor.equals(SUPPORT_API_REGISTRY)) {
                supportApiRegistry = Boolean.parseBoolean(newValue);
            }

            if (descriptor.equals(HTTP_CONTEXT_MAP)) {
                ControllerServiceLookup serviceLookup = getControllerServiceLookup();
                if (newValue != null) {
                    httpContextService = (HttpContextMap) serviceLookup.getControllerService(newValue);
                }
            }
            allProperties.put(descriptor.getName(), newValue);
        }

        // if not loaded, but when set group id, will update, or have loaded, but modify the properties.
        if (loaded || !loaded && descriptor.getName().equals(NiFiProperties.GROUP_ID)) { // avoid updating too often
            updateApiInfoFromService(ApiInfo.State.init);
            loaded = true;
        }
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) throws Exception {
        initialized.set(false);

        final PropertyValue apiServiceProp = context.getProperty(HTTP_API_REGISTRY);
        hasAPIService = apiServiceProp != null ? apiServiceProp.asControllerService() != null : Boolean.FALSE;
        supportApiRegistry = context.getProperty(SUPPORT_API_REGISTRY).asBoolean();

        updateApiInfoFromService(context, ApiInfo.State.running);
    }

    @OnStopped
    public void shutdown() throws Exception {
        updateApiInfoFromService(ApiInfo.State.stopped);

        if (server != null) {
            getLogger().debug("Shutting down server");
            server.stop();
            server.destroy();
            server.join();
            getLogger().info("Shut down {}", new Object[] { server });
        }
    }

    @OnRemoved
    public void onRemoved(ProcessContext processContext) {
        unregisterApiInfoFromService();
    }

    private synchronized void initializeServer(final ProcessContext context) throws Exception {
        if (initialized.get()) {
            return;
        }
        this.containerQueue = new LinkedBlockingQueue<>(context.getProperty(CONTAINER_QUEUE_SIZE).asInteger());
        final String host = context.getProperty(HOSTNAME).getValue();
        final int port = getListeningPort(context);
        final HttpContextMap httpContextMap = context.getProperty(HTTP_CONTEXT_MAP).asControllerService(HttpContextMap.class);
        final long requestTimeout = httpContextMap.getRequestTimeout(TimeUnit.MILLISECONDS);

        final SslContextFactory sslFactory = createSslFactory(context);
        final Server server = new Server(port);

        // create the http configuration
        final HttpConfiguration httpConfiguration = new HttpConfiguration();
        if (sslFactory == null) {
            // create the connector
            final ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpConfiguration));

            // set host and port
            if (StringUtils.isNotBlank(host)) {
                http.setHost(host);
            }
            http.setPort(port);

            // If request timeout is longer than default Idle Timeout, then increase Idle Timeout as well.
            http.setIdleTimeout(Math.max(http.getIdleTimeout(), requestTimeout));

            // add this connector
            server.setConnectors(new Connector[] { http });
        } else {
            // add some secure config
            final HttpConfiguration httpsConfiguration = new HttpConfiguration(httpConfiguration);
            httpsConfiguration.setSecureScheme("https");
            httpsConfiguration.setSecurePort(port);
            httpsConfiguration.addCustomizer(new SecureRequestCustomizer());

            // build the connector
            final ServerConnector https = new ServerConnector(server, new SslConnectionFactory(sslFactory, "http/1.1"), new HttpConnectionFactory(httpsConfiguration));

            // set host and port
            if (StringUtils.isNotBlank(host)) {
                https.setHost(host);
            }
            https.setPort(port);

            // If request timeout is longer than default Idle Timeout, then increase Idle Timeout as well.
            https.setIdleTimeout(Math.max(https.getIdleTimeout(), requestTimeout));

            // add this connector
            server.setConnectors(new Connector[] { https });
        }

        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
                handleHttp(context, target, baseRequest, request, response);
            }
        });

        this.server = server;
        server.start();

        getLogger().info("Server started and listening on port " + getPort());

        initialized.set(true);
    }

    protected int getPort() {
        for (final Connector connector : server.getConnectors()) {
            if (connector instanceof ServerConnector) {
                return ((ServerConnector) connector).getLocalPort();
            }
        }

        throw new IllegalStateException("Server is not listening on any ports");
    }

    protected int getListeningPort(final ProcessContext context) {
        final int port = context.getProperty(PORT).asInteger();
        return port;
    }

    protected int getRequestQueueSize() {
        return containerQueue.size();
    }

    protected SslContextFactory createSslFactory(final ProcessContext context) {
        final SSLContextService sslService = context.getProperty(SSL_CONTEXT).asControllerService(SSLContextService.class);
        final String clientAuthValue = context.getProperty(CLIENT_AUTH).getValue();
        final boolean need;
        final boolean want;
        if (CLIENT_NEED.equals(clientAuthValue)) {
            need = true;
            want = false;
        } else if (CLIENT_WANT.equals(clientAuthValue)) {
            need = false;
            want = true;
        } else {
            need = false;
            want = false;
        }

        final SslContextFactory sslFactory = (sslService == null) ? null : createSslFactory(sslService, need, want);
        return sslFactory;
    }

    protected SslContextFactory createSslFactory(final SSLContextService sslService, final boolean needClientAuth, final boolean wantClientAuth) {
        final SslContextFactory sslFactory = new SslContextFactory();

        sslFactory.setNeedClientAuth(needClientAuth);
        sslFactory.setWantClientAuth(wantClientAuth);

        sslFactory.setProtocol(sslService.getSslAlgorithm());

        if (sslService.isKeyStoreConfigured()) {
            sslFactory.setKeyStorePath(sslService.getKeyStoreFile());
            sslFactory.setKeyStorePassword(sslService.getKeyStorePassword());
            sslFactory.setKeyStoreType(sslService.getKeyStoreType());
        }

        if (sslService.isTrustStoreConfigured()) {
            sslFactory.setTrustStorePath(sslService.getTrustStoreFile());
            sslFactory.setTrustStorePassword(sslService.getTrustStorePassword());
            sslFactory.setTrustStoreType(sslService.getTrustStoreType());
        }

        return sslFactory;
    }

    protected Set<String> getAllowedMethods(final ProcessContext context) {
        final Set<String> allowedMethods = new HashSet<>();
        if (context.getProperty(ALLOW_GET).asBoolean()) {
            allowedMethods.add(HTTPUtils.METHOD_GET);
        }
        if (context.getProperty(ALLOW_POST).asBoolean()) {
            allowedMethods.add(HTTPUtils.METHOD_POST);
        }
        if (context.getProperty(ALLOW_PUT).asBoolean()) {
            allowedMethods.add(HTTPUtils.METHOD_PUT);
        }
        if (context.getProperty(ALLOW_DELETE).asBoolean()) {
            allowedMethods.add(HTTPUtils.METHOD_DELETE);
        }
        if (context.getProperty(ALLOW_HEAD).asBoolean()) {
            allowedMethods.add(HTTPUtils.METHOD_HEAD);
        }
        if (context.getProperty(ALLOW_OPTIONS).asBoolean()) {
            allowedMethods.add(HTTPUtils.METHOD_OPTIONS);
        }

        final String additionalMethods = context.getProperty(ADDITIONAL_METHODS).getValue();
        if (additionalMethods != null) {
            for (final String additionalMethod : additionalMethods.split(",")) {
                final String trimmed = additionalMethod.trim();
                if (!trimmed.isEmpty()) {
                    allowedMethods.add(trimmed.toUpperCase());
                }
            }
        }
        return allowedMethods;
    }

    protected Pattern getPathPattern(final ProcessContext context) {
        final String pathRegex = context.getProperty(PATH_REGEX).getValue();
        final Pattern pathPattern = (pathRegex == null) ? null : Pattern.compile(pathRegex);
        return pathPattern;
    }

    protected UriTemplate getUriTemplate(final ProcessContext context) {
        final String pathRegex = context.getProperty(PATH_REGEX).getValue();
        final UriTemplate pathTemplate = (pathRegex == null) ? null : new UriTemplate(pathRegex);
        return pathTemplate;
    }

    protected void handleHttp(final ProcessContext context, final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {
        final Set<String> allowedMethods = getAllowedMethods(context);
        final UriTemplate pathTemplate = getUriTemplate(context);

        final String requestUri = request.getRequestURI();
        if (!allowedMethods.contains(request.getMethod().toUpperCase())) {
            getLogger().info("Sending back METHOD_NOT_ALLOWED response to {}; method was {}; request URI was {}", new Object[] { request.getRemoteAddr(), request.getMethod(), requestUri });
            response.sendError(Status.METHOD_NOT_ALLOWED.getStatusCode());
            return;
        }

        if (pathTemplate != null) {
            final URI uri;
            try {
                uri = new URI(requestUri);
            } catch (final URISyntaxException e) {
                throw new ServletException(e);
            }
            if (!pathTemplate.matches(uri.getPath())) {

                // check if match with pattern
                final Pattern pathPattern = getPathPattern(context);
                if (pathPattern != null && !pathPattern.matcher(uri.getPath()).matches()) {
                    response.sendError(Status.NOT_FOUND.getStatusCode());
                    getLogger().info("Sending back NOT_FOUND response to {}; request was {} {}", new Object[] { request.getRemoteAddr(), request.getMethod(), requestUri });
                    return;
                }
            }
        }

        // If destination queues full, send back a 503: Service Unavailable.
        if (context.getAvailableRelationships().isEmpty()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        handleAfter(context, target, baseRequest, request, response);
    }

    protected void handleAfter(final ProcessContext context, final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        final String requestUri = request.getRequestURI();
        final HttpContextMap httpContextMap = context.getProperty(HTTP_CONTEXT_MAP).asControllerService(HttpContextMap.class);
        final long requestTimeout = httpContextMap.getRequestTimeout(TimeUnit.MILLISECONDS);

        // Right now, that information, though, is only in the ProcessSession, not the ProcessContext,
        // so it is not known to us. Should see if it can be added to the ProcessContext.
        final AsyncContext async = baseRequest.startAsync();
        async.setTimeout(requestTimeout);
        final boolean added = containerQueue.offer(new HttpRequestContainer(request, response, async));

        if (added) {
            getLogger().debug("Added Http Request to queue for {} {} from {}", new Object[] { request.getMethod(), requestUri, request.getRemoteAddr() });
        } else {
            getLogger().info("Sending back a SERVICE_UNAVAILABLE response to {}; request was {} {}", new Object[] { request.getRemoteAddr(), request.getMethod(), request.getRemoteAddr() });

            response.sendError(Status.SERVICE_UNAVAILABLE.getStatusCode());
            response.flushBuffer();
            async.complete();
        }
    }

    //验证请求信息是否合法
    private boolean authenticateRequestInfo(APIAuthenticationService authenService, HttpServletRequest request, HttpRequestContainer container) {
        //验证该地址是否可以通过
        String ipAddress = request.getRemoteAddr();
        if (!authenService.authenticateAddress(ipAddress)) {
            //该地址不通过
            getLogger().warn("the address {} is failed to pass!", new Object[] { ipAddress });
            sendBadRequest(container, request, Status.UNAUTHORIZED.getStatusCode());
            return false;
        }
        if (authenService.shouldAuthenticateAuthorizationInfo()) {
            //验证授权信息
            String authorizationInfo = request.getHeader("Authorization");
            try {
                if (authorizationInfo == null || !authenService.authenticateAuthorizationInfo(request.getMethod(), authorizationInfo)) {
                    getLogger().warn("the request {} is failed to authorize!", new Object[] { request });
                    sendBadRequest(container, request, Status.UNAUTHORIZED.getStatusCode());
                    return false;
                }
            } catch (final Exception e) {
                getLogger().warn("the request {} is failed to authorize!", new Object[] { request });
                sendBadRequest(container, request, Status.UNAUTHORIZED.getStatusCode());
                return false;
            } 
        }
        return true;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        try {
            if (!initialized.get()) {
                initializeServer(context);
            }
        } catch (Exception e) {
            context.yield();

            try {
                // shutdown to release any resources allocated during the failed initialization
                shutdown();
            } catch (final Exception shutdownException) {
                getLogger().debug("Failed to shutdown following a failed initialization: " + shutdownException);
            }

            throw new ProcessException("Failed to initialize the server", e);
        }

        HttpRequestContainer container;
        try {
            container = containerQueue.poll(2, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e1) {
            Thread.currentThread().interrupt();
            return;
        }

        if (container == null) {
            return;
        }

        final long start = System.nanoTime();
        final HttpServletRequest request = container.getRequest();

        final APIAuthenticationService authenService = context.getProperty(HTTP_AUTHENTICATION).asControllerService(APIAuthenticationService.class);
        if (authenService != null && !authenticateRequestInfo(authenService, request, container)) {
            return;
        }
        
        final Map<String, String> attributes = new HashMap<>();
        putAttribute(attributes, CoreAttributes.MIME_TYPE.key(), request.getContentType()); // set first, maybe will be modify in process

        FlowFile flowFile = session.create();
        try {
            flowFile = processRequest(context, session, request, flowFile, attributes);
        } catch (final IOException e) {
            // There may be many reasons which can produce an IOException on the HTTP stream and in some of them, eg.
            // bad requests, the connection to the client is not closed. In order to address also these cases, we try
            // and answer with a BAD_REQUEST, which lets the client know that the request has not been correctly
            // processed and makes it aware that the connection can be closed.
            getLogger().error("Failed to receive content from HTTP Request from {} due to {}", new Object[] { request.getRemoteAddr(), e });
            session.remove(flowFile);
            sendBadRequest(container, request, Status.BAD_REQUEST.getStatusCode());
            return;
        }

        final String charset = request.getCharacterEncoding() == null ? context.getProperty(URL_CHARACTER_SET).getValue() : request.getCharacterEncoding();

        final String contextIdentifier = UUID.randomUUID().toString();

        try {
            putAttribute(attributes, HTTPUtils.HTTP_CONTEXT_ID, contextIdentifier);
            putAttribute(attributes, "http.servlet.path", request.getServletPath());
            putAttribute(attributes, "http.context.path", request.getContextPath());
            putAttribute(attributes, "http.method", request.getMethod());
            putAttribute(attributes, "http.local.addr", request.getLocalAddr());
            putAttribute(attributes, HTTPUtils.HTTP_LOCAL_NAME, request.getLocalName());
            final String queryString = request.getQueryString();
            if (queryString != null) {
                putAttribute(attributes, "http.query.string", URLDecoder.decode(queryString, charset));
            }
            putAttribute(attributes, HTTPUtils.HTTP_REMOTE_HOST, request.getRemoteHost());
            putAttribute(attributes, "http.remote.addr", request.getRemoteAddr());
            putAttribute(attributes, "http.remote.user", request.getRemoteUser());
            putAttribute(attributes, "http.protocol", request.getProtocol());
            putAttribute(attributes, HTTPUtils.HTTP_REQUEST_URI, request.getRequestURI());
            putAttribute(attributes, "http.request.url", request.getRequestURL().toString());
            putAttribute(attributes, "http.auth.type", request.getAuthType());

            putAttribute(attributes, "http.requested.session.id", request.getRequestedSessionId());
            final DispatcherType dispatcherType = request.getDispatcherType();
            if (dispatcherType != null) {
                putAttribute(attributes, "http.dispatcher.type", dispatcherType.name());
            }
            putAttribute(attributes, "http.character.encoding", request.getCharacterEncoding());
            putAttribute(attributes, "http.locale", request.getLocale());
            putAttribute(attributes, "http.server.name", request.getServerName());
            putAttribute(attributes, HTTPUtils.HTTP_PORT, request.getServerPort());

            final Enumeration<String> paramEnumeration = request.getParameterNames();
            while (paramEnumeration.hasMoreElements()) {
                final String paramName = paramEnumeration.nextElement();
                final String value = request.getParameter(paramName);
                attributes.put("http.param." + paramName, value);
            }

            // set path param
            final UriTemplate pathTemplate = getUriTemplate(context);
            if (pathTemplate != null) {
                URI uri = null;
                try {
                    uri = new URI(request.getRequestURI());
                } catch (final URISyntaxException e) {
                    getLogger().warn("Failed to parse URI {} due to {}", new Object[] { request.getRequestURI(), e });
                }
                Map<String, String> pathParamMap = pathTemplate.match(uri.getPath());
                for (String key : pathParamMap.keySet()) {
                    attributes.put("http.path.param." + key, pathParamMap.get(key));
                }
            }

            final Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (final Cookie cookie : cookies) {
                    final String name = cookie.getName();
                    final String cookiePrefix = "http.cookie." + name + ".";
                    attributes.put(cookiePrefix + "value", cookie.getValue());
                    attributes.put(cookiePrefix + "domain", cookie.getDomain());
                    attributes.put(cookiePrefix + "path", cookie.getPath());
                    attributes.put(cookiePrefix + "max.age", String.valueOf(cookie.getMaxAge()));
                    attributes.put(cookiePrefix + "version", String.valueOf(cookie.getVersion()));
                    attributes.put(cookiePrefix + "secure", String.valueOf(cookie.getSecure()));
                }
            }

            if (queryString != null) {
                final String[] params = URL_QUERY_PARAM_DELIMITER.split(queryString);
                for (final String keyValueString : params) {
                    final int indexOf = keyValueString.indexOf("=");
                    if (indexOf < 0) {
                        // no =, then it's just a key with no value
                        attributes.put("http.query.param." + URLDecoder.decode(keyValueString, charset), "");
                    } else {
                        final String key = keyValueString.substring(0, indexOf);
                        final String value;

                        if (indexOf == keyValueString.length() - 1) {
                            value = "";
                        } else {
                            value = keyValueString.substring(indexOf + 1);
                        }

                        attributes.put("http.query.param." + URLDecoder.decode(key, charset), URLDecoder.decode(value, charset));
                    }
                }
            }
        } catch (final UnsupportedEncodingException uee) {
            throw new ProcessException("Invalid character encoding", uee); // won't happen because charset has been validated
        }

        final Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            final String headerName = headerNames.nextElement();
            final String headerValue = request.getHeader(headerName);
            putAttribute(attributes, "http.headers." + headerName, headerValue);
        }

        final Principal principal = request.getUserPrincipal();
        if (principal != null) {
            putAttribute(attributes, "http.principal.name", principal.getName());
        }

        final X509Certificate certs[] = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        final String subjectDn;
        if (certs != null && certs.length > 0) {
            final X509Certificate cert = certs[0];
            subjectDn = cert.getSubjectDN().getName();
            final String issuerDn = cert.getIssuerDN().getName();

            putAttribute(attributes, HTTPUtils.HTTP_SSL_CERT, subjectDn);
            putAttribute(attributes, "http.issuer.dn", issuerDn);
        } else {
            subjectDn = null;
        }

        flowFile = session.putAllAttributes(flowFile, attributes);

        final HttpContextMap contextMap = context.getProperty(HTTP_CONTEXT_MAP).asControllerService(HttpContextMap.class);
        Map<String, Object> additions = getContextAddtions(context, session, request);
        final boolean registered = contextMap.register(contextIdentifier, request, container.getResponse(), container.getContext(), additions);

        if (!registered) {
            getLogger().warn("Received request from {} but could not process it because too many requests are already outstanding; responding with SERVICE_UNAVAILABLE",
                    new Object[] { request.getRemoteAddr() });

            try {
                container.getResponse().setStatus(Status.SERVICE_UNAVAILABLE.getStatusCode());
                container.getResponse().flushBuffer();
                container.getContext().complete();
            } catch (final Exception e) {
                getLogger().warn("Failed to respond with SERVICE_UNAVAILABLE message to {} due to {}", new Object[] { request.getRemoteAddr(), e });
            }

            session.remove(flowFile);
            return;
        }

        final long receiveMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        session.getProvenanceReporter().receive(flowFile, HTTPUtils.getURI(attributes), "Received from " + request.getRemoteAddr() + (subjectDn == null ? "" : " with DN=" + subjectDn), receiveMillis);
        session.transfer(flowFile, REL_SUCCESS);
        getLogger().info("Transferring {} to 'success'; received from {}", new Object[] { flowFile, request.getRemoteAddr() });
    }

    private void putAttribute(final Map<String, String> map, final String key, final Object value) {
        if (value == null) {
            return;
        }

        putAttribute(map, key, value.toString());
    }

    private void putAttribute(final Map<String, String> map, final String key, final String value) {
        if (value == null) {
            return;
        }

        map.put(key, value);
    }

    protected FlowFile processRequest(final ProcessContext context, final ProcessSession session, final HttpServletRequest request, final FlowFile source, final Map<String, String> attributes)
            throws IOException {
        try (OutputStream flowFileOut = session.write(source)) {
            StreamUtils.copy(request.getInputStream(), flowFileOut);
        }
        return source;
    }

    protected Map<String, Object> getContextAddtions(final ProcessContext context, final ProcessSession session, final HttpServletRequest request) {
        return Collections.emptyMap();
    }

    private static class HttpRequestContainer {

        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final AsyncContext context;

        public HttpRequestContainer(final HttpServletRequest request, final HttpServletResponse response, final AsyncContext async) {
            this.request = request;
            this.response = response;
            this.context = async;
        }

        public HttpServletRequest getRequest() {
            return request;
        }

        public HttpServletResponse getResponse() {
            return response;
        }

        public AsyncContext getContext() {
            return context;
        }
    }

    private void sendBadRequest(HttpRequestContainer container, HttpServletRequest request, int code) {
        try {
            HttpServletResponse response = container.getResponse();
            response.sendError(code);
            response.flushBuffer();
            container.getContext().complete();
        } catch (final IOException ioe) {
            getLogger().warn("Failed to send HTTP response to {} due to {}", new Object[] { request.getRemoteAddr(), ioe });
        }
    }

    private void updateApiInfoFromService(final ProcessContext context, ApiInfo.State state) {
        if (hasAPIService || supportApiRegistry) {
            final String groupId = allProperties.get(NiFiProperties.GROUP_ID);
            final HttpContextMap httpContextMap = context.getProperty(HTTP_CONTEXT_MAP).asControllerService(HttpContextMap.class);
            final ApiInfo apiInfo = getApiInfo(context.getName(), allProperties, state, groupId, httpContextMap);
            APIServicesManager.getInstance().register(apiInfo);
        } else {
            unregisterApiInfoFromService();
        }
    }

    private void updateApiInfoFromService(ApiInfo.State state) {
        if (hasAPIService || supportApiRegistry) {
            final ApiInfo apiInfo = getApiInfo(null, allProperties, state, null, httpContextService);
            APIServicesManager.getInstance().register(apiInfo);
        } else {
            unregisterApiInfoFromService();
        }
    }

    private void unregisterApiInfoFromService() {
        APIServicesManager.getInstance().unregister(this.getIdentifier());
    }

    private ApiInfo getApiInfo(String name, Map<String, String> properties, ApiInfo.State state, String groupId, HttpContextMap httpContextMap) {
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.id = this.getIdentifier();

        // parameters
        for (Entry<String, String> entry : properties.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            if (key.equals(HOSTNAME.getName())) {
                apiInfo.host = value;
            } else if (key.equals(PORT.getName())) {
                if (StringUtils.isBlank(value)) {
                    apiInfo.port = Integer.parseInt(PORT.getDefaultValue());
                } else {
                    apiInfo.port = Integer.parseInt(value);
                }
            } else if (key.equals(PATH_REGEX.getName())) {
                apiInfo.path = value;
            } else if (key.equals(URL_CHARACTER_SET.getName())) {
                if (StringUtils.isBlank(value)) {// if don't change the value, will be default and value is null
                    apiInfo.charset = URL_CHARACTER_SET.getDefaultValue();
                } else {
                    apiInfo.charset = value;
                }
            } else if (key.equals(ALLOW_GET.getName())) {
                apiInfo.allowGet = Boolean.valueOf(value);
            } else if (key.equals(ALLOW_POST.getName())) {
                apiInfo.allowPost = Boolean.valueOf(value);
            } else if (key.equals(ALLOW_PUT.getName())) {
                apiInfo.allowPut = Boolean.valueOf(value);
            } else if (key.equals(ALLOW_DELETE.getName())) {
                apiInfo.allowDelete = Boolean.valueOf(value);
            } else if (key.equals(ALLOW_HEAD.getName())) {
                apiInfo.allowHead = Boolean.valueOf(value);
            } else if (key.equals(ALLOW_OPTIONS.getName())) {
                apiInfo.allowOptions = Boolean.valueOf(value);
            } else if (key.equals(SSL_CONTEXT.getName())) {
                if (StringUtils.isBlank(value) || value.equals("null")) {
                    apiInfo.scheme = "http";
                } else {
                    apiInfo.scheme = "https";
                }
            } else if (key.equals(NiFiProperties.GROUP_ID)) {
                apiInfo.groupID = value;
            } else if (key.equals(HttpContextMap.REQUEST_TIMEOUT)) {
                if (value != null && !value.isEmpty())
                    apiInfo.requestTimeout = Long.parseLong(value);
            }

        }

        // if set, use the provided one always.
        if (name != null)
            apiInfo.name = name;
        if (state != null)
            apiInfo.state = state.name();

        if (groupId != null)
            apiInfo.groupID = groupId;

        if (httpContextMap != null) {
            apiInfo.controllerServiceId = httpContextMap.getIdentifier();

            ControllerServiceLookup serviceLookup = getControllerServiceLookup();
            if (serviceLookup != null && serviceLookup.isControllerServiceEnabled(httpContextMap)) { // if set, always get from context map
                apiInfo.requestTimeout = httpContextMap.getRequestTimeout(TimeUnit.MILLISECONDS);
            }
        } else {
            apiInfo.controllerServiceId = null;
        }

        return apiInfo;
    }

}
