package com.quanlynhakhoa.entity;

import java.time.LocalDate;
import java.util.Objects;

public class KhachHang {
    private static final String REGEX_SDT = "0\\d{9}";
    private static final String REGEX_EMAIL = "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}";

    private String maKH;
    private String tenKH;
    private LocalDate ngaySinh;
    private boolean gioiTinh;
    private String sdt;
    private String diaChi;
    private String email;

    public KhachHang() {
    }

    public KhachHang(String maKH, String tenKH, LocalDate ngaySinh, boolean gioiTinh, String sdt, String diaChi, String email) {
        setMaKH(maKH);
        setTenKH(tenKH);
        setNgaySinh(ngaySinh);
        setGioiTinh(gioiTinh);
        setSdt(sdt);
        setDiaChi(diaChi);
        setEmail(email);
    }

    public KhachHang(KhachHang other) {
        this.maKH = other.maKH;
        this.tenKH = other.tenKH;
        this.ngaySinh = other.ngaySinh;
        this.gioiTinh = other.gioiTinh;
        this.sdt = other.sdt;
        this.diaChi = other.diaChi;
        this.email = other.email;
    }

    public String getMaKH() {
        return maKH;
    }

    public void setMaKH(String maKH) {
        if (maKH != null && !maKH.matches("KH\\d{5}")) {
            throw new IllegalArgumentException("Mã khách hàng không đúng định dạng");
        }
        this.maKH = maKH;
    }

    public String getTenKH() {
        return tenKH;
    }

    public void setTenKH(String tenKH) {
        if (tenKH == null || tenKH.isBlank()) {
            throw new IllegalArgumentException("Tên khách hàng không được rỗng");
        }
        this.tenKH = tenKH.trim();
    }

    public LocalDate getNgaySinh() {
        return ngaySinh;
    }

    public void setNgaySinh(LocalDate ngaySinh) {
        if (ngaySinh != null && ngaySinh.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày sinh không hợp lệ");
        }
        this.ngaySinh = ngaySinh;
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
            throw new IllegalArgumentException("Số điện thoại phải gồm đúng 10 chữ số");
        }
        this.sdt = sdt.trim();
    }

    public String getDiaChi() {
        return diaChi;
    }

    public void setDiaChi(String diaChi) {
        this.diaChi = (diaChi == null || diaChi.isBlank()) ? null : diaChi.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || email.isBlank()) {
            this.email = null;
            return;
        }
        if (!email.trim().matches(REGEX_EMAIL)) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }
        this.email = email.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof KhachHang other)) {
            return false;
        }
        return maKH != null && maKH.equals(other.maKH);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maKH);
    }

    @Override
    public String toString() {
        return "KhachHang[maKH=" + maKH + ", tenKH=" + tenKH + ", sdt=" + sdt + "]";
    }
}
