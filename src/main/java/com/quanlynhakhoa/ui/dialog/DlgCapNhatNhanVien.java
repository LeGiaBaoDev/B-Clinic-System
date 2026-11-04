package com.quanlynhakhoa.ui.dialog;

import com.quanlynhakhoa.dao.TaiKhoanDAO;
import com.quanlynhakhoa.entity.TaiKhoan;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** UC003 theo kế hoạch H1; dùng chung form và kiểm tra nhập liệu với UC002. */
public class DlgCapNhatNhanVien extends JDialog {
    public DlgCapNhatNhanVien(Window owner, TaiKhoan taiKhoan, Runnable daLuu) {
        super(owner, "Cập nhật nhân viên và tài khoản", ModalityType.APPLICATION_MODAL);
        var form = new DlgThemNhanVien.NoiDung(new TaiKhoanDAO()::capNhatNhanVienVaTaiKhoan,
                nv -> { dispose(); daLuu.run(); }, this::dispose, taiKhoan,
                () -> JOptionPane.showConfirmDialog(this,
                        "Khóa tài khoản " + taiKhoan.getTenTK() + "? Nhân viên sẽ không thể đăng nhập.",
                        "Xác nhận khóa tài khoản", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
                        == JOptionPane.YES_OPTION);
        setContentPane(form);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { form.yeuCauDong(); }
        });
        getRootPane().setDefaultButton(form.nutLuu());
        getRootPane().registerKeyboardAction(e -> form.yeuCauDong(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(680, bounds.width - 32), Math.min(690, bounds.height - 40));
        setMinimumSize(new Dimension(560, 480));
        setLocationRelativeTo(owner);
    }

}
