package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.dao.support.MaSinh;
import com.quanlynhakhoa.dao.support.NhatKy;
import com.quanlynhakhoa.dao.support.PhanQuyen;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.VaiTro;
import java.sql.*;
import java.util.*;

public class NhanVienDAO {

    private static final String SQL_SELECT = """
            SELECT maNV, hoTenNV, gioiTinh, sdt, email, chuyenMon
            FROM NhanVien
            """;

    public List<NhanVien> layTatCa() {
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c);
            return JdbcHelper.query(c, SQL_SELECT + " ORDER BY maNV", this::mapRow);
        });
    }

    public NhanVien layTheoMa(String ma) {
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c);
            return layTheoMa(c, ma);
        });
    }

    NhanVien layTheoMa(Connection c, String ma) throws SQLException {
        return JdbcHelper.one(c, SQL_SELECT + " WHERE maNV=?", this::mapRow, ma);
    }

    public List<NhanVien> timKiem(String tuKhoa) {
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c);
            return JdbcHelper.query(c, SQL_SELECT
                    + " WHERE maNV LIKE ? OR hoTenNV LIKE ? OR sdt LIKE ? ORDER BY maNV",
                    this::mapRow, JdbcHelper.tuKhoa(tuKhoa), JdbcHelper.tuKhoa(tuKhoa),
                    JdbcHelper.tuKhoa(tuKhoa));
        });
    }

    /**
     * Nha sĩ đang có tài khoản hoạt động — dùng cho lịch làm việc và đặt lịch.
     */
    public List<NhanVien> layNhaSiDangHoatDong() {
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c);
            return JdbcHelper.query(c, SQL_SELECT + " WHERE EXISTS(SELECT 1 FROM TaiKhoan tk"
                    + " WHERE tk.maNV=NhanVien.maNV AND tk.vaiTro='NHASI' AND tk.trangThai=1)"
                    + " ORDER BY maNV", this::mapRow);
        });
    }

    public boolean them(NhanVien nv) {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            them(c, nv);
            NhatKy.ghi(c, "THEM_NHAN_VIEN", nv.getMaNV());
            return true;
        });
    }

    void them(Connection c, NhanVien nv) throws SQLException {
        new NhanVien(null, nv.getHoTenNV(), nv.isGioiTinh(),
                nv.getSdt(), nv.getEmail(), nv.getChuyenMon());

        String ma = MaSinh.maMoi(c, MaSinh.Bang.NHAN_VIEN, null);
        JdbcHelper.update(c, "INSERT INTO NhanVien(maNV,hoTenNV,gioiTinh,sdt,email,chuyenMon)"
                + " VALUES(?,?,?,?,?,?)",
                ma, nv.getHoTenNV(), nv.isGioiTinh(), nv.getSdt(),
                nv.getEmail(), nv.getChuyenMon());
        nv.setMaNV(ma);
    }

    public boolean capNhat(NhanVien nv) {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            new NhanVien(nv);
            boolean ok = JdbcHelper.update(c,
                    "UPDATE NhanVien SET hoTenNV=?,gioiTinh=?,sdt=?,email=?,chuyenMon=? WHERE maNV=?",
                    nv.getHoTenNV(), nv.isGioiTinh(), nv.getSdt(), nv.getEmail(),
                    nv.getChuyenMon(), nv.getMaNV()) == 1;
            if (ok) {
                NhatKy.ghi(c, "CAP_NHAT_NHAN_VIEN", nv.getMaNV());
            }
            return ok;
        });
    }

    private NhanVien mapRow(ResultSet r) throws SQLException {
        return new NhanVien(
                r.getString("maNV"),
                r.getString("hoTenNV"),
                r.getBoolean("gioiTinh"),
                r.getString("sdt"),
                r.getString("email"),
                r.getString("chuyenMon"));
    }
}
