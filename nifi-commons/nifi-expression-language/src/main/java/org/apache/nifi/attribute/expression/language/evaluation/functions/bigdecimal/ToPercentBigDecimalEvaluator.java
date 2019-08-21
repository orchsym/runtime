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

import org.apache.nifi.attribute.expression.language.evaluation.Evaluator;
import org.apache.nifi.attribute.expression.language.evaluation.QueryResult;
import org.apache.nifi.attribute.expression.language.evaluation.StringEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.StringQueryResult;

/**
 * 
 * @author LiGuo BigDecimal 百分比
 */
public class ToPercentBigDecimalEvaluator extends StringEvaluator {

    private Evaluator<?> subject;

    public ToPercentBigDecimalEvaluator(Evaluator<?> subject) {
        this.subject = subject;
    }

    public QueryResult<String> evaluate(Map<String, String> attributes) {
        final QueryResult<?> result = subject.evaluate(attributes);
        final Object value = result.getValue();
        if (value == null) {
            return new StringQueryResult(null);
        }
        BigDecimal subject = new BigDecimal(value.toString());
        BigDecimal hundr = new BigDecimal(100);

        BigDecimal percent = subject.multiply(hundr);
        percent = percent.setScale(subject.scale() - 2, RoundingMode.DOWN); // 00 in end, cut it
        return new StringQueryResult(percent.toString() + "%");
    }

    @Override
    public Evaluator<?> getSubjectEvaluator() {
        return subject;
    }

}
