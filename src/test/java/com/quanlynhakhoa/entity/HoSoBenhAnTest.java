package com.quanlynhakhoa.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HoSoBenhAnTest {

    private static KhachHang khachHang() {
        return new KhachHang("KH00001", "Nguyễn Văn An", LocalDate.of(1995, 5, 20), true,
                "0912345678", null, null);
    }

    @Test
    void constructor_duLieuHopLe_gachDungGiaTri() {
        KhachHang kh = khachHang();
        HoSoBenhAn hs = new HoSoBenhAn("HS00001", "Dị ứng penicillin", kh);

        assertEquals("HS00001", hs.getMaHSBA());
        assertEquals("Dị ứng penicillin", hs.getTienSuBenh());
        assertSame(kh, hs.getKhachHang());
    }

    @Test
    void constructor_chuaCoMa_choPhepKhiDaoChuaSinhMa() {
        HoSoBenhAn hs = new HoSoBenhAn(null, null, khachHang());
        assertNull(hs.getMaHSBA());
    }

    @ParameterizedTest
    @ValueSource(strings = {"HS0001", "HS000001", "hs00001", "KH00001", "HS0000A"})
    void setMaHSBA_saiDinhDang_nemLoi(String ma) {
        HoSoBenhAn hs = new HoSoBenhAn();
        assertThrows(IllegalArgumentException.class, () -> hs.setMaHSBA(ma));
    }

    @Test
    void setKhachHang_null_nemLoi() {
        HoSoBenhAn hs = new HoSoBenhAn();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> hs.setKhachHang(null));
        assertEquals("Hồ sơ bệnh án phải gắn với một khách hàng", ex.getMessage());
    }

    @Test
    void constructor_khongCoKhachHang_nemLoi() {
        assertThrows(IllegalArgumentException.class, () -> new HoSoBenhAn("HS00001", "x", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void setTienSuBenh_rong_thanhNull(String tienSu) {
        HoSoBenhAn hs = new HoSoBenhAn("HS00001", "Cũ", khachHang());
        hs.setTienSuBenh(tienSu);
        assertNull(hs.getTienSuBenh());
    }

    @Test
    void setTienSuBenh_coKhoangTrangDauCuoi_duocCat() {
        HoSoBenhAn hs = new HoSoBenhAn(null, null, khachHang());
        hs.setTienSuBenh("  Tiểu đường  ");
        assertEquals("Tiểu đường", hs.getTienSuBenh());
    }

    @Test
    void equals_cungMaHSBA_laBang() {
        HoSoBenhAn a = new HoSoBenhAn("HS00001", "A", khachHang());
        HoSoBenhAn b = new HoSoBenhAn("HS00001", "B", khachHang());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_chuaCoMa_khongBangNhau() {
        HoSoBenhAn a = new HoSoBenhAn(null, "A", khachHang());
        HoSoBenhAn b = new HoSoBenhAn(null, "A", khachHang());

        assertFalse(a.equals(b));
    }

    @Test
    void constructorSaoChep_giuNguyenThamChieuKhachHang() {
        HoSoBenhAn goc = new HoSoBenhAn("HS00001", "Dị ứng", khachHang());
        HoSoBenhAn banSao = new HoSoBenhAn(goc);

        assertEquals(goc, banSao);
        assertEquals("Dị ứng", banSao.getTienSuBenh());
        assertSame(goc.getKhachHang(), banSao.getKhachHang());
    }
}
