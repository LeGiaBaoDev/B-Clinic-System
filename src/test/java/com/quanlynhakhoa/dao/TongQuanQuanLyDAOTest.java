package com.quanlynhakhoa.dao;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TongQuanQuanLyDAOTest {
    @Test void boSungNgayKhongPhatSinh_duBayNgayVaThuTu() {
        LocalDate ngay=LocalDate.of(2026,11,2);
        var rows=ThongKeDoanhThuDAO.boSungNgayKhongPhatSinh(ngay,List.of(
                new ThongKeDoanhThuDAO.DoanhThuNgay(ngay.minusDays(2),new BigDecimal("150000")),
                new ThongKeDoanhThuDAO.DoanhThuNgay(ngay,new BigDecimal("250000"))));
        assertEquals(7,rows.size());
        assertEquals(ngay.minusDays(6),rows.getFirst().ngay());
        assertEquals(BigDecimal.ZERO,rows.getFirst().soTien());
        assertEquals(new BigDecimal("150000"),rows.get(4).soTien());
        assertEquals(ngay,rows.getLast().ngay());
    }
    @Test void tyLeTangTruong_khongCoKyTruoc_khongChiaChoKhong() {
        assertTrue(ThongKeDoanhThuDAO.tinhTyLeTangTruong(BigDecimal.ONE,BigDecimal.ZERO).isEmpty());
        assertEquals(0,ThongKeDoanhThuDAO.tinhTyLeTangTruong(BigDecimal.ZERO,BigDecimal.ZERO).orElseThrow().compareTo(BigDecimal.ZERO));
    }
    @Test void tyLeTangTruong_coGiamDoanhThu_phanTramAm() {
        assertEquals(new BigDecimal("-25.0"),ThongKeDoanhThuDAO.tinhTyLeTangTruong(new BigDecimal("75000"),new BigDecimal("100000")).orElseThrow());
    }
}
