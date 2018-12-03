package com.baishancloud.orchsym.sap.option;

import java.util.Arrays;

import org.apache.nifi.components.AllowableValue;

/**
 * 
 * ISO two-character language code (for example, EN, DE, FR), or SAP-specific single-character language code.
 * 
 * @author GU Guoqiang
 */
public enum ESAPLanguage {

    ZH("Chinese"), // //$NON-NLS-1$
    EN("English"), // //$NON-NLS-1$
    DE("German"), // //$NON-NLS-1$
    ;
    private String displayName;

    private ESAPLanguage(String displayName) {
        this.displayName = displayName;
    }

    public String getValue() {
        return name();
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AllowableValue[] getAll() {
        return Arrays.asList(ESAPLanguage.values()).stream().map(v -> new AllowableValue(v.getValue(), v.getDisplayName())).toArray(AllowableValue[]::new);
    }

}
