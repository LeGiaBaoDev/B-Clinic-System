package com.quanlynhakhoa.util;

import com.quanlynhakhoa.entity.enums.VaiTro;
import java.text.Normalizer;
import java.util.Locale;

/** Quy tắc cấp tài khoản mới; không áp dụng lại khi đăng nhập hoặc đổi mật khẩu. */
public final class QuyTacTaiKhoan {
    public static final String MAT_KHAU_MAC_DINH = "Clinic@123";

    private QuyTacTaiKhoan() { }

    /** Giữ tên gốc nếu còn trống, nếu trùng thêm số nhỏ nhất trước dấu chấm. */
    public static String tenDangNhap(String hoTen, VaiTro vaiTro, java.util.Collection<String> daDung) {
        String goc = tenDangNhap(hoTen, vaiTro);
        java.util.Set<String> tenDaDung = new java.util.HashSet<>();
        for (String ten : daDung) { tenDaDung.add(ten.toLowerCase(Locale.ROOT)); }
        String ten = goc;
        int cham = goc.indexOf('.');
        for (int so = 1; tenDaDung.contains(ten); so++) {
            ten = goc.substring(0, cham) + so + goc.substring(cham);
            if (ten.length() > 50) {
                throw new IllegalArgumentException("Tên tài khoản tự sinh vượt quá 50 ký tự.");
            }
        }
        return ten;
    }

    /** Lấy tên cuối, viết thường không dấu và ghép hậu tố vai trò. */
    public static String tenDangNhap(String hoTen, VaiTro vaiTro) {
        if (hoTen == null || hoTen.isBlank() || vaiTro == null) {
            throw new IllegalArgumentException("Vui lòng nhập họ tên và chọn chuyên môn để tạo tài khoản.");
        }
        String name = hoTen.replaceAll("(?U)\\s+", " ").trim();
        String ten = name.substring(name.lastIndexOf(' ') + 1).toLowerCase(Locale.ROOT);
        ten = Normalizer.normalize(ten, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replace('đ', 'd').replaceAll("[^a-z]", "");
        if (ten.isEmpty()) {
            throw new IllegalArgumentException("Tên nhân viên phải chứa chữ cái để tạo tài khoản.");
        }
        String result = ten + "." + vaiTro.name().toLowerCase(Locale.ROOT);
        if (result.length() > 50) {
            throw new IllegalArgumentException("Tên tài khoản tự sinh vượt quá 50 ký tự.");
        }
        return result;
    }
}
