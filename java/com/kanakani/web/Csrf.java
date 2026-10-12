package com.kanakani.web;

import com.kanakani.security.PhoneVault;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

/**
 * Anti-forgery tokens. The token is a keyed MAC of the session token, so nothing needs to be stored,
 * and a token from one person's session is useless in another's.
 */
final class Csrf {

    private Csrf() { }

    static String token(byte[] key, String sessionToken) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] out = mac.doFinal(("csrf:" + sessionToken).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC is not available", e);
        }
    }

    static boolean valid(byte[] key, String sessionToken, String submitted) {
        return submitted != null && PhoneVault.sameToken(token(key, sessionToken), submitted);
    }
}
