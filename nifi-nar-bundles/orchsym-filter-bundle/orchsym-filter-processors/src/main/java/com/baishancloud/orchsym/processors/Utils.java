package com.baishancloud.orchsym.processors;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.commons.io.IOUtils;
import org.kitesdk.data.spi.JsonUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

public class Utils {
    private Utils() {
    }

    // 解析逗号分隔的字符串
    public static List<String> parseCommaDelimitedStr(String commaDelimitedStr) {
        return Arrays.asList(commaDelimitedStr.split("\\s*,\\s*"));
    }

    public static byte[] jsonToAvro(String json, String schemaStr) throws IOException {
        InputStream input = null;
        DataFileWriter<GenericRecord> writer = null;
        Encoder encoder = null;
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
            try { input.close(); }catch (Exception e) { }
        }
    }

    public static String avroToJson(byte[] avro) throws IOException {
        boolean pretty = false;
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
            encoder = EncoderFactory.get().jsonEncoder(schema, output, pretty);
            for (GenericRecord datum : streamReader) {
                writer.write(datum, encoder);
            }
            encoder.flush();
            output.flush();
            return new String(output.toByteArray());
        } finally {
            try { if (output != null) output.close(); } catch (Exception e) { }
        }
    }

    //获取Avro schema
    public static String inferAvroSchemaFromAvro(InputStream inputStream) throws IOException {
        DatumReader<GenericRecord> datumReader = new SpecificDatumReader<GenericRecord>();
        DataFileStream<GenericRecord> fileReader = new DataFileStream<GenericRecord>(inputStream, datumReader);
        Schema schema = fileReader.getSchema();
        fileReader.close();
        return schema.toString();
    }
    //获取json对应的AvroSchema
    public static String inferAvroSchemaFromJSON(String json) {
        InputStream in = new ByteArrayInputStream(json.getBytes());
        final AtomicReference<String> avroSchema = new AtomicReference<>();
        Schema as = JsonUtil.inferSchema(
                in, "orchsym_schema",
                10);
        avroSchema.set(as.toString(true));//格式和输出schema
        return avroSchema.get();
    }

    public static String jsonIncludeAndExclude(InputStream inRow, List<String> include, List<String> exclude) throws IOException {
        String jspath = IOUtils.toString(inRow, "UTF-8");
        List<String> includeList = JsonRemoveUtils.getKeysByJSON(jspath,include);
        List<String> excludeList = JsonRemoveUtils.getKeysByJSON(jspath,exclude);
        TreeSet<String> keySet = JsonRemoveUtils.getTreeSet(includeList);
        String data = jspath;
        JSONObject object = null;
        //删除指定字段
        if (include.isEmpty() && !exclude.isEmpty()) {
            if (isJsonArray(data)) {//流文件是json数组
                JSONArray dataJson = JSON.parseArray(data);
                JSONArray result = new JSONArray(dataJson.size());
                for (int i = 0; i < dataJson.size(); i++) {
                    object = JSON.parseObject(dataJson.get(i).toString());
                    JsonRemoveUtils.delJsonByKeys(object, excludeList);
                    result.add(i, object);
                }
                return result.toJSONString();
            } else {//jsonObject
                object = JSON.parseObject(data);
                JsonRemoveUtils.delJsonByKeys(object, excludeList);
                return object.toString();
            }
            //保留指定字段
        } else if (!includeList.isEmpty() && excludeList.isEmpty()){
            if (isJsonArray(data)) {//流文件是json数组
                JSONArray dataJson = JSON.parseArray(data);
                JSONArray result = new JSONArray(dataJson.size());
                for (int i = 0; i < dataJson.size(); i++) {
                    object = JSON.parseObject(dataJson.get(i).toString());
                    JsonRemoveUtils.keepJsonByKeys(keySet, object);
                    result.add(i, object);
                }
                return result.toJSONString();
            } else {//jsonObject
                object = JSON.parseObject(data);
                JsonRemoveUtils.keepJsonByKeys(keySet, object);
                return object.toString();
            }
            //先保留，再排除
        }else {
            if (isJsonArray(data)) {
                JSONArray dataJson = JSON.parseArray(data);
                JSONArray result = new JSONArray(dataJson.size());
                for (int i = 0; i < dataJson.size(); i++) {
                    object = JSON.parseObject(dataJson.get(i).toString());
                    JsonRemoveUtils.keepJsonByKeys(keySet, object);
                    JsonRemoveUtils.delJsonByKeys(object, excludeList);
                    result.add(i, object);
                }
                return result.toJSONString();
            } else {//jsonObject
                object = JSON.parseObject(data);
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
        JSONObject object = null;
        if (includeList.isEmpty() && !excludeList.isEmpty()) {
            if (isJsonArray(jsonData)) {//流文件是json数组
                JSONArray dataJson = JSON.parseArray(jsonData);
                JSONArray result = new JSONArray(dataJson.size());
                for (int i = 0; i < dataJson.size(); i++) {
                    object = JSON.parseObject(dataJson.get(i).toString());
                    JsonRemoveUtils.delJsonByKeys(object, excludeList);
                    result.add(i, object);
                }
                //获取json对应的averschema
                String stringSchema = Utils.inferAvroSchemaFromJSON(result.toJSONString());
                return Utils.jsonToAvro(result.toString(), stringSchema);
            } else {//jsonObject
                object = JSON.parseObject(jsonData);
                JsonRemoveUtils.delJsonByKeys(object, excludeList);
                //获取json对应的averschema
                String stringSchema = Utils.inferAvroSchemaFromJSON(object.toJSONString());
                return Utils.jsonToAvro(object.toString(), stringSchema);//json转Avro输出
            }
        } else if (!includeList.isEmpty() && excludeList.isEmpty()) {
            if (isJsonArray(jsonData)) {//流文件是json数组
                JSONArray dataJson = JSON.parseArray(jsonData);
                JSONArray result = new JSONArray(dataJson.size());
                for (int i = 0; i < dataJson.size(); i++) {
                    object = JSON.parseObject(dataJson.get(i).toString());
                    JsonRemoveUtils.keepJsonByKeys(keySet, object);
                    result.add(i, object);
                }
                //获取json对应的averschema
                String stringSchema = Utils.inferAvroSchemaFromJSON(result.toJSONString());
                return Utils.jsonToAvro(result.toString(), stringSchema);//json转Avro输出
            } else {//jsonObject
                object = JSON.parseObject(jsonData);
                JsonRemoveUtils.keepJsonByKeys(keySet, object);
                //获取json对应的averschema
                String stringSchema = Utils.inferAvroSchemaFromJSON(object.toJSONString());
                return Utils.jsonToAvro(object.toString(), stringSchema);//json转Avro输出
            }
        }else {//先保留，再排除
            if (isJsonArray(jsonData)) {
                JSONArray dataJson = JSON.parseArray(jsonData);
                JSONArray result = new JSONArray(dataJson.size());
                for (int i = 0; i < dataJson.size(); i++) {
                    object = JSON.parseObject(dataJson.get(i).toString());
                    JsonRemoveUtils.keepJsonByKeys(keySet, object);
                    JsonRemoveUtils.delJsonByKeys(object, excludeList);
                    result.add(i, object);
                }
                //获取json对应的averschema
                String stringSchema = Utils.inferAvroSchemaFromJSON(result.toJSONString());
                return Utils.jsonToAvro(result.toString(), stringSchema);//json转Avro输出
            } else {//jsonObject
                object = JSON.parseObject(jsonData);
                JsonRemoveUtils.keepJsonByKeys(keySet, object);
                JsonRemoveUtils.delJsonByKeys(object, excludeList);
                //获取json对应的averschema
                String stringSchema = Utils.inferAvroSchemaFromJSON(object.toJSONString());
                return Utils.jsonToAvro(object.toString(), stringSchema);//json转Avro输出
            }
        }

    }
    public static boolean isJsonArray(String data){
        return data.startsWith(Constant.JSON_ARRAY_START) && data.endsWith(Constant.JSON_ARRAY_END);
    }


}
