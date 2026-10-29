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
 * Dialog quên mật khẩu — 2 bước:
 *   Bước 1: Nhập họ tên + SĐT + email để xác minh danh tính.
 *   Bước 2: Nhập mật khẩu mới + xác nhận lại.
 */
public class DlgQuenMatKhau extends JDialog {

    // ── Màu ──────────────────────────────────────────────────────────────────
    private static final Color CLR_TEAL         = new Color(0x1A7A7A);
    private static final Color CLR_TEAL_HOVER   = new Color(0x15686A);
    private static final Color CLR_TEAL_PRESS   = new Color(0x115658);
    private static final Color CLR_ERROR        = new Color(0xCC2222);
    private static final Color CLR_SUCCESS      = new Color(0x1A7A7A);
    private static final Color CLR_BORDER       = new Color(0xCCCCCC);
    private static final Color CLR_FOCUS        = new Color(0x1A7A7A);
    private static final Color CLR_LABEL        = new Color(0x333333);
    private static final Color CLR_SUBLABEL     = new Color(0x666666);
    private static final Color CLR_BG           = Color.WHITE;
    private static final Color CLR_STEP_ACTIVE  = new Color(0x1A7A7A);
    private static final Color CLR_STEP_DONE    = new Color(0x22AA77);
    private static final Color CLR_STEP_INACTIVE= new Color(0xCCCCCC);

    // ── Font ─────────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD,  18);
    private static final Font FONT_LABEL  = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FONT_INPUT  = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_BTN    = new Font("Segoe UI", Font.BOLD,  13);

    // ── State ─────────────────────────────────────────────────────────────────
    private int buocHienTai = 1;           // 1 = xác minh, 2 = đặt mật khẩu mới
    private String maTKDaXacMinh = null;   // lưu sau bước 1

    // ── Bước 1 ───────────────────────────────────────────────────────────────
    private JTextField     txtHoTen;
    private JTextField     txtSdt;
    private JTextField     txtEmail;
    private JButton        btnXacMinh;

    // ── Bước 2 ───────────────────────────────────────────────────────────────
    private JPasswordField txtMatKhauMoi;
    private JPasswordField txtNhapLai;
    private JButton        btnDoiMatKhau;
    private JButton        btnToggle1;
    private JButton        btnToggle2;
    private boolean hien1 = false;
    private boolean hien2 = false;

    // ── Shared ───────────────────────────────────────────────────────────────
    private JLabel  lblThongBao;           // dòng lỗi / thành công
    private JPanel  cardContainer;        // chứa CardLayout bước 1/2
    private JPanel  pnlStepIndicator;

    private static final String CARD_BUOC1 = "buoc1";
    private static final String CARD_BUOC2 = "buoc2";

    private final TaiKhoanDAO taiKhoanDAO = new TaiKhoanDAO();

    // ─────────────────────────────────────────────────────────────────────────

    public DlgQuenMatKhau(Window owner) {
        super(owner, "Qu\u00ean m\u1eadt kh\u1ea9u", ModalityType.APPLICATION_MODAL);
        khoiTao();
    }

    // ── Khởi tạo ─────────────────────────────────────────────────────────────

    private void khoiTao() {
        setSize(440, 520);
        setMinimumSize(new Dimension(400, 480));
        setLocationRelativeTo(getOwner());
        setResizable(false);
        getRootPane().putClientProperty("FlatLaf.style", "arc: 12");

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(CLR_BG);
        root.setBorder(new EmptyBorder(28, 32, 24, 32));
        setContentPane(root);

        // ── Header ──
        JLabel lblTitle = new JLabel("\u0110\u1eb7t l\u1ea1i m\u1eadt kh\u1ea9u");
        lblTitle.setFont(FONT_TITLE);
        lblTitle.setForeground(CLR_LABEL);

        JLabel lblSub = new JLabel("<html>Nh\u1eadp th\u00f4ng tin \u0111\u1ec3 x\u00e1c minh danh t\u00ednh v\u00e0 \u0111\u1eb7t l\u1ea1i m\u1eadt kh\u1ea9u.</html>");
        lblSub.setFont(FONT_SMALL);
        lblSub.setForeground(CLR_SUBLABEL);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(lblTitle);
        header.add(Box.createVerticalStrut(4));
        header.add(lblSub);
        header.add(Box.createVerticalStrut(16));
        header.add(taoStepIndicator());
        header.add(Box.createVerticalStrut(4));

        // ── Thông báo lỗi/thành công ──
        lblThongBao = new JLabel(" ");
        lblThongBao.setFont(FONT_SMALL);
        lblThongBao.setForeground(CLR_ERROR);
        lblThongBao.setBorder(new EmptyBorder(4, 0, 0, 0));

        // ── CardLayout chứa 2 bước ──
        cardContainer = new JPanel(new CardLayout());
        cardContainer.setOpaque(false);
        cardContainer.add(taoBuoc1(), CARD_BUOC1);
        cardContainer.add(taoBuoc2(), CARD_BUOC2);

        root.add(header,        BorderLayout.NORTH);
        root.add(cardContainer, BorderLayout.CENTER);
        root.add(lblThongBao,   BorderLayout.SOUTH);
    }

    // ── Step indicator ───────────────────────────────────────────────────────

    private JPanel taoStepIndicator() {
        pnlStepIndicator = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth(), cy = getHeight() / 2;
                int r = 11; // bán kính vòng tròn
                int cx1 = 20, cx2 = w - 20;
                int midX = (cx1 + cx2) / 2;

                // Đường nối
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(buocHienTai >= 2 ? CLR_STEP_DONE : CLR_STEP_INACTIVE);
                g2.drawLine(cx1 + r, cy, cx2 - r, cy);

                // Vòng 1
                Color c1 = (buocHienTai == 1) ? CLR_STEP_ACTIVE : CLR_STEP_DONE;
                g2.setColor(c1);
                g2.fillOval(cx1 - r, cy - r, r * 2, r * 2);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                drawCenteredString(g2, buocHienTai > 1 ? "\u2713" : "1", cx1, cy);

                // Vòng 2
                Color c2 = (buocHienTai == 2) ? CLR_STEP_ACTIVE
                         : (buocHienTai > 2   ? CLR_STEP_DONE : CLR_STEP_INACTIVE);
                g2.setColor(c2);
                g2.fillOval(cx2 - r, cy - r, r * 2, r * 2);
                g2.setColor(buocHienTai >= 2 ? Color.WHITE : new Color(0x999999));
                drawCenteredString(g2, "2", cx2, cy);

                // Nhãn
                g2.setFont(FONT_SMALL);
                g2.setColor(buocHienTai == 1 ? CLR_STEP_ACTIVE : CLR_SUBLABEL);
                drawCenteredString(g2, "X\u00e1c minh", cx1, cy + r + 12);
                g2.setColor(buocHienTai == 2 ? CLR_STEP_ACTIVE : CLR_SUBLABEL);
                drawCenteredString(g2, "M\u1eadt kh\u1ea9u m\u1edbi", cx2, cy + r + 12);

                g2.dispose();
            }

            private void drawCenteredString(Graphics2D g2, String s, int cx, int cy) {
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(s, cx - fm.stringWidth(s) / 2, cy + fm.getAscent() / 2 - 1);
            }
        };
        pnlStepIndicator.setOpaque(false);
        pnlStepIndicator.setPreferredSize(new Dimension(0, 52));
        return pnlStepIndicator;
    }

    // ── Bước 1: xác minh ─────────────────────────────────────────────────────

    private JPanel taoBuoc1() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(8, 0, 0, 0));

        txtHoTen = taoInput("H\u1ecd v\u00e0 t\u00ean");
        txtSdt   = taoInput("S\u1ed1 \u0111i\u1ec7n tho\u1ea1i");
        txtEmail = taoInput("Email");

        btnXacMinh = taoBtnChinh("G\u1eedi x\u00e1c minh");
        btnXacMinh.addActionListener(e -> thucHienXacMinh());

        JButton btnHuy = taoBtnPhu("H\u1ee7y");
        btnHuy.addActionListener(e -> dispose());

        p.add(taoLabelRow("H\u1ecd v\u00e0 t\u00ean *"));
        p.add(Box.createVerticalStrut(5));
        p.add(txtHoTen);
        p.add(Box.createVerticalStrut(12));
        p.add(taoLabelRow("S\u1ed1 \u0111i\u1ec7n tho\u1ea1i *"));
        p.add(Box.createVerticalStrut(5));
        p.add(txtSdt);
        p.add(Box.createVerticalStrut(12));
        p.add(taoLabelRow("Email *"));
        p.add(Box.createVerticalStrut(5));
        p.add(txtEmail);
        p.add(Box.createVerticalStrut(20));
        p.add(btnXacMinh);
        p.add(Box.createVerticalStrut(8));
        p.add(btnHuy);
        return p;
    }

    // ── Bước 2: mật khẩu mới ─────────────────────────────────────────────────

    private JPanel taoBuoc2() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(8, 0, 0, 0));

        // Thông báo xác minh thành công
        JLabel lblOk = new JLabel("\u2713  X\u00e1c minh danh t\u00ednh th\u00e0nh c\u00f4ng!");
        lblOk.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblOk.setForeground(CLR_SUCCESS);
        lblOk.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Field mật khẩu mới
        JPanel pnlMk1 = taoPasswordRow(true);
        // Field nhập lại
        JPanel pnlMk2 = taoPasswordRow(false);

        // Thanh độ mạnh
        JLabel lblStrengthTitle = new JLabel("M\u1ee9c \u0111\u1ed9 m\u1eadt kh\u1ea9u:");
        lblStrengthTitle.setFont(FONT_SMALL);
        lblStrengthTitle.setForeground(CLR_SUBLABEL);
        lblStrengthTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel pnlStrength = taoStrengthBar();

        btnDoiMatKhau = taoBtnChinh("\u0110\u1eb7t l\u1ea1i m\u1eadt kh\u1ea9u");
        btnDoiMatKhau.addActionListener(e -> thucHienDoiMatKhau());

        JButton btnQuayLai = taoBtnPhu("\u2190  Qu\u00e1y l\u1ea1i");
        btnQuayLai.addActionListener(e -> chuyenBuoc(1));

        p.add(lblOk);
        p.add(Box.createVerticalStrut(14));
        p.add(taoLabelRow("M\u1eadt kh\u1ea9u m\u1edbi *"));
        p.add(Box.createVerticalStrut(5));
        p.add(pnlMk1);
        p.add(Box.createVerticalStrut(5));
        p.add(lblStrengthTitle);
        p.add(Box.createVerticalStrut(3));
        p.add(pnlStrength);
        p.add(Box.createVerticalStrut(12));
        p.add(taoLabelRow("Nh\u1eadp l\u1ea1i m\u1eadt kh\u1ea9u *"));
        p.add(Box.createVerticalStrut(5));
        p.add(pnlMk2);
        p.add(Box.createVerticalStrut(20));
        p.add(btnDoiMatKhau);
        p.add(Box.createVerticalStrut(8));
        p.add(btnQuayLai);
        return p;
    }

    // ── Logic ─────────────────────────────────────────────────────────────────

    private void thucHienXacMinh() {
        String hoTen = txtHoTen.getText().trim();
        String sdt   = txtSdt.getText().trim();
        String email = txtEmail.getText().trim();

        btnXacMinh.setEnabled(false);
        btnXacMinh.setText("Đang xác minh...");
        hienThongBao("", false);

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override protected String doInBackground() {
                return taiKhoanDAO.xacMinhDanhTinh(hoTen, sdt, email);
            }
            @Override protected void done() {
                btnXacMinh.setEnabled(true);
                btnXacMinh.setText("G\u1eedi x\u00e1c minh");
                try {
                    maTKDaXacMinh = get();
                    chuyenBuoc(2);
                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    hienThongBao(cause != null ? cause.getMessage()
                            : "X\u1ea3y ra l\u1ed7i kh\u00f4ng x\u00e1c \u0111\u1ecbnh.", true);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        worker.execute();
    }

    private void thucHienDoiMatKhau() {
        String mk1 = new String(txtMatKhauMoi.getPassword());
        String mk2 = new String(txtNhapLai.getPassword());

        if (mk1.length() < 6) {
            hienThongBao("M\u1eadt kh\u1ea9u ph\u1ea3i c\u00f3 \u00edt nh\u1ea5t 6 k\u00fd t\u1ef1.", true);
            txtMatKhauMoi.requestFocus();
            return;
        }
        if (!mk1.equals(mk2)) {
            hienThongBao("M\u1eadt kh\u1ea9u nh\u1eadp l\u1ea1i kh\u00f4ng kh\u1edbp.", true);
            txtNhapLai.requestFocus();
            return;
        }

        btnDoiMatKhau.setEnabled(false);
        btnDoiMatKhau.setText("\u0110ang c\u1eadp nh\u1eadt...");
        hienThongBao("", false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override protected Void doInBackground() {
                taiKhoanDAO.doiMatKhau(maTKDaXacMinh, mk1);
                return null;
            }
            @Override protected void done() {
                btnDoiMatKhau.setEnabled(true);
                btnDoiMatKhau.setText("\u0110\u1eb7t l\u1ea1i m\u1eadt kh\u1ea9u");
                try {
                    get();
                    JOptionPane.showMessageDialog(DlgQuenMatKhau.this,
                            "\u0110\u1eb7t l\u1ea1i m\u1eadt kh\u1ea9u th\u00e0nh c\u00f4ng!\nVui l\u00f2ng \u0111\u0103ng nh\u1eadp l\u1ea1i.",
                            "Th\u00e0nh c\u00f4ng", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    hienThongBao(cause != null ? cause.getMessage()
                            : "Kh\u00f4ng th\u1ec3 c\u1eadp nh\u1eadt m\u1eadt kh\u1ea9u.", true);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        worker.execute();
    }

    private void chuyenBuoc(int buoc) {
        buocHienTai = buoc;
        pnlStepIndicator.repaint();
        hienThongBao("", false);
        ((CardLayout) cardContainer.getLayout()).show(cardContainer,
                buoc == 1 ? CARD_BUOC1 : CARD_BUOC2);
    }

    private void hienThongBao(String msg, boolean laLoi) {
        lblThongBao.setText(msg.isEmpty() ? " " : msg);
        lblThongBao.setForeground(laLoi ? CLR_ERROR : CLR_SUCCESS);
    }

    // ── Helper UI ─────────────────────────────────────────────────────────────

    private JLabel taoLabelRow(String text) {
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

    /** Tạo panel password field + nút toggle ẩn/hiện (tick-box style) */
    private JPanel taoPasswordRow(boolean laMk1) {
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
        pf.setEchoChar('\u25cf');
        pf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { pf.repaint(); }
            @Override public void focusLost(FocusEvent e)   { pf.repaint(); }
        });

        if (laMk1) {
            txtMatKhauMoi = pf;
            // Cập nhật thanh độ mạnh khi gõ
            pf.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override public void insertUpdate(javax.swing.event.DocumentEvent e)  { capNhatDomManh(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent e)  { capNhatDomManh(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent e) {}
            });
        } else {
            txtNhapLai = pf;
        }

        JButton btnToggle = taoToggleBtn(laMk1);

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
        wrapper.add(pf, BorderLayout.CENTER);
        wrapper.add(btnToggle, BorderLayout.EAST);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        return wrapper;
    }

    /** Nút ô vuông: viền xám = ẩn, nền teal + tick = hiện */
    private JButton taoToggleBtn(boolean laMk1) {
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hien = laMk1 ? hien1 : hien2;
                int sz = 16, ox = (getWidth() - sz) / 2, oy = (getHeight() - sz) / 2;
                if (hien) {
                    g2.setColor(CLR_TEAL);
                    g2.fillRoundRect(ox, oy, sz, sz, 4, 4);
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(ox + 3, oy + 8, ox + 6, oy + 12);
                    g2.drawLine(ox + 6, oy + 12, ox + 13, oy + 4);
                } else {
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(ox, oy, sz, sz, 4, 4);
                    g2.setColor(CLR_BORDER);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(ox, oy, sz, sz, 4, 4);
                }
                g2.dispose();
            }
        };
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(36, 36));
        btn.setToolTipText("Hi\u1ec7n / \u1ea9n m\u1eadt kh\u1ea9u");
        btn.addActionListener(e -> {
            if (laMk1) {
                hien1 = !hien1;
                txtMatKhauMoi.setEchoChar(hien1 ? (char) 0 : '\u25cf');
            } else {
                hien2 = !hien2;
                txtNhapLai.setEchoChar(hien2 ? (char) 0 : '\u25cf');
            }
            btn.repaint();
        });
        return btn;
    }

    // ── Thanh độ mạnh mật khẩu ───────────────────────────────────────────────

    private JPanel strengthBar;

    private JPanel taoStrengthBar() {
        strengthBar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Nền
                g2.setColor(new Color(0xEEEEEE));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                // Thanh
                int level = tinhDomManh(txtMatKhauMoi != null ? new String(txtMatKhauMoi.getPassword()) : "");
                Color[] colors = {new Color(0xEE4444), new Color(0xFF8800), new Color(0xFFCC00), new Color(0x22AA55)};
                if (level > 0) {
                    g2.setColor(colors[level - 1]);
                    g2.fillRoundRect(0, 0, getWidth() * level / 4, getHeight(), 4, 4);
                }
                g2.dispose();
            }
        };
        strengthBar.setOpaque(false);
        strengthBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        strengthBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        strengthBar.setPreferredSize(new Dimension(0, 6));
        return strengthBar;
    }

    /** Trả về 0–4: 0=rỗng, 1=yếu, 2=trung bình, 3=khá, 4=mạnh */
    private int tinhDomManh(String mk) {
        if (mk.isEmpty()) return 0;
        int score = 0;
        if (mk.length() >= 6)  score++;
        if (mk.length() >= 10) score++;
        if (mk.matches(".*[A-Z].*") && mk.matches(".*[0-9].*")) score++;
        if (mk.matches(".*[^A-Za-z0-9].*")) score++;
        return Math.max(1, score);
    }

    private void capNhatDomManh() {
        if (strengthBar != null) strengthBar.repaint();
    }

    // ── Button helpers ────────────────────────────────────────────────────────

    private JButton taoBtnChinh(String text) {
        JButton btn = new JButton(text) {
            private Color bg = CLR_TEAL;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e)  { bg = CLR_TEAL_HOVER;  repaint(); }
                    @Override public void mouseExited(MouseEvent e)   { bg = CLR_TEAL;        repaint(); }
                    @Override public void mousePressed(MouseEvent e)  { bg = CLR_TEAL_PRESS;  repaint(); }
                    @Override public void mouseReleased(MouseEvent e) { bg = CLR_TEAL_HOVER;  repaint(); }
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
