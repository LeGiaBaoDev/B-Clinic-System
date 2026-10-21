package com.quanlynhakhoa.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HoSoBenhAn {
    private String maHSBA;
    private String tienSuBenh;
    private KhachHang khachHang;
}
