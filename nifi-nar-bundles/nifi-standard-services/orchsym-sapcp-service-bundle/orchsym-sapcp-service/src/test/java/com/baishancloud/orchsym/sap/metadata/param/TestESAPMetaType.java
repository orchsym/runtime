package com.baishancloud.orchsym.sap.metadata.param;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author GU Guoqiang
 *
 */
public class TestESAPMetaType {

    @Test
    public void defaultCharType() {
        final ESAPMetaType metaType = ESAPMetaType.get("ABC");
        Assert.assertEquals(ESAPMetaType.CHAR, metaType);

        final ESAPMetaType jcoMetaType = ESAPMetaType.get(1000);
        Assert.assertEquals(ESAPMetaType.CHAR, jcoMetaType);
    }

}
