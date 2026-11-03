package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

/** UC021–024/027: tiếp nhận, khám, khóa phiếu và tạo hóa đơn trong cùng transaction. */
public class PhieuKhamDAO extends DaoSupport {
    public PhieuKhamDAO() { }
    public PhieuKhamDAO(DaoContext context) { super(context); }
    public List<PhieuKham> layTatCa() { return timKiem(LocalDate.of(2000,1,1),LocalDate.of(2100,1,1)); }
    public List<PhieuKham> timKiem(LocalDate tu,LocalDate den) {
        khoangNgay(tu,den);
        return context.doc(c -> { TaiKhoan actor=quyen(c); return query(c,DaoViews.PHIEU_KHAM
                +" WHERE a.ngayLap>=? AND a.ngayLap<? AND (?=1 OR a_lh.maNV=?) ORDER BY a.ngayLap DESC",
                r -> ClinicalPrivacy.loc(mapRow(r),actor.getVaiTro()),tu.atStartOfDay(),den.plusDays(1).atStartOfDay(),
                actor.getVaiTro()!=VaiTro.NHASI,actor.getNhanVien().getMaNV()); });
    }
    public PhieuKham layTheoMa(String ma) {
        return context.doc(c -> { TaiKhoan actor=quyen(c); PhieuKham pk=layTheoMa(c,ma); kiemTraQuyen(pk,actor);
            return ClinicalPrivacy.loc(pk,actor.getVaiTro()); });
    }
    PhieuKham layTheoMa(Connection c,String ma) throws SQLException {
        return one(c,DaoViews.PHIEU_KHAM+" WHERE a.maPK=?",this::mapRow,ma);
    }
    static void kiemTraQuyen(PhieuKham pk,TaiKhoan actor) {
        if(pk!=null && actor.getVaiTro()==VaiTro.NHASI) {
            can(pk.getLichHen()!=null && pk.getLichHen().getNhaSi().getMaNV().equals(actor.getNhanVien().getMaNV()),"Phiếu khám không được phân công cho bạn.");
        }
    }
    /** UC021: chỉ tiếp nhận lịch cùng ngày; không nhận lịch đã hủy/không đến/hoàn tất. */
    public PhieuKham taoTuLichHen(String maLH) {
        return context.transaction(c -> {
            TaiKhoan actor=quyen(c,VaiTro.LETAN,VaiTro.QUANLY); khoa(c,"BClinic:lich");
            LichHen lh=new LichHenDAO(context).layTheoMa(c,maLH);
            can(lh!=null && lh.getTrangThai()==TrangThaiLichHen.DA_DAT,"Lịch hẹn không ở trạng thái đã đặt.");
            can(lh.getNgayGioHen().toLocalDate().equals(context.bayGio().toLocalDate()),"Chỉ tiếp nhận lịch của ngày hiện tại.");
            HoSoBenhAn hs=one(c,DaoViews.HO_SO+" WHERE a.maKH=?",DaoViews::hoSoBenhAn,lh.getKhachHang().getMaKH());
            can(hs!=null,"Khách hàng chưa có hồ sơ bệnh án.");
            String ma=maMoi(c,Bang.PHIEU_KHAM,context.bayGio().toLocalDate());
            update(c,"INSERT INTO PhieuKham(maPK,maHSBA,maLH,maNV_LapPhieu,ngayLap,trangThai) VALUES(?,?,?,?,?,'CHO_KHAM')",
                    ma,hs.getMaHSBA(),maLH,actor.getNhanVien().getMaNV(),context.bayGio());
            can(update(c,"UPDATE LichHen SET trangThai='DA_DEN' WHERE maLH=? AND trangThai='DA_DAT'",maLH)==1,"Lịch hẹn vừa được người khác xử lý.");
            nhatKy(c,"LAP_PHIEU_KHAM",ma);
            return ClinicalPrivacy.loc(layTheoMa(c,ma),actor.getVaiTro());
        });
    }
    public boolean batDauKham(String maPK) {
        return context.transaction(c -> {
            TaiKhoan actor=quyen(c,VaiTro.NHASI); khoa(c,"BClinic:kham");
            PhieuKham pk=layTheoMa(c,maPK); can(pk!=null,"Phiếu khám không tồn tại."); kiemTraQuyen(pk,actor);
            can(update(c,"UPDATE PhieuKham SET trangThai='DANG_KHAM' WHERE maPK=? AND trangThai='CHO_KHAM'",maPK)==1,"Chỉ bắt đầu phiếu đang chờ khám.");
            nhatKy(c,"BAT_DAU_KHAM",maPK); return true;
        });
    }
    /** dsThemMoi chỉ chứa dòng chưa lưu (maCTPK=0); dòng đã lưu được giữ nguyên. */
    public void hoanTatPhieuKham(PhieuKham pk,List<ChiTietPhieuKham> dsThemMoi) {
        can(pk!=null && dsThemMoi!=null,"Thiếu phiếu khám hoặc danh sách điều trị.");
        can(dsThemMoi.stream().allMatch(java.util.Objects::nonNull),"Danh sách chứa chi tiết rỗng.");
        List<Integer> maCu=dsThemMoi.stream().map(ChiTietPhieuKham::getMaCTPK).toList();
        try {
        context.transaction(c -> {
            TaiKhoan actor=quyen(c,VaiTro.NHASI); khoa(c,"BClinic:kham");
            PhieuKham stored=layTheoMa(c,pk.getMaPK()); can(stored!=null,"Phiếu khám không tồn tại."); kiemTraQuyen(stored,actor);
            can(stored.getTrangThai()==TrangThaiPhieuKham.DANG_KHAM,"Phiếu phải đang khám và chưa bị khóa.");
            ChiTietPhieuKhamDAO ctDAO=new ChiTietPhieuKhamDAO(context);
            for(ChiTietPhieuKham ct:dsThemMoi) {
                can(ct.getMaCTPK()==0,"Chỉ truyền chi tiết chưa lưu khi hoàn tất phiếu.");
                ctDAO.them(c,stored,ct,actor);
            }
            can(exists(c,"SELECT 1 FROM ChiTietPhieuKham WHERE maPK=?",stored.getMaPK()),"Phiếu khám phải có ít nhất một dịch vụ điều trị.");
            update(c,"UPDATE PhieuKham SET loiDan=?,trangThai='HOAN_TAT' WHERE maPK=? AND trangThai='DANG_KHAM'",pk.getLoiDan(),stored.getMaPK());
            new HoaDonDAO(context).taoTuPhieuKham(c,stored.getMaPK());
            nhatKy(c,"HOAN_TAT_KHAM",stored.getMaPK()); return null;
        });
        } catch(RuntimeException e) {
            for(int i=0;i<dsThemMoi.size();i++) { dsThemMoi.get(i).setMaCTPK(maCu.get(i)); }
            throw e;
        }
    }
    private PhieuKham mapRow(ResultSet r) throws SQLException { return DaoViews.phieuKham(r); }
}
