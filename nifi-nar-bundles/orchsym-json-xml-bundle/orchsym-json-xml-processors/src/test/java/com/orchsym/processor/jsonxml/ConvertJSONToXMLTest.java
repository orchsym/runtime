/*
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

public class ConvertJSONToXMLTest {
    private TestRunner testRunner;
    private ConvertJSONToXML processor;

    @Before
    public void setup() {
        processor = new ConvertJSONToXML();
        testRunner = TestRunners.newTestRunner(processor);
    }
/*
{
    "user": {
        "name": "lu",
        "hobby": ["swim", "football"],
        "additional": {
            "score": 90,
            "class": "No.1",
            "book": {
                "name": "English",
                "price": "17$"
            }
        }
    }
}
<?xml version="1.0" encoding="UTF-8"?>
<user>
    <additional>
        <score>90</score>
        <book>
            <price>17$</price>
            <name>English</name>
        </book>
        <class>No.1</class>
    </additional>
    <name>lu</name>
    <hobby>swim</hobby>
    <hobby>football</hobby>
</user>
*/
    @Test
    public void testJson_To_XML_() {
        String jsonStr = "{\"user\":{\"name\":\"lu\",\"hobby\":[\"swim\",\"football\"],\"additional\":{\"score\":90,\"class\":\"No.1\",\"book\":{\"name\":\"English\",\"price\":\"17$\"}}}}";
        String xmlStr = processor.convertJsonToXMLStr(jsonStr, null, null, null, null, "UTF-8", null);
        assertThat(xmlStr, equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?><user><additional><score>90</score><book><price>17$</price><name>English</name></book><class>No.1</class></additional><name>lu</name><hobby>swim</hobby><hobby>football</hobby></user>"));
    }

    @Test
    public void testJson_To_XML_Array() {
        String jsonStr = "{\"user\":{\"name\":\"lu\",\"age\":12,\"hobby\":[\"swim\",\"football\",\"basketball\"]}}";
        String xmlStr = processor.convertJsonToXMLStr(jsonStr, null, null, null, null, "UTF-8", null);
        assertThat(xmlStr, equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?><user><name>lu</name><age>12</age><hobby>swim</hobby><hobby>football</hobby><hobby>basketball</hobby></user>"));
    }
/*
{
    "user": {
        "name": "lu",
        "book": {
            "@id": "123",
            "@price": "17$",
            "content": "English"
        }
    }
}
<?xml version="1.0" encoding="UTF-8"?>
<user>
    <book id="123" price="17$">English</book>
    <name>lu</name>
</user>
*/
    @Test
    public void testJson_To_XML_Attributes() {
        String attributeMark = "@";
        String contentKeyName = "content";
        String jsonStr = "{\"user\":{\"name\":\"lu\",\"book\":{\"@id\":\"123\",\"@price\":\"17$\",\"content\":\"English\"}}}";
        String xmlStr = processor.convertJsonToXMLStr(jsonStr, null, attributeMark, null, contentKeyName, "UTF-8", null);
        assertThat(xmlStr, equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?><user><book id=\"123\" price=\"17$\">English</book><name>lu</name></user>"));
    }

    @Test
    public void testJson_To_XML_Invalid() {
        String jsonStr = "{\"user\":{\"name\":\"lu\",\"book\":}";
        String xmlStr = processor.convertJsonToXMLStr(jsonStr, null, null, null, null, "UTF-8", null);
        assertThat(xmlStr, equalTo(""));
    }

    @Test
    public void testJson_To_XML_NameSpace() {
        String jsonStr = "{\"user\":{\"name\":\"lu\",\"age\":30}}";
        String nameSpace = "testSpace";
        String xmlStr = processor.convertJsonToXMLStr(jsonStr, null, null, nameSpace, null, "UTF-8", null);
        assertThat(xmlStr, equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?><user xmlns:=\"testSpace\"><name>lu</name><age>30</age></user>"));
    }

    @Test
    public void testJson_To_XML_Complex() {
        String attributeMark = "@";
        String contentKeyName = "content";
        String jsonStr = "{\"glossary\":{\"title\":\"example glossary\",\"GlossDiv\":{\"title\":\"S\",\"GlossList\":{\"GlossEntry\":{\"ID\":\"SGML\",\"SortAs\":\"SGML\",\"GlossTerm\":\"Standard Generalized Markup Language\",\"Acronym\":\"SGML\",\"Abbrev\":\"ISO 8879:1986\",\"GlossDef\":{\"para\":\"A meta-markup language, used to create markup languages such as DocBook.\",\"GlossSeeAlso\":[\"GML\",\"XML\"]},\"GlossSee\":\"markup\",\"info\":{\"@name\":\"Jon\",\"@age\":12,\"content\":\"this is content\"}}}}}}";
        String xmlStr = processor.convertJsonToXMLStr(jsonStr, null, attributeMark, null, contentKeyName, "UTF-8", null);
        assertThat(xmlStr, equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?><glossary><title>example glossary</title><GlossDiv><GlossList><GlossEntry><GlossTerm>Standard Generalized Markup Language</GlossTerm><GlossSee>markup</GlossSee><SortAs>SGML</SortAs><GlossDef><para>A meta-markup language, used to create markup languages such as DocBook.</para><GlossSeeAlso>GML</GlossSeeAlso><GlossSeeAlso>XML</GlossSeeAlso></GlossDef><ID>SGML</ID><Acronym>SGML</Acronym><Abbrev>ISO 8879:1986</Abbrev><info age=\"12\" name=\"Jon\">this is content</info></GlossEntry></GlossList><title>S</title></GlossDiv></glossary>"));
    }
/*
{
    "store": {
        "book": [{
            "category": "reference",
            "author": "Nigel Rees",
            "title": "Sayings of the Century",
            "price": 8.95
        }, {
            "category": "fiction",
            "author": "Evelyn Waugh",
            "title": "Sword of Honour",
            "price": 12.99
        }],
        "bicycle": {
            "color": "red",
            "price": 19.95,
            "info": ["info1", "info2"]
        }
    }
}
*/
    private String getJsonString() {
        return "{\"store\":{\"book\":[{\"category\":\"reference\",\"author\":\"Nigel Rees\",\"title\":\"Sayings of the Century\",\"price\":8.95},{\"category\":\"fiction\",\"author\":\"Evelyn Waugh\",\"title\":\"Sword of Honour\",\"price\":12.99}],\"bicycle\":{\"color\":\"red\",\"price\":19.95,\"info\":[\"info1\",\"info2\"]}}}";
    }

    @Test
    public void testJson_To_XML_JsonPath() {
        String jsonStr = getJsonString();
        String jsonPath = "$.store.book[1]";
        String elementName = "Book";
        String xmlStr = processor.convertJsonToXMLStr(jsonStr, jsonPath, null, null, null, "UTF-8", elementName);
        assertThat(xmlStr, equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Book><category>fiction</category><title>Sword of Honour</title><author>Evelyn Waugh</author><price>12.99</price></Book>"));
    }

    @Test
    public void testJson_To_XML_JsonPath_Array() {
        String jsonStr = getJsonString();
        String jsonPath = "$.store.book[*].author";
        String elementName = "Author";
        String xmlStr = processor.convertJsonToXMLStr(jsonStr, jsonPath, null, null, null, "UTF-8", elementName);
        assertThat(xmlStr, equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?><list><Author>Nigel Rees</Author><Author>Evelyn Waugh</Author></list>"));
    }

    @Test
    public void testJson_To_XML_JsonPath_Array_2() {
        String jsonStr = getJsonString();
        String jsonPath = "$.store.bicycle";
        String elementName = "Bic";
        String xmlStr = processor.convertJsonToXMLStr(jsonStr, jsonPath, null, null, null, "UTF-8", elementName);
        assertThat(xmlStr, equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Bic><color>red</color><price>19.95</price><info>info1</info><info>info2</info></Bic>"));
    }

    @Test
    public void testJson_To_XML_JsonPath_PathNotFoundException() {
        String jsonStr = getJsonString();
        String jsonPath = "$.st";
        String elementName = "XXX";
        String xmlStr = processor.convertJsonToXMLStr(jsonStr, jsonPath, null, null, null, "UTF-8", elementName);
        assertThat(xmlStr, equalTo(""));
    }

    @Test
    public void testJson_To_XML_JsonPath_Not_Found() {
        String jsonStr = getJsonString();
        String jsonPath = "$.store.book[*]/author";
        String elementName = "XXX";
        String xmlStr = processor.convertJsonToXMLStr(jsonStr, jsonPath, null, null, null, "UTF-8", elementName);
        assertThat(xmlStr, equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?><list><XXX/></list>"));
    }

/*
{
    "bookstore": {
        "book": [{
            "@category": "cooking",
            "year": 2005,
            "author": "Giada De Laurentiis",
            "price": 30.0,
            "title": {
                "@lang": "en",
                "content": "Everyday Italian"
            }
        }]
    }
}
<?xml version="1.0" encoding="UTF-8"?>
<bookstore>
    <book category="cooking">
        <title lang="en">Everyday Italian</title>
        <year>2005</year>
        <author>Giada De Laurentiis</author>
        <price>30.0</price>
    </book>
</bookstore>

*/
    @Test
    public void testJson_To_XML_Attribute_WithOut_Content() {
        String jsonStr = "{\"bookstore\":{\"book\":[{\"@category\":\"cooking\",\"year\":2005,\"author\":\"Giada De Laurentiis\",\"price\":30.0,\"title\":{\"@lang\":\"en\",\"content\":\"Everyday Italian\"}}]}}";
        String attributeMark = "@";
        String contentKeyName = "content";
        String xmlStr = processor.convertJsonToXMLStr(jsonStr, null, attributeMark, null, contentKeyName, "UTF-8", null);
        assertThat(xmlStr, equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?><bookstore><book category=\"cooking\"><title lang=\"en\">Everyday Italian</title><year>2005</year><author>Giada De Laurentiis</author><price>30.0</price></book></bookstore>"));
    }
}
