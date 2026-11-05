package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.dao.support.PhanQuyen;
import com.quanlynhakhoa.dao.support.PhanQuyen.Actor;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.time.LocalDate;
import java.util.List;

public class ThongKeLichHenDAO {

    public record TheoNgay(LocalDate ngay, long tong, long daDat, long daDen,
            long hoanTat, long daHuy, long khongDen) {

        public double tyLeHuy() {
            return tong == 0 ? 0 : 100.0 * daHuy / tong;
        }

        public double tyLeKhongDen() {
            return tong == 0 ? 0 : 100.0 * khongDen / tong;
        }
    }

    /**
     * Tổng hợp lịch hẹn trong một khoảng ngày. Mọi tỷ lệ tính theo phần trăm trên tổng số lịch;
     * tổng bằng 0 thì mọi tỷ lệ bằng 0.
     */
    public record TongHop(long tong, long daDat, long daDen, long hoanTat, long daHuy, long khongDen) {

        public boolean coDuLieu() {
            return tong > 0;
        }

        public double tyLeDaDat() {
            return tyLe(daDat);
        }

        public double tyLeDaDen() {
            return tyLe(daDen);
        }

        public double tyLeHoanTat() {
            return tyLe(hoanTat);
        }

        public double tyLeDaHuy() {
            return tyLe(daHuy);
        }

        public double tyLeKhongDen() {
            return tyLe(khongDen);
        }

        /** Tỷ lệ chuyển đổi = số lịch hoàn tất / tổng số lịch (UC029). */
        public double tyLeChuyenDoi() {
            return tyLeHoanTat();
        }

        private double tyLe(long soLuong) {
            return tong == 0 ? 0 : 100.0 * soLuong / tong;
        }

        static TongHop cong(List<TheoNgay> ds) {
            long tong = 0;
            long daDat = 0;
            long daDen = 0;
            long hoanTat = 0;
            long daHuy = 0;
            long khongDen = 0;
            for (TheoNgay n : ds) {
                tong += n.tong();
                daDat += n.daDat();
                daDen += n.daDen();
                hoanTat += n.hoanTat();
                daHuy += n.daHuy();
                khongDen += n.khongDen();
            }
            return new TongHop(tong, daDat, daDen, hoanTat, daHuy, khongDen);
        }
    }

    /** UC029/UC034: tổng hợp theo khoảng ngày, có thể lọc theo nha sĩ ({@code null} = tất cả). */
    public TongHop tongHop(LocalDate tu, LocalDate den, String maNhaSi) {
        return TongHop.cong(thongKe(tu, den, maNhaSi));
    }

    /** UC032: tổng hợp của một ngày (mặc định lễ tân xem hôm nay). */
    public TongHop theoNgay(LocalDate ngay) {
        yeuCau(ngay != null, "Khoảng ngày không hợp lệ.");
        return tongHop(ngay, ngay, null);
    }

    public List<TheoNgay> thongKe(LocalDate tu, LocalDate den, String maNhaSi) {
        khoangNgay(tu, den);
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c);
            yeuCau(actor.vaiTro() != VaiTro.LETAN || tu.equals(den),
                    "Lễ tân chỉ được xem thống kê theo từng ngày.");
            String ns = maNhaSi;
            if (actor.vaiTro() == VaiTro.NHASI) {
                yeuCau(ns == null || ns.equals(actor.maNV()),
                        "Chỉ được thống kê lịch của mình.");
                ns = actor.maNV();
            }
            return JdbcHelper.query(c, "SELECT CAST(ngayGioHen AS DATE),COUNT_BIG(*),"
                    + "SUM(CASE WHEN trangThai='DA_DAT' THEN CAST(1 AS BIGINT) ELSE 0 END),"
                    + "SUM(CASE WHEN trangThai='DA_DEN' THEN CAST(1 AS BIGINT) ELSE 0 END),"
                    + "SUM(CASE WHEN trangThai='HOAN_TAT' THEN CAST(1 AS BIGINT) ELSE 0 END),"
                    + "SUM(CASE WHEN trangThai='DA_HUY' THEN CAST(1 AS BIGINT) ELSE 0 END),"
                    + "SUM(CASE WHEN trangThai='KHONG_DEN' THEN CAST(1 AS BIGINT) ELSE 0 END)"
                    + " FROM LichHen WHERE ngayGioHen>=? AND ngayGioHen<?"
                    + " AND (? IS NULL OR maNV=?)"
                    + " GROUP BY CAST(ngayGioHen AS DATE)"
                    + " ORDER BY CAST(ngayGioHen AS DATE)",
                    r -> new TheoNgay(r.getObject(1, LocalDate.class), r.getLong(2),
                            r.getLong(3), r.getLong(4), r.getLong(5), r.getLong(6), r.getLong(7)),
                    tu.atStartOfDay(), den.plusDays(1).atStartOfDay(), ns, ns);
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
