package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.dao.HoaDonDAO;
import com.quanlynhakhoa.entity.HoaDon;
import com.quanlynhakhoa.entity.HoSoBenhAn;
import com.quanlynhakhoa.entity.KhachHang;
import com.quanlynhakhoa.entity.LichHen;
import com.quanlynhakhoa.entity.PhieuKham;
import com.quanlynhakhoa.exception.BusinessException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * Bộ lọc tra cứu hóa đơn dùng chung cho trang "Hóa đơn" (trang do Bảo quản lý gắn vào).
 * Không có giao diện, không viết SQL: chỉ gọi API đọc của {@link HoaDonDAO} rồi lọc thêm trên máy.
 *
 * <p>Cách dùng trong trang (chạy trong SwingWorker vì có truy vấn DB):</p>
 * <pre>
 * TraCuuHoaDon traCuu = new TraCuuHoaDon();
 * List&lt;TraCuuHoaDon.Dong&gt; ketQua = traCuu.tim(txtTimKiem.getText(), TraCuuHoaDon.TrangThai.TAT_CA, tuNgay, denNgay);
 * // mỗi dòng: dong.hoaDon(), dong.khachHang(), dong.tongThanhToan(), TraCuuHoaDon.tenTrangThai(dong.hoaDon())
 * // nút "Xem": new DlgChiTietHoaDon(SwingUtilities.getWindowAncestor(this), maHD).setVisible(true);
 * </pre>
 *
 * <p>Vì sao lọc từ khóa trên máy: {@code HoaDonDAO.timKiem} chỉ lọc theo khoảng ngày lập và trạng thái.
 * Danh sách đã giới hạn theo khoảng ngày nên số hóa đơn nhỏ, lọc thêm mã / tên / SĐT trên máy đủ nhanh
 * (Dương chọn 05/11, không sửa file của Bảo).</p>
 */
public final class TraCuuHoaDon {

    /** Bộ lọc trạng thái; {@code daTra} là giá trị gửi cho HoaDonDAO (null = không lọc). */
    public enum TrangThai {
        TAT_CA("Tất cả", null),
        CHO_THANH_TOAN("Chờ thanh toán", Boolean.FALSE),
        DA_THANH_TOAN("Đã thanh toán", Boolean.TRUE);

        private final String ten;
        private final Boolean daTra;

        TrangThai(String ten, Boolean daTra) {
            this.ten = ten;
            this.daTra = daTra;
        }

        public String ten() {
            return ten;
        }

        public Boolean daTra() {
            return daTra;
        }

        @Override
        public String toString() {
            return ten; // để đưa thẳng vào JComboBox
        }
    }

    /** Một dòng kết quả: hóa đơn, khách hàng (có thể null nếu dữ liệu thiếu), tổng phải thanh toán. */
    public record Dong(HoaDon hoaDon, KhachHang khachHang, BigDecimal tongThanhToan) {
    }

    /** Hàm đọc của DAO; tách ra để test không cần DB. */
    @FunctionalInterface
    interface TimTheoNgay {
        List<HoaDon> tim(LocalDate tu, LocalDate den, Boolean daTra);
    }

    /** Số ngày mặc định của bộ lọc khoảng ngày (tính cả hôm nay). */
    public static final int SO_NGAY_MAC_DINH = 30;

    private final TimTheoNgay timTheoNgay;
    private final Function<String, BigDecimal> tinhTongTien;

    /** Dùng DAO thật của Bảo. */
    public TraCuuHoaDon() {
        this(new HoaDonDAO()::timKiem, new HoaDonDAO()::tinhTongTien);
    }

    TraCuuHoaDon(TimTheoNgay timTheoNgay, Function<String, BigDecimal> tinhTongTien) {
        this.timTheoNgay = Objects.requireNonNull(timTheoNgay);
        this.tinhTongTien = Objects.requireNonNull(tinhTongTien);
    }

    /**
     * Tìm hóa đơn theo từ khóa (mã hóa đơn, tên hoặc SĐT khách), trạng thái và khoảng ngày lập.
     * Kết quả giữ thứ tự của DAO (mới lập trước). Chỉ Quản lý, Lễ tân gọi được (DAO kiểm tra quyền).
     *
     * @param tuKhoa  rỗng/null = không lọc từ khóa
     * @param tu      ngày lập từ (tính cả ngày này)
     * @param den     ngày lập đến (tính cả ngày này)
     * @return danh sách rỗng nếu không có hóa đơn phù hợp (trang hiện "Không tìm thấy hóa đơn phù hợp")
     * @throws BusinessException khoảng ngày sai, không có quyền
     */
    public List<Dong> tim(String tuKhoa, TrangThai trangThai, LocalDate tu, LocalDate den) {
        kiemTraKhoangNgay(tu, den);
        TrangThai loc = trangThai == null ? TrangThai.TAT_CA : trangThai;
        List<Dong> ketQua = new ArrayList<>();
        for (HoaDon hd : timTheoNgay.tim(tu, den, loc.daTra())) {
            if (khop(hd, tuKhoa)) {
                // Tổng thanh toán = tiền dịch vụ + thuế từng dòng - giảm giá; DAO của Bảo tính để khớp với lúc thu tiền.
                ketQua.add(new Dong(hd, khachHang(hd), tinhTongTien.apply(hd.getMaHD())));
            }
        }
        return ketQua;
    }

    /** Khoảng ngày hợp lệ: có đủ 2 đầu và ngày bắt đầu không sau ngày kết thúc. */
    public static void kiemTraKhoangNgay(LocalDate tu, LocalDate den) {
        if (tu == null || den == null) {
            throw new BusinessException("Vui lòng chọn đủ ngày bắt đầu và ngày kết thúc.");
        }
        if (tu.isAfter(den)) {
            throw new BusinessException("Khoảng ngày không hợp lệ: ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        }
    }

    /** Ngày bắt đầu mặc định: {@value #SO_NGAY_MAC_DINH} ngày gần nhất tính tới {@code homNay}. */
    public static LocalDate tuNgayMacDinh(LocalDate homNay) {
        return homNay.minusDays(SO_NGAY_MAC_DINH - 1L);
    }

    /**
     * Hóa đơn có khớp từ khóa không. So mã hóa đơn, tên khách, SĐT khách; không phân biệt hoa thường
     * và bỏ dấu tiếng Việt (gõ "nguyen" vẫn ra "Nguyễn"). SĐT so theo chữ số nên gõ "0901 234" vẫn khớp.
     */
    public static boolean khop(HoaDon hd, String tuKhoa) {
        String tim = boDau(tuKhoa);
        if (tim.isEmpty()) {
            return true;
        }
        KhachHang kh = khachHang(hd);
        if (boDau(hd.getMaHD()).contains(tim)) {
            return true;
        }
        if (kh != null && boDau(kh.getTenKH()).contains(tim)) {
            return true;
        }
        if (kh != null && kh.getMaKH() != null && boDau(kh.getMaKH()).contains(tim)) {
            return true;
        }
        String soTim = tuKhoa.replaceAll("\\D", "");
        return kh != null && kh.getSdt() != null && !soTim.isEmpty() && kh.getSdt().replaceAll("\\D", "").contains(soTim);
    }

    /**
     * Khách hàng của hóa đơn: lấy theo lịch hẹn của phiếu khám, nếu phiếu không gắn lịch hẹn
     * thì lấy theo hồ sơ bệnh án. Thiếu dữ liệu thì trả null (trang hiện "—").
     */
    public static KhachHang khachHang(HoaDon hd) {
        PhieuKham pk = hd == null ? null : hd.getPhieuKham();
        if (pk == null) {
            return null;
        }
        LichHen lh = pk.getLichHen();
        if (lh != null && lh.getKhachHang() != null) {
            return lh.getKhachHang();
        }
        HoSoBenhAn hs = pk.getHoSoBenhAn();
        return hs == null ? null : hs.getKhachHang();
    }

    /** Chữ trạng thái thống nhất giữa danh sách và hộp chi tiết. */
    public static String tenTrangThai(HoaDon hd) {
        return hd.isTrangThai() ? TrangThai.DA_THANH_TOAN.ten() : TrangThai.CHO_THANH_TOAN.ten();
    }

    /** Chữ thường, bỏ dấu, gộp khoảng trắng — để so khớp từ khóa tiếng Việt. */
    static String boDau(String text) {
        if (text == null) {
            return "";
        }
        String khongDau = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D');
        return khongDau.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }
}
