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
 *         length: required, UnicodeLength
 * 
 *         nucLength: option, Non-UnicodeLength
 * 
 */
@RunWith(Parameterized.class)
public class JCoMetaUtil4CharIT extends AbsImportExportTestJCoMetaUtil {

    public JCoMetaUtil4CharIT(ESAPParamType paramType, int nucLength, int length) {
        super(paramType, nucLength, length);
    }

    protected String getParamName() {
        return "Name";
    }

    protected ESAPMetaType getMetaType() {
        return ESAPMetaType.CHAR;
    }

    @SuppressWarnings("rawtypes")
    @Parameters
    public static Collection setup() {
        return Arrays.asList(new Object[][] { //
                { ESAPParamType.IMPORT, 3, 5 }, //
                { ESAPParamType.IMPORT, 10, 10 }, //
                { ESAPParamType.EXPORT, 3, 5 }, //
                { ESAPParamType.EXPORT, 10, 10 } //
        });
    }
}
