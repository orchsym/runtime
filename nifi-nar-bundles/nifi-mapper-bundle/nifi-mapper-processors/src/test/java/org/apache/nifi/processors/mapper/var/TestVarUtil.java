package org.apache.nifi.processors.mapper.var;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author GU Guoqiang
 *
 */
public class TestVarUtil {
    
    @Disabled
    public void test_hasEL() {
        // no need test, test via EL.
    }

    @Test
    public void test_hasRP_empty_false() {
        boolean hasRP = VarUtil.hasRP(null);
        assertFalse(hasRP);

        hasRP = VarUtil.hasRP("");
        assertFalse(hasRP);
        hasRP = VarUtil.hasRP("    ");
        assertFalse(hasRP);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/abc", //
            "/abc/xyz", //
            "   /abc", //
            "/abc         ", //
            "/               abc ", //
            "   /  abc   /  xyz" })
    public void test_hasRP_startWith_true(String path) {
        boolean hasRP = VarUtil.hasRP(path);
        assertTrue(hasRP);
    }

    @ParameterizedTest
    @ValueSource(strings = { "x/abc", //
            "x  /abc", //
            "hello   / abc         " })
    public void test_hasRP_invalidToken_false(String path) {
        boolean hasRP = VarUtil.hasRP(path);
        assertFalse(hasRP);
    }

    @ParameterizedTest
    @ValueSource(strings = { "abc", "%&&%$" })
    public void test_hasRP_noToken_false(String path) {
        boolean hasRP = VarUtil.hasRP(path);
        assertFalse(hasRP);
    }

    @Test
    public void test_hasRP_quoteToken_false() {
        boolean hasRP = VarUtil.hasRP("\"/abc\"");
        assertFalse(hasRP);
    }

    @ParameterizedTest
    @ValueSource(strings = { "replace( /name, 'xyz', 'zyx' )", //
            "      replace( /name, 'xyz', 'zyx' )", //
            "      replace    ( /   name, 'xyz', 'zyx' )", //
            "replace(/name, 'xyz', 'zyx' )", //
            "replace( /name , 'xyz', 'zyx' )", //
            "/name[contains( ../workAddress/state, /details/preferredState )]", //
            "concat( ' test ', ' lives in ', /homeAddress/city )" })
    public void test_hasRP_function_true(String path) {
        boolean hasRP = VarUtil.hasRP(path);
        assertTrue(hasRP);

    }

    @Test
    public void test_hasRP_mulit_function_true() {
        boolean hasRP = VarUtil.hasRP("format( toDate(/eventDate, \"yyyy-MM-dd’T’HH:mm:ss’Z'\"), 'yyyy-MM-dd')");
        assertTrue(hasRP);
    }

    @ParameterizedTest
    @ValueSource(strings = { "Areplace( /name, 'xyz', 'zyx' )", //
            "A replace( /name, 'xyz', 'zyx' )", //
            "A replace( /name, 'xyz', 'zyx' )" })
    public void test_hasRP_invalidFun_false(String path) {
        boolean hasRP = VarUtil.hasRP(path);
        assertFalse(hasRP);
    }

    @Test
    public void test_hasRP_quoteFun_false() {
        boolean hasRP = VarUtil.hasRP("\"replace( /name, 'xyz', 'zyx' )\"");
        assertFalse(hasRP);
    }
}
