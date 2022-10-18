/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.core;


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
        if (val == null)
            return null;

        try {
            return decrypt(val);
        } catch (GeneralSecurityException | IOException e) {
            LoggerFactory.getLogger(PasswordCrypter.class).error("Could not decrypt property.", e);
            return null;
        }
    }

    public static void setEncryptetProp(String key, String value, Properties prop) {
        try {
            prop.setProperty(key, value != null ? encrypt(value) : null);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            LoggerFactory.getLogger(PasswordCrypter.class).error("Could not encrypt property. Property not set", e);
        }
    }

}

