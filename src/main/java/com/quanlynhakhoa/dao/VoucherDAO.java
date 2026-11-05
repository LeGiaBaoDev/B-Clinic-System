package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.dao.support.MaSinh;
import com.quanlynhakhoa.dao.support.NhatKy;
import com.quanlynhakhoa.dao.support.PhanQuyen;
import com.quanlynhakhoa.entity.Voucher;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class VoucherDAO {

    /** Xem mã còn trống, không giữ chỗ; them() vẫn cấp lại mã trong transaction lưu. */
    public String layMaDuKien() {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            return MaSinh.maMoi(c, MaSinh.Bang.VOUCHER, null);
        });
    }

    /** Dùng để khóa chỉnh sửa nội dung trên UI; DAO vẫn kiểm tra lại khi lưu. */
    public Set<String> layMaDaSuDung() {
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            return new HashSet<>(JdbcHelper.query(c,
                    "SELECT DISTINCT maVoucher FROM HoaDon WHERE maVoucher IS NOT NULL",
                    r -> r.getString(1)));
        });
    }

    private static final String SQL_SELECT = """
            SELECT maVoucher, tenVoucher, loaiGiam, giaGiam, giaTriGiamToiDa,
                   ngayBatDau, ngayKetThuc, trangThai
            FROM Voucher
            """;

    public List<Voucher> layTatCa() {
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            return JdbcHelper.query(c, SQL_SELECT + " ORDER BY maVoucher", this::mapRow);
        });
    }

    public Voucher layTheoMa(String ma) {
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY, VaiTro.LETAN);
            return layTheoMa(c, ma);
        });
    }

    Voucher layTheoMa(Connection c, String ma) throws SQLException {
        return JdbcHelper.one(c, SQL_SELECT + " WHERE maVoucher=?", this::mapRow, ma);
    }

    public List<Voucher> layConHieuLuc(LocalDate ngay) {
        yeuCau(ngay != null, "Thiếu ngày áp dụng.");
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY, VaiTro.LETAN);
            return JdbcHelper.query(c, SQL_SELECT
                            + " WHERE trangThai=1 AND ? BETWEEN ngayBatDau AND ngayKetThuc"
                            + " ORDER BY maVoucher",
                    this::mapRow, ngay);
        });
    }

    public boolean them(Voucher v) { return luu(v, true); }

    public boolean capNhat(Voucher v) { return luu(v, false); }

    private boolean luu(Voucher v, boolean themMoi) {
        String maDaLuu = JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            MaSinh.khoa(c, "BClinic:thanh-toan");
            new Voucher(v.getMaVoucher(), v.getTenVoucher(), v.isLoaiGiam(), v.getGiaGiam(),
                    v.getGiaTriGiamToiDa(), v.getNgayBatDau(), v.getNgayKetThuc(), v.isTrangThai());

            if (!themMoi) {
                yeuCau(!JdbcHelper.exists(c,
                                "SELECT 1 FROM HoaDon WITH(UPDLOCK,HOLDLOCK) WHERE maVoucher=?",
                                v.getMaVoucher()),
                        "Voucher đã dùng; chỉ được vô hiệu hóa để giữ lịch sử.");
            }

            String ma = themMoi ? MaSinh.maMoi(c, MaSinh.Bang.VOUCHER, null) : v.getMaVoucher();
            Object cap = v.getGiaTriGiamToiDa() == 0 ? null
                    : BigDecimal.valueOf(v.getGiaTriGiamToiDa());
            Object[] args = {
                    v.getTenVoucher(), v.isLoaiGiam() ? 1 : 0, BigDecimal.valueOf(v.getGiaGiam()),
                    cap, v.getNgayBatDau(), v.getNgayKetThuc(), v.isTrangThai(), ma
            };
            int rows = JdbcHelper.update(c, themMoi
                            ? "INSERT INTO Voucher(tenVoucher,loaiGiam,giaGiam,giaTriGiamToiDa,"
                            + "ngayBatDau,ngayKetThuc,trangThai,maVoucher) VALUES(?,?,?,?,?,?,?,?)"
                            : "UPDATE Voucher SET tenVoucher=?,loaiGiam=?,giaGiam=?,giaTriGiamToiDa=?,"
                            + "ngayBatDau=?,ngayKetThuc=?,trangThai=? WHERE maVoucher=?",
                    args);
            if (rows == 1) {
                NhatKy.ghi(c, themMoi ? "THEM_VOUCHER" : "CAP_NHAT_VOUCHER", ma);
            }
            return rows == 1 ? ma : null;
        });
        // Chỉ công bố mã sau khi cả dữ liệu và nhật ký đã commit thành công.
        if (maDaLuu == null) { return false; }
        v.setMaVoucher(maDaLuu);
        return true;
    }

    public boolean xoa(String ma) {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            MaSinh.khoa(c, "BClinic:thanh-toan");
            boolean ok = JdbcHelper.update(c, "UPDATE Voucher SET trangThai=0 WHERE maVoucher=?", ma) == 1;
            if (ok) { NhatKy.ghi(c, "VO_HIEU_VOUCHER", ma); }
            return ok;
        });
    }

    private Voucher mapRow(ResultSet r) throws SQLException {
        return new Voucher(
                r.getString("maVoucher"),
                r.getString("tenVoucher"),
                r.getBoolean("loaiGiam"),
                r.getDouble("giaGiam"),
                r.getDouble("giaTriGiamToiDa"),
                r.getObject("ngayBatDau", LocalDate.class),
                r.getObject("ngayKetThuc", LocalDate.class),
                r.getBoolean("trangThai"));
    }

    private static void yeuCau(boolean dieuKien, String message) {
        if (!dieuKien) { throw new BusinessException(message); }
    }
}
