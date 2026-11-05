package com.quanlynhakhoa.ui.dialog;

import com.quanlynhakhoa.entity.Voucher;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;
import java.time.LocalDate;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DlgVoucherTest {
    private DlgVoucher.NoiDung form;
    private final AtomicReference<Voucher> saved = new AtomicReference<>();
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicBoolean closed = new AtomicBoolean();

    private Voucher voucher() {
        return new Voucher("VC001", "Ưu đãi cũ", true, 10, 20000,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1), true);
    }
    private void create(Voucher value, boolean readOnly, Function<Voucher, Boolean> save) throws Exception {
        SwingUtilities.invokeAndWait(() -> form = new DlgVoucher.NoiDung(value, readOnly, save,
                saved::set, () -> closed.set(true)));
    }
    private void valid() {
        form.txtTen.setText("Ưu đãi mới"); form.cboLoai.setSelectedIndex(1); form.txtGiaGiam.setText("12,5");
        form.txtToiDa.setText("50000");
        ((JTextField) form.dtpBatDau.getDateEditor().getUiComponent()).setText("01/11/2026");
        ((JTextField) form.dtpKetThuc.getDateEditor().getUiComponent()).setText("01/12/2026");
    }
    private String validation() throws Exception {
        AtomicReference<String> error = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> error.set(assertThrows(IllegalArgumentException.class, form::docDuLieu).getMessage()));
        return error.get();
    }
    private void waitUntil(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        while (System.nanoTime() < deadline) {
            AtomicBoolean done = new AtomicBoolean(); SwingUtilities.invokeAndWait(() -> done.set(condition.getAsBoolean()));
            if (done.get()) return; Thread.sleep(25);
        }
        fail("Timeout chờ form voucher");
    }

    @Test void maDuKien_taiNen_hienThiKhongChoSua_vaKhongTinMaKhiLuu() throws Exception {
        create(null, false, v -> true);
        SwingUtilities.invokeAndWait(() -> form.taiMaDuKien(() -> {
            assertFalse(SwingUtilities.isEventDispatchThread()); return "VC007";
        }));
        waitUntil(() -> "VC007".equals(form.txtMa.getText()));
        SwingUtilities.invokeAndWait(() -> {
            assertFalse(form.txtMa.isEditable()); valid(); assertNull(form.docDuLieu().getMaVoucher());
        });
    }

    @Test void them_duLieuHopLe_luuNen_vaNhanMaSauLuu() throws Exception {
        create(null, false, v -> {
            assertFalse(SwingUtilities.isEventDispatchThread()); calls.incrementAndGet();
            assertNull(v.getMaVoucher()); assertEquals(12.5, v.getGiaGiam()); assertEquals(50000, v.getGiaTriGiamToiDa());
            v.setMaVoucher("VC003"); return true;
        });
        SwingUtilities.invokeAndWait(() -> { valid(); form.btnLuu.doClick(); });
        waitUntil(() -> saved.get() != null);
        assertEquals(1, calls.get()); assertEquals("VC003", saved.get().getMaVoucher());
    }

    @Test void giaTriSai_baoLoiTaiO_vaKhongGoiDao() throws Exception {
        create(null, false, v -> { calls.incrementAndGet(); return true; });
        SwingUtilities.invokeAndWait(() -> { valid(); form.txtGiaGiam.setText("101"); form.btnLuu.doClick(); });
        assertTrue(validation().contains("không quá 100"));
        SwingUtilities.invokeAndWait(() -> assertEquals("error", form.txtGiaGiam.getClientProperty("JComponent.outline")));
        assertEquals(0, calls.get());
        for (String value : new String[]{"NaN", "Infinity", "-1", "10.123", "100.000", ""}) {
            SwingUtilities.invokeAndWait(() -> form.txtGiaGiam.setText(value));
            assertFalse(validation().isBlank());
        }
    }

    @Test void ngaySai_vaKetThucBangBatDau_biChan() throws Exception {
        create(null, false, v -> true);
        SwingUtilities.invokeAndWait(() -> { valid(); ((JTextField) form.dtpKetThuc.getDateEditor().getUiComponent()).setText("31/02/2026"); });
        assertTrue(validation().contains("ngày hợp lệ"));
        SwingUtilities.invokeAndWait(() -> ((JTextField) form.dtpKetThuc.getDateEditor().getUiComponent()).setText("01/11/2026"));
        assertTrue(validation().contains("sau ngày bắt đầu"));
    }

    @Test void mucToiDa_deTrong_vaLoaiTien_boQuaMucToiDa() throws Exception {
        create(null, false, v -> true);
        SwingUtilities.invokeAndWait(() -> {
            valid(); form.txtToiDa.setText(""); assertEquals(0, form.docDuLieu().getGiaTriGiamToiDa());
            form.cboLoai.setSelectedIndex(0); form.txtGiaGiam.setText("100000"); form.txtToiDa.setText("không phải số");
            assertFalse(form.txtToiDa.isEnabled());
            assertEquals(0, form.docDuLieu().getGiaTriGiamToiDa());
            assertFalse(form.docDuLieu().isLoaiGiam());
        });
    }

    @Test void suaTrenBanSao_huyKhongDoiDoiTuongGoc() throws Exception {
        Voucher source = voucher(); create(source, false, v -> true);
        SwingUtilities.invokeAndWait(() -> {
            form.txtTen.setText("Tên khác"); form.chkHoatDong.setSelected(false);
            Voucher copy = form.docDuLieu();
            assertEquals("VC001", copy.getMaVoucher()); assertFalse(copy.isTrangThai()); form.btnDong.doClick();
        });
        assertEquals("Ưu đãi cũ", source.getTenVoucher()); assertTrue(source.isTrangThai()); assertTrue(closed.get()); assertNull(saved.get());
    }

    @Test void daSuDung_chiDoc_khongLuu() throws Exception {
        create(voucher(), true, v -> { calls.incrementAndGet(); return true; });
        SwingUtilities.invokeAndWait(() -> {
            assertFalse(form.txtTen.isEnabled()); assertFalse(form.cboLoai.isEnabled());
            assertFalse(form.chkHoatDong.isEnabled()); assertFalse(form.btnLuu.isVisible());
            form.btnLuu.doClick(); form.btnDong.doClick();
        });
        assertEquals(0, calls.get()); assertTrue(closed.get());
    }

    @Test void loiDao_giuNhapLieu_choThuLai_vaKhongLoChiTiet() throws Exception {
        create(voucher(), false, v -> {
            if (calls.incrementAndGet() == 1) throw new DataAccessException("secret-sql-server");
            return true;
        });
        SwingUtilities.invokeAndWait(() -> { valid(); form.btnLuu.doClick(); });
        waitUntil(() -> form.btnLuu.isEnabled());
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("Ưu đãi mới", form.txtTen.getText());
            assertFalse(form.lblLoi.getText().contains("secret")); form.btnLuu.doClick();
        });
        waitUntil(() -> saved.get() != null); assertEquals(2, calls.get());
    }

    @Test void loiNghiepVu_hienThongBao_khongThongBaoThanhCong() throws Exception {
        create(voucher(), false, v -> { throw new BusinessException("Voucher đã dùng; chỉ được vô hiệu hóa để giữ lịch sử."); });
        SwingUtilities.invokeAndWait(() -> { valid(); form.btnLuu.doClick(); });
        waitUntil(() -> form.lblLoi.getText().contains("Voucher đã dùng")); assertNull(saved.get());
    }

    @Test void luuCham_chanGuiTrung_vaKhongDongSom() throws Exception {
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        create(null, false, v -> {
            calls.incrementAndGet(); started.countDown();
            try { assertTrue(release.await(5, TimeUnit.SECONDS)); } catch (InterruptedException e) { throw new RuntimeException(e); }
            return true;
        });
        try {
            SwingUtilities.invokeAndWait(() -> { valid(); form.btnLuu.doClick(); });
            assertTrue(started.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                assertFalse(form.txtTen.isEnabled()); assertFalse(form.btnDong.isEnabled());
                form.btnLuu.doClick(); form.yeuCauDong();
            });
            assertFalse(closed.get()); assertEquals(1, calls.get());
        } finally { release.countDown(); }
        waitUntil(() -> saved.get() != null);
    }

    @Test void boCucForm_coDuChoNhapLieu_vaAnhXemTruoc() throws Exception {
        create(voucher(), false, v -> true);
        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame();
            try {
                frame.setContentPane(form); frame.pack(); frame.setSize(580, 670); frame.validate(); layout(frame);
                assertTrue(form.txtTen.getWidth() > 350);
                assertTrue(form.dtpBatDau.getWidth() > 180); assertTrue(form.dtpKetThuc.getWidth() > 180);
                if (Boolean.getBoolean("bclinic.preview")) {
                    var image = new java.awt.image.BufferedImage(form.getWidth(), form.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
                    var graphics = image.createGraphics(); form.printAll(graphics); graphics.dispose();
                    javax.imageio.ImageIO.write(image, "png", java.nio.file.Path.of("target/voucher-form-preview.png").toFile());
                }
            } catch (Exception e) { throw new RuntimeException(e); }
            finally { frame.dispose(); }
        });
    }
    private static void layout(java.awt.Container parent) {
        parent.doLayout(); for (java.awt.Component c : parent.getComponents()) if (c instanceof java.awt.Container child) layout(child);
    }
}
