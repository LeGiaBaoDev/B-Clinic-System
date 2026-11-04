package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.config.FlywayMigration;
import com.quanlynhakhoa.entity.NhanVien;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.util.SessionManager;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import org.junit.jupiter.api.Assumptions;

/**
 * Hỗ trợ test DAO cần SQL Server.
 *
 * <p>Test DAO ghi dữ liệu thật nên CHỈ chạy khi {@code database.properties} trên classpath trỏ tới
 * database có tên chứa "_Test" (vd {@code BClinic_Test}). Ngược lại, hoặc khi không kết nối được,
 * test tự bỏ qua để {@code mvn test} vẫn xanh và không bao giờ ghi vào database làm việc.</p>
 *
 * <p>Cách bật: tạo database trống {@code CREATE DATABASE BClinic_Test;} rồi đặt file
 * {@code src/test/resources/database.properties} (không commit) với {@code db.name=BClinic_Test}.
 * Flyway tự tạo bảng và dữ liệu mẫu (NV001–NV003, TK001–TK003) khi test chạy.</p>
 */
final class DaoTestSupport {

    static final String TK_QUAN_LY = "TK001";
    static final String NV_QUAN_LY = "NV001";
    static final String TK_NHA_SI = "TK002";
    static final String NV_NHA_SI = "NV002";
    static final String TK_LE_TAN = "TK003";
    static final String NV_LE_TAN = "NV003";

    private DaoTestSupport() {
    }

    /** Bỏ qua cả lớp test nếu không có database test; nếu có thì chạy migration. */
    static void yeuCauDatabaseTest() {
        Assumptions.assumeTrue(laDatabaseTest(),
                "Bỏ qua test DAO: database.properties không trỏ tới database có tên chứa '_Test'.");
        try {
            FlywayMigration.migrate();
            try (Connection c = DatabaseConnection.getInstance().getConnection()) {
                Assumptions.assumeTrue(c.isValid(5), "Không kết nối được database test.");
            }
        } catch (Throwable e) {
            Assumptions.assumeTrue(false, "Bỏ qua test DAO: không dùng được database test (" + e + ").");
        }
    }

    private static boolean laDatabaseTest() {
        try (InputStream is = DatabaseConnection.class.getClassLoader()
                .getResourceAsStream("database.properties")) {
            if (is == null) {
                return false;
            }
            Properties p = new Properties();
            p.load(is);
            return p.getProperty("db.name", "").matches("(?i).*_test.*");
        } catch (IOException e) {
            return false;
        }
    }

    static void dangNhap(VaiTro vaiTro) {
        String maTK;
        String maNV;
        switch (vaiTro) {
            case QUANLY -> {
                maTK = TK_QUAN_LY;
                maNV = NV_QUAN_LY;
            }
            case NHASI -> {
                maTK = TK_NHA_SI;
                maNV = NV_NHA_SI;
            }
            default -> {
                maTK = TK_LE_TAN;
                maNV = NV_LE_TAN;
            }
        }
        NhanVien nv = new NhanVien(maNV, "Nhân viên kiểm thử", true, "0398682230",
                "kiemthu@example.com", "Kiểm thử");
        SessionManager.getInstance().login(new TaiKhoan(maTK, "tk.kiemthu", "x", vaiTro, true, nv));
    }

    static void dangXuat() {
        SessionManager.getInstance().logout();
    }

    /** Xóa dữ liệu khách hàng do test tạo (hồ sơ trước, khách hàng sau). */
    static void xoaKhachHang(String maKH) {
        if (maKH == null) {
            return;
        }
        try (Connection c = DatabaseConnection.getInstance().getConnection()) {
            xoa(c, "DELETE FROM HoSoBenhAn WHERE maKH=?", maKH);
            xoa(c, "DELETE FROM KhachHang WHERE maKH=?", maKH);
        } catch (SQLException e) {
            throw new IllegalStateException("Không dọn được dữ liệu test của " + maKH, e);
        }
    }

    static int dem(String sql, String thamSo) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, thamSo);
            try (var r = ps.executeQuery()) {
                r.next();
                return r.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void xoa(Connection c, String sql, String tham) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tham);
            ps.executeUpdate();
        }
    }
}
