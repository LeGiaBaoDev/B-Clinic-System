package com.quanlynhakhoa.ui.dialog;

import static org.junit.jupiter.api.Assertions.*;

import com.quanlynhakhoa.dao.ChiTietPhieuKhamDAO.DongThanhToan;
import com.quanlynhakhoa.entity.HoSoBenhAn;
import com.quanlynhakhoa.entity.HoaDon;
import com.quanlynhakhoa.entity.KhachHang;
import com.quanlynhakhoa.entity.NhanVien;
import com.quanlynhakhoa.entity.PhieuKham;
import com.quanlynhakhoa.entity.Voucher;
import com.quanlynhakhoa.entity.enums.TrangThaiPhieuKham;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/*
 * Hộp thoại chi tiết hóa đơn (không cần DB, DAO giả)
 * [x] đã thanh toán: khách hàng, ngày lập, phiếu khám, nhãn "Đã thanh toán", hình thức + thời điểm + thu ngân
 * [x] từng dòng dịch vụ: ĐVT, SL, đơn giá, thành tiền, thuế (KCT / 5% / 10%), tiền thuế
 * [x] tổng tiền hàng, tổng thuế, voucher, số tiền giảm, tổng cộng thanh toán
 * [x] chưa thanh toán: "Chưa thanh toán", nhãn "Chờ thanh toán", voucher "Không áp dụng"
 * [x] đọc dữ liệu chạy ngoài EDT
 * [x] không tìm thấy hóa đơn / lỗi quyền / lỗi DB -> thông báo phù hợp
 */
class DlgChiTietHoaDonTest {

    private DlgChiTietHoaDon.NoiDung noiDung;
    private final AtomicBoolean daDong = new AtomicBoolean();

    private static final KhachHang KHACH = new KhachHang("KH01284", "Nguyễn Minh Anh",
            LocalDate.of(1995, 5, 20), false, "0901234567", null, null);
    private static final LocalDateTime LAP = LocalDateTime.of(2026, 10, 16, 10, 8);

    private static HoaDon hoaDon(boolean daTra, Voucher voucher, double giam) {
        PhieuKham pk = new PhieuKham("PK261016018", LAP.minusMinutes(40), TrangThaiPhieuKham.HOAN_TAT,
                null, new HoSoBenhAn(null, null, KHACH), null, null);
        NhanVien thuNgan = new NhanVien("NV003", "Nguyễn Thảo Linh", false, "0398682232", "letan@example.com", "Lễ tân");
        return daTra
                ? new HoaDon("HD261016042", LAP, true, HoaDon.CHUYEN_KHOAN, LAP.plusMinutes(12), giam, pk, voucher, thuNgan)
                : new HoaDon("HD261016042", LAP, false, null, null, giam, pk, voucher, null);
    }

    private static final List<DongThanhToan> DONG = List.of(
            new DongThanhToan(1, "DV001", "Cạo vôi răng", "Lần", 1, new BigDecimal("300000"),
                    BigDecimal.ZERO, new BigDecimal("300000"), BigDecimal.ZERO),
            new DongThanhToan(2, "DV004", "Bàn chải điện", "Cái", 2, new BigDecimal("850000"),
                    new BigDecimal("5.00"), new BigDecimal("1700000"), new BigDecimal("85000")),
            new DongThanhToan(3, "DV005", "Tẩy trắng răng", "Lần", 1, new BigDecimal("2500000"),
                    new BigDecimal("10.00"), new BigDecimal("2500000"), new BigDecimal("250000")));

    private void tao(Function<String, HoaDon> layHoaDon, Function<String, List<DongThanhToan>> layDong,
            Function<String, BigDecimal> tinhTong) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            noiDung = new DlgChiTietHoaDon.NoiDung("HD261016042", layHoaDon, layDong, tinhTong, () -> daDong.set(true));
            noiDung.taiDuLieu();
        });
    }

    @Test
    void daThanhToan_hienDuThongTin_tongTien_vaThanhToan() throws Exception {
        AtomicBoolean ngoaiEdt = new AtomicBoolean();
        AtomicReference<String> maPKDaHoi = new AtomicReference<>();
        Voucher vc = new Voucher("VC001", "Khai trương giảm 10%", true, 10, 500_000,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), true);
        tao(ma -> {
            ngoaiEdt.set(!SwingUtilities.isEventDispatchThread());
            return hoaDon(true, vc, 279_000);
        }, maPK -> {
            maPKDaHoi.set(maPK);
            return DONG;
        }, ma -> new BigDecimal("4556000"));
        choDen(() -> noiDung.lblTrangThai.isVisible());

        assertTrue(ngoaiEdt.get());
        assertEquals("PK261016018", maPKDaHoi.get(), "Lấy dòng dịch vụ theo phiếu khám của hóa đơn");
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("Hóa đơn HD261016042", noiDung.lblTieuDe.getText());
            assertEquals("Ngày lập 16/10/2026 10:08 · Phiếu khám PK261016018", noiDung.lblPhuDe.getText());
            assertEquals("Đã thanh toán", noiDung.lblTrangThai.getText());
            assertTrue(noiDung.lblKhachHang.getText().contains("Nguyễn Minh Anh"));
            assertTrue(noiDung.lblKhachHang.getText().contains("KH01284 · 0901234567"));
            assertTrue(noiDung.lblThanhToan.getText().contains("Chuyển khoản"));
            assertTrue(noiDung.lblThanhToan.getText().contains("16/10/2026 10:20"));
            assertTrue(noiDung.lblThanhToan.getText().contains("Thu ngân: Nguyễn Thảo Linh"));

            assertEquals(3, noiDung.modelDong.getRowCount());
            assertEquals("Bàn chải điện", noiDung.modelDong.getValueAt(1, 0));
            assertEquals("Cái", noiDung.modelDong.getValueAt(1, 1));
            assertEquals(2, noiDung.modelDong.getValueAt(1, 2));
            assertEquals("850.000 ₫", noiDung.modelDong.getValueAt(1, 3));
            assertEquals("1.700.000 ₫", noiDung.modelDong.getValueAt(1, 4));
            assertEquals("KCT", noiDung.modelDong.getValueAt(0, 5), "Thuế 0% hiển thị KCT");
            assertEquals("5%", noiDung.modelDong.getValueAt(1, 5));
            assertEquals("10%", noiDung.modelDong.getValueAt(2, 5));
            assertEquals("85.000 ₫", noiDung.modelDong.getValueAt(1, 6));

            assertEquals("4.500.000 ₫", noiDung.lblTongTienHang.getText());
            assertEquals("335.000 ₫", noiDung.lblTienThue.getText());
            assertEquals("VC001 – Khai trương giảm 10%", noiDung.lblVoucher.getText());
            assertEquals("−279.000 ₫", noiDung.lblTienGiam.getText());
            assertEquals("4.556.000 ₫", noiDung.lblTongCong.getText());
            assertEquals(" ", noiDung.lblThongBao.getText());
        });
    }

    @Test
    void chuaThanhToan_hienChoThanhToan_khongVoucher() throws Exception {
        tao(ma -> hoaDon(false, null, 0), maPK -> DONG.subList(0, 1), ma -> new BigDecimal("300000"));
        choDen(() -> noiDung.lblTrangThai.isVisible());
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("Chờ thanh toán", noiDung.lblTrangThai.getText());
            assertEquals("Chưa thanh toán", noiDung.lblThanhToan.getText());
            assertEquals("Không áp dụng", noiDung.lblVoucher.getText());
            assertEquals("0 ₫", noiDung.lblTienGiam.getText());
            assertEquals("300.000 ₫", noiDung.lblTongCong.getText());
        });
    }

    @Test
    void khongTimThay_hoacLoiQuyen_hoacLoiDb_baoPhuHop() throws Exception {
        tao(ma -> null, maPK -> List.of(), ma -> BigDecimal.ZERO);
        choDen(() -> noiDung.lblThongBao.getText().startsWith("Không tìm thấy hóa đơn HD261016042"));

        tao(ma -> {
            throw new BusinessException("Bạn không có quyền thực hiện thao tác này hoặc tài khoản đã bị khóa.");
        }, maPK -> List.of(), ma -> BigDecimal.ZERO);
        choDen(() -> noiDung.lblThongBao.getText().startsWith("Bạn không có quyền"));

        tao(ma -> {
            throw new DataAccessException("Login timeout chi tiết kỹ thuật", null);
        }, maPK -> List.of(), ma -> BigDecimal.ZERO);
        choDen(() -> noiDung.lblThongBao.getText().equals("Không thể tải hóa đơn. Vui lòng thử lại."));
        SwingUtilities.invokeAndWait(() -> assertFalse(noiDung.lblTrangThai.isVisible()));
    }

    @Test
    void nutDong_dongHopThoai() throws Exception {
        tao(ma -> hoaDon(false, null, 0), maPK -> List.of(), ma -> BigDecimal.ZERO);
        SwingUtilities.invokeAndWait(() -> noiDung.btnDong.doClick());
        assertTrue(daDong.get());
    }

    @Test
    void tenThue_vaDinhDangTien() {
        assertEquals("KCT", DlgChiTietHoaDon.NoiDung.tenThue(new BigDecimal("0.00")));
        assertEquals("KCT", DlgChiTietHoaDon.NoiDung.tenThue(null));
        assertEquals("5%", DlgChiTietHoaDon.NoiDung.tenThue(new BigDecimal("5.00")));
        assertEquals("1.250.000 ₫", DlgChiTietHoaDon.NoiDung.tien(new BigDecimal("1250000.00")));
        assertEquals("0 ₫", DlgChiTietHoaDon.NoiDung.tien(null));
    }

    private static void choDen(BooleanSupplier dieuKien) throws Exception {
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
