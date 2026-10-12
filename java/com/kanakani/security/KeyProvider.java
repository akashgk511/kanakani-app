package com.kanakani.security;

/**
 * Supplies the data keys that seal posts. Keys are numbered so they can be rotated:
 * new posts use {@link #currentKeyId()}, old posts keep the id they were sealed with.
 *
 * In production, implement this on top of a cloud KMS (envelope encryption).
 * {@link EnvKeyProvider} is the simple version for development and small deployments.
 */
public interface KeyProvider {

    int currentKeyId();

    /** Returns the 32-byte AES-256 key for this id, or throws if it is unknown. */
    byte[] key(int keyId);
}
