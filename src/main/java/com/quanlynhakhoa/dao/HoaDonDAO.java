package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.dao.support.MaSinh;
import com.quanlynhakhoa.dao.support.NhatKy;
import com.quanlynhakhoa.dao.support.PhanQuyen;
import com.quanlynhakhoa.dao.support.PhanQuyen.Actor;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import com.quanlynhakhoa.exception.BusinessException;
import java.math.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class HoaDonDAO {

    private static final String SQL_SELECT = """
            SELECT maHD, ngayLap, trangThai, hinhThucThanhToan, ngayThanhToan, tongKhuyenMai,
                   maPK, maVoucher, maNV_ThuNgan
            FROM HoaDon
            """;

    public List<HoaDon> layTatCa() {
        return timKiem(LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1), null);
    }

    public List<HoaDon> timKiem(LocalDate tu, LocalDate den, Boolean daTra) {
        khoangNgay(tu, den);
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.QUANLY, VaiTro.LETAN);
            return JdbcHelper.query(c, SQL_SELECT
                    + " WHERE ngayLap>=? AND ngayLap<? AND (? IS NULL OR trangThai=?)"
                    + " ORDER BY ngayLap DESC",
                    r -> loc(mapRow(c, r), actor.vaiTro()),
                    tu.atStartOfDay(), den.plusDays(1).atStartOfDay(), daTra, daTra);
        });
    }

    public HoaDon layTheoMa(String ma) {
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.QUANLY, VaiTro.LETAN);
            return loc(layTheoMa(c, ma), actor.vaiTro());
        });
    }

    public HoaDon layTheoPhieuKham(String maPK) {
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.QUANLY, VaiTro.LETAN);
            return loc(JdbcHelper.one(c, SQL_SELECT + " WHERE maPK=?",
                    r -> mapRow(c, r), maPK), actor.vaiTro());
        });
    }

    private HoaDon layTheoMa(Connection c, String ma) throws SQLException {
        return JdbcHelper.one(c, SQL_SELECT + " WHERE maHD=?",
                r -> mapRow(c, r), ma);
    }

    String taoTuPhieuKham(Connection c, String maPK) throws SQLException {
        yeuCau(JdbcHelper.exists(c, "SELECT 1 FROM PhieuKham WHERE maPK=?"
                + " AND trangThai='HOAN_TAT'", maPK), "Phiếu khám chưa hoàn tất.");
        yeuCau(!JdbcHelper.exists(c, "SELECT 1 FROM HoaDon WITH(UPDLOCK,HOLDLOCK) WHERE maPK=?",
                maPK), "Phiếu khám đã có hóa đơn.");

        String ma = MaSinh.maMoi(c, MaSinh.Bang.HOA_DON, LocalDate.now());
        JdbcHelper.update(c, "INSERT INTO HoaDon(maHD,maPK,ngayLap) VALUES(?,?,?)",
                ma, maPK, LocalDateTime.now());
        return ma;
    }

    static BigDecimal tongTruocGiam(Connection c, String maPK) throws SQLException {
        return JdbcHelper.one(c, "SELECT COALESCE(SUM(thanhTien"
                + "+ROUND(thanhTien*mucThueGTGT/100,0)),0)"
                + " FROM ChiTietPhieuKham WHERE maPK=?",
                r -> r.getBigDecimal(1), maPK);
    }

    public static BigDecimal tinhTienGiam(Voucher vc, BigDecimal tong) {
        return tienGiam(vc, tong);
    }

    static BigDecimal tienGiam(Voucher vc, BigDecimal tong) {
        BigDecimal giam = BigDecimal.valueOf(vc.getGiaGiam());
        if (vc.isLoaiGiam()) {
            giam = tong.multiply(giam).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (vc.getGiaTriGiamToiDa() > 0) {
                giam = giam.min(BigDecimal.valueOf(vc.getGiaTriGiamToiDa()));
            }
        }
        return giam.min(tong).max(BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP);
    }

    public BigDecimal tinhTongTien(String maHD) {
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY, VaiTro.LETAN);
            HoaDon hd = layTheoMa(c, maHD);
            yeuCau(hd != null, "Hóa đơn không tồn tại.");
            return tongTruocGiam(c, hd.getPhieuKham().getMaPK())
                    .subtract(BigDecimal.valueOf(hd.getTongKhuyenMai()))
                    .max(BigDecimal.ZERO);
        });
    }

    public boolean apDungVoucher(String maHD, String maVoucher) {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.LETAN, VaiTro.QUANLY);
            MaSinh.khoa(c, "BClinic:thanh-toan");
            HoaDon hd = layTheoMa(c, maHD);
            yeuCau(hd != null && !hd.isTrangThai(), "Hóa đơn không tồn tại hoặc đã thanh toán.");
            yeuCau(hd.getVoucher() == null, "Mỗi hóa đơn chỉ được áp dụng một voucher.");

            Voucher vc = new VoucherDAO().layTheoMa(c, maVoucher);
            yeuCau(vc != null && vc.kiemTraHieuLuc(LocalDate.now()), "Voucher không có hiệu lực.");
            BigDecimal giam = tienGiam(vc, tongTruocGiam(c, hd.getPhieuKham().getMaPK()));
            JdbcHelper.update(c, "UPDATE HoaDon SET maVoucher=?,tongKhuyenMai=?"
                    + " WHERE maHD=? AND trangThai=0", maVoucher, giam, maHD);
            NhatKy.ghi(c, "AP_DUNG_VOUCHER", maHD + " / " + maVoucher);
            return true;
        });
    }

    /**
     * Với tiền mặt yêu cầu tiền nhận đủ; chuyển khoản truyền đúng tổng để xác
     * nhận số tiền.
     */
    public BigDecimal thanhToan(String maHD, String hinhThuc, BigDecimal soTienNhan) {
        return thanhToan(maHD, hinhThuc, soTienNhan, null);
    }

    /**
     * Áp dụng voucher (nếu có) và thanh toán trong cùng một transaction để không
     * để lại hóa đơn đã giảm giá nhưng chưa thanh toán khi thao tác thất bại.
     */
    public BigDecimal thanhToan(String maHD, String hinhThuc, BigDecimal soTienNhan,
            String maVoucherMoi) {
        yeuCau(HoaDon.TIEN_MAT.equals(hinhThuc) || HoaDon.CHUYEN_KHOAN.equals(hinhThuc),
                "Hình thức thanh toán không hợp lệ.");
        String voucherChon = maVoucherMoi == null || maVoucherMoi.isBlank()
                ? null : maVoucherMoi.trim();
        return JdbcHelper.transaction(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.LETAN, VaiTro.QUANLY);
            MaSinh.khoa(c, "BClinic:thanh-toan");
            HoaDon hd = layTheoMa(c, maHD);
            yeuCau(hd != null && !hd.isTrangThai(), "Hóa đơn không tồn tại hoặc đã thanh toán.");
            String maVoucherHienTai = hd.getVoucher() == null ? null : hd.getVoucher().getMaVoucher();
            yeuCau(voucherChon == null || maVoucherHienTai == null
                    || voucherChon.equals(maVoucherHienTai),
                    "Hóa đơn đã áp dụng voucher khác.");

            BigDecimal tongTruoc = tongTruocGiam(c, hd.getPhieuKham().getMaPK());
            String maVoucherApDung = maVoucherHienTai == null ? voucherChon : maVoucherHienTai;
            BigDecimal giam = BigDecimal.valueOf(hd.getTongKhuyenMai());
            if (maVoucherApDung != null) {
                Voucher vc = new VoucherDAO().layTheoMa(c, maVoucherApDung);
                yeuCau(vc != null && vc.kiemTraHieuLuc(LocalDate.now()),
                        "Voucher không có hiệu lực.");
                giam = tienGiam(vc, tongTruoc);
                if (maVoucherHienTai != null) {
                    yeuCau(giam.compareTo(BigDecimal.valueOf(hd.getTongKhuyenMai())) == 0,
                            "Giá trị voucher đã thay đổi.");
                }
                yeuCau(maVoucherHienTai != null
                        || JdbcHelper.update(c, "UPDATE HoaDon SET maVoucher=?,tongKhuyenMai=?"
                        + " WHERE maHD=? AND trangThai=0", maVoucherApDung, giam, maHD) == 1,
                        "Không thể áp dụng voucher.");
                if (maVoucherHienTai == null) {
                    NhatKy.ghi(c, "AP_DUNG_VOUCHER", maHD + " / " + maVoucherApDung);
                }
            }

            BigDecimal tong = tongTruoc.subtract(giam).max(BigDecimal.ZERO);
            yeuCau(soTienNhan != null && soTienNhan.compareTo(tong) >= 0,
                    "Số tiền nhận chưa đủ.");
            yeuCau(!HoaDon.CHUYEN_KHOAN.equals(hinhThuc) || soTienNhan.compareTo(tong) == 0,
                    "Số tiền chuyển khoản phải khớp tổng hóa đơn.");
            yeuCau(hd.getPhieuKham().getTrangThai() == TrangThaiPhieuKham.HOAN_TAT,
                    "Phiếu khám chưa hoàn tất.");

            yeuCau(JdbcHelper.update(c, "UPDATE HoaDon SET trangThai=1,maNV_ThuNgan=?,"
                    + "hinhThucThanhToan=?,ngayThanhToan=? WHERE maHD=? AND trangThai=0",
                    actor.maNV(), hinhThuc, LocalDateTime.now(), maHD) == 1,
                    "Hóa đơn vừa được thanh toán.");

            LichHen lh = hd.getPhieuKham().getLichHen();
            if (lh != null) {
                yeuCau(JdbcHelper.update(c, "UPDATE LichHen SET trangThai='HOAN_TAT'"
                        + " WHERE maLH=? AND trangThai='DA_DEN'", lh.getMaLH()) == 1,
                        "Trạng thái lịch hẹn không phù hợp để thanh toán.");
            }
            NhatKy.ghi(c, "THANH_TOAN", maHD);
            return soTienNhan.subtract(tong);
        });
    }

    private HoaDon loc(HoaDon hd, VaiTro vaiTro) {
        if (hd != null) {
            PhieuKhamDAO.loc(hd.getPhieuKham(), vaiTro);
        }
        return hd;
    }

    private HoaDon mapRow(Connection c, ResultSet r) throws SQLException {
        String maVoucher = r.getString("maVoucher");
        String maNV = r.getString("maNV_ThuNgan");
        return new HoaDon(
                r.getString("maHD"),
                r.getObject("ngayLap", LocalDateTime.class),
                r.getBoolean("trangThai"),
                r.getString("hinhThucThanhToan"),
                r.getObject("ngayThanhToan", LocalDateTime.class),
                r.getDouble("tongKhuyenMai"),
                new PhieuKhamDAO().layTheoMa(c, r.getString("maPK"), true),
                maVoucher == null ? null : new VoucherDAO().layTheoMa(c, maVoucher),
                maNV == null ? null : new NhanVienDAO().layTheoMa(c, maNV));
    }

    private static void khoangNgay(LocalDate tu, LocalDate den) {
        yeuCau(tu != null && den != null && !den.isBefore(tu), "Khoảng ngày không hợp lệ.");
    }

    private static void yeuCau(boolean dieuKien, String message) {
        if (!dieuKien) {
            throw new BusinessException(message);
        }
    }
}
