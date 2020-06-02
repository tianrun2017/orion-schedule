package com.orion.schedule.progress.util;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.security.SecureRandom;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/7/25 10:25
 * @Version 1.0.0
 */
public class ScheduleEncrypt {
    static String key = "abcid12348";

    /**
     * encryt use the given key
     *
     * @param key
     * @param content
     * @return
     */
    public static String encrypt(String key, String content) {
        try {
            SecureRandom random = new SecureRandom();
            DESKeySpec desKey = new DESKeySpec(key.getBytes());
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey securekey = keyFactory.generateSecret(desKey);
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, securekey, random);
            byte[] result = cipher.doFinal(content.getBytes());
            Base64 base64 = new Base64();
            String s = base64.encodeAsString(result);
            return s;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * encrypt use the default key
     *
     * @param content
     * @return
     */
    public static String encrypt(String content) {
        return encrypt(key, content);
    }

    /**
     * decrypt use the default key
     *
     * @param content
     * @return
     */
    public static String decrypt(String content) {
        return decrypt(key, content);
    }

    /**
     * 解密
     *
     * @param content 待解密内容
     * @return
     */
    public static String decrypt(String key, String content) {
        try {
            byte[] decode = new Base64().decode(content);
            SecureRandom random = new SecureRandom();
            DESKeySpec desKey = new DESKeySpec(key.getBytes());
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey securekey = keyFactory.generateSecret(desKey);
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.DECRYPT_MODE, securekey, random);
            byte[] result = cipher.doFinal(decode);
            return new String(result);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}
