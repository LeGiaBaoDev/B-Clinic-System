package com.quanlynhakhoa.ui.panel;

import static org.junit.jupiter.api.Assertions.*;

import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.DoanhThuTrongNgay;
import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.HoaDonTrongNgay;
import com.quanlynhakhoa.entity.HoaDon;
import com.quanlynhakhoa.exception.DataAccessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/*
 * Trang doanh thu trong ngày của Lễ tân (không cần DB, nguồn giả)
 * [x] mở trang mặc định hôm nay; chọn ngày khác thì tải lại; nút "Hôm nay" quay về hôm nay
 * [x] 4 thẻ: tạm tính (+ số hóa đơn, chưa thu), đã thu, tiền mặt, chuyển khoản; bảng từng hóa đơn
 * [x] không có hóa đơn -> số 0 và "Chưa có hóa đơn trong ngày này"; lỗi DB -> thông báo chung
 */
class PnlDoanhThuNgayTest {

    private static final LocalDate HOM_NAY = LocalDate.of(2026, 11, 5);
    private final List<LocalDate> daGoi = new ArrayList<>();
    private PnlDoanhThuNgay trang;

    private static final DoanhThuTrongNgay MAU = new DoanhThuTrongNgay(HOM_NAY, List.of(
            new HoaDonTrongNgay("HD261105003", HOM_NAY.atTime(10, 15), "Trần Bình", false, null,
                    new BigDecimal("300000")),
            new HoaDonTrongNgay("HD261105002", HOM_NAY.atTime(9, 30), "Lê Chi", true, HoaDon.CHUYEN_KHOAN,
                    new BigDecimal("1785000")),
            new HoaDonTrongNgay("HD261105001", HOM_NAY.atTime(8, 30), "Nguyễn An", true, HoaDon.TIEN_MAT,
                    new BigDecimal("2770000"))));

    private void tao(Function<LocalDate, DoanhThuTrongNgay> nguon) throws Exception {
        SwingUtilities.invokeAndWait(() -> trang = new PnlDoanhThuNgay(ngay -> {
            synchronized (daGoi) {
                daGoi.add(ngay);
            }
            return nguon.apply(ngay);
        }, () -> LocalDateTime.of(2026, 11, 5, 14, 0)));
    }

    @Test
    void moTrang_homNay_theVaBang() throws Exception {
        tao(ngay -> MAU);
        PnlThongKeDichVuTest.choDen(() -> trang.model.getRowCount() == 3);
        assertEquals(HOM_NAY, daGoi.getFirst());
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("4.855.000 ₫", trang.lblTamTinh.getText());
            assertEquals("3 hóa đơn · Chưa thu: 300.000 ₫", trang.lblGhiChuTamTinh.getText());
            assertEquals("4.555.000 ₫", trang.lblDaThu.getText());
            assertEquals("2.770.000 ₫", trang.lblTienMat.getText());
            assertEquals("1.785.000 ₫", trang.lblChuyenKhoan.getText());
            assertEquals("HD261105003", trang.model.getValueAt(0, PnlDoanhThuNgay.COT_MA));
            assertEquals("10:15", trang.model.getValueAt(0, PnlDoanhThuNgay.COT_GIO));
            assertEquals(PnlDoanhThuNgay.CHO_THANH_TOAN, trang.model.getValueAt(0, PnlDoanhThuNgay.COT_TRANG_THAI));
            assertEquals("—", trang.model.getValueAt(0, PnlDoanhThuNgay.COT_HINH_THUC));
            assertEquals(HoaDon.TIEN_MAT, trang.model.getValueAt(2, PnlDoanhThuNgay.COT_HINH_THUC));
            assertEquals("2.770.000 ₫", trang.model.getValueAt(2, PnlDoanhThuNgay.COT_TIEN));
        });
    }

    @Test
    void chonNgayKhac_vaNutHomNay() throws Exception {
        tao(ngay -> new DoanhThuTrongNgay(ngay, List.of()));
        PnlThongKeDichVuTest.choDen(() -> !daGoi.isEmpty());
        LocalDate homQua = HOM_NAY.minusDays(1);
        SwingUtilities.invokeAndWait(() ->
                trang.dcNgay.setDate(Date.from(homQua.atStartOfDay(ZoneId.systemDefault()).toInstant())));
        PnlThongKeDichVuTest.choDen(() -> homQua.equals(daGoi.getLast()));
        SwingUtilities.invokeAndWait(() -> trang.btnHomNay.doClick());
        PnlThongKeDichVuTest.choDen(() -> HOM_NAY.equals(daGoi.getLast()));
    }

    @Test
    void khongCoHoaDon_vaLoiDb() throws Exception {
        tao(ngay -> new DoanhThuTrongNgay(ngay, List.of()));
        PnlThongKeDichVuTest.choDen(() -> trang.lblThongBao.getText().equals(PnlDoanhThuNgay.KHONG_CO_HOA_DON));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("0 ₫", trang.lblTamTinh.getText());
            assertEquals("0 ₫", trang.lblTienMat.getText());
            assertFalse(trang.lblThongBao.isVisible(), "Bảng trống thì thông báo nằm trong bảng");
        });

        tao(ngay -> {
            throw new DataAccessException("timeout", null);
        });
        PnlThongKeDichVuTest.choDen(() -> trang.lblThongBao.getText().equals("Không thể tải doanh thu. Vui lòng thử lại."));
        SwingUtilities.invokeAndWait(() -> assertEquals("—", trang.lblTamTinh.getText()));
    }
}
