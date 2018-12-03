package org.apache.nifi.nar.i18n;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author GU Guoqiang
 *
 */
@RunWith(Parameterized.class)
public class MessagesProviderTest {
    @Parameters
    public static Collection<Object[]> prepareData() {
        Object[][] object = { //
                { "A B", "A_B" }, //
                { "A  B", "A__B" }, //
                { "A* B", "A__B" }, //
                { "A/ B", "A__B" }, //
                { "A/*B", "A__B" }, //
                { "A: B", "A__B" }, //
                { "A, B", "A__B" }, //
                { "A; B", "A__B" }, //

                { "A@#$%^&B", "AB" }, //
                { "A(B)", "AB" }, //
                { "A<B>", "AB" }, //
                { "A'B'", "AB" }, //
                { "A\"B\"", "AB" }, //
                { "A[B]", "AB" }, //
                { "A{B}", "AB" }, //

                { "A\u200bB", "AB" }, //
                { "\u200bA\u200bB", "AB" }, //

        };
        return Arrays.asList(object);
    }

    private String name, expected;

    public MessagesProviderTest(String name, String expected) {
        this.name = name;
        this.expected = expected;
    }

    @Test
    public void test_fixKey() {
        final String fixKey = MessagesProvider.fixKey(name);
        Assert.assertEquals(expected, fixKey);
    }
}
