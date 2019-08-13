package com.baishancloud.orchsym.processors.sqlSchema;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JsonSchema extends AbstractSchema {
    private String tableData;
    private String tableName;
    Map<String, Table> table = null;
    /**
     * 创建JsonSchema.
     * @param tableName 数据表表名
     * @param tableData 数据内容
     */
    public JsonSchema(String tableName, String tableData) {
        super();
        this.tableName = tableName;
        //把对象转成数组，方便转成table
        if (!tableData.startsWith("[")) {
            this.tableData = '[' + tableData + ']';
        } else {
            this.tableData = tableData;
        }
        final ImmutableMap.Builder<String, Table> builder = ImmutableMap.builder();

        final Table table = fieldRelation();
        builder.put(tableName, table);
        this.table = builder.build();

    }

    @Override
    public String toString() {
        return "JsonSchema(tableName=" + tableName + ":tableData=" + tableData + ")";
    }

    public String gettableData() {
        return tableData;
    }

    @Override
    protected Map<String, Table> getTableMap() {
        return table;
    }

    Expression gettableDataExpression(SchemaPlus parentSchema, String name) {
        return Types.castIfNecessary(tableData.getClass(),
                Expressions.call(Schemas.unwrap(getExpression(parentSchema, name), JsonSchema.class),
                        BuiltInMethod.REFLECTIVE_SCHEMA_GET_TARGET.method));
    }

    private <T> Table fieldRelation() {
        JSONArray jsonArray = JSON.parseArray(tableData);
        return new JsonTable(jsonArray);
    }

    private static class JsonTable extends AbstractTable implements ScannableTable {
        private final JSONArray jsonArray;

        public JsonTable(JSONArray obj) {
            this.jsonArray = obj;
        }
        //获取字段类型
        public RelDataType getRowType(RelDataTypeFactory typeFactory) {

            final List<RelDataType> types = new ArrayList<RelDataType>();
            final List<String> names = new ArrayList<String>();
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            for (String string : jsonObject.keySet()) {
                final RelDataType type;
                type = typeFactory.createJavaType(jsonObject.get(string).getClass());
                names.add(string);
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
                    return new JsonEnumerator(jsonArray);
                }
            };
        }
    }

    public static class JsonEnumerator implements Enumerator<Object[]> {

        private Enumerator<Object[]> enumerator;

        public JsonEnumerator(JSONArray jsonarr) {
            List<Object[]> objs = new ArrayList<Object[]>();
            for (Object obj : jsonarr) {
                objs.add(((JSONObject) obj).values().toArray());
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
}