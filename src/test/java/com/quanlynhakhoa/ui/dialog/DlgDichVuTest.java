package com.quanlynhakhoa.ui.dialog;

import static org.junit.jupiter.api.Assertions.*;

import com.quanlynhakhoa.entity.DichVu;
import com.quanlynhakhoa.entity.LoaiDichVu;
import com.quanlynhakhoa.entity.enums.MucThue;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class DlgDichVuTest {

    private DlgDichVu.NoiDung form;
    private final AtomicReference<DichVu> daGui = new AtomicReference<>();
    private final AtomicReference<DichVu> daLuu = new AtomicReference<>();
    private final AtomicBoolean daDong = new AtomicBoolean();

    private void taoForm(DlgDichVu.LuuDichVu luu) throws Exception {
        SwingUtilities.invokeAndWait(() -> form = new DlgDichVu.NoiDung(dv -> {
            daGui.set(dv);
            return luu.luu(dv);
        }, daLuu::set, () -> daDong.set(true)));
    }

    private void nhapHopLe() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            form.txtTenDV.setText("  Cạo vôi răng  ");
            form.txtDonGia.setText("300.000");
            form.spnThoiLuong.setValue(30);
            form.cboPhanLoaiThue.setSelectedIndex(1);
        });
    }

    private String loiKhiLuu() throws Exception {
        AtomicReference<String> loi = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            form.luuDichVu();
            loi.set(form.lblThongBao.getText());
        });
        return loi.get();
    }

    @Test
    void duLieuSai_canhBaoTheoTungTruong_vaKhongGoiLuu() throws Exception {
        taoForm(dv -> true);
        assertEquals("Vui lòng nhập tên dịch vụ.", loiKhiLuu());

        nhapHopLe();
        SwingUtilities.invokeAndWait(() -> form.txtDonGia.setText("0"));
        assertEquals("Đơn giá phải lớn hơn 0.", loiKhiLuu());
        SwingUtilities.invokeAndWait(() -> form.txtDonGia.setText("ba trăm"));
        assertEquals("Đơn giá phải là số tiền hợp lệ (VNĐ).", loiKhiLuu());

        nhapHopLe();
        SwingUtilities.invokeAndWait(() -> form.spnThoiLuong.setValue(0));
        assertEquals("Thời lượng dự kiến phải lớn hơn 0 phút.", loiKhiLuu());

        nhapHopLe();
        SwingUtilities.invokeAndWait(() -> form.cboPhanLoaiThue.setSelectedIndex(0));
        assertEquals("Vui lòng chọn phân loại thuế.", loiKhiLuu());

        nhapHopLe();
        SwingUtilities.invokeAndWait(() -> form.txtTenDV.setText("a".repeat(DichVu.TEN_TOI_DA + 1)));
        assertTrue(loiKhiLuu().startsWith("Tên dịch vụ tối đa"));

        assertNull(daGui.get(), "Dữ liệu sai thì không gọi DAO");
    }

    @Test
    void chonPhanLoai_hienMucThueTuongUng() throws Exception {
        taoForm(dv -> true);
        SwingUtilities.invokeAndWait(() -> {
            form.cboPhanLoaiThue.setSelectedIndex(1);
            assertTrue(form.lblMucThue.getText().contains("KCT"));
            form.cboPhanLoaiThue.setSelectedIndex(2);
            assertTrue(form.lblMucThue.getText().endsWith("5%"));
            form.cboPhanLoaiThue.setSelectedIndex(3);
            assertTrue(form.lblMucThue.getText().endsWith("10%"));
        });
    }

    @Test
    void hopLe_guiDungDuLieu_luuNgoaiEdt_vaBaoThanhCong() throws Exception {
        AtomicBoolean ngoaiEdt = new AtomicBoolean();
        taoForm(dv -> {
            ngoaiEdt.set(!SwingUtilities.isEventDispatchThread());
            dv.setMaDV("DV007");
            return true;
        });
        nhapHopLe();
        SwingUtilities.invokeAndWait(() -> {
            form.txaLoTrinh.setText("Buổi 1: khám\nBuổi 2: cạo vôi");
            form.cboPhanLoaiThue.setSelectedIndex(3);
            form.luuDichVu();
        });
        choDen(() -> daLuu.get() != null);

        DichVu dv = daGui.get();
        assertTrue(ngoaiEdt.get());
        assertEquals("Cạo vôi răng", dv.getTenDV());
        assertEquals(300_000, dv.getDonGia());
        assertEquals(30, dv.getThoiLuong());
        assertEquals("Buổi 1: khám\nBuổi 2: cạo vôi", dv.getLoTrinhDichVu());
        assertEquals(LoaiDichVu.THAM_MY, dv.getLoaiDichVu().getTenLoaiDV());
        assertEquals(MucThue.THUE_10, dv.getLoaiDichVu().getMucThueGTGT());
        assertEquals("DV007", daLuu.get().getMaDV());
    }

    @Test
    void tenTrung_hienThongBaoDao_vaGiuForm() throws Exception {
        taoForm(dv -> {
            throw new BusinessException("Dịch vụ đã tồn tại. Vui lòng đổi tên dịch vụ.");
        });
        nhapHopLe();
        SwingUtilities.invokeAndWait(form::luuDichVu);
        choDen(() -> form.btnLuu.isEnabled());

        assertEquals("Dịch vụ đã tồn tại. Vui lòng đổi tên dịch vụ.", form.lblThongBao.getText());
        assertNull(daLuu.get());
        assertFalse(daDong.get());
    }

    @Test
    void loiDb_hienThongBaoChung() throws Exception {
        taoForm(dv -> {
            throw new DataAccessException("SQLState 08S01 chi tiết kỹ thuật", null);
        });
        nhapHopLe();
        SwingUtilities.invokeAndWait(form::luuDichVu);
        choDen(() -> form.btnLuu.isEnabled());

        assertEquals("Không thể lưu dữ liệu, vui lòng thử lại.", form.lblThongBao.getText());
    }

    // ---- Gợi ý lộ trình ----

    /** 7 lộ trình đã lưu, mới nhất trước (giống thứ tự DAO trả về). */
    private static final java.util.List<String> DA_LUU = java.util.List.of(
            "Buổi 1: lấy tủy\nBuổi 2: trám bít sau 7–14 ngày",
            "Buổi 1: khám\nBuổi 2: cạo vôi",
            "Buổi 1: lấy dấu\nBuổi 2: gắn sứ",
            "Buổi 1: tẩy trắng tại ghế",
            "Buổi 1: nhổ răng\nBuổi 2: cắt chỉ sau 7 ngày",
            "Buổi 1: chụp phim",
            "Buổi 1: tư vấn niềng");

    @Test
    void locGoiY_theoDoanDaGo_toiDa5_giuThuTuMoiNhat() {
        assertEquals(DA_LUU.subList(0, 5), DlgDichVu.NoiDung.locGoiY(DA_LUU, ""), "Ô trống: 5 cái mới nhất");
        assertEquals(java.util.List.of(DA_LUU.get(0), DA_LUU.get(1), DA_LUU.get(2), DA_LUU.get(4)),
                DlgDichVu.NoiDung.locGoiY(DA_LUU, "BUỔI 2"), "Không phân biệt hoa thường, giữ thứ tự mới nhất");
        assertEquals(java.util.List.of(DA_LUU.get(3)), DlgDichVu.NoiDung.locGoiY(DA_LUU, "tẩy trắng"));
        assertTrue(DlgDichVu.NoiDung.locGoiY(DA_LUU, DA_LUU.get(5)).isEmpty(), "Đã điền y hệt thì không gợi ý lại");
        assertTrue(DlgDichVu.NoiDung.locGoiY(DA_LUU, "không có").isEmpty());
    }

    @Test
    void moForm_hienGoiY_goThiLoc_bamThiDien() throws Exception {
        SwingUtilities.invokeAndWait(() -> form = new DlgDichVu.NoiDung(dv -> true, daLuu::set,
                () -> daDong.set(true), () -> DA_LUU));
        choDen(() -> demGoiY() == DlgDichVu.NoiDung.SO_GOI_Y);

        SwingUtilities.invokeAndWait(() -> form.txaLoTrinh.setText("cạo"));
        choDen(() -> demGoiY() == 1);

        SwingUtilities.invokeAndWait(() -> {
            javax.swing.JLabel khung = (javax.swing.JLabel) timGoiY(0);
            assertTrue(khung.getText().contains("Buổi 1: khám · Buổi 2: cạo vôi"), "Các buổi nối thành 1 dòng");
            // Giả lập bấm chuột vào khung gợi ý.
            for (java.awt.event.MouseListener l : khung.getMouseListeners()) {
                l.mouseClicked(new java.awt.event.MouseEvent(khung, java.awt.event.MouseEvent.MOUSE_CLICKED,
                        0, 0, 1, 1, 1, false));
            }
            assertEquals(DA_LUU.get(1), form.txaLoTrinh.getText());
            assertEquals(0, demGoiY(), "Đã điền đúng gợi ý thì gợi ý đó biến mất");
        });
    }

    @Test
    void loTrinhDeTrong_guiNullXuongDao() throws Exception {
        taoForm(dv -> true);
        nhapHopLe();
        SwingUtilities.invokeAndWait(() -> {
            form.txaLoTrinh.setText("   ");
            form.luuDichVu();
        });
        choDen(() -> daLuu.get() != null);
        assertNull(daGui.get().getLoTrinhDichVu(), "Lộ trình trống được lưu NULL");
    }

    private int demGoiY() {
        int dem = 0;
        for (java.awt.Component c : form.pnlGoiY.getComponents()) {
            if (c.getName() != null && c.getName().startsWith("goiY")) {
                dem++;
            }
        }
        return dem;
    }

    private java.awt.Component timGoiY(int thuTu) {
        for (java.awt.Component c : form.pnlGoiY.getComponents()) {
            if (("goiY" + thuTu).equals(c.getName())) {
                return c;
            }
        }
        throw new AssertionError("Thiếu khung gợi ý " + thuTu);
    }

    @Test
    void nutDong_huyThaoTac() throws Exception {
        taoForm(dv -> true);
        SwingUtilities.invokeAndWait(() -> form.btnDong.doClick());
        assertTrue(daDong.get());
        assertNull(daGui.get());
    }

    @Test
    void docDonGia_nhanNhieuCachNhap() {
        assertEquals(300_000, DlgDichVu.NoiDung.docDonGia("300.000"));
        assertEquals(300_000, DlgDichVu.NoiDung.docDonGia("300,000 ₫"));
        assertEquals(1_500_000, DlgDichVu.NoiDung.docDonGia(" 1 500 000đ "));
        assertNull(DlgDichVu.NoiDung.docDonGia("-5"));
        assertNull(DlgDichVu.NoiDung.docDonGia(""));
        assertNull(DlgDichVu.NoiDung.docDonGia("12a"));
    }

    private static void choDen(java.util.function.BooleanSupplier dieuKien) throws Exception {
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
