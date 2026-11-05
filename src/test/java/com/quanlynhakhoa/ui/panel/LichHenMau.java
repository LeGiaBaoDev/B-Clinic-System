package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.entity.DichVu;
import com.quanlynhakhoa.entity.KhachHang;
import com.quanlynhakhoa.entity.LichHen;
import com.quanlynhakhoa.entity.LoaiDichVu;
import com.quanlynhakhoa.entity.NhanVien;
import com.quanlynhakhoa.entity.enums.MucThue;
import com.quanlynhakhoa.entity.enums.TrangThaiLichHen;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Dữ liệu mẫu dùng chung cho test các màn thống kê lịch hẹn (không cần DB). */
final class LichHenMau {

    private LichHenMau() {
    }

    static NhanVien nhaSi(String ma, String ten, String sdt) {
        return new NhanVien(ma, ten, true, sdt, "nhasi@example.com", "Nha sĩ");
    }

    static LichHen lich(String maLH, LocalDateTime gio, TrangThaiLichHen trangThai, String tenKH, String tenDV,
            String tenNhaSi) {
        KhachHang kh = new KhachHang("KH00001", tenKH, LocalDate.of(1990, 1, 1), true, "0912345678",
                "Hà Nội", "khach@example.com");
        DichVu dv = new DichVu("DV001", tenDV, 100000, 30, null, true,
                new LoaiDichVu("LDV01", "Điều trị bệnh lý", MucThue.THUE_0));
        String lyDoHuy = trangThai == TrangThaiLichHen.DA_HUY ? "Khách bận" : null;
        return new LichHen(maLH, gio, trangThai, null, lyDoHuy, kh, nhaSi("NV002", tenNhaSi, "0398682231"), dv, null);
    }
}
