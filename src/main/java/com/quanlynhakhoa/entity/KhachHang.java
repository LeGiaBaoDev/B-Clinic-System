package com.quanlynhakhoa.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KhachHang {
    private String maKH;
    private String tenKH;
    private LocalDate ngaySinh;
    private boolean gioiTinh;
    private String sdt;
    private String diaChi;
    private String email;
}
