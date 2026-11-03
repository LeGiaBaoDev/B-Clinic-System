package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.entity.DichVu;
import com.quanlynhakhoa.entity.enums.VaiTro;
import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

/** UC005–008: danh mục, giá hiện tại; không sửa ảnh chụp giá ở chi tiết khám cũ. */
public class DichVuDAO extends DaoSupport {
    public DichVuDAO() { }
    public DichVuDAO(DaoContext context) { super(context); }
    public List<DichVu> layTatCa() { return timKiem("",false); }
    public List<DichVu> timKiem(String ten, boolean chiDangHoatDong) {
        return context.doc(c -> { quyen(c); return query(c,DaoViews.DICH_VU
                +" WHERE (a.tenDV LIKE ? OR a.maDV LIKE ?) AND (?=0 OR a.trangThai=1) ORDER BY a.maDV",
                this::mapRow,tuKhoa(ten),tuKhoa(ten),chiDangHoatDong); });
    }
    public DichVu layTheoMa(String ma) { return context.doc(c -> { quyen(c); return layTheoMa(c,ma); }); }
    DichVu layTheoMa(Connection c,String ma) throws SQLException {
        return one(c,DaoViews.DICH_VU+" WHERE a.maDV=?",this::mapRow,ma);
    }
    public boolean them(DichVu dv) { return luu(dv,true); }
    public boolean capNhat(DichVu dv) { return luu(dv,false); }
    private boolean luu(DichVu dv, boolean them) {
        return context.transaction(c -> {
            quyen(c,VaiTro.QUANLY);
            new DichVu(dv.getMaDV(),dv.getTenDV(),dv.getDonGia(),dv.getThoiLuong(),dv.getLoTrinhDichVu(),dv.isTrangThai(),dv.getLoaiDichVu());
            khoa(c,"BClinic:lich");
            khoa(c,"BClinic:dich-vu");
            if(!them) {
                DichVu old=layTheoMa(c,dv.getMaDV());
                can(old!=null,"Dịch vụ không tồn tại.");
                if(!dv.isTrangThai() || old.getThoiLuong()!=dv.getThoiLuong()) {
                    can(!exists(c,"SELECT 1 FROM LichHen WHERE maDV=? AND trangThai IN('DA_DAT','DA_DEN')",dv.getMaDV()),
                            "Cần xử lý các lịch hẹn hiện có trước khi đổi thời lượng hoặc ngừng dịch vụ.");
                }
            }
            can(!dv.isTrangThai() || !exists(c,"SELECT 1 FROM DichVu WHERE trangThai=1 AND tenDV=? AND maDV<>?",
                    dv.getTenDV(),them ? "" : dv.getMaDV()),"Tên dịch vụ đang hoạt động đã tồn tại.");
            can(exists(c,"SELECT 1 FROM LoaiDichVu WHERE maLoaiDV=?",dv.getLoaiDichVu().getMaLoaiDV()),"Loại dịch vụ không tồn tại.");
            String ma=them ? maMoi(c,Bang.DICH_VU,null) : dv.getMaDV();
            int rows=them ? update(c,"INSERT INTO DichVu(tenDV,maLoaiDV,donGia,thoiLuong,loTrinhDichVu,trangThai,maDV) VALUES(?,?,?,?,?,?,?)",
                    dv.getTenDV(),dv.getLoaiDichVu().getMaLoaiDV(),BigDecimal.valueOf(dv.getDonGia()),dv.getThoiLuong(),dv.getLoTrinhDichVu(),dv.isTrangThai(),ma)
                    : update(c,"UPDATE DichVu SET tenDV=?,maLoaiDV=?,donGia=?,thoiLuong=?,loTrinhDichVu=?,trangThai=? WHERE maDV=?",
                    dv.getTenDV(),dv.getLoaiDichVu().getMaLoaiDV(),BigDecimal.valueOf(dv.getDonGia()),dv.getThoiLuong(),dv.getLoTrinhDichVu(),dv.isTrangThai(),ma);
            if(rows==1) { dv.setMaDV(ma); nhatKy(c,them ? "THEM_DICH_VU" : "CAP_NHAT_DICH_VU",ma); }
            return rows==1;
        });
    }
    public boolean ngungCungCap(String ma) {
        return context.transaction(c -> { quyen(c,VaiTro.QUANLY); khoa(c,"BClinic:lich");
            can(!exists(c,"SELECT 1 FROM LichHen WHERE maDV=? AND trangThai IN('DA_DAT','DA_DEN')",ma),"Dịch vụ còn lịch hẹn chưa xử lý.");
            boolean ok=update(c,"UPDATE DichVu SET trangThai=0 WHERE maDV=?",ma)==1;
            if(ok) { nhatKy(c,"NGUNG_DICH_VU",ma); } return ok; });
    }
    public boolean xoa(String ma) { return ngungCungCap(ma); }
    private DichVu mapRow(ResultSet r) throws SQLException { return DaoViews.dichVu(r); }
}
