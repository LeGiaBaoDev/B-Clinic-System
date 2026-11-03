package com.quanlynhakhoa.dao;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.VaiTro;
import java.time.LocalDate;
import java.util.List;

/** UC033: đếm khách khác nhau đã có phiếu hoàn tất, chỉ trong phạm vi nha sĩ được xem. */
public class ThongKeBenhNhanDAO extends DaoSupport {
    public ThongKeBenhNhanDAO() { }
    public ThongKeBenhNhanDAO(DaoContext context) { super(context); }
    public record TheoNgay(LocalDate ngay,long soBenhNhan) { }
    public List<TheoNgay> thongKe(LocalDate tu,LocalDate den) {
        khoangNgay(tu,den);
        return context.doc(c -> { TaiKhoan actor=quyen(c,VaiTro.NHASI,VaiTro.QUANLY); return query(c,
                "SELECT CAST(pk.ngayLap AS DATE),COUNT(DISTINCT hs.maKH) FROM PhieuKham pk"
                +" JOIN HoSoBenhAn hs ON hs.maHSBA=pk.maHSBA JOIN LichHen lh ON lh.maLH=pk.maLH"
                +" WHERE pk.trangThai='HOAN_TAT' AND pk.ngayLap>=? AND pk.ngayLap<? AND (?=1 OR lh.maNV=?)"
                +" GROUP BY CAST(pk.ngayLap AS DATE) ORDER BY CAST(pk.ngayLap AS DATE)",
                r -> new TheoNgay(r.getObject(1,LocalDate.class),r.getLong(2)),tu.atStartOfDay(),den.plusDays(1).atStartOfDay(),actor.getVaiTro()==VaiTro.QUANLY,actor.getNhanVien().getMaNV()); });
    }
    public long demBenhNhanKhacNhau(LocalDate tu,LocalDate den) {
        khoangNgay(tu,den);
        return context.doc(c -> { TaiKhoan actor=quyen(c,VaiTro.NHASI,VaiTro.QUANLY);
            return one(c,"SELECT COUNT(DISTINCT hs.maKH) FROM PhieuKham pk JOIN HoSoBenhAn hs ON hs.maHSBA=pk.maHSBA JOIN LichHen lh ON lh.maLH=pk.maLH"
                    +" WHERE pk.trangThai='HOAN_TAT' AND pk.ngayLap>=? AND pk.ngayLap<? AND (?=1 OR lh.maNV=?)",r -> r.getLong(1),
                    tu.atStartOfDay(),den.plusDays(1).atStartOfDay(),actor.getVaiTro()==VaiTro.QUANLY,actor.getNhanVien().getMaNV()); });
    }
}
