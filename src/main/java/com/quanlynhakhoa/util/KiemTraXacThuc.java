package com.quanlynhakhoa.util;

public final class KiemTraXacThuc {

    public static final int DO_DAI_MAT_KHAU_TOI_THIEU = 6;

    private static final String REGEX_SDT = "0[35789]\\d{8}";
    private static final String REGEX_EMAIL = "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}";

    private KiemTraXacThuc() {
    }

    public static String loiDoiMatKhauLanDau(String matKhauMoi, String nhapLai) {
        if (matKhauMoi == null || matKhauMoi.isBlank() || matKhauMoi.length() < DO_DAI_MAT_KHAU_TOI_THIEU) {
            return "Mật khẩu mới phải có ít nhất " + DO_DAI_MAT_KHAU_TOI_THIEU + " ký tự, không chỉ gồm khoảng trắng.";
        }
        if (QuyTacTaiKhoan.MAT_KHAU_MAC_DINH.equals(matKhauMoi)) {
            return "Mật khẩu mới phải khác mật khẩu mặc định.";
        }
        if (!matKhauMoi.equals(nhapLai)) { return "Xác nhận mật khẩu không khớp."; }
        return null;
    }

    /**
     * @return câu lỗi, hoặc null nếu dữ liệu đăng nhập hợp lệ.
     */
    public static String loiDangNhap(String tenDangNhap, String matKhau) {
        if (tenDangNhap == null || tenDangNhap.isBlank()) {
            return "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.";
        }
        if (matKhau == null || matKhau.isBlank()) {
            return "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.";
        }
        return null;
    }

    /**
     * @return câu lỗi, hoặc null nếu dữ liệu đặt lại mật khẩu hợp lệ.
     */
    public static String loiDatLaiMatKhau(String lienHe, String matKhauMoi, String nhapLai) {
        if (lienHe == null || lienHe.isBlank()) {
            return "Vui lòng nhập số điện thoại hoặc email.";
        }
        if (!laLienHeHopLe(lienHe)) {
            return "Số điện thoại hoặc email không đúng định dạng.";
        }
        if (matKhauMoi == null || matKhauMoi.length() < DO_DAI_MAT_KHAU_TOI_THIEU) {
            return "Mật khẩu phải có ít nhất " + DO_DAI_MAT_KHAU_TOI_THIEU + " ký tự.";
        }
        if (!matKhauMoi.equals(nhapLai)) {
            return "Mật khẩu xác nhập không khớp.";
        }
        return null;
    }

    public static boolean laLienHeHopLe(String lienHe) {
        if (lienHe == null) {
            return false;
        }
        String id = lienHe.trim();
        return id.matches(REGEX_SDT) || id.matches(REGEX_EMAIL);
    }
}
