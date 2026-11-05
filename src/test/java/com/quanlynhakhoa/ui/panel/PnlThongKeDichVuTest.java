package com.quanlynhakhoa.ui.panel;

import static org.junit.jupiter.api.Assertions.*;

import com.quanlynhakhoa.dao.ThongKeDichVuDAO.DichVuXepHang;
import com.quanlynhakhoa.dao.ThongKeDichVuDAO.TieuChi;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/*
 * Thẻ "Dịch vụ được sử dụng nhiều" (không cần DB, nguồn giả)
 * [x] nhận khoảng ngày từ trang, luôn xếp theo số lượt; chạy ngoài EDT
 * [x] bảng: hạng, mã, dịch vụ, số lượt, đơn giá, doanh thu (định dạng tiền)
 * [x] không có dữ liệu -> "Không có dữ liệu." giữa bảng; khoảng ngày sai -> thông báo của DAO; lỗi DB -> thông báo chung
 */
class PnlThongKeDichVuTest {

    private record Goi(LocalDate tu, LocalDate den, TieuChi tieuChi, boolean ngoaiEdt) { }

    private PnlThongKeDichVu the;
    private final List<Goi> daGoi = new ArrayList<>();
    private static final LocalDate TU = LocalDate.of(2026, 10, 12);
    private static final LocalDate DEN = LocalDate.of(2026, 10, 16);

    private static final List<DichVuXepHang> MAU = List.of(
            new DichVuXepHang(1, "DV001", "Cạo vôi răng", new BigDecimal("300000"), 28, new BigDecimal("7830000")),
            new DichVuXepHang(2, "DV002", "Trám răng", new BigDecimal("500000"), 18, new BigDecimal("8550000")));

    private void tao(PnlThongKeDichVu.XepHang nguon) throws Exception {
        SwingUtilities.invokeAndWait(() -> the = new PnlThongKeDichVu((tu, den, tc) -> {
            synchronized (daGoi) {
                daGoi.add(new Goi(tu, den, tc, !SwingUtilities.isEventDispatchThread()));
            }
            return nguon.xepHang(tu, den, tc);
        }));
    }

    @Test
    void taiTheoKhoangNgay_macDinhSoLuot_doiTieuChiThiTaiLai() throws Exception {
        tao((tu, den, tc) -> MAU);
        SwingUtilities.invokeAndWait(() -> the.taiDuLieu(TU, DEN));
        choDen(() -> the.lblThongBao.getText().startsWith("Top 2"));
        assertEquals(new Goi(TU, DEN, TieuChi.SO_LUOT, true), daGoi.getLast());
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(2, the.model.getRowCount());
            assertEquals(1, the.model.getValueAt(0, PnlThongKeDichVu.COT_HANG));
            assertEquals("DV001", the.model.getValueAt(0, PnlThongKeDichVu.COT_MA));
            assertEquals("Cạo vôi răng", the.model.getValueAt(0, PnlThongKeDichVu.COT_TEN));
            assertEquals(28L, the.model.getValueAt(0, PnlThongKeDichVu.COT_SO_LUOT));
            assertEquals("300.000 ₫", the.model.getValueAt(0, PnlThongKeDichVu.COT_DON_GIA));
            assertEquals("7.830.000 ₫", the.model.getValueAt(0, PnlThongKeDichVu.COT_DOANH_THU));
            assertTrue(the.lblThongBao.getText().contains("số lượt sử dụng"));
            assertTrue(the.lblThongBao.isVisible(), "Có dòng thì thông báo hiện dưới bảng");
        });
    }

    @Test
    void chuaCoKhoangNgay_khongGoiNguon() throws Exception {
        tao((tu, den, tc) -> MAU);
        Thread.sleep(100);
        assertTrue(daGoi.isEmpty());
    }

    @Test
    void khongCoDuLieu_khoangNgaySai_loiDb() throws Exception {
        tao((tu, den, tc) -> List.of());
        SwingUtilities.invokeAndWait(() -> the.taiDuLieu(TU, DEN));
        choDen(() -> the.lblThongBao.getText().equals("Không có dữ liệu."));
        SwingUtilities.invokeAndWait(() -> assertFalse(the.lblThongBao.isVisible(),
                "Bảng trống thì thông báo vẽ giữa bảng, không hiện dòng dưới"));

        tao((tu, den, tc) -> {
            throw new BusinessException("Khoảng thời gian không hợp lệ: ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        });
        SwingUtilities.invokeAndWait(() -> the.taiDuLieu(DEN, TU));
        choDen(() -> the.lblThongBao.getText().startsWith("Khoảng thời gian không hợp lệ"));

        tao((tu, den, tc) -> {
            throw new DataAccessException("timeout", null);
        });
        SwingUtilities.invokeAndWait(() -> the.taiDuLieu(TU, DEN));
        choDen(() -> the.lblThongBao.getText().equals("Không thể tải thống kê dịch vụ. Vui lòng thử lại."));
    }

    static void choDen(BooleanSupplier dk) throws Exception {
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
}
