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
package org.apache.nifi.attribute.expression.language;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TestDate {

    @Test
    public void testDate_AddYears() {
        final Map<String, String> attributes = new HashMap<>();
        String timeStr = "2013-11-18 10:22:27.678";
        attributes.put("dateTime", timeStr);

        String ret = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addYears(2):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret, "2015-11-18 10:22:27.678");

        String ret2 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addYears(-2):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret2, "2011-11-18 10:22:27.678");
    }

    @Test
    public void testDate_AddMonths() {
        final Map<String, String> attributes = new HashMap<>();
        String timeStr = "2013-11-18 10:22:27.678";
        attributes.put("dateTime", timeStr);

        String ret = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addMonths(1):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret, "2013-12-18 10:22:27.678");

        String ret2 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addMonths(3):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret2, "2014-02-18 10:22:27.678");

        String ret3 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addMonths(-3):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret3, "2013-08-18 10:22:27.678");

        String ret4 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addMonths(-13):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret4, "2012-10-18 10:22:27.678");
    }

    @Test
    public void testDate_AddWeeks() {
        final Map<String, String> attributes = new HashMap<>();
        String timeStr = "2013-11-18 10:22:27.678";
        attributes.put("dateTime", timeStr);

        String ret = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addWeeks(1):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret, "2013-11-25 10:22:27.678");

        String ret2 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addWeeks(2):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret2, "2013-12-02 10:22:27.678");

        String ret3 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addWeeks(-2):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret3, "2013-11-04 10:22:27.678");

        String ret4 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addWeeks(-3):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret4, "2013-10-28 10:22:27.678");
    }

    @Test
    public void testDate_AddDays() {
        final Map<String, String> attributes = new HashMap<>();
        String timeStr = "2013-11-18 10:22:27.678";
        attributes.put("dateTime", timeStr);

        String ret = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addDays(10):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret, "2013-11-28 10:22:27.678");

        String ret2 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addDays(20):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret2, "2013-12-08 10:22:27.678");

        String ret3 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addDays(-10):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret3, "2013-11-08 10:22:27.678");

        String ret4 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addDays(-20):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret4, "2013-10-29 10:22:27.678");
    }

    @Test
    public void testDate_AddHours() {
        final Map<String, String> attributes = new HashMap<>();
        String timeStr = "2013-11-18 10:22:27.678";
        attributes.put("dateTime", timeStr);

        String ret = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addHours(10):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret, "2013-11-18 20:22:27.678");

        String ret2 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addHours(25):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret2, "2013-11-19 11:22:27.678");

        String ret3 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addHours(-10):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret3, "2013-11-18 00:22:27.678");

        String ret4 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addHours(-25):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret4, "2013-11-17 09:22:27.678");
    }

    @Test
    public void testDate_AddMins() {
        final Map<String, String> attributes = new HashMap<>();
        String timeStr = "2013-11-18 10:22:27.678";
        attributes.put("dateTime", timeStr);

        String ret = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addMinutes(10):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret, "2013-11-18 10:32:27.678");

        String ret2 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addMinutes(130):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret2, "2013-11-18 12:32:27.678");

        String ret3 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addMinutes(-10):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret3, "2013-11-18 10:12:27.678");

        String ret4 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addMinutes(-130):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret4, "2013-11-18 08:12:27.678");
    }

    @Test
    public void testDate_AddSecs() {
        final Map<String, String> attributes = new HashMap<>();
        String timeStr = "2013-11-18 10:22:27.678";
        attributes.put("dateTime", timeStr);

        String ret = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addSeconds(20):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret, "2013-11-18 10:22:47.678");

        String ret2 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addSeconds(70):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret2, "2013-11-18 10:23:37.678");

        String ret3 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addSeconds(-20):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret3, "2013-11-18 10:22:07.678");

        String ret4 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):addSeconds(-70):format('yyyy-MM-dd HH:mm:ss.SSS')}", attributes, null);
        assertEquals(ret4, "2013-11-18 10:21:17.678");
    }

    @Test
    public void testDate_GetYear() {
        final Map<String, String> attributes = new HashMap<>();
        String timeStr = "2013-11-18 10:22:27.678";
        attributes.put("dateTime", timeStr);

        String ret = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):getYear()}", attributes, null);
        assertEquals(ret, "2013");
    }

    @Test
    public void testDate_GetMonth() {
        final Map<String, String> attributes = new HashMap<>();
        String timeStr = "2013-11-18 10:22:27.678";
        attributes.put("dateTime", timeStr);

        String ret = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):getMonth()}", attributes, null);
        assertEquals(ret, "10"); // month start from 0
    }

    @Test
    public void testDate_GetDay() {
        final Map<String, String> attributes = new HashMap<>();
        String timeStr = "2019-02-10 10:22:27.678";
        attributes.put("dateTime", timeStr);

        String ret = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):getDayOfYear()}", attributes, null);
        assertEquals(ret, "41");

        String ret2 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):getDayOfMonth()}", attributes, null);
        assertEquals(ret2, "10");

        String ret3 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):getDayOfWeek()}", attributes, null);
        assertEquals(ret3, "1"); // 周日

        String ret4 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):getDayOfWeekInMonth()}", attributes, null);
        assertEquals(ret4, "2");
    }

    @Test
    public void testDate_GetWeek() {
        final Map<String, String> attributes = new HashMap<>();
        String timeStr = "2019-02-10 10:22:27.678";
        attributes.put("dateTime", timeStr);

        String ret = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):getWeekOfMonth()}", attributes, null);
        assertEquals(ret, "3");

        String ret2 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):getWeekOfYear()}", attributes, null);
        assertEquals(ret2, "7");
    }

    @Test
    public void testDate_GetHour() {
        final Map<String, String> attributes = new HashMap<>();
        String timeStr = "2019-03-06 15:05:17";
        attributes.put("dateTime", timeStr);

        String ret = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss'):getHour()}", attributes, null);
        assertEquals(ret, "3");
        String ret2 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss'):getHourOfDay()}", attributes, null);
        assertEquals(ret2, "15");

        String ret3 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss'):getHour('GMT')}", attributes, null);
        assertEquals(ret3, "7");
        String ret4 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss'):getHourOfDay('GMT')}", attributes, null);
        assertEquals(ret4, "7");
    }

    @Test
    public void testDate_GetTime() {
        final Map<String, String> attributes = new HashMap<>();
        String timeStr = "2019-03-06 15:05:17.122";
        attributes.put("dateTime", timeStr);

        String ret = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):getMinute()}", attributes, null);
        assertEquals(ret, "5");
        String ret2 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):getSecond()}", attributes, null);
        assertEquals(ret2, "17");

        String ret3 = Query.evaluateExpressions("${dateTime:toDate('yyyy-MM-dd HH:mm:ss.SSS'):getMilliSecond()}", attributes, null);
        assertEquals(ret3, "122");
    }

    @Test
    public void testDate_After() {
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("date1", "2019-03-08 15:05:17.122");
        attributes.put("date2", "2019-03-08 15:05:17.123");
        attributes.put("date3", "2019-03-08 15:05:17.100");

        String ret = Query.evaluateExpressions("${date1:toDate('yyyy-MM-dd HH:mm:ss.SSS'):after(${date2:toDate('yyyy-MM-dd HH:mm:ss.SSS')})}", attributes, null);
        assertEquals(ret, "false");
        String ret2 = Query.evaluateExpressions("${date1:toDate('yyyy-MM-dd HH:mm:ss.SSS'):after(${date3:toDate('yyyy-MM-dd HH:mm:ss.SSS')})}", attributes, null);
        assertEquals(ret2, "true");
        String ret3 = Query.evaluateExpressions("${date2:toDate('yyyy-MM-dd HH:mm:ss.SSS'):after(${date3:toDate('yyyy-MM-dd HH:mm:ss.SSS')})}", attributes, null);
        assertEquals(ret3, "true");
    }

    @Test
    public void testDate_Before() {
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("date1", "2019-03-08 15:05:17.122");
        attributes.put("date2", "2019-03-08 15:05:17.123");
        attributes.put("date3", "2019-03-08 15:05:17.100");

        String ret = Query.evaluateExpressions("${date1:toDate('yyyy-MM-dd HH:mm:ss.SSS'):before(${date2:toDate('yyyy-MM-dd HH:mm:ss.SSS')})}", attributes, null);
        assertEquals(ret, "true");
        String ret2 = Query.evaluateExpressions("${date1:toDate('yyyy-MM-dd HH:mm:ss.SSS'):before(${date3:toDate('yyyy-MM-dd HH:mm:ss.SSS')})}", attributes, null);
        assertEquals(ret2, "false");
        String ret3 = Query.evaluateExpressions("${date2:toDate('yyyy-MM-dd HH:mm:ss.SSS'):before(${date3:toDate('yyyy-MM-dd HH:mm:ss.SSS')})}", attributes, null);
        assertEquals(ret3, "false");
    }

    @Test
    public void testDate_GetDateForWeek() {
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("date1", "2019-03-08 15:05:17.122");

        String ret = Query.evaluateExpressions("${date1:toDate('yyyy-MM-dd HH:mm:ss.SSS'):getDateForWeek(2,3):format('yyyy-MM-dd')}", attributes, null); // next 2 week and the Wed
        assertEquals(ret, "2019-03-20");

        ret = Query.evaluateExpressions("${date1:toDate('yyyy-MM-dd HH:mm:ss.SSS'):getDateForWeek(3,0):format('yyyy-MM-dd')}", attributes, null); // next 2 week and the Wed
        assertEquals(ret, "2019-03-08"); // no modify

        ret = Query.evaluateExpressions("${date1:toDate('yyyy-MM-dd HH:mm:ss.SSS'):getDateForWeek(3,-1):format('yyyy-MM-dd')}", attributes, null); // next 2 week and the Wed
        assertEquals(ret, "2019-03-08"); // no modify

        ret = Query.evaluateExpressions("${date1:toDate('yyyy-MM-dd HH:mm:ss.SSS'):getDateForWeek(3,1):format('yyyy-MM-dd')}", attributes, null); // next 2 week and the Wed
        assertEquals(ret, "2019-03-25");

        ret = Query.evaluateExpressions("${date1:toDate('yyyy-MM-dd HH:mm:ss.SSS'):getDateForWeek(3,6):format('yyyy-MM-dd')}", attributes, null); // next 2 week and the Wed
        assertEquals(ret, "2019-03-30");

        ret = Query.evaluateExpressions("${date1:toDate('yyyy-MM-dd HH:mm:ss.SSS'):getDateForWeek(3,7):format('yyyy-MM-dd')}", attributes, null); // next 2 week and the Wed
        assertEquals(ret, "2019-03-31");

        //
        String ret2 = Query.evaluateExpressions("${date1:toDate('yyyy-MM-dd HH:mm:ss.SSS'):getDateForWeek(-1,6):format('yyyy-MM-dd')}", attributes, null); // last week and the SUN
        assertEquals(ret2, "2019-03-02");

        ret2 = Query.evaluateExpressions("${date1:toDate('yyyy-MM-dd HH:mm:ss.SSS'):getDateForWeek(-2,1):format('yyyy-MM-dd')}", attributes, null); // last week and the SUN
        assertEquals(ret2, "2019-02-18");

        ret2 = Query.evaluateExpressions("${date1:toDate('yyyy-MM-dd HH:mm:ss.SSS'):getDateForWeek(-2,6):format('yyyy-MM-dd')}", attributes, null); // last week and the SUN
        assertEquals(ret2, "2019-02-23");

        ret2 = Query.evaluateExpressions("${date1:toDate('yyyy-MM-dd HH:mm:ss.SSS'):getDateForWeek(-2,7):format('yyyy-MM-dd')}", attributes, null); // last week and the SUN
        assertEquals(ret2, "2019-02-24");
    }

    @Test
    public void testDate_IsWeekend() {
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("date1", "2019-03-08");
        attributes.put("date2", "2019-03-09");
        attributes.put("date3", "2019-03-10");
        attributes.put("date4", "2019-03-11");

        String ret = Query.evaluateExpressions("${date1:toDate('yyyy-MM-dd'):isWeekend()}", attributes, null);
        assertEquals(ret, "false");
        String ret2 = Query.evaluateExpressions("${date2:toDate('yyyy-MM-dd'):isWeekend()}", attributes, null);
        assertEquals(ret2, "true");
        String ret3 = Query.evaluateExpressions("${date3:toDate('yyyy-MM-dd'):isWeekend()}", attributes, null);
        assertEquals(ret3, "true");
        String ret4 = Query.evaluateExpressions("${date4:toDate('yyyy-MM-dd'):isWeekend()}", attributes, null);
        assertEquals(ret4, "false");
    }

    @Test
    public void testDate_GetQuarter() {
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("date1", "2019-03-08");
        attributes.put("date2", "2019-04-09");
        attributes.put("date3", "2019-08-01");
        attributes.put("date4", "2019-12-31");

        String ret = Query.evaluateExpressions("${date1:toDate('yyyy-MM-dd'):getQuarter()}", attributes, null);
        assertEquals(ret, "1");
        String ret2 = Query.evaluateExpressions("${date2:toDate('yyyy-MM-dd'):getQuarter()}", attributes, null);
        assertEquals(ret2, "2");
        String ret3 = Query.evaluateExpressions("${date3:toDate('yyyy-MM-dd'):getQuarter('GMT')}", attributes, null);
        assertEquals(ret3, "3");
        String ret4 = Query.evaluateExpressions("${date4:toDate('yyyy-MM-dd'):getQuarter()}", attributes, null);
        assertEquals(ret4, "4");
    }
}
