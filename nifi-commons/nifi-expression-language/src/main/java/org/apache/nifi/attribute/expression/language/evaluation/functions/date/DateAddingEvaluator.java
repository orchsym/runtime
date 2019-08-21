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
import java.util.Map;
import java.util.Date;

import org.apache.nifi.attribute.expression.language.evaluation.DateEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.Evaluator;
import org.apache.nifi.attribute.expression.language.evaluation.DateQueryResult;

public abstract class DateAddingEvaluator extends DateEvaluator {

    private final DateEvaluator subject;
    private final Evaluator<Number> addingEvaluator;

    public DateAddingEvaluator(final DateEvaluator subject, final Evaluator<Number> adding) {
        this.subject = subject;
        this.addingEvaluator = adding;
    }

    @Override
    public DateQueryResult evaluate(final Map<String, String> attributes) {
        final long subjectValue = subject.evaluate(attributes).getValue().getTime();
        final Number addingValue = addingEvaluator.evaluate(attributes).getValue();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(subjectValue);
        calendar.add(getAddingField(), addingValue.intValue());

        long time = calendar.getTime().getTime();
        return new DateQueryResult(new Date(time));
    }

    protected abstract int getAddingField();

    @Override
    public Evaluator<?> getSubjectEvaluator() {
        return subject;
    }

}
