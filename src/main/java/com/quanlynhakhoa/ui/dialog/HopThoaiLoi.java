package com.quanlynhakhoa.ui.dialog;

import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;
import java.awt.Component;
import java.util.concurrent.ExecutionException;
import javax.swing.JOptionPane;

/**
 * Hiển thị lỗi cho người dùng bằng tiếng Việt: lỗi nghiệp vụ là cảnh báo kèm nội dung; lỗi dữ liệu và
 * lỗi bất ngờ chỉ hiện câu chung, không lộ chi tiết hệ thống.
 */
public final class HopThoaiLoi {

    private HopThoaiLoi() {
    }

    /** Lấy nguyên nhân thật từ ngoại lệ bọc của {@code SwingWorker.get()}. */
    public static Throwable nguyenNhan(Throwable loi) {
        return loi instanceof ExecutionException && loi.getCause() != null ? loi.getCause() : loi;
    }

    public static void hien(Component cha, Throwable loi, String cauLoiDuLieu) {
        Throwable goc = nguyenNhan(loi);
        if (goc instanceof BusinessException) {
            JOptionPane.showMessageDialog(cha, goc.getMessage(), "Cảnh báo", JOptionPane.WARNING_MESSAGE);
        } else if (goc instanceof DataAccessException) {
            JOptionPane.showMessageDialog(cha, cauLoiDuLieu, "Lỗi", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(cha, "Đã xảy ra lỗi, vui lòng thử lại.", "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
