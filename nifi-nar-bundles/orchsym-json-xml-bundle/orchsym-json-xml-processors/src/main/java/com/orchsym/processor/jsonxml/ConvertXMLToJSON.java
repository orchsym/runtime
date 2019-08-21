/*
 * Licensed to the Orchsym Runtime under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * 
 * this file to You under the Orchsym License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * https://github.com/orchsym/runtime/blob/master/orchsym/LICENSE
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orchsym.processor.jsonxml;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.util.StopWatch;
import org.json.JSONObject;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Lu JB
 *
 */
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@Marks(categories = { "Convert & Control/Convert" }, createdDate = "2019-1-15")
@Tags({ "Convert", "JSON", "XML", "XML TO JSON" })
@CapabilityDescription("Provide the ability of Convert XML format contents to JSON format contents from the incoming flowfile")

public class ConvertXMLToJSON extends AbstractProcessor {

    public static final String JSON_ATTRIBUTE_NAME = "JSONAttributes";

    public static final String DESTINATION_ATTRIBUTE = "flowfile-attribute";
    public static final String DESTINATION_CONTENT = "flowfile-content";
    public static final String APPLICATION_JSON = "application/json";

    protected static final PropertyDescriptor XML_PATH_EXPRESSION = new PropertyDescriptor.Builder()//
            .name("xpath-expression")//
            .displayName("XPath Expression")//
            .description("The XPath expression to extract the contents").required(false).addValidator(StandardValidators.NON_EMPTY_VALIDATOR).expressionLanguageSupported(ExpressionLanguageScope.NONE)
            .build();

    protected static final PropertyDescriptor XML_ATTRIBUTE_MARK = new PropertyDescriptor.Builder()//
            .name("xml-attribute-mark")//
            .displayName("XML Attribute Mark")//
            .description("Set the prefix of XML attribute name when extract XML attributes")//
            .required(false)//
            .addValidator(Validator.VALID).defaultValue("@")//
            .build();

    protected static final PropertyDescriptor XML_CONTENT_KEY_NAME = new PropertyDescriptor.Builder()//
            .name("xml-content-key-name")//
            .displayName("XML Content Key Name")//
            .description("Set the key name of the content, which extract from the XML node text.")//
            .required(false)//
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR).defaultValue("content")//
            .build();

    public static final PropertyDescriptor DESTINATION = new PropertyDescriptor.Builder().name("Destination")
            .description("Control if JSON value is written as a new flowfile attribute '" + JSON_ATTRIBUTE_NAME + "' "
                    + "or written in the flowfile content. Writing to flowfile content will overwrite any " + "existing flowfile content.")
            .required(true).allowableValues(DESTINATION_ATTRIBUTE, DESTINATION_CONTENT).defaultValue(DESTINATION_ATTRIBUTE).build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()//
            .name("success") //
            .description("All FlowFiles that are converted will be routed to success")//
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()//
            .name("failure")//
            .description("FlowFiles are routed to this relationship when the conversion failed; like, if the FlowFile is not valid format.")//
            .build();

    protected List<PropertyDescriptor> properties;
    protected Set<Relationship> relationships;

    private String xpathExpression;
    private boolean destinationContent;
    private String attributeMark;
    private String contentKeyName;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(XML_ATTRIBUTE_MARK);
        properties.add(XML_CONTENT_KEY_NAME);
        properties.add(XML_PATH_EXPRESSION);
        properties.add(DESTINATION);
        this.properties = Collections.unmodifiableList(properties);

        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    @OnScheduled
    public void onScheduled(ProcessContext context) {
        xpathExpression = context.getProperty(XML_PATH_EXPRESSION).getValue();
        destinationContent = DESTINATION_CONTENT.equals(context.getProperty(DESTINATION).getValue());
        attributeMark = context.getProperty(XML_ATTRIBUTE_MARK).getValue();
        contentKeyName = context.getProperty(XML_CONTENT_KEY_NAME).getValue();
    }

    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }
        try (final InputStream inputStream = session.read(flowFile)) {
            final byte[] jsonBytes = convertXmlBytesToJsonBytes(inputStream, xpathExpression, attributeMark, contentKeyName);
            final StopWatch stopWatch = new StopWatch(true);
            FlowFile successFlow;
            if (destinationContent) {
                FlowFile contFlowfile = session.create(flowFile);
                contFlowfile = session.write(contFlowfile, (in, out) -> {
                    try (OutputStream outputStream = new BufferedOutputStream(out)) {
                        outputStream.write(jsonBytes);
                    }
                });
                contFlowfile = session.putAttribute(contFlowfile, CoreAttributes.MIME_TYPE.key(), APPLICATION_JSON);
                session.remove(flowFile);
                successFlow = contFlowfile;
                session.getProvenanceReporter().modifyContent(successFlow, stopWatch.getElapsed(TimeUnit.MILLISECONDS));
            } else {
                successFlow = session.putAttribute(flowFile, JSON_ATTRIBUTE_NAME, new String(jsonBytes, StandardCharsets.UTF_8));
                session.getProvenanceReporter().modifyAttributes(successFlow, "Add Attribute: " + JSON_ATTRIBUTE_NAME);
            }
            session.transfer(successFlow, REL_SUCCESS);
        } catch (Exception e) {
            getLogger().error(e.getMessage());
            session.transfer(flowFile, REL_FAILURE);
        }
    }

    private static byte[] getNodeBytesFromXmlBytes(InputStream xmlInputSream, String xpathExpression)
            throws ParserConfigurationException, IOException, SAXException, XPathExpressionException, TransformerException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);

        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource inputSource = new InputSource(new InputStreamReader(xmlInputSream));
        Document doc = builder.parse(inputSource);

        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression compile = xpath.compile(xpathExpression);
        NodeList nodeList = (NodeList) compile.evaluate(doc, XPathConstants.NODESET);

        for (int i = 0; i < nodeList.getLength(); ++i) {
            Node nd = nodeList.item(i);
            nd.getParentNode().removeChild(nd);
        }

        // Create and setup transformer
        Transformer transformer =  TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        // Turn the nodeList into a string
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        DOMSource source = new DOMSource();
        for (int i = 0; i < nodeList.getLength(); ++i) {
            source.setNode(nodeList.item(i));
            transformer.transform(source, result);
        }
        return writer.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] convertXmlBytesToJsonBytes(InputStream xmlInputStream, String xpathExpression, String mark, String keyName)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException, TransformerException {
        try(InputStreamReader isr = new InputStreamReader(xmlInputStream, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
            mark = mark == null ? "" : mark;
            keyName = keyName == null ? "content" : keyName;
            if (xpathExpression == null) {
                XMLConverter xmlInstance = new XMLConverter();
                xmlInstance.setAttributeMark(mark);
                xmlInstance.setDataTagName(keyName);
                JSONObject xmlJSONObj = xmlInstance.toJSONObject_p(br);
                xmlJSONObj.write(osw);
                osw.flush();
                return baos.toByteArray();
            } else {
                byte[] node = getNodeBytesFromXmlBytes(xmlInputStream, xpathExpression);
                try(ByteArrayInputStream bais = new ByteArrayInputStream(node)){
                    return convertXmlBytesToJsonBytes(bais, null, mark, keyName);
                }
            }
        }
    }
}
