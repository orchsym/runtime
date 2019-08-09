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
 * @author LiGuo BigDecimal 设置精度
 */
public class SetScaleBigDemEvaluator extends BigDecimalEvaluator {

    private final Evaluator<BigDecimal> subject;
    private final Evaluator<Number> scale;
    private final Evaluator<String> round;

    public SetScaleBigDemEvaluator(final Evaluator<BigDecimal> subject, final Evaluator<Number> scale) {
        this(subject, scale, null);
    }

    public SetScaleBigDemEvaluator(final Evaluator<BigDecimal> subject, final Evaluator<Number> scale, final Evaluator<String> roundMode) {
        this.subject = subject;
        this.scale = scale;
        this.round = roundMode;
    }

    @Override
    public QueryResult<BigDecimal> evaluate(final Map<String, String> attributes) {
        final BigDecimal subjectValue = subject.evaluate(attributes).getValue();
        if (subjectValue == null) {
            return new BigDecimalQueryResult(null);
        }

        final Integer scalevalue = scale.evaluate(attributes).getValue().intValue();
        RoundingMode roundMode = null;
        if (null != round) {
            final String roundStr = round.evaluate(attributes).getValue();
            for (RoundingMode rm : RoundingMode.values()) {
                if (rm.name().equalsIgnoreCase(roundStr)) {
                    roundMode = rm;
                }
            }
        }
        BigDecimal result = null;
        if (null != roundMode) {
            result = (BigDecimal) subjectValue.setScale(scalevalue, roundMode);
        } else {
            result = (BigDecimal) subjectValue.setScale(scalevalue);
        }

        return new BigDecimalQueryResult(result);
    }

    @Override
    public Evaluator<?> getSubjectEvaluator() {
        return subject;
    }

}
