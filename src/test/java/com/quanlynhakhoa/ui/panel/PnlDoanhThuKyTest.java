package com.quanlynhakhoa.ui.panel;

import static org.junit.jupiter.api.Assertions.*;

import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.DoanhThuMotNgay;
import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.TongHopDoanhThu;
import com.quanlynhakhoa.exception.DataAccessException;
import com.quanlynhakhoa.ui.panel.PnlBaoCao.KhoangNgay;
import com.quanlynhakhoa.ui.panel.PnlBaoCao.Ky;
import com.quanlynhakhoa.ui.panel.PnlDoanhThuKy.Cot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/*
 * Phần doanh thu trên trang Báo cáo (không cần DB, nguồn giả)
 * [x] % thay đổi: tăng "+", giảm "−", trước đó = 0 -> "—"
 * [x] dòng dưới số doanh thu: "so với ngày trước / tuần trước / tháng trước / năm trước / khoảng trước"
 * [x] biểu đồ: Ngày 1 cặp cột 2 màu; Tuần 7 cột Thứ 2 → CN; Tháng 12 cột Th1 → Th12; Năm 5 cột; Khoảng theo ngày
 * [x] gộp cột của Khoảng: ≤ 31 ngày theo ngày, ≤ 120 ngày theo tuần, dài hơn theo tháng
 * [x] tải: thẻ "Doanh thu đã thu", tiêu đề + chú thích biểu đồ đổi theo nút; chỉ Ngày có chú thích màu nhạt
 * [x] không có hóa đơn -> "Không có dữ liệu."; lỗi DB -> thông báo chung
 */
class PnlDoanhThuKyTest {

    private static final KhoangNgay NAY = new KhoangNgay(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18));
    private static final KhoangNgay TRUOC = new KhoangNgay(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 11));

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
    void thayDoi_tangGiamVaTruocDoBang0() {
        assertEquals("+12,4%", PnlDoanhThuKy.thayDoi(d(65_180_000), d(73_250_000)));
        assertEquals("−20,0%", PnlDoanhThuKy.thayDoi(d(10), d(8)));
        assertEquals("0,0%", PnlDoanhThuKy.thayDoi(d(5), d(5)));
        assertEquals("—", PnlDoanhThuKy.thayDoi(BigDecimal.ZERO, d(5)), "Trước đó 0 thì không tính được %");
    }

    @Test
    void dongSoVoiTruoc_ghiTheoNutDangChon() {
        assertEquals("↑ 12,4% so với ngày trước (100 ₫)", PnlDoanhThuKy.soVoiTruoc("+12,4%", Ky.NGAY.truoc(), d(100)));
        assertEquals("↓ 3,0% so với tháng trước (100 ₫)", PnlDoanhThuKy.soVoiTruoc("−3,0%", Ky.THANG.truoc(), d(100)));
        assertEquals("0,0% so với năm trước (5 ₫)", PnlDoanhThuKy.soVoiTruoc("0,0%", Ky.NAM.truoc(), d(5)));
        assertEquals("Tuần trước: 0 ₫", PnlDoanhThuKy.soVoiTruoc("—", Ky.TUAN.truoc(), BigDecimal.ZERO));
        assertEquals("Khoảng trước: 0 ₫", PnlDoanhThuKy.soVoiTruoc("—", Ky.KHOANG.truoc(), BigDecimal.ZERO));
        assertEquals("Doanh thu theo tuần", Ky.TUAN.tieuDeBieuDo());
        assertEquals("Doanh thu theo khoảng ngày", Ky.KHOANG.tieuDeBieuDo());
    }

    @Test
    void cotBieuDo_theoTungNut() {
        LocalDate ngay16 = LocalDate.of(2026, 10, 16);
        KhoangNgay motNgay = new KhoangNgay(ngay16, ngay16);
        assertNull(PnlDoanhThuKy.khoangBieuDo(Ky.NGAY, motNgay), "Ngày dùng luôn 2 số tổng");
        assertEquals(List.of(new Cot("16/10/2026", d(9), d(4))),
                PnlDoanhThuKy.cotBieuDo(Ky.NGAY, motNgay, List.of(), d(9), d(4)));

        // Tuần: 7 cột Thứ 2 → CN, cột không có tiền = 0
        assertEquals(NAY, PnlDoanhThuKy.khoangBieuDo(Ky.TUAN, new KhoangNgay(NAY.tu(), ngay16)));
        List<Cot> tuan = PnlDoanhThuKy.cotBieuDo(Ky.TUAN, NAY, ngay(NAY.tu(), 5, 10), d(50), d(0));
        assertEquals(7, tuan.size());
        assertEquals(new Cot("Thứ 2", d(10), BigDecimal.ZERO), tuan.get(0));
        assertEquals(new Cot("CN", BigDecimal.ZERO, BigDecimal.ZERO), tuan.get(6));

        // Tháng: 12 cột Th1 → Th12 của cả năm
        KhoangNgay thang10 = new KhoangNgay(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        KhoangNgay ca2026 = PnlDoanhThuKy.khoangBieuDo(Ky.THANG, thang10);
        assertEquals(new KhoangNgay(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)), ca2026);
        List<Cot> thang = PnlDoanhThuKy.cotBieuDo(Ky.THANG, thang10, ngay(ca2026.tu(), 365, 1), d(0), d(0));
        assertEquals(12, thang.size());
        assertEquals(new Cot("Th1", d(31), BigDecimal.ZERO), thang.get(0));
        assertEquals(new Cot("Th2", d(28), BigDecimal.ZERO), thang.get(1));
        assertEquals("Th12", thang.get(11).nhan());

        // Năm: 5 năm tới năm đang xem
        KhoangNgay nam2026 = new KhoangNgay(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals(LocalDate.of(2022, 1, 1), PnlDoanhThuKy.khoangBieuDo(Ky.NAM, nam2026).tu());
        List<Cot> nam = PnlDoanhThuKy.cotBieuDo(Ky.NAM, nam2026,
                List.of(new DoanhThuMotNgay(LocalDate.of(2025, 3, 1), d(7)),
                        new DoanhThuMotNgay(LocalDate.of(2026, 3, 1), d(8))), d(0), d(0));
        assertEquals(List.of("2022", "2023", "2024", "2025", "2026"), nam.stream().map(Cot::nhan).toList());
        assertEquals(d(7), nam.get(3).nay());
        assertEquals(d(8), nam.get(4).nay());
        assertEquals("Năm 2022 – 2026", PnlDoanhThuKy.chuThichBieuDo(Ky.NAM, nam2026));
        assertEquals("Tuần 12/10 – 18/10/2026", PnlDoanhThuKy.chuThichBieuDo(Ky.TUAN, NAY));
    }

    @Test
    void gopCot_theoNgayTuanThang() {
        List<Cot> theoNgay = PnlDoanhThuKy.gopCot(ngay(NAY.tu(), 5, 100), List.of(), 5);
        assertEquals(5, theoNgay.size());
        assertEquals(new Cot("12/10", d(100), BigDecimal.ZERO), theoNgay.get(0));

        LocalDate dau = LocalDate.of(2026, 8, 1);
        List<Cot> theoTuan = PnlDoanhThuKy.gopCot(ngay(dau, 60, 10), List.of(), 60);
        assertEquals(9, theoTuan.size(), "60 ngày -> 8 tuần đủ + 1 tuần lẻ 4 ngày");
        assertEquals(new Cot("01/08", d(70), BigDecimal.ZERO), theoTuan.get(0));
        assertEquals(d(40), theoTuan.get(8).nay());

        LocalDate dauNam = LocalDate.of(2026, 1, 1);
        List<Cot> theoThang = PnlDoanhThuKy.gopCot(ngay(dauNam, 289, 1), List.of(), 289);
        assertEquals(10, theoThang.size(), "01/01 – 16/10 -> 10 tháng");
        assertEquals(new Cot("01/2026", d(31), BigDecimal.ZERO), theoThang.get(0));
        assertEquals(d(16), theoThang.get(9).nay(), "Tháng 10 mới tới ngày 16");
    }

    @Test
    void soDepChoTrucDoc() {
        assertEquals(20, PnlDoanhThuKy.BieuDoCot.soDep(18.45));
        assertEquals(2.5, PnlDoanhThuKy.BieuDoCot.soDep(2.2));
        assertEquals(1, PnlDoanhThuKy.BieuDoCot.soDep(0));
    }

    private static PnlDoanhThuKy.Nguon nguon(TongHopDoanhThu nay, TongHopDoanhThu truoc) {
        return new PnlDoanhThuKy.Nguon() {
            @Override
            public TongHopDoanhThu tongHop(LocalDate tu, LocalDate den) {
                return tu.equals(TRUOC.tu()) || tu.equals(LocalDate.of(2026, 10, 15)) ? truoc : nay;
            }

            @Override
            public List<DoanhThuMotNgay> tungNgay(LocalDate tu, LocalDate den) {
                return ngay(tu, 7, 7_000_000);
            }
        };
    }

    @Test
    void taiTheoTuan_motMau_tieuDeTheoTuan() throws Exception {
        TongHopDoanhThu nay = new TongHopDoanhThu(12, d(70_000_000), d(5_250_000), d(2_000_000),
                d(73_250_000), d(40_000_000), d(33_250_000));
        TongHopDoanhThu truoc = new TongHopDoanhThu(10, d(62_000_000), d(4_180_000), d(1_000_000),
                d(65_180_000), d(40_000_000), d(25_180_000));
        PnlDoanhThuKy[] p = new PnlDoanhThuKy[1];
        SwingUtilities.invokeAndWait(() -> {
            p[0] = new PnlDoanhThuKy(nguon(nay, truoc));
            p[0].taiDuLieu(NAY, TRUOC, Ky.TUAN);
        });
        PnlThongKeDichVuTest.choDen(() -> p[0].lblDoanhThu.getText().equals("73.250.000 ₫"));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("↑ 12,4% so với tuần trước (65.180.000 ₫)", p[0].lblSoVoiTruoc.getText());
            assertEquals("Doanh thu theo tuần", p[0].lblTieuDeBieuDo.getText());
            assertEquals("Tuần 12/10 – 18/10/2026", p[0].lblChuThichNay.getText());
            assertFalse(p[0].lblChuThichTruoc.isVisible(), "Tuần chỉ 1 màu");
            assertFalse(p[0].bieuDo.haiMau());
            assertEquals(7, p[0].bieuDo.cot().size());
            assertNull(p[0].bieuDo.thongBao());
        });
    }

    @Test
    void taiTheoNgay_haiMau() throws Exception {
        LocalDate ngay16 = LocalDate.of(2026, 10, 16);
        TongHopDoanhThu nay = new TongHopDoanhThu(2, d(9_000_000), d(0), d(0), d(9_000_000), d(9_000_000), d(0));
        TongHopDoanhThu truoc = new TongHopDoanhThu(1, d(4_000_000), d(0), d(0), d(4_000_000), d(4_000_000), d(0));
        PnlDoanhThuKy[] p = new PnlDoanhThuKy[1];
        SwingUtilities.invokeAndWait(() -> {
            p[0] = new PnlDoanhThuKy(nguon(nay, truoc));
            p[0].taiDuLieu(new KhoangNgay(ngay16, ngay16), new KhoangNgay(ngay16.minusDays(1), ngay16.minusDays(1)),
                    Ky.NGAY);
        });
        PnlThongKeDichVuTest.choDen(() -> p[0].lblDoanhThu.getText().equals("9.000.000 ₫"));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("↑ 125,0% so với ngày trước (4.000.000 ₫)", p[0].lblSoVoiTruoc.getText());
            assertEquals("Doanh thu theo ngày", p[0].lblTieuDeBieuDo.getText());
            assertEquals("16/10/2026", p[0].lblChuThichNay.getText());
            assertEquals("15/10/2026", p[0].lblChuThichTruoc.getText());
            assertTrue(p[0].lblChuThichTruoc.isVisible(), "Chỉ Ngày có màu nhạt");
            assertTrue(p[0].bieuDo.haiMau());
            assertEquals(List.of(new Cot("16/10/2026", d(9_000_000), d(4_000_000))), p[0].bieuDo.cot());
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
                    return ngay(tu, 7, 0);
                }
            });
            p[0].taiDuLieu(NAY, TRUOC, Ky.TUAN);
        });
        PnlThongKeDichVuTest.choDen(() -> "Không có dữ liệu.".equals(p[0].bieuDo.thongBao()));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("0 ₫", p[0].lblDoanhThu.getText());
            assertEquals("Tuần trước: 0 ₫", p[0].lblSoVoiTruoc.getText());
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
            p[0].taiDuLieu(NAY, TRUOC, Ky.TUAN);
        });
        PnlThongKeDichVuTest.choDen(() -> "Không thể tải doanh thu. Vui lòng thử lại.".equals(p[0].bieuDo.thongBao()));
        SwingUtilities.invokeAndWait(() -> assertEquals("—", p[0].lblDoanhThu.getText()));
    }
}
