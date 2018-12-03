/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.nar.i18n;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.Restriction;
import org.apache.nifi.annotation.behavior.Stateful;
import org.apache.nifi.annotation.behavior.SystemResource;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.DeprecationNotice;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.bundle.Bundle;
import org.apache.nifi.components.AllowableValue;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.util.StringUtils;

/**
 * Support the i18n for properties of display names, descriptions, allowable values, tags, etc.
 *
 */
public class MessagesProvider {
    public static final String PATH_I18N = "i18n/";
    public static final String EXT = ".properties";

    private static final String PREFIX = "__";
    public static final String DESC_SUFFIX = ".description";
    public static final String DISPLAY_SUFFIX = ".displayName";

    public static final String KEY_DESC = PREFIX + CapabilityDescription.class.getSimpleName();
    public static final String KEY_TAGS = PREFIX + Tags.class.getSimpleName();
    public static final String KEY_MARK_VENDOR = PREFIX + Marks.class.getSimpleName() + ".vendor";
    public static final String KEY_MARK_CAT = PREFIX + Marks.class.getSimpleName() + ".categories";
    public static final String KEY_STATEFUL = PREFIX + Stateful.class.getSimpleName() + DESC_SUFFIX;
    public static final String KEY_DEPRECATION = PREFIX + DeprecationNotice.class.getSimpleName() + ".reason";

    private static Pattern WRONG_UNICODE_PATTERN = Pattern.compile(
            "([\\u007f-\\u009f]|\\u00ad|[\\u0483-\\u0489]|[\\u0559-\\u055a]|\\u058a|[\\u0591-\\u05bd]|\\u05bf|[\\u05c1-\\u05c2]|[\\u05c4-\\u05c7]|[\\u0606-\\u060a]|[\\u063b-\\u063f]|\\u0674|[\\u06e5-\\u06e6]|\\u070f|[\\u076e-\\u077f]|\\u0a51|\\u0a75|\\u0b44|[\\u0b62-\\u0b63]|[\\u0c62-\\u0c63]|[\\u0ce2-\\u0ce3]|[\\u0d62-\\u0d63]|\\u135f|[\\u200b-\\u200f]|[\\u2028-\\u202e]|\\u2044|\\u2071|[\\uf701-\\uf70e]|[\\uf710-\\uf71a]|\\ufb1e|[\\ufc5e-\\ufc62]|\\ufeff|\\ufffc)");

    private volatile static Locale defaultLocale;

    public static Locale getDefaultLocale() {
        if (defaultLocale == null) {
            return Locale.getDefault();
        }
        return defaultLocale;
    }

    public static void discoverExtensions(final Locale locale, final Set<Bundle> narBundles) {
        defaultLocale = locale;
        Locale.setDefault(locale);

        PropertiesExtensionManager.discoverExtensions(narBundles);
    }

    public static String fixKey(String key) {
        key = key.replaceAll("[ */:,;]", "_"); // underline
        key = key.replaceAll("[@#$%^&()<>\\[\\]{}'\"]", ""); // empty

        // \u200b, for GetFile
        key = replaceWrongUnicode(key, "");
        // TODO, need consider other cases
        return key;
    }

    private static String replaceWrongUnicode(String source, String replace) {
        if (StringUtils.isBlank(source)) {
            return source;
        }
        if (StringUtils.isBlank(replace)) {
            replace = "";
        }
        Matcher m = WRONG_UNICODE_PATTERN.matcher(source);
        if (m.find()) {
            return m.replaceAll(replace);
        }
        return source;
    }

    public static String getPropDisplayNameKey(String name) {
        return fixKey(name) + DISPLAY_SUFFIX;
    }

    public static String getPropDescKey(String name) {
        return fixKey(name) + DESC_SUFFIX;
    }

    public static String getRelationshipDescKey(String name) {
        return PREFIX + Relationship.class.getSimpleName() + '.' + fixKey(name) + DESC_SUFFIX;
    }

    public static String getReadsAttributeDescKey(String attribute) {
        return PREFIX + ReadsAttribute.class.getSimpleName() + '.' + fixKey(attribute) + DESC_SUFFIX;
    }

    public static String getWritesAttributeDescKey(String attribute) {
        return PREFIX + WritesAttribute.class.getSimpleName() + '.' + fixKey(attribute) + DESC_SUFFIX;
    }

    public static String getAllowableValueDisplayNameKey(String propName, String value) {
        return PREFIX + AllowableValue.class.getSimpleName() + '.' + fixKey(propName) + '.' + fixKey(value) + DISPLAY_SUFFIX;
    }

    public static String getAllowableValueDescKey(String propName, String value) {
        return PREFIX + AllowableValue.class.getSimpleName() + '.' + fixKey(propName) + '.' + fixKey(value) + DESC_SUFFIX;
    }

    public static String getSystemResourceDescKey(String name) {
        return PREFIX + SystemResource.class.getSimpleName() + '.' + fixKey(name) + DESC_SUFFIX;
    }

    public static String getRestrictionLabelKey(String name) {
        return PREFIX + Restriction.class.getSimpleName() + '.' + fixKey(name) + ".label";
    }

    public static String getRestrictionExplanationKey(String name) {
        return PREFIX + Restriction.class.getSimpleName() + '.' + fixKey(name) + ".explanation";
    }

    public static String getELScopeDescKey(String scope) {
        return ExpressionLanguageScope.class.getSimpleName() + '.' + scope + DESC_SUFFIX;
    }

    /**
     * Get the value of type with key, if locale is null, will no value directly.
     */

    public static String getValue(final Locale locale, final String type, final String key) {
        return PropertiesExtensionManager.getValue(locale, type, key);
    }

    /**
     * 
     * Get the description of type via key "__CapabilityDescription"
     */
    public static String getDescription(Locale locale, String type) {
        return getValue(locale, type, KEY_DESC);
    }

    /**
     * Get the tags of type via key "__Tags", also separate by comma
     */
    public static String getTags(Locale locale, String type) {
        return getValue(locale, type, KEY_TAGS);
    }

    public static Set<String> getTagsSet(Locale locale, String type) {
        final String tagsStr = getTags(locale, type);
        if (!StringUtils.isBlank(tagsStr)) {
            final String[] tagsArr = tagsStr.split(",");
            if (tagsArr != null && tagsArr.length > 0) {
                return new HashSet<>(Arrays.asList(tagsArr));
            }
        }
        return Collections.emptySet();
    }

    /**
     * Get the mark vendor of type via key "__Marks.vendor"
     */
    public static String getMarksVendor(final Locale locale, String type) {
        return getValue(locale, type, KEY_MARK_VENDOR);
    }

    /**
     * Get the mark categories of type via key "__Marks.categores", also separate by comma
     */
    public static String getMarksCategories(final Locale locale, String type) {
        return getValue(locale, type, KEY_MARK_CAT);
    }

    public static Set<String> getMarksCategoriesSet(final Locale locale, String type) {
        final String categoriesStr = getMarksCategories(locale, type);
        return convertCategoriesSet(categoriesStr);
    }

    private static Set<String> convertCategoriesSet(String categoriesStr) {
        if (!StringUtils.isBlank(categoriesStr)) {
            final String[] categoriesArr = categoriesStr.split(",");
            if (categoriesArr != null && categoriesArr.length > 0) {
                return new HashSet<>(Arrays.asList(categoriesArr));
            }
        }
        return Collections.emptySet();
    }

    /**
     * Get the property display name of type by property name via key "<property name>.displayName"
     */
    public static String getPropDisplayName(Locale locale, String type, String name) {
        return getValue(locale, type, getPropDisplayNameKey(name));
    }

    /**
     * Get the property description of type by property name via key "<property name>.description"
     */
    public static String getPropDesc(Locale locale, String type, String name) {
        return getValue(locale, type, getPropDescKey(name));
    }

    /**
     * Get the relation description of type by relation name via key "__Relationship.<relation name>.description"
     */
    public static String getRelationshipDesc(Locale locale, String type, String name) {
        return getValue(locale, type, getRelationshipDescKey(name));
    }

    /**
     * Get the read attribute description of type by attribute name via key "__ReadsAttribute.<attribute name>.description"
     */
    public static String getReadsAttributeDesc(Locale locale, String type, String attribute) {
        return getValue(locale, type, getReadsAttributeDescKey(attribute));
    }

    /**
     * Get the write attribute description of type by attribute name via key "__WritesAttribute.<attribute name>.description"
     */
    public static String getWritesAttributeDesc(Locale locale, String type, String attribute) {
        return getValue(locale, type, getWritesAttributeDescKey(attribute));
    }

    /**
     * Get the allowable display name of type by allowable value via key "__AllowableValue.<prop name>.<allowable value>.displayName"
     */
    public static String getAllowableValueDisplayName(Locale locale, String type, String propName, String value) {
        return getValue(locale, type, getAllowableValueDisplayNameKey(propName, value));
    }

    /**
     * Get the allowable description of type by allowable value via key "__AllowableValue.<prop name>.<allowable value>.description"
     */
    public static String getAllowableValueDesc(Locale locale, String type, String propName, String value) {
        return getValue(locale, type, getAllowableValueDescKey(propName, value));
    }

    /**
     * Get the system resource description of type by resource name via key "__SystemResource.<resource name>.description"
     */
    public static String getSystemResourceDesc(Locale locale, String type, String name) {
        return getValue(locale, type, getSystemResourceDescKey(name));
    }

    /**
     * Get the restriction label of type by name via key "__Restriction.<name>.label"
     */
    public static String getRestrictionLabel(Locale locale, String type, String name) {
        return getValue(locale, type, getRestrictionLabelKey(name));
    }

    /**
     * Get the explanation of type by name via key "__Restriction.<name>.explanation"
     */
    public static String getRestrictionExplanation(Locale locale, String type, String name) {
        return getValue(locale, type, getRestrictionExplanationKey(name));
    }

    /**
     * Get the stateful description of type by name via key "__Stateful.description"
     */
    public static String getStatefulDesc(Locale locale, String type) {
        return getValue(locale, type, KEY_STATEFUL);
    }

    /**
     * Get the deprecation reason of type by name via key "__DeprecationNotice.reason"
     */
    public static String getDeprecationReason(Locale locale, String type) {
        return getValue(locale, type, KEY_DEPRECATION);
    }

    public static String getFrameworkValue(Locale locale, final String key) {
        return getValue(locale, "framework", key);
    }
}
