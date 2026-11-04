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

    private void taiVaCho(String ketQua) throws Exception {
        SwingUtilities.invokeAndWait(panel::lamMoiDuLieu);
        choKetQua(ketQua);
    }

    @Test
    void taiLanDau_tabMacDinhDangCungCap_demDungSo_vaDinhDangCot() throws Exception {
        AtomicBoolean ngoaiEdt = new AtomicBoolean();
        taoPanel((k, t, m) -> {
            ngoaiEdt.set(!SwingUtilities.isEventDispatchThread());
            return List.of(caoVoi, niengRang);
        });
        taiVaCho("1 dịch vụ");

        assertTrue(ngoaiEdt.get());
        assertEquals(new DieuKien("", null, null), daGoi.getLast(), "Lấy mọi trạng thái để đếm 3 tab");
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("Tất cả (2)", ((JButton) tim("tabTatCa")).getText());
            assertEquals("Đang cung cấp (1)", ((JButton) tim("tabDangCungCap")).getText());
            assertEquals("Ngừng cung cấp (1)", ((JButton) tim("tabNgungCungCap")).getText());
            JTable table = bang();
            assertEquals(1, table.getRowCount(), "Mặc định chỉ hiện dịch vụ đang cung cấp (UC006 bước 2)");
            assertFalse(table.isCellEditable(0, 0));
            assertEquals("DV001", table.getValueAt(0, PnlDichVu.COT_MA));
            assertEquals(LoaiDichVu.DIEU_TRI, table.getValueAt(0, PnlDichVu.COT_DANH_MUC));
            assertEquals("KCT", table.getValueAt(0, PnlDichVu.COT_THUE));
            assertEquals("Đang cung cấp", table.getValueAt(0, PnlDichVu.COT_TRANG_THAI));
            assertEquals("300.000 ₫", hienThi(table, 0, PnlDichVu.COT_DON_GIA));
            assertEquals("30 phút", hienThi(table, 0, PnlDichVu.COT_THOI_LUONG));
        });
    }

    @Test
    void doiTab_chiLocLaiTrenMay_khongGoiDb() throws Exception {
        taoPanel((k, t, m) -> List.of(caoVoi, niengRang));
        taiVaCho("1 dịch vụ");
        int soLanGoi = daGoi.size();

        SwingUtilities.invokeAndWait(() -> {
            ((JButton) tim("tabNgungCungCap")).doClick();
            assertEquals(1, bang().getRowCount());
            assertEquals("DV002", bang().getValueAt(0, PnlDichVu.COT_MA));
            assertEquals("10%", bang().getValueAt(0, PnlDichVu.COT_THUE));
            assertEquals("Ngừng cung cấp", bang().getValueAt(0, PnlDichVu.COT_TRANG_THAI));
            assertEquals(2, ((JComboBox<?>) tim("locTrangThai")).getSelectedIndex(), "Dropdown đổi theo tab");
            ((JButton) tim("tabTatCa")).doClick();
            assertEquals(2, bang().getRowCount());
            assertEquals(0, ((JComboBox<?>) tim("locTrangThai")).getSelectedIndex());
        });
        assertEquals(soLanGoi, daGoi.size());
    }

    @Test
    void doiDropdownTrangThai_tabDoiTheo_khongGoiDb() throws Exception {
        taoPanel((k, t, m) -> List.of(caoVoi, niengRang));
        taiVaCho("1 dịch vụ");
        int soLanGoi = daGoi.size();

        SwingUtilities.invokeAndWait(() -> {
            JComboBox<?> trangThai = (JComboBox<?>) tim("locTrangThai");
            assertEquals(1, trangThai.getSelectedIndex(), "Mặc định Đang cung cấp");
            trangThai.setSelectedIndex(2); // Ngừng cung cấp
            assertEquals(1, bang().getRowCount());
            assertEquals("DV002", bang().getValueAt(0, PnlDichVu.COT_MA));
            assertEquals(TEAL_DANG_CHON, ((JButton) tim("tabNgungCungCap")).getForeground(), "Tab đổi theo");
        });
        assertEquals(soLanGoi, daGoi.size());
    }

    private static final java.awt.Color TEAL_DANG_CHON = com.quanlynhakhoa.ui.component.UiConstants.TEAL;

    @Test
    void timTheoTuKhoaVaPhanLoai_guiDungDieuKien() throws Exception {
        taoPanel((k, t, m) -> List.of(caoVoi));
        SwingUtilities.invokeAndWait(() -> {
            JTextField txt = (JTextField) tim("timDichVu");
            txt.setText("  cạo  ");
            txt.postActionEvent(); // Enter
        });
        choKetQua("1 dịch vụ");
        assertEquals(new DieuKien("cạo", null, null), daGoi.getLast());

        SwingUtilities.invokeAndWait(() -> ((JComboBox<?>) tim("locDanhMuc")).setSelectedIndex(3));
        choKetQua("1 dịch vụ");
        assertEquals(new DieuKien("cạo", null, MucThue.THUE_10), daGoi.getLast());
    }

    @Test
    void khongCoKetQua_hienThongBaoTheoBaoCao() throws Exception {
        taoPanel((k, t, m) -> List.of());
        taiVaCho("Không tìm thấy dịch vụ phù hợp.");
        SwingUtilities.invokeAndWait(() -> assertEquals(0, bang().getRowCount()));
    }

    @Test
    void phanTrang_10DongMoiTrang_quaTrangSau() throws Exception {
        List<DichVu> muoiHai = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            muoiHai.add(new DichVu(String.format("DV%03d", i), "Dịch vụ " + i, 100_000, 15, null, true, dieuTri));
        }
        taoPanel((k, t, m) -> muoiHai);
        taiVaCho("12 dịch vụ");
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(PnlDichVu.SO_DONG_MOI_TRANG, bang().getRowCount());
            assertEquals("Hiển thị 1–10 trong tổng số 12 dịch vụ", ((JLabel) tim("phanTrangDichVu")).getText());
            assertFalse(tim("trangTruoc").isEnabled());
            ((JButton) tim("trangSau")).doClick();
            assertEquals(2, bang().getRowCount());
            assertEquals("DV011", bang().getValueAt(0, PnlDichVu.COT_MA));
            assertEquals("Hiển thị 11–12 trong tổng số 12 dịch vụ", ((JLabel) tim("phanTrangDichVu")).getText());
            assertFalse(tim("trangSau").isEnabled());
            ((JButton) tim("trang1")).doClick();
            assertEquals("DV001", bang().getValueAt(0, PnlDichVu.COT_MA));
        });
    }

    @Test
    void chonDong_hienKhungLoTrinh_boChonThiAnKhung() throws Exception {
        taoPanel((k, t, m) -> List.of(caoVoi, niengRang));
        taiVaCho("1 dịch vụ");
        SwingUtilities.invokeAndWait(() -> {
            JPanel khung = (JPanel) tim("khungChiTietDichVu");
            assertFalse(khung.isVisible(), "Chưa chọn dòng: ẩn khung, bảng dài hết cỡ");
            ((JButton) tim("tabTatCa")).doClick();
            bang().setRowSelectionInterval(1, 1);
            assertTrue(khung.isVisible());
            String chiTiet = ((JLabel) tim("chiTietDichVu")).getText();
            assertTrue(chiTiet.contains("Niềng răng &lt;mắc cài&gt;"), "Escape HTML");
            assertTrue(chiTiet.contains("Lộ trình điều trị:<br>Buổi 1 khám<br>Buổi 2"), "Xuống dòng từng buổi");
            bang().setRowSelectionInterval(0, 0);
            assertTrue(((JLabel) tim("chiTietDichVu")).getText().contains("Lộ trình điều trị: Không có"));
            bang().clearSelection();
            assertFalse(khung.isVisible());
        });
    }

    @Test
    void loiNghiepVu_hienThongBaoDao_loiDb_hienThongBaoChung() throws Exception {
        taoPanel((k, t, m) -> {
            throw new BusinessException("Vui lòng đăng nhập.");
        });
        taiVaCho("Vui lòng đăng nhập.");

        taoPanel((k, t, m) -> {
            throw new DataAccessException("SQL timeout chi tiết kỹ thuật", null);
        });
        taiVaCho("Không thể tải danh mục dịch vụ. Vui lòng thử lại.");
    }

    @Test
    void xoaLoc_duaVeMacDinh_vaChiTaiMotLan() throws Exception {
        taoPanel((k, t, m) -> List.of(caoVoi, niengRang));
        SwingUtilities.invokeAndWait(() -> {
            ((JComboBox<?>) tim("locDanhMuc")).setSelectedIndex(2);
            ((JButton) tim("tabTatCa")).doClick();
        });
        choKetQua("2 dịch vụ");
        int truoc = daGoi.size();

        SwingUtilities.invokeAndWait(() -> ((JButton) tim("xoaLocDichVu")).doClick());
        choKetQua("1 dịch vụ");
        assertEquals(truoc + 1, daGoi.size(), "Đặt lại nhiều điều kiện chỉ tải một lần");
        assertEquals(new DieuKien("", null, null), daGoi.getLast());
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("", ((JTextField) tim("timDichVu")).getText());
            assertEquals(0, ((JComboBox<?>) tim("locDanhMuc")).getSelectedIndex());
            assertEquals(1, ((JComboBox<?>) tim("locTrangThai")).getSelectedIndex(), "Về Đang cung cấp");
            assertEquals("Đang cung cấp (1)", ((JButton) tim("tabDangCungCap")).getText());
            assertEquals(1, bang().getRowCount(), "Về tab mặc định Đang cung cấp");
        });
    }

    @Test
    void nutThemDichVu_chiHienVoiQuanLy() throws Exception {
        NhanVien nv = new NhanVien("NV001", "Người dùng thử", true, "0912345678", "thu@example.com", "Thử");
        try {
            for (VaiTro vaiTro : VaiTro.values()) {
                SessionManager.getInstance().login(new TaiKhoan("TK001", "tk.kiemthu", "x", vaiTro, true, nv));
                taoPanel((k, t, m) -> List.of(caoVoi));
                taiVaCho("1 dịch vụ");
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
    void nhanDup_quanLyMoTrangChinhSua_dichVuNgungThiBao_huyThiVeDanhSach() throws Exception {
        NhanVien nv = new NhanVien("NV001", "Người dùng thử", true, "0912345678", "thu@example.com", "Thử");
        try {
            SessionManager.getInstance().login(new TaiKhoan("TK001", "tk.kiemthu", "x", VaiTro.QUANLY, true, nv));
            taoPanel((k, t, m) -> List.of(caoVoi, niengRang));
            taiVaCho("1 dịch vụ");
            SwingUtilities.invokeAndWait(() -> {
                ((JButton) tim("tabTatCa")).doClick();
                nhanDup(1); // DV002 đã ngừng
                assertNull(panel.trangCapNhat(), "Dịch vụ đã ngừng không mở trang chỉnh sửa (UC007 tiền điều kiện)");
                assertTrue(((JLabel) tim("ketQuaDichVu")).getText().contains("đã ngừng cung cấp"));

                nhanDup(0); // DV001 đang cung cấp
                PnlCapNhatDichVu trangSua = panel.trangCapNhat();
                assertNotNull(trangSua);
                assertTrue(trangSua.isVisible(), "Đang hiện thẻ chỉnh sửa");
                assertEquals("DV001", trangSua.txtMaDV.getText());
                assertFalse(tim("theDanhSachDichVu").isVisible());

                trangSua.btnHuy.doClick(); // chưa sửa gì -> về danh sách ngay
                assertNull(panel.trangCapNhat());
                assertTrue(tim("theDanhSachDichVu").isVisible());
            });

            SessionManager.getInstance().login(new TaiKhoan("TK003", "tk.kiemthu", "x", VaiTro.LETAN, true, nv));
            taoPanel((k, t, m) -> List.of(caoVoi));
            taiVaCho("1 dịch vụ");
            SwingUtilities.invokeAndWait(() -> {
                nhanDup(0);
                assertNull(panel.trangCapNhat(), "Lễ tân chỉ xem, nhấn đúp không mở trang sửa");
            });
        } finally {
            SessionManager.getInstance().logout();
        }
    }

    /** Giả lập nhấn đúp chuột trái vào giữa dòng {@code row} của bảng. */
    private void nhanDup(int row) {
        JTable table = bang();
        java.awt.Rectangle o = table.getCellRect(row, PnlDichVu.COT_TEN, true);
        table.dispatchEvent(new java.awt.event.MouseEvent(table, java.awt.event.MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, o.x + o.width / 2, o.y + o.height / 2, 2, false,
                java.awt.event.MouseEvent.BUTTON1));
    }

    /** Dữ liệu "DB giả" có thể đổi trạng thái, để kiểm tra danh sách được tải lại sau thao tác. */
    private final List<DichVu> dbGia = new ArrayList<>();
    private final List<String> daNgung = new ArrayList<>();
    private final List<String> daMoLai = new ArrayList<>();
    private final List<String> daThongBao = new ArrayList<>();
    private final List<String> daBaoLoi = new ArrayList<>();
    private boolean traLoiXacNhan;
    private RuntimeException loiKhiDoi;

    /** Panel có ngừng / mở lại giả và hộp thoại giả (không bật JOptionPane thật). */
    private void taoPanelThaoTac() throws Exception {
        dbGia.clear();
        dbGia.add(new DichVu(caoVoi));
        dbGia.add(new DichVu(niengRang));
        SwingUtilities.invokeAndWait(() -> panel = new PnlDichVu((k, t, m) -> List.copyOf(dbGia), dv -> true,
                List::of, ma -> doiTrangThaiGia(ma, false, daNgung), ma -> doiTrangThaiGia(ma, true, daMoLai)) {
            @Override
            boolean xacNhan(String tieuDe, String noiDung, String nutDongY) {
                return traLoiXacNhan;
            }

            @Override
            void thongBao(String noiDung) {
                daThongBao.add(noiDung);
            }

            @Override
            void baoLoi(String noiDung) {
                daBaoLoi.add(noiDung);
            }
        });
    }

    private boolean doiTrangThaiGia(String ma, boolean trangThaiMoi, List<String> ghiLai) {
        ghiLai.add(ma);
        if (loiKhiDoi != null) {
            throw loiKhiDoi;
        }
        dbGia.replaceAll(dv -> {
            if (!dv.getMaDV().equals(ma)) {
                return dv;
            }
            DichVu moi = new DichVu(dv);
            moi.setTrangThai(trangThaiMoi);
            return moi;
        });
        return true;
    }

    private void dangNhap(VaiTro vaiTro) {
        NhanVien nv = new NhanVien("NV001", "Người dùng thử", true, "0912345678", "thu@example.com", "Thử");
        SessionManager.getInstance().login(new TaiKhoan("TK001", "tk.kiemthu", "x", vaiTro, true, nv));
    }

    @Test
    void cotThaoTac_quanLyThayLinkTheoTrangThai_leTanAnCot_bamTrungLink() throws Exception {
        try {
            dangNhap(VaiTro.QUANLY);
            taoPanelThaoTac();
            taiVaCho("1 dịch vụ");
            SwingUtilities.invokeAndWait(() -> {
                ((JButton) tim("tabTatCa")).doClick();
                JTable table = bang();
                assertTrue(table.getColumnModel().getColumn(PnlDichVu.COT_THAO_TAC).getWidth() > 0
                        || table.getColumnModel().getColumn(PnlDichVu.COT_THAO_TAC).getPreferredWidth() > 0);
                assertEquals("Sửa · Ngừng cung cấp", table.getValueAt(0, PnlDichVu.COT_THAO_TAC));
                assertEquals("Mở lại cung cấp", table.getValueAt(1, PnlDichVu.COT_THAO_TAC));

                // Dựng bảng có kích thước thật rồi trỏ đúng vào chữ "Ngừng cung cấp" ở dòng 0.
                table.setSize(1200, 300);
                table.doLayout();
                assertEquals(PnlDichVu.LINK_NGUNG, panel.linkTaiDiem(diemCuaLink(table, 0, PnlDichVu.LINK_NGUNG)));
                assertEquals(PnlDichVu.LINK_SUA, panel.linkTaiDiem(diemCuaLink(table, 0, PnlDichVu.LINK_SUA)));
                assertEquals(PnlDichVu.LINK_MO_LAI, panel.linkTaiDiem(diemCuaLink(table, 1, PnlDichVu.LINK_MO_LAI)));
                java.awt.Rectangle oTen = table.getCellRect(0, PnlDichVu.COT_TEN, true);
                assertNull(panel.linkTaiDiem(new java.awt.Point(oTen.x + 5, oTen.y + 5)), "Ngoài cột thao tác");
            });

            dangNhap(VaiTro.LETAN);
            taoPanelThaoTac();
            taiVaCho("1 dịch vụ");
            SwingUtilities.invokeAndWait(() -> assertEquals(0,
                    bang().getColumnModel().getColumn(PnlDichVu.COT_THAO_TAC).getMaxWidth(), "Lễ tân chỉ xem"));
        } finally {
            SessionManager.getInstance().logout();
        }
    }

    @Test
    void ngung_xacNhanCo_goiDaoDungMa_taiLai_baoThanhCong_huyThiKhongGoi() throws Exception {
        try {
            dangNhap(VaiTro.QUANLY);
            taoPanelThaoTac();
            taiVaCho("1 dịch vụ");

            traLoiXacNhan = false;
            SwingUtilities.invokeAndWait(() -> panel.chayThaoTac(PnlDichVu.LINK_NGUNG, dbGia.get(0)));
            assertTrue(daNgung.isEmpty(), "Hủy xác nhận thì không gọi DAO");

            traLoiXacNhan = true;
            SwingUtilities.invokeAndWait(() -> panel.chayThaoTac(PnlDichVu.LINK_NGUNG, dbGia.get(0)));
            choKetQua("Không tìm thấy dịch vụ phù hợp."); // tab Đang cung cấp giờ trống
            assertEquals(List.of("DV001"), daNgung);
            assertEquals(1, daThongBao.size());
            assertTrue(daThongBao.getFirst().startsWith("Đã ngừng cung cấp dịch vụ DV001"));
            SwingUtilities.invokeAndWait(() ->
                    assertEquals("Ngừng cung cấp (2)", ((JButton) tim("tabNgungCungCap")).getText()));
        } finally {
            SessionManager.getInstance().logout();
        }
    }

    @Test
    void moLai_xacNhanCo_dichVuVeDangCungCap() throws Exception {
        try {
            dangNhap(VaiTro.QUANLY);
            taoPanelThaoTac();
            taiVaCho("1 dịch vụ");
            traLoiXacNhan = true;
            SwingUtilities.invokeAndWait(() -> panel.chayThaoTac(PnlDichVu.LINK_MO_LAI, dbGia.get(1)));
            choKetQua("2 dịch vụ"); // tab Đang cung cấp: DV001 + DV002 vừa mở lại
            assertEquals(List.of("DV002"), daMoLai);
            assertTrue(daThongBao.getFirst().startsWith("Đã mở lại cung cấp dịch vụ DV002"));
        } finally {
            SessionManager.getInstance().logout();
        }
    }

    @Test
    void daoBaoLoiNghiepVu_hienDungThongBao_trangThaiGiuNguyen() throws Exception {
        try {
            dangNhap(VaiTro.QUANLY);
            taoPanelThaoTac();
            taiVaCho("1 dịch vụ");
            traLoiXacNhan = true;
            loiKhiDoi = new BusinessException("Dịch vụ còn lịch hẹn đã đặt hoặc đang khám. "
                    + "Cần xử lý các lịch hẹn đó trước khi ngừng cung cấp.");
            SwingUtilities.invokeAndWait(() -> panel.chayThaoTac(PnlDichVu.LINK_NGUNG, dbGia.get(0)));
            choKetQua("1 dịch vụ");
            long het = System.nanoTime() + 5_000_000_000L;
            while (daBaoLoi.isEmpty() && System.nanoTime() < het) {
                Thread.sleep(20);
            }
            assertEquals(1, daBaoLoi.size());
            assertTrue(daBaoLoi.getFirst().contains("trước khi ngừng cung cấp"));
            assertTrue(daThongBao.isEmpty());
            assertTrue(dbGia.get(0).isTrangThai(), "Vẫn đang cung cấp");
        } finally {
            loiKhiDoi = null;
            SessionManager.getInstance().logout();
        }
    }

    /** Điểm nằm giữa link {@code tenLink} trong ô THAO TÁC của dòng {@code row}. */
    private static java.awt.Point diemCuaLink(JTable table, int row, String tenLink) {
        java.awt.Rectangle o = table.getCellRect(row, PnlDichVu.COT_THAO_TAC, false);
        Component ve = table.prepareRenderer(table.getCellRenderer(row, PnlDichVu.COT_THAO_TAC),
                row, PnlDichVu.COT_THAO_TAC);
        ve.setBounds(0, 0, o.width, o.height);
        ve.doLayout();
        for (Component con : ((Container) ve).getComponents()) {
            if (tenLink.equals(con.getName())) {
                return new java.awt.Point(o.x + con.getX() + con.getWidth() / 2, o.y + con.getY() + con.getHeight() / 2);
            }
        }
        throw new AssertionError("Ô không có link " + tenLink);
    }

    @Test
    void tenMucThue_thue0HienKct() {
        assertEquals("KCT", PnlDichVu.tenMucThue(MucThue.THUE_0));
        assertEquals("5%", PnlDichVu.tenMucThue(MucThue.THUE_5));
        assertEquals("1.250.000 ₫", PnlDichVu.dinhDangTien(1_250_000));
    }

    private JTable bang() {
        return (JTable) tim("danhSachDichVu");
    }

    private static String hienThi(JTable table, int row, int column) {
        Component c = table.prepareRenderer(table.getCellRenderer(row, column), row, column);
        return ((JLabel) c).getText();
    }

    private void choKetQua(String expected) throws Exception {
        long timeout = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < timeout) {
            AtomicBoolean ready = new AtomicBoolean();
            SwingUtilities.invokeAndWait(() -> ready.set(tim("xoaLocDichVu").isEnabled()
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
