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
package com.baishancloud.orchsym.processors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.math.BigDecimal;

public class SQLUtils {
    public static String generateInSQL(String jsonArray,String logical){
        JsonArray array = new JsonParser().parse(jsonArray).getAsJsonArray();
        final StringBuilder query = new StringBuilder("SELECT * FROM \"flowFile\" ");
        if (logical.equals(Constant.LOGICAL_TYPE_NOT)){
            JsonObject object = array.get(0).getAsJsonObject();
            if (object.get("function").getAsString().isEmpty()){
                query.append("where ");
                query.append(object.get("field").getAsString());
                query.append(object.get("opertator").getAsString());
                //判断value类型
                judgeValue(object,query);
            }else {
                query.append("where ");
                query.append(SQLfunc(object.get("function").getAsString()));
                query.append("(");
                query.append(object.get("field").getAsString());
                query.append(")");
                query.append(object.get("opertator").getAsString());
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
    private static void queryAND_OR(JsonArray array, String op, StringBuilder query){
        query.append("where ");
        for(int i=0;i<array.size();i++){
            if (array.get(i).getAsJsonObject().get("function").getAsString().isEmpty()){
                query.append(array.get(i).getAsJsonObject().get("field").getAsString());
                query.append(array.get(i).getAsJsonObject().get("opertator").getAsString());
                judgeValue(array.get(i).getAsJsonObject(),query);
            }else {
                query.append(SQLfunc(array.get(i).getAsJsonObject().get("function").getAsString()));
                query.append("(");
                query.append(array.get(i).getAsJsonObject().get("field").getAsString());
                query.append(")");
                query.append(array.get(i).getAsJsonObject().get("opertator").getAsString());
                judgeValue(array.get(i).getAsJsonObject(),query);
            }
            if (i!=array.size()-1){
                query.append(" ");
                query.append(op);
                query.append(" ");
            }
        }
    }
    //判断条件中value字段是否为String类型
    private static void judgeValue(JsonObject object, StringBuilder query){
        if (isString(object.get("value").getAsString())){
            query.append("'");
            query.append(object.get("value").getAsString());
            query.append("'");
        }else {
            query.append(object.get("value").getAsString());
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
