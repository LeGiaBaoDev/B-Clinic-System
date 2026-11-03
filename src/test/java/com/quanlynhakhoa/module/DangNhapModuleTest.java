package com.quanlynhakhoa.module;

import com.quanlynhakhoa.util.KiemTraXacThuc;
import com.quanlynhakhoa.util.PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * module đăng nhập: kiểm tra dữ liệu đầu vào và xác minh mật khẩu (không cần
 * DB; phần truy vấn tài khoản do DAO đảm nhiệm).
 */
class DangNhapModuleTest {

    @Test
    void dangNhap_thieuTenHoacMatKhau_biChanTruocKhiGoiDB() {
        assertNotNull(KiemTraXacThuc.loiDangNhap("", "Clinic@123"));
        assertNotNull(KiemTraXacThuc.loiDangNhap("hieu.quanly", ""));
        assertNotNull(KiemTraXacThuc.loiDangNhap(null, null));
    }

    @Test
    void dangNhap_duDuLieu_vaMatKhauSeedDung_thiChoQua() {
        assertNull(KiemTraXacThuc.loiDangNhap("hieu.quanly", "Clinic@123"));
        assertTrue(PasswordHasher.xacMinh("Clinic@123", "Clinic@123"));
    }

    @Test
    void dangNhap_saiMatKhau_biTuChoi() {
        assertFalse(PasswordHasher.xacMinh("sai-mat-khau", "Clinic@123"));
        assertFalse(PasswordHasher.xacMinh("", "Clinic@123"));
    }

    @Test
    void dangNhap_taiKhoanDaHashPBKDF2_xacMinhDung() {
        String stored = PasswordHasher.bam("Clinic@123");
        assertTrue(PasswordHasher.xacMinh("Clinic@123", stored));
        assertFalse(PasswordHasher.xacMinh("Clinic@1234", stored));
    }

    @Test
    void sauKhiDoiMatKhau_matKhauCuKhongConDung() {
        String cu = PasswordHasher.bam("Clinic@123");
        String moi = PasswordHasher.bam("Moi@123456");

        assertTrue(PasswordHasher.xacMinh("Clinic@123", cu));
        assertTrue(PasswordHasher.xacMinh("Moi@123456", moi));
        assertFalse(PasswordHasher.xacMinh("Clinic@123", moi));
    }
}
