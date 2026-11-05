package com.quanlynhakhoa.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.dao.ThongKeLichHenDAO.TongHop;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** T1101/T1103 – thống kê lịch hẹn dùng chung cho UC029 (quản lý), UC032 (lễ tân), UC034 (nha sĩ). */
class ThongKeLichHenDAOTest {

    /** Xa hiện tại để không lẫn với dữ liệu của test khác. */
    private static final LocalDate NGAY = LocalDate.now().plusDays(300);
    private static final String NHA_SI = "NV002";
    private static final String NHA_SI_KHAC = "NV001";
    private static final DateTimeFormatter YYMMDD = DateTimeFormatter.ofPattern("yyMMdd");
    private static final AtomicInteger DEM = new AtomicInteger(700);

    private final ThongKeLichHenDAO dao = new ThongKeLichHenDAO();
    private final String hauTo = String.valueOf(ThreadLocalRandom.current().nextInt(10_000, 100_000));
    private String maKH;

    @BeforeAll
    static void chuanBi() {
        DaoTestSupport.yeuCauDatabaseTest();
    }

    @BeforeEach
    void taoDuLieu() throws SQLException {
        DaoTestSupport.dangNhap(VaiTro.QUANLY);
        maKH = "KH" + hauTo;
        String sdt = "09" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        try (Connection c = ketNoi()) {
            JdbcHelper.update(c, "IF NOT EXISTS(SELECT 1 FROM LoaiDichVu WHERE maLoaiDV='LDV98')"
                    + " INSERT INTO LoaiDichVu(maLoaiDV,tenLoaiDV,mucThueGTGT)"
                    + " VALUES('LDV98',N'Điều trị bệnh lý',0.00)");
            JdbcHelper.update(c, "IF NOT EXISTS(SELECT 1 FROM DichVu WHERE maDV='DV996')"
                    + " INSERT INTO DichVu(maDV,maLoaiDV,tenDV,donGia,thoiLuong,trangThai)"
                    + " VALUES('DV996','LDV98',N'Dịch vụ kiểm thử thống kê',100000,30,1)");
            JdbcHelper.update(c, "INSERT INTO KhachHang(maKH,tenKH,gioiTinh,sdt) VALUES(?,N'Zxqv Thống Kê',1,?)",
                    maKH, sdt);
        }
    }

    @AfterEach
    void don() throws SQLException {
        try (Connection c = ketNoi()) {
            JdbcHelper.update(c, "DELETE FROM LichHen WHERE maKH=?", maKH);
            JdbcHelper.update(c, "DELETE FROM KhachHang WHERE maKH=?", maKH);
            JdbcHelper.update(c, "DELETE FROM DichVu WHERE maDV='DV996'");
            JdbcHelper.update(c, "DELETE FROM LoaiDichVu WHERE maLoaiDV='LDV98'");
        }
        DaoTestSupport.dangXuat();
    }

    // ------------------------------------------------------------------ đếm theo trạng thái

    @Test
    void tongHop_demDungTungTrangThai() throws SQLException {
        lichNgay(NGAY, 1, "DA_DAT");
        lichNgay(NGAY, 2, "DA_DEN");
        lichNgay(NGAY, 3, "HOAN_TAT");
        lichNgay(NGAY, 4, "HOAN_TAT");
        lichNgay(NGAY, 5, "DA_HUY");
        lichNgay(NGAY, 6, "KHONG_DEN");

        TongHop t = dao.tongHop(NGAY, NGAY, null);

        assertEquals(6, t.tong());
        assertEquals(1, t.daDat());
        assertEquals(1, t.daDen());
        assertEquals(2, t.hoanTat());
        assertEquals(1, t.daHuy());
        assertEquals(1, t.khongDen());
        assertTrue(t.coDuLieu());
    }

    @Test
    void tongHop_tyLeTinhTheoPhanTramTrenTong() throws SQLException {
        lichNgay(NGAY, 1, "HOAN_TAT");
        lichNgay(NGAY, 2, "HOAN_TAT");
        lichNgay(NGAY, 3, "DA_HUY");
        lichNgay(NGAY, 4, "KHONG_DEN");

        TongHop t = dao.tongHop(NGAY, NGAY, null);

        assertEquals(50.0, t.tyLeHoanTat(), 1e-9);
        assertEquals(50.0, t.tyLeChuyenDoi(), 1e-9);
        assertEquals(25.0, t.tyLeDaHuy(), 1e-9);
        assertEquals(25.0, t.tyLeKhongDen(), 1e-9);
        assertEquals(0.0, t.tyLeDaDat(), 1e-9);
        assertEquals(0.0, t.tyLeDaDen(), 1e-9);
    }

    @Test
    void tongHop_khongCoDuLieu_traVeKhongVaTyLeKhong() {
        TongHop t = dao.tongHop(NGAY.plusDays(20), NGAY.plusDays(25), null);

        assertEquals(0, t.tong());
        assertFalse(t.coDuLieu());
        assertEquals(0.0, t.tyLeChuyenDoi(), 1e-9);
        assertEquals(0.0, t.tyLeDaHuy(), 1e-9);
    }

    // ------------------------------------------------------------------ khoảng ngày

    @Test
    void tongHop_gopNhieuNgay_vaTheoNgayTachRieng() throws SQLException {
        lichNgay(NGAY, 1, "HOAN_TAT");
        lichNgay(NGAY.plusDays(2), 1, "DA_HUY");
        lichNgay(NGAY.plusDays(2), 2, "HOAN_TAT");

        TongHop t = dao.tongHop(NGAY, NGAY.plusDays(2), null);

        assertEquals(3, t.tong());
        assertEquals(2, t.hoanTat());
        assertEquals(1, t.daHuy());
        assertEquals(2, dao.thongKe(NGAY, NGAY.plusDays(2), null).size());
    }

    @Test
    void tongHop_ngayCuoiKhoangTinhDenHetNgay() throws SQLException {
        chenLich(NGAY, 23, 50, "HOAN_TAT", NHA_SI);

        assertEquals(1, dao.tongHop(NGAY, NGAY, null).tong());
    }

    @Test
    void tongHop_khongTinhNgayNgoaiKhoang() throws SQLException {
        lichNgay(NGAY.minusDays(1), 1, "HOAN_TAT");
        lichNgay(NGAY, 1, "HOAN_TAT");
        lichNgay(NGAY.plusDays(1), 1, "HOAN_TAT");

        assertEquals(1, dao.tongHop(NGAY, NGAY, null).tong());
    }

    @Test
    void tongHop_khoangNgayNguocHoacThieu_nemLoi() {
        BusinessException a = assertThrows(BusinessException.class,
                () -> dao.tongHop(NGAY.plusDays(1), NGAY, null));
        BusinessException b = assertThrows(BusinessException.class,
                () -> dao.tongHop(null, NGAY, null));

        assertEquals("Khoảng ngày không hợp lệ.", a.getMessage());
        assertEquals("Khoảng ngày không hợp lệ.", b.getMessage());
    }

    // ------------------------------------------------------------------ lọc theo nha sĩ

    @Test
    void tongHop_locTheoNhaSi_chiTinhLichCuaNhaSiDo() throws SQLException {
        chenLich(NGAY, 9, 0, "HOAN_TAT", NHA_SI);
        chenLich(NGAY, 10, 0, "DA_HUY", NHA_SI);
        chenLich(NGAY, 11, 0, "HOAN_TAT", NHA_SI_KHAC);

        TongHop rieng = dao.tongHop(NGAY, NGAY, NHA_SI);
        TongHop tatCa = dao.tongHop(NGAY, NGAY, null);

        assertEquals(2, rieng.tong());
        assertEquals(1, rieng.hoanTat());
        assertEquals(3, tatCa.tong());
        assertEquals(2, tatCa.hoanTat());
    }

    @Test
    void tongHop_locNhaSiKhongCoLich_traVeKhong() throws SQLException {
        lichNgay(NGAY, 1, "HOAN_TAT");

        assertEquals(0, dao.tongHop(NGAY, NGAY, "NV999").tong());
    }

    // ------------------------------------------------------------------ UC032 theo ngày

    @Test
    void theoNgay_giongTongHopMotNgay() throws SQLException {
        lichNgay(NGAY, 1, "DA_DAT");
        lichNgay(NGAY, 2, "HOAN_TAT");

        TongHop t = dao.theoNgay(NGAY);

        assertEquals(dao.tongHop(NGAY, NGAY, null), t);
        assertEquals(2, t.tong());
    }

    @Test
    void theoNgay_thieuNgay_nemLoi() {
        assertThrows(BusinessException.class, () -> dao.theoNgay(null));
    }

    // ------------------------------------------------------------------ phân quyền

    @Test
    void leTan_xemTheoNgay_duoc() throws SQLException {
        lichNgay(NGAY, 1, "DA_DAT");
        DaoTestSupport.dangNhap(VaiTro.LETAN);

        assertEquals(1, dao.theoNgay(NGAY).daDat());
    }

    @Test
    void leTan_xemNhieuNgay_biChan() {
        DaoTestSupport.dangNhap(VaiTro.LETAN);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dao.tongHop(NGAY, NGAY.plusDays(1), null));

        assertEquals("Lễ tân chỉ được xem thống kê theo từng ngày.", ex.getMessage());
    }

    @Test
    void nhaSi_chiThayLichCuaMinh() throws SQLException {
        chenLich(NGAY, 9, 0, "HOAN_TAT", NHA_SI);
        chenLich(NGAY, 10, 0, "HOAN_TAT", NHA_SI_KHAC);
        DaoTestSupport.dangNhap(VaiTro.NHASI);

        assertEquals(1, dao.tongHop(NGAY, NGAY, null).tong());
        assertEquals(1, dao.tongHop(NGAY, NGAY, NHA_SI).tong());
    }

    @Test
    void nhaSi_xemNhaSiKhac_biChan() {
        DaoTestSupport.dangNhap(VaiTro.NHASI);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dao.tongHop(NGAY, NGAY, NHA_SI_KHAC));

        assertEquals("Chỉ được thống kê lịch của mình.", ex.getMessage());
    }

    @Test
    void chuaDangNhap_biChan() {
        DaoTestSupport.dangXuat();

        BusinessException ex = assertThrows(BusinessException.class, () -> dao.tongHop(NGAY, NGAY, null));

        assertEquals("Vui lòng đăng nhập.", ex.getMessage());
    }

    // ------------------------------------------------------------------ hỗ trợ

    private void lichNgay(LocalDate ngay, int soThuTu, String trangThai) throws SQLException {
        chenLich(ngay, 7 + soThuTu, 0, trangThai, NHA_SI);
    }

    private void chenLich(LocalDate ngay, int gio, int phut, String trangThai, String maNV) throws SQLException {
        LocalDateTime luc = ngay.atTime(gio, phut);
        String ma = "LH" + ngay.format(YYMMDD) + String.format("%03d", DEM.getAndIncrement() % 300 + 700);
        String lyDo = "DA_HUY".equals(trangThai) ? "Hủy kiểm thử" : null;
        try (Connection c = ketNoi()) {
            // trạng thái DA_DAT/DA_DEN bị trigger chặn trùng giờ với cùng nha sĩ nên mỗi lịch một giờ khác nhau
            JdbcHelper.update(c, "INSERT INTO LichHen(maLH,maKH,maNV,maDV,ngayGioHen,trangThai,lyDoHuy)"
                    + " VALUES(?,?,?,'DV996',?,?,?)", ma, maKH, maNV, luc, trangThai, lyDo);
        }
    }

    private static Connection ketNoi() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }
}
