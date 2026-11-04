package com.quanlynhakhoa.entity;

import com.quanlynhakhoa.entity.enums.CaTruc;
import com.quanlynhakhoa.entity.enums.TrangThaiLichLamViec;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public class LichLamViec {
    private String maLLV;
    private LocalDate ngayTruc;
    private CaTruc caTruc;
    private LocalTime gioBatDau;
    private LocalTime gioKetThuc;
    private TrangThaiLichLamViec trangThai = TrangThaiLichLamViec.DA_PHAN_CONG;
    private String ghiChuNghi;
    private NhanVien nhanVien;

    public LichLamViec() {
    }

    public LichLamViec(String maLLV, LocalDate ngayTruc, CaTruc caTruc, LocalTime gioBatDau, LocalTime gioKetThuc, TrangThaiLichLamViec trangThai, String ghiChuNghi, NhanVien nhanVien) {
        setMaLLV(maLLV);
        setNgayTruc(ngayTruc);
        setCaTruc(caTruc);
        setGioBatDau(gioBatDau);
        setGioKetThuc(gioKetThuc);
        setGhiChuNghi(ghiChuNghi);
        setTrangThai(trangThai);
        setNhanVien(nhanVien);
    }

    public LichLamViec(LichLamViec other) {
        this.maLLV = other.maLLV;
        this.ngayTruc = other.ngayTruc;
        this.caTruc = other.caTruc;
        this.gioBatDau = other.gioBatDau;
        this.gioKetThuc = other.gioKetThuc;
        this.trangThai = other.trangThai;
        this.ghiChuNghi = other.ghiChuNghi;
        this.nhanVien = other.nhanVien;
    }

    public String getMaLLV() {
        return maLLV;
    }

    public void setMaLLV(String maLLV) {
        if (maLLV != null && !maLLV.matches("LLV\\d{5}")) {
            throw new IllegalArgumentException("Mã lịch làm việc không đúng định dạng");
        }
        this.maLLV = maLLV;
    }

    public LocalDate getNgayTruc() {
        return ngayTruc;
    }

    public void setNgayTruc(LocalDate ngayTruc) {
        if (ngayTruc == null) {
            throw new IllegalArgumentException("Ngày trực không được để trống");
        }
        this.ngayTruc = ngayTruc;
    }

    public CaTruc getCaTruc() {
        return caTruc;
    }

    public void setCaTruc(CaTruc caTruc) {
        if (caTruc == null) {
            throw new IllegalArgumentException("Ca trực không được để trống");
        }
        this.caTruc = caTruc;
    }

    public LocalTime getGioBatDau() {
        return gioBatDau;
    }

    public void setGioBatDau(LocalTime gioBatDau) {
        if (gioBatDau == null) {
            throw new IllegalArgumentException("Giờ bắt đầu không được để trống");
        }
        if (gioKetThuc != null && !gioKetThuc.isAfter(gioBatDau)) {
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu");
        }
        this.gioBatDau = gioBatDau;
    }

    public LocalTime getGioKetThuc() {
        return gioKetThuc;
    }

    public void setGioKetThuc(LocalTime gioKetThuc) {
        if (gioKetThuc == null) {
            throw new IllegalArgumentException("Giờ kết thúc không được để trống");
        }
        if (gioBatDau != null && !gioKetThuc.isAfter(gioBatDau)) {
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu");
        }
        this.gioKetThuc = gioKetThuc;
    }

    public TrangThaiLichLamViec getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(TrangThaiLichLamViec trangThai) {
        if (trangThai == null) {
            throw new IllegalArgumentException("Trạng thái không được để trống");
        }
        if (trangThai == TrangThaiLichLamViec.NGHI_PHEP && ghiChuNghi == null) {
            throw new IllegalArgumentException("Vui lòng nhập lý do nghỉ");
        }
        this.trangThai = trangThai;
    }

    public String getGhiChuNghi() {
        return ghiChuNghi;
    }

    public void setGhiChuNghi(String ghiChuNghi) {
        this.ghiChuNghi = (ghiChuNghi == null || ghiChuNghi.isBlank()) ? null : ghiChuNghi.trim();
    }

    public NhanVien getNhanVien() {
        return nhanVien;
    }

    public void setNhanVien(NhanVien nhanVien) {
        if (nhanVien == null) {
            throw new IllegalArgumentException("Phải chọn nhân viên");
        }
        this.nhanVien = nhanVien;
    }

    public void ganGioTheoCa() {
        switch (caTruc) {
            case CA_SANG -> {
                this.gioBatDau = LocalTime.of(8, 0);
                this.gioKetThuc = LocalTime.of(12, 0);
            }
            case CA_CHIEU -> {
                this.gioBatDau = LocalTime.of(13, 30);
                this.gioKetThuc = LocalTime.of(17, 30);
            }
            case CA_NGAY -> {
                this.gioBatDau = LocalTime.of(8, 0);
                this.gioKetThuc = LocalTime.of(17, 30);
            }
        }
    }

    public void nghiPhep(String lyDo) {
        setGhiChuNghi(lyDo);
        setTrangThai(TrangThaiLichLamViec.NGHI_PHEP);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LichLamViec other)) {
            return false;
        }
        return maLLV != null && maLLV.equals(other.maLLV);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maLLV);
    }

    @Override
    public String toString() {
        return "LichLamViec[maLLV=" + maLLV + ", ngayTruc=" + ngayTruc + ", caTruc=" + caTruc + ", trangThai=" + trangThai + "]";
    }
}
