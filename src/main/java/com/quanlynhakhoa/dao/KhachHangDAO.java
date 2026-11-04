package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.dao.support.MaSinh;
import com.quanlynhakhoa.dao.support.NhatKy;
import com.quanlynhakhoa.dao.support.PhanQuyen;
import com.quanlynhakhoa.dao.support.PhanQuyen.Actor;
import com.quanlynhakhoa.entity.KhachHang;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class KhachHangDAO {

    private static final String SQL_SELECT = """
            SELECT maKH, tenKH, ngaySinh, gioiTinh, sdt, diaChi, email
            FROM KhachHang
            """;

    public List<KhachHang> layTatCa() {
        return timKiem("");
    }

    public List<KhachHang> timKiem(String text) {
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c);
            boolean xemTatCa = actor.vaiTro() != VaiTro.NHASI;
            String maNV = xemTatCa ? null : actor.maNV();
            return JdbcHelper.query(c, SQL_SELECT
                    + " WHERE (maKH LIKE ? OR tenKH LIKE ? OR sdt LIKE ?)"
                    + " AND (?=1 OR EXISTS(SELECT 1 FROM LichHen lh"
                    + " WHERE lh.maKH=KhachHang.maKH AND lh.maNV=?)) ORDER BY maKH",
                    this::mapRow, JdbcHelper.tuKhoa(text), JdbcHelper.tuKhoa(text),
                    JdbcHelper.tuKhoa(text), xemTatCa, maNV);
        });
    }

    /** Bản ghi tra cứu kèm lịch gần nhất và trạng thái hồ sơ để hiển thị danh sách. */
    public record TomTat(KhachHang khachHang, LocalDateTime lichGanNhat, int soLanKham,
            boolean coLichSapToi) {

        public String trangThaiHoSo() {
            if (lichGanNhat == null) {
                return "Mới";
            }
            return coLichSapToi ? "Đang điều trị" : "Theo dõi";
        }
    }

    private record ThongKeLich(LocalDateTime lichGanNhat, int soLanKham, int soLichSapToi) {
    }

    /**
     * Tra cứu khách hàng kèm lịch gần nhất/số lần khám, không phát sinh truy vấn theo từng dòng.
     */
    public List<TomTat> timKiemTomTat(String text) {
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c);
            boolean xemTatCa = actor.vaiTro() != VaiTro.NHASI;
            String maNV = xemTatCa ? null : actor.maNV();
            List<KhachHang> ds = JdbcHelper.query(c, SQL_SELECT
                    + " WHERE (maKH LIKE ? OR tenKH LIKE ? OR sdt LIKE ?)"
                    + " AND (?=1 OR EXISTS(SELECT 1 FROM LichHen lh"
                    + " WHERE lh.maKH=KhachHang.maKH AND lh.maNV=?)) ORDER BY maKH",
                    this::mapRow, JdbcHelper.tuKhoa(text), JdbcHelper.tuKhoa(text),
                    JdbcHelper.tuKhoa(text), xemTatCa, maNV);
            if (ds.isEmpty()) {
                return List.of();
            }
            Map<String, ThongKeLich> thongKe = new HashMap<>();
            JdbcHelper.query(c, "SELECT maKH, MAX(ngayGioHen) AS lichGanNhat,"
                    + " SUM(CASE WHEN trangThai='HOAN_TAT' THEN 1 ELSE 0 END) AS soLanKham,"
                    + " SUM(CASE WHEN ngayGioHen>=? AND trangThai IN('DA_DAT','DA_DEN')"
                    + " THEN 1 ELSE 0 END) AS soLichSapToi"
                    + " FROM LichHen WHERE (?=1 OR maNV=?) GROUP BY maKH",
                    r -> {
                        thongKe.put(r.getString("maKH"), new ThongKeLich(
                                r.getObject("lichGanNhat", LocalDateTime.class),
                                r.getInt("soLanKham"), r.getInt("soLichSapToi")));
                        return Boolean.TRUE;
                    }, LocalDateTime.now(), xemTatCa, maNV);
            List<TomTat> ketQua = new ArrayList<>(ds.size());
            for (KhachHang kh : ds) {
                ThongKeLich t = thongKe.get(kh.getMaKH());
                ketQua.add(new TomTat(kh, t == null ? null : t.lichGanNhat(),
                        t == null ? 0 : t.soLanKham(), t != null && t.soLichSapToi() > 0));
            }
            return ketQua;
        });
    }

    /** Mã kế tiếp để hiển thị trước khi lưu; không giữ khóa, mã thật vẫn cấp trong transaction. */
    public String maKeTiep() {
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c, VaiTro.LETAN, VaiTro.QUANLY);
            Set<String> daDung = new HashSet<>(JdbcHelper.query(c,
                    "SELECT maKH FROM KhachHang WHERE maKH LIKE 'KH%'",
                    r -> r.getString(1)));
            for (int n = 1; n < 100_000; n++) {
                String ma = "KH" + String.format(Locale.ROOT, "%05d", n);
                if (!daDung.contains(ma)) {
                    return ma;
                }
            }
            throw new BusinessException("Đã hết mã khách hàng.");
        });
    }

    public KhachHang layTheoMa(String ma) {
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c);
            if (actor.vaiTro() == VaiTro.NHASI) {
                yeuCau(JdbcHelper.exists(c, "SELECT 1 FROM LichHen WHERE maKH=? AND maNV=?",
                        ma, actor.maNV()),
                        "Bệnh nhân chưa được phân công cho bạn.");
            }
            return layTheoMa(c, ma);
        });
    }

    KhachHang layTheoMa(Connection c, String ma) throws SQLException {
        return JdbcHelper.one(c, SQL_SELECT + " WHERE maKH=?", this::mapRow, ma);
    }

    public List<KhachHang> timTrung(String ten, LocalDate ngaySinh, String sdt) {
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c, VaiTro.LETAN, VaiTro.QUANLY);
            return JdbcHelper.query(c, SQL_SELECT
                    + " WHERE sdt=? OR (tenKH LIKE ? AND (? IS NULL OR ngaySinh=?))",
                    this::mapRow, sdt, JdbcHelper.tuKhoa(ten), ngaySinh, ngaySinh);
        });
    }

    public boolean them(KhachHang kh) {
        return them(kh, null);
    }

    public boolean them(KhachHang kh, String tienSuBenh) {
        String maCu = kh.getMaKH();
        try {
            return JdbcHelper.transaction(c -> {
                PhanQuyen.quyen(c, VaiTro.LETAN, VaiTro.QUANLY);
                new KhachHang(null, kh.getTenKH(), kh.getNgaySinh(), kh.isGioiTinh(),
                        kh.getSdt(), kh.getDiaChi(), kh.getEmail());

                String ma = MaSinh.maMoi(c, MaSinh.Bang.KHACH_HANG, null);
                JdbcHelper.update(c, "INSERT INTO KhachHang(maKH,tenKH,ngaySinh,gioiTinh,sdt,diaChi,email)"
                        + " VALUES(?,?,?,?,?,?,?)",
                        ma, kh.getTenKH(), kh.getNgaySinh(), kh.isGioiTinh(),
                        kh.getSdt(), kh.getDiaChi(), kh.getEmail());
                JdbcHelper.update(c, "INSERT INTO HoSoBenhAn(maHSBA,maKH,tienSuBenh) VALUES(?,?,?)",
                        MaSinh.maMoi(c, MaSinh.Bang.HO_SO, null), ma, tienSuBenh);

                kh.setMaKH(ma);
                NhatKy.ghi(c, "THEM_KHACH_HANG", ma);
                return true;
            });
        } catch (RuntimeException e) {
            kh.setMaKH(maCu);
            throw e;
        }
    }

    public boolean capNhat(KhachHang kh) {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.LETAN, VaiTro.QUANLY);
            KhachHang cu = layTheoMa(c, kh.getMaKH());
            yeuCau(cu != null, "Khách hàng không tồn tại.");
            yeuCau(java.util.Objects.equals(cu.getNgaySinh(), kh.getNgaySinh()),
                    "Không được thay đổi ngày sinh đã lưu.");

            boolean ok = JdbcHelper.update(c,
                    "UPDATE KhachHang SET tenKH=?,gioiTinh=?,sdt=?,diaChi=?,email=? WHERE maKH=?",
                    kh.getTenKH(), kh.isGioiTinh(), kh.getSdt(), kh.getDiaChi(),
                    kh.getEmail(), kh.getMaKH()) == 1;
            if (ok) {
                NhatKy.ghi(c, "CAP_NHAT_KHACH_HANG", kh.getMaKH());
            }
            return ok;
        });
    }

    private KhachHang mapRow(ResultSet r) throws SQLException {
        return new KhachHang(
                r.getString("maKH"),
                r.getString("tenKH"),
                r.getObject("ngaySinh", LocalDate.class),
                r.getBoolean("gioiTinh"),
                r.getString("sdt"),
                r.getString("diaChi"),
                r.getString("email"));
    }

    private static void yeuCau(boolean dieuKien, String message) {
        if (!dieuKien) {
            throw new BusinessException(message);
        }
    }
}
