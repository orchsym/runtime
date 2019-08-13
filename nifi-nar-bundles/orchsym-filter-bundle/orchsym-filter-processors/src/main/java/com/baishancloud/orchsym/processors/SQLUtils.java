package com.baishancloud.orchsym.processors;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.math.BigDecimal;

public class SQLUtils {
    public static String generateInSQL(String jsonArray,String logical){
        JSONArray array = JSON.parseArray(jsonArray);
        final StringBuilder query = new StringBuilder("SELECT * FROM \"flowFile\" ");
        if (logical.equals(Constant.LOGICAL_TYPE_NOT)){
            JSONObject object = array.getJSONObject(0);
            if (object.get("function").equals("")){
                query.append("where ");
                query.append(object.get("field"));
                query.append(object.get("opertator"));
                //判断value类型
                judgeValue(object,query);
            }else {
                query.append("where ");
                query.append(SQLfunc(object.get("function").toString()));
                query.append("(");
                query.append(object.get("field"));
                query.append(")");
                query.append(object.get("opertator"));
                judgeValue(object,query);
            }
        }else if (logical.equals(Constant.LOGICAL_TYPE_AND)){
            queryAND_OR(array,"AND",query);
        }else {
            queryAND_OR(array,"OR",query);
        }
        return query.toString();
    }
    public static String generateNotInSQL(String jsonArray,String logical){
        StringBuilder notIn = new StringBuilder();
        notIn.append("select * from \"flowFile\" EXCEPT ");
        notIn.append(generateInSQL(jsonArray,logical));
        return notIn.toString();
    }
    private static void queryAND_OR(JSONArray array, String op, StringBuilder query){
        query.append("where ");
        for(int i=0;i<array.size();i++){
            if (array.getJSONObject(i).get("function").equals("")){
                query.append(array.getJSONObject(i).get("field"));
                query.append(array.getJSONObject(i).get("opertator"));
                judgeValue(array.getJSONObject(i),query);
            }else {
                query.append(SQLfunc(array.getJSONObject(i).get("function").toString()));
                query.append("(");
                query.append(array.getJSONObject(i).get("field"));
                query.append(")");
                query.append(array.getJSONObject(i).get("opertator"));
                judgeValue(array.getJSONObject(i),query);
            }
            if (i!=array.size()-1){
                query.append(" ");
                query.append(op);
                query.append(" ");
            }
        }
    }
    //判断条件中value字段是否为String类型
    private static void judgeValue(JSONObject object, StringBuilder query){
        if (isString(object.get("value").toString())){
            query.append("'");
            query.append(object.get("value"));
            query.append("'");
        }else {
            query.append(object.get("value"));
        }
    }
    private static boolean isString(String value){
        if (value.toLowerCase().equals("true") || value.toLowerCase().equals("false")){
            return false;
        }else {
            try {
                new BigDecimal(value);
            }catch (Exception e){
                return true;
            }
            return false;
        }
    }
    //将用户输入的函数名替换成calcite里的函数
    private static String SQLfunc(String func){
        String sqlFunc = null;
        switch (func){
            case "length":
                sqlFunc = "CHAR_LENGTH";
                break;
            case "log":
                sqlFunc = "LOG10";
                break;
            default:
                sqlFunc = func.toUpperCase();
        }
        return sqlFunc;
    }
}
