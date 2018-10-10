package com.baishancloud.orchsym.processors.soap.model;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.components.AllowableValue;

import com.baishancloud.orchsym.processors.soap.i18n.Messages;

/**
 * @author GU Guoqiang
 *
 */
public enum EEnvelopeOptions {

    RAW(Messages.getString("EEnvelopeOptions.raw")), //$NON-NLS-1$
    BODY(Messages.getString("EEnvelopeOptions.onlyBody")), //$NON-NLS-1$
    JSON(Messages.getString("EEnvelopeOptions.jsonBody")); //$NON-NLS-1$
    private String displayName;

    private EEnvelopeOptions(String displayName) {
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
        return Arrays.asList(EEnvelopeOptions.values()).stream().map(v -> new AllowableValue(v.getValue(), v.getDisplayName())).toArray(AllowableValue[]::new);
    }

    public static EEnvelopeOptions get(String name) {
        if (StringUtils.isNotBlank(name))
            for (EEnvelopeOptions op : EEnvelopeOptions.values())
                if (op.getValue().toUpperCase().equals(name.toUpperCase()))
                    return op;
        return null;
    }
}
