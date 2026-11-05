package com.quanlynhakhoa.ui.panel;

import static org.junit.jupiter.api.Assertions.*;

import com.quanlynhakhoa.entity.DichVu;
import com.quanlynhakhoa.entity.LoaiDichVu;
import com.quanlynhakhoa.entity.enums.MucThue;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class PnlCapNhatDichVuTest {

    private final DichVu caoVoi = new DichVu("DV001", "Cạo vôi răng", 300_000, 30,
            "Buổi 1: cạo vôi", true, new LoaiDichVu("LDV01", LoaiDichVu.DIEU_TRI, MucThue.THUE_0));
    private PnlCapNhatDichVu trang;
    private final AtomicReference<DichVu> daGui = new AtomicReference<>();
    private final AtomicReference<DichVu> daLuu = new AtomicReference<>();
    private final AtomicBoolean daDong = new AtomicBoolean();
    /** Câu trả lời giả cho hộp "Bỏ thay đổi?" (thay JOptionPane để test không bị treo). */
    private boolean dongYBoThayDoi;

    private void taoTrang(PnlCapNhatDichVu.LuuDichVu luu) throws Exception {
        taoTrang(luu, List.of());
    }

    private void taoTrang(PnlCapNhatDichVu.LuuDichVu luu, List<String> loTrinh) throws Exception {
        SwingUtilities.invokeAndWait(() -> trang = new PnlCapNhatDichVu(caoVoi, dv -> {
            daGui.set(dv);
            return luu.luu(dv);
        }, () -> loTrinh, daLuu::set, () -> daDong.set(true)) {
            @Override
            boolean xacNhanBoThayDoi() {
                return dongYBoThayDoi;
            }
        });
    }

    @Test
    void moTrang_dienSanDuLieu_maVaTrangThaiChiHienThi_giaDuKienDung() throws Exception {
        taoTrang(dv -> true);
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("DV001", trang.txtMaDV.getText());
            assertFalse(trang.txtMaDV.isEditable());
            assertEquals("Cạo vôi răng", trang.txtTenDV.getText());
            assertEquals(1, trang.cboDanhMuc.getSelectedIndex(), "Điều trị bệnh lý");
            assertEquals("Buổi 1: cạo vôi", trang.txaLoTrinh.getText());
            assertEquals("30", trang.txtThoiLuong.getText());
            assertEquals("300.000", trang.txtDonGia.getText());
            assertEquals("Đang cung cấp", trang.txtTrangThai.getText());
            assertFalse(trang.txtTrangThai.isEditable(), "Đổi trạng thái thuộc UC008");
            assertEquals("300.000 ₫", trang.lblGiaSauThue.getText(), "KCT: giá sau thuế = đơn giá");
            assertFalse(trang.btnLuu.isEnabled(), "Chưa sửa gì thì chưa cho lưu (UC007 4.1)");
            assertEquals("Dịch vụ / DV001 · Cạo vôi răng", trang.lblDuongDan.getText());
        });
    }

    @Test
    void suaRoiTraLaiNhuCu_baoKhongCoThayDoi() throws Exception {
        taoTrang(dv -> true);
        SwingUtilities.invokeAndWait(() -> {
            trang.txtDonGia.setText("350.000");
            assertTrue(trang.btnLuu.isEnabled());
            assertTrue(trang.lblDuongDan.getText().endsWith("· Thay đổi chưa được lưu"));
            trang.txtDonGia.setText("300000"); // về đúng giá cũ (viết khác định dạng)
            assertFalse(trang.btnLuu.isEnabled());
            assertEquals("Không có thay đổi để lưu.", trang.lblThongBao.getText());
        });
    }

    @Test
    void thoiLuongSai_hienLoi_hopCanhBaoNhacGiaTriDaLuu_vaTatNutLuu() throws Exception {
        taoTrang(dv -> true);
        SwingUtilities.invokeAndWait(() -> {
            trang.txtThoiLuong.setText("-15");
            assertEquals("Thời lượng phải là số phút (số nguyên).", trang.lblLoiThoiLuong.getText());
            assertTrue(trang.pnlCanhBao.isVisible());
            assertTrue(trang.lblCanhBao.getText().contains("Giá trị đã lưu vẫn là 30 phút"));
            assertFalse(trang.btnLuu.isEnabled());
            assertEquals("Vui lòng kiểm tra các trường được đánh dấu.", trang.lblThongBao.getText());
            trang.txtThoiLuong.setText("45");
            assertFalse(trang.pnlCanhBao.isVisible());
            assertTrue(trang.btnLuu.isEnabled());
        });
    }

    @Test
    void doiDanhMuc_thueVaGiaSauThueDoiTheo() throws Exception {
        taoTrang(dv -> true);
        SwingUtilities.invokeAndWait(() -> {
            trang.cboDanhMuc.setSelectedIndex(3); // Thẩm mỹ 10%
            assertEquals("Thuế GTGT (10%)", trang.lblGiaThueTen.getText());
            assertEquals("30.000 ₫", trang.lblGiaThue.getText());
            assertEquals("330.000 ₫", trang.lblGiaSauThue.getText());
            assertTrue(trang.btnLuu.isEnabled());
        });
    }

    @Test
    void luu_guiDungMaVaDuLieuMoi_ngoaiEdt_khongSuaDoiTuongGoc() throws Exception {
        AtomicBoolean ngoaiEdt = new AtomicBoolean();
        taoTrang(dv -> {
            ngoaiEdt.set(!SwingUtilities.isEventDispatchThread());
            return true;
        });
        SwingUtilities.invokeAndWait(() -> {
            trang.txtTenDV.setText("  Cạo vôi răng siêu âm  ");
            trang.txtDonGia.setText("350.000");
            trang.txaLoTrinh.setText("   ");
            trang.luuThayDoi();
        });
        choDen(() -> daLuu.get() != null);

        DichVu gui = daGui.get();
        assertTrue(ngoaiEdt.get());
        assertEquals("DV001", gui.getMaDV(), "Đúng mã để DAO cập nhật đúng dòng");
        assertEquals("Cạo vôi răng siêu âm", gui.getTenDV());
        assertEquals(350_000, gui.getDonGia());
        assertNull(gui.getLoTrinhDichVu(), "Lộ trình để trống -> lưu NULL");
        assertEquals(300_000, caoVoi.getDonGia(), "Không sửa đối tượng đang hiện trên bảng");
    }

    @Test
    void loiNghiepVu_hienThongBaoDao_vaGiuTrang() throws Exception {
        taoTrang(dv -> {
            throw new BusinessException("Tên dịch vụ đã tồn tại.");
        });
        SwingUtilities.invokeAndWait(() -> {
            trang.txtTenDV.setText("Tẩy trắng răng");
            trang.luuThayDoi();
        });
        choDen(() -> trang.txtTenDV.isEnabled());
        assertEquals("Tên dịch vụ đã tồn tại.", trang.lblThongBao.getText());
        assertNull(daLuu.get());
        assertFalse(daDong.get());
    }

    @Test
    void loiDb_hienThongBaoChung() throws Exception {
        taoTrang(dv -> {
            throw new DataAccessException("SQLState 08S01 chi tiết kỹ thuật", null);
        });
        SwingUtilities.invokeAndWait(() -> {
            trang.txtDonGia.setText("320.000");
            trang.luuThayDoi();
        });
        choDen(() -> trang.txtTenDV.isEnabled());
        assertEquals("Không thể lưu dữ liệu, vui lòng thử lại.", trang.lblThongBao.getText());
    }

    @Test
    void huy_chuaSuaThiDongNgay_daSuaThiHoiXacNhan() throws Exception {
        taoTrang(dv -> true);
        SwingUtilities.invokeAndWait(() -> trang.btnHuy.doClick());
        assertTrue(daDong.get(), "Chưa sửa gì: đóng ngay");

        daDong.set(false);
        taoTrang(dv -> true);
        SwingUtilities.invokeAndWait(() -> {
            trang.txtTenDV.setText("Tên mới");
            dongYBoThayDoi = false;
            trang.btnQuayLai.doClick();
            assertFalse(daDong.get(), "Chọn Không: ở lại trang");
            dongYBoThayDoi = true;
            trang.btnQuayLai.doClick();
            assertTrue(daDong.get(), "Chọn Có: bỏ thay đổi, về danh sách");
        });
        assertNull(daGui.get(), "Hủy không gọi DAO");
    }

    @Test
    void loTrinh_coGoiY() throws Exception {
        taoTrang(dv -> true, List.of("Buổi 1: khám\nBuổi 2: cạo vôi", "Buổi 1: lấy tủy"));
        // Lộ trình hiện tại "Buổi 1: cạo vôi" không khớp gợi ý nào; gõ "tủy" thì phải hiện 1 khung
        // (gợi ý nạp chạy nền, nạp xong sẽ vẽ lại theo nội dung đang có trong ô).
        SwingUtilities.invokeAndWait(() -> trang.txaLoTrinh.setText("tủy"));
        choDen(() -> trang.pnlGoiY.getComponentCount() == 2); // nhãn "Gợi ý:" + 1 khung
    }

    private static void choDen(BooleanSupplier dieuKien) throws Exception {
        long timeout = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < timeout) {
            AtomicBoolean ok = new AtomicBoolean();
            SwingUtilities.invokeAndWait(() -> ok.set(dieuKien.getAsBoolean()));
            if (ok.get()) {
                return;
            }
            Thread.sleep(20);
        }
        fail("Hết thời gian chờ");
    }
}
