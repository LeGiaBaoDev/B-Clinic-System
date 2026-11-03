package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import java.sql.*;
import java.time.*;

/** JOIN đầy đủ trong một truy vấn. Phiếu nguồn tái khám không nạp ngược lịch để tránh vòng lặp. */
final class DaoViews {
    private DaoViews() { }
    static final String NHAN_VIEN = """
            SELECT a.maNV AS a_maNV,
                   a.hoTenNV AS a_hoTenNV,
                   a.gioiTinh AS a_gioiTinh,
                   a.sdt AS a_sdt,
                   a.email AS a_email,
                   a.chuyenMon AS a_chuyenMon
            FROM NhanVien a
            """;

    static NhanVien nhanVien(ResultSet r) throws SQLException {
        return map_NhanVien_a(r);
    }

    private static NhanVien map_NhanVien_a(ResultSet r) throws SQLException {
        if (r.getObject("a_maNV") == null) { return null; }
        return new NhanVien(
                r.getString("a_maNV"),
                r.getString("a_hoTenNV"),
                r.getBoolean("a_gioiTinh"),
                r.getString("a_sdt"),
                r.getString("a_email"),
                r.getString("a_chuyenMon"));
    }

    static final String KHACH_HANG = """
            SELECT a.maKH AS a_maKH,
                   a.tenKH AS a_tenKH,
                   a.ngaySinh AS a_ngaySinh,
                   a.gioiTinh AS a_gioiTinh,
                   a.sdt AS a_sdt,
                   a.diaChi AS a_diaChi,
                   a.email AS a_email
            FROM KhachHang a
            """;

    static KhachHang khachHang(ResultSet r) throws SQLException {
        return map_KhachHang_a(r);
    }

    private static KhachHang map_KhachHang_a(ResultSet r) throws SQLException {
        if (r.getObject("a_maKH") == null) { return null; }
        return new KhachHang(
                r.getString("a_maKH"),
                r.getString("a_tenKH"),
                r.getObject("a_ngaySinh", LocalDate.class),
                r.getBoolean("a_gioiTinh"),
                r.getString("a_sdt"),
                r.getString("a_diaChi"),
                r.getString("a_email"));
    }

    static final String LOAI_DICH_VU = """
            SELECT a.maLoaiDV AS a_maLoaiDV,
                   a.tenLoaiDV AS a_tenLoaiDV,
                   a.mucThueGTGT AS a_mucThueGTGT
            FROM LoaiDichVu a
            """;

    static LoaiDichVu loaiDichVu(ResultSet r) throws SQLException {
        return map_LoaiDichVu_a(r);
    }

    private static LoaiDichVu map_LoaiDichVu_a(ResultSet r) throws SQLException {
        if (r.getObject("a_maLoaiDV") == null) { return null; }
        return new LoaiDichVu(
                r.getString("a_maLoaiDV"),
                r.getString("a_tenLoaiDV"),
                thue(r.getInt("a_mucThueGTGT")));
    }

    static final String DICH_VU = """
            SELECT a.maDV AS a_maDV,
                   a.tenDV AS a_tenDV,
                   a.donGia AS a_donGia,
                   a.thoiLuong AS a_thoiLuong,
                   a.loTrinhDichVu AS a_loTrinhDichVu,
                   a.trangThai AS a_trangThai,
                   a_loai.maLoaiDV AS a_loai_maLoaiDV,
                   a_loai.tenLoaiDV AS a_loai_tenLoaiDV,
                   a_loai.mucThueGTGT AS a_loai_mucThueGTGT
            FROM DichVu a
            LEFT JOIN LoaiDichVu a_loai ON a_loai.maLoaiDV=a.maLoaiDV
            """;

    static DichVu dichVu(ResultSet r) throws SQLException {
        return map_DichVu_a(r);
    }

    private static LoaiDichVu map_DichVu_a_loai(ResultSet r) throws SQLException {
        if (r.getObject("a_loai_maLoaiDV") == null) { return null; }
        return new LoaiDichVu(
                r.getString("a_loai_maLoaiDV"),
                r.getString("a_loai_tenLoaiDV"),
                thue(r.getInt("a_loai_mucThueGTGT")));
    }

    private static DichVu map_DichVu_a(ResultSet r) throws SQLException {
        if (r.getObject("a_maDV") == null) { return null; }
        return new DichVu(
                r.getString("a_maDV"),
                r.getString("a_tenDV"),
                r.getDouble("a_donGia"),
                r.getInt("a_thoiLuong"),
                r.getString("a_loTrinhDichVu"),
                r.getBoolean("a_trangThai"),
                map_DichVu_a_loai(r));
    }

    static final String TAI_KHOAN = """
            SELECT a.maTK AS a_maTK,
                   a.tenTK AS a_tenTK,
                   a.vaiTro AS a_vaiTro,
                   a.trangThai AS a_trangThai,
                   a_nv.maNV AS a_nv_maNV,
                   a_nv.hoTenNV AS a_nv_hoTenNV,
                   a_nv.gioiTinh AS a_nv_gioiTinh,
                   a_nv.sdt AS a_nv_sdt,
                   a_nv.email AS a_nv_email,
                   a_nv.chuyenMon AS a_nv_chuyenMon
            FROM TaiKhoan a
            LEFT JOIN NhanVien a_nv ON a_nv.maNV=a.maNV
            """;

    static TaiKhoan taiKhoan(ResultSet r) throws SQLException {
        return map_TaiKhoan_a(r);
    }

    private static NhanVien map_TaiKhoan_a_nv(ResultSet r) throws SQLException {
        if (r.getObject("a_nv_maNV") == null) { return null; }
        return new NhanVien(
                r.getString("a_nv_maNV"),
                r.getString("a_nv_hoTenNV"),
                r.getBoolean("a_nv_gioiTinh"),
                r.getString("a_nv_sdt"),
                r.getString("a_nv_email"),
                r.getString("a_nv_chuyenMon"));
    }

    private static TaiKhoan map_TaiKhoan_a(ResultSet r) throws SQLException {
        if (r.getObject("a_maTK") == null) { return null; }
        return new TaiKhoan(
                r.getString("a_maTK"),
                r.getString("a_tenTK"),
                "[Ẩn]",
                VaiTro.valueOf(r.getString("a_vaiTro")),
                r.getBoolean("a_trangThai"),
                map_TaiKhoan_a_nv(r));
    }

    static final String HO_SO = """
            SELECT a.maHSBA AS a_maHSBA,
                   a.tienSuBenh AS a_tienSuBenh,
                   a_kh.maKH AS a_kh_maKH,
                   a_kh.tenKH AS a_kh_tenKH,
                   a_kh.ngaySinh AS a_kh_ngaySinh,
                   a_kh.gioiTinh AS a_kh_gioiTinh,
                   a_kh.sdt AS a_kh_sdt,
                   a_kh.diaChi AS a_kh_diaChi,
                   a_kh.email AS a_kh_email
            FROM HoSoBenhAn a
            LEFT JOIN KhachHang a_kh ON a_kh.maKH=a.maKH
            """;

    static HoSoBenhAn hoSoBenhAn(ResultSet r) throws SQLException {
        return map_HoSoBenhAn_a(r);
    }

    private static KhachHang map_HoSoBenhAn_a_kh(ResultSet r) throws SQLException {
        if (r.getObject("a_kh_maKH") == null) { return null; }
        return new KhachHang(
                r.getString("a_kh_maKH"),
                r.getString("a_kh_tenKH"),
                r.getObject("a_kh_ngaySinh", LocalDate.class),
                r.getBoolean("a_kh_gioiTinh"),
                r.getString("a_kh_sdt"),
                r.getString("a_kh_diaChi"),
                r.getString("a_kh_email"));
    }

    private static HoSoBenhAn map_HoSoBenhAn_a(ResultSet r) throws SQLException {
        if (r.getObject("a_maHSBA") == null) { return null; }
        return new HoSoBenhAn(
                r.getString("a_maHSBA"),
                r.getString("a_tienSuBenh"),
                map_HoSoBenhAn_a_kh(r));
    }

    static final String LICH_LAM_VIEC = """
            SELECT a.maLLV AS a_maLLV,
                   a.ngayTruc AS a_ngayTruc,
                   a.caTruc AS a_caTruc,
                   a.gioBatDau AS a_gioBatDau,
                   a.gioKetThuc AS a_gioKetThuc,
                   a.trangThai AS a_trangThai,
                   a.ghiChuNghi AS a_ghiChuNghi,
                   a_nv.maNV AS a_nv_maNV,
                   a_nv.hoTenNV AS a_nv_hoTenNV,
                   a_nv.gioiTinh AS a_nv_gioiTinh,
                   a_nv.sdt AS a_nv_sdt,
                   a_nv.email AS a_nv_email,
                   a_nv.chuyenMon AS a_nv_chuyenMon
            FROM LichLamViec a
            LEFT JOIN NhanVien a_nv ON a_nv.maNV=a.maNV
            """;

    static LichLamViec lichLamViec(ResultSet r) throws SQLException {
        return map_LichLamViec_a(r);
    }

    private static NhanVien map_LichLamViec_a_nv(ResultSet r) throws SQLException {
        if (r.getObject("a_nv_maNV") == null) { return null; }
        return new NhanVien(
                r.getString("a_nv_maNV"),
                r.getString("a_nv_hoTenNV"),
                r.getBoolean("a_nv_gioiTinh"),
                r.getString("a_nv_sdt"),
                r.getString("a_nv_email"),
                r.getString("a_nv_chuyenMon"));
    }

    private static LichLamViec map_LichLamViec_a(ResultSet r) throws SQLException {
        if (r.getObject("a_maLLV") == null) { return null; }
        return new LichLamViec(
                r.getString("a_maLLV"),
                r.getObject("a_ngayTruc", LocalDate.class),
                CaTruc.valueOf(r.getString("a_caTruc")),
                r.getObject("a_gioBatDau", LocalTime.class),
                r.getObject("a_gioKetThuc", LocalTime.class),
                TrangThaiLichLamViec.valueOf(r.getString("a_trangThai")),
                r.getString("a_ghiChuNghi"),
                map_LichLamViec_a_nv(r));
    }

    static final String LICH_HEN = """
            SELECT a.maLH AS a_maLH,
                   a.ngayGioHen AS a_ngayGioHen,
                   a.trangThai AS a_trangThai,
                   a.ghiChu AS a_ghiChu,
                   a.lyDoHuy AS a_lyDoHuy,
                   a_kh.maKH AS a_kh_maKH,
                   a_kh.tenKH AS a_kh_tenKH,
                   a_kh.ngaySinh AS a_kh_ngaySinh,
                   a_kh.gioiTinh AS a_kh_gioiTinh,
                   a_kh.sdt AS a_kh_sdt,
                   a_kh.diaChi AS a_kh_diaChi,
                   a_kh.email AS a_kh_email,
                   a_ns.maNV AS a_ns_maNV,
                   a_ns.hoTenNV AS a_ns_hoTenNV,
                   a_ns.gioiTinh AS a_ns_gioiTinh,
                   a_ns.sdt AS a_ns_sdt,
                   a_ns.email AS a_ns_email,
                   a_ns.chuyenMon AS a_ns_chuyenMon,
                   a_dv.maDV AS a_dv_maDV,
                   a_dv.tenDV AS a_dv_tenDV,
                   a_dv.donGia AS a_dv_donGia,
                   a_dv.thoiLuong AS a_dv_thoiLuong,
                   a_dv.loTrinhDichVu AS a_dv_loTrinhDichVu,
                   a_dv.trangThai AS a_dv_trangThai,
                   a_dv_loai.maLoaiDV AS a_dv_loai_maLoaiDV,
                   a_dv_loai.tenLoaiDV AS a_dv_loai_tenLoaiDV,
                   a_dv_loai.mucThueGTGT AS a_dv_loai_mucThueGTGT,
                   a_nguon.maPK AS a_nguon_maPK,
                   a_nguon.ngayLap AS a_nguon_ngayLap,
                   a_nguon.trangThai AS a_nguon_trangThai,
                   a_nguon.loiDan AS a_nguon_loiDan,
                   a_nguon_hs.maHSBA AS a_nguon_hs_maHSBA,
                   a_nguon_hs.tienSuBenh AS a_nguon_hs_tienSuBenh,
                   a_nguon_hs_kh.maKH AS a_nguon_hs_kh_maKH,
                   a_nguon_hs_kh.tenKH AS a_nguon_hs_kh_tenKH,
                   a_nguon_hs_kh.ngaySinh AS a_nguon_hs_kh_ngaySinh,
                   a_nguon_hs_kh.gioiTinh AS a_nguon_hs_kh_gioiTinh,
                   a_nguon_hs_kh.sdt AS a_nguon_hs_kh_sdt,
                   a_nguon_hs_kh.diaChi AS a_nguon_hs_kh_diaChi,
                   a_nguon_hs_kh.email AS a_nguon_hs_kh_email,
                   a_nguon_lt.maNV AS a_nguon_lt_maNV,
                   a_nguon_lt.hoTenNV AS a_nguon_lt_hoTenNV,
                   a_nguon_lt.gioiTinh AS a_nguon_lt_gioiTinh,
                   a_nguon_lt.sdt AS a_nguon_lt_sdt,
                   a_nguon_lt.email AS a_nguon_lt_email,
                   a_nguon_lt.chuyenMon AS a_nguon_lt_chuyenMon
            FROM LichHen a
            LEFT JOIN KhachHang a_kh ON a_kh.maKH=a.maKH
            LEFT JOIN NhanVien a_ns ON a_ns.maNV=a.maNV
            LEFT JOIN DichVu a_dv ON a_dv.maDV=a.maDV
            LEFT JOIN LoaiDichVu a_dv_loai ON a_dv_loai.maLoaiDV=a_dv.maLoaiDV
            LEFT JOIN PhieuKham a_nguon ON a_nguon.maPK=a.maPKNguon
            LEFT JOIN HoSoBenhAn a_nguon_hs ON a_nguon_hs.maHSBA=a_nguon.maHSBA
            LEFT JOIN KhachHang a_nguon_hs_kh ON a_nguon_hs_kh.maKH=a_nguon_hs.maKH
            LEFT JOIN NhanVien a_nguon_lt ON a_nguon_lt.maNV=a_nguon.maNV_LapPhieu
            """;

    static LichHen lichHen(ResultSet r) throws SQLException {
        return map_LichHen_a(r);
    }

    private static KhachHang map_LichHen_a_kh(ResultSet r) throws SQLException {
        if (r.getObject("a_kh_maKH") == null) { return null; }
        return new KhachHang(
                r.getString("a_kh_maKH"),
                r.getString("a_kh_tenKH"),
                r.getObject("a_kh_ngaySinh", LocalDate.class),
                r.getBoolean("a_kh_gioiTinh"),
                r.getString("a_kh_sdt"),
                r.getString("a_kh_diaChi"),
                r.getString("a_kh_email"));
    }

    private static NhanVien map_LichHen_a_ns(ResultSet r) throws SQLException {
        if (r.getObject("a_ns_maNV") == null) { return null; }
        return new NhanVien(
                r.getString("a_ns_maNV"),
                r.getString("a_ns_hoTenNV"),
                r.getBoolean("a_ns_gioiTinh"),
                r.getString("a_ns_sdt"),
                r.getString("a_ns_email"),
                r.getString("a_ns_chuyenMon"));
    }

    private static LoaiDichVu map_LichHen_a_dv_loai(ResultSet r) throws SQLException {
        if (r.getObject("a_dv_loai_maLoaiDV") == null) { return null; }
        return new LoaiDichVu(
                r.getString("a_dv_loai_maLoaiDV"),
                r.getString("a_dv_loai_tenLoaiDV"),
                thue(r.getInt("a_dv_loai_mucThueGTGT")));
    }

    private static DichVu map_LichHen_a_dv(ResultSet r) throws SQLException {
        if (r.getObject("a_dv_maDV") == null) { return null; }
        return new DichVu(
                r.getString("a_dv_maDV"),
                r.getString("a_dv_tenDV"),
                r.getDouble("a_dv_donGia"),
                r.getInt("a_dv_thoiLuong"),
                r.getString("a_dv_loTrinhDichVu"),
                r.getBoolean("a_dv_trangThai"),
                map_LichHen_a_dv_loai(r));
    }

    private static KhachHang map_LichHen_a_nguon_hs_kh(ResultSet r) throws SQLException {
        if (r.getObject("a_nguon_hs_kh_maKH") == null) { return null; }
        return new KhachHang(
                r.getString("a_nguon_hs_kh_maKH"),
                r.getString("a_nguon_hs_kh_tenKH"),
                r.getObject("a_nguon_hs_kh_ngaySinh", LocalDate.class),
                r.getBoolean("a_nguon_hs_kh_gioiTinh"),
                r.getString("a_nguon_hs_kh_sdt"),
                r.getString("a_nguon_hs_kh_diaChi"),
                r.getString("a_nguon_hs_kh_email"));
    }

    private static HoSoBenhAn map_LichHen_a_nguon_hs(ResultSet r) throws SQLException {
        if (r.getObject("a_nguon_hs_maHSBA") == null) { return null; }
        return new HoSoBenhAn(
                r.getString("a_nguon_hs_maHSBA"),
                r.getString("a_nguon_hs_tienSuBenh"),
                map_LichHen_a_nguon_hs_kh(r));
    }

    private static NhanVien map_LichHen_a_nguon_lt(ResultSet r) throws SQLException {
        if (r.getObject("a_nguon_lt_maNV") == null) { return null; }
        return new NhanVien(
                r.getString("a_nguon_lt_maNV"),
                r.getString("a_nguon_lt_hoTenNV"),
                r.getBoolean("a_nguon_lt_gioiTinh"),
                r.getString("a_nguon_lt_sdt"),
                r.getString("a_nguon_lt_email"),
                r.getString("a_nguon_lt_chuyenMon"));
    }

    private static PhieuKham map_LichHen_a_nguon(ResultSet r) throws SQLException {
        if (r.getObject("a_nguon_maPK") == null) { return null; }
        return new PhieuKham(
                r.getString("a_nguon_maPK"),
                r.getObject("a_nguon_ngayLap", LocalDateTime.class),
                TrangThaiPhieuKham.valueOf(r.getString("a_nguon_trangThai")),
                r.getString("a_nguon_loiDan"),
                map_LichHen_a_nguon_hs(r),
                null,
                map_LichHen_a_nguon_lt(r));
    }

    private static LichHen map_LichHen_a(ResultSet r) throws SQLException {
        if (r.getObject("a_maLH") == null) { return null; }
        return new LichHen(
                r.getString("a_maLH"),
                r.getObject("a_ngayGioHen", LocalDateTime.class),
                TrangThaiLichHen.valueOf(r.getString("a_trangThai")),
                r.getString("a_ghiChu"),
                r.getString("a_lyDoHuy"),
                map_LichHen_a_kh(r),
                map_LichHen_a_ns(r),
                map_LichHen_a_dv(r),
                map_LichHen_a_nguon(r));
    }

    static final String PHIEU_KHAM = """
            SELECT a.maPK AS a_maPK,
                   a.ngayLap AS a_ngayLap,
                   a.trangThai AS a_trangThai,
                   a.loiDan AS a_loiDan,
                   a_hs.maHSBA AS a_hs_maHSBA,
                   a_hs.tienSuBenh AS a_hs_tienSuBenh,
                   a_hs_kh.maKH AS a_hs_kh_maKH,
                   a_hs_kh.tenKH AS a_hs_kh_tenKH,
                   a_hs_kh.ngaySinh AS a_hs_kh_ngaySinh,
                   a_hs_kh.gioiTinh AS a_hs_kh_gioiTinh,
                   a_hs_kh.sdt AS a_hs_kh_sdt,
                   a_hs_kh.diaChi AS a_hs_kh_diaChi,
                   a_hs_kh.email AS a_hs_kh_email,
                   a_lh.maLH AS a_lh_maLH,
                   a_lh.ngayGioHen AS a_lh_ngayGioHen,
                   a_lh.trangThai AS a_lh_trangThai,
                   a_lh.ghiChu AS a_lh_ghiChu,
                   a_lh.lyDoHuy AS a_lh_lyDoHuy,
                   a_lh_kh.maKH AS a_lh_kh_maKH,
                   a_lh_kh.tenKH AS a_lh_kh_tenKH,
                   a_lh_kh.ngaySinh AS a_lh_kh_ngaySinh,
                   a_lh_kh.gioiTinh AS a_lh_kh_gioiTinh,
                   a_lh_kh.sdt AS a_lh_kh_sdt,
                   a_lh_kh.diaChi AS a_lh_kh_diaChi,
                   a_lh_kh.email AS a_lh_kh_email,
                   a_lh_ns.maNV AS a_lh_ns_maNV,
                   a_lh_ns.hoTenNV AS a_lh_ns_hoTenNV,
                   a_lh_ns.gioiTinh AS a_lh_ns_gioiTinh,
                   a_lh_ns.sdt AS a_lh_ns_sdt,
                   a_lh_ns.email AS a_lh_ns_email,
                   a_lh_ns.chuyenMon AS a_lh_ns_chuyenMon,
                   a_lh_dv.maDV AS a_lh_dv_maDV,
                   a_lh_dv.tenDV AS a_lh_dv_tenDV,
                   a_lh_dv.donGia AS a_lh_dv_donGia,
                   a_lh_dv.thoiLuong AS a_lh_dv_thoiLuong,
                   a_lh_dv.loTrinhDichVu AS a_lh_dv_loTrinhDichVu,
                   a_lh_dv.trangThai AS a_lh_dv_trangThai,
                   a_lh_dv_loai.maLoaiDV AS a_lh_dv_loai_maLoaiDV,
                   a_lh_dv_loai.tenLoaiDV AS a_lh_dv_loai_tenLoaiDV,
                   a_lh_dv_loai.mucThueGTGT AS a_lh_dv_loai_mucThueGTGT,
                   a_lh_nguon.maPK AS a_lh_nguon_maPK,
                   a_lh_nguon.ngayLap AS a_lh_nguon_ngayLap,
                   a_lh_nguon.trangThai AS a_lh_nguon_trangThai,
                   a_lh_nguon.loiDan AS a_lh_nguon_loiDan,
                   a_lh_nguon_hs.maHSBA AS a_lh_nguon_hs_maHSBA,
                   a_lh_nguon_hs.tienSuBenh AS a_lh_nguon_hs_tienSuBenh,
                   a_lh_nguon_hs_kh.maKH AS a_lh_nguon_hs_kh_maKH,
                   a_lh_nguon_hs_kh.tenKH AS a_lh_nguon_hs_kh_tenKH,
                   a_lh_nguon_hs_kh.ngaySinh AS a_lh_nguon_hs_kh_ngaySinh,
                   a_lh_nguon_hs_kh.gioiTinh AS a_lh_nguon_hs_kh_gioiTinh,
                   a_lh_nguon_hs_kh.sdt AS a_lh_nguon_hs_kh_sdt,
                   a_lh_nguon_hs_kh.diaChi AS a_lh_nguon_hs_kh_diaChi,
                   a_lh_nguon_hs_kh.email AS a_lh_nguon_hs_kh_email,
                   a_lh_nguon_lt.maNV AS a_lh_nguon_lt_maNV,
                   a_lh_nguon_lt.hoTenNV AS a_lh_nguon_lt_hoTenNV,
                   a_lh_nguon_lt.gioiTinh AS a_lh_nguon_lt_gioiTinh,
                   a_lh_nguon_lt.sdt AS a_lh_nguon_lt_sdt,
                   a_lh_nguon_lt.email AS a_lh_nguon_lt_email,
                   a_lh_nguon_lt.chuyenMon AS a_lh_nguon_lt_chuyenMon,
                   a_lt.maNV AS a_lt_maNV,
                   a_lt.hoTenNV AS a_lt_hoTenNV,
                   a_lt.gioiTinh AS a_lt_gioiTinh,
                   a_lt.sdt AS a_lt_sdt,
                   a_lt.email AS a_lt_email,
                   a_lt.chuyenMon AS a_lt_chuyenMon
            FROM PhieuKham a
            LEFT JOIN HoSoBenhAn a_hs ON a_hs.maHSBA=a.maHSBA
            LEFT JOIN KhachHang a_hs_kh ON a_hs_kh.maKH=a_hs.maKH
            LEFT JOIN LichHen a_lh ON a_lh.maLH=a.maLH
            LEFT JOIN KhachHang a_lh_kh ON a_lh_kh.maKH=a_lh.maKH
            LEFT JOIN NhanVien a_lh_ns ON a_lh_ns.maNV=a_lh.maNV
            LEFT JOIN DichVu a_lh_dv ON a_lh_dv.maDV=a_lh.maDV
            LEFT JOIN LoaiDichVu a_lh_dv_loai ON a_lh_dv_loai.maLoaiDV=a_lh_dv.maLoaiDV
            LEFT JOIN PhieuKham a_lh_nguon ON a_lh_nguon.maPK=a_lh.maPKNguon
            LEFT JOIN HoSoBenhAn a_lh_nguon_hs ON a_lh_nguon_hs.maHSBA=a_lh_nguon.maHSBA
            LEFT JOIN KhachHang a_lh_nguon_hs_kh ON a_lh_nguon_hs_kh.maKH=a_lh_nguon_hs.maKH
            LEFT JOIN NhanVien a_lh_nguon_lt ON a_lh_nguon_lt.maNV=a_lh_nguon.maNV_LapPhieu
            LEFT JOIN NhanVien a_lt ON a_lt.maNV=a.maNV_LapPhieu
            """;

    static PhieuKham phieuKham(ResultSet r) throws SQLException {
        return map_PhieuKham_a(r);
    }

    private static KhachHang map_PhieuKham_a_hs_kh(ResultSet r) throws SQLException {
        if (r.getObject("a_hs_kh_maKH") == null) { return null; }
        return new KhachHang(
                r.getString("a_hs_kh_maKH"),
                r.getString("a_hs_kh_tenKH"),
                r.getObject("a_hs_kh_ngaySinh", LocalDate.class),
                r.getBoolean("a_hs_kh_gioiTinh"),
                r.getString("a_hs_kh_sdt"),
                r.getString("a_hs_kh_diaChi"),
                r.getString("a_hs_kh_email"));
    }

    private static HoSoBenhAn map_PhieuKham_a_hs(ResultSet r) throws SQLException {
        if (r.getObject("a_hs_maHSBA") == null) { return null; }
        return new HoSoBenhAn(
                r.getString("a_hs_maHSBA"),
                r.getString("a_hs_tienSuBenh"),
                map_PhieuKham_a_hs_kh(r));
    }

    private static KhachHang map_PhieuKham_a_lh_kh(ResultSet r) throws SQLException {
        if (r.getObject("a_lh_kh_maKH") == null) { return null; }
        return new KhachHang(
                r.getString("a_lh_kh_maKH"),
                r.getString("a_lh_kh_tenKH"),
                r.getObject("a_lh_kh_ngaySinh", LocalDate.class),
                r.getBoolean("a_lh_kh_gioiTinh"),
                r.getString("a_lh_kh_sdt"),
                r.getString("a_lh_kh_diaChi"),
                r.getString("a_lh_kh_email"));
    }

    private static NhanVien map_PhieuKham_a_lh_ns(ResultSet r) throws SQLException {
        if (r.getObject("a_lh_ns_maNV") == null) { return null; }
        return new NhanVien(
                r.getString("a_lh_ns_maNV"),
                r.getString("a_lh_ns_hoTenNV"),
                r.getBoolean("a_lh_ns_gioiTinh"),
                r.getString("a_lh_ns_sdt"),
                r.getString("a_lh_ns_email"),
                r.getString("a_lh_ns_chuyenMon"));
    }

    private static LoaiDichVu map_PhieuKham_a_lh_dv_loai(ResultSet r) throws SQLException {
        if (r.getObject("a_lh_dv_loai_maLoaiDV") == null) { return null; }
        return new LoaiDichVu(
                r.getString("a_lh_dv_loai_maLoaiDV"),
                r.getString("a_lh_dv_loai_tenLoaiDV"),
                thue(r.getInt("a_lh_dv_loai_mucThueGTGT")));
    }

    private static DichVu map_PhieuKham_a_lh_dv(ResultSet r) throws SQLException {
        if (r.getObject("a_lh_dv_maDV") == null) { return null; }
        return new DichVu(
                r.getString("a_lh_dv_maDV"),
                r.getString("a_lh_dv_tenDV"),
                r.getDouble("a_lh_dv_donGia"),
                r.getInt("a_lh_dv_thoiLuong"),
                r.getString("a_lh_dv_loTrinhDichVu"),
                r.getBoolean("a_lh_dv_trangThai"),
                map_PhieuKham_a_lh_dv_loai(r));
    }

    private static KhachHang map_PhieuKham_a_lh_nguon_hs_kh(ResultSet r) throws SQLException {
        if (r.getObject("a_lh_nguon_hs_kh_maKH") == null) { return null; }
        return new KhachHang(
                r.getString("a_lh_nguon_hs_kh_maKH"),
                r.getString("a_lh_nguon_hs_kh_tenKH"),
                r.getObject("a_lh_nguon_hs_kh_ngaySinh", LocalDate.class),
                r.getBoolean("a_lh_nguon_hs_kh_gioiTinh"),
                r.getString("a_lh_nguon_hs_kh_sdt"),
                r.getString("a_lh_nguon_hs_kh_diaChi"),
                r.getString("a_lh_nguon_hs_kh_email"));
    }

    private static HoSoBenhAn map_PhieuKham_a_lh_nguon_hs(ResultSet r) throws SQLException {
        if (r.getObject("a_lh_nguon_hs_maHSBA") == null) { return null; }
        return new HoSoBenhAn(
                r.getString("a_lh_nguon_hs_maHSBA"),
                r.getString("a_lh_nguon_hs_tienSuBenh"),
                map_PhieuKham_a_lh_nguon_hs_kh(r));
    }

    private static NhanVien map_PhieuKham_a_lh_nguon_lt(ResultSet r) throws SQLException {
        if (r.getObject("a_lh_nguon_lt_maNV") == null) { return null; }
        return new NhanVien(
                r.getString("a_lh_nguon_lt_maNV"),
                r.getString("a_lh_nguon_lt_hoTenNV"),
                r.getBoolean("a_lh_nguon_lt_gioiTinh"),
                r.getString("a_lh_nguon_lt_sdt"),
                r.getString("a_lh_nguon_lt_email"),
                r.getString("a_lh_nguon_lt_chuyenMon"));
    }

    private static PhieuKham map_PhieuKham_a_lh_nguon(ResultSet r) throws SQLException {
        if (r.getObject("a_lh_nguon_maPK") == null) { return null; }
        return new PhieuKham(
                r.getString("a_lh_nguon_maPK"),
                r.getObject("a_lh_nguon_ngayLap", LocalDateTime.class),
                TrangThaiPhieuKham.valueOf(r.getString("a_lh_nguon_trangThai")),
                r.getString("a_lh_nguon_loiDan"),
                map_PhieuKham_a_lh_nguon_hs(r),
                null,
                map_PhieuKham_a_lh_nguon_lt(r));
    }

    private static LichHen map_PhieuKham_a_lh(ResultSet r) throws SQLException {
        if (r.getObject("a_lh_maLH") == null) { return null; }
        return new LichHen(
                r.getString("a_lh_maLH"),
                r.getObject("a_lh_ngayGioHen", LocalDateTime.class),
                TrangThaiLichHen.valueOf(r.getString("a_lh_trangThai")),
                r.getString("a_lh_ghiChu"),
                r.getString("a_lh_lyDoHuy"),
                map_PhieuKham_a_lh_kh(r),
                map_PhieuKham_a_lh_ns(r),
                map_PhieuKham_a_lh_dv(r),
                map_PhieuKham_a_lh_nguon(r));
    }

    private static NhanVien map_PhieuKham_a_lt(ResultSet r) throws SQLException {
        if (r.getObject("a_lt_maNV") == null) { return null; }
        return new NhanVien(
                r.getString("a_lt_maNV"),
                r.getString("a_lt_hoTenNV"),
                r.getBoolean("a_lt_gioiTinh"),
                r.getString("a_lt_sdt"),
                r.getString("a_lt_email"),
                r.getString("a_lt_chuyenMon"));
    }

    private static PhieuKham map_PhieuKham_a(ResultSet r) throws SQLException {
        if (r.getObject("a_maPK") == null) { return null; }
        return new PhieuKham(
                r.getString("a_maPK"),
                r.getObject("a_ngayLap", LocalDateTime.class),
                TrangThaiPhieuKham.valueOf(r.getString("a_trangThai")),
                r.getString("a_loiDan"),
                map_PhieuKham_a_hs(r),
                map_PhieuKham_a_lh(r),
                map_PhieuKham_a_lt(r));
    }

    static final String CHI_TIET = """
            SELECT a.maCTPK AS a_maCTPK,
                   a.trieuChung AS a_trieuChung,
                   a.chanDoan AS a_chanDoan,
                   a.donGia AS a_donGia,
                   a.soLuong AS a_soLuong,
                   a.donViTinh AS a_donViTinh,
                   a.mucThueGTGT AS a_mucThueGTGT,
                   a.ketQuaDieuTri AS a_ketQuaDieuTri,
                   a_pk.maPK AS a_pk_maPK,
                   a_pk.ngayLap AS a_pk_ngayLap,
                   a_pk.trangThai AS a_pk_trangThai,
                   a_pk.loiDan AS a_pk_loiDan,
                   a_pk_hs.maHSBA AS a_pk_hs_maHSBA,
                   a_pk_hs.tienSuBenh AS a_pk_hs_tienSuBenh,
                   a_pk_hs_kh.maKH AS a_pk_hs_kh_maKH,
                   a_pk_hs_kh.tenKH AS a_pk_hs_kh_tenKH,
                   a_pk_hs_kh.ngaySinh AS a_pk_hs_kh_ngaySinh,
                   a_pk_hs_kh.gioiTinh AS a_pk_hs_kh_gioiTinh,
                   a_pk_hs_kh.sdt AS a_pk_hs_kh_sdt,
                   a_pk_hs_kh.diaChi AS a_pk_hs_kh_diaChi,
                   a_pk_hs_kh.email AS a_pk_hs_kh_email,
                   a_pk_lh.maLH AS a_pk_lh_maLH,
                   a_pk_lh.ngayGioHen AS a_pk_lh_ngayGioHen,
                   a_pk_lh.trangThai AS a_pk_lh_trangThai,
                   a_pk_lh.ghiChu AS a_pk_lh_ghiChu,
                   a_pk_lh.lyDoHuy AS a_pk_lh_lyDoHuy,
                   a_pk_lh_kh.maKH AS a_pk_lh_kh_maKH,
                   a_pk_lh_kh.tenKH AS a_pk_lh_kh_tenKH,
                   a_pk_lh_kh.ngaySinh AS a_pk_lh_kh_ngaySinh,
                   a_pk_lh_kh.gioiTinh AS a_pk_lh_kh_gioiTinh,
                   a_pk_lh_kh.sdt AS a_pk_lh_kh_sdt,
                   a_pk_lh_kh.diaChi AS a_pk_lh_kh_diaChi,
                   a_pk_lh_kh.email AS a_pk_lh_kh_email,
                   a_pk_lh_ns.maNV AS a_pk_lh_ns_maNV,
                   a_pk_lh_ns.hoTenNV AS a_pk_lh_ns_hoTenNV,
                   a_pk_lh_ns.gioiTinh AS a_pk_lh_ns_gioiTinh,
                   a_pk_lh_ns.sdt AS a_pk_lh_ns_sdt,
                   a_pk_lh_ns.email AS a_pk_lh_ns_email,
                   a_pk_lh_ns.chuyenMon AS a_pk_lh_ns_chuyenMon,
                   a_pk_lh_dv.maDV AS a_pk_lh_dv_maDV,
                   a_pk_lh_dv.tenDV AS a_pk_lh_dv_tenDV,
                   a_pk_lh_dv.donGia AS a_pk_lh_dv_donGia,
                   a_pk_lh_dv.thoiLuong AS a_pk_lh_dv_thoiLuong,
                   a_pk_lh_dv.loTrinhDichVu AS a_pk_lh_dv_loTrinhDichVu,
                   a_pk_lh_dv.trangThai AS a_pk_lh_dv_trangThai,
                   a_pk_lh_dv_loai.maLoaiDV AS a_pk_lh_dv_loai_maLoaiDV,
                   a_pk_lh_dv_loai.tenLoaiDV AS a_pk_lh_dv_loai_tenLoaiDV,
                   a_pk_lh_dv_loai.mucThueGTGT AS a_pk_lh_dv_loai_mucThueGTGT,
                   a_pk_lh_nguon.maPK AS a_pk_lh_nguon_maPK,
                   a_pk_lh_nguon.ngayLap AS a_pk_lh_nguon_ngayLap,
                   a_pk_lh_nguon.trangThai AS a_pk_lh_nguon_trangThai,
                   a_pk_lh_nguon.loiDan AS a_pk_lh_nguon_loiDan,
                   a_pk_lh_nguon_hs.maHSBA AS a_pk_lh_nguon_hs_maHSBA,
                   a_pk_lh_nguon_hs.tienSuBenh AS a_pk_lh_nguon_hs_tienSuBenh,
                   a_pk_lh_nguon_hs_kh.maKH AS a_pk_lh_nguon_hs_kh_maKH,
                   a_pk_lh_nguon_hs_kh.tenKH AS a_pk_lh_nguon_hs_kh_tenKH,
                   a_pk_lh_nguon_hs_kh.ngaySinh AS a_pk_lh_nguon_hs_kh_ngaySinh,
                   a_pk_lh_nguon_hs_kh.gioiTinh AS a_pk_lh_nguon_hs_kh_gioiTinh,
                   a_pk_lh_nguon_hs_kh.sdt AS a_pk_lh_nguon_hs_kh_sdt,
                   a_pk_lh_nguon_hs_kh.diaChi AS a_pk_lh_nguon_hs_kh_diaChi,
                   a_pk_lh_nguon_hs_kh.email AS a_pk_lh_nguon_hs_kh_email,
                   a_pk_lh_nguon_lt.maNV AS a_pk_lh_nguon_lt_maNV,
                   a_pk_lh_nguon_lt.hoTenNV AS a_pk_lh_nguon_lt_hoTenNV,
                   a_pk_lh_nguon_lt.gioiTinh AS a_pk_lh_nguon_lt_gioiTinh,
                   a_pk_lh_nguon_lt.sdt AS a_pk_lh_nguon_lt_sdt,
                   a_pk_lh_nguon_lt.email AS a_pk_lh_nguon_lt_email,
                   a_pk_lh_nguon_lt.chuyenMon AS a_pk_lh_nguon_lt_chuyenMon,
                   a_pk_lt.maNV AS a_pk_lt_maNV,
                   a_pk_lt.hoTenNV AS a_pk_lt_hoTenNV,
                   a_pk_lt.gioiTinh AS a_pk_lt_gioiTinh,
                   a_pk_lt.sdt AS a_pk_lt_sdt,
                   a_pk_lt.email AS a_pk_lt_email,
                   a_pk_lt.chuyenMon AS a_pk_lt_chuyenMon,
                   a_dv.maDV AS a_dv_maDV,
                   a_dv.tenDV AS a_dv_tenDV,
                   a_dv.donGia AS a_dv_donGia,
                   a_dv.thoiLuong AS a_dv_thoiLuong,
                   a_dv.loTrinhDichVu AS a_dv_loTrinhDichVu,
                   a_dv.trangThai AS a_dv_trangThai,
                   a_dv_loai.maLoaiDV AS a_dv_loai_maLoaiDV,
                   a_dv_loai.tenLoaiDV AS a_dv_loai_tenLoaiDV,
                   a_dv_loai.mucThueGTGT AS a_dv_loai_mucThueGTGT,
                   a_ns.maNV AS a_ns_maNV,
                   a_ns.hoTenNV AS a_ns_hoTenNV,
                   a_ns.gioiTinh AS a_ns_gioiTinh,
                   a_ns.sdt AS a_ns_sdt,
                   a_ns.email AS a_ns_email,
                   a_ns.chuyenMon AS a_ns_chuyenMon
            FROM ChiTietPhieuKham a
            LEFT JOIN PhieuKham a_pk ON a_pk.maPK=a.maPK
            LEFT JOIN HoSoBenhAn a_pk_hs ON a_pk_hs.maHSBA=a_pk.maHSBA
            LEFT JOIN KhachHang a_pk_hs_kh ON a_pk_hs_kh.maKH=a_pk_hs.maKH
            LEFT JOIN LichHen a_pk_lh ON a_pk_lh.maLH=a_pk.maLH
            LEFT JOIN KhachHang a_pk_lh_kh ON a_pk_lh_kh.maKH=a_pk_lh.maKH
            LEFT JOIN NhanVien a_pk_lh_ns ON a_pk_lh_ns.maNV=a_pk_lh.maNV
            LEFT JOIN DichVu a_pk_lh_dv ON a_pk_lh_dv.maDV=a_pk_lh.maDV
            LEFT JOIN LoaiDichVu a_pk_lh_dv_loai ON a_pk_lh_dv_loai.maLoaiDV=a_pk_lh_dv.maLoaiDV
            LEFT JOIN PhieuKham a_pk_lh_nguon ON a_pk_lh_nguon.maPK=a_pk_lh.maPKNguon
            LEFT JOIN HoSoBenhAn a_pk_lh_nguon_hs ON a_pk_lh_nguon_hs.maHSBA=a_pk_lh_nguon.maHSBA
            LEFT JOIN KhachHang a_pk_lh_nguon_hs_kh ON a_pk_lh_nguon_hs_kh.maKH=a_pk_lh_nguon_hs.maKH
            LEFT JOIN NhanVien a_pk_lh_nguon_lt ON a_pk_lh_nguon_lt.maNV=a_pk_lh_nguon.maNV_LapPhieu
            LEFT JOIN NhanVien a_pk_lt ON a_pk_lt.maNV=a_pk.maNV_LapPhieu
            LEFT JOIN DichVu a_dv ON a_dv.maDV=a.maDV
            LEFT JOIN LoaiDichVu a_dv_loai ON a_dv_loai.maLoaiDV=a_dv.maLoaiDV
            LEFT JOIN NhanVien a_ns ON a_ns.maNV=a.maNV_NhaSi
            """;

    static ChiTietPhieuKham chiTietPhieuKham(ResultSet r) throws SQLException {
        return map_ChiTietPhieuKham_a(r);
    }

    private static KhachHang map_ChiTietPhieuKham_a_pk_hs_kh(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_hs_kh_maKH") == null) { return null; }
        return new KhachHang(
                r.getString("a_pk_hs_kh_maKH"),
                r.getString("a_pk_hs_kh_tenKH"),
                r.getObject("a_pk_hs_kh_ngaySinh", LocalDate.class),
                r.getBoolean("a_pk_hs_kh_gioiTinh"),
                r.getString("a_pk_hs_kh_sdt"),
                r.getString("a_pk_hs_kh_diaChi"),
                r.getString("a_pk_hs_kh_email"));
    }

    private static HoSoBenhAn map_ChiTietPhieuKham_a_pk_hs(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_hs_maHSBA") == null) { return null; }
        return new HoSoBenhAn(
                r.getString("a_pk_hs_maHSBA"),
                r.getString("a_pk_hs_tienSuBenh"),
                map_ChiTietPhieuKham_a_pk_hs_kh(r));
    }

    private static KhachHang map_ChiTietPhieuKham_a_pk_lh_kh(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_lh_kh_maKH") == null) { return null; }
        return new KhachHang(
                r.getString("a_pk_lh_kh_maKH"),
                r.getString("a_pk_lh_kh_tenKH"),
                r.getObject("a_pk_lh_kh_ngaySinh", LocalDate.class),
                r.getBoolean("a_pk_lh_kh_gioiTinh"),
                r.getString("a_pk_lh_kh_sdt"),
                r.getString("a_pk_lh_kh_diaChi"),
                r.getString("a_pk_lh_kh_email"));
    }

    private static NhanVien map_ChiTietPhieuKham_a_pk_lh_ns(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_lh_ns_maNV") == null) { return null; }
        return new NhanVien(
                r.getString("a_pk_lh_ns_maNV"),
                r.getString("a_pk_lh_ns_hoTenNV"),
                r.getBoolean("a_pk_lh_ns_gioiTinh"),
                r.getString("a_pk_lh_ns_sdt"),
                r.getString("a_pk_lh_ns_email"),
                r.getString("a_pk_lh_ns_chuyenMon"));
    }

    private static LoaiDichVu map_ChiTietPhieuKham_a_pk_lh_dv_loai(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_lh_dv_loai_maLoaiDV") == null) { return null; }
        return new LoaiDichVu(
                r.getString("a_pk_lh_dv_loai_maLoaiDV"),
                r.getString("a_pk_lh_dv_loai_tenLoaiDV"),
                thue(r.getInt("a_pk_lh_dv_loai_mucThueGTGT")));
    }

    private static DichVu map_ChiTietPhieuKham_a_pk_lh_dv(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_lh_dv_maDV") == null) { return null; }
        return new DichVu(
                r.getString("a_pk_lh_dv_maDV"),
                r.getString("a_pk_lh_dv_tenDV"),
                r.getDouble("a_pk_lh_dv_donGia"),
                r.getInt("a_pk_lh_dv_thoiLuong"),
                r.getString("a_pk_lh_dv_loTrinhDichVu"),
                r.getBoolean("a_pk_lh_dv_trangThai"),
                map_ChiTietPhieuKham_a_pk_lh_dv_loai(r));
    }

    private static KhachHang map_ChiTietPhieuKham_a_pk_lh_nguon_hs_kh(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_lh_nguon_hs_kh_maKH") == null) { return null; }
        return new KhachHang(
                r.getString("a_pk_lh_nguon_hs_kh_maKH"),
                r.getString("a_pk_lh_nguon_hs_kh_tenKH"),
                r.getObject("a_pk_lh_nguon_hs_kh_ngaySinh", LocalDate.class),
                r.getBoolean("a_pk_lh_nguon_hs_kh_gioiTinh"),
                r.getString("a_pk_lh_nguon_hs_kh_sdt"),
                r.getString("a_pk_lh_nguon_hs_kh_diaChi"),
                r.getString("a_pk_lh_nguon_hs_kh_email"));
    }

    private static HoSoBenhAn map_ChiTietPhieuKham_a_pk_lh_nguon_hs(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_lh_nguon_hs_maHSBA") == null) { return null; }
        return new HoSoBenhAn(
                r.getString("a_pk_lh_nguon_hs_maHSBA"),
                r.getString("a_pk_lh_nguon_hs_tienSuBenh"),
                map_ChiTietPhieuKham_a_pk_lh_nguon_hs_kh(r));
    }

    private static NhanVien map_ChiTietPhieuKham_a_pk_lh_nguon_lt(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_lh_nguon_lt_maNV") == null) { return null; }
        return new NhanVien(
                r.getString("a_pk_lh_nguon_lt_maNV"),
                r.getString("a_pk_lh_nguon_lt_hoTenNV"),
                r.getBoolean("a_pk_lh_nguon_lt_gioiTinh"),
                r.getString("a_pk_lh_nguon_lt_sdt"),
                r.getString("a_pk_lh_nguon_lt_email"),
                r.getString("a_pk_lh_nguon_lt_chuyenMon"));
    }

    private static PhieuKham map_ChiTietPhieuKham_a_pk_lh_nguon(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_lh_nguon_maPK") == null) { return null; }
        return new PhieuKham(
                r.getString("a_pk_lh_nguon_maPK"),
                r.getObject("a_pk_lh_nguon_ngayLap", LocalDateTime.class),
                TrangThaiPhieuKham.valueOf(r.getString("a_pk_lh_nguon_trangThai")),
                r.getString("a_pk_lh_nguon_loiDan"),
                map_ChiTietPhieuKham_a_pk_lh_nguon_hs(r),
                null,
                map_ChiTietPhieuKham_a_pk_lh_nguon_lt(r));
    }

    private static LichHen map_ChiTietPhieuKham_a_pk_lh(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_lh_maLH") == null) { return null; }
        return new LichHen(
                r.getString("a_pk_lh_maLH"),
                r.getObject("a_pk_lh_ngayGioHen", LocalDateTime.class),
                TrangThaiLichHen.valueOf(r.getString("a_pk_lh_trangThai")),
                r.getString("a_pk_lh_ghiChu"),
                r.getString("a_pk_lh_lyDoHuy"),
                map_ChiTietPhieuKham_a_pk_lh_kh(r),
                map_ChiTietPhieuKham_a_pk_lh_ns(r),
                map_ChiTietPhieuKham_a_pk_lh_dv(r),
                map_ChiTietPhieuKham_a_pk_lh_nguon(r));
    }

    private static NhanVien map_ChiTietPhieuKham_a_pk_lt(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_lt_maNV") == null) { return null; }
        return new NhanVien(
                r.getString("a_pk_lt_maNV"),
                r.getString("a_pk_lt_hoTenNV"),
                r.getBoolean("a_pk_lt_gioiTinh"),
                r.getString("a_pk_lt_sdt"),
                r.getString("a_pk_lt_email"),
                r.getString("a_pk_lt_chuyenMon"));
    }

    private static PhieuKham map_ChiTietPhieuKham_a_pk(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_maPK") == null) { return null; }
        return new PhieuKham(
                r.getString("a_pk_maPK"),
                r.getObject("a_pk_ngayLap", LocalDateTime.class),
                TrangThaiPhieuKham.valueOf(r.getString("a_pk_trangThai")),
                r.getString("a_pk_loiDan"),
                map_ChiTietPhieuKham_a_pk_hs(r),
                map_ChiTietPhieuKham_a_pk_lh(r),
                map_ChiTietPhieuKham_a_pk_lt(r));
    }

    private static LoaiDichVu map_ChiTietPhieuKham_a_dv_loai(ResultSet r) throws SQLException {
        if (r.getObject("a_dv_loai_maLoaiDV") == null) { return null; }
        return new LoaiDichVu(
                r.getString("a_dv_loai_maLoaiDV"),
                r.getString("a_dv_loai_tenLoaiDV"),
                thue(r.getInt("a_dv_loai_mucThueGTGT")));
    }

    private static DichVu map_ChiTietPhieuKham_a_dv(ResultSet r) throws SQLException {
        if (r.getObject("a_dv_maDV") == null) { return null; }
        return new DichVu(
                r.getString("a_dv_maDV"),
                r.getString("a_dv_tenDV"),
                r.getDouble("a_dv_donGia"),
                r.getInt("a_dv_thoiLuong"),
                r.getString("a_dv_loTrinhDichVu"),
                r.getBoolean("a_dv_trangThai"),
                map_ChiTietPhieuKham_a_dv_loai(r));
    }

    private static NhanVien map_ChiTietPhieuKham_a_ns(ResultSet r) throws SQLException {
        if (r.getObject("a_ns_maNV") == null) { return null; }
        return new NhanVien(
                r.getString("a_ns_maNV"),
                r.getString("a_ns_hoTenNV"),
                r.getBoolean("a_ns_gioiTinh"),
                r.getString("a_ns_sdt"),
                r.getString("a_ns_email"),
                r.getString("a_ns_chuyenMon"));
    }

    private static ChiTietPhieuKham map_ChiTietPhieuKham_a(ResultSet r) throws SQLException {
        if (r.getObject("a_maCTPK") == null) { return null; }
        return new ChiTietPhieuKham(
                r.getInt("a_maCTPK"),
                r.getString("a_trieuChung"),
                r.getString("a_chanDoan"),
                r.getDouble("a_donGia"),
                r.getInt("a_soLuong"),
                r.getString("a_donViTinh"),
                r.getDouble("a_mucThueGTGT"),
                r.getString("a_ketQuaDieuTri"),
                map_ChiTietPhieuKham_a_pk(r),
                map_ChiTietPhieuKham_a_dv(r),
                map_ChiTietPhieuKham_a_ns(r));
    }

    static final String VOUCHER = """
            SELECT a.maVoucher AS a_maVoucher,
                   a.tenVoucher AS a_tenVoucher,
                   a.loaiGiam AS a_loaiGiam,
                   a.giaGiam AS a_giaGiam,
                   a.giaTriGiamToiDa AS a_giaTriGiamToiDa,
                   a.ngayBatDau AS a_ngayBatDau,
                   a.ngayKetThuc AS a_ngayKetThuc,
                   a.trangThai AS a_trangThai
            FROM Voucher a
            """;

    static Voucher voucher(ResultSet r) throws SQLException {
        return map_Voucher_a(r);
    }

    private static Voucher map_Voucher_a(ResultSet r) throws SQLException {
        if (r.getObject("a_maVoucher") == null) { return null; }
        return new Voucher(
                r.getString("a_maVoucher"),
                r.getString("a_tenVoucher"),
                r.getBoolean("a_loaiGiam"),
                r.getDouble("a_giaGiam"),
                r.getDouble("a_giaTriGiamToiDa"),
                r.getObject("a_ngayBatDau", LocalDate.class),
                r.getObject("a_ngayKetThuc", LocalDate.class),
                r.getBoolean("a_trangThai"));
    }

    static final String HOA_DON = """
            SELECT a.maHD AS a_maHD,
                   a.ngayLap AS a_ngayLap,
                   a.trangThai AS a_trangThai,
                   a.hinhThucThanhToan AS a_hinhThucThanhToan,
                   a.ngayThanhToan AS a_ngayThanhToan,
                   a.tongKhuyenMai AS a_tongKhuyenMai,
                   a_pk.maPK AS a_pk_maPK,
                   a_pk.ngayLap AS a_pk_ngayLap,
                   a_pk.trangThai AS a_pk_trangThai,
                   a_pk.loiDan AS a_pk_loiDan,
                   a_pk_hs.maHSBA AS a_pk_hs_maHSBA,
                   a_pk_hs.tienSuBenh AS a_pk_hs_tienSuBenh,
                   a_pk_hs_kh.maKH AS a_pk_hs_kh_maKH,
                   a_pk_hs_kh.tenKH AS a_pk_hs_kh_tenKH,
                   a_pk_hs_kh.ngaySinh AS a_pk_hs_kh_ngaySinh,
                   a_pk_hs_kh.gioiTinh AS a_pk_hs_kh_gioiTinh,
                   a_pk_hs_kh.sdt AS a_pk_hs_kh_sdt,
                   a_pk_hs_kh.diaChi AS a_pk_hs_kh_diaChi,
                   a_pk_hs_kh.email AS a_pk_hs_kh_email,
                   a_pk_lh.maLH AS a_pk_lh_maLH,
                   a_pk_lh.ngayGioHen AS a_pk_lh_ngayGioHen,
                   a_pk_lh.trangThai AS a_pk_lh_trangThai,
                   a_pk_lh.ghiChu AS a_pk_lh_ghiChu,
                   a_pk_lh.lyDoHuy AS a_pk_lh_lyDoHuy,
                   a_pk_lh_kh.maKH AS a_pk_lh_kh_maKH,
                   a_pk_lh_kh.tenKH AS a_pk_lh_kh_tenKH,
                   a_pk_lh_kh.ngaySinh AS a_pk_lh_kh_ngaySinh,
                   a_pk_lh_kh.gioiTinh AS a_pk_lh_kh_gioiTinh,
                   a_pk_lh_kh.sdt AS a_pk_lh_kh_sdt,
                   a_pk_lh_kh.diaChi AS a_pk_lh_kh_diaChi,
                   a_pk_lh_kh.email AS a_pk_lh_kh_email,
                   a_pk_lh_ns.maNV AS a_pk_lh_ns_maNV,
                   a_pk_lh_ns.hoTenNV AS a_pk_lh_ns_hoTenNV,
                   a_pk_lh_ns.gioiTinh AS a_pk_lh_ns_gioiTinh,
                   a_pk_lh_ns.sdt AS a_pk_lh_ns_sdt,
                   a_pk_lh_ns.email AS a_pk_lh_ns_email,
                   a_pk_lh_ns.chuyenMon AS a_pk_lh_ns_chuyenMon,
                   a_pk_lh_dv.maDV AS a_pk_lh_dv_maDV,
                   a_pk_lh_dv.tenDV AS a_pk_lh_dv_tenDV,
                   a_pk_lh_dv.donGia AS a_pk_lh_dv_donGia,
                   a_pk_lh_dv.thoiLuong AS a_pk_lh_dv_thoiLuong,
                   a_pk_lh_dv.loTrinhDichVu AS a_pk_lh_dv_loTrinhDichVu,
                   a_pk_lh_dv.trangThai AS a_pk_lh_dv_trangThai,
                   a_pk_lh_dv_loai.maLoaiDV AS a_pk_lh_dv_loai_maLoaiDV,
                   a_pk_lh_dv_loai.tenLoaiDV AS a_pk_lh_dv_loai_tenLoaiDV,
                   a_pk_lh_dv_loai.mucThueGTGT AS a_pk_lh_dv_loai_mucThueGTGT,
                   a_pk_lh_nguon.maPK AS a_pk_lh_nguon_maPK,
                   a_pk_lh_nguon.ngayLap AS a_pk_lh_nguon_ngayLap,
                   a_pk_lh_nguon.trangThai AS a_pk_lh_nguon_trangThai,
                   a_pk_lh_nguon.loiDan AS a_pk_lh_nguon_loiDan,
                   a_pk_lh_nguon_hs.maHSBA AS a_pk_lh_nguon_hs_maHSBA,
                   a_pk_lh_nguon_hs.tienSuBenh AS a_pk_lh_nguon_hs_tienSuBenh,
                   a_pk_lh_nguon_hs_kh.maKH AS a_pk_lh_nguon_hs_kh_maKH,
                   a_pk_lh_nguon_hs_kh.tenKH AS a_pk_lh_nguon_hs_kh_tenKH,
                   a_pk_lh_nguon_hs_kh.ngaySinh AS a_pk_lh_nguon_hs_kh_ngaySinh,
                   a_pk_lh_nguon_hs_kh.gioiTinh AS a_pk_lh_nguon_hs_kh_gioiTinh,
                   a_pk_lh_nguon_hs_kh.sdt AS a_pk_lh_nguon_hs_kh_sdt,
                   a_pk_lh_nguon_hs_kh.diaChi AS a_pk_lh_nguon_hs_kh_diaChi,
                   a_pk_lh_nguon_hs_kh.email AS a_pk_lh_nguon_hs_kh_email,
                   a_pk_lh_nguon_lt.maNV AS a_pk_lh_nguon_lt_maNV,
                   a_pk_lh_nguon_lt.hoTenNV AS a_pk_lh_nguon_lt_hoTenNV,
                   a_pk_lh_nguon_lt.gioiTinh AS a_pk_lh_nguon_lt_gioiTinh,
                   a_pk_lh_nguon_lt.sdt AS a_pk_lh_nguon_lt_sdt,
                   a_pk_lh_nguon_lt.email AS a_pk_lh_nguon_lt_email,
                   a_pk_lh_nguon_lt.chuyenMon AS a_pk_lh_nguon_lt_chuyenMon,
                   a_pk_lt.maNV AS a_pk_lt_maNV,
                   a_pk_lt.hoTenNV AS a_pk_lt_hoTenNV,
                   a_pk_lt.gioiTinh AS a_pk_lt_gioiTinh,
                   a_pk_lt.sdt AS a_pk_lt_sdt,
                   a_pk_lt.email AS a_pk_lt_email,
                   a_pk_lt.chuyenMon AS a_pk_lt_chuyenMon,
                   a_vc.maVoucher AS a_vc_maVoucher,
                   a_vc.tenVoucher AS a_vc_tenVoucher,
                   a_vc.loaiGiam AS a_vc_loaiGiam,
                   a_vc.giaGiam AS a_vc_giaGiam,
                   a_vc.giaTriGiamToiDa AS a_vc_giaTriGiamToiDa,
                   a_vc.ngayBatDau AS a_vc_ngayBatDau,
                   a_vc.ngayKetThuc AS a_vc_ngayKetThuc,
                   a_vc.trangThai AS a_vc_trangThai,
                   a_tn.maNV AS a_tn_maNV,
                   a_tn.hoTenNV AS a_tn_hoTenNV,
                   a_tn.gioiTinh AS a_tn_gioiTinh,
                   a_tn.sdt AS a_tn_sdt,
                   a_tn.email AS a_tn_email,
                   a_tn.chuyenMon AS a_tn_chuyenMon
            FROM HoaDon a
            LEFT JOIN PhieuKham a_pk ON a_pk.maPK=a.maPK
            LEFT JOIN HoSoBenhAn a_pk_hs ON a_pk_hs.maHSBA=a_pk.maHSBA
            LEFT JOIN KhachHang a_pk_hs_kh ON a_pk_hs_kh.maKH=a_pk_hs.maKH
            LEFT JOIN LichHen a_pk_lh ON a_pk_lh.maLH=a_pk.maLH
            LEFT JOIN KhachHang a_pk_lh_kh ON a_pk_lh_kh.maKH=a_pk_lh.maKH
            LEFT JOIN NhanVien a_pk_lh_ns ON a_pk_lh_ns.maNV=a_pk_lh.maNV
            LEFT JOIN DichVu a_pk_lh_dv ON a_pk_lh_dv.maDV=a_pk_lh.maDV
            LEFT JOIN LoaiDichVu a_pk_lh_dv_loai ON a_pk_lh_dv_loai.maLoaiDV=a_pk_lh_dv.maLoaiDV
            LEFT JOIN PhieuKham a_pk_lh_nguon ON a_pk_lh_nguon.maPK=a_pk_lh.maPKNguon
            LEFT JOIN HoSoBenhAn a_pk_lh_nguon_hs ON a_pk_lh_nguon_hs.maHSBA=a_pk_lh_nguon.maHSBA
            LEFT JOIN KhachHang a_pk_lh_nguon_hs_kh ON a_pk_lh_nguon_hs_kh.maKH=a_pk_lh_nguon_hs.maKH
            LEFT JOIN NhanVien a_pk_lh_nguon_lt ON a_pk_lh_nguon_lt.maNV=a_pk_lh_nguon.maNV_LapPhieu
            LEFT JOIN NhanVien a_pk_lt ON a_pk_lt.maNV=a_pk.maNV_LapPhieu
            LEFT JOIN Voucher a_vc ON a_vc.maVoucher=a.maVoucher
            LEFT JOIN NhanVien a_tn ON a_tn.maNV=a.maNV_ThuNgan
            """;

    static HoaDon hoaDon(ResultSet r) throws SQLException {
        return map_HoaDon_a(r);
    }

    private static KhachHang map_HoaDon_a_pk_hs_kh(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_hs_kh_maKH") == null) { return null; }
        return new KhachHang(
                r.getString("a_pk_hs_kh_maKH"),
                r.getString("a_pk_hs_kh_tenKH"),
                r.getObject("a_pk_hs_kh_ngaySinh", LocalDate.class),
                r.getBoolean("a_pk_hs_kh_gioiTinh"),
                r.getString("a_pk_hs_kh_sdt"),
                r.getString("a_pk_hs_kh_diaChi"),
                r.getString("a_pk_hs_kh_email"));
    }

    private static HoSoBenhAn map_HoaDon_a_pk_hs(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_hs_maHSBA") == null) { return null; }
        return new HoSoBenhAn(
                r.getString("a_pk_hs_maHSBA"),
                r.getString("a_pk_hs_tienSuBenh"),
                map_HoaDon_a_pk_hs_kh(r));
    }

    private static KhachHang map_HoaDon_a_pk_lh_kh(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_lh_kh_maKH") == null) { return null; }
        return new KhachHang(
                r.getString("a_pk_lh_kh_maKH"),
                r.getString("a_pk_lh_kh_tenKH"),
                r.getObject("a_pk_lh_kh_ngaySinh", LocalDate.class),
                r.getBoolean("a_pk_lh_kh_gioiTinh"),
                r.getString("a_pk_lh_kh_sdt"),
                r.getString("a_pk_lh_kh_diaChi"),
                r.getString("a_pk_lh_kh_email"));
    }

    private static NhanVien map_HoaDon_a_pk_lh_ns(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_lh_ns_maNV") == null) { return null; }
        return new NhanVien(
                r.getString("a_pk_lh_ns_maNV"),
                r.getString("a_pk_lh_ns_hoTenNV"),
                r.getBoolean("a_pk_lh_ns_gioiTinh"),
                r.getString("a_pk_lh_ns_sdt"),
                r.getString("a_pk_lh_ns_email"),
                r.getString("a_pk_lh_ns_chuyenMon"));
    }

    private static LoaiDichVu map_HoaDon_a_pk_lh_dv_loai(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_lh_dv_loai_maLoaiDV") == null) { return null; }
        return new LoaiDichVu(
                r.getString("a_pk_lh_dv_loai_maLoaiDV"),
                r.getString("a_pk_lh_dv_loai_tenLoaiDV"),
                thue(r.getInt("a_pk_lh_dv_loai_mucThueGTGT")));
    }

    private static DichVu map_HoaDon_a_pk_lh_dv(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_lh_dv_maDV") == null) { return null; }
        return new DichVu(
                r.getString("a_pk_lh_dv_maDV"),
                r.getString("a_pk_lh_dv_tenDV"),
                r.getDouble("a_pk_lh_dv_donGia"),
                r.getInt("a_pk_lh_dv_thoiLuong"),
                r.getString("a_pk_lh_dv_loTrinhDichVu"),
                r.getBoolean("a_pk_lh_dv_trangThai"),
                map_HoaDon_a_pk_lh_dv_loai(r));
    }

    private static KhachHang map_HoaDon_a_pk_lh_nguon_hs_kh(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_lh_nguon_hs_kh_maKH") == null) { return null; }
        return new KhachHang(
                r.getString("a_pk_lh_nguon_hs_kh_maKH"),
                r.getString("a_pk_lh_nguon_hs_kh_tenKH"),
                r.getObject("a_pk_lh_nguon_hs_kh_ngaySinh", LocalDate.class),
                r.getBoolean("a_pk_lh_nguon_hs_kh_gioiTinh"),
                r.getString("a_pk_lh_nguon_hs_kh_sdt"),
                r.getString("a_pk_lh_nguon_hs_kh_diaChi"),
                r.getString("a_pk_lh_nguon_hs_kh_email"));
    }

    private static HoSoBenhAn map_HoaDon_a_pk_lh_nguon_hs(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_lh_nguon_hs_maHSBA") == null) { return null; }
        return new HoSoBenhAn(
                r.getString("a_pk_lh_nguon_hs_maHSBA"),
                r.getString("a_pk_lh_nguon_hs_tienSuBenh"),
                map_HoaDon_a_pk_lh_nguon_hs_kh(r));
    }

    private static NhanVien map_HoaDon_a_pk_lh_nguon_lt(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_lh_nguon_lt_maNV") == null) { return null; }
        return new NhanVien(
                r.getString("a_pk_lh_nguon_lt_maNV"),
                r.getString("a_pk_lh_nguon_lt_hoTenNV"),
                r.getBoolean("a_pk_lh_nguon_lt_gioiTinh"),
                r.getString("a_pk_lh_nguon_lt_sdt"),
                r.getString("a_pk_lh_nguon_lt_email"),
                r.getString("a_pk_lh_nguon_lt_chuyenMon"));
    }

    private static PhieuKham map_HoaDon_a_pk_lh_nguon(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_lh_nguon_maPK") == null) { return null; }
        return new PhieuKham(
                r.getString("a_pk_lh_nguon_maPK"),
                r.getObject("a_pk_lh_nguon_ngayLap", LocalDateTime.class),
                TrangThaiPhieuKham.valueOf(r.getString("a_pk_lh_nguon_trangThai")),
                r.getString("a_pk_lh_nguon_loiDan"),
                map_HoaDon_a_pk_lh_nguon_hs(r),
                null,
                map_HoaDon_a_pk_lh_nguon_lt(r));
    }

    private static LichHen map_HoaDon_a_pk_lh(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_lh_maLH") == null) { return null; }
        return new LichHen(
                r.getString("a_pk_lh_maLH"),
                r.getObject("a_pk_lh_ngayGioHen", LocalDateTime.class),
                TrangThaiLichHen.valueOf(r.getString("a_pk_lh_trangThai")),
                r.getString("a_pk_lh_ghiChu"),
                r.getString("a_pk_lh_lyDoHuy"),
                map_HoaDon_a_pk_lh_kh(r),
                map_HoaDon_a_pk_lh_ns(r),
                map_HoaDon_a_pk_lh_dv(r),
                map_HoaDon_a_pk_lh_nguon(r));
    }

    private static NhanVien map_HoaDon_a_pk_lt(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_lt_maNV") == null) { return null; }
        return new NhanVien(
                r.getString("a_pk_lt_maNV"),
                r.getString("a_pk_lt_hoTenNV"),
                r.getBoolean("a_pk_lt_gioiTinh"),
                r.getString("a_pk_lt_sdt"),
                r.getString("a_pk_lt_email"),
                r.getString("a_pk_lt_chuyenMon"));
    }

    private static PhieuKham map_HoaDon_a_pk(ResultSet r) throws SQLException {
        if (r.getObject("a_pk_maPK") == null) { return null; }
        return new PhieuKham(
                r.getString("a_pk_maPK"),
                r.getObject("a_pk_ngayLap", LocalDateTime.class),
                TrangThaiPhieuKham.valueOf(r.getString("a_pk_trangThai")),
                r.getString("a_pk_loiDan"),
                map_HoaDon_a_pk_hs(r),
                map_HoaDon_a_pk_lh(r),
                map_HoaDon_a_pk_lt(r));
    }

    private static Voucher map_HoaDon_a_vc(ResultSet r) throws SQLException {
        if (r.getObject("a_vc_maVoucher") == null) { return null; }
        return new Voucher(
                r.getString("a_vc_maVoucher"),
                r.getString("a_vc_tenVoucher"),
                r.getBoolean("a_vc_loaiGiam"),
                r.getDouble("a_vc_giaGiam"),
                r.getDouble("a_vc_giaTriGiamToiDa"),
                r.getObject("a_vc_ngayBatDau", LocalDate.class),
                r.getObject("a_vc_ngayKetThuc", LocalDate.class),
                r.getBoolean("a_vc_trangThai"));
    }

    private static NhanVien map_HoaDon_a_tn(ResultSet r) throws SQLException {
        if (r.getObject("a_tn_maNV") == null) { return null; }
        return new NhanVien(
                r.getString("a_tn_maNV"),
                r.getString("a_tn_hoTenNV"),
                r.getBoolean("a_tn_gioiTinh"),
                r.getString("a_tn_sdt"),
                r.getString("a_tn_email"),
                r.getString("a_tn_chuyenMon"));
    }

    private static HoaDon map_HoaDon_a(ResultSet r) throws SQLException {
        if (r.getObject("a_maHD") == null) { return null; }
        return new HoaDon(
                r.getString("a_maHD"),
                r.getObject("a_ngayLap", LocalDateTime.class),
                r.getBoolean("a_trangThai"),
                r.getString("a_hinhThucThanhToan"),
                r.getObject("a_ngayThanhToan", LocalDateTime.class),
                r.getDouble("a_tongKhuyenMai"),
                map_HoaDon_a_pk(r),
                map_HoaDon_a_vc(r),
                map_HoaDon_a_tn(r));
    }

    static final String NHAT_KY = """
            SELECT a.maNhatKy AS a_maNhatKy,
                   a.thoiDiem AS a_thoiDiem,
                   a.hanhDong AS a_hanhDong,
                   a.noiDungThayDoi AS a_noiDungThayDoi,
                   a_tk.maTK AS a_tk_maTK,
                   a_tk.tenTK AS a_tk_tenTK,
                   a_tk.vaiTro AS a_tk_vaiTro,
                   a_tk.trangThai AS a_tk_trangThai,
                   a_tk_nv.maNV AS a_tk_nv_maNV,
                   a_tk_nv.hoTenNV AS a_tk_nv_hoTenNV,
                   a_tk_nv.gioiTinh AS a_tk_nv_gioiTinh,
                   a_tk_nv.sdt AS a_tk_nv_sdt,
                   a_tk_nv.email AS a_tk_nv_email,
                   a_tk_nv.chuyenMon AS a_tk_nv_chuyenMon
            FROM NhatKyHeThong a
            LEFT JOIN TaiKhoan a_tk ON a_tk.maTK=a.maTK
            LEFT JOIN NhanVien a_tk_nv ON a_tk_nv.maNV=a_tk.maNV
            """;

    static NhatKyHeThong nhatKyHeThong(ResultSet r) throws SQLException {
        return map_NhatKyHeThong_a(r);
    }

    private static NhanVien map_NhatKyHeThong_a_tk_nv(ResultSet r) throws SQLException {
        if (r.getObject("a_tk_nv_maNV") == null) { return null; }
        return new NhanVien(
                r.getString("a_tk_nv_maNV"),
                r.getString("a_tk_nv_hoTenNV"),
                r.getBoolean("a_tk_nv_gioiTinh"),
                r.getString("a_tk_nv_sdt"),
                r.getString("a_tk_nv_email"),
                r.getString("a_tk_nv_chuyenMon"));
    }

    private static TaiKhoan map_NhatKyHeThong_a_tk(ResultSet r) throws SQLException {
        if (r.getObject("a_tk_maTK") == null) { return null; }
        return new TaiKhoan(
                r.getString("a_tk_maTK"),
                r.getString("a_tk_tenTK"),
                "[Ẩn]",
                VaiTro.valueOf(r.getString("a_tk_vaiTro")),
                r.getBoolean("a_tk_trangThai"),
                map_NhatKyHeThong_a_tk_nv(r));
    }

    private static NhatKyHeThong map_NhatKyHeThong_a(ResultSet r) throws SQLException {
        if (r.getObject("a_maNhatKy") == null) { return null; }
        return new NhatKyHeThong(
                r.getString("a_maNhatKy"),
                r.getObject("a_thoiDiem", LocalDateTime.class),
                r.getString("a_hanhDong"),
                r.getString("a_noiDungThayDoi"),
                map_NhatKyHeThong_a_tk(r));
    }

    private static MucThue thue(int value) {
        return switch(value) { case 0 -> MucThue.THUE_0; case 5 -> MucThue.THUE_5;
            case 10 -> MucThue.THUE_10; default -> throw new IllegalArgumentException("Mức thuế không hợp lệ."); };
    }
}
