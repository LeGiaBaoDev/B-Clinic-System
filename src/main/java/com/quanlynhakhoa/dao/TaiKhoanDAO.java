package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.entity.NhanVien;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO xử lý truy vấn liên quan đến TaiKhoan.
 * Không import javax.swing — ném exception để UI xử lý.
 */
public class TaiKhoanDAO {

    private static final Logger LOG = Logger.getLogger(TaiKhoanDAO.class.getName());

    // UC001 — Đăng nhập
    private static final String SQL_DANG_NHAP =
            "SELECT tk.maTK, tk.tenTK, tk.matKhau, tk.vaiTro, tk.trangThai, " +
            "       nv.maNV, nv.hoTenNV, nv.gioiTinh, nv.sdt, nv.email, nv.chuyenMon " +
            "FROM TaiKhoan tk " +
            "JOIN NhanVien nv ON nv.maNV = tk.maNV " +
            "WHERE tk.tenTK = ?";

    // UC — Quên mật khẩu: xác minh danh tính
    private static final String SQL_XAC_MINH =
            "SELECT tk.maTK FROM TaiKhoan tk " +
            "JOIN NhanVien nv ON nv.maNV = tk.maNV " +
            "WHERE nv.hoTenNV = ? AND nv.sdt = ? AND nv.email = ? AND tk.trangThai = 1";

    // UC — Quên mật khẩu: cập nhật mật khẩu mới
    private static final String SQL_DOI_MAT_KHAU =
            "UPDATE TaiKhoan SET matKhau = ? WHERE maTK = ?";

    // ── Đăng nhập ─────────────────────────────────────────────────────────────

    /**
     * Xác thực đăng nhập bằng tên đăng nhập + mật khẩu gốc.
     * Mật khẩu trong DB lưu plain-text hoặc SHA-256 hex.
     * TODO: thay bằng BCrypt khi tích hợp jBCrypt (T004).
     *
     * @throws BusinessException   tài khoản không tồn tại, sai mật khẩu, hoặc bị khóa
     * @throws DataAccessException lỗi DB
     */
    public TaiKhoan dangNhap(String tenDangNhap, String matKhauNhap) {
        if (tenDangNhap == null || tenDangNhap.isBlank())
            throw new BusinessException("T\u00ean \u0111\u0103ng nh\u1eadp kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng.");
        if (matKhauNhap == null || matKhauNhap.isBlank())
            throw new BusinessException("M\u1eadt kh\u1ea9u kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng.");

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DANG_NHAP)) {

            ps.setString(1, tenDangNhap.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    throw new BusinessException("T\u00ean \u0111\u0103ng nh\u1eadp ho\u1eb7c m\u1eadt kh\u1ea9u ch\u01b0a ch\u00ednh x\u00e1c. Vui l\u00f2ng th\u1eed l\u1ea1i.");

                String matKhauDB = rs.getString("matKhau");
                if (!kiemTraMatKhau(matKhauNhap, matKhauDB))
                    throw new BusinessException("T\u00ean \u0111\u0103ng nh\u1eadp ho\u1eb7c m\u1eadt kh\u1ea9u ch\u01b0a ch\u00ednh x\u00e1c. Vui l\u00f2ng th\u1eed l\u1ea1i.");

                boolean trangThai = rs.getBoolean("trangThai");
                if (!trangThai)
                    throw new BusinessException("T\u00e0i kho\u1ea3n \u0111\u00e3 b\u1ecb kh\u00f3a. Vui l\u00f2ng li\u00ean h\u1ec7 qu\u1ea3n tr\u1ecb vi\u00ean.");

                NhanVien nv = new NhanVien();
                // Map trực tiếp vào field — không qua setter có validate
                // vì PK trong DB là INT IDENTITY, không có format NV###
                mapNhanVien(nv, rs);

                TaiKhoan tk = new TaiKhoan();
                mapTaiKhoan(tk, rs, nv, matKhauDB, trangThai);
                return tk;
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "L\u1ed7i \u0111\u0103ng nh\u1eadp SQL", e);
            throw new DataAccessException("Kh\u00f4ng th\u1ec3 k\u1ebft n\u1ed1i c\u01a1 s\u1edf d\u1eef li\u1ec7u. Vui l\u00f2ng th\u1eed l\u1ea1i sau.", e);
        }
    }

    // ── Quên mật khẩu ────────────────────────────────────────────────────────

    /**
     * Xác minh danh tính nhân viên qua họ tên + SĐT + email.
     *
     * @return maTK nếu khớp
     * @throws BusinessException   thông tin không khớp
     * @throws DataAccessException lỗi DB
     */
    public String xacMinhDanhTinh(String hoTen, String sdt, String email) {
        if (hoTen == null || hoTen.isBlank())  throw new BusinessException("Vui l\u00f2ng nh\u1eadp h\u1ecd t\u00ean.");
        if (sdt   == null || sdt.isBlank())    throw new BusinessException("Vui l\u00f2ng nh\u1eadp s\u1ed1 \u0111i\u1ec7n tho\u1ea1i.");
        if (email == null || email.isBlank())   throw new BusinessException("Vui l\u00f2ng nh\u1eadp email.");

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_XAC_MINH)) {

            ps.setString(1, hoTen.trim());
            ps.setString(2, sdt.trim());
            ps.setString(3, email.trim().toLowerCase());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    throw new BusinessException(
                            "Th\u00f4ng tin kh\u00f4ng kh\u1edbp v\u1edbi b\u1ea5t k\u1ef3 t\u00e0i kho\u1ea3n n\u00e0o.\n"
                          + "Vui l\u00f2ng ki\u1ec3m tra l\u1ea1i h\u1ecd t\u00ean, s\u1ed1 \u0111i\u1ec7n tho\u1ea1i v\u00e0 email.");
                return rs.getString("maTK");
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "L\u1ed7i x\u00e1c minh danh t\u00ednh", e);
            throw new DataAccessException("Kh\u00f4ng th\u1ec3 k\u1ebft n\u1ed1i c\u01a1 s\u1edf d\u1eef li\u1ec7u. Vui l\u00f2ng th\u1eed l\u1ea1i sau.", e);
        }
    }

    /**
     * Cập nhật mật khẩu mới cho tài khoản.
     * TODO: BCrypt hash trước khi lưu khi có jBCrypt.
     *
     * @throws BusinessException   mật khẩu không hợp lệ
     * @throws DataAccessException lỗi DB
     */
    public void doiMatKhau(String maTK, String matKhauMoi) {
        TaiKhoan.kiemTraMatKhauGoc(matKhauMoi);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DOI_MAT_KHAU)) {

            ps.setString(1, matKhauMoi);  // TODO: BCrypt.hashpw(matKhauMoi, BCrypt.gensalt())
            ps.setString(2, maTK);
            ps.executeUpdate();

        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "L\u1ed7i \u0111\u1ed5i m\u1eadt kh\u1ea9u", e);
            throw new DataAccessException("Kh\u00f4ng th\u1ec3 c\u1eadp nh\u1eadt m\u1eadt kh\u1ea9u. Vui l\u00f2ng th\u1eed l\u1ea1i sau.", e);
        }
    }

    // ── Nội bộ ────────────────────────────────────────────────────────────────

    /**
     * Map NhanVien từ ResultSet — dùng reflection-style set thẳng vào field
     * để tránh validate format NV### (PK trong DB là INT IDENTITY).
     */
    private void mapNhanVien(NhanVien nv, java.sql.ResultSet rs) throws java.sql.SQLException {
        // Dùng constructor đầy đủ với giá trị thô từ DB
        // setMaNV validate "NV\\d{3}" nên không dùng — lưu maNV dạng chuỗi số
        try {
            java.lang.reflect.Field f = NhanVien.class.getDeclaredField("maNV");
            f.setAccessible(true);
            f.set(nv, String.valueOf(rs.getInt("maNV")));
        } catch (Exception e) {
            // fallback: bỏ qua maNV nếu không set được
        }
        nv.setHoTenNV(rs.getString("hoTenNV"));
        nv.setGioiTinh(rs.getBoolean("gioiTinh"));
        nv.setSdt(rs.getString("sdt"));
        nv.setEmail(rs.getString("email"));
        nv.setChuyenMon(rs.getString("chuyenMon"));
    }

    /**
     * Map TaiKhoan từ ResultSet — tương tự, bỏ qua validate maTK format.
     */
    private void mapTaiKhoan(TaiKhoan tk, java.sql.ResultSet rs,
                              NhanVien nv, String matKhauDB, boolean trangThai)
            throws java.sql.SQLException {
        try {
            java.lang.reflect.Field f = TaiKhoan.class.getDeclaredField("maTK");
            f.setAccessible(true);
            f.set(tk, String.valueOf(rs.getInt("maTK")));
        } catch (Exception e) {
            // fallback
        }
        tk.setTenTK(rs.getString("tenTK"));
        tk.setMatKhau(matKhauDB);
        tk.setVaiTro(VaiTro.valueOf(rs.getString("vaiTro")));
        tk.setTrangThai(trangThai);
        tk.setNhanVien(nv);
    }

    private boolean kiemTraMatKhau(String matKhauNhap, String matKhauDB) {
        if (matKhauDB == null) return false;
        if (matKhauNhap.equals(matKhauDB)) return true;
        return matKhauDB.equals(shaHex(matKhauNhap));
    }

    private String shaHex(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new DataAccessException("L\u1ed7i m\u00e3 ho\u00e1 m\u1eadt kh\u1ea9u.", e);
        }
    }
}
