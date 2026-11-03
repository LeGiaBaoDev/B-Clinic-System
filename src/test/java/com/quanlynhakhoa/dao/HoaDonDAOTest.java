package com.quanlynhakhoa.dao;
import com.quanlynhakhoa.entity.Voucher;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class HoaDonDAOTest {
    private Voucher voucher(boolean percent,double amount,double cap) {
        return new Voucher("VC001","Voucher test",percent,amount,cap,LocalDate.of(2026,1,1),LocalDate.of(2027,1,1),true);
    }
    @Test void tienGiam_coTranGiam_khongVuotTran() {
        assertEquals(new BigDecimal("100000"),HoaDonDAO.tienGiam(voucher(true,10,100000),new BigDecimal("2200000")));
    }
    @Test void tienGiam_giamTienLonHonTong_khongAm() {
        assertEquals(new BigDecimal("30000"),HoaDonDAO.tienGiam(voucher(false,50000,0),new BigDecimal("30000")));
    }
    @Test void tienGiam_phanTramLamTron_HALF_UP() {
        assertEquals(new BigDecimal("101"),HoaDonDAO.tienGiam(voucher(true,10,0),new BigDecimal("1005")));
    }
}
