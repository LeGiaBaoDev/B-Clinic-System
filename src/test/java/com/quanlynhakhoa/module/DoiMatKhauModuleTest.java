package com.quanlynhakhoa.module;

import com.quanlynhakhoa.util.KiemTraXacThuc;
import com.quanlynhakhoa.util.PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * đặt lại mật khẩu: kiểm tra số điện thoại/email, độ dài, xác nhận mật khẩu và
 * việc đăng nhập bằng mật khẩu mới.
 */
class DoiMatKhauModuleTest {

    @Test
    void datLai_hopLe_bangSoDienThoai() {
        assertNull(KiemTraXacThuc.loiDatLaiMatKhau("0912345678", "Clinic@123", "Clinic@123"));
    }

    @Test
    void datLai_hopLe_bangEmail() {
        assertNull(KiemTraXacThuc.loiDatLaiMatKhau("hieu.nhasi@clinic.vn", "Clinic@123", "Clinic@123"));
    }

    @Test
    void datLai_thieuLienHe_baoLoi() {
        assertEquals("Vui lòng nhập số điện thoại hoặc email.",
                KiemTraXacThuc.loiDatLaiMatKhau("", "Clinic@123", "Clinic@123"));
    }

    @Test
    void datLai_lienHeSaiDinhDang_baoLoi() {
        assertEquals("Số điện thoại hoặc email không đúng định dạng.",
                KiemTraXacThuc.loiDatLaiMatKhau("khong-phai-lien-he", "Clinic@123", "Clinic@123"));
    }

    @Test
    void datLai_matKhauNganHon6KyTu_baoLoi() {
        assertEquals("Mật khẩu phải có ít nhất 6 ký tự.",
                KiemTraXacThuc.loiDatLaiMatKhau("0912345678", "12345", "12345"));
    }

    @Test
    void datLai_xacNhanKhongKhop_baoLoi() {
        assertEquals("Mật khẩu xác nhập không khớp.",
                KiemTraXacThuc.loiDatLaiMatKhau("0912345678", "Clinic@123", "Clinic@124"));
    }

    @Test
    void sauKhiDatLai_dangNhapBangMatKhauMoi_vaMatKhauCuThatBai() {
        String matKhauMoi = "Moi@123456";
        String stored = PasswordHasher.bam(matKhauMoi);

        assertTrue(PasswordHasher.xacMinh(matKhauMoi, stored));
        assertFalse(PasswordHasher.xacMinh("Clinic@123", stored));
    }
}
