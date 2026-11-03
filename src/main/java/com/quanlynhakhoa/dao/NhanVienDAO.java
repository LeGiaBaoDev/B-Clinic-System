package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.entity.NhanVien;
import com.quanlynhakhoa.entity.enums.VaiTro;
import java.sql.*;
import java.util.List;

/** UC002–004: thông tin nhân viên; tài khoản được tạo bằng TaiKhoanDAO.taoNhanVienVaTaiKhoan. */
public class NhanVienDAO extends DaoSupport {
    public NhanVienDAO() { }
    public NhanVienDAO(DaoContext context) { super(context); }

    public List<NhanVien> layTatCa() {
        return context.doc(c -> { quyen(c); return query(c, DaoViews.NHAN_VIEN + " ORDER BY a.maNV", this::mapRow); });
    }

    public NhanVien layTheoMa(String ma) {
        return context.doc(c -> { quyen(c); return layTheoMa(c, ma); });
    }

    NhanVien layTheoMa(Connection c, String ma) throws SQLException {
        return one(c, DaoViews.NHAN_VIEN + " WHERE a.maNV=?", this::mapRow, ma);
    }

    public List<NhanVien> timKiem(String tuKhoa) {
        return context.doc(c -> { quyen(c); return query(c, DaoViews.NHAN_VIEN
                + " WHERE a.maNV LIKE ? OR a.hoTenNV LIKE ? OR a.sdt LIKE ? ORDER BY a.maNV",
                this::mapRow, tuKhoa(tuKhoa), tuKhoa(tuKhoa), tuKhoa(tuKhoa)); });
    }

    public List<NhanVien> layNhaSiDangHoatDong() {
        return context.doc(c -> { quyen(c); return query(c, DaoViews.NHAN_VIEN
                + " WHERE EXISTS(SELECT 1 FROM TaiKhoan tk WHERE tk.maNV=a.maNV AND tk.vaiTro='NHASI' AND tk.trangThai=1)", this::mapRow); });
    }

    public boolean them(NhanVien nv) {
        return context.transaction(c -> { quyen(c, VaiTro.QUANLY); them(c,nv); nhatKy(c,"THEM_NHAN_VIEN",nv.getMaNV()); return true; });
    }

    void them(Connection c, NhanVien nv) throws SQLException {
        new NhanVien(null,nv.getHoTenNV(),nv.isGioiTinh(),nv.getSdt(),nv.getEmail(),nv.getChuyenMon());
        String ma = maMoi(c, Bang.NHAN_VIEN, null);
        update(c,"INSERT INTO NhanVien(maNV,hoTenNV,gioiTinh,sdt,email,chuyenMon) VALUES(?,?,?,?,?,?)",
                ma,nv.getHoTenNV(),nv.isGioiTinh(),nv.getSdt(),nv.getEmail(),nv.getChuyenMon());
        nv.setMaNV(ma);
    }

    public boolean capNhat(NhanVien nv) {
        return context.transaction(c -> {
            quyen(c,VaiTro.QUANLY);
            new NhanVien(nv);
            boolean ok = update(c,"UPDATE NhanVien SET hoTenNV=?,gioiTinh=?,sdt=?,email=?,chuyenMon=? WHERE maNV=?",
                    nv.getHoTenNV(),nv.isGioiTinh(),nv.getSdt(),nv.getEmail(),nv.getChuyenMon(),nv.getMaNV())==1;
            if(ok) { nhatKy(c,"CAP_NHAT_NHAN_VIEN",nv.getMaNV()); }
            return ok;
        });
    }

    private NhanVien mapRow(ResultSet r) throws SQLException { return DaoViews.nhanVien(r); }
}
