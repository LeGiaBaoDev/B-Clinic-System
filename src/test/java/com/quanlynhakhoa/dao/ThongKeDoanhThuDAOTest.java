package com.quanlynhakhoa.dao;

import static org.junit.jupiter.api.Assertions.*;

import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.DoanhThuMotNgay;
import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.DoanhThuTrongNgay;
import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.HoaDonTrongNgay;
import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.TongHopDoanhThu;
import com.quanlynhakhoa.entity.DichVu;
import com.quanlynhakhoa.entity.HoaDon;
import com.quanlynhakhoa.entity.LoaiDichVu;
import com.quanlynhakhoa.entity.enums.MucThue;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/*
 * Các hàm báo cáo doanh thu mới của ThongKeDoanhThuDAO — cần database test (xem DaoTestSupport).
 * Dữ liệu tự tạo vào 1 ngày ngẫu nhiên năm 2099 để không lẫn dữ liệu khác; tự dọn sau mỗi test.
 * [x] tổng hợp kỳ: tiền hàng, thuế, giảm voucher, thực nhận, tách tiền mặt / chuyển khoản; chỉ hóa đơn đã
 *     thanh toán, theo NGÀY THANH TOÁN
 * [x] doanh thu từng ngày đủ mọi ngày trong khoảng (ngày không có = 0)
 * [x] doanh thu trong ngày (Lễ tân): mọi hóa đơn LẬP trong ngày, gồm cả chưa thanh toán; tổng khớp bảng
 * [x] không có dữ liệu -> số 0 / danh sách rỗng; khoảng ngày sai -> báo lỗi
 * [x] Lễ tân xem được doanh thu trong ngày, không xem được tổng hợp kỳ
 */
class ThongKeDoanhThuDAOTest {

    private final ThongKeDoanhThuDAO dao = new ThongKeDoanhThuDAO();
    private final DichVuDAO dichVuDAO = new DichVuDAO();
    private final List<String> hoaDon = new ArrayList<>();
    private final List<String> phieuKham = new ArrayList<>();
    private final List<String> dichVu = new ArrayList<>();
    private String maKH;
    private String maHS;
    private String maVC;
    private LocalDate ngay;
    private String tien; // tiền tố tên dịch vụ riêng của lần chạy

    private DichVu caoVoi;   // 300.000, không chịu thuế
    private DichVu tayTrang; // 2.500.000, thuế 10%
    private DichVu banChai;  // 850.000, thuế 5%

    @BeforeAll
    static void chuanBi() {
        DaoTestSupport.yeuCauDatabaseTest();
    }

    @BeforeEach
    void taoDuLieu() throws SQLException {
        DaoTestSupport.dangNhap(VaiTro.QUANLY);
        ThreadLocalRandom rd = ThreadLocalRandom.current();
        ngay = LocalDate.of(2099, 1, 2).plusDays(rd.nextInt(3000));
        tien = "DT" + rd.nextInt(100_000, 1_000_000);
        caoVoi = themDichVu(" Cạo vôi", 300_000, LoaiDichVu.DIEU_TRI, MucThue.THUE_0);
        tayTrang = themDichVu(" Tẩy trắng", 2_500_000, LoaiDichVu.THAM_MY, MucThue.THUE_10);
        banChai = themDichVu(" Bàn chải điện", 850_000, LoaiDichVu.VAT_TU, MucThue.THUE_5);

        int so = rd.nextInt(90_000, 100_000);
        maKH = "KH" + so;
        maHS = "HS" + so;
        try (Connection c = ketNoi()) {
            chay(c, "INSERT INTO KhachHang(maKH,tenKH,gioiTinh,sdt) VALUES(?,?,1,?)", maKH, "Khách doanh thu", "08" + so + "456");
            chay(c, "INSERT INTO HoSoBenhAn(maHSBA,maKH) VALUES(?,?)", maHS, maKH);
            maVC = null;
            for (int i = 0; i < 50 && maVC == null; i++) { // tìm 1 mã voucher còn trống
                String thu = String.format("VC%03d", rd.nextInt(800, 900));
                if (DaoTestSupport.dem("SELECT COUNT(*) FROM Voucher WHERE maVoucher=?", thu) == 0) {
                    maVC = thu;
                }
            }
            chay(c, "INSERT INTO Voucher(maVoucher,tenVoucher,loaiGiam,giaGiam,ngayBatDau,ngayKetThuc)"
                    + " VALUES(?,?,1,10,'2099-01-01','2108-01-01')", maVC, "Giảm 10% doanh thu");
        }
    }

    @AfterEach
    void don() throws SQLException {
        try (Connection c = ketNoi()) {
            for (String hd : hoaDon) {
                chay(c, "DELETE FROM HoaDon WHERE maHD=?", hd); // xóa hóa đơn trước thì trigger mới cho xóa chi tiết
            }
            for (String pk : phieuKham) {
                chay(c, "DELETE FROM PhieuKham WHERE maPK=?", pk); // chi tiết xóa theo (ON DELETE CASCADE)
            }
            chay(c, "DELETE FROM HoSoBenhAn WHERE maHSBA=?", maHS);
            chay(c, "DELETE FROM KhachHang WHERE maKH=?", maKH);
            chay(c, "DELETE FROM Voucher WHERE maVoucher=?", maVC);
            for (String dv : dichVu) {
                chay(c, "DELETE FROM DichVu WHERE maDV=?", dv);
            }
        } finally {
            DaoTestSupport.dangXuat();
        }
    }

    /**
     * Dữ liệu chuẩn, mọi hóa đơn LẬP trong ngày {@code ngay}:
     * HĐ1 08:00 tiền mặt, voucher 280.000: Cạo vôi + Tẩy trắng -> hàng 2.800.000, thuế 250.000, thực nhận 2.770.000.
     * HĐ2 09:00 chuyển khoản: Bàn chải × 2 -> hàng 1.700.000, thuế 85.000, thực nhận 1.785.000.
     * HĐ3 10:00 CHƯA thanh toán: Cạo vôi -> 300.000.
     * HĐ4 11:00 tiền mặt nhưng THANH TOÁN NGÀY SAU: Bàn chải × 1 -> 850.000 + 42.500 = 892.500.
     */
    private void taoHoaDonChuan() throws SQLException {
        taoHoaDon(ngay.atTime(8, 0), ngay.atTime(8, 30), HoaDon.TIEN_MAT, 280_000,
                new Object[][]{{caoVoi, 1}, {tayTrang, 1}});
        taoHoaDon(ngay.atTime(9, 0), ngay.atTime(9, 30), HoaDon.CHUYEN_KHOAN, 0, new Object[][]{{banChai, 2}});
        taoHoaDon(ngay.atTime(10, 0), null, null, 0, new Object[][]{{caoVoi, 1}});
        taoHoaDon(ngay.atTime(11, 0), ngay.plusDays(1).atTime(8, 0), HoaDon.TIEN_MAT, 0, new Object[][]{{banChai, 1}});
    }

    @Test
    void tongHop_tachThueVoucherHinhThuc_theoNgayThanhToan() throws SQLException {
        taoHoaDonChuan();

        TongHopDoanhThu motNgay = dao.tongHopDoanhThu(ngay, ngay);
        assertEquals(2, motNgay.soHoaDon(), "HĐ chưa thanh toán và HĐ thanh toán ngày sau không tính");
        tien(4_500_000, motNgay.tienHang());
        tien(335_000, motNgay.thueGTGT());
        tien(280_000, motNgay.giamGia());
        tien(4_555_000, motNgay.thucNhan(), "4.500.000 + 335.000 − 280.000");
        tien(2_770_000, motNgay.tienMat());
        tien(1_785_000, motNgay.chuyenKhoan());

        TongHopDoanhThu haiNgay = dao.tongHopDoanhThu(ngay, ngay.plusDays(1));
        assertEquals(3, haiNgay.soHoaDon());
        tien(5_447_500, haiNgay.thucNhan());
        tien(3_662_500, haiNgay.tienMat());
        assertEquals(0, haiNgay.thucNhan().compareTo(haiNgay.tienMat().add(haiNgay.chuyenKhoan())),
                "Thực nhận = tiền mặt + chuyển khoản");
    }

    @Test
    void doanhThuTungNgay_duMoiNgay() throws SQLException {
        taoHoaDonChuan();

        List<DoanhThuMotNgay> ds = dao.doanhThuTungNgay(ngay.minusDays(1), ngay.plusDays(1));
        assertEquals(List.of(ngay.minusDays(1), ngay, ngay.plusDays(1)), ds.stream().map(DoanhThuMotNgay::ngay).toList());
        tien(0, ds.get(0).thucNhan(), "Ngày không có hóa đơn vẫn có điểm 0");
        tien(4_555_000, ds.get(1).thucNhan());
        tien(892_500, ds.get(2).thucNhan(), "HĐ4 tính vào ngày thanh toán");
    }

    @Test
    void doanhThuTrongNgay_gomCaChuaThanhToan_tongKhopBang() throws SQLException {
        taoHoaDonChuan();
        DaoTestSupport.dangNhap(VaiTro.LETAN); // trang này của Lễ tân

        DoanhThuTrongNgay kq = dao.doanhThuTrongNgay(ngay);
        assertEquals(4, kq.soHoaDon(), "Mọi hóa đơn lập trong ngày, kể cả chưa thanh toán");
        List<HoaDonTrongNgay> ds = kq.hoaDon();
        assertEquals(hoaDon.reversed(), ds.stream().map(HoaDonTrongNgay::maHD).toList(), "Mới nhất ở trên");
        HoaDonTrongNgay chuaTra = ds.get(1);
        assertFalse(chuaTra.daThanhToan());
        assertNull(chuaTra.hinhThuc());
        assertEquals("Khách doanh thu", chuaTra.tenKH());
        tien(300_000, chuaTra.tongTien());
        tien(5_747_500, kq.tamTinh(), "2.770.000 + 1.785.000 + 300.000 + 892.500");
        tien(5_447_500, kq.daThu());
        tien(300_000, kq.chuaThu());
        tien(3_662_500, kq.tienMat());
        tien(1_785_000, kq.chuyenKhoan());
    }

    @Test
    void khongCoDuLieu_traSo0() {
        TongHopDoanhThu kq = dao.tongHopDoanhThu(ngay, ngay);
        assertEquals(0, kq.soHoaDon());
        tien(0, kq.thucNhan());
        tien(0, kq.tienMat());
        DoanhThuTrongNgay trongNgay = dao.doanhThuTrongNgay(ngay);
        assertTrue(trongNgay.hoaDon().isEmpty());
        tien(0, trongNgay.tamTinh());
    }

    @Test
    void khoangNgaySai_vaPhanQuyen() {
        assertTrue(assertThrows(BusinessException.class, () -> dao.tongHopDoanhThu(ngay, ngay.minusDays(1)))
                .getMessage().startsWith("Khoảng thời gian không hợp lệ"));
        assertThrows(BusinessException.class, () -> dao.doanhThuTungNgay(null, ngay));
        assertThrows(BusinessException.class, () -> dao.doanhThuTrongNgay(null));
        DaoTestSupport.dangNhap(VaiTro.LETAN);
        assertThrows(BusinessException.class, () -> dao.tongHopDoanhThu(ngay, ngay), "Lễ tân không xem báo cáo kỳ");
        assertThrows(BusinessException.class, () -> dao.doanhThuTungNgay(ngay, ngay));
        assertDoesNotThrow(() -> dao.doanhThuTrongNgay(ngay));
    }

    // ===================== Tạo dữ liệu =====================

    private DichVu themDichVu(String ten, double gia, String tenLoai, MucThue thue) {
        DichVu dv = new DichVu(null, tien + ten, gia, 30, null, true, new LoaiDichVu(null, tenLoai, thue));
        assertTrue(dichVuDAO.them(dv));
        dichVu.add(dv.getMaDV());
        return dv;
    }

    /**
     * Tạo 1 phiếu khám (DANG_KHAM, để dọn được) + các dòng {dịch vụ, số lượng} + 1 hóa đơn lập lúc {@code lap}.
     * {@code thanhToan} null -> hóa đơn chưa thanh toán.
     */
    private void taoHoaDon(LocalDateTime lap, LocalDateTime thanhToan, String hinhThuc, double giam, Object[][] dong)
            throws SQLException {
        ThreadLocalRandom rd = ThreadLocalRandom.current();
        String duoi = "98" + String.format("%07d", rd.nextInt(10_000_000));
        String maPK = "PK" + duoi;
        String maHD = "HD" + duoi;
        try (Connection c = ketNoi()) {
            chay(c, "INSERT INTO PhieuKham(maPK,maHSBA,maNV_LapPhieu,ngayLap,trangThai) VALUES(?,?,?,?,'DANG_KHAM')",
                    maPK, maHS, DaoTestSupport.NV_LE_TAN, lap);
            phieuKham.add(maPK);
            for (Object[] d : dong) {
                DichVu dv = (DichVu) d[0];
                int thue = switch (dv.getLoaiDichVu().getMucThueGTGT()) {
                    case THUE_0 -> 0;
                    case THUE_5 -> 5;
                    case THUE_10 -> 10;
                };
                chay(c, "INSERT INTO ChiTietPhieuKham(maPK,maDV,maNV_NhaSi,trieuChung,chanDoan,donViTinh,soLuong,"
                        + "donGia,mucThueGTGT) VALUES(?,?,?,N'Kiểm thử',N'Kiểm thử',N'Lần',?,?,?)",
                        maPK, dv.getMaDV(), DaoTestSupport.NV_NHA_SI, d[1], BigDecimal.valueOf(dv.getDonGia()), thue);
            }
            if (thanhToan != null) {
                chay(c, "INSERT INTO HoaDon(maHD,maPK,maVoucher,maNV_ThuNgan,ngayLap,trangThai,hinhThucThanhToan,"
                        + "ngayThanhToan,tongKhuyenMai) VALUES(?,?,?,?,?,1,?,?,?)",
                        maHD, maPK, giam > 0 ? maVC : null, DaoTestSupport.NV_LE_TAN, lap, hinhThuc, thanhToan,
                        BigDecimal.valueOf(giam));
            } else {
                chay(c, "INSERT INTO HoaDon(maHD,maPK,ngayLap) VALUES(?,?,?)", maHD, maPK, lap);
            }
            hoaDon.add(maHD);
        }
    }

    private static void tien(long mongDoi, BigDecimal thucTe) {
        tien(mongDoi, thucTe, null);
    }

    /** So sánh tiền theo giá trị (bỏ qua số chữ số thập phân: 300000 = 300000.00). */
    private static void tien(long mongDoi, BigDecimal thucTe, String ghiChu) {
        assertNotNull(thucTe, ghiChu);
        assertEquals(0, BigDecimal.valueOf(mongDoi).compareTo(thucTe),
                (ghiChu == null ? "" : ghiChu + " — ") + "mong đợi " + mongDoi + ", thực tế " + thucTe);
    }

    private static Connection ketNoi() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    private static void chay(Connection c, String sql, Object... thamSo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < thamSo.length; i++) {
                ps.setObject(i + 1, thamSo[i]);
            }
            ps.executeUpdate();
        }
    }
}
