package com.quanlynhakhoa.ui.dialog;

import com.quanlynhakhoa.dao.TaiKhoanDAO;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;
import com.quanlynhakhoa.util.KiemTraXacThuc;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Bước 3 quên mật khẩu: đặt mật khẩu mới sau khi OTP đã được xác thực. */
public class DlgDatMatKhauMoi extends JDialog {

    public DlgDatMatKhauMoi(Window owner, String maTK, String otp) {
        super(owner, "Đặt mật khẩu mới", ModalityType.APPLICATION_MODAL);
        setName("datMatKhauMoi");
        NoiDung form = new NoiDung(maTK, otp);
        setContentPane(form);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(form.btnLuu);
        setMinimumSize(new Dimension(500, 370));
        pack();
        setLocationRelativeTo(owner);
    }

    static final class NoiDung extends JPanel {
        private final String maTK;
        private final String otp;
        private final JPasswordField txtMoi = new JPasswordField();
        private final JPasswordField txtXacNhan = new JPasswordField();
        private final JLabel lblThongBao = new JLabel(" ");
        private final JButton btnLuu = new JButton("Xác nhận");
        private final JButton btnHuy = new JButton("Hủy");
        private boolean dangLuu;

        NoiDung(String maTK, String otp) {
            super(new BorderLayout(0, 16));
            this.maTK = maTK;
            this.otp = otp;
            setBackground(SURFACE);
            setBorder(new EmptyBorder(24, 24, 24, 24));

            JPanel header = new JPanel();
            header.setOpaque(false);
            header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
            JLabel title = new JLabel("Đặt mật khẩu mới");
            title.setFont(bold(22));
            title.setForeground(TEXT);
            JLabel sub = new JLabel("<html>Mã xác thực đã được xác nhận. "
                    + "Nhập mật khẩu mới (ít nhất 6 ký tự).</html>");
            sub.setFont(font(12));
            sub.setForeground(MUTED);
            header.add(title);
            header.add(Box.createVerticalStrut(6));
            header.add(sub);
            add(header, BorderLayout.NORTH);

            JPanel fields = new JPanel(new GridBagLayout());
            fields.setOpaque(false);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 0, 8, 0);
            themTruong(fields, c, "Mật khẩu mới", txtMoi, "matKhauMoi");
            themTruong(fields, c, "Xác nhận mật khẩu mới", txtXacNhan, "xacNhanMatKhau");
            JCheckBox hien = new JCheckBox("Hiện mật khẩu");
            hien.setOpaque(false);
            hien.setFont(font(12));
            char echo = txtMoi.getEchoChar();
            hien.addActionListener(e -> {
                txtMoi.setEchoChar(hien.isSelected() ? (char) 0 : echo);
                txtXacNhan.setEchoChar(hien.isSelected() ? (char) 0 : echo);
            });
            c.gridy++;
            fields.add(hien, c);
            add(fields, BorderLayout.CENTER);

            JPanel footer = new JPanel(new BorderLayout(0, 12));
            footer.setOpaque(false);
            lblThongBao.setName("thongBaoDatMatKhau");
            lblThongBao.setFont(font(12));
            lblThongBao.setForeground(new Color(0xC94C48));
            lblThongBao.setPreferredSize(new Dimension(440, 34));
            footer.add(lblThongBao, BorderLayout.NORTH);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            buttons.setOpaque(false);
            btnLuu.setName("luuMatKhauMoi");
            btnHuy.setName("huyDatMatKhauMoi");
            btnLuu.setBackground(TEAL);
            btnLuu.setForeground(Color.WHITE);
            for (JButton b : new JButton[]{btnHuy, btnLuu}) {
                b.setFont(bold(12));
                buttons.add(b);
            }
            footer.add(buttons, BorderLayout.SOUTH);
            add(footer, BorderLayout.SOUTH);

            btnHuy.addActionListener(e -> {
                if (!dangLuu) {
                    Window w = SwingUtilities.getWindowAncestor(this);
                    if (w != null) {
                        w.dispose();
                    }
                }
            });
            btnLuu.addActionListener(e -> luu());
            txtMoi.addActionListener(e -> luu());
            txtXacNhan.addActionListener(e -> luu());
        }

        private void themTruong(JPanel fields, GridBagConstraints c, String ten,
                JPasswordField input, String name) {
            c.gridy++;
            JLabel label = new JLabel(ten);
            label.setFont(font(12));
            label.setLabelFor(input);
            fields.add(label, c);
            c.gridy++;
            input.setName(name);
            input.setFont(font(14));
            input.setPreferredSize(new Dimension(440, 38));
            fields.add(input, c);
        }

        private void luu() {
            if (dangLuu) {
                return;
            }
            char[] first = txtMoi.getPassword();
            char[] second = txtXacNhan.getPassword();
            String moi = new String(first);
            String nhapLai = new String(second);
            Arrays.fill(first, '\0');
            Arrays.fill(second, '\0');
            String loi = KiemTraXacThuc.loiDoiMatKhauLanDau(moi, nhapLai);
            if (loi != null) {
                lblThongBao.setForeground(new Color(0xC94C48));
                lblThongBao.setText("<html>" + loi + "</html>");
                return;
            }
            dangLuu(true);
            lblThongBao.setForeground(MUTED);
            lblThongBao.setText("Đang đổi mật khẩu...");
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    new TaiKhoanDAO().doiMatKhau(maTK, otp, moi);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(NoiDung.this),
                                "Đặt lại mật khẩu thành công!\nVui lòng đăng nhập lại.",
                                "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        Window w = SwingUtilities.getWindowAncestor(NoiDung.this);
                        if (w != null) {
                            w.dispose();
                        }
                    } catch (ExecutionException ex) {
                        dangLuu(false);
                        Throwable cause = ex.getCause();
                        lblThongBao.setForeground(new Color(0xC94C48));
                        lblThongBao.setText(cause instanceof BusinessException
                                || cause instanceof DataAccessException
                                ? cause.getMessage() : "Không đổi được mật khẩu. Vui lòng thử lại.");
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        dangLuu(false);
                    }
                }
            }.execute();
        }

        private void dangLuu(boolean value) {
            dangLuu = value;
            txtMoi.setEnabled(!value);
            txtXacNhan.setEnabled(!value);
            btnHuy.setEnabled(!value);
            btnLuu.setEnabled(!value);
            btnLuu.setText(value ? "Đang lưu..." : "Xác nhận");
        }
    }
}
