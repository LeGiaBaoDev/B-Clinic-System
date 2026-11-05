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
        return timKiem("", tu, den, trangThai);
    }

    
    public List<LichHen> timKiem(String tuKhoa, LocalDate tu, LocalDate den, TrangThaiLichHen trangThai) {
        khoangNgay(tu, den);
        String mau = JdbcHelper.tuKhoa(tuKhoa);
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c);
            return JdbcHelper.query(c, """
                    SELECT lh.maLH, lh.ngayGioHen, lh.trangThai, lh.ghiChu, lh.lyDoHuy,
                           lh.maKH, lh.maNV, lh.maDV, lh.maPKNguon
                    FROM LichHen lh JOIN KhachHang kh ON kh.maKH = lh.maKH
                    WHERE lh.ngayGioHen>=? AND lh.ngayGioHen<? AND (? IS NULL OR lh.trangThai=?)
                      AND (lh.maLH LIKE ? OR lh.maKH LIKE ? OR kh.tenKH LIKE ? OR kh.sdt LIKE ?)
                      AND (?=1 OR lh.maNV=?)
                    ORDER BY lh.ngayGioHen
                    """,
                    r -> loc(mapRow(c, r, true), actor.vaiTro()),
                    tu.atStartOfDay(), den.plusDays(1).atStartOfDay(), trangThai, trangThai,
                    mau, mau, mau, mau, actor.vaiTro() != VaiTro.NHASI, actor.maNV());
        });
    }

    
    public List<LichHen> timTheoKhachHang(String maKH) {
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c);
            return JdbcHelper.query(c, SQL_SELECT
                    + " WHERE maKH=? AND (?=1 OR maNV=?) ORDER BY ngayGioHen DESC",
                    r -> loc(mapRow(c, r, true), actor.vaiTro()),
                    maKH, actor.vaiTro() != VaiTro.NHASI, actor.maNV());
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

    
    public boolean capNhatThongTin(LichHen lh) {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.LETAN, VaiTro.QUANLY);
            MaSinh.khoa(c, "BClinic:lich");
            LichHen old = lh.getMaLH() == null ? null : layTheoMa(c, lh.getMaLH(), false);
            yeuCau(old != null && old.getTrangThai() == TrangThaiLichHen.DA_DAT,
                    "Chỉ được cập nhật lịch hẹn đang ở trạng thái đã đặt.");
            yeuCau(lh.getKhachHang() != null
                    && old.getKhachHang().getMaKH().equals(lh.getKhachHang().getMaKH()),
                    "Không được đổi bệnh nhân của lịch hẹn.");
            yeuCau(lh.getNhaSi() != null && lh.getNgayGioHen() != null
                    && old.getNgayGioHen().equals(lh.getNgayGioHen())
                    && old.getNhaSi().getMaNV().equals(lh.getNhaSi().getMaNV()),
                    "Cập nhật thông tin không được đổi ngày giờ hoặc nha sĩ; hãy dùng chức năng dời lịch.");
            kiemTra(c, lh, lh.getMaLH(), false);
            int rows = JdbcHelper.update(c, "UPDATE LichHen SET maDV=?,ghiChu=?"
                    + " WHERE maLH=? AND trangThai='DA_DAT'",
                    lh.getDichVu().getMaDV(), lh.getGhiChu(), lh.getMaLH());
            if (rows == 1) {
                NhatKy.ghi(c, "CAP_NHAT_LICH_HEN", lh.getMaLH());
            }
            return rows == 1;
        });
    }

    
    boolean danhDauDaDen(Connection c, String maLH) throws SQLException {
        return JdbcHelper.update(c, "UPDATE LichHen SET trangThai='DA_DEN'"
                + " WHERE maLH=? AND trangThai='DA_DAT'", maLH) == 1;
    }

    
    boolean danhDauHoanTat(Connection c, String maLH) throws SQLException {
        return JdbcHelper.update(c, "UPDATE LichHen SET trangThai='HOAN_TAT'"
                + " WHERE maLH=? AND trangThai='DA_DEN'", maLH) == 1;
    }

    private boolean luu(LichHen lh, boolean themMoi) {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.LETAN, VaiTro.QUANLY);
            MaSinh.khoa(c, "BClinic:lich");
            return luuTrongGiaoDich(c, lh, themMoi);
        });
    }

    /** Phần ghi của {@link #luu}: đã có giao dịch, đã kiểm quyền và đã khóa mã sinh. */
    private boolean luuTrongGiaoDich(Connection c, LichHen lh, boolean themMoi) throws SQLException {
        {
            if (!themMoi) {
                LichHen old = layTheoMa(c, lh.getMaLH(), false);
                yeuCau(old != null && old.getTrangThai() == TrangThaiLichHen.DA_DAT,
                        "Chỉ được dời lịch đang ở trạng thái đã đặt.");
                yeuCau(lh.getKhachHang() != null
                        && old.getKhachHang().getMaKH().equals(lh.getKhachHang().getMaKH()),
                        "Không được đổi bệnh nhân của lịch hẹn.");
            }
            yeuCau(lh.getTrangThai() == TrangThaiLichHen.DA_DAT,
                    "Đặt/dời lịch phải ở trạng thái đã đặt.");
            kiemTra(c, lh, themMoi ? "" : lh.getMaLH(), true);

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
        }
    }

    /** Khoảng ngày cho phép đặt tái khám (gồm cả hai đầu). */
    public record KhoangNgay(LocalDate tu, LocalDate den) {

        public boolean chua(LocalDate ngay) {
            return ngay != null && !ngay.isBefore(tu) && !ngay.isAfter(den);
        }
    }

    /**
     * UC017 – đặt lịch tái khám từ phiếu khám đã hoàn tất. Khách hàng, nha sĩ và dịch vụ lấy từ
     * phiếu nguồn (giữ cùng nha sĩ); ngày hẹn phải nằm trong khoảng tái khám cho phép.
     *
     * @return lịch hẹn vừa tạo (đã có mã)
     */
    public LichHen taoTaiKham(String maPK, LocalDateTime ngayGio, String ghiChu) {
        yeuCau(ngayGio != null, "Thiếu ngày giờ tái khám.");
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.LETAN, VaiTro.QUANLY);
            MaSinh.khoa(c, "BClinic:lich");
            PhieuKham nguon = phieuNguon(c, maPK);
            LichHen lh = new LichHen(null, ngayGio, TrangThaiLichHen.DA_DAT, ghiChu, null,
                    nguon.getHoSoBenhAn().getKhachHang(), nguon.getLichHen().getNhaSi(),
                    nguon.getLichHen().getDichVu(), nguon);
            yeuCau(luuTrongGiaoDich(c, lh, true), "Không đặt được lịch tái khám.");
            return lh;
        });
    }

    /** Khoảng ngày tái khám hợp lệ của một phiếu khám đã hoàn tất (để giới hạn ô chọn ngày). */
    public KhoangNgay khoangNgayTaiKham(String maPK) {
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c, VaiTro.LETAN, VaiTro.QUANLY);
            return khoangTaiKham(phieuNguon(c, maPK));
        });
    }

    /** Gợi ý khung giờ trống của đúng nha sĩ đã khám, cho một ngày nằm trong khoảng tái khám. */
    public List<LocalDateTime> goiYKhungTrongTaiKham(String maPK, LocalDate ngay) {
        yeuCau(ngay != null, "Thiếu ngày cần tìm.");
        LichHen goc = JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c, VaiTro.LETAN, VaiTro.QUANLY);
            PhieuKham nguon = phieuNguon(c, maPK);
            yeuCau(khoangTaiKham(nguon).chua(ngay), "Ngày tái khám ngoài khoảng cho phép.");
            return nguon.getLichHen();
        });
        return goiYKhungTrong(goc.getKhachHang().getMaKH(), goc.getNhaSi().getMaNV(),
                goc.getDichVu().getMaDV(), ngay);
    }

    private PhieuKham phieuNguon(Connection c, String maPK) throws SQLException {
        PhieuKham nguon = maPK == null ? null : new PhieuKhamDAO().layTheoMa(c, maPK, true);
        yeuCau(nguon != null && nguon.getTrangThai() == TrangThaiPhieuKham.HOAN_TAT
                && nguon.getLichHen() != null, "Phiếu nguồn tái khám chưa hoàn tất.");
        return nguon;
    }

    private KhoangNgay khoangTaiKham(PhieuKham nguon) {
        LocalDate ngayLap = nguon.getNgayLap().toLocalDate();
        return new KhoangNgay(ngayLap.plusDays(quyDinh.taiKhamMinNgay()),
                ngayLap.plusDays(quyDinh.taiKhamMaxNgay()));
    }

    /**
     * @param apDungGioiHanHuy false khi chỉ sửa thông tin của lịch đã có (không đổi giờ), nên không
     *                         áp dụng hạn chế khung giờ do hủy nhiều lần
     */
    private void kiemTra(Connection c, LichHen lh, String boQua, boolean apDungGioiHanHuy)
            throws SQLException {
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

        if (apDungGioiHanHuy) {
            int huy = demHuy(c, lh.getKhachHang().getMaKH(), start.toLocalDate());
            yeuCau(huy < quyDinh.nguongHuy()
                    || quyDinh.thuocKhungGioiHan(start.toLocalTime(), end.toLocalTime()),
                    "Khách hủy nhiều lần trong tháng; chỉ đặt khung đầu/cuối ngày.");
        }

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
            yeuCau(!JdbcHelper.exists(c, "SELECT 1 FROM LichHen WHERE maPKNguon=? AND maLH<>?"
                    + " AND trangThai IN('DA_DAT','DA_DEN','HOAN_TAT')", src.getMaPK(), boQua),
                    "Phiếu khám này đã có lịch tái khám.");
            yeuCau(khoangTaiKham(src).chua(start.toLocalDate()),
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

    public int demSoLanHuyTrongThangChoNhaSi(String maKH, LocalDate ngay) {
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.NHASI, VaiTro.QUANLY);
            yeuCau(actor.vaiTro() == VaiTro.QUANLY || JdbcHelper.exists(c,
                    "SELECT 1 FROM LichHen WHERE maKH=? AND maNV=?", maKH, actor.maNV()),
                    "Bạn không được xem lịch sử hủy của bệnh nhân này.");
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
        return goiYKhungTrong(maKH, maNV, maDV, ngay, null);
    }

    /**
     * Như {@link #goiYKhungTrong(String, String, String, LocalDate)}, nhưng bỏ qua lịch
     * {@code maLHBoQua} (lịch đang được dời) để khung cũ của nó vẫn được gợi ý.
     */
    public List<LocalDateTime> goiYKhungTrong(String maKH, String maNV, String maDV, LocalDate ngay,
            String maLHBoQua) {
        yeuCau(ngay != null, "Thiếu ngày cần tìm.");
        String boQua = maLHBoQua == null ? "" : maLHBoQua;
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
                    + " AND lh.ngayGioHen<? AND lh.trangThai NOT IN('DA_HUY','KHONG_DEN')"
                    + " AND lh.maLH<>?",
                    r -> new Khoang(r.getString(1), r.getString(2),
                            r.getObject(3, LocalDateTime.class), r.getObject(4, LocalDateTime.class)),
                    maKH, maNV, ngay.atStartOfDay(), ngay.plusDays(1).atStartOfDay(), boQua);

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
