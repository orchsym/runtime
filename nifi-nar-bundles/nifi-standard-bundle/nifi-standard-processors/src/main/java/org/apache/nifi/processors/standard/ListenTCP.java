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
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.SupportsBatching;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.lifecycle.OnStopped;
import org.apache.nifi.annotation.lifecycle.OnUnscheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.csv.CSVUtils;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.lookup.KeyValueLookupService;
import org.apache.nifi.processor.DataUnit;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.processor.util.listen.AbstractListenEventBatchingProcessor;
import org.apache.nifi.processor.util.listen.dispatcher.AsyncChannelDispatcher;
import org.apache.nifi.processor.util.listen.dispatcher.ChannelDispatcher;
import org.apache.nifi.processor.util.listen.dispatcher.SocketChannelDispatcher;
import org.apache.nifi.processor.util.listen.event.EventFactory;
import org.apache.nifi.processor.util.listen.event.StandardEvent;
import org.apache.nifi.processor.util.listen.event.StandardEventFactory;
import org.apache.nifi.processor.util.listen.handler.ChannelHandlerFactory;
import org.apache.nifi.processor.util.listen.handler.socket.SocketChannelHandlerFactory;
import org.apache.nifi.processor.util.listen.response.ChannelResponder;
import org.apache.nifi.processor.util.listen.response.ChannelResponse;
import org.apache.nifi.processor.util.listen.response.socket.SocketChannelResponder;
import org.apache.nifi.security.util.SslContextFactory;
import org.apache.nifi.ssl.RestrictedSSLContextService;
import org.apache.nifi.ssl.SSLContextService;

@SupportsBatching
@InputRequirement(InputRequirement.Requirement.INPUT_FORBIDDEN)
@Tags({ "listen", "tcp", "tls", "ssl" })
@CapabilityDescription("Listens for incoming TCP connections and reads data from each connection using a line separator "
        + "as the message demarcator. The default behavior is for each message to produce a single FlowFile, however this can "
        + "be controlled by increasing the Batch Size to a larger value for higher throughput. The Receive Buffer Size must be "
        + "set as large as the largest messages expected to be received, meaning if every 100kb there is a line separator, then " + "the Receive Buffer Size must be greater than 100kb.")
@WritesAttributes({ @WritesAttribute(attribute = ListenTCP.TCP_SENDER, description = "The sending host of the messages."),
        @WritesAttribute(attribute = ListenTCP.TCP_PORT, description = "The sending port the messages were received."),
        @WritesAttribute(attribute = ListenTCP.TCP_MESSAGES, description = "The sending the messages were received."),
        @WritesAttribute(attribute = ListenTCP.TCP_CONTEXT_ID, description = "The sending identifier of the messages."),
        @WritesAttribute(attribute = ListenTCP.TCP_CONTEXT_CHARSET, description = "The sending charset of the messages."),
        @WritesAttribute(attribute = ListenTCP.TCP_RESPONSE_DELIMITER, description = "The sending response delimiter of the messages.") })
public class ListenTCP extends AbstractListenEventBatchingProcessor<StandardEvent> {
    public static final String TCP_SENDER = "tcp.sender";
    public static final String TCP_PORT = "tcp.port";
    public static final String TCP_MESSAGES = "tcp.messages";
    public static final String TCP_CONTEXT_ID = "tcp.context.identifier";
    public static final String TCP_CONTEXT_CHARSET = "tcp.context.charset";
    public static final String TCP_RESPONSE_DELIMITER = "tcp.response.delimiter";

    public static final PropertyDescriptor SSL_CONTEXT_SERVICE = new PropertyDescriptor.Builder().name("SSL Context Service")
            .description("The Controller Service to use in order to obtain an SSL Context. If this property is set, " + "messages will be received over a secure connection.").required(false)
            .identifiesControllerService(RestrictedSSLContextService.class).build();

    public static final PropertyDescriptor CLIENT_AUTH = new PropertyDescriptor.Builder().name("Client Auth")
            .description("The client authentication policy to use for the SSL Context. Only used if an SSL Context Service is provided.").required(false)
            .allowableValues(SSLContextService.ClientAuth.values()).defaultValue(SSLContextService.ClientAuth.REQUIRED.name()).build();

    public static final PropertyDescriptor RESPONDER_CONTEXT_MAP = new PropertyDescriptor.Builder().name("Responder context map")
            .description("The Controller Service to use in order to hold the response of current TCP, If this property is set, " + "messages will be sent over a secure connection.").required(false)
            .identifiesControllerService(KeyValueLookupService.class).build();

    protected volatile int maxBatchSize;
    protected volatile String contextIdentifier;
    protected volatile boolean keepAlive;
    protected volatile String responseDelimiter;

    @Override
    protected List<PropertyDescriptor> getAdditionalProperties() {
        return Arrays.asList(MAX_CONNECTIONS, SSL_CONTEXT_SERVICE, CLIENT_AUTH, RESPONDER_CONTEXT_MAP);
    }

    @Override
    @OnScheduled
    public void onScheduled(ProcessContext context) throws IOException {
        super.onScheduled(context);

        maxBatchSize = context.getProperty(MAX_BATCH_SIZE).asInteger();
        contextIdentifier = UUID.randomUUID().toString();

        responseDelimiter = new String(messageDemarcatorBytes, charset); // reuse the value of property MESSAGE_DELIMITER.

        // if set responder, need keep alive
        final KeyValueLookupService lookupService = context.getProperty(RESPONDER_CONTEXT_MAP).asControllerService(KeyValueLookupService.class);
        keepAlive = (lookupService != null);
    }

    @Override
    @OnUnscheduled
    public void onUnscheduled() {
        if (dispatcher != null && !keepAlive) {
            dispatcher.close();
        }
    }

    @OnStopped
    public void onStopped() {
        // force to stop always
        if (dispatcher != null) {
            dispatcher.close();
        }
    }

    @Override
    protected Collection<ValidationResult> customValidate(final ValidationContext validationContext) {
        final List<ValidationResult> results = new ArrayList<>();

        final String clientAuth = validationContext.getProperty(CLIENT_AUTH).getValue();
        final SSLContextService sslContextService = validationContext.getProperty(SSL_CONTEXT_SERVICE).asControllerService(SSLContextService.class);

        if (sslContextService != null && StringUtils.isBlank(clientAuth)) {
            results.add(new ValidationResult.Builder().explanation("Client Auth must be provided when using TLS/SSL").valid(false).subject("Client Auth").build());
        }

        return results;
    }

    @Override
    protected ChannelDispatcher createDispatcher(final ProcessContext context, final BlockingQueue<StandardEvent> events) throws IOException {

        final int maxConnections = context.getProperty(MAX_CONNECTIONS).asInteger();
        final int bufferSize = context.getProperty(RECV_BUFFER_SIZE).asDataSize(DataUnit.B).intValue();

        // initialize the buffer pool based on max number of connections and the buffer size
        final BlockingQueue<ByteBuffer> bufferPool = createBufferPool(maxConnections, bufferSize);

        // if an SSLContextService was provided then create an SSLContext to pass down to the dispatcher
        SSLContext sslContext = null;
        SslContextFactory.ClientAuth clientAuth = null;

        final SSLContextService sslContextService = context.getProperty(SSL_CONTEXT_SERVICE).asControllerService(SSLContextService.class);
        if (sslContextService != null) {
            final String clientAuthValue = context.getProperty(CLIENT_AUTH).getValue();
            sslContext = sslContextService.createSSLContext(SSLContextService.ClientAuth.valueOf(clientAuthValue));
            clientAuth = SslContextFactory.ClientAuth.valueOf(clientAuthValue);
        }

        final EventFactory<StandardEvent> eventFactory = new StandardEventFactory();
        final ChannelHandlerFactory<StandardEvent<SocketChannel>, AsyncChannelDispatcher> handlerFactory = new SocketChannelHandlerFactory<>();
        return new SocketChannelDispatcher(eventFactory, handlerFactory, bufferPool, events, getLogger(), maxConnections, sslContext, clientAuth, charset);
    }

    @Override
    protected Map<String, String> getAttributes(final FlowFileEventBatch batch) {
        final StandardEvent standardEvent = batch.getEvents().get(0);
        final String sender = standardEvent.getSender();
        final Map<String, String> attributes = new HashMap<>(3);
        attributes.put(TCP_SENDER, sender);
        attributes.put(TCP_PORT, String.valueOf(port));
        attributes.put(TCP_CONTEXT_ID, contextIdentifier);
        attributes.put(TCP_CONTEXT_CHARSET, charset.name());
        attributes.put(TCP_RESPONSE_DELIMITER, responseDelimiter);

        final byte[] data = standardEvent.getData();
        if (data != null && maxBatchSize == 1) { // only add the messages attribute when batch is 1.
            attributes.put(TCP_MESSAGES, new String(data, charset));
        }
        return attributes;
    }

    @Override
    protected String getTransitUri(FlowFileEventBatch batch) {
        return getTransitUri(batch.getEvents().get(0).getSender(), String.valueOf(port));
    }

    @Override
    protected void postProcess(ProcessContext context, ProcessSession session, List<StandardEvent> events) {
        super.postProcess(context, session, events);

        final KeyValueLookupService lookupService = context.getProperty(RESPONDER_CONTEXT_MAP).asControllerService(KeyValueLookupService.class);
        if (lookupService != null) { // if not set, won't register
            lookupService.register(contextIdentifier, new TCPContextEvent(dispatcher, events));
        }
    }

    static String getTransitUri(String sender, String port) {
        final String senderHost = sender.startsWith("/") && sender.length() > 1 ? sender.substring(1) : sender;
        final String transitUri = new StringBuilder().append("tcp").append("://").append(senderHost).append(":").append(port).toString();
        return transitUri;
    }

    static class TCPContextEvent {
        List<StandardEvent> events;
        ChannelDispatcher dispatcher;

        TCPContextEvent(ChannelDispatcher dispatcher, List<StandardEvent> events) {
            super();
            this.dispatcher = dispatcher;
            this.events = events;
        }

    }
}
