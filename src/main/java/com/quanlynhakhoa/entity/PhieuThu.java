package com.quanlynhakhoa.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class PhieuThu {
    private String maPT;
    private HoaDon hoaDon;
    private LocalDateTime ngayThu=LocalDateTime.now();
    private BigDecimal soTienThu=BigDecimal.ZERO;
    private String hinhThucThanhToan;
    private NhanVien thuNgan;
    private String maThamChieu;
    private String khoaYeuCau;
    private String ghiChu;

    public PhieuThu() { }
    public PhieuThu(String maPT,HoaDon hoaDon,LocalDateTime ngayThu,BigDecimal soTienThu,
            String hinhThuc,NhanVien thuNgan,String maThamChieu,String khoaYeuCau,String ghiChu) {
        setMaPT(maPT);setHoaDon(hoaDon);setNgayThu(ngayThu);setSoTienThu(soTienThu);
        setHinhThucThanhToan(hinhThuc);setThuNgan(thuNgan);setMaThamChieu(maThamChieu);
        setKhoaYeuCau(khoaYeuCau);setGhiChu(ghiChu);
    }
    public PhieuThu(PhieuThu o) {
        maPT=o.maPT;hoaDon=o.hoaDon;ngayThu=o.ngayThu;soTienThu=o.soTienThu;
        hinhThucThanhToan=o.hinhThucThanhToan;thuNgan=o.thuNgan;
        maThamChieu=o.maThamChieu;khoaYeuCau=o.khoaYeuCau;ghiChu=o.ghiChu;
    }
    public String getMaPT() { return maPT; }
    public void setMaPT(String value) {
        if(value!=null && !value.matches("PT\\d{9}")) throw new IllegalArgumentException("Mã phiếu thu không hợp lệ.");maPT=value;
    }
    public HoaDon getHoaDon() { return hoaDon; }
    public void setHoaDon(HoaDon value) { hoaDon=Objects.requireNonNull(value,"Thiếu hóa đơn."); }
    public LocalDateTime getNgayThu() { return ngayThu; }
    public void setNgayThu(LocalDateTime value) { ngayThu=Objects.requireNonNull(value,"Thiếu ngày thu."); }
    public BigDecimal getSoTienThu() { return soTienThu; }
    public void setSoTienThu(BigDecimal value) {
        value=ChiTietHoaDon.tien(value);if(value.signum()<=0) throw new IllegalArgumentException("Số tiền thu phải lớn hơn 0.");soTienThu=value;
    }
    public String getHinhThucThanhToan() { return hinhThucThanhToan; }
    public void setHinhThucThanhToan(String value) {
        if(!HoaDon.TIEN_MAT.equals(value) && !HoaDon.CHUYEN_KHOAN.equals(value)) throw new IllegalArgumentException("Hình thức thanh toán không hợp lệ.");hinhThucThanhToan=value;
    }
    public NhanVien getThuNgan() { return thuNgan; }
    public void setThuNgan(NhanVien value) { thuNgan=Objects.requireNonNull(value,"Thiếu nhân viên thu tiền."); }
    public String getMaThamChieu() { return maThamChieu; }
    public void setMaThamChieu(String value) { maThamChieu=chuoi(value,100,false); }
    public String getKhoaYeuCau() { return khoaYeuCau; }
    public void setKhoaYeuCau(String value) { khoaYeuCau=chuoi(value,100,true); }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String value) { ghiChu=chuoi(value,255,false); }
    private static String chuoi(String value,int max,boolean batBuoc) {
        if(value==null || value.isBlank()) { if(batBuoc) throw new IllegalArgumentException("Thiếu khóa chống trùng.");return null; }
        if(value.trim().length()>max) throw new IllegalArgumentException("Nội dung quá dài.");return value.trim();
    }
    @Override public boolean equals(Object o) { return this==o || o instanceof PhieuThu p && maPT!=null && maPT.equals(p.maPT); }
    @Override public int hashCode() { return Objects.hash(maPT); }
    @Override public String toString() { return "PhieuThu[maPT="+maPT+", soTienThu="+soTienThu+"]"; }
}
