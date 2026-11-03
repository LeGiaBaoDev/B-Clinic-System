package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.config.QuyDinhLichHen;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import java.sql.*;
import java.time.*;
import java.util.List;

/** UC016–020: đặt/dời/hủy/tái khám; khóa transaction để chống đặt trùng đồng thời. */
public class LichHenDAO extends DaoSupport {
    private final QuyDinhLichHen quyDinh;
    public LichHenDAO() { this(DaoContext.macDinh(),QuyDinhLichHen.macDinh()); }
    public LichHenDAO(DaoContext context) { this(context,QuyDinhLichHen.macDinh()); }
    public LichHenDAO(DaoContext context,QuyDinhLichHen quyDinh) { super(context); this.quyDinh=java.util.Objects.requireNonNull(quyDinh); }
    public List<LichHen> layTatCa() { return timKiem(LocalDate.of(2000,1,1),LocalDate.of(2100,1,1),null); }
    public List<LichHen> timKiem(LocalDate tu,LocalDate den,TrangThaiLichHen trangThai) {
        khoangNgay(tu,den);
        return context.doc(c -> { TaiKhoan actor=quyen(c); return query(c,DaoViews.LICH_HEN
                +" WHERE a.ngayGioHen>=? AND a.ngayGioHen<? AND (? IS NULL OR a.trangThai=?) AND (?=1 OR a.maNV=?) ORDER BY a.ngayGioHen",
                r -> ClinicalPrivacy.loc(mapRow(r),actor.getVaiTro()),tu.atStartOfDay(),den.plusDays(1).atStartOfDay(),trangThai,trangThai,
                actor.getVaiTro()!=VaiTro.NHASI,actor.getNhanVien().getMaNV()); });
    }
    public LichHen layTheoMa(String ma) {
        return context.doc(c -> { TaiKhoan actor=quyen(c); LichHen lh=layTheoMa(c,ma);
            can(lh==null || actor.getVaiTro()!=VaiTro.NHASI || lh.getNhaSi().getMaNV().equals(actor.getNhanVien().getMaNV()),"Lịch chưa được phân công cho bạn.");
            return ClinicalPrivacy.loc(lh,actor.getVaiTro()); });
    }
    LichHen layTheoMa(Connection c,String ma) throws SQLException { return one(c,DaoViews.LICH_HEN+" WHERE a.maLH=?",this::mapRow,ma); }
    public boolean them(LichHen lh) { return luu(lh,true); }
    public boolean capNhat(LichHen lh) { return luu(lh,false); }
    private boolean luu(LichHen lh,boolean them) {
        return context.transaction(c -> {
            quyen(c,VaiTro.LETAN,VaiTro.QUANLY); khoa(c,"BClinic:lich");
            if(!them) { LichHen old=layTheoMa(c,lh.getMaLH()); can(old!=null && old.getTrangThai()==TrangThaiLichHen.DA_DAT,"Chỉ được dời lịch đang ở trạng thái đã đặt."); }
            can(lh.getTrangThai()==TrangThaiLichHen.DA_DAT,"Đặt/dời lịch phải ở trạng thái đã đặt.");
            kiemTra(c,lh,them ? "" : lh.getMaLH());
            String ma=them ? maMoi(c,Bang.LICH_HEN,lh.getNgayGioHen().toLocalDate()) : lh.getMaLH();
            Object[] args={lh.getKhachHang().getMaKH(),lh.getNhaSi().getMaNV(),lh.getDichVu().getMaDV(),
                    lh.getPhieuKhamNguon()==null?null:lh.getPhieuKhamNguon().getMaPK(),lh.getNgayGioHen(),lh.getGhiChu(),ma};
            int rows=update(c,them ? "INSERT INTO LichHen(maKH,maNV,maDV,maPKNguon,ngayGioHen,ghiChu,maLH) VALUES(?,?,?,?,?,?,?)"
                    : "UPDATE LichHen SET maKH=?,maNV=?,maDV=?,maPKNguon=?,ngayGioHen=?,ghiChu=? WHERE maLH=? AND trangThai='DA_DAT'",args);
            if(rows==1) { lh.setMaLH(ma); nhatKy(c,them?"DAT_LICH":"DOI_LICH",ma); } return rows==1;
        });
    }
    private void kiemTra(Connection c,LichHen lh,String boQua) throws SQLException {
        can(lh.getKhachHang()!=null && lh.getNhaSi()!=null && lh.getDichVu()!=null && lh.getNgayGioHen()!=null,"Thiếu khách hàng, nha sĩ, dịch vụ hoặc thời gian hẹn.");
        can(lh.getNgayGioHen().isAfter(context.bayGio()),"Ngày giờ hẹn phải ở tương lai.");
        DichVu dv=new DichVuDAO(context).layTheoMa(c,lh.getDichVu().getMaDV());
        can(dv!=null && dv.isTrangThai(),"Dịch vụ không hoạt động.");
        can(exists(c,"SELECT 1 FROM KhachHang WHERE maKH=?",lh.getKhachHang().getMaKH()),"Khách hàng không tồn tại.");
        can(exists(c,"SELECT 1 FROM TaiKhoan WHERE maNV=? AND vaiTro='NHASI' AND trangThai=1",lh.getNhaSi().getMaNV()),"Nhân viên không phải nha sĩ đang hoạt động.");
        LocalDateTime start=lh.getNgayGioHen(),end=start.plusMinutes(dv.getThoiLuong());
        can(start.toLocalDate().equals(end.toLocalDate()),"Lịch hẹn không được qua ngày.");
        can(exists(c,"SELECT 1 FROM LichLamViec WITH(UPDLOCK,HOLDLOCK) WHERE maNV=? AND ngayTruc=? AND trangThai='DA_PHAN_CONG' AND gioBatDau<=? AND gioKetThuc>=?",
                lh.getNhaSi().getMaNV(),start.toLocalDate(),start.toLocalTime(),end.toLocalTime()),"Nha sĩ không có ca làm việc phù hợp hoặc đang nghỉ phép.");
        can(!trungLich(c,lh.getKhachHang().getMaKH(),lh.getNhaSi().getMaNV(),start,end,boQua),"Trùng lịch khách hàng/nha sĩ hoặc thiếu khoảng cách chuẩn bị.");
        int huy=demHuy(c,lh.getKhachHang().getMaKH(),start.toLocalDate());
        can(huy<quyDinh.nguongHuy() || quyDinh.thuocKhungGioiHan(start.toLocalTime(),end.toLocalTime()),"Khách hủy nhiều lần trong tháng; chỉ đặt khung đầu/cuối ngày.");
        if(lh.getPhieuKhamNguon()!=null) {
            PhieuKham src=new PhieuKhamDAO(context).layTheoMa(c,lh.getPhieuKhamNguon().getMaPK());
            can(src!=null && src.getTrangThai()==TrangThaiPhieuKham.HOAN_TAT && src.getLichHen()!=null,"Phiếu nguồn tái khám chưa hoàn tất.");
            can(src.getHoSoBenhAn().getKhachHang().getMaKH().equals(lh.getKhachHang().getMaKH())
                    && src.getLichHen().getNhaSi().getMaNV().equals(lh.getNhaSi().getMaNV())
                    && src.getLichHen().getDichVu().getMaDV().equals(dv.getMaDV()),"Tái khám phải giữ khách hàng, nha sĩ và dịch vụ nguồn.");
            can(dv.getLoTrinhDichVu()!=null,"Dịch vụ chưa có lộ trình tái khám.");
            long days=java.time.temporal.ChronoUnit.DAYS.between(src.getNgayLap().toLocalDate(),start.toLocalDate());
            can(days>=quyDinh.taiKhamMinNgay() && days<=quyDinh.taiKhamMaxNgay(),"Ngày tái khám ngoài khoảng cho phép.");
        }
    }
    boolean trungLich(Connection c,String maKH,String maNV,LocalDateTime start,LocalDateTime end,String boQua) throws SQLException {
        return exists(c,"SELECT 1 FROM LichHen lh WITH(UPDLOCK,HOLDLOCK) JOIN DichVu dv ON dv.maDV=lh.maDV"
                +" WHERE lh.maLH<>? AND lh.trangThai NOT IN('DA_HUY','KHONG_DEN') AND ((lh.maKH=? AND lh.ngayGioHen<?"
                +" AND DATEADD(MINUTE,dv.thoiLuong,lh.ngayGioHen)>?) OR (lh.maNV=? AND lh.ngayGioHen<?"
                +" AND DATEADD(MINUTE,dv.thoiLuong+?,lh.ngayGioHen)>?))",boQua,maKH,end,start,maNV,end.plusMinutes(quyDinh.khoangCachPhut()),quyDinh.khoangCachPhut(),start);
    }
    private int demHuy(Connection c,String maKH,LocalDate ngay) throws SQLException {
        return one(c,"SELECT COUNT(*) FROM LichHen WHERE maKH=? AND trangThai='DA_HUY' AND ngayGioHen>=? AND ngayGioHen<?",
                r -> r.getInt(1),maKH,ngay.withDayOfMonth(1).atStartOfDay(),ngay.withDayOfMonth(1).plusMonths(1).atStartOfDay());
    }
    public int demSoLanHuyTrongThang(String maKH,LocalDate ngay) {
        return context.doc(c -> { quyen(c,VaiTro.LETAN,VaiTro.QUANLY); return demHuy(c,maKH,ngay); });
    }
    public boolean huy(String ma,String lyDo) {
        can(lyDo!=null && !lyDo.isBlank(),"Vui lòng nhập lý do hủy.");
        return context.transaction(c -> { quyen(c,VaiTro.LETAN,VaiTro.QUANLY); khoa(c,"BClinic:lich");
            can(update(c,"UPDATE LichHen SET trangThai='DA_HUY',lyDoHuy=? WHERE maLH=? AND trangThai='DA_DAT'",lyDo.trim(),ma)==1,"Chỉ được hủy lịch đang đặt.");
            nhatKy(c,"HUY_LICH",ma); return true; });
    }
    /** Gọi khi tải danh sách hoặc từ job; chỉ xử lý lịch chưa đến sau thời gian cho phép trễ. */
    public int capNhatKhongDen() {
        return context.transaction(c -> { quyen(c,VaiTro.LETAN,VaiTro.QUANLY); khoa(c,"BClinic:lich");
            return update(c,"UPDATE LichHen SET trangThai='KHONG_DEN' WHERE trangThai='DA_DAT' AND ngayGioHen<?",context.bayGio().minusMinutes(quyDinh.treToiDaPhut())); });
    }
    /** Gợi ý theo ca trực và thời lượng dịch vụ, loại trùng khách/nha sĩ; bước quét theo khoảng chuẩn bị. */
    public List<LocalDateTime> goiYKhungTrong(String maKH,String maNV,String maDV,LocalDate ngay) {
        can(ngay!=null,"Thiếu ngày cần tìm.");
        return context.doc(c -> {
            quyen(c,VaiTro.LETAN,VaiTro.QUANLY);
            DichVu dv=new DichVuDAO(context).layTheoMa(c,maDV); can(dv!=null && dv.isTrangThai(),"Dịch vụ không hoạt động.");
            List<LocalTime[]> shifts=query(c,"SELECT gioBatDau,gioKetThuc FROM LichLamViec WHERE maNV=? AND ngayTruc=? AND trangThai='DA_PHAN_CONG' ORDER BY gioBatDau",
                    r -> new LocalTime[]{r.getObject(1,LocalTime.class),r.getObject(2,LocalTime.class)},maNV,ngay);
            record Khoang(String kh,String ns,LocalDateTime batDau,LocalDateTime ketThuc) { }
            List<Khoang> busy=query(c,"SELECT lh.maKH,lh.maNV,lh.ngayGioHen,DATEADD(MINUTE,dv.thoiLuong,lh.ngayGioHen) FROM LichHen lh JOIN DichVu dv ON dv.maDV=lh.maDV"
                    +" WHERE (lh.maKH=? OR lh.maNV=?) AND lh.ngayGioHen>=? AND lh.ngayGioHen<? AND lh.trangThai NOT IN('DA_HUY','KHONG_DEN')",
                    r -> new Khoang(r.getString(1),r.getString(2),r.getObject(3,LocalDateTime.class),r.getObject(4,LocalDateTime.class)),maKH,maNV,ngay.atStartOfDay(),ngay.plusDays(1).atStartOfDay());
            boolean limited=demHuy(c,maKH,ngay)>=quyDinh.nguongHuy();
            var result=new java.util.ArrayList<LocalDateTime>();
            for(LocalTime[] shift:shifts) {
                for(LocalDateTime start=ngay.atTime(shift[0]); !start.plusMinutes(dv.getThoiLuong()).isAfter(ngay.atTime(shift[1])); start=start.plusMinutes(quyDinh.khoangCachPhut())) {
                    LocalDateTime end=start.plusMinutes(dv.getThoiLuong());
                    if(!start.isAfter(context.bayGio()) || (limited && !quyDinh.thuocKhungGioiHan(start.toLocalTime(),end.toLocalTime()))) { continue; }
                    boolean conflict=false;
                    for(Khoang b:busy) {
                        if((maKH.equals(b.kh()) && start.isBefore(b.ketThuc()) && end.isAfter(b.batDau()))
                                || (maNV.equals(b.ns()) && start.isBefore(b.ketThuc().plusMinutes(quyDinh.khoangCachPhut()))
                                && end.plusMinutes(quyDinh.khoangCachPhut()).isAfter(b.batDau()))) { conflict=true; break; }
                    }
                    if(!conflict) { result.add(start); }
                }
            }
            return result;
        });
    }
    private LichHen mapRow(ResultSet r) throws SQLException { return DaoViews.lichHen(r); }
}
