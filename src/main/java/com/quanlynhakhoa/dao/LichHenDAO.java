package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.config.QuyDinhLichHen;
import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.dao.support.MaSinh;
import com.quanlynhakhoa.dao.support.NhatKy;
import com.quanlynhakhoa.dao.support.PhanQuyen;
import com.quanlynhakhoa.dao.support.PhanQuyen.Actor;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import com.quanlynhakhoa.exception.BusinessException;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class LichHenDAO {

    private static final String SQL_SELECT = """
            SELECT maLH, ngayGioHen, trangThai, ghiChu, lyDoHuy,
                   maKH, maNV, maDV, maPKNguon
            FROM LichHen
            """;

    private final QuyDinhLichHen quyDinh;

    public LichHenDAO() {
        this(QuyDinhLichHen.macDinh());
    }

    public LichHenDAO(QuyDinhLichHen quyDinh) {
        this.quyDinh = java.util.Objects.requireNonNull(quyDinh);
    }

    public List<LichHen> layTatCa() {
        return timKiem(LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1), null);
    }

    public List<LichHen> timKiem(LocalDate tu, LocalDate den, TrangThaiLichHen trangThai) {
        khoangNgay(tu, den);
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c);
            return JdbcHelper.query(c, SQL_SELECT
                    + " WHERE ngayGioHen>=? AND ngayGioHen<? AND (? IS NULL OR trangThai=?)"
                    + " AND (?=1 OR maNV=?) ORDER BY ngayGioHen",
                    r -> loc(mapRow(c, r, true), actor.vaiTro()),
                    tu.atStartOfDay(), den.plusDays(1).atStartOfDay(), trangThai, trangThai,
                    actor.vaiTro() != VaiTro.NHASI, actor.maNV());
        });
    }

    public LichHen layTheoMa(String ma) {
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c);
            LichHen lh = layTheoMa(c, ma, true);
            yeuCau(lh == null || actor.vaiTro() != VaiTro.NHASI
                    || lh.getNhaSi().getMaNV().equals(actor.maNV()),
                    "Lịch chưa được phân công cho bạn.");
            return loc(lh, actor.vaiTro());
        });
    }

    LichHen layTheoMa(Connection c, String ma, boolean napNguon) throws SQLException {
        return JdbcHelper.one(c, SQL_SELECT + " WHERE maLH=?", r -> mapRow(c, r, napNguon), ma);
    }

    public boolean them(LichHen lh) {
        return luu(lh, true);
    }

    public boolean capNhat(LichHen lh) {
        return luu(lh, false);
    }

    private boolean luu(LichHen lh, boolean themMoi) {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.LETAN, VaiTro.QUANLY);
            MaSinh.khoa(c, "BClinic:lich");
            if (!themMoi) {
                LichHen old = layTheoMa(c, lh.getMaLH(), false);
                yeuCau(old != null && old.getTrangThai() == TrangThaiLichHen.DA_DAT,
                        "Chỉ được dời lịch đang ở trạng thái đã đặt.");
            }
            yeuCau(lh.getTrangThai() == TrangThaiLichHen.DA_DAT,
                    "Đặt/dời lịch phải ở trạng thái đã đặt.");
            kiemTra(c, lh, themMoi ? "" : lh.getMaLH());

            String ma = themMoi
                    ? MaSinh.maMoi(c, MaSinh.Bang.LICH_HEN, lh.getNgayGioHen().toLocalDate())
                    : lh.getMaLH();
            Object[] args = {
                lh.getKhachHang().getMaKH(), lh.getNhaSi().getMaNV(), lh.getDichVu().getMaDV(),
                lh.getPhieuKhamNguon() == null ? null : lh.getPhieuKhamNguon().getMaPK(),
                lh.getNgayGioHen(), lh.getGhiChu(), ma
            };
            int rows = JdbcHelper.update(c, themMoi
                    ? "INSERT INTO LichHen(maKH,maNV,maDV,maPKNguon,ngayGioHen,ghiChu,maLH)"
                    + " VALUES(?,?,?,?,?,?,?)"
                    : "UPDATE LichHen SET maKH=?,maNV=?,maDV=?,maPKNguon=?,ngayGioHen=?,"
                    + "ghiChu=? WHERE maLH=? AND trangThai='DA_DAT'",
                    args);
            if (rows == 1) {
                lh.setMaLH(ma);
                NhatKy.ghi(c, themMoi ? "DAT_LICH" : "DOI_LICH", ma);
            }
            return rows == 1;
        });
    }

    private void kiemTra(Connection c, LichHen lh, String boQua) throws SQLException {
        yeuCau(lh.getKhachHang() != null && lh.getNhaSi() != null
                && lh.getDichVu() != null && lh.getNgayGioHen() != null,
                "Thiếu khách hàng, nha sĩ, dịch vụ hoặc thời gian hẹn.");
        yeuCau(lh.getNgayGioHen().isAfter(LocalDateTime.now()), "Ngày giờ hẹn phải ở tương lai.");

        DichVu dv = new DichVuDAO().layTheoMa(c, lh.getDichVu().getMaDV());
        yeuCau(dv != null && dv.isTrangThai(), "Dịch vụ không hoạt động.");
        yeuCau(JdbcHelper.exists(c, "SELECT 1 FROM KhachHang WHERE maKH=?",
                lh.getKhachHang().getMaKH()), "Khách hàng không tồn tại.");
        yeuCau(JdbcHelper.exists(c, "SELECT 1 FROM TaiKhoan WHERE maNV=?"
                + " AND vaiTro='NHASI' AND trangThai=1", lh.getNhaSi().getMaNV()),
                "Nhân viên không phải nha sĩ đang hoạt động.");

        LocalDateTime start = lh.getNgayGioHen();
        LocalDateTime end = start.plusMinutes(dv.getThoiLuong());
        yeuCau(start.toLocalDate().equals(end.toLocalDate()), "Lịch hẹn không được qua ngày.");
        yeuCau(JdbcHelper.exists(c, "SELECT 1 FROM LichLamViec WITH(UPDLOCK,HOLDLOCK)"
                + " WHERE maNV=? AND ngayTruc=? AND trangThai='DA_PHAN_CONG'"
                + " AND gioBatDau<=? AND gioKetThuc>=?",
                lh.getNhaSi().getMaNV(), start.toLocalDate(),
                start.toLocalTime(), end.toLocalTime()),
                "Nha sĩ không có ca làm việc phù hợp hoặc đang nghỉ phép.");
        yeuCau(!trungLich(c, lh.getKhachHang().getMaKH(), lh.getNhaSi().getMaNV(), start, end, boQua),
                "Trùng lịch khách hàng/nha sĩ hoặc thiếu khoảng cách chuẩn bị.");

        int huy = demHuy(c, lh.getKhachHang().getMaKH(), start.toLocalDate());
        yeuCau(huy < quyDinh.nguongHuy()
                || quyDinh.thuocKhungGioiHan(start.toLocalTime(), end.toLocalTime()),
                "Khách hủy nhiều lần trong tháng; chỉ đặt khung đầu/cuối ngày.");

        if (lh.getPhieuKhamNguon() != null) {
            PhieuKham src = new PhieuKhamDAO().layTheoMa(c, lh.getPhieuKhamNguon().getMaPK(), true);
            yeuCau(src != null && src.getTrangThai() == TrangThaiPhieuKham.HOAN_TAT
                    && src.getLichHen() != null,
                    "Phiếu nguồn tái khám chưa hoàn tất.");
            yeuCau(src.getHoSoBenhAn().getKhachHang().getMaKH().equals(lh.getKhachHang().getMaKH())
                    && src.getLichHen().getNhaSi().getMaNV().equals(lh.getNhaSi().getMaNV())
                    && src.getLichHen().getDichVu().getMaDV().equals(dv.getMaDV()),
                    "Tái khám phải giữ khách hàng, nha sĩ và dịch vụ nguồn.");
            yeuCau(dv.getLoTrinhDichVu() != null, "Dịch vụ chưa có lộ trình tái khám.");
            long days = ChronoUnit.DAYS.between(src.getNgayLap().toLocalDate(), start.toLocalDate());
            yeuCau(days >= quyDinh.taiKhamMinNgay() && days <= quyDinh.taiKhamMaxNgay(),
                    "Ngày tái khám ngoài khoảng cho phép.");
        }
    }

    boolean trungLich(Connection c, String maKH, String maNV,
            LocalDateTime start, LocalDateTime end, String boQua) throws SQLException {
        return JdbcHelper.exists(c, "SELECT 1 FROM LichHen lh WITH(UPDLOCK,HOLDLOCK)"
                + " JOIN DichVu dv ON dv.maDV=lh.maDV"
                + " WHERE lh.maLH<>? AND lh.trangThai NOT IN('DA_HUY','KHONG_DEN')"
                + " AND ((lh.maKH=? AND lh.ngayGioHen<?"
                + " AND DATEADD(MINUTE,dv.thoiLuong,lh.ngayGioHen)>?)"
                + " OR (lh.maNV=? AND lh.ngayGioHen<?"
                + " AND DATEADD(MINUTE,dv.thoiLuong+?,lh.ngayGioHen)>?))",
                boQua, maKH, end, start, maNV,
                end.plusMinutes(quyDinh.khoangCachPhut()), quyDinh.khoangCachPhut(), start);
    }

    private int demHuy(Connection c, String maKH, LocalDate ngay) throws SQLException {
        return JdbcHelper.one(c, "SELECT COUNT(*) FROM LichHen WHERE maKH=?"
                + " AND trangThai='DA_HUY' AND ngayGioHen>=? AND ngayGioHen<?",
                r -> r.getInt(1), maKH, ngay.withDayOfMonth(1).atStartOfDay(),
                ngay.withDayOfMonth(1).plusMonths(1).atStartOfDay());
    }

    public int demSoLanHuyTrongThang(String maKH, LocalDate ngay) {
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c, VaiTro.LETAN, VaiTro.QUANLY);
            return demHuy(c, maKH, ngay);
        });
    }

    public boolean huy(String ma, String lyDo) {
        yeuCau(lyDo != null && !lyDo.isBlank(), "Vui lòng nhập lý do hủy.");
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.LETAN, VaiTro.QUANLY);
            MaSinh.khoa(c, "BClinic:lich");
            yeuCau(JdbcHelper.update(c, "UPDATE LichHen SET trangThai='DA_HUY',lyDoHuy=?"
                    + " WHERE maLH=? AND trangThai='DA_DAT'", lyDo.trim(), ma) == 1,
                    "Chỉ được hủy lịch đang đặt.");
            NhatKy.ghi(c, "HUY_LICH", ma);
            return true;
        });
    }

    /**
     * Gọi khi tải danh sách hoặc từ job; chỉ xử lý lịch chưa đến sau thời gian
     * cho phép trễ.
     */
    public int capNhatKhongDen() {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.LETAN, VaiTro.QUANLY);
            MaSinh.khoa(c, "BClinic:lich");
            return JdbcHelper.update(c, "UPDATE LichHen SET trangThai='KHONG_DEN'"
                    + " WHERE trangThai='DA_DAT' AND ngayGioHen<?",
                    LocalDateTime.now().minusMinutes(quyDinh.treToiDaPhut()));
        });
    }

    /**
     * Gợi ý theo ca trực và thời lượng dịch vụ, loại trùng khách/nha sĩ.
     */
    public List<LocalDateTime> goiYKhungTrong(String maKH, String maNV, String maDV, LocalDate ngay) {
        yeuCau(ngay != null, "Thiếu ngày cần tìm.");
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c, VaiTro.LETAN, VaiTro.QUANLY);
            DichVu dv = new DichVuDAO().layTheoMa(c, maDV);
            yeuCau(dv != null && dv.isTrangThai(), "Dịch vụ không hoạt động.");

            List<LocalTime[]> shifts = JdbcHelper.query(c,
                    "SELECT gioBatDau,gioKetThuc FROM LichLamViec WHERE maNV=? AND ngayTruc=?"
                    + " AND trangThai='DA_PHAN_CONG' ORDER BY gioBatDau",
                    r -> new LocalTime[]{r.getObject(1, LocalTime.class),
                        r.getObject(2, LocalTime.class)}, maNV, ngay);
            record Khoang(String kh, String ns, LocalDateTime batDau, LocalDateTime ketThuc) {

            }
            List<Khoang> busy = JdbcHelper.query(c,
                    "SELECT lh.maKH,lh.maNV,lh.ngayGioHen,DATEADD(MINUTE,dv.thoiLuong,lh.ngayGioHen)"
                    + " FROM LichHen lh JOIN DichVu dv ON dv.maDV=lh.maDV"
                    + " WHERE (lh.maKH=? OR lh.maNV=?) AND lh.ngayGioHen>=?"
                    + " AND lh.ngayGioHen<? AND lh.trangThai NOT IN('DA_HUY','KHONG_DEN')",
                    r -> new Khoang(r.getString(1), r.getString(2),
                            r.getObject(3, LocalDateTime.class), r.getObject(4, LocalDateTime.class)),
                    maKH, maNV, ngay.atStartOfDay(), ngay.plusDays(1).atStartOfDay());

            boolean limited = demHuy(c, maKH, ngay) >= quyDinh.nguongHuy();
            LocalDateTime bayGio = LocalDateTime.now();
            List<LocalDateTime> result = new ArrayList<>();
            for (LocalTime[] shift : shifts) {
                for (LocalDateTime start = ngay.atTime(shift[0]);
                        !start.plusMinutes(dv.getThoiLuong()).isAfter(ngay.atTime(shift[1]));
                        start = start.plusMinutes(quyDinh.khoangCachPhut())) {
                    LocalDateTime end = start.plusMinutes(dv.getThoiLuong());
                    if (!start.isAfter(bayGio)
                            || (limited && !quyDinh.thuocKhungGioiHan(start.toLocalTime(), end.toLocalTime()))) {
                        continue;
                    }
                    boolean conflict = false;
                    for (Khoang b : busy) {
                        if ((maKH.equals(b.kh()) && start.isBefore(b.ketThuc()) && end.isAfter(b.batDau()))
                                || (maNV.equals(b.ns())
                                && start.isBefore(b.ketThuc().plusMinutes(quyDinh.khoangCachPhut()))
                                && end.plusMinutes(quyDinh.khoangCachPhut()).isAfter(b.batDau()))) {
                            conflict = true;
                            break;
                        }
                    }
                    if (!conflict) {
                        result.add(start);
                    }
                }
            }
            return result;
        });
    }

    private LichHen mapRow(Connection c, ResultSet r, boolean napNguon) throws SQLException {
        PhieuKham nguon = null;
        String maPKNguon = r.getString("maPKNguon");
        if (napNguon && maPKNguon != null) {
            nguon = new PhieuKhamDAO().layTheoMa(c, maPKNguon, false);
        }
        return new LichHen(
                r.getString("maLH"),
                r.getObject("ngayGioHen", LocalDateTime.class),
                TrangThaiLichHen.valueOf(r.getString("trangThai")),
                r.getString("ghiChu"),
                r.getString("lyDoHuy"),
                new KhachHangDAO().layTheoMa(c, r.getString("maKH")),
                new NhanVienDAO().layTheoMa(c, r.getString("maNV")),
                new DichVuDAO().layTheoMa(c, r.getString("maDV")),
                nguon);
    }

    static LichHen loc(LichHen lh, VaiTro vaiTro) {
        if (lh != null && vaiTro == VaiTro.LETAN && lh.getPhieuKhamNguon() != null) {
            PhieuKham nguon = lh.getPhieuKhamNguon();
            nguon.setLoiDan(null);
            if (nguon.getHoSoBenhAn() != null) {
                nguon.getHoSoBenhAn().setTienSuBenh(null);
            }
        }
        return lh;
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
