package com.baishancloud.orchsym.processors.sap.option;

import java.util.Arrays;

import org.apache.nifi.components.AllowableValue;

import com.baishancloud.orchsym.processors.sap.i18n.Messages;

public enum ResponseOption {
    FLOW("flow", Messages.getString("ResponseOption.flow")), //$NON-NLS-1$ //$NON-NLS-2$
    CUSTOM("custom", Messages.getString("ResponseOption.custom")), //$NON-NLS-1$ //$NON-NLS-2$
    ;
    private String value;
    private String displayName;

    private ResponseOption(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ResponseOption get(String value) {
        if (value != null) {
            for (ResponseOption op : ResponseOption.values()) {
                if (op.getValue().equalsIgnoreCase(value)) {
                    return op;
                }
            }
        }
        return ResponseOption.CUSTOM;
    }

    public static AllowableValue[] getAll() {
        return Arrays.asList(ResponseOption.values()).stream().map(c -> new AllowableValue(c.value, c.displayName)).toArray(AllowableValue[]::new);
    }
}
