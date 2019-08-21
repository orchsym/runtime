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

import java.util.Date;
import java.util.Map;
import java.util.Calendar;
import java.util.TimeZone;

import org.apache.nifi.attribute.expression.language.evaluation.DateEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.Evaluator;
import org.apache.nifi.attribute.expression.language.evaluation.QueryResult;
import org.apache.nifi.attribute.expression.language.evaluation.NumberEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.NumberQueryResult;

public abstract class DateGettingEvaluator extends NumberEvaluator {

    private final DateEvaluator subject;
    private final Evaluator<String> timeZone;

    public DateGettingEvaluator(final DateEvaluator subject, final Evaluator<String> timeZone) {
        this.subject = subject;
        this.timeZone = timeZone;
    }

    @Override
    public QueryResult<Number> evaluate(final Map<String, String> attributes) {
        final long subjectValue = subject.evaluate(attributes).getValue().getTime();
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(subjectValue));
        if (timeZone != null) {
            final QueryResult<String> tzResult = timeZone.evaluate(attributes);
            final String tz = tzResult.getValue();
            if (tz != null && TimeZone.getTimeZone(tz) != null) {
                cal.setTimeZone(TimeZone.getTimeZone(tz));
            }
        }
        final Number result = cal.get(getGettingField());

        return new NumberQueryResult(result);
    }

    protected abstract int getGettingField();

    @Override
    public Evaluator<?> getSubjectEvaluator() {
        return subject;
    }

}
