package com.quanlynhakhoa.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class HoaDon {
    public static final String TIEN_MAT = "Tiền mặt";
    public static final String CHUYEN_KHOAN = "Chuyển khoản";

    private String maHD;
    private LocalDateTime ngayLap = LocalDateTime.now();
    private boolean trangThai;
    private String hinhThucThanhToan;
    private LocalDateTime ngayThanhToan;
    private double tongKhuyenMai;
    private PhieuKham phieuKham;
    private Voucher voucher;
    private NhanVien thuNgan;

    public HoaDon() {
    }

    public HoaDon(String maHD, LocalDateTime ngayLap, boolean trangThai, String hinhThucThanhToan, LocalDateTime ngayThanhToan, double tongKhuyenMai, PhieuKham phieuKham, Voucher voucher, NhanVien thuNgan) {
        setMaHD(maHD);
        setNgayLap(ngayLap);
        setTrangThai(trangThai);
        setHinhThucThanhToan(hinhThucThanhToan);
        setNgayThanhToan(ngayThanhToan);
        setTongKhuyenMai(tongKhuyenMai);
        setPhieuKham(phieuKham);
        setVoucher(voucher);
        setThuNgan(thuNgan);
    }

    public HoaDon(HoaDon other) {
        this.maHD = other.maHD;
        this.ngayLap = other.ngayLap;
        this.trangThai = other.trangThai;
        this.hinhThucThanhToan = other.hinhThucThanhToan;
        this.ngayThanhToan = other.ngayThanhToan;
        this.tongKhuyenMai = other.tongKhuyenMai;
        this.phieuKham = other.phieuKham;
        this.voucher = other.voucher;
        this.thuNgan = other.thuNgan;
    }

    public String getMaHD() {
        return maHD;
    }

    public void setMaHD(String maHD) {
        if (maHD != null && !maHD.matches("HD\\d{9}")) {
            throw new IllegalArgumentException("Mã hóa đơn không đúng định dạng");
        }
        this.maHD = maHD;
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

    public boolean isTrangThai() {
        return trangThai;
    }

    public void setTrangThai(boolean trangThai) {
        if (this.trangThai && !trangThai) {
            throw new IllegalStateException("Hóa đơn đã thanh toán không thể chuyển về chưa thanh toán");
        }
        this.trangThai = trangThai;
    }

    public String getHinhThucThanhToan() {
        return hinhThucThanhToan;
    }

    public void setHinhThucThanhToan(String hinhThucThanhToan) {
        if (hinhThucThanhToan == null || hinhThucThanhToan.isBlank()) {
            this.hinhThucThanhToan = null;
            return;
        }
        String hinhThuc = hinhThucThanhToan.trim();
        if (!hinhThuc.equals(TIEN_MAT) && !hinhThuc.equals(CHUYEN_KHOAN)) {
            throw new IllegalArgumentException("Hình thức thanh toán không hợp lệ");
        }
        this.hinhThucThanhToan = hinhThuc;
    }

    public LocalDateTime getNgayThanhToan() {
        return ngayThanhToan;
    }

    public void setNgayThanhToan(LocalDateTime ngayThanhToan) {
        if (ngayThanhToan != null && ngayLap != null && ngayThanhToan.isBefore(ngayLap)) {
            throw new IllegalArgumentException("Ngày thanh toán phải sau ngày lập hóa đơn");
        }
        this.ngayThanhToan = ngayThanhToan;
    }

    public double getTongKhuyenMai() {
        return tongKhuyenMai;
    }

    public void setTongKhuyenMai(double tongKhuyenMai) {
        if (tongKhuyenMai < 0) {
            throw new IllegalArgumentException("Tổng khuyến mãi không được âm");
        }
        this.tongKhuyenMai = tongKhuyenMai;
    }

    public PhieuKham getPhieuKham() {
        return phieuKham;
    }

    public void setPhieuKham(PhieuKham phieuKham) {
        if (phieuKham == null) {
            throw new IllegalArgumentException("Hóa đơn phải gắn với một phiếu khám");
        }
        this.phieuKham = phieuKham;
    }

    public Voucher getVoucher() {
        return voucher;
    }

    public void setVoucher(Voucher voucher) {
        this.voucher = voucher;
    }

    public NhanVien getThuNgan() {
        return thuNgan;
    }

    public void setThuNgan(NhanVien thuNgan) {
        this.thuNgan = thuNgan;
    }

    public double tinhTongTien(List<ChiTietPhieuKham> dsChiTiet) {
        return Math.max(0, PhieuKham.tinhTongChiPhi(dsChiTiet) - tongKhuyenMai);
    }

    public void apDungVoucher(Voucher v, List<ChiTietPhieuKham> dsChiTiet, LocalDate ngay) {
        if (trangThai) {
            throw new IllegalStateException("Hóa đơn đã thanh toán");
        }
        if (voucher != null) {
            throw new IllegalStateException("Mỗi hóa đơn chỉ được áp dụng 1 voucher");
        }
        if (!v.kiemTraHieuLuc(ngay)) {
            throw new IllegalArgumentException("Voucher đã hết hiệu lực");
        }
        this.voucher = v;
        setTongKhuyenMai(v.tinhTienGiam(PhieuKham.tinhTongChiPhi(dsChiTiet)));
    }

    public void thanhToan(String hinhThuc, NhanVien thuNganXacNhan) {
        if (trangThai) {
            throw new IllegalStateException("Hóa đơn đã được thanh toán");
        }
        if (thuNganXacNhan == null) {
            throw new IllegalArgumentException("Phải có nhân viên xác nhận thanh toán");
        }
        setHinhThucThanhToan(hinhThuc);
        if (this.hinhThucThanhToan == null) {
            throw new IllegalArgumentException("Vui lòng chọn hình thức thanh toán");
        }
        setNgayThanhToan(LocalDateTime.now());
        this.thuNgan = thuNganXacNhan;
        this.trangThai = true;
    }

    public static double tinhTienThoi(double tongTien, double soTienKhachDua) {
        if (soTienKhachDua < tongTien) {
            throw new IllegalArgumentException("Số tiền khách đưa chưa đủ");
        }
        return soTienKhachDua - tongTien;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HoaDon other)) {
            return false;
        }
        return maHD != null && maHD.equals(other.maHD);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maHD);
    }

    @Override
    public String toString() {
        return "HoaDon[maHD=" + maHD + ", ngayLap=" + ngayLap + ", trangThai=" + trangThai + "]";
    }
}
