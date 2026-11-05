package com.quanlynhakhoa.util;

/** Định dạng và giới hạn đầu vào dùng chung cho entity/form. */
public final class DinhDang {

    /** SĐT Việt Nam: cố định 02x hoặc di động 03/05/07/08/09. */
    public static final String SDT = "0[235789]\\d{8}";
    /** Nhân viên dùng di động để nhận OTP. */
    public static final String SDT_NHAN_VIEN = "0[35789]\\d{8}";
    /** Email: không có dấu chấm liền nhau, không bắt đầu/kết thúc bằng dấu chấm. */
    public static final String EMAIL = "[A-Za-z0-9]+(?:[._%+-][A-Za-z0-9]+)*"
            + "@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?"
            + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)*\\.[A-Za-z]{2,}";
    /** Họ tên: chữ cái Unicode (kể cả dấu tiếng Việt), khoảng trắng, . ' - ; 2–100 ký tự. */
    public static final String HO_TEN = "[\\p{L}\\p{M}][\\p{L}\\p{M}\\s.'-]{0,98}[\\p{L}\\p{M}]";

    public static final int HO_TEN_TOI_DA = 100;
    public static final int EMAIL_TOI_DA = 100;
    public static final int DIA_CHI_TOI_DA = 255;
    public static final int GHI_CHU_TOI_DA = 255;
    public static final int NOI_DUNG_TOI_DA = 500;

    private DinhDang() {
    }

    public static boolean laSdt(String sdt) {
        return sdt != null && sdt.trim().matches(SDT);
    }

    public static boolean laSdtNhanVien(String sdt) {
        return sdt != null && sdt.trim().matches(SDT_NHAN_VIEN);
    }

    public static boolean laEmail(String email) {
        return email != null && email.trim().matches(EMAIL);
    }

    public static boolean laHoTen(String hoTen) {
        return hoTen != null && hoTen.trim().matches(HO_TEN);
    }

    /** Trim và chặn vượt độ dài; rỗng/null trả về null. */
    public static String toiDa(String giaTri, int toiDa, String tenTruong) {
        if (giaTri == null || giaTri.isBlank()) {
            return null;
        }
        String s = giaTri.trim();
        if (s.length() > toiDa) {
            throw new IllegalArgumentException(tenTruong + " tối đa " + toiDa + " ký tự.");
        }
        return s;
    }

    /** Chuẩn hóa khoảng trắng Unicode (kể cả NBSP) rồi trim. */
    public static String chuanHoaKhoangTrang(String giaTri) {
        return giaTri == null ? null : giaTri.replaceAll("(?U)\\s+", " ").trim();
    }
}
