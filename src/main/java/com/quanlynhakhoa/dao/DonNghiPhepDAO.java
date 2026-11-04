package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.*;
import com.quanlynhakhoa.entity.LichLamViec;
import com.quanlynhakhoa.entity.enums.*;
import com.quanlynhakhoa.exception.BusinessException;
import java.sql.*;
import java.time.*;
import java.util.List;

/** UC015: nhân viên gửi đơn của mình; Quản lý xử lý đơn đã được lưu. */
public class DonNghiPhepDAO {
    /** Kết quả truy vấn chỉ đọc, chứa bản chụp ca tại thời điểm gửi đơn. */
    public record Don(String maDon,LichLamViec ca,String lyDo,LocalDateTime ngayGui,
            String trangThai,String nguoiXuLy,LocalDateTime ngayXuLy,String lyDoTuChoi) { }
    private static final String SELECT="SELECT d.*,nv.hoTenNV AS tenNguoiXuLy FROM DonNghiPhep d "
            +"LEFT JOIN TaiKhoan tk ON tk.maTK=d.maTKXuLy LEFT JOIN NhanVien nv ON nv.maNV=tk.maNV ";
    private final LichLamViecDAO lich=new LichLamViecDAO();

    public String guiDon(String maLLV,String lyDo) {
        return JdbcHelper.transaction(c->{
            var actor=PhanQuyen.quyen(c);MaSinh.khoa(c,"BClinic:lich");String reason=lyDo(lyDo);
            var ca=lich.layTheoMa(c,maLLV);
            can(ca!=null&&actor.maNV().equals(ca.getNhanVien().getMaNV()),"Chỉ được xin nghỉ ca của bản thân.");
            can(ca.getTrangThai()==TrangThaiLichLamViec.DA_PHAN_CONG,"Ca đã nghỉ phép.");
            can(ca.getNgayTruc().atTime(ca.getGioKetThuc()).isAfter(LocalDateTime.now()),"Không xin nghỉ ca đã kết thúc.");
            can(!JdbcHelper.exists(c,"SELECT 1 FROM DonNghiPhep WITH(UPDLOCK,HOLDLOCK) WHERE maLLV=? AND trangThai='CHO_DUYET'",maLLV),"Ca này đã có đơn chờ duyệt.");
            String id=MaSinh.maMoi(c,MaSinh.Bang.DON_NGHI_PHEP,null);
            JdbcHelper.update(c,"INSERT INTO DonNghiPhep(maDon,maLLV,maNV,maTKGui,ngayTruc,caTruc,gioBatDau,gioKetThuc,lyDo) VALUES(?,?,?,?,?,?,?,?,?)",
                    id,maLLV,actor.maNV(),actor.maTK(),ca.getNgayTruc(),ca.getCaTruc(),ca.getGioBatDau(),ca.getGioKetThuc(),reason);
            NhatKy.ghi(c,"GUI_DON_NGHI_PHEP",id+" "+actor.maNV());return id;
        });
    }
    public List<Don> layDonCuaToi() {
        return JdbcHelper.doc(c->{var actor=PhanQuyen.quyen(c);return JdbcHelper.query(c,SELECT+"WHERE d.maNV=? ORDER BY d.ngayGui DESC,d.maDon DESC",r->map(c,r),actor.maNV());});
    }
    public List<Don> layDonQuanLy() {
        return JdbcHelper.doc(c->{PhanQuyen.quyen(c,VaiTro.QUANLY);return JdbcHelper.query(c,SELECT+"ORDER BY d.ngayGui,d.maDon",r->map(c,r));});
    }
    public List<LichLamViecDAO.LichHenAnhHuong> kiemTra(String maDon) {
        return JdbcHelper.transaction(c->{PhanQuyen.quyen(c,VaiTro.QUANLY);MaSinh.khoa(c,"BClinic:lich");var d=choDuyet(c,maDon);return lich.lichHenAnhHuong(c,lich.caXinNghi(c,d.ca(),d.lyDo()));});
    }
    public boolean duyet(String maDon) {
        return JdbcHelper.transaction(c->{
            var actor=PhanQuyen.quyen(c,VaiTro.QUANLY);MaSinh.khoa(c,"BClinic:lich");var d=choDuyet(c,maDon);
            var ca=lich.caXinNghi(c,d.ca(),d.lyDo());
            can(lich.lichHenAnhHuong(c,ca).isEmpty(),"Ca còn lịch hẹn đã đặt/đã đến. Cần dời hoặc hủy lịch hẹn trước khi duyệt.");
            can(JdbcHelper.update(c,"UPDATE LichLamViec SET trangThai='NGHI_PHEP',ghiChuNghi=? WHERE maLLV=?",d.lyDo(),ca.getMaLLV())==1,"Không thể cập nhật ca.");
            ketQua(c,d,actor.maTK(),true,null);lich.ghiKetQuaNghi(c,ca,d.lyDo(),true);return true;
        });
    }
    public boolean tuChoi(String maDon,String lyDoTuChoi) {
        return JdbcHelper.transaction(c->{
            var actor=PhanQuyen.quyen(c,VaiTro.QUANLY);MaSinh.khoa(c,"BClinic:lich");String reason=lyDo(lyDoTuChoi);var d=choDuyet(c,maDon);
            // Vẫn được từ chối đơn cũ nếu ca đã đổi hoặc kết thúc.
            ketQua(c,d,actor.maTK(),false,reason);lich.ghiKetQuaNghi(c,d.ca(),d.lyDo()+"; Lý do từ chối: "+reason,false);return true;
        });
    }
    private void ketQua(Connection c,Don d,String maTK,boolean approved,String reason) throws SQLException {
        can(JdbcHelper.update(c,"UPDATE DonNghiPhep SET trangThai=?,maTKXuLy=?,ngayXuLy=SYSDATETIME(),lyDoTuChoi=? WHERE maDon=? AND trangThai='CHO_DUYET'",
                approved?"DA_DUYET":"TU_CHOI",maTK,reason,d.maDon())==1,"Đơn đã được xử lý. Hãy tải lại.");
    }
    private Don choDuyet(Connection c,String maDon) throws SQLException {
        var d=JdbcHelper.one(c,SELECT+"WHERE d.maDon=?",r->map(c,r),maDon);
        can(d!=null&&d.trangThai().equals("CHO_DUYET"),"Đơn không tồn tại hoặc đã được xử lý.");return d;
    }
    private Don map(Connection c,ResultSet r) throws SQLException {
        var ca=new LichLamViec(r.getString("maLLV"),r.getObject("ngayTruc",LocalDate.class),CaTruc.valueOf(r.getString("caTruc")),
                r.getObject("gioBatDau",LocalTime.class),r.getObject("gioKetThuc",LocalTime.class),TrangThaiLichLamViec.DA_PHAN_CONG,null,new NhanVienDAO().layTheoMa(c,r.getString("maNV")));
        return new Don(r.getString("maDon"),ca,r.getString("lyDo"),r.getObject("ngayGui",LocalDateTime.class),r.getString("trangThai"),r.getString("tenNguoiXuLy"),r.getObject("ngayXuLy",LocalDateTime.class),r.getString("lyDoTuChoi"));
    }
    private static String lyDo(String s){can(s!=null&&!s.isBlank()&&s.strip().length()<=255,"Lý do bắt buộc nhập, tối đa 255 ký tự.");return s.strip();}
    private static void can(boolean ok,String message){if(!ok)throw new BusinessException(message);}
}
