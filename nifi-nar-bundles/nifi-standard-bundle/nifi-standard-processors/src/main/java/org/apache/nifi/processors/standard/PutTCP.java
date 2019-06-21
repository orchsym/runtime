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

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.CopyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.behavior.TriggerWhenEmpty;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.DataUnit;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessSessionFactory;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.processor.util.put.AbstractPutEventProcessor;
import org.apache.nifi.processor.util.put.sender.ChannelSender;
import org.apache.nifi.processor.util.put.sender.SocketChannelSender;
import org.apache.nifi.remote.io.socket.SocketChannelInputStream;
import org.apache.nifi.ssl.SSLContextService;
import org.apache.nifi.util.StopWatch;

import javax.net.ssl.SSLContext;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;

/**
 * <p>
 * The PutTCP processor receives a FlowFile and transmits the FlowFile content over a TCP connection to the configured TCP server. By default, the FlowFiles are transmitted over the same TCP
 * connection (or pool of TCP connections if multiple input threads are configured). To assist the TCP server with determining message boundaries, an optional "Outgoing Message Delimiter" string can
 * be configured which is appended to the end of each FlowFiles content when it is transmitted over the TCP connection. An optional "Connection Per FlowFile" parameter can be specified to change the
 * behaviour so that each FlowFiles content is transmitted over a single TCP connection which is opened when the FlowFile is received and closed after the FlowFile has been sent. This option should
 * only be used for low message volume scenarios, otherwise the platform may run out of TCP sockets.
 * </p>
 *
 * <p>
 * This processor has the following required properties:
 * <ul>
 * <li><b>Hostname</b> - The IP address or host name of the destination TCP server.</li>
 * <li><b>Port</b> - The TCP port of the destination TCP server.</li>
 * </ul>
 * </p>
 *
 * <p>
 * This processor has the following optional properties:
 * <ul>
 * <li><b>Connection Per FlowFile</b> - Specifies that each FlowFiles content will be transmitted on a separate TCP connection.</li>
 * <li><b>Idle Connection Expiration</b> - The time threshold after which a TCP sender is deemed eligible for pruning - the associated TCP connection will be closed after this timeout.</li>
 * <li><b>Max Size of Socket Send Buffer</b> - The maximum size of the socket send buffer that should be used. This is a suggestion to the Operating System to indicate how big the socket buffer should
 * be. If this value is set too low, the buffer may fill up before the data can be read, and incoming data will be dropped.</li>
 * <li><b>Outgoing Message Delimiter</b> - A string to append to the end of each FlowFiles content to indicate the end of the message to the TCP server.</li>
 * <li><b>Timeout</b> - The timeout period for determining an error has occurred whilst connecting or sending data.</li>
 * </ul>
 * </p>
 *
 * <p>
 * The following relationships are required:
 * <ul>
 * <li><b>failure</b> - Where to route FlowFiles that failed to be sent.</li>
 * <li><b>success</b> - Where to route FlowFiles after they were successfully sent to the TCP server.</li>
 * </ul>
 * </p>
 *
 */
@CapabilityDescription("The PutTCP processor receives a FlowFile and transmits the FlowFile content over a TCP connection to the configured TCP server. "
        + "By default, the FlowFiles are transmitted over the same TCP connection (or pool of TCP connections if multiple input threads are configured). "
        + "To assist the TCP server with determining message boundaries, an optional \"Outgoing Message Delimiter\" string can be configured which is appended "
        + "to the end of each FlowFiles content when it is transmitted over the TCP connection. An optional \"Connection Per FlowFile\" parameter can be "
        + "specified to change the behaviour so that each FlowFiles content is transmitted over a single TCP connection which is opened when the FlowFile "
        + "is received and closed after the FlowFile has been sent. This option should only be used for low message volume scenarios, otherwise the platform " + "may run out of TCP sockets.")
@InputRequirement(Requirement.INPUT_REQUIRED)
@SeeAlso(ListenTCP.class)
@Tags({ "remote", "egress", "put", "tcp" })
@TriggerWhenEmpty // trigger even when queue is empty so that the processor can check for idle senders to prune.
public class PutTCP extends AbstractPutEventProcessor {

    private static final String PROPERTIES_NIFI_WEB_HTTP_HOST = "nifi.web.http.host";
    private static final String PROPERTIES_NIFI_WEB_HTTP_PORT = "nifi.web.http.port";
    private static final String PROPERTIES_NIFI_WEB_HTTPS_HOST = "nifi.web.https.host";
    private static final String PROPERTIES_NIFI_WEB_HTTPS_PORT = "nifi.web.https.port";
    
    public static final PropertyDescriptor RECEIVE_BUFFER_SIZE = new PropertyDescriptor.Builder()
            .name("receive-buffer-size")
            .displayName("Receive Buffer Size")
            .description("The size of the buffer to receive data in. Can set like 2048 B(2 KB), if not set, then response message will not be handled.")
            .required(false)
            .addValidator(new Validator() {

                @Override
                public ValidationResult validate(String subject, String input, ValidationContext context) {
                    if (StringUtils.isEmpty(input)) {
                        return new ValidationResult.Builder().subject(subject).input(input).explanation("Expression Language Present").valid(true).build();
                    }
                    return StandardValidators.DATA_SIZE_VALIDATOR.validate(subject, input, context);
                }
            })
            .build();

    public static final PropertyDescriptor END_OF_MESSAGE_BYTE = new PropertyDescriptor.Builder()
            .name("end-of-message-byte")
            .displayName("Receive message delimiter byte")
            .description("Byte value which denotes end of message. Must be specified as integer within "
                    + "the valid byte range (-128 thru 127). For example, '13' = Carriage return and '10' = New line. Default '13'."
                    + "Receive Buffer Size property must be set and should be greater than max message size")
            .required(true)
            .defaultValue("13")
            .addValidator(StandardValidators.createLongValidator(-128, 127, true))
            .build();
    
    public static final Relationship REL_RESPONSE = new Relationship.Builder()
            .name("response")
            .description("FlowFiles that are received successfully from the destination are sent out this relationship.")
            .build();
    
    private volatile int receiveBufferSize;
    
    private volatile byte endOfMessageByte;

    /**
     * Creates a concrete instance of a ChannelSender object to use for sending messages over a TCP stream.
     *
     * @param context
     *            - the current process context.
     * @param flowFile
     *            - the FlowFile being processed in this session.
     * @return ChannelSender object.
     */
    @Override
    protected ChannelSender createSender(final ProcessContext context, final FlowFile flowfile) throws IOException {
        final String protocol = TCP_VALUE.getValue();
        final String hostname = context.getProperty(HOSTNAME).evaluateAttributeExpressions(flowfile).getValue();
        final int port = context.getProperty(PORT).evaluateAttributeExpressions(flowfile).asInteger();
        final int timeout = context.getProperty(TIMEOUT).asTimePeriod(TimeUnit.MILLISECONDS).intValue();
        final int bufferSize = context.getProperty(MAX_SOCKET_SEND_BUFFER_SIZE).asDataSize(DataUnit.B).intValue();
        final SSLContextService sslContextService = (SSLContextService) context.getProperty(SSL_CONTEXT_SERVICE).asControllerService();

        SSLContext sslContext = null;
        if (sslContextService != null) {
            sslContext = sslContextService.createSSLContext(SSLContextService.ClientAuth.REQUIRED);
        }
        
        if (context.getProperty(RECEIVE_BUFFER_SIZE).isSet()) {
            this.receiveBufferSize = context.getProperty(RECEIVE_BUFFER_SIZE).asDataSize(DataUnit.B).intValue();
        }
        this.endOfMessageByte = ((byte) context.getProperty(END_OF_MESSAGE_BYTE).asInteger().intValue());

        return createSender(protocol, hostname, port, timeout, bufferSize, sslContext);
    }
    
    protected List<Relationship> getAdditionalRelationships() {
        return  Arrays.asList(REL_RESPONSE);
    }

    /**
     * Creates a Universal Resource Identifier (URI) for this processor. Constructs a URI of the form TCP://< host >:< port > where the host and port
     * values are taken from the configured property values.
     *
     * @param context
     *            - the current process context.
     *
     * @return The URI value as a String.
     */
    @Override
    protected String createTransitUri(final ProcessContext context) {
        final String protocol = TCP_VALUE.getValue();
        final String host = context.getProperty(HOSTNAME).evaluateAttributeExpressions().getValue();
        final String port = context.getProperty(PORT).evaluateAttributeExpressions().getValue();

        return new StringBuilder().append(protocol).append("://").append(host).append(":").append(port).toString();
    }

    /**
     * Get the additional properties that are used by this processor.
     *
     * @return List of PropertyDescriptors describing the additional properties.
     */
    @Override
    protected List<PropertyDescriptor> getAdditionalProperties() {
        return Arrays.asList(CONNECTION_PER_FLOWFILE,
                OUTGOING_MESSAGE_DELIMITER,
                TIMEOUT,
                SSL_CONTEXT_SERVICE,
                RECEIVE_BUFFER_SIZE,
                END_OF_MESSAGE_BYTE,
                CHARSET);
    }

    /**
     * event handler method to handle the FlowFile being forwarded to the Processor by the framework. The FlowFile contents is sent out over a TCP connection using an acquired ChannelSender object. If
     * the FlowFile contents was sent out successfully then the FlowFile is forwarded to the success relationship. If an error occurred then the FlowFile is forwarded to the failure relationship.
     *
     * @param context
     *            - the current process context.
     *
     * @param sessionFactory
     *            - a factory object to obtain a process session.
     */
    @Override
    public void onTrigger(final ProcessContext context, final ProcessSessionFactory sessionFactory) throws ProcessException {
        final ProcessSession session = sessionFactory.createSession();
        final FlowFile flowFile = session.get();
        if (flowFile == null) {
            final PruneResult result = pruneIdleSenders(context.getProperty(IDLE_EXPIRATION).asTimePeriod(TimeUnit.MILLISECONDS).longValue());
            // yield if we closed an idle connection, or if there were no connections in the first place
            if (result.getNumClosed() > 0 || (result.getNumClosed() == 0 && result.getNumConsidered() == 0)) {
                context.yield();
            }
            return;
        }

        ChannelSender sender = acquireSender(context, session, flowFile);
        if (sender == null) {
            return;
        }

        // really shouldn't happen since we know the protocol is TCP here, but this is more graceful so we
        // can cast to a SocketChannelSender later in order to obtain the OutputStream
        if (!(sender instanceof SocketChannelSender)) {
            getLogger().error("Processor can only be used with a SocketChannelSender, but obtained: " + sender.getClass().getCanonicalName());
            context.yield();
            return;
        }

        boolean closeSender = isConnectionPerFlowFile(context);
        try {
            // We might keep the connection open across invocations of the processor so don't auto-close this
            final OutputStream out = ((SocketChannelSender)sender).getOutputStream();
            final String delimiter = getOutgoingMessageDelimiter(context, flowFile);
            String response = null;
            final Charset charSet = Charset.forName(context.getProperty(CHARSET).getValue());

            final StopWatch stopWatch = new StopWatch(true);
            try (final InputStream rawIn = session.read(flowFile);
                 final BufferedInputStream in = new BufferedInputStream(rawIn)) {
                String annotation = context.getAnnotationData();

                if (annotation == null) {
                    //没有关于封包字段的信息，按照默认逻辑发送flowfile数据到tcp连接
                    IOUtils.copy(in, out);
                    if (delimiter != null) {
                        out.write(delimiter.getBytes(charSet), 0, delimiter.length());
                    }
                } else {
                    ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
                    ArrayList<PackingInfo> packingInfos = parsePackingInfos(annotation);
                    //将flowfile的attribute信息按照封包字段结构信息，写入tcp发送buffer
                    handleBufferInfo(byteArrayStream, flowFile, packingInfos, charSet);
                    if (delimiter != null) {
                        byteArrayStream.write(delimiter.getBytes(charSet));
                    }
                    CopyUtils.copy(byteArrayStream.toByteArray(), out);
                }
                out.flush();
                if(receiveBufferSize > 0 && endOfMessageByte > 0 ) {
                    SocketChannel socketChannel = ((SocketChannelSender)sender).getOpenChannel();
                    SocketChannelInputStream sockIn = new SocketChannelInputStream(socketChannel);
                    int count = 0;
                    sockIn.setTimeout(100);
                    byte[] buf = new byte[receiveBufferSize];
                    byte[] bufByte = new byte[1];
                    boolean finished = false;
                    while (!finished) {
                        int n;
                        while (IOUtils.EOF != (n = sockIn.read(bufByte))) {
                            buf[count] = bufByte[0];
                            count ++;
                            if (count >= receiveBufferSize || bufByte[0] == endOfMessageByte) {
                                finished = true;
                                break;
                            }
                        }
                    }
                    byte ar[] = new String(buf).replaceAll("\0", "").getBytes();
                    response = new String(ar,charSet);
                }
                
            } catch (final Exception e) {
                closeSender = true;
                throw e;
            }

            session.getProvenanceReporter().send(flowFile, transitUri, stopWatch.getElapsed(TimeUnit.MILLISECONDS));
            if(response != null && (receiveBufferSize > 0 && endOfMessageByte > 0) ) {
                // transfer to response relationship
                FlowFile ff = processResponse(session, response, charSet);
                ff = session.putAllAttributes(ff, flowFile.getAttributes());
                session.transfer(ff, REL_RESPONSE);
                
            }
            session.transfer(flowFile, REL_SUCCESS);
            session.commit();
        } catch (Exception e) {
            onFailure(context, session, flowFile);
            getLogger().error("Exception while handling a process session, transferring {} to failure.", new Object[] { flowFile }, e);
        } finally {
            if (closeSender) {
                getLogger().debug("Closing sender");
                sender.close();
            } else {
                getLogger().debug("Relinquishing sender");
                relinquishSender(sender);
            }
        }
    }
    
    FlowFile processResponse(ProcessSession session, final String result, final Charset charSet) {

        FlowFile intermediateFlowFile = session.create();

        intermediateFlowFile = session.write(intermediateFlowFile, new OutputStreamCallback() {
            @Override
            public void process(final OutputStream out) throws IOException {
                try {
                    out.write(result.getBytes(charSet));
                } catch (Exception e) {
                    final ComponentLog logger = getLogger();
                    if (null != logger)
                        logger.error("Failed write response data", e);
                    throw new ProcessException(e);
                }
            }
        });

        final Map<String, String> attributes = new HashMap<>();
        return session.putAllAttributes(intermediateFlowFile, attributes);
    }

    /**
     * Event handler method to perform the required actions when a failure has occurred. The FlowFile is penalized, forwarded to the failure relationship and the context is yielded.
     *
     * @param context
     *            - the current process context.
     *
     * @param session
     *            - the current process session.
     * @param flowFile
     *            - the FlowFile that has failed to have been processed.
     */
    protected void onFailure(final ProcessContext context, final ProcessSession session, final FlowFile flowFile) {
        session.transfer(session.penalize(flowFile), REL_FAILURE);
        session.commit();
        context.yield();
    }

    /**
     * Gets the current value of the "Connection Per FlowFile" property.
     *
     * @param context
     *            - the current process context.
     *
     * @return boolean value - true if a connection per FlowFile is specified.
     */
    protected boolean isConnectionPerFlowFile(final ProcessContext context) {
        return context.getProperty(CONNECTION_PER_FLOWFILE).getValue().equalsIgnoreCase("true");
    }

    //根据封包字段信息将flowfile相关的attribute内容写入tcp的发送buffer
    public void handleBufferInfo(ByteArrayOutputStream outputStream, FlowFile flowFile, ArrayList<PackingInfo> packingInfos, Charset charSet) throws IOException{

        Map<String, String> attributes =  flowFile.getAttributes();
        for (int i=0; i<packingInfos.size(); i++) {
            PackingInfo info = packingInfos.get(i);
            int length = info.length;  //字段长度
            byte[] bytes = new byte[length];

            String value = attributes.get(info.name);
            if (value == null) {
                value = "";
            }

            byte[] valueBytes = value.getBytes(charSet);
            int valueBytesLen = valueBytes.length; //内容长度

            if (valueBytesLen >= length) {
                //不拷贝超出字段长度范围的内容
                System.arraycopy(valueBytes, 0, bytes, 0, length);
            } else {
                String align = info.align;
                if (align.equals("align-left")) {
                    //左对齐，默认填充方式，填充在内容之后
                    System.arraycopy(valueBytes, 0, bytes, 0, valueBytesLen);
                    //填充字符，默认填充字符为0
                    char stuffing = info.stuffing;
                    if (stuffing != 0) {
                        for (int j=valueBytesLen; j<length ; j++) {
                            bytes[j] = (byte)stuffing;
                        }
                    }
                } else if (align.equals("align-right")) {
                    //右对齐，填充在内容之前
                    System.arraycopy(valueBytes, 0, bytes, length-valueBytesLen, valueBytesLen);
                    //填充字符，默认填充字符为0
                    char stuffing = info.stuffing;
                    if (stuffing != 0) {
                        for (int j=0; j<length-valueBytesLen ; j++) {
                            bytes[j] = (byte)stuffing;
                        }
                    }
                }
            }
            outputStream.write(bytes);
        }
    }

    public ArrayList<PackingInfo> parsePackingInfos(String info) {

        ArrayList<PackingInfo> packingInfos = new ArrayList();
        Gson gson = new Gson();
        JsonObject infoObject = new JsonParser().parse(info).getAsJsonObject();
        JsonArray jsonInfosArr = infoObject.getAsJsonArray("infos");
        String align = infoObject.get("alignType").getAsString();
        for (int i=0; jsonInfosArr!=null && i<jsonInfosArr.size(); i++) {
            PackingInfo packingInfo = new PackingInfo();
            JsonObject object = (JsonObject) jsonInfosArr.get(i).getAsJsonObject();
            packingInfo.name = object.get("name").getAsString();
            packingInfo.length = object.get("length").getAsInt();
            if(object.has("stuffing")){
                packingInfo.stuffing = object.get("stuffing").getAsCharacter();
            }
            packingInfo.align = align;
            packingInfos.add(packingInfo);
        }
        return packingInfos;
    }
}
