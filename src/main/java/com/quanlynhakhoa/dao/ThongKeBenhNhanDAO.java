package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.dao.support.PhanQuyen;
import com.quanlynhakhoa.dao.support.PhanQuyen.Actor;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.time.LocalDate;
import java.util.List;

public class ThongKeBenhNhanDAO {

    public record TheoNgay(LocalDate ngay, long soBenhNhan) {

    }

    public List<TheoNgay> thongKe(LocalDate tu, LocalDate den) {
        khoangNgay(tu, den);
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.NHASI, VaiTro.QUANLY);
            return JdbcHelper.query(c, "SELECT CAST(pk.ngayLap AS DATE),COUNT(DISTINCT hs.maKH)"
                    + " FROM PhieuKham pk JOIN HoSoBenhAn hs ON hs.maHSBA=pk.maHSBA"
                    + " JOIN LichHen lh ON lh.maLH=pk.maLH"
                    + " WHERE pk.trangThai='HOAN_TAT' AND pk.ngayLap>=? AND pk.ngayLap<?"
                    + " AND (?=1 OR lh.maNV=?)"
                    + " GROUP BY CAST(pk.ngayLap AS DATE)"
                    + " ORDER BY CAST(pk.ngayLap AS DATE)",
                    r -> new TheoNgay(r.getObject(1, LocalDate.class), r.getLong(2)),
                    tu.atStartOfDay(), den.plusDays(1).atStartOfDay(),
                    actor.vaiTro() == VaiTro.QUANLY, actor.maNV());
        });
    }

    public long demBenhNhanKhacNhau(LocalDate tu, LocalDate den) {
        khoangNgay(tu, den);
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.NHASI, VaiTro.QUANLY);
            return JdbcHelper.one(c, "SELECT COUNT(DISTINCT hs.maKH) FROM PhieuKham pk"
                    + " JOIN HoSoBenhAn hs ON hs.maHSBA=pk.maHSBA"
                    + " JOIN LichHen lh ON lh.maLH=pk.maLH"
                    + " WHERE pk.trangThai='HOAN_TAT' AND pk.ngayLap>=? AND pk.ngayLap<?"
                    + " AND (?=1 OR lh.maNV=?)",
                    r -> r.getLong(1), tu.atStartOfDay(), den.plusDays(1).atStartOfDay(),
                    actor.vaiTro() == VaiTro.QUANLY, actor.maNV());
        });
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
