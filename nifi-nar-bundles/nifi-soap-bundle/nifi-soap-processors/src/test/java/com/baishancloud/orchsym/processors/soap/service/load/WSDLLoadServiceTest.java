package com.baishancloud.orchsym.processors.soap.service.load;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.wsdl.Definition;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.nifi.util.file.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.baishancloud.orchsym.processors.soap.service.WSDLDoc;
import com.baishancloud.orchsym.processors.soap.service.load.WSDLLoadService;

/**
 * 
 * @author GU Guoqiang
 *
 */
public class WSDLLoadServiceTest {
    public static final File RES_FOLDER = new File("src/test/resources");

    public static final File weatherWSDLFile = new File(RES_FOLDER, "wsdl/WeatherWebService.asmx.wsdl");

    private WSDLLoadService worker;
    private File testFolder;

    @Before
    public void setup() throws IOException {
        worker = new WSDLLoadService();
        testFolder = File.createTempFile("wsdl-junit", "");
        testFolder.delete();
        testFolder.mkdirs();
    }

    @After
    public void clean() throws IOException {
        FileUtils.deleteFile(testFolder, true);
    }

    @Test
    public void test_loadURI_null() throws Exception {
        WSDLDoc wsdlDoc = worker.loadURI(null);
        assertNull(wsdlDoc);

        wsdlDoc = worker.loadURI("");
        assertNull(wsdlDoc);

        wsdlDoc = worker.loadURI("        ");
        assertNull(wsdlDoc);
    }

    @Test(expected = javax.wsdl.WSDLException.class)
    public void test_loadURI_local_nonexisted() throws Exception {
        worker.loadURI("abc.wsdl");
    }

    @Test(expected = javax.wsdl.WSDLException.class)
    public void test_loadURI_local_empty() throws Exception {
        File file = new File(testFolder, "test.txt");
        file.createNewFile();
        worker.loadURI(file.getAbsolutePath());
    }

    @Test
    public void test_loadURI_local_invalid() throws Exception {
        File file = new File(testFolder, "test.xml");

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.newDocument();
        Element root = document.createElement("root");
        for (int i = 0; i < 3; i++) {
            Element student = document.createElement("test");
            Element name = document.createElement("name"), age = document.createElement("age"), grade = document.createElement("grade");
            student.setAttribute("id", i + "");
            name.setTextContent("test" + i);
            age.setTextContent("" + i * 5);
            grade.setTextContent("" + i * 20);
            student.appendChild(name);
            student.appendChild(age);
            student.appendChild(grade);
            root.appendChild(student);
        }
        document.appendChild(root);
        TransformerFactory tff = TransformerFactory.newInstance();
        Transformer tf = tff.newTransformer();
        tf.setOutputProperty(OutputKeys.INDENT, "yes");
        tf.transform(new DOMSource(document), new StreamResult(file));

        WSDLDoc wsdlDoc = worker.loadURI(file.getAbsolutePath());
        assertNull(wsdlDoc);
    }

    @Test
    public void test_loadURI_local_xml() throws Exception {
        File file = new File(testFolder, "test.xml");
        org.apache.commons.io.FileUtils.copyFile(weatherWSDLFile, file);

        WSDLDoc wsdlDoc = worker.loadURI(file.getAbsolutePath());
        doTest_loadURI_local(wsdlDoc);
    }

    @Test
    public void test_loadURI_local_wsdl() throws Exception {
        WSDLDoc wsdlDoc = worker.loadURI(weatherWSDLFile.getAbsolutePath());
        doTest_loadURI_local(wsdlDoc);
    }

    @Test
    public void test_loadURI_local_uri() throws Exception {
        WSDLDoc wsdlDoc = worker.loadURI("file://" + weatherWSDLFile.getAbsolutePath());
        doTest_loadURI_local(wsdlDoc);
    }

    private void doTest_loadURI_local(WSDLDoc wsdlDoc) {
        assertNotNull(wsdlDoc);
        assertTrue(wsdlDoc.getDefinition() instanceof Definition);
        Definition def = (Definition) wsdlDoc.getDefinition();

        assertTrue(def.getBindings().size() > 0);
    }

    @Test(expected = javax.wsdl.WSDLException.class)
    public void test_loadURI_remote_nonexisted() throws Exception {
        worker.loadURI("http://example.org/abc.asmx");
    }

    @Test(timeout = 10000)
    public void test_loadURI_remote_wsdl11() throws Exception {
        WSDLDoc wsdlDoc = worker.loadURI("http://www.webxml.com.cn/WebServices/WeatherWebService.asmx?wsdl");
        doTest_loadURI_remote_wsdl11(wsdlDoc);
    }

    @Test(timeout = 10000)
    public void test_loadURI_remote_wsdl11_without_suffix() throws Exception {
        WSDLDoc wsdlDoc = worker.loadURI("http://www.webxml.com.cn/WebServices/WeatherWebService.asmx");
        doTest_loadURI_remote_wsdl11(wsdlDoc);
    }

    private void doTest_loadURI_remote_wsdl11(WSDLDoc wsdlDoc) {
        assertNotNull(wsdlDoc);
        assertTrue(wsdlDoc.getDefinition() instanceof Definition);
        Definition def = (Definition) wsdlDoc.getDefinition();

        assertTrue(def.getBindings().size() > 0);
    }

    @Test
    @Ignore
    public void test_loadURI_remote_wsdl20() throws Exception {
        WSDLDoc wsdlDoc = worker.loadURI("");
        assertNotNull(wsdlDoc);
        assertTrue(wsdlDoc.getDefinition() instanceof Definition);
        Definition def = (Definition) wsdlDoc.getDefinition();

        assertTrue(def.getBindings().size() > 0);
    }

    @Test
    @Ignore
    public void test_loadURI_remote_wsdl20_without_suffix() throws Exception {
        WSDLDoc wsdlDoc = worker.loadURI("");
        assertNotNull(wsdlDoc);
        assertTrue(wsdlDoc.getDefinition() instanceof Definition);
        Definition def = (Definition) wsdlDoc.getDefinition();

        assertTrue(def.getBindings().size() > 0);
    }
}
