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
import java.util.function.BiFunction;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/*
 * UC034 – nha sĩ xem số lịch hẹn của mình (không cần DB, nguồn giả)
 * [x] mở trang mặc định tuần này (thứ Hai – Chủ nhật); kỳ Ngày chỉ cần chọn MỘT ngày; kỳ Tháng; kỳ Khoảng
 * [x] nút "Hôm nay"; 6 thẻ kèm tỷ lệ; bảng lịch hẹn; khoảng sai -> báo lỗi, không gọi nguồn
 * [x] không có lịch -> "Không có lịch hẹn trong kỳ này"; lỗi DB -> thông báo chung
 */
class PnlThongKeLichHenNhaSiTest {

    /** Thứ Năm; tuần này là 02/11 – 08/11/2026. */
    private static final LocalDate HOM_NAY = LocalDate.of(2026, 11, 5);
    private final List<LocalDate[]> daGoi = new ArrayList<>();
    private PnlThongKeLichHenNhaSi trang;

    private static final TongHop MAU = new TongHop(4, 1, 0, 2, 1, 0);
    private static final List<LichHen> LICH = List.of(
            LichHenMau.lich("LH261105001", HOM_NAY.atTime(8, 0), TrangThaiLichHen.HOAN_TAT,
                    "Nguyễn An", "Cạo vôi răng", "Trần Dũng"),
            LichHenMau.lich("LH261106001", HOM_NAY.plusDays(1).atTime(15, 30), TrangThaiLichHen.DA_DAT,
                    "Lê Chi", "Trám răng", "Trần Dũng"));

    private void tao(BiFunction<LocalDate, LocalDate, TongHop> nguon) throws Exception {
        SwingUtilities.invokeAndWait(() -> trang = new PnlThongKeLichHenNhaSi((tu, den) -> {
            synchronized (daGoi) {
                daGoi.add(new LocalDate[]{tu, den});
            }
            return nguon.apply(tu, den);
        }, (tu, den) -> LICH, () -> LocalDateTime.of(2026, 11, 5, 14, 0)));
    }

    private static Date ngay(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private void daGoiCuoi(LocalDate tu, LocalDate den) throws Exception {
        PnlThongKeDichVuTest.choDen(() -> {
            synchronized (daGoi) {
                LocalDate[] cuoi = daGoi.isEmpty() ? null : daGoi.getLast();
                return cuoi != null && tu.equals(cuoi[0]) && den.equals(cuoi[1]);
            }
        });
    }

    @Test
    void moTrang_macDinhTuanNay_theVaBang() throws Exception {
        tao((tu, den) -> MAU);
        PnlThongKeDichVuTest.choDen(() -> trang.model.getRowCount() == 2);
        assertEquals(LocalDate.of(2026, 11, 2), daGoi.getFirst()[0]);
        assertEquals(LocalDate.of(2026, 11, 8), daGoi.getFirst()[1]);
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("4", trang.lblTong.getText());
            assertEquals("2", trang.lblHoanTat.getText());
            assertEquals("50,0% tổng số lịch", trang.lblTyLeHoanTat.getText());
            assertEquals("05/11 08:00", trang.model.getValueAt(0, PnlThongKeLichHenNhaSi.COT_GIO));
            assertEquals("Nguyễn An", trang.model.getValueAt(0, PnlThongKeLichHenNhaSi.COT_KHACH));
            assertEquals("Hoàn tất", trang.model.getValueAt(0, PnlThongKeLichHenNhaSi.COT_TRANG_THAI));
            assertEquals("Đã đặt", trang.model.getValueAt(1, PnlThongKeLichHenNhaSi.COT_TRANG_THAI));
            assertTrue(trang.btnTuan.isSelected());
            assertTrue(trang.pnlMotNgay.isVisible(), "Kỳ Tuần dùng một ô ngày");
        });
    }

    @Test
    void kyNgay_chiCanChonMotNgay() throws Exception {
        tao((tu, den) -> MAU);
        PnlThongKeDichVuTest.choDen(() -> !daGoi.isEmpty());
        SwingUtilities.invokeAndWait(() -> trang.btnNgay.doClick());
        SwingUtilities.invokeAndWait(() -> trang.dcNgay.setDate(ngay(LocalDate.of(2026, 11, 6))));
        daGoiCuoi(LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 6));
    }

    @Test
    void kyThang_vaNutHomNay() throws Exception {
        tao((tu, den) -> MAU);
        PnlThongKeDichVuTest.choDen(() -> !daGoi.isEmpty());
        SwingUtilities.invokeAndWait(() -> trang.btnThang.doClick());
        daGoiCuoi(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
        SwingUtilities.invokeAndWait(() -> trang.btnHomNay.doClick());
        daGoiCuoi(HOM_NAY, HOM_NAY);
        SwingUtilities.invokeAndWait(() -> assertTrue(trang.btnNgay.isSelected()));
    }

    @Test
    void kyKhoang_dienSanKhoangDangXem_vaBaoLoiKhiTuSauDen() throws Exception {
        tao((tu, den) -> MAU);
        PnlThongKeDichVuTest.choDen(() -> !daGoi.isEmpty());
        SwingUtilities.invokeAndWait(() -> trang.btnKhoang.doClick());
        PnlThongKeDichVuTest.choDen(() -> daGoi.size() >= 2);
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(ngay(LocalDate.of(2026, 11, 2)), trang.dcTu.getDate());
            assertEquals(ngay(LocalDate.of(2026, 11, 8)), trang.dcDen.getDate());
            assertFalse(trang.pnlMotNgay.isVisible(), "Kỳ Khoảng dùng hai ô Từ – Đến");
            assertTrue(trang.pnlKhoang.isVisible());
        });
        int truocKhi = daGoi.size();
        SwingUtilities.invokeAndWait(() -> trang.dcTu.setDate(ngay(LocalDate.of(2026, 11, 20))));
        PnlThongKeDichVuTest.choDen(() -> trang.lblThongBao.getText().startsWith("Khoảng thời gian không hợp lệ"));
        assertEquals(truocKhi, daGoi.size(), "Khoảng sai thì không gọi nguồn dữ liệu");
    }

    @Test
    void khongCoLich_vaLoi() throws Exception {
        tao((tu, den) -> new TongHop(0, 0, 0, 0, 0, 0));
        PnlThongKeDichVuTest.choDen(() ->
                trang.lblThongBao.getText().equals(PnlThongKeLichHenNhaSi.KHONG_CO_LICH));
        SwingUtilities.invokeAndWait(() -> assertEquals("0", trang.lblTong.getText()));

        tao((tu, den) -> {
            throw new BusinessException("Chỉ được thống kê lịch của mình.");
        });
        PnlThongKeDichVuTest.choDen(() ->
                trang.lblThongBao.getText().equals("Chỉ được thống kê lịch của mình."));

        tao((tu, den) -> {
            throw new DataAccessException("secret", null);
        });
        PnlThongKeDichVuTest.choDen(() ->
                trang.lblThongBao.getText().equals("Không thể tải thống kê. Vui lòng thử lại."));
        SwingUtilities.invokeAndWait(() -> assertEquals("—", trang.lblTong.getText()));
    }

    @Test
    void tuanVaThangCua_cacNgayBien() {
        assertEquals(LocalDate.of(2026, 11, 2), PnlThongKeLichHenNhaSi.tuanCua(LocalDate.of(2026, 11, 8))[0],
                "Chủ nhật thuộc tuần bắt đầu thứ Hai trước đó");
        assertEquals(LocalDate.of(2026, 11, 8), PnlThongKeLichHenNhaSi.tuanCua(LocalDate.of(2026, 11, 2))[1]);
        assertEquals(LocalDate.of(2026, 2, 28), PnlThongKeLichHenNhaSi.thangCua(LocalDate.of(2026, 2, 10))[1]);
        assertEquals(LocalDate.of(2026, 2, 1), PnlThongKeLichHenNhaSi.thangCua(LocalDate.of(2026, 2, 28))[0]);
    }
}
