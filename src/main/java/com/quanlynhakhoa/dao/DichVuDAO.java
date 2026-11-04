package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.dao.support.MaSinh;
import com.quanlynhakhoa.dao.support.NhatKy;
import com.quanlynhakhoa.dao.support.PhanQuyen;
import com.quanlynhakhoa.entity.DichVu;
import com.quanlynhakhoa.entity.LoaiDichVu;
import com.quanlynhakhoa.entity.enums.MucThue;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * UC005–008: danh mục, giá hiện tại; không sửa ảnh chụp giá ở chi tiết khám cũ.
 */
public class DichVuDAO {

    private static final String SQL_SELECT = """
            SELECT dv.maDV, dv.tenDV, dv.donGia, dv.thoiLuong, dv.loTrinhDichVu, dv.trangThai,
                   ldv.maLoaiDV, ldv.tenLoaiDV, ldv.mucThueGTGT
            FROM DichVu dv
            LEFT JOIN LoaiDichVu ldv ON ldv.maLoaiDV = dv.maLoaiDV
            """;

    public List<DichVu> layTatCa() {
        return timKiem("", false);
    }

    /**
     * Danh sách chọn dịch vụ cho bên khác (đặt lịch, chỉ định): {@code timKiem("", true)}
     * chỉ trả dịch vụ đang cung cấp (UC008).
     */
    public List<DichVu> timKiem(String ten, boolean chiDangHoatDong) {
        return timKiem(ten, chiDangHoatDong ? Boolean.TRUE : null, null);
    }

    /**
     * UC006: tra cứu danh mục theo tên hoặc mã, lọc trạng thái và phân loại thuế.
     *
     * @param tuKhoa    một phần tên hoặc mã dịch vụ; rỗng/null = tất cả
     * @param trangThai true = đang cung cấp, false = ngừng cung cấp, null = tất cả
     * @param mucThue   mức thuế GTGT của loại dịch vụ; null = tất cả
     */
    public List<DichVu> timKiem(String tuKhoa, Boolean trangThai, MucThue mucThue) {
        StringBuilder sql = new StringBuilder(SQL_SELECT)
                .append(" WHERE (dv.tenDV LIKE ? ESCAPE '\\' OR dv.maDV LIKE ? ESCAPE '\\')");
        List<Object> thamSo = new ArrayList<>();
        String mau = mauTimKiem(tuKhoa);
        thamSo.add(mau);
        thamSo.add(mau);
        // Chỉ ghép đoạn SQL cố định; giá trị lọc luôn đi qua tham số.
        if (trangThai != null) {
            sql.append(" AND dv.trangThai=?");
            thamSo.add(trangThai);
        }
        if (mucThue != null) {
            sql.append(" AND ldv.mucThueGTGT=?");
            thamSo.add(LoaiDichVuDAO.thue(mucThue));
        }
        sql.append(" ORDER BY dv.trangThai DESC, dv.maDV");
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c);
            return JdbcHelper.query(c, sql.toString(), this::mapRow, thamSo.toArray());
        });
    }

    /** Mẫu LIKE coi %, _, [ người dùng gõ là ký tự thường. */
    private static String mauTimKiem(String tuKhoa) {
        String text = tuKhoa == null ? "" : tuKhoa.trim();
        return "%" + text.replace("\\", "\\\\").replace("%", "\\%")
                .replace("_", "\\_").replace("[", "\\[") + "%";
    }

    public DichVu layTheoMa(String ma) {
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c);
            return layTheoMa(c, ma);
        });
    }

    DichVu layTheoMa(Connection c, String ma) throws SQLException {
        return JdbcHelper.one(c, SQL_SELECT + " WHERE dv.maDV=?", this::mapRow, ma);
    }

    /**
     * UC005: thêm dịch vụ mới, sinh mã DV###, trạng thái "Đang cung cấp". Loại chỉ cần đúng tên
     * (mã có thể null): DB chưa có loại thì tạo theo 3 loại cố định của Bảng 4.5. Chỉ Quản lý.
     */
    public boolean them(DichVu dv) {
        return luu(dv, true);
    }

    public boolean capNhat(DichVu dv) {
        return luu(dv, false);
    }

    private boolean luu(DichVu dv, boolean themMoi) {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            new DichVu(dv.getMaDV(), dv.getTenDV(), dv.getDonGia(), dv.getThoiLuong(),
                    dv.getLoTrinhDichVu(), dv.isTrangThai(), dv.getLoaiDichVu());
            MaSinh.khoa(c, "BClinic:lich");
            MaSinh.khoa(c, "BClinic:dich-vu");

            if (!themMoi) {
                DichVu old = layTheoMa(c, dv.getMaDV());
                yeuCau(old != null, "Dịch vụ không tồn tại.");
                if (!dv.isTrangThai() || old.getThoiLuong() != dv.getThoiLuong()) {
                    yeuCau(!JdbcHelper.exists(c, "SELECT 1 FROM LichHen WHERE maDV=?"
                            + " AND trangThai IN('DA_DAT','DA_DEN')", dv.getMaDV()),
                            "Cần xử lý các lịch hẹn hiện có trước khi đổi thời lượng hoặc ngừng dịch vụ.");
                }
            }
            // UC005 hậu điều kiện: dịch vụ mới luôn ở trạng thái "Đang cung cấp".
            boolean dangCungCap = themMoi || dv.isTrangThai();
            // UC005 5.1 / UC007 5.2: tên không trùng dịch vụ khác đang hoạt động (collation không phân biệt hoa thường).
            yeuCau(!dangCungCap || !JdbcHelper.exists(c,
                    "SELECT 1 FROM DichVu WITH(UPDLOCK,HOLDLOCK) WHERE trangThai=1 AND tenDV=? AND maDV<>?",
                    dv.getTenDV(), themMoi ? "" : dv.getMaDV()),
                    themMoi ? "Dịch vụ đã tồn tại. Vui lòng đổi tên dịch vụ." : "Tên dịch vụ đã tồn tại.");
            // UC005 bước 6: mức thuế đi theo phân loại đã chọn.
            LoaiDichVu loai = new LoaiDichVuDAO().layHoacTao(c, dv.getLoaiDichVu());

            String ma = themMoi ? MaSinh.maMoi(c, MaSinh.Bang.DICH_VU, null) : dv.getMaDV();
            int rows = themMoi
                    ? JdbcHelper.update(c, "INSERT INTO DichVu(maDV,tenDV,maLoaiDV,donGia,thoiLuong,"
                            + "loTrinhDichVu,trangThai) VALUES(?,?,?,?,?,?,?)",
                            ma, dv.getTenDV(), loai.getMaLoaiDV(),
                            BigDecimal.valueOf(dv.getDonGia()), dv.getThoiLuong(),
                            dv.getLoTrinhDichVu(), dangCungCap)
                    : JdbcHelper.update(c, "UPDATE DichVu SET tenDV=?,maLoaiDV=?,donGia=?,thoiLuong=?,"
                            + "loTrinhDichVu=?,trangThai=? WHERE maDV=?",
                            dv.getTenDV(), loai.getMaLoaiDV(),
                            BigDecimal.valueOf(dv.getDonGia()), dv.getThoiLuong(),
                            dv.getLoTrinhDichVu(), dangCungCap, ma);
            if (rows == 1) {
                dv.setMaDV(ma);
                dv.setTrangThai(dangCungCap);
                dv.setLoaiDichVu(loai);
                NhatKy.ghi(c, themMoi ? "THEM_DICH_VU" : "CAP_NHAT_DICH_VU", ma);
            }
            return rows == 1;
        });
    }

    public boolean ngungCungCap(String ma) {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            MaSinh.khoa(c, "BClinic:lich");
            yeuCau(!JdbcHelper.exists(c, "SELECT 1 FROM LichHen WHERE maDV=?"
                    + " AND trangThai IN('DA_DAT','DA_DEN')", ma),
                    "Dịch vụ còn lịch hẹn chưa xử lý.");
            boolean ok = JdbcHelper.update(c, "UPDATE DichVu SET trangThai=0 WHERE maDV=?", ma) == 1;
            if (ok) {
                NhatKy.ghi(c, "NGUNG_DICH_VU", ma);
            }
            return ok;
        });
    }

    public boolean xoa(String ma) {
        return ngungCungCap(ma);
    }

    private DichVu mapRow(ResultSet r) throws SQLException {
        LoaiDichVu loai = null;
        if (r.getString("maLoaiDV") != null) {
            loai = new LoaiDichVu(
                    r.getString("maLoaiDV"),
                    r.getString("tenLoaiDV"),
                    mucThue(r.getInt("mucThueGTGT")));
        }
        return new DichVu(
                r.getString("maDV"),
                r.getString("tenDV"),
                r.getDouble("donGia"),
                r.getInt("thoiLuong"),
                r.getString("loTrinhDichVu"),
                r.getBoolean("trangThai"),
                loai);
    }

    private static com.quanlynhakhoa.entity.enums.MucThue mucThue(int value) {
        return switch (value) {
            case 0 ->
                com.quanlynhakhoa.entity.enums.MucThue.THUE_0;
            case 5 ->
                com.quanlynhakhoa.entity.enums.MucThue.THUE_5;
            case 10 ->
                com.quanlynhakhoa.entity.enums.MucThue.THUE_10;
            default ->
                throw new BusinessException("Mức thuế GTGT không hợp lệ: " + value);
        };
    }

    private static void yeuCau(boolean dieuKien, String message) {
        if (!dieuKien) {
            throw new BusinessException(message);
        }
    }
}
