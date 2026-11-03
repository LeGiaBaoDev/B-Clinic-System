package com.quanlynhakhoa.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Quy tắc dữ liệu dùng chung cho đăng nhập và đặt lại mật khẩu.
 */
class KiemTraXacThucTest {

    @Test
    void dangNhap_thieuTenHoacMatKhau_traVeLoi() {
        assertNotNull(KiemTraXacThuc.loiDangNhap(null, "abc123"));
        assertNotNull(KiemTraXacThuc.loiDangNhap("  ", "abc123"));
        assertNotNull(KiemTraXacThuc.loiDangNhap("hieu.quanly", null));
        assertNotNull(KiemTraXacThuc.loiDangNhap("hieu.quanly", "   "));
    }

    @Test
    void dangNhap_duDuLieu_traVeNull() {
        assertNull(KiemTraXacThuc.loiDangNhap("hieu.quanly", "Clinic@123"));
    }

    @Test
    void datLai_thieuLienHe_traVeLoi() {
        assertEquals("Vui lòng nhập số điện thoại hoặc email.",
                KiemTraXacThuc.loiDatLaiMatKhau("", "Clinic@123", "Clinic@123"));
    }

    @Test
    void datLai_lienHeSaiDinhDang_traVeLoi() {
        assertNotNull(KiemTraXacThuc.loiDatLaiMatKhau("abc", "Clinic@123", "Clinic@123"));
        assertNotNull(KiemTraXacThuc.loiDatLaiMatKhau("0123", "Clinic@123", "Clinic@123"));
        assertNotNull(KiemTraXacThuc.loiDatLaiMatKhau("nv@clinic", "Clinic@123", "Clinic@123"));
    }

    @Test
    void datLai_matKhauNganHon6_traVeLoi() {
        assertEquals("Mật khẩu phải có ít nhất 6 ký tự.",
                KiemTraXacThuc.loiDatLaiMatKhau("0912345678", "12345", "12345"));
    }

    @Test
    void datLai_xacNhanKhongKhop_traVeLoi() {
        assertEquals("Mật khẩu xác nhập không khớp.",
                KiemTraXacThuc.loiDatLaiMatKhau("0912345678", "Clinic@123", "Clinic@124"));
    }

    @Test
    void datLai_hopLe_bangSoDienThoai_hoacEmail() {
        assertNull(KiemTraXacThuc.loiDatLaiMatKhau("0912345678", "Clinic@123", "Clinic@123"));
        assertNull(KiemTraXacThuc.loiDatLaiMatKhau("hieu.nhasi@clinic.vn", "Clinic@123", "Clinic@123"));
    }

    @Test
    void laLienHeHopLe_kiemTraDinhDang() {
        assertTrue(KiemTraXacThuc.laLienHeHopLe("0912345678"));
        assertTrue(KiemTraXacThuc.laLienHeHopLe("hieu.nhasi@clinic.vn"));
        assertFalse(KiemTraXacThuc.laLienHeHopLe("0123456789"));
        assertFalse(KiemTraXacThuc.laLienHeHopLe("091234567"));
        assertFalse(KiemTraXacThuc.laLienHeHopLe(null));
    }
}
