package com.quanlynhakhoa.ui.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quanlynhakhoa.dao.ThongKeLichHenDAO.TongHop;
import com.quanlynhakhoa.entity.LichHen;
import com.quanlynhakhoa.entity.enums.TrangThaiLichHen;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;
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
 * UC032 – lễ tân xem lịch hẹn trong ngày theo trạng thái (không cần DB, nguồn giả)
 * [x] mở trang mặc định hôm nay; chọn ngày khác thì tải lại; nút "Hôm nay" quay về hôm nay
 * [x] 6 thẻ: tổng + 5 trạng thái kèm tỷ lệ; bảng từng lịch hẹn trong ngày
 * [x] không có lịch -> số 0 và "Không có lịch hẹn trong ngày này"; lỗi -> thông báo, không lộ chi tiết DB
 */
class PnlThongKeLichHenNgayTest {

    private static final LocalDate HOM_NAY = LocalDate.of(2026, 11, 5);
    private final List<LocalDate> daGoi = new ArrayList<>();
    private PnlThongKeLichHenNgay trang;

    private static final TongHop MAU = new TongHop(6, 1, 1, 2, 1, 1);
    private static final List<LichHen> LICH = List.of(
            LichHenMau.lich("LH261105001", HOM_NAY.atTime(8, 0), TrangThaiLichHen.HOAN_TAT,
                    "Nguyễn An", "Cạo vôi răng", "Trần Dũng"),
            LichHenMau.lich("LH261105002", HOM_NAY.atTime(9, 30), TrangThaiLichHen.DA_HUY,
                    "Lê Chi", "Trám răng", "Trần Dũng"),
            LichHenMau.lich("LH261105003", HOM_NAY.atTime(14, 0), TrangThaiLichHen.DA_DAT,
                    "Phạm Bình", "Nhổ răng", "Trần Dũng"));

    private void tao(Function<LocalDate, TongHop> nguon, Function<LocalDate, List<LichHen>> danhSach)
            throws Exception {
        SwingUtilities.invokeAndWait(() -> trang = new PnlThongKeLichHenNgay(ngay -> {
            synchronized (daGoi) {
                daGoi.add(ngay);
            }
            return nguon.apply(ngay);
        }, danhSach, () -> LocalDateTime.of(2026, 11, 5, 14, 0)));
    }

    @Test
    void moTrang_homNay_theVaBang() throws Exception {
        tao(ngay -> MAU, ngay -> LICH);
        PnlThongKeDichVuTest.choDen(() -> trang.model.getRowCount() == 3);
        assertEquals(HOM_NAY, daGoi.getFirst());
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("6", trang.lblTong.getText());
            assertEquals("1", trang.lblDaDat.getText());
            assertEquals("1", trang.lblDaDen.getText());
            assertEquals("2", trang.lblHoanTat.getText());
            assertEquals("1", trang.lblDaHuy.getText());
            assertEquals("1", trang.lblKhongDen.getText());
            assertEquals("33,3% tổng số lịch", trang.lblTyLeHoanTat.getText());
            assertEquals("16,7% tổng số lịch", trang.lblTyLeDaHuy.getText());
            assertEquals("08:00", trang.model.getValueAt(0, PnlThongKeLichHenNgay.COT_GIO));
            assertEquals("LH261105001", trang.model.getValueAt(0, PnlThongKeLichHenNgay.COT_MA));
            assertEquals("Nguyễn An", trang.model.getValueAt(0, PnlThongKeLichHenNgay.COT_KHACH));
            assertEquals("Cạo vôi răng", trang.model.getValueAt(0, PnlThongKeLichHenNgay.COT_DICH_VU));
            assertEquals("Trần Dũng", trang.model.getValueAt(0, PnlThongKeLichHenNgay.COT_NHA_SI));
            assertEquals("Hoàn tất", trang.model.getValueAt(0, PnlThongKeLichHenNgay.COT_TRANG_THAI));
            assertEquals("Đã hủy", trang.model.getValueAt(1, PnlThongKeLichHenNgay.COT_TRANG_THAI));
            assertEquals("Đã đặt", trang.model.getValueAt(2, PnlThongKeLichHenNgay.COT_TRANG_THAI));
            assertTrue(trang.lblThongBao.isVisible(), "Có dòng thì thông báo hiện dưới bảng");
        });
    }

    @Test
    void chonNgayKhac_vaNutHomNay() throws Exception {
        tao(ngay -> new TongHop(0, 0, 0, 0, 0, 0), ngay -> List.of());
        PnlThongKeDichVuTest.choDen(() -> !daGoi.isEmpty());
        LocalDate homQua = HOM_NAY.minusDays(1);
        SwingUtilities.invokeAndWait(() ->
                trang.dcNgay.setDate(Date.from(homQua.atStartOfDay(ZoneId.systemDefault()).toInstant())));
        PnlThongKeDichVuTest.choDen(() -> homQua.equals(daGoi.getLast()));
        SwingUtilities.invokeAndWait(() -> trang.btnHomNay.doClick());
        PnlThongKeDichVuTest.choDen(() -> HOM_NAY.equals(daGoi.getLast()));
    }

    @Test
    void khongCoLich_baoKhongCoLichVaThePhaiBangKhong() throws Exception {
        tao(ngay -> new TongHop(0, 0, 0, 0, 0, 0), ngay -> List.of());
        PnlThongKeDichVuTest.choDen(() ->
                trang.lblThongBao.getText().equals(PnlThongKeLichHenNgay.KHONG_CO_LICH));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("0", trang.lblTong.getText());
            assertEquals("0", trang.lblHoanTat.getText());
            assertEquals("0,0% tổng số lịch", trang.lblTyLeHoanTat.getText());
            assertEquals(0, trang.model.getRowCount());
            assertFalse(trang.lblThongBao.isVisible(), "Bảng trống thì thông báo nằm giữa bảng");
        });
    }

    @Test
    void loiNghiepVu_vaLoiDb_khongLoChiTiet() throws Exception {
        tao(ngay -> {
            throw new BusinessException("Lễ tân chỉ được xem thống kê theo từng ngày.");
        }, ngay -> List.of());
        PnlThongKeDichVuTest.choDen(() ->
                trang.lblThongBao.getText().equals("Lễ tân chỉ được xem thống kê theo từng ngày."));

        tao(ngay -> {
            throw new DataAccessException("secret-sql-detail", null);
        }, ngay -> List.of());
        PnlThongKeDichVuTest.choDen(() ->
                trang.lblThongBao.getText().equals("Không thể tải thống kê. Vui lòng thử lại."));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("—", trang.lblTong.getText());
            assertFalse(trang.lblThongBao.getText().contains("secret"));
        });
    }
}
