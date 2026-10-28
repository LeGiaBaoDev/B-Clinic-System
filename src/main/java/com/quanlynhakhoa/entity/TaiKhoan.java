package com.quanlynhakhoa.entity;

import com.quanlynhakhoa.entity.enums.VaiTro;

import java.util.Objects;

public class TaiKhoan {
    public static final int DO_DAI_TEN_TOI_THIEU = 4;
    public static final int DO_DAI_MAT_KHAU_TOI_THIEU = 6;

    private String maTK;
    private String tenTK;
    private String matKhau;
    private VaiTro vaiTro;
    private boolean trangThai = true;
    private NhanVien nhanVien;

    public TaiKhoan() {
    }

    public TaiKhoan(String maTK, String tenTK, String matKhau, VaiTro vaiTro, boolean trangThai, NhanVien nhanVien) {
        setMaTK(maTK);
        setTenTK(tenTK);
        setMatKhau(matKhau);
        setVaiTro(vaiTro);
        setTrangThai(trangThai);
        setNhanVien(nhanVien);
    }

    public TaiKhoan(TaiKhoan other) {
        this.maTK = other.maTK;
        this.tenTK = other.tenTK;
        this.matKhau = other.matKhau;
        this.vaiTro = other.vaiTro;
        this.trangThai = other.trangThai;
        this.nhanVien = other.nhanVien;
    }

    public String getMaTK() {
        return maTK;
    }

    public void setMaTK(String maTK) {
        if (maTK != null && !maTK.matches("TK\\d{3}")) {
            throw new IllegalArgumentException("Mã tài khoản không đúng định dạng");
        }
        this.maTK = maTK;
    }

    public String getTenTK() {
        return tenTK;
    }

    public void setTenTK(String tenTK) {
        if (tenTK == null || tenTK.trim().length() < DO_DAI_TEN_TOI_THIEU) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại hoặc rỗng");
        }
        this.tenTK = tenTK.trim();
    }

    public String getMatKhau() {
        return matKhau;
    }

    public void setMatKhau(String matKhau) {
        if (matKhau == null || matKhau.isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không được rỗng");
        }
        this.matKhau = matKhau;
    }

    public VaiTro getVaiTro() {
        return vaiTro;
    }

    public void setVaiTro(VaiTro vaiTro) {
        if (vaiTro == null) {
            throw new IllegalArgumentException("Vai trò không được để trống");
        }
        this.vaiTro = vaiTro;
    }

    public boolean isTrangThai() {
        return trangThai;
    }

    public void setTrangThai(boolean trangThai) {
        this.trangThai = trangThai;
    }

    public NhanVien getNhanVien() {
        return nhanVien;
    }

    public void setNhanVien(NhanVien nhanVien) {
        if (nhanVien == null) {
            throw new IllegalArgumentException("Tài khoản phải thuộc về một nhân viên");
        }
        this.nhanVien = nhanVien;
    }

    public static void kiemTraMatKhauGoc(String matKhauGoc) {
        if (matKhauGoc == null || matKhauGoc.length() < DO_DAI_MAT_KHAU_TOI_THIEU) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất " + DO_DAI_MAT_KHAU_TOI_THIEU + " ký tự");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TaiKhoan other)) {
            return false;
        }
        return maTK != null && maTK.equals(other.maTK);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maTK);
    }

    @Override
    public String toString() {
        return "TaiKhoan[maTK=" + maTK + ", tenTK=" + tenTK + ", vaiTro=" + vaiTro + ", trangThai=" + trangThai + "]";
    }
}
