package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.dao.support.PhanQuyen;
import com.quanlynhakhoa.dao.support.PhanQuyen.Actor;
import com.quanlynhakhoa.entity.enums.TrangThaiLichHen;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/** UC035: dữ liệu tổng quan Quản lý, tách riêng khỏi báo cáo doanh thu. */
public class TongQuanQuanLyDAO {

    private static final String TONG = """
            WITH TienPhieu AS (
                SELECT maPK,SUM(thanhTien+ROUND(thanhTien*mucThueGTGT/100,0)) AS tong
                FROM ChiTietPhieuKham GROUP BY maPK
            )
            """;

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

    private static void yeuCau(boolean dieuKien, String message) {
        if (!dieuKien) {
            throw new BusinessException(message);
        }
    }
}
