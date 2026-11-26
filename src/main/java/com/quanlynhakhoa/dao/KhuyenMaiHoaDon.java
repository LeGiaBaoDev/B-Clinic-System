package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.entity.Voucher;
import java.math.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** Chọn mã tốt nhất trên tổng sau thuế của từng dịch vụ; phân bổ xuống dòng bằng lũy kế. */
final class KhuyenMaiHoaDon {
    private record Dong(int id,String maDV,BigDecimal gross) { }
    private record UngVien(String maDV,Voucher voucher) { }
    private KhuyenMaiHoaDon() { }
    static void apDung(Connection c,String maHD,String maNhap) throws SQLException {
        BigDecimal daThu=JdbcHelper.one(c,"SELECT tongDaThu FROM HoaDon WITH(UPDLOCK,HOLDLOCK) WHERE maHD=?",r->r.getBigDecimal(1),maHD);
        if(daThu==null) throw new com.quanlynhakhoa.exception.BusinessException("Hóa đơn không tồn tại.");
        if(daThu.signum()>0) {
            if(maNhap!=null) throw new com.quanlynhakhoa.exception.BusinessException("Khuyến mãi đã chốt sau lần thu đầu tiên.");
            return;
        }
        List<Dong> rows=JdbcHelper.query(c,"SELECT maCTHD,maDV,tienHang+tienGTGT FROM ChiTietHoaDon WHERE maHD=? ORDER BY maCTHD",
                r->new Dong(r.getInt(1),r.getString(2),r.getBigDecimal(3)),maHD);
        Map<String,BigDecimal> totals=new LinkedHashMap<>();
        for(Dong row:rows) totals.merge(row.maDV(),row.gross(),BigDecimal::add);
        LocalDate day=LocalDate.now();
        List<UngVien> candidates=JdbcHelper.query(c,"""
                SELECT m.maDV,v.* FROM VoucherDichVu m JOIN Voucher v ON v.maVoucher=m.maVoucher
                WHERE v.trangThai=1 AND v.ngayBatDau<=? AND v.ngayKetThuc>=?
                  AND EXISTS(SELECT 1 FROM ChiTietHoaDon d WHERE d.maHD=? AND d.maDV=m.maDV)
                ORDER BY v.maVoucher
                """,r->{
            Voucher v=new Voucher(r.getString("maVoucher"),r.getString("tenVoucher"),r.getBoolean("loaiGiam"),
                r.getBigDecimal("giaGiam"),r.getBigDecimal("giaTriGiamToiDa"),r.getObject("ngayBatDau",LocalDate.class),r.getObject("ngayKetThuc",LocalDate.class),true);
            v.setGiaGiam(r.getBigDecimal("giaGiam"));
            BigDecimal cap=r.getBigDecimal("giaTriGiamToiDa");v.setGiaTriGiamToiDa(cap==null?BigDecimal.ZERO:cap);
            return new UngVien(r.getString("maDV"),v);
        },day,day,maHD);
        if(maNhap!=null && candidates.stream().noneMatch(v->v.voucher().getMaVoucher().equals(maNhap)))
            throw new com.quanlynhakhoa.exception.BusinessException("Voucher không phù hợp với dịch vụ hoặc đã hết hiệu lực.");
        Map<String,Voucher> best=new HashMap<>();Map<String,BigDecimal> discounts=new HashMap<>();
        for(UngVien v:candidates){
            BigDecimal amount=v.voucher().tinhTienGiam(totals.get(v.maDV()));
            if(amount.compareTo(discounts.getOrDefault(v.maDV(),BigDecimal.ZERO))>0){best.put(v.maDV(),v.voucher());discounts.put(v.maDV(),amount);}
        }
        Map<String,BigDecimal> cumulative=new HashMap<>();
        for(Dong row:rows){
            Voucher v=best.get(row.maDV());BigDecimal discount=BigDecimal.ZERO;
            if(v!=null){
                BigDecimal before=cumulative.getOrDefault(row.maDV(),BigDecimal.ZERO),after=before.add(row.gross());
                BigDecimal full=discounts.get(row.maDV()),total=totals.get(row.maDV());
                discount=full.multiply(after).divide(total,2,RoundingMode.HALF_UP)
                    .subtract(full.multiply(before).divide(total,2,RoundingMode.HALF_UP));
                cumulative.put(row.maDV(),after);
            }
            JdbcHelper.update(c,"""
                    UPDATE ChiTietHoaDon SET maVoucher=?,tenKhuyenMai=?,loaiGiam=?,giaTriGiam=?,
                        tienGiam=?,thanhTien=tienHang+tienGTGT-? WHERE maCTHD=?
                    """,v==null?null:v.getMaVoucher(),v==null?null:v.getTenVoucher(),v==null?null:v.isLoaiGiam(),
                    v==null?null:v.getGiaGiamChinhXac(),discount,discount,row.id());
        }
    }
}
