package com.baishancloud.orchsym.sap.metadata.param;

import org.apache.commons.lang3.StringUtils;

/**
 * @author GU Guoqiang
 *
 */
public enum ESAPTabType {
    INPUT, OUTPUT, IN_OUT,;

    public String getLowerName() {
        return name().toLowerCase();
    }

    public static ESAPTabType get(String name) {
        if (StringUtils.isNotBlank(name)) {
            // return ESAPTabType.valueOf(value.toUpperCase()); //have exception
            for (ESAPTabType t : ESAPTabType.values()) {
                if (t.name().equalsIgnoreCase(name)) {
                    return t;
                }
            }
        }
        return null;
    }
}
