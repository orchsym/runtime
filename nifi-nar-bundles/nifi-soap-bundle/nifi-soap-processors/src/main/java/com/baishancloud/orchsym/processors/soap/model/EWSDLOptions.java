package com.baishancloud.orchsym.processors.soap.model;

import java.util.Arrays;

import org.apache.nifi.components.AllowableValue;

/**
 * @author GU Guoqiang
 *
 */
public enum EWSDLOptions {

    URI, //
    CONTENTS("Custom Contents"); //$NON-NLS-1$
    private String displayName;

    private EWSDLOptions() {
        //
    }

    private EWSDLOptions(String displayName) {
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

    public static AllowableValue[] getAll() {
        return Arrays.asList(EWSDLOptions.values()).stream().map(v -> new AllowableValue(v.getValue(), v.getDisplayName())).toArray(AllowableValue[]::new);
    }

}
