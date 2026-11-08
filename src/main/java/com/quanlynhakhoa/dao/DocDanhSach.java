package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;

/** Bộ nạp chỉ dùng trong một lần đọc, chỉ lấy các mã được danh sách có quyền tham chiếu. */
final class DocDanhSach {
    record Lich(String ma, LocalDateTime ngay, String tt, String ghiChu, String lyDo,
                String kh, String nv, String dv, String nguon) { }
    record Phieu(String ma, LocalDateTime ngay, String tt, String loiDan, String hs, String lh, String nv) { }
    private final Connection c;
    private final Map<String, KhachHang> kh = new HashMap<>();
    private final Map<String, NhanVien> nv = new HashMap<>();
    private final Map<String, DichVu> dv = new HashMap<>();
    private final Map<String, HoSoBenhAn> hs = new HashMap<>();
    private final Map<String, Phieu> nguon = new HashMap<>();

    private DocDanhSach(Connection c) { this.c = c; }
    static Lich docLich(ResultSet r) throws SQLException {
        return new Lich(r.getString("maLH"),r.getObject("ngayGioHen",LocalDateTime.class),
                r.getString("trangThai"),r.getString("ghiChu"),r.getString("lyDoHuy"),
                r.getString("maKH"),r.getString("maNV"),r.getString("maDV"),r.getString("maPKNguon"));
    }
    static Phieu docPhieu(ResultSet r) throws SQLException {
        return new Phieu(r.getString("maPK"),r.getObject("ngayLap",LocalDateTime.class),
                r.getString("trangThai"),r.getString("loiDan"),r.getString("maHSBA"),
                r.getString("maLH"),r.getString("maNV_LapPhieu"));
    }
    static List<LichHen> lich(Connection c, List<Lich> rows) throws SQLException {
        DocDanhSach d = new DocDanhSach(c);
        d.nap(rows,List.of());
        return rows.stream().map(d::taoLich).toList();
    }
    static List<PhieuKham> phieu(Connection c, List<Phieu> rows) throws SQLException {
        DocDanhSach d = new DocDanhSach(c);
        List<Lich> lich = d.theoMa("SELECT * FROM LichHen WHERE maLH IN (",ma(rows,Phieu::lh),DocDanhSach::docLich);
        d.nap(lich,rows);
        Map<String,LichHen> byId = new HashMap<>();
        for(Lich l:lich) byId.put(l.ma(),d.taoLich(l));
        return rows.stream().map(p->d.taoPhieu(p,byId.get(p.lh()))).toList();
    }
    private void nap(List<Lich> lich,List<Phieu> phieu) throws SQLException {
        List<Phieu> sources = theoMa("SELECT * FROM PhieuKham WHERE maPK IN (",ma(lich,Lich::nguon),DocDanhSach::docPhieu);
        for(Phieu p:sources) nguon.put(p.ma(),p);
        List<Phieu> all = new ArrayList<>(phieu); all.addAll(sources);
        Set<String> khIds = ma(lich,Lich::kh);
        record HoSo(String ma,String tienSu,String kh) { }
        List<HoSo> hoSo = theoMa("SELECT maHSBA,tienSuBenh,maKH FROM HoSoBenhAn WHERE maHSBA IN (",ma(all,Phieu::hs),
                r->new HoSo(r.getString(1),r.getString(2),r.getString(3)));
        khIds.addAll(ma(hoSo,HoSo::kh));
        for(KhachHang k:theoMa("SELECT * FROM KhachHang WHERE maKH IN (",khIds,
                r->KhachHang.tuDuLieu(r.getString("maKH"),r.getString("tenKH"),r.getObject("ngaySinh",LocalDate.class),
                        r.getBoolean("gioiTinh"),r.getString("sdt"),r.getString("diaChi"),r.getString("email")))) kh.put(k.getMaKH(),k);
        for(HoSo h:hoSo) hs.put(h.ma(),new HoSoBenhAn(h.ma(),h.tienSu(),kh.get(h.kh())));
        Set<String> nvIds=ma(lich,Lich::nv); nvIds.addAll(ma(all,Phieu::nv));
        for(NhanVien n:theoMa("SELECT * FROM NhanVien WHERE maNV IN (",nvIds,
                r->NhanVien.tuDuLieu(r.getString("maNV"),r.getString("hoTenNV"),r.getBoolean("gioiTinh"),
                        r.getString("sdt"),r.getString("email"),r.getString("chuyenMon")))) nv.put(n.getMaNV(),n);
        for(DichVu v:theoMa("SELECT d.*,l.tenLoaiDV,l.mucThueGTGT FROM DichVu d LEFT JOIN LoaiDichVu l ON l.maLoaiDV=d.maLoaiDV WHERE d.maDV IN (",ma(lich,Lich::dv),r->{
            LoaiDichVu loai=r.getString("maLoaiDV")==null?null:new LoaiDichVu(r.getString("maLoaiDV"),r.getString("tenLoaiDV"),switch(r.getInt("mucThueGTGT")){
                case 0->MucThue.THUE_0;case 5->MucThue.THUE_5;case 10->MucThue.THUE_10;default->throw new SQLException("Thuế không hợp lệ");});
            return new DichVu(r.getString("maDV"),r.getString("tenDV"),r.getDouble("donGia"),r.getInt("thoiLuong"),r.getString("loTrinhDichVu"),r.getBoolean("trangThai"),loai);
        })) dv.put(v.getMaDV(),v);
    }
    private LichHen taoLich(Lich l) {
        Phieu p=nguon.get(l.nguon());
        return new LichHen(l.ma(),l.ngay(),TrangThaiLichHen.valueOf(l.tt()),l.ghiChu(),l.lyDo(),
                kh.get(l.kh()),nv.get(l.nv()),dv.get(l.dv()),p==null?null:taoPhieu(p,null));
    }
    private PhieuKham taoPhieu(Phieu p,LichHen lich) {
        return new PhieuKham(p.ma(),p.ngay(),TrangThaiPhieuKham.valueOf(p.tt()),p.loiDan(),hs.get(p.hs()),lich,nv.get(p.nv()));
    }
    private static <T> Set<String> ma(List<T> rows,Function<T,String> key) {
        Set<String> ids=new LinkedHashSet<>();for(T row:rows){String id=key.apply(row);if(id!=null)ids.add(id);}return ids;
    }
    private <T> List<T> theoMa(String sql,Set<String> ids,JdbcHelper.Mapper<T> mapper) throws SQLException {
        List<T> result=new ArrayList<>(); List<String> list=new ArrayList<>(ids);
        for(int i=0;i<list.size();i+=500){
            List<String> part=list.subList(i,Math.min(i+500,list.size()));
            result.addAll(JdbcHelper.query(c,sql+String.join(",",Collections.nCopies(part.size(),"?"))+")",mapper,part.toArray()));
        }
        return result;
    }
}
