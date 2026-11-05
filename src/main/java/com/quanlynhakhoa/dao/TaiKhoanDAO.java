package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.*;
import com.quanlynhakhoa.dao.support.PhanQuyen.Actor;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.util.PasswordHasher;
import com.quanlynhakhoa.util.QuyTacTaiKhoan;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TaiKhoanDAO {

    private static final String SQL_SELECT = """
            SELECT tk.maTK, tk.tenTK, tk.vaiTro, tk.trangThai,
                   nv.maNV, nv.hoTenNV, nv.gioiTinh, nv.sdt, nv.email, nv.chuyenMon
            FROM TaiKhoan tk
            LEFT JOIN NhanVien nv ON nv.maNV = tk.maNV
            """;

    private static final int SO_LAN_NHAP_OTP_TOI_DA = 5;
    private static final int HAN_OTP_PHUT = 5;
    private static final java.security.SecureRandom NGauNhien = new java.security.SecureRandom();
    private static final ConcurrentHashMap<String, MaOtp> OTP = new ConcurrentHashMap<>();

    private record MaOtp(String ma, LocalDateTime hetHan, int soLanSai) {
        MaOtp(String ma, LocalDateTime hetHan) {
            this(ma, hetHan, 0);
        }
    }

    /** Kết quả tra cứu khi quên mật khẩu: tài khoản và email sẽ nhận mã xác thực. */
    public record ThongTinDatLai(String maTK, String email) {
    }

    public TaiKhoan dangNhap(String tenDangNhap, String matKhauNhap) {
        yeuCau(tenDangNhap != null && !tenDangNhap.isBlank()
                && matKhauNhap != null && !matKhauNhap.isBlank(),
                "Vui lòng nhập tên đăng nhập và mật khẩu.");
        return JdbcHelper.transaction(c -> {
            String stored = JdbcHelper.one(c,
                    "SELECT matKhau FROM TaiKhoan WITH(UPDLOCK,HOLDLOCK) WHERE tenTK=?",
                    r -> r.getString(1), tenDangNhap.trim());
            yeuCau(PasswordHasher.xacMinh(matKhauNhap, stored),
                    "Tên đăng nhập hoặc mật khẩu chưa chính xác.");

            TaiKhoan tk = JdbcHelper.one(c, SQL_SELECT + " WHERE tk.tenTK=?", this::mapRow,
                    tenDangNhap.trim());
            yeuCau(tk != null && tk.isTrangThai(), "Tài khoản đã bị khóa. Vui lòng liên hệ quản lý.");

            if (!stored.startsWith("PBKDF2$")) {
                JdbcHelper.update(c, "UPDATE TaiKhoan SET matKhau=? WHERE maTK=?",
                        PasswordHasher.bam(matKhauNhap), tk.getMaTK());
            }
            NhatKy.ghi(c, tk.getMaTK(), "DANG_NHAP", "Đăng nhập thành công.");
            return tk;
        });
    }

    /**
     * Tra tài khoản và email nhận mã theo số điện thoại hoặc email.
     * Cả hai đều gắn 1–1 với nhân viên nên nhập cách nào cũng về cùng email.
     */
    public ThongTinDatLai traCuuDatLai(String lienHe) {
        yeuCau(lienHe != null && !lienHe.isBlank(), "Vui lòng nhập số điện thoại hoặc email.");
        String id = lienHe.trim();
        ThongTinDatLai tt = JdbcHelper.doc(c -> JdbcHelper.one(c, """
                SELECT tk.maTK, nv.email
                FROM TaiKhoan tk
                JOIN NhanVien nv ON nv.maNV = tk.maNV
                WHERE tk.trangThai=1 AND (nv.sdt=? OR nv.email=?)
                """, r -> new ThongTinDatLai(r.getString(1), r.getString(2)), id, id));
        yeuCau(tt != null, "Số điện thoại hoặc email không hợp lệ hoặc chưa có tài khoản.");
        yeuCau(tt.email() != null && !tt.email().isBlank(),
                "Tài khoản chưa có email nhận mã xác thực.");
        return tt;
    }

    /** Sinh OTP 6 số, hiệu lực 5 phút và ghi đè mã cũ của cùng tài khoản. */
    public String taoOtp(String maTK) {
        yeuCau(maTK != null && !maTK.isBlank(), "Thiếu tài khoản cần đặt lại mật khẩu.");
        String ma = String.format("%06d", NGauNhien.nextInt(1_000_000));
        LocalDateTime bayGio = LocalDateTime.now();
        OTP.entrySet().removeIf(e -> !e.getValue().hetHan().isAfter(bayGio));
        OTP.put(maTK, new MaOtp(ma, bayGio.plusMinutes(HAN_OTP_PHUT)));
        return ma;
    }

    /** Đổi mật khẩu sau khi xác thực OTP đã gửi tới email của tài khoản. */
    public void doiMatKhau(String maTK, String otp, String matKhauMoi) {
        TaiKhoan.kiemTraMatKhauGoc(matKhauMoi);
        kiemTraOtp(maTK, otp);
        JdbcHelper.transaction(c -> {
            yeuCau(JdbcHelper.update(c, "UPDATE TaiKhoan SET matKhau=? WHERE maTK=? AND trangThai=1",
                    PasswordHasher.bam(matKhauMoi), maTK) == 1,
                    "Tài khoản không tồn tại hoặc đã bị khóa.");
            NhatKy.ghi(c, maTK, "DOI_MAT_KHAU", "Đã đổi mật khẩu; không ghi nội dung mật khẩu.");
            return null;
        });
        OTP.remove(maTK);
    }

    /** Xác thực OTP ở bước riêng; OTP vẫn còn hiệu lực cho bước đổi mật khẩu kế tiếp. */
    public void xacThucOtp(String maTK, String otp) {
        yeuCau(maTK != null && !maTK.isBlank(), "Thiếu tài khoản cần xác thực.");
        kiemTraOtp(maTK, otp);
    }

    private void kiemTraOtp(String maTK, String otp) {
        yeuCau(otp != null && !otp.isBlank(), "Vui lòng nhập mã xác thực đã gửi qua email.");
        MaOtp daGui = OTP.get(maTK);
        yeuCau(daGui != null && daGui.hetHan().isAfter(LocalDateTime.now()),
                "Mã xác thực đã hết hạn. Vui lòng gửi lại mã mới.");
        if (!daGui.ma().equals(otp.trim())) {
            int sai = daGui.soLanSai() + 1;
            if (sai >= SO_LAN_NHAP_OTP_TOI_DA) {
                OTP.remove(maTK);
                yeuCau(false, "Nhập sai quá số lần cho phép. Vui lòng gửi lại mã mới.");
            }
            OTP.put(maTK, new MaOtp(daGui.ma(), daGui.hetHan(), sai));
            yeuCau(false, "Mã xác thực chưa chính xác.");
        }
    }

    /** Đổi mật khẩu mặc định trước khi cấp session; luôn kiểm tra lại thông tin xác thực trong transaction. */
    public TaiKhoan doiMatKhauLanDau(String maTK, String matKhauHienTai, String matKhauMoi) {
        String loi = com.quanlynhakhoa.util.KiemTraXacThuc.loiDoiMatKhauLanDau(matKhauMoi, matKhauMoi);
        yeuCau(loi == null, loi);
        yeuCau(QuyTacTaiKhoan.MAT_KHAU_MAC_DINH.equals(matKhauHienTai), "Mật khẩu hiện tại không hợp lệ.");
        return JdbcHelper.transaction(c -> {
            String hash = JdbcHelper.one(c,
                    "SELECT matKhau FROM TaiKhoan WITH(UPDLOCK,HOLDLOCK) WHERE maTK=? AND trangThai=1",
                    r -> r.getString(1), maTK);
            yeuCau(PasswordHasher.xacMinh(matKhauHienTai, hash),
                    "Tài khoản đã bị khóa hoặc mật khẩu đã thay đổi. Vui lòng đăng nhập lại.");
            JdbcHelper.update(c, "UPDATE TaiKhoan SET matKhau=? WHERE maTK=?",
                    PasswordHasher.bam(matKhauMoi), maTK);
            NhatKy.ghi(c, maTK, "DOI_MAT_KHAU_LAN_DAU", "Đã đổi mật khẩu mặc định; không ghi nội dung mật khẩu.");
            return layTheoMa(c, maTK);
        });
    }

    public List<TaiKhoan> layTatCa() {
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            return JdbcHelper.query(c, SQL_SELECT + " ORDER BY tk.maTK", this::mapRow);
        });
    }

    public TaiKhoan layTheoMa(String ma) {
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c);
            yeuCau(actor.vaiTro() == VaiTro.QUANLY || actor.maTK().equals(ma),
                    "Không được xem tài khoản khác.");
            return JdbcHelper.one(c, SQL_SELECT + " WHERE tk.maTK=?", this::mapRow, ma);
        });
    }

    TaiKhoan layTheoMa(Connection c, String ma) throws SQLException {
        return JdbcHelper.one(c, SQL_SELECT + " WHERE tk.maTK=?", this::mapRow, ma);
    }

    public List<TaiKhoan> timKiem(String ten) {
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            return JdbcHelper.query(c, SQL_SELECT
                            + " WHERE tk.tenTK LIKE ? OR nv.hoTenNV LIKE ? ORDER BY tk.maTK",
                    this::mapRow, JdbcHelper.tuKhoa(ten), JdbcHelper.tuKhoa(ten));
        });
    }

    public boolean them(TaiKhoan tk) {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            them(c, tk);
            NhatKy.ghi(c, "TAO_TAI_KHOAN", tk.getMaTK());
            return true;
        });
    }

    /** Tạo nhân viên và tài khoản trong một transaction; lỗi ở bước sau rollback cả nhân viên. */
    public boolean taoNhanVienVaTaiKhoan(NhanVien nv, TaiKhoan tk) {
        String maNVCu = nv.getMaNV();
        String maTKCu = tk.getMaTK();
        try {
            return JdbcHelper.transaction(c -> {
                PhanQuyen.quyen(c, VaiTro.QUANLY);
                new NhanVienDAO().them(c, nv);
                tk.setNhanVien(nv);

                them(c, tk);
                NhatKy.ghi(c, "TAO_NHAN_VIEN_TAI_KHOAN", nv.getMaNV() + " / " + tk.getMaTK());
                return true;
            });
        } catch (RuntimeException e) {
            nv.setMaNV(maNVCu);
            tk.setMaTK(maTKCu);
            throw e;
        }
    }

    public boolean capNhat(TaiKhoan tk) {
        return JdbcHelper.transaction(c -> capNhat(c, tk));
    }

    
    public boolean capNhatNhanVienVaTaiKhoan(NhanVien nv, TaiKhoan tk) {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            new NhanVien(nv.getMaNV(), nv.getHoTenNV(), nv.isGioiTinh(), nv.getSdt(), nv.getEmail(), nv.getChuyenMon());
            VaiTro role = switch (nv.getChuyenMon()) {
                case "Nha sĩ" -> VaiTro.NHASI;
                case "Lễ tân" -> VaiTro.LETAN;
                case "Quản lý" -> VaiTro.QUANLY;
                default -> throw new BusinessException("Vui lòng chọn chuyên môn hợp lệ.");
            };
            yeuCau(tk.getVaiTro() == role, "Vai trò không khớp chuyên môn.");
            TaiKhoan cu = layTheoMa(c, tk.getMaTK());
            yeuCau(cu != null && cu.getNhanVien().getMaNV().equals(nv.getMaNV()),
                    "Không tìm thấy tài khoản nhân viên.");
            TaiKhoan moi = new TaiKhoan(tk);
            moi.setTenTK(cu.getTenTK());
            yeuCau(capNhat(c, moi), "Không tìm thấy tài khoản nhân viên.");
            yeuCau(JdbcHelper.update(c,
                    "UPDATE NhanVien SET hoTenNV=?,gioiTinh=?,sdt=?,email=?,chuyenMon=? WHERE maNV=?",
                    nv.getHoTenNV(), nv.isGioiTinh(), nv.getSdt(), nv.getEmail(), nv.getChuyenMon(), nv.getMaNV()) == 1,
                    "Không tìm thấy nhân viên.");
            NhatKy.ghi(c, "CAP_NHAT_NHAN_VIEN_TAI_KHOAN", nv.getMaNV() + " / " + tk.getMaTK());
            return true;
        });
    }

    private boolean capNhat(Connection c, TaiKhoan tk) throws SQLException {
            Actor actor = PhanQuyen.quyen(c, VaiTro.QUANLY);
            MaSinh.khoa(c, "BClinic:lich");
            MaSinh.khoa(c, "BClinic:tai-khoan");
            yeuCau(!(actor.maTK().equals(tk.getMaTK())
                            && (!tk.isTrangThai() || tk.getVaiTro() != VaiTro.QUANLY)),
                    "Không được tự khóa hoặc hạ quyền tài khoản đang đăng nhập.");
            if (!tk.isTrangThai() || tk.getVaiTro() != VaiTro.NHASI) {
                yeuCau(!JdbcHelper.exists(c, "SELECT 1 FROM LichHen WHERE maNV="
                                + "(SELECT maNV FROM TaiKhoan WHERE maTK=?)"
                                + " AND trangThai IN('DA_DAT','DA_DEN')", tk.getMaTK()),
                        "Nhân viên còn lịch khám chưa xử lý.");
            }
            boolean ok = JdbcHelper.update(c,
                    "UPDATE TaiKhoan SET tenTK=?,vaiTro=?,trangThai=? WHERE maTK=?",
                    tk.getTenTK(), tk.getVaiTro(), tk.isTrangThai(), tk.getMaTK()) == 1;
            if (ok) { NhatKy.ghi(c, "CAP_NHAT_TAI_KHOAN", tk.getMaTK()); }
            return ok;
    }

    public boolean khoaTaiKhoan(String ma) {
        TaiKhoan tk = layTheoMa(ma);
        yeuCau(tk != null, "Tài khoản không tồn tại.");
        tk.setTrangThai(false);
        return capNhat(tk);
    }

    public boolean xoa(String ma) { return khoaTaiKhoan(ma); }

    private void them(Connection c, TaiKhoan tk) throws SQLException {
        yeuCau(tk.getNhanVien() != null, "Thiếu nhân viên để tạo tài khoản.");
        NhanVien nv = new NhanVienDAO().layTheoMa(c, tk.getNhanVien().getMaNV());
        yeuCau(nv != null, "Nhân viên không tồn tại.");
        // Giữ khóa tới khi commit để hai lần tạo đồng thời không chọn cùng tên trống.
        List<String> daDung = JdbcHelper.query(c,
                "SELECT tenTK FROM TaiKhoan WITH(TABLOCKX,HOLDLOCK)", r -> r.getString(1));
        String ten = QuyTacTaiKhoan.tenDangNhap(nv.getHoTenNV(), tk.getVaiTro(), daDung);
        new TaiKhoan(null, ten, QuyTacTaiKhoan.MAT_KHAU_MAC_DINH, tk.getVaiTro(), tk.isTrangThai(), nv);

        String ma = MaSinh.maMoi(c, MaSinh.Bang.TAI_KHOAN, null);
        JdbcHelper.update(c, "INSERT INTO TaiKhoan(maTK,tenTK,matKhau,vaiTro,trangThai,maNV)"
                        + " VALUES(?,?,?,?,?,?)",
                ma, ten, PasswordHasher.bam(QuyTacTaiKhoan.MAT_KHAU_MAC_DINH), tk.getVaiTro(),
                tk.isTrangThai(), tk.getNhanVien().getMaNV());
        tk.setTenTK(ten);
        tk.setMaTK(ma);
    }

    private TaiKhoan mapRow(ResultSet r) throws SQLException {
        NhanVien nv = NhanVien.tuDuLieu(
                r.getString("maNV"),
                r.getString("hoTenNV"),
                r.getBoolean("gioiTinh"),
                r.getString("sdt"),
                r.getString("email"),
                r.getString("chuyenMon"));
        return new TaiKhoan(
                r.getString("maTK"),
                r.getString("tenTK"),
                "[Ẩn]",
                VaiTro.valueOf(r.getString("vaiTro")),
                r.getBoolean("trangThai"),
                nv);
    }

    private static void yeuCau(boolean dieuKien, String message) {
        if (!dieuKien) { throw new BusinessException(message); }
    }
}
