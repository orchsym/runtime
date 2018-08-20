package org.apache.nifi.processors.hl7;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.behavior.SideEffectFree;
import org.apache.nifi.annotation.behavior.SupportsBatching;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.lifecycle.OnStopped;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.app.Connection;
import ca.uhn.hl7v2.app.ConnectionListener;
import ca.uhn.hl7v2.app.HL7Service;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.protocol.ReceivingApplication;
import ca.uhn.hl7v2.protocol.ReceivingApplicationExceptionHandler;
import ca.uhn.hl7v2.validation.ValidationContext;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;

@SideEffectFree
@SupportsBatching
@InputRequirement(Requirement.INPUT_FORBIDDEN)
@Tags({"HL7", "health level 7", "healthcare", "ack", "attributes"})
@CapabilityDescription("HL7 message listener.Generate a simple acknowledgment message from reveived message.")

public class ListenHL7 extends AbstractProcessor {

    private volatile HapiContext hapiContext = new DefaultHapiContext();
    private AtomicBoolean initialized = new AtomicBoolean(false);
    private volatile BlockingQueue<Message> containerQueue;

    public static final PropertyDescriptor CHARACTER_SET = new PropertyDescriptor.Builder()
            .name("Character Encoding")
            .displayName("Character Encoding")
            .description("The Character Encoding that is used to encode the HL7 data")
            .required(true)
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .addValidator(StandardValidators.CHARACTER_SET_VALIDATOR)
            .defaultValue("UTF-8")
            .build();

    public static final PropertyDescriptor CONTAINER_QUEUE_SIZE = new PropertyDescriptor.Builder()
            .name("container-queue-size").displayName("Container Queue Size")
            .description("The size of the queue for Http Request Containers").required(true)
            .addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR).defaultValue("50").build();

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
            .description("A FlowFile is routed to this relationship if it is properly listen and transfer to be ack message!")
            .build();
    
    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("A FlowFile is routed to this relationship if it parse message from client. This would happen if the FlowFile does not contain valid HL7 data")
            .build();
    
    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        final List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(CHARACTER_SET);
        properties.add(PORT);
        properties.add(TLS);
        properties.add(SKIP_VALIDATION);
        properties.add(HL7_INPUT_VERSION);
        properties.add(CONTAINER_QUEUE_SIZE);
        return properties;
    }
    
    @Override
    public Set<Relationship> getRelationships() {
        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
        return relationships;
    }

    private volatile HL7Service server;
    
    @OnScheduled
    public void onScheduled(final ProcessContext context) throws Exception {
        initialized.set(false);
    }
    
    @OnStopped
    public void shutdown() throws Exception {
        if (server != null) {
            getLogger().debug("Shutting down server");
            server.stop();
            getLogger().info("Shut down {}", new Object[]{server});
        }
    }

    private synchronized void initializeServer(final ProcessContext context,ProcessSession session) throws Exception {
        if(initialized.get()){
            return;
        }
        this.containerQueue = new LinkedBlockingQueue<>(context.getProperty(CONTAINER_QUEUE_SIZE).asInteger());
        final Integer port = context.getProperty(PORT).asInteger();
        final Boolean isTLS = context.getProperty(TLS).asBoolean();
        final Boolean skipValidation = context.getProperty(SKIP_VALIDATION).asBoolean();
        final String inputVersion = context.getProperty(HL7_INPUT_VERSION).getValue();
        if (!inputVersion.equals("autodetect")) {
            hapiContext.setModelClassFactory(new CanonicalModelClassFactory(inputVersion));
        }
        if (skipValidation) {
            hapiContext.setValidationContext((ValidationContext) ValidationContextFactory.noValidation());
        }
        HL7Service server = hapiContext.newServer(port, isTLS);
        ReceivingApplication handler = new ReceivingApplication() {
            @Override
            public Message processMessage(Message theMessage, Map<String, Object> arg1) throws HL7Exception {
                Message res = null;
                try {
                    res = theMessage.generateACK();
                } catch (IOException e) {
                    getLogger().warn("Could not get ACK message!" + e);
                }
                containerQueue.offer(theMessage);
                return res;
            }

            @Override
            public boolean canProcess(Message msg) {
                return true;
            }
        };
        server.registerApplication(handler);
        server.registerConnectionListener(new MyConnectionListener());
        server.setExceptionHandler(new MyExceptionHandler());
        this.server = server;
        server.startAndWait();
        Throwable t = server.getServiceExitedWithException();
        if(t !=null){
            shutdown();
            throw new RuntimeException(t.getMessage());
        }
        initialized.set(true);
    }

    protected int getRequestQueueSize() {
        return containerQueue.size();
    }
    
    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        try {
            if(!initialized.get()) {
                initializeServer(context, session);
            }
        } catch (Exception e) {
            context.yield();
            getLogger().error("Init HL7 Listener failed: " + e);
            try {
                shutdown();
            } catch (final Exception shutdownException) {
                getLogger().debug("Failed to shutdown following a failed initialization: " + shutdownException);
            }
            throw new ProcessException("Failed to initialize the server", e);
        }

        Message theMessage = null;
        FlowFile flowFile = null;
        try {
            theMessage = containerQueue.poll(2, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        if(theMessage == null){
            return;
        }
        PipeParser pipeParser = hapiContext.getPipeParser();
        try {
            String receivedMessage = pipeParser.encode(theMessage);
            Message ackMessage = theMessage.generateACK();
            flowFile = session.create();
            final String encoding = context.getProperty(CHARACTER_SET).getValue();
            if (receivedMessage.length() > 0) {
                flowFile = session.write(flowFile, new OutputStreamCallback() {
                    @Override
                    public void process(final OutputStream out) throws IOException {
                        out.write(receivedMessage.getBytes(encoding));
                    }
                });
            }
            session.putAttribute(flowFile, "response_message", ackMessage.toString());
            session.getProvenanceReporter().create(flowFile);
        } catch (HL7Exception e) {
            getLogger().error("Warn when get ACK from receive message." + e);
            session.transfer(flowFile, REL_FAILURE);
            return;
        } catch (IOException e) {
            getLogger().error("Warn when get ACK from receive message." + e);
            session.transfer(flowFile, REL_FAILURE);
            return;
        }
        session.transfer(flowFile, REL_SUCCESS);
    }

    /**
     * Connection listener which is notified whenever a new
     * connection comes in or is lost
     */
    public static class MyConnectionListener implements ConnectionListener {

        public void connectionReceived(Connection theC) {
        }

        public void connectionDiscarded(Connection theC) {
            theC.close();
        }

    }

    /**
     * Exception handler which is notified any time
     */
    public static class MyExceptionHandler implements ReceivingApplicationExceptionHandler {

        public String processException(String theIncomingMessage, Map<String, Object> theIncomingMetadata, String theOutgoingMessage, Exception theE) throws HL7Exception {
            return theOutgoingMessage;
        }

    }

}
