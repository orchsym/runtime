package com.baishancloud.orchsym.sap.option;

import java.util.Arrays;

import org.apache.nifi.components.AllowableValue;

/**
 * @author GU Guoqiang
 *
 */
public enum ESAPServerType {
    AS("Application Server"), //$NON-NLS-1$
    ASP("Application Server with Pool"), //$NON-NLS-1$
    MS("Message Server"), //$NON-NLS-1$
    ;
    private String displayName;

    private ESAPServerType(String displayName) {
        this.displayName = displayName;
    }

    public String getValue() {
        return name();
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AllowableValue[] getAll() {
        return Arrays.asList(ESAPServerType.values()).stream().map(v -> new AllowableValue(v.getValue(), v.getDisplayName())).toArray(AllowableValue[]::new);
    }

    public static ESAPServerType indexOf(String value) {
        if (value != null) {
            return ESAPServerType.valueOf(value.toUpperCase());
        }
        return ESAPServerType.AS;
    }
}
