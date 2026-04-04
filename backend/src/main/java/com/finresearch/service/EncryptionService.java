package com.finresearch.service;

import com.finresearch.config.FinResearchProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class EncryptionService {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final FinResearchProperties props;

    private SecretKey aesKey() {
        try {
            String b64 = props.getEncryption().getKeyBase64();
            if (b64 != null && !b64.isBlank()) {
                byte[] decoded = Base64.getDecoder().decode(b64.trim());
                if (decoded.length == 32) {
                    return new SecretKeySpec(decoded, "AES");
                }
            }
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, "AES");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public byte[] encrypt(byte[] plain) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] cipherText = cipher.doFinal(plain);
            ByteBuffer buf = ByteBuffer.allocate(iv.length + cipherText.length);
            buf.put(iv);
            buf.put(cipherText);
            return buf.array();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public byte[] decrypt(byte[] packed) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(packed);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buf.get(iv);
            byte[] cipherBytes = new byte[buf.remaining()];
            buf.get(cipherBytes);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return cipher.doFinal(cipherBytes);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
