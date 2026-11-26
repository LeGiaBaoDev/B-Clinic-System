package com.quanlynhakhoa.entity;

import java.time.LocalDate;
import java.util.Objects;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Voucher {
    private String maVoucher;
    private String tenVoucher;
    private boolean loaiGiam;
    private BigDecimal giaGiam = BigDecimal.ZERO;
    private BigDecimal giaTriGiamToiDa = BigDecimal.ZERO;
    private LocalDate ngayBatDau;
    private LocalDate ngayKetThuc;
    private boolean trangThai = true;

    public Voucher() {
    }

    public Voucher(String maVoucher, String tenVoucher, boolean loaiGiam, double giaGiam, double giaTriGiamToiDa, LocalDate ngayBatDau, LocalDate ngayKetThuc, boolean trangThai) {
        setMaVoucher(maVoucher);
        setTenVoucher(tenVoucher);
        setLoaiGiam(loaiGiam);
        setGiaGiam(giaGiam);
        setGiaTriGiamToiDa(giaTriGiamToiDa);
        setNgayBatDau(ngayBatDau);
        setNgayKetThuc(ngayKetThuc);
        setTrangThai(trangThai);
    }

    public Voucher(Voucher other) {
        this.maVoucher = other.maVoucher;
        this.tenVoucher = other.tenVoucher;
        this.loaiGiam = other.loaiGiam;
        this.giaGiam = other.giaGiam;
        this.giaTriGiamToiDa = other.giaTriGiamToiDa;
        this.ngayBatDau = other.ngayBatDau;
        this.ngayKetThuc = other.ngayKetThuc;
        this.trangThai = other.trangThai;
    }

    public Voucher(String ma,String ten,boolean loai,BigDecimal giaGiam,BigDecimal toiDa,
            LocalDate tu,LocalDate den,boolean trangThai) {
        this(ma,ten,loai,giaGiam.doubleValue(),toiDa==null?0:toiDa.doubleValue(),tu,den,trangThai);
        setGiaGiam(giaGiam);setGiaTriGiamToiDa(toiDa==null?BigDecimal.ZERO:toiDa);
    }

    public String getMaVoucher() {
        return maVoucher;
    }

    public void setMaVoucher(String maVoucher) {
        if (maVoucher != null && !maVoucher.matches("VC\\d{3}")) {
            throw new IllegalArgumentException("Mã voucher không đúng định dạng");
        }
        this.maVoucher = maVoucher;
    }

    public String getTenVoucher() {
        return tenVoucher;
    }

    public void setTenVoucher(String tenVoucher) {
        if (tenVoucher == null || tenVoucher.isBlank() || tenVoucher.trim().length() < 2 || tenVoucher.trim().length() > 100) {
            throw new IllegalArgumentException("Tên voucher không được rỗng");
        }
        this.tenVoucher = tenVoucher.trim();
    }

    public boolean isLoaiGiam() {
        return loaiGiam;
    }

    public void setLoaiGiam(boolean loaiGiam) {
        if (loaiGiam && giaGiam.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Giá trị giảm phần trăm không được vượt quá 100");
        }
        this.loaiGiam = loaiGiam;
    }

    public double getGiaGiam() {
        return giaGiam.doubleValue();
    }

    public void setGiaGiam(double giaGiam) {
        if (!Double.isFinite(giaGiam) || giaGiam <= 0 || (loaiGiam && giaGiam > 100)) {
            throw new IllegalArgumentException("Giá trị giảm không hợp lệ");
        }
        this.giaGiam = ChiTietHoaDon.tien(BigDecimal.valueOf(giaGiam));
    }

    public double getGiaTriGiamToiDa() {
        return giaTriGiamToiDa.doubleValue();
    }

    public void setGiaTriGiamToiDa(double giaTriGiamToiDa) {
        if (!Double.isFinite(giaTriGiamToiDa) || giaTriGiamToiDa < 0) {
            throw new IllegalArgumentException("Giá trị giảm tối đa không được âm");
        }
        this.giaTriGiamToiDa = ChiTietHoaDon.tien(BigDecimal.valueOf(giaTriGiamToiDa));
    }

    public LocalDate getNgayBatDau() {
        return ngayBatDau;
    }

    public void setNgayBatDau(LocalDate ngayBatDau) {
        if (ngayBatDau == null || (ngayKetThuc != null && !ngayBatDau.isBefore(ngayKetThuc))) {
            throw new IllegalArgumentException("Ngày bắt đầu không hợp lệ");
        }
        this.ngayBatDau = ngayBatDau;
    }

    public LocalDate getNgayKetThuc() {
        return ngayKetThuc;
    }

    public void setNgayKetThuc(LocalDate ngayKetThuc) {
        if (ngayKetThuc == null || (ngayBatDau != null && !ngayKetThuc.isAfter(ngayBatDau))) {
            throw new IllegalArgumentException("Thời hạn không hợp lệ");
        }
        this.ngayKetThuc = ngayKetThuc;
    }

    public boolean isTrangThai() {
        return trangThai;
    }

    public void setTrangThai(boolean trangThai) {
        this.trangThai = trangThai;
    }

    public boolean kiemTraHieuLuc(LocalDate ngay) {
        return trangThai && !ngay.isBefore(ngayBatDau) && !ngay.isAfter(ngayKetThuc);
    }

    public BigDecimal getGiaGiamChinhXac() { return giaGiam; }
    public BigDecimal getGiaTriGiamToiDaChinhXac() { return giaTriGiamToiDa; }
    public void setGiaGiam(BigDecimal value) {
        value=ChiTietHoaDon.tien(value);
        if(value.signum()<=0 || loaiGiam && value.compareTo(BigDecimal.valueOf(100))>0) throw new IllegalArgumentException("Giá trị giảm không hợp lệ.");
        giaGiam=value;
    }
    public void setGiaTriGiamToiDa(BigDecimal value) { giaTriGiamToiDa=ChiTietHoaDon.tien(value); }
    public BigDecimal tinhTienGiam(BigDecimal tongTien) {
        if(tongTien==null || tongTien.signum()<=0) return BigDecimal.ZERO;
        BigDecimal giam=loaiGiam?tongTien.multiply(giaGiam).divide(BigDecimal.valueOf(100),0,RoundingMode.HALF_UP):giaGiam;
        if(loaiGiam && giaTriGiamToiDa.signum()>0) giam=giam.min(giaTriGiamToiDa);
        return giam.min(tongTien);
    }
    public double tinhTienGiam(double tongTien) { return tinhTienGiam(BigDecimal.valueOf(tongTien)).doubleValue(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Voucher other)) {
            return false;
        }
        return maVoucher != null && maVoucher.equals(other.maVoucher);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maVoucher);
    }

    @Override
    public String toString() {
        return "Voucher[maVoucher=" + maVoucher + ", tenVoucher=" + tenVoucher + ", giaGiam=" + giaGiam + ", ngayKetThuc=" + ngayKetThuc + "]";
    }
}
