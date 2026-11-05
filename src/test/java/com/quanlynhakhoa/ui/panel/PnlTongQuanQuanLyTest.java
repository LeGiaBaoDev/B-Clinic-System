package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.dao.TongQuanQuanLyDAO.*;
import com.quanlynhakhoa.entity.enums.TrangThaiLichHen;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class PnlTongQuanQuanLyTest {
    private PnlTongQuanQuanLy panel;
    private final AtomicReference<PnlTongQuanQuanLy.TrangThaiTai> state = new AtomicReference<>();
    private final LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));

    private void create(Function<LocalDate, TongQuanQuanLy> load) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            panel = new PnlTongQuanQuanLy("Kiểm thử", s -> {}, load);
            panel.langNgheTaiDuLieu(state::set);
        });
    }

    @AfterEach void stop() throws Exception {
        if (panel != null) SwingUtilities.invokeAndWait(panel::removeNotify);
    }

    private TongQuanQuanLy data(LocalDate day, int count) {
        return new TongQuanQuanLy(day, today.atTime(10, 0), count, count, 0, 0, 0, 0,
                1, new BigDecimal("200000"), 1, new BigDecimal("300000"),
                BigDecimal.ZERO, BigDecimal.ZERO, List.of(),
                List.of(new LichHenTrongNgay("LH261105001", day.atTime(11, 0), "Bệnh nhân", "Nha sĩ", TrangThaiLichHen.DA_DAT, null)),
                List.of(new NhaSiHienTai("NV002", "Nha sĩ", true, false, null, null, null, null)), null);
    }

    private void await(LocalDate day) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        while (System.nanoTime() < end) {
            SwingUtilities.invokeAndWait(() -> {});
            var s = state.get();
            if (s != null && !s.dangTai() && s.duLieu() != null && day.equals(s.duLieu().ngayXem())) return;
            Thread.sleep(20);
        }
        fail("Không tải xong ngày " + day + ": " + state.get());
    }

    @Test void tongQuan_chiHomNay_vaNutLamMoi() throws Exception {
        java.util.concurrent.atomic.AtomicInteger loads = new java.util.concurrent.atomic.AtomicInteger();
        create(day -> { assertEquals(today, day); loads.incrementAndGet(); return data(day, 3); });
        SwingUtilities.invokeAndWait(panel::lamMoiDuLieu); await(today);
        SwingUtilities.invokeAndWait(() -> {
            assertNull(find(panel, "ngayXemTongQuan"));
            assertNull(find(panel, "homNayTongQuan"));
            JButton button = (JButton) find(panel, "lamMoiTongQuan");
            assertEquals("Làm mới", button.getText());
            assertTrue(button.isContentAreaFilled());
            assertTrue(text(panel).contains("Hóa đơn lập hôm nay"));
            button.doClick();
        });
        await(today);
        assertEquals(2, loads.get());
    }

    @Test void lamMoiGapLoi_giuSnapshotVaMoLaiNut() throws Exception {
        java.util.concurrent.atomic.AtomicBoolean fail = new java.util.concurrent.atomic.AtomicBoolean();
        create(day -> { if (fail.get()) throw new IllegalStateException("private-sql-detail"); return data(day, 3); });
        SwingUtilities.invokeAndWait(panel::lamMoiDuLieu); await(today);
        fail.set(true);
        SwingUtilities.invokeAndWait(() -> ((JButton) find(panel, "lamMoiTongQuan")).doClick());
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while ((state.get() == null || state.get().dangTai()) && System.nanoTime() < end) Thread.sleep(20);
        assertNotNull(state.get().loi());
        assertNotNull(state.get().duLieu());
        assertFalse(state.get().loi().contains("private-sql-detail"));
        SwingUtilities.invokeAndWait(() -> assertTrue(find(panel, "lamMoiTongQuan").isEnabled()));
    }

    @Test void homNay_vaRender1366x768() throws Exception {
        create(day -> data(day, 1));
        SwingUtilities.invokeAndWait(panel::lamMoiDuLieu); await(today);
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(panel.coLichDangCho());
            panel.setSize(1158, 612);
            layout(panel);
            if (Boolean.getBoolean("bclinic.preview")) {
                BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics(); panel.printAll(g); g.dispose();
                try { ImageIO.write(image, "png", Path.of("target/h4-dashboard-preview.png").toFile()); }
                catch (Exception e) { throw new RuntimeException(e); }
            }
        });
    }

    private static void layout(Container c) { c.doLayout(); for (Component child : c.getComponents()) if (child instanceof Container next) layout(next); }
    private static Component find(Container c, String name) {
        for (Component child : c.getComponents()) {
            if (name.equals(child.getName())) return child;
            if (child instanceof Container next) { Component result = find(next, name); if (result != null) return result; }
        }
        return null;
    }
    private static String text(Container c) {
        StringBuilder result = new StringBuilder();
        for (Component child : c.getComponents()) {
            if (child instanceof JLabel label) result.append(label.getText()).append('\n');
            if (child instanceof Container next) result.append(text(next));
        }
        return result.toString();
    }
}
