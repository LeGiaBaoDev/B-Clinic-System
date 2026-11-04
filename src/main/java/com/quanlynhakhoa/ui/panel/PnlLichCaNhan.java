package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.dao.LichLamViecDAO;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.util.SessionManager;
import java.util.List;

/** UC012: lịch cá nhân cho cả ba vai trò; không nhận mã nhân viên từ giao diện. */
public class PnlLichCaNhan extends PnlLichLamViec {
    public PnlLichCaNhan() {
        super(PnlLichCaNhan::taiKhoanHienTai,new LichLamViecDAO()::layLichCaNhan,
                ca -> { throw new IllegalStateException("Lịch cá nhân chỉ được xem."); },true);
    }
    private static List<TaiKhoan> taiKhoanHienTai() {
        TaiKhoan tk=SessionManager.getInstance().getCurrentUser();
        return tk==null?List.of():List.of(tk);
    }
}
