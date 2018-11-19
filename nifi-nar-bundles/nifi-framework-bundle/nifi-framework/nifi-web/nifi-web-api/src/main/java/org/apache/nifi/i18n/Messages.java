package org.apache.nifi.i18n;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.nifi.util.StringUtils;

/**
 * 
 * @author GU Guoqiang
 *
 */
public final class Messages {
    private static final String BUNDLE_NAME = "messages"; //$NON-NLS-1$

    private static final ResourceBundle DEFAULT_RES_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);
    private static final Map<Locale, ResourceBundle> RES_BUNDLE_MAP = new HashMap<>();

    private Messages() {
    }

    // public static String getString(String key, final Object... args) {
    // return getString(MessagesProvider.currentLocale.get(), key, args);
    // }

    public static String getString(Locale locale, String key, final Object... args) {
        if (locale == null) {
            locale = Locale.ENGLISH; // default
        }
        ResourceBundle resourceBundle = RES_BUNDLE_MAP.get(locale);
        if (resourceBundle == null) {
            synchronized (BUNDLE_NAME) {
                if (resourceBundle == null) {
                    final Locale oldDefLocale = Locale.getDefault();
                    try {
                        Locale.setDefault(locale); // make sure the fallbackLocale is same as current one.

                        resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
                        if (resourceBundle != null) {
                            RES_BUNDLE_MAP.put(locale, resourceBundle);
                        }
                    } catch (MissingResourceException e) {
                        //
                    } finally {
                        if (oldDefLocale != null) {
                            Locale.setDefault(oldDefLocale);
                        }
                    }
                }
            }
        }

        try {
            if (resourceBundle != null) {
                final String value = getString(resourceBundle, key, args);
                if (!StringUtils.isEmpty(value)) {
                    return value;
                }
            }
        } catch (MissingResourceException e) {
            //
        }

        try {
            return getString(DEFAULT_RES_BUNDLE, key, args);
        } catch (MissingResourceException e) {
            return '!' + key + '!'; // also not found from default
        }

    }

    private static String getString(ResourceBundle resBundle, String key, final Object... args) {
        try {
            return MessageFormat.format(resBundle.getString(key), args);
        } catch (MissingResourceException e) {
            return null;
        }
    }
}
