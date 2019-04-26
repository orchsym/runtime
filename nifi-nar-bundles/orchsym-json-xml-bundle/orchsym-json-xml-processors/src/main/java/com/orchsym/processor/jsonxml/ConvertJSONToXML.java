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
import org.apache.commons.lang3.StringUtils;

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

import org.json.JSONObject;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;

/**
 * @author Lu JB
 *
 */
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@Marks(categories = { "Convert & Control/Convert" }, createdDate = "2019-1-17")
@Tags({ "Convert", "JSON", "XML", "JSON TO XML" })
@CapabilityDescription("Provide the ability of Convert JSON format contents to XML format contents from the incoming flowfile")

public class ConvertJSONToXML extends AbstractProcessor {

    public static final String XML_ATTRIBUTE_NAME = "XMLAttributes";

    public static final String DESTINATION_ATTRIBUTE = "flowfile-attribute";
    public static final String DESTINATION_CONTENT = "flowfile-content";
    public static final String APPLICATION_XML = "application/xml";

    protected static final PropertyDescriptor JSON_ATTRIBUTE_MARK = new PropertyDescriptor.Builder()//
            .name("json-attribute-mark")//
            .displayName("Json Attribute Mark")// @
            .description("Set the mark value, if a json key string is start with the mark character, then the json key and value will be extracted as xml attribute")//
            .required(false)//
            .addValidator(Validator.VALID).defaultValue("@")//
            .build();

    protected static final PropertyDescriptor JSON_CONTENT_KEY_NAME = new PropertyDescriptor.Builder()//
            .name("json-content-key-name")//
            .displayName("Json Content Key Name")//
            .description("Set the key name, if a json key is equal to the key name, then the json value will be extracted as xml node value")//
            .required(false)//
            .addValidator(Validator.VALID).defaultValue("content")//
            .build();

    protected static final PropertyDescriptor JSON_PATH_EXPRESSION = new PropertyDescriptor.Builder()//
            .name("json-path-expression")//
            .displayName("Json Path Expression")//
            .description("The json path expression to extract the contents").required(false).addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

    protected static final PropertyDescriptor ELEMENT_NAME = new PropertyDescriptor.Builder()//
            .name("element-name")//
            .displayName("Element Name")//
            .description(
                    "Specfic the element name. When user extracts content by Json Path expression, the content will be converted to xml format, and put into element with the element name. If the extracted content is JSONArray, then all element will put into a root element with name 'list'")
            .required(false).addValidator(Validator.VALID).build();

    protected static final PropertyDescriptor ENCODING = new PropertyDescriptor.Builder()//
            .name("xml-encoding")//
            .displayName("Encoding")//
            .description("Specfic the encoding name").required(false).addValidator(StandardValidators.NON_EMPTY_VALIDATOR).defaultValue("UTF-8").build();

    protected static final PropertyDescriptor NAMESPACE = new PropertyDescriptor.Builder()//
            .name("name-space")//
            .displayName("Name Space")//
            .description("Specfic the encoding name").required(false).addValidator(Validator.VALID).build();

    public static final PropertyDescriptor DESTINATION = new PropertyDescriptor.Builder().name("Destination")
            .description("Control if XML value is written as a new flowfile attribute '" + XML_ATTRIBUTE_NAME + "' "
                    + "or written in the flowfile content. Writing to flowfile content will overwrite any " + "existing flowfile content.")
            .required(true).allowableValues(DESTINATION_ATTRIBUTE, DESTINATION_CONTENT).defaultValue(DESTINATION_CONTENT).build();

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

    private String jsonPathExpression;
    private boolean destinationContent;
    private String attributeMark;
    private String contentKeyName;
    private String encoding;
    private String elementName;
    private String nameSpace;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(ENCODING);
        properties.add(NAMESPACE);
        properties.add(JSON_ATTRIBUTE_MARK);
        properties.add(JSON_CONTENT_KEY_NAME);
        properties.add(JSON_PATH_EXPRESSION);
        properties.add(ELEMENT_NAME);
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
        jsonPathExpression = context.getProperty(JSON_PATH_EXPRESSION).getValue();
        destinationContent = DESTINATION_CONTENT.equals(context.getProperty(DESTINATION).getValue());
        attributeMark = context.getProperty(JSON_ATTRIBUTE_MARK).getValue();
        contentKeyName = context.getProperty(JSON_CONTENT_KEY_NAME).getValue();
        encoding = context.getProperty(ENCODING).getValue();
        elementName = context.getProperty(ELEMENT_NAME).getValue();
        nameSpace = context.getProperty(NAMESPACE).getValue();
        attributeMark = StringUtils.isEmpty(attributeMark) ? null : attributeMark;
        contentKeyName = StringUtils.isEmpty(contentKeyName) ? null : contentKeyName;
        nameSpace = StringUtils.isEmpty(nameSpace) ? null : nameSpace;
        elementName = StringUtils.isEmpty(elementName) ? null : elementName;
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
        final String xmlContent = convertJsonToXMLStr(content, jsonPathExpression, attributeMark, nameSpace, contentKeyName, encoding, elementName);
        try {
            final StopWatch stopWatch = new StopWatch(true);
            FlowFile successFlow = null;
            if (destinationContent) {
                FlowFile contFlowfile = session.create(flowFile);
                contFlowfile = session.write(contFlowfile, (in, out) -> {
                    try (OutputStream outputStream = new BufferedOutputStream(out)) {
                        outputStream.write(xmlContent.getBytes("UTF-8"));
                    }
                });
                contFlowfile = session.putAttribute(contFlowfile, CoreAttributes.MIME_TYPE.key(), APPLICATION_XML);
                session.remove(flowFile);
                successFlow = contFlowfile;
            } else {
                successFlow = session.putAttribute(flowFile, XML_ATTRIBUTE_NAME, xmlContent);
            }
            session.getProvenanceReporter().modifyContent(successFlow, stopWatch.getElapsed(TimeUnit.MILLISECONDS));
            session.transfer(successFlow, REL_SUCCESS);

        } catch (Exception e) {
            getLogger().error(e.getMessage());
            session.transfer(flowFile, REL_FAILURE);
        }
    }

    protected String convertJsonToXMLStr(String jsonStr, String jsonPathExpression, String attributeMark, String nameSpace, String contentKeyName, String encoding, String elementName) {

        String xml = "";
        try {
            XMLConverter xmlInstance = new XMLConverter();
            xmlInstance.setNameSpace(nameSpace);
            xmlInstance.setAttributeMark(attributeMark);
            if (contentKeyName != null) {
                xmlInstance.setDataTagName(contentKeyName);

            } else {
                xmlInstance.setDataTagName("");
            }
            if (jsonPathExpression == null) {
                JSONObject jsonObject = new JSONObject(jsonStr);
                xml = xmlInstance.toString_p(jsonObject, elementName);
            } else {
                Object items = JsonPath.read(jsonStr, jsonPathExpression);
                Map<String, Object> map = new HashMap<String, Object>();
                map.put(elementName, items);
                JSONObject jsonObject = new JSONObject(map);
                if ((items instanceof JSONArray || items.getClass().isArray())) {
                    if (((List) items).size() == 0) {
                        map.put(elementName, "");
                        jsonObject = new JSONObject(map);
                    }
                    xml = xmlInstance.toString_p(jsonObject, "list");
                } else {
                    xml = xmlInstance.toString_p(jsonObject, null);
                }
            }
            xml = "<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>" + xml;
        } catch (Exception e) {
            return xml;

        }
        return xml;
    }
}
