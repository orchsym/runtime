package org.apache.nifi.lookup;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.expression.ExpressionLanguageScope;

public class CommonKeyValueLookupService extends AbstractControllerService implements KeyValueLookupService {

    private static final String KEY = "key";
    private static final Set<String> REQUIRED_KEYS = Stream.of(KEY).collect(Collectors.toSet());
    private volatile Map<String, Object> lookupValues = new HashMap<>();

    @Override
    protected PropertyDescriptor getSupportedDynamicPropertyDescriptor(final String propertyDescriptorName) {
        return new PropertyDescriptor.Builder().name(propertyDescriptorName).required(false).dynamic(true).addValidator(Validator.VALID)
                .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY).build();
    }

    // @OnEnabled
    // public void cacheConfiguredValues(final ConfigurationContext context) {
    // //adds the properties by default
    // lookupValues = context.getProperties().entrySet().stream()
    // .collect(Collectors.toMap(entry -> entry.getKey().getName(), entry -> context.getProperty(entry.getKey()).evaluateAttributeExpressions().getValue()));
    // }

    @Override
    public Optional<Object> lookup(final Map<String, Object> coordinates) {
        if (coordinates == null) {
            return Optional.empty();
        }

        final String key = coordinates.get(KEY).toString();
        if (key == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(lookupValues.get(key));
    }

    @Override
    public Set<String> getRequiredKeys() {
        return REQUIRED_KEYS;
    }

    public boolean register(final String name, final Object value) {
        if (StringUtils.isEmpty(name)) {
            return false;
        }
        lookupValues.put(name, value);
        return true;
    }

    public Optional<Object> get(final String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(lookupValues.get(name));
    }

}
