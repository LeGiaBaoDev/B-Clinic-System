package com.quanlynhakhoa.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NhatKyHeThong {
    private String maNhatKy;
    private LocalDateTime thoiDiem;
    private String hanhDong;
    private String noiDungThayDoi;
    private TaiKhoan taiKhoan;
}
