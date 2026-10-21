package com.quanlynhakhoa.entity;

import com.quanlynhakhoa.entity.enums.TrangThaiPhieuKham;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhieuKham {
    private String maPK;
    private LocalDateTime ngayLap;
    private TrangThaiPhieuKham trangThai;
    private String loiDan;
    private HoSoBenhAn hoSoBenhAn;
    private LichHen lichHen;
    private NhanVien leTan;
}
