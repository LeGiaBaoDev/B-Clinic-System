package com.quanlynhakhoa.dao;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.VaiTro;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

/** UC009–011: thêm khách và hồ sơ cùng transaction; ngày sinh khóa sau khi tạo. */
public class KhachHangDAO extends DaoSupport {
    public KhachHangDAO() { }
    public KhachHangDAO(DaoContext context) { super(context); }
    public List<KhachHang> layTatCa() { return timKiem(""); }
    public List<KhachHang> timKiem(String text) {
        return context.doc(c -> { TaiKhoan actor=quyen(c);
            String scope=actor.getVaiTro()==VaiTro.NHASI ? " AND EXISTS(SELECT 1 FROM LichHen lh WHERE lh.maKH=a.maKH AND lh.maNV=?)" : "";
            String sql=DaoViews.KHACH_HANG+" WHERE (a.maKH LIKE ? OR a.tenKH LIKE ? OR a.sdt LIKE ?)"+scope+" ORDER BY a.maKH";
            return actor.getVaiTro()==VaiTro.NHASI ? query(c,sql,this::mapRow,tuKhoa(text),tuKhoa(text),tuKhoa(text),actor.getNhanVien().getMaNV())
                    : query(c,sql,this::mapRow,tuKhoa(text),tuKhoa(text),tuKhoa(text)); });
    }
    public KhachHang layTheoMa(String ma) {
        return context.doc(c -> { TaiKhoan actor=quyen(c); if(actor.getVaiTro()==VaiTro.NHASI) {
                can(exists(c,"SELECT 1 FROM LichHen WHERE maKH=? AND maNV=?",ma,actor.getNhanVien().getMaNV()),"Bệnh nhân chưa được phân công cho bạn."); }
            return layTheoMa(c,ma); });
    }
    KhachHang layTheoMa(Connection c,String ma) throws SQLException { return one(c,DaoViews.KHACH_HANG+" WHERE a.maKH=?",this::mapRow,ma); }
    public List<KhachHang> timTrung(String ten,LocalDate ngaySinh,String sdt) {
        return context.doc(c -> { quyen(c,VaiTro.LETAN,VaiTro.QUANLY); return query(c,DaoViews.KHACH_HANG
                +" WHERE a.sdt=? OR (a.tenKH LIKE ? AND (? IS NULL OR a.ngaySinh=?))",this::mapRow,sdt,tuKhoa(ten),ngaySinh,ngaySinh); });
    }
    public boolean them(KhachHang kh) { return them(kh,null); }
    public boolean them(KhachHang kh,String tienSuBenh) {
        String cu=kh.getMaKH();
        try { return context.transaction(c -> {
            quyen(c,VaiTro.LETAN,VaiTro.QUANLY);
            new KhachHang(null,kh.getTenKH(),kh.getNgaySinh(),kh.isGioiTinh(),kh.getSdt(),kh.getDiaChi(),kh.getEmail());
            String ma=maMoi(c,Bang.KHACH_HANG,null);
            update(c,"INSERT INTO KhachHang(maKH,tenKH,ngaySinh,gioiTinh,sdt,diaChi,email) VALUES(?,?,?,?,?,?,?)",
                    ma,kh.getTenKH(),kh.getNgaySinh(),kh.isGioiTinh(),kh.getSdt(),kh.getDiaChi(),kh.getEmail());
            kh.setMaKH(ma); new HoSoBenhAnDAO(context).them(c,new HoSoBenhAn(null,tienSuBenh,kh));
            nhatKy(c,"THEM_KHACH_HANG",ma); return true;
        }); } catch(RuntimeException e) { kh.setMaKH(cu); throw e; }
    }
    public boolean capNhat(KhachHang kh) {
        return context.transaction(c -> {
            quyen(c,VaiTro.LETAN,VaiTro.QUANLY);
            KhachHang cu=layTheoMa(c,kh.getMaKH()); can(cu!=null,"Khách hàng không tồn tại.");
            can(java.util.Objects.equals(cu.getNgaySinh(),kh.getNgaySinh()),"Không được thay đổi ngày sinh đã lưu.");
            boolean ok=update(c,"UPDATE KhachHang SET tenKH=?,gioiTinh=?,sdt=?,diaChi=?,email=? WHERE maKH=?",
                    kh.getTenKH(),kh.isGioiTinh(),kh.getSdt(),kh.getDiaChi(),kh.getEmail(),kh.getMaKH())==1;
            if(ok) { nhatKy(c,"CAP_NHAT_KHACH_HANG",kh.getMaKH()); } return ok;
        });
    }
    private KhachHang mapRow(ResultSet r) throws SQLException { return DaoViews.khachHang(r); }
}
