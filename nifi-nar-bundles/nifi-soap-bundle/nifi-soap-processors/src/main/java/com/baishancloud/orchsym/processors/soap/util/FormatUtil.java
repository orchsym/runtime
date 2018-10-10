package com.baishancloud.orchsym.processors.soap.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;

/**
 * @author GU Guoqiang
 *
 */
public class FormatUtil {

    public static void formatXML(Document doc, OutputStream outStream) throws IOException {
        try {
            Transformer tf = TransformerFactory.newInstance().newTransformer();
            tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            tf.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
            tf.setOutputProperty(OutputKeys.INDENT, "yes");
            tf.transform(new DOMSource(doc), new StreamResult(outStream));

            // OutputFormat formater = OutputFormat.createPrettyPrint();
            // // formater=OutputFormat.createCompactFormat();
            // formater.setEncoding(StandardCharsets.UTF_8.name());
            // formater.setOmitEncoding(true);
            // XMLWriter writer = new XMLWriter(out, formater);
            // writer.write(doc);
            // writer.close();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static void formatJson(Object json, OutputStream outStream) throws IOException {
        final String result = formatJson(json);
        IOUtils.write(result, outStream);
    }

    public static String formatJson(Object json) throws IOException {
        final int indentFactor = 4;
        if (json instanceof JSONObject) {
            return ((JSONObject) json).toString(indentFactor);
        } else if (json instanceof JSONArray) {
            return ((JSONArray) json).toString(indentFactor);
        }
        return json.toString();
    }
}
