package org.apache.nifi.nar.i18n;

/**
 * @author GU Guoqiang
 *
 */
public class LanguageHelper {
    /**
     * 根据Unicode编码判断中文汉字和符号
     */
    private static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS //
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS //
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A//
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B //
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION//
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS //
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION) {//
            return true;
        }
        return false;
    }

    // private static boolean isJapanese(char c) {
    // Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
    // if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS // ?? same as Chinese?
    // || ub == Character.UnicodeBlock.HIRAGANA //
    // || ub == Character.UnicodeBlock.KATAKANA) {//
    // return true;
    // }
    // return false;
    // }

    /**
     * 判断是否包含中文汉字和符号
     */
    public static boolean isChinese(String strName) {
        char[] ch = strName.toCharArray();
        for (int i = 0; i < ch.length; i++) {
            char c = ch[i];
            if (isChinese(c)) {
                return true;
            }
        }
        return false;
    }

}
