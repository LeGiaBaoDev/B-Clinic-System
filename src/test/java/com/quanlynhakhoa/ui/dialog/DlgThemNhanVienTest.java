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
    void nhapSai_hienHuongDanDuoiO_suaDungThiAn() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            form = new DlgThemNhanVien.NoiDung((nv, tk) -> true, nv -> {}, () -> {});
            for (String name : new String[]{"sdt", "email", "hoTen"}) {
                JTextField field = (JTextField) tim(name);
                JLabel error = (JLabel) tim(name + "Loi");
                assertFalse(error.isVisible());
                field.setText(name.equals("hoTen") ? "Anh" : "sai");
                java.awt.event.FocusEvent event = new java.awt.event.FocusEvent(field, java.awt.event.FocusEvent.FOCUS_LOST);
                for (java.awt.event.FocusListener listener : field.getFocusListeners()) { listener.focusLost(event); }
                assertTrue(error.isVisible());
                assertFalse(error.getText().isBlank());
                assertTrue(error.getForeground().getRed() > error.getForeground().getGreen());
                field.setText(switch (name) {
                    case "sdt" -> "0912345678";
                    case "email" -> "ten@example.com";
                    default -> "Nguyễn Văn Anh";
                });
                for (java.awt.event.FocusListener listener : field.getFocusListeners()) { listener.focusLost(event); }
                assertFalse(error.isVisible());
                assertNull(field.getClientProperty("JComponent.outline"));
            }
        });
    }

    @Test
    void tenTrung_hienSoConTrongTheoChuyenMon() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            form = new DlgThemNhanVien.NoiDung((nv, tk) -> true, nv -> {}, () -> {});
            text("hoTen", "Nguyễn Văn Anh");
            roiOHoTen();
            form.capNhatTenDaDung(java.util.List.of("anh.letan", "anh1.letan", "anh.nhasi"));
            assertEquals("anh1.nhasi", ((JTextField) tim("tenTaiKhoan")).getText());
            ((JComboBox<?>) tim("chuyenMon")).setSelectedItem("Lễ tân");
            assertEquals("anh2.letan", ((JTextField) tim("tenTaiKhoan")).getText());
        });
    }

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
    void tenTaiKhoanCapNhatTheoHoTenVaChuyenMon_khongChoNhapTay() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            form = new DlgThemNhanVien.NoiDung((nv, tk) -> true, nv -> {}, () -> {});
            JTextField username = (JTextField) tim("tenTaiKhoan");
            JTextField password = (JTextField) tim("matKhau");
            assertEquals("", username.getText());
            assertFalse(username.isEditable());
            assertFalse(password.isEditable());
            assertEquals("Clinic@123", password.getText());
            text("hoTen", "  nGUYỄN   vĂN aNH  ");
            assertEquals("", username.getText());
            roiOHoTen();
            assertEquals("Nguyễn Văn Anh", ((JTextField) tim("hoTen")).getText());
            assertEquals("anh.nhasi", username.getText());
            ((JComboBox<?>) tim("chuyenMon")).setSelectedItem("Lễ tân");
            assertEquals("anh.letan", username.getText());
            text("hoTen", "Đỗ Văn Đạt");
            form.capNhatTenDaDung(java.util.List.of("dat.letan"));
            assertEquals("anh.letan", username.getText());
            roiOHoTen();
            assertEquals("dat1.letan", username.getText());
            text("hoTen", "Đỗ Văn Đạt");
            form.capNhatTenDaDung(java.util.List.of());
            roiOHoTen();
            assertEquals("dat.letan", username.getText());
        });
    }

    @Test
    void taoThanhCong_luuNenMotLan_chungNhanVien_vaXoaMatKhauTrongDoiTuong() throws Exception {
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
                assertEquals("Nguyễn Văn Anh", nv.getHoTenNV());
                assertEquals("anh.nhasi", tk.getTenTK());
                assertEquals("Clinic@123", tk.getMatKhau());
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
        SwingUtilities.invokeAndWait(() -> assertEquals("Clinic@123",
                ((JTextField) tim("matKhau")).getText()));
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
            assertEquals("anh.nhasi", ((JTextField) tim("tenTaiKhoan")).getText());
            assertEquals("Clinic@123", ((JTextField) tim("matKhau")).getText());
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
    void huyForm_khongLuu() throws Exception {
        AtomicBoolean called = new AtomicBoolean();
        AtomicBoolean closed = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> {
            form = new DlgThemNhanVien.NoiDung((nv, tk) -> { called.set(true); return true; }, nv -> {},
                    () -> closed.set(true));
            dienHopLe();
            nut("huyNhanVien").doClick();
            assertEquals("Clinic@123", ((JTextField) tim("matKhau")).getText());
        });
        assertTrue(closed.get());
        assertFalse(called.get());
    }

    @Test
    void chuyenMon_dongBoCaBaVaiTro_vaKhongChoDoiQuyenRieng() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            form = new DlgThemNhanVien.NoiDung((nv, tk) -> true, nv -> {}, () -> {});
            JComboBox<?> specialty = (JComboBox<?>) tim("chuyenMon");
            JComboBox<?> role = (JComboBox<?>) tim("vaiTro");
            assertFalse(specialty.isEditable());
            assertEquals(3, specialty.getItemCount());
            String[] labels = {"Nha sĩ", "Lễ tân", "Quản lý"};
            VaiTro[] roles = {VaiTro.NHASI, VaiTro.LETAN, VaiTro.QUANLY};
            for (int i = 0; i < labels.length; i++) {
                specialty.setSelectedItem(labels[i]);
                assertEquals(roles[i], role.getSelectedItem());
                assertFalse(role.isEnabled());
            }
        });
    }

    @Test
    void luuNhanVien_layQuyenTheoChuyenMon_keCaKhiGiaTriQuyenBiDoiBangCode() throws Exception {
        AtomicReference<TaiKhoan> saved = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            form = new DlgThemNhanVien.NoiDung((nv, tk) -> {
                saved.set(tk);
                return true;
            }, nv -> {}, () -> {});
            dienHopLe();
            ((JComboBox<?>) tim("chuyenMon")).setSelectedItem("Lễ tân");
            ((JComboBox<?>) tim("vaiTro")).setSelectedItem(VaiTro.QUANLY);
            text("tenTaiKhoan", "ten.bi.sua");
            text("matKhau", "MatKhauBiSua");
            nut("luuNhanVien").doClick();
        });
        choXong();
        assertEquals("anh.letan", saved.get().getTenTK());
        assertEquals("Lễ tân", saved.get().getNhanVien().getChuyenMon());
        assertEquals(VaiTro.LETAN, saved.get().getVaiTro());
        SwingUtilities.invokeAndWait(() -> assertFalse(tim("vaiTro").isEnabled()));
    }

    private void dienHopLe() {
        text("hoTen", "Nguyễn Văn Anh");
        text("sdt", "0912345678");
        text("email", "moi@example.com");
        ((JComboBox<?>) tim("chuyenMon")).setSelectedItem("Nha sĩ");
    }

    private void text(String name, String value) { ((JTextField) tim(name)).setText(value); }
    private void roiOHoTen() {
        JTextField field = (JTextField) tim("hoTen");
        java.awt.event.FocusEvent event = new java.awt.event.FocusEvent(field, java.awt.event.FocusEvent.FOCUS_LOST);
        for (java.awt.event.FocusListener listener : field.getFocusListeners()) { listener.focusLost(event); }
    }
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
