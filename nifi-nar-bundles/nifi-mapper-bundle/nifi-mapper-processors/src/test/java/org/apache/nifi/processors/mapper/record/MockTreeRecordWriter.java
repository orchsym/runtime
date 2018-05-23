package org.apache.nifi.processors.mapper.record;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.nifi.avro.AvroTypeUtil;
import org.apache.nifi.serialization.record.MockRecordWriter;
import org.apache.nifi.serialization.record.Record;

/**
 * 
 * @author GU Guoqiang
 * 
 */
public class MockTreeRecordWriter extends MockRecordWriter {
    private Schema schema;

    public MockTreeRecordWriter(String header, Schema schema) {
        super(header, false);
        this.schema = schema;
    }

    public MockTreeRecordWriter(String header) {
        this(header, null);
    }

    @Override
    protected void writeRecord(OutputStream out, Record record) throws IOException {
        if (schema != null) {
            final GenericRecord avroRecord = AvroTypeUtil.createAvroRecord(record, schema);
            out.write(GenericData.get().toString(avroRecord).getBytes(StandardCharsets.UTF_8));
        } else {
            out.write(GenericData.get().toString(record).getBytes(StandardCharsets.UTF_8));
        }
        out.write("\n".getBytes());
    }

}
