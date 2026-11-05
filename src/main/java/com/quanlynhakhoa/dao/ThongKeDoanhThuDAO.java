package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.dao.support.PhanQuyen;
import com.quanlynhakhoa.dao.support.PhanQuyen.Actor;
import com.quanlynhakhoa.entity.HoaDon;
import com.quanlynhakhoa.entity.enums.TrangThaiLichHen;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class ThongKeDoanhThuDAO {

    private static final String TONG = """
            WITH TienPhieu AS (
                SELECT maPK,SUM(thanhTien+ROUND(thanhTien*mucThueGTGT/100,0)) AS tong
                FROM ChiTietPhieuKham GROUP BY maPK
            )
            """;

    public record TheoNgay(LocalDate ngay, long soHoaDon, BigDecimal doanhThu,
            BigDecimal tienMat, BigDecimal chuyenKhoan) {

    }

    public List<TheoNgay> thongKe(LocalDate tu, LocalDate den) {
        khoangNgay(tu, den);
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.QUANLY, VaiTro.LETAN);
            yeuCau(actor.vaiTro() == VaiTro.QUANLY || tu.equals(den),
                    "Lễ tân chỉ được xem doanh thu theo ngày.");
            return JdbcHelper.query(c, TONG
                    + "SELECT CAST(hd.ngayThanhToan AS DATE),COUNT_BIG(*),"
                    + "SUM(t.tong-hd.tongKhuyenMai),"
                    + "SUM(CASE WHEN hd.hinhThucThanhToan=? THEN t.tong-hd.tongKhuyenMai ELSE 0 END),"
                    + "SUM(CASE WHEN hd.hinhThucThanhToan=? THEN t.tong-hd.tongKhuyenMai ELSE 0 END)"
                    + " FROM HoaDon hd JOIN TienPhieu t ON t.maPK=hd.maPK"
                    + " WHERE hd.trangThai=1 AND hd.ngayThanhToan>=? AND hd.ngayThanhToan<?"
                    + " GROUP BY CAST(hd.ngayThanhToan AS DATE)"
                    + " ORDER BY CAST(hd.ngayThanhToan AS DATE)",
                    r -> new TheoNgay(r.getObject(1, LocalDate.class), r.getLong(2),
                            r.getBigDecimal(3), r.getBigDecimal(4), r.getBigDecimal(5)),
                    HoaDon.TIEN_MAT, HoaDon.CHUYEN_KHOAN,
                    tu.atStartOfDay(), den.plusDays(1).atStartOfDay());
        });
    }

    public record DichVuSuDung(String maDV, String tenDV, long soLuong, BigDecimal tienTruocGiam) {

    }

    /**
     * Tổng dịch vụ sau thuế trước voucher; không cộng nhầm giảm giá cả hóa đơn
     * vào mỗi dòng.
     */
    public List<DichVuSuDung> dichVuSuDung(LocalDate tu, LocalDate den) {
        khoangNgay(tu, den);
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            return JdbcHelper.query(c, "SELECT dv.maDV,dv.tenDV,SUM(CAST(ct.soLuong AS BIGINT)),"
                    + "SUM(ct.thanhTien+ROUND(ct.thanhTien*ct.mucThueGTGT/100,0))"
                    + " FROM ChiTietPhieuKham ct JOIN DichVu dv ON dv.maDV=ct.maDV"
                    + " JOIN HoaDon hd ON hd.maPK=ct.maPK"
                    + " WHERE hd.trangThai=1 AND hd.ngayThanhToan>=? AND hd.ngayThanhToan<?"
                    + " GROUP BY dv.maDV,dv.tenDV"
                    + " ORDER BY SUM(CAST(ct.soLuong AS BIGINT)) DESC,dv.maDV",
                    r -> new DichVuSuDung(r.getString(1), r.getString(2), r.getLong(3),
                            r.getBigDecimal(4)), tu.atStartOfDay(), den.plusDays(1).atStartOfDay());
        });
    }

    public record SoSanh(BigDecimal kyTruoc, BigDecimal kySau, BigDecimal chenhLech) {

    }

    public SoSanh soSanh(LocalDate tuTruoc, LocalDate denTruoc,
            LocalDate tuSau, LocalDate denSau) {
        BigDecimal truoc = thongKe(tuTruoc, denTruoc).stream()
                .map(TheoNgay::doanhThu).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sau = thongKe(tuSau, denSau).stream()
                .map(TheoNgay::doanhThu).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SoSanh(truoc, sau, sau.subtract(truoc));
    }

    public record TongQuan(long lichHen, BigDecimal daThu, BigDecimal choThanhToan,
            long nhaSiDangKham) {

    }

    public TongQuan tongQuanTrongNgay(LocalDate ngay) {
        yeuCau(ngay != null, "Thiếu ngày thống kê.");
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            long lich = JdbcHelper.one(c, "SELECT COUNT_BIG(*) FROM LichHen"
                    + " WHERE ngayGioHen>=? AND ngayGioHen<?",
                    r -> r.getLong(1), ngay.atStartOfDay(), ngay.plusDays(1).atStartOfDay());
            BigDecimal paid = JdbcHelper.one(c, TONG
                    + "SELECT COALESCE(SUM(t.tong-hd.tongKhuyenMai),0)"
                    + " FROM HoaDon hd JOIN TienPhieu t ON t.maPK=hd.maPK"
                    + " WHERE hd.trangThai=1 AND hd.ngayThanhToan>=? AND hd.ngayThanhToan<?",
                    r -> r.getBigDecimal(1), ngay.atStartOfDay(), ngay.plusDays(1).atStartOfDay());
            BigDecimal pending = JdbcHelper.one(c, TONG
                    + "SELECT COALESCE(SUM(t.tong-hd.tongKhuyenMai),0)"
                    + " FROM HoaDon hd JOIN TienPhieu t ON t.maPK=hd.maPK"
                    + " WHERE hd.trangThai=0 AND hd.ngayLap>=? AND hd.ngayLap<?",
                    r -> r.getBigDecimal(1), ngay.atStartOfDay(), ngay.plusDays(1).atStartOfDay());
            long busy = JdbcHelper.one(c, "SELECT COUNT(DISTINCT lh.maNV)"
                    + " FROM PhieuKham pk JOIN LichHen lh ON lh.maLH=pk.maLH"
                    + " WHERE pk.trangThai='DANG_KHAM' AND pk.ngayLap>=? AND pk.ngayLap<?",
                    r -> r.getLong(1), ngay.atStartOfDay(), ngay.plusDays(1).atStartOfDay());
            return new TongQuan(lich, paid, pending, busy);
        });
    }

    public record DoanhThuNgay(LocalDate ngay, BigDecimal soTien) {

    }

    public record LichHenTrongNgay(String maLH, LocalDateTime ngayGio, String benhNhan,
            String nhaSi, TrangThaiLichHen trangThai,
            String trangThaiPhieu) {

    }

    public record NhaSiHienTai(String maNV, String hoTen, boolean trongCa, boolean nghiPhep,
            String benhNhanDangKham, LocalDateTime batDauKham,
            String benhNhanTiep, LocalDateTime lichTiep) {

    }

    public record CaHienTai(String caTruc, LocalTime batDau, LocalTime ketThuc) {

    }

    public record TongQuanQuanLy(LocalDateTime capNhatLuc, long tongLichHen, long daDat,
            long daDen, long hoanTat, long daHuy, long khongDen,
            long hoaDonDaTra, BigDecimal daThu, long hoaDonChuaTra,
            BigDecimal conPhaiThu, BigDecimal thuBayNgay,
            BigDecimal thuBayNgayTruoc, List<DoanhThuNgay> bieuDo,
            List<LichHenTrongNgay> lichHen, List<NhaSiHienTai> nhaSi,
            CaHienTai caHienTai) {

        public TongQuanQuanLy {
            bieuDo = List.copyOf(bieuDo);
            lichHen = List.copyOf(lichHen);
            nhaSi = List.copyOf(nhaSi);
        }

        public BigDecimal doanhThuDuKien() {
            return daThu.add(conPhaiThu);
        }

        public Optional<BigDecimal> tangTruong() {
            return tinhTyLeTangTruong(thuBayNgay, thuBayNgayTruoc);
        }
    }

    static Optional<BigDecimal> tinhTyLeTangTruong(BigDecimal moi, BigDecimal cu) {
        if (cu.signum() == 0) {
            return moi.signum() == 0 ? Optional.of(BigDecimal.ZERO) : Optional.empty();
        }
        return Optional.of(moi.subtract(cu).multiply(BigDecimal.valueOf(100))
                .divide(cu, 1, RoundingMode.HALF_UP));
    }

    static List<DoanhThuNgay> boSungNgayKhongPhatSinh(LocalDate ngay, List<DoanhThuNgay> rows) {
        Map<LocalDate, BigDecimal> byDate = new HashMap<>();
        for (DoanhThuNgay row : rows) {
            byDate.merge(row.ngay(), row.soTien(), BigDecimal::add);
        }
        List<DoanhThuNgay> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = ngay.minusDays(i);
            result.add(new DoanhThuNgay(d, byDate.getOrDefault(d, BigDecimal.ZERO)));
        }
        return List.copyOf(result);
    }

    /**
     * không thay trạng thái lịch khi tải dashboard.
     */
    public TongQuanQuanLy layTongQuanQuanLy() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate ngay = now.toLocalDate();
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.QUANLY);

            List<LichHenTrongNgay> appointments = JdbcHelper.query(c, """
                    SELECT lh.maLH,lh.ngayGioHen,kh.tenKH,nv.hoTenNV,lh.trangThai,pk.trangThai
                    FROM LichHen lh JOIN KhachHang kh ON kh.maKH=lh.maKH
                    JOIN NhanVien nv ON nv.maNV=lh.maNV LEFT JOIN PhieuKham pk ON pk.maLH=lh.maLH
                    WHERE lh.ngayGioHen>=? AND lh.ngayGioHen<? ORDER BY lh.ngayGioHen,lh.maLH
                    """, r -> new LichHenTrongNgay(r.getString(1),
                    r.getObject(2, LocalDateTime.class), r.getString(3), r.getString(4),
                    TrangThaiLichHen.valueOf(r.getString(5)), r.getString(6)),
                    ngay.atStartOfDay(), ngay.plusDays(1).atStartOfDay());

            record HoaDonTong(long daTra, BigDecimal daThu, long chuaTra, BigDecimal conLai) {

            }
            HoaDonTong invoices = JdbcHelper.one(c, TONG + """
                    SELECT COALESCE(SUM(CASE WHEN hd.trangThai=1 AND hd.ngayThanhToan>=? AND hd.ngayThanhToan<=? THEN CAST(1 AS BIGINT) ELSE 0 END),0),
                           COALESCE(SUM(CASE WHEN hd.trangThai=1 AND hd.ngayThanhToan>=? AND hd.ngayThanhToan<=? THEN tien.net ELSE 0 END),0),
                           COALESCE(SUM(CASE WHEN hd.trangThai=0 AND hd.ngayLap<=? THEN CAST(1 AS BIGINT) ELSE 0 END),0),
                           COALESCE(SUM(CASE WHEN hd.trangThai=0 AND hd.ngayLap<=? THEN tien.net ELSE 0 END),0)
                    FROM HoaDon hd LEFT JOIN TienPhieu t ON t.maPK=hd.maPK
                    CROSS APPLY(SELECT CASE WHEN COALESCE(t.tong,0)>hd.tongKhuyenMai
                        THEN COALESCE(t.tong,0)-hd.tongKhuyenMai ELSE 0 END AS net) tien
                    """, r -> new HoaDonTong(r.getLong(1), r.getBigDecimal(2),
                    r.getLong(3), r.getBigDecimal(4)),
                    ngay.atStartOfDay(), now, ngay.atStartOfDay(), now, now, now);

            List<DoanhThuNgay> fourteen = JdbcHelper.query(c, TONG + """
                    SELECT CAST(hd.ngayThanhToan AS DATE),SUM(CASE WHEN t.tong>hd.tongKhuyenMai THEN t.tong-hd.tongKhuyenMai ELSE 0 END)
                    FROM HoaDon hd JOIN TienPhieu t ON t.maPK=hd.maPK
                    WHERE hd.trangThai=1 AND hd.ngayThanhToan>=? AND hd.ngayThanhToan<=?
                    GROUP BY CAST(hd.ngayThanhToan AS DATE) ORDER BY CAST(hd.ngayThanhToan AS DATE)
                    """, r -> new DoanhThuNgay(r.getObject(1, LocalDate.class), r.getBigDecimal(2)),
                    ngay.minusDays(13).atStartOfDay(), now);

            List<DoanhThuNgay> chart = boSungNgayKhongPhatSinh(ngay, fourteen);
            BigDecimal total = chart.stream().map(DoanhThuNgay::soTien)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal previous = fourteen.stream().filter(d -> d.ngay().isBefore(ngay.minusDays(6)))
                    .map(DoanhThuNgay::soTien).reduce(BigDecimal.ZERO, BigDecimal::add);

            List<NhaSiHienTai> doctors = JdbcHelper.query(c, """
                    SELECT nv.maNV,nv.hoTenNV,
                        CASE WHEN EXISTS(SELECT 1 FROM LichLamViec ll WHERE ll.maNV=nv.maNV AND ll.ngayTruc=?
                            AND ll.gioBatDau<=? AND ll.gioKetThuc>? AND ll.trangThai='DA_PHAN_CONG') THEN 1 ELSE 0 END,
                        CASE WHEN EXISTS(SELECT 1 FROM LichLamViec ll WHERE ll.maNV=nv.maNV AND ll.ngayTruc=?
                            AND ll.gioBatDau<=? AND ll.gioKetThuc>? AND ll.trangThai='NGHI_PHEP') THEN 1 ELSE 0 END,
                        active.tenKH,active.ngayLap,nextVisit.tenKH,nextVisit.ngayGioHen
                    FROM NhanVien nv JOIN TaiKhoan tk ON tk.maNV=nv.maNV
                    OUTER APPLY(SELECT TOP 1 kh.tenKH,pk.ngayLap FROM PhieuKham pk
                        JOIN LichHen lh ON lh.maLH=pk.maLH JOIN KhachHang kh ON kh.maKH=lh.maKH
                        WHERE lh.maNV=nv.maNV AND pk.trangThai='DANG_KHAM' AND pk.ngayLap<=?
                        ORDER BY pk.ngayLap,pk.maPK) active
                    OUTER APPLY(SELECT TOP 1 kh.tenKH,lh.ngayGioHen FROM LichHen lh JOIN KhachHang kh ON kh.maKH=lh.maKH
                        WHERE lh.maNV=nv.maNV AND lh.trangThai='DA_DAT' AND lh.ngayGioHen>=? AND lh.ngayGioHen<?
                        ORDER BY lh.ngayGioHen,lh.maLH) nextVisit
                    WHERE tk.vaiTro='NHASI' AND tk.trangThai=1 ORDER BY nv.hoTenNV,nv.maNV
                    """, r -> new NhaSiHienTai(r.getString(1), r.getString(2),
                    r.getBoolean(3), r.getBoolean(4), r.getString(5),
                    r.getObject(6, LocalDateTime.class), r.getString(7),
                    r.getObject(8, LocalDateTime.class)),
                    ngay, now.toLocalTime(), now.toLocalTime(),
                    ngay, now.toLocalTime(), now.toLocalTime(),
                    now, now, ngay.plusDays(1).atStartOfDay());

            CaHienTai shift = JdbcHelper.one(c, """
                    SELECT TOP 1 caTruc,gioBatDau,gioKetThuc FROM LichLamViec WHERE maNV=? AND ngayTruc=?
                    AND gioBatDau<=? AND gioKetThuc>? AND trangThai='DA_PHAN_CONG' ORDER BY gioBatDau
                    """, r -> new CaHienTai(r.getString(1),
                    r.getObject(2, LocalTime.class), r.getObject(3, LocalTime.class)),
                    actor.maNV(), ngay, now.toLocalTime(), now.toLocalTime());

            long[] counts = new long[TrangThaiLichHen.values().length];
            for (LichHenTrongNgay appointment : appointments) {
                counts[appointment.trangThai().ordinal()]++;
            }
            return new TongQuanQuanLy(now, appointments.size(),
                    counts[TrangThaiLichHen.DA_DAT.ordinal()],
                    counts[TrangThaiLichHen.DA_DEN.ordinal()],
                    counts[TrangThaiLichHen.HOAN_TAT.ordinal()],
                    counts[TrangThaiLichHen.DA_HUY.ordinal()],
                    counts[TrangThaiLichHen.KHONG_DEN.ordinal()],
                    invoices.daTra(), invoices.daThu(), invoices.chuaTra(), invoices.conLai(),
                    total, previous, chart, appointments, doctors, shift);
        });
    }

    private static void khoangNgay(LocalDate tu, LocalDate den) {
        yeuCau(tu != null && den != null && !den.isBefore(tu), "Khoảng ngày không hợp lệ.");
    }

    private static void yeuCau(boolean dieuKien, String message) {
        if (!dieuKien) {
            throw new BusinessException(message);
        }
    }

    // =====================================================================================================
    // Báo cáo doanh thu (Dương) — CHỈ THÊM MỚI bên dưới, không sửa code phía trên.
    // Phía trên còn phần Tổng quan quản lý của Hiếu, sẽ được tách sang TongQuanQuanLyDAO; giữ nguyên để
    // khi tách không bị xung đột. Các record/hàm dưới đây không dùng record nào của phần Tổng quan.
    //
    // Tiền của 1 hóa đơn (giống HoaDonDAO và quy tắc R-HD-1):
    //   tiền hàng = Σ thành tiền các dòng chi tiết
    //   thuế GTGT = Σ ROUND(thành tiền × mức thuế / 100, 0)        (làm tròn từng dòng như HoaDonDAO)
    //   thực nhận = tiền hàng + thuế − tongKhuyenMai               (= số tiền khách trả)
    // =====================================================================================================

    /**
     * CTE "HD": mỗi hóa đơn một dòng, kèm tiền hàng, thuế, giảm giá, thực nhận và tên khách.
     * LEFT JOIN để hóa đơn chưa có dòng chi tiết vẫn có mặt (tiền = 0).
     */
    private static final String CTE_HOA_DON = """
            WITH TienHD AS (
                SELECT maPK, SUM(thanhTien) AS tienHang,
                       SUM(ROUND(thanhTien * mucThueGTGT / 100, 0)) AS thue
                FROM ChiTietPhieuKham GROUP BY maPK
            ), HD AS (
                SELECT hd.maHD, hd.ngayLap, hd.ngayThanhToan, hd.trangThai,
                       hd.hinhThucThanhToan AS hinhThuc, kh.tenKH,
                       COALESCE(t.tienHang, 0) AS tienHang, COALESCE(t.thue, 0) AS thue,
                       hd.tongKhuyenMai AS giam,
                       COALESCE(t.tienHang, 0) + COALESCE(t.thue, 0) - hd.tongKhuyenMai AS thucNhan
                FROM HoaDon hd
                JOIN PhieuKham pk ON pk.maPK = hd.maPK
                JOIN HoSoBenhAn hs ON hs.maHSBA = pk.maHSBA
                JOIN KhachHang kh ON kh.maKH = hs.maKH
                LEFT JOIN TienHD t ON t.maPK = hd.maPK
            )
            """;

    /**
     * Tổng hợp doanh thu của một kỳ (trang Báo cáo của Quản lý).
     * {@code tienHang + thueGTGT − giamGia = thucNhan = tienMat + chuyenKhoan}.
     */
    public record TongHopDoanhThu(long soHoaDon, BigDecimal tienHang, BigDecimal thueGTGT, BigDecimal giamGia,
            BigDecimal thucNhan, BigDecimal tienMat, BigDecimal chuyenKhoan) {

        /** Kỳ không có hóa đơn nào. */
        public static final TongHopDoanhThu RONG = new TongHopDoanhThu(0, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /**
     * Tổng hợp các hóa đơn ĐÃ THANH TOÁN có ngày thanh toán trong [tu, den] (tính cả 2 đầu).
     * Chỉ Quản lý. Không có hóa đơn thì trả {@link TongHopDoanhThu#RONG} (số hóa đơn = 0).
     */
    public TongHopDoanhThu tongHopDoanhThu(LocalDate tu, LocalDate den) {
        kiemTraKhoang(tu, den);
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            return JdbcHelper.one(c, CTE_HOA_DON + """
                    SELECT COUNT_BIG(*), COALESCE(SUM(tienHang), 0), COALESCE(SUM(thue), 0),
                           COALESCE(SUM(giam), 0), COALESCE(SUM(thucNhan), 0),
                           COALESCE(SUM(CASE WHEN hinhThuc = ? THEN thucNhan END), 0),
                           COALESCE(SUM(CASE WHEN hinhThuc = ? THEN thucNhan END), 0)
                    FROM HD
                    WHERE trangThai = 1 AND ngayThanhToan >= ? AND ngayThanhToan < ?
                    """,
                    r -> new TongHopDoanhThu(r.getLong(1), r.getBigDecimal(2), r.getBigDecimal(3),
                            r.getBigDecimal(4), r.getBigDecimal(5), r.getBigDecimal(6), r.getBigDecimal(7)),
                    HoaDon.TIEN_MAT, HoaDon.CHUYEN_KHOAN, tu.atStartOfDay(), den.plusDays(1).atStartOfDay());
        });
    }

    /** Doanh thu thực nhận của một ngày (điểm trên biểu đồ "Doanh thu theo ngày"). */
    public record DoanhThuMotNgay(LocalDate ngay, BigDecimal thucNhan) {

    }

    /**
     * Doanh thu thực nhận TỪNG NGÀY trong [tu, den], đủ mọi ngày (ngày không có hóa đơn = 0) để vẽ biểu đồ
     * liền mạch. Chỉ hóa đơn đã thanh toán, theo ngày thanh toán. Chỉ Quản lý.
     */
    public List<DoanhThuMotNgay> doanhThuTungNgay(LocalDate tu, LocalDate den) {
        kiemTraKhoang(tu, den);
        Map<LocalDate, BigDecimal> theoNgay = JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            Map<LocalDate, BigDecimal> m = new HashMap<>();
            JdbcHelper.query(c, CTE_HOA_DON + """
                    SELECT CAST(ngayThanhToan AS DATE), SUM(thucNhan)
                    FROM HD
                    WHERE trangThai = 1 AND ngayThanhToan >= ? AND ngayThanhToan < ?
                    GROUP BY CAST(ngayThanhToan AS DATE)
                    """,
                    r -> m.put(r.getObject(1, LocalDate.class), r.getBigDecimal(2)),
                    tu.atStartOfDay(), den.plusDays(1).atStartOfDay());
            return m;
        });
        List<DoanhThuMotNgay> ketQua = new ArrayList<>();
        for (LocalDate d = tu; !d.isAfter(den); d = d.plusDays(1)) {
            ketQua.add(new DoanhThuMotNgay(d, theoNgay.getOrDefault(d, BigDecimal.ZERO)));
        }
        return ketQua;
    }

    /** Một dòng hóa đơn trên trang doanh thu trong ngày của Lễ tân. {@code hinhThuc} null khi chưa thanh toán. */
    public record HoaDonTrongNgay(String maHD, LocalDateTime ngayLap, String tenKH, boolean daThanhToan,
            String hinhThuc, BigDecimal tongTien) {

    }

    /**
     * Doanh thu trong một ngày (trang của Lễ tân): mọi hóa đơn LẬP trong ngày, gồm cả chưa thanh toán
     * ("doanh thu tạm tính" theo báo cáo). Các tổng được cộng từ danh sách để luôn khớp với bảng.
     */
    public record DoanhThuTrongNgay(LocalDate ngay, List<HoaDonTrongNgay> hoaDon) {

        public DoanhThuTrongNgay {
            hoaDon = List.copyOf(hoaDon);
        }

        public int soHoaDon() {
            return hoaDon.size();
        }

        /** Tổng tiền mọi hóa đơn đã lập trong ngày (cả chưa thanh toán). */
        public BigDecimal tamTinh() {
            return cong(h -> true);
        }

        public BigDecimal daThu() {
            return cong(HoaDonTrongNgay::daThanhToan);
        }

        public BigDecimal chuaThu() {
            return cong(h -> !h.daThanhToan());
        }

        public BigDecimal tienMat() {
            return cong(h -> h.daThanhToan() && HoaDon.TIEN_MAT.equals(h.hinhThuc()));
        }

        public BigDecimal chuyenKhoan() {
            return cong(h -> h.daThanhToan() && HoaDon.CHUYEN_KHOAN.equals(h.hinhThuc()));
        }

        private BigDecimal cong(java.util.function.Predicate<HoaDonTrongNgay> dieuKien) {
            return hoaDon.stream().filter(dieuKien).map(HoaDonTrongNgay::tongTien)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    /**
     * Hóa đơn lập trong ngày {@code ngay} (theo ngày lập, gồm cả chưa thanh toán), mới nhất ở trên.
     * Lễ tân và Quản lý đều xem được.
     */
    public DoanhThuTrongNgay doanhThuTrongNgay(LocalDate ngay) {
        yeuCau(ngay != null, "Vui lòng chọn ngày xem.");
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c, VaiTro.LETAN, VaiTro.QUANLY);
            List<HoaDonTrongNgay> ds = JdbcHelper.query(c, CTE_HOA_DON + """
                    SELECT maHD, ngayLap, tenKH, trangThai, hinhThuc, thucNhan
                    FROM HD
                    WHERE ngayLap >= ? AND ngayLap < ?
                    ORDER BY ngayLap DESC, maHD DESC
                    """,
                    r -> new HoaDonTrongNgay(r.getString(1), r.getObject(2, LocalDateTime.class), r.getString(3),
                            r.getBoolean(4), r.getBoolean(4) ? r.getString(5) : null, r.getBigDecimal(6)),
                    ngay.atStartOfDay(), ngay.plusDays(1).atStartOfDay());
            return new DoanhThuTrongNgay(ngay, ds);
        });
    }

    /** Khoảng ngày cho các hàm báo cáo mới (thông báo rõ hơn hàm khoangNgay phía trên). */
    private static void kiemTraKhoang(LocalDate tu, LocalDate den) {
        yeuCau(tu != null && den != null, "Vui lòng chọn đủ ngày bắt đầu và ngày kết thúc.");
        yeuCau(!tu.isAfter(den), "Khoảng thời gian không hợp lệ: ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
    }
}
