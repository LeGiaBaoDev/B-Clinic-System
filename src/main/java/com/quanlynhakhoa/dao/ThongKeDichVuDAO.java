package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.dao.support.PhanQuyen;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Thống kê dịch vụ được sử dụng nhiều nhất (chỉ Quản lý). Chỉ đọc; dữ liệu lấy từ các dòng
 * ChiTietPhieuKham của những hóa đơn ĐÃ THANH TOÁN, lọc theo ngày thanh toán.
 *
 * <h2>Cách tính (sửa ở đây nếu nhóm đổi quy định)</h2>
 * <ul>
 *   <li><b>Số lượt</b> = số phiếu khám khác nhau có dùng dịch vụ (Dương chốt 05/11, phương án "đếm phiếu").
 *       Một phiếu có 2 dòng cùng dịch vụ, hoặc mua số lượng 2, vẫn tính 1 lượt.
 *       Muốn đổi sang "cộng số lượng": thay {@code COUNT(DISTINCT d.maPK)} bằng {@code SUM(d.soLuong)}.</li>
 *   <li><b>Tiền của một dòng trước giảm</b> = thành tiền + thuế GTGT của dòng, trong đó thuế = ROUND(thành tiền ×
 *       mức thuế / 100, 0). Giống hệt cách HoaDonDAO (của Bảo) cộng tổng hóa đơn trước giảm, để số liệu khớp.</li>
 *   <li><b>Doanh thu (sau voucher)</b> — Dương chốt 05/11 tính SAU voucher. Báo cáo và bộ AGENTS chỉ định nghĩa
 *       tổng hóa đơn = Σ(thành tiền + thuế) − tiền giảm voucher (quy tắc R-HD-1), KHÔNG quy định chia tiền giảm
 *       cho từng dịch vụ. Voucher giảm trên cả hóa đơn nên ở đây <b>chia tiền giảm theo tỷ lệ</b>:
 *       <pre>
 *       tiền giảm của dòng = tongKhuyenMai của hóa đơn × (tiền dòng trước giảm / tổng tiền trước giảm của hóa đơn)
 *       doanh thu của dòng  = tiền dòng trước giảm − tiền giảm của dòng
 *       doanh thu dịch vụ   = tổng doanh thu các dòng của dịch vụ đó, làm tròn tới đồng (HALF_UP) sau khi cộng
 *       </pre>
 *       Ví dụ: hóa đơn có Cạo vôi 300.000 (KCT) + Tẩy trắng 2.500.000 + thuế 10% 250.000, voucher giảm 305.000.
 *       Tổng trước giảm 3.050.000 → Cạo vôi chịu 305.000 × 300.000/3.050.000 = 30.000, doanh thu 270.000;
 *       Tẩy trắng chịu 275.000, doanh thu 2.475.000. Cộng lại đúng 2.745.000 = tiền khách trả.
 *       Do làm tròn từng dịch vụ, tổng doanh thu các dịch vụ có thể lệch vài đồng so với tổng các hóa đơn.
 *       Muốn tính TRƯỚC voucher: bỏ phần trừ tiền giảm (chỉ cộng {@code tienDong}).</li>
 *   <li><b>Đơn giá</b> hiển thị là giá hiện tại trong danh mục DichVu (giá đã bán từng lượt có thể khác, nằm ở
 *       ChiTietPhieuKham.donGia).</li>
 * </ul>
 */
public class ThongKeDichVuDAO {

    /** Tiêu chí xếp hạng: nhiều lượt nhất hoặc nhiều doanh thu nhất. */
    public enum TieuChi {
        SO_LUOT("Số lượt sử dụng"), DOANH_THU("Doanh thu");

        private final String ten;

        TieuChi(String ten) {
            this.ten = ten;
        }

        @Override
        public String toString() {
            return ten; // để đưa thẳng vào JComboBox / nút chọn
        }
    }

    /** Một dòng xếp hạng (dữ liệu chỉ đọc). Hạng bắt đầu từ 1. */
    public record DichVuXepHang(int hang, String maDV, String tenDV, BigDecimal donGia, long soLuot,
            BigDecimal doanhThu) {
    }

    /** Số dịch vụ tối đa trong bảng xếp hạng. */
    public static final int GIOI_HAN_MAC_DINH = 10;

    /*
     * CTE "Dong": mỗi dòng chi tiết của hóa đơn đã thanh toán trong khoảng ngày, kèm tiền dòng trước giảm
     * và tổng trước giảm của cả hóa đơn (SUM ... OVER theo phiếu khám — mỗi phiếu chỉ có 1 hóa đơn).
     * Câu SELECT ngoài gộp theo dịch vụ; phần ORDER BY được ghép từ enum (không có dữ liệu người dùng).
     */
    private static final String SQL = """
            WITH Dong AS (
                SELECT ct.maPK, ct.maDV,
                       ct.thanhTien + ROUND(ct.thanhTien * ct.mucThueGTGT / 100, 0) AS tienDong,
                       hd.tongKhuyenMai,
                       SUM(ct.thanhTien + ROUND(ct.thanhTien * ct.mucThueGTGT / 100, 0))
                           OVER (PARTITION BY ct.maPK) AS tongHoaDon
                FROM ChiTietPhieuKham ct
                JOIN HoaDon hd ON hd.maPK = ct.maPK
                WHERE hd.trangThai = 1 AND hd.ngayThanhToan >= ? AND hd.ngayThanhToan < ?
            )
            SELECT TOP (?) d.maDV, dv.tenDV, dv.donGia,
                   COUNT(DISTINCT d.maPK) AS soLuot,
                   SUM(d.tienDong - CASE WHEN d.tongHoaDon > 0
                                         THEN d.tongKhuyenMai * d.tienDong / d.tongHoaDon
                                         ELSE 0 END) AS doanhThu
            FROM Dong d
            JOIN DichVu dv ON dv.maDV = d.maDV
            GROUP BY d.maDV, dv.tenDV, dv.donGia
            """;

    /** Xếp hạng tối đa {@value #GIOI_HAN_MAC_DINH} dịch vụ trong [tu, den]. */
    public List<DichVuXepHang> xepHang(LocalDate tu, LocalDate den, TieuChi tieuChi) {
        return xepHang(tu, den, tieuChi, GIOI_HAN_MAC_DINH);
    }

    /**
     * Xếp hạng dịch vụ trong khoảng ngày thanh toán [tu, den] (tính cả 2 đầu).
     * Bằng nhau ở tiêu chí chính thì xét tiêu chí còn lại, rồi tới mã dịch vụ, để thứ tự ổn định.
     *
     * @param gioiHan số dịch vụ tối đa, từ 1 đến {@value #GIOI_HAN_MAC_DINH}
     * @return danh sách rỗng nếu không có dữ liệu (giao diện hiện "Không có dữ liệu")
     * @throws BusinessException khoảng ngày sai, giới hạn sai, không phải Quản lý
     */
    public List<DichVuXepHang> xepHang(LocalDate tu, LocalDate den, TieuChi tieuChi, int gioiHan) {
        kiemTraKhoangNgay(tu, den);
        yeuCau(gioiHan >= 1 && gioiHan <= GIOI_HAN_MAC_DINH,
                "Số dịch vụ hiển thị phải từ 1 đến " + GIOI_HAN_MAC_DINH + ".");
        TieuChi theo = tieuChi == null ? TieuChi.SO_LUOT : tieuChi;
        String sapXep = theo == TieuChi.DOANH_THU
                ? " ORDER BY doanhThu DESC, soLuot DESC, d.maDV"
                : " ORDER BY soLuot DESC, doanhThu DESC, d.maDV";
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            List<Object[]> dong = JdbcHelper.query(c, SQL + sapXep, r -> new Object[]{
                r.getString("maDV"), r.getString("tenDV"), r.getBigDecimal("donGia"),
                r.getLong("soLuot"), r.getBigDecimal("doanhThu")},
                    tu.atStartOfDay(), den.plusDays(1).atStartOfDay(), gioiHan);
            List<DichVuXepHang> ketQua = new ArrayList<>();
            for (int i = 0; i < dong.size(); i++) {
                Object[] d = dong.get(i);
                BigDecimal doanhThu = d[4] == null ? BigDecimal.ZERO
                        : ((BigDecimal) d[4]).setScale(0, RoundingMode.HALF_UP); // làm tròn tới đồng sau khi cộng
                ketQua.add(new DichVuXepHang(i + 1, (String) d[0], (String) d[1], (BigDecimal) d[2],
                        (Long) d[3], doanhThu));
            }
            return ketQua;
        });
    }

    /** Khoảng ngày hợp lệ: đủ 2 đầu và ngày bắt đầu không sau ngày kết thúc. */
    static void kiemTraKhoangNgay(LocalDate tu, LocalDate den) {
        yeuCau(tu != null && den != null, "Vui lòng chọn đủ ngày bắt đầu và ngày kết thúc.");
        yeuCau(!tu.isAfter(den), "Khoảng thời gian không hợp lệ: ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
    }

    private static void yeuCau(boolean dieuKien, String thongBao) {
        if (!dieuKien) {
            throw new BusinessException(thongBao);
        }
    }
}
