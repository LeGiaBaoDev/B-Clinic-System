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
class H3VoucherIntegrationTest {
    @Test
    void t501_voucher() throws Exception {
        Properties props = new Properties();
        try (var in = Files.newInputStream(Path.of("src/main/resources/database.properties"))) { props.load(in); }
        String name = "BClinic_Test_" + UUID.randomUUID().toString().replace("-", "");
        if (!name.matches("BClinic_Test_[a-f0-9]{32}")) { throw new IllegalStateException("Tên DB test không hợp lệ."); }
        String base = "jdbc:sqlserver://" + props.getProperty("db.host", "localhost") + ":"
                + props.getProperty("db.port", "1433") + ";encrypt=true;trustServerCertificate=true;"
                + (Boolean.parseBoolean(props.getProperty("db.integratedSecurity", "false"))
                        ? "integratedSecurity=true;" : "") + "databaseName=";
        Path directory = Files.createTempDirectory(Path.of("target"), "h3-voucher-");
        Path config = directory.resolve("database.properties");
        Path output = directory.resolve("result.log");
        Process child = null;
        boolean created = false;
        try (Connection master = DriverManager.getConnection(base + "master",
                props.getProperty("db.username"), props.getProperty("db.password"))) {
            try {
                try (var s = master.createStatement()) { s.execute("CREATE DATABASE [" + name + "]"); created = true; }
                Flyway.configure().dataSource(base + name, props.getProperty("db.username"),
                        props.getProperty("db.password")).locations("classpath:db/migration").load().migrate();
                props.setProperty("db.name", name);
                try (var out = Files.newOutputStream(config)) { props.store(out, "Isolated H3_VOUCHER verification"); }
                String cp = directory.toAbsolutePath() + java.io.File.pathSeparator
                        + System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
                String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
                child = new ProcessBuilder(java, "-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-Dsun.stderr.encoding=UTF-8", "-cp", cp,
                        Scenario.class.getName(), name).redirectErrorStream(true).redirectOutput(output.toFile()).start();
                assertTrue(child.waitFor(90, TimeUnit.SECONDS), "H3_VOUCHER integration quá 90 giây.");
                String result = Files.readString(output);
                System.out.println(result);
                assertEquals(0, child.exitValue(), result);
                assertTrue(result.contains("H3_VOUCHER_INTEGRATION_OK"), result);
            } finally {
                if (child != null && child.isAlive()) {
                    child.destroyForcibly();
                    if (!child.waitFor(10, TimeUnit.SECONDS)) { throw new IllegalStateException("JVM test chưa dừng."); }
                }
                if (created) {
                    try (var s = master.createStatement()) {
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
        private static final VoucherDAO DAO = new VoucherDAO();
        private static final LocalDate DAY = LocalDate.of(2026, 11, 5);

        public static void main(String[] args) throws Exception {
            try (Connection c = DatabaseConnection.getInstance().getConnection()) {
                assertTrue(args[0].matches("BClinic_Test_[a-f0-9]{32}"));
                assertEquals(args[0], scalar(c, "SELECT DB_NAME()"));
                System.out.println("ISOLATED_DATABASE_CONFIRMED");
                login(VaiTro.QUANLY);
                crud(c);
                permissions(c);
                history(c);
                rollback(c);
                concurrentCreation();
                uiLuuVoucher();
                H3ThanhToanScenario.run(c);
                System.out.println("H3_VOUCHER_INTEGRATION_OK");
            } finally {
                SessionManager.getInstance().logout();
                DatabaseConnection.getInstance().getDataSource().close();
            }
        }

        private static void crud(Connection c) throws Exception {
            assertEquals("VC001", DAO.layMaDuKien());
            assertEquals("VC001", DAO.layMaDuKien(), "Xem mã không giữ chỗ hoặc tạo voucher");
            Voucher a = voucher();
            assertTrue(DAO.them(a));
            assertEquals("VC001", a.getMaVoucher());
            assertEquals("VC002", DAO.layMaDuKien());
            assertTrue(a.getMaVoucher().matches("VC\\d{3}"));
            assertEquals("NULL", scalar(c, "SELECT COALESCE(CONVERT(varchar,giaTriGiamToiDa),'NULL') FROM Voucher WHERE maVoucher='" + a.getMaVoucher() + "'"));
            assertEquals(0, DAO.layTheoMa(a.getMaVoucher()).getGiaTriGiamToiDa());
            a.setGiaTriGiamToiDa(25000);
            a.setTenVoucher("Ưu đãi cập nhật");
            assertTrue(DAO.capNhat(a));
            assertEquals(25000, DAO.layTheoMa(a.getMaVoucher()).getGiaTriGiamToiDa());
            assertEquals("Ưu đãi cập nhật", DAO.layTheoMa(a.getMaVoucher()).getTenVoucher());
            assertTrue(DAO.layConHieuLuc(DAY).contains(a));
            assertTrue(DAO.layConHieuLuc(DAY.plusDays(10)).contains(a));
            assertFalse(DAO.layConHieuLuc(DAY.minusDays(1)).contains(a));
            assertFalse(DAO.layConHieuLuc(DAY.plusDays(11)).contains(a));
            assertTrue(DAO.xoa(a.getMaVoucher()));
            assertFalse(DAO.layTheoMa(a.getMaVoucher()).isTrangThai());
            assertFalse(DAO.layConHieuLuc(DAY).contains(a));
            assertTrue(DAO.layTatCa().contains(a));
            Voucher missing = voucher();
            missing.setMaVoucher("VC999");
            assertNull(DAO.layTheoMa("VC999"));
            assertFalse(DAO.capNhat(missing));
            assertFalse(DAO.xoa("VC999"));
            assertThrows(BusinessException.class, () -> DAO.layConHieuLuc(null));
            System.out.println("CRUD_MAPPING_VALIDITY_OK");
        }

        private static void permissions(Connection c) throws Exception {
            for (VaiTro role : new VaiTro[]{VaiTro.LETAN, VaiTro.NHASI}) {
                login(role);
                assertThrows(BusinessException.class, DAO::layTatCa);
                assertThrows(BusinessException.class, DAO::layMaDaSuDung);
                assertThrows(BusinessException.class, DAO::layMaDuKien);
                assertThrows(BusinessException.class, () -> DAO.them(voucher()));
                Voucher update = voucher(); update.setMaVoucher("VC001");
                assertThrows(BusinessException.class, () -> DAO.capNhat(update));
                assertThrows(BusinessException.class, () -> DAO.xoa("VC001"));
                if (role == VaiTro.LETAN) {
                    assertNotNull(DAO.layTheoMa("VC001"));
                    assertNotNull(DAO.layConHieuLuc(DAY));
                } else {
                    assertThrows(BusinessException.class, () -> DAO.layTheoMa("VC001"));
                    assertThrows(BusinessException.class, () -> DAO.layConHieuLuc(DAY));
                }
            }
            SessionManager.getInstance().getCurrentUser().setVaiTro(VaiTro.QUANLY);
            assertThrows(BusinessException.class, () -> DAO.them(voucher()));
            SessionManager.getInstance().logout();
            assertThrows(BusinessException.class, DAO::layTatCa);
            assertThrows(BusinessException.class, () -> DAO.them(voucher()));
            login(VaiTro.QUANLY);
            execute(c, "UPDATE TaiKhoan SET trangThai=0 WHERE maTK='TK001'");
            try { assertThrows(BusinessException.class, () -> DAO.them(voucher())); }
            finally { execute(c, "UPDATE TaiKhoan SET trangThai=1 WHERE maTK='TK001'"); }
            System.out.println("PERMISSIONS_OK");
        }

        private static void history(Connection c) throws Exception {
            Voucher used = voucher(); assertTrue(DAO.them(used));
            execute(c, "INSERT INTO KhachHang(maKH,tenKH,sdt,gioiTinh) VALUES('KH00001',N'Khách kiểm thử','0900000001',1)");
            execute(c, "INSERT INTO HoSoBenhAn(maHSBA,maKH) VALUES('HS00001','KH00001')");
            execute(c, "INSERT INTO PhieuKham(maPK,maHSBA,maNV_LapPhieu,trangThai) VALUES('PK261105001','HS00001','NV003','HOAN_TAT')");
            execute(c, "INSERT INTO HoaDon(maHD,maPK,maVoucher,tongKhuyenMai) VALUES('HD261105001','PK261105001','" + used.getMaVoucher() + "',10000)");
            used.setTenVoucher("Không được đổi");
            assertThrows(BusinessException.class, () -> DAO.capNhat(used));
            assertEquals("Ưu đãi kiểm thử", DAO.layTheoMa(used.getMaVoucher()).getTenVoucher());
            assertTrue(DAO.xoa(used.getMaVoucher()));
            assertTrue(DAO.layMaDaSuDung().contains(used.getMaVoucher()));
            assertFalse(DAO.layMaDaSuDung().contains("VC001"));
            assertEquals(used.getMaVoucher(), scalar(c, "SELECT maVoucher FROM HoaDon WHERE maHD='HD261105001'"));
            assertEquals("10000.00", scalar(c, "SELECT tongKhuyenMai FROM HoaDon WHERE maHD='HD261105001'"));
            assertFalse(DAO.layConHieuLuc(DAY).contains(used));
            System.out.println("USED_VOUCHER_HISTORY_OK");
        }

        private static void rollback(Connection c) throws Exception {
            String before = scalar(c, "SELECT COUNT(*) FROM Voucher");
            Voucher failed = voucher();
            execute(c, "CREATE TRIGGER trg_TestVoucherLog ON NhatKyHeThong AFTER INSERT AS BEGIN THROW 51000, 'Forced audit failure', 1; END");
            try {
                assertThrows(DataAccessException.class, () -> DAO.them(failed));
                assertEquals(before, scalar(c, "SELECT COUNT(*) FROM Voucher"));
                assertNull(failed.getMaVoucher(), "Không gán mã khi transaction bị rollback");
                Voucher existing = DAO.layTheoMa("VC001");
                existing.setTenVoucher("Rollback cập nhật");
                assertThrows(DataAccessException.class, () -> DAO.capNhat(existing));
                assertEquals("Ưu đãi cập nhật", DAO.layTheoMa("VC001").getTenVoucher());
            } finally { execute(c, "DROP TRIGGER trg_TestVoucherLog"); }
            Voucher active = voucher(); DAO.them(active);
            execute(c, "CREATE TRIGGER trg_TestVoucherLog ON NhatKyHeThong AFTER INSERT AS BEGIN THROW 51000, 'Forced audit failure', 1; END");
            try {
                assertThrows(DataAccessException.class, () -> DAO.xoa(active.getMaVoucher()));
                assertTrue(DAO.layTheoMa(active.getMaVoucher()).isTrangThai());
            } finally { execute(c, "DROP TRIGGER trg_TestVoucherLog"); }
            assertTrue(DAO.them(failed));
            assertNotNull(failed.getMaVoucher());
            System.out.println("ROLLBACK_AND_OBJECT_STATE_OK");
        }

        private static void concurrentCreation() throws Exception {
            ExecutorService pool = Executors.newFixedThreadPool(4);
            CountDownLatch ready = new CountDownLatch(4), start = new CountDownLatch(1);
            try {
                List<Future<String>> jobs = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    jobs.add(pool.submit(() -> {
                        ready.countDown();
                        assertTrue(start.await(10, TimeUnit.SECONDS));
                        Voucher v = voucher(); assertTrue(DAO.them(v));
                        return v.getMaVoucher();
                    }));
                }
                assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
                Set<String> ids = new HashSet<>();
                for (Future<String> job : jobs) { ids.add(job.get(30, TimeUnit.SECONDS)); }
                assertEquals(4, ids.size());
                for (String id : ids) { assertNotNull(DAO.layTheoMa(id)); }
            } finally {
                start.countDown(); pool.shutdownNow();
                assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
            }
            System.out.println("CONCURRENT_CODES_OK");
        }

        private static Voucher voucher() {
            return new Voucher(null, "Ưu đãi kiểm thử", true, 10, 0, DAY, DAY.plusDays(10), true);
        }

        private static void uiLuuVoucher() throws Exception {
            AtomicReference<Voucher> saved = new AtomicReference<>();
            AtomicReference<DlgVoucher> dialog = new AtomicReference<>();
            try {
                SwingUtilities.invokeAndWait(() -> {
                    DlgVoucher form = new DlgVoucher(null, null, false, DAO::them, saved::set);
                    dialog.set(form);
                    ((JTextField) find(form.getContentPane(), "tenVoucher")).setText("Ưu đãi từ giao diện");
                    ((JTextField) find(form.getContentPane(), "giaGiamVoucher")).setText("50000");
                    ((JButton) find(form.getContentPane(), "luuVoucher")).doClick();
                });
                waitSaved(saved);
                Voucher created = DAO.layTheoMa(saved.get().getMaVoucher());
                assertEquals("Ưu đãi từ giao diện", created.getTenVoucher());
                assertEquals(50000, created.getGiaGiam());
                assertFalse(created.isLoaiGiam());
                SwingUtilities.invokeAndWait(() -> dialog.get().dispose());
                saved.set(null);
                SwingUtilities.invokeAndWait(() -> {
                    DlgVoucher form = new DlgVoucher(null, created, false, DAO::capNhat, saved::set);
                    dialog.set(form);
                    ((JTextField) find(form.getContentPane(), "tenVoucher")).setText("Cập nhật từ giao diện");
                    ((JButton) find(form.getContentPane(), "luuVoucher")).doClick();
                });
                waitSaved(saved);
                assertEquals(created.getMaVoucher(), saved.get().getMaVoucher());
                assertEquals("Cập nhật từ giao diện", DAO.layTheoMa(created.getMaVoucher()).getTenVoucher());
                System.out.println("UI_CREATE_UPDATE_SQL_OK");
            } finally {
                SwingUtilities.invokeAndWait(() -> { if (dialog.get() != null) dialog.get().dispose(); });
            }
        }

        private static void waitSaved(AtomicReference<Voucher> saved) throws InterruptedException {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
            while (saved.get() == null && System.nanoTime() < deadline) Thread.sleep(25);
            assertNotNull(saved.get(), "Form chưa lưu voucher thành công");
        }

        private static Component find(Container root, String name) {
            for (Component child : root.getComponents()) {
                if (name.equals(child.getName())) return child;
                if (child instanceof Container container) {
                    Component match = find(container, name);
                    if (match != null) return match;
                }
            }
            return null;
        }
        private static void login(VaiTro role) {
            SessionManager.getInstance().login(new TaiKhoanDAO().dangNhap("hieu." + role.name().toLowerCase(Locale.ROOT), "Clinic@123"));
        }
        private static void execute(Connection c, String sql) throws SQLException {
            try (var statement = c.prepareStatement(sql)) { statement.execute(); }
        }
        private static String scalar(Connection c, String sql) throws SQLException {
            try (var statement = c.prepareStatement(sql); var result = statement.executeQuery()) {
                assertTrue(result.next()); return result.getString(1);
            }
        }
    }
}
