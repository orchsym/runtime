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
 *         precision: option, set when need
 */
@RunWith(Parameterized.class)
public class JCoMetaUtil4FloatIT extends AbsFixedLengthTestJCoMetaUtil2 {

    public JCoMetaUtil4FloatIT(ESAPParamType paramType, int length, int pricesion) {
        super(paramType, length, length, pricesion, 8, 8, 4);
    }

    protected String getParamName() {
        return "length";
    }

    protected ESAPMetaType getMetaType() {
        return ESAPMetaType.FLOAT;
    }
}
