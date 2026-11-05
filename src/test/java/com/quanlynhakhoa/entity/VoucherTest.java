package com.quanlynhakhoa.entity;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VoucherTest {
    private static final LocalDate START = LocalDate.of(2026, 11, 1);
    private Voucher voucher() {
        return new Voucher(null, "Ưu đãi tháng 11", true, 10, 0, START, START.plusDays(10), true);
    }

    @Test void giaGiam_khongHuuHan_biTuChoi() {
        for (double value : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> voucher().setGiaGiam(value));
        }
    }
    @Test void mucToiDa_khongHuuHan_biTuChoi() {
        for (double value : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> voucher().setGiaTriGiamToiDa(value));
        }
    }
    @Test void doiLoaiGiam_giaTriVuot100_giuNguyenLoaiCu() {
        Voucher v = new Voucher(null, "Giảm tiền", false, 50000, 0, START, START.plusDays(10), true);
        assertThrows(IllegalArgumentException.class, () -> v.setLoaiGiam(true));
        assertFalse(v.isLoaiGiam());
    }
    @Test void hieuLuc_baoGomHaiDau_vaTrangThai() {
        Voucher v = voucher();
        assertTrue(v.kiemTraHieuLuc(START));
        assertTrue(v.kiemTraHieuLuc(START.plusDays(10)));
        assertFalse(v.kiemTraHieuLuc(START.minusDays(1)));
        assertFalse(v.kiemTraHieuLuc(START.plusDays(11)));
        v.setTrangThai(false);
        assertFalse(v.kiemTraHieuLuc(START));
    }
    @Test void giaTriGiam_bienHopLe_vaKhongHopLe() {
        Voucher v = voucher();
        v.setGiaGiam(100);
        assertThrows(IllegalArgumentException.class, () -> v.setGiaGiam(100.01));
        assertThrows(IllegalArgumentException.class, () -> v.setGiaGiam(0));
        assertThrows(IllegalArgumentException.class, () -> v.setGiaGiam(-1));
        assertThrows(IllegalArgumentException.class, () -> v.setGiaTriGiamToiDa(-1));
    }
    @Test void ngay_thuTuBatBuoc_caHaiSetter() {
        Voucher v = voucher();
        assertThrows(IllegalArgumentException.class, () -> v.setNgayKetThuc(START));
        assertThrows(IllegalArgumentException.class, () -> v.setNgayBatDau(START.plusDays(10)));
        assertThrows(IllegalArgumentException.class, () -> v.setNgayBatDau(null));
        assertThrows(IllegalArgumentException.class, () -> v.setNgayKetThuc(null));
    }
    @Test void maVaTen_kiemTraDinhDang() {
        Voucher v = voucher();
        v.setMaVoucher("VC001");
        v.setTenVoucher("  Ưu đãi mới  ");
        assertEquals("Ưu đãi mới", v.getTenVoucher());
        assertThrows(IllegalArgumentException.class, () -> v.setMaVoucher("VC1000"));
        for (String name : new String[]{null, " ", "A", "A".repeat(101)}) {
            assertThrows(IllegalArgumentException.class, () -> v.setTenVoucher(name));
        }
    }
    @Test void tinhTienGiam_phanTram_gioiHan_vaKhongVuotTong() {
        Voucher v = voucher();
        assertEquals(10000, v.tinhTienGiam(100000));
        v.setGiaTriGiamToiDa(5000);
        assertEquals(5000, v.tinhTienGiam(100000));
        v.setLoaiGiam(false);
        v.setGiaGiam(200000);
        assertEquals(100000, v.tinhTienGiam(100000));
        assertEquals(0, v.tinhTienGiam(0));
        assertEquals(0, v.tinhTienGiam(-1));
    }
}
