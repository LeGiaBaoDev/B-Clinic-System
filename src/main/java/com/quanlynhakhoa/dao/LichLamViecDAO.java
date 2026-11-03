package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.dao.support.MaSinh;
import com.quanlynhakhoa.dao.support.NhatKy;
import com.quanlynhakhoa.dao.support.PhanQuyen;
import com.quanlynhakhoa.dao.support.PhanQuyen.Actor;
import com.quanlynhakhoa.entity.LichLamViec;
import com.quanlynhakhoa.entity.enums.CaTruc;
import com.quanlynhakhoa.entity.enums.TrangThaiLichLamViec;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.sql.*;
import java.time.*;
import java.util.List;

public class LichLamViecDAO {

    private static final String SQL_SELECT = """
            SELECT maLLV, ngayTruc, caTruc, gioBatDau, gioKetThuc,
                   trangThai, ghiChuNghi, maNV
            FROM LichLamViec
            """;

    public List<LichLamViec> layTatCa() {
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c);
            return JdbcHelper.query(c, SQL_SELECT + " WHERE (?=1 OR maNV=?)"
                    + " ORDER BY ngayTruc, gioBatDau",
                    r -> mapRow(c, r), actor.vaiTro() == VaiTro.QUANLY, actor.maNV());
        });
    }

    public List<LichLamViec> layTheoNhanVien(String maNV, LocalDate tu, LocalDate den) {
        khoangNgay(tu, den);
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c);
            yeuCau(actor.vaiTro() == VaiTro.QUANLY || actor.maNV().equals(maNV),
                    "Chỉ được xem lịch làm việc của mình.");
            return JdbcHelper.query(c, SQL_SELECT
                    + " WHERE maNV=? AND ngayTruc BETWEEN ? AND ?"
                    + " ORDER BY ngayTruc, gioBatDau",
                    r -> mapRow(c, r), maNV, tu, den);
        });
    }

    public LichLamViec layTheoMa(String ma) {
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c);
            LichLamViec ll = layTheoMa(c, ma);
            yeuCau(ll == null || actor.vaiTro() == VaiTro.QUANLY
                    || ll.getNhanVien().getMaNV().equals(actor.maNV()),
                    "Chỉ được xem lịch làm việc của mình.");
            return ll;
        });
    }

    private LichLamViec layTheoMa(Connection c, String ma) throws SQLException {
        return JdbcHelper.one(c, SQL_SELECT + " WHERE maLLV=?", r -> mapRow(c, r), ma);
    }

    public boolean them(LichLamViec ll) {
        return luu(ll, true);
    }

    public boolean capNhat(LichLamViec ll) {
        return luu(ll, false);
    }

    private boolean luu(LichLamViec ll, boolean themMoi) {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            MaSinh.khoa(c, "BClinic:lich");
            new LichLamViec(ll.getMaLLV(), ll.getNgayTruc(), ll.getCaTruc(), ll.getGioBatDau(),
                    ll.getGioKetThuc(), ll.getTrangThai(), ll.getGhiChuNghi(), ll.getNhanVien());

            LocalDate homNay = LocalDate.now();
            yeuCau(!ll.getNgayTruc().isBefore(homNay), "Không thay đổi ca trong quá khứ.");
            LocalTime min = ll.getCaTruc() == CaTruc.CA_CHIEU ? LocalTime.of(13, 30) : LocalTime.of(7, 0);
            LocalTime max = ll.getCaTruc() == CaTruc.CA_SANG ? LocalTime.NOON : LocalTime.of(17, 30);
            yeuCau(!ll.getGioBatDau().isBefore(min) && !ll.getGioKetThuc().isAfter(max),
                    "Giờ làm việc nằm ngoài khung của ca.");
            yeuCau(JdbcHelper.exists(c, "SELECT 1 FROM TaiKhoan WHERE maNV=? AND trangThai=1",
                    ll.getNhanVien().getMaNV()),
                    "Nhân viên không có tài khoản hoạt động.");

            String ma = themMoi ? MaSinh.maMoi(c, MaSinh.Bang.LICH_LAM_VIEC, null) : ll.getMaLLV();
            if (!themMoi) {
                LichLamViec old = layTheoMa(c, ma);
                yeuCau(old != null, "Ca làm việc không tồn tại.");
                yeuCau(!old.getNgayTruc().isBefore(homNay), "Không thay đổi lịch sử ca trực.");
            }
            yeuCau(!JdbcHelper.exists(c, "SELECT 1 FROM LichLamViec WHERE maNV=? AND ngayTruc=?"
                    + " AND maLLV<>? AND gioBatDau<? AND gioKetThuc>?",
                    ll.getNhanVien().getMaNV(), ll.getNgayTruc(), ma,
                    ll.getGioKetThuc(), ll.getGioBatDau()),
                    "Nhân viên đã có ca trùng giờ hoặc ca nghỉ phép.");

            if (!themMoi) {
                LichLamViec old = layTheoMa(c, ma);
                var lich = JdbcHelper.query(c, "SELECT lh.ngayGioHen,dv.thoiLuong FROM LichHen lh"
                        + " JOIN DichVu dv ON dv.maDV=lh.maDV"
                        + " WHERE lh.maNV=? AND CAST(lh.ngayGioHen AS DATE)=?"
                        + " AND lh.trangThai IN('DA_DAT','DA_DEN')",
                        r -> new Object[]{r.getObject(1, LocalDateTime.class), r.getInt(2)},
                        old.getNhanVien().getMaNV(), old.getNgayTruc());
                for (Object[] row : lich) {
                    LocalDateTime start = (LocalDateTime) row[0];
                    LocalDateTime end = start.plusMinutes((int) row[1]);
                    boolean covered = ll.getNhanVien().getMaNV().equals(old.getNhanVien().getMaNV())
                            && ll.getNgayTruc().equals(start.toLocalDate())
                            && ll.getTrangThai() == TrangThaiLichLamViec.DA_PHAN_CONG
                            && !start.toLocalTime().isBefore(ll.getGioBatDau())
                            && !end.toLocalTime().isAfter(ll.getGioKetThuc());
                    yeuCau(covered || JdbcHelper.exists(c, "SELECT 1 FROM LichLamViec"
                            + " WHERE maNV=? AND ngayTruc=? AND maLLV<>?"
                            + " AND trangThai='DA_PHAN_CONG' AND gioBatDau<=? AND gioKetThuc>=?",
                            old.getNhanVien().getMaNV(), start.toLocalDate(), ma,
                            start.toLocalTime(), end.toLocalTime()),
                            "Cần dời lịch hẹn trước khi đổi ca hoặc duyệt nghỉ.");
                }
            }

            Object[] args = {ll.getNhanVien().getMaNV(), ll.getNgayTruc(), ll.getCaTruc(),
                ll.getGioBatDau(), ll.getGioKetThuc(), ll.getTrangThai(),
                ll.getGhiChuNghi(), ma};
            int rows = JdbcHelper.update(c, themMoi
                    ? "INSERT INTO LichLamViec(maNV,ngayTruc,caTruc,gioBatDau,gioKetThuc,"
                    + "trangThai,ghiChuNghi,maLLV) VALUES(?,?,?,?,?,?,?,?)"
                    : "UPDATE LichLamViec SET maNV=?,ngayTruc=?,caTruc=?,gioBatDau=?,"
                    + "gioKetThuc=?,trangThai=?,ghiChuNghi=? WHERE maLLV=?",
                    args);
            if (rows == 1) {
                ll.setMaLLV(ma);
                NhatKy.ghi(c, "PHAN_CONG_LICH", ma);
            }
            return rows == 1;
        });
    }

    public boolean duyetNghiPhep(String ma, String lyDo) {
        LichLamViec ll = layTheoMa(ma);
        yeuCau(ll != null, "Ca làm việc không tồn tại.");
        ll.nghiPhep(lyDo);
        return capNhat(ll);
    }

    private LichLamViec mapRow(Connection c, ResultSet r) throws SQLException {
        return new LichLamViec(
                r.getString("maLLV"),
                r.getObject("ngayTruc", LocalDate.class),
                CaTruc.valueOf(r.getString("caTruc")),
                r.getObject("gioBatDau", LocalTime.class),
                r.getObject("gioKetThuc", LocalTime.class),
                TrangThaiLichLamViec.valueOf(r.getString("trangThai")),
                r.getString("ghiChuNghi"),
                new NhanVienDAO().layTheoMa(c, r.getString("maNV")));
    }

    private static void khoangNgay(LocalDate tu, LocalDate den) {
        yeuCau(tu != null && den != null && !den.isBefore(tu), "Khoảng ngày không hợp lệ.");
    }

    private static void yeuCau(boolean dieuKien, String message) {
        if (!dieuKien) {
            throw new BusinessException(message);
        }
    }
}
