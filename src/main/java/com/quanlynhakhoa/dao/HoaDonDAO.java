package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import java.math.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

/** UC025–026: giá/thuế lấy từ chi tiết đã lưu; khóa để không thanh toán hai lần. */
public class HoaDonDAO extends DaoSupport {
    public HoaDonDAO() { }
    public HoaDonDAO(DaoContext context) { super(context); }
    public List<HoaDon> layTatCa() { return timKiem(LocalDate.of(2000,1,1),LocalDate.of(2100,1,1),null); }
    public List<HoaDon> timKiem(LocalDate tu,LocalDate den,Boolean daTra) {
        khoangNgay(tu,den);
        return context.doc(c -> { TaiKhoan actor=quyen(c,VaiTro.QUANLY,VaiTro.LETAN);
            return query(c,DaoViews.HOA_DON+" WHERE a.ngayLap>=? AND a.ngayLap<? AND (? IS NULL OR a.trangThai=?) ORDER BY a.ngayLap DESC",
                    r -> loc(mapRow(r),actor),tu.atStartOfDay(),den.plusDays(1).atStartOfDay(),daTra,daTra); });
    }
    public HoaDon layTheoMa(String ma) {
        return context.doc(c -> { TaiKhoan actor=quyen(c,VaiTro.QUANLY,VaiTro.LETAN); return loc(layTheoMa(c,ma),actor); });
    }
    HoaDon layTheoMa(Connection c,String ma) throws SQLException { return one(c,DaoViews.HOA_DON+" WHERE a.maHD=?",this::mapRow,ma); }
    String taoTuPhieuKham(Connection c,String maPK) throws SQLException {
        can(exists(c,"SELECT 1 FROM PhieuKham WHERE maPK=? AND trangThai='HOAN_TAT'",maPK),"Phiếu khám chưa hoàn tất.");
        can(!exists(c,"SELECT 1 FROM HoaDon WITH(UPDLOCK,HOLDLOCK) WHERE maPK=?",maPK),"Phiếu khám đã có hóa đơn.");
        String ma=maMoi(c,Bang.HOA_DON,context.bayGio().toLocalDate());
        update(c,"INSERT INTO HoaDon(maHD,maPK,ngayLap) VALUES(?,?,?)",ma,maPK,context.bayGio()); return ma;
    }
    static BigDecimal tongTruocGiam(Connection c,String maPK) throws SQLException {
        return one(c,"SELECT COALESCE(SUM(thanhTien+ROUND(thanhTien*mucThueGTGT/100,0)),0) FROM ChiTietPhieuKham WHERE maPK=?",r -> r.getBigDecimal(1),maPK);
    }
    static BigDecimal tienGiam(Voucher vc,BigDecimal tong) {
        BigDecimal giam=BigDecimal.valueOf(vc.getGiaGiam());
        if(vc.isLoaiGiam()) {
            giam=tong.multiply(giam).divide(BigDecimal.valueOf(100),0,RoundingMode.HALF_UP);
            if(vc.getGiaTriGiamToiDa()>0) { giam=giam.min(BigDecimal.valueOf(vc.getGiaTriGiamToiDa())); }
        }
        return giam.min(tong).max(BigDecimal.ZERO).setScale(0,RoundingMode.HALF_UP);
    }
    public BigDecimal tinhTongTien(String maHD) {
        return context.doc(c -> { quyen(c,VaiTro.QUANLY,VaiTro.LETAN); HoaDon hd=layTheoMa(c,maHD); can(hd!=null,"Hóa đơn không tồn tại.");
            return tongTruocGiam(c,hd.getPhieuKham().getMaPK()).subtract(BigDecimal.valueOf(hd.getTongKhuyenMai())).max(BigDecimal.ZERO); });
    }
    public boolean apDungVoucher(String maHD,String maVoucher) {
        return context.transaction(c -> {
            quyen(c,VaiTro.LETAN,VaiTro.QUANLY); khoa(c,"BClinic:thanh-toan");
            HoaDon hd=layTheoMa(c,maHD); can(hd!=null && !hd.isTrangThai(),"Hóa đơn không tồn tại hoặc đã thanh toán.");
            can(hd.getVoucher()==null,"Mỗi hóa đơn chỉ được áp dụng một voucher.");
            Voucher vc=new VoucherDAO(context).layTheoMa(c,maVoucher);
            can(vc!=null && vc.kiemTraHieuLuc(context.bayGio().toLocalDate()),"Voucher không có hiệu lực.");
            BigDecimal giam=tienGiam(vc,tongTruocGiam(c,hd.getPhieuKham().getMaPK()));
            update(c,"UPDATE HoaDon SET maVoucher=?,tongKhuyenMai=? WHERE maHD=? AND trangThai=0",maVoucher,giam,maHD);
            nhatKy(c,"AP_DUNG_VOUCHER",maHD+" / "+maVoucher); return true;
        });
    }
    /** Với tiền mặt yêu cầu tiền nhận đủ; chuyển khoản truyền đúng tổng để xác nhận số tiền. */
    public BigDecimal thanhToan(String maHD,String hinhThuc,BigDecimal soTienNhan) {
        can(HoaDon.TIEN_MAT.equals(hinhThuc) || HoaDon.CHUYEN_KHOAN.equals(hinhThuc),"Hình thức thanh toán không hợp lệ.");
        return context.transaction(c -> {
            TaiKhoan actor=quyen(c,VaiTro.LETAN,VaiTro.QUANLY); khoa(c,"BClinic:thanh-toan");
            HoaDon hd=layTheoMa(c,maHD); can(hd!=null && !hd.isTrangThai(),"Hóa đơn không tồn tại hoặc đã thanh toán.");
            if(hd.getVoucher()!=null) {
                Voucher vc=new VoucherDAO(context).layTheoMa(c,hd.getVoucher().getMaVoucher());
                can(vc.kiemTraHieuLuc(context.bayGio().toLocalDate()),"Voucher đã hết hiệu lực trước khi thanh toán.");
                can(tienGiam(vc,tongTruocGiam(c,hd.getPhieuKham().getMaPK())).compareTo(BigDecimal.valueOf(hd.getTongKhuyenMai()))==0,"Giá trị voucher đã thay đổi.");
            }
            BigDecimal tong=tongTruocGiam(c,hd.getPhieuKham().getMaPK()).subtract(BigDecimal.valueOf(hd.getTongKhuyenMai())).max(BigDecimal.ZERO);
            can(soTienNhan!=null && soTienNhan.compareTo(tong)>=0,"Số tiền nhận chưa đủ.");
            can(!HoaDon.CHUYEN_KHOAN.equals(hinhThuc) || soTienNhan.compareTo(tong)==0,"Số tiền chuyển khoản phải khớp tổng hóa đơn.");
            can(hd.getPhieuKham().getTrangThai()==TrangThaiPhieuKham.HOAN_TAT,"Phiếu khám chưa hoàn tất.");
            can(update(c,"UPDATE HoaDon SET trangThai=1,maNV_ThuNgan=?,hinhThucThanhToan=?,ngayThanhToan=? WHERE maHD=? AND trangThai=0",
                    actor.getNhanVien().getMaNV(),hinhThuc,context.bayGio(),maHD)==1,"Hóa đơn vừa được thanh toán.");
            LichHen lh=hd.getPhieuKham().getLichHen();
            if(lh!=null) { can(update(c,"UPDATE LichHen SET trangThai='HOAN_TAT' WHERE maLH=? AND trangThai='DA_DEN'",lh.getMaLH())==1,"Trạng thái lịch hẹn không phù hợp để thanh toán."); }
            nhatKy(c,"THANH_TOAN",maHD); return soTienNhan.subtract(tong);
        });
    }
    private HoaDon loc(HoaDon hd,TaiKhoan actor) { if(hd!=null) { ClinicalPrivacy.loc(hd.getPhieuKham(),actor.getVaiTro()); } return hd; }
    private HoaDon mapRow(ResultSet r) throws SQLException { return DaoViews.hoaDon(r); }
}
