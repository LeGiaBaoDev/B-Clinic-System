package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.entity.Voucher;
import com.quanlynhakhoa.entity.enums.VaiTro;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

/** UC036–038: không xóa voucher đã sử dụng. */
public class VoucherDAO extends DaoSupport {
    public VoucherDAO() { }
    public VoucherDAO(DaoContext context) { super(context); }
    public List<Voucher> layTatCa() { return context.doc(c -> { quyen(c,VaiTro.QUANLY); return query(c,DaoViews.VOUCHER+" ORDER BY a.maVoucher",this::mapRow); }); }
    public Voucher layTheoMa(String ma) { return context.doc(c -> { quyen(c,VaiTro.QUANLY,VaiTro.LETAN); return layTheoMa(c,ma); }); }
    Voucher layTheoMa(Connection c,String ma) throws SQLException { return one(c,DaoViews.VOUCHER+" WHERE a.maVoucher=?",this::mapRow,ma); }
    public List<Voucher> layConHieuLuc(LocalDate ngay) {
        can(ngay!=null,"Thiếu ngày áp dụng.");
        return context.doc(c -> { quyen(c,VaiTro.QUANLY,VaiTro.LETAN); return query(c,DaoViews.VOUCHER
                +" WHERE a.trangThai=1 AND ? BETWEEN a.ngayBatDau AND a.ngayKetThuc ORDER BY a.maVoucher",this::mapRow,ngay); });
    }
    public boolean them(Voucher v) { return luu(v,true); }
    public boolean capNhat(Voucher v) { return luu(v,false); }
    private boolean luu(Voucher v,boolean them) {
        return context.transaction(c -> {
            quyen(c,VaiTro.QUANLY);
            khoa(c,"BClinic:thanh-toan");
            new Voucher(v.getMaVoucher(),v.getTenVoucher(),v.isLoaiGiam(),v.getGiaGiam(),v.getGiaTriGiamToiDa(),v.getNgayBatDau(),v.getNgayKetThuc(),v.isTrangThai());
            if(!them) { can(!exists(c,"SELECT 1 FROM HoaDon WITH(UPDLOCK,HOLDLOCK) WHERE maVoucher=?",v.getMaVoucher()),"Voucher đã dùng; chỉ được vô hiệu hóa để giữ lịch sử."); }
            String ma=them ? maMoi(c,Bang.VOUCHER,null) : v.getMaVoucher();
            Object cap=v.getGiaTriGiamToiDa()==0 ? null : BigDecimal.valueOf(v.getGiaTriGiamToiDa());
            Object[] args={v.getTenVoucher(),v.isLoaiGiam()?1:0,BigDecimal.valueOf(v.getGiaGiam()),cap,v.getNgayBatDau(),v.getNgayKetThuc(),v.isTrangThai(),ma};
            int rows=update(c,them ? "INSERT INTO Voucher(tenVoucher,loaiGiam,giaGiam,giaTriGiamToiDa,ngayBatDau,ngayKetThuc,trangThai,maVoucher) VALUES(?,?,?,?,?,?,?,?)"
                    : "UPDATE Voucher SET tenVoucher=?,loaiGiam=?,giaGiam=?,giaTriGiamToiDa=?,ngayBatDau=?,ngayKetThuc=?,trangThai=? WHERE maVoucher=?",args);
            if(rows==1) { v.setMaVoucher(ma); nhatKy(c,them?"THEM_VOUCHER":"CAP_NHAT_VOUCHER",ma); }
            return rows==1;
        });
    }
    public boolean xoa(String ma) {
        return context.transaction(c -> { quyen(c,VaiTro.QUANLY); khoa(c,"BClinic:thanh-toan"); boolean ok=update(c,"UPDATE Voucher SET trangThai=0 WHERE maVoucher=?",ma)==1;
            if(ok) { nhatKy(c,"VO_HIEU_VOUCHER",ma); } return ok; });
    }
    private Voucher mapRow(ResultSet r) throws SQLException { return DaoViews.voucher(r); }
}
