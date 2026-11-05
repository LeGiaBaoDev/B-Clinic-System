package com.quanlynhakhoa.integration;

import com.quanlynhakhoa.dao.*;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import com.quanlynhakhoa.exception.*;
import com.quanlynhakhoa.util.SessionManager;
import com.quanlynhakhoa.ui.dialog.DlgThanhToan;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.*;
import static org.junit.jupiter.api.Assertions.*;

/** Chỉ được gọi sau xác nhận DB UUID trong H3VoucherIntegrationTest.Scenario. */
final class H3ThanhToanScenario {
    private static final HoaDonDAO HD = new HoaDonDAO();
    private static final VoucherDAO VC = new VoucherDAO();
    private static final PhieuKhamDAO PK = new PhieuKhamDAO();
    private static int sequence;
    private record Bill(String hd, String pk, String lh) { }

    static void run(Connection c) throws Exception {
        seed(c);
        cashAndBank(c);
        invalidAndRollback(c);
        concurrency(c);
        paymentUi(c);
        System.out.println("H3_PAYMENT_SCENARIOS_OK");
    }

    private static void seed(Connection c) throws SQLException {
        sql(c, "INSERT INTO LoaiDichVu(maLoaiDV,tenLoaiDV,mucThueGTGT) VALUES('LDV01',N'Điều trị bệnh lý',0),('LDV02',N'Vật tư/thiết bị bán kèm',5),('LDV03',N'Thẩm mỹ',10)");
        sql(c, "INSERT INTO DichVu(maDV,maLoaiDV,tenDV,donGia,thoiLuong) VALUES('DV701','LDV01',N'Khám kiểm thử',100000,15),('DV702','LDV02',N'Vật tư kiểm thử',200000,15),('DV703','LDV03',N'Thẩm mỹ kiểm thử',300000,15)");
    }

    /** Lịch fixture hôm nay → tiếp nhận → bắt đầu khám → hoàn tất + tự sinh hóa đơn qua DAO thật. */
    private static Bill bill(Connection c) throws Exception {
        int n = ++sequence;
        String lh = "LH" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")) + String.format(Locale.ROOT, "%03d", 200 + n);
        sql(c, "INSERT INTO LichHen(maLH,maKH,maNV,maDV,ngayGioHen) VALUES(?,'KH00001','NV002','DV701',?)",
                lh, LocalDate.now().atStartOfDay().plusMinutes(n * 30L));
        login(VaiTro.LETAN);
        PhieuKham pk = PK.taoTuLichHen(lh);
        login(VaiTro.NHASI);
        assertTrue(PK.batDauKham(pk.getMaPK()));
        pk = PK.layTheoMa(pk.getMaPK());
        List<ChiTietPhieuKham> details = new ArrayList<>();
        for (String id : new String[]{"DV701", "DV702", "DV703"}) {
            DichVu dv = new DichVuDAO().layTheoMa(id);
            details.add(new ChiTietPhieuKham(0, "Triệu chứng test", "Chẩn đoán test", dv.getDonGia(), 1,
                    "Lần", switch (dv.getLoaiDichVu().getMucThueGTGT()) { case THUE_0 -> 0; case THUE_5 -> 5; case THUE_10 -> 10; },
                    "Kết quả test", pk, dv, pk.getLichHen().getNhaSi()));
        }
        PK.hoanTatPhieuKham(pk, details);
        login(VaiTro.LETAN);
        String hd = scalar(c, "SELECT maHD FROM HoaDon WHERE maPK=?", pk.getMaPK());
        assertEquals(0, HD.tinhTongTien(hd).compareTo(new BigDecimal("640000")));
        assertEquals("HOAN_TAT", scalar(c, "SELECT trangThai FROM PhieuKham WHERE maPK=?", pk.getMaPK()));
        assertEquals("DA_DEN", scalar(c, "SELECT trangThai FROM LichHen WHERE maLH=?", lh));
        return new Bill(hd, pk.getMaPK(), lh);
    }

    private static Voucher voucher(boolean percent, double value, double cap) {
        return voucher(percent, value, cap, LocalDate.now().minusDays(1), LocalDate.now().plusDays(2), true);
    }
    private static Voucher voucher(boolean percent, double value, double cap, LocalDate start, LocalDate end, boolean active) {
        login(VaiTro.QUANLY);
        Voucher v = new Voucher(null, "Ưu đãi thanh toán test", percent, value, cap, start, end, active);
        assertTrue(VC.them(v)); return v;
    }
    private static void money(String expected, BigDecimal actual) { assertEquals(0, new BigDecimal(expected).compareTo(actual)); }
    private static void paid(Connection c, Bill b, Voucher v, String discount) throws SQLException {
        assertEquals("1", scalar(c, "SELECT trangThai FROM HoaDon WHERE maHD=?", b.hd()));
        assertEquals(v.getMaVoucher(), scalar(c, "SELECT maVoucher FROM HoaDon WHERE maHD=?", b.hd()));
        money(discount, new BigDecimal(scalar(c, "SELECT tongKhuyenMai FROM HoaDon WHERE maHD=?", b.hd())));
        assertEquals("HOAN_TAT", scalar(c, "SELECT trangThai FROM LichHen WHERE maLH=?", b.lh()));
        assertEquals("1", scalar(c, "SELECT COUNT(*) FROM NhatKyHeThong WHERE hanhDong='THANH_TOAN' AND noiDungThayDoi=?", b.hd()));
    }
    private static void untouched(Connection c, Bill b) throws SQLException {
        assertEquals("0", scalar(c, "SELECT trangThai FROM HoaDon WHERE maHD=?", b.hd()));
        assertEquals("NULL", scalar(c, "SELECT COALESCE(maVoucher,'NULL') FROM HoaDon WHERE maHD=?", b.hd()));
        money("0", new BigDecimal(scalar(c, "SELECT tongKhuyenMai FROM HoaDon WHERE maHD=?", b.hd())));
        assertEquals("DA_DEN", scalar(c, "SELECT trangThai FROM LichHen WHERE maLH=?", b.lh()));
    }

    private static void cashAndBank(Connection c) throws Exception {
        Bill cash = bill(c); Voucher fixed = voucher(false, 50000, 0);
        login(VaiTro.LETAN);
        money("10000", HD.thanhToan(cash.hd(), HoaDon.TIEN_MAT, new BigDecimal("600000"), fixed.getMaVoucher()));
        paid(c, cash, fixed, "50000");
        assertThrows(BusinessException.class, () -> HD.thanhToan(cash.hd(), HoaDon.TIEN_MAT, new BigDecimal("600000"), fixed.getMaVoucher()));
        login(VaiTro.QUANLY);
        fixed.setGiaGiam(1); assertThrows(BusinessException.class, () -> VC.capNhat(fixed));
        assertTrue(VC.xoa(fixed.getMaVoucher())); paid(c, cash, fixed, "50000");

        Bill bank = bill(c); Voucher percent = voucher(true, 10, 0);
        money("0", HD.thanhToan(bank.hd(), HoaDon.CHUYEN_KHOAN, new BigDecimal("576000"), percent.getMaVoucher()));
        paid(c, bank, percent, "64000");
        Bill capped = bill(c); Voucher cap = voucher(true, 20, 70000);
        money("0", HD.thanhToan(capped.hd(), HoaDon.CHUYEN_KHOAN, new BigDecimal("570000"), cap.getMaVoucher()));
        paid(c, capped, cap, "70000");
        Bill free = bill(c); Voucher over = voucher(false, 1000000, 0);
        money("0", HD.thanhToan(free.hd(), HoaDon.TIEN_MAT, BigDecimal.ZERO, over.getMaVoucher()));
        paid(c, free, over, "640000");
        System.out.println("PAYMENT_TAX_CASH_BANK_CAP_HISTORY_OK");
    }

    private static void invalidAndRollback(Connection c) throws Exception {
        Bill b = bill(c);
        for (Voucher invalid : new Voucher[]{
                voucher(true, 10, 0, LocalDate.now().minusDays(4), LocalDate.now().minusDays(1), true),
                voucher(true, 10, 0, LocalDate.now().plusDays(1), LocalDate.now().plusDays(4), true),
                voucher(false, 50000, 0, LocalDate.now().minusDays(1), LocalDate.now().plusDays(2), false)}) {
            assertThrows(BusinessException.class, () -> HD.thanhToan(b.hd(), HoaDon.TIEN_MAT, new BigDecimal("640000"), invalid.getMaVoucher()));
            untouched(c, b);
            assertFalse(VC.layConHieuLuc(LocalDate.now()).stream().anyMatch(v -> v.getMaVoucher().equals(invalid.getMaVoucher())));
        }
        assertThrows(BusinessException.class, () -> HD.thanhToan(b.hd(), HoaDon.TIEN_MAT, new BigDecimal("640000"), "VC999"));
        Voucher good = voucher(false, 50000, 0);
        login(VaiTro.NHASI);
        assertThrows(BusinessException.class, () -> HD.thanhToan(b.hd(), HoaDon.TIEN_MAT, new BigDecimal("640000"), good.getMaVoucher()));
        login(VaiTro.LETAN);
        assertThrows(BusinessException.class, () -> HD.thanhToan(b.hd(), HoaDon.TIEN_MAT, new BigDecimal("589999"), good.getMaVoucher()));
        untouched(c, b);
        assertThrows(BusinessException.class, () -> HD.thanhToan(b.hd(), HoaDon.CHUYEN_KHOAN, new BigDecimal("600000"), good.getMaVoucher()));
        untouched(c, b);
        String audits = scalar(c, "SELECT COUNT(*) FROM NhatKyHeThong");
        sql(c, "CREATE TRIGGER trg_TestPaymentAudit ON NhatKyHeThong AFTER INSERT AS BEGIN IF EXISTS(SELECT 1 FROM inserted WHERE hanhDong='THANH_TOAN') THROW 51000, 'Forced payment audit failure', 1; END");
        try {
            assertThrows(DataAccessException.class, () -> HD.thanhToan(b.hd(), HoaDon.TIEN_MAT, new BigDecimal("600000"), good.getMaVoucher()));
            untouched(c, b); assertEquals(audits, scalar(c, "SELECT COUNT(*) FROM NhatKyHeThong"));
        } finally { sql(c, "DROP TRIGGER trg_TestPaymentAudit"); }
        money("10000", HD.thanhToan(b.hd(), HoaDon.TIEN_MAT, new BigDecimal("600000"), good.getMaVoucher()));
        paid(c, b, good, "50000");

        Bill second = bill(c); Voucher a = voucher(false, 20000, 0), other = voucher(false, 30000, 0);
        assertTrue(HD.apDungVoucher(second.hd(), a.getMaVoucher()));
        assertThrows(BusinessException.class, () -> HD.apDungVoucher(second.hd(), other.getMaVoucher()));
        assertThrows(BusinessException.class, () -> HD.thanhToan(second.hd(), HoaDon.TIEN_MAT, new BigDecimal("640000"), other.getMaVoucher()));
        money("0", HD.thanhToan(second.hd(), HoaDon.CHUYEN_KHOAN, new BigDecimal("620000"), a.getMaVoucher()));
        paid(c, second, a, "20000");
        System.out.println("PAYMENT_INVALID_PERMISSION_ROLLBACK_SINGLE_VOUCHER_OK");
    }

    private static void concurrency(Connection c) throws Exception {
        Bill duplicate = bill(c); Voucher v = voucher(false, 50000, 0);
        List<Boolean> results = race(() -> pay(duplicate, v), () -> pay(duplicate, v));
        assertEquals(1, results.stream().filter(Boolean::booleanValue).count()); paid(c, duplicate, v, "50000");

        Bill locking = bill(c); Voucher locked = voucher(false, 50000, 0);
        List<Boolean> lockResults = race(() -> VC.xoa(locked.getMaVoucher()), () -> pay(locking, locked));
        assertTrue(lockResults.get(0)); assertFalse(VC.layTheoMa(locked.getMaVoucher()).isTrangThai());
        if (lockResults.get(1)) paid(c, locking, locked, "50000"); else untouched(c, locking);

        Bill editing = bill(c); Voucher edited = voucher(false, 50000, 0);
        Voucher copy = new Voucher(edited); copy.setGiaGiam(45000);
        List<Boolean> editResults = race(() -> VC.capNhat(copy), () -> pay(editing, edited));
        assertTrue(editResults.get(1));
        paid(c, editing, edited, editResults.get(0) ? "45000" : "50000");
        money(editResults.get(0) ? "45000" : "50000", BigDecimal.valueOf(VC.layTheoMa(edited.getMaVoucher()).getGiaGiam()));
        System.out.println("PAYMENT_CONCURRENT_DOUBLE_LOCK_EDIT_OK");
    }
    private static boolean pay(Bill b, Voucher v) {
        HD.thanhToan(b.hd(), HoaDon.TIEN_MAT, new BigDecimal("640000"), v.getMaVoucher()); return true;
    }
    private static List<Boolean> race(Callable<Boolean> first, Callable<Boolean> second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2); CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> jobs = new ArrayList<>();
            for (Callable<Boolean> task : List.of(first, second)) jobs.add(pool.submit(() -> {
                assertTrue(start.await(10, TimeUnit.SECONDS));
                try { return task.call(); } catch (BusinessException expected) { return false; }
            }));
            start.countDown(); return List.of(jobs.get(0).get(30, TimeUnit.SECONDS), jobs.get(1).get(30, TimeUnit.SECONDS));
        } finally { start.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS)); }
    }

    /** Gọi constructor UI thật, không sửa lớp do Bảo sở hữu chỉ để chèn hook test. */
    private static void paymentUi(Connection c) throws Exception {
        Bill b = bill(c); Voucher v = voucher(true, 10, 0); login(VaiTro.LETAN);
        HoaDon invoice = HD.layTheoMa(b.hd());
        AtomicReference<DlgThanhToan> dialog = new AtomicReference<>(); AtomicBoolean completed = new AtomicBoolean();
        javax.swing.Timer closePrompt = new javax.swing.Timer(50, e -> {
            DlgThanhToan owner = dialog.get();
            if (owner != null) for (Window window : owner.getOwnedWindows()) {
                if (window instanceof JDialog prompt && "Thanh toán thành công".equals(prompt.getTitle())) {
                    JOptionPane pane = findPane(prompt.getContentPane()); if (pane != null) pane.setValue(JOptionPane.NO_OPTION);
                }
            }
        });
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    var ctor = DlgThanhToan.class.getDeclaredConstructor(Window.class, HoaDon.class, Runnable.class);
                    ctor.setAccessible(true); dialog.set(ctor.newInstance(null, invoice, (Runnable) () -> completed.set(true)));
                } catch (Exception e) { throw new RuntimeException(e); }
                closePrompt.start();
            });
            awaitEdt(() -> {
                JComboBox<?> choices = field(dialog.get(), "cboVoucher", JComboBox.class);
                return choices.isEnabled() && choices.getItemCount() > 1;
            });
            SwingUtilities.invokeAndWait(() -> {
                JComboBox<?> choices = field(dialog.get(), "cboVoucher", JComboBox.class);
                boolean found = false;
                for (int i = 0; i < choices.getItemCount(); i++) if (choices.getItemAt(i).toString().contains(v.getMaVoucher())) {
                    choices.setSelectedIndex(i); found = true; break;
                }
                assertTrue(found);
                field(dialog.get(), "txtKhachDua", JTextField.class).setText("600000");
                field(dialog.get(), "btnThanhToan", JButton.class).doClick();
            });
            awaitEdt(completed::get);
            paid(c, b, v, "64000");
            SwingUtilities.invokeAndWait(() -> assertFalse(field(dialog.get(), "btnThanhToan", JButton.class).isEnabled()));
            System.out.println("PAYMENT_REAL_DIALOG_TO_SQL_OK");
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                closePrompt.stop(); if (dialog.get() != null) { for (Window w : dialog.get().getOwnedWindows()) w.dispose(); dialog.get().dispose(); }
            });
        }
    }
    private static <T> T field(Object object, String name, Class<T> type) {
        try { var field = object.getClass().getDeclaredField(name); field.setAccessible(true); return type.cast(field.get(object)); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static void awaitEdt(java.util.function.BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < deadline) {
            AtomicBoolean done = new AtomicBoolean(); SwingUtilities.invokeAndWait(() -> done.set(condition.getAsBoolean()));
            if (done.get()) return; Thread.sleep(25);
        }
        fail("Timeout chờ UI thanh toán");
    }
    private static JOptionPane findPane(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JOptionPane pane) return pane;
            if (child instanceof Container container) { JOptionPane match = findPane(container); if (match != null) return match; }
        }
        return null;
    }
    private static void login(VaiTro role) {
        SessionManager.getInstance().login(new TaiKhoanDAO().dangNhap("hieu." + role.name().toLowerCase(Locale.ROOT), "Clinic@123"));
    }
    private static void sql(Connection c, String query, Object... args) throws SQLException {
        try (var s = c.prepareStatement(query)) {
            for (int i = 0; i < args.length; i++) s.setObject(i + 1, args[i]); s.execute();
        }
    }
    private static String scalar(Connection c, String query, Object... args) throws SQLException {
        try (var s = c.prepareStatement(query)) {
            for (int i = 0; i < args.length; i++) s.setObject(i + 1, args[i]);
            try (var rs = s.executeQuery()) { assertTrue(rs.next()); return rs.getString(1); }
        }
    }
}
