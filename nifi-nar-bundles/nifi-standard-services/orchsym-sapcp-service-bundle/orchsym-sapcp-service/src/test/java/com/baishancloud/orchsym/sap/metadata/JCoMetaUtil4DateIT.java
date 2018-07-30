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
 *         length: requried??
 */
@RunWith(Parameterized.class)
public class JCoMetaUtil4DateIT extends AbsFixedLengthTestJCoMetaUtil1 {

    public JCoMetaUtil4DateIT(ESAPParamType paramType, int length) {
        super(paramType, length, -1, -1, 8);
    }

    protected String getParamName() {
        return "date";
    }

    protected ESAPMetaType getMetaType() {
        return ESAPMetaType.DATE;
    }

}
