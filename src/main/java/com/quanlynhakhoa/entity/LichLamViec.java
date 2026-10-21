package com.quanlynhakhoa.entity;

import com.quanlynhakhoa.entity.enums.CaTruc;
import com.quanlynhakhoa.entity.enums.TrangThaiLichLamViec;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LichLamViec {
    private String maLLV;
    private LocalDate ngayTruc;
    private CaTruc caTruc;
    private LocalTime gioBatDau;
    private LocalTime gioKetThuc;
    private TrangThaiLichLamViec trangThai;
    private String ghiChuNghi;
    private NhanVien nhanVien;
}
