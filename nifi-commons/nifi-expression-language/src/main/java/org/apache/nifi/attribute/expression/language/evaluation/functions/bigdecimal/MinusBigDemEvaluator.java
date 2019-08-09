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
import java.util.Map;

import org.apache.nifi.attribute.expression.language.evaluation.BigDecimalEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.BigDecimalQueryResult;
import org.apache.nifi.attribute.expression.language.evaluation.Evaluator;
import org.apache.nifi.attribute.expression.language.evaluation.QueryResult;
/**
 * 
 * @author LiGuo
 * BigDecimal 减法
 */
public class MinusBigDemEvaluator extends BigDecimalEvaluator {

    private final Evaluator<BigDecimal> subject;
    private final Evaluator<BigDecimal> plusValue;

    public MinusBigDemEvaluator(final Evaluator<BigDecimal> subject, final Evaluator<BigDecimal> plusValue) {
        this.subject = subject;
        this.plusValue = plusValue;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public QueryResult<BigDecimal> evaluate(final Map<String, String> attributes) {
        final BigDecimal subjectValue = subject.evaluate(attributes).getValue();
        if (subjectValue == null) {
            return new BigDecimalQueryResult(null);
        }

        final BigDecimal minus = plusValue.evaluate(attributes).getValue();
        if (minus == null) {
            return new BigDecimalQueryResult(null);
        }

        BigDecimal result  = (BigDecimal) subjectValue.subtract(minus);
        
        return new BigDecimalQueryResult(result);
    }

    @Override
    public Evaluator<?> getSubjectEvaluator() {
        return subject;
    }

}
