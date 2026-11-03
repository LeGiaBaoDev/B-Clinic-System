package com.quanlynhakhoa.entity;

import com.quanlynhakhoa.entity.enums.VaiTro;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Ràng buộc của tài khoản liên quan trực tiếp tới đăng nhập/đổi mật khẩu. */
class TaiKhoanTest {

    @Test
    void kiemTraMatKhauGoc_du6KyTu_khongNemLoi() {
        assertDoesNotThrow(() -> TaiKhoan.kiemTraMatKhauGoc("123456"));
        assertDoesNotThrow(() -> TaiKhoan.kiemTraMatKhauGoc("Clinic@123"));
    }

    @Test
    void kiemTraMatKhauGoc_nullHoacDuoi6KyTu_nemLoi() {
        assertThrows(IllegalArgumentException.class, () -> TaiKhoan.kiemTraMatKhauGoc(null));
        assertThrows(IllegalArgumentException.class, () -> TaiKhoan.kiemTraMatKhauGoc("12345"));
    }

    @Test
    void setTenTK_duoi4KyTu_nemLoi() {
        TaiKhoan tk = new TaiKhoan();
        assertThrows(IllegalArgumentException.class, () -> tk.setTenTK(null));
        assertThrows(IllegalArgumentException.class, () -> tk.setTenTK("abc"));
        assertDoesNotThrow(() -> tk.setTenTK("hieu"));
    }

    @Test
    void setMaTK_saiDinhDang_nemLoi() {
        TaiKhoan tk = new TaiKhoan();
        assertThrows(IllegalArgumentException.class, () -> tk.setMaTK("TK01"));
        assertThrows(IllegalArgumentException.class, () -> tk.setMaTK("AB001"));
        assertDoesNotThrow(() -> tk.setMaTK("TK001"));
        assertDoesNotThrow(() -> tk.setMaTK(null));
    }

    @Test
    void setNhanVien_null_nemLoi() {
        TaiKhoan tk = new TaiKhoan();
        assertThrows(IllegalArgumentException.class, () -> tk.setNhanVien(null));
    }

    @Test
    void setVaiTro_null_nemLoi() {
        TaiKhoan tk = new TaiKhoan();
        assertThrows(IllegalArgumentException.class, () -> tk.setVaiTro(null));
        assertDoesNotThrow(() -> tk.setVaiTro(VaiTro.QUANLY));
    }
}
