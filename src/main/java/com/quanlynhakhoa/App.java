package com.quanlynhakhoa;

import com.formdev.flatlaf.FlatLightLaf;
import com.quanlynhakhoa.config.FlywayMigration;
import com.quanlynhakhoa.ui.frame.FrmDangNhap;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point của ứng dụng B-Clinic.
 * Thứ tự khởi động:
 * 1. Bật FlatLaf (Look & Feel)
 * 2. Chạy Flyway migration (tạo/cập nhật schema DB)
 * 3. Mở FrmDangNhap trên EDT
 */
public class App {

    private static final Logger LOG = Logger.getLogger(App.class.getName());

    public static void main(String[] args) {
        // 1. Look & Feel — phải gọi trước khi tạo bất kỳ component nào
        FlatLightLaf.setup();
        UIManager.put("defaultFont", new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));

        // 2. Flyway migration — chạy trên main thread trước khi mở UI
        try {
            FlywayMigration.migrate();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Migration thất bại", e);
            JOptionPane.showMessageDialog(null,
                    "Không thể kết nối hoặc khởi tạo cơ sở dữ liệu.\n" + e.getMessage(),
                    "Lỗi khởi động", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // 3. Mở màn hình đăng nhập trên Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            FrmDangNhap frm = new FrmDangNhap();
            frm.setVisible(true);
        });
    }
}
