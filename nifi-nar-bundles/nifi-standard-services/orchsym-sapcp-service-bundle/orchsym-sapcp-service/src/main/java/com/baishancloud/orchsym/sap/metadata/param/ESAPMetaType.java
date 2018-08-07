package com.baishancloud.orchsym.sap.metadata.param;

import java.math.BigDecimal;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.sap.conn.jco.JCoMetaData;
import com.sap.conn.jco.JCoStructure;
import com.sap.conn.jco.JCoTable;
import com.sap.conn.jco.rt.AbstractMetaData;

/**
 * @author GU Guoqiang
 *
 */
public enum ESAPMetaType {

    CHAR(JCoMetaData.TYPE_CHAR, String.class), // 0
    DATE(JCoMetaData.TYPE_DATE, -1, 8, Date.class), // 1
    BCD(JCoMetaData.TYPE_BCD, BigDecimal.class), // 2
    TIME(JCoMetaData.TYPE_TIME, -1, 6, Date.class), // 3
    BYTE(JCoMetaData.TYPE_BYTE, byte[].class), // 4
    NUM(JCoMetaData.TYPE_NUM, String.class), // 6
    FLOAT(JCoMetaData.TYPE_FLOAT, 8, 4, Double.class), // 7
    INT(JCoMetaData.TYPE_INT, 4, 2, Integer.class), // 8
    INT2(JCoMetaData.TYPE_INT2, 2, 1, Integer.class), // 9
    INT1(JCoMetaData.TYPE_INT1, 1, 1, Integer.class), // 10
    STRUCTURE(JCoMetaData.TYPE_STRUCTURE, -1, 0, JCoStructure.class), // 17
    DECF16(JCoMetaData.TYPE_DECF16, 8, 4, BigDecimal.class), // 23
    DECF34(JCoMetaData.TYPE_DECF34, 16, 8, BigDecimal.class), // 24
    STRING(JCoMetaData.TYPE_STRING, 8, 0, String.class), // 29
    XSTRING(JCoMetaData.TYPE_XSTRING, 8, 0, byte[].class), // 30
    EXCEPTION(JCoMetaData.TYPE_EXCEPTION, Object.class), // 98 // object??
    TABLE(JCoMetaData.TYPE_TABLE, -1, 0, JCoTable.class), // 99

    // INVALID(JCoMetaData.TYPE_INVALID), // -1
    ITAB(JCoMetaData.TYPE_ITAB, Object.class/* ? */), // 5
    ;

    /*
     * JCoMetaData.TYPE_XXX
     */
    private final int jcoType;
    /*
     * calc or fix the value like AbstractMetaData.add
     */
    private final int fixByteLength;

    /**
     * for AbstractMetaData.getJavaBufferLength
     */
    private final int fixJavaLength;

    /*
     * @see getClassNameOfField of class AbstractMetaData
     */
    private final Class<?> javaType;

    private ESAPMetaType(int type, Class<?> javaType) {
        this(type, -1, -1, javaType);
    }

    private ESAPMetaType(int type, int byteLength, int javaLength, Class<?> javaType) {
        this.jcoType = type;
        this.fixByteLength = byteLength;
        this.fixJavaLength = javaLength;
        this.javaType = javaType;
    }

    public int getJCoType() {
        return this.jcoType;
    }

    public int getFixByteLength() {
        return fixByteLength;
    }

    public int getFixJavaLength() {
        return fixJavaLength;
    }

    /**
     * same as AbstractMetaData.getLength
     */
    public int getLength(int nucbyteLength) {
        if (this == ESAPMetaType.ITAB || this == ESAPMetaType.STRUCTURE || this == ESAPMetaType.TABLE // 5,17,99
                || this == ESAPMetaType.STRING || this == ESAPMetaType.XSTRING // 29,30
        ) {
            return 0;
        }
        return getByteLength(nucbyteLength); // same as byteLength
    }

    /**
     * same as AbstractMetaData.add to deal with the fieldType
     */
    public int getByteLength(int nucbyteLength) {
        if (this == ESAPMetaType.FLOAT // 7
                || this == ESAPMetaType.INT || this == ESAPMetaType.INT2 || this == ESAPMetaType.INT1 // 8,9,10
                || this == ESAPMetaType.DECF16 || this == ESAPMetaType.DECF34 // 23,24
                || this == ESAPMetaType.STRING || this == ESAPMetaType.XSTRING) { // 29,30
            return getFixByteLength();
        }
        return nucbyteLength;
    }

    /**
     * same as AbstractMetaData.add to deal with the fieldType
     */
    public int getJavaLength(int nucbyteLength) {
        if (this == ESAPMetaType.CHAR || this == ESAPMetaType.NUM // 0,6
                || this == ESAPMetaType.ITAB || this == ESAPMetaType.EXCEPTION) { // 5,98
            return nucbyteLength;
        } else if (this == ESAPMetaType.BCD || this == ESAPMetaType.BYTE) {// 2,4
            return (nucbyteLength / 2 + nucbyteLength % 2);
        }
        return getFixJavaLength(); // use the set one
    }

    public Class<?> getJavaType() {
        return javaType;
    }

    public String getName() {
        return this.name();
    }

    /**
     * maybe only work for test to create lower case type.
     */
    public String getLowerName() {
        return getName().toLowerCase();
    }

    public static ESAPMetaType get(String name) {
        if (StringUtils.isNotBlank(name)) {
            for (ESAPMetaType t : ESAPMetaType.values()) {
                if (t.getName().equalsIgnoreCase(name))
                    return t;
            }
        }
        return ESAPMetaType.CHAR; // default, same as AbstractMetaData.getJCOTypeString();
    }

    public static ESAPMetaType get(int jcoType) {
        // for(ESAPMetaType t:ESAPMetaType.values()) {
        // if(t.getJCoType()==jcoType)
        // return t;
        // }
        String typeName = AbstractMetaData.getJCOTypeString(jcoType); // reuse JCo api
        return ESAPMetaType.valueOf(typeName);
    }
}
