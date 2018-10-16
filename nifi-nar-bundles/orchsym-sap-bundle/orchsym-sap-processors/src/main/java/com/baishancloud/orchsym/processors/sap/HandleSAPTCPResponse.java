package com.baishancloud.orchsym.processors.sap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.lookup.KeyValueLookupService;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.StreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.util.StopWatch;
import org.codehaus.jackson.map.ObjectMapper;

import com.baishancloud.orchsym.processors.sap.event.SAPTCPEvent;
import com.baishancloud.orchsym.processors.sap.i18n.Messages;
import com.baishancloud.orchsym.processors.sap.option.ResponseOption;

/**
 * @author GU Guoqiang
 *
 */
@Marks(categories={"数据处理/数据输出", "网络/网络通信"}, createdDate="2018-07-30")
@Tags({ "SAP", "TCP", "RFC", "ABAP", "Response", "JCo" })
@InputRequirement(Requirement.INPUT_REQUIRED)
public class HandleSAPTCPResponse extends AbstractSAPProcessor {

    static final PropertyDescriptor RESPONDER_CONTEXT_MAP = new PropertyDescriptor.Builder()//
            .name("Responder-context-map")//$NON-NLS-1$
            .displayName(Messages.getString("ListenSAPTCP.ResponderContext"))//$NON-NLS-1$
            .description(Messages.getString("ListenSAPTCP.ResponderContext_Desc"))//$NON-NLS-1$
            .required(true)//
            .identifiesControllerService(KeyValueLookupService.class)//
            .build();

    static final PropertyDescriptor RESPONSE_OPTIONS = new PropertyDescriptor.Builder()//
            .name("response-options")//$NON-NLS-1$
            .displayName(Messages.getString("HandleSAPTCPResponse.ResponseOption"))//$NON-NLS-1$
            .description(Messages.getString("HandleSAPTCPResponse.ResponseOption_Desc"))//$NON-NLS-1$
            .required(true)//
            .defaultValue(ResponseOption.FLOW.getValue()).allowableValues(ResponseOption.getAll())//
            .build();

    static final PropertyDescriptor RESPONSE_DATA = new PropertyDescriptor.Builder().name("response-data")//
            .displayName(Messages.getString("HandleSAPTCPResponse.ResponseData"))//$NON-NLS-1$
            .description(Messages.getString("HandleSAPTCPResponse.ResponseData_Desc", ResponseOption.CUSTOM.getDisplayName()))//$NON-NLS-1$
            .required(false)//
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)//
            .build();

    static final Relationship REL_RESPONSE = new Relationship.Builder().name("Response")//$NON-NLS-1$
            .description("A Response FlowFile will be routed upon success.")//
            .build();

    private volatile ResponseOption responseOption;
    protected volatile KeyValueLookupService kvLookupService;
    protected volatile String responseData;
    protected volatile SAPTCPEvent sapEvent;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        super.init(context);

        final List<PropertyDescriptor> descriptors = new ArrayList<>(this.descriptors);
        descriptors.add(RESPONDER_CONTEXT_MAP);
        descriptors.add(RESPONSE_OPTIONS);
        descriptors.add(RESPONSE_DATA);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(REL_RESPONSE);
        relationships.add(REL_FAILURE);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public void onScheduled(ProcessContext context) {
        super.onScheduled(context);

        responseOption = ResponseOption.get(context.getProperty(RESPONSE_OPTIONS).getValue());
        kvLookupService = context.getProperty(RESPONDER_CONTEXT_MAP).asControllerService(KeyValueLookupService.class);
    }

    @Override
    public void onStopped(ProcessContext context) {
        super.onStopped(context);

        if (sapEvent != null) { // set flag
            sapEvent.finishAndClean();
        }
    }

    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }
        final StopWatch stopWatch = new StopWatch(true);

        final String contextIdentifier = flowFile.getAttribute(ListenSAPTCP.KEY_CONTEXT_ID);
        if (contextIdentifier == null || kvLookupService == null) {
            session.transfer(flowFile, REL_FAILURE);
            getLogger().warn("Failed to respond to TCP request for {} because FlowFile did not have an '" + ListenSAPTCP.KEY_CONTEXT_ID + "' attribute", new Object[] { flowFile }); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        Optional<Object> optional = kvLookupService.get(contextIdentifier);
        if (!optional.isPresent()) {
            session.transfer(flowFile, REL_FAILURE);
            getLogger().warn("Failed to respond to TCP request for {} because can't find the value from responder context map for key '" + ListenSAPTCP.KEY_CONTEXT_ID + '\'', //$NON-NLS-1$
                    new Object[] { flowFile });
            return;
        }

        // test the custom data
        if (responseOption == ResponseOption.CUSTOM) {
            responseData = context.getProperty(RESPONSE_DATA).evaluateAttributeExpressions(flowFile).getValue();
            if (StringUtils.isNotBlank(responseData)) { // have data
                try {
                    // test the value is valid json or not
                    new ObjectMapper().readTree(responseData);
                } catch (IOException e) {
                    throw new ProcessException(Messages.getString("HandleSAPTCPResponse.wrongJsonValue"), e);
                }
            }
        }
        sapEvent = (SAPTCPEvent) optional.get();

        try {
            FlowFile outputFlowFile = session.write(flowFile, new StreamCallback() {

                @Override
                public void process(InputStream rawIn, OutputStream rawOut) throws IOException {
                    try (final OutputStream out = new BufferedOutputStream(rawOut)) {
                        String result = null;
                        if (responseOption == ResponseOption.FLOW) {
                            try (final InputStream in = new BufferedInputStream(rawIn);

                                    final DataFileStream<GenericRecord> reader = new DataFileStream<>(in, new GenericDatumReader<GenericRecord>())) {
                                GenericRecordString recordString = new GenericRecordString();

                                GenericRecord currRecord = null;
                                while (reader.hasNext()) {
                                    currRecord = reader.next(currRecord);
                                    recordString.write(currRecord);
                                }
                                if (recordString.getCount() > 0) { // have record
                                    result = recordString.getResult();
                                }
                            }
                        } else if (responseOption == ResponseOption.CUSTOM) {
                            if (StringUtils.isNotBlank(responseData)) { // have valid the data before.
                                result = responseData;
                            }
                        }

                        writeStr(out, result); // if no result, will write empty json also.
                        sapEvent.finishAndSet(result);
                    }
                }

            });

            // add attributes
            final Map<String, String> attributes = new HashMap<>();
            attributes.put(CoreAttributes.MIME_TYPE.key(), APPLICATION_JSON);
            outputFlowFile = session.putAllAttributes(outputFlowFile, attributes);

            session.getProvenanceReporter().modifyContent(outputFlowFile, stopWatch.getElapsed(TimeUnit.MILLISECONDS));
            session.transfer(outputFlowFile, REL_RESPONSE);

            getLogger().info("Successfully responded to TCP Request for {} ", new Object[] { outputFlowFile }); //$NON-NLS-1$
        } catch (Exception e) {
            getLogger().error("Routing to {} due to exception: {}", new Object[] { REL_FAILURE.getName(), e }, e); //$NON-NLS-1$
            session.transfer(flowFile, REL_FAILURE);
        }

    }

    static class GenericRecordString extends GenericData {
        private final StringBuilder result;
        private int count = 0;

        public GenericRecordString() {
            super();
            this.result = new StringBuilder();
        }

        public void write(GenericRecord record) {
            if (count > 0) {
                result.append(',');
            }
            count++;
            toString(record, result);
        }

        public int getCount() {
            return count;
        }

        public String getResult() {
            if (count > 1) {
                result.insert(0, '[');
                result.append(']');
            }
            return result.toString();
        }

    }
}
