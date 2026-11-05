package com.quanlynhakhoa.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ThongKeBenhNhanDAOTest {

    private final ThongKeBenhNhanDAO dao = new ThongKeBenhNhanDAO();
    private final LocalDate homNay = LocalDate.now();
    private final LocalDate homQua = homNay.minusDays(1);
    private String maKHA;
    private String maKHB;
    private String maKHC;
    private String maLHA1;
    private String maLHA2;
    private String maLHB;
    private String maLHC;
    private String maPKA1;
    private String maPKA2;
    private String maPKB;
    private String maPKC;

    @BeforeAll
    static void chuanBi() {
        DaoTestSupport.yeuCauDatabaseTest();
    }

    @BeforeEach
    void taoDuLieu() throws SQLException {
        int so = ThreadLocalRandom.current().nextInt(100_000, 1_000_000);
        maKHA = "KH" + String.format("%05d", so % 100_000);
        maKHB = "KH" + String.format("%05d", (so + 1) % 100_000);
        maKHC = "KH" + String.format("%05d", (so + 2) % 100_000);
        String tienToLH = "LH" + LocalDate.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyMMdd"));
        maLHA1 = tienToLH + String.format("%03d", so % 1000);
        maLHA2 = tienToLH + String.format("%03d", (so + 1) % 1000);
        maLHB = tienToLH + String.format("%03d", (so + 2) % 1000);
        maLHC = tienToLH + String.format("%03d", (so + 3) % 1000);
        maPKA1 = "PK" + String.format("%09d", so);
        maPKA2 = "PK" + String.format("%09d", so + 1);
        maPKB = "PK" + String.format("%09d", so + 2);
        maPKC = "PK" + String.format("%09d", so + 3);

        try (Connection c = DatabaseConnection.getInstance().getConnection()) {
            JdbcHelper.update(c, "IF NOT EXISTS(SELECT 1 FROM LoaiDichVu WHERE maLoaiDV='LDV99')"
                    + " INSERT INTO LoaiDichVu(maLoaiDV,tenLoaiDV,mucThueGTGT)"
                    + " VALUES('LDV99',N'Điều trị bệnh lý',0.00)");
            JdbcHelper.update(c, "IF NOT EXISTS(SELECT 1 FROM DichVu WHERE maDV='DV999')"
                    + " INSERT INTO DichVu(maDV,maLoaiDV,tenDV,donGia,thoiLuong,trangThai)"
                    + " VALUES('DV999','LDV99',N'Dịch vụ kiểm thử UC033',100000,30,1)");
            taoKhach(c, maKHA, "A");
            taoKhach(c, maKHB, "B");
            taoKhach(c, maKHC, "C");

            taoLich(c, maLHA1, maKHA, "NV002", homQua.atTime(9, 0));
            taoLich(c, maLHA2, maKHA, "NV002", homNay.atTime(8, 0));
            taoLich(c, maLHB, maKHB, "NV002", homNay.atTime(10, 0));
            taoLich(c, maLHC, maKHC, "NV001", homNay.atTime(11, 0));

            taoPhieu(c, maPKA1, maKHA, maLHA1, homQua.atTime(9, 0), "NV002", 1);
            taoPhieu(c, maPKA2, maKHA, maLHA2, homNay.atTime(8, 0), "NV002", 2);
            taoPhieu(c, maPKB, maKHB, maLHB, homNay.atTime(10, 0), "NV002", 1);
            taoPhieu(c, maPKC, maKHC, maLHC, homNay.atTime(11, 0), "NV001", 1);
        }
    }

    private static void taoKhach(Connection c, String maKH, String ten) throws SQLException {
        JdbcHelper.update(c, "INSERT INTO KhachHang(maKH,tenKH,gioiTinh,ngaySinh,sdt,email,diaChi)"
                + " VALUES(?,?,0,'1990-01-01',?,?,N'Test')", maKH, "Khách " + ten,
                "0912" + maKH.substring(2), maKH.toLowerCase() + "@example.com");
        JdbcHelper.update(c, "INSERT INTO HoSoBenhAn(maHSBA,maKH) VALUES(?,?)", "HS" + maKH.substring(2), maKH);
    }

    private static void taoLich(Connection c, String maLH, String maKH, String maNV,
            LocalDateTime ngay) throws SQLException {
        JdbcHelper.update(c, "INSERT INTO LichHen(maLH,maKH,maNV,maDV,ngayGioHen,trangThai)"
                + " VALUES(?,?,?,'DV999',?,'DA_DEN')", maLH, maKH, maNV, ngay);
    }

    private static void taoPhieu(Connection c, String maPK, String maKH, String maLH,
            LocalDateTime ngay, String maNVNhaSi, int soLuong) throws SQLException {
        JdbcHelper.update(c, "INSERT INTO PhieuKham(maPK,maHSBA,maLH,maNV_LapPhieu,ngayLap,trangThai)"
                + " VALUES(?,?,?,'NV003',?,'DANG_KHAM')", maPK, "HS" + maKH.substring(2), maLH, ngay);
        JdbcHelper.update(c, "INSERT INTO ChiTietPhieuKham(maPK,maDV,maNV_NhaSi,trieuChung,chanDoan,"
                + "ketQuaDieuTri,donViTinh,soLuong,donGia,mucThueGTGT)"
                + " VALUES(?,'DV999',?,N'Đau răng',N'Sâu răng',N'Đã xử lý',N'Lần',?,100000,0)",
                maPK, maNVNhaSi, soLuong);
        JdbcHelper.update(c, "UPDATE PhieuKham SET trangThai='HOAN_TAT' WHERE maPK=?", maPK);
    }

    @AfterEach
    void don() throws SQLException {
        try (Connection c = DatabaseConnection.getInstance().getConnection()) {
            JdbcHelper.update(c, "DISABLE TRIGGER trg_KhoaChiTietPhieuKham ON ChiTietPhieuKham");
            JdbcHelper.update(c, "DISABLE TRIGGER trg_KhoaDuLieuPhieuKhamHoanTat ON PhieuKham");
            JdbcHelper.update(c, "DELETE FROM PhieuKham WHERE maPK IN (?,?,?,?)",
                    maPKA1, maPKA2, maPKB, maPKC);
            JdbcHelper.update(c, "ENABLE TRIGGER trg_KhoaDuLieuPhieuKhamHoanTat ON PhieuKham");
            JdbcHelper.update(c, "ENABLE TRIGGER trg_KhoaChiTietPhieuKham ON ChiTietPhieuKham");
            JdbcHelper.update(c, "DELETE FROM LichHen WHERE maKH IN (?,?,?)", maKHA, maKHB, maKHC);
            JdbcHelper.update(c, "DELETE FROM HoSoBenhAn WHERE maKH IN (?,?,?)", maKHA, maKHB, maKHC);
            JdbcHelper.update(c, "DELETE FROM KhachHang WHERE maKH IN (?,?,?)", maKHA, maKHB, maKHC);
            JdbcHelper.update(c, "DELETE FROM DichVu WHERE maDV='DV999'");
            JdbcHelper.update(c, "DELETE FROM LoaiDichVu WHERE maLoaiDV='LDV99'");
        }
        DaoTestSupport.dangXuat();
    }

    @Test
    void nhaSiChiThayBenhNhanCuaMinh() {
        DaoTestSupport.dangNhap(VaiTro.NHASI);

        List<ThongKeBenhNhanDAO.TheoNgay> ds = dao.thongKe(homQua, homNay);

        assertEquals(2, ds.size());
        assertEquals(1, ds.get(0).soBenhNhan());
        assertEquals(2, ds.get(1).soBenhNhan());
        assertEquals(2, dao.demBenhNhanKhacNhau(homQua, homNay));
    }

    @Test
    void quanLyThayTatCaBenhNhan() {
        DaoTestSupport.dangNhap(VaiTro.QUANLY);

        assertEquals(3, dao.demBenhNhanKhacNhau(homNay, homNay));
    }

    @Test
    void khongCoDuLieu_traVeRong() {
        DaoTestSupport.dangNhap(VaiTro.QUANLY);

        assertTrue(dao.thongKe(homNay.plusDays(5), homNay.plusDays(6)).isEmpty());
        assertEquals(0, dao.demBenhNhanKhacNhau(homNay.plusDays(5), homNay.plusDays(6)));
    }

    @Test
    void dichVuSuDung_sapXepGiamVaLocTheoNhaSi() {
        DaoTestSupport.dangNhap(VaiTro.NHASI);

        List<ThongKeBenhNhanDAO.DichVuSuDung> ds = dao.dichVuSuDung(homQua, homNay);

        assertEquals(1, ds.size());
        assertEquals(4, ds.get(0).soLuong());

        DaoTestSupport.dangNhap(VaiTro.QUANLY);
        assertEquals(5, dao.dichVuSuDung(homQua, homNay).get(0).soLuong());
    }

    @Test
    void danhSachBenhNhan_locQuyenVaCoChanDoan() {
        DaoTestSupport.dangNhap(VaiTro.NHASI);

        List<ThongKeBenhNhanDAO.BenhNhanTrongKy> ds = dao.danhSachBenhNhan(homQua, homNay);

        assertEquals(2, ds.size());
        assertTrue(ds.stream().allMatch(bn -> bn.chanDoan() != null));

        DaoTestSupport.dangNhap(VaiTro.QUANLY);
        assertEquals(3, dao.danhSachBenhNhan(homQua, homNay).size());
    }

    @Test
    void khoangNgaySai_nemLoi() {
        DaoTestSupport.dangNhap(VaiTro.QUANLY);

        assertThrows(BusinessException.class, () -> dao.thongKe(homNay, homQua));
    }
}
