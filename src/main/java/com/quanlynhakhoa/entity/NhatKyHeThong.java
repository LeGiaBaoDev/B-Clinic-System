package com.quanlynhakhoa.entity;

import java.time.LocalDateTime;
import java.util.Objects;

public class NhatKyHeThong {
    private String maNhatKy;
    private LocalDateTime thoiDiem = LocalDateTime.now();
    private String hanhDong;
    private String noiDungThayDoi;
    private TaiKhoan taiKhoan;

    public NhatKyHeThong() {
    }

    public NhatKyHeThong(String maNhatKy, LocalDateTime thoiDiem, String hanhDong, String noiDungThayDoi, TaiKhoan taiKhoan) {
        setMaNhatKy(maNhatKy);
        setThoiDiem(thoiDiem);
        setHanhDong(hanhDong);
        setNoiDungThayDoi(noiDungThayDoi);
        setTaiKhoan(taiKhoan);
    }

    public NhatKyHeThong(NhatKyHeThong other) {
        this.maNhatKy = other.maNhatKy;
        this.thoiDiem = other.thoiDiem;
        this.hanhDong = other.hanhDong;
        this.noiDungThayDoi = other.noiDungThayDoi;
        this.taiKhoan = other.taiKhoan;
    }

    public String getMaNhatKy() {
        return maNhatKy;
    }

    public void setMaNhatKy(String maNhatKy) {
        if (maNhatKy != null && !maNhatKy.matches("NK\\d{6}")) {
            throw new IllegalArgumentException("Mã nhật ký không đúng định dạng");
        }
        this.maNhatKy = maNhatKy;
    }

    public LocalDateTime getThoiDiem() {
        return thoiDiem;
    }

    public void setThoiDiem(LocalDateTime thoiDiem) {
        if (thoiDiem == null) {
            throw new IllegalArgumentException("Thời điểm không được để trống");
        }
        this.thoiDiem = thoiDiem;
    }

    public String getHanhDong() {
        return hanhDong;
    }

    public void setHanhDong(String hanhDong) {
        if (hanhDong == null || hanhDong.isBlank()) {
            throw new IllegalArgumentException("Hành động không được rỗng");
        }
        this.hanhDong = hanhDong.trim();
    }

    public String getNoiDungThayDoi() {
        return noiDungThayDoi;
    }

    public void setNoiDungThayDoi(String noiDungThayDoi) {
        this.noiDungThayDoi = (noiDungThayDoi == null || noiDungThayDoi.isBlank()) ? null : noiDungThayDoi.trim();
    }

    public TaiKhoan getTaiKhoan() {
        return taiKhoan;
    }

    public void setTaiKhoan(TaiKhoan taiKhoan) {
        this.taiKhoan = taiKhoan;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NhatKyHeThong other)) {
            return false;
        }
        return maNhatKy != null && maNhatKy.equals(other.maNhatKy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maNhatKy);
    }

    @Override
    public String toString() {
        return "NhatKyHeThong[thoiDiem=" + thoiDiem + ", hanhDong=" + hanhDong + "]";
    }
}
