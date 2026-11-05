package com.quanlynhakhoa.ui.panel;

import static org.junit.jupiter.api.Assertions.*;

import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.DoanhThuMotNgay;
import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.TongHopDoanhThu;
import com.quanlynhakhoa.ui.panel.PnlBaoCao.KhoangNgay;
import com.quanlynhakhoa.ui.panel.PnlBaoCao.Ky;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/*
 * Trang Báo cáo hoạt động (không cần DB)
 * [x] kỳ Ngày / Tuần (từ thứ Hai) / Tháng (từ ngày 1) / Năm (từ 01/01) tính tới hôm nay; kỳ trước để so sánh
 * [x] phần doanh thu nhận cả kỳ này và kỳ trước
 * [x] mở trang mặc định "Tuần", truyền đúng khoảng ngày cho thẻ dịch vụ, ghi "Dữ liệu đến ..." và "So sánh: ..."
 * [x] đổi kỳ / tự chọn ngày trên lịch -> thẻ tải lại với khoảng mới, bỏ chọn nút kỳ
 */
class PnlBaoCaoTest {

    private static final LocalDate THU_SAU = LocalDate.of(2026, 10, 16); // như ảnh thiết kế
    private final List<LocalDate[]> daGoi = new ArrayList<>();
    private PnlBaoCao trang;

    @Test
    void khoangCuaKy_vaKyTruoc_nhuAnh() {
        assertEquals(new KhoangNgay(THU_SAU, THU_SAU), PnlBaoCao.khoangCuaKy(Ky.NGAY, THU_SAU));
        KhoangNgay tuan = PnlBaoCao.khoangCuaKy(Ky.TUAN, THU_SAU);
        assertEquals(new KhoangNgay(LocalDate.of(2026, 10, 12), THU_SAU), tuan, "Tuần tính từ thứ Hai");
        assertEquals(new KhoangNgay(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 9)), PnlBaoCao.kyTruoc(Ky.TUAN, tuan));
        KhoangNgay thang = PnlBaoCao.khoangCuaKy(Ky.THANG, THU_SAU);
        assertEquals(new KhoangNgay(LocalDate.of(2026, 10, 1), THU_SAU), thang);
        assertEquals(new KhoangNgay(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 16)), PnlBaoCao.kyTruoc(Ky.THANG, thang));
        assertEquals(new KhoangNgay(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 15)),
                PnlBaoCao.kyTruoc(Ky.NGAY, new KhoangNgay(THU_SAU, THU_SAU)));
        assertEquals(new KhoangNgay(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10)),
                PnlBaoCao.kyTruoc(Ky.TUY_CHINH, new KhoangNgay(LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 20))),
                "Tự chọn 10 ngày -> 10 ngày ngay trước");
    }

    /** Các khoảng ngày phần doanh thu được yêu cầu tải (kỳ này và kỳ trước xen kẽ). */
    private final List<LocalDate[]> doanhThuGoi = new ArrayList<>();

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
    void kyNam_vaTruyenKyTruocChoDoanhThu() throws Exception {
        assertEquals(new KhoangNgay(LocalDate.of(2026, 1, 1), THU_SAU), PnlBaoCao.khoangCuaKy(Ky.NAM, THU_SAU));
        assertEquals(new KhoangNgay(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 10, 16)),
                PnlBaoCao.kyTruoc(Ky.NAM, PnlBaoCao.khoangCuaKy(Ky.NAM, THU_SAU)), "So với cùng kỳ năm trước");
        tao();
        PnlThongKeDichVuTest.choDen(() -> doanhThuGoi.size() >= 2);
        synchronized (doanhThuGoi) {
            assertArrayEquals(new LocalDate[]{LocalDate.of(2026, 10, 12), THU_SAU}, doanhThuGoi.get(0), "Kỳ này");
            assertArrayEquals(new LocalDate[]{LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 9)},
                    doanhThuGoi.get(1), "Kỳ trước");
            doanhThuGoi.clear();
        }
        SwingUtilities.invokeAndWait(() -> trang.btnNam.doClick());
        PnlThongKeDichVuTest.choDen(() -> doanhThuGoi.size() >= 2);
        synchronized (doanhThuGoi) {
            assertArrayEquals(new LocalDate[]{LocalDate.of(2026, 1, 1), THU_SAU}, doanhThuGoi.get(0));
        }
        SwingUtilities.invokeAndWait(() -> assertTrue(trang.btnNam.isSelected() && !trang.btnTuan.isSelected()));
    }

    @Test
    void moTrang_macDinhTuan_truyenKhoangNgayChoThe() throws Exception {
        tao();
        PnlThongKeDichVuTest.choDen(() -> !daGoi.isEmpty());
        assertArrayEquals(new LocalDate[]{LocalDate.of(2026, 10, 12), THU_SAU}, daGoi.getLast());
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(trang.btnTuan.isSelected());
            assertEquals("Doanh thu đã thu và hiệu quả vận hành · Dữ liệu đến 09:20, 16/10/2026",
                    trang.lblPhuDe.getText());
            assertEquals("So sánh: 05/10 – 09/10/2026", trang.lblSoSanh.getText());
        });
    }

    @Test
    void doiKy_vaTuChonNgay_theTaiLai() throws Exception {
        tao();
        PnlThongKeDichVuTest.choDen(() -> !daGoi.isEmpty());
        SwingUtilities.invokeAndWait(() -> trang.btnThang.doClick());
        PnlThongKeDichVuTest.choDen(() -> daGoi.getLast()[0].equals(LocalDate.of(2026, 10, 1)));

        SwingUtilities.invokeAndWait(() -> {
            trang.dcTu.setDate(Date.from(LocalDate.of(2026, 10, 3).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            assertFalse(trang.btnNgay.isSelected() || trang.btnTuan.isSelected() || trang.btnThang.isSelected(),
                    "Tự chọn ngày thì bỏ chọn nút kỳ");
            assertEquals("So sánh: 19/09 – 02/10/2026", trang.lblSoSanh.getText(), "14 ngày -> 14 ngày ngay trước");
        });
        PnlThongKeDichVuTest.choDen(() -> daGoi.getLast()[0].equals(LocalDate.of(2026, 10, 3)));
    }
}
