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
package org.apache.nifi.attribute.expression.language.evaluation.cast;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.nifi.attribute.expression.language.evaluation.BigDecimalEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.BigDecimalQueryResult;
import org.apache.nifi.attribute.expression.language.evaluation.DecimalQueryResult;
import org.apache.nifi.attribute.expression.language.evaluation.Evaluator;
import org.apache.nifi.attribute.expression.language.evaluation.NumberQueryResult;
import org.apache.nifi.attribute.expression.language.evaluation.QueryResult;
import org.apache.nifi.attribute.expression.language.evaluation.StringQueryResult;
import org.apache.nifi.attribute.expression.language.evaluation.WholeNumberQueryResult;
import org.apache.nifi.attribute.expression.language.exception.AttributeExpressionLanguageParsingException;
import org.apache.nifi.expression.AttributeExpression.ResultType;

public class BigDecimalCastEvaluator extends BigDecimalEvaluator {

    private final Evaluator<?> subjectEvaluator;

    public BigDecimalCastEvaluator(final Evaluator<?> subjectEvaluator) {
        if (subjectEvaluator.getResultType() == ResultType.BOOLEAN) {
            throw new AttributeExpressionLanguageParsingException("Cannot implicitly convert Data Type " + subjectEvaluator.getResultType() + " to " + ResultType.BIGDECIMAL);
        }
        this.subjectEvaluator = subjectEvaluator;
    }

    @Override
    public QueryResult<BigDecimal> evaluate(final Map<String, String> attributes) {
        final QueryResult<?> result = subjectEvaluator.evaluate(attributes);
        if (result.getValue() == null) {
            return new BigDecimalQueryResult(null);
        }

        switch (result.getResultType()) {
        case BIGDECIMAL:
            return (BigDecimalQueryResult) result;
        case STRING:
            final String trimmed = ((StringQueryResult) result).getValue().trim();
            return new BigDecimalQueryResult(new BigDecimal(trimmed));
        case WHOLE_NUMBER:
            final Long resultValue = ((WholeNumberQueryResult) result).getValue();
            return new BigDecimalQueryResult(BigDecimal.valueOf(resultValue));
        case NUMBER:
            final Number numberValue = ((NumberQueryResult) result).getValue();
            return new BigDecimalQueryResult(BigDecimal.valueOf(numberValue.doubleValue()));
        case DECIMAL:
            final Double doubleValue = ((DecimalQueryResult) result).getValue();
            return new BigDecimalQueryResult(BigDecimal.valueOf(doubleValue));
        default:
            return new BigDecimalQueryResult(null);
        }
    }

    @Override
    public Evaluator<?> getSubjectEvaluator() {
        return subjectEvaluator;
    }

}
