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
 */
@RunWith(Parameterized.class)
public class JCoMetaUtil4NumIT extends AbsImportExportTestJCoMetaUtil {

    public JCoMetaUtil4NumIT(ESAPParamType paramType, int length) {
        super(paramType, length);
    }

    @Override
    protected String getParamName() {
        return "number";
    }

    @Override
    protected ESAPMetaType getMetaType() {
        return ESAPMetaType.NUM;
    }

    @SuppressWarnings("rawtypes")
    @Parameters
    public static Collection setup() {
        return Arrays.asList(new Object[][] { //
                { ESAPParamType.IMPORT, 3, }, //
                { ESAPParamType.IMPORT, 10 }, //
                { ESAPParamType.EXPORT, 3 }, //
                { ESAPParamType.EXPORT, 10 } //
        });
    }
}
