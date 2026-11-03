package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.entity.LoaiDichVu;
import com.quanlynhakhoa.entity.enums.*;
import java.sql.*;
import java.util.List;

/** Danh mục loại dịch vụ và thuế tương ứng trong schema hiện hành. */
public class LoaiDichVuDAO extends DaoSupport {
    public LoaiDichVuDAO() { }
    public LoaiDichVuDAO(DaoContext context) { super(context); }
    public List<LoaiDichVu> layTatCa() {
        return context.doc(c -> { quyen(c); return query(c,DaoViews.LOAI_DICH_VU+" ORDER BY a.maLoaiDV",this::mapRow); });
    }
    public LoaiDichVu layTheoMa(String ma) {
        return context.doc(c -> { quyen(c); return one(c,DaoViews.LOAI_DICH_VU+" WHERE a.maLoaiDV=?",this::mapRow,ma); });
    }
    public boolean them(LoaiDichVu loai) {
        return context.transaction(c -> {
            quyen(c,VaiTro.QUANLY);
            new LoaiDichVu(null,loai.getTenLoaiDV(),loai.getMucThueGTGT());
            khoa(c,"BClinic:loai-dich-vu");
            can(!exists(c,"SELECT 1 FROM LoaiDichVu WHERE tenLoaiDV=?",loai.getTenLoaiDV()),"Loại dịch vụ đã tồn tại.");
            String ma=maMoi(c,Bang.LOAI_DICH_VU,null);
            update(c,"INSERT INTO LoaiDichVu(maLoaiDV,tenLoaiDV,mucThueGTGT) VALUES(?,?,?)",ma,loai.getTenLoaiDV(),thue(loai.getMucThueGTGT()));
            loai.setMaLoaiDV(ma); nhatKy(c,"THEM_LOAI_DICH_VU",ma); return true;
        });
    }
    public boolean capNhat(LoaiDichVu loai) {
        return context.transaction(c -> {
            quyen(c,VaiTro.QUANLY);
            new LoaiDichVu(loai.getMaLoaiDV(),loai.getTenLoaiDV(),loai.getMucThueGTGT());
            khoa(c,"BClinic:loai-dich-vu");
            can(!exists(c,"SELECT 1 FROM LoaiDichVu WHERE tenLoaiDV=? AND maLoaiDV<>?",loai.getTenLoaiDV(),loai.getMaLoaiDV()),"Loại dịch vụ đã tồn tại.");
            return update(c,"UPDATE LoaiDichVu SET tenLoaiDV=?,mucThueGTGT=? WHERE maLoaiDV=?",loai.getTenLoaiDV(),thue(loai.getMucThueGTGT()),loai.getMaLoaiDV())==1;
        });
    }
    static int thue(MucThue value) { return switch(value) { case THUE_0 -> 0; case THUE_5 -> 5; case THUE_10 -> 10; }; }
    private LoaiDichVu mapRow(ResultSet r) throws SQLException { return DaoViews.loaiDichVu(r); }
}
