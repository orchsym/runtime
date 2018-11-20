package com.baishancloud.orchsym.processors.soap.model;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.components.AllowableValue;

/**
 * @author GU Guoqiang
 *
 */
public enum EInputOptions {

    RAW("Raw contents"), //$NON-NLS-1$
    BODY("Contents as Body"); //$NON-NLS-1$
    private String displayName;

    private EInputOptions(String displayName) {
        this.displayName = displayName;
    }

    public String getValue() {
        return name();
    }

    public String getDisplayName() {
        if (displayName == null) {
            return getValue();
        }
        return displayName;
    }

    public static EInputOptions get(String value) {
        if (StringUtils.isNoneBlank(value))
            for (EInputOptions op : EInputOptions.values())
                if (op.getValue().equals(value))
                    return op;

        return null;
    }

    public static AllowableValue[] getAll() {
        return Arrays.asList(EInputOptions.values()).stream().map(v -> new AllowableValue(v.getValue(), v.getDisplayName())).toArray(AllowableValue[]::new);
    }

}
