package com.quanlynhakhoa.ui.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quanlynhakhoa.dao.ThongKeLichHenDAO.TongHop;
import com.quanlynhakhoa.entity.NhanVien;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;
import com.quanlynhakhoa.ui.panel.PnlBaoCao.KhoangNgay;
import java.awt.Color;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/*
 * UC029 – thẻ lịch hẹn trong trang Báo cáo của Quản lý (không cần DB, nguồn giả)
 * [x] thẻ hoàn tất: số, tỷ lệ chuyển đổi, so với tuần trước
 * [x] thẻ hủy / không đến: số, chi tiết, so với tuần trước (tăng = đỏ vì xấu hơn)
 * [x] biểu đồ tròn + chú thích 5 trạng thái; lọc theo nha sĩ; không dữ liệu / lỗi -> thông báo
 */
class PnlThongKeLichHenKyTest {

    private static final KhoangNgay NAY = new KhoangNgay(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 16));
    private static final KhoangNgay TRUOC = new KhoangNgay(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 9));
    private static final Color DO = new Color(0xC94C48);

    private final List<String[]> daGoi = new ArrayList<>();
    private PnlThongKeLichHenKy the;

    private interface TinhToan {

        TongHop tinh(LocalDate tu, LocalDate den, String maNhaSi);
    }

    private void tao(TinhToan tinh) throws Exception {
        SwingUtilities.invokeAndWait(() -> the = new PnlThongKeLichHenKy(new PnlThongKeLichHenKy.Nguon() {
            @Override
            public TongHop tongHop(LocalDate tu, LocalDate den, String maNhaSi) {
                synchronized (daGoi) {
                    daGoi.add(new String[]{tu.toString(), den.toString(), maNhaSi});
                }
                return tinh.tinh(tu, den, maNhaSi);
            }

            @Override
            public List<NhanVien> danhSachNhaSi() {
                return List.of(LichHenMau.nhaSi("NV002", "Trần Dũng", "0398682231"),
                        LichHenMau.nhaSi("NV004", "Lê Hà", "0398682232"));
            }
        }));
    }

    /** Kỳ này 10 lịch: 1 đặt, 1 đến, 5 hoàn tất, 2 hủy, 1 không đến. Kỳ trước 8 lịch: 4 hoàn tất, 3 hủy + không đến. */
    private static TongHop mau(LocalDate tu, LocalDate den, String maNhaSi) {
        return tu.equals(NAY.tu()) ? new TongHop(10, 1, 1, 5, 2, 1) : new TongHop(8, 1, 0, 4, 2, 1);
    }

    @Test
    void theHoanTat_soTyLeChuyenDoiVaSoVoiKyTruoc() throws Exception {
        tao(PnlThongKeLichHenKyTest::mau);
        SwingUtilities.invokeAndWait(() -> the.taiDuLieu(NAY, TRUOC, PnlBaoCao.Ky.TUAN));
        PnlThongKeDichVuTest.choDen(() -> the.lblHoanTat.getText().equals("5"));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("Chuyển đổi 50,0% tổng 10 lịch", the.lblChuyenDoi.getText());
            assertEquals("↑ 25,0% so với tuần trước (4 lịch)", the.lblHoanTatSoVoiTruoc.getText(), "Hoàn tất tăng 4 -> 5");
        });
    }

    @Test
    void theHuyKhongDen_bangKyTruoc_hienKhongPhanTram() throws Exception {
        tao(PnlThongKeLichHenKyTest::mau);
        SwingUtilities.invokeAndWait(() -> the.taiDuLieu(NAY, TRUOC, PnlBaoCao.Ky.TUAN));
        PnlThongKeDichVuTest.choDen(() -> the.lblHuyKhongDen.getText().equals("3"));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("Hủy 2 · Không đến 1", the.lblChiTietHuy.getText());
            assertEquals("0,0% so với tuần trước (3 lịch)", the.lblHuySoVoiTruoc.getText(), "Cùng 3 nên thay đổi 0,0%");
        });
    }

    @Test
    void theHuyKhongDen_tangSoVoiKyTruoc_toDo() throws Exception {
        tao((tu, den, ns) -> tu.equals(NAY.tu()) ? new TongHop(10, 0, 0, 5, 3, 2) : new TongHop(8, 0, 0, 5, 1, 2));
        SwingUtilities.invokeAndWait(() -> the.taiDuLieu(NAY, TRUOC, PnlBaoCao.Ky.TUAN));
        PnlThongKeDichVuTest.choDen(() -> the.lblHuyKhongDen.getText().equals("5"));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("↑ 66,7% so với tuần trước (3 lịch)", the.lblHuySoVoiTruoc.getText());
            assertEquals(DO, the.lblHuySoVoiTruoc.getForeground());
        });
    }

    @Test
    void bieuDoTronVaChuThich_namTrangThai() throws Exception {
        tao(PnlThongKeLichHenKyTest::mau);
        SwingUtilities.invokeAndWait(() -> the.taiDuLieu(NAY, TRUOC, PnlBaoCao.Ky.TUAN));
        PnlThongKeDichVuTest.choDen(() -> the.duLieuBieuDo.getItemCount() == 5);
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(5.0, the.duLieuBieuDo.getValue("Hoàn tất").doubleValue());
            assertEquals(2.0, the.duLieuBieuDo.getValue("Đã hủy").doubleValue());
            assertEquals("5 · 50,0%", the.lblSoLuong[0].getText());
            assertEquals("2 · 20,0%", the.lblSoLuong[3].getText());
            assertEquals("10 lịch hẹn trong tuần", the.lblTongDaKetThuc.getText());
        });
    }

    @Test
    void trangThaiBangKhong_khongCoLatCatTrenBieuDo() throws Exception {
        tao((tu, den, ns) -> new TongHop(3, 0, 0, 2, 1, 0));
        SwingUtilities.invokeAndWait(() -> the.taiDuLieu(NAY, TRUOC, PnlBaoCao.Ky.TUAN));
        PnlThongKeDichVuTest.choDen(() -> the.duLieuBieuDo.getItemCount() == 2);
        SwingUtilities.invokeAndWait(() -> assertEquals("0 · 0,0%", the.lblSoLuong[1].getText()));
    }

    @Test
    void khongCoDuLieu_baoKhongCoDuLieu() throws Exception {
        tao((tu, den, ns) -> new TongHop(0, 0, 0, 0, 0, 0));
        SwingUtilities.invokeAndWait(() -> the.taiDuLieu(NAY, TRUOC, PnlBaoCao.Ky.TUAN));
        PnlThongKeDichVuTest.choDen(() -> the.lblThongBao.getText().equals(PnlThongKeLichHenKy.khongCoDuLieu(PnlBaoCao.Ky.TUAN)));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("0", the.lblHoanTat.getText());
            assertEquals("Tuần trước: 0 lịch", the.lblHoanTatSoVoiTruoc.getText());
            assertEquals(0, the.duLieuBieuDo.getItemCount());
        });
    }

    @Test
    void locTheoNhaSi_truyenMaNhaSiChoNguon() throws Exception {
        tao(PnlThongKeLichHenKyTest::mau);
        SwingUtilities.invokeAndWait(() -> the.taiDuLieu(NAY, TRUOC, PnlBaoCao.Ky.TUAN));
        PnlThongKeDichVuTest.choDen(() -> the.cboNhaSi.getItemCount() == 3);
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(PnlThongKeLichHenKy.TAT_CA, the.cboNhaSi.getItemAt(0).toString());
            the.cboNhaSi.setSelectedIndex(1);
        });
        PnlThongKeDichVuTest.choDen(() -> {
            synchronized (daGoi) {
                return daGoi.stream().anyMatch(g -> "NV002".equals(g[2]));
            }
        });
        synchronized (daGoi) {
            assertNull(daGoi.getFirst()[2], "Lần đầu tải tất cả nha sĩ");
        }
    }

    @Test
    void loiNghiepVuVaLoiDb_khongLoChiTiet() throws Exception {
        tao((tu, den, ns) -> {
            throw new BusinessException("Khoảng ngày không hợp lệ.");
        });
        SwingUtilities.invokeAndWait(() -> the.taiDuLieu(NAY, TRUOC, PnlBaoCao.Ky.TUAN));
        PnlThongKeDichVuTest.choDen(() -> the.lblThongBao.getText().equals("Khoảng ngày không hợp lệ."));

        tao((tu, den, ns) -> {
            throw new DataAccessException("secret-sql", null);
        });
        SwingUtilities.invokeAndWait(() -> the.taiDuLieu(NAY, TRUOC, PnlBaoCao.Ky.TUAN));
        PnlThongKeDichVuTest.choDen(() ->
                the.lblThongBao.getText().equals("Không thể tải lịch hẹn. Vui lòng thử lại."));
        SwingUtilities.invokeAndWait(() -> assertEquals("—", the.lblHoanTat.getText()));
    }

    @Test
    void baoLoi_xoaSoLieuVaBieuDo() throws Exception {
        tao(PnlThongKeLichHenKyTest::mau);
        SwingUtilities.invokeAndWait(() -> the.taiDuLieu(NAY, TRUOC, PnlBaoCao.Ky.TUAN));
        PnlThongKeDichVuTest.choDen(() -> the.duLieuBieuDo.getItemCount() == 5);
        SwingUtilities.invokeAndWait(() -> {
            the.baoLoi("Vui lòng chọn đủ ngày bắt đầu và ngày kết thúc.");
            assertEquals("—", the.lblHuyKhongDen.getText());
            assertEquals(0, the.duLieuBieuDo.getItemCount());
            assertTrue(the.lblThongBao.getText().startsWith("Vui lòng chọn đủ ngày"));
        });
    }
}
