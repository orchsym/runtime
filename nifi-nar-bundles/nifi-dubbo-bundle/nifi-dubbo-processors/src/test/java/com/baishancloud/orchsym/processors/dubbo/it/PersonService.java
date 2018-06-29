package com.baishancloud.orchsym.processors.dubbo.it;

import java.util.List;
import java.util.Map;

/**
 * @author GU Guoqiang
 *
 */
public interface PersonService {

    String sayHello(String name);

    String call(int arg1, double arg2, boolean arg3, String arg4);

    String call(char arg1, byte arg2, short arg3, int arg4, long arg5, float arg6, double arg7, boolean arg8);

    String call(byte[] arg1, short[] arg2, int[] arg3, long[] arg4, float[] arg5, double[] arg6, boolean[] arg7);

    String call(char[] arg1); // can't work yet for Dubbo, after send, will process as string

    String call(Character arg1, Byte arg2, Short arg3, Integer arg4, Long arg5, Float arg6, Double arg7, Boolean arg8, String arg9);

    List<String> call(Character[] arg1, Byte[] arg2, Short[] arg3, Integer[] arg4, Long[] arg5, Float[] arg6, Double[] arg7, Boolean[] arg8, String[] arg9);

    List<String> call(List<Character> arg1, List<Byte> arg2, List<Short> arg3, List<Integer> arg4, List<Long> arg5, List<Float> arg6, List<Double> arg7, List<Boolean> arg8, List<String> arg9);

    Map<String, Object> call(Map<Short, Character> arg1, Map<Long, Byte> arg2, Map<Double, Boolean> arg3);

    Map<String, Object> call(Map<String, Character> arg1, Map<String, Short> arg2);

    boolean valid(Person person);

    List<Person> filter(List<Person> persons, boolean withChild);

    Person[] filter(Person[] persons, boolean withChild);

    Map<Integer, Person> convert(Map<String, Person> persons);

}
