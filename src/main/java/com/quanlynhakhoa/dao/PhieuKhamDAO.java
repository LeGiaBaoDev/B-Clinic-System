package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.dao.support.MaSinh;
import com.quanlynhakhoa.dao.support.NhatKy;
import com.quanlynhakhoa.dao.support.PhanQuyen;
import com.quanlynhakhoa.dao.support.PhanQuyen.Actor;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import com.quanlynhakhoa.exception.BusinessException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class PhieuKhamDAO {

    private static final String SQL_SELECT = """
            SELECT maPK, ngayLap, trangThai, loiDan, maHSBA, maLH, maNV_LapPhieu
            FROM PhieuKham
            """;

    public List<PhieuKham> layTatCa() {
        return timKiem(LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1));
    }

    public List<PhieuKham> timKiem(LocalDate tu, LocalDate den) {
        khoangNgay(tu, den);
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c);
            return JdbcHelper.query(c, SQL_SELECT
                    + " WHERE ngayLap>=? AND ngayLap<? AND (?=1 OR EXISTS("
                    + "SELECT 1 FROM LichHen lh WHERE lh.maLH=PhieuKham.maLH AND lh.maNV=?))"
                    + " ORDER BY ngayLap DESC",
                    r -> loc(mapRow(c, r, true), actor.vaiTro()),
                    tu.atStartOfDay(), den.plusDays(1).atStartOfDay(),
                    actor.vaiTro() != VaiTro.NHASI, actor.maNV());
        });
    }

    public PhieuKham layTheoMa(String ma) {
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c);
            PhieuKham pk = layTheoMa(c, ma, true);
            kiemTraQuyen(pk, actor);
            return loc(pk, actor.vaiTro());
        });
    }

    PhieuKham layTheoMa(Connection c, String ma, boolean napLichHen) throws SQLException {
        return JdbcHelper.one(c, SQL_SELECT + " WHERE maPK=?",
                r -> mapRow(c, r, napLichHen), ma);
    }

    static void kiemTraQuyen(PhieuKham pk, Actor actor) {
        if (pk != null && actor.vaiTro() == VaiTro.NHASI) {
            yeuCau(pk.getLichHen() != null
                    && pk.getLichHen().getNhaSi().getMaNV().equals(actor.maNV()),
                    "Phiếu khám không được phân công cho bạn.");
        }
    }

    /**
     * chỉ tiếp nhận lịch cùng ngày; không nhận lịch đã hủy/không đến/hoàn tất.
     */
    public PhieuKham taoTuLichHen(String maLH) {
        return JdbcHelper.transaction(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.LETAN, VaiTro.QUANLY);
            MaSinh.khoa(c, "BClinic:lich");
            LichHen lh = new LichHenDAO().layTheoMa(c, maLH, false);
            yeuCau(lh != null && lh.getTrangThai() == TrangThaiLichHen.DA_DAT,
                    "Lịch hẹn không ở trạng thái đã đặt.");
            yeuCau(lh.getNgayGioHen().toLocalDate().equals(LocalDate.now()),
                    "Chỉ tiếp nhận lịch của ngày hiện tại.");

            HoSoBenhAn hs = new HoSoBenhAnDAO()
                    .layTheoMaKhachHang(c, lh.getKhachHang().getMaKH());
            yeuCau(hs != null, "Khách hàng chưa có hồ sơ bệnh án.");

            String ma = MaSinh.maMoi(c, MaSinh.Bang.PHIEU_KHAM, LocalDate.now());
            JdbcHelper.update(c, "INSERT INTO PhieuKham(maPK,maHSBA,maLH,maNV_LapPhieu,ngayLap,"
                    + "trangThai) VALUES(?,?,?,?,?,'CHO_KHAM')",
                    ma, hs.getMaHSBA(), maLH, actor.maNV(), LocalDateTime.now());
            yeuCau(JdbcHelper.update(c, "UPDATE LichHen SET trangThai='DA_DEN'"
                    + " WHERE maLH=? AND trangThai='DA_DAT'", maLH) == 1,
                    "Lịch hẹn vừa được người khác xử lý.");
            NhatKy.ghi(c, "LAP_PHIEU_KHAM", ma);
            return loc(layTheoMa(c, ma, true), actor.vaiTro());
        });
    }

    public boolean batDauKham(String maPK) {
        return JdbcHelper.transaction(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.NHASI);
            MaSinh.khoa(c, "BClinic:kham");
            PhieuKham pk = layTheoMa(c, maPK, true);
            yeuCau(pk != null, "Phiếu khám không tồn tại.");
            kiemTraQuyen(pk, actor);
            yeuCau(JdbcHelper.update(c, "UPDATE PhieuKham SET trangThai='DANG_KHAM'"
                    + " WHERE maPK=? AND trangThai='CHO_KHAM'", maPK) == 1,
                    "Chỉ bắt đầu phiếu đang chờ khám.");
            NhatKy.ghi(c, "BAT_DAU_KHAM", maPK);
            return true;
        });
    }

    /**
     * dsThemMoi chỉ chứa dòng chưa lưu (maCTPK=0); dòng đã lưu được giữ nguyên.
     */
    public void hoanTatPhieuKham(PhieuKham pk, List<ChiTietPhieuKham> dsThemMoi) {
        yeuCau(pk != null && dsThemMoi != null, "Thiếu phiếu khám hoặc danh sách điều trị.");
        yeuCau(dsThemMoi.stream().allMatch(Objects::nonNull), "Danh sách chứa chi tiết rỗng.");
        List<Integer> maCu = dsThemMoi.stream().map(ChiTietPhieuKham::getMaCTPK).toList();
        try {
            JdbcHelper.transaction(c -> {
                Actor actor = PhanQuyen.quyen(c, VaiTro.NHASI);
                MaSinh.khoa(c, "BClinic:kham");
                PhieuKham stored = layTheoMa(c, pk.getMaPK(), true);
                yeuCau(stored != null, "Phiếu khám không tồn tại.");
                kiemTraQuyen(stored, actor);
                yeuCau(stored.getTrangThai() == TrangThaiPhieuKham.DANG_KHAM,
                        "Phiếu phải đang khám và chưa bị khóa.");

                ChiTietPhieuKhamDAO ctDAO = new ChiTietPhieuKhamDAO();
                for (ChiTietPhieuKham ct : dsThemMoi) {
                    yeuCau(ct.getMaCTPK() == 0, "Chỉ truyền chi tiết chưa lưu khi hoàn tất phiếu.");
                    ctDAO.them(c, stored, ct, actor);
                }
                yeuCau(JdbcHelper.exists(c, "SELECT 1 FROM ChiTietPhieuKham WHERE maPK=?",
                        stored.getMaPK()),
                        "Phiếu khám phải có ít nhất một dịch vụ điều trị.");

                JdbcHelper.update(c, "UPDATE PhieuKham SET loiDan=?,trangThai='HOAN_TAT'"
                        + " WHERE maPK=? AND trangThai='DANG_KHAM'",
                        pk.getLoiDan(), stored.getMaPK());
                new HoaDonDAO().taoTuPhieuKham(c, stored.getMaPK());
                NhatKy.ghi(c, "HOAN_TAT_KHAM", stored.getMaPK());
                return null;
            });
        } catch (RuntimeException e) {
            for (int i = 0; i < dsThemMoi.size(); i++) {
                dsThemMoi.get(i).setMaCTPK(maCu.get(i));
            }
            throw e;
        }
    }

    private PhieuKham mapRow(Connection c, ResultSet r, boolean napLichHen) throws SQLException {
        LichHen lh = null;
        String maLH = r.getString("maLH");
        if (napLichHen && maLH != null) {
            lh = new LichHenDAO().layTheoMa(c, maLH, false);
        }
        return new PhieuKham(
                r.getString("maPK"),
                r.getObject("ngayLap", LocalDateTime.class),
                TrangThaiPhieuKham.valueOf(r.getString("trangThai")),
                r.getString("loiDan"),
                new HoSoBenhAnDAO().layTheoMa(c, r.getString("maHSBA")),
                lh,
                new NhanVienDAO().layTheoMa(c, r.getString("maNV_LapPhieu")));
    }

    static PhieuKham loc(PhieuKham pk, VaiTro vaiTro) {
        if (pk != null && vaiTro == VaiTro.LETAN) {
            pk.setLoiDan(null);
            if (pk.getHoSoBenhAn() != null) {
                pk.getHoSoBenhAn().setTienSuBenh(null);
            }
            LichHenDAO.loc(pk.getLichHen(), vaiTro);
        }
        return pk;
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
