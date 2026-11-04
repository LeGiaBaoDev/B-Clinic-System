package com.quanlynhakhoa.ui.dialog;

import com.quanlynhakhoa.dao.TaiKhoanDAO;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.util.KiemTraXacThuc;
import com.quanlynhakhoa.util.QuyTacTaiKhoan;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Chỉ đổi thành công mới trả tài khoản cho luồng đăng nhập cấp session. */
public class DlgDoiMatKhauLanDau extends JDialog {
    private TaiKhoan ketQua;

    public DlgDoiMatKhauLanDau(Window owner, TaiKhoan taiKhoan) {
        super(owner, "Đổi mật khẩu lần đầu", ModalityType.APPLICATION_MODAL);
        setName("doiMatKhauLanDau");
        NoiDung form = new NoiDung(moi -> new TaiKhoanDAO().doiMatKhauLanDau(taiKhoan.getMaTK(),
                QuyTacTaiKhoan.MAT_KHAU_MAC_DINH, moi), tk -> { ketQua = tk; dispose(); }, this::dispose);
        setContentPane(form);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { form.yeuCauDong(); }
        });
        getRootPane().setDefaultButton(form.btnLuu);
        getRootPane().registerKeyboardAction(e -> form.yeuCauDong(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        setMinimumSize(new Dimension(500, 370));
        pack();
        setLocationRelativeTo(owner);
    }

    public TaiKhoan getKetQua() { return ketQua; }

    static final class NoiDung extends JPanel {
        private final JPasswordField txtMoi = new JPasswordField();
        private final JPasswordField txtXacNhan = new JPasswordField();
        private final JLabel lblLoi = new JLabel(" ");
        private final JButton btnLuu = new JButton("Đổi mật khẩu");
        private final JButton btnHuy = new JButton("Quay lại đăng nhập");
        private final Function<String, TaiKhoan> luu;
        private final Consumer<TaiKhoan> thanhCong;
        private final Runnable dong;
        private boolean dangLuu;

        NoiDung(Function<String, TaiKhoan> luu, Consumer<TaiKhoan> thanhCong, Runnable dong) {
            super(new BorderLayout(0, 16));
            this.luu = luu; this.thanhCong = thanhCong; this.dong = dong;
            setBackground(SURFACE);
            setBorder(new EmptyBorder(24, 24, 24, 24));
            JLabel title = new JLabel("Đặt mật khẩu của bạn");
            title.setFont(bold(22));title.setForeground(TEXT);
            add(title, BorderLayout.NORTH);
            JPanel fields = new JPanel(new GridBagLayout());fields.setOpaque(false);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;c.gridy = 0;c.weightx = 1;c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 0, 10, 0);
            JLabel note = new JLabel("<html>Bạn đang sử dụng mật khẩu mặc định.<br>Vui lòng đổi mật khẩu trước khi vào hệ thống.</html>");
            note.setFont(font(13));fields.add(note, c);
            themTruong(fields, c, "Mật khẩu mới (ít nhất 6 ký tự)", txtMoi, "matKhauMoi");
            themTruong(fields, c, "Xác nhận mật khẩu mới", txtXacNhan, "xacNhanMatKhau");
            JCheckBox hien = new JCheckBox("Hiện mật khẩu");hien.setOpaque(false);hien.setFont(font(12));
            char echo = txtMoi.getEchoChar();
            hien.addActionListener(e -> { txtMoi.setEchoChar(hien.isSelected() ? (char) 0 : echo);
                txtXacNhan.setEchoChar(hien.isSelected() ? (char) 0 : echo); });
            c.gridy++;fields.add(hien, c);
            add(fields, BorderLayout.CENTER);
            JPanel footer = new JPanel(new BorderLayout(0, 12));footer.setOpaque(false);
            lblLoi.setName("loiDoiMatKhau");lblLoi.setFont(font(12));lblLoi.setForeground(new Color(0xC94C48));
            lblLoi.setPreferredSize(new Dimension(440, 40));footer.add(lblLoi, BorderLayout.NORTH);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));buttons.setOpaque(false);
            btnLuu.setName("luuMatKhau");btnHuy.setName("huyDoiMatKhau");
            btnLuu.setBackground(TEAL);btnLuu.setForeground(Color.WHITE);
            for (JButton b : new JButton[]{btnHuy, btnLuu}) { b.setFont(bold(12));buttons.add(b); }
            footer.add(buttons, BorderLayout.SOUTH);add(footer, BorderLayout.SOUTH);
            btnHuy.addActionListener(e -> yeuCauDong());btnLuu.addActionListener(e -> luu());
            txtMoi.addActionListener(e -> luu());txtXacNhan.addActionListener(e -> luu());
        }

        private void themTruong(JPanel fields, GridBagConstraints c, String ten, JPasswordField input, String name) {
            c.gridy++;JLabel label = new JLabel(ten);label.setFont(font(12));label.setLabelFor(input);fields.add(label, c);
            input.setName(name);input.setFont(font(13));input.setPreferredSize(new Dimension(440, 36));
            c.gridy++;fields.add(input, c);
        }

        private void luu() {
            if (dangLuu) { return; }
            char[] first = txtMoi.getPassword(), second = txtXacNhan.getPassword();
            String moi = new String(first), nhapLai = new String(second);
            Arrays.fill(first, '\0');Arrays.fill(second, '\0');
            String loi = KiemTraXacThuc.loiDoiMatKhauLanDau(moi, nhapLai);
            if (loi != null) { lblLoi.setText("<html>" + loi + "</html>");return; }
            datDangLuu(true);lblLoi.setText("Đang đổi mật khẩu...");
            new SwingWorker<TaiKhoan, Void>() {
                protected TaiKhoan doInBackground() { return luu.apply(moi); }
                protected void done() {
                    TaiKhoan tk = null;
                    try { tk = get(); }
                    catch (ExecutionException ex) {
                        lblLoi.setText("<html>" + (ex.getCause() instanceof BusinessException ? ex.getCause().getMessage()
                                : "Không thể đổi mật khẩu. Vui lòng thử lại.") + "</html>");
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();lblLoi.setText("Đã dừng đổi mật khẩu. Vui lòng thử lại.");
                    } finally { datDangLuu(false); }
                    if (tk != null) { xoaMatKhau();thanhCong.accept(tk); }
                }
            }.execute();
        }

        private void datDangLuu(boolean value) {
            dangLuu = value;txtMoi.setEnabled(!value);txtXacNhan.setEnabled(!value);
            btnLuu.setEnabled(!value);btnHuy.setEnabled(!value);btnLuu.setText(value ? "Đang lưu..." : "Đổi mật khẩu");
        }
        private void xoaMatKhau() { txtMoi.setText("");txtXacNhan.setText(""); }
        void yeuCauDong() { if (!dangLuu) { xoaMatKhau();dong.run(); } }
    }
}
