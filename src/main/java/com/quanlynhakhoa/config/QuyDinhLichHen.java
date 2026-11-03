package com.quanlynhakhoa.config;

import java.time.LocalTime;

/** Giá trị mặc định phải tương thích trigger DB đang yêu cầu ít nhất 15 phút. */
public record QuyDinhLichHen(int khoangCachPhut,int nguongHuy,int treToiDaPhut,
        LocalTime dauNgay,LocalTime hetKhungDau,LocalTime dauKhungCuoi,LocalTime cuoiNgay,
        int taiKhamMinNgay,int taiKhamMaxNgay) {
    public QuyDinhLichHen {
        if(khoangCachPhut<15 || nguongHuy<1 || treToiDaPhut<0 || taiKhamMinNgay<1
                || taiKhamMaxNgay<taiKhamMinNgay || dauNgay==null || hetKhungDau==null
                || dauKhungCuoi==null || cuoiNgay==null || !dauNgay.isBefore(hetKhungDau)
                || !hetKhungDau.isBefore(dauKhungCuoi) || !dauKhungCuoi.isBefore(cuoiNgay)) {
            throw new IllegalArgumentException("Quy định lịch hẹn không hợp lệ.");
        }
    }
    public static QuyDinhLichHen macDinh() {
        return new QuyDinhLichHen(15,3,15,LocalTime.of(7,0),LocalTime.of(9,30),
                LocalTime.of(16,0),LocalTime.of(17,30),7,14);
    }
    public boolean thuocKhungGioiHan(LocalTime batDau,LocalTime ketThuc) {
        return (!batDau.isBefore(dauNgay) && !ketThuc.isAfter(hetKhungDau))
                || (!batDau.isBefore(dauKhungCuoi) && !ketThuc.isAfter(cuoiNgay));
    }
}
