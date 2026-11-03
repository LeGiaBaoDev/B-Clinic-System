package com.quanlynhakhoa.dao.support;

import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.util.SessionManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

/** Ghi nhật ký hệ thống; mật khẩu và nội dung nhạy cảm không được ghi vào đây. */
public final class NhatKy {

    private NhatKy() { }

    /** Ghi theo người đang đăng nhập; bỏ qua nếu chưa có phiên (vd lúc đăng nhập). */
    public static void ghi(Connection c, String hanhDong, String noiDung) throws SQLException {
        TaiKhoan actor = SessionManager.getInstance().getCurrentUser();
        if (actor != null) {
            ghi(c, actor.getMaTK(), hanhDong, noiDung);
        }
    }

    /** Ghi với maTK chỉ định (dùng cho đăng nhập/đổi mật khẩu khi chưa có phiên). */
    public static void ghi(Connection c, String maTK, String hanhDong, String noiDung)
            throws SQLException {
        if (maTK == null) { return; }
        JdbcHelper.update(c, "INSERT INTO NhatKyHeThong(maNhatKy,maTK,thoiDiem,hanhDong,noiDungThayDoi)"
                        + " VALUES(?,?,?,?,?)",
                MaSinh.maMoi(c, MaSinh.Bang.NHAT_KY, null),
                maTK, LocalDateTime.now(), hanhDong, noiDung);
    }
}
