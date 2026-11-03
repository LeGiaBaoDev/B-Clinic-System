package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.sql.*;
import java.util.*;

/** Hạ tầng JDBC nội bộ; tên bảng/cột chỉ lấy từ enum, giá trị luôn bind tham số. */
abstract class DaoSupport {
    protected final DaoContext context;
    protected DaoSupport() { this(DaoContext.macDinh()); }
    protected DaoSupport(DaoContext context) { this.context = Objects.requireNonNull(context); }

    @FunctionalInterface interface Mapper<T> { T map(ResultSet r) throws SQLException; }

    static void bind(PreparedStatement ps, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            Object value = args[i] instanceof Enum<?> e ? e.name() : args[i];
            if (value instanceof java.time.LocalTime) {
                Connection connection = ps.getConnection();
                if (connection.isWrapperFor(com.microsoft.sqlserver.jdbc.ISQLServerConnection.class)) {
                    connection.unwrap(com.microsoft.sqlserver.jdbc.ISQLServerConnection.class).setSendTimeAsDatetime(false);
                }
                ps.setObject(i + 1, value, Types.TIME);
            } else if (value instanceof java.time.LocalDate) {
                ps.setObject(i + 1, value, Types.DATE);
            } else if (value instanceof java.time.LocalDateTime) {
                ps.setObject(i + 1, value, Types.TIMESTAMP);
            } else {
                ps.setObject(i + 1, value);
            }
        }
    }

    static int update(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setQueryTimeout(60);
            bind(ps, args);
            return ps.executeUpdate();
        }
    }

    static <T> List<T> query(Connection c, String sql, Mapper<T> mapper, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setQueryTimeout(60);
            bind(ps, args);
            try (ResultSet r = ps.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (r.next()) { rows.add(mapper.map(r)); }
                return rows;
            }
        }
    }

    static <T> T one(Connection c, String sql, Mapper<T> mapper, Object... args) throws SQLException {
        List<T> rows = query(c, sql, mapper, args);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    static boolean exists(Connection c, String sql, Object... args) throws SQLException {
        return one(c, sql, r -> true, args) != null;
    }

    /** Kiểm tra lại quyền bằng DB, không tin vai trò/trạng thái đã lưu lâu trong UI. */
    protected TaiKhoan quyen(Connection c, VaiTro... choPhep) throws SQLException {
        TaiKhoan actor = context.nguoiDung();
        if (actor == null) { throw new BusinessException("Vui lòng đăng nhập."); }
        TaiKhoan current = one(c, DaoViews.TAI_KHOAN + " WHERE a.maTK=? AND a.trangThai=1",
                DaoViews::taiKhoan, actor.getMaTK());
        if (current == null || (choPhep.length > 0 && Arrays.stream(choPhep).noneMatch(v -> v == current.getVaiTro()))) {
            throw new BusinessException("Bạn không có quyền thực hiện thao tác này hoặc tài khoản đã bị khóa.");
        }
        return current;
    }

    static void can(boolean dieuKien, String message) {
        if (!dieuKien) { throw new BusinessException(message); }
    }

    static void khoa(Connection c, String resource) throws SQLException {
        Integer result = one(c, "DECLARE @r int; EXEC @r=sys.sp_getapplock @Resource=?,"
                + "@LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=10000; SELECT @r;", r -> r.getInt(1), resource);
        can(result != null && result >= 0, "Dữ liệu đang được xử lý. Vui lòng thử lại.");
    }

    enum Bang {
        NHAN_VIEN("NhanVien", "maNV", "NV", 3), TAI_KHOAN("TaiKhoan", "maTK", "TK", 3),
        KHACH_HANG("KhachHang", "maKH", "KH", 5), HO_SO("HoSoBenhAn", "maHSBA", "HS", 5),
        LOAI_DICH_VU("LoaiDichVu", "maLoaiDV", "LDV", 2), DICH_VU("DichVu", "maDV", "DV", 3),
        LICH_LAM_VIEC("LichLamViec", "maLLV", "LLV", 5), LICH_HEN("LichHen", "maLH", "LH", 3),
        PHIEU_KHAM("PhieuKham", "maPK", "PK", 3), HOA_DON("HoaDon", "maHD", "HD", 3),
        VOUCHER("Voucher", "maVoucher", "VC", 3), NHAT_KY("NhatKyHeThong", "maNhatKy", "NK", 6);
        final String table, key, prefix;
        final int digits;
        Bang(String table, String key, String prefix, int digits) {
            this.table=table; this.key=key; this.prefix=prefix; this.digits=digits;
        }
    }

    /** Mã chỉ được cấp trong transaction, có khóa đến commit. Không dùng MAX để tránh mã demo 99999. */
    static String maMoi(Connection c, Bang bang, java.time.LocalDate ngay) throws SQLException {
        String prefix = bang.prefix + (ngay == null ? "" : ngay.format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd")));
        khoa(c, "BClinic:ma:" + bang.table);
        Set<String> used = new HashSet<>(query(c, "SELECT " + bang.key + " FROM " + bang.table
                + " WITH(UPDLOCK,HOLDLOCK) WHERE " + bang.key + " LIKE ?", r -> r.getString(1), prefix + "%"));
        int limit = (int) Math.pow(10, bang.digits);
        for (int n=1; n<limit; n++) {
            String ma = prefix + String.format(Locale.ROOT, "%0" + bang.digits + "d", n);
            if (!used.contains(ma)) { return ma; }
        }
        throw new BusinessException("Đã hết mã cho " + bang.table + ".");
    }

    void nhatKy(Connection c, String hanhDong, String noiDung) throws SQLException {
        TaiKhoan actor = context.nguoiDung();
        if (actor != null) {
            update(c, "INSERT INTO NhatKyHeThong(maNhatKy,maTK,thoiDiem,hanhDong,noiDungThayDoi) VALUES(?,?,?,?,?)",
                    maMoi(c, Bang.NHAT_KY, null), actor.getMaTK(), context.bayGio(), hanhDong, noiDung);
        }
    }

    protected static String tuKhoa(String value) { return "%" + (value == null ? "" : value.trim()) + "%"; }
    protected static void khoangNgay(java.time.LocalDate tu, java.time.LocalDate den) {
        can(tu != null && den != null && !den.isBefore(tu), "Khoảng ngày không hợp lệ.");
    }
}
