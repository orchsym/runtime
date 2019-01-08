package com.orchsym.processor.attributes;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.xquery.XQConnection;
import javax.xml.xquery.XQResultSequence;
import javax.xml.xquery.XQStaticContext;

import net.sf.saxon.xqj.SaxonXQDataSource;

/**
 * @author Lu JB
 *
 */
public class ExtractXMLToAttributesTest {
    private ExtractXMLToAttributes processor;

    @BeforeEach
    public void before() {
        processor = new ExtractXMLToAttributes();
    }

    @AfterEach
    public void after() {
        processor = null;
    }
/*
<company>
    <customers>
        <customer cno="1000" tittle="doctor">
            <cname>Charles</cname>
            <city>xiamen</city>
            <zip>67226</zip>
            <age>28</age>
            <phone>120</phone>
        </customer>
        <customer cno="1001"  tittle="master">
            <cname>Bismita</cname>
            <city>beijing</city>
            <age>40</age>
            <zip>363900</zip>
            <phone>110</phone>
        </customer>
    </customers>
</company>
*/
    private String createXmlString() {
        return "<company><customers><customer cno=\"1000\" tittle=\"doctor\"><cname>Charles</cname><city>xiamen</city><zip>67226</zip><age>28</age><phone>120</phone></customer><customer cno=\"1001\"  tittle=\"master\"><cname>Bismita</cname><city>beijing</city><age>40</age><zip>363900</zip><phone>110</phone></customer></customers></company>";
    }

    @Test
    public void test_processAttributes_Xpath() {
        try {
            String xmlContent = createXmlString();

            Map<String, String> attributes = new HashMap<>();
            processor.processAttributesByXpath(attributes, xmlContent, "cname", "//company/customers/customer/cname", null, null, false, null);
            assertThat(attributes, hasEntry("cname", "Charles"));
            assertThat(attributes.keySet(), hasSize(1));
        } catch (Exception e) {
            assertThat("", equalTo(e.getMessage()));
        }
    }

    @Test
    public void test_processAttributes_Xpath_Array() {
        try {
            String xmlContent = createXmlString();
            processor.allowArray = true;
            Map<String, String> attributes = new HashMap<>();
            processor.processAttributesByXpath(attributes, xmlContent, "cname", "//company/customers/customer/cname", null, null, false, null);
            assertThat(attributes, hasEntry("cname.0", "Charles"));
            assertThat(attributes, hasEntry("cname.1", "Bismita"));
            assertThat(attributes.keySet(), hasSize(2));
        } catch (Exception e) {
            assertThat("", equalTo(e.getMessage()));
        }
    }

    @Test
    public void test_processAttributes_Xpath_Attribute() {
        try {
            String xmlContent = createXmlString();
            Map<String, String> attributes = new HashMap<>();
            processor.processAttributesByXpath(attributes, xmlContent, "cno", "//company/customers/customer/@*", null, null, false, null);
            assertThat(attributes, hasEntry("cno", "1000"));
            assertThat(attributes.keySet(), hasSize(1));
        } catch (Exception e) {
            assertThat("", equalTo(e.getMessage()));
        }
    }

    @Test
    public void test_processAttributes_Xpath_Attribute_Mark() {
        try {
            String xmlContent = createXmlString();
            Map<String, String> attributes = new HashMap<>();
            processor.processAttributesByXpath(attributes, xmlContent, "customer", "//company/customers/customer[1]", null, null, true, "@");
            assertThat(attributes, hasEntry("customer", "Charlesxiamen6722628120"));
            assertThat(attributes, hasEntry("@cno", "1000"));
            assertThat(attributes, hasEntry("@tittle", "doctor"));
            assertThat(attributes.keySet(), hasSize(3));
        } catch (Exception e) {
            assertThat("", equalTo(e.getMessage()));
        }
    }

    @Test
    public void test_processAttributes_Xpath_Attributes() {
        try {
            processor.allowArray = true;
            String xmlContent = createXmlString();
            Map<String, String> attributes = new HashMap<>();
            processor.processAttributesByXpath(attributes, xmlContent, "cno", "//company/customers/customer/@*", null, null, false, null);
            assertThat(attributes, hasEntry("cno.0", "1000"));
            assertThat(attributes, hasEntry("cno.1", "doctor"));
            assertThat(attributes, hasEntry("cno.2", "1001"));
            assertThat(attributes, hasEntry("cno.3", "master"));
            assertThat(attributes.keySet(), hasSize(4));
        } catch (Exception e) {
            assertThat("", equalTo(e.getMessage()));
        }
    }

    @Test
    public void test_processAttributes_Xpath_Object() {
        try {
            String xmlContent = createXmlString();
            Map<String, String> attributes = new HashMap<>();
            processor.processAttributesByXpath(attributes, xmlContent, "customer", "//company/customers/customer", null, null, false, null);
            assertThat(attributes, hasEntry("customer", "Charlesxiamen6722628120"));
            assertThat(attributes.keySet(), hasSize(1));
        } catch (Exception e) {
            assertThat("", equalTo(e.getMessage()));
        }
    }

    @Test
    public void test_processAttributes_Xpath_NotFound() {
        try {
            String xmlContent = createXmlString();
            Map<String, String> attributes = new HashMap<>();
            processor.processAttributesByXpath(attributes, xmlContent, "customer", "//company/customers/error", null, null, false, null);
            assertThat(attributes.keySet(), hasSize(0));
        } catch (Exception e) {
            assertThat("", equalTo(e.getMessage()));
        }
    }

    @Test
    public void test_processAttributes_Xquery() {
        try {
            SaxonXQDataSource ds = new SaxonXQDataSource();
            XQConnection conn = ds.getConnection();
            XQStaticContext sc = conn.getStaticContext();
            String xmlContent = createXmlString();
            Map<String, String> attributes = new HashMap<>();
            XQResultSequence result = processor.retriveResultSequence(sc, conn, "<names>{for $c in  /company/customers/customer/cname/text() return <name>{$c}</name>}</names>", xmlContent);
            processor.processAttributesByXquery(attributes, result, "customer", null, null);
            assertThat(attributes, hasEntry("customer", "<names>\n   <name>Charles</name>\n   <name>Bismita</name>\n</names>"));
            assertThat(attributes.keySet(), hasSize(1));
        } catch (Exception e) {
            assertThat("", equalTo(e.getMessage()));
        }
    }

    @Test
    public void test_processAttributes_Xquery_Attribute() {
        try {
            SaxonXQDataSource ds = new SaxonXQDataSource();
            XQConnection conn = ds.getConnection();
            XQStaticContext sc = conn.getStaticContext();
            String xmlContent = createXmlString();
            Map<String, String> attributes = new HashMap<>();
            XQResultSequence result = processor.retriveResultSequence(sc, conn, "for $c in /company/customers/customer return data($c/@cno)", xmlContent);
            processor.processAttributesByXquery(attributes, result, "cno", null, null);
            assertThat(attributes, hasEntry("cno", "1000"));
            assertThat(attributes.keySet(), hasSize(1));
        } catch (Exception e) {
            assertThat("", equalTo(e.getMessage()));
        }
    }

    @Test
    public void test_processAttributes_Xquery_Attribute_Array() {
        try {
            processor.allowArray = true;
            SaxonXQDataSource ds = new SaxonXQDataSource();
            XQConnection conn = ds.getConnection();
            XQStaticContext sc = conn.getStaticContext();
            String xmlContent = createXmlString();
            Map<String, String> attributes = new HashMap<>();
            XQResultSequence result = processor.retriveResultSequence(sc, conn, "for $c in /company/customers/customer return data($c/@cno)", xmlContent);
            processor.processAttributesByXquery(attributes, result, "cno", null, null);
            assertThat(attributes, hasEntry("cno.0", "1000"));
            assertThat(attributes, hasEntry("cno.1", "1001"));
            assertThat(attributes.keySet(), hasSize(2));
        } catch (Exception e) {
            assertThat("", equalTo(e.getMessage()));
        }
    }

    @Test
    public void test_processAttributes_Xquery_Evaluate_Value() {
        try {
            processor.allowArray = true;

            SaxonXQDataSource ds = new SaxonXQDataSource();
            XQConnection conn = ds.getConnection();
            XQStaticContext sc = conn.getStaticContext();
            String xmlContent = createXmlString();
            Map<String, String> attributes = new HashMap<>();
            XQResultSequence result = processor.retriveResultSequence(sc, conn, "/company/customers/customer[1]/age > 30", xmlContent);
            processor.processAttributesByXquery(attributes, result, "ret", null, null);
            assertThat(attributes, hasEntry("ret", "false"));
            assertThat(attributes.keySet(), hasSize(1));
        } catch (Exception e) {
            assertThat("", equalTo(e.getMessage()));
        }
    }

    @Test
    public void test_processAttributes_Xquery_Evaluate_Object() {
        try {
            processor.allowArray = true;

            SaxonXQDataSource ds = new SaxonXQDataSource();
            XQConnection conn = ds.getConnection();
            XQStaticContext sc = conn.getStaticContext();
            String xmlContent = createXmlString();
            Map<String, String> attributes = new HashMap<>();
            XQResultSequence result = processor.retriveResultSequence(sc, conn, "/company/customers/customer[2]", xmlContent);
            processor.processAttributesByXquery(attributes, result, "ret", null, null);
            assertThat(attributes, hasEntry("ret", "<customer cno=\"1001\" tittle=\"master\">\n   <cname>Bismita</cname>\n   <city>beijing</city>\n   <age>40</age>\n   <zip>363900</zip>\n   <phone>110</phone>\n</customer>"));
            assertThat(attributes.keySet(), hasSize(1));
        } catch (Exception e) {
            assertThat("", equalTo(e.getMessage()));
        }
    }

    @Test
    public void test_processAttributes_Xquery_Path_NotFound() {
        try {
            processor.allowArray = true;

            SaxonXQDataSource ds = new SaxonXQDataSource();
            XQConnection conn = ds.getConnection();
            XQStaticContext sc = conn.getStaticContext();
            String xmlContent = createXmlString();
            Map<String, String> attributes = new HashMap<>();
            XQResultSequence result = processor.retriveResultSequence(sc, conn, "/company/invalid", xmlContent);
            processor.processAttributesByXquery(attributes, result, "ret", null, null);
            assertThat(attributes.keySet(), hasSize(0));
        } catch (Exception e) {
            assertThat("", equalTo(e.getMessage()));
        }
    }
}
