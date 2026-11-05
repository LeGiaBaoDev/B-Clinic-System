package com.quanlynhakhoa.ui.dialog;

import static com.quanlynhakhoa.ui.component.UiConstants.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Hộp thoại xác nhận dùng chung, thay cho JOptionPane mặc định. */
public final class DlgXacNhan {

    private DlgXacNhan() {
    }

    public static boolean hoi(Component cha, String tieuDe, String noiDung, String nhanDongY) {
        boolean[] dongY = {false};
        Window owner = cha == null ? null : SwingUtilities.getWindowAncestor(cha);
        JDialog dialog = new JDialog(owner, tieuDe, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(SURFACE);
        root.setBorder(new EmptyBorder(24, 24, 20, 24));

        JLabel title = new JLabel(tieuDe);
        title.setFont(bold(18));
        title.setForeground(TEXT);
        JLabel noiDungLbl = new JLabel("<html><div style='width:320px'>" + noiDung + "</div></html>");
        noiDungLbl.setFont(font(13));
        noiDungLbl.setForeground(MUTED);
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.add(title);
        head.add(Box.createVerticalStrut(8));
        head.add(noiDungLbl);
        root.add(head, BorderLayout.CENTER);

        JButton btnHuy = new JButton("Hủy");
        JButton btnDongY = new JButton(nhanDongY);
        btnDongY.setBackground(new Color(0xC94C48));
        btnDongY.setForeground(Color.WHITE);
        for (JButton b : new JButton[]{btnHuy, btnDongY}) {
            b.setFont(bold(12));
        }
        btnHuy.addActionListener(e -> dialog.dispose());
        btnDongY.addActionListener(e -> {
            dongY[0] = true;
            dialog.dispose();
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(btnHuy);
        buttons.add(btnDongY);
        root.add(buttons, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.getRootPane().setDefaultButton(btnHuy);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        dialog.setLocationRelativeTo(cha);
        dialog.setVisible(true);
        return dongY[0];
    }
}
