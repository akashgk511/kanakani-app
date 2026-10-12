package com.kanakani.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Turns a phone number into a one-way token. The number itself is never stored.
 *
 * The token is HMAC-SHA256 under a secret key that lives outside the database (KMS, HSM or a
 * locked-down environment variable). A plain hash would not be safe: there are only about
 * 10^9 Indian mobile numbers, so anyone with a leaked database could try them all.
 * With a keyed MAC, a leaked database alone is not enough.
 */
public final class PhoneVault {

    private final byte[] key;

    public PhoneVault(byte[] key) {
        if (key == null || key.length < 32) {
            throw new IllegalArgumentException("The phone key must be at least 32 bytes");
        }
        this.key = key.clone();
    }

    /** Accepts "98300 12345", "+91 98300 12345" or "098300-12345" and returns "+919830012345". */
    public static String normalizeIndian(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Enter your mobile number");
        }
        String d = raw.replaceAll("\\D", "");
        if (d.length() == 12 && d.startsWith("91")) {
            d = d.substring(2);
        } else if (d.length() == 11 && d.startsWith("0")) {
            d = d.substring(1);
        }
        if (!d.matches("[6-9]\\d{9}")) {
            throw new IllegalArgumentException("Enter a valid 10-digit Indian mobile number");
        }
        return "+91" + d;
    }

    /** Returns 64 lowercase hex characters. */
    public String token(String rawNumber) {
        return mac(normalizeIndian(rawNumber));
    }

    String mac(String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC is not available", e);
        }
    }

    /** Constant-time comparison, so response timing leaks nothing. */
    public static boolean sameToken(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
