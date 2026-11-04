package com.quanlynhakhoa.integration;

import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.dao.*;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.entity.enums.CaTruc;
import com.quanlynhakhoa.entity.enums.TrangThaiLichLamViec;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;

import com.quanlynhakhoa.util.*;

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
class H2DaoIntegrationTest {
    @Test
    void t601_lichLamViec() throws Exception {
        Properties props = new Properties();
        try (var in = Files.newInputStream(Path.of("src/main/resources/database.properties"))) { props.load(in); }
        String name = "BClinic_Test_" + UUID.randomUUID().toString().replace("-", "");
        if (!name.matches("BClinic_Test_[a-f0-9]{32}")) { throw new IllegalStateException("Tên DB test không hợp lệ."); }
        String base = "jdbc:sqlserver://" + props.getProperty("db.host", "localhost") + ":"
                + props.getProperty("db.port", "1433") + ";encrypt=true;trustServerCertificate=true;"
                + (Boolean.parseBoolean(props.getProperty("db.integratedSecurity", "false"))
                        ? "integratedSecurity=true;" : "") + "databaseName=";
        Path directory = Files.createTempDirectory(Path.of("target"), "h2-dao-");
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
                try (var out = Files.newOutputStream(config)) { props.store(out, "Isolated H2_DAO verification"); }
                String cp = directory.toAbsolutePath() + java.io.File.pathSeparator
                        + System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
                String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
                child = new ProcessBuilder(java, "-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-Dsun.stderr.encoding=UTF-8", "-cp", cp,
                        Scenario.class.getName(), name).redirectErrorStream(true).redirectOutput(output.toFile()).start();
                assertTrue(child.waitFor(90, TimeUnit.SECONDS), "H2_DAO integration quá 90 giây.");
                String result = Files.readString(output);
                System.out.println(result);
                assertEquals(0, child.exitValue(), result);
                assertTrue(result.contains("H2_DAO_INTEGRATION_OK"), result);
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
        private static final LichLamViecDAO DAO = new LichLamViecDAO();
        private static final LocalDate DAY = LocalDate.now().plusYears(2);

        public static void main(String[] args) throws Exception {
            try (Connection c = DatabaseConnection.getInstance().getConnection()) {
                assertEquals(args[0], scalar(c, "SELECT DB_NAME()"));
                System.out.println("ISOLATED_DATABASE_CONFIRMED");
                try {
                    login(VaiTro.QUANLY);
                    LichLamViec a = shift(DAY, "NV002", 7, 9);
                    assertTrue(DAO.them(a));
                    assertTrue(a.getMaLLV().matches("LLV\\d{5}"));
                    assertEquals(LocalTime.of(7, 0), DAO.layTheoMa(a.getMaLLV()).getGioBatDau());
                    LichLamViec duplicate = shift(DAY, "NV002", 9, 12);
                    BusinessException ex = assertThrows(BusinessException.class, () -> DAO.them(duplicate));
                    assertEquals("Nhân viên đã được phân công ca này trong ngày.", ex.getMessage());
                    assertNull(duplicate.getMaLLV());
                    assertThrows(BusinessException.class, () -> DAO.them(shift(DAY, "NV002", 8, 10)));
                    LichLamViec other = shift(DAY, "NV003", 7, 9);
                    assertTrue(DAO.them(other));
                    assertNotEquals(a.getMaLLV(), other.getMaLLV());
                    LichLamViec afternoon = shift(DAY, "NV002", 14, 17);
                    afternoon.setCaTruc(CaTruc.CA_CHIEU);
                    assertTrue(DAO.them(afternoon));
                    LichLamViec full = shift(DAY, "NV002", 7, 17);
                    full.setCaTruc(CaTruc.CA_NGAY);
                    assertThrows(BusinessException.class, () -> DAO.them(full));
                    assertThrows(BusinessException.class, () -> DAO.them(shift(LocalDate.now().minusDays(1), "NV002", 7, 9)));
                    assertThrows(BusinessException.class, () -> DAO.them(shift(DAY.plusDays(1), "NV002", 6, 9)));
                    assertTrue(DAO.capNhat(a)); // không tự xem ca hiện tại là trùng
                    LichLamViec changed = new LichLamViec(other);
                    changed.setNhanVien(a.getNhanVien());
                    assertThrows(BusinessException.class, () -> DAO.capNhat(changed));
                    assertEquals("NV003", DAO.layTheoMa(other.getMaLLV()).getNhanVien().getMaNV());
                    LichLamViec missing = new LichLamViec(a);
                    missing.setMaLLV("LLV99999");
                    assertThrows(BusinessException.class, () -> DAO.capNhat(missing));
                    assertThrows(BusinessException.class, () -> DAO.layTheoNhanVien("NV002", DAY, DAY.minusDays(1)));
                    login(VaiTro.NHASI);
                    assertEquals(2, DAO.layTheoNhanVien("NV002", DAY, DAY).size());
                    assertTrue(DAO.layTatCa().stream().allMatch(x -> x.getNhanVien().getMaNV().equals("NV002")));
                    assertThrows(BusinessException.class, () -> DAO.layTheoMa(other.getMaLLV()));
                    assertThrows(BusinessException.class, () -> DAO.layTheoNhanVien("NV003", DAY, DAY));
                    assertThrows(BusinessException.class, () -> DAO.them(shift(DAY.plusDays(1), "NV002", 7, 9)));
                    login(VaiTro.LETAN);
                    assertThrows(BusinessException.class, () -> DAO.capNhat(other));
                    SessionManager.getInstance().logout();
                    assertThrows(BusinessException.class, DAO::layTatCa);
                    login(VaiTro.QUANLY);
                    try (var st = c.createStatement()) {
                        st.executeUpdate("UPDATE TaiKhoan SET trangThai=0 WHERE maNV='NV003'");
                    }
                    assertThrows(BusinessException.class, () -> DAO.them(shift(DAY.plusDays(1), "NV003", 7, 9)));
                    try (var st = c.createStatement()) {
                        st.executeUpdate("UPDATE TaiKhoan SET trangThai=1 WHERE maNV='NV003'");
                        st.execute("CREATE TRIGGER T601_FailAudit ON NhatKyHeThong AFTER INSERT AS BEGIN THROW 51000, 'T601 rollback', 1; END");
                    }
                    LichLamViec rollback = shift(DAY.plusDays(2), "NV002", 7, 9);
                    assertThrows(DataAccessException.class, () -> DAO.them(rollback));
                    assertNull(rollback.getMaLLV(), "Rollback không được để lại mã trên đối tượng");
                    assertTrue(DAO.layTheoNhanVien("NV002", DAY.plusDays(2), DAY.plusDays(2)).isEmpty());
                    try (var st = c.createStatement()) { st.execute("DROP TRIGGER T601_FailAudit"); }
                    ExecutorService pool = Executors.newFixedThreadPool(2);
                    CountDownLatch gate = new CountDownLatch(1);
                    try {
                        List<Future<Boolean>> results = new ArrayList<>();
                        for (int i=0; i<2; i++) results.add(pool.submit(() -> {
                            gate.await();
                            try { return DAO.them(shift(DAY.plusDays(3), "NV002", 7, 9)); }
                            catch (BusinessException expected) { return false; }
                        }));
                        gate.countDown();
                        int success = 0;
                        for (Future<Boolean> result : results) if (result.get(20, TimeUnit.SECONDS)) success++;
                        assertEquals(1, success);
                        assertEquals(1, DAO.layTheoNhanVien("NV002", DAY.plusDays(3), DAY.plusDays(3)).size());
                    } finally { pool.shutdownNow(); }
                    var leave=shift(DAY.plusDays(5),"NV002",7,17);
                    leave.setCaTruc(CaTruc.CA_NGAY); leave.nghiPhep("Nghỉ việc gia đình");
                    assertTrue(DAO.them(leave));
                    var blocked=shift(DAY.plusDays(5),"NV002",8,12);
                    assertTrue(assertThrows(BusinessException.class,()->DAO.them(blocked)).getMessage().contains("nghỉ phép"));
                    assertNull(blocked.getMaLLV());
                    var blockedAfternoon=shift(DAY.plusDays(5),"NV002",14,17); blockedAfternoon.setCaTruc(CaTruc.CA_CHIEU);
                    assertTrue(assertThrows(BusinessException.class,()->DAO.them(blockedAfternoon)).getMessage().contains("nghỉ phép"));
                    assertEquals(1,DAO.layTheoNhanVien("NV002",DAY.plusDays(5),DAY.plusDays(5)).size());
                    phanCongQuaGiaoDien();
                    System.out.println("H2_DAO_INTEGRATION_OK");
                } finally {
                    SessionManager.getInstance().logout();
                    DatabaseConnection.getInstance().getDataSource().close();
                }
            }
        }

        private static void phanCongQuaGiaoDien() throws Exception {
            var ref = new java.util.concurrent.atomic.AtomicReference<com.quanlynhakhoa.ui.panel.PnlLichLamViec>();
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                var panel = new com.quanlynhakhoa.ui.panel.PnlLichLamViec();
                ref.set(panel); panel.lamMoiDuLieu();
            });
            var panel = ref.get();
            choGiaoDien(() -> ((javax.swing.JComboBox<?>)tim(panel,"nhanVienPhanCong")).isEnabled());
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                ((javax.swing.JComboBox<?>)tim(panel,"viTri")).setSelectedItem("Nha sĩ");
                ((javax.swing.JComboBox<?>)tim(panel,"caPhanCong")).setSelectedIndex(1);
                ((com.toedter.calendar.JDateChooser)tim(panel,"ngayPhanCong")).setDate(java.util.Date.from(
                        DAY.plusDays(4).atStartOfDay(ZoneId.systemDefault()).toInstant()));
                ((javax.swing.JButton)tim(panel,"luuPhanCong")).doClick();
            });
            choGiaoDien(() -> ((javax.swing.JComboBox<?>)tim(panel,"nhanVienPhanCong")).isEnabled()
                    && ((javax.swing.JLabel)tim(panel,"thongBaoPhanCong")).getText().contains("thành công"));
            var rows = DAO.layTheoNhanVien("NV002",DAY.plusDays(4),DAY.plusDays(4));
            assertEquals(1,rows.size()); assertEquals(LocalTime.of(13,30),rows.getFirst().getGioBatDau());
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                var table=(javax.swing.JTable)tim(panel,"bangLichLamViec");
                String day=DAY.plusDays(4).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                assertTrue(java.util.stream.IntStream.range(0,table.getRowCount())
                        .anyMatch(i -> day.equals(table.getValueAt(i,0))&&table.getValueAt(i,4).toString().contains("13:30")));
                assertFalse(((javax.swing.JButton)tim(panel,"luuPhanCong")).isEnabled());
            });
            var duplicate=shift(DAY.plusDays(4),"NV002",14,17); duplicate.setCaTruc(CaTruc.CA_CHIEU);
            assertThrows(BusinessException.class,()->DAO.them(duplicate));
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                ((com.toedter.calendar.JDateChooser)tim(panel,"ngayPhanCong")).setDate(java.util.Date.from(
                        DAY.plusDays(5).atStartOfDay(ZoneId.systemDefault()).toInstant()));
                assertFalse(((javax.swing.JButton)tim(panel,"luuPhanCong")).isEnabled());
                assertTrue(((javax.swing.JLabel)tim(panel,"thongBaoPhanCong")).getText().contains("nghỉ phép"));
                ((javax.swing.JComboBox<?>)tim(panel,"caPhanCong")).setSelectedIndex(0);
                assertFalse(((javax.swing.JButton)tim(panel,"luuPhanCong")).isEnabled());
                ((com.toedter.calendar.JDateChooser)tim(panel,"ngayPhanCong")).setDate(java.util.Date.from(
                        DAY.plusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant()));
                assertTrue(((javax.swing.JButton)tim(panel,"luuPhanCong")).isEnabled());
            });
            assertEquals(1,DAO.layTheoNhanVien("NV002",DAY.plusDays(4),DAY.plusDays(4)).size());
            System.out.println("H2_UC013_UI_SAVE_AND_DUPLICATE_OK");
            System.out.println("H2_LEAVE_BLOCKED_UI_AND_DAO_OK");
        }

        private static java.awt.Component tim(java.awt.Container root,String name) {
            for(var child:root.getComponents()) {
                if(name.equals(child.getName())) return child;
                if(child instanceof java.awt.Container c) { var found=tim(c,name); if(found!=null)return found; }
            }
            return null;
        }

        private static void choGiaoDien(java.util.function.BooleanSupplier condition) throws Exception {
            for(int i=0;i<200;i++) {
                var ok=new java.util.concurrent.atomic.AtomicBoolean();
                javax.swing.SwingUtilities.invokeAndWait(() -> ok.set(condition.getAsBoolean()));
                if(ok.get())return;
                Thread.sleep(50);
            }
            fail("Giao diện phân công chưa hoàn tất xử lý.");
        }

        private static void login(VaiTro role) {
            SessionManager.getInstance().login(new TaiKhoanDAO().dangNhap("hieu." + role.name().toLowerCase(Locale.ROOT), "Clinic@123"));
        }

        private static LichLamViec shift(LocalDate day, String staff, int start, int end) {
            NhanVien nv = new NhanVien(); nv.setMaNV(staff);
            return new LichLamViec(null, day, CaTruc.CA_SANG,
                    LocalTime.of(start, 0), LocalTime.of(end, 0),
                    TrangThaiLichLamViec.DA_PHAN_CONG, null, nv);
        }

        private static String scalar(Connection c, String sql) throws SQLException {
            try (var s = c.prepareStatement(sql); var r = s.executeQuery()) { r.next(); return r.getString(1); }
        }
    }
}
