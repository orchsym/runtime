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
package com.orchsym.processor.jsonxml;

import java.io.IOException;

import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.CoreMatchers.*;

/**
 * @author Lu JB
 *
 */

public class ConvertXMLToJSONTest {
    private TestRunner testRunner;
    private ConvertXMLToJSON processor;

    @Before
    public void setup() {
        processor = new ConvertXMLToJSON();
        testRunner = TestRunners.newTestRunner(processor);
    }
/*
<Employees>
    <Employee id="1">
        <device>computer</device>
        <device>ipad</device>
        <age>30</age>
        <name>jiangbin</name>
        <gender>Male</gender>
        <role department="IT" extra="manager">Java Developer</role>
        <additional>
            <petname>Doggy</petname>
            <petage>2</petage>
        </additional>
    </Employee>
    <Employee id="2">
        <age>35</age>
        <name>Lisa</name>
        <gender>Female</gender>
        <role  department="Decision" extra="CIO">CEO</role>
    </Employee>
    <Employee id="3">lu</Employee>
</Employees>
*/
    private String createXmlString() {
        return "<Employees><Employee id=\"1\"><device>computer</device><device>ipad</device><age>30</age><name>jiangbin</name><gender>Male</gender><role department=\"IT\" extra=\"manager\">Java Developer</role><additional><petname>Doggy</petname><petage>2</petage></additional></Employee><Employee id=\"2\"><age>35</age><name>Lisa</name><gender>Female</gender><role  department=\"Decision\" extra=\"CIO\">CEO</role></Employee><Employee id=\"3\">lu</Employee></Employees>";
    }

    @Test
    public void testXml_To_Map() {
        try {
            String xmlContent = createXmlString();
            Map<String, Object> map = processor.convertXMLStrToMap(xmlContent, null, null, "content");
            assertThat(map.keySet(), hasSize(1));
            assertThat(map, hasKey("Employees"));

            Map<String, Object> employeesMap = (Map<String, Object>)map.get("Employees");
            assertThat(employeesMap.keySet(), hasSize(1));

            List employeeList = (List)employeesMap.get("Employee");
            assertThat(employeeList.size(), equalTo(3));

            Map<String, Object> employee1 = (Map<String, Object>) employeeList.get(0);
            String gender = (String) employee1.get("gender");
            assertThat(gender, equalTo("Male"));

            //test attributes
            Map<String, Object> roleMap = (Map<String, Object> )employee1.get("role");
            assertThat(roleMap.keySet(), hasSize(3));
            assertThat(roleMap, hasEntry("department", "IT"));
            assertThat(roleMap, hasEntry("extra", "manager"));
            assertThat(roleMap, hasEntry("content", "Java Developer"));
            
            //test multi items with same name
            Object devicesObject = employee1.get("device");
            assertTrue(devicesObject instanceof List<?>);

            List<String> devicesList = (List<String>)employee1.get("device");
            assertThat(devicesList, hasItems("computer","ipad"));
            assertThat(devicesList, hasSize(2));

            //test sub map
            Map<String, Object> additionalMap = (Map<String, Object> )employee1.get("additional");
            assertThat(additionalMap.keySet(), hasSize(2));
            assertThat(additionalMap.get("petname"), equalTo("Doggy"));
            assertThat(additionalMap.get("petage"), equalTo(2));

        } catch (Exception e) {
            assertEquals("", e.getMessage());
        }
    }

    @Test
    public void test_Invalid_Xml_To_Map() {
        try {
            String xmlContent = "<item>123<";
            Map<String, Object> map = processor.convertXMLStrToMap(xmlContent, null, null, "content");
            assertThat(map.keySet(), empty());
        } catch (Exception e) {
            assertEquals("", e.getMessage());
        }
    }

    @Test
    public void testXml_To_Map_With_XPath_Expression() {
        try {
            String xmlContent = createXmlString();
            Map<String, Object> map = processor.convertXMLStrToMap(xmlContent, "/Employees/Employee[3]", null, "content");
            assertThat(map.keySet(), hasSize(1));
            assertThat(map, hasKey("Employee"));

            Map<String, Object> employeeMap = (Map<String, Object>)map.get("Employee");
            //test attributes
            assertThat(employeeMap.keySet(), hasSize(2));
            assertThat(employeeMap, hasEntry("id", "3"));
            assertThat(employeeMap, hasEntry("content", "lu"));
        } catch (Exception e) {
            assertEquals("", e.getMessage());
        }
    }

    @Test
    public void testXml_To_Map_With_XPath_Expression_Age() {
        try {
            String xmlContent = createXmlString();
            Map<String, Object> map = processor.convertXMLStrToMap(xmlContent, "/Employees/Employee[2]/age", null, "content");
            assertThat(map.keySet(), hasSize(1));
            assertThat(map, hasKey("age"));
            assertThat(map, hasEntry("age", "35"));
        } catch (Exception e) {
            assertEquals("", e.getMessage());
        }
    }

    @Test
    public void testXml_To_Map_With_XPath_Expression_Not_Found() {
        try {
            String xmlContent = createXmlString();
            Map<String, Object> map = processor.convertXMLStrToMap(xmlContent, "/Employees/Employee[11]/age", null, "content");
            assertThat(map.keySet(), empty());
        } catch (Exception e) {
            assertEquals("", e.getMessage());
        }
    }

    @Test
    public void testXml_To_Map_With_Invalid_XPath_Expression() {
        try {
            String xmlContent = createXmlString();
            Map<String, Object> map = processor.convertXMLStrToMap(xmlContent, "###/Em", null, "content");
            assertThat(map.keySet(), empty());
        } catch (Exception e) {
            assertEquals("", e.getMessage());
        }
    }

    @Test
    public void testXml_To_Map_With_Mark_KeyName() {
        try {
            String xmlContent = "<Employees><Employee id=\"3\">lu</Employee></Employees>";
            String attributesMark = "@";
            String keyName = "newKeyName";
            Map<String, Object> map = processor.convertXMLStrToMap(xmlContent, "/Employees/Employee", attributesMark, keyName);
            assertThat(map.keySet(), hasSize(1));

            Map<String, Object> employeeMap = (Map<String, Object>)map.get("Employee");
            assertThat(employeeMap, hasEntry("@id", "3"));
            assertThat(employeeMap, hasEntry("newKeyName", "lu"));
        } catch (Exception e) {
            assertEquals("", e.getMessage());
        }
    }

    @Test
    public void testXml_To_Map_With_Default_KeyName() {
        try {
            String xmlContent = "<Employees><Employee id=\"3\">lu</Employee></Employees>";
            String attributesMark = "@";
            String keyName = null;
            Map<String, Object> map = processor.convertXMLStrToMap(xmlContent, "/Employees/Employee", attributesMark, keyName);
            assertThat(map.keySet(), hasSize(1));

            Map<String, Object> employeeMap = (Map<String, Object>)map.get("Employee");
            assertThat(employeeMap, hasEntry("@id", "3"));
            assertThat(employeeMap, hasEntry("content", "lu"));
        } catch (Exception e) {
            assertEquals("", e.getMessage());
        }
    }

    @Test
    public void testXml_To_Map_With_Default_KeyName_2() {
        try {
            String xmlContent = "<Employees><Employee id=\"3\">lu</Employee></Employees>";
            String attributesMark = "@";
            String keyName = null;
            Map<String, Object> map = processor.convertXMLStrToMap(xmlContent, null, attributesMark, keyName);
            assertThat(map.keySet(), hasSize(1));

            Map<String, Object> employeesMap = (Map<String, Object>)map.get("Employees");
            Map<String, Object> employeeMap = (Map<String, Object>)employeesMap.get("Employee");
            assertThat(employeeMap.keySet(), hasSize(2));
            assertThat(employeeMap, hasEntry("@id", 3));
            assertThat(employeeMap, hasEntry("content", "lu"));
        } catch (Exception e) {
            assertEquals("", e.getMessage());
        }
    }

    @Test
    public void testXml_To_Map_With_Default_KeyName_NO_Mark() {
        try {
            String xmlContent = "<Employees><Employee id=\"3\">lu</Employee></Employees>";
            String attributesMark = null;
            String keyName = null;
            Map<String, Object> map = processor.convertXMLStrToMap(xmlContent, "/Employees/Employee", attributesMark, keyName);
            assertThat(map.keySet(), hasSize(1));

            Map<String, Object> employeeMap = (Map<String, Object>)map.get("Employee");
            assertThat(employeeMap, hasEntry("id", "3"));
            assertThat(employeeMap, hasEntry("content", "lu"));
        } catch (Exception e) {
            assertEquals("", e.getMessage());
        }
    }

    @Test
    public void testXml_To_Map_With_Default_KeyName2_NO_Mark() {
        try {
            String xmlContent = "<Employees><Employee id=\"3\">lu</Employee></Employees>";
            String attributesMark = null;
            String keyName = null;
            Map<String, Object> map = processor.convertXMLStrToMap(xmlContent, null, attributesMark, keyName);
            assertThat(map.keySet(), hasSize(1));

            Map<String, Object> employeesMap = (Map<String, Object>)map.get("Employees");
            Map<String, Object> employeeMap = (Map<String, Object>)employeesMap.get("Employee");
            assertThat(employeeMap.keySet(), hasSize(2));
            assertThat(employeeMap, hasEntry("id", 3));
            assertThat(employeeMap, hasEntry("content", "lu"));
        } catch (Exception e) {
            assertEquals("", e.getMessage());
        }
    }
}
