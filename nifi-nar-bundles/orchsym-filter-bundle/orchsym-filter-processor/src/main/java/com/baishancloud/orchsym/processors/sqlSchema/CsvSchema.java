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
package com.baishancloud.orchsym.processors.sqlSchema;

import com.google.common.collect.ImmutableMap;
import org.apache.calcite.DataContext;
import org.apache.calcite.linq4j.AbstractEnumerable;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.linq4j.Linq4j;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.linq4j.tree.Expressions;
import org.apache.calcite.linq4j.tree.Types;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.ScannableTable;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Schemas;
import org.apache.calcite.schema.Statistic;
import org.apache.calcite.schema.Statistics;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.util.BuiltInMethod;
import org.apache.calcite.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvSchema extends AbstractSchema {
    private InputStream tableData;
    private String tableName;
    Map<String, Table> table = null;
    /**
     * 创建JsonSchema.
     * @param tableName 数据表表名
     * @param tableData 数据内容
     */
    public CsvSchema(String tableName, InputStream tableData) {
        super();
        this.tableName = tableName;
        this.tableData = tableData;
        final ImmutableMap.Builder<String, Table> builder = ImmutableMap.builder();

        final Table table = fieldRelation();
        builder.put(tableName, table);
        this.table = builder.build();

    }

    @Override
    public String toString() {
        return "csvSchema(tableName=" + tableName + ":tableData=" + tableData + ")";
    }

    public InputStream gettableData() {
        return tableData;
    }

    @Override
    protected Map<String, Table> getTableMap() {
        return table;
    }


    Expression gettableDataExpression(SchemaPlus parentSchema, String name) {
        return Types.castIfNecessary(tableData.getClass(),
                Expressions.call(Schemas.unwrap(getExpression(parentSchema, name), CsvSchema.class),
                        BuiltInMethod.REFLECTIVE_SCHEMA_GET_TARGET.method));
    }

    private <T> Table fieldRelation() {
        String[] array = toArrayByInputStreamReader(tableData);
        return new CsvSchema.csvTable(array);
    }

    private static class csvTable extends AbstractTable implements ScannableTable {
        private final String[] array;

        public csvTable(String[] array) {
            this.array = array;
        }
        //获取字段类型
        public RelDataType getRowType(RelDataTypeFactory typeFactory) {
            final List<RelDataType> types = new ArrayList<RelDataType>();
            final List<String> names = new ArrayList<String>();
            ArrayList heads = csvToArray(array[0]);
            ArrayList bodys = csvToArray(array[1]);
            for (int i=0;i<heads.size();i++) {
                final RelDataType type;
                type = typeFactory.createJavaType(getDataType(bodys.get(i).toString()));
                names.add(heads.get(i).toString());
                types.add(type);
            }
            if (names.isEmpty()) {
                names.add("line");
                types.add(typeFactory.createJavaType(String.class));
            }
            return typeFactory.createStructType(Pair.zip(names, types));
        }

        public Statistic getStatistic() {
            return Statistics.UNKNOWN;
        }

        public Enumerable<Object[]> scan(DataContext root) {
            return new AbstractEnumerable<Object[]>() {
                public Enumerator<Object[]> enumerator() {
                    return new CsvSchema.csvEnumerator(array);
                }
            };
        }
    }

    public static class csvEnumerator implements Enumerator<Object[]> {

        private Enumerator<Object[]> enumerator;

        public csvEnumerator(String[] array) {
            List<Object[]> objs = new ArrayList<Object[]>();
            for (int i=1;i<array.length;i++) {
                objs.add(csvToArray(array[i]).toArray());
            }
            enumerator = Linq4j.enumerator(objs);
        }

        public Object[] current() {
            return enumerator.current();
        }

        public boolean moveNext() {
            return enumerator.moveNext();
        }

        public void reset() {
            enumerator.reset();
        }

        public void close() {
            enumerator.close();
        }

    }

    public static String[] toArrayByInputStreamReader(InputStream in){
        // 使用ArrayList来存储每行读取到的字符串
        ArrayList<String> arrayList = new ArrayList<>();
        try {
            //将字节流转化成字符流，并指定字符集
            InputStreamReader inputReader = new InputStreamReader(in,"UTF-8");
            BufferedReader bf = new BufferedReader(inputReader);
            // 按行读取字符串
            String str;
            while ((str = bf.readLine()) != null) {
                arrayList.add(str);
            }
            bf.close();
            inputReader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //对ArrayList中存储的字符串进行处理
        int length = arrayList.size();
        String[] array = new String[length];
        for (int i = 0; i < length; i++) {
            String s = arrayList.get(i);
            array[i] = s;
        }
        return array;
    }
    // 把CSV文件的一行转换成ArrayList。
    public static ArrayList csvToArray(String source) {
        if (source == null || source.length() == 0) {
            return new ArrayList();
        }
        int currentPosition = 0;
        int maxPosition = source.length();
        int nextComma = 0;
        ArrayList rtnArray = new ArrayList();
        while (currentPosition < maxPosition) {
            nextComma = nextComma(source, currentPosition);
            rtnArray.add(nextToken(source, currentPosition, nextComma));
            currentPosition = nextComma + 1;
            if (currentPosition == maxPosition) {
                rtnArray.add("");
            }
        }
        return rtnArray;
    }
    private static int nextComma(String source, int st) {
        int maxPosition = source.length();
        boolean inquote = false;
        while (st < maxPosition) {
            char ch = source.charAt(st);
            if (!inquote && ch == ',') {
                break;
            } else if ('"' == ch) {
                inquote = !inquote;
            }
            st++;
        }
        return st;
    }
    private static Object nextToken(String source, int st, int nextComma) {
        StringBuffer strb = new StringBuffer();
        int next = st;
        while (next < nextComma) {
            char ch = source.charAt(next++);
            if (ch == '"') {
                if ((st + 1 < next && next < nextComma) && (source.charAt(next) == '"')) {
                    strb.append(ch);
                    next++;
                }
            } else {
                strb.append(ch);
            }
        }
        if(getDataType(strb.toString()).equals(String.class)){
            return strb.toString();
        }else if (getDataType(strb.toString()).equals(Boolean.class)){
            return Boolean.parseBoolean(strb.toString());
        }else {
            return new BigDecimal(strb.toString());
        }
    }
    private static <T> T getDataType(String value){
        if (value.toLowerCase().equals("true") || value.toLowerCase().equals("false")){
            return (T) Boolean.class;
        }else {
            try {
                new BigDecimal(value);
            }catch (Exception e){
                return (T) String.class;
            }
            return (T) BigDecimal.class;
        }
    }
}