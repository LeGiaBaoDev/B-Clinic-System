package com.quanlynhakhoa.util;

import com.quanlynhakhoa.entity.enums.VaiTro;
import java.util.Collection;
import java.util.Locale;

public final class QuyTacTaiKhoan {
    public static final String MAT_KHAU_MAC_DINH = "Clinic@123";
    private QuyTacTaiKhoan() { }
    public static String tienTo(VaiTro vaiTro) {
        if(vaiTro==null) throw new IllegalArgumentException("Vui lòng chọn vai trò.");
        return switch(vaiTro) { case QUANLY->"QL";case NHASI->"NS";case LETAN->"LT"; };
    }
    public static String tenDangNhap(String maNV,VaiTro vaiTro) {
        String prefix=tienTo(vaiTro);
        if(maNV==null || !maNV.matches(prefix+"\\d{3}") || maNV.endsWith("000")) throw new IllegalArgumentException("Tên đăng nhập phải là mã nhân viên đúng vai trò.");
        return maNV;
    }
    /** Chỉ là gợi ý hiển thị; DB cấp mã chính thức trong transaction. Không lấp lại mã đã dùng. */
    public static String goiYTenDangNhap(VaiTro vaiTro,Collection<String> daDung) {
        String prefix=tienTo(vaiTro);int max=0;
        for(String s:daDung) if(s!=null && s.toUpperCase(Locale.ROOT).matches(prefix+"\\d{3}")) max=Math.max(max,Integer.parseInt(s.substring(2)));
        if(max>=999) throw new IllegalStateException("Đã hết mã nhân viên cho vai trò.");
        return prefix+String.format(Locale.ROOT,"%03d",max+1);
    }
    /** Tương thích bộ gợi ý cũ; tên người không còn quyết định tên đăng nhập. */
    public static String tenDangNhap(String hoTen,VaiTro vaiTro,Collection<String> daDung) {
        if(hoTen==null || hoTen.isBlank()) throw new IllegalArgumentException("Vui lòng nhập họ tên.");
        return goiYTenDangNhap(vaiTro,daDung);
    }
}
