package com.quanlynhakhoa.entity;

import com.quanlynhakhoa.entity.enums.TrangThaiLichHen;

import java.time.LocalDateTime;
import java.util.Objects;

public class LichHen {
    private String maLH;
    private LocalDateTime ngayGioHen;
    private TrangThaiLichHen trangThai = TrangThaiLichHen.DA_DAT;
    private String ghiChu;
    private String lyDoHuy;
    private KhachHang khachHang;
    private NhanVien nhaSi;
    private DichVu dichVu;
    private PhieuKham phieuKhamNguon;

    public LichHen() {
    }

    public LichHen(String maLH, LocalDateTime ngayGioHen, TrangThaiLichHen trangThai, String ghiChu, String lyDoHuy, KhachHang khachHang, NhanVien nhaSi, DichVu dichVu, PhieuKham phieuKhamNguon) {
        setMaLH(maLH);
        setNgayGioHen(ngayGioHen);
        setGhiChu(ghiChu);
        setLyDoHuy(lyDoHuy);
        setTrangThai(trangThai);
        setKhachHang(khachHang);
        setNhaSi(nhaSi);
        setDichVu(dichVu);
        setPhieuKhamNguon(phieuKhamNguon);
    }

    public LichHen(LichHen other) {
        this.maLH = other.maLH;
        this.ngayGioHen = other.ngayGioHen;
        this.trangThai = other.trangThai;
        this.ghiChu = other.ghiChu;
        this.lyDoHuy = other.lyDoHuy;
        this.khachHang = other.khachHang;
        this.nhaSi = other.nhaSi;
        this.dichVu = other.dichVu;
        this.phieuKhamNguon = other.phieuKhamNguon;
    }

    public String getMaLH() {
        return maLH;
    }

    public void setMaLH(String maLH) {
        if (maLH != null && !maLH.matches("LH\\d{9}")) {
            throw new IllegalArgumentException("Mã lịch hẹn không đúng định dạng");
        }
        this.maLH = maLH;
    }

    public LocalDateTime getNgayGioHen() {
        return ngayGioHen;
    }

    public void setNgayGioHen(LocalDateTime ngayGioHen) {
        if (ngayGioHen == null) {
            throw new IllegalArgumentException("Ngày giờ hẹn không được để trống");
        }
        this.ngayGioHen = ngayGioHen;
    }

    public TrangThaiLichHen getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(TrangThaiLichHen trangThai) {
        if (trangThai == null) {
            throw new IllegalArgumentException("Trạng thái lịch hẹn không được để trống");
        }
        if (trangThai == TrangThaiLichHen.DA_HUY && lyDoHuy == null) {
            throw new IllegalArgumentException("Vui lòng nhập lý do hủy");
        }
        this.trangThai = trangThai;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = (ghiChu == null || ghiChu.isBlank()) ? null : ghiChu.trim();
    }

    public String getLyDoHuy() {
        return lyDoHuy;
    }

    public void setLyDoHuy(String lyDoHuy) {
        this.lyDoHuy = (lyDoHuy == null || lyDoHuy.isBlank()) ? null : lyDoHuy.trim();
    }

    public KhachHang getKhachHang() {
        return khachHang;
    }

    public void setKhachHang(KhachHang khachHang) {
        if (khachHang == null) {
            throw new IllegalArgumentException("Phải chọn khách hàng");
        }
        this.khachHang = khachHang;
    }

    public NhanVien getNhaSi() {
        return nhaSi;
    }

    public void setNhaSi(NhanVien nhaSi) {
        if (nhaSi == null) {
            throw new IllegalArgumentException("Phải chọn nha sĩ");
        }
        this.nhaSi = nhaSi;
    }

    public DichVu getDichVu() {
        return dichVu;
    }

    public void setDichVu(DichVu dichVu) {
        if (dichVu == null) {
            throw new IllegalArgumentException("Phải chọn dịch vụ");
        }
        this.dichVu = dichVu;
    }

    public PhieuKham getPhieuKhamNguon() {
        return phieuKhamNguon;
    }

    public void setPhieuKhamNguon(PhieuKham phieuKhamNguon) {
        this.phieuKhamNguon = phieuKhamNguon;
    }

    public LocalDateTime getGioKetThuc() {
        return ngayGioHen.plusMinutes(dichVu.getThoiLuong());
    }

    public void capNhatTrangThai(TrangThaiLichHen moi) {
        boolean hopLe = switch (moi) {
            case DA_DEN, DA_HUY, KHONG_DEN -> trangThai == TrangThaiLichHen.DA_DAT;
            case HOAN_TAT -> trangThai == TrangThaiLichHen.DA_DEN;
            case DA_DAT -> false;
        };
        if (!hopLe) {
            throw new IllegalStateException("Không thể chuyển lịch hẹn từ " + trangThai + " sang " + moi);
        }
        setTrangThai(moi);
    }

    public void huy(String lyDo) {
        if (lyDo == null || lyDo.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do hủy");
        }
        setLyDoHuy(lyDo);
        capNhatTrangThai(TrangThaiLichHen.DA_HUY);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LichHen other)) {
            return false;
        }
        return maLH != null && maLH.equals(other.maLH);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maLH);
    }

    @Override
    public String toString() {
        return "LichHen[maLH=" + maLH + ", ngayGioHen=" + ngayGioHen + ", trangThai=" + trangThai + "]";
    }
}
