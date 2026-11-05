package com.quanlynhakhoa.ui.dialog;

import com.quanlynhakhoa.dao.TaiKhoanDAO;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;
import com.quanlynhakhoa.util.GuiMail;
import com.quanlynhakhoa.util.KiemTraXacThuc;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import java.awt.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Bước 1 quên mật khẩu: nhập số điện thoại hoặc email để nhận mã OTP qua email. */
public class DlgQuenMatKhau extends JDialog {

    public DlgQuenMatKhau(Window owner) {
        super(owner, "Quên mật khẩu", ModalityType.APPLICATION_MODAL);
        setName("quenMatKhau");
        NoiDung form = new NoiDung(this::moNhapOtp);
        setContentPane(form);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(form.btnGuiMa);
        setMinimumSize(new Dimension(460, 300));
        pack();
        setLocationRelativeTo(owner);
    }

    private void moNhapOtp(TaiKhoanDAO.ThongTinDatLai tt) {
        Window owner = getOwner();
        dispose();
        new DlgNhapOtp(owner, tt.maTK(), tt.email()).setVisible(true);
    }

    static final class NoiDung extends JPanel {
        private final JTextField txtLienHe = new JTextField();
        private final JLabel lblThongBao = new JLabel(" ");
        private final JButton btnGuiMa = new JButton("Gửi mã xác thực");
        private final JButton btnHuy = new JButton("Hủy");
        private final Consumer<TaiKhoanDAO.ThongTinDatLai> thanhCong;
        private boolean dangGui;

        NoiDung(Consumer<TaiKhoanDAO.ThongTinDatLai> thanhCong) {
            super(new BorderLayout(0, 16));
            this.thanhCong = thanhCong;
            setBackground(SURFACE);
            setBorder(new EmptyBorder(24, 24, 24, 24));

            JPanel header = new JPanel();
            header.setOpaque(false);
            header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
            JLabel title = new JLabel("Đặt lại mật khẩu");
            title.setFont(bold(22));
            title.setForeground(TEXT);
            JLabel sub = new JLabel("<html>Nhập số điện thoại hoặc email của bạn. "
                    + "Mã xác thực sẽ được gửi tới email đã liên kết với tài khoản.</html>");
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
            JLabel lbl = new JLabel("Số điện thoại hoặc email");
            lbl.setFont(font(12));
            lbl.setLabelFor(txtLienHe);
            fields.add(lbl, c);
            c.gridy++;
            txtLienHe.setName("lienHe");
            txtLienHe.setFont(font(14));
            txtLienHe.setPreferredSize(new Dimension(400, 38));
            txtLienHe.putClientProperty("JTextField.placeholderText", "VD: 0912345678 hoặc nv@clinic.vn");
            fields.add(txtLienHe, c);
            add(fields, BorderLayout.CENTER);

            JPanel footer = new JPanel(new BorderLayout(0, 12));
            footer.setOpaque(false);
            lblThongBao.setName("thongBaoQuenMatKhau");
            lblThongBao.setFont(font(12));
            lblThongBao.setForeground(new Color(0xC94C48));
            lblThongBao.setPreferredSize(new Dimension(400, 34));
            footer.add(lblThongBao, BorderLayout.NORTH);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            buttons.setOpaque(false);
            btnGuiMa.setName("guiMa");
            btnHuy.setName("huyQuenMatKhau");
            btnGuiMa.setBackground(TEAL);
            btnGuiMa.setForeground(Color.WHITE);
            for (JButton b : new JButton[]{btnHuy, btnGuiMa}) {
                b.setFont(bold(12));
                buttons.add(b);
            }
            footer.add(buttons, BorderLayout.SOUTH);
            add(footer, BorderLayout.SOUTH);

            btnHuy.addActionListener(e -> {
                if (!dangGui) {
                    Window w = SwingUtilities.getWindowAncestor(this);
                    if (w != null) {
                        w.dispose();
                    }
                }
            });
            btnGuiMa.addActionListener(e -> guiMa());
            txtLienHe.addActionListener(e -> guiMa());
        }

        private void guiMa() {
            if (dangGui) {
                return;
            }
            String lienHe = txtLienHe.getText().trim();
            if (!KiemTraXacThuc.laLienHeHopLe(lienHe)) {
                lblThongBao.setForeground(new Color(0xC94C48));
                lblThongBao.setText("Số điện thoại hoặc email không đúng định dạng.");
                txtLienHe.requestFocus();
                return;
            }
            dangGui(true);
            lblThongBao.setForeground(MUTED);
            lblThongBao.setText("Đang gửi mã...");
            new SwingWorker<TaiKhoanDAO.ThongTinDatLai, Void>() {
                @Override
                protected TaiKhoanDAO.ThongTinDatLai doInBackground() {
                    TaiKhoanDAO dao = new TaiKhoanDAO();
                    TaiKhoanDAO.ThongTinDatLai tt = dao.traCuuDatLai(lienHe);
                    String otp = dao.taoOtp(tt.maTK());
                    GuiMail.guiOtp(tt.email(), otp);
                    return tt;
                }

                @Override
                protected void done() {
                    try {
                        thanhCong.accept(get());
                    } catch (ExecutionException ex) {
                        dangGui(false);
                        Throwable cause = ex.getCause();
                        lblThongBao.setForeground(new Color(0xC94C48));
                        lblThongBao.setText(cause instanceof BusinessException
                                || cause instanceof DataAccessException
                                ? cause.getMessage() : "Không gửi được mã. Vui lòng thử lại.");
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        dangGui(false);
                    }
                }
            }.execute();
        }

        private void dangGui(boolean value) {
            dangGui = value;
            txtLienHe.setEnabled(!value);
            btnHuy.setEnabled(!value);
            btnGuiMa.setEnabled(!value);
            btnGuiMa.setText(value ? "Đang gửi..." : "Gửi mã xác thực");
        }
    }
}
