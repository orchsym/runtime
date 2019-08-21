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
package org.apache.nifi.attribute.expression.language.evaluation.functions.bigdecimal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.apache.nifi.attribute.expression.language.evaluation.BigDecimalEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.BigDecimalQueryResult;
import org.apache.nifi.attribute.expression.language.evaluation.Evaluator;
import org.apache.nifi.attribute.expression.language.evaluation.QueryResult;

/**
 * 
 * @author LiGuo BigDecimal 除法
 */
public class DivideBigDemEvaluator extends BigDecimalEvaluator {

    private final Evaluator<BigDecimal> subject;
    private final Evaluator<BigDecimal> divideValue;
    private final Evaluator<Number> scale;
    private final Evaluator<String> round;

    public DivideBigDemEvaluator(final Evaluator<BigDecimal> subject, final Evaluator<BigDecimal> divideValue) {
        this(subject, divideValue, null, null);
    }

    public DivideBigDemEvaluator(final Evaluator<BigDecimal> subject, final Evaluator<BigDecimal> divideValue, final Evaluator<Number> scale) {
        this(subject, divideValue, scale, null);
    }

    public DivideBigDemEvaluator(final Evaluator<BigDecimal> subject, final Evaluator<BigDecimal> divideValue, final Evaluator<Number> scale, Evaluator<String> round) {
        this.subject = subject;
        this.divideValue = divideValue;
        this.scale = scale;
        this.round = round;
    }

    @Override
    public QueryResult<BigDecimal> evaluate(final Map<String, String> attributes) {
        final BigDecimal subjectValue = subject.evaluate(attributes).getValue();
        if (subjectValue == null) {
            return new BigDecimalQueryResult(null);
        }

        final BigDecimal divide = divideValue.evaluate(attributes).getValue();
        if (divide == null) {
            return new BigDecimalQueryResult(null);
        }

        RoundingMode roundMode = RoundingMode.HALF_UP;
        if (null != round) {
            final String roundStr = round.evaluate(attributes).getValue();
            for (RoundingMode rm : RoundingMode.values()) {
                if (rm.name().equalsIgnoreCase(roundStr)) {
                    roundMode = rm;
                }
            }
        }

        Integer scalevalue = null;
        if (null != scale) {
            scalevalue = scale.evaluate(attributes).getValue().intValue();
        }

        BigDecimal result = null;
        if (null != roundMode) {
            if (null != scalevalue) {
                result = subjectValue.divide(divide, scalevalue, roundMode);
            } else {
                result = subjectValue.divide(divide, roundMode);
            }
        } else {
            if (null != scalevalue) {
                result = subjectValue.divide(divide, scalevalue);
            } else {
                result = subjectValue.divide(divide);
            }
        }

        return new BigDecimalQueryResult(result);
    }

    @Override
    public Evaluator<?> getSubjectEvaluator() {
        return subject;
    }

}
