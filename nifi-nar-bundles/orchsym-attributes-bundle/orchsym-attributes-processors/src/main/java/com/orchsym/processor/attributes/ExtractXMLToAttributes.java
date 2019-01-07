package com.orchsym.processor.attributes;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.attributes.BooleanAllowableValues;
import org.apache.nifi.processor.ProcessorInitializationContext;

/**
 * @author GU Guoqiang
 *
 */
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@Marks(categories = { "Convert & Control/Convert" }, createdDate = "2018-12-14")
@Tags({ "Extract", "Attribute", "Record", "XML" })
@CapabilityDescription("Provide the abblity of extracting the attributes by XPath or XQuery for XML format contents from the incoming flowfile")
public class ExtractXMLToAttributes extends AbstractExtractToAttributesProcessor {

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
            .defaultValue("@")//
            .build();

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

}
