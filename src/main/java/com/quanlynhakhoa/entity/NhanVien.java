package com.quanlynhakhoa.entity;

import java.util.Objects;

public class NhanVien {
    private static final String REGEX_SDT = "0[35789]\\d{8}";
    private static final String REGEX_EMAIL = "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}";

    private String maNV;
    private String hoTenNV;
    private boolean gioiTinh;
    private String sdt;
    private String email;
    private String chuyenMon;

    public NhanVien() {
    }

    public NhanVien(String maNV, String hoTenNV, boolean gioiTinh, String sdt, String email, String chuyenMon) {
        setMaNV(maNV);
        setHoTenNV(hoTenNV);
        setGioiTinh(gioiTinh);
        setSdt(sdt);
        setEmail(email);
        setChuyenMon(chuyenMon);
    }

    public NhanVien(NhanVien other) {
        this.maNV = other.maNV;
        this.hoTenNV = other.hoTenNV;
        this.gioiTinh = other.gioiTinh;
        this.sdt = other.sdt;
        this.email = other.email;
        this.chuyenMon = other.chuyenMon;
    }

    public String getMaNV() {
        return maNV;
    }

    public void setMaNV(String maNV) {
        if (maNV != null && !maNV.matches("NV\\d{3}")) {
            throw new IllegalArgumentException("Mã nhân viên không đúng định dạng");
        }
        this.maNV = maNV;
    }

    public String getHoTenNV() {
        return hoTenNV;
    }

    public void setHoTenNV(String hoTenNV) {
        if (hoTenNV == null || hoTenNV.isBlank() || hoTenNV.trim().length() < 2 || hoTenNV.trim().length() > 100) {
            throw new IllegalArgumentException("Họ tên không được rỗng");
        }
        this.hoTenNV = hoTenNV.trim();
    }

    public boolean isGioiTinh() {
        return gioiTinh;
    }

    public void setGioiTinh(boolean gioiTinh) {
        this.gioiTinh = gioiTinh;
    }

    public String getSdt() {
        return sdt;
    }

    public void setSdt(String sdt) {
        if (sdt == null || !sdt.trim().matches(REGEX_SDT)) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ");
        }
        this.sdt = sdt.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || !email.trim().matches(REGEX_EMAIL)) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }
        this.email = email.trim();
    }

    public String getChuyenMon() {
        return chuyenMon;
    }

    public void setChuyenMon(String chuyenMon) {
        if (chuyenMon == null || chuyenMon.isBlank()) {
            throw new IllegalArgumentException("Chuyên môn không được rỗng");
        }
        this.chuyenMon = chuyenMon.trim();
    }

    public String getHoTen() {
        return hoTenNV;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NhanVien other)) {
            return false;
        }
        return maNV != null && maNV.equals(other.maNV);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maNV);
    }

    @Override
    public String toString() {
        return "NhanVien[maNV=" + maNV + ", hoTenNV=" + hoTenNV + ", sdt=" + sdt + ", email=" + email + ", chuyenMon=" + chuyenMon + "]";
    }
}
