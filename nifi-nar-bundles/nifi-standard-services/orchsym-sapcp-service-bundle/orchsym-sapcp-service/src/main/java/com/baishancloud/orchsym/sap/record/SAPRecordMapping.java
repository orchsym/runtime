package com.baishancloud.orchsym.sap.record;

import static com.sap.conn.jco.JCoMetaData.TYPE_BCD;
import static com.sap.conn.jco.JCoMetaData.TYPE_BYTE;
import static com.sap.conn.jco.JCoMetaData.TYPE_CHAR;
import static com.sap.conn.jco.JCoMetaData.TYPE_DATE;
import static com.sap.conn.jco.JCoMetaData.TYPE_DECF16;
import static com.sap.conn.jco.JCoMetaData.TYPE_DECF34;
import static com.sap.conn.jco.JCoMetaData.TYPE_EXCEPTION;
import static com.sap.conn.jco.JCoMetaData.TYPE_FLOAT;
import static com.sap.conn.jco.JCoMetaData.TYPE_INT;
import static com.sap.conn.jco.JCoMetaData.TYPE_INT1;
import static com.sap.conn.jco.JCoMetaData.TYPE_INT2;
import static com.sap.conn.jco.JCoMetaData.TYPE_INVALID;
import static com.sap.conn.jco.JCoMetaData.TYPE_ITAB;
import static com.sap.conn.jco.JCoMetaData.TYPE_NUM;
import static com.sap.conn.jco.JCoMetaData.TYPE_STRING;
import static com.sap.conn.jco.JCoMetaData.TYPE_STRUCTURE;
import static com.sap.conn.jco.JCoMetaData.TYPE_TABLE;
import static com.sap.conn.jco.JCoMetaData.TYPE_TIME;
import static com.sap.conn.jco.JCoMetaData.TYPE_XSTRING;

import org.apache.nifi.serialization.record.DataType;
import org.apache.nifi.serialization.record.RecordFieldType;

/**
 * 
 * @author GU Guoqiang
 *
 */
public final class SAPRecordMapping {

    public static RecordFieldType mapFieldType(int sapType) {
        switch (sapType) {
        case TYPE_BYTE:// Raw data, binary, fixed length, zero padded.
            // return RecordFieldType.BYTE;
            return RecordFieldType.STRING;
        case TYPE_CHAR:// 1-byte or multibyte character.Fixed sized, blank padded.
            // return RecordFieldType.CHAR;
            return RecordFieldType.STRING;
        case TYPE_DATE:// Date (YYYYYMMDD).
        case TYPE_TIME:// Time (HHMMSS).
            return RecordFieldType.DATE;
        case TYPE_BCD:// Packed BCD number, any length between 1 and 16 bytes.
        case TYPE_DECF16:// decimal floating point.
        case TYPE_DECF34:// decimal floating point.
        case TYPE_FLOAT:// Floating point,double precission.
            return RecordFieldType.DOUBLE;
        case TYPE_INT1:// 1-byte integer .
            return RecordFieldType.SHORT;
        case TYPE_INT2:// 2-byte integer .
            return RecordFieldType.INT;
        case TYPE_INT:// 4-byte integer .
            return RecordFieldType.BIGINT;
        case TYPE_NUM:// Digits, fixed size,'0' padded.
        case TYPE_STRING:// UTF8 encoded string of variable length.
        case TYPE_XSTRING:// Byte array of variable length.
            return RecordFieldType.STRING;

        case TYPE_TABLE:// A JCoTable.
        case TYPE_ITAB:// Internal table.???
            return RecordFieldType.RECORD;
        case TYPE_STRUCTURE:// A heterogeneous structure.
            return RecordFieldType.MAP;// ???
        case TYPE_EXCEPTION:// ABAP exception.
        case TYPE_INVALID:// The field info does not contain a valid JCO type
            break;
        }
        return null;
    }

    public static DataType map(int sapType) {
        RecordFieldType type = mapFieldType(sapType);
        if (type != null) {
            return type.getDataType();
        }
        return null;
    }
}
