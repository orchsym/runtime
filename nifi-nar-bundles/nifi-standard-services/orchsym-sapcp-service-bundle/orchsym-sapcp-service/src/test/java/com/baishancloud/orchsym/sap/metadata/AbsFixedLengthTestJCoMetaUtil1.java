package com.baishancloud.orchsym.sap.metadata;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import com.baishancloud.orchsym.sap.metadata.param.ESAPParamType;

/**
 * @author GU Guoqiang
 *
 */
public abstract class AbsFixedLengthTestJCoMetaUtil1 extends AbsImportExportTestJCoMetaUtil {

    public AbsFixedLengthTestJCoMetaUtil1(ESAPParamType paramType, int ucLength, int fixLength, int fixByteLength, int fixJavaLength) {
        super(paramType, ucLength, ucLength, 0, fixLength, fixByteLength, fixJavaLength);
    }

    @SuppressWarnings("rawtypes")
    @Parameters
    public static Collection setup() {
        return Arrays.asList(new Object[][] { //
                { ESAPParamType.IMPORT, 3 }, //
                { ESAPParamType.IMPORT, 10 }, //
                { ESAPParamType.IMPORT, 50 }, //
                { ESAPParamType.IMPORT, 0 }, // no value
                { ESAPParamType.EXPORT, 3 }, //
                { ESAPParamType.EXPORT, 10 }, //
                { ESAPParamType.EXPORT, 50 }, //
                { ESAPParamType.EXPORT, 0 }, // no value
        });
    }
}
