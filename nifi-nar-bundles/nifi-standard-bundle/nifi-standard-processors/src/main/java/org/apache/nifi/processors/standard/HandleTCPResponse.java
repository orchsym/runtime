package org.apache.nifi.processors.standard;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.lifecycle.OnStopped;
import org.apache.nifi.annotation.lifecycle.OnUnscheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.lookup.KeyValueLookupService;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.processor.util.listen.dispatcher.ChannelDispatcher;
import org.apache.nifi.processor.util.listen.event.StandardEvent;
import org.apache.nifi.processor.util.listen.response.ChannelResponder;
import org.apache.nifi.processor.util.listen.response.ChannelResponse;
import org.apache.nifi.processors.standard.ListenTCP.TCPContextEvent;
import org.apache.nifi.processors.standard.ListenTCP.TCPResponse;
import org.apache.nifi.util.StopWatch;

@InputRequirement(Requirement.INPUT_REQUIRED)
@Tags({ "TCP", "response", "web service" })
@CapabilityDescription("Sends an TCP Response to the TCP listener that generated a FlowFile. This Processor is designed to be used in conjunction with "
        + "the ListenTCP in order to create a web service.")
@ReadsAttributes({ @ReadsAttribute(attribute = ListenTCP.TCP_SENDER, description = "The sending host of the messages."),
        @ReadsAttribute(attribute = ListenTCP.TCP_PORT, description = "The sending port the messages were received."),
        @ReadsAttribute(attribute = ListenTCP.TCP_CONTEXT_ID, description = "The sending identifier of the messages."),
        @ReadsAttribute(attribute = ListenTCP.TCP_CONTEXT_CHARSET, description = "The sending charset of the messages.") })
@SeeAlso(value = { ListenTCP.class }, classNames = { "org.apache.nifi.lookup.CommonKeyValueLookupService" })
public class HandleTCPResponse extends AbstractProcessor {

    public static final PropertyDescriptor RESPONSE_TEXT = new PropertyDescriptor.Builder().name("Responder text").description("The value of response after process data").required(true)
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR).expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES).build();

    public static final PropertyDescriptor RESPONSE_DELIMITER = new PropertyDescriptor.Builder().name("Response delimiter")
            .description("Specifies the delimiter to place between messages， if not set, will be same as ListenTCP.").addValidator(Validator.VALID)
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES).defaultValue("${" + ListenTCP.TCP_RESPONSE_SEPERATOR + "}").required(false).build();

    public static final PropertyDescriptor RESPONDER_CONTEXT_MAP = new PropertyDescriptor.Builder().name("Responder context map")
            .description("The Controller Service to use in order to hold the response of current TCP, If this property is set, " + "messages will be sent over a secure connection.").required(true)
            .identifiesControllerService(KeyValueLookupService.class).build();


    public static final Relationship REL_SUCCESS = new Relationship.Builder().name("success")
            .description("FlowFiles will be routed to this Relationship after the response has been successfully sent to the requestor").build();

    public static final Relationship REL_FAILURE = new Relationship.Builder().name("failure")
            .description("FlowFiles will be routed to this Relationship if the Processor is unable to respond to the requestor. This may happen, "
                    + "for instance, if the connection times out or if NiFi is restarted before responding to the HTTP Request.")
            .build();


    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        final List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(RESPONSE_TEXT);
        properties.add(RESPONSE_DELIMITER);
        properties.add(RESPONDER_CONTEXT_MAP);
        return properties;
    }

    @Override
    public Set<Relationship> getRelationships() {
        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
        return relationships;
    }


    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }
        final StopWatch stopWatch = new StopWatch(true);

        final String contextIdentifier = flowFile.getAttribute(ListenTCP.TCP_CONTEXT_ID);
        if (contextIdentifier == null) {
            session.transfer(flowFile, REL_FAILURE);
            getLogger().warn("Failed to respond to TCP request for {} because FlowFile did not have an '" + ListenTCP.TCP_CONTEXT_ID + "' attribute", new Object[] { flowFile });
            return;
        }

        final KeyValueLookupService lookupService = context.getProperty(RESPONDER_CONTEXT_MAP).asControllerService(KeyValueLookupService.class);
        Optional<Object> optional = lookupService.get(contextIdentifier);
        if (!optional.isPresent()) {
            session.transfer(flowFile, REL_FAILURE);
            getLogger().warn("Failed to respond to TCP request for {} because can't find the value from responder context map for key '" + ListenTCP.TCP_CONTEXT_ID + "'", new Object[] { flowFile });
            return;
        }

        TCPContextEvent contextEvent = (TCPContextEvent) optional.get();
        if (contextEvent.events != null && !contextEvent.events.isEmpty()) {
            // sent response text
            final String responseText = context.getProperty(RESPONSE_TEXT).evaluateAttributeExpressions(flowFile).getValue();

            // same as request charset
            Charset charset = StandardCharsets.UTF_8;
            String charsetStr = flowFile.getAttribute(ListenTCP.TCP_CONTEXT_CHARSET);
            if (StringUtils.isNotBlank(charsetStr) && StringUtils.isNotBlank(charsetStr)) {
                charset = Charset.forName(charsetStr);
            }
            String delimiter = context.getProperty(RESPONSE_DELIMITER).evaluateAttributeExpressions(flowFile).getValue();
            String delimiterStr = flowFile.getAttribute(ListenTCP.TCP_RESPONSE_SEPERATOR);
            if (StringUtils.isEmpty(delimiter) && StringUtils.isNotEmpty(delimiterStr)) {
                delimiter = delimiterStr;
            }

            if (StringUtils.isNotEmpty(responseText)) {
                final ChannelResponse response = new TCPResponse(responseText, delimiter, charset);

                for (StandardEvent event : contextEvent.events) {
                    ChannelResponder responder = event.getResponder();
                    responder.addResponse(response);
                    try {
                        responder.respond();
                    } catch (IOException e) {
                        session.transfer(flowFile, REL_FAILURE);
                        getLogger().error("Error sending response for transaction {} due to {}", new Object[] { responseText, e.getMessage() }, e);
                        return;
                    }
                }
            }
        } else {
            session.transfer(flowFile, REL_FAILURE);
            getLogger().warn("Failed to respond to TCP request for {} because no reponder to process for the '" + ListenTCP.TCP_CONTEXT_ID + "'", new Object[] { flowFile });
            return;
        }

        session.getProvenanceReporter().send(flowFile, getTransitUri(flowFile.getAttributes()), stopWatch.getElapsed(TimeUnit.MILLISECONDS));
        session.transfer(flowFile, REL_SUCCESS);
        getLogger().info("Successfully responded to TCP Request for {} ", new Object[] { flowFile });
    }

    protected String getTransitUri(Map<String, String> attributes) {
        final String sender = attributes.get(ListenTCP.TCP_SENDER);
        final String port = attributes.get(ListenTCP.TCP_PORT);
        return ListenTCP.getTransitUri(sender, port);
    }
}
