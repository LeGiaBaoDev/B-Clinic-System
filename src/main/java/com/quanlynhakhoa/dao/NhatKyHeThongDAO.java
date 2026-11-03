package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.dao.support.NhatKy;
import com.quanlynhakhoa.dao.support.PhanQuyen;
import com.quanlynhakhoa.entity.NhatKyHeThong;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class NhatKyHeThongDAO {

    private static final String SQL_SELECT = """
            SELECT maNhatKy, thoiDiem, hanhDong, noiDungThayDoi, maTK
            FROM NhatKyHeThong
            """;

    public void ghi(String hanhDong, String noiDung) {
        yeuCau(hanhDong != null && !hanhDong.isBlank() && hanhDong.length() <= 100,
                "Hành động nhật ký không hợp lệ.");
        JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c);
            NhatKy.ghi(c, hanhDong, noiDung);
            return null;
        });
    }

    public List<NhatKyHeThong> layTatCa() {
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            return JdbcHelper.query(c, SQL_SELECT + " ORDER BY thoiDiem DESC, maNhatKy DESC",
                    r -> mapRow(c, r));
        });
    }

    public List<NhatKyHeThong> timKiem(LocalDate tu, LocalDate den, String maTK) {
        khoangNgay(tu, den);
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            return JdbcHelper.query(c, SQL_SELECT
                    + " WHERE thoiDiem>=? AND thoiDiem<? AND (? IS NULL OR maTK=?)"
                    + " ORDER BY thoiDiem DESC",
                    r -> mapRow(c, r), tu.atStartOfDay(), den.plusDays(1).atStartOfDay(),
                    maTK, maTK);
        });
    }

    private NhatKyHeThong mapRow(Connection c, ResultSet r) throws SQLException {
        return new NhatKyHeThong(
                r.getString("maNhatKy"),
                r.getObject("thoiDiem", LocalDateTime.class),
                r.getString("hanhDong"),
                r.getString("noiDungThayDoi"),
                new TaiKhoanDAO().layTheoMa(c, r.getString("maTK")));
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
