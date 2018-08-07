package com.baishancloud.orchsym.sap.option;

import java.util.Arrays;

import org.apache.nifi.components.AllowableValue;

import com.baishancloud.orchsym.sap.i18n.Messages;

/**
 * 
 * ISO two-character language code (for example, EN, DE, FR), or SAP-specific single-character language code.
 * 
 * @author GU Guoqiang
 */
public enum ESAPLanguage {

    ZH(Messages.getString("SAPLanguage.ZH")), // //$NON-NLS-1$
    EN(Messages.getString("SAPLanguage.EN")), // //$NON-NLS-1$
    DE(Messages.getString("SAPLanguage.DE")), // //$NON-NLS-1$
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
