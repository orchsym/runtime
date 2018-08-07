package com.baishancloud.orchsym.sap.metadata;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.baishancloud.orchsym.sap.metadata.param.ESAPMetaType;
import com.baishancloud.orchsym.sap.metadata.param.ESAPParamType;

/**
 * @author GU Guoqiang
 *
 *         length: required
 * 
 *         nucLength: option
 * 
 */
@RunWith(Parameterized.class)
public class JCoMetaUtil4BCDIT extends AbsImportExportTestJCoMetaUtil {

    public JCoMetaUtil4BCDIT(ESAPParamType paramType, int nucLength, int length) {
        super(paramType, nucLength, length, 0, -1, -1, (nucLength / 2 + nucLength % 2));
    }

    protected String getParamName() {
        return "bcd";
    }

    protected ESAPMetaType getMetaType() {
        return ESAPMetaType.BCD;
    }

    @SuppressWarnings("rawtypes")
    @Parameters
    public static Collection setup() {
        return Arrays.asList(new Object[][] { //
                { ESAPParamType.IMPORT, 3, 5 }, //
                { ESAPParamType.IMPORT, 10, 10 }, //
                { ESAPParamType.EXPORT, 3, 5 }, //
                { ESAPParamType.EXPORT, 10, 10 }, //
        });
    }
}
