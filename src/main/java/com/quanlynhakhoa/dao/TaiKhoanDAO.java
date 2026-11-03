package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.util.PasswordHasher;
import java.sql.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

/** UC001–003: giữ API đăng nhập/quên mật khẩu; mật khẩu mới lưu PBKDF2 có salt. */
public class TaiKhoanDAO extends DaoSupport {
    private static final ConcurrentHashMap<String,LocalDateTime> XAC_MINH = new ConcurrentHashMap<>();
    public TaiKhoanDAO() { }
    public TaiKhoanDAO(DaoContext context) { super(context); }

    public TaiKhoan dangNhap(String tenDangNhap,String matKhauNhap) {
        can(tenDangNhap!=null && !tenDangNhap.isBlank() && matKhauNhap!=null && !matKhauNhap.isBlank(),"Vui lòng nhập tên đăng nhập và mật khẩu.");
        return context.transaction(c -> {
            String stored=one(c,"SELECT matKhau FROM TaiKhoan WITH(UPDLOCK,HOLDLOCK) WHERE tenTK=?",r -> r.getString(1),tenDangNhap.trim());
            can(PasswordHasher.xacMinh(matKhauNhap,stored),"Tên đăng nhập hoặc mật khẩu chưa chính xác.");
            TaiKhoan tk=one(c,DaoViews.TAI_KHOAN+" WHERE a.tenTK=?",this::mapRow,tenDangNhap.trim());
            can(tk!=null && tk.isTrangThai(),"Tài khoản đã bị khóa. Vui lòng liên hệ quản lý.");
            if(!stored.startsWith("PBKDF2$")) { update(c,"UPDATE TaiKhoan SET matKhau=? WHERE maTK=?",PasswordHasher.bam(matKhauNhap),tk.getMaTK()); }
            update(c,"INSERT INTO NhatKyHeThong(maNhatKy,maTK,thoiDiem,hanhDong,noiDungThayDoi) VALUES(?,?,?,?,?)",
                    maMoi(c,Bang.NHAT_KY,null),tk.getMaTK(),context.bayGio(),"DANG_NHAP","Đăng nhập thành công.");
            return tk;
        });
    }
    /** Tương thích UI hiện tại; mã NV đơn lẻ chưa phải yếu tố xác minh danh tính mạnh. */
    public String xacMinhMaNV(String maNV) {
        can(maNV!=null && !maNV.isBlank(),"Vui lòng nhập mã nhân viên.");
        String ma=context.doc(c -> one(c,"SELECT maTK FROM TaiKhoan WHERE maNV=? AND trangThai=1",r -> r.getString(1),maNV.trim()));
        can(ma!=null,"Mã nhân viên không hợp lệ.");
        XAC_MINH.entrySet().removeIf(e -> e.getValue().isBefore(context.bayGio()));
        XAC_MINH.put(ma,context.bayGio().plusMinutes(5)); return ma;
    }
    public void doiMatKhau(String maTK,String matKhauMoi) {
        TaiKhoan.kiemTraMatKhauGoc(matKhauMoi);
        context.transaction(c -> {
            LocalDateTime han=XAC_MINH.get(maTK);
            if(han==null || !han.isAfter(context.bayGio())) {
                TaiKhoan actor=quyen(c);
                can(actor.getMaTK().equals(maTK) || actor.getVaiTro()==VaiTro.QUANLY,"Không được đổi mật khẩu tài khoản khác.");
            }
            can(update(c,"UPDATE TaiKhoan SET matKhau=? WHERE maTK=? AND trangThai=1",PasswordHasher.bam(matKhauMoi),maTK)==1,"Tài khoản không tồn tại hoặc đã bị khóa.");
            update(c,"INSERT INTO NhatKyHeThong(maNhatKy,maTK,thoiDiem,hanhDong,noiDungThayDoi) VALUES(?,?,?,?,?)",
                    maMoi(c,Bang.NHAT_KY,null),maTK,context.bayGio(),"DOI_MAT_KHAU","Đã đổi mật khẩu; không ghi nội dung mật khẩu.");
            return null;
        }); XAC_MINH.remove(maTK);
    }
    public List<TaiKhoan> layTatCa() {
        return context.doc(c -> { quyen(c,VaiTro.QUANLY); return query(c,DaoViews.TAI_KHOAN+" ORDER BY a.maTK",this::mapRow); });
    }
    public TaiKhoan layTheoMa(String ma) {
        return context.doc(c -> { TaiKhoan actor=quyen(c); can(actor.getVaiTro()==VaiTro.QUANLY || actor.getMaTK().equals(ma),"Không được xem tài khoản khác.");
            return one(c,DaoViews.TAI_KHOAN+" WHERE a.maTK=?",this::mapRow,ma); });
    }
    public List<TaiKhoan> timKiem(String ten) {
        return context.doc(c -> { quyen(c,VaiTro.QUANLY); return query(c,DaoViews.TAI_KHOAN
                +" WHERE a.tenTK LIKE ? OR a_nv.hoTenNV LIKE ? ORDER BY a.maTK",this::mapRow,tuKhoa(ten),tuKhoa(ten)); });
    }
    public boolean them(TaiKhoan tk) {
        return context.transaction(c -> { quyen(c,VaiTro.QUANLY); them(c,tk); nhatKy(c,"TAO_TAI_KHOAN",tk.getMaTK()); return true; });
    }
    void them(Connection c,TaiKhoan tk) throws SQLException {
        new TaiKhoan(null,tk.getTenTK(),tk.getMatKhau(),tk.getVaiTro(),tk.isTrangThai(),tk.getNhanVien());
        TaiKhoan.kiemTraMatKhauGoc(tk.getMatKhau());
        String ma=maMoi(c,Bang.TAI_KHOAN,null);
        update(c,"INSERT INTO TaiKhoan(maTK,tenTK,matKhau,vaiTro,trangThai,maNV) VALUES(?,?,?,?,?,?)",
                ma,tk.getTenTK(),PasswordHasher.bam(tk.getMatKhau()),tk.getVaiTro(),tk.isTrangThai(),tk.getNhanVien().getMaNV());
        tk.setMaTK(ma);
    }
    /** Tạo nhân viên và tài khoản nguyên tử; lỗi trùng tên rollback cả nhân viên. */
    public boolean taoNhanVienVaTaiKhoan(NhanVien nv,TaiKhoan tk) {
        String maCu=nv.getMaNV(),maTkCu=tk.getMaTK();
        try {
            return context.transaction(c -> { quyen(c,VaiTro.QUANLY);
                new NhanVienDAO(context).them(c,nv); tk.setNhanVien(nv); them(c,tk);
                nhatKy(c,"TAO_NHAN_VIEN_TAI_KHOAN",nv.getMaNV()+" / "+tk.getMaTK()); return true; });
        } catch(RuntimeException e) { nv.setMaNV(maCu); tk.setMaTK(maTkCu); throw e; }
    }
    public boolean capNhat(TaiKhoan tk) {
        return context.transaction(c -> {
            TaiKhoan actor=quyen(c,VaiTro.QUANLY); khoa(c,"BClinic:lich"); khoa(c,"BClinic:tai-khoan");
            can(!(actor.getMaTK().equals(tk.getMaTK()) && (!tk.isTrangThai() || tk.getVaiTro()!=VaiTro.QUANLY)),"Không được tự khóa hoặc hạ quyền tài khoản đang đăng nhập.");
            if(!tk.isTrangThai() || tk.getVaiTro()!=VaiTro.NHASI) {
                can(!exists(c,"SELECT 1 FROM LichHen WHERE maNV=(SELECT maNV FROM TaiKhoan WHERE maTK=?) AND trangThai IN('DA_DAT','DA_DEN')",tk.getMaTK()),"Nhân viên còn lịch khám chưa xử lý.");
            }
            boolean ok=update(c,"UPDATE TaiKhoan SET tenTK=?,vaiTro=?,trangThai=? WHERE maTK=?",tk.getTenTK(),tk.getVaiTro(),tk.isTrangThai(),tk.getMaTK())==1;
            if(ok) { nhatKy(c,"CAP_NHAT_TAI_KHOAN",tk.getMaTK()); } return ok;
        });
    }
    public boolean khoaTaiKhoan(String ma) {
        TaiKhoan tk=layTheoMa(ma); can(tk!=null,"Tài khoản không tồn tại."); tk.setTrangThai(false); return capNhat(tk);
    }
    public boolean xoa(String ma) { return khoaTaiKhoan(ma); }
    private TaiKhoan mapRow(ResultSet r) throws SQLException { return DaoViews.taiKhoan(r); }
}
