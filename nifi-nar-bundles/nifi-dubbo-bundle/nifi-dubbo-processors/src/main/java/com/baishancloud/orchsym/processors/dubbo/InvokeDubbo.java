package com.baishancloud.orchsym.processors.dubbo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.DynamicProperty;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.behavior.SideEffectFree;
import org.apache.nifi.annotation.behavior.SupportsBatching;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.lifecycle.OnStopped;
import org.apache.nifi.components.AllowableValue;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.StreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.util.StopWatch;
import org.apache.nifi.util.Tuple;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.baishancloud.orchsym.processors.dubbo.param.CheckParam;
import com.baishancloud.orchsym.processors.dubbo.param.CustomParam;
import com.baishancloud.orchsym.processors.dubbo.param.FlowParam;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author GU Guoqiang
 *
 */
@SideEffectFree
@SupportsBatching
@Marks(categories={"数据处理/数据抓取", "网络/网络通信"}, createdDate="2018-06-11")
@Tags({ "dubbo", "get", "json" })
@InputRequirement(Requirement.INPUT_ALLOWED)
@CapabilityDescription("Execute provided request against the Dubbo comsumer. The result will be left in it's orginal form. " + "If it is triggered by an incoming FlowFile, then attributes of "
        + "that FlowFile will be available when evaluating the executing the Dubbo request.")
@DynamicProperty(name = "The name of a input parameter needs to be passed to the Dubbo method being invoked.", //
        value = "The value for this parameter '=' and ',' are not considered valid values and must be escaped . Note, if the value of parameter needs to be an array the format should be key1=value1,key2=value2.  ", //
        expressionLanguageScope = ExpressionLanguageScope.FLOWFILE_ATTRIBUTES, description = "The name provided will be the name sent in the Dubbo method, therefore please make sure "
                + "it matches the api for the Dubbo service being called. In the case of arrays " + "the name will be the name of the array and the key's specified in the value will be the element "
                + "names pased.")
public class InvokeDubbo extends AbstractProcessor {
    /*
     * match the version x, x.y, x.y.z
     */
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\d+(\\.\\d+){0,2}");
    private static final String VERSION_ALL = "*";
    public static final CheckParam CP = new CheckParam();

    public enum PARAM_CHOICE {
        FLOW("Flow"), CUSTOM("Custom");
        private String display;

        private PARAM_CHOICE(String display) {
            this.display = display;
        }

        public String getName() {
            return this.name();
        }

        public String getDisplay() {
            return display;
        }

        public static AllowableValue[] createList() {
            final PARAM_CHOICE[] values = PARAM_CHOICE.values();
            AllowableValue[] allowableValues = new AllowableValue[values.length];
            for (int i = 0; i < values.length; i++) {
                allowableValues[i] = new AllowableValue(values[i].getName(), values[i].getDisplay());
            }
            return allowableValues;
        }

    }

    protected static final PropertyDescriptor ADDRESSES = new PropertyDescriptor.Builder().name("Addresses").description(
            "The addresses of Providers to use for Dubbo consumer. Make sure the address should be matched the providers. If have several addresses, separate via comma and the format is <host>:<port>")
            .required(true).expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY).addValidator(new Validator() {
                @Override
                public ValidationResult validate(final String subject, final String value, final ValidationContext context) {
                    // not empty
                    ValidationResult nonEmptyValidatorResult = StandardValidators.NON_EMPTY_EL_VALIDATOR.validate(subject, value, context);
                    if (!nonEmptyValidatorResult.isValid()) {
                        return nonEmptyValidatorResult;
                    }

                    // check format
                    final List<String> Uris = Arrays.asList(value.split(","));
                    for (String uri : Uris) {
                        final ValidationResult uriValidatorResult = StandardValidators.URI_VALIDATOR.validate(subject, uri, context);
                        if (!uriValidatorResult.isValid()) {
                            return uriValidatorResult;
                        }
                    }
                    return new ValidationResult.Builder().subject(subject).input(value).explanation("Valid address").valid(true).build();
                }
            }).build();

    protected static final PropertyDescriptor SERVICE_INTERFACE = new PropertyDescriptor.Builder().name("Service interface")
            .description("The interface of server exposed by the Dubbo API that should be invoked.").required(true).expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

    protected static final PropertyDescriptor SERVICE_METHOD = new PropertyDescriptor.Builder().name("Service method").description("The method exposed by the Dubbo API that should be invoked.")
            .required(true).expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY).addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

    protected static final PropertyDescriptor SERVICE_VERSION = new PropertyDescriptor.Builder().name("Service version")
            .description("The version of service that should be invoked. if need support all version, can set start '*', and the special versions, like 1.0.0").required(false)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY).addValidator(new Validator() {
                @Override
                public ValidationResult validate(final String subject, final String value, final ValidationContext context) {
                    // allow empty value, if set, need match * for all or the pattern of version.
                    String reason = null;
                    if (StringUtils.isNotBlank(value) && !VERSION_ALL.equals(value) && !VERSION_PATTERN.matcher(value).matches()) {
                        reason = "Invalid value of version:" + value;
                    }

                    return new ValidationResult.Builder().subject(subject).input(value).explanation(reason).valid(reason == null).build();
                }
            }).build();

    protected static final PropertyDescriptor USERNAME = new PropertyDescriptor.Builder().name("Username").description("The username to use in the case of basic Auth").required(false)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY).addValidator(Validator.VALID).build();

    protected static final PropertyDescriptor PASSWORD = new PropertyDescriptor.Builder().name("Password").description("The password to use in the case of basic Auth").required(false).sensitive(true)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY).addValidator(Validator.VALID).build();

    protected static final PropertyDescriptor TIMEOUT = new PropertyDescriptor.Builder().name("Timeout").description("The timeout of connections to invoke").required(false).defaultValue("10s")
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY).addValidator(StandardValidators.TIME_PERIOD_VALIDATOR).build();

    protected static final PropertyDescriptor MAX_CONNECTIONS = new PropertyDescriptor.Builder().name("Max of connections").description("The max of connections to invoke").required(false)
            .defaultValue("10").expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY).addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR).build();

    protected static final PropertyDescriptor PARAMETERS_CHOICE = new PropertyDescriptor.Builder().name("Parameters choice").description("The choice of parameters settings.").required(true)
            .allowableValues(PARAM_CHOICE.createList()).defaultValue(PARAM_CHOICE.FLOW.getName()).expressionLanguageSupported(ExpressionLanguageScope.NONE).addValidator(Validator.VALID).build();

    protected static final PropertyDescriptor CUSTOM_PARAMETERS = new PropertyDescriptor.Builder().name("Custom parameters")
            .description("The custom parameters settings of method to invoke, when the parameters are complex types. the field value support expression.").required(false)
            .expressionLanguageSupported(ExpressionLanguageScope.NONE).addValidator(new Validator() {

                @Override
                public ValidationResult validate(String subject, String value, ValidationContext context) {
                    String reason = null;
                    if (StringUtils.isNotBlank(value)) {
                        try {
                            final JsonNode json = new ObjectMapper().readTree(value);
                            if (!(json instanceof ArrayNode) && !(json instanceof ObjectNode)) {
                                reason = "Invalid json";
                            }
                        } catch (IOException e) {
                            reason = "Invalid json, because: " + e.getMessage();
                        }
                    }

                    return new ValidationResult.Builder().subject(subject).input(value).explanation(reason).valid(reason == null).build();
                }
            }).build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder().name("Success").description("A Response FlowFile will be routed upon success.").build();

    public static final Relationship REL_FAILURE = new Relationship.Builder().name("Failure")
            .description("The original FlowFile will be routed on any type of connection failure, timeout or general exception. " + "It will have new attributes detailing the request.").build();

    static final String FIELD_RESPONSE = "response";

    static final String APPLICATION_JSON = "application/json";
    static final String ATTR_SERVER = "dubbo.server";
    static final String ATTR_APP_NAME = "dubbo.application.name";

    private List<PropertyDescriptor> descriptors;
    private Set<Relationship> relationships;

    private volatile String methodName;
    private volatile GenericService genericService;
    private volatile ReferenceConfig<GenericService> genericReference;
    private volatile PARAM_CHOICE choice;
    private volatile List<CustomParam> customParameters;
    private volatile boolean customWithoutExpression;
    private volatile String[] customParametersTypes;
    private volatile Object[] customArgsValues;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(ADDRESSES);
        descriptors.add(SERVICE_INTERFACE);
        descriptors.add(SERVICE_METHOD);
        descriptors.add(SERVICE_VERSION);
        descriptors.add(USERNAME);
        descriptors.add(PASSWORD);
        descriptors.add(TIMEOUT);
        descriptors.add(MAX_CONNECTIONS);
        descriptors.add(PARAMETERS_CHOICE);
        descriptors.add(CUSTOM_PARAMETERS);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<>(3);
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
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
    protected PropertyDescriptor getSupportedDynamicPropertyDescriptor(final String propertyDescriptorName) {
        return new PropertyDescriptor.Builder().description("Specifies the method name and parameter names and values for '" + propertyDescriptorName + "' the Dubbo method being called.")
                .name(propertyDescriptorName).addValidator(StandardValidators.NON_EMPTY_VALIDATOR).expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES).dynamic(true).build();
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        choice = PARAM_CHOICE.valueOf(context.getProperty(PARAMETERS_CHOICE).getValue());

        try {
            customParameters = new CustomParam.Parser().parse(context.getProperty(CUSTOM_PARAMETERS).getValue());

        } catch (IOException e) {
            getLogger().error("Parse the custom parameter failed");
        }
        customWithoutExpression = true;
        List<String> parametersTypes = new ArrayList<>();
        List<Object> parametersValues = new ArrayList<>();
        for (CustomParam param : customParameters) {
            if (param.isValuePresent(context)) { // if one field value contain expression, won't eval at more, will eval for each record.
                customWithoutExpression = false;
                break;
            }
            parametersTypes.add(param.getClassName());
            parametersValues.add(param.getValue()); // if no expression, add value directly.
        }
        if (customWithoutExpression) {
            customParametersTypes = parametersTypes.toArray(new String[0]);
            customArgsValues = parametersValues.toArray();
        } else {
            customParametersTypes = new String[0];
            customArgsValues = new Object[0];
        }

        initConsumer(context);
    }

    protected void initConsumer(final ProcessContext context) {
        final String addresses = context.getProperty(ADDRESSES).evaluateAttributeExpressions().getValue();
        final String interfaceName = context.getProperty(SERVICE_INTERFACE).evaluateAttributeExpressions().getValue();
        methodName = context.getProperty(SERVICE_METHOD).evaluateAttributeExpressions().getValue();
        final String version = context.getProperty(SERVICE_VERSION).evaluateAttributeExpressions().getValue();
        final String username = context.getProperty(USERNAME).evaluateAttributeExpressions().getValue();
        final String password = context.getProperty(PASSWORD).evaluateAttributeExpressions().getValue();
        final long timeout = context.getProperty(TIMEOUT).evaluateAttributeExpressions().asTimePeriod(TimeUnit.MILLISECONDS);
        final int maxConnections = context.getProperty(MAX_CONNECTIONS).evaluateAttributeExpressions().asInteger();

        // create dubbo consumer
        ApplicationConfig application = new ApplicationConfig();
        application.setName("Dubbo-invoker" + UUID.randomUUID().toString());

        // add registry addresses
        final String[] addressesArr = addresses.split(",");
        final List<RegistryConfig> registries = new ArrayList<>(addressesArr.length);
        for (String address : addressesArr) {
            if (StringUtils.isNotBlank(address)) {
                RegistryConfig registry = new RegistryConfig();
                registry.setAddress(address);
                if (StringUtils.isNotBlank(username)) {
                    registry.setUsername(username);
                    if (StringUtils.isNotEmpty(password)) {
                        registry.setPassword(password);
                    }
                }
                registries.add(registry);
            }
        }

        genericReference = new ReferenceConfig<GenericService>();
        genericReference.setApplication(application);
        genericReference.setRegistries(registries);
        genericReference.setInterface(interfaceName);
        genericReference.setConnections(maxConnections);
        genericReference.setTimeout((int) timeout);
        if (StringUtils.isNotBlank(version)) {
            genericReference.setVersion(version);
        } else {
            // FIXME, if set *, make sure the provider set some version, if no version, can't use * instead, else won't match and throw timeout error.
            // genericReference.setVersion("*");
        }
        // don't know the exact service api , so use generic service
        genericReference.setGeneric(true);

        try {
            genericService = genericReference.get();
        } catch (IllegalStateException e) {
            // if didn't start the provider,will have error here
            throw new IllegalStateException("Connect the service failure, make sure set the right provider service or service has been started already.", e);
        } catch (Throwable e) {
            throw new IllegalStateException("Initialize consumer failure, make sure the settings are right.", e);
        }
    }

    @OnStopped
    public void onStopped(final ProcessContext context) {
        genericReference.destroy();
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        final FlowFile original = session.get();
        final boolean hasInput = original != null;

        if (!hasInput && choice == PARAM_CHOICE.FLOW) { // when flow, must have input
            final String message = "When in " + PARAM_CHOICE.FLOW.getDisplay() + " mode, must have input flow.";
            getLogger().error(message);
            throw new ProcessException(message);
        }
        // init the custom parameters first
        if (choice == PARAM_CHOICE.CUSTOM && !customWithoutExpression && !customParameters.isEmpty()) { // have custom with expression
            customParametersTypes = customParameters.stream().map(p -> p.getClassName()).toArray(String[]::new);
            customArgsValues = customParameters.stream().map(p -> p.evalValue(context, original)).toArray(Object[]::new);
        }

        try {
            final StopWatch stopWatch = new StopWatch(true);
            FlowFile response = hasInput ? original : session.create();

            response = session.write(response, new StreamCallback() {

                @Override
                public void process(InputStream in, OutputStream out) throws IOException {
                    if (hasInput) {
                        try (DataFileStream<GenericRecord> reader = new DataFileStream<>(in, new GenericDatumReader<GenericRecord>())) {
                            GenericRecord record = null;
                            while (reader.hasNext()) {
                                record = reader.next(record);

                                final byte[] results = processResult(context, original, record);
                                out.write(results);
                                out.write('\n');
                            }

                        }
                    } else { // no input and record
                        final byte[] results = processResult(context, null, null);
                        out.write(results);
                        out.write('\n');
                    }

                }
            });

            final String serverIP = RpcContext.getContext().getRemoteHost();
            final String application = RpcContext.getContext().getUrl().getParameter("application");

            final Map<String, String> attributes = new HashMap<>();
            attributes.put(ATTR_SERVER, serverIP);
            attributes.put(ATTR_APP_NAME, application);
            attributes.put(CoreAttributes.MIME_TYPE.key(), APPLICATION_JSON);
            response = session.putAllAttributes(response, attributes);

            session.transfer(response, REL_SUCCESS);

            session.getProvenanceReporter().modifyContent(response, stopWatch.getElapsed(TimeUnit.MILLISECONDS));
        } catch (final Exception e) {
            getLogger().error("Routing to {} due to exception: {}", new Object[] { REL_FAILURE.getName(), e }, e);
            if (original != null) {
                session.transfer(original, REL_FAILURE);
            }
        }
    }

    /**
     * currently, only support the flat record simply, don't support the generic classes yet. @see http://dubbo.incubator.apache.org/books/dubbo-user-book/demos/generic-reference.html
     */
    protected byte[] processResult(final ProcessContext context, final FlowFile flowFile, final GenericRecord record) throws IOException {

        Object result = null;
        try {
            if (choice == PARAM_CHOICE.CUSTOM) { // custom parameters and values
                result = genericService.$invoke(methodName, customParametersTypes, customArgsValues);

            } else if (choice == PARAM_CHOICE.FLOW) { // parameters and values from flow
                final Tuple<String[], Object[]> parameters = FlowParam.retrieve(record);
                result = genericService.$invoke(methodName, parameters.getKey(), parameters.getValue());

            } // else{ //shouldn't here
        } catch (com.alibaba.dubbo.rpc.RpcException e) {
            String realMessage = e.getMessage();
            final Throwable cause = e.getCause();
            if (cause instanceof com.alibaba.dubbo.remoting.RemotingException) {
                realMessage = ((com.alibaba.dubbo.remoting.RemotingException) cause).getMessage();
                getLogger().error("Routing to {} due to exception: {}", new Object[] { REL_FAILURE.getName(), cause }, cause);
            }
            throw new IOException(realMessage, cause);
        } catch (com.alibaba.dubbo.rpc.service.GenericException e) {
            getLogger().error("Routing to {} due to exception: {}", new Object[] { REL_FAILURE.getName(), e }, e);
            throw new IOException("Seems the parameters and values are not matched", e);
        } catch (Throwable e) {
            // if the parameters are not matched will throw NoSuchMethodException?
            getLogger().error("Routing to {} due to exception: {}", new Object[] { REL_FAILURE.getName(), e }, e);
            throw new IOException("Seem invoke the service failure, make sure the settings of service have been set rightly", e);
        }

        if (result == null) {// even null, still have empty value
            result = "";
        } // else { //if POJO, the Dubbo will convert to Map (with "class" field additionally)

        Map<String, Object> response = new HashMap<>(1);
        response.put(FIELD_RESPONSE, result);

        return new ObjectMapper().writeValueAsBytes(response);
    }
}
