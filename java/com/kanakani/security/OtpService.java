package com.kanakani.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;

/**
 * One-time password rules. The code is never stored. Only a keyed MAC of (phone token + code) is.
 * The phone number is used once to send the SMS and then dropped.
 */
public final class OtpService {

    public static final Duration TTL = Duration.ofMinutes(5);
    public static final int MAX_ATTEMPTS = 5;

    /** Sends the code. The real class talks to an SMS provider. Note that the provider sees the number. */
    public interface SmsGateway {
        void send(String e164Number, String message);
    }

    private final byte[] key;
    private final SecureRandom random = new SecureRandom();

    public OtpService(byte[] key) {
        if (key == null || key.length < 32) {
            throw new IllegalArgumentException("The OTP key must be at least 32 bytes");
        }
        this.key = key.clone();
    }

    public String newCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    public String mac(String phoneToken, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((phoneToken + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC is not available", e);
        }
    }

    public boolean matches(String phoneToken, String code, String storedMac) {
        if (code == null || !code.matches("\\d{6}")) {
            return false;
        }
        return PhoneVault.sameToken(mac(phoneToken, code), storedMac);
    }
}
