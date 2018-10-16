package com.baishancloud.orchsym.processors.sap;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.behavior.SideEffectFree;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.lifecycle.OnStopped;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.lookup.KeyValueLookupService;
import org.apache.nifi.processor.AbstractSessionFactoryProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessSessionFactory;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;

import com.baishancloud.orchsym.processors.sap.event.SAPTCPEvent;
import com.baishancloud.orchsym.processors.sap.i18n.Messages;
import com.baishancloud.orchsym.sap.SAPException;
import com.baishancloud.orchsym.sap.server.SAPRequestCallback;
import com.baishancloud.orchsym.sap.server.SAPServerConnectionPoolService;
import com.baishancloud.orchsym.sap.server.SAPServerException;

/**
 * @author GU Guoqiang
 *
 */
@SideEffectFree
@Marks(categories={"数据处理/数据抓取", "网络/网络通信"}, createdDate="2018-07-30")
@Tags({ "SAP", "TCP", "RFC", "ABAP", "JCo", "JSON" })
@InputRequirement(Requirement.INPUT_FORBIDDEN)
@CapabilityDescription("通过配置SAP连接来启动SAP JCo服务器，提供给ABAP的TCP/IP连接来调用服务器注册的远程动态函数。")
@WritesAttributes({ @WritesAttribute(attribute = ListenSAPTCP.KEY_CONTEXT_ID, description = "The sending context id of the messages.") })
public class ListenSAPTCP extends AbstractSessionFactoryProcessor {

    public static final String KEY_CONTEXT_ID = "sap.context.identifier";

    static final PropertyDescriptor SAP_CP = new PropertyDescriptor.Builder()//
            .name("sap-conn-pool") //$NON-NLS-1$
            .displayName(Messages.getString("SAPProcessor.Connector"))//$NON-NLS-1$
            .description(Messages.getString("SAPProcessor.Connector_Desc"))//$NON-NLS-1$
            .required(true)//
            .identifiesControllerService(SAPServerConnectionPoolService.class)//
            .build();

    static final PropertyDescriptor SAP_FUNCTION_NAME = new PropertyDescriptor.Builder()//
            .name("sap-function-name") //$NON-NLS-1$
            .displayName(Messages.getString("ListenSAPTCP.Function"))//$NON-NLS-1$
            .description(Messages.getString("ListenSAPTCP.Function_Desc"))//$NON-NLS-1$
            .required(true)//
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    static final PropertyDescriptor SAP_IMPORT_TABLES = new PropertyDescriptor.Builder()//
            .name("sap-import-tables") //$NON-NLS-1$
            .displayName(Messages.getString("ListenSAPTCP.ImportTables")) //$NON-NLS-1$
            .description(Messages.getString("ListenSAPTCP.ImportTables_Desc"))//$NON-NLS-1$
            .required(false)//
            .addValidator(Validator.VALID)//
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
            .build();

    // static final PropertyDescriptor SAP_CUSTOM_FUN = new PropertyDescriptor.Builder()//
    // .name("sap-custom-fun")//$NON-NLS-1$
    // .displayName(Messages.getString("ListenSAPTCP.CustomFun"))//$NON-NLS-1$
    // .description(Messages.getString("ListenSAPTCP.CustomFun_Desc"))//$NON-NLS-1$
    // .required(false)//
    // .defaultValue(BoolOption.NO.getValue()).allowableValues(BoolOption.getAll())//
    // .addValidator(StandardValidators.NON_BLANK_VALIDATOR)//
    // .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
    // .build();
    //
    // static final PropertyDescriptor SAP_FUN_METADATA = new PropertyDescriptor.Builder()//
    // .name("sap-function-metadata")//$NON-NLS-1$
    // .displayName(Messages.getString("ListenSAPTCP.FunMetadata"))//$NON-NLS-1$
    // .description(Messages.getString("ListenSAPTCP.FunMetadata_Desc"))//$NON-NLS-1$
    // .required(false)//
    // .addValidator(StandardValidators.NON_BLANK_VALIDATOR)//
    // .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)//
    // .build();

    static final PropertyDescriptor RESPONDER_CONTEXT_MAP = new PropertyDescriptor.Builder()//
            .name("Responder-context-map")//$NON-NLS-1$
            .displayName(Messages.getString("ListenSAPTCP.ResponderContext"))//$NON-NLS-1$
            .description(Messages.getString("ListenSAPTCP.ResponderContext_Desc"))//$NON-NLS-1$
            .required(false)//
            .identifiesControllerService(KeyValueLookupService.class)//
            .build();

    static final PropertyDescriptor JSON_IGNORE_EMPTY_VALUES = AbstractSAPProcessor.JSON_IGNORE_EMPTY_VALUES;

    static final Relationship REL_SUCCESS = AbstractSAPProcessor.REL_SUCCESS;

    protected List<PropertyDescriptor> descriptors;
    protected Set<Relationship> relationships;

    private volatile SAPServerConnectionPoolService sapServerCP;
    private volatile String functionName;
    private volatile String[] importTables;
    private volatile String funMetadataJson;
    private volatile boolean ignoreEmptyValues;

    private volatile KeyValueLookupService kvLookupService;
    private volatile String contextIdentifier;

    private volatile AtomicBoolean stopped = new AtomicBoolean(false);

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<>();
        descriptors.add(SAP_CP);
        descriptors.add(SAP_FUNCTION_NAME);
        descriptors.add(SAP_IMPORT_TABLES);
        // descriptors.add(SAP_CUSTOM_FUN);
        // descriptors.add(SAP_FUN_METADATA);

        descriptors.add(RESPONDER_CONTEXT_MAP);
        descriptors.add(JSON_IGNORE_EMPTY_VALUES);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<>(2);
        relationships.add(REL_SUCCESS);
        this.relationships = Collections.unmodifiableSet(relationships);

    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    @Override
    public void onPropertyModified(PropertyDescriptor descriptor, String oldValue, String newValue) {
        super.onPropertyModified(descriptor, oldValue, newValue);
        
    }

    @OnScheduled
    public void onScheduled(ProcessContext context) {
        contextIdentifier = this.getIdentifier();
        ignoreEmptyValues = context.getProperty(JSON_IGNORE_EMPTY_VALUES).asBoolean();

        sapServerCP = context.getProperty(SAP_CP).evaluateAttributeExpressions().asControllerService(SAPServerConnectionPoolService.class);
        try {
            sapServerCP.connect();
        } catch (SAPException e) {
            throw new ProcessException(e);
        }

        functionName = context.getProperty(SAP_FUNCTION_NAME).evaluateAttributeExpressions().getValue();
        String itabValues = context.getProperty(SAP_IMPORT_TABLES).evaluateAttributeExpressions().getValue();
        if (StringUtils.isNotBlank(itabValues)) {
            importTables = Arrays.asList(itabValues.split(",")).stream().map(f -> f.trim()).filter(f -> StringUtils.isNotBlank(f)).toArray(String[]::new);
        }

        // boolean customFun = context.getProperty(SAP_CUSTOM_FUN).evaluateAttributeExpressions().asBoolean();
        // if (customFun) {
        // String funMetadata = context.getProperty(SAP_FUN_METADATA).evaluateAttributeExpressions().getValue();
        // if (StringUtils.isBlank(funMetadata)) {
        // throw new ProcessException("The metadata of function should not be empty");
        // } else {
        // try {
        // new ObjectMapper().readTree(funMetadata);
        // } catch (IOException e) {
        // throw new ProcessException("The metadata of function is invalid format");
        // }
        // funMetadataJson = funMetadata;
        // }
        // }

        // if set responder, need keep alive
        kvLookupService = context.getProperty(RESPONDER_CONTEXT_MAP).asControllerService(KeyValueLookupService.class);
        if (kvLookupService != null) {
            kvLookupService.register(contextIdentifier, new SAPTCPEvent());
        }

    }

    @OnStopped
    public void onStopped(ProcessContext context) {
        stopped.set(true);
        sapServerCP.unregistryRequest(contextIdentifier);
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSessionFactory sessionFactory) throws ProcessException {
        final SAPRequestCallback requestCallback = new SAPRequestCallback() {

            @Override
            public String getIdentifier() {
                return contextIdentifier;
            }

            @Override
            public String getFunName() {
                return functionName;
            }

            @Override
            public String getFunMetadata() {
                return funMetadataJson;
            }

            @Override
            public String[] getImportTables() {
                return importTables;
            }

            @Override
            public void process(final Object importData) {
                stopped.set(false);
                // set the input data to flow
                processData(importData, context, sessionFactory);
            }

            @Override
            public boolean ignoreEmptyValue() {
                return ignoreEmptyValues;
            }

            @Override
            public String waitResponse() throws SAPServerException {
                // waiting for the output data from response
                while (!stopped.get()) {
                    if (kvLookupService != null) {
                        final Optional<Object> optional = kvLookupService.get(contextIdentifier);
                        if (optional.isPresent()) {
                            SAPTCPEvent event = (SAPTCPEvent) optional.get();
                            if (event.finished.get()) {
                                stopped.set(true);
                                return event.getAndClean();
                            }
                        } // else // if not set, need wait
                    }
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        //
                    }
                }
                return null;
            }

        };
        sapServerCP.registryRequest(requestCallback);
    }

    protected void processData(final Object data, final ProcessContext context, final ProcessSessionFactory sessionFactory) {
        final ProcessSession session = sessionFactory.createSession();

        FlowFile output = session.create();
        output = session.write(output, new OutputStreamCallback() {

            @Override
            public void process(OutputStream out) throws IOException {
                AbstractSAPProcessor.write(out, data);
            }

        });

        final Map<String, String> attributes = new HashMap<>();
        attributes.put(KEY_CONTEXT_ID, this.getIdentifier());
        attributes.put(CoreAttributes.MIME_TYPE.key(), AbstractSAPProcessor.APPLICATION_JSON);

        output = session.putAllAttributes(output, attributes);

        session.getProvenanceReporter().create(output);
        session.transfer(output, REL_SUCCESS);

        session.commit();
    }
}
