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
public class JCoMetaUtil4XStringIT extends AbsFixedLengthTestJCoMetaUtil2 {

    public JCoMetaUtil4XStringIT(ESAPParamType paramType, int nucLength, int length) {
        super(paramType, nucLength, length,  0, 8, 0);
    }

    protected String getParamName() {
        return "xstr";
    }

    protected ESAPMetaType getMetaType() {
        return ESAPMetaType.XSTRING;
    }
}
