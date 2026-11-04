package com.quanlynhakhoa.dao.support;

import com.quanlynhakhoa.exception.BusinessException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Sinh mã tự tăng có khóa ứng dụng SQL Server, chống trùng khi nhiều
 * transaction chạy cùng lúc.
 */
public final class MaSinh {

    private MaSinh() {
    }

    /**
     * Bảng, cột khóa, tiền tố và số chữ số của mã tự sinh.
     */
    public enum Bang {
        NHAN_VIEN("NhanVien", "maNV", "NV", 3),
        TAI_KHOAN("TaiKhoan", "maTK", "TK", 3),
        KHACH_HANG("KhachHang", "maKH", "KH", 5),
        HO_SO("HoSoBenhAn", "maHSBA", "HS", 5),
        LOAI_DICH_VU("LoaiDichVu", "maLoaiDV", "LDV", 2),
        DICH_VU("DichVu", "maDV", "DV", 3),
        LICH_LAM_VIEC("LichLamViec", "maLLV", "LLV", 5),
        LICH_HEN("LichHen", "maLH", "LH", 3),
        PHIEU_KHAM("PhieuKham", "maPK", "PK", 3),
        HOA_DON("HoaDon", "maHD", "HD", 3),
        VOUCHER("Voucher", "maVoucher", "VC", 3),
        NHAT_KY("NhatKyHeThong", "maNhatKy", "NK", 6);

        final String table, key, prefix;
        final int digits;

        Bang(String table, String key, String prefix, int digits) {
            this.table = table;
            this.key = key;
            this.prefix = prefix;
            this.digits = digits;
        }
    }

    public static void khoa(Connection c, String resource) throws SQLException {
        Integer result = JdbcHelper.one(c, "DECLARE @r int; EXEC @r=sys.sp_getapplock @Resource=?,"
                + "@LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=10000; SELECT @r;",
                r -> r.getInt(1), resource);
        if (result == null || result < 0) {
            throw new BusinessException("Dữ liệu đang được xử lý. Vui lòng thử lại.");
        }
    }

    /**
     * Mã chỉ cấp trong transaction, có khóa đến commit. Không dùng MAX để tránh
     * mã demo 99999. {@code ngay} khác null thì chèn yyMMdd sau tiền tố (LH/
     * PK/ HD).
     * 
     */
    public static String maMoi(Connection c, Bang bang, LocalDate ngay) throws SQLException {
        String prefix = bang.prefix
                + (ngay == null ? "" : ngay.format(DateTimeFormatter.ofPattern("yyMMdd")));
        khoa(c, "BClinic:ma:" + bang.table);
        Set<String> used = new HashSet<>(JdbcHelper.query(c, "SELECT " + bang.key + " FROM " + bang.table
                + " WITH(UPDLOCK,HOLDLOCK) WHERE " + bang.key + " LIKE ?",
                r -> r.getString(1), prefix + "%"));
        int limit = (int) Math.pow(10, bang.digits);
        for (int n = 1; n < limit; n++) {
            String ma = prefix + String.format(Locale.ROOT, "%0" + bang.digits + "d", n);
            if (!used.contains(ma)) {
                return ma;
            }
        }
        throw new BusinessException("Đã hết mã cho " + bang.table + ".");
    }
}
