package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.dao.support.MaSinh;
import com.quanlynhakhoa.dao.support.NhatKy;
import com.quanlynhakhoa.dao.support.PhanQuyen;
import com.quanlynhakhoa.dao.support.PhanQuyen.Actor;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import com.quanlynhakhoa.exception.BusinessException;
import java.math.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class HoaDonDAO {

    public record DongDanhSach(HoaDon hoaDon, KhachHang khachHang, BigDecimal tongThanhToan) { }

    /** Dữ liệu danh sách: một truy vấn, chỉ tải chi tiết khi mở hóa đơn. */
    public List<DongDanhSach> layDanhSach(LocalDate tu, LocalDate den, Boolean daTra) {
        khoangNgay(tu, den);
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY, VaiTro.LETAN);
            return JdbcHelper.query(c, """
                    SELECT hd.maHD,hd.ngayLap,hd.trangThai,hd.tongKhuyenMai,hd.tongTienHang,hd.tongThueGTGT,hd.tongThanhToan,hd.tongDaThu,
                           hd.tenKhachHang,hd.sdtKhachHang,hd.diaChiKhachHang,
                           kh.maKH,kh.tenKH,kh.ngaySinh,kh.gioiTinh,kh.sdt,kh.diaChi,kh.email,
                           hs.maHSBA,pk.maPK,
                           hd.tongThanhToan AS tong
                    FROM HoaDon hd
                    JOIN PhieuKham pk ON pk.maPK=hd.maPK
                    JOIN HoSoBenhAn hs ON hs.maHSBA=pk.maHSBA
                    JOIN KhachHang kh ON kh.maKH=hs.maKH
                    WHERE hd.ngayLap>=? AND hd.ngayLap<? AND (? IS NULL OR hd.trangThai=?)
                    ORDER BY hd.ngayLap DESC
                    """, r -> {
                KhachHang kh = KhachHang.tuDuLieu(r.getString("maKH"), r.getString("tenKH"),
                        r.getObject("ngaySinh", LocalDate.class), r.getBoolean("gioiTinh"),
                        r.getString("sdt"), r.getString("diaChi"), r.getString("email"));
                PhieuKham pk = new PhieuKham();
                pk.setMaPK(r.getString("maPK"));
                pk.setHoSoBenhAn(new HoSoBenhAn(r.getString("maHSBA"), null, kh));
                HoaDon hd = new HoaDon();
                hd.setMaHD(r.getString("maHD"));
                hd.setNgayLap(r.getObject("ngayLap", LocalDateTime.class));
                hd.setTongKhuyenMai(r.getBigDecimal("tongKhuyenMai"));
                hd.setPhieuKham(pk);
                hd.setTrangThai(r.getBoolean("trangThai"));
                hd=HoaDon.tuDuLieuTaiChinh(hd,kh,r.getString("tenKhachHang"),r.getString("sdtKhachHang"),r.getString("diaChiKhachHang"),
                        r.getBigDecimal("tongTienHang"),r.getBigDecimal("tongThueGTGT"),r.getBigDecimal("tongKhuyenMai"),
                        r.getBigDecimal("tongThanhToan"),r.getBigDecimal("tongDaThu"));
                return new DongDanhSach(hd, kh, r.getBigDecimal("tong"));
            }, tu.atStartOfDay(), den.plusDays(1).atStartOfDay(), daTra, daTra);
        });
    }

    private static final String SQL_SELECT = """
            SELECT maHD, ngayLap, trangThai, hinhThucThanhToan, ngayThanhToan, tongKhuyenMai,
                   maPK, maVoucher, maNV_ThuNgan, maKH, tenKhachHang, sdtKhachHang, diaChiKhachHang,
                   tongTienHang, tongThueGTGT, tongThanhToan, tongDaThu
            FROM HoaDon
            """;

    public List<HoaDon> layTatCa() {
        return timKiem(LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1), null);
    }

    public List<HoaDon> timKiem(LocalDate tu, LocalDate den, Boolean daTra) {
        khoangNgay(tu, den);
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.QUANLY, VaiTro.LETAN);
            return JdbcHelper.query(c, SQL_SELECT
                    + " WHERE ngayLap>=? AND ngayLap<? AND (? IS NULL OR trangThai=?)"
                    + " ORDER BY ngayLap DESC",
                    r -> loc(mapRow(c, r), actor.vaiTro()),
                    tu.atStartOfDay(), den.plusDays(1).atStartOfDay(), daTra, daTra);
        });
    }

    public HoaDon layTheoMa(String ma) {
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.QUANLY, VaiTro.LETAN);
            return loc(layTheoMa(c, ma), actor.vaiTro());
        });
    }

    public HoaDon layTheoPhieuKham(String maPK) {
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.QUANLY, VaiTro.LETAN);
            return loc(JdbcHelper.one(c, SQL_SELECT + " WHERE maPK=?",
                    r -> mapRow(c, r), maPK), actor.vaiTro());
        });
    }

    private HoaDon layTheoMa(Connection c, String ma) throws SQLException {
        return JdbcHelper.one(c, SQL_SELECT + " WHERE maHD=?",
                r -> mapRow(c, r), ma);
    }

    String taoTuPhieuKham(Connection c, String maPK) throws SQLException {
        yeuCau(JdbcHelper.exists(c, "SELECT 1 FROM PhieuKham WHERE maPK=?"
                + " AND trangThai='HOAN_TAT'", maPK), "Phiếu khám chưa hoàn tất.");
        yeuCau(!JdbcHelper.exists(c, "SELECT 1 FROM HoaDon WITH(UPDLOCK,HOLDLOCK) WHERE maPK=?",
                maPK), "Phiếu khám đã có hóa đơn.");

        String ma = MaSinh.maMoi(c, MaSinh.Bang.HOA_DON, LocalDate.now());
        JdbcHelper.update(c, """
                INSERT INTO HoaDon(maHD,maPK,ngayLap,maKH,tenKhachHang,sdtKhachHang,diaChiKhachHang)
                SELECT ?,p.maPK,?,k.maKH,k.tenKH,k.sdt,k.diaChi FROM PhieuKham p
                JOIN HoSoBenhAn h ON h.maHSBA=p.maHSBA JOIN KhachHang k ON k.maKH=h.maKH
                WHERE p.maPK=?
                """,ma,LocalDateTime.now(),maPK);
        JdbcHelper.update(c, """
                INSERT ChiTietHoaDon(maHD,maPK,maCTPK,maDV,tenDichVu,donViTinh,soLuong,donGia,
                    loaiGTGT,thueSuatGTGT,tienHang,tienGTGT,tienGiam,thanhTien)
                SELECT ?,ct.maPK,ct.maCTPK,ct.maDV,d.tenDV,ct.donViTinh,ct.soLuong,ct.donGia,
                    ct.loaiGTGT,CASE WHEN ct.loaiGTGT=N'KHONG_CHIU_THUE' THEN NULL ELSE ct.mucThueGTGT END,
                    ct.thanhTien,ROUND(ct.thanhTien*ct.mucThueGTGT/100,0),0,
                    ct.thanhTien+ROUND(ct.thanhTien*ct.mucThueGTGT/100,0)
                FROM ChiTietPhieuKham ct JOIN DichVu d ON d.maDV=ct.maDV WHERE ct.maPK=?
                """,ma,maPK);
        return ma;
    }

    static BigDecimal tongTruocGiam(Connection c, String maPK) throws SQLException {
        return JdbcHelper.one(c, "SELECT COALESCE(SUM(thanhTien"
                + "+ROUND(thanhTien*mucThueGTGT/100,0)),0)"
                + " FROM ChiTietPhieuKham WHERE maPK=?",
                r -> r.getBigDecimal(1), maPK);
    }

    public static BigDecimal tinhTienGiam(Voucher vc, BigDecimal tong) {
        return tienGiam(vc, tong);
    }

    static BigDecimal tienGiam(Voucher vc, BigDecimal tong) {
        BigDecimal giam = vc.getGiaGiamChinhXac();
        if (vc.isLoaiGiam()) {
            giam = tong.multiply(giam).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (vc.getGiaTriGiamToiDa() > 0) {
                giam = giam.min(vc.getGiaTriGiamToiDaChinhXac());
            }
        }
        return giam.min(tong).max(BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP);
    }

    public BigDecimal tinhTongTien(String maHD) {
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c,VaiTro.QUANLY,VaiTro.LETAN);
            BigDecimal tong=JdbcHelper.one(c,"SELECT tongThanhToan FROM HoaDon WHERE maHD=?",r->r.getBigDecimal(1),maHD);
            yeuCau(tong!=null,"Hóa đơn không tồn tại.");return tong;
        });
    }

    /** Mã chỉ được áp cho dịch vụ được cấu hình; vẫn chọn phương án tốt nhất. */
    public boolean apDungVoucher(String maHD,String maVoucher) {
        return JdbcHelper.transaction(c->{
            PhanQuyen.quyen(c,VaiTro.QUANLY,VaiTro.LETAN);
            MaSinh.khoa(c,"BClinic:thu:"+maHD);
            KhuyenMaiHoaDon.apDung(c,maHD,maVoucher);
            NhatKy.ghi(c,"AP_DUNG_VOUCHER",maHD);return true;
        });
    }
    public BigDecimal thanhToan(String maHD,String hinhThuc,BigDecimal soTienNhan) {
        return thanhToan(maHD,hinhThuc,soTienNhan,null);
    }
    public BigDecimal thanhToan(String maHD,String hinhThuc,BigDecimal soTienNhan,String maVoucherMoi) {
        return new PhieuThuDAO().thanhToanHet(maHD,hinhThuc,soTienNhan,
                maVoucherMoi==null || maVoucherMoi.isBlank()?null:maVoucherMoi.trim());
    }

    private HoaDon loc(HoaDon hd, VaiTro vaiTro) {
        if (hd != null) {
            PhieuKhamDAO.loc(hd.getPhieuKham(), vaiTro);
        }
        return hd;
    }

    private HoaDon mapRow(Connection c, ResultSet r) throws SQLException {
        String maVoucher = r.getString("maVoucher");
        String maNV = r.getString("maNV_ThuNgan");
        HoaDon hd = new HoaDon(
                r.getString("maHD"),
                r.getObject("ngayLap", LocalDateTime.class),
                r.getBoolean("trangThai"),
                r.getString("hinhThucThanhToan"),
                r.getObject("ngayThanhToan", LocalDateTime.class),
                r.getBigDecimal("tongKhuyenMai"),
                new PhieuKhamDAO().layTheoMa(c, r.getString("maPK"), true),
                maVoucher == null ? null : new VoucherDAO().layTheoMa(c, maVoucher),
                maNV == null ? null : new NhanVienDAO().layTheoMa(c, maNV));
        return HoaDon.tuDuLieuTaiChinh(hd,hd.getPhieuKham().getHoSoBenhAn().getKhachHang(),
                r.getString("tenKhachHang"),r.getString("sdtKhachHang"),r.getString("diaChiKhachHang"),
                r.getBigDecimal("tongTienHang"),r.getBigDecimal("tongThueGTGT"),r.getBigDecimal("tongKhuyenMai"),
                r.getBigDecimal("tongThanhToan"),r.getBigDecimal("tongDaThu"));
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
