package de.unijena.bioinf.ms.frontend.core;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 07.10.16.
 */

import de.unijena.bioinf.babelms.utils.Base64;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Properties;

//import sun.misc.BASE64Decoder;
//import sun.misc.BASE64Encoder;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class PasswordCrypter {

    //TODO for that stuff there is a new API in JDK-8
    private static final char[] PASSWORD = "enfldsgbnlsngdlksdsgm".toCharArray();
    private static final String METHOD = "PBEWithMD5AndDES";
    private static final byte[] SALT = {
            (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
            (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
    };

    public static void main(String[] args) throws Exception {
        String originalPassword = "secret";
        System.out.println("Original password: " + originalPassword);
        String encryptedPassword = encrypt(originalPassword);
        System.out.println("Encrypted password: " + encryptedPassword);
        String decryptedPassword = decrypt(encryptedPassword);
        System.out.println("Decrypted password: " + decryptedPassword);
    }

    private static String encrypt(String property) throws GeneralSecurityException, UnsupportedEncodingException {
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(METHOD);
        SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
        Cipher pbeCipher = Cipher.getInstance(METHOD);
        pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
        return base64Encode(pbeCipher.doFinal(property.getBytes("UTF-8")));
    }

    private static String base64Encode(byte[] bytes) {
        return Base64.encodeBytes(bytes);
    }

    private static String decrypt(String property) throws GeneralSecurityException, IOException {
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(METHOD);
        SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
        Cipher pbeCipher = Cipher.getInstance(METHOD);
        pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
        return new String(pbeCipher.doFinal(base64Decode(property)), "UTF-8");
    }

    private static byte[] base64Decode(String property) throws IOException {
        return Base64.decode(property);
    }

    public static String decryptProp(String key, Properties prop) {
        String val = prop.getProperty(key);
        try {
            return decrypt(val);
        } catch (GeneralSecurityException | IOException e) {
            LoggerFactory.getLogger(PasswordCrypter.class).error("Could not decrypt property.", e);
            return null;
        }
    }

    public static void setEncryptetProp(String key, String value, Properties prop) {
        try {
            prop.setProperty(key,encrypt(value));
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            LoggerFactory.getLogger(PasswordCrypter.class).error("Could not encrypt property. Property not set", e);
        }
    }

}

