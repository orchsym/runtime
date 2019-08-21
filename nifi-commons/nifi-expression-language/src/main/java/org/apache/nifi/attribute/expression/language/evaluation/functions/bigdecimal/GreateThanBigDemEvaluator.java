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
import java.util.Map;

import org.apache.nifi.attribute.expression.language.evaluation.BigDecimalEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.BigDecimalQueryResult;
import org.apache.nifi.attribute.expression.language.evaluation.BooleanEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.BooleanQueryResult;
import org.apache.nifi.attribute.expression.language.evaluation.Evaluator;
import org.apache.nifi.attribute.expression.language.evaluation.QueryResult;

/**
 * 
 * @author LiGuo BigDecimal 大于
 */
public class GreateThanBigDemEvaluator extends BooleanEvaluator {

    private final Evaluator<BigDecimal> subject;
    private final Evaluator<BigDecimal> compar;

    public GreateThanBigDemEvaluator(final Evaluator<BigDecimal> subject, final Evaluator<BigDecimal> compar) {
        this.subject = subject;
        this.compar = compar;
    }

    @Override
    public QueryResult<Boolean> evaluate(final Map<String, String> attributes) {
        final BigDecimal subjectValue = subject.evaluate(attributes).getValue();
        if (subjectValue == null) {
            return new BooleanQueryResult(null);
        }

        final BigDecimal val = compar.evaluate(attributes).getValue();
        if (val == null) {
            return new BooleanQueryResult(null);
        }

        Integer result = subjectValue.compareTo(val);

        if (result > 0) {
            return new BooleanQueryResult(true);
        } else {
            return new BooleanQueryResult(false);
        }

    }

    @Override
    public Evaluator<?> getSubjectEvaluator() {
        return subject;
    }

}
