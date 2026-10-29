package com.quanlynhakhoa.util;

import com.quanlynhakhoa.entity.TaiKhoan;

/**
 * Singleton lưu thông tin tài khoản đang đăng nhập.
 * Mọi màn hình dùng {@code SessionManager.getInstance().getCurrentUser()}
 * để lấy vai trò và phân quyền hiển thị.
 */
public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private TaiKhoan currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    /** Gán tài khoản sau khi đăng nhập thành công. */
    public void login(TaiKhoan taiKhoan) {
        if (taiKhoan == null) {
            throw new IllegalArgumentException("Tài khoản không được null");
        }
        this.currentUser = taiKhoan;
    }

    /** Xoá phiên đăng nhập hiện tại. */
    public void logout() {
        this.currentUser = null;
    }

    /** Trả về tài khoản đang đăng nhập, hoặc {@code null} nếu chưa đăng nhập. */
    public TaiKhoan getCurrentUser() {
        return currentUser;
    }

    /** Kiểm tra có tài khoản đang đăng nhập không. */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
