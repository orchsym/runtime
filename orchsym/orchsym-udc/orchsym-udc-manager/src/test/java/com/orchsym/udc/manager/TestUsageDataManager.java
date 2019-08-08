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
package com.orchsym.udc.manager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.orchsym.udc.manager.UsageDataManager.Freq;

/**
 * @author GU Guoqiang
 *
 */
public class TestUsageDataManager {

    private List<String> dateList;
    private LocalDate now = LocalDate.now().withYear(2019).withMonth(8).withDayOfMonth(8);;

    @Before
    public void initData() {
        dateList = new ArrayList<>();

        dateList.addAll(generateDate(2018, 3));
        dateList.addAll(generateDate(2018, 6));

        dateList.addAll(generateDate(2019, 5));
        dateList.addAll(generateDate(2019, 6));
        dateList.addAll(generateDate(2019, 7));

        dateList.addAll(generateDate(now.getYear(), now.getMonthValue(), now.getDayOfMonth()));

        Collections.sort(dateList);
        Assert.assertEquals(158, dateList.size());
    }

    List<String> generateDate(int year, int month) {
        return generateDate(year, month, 30);
    }

    List<String> generateDate(int year, int month, int day) {
        List<String> genList = new ArrayList<>();
        LocalDate cur = LocalDate.now().withYear(year).withMonth(month).withDayOfMonth(day);
        LocalDate date = LocalDate.now().withYear(cur.getYear()).withMonth(cur.getMonthValue());
        for (int i = 1; i <= cur.getDayOfMonth(); i++) {
            genList.add(date.withDayOfMonth(i).toString());
        }
        return genList;
    }

    @After
    public void cleanData() {
        dateList.clear();
        dateList = null;
    }

    @Test
    public void test_filterDateAndGroup_weekly() {
        final Map<LocalDate, List<LocalDate>> group = UsageDataManager.filterDateAndGroup(dateList, now, Freq.weekly);

        Assert.assertEquals(24, group.size());

        testLeftGroup(group);
    }

    @Test
    public void test_filterDateAndGroup_monthly() {
        final Map<LocalDate, List<LocalDate>> group = UsageDataManager.filterDateAndGroup(dateList, now, Freq.monthly);

        Assert.assertEquals(5, group.size());

        testLeftGroup(group);
    }

    @Test
    public void test_filterDateAndGroup_yearly() {
        final Map<LocalDate, List<LocalDate>> group = UsageDataManager.filterDateAndGroup(dateList, now, Freq.yearly);

        Assert.assertEquals(2, group.size());

        testLeftGroup(group);
    }

    private void testLeftGroup(final Map<LocalDate, List<LocalDate>> group) {
        final List<LocalDate> result = group.values().stream().flatMap(list -> list.stream()).collect(Collectors.toList());

        Assert.assertEquals(150, result.size());
    }
}
