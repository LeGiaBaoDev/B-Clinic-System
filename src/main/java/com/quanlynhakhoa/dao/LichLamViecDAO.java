package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import java.sql.*;
import java.time.*;
import java.util.List;

/** UC012–015: quản lý phân công/đổi/duyệt nghỉ; nhân viên chỉ xem lịch mình. */
public class LichLamViecDAO extends DaoSupport {
    public LichLamViecDAO() { }
    public LichLamViecDAO(DaoContext context) { super(context); }
    public List<LichLamViec> layTatCa() {
        return context.doc(c -> { TaiKhoan actor=quyen(c); return query(c,DaoViews.LICH_LAM_VIEC
                +" WHERE (?=1 OR a.maNV=?) ORDER BY a.ngayTruc,a.gioBatDau",this::mapRow,
                actor.getVaiTro()==VaiTro.QUANLY,actor.getNhanVien().getMaNV()); });
    }
    public List<LichLamViec> layTheoNhanVien(String maNV,LocalDate tu,LocalDate den) {
        khoangNgay(tu,den);
        return context.doc(c -> { TaiKhoan actor=quyen(c); can(actor.getVaiTro()==VaiTro.QUANLY || actor.getNhanVien().getMaNV().equals(maNV),"Chỉ được xem lịch làm việc của mình.");
            return query(c,DaoViews.LICH_LAM_VIEC+" WHERE a.maNV=? AND a.ngayTruc BETWEEN ? AND ? ORDER BY a.ngayTruc,a.gioBatDau",this::mapRow,maNV,tu,den); });
    }
    public LichLamViec layTheoMa(String ma) {
        return context.doc(c -> { TaiKhoan actor=quyen(c); LichLamViec ll=layTheoMa(c,ma);
            can(ll==null || actor.getVaiTro()==VaiTro.QUANLY || ll.getNhanVien().getMaNV().equals(actor.getNhanVien().getMaNV()),"Chỉ được xem lịch làm việc của mình."); return ll; });
    }
    private LichLamViec layTheoMa(Connection c,String ma) throws SQLException { return one(c,DaoViews.LICH_LAM_VIEC+" WHERE a.maLLV=?",this::mapRow,ma); }
    public boolean them(LichLamViec ll) { return luu(ll,true); }
    public boolean capNhat(LichLamViec ll) { return luu(ll,false); }
    private boolean luu(LichLamViec ll,boolean them) {
        return context.transaction(c -> {
            quyen(c,VaiTro.QUANLY); khoa(c,"BClinic:lich");
            new LichLamViec(ll.getMaLLV(),ll.getNgayTruc(),ll.getCaTruc(),ll.getGioBatDau(),ll.getGioKetThuc(),ll.getTrangThai(),ll.getGhiChuNghi(),ll.getNhanVien());
            can(!ll.getNgayTruc().isBefore(context.bayGio().toLocalDate()),"Không thay đổi ca trong quá khứ.");
            LocalTime min=ll.getCaTruc()==CaTruc.CA_CHIEU ? LocalTime.of(13,30) : LocalTime.of(7,0);
            LocalTime max=ll.getCaTruc()==CaTruc.CA_SANG ? LocalTime.NOON : LocalTime.of(17,30);
            can(!ll.getGioBatDau().isBefore(min) && !ll.getGioKetThuc().isAfter(max),"Giờ làm việc nằm ngoài khung của ca.");
            can(exists(c,"SELECT 1 FROM TaiKhoan WHERE maNV=? AND trangThai=1",ll.getNhanVien().getMaNV()),"Nhân viên không có tài khoản hoạt động.");
            String ma=them ? maMoi(c,Bang.LICH_LAM_VIEC,null) : ll.getMaLLV();
            if(!them) { LichLamViec old=layTheoMa(c,ma); can(old!=null,"Ca làm việc không tồn tại.");
                can(!old.getNgayTruc().isBefore(context.bayGio().toLocalDate()),"Không thay đổi lịch sử ca trực."); }
            can(!exists(c,"SELECT 1 FROM LichLamViec WHERE maNV=? AND ngayTruc=? AND maLLV<>? AND gioBatDau<? AND gioKetThuc>?",
                    ll.getNhanVien().getMaNV(),ll.getNgayTruc(),ma,ll.getGioKetThuc(),ll.getGioBatDau()),"Nhân viên đã có ca trùng giờ hoặc ca nghỉ phép.");
            if(!them) {
                // Tat ca lich dang hoat dong duoc ca cu bao phu van phai co ca moi/ca khac bao phu.
                LichLamViec old=layTheoMa(c,ma);
                var lich=query(c,"SELECT lh.ngayGioHen,dv.thoiLuong FROM LichHen lh JOIN DichVu dv ON dv.maDV=lh.maDV"
                        +" WHERE lh.maNV=? AND CAST(lh.ngayGioHen AS DATE)=? AND lh.trangThai IN('DA_DAT','DA_DEN')",
                        r -> new Object[]{r.getObject(1,LocalDateTime.class),r.getInt(2)},old.getNhanVien().getMaNV(),old.getNgayTruc());
                for(Object[] row:lich) {
                    LocalDateTime start=(LocalDateTime)row[0],end=start.plusMinutes((int)row[1]);
                    boolean covered=ll.getNhanVien().getMaNV().equals(old.getNhanVien().getMaNV())
                            && ll.getNgayTruc().equals(start.toLocalDate()) && ll.getTrangThai()==TrangThaiLichLamViec.DA_PHAN_CONG
                            && !start.toLocalTime().isBefore(ll.getGioBatDau()) && !end.toLocalTime().isAfter(ll.getGioKetThuc());
                    can(covered || exists(c,"SELECT 1 FROM LichLamViec WHERE maNV=? AND ngayTruc=? AND maLLV<>? AND trangThai='DA_PHAN_CONG' AND gioBatDau<=? AND gioKetThuc>=?",
                            old.getNhanVien().getMaNV(),start.toLocalDate(),ma,start.toLocalTime(),end.toLocalTime()),"Cần dời lịch hẹn trước khi đổi ca hoặc duyệt nghỉ.");
                }
            }
            Object[] args={ll.getNhanVien().getMaNV(),ll.getNgayTruc(),ll.getCaTruc(),ll.getGioBatDau(),ll.getGioKetThuc(),ll.getTrangThai(),ll.getGhiChuNghi(),ma};
            int rows=update(c,them ? "INSERT INTO LichLamViec(maNV,ngayTruc,caTruc,gioBatDau,gioKetThuc,trangThai,ghiChuNghi,maLLV) VALUES(?,?,?,?,?,?,?,?)"
                    : "UPDATE LichLamViec SET maNV=?,ngayTruc=?,caTruc=?,gioBatDau=?,gioKetThuc=?,trangThai=?,ghiChuNghi=? WHERE maLLV=?",args);
            if(rows==1) { ll.setMaLLV(ma); nhatKy(c,"PHAN_CONG_LICH",ma); } return rows==1;
        });
    }
    public boolean duyetNghiPhep(String ma,String lyDo) {
        LichLamViec ll=layTheoMa(ma); can(ll!=null,"Ca làm việc không tồn tại."); ll.nghiPhep(lyDo); return capNhat(ll);
    }
    private LichLamViec mapRow(ResultSet r) throws SQLException { return DaoViews.lichLamViec(r); }
}
