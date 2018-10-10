package com.baishancloud.orchsym.processors.soap;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.impl.builder.BuilderUtil;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.util.XMLUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.DynamicProperty;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.PropertyValue;
import org.apache.nifi.components.Validator;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.io.StreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.processors.standard.HandleHttpResponse;
import org.apache.nifi.processors.standard.util.HTTPUtils;
import org.apache.nifi.stream.io.StreamUtils;
import org.eclipse.jetty.http.MimeTypes;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;

import com.baishancloud.orchsym.processors.soap.i18n.Messages;
import com.baishancloud.orchsym.processors.soap.model.EInputOptions;
import com.baishancloud.orchsym.processors.soap.service.WSDLDoc;
import com.baishancloud.orchsym.processors.soap.util.FormatUtil;

/**
 * @author GU Guoqiang
 *
 */
@InputRequirement(Requirement.INPUT_REQUIRED)
@Tags({ "SOAP", "Response", "http", "https", "Web Service", "WSDL" })
@CapabilityDescription("Sends an SOAP HTTP Response to the Requestor that generated a FlowFile. This Processor is designed to be used in conjunction with "
        + "the HandleSOAPHttpRequest in order to create a web service.")
@DynamicProperty(name = "A SOAP Envelope header name", value = "A SOAP Envelope header value", description = "These SOAP Envelope Headers are set in the SOAP HTTP Response", expressionLanguageScope = ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
@ReadsAttributes({
        @ReadsAttribute(attribute = HTTPUtils.HTTP_CONTEXT_ID, description = "The value of this attribute is used to lookup the HTTP Response so that the "
                + "proper message can be sent back to the requestor. If this attribute is missing, the FlowFile will be routed to 'failure.'"),
        @ReadsAttribute(attribute = HTTPUtils.HTTP_REQUEST_URI, description = "Value of the URI requested by the client. Used for provenance event."),
        @ReadsAttribute(attribute = HTTPUtils.HTTP_REMOTE_HOST, description = "IP address of the client. Used for provenance event."),
        @ReadsAttribute(attribute = HTTPUtils.HTTP_LOCAL_NAME, description = "IP address/hostname of the server. Used for provenance event."),
        @ReadsAttribute(attribute = HTTPUtils.HTTP_PORT, description = "Listening port of the server. Used for provenance event."),
        @ReadsAttribute(attribute = HTTPUtils.HTTP_SSL_CERT, description = "SSL distinguished name (if any). Used for provenance event.") })
@SeeAlso(value = { HandleSOAPHttpRequest.class, HandleHttpResponse.class }, classNames = { "org.apache.nifi.http.StandardHttpContextMap", "org.apache.nifi.ssl.StandardSSLContextService" })
public class HandleSOAPHttpResponse extends HandleHttpResponse {
    private static final String PRE_HEADER = "soap.envelope.header";

    public static final PropertyDescriptor INPUT_OPTIONS = new PropertyDescriptor.Builder()//
            .name("soap-input-options")//$NON-NLS-1$
            .displayName(Messages.getString("HandleSOAPHttpResponse.inputOption"))//$NON-NLS-1$
            .description(Messages.getString("HandleSOAPHttpResponse.inputOptionDesc"))//$NON-NLS-1$
            .required(true)//
            .allowableValues(EInputOptions.getAll()).defaultValue(EInputOptions.RAW.getValue())//
            .build();

    public static final PropertyDescriptor PRETTY_PRINT = new PropertyDescriptor.Builder()//
            .name("soap-pretty-print")//$NON-NLS-1$
            .displayName(Messages.getString("HandleSOAPHttpResponse.prettyPrint"))//$NON-NLS-1$
            .description(Messages.getString("HandleSOAPHttpResponse.prettyPrintDesc"))//$NON-NLS-1$
            .required(false)//
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)//
            .allowableValues(Boolean.TRUE.toString(), Boolean.FALSE.toString()).defaultValue(Boolean.TRUE.toString())//
            .build();

    public static final PropertyDescriptor XML_ROOT_TAG = new PropertyDescriptor.Builder()//
            .name("soap-root-tag")//$NON-NLS-1$
            .displayName(Messages.getString("HandleSOAPHttpResponse.rootTag"))//$NON-NLS-1$
            .description(Messages.getString("HandleSOAPHttpResponse.rootTagDesc"))//$NON-NLS-1$
            .required(false)//
            .addValidator(Validator.VALID)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY) //
            .build();

    @Override
    public List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        final List<PropertyDescriptor> properties = super.getSupportedPropertyDescriptors();
        properties.add(INPUT_OPTIONS);
        properties.add(PRETTY_PRINT);
        properties.add(XML_ROOT_TAG);
        return properties;
    }

    @Override
    protected PropertyDescriptor getSupportedDynamicPropertyDescriptor(String propertyDescriptorName) {
        if (propertyDescriptorName.startsWith(PRE_HEADER)) {
            String headerName = propertyDescriptorName.substring(PRE_HEADER.length() + 1);
            return new PropertyDescriptor.Builder() //
                    .name(propertyDescriptorName)//
                    .description("Specifies the value to send for the '" + headerName + "' SOAP Envelope Header")//
                    .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)//
                    .dynamic(true)//
                    .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)//
                    .build();
        }
        return super.getSupportedDynamicPropertyDescriptor(propertyDescriptorName);
    }

    @Override
    protected FlowFile processResponse(final ProcessContext context, final ProcessSession session, final FlowFile source, final HttpServletResponse response, final Map<String, Object> additions)
            throws IOException {
        final EInputOptions option = EInputOptions.get(context.getProperty(INPUT_OPTIONS).getValue());

        String requestContentType = source.getAttribute("http.headers.Content-Type");// same as the request //$NON-NLS-1$
        if (StringUtils.isNotBlank(requestContentType)) {
            requestContentType = MimeTypes.getContentTypeWithoutCharset(requestContentType);
        }
        response.setContentType(requestContentType); // default same as request

        try {
            final Map<String, String> attributes = new HashMap<>();
            FlowFile responseFlowFile = session.write(source, new StreamCallback() {

                @Override
                public void process(InputStream in, OutputStream out) throws IOException {
                    String flowContentType = source.getAttribute(CoreAttributes.MIME_TYPE.key());// same as the request
                    if (StringUtils.isNotBlank(flowContentType)) {
                        flowContentType = MimeTypes.getContentTypeWithoutCharset(flowContentType);
                    }

                    if ("application/xml".equals(flowContentType) || MimeTypes.Type.TEXT_XML.asString().equals(flowContentType)) { // xml
                        processXML(context, session, source, in, out, attributes, response, additions, option);

                    } else if (MimeTypes.Type.APPLICATION_JSON.asString().equals(flowContentType) || MimeTypes.Type.TEXT_JSON.asString().equals(flowContentType)) { // json
                        processJSON(context, session, source, in, out, attributes, response, additions, option);

                    } else if ("application/avro-binary".equals(flowContentType) || "application/avro".equals(flowContentType)) { // avro
                        processAvro(context, session, source, in, out, attributes, response, additions, option);

                    } else {
                        IOUtils.copyLarge(in, response.getOutputStream());
                    }
                }
            });

            responseFlowFile = session.putAllAttributes(responseFlowFile, attributes);
            return responseFlowFile;
        } finally {
            response.flushBuffer();
        }
    }

    void processXML(final ProcessContext context, final ProcessSession session, final FlowFile source, final InputStream flowIn, final OutputStream flowOut, final Map<String, String> attributes,
            final HttpServletResponse response, final Map<String, Object> additions, final EInputOptions inputOption) throws IOException {
        try {

            if (inputOption == EInputOptions.RAW) {
                final Boolean prettyPrint = context.getProperty(PRETTY_PRINT).asBoolean();
                if (prettyPrint != null && prettyPrint) {
                    final Document doc = XMLUtils.newDocument(flowIn);

                    attributes.put(CoreAttributes.MIME_TYPE.key(), MimeTypes.Type.TEXT_XML_UTF_8.asString());
                    response.setContentType(MimeTypes.Type.TEXT_XML_UTF_8.asString());

                    FormatUtil.formatXML(doc, response.getOutputStream());
                    FormatUtil.formatXML(doc, flowOut);
                } else {
                    IOUtils.copyLarge(flowIn, response.getOutputStream());
                }
            } else if (inputOption == EInputOptions.BODY) { // body
                writeBodyXMLStream(context, session, source, flowOut, attributes, response, additions, flowIn);
            }
        } catch (Exception e) { // not xml
            throw new IOException(Messages.getString("HandleSOAPHttpResponse.invalidXmlMessage"), e); //$NON-NLS-1$
        }
    }

    void processJSON(final ProcessContext context, final ProcessSession session, final FlowFile source, final InputStream flowIn, final OutputStream flowOut, final Map<String, String> attributes,
            final HttpServletResponse response, final Map<String, Object> additions, final EInputOptions inputOption) throws IOException {
        try {

            if (inputOption == EInputOptions.RAW) {
                final Boolean prettyPrint = context.getProperty(PRETTY_PRINT).asBoolean();
                if (prettyPrint != null && prettyPrint) {
                    final Object json = readJson(flowIn);

                    attributes.put(CoreAttributes.MIME_TYPE.key(), MimeTypes.Type.APPLICATION_JSON_UTF_8.asString());
                    response.setContentType(MimeTypes.Type.APPLICATION_JSON_UTF_8.asString());

                    FormatUtil.formatJson(json, response.getOutputStream());
                    FormatUtil.formatJson(json, flowOut);

                } else {
                    IOUtils.copyLarge(flowIn, response.getOutputStream());
                }
            } else if (inputOption == EInputOptions.BODY) { // body
                String tag = context.getProperty(XML_ROOT_TAG).evaluateAttributeExpressions().getValue();
                if (StringUtils.isBlank(tag)) {
                    tag = null;
                }

                final Object json = readJson(flowIn);
                final String xml = XML.toString(json, tag);

                InputStream xmlStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
                writeBodyXMLStream(context, session, source, flowOut, attributes, response, additions, xmlStream);
            }
        } catch (Exception e) { // not xml
            throw new IOException(Messages.getString("HandleSOAPHttpResponse.invalidJsonMessage"), e); //$NON-NLS-1$
        }
    }

    Object readJson(final InputStream flowStream) throws IOException {
        final ByteArrayOutputStream jsonStream = new ByteArrayOutputStream(1000);
        StreamUtils.copy(flowStream, jsonStream);

        final String jsonStr = jsonStream.toString(StandardCharsets.UTF_8.name()).trim();
        Object json = null;
        if (jsonStr.startsWith("[")) { // array
            json = new JSONArray(jsonStr);
        } else {
            json = new JSONObject(jsonStr);
        }
        return json;
    }

    void processAvro(final ProcessContext context, final ProcessSession session, final FlowFile source, final InputStream flowIn, final OutputStream flowOut, final Map<String, String> attributes,
            final HttpServletResponse response, final Map<String, Object> additions, final EInputOptions inputOption) throws IOException {
        try {

            if (inputOption == EInputOptions.RAW) {
                final Boolean prettyPrint = context.getProperty(PRETTY_PRINT).asBoolean();
                if (prettyPrint != null && prettyPrint) {
                    final String result = convertJson(flowIn);
                    final JSONArray array = new JSONArray(result);

                    attributes.put(CoreAttributes.MIME_TYPE.key(), MimeTypes.Type.APPLICATION_JSON_UTF_8.asString());
                    response.setContentType(MimeTypes.Type.APPLICATION_JSON_UTF_8.asString());

                    FormatUtil.formatJson(array, response.getOutputStream());
                    FormatUtil.formatJson(array, flowOut);
                } else {
                    IOUtils.copyLarge(flowIn, response.getOutputStream());
                }
            } else if (inputOption == EInputOptions.BODY) { // body
                String tag = context.getProperty(XML_ROOT_TAG).evaluateAttributeExpressions().getValue();
                if (StringUtils.isBlank(tag)) {
                    tag = null;
                }

                final String jsonStr = convertJson(flowIn);
                if (StringUtils.isNotBlank(jsonStr)) { // must be array
                    JSONArray array = new JSONArray(jsonStr);
                    final String xml = XML.toString(array, tag);
                    InputStream xmlStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
                    writeBodyXMLStream(context, session, source, flowOut, attributes, response, additions, xmlStream);
                }
            }
        } catch (Exception e) { // not xml
            throw new IOException(Messages.getString("HandleSOAPHttpResponse.invalidRecordMessage"), e); //$NON-NLS-1$
        }
    }

    String convertJson(final InputStream flowStream) throws IOException {

        StringBuilder jsonStr = new StringBuilder(1000);
        jsonStr.append('[');

        try (final InputStream in = new BufferedInputStream(flowStream); //
                final DataFileStream<GenericRecord> reader = new DataFileStream<>(in, new GenericDatumReader<GenericRecord>())) {
            final GenericData genericData = GenericData.get();

            int recordCount = 0;
            GenericRecord currRecord = null;
            while (reader.hasNext()) {
                if (recordCount > 0)
                    jsonStr.append(',');
                currRecord = reader.next(currRecord);
                jsonStr.append(genericData.toString(currRecord).getBytes(StandardCharsets.UTF_8));
                recordCount++;
            }

        }
        jsonStr.append(']');
        return jsonStr.toString();
    }

    void writeBodyXMLStream(final ProcessContext context, final ProcessSession session, final FlowFile source, final OutputStream flowOut, final Map<String, String> attributes,
            final HttpServletResponse response, final Map<String, Object> additions, final InputStream xmlStream) throws IOException {
        if (xmlStream != null) {
            try {

                final Object wd = additions.get(WSDLDoc.KEY);
                if (wd instanceof WSDLDoc) {
                    final WSDLDoc wsdlDoc = (WSDLDoc) wd;

                    final SOAPFactory soapFactory = wsdlDoc.isWSDL20() ? OMAbstractFactory.getSOAP12Factory() : OMAbstractFactory.getSOAP11Factory();
                    final SOAPEnvelope envelope = soapFactory.createSOAPEnvelope();
                    // header
                    final SOAPHeader soapHeader = soapFactory.createSOAPHeader(envelope);
                    for (final Map.Entry<PropertyDescriptor, String> e : context.getProperties().entrySet()) {
                        final PropertyDescriptor descriptor = e.getKey();
                        if (descriptor.isDynamic() && descriptor.getName().startsWith(PRE_HEADER)) {
                            final PropertyValue value = context.getProperty(descriptor);

                            String headerName = descriptor.getName().substring(PRE_HEADER.length() + 1);
                            OMElement headerOM = OMAbstractFactory.getOMFactory().createOMElement(headerName, null);
                            String headerText = value.evaluateAttributeExpressions(source).getValue();
                            headerOM.setText(headerText);

                            soapHeader.addChild(headerOM);
                        }
                    }

                    // body
                    final SOAPBody soapBody = soapFactory.createSOAPBody(envelope);
                    // final OMElement bodyOM = AXIOMUtil.stringToOM( "");
                    final OMElement bodyOM = OMXMLBuilderFactory.createOMBuilder(OMAbstractFactory.getOMFactory(), new InputStreamReader(xmlStream)).getDocumentElement();
                    BuilderUtil.setNamespace(bodyOM, wsdlDoc.getDefinition().getTargetNamespace(), null, false);
                    soapBody.addChild(bodyOM);

                    final OMOutputFormat outputFormat = new OMOutputFormat();
                    outputFormat.setSOAP11(wsdlDoc.isWSDL11());
                    outputFormat.setCharSetEncoding(StandardCharsets.UTF_8.name());

                    attributes.put(CoreAttributes.MIME_TYPE.key(), MimeTypes.Type.TEXT_XML_UTF_8.asString());
                    response.setContentType(MimeTypes.Type.TEXT_XML_UTF_8.asString());

                    envelope.serialize(response.getOutputStream(), outputFormat);
                    envelope.serialize(flowOut, outputFormat);
                }
            } catch (Exception e) {
                throw new IOException(Messages.getString("HandleSOAPHttpResponse.invalidXmlMessage"), e); //$NON-NLS-1$
            }
        } else {
            throw new IOException(Messages.getString("HandleSOAPHttpResponse.unsupportInputMessage")); //$NON-NLS-1$
        }
    }

}
