package com.orchsym.processor.jsonxml;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.BooleanAllowableValues;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.util.StopWatch;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.flowfile.attributes.CoreAttributes;

import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;
import java.util.Collections;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.json.JSONObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
        String content = null;
        try (final InputStream inputStream = session.read(flowFile)) {
            content = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            getLogger().error("Failed to read the flowfile {}", new Object[] { flowFile });
            session.transfer(flowFile, REL_FAILURE);
            return;
        }

        Map<String, Object> jsonMap = convertXMLStrToMap(content, xpathExpression, attributeMark, contentKeyName);
        Gson gson = new GsonBuilder().create();
        final String jsonContent = gson.toJson(jsonMap);
        try {
            final StopWatch stopWatch = new StopWatch(true);
            FlowFile successFlow = null;
            if (destinationContent) {
                FlowFile contFlowfile = session.create(flowFile);
                contFlowfile = session.write(contFlowfile, (in, out) -> {
                    try (OutputStream outputStream = new BufferedOutputStream(out)) {
                        outputStream.write(jsonContent.getBytes("UTF-8"));
                    }
                });
                contFlowfile = session.putAttribute(contFlowfile, CoreAttributes.MIME_TYPE.key(), APPLICATION_JSON);
                session.remove(flowFile);
                successFlow = contFlowfile;
            } else {
                successFlow = session.putAttribute(flowFile, JSON_ATTRIBUTE_NAME, jsonContent);
            }

            session.getProvenanceReporter().modifyContent(successFlow, stopWatch.getElapsed(TimeUnit.MILLISECONDS));
            session.transfer(successFlow, REL_SUCCESS);

        } catch (Exception e) {
            getLogger().error(e.getMessage());
            session.transfer(flowFile, REL_FAILURE);
        }
    }

    protected Map<String, Object> convertXMLStrToMap(String xmlStr, String xpathExpression, String mark, String keyName) {
        Map<String, Object> ret = new HashMap<String, Object>();
        try {
            if (xmlStr == null) {
                return ret;
            }
            mark = mark == null ? "" : mark;
            keyName = keyName == null ? "content" : keyName;
            if (xpathExpression == null) {
                XMLConverter xmlInstance = new XMLConverter();
                xmlInstance.setAttributeMark(mark);
                xmlInstance.setDataTagName(keyName);
                JSONObject xmlJSONObj = xmlInstance.toJSONObject_p(xmlStr);
                ret = xmlJSONObj.toMap();
            } else {
                NodeList nodeList = getNodeListFromXmlStr(xmlStr, xpathExpression);
                ret = getJsonStringFromNodeList(nodeList, mark, keyName);
            }
        } catch (Exception e) {
            return ret;
        }

        return ret;
    }

    private NodeList getNodeListFromXmlStr(String xmlStr, String xpathExpression) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);

        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource inputSource = new InputSource(new StringReader(xmlStr));
        Document doc = builder.parse(inputSource);

        XPathFactory xpf = XPathFactory.newInstance();
        XPath xpath = xpf.newXPath();
        XPathExpression compile = xpath.compile(xpathExpression);
        NodeList nodeList = (NodeList) compile.evaluate(doc, XPathConstants.NODESET);
        return nodeList;
    }

    private Map<String, Object> getJsonStringFromNodeList(NodeList nodeList, String mark, String keyName) {
        Map<String, Object> nodeMap = new HashMap<String, Object>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            String nodeName = node.getNodeName();
            Map<String, Object> map = new HashMap<String, Object>();
            processNodeChildren(node, map, mark, keyName);

            Map<String, Object> attrMap = getNodeAttributes(node, mark);
            if (attrMap != null) {
                ((Map<String, Object>) (map.get(nodeName))).putAll(attrMap);
            }
            mergeContentToMap(nodeMap, nodeName, map.get(nodeName));
        }
        return nodeMap;
    }

    private void processNodeChildren(Node node, Map<String, Object> map, String mark, String keyName) {
        Map<String, Object> childMap = new HashMap<String, Object>();
        String nodeName = node.getNodeName();
        if (node.getChildNodes().getLength() == 1) {
            processNode(node, map, mark, keyName);
        } else {
            for (int i = 0; i < node.getChildNodes().getLength(); i++) {
                Node subNode = node.getChildNodes().item(i);
                if (subNode.getChildNodes().getLength() > 1) {
                    // 递归获取所有子node
                    processNodeChildren(subNode, childMap, mark, keyName);
                } else {
                    processNode(subNode, childMap, mark, keyName);
                }
            }
            map.put(nodeName, childMap);
        }
    }

    private void processNode(Node node, Map<String, Object> map, String mark, String keyName) {
        String nodeKey = node.getNodeName();
        Object nodeContent = node.getTextContent();
        NamedNodeMap attributes = node.getAttributes();

        Map<String, Object> attributeMap = getNodeAttributes(node, mark);
        if (attributeMap != null) {
            attributeMap.put(keyName, nodeContent);
            nodeContent = attributeMap;
        }
        mergeContentToMap(map, nodeKey, nodeContent);
    }

    private Map<String, Object> getNodeAttributes(Node node, String mark) {
        NamedNodeMap attributes = node.getAttributes();
        if (attributes == null || attributes.getLength() == 0) {
            return null;
        }
        Map<String, Object> map = new HashMap<String, Object>();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attr = (Attr) attributes.item(i);
            String attrName = attr.getNodeName();
            attrName = mark + attrName;
            String attrValue = attr.getNodeValue();
            map.put(attrName, attrValue);
        }
        return map;
    }

    private void mergeContentToMap(Map<String, Object> map, String key, Object content) {
        if (map.containsKey(key)) {
            if (map.get(key) instanceof List<?>) {
                ((List) map.get(key)).add(content);
            } else {
                List<Object> conentArr = new ArrayList<Object>();
                conentArr.add((Object) map.get(key));
                conentArr.add(content);
                map.put(key, conentArr);
            }
        } else {
            map.put(key, content);
        }

    }
}
