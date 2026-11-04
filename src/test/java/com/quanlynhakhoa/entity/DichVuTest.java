package com.quanlynhakhoa.entity;

import static org.junit.jupiter.api.Assertions.*;

import com.quanlynhakhoa.entity.enums.MucThue;
import org.junit.jupiter.api.Test;

/** Bảng 4.6 – ràng buộc thực thể DichVu dùng ở UC005/UC007. */
class DichVuTest {

    private final LoaiDichVu dieuTri = new LoaiDichVu(null, LoaiDichVu.DIEU_TRI, MucThue.THUE_0);

    @Test
    void taoMoi_macDinhDangCungCap_vaCatKhoangTrang() {
        DichVu dv = new DichVu(null, "  Cạo vôi răng  ", 300_000, 30, "   ", true, dieuTri);
        assertEquals("Cạo vôi răng", dv.getTenDV());
        assertNull(dv.getLoTrinhDichVu(), "Lộ trình rỗng lưu null");
        assertTrue(new DichVu().isTrangThai());
    }

    @Test
    void setTenDV_rongHoacQua150KyTu_nemLoi() {
        DichVu dv = new DichVu();
        assertThrows(IllegalArgumentException.class, () -> dv.setTenDV("   "));
        assertThrows(IllegalArgumentException.class, () -> dv.setTenDV("a".repeat(DichVu.TEN_TOI_DA + 1)));
        dv.setTenDV("a".repeat(DichVu.TEN_TOI_DA));
        assertEquals(DichVu.TEN_TOI_DA, dv.getTenDV().length());
    }

    @Test
    void donGiaVaThoiLuong_khongDuong_nemLoi() {
        DichVu dv = new DichVu();
        assertEquals("Đơn giá phải lớn hơn 0",
                assertThrows(IllegalArgumentException.class, () -> dv.setDonGia(0)).getMessage());
        assertThrows(IllegalArgumentException.class, () -> dv.setDonGia(-1));
        assertThrows(IllegalArgumentException.class, () -> dv.setThoiLuong(0));
    }

    @Test
    void maDV_saiDinhDang_vaThieuLoai_nemLoi() {
        DichVu dv = new DichVu();
        assertThrows(IllegalArgumentException.class, () -> dv.setMaDV("DV1"));
        assertThrows(IllegalArgumentException.class, () -> dv.setLoaiDichVu(null));
    }
}
