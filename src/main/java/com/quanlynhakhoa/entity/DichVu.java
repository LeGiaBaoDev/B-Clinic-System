package com.quanlynhakhoa.entity;

import java.util.Objects;

public class DichVu {
    private String maDV;
    private String tenDV;
    private double donGia;
    private int thoiLuong;
    private String loTrinhDichVu;
    private boolean trangThai = true;
    private LoaiDichVu loaiDichVu;

    public DichVu() {
    }

    public DichVu(String maDV, String tenDV, double donGia, int thoiLuong, String loTrinhDichVu, boolean trangThai, LoaiDichVu loaiDichVu) {
        setMaDV(maDV);
        setTenDV(tenDV);
        setDonGia(donGia);
        setThoiLuong(thoiLuong);
        setLoTrinhDichVu(loTrinhDichVu);
        setTrangThai(trangThai);
        setLoaiDichVu(loaiDichVu);
    }

    public DichVu(DichVu other) {
        this.maDV = other.maDV;
        this.tenDV = other.tenDV;
        this.donGia = other.donGia;
        this.thoiLuong = other.thoiLuong;
        this.loTrinhDichVu = other.loTrinhDichVu;
        this.trangThai = other.trangThai;
        this.loaiDichVu = other.loaiDichVu;
    }

    public String getMaDV() {
        return maDV;
    }

    public void setMaDV(String maDV) {
        if (maDV != null && !maDV.matches("DV\\d{3}")) {
            throw new IllegalArgumentException("Mã dịch vụ không đúng định dạng");
        }
        this.maDV = maDV;
    }

    public String getTenDV() {
        return tenDV;
    }

    public void setTenDV(String tenDV) {
        if (tenDV == null || tenDV.isBlank()) {
            throw new IllegalArgumentException("Tên dịch vụ không được rỗng");
        }
        this.tenDV = tenDV.trim();
    }

    public double getDonGia() {
        return donGia;
    }

    public void setDonGia(double donGia) {
        if (donGia <= 0) {
            throw new IllegalArgumentException("Đơn giá phải lớn hơn 0");
        }
        this.donGia = donGia;
    }

    public int getThoiLuong() {
        return thoiLuong;
    }

    public void setThoiLuong(int thoiLuong) {
        if (thoiLuong <= 0) {
            throw new IllegalArgumentException("Thời lượng phải lớn hơn 0 phút");
        }
        this.thoiLuong = thoiLuong;
    }

    public String getLoTrinhDichVu() {
        return loTrinhDichVu;
    }

    public void setLoTrinhDichVu(String loTrinhDichVu) {
        this.loTrinhDichVu = (loTrinhDichVu == null || loTrinhDichVu.isBlank()) ? null : loTrinhDichVu.trim();
    }

    public boolean isTrangThai() {
        return trangThai;
    }

    public void setTrangThai(boolean trangThai) {
        this.trangThai = trangThai;
    }

    public LoaiDichVu getLoaiDichVu() {
        return loaiDichVu;
    }

    public void setLoaiDichVu(LoaiDichVu loaiDichVu) {
        if (loaiDichVu == null) {
            throw new IllegalArgumentException("Phải chọn loại dịch vụ");
        }
        this.loaiDichVu = loaiDichVu;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DichVu other)) {
            return false;
        }
        return maDV != null && maDV.equals(other.maDV);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maDV);
    }

    @Override
    public String toString() {
        return "DichVu[maDV=" + maDV + ", tenDV=" + tenDV + ", donGia=" + donGia + ", trangThai=" + trangThai + "]";
    }
}
