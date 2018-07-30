package com.baishancloud.orchsym.sap.metadata;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.baishancloud.orchsym.sap.metadata.param.ESAPMetaType;
import com.baishancloud.orchsym.sap.metadata.param.ESAPParamType;

/**
 * @author GU Guoqiang
 *
 *         no length
 * 
 */
@RunWith(Parameterized.class)
public class JCoMetaUtil4IntIT extends AbsFixedLengthTestJCoMetaUtil1 {

    public JCoMetaUtil4IntIT(ESAPParamType paramType, int length) {
        super(paramType, length, 4, 4, 2);
    }

    protected String getParamName() {
        return "length";
    }

    protected ESAPMetaType getMetaType() {
        return ESAPMetaType.INT;
    }

}
