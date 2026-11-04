package com.quanlynhakhoa.ui.dialog;

import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.util.SessionManager;
import java.awt.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DlgDoiMatKhauLanDauTest {
    private DlgDoiMatKhauLanDau.NoiDung form;

    @Test
    void duLieuSai_khongLuu_hienLoi() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            form = new DlgDoiMatKhauLanDau.NoiDung(m -> { calls.incrementAndGet(); return new TaiKhoan(); }, t -> {}, () -> {});
            nhap("Clinic@123", "Clinic@123");nut("luuMatKhau").doClick();
            assertTrue(loi().contains("khác mật khẩu mặc định"));
            nhap("Moi@123", "Khac@123");nut("luuMatKhau").doClick();
            assertTrue(loi().contains("không khớp"));
            nhap("123", "123");nut("luuMatKhau").doClick();
            assertTrue(loi().contains("ít nhất 6"));
        });
        assertEquals(0, calls.get());
    }

    @Test
    void luuNen_chongGuiLap_chanDong_khongTuCapSession() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1), completed = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();AtomicBoolean closed = new AtomicBoolean();
        TaiKhoan result = new TaiKhoan();
        SessionManager.getInstance().logout();
        SwingUtilities.invokeAndWait(() -> {
            form = new DlgDoiMatKhauLanDau.NoiDung(m -> {
                assertFalse(SwingUtilities.isEventDispatchThread());assertEquals("Moi@123", m);
                calls.incrementAndGet();entered.countDown();
                try { assertTrue(release.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException e) { throw new AssertionError(e); }
                return result;
            }, tk -> { assertSame(result, tk);completed.countDown(); }, () -> closed.set(true));
            nhap("Moi@123", "Moi@123");nut("luuMatKhau").doClick();
        });
        try {
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                assertFalse(nut("luuMatKhau").isEnabled());form.yeuCauDong();nut("luuMatKhau").doClick();
            });
            assertFalse(closed.get());assertNull(SessionManager.getInstance().getCurrentUser());
        } finally { release.countDown(); }
        assertTrue(completed.await(5, TimeUnit.SECONDS));assertEquals(1, calls.get());
        SwingUtilities.invokeAndWait(() -> assertEquals(0, ((JPasswordField) tim(form, "matKhauMoi")).getPassword().length));
    }

    @Test
    void huy_khongLuu_khongBaoThanhCong() throws Exception {
        AtomicBoolean closed = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> {
            form = new DlgDoiMatKhauLanDau.NoiDung(m -> { fail("Không được lưu");return null; },
                    t -> fail("Không được đăng nhập"), () -> closed.set(true));
            nhap("Moi@123", "Moi@123");form.yeuCauDong();
            assertEquals(0, ((JPasswordField) tim(form, "matKhauMoi")).getPassword().length);
        });
        assertTrue(closed.get());
    }

    @Test
    void daoLoi_giuForm_choThuLai() throws Exception {
        AtomicBoolean success = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> {
            form = new DlgDoiMatKhauLanDau.NoiDung(m -> { throw new BusinessException("Tài khoản đã bị khóa."); },
                    t -> success.set(true), () -> {});
            nhap("Moi@123", "Moi@123");nut("luuMatKhau").doClick();
        });
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        AtomicBoolean ready = new AtomicBoolean();
        while (System.nanoTime() < deadline) {
            SwingUtilities.invokeAndWait(() -> ready.set(nut("luuMatKhau").isEnabled()));
            if (ready.get()) { break; }Thread.sleep(20);
        }
        assertTrue(ready.get());assertFalse(success.get());
        SwingUtilities.invokeAndWait(() -> assertTrue(loi().contains("bị khóa")));
    }

    private void nhap(String a, String b) {
        ((JPasswordField) tim(form, "matKhauMoi")).setText(a);
        ((JPasswordField) tim(form, "xacNhanMatKhau")).setText(b);
    }
    private JButton nut(String name) { return (JButton) tim(form, name); }
    private String loi() { return ((JLabel) tim(form, "loiDoiMatKhau")).getText(); }
    private Component tim(Container parent, String name) {
        for (Component c : parent.getComponents()) {
            if (name.equals(c.getName())) { return c; }
            if (c instanceof Container p) { Component found = tim(p, name);if (found != null) { return found; } }
        }
        return null;
    }
}
