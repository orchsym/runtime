package com.baishancloud.orchsym.sap.i18n;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * 
 * @author GU Guoqiang
 *
 */
public class Messages {
    private static final String BUNDLE_NAME = "messages"; //$NON-NLS-1$

    private static final ResourceBundle RES_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    private Messages() {
    }

    public static String getString(String key, final Object... args) {
        try {
            return MessageFormat.format(RES_BUNDLE.getString(key), args);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }
}
