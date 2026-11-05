package com.quanlynhakhoa.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class DinhDangTest {

    @Test
    void sdt_chapNhanDiDongVaCoDinh_loaiDauSai() {
        assertTrue(DinhDang.laSdt("0912345678"));
        assertTrue(DinhDang.laSdt("0398682230"));
        assertTrue(DinhDang.laSdt("0241234567"));
        assertFalse(DinhDang.laSdt("0123456789"));
        assertFalse(DinhDang.laSdt("0412345678"));
        assertFalse(DinhDang.laSdt("091234567"));
        assertFalse(DinhDang.laSdt("1912345678"));
        assertFalse(DinhDang.laSdt(null));
    }

    @Test
    void sdtNhanVien_chiChapNhanDiDong() {
        assertTrue(DinhDang.laSdtNhanVien("0912345678"));
        assertTrue(DinhDang.laSdtNhanVien("0398682230"));
        assertFalse(DinhDang.laSdtNhanVien("0241234567"));
    }

    @Test
    void email_loaiDauChamLienTiepVaThieuMien() {
        assertTrue(DinhDang.laEmail("an@example.com"));
        assertTrue(DinhDang.laEmail("hieu.nhasi@clinic.vn"));
        assertTrue(DinhDang.laEmail("macvestdinet+nhasi@gmail.com"));
        assertTrue(DinhDang.laEmail("a@b.co.uk"));
        assertFalse(DinhDang.laEmail("a..b@x.com"));
        assertFalse(DinhDang.laEmail(".a@x.com"));
        assertFalse(DinhDang.laEmail("a.@x.com"));
        assertFalse(DinhDang.laEmail("an@example"));
        assertFalse(DinhDang.laEmail("an @example.com"));
        assertFalse(DinhDang.laEmail(null));
    }

    @Test
    void hoTen_chiChuCaiVaKhoangTrang() {
        assertTrue(DinhDang.laHoTen("Nguyễn Văn An"));
        assertTrue(DinhDang.laHoTen("Trần Thị Bình"));
        assertFalse(DinhDang.laHoTen("Nguyễn Văn An1"));
        assertFalse(DinhDang.laHoTen("An <Quản lý>"));
        assertFalse(DinhDang.laHoTen("A"));
        assertFalse(DinhDang.laHoTen(null));
    }

    @Test
    void toiDa_trimVaChanVuotDoDai() {
        assertEquals("Quận 1", DinhDang.toiDa("  Quận 1  ", 255, "Địa chỉ"));
        assertNull(DinhDang.toiDa("   ", 255, "Địa chỉ"));
        assertThrows(IllegalArgumentException.class,
                () -> DinhDang.toiDa("a".repeat(256), 255, "Địa chỉ"));
    }

    @Test
    void chuanHoaKhoangTrang_gopNbspVaTab() {
        assertEquals("Nguyễn Văn An", DinhDang.chuanHoaKhoangTrang("  Nguyễn\u00a0Văn\tAn  "));
    }
}
