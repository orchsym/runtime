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
package com.baishancloud.orchsym.processors;

import com.baishancloud.orchsym.processors.sqlSchema.JsonSchema;
import com.baishancloud.orchsym.processors.sqlSchema.CsvSchema;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.calcite.config.CalciteConnectionProperty;
import org.apache.calcite.config.Lex;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.util.ConversionUtil;
import org.apache.commons.io.IOUtils;
import org.kitesdk.data.spi.JsonUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

public class Utils {
    private static Statement statement;
    private static Connection connection;
    private static CalciteConnection calciteConnection;
    private static SchemaPlus rootSchema;
    private Utils() {
    }

    // 解析逗号分隔的字符串
    public static List<String> parseCommaDelimitedStr(String commaDelimitedStr) {
        return Arrays.asList(commaDelimitedStr.split("\\s*,\\s*"));
    }
    //获取Avro schema
    private static String inferAvroSchemaFromAvro(InputStream inputStream) throws IOException {
        DatumReader<GenericRecord> datumReader = new SpecificDatumReader<GenericRecord>();
        DataFileStream<GenericRecord> fileReader = new DataFileStream<GenericRecord>(inputStream, datumReader);
        Schema schema = fileReader.getSchema();
        fileReader.close();
        return schema.toString();
    }
    public static String jsonIncludeAndExclude(InputStream inRow, List<String> include, List<String> exclude) throws IOException {
        String jspath = IOUtils.toString(inRow, "UTF-8");
        List<String> includeList = JsonRemoveUtils.getKeysByJSON(jspath,include);
        List<String> excludeList = JsonRemoveUtils.getKeysByJSON(jspath,exclude);
        TreeSet<String> keySet = JsonRemoveUtils.getTreeSet(includeList);
        String data = jspath;
        JsonObject object = null;
        //删除指定字段
        if (include.isEmpty() && !exclude.isEmpty()) {
            if (isJsonArray(data)) {//流文件是json数组
                JsonArray dataJson = new JsonParser().parse(data).getAsJsonArray();
                JsonArray result = new JsonArray(dataJson.size());
                for (int i = 0; i < dataJson.size(); i++) {
                    object = new JsonParser().parse(dataJson.get(i).toString()).getAsJsonObject();
                    JsonRemoveUtils.delJsonByKeys(object, excludeList);
                    //result.add(i, object);
                    result.add(object);
                }
                return result.getAsString();
            } else {//jsonObject
                object = new JsonParser().parse(data).getAsJsonObject();
                JsonRemoveUtils.delJsonByKeys(object, excludeList);
                return object.toString();
            }
            //保留指定字段
        } else if (!includeList.isEmpty() && excludeList.isEmpty()){
            if (isJsonArray(data)) {//流文件是json数组
                JsonArray dataJson = new JsonParser().parse(data).getAsJsonArray();
                JsonArray result = new JsonArray(dataJson.size());
                for (int i = 0; i < dataJson.size(); i++) {
                    object = new JsonParser().parse(dataJson.get(i).toString()).getAsJsonObject();
                    JsonRemoveUtils.keepJsonByKeys(keySet, object);
                    //result.add(i, object);
                    result.add(object);
                }
                return result.toString();
            } else {//jsonObject
                object = new JsonParser().parse(data).getAsJsonObject();
                JsonRemoveUtils.keepJsonByKeys(keySet, object);
                return object.toString();
            }
            //先保留，再排除
        }else {
            if (isJsonArray(data)) {
                JsonArray dataJson = new JsonParser().parse(data).getAsJsonArray();
                JsonArray result = new JsonArray(dataJson.size());
                for (int i = 0; i < dataJson.size(); i++) {
                    object = new JsonParser().parse(dataJson.get(i).toString()).getAsJsonObject();
                    JsonRemoveUtils.keepJsonByKeys(keySet, object);
                    JsonRemoveUtils.delJsonByKeys(object, excludeList);
                    result.add(object);
                }
                return result.toString();
            } else {//jsonObject
                object = new JsonParser().parse(data).getAsJsonObject();
                JsonRemoveUtils.keepJsonByKeys(keySet, object);
                JsonRemoveUtils.delJsonByKeys(object, excludeList);
                return object.toString();
            }
        }
    }

    public static byte[] avroIncludeAndExclude(InputStream inRow, List<String> include, List<String> exclude) throws IOException {
        //数据是AVRO
        String jsonData = Utils.avroToJson(IOUtils.toByteArray(inRow));//avro转JSON
        List<String> includeList = JsonRemoveUtils.getKeysByJSON(jsonData,include);
        List<String> excludeList = JsonRemoveUtils.getKeysByJSON(jsonData,exclude);
        TreeSet<String> keySet = JsonRemoveUtils.getTreeSet(includeList);
        JsonObject object = null;
        if (includeList.isEmpty() && !excludeList.isEmpty()) {
            if (isJsonArray(jsonData)) {//流文件是json数组
                JsonArray dataJson = new JsonParser().parse(jsonData).getAsJsonArray();
                JsonArray result = new JsonArray(dataJson.size());
                for (int i = 0; i < dataJson.size(); i++) {
                    object = new JsonParser().parse(dataJson.get(i).toString()).getAsJsonObject();
                    JsonRemoveUtils.delJsonByKeys(object, excludeList);
                    result.add(object);
                }
                //获取json对应的averschema
                String stringSchema = Utils.inferAvroSchemaFromJSON(result.toString());
                return Utils.jsonToAvro(result.toString(), stringSchema);
            } else {//jsonObject
                object = new JsonParser().parse(jsonData).getAsJsonObject();
                JsonRemoveUtils.delJsonByKeys(object, excludeList);
                //获取json对应的averschema
                String stringSchema = Utils.inferAvroSchemaFromJSON(object.toString());
                return Utils.jsonToAvro(object.toString(), stringSchema);//json转Avro输出
            }
        } else if (!includeList.isEmpty() && excludeList.isEmpty()) {
            if (isJsonArray(jsonData)) {//流文件是json数组
                JsonArray dataJson = new JsonParser().parse(jsonData).getAsJsonArray();
                JsonArray result = new JsonArray(dataJson.size());
                for (int i = 0; i < dataJson.size(); i++) {
                    object = new JsonParser().parse(dataJson.get(i).toString()).getAsJsonObject();
                    JsonRemoveUtils.keepJsonByKeys(keySet, object);
                    result.add(object);
                }
                //获取json对应的averschema
                String stringSchema = Utils.inferAvroSchemaFromJSON(result.toString());
                return Utils.jsonToAvro(result.toString(), stringSchema);//json转Avro输出
            } else {//jsonObject
                object = new JsonParser().parse(jsonData).getAsJsonObject();
                JsonRemoveUtils.keepJsonByKeys(keySet, object);
                //获取json对应的averschema
                String stringSchema = Utils.inferAvroSchemaFromJSON(object.toString());
                return Utils.jsonToAvro(object.toString(), stringSchema);//json转Avro输出
            }
        }else {//先保留，再排除
            if (isJsonArray(jsonData)) {
                JsonArray dataJson = new JsonParser().parse(jsonData).getAsJsonArray();
                JsonArray result = new JsonArray(dataJson.size());
                for (int i = 0; i < dataJson.size(); i++) {
                    object =dataJson.get(i).getAsJsonObject();
                    JsonRemoveUtils.keepJsonByKeys(keySet, object);
                    JsonRemoveUtils.delJsonByKeys(object, excludeList);
                    result.add(object);
                }
                //获取json对应的averschema
                String stringSchema = Utils.inferAvroSchemaFromJSON(result.toString());
                return Utils.jsonToAvro(result.toString(), stringSchema);//json转Avro输出
            } else {//jsonObject
                object = new JsonParser().parse(jsonData).getAsJsonObject();
                JsonRemoveUtils.keepJsonByKeys(keySet, object);
                JsonRemoveUtils.delJsonByKeys(object, excludeList);
                //获取json对应的averschema
                String stringSchema = Utils.inferAvroSchemaFromJSON(object.toString());
                return Utils.jsonToAvro(object.toString(), stringSchema);//json转Avro输出
            }
        }

    }
    private static boolean isJsonArray(String data){
        return data.startsWith(Constant.JSON_ARRAY_START) && data.endsWith(Constant.JSON_ARRAY_END);
    }

    public static byte[] filterRecord(InputStream inRow,String dataType,String sql) throws IOException, SQLException, ClassNotFoundException {
        if (dataType.contains(Constant.CONTENT_TYPE_JSON)){
            return JsonSQL(IOUtils.toString(inRow),sql).getBytes();
        }else if (dataType.contains(Constant.CONTENT_TYPE_TXT)){
            return CsvSQL(inRow,sql).getBytes();
        }else{
            return avroSQL(inRow,sql).getBytes();
        }
    }

    private static void init() throws ClassNotFoundException, SQLException{
        Class.forName("org.apache.calcite.jdbc.Driver");
        //修改默认编码格式为UTF16,避免中文数据查询不到
        System.setProperty("calcite.default.charset", ConversionUtil.NATIVE_UTF16_CHARSET_NAME);
        Properties properties = new Properties();
        properties.put(CalciteConnectionProperty.LEX.camelName(), Lex.MYSQL_ANSI.name());
        connection = DriverManager.getConnection("jdbc:calcite:",properties);
        calciteConnection = connection.unwrap(CalciteConnection.class);
        calciteConnection.setSchema(Constant.DATABASE_NAME);// 设置默认Schema,避免每次sql都要指定database
        rootSchema = calciteConnection.getRootSchema();
        rootSchema.setCacheEnabled(false);
        statement = connection.createStatement();
    }
    private static byte[] jsonToAvro(String json, String schemaStr) throws IOException {
        InputStream input = null;
        DataFileWriter<GenericRecord> writer = null;
        ByteArrayOutputStream output = null;
        try {
            Schema schema = new Schema.Parser().parse(schemaStr);
            DatumReader<GenericRecord> reader = new GenericDatumReader<GenericRecord>(schema);
            input = new ByteArrayInputStream(json.getBytes());
            output = new ByteArrayOutputStream();
            DataInputStream din = new DataInputStream(input);
            writer = new DataFileWriter<GenericRecord>(new GenericDatumWriter<GenericRecord>());
            writer.create(schema, output);
            Decoder decoder = DecoderFactory.get().jsonDecoder(schema, din);
            GenericRecord datum;
            while (true) {
                try {
                    datum = reader.read(null, decoder);
                } catch (EOFException eofe) {
                    break;
                }
                writer.append(datum);
            }
            writer.flush();
            return output.toByteArray();
        }finally {
            try {
                input.close();
            }catch (Exception e) {
                throw new IOException(e);
            }
        }
    }

    public static String avroToJson(byte[] avro) throws IOException {
        GenericDatumReader<GenericRecord> reader = null;
        JsonEncoder encoder = null;
        ByteArrayOutputStream output = null;
        try {
            reader = new GenericDatumReader<GenericRecord>();
            InputStream input = new ByteArrayInputStream(avro);
            DataFileStream<GenericRecord> streamReader = new DataFileStream<GenericRecord>(input, reader);
            output = new ByteArrayOutputStream();
            Schema schema = streamReader.getSchema();
            DatumWriter<GenericRecord> writer = new GenericDatumWriter<GenericRecord>(schema);
            encoder = EncoderFactory.get().jsonEncoder(schema, output, false);
            for (GenericRecord datum : streamReader) {
                writer.write(datum, encoder);
            }
            encoder.flush();
            output.flush();
            return new String(output.toByteArray());
        } finally {
            try {
                if (output != null)
                {
                    output.close();
                }
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }
    //获取json对应的AvroSchema
    private static String inferAvroSchemaFromJSON(String json) {
        InputStream in = new ByteArrayInputStream(json.getBytes());
        final AtomicReference<String> avroSchema = new AtomicReference<>();
        Schema as = JsonUtil.inferSchema(
                in, "orchsym_schema",
                10);
        //格式化输出schema
        avroSchema.set(as.toString(true));
        return avroSchema.get();
    }
    //json sql
    public static String JsonSQL(String json,String sql) throws ClassNotFoundException, SQLException {
        init();
        rootSchema.add(Constant.DATABASE_NAME, new JsonSchema(Constant.TABLE_NAME, json));
        ResultSet resultSet = statement.executeQuery(sql);
        JsonArray array = new JsonArray();
        while (resultSet.next()) {
            JsonObject jo = new JsonObject();
            int n = resultSet.getMetaData().getColumnCount();
            for (int i = 1; i <= n; i++) {
                jo.addProperty(resultSet.getMetaData().getColumnName(i), resultSet.getObject(i).toString());
            }
            array.add(jo);
        }
        resultSet.close();
        statement.close();
        connection.close();
        return array.isJsonNull()?Constant.STRING_EMPTY:array.toString();
    }
    private static String CsvSQL(InputStream csv,String sql) throws SQLException, ClassNotFoundException {
        init();
        rootSchema.add(Constant.DATABASE_NAME, new CsvSchema(Constant.TABLE_NAME, csv));
        ResultSet resultSet = statement.executeQuery(sql);
        int n = resultSet.getMetaData().getColumnCount();
        StringBuilder result =new StringBuilder();
        StringBuilder heard = new StringBuilder();
        //表头
        for (int i = 1; i <= n; i++) {
            if (i==n){
                heard.append(resultSet.getMetaData().getColumnLabel(i));
            }else {
                heard.append(resultSet.getMetaData().getColumnLabel(i)+Constant.SIGN_COMMA);
            }
        }
        result.append(heard);
        result.append("\n");
        //表内容
        while (resultSet.next()) {
            StringBuilder table = new StringBuilder();
            for (int i = 1; i <= n; i++) {
                if (i==n){
                    table.append(resultSet.getObject(i));
                }else {
                    table.append(resultSet.getObject(i)+Constant.SIGN_COMMA);
                }
            }
            result.append(table);
            result.append("\n");
        }
        resultSet.close();
        statement.close();
        connection.close();
        return result.toString();
    }
    private static String avroSQL(InputStream avro,String sql) throws IOException, SQLException, ClassNotFoundException {
        String json = avroToJson(IOUtils.toByteArray(avro));
        String query = JsonSQL(json,sql);
        //排除查不到数据时返回“[]”的情况
        if (query.length()>2){
            query = query.substring(1,query.length()-1);
            String schema = inferAvroSchemaFromJSON(query);
            return IOUtils.toString(jsonToAvro(query,schema));
        }else {
            return Constant.STRING_EMPTY;
        }

    }
}
