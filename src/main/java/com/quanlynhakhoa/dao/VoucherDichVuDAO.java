package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.*;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.sql.*;
import java.util.*;

public class VoucherDichVuDAO {
    /** Gán phạm vi trong một transaction; đã dùng thì tạo chương trình mới thay vì sửa lịch sử. */
    public void ganDichVu(String maVoucher,List<String> maDichVu) {
        if(maDichVu==null || maDichVu.isEmpty() || maDichVu.stream().anyMatch(Objects::isNull)) throw new BusinessException("Phải chọn dịch vụ áp dụng.");
        Set<String> ids=new LinkedHashSet<>(maDichVu);
        JdbcHelper.transaction(c->{
            PhanQuyen.quyen(c,VaiTro.QUANLY);MaSinh.khoa(c,"BClinic:voucher:"+maVoucher);
            if(!JdbcHelper.exists(c,"SELECT 1 FROM Voucher WITH(UPDLOCK,HOLDLOCK) WHERE maVoucher=?",maVoucher))throw new BusinessException("Voucher không tồn tại.");
            if(JdbcHelper.exists(c,"SELECT 1 FROM ChiTietHoaDon WHERE maVoucher=?",maVoucher))throw new BusinessException("Voucher đã dùng; hãy tạo voucher mới.");
            for(String id:ids) if(!JdbcHelper.exists(c,"SELECT 1 FROM DichVu WHERE maDV=?",id))throw new BusinessException("Dịch vụ không tồn tại.");
            JdbcHelper.update(c,"DELETE FROM VoucherDichVu WHERE maVoucher=?",maVoucher);
            for(String id:ids)JdbcHelper.update(c,"INSERT VoucherDichVu(maVoucher,maDV) VALUES(?,?)",maVoucher,id);
            NhatKy.ghi(c,"GAN_VOUCHER_DICH_VU",maVoucher);return null;
        });
    }
    /** Các cấu hình đã từng được gán, gồm chương trình hết hạn để quản lý sao chép tạo mới. */
    public List<Voucher> layTheoDichVu(String maDV) {
        return JdbcHelper.doc(c->{
            PhanQuyen.quyen(c,VaiTro.QUANLY,VaiTro.LETAN);
            return JdbcHelper.query(c,"SELECT v.* FROM Voucher v JOIN VoucherDichVu m ON m.maVoucher=v.maVoucher WHERE m.maDV=? ORDER BY v.ngayBatDau DESC,v.maVoucher",r->{
                Voucher v=new Voucher(r.getString("maVoucher"),r.getString("tenVoucher"),r.getBoolean("loaiGiam"),r.getBigDecimal("giaGiam"),r.getBigDecimal("giaTriGiamToiDa"),r.getObject("ngayBatDau",java.time.LocalDate.class),r.getObject("ngayKetThuc",java.time.LocalDate.class),r.getBoolean("trangThai"));
                v.setGiaGiam(r.getBigDecimal("giaGiam"));java.math.BigDecimal cap=r.getBigDecimal("giaTriGiamToiDa");v.setGiaTriGiamToiDa(cap==null?java.math.BigDecimal.ZERO:cap);return v;
            },maDV);
        });
    }
}
