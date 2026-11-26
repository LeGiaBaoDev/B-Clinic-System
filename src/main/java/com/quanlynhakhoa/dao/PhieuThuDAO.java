package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.*;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;

public class PhieuThuDAO {
    public PhieuThu thuTien(String maHD,BigDecimal soTien,String hinhThuc,String khoaYeuCau,String maThamChieu) {
        if(soTien==null || soTien.signum()<=0 || soTien.scale()>2) throw new BusinessException("Số tiền thu không hợp lệ.");
        return JdbcHelper.transaction(c->ghi(c,maHD,soTien,hinhThuc,khoaYeuCau,maThamChieu,null));
    }
    BigDecimal thanhToanHet(String maHD,String hinhThuc,BigDecimal khachDua,String voucher) {
        return JdbcHelper.transaction(c->{
            PhanQuyen.quyen(c,VaiTro.QUANLY,VaiTro.LETAN);MaSinh.khoa(c,"BClinic:thu:"+maHD);
            KhuyenMaiHoaDon.apDung(c,maHD,voucher);
            BigDecimal conNo=JdbcHelper.one(c,"SELECT conNo FROM HoaDon WITH(UPDLOCK,HOLDLOCK) WHERE maHD=?",r->r.getBigDecimal(1),maHD);
            if(conNo==null || conNo.signum()<=0) throw new BusinessException("Hóa đơn không tồn tại hoặc đã thanh toán.");
            if(khachDua==null || khachDua.compareTo(conNo)<0 || HoaDon.CHUYEN_KHOAN.equals(hinhThuc) && khachDua.compareTo(conNo)!=0)
                throw new BusinessException("Số tiền khách đưa không phù hợp.");
            ghi(c,maHD,conNo,hinhThuc,UUID.randomUUID().toString(),null,null);
            return khachDua.subtract(conNo);
        });
    }
    private PhieuThu ghi(Connection c,String maHD,BigDecimal soTien,String hinhThuc,String key,String ref,String voucher) throws SQLException {
        var actor=PhanQuyen.quyen(c,VaiTro.QUANLY,VaiTro.LETAN);
        if(maHD==null || !maHD.matches("HD\\d{9}") || key==null || key.isBlank() || key.length()>100) throw new BusinessException("Thiếu mã hóa đơn hoặc khóa chống trùng.");
        if(!HoaDon.TIEN_MAT.equals(hinhThuc) && !HoaDon.CHUYEN_KHOAN.equals(hinhThuc)) throw new BusinessException("Hình thức thanh toán không hợp lệ.");
        MaSinh.khoa(c,"BClinic:thu:"+maHD);
        PhieuThu existing=JdbcHelper.one(c,"SELECT * FROM PhieuThu WITH(UPDLOCK,HOLDLOCK) WHERE khoaYeuCau=?",r->map(c,r),key);
        if(existing!=null){
            if(!maHD.equals(existing.getHoaDon().getMaHD()) || existing.getSoTienThu().compareTo(soTien)!=0
                || !hinhThuc.equals(existing.getHinhThucThanhToan()) || !actor.maNV().equals(existing.getThuNgan().getMaNV()))
                throw new BusinessException("Khóa chống trùng đã dùng cho giao dịch khác.");
            return existing;
        }
        KhuyenMaiHoaDon.apDung(c,maHD,voucher);
        BigDecimal conNo=JdbcHelper.one(c,"SELECT conNo FROM HoaDon WITH(UPDLOCK,HOLDLOCK) WHERE maHD=?",r->r.getBigDecimal(1),maHD);
        if(conNo==null || soTien.compareTo(conNo)>0 || soTien.signum()<=0) throw new BusinessException("Số tiền vượt công nợ.");
        String ma=MaSinh.maMoi(c,MaSinh.Bang.PHIEU_THU,LocalDate.now());
        LocalDateTime now=LocalDateTime.now().withNano(0);
        JdbcHelper.update(c,"INSERT PhieuThu(maPT,maHD,ngayThu,soTienThu,hinhThucThanhToan,maNVThu,maThamChieu,khoaYeuCau) VALUES(?,?,?,?,?,?,?,?)",
                ma,maHD,now,soTien,hinhThuc,actor.maNV(),ref,key);
        NhatKy.ghi(c,"THU_TIEN",ma+" / "+maHD);
        return JdbcHelper.one(c,"SELECT * FROM PhieuThu WHERE maPT=?",r->map(c,r),ma);
    }
    public List<PhieuThu> layTheoHoaDon(String maHD) {
        return JdbcHelper.doc(c->{PhanQuyen.quyen(c,VaiTro.QUANLY,VaiTro.LETAN);
            return JdbcHelper.query(c,"SELECT * FROM PhieuThu WHERE maHD=? ORDER BY ngayThu,maPT",r->map(c,r),maHD);});
    }
    private PhieuThu map(Connection c,ResultSet r)throws SQLException {
        HoaDon hd=new HoaDon();hd.setMaHD(r.getString("maHD"));
        NhanVien nv=new NhanVienDAO().layTheoMa(c,r.getString("maNVThu"));
        return new PhieuThu(r.getString("maPT"),hd,r.getObject("ngayThu",LocalDateTime.class),r.getBigDecimal("soTienThu"),
                r.getString("hinhThucThanhToan"),nv,r.getString("maThamChieu"),r.getString("khoaYeuCau"),r.getString("ghiChu"));
    }
}
