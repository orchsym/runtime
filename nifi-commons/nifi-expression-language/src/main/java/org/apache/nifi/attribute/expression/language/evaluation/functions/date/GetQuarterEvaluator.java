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

import org.apache.nifi.attribute.expression.language.evaluation.DateEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.Evaluator;
import org.apache.nifi.attribute.expression.language.evaluation.NumberQueryResult;
import org.apache.nifi.attribute.expression.language.evaluation.QueryResult;

public class GetQuarterEvaluator extends GetMonthEvaluator {

    public GetQuarterEvaluator(DateEvaluator subject, Evaluator<String> timeZone) {
        super(subject, timeZone);
    }

    @Override
    public QueryResult<Number> evaluate(Map<String, String> attributes) {
        QueryResult<Number> result = super.evaluate(attributes);
        int quarter = 0;
        int month = result.getValue().intValue();
        if (Calendar.JANUARY == month || Calendar.FEBRUARY == month || Calendar.MARCH == month) {
            quarter = 1;
        } else if (Calendar.APRIL == month || Calendar.MAY == month || Calendar.JUNE == month) {
            quarter = 2;
        } else if (Calendar.JULY == month || Calendar.AUGUST == month || Calendar.SEPTEMBER == month) {
            quarter = 3;
        } else if (Calendar.OCTOBER == month || Calendar.NOVEMBER == month || Calendar.DECEMBER == month) {
            quarter = 4;
        }

        return new NumberQueryResult(quarter);
    }

}
