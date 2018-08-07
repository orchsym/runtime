package com.baishancloud.orchsym.sap.metadata.param;

import java.util.Arrays;
import java.util.Collection;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.conn.jco.JCoMetaData;

/**
 * @author GU Guoqiang
 *
 *         Test the enum is same as JCo api with types.
 */
@RunWith(Parameterized.class)
public class TestParamsESAPMetaType {
    private int jcoType;
    private ESAPMetaType type;

    public TestParamsESAPMetaType(int jcoType, ESAPMetaType type) {
        super();
        this.jcoType = jcoType;
        this.type = type;
    }

    private static int num;

    @BeforeClass
    public static void init() {
        num = 0;
    }

    @AfterClass
    public static void verifyTimes() {
        // make sure no type to missing
        Assert.assertEquals(num, ESAPMetaType.values().length);
        num = 0; // clean
    }

    @Before
    public void accumulate() {
        num++;
    }

    @SuppressWarnings("rawtypes")
    @Parameters
    public static Collection setup() {
        return Arrays.asList(new Object[][] { { JCoMetaData.TYPE_CHAR, ESAPMetaType.CHAR }, { JCoMetaData.TYPE_DATE, ESAPMetaType.DATE }, { JCoMetaData.TYPE_BCD, ESAPMetaType.BCD },
                { JCoMetaData.TYPE_TIME, ESAPMetaType.TIME }, { JCoMetaData.TYPE_BYTE, ESAPMetaType.BYTE }, { JCoMetaData.TYPE_NUM, ESAPMetaType.NUM }, { JCoMetaData.TYPE_FLOAT, ESAPMetaType.FLOAT },
                { JCoMetaData.TYPE_INT, ESAPMetaType.INT }, { JCoMetaData.TYPE_INT2, ESAPMetaType.INT2 }, { JCoMetaData.TYPE_INT1, ESAPMetaType.INT1 },
                { JCoMetaData.TYPE_STRUCTURE, ESAPMetaType.STRUCTURE }, { JCoMetaData.TYPE_DECF16, ESAPMetaType.DECF16 }, { JCoMetaData.TYPE_DECF34, ESAPMetaType.DECF34 },
                { JCoMetaData.TYPE_STRING, ESAPMetaType.STRING }, { JCoMetaData.TYPE_XSTRING, ESAPMetaType.XSTRING }, { JCoMetaData.TYPE_EXCEPTION, ESAPMetaType.EXCEPTION },
                { JCoMetaData.TYPE_TABLE, ESAPMetaType.TABLE }, { JCoMetaData.TYPE_EXCEPTION, ESAPMetaType.EXCEPTION } });
    }

    @Test
    public void makeSureRightTypes() {
        final ESAPMetaType jcoMetaType = ESAPMetaType.get(jcoType);
        Assert.assertEquals(type, jcoMetaType);
        Assert.assertEquals(type.getJCoType(), jcoType);

        final ESAPMetaType metaType = ESAPMetaType.get(type.getName());
        Assert.assertEquals(type, metaType);
        Assert.assertEquals(type.getJCoType(), metaType.getJCoType());
    }

    public static void main(String[] args) {
        for (ESAPMetaType t : ESAPMetaType.values()) {
            System.out.println("{ JCoMetaData.TYPE_" + t.name() + ", ESAPMetaType." + t.name() + " }, ");
        }
    }
}
