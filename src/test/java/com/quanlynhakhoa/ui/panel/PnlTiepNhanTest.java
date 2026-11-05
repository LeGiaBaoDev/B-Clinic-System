package com.quanlynhakhoa.ui.panel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quanlynhakhoa.entity.DichVu;
import com.quanlynhakhoa.entity.KhachHang;
import com.quanlynhakhoa.entity.LichHen;
import com.quanlynhakhoa.entity.LoaiDichVu;
import com.quanlynhakhoa.entity.NhanVien;
import com.quanlynhakhoa.entity.enums.MucThue;
import com.quanlynhakhoa.entity.enums.TrangThaiLichHen;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PnlTiepNhanTest {

    @Test
    void khopTheoMaTenVaSoDienThoai() {
        LichHen lh = lich();

        assertTrue(PnlTiepNhan.khop(lh, "LH000000123"));
        assertTrue(PnlTiepNhan.khop(lh, "nguyễn minh anh"));
        assertTrue(PnlTiepNhan.khop(lh, "0903456789"));
        assertTrue(PnlTiepNhan.khop(lh, "kh00012"));
        assertTrue(PnlTiepNhan.khop(lh, ""));
    }

    @Test
    void khongKhopTuKhoaKhac() {
        assertFalse(PnlTiepNhan.khop(lich(), "trần đức huy"));
    }

    private static LichHen lich() {
        LichHen lh = new LichHen();
        lh.setMaLH("LH000000123");
        lh.setNgayGioHen(LocalDateTime.of(2026, 11, 4, 9, 30));
        lh.setKhachHang(new KhachHang("KH00012", "Nguyễn Minh Anh",
                LocalDate.of(1998, 3, 12), false, "0903456789", "TP.HCM", null));
        lh.setNhaSi(new NhanVien("NV002", "Trần Hoàng Nam", true, "0398682231",
                "nhasi@example.com", "Nha sĩ"));
        lh.setDichVu(new DichVu("DV001", "Khám tổng quát", 100_000, 30, null, true,
                new LoaiDichVu("LDV01", "Điều trị bệnh lý", MucThue.THUE_0)));
        lh.setTrangThai(TrangThaiLichHen.DA_DAT);
        return lh;
    }
}
