package com.baishancloud.orchsym.processors;

public class Constant {
        private Constant(){}

        public static final String CONTENT_TYPE_JSON = "JSON";
        public static final String CONTENT_TYPE_AVRO = "AVRO";
        public static final String CONTENT_TYPE_TXT = "TXT";
        public static final String INNER_SPLIT_FLAG = ",";
        public static final String JSON_ARRAY_START = "[";
        public static final String JSON_ARRAY_END = "]";

        public static final String LOGICAL_TYPE_AND = "AND";
        public static final String LOGICAL_TYPE_OR = "OR";
        public static final String LOGICAL_TYPE_NOT = "NOT";
        public static final String JSON_SCHEMA = "{\"type\":\"array\",\"items\":{\"type\":\"object\",\"required\":[\"field\",\"function\",\"opertator\",\"value\"],\"properties\":{\"field\":{\"type\":\"string\",\"default\":\"\",\"pattern\":\"^(.*)$\"},\"function\":{\"type\":\"string\",\"default\":\"\",\"pattern\":\"^(.*)$\"},\"opertator\":{\"type\":\"string\",\"default\":\"\",\"pattern\":\"^(.*)$\"},\"value\":{\"type\":\"string\",\"default\":\"\",\"pattern\":\"^(.*)$\"}}}}";
        public static final String STRING_EMPTY = "";
        public static final String SIGN_COMMA = ",";
        public static final String DATABASE_NAME = "database";
        public static final String TABLE_NAME = "flowFile";

}
