package com.quanlynhakhoa.ui.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quanlynhakhoa.entity.DichVu;
import com.quanlynhakhoa.entity.KhachHang;
import com.quanlynhakhoa.entity.LichHen;
import com.quanlynhakhoa.entity.LichLamViec;
import com.quanlynhakhoa.entity.LoaiDichVu;
import com.quanlynhakhoa.entity.NhanVien;
import com.quanlynhakhoa.entity.PhieuKham;
import com.quanlynhakhoa.entity.enums.CaTruc;
import com.quanlynhakhoa.entity.enums.MucThue;
import com.quanlynhakhoa.entity.enums.TrangThaiLichHen;
import com.quanlynhakhoa.entity.enums.TrangThaiLichLamViec;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class PnlLichNhaSiTest {

    private static final LocalDate HOM_NAY = LocalDate.of(2026, 11, 4);

    @Test
    void thongKe_chiTinhHomNay_vaBoQuaLichHuy() {
        List<LichHen> ds = List.of(
                lich("LH000000001", HOM_NAY.atTime(8, 30), TrangThaiLichHen.DA_DAT, "KH00001", "Nguyễn An", "Cạo vôi", false),
                lich("LH000000002", HOM_NAY.atTime(9, 0), TrangThaiLichHen.DA_DEN, "KH00002", "Trần Bình", "Trám răng", false),
                lich("LH000000003", HOM_NAY.atTime(10, 0), TrangThaiLichHen.HOAN_TAT, "KH00003", "Lê Chi", "Nhổ răng", true),
                lich("LH000000004", HOM_NAY.atTime(11, 0), TrangThaiLichHen.DA_HUY, "KH00004", "Vũ Dũng", "Tẩy trắng", false),
                lich("LH000000005", HOM_NAY.plusDays(1).atTime(9, 0), TrangThaiLichHen.DA_DAT, "KH00005", "Đỗ Em", "Cạo vôi", false));

        PnlLichNhaSi.ThongKe tk = PnlLichNhaSi.tinhThongKe(ds, HOM_NAY);

        assertEquals(3, tk.homNay());
        assertEquals(1, tk.hoanTat());
        assertEquals(1, tk.choKham());
        assertEquals(1, tk.taiKham());
    }

    @Test
    void locDanhSach_homNay_chiLayDungNgay() {
        List<LichHen> ds = List.of(
                lich("LH000000001", HOM_NAY.atTime(8, 30), TrangThaiLichHen.DA_DAT, "KH00001", "Nguyễn An", "Cạo vôi", false),
                lich("LH000000002", HOM_NAY.plusDays(1).atTime(9, 0), TrangThaiLichHen.DA_DAT, "KH00002", "Trần Bình", "Trám răng", false));

        assertEquals(1, PnlLichNhaSi.locDanhSach(ds, HOM_NAY, false, null, "").size());
        assertEquals(2, PnlLichNhaSi.locDanhSach(ds, HOM_NAY, true, null, "").size());
    }

    @Test
    void locDanhSach_theoTrangThai() {
        List<LichHen> ds = List.of(
                lich("LH000000001", HOM_NAY.atTime(8, 30), TrangThaiLichHen.DA_DAT, "KH00001", "Nguyễn An", "Cạo vôi", false),
                lich("LH000000002", HOM_NAY.atTime(9, 0), TrangThaiLichHen.DA_DEN, "KH00002", "Trần Bình", "Trám răng", false));

        List<LichHen> ketQua = PnlLichNhaSi.locDanhSach(ds, HOM_NAY, false, TrangThaiLichHen.DA_DEN, null);

        assertEquals(1, ketQua.size());
        assertEquals("LH000000002", ketQua.getFirst().getMaLH());
    }

    @Test
    void locDanhSach_theoTuKhoa_khongPhanBietHoaThuong() {
        List<LichHen> ds = List.of(
                lich("LH000000001", HOM_NAY.atTime(8, 30), TrangThaiLichHen.DA_DAT, "KH00001", "Nguyễn An", "Cạo vôi", false),
                lich("LH000000002", HOM_NAY.atTime(9, 0), TrangThaiLichHen.DA_DAT, "KH00002", "Trần Bình", "Trám răng", false));

        assertEquals(1, PnlLichNhaSi.locDanhSach(ds, HOM_NAY, false, null, "NGUYỄN").size());
        assertEquals(1, PnlLichNhaSi.locDanhSach(ds, HOM_NAY, false, null, "kh00002").size());
        assertEquals(1, PnlLichNhaSi.locDanhSach(ds, HOM_NAY, false, null, "trám").size());
        assertEquals(0, PnlLichNhaSi.locDanhSach(ds, HOM_NAY, false, null, "không có").size());
    }

    @Test
    void khungTrong_tinhDungKhoangGiuaCacLichHen() {
        List<LichHen> ds = List.of(
                lich("LH000000001", HOM_NAY.atTime(8, 30), TrangThaiLichHen.DA_DEN, "KH00001", "Nguyễn An", "Cạo vôi", false),
                lich("LH000000002", HOM_NAY.atTime(9, 30), TrangThaiLichHen.DA_DAT, "KH00002", "Trần Bình", "Trám răng", false));

        List<String> khung = PnlLichNhaSi.tinhKhungTrong(ds, List.of(caLamViec()));

        assertEquals(List.of("08:00–08:30", "09:00–09:30", "10:00–12:00"), khung);
    }

    @Test
    void khungTrong_boQuaLichHuy() {
        List<LichHen> ds = List.of(
                lich("LH000000001", HOM_NAY.atTime(8, 0), TrangThaiLichHen.DA_HUY, "KH00001", "Nguyễn An", "Cạo vôi", false));

        List<String> khung = PnlLichNhaSi.tinhKhungTrong(ds, List.of(caLamViec()));

        assertEquals(List.of("08:00–12:00"), khung);
    }

    @Test
    void nhanTrangThai_vaTaiKham() {
        assertEquals("Đã đặt", PnlLichNhaSi.nhanTrangThai(TrangThaiLichHen.DA_DAT));
        assertEquals("Không đến", PnlLichNhaSi.nhanTrangThai(TrangThaiLichHen.KHONG_DEN));
        assertTrue(PnlLichNhaSi.laTaiKham(lich("LH000000002", HOM_NAY.atTime(9, 0),
                TrangThaiLichHen.DA_DAT, "KH00002", "Trần Bình", "Trám răng", true)));
        assertFalse(PnlLichNhaSi.laTaiKham(lich("LH000000003", HOM_NAY.atTime(10, 0),
                TrangThaiLichHen.DA_DAT, "KH00003", "Lê Chi", "Nhổ răng", false)));
    }

    private static LichLamViec caLamViec() {
        return new LichLamViec("LLV00001", HOM_NAY, CaTruc.CA_SANG,
                LocalTime.of(8, 0), LocalTime.of(12, 0),
                TrangThaiLichLamViec.DA_PHAN_CONG, null,
                new NhanVien("NV002", "Nha sĩ kiểm thử", true, "0398682231",
                        "nhasi@example.com", "Nha sĩ"));
    }

    private static LichHen lich(String ma, LocalDateTime thoiGian, TrangThaiLichHen trangThai,
            String maKH, String tenKH, String tenDV, boolean taiKham) {
        LichHen lh = new LichHen();
        lh.setMaLH(ma);
        lh.setNgayGioHen(thoiGian);
        lh.setKhachHang(new KhachHang(maKH, tenKH, LocalDate.of(1990, 1, 1), true,
                "0912345678", "Hà Nội", null));
        lh.setNhaSi(new NhanVien("NV002", "Nha sĩ kiểm thử", true, "0398682231",
                "nhasi@example.com", "Nha sĩ"));
        lh.setDichVu(new DichVu("DV001", tenDV, 100_000, 30, null, true,
                new LoaiDichVu("LDV01", "Điều trị bệnh lý", MucThue.THUE_0)));
        if (taiKham) {
            lh.setPhieuKhamNguon(new PhieuKham());
        }
        if (trangThai == TrangThaiLichHen.DA_HUY) {
            lh.setLyDoHuy("Khách bận");
        }
        lh.setTrangThai(trangThai);
        return lh;
    }
}
