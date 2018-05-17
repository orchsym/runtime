package org.apache.nifi.processors.standard;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.csv.CSVUtils;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.lookup.KeyValueLookupService;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.processor.util.listen.event.StandardEvent;
import org.apache.nifi.processor.util.listen.response.ChannelResponder;
import org.apache.nifi.processor.util.listen.response.ChannelResponse;
import org.apache.nifi.processors.standard.ListenTCP.TCPContextEvent;
import org.apache.nifi.util.StopWatch;

@InputRequirement(Requirement.INPUT_REQUIRED)
@Tags({ "TCP", "response", "web service" })
@CapabilityDescription("Sends an TCP Response to the TCP listener that generated a FlowFile. This Processor is designed to be used in conjunction with "
        + "the ListenTCP in order to create a web service.")
@ReadsAttributes({ @ReadsAttribute(attribute = ListenTCP.TCP_SENDER, description = "The sending host of the messages."),
        @ReadsAttribute(attribute = ListenTCP.TCP_PORT, description = "The sending port the messages were received."),
        @ReadsAttribute(attribute = ListenTCP.TCP_CONTEXT_ID, description = "The sending identifier of the messages."),
        @ReadsAttribute(attribute = ListenTCP.TCP_CONTEXT_CHARSET, description = "The sending charset of the messages."),
        @ReadsAttribute(attribute = ListenTCP.TCP_RESPONSE_DELIMITER, description = "The sending response delimiter of the messages.") })
@WritesAttributes({ @WritesAttribute(attribute = HandleTCPResponse.TCP_RESPONSE_SENDER, description = "The sending host of the messages."),
        @WritesAttribute(attribute = ListenTCP.TCP_PORT, description = "The sending port the messages were received."),
        @WritesAttribute(attribute = ListenTCP.TCP_CONTEXT_CHARSET, description = "The sending charset of the messages."),
        @WritesAttribute(attribute = ListenTCP.TCP_RESPONSE_DELIMITER, description = "The sending response delimiter of the messages.") })
@SeeAlso(value = { ListenTCP.class }, classNames = { "org.apache.nifi.lookup.CommonKeyValueLookupService" })
public class HandleTCPResponse extends AbstractProcessor {
    public enum ResponseType {
        Flow, Text;
    }

    public static final String TCP_RESPONSE_SENDER = "tcp.response.sender";

    public static final PropertyDescriptor RESPONDER_CONTEXT_MAP = new PropertyDescriptor.Builder().name("Responder context map")
            .description("The Controller Service to use in order to hold the response of current TCP, If this property is set, " + "messages will be sent over a secure connection.").required(true)
            .identifiesControllerService(KeyValueLookupService.class).build();

    public static final PropertyDescriptor RESPONSE_DELIMITER = new PropertyDescriptor.Builder().name("Response delimiter")
            .description("Specifies the delimiter to place between messages， if not set, try to read from the ListenTCP. also can set like \\n, \\r, \\t, etc").addValidator(Validator.VALID)
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES).required(false).build();

    public static final PropertyDescriptor RESPONSE_TYPE = new PropertyDescriptor.Builder().name("Response type")
            .description("The type of response. do response for the contents of flow when flow type, the response text with flow attributes when text type.").required(true)
            .allowableValues(ResponseType.values()).defaultValue(ResponseType.Flow.name()).addValidator(Validator.VALID).build();

    public static final PropertyDescriptor RESPONSE_MAX_DATA_LENGTH = new PropertyDescriptor.Builder().name("Contents max length")
            .description("The max length of response, when process the contents of type " + ResponseType.Flow).required(false).defaultValue("1024")
            .addValidator(StandardValidators.createLongValidator(32, Integer.MAX_VALUE, true)).expressionLanguageSupported(ExpressionLanguageScope.NONE).build();

    public static final PropertyDescriptor RESPONSE_TEXT = new PropertyDescriptor.Builder().name("Response text").description("The value of response, when the type is " + ResponseType.Text)
            .required(false).addValidator(StandardValidators.NON_BLANK_VALIDATOR).expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES).build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder().name("success")
            .description("FlowFiles will be routed to this Relationship after the response has been successfully sent to the requestor").build();

    public static final Relationship REL_ORIGINAL = new Relationship.Builder().name("original")
            .description("FlowFiles will be routed to this original Relationship after the response has been successfully sent to the requestor").build();

    public static final Relationship REL_FAILURE = new Relationship.Builder().name("failure")
            .description("FlowFiles will be routed to this Relationship if the Processor is unable to respond to the requestor. This may happen, "
                    + "for instance, if the connection times out or if NiFi is restarted before responding to the TCP Request.")
            .build();

    protected volatile ResponseType responseType;
    protected volatile int dataLen;

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return Arrays.asList(RESPONDER_CONTEXT_MAP, RESPONSE_DELIMITER, RESPONSE_TYPE, RESPONSE_MAX_DATA_LENGTH, RESPONSE_TEXT);
    }

    @Override
    public Set<Relationship> getRelationships() {
        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_ORIGINAL);
        relationships.add(REL_FAILURE);
        return relationships;
    }

    @OnScheduled
    public void onScheduled(ProcessContext context) throws IOException {
        responseType = ResponseType.valueOf(context.getProperty(RESPONSE_TYPE).getValue());
        dataLen = context.getProperty(RESPONSE_MAX_DATA_LENGTH).asInteger();
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        FlowFile originalFlowFile = session.get();
        if (originalFlowFile == null) {
            return;
        }
        final StopWatch stopWatch = new StopWatch(true);

        final String contextIdentifier = originalFlowFile.getAttribute(ListenTCP.TCP_CONTEXT_ID);
        if (contextIdentifier == null) {
            session.transfer(originalFlowFile, REL_FAILURE);
            getLogger().warn("Failed to respond to TCP request for {} because FlowFile did not have an '" + ListenTCP.TCP_CONTEXT_ID + "' attribute", new Object[] { originalFlowFile });
            return;
        }

        final KeyValueLookupService lookupService = context.getProperty(RESPONDER_CONTEXT_MAP).asControllerService(KeyValueLookupService.class);
        Optional<Object> optional = lookupService.get(contextIdentifier);
        if (!optional.isPresent()) {
            session.transfer(originalFlowFile, REL_FAILURE);
            getLogger().warn("Failed to respond to TCP request for {} because can't find the value from responder context map for key '" + ListenTCP.TCP_CONTEXT_ID + "'",
                    new Object[] { originalFlowFile });
            return;
        }

        // same as request charset
        Charset charset = StandardCharsets.UTF_8;
        String charsetStr = originalFlowFile.getAttribute(ListenTCP.TCP_CONTEXT_CHARSET);
        if (StringUtils.isNotBlank(charsetStr)) {
            charset = Charset.forName(charsetStr);
        }
        String delimiter = context.getProperty(RESPONSE_DELIMITER).evaluateAttributeExpressions(originalFlowFile).getValue();
        String flowDelimiter = originalFlowFile.getAttribute(ListenTCP.TCP_RESPONSE_DELIMITER);
        if (StringUtils.isNotEmpty(delimiter)) { // set value
            delimiter = CSVUtils.unescape(delimiter); // reuse for \r \n \t
        } else if (StringUtils.isNotEmpty(flowDelimiter)) {// when empty value and have set in flow attribute.
            delimiter = flowDelimiter;
        } else {
            delimiter = ""; // without delimiter settings
        }
        final byte[] delimiterBytes = delimiter.getBytes(charset);

        FlowFile outputFlowFile = session.clone(originalFlowFile);
        final Map<String, String> attributes = new HashMap<>();

        try {
            TCPContextEvent contextEvent = (TCPContextEvent) optional.get();
            if (responseType == ResponseType.Flow) {
                responseFlow(context, session, outputFlowFile, contextEvent, charset, delimiterBytes, attributes);
            } else if (responseType == ResponseType.Text) {
                responseText(context, session, outputFlowFile, contextEvent, charset, delimiterBytes, attributes);
            }
        } catch (Exception e) {
            session.transfer(originalFlowFile, REL_FAILURE);
            getLogger().warn("Failed to respond to TCP request for {} because no reponder to process for the '" + ListenTCP.TCP_CONTEXT_ID + "'", new Object[] { originalFlowFile });
            return;
        }

        // add attributes
        attributes.put(ListenTCP.TCP_CONTEXT_CHARSET, charset.name());
        attributes.put(ListenTCP.TCP_RESPONSE_DELIMITER, delimiter);
        outputFlowFile = session.putAllAttributes(outputFlowFile, attributes);

        session.getProvenanceReporter().send(outputFlowFile, getTransitUri(outputFlowFile.getAttributes()), stopWatch.getElapsed(TimeUnit.MILLISECONDS));
        session.transfer(outputFlowFile, REL_SUCCESS);

        session.transfer(originalFlowFile, REL_ORIGINAL);
        getLogger().info("Successfully responded to TCP Request for {} ", new Object[] { outputFlowFile });

    }

    protected String getTransitUri(Map<String, String> attributes) {
        final String sender = attributes.get(ListenTCP.TCP_SENDER);
        final String port = attributes.get(ListenTCP.TCP_PORT);
        return ListenTCP.getTransitUri(sender, port);
    }

    protected void responseFlow(final ProcessContext context, final ProcessSession session, final FlowFile flowFile, final TCPContextEvent contextEvent, final Charset charset,
            final byte[] delimiterBytes, final Map<String, String> attributes) throws IOException {
        try (final InputStream rawIn = session.read(flowFile); final BufferedInputStream in = new BufferedInputStream(rawIn)) {
            byte[] buffer = new byte[dataLen];
            int len = 0;
            while ((len = in.read(buffer)) != -1) {
                final ChannelResponse response = new TCPResponse(Arrays.copyOf(buffer, len), delimiterBytes);
                respond(contextEvent, response, attributes);
            }
        }
    }

    protected void responseText(final ProcessContext context, final ProcessSession session, final FlowFile flowFile, final TCPContextEvent contextEvent, final Charset charset,
            final byte[] delimiterBytes, final Map<String, String> attributes) throws IOException {
        if (contextEvent.events == null || contextEvent.events.isEmpty()) {
            return;
        }
        // sent response text
        final String responseText = context.getProperty(RESPONSE_TEXT).evaluateAttributeExpressions(flowFile).getValue();

        if (StringUtils.isEmpty(responseText)) {
            throw new IllegalArgumentException("The response text is required, when response type is " + ResponseType.Text);
        }
        final ChannelResponse response = new TCPResponse(responseText.getBytes(charset), delimiterBytes);
        respond(contextEvent, response, attributes);
    }

    private void respond(final TCPContextEvent contextEvent, final ChannelResponse response, final Map<String, String> attributes) throws IOException {
        for (StandardEvent event : contextEvent.events) {
            ChannelResponder responder = event.getResponder();
            // SelectableChannel channel = responder.getChannel();
            // if (channel instanceof SocketChannel) { // make sure no duplicated responses
            // responder = new SocketChannelResponder((SocketChannel) channel);
            // }
            responder.addResponse(response);
            responder.respond();

            attributes.put(TCP_RESPONSE_SENDER, event.getSender());
        }
    }

    static class TCPResponse implements ChannelResponse {

        private byte[] values;

        public TCPResponse(byte[] responseText, byte[] delimiter) {
            final int textLen = responseText.length;
            final int delimiterLen = delimiter.length;

            final byte[] writeBuffer = new byte[textLen + delimiterLen];

            System.arraycopy(responseText, 0, writeBuffer, 0, textLen);
            System.arraycopy(delimiter, 0, writeBuffer, textLen, delimiterLen);

            values = Arrays.copyOf(writeBuffer, textLen + delimiterLen);
        }

        @Override
        public byte[] toByteArray() {
            return values;
        }

    }

}
