package com.baishancloud.orchsym.processors.sap.option;

import java.util.Arrays;

import org.apache.nifi.components.AllowableValue;

import com.baishancloud.orchsym.processors.sap.i18n.Messages;

/**
 * @author GU Guoqiang
 *
 */
public enum BoolOption {

    YES(true, Messages.getString("BoolOption.yes")), //$NON-NLS-1$ //$NON-NLS-2$
    NO(false, Messages.getString("BoolOption.no")), //$NON-NLS-1$ //$NON-NLS-2$
    ;
    private boolean value;
    private String displayName;

    private BoolOption(boolean value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return Boolean.toString(value);
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AllowableValue[] getAll() {
        return Arrays.asList(BoolOption.values()).stream().map(c -> new AllowableValue(c.getValue(), c.getDisplayName())).toArray(AllowableValue[]::new);
    }

}
