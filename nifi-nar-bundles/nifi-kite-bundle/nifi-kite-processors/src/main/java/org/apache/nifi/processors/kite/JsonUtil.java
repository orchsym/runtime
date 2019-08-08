package org.apache.nifi.processors.kite;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;

import java.util.List;
import java.util.Map;

public class JsonUtil {
    private JsonUtil(){}

    private static void removeKeyFromEntry(Map.Entry<?, ?> entry, String keyToRemove){
        if(entry.getValue() instanceof Map){
            removeKeyFromMap((Map<?, ?>)entry.getValue(), keyToRemove);
        }else if(entry.getValue() instanceof List){
            removeKeyFromList((List) entry.getValue(), keyToRemove);
        }else{
            if(entry.getKey().equals(keyToRemove)){
                entry.setValue(null);
            }
        }
    }

    private static void removeKeyFromList(List list, String keyToRemove){
        list.forEach(item -> {
            if(item instanceof Map) removeKeyFromMap((Map<?, ?>) item, keyToRemove);
        });
    }

    private static void removeKeyFromMap(Map<?, ?> map, String keyToRemove){
        map.remove(keyToRemove);
        map.entrySet().forEach(entry -> removeKeyFromEntry(entry, keyToRemove));
    }

    public static String removeKey(String jsonStr, String keyToRemove, boolean prettyOutput){
        LinkedTreeMap<?, ?> ltm = new Gson().fromJson(jsonStr, LinkedTreeMap.class);
        removeKeyFromMap(ltm, keyToRemove);

        if(prettyOutput) {
            return new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(ltm);
        }else{
            return new GsonBuilder().disableHtmlEscaping().create().toJson(ltm);
        }
    }

}
