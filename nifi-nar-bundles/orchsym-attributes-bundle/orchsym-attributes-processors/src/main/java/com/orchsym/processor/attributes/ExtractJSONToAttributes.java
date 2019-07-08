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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.DynamicProperty;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnRemoved;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.io.InputStreamCallback;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;

/**
 * @author GU Guoqiang
 *
 */
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@Marks(categories = { "Convert & Control/Convert" }, createdDate = "2018-12-12")
@Tags({ "Extract", "Attribute", "Record", "JSON" })
@CapabilityDescription("Provide the abblity of extracting the attributes by JSON Path for JSON format contents from the incoming flowfile. If don't set the dynamic property to set with JSON path expression, will use the name 'ALL' with JSON Path expression '$' for all by default")
@DynamicProperty(name = "JSON Path property", //
        value = "The name of dynamic property with JSON Path expression", //
        expressionLanguageScope = ExpressionLanguageScope.FLOWFILE_ATTRIBUTES, //
        description = "set the dynamic property with JSON Path expression")
@WritesAttributes({ //
        @WritesAttribute(attribute = AbstractExtractToAttributesProcessor.ATTR_REASON, description = "The error message of extracting failure")//
})
public class ExtractJSONToAttributes extends AbstractExtractToAttributesProcessor {
    static final Configuration STRICT_PROVIDER_CONFIGURATION = Configuration.builder().jsonProvider(new JacksonJsonProvider()).build();
    static final JsonProvider JSON_PROVIDER = STRICT_PROVIDER_CONFIGURATION.jsonProvider();
    static final Pattern PATH_ATTR = Pattern.compile("\\['(.*?)'\\]$");

    private final ConcurrentMap<String, JsonPath> cachedJsonPathMap = new ConcurrentHashMap<>();

    static abstract class JsonPathValidator implements Validator {

        @Override
        public ValidationResult validate(final String subject, final String input, final ValidationContext context) {
            String error = null;
            if (isStale(subject, input)) {
                try {
                    JsonPath compiledJsonPath = JsonPath.compile(input);
                    cacheComputedValue(subject, input, compiledJsonPath);
                } catch (Exception e) {
                    error = "specified expression was not valid: " + input + " \n" + e.getMessage();
                }
            }
            return new ValidationResult.Builder().subject(subject).valid(error == null).explanation(error).build();
        }

        abstract void cacheComputedValue(String subject, String input, JsonPath computedJsonPath);

        abstract boolean isStale(String subject, String input);
    }

    @Override
    protected Validator getPathValidator() {
        return new JsonPathValidator() {
            @Override
            public void cacheComputedValue(String subject, String input, JsonPath computedJsonPath) {
                cachedJsonPathMap.put(input, computedJsonPath);
            }

            @Override
            public boolean isStale(String subject, String input) {
                return cachedJsonPathMap.get(input) == null;
            }
        };
    }

    @Override
    protected String getDefaultAttributesPath() {
        return "$";
    }

    @Override
    public void onPropertyModified(PropertyDescriptor descriptor, String oldValue, String newValue) {
        super.onPropertyModified(descriptor, oldValue, newValue);
        if (descriptor.isDynamic()) {
            if (!StringUtils.equals(oldValue, newValue)) {
                if (oldValue != null) {
                    cachedJsonPathMap.remove(oldValue);
                }
            }
        }
    }

    @OnRemoved
    public void onRemoved(ProcessContext processContext) {
        for (PropertyDescriptor propertyDescriptor : getPropertyDescriptors()) {
            if (propertyDescriptor.isDynamic()) {
                cachedJsonPathMap.remove(processContext.getProperty(propertyDescriptor).getValue());
            }
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void retrieveAttributes(ProcessContext context, ProcessSession session, FlowFile flowFile, final Map<String, String> attributesFromRecords, final Map<String, String> attrPathSettings,
            final List<Pattern> includeFields, final List<Pattern> excludeFields) {
        DocumentContext documentContext;
        try {
            final AtomicReference<DocumentContext> contextHolder = new AtomicReference<>(null);
            session.read(flowFile, new InputStreamCallback() {
                @Override
                public void process(InputStream in) throws IOException {
                    try (BufferedInputStream bufferedInputStream = new BufferedInputStream(in)) {
                        DocumentContext ctx = JsonPath.using(STRICT_PROVIDER_CONFIGURATION).parse(bufferedInputStream);
                        contextHolder.set(ctx);
                    }
                }
            });

            documentContext = contextHolder.get();
        } catch (InvalidJsonException e) {
            getLogger().error("FlowFile {} did not have valid JSON content.", new Object[] { flowFile });
            throw e;
        }

        Map<String, JsonPath> jsonPaths;
        try {
            jsonPaths = attrPathSettings.keySet().stream().collect(Collectors.toMap(Function.identity(), n -> JsonPath.compile(attrPathSettings.get(n))));
        } catch (Exception e) {
            getLogger().error("Invald JSON Path settings");
            throw e;
        }
        final Object fullData = documentContext.json();
        if (fullData instanceof List) { // array
            List jsonArr = (List) fullData;
            if (jsonArr.size() == 1) {
                processAttributes(attributesFromRecords, getOne(jsonArr.get(0)), jsonPaths, -1, includeFields, excludeFields);
            } else if (jsonArr.size() > 1 && allowArray) {
                for (int i = 0; i < jsonArr.size(); i++) {
                    processAttributes(attributesFromRecords, getOne(jsonArr.get(i)), jsonPaths, i, includeFields, excludeFields);
                }
            }
        } else if (fullData instanceof Map) {
            processAttributes(attributesFromRecords, documentContext, jsonPaths, -1, includeFields, excludeFields);
        }
    }

    protected DocumentContext getOne(Object object) {
        return JsonPath.using(STRICT_PROVIDER_CONFIGURATION).parse(object);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void processAttributes(Map<String, String> attributes, DocumentContext jsonContext, Map<String, JsonPath> jsonPaths, int index, final List<Pattern> includeFields,
            final List<Pattern> excludeFields) {
        if (jsonPaths == null || jsonPaths.isEmpty()) {
            return;
        }
        for (Entry<String, JsonPath> entry : jsonPaths.entrySet()) {
            final String jsonPathAttrKey = entry.getKey();
            final JsonPath jsonPath = entry.getValue();

            final Map<String, List<Object>> results = new LinkedHashMap<>();
            final Map<String, List<Object>> scalarResults = new LinkedHashMap<>();
            final Map<String, List<Object>> scalarArrResults = new LinkedHashMap<>();
            boolean filtered = false;

            try {
                final Object value = jsonContext.read(jsonPath);

                String name = null;
                final String path = jsonPath.getPath();
                // $..id[0] ==> $..['ids'][0]
                // $.addr.additions.ids ==> $['addr']['additions']['ids']
                final int begin = path.lastIndexOf("['");
                final int end = path.lastIndexOf("']");
                if (begin > 0 && end > 0) {
                    name = path.substring(begin + 2, end);
                }

                // if value is simple or if the array is simple will filter the parent name
                if (name != null && (isScalar(value) || isScalarList(value)) && ignoreField(name, includeFields, excludeFields)) {
                    filtered = true;
                    continue;
                }

                String fieldName = jsonPathAttrKey;
                if (StringUtils.isNotBlank(name)) {
                    fieldName = name;
                }
                List<Object> valueList = new ArrayList<>();
                if (value instanceof Object[]) {
                    valueList = Arrays.asList((Object[]) value);
                } else if (value instanceof List) {
                    valueList = (List) value;
                } else {
                    valueList = Arrays.asList(value);
                }

                if (isScalar(value) || isScalarList(valueList) && valueList.size() == 1) {
                    scalarResults.put(fieldName, valueList);
                } else if (isScalarList(value)) {
                    scalarArrResults.put(fieldName, valueList);
                } else {
                    results.put(fieldName, valueList);
                }

            } catch (PathNotFoundException e) {
                // ignore
            }

            setResults(attributes, jsonPathAttrKey, index, includeFields, excludeFields, results, scalarResults, scalarArrResults, filtered);
        }
    }

}
