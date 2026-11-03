package com.quanlynhakhoa.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

/**
 * kiểm tra băm và xác minh mật khẩu (PBKDF2 + tương thích dữ liệu cũ).
 */
class PasswordHasherTest {

    @Test
    void bam_taoChuoiPBKDF2CoSalt() {
        String hash = PasswordHasher.bam("Clinic@123");
        assertTrue(hash.startsWith("PBKDF2$210000$"));
        assertEquals(4, hash.split("\\$").length);
    }

    @Test
    void bam_cungMatKhau_raHaiChuoiKhacNhau_viSaltNgauNhien() {
        assertNotEquals(PasswordHasher.bam("Clinic@123"), PasswordHasher.bam("Clinic@123"));
    }

    @Test
    void xacMinh_dungMatKhau_traVeTrue() {
        assertTrue(PasswordHasher.xacMinh("Clinic@123", PasswordHasher.bam("Clinic@123")));
    }

    @Test
    void xacMinh_saiMatKhau_traVeFalse() {
        assertFalse(PasswordHasher.xacMinh("sai-mat-khau", PasswordHasher.bam("Clinic@123")));
    }

    @Test
    void xacMinh_matKhauSeedPlaintext_vanDangNhapDuoc() {
        assertTrue(PasswordHasher.xacMinh("Clinic@123", "Clinic@123"));
        assertFalse(PasswordHasher.xacMinh("Clinic@123", "clinic@123"));
    }

    @Test
    void xacMinh_sha256Cu_vanDangNhapDuoc() {
        assertTrue(PasswordHasher.xacMinh("Clinic@123", sha256Hex("Clinic@123")));
        assertFalse(PasswordHasher.xacMinh("Clinic@123", sha256Hex("khac")));
    }

    @Test
    void xacMinh_chuoiHongHoacNull_traVeFalse() {
        assertFalse(PasswordHasher.xacMinh("x", null));
        assertFalse(PasswordHasher.xacMinh(null, "Clinic@123"));
        assertFalse(PasswordHasher.xacMinh("x", "PBKDF2$abc"));
        assertFalse(PasswordHasher.xacMinh("x", "PBKDF2$999999$AAAA$AAAA"));
    }

    @Test
    void bam_matKhauRong_nemIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.bam(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.bam("   "));
    }

    private static String sha256Hex(String input) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
