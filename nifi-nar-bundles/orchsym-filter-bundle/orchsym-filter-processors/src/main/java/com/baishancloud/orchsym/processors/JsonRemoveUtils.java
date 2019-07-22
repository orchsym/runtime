package com.baishancloud.orchsym.processors;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


public class JsonRemoveUtils {
    public JsonRemoveUtils(){}

    //递归删除指定字段
    private static void deleteJsonProperty(Object outterObject, String source) {
        if(outterObject==null|| source==null||"".equals(source))
            return;
        if (outterObject instanceof JSONObject) {
            JSONObject tempJson = (JSONObject) outterObject;
            if (source.contains(".")) {
                String arg = source.substring(0, source.indexOf('.'));
                deleteJsonProperty(tempJson.get(arg), source.substring(source.indexOf('.') + 1));
                return;
            }
            tempJson.remove(source);

        } else if (outterObject instanceof JSONArray) {
            JSONArray tempArray = (JSONArray) outterObject;
            for (int i = 0; i < tempArray.size(); i++) {
                JSONObject tempJson = (JSONObject) tempArray.get(i);
                if (source.contains(".")) {
                    String arg = source.substring(0, source.indexOf('.'));
                    deleteJsonProperty(tempJson.get(arg), source.substring(source.indexOf('.') + 1));
                    return;
                } else {
                    tempJson.remove(source);
                }
            }
        }
    }

    //递归实现字段保留
    private static void keepKeys(Object outterObject, String source) {
        if(outterObject==null|| source==null||"".equals(source))
            return;

        if (outterObject instanceof JSONObject) {
            JSONObject obj = ((JSONObject) outterObject);
            if (source.contains(".")) {
                String arg = source.substring(0, source.indexOf('.'));
                keepKeys(obj.get(arg), source.substring(source.indexOf('.') + 1));
            }else
                keepJsonObjectByGivenKeys(obj, source);
        } else if (outterObject instanceof JSONArray) {
            JSONArray tempArray = (JSONArray) outterObject;
            for (int i = 0; i < tempArray.size(); i++) {
                JSONObject obj = tempArray.getJSONObject(i);
                if (source.contains(".")) {
                    String arg = source.substring(0, source.indexOf('.'));
                    keepKeys(obj.get(arg), source.substring(source.indexOf('.') + 1));
                } else {
                    keepJsonObjectByGivenKeys(obj, source);
                }
            }
        }
    }

    private static void keepJsonObjectByGivenKeys(JSONObject jsonObj, String keys) {
        String[] keyAttr = keys.split(Constant.INNER_SPLIT_FLAG);
        Set<String> keyKeepSet = new HashSet<String>();
        for (String s : keyAttr) {
            keyKeepSet.add(s);
        }
        Set<String> keyRemove = new HashSet<String>();
        keyRemove.addAll(jsonObj.keySet());
        keyRemove.removeAll(keyKeepSet);
        for (String key : keyRemove) {
            jsonObj.remove(key);
        }
    }
    public static void keepJsonByKeys(TreeSet<String> keySet, JSONObject obj){
        if(!(keySet instanceof TreeSet))
            return;
        for(String key:keySet){
            keepKeys(obj,key);
        }
    }
    public static void delJsonByKeys(JSONObject json, List<String> keys){
        for (int i =0;i<keys.size();i++){
            deleteJsonProperty(json,keys.get(i));
        }
    }
    //把json key转成tree set，便于保留字段
    public static TreeSet<String> getTreeSet(List<String> keys){
        int max = geMaxAtppear(keys);//统计json最大层数
        TreeSet<String> set = new TreeSet<String>();
        for (int j=0;j<=max;j++){
            if (j>0){
                //同一层次的key去重
                List<String> sameLevel = removeDuplicate(findCount(keys,j));
                //去掉同一层子层的前缀
                List<String> noPref = removePref(sameLevel,j);
                set.add(noPref.toString().substring(1,noPref.toString().length()-1).replaceAll(" ",""));
            }else {
                //同一层次的key去重
                List<String> sameLevel = removeDuplicate(findCount(keys,j));
                set.add(sameLevel.toString().substring(1,sameLevel.toString().length()-1).replaceAll(" ",""));
            }

        }
        return set;
    }
    //去除list里面的重复元素
    private static List<String> removeDuplicate(List<String> list) {
        LinkedHashSet<String> set = new LinkedHashSet<String>(list.size());
        List<String> result = new ArrayList<String>();
        set.addAll(list);
        result.addAll(set);
        return result;
    }
    //去除同一层中相同的前缀
    private static List<String> removePref(List<String> list,int layer){
        List<String> result = new ArrayList<String>();
        List<String> pref = new ArrayList<String>();
        for (int i = 0; i < list.size(); i++) {
            if (pref.contains(getPref(list.get(i), layer))) {
                result.add(getSuffix(list.get(i), layer));
            } else {
                pref.add(getPref(list.get(i), layer));
                result.add(list.get(i));
            }
        }
        return result;
    }
    //获取某个元素的前缀
    private static String getPref(String str,int layer){
        int index = StringUtils.ordinalIndexOf(str,".",layer);
        return str.substring(0,index);
    }
    //获取某个元素的后缀
    private static String getSuffix(String str,int layer){
        int index = StringUtils.ordinalIndexOf(str,".",layer);
        return str.substring(index+1,str.length());
    }
    //统计某个字符在字符串中出现的次数
    private static int appearNumber(String srcText, String findText) {
        int count = 0;
        int index = 0;
        while ((index = srcText.indexOf(findText, index)) != -1) {
            index = index + findText.length();
            count++;
        }
        return count;
    }
    //统计list元素里面出现“.”最多的次数
    private static int geMaxAtppear(List<String> list){
        int max = 0;
        for (int i=0;i<list.size();i++){
            if (max<appearNumber(list.get(i),".")){
                max=appearNumber(list.get(i),".");
            }
        }
        return max;
    }
    //返回list里面指定"."个数的元素
    private static List<String> findCount(List<String> list,int count){
        List<String> stringList = new ArrayList<String>();
        for (int i=0;i<list.size();i++){
            if (appearNumber(list.get(i),".")==count){
                stringList.add(list.get(i));
            }
            if (appearNumber(list.get(i),".")>count){
                stringList.add(list.get(i).substring(0,StringUtils.ordinalIndexOf(list.get(i),".",count+1)));
            }
        }
        return stringList;
    }
    //返回json keys与给定list里面相似度最大的元素列表
    public static List<String> getKeysByJSON(String json,List<String> list){
        JSONObject jsonObject = null;
        if (json.startsWith("[")){
            jsonObject = JSON.parseArray(json).getJSONObject(0);
        }else{
            jsonObject = JSON.parseObject(json);
        }
        if (list.isEmpty()){
            return new ArrayList<String>();
        }else {
            List<String> keys = new ArrayList<String>();
            List<String> jsonKey = getKeysList(jsonObject);
            for (int i=0;i<list.size();i++){
                if (!list.get(i).equals("")){
                    keys.add(maxSimiFromList(list.get(i),jsonKey));
                }
            }
            return keys;
        }
    }
    //递归获取json keys
    private static List<String> getJsonPath(JSONObject obj, String path) throws JSONException {
        List<String> keys = new ArrayList<String>();
        for (Map.Entry entry : obj.entrySet()) {
            keys.add(entry.getKey().toString());
        }
        List<String> keyList = new ArrayList<String>();
        for (int i = 0; i < keys.size (); ++i) {
            String key = keys.get (i);
            if (obj.get(key) instanceof JSONObject) {
                String newPath = path + "." + key;
                keyList.add(getJsonPath((JSONObject)obj.get(key), newPath).toString().substring(1,getJsonPath((JSONObject)obj.get(key), newPath).toString().length()-1));
            } else if (obj.get(key) instanceof JSONArray) {
                JSONArray arr = (JSONArray) obj.get(key);
                int j = 0;
                for (int k = 0; k<arr.size();k++ ) {
                    JSONObject object = (JSONObject)arr.get(k);
                    String newPath = path + "." + key;
                    keyList.add(getJsonPath(object, newPath).toString().substring(1,getJsonPath(object, newPath).toString().length()-1));
                    j++;
                }
            }
            keyList.add(path + "." + key);
        }
        return removeDuplicate(keyList);
    }
    //获取json key的list
    private static List<String> getKeysList(JSONObject json){
        String key = getJsonPath(json,"$").toString();
        List<String> keys = Utils.parseCommaDelimitedStr(key.substring(1,key.length()-1));
        List<String> result = new ArrayList<String>();
        for (int i=0;i<keys.size();i++){
            result.add(keys.get(i).replaceAll("\\s*", "").replaceAll("\\$.",""));
        }
        return result;
    }
    //寻找List里面与str相似度最大的元素
    private static String maxSimiFromList(String str,List<String> list){
        double[] simly = new double[list.size()];
        for (int i=0;i<list.size();i++){
            simly[i] = levenshtein(str,list.get(i));
        }
        return list.get(getMaxIndex(simly));
    }
    //编辑距离算法获取两字符串匹配度
    private static double levenshtein(String str1,String str2) {
        //计算两个字符串的长度。
        int len1 = str1.length();
        int len2 = str2.length();
        //建立上面说的数组，比字符长度大一个空间
        int[][] dif = new int[len1 + 1][len2 + 1];
        //赋初值，步骤B。
        for (int a = 0; a <= len1; a++) {
            dif[a][0] = a;
        }
        for (int a = 0; a <= len2; a++) {
            dif[0][a] = a;
        }
        //计算两个字符是否一样，计算左上的值
        int temp;
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    temp = 0;
                } else {
                    temp = 1;
                }
                //取三个值中最小的
                dif[i][j] = min(dif[i - 1][j - 1] + temp, dif[i][j - 1] + 1,dif[i - 1][j] + 1);
            }
        }
        //计算相似度
        float similarity =1 - (float) dif[len1][len2] / Math.max(str1.length(), str2.length());
        return similarity;
    }

    //得到最小值
    private static int min(int... is) {
        int min = Integer.MAX_VALUE;
        for (int i : is) {
            if (min > i) {
                min = i;
            }
        }
        return min;
    }
    //获取最大值的下标
    private static int getMaxIndex(double[] arr){
        int maxIndex = 0;	//获取到的最大值的角标
        for(int i=0; i<arr.length; i++){
            if(arr[i] > arr[maxIndex]){
                maxIndex = i;
            }
        }
        return maxIndex;
    }

}
