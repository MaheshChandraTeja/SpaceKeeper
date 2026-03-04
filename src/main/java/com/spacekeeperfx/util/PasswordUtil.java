package com.spacekeeperfx.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

public final class PasswordUtil {
    private static final SecureRandom RNG = new SecureRandom();
    private PasswordUtil() {}

    public static String hash(char[] password) {
        Objects.requireNonNull(password);
        byte[] salt = new byte[16];
        RNG.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, 200_000, 256);
        return "pbkdf2-sha256:200000:" + Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verify(char[] password, String stored) {
        Objects.requireNonNull(password);
        if (stored == null || !stored.startsWith("pbkdf2-sha256:")) return false;
        try {
            String[] parts = stored.split(":");
            int iters = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expect = Base64.getDecoder().decode(parts[3]);
            byte[] got = pbkdf2(password, salt, iters, expect.length * 8);
            return constantTimeEq(expect, got);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] pwd, byte[] salt, int iters, int bits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(pwd, salt, iters, bits);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean constantTimeEq(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int r = 0;
        for (int i = 0; i < a.length; i++) r |= a[i] ^ b[i];
        return r == 0;
    }
}
