package com.quanlynhakhoa.integration;

import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.dao.*;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.ui.frame.FrmMain;
import com.quanlynhakhoa.util.*;
import java.awt.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in: database riêng và JVM riêng, không khởi tạo pool của DB ứng dụng trong JVM test. */
@EnabledIfSystemProperty(named = "bclinic.integration", matches = "true")
class H1IntegrationTest {
    @Test
    void h1_dangNhapPhanQuyenNhatKyTaoDongThoiVaCapNhat() throws Exception {
        Properties props = new Properties();
        try (var in = Files.newInputStream(Path.of("src/main/resources/database.properties"))) { props.load(in); }
        String name = "BClinic_Test_" + UUID.randomUUID().toString().replace("-", "");
        if (!name.matches("BClinic_Test_[a-f0-9]{32}")) { throw new IllegalStateException("Tên DB test không hợp lệ."); }
        String base = "jdbc:sqlserver://" + props.getProperty("db.host", "localhost") + ":"
                + props.getProperty("db.port", "1433") + ";encrypt=true;trustServerCertificate=true;"
                + (Boolean.parseBoolean(props.getProperty("db.integratedSecurity", "false"))
                        ? "integratedSecurity=true;" : "") + "databaseName=";
        Path directory = Files.createTempDirectory(Path.of("target"), "h1-integration-");
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
                try (var out = Files.newOutputStream(config)) { props.store(out, "Isolated H1 verification"); }
                String cp = directory.toAbsolutePath() + java.io.File.pathSeparator
                        + System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
                String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
                child = new ProcessBuilder(java, "-Dfile.encoding=UTF-8", "-cp", cp,
                        Scenario.class.getName(), name).redirectErrorStream(true).redirectOutput(output.toFile()).start();
                assertTrue(child.waitFor(90, TimeUnit.SECONDS), "H1 integration quá 90 giây.");
                String result = Files.readString(output);
                System.out.println(result);
                assertEquals(0, child.exitValue(), result);
                assertTrue(result.contains("H1_INTEGRATION_OK"), result);
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
        private static final TaiKhoanDAO DAO = new TaiKhoanDAO();
        private static final SessionManager SESSION = SessionManager.getInstance();

        public static void main(String[] args) throws Exception {
            try (Connection c = DatabaseConnection.getInstance().getConnection()) {
                check(args.length == 1 && args[0].matches("BClinic_Test_[a-f0-9]{32}")
                        && args[0].equals(scalar(c, "SELECT DB_NAME()")), "ISOLATED_DATABASE_CONFIRMED");
                try {
                    dangNhapVaPhanQuyen(c);
                    taoDongThoi(c);
                    capNhatVaRollback(c);
                    doiMatKhauLanDau(c);
                    check("0".equals(scalar(c, "SELECT COUNT(*) FROM NhatKyHeThong WHERE "
                            + "noiDungThayDoi LIKE '%Clinic@123%' OR noiDungThayDoi LIKE '%PBKDF2$%'")), "AUDIT_NO_PASSWORD");
                    System.out.println("H1_INTEGRATION_OK");
                } finally {
                    SESSION.logout();
                    SwingUtilities.invokeAndWait(() -> { for (Window w : Window.getWindows()) { w.dispose(); } });
                    DatabaseConnection.getInstance().getDataSource().close();
                }
            }
        }

        private static void dangNhapVaPhanQuyen(Connection c) throws Exception {
            denied(() -> DAO.dangNhap("hieu.quanly", "sai"), "WRONG_PASSWORD");
            denied(() -> DAO.dangNhap("khong.ton.tai", "Clinic@123"), "UNKNOWN_ACCOUNT");
            denied(() -> DAO.dangNhap("", ""), "EMPTY_CREDENTIALS");
            for (VaiTro role : VaiTro.values()) {
                TaiKhoan tk = DAO.dangNhap("hieu." + role.name().toLowerCase(Locale.ROOT), "Clinic@123");
                SESSION.login(tk);
                check(tk.getVaiTro() == role, "LOGIN_" + role);
                check(scalar(c, "SELECT matKhau FROM TaiKhoan WHERE maTK='" + tk.getMaTK() + "'")
                        .startsWith("PBKDF2$"), "HASH_UPGRADE_" + role);
                if (role == VaiTro.QUANLY) {
                    check(DAO.layTatCa().size() == 3, "MANAGER_LIST");
                    check(!new NhatKyHeThongDAO().layTatCa().isEmpty(), "MANAGER_AUDIT_READ");
                } else {
                    denied(DAO::layTatCa, "LIST_DENIED_" + role);
                    denied(() -> DAO.layTheoMa("TK001"), "OTHER_ACCOUNT_DENIED_" + role);
                    denied(() -> new NhatKyHeThongDAO().layTatCa(), "AUDIT_READ_DENIED_" + role);
                    denied(() -> create("Trần Văn Anh", "0900000000", "denied@example.com"), "CREATE_DENIED_" + role);
                    // Không tin vai trò bị sửa trong session UI.
                    tk.setVaiTro(VaiTro.QUANLY);
                    denied(DAO::layTatCa, "FORGED_UI_ROLE_DENIED_" + role);
                    tk.setVaiTro(role);
                }
                dangXuatQuaFrame(tk);
                check(SESSION.getCurrentUser() == null, "SESSION_CLEARED_" + role);
                check("1".equals(scalar(c, "SELECT COUNT(*) FROM NhatKyHeThong WHERE maTK='"
                        + tk.getMaTK() + "' AND hanhDong='DANG_XUAT'")), "LOGOUT_AUDIT_" + role);
                denied(DAO::layTatCa, "NO_SESSION_DENIED_" + role);
            }
            SESSION.login(DAO.dangNhap("hieu.quanly", "Clinic@123"));
        }

        /** Gọi đúng handler của FrmMain, tự trả lời hộp xác nhận; focus/mouse vật lý không cần thiết. */
        private static void dangXuatQuaFrame(TaiKhoan tk) throws Exception {
            if (GraphicsEnvironment.isHeadless()) { throw new IllegalStateException("Cần desktop để kiểm tra logout UI."); }
            CompletableFuture<Void> invoked = new CompletableFuture<>();
            SwingUtilities.invokeLater(() -> {
                FrmMain frame = new FrmMain(tk);
                frame.setVisible(true);
                javax.swing.Timer accept = new javax.swing.Timer(50, e -> {
                    for (Window w : Window.getWindows()) {
                        if (w instanceof JDialog d && d.isVisible() && "Đăng xuất".equals(d.getTitle())) {
                            acceptOption(d);
                        }
                    }
                });
                accept.start();
                try {
                    var method = FrmMain.class.getDeclaredMethod("yeuCauDangXuat");
                    method.setAccessible(true);
                    method.invoke(frame);
                    invoked.complete(null);
                } catch (Throwable e) { invoked.completeExceptionally(e); }
                finally { accept.stop(); }
            });
            invoked.get(15, TimeUnit.SECONDS);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
            boolean[] done = {false};
            while (System.nanoTime() < deadline) {
                SwingUtilities.invokeAndWait(() -> done[0] = SESSION.getCurrentUser() == null);
                if (done[0]) { break; }
                Thread.sleep(25);
            }
            check(done[0], "LOGOUT_UI_COMPLETED_" + tk.getVaiTro());
            SwingUtilities.invokeAndWait(() -> { for (Window w : Window.getWindows()) { w.dispose(); } });
        }

        private static void acceptOption(Container c) {
            for (Component child : c.getComponents()) {
                if (child instanceof JOptionPane option) { option.setValue(JOptionPane.YES_OPTION); }
                else if (child instanceof Container container) { acceptOption(container); }
            }
        }

        private static void taoDongThoi(Connection c) throws Exception {
            ExecutorService executor = Executors.newFixedThreadPool(4);
            Set<String> actual = new HashSet<>();
            try {
                for (int round = 0; round < 2; round++) {
                    CountDownLatch start = new CountDownLatch(1);
                    List<Future<TaiKhoan>> jobs = new ArrayList<>();
                    for (int i = 0; i < 4; i++) {
                        int number = round * 4 + i;
                        jobs.add(executor.submit(() -> {
                            start.await();
                            return create("Trần Văn Đồng", "09000001" + String.format("%02d", number),
                                    "concurrent" + number + "@example.com");
                        }));
                    }
                    start.countDown();
                    for (Future<TaiKhoan> job : jobs) {
                        TaiKhoan tk = job.get(25, TimeUnit.SECONDS);
                        check(actual.add(tk.getTenTK()), "UNIQUE_CONCURRENT_" + tk.getTenTK());
                        check(PasswordHasher.xacMinh("Clinic@123", scalar(c,
                                "SELECT matKhau FROM TaiKhoan WHERE maTK='" + tk.getMaTK() + "'")), "DEFAULT_HASH_" + tk.getMaTK());
                    }
                }
            } finally { executor.shutdownNow(); executor.awaitTermination(10, TimeUnit.SECONDS); }
            Set<String> expected = new HashSet<>(); expected.add("dong.nhasi");
            for (int i = 1; i < 8; i++) { expected.add("dong" + i + ".nhasi"); }
            check(expected.equals(actual), "CONCURRENT_SEQUENCE");
            check("11".equals(scalar(c, "SELECT COUNT(*) FROM NhanVien")), "NO_ORPHAN_EMPLOYEES");
        }

        private static void capNhatVaRollback(Connection c) throws Exception {
            TaiKhoan tk = DAO.layTatCa().stream().filter(t -> "dong.nhasi".equals(t.getTenTK())).findFirst().orElseThrow();
            String username = tk.getTenTK();
            String hash = scalar(c, "SELECT matKhau FROM TaiKhoan WHERE maTK='" + tk.getMaTK() + "'");
            NhanVien nv = new NhanVien(tk.getNhanVien());
            nv.setChuyenMon("Lễ tân");tk.setVaiTro(VaiTro.LETAN);tk.setTrangThai(false);
            nv.setEmail(DAO.layTheoMa("TK001").getNhanVien().getEmail());
            denied(() -> DAO.capNhatNhanVienVaTaiKhoan(nv, tk), "DUPLICATE_EMAIL_ROLLBACK", "trùng");
            check(DAO.layTheoMa(tk.getMaTK()).isTrangThai()
                    && DAO.layTheoMa(tk.getMaTK()).getVaiTro() == VaiTro.NHASI, "ACCOUNT_ROLLED_BACK");
            nv.setEmail("updated@example.com");nv.setHoTenNV("Trần Văn Đạt");
            check(DAO.capNhatNhanVienVaTaiKhoan(nv, tk), "UPDATE_LOCK");
            denied(() -> DAO.dangNhap(username, "Clinic@123"), "LOCKED_LOGIN_DENIED");
            check(hash.equals(scalar(c, "SELECT matKhau FROM TaiKhoan WHERE maTK='" + tk.getMaTK() + "'")), "PASSWORD_UNCHANGED");
            check(username.equals(DAO.layTheoMa(tk.getMaTK()).getTenTK()), "USERNAME_UNCHANGED");
            tk.setTrangThai(true);DAO.capNhatNhanVienVaTaiKhoan(nv, tk);
            check(DAO.dangNhap(username, "Clinic@123").getVaiTro() == VaiTro.LETAN, "UNLOCK_LOGIN");
            tk.setVaiTro(VaiTro.NHASI);nv.setChuyenMon("Nha sĩ");
            DAO.capNhatNhanVienVaTaiKhoan(nv, tk);
            try (var s = c.createStatement()) {
                s.executeUpdate("INSERT INTO KhachHang(maKH,tenKH,gioiTinh,sdt) VALUES('KH00001',N'Khách Kiểm Thử',1,'0909999999')");
                s.executeUpdate("INSERT INTO LoaiDichVu(maLoaiDV,tenLoaiDV,mucThueGTGT) VALUES('LDV01',N'Điều trị bệnh lý',0)");
                s.executeUpdate("INSERT INTO DichVu(maDV,maLoaiDV,tenDV,donGia,thoiLuong) VALUES('DV001','LDV01',N'Khám',100000,30)");
            }
            try (var s = c.prepareStatement("INSERT INTO LichHen(maLH,maKH,maNV,maDV,ngayGioHen,trangThai)"
                    + " VALUES('LH261104001','KH00001',?,'DV001','2026-11-05T09:00:00','DA_DAT')")) {
                s.setString(1, nv.getMaNV());s.executeUpdate();
            }
            tk.setTrangThai(false);
            denied(() -> DAO.capNhatNhanVienVaTaiKhoan(nv, tk), "PENDING_APPOINTMENT_LOCK_DENIED", "lịch khám");
            tk.setTrangThai(true);tk.setVaiTro(VaiTro.LETAN);nv.setChuyenMon("Lễ tân");
            denied(() -> DAO.capNhatNhanVienVaTaiKhoan(nv, tk), "PENDING_APPOINTMENT_ROLE_DENIED", "lịch khám");
            tk.setMaTK("TK999");
            denied(() -> DAO.capNhatNhanVienVaTaiKhoan(nv, tk), "MISSING_ACCOUNT_DENIED", "Không tìm thấy");
            TaiKhoan self = DAO.layTheoMa("TK001");NhanVien manager = new NhanVien(self.getNhanVien());
            manager.setChuyenMon("Quản lý");self.setTrangThai(false);
            denied(() -> DAO.capNhatNhanVienVaTaiKhoan(manager, self), "SELF_LOCK_DENIED", "tự khóa");
            self.setTrangThai(true);self.setVaiTro(VaiTro.LETAN);manager.setChuyenMon("Lễ tân");
            denied(() -> DAO.capNhatNhanVienVaTaiKhoan(manager, self), "SELF_DEMOTION_DENIED", "hạ quyền");
            String count = scalar(c, "SELECT COUNT(*) FROM NhanVien");
            denied(() -> create("Trần Văn Anh", manager.getSdt(), "dup@example.com"), "DUPLICATE_PHONE");
            check(count.equals(scalar(c, "SELECT COUNT(*) FROM NhanVien")), "CREATE_ROLLBACK");
        }

        private static TaiKhoan create(String name, String phone, String email) {
            NhanVien nv = new NhanVien(null, name, true, phone, email, "Nha sĩ");
            TaiKhoan tk = new TaiKhoan(null, "forged.username", "Forged@123", VaiTro.NHASI, true, nv);
            check(DAO.taoNhanVienVaTaiKhoan(nv, tk), "CREATE_" + email);
            return tk;
        }

        private static void doiMatKhauLanDau(Connection c) throws Exception {
            TaiKhoan tk = create("Lê Văn Mới", "0901111999", "first-login@example.com");
            denied(() -> DAO.doiMatKhauLanDau(tk.getMaTK(), "sai", "Moi@123"), "FIRST_CHANGE_WRONG_CREDENTIAL");
            denied(() -> DAO.doiMatKhauLanDau(tk.getMaTK(), "Clinic@123", "Clinic@123"), "FIRST_CHANGE_DEFAULT_DENIED");
            SESSION.logout();
            dangNhapQuaFrame(tk.getTenTK(), "Clinic@123", true, false);
            check(PasswordHasher.xacMinh("Clinic@123", scalar(c,
                    "SELECT matKhau FROM TaiKhoan WHERE maTK='" + tk.getMaTK() + "'")), "CANCEL_DEFAULT_UNCHANGED");
            dangNhapQuaFrame(tk.getTenTK(), "Clinic@123", true, true);
            check(PasswordHasher.xacMinh("MoiLanDau@456", scalar(c,
                    "SELECT matKhau FROM TaiKhoan WHERE maTK='" + tk.getMaTK() + "'")), "FIRST_CHANGE_HASH");
            denied(() -> DAO.dangNhap(tk.getTenTK(), "Clinic@123"), "OLD_DEFAULT_LOGIN_DENIED");
            denied(() -> DAO.doiMatKhauLanDau(tk.getMaTK(), "Clinic@123", "Khac@123"), "DEFAULT_CANNOT_REUSE");
            check("1".equals(scalar(c, "SELECT COUNT(*) FROM NhatKyHeThong WHERE maTK='"
                    + tk.getMaTK() + "' AND hanhDong='DOI_MAT_KHAU_LAN_DAU'")), "FIRST_CHANGE_AUDIT");
            SESSION.logout();
            dangNhapQuaFrame(tk.getTenTK(), "MoiLanDau@456", false, true);
            SESSION.logout();
        }

        /** Đi qua FrmDangNhap thật, kiểm tra không cấp session/mở main trước khi đổi thành công. */
        private static void dangNhapQuaFrame(String ten, String password, boolean expectDialog, boolean accept) throws Exception {
            CompletableFuture<Void> finished = new CompletableFuture<>();
            SwingUtilities.invokeLater(() -> {
                com.quanlynhakhoa.ui.frame.FrmDangNhap frame = new com.quanlynhakhoa.ui.frame.FrmDangNhap();
                frame.setVisible(true);
                boolean[] seen = {false};
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
                javax.swing.Timer timer = new javax.swing.Timer(50, null);
                timer.addActionListener(e -> {
                    try {
                        for (Window w : Window.getWindows()) {
                            if (w.isVisible() && "doiMatKhauLanDau".equals(w.getName()) && !seen[0]) {
                                check(expectDialog, "FIRST_CHANGE_DIALOG_EXPECTED");
                                check(SESSION.getCurrentUser() == null, "NO_SESSION_BEFORE_CHANGE");
                                check(Arrays.stream(Window.getWindows()).noneMatch(x -> x instanceof FrmMain && x.isVisible()),
                                        "NO_MAIN_BEFORE_CHANGE");
                                seen[0] = true;
                                if (!accept) { w.dispatchEvent(new java.awt.event.WindowEvent(w, java.awt.event.WindowEvent.WINDOW_CLOSING)); }
                                else {
                                    ((JPasswordField) find((Container) w, "matKhauMoi")).setText("MoiLanDau@456");
                                    ((JPasswordField) find((Container) w, "xacNhanMatKhau")).setText("MoiLanDau@456");
                                    ((JButton) find((Container) w, "luuMatKhau")).doClick();
                                }
                            }
                        }
                        boolean main = Arrays.stream(Window.getWindows()).anyMatch(w -> w instanceof FrmMain && w.isVisible());
                        if (!accept && seen[0] && Arrays.stream(Window.getWindows())
                                .noneMatch(w -> w.isVisible() && "doiMatKhauLanDau".equals(w.getName()))) {
                            check(!main && SESSION.getCurrentUser() == null, "CANCEL_STAYS_AT_LOGIN");
                            timer.stop();finished.complete(null);
                        } else if (accept && main) {
                            check(SESSION.getCurrentUser() != null, "SESSION_AFTER_CHANGE");
                            check(seen[0] == expectDialog, "CHALLENGE_ONLY_FOR_DEFAULT_PASSWORD");
                            timer.stop();finished.complete(null);
                        } else if (System.nanoTime() > deadline) { throw new AssertionError("Login UI quá thời gian."); }
                    } catch (Throwable ex) { timer.stop();finished.completeExceptionally(ex); }
                });
                try {
                    var nameField = frame.getClass().getDeclaredField("txtTenDangNhap");nameField.setAccessible(true);
                    ((JTextField) nameField.get(frame)).setText(ten);
                    var passField = frame.getClass().getDeclaredField("txtMatKhau");passField.setAccessible(true);
                    ((JPasswordField) passField.get(frame)).setText(password);
                    var method = frame.getClass().getDeclaredMethod("thucHienDangNhap");method.setAccessible(true);
                    timer.start();method.invoke(frame);
                } catch (Throwable ex) { timer.stop();finished.completeExceptionally(ex); }
            });
            try { finished.get(25, TimeUnit.SECONDS); }
            finally { SwingUtilities.invokeAndWait(() -> { for (Window w : Window.getWindows()) { w.dispose(); } }); }
        }

        private static Component find(Container parent, String name) {
            for (Component c : parent.getComponents()) {
                if (name.equals(c.getName())) { return c; }
                if (c instanceof Container p) { Component result = find(p, name);if (result != null) { return result; } }
            }
            return null;
        }

        private static String scalar(Connection c, String sql) throws SQLException {
            try (var s = c.createStatement(); var r = s.executeQuery(sql)) { r.next(); return r.getString(1); }
        }
        private static void denied(Runnable action, String name) {
            denied(action, name, null);
        }
        private static void denied(Runnable action, String name, String reason) {
            try { action.run(); throw new AssertionError("Không chặn: " + name); }
            catch (BusinessException expected) {
                if (reason != null && !expected.getMessage().contains(reason)) {
                    throw new AssertionError("Lỗi sai nguyên nhân: " + name, expected);
                }
                System.out.println(name + "=OK");
            }
        }
        private static void check(boolean value, String name) {
            if (!value) { throw new AssertionError(name); }
            System.out.println(name + "=OK");
        }
    }
}
