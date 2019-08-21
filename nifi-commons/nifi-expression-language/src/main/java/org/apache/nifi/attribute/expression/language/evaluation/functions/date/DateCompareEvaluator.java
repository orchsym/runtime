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
package org.apache.nifi.attribute.expression.language.evaluation.functions.date;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.nifi.attribute.expression.language.evaluation.BooleanEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.BooleanQueryResult;
import org.apache.nifi.attribute.expression.language.evaluation.DateEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.Evaluator;
import org.apache.nifi.attribute.expression.language.evaluation.QueryResult;

public abstract class DateCompareEvaluator extends BooleanEvaluator {
    private final DateEvaluator subject;
    private final Evaluator<Date> another;

    public DateCompareEvaluator(DateEvaluator subject, final Evaluator<Date> another) {
        super();
        this.subject = subject;
        this.another = another;
    }

    @Override
    public QueryResult<Boolean> evaluate(Map<String, String> attributes) {
        final Date subjectValue = subject.evaluate(attributes).getValue();
        if (subjectValue == null) {
            return new BooleanQueryResult(false);
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(subjectValue);

        Date date2 = another.evaluate(attributes).getValue();
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);

        return new BooleanQueryResult(apply(cal, cal2));
    }

    protected abstract boolean apply(Calendar current, Calendar another);

    @Override
    public Evaluator<?> getSubjectEvaluator() {
        return subject;
    }

}
