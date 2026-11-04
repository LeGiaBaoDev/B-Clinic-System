package com.quanlynhakhoa.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.entity.PhieuKham;
import com.quanlynhakhoa.entity.enums.TrangThaiPhieuKham;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/* T803 – UC021: tiếp nhận lịch hẹn, tạo phiếu khám và chuyển lịch DA_DEN. */
class PhieuKhamDAOTest {

    private final PhieuKhamDAO dao = new PhieuKhamDAO();
    private String maKH;
    private String maLH;
    private String maLLV;

    @BeforeAll
    static void chuanBi() {
        DaoTestSupport.yeuCauDatabaseTest();
    }

    @BeforeEach
    void taoDuLieu() throws SQLException {
        DaoTestSupport.dangNhap(VaiTro.LETAN);
        String suffix = String.format("%05d", ThreadLocalRandom.current().nextInt(10_000, 100_000));
        maKH = "KH" + suffix;
        maLLV = "LLV" + suffix;
        maLH = "LH" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1, 1000));
        try (Connection c = DatabaseConnection.getInstance().getConnection()) {
            JdbcHelper.update(c, "IF NOT EXISTS(SELECT 1 FROM LoaiDichVu WHERE maLoaiDV='LDV99')"
                    + " INSERT INTO LoaiDichVu(maLoaiDV,tenLoaiDV,mucThueGTGT)"
                    + " VALUES('LDV99',N'Điều trị bệnh lý',0.00)");
            JdbcHelper.update(c, "IF NOT EXISTS(SELECT 1 FROM DichVu WHERE maDV='DV999')"
                    + " INSERT INTO DichVu(maDV,maLoaiDV,tenDV,donGia,thoiLuong,trangThai)"
                    + " VALUES('DV999','LDV99',N'Dịch vụ kiểm thử UC021',100000,30,1)");
            JdbcHelper.update(c, "INSERT INTO KhachHang(maKH,tenKH,gioiTinh,ngaySinh,sdt,email,diaChi)"
                    + " VALUES(?,N'Khách kiểm thử UC021',0,'1990-01-01','0912345678',?,N'Test')",
                    maKH, maKH.toLowerCase() + "@example.com");
            JdbcHelper.update(c, "INSERT INTO HoSoBenhAn(maHSBA,maKH) VALUES(?,?)",
                    "HS" + suffix, maKH);
            JdbcHelper.update(c, "INSERT INTO LichLamViec(maLLV,maNV,ngayTruc,caTruc,gioBatDau,"
                    + "gioKetThuc,trangThai) VALUES(?,'NV002',CAST(SYSDATETIME() AS DATE),"
                    + "'CA_NGAY','07:00','17:30','DA_PHAN_CONG')", maLLV);
            JdbcHelper.update(c, "INSERT INTO LichHen(maLH,maKH,maNV,maDV,ngayGioHen,trangThai)"
                    + " VALUES(?,?,'NV002','DV999',DATEADD(HOUR,10,CAST(CAST(SYSDATETIME() AS DATE) AS DATETIME2)),'DA_DAT')",
                    maLH, maKH);
        }
    }

    @AfterEach
    void don() throws SQLException {
        try (Connection c = DatabaseConnection.getInstance().getConnection()) {
            JdbcHelper.update(c, "DELETE FROM PhieuKham WHERE maLH=?", maLH);
            JdbcHelper.update(c, "DELETE FROM LichHen WHERE maKH=?", maKH);
            JdbcHelper.update(c, "DELETE FROM LichLamViec WHERE maLLV=?", maLLV);
            JdbcHelper.update(c, "DELETE FROM HoSoBenhAn WHERE maKH=?", maKH);
            JdbcHelper.update(c, "DELETE FROM KhachHang WHERE maKH=?", maKH);
            JdbcHelper.update(c, "DELETE FROM DichVu WHERE maDV='DV999'");
            JdbcHelper.update(c, "DELETE FROM LoaiDichVu WHERE maLoaiDV='LDV99'");
        }
        DaoTestSupport.dangXuat();
    }

    @Test
    void tiepNhan_taoPhieuVaChuyenLichDaDen() throws SQLException {
        PhieuKham pk = dao.taoTuLichHen(maLH);

        assertNotNull(pk);
        assertEquals(TrangThaiPhieuKham.CHO_KHAM, pk.getTrangThai());
        assertTrue(pk.getMaPK().matches("PK\\d{9}"));
        assertEquals(1, dem("SELECT COUNT(*) FROM PhieuKham WHERE maLH=?", maLH));
        assertEquals("DA_DEN", chuoi("SELECT trangThai FROM LichHen WHERE maLH=?", maLH));
    }

    @Test
    void tiepNhan_lanHai_nemLoiVaKhongTaoTrung() throws SQLException {
        dao.taoTuLichHen(maLH);

        assertThrows(BusinessException.class, () -> dao.taoTuLichHen(maLH));
        assertEquals(1, dem("SELECT COUNT(*) FROM PhieuKham WHERE maLH=?", maLH));
    }

    @Test
    void tiepNhan_lichNgayKhac_nemLoi() throws SQLException {
        String maKhac = maLH.substring(0, maLH.length() - 1) + "8";
        try (Connection c = DatabaseConnection.getInstance().getConnection()) {
            JdbcHelper.update(c, "DELETE FROM LichHen WHERE maLH=?", maKhac);
            JdbcHelper.update(c, "INSERT INTO LichHen(maLH,maKH,maNV,maDV,ngayGioHen,trangThai)"
                    + " VALUES(?,?,'NV002','DV999',DATEADD(DAY,1,CAST(SYSDATETIME() AS DATE)),'DA_DAT')",
                    maKhac, maKH);
        }
        try {
            assertThrows(BusinessException.class, () -> dao.taoTuLichHen(maKhac));
            assertEquals(0, dem("SELECT COUNT(*) FROM PhieuKham WHERE maLH=?", maKhac));
        } finally {
            try (Connection c = DatabaseConnection.getInstance().getConnection()) {
                JdbcHelper.update(c, "DELETE FROM LichHen WHERE maLH=?", maKhac);
            }
        }
    }

    @Test
    void tiepNhan_lichDaHuy_nemLoi() throws SQLException {
        String maHuy = maLH.substring(0, maLH.length() - 1) + "9";
        try (Connection c = DatabaseConnection.getInstance().getConnection()) {
            JdbcHelper.update(c, "DELETE FROM LichHen WHERE maLH=?", maHuy);
            JdbcHelper.update(c, "INSERT INTO LichHen(maLH,maKH,maNV,maDV,ngayGioHen,trangThai,lyDoHuy)"
                    + " VALUES(?,?,'NV002','DV999',DATEADD(HOUR,11,CAST(CAST(SYSDATETIME() AS DATE) AS DATETIME2)),"
                    + "'DA_HUY',N'Test')", maHuy, maKH);
        }
        try {
            assertThrows(BusinessException.class, () -> dao.taoTuLichHen(maHuy));
            assertEquals(0, dem("SELECT COUNT(*) FROM PhieuKham WHERE maLH=?", maHuy));
        } finally {
            try (Connection c = DatabaseConnection.getInstance().getConnection()) {
                JdbcHelper.update(c, "DELETE FROM LichHen WHERE maLH=?", maHuy);
            }
        }
    }

    @Test
    void nhaSiKhongDuocTiepNhan() {
        DaoTestSupport.dangNhap(VaiTro.NHASI);

        assertThrows(BusinessException.class, () -> dao.taoTuLichHen(maLH));
    }

    private static int dem(String sql, String ma) throws SQLException {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, ma);
            try (var r = ps.executeQuery()) {
                r.next();
                return r.getInt(1);
            }
        }
    }

    private static String chuoi(String sql, String ma) throws SQLException {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, ma);
            try (var r = ps.executeQuery()) {
                return r.next() ? r.getString(1) : null;
            }
        }
    }
}
