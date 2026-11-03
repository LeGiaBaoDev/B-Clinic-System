package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

/** Chi tiết điều trị chỉ thêm khi đang khám; không cung cấp API sửa/xóa bệnh án. */
public class ChiTietPhieuKhamDAO extends DaoSupport {
    public ChiTietPhieuKhamDAO() { }
    public ChiTietPhieuKhamDAO(DaoContext context) { super(context); }
    public List<ChiTietPhieuKham> layTheoPhieuKham(String maPK) {
        return context.doc(c -> {
            TaiKhoan actor=quyen(c,VaiTro.NHASI,VaiTro.QUANLY);
            PhieuKham pk=new PhieuKhamDAO(context).layTheoMa(c,maPK); can(pk!=null,"Phiếu khám không tồn tại."); PhieuKhamDAO.kiemTraQuyen(pk,actor);
            return query(c,DaoViews.CHI_TIET+" WHERE a.maPK=? ORDER BY a.maCTPK",this::mapRow,maPK);
        });
    }
    /** Dữ liệu tính tiền không chứa triệu chứng/chẩn đoán, dành cho lễ tân. */
    public record DongThanhToan(int maCTPK,String maDV,String tenDV,String donViTinh,int soLuong,
            BigDecimal donGia,BigDecimal mucThueGTGT,BigDecimal thanhTien,BigDecimal tienThue) { }
    public List<DongThanhToan> layDongThanhToan(String maPK) {
        return context.doc(c -> { quyen(c,VaiTro.LETAN,VaiTro.QUANLY);
            return query(c,"SELECT ct.maCTPK,ct.maDV,dv.tenDV,ct.donViTinh,ct.soLuong,ct.donGia,ct.mucThueGTGT,ct.thanhTien,ROUND(ct.thanhTien*ct.mucThueGTGT/100,0) AS tienThue"
                    +" FROM ChiTietPhieuKham ct JOIN DichVu dv ON dv.maDV=ct.maDV WHERE ct.maPK=? ORDER BY ct.maCTPK",
                    r -> new DongThanhToan(r.getInt(1),r.getString(2),r.getString(3),r.getString(4),r.getInt(5),r.getBigDecimal(6),r.getBigDecimal(7),r.getBigDecimal(8),r.getBigDecimal(9)),maPK); });
    }
    public boolean them(ChiTietPhieuKham ct) {
        can(ct!=null && ct.getPhieuKham()!=null,"Thiếu phiếu khám.");
        return context.transaction(c -> {
            TaiKhoan actor=quyen(c,VaiTro.NHASI); khoa(c,"BClinic:kham");
            PhieuKham pk=new PhieuKhamDAO(context).layTheoMa(c,ct.getPhieuKham().getMaPK());
            can(pk!=null,"Phiếu khám không tồn tại."); PhieuKhamDAO.kiemTraQuyen(pk,actor);
            them(c,pk,ct,actor); nhatKy(c,"THEM_CHI_TIET_KHAM",pk.getMaPK()); return true;
        });
    }
    void them(Connection c,PhieuKham pk,ChiTietPhieuKham ct,TaiKhoan actor) throws SQLException {
        can(pk.getTrangThai()==TrangThaiPhieuKham.DANG_KHAM,"Chỉ thêm điều trị khi phiếu đang khám.");
        can(ct.getDichVu()!=null && ct.getSoLuong()>0 && ct.getDonViTinh()!=null && !ct.getDonViTinh().isBlank()
                && ct.getTrieuChung()!=null && !ct.getTrieuChung().isBlank() && ct.getChanDoan()!=null && !ct.getChanDoan().isBlank(),"Chi tiết khám thiếu dịch vụ, triệu chứng, chẩn đoán hoặc số lượng.");
        DichVu dv=new DichVuDAO(context).layTheoMa(c,ct.getDichVu().getMaDV()); can(dv!=null && dv.isTrangThai(),"Dịch vụ không hoạt động.");
        try(PreparedStatement ps=c.prepareStatement("INSERT INTO ChiTietPhieuKham(maPK,maDV,maNV_NhaSi,trieuChung,chanDoan,ketQuaDieuTri,donViTinh,soLuong,donGia,mucThueGTGT) VALUES(?,?,?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
            bind(ps,pk.getMaPK(),dv.getMaDV(),actor.getNhanVien().getMaNV(),ct.getTrieuChung(),ct.getChanDoan(),ct.getKetQuaDieuTri(),ct.getDonViTinh(),ct.getSoLuong(),BigDecimal.valueOf(dv.getDonGia()),LoaiDichVuDAO.thue(dv.getLoaiDichVu().getMucThueGTGT()));
            ps.executeUpdate();
            try(ResultSet keys=ps.getGeneratedKeys()) { if(keys.next()) { ct.setMaCTPK(keys.getInt(1)); } }
        }
        ct.setDonGia(dv.getDonGia()); ct.setMucThueGTGT(LoaiDichVuDAO.thue(dv.getLoaiDichVu().getMucThueGTGT()));
    }
    private ChiTietPhieuKham mapRow(ResultSet r) throws SQLException { return DaoViews.chiTietPhieuKham(r); }
}
