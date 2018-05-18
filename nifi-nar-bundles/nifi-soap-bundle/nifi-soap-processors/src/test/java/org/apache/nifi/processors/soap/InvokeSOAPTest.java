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
package org.apache.nifi.processors.soap;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.impl.common.OMNamespaceImpl;
import org.apache.axiom.om.impl.llom.OMElementImpl;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;

public class InvokeSOAPTest {

    private TestRunner testRunner;

    private static ClientAndServer mockServer;

    @BeforeClass
    public static void setup() {
        mockServer = startClientAndServer(9090);
    }
    
    @AfterClass
    public static void tearDown(){
        mockServer.stop();
    }

    @Before
    public  void init() {
        testRunner = TestRunners.newTestRunner(InvokeSOAP.class);
        mockServer.reset();
    }
    
    @After
    public void after(){
        testRunner.shutdown();
    }

    @Test
    public void testHTTPWithoutUsernamePassword() throws IOException {

        final String xmlBody = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                "<SOAP-ENV:Envelope SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
                "    <SOAP-ENV:Body>\n" +
                "        <ns1:LatLonListZipCodeResponse xmlns:ns1=\"http://graphical.weather.gov/xml/DWMLgen/wsdl/ndfdXML.wsdl\">\n" +
                "            <listLatLonOut xsi:type=\"xsd:string\">&lt;?xml version=&apos;1.0&apos;?&gt;&lt;dwml version=&apos;1.0&apos; xmlns:xsd=&apos;http://www.w3.org/2001/XMLSchema&apos; xmlns:xsi=&apos;http://www.w3.org/2001/XMLSchema-instance&apos; xsi:noNamespaceSchemaLocation=&apos;http://graphical.weather.gov/xml/DWMLgen/schema/DWML.xsd&apos;&gt;&lt;latLonList&gt;35.9153,-79.0838&lt;/latLonList&gt;&lt;/dwml&gt;</listLatLonOut>\n" +
                "        </ns1:LatLonListZipCodeResponse>\n" +
                "    </SOAP-ENV:Body>\n" +
                "</SOAP-ENV:Envelope>";

        mockServer.when(request().withMethod("POST"))
				.respond(response().withBody(xmlBody));

        testRunner.setProperty(InvokeSOAP.ENDPOINT_URL,"http://localhost:9090/test_path");
        testRunner.setProperty(InvokeSOAP.TARGET_NAMESPACE,"http://localhost:9090/test_path.wsdl");
        testRunner.setProperty(InvokeSOAP.METHOD_NAME,"testMethod");
        testRunner.setValidateExpressionUsage(false);
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(InvokeSOAP.REL_SUCCESS,1);
        List<MockFlowFile> flowFileList = testRunner.getFlowFilesForRelationship(InvokeSOAP.REL_SUCCESS);
        assert(null != flowFileList);

        final String expectedBody = "<listLatLonOut xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xsd:string\">&lt;?xml version='1.0'?>&lt;dwml version='1.0' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:noNamespaceSchemaLocation='http://graphical.weather.gov/xml/DWMLgen/schema/DWML.xsd'>&lt;latLonList>35.9153,-79.0838&lt;/latLonList>&lt;/dwml></listLatLonOut>";
        flowFileList.get(0).assertContentEquals(expectedBody.getBytes());

    }

    @Test
    public void testHTTPWithUsernamePassword() throws IOException {

        final String xmlBody = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                "<SOAP-ENV:Envelope SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
                "    <SOAP-ENV:Body>\n" +
                "        <ns1:LatLonListZipCodeResponse xmlns:ns1=\"http://graphical.weather.gov/xml/DWMLgen/wsdl/ndfdXML.wsdl\">\n" +
                "            <listLatLonOut xsi:type=\"xsd:string\">&lt;?xml version=&apos;1.0&apos;?&gt;&lt;dwml version=&apos;1.0&apos; xmlns:xsd=&apos;http://www.w3.org/2001/XMLSchema&apos; xmlns:xsi=&apos;http://www.w3.org/2001/XMLSchema-instance&apos; xsi:noNamespaceSchemaLocation=&apos;http://graphical.weather.gov/xml/DWMLgen/schema/DWML.xsd&apos;&gt;&lt;latLonList&gt;35.9153,-79.0838&lt;/latLonList&gt;&lt;/dwml&gt;</listLatLonOut>\n" +
                "        </ns1:LatLonListZipCodeResponse>\n" +
                "    </SOAP-ENV:Body>\n" +
                "</SOAP-ENV:Envelope>";

        mockServer.when(request().withMethod("POST"))
				.respond(response().withBody(xmlBody));

        testRunner.setProperty(InvokeSOAP.ENDPOINT_URL,"http://localhost:9090/test_path");
        testRunner.setProperty(InvokeSOAP.TARGET_NAMESPACE,"http://localhost:9090/test_path.wsdl");
        testRunner.setProperty(InvokeSOAP.METHOD_NAME,"testMethod");
        testRunner.setProperty(InvokeSOAP.USER_NAME,"username");
        testRunner.setProperty(InvokeSOAP.PASSWORD,"password");
        testRunner.setValidateExpressionUsage(false);
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(InvokeSOAP.REL_SUCCESS,1);
        List<MockFlowFile> flowFileList = testRunner.getFlowFilesForRelationship(InvokeSOAP.REL_SUCCESS);
        assert(null != flowFileList);

        final String expectedBody = "<listLatLonOut xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xsd:string\">&lt;?xml version='1.0'?>&lt;dwml version='1.0' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:noNamespaceSchemaLocation='http://graphical.weather.gov/xml/DWMLgen/schema/DWML.xsd'>&lt;latLonList>35.9153,-79.0838&lt;/latLonList>&lt;/dwml></listLatLonOut>";
        flowFileList.get(0).assertContentEquals(expectedBody.getBytes());

    }
    
    @Test
    public void testExpressionLanuageSupport() throws IOException {

        final String xmlBody = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                "<SOAP-ENV:Envelope SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
                "    <SOAP-ENV:Body>\n" +
                "        <ns1:LatLonListZipCodeResponse xmlns:ns1=\"http://graphical.weather.gov/xml/DWMLgen/wsdl/ndfdXML.wsdl\">\n" +
                "            <listLatLonOut></listLatLonOut>\n" +
                "        </ns1:LatLonListZipCodeResponse>\n" +
                "    </SOAP-ENV:Body>\n" +
                "</SOAP-ENV:Envelope>";

        mockServer.when(request().withMethod("POST"))
                .respond(response().withBody(xmlBody));

        testRunner.setProperty(InvokeSOAP.ENDPOINT_URL,"http://localhost:9090/test_path");
        testRunner.setProperty(InvokeSOAP.TARGET_NAMESPACE,"http://localhost:9090/test_path.wsdl");
        testRunner.setProperty(InvokeSOAP.METHOD_NAME,"${soap.methodName}");
        testRunner.setProperty(InvokeSOAP.USER_NAME,"username");
        testRunner.setProperty(InvokeSOAP.PASSWORD,"password");
        final Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("soap.methodName", "testMethod");
        testRunner.enqueue("test".getBytes(), attributes);
        testRunner.run();

        testRunner.assertTransferCount(InvokeSOAP.REL_SUCCESS,1);
        testRunner.assertTransferCount(InvokeSOAP.REL_ORIGINAL,1);
        List<MockFlowFile> flowFileList = testRunner.getFlowFilesForRelationship(InvokeSOAP.REL_SUCCESS);
        assert(null != flowFileList);

        flowFileList.get(0).assertContentEquals("<listLatLonOut/>".getBytes());

    }
    
    @Test
    public void testFailureRelationshipHandling() throws IOException {

        final String xmlBody = "Service unavailable";

        mockServer.when(request().withMethod("POST"))
				.respond(response().withStatusCode(503).withBody(xmlBody));

        testRunner.setProperty(InvokeSOAP.ENDPOINT_URL,"http://localhost:9090/test_path");
        testRunner.setProperty(InvokeSOAP.TARGET_NAMESPACE,"http://localhost:9090/test_path.wsdl");
        testRunner.setProperty(InvokeSOAP.METHOD_NAME,"testMethod");
        testRunner.enqueue("test".getBytes());
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(InvokeSOAP.REL_FAILURE,1);
        testRunner.assertTransferCount(InvokeSOAP.REL_ORIGINAL,0);
        testRunner.assertTransferCount(InvokeSOAP.REL_SUCCESS,0);
        List<MockFlowFile> flowFileList = testRunner.getFlowFilesForRelationship(InvokeSOAP.REL_FAILURE);
        assert(null != flowFileList);

        flowFileList.get(0).assertContentEquals("test".getBytes());

    }

    @Test
    public void testGeoServiceHTTPWithArguments() throws IOException {


        testRunner.setProperty(InvokeSOAP.ENDPOINT_URL,"https://graphical.weather.gov/xml/SOAP_server/ndfdXMLserver.php");
        testRunner.setProperty(InvokeSOAP.TARGET_NAMESPACE,"https://graphical.weather.gov/xml/DWMLgen/wsdl/ndfdXML.wsdl");
        testRunner.setProperty(InvokeSOAP.METHOD_NAME,"LatLonListZipCode");
        testRunner.setProperty("zipCodeList","27510");
        testRunner.enqueue("test".getBytes());
        testRunner.run();

        testRunner.assertTransferCount(InvokeSOAP.REL_SUCCESS,1);
        testRunner.assertTransferCount(InvokeSOAP.REL_ORIGINAL,1);
        List<MockFlowFile> flowFileList = testRunner.getFlowFilesForRelationship(InvokeSOAP.REL_SUCCESS);
        assert(null != flowFileList);

        final String expectedBody = "<listLatLonOut xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xsd:string\">&lt;?xml version='1.0'?>&lt;dwml version='1.0' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:noNamespaceSchemaLocation='https://graphical.weather.gov/xml/DWMLgen/schema/DWML.xsd'>&lt;latLonList>35.9153,-79.0838&lt;/latLonList>&lt;/dwml></listLatLonOut>";
        flowFileList.get(0).assertContentEquals(expectedBody.getBytes());
        
        flowFileList = testRunner.getFlowFilesForRelationship(InvokeSOAP.REL_ORIGINAL);
        assert(null != flowFileList);
        flowFileList.get(0).assertContentEquals("test".getBytes());

    }

    @Test
    public void testRelationships(){
        InvokeSOAP getSOAP = new InvokeSOAP();
        Set<Relationship> relationshipSet = getSOAP.getRelationships();
        assert(null != relationshipSet);
        assert(3 == relationshipSet.size());
    }
    
    @Test
    public void testGetSoapMethod(){

        final String namespaceUrl = "http://localhost.com/stockquote.wsdl";
        final String namespacePrefix = "nifi";
        final String localName = "testMethod";
        OMElement expectedElement = new OMElementImpl();
        expectedElement.setNamespace(new OMNamespaceImpl(namespaceUrl,namespacePrefix));
        expectedElement.setLocalName(localName);

        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNamespace = fac.createOMNamespace(namespaceUrl,namespacePrefix);

        InvokeSOAP getSOAP = new InvokeSOAP();
        OMElement element = getSOAP.getSoapMethod(fac,omNamespace,"testMethod");
        assert(null != element);
        assert(namespaceUrl.contentEquals(element.getNamespaceURI()));
        assert(localName.contentEquals(element.getLocalName()));

    }

    @Test
    public void testAddArguments(){
        //addArgumentsToMethod(ProcessContext context, OMFactory fac, OMNamespace omNamespace, OMElement method)
        final String namespaceUrl = "http://localhost.com/stockquote.wsdl";
        final String namespacePrefix = "nifi";
        final String localName = "testMethod";
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNamespace = fac.createOMNamespace(namespaceUrl,namespacePrefix);
        OMElement expectedElement = new OMElementImpl();
        expectedElement.setNamespace(new OMNamespaceImpl(namespaceUrl,namespacePrefix));
        expectedElement.setLocalName(localName);


        PropertyDescriptor arg1 = new PropertyDescriptor
                .Builder()
                .name("Argument1")
                .defaultValue("60000")
                .description("The timeout value to use waiting to establish a connection to the web service")
                .dynamic(true)
                .expressionLanguageSupported(false)
                .build();

        testRunner.setProperty(arg1,"111");

        InvokeSOAP getSOAP = new InvokeSOAP();
        getSOAP.addArgumentsToMethod(null, testRunner.getProcessContext(),fac,omNamespace,expectedElement);
        Iterator<OMElement> childItr = expectedElement.getChildElements();
        assert(null != childItr);
        assert(childItr.hasNext());
        assert(arg1.getName().contentEquals(childItr.next().getLocalName()));
        assert(!childItr.hasNext());

    }

    @Test
    public void testProcessResult() throws IOException {
        final String xmlBody = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                "<SOAP-ENV:Envelope SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
                "    <SOAP-ENV:Body>\n" +
                "        <ns1:LatLonListZipCodeResponse xmlns:ns1=\"http://graphical.weather.gov/xml/DWMLgen/wsdl/ndfdXML.wsdl\">\n" +
                "            <listLatLonOut xsi:type=\"xsd:string\">&lt;?xml version=&apos;1.0&apos;?&gt;&lt;dwml version=&apos;1.0&apos; xmlns:xsd=&apos;http://www.w3.org/2001/XMLSchema&apos; xmlns:xsi=&apos;http://www.w3.org/2001/XMLSchema-instance&apos; xsi:noNamespaceSchemaLocation=&apos;http://graphical.weather.gov/xml/DWMLgen/schema/DWML.xsd&apos;&gt;&lt;latLonList&gt;35.9153,-79.0838&lt;/latLonList&gt;&lt;/dwml&gt;</listLatLonOut>\n" +
                "        </ns1:LatLonListZipCodeResponse>\n" +
                "    </SOAP-ENV:Body>\n" +
                "</SOAP-ENV:Envelope>";

        final String namespaceUrl = "http://localhost.com/stockquote.wsdl";
        final String namespacePrefix = "nifi";
        final String localName = "testMethod";
        OMElement expectedElement = new OMElementImpl();
        expectedElement.setNamespace(new OMNamespaceImpl(namespaceUrl,namespacePrefix));
        expectedElement.setLocalName(localName);
        OMElementImpl childElement = new OMElementImpl();
        childElement.setText(xmlBody);
        expectedElement.addChild(childElement);

        InvokeSOAP getSOAP = new InvokeSOAP();
        FlowFile flowFile = getSOAP.processSoapRequest(testRunner.getProcessSessionFactory().createSession(),expectedElement);
        assert(null != flowFile);
        ((MockFlowFile)flowFile).assertContentEquals(xmlBody.getBytes());

    }

}
