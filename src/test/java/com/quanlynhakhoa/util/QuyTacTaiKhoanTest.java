package com.quanlynhakhoa.util;

import com.quanlynhakhoa.entity.enums.VaiTro;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuyTacTaiKhoanTest {
    @Test void dangNhapChinhLaMaNhanVien() {
        assertEquals("QL001",QuyTacTaiKhoan.tenDangNhap("QL001",VaiTro.QUANLY));
        assertEquals("NS001",QuyTacTaiKhoan.tenDangNhap("NS001",VaiTro.NHASI));
        assertEquals("LT001",QuyTacTaiKhoan.tenDangNhap("LT001",VaiTro.LETAN));
    }
    @Test void khongChapNhanMaCuHoacSaiVaiTro() {
        assertThrows(IllegalArgumentException.class,()->QuyTacTaiKhoan.tenDangNhap("NV001",VaiTro.NHASI));
        assertThrows(IllegalArgumentException.class,()->QuyTacTaiKhoan.tenDangNhap("LT001",VaiTro.NHASI));
        assertThrows(IllegalArgumentException.class,()->QuyTacTaiKhoan.tenDangNhap("NS000",VaiTro.NHASI));
        assertThrows(IllegalArgumentException.class,()->QuyTacTaiKhoan.tenDangNhap(null,VaiTro.NHASI));
    }
    @Test void goiYMaKhongLapLaiSoDaSuDung() {
        assertEquals("NS004",QuyTacTaiKhoan.goiYTenDangNhap(VaiTro.NHASI,java.util.List.of("NS001","NS003","LT005")));
        assertEquals("LT001",QuyTacTaiKhoan.goiYTenDangNhap(VaiTro.LETAN,java.util.List.of()));
        assertThrows(IllegalStateException.class,()->QuyTacTaiKhoan.goiYTenDangNhap(VaiTro.LETAN,java.util.List.of("LT999")));
    }
    @Test void hoTenKhongQuyetDinhTenDangNhap() {
        assertEquals("NS002",QuyTacTaiKhoan.tenDangNhap("Nguyễn Trọng Hiếu",VaiTro.NHASI,java.util.List.of("NS001")));
        assertThrows(IllegalArgumentException.class,()->QuyTacTaiKhoan.tenDangNhap("",VaiTro.NHASI,java.util.List.of()));
        assertThrows(IllegalArgumentException.class,()->QuyTacTaiKhoan.goiYTenDangNhap(null,java.util.List.of()));
    }
}
