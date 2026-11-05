package com.quanlynhakhoa.ui.panel;

import com.formdev.flatlaf.FlatLightLaf;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.DataAccessException;
import com.quanlynhakhoa.util.SessionManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class PnlVoucherTest {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 5);
    private static final Clock CLOCK = Clock.fixed(DAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    private PnlVoucher panel;
    private final List<Voucher> data = new ArrayList<>();
    private final Set<String> used = new HashSet<>();
    private final AtomicInteger writes = new AtomicInteger();
    private final AtomicReference<String> opened = new AtomicReference<>();
    private boolean confirmed = true;

    @BeforeEach void setup() {
        NhanVien nv = new NhanVien("NV001", "Người kiểm thử", true, "0900000001", "test@example.com", "Quản lý");
        SessionManager.getInstance().login(new TaiKhoan("TK001", "test.quanly", "x", VaiTro.QUANLY, true, nv));
        data.add(voucher("VC001", "Chăm sóc nụ cười", true, 10, DAY.minusDays(2), DAY.plusDays(10), true));
        data.add(voucher("VC002", "Khách hàng mới", false, 100000, DAY.minusDays(2), DAY.plusDays(10), true));
        data.add(voucher("VC003", "Hè vui", true, 15, DAY.minusMonths(2), DAY.minusDays(1), true));
        data.add(voucher("VC004", "Thành viên", true, 20, DAY.minusDays(2), DAY.plusDays(10), false));
        data.add(voucher("VC005", "Ưu đãi sắp tới", true, 10, DAY.plusDays(1), DAY.plusMonths(1), true));
    }
    @AfterEach void cleanup() throws Exception {
        SwingUtilities.invokeAndWait(() -> { if (panel != null) panel.removeNotify(); });
        SessionManager.getInstance().logout();
    }
    private static Voucher voucher(String id, String name, boolean percent, double value, LocalDate start, LocalDate end, boolean active) {
        return new Voucher(id, name, percent, value, percent ? 50000 : 0, start, end, active);
    }
    private void create(Supplier<PnlVoucher.DuLieu> source) throws Exception {
        SwingUtilities.invokeAndWait(() -> panel = new PnlVoucher(source, v -> true, v -> {
            assertFalse(SwingUtilities.isEventDispatchThread()); writes.incrementAndGet();
            data.stream().filter(x -> x.getMaVoucher().equals(v.getMaVoucher())).findFirst().orElseThrow().setTrangThai(v.isTrangThai()); return true;
        }, id -> {
            assertFalse(SwingUtilities.isEventDispatchThread()); writes.incrementAndGet();
            data.stream().filter(v -> id.equals(v.getMaVoucher())).findFirst().orElseThrow().setTrangThai(false); return true;
        }, CLOCK) {
            @Override boolean xacNhan(String message) { return confirmed; }
            @Override void moForm(Voucher v) { opened.set(v == null ? "new" : v.getMaVoucher()); }
        });
    }
    private void create() throws Exception { create(() -> new PnlVoucher.DuLieu(data, used)); }
    private void load() throws Exception {
        SwingUtilities.invokeAndWait(panel::lamMoiDuLieu); waitUntil(() -> panel.btnThem.isEnabled());
    }
    private void waitUntil(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        while (System.nanoTime() < deadline) {
            AtomicBoolean done = new AtomicBoolean(); SwingUtilities.invokeAndWait(() -> done.set(condition.getAsBoolean()));
            if (done.get()) return; Thread.sleep(25);
        }
        fail("Timeout chờ panel voucher");
    }

    @Test void locKhongDau_loai_vaNamTrangThai() throws Exception {
        create(); load();
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(5, panel.tblVoucher.getRowCount());
            panel.txtTim.setText("cham soc"); panel.txtTim.postActionEvent(); assertEquals(1, panel.tblVoucher.getRowCount());
            panel.txtTim.setText(""); panel.cboLoai.setSelectedIndex(0); panel.btnTab[0].doClick(); panel.cboLoai.setSelectedIndex(2); assertEquals(1, panel.tblVoucher.getRowCount());
            panel.txtTim.setText(""); panel.cboLoai.setSelectedIndex(0); panel.btnTab[0].doClick();
            int[] counts = {5, 2, 1, 1, 1};
            for (int i = 0; i < counts.length; i++) { panel.btnTab[i].doClick(); assertEquals(counts[i], panel.tblVoucher.getRowCount()); }
            panel.txtTim.setText(""); panel.cboLoai.setSelectedIndex(0); panel.btnTab[0].doClick(); panel.txtTim.setText("không tồn tại"); panel.txtTim.postActionEvent();
            assertEquals(0, panel.tblVoucher.getRowCount()); assertFalse(panel.btnXuat.isEnabled());
        });
    }

    @Test void hieuLucHaiDau_vaKhoaUuTienHonHetHan() {
        Voucher v = data.get(0);
        assertEquals(1, PnlVoucher.trangThai(v, v.getNgayBatDau()));
        assertEquals(1, PnlVoucher.trangThai(v, v.getNgayKetThuc()));
        v.setTrangThai(false); assertEquals(4, PnlVoucher.trangThai(v, DAY.plusYears(1)));
    }

    @Test void phanTrang_vaChiTietSauSapXep_dungMa() throws Exception {
        for (int i = 6; i <= 19; i++) data.add(voucher(String.format("VC%03d", i), "Voucher " + i, true, 10, DAY, DAY.plusDays(5), true));
        create(); load();
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(8, panel.tblVoucher.getRowCount()); panel.btnSau.doClick(); assertEquals("2", panel.txtTrang.getText()); assertEquals("/ 3", panel.lblTrang.getText());
            panel.btnSau.doClick(); assertEquals(3, panel.tblVoucher.getRowCount()); assertFalse(panel.btnSau.isEnabled());
            panel.btnTruoc.doClick(); panel.btnTruoc.doClick();
            panel.tblVoucher.getRowSorter().toggleSortOrder(0); panel.tblVoucher.getRowSorter().toggleSortOrder(0);
            Rectangle cell = panel.tblVoucher.getCellRect(0, 0, true);
            panel.tblVoucher.dispatchEvent(new MouseEvent(panel.tblVoucher, MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(), 0, cell.x + 8, cell.y + 8, 2, false, MouseEvent.BUTTON1));
            assertEquals("VC008", opened.get());
            panel.btnTab[3].doClick(); assertEquals("1", panel.txtTrang.getText()); assertEquals("/ 1", panel.lblTrang.getText());
        });
    }

    @Test void nhapTrang_enterChuyenTrang_chanSoSai_vaDongBoKhiLoc() throws Exception {
        for (int i = 6; i <= 19; i++) data.add(voucher(String.format("VC%03d", i), "Voucher " + i, true, 10, DAY, DAY.plusDays(5), true));
        create(); load();
        SwingUtilities.invokeAndWait(() -> {
            panel.txtTrang.setText("3"); panel.txtTrang.postActionEvent();
            assertEquals(3, panel.tblVoucher.getRowCount());
            assertEquals("VC017", ((Voucher) panel.tblVoucher.getValueAt(0, 0)).getMaVoucher());
            assertFalse(panel.btnSau.isEnabled());
            for (String invalid : new String[]{"0", "4", "-1", "abc", "1.5", "", "999999999999999999999"}) {
                panel.txtTrang.setText(invalid); panel.txtTrang.postActionEvent();
                assertEquals(3, panel.tblVoucher.getRowCount());
                assertEquals("error", panel.txtTrang.getClientProperty("JComponent.outline"));
            }
            panel.txtTrang.setText(" 2 "); panel.txtTrang.postActionEvent();
            assertEquals("2", panel.txtTrang.getText()); assertEquals(8, panel.tblVoucher.getRowCount());
            assertNull(panel.txtTrang.getClientProperty("JComponent.outline"));
            panel.btnTab[3].doClick(); assertEquals("1", panel.txtTrang.getText()); assertEquals("/ 1", panel.lblTrang.getText());
            panel.txtTim.setText("không có"); panel.txtTim.postActionEvent(); assertFalse(panel.txtTrang.isEnabled());
        });
    }

    @Test void khoa_huyXacNhanKhongGhi_xacNhanLuuNen_vaTaiLai() throws Exception {
        create(); load(); confirmed = false;
        SwingUtilities.invokeAndWait(() -> panel.doiTrangThai(data.get(0))); assertEquals(0, writes.get());
        confirmed = true; SwingUtilities.invokeAndWait(() -> panel.doiTrangThai(data.get(0)));
        waitUntil(() -> panel.btnThem.isEnabled() && writes.get() == 1);
        assertFalse(data.get(0).isTrangThai());
        SwingUtilities.invokeAndWait(() -> panel.btnTab[4].doClick());
        SwingUtilities.invokeAndWait(() -> assertEquals(2, panel.tblVoucher.getRowCount()));
    }

    @Test void moKhoa_chiVoucherChuaDung() throws Exception {
        used.add("VC001"); data.get(0).setTrangThai(false); create(); load();
        SwingUtilities.invokeAndWait(() -> panel.doiTrangThai(data.get(0))); assertEquals(0, writes.get());
        SwingUtilities.invokeAndWait(() -> panel.doiTrangThai(data.get(3)));
        waitUntil(() -> panel.btnThem.isEnabled() && writes.get() == 1); assertTrue(data.get(3).isTrangThai());
    }

    @Test void loiTaiDuLieu_xoaDuLieuCu_khongLoLoiDb() throws Exception {
        AtomicBoolean fail = new AtomicBoolean();
        create(() -> { if (fail.get()) throw new DataAccessException("secret-sql"); return new PnlVoucher.DuLieu(data, used); });
        load(); fail.set(true); SwingUtilities.invokeAndWait(panel::lamMoiDuLieu);
        waitUntil(() -> panel.lblKetQua.getText().contains("Không thể tải"));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(0, panel.tblVoucher.getRowCount()); assertFalse(panel.btnThem.isEnabled()); assertFalse(panel.btnXuat.isEnabled());
            assertFalse(panel.lblKetQua.getText().contains("secret"));
        });
    }

    @Test void khongPhaiQuanLy_khongGoiNguonDuLieu() throws Exception {
        AtomicInteger reads = new AtomicInteger(); create(() -> { reads.incrementAndGet(); return new PnlVoucher.DuLieu(data, used); });
        SessionManager.getInstance().getCurrentUser().setVaiTro(VaiTro.LETAN);
        SwingUtilities.invokeAndWait(() -> { panel.lamMoiDuLieu(); assertFalse(panel.btnThem.isEnabled()); });
        assertEquals(0, reads.get());
    }

    @Test void xuatExcel_dungSnapshot_vaChuoiKhongTroThanhFormula(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        data.get(0).setTenVoucher("=1+1"); Path file = dir.resolve("voucher.xlsx");
        PnlVoucher.ghiExcel(file, data, DAY);
        try (var input = Files.newInputStream(file); var book = new XSSFWorkbook(input)) {
            var sheet = book.getSheetAt(0); assertEquals(5, sheet.getLastRowNum());
            assertEquals("=1+1", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals(org.apache.poi.ss.usermodel.CellType.STRING, sheet.getRow(1).getCell(1).getCellType());
            assertEquals(100000, sheet.getRow(2).getCell(3).getNumericCellValue());
            assertEquals("Chưa hiệu lực", sheet.getRow(5).getCell(7).getStringCellValue());
        }
    }

    @Test void boCuc_1366x768_vaAnhXemTruoc() throws Exception {
        SwingUtilities.invokeAndWait(FlatLightLaf::setup); create(); load();
        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame();
            try {
                frame.setContentPane(panel); frame.pack(); frame.setSize(1110, 660); frame.validate(); layout(frame);
                if (Boolean.getBoolean("bclinic.preview")) {
                    BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = image.createGraphics(); panel.printAll(g); g.dispose();
                    ImageIO.write(image, "png", Path.of("target/voucher-preview.png").toFile());
                }
                assertTrue(panel.txtTim.getWidth() > 200); assertTrue(panel.cboLoai.getWidth() >= 150);
                assertTrue(panel.tblVoucher.getParent().getHeight() > 200,
                        "Panel=" + panel.getSize() + ", viewport=" + panel.tblVoucher.getParent().getSize());
            } catch (Exception e) { throw new RuntimeException(e); }
            finally { frame.dispose(); }
        });
    }
    private static void layout(Container parent) { parent.doLayout(); for (Component c : parent.getComponents()) if (c instanceof Container child) layout(child); }
}
