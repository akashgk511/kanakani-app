package com.kanakani.security;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads keys from one setting, for example KANAKANI_POST_KEYS="1:BASE64KEY,2:BASE64KEY".
 * The highest id is the current key. Each key must decode to exactly 32 bytes.
 */
public final class EnvKeyProvider implements KeyProvider {

    private final Map<Integer, byte[]> keys = new HashMap<>();
    private final int current;

    public EnvKeyProvider(String spec) {
        if (spec == null || spec.isBlank()) {
            throw new IllegalArgumentException("No post keys configured");
        }
        int highest = -1;
        for (String part : spec.split(",")) {
            String[] kv = part.trim().split(":", 2);
            if (kv.length != 2) {
                throw new IllegalArgumentException("Each key must look like id:base64");
            }
            int id = Integer.parseInt(kv[0].trim());
            byte[] key = Base64.getDecoder().decode(kv[1].trim());
            if (key.length != 32) {
                throw new IllegalArgumentException("Key " + id + " must be 32 bytes");
            }
            keys.put(id, key);
            highest = Math.max(highest, id);
        }
        this.current = highest;
    }

    @Override
    public int currentKeyId() {
        return current;
    }

    @Override
    public byte[] key(int keyId) {
        byte[] k = keys.get(keyId);
        if (k == null) {
            throw new IllegalStateException("Unknown key id " + keyId);
        }
        return k.clone();
    }
}
