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
public class JCoMetaUtil4Int1IT extends AbsFixedLengthTestJCoMetaUtil1 {

    public JCoMetaUtil4Int1IT(ESAPParamType paramType, int length) {
        super(paramType, length, 1, 1, 1);
    }

    protected String getParamName() {
        return "length";
    }

    protected ESAPMetaType getMetaType() {
        return ESAPMetaType.INT1;
    }
}
