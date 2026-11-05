package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.dao.support.PhanQuyen;
import com.quanlynhakhoa.dao.support.PhanQuyen.Actor;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ThongKeBenhNhanDAO {

    public record TheoNgay(LocalDate ngay, long soBenhNhan) {

    }

    public record DichVuSuDung(String maDV, String tenDV, long soLuong) {

    }

    public record BenhNhanTrongKy(String maKH, String tenKH, LocalDate ngaySinh,
            boolean gioiTinh, String sdt, String diaChi, String maPK, LocalDate ngayKham,
            String chanDoan, String nhaSi) {

    }

    public List<BenhNhanTrongKy> danhSachBenhNhan(LocalDate tu, LocalDate den) {
        khoangNgay(tu, den);
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.NHASI, VaiTro.QUANLY);
            return JdbcHelper.query(c, """
                    WITH LanKham AS (
                        SELECT kh.maKH,kh.tenKH,kh.ngaySinh,kh.gioiTinh,kh.sdt,kh.diaChi,
                               pk.maPK,pk.ngayLap,nv.hoTenNV,
                               (SELECT TOP 1 ct.chanDoan FROM ChiTietPhieuKham ct
                                WHERE ct.maPK=pk.maPK ORDER BY ct.maCTPK) AS chanDoan,
                               ROW_NUMBER() OVER(PARTITION BY kh.maKH
                                   ORDER BY pk.ngayLap DESC,pk.maPK DESC) AS rn
                        FROM PhieuKham pk
                        JOIN HoSoBenhAn hs ON hs.maHSBA=pk.maHSBA
                        JOIN KhachHang kh ON kh.maKH=hs.maKH
                        JOIN LichHen lh ON lh.maLH=pk.maLH
                        JOIN NhanVien nv ON nv.maNV=lh.maNV
                        WHERE pk.trangThai='HOAN_TAT' AND pk.ngayLap>=? AND pk.ngayLap<?
                          AND (?=1 OR lh.maNV=?)
                    )
                    SELECT maKH,tenKH,ngaySinh,gioiTinh,sdt,diaChi,maPK,
                           CAST(ngayLap AS DATE),chanDoan,hoTenNV
                    FROM LanKham WHERE rn=1 ORDER BY ngayLap DESC,tenKH
                    """,
                    r -> new BenhNhanTrongKy(r.getString(1), r.getString(2),
                            r.getObject(3, LocalDate.class), r.getBoolean(4), r.getString(5),
                            r.getString(6), r.getString(7), r.getObject(8, LocalDate.class),
                            r.getString(9), r.getString(10)),
                    tu.atStartOfDay(), den.plusDays(1).atStartOfDay(),
                    actor.vaiTro() == VaiTro.QUANLY, actor.maNV());
        });
    }

    public List<TheoNgay> thongKe(LocalDate tu, LocalDate den) {
        khoangNgay(tu, den);
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.NHASI, VaiTro.QUANLY);
            return JdbcHelper.query(c, "SELECT CAST(pk.ngayLap AS DATE),COUNT(DISTINCT hs.maKH)"
                    + " FROM PhieuKham pk JOIN HoSoBenhAn hs ON hs.maHSBA=pk.maHSBA"
                    + " JOIN LichHen lh ON lh.maLH=pk.maLH"
                    + " WHERE pk.trangThai='HOAN_TAT' AND pk.ngayLap>=? AND pk.ngayLap<?"
                    + " AND (?=1 OR lh.maNV=?)"
                    + " GROUP BY CAST(pk.ngayLap AS DATE)"
                    + " ORDER BY CAST(pk.ngayLap AS DATE)",
                    r -> new TheoNgay(r.getObject(1, LocalDate.class), r.getLong(2)),
                    tu.atStartOfDay(), den.plusDays(1).atStartOfDay(),
                    actor.vaiTro() == VaiTro.QUANLY, actor.maNV());
        });
    }

    public List<DichVuSuDung> dichVuSuDung(LocalDate tu, LocalDate den) {
        khoangNgay(tu, den);
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.NHASI, VaiTro.QUANLY);
            return JdbcHelper.query(c, """
                    SELECT dv.maDV,dv.tenDV,SUM(CAST(ct.soLuong AS BIGINT))
                    FROM ChiTietPhieuKham ct
                    JOIN DichVu dv ON dv.maDV=ct.maDV
                    JOIN PhieuKham pk ON pk.maPK=ct.maPK
                    WHERE pk.trangThai='HOAN_TAT' AND pk.ngayLap>=? AND pk.ngayLap<?
                      AND (?=1 OR ct.maNV_NhaSi=?)
                    GROUP BY dv.maDV,dv.tenDV
                    ORDER BY SUM(CAST(ct.soLuong AS BIGINT)) DESC,dv.maDV
                    """,
                    r -> new DichVuSuDung(r.getString(1), r.getString(2), r.getLong(3)),
                    tu.atStartOfDay(), den.plusDays(1).atStartOfDay(),
                    actor.vaiTro() == VaiTro.QUANLY, actor.maNV());
        });
    }

    public long demBenhNhanKhacNhau(LocalDate tu, LocalDate den) {
        khoangNgay(tu, den);
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.NHASI, VaiTro.QUANLY);
            return JdbcHelper.one(c, "SELECT COUNT(DISTINCT hs.maKH) FROM PhieuKham pk"
                    + " JOIN HoSoBenhAn hs ON hs.maHSBA=pk.maHSBA"
                    + " JOIN LichHen lh ON lh.maLH=pk.maLH"
                    + " WHERE pk.trangThai='HOAN_TAT' AND pk.ngayLap>=? AND pk.ngayLap<?"
                    + " AND (?=1 OR lh.maNV=?)",
                    r -> r.getLong(1), tu.atStartOfDay(), den.plusDays(1).atStartOfDay(),
                    actor.vaiTro() == VaiTro.QUANLY, actor.maNV());
        });
    }

    /** Tổng hợp các chỉ số trong khoảng: lượt khám, bệnh nhân khác nhau, bệnh nhân mới, doanh thu. */
    public record TomTat(long soLuotKham, long soBenhNhan, long soBenhNhanMoi, double doanhThu) {

    }

    public TomTat tongHop(LocalDate tu, LocalDate den) {
        khoangNgay(tu, den);
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.NHASI, VaiTro.QUANLY);
            boolean ql = actor.vaiTro() == VaiTro.QUANLY;
            String maNV = actor.maNV();
            LocalDateTime tuDt = tu.atStartOfDay();
            LocalDateTime denDt = den.plusDays(1).atStartOfDay();
            long luot = JdbcHelper.one(c,
                    "SELECT COUNT(*) FROM PhieuKham pk JOIN LichHen lh ON lh.maLH=pk.maLH"
                    + " WHERE pk.trangThai='HOAN_TAT' AND pk.ngayLap>=? AND pk.ngayLap<? AND (?=1 OR lh.maNV=?)",
                    r -> r.getLong(1), tuDt, denDt, ql, maNV);
            long benhNhan = JdbcHelper.one(c,
                    "SELECT COUNT(DISTINCT hs.maKH) FROM PhieuKham pk"
                    + " JOIN HoSoBenhAn hs ON hs.maHSBA=pk.maHSBA JOIN LichHen lh ON lh.maLH=pk.maLH"
                    + " WHERE pk.trangThai='HOAN_TAT' AND pk.ngayLap>=? AND pk.ngayLap<? AND (?=1 OR lh.maNV=?)",
                    r -> r.getLong(1), tuDt, denDt, ql, maNV);
            long moi = JdbcHelper.one(c,
                    "SELECT COUNT(DISTINCT hs.maKH) FROM PhieuKham pk"
                    + " JOIN HoSoBenhAn hs ON hs.maHSBA=pk.maHSBA JOIN LichHen lh ON lh.maLH=pk.maLH"
                    + " WHERE pk.trangThai='HOAN_TAT' AND pk.ngayLap>=? AND pk.ngayLap<? AND (?=1 OR lh.maNV=?)"
                    + " AND NOT EXISTS (SELECT 1 FROM PhieuKham pk2"
                    + " JOIN HoSoBenhAn hs2 ON hs2.maHSBA=pk2.maHSBA JOIN LichHen lh2 ON lh2.maLH=pk2.maLH"
                    + " WHERE hs2.maKH=hs.maKH AND pk2.trangThai='HOAN_TAT' AND pk2.ngayLap<? AND (?=1 OR lh2.maNV=?))",
                    r -> r.getLong(1), tuDt, denDt, ql, maNV, tuDt, ql, maNV);
            double doanhThu = JdbcHelper.one(c,
                    "SELECT COALESCE(SUM(ct.thanhTien),0) FROM ChiTietPhieuKham ct"
                    + " JOIN PhieuKham pk ON pk.maPK=ct.maPK JOIN LichHen lh ON lh.maLH=pk.maLH"
                    + " WHERE pk.trangThai='HOAN_TAT' AND pk.ngayLap>=? AND pk.ngayLap<? AND (?=1 OR lh.maNV=?)",
                    r -> r.getDouble(1), tuDt, denDt, ql, maNV);
            return new TomTat(luot, benhNhan, moi, doanhThu);
        });
    }

    /** Dịch vụ được nha sĩ thực hiện nhiều nhất (theo tổng số lượng) trong khoảng. */
    public record DichVuTop(String tenDV, long soLuong) {

    }

    public DichVuTop dichVuNhieuNhat(LocalDate tu, LocalDate den) {
        khoangNgay(tu, den);
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.NHASI, VaiTro.QUANLY);
            return JdbcHelper.one(c,
                    "SELECT TOP 1 dv.tenDV, SUM(ct.soLuong) FROM ChiTietPhieuKham ct"
                    + " JOIN DichVu dv ON dv.maDV=ct.maDV JOIN PhieuKham pk ON pk.maPK=ct.maPK"
                    + " JOIN LichHen lh ON lh.maLH=pk.maLH"
                    + " WHERE pk.trangThai='HOAN_TAT' AND pk.ngayLap>=? AND pk.ngayLap<? AND (?=1 OR lh.maNV=?)"
                    + " GROUP BY dv.tenDV ORDER BY SUM(ct.soLuong) DESC",
                    r -> new DichVuTop(r.getString(1), r.getLong(2)),
                    tu.atStartOfDay(), den.plusDays(1).atStartOfDay(),
                    actor.vaiTro() == VaiTro.QUANLY, actor.maNV());
        });
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
