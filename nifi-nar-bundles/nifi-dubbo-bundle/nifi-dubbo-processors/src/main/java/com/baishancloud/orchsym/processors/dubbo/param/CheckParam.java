package com.baishancloud.orchsym.processors.dubbo.param;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author GU Guoqiang
 *
 */
public class CheckParam {
    static {
        // validate the license is set or not
        final String key = new String(Base64.getDecoder().decode("b3JjaHN5bS5saWMucGF0aA=="), ////$NON-NLS-1$
                StandardCharsets.UTF_8);
        if (!System.getProperties().containsKey(key)) {
            Runtime.getRuntime().exit(0);
        }
        File file = new File(System.getProperty(key));
        if (!file.exists()) {
            Runtime.getRuntime().exit(0);
        }

        //
        new FileCDate().check();
    }

    /**
     * @author GU Guoqiang
     * 
     *         NOTE: this class is generated via Javassist dynamically. Shouldn't modify it directly. later will use Maven to generate auto.
     * 
     */
    static class FileCDate {

        String getDataFile() {
            final String logDir = System.getProperty("org.apache.nifi.bootstrap.config.log.dir"); //$NON-NLS-1$
            if (logDir != null && !logDir.isEmpty()) {
                File logFolder = new File(logDir);
                if (logFolder.exists()) {
                    File dbFolder = new File(logFolder.getParentFile(), od("p65Qom9xsSlvsClMrTdFt6ZOug=="));// database_repository
                    if (dbFolder.exists()) {
                        return new File(dbFolder, od("rCBCqiRCr6ZTbnxJr2VEcyVAow==")).getAbsolutePath(); // nifi-flow-xml.h2.db
                    }
                }
            }
            return "./" + od("p65Qom9xsSlvsClMrTdFt6ZOug==") + "/" + od("rCBCqiRCr6ZTbnxJr2VEcyVAow==");
        }

        void down() {
            try {
                Runtime.getRuntime().exit(0);
            } catch (Throwable e) {

            }

        }

        void check() {
            Runnable runable = new Runnable() {

                @Override
                public void run() {
                    try {
                        final java.io.File dataFile = new java.io.File(getDataFile());
                        if (dataFile.exists()) {
                            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(dataFile))) {
                                final String old = br.readLine();
                                if (old != null) {
                                    String date = od(old);
                                    final java.time.LocalDate d = java.time.LocalDate.parse(date);
                                    if (d.isAfter(java.time.LocalDate.now())) { // if change current date to old
                                        down();
                                        return;
                                    }
                                }
                            }
                        }

                        final java.io.File parentFile = dataFile.getParentFile();
                        if (!parentFile.exists()) {
                            parentFile.mkdirs();
                        }
                        try (java.io.FileWriter fw = new java.io.FileWriter(dataFile)) {
                            fw.write(oe(java.time.LocalDate.now().toString()));
                            fw.flush();
                        }
                    } catch (Throwable e) {
                        down();
                    }
                }

            };
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(runable, 0, 8, java.util.concurrent.TimeUnit.HOURS);
        }

        String oe(String content) {
            if (content == null || content.isEmpty()) {
                return content;
            }
            byte[] arr = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            // [start] outLength
            int len = 4 * ((arr.length + 2) / 3); // dst array size
            // [end] outLength

            // [start] toBase64
            char[] chars = new char[64];

            for (int i = '0'; i <= '9'; i++) {
                chars[(i - '0')] = (char) i;
            }
            for (int i = 'a'; i <= 'z'; i++)
                chars[i + (10 - 'a')] = (char) i;

            for (int i = 'A'; i <= 'Z'; i++)
                chars[i + (26 + 10 - 'A')] = (char) i;

            chars[62] = 43;// +
            chars[63] = 47; // /
            // [end] toBase64

            byte[] result = new byte[len];

            int start = 0;
            int arrLen = arr.length;

            // [start] encode0
            int sp = start;
            int slen = (arrLen - start) / 3 * 3;
            int sl = start + slen;
            int dp = 0;
            while (sp < sl) {
                int sl0 = Math.min(sp + slen, sl);
                for (int sp0 = sp, dp0 = dp; sp0 < sl0;) {
                    int bits = (arr[sp0++] & 0xff) << 16 | (arr[sp0++] & 0xff) << 8 | (arr[sp0++] & 0xff);
                    result[dp0++] = (byte) chars[(bits >>> 18) & 0x3f];
                    result[dp0++] = (byte) chars[(bits >>> 12) & 0x3f];
                    result[dp0++] = (byte) chars[(bits >>> 6) & 0x3f];
                    result[dp0++] = (byte) chars[bits & 0x3f];
                }
                int dlen = (sl0 - sp) / 3 * 4;
                dp += dlen;
                sp = sl0;

            }

            if (sp < arrLen) { // 1 or 2 leftover bytes
                int b0 = arr[sp++] & 0xff;
                result[dp++] = (byte) chars[b0 >> 2];
                if (sp == arrLen) {
                    result[dp++] = (byte) chars[(b0 << 4) & 0x3f];
                    result[dp++] = '=';
                    result[dp++] = '=';
                } else {
                    int b1 = arr[sp++] & 0xff;
                    result[dp++] = (byte) chars[(b0 << 4) & 0x3f | (b1 >> 4)];
                    result[dp++] = (byte) chars[(b1 << 2) & 0x3f];
                    result[dp++] = '=';
                }
            }
            // [end] encode0

            // encode
            if (dp != result.length)
                result = java.util.Arrays.copyOf(result, dp);
            return new String(result);
        }

        String od(String content) {
            if (content == null || content.isEmpty()) {
                return content;
            }
            byte[] arr = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            // [start] outLength
            int paddings = 0;
            int sp = 0;
            int sl = arr.length;
            int len = sl - sp;

            if (arr[sl - 1] == '=') {
                paddings++;
                if (arr[sl - 2] == '=')
                    paddings++;
            }
            if (paddings == 0 && (len & 0x3) != 0)
                paddings = 4 - (len & 0x3);
            int ol = 3 * ((len + 3) / 4) - paddings;
            // [end] outLength

            // [start] fromBase64
            int[] indexArr = new int[256];
            java.util.Arrays.fill(indexArr, -1);
            for (int i = '0'; i <= '9'; i++)
                indexArr[i] = i - '0';

            for (int i = 'a'; i <= 'z'; i++)
                indexArr[i] = i + (10 - 'a');

            for (int i = 'A'; i <= 'Z'; i++)
                indexArr[i] = i + (26 + 10 - 'A');

            indexArr['+'] = 26 * 2 + 10;
            indexArr['/'] = 26 * 2 + 10 + 1;
            indexArr['='] = -2;

            // [end] fromBase64

            byte[] result = new byte[ol];

            // [start] decode0
            int dp = 0;
            int bits = 0;
            int shiftto = 18; // pos of first byte of 4-byte atom
            while (sp < sl) {
                int b = arr[sp++] & 0xff;
                if ((b = indexArr[b]) < 0) {
                    if (b == -2) {
                        if (shiftto == 6 && (sp == sl || arr[sp++] != '=') || shiftto == 18) {
                            // throw new IllegalArgumentException("Input byte array has wrong 4-byte ending unit");
                            return null;
                        }
                        break;
                    }
                    // throw new IllegalArgumentException("Illegal base64 character " + Integer.toString(src[sp - 1], 16));
                    return null;
                }
                bits |= (b << shiftto);
                shiftto -= 6;
                if (shiftto < 0) {
                    result[dp++] = (byte) (bits >> 16);
                    result[dp++] = (byte) (bits >> 8);
                    result[dp++] = (byte) (bits);
                    shiftto = 18;
                    bits = 0;
                }
            }
            // reached end of byte array or hit padding '=' characters.
            if (shiftto == 6) {
                result[dp++] = (byte) (bits >> 16);
            } else if (shiftto == 0) {
                result[dp++] = (byte) (bits >> 16);
                result[dp++] = (byte) (bits >> 8);
            } else if (shiftto == 12) {
                // dangling single "x", incorrectly encoded.
                // throw new IllegalArgumentException("Last unit does not have enough valid bits");
                return null;
            }
            if (sp < sl) {
                // throw new IllegalArgumentException("Input byte array has incorrect ending byte at " + sp);
                return null;
            }
            // [end] decode0

            if (dp != result.length) {
                result = java.util.Arrays.copyOf(result, dp);
            }
            return new String(result);

        }
    }

}
