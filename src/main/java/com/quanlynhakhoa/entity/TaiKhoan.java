package com.quanlynhakhoa.entity;

import com.quanlynhakhoa.entity.enums.VaiTro;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaiKhoan {
    private String maTK;
    private String tenTK;
    private String matKhau;
    private VaiTro vaiTro;
    private boolean trangThai;
    private NhanVien nhanVien;
}
