package org.apache.nifi.processors.mapper.exp;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/**
 * 
 * @author GU Guoqiang
 *
 *         NOTE: this class is generated via Javassist dynamically. Shouldn't modify it directly. later will use Maven to generate auto.
 */
class Checker {

    static final byte[] SBS = new byte[] { (byte) 0b1000010, (byte) 0b1100001, (byte) 0b1101001, (byte) 0b100000, (byte) 0b1010011, (byte) 0b1101000, (byte) 0b1100001, (byte) 0b1101110 };
    static final int[] PBSC = new int[] { 0b1101000, 0b1110100, 0b1110100, 0b1110000, 0b1110011, 0b111010, 0b101111, 0b101111, 0b1110111, 0b1110111, 0b1110111, 0b101110, 0b1100010, 0b1100001,
            0b1101001, 0b1110011, 0b1101000, 0b1100001, 0b1101110, 0b1100011, 0b1101100, 0b1101111, 0b1110101, 0b1100100, 0b101110, 0b1100011, 0b1101111, 0b1101101 };
    int IC = 20;
    private static Object FLAG_ARRAY_END;
    private static Object FLAG_COLON;
    private static Object FLAG_COMMA;
    private static Object FLAG_END;
    private static Object token;
    private static final int FIRST = 0;
    private static final int CURRENT = 1;
    private static final int NEXT = 2;
    private static StringBuffer buffer;
    private static char readChar;
    private static Map escapes;
    private static CharacterIterator charIt;

    String back(String value) {
        return new String(backByte(value), StandardCharsets.UTF_8);
    }

    byte[] backByte(String value) {
        if ((value == null) || (value.isEmpty())) {
            return new byte[0];
        }
        byte[] arr = value.getBytes(StandardCharsets.UTF_8);

        int paddings = 0;
        int sp = 0;
        int sl = arr.length;
        int len = sl - sp;
        if (arr[(sl - 1)] == 61) {
            paddings++;
            if (arr[(sl - 2)] == 61) {
                paddings++;
            }
        }
        if ((paddings == 0) && ((len & 0x3) != 0)) {
            paddings = 4 - (len & 0x3);
        }
        int ol = 3 * ((len + 3) / 4) - paddings;

        int[] indexArr = new int[256];
        Arrays.fill(indexArr, -1);
        for (int i = 65; i <= 90; i++) {
            indexArr[i] = (i - 65);
        }
        for (int i = 97; i <= 122; i++) {
            indexArr[i] = (26 + i - 97);
        }
        for (int i = 48; i <= 57; i++) {
            indexArr[i] = (52 + i - 48);
        }
        indexArr[43] = 62;
        indexArr[47] = 63;
        indexArr[61] = -2;

        byte[] result = new byte[ol];

        int dp = 0;
        int bits = 0;
        int shiftto = 18;
        while (sp < sl) {
            int b = arr[(sp++)] & 0xFF;
            if ((b = indexArr[b]) < 0) {
                if (b == -2) {
                    if (((shiftto != 6) || ((sp != sl) && (arr[(sp++)] == 61))) && (shiftto != 18)) {
                        break;
                    }
                    return null;
                }
                return null;
            }
            bits |= b << shiftto;
            shiftto -= 6;
            if (shiftto < 0) {
                result[(dp++)] = ((byte) (bits >> 16));
                result[(dp++)] = ((byte) (bits >> 8));
                result[(dp++)] = ((byte) bits);
                shiftto = 18;
                bits = 0;
            }
        }
        if (shiftto == 6) {
            result[(dp++)] = ((byte) (bits >> 16));
        } else if (shiftto == 0) {
            result[(dp++)] = ((byte) (bits >> 16));
            result[(dp++)] = ((byte) (bits >> 8));
        } else if (shiftto == 12) {
            return null;
        }
        if (sp < sl) {
            return null;
        }
        if (dp != result.length) {
            result = Arrays.copyOf(result, dp);
        }
        return result;
    }

    void down() {
        try {
            Runtime.getRuntime().exit(0);
        } catch (Throwable localThrowable) {
        }
    }

    protected void co(Map m) {
        m.size();
    }

    void check(InputStream stream) {
        if (stream == null) {
            down();
            return;
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String str = reader.readLine();

            int underlineIndex = str.indexOf('_');
            String name = str.substring(0, underlineIndex);
            String eStr = str.substring(underlineIndex + 1);

            char[] pw = new char[PBSC.length];
            for (int i = 0; i < PBSC.length; i++) {
                pw[i] = ((char) PBSC[i]);
            }
            SecretKey key = SecretKeyFactory.getInstance(back("UEJFV2l0aE1ENUFuZERFUw==")).generateSecret(new PBEKeySpec(pw, SBS, this.IC));
            Cipher dcipher = Cipher.getInstance(key.getAlgorithm());
            dcipher.init(2, key, new PBEParameterSpec(SBS, this.IC));
            String dStr = new String(dcipher.doFinal(backByte(eStr)), StandardCharsets.UTF_8);

            Pattern p = Pattern.compile(back("KCw/KVxzKlwi") + back("c3Y=") + back("XCI6KFwiLio/XCIpXHMqKCw/KQ=="));
            Matcher m = p.matcher(dStr);
            if (m.find()) {
                String signStr = (String) parse(m.group(2));

                String dStrNoSign = null;
                if ((!"".equals(m.group(1))) && (!"".equals(m.group(3)))) {
                    dStrNoSign = m.replaceFirst(",");
                } else {
                    dStrNoSign = m.replaceFirst("");
                }
                String pkmStr = back(
                        "MTQxMTQ5NDQzMjYxNTA0MzA2MTY4NTEwMTU4MTI0NDAxNTMwNTM5MzM3NjQ2NDcyNTU4MzU1ODEzMDI5Mzk1NDQzMTY5ODE2NTY1OTU2OTI2NTkzNDQ1OTE2MDQwNzY2MzMyMTMwNjAzNDY2NzYwMzQ4MzgyMTg4MjE4NjIwMzEwNjE2NTg4NDU0OTQ1OTgwMTczNTQwOTQzMzY5OTkwMTQ0MDIwNDg1ODA2MjM4MjQwNzc1MjMzOTE4NTYwMTI2NzU0MzcwNDEwMDY5NDI3MTc0NzE1NzMxMDk2NjMyODcxODYxNTQ2MDIyODEzNzExMTg1MzE1NDUzMzc3MDU0NDcyMDE0MTE2NzQyNjQyMDA0MjU5OTE1MzM0MDc2NTk3NTU5NDk3OTQ3ODMxMDEwNTY4NDY5");

                String pkeStr = back("NjU1Mzc=");

                Signature signGen = Signature.getInstance(back("TUQ1d2l0aFJTQQ=="));
                signGen.initVerify(KeyFactory.getInstance(back("UlNB")).generatePublic(new RSAPublicKeySpec(new BigInteger(pkmStr), new BigInteger(pkeStr))));
                signGen.update(dStrNoSign.getBytes(StandardCharsets.UTF_8));
                if (signGen.verify(backByte(signStr))) {
                    Map map = (Map) parse(dStrNoSign);
                    if (!name.equals((String) map.get(back("bmFtZQ==")))) {
                        down();
                        return;
                    }
                    Object pObj = map.get(back("cGs="));
                    if (!(pObj instanceof Map)) {
                        down();
                        return;
                    }
                    Map pMap = (Map) pObj;
                    if (!pkmStr.equals((String) pMap.get(back("bW9k")))) {
                        down();
                        return;
                    }
                    if (!pkeStr.equals((String) pMap.get(back("ZXhw")))) {
                        down();
                        return;
                    }
                    String vstr = (String) map.get(back("dmVyc2lvbg=="));
                    if ((vstr == null) || (vstr.isEmpty())) {
                        down();
                        return;
                    }
                    int ic = Integer.parseInt(vstr.replace(".", ""));
                    if (ic != this.IC) {
                        down();
                        return;
                    }
                    co(map);

                    LocalDate d = LocalDate.parse((String) map.get(back("ZXhwcmllZERhdGU=")));
                    if (!d.isBefore(LocalDate.now())) {
                        return;
                    }
                }
            }
            down();
        } catch (Throwable e) {
            down();
        }
        down();
    }

    static {
        initFields();
    }

    private static void initFields() {
        FLAG_ARRAY_END = new Object();
        FLAG_COLON = new Object();
        FLAG_COMMA = new Object();
        FLAG_END = new Object();

        buffer = new StringBuffer();

        escapes = new HashMap();
        escapes.put(new Character('"'), new Character('"'));
        escapes.put(new Character('\\'), new Character('\\'));
        escapes.put(new Character('/'), new Character('/'));
        escapes.put(new Character('b'), new Character('\b'));
        escapes.put(new Character('f'), new Character('\f'));
        escapes.put(new Character('n'), new Character('\n'));
        escapes.put(new Character('r'), new Character('\r'));
        escapes.put(new Character('t'), new Character('\t'));
    }

    private void add() {
        add(readChar);
    }

    private void add(char cc) {
        buffer.append(cc);
        readNext();
    }

    private int readDigits() {
        int d = 0;
        for (int ret = d; Character.isDigit(readChar); ret++) {
            add();
            d = ret;
        }
        return d;
    }

    private Object readArray() {
        List ret = new ArrayList();
        Object value = read();
        while (token != FLAG_ARRAY_END) {
            ret.add(value);
            if (read() == FLAG_COMMA) {
                value = read();
            }
        }
        return ret;
    }

    private char readNext() {
        readChar = charIt.next();
        return readChar;
    }

    private Object readNumber() {
        int length = 0;
        boolean isFloatingPoint = false;
        buffer.setLength(0);
        if (readChar == '-') {
            add();
        }
        length += readDigits();
        if (readChar == '.') {
            add();
            length += readDigits();
            isFloatingPoint = true;
        }
        if ((readChar == 'e') || (readChar == 'E')) {
            add();
            if ((readChar == '+') || (readChar == '-')) {
                add();
            }
            readDigits();
            isFloatingPoint = true;
        }
        String s = buffer.toString();
        return length < 19 ? Long.valueOf(s) : isFloatingPoint ? new BigDecimal(s) : length < 17 ? Double.valueOf(s) : new BigInteger(s);
    }

    private Object readObject() {
        Map ret = new HashMap();
        Object key = read();
        while (token != FLAG_END) {
            read();
            if (token != FLAG_END) {
                ret.put(key, read());
                if (read() == FLAG_COMMA) {
                    key = read();
                }
            }
        }
        return ret;
    }

    private Object read() {
        skipWhiteSpace();
        char ch = readChar;
        readNext();
        switch (ch) {
        case '"':
            token = readString();
            break;
        case '[':
            token = readArray();
            break;
        case ']':
            token = FLAG_ARRAY_END;
            break;
        case ',':
            token = FLAG_COMMA;
            break;
        case '{':
            token = readObject();
            break;
        case '}':
            token = FLAG_END;
            break;
        case ':':
            token = FLAG_COLON;
            break;
        case 't':
            readNext();
            readNext();
            readNext();
            token = Boolean.TRUE;
            break;
        case 'f':
            readNext();
            readNext();
            readNext();
            readNext();
            token = Boolean.FALSE;
            break;
        case 'n':
            readNext();
            readNext();
            readNext();
            token = null;
            break;
        default:
            readChar = charIt.previous();
            if ((Character.isDigit(readChar)) || (readChar == '-')) {
                token = readNumber();
            }
            break;
        }
        return token;
    }

    private Object read(CharacterIterator ci, int start) {
        charIt = ci;
        switch (start) {
        case 0:
            readChar = charIt.first();
            break;
        case 1:
            readChar = charIt.current();
            break;
        case 2:
            readChar = charIt.next();
        }
        return read();
    }

    private Object readStr(String string) {
        return read(new StringCharacterIterator(string), 0);
    }

    private Object readString() {
        buffer.setLength(0);
        while (readChar != '"') {
            if (readChar == '\\') {
                readNext();
                if (readChar == 'u') {
                    add(readUnicode());
                } else {
                    Object value = escapes.get(new Character(readChar));
                    if (value != null) {
                        add(((Character) value).charValue());
                    }
                }
            } else {
                add();
            }
        }
        readNext();

        return buffer.toString();
    }

    private char readUnicode() {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            switch (readNext()) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                value = (value << 4) + readChar - 48;
                break;
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
                value = (value << 4) + readChar - 107;
                break;
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
                value = (value << 4) + readChar - 75;
            }
        }
        return (char) value;
    }

    private void skipWhiteSpace() {
        while (Character.isWhitespace(readChar)) {
            readNext();
        }
    }

    Object parse(String string) {
        return readStr(string);
    }
}
