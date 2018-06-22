package org.apache.nifi.processors.mapper.avro;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.jackson.Base64Variant;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonStreamContext;
import org.codehaus.jackson.ObjectCodec;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DelegateJsonGenerator extends JsonGenerator {
    private final com.fasterxml.jackson.core.JsonGenerator delegate;

    public DelegateJsonGenerator(com.fasterxml.jackson.core.JsonGenerator delegate) {
        super();
        this.delegate = delegate;
    }

    private com.fasterxml.jackson.core.JsonGenerator.Feature convert(Feature f) {
        return null;
    }

    @Override
    public JsonGenerator enable(Feature f) {
        // return delegate.enable(convert(f));
        return this;
    }

    @Override
    public JsonGenerator disable(Feature f) {
        // return delegate.disable(f);
        return this;
    }

    @Override
    public boolean isEnabled(Feature f) {
        // return delegate.isEnabled(f);
        return false;
    }

    @Override
    public JsonGenerator setCodec(ObjectCodec oc) {
        // return delegate.setCodec(oc);
        return this;
    }

    @Override
    public ObjectCodec getCodec() {
        // return delegate.getCodec();
        return null;
    }

    @Override
    public JsonGenerator useDefaultPrettyPrinter() {
        delegate.useDefaultPrettyPrinter();
        return this;
    }

    @Override
    public void writeStartArray() throws IOException, JsonGenerationException {
        delegate.writeStartArray();
    }

    @Override
    public void writeEndArray() throws IOException, JsonGenerationException {
        delegate.writeEndArray();
    }

    @Override
    public void writeStartObject() throws IOException, JsonGenerationException {
        delegate.writeStartObject();
    }

    @Override
    public void writeEndObject() throws IOException, JsonGenerationException {
        delegate.writeEndObject();
    }

    @Override
    public void writeFieldName(String name) throws IOException, JsonGenerationException {
        delegate.writeFieldName(name);
    }

    @Override
    public void writeString(String text) throws IOException, JsonGenerationException {
        delegate.writeString(text);
    }

    @Override
    public void writeString(char[] text, int offset, int len) throws IOException, JsonGenerationException {
        delegate.writeString(text, offset, len);
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int length) throws IOException, JsonGenerationException {
        delegate.writeRawUTF8String(text, offset, length);
    }

    @Override
    public void writeUTF8String(byte[] text, int offset, int length) throws IOException, JsonGenerationException {
        delegate.writeUTF8String(text, offset, length);
    }

    @Override
    public void writeRaw(String text) throws IOException, JsonGenerationException {
        delegate.writeRaw(text);
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws IOException, JsonGenerationException {
        delegate.writeRaw(text, offset, len);
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws IOException, JsonGenerationException {
        delegate.writeRaw(text, offset, len);

    }

    @Override
    public void writeRaw(char c) throws IOException, JsonGenerationException {
        delegate.writeRaw(c);

    }

    @Override
    public void writeRawValue(String text) throws IOException, JsonGenerationException {
        delegate.writeRawValue(text);
    }

    @Override
    public void writeRawValue(String text, int offset, int len) throws IOException, JsonGenerationException {
        delegate.writeRawValue(text, offset, len);
    }

    @Override
    public void writeRawValue(char[] text, int offset, int len) throws IOException, JsonGenerationException {
        delegate.writeRawValue(text, offset, len);
    }

    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws IOException, JsonGenerationException {
        // delegate.writeBinary(b64variant, data, offset, len);
    }

    @Override
    public void writeNumber(int v) throws IOException, JsonGenerationException {
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(long v) throws IOException, JsonGenerationException {
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException, JsonGenerationException {
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(double d) throws IOException, JsonGenerationException {
        delegate.writeNumber(d);
    }

    @Override
    public void writeNumber(float f) throws IOException, JsonGenerationException {
        delegate.writeNumber(f);
    }

    @Override
    public void writeNumber(BigDecimal dec) throws IOException, JsonGenerationException {
        delegate.writeNumber(dec);
    }

    @Override
    public void writeNumber(String encodedValue) throws IOException, JsonGenerationException, UnsupportedOperationException {
        delegate.writeNumber(encodedValue);
    }

    @Override
    public void writeBoolean(boolean state) throws IOException, JsonGenerationException {
        delegate.writeBoolean(state);
    }

    @Override
    public void writeNull() throws IOException, JsonGenerationException {
        delegate.writeNull();
    }

    @Override
    public void writeObject(Object pojo) throws IOException, JsonProcessingException {
        delegate.writeObject(pojo);
    }

    @Override
    public void writeTree(JsonNode rootNode) throws IOException, JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        final com.fasterxml.jackson.databind.JsonNode treeNode = mapper.readTree(rootNode.toString());
        delegate.writeTree(treeNode);
    }

    @Override
    public void copyCurrentEvent(JsonParser jp) throws IOException, JsonProcessingException {
        // delegate.copyCurrentEvent(jp);
    }

    @Override
    public void copyCurrentStructure(JsonParser jp) throws IOException, JsonProcessingException {
        // delegate.copyCurrentStructure(jp);
    }

    @Override
    public JsonStreamContext getOutputContext() {
        // return delegate.getOutputContext();
        return null;
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

}
