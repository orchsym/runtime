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
package com.orchsym.processor.attributes;

/**
 * @author Lu JB
 *
 */
public class ExtractXMLToAttributesTest {
    // private ExtractXMLToAttributes processor;
    //
    // @BeforeEach
    // public void before() {
    // processor = new ExtractXMLToAttributes();
    // }
    //
    // @AfterEach
    // public void after() {
    // processor = null;
    // }
    /// *
    // <company>
    // <customers>
    // <customer cno="1000" tittle="doctor">
    // <cname>Charles</cname>
    // <city>xiamen</city>
    // <zip>67226</zip>
    // <age>28</age>
    // <phone>120</phone>
    // </customer>
    // <customer cno="1001" tittle="master">
    // <cname>Bismita</cname>
    // <city>beijing</city>
    // <age>40</age>
    // <zip>363900</zip>
    // <phone>110</phone>
    // </customer>
    // </customers>
    // </company>
    // */
    // private String createXmlString() {
    // return "<company><customers><customer cno=\"1000\" tittle=\"doctor\"><cname>Charles</cname><city>xiamen</city><zip>67226</zip><age>28</age><phone>120</phone></customer><customer cno=\"1001\"
    // tittle=\"master\"><cname>Bismita</cname><city>beijing</city><age>40</age><zip>363900</zip><phone>110</phone></customer></customers></company>";
    // }
    //
    //
    // @Test
    // public void test_processAttributes_Xquery() {
    // try {
    // SaxonXQDataSource ds = new SaxonXQDataSource();
    // XQConnection conn = ds.getConnection();
    // XQStaticContext sc = conn.getStaticContext();
    // String xmlContent = createXmlString();
    // Map<String, String> attributes = new HashMap<>();
    // XQResultSequence result = processor.retriveResultSequence(sc, conn, "<names>{for $c in /company/customers/customer/cname/text() return <name>{$c}</name>}</names>", xmlContent);
    // processor.processAttributesByXquery(attributes, result, "customer", null, null);
    // assertThat(attributes, hasEntry("customer", "<names>\n <name>Charles</name>\n <name>Bismita</name>\n</names>"));
    // assertThat(attributes.keySet(), hasSize(1));
    // } catch (Exception e) {
    // assertThat("", equalTo(e.getMessage()));
    // }
    // }
    //
    // @Test
    // public void test_processAttributes_Xquery_Attribute() {
    // try {
    // SaxonXQDataSource ds = new SaxonXQDataSource();
    // XQConnection conn = ds.getConnection();
    // XQStaticContext sc = conn.getStaticContext();
    // String xmlContent = createXmlString();
    // Map<String, String> attributes = new HashMap<>();
    // XQResultSequence result = processor.retriveResultSequence(sc, conn, "for $c in /company/customers/customer return data($c/@cno)", xmlContent);
    // processor.processAttributesByXquery(attributes, result, "cno", null, null);
    // assertThat(attributes, hasEntry("cno", "1000"));
    // assertThat(attributes.keySet(), hasSize(1));
    // } catch (Exception e) {
    // assertThat("", equalTo(e.getMessage()));
    // }
    // }
    //
    // @Test
    // public void test_processAttributes_Xquery_Attribute_Array() {
    // try {
    // processor.allowArray = true;
    // SaxonXQDataSource ds = new SaxonXQDataSource();
    // XQConnection conn = ds.getConnection();
    // XQStaticContext sc = conn.getStaticContext();
    // String xmlContent = createXmlString();
    // Map<String, String> attributes = new HashMap<>();
    // XQResultSequence result = processor.retriveResultSequence(sc, conn, "for $c in /company/customers/customer return data($c/@cno)", xmlContent);
    // processor.processAttributesByXquery(attributes, result, "cno", null, null);
    // assertThat(attributes, hasEntry("cno.0", "1000"));
    // assertThat(attributes, hasEntry("cno.1", "1001"));
    // assertThat(attributes.keySet(), hasSize(2));
    // } catch (Exception e) {
    // assertThat("", equalTo(e.getMessage()));
    // }
    // }
    //
    // @Test
    // public void test_processAttributes_Xquery_Evaluate_Value() {
    // try {
    // processor.allowArray = true;
    //
    // SaxonXQDataSource ds = new SaxonXQDataSource();
    // XQConnection conn = ds.getConnection();
    // XQStaticContext sc = conn.getStaticContext();
    // String xmlContent = createXmlString();
    // Map<String, String> attributes = new HashMap<>();
    // XQResultSequence result = processor.retriveResultSequence(sc, conn, "/company/customers/customer[1]/age > 30", xmlContent);
    // processor.processAttributesByXquery(attributes, result, "ret", null, null);
    // assertThat(attributes, hasEntry("ret", "false"));
    // assertThat(attributes.keySet(), hasSize(1));
    // } catch (Exception e) {
    // assertThat("", equalTo(e.getMessage()));
    // }
    // }
    //
    // @Test
    // public void test_processAttributes_Xquery_Evaluate_Object() {
    // try {
    // processor.allowArray = true;
    //
    // SaxonXQDataSource ds = new SaxonXQDataSource();
    // XQConnection conn = ds.getConnection();
    // XQStaticContext sc = conn.getStaticContext();
    // String xmlContent = createXmlString();
    // Map<String, String> attributes = new HashMap<>();
    // XQResultSequence result = processor.retriveResultSequence(sc, conn, "/company/customers/customer[2]", xmlContent);
    // processor.processAttributesByXquery(attributes, result, "ret", null, null);
    // assertThat(attributes, hasEntry("ret", "<customer cno=\"1001\" tittle=\"master\">\n <cname>Bismita</cname>\n <city>beijing</city>\n <age>40</age>\n <zip>363900</zip>\n
    // <phone>110</phone>\n</customer>"));
    // assertThat(attributes.keySet(), hasSize(1));
    // } catch (Exception e) {
    // assertThat("", equalTo(e.getMessage()));
    // }
    // }
    //
    // @Test
    // public void test_processAttributes_Xquery_Path_NotFound() {
    // try {
    // processor.allowArray = true;
    //
    // SaxonXQDataSource ds = new SaxonXQDataSource();
    // XQConnection conn = ds.getConnection();
    // XQStaticContext sc = conn.getStaticContext();
    // String xmlContent = createXmlString();
    // Map<String, String> attributes = new HashMap<>();
    // XQResultSequence result = processor.retriveResultSequence(sc, conn, "/company/invalid", xmlContent);
    // processor.processAttributesByXquery(attributes, result, "ret", null, null);
    // assertThat(attributes.keySet(), hasSize(0));
    // } catch (Exception e) {
    // assertThat("", equalTo(e.getMessage()));
    // }
    // }
}
