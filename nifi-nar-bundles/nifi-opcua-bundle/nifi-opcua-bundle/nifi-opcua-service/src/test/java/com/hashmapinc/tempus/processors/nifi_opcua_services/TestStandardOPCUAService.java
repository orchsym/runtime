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
package com.hashmapinc.tempus.processors.nifi_opcua_services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashmap.tempus.opc.test.server.TestServer;
import com.hashmapinc.tempus.processors.StandardOPCUAService;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opcfoundation.ua.builtintypes.ExpandedNodeId;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.core.Identifiers;
import static org.junit.Assert.*;
import java.util.*;
import java.util.regex.Pattern;

public class TestStandardOPCUAService {

    TestServer server;

    @Before
    public void init() {
        try {
            server = new TestServer(45678, "user", "test");
        } catch (Exception e) {
            e.printStackTrace();
        }
        server.start();
    }

    @Test
    public void testMissingPropertyValues() throws InitializationException {
        final TestRunner runner = TestRunners.newTestRunner(TestProcessor.class);
        final StandardOPCUAService service = new StandardOPCUAService();
        final Map<String, String> properties = new HashMap<String, String>();
        runner.addControllerService("test-bad1", service, properties);
        runner.assertNotValid(service);
    }

    @Test
    public void testGetNodeListInsecure() throws InitializationException {
        final TestRunner runner = TestRunners.newTestRunner(TestProcessor.class);
        final StandardOPCUAService service = new StandardOPCUAService();

        StringBuilder stringBuilder = new StringBuilder();

        runner.addControllerService("hi", service);

        runner.setProperty(service, StandardOPCUAService.APPLICATION_NAME, "nifi");
        runner.setProperty(service, StandardOPCUAService.ENDPOINT, "opc.tcp://127.0.0.1:45678/test");
        runner.setProperty(service, StandardOPCUAService.SECURITY_POLICY, "NONE");
        runner.setProperty(service, StandardOPCUAService.AUTH_POLICY, "Anon");

        runner.enableControllerService(service);
        List<ExpandedNodeId> ids = new ArrayList<>();
        ids.add(new ExpandedNodeId((Identifiers.RootFolder)));
        Pattern pattern = Pattern.compile("[^\\.].*");
        stringBuilder.append(service.getNameSpace("No", 3, pattern, new UnsignedInteger(1000)));

        String result = stringBuilder.toString();

        runner.assertValid();

        assertNotNull(result);
        assertNotEquals(result.length(), 0);
    }

    @Test
    public void testGetNodeListSecure() throws InitializationException {
        final TestRunner runner = TestRunners.newTestRunner(TestProcessor.class);
        final StandardOPCUAService service = new StandardOPCUAService();

        StringBuilder stringBuilder = new StringBuilder();

        runner.addControllerService("hi", service);

        runner.setProperty(service, StandardOPCUAService.APPLICATION_NAME, "nifi");
        runner.setProperty(service, StandardOPCUAService.ENDPOINT, "opc.tcp://127.0.0.1:45678/test");
        runner.setProperty(service, StandardOPCUAService.SECURITY_POLICY, "NONE");
        runner.setProperty(service, StandardOPCUAService.AUTH_POLICY, "Username");
        runner.setProperty(service, StandardOPCUAService.USERNAME, "user");
        runner.setProperty(service, StandardOPCUAService.PASSWORD, "test");
        runner.enableControllerService(service);

        List<ExpandedNodeId> ids = new ArrayList<>();
        ids.add(new ExpandedNodeId((Identifiers.RootFolder)));
        Pattern pattern = Pattern.compile("[^\\.].*");
        stringBuilder.append(service.getNameSpace("No", 3, pattern, new UnsignedInteger(1000)));

        String result = stringBuilder.toString();

        runner.assertValid();

        assertNotNull(result);
        assertNotEquals(result.length(), 0);
    }


    @Test
    public void testGetDataTempusDeviceInsecure() throws InitializationException {

        final TestRunner runner = TestRunners.newTestRunner(TestProcessor.class);
        final StandardOPCUAService service = new StandardOPCUAService();

        StringBuilder stringBuilder = new StringBuilder();

        runner.addControllerService("hi", service);

        runner.setProperty(service, StandardOPCUAService.APPLICATION_NAME, "nifi");
        runner.setProperty(service, StandardOPCUAService.ENDPOINT, "opc.tcp://127.0.0.1:45678/test");
        runner.setProperty(service, StandardOPCUAService.SECURITY_POLICY, "NONE");
        runner.setProperty(service, StandardOPCUAService.AUTH_POLICY, "Anon");

        runner.enableControllerService(service);
        List<ExpandedNodeId> ids = new ArrayList<>();
        ids.add(new ExpandedNodeId((Identifiers.RootFolder)));
        Pattern pattern = Pattern.compile("ns=1;s=VendorServerInfo");
        stringBuilder.append(service.getNameSpace("No", 3, pattern, new UnsignedInteger(1000)));


        String result = stringBuilder.toString();
        assertNotNull(result);
        String [] tagNames = result.split("\n");
        List<String> tagList = Arrays.asList(tagNames);

        assertNotEquals(tagList.size(), 0);

        byte[] tempusJSON = service.getValue( tagList , "SourceTimestamp", "false",
                                                "", "TEMPUS", true,
                                                "Device", "");

        String jsonValues = new String(tempusJSON);
        assertNotNull(tempusJSON);


        runner.assertValid();
        try {

            ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(jsonValues);

        } catch (Exception ex) {  fail(ex.getMessage()); }

    }


    @Test
    public void testGetDataTempusGatewayInsecure() throws InitializationException {

        final TestRunner runner = TestRunners.newTestRunner(TestProcessor.class);
        final StandardOPCUAService service = new StandardOPCUAService();

        StringBuilder stringBuilder = new StringBuilder();

        runner.addControllerService("hi", service);

        runner.setProperty(service, StandardOPCUAService.APPLICATION_NAME, "nifi");
        runner.setProperty(service, StandardOPCUAService.ENDPOINT, "opc.tcp://127.0.0.1:45678/test");
        runner.setProperty(service, StandardOPCUAService.SECURITY_POLICY, "NONE");
        runner.setProperty(service, StandardOPCUAService.AUTH_POLICY, "Anon");

        runner.enableControllerService(service);
        List<ExpandedNodeId> ids = new ArrayList<>();
        ids.add(new ExpandedNodeId((Identifiers.RootFolder)));
        Pattern pattern = Pattern.compile("ns=1;s=VendorServerInfo");
        stringBuilder.append(service.getNameSpace("No", 3, pattern, new UnsignedInteger(1000)));

        String result = stringBuilder.toString();
        assertNotNull(result);
        String [] tagNames = result.split("\n");
        List<String> tagList = Arrays.asList(tagNames);

        assertNotEquals(tagList.size(), 0);

        byte[] tempusJSON = service.getValue( tagList , "SourceTimestamp", "false",
                "", "TEMPUS", true,
                "Gateway", "Server 123");

        String jsonValues = new String(tempusJSON);
        assertNotNull(tempusJSON);

        try {

            ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(jsonValues);

        } catch (Exception ex) {  fail(ex.getMessage()); }

        runner.assertValid();
    }

    /*@Test
    public void testNodeListFromMultipleServers() throws InitializationException, Exception {
        try
        {
            TestServer secondServer = new TestServer(12345);

            //First Server
            final TestRunner runner = TestRunners.newTestRunner(TestProcessor.class);
            final StandardOPCUAService service = new StandardOPCUAService();

            StringBuilder stringBuilder = new StringBuilder();

            runner.addControllerService("OPC1", service);

            runner.setProperty(service, StandardOPCUAService.APPLICATION_NAME, "nifi");
            runner.setProperty(service, StandardOPCUAService.ENDPOINT, "opc.tcp://127.0.0.1:45678/test");
            runner.setProperty(service, StandardOPCUAService.SECURITY_POLICY, "NONE");

            runner.enableControllerService(service);

            //Second Server
            final TestRunner secondRunner = TestRunners.newTestRunner(TestProcessor.class);
            final StandardOPCUAService secondService = new StandardOPCUAService();

            secondRunner.addControllerService("OPC2", secondService);

            secondRunner.setProperty(secondService, StandardOPCUAService.APPLICATION_NAME, "nifi");
            secondRunner.setProperty(secondService, StandardOPCUAService.ENDPOINT, "opc.tcp://127.0.0.1:12345/test");
            secondRunner.setProperty(secondService, StandardOPCUAService.SECURITY_POLICY, "NONE");

            secondRunner.enableControllerService(secondService);

            StringBuilder secondStringBuilder = new StringBuilder();

            //calling nodelist for first server
            List<ExpandedNodeId> ids = new ArrayList<>();
            ids.add(new ExpandedNodeId((Identifiers.RootFolder)));
            stringBuilder.append(service.getNameSpace("No", 3, ids, new UnsignedInteger(1000)));

            String result = stringBuilder.toString();

            runner.assertValid();

            assertNotNull(result);
            assertNotEquals(result.length(), 0);


            //calling nodeList for second server
            List<ExpandedNodeId> idsList = new ArrayList<>();
            idsList.add(new ExpandedNodeId((Identifiers.RootFolder)));
            stringBuilder.append(secondService.getNameSpace("No", 3, idsList, new UnsignedInteger(1000)));

            String secondResult = stringBuilder.toString();

            secondRunner.assertValid();

            assertNotNull(secondResult);
            assertNotEquals(secondResult.length(), 0);

        }catch(Exception e)
        {
            //todo
            throw e;
        }

    }*/

    @After
    public void shutdown(){
        server.stop();
    }

}
