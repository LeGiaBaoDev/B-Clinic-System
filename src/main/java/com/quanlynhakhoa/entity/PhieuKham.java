package com.quanlynhakhoa.entity;

import com.quanlynhakhoa.entity.enums.TrangThaiPhieuKham;
import com.quanlynhakhoa.util.DinhDang;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;
import java.util.Objects;

public class PhieuKham {
    private String maPK;
    private LocalDateTime ngayLap = LocalDateTime.now();
    private TrangThaiPhieuKham trangThai = TrangThaiPhieuKham.CHO_KHAM;
    private String loiDan;
    private HoSoBenhAn hoSoBenhAn;
    private LichHen lichHen;
    private NhanVien leTan;
    private NhanVien nhaSiPhuTrach;
    private LocalDateTime ngayBatDauKham;
    private LocalDateTime ngayHoanTat;

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
        this.nhaSiPhuTrach=other.nhaSiPhuTrach;
        this.ngayBatDauKham=other.ngayBatDauKham;this.ngayHoanTat=other.ngayHoanTat;
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
        this.loiDan = DinhDang.toiDa(loiDan, DinhDang.NOI_DUNG_TOI_DA, "Lời dặn");
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

    public NhanVien getNhaSiPhuTrach() { return nhaSiPhuTrach; }
    public void setNhaSiPhuTrach(NhanVien value) {
        if(value!=null && (value.getMaNV()==null || !value.getMaNV().startsWith("NS"))) throw new IllegalArgumentException("Người phụ trách phải là nha sĩ.");
        nhaSiPhuTrach=value;
    }
    public LocalDateTime getNgayBatDauKham() { return ngayBatDauKham; }
    public void setNgayBatDauKham(LocalDateTime value) {
        if(value!=null && (value.isBefore(ngayLap) || ngayHoanTat!=null && value.isAfter(ngayHoanTat))) throw new IllegalArgumentException("Thời gian bắt đầu khám không hợp lệ.");
        ngayBatDauKham=value;
    }
    public LocalDateTime getNgayHoanTat() { return ngayHoanTat; }
    public void setNgayHoanTat(LocalDateTime value) {
        if(value!=null && (value.isBefore(ngayLap) || ngayBatDauKham!=null && value.isBefore(ngayBatDauKham))) throw new IllegalArgumentException("Thời gian hoàn tất không hợp lệ.");
        ngayHoanTat=value;
    }

    public static double tinhTongChiPhi(List<ChiTietPhieuKham> dsChiTiet) {
        return tinhTongChiPhiChinhXac(dsChiTiet).doubleValue();
    }

    public static BigDecimal tinhTongChiPhiChinhXac(List<ChiTietPhieuKham> dsChiTiet) {
        BigDecimal tong=BigDecimal.ZERO;
        for(ChiTietPhieuKham ct:dsChiTiet) tong=tong.add(ct.getThanhTienChinhXac()).add(ct.getTienThueChinhXac());
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
