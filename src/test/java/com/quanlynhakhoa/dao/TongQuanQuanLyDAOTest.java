package com.quanlynhakhoa.dao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TongQuanQuanLyDAOTest {

    @Test
    void tangTruong_kyTruocBangKhong_khongChiaChoKhong() {
        assertTrue(TongQuanQuanLyDAO.tinhTyLeTangTruong(BigDecimal.TEN, BigDecimal.ZERO).isEmpty());
        assertEquals(BigDecimal.ZERO,
                TongQuanQuanLyDAO.tinhTyLeTangTruong(BigDecimal.ZERO, BigDecimal.ZERO).orElseThrow());
    }

    @Test
    void tangTruong_tangVaGiam_lamTronMotChuSo() {
        assertEquals(new BigDecimal("33.3"), TongQuanQuanLyDAO.tinhTyLeTangTruong(
                new BigDecimal("400"), new BigDecimal("300")).orElseThrow());
        assertEquals(new BigDecimal("-25.0"), TongQuanQuanLyDAO.tinhTyLeTangTruong(
                new BigDecimal("300"), new BigDecimal("400")).orElseThrow());
    }

    @Test
    void bieuDo_duBayNgay_congGopVaBoNgayNgoaiKhoang() {
        LocalDate ngay = LocalDate.of(2026, 11, 5);
        var rows = List.of(
                new TongQuanQuanLyDAO.DoanhThuNgay(ngay, BigDecimal.TEN),
                new TongQuanQuanLyDAO.DoanhThuNgay(ngay, BigDecimal.ONE),
                new TongQuanQuanLyDAO.DoanhThuNgay(ngay.minusDays(7), BigDecimal.TEN));
        var result = TongQuanQuanLyDAO.boSungNgayKhongPhatSinh(ngay, rows);
        assertEquals(7, result.size());
        for (int i = 0; i < 7; i++) {
            assertEquals(ngay.minusDays(6 - i), result.get(i).ngay());
            assertEquals(i == 6 ? new BigDecimal("11") : BigDecimal.ZERO, result.get(i).soTien());
        }
        assertThrows(UnsupportedOperationException.class, () -> result.clear());
    }
}
