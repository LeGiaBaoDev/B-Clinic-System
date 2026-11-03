package com.quanlynhakhoa.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {
    @Test void bam_cungMatKhau_saltKhacNhauVaXacMinhDuoc() {
        String a = PasswordHasher.bam("Clinic@123");
        String b = PasswordHasher.bam("Clinic@123");
        assertNotEquals(a, b);
        assertTrue(PasswordHasher.xacMinh("Clinic@123", a));
        assertFalse(PasswordHasher.xacMinh("Sai@123", a));
    }

    @Test void xacMinh_hashHong_traFalse() {
        assertFalse(PasswordHasher.xacMinh("Clinic@123", "PBKDF2$-1$bad$bad"));
        assertFalse(PasswordHasher.xacMinh(null, null));
    }

    @Test void xacMinh_matKhauCu_tuongThichVaKhongNhanHashLamMatKhau() {
        assertTrue(PasswordHasher.xacMinh("Clinic@123", "Clinic@123"));
        String legacy = "6d6a";
        assertFalse(PasswordHasher.xacMinh("Clinic@123", legacy));
        String hash = PasswordHasher.bam("Clinic@123");
        assertFalse(PasswordHasher.xacMinh(hash, hash));
    }
}
