package com.baishancloud.orchsym.sap.metadata;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import com.baishancloud.orchsym.sap.metadata.param.ESAPParamType;

/**
 * @author GU Guoqiang
 *
 */
public abstract class AbsFixedLengthTestJCoMetaUtil2 extends AbsImportExportTestJCoMetaUtil {

    public AbsFixedLengthTestJCoMetaUtil2(ESAPParamType paramType, int nucLength, int ucLength, int pricesion, int fixLength, int fixByteLength, int fixJavaLength) {
        super(paramType, nucLength, ucLength, pricesion, fixLength, fixByteLength, fixJavaLength);
    }

    public AbsFixedLengthTestJCoMetaUtil2(ESAPParamType paramType, int nucLength, int ucLength, int fixLength, int fixByteLength, int fixJavaLength) {
        super(paramType, nucLength, ucLength, 0, fixLength, fixByteLength, fixJavaLength);
    }

    @SuppressWarnings("rawtypes")
    @Parameters
    public static Collection setup() {
        return Arrays.asList(new Object[][] { //
                { ESAPParamType.IMPORT, 3, 5 }, //
                { ESAPParamType.IMPORT, 10, 10 }, //
                { ESAPParamType.IMPORT, 50, 80 }, //
                { ESAPParamType.IMPORT, 0, 0 }, // no value
                { ESAPParamType.EXPORT, 3, 5 }, //
                { ESAPParamType.EXPORT, 10, 10 }, //
                { ESAPParamType.EXPORT, 50, 80 }, //
                { ESAPParamType.EXPORT, 0, 0 }, // no value
        });
    }
}
