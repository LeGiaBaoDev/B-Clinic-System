package com.quanlynhakhoa.ui.panel;

import static org.junit.jupiter.api.Assertions.*;

import com.quanlynhakhoa.entity.DichVu;
import com.quanlynhakhoa.entity.LoaiDichVu;
import com.quanlynhakhoa.entity.NhanVien;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.MucThue;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.util.SessionManager;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/*
 * UC006 – PnlDichVu (không cần DB, nguồn dữ liệu giả)
 * [x] mặc định lọc "Đang cung cấp", mọi phân loại; truy vấn chạy ngoài EDT
 * [x] hiển thị đơn giá #,##0 ₫, thuế KCT/5%/10%, thời lượng phút, trạng thái
 * [x] đổi bộ lọc trạng thái / phân loại thuế gửi đúng điều kiện cho DAO
 * [x] không có kết quả -> "Không tìm thấy dịch vụ phù hợp."
 * [x] chọn dòng sau khi sắp xếp hiện đúng lộ trình, escape HTML
 * [x] lỗi nghiệp vụ hiện thông báo DAO; lỗi DB hiện thông báo chung
 * [x] "Làm mới" đưa bộ lọc về mặc định
 * UC005: nút "Thêm dịch vụ" chỉ hiện với Quản lý
 */
class PnlDichVuTest {

    private record DieuKien(String tuKhoa, Boolean trangThai, MucThue mucThue) { }

    private PnlDichVu panel;
    private final List<DieuKien> daGoi = new ArrayList<>();
    private final LoaiDichVu dieuTri = new LoaiDichVu("LDV01", LoaiDichVu.DIEU_TRI, MucThue.THUE_0);
    private final LoaiDichVu thamMy = new LoaiDichVu("LDV03", LoaiDichVu.THAM_MY, MucThue.THUE_10);
    private final DichVu caoVoi = new DichVu("DV001", "Cạo vôi răng", 300_000, 30, null, true, dieuTri);
    private final DichVu niengRang = new DichVu("DV002", "Niềng răng <mắc cài>", 25_000_000, 60,
            "Buổi 1 khám\nBuổi 2 gắn mắc cài sau 7 ngày", false, thamMy);

    @AfterEach
    void donDep() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (panel != null) {
                panel.removeNotify();
            }
        });
    }

    private void taoPanel(PnlDichVu.TimDichVu nguon) throws Exception {
        SwingUtilities.invokeAndWait(() -> panel = new PnlDichVu((tuKhoa, trangThai, mucThue) -> {
            synchronized (daGoi) {
                daGoi.add(new DieuKien(tuKhoa, trangThai, mucThue));
            }
            return nguon.tim(tuKhoa, trangThai, mucThue);
        }));
    }

    @Test
    void taiLanDau_macDinhDangCungCap_chayNgoaiEdt_vaDinhDangCot() throws Exception {
        AtomicBoolean ngoaiEdt = new AtomicBoolean();
        taoPanel((k, t, m) -> {
            ngoaiEdt.set(!SwingUtilities.isEventDispatchThread());
            return List.of(caoVoi, niengRang);
        });
        SwingUtilities.invokeAndWait(panel::lamMoiDuLieu);
        choKetQua("Tìm thấy 2");

        assertTrue(ngoaiEdt.get());
        assertEquals(new DieuKien("", true, null), daGoi.getLast());
        SwingUtilities.invokeAndWait(() -> {
            JTable table = (JTable) tim("danhSachDichVu");
            assertFalse(table.isCellEditable(0, 0));
            assertEquals("Cạo vôi răng", table.getValueAt(0, 1));
            assertEquals(LoaiDichVu.DIEU_TRI, table.getValueAt(0, 2));
            assertEquals("KCT", table.getValueAt(0, 4));
            assertEquals("10%", table.getValueAt(1, 4));
            assertEquals("Đang cung cấp", table.getValueAt(0, 6));
            assertEquals("Ngừng cung cấp", table.getValueAt(1, 6));
            assertEquals("300.000 ₫", hienThi(table, 0, 3));
            assertEquals("30 phút", hienThi(table, 0, 5));
        });
    }

    @Test
    void doiBoLoc_guiDungTrangThaiVaMucThue() throws Exception {
        taoPanel((k, t, m) -> List.of(niengRang));
        SwingUtilities.invokeAndWait(() -> {
            ((JTextField) tim("timDichVu")).setText("  răng  ");
            ((JComboBox<?>) tim("locTrangThai")).setSelectedIndex(1);
        });
        choKetQua("Tìm thấy 1");
        assertEquals(new DieuKien("răng", false, null), daGoi.getLast());

        SwingUtilities.invokeAndWait(() -> ((JComboBox<?>) tim("locPhanLoaiThue")).setSelectedIndex(3));
        choKetQua("Tìm thấy 1");
        assertEquals(new DieuKien("răng", false, MucThue.THUE_10), daGoi.getLast());

        SwingUtilities.invokeAndWait(() -> ((JComboBox<?>) tim("locTrangThai")).setSelectedIndex(2));
        choKetQua("Tìm thấy 1");
        assertEquals(new DieuKien("răng", null, MucThue.THUE_10), daGoi.getLast());
    }

    @Test
    void khongCoKetQua_hienThongBaoTheoBaoCao() throws Exception {
        taoPanel((k, t, m) -> List.of());
        SwingUtilities.invokeAndWait(panel::lamMoiDuLieu);
        choKetQua("Không tìm thấy dịch vụ phù hợp.");
        SwingUtilities.invokeAndWait(() -> assertEquals(0, ((JTable) tim("danhSachDichVu")).getRowCount()));
    }

    @Test
    void chonDongSauSapXep_hienDungLoTrinh_vaEscapeHtml() throws Exception {
        taoPanel((k, t, m) -> List.of(caoVoi, niengRang));
        SwingUtilities.invokeAndWait(panel::lamMoiDuLieu);
        choKetQua("Tìm thấy 2");
        SwingUtilities.invokeAndWait(() -> {
            JTable table = (JTable) tim("danhSachDichVu");
            table.getRowSorter().toggleSortOrder(0);
            table.getRowSorter().toggleSortOrder(0); // DV002 lên đầu
            table.setRowSelectionInterval(0, 0);
            String chiTiet = ((JLabel) tim("chiTietDichVu")).getText();
            assertTrue(chiTiet.contains("Niềng răng &lt;mắc cài&gt;"));
            assertTrue(chiTiet.contains("Lộ trình điều trị:<br>Buổi 1 khám<br>Buổi 2"),
                    "Tiêu đề lộ trình xuống dòng rồi mới liệt kê từng buổi");
            table.getSelectionModel().setSelectionInterval(1, 1);
            assertTrue(((JLabel) tim("chiTietDichVu")).getText()
                    .contains("Lộ trình điều trị: Không có"));
        });
    }

    @Test
    void loiNghiepVu_hienThongBaoDao_loiDb_hienThongBaoChung() throws Exception {
        taoPanel((k, t, m) -> {
            throw new BusinessException("Vui lòng đăng nhập.");
        });
        SwingUtilities.invokeAndWait(panel::lamMoiDuLieu);
        choKetQua("Vui lòng đăng nhập.");

        taoPanel((k, t, m) -> {
            throw new DataAccessException("SQL timeout chi tiết kỹ thuật", null);
        });
        SwingUtilities.invokeAndWait(panel::lamMoiDuLieu);
        choKetQua("Không thể tải danh mục dịch vụ. Vui lòng thử lại.");
    }

    @Test
    void lamMoi_duaBoLocVeMacDinh_vaChiGoiMotLan() throws Exception {
        taoPanel((k, t, m) -> List.of(caoVoi));
        SwingUtilities.invokeAndWait(() -> {
            ((JTextField) tim("timDichVu")).setText("abc");
            ((JComboBox<?>) tim("locPhanLoaiThue")).setSelectedIndex(2);
        });
        choKetQua("Tìm thấy 1");
        int truoc = daGoi.size();

        SwingUtilities.invokeAndWait(() -> ((JButton) tim("lamMoiDichVu")).doClick());
        choKetQua("Tìm thấy 1");
        assertEquals(truoc + 1, daGoi.size(), "Đặt lại 3 điều kiện chỉ tải một lần");
        assertEquals(new DieuKien("", true, null), daGoi.getLast());
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("", ((JTextField) tim("timDichVu")).getText());
            assertEquals(0, ((JComboBox<?>) tim("locTrangThai")).getSelectedIndex());
            assertEquals(0, ((JComboBox<?>) tim("locPhanLoaiThue")).getSelectedIndex());
        });
    }

    @Test
    void nutThemDichVu_chiHienVoiQuanLy() throws Exception {
        NhanVien nv = new NhanVien("NV001", "Người dùng thử", true, "0912345678", "thu@example.com", "Thử");
        try {
            for (VaiTro vaiTro : VaiTro.values()) {
                SessionManager.getInstance().login(new TaiKhoan("TK001", "tk.kiemthu", "x", vaiTro, true, nv));
                taoPanel((k, t, m) -> List.of(caoVoi));
                SwingUtilities.invokeAndWait(panel::lamMoiDuLieu);
                choKetQua("Tìm thấy 1");
                SwingUtilities.invokeAndWait(() -> assertEquals(vaiTro == VaiTro.QUANLY,
                        tim("themDichVu").isVisible(), "Vai trò " + vaiTro));
            }
            SessionManager.getInstance().logout();
            taoPanel((k, t, m) -> List.of());
            SwingUtilities.invokeAndWait(() -> assertFalse(tim("themDichVu").isVisible()));
        } finally {
            SessionManager.getInstance().logout();
        }
    }

    @Test
    void tenMucThue_thue0HienKct() {
        assertEquals("KCT", PnlDichVu.tenMucThue(MucThue.THUE_0));
        assertEquals("5%", PnlDichVu.tenMucThue(MucThue.THUE_5));
        assertEquals("1.250.000 ₫", PnlDichVu.dinhDangTien(1_250_000));
    }

    private static String hienThi(JTable table, int row, int column) {
        Component c = table.prepareRenderer(table.getCellRenderer(row, column), row, column);
        return ((JLabel) c).getText();
    }

    private void choKetQua(String expected) throws Exception {
        long timeout = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < timeout) {
            AtomicBoolean ready = new AtomicBoolean();
            SwingUtilities.invokeAndWait(() -> ready.set(tim("lamMoiDichVu").isEnabled()
                    && ((JLabel) tim("ketQuaDichVu")).getText().contains(expected)));
            if (ready.get()) {
                return;
            }
            Thread.sleep(20);
        }
        fail("Không nhận được kết quả: " + expected);
    }

    private Component tim(String name) {
        Component found = timTrong(panel, name);
        if (found == null) {
            throw new AssertionError("Thiếu component " + name);
        }
        return found;
    }

    private Component timTrong(Component component, String name) {
        if (name.equals(component.getName())) {
            return component;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                Component found = timTrong(child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
