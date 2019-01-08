package com.orchsym.processor.attributes;

import java.io.StringReader;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.flowfile.attributes.BooleanAllowableValues;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.util.StringUtils;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.util.StandardValidators;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.sax.SAXSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import javax.xml.xquery.XQConnection;
import javax.xml.xquery.XQConstants;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQPreparedExpression;
import javax.xml.xquery.XQResultSequence;
import javax.xml.xquery.XQStaticContext;

import org.xml.sax.InputSource;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.type.Type;
import net.sf.saxon.xpath.XPathFactoryImpl;
import net.sf.saxon.xqj.SaxonXQDataSource;

/**
 * @author GU Guoqiang
 *
 */
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@Marks(categories = { "Convert & Control/Convert" }, createdDate = "2018-12-14")
@Tags({ "Extract", "Attribute", "Record", "XML" })
@CapabilityDescription("Provide the abblity of extracting the attributes by XPath or XQuery for XML format contents from the incoming flowfile")
public class ExtractXMLToAttributes extends AbstractExtractToAttributesProcessor {

    private boolean isXpathType = true;

    protected static final PropertyDescriptor XML_PATH_TYPE = new PropertyDescriptor.Builder()//
            .name("xml-path-type")//
            .displayName("XML Path Type")//
            .description("Indicates the way to extract the elements")//
            .required(false)//
            .allowableValues("XPath", "XQuery")//
            .defaultValue("XPath")//
            .build();

    protected static final PropertyDescriptor ALLOW_XML_ATTRIBUTES = new PropertyDescriptor.Builder()//
            .name("allow-xml-attributes")//
            .displayName("Allow XML attributes")//
            .description("Allow to extract the attributes for the XML attribute elements")//
            .required(false)//
            .allowableValues(BooleanAllowableValues.list())//
            .defaultValue(BooleanAllowableValues.TRUE.value())//
            .addValidator(BooleanAllowableValues.validator())//
            .build();

    protected static final PropertyDescriptor XML_ATTRIBUTE_MARK = new PropertyDescriptor.Builder()//
            .name("xml-attribute-mark")//
            .displayName("XML attribute mark")//
            .description("Set the prefix of XML attribute name when allow to extract XML attributes")//
            .required(false)//
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .defaultValue("@")//
            .build();


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

    @Override
    protected void retrieveAttributes(ProcessContext context, ProcessSession session, FlowFile flowFile, final Map<String, String> attributesFromRecords, final List<Pattern> includeFields,
            final List<Pattern> excludeFields) {
        String pathType = context.getProperty(XML_PATH_TYPE).getValue();
        if (pathType.equals("XPath")) {
            retrieveAttributesByXpath(context, session, flowFile, attributesFromRecords, includeFields, excludeFields);
        } else {
            retrieveAttributesByXquery(context, session, flowFile, attributesFromRecords, includeFields, excludeFields);
        }    
    }

    //xpath
    private void retrieveAttributesByXpath(ProcessContext context, ProcessSession session, FlowFile flowFile, final Map<String, String> attributesFromRecords, final List<Pattern> includeFields,
            final List<Pattern> excludeFields) {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        session.exportTo(flowFile, bytes);
        final String xmlContent = bytes.toString();

        boolean allowXmlAttributes = context.getProperty(ALLOW_XML_ATTRIBUTES).asBoolean();
        String attributeMark = context.getProperty(XML_ATTRIBUTE_MARK).getValue();
        for (Entry<String, String> entry : attrPaths.entrySet()) {
            final String pathAttrKey = entry.getKey();
            final String pathString = entry.getValue();
            try {    
                processAttributesByXpath(attributesFromRecords, xmlContent, pathAttrKey, pathString, includeFields, excludeFields, allowXmlAttributes, attributeMark);
            } catch (Exception e) {
                final String attrName = getAttributeName(pathAttrKey, null, -1);
                attributesFromRecords.put(attrName, "");
            }
        } 
    }

    protected void processAttributesByXpath(Map<String, String> attributes, String xmlConentStr,String jsonPathAttrKey, String xpathString, final List<Pattern> includeFields, final List<Pattern> excludeFields, boolean allowXmlAttributes, String attributeMark) throws Exception {
        if (ignoreField(jsonPathAttrKey, includeFields, excludeFields)) {
            return;
        }
        XPathFactory xPathFactory = new net.sf.saxon.xpath.XPathFactoryImpl();
        XPath xPath = xPathFactory.newXPath();
        InputSource inputSource = new InputSource(new StringReader(xmlConentStr));
        SAXSource saxSource = new SAXSource(inputSource);
        Configuration config = ((XPathFactoryImpl) xPathFactory).getConfiguration();
        DocumentInfo document = config.buildDocument(saxSource); 
        XPathExpression xPathExpression = xPath.compile(xpathString);  
        List resultList = (List)xPathExpression.evaluate(document, XPathConstants.NODESET); 

        if (resultList.size() == 1 || (resultList.size() > 0 && !allowArray)) {
            NodeInfo node = (NodeInfo)resultList.get(0);
            setAttributes(attributes, jsonPathAttrKey, node.getStringValue(), -1, includeFields, excludeFields);
            if (allowXmlAttributes && node.getNodeKind() != Type.ATTRIBUTE) {
                processNodeAttributesByXpath(attributes, node, attributeMark);
            }
        } else if (resultList.size() > 1 && allowArray) {
            for (int i = 0; i < resultList.size(); i++) {
                NodeInfo node = (NodeInfo) resultList.get(i);
                setAttributes(attributes, jsonPathAttrKey, node.getStringValue(), i, includeFields, excludeFields);
                if (allowXmlAttributes && node.getNodeKind() != Type.ATTRIBUTE) {
                    processNodeAttributesByXpath(attributes, node, attributeMark);
                }
            }
        }
    }

    private void processNodeAttributesByXpath(Map<String, String> attributes, NodeInfo node, String attributeMark) {
        AxisIterator it = node.iterateAxis(Axis.ATTRIBUTE);
        NodeInfo attr;
        while ((attr = (NodeInfo) it.next()) != null) {
          String attr_name = attr.getDisplayName();
          String attr_value = attr.getStringValue();
          String attrFixName = StringUtils.isEmpty(attributeMark) ? attr_name : attributeMark + attr_name;
          attributes.put(attrFixName, attr_value);
        }
    }

    //xquery
    private void retrieveAttributesByXquery(ProcessContext context, ProcessSession session, FlowFile flowFile, final Map<String, String> attributesFromRecords, final List<Pattern> includeFields, final List<Pattern> excludeFields) {

        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        session.exportTo(flowFile, bytes);
        final String xmlContent = bytes.toString();

        try {
            SaxonXQDataSource ds = new SaxonXQDataSource();
            XQConnection conn = ds.getConnection();
            XQStaticContext sc = conn.getStaticContext();

            for (Entry<String, String> entry : attrPaths.entrySet()) {
                final String pathAttrKey = entry.getKey();
                final String pathString = entry.getValue();
                try {
                    XQResultSequence result = retriveResultSequence(sc, conn, pathString, xmlContent);
                    processAttributesByXquery(attributesFromRecords, result, pathAttrKey, includeFields, excludeFields);
                } catch (Exception e) {
                    final String attrName = getAttributeName(pathAttrKey, null, -1);
                    attributesFromRecords.put(attrName, "");
                }
            } 
        } catch (Exception e) {
            getLogger().error("retrieveAttributesByXquery error {}.", e);
        }
    }

    protected XQResultSequence retriveResultSequence(XQStaticContext sc, XQConnection conn, String xqueryString, String xmlContent) throws XQException, XMLStreamException {
        XQPreparedExpression expression = conn.prepareExpression(xqueryString, sc);
        expression.bindDocument(XQConstants.CONTEXT_ITEM, readXMLFromString(xmlContent), conn.createDocumentType());
        XQResultSequence result = expression.executeQuery();
        return result;
    }

    private XMLStreamReader readXMLFromString(final String xmlContent) throws XMLStreamException
    {
        final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        final StringReader reader = new StringReader(xmlContent);
        return inputFactory.createXMLStreamReader(reader);
    }

    protected void processAttributesByXquery(Map<String, String> attributes, XQResultSequence resultSequence, String jsonPathAttrKey, final List<Pattern> includeFields,
            final List<Pattern> excludeFields) throws XQException {
        if (ignoreField(jsonPathAttrKey, includeFields, excludeFields)) {
            return;
        }
        List<String> resultArray = new ArrayList();
        while (resultSequence.next()) {
            resultArray.add(resultSequence.getItemAsString(null));
        }
        if (resultArray.size() == 1 || (resultArray.size() > 0 && !allowArray)){
            setAttributes(attributes, jsonPathAttrKey, resultArray.get(0), -1, includeFields, excludeFields);
        } else if (resultArray.size() > 1 && allowArray) {
            for (int i = 0; i < resultArray.size(); i++) {
                setAttributes(attributes, jsonPathAttrKey, resultArray.get(i), i, includeFields, excludeFields);
            }
        }
    }
}
