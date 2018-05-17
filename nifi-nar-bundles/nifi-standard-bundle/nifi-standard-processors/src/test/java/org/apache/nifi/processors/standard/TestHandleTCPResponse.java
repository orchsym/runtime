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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.lookup.CommonKeyValueLookupService;
import org.apache.nifi.lookup.KeyValueLookupService;
import org.apache.nifi.processor.util.listen.event.StandardEvent;
import org.apache.nifi.processor.util.listen.response.ChannelResponder;
import org.apache.nifi.processor.util.listen.response.ChannelResponse;
import org.apache.nifi.processor.util.listen.response.socket.SocketChannelResponder;
import org.apache.nifi.processors.standard.HandleTCPResponse.ResponseType;
import org.apache.nifi.processors.standard.ListenTCP.TCPContextEvent;
import org.apache.nifi.provenance.ProvenanceEventType;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;

public class TestHandleTCPResponse {
    static class MockChannelResponder implements ChannelResponder {
        final List<ChannelResponse> responses = new ArrayList<>();

        @Override
        public SelectableChannel getChannel() {
            return null;
        }

        @Override
        public List<ChannelResponse> getResponses() {
            return responses;
        }

        @Override
        public void addResponse(ChannelResponse response) {
            responses.add(response);
        }

        @Override
        public void respond() throws IOException {
            //
        }

    }

    TestRunner runner;
    KeyValueLookupService service;

    @Before
    public void before() throws Exception {
        runner = TestRunners.newTestRunner(HandleTCPResponse.class);

        service = new CommonKeyValueLookupService();
        runner.addControllerService("tcp-context-map", service);
        runner.enableControllerService(service);
        runner.setProperty(HandleTCPResponse.RESPONDER_CONTEXT_MAP, "tcp-context-map");

    }

    @Test
    public void testMissingContextId() {
        runner.enqueue("hello".getBytes(), Collections.emptyMap());
        runner.run();
        runner.assertAllFlowFilesTransferred(HandleTCPResponse.REL_FAILURE, 1);
    }

    @Test
    public void testMissingContextMap() {
        final Map<String, String> attributes = new HashMap<>();
        attributes.put(ListenTCP.TCP_CONTEXT_ID, UUID.randomUUID().toString());

        runner.enqueue("hello".getBytes(), attributes);
        runner.run();
        runner.assertAllFlowFilesTransferred(HandleTCPResponse.REL_FAILURE, 1);
    }

    @Test
    public void testEmptyResponderEvents() {
        final Map<String, String> attributes = new HashMap<>();
        attributes.put(ListenTCP.TCP_CONTEXT_ID, UUID.randomUUID().toString());
        String id = UUID.randomUUID().toString();
        service.register(id, new TCPContextEvent(null, Collections.emptyList()));

        runner.enqueue("hello".getBytes(), attributes);
        runner.run();

        runner.assertAllFlowFilesTransferred(HandleTCPResponse.REL_FAILURE, 1);
    }

    @Test
    public void testResponder() throws Exception {
        String id = UUID.randomUUID().toString();

        StandardEvent event = new StandardEvent("Test", null, new MockChannelResponder());
        service.register(id, new TCPContextEvent(null, Arrays.asList(event)));

        final Map<String, String> attributes = new HashMap<>();
        attributes.put(ListenTCP.TCP_SENDER, "Test");
        attributes.put(ListenTCP.TCP_PORT, "8800");
        attributes.put(ListenTCP.TCP_CONTEXT_ID, id);
        attributes.put(ListenTCP.TCP_CONTEXT_CHARSET, StandardCharsets.UTF_8.name());
        runner.enqueue("hello".getBytes(), attributes);
        runner.run();

        final List<MockFlowFile> outputFlowFiles = runner.getFlowFilesForRelationship(HandleTCPResponse.REL_SUCCESS);
        final List<MockFlowFile> originalFlowFiles = runner.getFlowFilesForRelationship(HandleTCPResponse.REL_ORIGINAL);
        assertEquals(1, outputFlowFiles.size());
        assertEquals(1, originalFlowFiles.size());

        assertTrue(runner.getProvenanceEvents().size() == 1);
        assertEquals(ProvenanceEventType.SEND, runner.getProvenanceEvents().get(0).getEventType());
        assertEquals("tcp://Test:8800", runner.getProvenanceEvents().get(0).getTransitUri());
    }

    @Test
    public void testFlowType_withoutDelimiter() throws Exception {
        runner.setProperty(HandleTCPResponse.RESPONSE_TYPE, ResponseType.Flow.name());

        StandardEvent event = doTestAndReturnResponder(null);

        MockChannelResponder responder = (MockChannelResponder) event.getResponder();
        assertEquals(1, responder.getResponses().size());
        assertEquals("hello", new String(responder.getResponses().get(0).toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void testFlowType_withFlowDelimter() throws Exception {
        runner.setProperty(HandleTCPResponse.RESPONSE_TYPE, ResponseType.Flow.name());

        StandardEvent event = doTestAndReturnResponder("\n");

        MockChannelResponder responder = (MockChannelResponder) event.getResponder();
        assertEquals(1, responder.getResponses().size());
        assertEquals("hello\n", new String(responder.getResponses().get(0).toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void testFlowType_withAttrDelimter() throws Exception {
        runner.setProperty(HandleTCPResponse.RESPONSE_TYPE, ResponseType.Flow.name());
        runner.setProperty(HandleTCPResponse.RESPONSE_DELIMITER, "\r");

        StandardEvent event = doTestAndReturnResponder(null);

        MockChannelResponder responder = (MockChannelResponder) event.getResponder();
        assertEquals(1, responder.getResponses().size());
        assertEquals("hello\r", new String(responder.getResponses().get(0).toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void testFlowType_withDifferentDelimters() throws Exception {
        runner.setProperty(HandleTCPResponse.RESPONSE_TYPE, ResponseType.Flow.name());
        runner.setProperty(HandleTCPResponse.RESPONSE_DELIMITER, "\r");

        StandardEvent event = doTestAndReturnResponder("\t");

        MockChannelResponder responder = (MockChannelResponder) event.getResponder();
        assertEquals(1, responder.getResponses().size());
        assertEquals("hello\r", new String(responder.getResponses().get(0).toByteArray(), StandardCharsets.UTF_8));

        runner.getFlowFilesForRelationship(HandleTCPResponse.REL_ORIGINAL).get(0).assertAttributeEquals(ListenTCP.TCP_RESPONSE_DELIMITER, "\t");
        runner.getFlowFilesForRelationship(HandleTCPResponse.REL_SUCCESS).get(0).assertAttributeEquals(ListenTCP.TCP_RESPONSE_DELIMITER, "\r");
    }

    @Test
    public void testTextType_withoutDelimiter() throws Exception {
        runner.setProperty(HandleTCPResponse.RESPONSE_TYPE, ResponseType.Text.name());
        runner.setProperty(HandleTCPResponse.RESPONSE_TEXT, "abcd");

        StandardEvent event = doTestAndReturnResponder(null);

        MockChannelResponder responder = (MockChannelResponder) event.getResponder();
        assertEquals(1, responder.getResponses().size());
        assertEquals("abcd", new String(responder.getResponses().get(0).toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void testTextType_withAttrDelimiter() throws Exception {
        runner.setProperty(HandleTCPResponse.RESPONSE_TYPE, ResponseType.Text.name());
        runner.setProperty(HandleTCPResponse.RESPONSE_TEXT, "abcd");
        runner.setProperty(HandleTCPResponse.RESPONSE_DELIMITER, "\t");

        StandardEvent event = doTestAndReturnResponder(null);

        MockChannelResponder responder = (MockChannelResponder) event.getResponder();
        assertEquals(1, responder.getResponses().size());
        assertEquals("abcd\t", new String(responder.getResponses().get(0).toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void testTextType_withFlowDelimiterAttr() throws Exception {
        runner.setProperty(HandleTCPResponse.RESPONSE_TYPE, ResponseType.Text.name());
        runner.setProperty(HandleTCPResponse.RESPONSE_TEXT, "abcd");

        StandardEvent event = doTestAndReturnResponder("\n");

        MockChannelResponder responder = (MockChannelResponder) event.getResponder();
        assertEquals(1, responder.getResponses().size());
        assertEquals("abcd\n", new String(responder.getResponses().get(0).toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void testTextType_withDifferentDelimiters() throws Exception {
        runner.setProperty(HandleTCPResponse.RESPONSE_TYPE, ResponseType.Text.name());
        runner.setProperty(HandleTCPResponse.RESPONSE_TEXT, "abcd");
        runner.setProperty(HandleTCPResponse.RESPONSE_DELIMITER, "\t");


        StandardEvent event = doTestAndReturnResponder("\n");

        MockChannelResponder responder = (MockChannelResponder) event.getResponder();
        assertEquals(1, responder.getResponses().size());
        assertEquals("abcd\t", new String(responder.getResponses().get(0).toByteArray(), StandardCharsets.UTF_8));
        
        runner.getFlowFilesForRelationship(HandleTCPResponse.REL_ORIGINAL).get(0).assertAttributeEquals(ListenTCP.TCP_RESPONSE_DELIMITER, "\n");
        runner.getFlowFilesForRelationship(HandleTCPResponse.REL_SUCCESS).get(0).assertAttributeEquals(ListenTCP.TCP_RESPONSE_DELIMITER, "\t");
  
    }

    private StandardEvent doTestAndReturnResponder(String delimiter) {
        String id = UUID.randomUUID().toString();

        StandardEvent event = new StandardEvent("abc", null, new MockChannelResponder());
        service.register(id, new TCPContextEvent(null, Arrays.asList(event)));

        final Map<String, String> attributes = new HashMap<>();
        attributes.put(ListenTCP.TCP_SENDER, "Test");
        attributes.put(ListenTCP.TCP_PORT, "8800");
        attributes.put(ListenTCP.TCP_CONTEXT_ID, id);
        attributes.put(ListenTCP.TCP_CONTEXT_CHARSET, StandardCharsets.UTF_8.name());
        if (StringUtils.isNotEmpty(delimiter))
            attributes.put(ListenTCP.TCP_RESPONSE_DELIMITER, delimiter);

        runner.enqueue("hello".getBytes(), attributes);
        runner.run();

        final List<MockFlowFile> outputFlowFiles = runner.getFlowFilesForRelationship(HandleTCPResponse.REL_SUCCESS);
        final List<MockFlowFile> originalFlowFiles = runner.getFlowFilesForRelationship(HandleTCPResponse.REL_ORIGINAL);
        assertEquals(1, outputFlowFiles.size());
        assertEquals(1, originalFlowFiles.size());

        final MockFlowFile outputFlowFile = outputFlowFiles.get(0);
        outputFlowFile.assertAttributeEquals(HandleTCPResponse.TCP_RESPONSE_SENDER, "abc");
        outputFlowFile.assertAttributeEquals(ListenTCP.TCP_SENDER, "Test");
        outputFlowFile.assertAttributeExists(ListenTCP.TCP_CONTEXT_ID);
        outputFlowFile.assertAttributeEquals(ListenTCP.TCP_PORT, "8800");
        outputFlowFile.assertAttributeEquals(ListenTCP.TCP_CONTEXT_CHARSET, "UTF-8");
        outputFlowFile.assertContentEquals("hello");

        final MockFlowFile originalFlowFile = originalFlowFiles.get(0);
        originalFlowFile.assertAttributeEquals(ListenTCP.TCP_SENDER, "Test");
        originalFlowFile.assertAttributeEquals(ListenTCP.TCP_PORT, "8800");
        originalFlowFile.assertAttributeEquals(ListenTCP.TCP_CONTEXT_CHARSET, "UTF-8");
        originalFlowFile.assertAttributeExists(ListenTCP.TCP_CONTEXT_ID);
        originalFlowFile.assertContentEquals("hello");

        assertEquals(ProvenanceEventType.SEND, runner.getProvenanceEvents().get(0).getEventType());
        assertEquals("tcp://Test:8800", runner.getProvenanceEvents().get(0).getTransitUri());

        return event;
    }

    /**
     * Should be IllegalArgumentException, but use mock test runner, so will be AssertionError
     */
    @Test(expected = AssertionError.class)
    public void testTextType_withoutResponseText() throws Exception {
        runner.setProperty(HandleTCPResponse.RESPONSE_TYPE, ResponseType.Text.name());
        runner.setProperty(HandleTCPResponse.RESPONSE_TEXT, "");

        String id = UUID.randomUUID().toString();

        StandardEvent event = mock(StandardEvent.class);
        SocketChannel channel = mock(SocketChannel.class);
        SocketChannelResponder responder = new SocketChannelResponder(channel);
        when(event.getResponder()).thenReturn(responder);

        service.register(id, new TCPContextEvent(null, Arrays.asList(event)));

        final Map<String, String> attributes = new HashMap<>();
        attributes.put(ListenTCP.TCP_SENDER, "Test");
        attributes.put(ListenTCP.TCP_PORT, "8800");
        attributes.put(ListenTCP.TCP_CONTEXT_ID, id);
        attributes.put(ListenTCP.TCP_CONTEXT_CHARSET, StandardCharsets.UTF_8.name());
        runner.enqueue("hello".getBytes(), attributes);
        runner.run();
    }

}
