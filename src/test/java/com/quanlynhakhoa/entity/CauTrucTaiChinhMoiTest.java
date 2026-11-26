package com.quanlynhakhoa.entity;

import com.quanlynhakhoa.entity.enums.*;
import com.quanlynhakhoa.util.QuyTacTaiKhoan;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CauTrucTaiChinhMoiTest {
    private static BigDecimal tien(String value) { return new BigDecimal(value); }
    private ChiTietHoaDon dong() {
        ChiTietHoaDon d = new ChiTietHoaDon();
        d.setSoLuong(1); d.setDonGia(tien("1000000"));
        d.setLoaiGTGT(LoaiGTGT.CHIU_THUE); d.setThueSuatGTGT(tien("10"));
        return d;
    }
    @Test void giamSauThue_khongLamGiamTienGTGT() {
        ChiTietHoaDon d = dong(); d.setTienGiam(tien("100000"));
        assertEquals(0, tien("100000").compareTo(d.getTienGTGT()));
        assertEquals(0, tien("1000000").compareTo(d.getThanhTien()));
        assertThrows(IllegalArgumentException.class, () -> d.setTienGiam(tien("1100001")));
    }
    @Test void khongChiuThue_khacVoiThueSuatKhong() {
        ChiTietHoaDon d=dong(); d.setLoaiGTGT(LoaiGTGT.KHONG_CHIU_THUE);
        assertNull(d.getThueSuatGTGT()); assertEquals(0,d.getTienGTGT().signum());
        d.setLoaiGTGT(LoaiGTGT.CHIU_THUE);d.setThueSuatGTGT(BigDecimal.ZERO);
        assertNotNull(d.getThueSuatGTGT());
    }
    @Test void thanhToanNhieuLan_chotHoaDon_chongTrungVaVuotNo() {
        HoaDon hd=new HoaDon(); hd.setMaHD("HD261120001");
        hd.chotChiTiet(java.util.List.of(dong()));
        assertEquals(TrangThaiThanhToan.CHUA_THANH_TOAN,hd.getTrangThaiThanhToan());
        PhieuThu a=thu(hd,"request-a","300000"); hd.ghiNhanPhieuThu(a);
        assertEquals(TrangThaiThanhToan.THANH_TOAN_MOT_PHAN,hd.getTrangThaiThanhToan());
        assertEquals(0,tien("800000").compareTo(hd.getConNo()));
        assertThrows(IllegalStateException.class,()->hd.chotChiTiet(java.util.List.of(dong())));
        assertThrows(IllegalStateException.class,()->hd.ghiNhanPhieuThu(a));
        assertThrows(IllegalArgumentException.class,()->hd.ghiNhanPhieuThu(thu(hd,"request-b","800001")));
        hd.ghiNhanPhieuThu(thu(hd,"request-b","800000"));
        assertEquals(TrangThaiThanhToan.DA_THANH_TOAN,hd.getTrangThaiThanhToan());
        assertTrue(hd.isTrangThai()); assertEquals(0,hd.getConNo().signum());
        a.setSoTienThu(tien("1"));
        assertEquals(0,tien("1100000").compareTo(hd.getTongDaThu()));
    }
    @Test void banChupHoaDon_khongDoiTheoDongNguon() {
        HoaDon hd=new HoaDon();ChiTietHoaDon d=dong();hd.chotChiTiet(java.util.List.of(d));
        d.setDonGia(tien("1"));
        assertEquals(0,tien("1100000").compareTo(hd.getTongThanhToan()));
    }
    @Test void maNhanVienVaDangNhap_theoVaiTro() {
        NhanVien n=new NhanVien();n.setMaNV("NS001");
        assertThrows(IllegalArgumentException.class,()->n.setMaNV("NV001"));
        assertThrows(IllegalArgumentException.class,()->n.setMaNV("NS000"));
        assertEquals("LT001",QuyTacTaiKhoan.tenDangNhap("LT001",VaiTro.LETAN));
        assertThrows(IllegalArgumentException.class,()->QuyTacTaiKhoan.tenDangNhap("NS001",VaiTro.LETAN));
        assertEquals("NS004",QuyTacTaiKhoan.goiYTenDangNhap(VaiTro.NHASI,java.util.List.of("NS001","NS003","LT004")));
    }
    @Test void phieuThu_batBuocSoTienDuong_vaKhoaChongTrung() {
        PhieuThu p=new PhieuThu();
        assertThrows(IllegalArgumentException.class,()->p.setSoTienThu(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,()->p.setKhoaYeuCau(" "));
    }
    private PhieuThu thu(HoaDon hd,String key,String amount) {
        PhieuThu p=new PhieuThu();p.setHoaDon(hd);p.setKhoaYeuCau(key);
        p.setSoTienThu(tien(amount));p.setNgayThu(LocalDateTime.now());
        p.setHinhThucThanhToan(HoaDon.TIEN_MAT);
        NhanVien nv=new NhanVien();nv.setMaNV("LT001");p.setThuNgan(nv);
        return p;
    }
}
