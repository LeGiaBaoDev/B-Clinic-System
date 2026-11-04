package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.dao.support.MaSinh;
import com.quanlynhakhoa.dao.support.NhatKy;
import com.quanlynhakhoa.dao.support.PhanQuyen;
import com.quanlynhakhoa.dao.support.PhanQuyen.Actor;
import com.quanlynhakhoa.entity.LichLamViec;
import com.quanlynhakhoa.entity.enums.CaTruc;
import com.quanlynhakhoa.entity.enums.TrangThaiLichLamViec;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.sql.*;
import java.time.*;
import java.util.List;

public class LichLamViecDAO {

    private static final String SQL_SELECT = """
            SELECT maLLV, ngayTruc, caTruc, gioBatDau, gioKetThuc,
                   trangThai, ghiChuNghi, maNV
            FROM LichLamViec
            """;

    public List<LichLamViec> layTatCa() {
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c);
            return JdbcHelper.query(c, SQL_SELECT + " WHERE (?=1 OR maNV=?)"
                    + " ORDER BY ngayTruc, gioBatDau",
                    r -> mapRow(c, r), actor.vaiTro() == VaiTro.QUANLY, actor.maNV());
        });
    }

    /** UC012: luôn lấy mã nhân viên từ tài khoản còn hoạt động trong DB, gồm cả Quản lý. */
    public List<LichLamViec> layLichCaNhan() {
        return JdbcHelper.doc(c -> {
            Actor actor=PhanQuyen.quyen(c);
            var nhanVien=new NhanVienDAO().layTheoMa(c,actor.maNV());
            return JdbcHelper.query(c,SQL_SELECT+" WHERE maNV=? ORDER BY ngayTruc,gioBatDau",
                    r -> mapRow(r,nhanVien),actor.maNV());
        });
    }

    public List<LichLamViec> layTheoNhanVien(String maNV, LocalDate tu, LocalDate den) {
        khoangNgay(tu, den);
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c);
            yeuCau(actor.vaiTro() == VaiTro.QUANLY || actor.maNV().equals(maNV),
                    "Chỉ được xem lịch làm việc của mình.");
            return JdbcHelper.query(c, SQL_SELECT
                    + " WHERE maNV=? AND ngayTruc BETWEEN ? AND ?"
                    + " ORDER BY ngayTruc, gioBatDau",
                    r -> mapRow(c, r), maNV, tu, den);
        });
    }

    public LichLamViec layTheoMa(String ma) {
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c);
            LichLamViec ll = layTheoMa(c, ma);
            yeuCau(ll == null || actor.vaiTro() == VaiTro.QUANLY
                    || ll.getNhanVien().getMaNV().equals(actor.maNV()),
                    "Chỉ được xem lịch làm việc của mình.");
            return ll;
        });
    }

    private LichLamViec layTheoMa(Connection c, String ma) throws SQLException {
        return JdbcHelper.one(c, SQL_SELECT + " WHERE maLLV=?", r -> mapRow(c, r), ma);
    }

    public boolean them(LichLamViec ll) {
        return luu(ll, true);
    }

    public boolean capNhat(LichLamViec ll) {
        return luu(ll, false);
    }

    /** UC014: caDich khác null là hoán đổi; ngược lại dời caNguon sang ngày/ca mới. */
    public record YeuCauDoiCa(LichLamViec caNguon,LichLamViec caDich,LocalDate ngayMoi,CaTruc caMoi) { }
    public record ThongBaoDoiCa(String maTK,LocalDateTime thoiDiem,String noiDung) { }
    /** Thông báo cá nhân lấy từ thay đổi đã commit; không trả phần lịch của người hoán đổi còn lại. */
    public ThongBaoDoiCa layThongBaoDoiCaGanNhat() {
        return JdbcHelper.doc(c -> {
            Actor actor=PhanQuyen.quyen(c);
            return JdbcHelper.one(c,"SELECT TOP 1 thoiDiem,noiDungThayDoi FROM NhatKyHeThong"
                    +" WHERE hanhDong IN('DOI_CA','HOAN_DOI_CA') AND noiDungThayDoi LIKE ? ORDER BY thoiDiem DESC,maNhatKy DESC",
                    r -> {
                        String segment=java.util.Arrays.stream(r.getString(2).split(";"))
                                .filter(x -> x.contains(" "+actor.maNV()+": ")).findFirst().orElse("");
                        String text=segment.isBlank()?"Hãy làm mới để xem ca mới.":segment.substring(segment.indexOf(": ")+2).trim();
                        return new ThongBaoDoiCa(actor.maTK(),r.getObject(1,LocalDateTime.class),"Lịch làm việc của bạn đã được đổi: "+text);
                    },"% "+actor.maNV()+": %");
        });
    }
    private record KeHoachDoiCa(List<LichLamViec> cu,List<LichLamViec> moi) { }

    public void kiemTraDoiCa(YeuCauDoiCa request) {
        JdbcHelper.transaction(c -> { chuanBiDoiCa(c,request); return null; });
    }
    public boolean doiCa(YeuCauDoiCa request) {
        return JdbcHelper.transaction(c -> {
            KeHoachDoiCa plan=chuanBiDoiCa(c,request);
            for(LichLamViec ca:plan.moi()) {
                yeuCau(JdbcHelper.update(c,"UPDATE LichLamViec SET ngayTruc=?,caTruc=?,gioBatDau=?,gioKetThuc=? WHERE maLLV=?",
                        ca.getNgayTruc(),ca.getCaTruc(),ca.getGioBatDau(),ca.getGioKetThuc(),ca.getMaLLV())==1,"Không thể cập nhật ca làm việc.");
            }
            String audit=java.util.stream.IntStream.range(0,plan.cu().size()).mapToObj(i -> {
                var old=plan.cu().get(i); var next=plan.moi().get(i);
                var format=java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                return old.getMaLLV()+" "+old.getNhanVien().getMaNV()+": "+old.getNgayTruc().format(format)+" "+tenCa(old.getCaTruc())+" → "+next.getNgayTruc().format(format)+" "+tenCa(next.getCaTruc());
            }).collect(java.util.stream.Collectors.joining("; "));
            NhatKy.ghi(c,plan.cu().size()==2?"HOAN_DOI_CA":"DOI_CA",audit);
            return true;
        });
    }
    private KeHoachDoiCa chuanBiDoiCa(Connection c,YeuCauDoiCa request) throws SQLException {
        PhanQuyen.quyen(c,VaiTro.QUANLY); MaSinh.khoa(c,"BClinic:lich");
        yeuCau(request!=null&&request.caNguon()!=null,"Vui lòng chọn ca cần đổi.");
        LichLamViec a=caChuaDoi(c,request.caNguon());
        java.util.ArrayList<LichLamViec> old=new java.util.ArrayList<>(), next=new java.util.ArrayList<>(); old.add(a);
        if(request.caDich()!=null) {
            LichLamViec b=caChuaDoi(c,request.caDich());
            yeuCau(!a.getNhanVien().getMaNV().equals(b.getNhanVien().getMaNV()),"Chọn ca của hai nhân viên khác nhau để hoán đổi.");
            String roleA=vaiTroDangLam(c,a),roleB=vaiTroDangLam(c,b);
            yeuCau(roleA.equals(roleB),"Chỉ được hoán đổi giữa hai nhân viên cùng chức vụ.");
            yeuCau(!a.getNgayTruc().equals(b.getNgayTruc())||a.getCaTruc()!=b.getCaTruc()
                    ||!a.getGioBatDau().equals(b.getGioBatDau())||!a.getGioKetThuc().equals(b.getGioKetThuc()),"Hai ca có cùng ngày và giờ, không cần hoán đổi.");
            old.add(b); next.add(doiKhungGio(a,b)); next.add(doiKhungGio(b,a));
        } else {
            vaiTroDangLam(c,a);
            yeuCau(request.ngayMoi()!=null&&request.caMoi()!=null,"Vui lòng chọn ngày và ca mới.");
            LichLamViec moved=new LichLamViec(a); moved.setNgayTruc(request.ngayMoi()); moved.setCaTruc(request.caMoi()); moved.ganGioTheoCa();
            yeuCau(!a.getNgayTruc().equals(moved.getNgayTruc())||a.getCaTruc()!=moved.getCaTruc(),"Ngày và ca mới phải khác ca hiện tại.");
            next.add(moved);
        }
        String id1=old.getFirst().getMaLLV(),id2=old.getLast().getMaLLV();
        for(LichLamViec ca:next) {
            yeuCau(ca.getNgayTruc().atTime(ca.getGioBatDau()).isAfter(LocalDateTime.now()),"Không chuyển sang ca đã bắt đầu hoặc trong quá khứ.");
            yeuCau(!JdbcHelper.exists(c,"SELECT 1 FROM LichLamViec WITH(UPDLOCK,HOLDLOCK) WHERE maNV=? AND ngayTruc=?"
                    +" AND maLLV NOT IN(?,?) AND (caTruc=? OR (gioBatDau<? AND gioKetThuc>?))",
                    ca.getNhanVien().getMaNV(),ca.getNgayTruc(),id1,id2,ca.getCaTruc(),ca.getGioKetThuc(),ca.getGioBatDau()),
                    "Nhân viên đã có ca trùng hoặc nghỉ phép ở thời gian mới.");
        }
        for(LichLamViec before:old) {
            var appointments=JdbcHelper.query(c,"SELECT lh.ngayGioHen,DATEADD(MINUTE,dv.thoiLuong,lh.ngayGioHen) AS ketThuc"
                    +" FROM LichHen lh WITH(UPDLOCK,HOLDLOCK) JOIN DichVu dv ON dv.maDV=lh.maDV"
                    +" WHERE lh.maNV=? AND lh.ngayGioHen>=? AND lh.ngayGioHen<? AND lh.trangThai IN('DA_DAT','DA_DEN')",
                    r -> new LocalDateTime[]{r.getObject(1,LocalDateTime.class),r.getObject(2,LocalDateTime.class)},
                    before.getNhanVien().getMaNV(),before.getNgayTruc().atStartOfDay(),before.getNgayTruc().plusDays(1).atStartOfDay());
            for(var ap:appointments) {
                boolean covered=next.stream().anyMatch(x -> x.getNhanVien().getMaNV().equals(before.getNhanVien().getMaNV())
                        &&!ap[0].isBefore(x.getNgayTruc().atTime(x.getGioBatDau()))&&!ap[1].isAfter(x.getNgayTruc().atTime(x.getGioKetThuc())));
                yeuCau(covered||JdbcHelper.exists(c,"SELECT 1 FROM LichLamViec WITH(UPDLOCK,HOLDLOCK) WHERE maNV=? AND ngayTruc=?"
                        +" AND maLLV NOT IN(?,?) AND trangThai='DA_PHAN_CONG' AND DATEADD(SECOND,DATEDIFF(SECOND,0,gioBatDau),CAST(ngayTruc AS DATETIME2))<=?"
                        +" AND DATEADD(SECOND,DATEDIFF(SECOND,0,gioKetThuc),CAST(ngayTruc AS DATETIME2))>=?",
                        before.getNhanVien().getMaNV(),before.getNgayTruc(),id1,id2,ap[0],ap[1]),
                        "Không thể đổi ca vì còn lịch hẹn đã đặt/đã đến. Cần xử lý lịch hẹn trước.");
            }
        }
        return new KeHoachDoiCa(List.copyOf(old),List.copyOf(next));
    }
    private LichLamViec caChuaDoi(Connection c,LichLamViec expected) throws SQLException {
        yeuCau(expected.getMaLLV()!=null&&expected.getNhanVien()!=null,"Thông tin ca cần đổi không hợp lệ.");
        LichLamViec actual=JdbcHelper.one(c,SQL_SELECT+" WITH(UPDLOCK,HOLDLOCK) WHERE maLLV=?",r -> mapRow(c,r),expected.getMaLLV());
        yeuCau(actual!=null,"Ca làm việc không còn tồn tại.");
        yeuCau(actual.getNhanVien().getMaNV().equals(expected.getNhanVien().getMaNV())&&actual.getNgayTruc().equals(expected.getNgayTruc())
                &&actual.getCaTruc()==expected.getCaTruc()&&actual.getGioBatDau().equals(expected.getGioBatDau())
                &&actual.getGioKetThuc().equals(expected.getGioKetThuc())&&actual.getTrangThai()==expected.getTrangThai()
                &&java.util.Objects.equals(actual.getGhiChuNghi(),expected.getGhiChuNghi()),"Ca đã thay đổi. Hãy tải lại danh sách trước khi đổi.");
        yeuCau(actual.getTrangThai()==TrangThaiLichLamViec.DA_PHAN_CONG,"Không đổi ca đã nghỉ phép.");
        yeuCau(actual.getNgayTruc().atTime(actual.getGioBatDau()).isAfter(LocalDateTime.now()),"Chỉ được đổi ca chưa bắt đầu.");
        return actual;
    }
    private String vaiTroDangLam(Connection c,LichLamViec ca) throws SQLException {
        String role=JdbcHelper.one(c,"SELECT vaiTro FROM TaiKhoan WITH(UPDLOCK,HOLDLOCK) WHERE maNV=? AND trangThai=1",r -> r.getString(1),ca.getNhanVien().getMaNV());
        yeuCau(role!=null,"Nhân viên không có tài khoản hoạt động."); return role;
    }
    private LichLamViec doiKhungGio(LichLamViec owner,LichLamViec slot) {
        return new LichLamViec(owner.getMaLLV(),slot.getNgayTruc(),slot.getCaTruc(),slot.getGioBatDau(),slot.getGioKetThuc(),
                owner.getTrangThai(),owner.getGhiChuNghi(),owner.getNhanVien());
    }
    private static String tenCa(CaTruc ca) { return switch(ca){case CA_SANG -> "ca sáng";case CA_CHIEU -> "ca chiều";case CA_NGAY -> "cả ngày";}; }

    private boolean luu(LichLamViec ll, boolean themMoi) {
        String maDaLuu = JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            MaSinh.khoa(c, "BClinic:lich");
            new LichLamViec(ll.getMaLLV(), ll.getNgayTruc(), ll.getCaTruc(), ll.getGioBatDau(),
                    ll.getGioKetThuc(), ll.getTrangThai(), ll.getGhiChuNghi(), ll.getNhanVien());

            LocalDate homNay = LocalDate.now();
            yeuCau(!ll.getNgayTruc().isBefore(homNay), "Không thay đổi ca trong quá khứ.");
            LocalTime min = ll.getCaTruc() == CaTruc.CA_CHIEU ? LocalTime.of(13, 30) : LocalTime.of(7, 0);
            LocalTime max = ll.getCaTruc() == CaTruc.CA_SANG ? LocalTime.NOON : LocalTime.of(17, 30);
            yeuCau(!ll.getGioBatDau().isBefore(min) && !ll.getGioKetThuc().isAfter(max),
                    "Giờ làm việc nằm ngoài khung của ca.");
            yeuCau(JdbcHelper.exists(c, "SELECT 1 FROM TaiKhoan WHERE maNV=? AND trangThai=1",
                    ll.getNhanVien().getMaNV()),
                    "Nhân viên không có tài khoản hoạt động.");

            String ma = themMoi ? MaSinh.maMoi(c, MaSinh.Bang.LICH_LAM_VIEC, null) : ll.getMaLLV();
            if (!themMoi) {
                LichLamViec old = layTheoMa(c, ma);
                yeuCau(old != null, "Ca làm việc không tồn tại.");
                yeuCau(!old.getNgayTruc().isBefore(homNay), "Không thay đổi lịch sử ca trực.");
            }
            yeuCau(!JdbcHelper.exists(c, "SELECT 1 FROM LichLamViec WITH(UPDLOCK,HOLDLOCK)"
                    + " WHERE maNV=? AND ngayTruc=? AND maLLV<>? AND trangThai='NGHI_PHEP'"
                    + " AND (caTruc=? OR (gioBatDau<? AND gioKetThuc>?))",
                    ll.getNhanVien().getMaNV(), ll.getNgayTruc(), ma, ll.getCaTruc(),
                    ll.getGioKetThuc(), ll.getGioBatDau()),
                    "Nhân viên đã nghỉ phép trong ca này. Vui lòng chọn ca hoặc nhân viên khác.");
            yeuCau(!JdbcHelper.exists(c, "SELECT 1 FROM LichLamViec WITH(UPDLOCK,HOLDLOCK)"
                    + " WHERE maNV=? AND ngayTruc=? AND caTruc=? AND maLLV<>?",
                    ll.getNhanVien().getMaNV(), ll.getNgayTruc(), ll.getCaTruc(), ma),
                    "Nhân viên đã được phân công ca này trong ngày.");
            yeuCau(!JdbcHelper.exists(c, "SELECT 1 FROM LichLamViec WITH(UPDLOCK,HOLDLOCK) WHERE maNV=? AND ngayTruc=?"
                    + " AND maLLV<>? AND gioBatDau<? AND gioKetThuc>?",
                    ll.getNhanVien().getMaNV(), ll.getNgayTruc(), ma,
                    ll.getGioKetThuc(), ll.getGioBatDau()),
                    "Nhân viên đã có ca trùng giờ hoặc ca nghỉ phép.");

            if (!themMoi) {
                LichLamViec old = layTheoMa(c, ma);
                var lich = JdbcHelper.query(c, "SELECT lh.ngayGioHen,dv.thoiLuong FROM LichHen lh"
                        + " JOIN DichVu dv ON dv.maDV=lh.maDV"
                        + " WHERE lh.maNV=? AND CAST(lh.ngayGioHen AS DATE)=?"
                        + " AND lh.trangThai IN('DA_DAT','DA_DEN')",
                        r -> new Object[]{r.getObject(1, LocalDateTime.class), r.getInt(2)},
                        old.getNhanVien().getMaNV(), old.getNgayTruc());
                for (Object[] row : lich) {
                    LocalDateTime start = (LocalDateTime) row[0];
                    LocalDateTime end = start.plusMinutes((int) row[1]);
                    boolean covered = ll.getNhanVien().getMaNV().equals(old.getNhanVien().getMaNV())
                            && ll.getNgayTruc().equals(start.toLocalDate())
                            && ll.getTrangThai() == TrangThaiLichLamViec.DA_PHAN_CONG
                            && !start.toLocalTime().isBefore(ll.getGioBatDau())
                            && !end.toLocalTime().isAfter(ll.getGioKetThuc());
                    yeuCau(covered || JdbcHelper.exists(c, "SELECT 1 FROM LichLamViec"
                            + " WHERE maNV=? AND ngayTruc=? AND maLLV<>?"
                            + " AND trangThai='DA_PHAN_CONG' AND gioBatDau<=? AND gioKetThuc>=?",
                            old.getNhanVien().getMaNV(), start.toLocalDate(), ma,
                            start.toLocalTime(), end.toLocalTime()),
                            "Cần dời lịch hẹn trước khi đổi ca hoặc duyệt nghỉ.");
                }
            }

            Object[] args = {ll.getNhanVien().getMaNV(), ll.getNgayTruc(), ll.getCaTruc(),
                ll.getGioBatDau(), ll.getGioKetThuc(), ll.getTrangThai(),
                ll.getGhiChuNghi(), ma};
            int rows = JdbcHelper.update(c, themMoi
                    ? "INSERT INTO LichLamViec(maNV,ngayTruc,caTruc,gioBatDau,gioKetThuc,"
                    + "trangThai,ghiChuNghi,maLLV) VALUES(?,?,?,?,?,?,?,?)"
                    : "UPDATE LichLamViec SET maNV=?,ngayTruc=?,caTruc=?,gioBatDau=?,"
                    + "gioKetThuc=?,trangThai=?,ghiChuNghi=? WHERE maLLV=?",
                    args);
            if (rows == 1) {
                NhatKy.ghi(c, "PHAN_CONG_LICH", ma);
            }
            return rows == 1 ? ma : null;
        });
        // Chỉ cấp mã cho đối tượng sau khi cả lịch và nhật ký đã commit thành công.
        if (maDaLuu != null) {
            ll.setMaLLV(maDaLuu);
        }
        return maDaLuu != null;
    }

    public boolean duyetNghiPhep(String ma, String lyDo) {
        LichLamViec ll = layTheoMa(ma);
        yeuCau(ll != null, "Ca làm việc không tồn tại.");
        ll.nghiPhep(lyDo);
        return capNhat(ll);
    }

    private LichLamViec mapRow(Connection c, ResultSet r) throws SQLException {
        return mapRow(r,new NhanVienDAO().layTheoMa(c,r.getString("maNV")));
    }
    private LichLamViec mapRow(ResultSet r,com.quanlynhakhoa.entity.NhanVien nhanVien) throws SQLException {
        return new LichLamViec(
                r.getString("maLLV"),
                r.getObject("ngayTruc", LocalDate.class),
                CaTruc.valueOf(r.getString("caTruc")),
                r.getObject("gioBatDau", LocalTime.class),
                r.getObject("gioKetThuc", LocalTime.class),
                TrangThaiLichLamViec.valueOf(r.getString("trangThai")),
                r.getString("ghiChuNghi"),
                nhanVien);
    }

    private static void khoangNgay(LocalDate tu, LocalDate den) {
        yeuCau(tu != null && den != null && !den.isBefore(tu), "Khoảng ngày không hợp lệ.");
    }

    private static void yeuCau(boolean dieuKien, String message) {
        if (!dieuKien) {
            throw new BusinessException(message);
        }
    }
}
