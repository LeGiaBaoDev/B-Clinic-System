package com.quanlynhakhoa.ui.panel;

import com.formdev.flatlaf.FlatLightLaf;
import com.quanlynhakhoa.dao.BaoCaoQuanLyDAO.*;
import com.quanlynhakhoa.exception.DataAccessException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Function;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class PnlBaoCaoQuanLyTest {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 5);
    private static final Clock CLOCK = Clock.fixed(DAY.atTime(10, 0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(), ZoneOffset.UTC);
    private PnlBaoCaoQuanLy panel;
    private final AtomicReference<KyBaoCao> requested = new AtomicReference<>();

    private void create(Function<KyBaoCao, BaoCao> source) throws Exception {
        SwingUtilities.invokeAndWait(() -> { FlatLightLaf.setup(); panel = new PnlBaoCaoQuanLy(ky -> { assertFalse(SwingUtilities.isEventDispatchThread()); requested.set(ky); return source.apply(ky); }, CLOCK); panel.lamMoiDuLieu(); });
    }
    @AfterEach void stop() throws Exception { if (panel != null) SwingUtilities.invokeAndWait(panel::removeNotify); }
    private static List<DoanhThuNgay> days(KhoangNgay range, boolean previous) {
        long[] amounts = previous ? new long[]{6500000,12400000,15200000,14400000,16680000,0,0} : new long[]{7200000,13800000,17400000,16400000,18450000,0,0};
        List<DoanhThuNgay> result = new ArrayList<>();
        for (int i = 0; i < range.soNgay(); i++) { BigDecimal value = BigDecimal.valueOf(amounts[i % amounts.length]); result.add(new DoanhThuNgay(range.tu().plusDays(i), value.signum() > 0 ? 1 : 0, value, value.divide(BigDecimal.TWO), value.divide(BigDecimal.TWO))); }
        return result;
    }
    private static BaoCao data(KyBaoCao ky) {
        return new BaoCao(ky, DAY.atTime(10, 0), days(ky.hienTai(), false), days(ky.kyTruoc(), true),
                new KetQuaLich(90,3,2,74,8,3), new KetQuaLich(80,0,0,66,10,4),
                List.of(new DichVuSuDung("DV001", "Cạo vôi răng",28,new BigDecimal("8400000")),
                        new DichVuSuDung("DV002", "Trám răng",18,new BigDecimal("9000000")),
                        new DichVuSuDung("DV003", "Khám tổng quát",16,new BigDecimal("2400000")),
                        new DichVuSuDung("DV004", "Tẩy trắng răng",6,new BigDecimal("12000000")),
                        new DichVuSuDung("DV005", "Nhổ răng sữa",6,new BigDecimal("1200000"))));
    }
    private void ready() throws Exception { waitFor(() -> component("xuatBaoCao").isEnabled()); }
    private void waitFor(java.util.function.BooleanSupplier condition) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        while (System.nanoTime() < end) { AtomicBoolean okay = new AtomicBoolean(); SwingUtilities.invokeAndWait(() -> okay.set(condition.getAsBoolean())); if (okay.get()) return; Thread.sleep(20); }
        fail("UI không đạt điều kiện trong 8 giây");
    }
    private Component component(String name) { return find(panel, name); }
    private void click(String name) throws Exception { SwingUtilities.invokeAndWait(() -> ((AbstractButton) component(name)).doClick()); }

    @Test void macDinhTuan_ngayThangVaTuyChon_dungKy() throws Exception {
        create(PnlBaoCaoQuanLyTest::data); ready();
        assertEquals(LocalDate.of(2026,11,2), requested.get().hienTai().tu());
        click("kyBaoCaoNGAY"); ready(); assertEquals(DAY, requested.get().hienTai().tu());
        click("kyBaoCaoTHANG"); ready(); assertEquals(LocalDate.of(2026,11,30), requested.get().hienTai().den());
        click("kyBaoCaoTUY_CHON");
        SwingUtilities.invokeAndWait(() -> {
            assertFalse(component("xuatBaoCao").isEnabled());
            setDate("tuNgayBaoCao", DAY.minusDays(2)); setDate("denNgayBaoCao", DAY);
        });
        click("xemBaoCao"); ready(); assertEquals(3, requested.get().hienTai().soNgay());
        SwingUtilities.invokeAndWait(() -> { setDate("denNgayBaoCao", DAY.minusDays(5)); });
        click("xemBaoCao");
        SwingUtilities.invokeAndWait(() -> { assertFalse(component("xuatBaoCao").isEnabled()); assertTrue(text(panel).contains("Khoảng ngày không hợp lệ")); });
    }

    @Test void doiKyKetQuaCu_khongGhiDe_nguonKhongChayEdt() throws Exception {
        CountDownLatch start = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicInteger loads = new AtomicInteger();
        create(ky -> { if (loads.incrementAndGet() == 1) { start.countDown(); try { release.await(4, TimeUnit.SECONDS); } catch (InterruptedException ignored) { } } return data(ky); });
        try { assertTrue(start.await(3, TimeUnit.SECONDS)); click("kyBaoCaoNGAY"); ready(); release.countDown();
            SwingUtilities.invokeAndWait(() -> assertTrue(text(panel).contains("05/11/2026 – 05/11/2026")));
        } finally { release.countDown(); }
    }

    @Test void loiTaiKhongLoSql_vaThuLaiDuoc() throws Exception {
        AtomicBoolean fail = new AtomicBoolean(true);
        create(ky -> { if (fail.get()) throw new DataAccessException("private-db-detail"); return data(ky); });
        waitFor(() -> text(panel).contains("Không tải được báo cáo."));
        SwingUtilities.invokeAndWait(() -> { assertFalse(text(panel).contains("private-db-detail")); assertFalse(component("xuatBaoCao").isEnabled()); });
        fail.set(false); click("xemBaoCao"); ready();
    }

    @Test void exportSnapshot_soTienDayDu_vaChuoiKhongThanhFormula() throws Exception {
        var ky = KyBaoCao.tao(CheDo.TUAN, DAY, null); var source = data(ky);
        var report = new BaoCao(ky, source.capNhatLuc(), source.hienTai(), source.kyTruoc(), source.lichHienTai(), source.lichKyTruoc(),
                List.of(new DichVuSuDung("DV001", "=1+1", 2, new BigDecimal("123456"))));
        Path directory = Files.createTempDirectory(Path.of("target"), "baocao-export-"); Path output = directory.resolve("report.xlsx");
        try {
            PnlBaoCaoQuanLy.xuatExcel(report, output);
            try (var book = new XSSFWorkbook(Files.newInputStream(output))) {
                assertEquals(4, book.getNumberOfSheets());
                assertEquals(73250000, book.getSheet("Tổng hợp").getRow(3).getCell(2).getNumericCellValue());
                assertEquals(7, book.getSheet("Doanh thu kỳ này").getLastRowNum());
                var cell = book.getSheet("Dịch vụ").getRow(1).getCell(1);
                assertEquals(CellType.STRING, cell.getCellType()); assertEquals("=1+1", cell.getStringCellValue());
            }
            try (var list = Files.list(directory)) { assertEquals(1, list.count()); }
        } finally { Files.deleteIfExists(output); Files.deleteIfExists(directory); }
    }

    @Test void render_1366x768_vaBangKhongSuaDuoc() throws Exception {
        create(PnlBaoCaoQuanLyTest::data); ready();
        SwingUtilities.invokeAndWait(() -> {
            JTable table = (JTable) component("dichVuBaoCao"); assertEquals(5, table.getRowCount()); assertFalse(table.isCellEditable(0, 1));
            panel.setSize(1158, 612); layout(panel);
            if (Boolean.getBoolean("bclinic.preview")) {
                // Render full scroll body as well as the visible viewport for QA.
                try { render(panel, "target/baocao-viewport.png");
                    JScrollPane scroll = (JScrollPane) panel.getComponent(0); JComponent body = (JComponent) scroll.getViewport().getView();
                    body.setSize(1135, 900); layout(body); render(body, "target/baocao-full.png");
                } catch (Exception e) { throw new RuntimeException(e); }
            }
        });
    }
    private void setDate(String name, LocalDate day) { ((com.toedter.calendar.JDateChooser) component(name)).setDate(Date.from(day.atStartOfDay(ZoneId.systemDefault()).toInstant())); }
    private static void render(JComponent c, String output) throws Exception { BufferedImage image = new BufferedImage(c.getWidth(),c.getHeight(),BufferedImage.TYPE_INT_RGB); Graphics2D g = image.createGraphics(); c.printAll(g); g.dispose(); ImageIO.write(image,"png",Path.of(output).toFile()); }
    private static void layout(Container c) { c.doLayout(); for (Component child : c.getComponents()) if (child instanceof Container next) layout(next); }
    private static Component find(Container c, String name) { for (Component child : c.getComponents()) { if (name.equals(child.getName())) return child; if (child instanceof Container next) { Component result = find(next,name); if(result!=null)return result; } } return null; }
    private static String text(Container c) { StringBuilder s = new StringBuilder(); for(Component child:c.getComponents()){if(child instanceof JLabel l)s.append(l.getText()).append('\n');if(child instanceof Container next)s.append(text(next));}return s.toString(); }
}
