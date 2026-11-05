package com.quanlynhakhoa.dao;

import static org.junit.jupiter.api.Assertions.*;

import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.dao.ThongKeDichVuDAO.DichVuXepHang;
import com.quanlynhakhoa.dao.ThongKeDichVuDAO.TieuChi;
import com.quanlynhakhoa.entity.DichVu;
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
 * ThongKeDichVuDAO — cần database test (xem DaoTestSupport). Dữ liệu tự tạo vào 1 ngày thanh toán
 * ngẫu nhiên năm 2099 để không lẫn dữ liệu khác; phiếu khám để "DANG_KHAM" để dọn được (phiếu hoàn tất
 * bị trigger cấm xóa — trạng thái phiếu không ảnh hưởng thống kê, chỉ hóa đơn đã thanh toán mới tính).
 * [x] số lượt = số phiếu khám có dùng dịch vụ (2 dòng cùng dịch vụ trong 1 phiếu vẫn 1 lượt)
 * [x] doanh thu = thành tiền + thuế dòng, trừ phần voucher chia theo tỷ lệ; làm tròn tới đồng
 * [x] chỉ tính hóa đơn đã thanh toán, trong khoảng ngày thanh toán
 * [x] xếp theo số lượt / theo doanh thu; giới hạn số dòng; hạng 1..n
 * [x] khoảng ngày sai, giới hạn sai -> báo lỗi; không có dữ liệu -> rỗng; Lễ tân bị chặn
 */
class ThongKeDichVuDAOTest {

    private final ThongKeDichVuDAO dao = new ThongKeDichVuDAO();
    private final DichVuDAO dichVuDAO = new DichVuDAO();
    private final List<String> hoaDon = new ArrayList<>();
    private final List<String> phieuKham = new ArrayList<>();
    private final List<String> dichVu = new ArrayList<>();
    private String maKH;
    private String maHS;
    private String maVC;
    private LocalDate ngay;
    private String tien; // tiền tố tên dịch vụ riêng của lần chạy

    private DichVu caoVoi;
    private DichVu tayTrang;
    private DichVu banChai;

    @BeforeAll
    static void chuanBi() {
        DaoTestSupport.yeuCauDatabaseTest();
    }

    @BeforeEach
    void taoDuLieu() throws SQLException {
        DaoTestSupport.dangNhap(VaiTro.QUANLY);
        ThreadLocalRandom rd = ThreadLocalRandom.current();
        ngay = LocalDate.of(2099, 1, 1).plusDays(rd.nextInt(3000));
        tien = "TK" + rd.nextInt(100_000, 1_000_000);
        caoVoi = themDichVu(" Cạo vôi", 300_000, LoaiDichVu.DIEU_TRI, MucThue.THUE_0);
        tayTrang = themDichVu(" Tẩy trắng", 2_500_000, LoaiDichVu.THAM_MY, MucThue.THUE_10);
        banChai = themDichVu(" Bàn chải điện", 850_000, LoaiDichVu.VAT_TU, MucThue.THUE_5);

        int so = rd.nextInt(80_000, 90_000);
        maKH = "KH" + so;
        maHS = "HS" + so;
        try (Connection c = ketNoi()) {
            chay(c, "INSERT INTO KhachHang(maKH,tenKH,gioiTinh,sdt) VALUES(?,?,1,?)", maKH, "Khách thống kê", "07" + so + "456");
            chay(c, "INSERT INTO HoSoBenhAn(maHSBA,maKH) VALUES(?,?)", maHS, maKH);
            maVC = null;
            for (int i = 0; i < 50 && maVC == null; i++) { // tìm 1 mã voucher còn trống
                String thu = String.format("VC%03d", rd.nextInt(900, 1000));
                if (DaoTestSupport.dem("SELECT COUNT(*) FROM Voucher WHERE maVoucher=?", thu) == 0) {
                    maVC = thu;
                }
            }
            chay(c, "INSERT INTO Voucher(maVoucher,tenVoucher,loaiGiam,giaGiam,ngayBatDau,ngayKetThuc)"
                    + " VALUES(?,?,1,10,'2099-01-01','2108-01-01')", maVC, "Giảm 10% thống kê");
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
     * Dữ liệu chuẩn trong ngày {@code ngay}:
     * HĐ1 (voucher giảm 305.000): Cạo vôi 300.000 KCT + Tẩy trắng 2.500.000 thuế 10%.
     * HĐ2 (không voucher): Cạo vôi 2 dòng (cùng 1 phiếu) + Bàn chải 2 cái × 850.000 thuế 5%.
     * HĐ3 CHƯA thanh toán (Tẩy trắng) và HĐ4 thanh toán NGÀY SAU (Bàn chải) — đều không được tính.
     */
    private void taoHoaDonChuan() throws SQLException {
        taoHoaDon(true, ngay, 305_000, new Object[][]{{caoVoi, 1}, {tayTrang, 1}});
        taoHoaDon(true, ngay, 0, new Object[][]{{caoVoi, 1}, {caoVoi, 1}, {banChai, 2}});
        taoHoaDon(false, ngay, 0, new Object[][]{{tayTrang, 1}});
        taoHoaDon(true, ngay.plusDays(1), 0, new Object[][]{{banChai, 1}});
    }

    @Test
    void xepTheoSoLuot_soLuotLaSoPhieu_doanhThuSauVoucherChiaTheoTyLe() throws SQLException {
        taoHoaDonChuan();

        List<DichVuXepHang> kq = dao.xepHang(ngay, ngay, TieuChi.SO_LUOT);

        assertEquals(List.of(caoVoi.getMaDV(), tayTrang.getMaDV(), banChai.getMaDV()), ma(kq));
        DichVuXepHang cao = kq.get(0);
        assertEquals(1, cao.hang());
        assertEquals(2, cao.soLuot(), "2 phiếu đã thanh toán; 2 dòng trong cùng 1 phiếu vẫn 1 lượt");
        // HĐ1: 300.000 − 305.000 × 300.000/3.050.000 = 270.000; HĐ2: 300.000 + 300.000 = 600.000
        assertEquals(new BigDecimal("870000"), cao.doanhThu());
        assertEquals(new BigDecimal("2475000"), kq.get(1).doanhThu(), "2.750.000 − 275.000 phần voucher");
        assertEquals(1, kq.get(1).soLuot(), "Hóa đơn chưa thanh toán không tính");
        assertEquals(new BigDecimal("1785000"), kq.get(2).doanhThu(), "1.700.000 + thuế 85.000; HĐ ngày sau không tính");
        assertEquals(0, new BigDecimal("300000").compareTo(cao.donGia()), "Đơn giá hiện tại trong danh mục");
        assertEquals(3, kq.get(2).hang());
    }

    @Test
    void xepTheoDoanhThu_vaGioiHanSoDong() throws SQLException {
        taoHoaDonChuan();

        assertEquals(List.of(tayTrang.getMaDV(), banChai.getMaDV(), caoVoi.getMaDV()),
                ma(dao.xepHang(ngay, ngay, TieuChi.DOANH_THU)));
        assertEquals(List.of(tayTrang.getMaDV(), banChai.getMaDV()),
                ma(dao.xepHang(ngay, ngay, TieuChi.DOANH_THU, 2)));
        assertEquals(3, dao.xepHang(ngay, ngay.plusDays(1), TieuChi.SO_LUOT).size());
        assertEquals(2, dao.xepHang(ngay, ngay.plusDays(1), TieuChi.SO_LUOT).stream()
                .filter(d -> d.maDV().equals(banChai.getMaDV())).findFirst().orElseThrow().soLuot(),
                "Mở rộng khoảng ngày thì tính cả HĐ thanh toán ngày sau");
    }

    @Test
    void chiaVoucherLe_lamTronToiDong() throws SQLException {
        // 100.000 + 200.000, voucher 100.000 -> 33.333,33 và 66.666,67 -> doanh thu 66.667 và 133.333
        DichVu a = themDichVu(" Lẻ A", 100_000, LoaiDichVu.DIEU_TRI, MucThue.THUE_0);
        DichVu b = themDichVu(" Lẻ B", 200_000, LoaiDichVu.DIEU_TRI, MucThue.THUE_0);
        taoHoaDon(true, ngay, 100_000, new Object[][]{{a, 1}, {b, 1}});

        List<DichVuXepHang> kq = dao.xepHang(ngay, ngay, TieuChi.DOANH_THU);
        assertEquals(new BigDecimal("133333"), kq.get(0).doanhThu());
        assertEquals(new BigDecimal("66667"), kq.get(1).doanhThu());
    }

    @Test
    void khongCoDuLieu_traRong() {
        assertTrue(dao.xepHang(ngay, ngay, TieuChi.SO_LUOT).isEmpty());
    }

    @Test
    void khoangNgaySai_gioiHanSai_leTan_biChan() {
        assertTrue(assertThrows(BusinessException.class, () -> dao.xepHang(ngay, ngay.minusDays(1), TieuChi.SO_LUOT))
                .getMessage().startsWith("Khoảng thời gian không hợp lệ"));
        assertThrows(BusinessException.class, () -> dao.xepHang(null, ngay, TieuChi.SO_LUOT));
        assertThrows(BusinessException.class, () -> dao.xepHang(ngay, ngay, TieuChi.SO_LUOT, 11));
        DaoTestSupport.dangNhap(VaiTro.LETAN);
        assertThrows(BusinessException.class, () -> dao.xepHang(ngay, ngay, TieuChi.SO_LUOT));
    }

    // ===================== Tạo dữ liệu =====================

    private DichVu themDichVu(String ten, double gia, String tenLoai, MucThue thue) {
        DichVu dv = new DichVu(null, tien + ten, gia, 30, null, true, new LoaiDichVu(null, tenLoai, thue));
        assertTrue(dichVuDAO.them(dv));
        dichVu.add(dv.getMaDV());
        return dv;
    }

    /** Tạo 1 phiếu khám (DANG_KHAM) + các dòng {dịch vụ, số lượng} + 1 hóa đơn. */
    private void taoHoaDon(boolean daTra, LocalDate ngayTT, double giam, Object[][] dong) throws SQLException {
        ThreadLocalRandom rd = ThreadLocalRandom.current();
        String duoi = "99" + String.format("%07d", rd.nextInt(10_000_000));
        String maPK = "PK" + duoi;
        String maHD = "HD" + duoi;
        LocalDateTime lap = ngayTT.atTime(8, 0);
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
            if (daTra) {
                chay(c, "INSERT INTO HoaDon(maHD,maPK,maVoucher,maNV_ThuNgan,ngayLap,trangThai,hinhThucThanhToan,"
                        + "ngayThanhToan,tongKhuyenMai) VALUES(?,?,?,?,?,1,N'Tiền mặt',?,?)",
                        maHD, maPK, giam > 0 ? maVC : null, DaoTestSupport.NV_LE_TAN, lap, lap.plusHours(1),
                        BigDecimal.valueOf(giam));
            } else {
                chay(c, "INSERT INTO HoaDon(maHD,maPK,ngayLap) VALUES(?,?,?)", maHD, maPK, lap);
            }
            hoaDon.add(maHD);
        }
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

    private static List<String> ma(List<DichVuXepHang> ds) {
        return ds.stream().map(DichVuXepHang::maDV).toList();
    }
}
