package com.quanlynhakhoa.entity;

import java.util.Objects;

public class HoSoBenhAn {
    private String maHSBA;
    private String tienSuBenh;
    private KhachHang khachHang;

    public HoSoBenhAn() {
    }

    public HoSoBenhAn(String maHSBA, String tienSuBenh, KhachHang khachHang) {
        setMaHSBA(maHSBA);
        setTienSuBenh(tienSuBenh);
        setKhachHang(khachHang);
    }

    public HoSoBenhAn(HoSoBenhAn other) {
        this.maHSBA = other.maHSBA;
        this.tienSuBenh = other.tienSuBenh;
        this.khachHang = other.khachHang;
    }

    public String getMaHSBA() {
        return maHSBA;
    }

    public void setMaHSBA(String maHSBA) {
        if (maHSBA != null && !maHSBA.matches("HS\\d{5}")) {
            throw new IllegalArgumentException("Mã hồ sơ bệnh án không đúng định dạng");
        }
        this.maHSBA = maHSBA;
    }

    public String getTienSuBenh() {
        return tienSuBenh;
    }

    public void setTienSuBenh(String tienSuBenh) {
        this.tienSuBenh = (tienSuBenh == null || tienSuBenh.isBlank()) ? null : tienSuBenh.trim();
    }

    public KhachHang getKhachHang() {
        return khachHang;
    }

    public void setKhachHang(KhachHang khachHang) {
        if (khachHang == null) {
            throw new IllegalArgumentException("Hồ sơ bệnh án phải gắn với một khách hàng");
        }
        this.khachHang = khachHang;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HoSoBenhAn other)) {
            return false;
        }
        return maHSBA != null && maHSBA.equals(other.maHSBA);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maHSBA);
    }

    @Override
    public String toString() {
        return "HoSoBenhAn[maHSBA=" + maHSBA + ", tienSuBenh=" + tienSuBenh + "]";
    }
}
