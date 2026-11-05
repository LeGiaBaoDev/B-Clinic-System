package com.quanlynhakhoa.ui.panel;

import static org.junit.jupiter.api.Assertions.*;

import com.quanlynhakhoa.entity.HoSoBenhAn;
import com.quanlynhakhoa.entity.HoaDon;
import com.quanlynhakhoa.entity.KhachHang;
import com.quanlynhakhoa.entity.NhanVien;
import com.quanlynhakhoa.entity.PhieuKham;
import com.quanlynhakhoa.entity.enums.TrangThaiPhieuKham;
import com.quanlynhakhoa.exception.DataAccessException;
import java.awt.Component;
import java.awt.Container;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/*
 * Khung danh sách hóa đơn (không cần DB, DAO giả)
 * [x] mở: tab mặc định "Chờ thanh toán", số đếm 3 tab, khoảng ngày mặc định 30 ngày, gọi DAO 1 lần ngoài EDT
 * [x] bảng: mã HĐ, khách, mã khách, ngày lập, tổng tiền, trạng thái, thao tác; dòng cuối hiển thị + tổng
 * [x] đổi tab / dropdown trạng thái: chỉ lọc trên máy, hai bên đồng bộ, đổi chữ "Tổng ..."
 * [x] tìm theo từ khóa (Enter), không có kết quả -> "Không tìm thấy hóa đơn phù hợp."
 * [x] khoảng ngày sai -> báo lỗi, không gọi DAO
 * [x] Xem -> mở chi tiết; Thu tiền chỉ với hóa đơn chưa trả, thu xong tải lại; không có quyền thu thì ẩn link
 * [x] lỗi DB -> thông báo chung
 */
class PnlDanhSachHoaDonTest {

    private static final LocalDate HOM_NAY = LocalDate.of(2026, 10, 16);
    private PnlDanhSachHoaDon panel;
    private final List<LocalDate[]> daGoi = new ArrayList<>();
    private final AtomicInteger soLanGoi = new AtomicInteger();
    private final AtomicReference<HoaDon> daXem = new AtomicReference<>();
    private final AtomicReference<HoaDon> daThu = new AtomicReference<>();

    private static HoaDon hoaDon(String maHD, String maKH, String ten, String sdt, boolean daTra, int gio) {
        KhachHang kh = new KhachHang(maKH, ten, LocalDate.of(1995, 5, 20), true, sdt, null, null);
        LocalDateTime lap = LocalDateTime.of(2026, 10, 16, gio, 8);
        PhieuKham pk = new PhieuKham("PK" + maHD.substring(2), lap.minusMinutes(30), TrangThaiPhieuKham.HOAN_TAT,
                null, new HoSoBenhAn(null, null, kh), null, null);
        NhanVien thuNgan = new NhanVien("NV003", "Thảo Linh", false, "0398682232", "letan@example.com", "Lễ tân");
        return daTra ? new HoaDon(maHD, lap, true, HoaDon.TIEN_MAT, lap.plusMinutes(5), 0, pk, null, thuNgan)
                : new HoaDon(maHD, lap, false, null, null, 0, pk, null, null);
    }

    private final List<HoaDon> db = List.of(
            hoaDon("HD261016042", "KH01284", "Nguyễn Minh Anh", "0901234567", false, 10),
            hoaDon("HD261016041", "KH01172", "Trần Đức Huy", "0912345678", false, 9),
            hoaDon("HD261016039", "KH01205", "Võ Thanh Tâm", "0923456789", true, 8));

    private TraCuuHoaDon traCuuGia(RuntimeException loi) {
        return new TraCuuHoaDon((tu, den, daTra) -> {
            soLanGoi.incrementAndGet();
            daGoi.add(new LocalDate[]{tu, den});
            if (loi != null) {
                throw loi;
            }
            assertNull(daTra, "Khung luôn lấy mọi trạng thái để đếm 3 tab");
            return db;
        }, ma -> switch (ma) {
            case "HD261016042" -> new BigDecimal("1250000");
            case "HD261016041" -> new BigDecimal("2400000");
            default -> new BigDecimal("950000");
        });
    }

    private void tao(RuntimeException loi, boolean choThuTien) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            panel = new PnlDanhSachHoaDon(traCuuGia(loi), daXem::set,
                    choThuTien ? (cha, hd, sau) -> {
                        daThu.set(hd);
                        sau.run(); // giả lập thu xong -> tải lại
                    } : null, () -> HOM_NAY);
            panel.lamMoiDuLieu();
        });
    }

    @AfterEach
    void don() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (panel != null) {
                panel.removeNotify();
            }
        });
    }

    @Test
    void moKhung_tabMacDinhChoThanhToan_demDung_tongDung() throws Exception {
        tao(null, true);
        choHienThi("Hiển thị 2/2");

        assertEquals(1, soLanGoi.get());
        assertEquals(LocalDate.of(2026, 9, 17), daGoi.getFirst()[0], "Mặc định 30 ngày gần nhất");
        assertEquals(HOM_NAY, daGoi.getFirst()[1]);
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("Chờ thanh toán (2)", ((JButton) tim("tabChoThanhToan")).getText());
            assertEquals("Đã thanh toán (1)", ((JButton) tim("tabDaThanhToan")).getText());
            assertEquals("Tất cả (3)", ((JButton) tim("tabTatCa")).getText());
            assertEquals(2, panel.model.getRowCount());
            assertEquals("HD261016042", panel.model.getValueAt(0, PnlDanhSachHoaDon.COT_MA));
            assertEquals("Nguyễn Minh Anh", panel.model.getValueAt(0, PnlDanhSachHoaDon.COT_KHACH));
            assertEquals("KH01284", panel.model.getValueAt(0, PnlDanhSachHoaDon.COT_MA_KHACH));
            assertEquals("16/10/2026 10:08", panel.model.getValueAt(0, PnlDanhSachHoaDon.COT_NGAY_LAP));
            assertEquals("1.250.000 ₫", panel.model.getValueAt(0, PnlDanhSachHoaDon.COT_TONG));
            assertEquals("Chờ thanh toán", panel.model.getValueAt(0, PnlDanhSachHoaDon.COT_TRANG_THAI));
            assertEquals("Xem · Thanh toán", panel.model.getValueAt(0, PnlDanhSachHoaDon.COT_THAO_TAC));
            assertEquals("Hiển thị 2/2 hóa đơn · Đơn vị: VNĐ", panel.lblHienThi.getText());
            assertEquals("Tổng cần thanh toán", panel.lblTenTong.getText());
            assertEquals("3.650.000 ₫", panel.lblTong.getText());
        });
    }

    @Test
    void doiTabHoacDropdown_locTrenMay_dongBo_doiChuTong() throws Exception {
        tao(null, true);
        choHienThi("Hiển thị 2/2");
        SwingUtilities.invokeAndWait(() -> {
            ((JButton) tim("tabDaThanhToan")).doClick();
            assertEquals(1, panel.model.getRowCount());
            assertEquals("HD261016039", panel.model.getValueAt(0, PnlDanhSachHoaDon.COT_MA));
            assertEquals("Đã thanh toán", panel.model.getValueAt(0, PnlDanhSachHoaDon.COT_TRANG_THAI));
            assertEquals("Xem · In hóa đơn", panel.model.getValueAt(0, PnlDanhSachHoaDon.COT_THAO_TAC));
            assertEquals("Tổng đã thu", panel.lblTenTong.getText());
            assertEquals(TraCuuHoaDon.TrangThai.DA_THANH_TOAN, panel.cboTrangThai.getSelectedItem(), "Dropdown theo tab");

            panel.cboTrangThai.setSelectedItem(TraCuuHoaDon.TrangThai.TAT_CA);
            assertEquals(3, panel.model.getRowCount(), "Tab đổi theo dropdown");
            assertEquals("Tổng giá trị hóa đơn", panel.lblTenTong.getText());
            assertEquals("4.600.000 ₫", panel.lblTong.getText());
        });
        assertEquals(1, soLanGoi.get(), "Đổi tab không gọi DB");
    }

    @Test
    void timTuKhoa_vaKhongCoKetQua() throws Exception {
        tao(null, true);
        choHienThi("Hiển thị 2/2");
        SwingUtilities.invokeAndWait(() -> {
            panel.txtTimKiem.setText("tran duc");
            panel.txtTimKiem.postActionEvent(); // Enter
        });
        choHienThi("Hiển thị 1/1");
        SwingUtilities.invokeAndWait(() ->
                assertEquals("HD261016041", panel.model.getValueAt(0, PnlDanhSachHoaDon.COT_MA)));

        SwingUtilities.invokeAndWait(() -> {
            panel.txtTimKiem.setText("không có ai");
            panel.txtTimKiem.postActionEvent();
        });
        choHienThi("Không tìm thấy hóa đơn phù hợp.");
        SwingUtilities.invokeAndWait(() -> assertEquals("0 ₫", panel.lblTong.getText()));
    }

    @Test
    void khoangNgaySai_baoLoi_khongGoiDao() throws Exception {
        tao(null, true);
        choHienThi("Hiển thị 2/2");
        int truoc = soLanGoi.get();
        SwingUtilities.invokeAndWait(() -> panel.dcTuNgay.setDate(
                Date.from(HOM_NAY.plusDays(3).atStartOfDay(ZoneId.systemDefault()).toInstant())));
        choHienThi("Khoảng ngày không hợp lệ");
        assertEquals(truoc, soLanGoi.get());
    }

    @Test
    void xemVaThuTien_chiThuHoaDonChuaTra_thuXongTaiLai() throws Exception {
        tao(null, true);
        choHienThi("Hiển thị 2/2");
        int truoc = soLanGoi.get();
        SwingUtilities.invokeAndWait(() -> {
            panel.chayThaoTac(PnlDanhSachHoaDon.LINK_XEM, db.get(2));
            panel.chayThaoTac(PnlDanhSachHoaDon.LINK_THU_TIEN, db.get(2)); // đã thanh toán: bỏ qua
            panel.chayThaoTac(PnlDanhSachHoaDon.LINK_THU_TIEN, db.get(0));
        });
        assertEquals("HD261016039", daXem.get().getMaHD());
        assertEquals("HD261016042", daThu.get().getMaHD());
        choDen(() -> soLanGoi.get() > truoc);
    }

    @Test
    void khongCoQuyenThu_anLinkThuTien() throws Exception {
        tao(null, false);
        choHienThi("Hiển thị 2/2");
        SwingUtilities.invokeAndWait(() -> {
            JTable bang = panel.tblHoaDon;
            int cot = PnlDanhSachHoaDon.COT_THAO_TAC;
            Component o = bang.prepareRenderer(bang.getCellRenderer(0, cot), 0, cot);
            boolean coThu = false;
            for (Component con : ((Container) o).getComponents()) {
                if (PnlDanhSachHoaDon.LINK_THU_TIEN.equals(con.getName()) && con.isVisible()) {
                    coThu = true;
                }
            }
            assertFalse(coThu, "Không truyền hành động thu tiền thì không hiện link Thu tiền");
            panel.chayThaoTac(PnlDanhSachHoaDon.LINK_THU_TIEN, db.get(0));
        });
        assertNull(daThu.get());
    }

    @Test
    void loiDb_hienThongBaoChung() throws Exception {
        tao(new DataAccessException("timeout chi tiết kỹ thuật", null), true);
        choHienThi("Không thể tải danh sách hóa đơn. Vui lòng thử lại.");
        SwingUtilities.invokeAndWait(() -> assertEquals(0, panel.model.getRowCount()));
    }

    private void choHienThi(String chu) throws Exception {
        choDen(() -> panel.lblHienThi.getText().contains(chu));
    }

    private static void choDen(java.util.function.BooleanSupplier dk) throws Exception {
        long het = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < het) {
            AtomicBoolean ok = new AtomicBoolean();
            SwingUtilities.invokeAndWait(() -> ok.set(dk.getAsBoolean()));
            if (ok.get()) {
                return;
            }
            Thread.sleep(20);
        }
        fail("Hết thời gian chờ");
    }

    private Component tim(String ten) {
        Component c = timTrong(panel, ten);
        if (c == null) {
            throw new AssertionError("Thiếu component " + ten);
        }
        return c;
    }

    private static Component timTrong(Component c, String ten) {
        if (ten.equals(c.getName())) {
            return c;
        }
        if (c instanceof Container ct) {
            for (Component con : ct.getComponents()) {
                Component kq = timTrong(con, ten);
                if (kq != null) {
                    return kq;
                }
            }
        }
        return null;
    }
}
