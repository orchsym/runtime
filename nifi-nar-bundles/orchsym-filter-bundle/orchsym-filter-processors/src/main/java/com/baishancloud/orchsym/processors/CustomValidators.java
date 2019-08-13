package com.baishancloud.orchsym.processors;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;

import java.io.IOException;

public class CustomValidators {

        public static final Validator JSON_SCHEMA_VALIDATOR = new Validator() {
        @Override
        public ValidationResult validate(final String subject, final String input, final ValidationContext context) {
            if (context.isExpressionLanguagePresent(input)) {
                return new ValidationResult.Builder()
                        .input(input)
                        .subject(subject)
                        .valid(true)
                        .explanation("Expression Language Present")
                        .build();
            }
            //获取要验证的值
            final String json = context.newPropertyValue(input).evaluateAttributeExpressions().getValue();
            try {
                if (validatorSchema(Constant.JSON_SCHEMA, json)) {
                    return new ValidationResult.Builder()
                            .subject(subject)
                            .input(input)
                            .valid(true)
                            .build();
                } else {
                    return new ValidationResult.Builder()
                            .subject(subject)
                            .input(input)
                            .valid(false)
                            .build();
                }
            } catch (final Exception e) {
                return new ValidationResult.Builder()
                        .subject(subject)
                        .input(input)
                        .valid(false)
                        .explanation("Not a valid JSON Statement: " + e.getMessage())
                        .build();
            }
        }
    };
    //验证JSON
    private final static JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
    private static boolean validatorSchema(String jsonSchema, String json) throws IOException, ProcessingException {
        JsonNode mainNode = JsonLoader.fromString(jsonSchema);
        JsonNode instanceNode = JsonLoader.fromString(json);
        JsonSchema schema = factory.getJsonSchema(mainNode);
        ProcessingReport processingReport = schema.validate(instanceNode);
        return processingReport.isSuccess();
    }
}
