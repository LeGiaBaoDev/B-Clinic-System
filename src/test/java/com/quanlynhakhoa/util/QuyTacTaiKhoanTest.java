package com.quanlynhakhoa.util;

import com.quanlynhakhoa.entity.enums.VaiTro;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuyTacTaiKhoanTest {
    @Test
    void trungTen_themSoTruocVaiTro_vaChonSoTrongDauTien() {
        assertEquals("anh.letan", QuyTacTaiKhoan.tenDangNhap("Anh", VaiTro.LETAN, java.util.List.of()));
        assertEquals("anh2.letan", QuyTacTaiKhoan.tenDangNhap("Anh", VaiTro.LETAN,
                java.util.List.of("ANH.LETAN", "anh1.letan", "anh3.letan")));
        assertEquals("anh1.letan", QuyTacTaiKhoan.tenDangNhap("Anh", VaiTro.LETAN,
                java.util.List.of("anh.letan", "anh2.letan", "anh1.nhasi")));
    }
    @Test
    void layTenCuoi_boDau_vaDungHauToVaiTro() {
        assertEquals("anh.nhasi", QuyTacTaiKhoan.tenDangNhap("Nguyễn Văn Anh", VaiTro.NHASI));
        assertEquals("anh.letan", QuyTacTaiKhoan.tenDangNhap("Nguyễn Văn Anh", VaiTro.LETAN));
        assertEquals("anh.quanly", QuyTacTaiKhoan.tenDangNhap("Nguyễn Văn Anh", VaiTro.QUANLY));
    }

    @Test
    void chuanHoaChuHoa_dChuCaiDacBiet_khoangTrangVaUnicodeToHop() {
        assertEquals("dat.nhasi", QuyTacTaiKhoan.tenDangNhap("  ĐỖ VĂN ĐẠT  ", VaiTro.NHASI));
        assertEquals("anh.letan", QuyTacTaiKhoan.tenDangNhap("Lê\u00a0Thị\tA\u0301nh", VaiTro.LETAN));
        assertEquals("an.quanly", QuyTacTaiKhoan.tenDangNhap("An", VaiTro.QUANLY));
    }

    @Test
    void duLieuKhongTheSinhTen_baoLoi() {
        assertThrows(IllegalArgumentException.class, () -> QuyTacTaiKhoan.tenDangNhap(null, VaiTro.NHASI));
        assertThrows(IllegalArgumentException.class, () -> QuyTacTaiKhoan.tenDangNhap("  ", VaiTro.NHASI));
        assertThrows(IllegalArgumentException.class, () -> QuyTacTaiKhoan.tenDangNhap("---", VaiTro.NHASI));
        assertThrows(IllegalArgumentException.class, () -> QuyTacTaiKhoan.tenDangNhap("Anh", null));
        assertThrows(IllegalArgumentException.class, () -> QuyTacTaiKhoan.tenDangNhap("a".repeat(50), VaiTro.NHASI));
    }
}
