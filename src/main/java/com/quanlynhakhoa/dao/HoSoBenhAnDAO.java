package com.quanlynhakhoa.dao;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.VaiTro;
import java.sql.*;
import java.util.List;

/** UC024: hồ sơ chỉ thêm; đính chính điều trị qua chi tiết khám mới, không UPDATE/DELETE. */
public class HoSoBenhAnDAO extends DaoSupport {
    public HoSoBenhAnDAO() { }
    public HoSoBenhAnDAO(DaoContext context) { super(context); }
    public List<HoSoBenhAn> layTatCa() {
        return context.doc(c -> { TaiKhoan actor=quyen(c,VaiTro.QUANLY,VaiTro.NHASI);
            return query(c,DaoViews.HO_SO+" WHERE (?=1 OR EXISTS(SELECT 1 FROM LichHen lh WHERE lh.maKH=a.maKH AND lh.maNV=?)) ORDER BY a.maHSBA",
                    this::mapRow,actor.getVaiTro()==VaiTro.QUANLY,actor.getNhanVien().getMaNV()); });
    }
    public HoSoBenhAn layTheoMa(String ma) {
        return context.doc(c -> { TaiKhoan actor=quyen(c,VaiTro.QUANLY,VaiTro.NHASI); HoSoBenhAn hs=layTheoMa(c,ma);
            if(hs!=null && actor.getVaiTro()==VaiTro.NHASI) { can(exists(c,"SELECT 1 FROM LichHen WHERE maKH=? AND maNV=?",hs.getKhachHang().getMaKH(),actor.getNhanVien().getMaNV()),"Bạn không được xem bệnh án này."); }
            return hs; });
    }
    HoSoBenhAn layTheoMa(Connection c,String ma) throws SQLException { return one(c,DaoViews.HO_SO+" WHERE a.maHSBA=?",this::mapRow,ma); }
    public boolean them(HoSoBenhAn hs) {
        return context.transaction(c -> { quyen(c,VaiTro.QUANLY,VaiTro.LETAN); them(c,hs); nhatKy(c,"TAO_HO_SO",hs.getMaHSBA()); return true; });
    }
    void them(Connection c,HoSoBenhAn hs) throws SQLException {
        String ma=maMoi(c,Bang.HO_SO,null);
        update(c,"INSERT INTO HoSoBenhAn(maHSBA,maKH,tienSuBenh) VALUES(?,?,?)",ma,hs.getKhachHang().getMaKH(),hs.getTienSuBenh()); hs.setMaHSBA(ma);
    }
    private HoSoBenhAn mapRow(ResultSet r) throws SQLException { return DaoViews.hoSoBenhAn(r); }
}
