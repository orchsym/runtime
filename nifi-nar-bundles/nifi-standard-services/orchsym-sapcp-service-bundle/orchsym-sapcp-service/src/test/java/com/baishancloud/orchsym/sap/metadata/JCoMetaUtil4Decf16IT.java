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
 *         no length
 * 
 */
@RunWith(Parameterized.class)
public class JCoMetaUtil4Decf16IT extends AbsFixedLengthTestJCoMetaUtil1 {

    public JCoMetaUtil4Decf16IT(ESAPParamType paramType, int length) {
        super(paramType, length, 8, 8, 4);
    }

    protected String getParamName() {
        return "decf";
    }

    protected ESAPMetaType getMetaType() {
        return ESAPMetaType.DECF16;
    }

}
