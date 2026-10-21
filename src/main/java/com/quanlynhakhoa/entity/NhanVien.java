package com.quanlynhakhoa.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NhanVien {
    private String maNV;
    private String hoTenNV;
    private boolean gioiTinh;
    private String sdt;
    private String email;
    private String chuyenMon;
}
