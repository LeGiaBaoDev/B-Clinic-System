package com.quanlynhakhoa.dao;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.VaiTro;

/** Loại bỏ nội dung bệnh án khỏi đồ thị trả cho lễ tân, kể cả phiếu tái khám lồng nhau. */
final class ClinicalPrivacy {
    private ClinicalPrivacy() { }
    static PhieuKham loc(PhieuKham pk,VaiTro role) {
        if(pk!=null && role==VaiTro.LETAN) {
            pk.setLoiDan(null);
            if(pk.getHoSoBenhAn()!=null) { pk.getHoSoBenhAn().setTienSuBenh(null); }
            loc(pk.getLichHen(),role);
        }
        return pk;
    }
    static LichHen loc(LichHen lh,VaiTro role) {
        if(lh!=null && role==VaiTro.LETAN) { loc(lh.getPhieuKhamNguon(),role); }
        return lh;
    }
}
