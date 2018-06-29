package com.baishancloud.orchsym.processors.dubbo.param;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.components.PropertyValue;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;

import com.baishancloud.orchsym.processors.dubbo.Constants;
import com.baishancloud.orchsym.processors.dubbo.convertor.SimpleConvertor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author GU Guoqiang
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomParam {
    public static final String DEFAULT_CLASS = String.class.getName(); // String by default

    @JsonProperty(Constants.FIELD_CLASS)
    private String className = DEFAULT_CLASS;

    @JsonProperty(Constants.FIELD_VALUE)
    private Object value;

    private String desc;

    @JsonIgnoreProperties
    @JsonIgnore
    private volatile Object convertedValue;

    public CustomParam() {
        super();
    }

    public CustomParam(String className, Object value) {
        this();
        this.className = className;
        this.value = value;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return "CustomParam [javaType=" + className + ", value=" + value + "]";
    }

    /**
     * need fix the value to right types.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Object getValue() {
        Object result = convertedValue;
        if (result == null) {
            // synchronized (DEFAULT_CLASS) {
            // if(result == null) {
            Object rawValue = value;
            if (rawValue instanceof List) { // the array in json, will be list
                if (SimpleConvertor.NAMES_PRIMITIVE_ARRAY.contains(getClassName())) { // primitive array, like int[]
                    result = SimpleConvertor.convertSimpleArray(getClassName(), ((List) rawValue).toArray());

                } else if (SimpleConvertor.isArrayClass(getClassName())) { // normal array, like Integer[]
                    result = SimpleConvertor.convertObjectArray(getClassName(), ((List) rawValue).toArray());

                } else if (SimpleConvertor.isList(getClassName(), rawValue)) { // List , like List<String>
                    result = SimpleConvertor.convertObjectList(((List) rawValue));

                } else if (SimpleConvertor.isMap(getClassName(), rawValue)) { // Map for primitive objects , like Map<Short, Float>
                    result = SimpleConvertor.convertObjectMap(((List) rawValue));
                }
            } else if (rawValue instanceof Map) { // Map for Map<String,Object>, means the key must String.
                result = SimpleConvertor.convertObjectMap((Map<String, Object>) rawValue);
            } else {
                result = SimpleConvertor.convertSingle(getClassName(), rawValue);
            }

            // not set still
            if (result == null) {
                result = rawValue; // original
            }

            convertedValue = result;
            // }
            // }
        }

        return convertedValue;

    }

    /**
     * need ignore, else will be serialize via Jackson.
     */
    @JsonIgnore
    public Object getRawValue() {
        return value;
    }

    @JsonIgnore
    public boolean isValuePresent(final ProcessContext context) {
        return isExpPresent(context, getValue());
    }

    /**
     * Check the Value has expression or not, even the children values.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @JsonIgnore
    protected boolean isExpPresent(final ProcessContext context, final Object fieldValue) {
        if (fieldValue == null || context == null) {
            return false;
        }
        if (fieldValue instanceof List) {
            for (Object o : (List) fieldValue) {
                if (isExpPresent(context, o)) {
                    return true;
                }
            }
        } else if (fieldValue instanceof Map) {
            for (Entry<String, Object> entry : ((Map<String, Object>) fieldValue).entrySet()) {
                if (Constants.FIELD_CLASS.equals(entry.getKey())) { // no need check the class field
                    continue;
                }
                if (isExpPresent(context, entry.getValue())) {
                    return true;
                }
            }
        } else {
            final PropertyValue paramPropertyValue = context.newPropertyValue(fieldValue.toString());
            return paramPropertyValue.isExpressionLanguagePresent();
        }
        return false;
    }

    @JsonIgnore
    public Object evalValue(final ProcessContext context, final FlowFile flowFile) {
        return evalValue(context, flowFile, getValue());
    }

    /**
     * eval the values with children
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @JsonIgnore
    protected Object evalValue(final ProcessContext context, final FlowFile flowFile, Object fieldValue) {
        if (fieldValue == null || context == null) {
            return null;
        }
        if (fieldValue instanceof List) {
            final List list = (List) fieldValue;
            for (int i = 0; i < list.size(); i++) {
                if (isExpPresent(context, list.get(i))) {
                    // set new value
                    list.set(i, evalValue(context, flowFile, list.get(i)));
                }
            }
        } else if (fieldValue instanceof Map) {
            final Map<String, Object> map = (Map<String, Object>) fieldValue;
            for (Entry<String, Object> entry : map.entrySet()) {
                if (Constants.FIELD_CLASS.equals(entry.getKey())) { // no need check the class field
                    continue;
                }
                if (isExpPresent(context, entry.getValue())) {
                    // set new value
                    map.put(entry.getKey(), evalValue(context, flowFile, entry.getValue()));
                }
            }
        } else if (isExpPresent(context, fieldValue)) { // normal field with expression
            final PropertyValue paramPropertyValue = context.newPropertyValue(fieldValue.toString());
            String newValue = null;
            if (flowFile != null) {
                newValue = paramPropertyValue.evaluateAttributeExpressions(flowFile).getValue();
            } else {
                newValue = paramPropertyValue.evaluateAttributeExpressions().getValue();
            }
            return newValue;
        }
        return fieldValue;

    }

    /**
     * Unify to return the list of CustomParam, if the value of CustomParam is Object, will convert to LinkedHashMap (POJO also) or ArrayList(array also).
     * 
     * @author GU Guoqiang
     *
     */
    public static class Parser {

        @SuppressWarnings("unchecked")
        public List<CustomParam> parse(String value) throws IOException {
            if (StringUtils.isBlank(value)) {
                return Collections.emptyList();
            }
            final ObjectMapper objectMapper = new ObjectMapper();
            JsonNode json = objectMapper.readTree(value);
            if (json instanceof ArrayNode) { // if have several parameters
                final JavaType type = objectMapper.getTypeFactory().constructParametricType(ArrayList.class, CustomParam.class);
                return (List<CustomParam>) objectMapper.readValue(value, type);
            } else if (json instanceof ObjectNode) { // when only one parameter
                if (json.has(Constants.FIELD_CLASS) && json.has(Constants.FIELD_VALUE)) {
                    final CustomParam readValue = objectMapper.readValue(value, CustomParam.class);
                    return Collections.unmodifiableList(Arrays.asList(readValue));
                } else { // default types with value, only support the int, double, boolean, string
                    final JavaType type = objectMapper.getTypeFactory().constructType(LinkedHashMap.class);
                    final Map<String, Object> map = objectMapper.readValue(value, type);
                    List<CustomParam> params = map.values().stream()//
                            .filter(obj -> obj.getClass().equals(String.class) || SimpleConvertor.NAMES_PRIMITIVE_OBJ.contains(obj.getClass().getName()))//
                            .map(o -> {
                                final Class<?> primitive = SimpleConvertor.MAP_PRIMITIVE_OBJ.get(o.getClass());
                                if (primitive != null) {
                                    return new CustomParam(primitive.getName(), o);
                                } else { // should be String
                                    return new CustomParam(String.class.getName(), o);
                                }
                            }).collect(Collectors.toList());

                    if (params.size() == map.size()) { // all are primitive type
                        return params;
                    }
                }
            }
            return Collections.emptyList();
        }
    }

    /**
     * Currently, can't add the class field in final map. only simply to convert the pojo to map.
     * 
     * @author GU Guoqiang
     * 
     */
    public static class Writer {

        public String write(List<CustomParam> params) throws IOException {
            final ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(params);
        }

        public String write(CustomParam param) throws IOException {
            final ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(param);
        }
    }
}
