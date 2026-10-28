package com.quanlynhakhoa.entity;

import com.quanlynhakhoa.entity.enums.TrangThaiPhieuKham;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class PhieuKham {
    private String maPK;
    private LocalDateTime ngayLap = LocalDateTime.now();
    private TrangThaiPhieuKham trangThai = TrangThaiPhieuKham.CHO_KHAM;
    private String loiDan;
    private HoSoBenhAn hoSoBenhAn;
    private LichHen lichHen;
    private NhanVien leTan;

    public PhieuKham() {
    }

    public PhieuKham(String maPK, LocalDateTime ngayLap, TrangThaiPhieuKham trangThai, String loiDan, HoSoBenhAn hoSoBenhAn, LichHen lichHen, NhanVien leTan) {
        setMaPK(maPK);
        setNgayLap(ngayLap);
        setTrangThai(trangThai);
        setLoiDan(loiDan);
        setHoSoBenhAn(hoSoBenhAn);
        setLichHen(lichHen);
        setLeTan(leTan);
    }

    public PhieuKham(PhieuKham other) {
        this.maPK = other.maPK;
        this.ngayLap = other.ngayLap;
        this.trangThai = other.trangThai;
        this.loiDan = other.loiDan;
        this.hoSoBenhAn = other.hoSoBenhAn;
        this.lichHen = other.lichHen;
        this.leTan = other.leTan;
    }

    public String getMaPK() {
        return maPK;
    }

    public void setMaPK(String maPK) {
        if (maPK != null && !maPK.matches("PK\\d{9}")) {
            throw new IllegalArgumentException("Mã phiếu khám không đúng định dạng");
        }
        this.maPK = maPK;
    }

    public LocalDateTime getNgayLap() {
        return ngayLap;
    }

    public void setNgayLap(LocalDateTime ngayLap) {
        if (ngayLap == null) {
            throw new IllegalArgumentException("Ngày lập không được để trống");
        }
        this.ngayLap = ngayLap;
    }

    public TrangThaiPhieuKham getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(TrangThaiPhieuKham trangThai) {
        if (trangThai == null) {
            throw new IllegalArgumentException("Trạng thái phiếu khám không được để trống");
        }
        this.trangThai = trangThai;
    }

    public String getLoiDan() {
        return loiDan;
    }

    public void setLoiDan(String loiDan) {
        this.loiDan = (loiDan == null || loiDan.isBlank()) ? null : loiDan.trim();
    }

    public HoSoBenhAn getHoSoBenhAn() {
        return hoSoBenhAn;
    }

    public void setHoSoBenhAn(HoSoBenhAn hoSoBenhAn) {
        if (hoSoBenhAn == null) {
            throw new IllegalArgumentException("Phiếu khám phải gắn với hồ sơ bệnh án");
        }
        this.hoSoBenhAn = hoSoBenhAn;
    }

    public LichHen getLichHen() {
        return lichHen;
    }

    public void setLichHen(LichHen lichHen) {
        this.lichHen = lichHen;
    }

    public NhanVien getLeTan() {
        return leTan;
    }

    public void setLeTan(NhanVien leTan) {
        this.leTan = leTan;
    }

    public boolean isDaKhoa() {
        return trangThai == TrangThaiPhieuKham.HOAN_TAT;
    }

    public static double tinhTongChiPhi(List<ChiTietPhieuKham> dsChiTiet) {
        double tong = 0;
        for (ChiTietPhieuKham ct : dsChiTiet) {
            tong += ct.getThanhTienSauThue();
        }
        return tong;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PhieuKham other)) {
            return false;
        }
        return maPK != null && maPK.equals(other.maPK);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maPK);
    }

    @Override
    public String toString() {
        return "PhieuKham[maPK=" + maPK + ", ngayLap=" + ngayLap + ", trangThai=" + trangThai + "]";
    }
}
