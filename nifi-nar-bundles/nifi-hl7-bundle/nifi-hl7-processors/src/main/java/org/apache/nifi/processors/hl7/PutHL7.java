package org.apache.nifi.processors.hl7;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.behavior.SideEffectFree;
import org.apache.nifi.annotation.behavior.SupportsBatching;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.StreamCallback;
import org.apache.nifi.processor.util.StandardValidators;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.app.Connection;
import ca.uhn.hl7v2.app.ConnectionListener;
import ca.uhn.hl7v2.app.Initiator;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.protocol.ReceivingApplicationExceptionHandler;
import ca.uhn.hl7v2.validation.ValidationContext;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;

@SideEffectFree
@SupportsBatching
@InputRequirement(Requirement.INPUT_REQUIRED)
@Tags({"HL7", "health level 7", "healthcare", "ack", "attributes","put","send"})
@CapabilityDescription("Put HL7 message.")

public class PutHL7 extends AbstractProcessor {

    public static final PropertyDescriptor CHARACTER_SET = new PropertyDescriptor.Builder()
            .name("Character Encoding")
            .displayName("Character Encoding")
            .description("The Character Encoding that is used to encode the HL7 data")
            .required(true)
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .addValidator(StandardValidators.CHARACTER_SET_VALIDATOR)
            .defaultValue("UTF-8")
            .build();

    public static final PropertyDescriptor HOST = new PropertyDescriptor.Builder()
            .name("HOST")
            .displayName("HOST")
            .description("HOST")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    public static final PropertyDescriptor PORT = new PropertyDescriptor.Builder()
            .name("Single MLLP PORT")
            .displayName("Single MLLP PORT")
            .description("Single MLLP PORT.")
            .required(true)
            .addValidator(StandardValidators.PORT_VALIDATOR)
            .build();

    public static final PropertyDescriptor TLS = new PropertyDescriptor.Builder()
            .name("Transport Layer Security")
            .displayName("Transport Layer Security Protocol")
            .description("Support Transport Layer Security Protocol.")
            .required(true)
            .allowableValues("true", "false")
            .defaultValue("false")
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();

    public static final PropertyDescriptor SKIP_VALIDATION = new PropertyDescriptor.Builder()
            .name("skip-validation")
            .displayName("Skip Validation")
            .description("Whether or not to validate HL7 message values")
            .required(true)
            .allowableValues("true", "false")
            .defaultValue("true")
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();
    
    public static final PropertyDescriptor HL7_INPUT_VERSION = new PropertyDescriptor.Builder()
            .name("hl7-input-version")
            .displayName("HL7 Input Version")
            .description("The HL7 version to use for parsing and validation")
            .required(true)
            .allowableValues("autodetect", "2.2", "2.3", "2.3.1", "2.4", "2.5", "2.5.1", "2.6")
            .defaultValue("autodetect")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    
    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("A FlowFile is routed to this relationship if it is properly parsed as HL7 and its attributes extracted")
            .build();
    
    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("A FlowFile is routed to this relationship if it cannot be mapped to FlowFile Attributes. This would happen if the FlowFile does not contain valid HL7 data")
            .build();
    
    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        final List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(HOST);
        properties.add(PORT);
        properties.add(TLS);
        properties.add(SKIP_VALIDATION);
        properties.add(HL7_INPUT_VERSION);
        properties.add(CHARACTER_SET);
        return properties;
    }
    
    @Override
    public Set<Relationship> getRelationships() {
        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
        return relationships;
    }
    
    private volatile HapiContext hapiContext = new DefaultHapiContext();
    
    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }
        try {
            final String host = context.getProperty(HOST).getValue();
            final Integer port = context.getProperty(PORT).asInteger();
            final Boolean isTLS = context.getProperty(TLS).asBoolean();
            final Boolean skipValidation = context.getProperty(SKIP_VALIDATION).asBoolean();
            final String inputVersion = context.getProperty(HL7_INPUT_VERSION).getValue();
            final String encoding = context.getProperty(CHARACTER_SET).getValue();
            if (!"autodetect".equals(inputVersion)) {
                hapiContext.setModelClassFactory(new CanonicalModelClassFactory(inputVersion));
            }
            if (skipValidation) {
                hapiContext.setValidationContext((ValidationContext) ValidationContextFactory.noValidation());
            }
            PipeParser p = hapiContext.getPipeParser();
            Connection conn = hapiContext.newClient(host, port, isTLS);
            Initiator initiator = conn.getInitiator();
            flowFile = session.write(flowFile, new StreamCallback() {
                @Override
                public void process(final InputStream in, final OutputStream out) throws IOException {
                    String receiveMsg = IOUtils.toString(in, encoding); 
                    Message adt = null;
                    try {
                        receiveMsg = receiveMsg.replaceAll("\n","\r");
                        adt = p.parse(receiveMsg);
                        Message response = initiator.sendAndReceive(adt);
                        String responseString = p.encode(response).replaceAll("\r","\n");
                        if (responseString.length() > 0) {
                            out.write(responseString.getBytes(encoding));
                        }
                    } catch (Exception e) {
                        getLogger().error("Send and receive message failed." + e);
                    }finally{
                        conn.close();
                    }
                }
            });
            session.getProvenanceReporter().create(flowFile);
            session.transfer(flowFile, REL_SUCCESS);
        } catch (Exception e) {
            context.yield();
            getLogger().error("Send and receive message failed." + e);
            session.transfer(flowFile,REL_FAILURE);
        }
    }

    public static class MyConnectionListener implements ConnectionListener {

        public void connectionReceived(Connection theC) {
        }

        public void connectionDiscarded(Connection theC) {
            theC.close();
        }

    }

    public static class MyExceptionHandler implements ReceivingApplicationExceptionHandler {

        public String processException(String theIncomingMessage, Map<String, Object> theIncomingMetadata, String theOutgoingMessage, Exception theE) throws HL7Exception {
            return theOutgoingMessage;
        }

    }

}
