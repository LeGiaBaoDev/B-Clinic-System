package com.quanlynhakhoa.entity;

import java.time.LocalDate;
import java.util.Objects;

public class Voucher {
    private String maVoucher;
    private String tenVoucher;
    private boolean loaiGiam;
    private double giaGiam;
    private double giaTriGiamToiDa;
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
        this.loaiGiam = loaiGiam;
    }

    public double getGiaGiam() {
        return giaGiam;
    }

    public void setGiaGiam(double giaGiam) {
        if (giaGiam <= 0 || (loaiGiam && giaGiam > 100)) {
            throw new IllegalArgumentException("Giá trị giảm không hợp lệ");
        }
        this.giaGiam = giaGiam;
    }

    public double getGiaTriGiamToiDa() {
        return giaTriGiamToiDa;
    }

    public void setGiaTriGiamToiDa(double giaTriGiamToiDa) {
        if (giaTriGiamToiDa < 0) {
            throw new IllegalArgumentException("Giá trị giảm tối đa không được âm");
        }
        this.giaTriGiamToiDa = giaTriGiamToiDa;
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

    public double tinhTienGiam(double tongTien) {
        if (tongTien <= 0) {
            return 0;
        }
        double giam = loaiGiam ? Math.round(tongTien * giaGiam / 100) : giaGiam;
        if (loaiGiam && giaTriGiamToiDa > 0) {
            giam = Math.min(giam, giaTriGiamToiDa);
        }
        return Math.min(giam, tongTien);
    }

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
