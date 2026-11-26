package com.quanlynhakhoa.entity;

import com.quanlynhakhoa.entity.enums.LoaiGTGT;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Bản chụp dòng dịch vụ. Thuế luôn tính trước giảm giá. */
public class ChiTietHoaDon {
    private int maCTHD;
    private HoaDon hoaDon;
    private ChiTietPhieuKham chiTietPhieuKham;
    private DichVu dichVu;
    private String tenDichVu;
    private String donViTinh;
    private int soLuong = 1;
    private BigDecimal donGia = BigDecimal.ZERO;
    private LoaiGTGT loaiGTGT = LoaiGTGT.KHONG_CHIU_THUE;
    private BigDecimal thueSuatGTGT;
    private BigDecimal tienGiam = BigDecimal.ZERO;
    private Voucher voucher;
    private String tenKhuyenMai;
    private boolean loaiGiam;
    private BigDecimal giaTriGiam = BigDecimal.ZERO;

    public ChiTietHoaDon() { }

    public ChiTietHoaDon(int maCTHD, HoaDon hoaDon, ChiTietPhieuKham chiTiet,
            DichVu dichVu, String tenDichVu, String donViTinh, int soLuong,
            BigDecimal donGia, LoaiGTGT loaiGTGT, BigDecimal thueSuat, BigDecimal tienGiam) {
        setMaCTHD(maCTHD); setHoaDon(hoaDon); setChiTietPhieuKham(chiTiet);
        setDichVu(dichVu); setTenDichVu(tenDichVu); setDonViTinh(donViTinh);
        setSoLuong(soLuong); setDonGia(donGia); setLoaiGTGT(loaiGTGT);
        setThueSuatGTGT(thueSuat); setTienGiam(tienGiam);
    }

    public ChiTietHoaDon(ChiTietHoaDon other) {
        this.maCTHD=other.maCTHD; this.hoaDon=other.hoaDon;
        this.chiTietPhieuKham=other.chiTietPhieuKham; this.dichVu=other.dichVu;
        this.tenDichVu=other.tenDichVu; this.donViTinh=other.donViTinh;
        this.soLuong=other.soLuong; this.donGia=other.donGia;
        this.loaiGTGT=other.loaiGTGT; this.thueSuatGTGT=other.thueSuatGTGT;
        this.tienGiam=other.tienGiam; this.voucher=other.voucher;
        this.tenKhuyenMai=other.tenKhuyenMai; this.loaiGiam=other.loaiGiam;
        this.giaTriGiam=other.giaTriGiam;
    }

    public int getMaCTHD() { return maCTHD; }
    public void setMaCTHD(int value) {
        if(value<0) throw new IllegalArgumentException("Mã chi tiết không hợp lệ."); maCTHD=value;
    }
    public HoaDon getHoaDon() { return hoaDon; }
    public void setHoaDon(HoaDon value) { hoaDon=Objects.requireNonNull(value,"Thiếu hóa đơn."); }
    public ChiTietPhieuKham getChiTietPhieuKham() { return chiTietPhieuKham; }
    public void setChiTietPhieuKham(ChiTietPhieuKham value) { chiTietPhieuKham=Objects.requireNonNull(value,"Thiếu dòng điều trị."); }
    public DichVu getDichVu() { return dichVu; }
    public void setDichVu(DichVu value) { dichVu=Objects.requireNonNull(value,"Thiếu dịch vụ."); }
    public String getTenDichVu() { return tenDichVu; }
    public void setTenDichVu(String value) { tenDichVu=chuoi(value,150,"Tên dịch vụ"); }
    public String getDonViTinh() { return donViTinh; }
    public void setDonViTinh(String value) { donViTinh=chuoi(value,30,"Đơn vị tính"); }
    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int value) {
        if(value<=0) throw new IllegalArgumentException("Số lượng phải lớn hơn 0.");
        kiemTraGiam(donGia.multiply(BigDecimal.valueOf(value)),loaiGTGT,thueSuatGTGT,tienGiam);
        soLuong=value;
    }
    public BigDecimal getDonGia() { return donGia; }
    public void setDonGia(BigDecimal value) {
        value=tien(value); if(value.signum()<=0) throw new IllegalArgumentException("Đơn giá phải lớn hơn 0.");
        kiemTraGiam(value.multiply(BigDecimal.valueOf(soLuong)),loaiGTGT,thueSuatGTGT,tienGiam);
        donGia=value;
    }
    public LoaiGTGT getLoaiGTGT() { return loaiGTGT; }
    public void setLoaiGTGT(LoaiGTGT value) {
        Objects.requireNonNull(value,"Thiếu loại GTGT.");
        BigDecimal rate=value==LoaiGTGT.KHONG_CHIU_THUE?null:(thueSuatGTGT==null?BigDecimal.ZERO:thueSuatGTGT);
        kiemTraGiam(getTienHang(),value,rate,tienGiam);loaiGTGT=value;thueSuatGTGT=rate;
    }
    public BigDecimal getThueSuatGTGT() { return thueSuatGTGT; }
    public void setThueSuatGTGT(BigDecimal value) {
        if(loaiGTGT==LoaiGTGT.KHONG_CHIU_THUE) {
            if(value!=null) throw new IllegalArgumentException("Không chịu thuế không có thuế suất.");
        } else if(value==null || value.signum()<0 || value.compareTo(BigDecimal.valueOf(100))>0 || value.scale()>2) {
            throw new IllegalArgumentException("Thuế suất không hợp lệ.");
        }
        kiemTraGiam(getTienHang(),loaiGTGT,value,tienGiam);thueSuatGTGT=value;
    }
    public BigDecimal getTienGiam() { return tienGiam; }
    public void setTienGiam(BigDecimal value) {
        value=tien(value);kiemTraGiam(getTienHang(),loaiGTGT,thueSuatGTGT,value);tienGiam=value;
    }
    public Voucher getVoucher() { return voucher; }
    public void setVoucher(Voucher value) { voucher=value; }
    public String getTenKhuyenMai() { return tenKhuyenMai; }
    public void setTenKhuyenMai(String value) { tenKhuyenMai=value==null?null:chuoi(value,100,"Tên khuyến mãi"); }
    public boolean isLoaiGiam() { return loaiGiam; }
    public void setLoaiGiam(boolean value) {
        if(value && giaTriGiam.compareTo(BigDecimal.valueOf(100))>0) throw new IllegalArgumentException("Phần trăm giảm vượt 100.");loaiGiam=value;
    }
    public BigDecimal getGiaTriGiam() { return giaTriGiam; }
    public void setGiaTriGiam(BigDecimal value) {
        value=tien(value); if(loaiGiam && value.compareTo(BigDecimal.valueOf(100))>0) throw new IllegalArgumentException("Phần trăm giảm vượt 100.");giaTriGiam=value;
    }
    public BigDecimal getTienHang() { return getDonGia().multiply(BigDecimal.valueOf(soLuong)).setScale(2); }
    public BigDecimal getTienGTGT() { return thue(getTienHang(),loaiGTGT,thueSuatGTGT); }
    public BigDecimal getTongSauThue() { return getTienHang().add(getTienGTGT()); }
    public BigDecimal getThanhTien() { return getTongSauThue().subtract(tienGiam); }

    static BigDecimal tien(BigDecimal value) {
        if(value==null || value.signum()<0) throw new IllegalArgumentException("Số tiền không được âm hoặc trống.");
        try { value=value.setScale(2,RoundingMode.UNNECESSARY); }
        catch(ArithmeticException ex) { throw new IllegalArgumentException("Số tiền tối đa hai chữ số thập phân."); }
        if(value.precision()>18) throw new IllegalArgumentException("Số tiền vượt giới hạn lưu trữ.");
        return value;
    }
    private static String chuoi(String value,int max,String label) {
        if(value==null || value.isBlank() || value.trim().length()>max) throw new IllegalArgumentException(label+" không hợp lệ.");return value.trim();
    }
    private static BigDecimal thue(BigDecimal base,LoaiGTGT loai,BigDecimal rate) {
        return loai==LoaiGTGT.KHONG_CHIU_THUE || rate==null?BigDecimal.ZERO.setScale(2)
                :base.multiply(rate).divide(BigDecimal.valueOf(100),0,RoundingMode.HALF_UP).setScale(2);
    }
    private static void kiemTraGiam(BigDecimal base,LoaiGTGT loai,BigDecimal rate,BigDecimal giam) {
        if(giam.signum()<0 || giam.compareTo(base.add(thue(base,loai,rate)))>0) throw new IllegalArgumentException("Mức giảm vượt tổng sau thuế.");
    }
    @Override public boolean equals(Object o) { return this==o || o instanceof ChiTietHoaDon d && maCTHD>0 && maCTHD==d.maCTHD; }
    @Override public int hashCode() { return Objects.hash(maCTHD); }
    @Override public String toString() { return "ChiTietHoaDon[maCTHD="+maCTHD+", thanhTien="+getThanhTien()+"]"; }
}
