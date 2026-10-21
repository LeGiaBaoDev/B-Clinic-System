package com.quanlynhakhoa.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChiTietPhieuKham {
    private int maCTPK;
    private String trieuChung;
    private String chanDoan;
    private double donGia;
    private int soLuong;
    private String donViTinh;
    private double mucThueGTGT;
    private String ketQuaDieuTri;
    private PhieuKham phieuKham;
    private DichVu dichVu;
    private NhanVien nhaSi;
}
