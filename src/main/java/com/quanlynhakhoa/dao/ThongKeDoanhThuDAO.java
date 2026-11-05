package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.dao.support.PhanQuyen;
import com.quanlynhakhoa.dao.support.PhanQuyen.Actor;
import com.quanlynhakhoa.entity.HoaDon;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class ThongKeDoanhThuDAO {

    private static final String TONG = """
            WITH TienPhieu AS (
                SELECT maPK,SUM(thanhTien+ROUND(thanhTien*mucThueGTGT/100,0)) AS tong
                FROM ChiTietPhieuKham GROUP BY maPK
            )
            """;

    public record TheoNgay(LocalDate ngay, long soHoaDon, BigDecimal doanhThu,
            BigDecimal tienMat, BigDecimal chuyenKhoan) {

    }

    public List<TheoNgay> thongKe(LocalDate tu, LocalDate den) {
        khoangNgay(tu, den);
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.QUANLY, VaiTro.LETAN);
            yeuCau(actor.vaiTro() == VaiTro.QUANLY || tu.equals(den),
                    "Lễ tân chỉ được xem doanh thu theo ngày.");
            return JdbcHelper.query(c, TONG
                    + "SELECT CAST(hd.ngayThanhToan AS DATE),COUNT_BIG(*),"
                    + "SUM(t.tong-hd.tongKhuyenMai),"
                    + "SUM(CASE WHEN hd.hinhThucThanhToan=? THEN t.tong-hd.tongKhuyenMai ELSE 0 END),"
                    + "SUM(CASE WHEN hd.hinhThucThanhToan=? THEN t.tong-hd.tongKhuyenMai ELSE 0 END)"
                    + " FROM HoaDon hd JOIN TienPhieu t ON t.maPK=hd.maPK"
                    + " WHERE hd.trangThai=1 AND hd.ngayThanhToan>=? AND hd.ngayThanhToan<?"
                    + " GROUP BY CAST(hd.ngayThanhToan AS DATE)"
                    + " ORDER BY CAST(hd.ngayThanhToan AS DATE)",
                    r -> new TheoNgay(r.getObject(1, LocalDate.class), r.getLong(2),
                            r.getBigDecimal(3), r.getBigDecimal(4), r.getBigDecimal(5)),
                    HoaDon.TIEN_MAT, HoaDon.CHUYEN_KHOAN,
                    tu.atStartOfDay(), den.plusDays(1).atStartOfDay());
        });
    }

    public record DichVuSuDung(String maDV, String tenDV, long soLuong, BigDecimal tienTruocGiam) {

    }

    /**
     * Tổng dịch vụ sau thuế trước voucher; không cộng nhầm giảm giá cả hóa đơn
     * vào mỗi dòng.
     */
    public List<DichVuSuDung> dichVuSuDung(LocalDate tu, LocalDate den) {
        khoangNgay(tu, den);
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            return JdbcHelper.query(c, "SELECT dv.maDV,dv.tenDV,SUM(CAST(ct.soLuong AS BIGINT)),"
                    + "SUM(ct.thanhTien+ROUND(ct.thanhTien*ct.mucThueGTGT/100,0))"
                    + " FROM ChiTietPhieuKham ct JOIN DichVu dv ON dv.maDV=ct.maDV"
                    + " JOIN HoaDon hd ON hd.maPK=ct.maPK"
                    + " WHERE hd.trangThai=1 AND hd.ngayThanhToan>=? AND hd.ngayThanhToan<?"
                    + " GROUP BY dv.maDV,dv.tenDV"
                    + " ORDER BY SUM(CAST(ct.soLuong AS BIGINT)) DESC,dv.maDV",
                    r -> new DichVuSuDung(r.getString(1), r.getString(2), r.getLong(3),
                            r.getBigDecimal(4)), tu.atStartOfDay(), den.plusDays(1).atStartOfDay());
        });
    }

    public record SoSanh(BigDecimal kyTruoc, BigDecimal kySau, BigDecimal chenhLech) {

    }

    public SoSanh soSanh(LocalDate tuTruoc, LocalDate denTruoc,
            LocalDate tuSau, LocalDate denSau) {
        BigDecimal truoc = thongKe(tuTruoc, denTruoc).stream()
                .map(TheoNgay::doanhThu).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sau = thongKe(tuSau, denSau).stream()
                .map(TheoNgay::doanhThu).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SoSanh(truoc, sau, sau.subtract(truoc));
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
