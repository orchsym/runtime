package com.baishancloud.orchsym.processors.dubbo.it;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * Because use lambda in method, must set the dependency of javassist to higher version 3.18+
 * 
 * @author GU Guoqiang
 *
 */
public class PersonServiceImpl implements PersonService {

    @Override
    public String sayHello(String name) {
        return "Hello " + name;
    }

    @Override
    public String call(int arg1, double arg2, boolean arg3, String arg4) {
        return "" + arg1 + ',' + arg2 + ',' + arg3 + ',' + arg4;
    }

    @Override
    public String call(char arg1, byte arg2, short arg3, int arg4, long arg5, float arg6, double arg7, boolean arg8) {
        return call(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, "Hello");
    }

    @Override
    public String call(byte[] arg1, short[] arg2, int[] arg3, long[] arg4, float[] arg5, double[] arg6, boolean[] arg7) {
        List<String> result = new ArrayList<>();

        StringBuffer one = new StringBuffer(30);
        for (int i = 0; i < arg1.length; i++) {
            if (i > 0) {
                one.append(',');
            }
            one.append(arg1[i]);
        }
        result.add(one.toString());
        one.delete(0, one.length());

        for (int i = 0; i < arg2.length; i++) {
            if (i > 0) {
                one.append(',');
            }
            one.append(arg2[i]);
        }
        result.add(one.toString());
        one.delete(0, one.length());

        for (int i = 0; i < arg3.length; i++) {
            if (i > 0) {
                one.append(',');
            }
            one.append(arg3[i]);
        }
        result.add(one.toString());
        one.delete(0, one.length());

        for (int i = 0; i < arg4.length; i++) {
            if (i > 0) {
                one.append(',');
            }
            one.append(arg4[i]);
        }
        result.add(one.toString());
        one.delete(0, one.length());

        for (int i = 0; i < arg5.length; i++) {
            if (i > 0) {
                one.append(',');
            }
            one.append(arg5[i]);
        }
        result.add(one.toString());
        one.delete(0, one.length());

        for (int i = 0; i < arg6.length; i++) {
            if (i > 0) {
                one.append(',');
            }
            one.append(arg6[i]);
        }
        result.add(one.toString());
        one.delete(0, one.length());

        for (int i = 0; i < arg7.length; i++) {
            if (i > 0) {
                one.append(',');
            }
            one.append(arg7[i]);
        }
        result.add(one.toString());
        one.delete(0, one.length());

        return StringUtils.join(result, ";");
    }

    @Override
    public String call(char[] arg1) {
        return StringUtils.join(arg1, ",");
    }

    @Override
    public String call(Character arg1, Byte arg2, Short arg3, Integer arg4, Long arg5, Float arg6, Double arg7, Boolean arg8, String arg9) {
        return "" + arg1 + ',' + arg2 + ',' + arg3 + ',' + arg4 + ',' + arg5 + ',' + arg6 + ',' + arg7 + ',' + arg8 + ',' + arg9;
    }

    @Override
    public List<String> call(Character[] arg1, Byte[] arg2, Short[] arg3, Integer[] arg4, Long[] arg5, Float[] arg6, Double[] arg7, Boolean[] arg8, String[] arg9) {
        List<String> result = new ArrayList<>();
        result.add(StringUtils.join(arg1, ","));
        result.add(StringUtils.join(arg2, ","));
        result.add(StringUtils.join(arg3, ","));
        result.add(StringUtils.join(arg4, ","));
        result.add(StringUtils.join(arg5, ","));
        result.add(StringUtils.join(arg6, ","));
        result.add(StringUtils.join(arg7, ","));
        result.add(StringUtils.join(arg8, ","));
        result.add(StringUtils.join(arg9, ","));
        return result;
    }

    @Override
    public List<String> call(List<Character> arg1, List<Byte> arg2, List<Short> arg3, List<Integer> arg4, List<Long> arg5, List<Float> arg6, List<Double> arg7, List<Boolean> arg8, List<String> arg9) {
        List<String> result = new ArrayList<>();
        result.add(StringUtils.join(arg1, ","));
        result.add(StringUtils.join(arg2, ","));
        result.add(StringUtils.join(arg3, ","));
        result.add(StringUtils.join(arg4, ","));
        result.add(StringUtils.join(arg5, ","));
        result.add(StringUtils.join(arg6, ","));
        result.add(StringUtils.join(arg7, ","));
        result.add(StringUtils.join(arg8, ","));
        result.add(StringUtils.join(arg9, ","));
        return result;
    }


    @Override
    public Map<String, Object> call(Map<String, Character> arg1, Map<String, Short> arg2) {
        Map<String, Object> results = new LinkedHashMap<>();
        results.putAll(arg1);
        results.putAll(arg2);
        return results;
    }

    @Override
    public Map<String, Object> call(Map<Short, Character> arg1, Map<Long, Byte> arg2, Map<Double, Boolean> arg3) {
        Map<String, Object> results = new LinkedHashMap<>();

        final Short key1 = arg1.keySet().iterator().next();
        results.put(key1.toString(), arg1.get(key1));
        final Long key2 = arg2.keySet().iterator().next();
        results.put(key2.toString(), arg2.get(key2));
        final Double key3 = arg3.keySet().iterator().next();
        results.put(key3.toString(), arg3.get(key3));

        return results;
    }

    @Override
    public boolean valid(Person person) {
        return person != null && person.getAge() > 18;
    }

    @Override
    public List<Person> filter(List<Person> persons, boolean withChild) {
        if (persons == null || persons.isEmpty()) {
            return Collections.emptyList();
        }
        // only return the parent
        final List<Person> filtered = persons.stream().filter(p -> {
            if (valid(p)) {
                final List<Person> children = p.getChildren();
                if (withChild && children != null && !children.isEmpty()) { // have children
                    final List<Person> filter = filter(children, withChild);
                    return filter.size() > 0;
                } else {
                    return true;
                }
            }
            return false;
        }).map(p -> new Person(p.getId(), p.getName(), p.getAge())).collect(Collectors.toList());

        return filtered;
    }

    @Override
    public Person[] filter(Person[] persons, boolean withChild) {
        if (persons == null || persons.length == 0) {
            return new Person[0];
        }
        return filter(Arrays.asList(persons), withChild).toArray(new Person[persons.length]);
    }

    @Override
    public Map<Integer, Person> convert(Map<String, Person> persons) {
        if (persons == null || persons.isEmpty()) {
            return Collections.emptyMap();
        }
        return persons.values().stream().collect(Collectors.toMap(Person::getId, p -> new Person(p.getId(), p.getName(), p.getAge())));
    }

}
