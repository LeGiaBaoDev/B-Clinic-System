package com.quanlynhakhoa.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DichVu {
    private String maDV;
    private String tenDV;
    private double donGia;
    private int thoiLuong;
    private String loTrinhDichVu;
    private boolean trangThai;
    private LoaiDichVu loaiDichVu;
}
