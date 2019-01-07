package org.apache.nifi.processors.hl7;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.behavior.SideEffectFree;
import org.apache.nifi.annotation.behavior.SupportsBatching;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.stream.io.StreamUtils;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.parser.XMLParser;
import ca.uhn.hl7v2.validation.ValidationContext;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;

@SideEffectFree
@SupportsBatching
@Marks(categories={"转换控制/数据转换"}, createdDate="2018-08-07")
@InputRequirement(Requirement.INPUT_REQUIRED)
@Tags({"HL7", "health level 7", "healthcare", "convert", "xml", "pipe", "attributes"})
@CapabilityDescription("Convert HL7 pipe message to XML message,also can convert XML message to a standard HL7 pipe message.")

public class ConvertHL7Message extends AbstractProcessor {

    static  class MutexValidator implements Validator{

        private PropertyDescriptor source_value;
        private PropertyDescriptor target_value;
        private String message;

        public MutexValidator(String source_name,String target_name){
            this.source_value = new PropertyDescriptor.Builder().name(source_name).build();
            this.target_value = new PropertyDescriptor.Builder().name(target_name).build();
            this.message = source_name + " and " + target_name +" could not be same!";
        }
        @Override
        public ValidationResult validate(final String subject, final String value, final org.apache.nifi.components.ValidationContext context) {
            final String sourceType = context.getProperty(source_value).getValue();
            final String targetType = context.getProperty(target_value).getValue();
            return new ValidationResult.Builder().subject(subject).input(value).explanation(message).valid(!sourceType.equals(targetType)).build();
        }
    }

    public static final PropertyDescriptor CHARACTER_SET = new PropertyDescriptor.Builder()
            .name("Character Encoding")
            .displayName("Character Encoding")
            .description("The Character Encoding that is used to encode the HL7 data")
            .required(true)
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .addValidator(StandardValidators.CHARACTER_SET_VALIDATOR)
            .defaultValue("UTF-8")
            .build();

    public static final PropertyDescriptor SOURCE_TYPE = new PropertyDescriptor.Builder()
            .name("Source message Type")
            .displayName("Source message Type")
            .description("Set source message type,pipe or xml.")
            .required(true)
            .allowableValues("PIPE", "XML")
            .defaultValue("XML")
            .build();

    public static final PropertyDescriptor TARGET_TYPE = new PropertyDescriptor.Builder()
            .name("Target message Type")
            .displayName("Target message Type")
            .description("Set target message type,pipe or xml.")
            .required(true)
            .allowableValues("PIPE", "XML")
            .defaultValue("PIPE")
            .addValidator(new MutexValidator("Source message Type","Target message Type"))
            .build();

    public static final PropertyDescriptor SKIP_VALIDATION = new PropertyDescriptor.Builder()
            .name("skip-validation")
            .displayName("Skip Validation")
            .description("Whether or not to validate HL7 message values")
            .required(true)
            .allowableValues("true", "false")
            .defaultValue("true")
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();

    public static final PropertyDescriptor HL7_INPUT_VERSION = new PropertyDescriptor.Builder()
            .name("hl7-input-version")
            .displayName("HL7 Input Version")
            .description("The HL7 version to use for parsing and validation")
            .required(true)
            .allowableValues("autodetect", "2.2", "2.3", "2.3.1", "2.4", "2.5", "2.5.1", "2.6")
            .defaultValue("autodetect")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("A FlowFile is routed to this relationship if it is properly parsed as HL7 and convert HL7 message!")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("A FlowFile is routed to this relationship if it cannot convert HL7 message. This would happen if the FlowFile does not contain valid HL7 data")
            .build();

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        final List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(CHARACTER_SET);
        properties.add(SOURCE_TYPE);
        properties.add(TARGET_TYPE);
        properties.add(SKIP_VALIDATION);
        properties.add(HL7_INPUT_VERSION);
        return properties;
    }

    @Override
    public Set<Relationship> getRelationships() {
        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
        return relationships;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }
        final Charset charset = Charset.forName(context.getProperty(CHARACTER_SET).evaluateAttributeExpressions(flowFile).getValue());
        final String sourceType = context.getProperty(SOURCE_TYPE).getValue();
        final String targetType = context.getProperty(TARGET_TYPE).getValue();
        final Boolean skipValidation = context.getProperty(SKIP_VALIDATION).asBoolean();
        final String inputVersion = context.getProperty(HL7_INPUT_VERSION).getValue();
        final byte[] buffer = new byte[(int) flowFile.getSize()];
        session.read(flowFile, new InputStreamCallback() {
            @Override
            public void process(final InputStream in) throws IOException {
                StreamUtils.fillBuffer(in, buffer);
            }
        });
        final HapiContext hapiContext = new DefaultHapiContext();
        if (!inputVersion.equals("autodetect")) {
            hapiContext.setModelClassFactory(new CanonicalModelClassFactory(inputVersion));
        }
        if (skipValidation) {
            hapiContext.setValidationContext((ValidationContext) ValidationContextFactory.noValidation());
        }

        final String hl7Text = new String(buffer,charset);
        try {
            if(sourceType.equals(targetType)){
                getLogger().debug("HL7 source type and target type are same,so nothing to do.");
            }else{
                String result = convertMessage(hl7Text,sourceType,targetType,hapiContext);
                final byte[] resultBytes = result.getBytes(charset);
                flowFile = session.write(flowFile, new OutputStreamCallback() {
                    @Override
                    public void process(final OutputStream out) throws IOException {
                        out.write(resultBytes);
                    }
                });
                getLogger().debug("Convert HL7 message from " + sourceType + " type to "+targetType +" type.");
            }
        } catch (final HL7Exception e) {
            getLogger().error("Failed to convert HL7 message from " + sourceType + " type to "+targetType +" type:" + e);
            session.transfer(flowFile, REL_FAILURE);
            return;
        }
        session.transfer(flowFile, REL_SUCCESS);
    }

    private String convertMessage(String hl7Text, String sourceType, String targetType, HapiContext hapiContext) throws HL7Exception {
        final PipeParser pipeParser = hapiContext.getPipeParser();
        final XMLParser xmlParser = hapiContext.getXMLParser();
        Message m = null;
        String result = "";
        if("XML".equals(sourceType)){
            m = xmlParser.parse(hl7Text);
        }else if("PIPE".equals(sourceType)){
            m = pipeParser.parse(hl7Text);
        }
        if("XML".equals(targetType)){
            result = xmlParser.encode(m);
        }else if("PIPE".equals(targetType)){
            result = pipeParser.encode(m);
        }
        return result;
    }

}
