package com.baishancloud.orchsym.processors.sqlSchema;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        JsonArray jsonArray = new JsonParser().parse(tableData).getAsJsonArray();
        return new JsonTable(jsonArray);
    }

    private static class JsonTable extends AbstractTable implements ScannableTable {
        private final JsonArray jsonArray;

        public JsonTable(JsonArray obj) {
            this.jsonArray = obj;
        }
        //获取字段类型
        public RelDataType getRowType(RelDataTypeFactory typeFactory) {

            final List<RelDataType> types = new ArrayList<RelDataType>();
            final List<String> names = new ArrayList<String>();
            JsonObject jsonObject = jsonArray.get(0).getAsJsonObject();
            for (String string : jsonObject.keySet()) {
                final RelDataType type;
                type = typeFactory.createJavaType(getDataType(jsonObject.get(string).getAsString()));
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

        public JsonEnumerator(JsonArray jsonarr) {
            List<Object[]> objs = new ArrayList<Object[]>();
            for (JsonElement obj : jsonarr) {
                objs.add(jsonTree(obj).toArray());
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
    private static ArrayList jsonTree(JsonElement e)
    {
        ArrayList array =new ArrayList<>();
        if (e.isJsonObject())
        {
            Set<Map.Entry<String, JsonElement>> es = e.getAsJsonObject().entrySet();
            for (Map.Entry<String, JsonElement> en : es)
            {
                if(getDataType(en.getValue().getAsString()).equals(String.class)){
                    array.add(en.getValue().getAsString());
                }else if (getDataType(en.getValue().getAsString()).equals(Boolean.class)){
                    array.add(Boolean.parseBoolean(en.getValue().getAsString()));
                }else {
                    array.add(new BigDecimal(en.getValue().getAsString()));
                }
            }
        }
        return array;
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