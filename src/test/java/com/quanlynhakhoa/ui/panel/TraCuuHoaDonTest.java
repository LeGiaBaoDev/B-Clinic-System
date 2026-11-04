package com.quanlynhakhoa.ui.panel;

import static org.junit.jupiter.api.Assertions.*;

import com.quanlynhakhoa.entity.HoSoBenhAn;
import com.quanlynhakhoa.entity.HoaDon;
import com.quanlynhakhoa.entity.KhachHang;
import com.quanlynhakhoa.entity.NhanVien;
import com.quanlynhakhoa.entity.PhieuKham;
import com.quanlynhakhoa.entity.enums.TrangThaiPhieuKham;
import com.quanlynhakhoa.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/*
 * Bộ lọc tra cứu hóa đơn (không cần DB, DAO giả)
 * [x] gửi đúng khoảng ngày + trạng thái cho HoaDonDAO (Tất cả = null, Chờ = false, Đã = true)
 * [x] từ khóa: mã hóa đơn, tên khách (không dấu, không phân biệt hoa thường), mã khách, SĐT (bỏ khoảng trắng)
 * [x] mỗi dòng có khách hàng + tổng thanh toán lấy từ DAO; giữ thứ tự DAO trả về
 * [x] không có kết quả -> danh sách rỗng
 * [x] khoảng ngày sai / thiếu -> báo lỗi, không gọi DAO
 * [x] chữ trạng thái, ngày mặc định 30 ngày
 */
class TraCuuHoaDonTest {

    private record GoiDao(LocalDate tu, LocalDate den, Boolean daTra) { }

    private final List<GoiDao> daGoi = new ArrayList<>();
    private final LocalDate homNay = LocalDate.of(2026, 10, 16);

    private static HoaDon hoaDon(String maHD, String maKH, String ten, String sdt, boolean daTra) {
        KhachHang kh = new KhachHang(maKH, ten, LocalDate.of(1995, 5, 20), true, sdt, null, null);
        PhieuKham pk = new PhieuKham("PK" + maHD.substring(2), LocalDateTime.of(2026, 10, 16, 9, 0),
                TrangThaiPhieuKham.HOAN_TAT, null, new HoSoBenhAn(null, null, kh), null, null);
        NhanVien thuNgan = new NhanVien("NV003", "Thảo Linh", false, "0398682232", "letan@example.com", "Lễ tân");
        LocalDateTime lap = LocalDateTime.of(2026, 10, 16, 10, 8);
        return daTra
                ? new HoaDon(maHD, lap, true, HoaDon.TIEN_MAT, lap.plusMinutes(5), 0, pk, null, thuNgan)
                : new HoaDon(maHD, lap, false, null, null, 0, pk, null, null);
    }

    private final HoaDon minhAnh = hoaDon("HD261016042", "KH01284", "Nguyễn Minh Anh", "0901234567", false);
    private final HoaDon ducHuy = hoaDon("HD261016041", "KH01172", "Trần Đức Huy", "0912345678", true);

    private TraCuuHoaDon traCuu(List<HoaDon> duLieu) {
        return new TraCuuHoaDon((tu, den, daTra) -> {
            daGoi.add(new GoiDao(tu, den, daTra));
            return duLieu;
        }, ma -> ma.equals("HD261016042") ? new BigDecimal("1250000") : new BigDecimal("2400000"));
    }

    @Test
    void tim_guiDungKhoangNgayVaTrangThaiChoDao() {
        TraCuuHoaDon tc = traCuu(List.of(minhAnh));
        LocalDate tu = homNay.minusDays(6);

        tc.tim("", TraCuuHoaDon.TrangThai.TAT_CA, tu, homNay);
        tc.tim("", TraCuuHoaDon.TrangThai.CHO_THANH_TOAN, tu, homNay);
        tc.tim("", TraCuuHoaDon.TrangThai.DA_THANH_TOAN, tu, homNay);
        tc.tim("", null, tu, homNay);

        assertEquals(List.of(new GoiDao(tu, homNay, null), new GoiDao(tu, homNay, false),
                new GoiDao(tu, homNay, true), new GoiDao(tu, homNay, null)), daGoi);
    }

    @Test
    void tim_moiDongCoKhachVaTongTien_giuThuTuDao() {
        List<TraCuuHoaDon.Dong> kq = traCuu(List.of(minhAnh, ducHuy))
                .tim(null, TraCuuHoaDon.TrangThai.TAT_CA, homNay, homNay);

        assertEquals(2, kq.size());
        assertEquals("HD261016042", kq.get(0).hoaDon().getMaHD());
        assertEquals("Nguyễn Minh Anh", kq.get(0).khachHang().getTenKH());
        assertEquals(new BigDecimal("1250000"), kq.get(0).tongThanhToan());
        assertEquals(new BigDecimal("2400000"), kq.get(1).tongThanhToan());
    }

    @Test
    void tuKhoa_khopMaHoaDon_tenKhongDau_maKhach_soDienThoai() {
        TraCuuHoaDon tc = traCuu(List.of(minhAnh, ducHuy));
        assertEquals(List.of("HD261016042"), maCua(tc.tim("hd261016042", null, homNay, homNay)));
        assertEquals(List.of("HD261016042"), maCua(tc.tim("nguyen minh", null, homNay, homNay)), "Gõ không dấu");
        assertEquals(List.of("HD261016041"), maCua(tc.tim("  TRẦN   Đức ", null, homNay, homNay)));
        assertEquals(List.of("HD261016041"), maCua(tc.tim("KH01172", null, homNay, homNay)));
        assertEquals(List.of("HD261016041"), maCua(tc.tim("0912 345", null, homNay, homNay)), "SĐT bỏ khoảng trắng");
        assertEquals(List.of("HD261016042", "HD261016041"), maCua(tc.tim("2610160", null, homNay, homNay)),
                "Một phần mã khớp cả hai");
    }

    @Test
    void khongCoKetQua_traDanhSachRong() {
        assertTrue(traCuu(List.of(minhAnh, ducHuy)).tim("không ai tên này", null, homNay, homNay).isEmpty());
        assertTrue(traCuu(List.of()).tim("", null, homNay, homNay).isEmpty());
    }

    @Test
    void khoangNgaySaiHoacThieu_baoLoi_khongGoiDao() {
        TraCuuHoaDon tc = traCuu(List.of(minhAnh));
        BusinessException loi = assertThrows(BusinessException.class,
                () -> tc.tim("", null, homNay, homNay.minusDays(1)));
        assertTrue(loi.getMessage().startsWith("Khoảng ngày không hợp lệ"));
        assertThrows(BusinessException.class, () -> tc.tim("", null, null, homNay));
        assertTrue(daGoi.isEmpty());
    }

    @Test
    void tenTrangThai_vaNgayMacDinh() {
        assertEquals("Chờ thanh toán", TraCuuHoaDon.tenTrangThai(minhAnh));
        assertEquals("Đã thanh toán", TraCuuHoaDon.tenTrangThai(ducHuy));
        assertEquals(LocalDate.of(2026, 9, 17), TraCuuHoaDon.tuNgayMacDinh(homNay), "30 ngày tính cả hôm nay");
        assertEquals("Tất cả", TraCuuHoaDon.TrangThai.TAT_CA.toString());
    }

    @Test
    void khachHang_thieuPhieuKham_traNull() {
        assertNull(TraCuuHoaDon.khachHang(new HoaDon()));
        assertNull(TraCuuHoaDon.khachHang(null));
    }

    private static List<String> maCua(List<TraCuuHoaDon.Dong> ds) {
        return ds.stream().map(d -> d.hoaDon().getMaHD()).toList();
    }
}
