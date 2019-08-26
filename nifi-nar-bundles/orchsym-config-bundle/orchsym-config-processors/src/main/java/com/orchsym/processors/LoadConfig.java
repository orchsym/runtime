/*
 * Licensed to the Orchsym Runtime under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * this file to You under the Orchsym License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://github.com/orchsym/runtime/blob/master/orchsym/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orchsym.processors;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.IOUtils;
import org.apache.nifi.annotation.behavior.*;
import org.apache.nifi.components.*;
import org.apache.nifi.distributed.cache.client.DistributedMapCacheClient;
import org.apache.nifi.distributed.cache.client.Serializer;
import org.apache.nifi.distributed.cache.client.exception.SerializationException;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author liuxun
 * @apiNote 将配置数据抽取到全局配置中
 */

@InputRequirement(InputRequirement.Requirement.INPUT_ALLOWED)
@Tags({"config", "ini", "property"})
@CapabilityDescription("Extract text or flowfiles or attributes in the format " +
        "of ini, properties, yml to the global configuration")
public class LoadConfig extends AbstractProcessor {
    public static final AllowableValue FILE_VALUE = new AllowableValue("FILE_SOURCE", "FILE_SOURCE", "load data from file");
    public static final AllowableValue FLOW_FILE_VALUE = new AllowableValue("FLOW_FILE_SOURCE", "FLOW_FILE_SOURCE", "load data from flowfile");
    public static final AllowableValue ATTR_VALUE = new AllowableValue("ATTR_SOURCE", "ATTR_SOURCE", "load data from  attr fo flowfile");

    public static final AllowableValue INI_TYPE = new AllowableValue("INI_TYPE", "INI_TYPE", "the type of data is ini");
    public static final AllowableValue PROP_TYPE = new AllowableValue("PROP_TYPE", "PROP_TYPE", "the type of data is properties");
    public static final AllowableValue YML_TYPE = new AllowableValue("YML_TYPE", "YML_TYPE", "the type of data is yaml");
    public static final AllowableValue JSON_TYPE = new AllowableValue("JSON_TYPE", "JSON_TYPE", "the type of data is json");

    public static final PropertyDescriptor PROP_DISTRIBUTED_CACHE_SERVICE = new PropertyDescriptor.Builder()
            .name("distribute-cache-service")
            .description("The Controller Service that is used to get or put the cached values.")
            .required(true)
            .identifiesControllerService(DistributedMapCacheClient.class)
            .build();

    public static final PropertyDescriptor SOURCE_PROPERTY = new PropertyDescriptor
            .Builder().name("config-source")
            .displayName("config-source")
            .description("Choose input source")
            .required(true)
            .allowableValues(FILE_VALUE, FLOW_FILE_VALUE, ATTR_VALUE)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor TYPE_PROPERTY = new PropertyDescriptor
            .Builder().name("data-type")
            .displayName("data-type")
            .description("Choose data type")
            .required(false)
            .allowableValues(INI_TYPE, PROP_TYPE, YML_TYPE, JSON_TYPE)
            .defaultValue(PROP_TYPE.getValue())
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor URL_PROPERTY = new PropertyDescriptor
            .Builder().name("file-url")
            .displayName("file-url")
            .description("the url of file, if you choosed FILE_SOURCE")
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .addValidator(StandardValidators.createURLorFileValidator())
            .build();

    public static final PropertyDescriptor INCLUDE_PROPERTY = new PropertyDescriptor
            .Builder().name("include-properties")
            .displayName("include-properties")
            .description("if not null, only extract config from include range")
            .required(false)
            .addValidator(StandardValidators.createListValidator(true, false, StandardValidators.NON_BLANK_VALIDATOR))
            .build();

    public static final PropertyDescriptor EXCLUDE_PROPERTY = new PropertyDescriptor
            .Builder().name("exclude-properties")
            .displayName("exclude-properties")
            .description("if not null, exclude properties from extractd range")
            .required(false)
            .addValidator(StandardValidators.createListValidator(true, false, StandardValidators.NON_BLANK_VALIDATOR))
            .build();

    public static final PropertyDescriptor SUPPORT_REGEX = new PropertyDescriptor
            .Builder().name("support-regex")
            .displayName("support-regex")
            .description("indicate is or not support the includes and excludes contains regex expressions")
            .required(true)
            .allowableValues("true", "false")
            .defaultValue("false")
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("FlowFiles that are sent successfully to the destination are transferred to this relationship.")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("If unable to communicate with the cache or if the cache entry is evaluated to be blank, the FlowFile will be penalized and routed to this relationship")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    private final Serializer<String> strSerializer = new StringSerializer();


    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<>();
        descriptors.add(PROP_DISTRIBUTED_CACHE_SERVICE);
        descriptors.add(SOURCE_PROPERTY);
        descriptors.add(TYPE_PROPERTY);
        descriptors.add(URL_PROPERTY);
        descriptors.add(INCLUDE_PROPERTY);
        descriptors.add(EXCLUDE_PROPERTY);
        descriptors.add(SUPPORT_REGEX);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @Override
    public void onPropertyModified(PropertyDescriptor descriptor, String oldValue, String newValue) {
        super.onPropertyModified(descriptor, oldValue, newValue);
    }

    /**
     * 每次点击start的时候执行
     */
    @OnScheduled
    public void onScheduled(final ProcessContext context) {

        getLogger().debug("+++++schedule++++++");
    }

    @Override
    protected Collection<ValidationResult> customValidate(ValidationContext validationContext) {
        List<ValidationResult> results = new ArrayList<>(super.customValidate(validationContext));
        final String sourceValue = validationContext.getProperty(SOURCE_PROPERTY).getValue();
        final String fileUrlValue = validationContext.getProperty(URL_PROPERTY).getValue();
        final PropertyValue typeProp = validationContext.getProperty(TYPE_PROPERTY);
        if (sourceValue != null && sourceValue.equals(FILE_VALUE.getValue())) {
            if (fileUrlValue == null || StringUtils.isBlank(fileUrlValue)) {
                results.add(new ValidationResult.Builder()
                        .subject(URL_PROPERTY.getDisplayName())
                        .explanation(URL_PROPERTY.getDisplayName() + " cant be blank when choose file source ")
                        .valid(false)
                        .build());
            }
        }

        if (sourceValue != null && !sourceValue.equals(ATTR_VALUE.getValue())) {
            if (!typeProp.isSet()) {
                results.add(new ValidationResult.Builder()
                        .subject(TYPE_PROPERTY.getDisplayName())
                        .explanation(TYPE_PROPERTY.getDisplayName() + " cant be empty unless  ")
                        .valid(false)
                        .build());
            }
        }

        return results;
    }

    /**
     * 每次处理flowfile的时候都会触发
     */
    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session)
            throws ProcessException {
        final ComponentLog logger = getLogger();

        FlowFile flowFile = session.get();

        final String sourceValue = context.getProperty(SOURCE_PROPERTY).getValue();
        final String dataTypeValue = context.getProperty(TYPE_PROPERTY).getValue();
        String fileUrl = context.getProperty(URL_PROPERTY).evaluateAttributeExpressions(flowFile).getValue();
        final PropertyValue includeProp = context.getProperty(INCLUDE_PROPERTY);
        final PropertyValue excludeProp = context.getProperty(EXCLUDE_PROPERTY);
        final String[] includeProps = includeProp.isSet() ? includeProp.getValue().split(",") : new String[]{};
        final String[] excludeProps = excludeProp.isSet() ? excludeProp.getValue().split(",") : new String[]{};
        final String isSupportRegex = context.getProperty(SUPPORT_REGEX).getValue();

        Map<String, Object> destinationMap = null;
        DistributedMapCacheClient cacheClient = context.getProperty(PROP_DISTRIBUTED_CACHE_SERVICE).asControllerService(DistributedMapCacheClient.class);
        try {

            if (sourceValue.equals(FILE_VALUE.getValue())) {
                InputStream inputStream = null;
                if (!fileUrl.startsWith("http://") && !fileUrl.startsWith("file:///")){
                    fileUrl = "file:///" + fileUrl;
                }
                inputStream = new URL(fileUrl).openStream();
                destinationMap = getMapByStream(inputStream, dataTypeValue);
                getDestinationMap(isSupportRegex, includeProps, excludeProps, destinationMap);
            } else if (sourceValue.equals(FLOW_FILE_VALUE.getValue())) {
                destinationMap = getMapByStream(session.read(flowFile), dataTypeValue);
                getDestinationMap(isSupportRegex, includeProps, excludeProps, destinationMap);
            } else if (sourceValue.equals(ATTR_VALUE)) {
                if (flowFile == null) {
                    logger.error("when choose ATTR, the flowFile cant be null");
                    return;
                }
                getDestinationMap(isSupportRegex, includeProps, excludeProps, flowFile.getAttributes());
            }

            logger.debug("+++++resultMap=" + destinationMap);

            flowFile = flowFile == null ? session.create() : flowFile;

            // write map to distribute
            for (String key : destinationMap.keySet()) {
                Object value = destinationMap.get(key);
                if (value instanceof String) {
                    cacheClient.put(key, (String) value, strSerializer, strSerializer);
                } else {
                    cacheClient.put(key, value.toString(), strSerializer, strSerializer);
                }
            }
            // write map to attribute
            session.putAllAttributes(flowFile, convertMap(destinationMap));

            session.transfer(flowFile, REL_SUCCESS);

        } catch (IOException e) {
            session.transfer(flowFile == null ? session.get() : flowFile, REL_FAILURE);
            logger.error("Unable to load config to cache when processing {} due to {}", new Object[]{flowFile, e});
        }

    }

    private static Map getMapByStream(InputStream in, String dataType) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] bytes = null;
        IOUtils.copy(in, baos);
        bytes = baos.toByteArray();
        in.close();
        baos.close();

        if (bytes.length == 0) {
            // 若内容为空，直接返回
            return new HashMap();
        }

        if (dataType.equals(INI_TYPE.getValue()) || dataType.equals(PROP_TYPE.getValue())) {
            final Properties props = new Properties();
            props.load(new ByteArrayInputStream(bytes));
            return props;
        } else if (dataType.equals(YML_TYPE.getValue())) {
            Yaml yaml = new Yaml();
            Object source = yaml.load(new ByteArrayInputStream(bytes));
            Map targetMap = new HashMap<>();
            if (source != null && source instanceof Map) {
                Map<Object, Object> map = (Map<Object, Object>) source;
                YamlMapUtil.toMap(map, targetMap, "");
                return targetMap;
            } else {
                throw new IOException("the style of yaml have error of is not map");
            }
        } else if (dataType.equals(JSON_TYPE.getValue())) {
            Object result = JSON.parse(bytes);
            if (result instanceof Map) {
                Map targetMap = new HashMap();
                YamlMapUtil.toMap((Map) result, targetMap, "");
                return targetMap;
            } else {
                throw new IOException("the style of cant be list");
            }
        } else {
            throw new IOException("the type of file or flowfile mast be ini 、properties or yaml");
        }

    }

    private static void getDestinationMap(String isSupportRegex, String[] includes, String[] excludes, Map map) {
        if ("true".equals(isSupportRegex)) {
            getDestinationMapWithRegex(includes, excludes, map);
        } else {
            getDestinationMapNotRegex(includes, excludes, map);
        }
    }

    private static void getDestinationMapWithRegex(String[] includes, String[] excludes, Map map) {
        final HashSet<String> includesRegexSet = new HashSet<>(Arrays.asList(includes));
        final HashSet<String> excludesRegexSet = new HashSet<>(Arrays.asList(excludes));
        final HashSet<String> includesSet = new HashSet<>();
        final HashSet<String> excludesSet = new HashSet<>();
        if (includesRegexSet.isEmpty() && excludesRegexSet.isEmpty()) {
            return;
        }

        for (Object key : map.keySet()) {
            for (String includeRegex : includesRegexSet) {
                if (key.toString().matches(includeRegex)) {
                    includesSet.add(key.toString());
                }
            }
            for (String excludeRegex : excludesRegexSet) {
                if (key.toString().matches(excludeRegex)) {
                    excludesSet.add(key.toString());
                }
            }
        }

        // because the Priority of includes is max， so when find not key matches regex of includes need clear map
        if (!includesRegexSet.isEmpty() && includesSet.isEmpty()){
            map.clear();
            return;
        }


        String[] tempIncludes = new String[]{};
        String[] tempExcludes = new String[]{};
        getDestinationMapNotRegex(includesSet.toArray(tempIncludes), excludesSet.toArray(tempExcludes), map);

    }

    private static void getDestinationMapNotRegex(String[] includes, String[] excludes, Map map) {
        final HashSet<String> includesSet = new HashSet<>(Arrays.asList(includes));
        final HashSet<String> excludesSet = new HashSet<>(Arrays.asList(excludes));
        final HashSet<String> resultSet = new HashSet<>();

        if (!includesSet.isEmpty()) {
            resultSet.addAll(includesSet);
            if (!excludesSet.isEmpty()) {
                resultSet.removeAll(excludesSet);
            }
        } else {
            if (!excludesSet.isEmpty()) {
                resultSet.addAll(map.keySet());
                resultSet.removeAll(excludesSet);
            } else {
                return;
            }
        }

        final Set<Map.Entry<String, String>> dataSet = map.entrySet();
        final Iterator<Map.Entry<String, String>> iterator = dataSet.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            if (!resultSet.isEmpty() && !resultSet.contains(entry.getKey())) {
                iterator.remove();
            }
        }

    }

    /**
     * 将map转为所有value为String的新Map
     *
     * @return
     */
    private static Map<String, String> convertMap(Map<String, Object> map) {
        Map<String, String> resultMap = new HashMap<>();
        for (String key : map.keySet()) {
            resultMap.put(key, map.get(key).toString());
        }
        return resultMap;
    }

    public static class StringSerializer implements Serializer<String> {
        @Override
        public void serialize(final String value, final OutputStream out) throws SerializationException, IOException {
            out.write(value.getBytes(StandardCharsets.UTF_8));
        }
    }

}
