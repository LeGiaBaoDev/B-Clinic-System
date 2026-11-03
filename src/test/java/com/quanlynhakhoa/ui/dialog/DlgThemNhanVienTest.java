package com.quanlynhakhoa.ui.dialog;

import com.quanlynhakhoa.entity.NhanVien;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.util.SessionManager;
import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DlgThemNhanVienTest {
    private DlgThemNhanVien.NoiDung form;

    @BeforeEach
    void dangNhap() {
        TaiKhoan actor = new TaiKhoan();
        actor.setVaiTro(VaiTro.QUANLY);
        SessionManager.getInstance().login(actor);
    }

    @AfterEach
    void dangXuat() { SessionManager.getInstance().logout(); }

    @Test
    void duLieuSai_khongGoiDao_hienLoiTruong() throws Exception {
        AtomicBoolean called = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> {
            form = new DlgThemNhanVien.NoiDung((nv, tk) -> { called.set(true); return true; }, nv -> {}, () -> {});
            dienHopLe();
            text("email", "sai-email");
            nut("luuNhanVien").doClick();
            assertTrue(thongBao().contains("Email"));
        });
        assertFalse(called.get());
    }

    @Test
    void matKhauKhongKhop_khongLuu() throws Exception {
        AtomicBoolean called = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> {
            form = new DlgThemNhanVien.NoiDung((nv, tk) -> { called.set(true); return true; }, nv -> {}, () -> {});
            dienHopLe();
            text("nhapLai", "Khac@123");
            nut("luuNhanVien").doClick();
            assertTrue(thongBao().contains("không khớp"));
        });
        assertFalse(called.get());
    }

    @Test
    void taoThanhCong_luuNenMotLan_chungNhanVien_vaXoaMatKhau() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger saves = new AtomicInteger();
        AtomicBoolean background = new AtomicBoolean();
        AtomicReference<NhanVien> created = new AtomicReference<>();
        AtomicReference<TaiKhoan> account = new AtomicReference<>();
        AtomicBoolean closed = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> {
            form = new DlgThemNhanVien.NoiDung((nv, tk) -> {
                saves.incrementAndGet();
                background.set(!SwingUtilities.isEventDispatchThread());
                assertSame(nv, tk.getNhanVien());
                assertNull(nv.getMaNV());
                assertNull(tk.getMaTK());
                assertEquals("Nha sĩ mới", nv.getHoTenNV());
                assertEquals(VaiTro.NHASI, tk.getVaiTro());
                assertTrue(tk.isTrangThai());
                account.set(tk);
                entered.countDown();
                try { assertTrue(release.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException ex) { throw new AssertionError(ex); }
                nv.setMaNV("NV010");
                tk.setMaTK("TK010");
                return true;
            }, created::set, () -> closed.set(true));
            dienHopLe();
            nut("luuNhanVien").doClick();
        });
        try {
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                assertFalse(nut("luuNhanVien").isEnabled());
                assertFalse(nut("huyNhanVien").isEnabled());
                form.yeuCauDong();
                nut("luuNhanVien").doClick();
            });
            assertFalse(closed.get());
        } finally { release.countDown(); }
        choXong();
        assertTrue(background.get());
        assertEquals(1, saves.get());
        assertEquals("NV010", created.get().getMaNV());
        assertEquals("[Ẩn]", account.get().getMatKhau());
        SwingUtilities.invokeAndWait(() -> assertEquals(0,
                ((JPasswordField) tim("matKhau")).getPassword().length));
    }

    @Test
    void duLieuTrung_baoLoi_giuThongTinChoSua_khongBaoThanhCong() throws Exception {
        AtomicBoolean created = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> {
            form = new DlgThemNhanVien.NoiDung((nv, tk) -> {
                throw new BusinessException("Số điện thoại hoặc tên đăng nhập đã tồn tại.");
            }, nv -> created.set(true), () -> {});
            dienHopLe();
            nut("luuNhanVien").doClick();
        });
        choXong();
        assertFalse(created.get());
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(thongBao().contains("đã tồn tại"));
            assertEquals("nhasi.moi", ((JTextField) tim("tenTaiKhoan")).getText());
            assertEquals(0, ((JPasswordField) tim("matKhau")).getPassword().length);
            assertTrue(nut("huyNhanVien").isEnabled());
        });
    }

    @Test
    void nhaSi_khongDuocGoiLuu() throws Exception {
        SessionManager.getInstance().getCurrentUser().setVaiTro(VaiTro.NHASI);
        AtomicBoolean called = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> {
            form = new DlgThemNhanVien.NoiDung((nv, tk) -> { called.set(true); return true; }, nv -> {}, () -> {});
            dienHopLe();
            nut("luuNhanVien").doClick();
            assertTrue(thongBao().contains("Chỉ Quản lý"));
        });
        assertFalse(called.get());
    }

    @Test
    void huyForm_khongLuu_vaXoaMatKhau() throws Exception {
        AtomicBoolean called = new AtomicBoolean();
        AtomicBoolean closed = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> {
            form = new DlgThemNhanVien.NoiDung((nv, tk) -> { called.set(true); return true; }, nv -> {},
                    () -> closed.set(true));
            dienHopLe();
            nut("huyNhanVien").doClick();
            assertEquals(0, ((JPasswordField) tim("matKhau")).getPassword().length);
        });
        assertTrue(closed.get());
        assertFalse(called.get());
    }

    private void dienHopLe() {
        text("hoTen", "Nha sĩ mới");
        text("sdt", "0912345678");
        text("email", "moi@example.com");
        text("chuyenMon", "Răng hàm mặt");
        text("tenTaiKhoan", "nhasi.moi");
        text("matKhau", "Clinic@123");
        text("nhapLai", "Clinic@123");
    }

    private void text(String name, String value) { ((JTextField) tim(name)).setText(value); }
    private JButton nut(String name) { return (JButton) tim(name); }
    private String thongBao() { return ((JLabel) tim("thongBao")).getText(); }

    private void choXong() throws Exception {
        for (int i = 0; i < 250; i++) {
            AtomicBoolean ready = new AtomicBoolean();
            SwingUtilities.invokeAndWait(() -> ready.set(nut("luuNhanVien").isEnabled()));
            if (ready.get()) { return; }
            Thread.sleep(20);
        }
        fail("Lưu không hoàn tất trong 5 giây");
    }

    private Component tim(String name) {
        Component found = timTrong(form, name);
        if (found == null) { throw new AssertionError("Thiếu component " + name); }
        return found;
    }

    private Component timTrong(Component c, String name) {
        if (name.equals(c.getName())) { return c; }
        if (c instanceof Container container) {
            for (Component child : container.getComponents()) {
                Component found = timTrong(child, name);
                if (found != null) { return found; }
            }
        }
        return null;
    }
}
