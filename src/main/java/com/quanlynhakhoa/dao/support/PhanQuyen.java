package com.quanlynhakhoa.dao.support;

import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.util.SessionManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

/** Kiểm tra quyền bằng dữ liệu DB, không tin vai trò đã lưu trong UI. */
public final class PhanQuyen {

    private PhanQuyen() { }

    public record Actor(String maTK, VaiTro vaiTro, String maNV) { }

    public static Actor quyen(Connection c, VaiTro... choPhep) throws SQLException {
        TaiKhoan actor = SessionManager.getInstance().getCurrentUser();
        if (actor == null) { throw new BusinessException("Vui lòng đăng nhập."); }

        Actor current = JdbcHelper.one(c, """
                SELECT tk.maTK, tk.vaiTro, nv.maNV
                FROM TaiKhoan tk
                JOIN NhanVien nv ON nv.maNV = tk.maNV
                WHERE tk.maTK=? AND tk.trangThai=1
                """, r -> new Actor(r.getString("maTK"), VaiTro.valueOf(r.getString("vaiTro")),
                r.getString("maNV")), actor.getMaTK());

        if (current == null || (choPhep.length > 0
                && Arrays.stream(choPhep).noneMatch(v -> v == current.vaiTro()))) {
            throw new BusinessException("Bạn không có quyền thực hiện thao tác này hoặc tài khoản đã bị khóa.");
        }
        return current;
    }
}
