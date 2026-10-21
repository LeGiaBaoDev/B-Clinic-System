package com.quanlynhakhoa.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Voucher {
    private String maVoucher;
    private String tenVoucher;
    private boolean loaiGiam;
    private double giaGiam;
    private double giaTriGiamToiDa;
    private LocalDate ngayBatDau;
    private LocalDate ngayKetThuc;
    private boolean trangThai;
}
