package com.quanlynhakhoa.entity;

import com.quanlynhakhoa.util.DinhDang;
import java.util.Objects;

public class NhanVien {
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

    /** Dựng từ dữ liệu đã lưu, bỏ qua kiểm tra định dạng. */
    public static NhanVien tuDuLieu(String maNV, String hoTenNV, boolean gioiTinh,
            String sdt, String email, String chuyenMon) {
        NhanVien nv = new NhanVien();
        nv.maNV = maNV;
        nv.hoTenNV = hoTenNV;
        nv.gioiTinh = gioiTinh;
        nv.sdt = sdt;
        nv.email = email;
        nv.chuyenMon = chuyenMon;
        return nv;
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
        if (hoTenNV == null) {
            throw new IllegalArgumentException("Họ tên cần ít nhất 2 từ, tối đa 100 ký tự.");
        }
        String ten = DinhDang.chuanHoaKhoangTrang(hoTenNV);
        if (ten.length() > DinhDang.HO_TEN_TOI_DA || ten.split(" ").length < 2
                || !ten.matches(DinhDang.HO_TEN)) {
            throw new IllegalArgumentException("Họ tên cần ít nhất 2 từ, tối đa 100 ký tự.");
        }
        this.hoTenNV = ten;
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
        if (sdt == null || !sdt.trim().matches(DinhDang.SDT_NHAN_VIEN)) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ");
        }
        this.sdt = sdt.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        String mail = email == null ? null : email.trim();
        if (mail == null || mail.length() > DinhDang.EMAIL_TOI_DA || !mail.matches(DinhDang.EMAIL)) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }
        this.email = mail;
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
