package com.quanlynhakhoa.ui.panel;

import static org.junit.jupiter.api.Assertions.*;

import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.DoanhThuMotNgay;
import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.TongHopDoanhThu;
import com.quanlynhakhoa.exception.DataAccessException;
import com.quanlynhakhoa.ui.panel.PnlBaoCao.KhoangNgay;
import com.quanlynhakhoa.ui.panel.PnlDoanhThuKy.Cot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/*
 * Phần doanh thu trên trang Báo cáo (không cần DB, nguồn giả)
 * [x] % thay đổi: tăng "+", giảm "−", kỳ trước = 0 -> "—"
 * [x] gộp cột biểu đồ: ≤ 31 ngày theo ngày, ≤ 120 ngày theo tuần, dài hơn theo tháng; ghép kỳ trước theo thứ tự
 * [x] tải 2 kỳ: thẻ "Doanh thu đã thu" + dòng so với kỳ trước, bảng chi tiết 7 chỉ tiêu, biểu đồ có cột
 * [x] kỳ không có hóa đơn -> "Không có dữ liệu"; lỗi DB -> thông báo chung
 */
class PnlDoanhThuKyTest {

    private static final KhoangNgay NAY = new KhoangNgay(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 16));
    private static final KhoangNgay TRUOC = new KhoangNgay(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 9));

    private static BigDecimal d(long v) {
        return BigDecimal.valueOf(v);
    }

    private static List<DoanhThuMotNgay> ngay(LocalDate tu, int soNgay, long moiNgay) {
        List<DoanhThuMotNgay> ds = new ArrayList<>();
        for (int i = 0; i < soNgay; i++) {
            ds.add(new DoanhThuMotNgay(tu.plusDays(i), d(moiNgay)));
        }
        return ds;
    }

    @Test
    void thayDoi_tangGiamVaKyTruocBang0() {
        assertEquals("+12,4%", PnlDoanhThuKy.thayDoi(d(65_180_000), d(73_250_000)));
        assertEquals("−20,0%", PnlDoanhThuKy.thayDoi(d(10), d(8)));
        assertEquals("0,0%", PnlDoanhThuKy.thayDoi(d(5), d(5)));
        assertEquals("—", PnlDoanhThuKy.thayDoi(BigDecimal.ZERO, d(5)), "Kỳ trước 0 thì không tính được %");
    }

    @Test
    void gopCot_theoNgayTuanThang() {
        List<Cot> theoNgay = PnlDoanhThuKy.gopCot(ngay(NAY.tu(), 5, 100), ngay(TRUOC.tu(), 5, 50), 5);
        assertEquals(5, theoNgay.size());
        assertEquals(new Cot("12/10", d(100), d(50)), theoNgay.get(0));

        LocalDate dau = LocalDate.of(2026, 8, 1);
        List<Cot> theoTuan = PnlDoanhThuKy.gopCot(ngay(dau, 60, 10), ngay(dau.minusDays(60), 55, 1), 60);
        assertEquals(9, theoTuan.size(), "60 ngày -> 8 tuần đủ + 1 tuần lẻ 4 ngày");
        assertEquals(new Cot("01/08", d(70), d(7)), theoTuan.get(0));
        assertEquals(d(40), theoTuan.get(8).nay());
        assertEquals(BigDecimal.ZERO, theoTuan.get(8).truoc(), "Kỳ trước ngắn hơn thì phần thiếu tính 0");

        LocalDate dauNam = LocalDate.of(2026, 1, 1);
        List<Cot> theoThang = PnlDoanhThuKy.gopCot(ngay(dauNam, 289, 1), ngay(dauNam.minusYears(1), 289, 2), 289);
        assertEquals(10, theoThang.size(), "01/01 – 16/10 -> 10 tháng");
        assertEquals(new Cot("01/2026", d(31), d(62)), theoThang.get(0));
        assertEquals(new Cot("02/2026", d(28), d(56)), theoThang.get(1));
        assertEquals(d(16), theoThang.get(9).nay(), "Tháng 10 mới tới ngày 16");
    }

    @Test
    void soDepChoTrucDoc() {
        assertEquals(20, PnlDoanhThuKy.BieuDoCot.soDep(18.45));
        assertEquals(2.5, PnlDoanhThuKy.BieuDoCot.soDep(2.2));
        assertEquals(1, PnlDoanhThuKy.BieuDoCot.soDep(0));
    }

    @Test
    void taiHaiKy_hienTheVaBang() throws Exception {
        TongHopDoanhThu nay = new TongHopDoanhThu(12, d(70_000_000), d(5_250_000), d(2_000_000),
                d(73_250_000), d(40_000_000), d(33_250_000));
        TongHopDoanhThu truoc = new TongHopDoanhThu(10, d(62_000_000), d(4_180_000), d(1_000_000),
                d(65_180_000), d(40_000_000), d(25_180_000));
        PnlDoanhThuKy[] p = new PnlDoanhThuKy[1];
        SwingUtilities.invokeAndWait(() -> {
            p[0] = new PnlDoanhThuKy(new PnlDoanhThuKy.Nguon() {
                @Override
                public TongHopDoanhThu tongHop(LocalDate tu, LocalDate den) {
                    return tu.equals(NAY.tu()) ? nay : truoc;
                }

                @Override
                public List<DoanhThuMotNgay> tungNgay(LocalDate tu, LocalDate den) {
                    return ngay(tu, 5, 7_000_000);
                }
            });
            p[0].taiDuLieu(NAY, TRUOC);
        });
        PnlThongKeDichVuTest.choDen(() -> p[0].lblDoanhThu.getText().equals("73.250.000 ₫"));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("↑ 12,4% · Kỳ trước: 65.180.000 ₫", p[0].lblSoVoiKyTruoc.getText());
            assertEquals("12/10 – 16/10/2026", p[0].lblChuThichNay.getText());
            assertEquals(PnlDoanhThuKy.CHI_TIEU.length, p[0].model.getRowCount());
            assertEquals("Thực nhận", p[0].model.getValueAt(4, 0));
            assertEquals("65.180.000", p[0].model.getValueAt(4, PnlDoanhThuKy.COT_KY_TRUOC));
            assertEquals("73.250.000", p[0].model.getValueAt(4, PnlDoanhThuKy.COT_KY_NAY));
            assertEquals("+12,4%", p[0].model.getValueAt(4, PnlDoanhThuKy.COT_THAY_DOI));
            assertEquals("0,0%", p[0].model.getValueAt(5, PnlDoanhThuKy.COT_THAY_DOI), "Tiền mặt không đổi");
            assertEquals("12", p[0].model.getValueAt(0, PnlDoanhThuKy.COT_KY_NAY), "Số hóa đơn");
            assertEquals(5, p[0].bieuDo.cot().size());
            assertNull(p[0].bieuDo.thongBao());
        });
    }

    @Test
    void khongCoHoaDon_vaLoiDb() throws Exception {
        PnlDoanhThuKy[] p = new PnlDoanhThuKy[1];
        SwingUtilities.invokeAndWait(() -> {
            p[0] = new PnlDoanhThuKy(new PnlDoanhThuKy.Nguon() {
                @Override
                public TongHopDoanhThu tongHop(LocalDate tu, LocalDate den) {
                    return TongHopDoanhThu.RONG;
                }

                @Override
                public List<DoanhThuMotNgay> tungNgay(LocalDate tu, LocalDate den) {
                    return ngay(tu, 5, 0);
                }
            });
            p[0].taiDuLieu(NAY, TRUOC);
        });
        PnlThongKeDichVuTest.choDen(() -> p[0].lblThongBaoChiTiet.getText().equals("Không có dữ liệu trong kỳ này."));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("0 ₫", p[0].lblDoanhThu.getText());
            assertEquals("Kỳ trước: 0 ₫", p[0].lblSoVoiKyTruoc.getText());
            assertEquals("Không có dữ liệu.", p[0].bieuDo.thongBao());
        });

        SwingUtilities.invokeAndWait(() -> {
            p[0] = new PnlDoanhThuKy(new PnlDoanhThuKy.Nguon() {
                @Override
                public TongHopDoanhThu tongHop(LocalDate tu, LocalDate den) {
                    throw new DataAccessException("timeout", null);
                }

                @Override
                public List<DoanhThuMotNgay> tungNgay(LocalDate tu, LocalDate den) {
                    return List.of();
                }
            });
            p[0].taiDuLieu(NAY, TRUOC);
        });
        PnlThongKeDichVuTest.choDen(() ->
                p[0].lblThongBaoChiTiet.getText().equals("Không thể tải doanh thu. Vui lòng thử lại."));
        SwingUtilities.invokeAndWait(() -> assertEquals("—", p[0].lblDoanhThu.getText()));
    }
}
