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

import org.apache.nifi.attribute.expression.language.evaluation.DateEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.DateQueryResult;
import org.apache.nifi.attribute.expression.language.evaluation.Evaluator;

public class GetDateForWeekEvaluator extends DateEvaluator {

    private final DateEvaluator subject;
    private final Evaluator<Number> addingEvaluator;
    private final Evaluator<Number> weekIndexEvaluator;

    public GetDateForWeekEvaluator(final DateEvaluator subject, final Evaluator<Number> adding, final Evaluator<Number> weekIndex) {
        this.subject = subject;
        this.addingEvaluator = adding;
        this.weekIndexEvaluator = weekIndex;
    }

    @Override
    public DateQueryResult evaluate(final Map<String, String> attributes) {
        final long subjectValue = subject.evaluate(attributes).getValue().getTime();
        final Number addingValue = addingEvaluator.evaluate(attributes).getValue();
        final Number weekIndexValue = weekIndexEvaluator.evaluate(attributes).getValue();

        int index = weekIndexValue.intValue();
        if (index < 1 || index > 7) {// only support 1-7, match to MON-SUN
            return new DateQueryResult(new Date(subjectValue));// no modify
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(subjectValue);
        calendar.add(Calendar.WEEK_OF_MONTH, addingValue.intValue());

        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        if (7 == index) { // SUN
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY); // SUN is 1
        } else {
            int value = index + 1; // MON-SAT
            calendar.set(Calendar.DAY_OF_WEEK, value); // MON is 2
        }

        long time = calendar.getTimeInMillis();
        return new DateQueryResult(new Date(time));
    }

    @Override
    public Evaluator<?> getSubjectEvaluator() {
        return subject;
    }
}
