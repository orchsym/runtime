package com.baishancloud.orchsym.sap.metadata;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.baishancloud.orchsym.sap.metadata.param.ESAPMetaType;
import com.baishancloud.orchsym.sap.metadata.param.ESAPParamType;

/**
 * @author GU Guoqiang
 *
 *         length: requried??
 */
@RunWith(Parameterized.class)
public class JCoMetaUtil4TimeIT extends AbsFixedLengthTestJCoMetaUtil1 {

    public JCoMetaUtil4TimeIT(ESAPParamType paramType, int length) {
        super(paramType, length, -1, -1, 6);
    }

    protected String getParamName() {
        return "time";
    }

    protected ESAPMetaType getMetaType() {
        return ESAPMetaType.TIME;
    }
}
