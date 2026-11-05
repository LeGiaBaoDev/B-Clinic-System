package com.quanlynhakhoa.ui.dialog;

import com.quanlynhakhoa.dao.TaiKhoanDAO;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;
import com.quanlynhakhoa.util.GuiMail;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import java.awt.*;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Bước 2 quên mật khẩu: nhập mã OTP đã gửi tới email liên kết. */
public class DlgNhapOtp extends JDialog {

    public DlgNhapOtp(Window owner, String maTK, String email) {
        super(owner, "Xác thực mã OTP", ModalityType.APPLICATION_MODAL);
        setName("nhapOtp");
        NoiDung form = new NoiDung(maTK, email, owner);
        setContentPane(form);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(form.btnXacThuc);
        setMinimumSize(new Dimension(460, 340));
        pack();
        setLocationRelativeTo(owner);
    }

    static final class NoiDung extends JPanel {
        private final String maTK;
        private final String email;
        private final Window chuSoHuu;
        private final JTextField txtOtp = new JTextField();
        private final JLabel lblThongBao = new JLabel(" ");
        private final JButton btnXacThuc = new JButton("Xác thực");
        private final JButton btnGuiLai = new JButton("Gửi lại mã");
        private final JButton btnHuy = new JButton("Hủy");
        private boolean dangXuLy;

        NoiDung(String maTK, String email, Window chuSoHuu) {
            super(new BorderLayout(0, 16));
            this.maTK = maTK;
            this.email = email;
            this.chuSoHuu = chuSoHuu;
            setBackground(SURFACE);
            setBorder(new EmptyBorder(24, 24, 24, 24));

            JPanel header = new JPanel();
            header.setOpaque(false);
            header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
            JLabel title = new JLabel("Nhập mã xác thực");
            title.setFont(bold(22));
            title.setForeground(TEXT);
            JLabel sub = new JLabel("<html>Mã gồm 6 chữ số đã được gửi tới <b>" + email
                    + "</b>. Mã có hiệu lực 5 phút.</html>");
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
            c.gridy = 0;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 0, 8, 0);
            JLabel lbl = new JLabel("Mã xác thực (OTP)");
            lbl.setFont(font(12));
            lbl.setLabelFor(txtOtp);
            fields.add(lbl, c);
            c.gridy++;
            txtOtp.setName("otp");
            txtOtp.setFont(font(18));
            txtOtp.setPreferredSize(new Dimension(400, 42));
            txtOtp.putClientProperty("JTextField.placeholderText", "Nhập 6 chữ số");
            fields.add(txtOtp, c);
            add(fields, BorderLayout.CENTER);

            JPanel footer = new JPanel(new BorderLayout(0, 12));
            footer.setOpaque(false);
            lblThongBao.setName("thongBaoOtp");
            lblThongBao.setFont(font(12));
            lblThongBao.setForeground(new Color(0xC94C48));
            lblThongBao.setPreferredSize(new Dimension(400, 34));
            footer.add(lblThongBao, BorderLayout.NORTH);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            buttons.setOpaque(false);
            btnXacThuc.setName("xacThucOtp");
            btnGuiLai.setName("guiLaiOtp");
            btnHuy.setName("huyNhapOtp");
            btnXacThuc.setBackground(TEAL);
            btnXacThuc.setForeground(Color.WHITE);
            for (JButton b : new JButton[]{btnHuy, btnGuiLai, btnXacThuc}) {
                b.setFont(bold(12));
                buttons.add(b);
            }
            footer.add(buttons, BorderLayout.SOUTH);
            add(footer, BorderLayout.SOUTH);

            btnHuy.addActionListener(e -> {
                if (!dangXuLy) {
                    Window w = SwingUtilities.getWindowAncestor(this);
                    if (w != null) {
                        w.dispose();
                    }
                }
            });
            btnXacThuc.addActionListener(e -> xacThuc());
            btnGuiLai.addActionListener(e -> guiLai());
            txtOtp.addActionListener(e -> xacThuc());
        }

        private void xacThuc() {
            if (dangXuLy) {
                return;
            }
            String otp = txtOtp.getText().trim();
            if (!otp.matches("\\d{6}")) {
                lblThongBao.setForeground(new Color(0xC94C48));
                lblThongBao.setText("Mã xác thực phải gồm 6 chữ số.");
                txtOtp.requestFocus();
                return;
            }
            dangXuLy(true);
            lblThongBao.setForeground(MUTED);
            lblThongBao.setText("Đang xác thực...");
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    new TaiKhoanDAO().xacThucOtp(maTK, otp);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        moDatMatKhauMoi(otp);
                    } catch (ExecutionException ex) {
                        dangXuLy(false);
                        Throwable cause = ex.getCause();
                        lblThongBao.setForeground(new Color(0xC94C48));
                        lblThongBao.setText(cause instanceof BusinessException
                                || cause instanceof DataAccessException
                                ? cause.getMessage() : "Không xác thực được mã. Vui lòng thử lại.");
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        dangXuLy(false);
                    }
                }
            }.execute();
        }

        private void moDatMatKhauMoi(String otp) {
            Window cuaSo = SwingUtilities.getWindowAncestor(this);
            if (cuaSo != null) {
                cuaSo.dispose();
            }
            new DlgDatMatKhauMoi(chuSoHuu, maTK, otp).setVisible(true);
        }

        private void guiLai() {
            if (dangXuLy) {
                return;
            }
            dangXuLy(true);
            lblThongBao.setForeground(MUTED);
            lblThongBao.setText("Đang gửi lại mã...");
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    TaiKhoanDAO dao = new TaiKhoanDAO();
                    String otp = dao.taoOtp(maTK);
                    GuiMail.guiOtp(email, otp);
                    return null;
                }

                @Override
                protected void done() {
                    dangXuLy(false);
                    try {
                        get();
                        txtOtp.setText("");
                        lblThongBao.setForeground(GREEN);
                        lblThongBao.setText("Đã gửi mã mới tới " + email + ".");
                        txtOtp.requestFocus();
                    } catch (ExecutionException ex) {
                        Throwable cause = ex.getCause();
                        lblThongBao.setForeground(new Color(0xC94C48));
                        lblThongBao.setText(cause instanceof BusinessException
                                || cause instanceof DataAccessException
                                ? cause.getMessage() : "Không gửi lại được mã. Vui lòng thử lại.");
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }.execute();
        }

        private void dangXuLy(boolean value) {
            dangXuLy = value;
            txtOtp.setEnabled(!value);
            btnHuy.setEnabled(!value);
            btnGuiLai.setEnabled(!value);
            btnXacThuc.setEnabled(!value);
            btnXacThuc.setText(value ? "Đang xử lý..." : "Xác thực");
        }
    }
}
