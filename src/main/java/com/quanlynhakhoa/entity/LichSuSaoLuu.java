package com.quanlynhakhoa.entity;

import java.time.LocalDateTime;
import java.util.*;

/** Lịch sử ngoài DB, còn nguyên khi khôi phục. */
public final class LichSuSaoLuu {
    private final LocalDateTime thoiGian;
    private final String loai,duongDan,ketQua,nguoiThucHien;
    private final long kichThuoc;
    public LichSuSaoLuu(LocalDateTime thoiGian,String loai,String duongDan,long kichThuoc,String ketQua,String nguoiThucHien){
        this.thoiGian=Objects.requireNonNull(thoiGian);this.loai=Objects.requireNonNull(loai);this.duongDan=Objects.requireNonNull(duongDan);this.kichThuoc=kichThuoc;this.ketQua=Objects.requireNonNull(ketQua);this.nguoiThucHien=Objects.requireNonNull(nguoiThucHien);
    }
    public LocalDateTime getThoiGian(){return thoiGian;} public String getLoai(){return loai;} public String getDuongDan(){return duongDan;}
    public long getKichThuoc(){return kichThuoc;} public String getKetQua(){return ketQua;} public String getNguoiThucHien(){return nguoiThucHien;}
}
