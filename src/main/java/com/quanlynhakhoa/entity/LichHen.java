package com.quanlynhakhoa.entity;

import com.quanlynhakhoa.entity.enums.TrangThaiLichHen;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LichHen {
    private String maLH;
    private LocalDateTime ngayGioHen;
    private TrangThaiLichHen trangThai;
    private String ghiChu;
    private String lyDoHuy;
    private KhachHang khachHang;
    private NhanVien nhaSi;
    private DichVu dichVu;
    private PhieuKham phieuKhamNguon;
}
