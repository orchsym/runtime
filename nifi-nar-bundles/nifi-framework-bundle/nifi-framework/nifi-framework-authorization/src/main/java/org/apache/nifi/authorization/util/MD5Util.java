package org.apache.nifi.authorization.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MD5Util {

    private static final Logger logger = LoggerFactory.getLogger(MD5Util.class);

    public static String MD5(String str) {
        if (str == null) {
            return null;
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            return byte2HexString(messageDigest.digest(str.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            logger.warn("Throw exception when generate md5 for "+str,e);
            return "";
        }
    }

    private static String byte2HexString(byte[] data) {
        StringBuffer checksumSb = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            String hexStr = Integer.toHexString(0x00ff & data[i]);
            if (hexStr.length() < 2) {
                checksumSb.append("0");
            }
            checksumSb.append(hexStr);
        }
        return checksumSb.toString();
    }
}
