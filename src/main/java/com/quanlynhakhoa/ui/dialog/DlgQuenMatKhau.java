package com.quanlynhakhoa.ui.dialog;

import com.quanlynhakhoa.dao.TaiKhoanDAO;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Dialog quên mật khẩu — 1 bước (UC001 bước 5.5–5.10):
 *   Nhập Mã nhân viên + Mật khẩu mới + Xác nhận mật khẩu mới.
 */
public class DlgQuenMatKhau extends JDialog {

    // ── Màu ──────────────────────────────────────────────────────────────────
    private static final Color CLR_TEAL       = new Color(0x1A7A7A);
    private static final Color CLR_TEAL_HOVER = new Color(0x15686A);
    private static final Color CLR_TEAL_PRESS = new Color(0x115658);
    private static final Color CLR_ERROR      = new Color(0xCC2222);
    private static final Color CLR_SUCCESS    = new Color(0x1A7A7A);
    private static final Color CLR_BORDER     = new Color(0xCCCCCC);
    private static final Color CLR_FOCUS      = new Color(0x1A7A7A);
    private static final Color CLR_LABEL      = new Color(0x333333);
    private static final Color CLR_SUBLABEL   = new Color(0x666666);

    // Font
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD,  18);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FONT_INPUT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_BTN   = new Font("Segoe UI", Font.BOLD,  13);

    // Components
    private JTextField     txtMaNV;
    private JPasswordField txtMatKhauMoi;
    private JPasswordField txtNhapLai;
    private JButton        btnXacNhan;
    private JButton        btnToggle1;
    private JButton        btnToggle2;
    private JLabel         lblThongBao;
    private boolean        hien1 = false;
    private boolean        hien2 = false;

    private final TaiKhoanDAO taiKhoanDAO = new TaiKhoanDAO();

    public DlgQuenMatKhau(Window owner) {
        super(owner, "Quên mật khẩu", ModalityType.APPLICATION_MODAL);
        khoiTao();
    }

    private void khoiTao() {
        setSize(420, 460);
        setMinimumSize(new Dimension(380, 420));
        setLocationRelativeTo(getOwner());
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(28, 32, 24, 32));
        setContentPane(root);

        // Header
        JLabel lblTitle = new JLabel("Đặt lại mật khẩu");
        lblTitle.setFont(FONT_TITLE);
        lblTitle.setForeground(CLR_LABEL);

        JLabel lblSub = new JLabel("<html>Nhập mã nhân viên và mật khẩu mới của bạn.</html>");
        lblSub.setFont(FONT_SMALL);
        lblSub.setForeground(CLR_SUBLABEL);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(lblTitle);
        header.add(Box.createVerticalStrut(4));
        header.add(lblSub);

        // Form
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        txtMaNV = taoInput("VD: NV001");
        JPanel pnlMk1 = taoPasswordPanel(true);
        JPanel pnlMk2 = taoPasswordPanel(false);

        btnXacNhan = taoBtnChinh("Xác nhập đặt lại mật khẩu");
        btnXacNhan.addActionListener(e -> thucHien());

        JButton btnHuy = taoBtnPhu("Hủy");
        btnHuy.addActionListener(e -> dispose());

        // Thông báo lỗi/thành công
        lblThongBao = new JLabel(" ");
        lblThongBao.setFont(FONT_SMALL);
        lblThongBao.setForeground(CLR_ERROR);
        lblThongBao.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(taoLabel("Mã nhân viên *"));
        form.add(Box.createVerticalStrut(5));
        form.add(txtMaNV);
        form.add(Box.createVerticalStrut(14));
        form.add(taoLabel("Mật khẩu mới *"));
        form.add(Box.createVerticalStrut(5));
        form.add(pnlMk1);
        form.add(Box.createVerticalStrut(14));
        form.add(taoLabel("Xác nhập mật khẩu *"));
        form.add(Box.createVerticalStrut(5));
        form.add(pnlMk2);
        form.add(Box.createVerticalStrut(6));
        form.add(lblThongBao);
        form.add(Box.createVerticalStrut(14));
        form.add(btnXacNhan);
        form.add(Box.createVerticalStrut(8));
        form.add(btnHuy);

        root.add(header, BorderLayout.NORTH);
        root.add(form,   BorderLayout.CENTER);
    }

    // Logic

    private void thucHien() {
        String maNV = txtMaNV.getText().trim();
        String mk1  = new String(txtMatKhauMoi.getPassword());
        String mk2  = new String(txtNhapLai.getPassword());

        // Validate phía client
        if (maNV.isEmpty()) {
            hienLoi("Vui lòng nhập mã nhân viên.");
            txtMaNV.requestFocus();
            return;
        }
        if (mk1.length() < 6) {
            hienLoi("Mật khẩu phải có ít nhất 6 ký tự.");
            txtMatKhauMoi.requestFocus();
            return;
        }
        if (!mk1.equals(mk2)) {
            hienLoi("Mật khẩu xác nhập không khớp.");
            txtNhapLai.requestFocus();
            return;
        }

        btnXacNhan.setEnabled(false);
        btnXacNhan.setText("Đang xử lý...");
        hienLoi("");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override protected Void doInBackground() {
                String maTK = taiKhoanDAO.xacMinhMaNV(maNV);
                taiKhoanDAO.doiMatKhau(maTK, mk1);
                return null;
            }
            @Override protected void done() {
                btnXacNhan.setEnabled(true);
                btnXacNhan.setText("Xác nhập đặt lại mật khẩu");
                try {
                    get();
                    JOptionPane.showMessageDialog(DlgQuenMatKhau.this,
                            "Đặt lại mật khẩu thành công!\nVui lòng đăng nhập lại.",
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    hienLoi(cause instanceof BusinessException || cause instanceof DataAccessException
                            ? cause.getMessage()
                            : "Xảy ra lỗi không xác định. Vui lòng thử lại.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        worker.execute();
    }

    private void hienLoi(String msg) {
        lblThongBao.setText(msg.isEmpty() ? " " : msg);
        lblThongBao.setForeground(CLR_ERROR);
    }

    // Helper UI

    private JLabel taoLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(CLR_LABEL);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JTextField taoInput(String placeholder) {
        JTextField tf = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    g2.setFont(getFont());
                    g2.setColor(new Color(0xAAAAAA));
                    Insets ins = getInsets();
                    g2.drawString(placeholder, ins.left + 2,
                            getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 1);
                }
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isFocusOwner() ? CLR_FOCUS : CLR_BORDER);
                g2.setStroke(new BasicStroke(isFocusOwner() ? 2f : 1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        tf.setFont(FONT_INPUT);
        tf.setBackground(Color.WHITE);
        tf.setOpaque(false);
        tf.setBorder(new EmptyBorder(9, 12, 9, 12));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
        tf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { tf.repaint(); }
            @Override public void focusLost(FocusEvent e)   { tf.repaint(); }
        });
        return tf;
    }

    private JPanel taoPasswordPanel(boolean laMk1) {
        JPasswordField pf = new JPasswordField() {
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isFocusOwner() ? CLR_FOCUS : CLR_BORDER);
                g2.setStroke(new BasicStroke(isFocusOwner() ? 2f : 1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        pf.setFont(FONT_INPUT);
        pf.setBackground(Color.WHITE);
        pf.setOpaque(false);
        pf.setBorder(new EmptyBorder(9, 12, 9, 8));
        pf.setEchoChar('●');
        pf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { pf.repaint(); }
            @Override public void focusLost(FocusEvent e)   { pf.repaint(); }
        });

        if (laMk1) {
            txtMatKhauMoi = pf;
        } else {
            txtNhapLai = pf;
        }

        // Eye icon toggle
        JButton btnToggle = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hien = laMk1 ? hien1 : hien2;
                int cx = getWidth() / 2, cy = getHeight() / 2;
                g2.setColor(new Color(0xBBBBBB));
                g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawArc(cx - 8, cy - 5, 16, 10, 0, 180);
                g2.drawArc(cx - 8, cy - 5, 16, 10, 180, 180);
                g2.fillOval(cx - 2, cy - 2, 5, 5);
                if (!hien) {
                    g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(cx - 7, cy + 5, cx + 7, cy - 6);
                }
                g2.dispose();
            }
        };
        btnToggle.setBorderPainted(false);
        btnToggle.setContentAreaFilled(false);
        btnToggle.setFocusPainted(false);
        btnToggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnToggle.setPreferredSize(new Dimension(36, 36));
        btnToggle.addActionListener(e -> {
            if (laMk1) {
                hien1 = !hien1;
                pf.setEchoChar(hien1 ? (char) 0 : '●');
            } else {
                hien2 = !hien2;
                pf.setEchoChar(hien2 ? (char) 0 : '●');
            }
            btnToggle.repaint();
        });

        if (laMk1) btnToggle1 = btnToggle;
        else       btnToggle2 = btnToggle;

        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {}
        };
        wrapper.setOpaque(false);
        wrapper.add(pf,        BorderLayout.CENTER);
        wrapper.add(btnToggle, BorderLayout.EAST);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        return wrapper;
    }

    private JButton taoBtnChinh(String text) {
        JButton btn = new JButton(text) {
            private Color bg = CLR_TEAL;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e)  { bg = CLR_TEAL_HOVER; repaint(); }
                    @Override public void mouseExited(MouseEvent e)   { bg = CLR_TEAL;       repaint(); }
                    @Override public void mousePressed(MouseEvent e)  { bg = CLR_TEAL_PRESS; repaint(); }
                    @Override public void mouseReleased(MouseEvent e) { bg = CLR_TEAL_HOVER; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isEnabled() ? bg : new Color(0xAAAAAA));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setFont(getFont());
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {}
        };
        btn.setFont(FONT_BTN);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        return btn;
    }

    private JButton taoBtnPhu(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xF0F0F0));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setFont(getFont());
                g2.setColor(CLR_LABEL);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {}
        };
        btn.setFont(FONT_BTN);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        return btn;
    }
}
