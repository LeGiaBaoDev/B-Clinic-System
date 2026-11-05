package com.quanlynhakhoa.ui.panel;

import static org.junit.jupiter.api.Assertions.*;

import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.DoanhThuMotNgay;
import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.TongHopDoanhThu;
import com.quanlynhakhoa.ui.panel.PnlBaoCao.KhoangNgay;
import com.quanlynhakhoa.ui.panel.PnlBaoCao.Ky;
import java.awt.Component;
import java.awt.Container;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/*
 * Trang Báo cáo hoạt động (không cần DB)
 * [x] Ngày / Tuần (thứ Hai → CN) / Tháng / Năm chứa ngày chọn; so với ngày trước / tuần trước / tháng trước / năm trước;
 *     Khoảng so với khoảng cùng số ngày ngay trước đó
 * [x] mở trang mặc định "Tuần": 1 ô chọn ngày, dòng mô tả ghi tuần đang xem và "so với tuần trước"
 * [x] Năm: ẩn hết ô lịch; Khoảng: hiện ô Từ – Đến, điền sẵn khoảng đang xem, đổi ngày thì tải lại
 * [x] không còn dòng "So sánh", ô chi nhánh, nút Xuất báo cáo; không chữ "kỳ" nào trên cả trang
 */
class PnlBaoCaoTest {

    private static final LocalDate THU_SAU = LocalDate.of(2026, 10, 16);
    private final List<LocalDate[]> daGoi = new ArrayList<>();
    /** Các khoảng ngày phần doanh thu được yêu cầu tải tổng (đang xem và trước đó xen kẽ). */
    private final List<LocalDate[]> doanhThuGoi = new ArrayList<>();
    private PnlBaoCao trang;

    private static Date date(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    @Test
    void khoangCua_vaKhoangTruoc() {
        assertEquals(new KhoangNgay(THU_SAU, THU_SAU), PnlBaoCao.khoangCua(Ky.NGAY, THU_SAU));
        assertEquals(new KhoangNgay(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 15)),
                PnlBaoCao.kyTruoc(Ky.NGAY, new KhoangNgay(THU_SAU, THU_SAU)));
        KhoangNgay tuan = PnlBaoCao.khoangCua(Ky.TUAN, THU_SAU);
        assertEquals(new KhoangNgay(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18)), tuan, "Thứ Hai → CN");
        assertEquals(new KhoangNgay(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 11)), PnlBaoCao.kyTruoc(Ky.TUAN, tuan));
        KhoangNgay thang = PnlBaoCao.khoangCua(Ky.THANG, THU_SAU);
        assertEquals(new KhoangNgay(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)), thang);
        assertEquals(new KhoangNgay(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)), PnlBaoCao.kyTruoc(Ky.THANG, thang),
                "Cả tháng 10 so với cả tháng 9");
        KhoangNgay nam = PnlBaoCao.khoangCua(Ky.NAM, THU_SAU);
        assertEquals(new KhoangNgay(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)), nam);
        assertEquals(new KhoangNgay(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)), PnlBaoCao.kyTruoc(Ky.NAM, nam));
        assertEquals(new KhoangNgay(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10)),
                PnlBaoCao.kyTruoc(Ky.KHOANG, new KhoangNgay(LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 20))),
                "Khoảng 10 ngày -> 10 ngày ngay trước");
        assertEquals("Báo cáo tháng 10/2026, so với tháng trước", PnlBaoCao.moTa(Ky.THANG, thang));
        assertEquals("Báo cáo năm 2026, so với năm trước", PnlBaoCao.moTa(Ky.NAM, nam));
        assertEquals("Báo cáo từ 11/10/2026 đến 20/10/2026, so với khoảng trước", PnlBaoCao.moTa(Ky.KHOANG,
                new KhoangNgay(LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 20))));
    }

    private void tao() throws Exception {
        PnlDoanhThuKy.Nguon nguonDoanhThu = new PnlDoanhThuKy.Nguon() {
            @Override
            public TongHopDoanhThu tongHop(LocalDate tu, LocalDate den) {
                synchronized (doanhThuGoi) {
                    doanhThuGoi.add(new LocalDate[]{tu, den});
                }
                return TongHopDoanhThu.RONG;
            }

            @Override
            public List<DoanhThuMotNgay> tungNgay(LocalDate tu, LocalDate den) {
                return List.of();
            }
        };
        SwingUtilities.invokeAndWait(() -> trang = new PnlBaoCao(new PnlThongKeDichVu((tu, den, tc) -> {
            synchronized (daGoi) {
                daGoi.add(new LocalDate[]{tu, den});
            }
            return List.of();
        }), new PnlDoanhThuKy(nguonDoanhThu), () -> LocalDateTime.of(2026, 10, 16, 9, 20)));
    }

    @Test
    void moTrang_macDinhTuan_motOChonNgay() throws Exception {
        tao();
        PnlThongKeDichVuTest.choDen(() -> !daGoi.isEmpty());
        assertArrayEquals(new LocalDate[]{LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18)}, daGoi.getLast());
        PnlThongKeDichVuTest.choDen(() -> doanhThuGoi.size() >= 2);
        synchronized (doanhThuGoi) {
            assertArrayEquals(new LocalDate[]{LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 11)},
                    doanhThuGoi.get(1), "So với tuần trước");
        }
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(trang.btnTuan.isSelected());
            assertTrue(trang.pnlMotNgay.isVisible());
            assertFalse(trang.pnlKhoang.isVisible());
            assertEquals("Báo cáo tuần 12/10 – 18/10/2026, so với tuần trước · Dữ liệu đến 09:20, 16/10/2026",
                    trang.lblPhuDe.getText());
        });
    }

    @Test
    void nam_anLich_khoang_hienTuDen() throws Exception {
        tao();
        PnlThongKeDichVuTest.choDen(() -> !daGoi.isEmpty());
        SwingUtilities.invokeAndWait(() -> trang.btnNam.doClick());
        PnlThongKeDichVuTest.choDen(() -> daGoi.getLast()[0].equals(LocalDate.of(2026, 1, 1)));
        SwingUtilities.invokeAndWait(() -> {
            assertFalse(trang.pnlMotNgay.isVisible() || trang.pnlKhoang.isVisible(), "Năm không có ô lịch");
            assertTrue(trang.btnNam.isSelected() && !trang.btnTuan.isSelected());
        });

        SwingUtilities.invokeAndWait(() -> trang.btnThang.doClick());
        SwingUtilities.invokeAndWait(() -> trang.btnKhoang.doClick());
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(trang.pnlKhoang.isVisible());
            assertFalse(trang.pnlMotNgay.isVisible());
            assertEquals(date(LocalDate.of(2026, 10, 1)), trang.dcTu.getDate(), "Điền sẵn tháng đang xem");
            trang.dcTu.setDate(date(LocalDate.of(2026, 10, 3)));
        });
        PnlThongKeDichVuTest.choDen(() -> daGoi.getLast()[0].equals(LocalDate.of(2026, 10, 3)));
        SwingUtilities.invokeAndWait(() -> assertTrue(trang.lblPhuDe.getText().contains("so với khoảng trước")));
    }

    @Test
    void khongConChuKy_soSanh_chiNhanh_xuatBaoCao() throws Exception {
        tao();
        PnlThongKeDichVuTest.choDen(() -> doanhThuGoi.size() >= 2);
        for (JLabelHoacNut nut : new JLabelHoacNut[]{() -> trang.btnNgay, () -> trang.btnTuan, () -> trang.btnThang,
            () -> trang.btnNam, () -> trang.btnKhoang}) {
            SwingUtilities.invokeAndWait(() -> nut.lay().doClick());
            Thread.sleep(150);
            SwingUtilities.invokeAndWait(() -> {
                List<String> chu = new ArrayList<>();
                gomChu(trang, chu, List.of()); // quét cả trang, gồm 3 thẻ lịch hẹn
                for (String c : chu) {
                    assertFalse(c.toLowerCase(Locale.ROOT).contains("kỳ"), "Còn chữ \"kỳ\": " + c);
                    assertFalse(c.startsWith("So sánh") || c.contains("Chi nhánh") || c.equals("Xuất báo cáo"), c);
                }
            });
        }
    }

    private interface JLabelHoacNut {
        AbstractButton lay();
    }

    private static void gomChu(Container c, List<String> chu, List<Container> boQua) {
        for (Component con : c.getComponents()) {
            if (boQua.contains(con)) {
                continue;
            }
            if (con instanceof JLabel l && l.getText() != null) {
                chu.add(l.getText());
            } else if (con instanceof AbstractButton b && b.getText() != null) {
                chu.add(b.getText());
            }
            if (con instanceof Container k) {
                gomChu(k, chu, boQua);
            }
        }
    }
}
