package com.quanlynhakhoa.dao;
import com.quanlynhakhoa.entity.NhatKyHeThong;
import com.quanlynhakhoa.entity.enums.VaiTro;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

/** Nhật ký chỉ thêm; quyền xem toàn bộ dành cho quản lý. */
public class NhatKyHeThongDAO extends DaoSupport {
    public NhatKyHeThongDAO() { }
    public NhatKyHeThongDAO(DaoContext context) { super(context); }
    public void ghi(String hanhDong,String noiDung) {
        can(hanhDong!=null && !hanhDong.isBlank() && hanhDong.length()<=100,"Hành động nhật ký không hợp lệ.");
        context.transaction(c -> { quyen(c); nhatKy(c,hanhDong,noiDung); return null; });
    }
    public List<NhatKyHeThong> layTatCa() {
        return context.doc(c -> { quyen(c,VaiTro.QUANLY); return query(c,DaoViews.NHAT_KY+" ORDER BY a.thoiDiem DESC,a.maNhatKy DESC",this::mapRow); });
    }
    public List<NhatKyHeThong> timKiem(LocalDate tu,LocalDate den,String maTK) {
        khoangNgay(tu,den);
        return context.doc(c -> { quyen(c,VaiTro.QUANLY); return query(c,DaoViews.NHAT_KY
                +" WHERE a.thoiDiem>=? AND a.thoiDiem<? AND (? IS NULL OR a.maTK=?) ORDER BY a.thoiDiem DESC",
                this::mapRow,tu.atStartOfDay(),den.plusDays(1).atStartOfDay(),maTK,maTK); });
    }
    private NhatKyHeThong mapRow(ResultSet r) throws SQLException { return DaoViews.nhatKyHeThong(r); }
}
