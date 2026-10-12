package com.kanakani.security;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

/**
 * Seals post text with AES-256-GCM before it reaches the database.
 *
 * The associated data (AAD) is the post id. A ciphertext copied onto another post fails
 * to decrypt, so rows cannot be swapped around by someone with database access.
 *
 * Be clear about what this protects: the database and its backups. The application must
 * decrypt posts to show them to everyone in the office, so a public feed cannot be
 * end-to-end encrypted. Private chats can be, and are (see docs/SECURITY.md).
 */
public final class PostCrypto {

    public record Sealed(int keyId, byte[] iv, byte[] ciphertext) { }

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final KeyProvider keys;
    private final SecureRandom random = new SecureRandom();

    public PostCrypto(KeyProvider keys) {
        this.keys = keys;
    }

    public Sealed seal(String plaintext, String aad) {
        try {
            int keyId = keys.currentKeyId();
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keys.key(keyId), "AES"), new GCMParameterSpec(TAG_BITS, iv));
            c.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
            return new Sealed(keyId, iv, c.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not seal the post", e);
        }
    }

    public String open(Sealed sealed, String aad) {
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keys.key(sealed.keyId()), "AES"), new GCMParameterSpec(TAG_BITS, sealed.iv()));
            c.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
            return new String(c.doFinal(sealed.ciphertext()), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not open the post. Wrong key or the row was tampered with.", e);
        }
    }
}
