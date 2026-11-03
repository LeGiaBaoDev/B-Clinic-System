package com.quanlynhakhoa.util;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {

    private static final int VONG_LAP = 210_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String bam(String matKhau) {
        if (matKhau == null || matKhau.isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không được rỗng.");
        }
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return "PBKDF2$" + VONG_LAP + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(khoa(matKhau, salt, VONG_LAP));
    }

    public static boolean xacMinh(String matKhau, String stored) {
        if (matKhau == null || stored == null) {
            return false;
        }
        try {
            if (stored.startsWith("PBKDF2$")) {
                String[] parts = stored.split("\\$");
                if (parts.length != 4) {
                    return false;
                }
                int n = Integer.parseInt(parts[1]);
                if (n < 100_000 || n > 1_000_000) {
                    return false;
                }
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] expected = Base64.getDecoder().decode(parts[3]);
                if (salt.length != 16 || expected.length != 32) {
                    return false;
                }
                return MessageDigest.isEqual(expected, khoa(matKhau, salt, n));
            }
            if (stored.matches("[a-fA-F0-9]{64}")) {
                return stored.equalsIgnoreCase(java.util.HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(matKhau.getBytes(StandardCharsets.UTF_8))));
            }
            return MessageDigest.isEqual(stored.getBytes(StandardCharsets.UTF_8), matKhau.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            return false;
        }
    }

    private static byte[] khoa(String password, byte[] salt, int n) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, n, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không thể mã hóa mật khẩu.", e);
        } finally {
            spec.clearPassword();
        }
    }
}
