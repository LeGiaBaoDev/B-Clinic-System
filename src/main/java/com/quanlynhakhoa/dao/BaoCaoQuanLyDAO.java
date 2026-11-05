package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.dao.support.DocThongKe;
import com.quanlynhakhoa.dao.support.PhanQuyen;
import com.quanlynhakhoa.entity.HoaDon;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Báo cáo tổng hợp chỉ đọc cho Quản lý. Không thay đổi trạng thái nghiệp vụ. */
public class BaoCaoQuanLyDAO {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final Clock dongHo;

    public BaoCaoQuanLyDAO() { this(Clock.system(ZONE)); }
    public BaoCaoQuanLyDAO(Clock dongHo) { this.dongHo = Objects.requireNonNull(dongHo).withZone(ZONE); }

    public enum CheDo { NGAY, TUAN, THANG, TUY_CHON }

    public record KhoangNgay(LocalDate tu, LocalDate den) {
        public KhoangNgay {
            kiemTra(tu != null && den != null, "Vui lòng chọn đủ ngày bắt đầu và kết thúc.");
            kiemTra(tu.getYear() >= 1900 && den.getYear() <= 9998 && !den.isBefore(tu), "Khoảng ngày không hợp lệ.");
            kiemTra(ChronoUnit.DAYS.between(tu, den) < 366, "Mỗi lần xem tối đa 366 ngày.");
        }
        public int soNgay() { return (int) ChronoUnit.DAYS.between(tu, den) + 1; }
    }

    public record KyBaoCao(CheDo cheDo, KhoangNgay hienTai, KhoangNgay kyTruoc) {
        public KyBaoCao {
            Objects.requireNonNull(cheDo); Objects.requireNonNull(hienTai); Objects.requireNonNull(kyTruoc);
            kiemTra(kyTruoc.den().isBefore(hienTai.tu()), "Hai kỳ so sánh không được chồng nhau.");
        }
        public static KyBaoCao tao(CheDo cheDo, LocalDate moc, LocalDate den) {
            kiemTra(cheDo != null && moc != null, "Vui lòng chọn ngày xem báo cáo.");
            kiemTra(moc.getYear() >= 1901 && moc.getYear() <= 9998, "Ngày xem phải từ năm 1901 đến 9998.");
            LocalDate tu = switch (cheDo) {
                case TUAN -> moc.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                case THANG -> moc.withDayOfMonth(1);
                default -> moc;
            };
            LocalDate end = switch (cheDo) {
                case NGAY -> tu;
                case TUAN -> tu.plusDays(6);
                case THANG -> tu.with(TemporalAdjusters.lastDayOfMonth());
                case TUY_CHON -> den;
            };
            KhoangNgay current = new KhoangNgay(tu, end);
            KhoangNgay previous = cheDo == CheDo.THANG
                    ? new KhoangNgay(tu.minusMonths(1), tu.minusDays(1))
                    : new KhoangNgay(tu.minusDays(current.soNgay()), tu.minusDays(1));
            return new KyBaoCao(cheDo, current, previous);
        }
    }

    public record DoanhThuNgay(LocalDate ngay, long soHoaDon, BigDecimal doanhThu,
            BigDecimal tienMat, BigDecimal chuyenKhoan) { }

    public record KetQuaLich(long tong, long daDat, long daDen, long hoanTat, long daHuy, long khongDen) {
        public static final KetQuaLich TRONG = new KetQuaLich(0, 0, 0, 0, 0, 0);
        public long daKetThuc() { return hoanTat + daHuy + khongDen; }
        public double tyLeHoanTat() { return daKetThuc() == 0 ? 0 : 100.0 * hoanTat / daKetThuc(); }
        KetQuaLich cong(KetQuaLich other) {
            return new KetQuaLich(tong + other.tong, daDat + other.daDat, daDen + other.daDen,
                    hoanTat + other.hoanTat, daHuy + other.daHuy, khongDen + other.khongDen);
        }
    }

    public record DichVuSuDung(String maDV, String tenDV, long soLuong, BigDecimal tienTruocGiam) { }

    public record BaoCao(KyBaoCao ky, LocalDateTime capNhatLuc, List<DoanhThuNgay> hienTai,
            List<DoanhThuNgay> kyTruoc, KetQuaLich lichHienTai, KetQuaLich lichKyTruoc,
            List<DichVuSuDung> dichVu) {
        public BaoCao {
            Objects.requireNonNull(ky); Objects.requireNonNull(capNhatLuc);
            hienTai = List.copyOf(hienTai); kyTruoc = List.copyOf(kyTruoc); dichVu = List.copyOf(dichVu);
            Objects.requireNonNull(lichHienTai); Objects.requireNonNull(lichKyTruoc);
        }
        public BigDecimal doanhThu() { return hienTai.stream().map(DoanhThuNgay::doanhThu).reduce(BigDecimal.ZERO, BigDecimal::add); }
        public BigDecimal doanhThuTruoc() { return kyTruoc.stream().map(DoanhThuNgay::doanhThu).reduce(BigDecimal.ZERO, BigDecimal::add); }
        public BigDecimal tienMat() { return hienTai.stream().map(DoanhThuNgay::tienMat).reduce(BigDecimal.ZERO, BigDecimal::add); }
        public BigDecimal chuyenKhoan() { return hienTai.stream().map(DoanhThuNgay::chuyenKhoan).reduce(BigDecimal.ZERO, BigDecimal::add); }
        public Optional<BigDecimal> tangTruong() {
            BigDecimal old = doanhThuTruoc(), current = doanhThu();
            return old.signum() == 0 ? (current.signum() == 0 ? Optional.of(BigDecimal.ZERO) : Optional.empty())
                    : Optional.of(current.subtract(old).multiply(BigDecimal.valueOf(100)).divide(old, 1, RoundingMode.HALF_UP));
        }
    }

    private static final String TONG = """
            WITH TienPhieu AS (
                SELECT maPK,SUM(thanhTien+ROUND(thanhTien*mucThueGTGT/100,0)) AS tong
                FROM ChiTietPhieuKham GROUP BY maPK
            )
            """;

    public BaoCao taiBaoCao(KyBaoCao ky) {
        kiemTra(ky != null, "Thiếu kỳ báo cáo.");
        return DocThongKe.quanLy(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            KhoangNgay current = ky.hienTai(), previous = ky.kyTruoc();
            var daily = JdbcHelper.query(c, TONG + """
                    SELECT CAST(hd.ngayThanhToan AS DATE),COUNT_BIG(*),
                           SUM(tien.net),
                           SUM(CASE WHEN hd.hinhThucThanhToan=? THEN tien.net ELSE 0 END),
                           SUM(CASE WHEN hd.hinhThucThanhToan=? THEN tien.net ELSE 0 END)
                    FROM HoaDon hd LEFT JOIN TienPhieu t ON t.maPK=hd.maPK
                    CROSS APPLY(SELECT CASE WHEN COALESCE(t.tong,0)>hd.tongKhuyenMai
                        THEN COALESCE(t.tong,0)-hd.tongKhuyenMai ELSE 0 END AS net) tien
                    WHERE hd.trangThai=1 AND hd.ngayThanhToan>=? AND hd.ngayThanhToan<?
                    GROUP BY CAST(hd.ngayThanhToan AS DATE) ORDER BY CAST(hd.ngayThanhToan AS DATE)
                    """, r -> new DoanhThuNgay(r.getObject(1, LocalDate.class), r.getLong(2),
                            r.getBigDecimal(3), r.getBigDecimal(4), r.getBigDecimal(5)),
                    HoaDon.TIEN_MAT, HoaDon.CHUYEN_KHOAN, previous.tu().atStartOfDay(), current.den().plusDays(1).atStartOfDay());
            record LichNgay(LocalDate ngay, KetQuaLich soLieu) { }
            var appointments = JdbcHelper.query(c, """
                    SELECT CAST(ngayGioHen AS DATE),COUNT_BIG(*),
                        SUM(CASE WHEN trangThai='DA_DAT' THEN CAST(1 AS BIGINT) ELSE 0 END),
                        SUM(CASE WHEN trangThai='DA_DEN' THEN CAST(1 AS BIGINT) ELSE 0 END),
                        SUM(CASE WHEN trangThai='HOAN_TAT' THEN CAST(1 AS BIGINT) ELSE 0 END),
                        SUM(CASE WHEN trangThai='DA_HUY' THEN CAST(1 AS BIGINT) ELSE 0 END),
                        SUM(CASE WHEN trangThai='KHONG_DEN' THEN CAST(1 AS BIGINT) ELSE 0 END)
                    FROM LichHen WHERE ngayGioHen>=? AND ngayGioHen<? GROUP BY CAST(ngayGioHen AS DATE)
                    """, r -> new LichNgay(r.getObject(1, LocalDate.class), new KetQuaLich(r.getLong(2),
                            r.getLong(3), r.getLong(4), r.getLong(5), r.getLong(6), r.getLong(7))),
                    previous.tu().atStartOfDay(), current.den().plusDays(1).atStartOfDay());
            var services = JdbcHelper.query(c, """
                    SELECT dv.maDV,dv.tenDV,SUM(CAST(ct.soLuong AS BIGINT)),
                           SUM(ct.thanhTien+ROUND(ct.thanhTien*ct.mucThueGTGT/100,0))
                    FROM ChiTietPhieuKham ct JOIN DichVu dv ON dv.maDV=ct.maDV
                    JOIN HoaDon hd ON hd.maPK=ct.maPK
                    WHERE hd.trangThai=1 AND hd.ngayThanhToan>=? AND hd.ngayThanhToan<?
                    GROUP BY dv.maDV,dv.tenDV ORDER BY SUM(CAST(ct.soLuong AS BIGINT)) DESC,dv.maDV
                    """, r -> new DichVuSuDung(r.getString(1), r.getString(2), r.getLong(3), r.getBigDecimal(4)),
                    current.tu().atStartOfDay(), current.den().plusDays(1).atStartOfDay());
            var result = new BaoCao(ky, LocalDateTime.now(dongHo), boSungNgay(current, daily), boSungNgay(previous, daily),
                    appointments.stream().filter(row -> thuoc(row.ngay(), current)).map(LichNgay::soLieu).reduce(KetQuaLich.TRONG, KetQuaLich::cong),
                    appointments.stream().filter(row -> thuoc(row.ngay(), previous)).map(LichNgay::soLieu).reduce(KetQuaLich.TRONG, KetQuaLich::cong), services);
            return result;
        });
    }

    static List<DoanhThuNgay> boSungNgay(KhoangNgay range, List<DoanhThuNgay> rows) {
        Map<LocalDate, DoanhThuNgay> byDate = new HashMap<>();
        for (DoanhThuNgay row : rows) byDate.put(row.ngay(), row);
        return range.tu().datesUntil(range.den().plusDays(1)).map(day -> byDate.getOrDefault(day,
                new DoanhThuNgay(day, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))).toList();
    }
    private static boolean thuoc(LocalDate day, KhoangNgay range) { return !day.isBefore(range.tu()) && !day.isAfter(range.den()); }
    private static void kiemTra(boolean condition, String message) { if (!condition) throw new BusinessException(message); }
}
