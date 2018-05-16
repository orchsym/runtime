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
import static org.mockito.Mockito.*;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.nifi.lookup.CommonKeyValueLookupService;
import org.apache.nifi.lookup.KeyValueLookupService;
import org.apache.nifi.processor.util.listen.event.StandardEvent;
import org.apache.nifi.processor.util.listen.response.ChannelResponder;
import org.apache.nifi.processors.standard.ListenTCP.TCPContextEvent;
import org.apache.nifi.provenance.ProvenanceEventType;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;

public class TestHandleTCPResponse {
    TestRunner runner;
    KeyValueLookupService service;

    @Before
    public void before() throws Exception {
        runner = TestRunners.newTestRunner(HandleTCPResponse.class);

        service = new CommonKeyValueLookupService();
        runner.addControllerService("tcp-context-map", service);
        runner.enableControllerService(service);
        runner.setProperty(HandleTCPResponse.RESPONDER_CONTEXT_MAP, "tcp-context-map");

        runner.setProperty(HandleTCPResponse.RESPONSE_TEXT, "got");
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
    public void testWithoutResponderEvents() {
        final Map<String, String> attributes = new HashMap<>();
        attributes.put(ListenTCP.TCP_CONTEXT_ID, UUID.randomUUID().toString());
        String id = UUID.randomUUID().toString();
        service.register(id, new TCPContextEvent(null, Collections.emptyList()));

        runner.enqueue("hello".getBytes(), attributes);
        runner.run();

        runner.assertAllFlowFilesTransferred(HandleTCPResponse.REL_FAILURE, 1);
    }

    @Test
    public void testResponder() throws InitializationException {
        String id = UUID.randomUUID().toString();

        StandardEvent event = mock(StandardEvent.class);
        ChannelResponder responder = mock(ChannelResponder.class);
        when(event.getResponder()).thenReturn(responder);

        service.register(id, new TCPContextEvent(null, Arrays.asList(event)));

        final Map<String, String> attributes = new HashMap<>();
        attributes.put(ListenTCP.TCP_SENDER, "Test");
        attributes.put(ListenTCP.TCP_PORT, "8800");
        attributes.put(ListenTCP.TCP_CONTEXT_ID, id);
        attributes.put(ListenTCP.TCP_CONTEXT_CHARSET, StandardCharsets.UTF_8.name());
        runner.enqueue("hello".getBytes(), attributes);
        runner.run();

        runner.assertAllFlowFilesTransferred(HandleTCPResponse.REL_SUCCESS, 1);
        assertTrue(runner.getProvenanceEvents().size() == 1);

        assertEquals(ProvenanceEventType.SEND, runner.getProvenanceEvents().get(0).getEventType());
        assertEquals("tcp://Test:8800", runner.getProvenanceEvents().get(0).getTransitUri());
    }
}
