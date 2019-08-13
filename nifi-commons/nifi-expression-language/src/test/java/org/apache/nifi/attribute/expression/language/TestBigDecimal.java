package org.apache.nifi.attribute.expression.language;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.nifi.attribute.expression.language.exception.AttributeExpressionLanguageException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestBigDecimal {
    static final String pi = "3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679";
    static final String number1 = "0.0100001";
    Map<String, String> valueLookup = new HashMap<>();

    @Before
    public void initData() {
        valueLookup.put("pi", pi);
        valueLookup.put("number1", number1);
    }

    @After
    public void cleanData() {
        valueLookup.clear();
    }

    @Test
    public void testBigDecimal_plus() {
        final String expected = "3.1515927535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679";

        do_compute("plus", expected);
    }

    @Test
    public void testBigDecimal_minus() {
        final String expected = "3.1315925535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679";

        do_compute("minus", expected);
    }

    @Test
    public void testBigDecimal_multiply() {
        final String expected = "0.03141624069516329136395028009713335679226011371069099572033154341753739484450271860718021105690370489070679";

        do_compute("multiply", expected);
    }

    private void do_compute(String func, String expected) {
        String result = null;

        // attr
        result = Query.evaluateExpressions("${pi:toBigDecimal():" + func + "(${number1})}", valueLookup, null);
        assertEquals(expected, result);

        // number
        result = Query.evaluateExpressions("${pi:toBigDecimal():" + func + "(" + number1 + ")}", valueLookup, null);
        assertEquals(expected, result);

        // bigdecimal
        result = Query.evaluateExpressions("${pi:toBigDecimal():" + func + "(${number1:toBigDecimal()})}", valueLookup, null);
        assertEquals(expected, result);
    }

    @Test
    public void testBigDecimal_divide() {

        String result = null;

        // no scale
        result = Query.evaluateExpressions("${literal('3.14'):toBigDecimal():divide(2.0)}", valueLookup, null);
        assertEquals("1.57", result);

        // scale
        result = Query.evaluateExpressions("${literal('3.14'):toBigDecimal():divide(2.0, 1)}", valueLookup, null);
        assertEquals("1.6", result);

        // invalid var
        result = Query.evaluateExpressions("${'3.14':toBigDecimal():divide(2.0, 1)}", valueLookup, null);
        assertEquals("", result);

        // attr
        result = Query.evaluateExpressions("${pi:toBigDecimal():divide(${number1},7,'up')}", valueLookup, null);
        assertEquals("314.1561238", result);

        // not attr
        result = Query.evaluateExpressions("${pi:toBigDecimal():divide(${number2})}", valueLookup, null);
        assertEquals("", result);

        // number
        result = Query.evaluateExpressions("${pi:toBigDecimal():divide(" + number1 + ",5,\"up\")}", valueLookup, null);
        assertEquals("314.15613", result);

        // bigdecimal
        result = Query.evaluateExpressions("${pi:toBigDecimal():divide(${number1:toBigDecimal()},2,'down')}", valueLookup, null);
        assertEquals("314.15", result);

    }

    @Test(expected = AttributeExpressionLanguageException.class)
    public void testBigDecimal_divide_wrong() {
        // wrong expression
        Query.evaluateExpressions("${3.14:toBigDecimal():divide(2.0, 1)}", valueLookup, null);
    }

    @Test
    public void testBigDecimal_min() {
        do_logic("min", number1, "0.1");
    }

    @Test
    public void testBigDecimal_max() {
        do_logic("max", pi, "10");
    }

    private void do_logic(String func, String expected1, String expected2) {
        String result = Query.evaluateExpressions("${pi:toBigDecimal():" + func + "(${number1})}", valueLookup, null);
        assertEquals(expected1, result);

        result = Query.evaluateExpressions("${pi:toBigDecimal():" + func + "('${number1}')}", valueLookup, null);
        assertEquals(expected1, result);

        result = Query.evaluateExpressions("${pi:toBigDecimal():" + func + "(\"${number1}\")}", valueLookup, null);
        assertEquals(expected1, result);

        result = Query.evaluateExpressions("${pi:toBigDecimal():" + func + "(" + expected2 + ")}", valueLookup, null);
        assertEquals(expected2, result);

        result = Query.evaluateExpressions("${pi:toBigDecimal():" + func + "('" + expected2 + "')}", valueLookup, null);
        assertEquals(expected2, result);

        result = Query.evaluateExpressions("${pi:toBigDecimal():" + func + "(\"" + expected2 + "\")}", valueLookup, null);
        assertEquals(expected2, result);
    }

    @Test
    public void testBigDecimal_percent() {
        // 百分比后，由于乘以100，小数点后精度应该减少2位
        String result = Query.evaluateExpressions("${literal('0.3141592653589793'):toBigDecimal():setScale(5,'UP'):toPercent()}", valueLookup, null);
        // assertEquals("31.41600%", result);
        assertEquals("31.416%", result);

        result = Query.evaluateExpressions("${literal('0.3141592653589793'):toBigDecimal():setScale(5,'DOWN'):toPercent()}", valueLookup, null);
        // assertEquals("31.41500%", result);
        assertEquals("31.415%", result);

        // 四舍五入
        result = Query.evaluateExpressions("${literal('0.3141592653589793'):toBigDecimal():setScale(4,'HALF_UP'):toPercent()}", valueLookup, null);
        // assertEquals("31.4200%", result);
        assertEquals("31.42%", result);

        result = Query.evaluateExpressions("${literal('0.3141592653589793'):toBigDecimal():toPercent()}", valueLookup, null);
        // assertEquals("31.4159265358979300%", result);
        assertEquals("31.41592653589793%", result);
    }

    @Test
    public void testBigDecimal_floatValue() {
        String result = Query.evaluateExpressions("${literal('0.3141592653589793'):toBigDecimal():floatValue()}", valueLookup, null);
        assertEquals("0.31415927", result);
    }

    @Test
    public void testBigDecimal_equals() {
        String result = Query.evaluateExpressions("${literal('0.14159265358979358'):toBigDecimal():equals(3.14159265358979358)}", valueLookup, null);
        assertEquals("false", result);

        result = Query.evaluateExpressions("${literal('0.14159265358979358'):toBigDecimal():equals('3.14159265358979358')}", valueLookup, null);
        assertEquals("false", result);

        result = Query.evaluateExpressions("${literal('0.14159265358979358'):toBigDecimal():equals(${literal('3.14159265358979358'):toBigDecimal()})}", valueLookup, null);
        assertEquals("false", result);

        // 丢失精度
        result = Query.evaluateExpressions("${literal('3.14159265358979358'):toBigDecimal():equals(3.14159265358979358)}", valueLookup, null);
        assertEquals("false", result);

        // 精度未丢失
        result = Query.evaluateExpressions("${literal('3.1415926'):toBigDecimal():equals(3.1415926)}", valueLookup, null);
        assertEquals("true", result);

        result = Query.evaluateExpressions("${literal('3.14159265358979358'):toBigDecimal():equals('3.14159265358979358')}", valueLookup, null);
        assertEquals("true", result);

        result = Query.evaluateExpressions("${literal('3.14159265358979358'):toBigDecimal():equals(${literal('3.14159265358979358'):toBigDecimal()})}", valueLookup, null);
        assertEquals("true", result);
    }

    @Test
    public void testBigDecimal_Compute() {
        final Map<String, String> attributes = new HashMap<>();
        String number = "3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679";
        String number1 = "0.0100001";
        String scale1 = "7";
        String n = "6";
        String round = "UP";

        attributes.put("number", number);
        attributes.put("number1", number1);
        attributes.put("scale1", scale1);
        attributes.put("n", n);
        attributes.put("round", round);

        String ret = Query.evaluateExpressions("${number:toBigDecimal():plus(${number1:toBigDecimal()})}", attributes, null);
        assertEquals(ret, "3.1515927535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679");

        ret = Query.evaluateExpressions("${number:toBigDecimal():minus(${number1})}", attributes, null);
        assertEquals(ret, "3.1315925535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679");

        ret = Query.evaluateExpressions("${number:toBigDecimal():multiply(${number1})}", attributes, null);
        assertEquals(ret, "0.03141624069516329136395028009713335679226011371069099572033154341753739484450271860718021105690370489070679");

        ret = Query.evaluateExpressions("${number:toBigDecimal():divide(${number1},${scale1},${round})}", attributes, null);
        assertEquals(ret, "314.1561238");

        ret = Query.evaluateExpressions("${number:toBigDecimal():negate()}", attributes, null);
        assertEquals(ret, "-3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679");

        ret = Query.evaluateExpressions("${number:toBigDecimal():negate():abs()}", attributes, null);
        assertEquals(ret, "3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679");

        ret = Query.evaluateExpressions("${number:toBigDecimal():negate():abs():floatValue()}", attributes, null);
        assertEquals(ret, "3.1415927");

        ret = Query.evaluateExpressions("${number:toBigDecimal():negate():abs():intValue()}", attributes, null);
        assertEquals(ret, "3");

        ret = Query.evaluateExpressions("${number:toBigDecimal():negate():abs():multiply(${n}):multiply(${number1})}", attributes, null);
        assertEquals(ret, "0.18849744417097974818370168058280014075356068226414597432198926050522436906701631164308126634142222934424074");

        ret = Query.evaluateExpressions("${number:toBigDecimal():negate():abs():multiply(${n}):multiply(${number1}):intValue()}", attributes, null);
        assertEquals(ret, "0");

        ret = Query.evaluateExpressions("${number:toBigDecimal():negate():abs():multiply(${n}):multiply(${number1}):longValue()}", attributes, null);
        assertEquals(ret, "0");

        ret = Query.evaluateExpressions("${number:toBigDecimal():negate():abs():multiply(${n}):multiply(${number1}):shortValue()}", attributes, null);
        assertEquals(ret, "0");

    }

    @Test
    public void testBigDecimal_Compare() {
        final Map<String, String> attributes = new HashMap<>();
        String number = "3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679";
        String number1 = "0.0100001";
        String scale1 = "7";
        String n = "6";

        attributes.put("number", number);
        attributes.put("number1", number1);
        attributes.put("scale1", scale1);
        attributes.put("round", "up");
        attributes.put("n", n);

        String ret = Query.evaluateExpressions("${number:toBigDecimal():min(${number1})}", attributes, null);
        assertEquals(ret, "0.0100001");

        ret = Query.evaluateExpressions("${number:toBigDecimal():max(${number1})}", attributes, null);
        assertEquals(ret, "3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679");

        ret = Query.evaluateExpressions("${number:toBigDecimal():compareTo(${number1})}", attributes, null);
        assertEquals(ret, "1");

        ret = Query.evaluateExpressions("${number:toBigDecimal():equals(${number1})}", attributes, null);
        assertEquals(ret, "false");

        ret = Query.evaluateExpressions("${number:toBigDecimal():equals(${number})}", attributes, null);
        assertEquals(ret, "true");

        ret = Query.evaluateExpressions("${number:toBigDecimal():pow(${n})}", attributes, null);
        assertEquals(ret,
                "961.389193575304437030219443652419898867217528081046615941076187484093912842350213499221409151915736000280246405278465872660357807620005135279422236248573953738370729699460331930799492547929781537685630455999078157608184450720962351166900832358782735405853483284741528629479549640601841775345501611621546470641160128754424785908336487037676176343911289421422664006348231052168740387025365335473945320281291196054873597930644539270386617096188067014475017542076535860513456259473881815295197656618738023706631534161262151199794177439442508513086973807203624876218315430317111136512750178374268754168871921");

        ret = Query.evaluateExpressions("${number:toBigDecimal():powOfTen(${n})}", attributes, null);
        assertEquals(ret, "3141592.6535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679");

        ret = Query.evaluateExpressions("${number:toBigDecimal():moveRight(${n})}", attributes, null);
        assertEquals(ret, "3141592.6535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679");

        ret = Query.evaluateExpressions("${number:toBigDecimal():moveLeft(${n})}", attributes, null);
        assertEquals(ret, "0.0000031415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679");

        ret = Query.evaluateExpressions("${number:toBigDecimal():moveLeft(${n}):setScale(${n},${round})}", attributes, null);
        assertEquals(ret, "0.000004");

        ret = Query.evaluateExpressions("${number:toBigDecimal():moveLeft(2):setScale(5,${round}):toPercent()}", attributes, null);
        assertEquals(ret, "3.142%");

        ret = Query.evaluateExpressions("${number:toBigDecimal():moveLeft(3):lt(${number1})}", attributes, null);
        assertEquals(ret, "true");

        ret = Query.evaluateExpressions("${number:toBigDecimal():le(${number1})}", attributes, null);
        assertEquals(ret, "false");

        ret = Query.evaluateExpressions("${number:toBigDecimal():gt(${number1})}", attributes, null);
        assertEquals(ret, "true");

        ret = Query.evaluateExpressions("${number:toBigDecimal():ge(${number})}", attributes, null);
        assertEquals(ret, "true");
    }
}
