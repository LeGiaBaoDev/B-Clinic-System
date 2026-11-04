package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.entity.NhanVien;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.util.SessionManager;
import java.awt.Component;
import java.awt.Container;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PnlNhanVienTest {

    private PnlNhanVien panel;
    private final NhanVien an = new NhanVien("NV001", "An <Quản lý>", true,
            "0912345678", "an@example.com", "Quản lý");
    private final NhanVien binh = new NhanVien("NV002", "Bình Nha sĩ", false,
            "0987654321", "binh@example.com", "Nha sĩ");

    @BeforeEach
    void dangNhapQuanLy() {
        SessionManager.getInstance().login(taiKhoan(an, VaiTro.QUANLY, true));
    }

    @AfterEach
    void donDep() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (panel != null) { panel.removeNotify(); }
        });
        SessionManager.getInstance().logout();
    }

    @Test
    void taiDuLieu_chayNen_hienTaiKhoanVaNhanVienChuaCoTaiKhoan() throws Exception {
        AtomicBoolean ngoaiEdt = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> {
            panel = new PnlNhanVien(keyword -> {
                ngoaiEdt.set(!SwingUtilities.isEventDispatchThread());
                return List.of(an, binh);
            }, () -> List.of(taiKhoan(an, VaiTro.QUANLY, true)));
            panel.lamMoiDuLieu();
        });
        choKetQua("Tìm thấy 2");
        assertTrue(ngoaiEdt.get());
        SwingUtilities.invokeAndWait(() -> {
            JTable table = (JTable) tim("danhSachNhanVien");
            assertEquals("******5678", table.getValueAt(0, 2));
            assertEquals("quanly", table.getValueAt(0, 4));
            assertEquals("Quản lý", table.getValueAt(0, 5));
            assertEquals("Chưa có tài khoản", table.getValueAt(1, 6));
            assertFalse(table.isCellEditable(0, 0));
            JButton edit = (JButton) tim("capNhatNhanVien");
            assertFalse(edit.isEnabled());
            table.setRowSelectionInterval(0, 0);
            assertTrue(edit.isEnabled());
            table.setRowSelectionInterval(1, 1);
            assertFalse(edit.isEnabled());
            table.clearSelection();
            assertFalse(edit.isEnabled());
        });
    }

    @Test
    void chonDongSauSapXep_hienDungNguoi_vaEscapeHtml() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            panel = new PnlNhanVien(keyword -> List.of(an, binh),
                    () -> List.of(taiKhoan(binh, VaiTro.NHASI, false)));
            panel.lamMoiDuLieu();
        });
        choKetQua("Tìm thấy 2");
        SwingUtilities.invokeAndWait(() -> {
            JTable table = (JTable) tim("danhSachNhanVien");
            table.getRowSorter().toggleSortOrder(0);
            table.getRowSorter().toggleSortOrder(0);
            table.setRowSelectionInterval(0, 0);
            assertEquals("Bình Nha sĩ", ((JLabel) tim("chiTietNhanVien")).getText());
            assertTrue(((JLabel) tim("chiTietMetaNhanVien")).getText().contains("binh@example.com"));
            assertEquals("Đã khóa", ((JLabel) tim("chiTietTrangThaiNhanVien")).getText());
            table.setRowSelectionInterval(1, 1);
            assertEquals("An <Quản lý>", ((JLabel) tim("chiTietNhanVien")).getText());
        });
    }

    @Test
    void timKiem_truyenDungTuKhoa_vaHienKetQuaRong() throws Exception {
        AtomicReference<String> query = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            panel = new PnlNhanVien(keyword -> { query.set(keyword); return List.of(); }, List::of);
            JTextField search = (JTextField) tim("timNhanVien");
            search.setText("  0912  ");
            search.postActionEvent();
        });
        choKetQua("Không tìm thấy");
        assertEquals("0912", query.get());
        SwingUtilities.invokeAndWait(() ->
                assertEquals(0, ((JTable) tim("danhSachNhanVien")).getRowCount()));
    }

    @Test
    void loiTaiDuLieu_hienThongBaoChung_khongLoLoiHeThong() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            panel = new PnlNhanVien(keyword -> List.of(an),
                    () -> { throw new IllegalStateException("secret-system-detail"); });
            panel.lamMoiDuLieu();
        });
        choKetQua("Không thể tải danh sách");
        SwingUtilities.invokeAndWait(() -> {
            assertFalse(((JLabel) tim("ketQuaNhanVien")).getText().contains("secret-system-detail"));
            assertEquals(0, ((JTable) tim("danhSachNhanVien")).getRowCount());
            assertTrue(tim("lamMoiNhanVien").isEnabled());
        });
    }

    @Test
    void khongPhaiQuanLy_khongGoiNguonDuLieu() throws Exception {
        AtomicBoolean called = new AtomicBoolean();
        SessionManager.getInstance().login(taiKhoan(binh, VaiTro.NHASI, true));
        SwingUtilities.invokeAndWait(() -> {
            panel = new PnlNhanVien(keyword -> { called.set(true); return List.of(an); },
                    () -> { called.set(true); return List.of(); });
            panel.lamMoiDuLieu();
            assertTrue(((JLabel) tim("ketQuaNhanVien")).getText().contains("Chỉ Quản lý"));
        });
        assertFalse(called.get());
    }

    private TaiKhoan taiKhoan(NhanVien nv, VaiTro vaiTro, boolean active) {
        return new TaiKhoan(nv == an ? "TK001" : "TK002", nv == an ? "quanly" : "nhasi",
                "[Ẩn]", vaiTro, active, nv);
    }

    private void choKetQua(String expected) throws Exception {
        long timeout = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < timeout) {
            AtomicBoolean ready = new AtomicBoolean();
            SwingUtilities.invokeAndWait(() -> ready.set(tim("lamMoiNhanVien").isEnabled()
                    && ((JLabel) tim("ketQuaNhanVien")).getText().contains(expected)));
            if (ready.get()) { return; }
            Thread.sleep(20);
        }
        fail("Không nhận được kết quả: " + expected);
    }

    private Component tim(String name) {
        Component found = timTrong(panel, name);
        if (found == null) { throw new AssertionError("Thiếu component " + name); }
        return found;
    }

    private Component timTrong(Component component, String name) {
        if (name.equals(component.getName())) { return component; }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                Component found = timTrong(child, name);
                if (found != null) { return found; }
            }
        }
        return null;
    }
}
