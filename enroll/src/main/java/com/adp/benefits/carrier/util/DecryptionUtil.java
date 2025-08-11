package com.adp.benefits.carrier.util;

import com.adp.benefits.carrier.exceptions.DecryptionException;
import com.adp.benefits.carrier.exceptions.ShortEncryptedDataException;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class DecryptionUtil {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int NONCE_LENGTH = 12;
    private static final int KEY_LENGTH = 16;
    private static final String STATIC_AES_KEY = "CPM_Secret_Key_16"; // 16 bytes (AES-128)

    private DecryptionUtil() {}

    private static byte[] padKey() {
        byte[] keyBytes = STATIC_AES_KEY.getBytes(StandardCharsets.UTF_8);
        byte[] paddedKey = new byte[KEY_LENGTH];
        int len = Math.min(keyBytes.length, KEY_LENGTH);
        System.arraycopy(keyBytes, 0, paddedKey, 0, len);
        return paddedKey;
    }

    public static String decrypt(String encryptedPassword) throws DecryptionException {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedPassword);
            if (decodedBytes.length < NONCE_LENGTH) {
                throw new ShortEncryptedDataException(
                        "Encrypted password data is too short. Ensure the password is encrypted correctly.");
            }

            byte[] nonce = new byte[NONCE_LENGTH];
            byte[] ciphertext = new byte[decodedBytes.length - NONCE_LENGTH];

            System.arraycopy(decodedBytes, 0, nonce, 0, NONCE_LENGTH);
            System.arraycopy(decodedBytes, NONCE_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM, "BC");
            SecretKeySpec keySpec = new SecretKeySpec(padKey(), "AES");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce);

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
            byte[] decryptedBytes = cipher.doFinal(ciphertext);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new DecryptionException("Invalid Base64 encoded data", e);
        } catch (ShortEncryptedDataException e) {
            throw new DecryptionException("Encrypted data is too short", e);
        } catch (Exception e) {
            throw new DecryptionException("Decryption failed", e);
        }
    }
}
