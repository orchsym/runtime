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
package org.apache.nifi.web;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.avro.Conversions;
import org.apache.avro.data.TimeConversions;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.DatumReader;
import org.apache.nifi.web.ViewableContent.DisplayMode;
import org.apache.nifi.stream.io.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

public class StandardContentViewerController extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(StandardContentViewerController.class);

    private static final Set<String> supportedMimeTypes = new HashSet<>();

    //1MB，最多显示的字节数，防止buffer过大造成页面卡顿。
    private final static int MAX_BUFFER_LENGTH = 1024 * 1024;

    static {
        supportedMimeTypes.add("application/json");
        supportedMimeTypes.add("application/xml");
        supportedMimeTypes.add("text/xml");
        supportedMimeTypes.add("text/plain");
        supportedMimeTypes.add("text/csv");
        supportedMimeTypes.add("application/avro-binary");
        supportedMimeTypes.add("avro/binary");
        supportedMimeTypes.add("application/avro+binary");
    }

    /**
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final ViewableContent content = (ViewableContent) request.getAttribute(ViewableContent.CONTENT_REQUEST_ATTRIBUTE);

        // handle json/xml specifically, treat others as plain text
        String contentType = content.getContentType();
        byte[] byteContent = getLimitContentBytes(content.getContentStream());
        if (supportedMimeTypes.contains(contentType)) {
            final String formatted;
            // leave the content alone if specified
            if (DisplayMode.Original.equals(content.getDisplayMode())) {
                formatted = new String(byteContent);
            } else {
                if ("application/json".equals(contentType)) {
                    // format json
                    String contentStr = new String(byteContent);
                    formatted = prettyPrintJSONAsString(contentStr);
                } else if ("application/xml".equals(contentType) || "text/xml".equals(contentType)) {
                    // format xml
                    final StringWriter writer = new StringWriter();
                    if (byteContent.length < MAX_BUFFER_LENGTH) {
                        String contentStr = new String(byteContent);
                        try {
                            final StreamSource source = new StreamSource(new StringReader(contentStr));
                            final StreamResult result = new StreamResult(writer);

                            final TransformerFactory transformFactory = TransformerFactory.newInstance();
                            final Transformer transformer = transformFactory.newTransformer();
                            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

                            transformer.transform(source, result);
                        } catch (final TransformerFactoryConfigurationError | TransformerException te) {
                            throw new IOException("Unable to transform content as XML: " + te, te);
                        }
                        // get the transformed xml
                        formatted = writer.toString();
                    } else {
                        //ontentStr's length larger than expected, do not transform to xml
                        formatted = new String(byteContent);
                    }
                } else if ("application/avro-binary".equals(contentType) || "avro/binary".equals(contentType) || "application/avro+binary".equals(contentType)) {
                    final StringBuilder sb = new StringBuilder();
                    sb.append("[");
                    // Use Avro conversions to display logical type values in human readable way.
                    final GenericData genericData = new GenericData(){
                        @Override
                        protected void toString(Object datum, StringBuilder buffer) {
                            // Since these types are not quoted and produce a malformed JSON string, quote it here.
                            if (datum instanceof LocalDate || datum instanceof LocalTime || datum instanceof DateTime) {
                                buffer.append("\"").append(datum).append("\"");
                                return;
                            }
                            super.toString(datum, buffer);
                        }
                    };
                    genericData.addLogicalTypeConversion(new Conversions.DecimalConversion());
                    genericData.addLogicalTypeConversion(new TimeConversions.DateConversion());
                    genericData.addLogicalTypeConversion(new TimeConversions.TimeConversion());
                    genericData.addLogicalTypeConversion(new TimeConversions.TimestampConversion());
                    final DatumReader<GenericData.Record> datumReader = new GenericDatumReader<>(null, null, genericData);
                    if (byteContent.length < MAX_BUFFER_LENGTH) {
                        try (final DataFileStream<GenericData.Record> dataFileReader = new DataFileStream<>(new ByteArrayInputStream(byteContent), datumReader)) {
                            while (dataFileReader.hasNext()) {
                                final GenericData.Record record = dataFileReader.next();
                                final String formattedRecord = genericData.toString(record);
                                sb.append(formattedRecord);
                                sb.append(",");
                            }
                        }
                        if (sb.length() > 1) {
                            sb.deleteCharAt(sb.length() - 1);
                        }
                        sb.append("]");
                        final String json = sb.toString();

                        final ObjectMapper mapper = new ObjectMapper();
                        final Object objectJson = mapper.readValue(json, Object.class);
                        formatted = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectJson);
                    } else {
                        //contentStr's length larger than expected, do not transform to json
                        formatted = new String(byteContent);;
                    }
                    contentType = "application/json";
                } else {
                    // leave plain text alone when formatting
                    formatted = new String(byteContent);
                }
            }

            // defer to the jsp
            request.setAttribute("mode", contentType);
            request.setAttribute("content", formatted);
            request.getRequestDispatcher("/WEB-INF/jsp/codemirror.jsp").include(request, response);
        } else {
            final PrintWriter out = response.getWriter();
        }
    }

    private byte[] getLimitContentBytes(InputStream inputStream) {

        byte[] bytes = null;
        try {
            final byte[] buffer = new byte[MAX_BUFFER_LENGTH];
            final int read = StreamUtils.fillBuffer(inputStream, buffer, false);
            // trim the byte array if necessary
            bytes = buffer;
            if (read != buffer.length) {
                bytes = new byte[read];
                System.arraycopy(buffer, 0, bytes, 0, read);
            }
        } catch (Exception e) {
            logger.error("fail to getLimitContentBytes: ", e);
        }
        return bytes;
    }

    public String prettyPrintJSONAsString(String jsonString) {
        int tabCount = 0;
        StringBuffer prettyPrintJson = new StringBuffer();
        String lineSeparator = "\r\n";
        String tab = "  ";
        boolean ignoreNext = false;
        boolean inQuote = false;
        char character;
        //遍历每个字符，格式化输出
        for (int i = 0; i < jsonString.length(); i++) {
            character = jsonString.charAt(i);
            if (inQuote) {
                if (ignoreNext) {
                    ignoreNext = false;
                } else if (character == '"') {
                    inQuote = !inQuote;
                }
                prettyPrintJson.append(character);
            } else {
                if (ignoreNext ? ignoreNext = !ignoreNext : ignoreNext);
                switch (character) {
                case '[':
                    ++tabCount;
                    prettyPrintJson.append(character);
                    prettyPrintJson.append(lineSeparator);
                    printIndent(tabCount, prettyPrintJson, tab);
                    break;
                case ']':
                    --tabCount;
                    prettyPrintJson.append(lineSeparator);
                    printIndent(tabCount, prettyPrintJson, tab);
                    prettyPrintJson.append(character);
                    break;
                case '{':
                    ++tabCount;
                    prettyPrintJson.append(character);
                    prettyPrintJson.append(lineSeparator);
                    printIndent(tabCount, prettyPrintJson, tab);
                    break;
                case '}':
                    --tabCount;
                    prettyPrintJson.append(lineSeparator);
                    printIndent(tabCount, prettyPrintJson, tab);
                    prettyPrintJson.append(character);
                    break;
                case '"':
                    inQuote = !inQuote;
                    prettyPrintJson.append(character);
                    break;

                case ',':
                    prettyPrintJson.append(character);
                    prettyPrintJson.append(lineSeparator);
                    printIndent(tabCount, prettyPrintJson, tab);
                    break;
                case ':':
                    prettyPrintJson.append(character + " ");
                    break;
                case '\\':
                    prettyPrintJson.append(character);
                    ignoreNext = true;
                    break;
                default:
                    prettyPrintJson.append(character);
                    break;
                }
            }
        }
        return prettyPrintJson.toString();
    }

    private void printIndent(int count, StringBuffer stringBuffer, String indent) {
        for (int i = 0; i < count; i++) {
            stringBuffer.append(indent);
        }
    }
}
