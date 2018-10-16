package com.baishancloud.orchsym.processors.sap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.behavior.SideEffectFree;
import org.apache.nifi.annotation.behavior.SupportsBatching;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.io.StreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.util.StopWatch;

import com.baishancloud.orchsym.processors.sap.i18n.Messages;
import com.baishancloud.orchsym.processors.sap.option.BoolOption;
import com.baishancloud.orchsym.processors.sap.option.ContainerOption;
import com.baishancloud.orchsym.sap.SAPException;
import com.baishancloud.orchsym.sap.client.SAPClientConnectionPoolService;

/**
 * @author GU Guoqiang
 *
 */
@SideEffectFree
@SupportsBatching
@Marks(categories={"数据处理/数据抓取"}, createdDate="2018-07-30")
@Tags({ "SAP", "RFC", "ABAP", "JCo", "JSON"})
@InputRequirement(Requirement.INPUT_ALLOWED)
@CapabilityDescription("通过配置SAP连接和RFC功能远程调用函数（Funnction）并将结果输出为JSON格式。"//
        + "如果函数需要输入参数，则必须提供输入流，然后逐条记录将作为输入参数给函数调用，即通过输入记录中的名字和值来对应函数的输入参数。")
public class InvokeSAPRFC extends AbstractSAPProcessor {

    static final PropertyDescriptor SAP_CP = new PropertyDescriptor.Builder().name("sap-conn-pool") //$NON-NLS-1$
            .displayName(Messages.getString("SAPProcessor.Connector"))//$NON-NLS-1$
            .description(Messages.getString("SAPProcessor.Connector_Desc"))//$NON-NLS-1$
            .required(true)//
            .identifiesControllerService(SAPClientConnectionPoolService.class)//
            .build();
    
    static final PropertyDescriptor SAP_FUNCTION = new PropertyDescriptor.Builder().name("sap-function") //$NON-NLS-1$
            .displayName(Messages.getString("SAPProcessor.Function"))//$NON-NLS-1$
            .description(Messages.getString("SAPProcessor.Function_Desc"))//$NON-NLS-1$
            .required(true)//
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    static final PropertyDescriptor SAP_EXPORT_TABLES = new PropertyDescriptor.Builder()//
            .name("sap-export-tables") //$NON-NLS-1$
            .displayName(Messages.getString("InvokeSAPRFC.ExportTables")) //$NON-NLS-1$
            .description(Messages.getString("InvokeSAPRFC.ExportTables_Desc"))//$NON-NLS-1$
            .required(false)//
            .addValidator(Validator.VALID)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    static final PropertyDescriptor JSON_CONTAINER_OPTIONS = new PropertyDescriptor.Builder()//
            .name("json-container-options")//$NON-NLS-1$
            .displayName(Messages.getString("InvokeSAPRFC.ContainerOption")) //$NON-NLS-1$
            .description(Messages.getString("InvokeSAPRFC.ContainerOption_Desc", ContainerOption.NONE.getDisplayName(), ContainerOption.ARRAY.getDisplayName())) //$NON-NLS-1$
            .required(true)//
            .defaultValue(ContainerOption.ARRAY.getValue())//
            .allowableValues(ContainerOption.getAll())//
            .build();

    static final PropertyDescriptor JSON_WRAP_SINGLE_RECORD = new PropertyDescriptor.Builder()//
            .name("json-wrap-single-record")//$NON-NLS-1$
            .displayName(Messages.getString("InvokeSAPRFC.Wrap"))//$NON-NLS-1$
            .description(Messages.getString("InvokeSAPRFC.Wrap_Desc", JSON_CONTAINER_OPTIONS.getName()))//$NON-NLS-1$
            .required(false)//
            .defaultValue(BoolOption.NO.getValue())//
            .allowableValues(BoolOption.getAll())//
            .build();

    protected volatile SAPClientConnectionPoolService sapClientCP;
    protected volatile String functionName;
    protected volatile boolean ignoreEmptyValues;
    private volatile String[] exportTables;
    private volatile boolean useContainer;
    private volatile boolean wrapSingleRecord;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        super.init(context);

        final List<PropertyDescriptor> descriptors = new ArrayList<>(this.descriptors);
        descriptors.add(SAP_CP);
        descriptors.add(SAP_FUNCTION);
        descriptors.add(SAP_EXPORT_TABLES);

        descriptors.add(JSON_CONTAINER_OPTIONS);
        descriptors.add(JSON_WRAP_SINGLE_RECORD);

        descriptors.add(JSON_IGNORE_EMPTY_VALUES);
        this.descriptors = Collections.unmodifiableList(descriptors);
    }

    public void onScheduled(final ProcessContext context) {
        super.onScheduled(context);

        functionName = context.getProperty(SAP_FUNCTION).evaluateAttributeExpressions().getValue();
        if (StringUtils.isBlank(functionName)) {
            throw new ProcessException(Messages.getString("InvokeSAPRFC.EmptyFunction")); //$NON-NLS-1$
        }
        ignoreEmptyValues = context.getProperty(JSON_IGNORE_EMPTY_VALUES).asBoolean();
        
        sapClientCP = context.getProperty(SAP_CP).evaluateAttributeExpressions().asControllerService(SAPClientConnectionPoolService.class);
        try {
            sapClientCP.connect();
        } catch (SAPException e) {
            throw new ProcessException(e);
        }
        if (!sapClientCP.isConnected()) {
            throw new ProcessException(Messages.getString("InvokeSAPRFC.ConnectFailureMessage")); //$NON-NLS-1$
        }

        String tables = context.getProperty(SAP_EXPORT_TABLES).evaluateAttributeExpressions().getValue();
        if (StringUtils.isNotBlank(tables)) {
            exportTables = tables.trim().split(","); //$NON-NLS-1$
        }

        useContainer = (ContainerOption.get(context.getProperty(JSON_CONTAINER_OPTIONS).getValue()) == ContainerOption.ARRAY);
        // Wrap a single record (inclusive of no records) only when a container is being used
        wrapSingleRecord = context.getProperty(JSON_WRAP_SINGLE_RECORD).asBoolean() && useContainer;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        final FlowFile original = session.get();
        final boolean hasInput = (original != null);

        final Map<String, String> attributes = new HashMap<>();
        try {
            final StopWatch stopWatch = new StopWatch(true);

            final AtomicLong recordCount = new AtomicLong();

            FlowFile output = null;
            if (hasInput) {
                attributes.putAll(original.getAttributes());

                output = session.write(original, new StreamCallback() {
                    /**
                     * most like ConvertAvroToJSON
                     */
                    @Override
                    public void process(InputStream rawIn, OutputStream rawOut) throws IOException {
                        try (final InputStream in = new BufferedInputStream(rawIn);
                                final OutputStream out = new BufferedOutputStream(rawOut);
                                final DataFileStream<GenericRecord> reader = new DataFileStream<>(in, new GenericDatumReader<GenericRecord>())) {

                            GenericRecord currRecord = null;
                            if (reader.hasNext()) {
                                currRecord = reader.next();
                                recordCount.incrementAndGet();
                            }

                            // Open container if desired output is an array format and there are are multiple records or
                            // if configured to wrap single record
                            if (reader.hasNext() && useContainer || wrapSingleRecord) {
                                out.write('[');
                            }

                            Object result = callFunction(currRecord);

                            // Determine the initial output record, inclusive if we should have an empty set of Avro records
                            write(out, result);

                            while (reader.hasNext()) {
                                if (useContainer) {
                                    out.write(',');
                                } else {
                                    out.write('\n');
                                }

                                currRecord = reader.next(currRecord);

                                result = callFunction(currRecord);
                                if (result != null) {
                                    write(out, result);
                                    recordCount.incrementAndGet();
                                }
                            }

                            // Close container if desired output is an array format and there are multiple records or if
                            // configured to wrap a single record
                            if (recordCount.get() > 1 && useContainer || wrapSingleRecord) {
                                out.write(']');
                            }

                        } catch (SAPException e) {
                            throw new ProcessException(e.getLocalizedMessage(), e);
                        }
                    }
                });

            } else {
                output = session.create();
                output = session.write(output, new OutputStreamCallback() {

                    @Override
                    public void process(OutputStream rawOut) throws IOException {
                        try (final OutputStream out = new BufferedOutputStream(rawOut);) {
                            Object result = callFunction(null);// no import params
                            if (wrapSingleRecord) {
                                out.write('[');
                            }
                            write(out, result);
                            if (wrapSingleRecord) {
                                out.write(']');
                            }
                            recordCount.incrementAndGet();
                        } catch (SAPException e) {
                            throw new ProcessException(e.getLocalizedMessage(), e);
                        }
                    }
                });
            }
            final Map<String, String> destinationAttributes = sapClientCP.getAttributes();
            destinationAttributes.keySet().stream()//
                    .forEach(n -> attributes.put("sap." + n, destinationAttributes.get(n))); //$NON-NLS-1$
            attributes.put(CoreAttributes.MIME_TYPE.key(), APPLICATION_JSON);
            output = session.putAllAttributes(output, attributes);

            session.getProvenanceReporter().modifyContent(output, stopWatch.getElapsed(TimeUnit.MILLISECONDS));

            session.transfer(output, REL_SUCCESS);
        } catch (Exception e) {
            getLogger().error("Routing to {} due to exception: {}", new Object[] { REL_FAILURE.getName(), e }, e); //$NON-NLS-1$
            session.transfer(original, REL_FAILURE);
        }
    }

    private Object callFunction(GenericRecord record) throws SAPException {
        return sapClientCP.call(functionName, record, ignoreEmptyValues, exportTables);
    }
}
