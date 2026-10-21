package com.quanlynhakhoa.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HoaDon {
    private String maHD;
    private LocalDateTime ngayLap;
    private boolean trangThai;
    private String hinhThucThanhToan;
    private LocalDateTime ngayThanhToan;
    private double tongKhuyenMai;
    private PhieuKham phieuKham;
    private Voucher voucher;
    private NhanVien thuNgan;
}
