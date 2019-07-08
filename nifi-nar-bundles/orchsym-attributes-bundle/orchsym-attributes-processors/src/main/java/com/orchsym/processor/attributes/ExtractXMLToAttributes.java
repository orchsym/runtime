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
package com.orchsym.processor.attributes;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.DynamicProperty;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.BooleanAllowableValues;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.util.StandardValidators;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.orchsym.processor.attributes.xml.ExtractXPathResolver;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.s9api.XdmArray;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;

/**
 * @author GU Guoqiang
 *
 */
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@Marks(categories = { "Convert & Control/Convert" }, createdDate = "2018-12-14")
@Tags({ "Extract", "Attribute", "Record", "XML" })
@CapabilityDescription("Provide the abblity of extracting the attributes by XPath or XQuery for XML format contents from the incoming flowfile. if XPath, the root '/*' will be default.")
@DynamicProperty(name = "XML property", //
        value = "The name of dynamic property with XPath or XQuery expression", //
        expressionLanguageScope = ExpressionLanguageScope.FLOWFILE_ATTRIBUTES, //
        description = "set the dynamic property with XPath or XQuery expression")
@WritesAttributes({ //
        @WritesAttribute(attribute = AbstractExtractToAttributesProcessor.ATTR_REASON, description = "The error message of extracting failure")//
})
public class ExtractXMLToAttributes extends AbstractExtractToAttributesProcessor {

    static final String XPATH = "XPath";
    static final String XQUERY = "XQuery";
    static final String PATH_DEFAULT = XPATH;

    protected static final PropertyDescriptor XML_PATH_TYPE = new PropertyDescriptor.Builder()//
            .name("xml-path-type")//
            .displayName("XML Path Type")//
            .description("Indicates the way to extract the elements")//
            .required(false)//
            .allowableValues(XPATH, XQUERY)//
            .defaultValue(PATH_DEFAULT)//
            .build();

    protected static final PropertyDescriptor ALLOW_XML_ATTRIBUTES = new PropertyDescriptor.Builder()//
            .name("allow-xml-attributes")//
            .displayName("Allow XML attributes")//
            .description("Allow to extract the attributes for the XML attribute elements")//
            .required(false)//
            .allowableValues(BooleanAllowableValues.list())//
            .defaultValue(BooleanAllowableValues.FALSE.value())//
            .addValidator(BooleanAllowableValues.validator())//
            .build();

    protected static final PropertyDescriptor XML_ATTRIBUTE_MARK = new PropertyDescriptor.Builder()//
            .name("xml-attribute-mark")//
            .displayName("XML attribute mark")//
            .description("Set the prefix of XML attribute name when allow to extract XML attributes")//
            .required(false)//
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR) //
            .defaultValue("@")//
            .build();

    protected volatile String pathType = PATH_DEFAULT;
    protected volatile boolean allowXmlAttributes;
    protected volatile String attributeMark;

    @Override
    protected Validator getPathValidator() {
        return Validator.VALID;
    }

    @Override
    protected void init(final ProcessorInitializationContext context) {
        super.init(context);

        final List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(XML_PATH_TYPE);
        properties.add(ALLOW_XML_ATTRIBUTES);
        properties.add(XML_ATTRIBUTE_MARK);
        properties.addAll(this.properties);
        this.properties = Collections.unmodifiableList(properties);
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        super.onScheduled(context);
        pathType = context.getProperty(XML_PATH_TYPE).getValue();
        allowXmlAttributes = context.getProperty(ALLOW_XML_ATTRIBUTES).asBoolean();
        attributeMark = context.getProperty(XML_ATTRIBUTE_MARK).getValue();
    }

    protected String getDefaultAttributesPath() {
        if (XPATH.equals(pathType)) {
            return "/*"; // for XPath
        } else if (XQUERY.equals(pathType)) {
            return "for $elem in root() return $elem"; // root for XQuery
        }
        return null;
    }

    @Override
    protected void retrieveAttributes(ProcessContext context, ProcessSession session, FlowFile flowFile, InputStream rawIn, Map<String, String> extractedAttributes,
            final Map<String, String> attrPathSettings, final List<Pattern> includeFields, final List<Pattern> excludeFields) throws IOException {
        Node rootNode = createDomNode(rawIn);

        if (XPATH.equals(pathType)) {
            retrieveAttributesByXpath(context, session, flowFile, rootNode, extractedAttributes, attrPathSettings, includeFields, excludeFields);
        } else {
            retrieveAttributesByXquery(context, session, flowFile, rootNode, extractedAttributes, attrPathSettings, includeFields, excludeFields);
        }
    }

    private Node createDomNode(InputStream rawIn) throws IOException {
        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
        dfactory.setNamespaceAware(false);
        Node rootNode;
        try {
            rootNode = dfactory.newDocumentBuilder().parse(new InputSource(rawIn));
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException(e);
        }

        return rootNode;
    }

    // XPath
    private void retrieveAttributesByXpath(ProcessContext context, ProcessSession session, FlowFile flowFile, Node rootNode, final Map<String, String> extractedAttributes,
            final Map<String, String> attrPathSettings, final List<Pattern> includeFields, final List<Pattern> excludeFields) throws IOException {

        final Map<String, XPathExpression> xPathExpMap = new LinkedHashMap<>();
        final Map<String, net.sf.saxon.xpath.XPathExpressionImpl> saxonXPathExpMap = new LinkedHashMap<>();
        try {
            // final XPathFactoryImpl xPathFactory = XPathFactory.newInstance(NamespaceConstant.OBJECT_MODEL_SAXON);
            final net.sf.saxon.xpath.XPathFactoryImpl saxonXPathFactory = new net.sf.saxon.xpath.XPathFactoryImpl();
            final XPath saxonXPath = saxonXPathFactory.newXPath();

            final XPathFactory xPathFactory = XPathFactory.newInstance(XPathConstants.DOM_OBJECT_MODEL);

            XPath xPath = xPathFactory.newXPath();
            xPath.setXPathVariableResolver(new ExtractXPathResolver(context, flowFile));

            for (Entry<String, String> entry : attrPathSettings.entrySet()) {
                xPathExpMap.put(entry.getKey(), xPath.compile(entry.getValue()));
                saxonXPathExpMap.put(entry.getKey(), (net.sf.saxon.xpath.XPathExpressionImpl) saxonXPath.compile(entry.getValue()));
            }
        } catch (XPathExpressionException | XPathFactoryConfigurationException e) {
            getLogger().error(e.getMessage(), e);
            throw new IOException(e);
        }

        try {

            for (Entry<String, XPathExpression> entry : xPathExpMap.entrySet()) {
                final String pathPropName = entry.getKey();
                final XPathExpression xPathExpression = entry.getValue();

                final Expression expression = saxonXPathExpMap.get(pathPropName).getInternalExpression();
                final NodeList resultList = (NodeList) xPathExpression.evaluate(rootNode, XPathConstants.NODESET);

                checkAndSetResults(extractedAttributes, resultList, expression, pathPropName, includeFields, excludeFields);
            }
        } catch (Exception e) {
            final String message = "Extract the XML with XPath failure, because " + e.getMessage();
            getLogger().error(message, e);
            throw new IOException(message, e);
        }
    }

    private List<Node> filterChildrenNodes(final NodeList childNodes, final List<Pattern> includeFields, final List<Pattern> excludeFields) {
        return filterChildrenElements(childNodes).stream().filter(n -> !(isScalar(n) && ignoreField(n.getNodeName(), includeFields, excludeFields))).collect(Collectors.toList());
    }

    private List<Node> filterChildrenElements(final NodeList childNodes) {
        if (childNodes == null) {
            return Collections.emptyList();
        }
        List<Node> nodesList = new ArrayList<>();
        for (int i = 0; i < childNodes.getLength(); i++) {
            final Node child = childNodes.item(i);
            if (Node.ELEMENT_NODE == child.getNodeType()) {
                nodesList.add(child);
            }
            if (Node.ATTRIBUTE_NODE == child.getNodeType()) {
                nodesList.add(child);
            }
        }

        return nodesList;
    }

    private Map<String, List<Node>> convertFilteredChildrenNodes(final NodeList childNodes, final List<Pattern> includeFields, final List<Pattern> excludeFields) {
        final Map<String, List<Node>> results = new LinkedHashMap<>();

        filterChildrenNodes(childNodes, includeFields, excludeFields).forEach(child -> {
            if (isScalar(child) || recurseChildren) {
                final String childName = child.getNodeName();
                List<Node> list = results.get(childName);
                if (list == null) {
                    list = new ArrayList<>();
                    results.put(childName, list);
                }
                list.add(child);
            }
        });
        return results;
    }

    private String getXPathLastName(final net.sf.saxon.expr.Expression expression) {
        if (expression instanceof net.sf.saxon.expr.SlashExpression) {
            final net.sf.saxon.expr.SlashExpression slashExpression = (net.sf.saxon.expr.SlashExpression) expression;
            final net.sf.saxon.expr.Expression lastStep = slashExpression.getLastStep();
            if (lastStep instanceof net.sf.saxon.expr.AxisExpression) {
                final net.sf.saxon.expr.AxisExpression axisExpression = (net.sf.saxon.expr.AxisExpression) lastStep;
                final net.sf.saxon.pattern.NodeTest nodeTest = axisExpression.getNodeTest();
                if (nodeTest instanceof net.sf.saxon.pattern.NameTest) { // like /Data
                    return nodeTest.toShortString();
                } else if (nodeTest instanceof net.sf.saxon.pattern.NodeKindTest) { // like /Data/*
                    // only for name directly
                    // final Expression lhsExpression = slashExpression.getLhsExpression();
                    // return getFiledName(lhsExpression);
                }

            }

        }
        return null;
    }

    // XQuery
    private void retrieveAttributesByXquery(ProcessContext context, ProcessSession session, FlowFile flowFile, Node rootNode, final Map<String, String> extractedAttributes,
            final Map<String, String> attrPathSettings, final List<Pattern> includeFields, final List<Pattern> excludeFields) throws IOException {

        final Map<String, XQueryExpression> xqueryExpMap = new LinkedHashMap<>();

        final Configuration config = new Configuration();
        try {
            final StaticQueryContext queryContext = config.newStaticQueryContext();
            for (Entry<String, String> entry : attrPathSettings.entrySet()) {
                final XQueryExpression queryExp = queryContext.compileQuery(entry.getValue());
                xqueryExpMap.put(entry.getKey(), queryExp);
            }
        } catch (Exception e) {
            getLogger().error(e.getMessage(), e);
            throw new IOException(e);
        }

        try {
            final DocumentInfo docInfo = new DocumentInfo(config.buildDocumentTree(new DOMSource(rootNode)).getRootNode());
            final DynamicQueryContext dynamicContext = new DynamicQueryContext(config);
            dynamicContext.setContextItem(docInfo);

            final Properties outputProperties = new Properties();
            // outputProperties.setProperty(OutputKeys.METHOD, "xml");
            // outputProperties.setProperty(OutputKeys.INDENT, "yes");

            for (Entry<String, XQueryExpression> entry : xqueryExpMap.entrySet()) {
                final String queryPropName = entry.getKey();
                final XQueryExpression queryExp = entry.getValue();
                DOMResult result = new DOMResult();
                queryExp.run(dynamicContext, result, outputProperties);

                final Expression expression = queryExp.getExpression();
                final Node doc = result.getNode();
                final NodeList resultList = doc.getChildNodes();

                checkAndSetResults(extractedAttributes, resultList, expression, queryPropName, includeFields, excludeFields);
            }

        } catch (Exception e) {
            final String message = "Extract the XML with XQuery failure, because " + e.getMessage();
            getLogger().error(message, e);
            throw new IOException(message, e);
        }
    }

    private void checkAndSetResults(final Map<String, String> extractedAttributes, final NodeList resultList, final Expression expression, final String queryPropName,
            final List<Pattern> includeFields, final List<Pattern> excludeFields) {
        final String queryLastName = getXPathLastName(expression);
        // some xpath like /xxx/yyy, not /xxx/*, and need check /xxx/text(), /Data[@xmlns], /Data/@xmlns etc
        final boolean isDirectNode = StringUtils.isNotBlank(queryLastName);

        final List<Node> filteredList = filterChildrenElements(resultList);

        if (filteredList.size() == 0) { // no result
            setAttributes(extractedAttributes, queryPropName, "", -1, includeFields, excludeFields);
            // continue;
            return;
        }

        if (isDirectNode) {
            if (filteredList.size() == 1 && !isScalar(filteredList.get(0))) {
                final Node node = filteredList.get(0);

                String nodeAttrPrefix = containPropName ? queryPropName : "";
                setAttributesForNode(extractedAttributes, nodeAttrPrefix, node, includeFields, excludeFields, allowXmlAttributes);
                // continue;
                return;
            } else if (allowArray && filteredList.size() > 1 && isScalarList(filteredList)) {
                for (int i = 0; i < filteredList.size(); i++) {
                    final Node node = filteredList.get(i);
                    final String nodeAttrPrefix = getAttributeName(queryPropName, null, i);

                    setAttributesForNode(extractedAttributes, nodeAttrPrefix, node, includeFields, excludeFields, allowXmlAttributes);
                }
                // continue;
                return;
            }
        }

        final Map<String, List<Object>> results = new LinkedHashMap<>();
        final Map<String, List<Object>> allScalarResults = new LinkedHashMap<>();
        boolean filtered = false;

        for (Node node : filteredList) {
            final String nodeName = node.getNodeName();
            if (isScalar(node) && ignoreField(nodeName, includeFields, excludeFields)) {
                filtered = true;
                continue;
            }

            if (isScalar(node)) {
                List<Object> list = allScalarResults.get(nodeName);
                if (list == null) {
                    list = new ArrayList<>();
                    allScalarResults.put(nodeName, list);
                }
                list.add(node);
            } else {
                List<Object> list = results.get(nodeName);
                if (list == null) {
                    list = new ArrayList<>();
                    results.put(nodeName, list);
                }
                list.add(node);
            }

        }
        final Map<String, List<Object>> scalarResults = new LinkedHashMap<>();
        final Map<String, List<Object>> scalarArrResults = new LinkedHashMap<>();
        for (Entry<String, List<Object>> se : allScalarResults.entrySet()) {
            final List<Object> list = se.getValue();
            if (list.size() > 1) {
                scalarArrResults.put(se.getKey(), list);
            } else {
                scalarResults.put(se.getKey(), list);
            }
        }
        setResults(extractedAttributes, queryPropName, -1, includeFields, excludeFields, results, scalarResults, scalarArrResults, filtered);

    }

    protected void setResults(Map<String, String> extractedAttributes, final String attrName, int index, final List<Pattern> includeFields, final List<Pattern> excludeFields,
            final Map<String, List<Object>> results, final Map<String, List<Object>> scalarResults, final Map<String, List<Object>> scalarArrResults, final boolean filtered) {

        if (results.isEmpty() && scalarResults.isEmpty() && scalarArrResults.isEmpty() && !filtered) {
            String prefix = getAttributeName(attrName, null, index); // force setting the name
            // if not found, set empty
            setAttributes(extractedAttributes, prefix, "", -1, includeFields, excludeFields);
            return;
        }

        String prefix = getAttributeName(attrName, null, index);

        //
        if (scalarResults.size() == 1) {
            // only one field, but maybe array
            final List<Object> nodes = scalarResults.get(scalarResults.keySet().iterator().next());
            if (nodes.size() == 1) {
                setAttributesForNode(extractedAttributes, prefix, (Node) nodes.get(0), includeFields, excludeFields, allowXmlAttributes);
            } else if (nodes.size() > 1) {
                for (int i = 0; i < nodes.size(); i++) {
                    setAttributes(extractedAttributes, prefix, (Node) nodes.get(0), i, includeFields, excludeFields);
                }
            }
        } else if (scalarResults.size() > 1) {
            setAttributes(extractedAttributes, prefix, scalarResults, -1, includeFields, excludeFields);
        }

        if (!containPropName) { // need ignore the field name
            prefix = getAttributeName(null, null, index);
        }

        //
        if (!scalarArrResults.isEmpty()) {
            final Map<String, List<Node>> convertNodesMap = new LinkedHashMap<>();

            for (Entry<String, List<Object>> e : scalarArrResults.entrySet()) {
                convertNodesMap.put(e.getKey(), e.getValue().stream().map(n -> (Node) n).collect(Collectors.toList()));
            }

            setAttributesForArray(extractedAttributes, prefix, includeFields, excludeFields, convertNodesMap);
        }

        //
        if (!results.isEmpty()) {

            for (Entry<String, List<Object>> e : results.entrySet()) {
                final List<Object> list = e.getValue();
                if (list.size() == 1) {
                    setAttributesForNode(extractedAttributes, prefix, (Node) list.get(0), includeFields, excludeFields, allowXmlAttributes);
                } else if (list.size() > 1) { // array
                    final Map<String, List<Node>> filteredNodesMap = new LinkedHashMap<>();

                    for (int i = 0; i < list.size(); i++) {
                        Node node = (Node) list.get(i);

                        if (allowXmlAttributes) {
                            setNodeAttributes(extractedAttributes, prefix, node, i, includeFields, excludeFields);
                        }

                        filterChildrenNodes(node.getChildNodes(), includeFields, excludeFields).forEach(c -> {
                            String childName = c.getNodeName();
                            List<Node> childList = filteredNodesMap.get(childName);
                            if (null == childList) {
                                childList = new ArrayList<>();
                                filteredNodesMap.put(childName, childList);
                            }
                            childList.add(c);
                        });

                    }

                    setAttributesForArray(extractedAttributes, prefix, includeFields, excludeFields, filteredNodesMap);

                }
            }
        }

    }

    protected void setAttributes(Map<String, String> extractedAttributes, final String attrPrefix, Object data, int index, final List<Pattern> includeFields, final List<Pattern> excludeFields) {
        if (data instanceof Node) {
            Node node = (Node) data;

            final String nodeName = node.getNodeName();
            boolean withAttr = allowXmlAttributes;
            if (index > -1 && allowXmlAttributes) {// set index
                final String nodeAttrPrefix = getAttributeName(attrPrefix, nodeName, -1);
                setNodeAttributes(extractedAttributes, nodeAttrPrefix, node, index, includeFields, excludeFields);
                withAttr = false; // have done, so need ignore
            }
            final String nodeAttrPrefix = getAttributeName(attrPrefix, nodeName, index);

            setAttributesForNode(extractedAttributes, nodeAttrPrefix, node, includeFields, excludeFields, withAttr);

        } else {
            super.setAttributes(extractedAttributes, attrPrefix, data, index, includeFields, excludeFields);
        }
    }

    protected void setAttributesForNode(Map<String, String> attributes, final String nodeAttrPrefix, Node node, final List<Pattern> includeFields, final List<Pattern> excludeFields,
            boolean withAttr) {
        //
        if (withAttr) {
            setNodeAttributes(attributes, nodeAttrPrefix, node, -1, includeFields, excludeFields);
        }

        //
        if (isScalar(node)) {
            setAttributes(attributes, nodeAttrPrefix, node.getTextContent(), -1, includeFields, excludeFields);
        } else {
            //
            final Map<String, List<Node>> filteredNodesMap = convertFilteredChildrenNodes(node.getChildNodes(), includeFields, excludeFields);

            setAttributesForArray(attributes, nodeAttrPrefix, includeFields, excludeFields, filteredNodesMap);

        }
    }

    protected void setAttributesForArray(Map<String, String> extractedAttributes, final String nodeAttrPrefix, final List<Pattern> includeFields, final List<Pattern> excludeFields,
            final Map<String, List<Node>> filteredNodesMap) {
        for (Entry<String, List<Node>> entry : filteredNodesMap.entrySet()) {
            final List<Node> list = entry.getValue();
            final int size = list.size();
            if (size == 1) { // not array
                setAttributes(extractedAttributes, nodeAttrPrefix, list.get(0), -1, includeFields, excludeFields);
            } else if (size > 1 && allowArray) { // array, like "links"
                if (isScalarList(list)) {
                    setAttributes(extractedAttributes, nodeAttrPrefix, list, -1, includeFields, excludeFields);
                } else {
                    for (int i = 0; i < size; i++) {
                        final Node arrOne = list.get(i);
                        final String arrAttrPrefix = getAttributeName(nodeAttrPrefix, arrOne.getNodeName(), -1);
                        final int arrIndex = i;
                        if (allowXmlAttributes) {
                            setNodeAttributes(extractedAttributes, arrAttrPrefix, arrOne, arrIndex, includeFields, excludeFields);
                        }

                        filterChildrenNodes(arrOne.getChildNodes(), includeFields, excludeFields).forEach(n -> {
                            setAttributes(extractedAttributes, arrAttrPrefix, n, arrIndex, includeFields, excludeFields);
                        });

                    }
                }
            }
        }
    }

    protected void setNodeAttributes(Map<String, String> extractedAttributes, String nodePrefix, Node node, int index, final List<Pattern> includeFields, final List<Pattern> excludeFields) {
        final NamedNodeMap nodeAttrs = node.getAttributes();
        if (nodeAttrs == null || node.getNodeType() != Node.ELEMENT_NODE) {
            return;
        }
        for (int i = 0; i < nodeAttrs.getLength(); i++) {
            final Node nodeAttr = nodeAttrs.item(i);
            final String nodeAttrName = nodeAttr.getNodeName();
            if (ignoreField(nodeAttrName, includeFields, excludeFields)) {
                continue;
            }

            String nodeAttrPrefix = getAttributeName(nodePrefix, StringUtils.isNotBlank(attributeMark) ? attributeMark + nodeAttrName : nodeAttrName, index);
            setAttributes(extractedAttributes, nodeAttrPrefix, nodeAttr.getTextContent(), -1, includeFields, excludeFields);
        }
    }

    @Override
    protected boolean isScalarList(Object value) {
        if (value instanceof NodeList) {
            NodeList list = (NodeList) value;
            boolean allScalar = true;

            for (int i = 0; i < list.getLength(); i++) {
                if (!isScalar(list.item(i))) {
                    allScalar = false;
                    break;
                }
            }
            return allScalar;
        } else if (value instanceof XdmArray) {
            XdmArray array = ((XdmArray) value);
            boolean allScalar = true;

            for (int i = 0; i < array.arrayLength(); i++) {
                if (!isScalar(array.get(i))) {
                    allScalar = false;
                    break;
                }
            }
            return allScalar;
        }
        return super.isScalarList(value);
    }

    @Override
    protected boolean isScalar(Object value) {
        if (value instanceof net.sf.saxon.om.NodeInfo) {
            net.sf.saxon.om.NodeInfo node = (net.sf.saxon.om.NodeInfo) value;
            if (node.getNodeKind() == net.sf.saxon.type.Type.ATTRIBUTE || node.getNodeKind() == net.sf.saxon.type.Type.TEXTUAL_ELEMENT//
                    || node.getNodeKind() == net.sf.saxon.type.Type.COMMENT || node.getNodeKind() == net.sf.saxon.type.Type.NAMESPACE //
                    || node.getNodeKind() == net.sf.saxon.type.Type.TEXT || node.getNodeKind() == net.sf.saxon.type.Type.WHITESPACE_TEXT //
            ) {
                return true;
            } else if (node.getNodeKind() == net.sf.saxon.type.Type.ELEMENT) {
                // final AxisIterator childrenIterator = node.iterateAxis(AxisInfo.CHILD);
                // NodeInfo child;
                // while ((child = (NodeInfo) childrenIterator.next()) != null) {
                // if (node.getNodeKind() == Type.ELEMENT) {
                // return false;
                // }
                // }
                return node.iterateAxis(net.sf.saxon.om.AxisInfo.CHILD, net.sf.saxon.pattern.NodeKindTest.ELEMENT).toList().size() == 0;
            }
            return false;
        } else if (value instanceof Node) {
            Node node = (Node) value;
            final short nodeType = node.getNodeType();
            if (Node.ATTRIBUTE_NODE == nodeType || Node.TEXT_NODE == nodeType //
                    || Node.COMMENT_NODE == nodeType || Node.CDATA_SECTION_NODE == nodeType) {
                return true;
            } else if (Node.ELEMENT_NODE == nodeType) {
                final NodeList childNodes = node.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    if (Node.ELEMENT_NODE == childNodes.item(i).getNodeType()) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        } else if (value instanceof XdmValue) {
            if (value instanceof XdmAtomicValue) {
                return true;
            } else if (value instanceof XdmNode) {
                final net.sf.saxon.om.NodeInfo underlyingNode = ((XdmNode) value).getUnderlyingNode();
                return isScalar(underlyingNode);
            }
        }

        return super.isScalar(value);
    }

}
