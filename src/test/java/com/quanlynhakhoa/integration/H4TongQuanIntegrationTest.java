package com.quanlynhakhoa.integration;

import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.dao.*;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;

import com.quanlynhakhoa.util.*;
import com.quanlynhakhoa.ui.dialog.DlgVoucher;
import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.atomic.AtomicReference;

import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in: database riêng và JVM riêng, không khởi tạo pool của DB ứng dụng trong JVM test. */
@EnabledIfSystemProperty(named = "bclinic.integration", matches = "true")
class H4TongQuanIntegrationTest {
    @Test
    void uc035_theoNgay() throws Exception {
        Properties props = new Properties();
        try (var in = Files.newInputStream(Path.of("src/main/resources/database.properties"))) { props.load(in); }
        String name = "BClinic_Test_" + UUID.randomUUID().toString().replace("-", "");
        if (!name.matches("BClinic_Test_[a-f0-9]{32}")) { throw new IllegalStateException("Tên DB test không hợp lệ."); }
        String base = "jdbc:sqlserver://" + props.getProperty("db.host", "localhost") + ":"
                + props.getProperty("db.port", "1433") + ";encrypt=true;trustServerCertificate=true;loginTimeout=10;socketTimeout=20000;"
                + (Boolean.parseBoolean(props.getProperty("db.integratedSecurity", "false"))
                        ? "integratedSecurity=true;" : "") + "databaseName=";
        Path directory = Files.createTempDirectory(Path.of("target"), "h4-dashboard-");
        Path config = directory.resolve("database.properties");
        Path output = directory.resolve("result.log");
        Process child = null;
        boolean created = false;
        try (Connection master = DriverManager.getConnection(base + "master",
                props.getProperty("db.username"), props.getProperty("db.password"))) {
            try {
                try (var s = master.createStatement()) { s.setQueryTimeout(20); s.execute("CREATE DATABASE [" + name + "]"); created = true; }
                Flyway.configure().dataSource(base + name, props.getProperty("db.username"),
                        props.getProperty("db.password")).locations("classpath:db/migration").load().migrate();
                props.setProperty("db.name", name);
                try (var out = Files.newOutputStream(config)) { props.store(out, "Isolated H4_DASHBOARD verification"); }
                String cp = directory.toAbsolutePath() + java.io.File.pathSeparator
                        + System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
                String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
                child = new ProcessBuilder(java, "-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-Dsun.stderr.encoding=UTF-8", "-cp", cp,
                        Scenario.class.getName(), name).redirectErrorStream(true).redirectOutput(output.toFile()).start();
                assertTrue(child.waitFor(90, TimeUnit.SECONDS), "H4_DASHBOARD integration quá 90 giây.");
                String result = Files.readString(output);
                System.out.println(result);
                assertEquals(0, child.exitValue(), result);
                assertTrue(result.contains("H4_DASHBOARD_INTEGRATION_OK"), result);
            } finally {
                if (child != null && child.isAlive()) {
                    child.destroyForcibly();
                    if (!child.waitFor(10, TimeUnit.SECONDS)) { throw new IllegalStateException("JVM test chưa dừng."); }
                }
                if (created) {
                    try (var s = master.createStatement()) {
                        s.setQueryTimeout(20);
                        s.execute("ALTER DATABASE [" + name + "] SET SINGLE_USER WITH ROLLBACK IMMEDIATE");
                        s.execute("DROP DATABASE [" + name + "]");
                    }
                    System.out.println("TEMP_TEST_DB_REMOVED");
                }
            }
        } finally {
            Files.deleteIfExists(config);
            // result.log không chứa mật khẩu; giữ lại để xem bằng chứng khi test lỗi.
        }
    }

    public static class Scenario {
        private static final LocalDate DAY = LocalDate.of(2026, 11, 5);
        private static final TongQuanQuanLyDAO DAO = new TongQuanQuanLyDAO(
                Clock.fixed(DAY.atTime(10, 0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(), ZoneOffset.UTC));
        public static void main(String[] args) throws Exception {
            try (Connection c = DatabaseConnection.getInstance().getConnection()) {
                try (var p = c.prepareStatement("SELECT DB_NAME()"); var r = p.executeQuery()) {
                    assertTrue(r.next()); assertTrue(args[0].matches("BClinic_Test_[a-f0-9]{32}")); assertEquals(args[0], r.getString(1));
                }
                sql(c, "INSERT INTO KhachHang(maKH,tenKH,gioiTinh,sdt) VALUES('KH00001',N'Kiểm thử',1,'0900999001')");
                sql(c, "INSERT INTO HoSoBenhAn(maHSBA,maKH) VALUES('HS00001','KH00001')");
                sql(c, "INSERT INTO LoaiDichVu(maLoaiDV,tenLoaiDV,mucThueGTGT) VALUES('LDV01',N'Điều trị bệnh lý',0)");
                sql(c, "INSERT INTO DichVu(maDV,maLoaiDV,tenDV,donGia,thoiLuong) VALUES('DV701','LDV01',N'Khám',100000,15)");
                sql(c, "INSERT INTO Voucher(maVoucher,tenVoucher,loaiGiam,giaGiam,ngayBatDau,ngayKetThuc) VALUES('VC701',N'Giảm kiểm thử',0,10000,'20260101','20261231')");
                // Hóa đơn hôm qua trả hôm nay; nợ cũ; hóa đơn hôm nay chưa trả và trả ngày mai.
                bill(c, 1, DAY.minusDays(1).atTime(8,0), DAY.atTime(9,0), 500000, 0);
                bill(c, 2, DAY.minusDays(1).atTime(10,0), null, 700000, 0);
                bill(c, 3, DAY.atStartOfDay(), null, 300000, 5);
                bill(c, 4, DAY.atTime(8,0), DAY.plusDays(1).atStartOfDay(), 200000, 10);
                bill(c, 5, DAY.plusDays(1).atStartOfDay(), null, 900000, 0);
                sql(c, "INSERT INTO LichLamViec(maLLV,maNV,ngayTruc,caTruc,gioBatDau,gioKetThuc) VALUES('LLV00701','NV002',?,'CA_SANG','08:00','12:00')", DAY.minusDays(1));
                var statuses = com.quanlynhakhoa.entity.enums.TrangThaiLichHen.values();
                for (int i = 0; i < statuses.length; i++) {
                    sql(c, "INSERT INTO LichHen(maLH,maKH,maNV,maDV,ngayGioHen,trangThai,lyDoHuy) VALUES(?,'KH00001','NV002','DV701',?,?,?)",
                            "LH261105" + String.format(Locale.ROOT, "%03d", i + 1), DAY.atTime(8 + i, 0),
                            statuses[i].name(), statuses[i].name().equals("DA_HUY") ? "Kiểm thử" : null);
                }
                login(VaiTro.QUANLY);
                var current = DAO.layTongQuanQuanLy(DAY);
                assertEquals(DAY, current.ngayXem());
                assertEquals(5, current.tongLichHen());
                assertEquals(1, current.daDat()); assertEquals(1, current.daDen());
                assertEquals(1, current.hoanTat()); assertEquals(1, current.daHuy()); assertEquals(1, current.khongDen());
                assertEquals(current.lichHen(), DAO.layTongQuanQuanLy(DAY).lichHen(), "Tải không đổi trạng thái lịch");
                assertEquals(1, current.hoaDonDaTra()); assertEquals(1, current.hoaDonChuaTra());
                money("210000", current.daThu()); money("305000", current.conPhaiThu());
                money("515000", current.doanhThuDuKien());
                money("500000", current.bieuDo().get(6).soTien());
                assertEquals(7, current.bieuDo().size());
                var previous = DAO.layTongQuanQuanLy(DAY.minusDays(1));
                money("1200000", previous.doanhThuDuKien());
                assertTrue(previous.nhaSi().stream().anyMatch(n -> n.trongCa()));
                assertTrue(previous.nhaSi().stream().allMatch(n -> n.benhNhanDangKham() == null));
                var tomorrow = DAO.layTongQuanQuanLy(DAY.plusDays(1));
                money("900000", tomorrow.doanhThuDuKien());
                money("210000", tomorrow.bieuDo().get(6).soTien());
                var empty = DAO.layTongQuanQuanLy(DAY.minusDays(30));
                money("0", empty.doanhThuDuKien()); assertEquals(0, empty.tongLichHen());
                money("210000", DAO.tongQuanTrongNgay(DAY).daThu());
                for (VaiTro role : List.of(VaiTro.LETAN, VaiTro.NHASI)) {
                    login(role); assertThrows(BusinessException.class, () -> DAO.layTongQuanQuanLy(DAY));
                }
                SessionManager.getInstance().logout();
                assertThrows(BusinessException.class, () -> DAO.layTongQuanQuanLy(DAY));
                System.out.println("H4_DASHBOARD_INTEGRATION_OK");
            } finally {
                SessionManager.getInstance().logout(); DatabaseConnection.getInstance().getDataSource().close();
            }
        }
        private static void bill(Connection c, int n, LocalDateTime created, LocalDateTime paid, int price, int tax) throws Exception {
            String pk = "PK261105" + String.format(Locale.ROOT, "%03d", n);
            String hd = "HD261105" + String.format(Locale.ROOT, "%03d", n);
            sql(c, "INSERT INTO PhieuKham(maPK,maHSBA,maNV_LapPhieu,ngayLap) VALUES(?,'HS00001','NV003',?)", pk, created);
            sql(c, "INSERT INTO ChiTietPhieuKham(maPK,maDV,maNV_NhaSi,trieuChung,chanDoan,donViTinh,soLuong,donGia,mucThueGTGT) VALUES(?,'DV701','NV002',N'Khám',N'Khám',N'Lần',1,?,?)", pk, price, tax);
            sql(c, "INSERT INTO HoaDon(maHD,maPK,ngayLap,trangThai,ngayThanhToan,hinhThucThanhToan,maNV_ThuNgan,maVoucher,tongKhuyenMai) VALUES(?,?,?,?,?,?,?,?,?)", hd, pk, created, paid != null, paid, paid == null ? null : "Tiền mặt", paid == null ? null : "NV003", tax > 0 ? "VC701" : null, tax > 0 ? 10000 : 0);
        }
        private static void login(VaiTro role) {
            SessionManager.getInstance().login(new TaiKhoanDAO().dangNhap("hieu." + role.name().toLowerCase(Locale.ROOT), "Clinic@123"));
        }
        private static void money(String expected, java.math.BigDecimal actual) { assertEquals(0, new java.math.BigDecimal(expected).compareTo(actual)); }
        private static void sql(Connection c, String query, Object... args) throws Exception {
            try (var ps = c.prepareStatement(query)) { com.quanlynhakhoa.dao.support.JdbcHelper.bind(ps, args); ps.executeUpdate(); }
        }
    }
}
