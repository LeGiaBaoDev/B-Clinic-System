package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.dao.support.MaSinh;
import com.quanlynhakhoa.dao.support.NhatKy;
import com.quanlynhakhoa.dao.support.PhanQuyen;
import com.quanlynhakhoa.dao.support.PhanQuyen.Actor;
import com.quanlynhakhoa.entity.HoSoBenhAn;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.sql.*;
import java.util.List;

public class HoSoBenhAnDAO {

    private static final String SQL_SELECT = """
            SELECT maHSBA, tienSuBenh, maKH
            FROM HoSoBenhAn
            """;

    public List<HoSoBenhAn> layTatCa() {
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.QUANLY, VaiTro.NHASI);
            return JdbcHelper.query(c, SQL_SELECT
                    + " WHERE (?=1 OR EXISTS(SELECT 1 FROM LichHen lh"
                    + " WHERE lh.maKH=HoSoBenhAn.maKH AND lh.maNV=?)) ORDER BY maHSBA",
                    r -> mapRow(c, r), actor.vaiTro() == VaiTro.QUANLY, actor.maNV());
        });
    }

    public HoSoBenhAn layTheoMa(String ma) {
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.QUANLY, VaiTro.NHASI);
            HoSoBenhAn hs = layTheoMa(c, ma);
            if (hs != null && actor.vaiTro() == VaiTro.NHASI) {
                yeuCau(JdbcHelper.exists(c, "SELECT 1 FROM LichHen WHERE maKH=? AND maNV=?",
                        hs.getKhachHang().getMaKH(), actor.maNV()),
                        "Bạn không được xem bệnh án này.");
            }
            return hs;
        });
    }

    HoSoBenhAn layTheoMa(Connection c, String ma) throws SQLException {
        return JdbcHelper.one(c, SQL_SELECT + " WHERE maHSBA=?", r -> mapRow(c, r), ma);
    }

    HoSoBenhAn layTheoMaKhachHang(Connection c, String maKH) throws SQLException {
        return JdbcHelper.one(c, SQL_SELECT + " WHERE maKH=?", r -> mapRow(c, r), maKH);
    }

    public boolean them(HoSoBenhAn hs) {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY, VaiTro.LETAN);
            them(c, hs);
            NhatKy.ghi(c, "TAO_HO_SO", hs.getMaHSBA());
            return true;
        });
    }

    void them(Connection c, HoSoBenhAn hs) throws SQLException {
        String ma = MaSinh.maMoi(c, MaSinh.Bang.HO_SO, null);
        JdbcHelper.update(c, "INSERT INTO HoSoBenhAn(maHSBA,maKH,tienSuBenh) VALUES(?,?,?)",
                ma, hs.getKhachHang().getMaKH(), hs.getTienSuBenh());
        hs.setMaHSBA(ma);
    }

    private HoSoBenhAn mapRow(Connection c, ResultSet r) throws SQLException {
        return new HoSoBenhAn(
                r.getString("maHSBA"),
                r.getString("tienSuBenh"),
                new KhachHangDAO().layTheoMa(c, r.getString("maKH")));
    }

    private static void yeuCau(boolean dieuKien, String message) {
        if (!dieuKien) {
            throw new BusinessException(message);
        }
    }
}
