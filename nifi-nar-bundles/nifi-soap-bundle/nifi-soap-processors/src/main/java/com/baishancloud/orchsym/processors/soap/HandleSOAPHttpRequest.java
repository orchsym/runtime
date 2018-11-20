package com.baishancloud.orchsym.processors.soap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPModelBuilder;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.description.AxisDescription;
import org.apache.axis2.util.XMLUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.processors.standard.HandleHttpRequest;
import org.apache.nifi.processors.standard.util.HTTPUtils;
import org.apache.nifi.ssl.SSLContextService;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;

import com.baishancloud.orchsym.processors.soap.i18n.Messages;
import com.baishancloud.orchsym.processors.soap.model.EEnvelopeOptions;
import com.baishancloud.orchsym.processors.soap.model.EWSDLOptions;
import com.baishancloud.orchsym.processors.soap.service.WSDLDoc;
import com.baishancloud.orchsym.processors.soap.service.get.HandleGetService;
import com.baishancloud.orchsym.processors.soap.service.load.WSDLLoadService;
import com.baishancloud.orchsym.processors.soap.service.post.HandleOperationValidateService;
import com.baishancloud.orchsym.processors.soap.util.FormatUtil;

/**
 * @author GU Guoqiang
 *
 */
@InputRequirement(Requirement.INPUT_FORBIDDEN)
@Marks(categories={"网络/网络通信"}, createdDate="2018-09-07")
@Tags({ "SOAP", "Request", "http", "https", "Web Service", "WSDL" })
@CapabilityDescription("Starts an HTTP Server and listens for SOAP Requests. For each request, creates a FlowFile and transfers to 'success'. "
        + "This Processor is designed to be used in conjunction with the HandleSOAPHttpResponse Processor in order to create a Web Service")
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
@SeeAlso(value = { HandleSOAPHttpResponse.class, HandleHttpRequest.class })
public class HandleSOAPHttpRequest extends HandleHttpRequest {

    public static final PropertyDescriptor SOAP_PORT = new PropertyDescriptor.Builder()//
            .name("soap-listening-port")//$NON-NLS-1$
            .displayName(HandleHttpRequest.PORT.getName())//
            .description("The Port to listen on for incoming SOAP HTTP requests")//
            .required(true)//
            .addValidator(HandleHttpRequest.PORT.getValidators().get(0))//
            .expressionLanguageSupported(HandleHttpRequest.PORT.getExpressionLanguageScope())//
            .defaultValue("7001")//$NON-NLS-1$
            .build();

    public static final PropertyDescriptor SOAP_PATH_REGEX = new PropertyDescriptor.Builder()//
            .name("soap-paths")//$NON-NLS-1$
            .displayName(HandleHttpRequest.PATH_REGEX.getName())//
            .description(HandleHttpRequest.PATH_REGEX.getDescription())//
            .required(true)//
            .addValidator(HandleHttpRequest.PATH_REGEX.getValidators().get(0))//
            .expressionLanguageSupported(HandleHttpRequest.PATH_REGEX.getExpressionLanguageScope())//
            .defaultValue("/orchsym/soap")//$NON-NLS-1$
            .build();

    public static final PropertyDescriptor SOAP_WSDL_OPTIONS = new PropertyDescriptor.Builder()//
            .name("soap-wsdl-options")//$NON-NLS-1$
            .displayName("WSDL Defination Options")//
            .description("The options of WSDL defination, support local wsdl file, remote URL and custom wsdl contents")//
            .required(true)//
            .allowableValues(EWSDLOptions.getAll()).defaultValue(EWSDLOptions.URI.getValue())//
            .build();

    public static final PropertyDescriptor SOAP_WSDL_URI = new PropertyDescriptor.Builder()//
            .name("soap-wsdl-uri")//$NON-NLS-1$
            .displayName("WSDL URI")//
            .description("The value can be local file path and remote URL or such")//
            .required(false)//
            .addValidator(StandardValidators.URI_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    public static final PropertyDescriptor SOAP_WSDL_CONTENTS = new PropertyDescriptor.Builder()//
            .name("soap-wsdl-contents")//$NON-NLS-1$
            .displayName("WSDL custom contents")//
            .description("The value must be valid full contents of WSDL")//
            .required(false)//
            .addValidator(Validator.VALID)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    public static final PropertyDescriptor SOAP_ENVELOPE_OPTIONS = new PropertyDescriptor.Builder()//
            .name("soap-envelope-options")//$NON-NLS-1$
            .displayName("Envelope Options")//
            .description("The options of the SOAP request envelope to deal with")//
            .required(true)//
            .addValidator(Validator.VALID)//
            .allowableValues(EEnvelopeOptions.getAll()).defaultValue(EEnvelopeOptions.RAW.getValue())//
            .build();

    private List<PropertyDescriptor> descriptors;

    private volatile WSDLDoc wsdlDoc;
    private volatile SOAPEnvelope soapEnvelope;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(SOAP_PORT);
        descriptors.add(SOAP_PATH_REGEX);
        descriptors.add(HOSTNAME);
        descriptors.add(HTTP_CONTEXT_MAP);
        descriptors.add(SOAP_WSDL_OPTIONS);
        descriptors.add(SOAP_WSDL_URI);
        descriptors.add(SOAP_WSDL_CONTENTS);
        descriptors.add(SSL_CONTEXT);
        descriptors.add(URL_CHARACTER_SET);
        descriptors.add(CONTAINER_QUEUE_SIZE);
        descriptors.add(SOAP_ENVELOPE_OPTIONS);
        this.descriptors = Collections.unmodifiableList(descriptors);
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @OnScheduled
    @Override
    public void onScheduled(final ProcessContext context) throws Exception {
        super.onScheduled(context);
        soapEnvelope = null;

        final WSDLLoadService loadService = new WSDLLoadService();

        final String wsdlOption = context.getProperty(SOAP_WSDL_OPTIONS).getValue();
        if (EWSDLOptions.URI.getValue().equals(wsdlOption)) {
            final String wsdlUri = context.getProperty(SOAP_WSDL_URI).evaluateAttributeExpressions().getValue();
            if (StringUtils.isBlank(wsdlUri)) {
                throw new IllegalArgumentException(Messages.getString("HandleSOAPHttpRequest.missingUriMessage")); //$NON-NLS-1$
            }
            wsdlDoc = loadService.populateURIDoc(wsdlUri);
        } else if (EWSDLOptions.CONTENTS.getValue().equals(wsdlOption)) {
            final String wsdlContents = context.getProperty(SOAP_WSDL_CONTENTS).evaluateAttributeExpressions().getValue();
            if (StringUtils.isBlank(wsdlContents)) {
                throw new IllegalArgumentException(Messages.getString("HandleSOAPHttpRequest.emptyContentsMessage")); //$NON-NLS-1$
            }
            wsdlDoc = loadService.populateContentDoc(wsdlContents);
        }
        if (wsdlDoc == null || !wsdlDoc.valid()) {
            throw new IllegalArgumentException(Messages.getString("HandleSOAPHttpRequest.emptySoapServiceMessage")); //$NON-NLS-1$
        }

    }

    @Override
    public int getPort() {
        return super.getPort();
    }

    @Override
    protected int getListeningPort(ProcessContext context) {
        final int port = context.getProperty(SOAP_PORT).asInteger();
        return port;
    }

    @Override
    protected Pattern getPathPattern(ProcessContext context) {
        final String pathRegex = context.getProperty(SOAP_PATH_REGEX).getValue();
        final Pattern pathPattern = (pathRegex == null) ? null : Pattern.compile(pathRegex);
        return pathPattern;
    }

    @Override
    protected SslContextFactory createSslFactory(ProcessContext context) {
        final SSLContextService sslService = context.getProperty(SSL_CONTEXT).asControllerService(SSLContextService.class);
        if (sslService != null) {
            return createSslFactory(sslService, false, false);
        }
        return null;
    }

    @Override
    protected Set<String> getAllowedMethods(ProcessContext context) {
        final Set<String> allowedMethods = new HashSet<>();
        allowedMethods.add(HTTPUtils.METHOD_GET);
        allowedMethods.add(HTTPUtils.METHOD_POST);
        return allowedMethods;
    }

    @Override
    protected void handleHttp(ProcessContext context, String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        final Set<String> allowedMethods = getAllowedMethods(context);
        final String requestUri = request.getRequestURI();
        if (!allowedMethods.contains(request.getMethod().toUpperCase())) {
            getLogger().info("Sending back METHOD_NOT_ALLOWED response to {}; method was {}; request URI was {}", new Object[] { request.getRemoteAddr(), request.getMethod(), requestUri }); //$NON-NLS-1$
            response.sendError(Status.METHOD_NOT_ALLOWED.getStatusCode());
            return;
        }

        final Pattern pathPattern = getPathPattern(context);
        if (pathPattern != null) {
            final URI uri;
            try {
                uri = new URI(requestUri);
            } catch (final URISyntaxException e) {
                throw new ServletException(e);
            }
            String path = uri.getPath();
            if (!path.endsWith("/")) { //$NON-NLS-1$
                path += '/';
            }

            String pattern = pathPattern.pattern();
            if (!pattern.endsWith("/")) { //$NON-NLS-1$
                pattern += '/';
            }
            if (!path.startsWith(pattern) // same prefix
                    && !pathPattern.matcher(uri.getPath()).matches()) {
                response.sendError(Status.NOT_FOUND.getStatusCode());
                getLogger().info("Sending back NOT_FOUND response to {}; request was {} {}", new Object[] { request.getRemoteAddr(), request.getMethod(), requestUri }); //$NON-NLS-1$
                return;
            }
        }

        // If destination queues full, send back a 503: Service Unavailable.
        if (context.getAvailableRelationships().isEmpty()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        final HandleGetService handleParamGetService = new HandleGetService(wsdlDoc);
        if (handleParamGetService.handle(request, response)) {
            return;
        }

        try {
            readEnvelope(request);
        } catch (Exception e) {
            // invalid SOAPEnvelope
            response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, e.getMessage());
            return;
        }

        final HandleOperationValidateService handleOperationValidateService = new HandleOperationValidateService(wsdlDoc) {

            @Override
            protected boolean checkInputMessage(HttpServletRequest request, HttpServletResponse response, AxisDescription inMessage) throws IOException {
                if (soapEnvelope == null) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, Messages.getString("HandleSOAPHttpRequest.invalidBodyMessage")); //$NON-NLS-1$
                    return true;
                }
                final SOAPBody body = soapEnvelope.getBody();
                final OMElement firstElement = body.getFirstElement();
                if (inMessage == null) { // no input?
                    if (firstElement != null) { // have value, not match
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, Messages.getString("HandleSOAPHttpRequest.notMatchedMessages")); //$NON-NLS-1$
                        return true;
                    }
                } else {
                    if (firstElement == null) { // no value, not match
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, Messages.getString("HandleSOAPHttpRequest.requiredValuesMessage")); //$NON-NLS-1$
                        return true;
                    }
                    // final Iterator bodyChildElements = body.getChildElements();
                    // final Iterator<? extends AxisDescription> wsdlInMessage = inMessage.getChildren();
                    //
                    // TODO, Validation
                }

                return false;
            }

        };
        if (handleOperationValidateService.handle(request, response)) {
            return;
        }

        handleAfter(context, target, baseRequest, request, response);
    }

    private void readEnvelope(HttpServletRequest request) throws IOException {
        final ServletInputStream inputStream = request.getInputStream();
        if (soapEnvelope == null && !inputStream.isFinished() && inputStream.isReady()) {
            final SOAPModelBuilder builder = BuilderUtil.createSOAPModelBuilder(inputStream, getRequestEncoding(request));
            soapEnvelope = (SOAPEnvelope) builder.getDocumentElement();
        }

    }

    private String getRequestEncoding(HttpServletRequest request) {
        String encoding = BuilderUtil.getCharSetEncoding(request.getContentType());
        if (StringUtils.isBlank(encoding)) {
            encoding = StandardCharsets.UTF_8.name();
        }
        return encoding;
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected FlowFile processRequest(final ProcessContext context, final ProcessSession session, final HttpServletRequest request, final FlowFile source, final Map<String, String> attributes)
            throws IOException {
        readEnvelope(request);

        if (soapEnvelope == null) {
            throw new IOException(Messages.getString("HandleSOAPHttpRequest.unableWriteMessage")); //$NON-NLS-1$
        }
        return session.write(source, new OutputStreamCallback() {

            @Override
            public void process(OutputStream flowFileOut) throws IOException {
                try {
                    final String envelopeOptions = context.getProperty(SOAP_ENVELOPE_OPTIONS).getValue();
                    final EEnvelopeOptions eoption = EEnvelopeOptions.get(envelopeOptions);

                    final OMOutputFormat format = new OMOutputFormat();
                    format.setSOAP11(wsdlDoc.isWSDL11());
                    format.setCharSetEncoding(getRequestEncoding(request));

                    if (EEnvelopeOptions.RAW.equals(eoption)) {
                        soapEnvelope.serialize(flowFileOut, format);
                    } else {
                        final SOAPBody body = soapEnvelope.getBody();

                        final Iterator childElements = body.getChildElements();
                        List<OMElement> bodyElement = new ArrayList<>();
                        while (childElements.hasNext()) {
                            bodyElement.add((OMElement) childElements.next());
                        }
                        OMElement bodyElem = body;
                        if (bodyElement.size() == 1) {// only get the element
                            bodyElem = bodyElement.get(0);
                        }
                        ByteArrayOutputStream bytesStream = new ByteArrayOutputStream(1024);
                        bodyElem.serialize(bytesStream, format);

                        if (EEnvelopeOptions.BODY.equals(eoption)) {
                            attributes.put(CoreAttributes.MIME_TYPE.key(), MimeTypes.Type.TEXT_XML.asString());

                            final Document doc = XMLUtils.newDocument(new ByteArrayInputStream(bytesStream.toByteArray()));
                            FormatUtil.formatXML(doc, flowFileOut);

                        } else if (EEnvelopeOptions.JSON.equals(eoption)) {
                            attributes.put(CoreAttributes.MIME_TYPE.key(), MimeTypes.Type.APPLICATION_JSON.asString());

                            final JSONObject json = XML.toJSONObject(new InputStreamReader(new ByteArrayInputStream(bytesStream.toByteArray())));
                            FormatUtil.formatJson(json, flowFileOut);

                        }
                    }

                } catch (Exception e) {
                    throw new IOException(e);
                }

            }
        });
    }

    @Override
    protected Map<String, Object> getContextAddtions(ProcessContext context, ProcessSession session, HttpServletRequest request) {
        Map<String, Object> addtions = new HashMap<>();
        addtions.put(WSDLDoc.KEY, wsdlDoc);
        return addtions;
    }

}
