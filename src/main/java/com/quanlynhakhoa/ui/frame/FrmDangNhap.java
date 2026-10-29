package com.quanlynhakhoa.ui.frame;

import com.quanlynhakhoa.dao.TaiKhoanDAO;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;
import com.quanlynhakhoa.util.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Màn hình đăng nhập B-Clinic.
 * Bố cục: panel trái (tối, logo + thông tin) + panel phải (form đăng nhập).
 * UC001 — Đăng nhập hệ thống.
 */
public class FrmDangNhap extends JFrame {

    // ── Màu sắc ──────────────────────────────────────────────────────────────
    private static final Color CLR_LEFT_BG      = new Color(0x1A3C40);
    private static final Color CLR_LEFT_ACCENT  = new Color(0x2A5C61);
    private static final Color CLR_TEAL         = new Color(0x1A7A7A);
    private static final Color CLR_TEAL_HOVER   = new Color(0x15686A);
    private static final Color CLR_TEAL_PRESS   = new Color(0x115658);
    private static final Color CLR_ERROR_BG     = new Color(0xFFF0F0);
    private static final Color CLR_ERROR_BORDER = new Color(0xFFCCCC);
    private static final Color CLR_ERROR_TEXT   = new Color(0xCC2222);
    private static final Color CLR_INPUT_BORDER = new Color(0xCCCCCC);
    private static final Color CLR_INPUT_FOCUS  = new Color(0x1A7A7A);
    private static final Color CLR_LABEL        = new Color(0x333333);
    private static final Color CLR_SUBLABEL     = new Color(0x666666);
    private static final Color CLR_RIGHT_BG     = Color.WHITE;
    private static final Color CLR_OUTER_BG     = new Color(0xEDF3F4);

    // ── Font ─────────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD,  22);
    private static final Font FONT_SUB    = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_LABEL  = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FONT_INPUT  = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_BTN    = new Font("Segoe UI", Font.BOLD,  14);
    private static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 11);

    // ── Component ─────────────────────────────────────────────────────────────
    private JTextField     txtTenDangNhap;
    private JPasswordField txtMatKhau;
    private JButton        btnDangNhap;
    private JButton        btnToggleMatKhau;
    private JPanel         pnlError;
    private JLabel         lblErrorMsg;
    private JLabel         lblErrorSub;
    private JLabel         lblDateTime;

    private final TaiKhoanDAO taiKhoanDAO = new TaiKhoanDAO();
    // Mặc định ẩn — chỉ hiện khi nhấn toggle (tick)
    private boolean hienMatKhau = false;

    // ── Logo đã xử lý (load 1 lần) ───────────────────────────────────────────
    private final BufferedImage logoProcessed;

    // ─────────────────────────────────────────────────────────────────────────

    public FrmDangNhap() {
        logoProcessed = loadLogo();
        khoiTaoFrame();
        khoiTaoComponents();
        ganSuKien();
        batDauDongHo();
    }

    // ── Load & xử lý logo ────────────────────────────────────────────────────

    /** Load logo.png, scale về width=190, xóa nền trắng → trả BufferedImage ARGB. */
    private BufferedImage loadLogo() {
        java.net.URL url = getClass().getClassLoader().getResource("images/logo.png");
        if (url == null) url = getClass().getClassLoader().getResource("images/logo.jpg");
        if (url == null) return null;

        ImageIcon raw = new ImageIcon(url);
        int targetW = 190;
        int targetH = (int) (raw.getIconHeight() * (targetW / (double) raw.getIconWidth()));
        Image scaled = raw.getImage().getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);

        // Chuyển sang BufferedImage ARGB
        BufferedImage bi = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();

        // Xóa nền trắng/kem: pixel R,G,B đều > 210 → trong suốt
        final int THRESH = 210;
        for (int y = 0; y < targetH; y++) {
            for (int x = 0; x < targetW; x++) {
                int rgba = bi.getRGB(x, y);
                int a = (rgba >> 24) & 0xFF;
                int r = (rgba >> 16) & 0xFF;
                int g = (rgba >>  8) & 0xFF;
                int b =  rgba        & 0xFF;
                if (a < 10 || (r > THRESH && g > THRESH && b > THRESH)) {
                    bi.setRGB(x, y, 0x00000000);
                }
            }
        }
        return bi;
    }

    // ── Khởi tạo frame ───────────────────────────────────────────────────────

    private void khoiTaoFrame() {
        setTitle("B-Clinic \u2014 Qu\u1ea3n l\u00fd ph\u00f2ng kh\u00e1m nha khoa");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(960, 620);
        setMinimumSize(new Dimension(900, 580));
        setLocationRelativeTo(null);
        getContentPane().setBackground(CLR_OUTER_BG);
    }

    // ── Build UI ─────────────────────────────────────────────────────────────

    private void khoiTaoComponents() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(CLR_OUTER_BG);
        setContentPane(outer);

        // Card 2 panel
        JPanel card = new JPanel(new GridLayout(1, 2)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CLR_RIGHT_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(820, 480));
        card.add(taoLeftPanel());
        card.add(taoRightPanel());

        // Footer
        JLabel lblFooter = new JLabel("\u00a9 2026 B-Clinic  \u2022  H\u1ec7 th\u1ed1ng qu\u1ea3n l\u00fd ph\u00f2ng kh\u00e1m nha khoa");
        lblFooter.setFont(FONT_SMALL);
        lblFooter.setForeground(CLR_SUBLABEL);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setOpaque(false);
        footer.add(lblFooter);

        // Status bar
        JLabel dot = new JLabel("\u25cf");
        dot.setForeground(new Color(0x22CC55));
        dot.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        JLabel lblStatus = new JLabel("M\u00e1y ch\u1ee7 s\u1eb5n s\u00e0ng");
        lblStatus.setFont(FONT_SMALL);
        lblStatus.setForeground(CLR_SUBLABEL);

        lblDateTime = new JLabel();
        lblDateTime.setFont(FONT_SMALL);
        lblDateTime.setForeground(CLR_SUBLABEL);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setOpaque(false);
        statusBar.setBorder(new EmptyBorder(6, 28, 8, 28));
        JPanel leftStatus = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        leftStatus.setOpaque(false);
        leftStatus.add(dot);
        leftStatus.add(lblStatus);
        statusBar.add(leftStatus, BorderLayout.WEST);
        statusBar.add(lblDateTime, BorderLayout.EAST);

        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setOpaque(false);
        wrapper.add(card, BorderLayout.CENTER);
        wrapper.add(footer, BorderLayout.SOUTH);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        outer.add(wrapper, gbc);
        gbc.gridy = 1;
        outer.add(statusBar, gbc);
    }

    // ── Panel trái — chỉ hiển thị logo căn giữa trên nền tối ────────────────

    private JPanel taoLeftPanel() {
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CLR_LEFT_BG);
                // Bo góc trái, vuông góc phải (để ghép liền với panel trắng)
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
                g2.fillRect(getWidth() / 2, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        panel.setOpaque(false);

        if (logoProcessed != null) {
            // Scale logo vừa 70% chiều rộng panel trái (panel = 410px → logo ~280px)
            int targetW = 260;
            int srcW    = logoProcessed.getWidth();
            int srcH    = logoProcessed.getHeight();
            int targetH = (int) (srcH * (targetW / (double) srcW));

            // Scale lại từ BufferedImage đã lọc nền
            BufferedImage scaled = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D sg = scaled.createGraphics();
            sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            sg.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
            sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
            sg.drawImage(logoProcessed, 0, 0, targetW, targetH, null);
            sg.dispose();

            JLabel lblLogo = new JLabel(new ImageIcon(scaled));
            lblLogo.setOpaque(false);
            panel.add(lblLogo, new GridBagConstraints());
        } else {
            // Fallback text nếu chưa có file logo
            JLabel lbl = new JLabel("B-CLINIC");
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 28));
            lbl.setForeground(new Color(0x4EB8B8));
            panel.add(lbl, new GridBagConstraints());
        }

        return panel;
    }

    // taoInfoRow đã được xóa (panel trái giờ chỉ hiển thị logo)

    // ── Panel phải ───────────────────────────────────────────────────────────

    private JPanel taoRightPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(CLR_RIGHT_BG);

        JPanel form = new JPanel();
        form.setBackground(CLR_RIGHT_BG);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(0, 40, 0, 40));

        JLabel lblTitle = new JLabel("\u0110\u0103ng nh\u1eadp h\u1ec7 th\u1ed1ng");
        lblTitle.setFont(FONT_TITLE);
        lblTitle.setForeground(CLR_LABEL);

        JLabel lblSub = new JLabel("Ch\u00e0o m\u1eebng tr\u1edf l\u1ea1i. Vui l\u00f2ng nh\u1eadp t\u00e0i kho\u1ea3n nh\u00e2n vi\u00ean.");
        lblSub.setFont(FONT_SUB);
        lblSub.setForeground(CLR_SUBLABEL);

        pnlError = taoErrorBanner();
        pnlError.setVisible(false);

        JLabel lblTen = taoLabel("T\u00ean \u0111\u0103ng nh\u1eadp *");
        txtTenDangNhap = taoTextField("Nh\u1eadp t\u00ean \u0111\u0103ng nh\u1eadp...");

        JLabel lblMk = taoLabel("M\u1eadt kh\u1ea9u *");
        JPanel pnlMk = taoPasswordPanel();

        // Quên mật khẩu — màu đỏ
        JPanel pnlQuen = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pnlQuen.setOpaque(false);
        JLabel lblQuen = new JLabel("Qu\u00ean m\u1eadt kh\u1ea9u?");
        lblQuen.setFont(FONT_SMALL);
        lblQuen.setForeground(new Color(0xCC2222));
        lblQuen.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblQuen.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                new com.quanlynhakhoa.ui.dialog.DlgQuenMatKhau(FrmDangNhap.this).setVisible(true);
            }
        });
        pnlQuen.add(lblQuen);

        btnDangNhap = taoBtnDangNhap();

        form.add(lblTitle);
        form.add(Box.createVerticalStrut(4));
        form.add(lblSub);
        form.add(Box.createVerticalStrut(14));
        form.add(pnlError);
        form.add(Box.createVerticalStrut(6));
        form.add(lblTen);
        form.add(Box.createVerticalStrut(5));
        form.add(txtTenDangNhap);
        form.add(Box.createVerticalStrut(14));
        form.add(lblMk);
        form.add(Box.createVerticalStrut(5));
        form.add(pnlMk);
        form.add(Box.createVerticalStrut(2));
        form.add(pnlQuen);
        form.add(Box.createVerticalStrut(16));
        form.add(btnDangNhap);

        panel.add(form);
        return panel;
    }

    // ── Helper components ────────────────────────────────────────────────────

    private JLabel taoLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(CLR_LABEL);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JTextField taoTextField(String placeholder) {
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
                g2.setColor(isFocusOwner() ? CLR_INPUT_FOCUS : CLR_INPUT_BORDER);
                g2.setStroke(new BasicStroke(isFocusOwner() ? 2f : 1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        tf.setFont(FONT_INPUT);
        tf.setForeground(CLR_LABEL);
        tf.setBackground(Color.WHITE);
        tf.setOpaque(false);
        tf.setBorder(new EmptyBorder(10, 12, 10, 12));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
        tf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { tf.repaint(); }
            @Override public void focusLost(FocusEvent e)   { tf.repaint(); }
        });
        return tf;
    }

    private JPanel taoPasswordPanel() {
        txtMatKhau = new JPasswordField() {
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isFocusOwner() ? CLR_INPUT_FOCUS : CLR_INPUT_BORDER);
                g2.setStroke(new BasicStroke(isFocusOwner() ? 2f : 1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        txtMatKhau.setFont(FONT_INPUT);
        txtMatKhau.setForeground(CLR_LABEL);
        txtMatKhau.setBackground(Color.WHITE);
        txtMatKhau.setOpaque(false);
        txtMatKhau.setBorder(new EmptyBorder(10, 12, 10, 8));
        txtMatKhau.setEchoChar('\u25cf'); // ● ẩn mặc định
        txtMatKhau.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { txtMatKhau.repaint(); }
            @Override public void focusLost(FocusEvent e)   { txtMatKhau.repaint(); }
        });

        // Nút toggle: ô vuông viền xám (ẩn) ↔ ô vuông teal + tick (hiện)
        btnToggleMatKhau = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = 16;
                int ox = (getWidth()  - size) / 2;
                int oy = (getHeight() - size) / 2;
                if (hienMatKhau) {
                    // Nền teal + dấu tick trắng
                    g2.setColor(CLR_TEAL);
                    g2.fillRoundRect(ox, oy, size, size, 4, 4);
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(ox + 3, oy + 8,  ox + 6,  oy + 12);
                    g2.drawLine(ox + 6, oy + 12, ox + 13, oy + 4);
                } else {
                    // Ô vuông trắng viền xám — chưa tick
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(ox, oy, size, size, 4, 4);
                    g2.setColor(CLR_INPUT_BORDER);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(ox, oy, size, size, 4, 4);
                }
                g2.dispose();
            }
        };
        btnToggleMatKhau.setBorderPainted(false);
        btnToggleMatKhau.setContentAreaFilled(false);
        btnToggleMatKhau.setFocusPainted(false);
        btnToggleMatKhau.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnToggleMatKhau.setPreferredSize(new Dimension(36, 36));
        btnToggleMatKhau.setToolTipText("Hi\u1ec7n / \u1ea9n m\u1eadt kh\u1ea9u");

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
        wrapper.add(txtMatKhau,        BorderLayout.CENTER);
        wrapper.add(btnToggleMatKhau,  BorderLayout.EAST);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        return wrapper;
    }

    private JPanel taoErrorBanner() {
        JPanel panel = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CLR_ERROR_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(CLR_ERROR_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 12, 10, 12));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblIcon = new JLabel("\u2297");
        lblIcon.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblIcon.setForeground(CLR_ERROR_TEXT);

        JPanel txtPanel = new JPanel();
        txtPanel.setOpaque(false);
        txtPanel.setLayout(new BoxLayout(txtPanel, BoxLayout.Y_AXIS));

        lblErrorMsg = new JLabel("Kh\u00f4ng th\u1ec3 \u0111\u0103ng nh\u1eadp");
        lblErrorMsg.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblErrorMsg.setForeground(CLR_ERROR_TEXT);

        lblErrorSub = new JLabel("T\u00ean \u0111\u0103ng nh\u1eadp ho\u1eb7c m\u1eadt kh\u1ea9u ch\u01b0a ch\u00ednh x\u00e1c.");
        lblErrorSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblErrorSub.setForeground(CLR_ERROR_TEXT);

        txtPanel.add(lblErrorMsg);
        txtPanel.add(lblErrorSub);
        panel.add(lblIcon,   BorderLayout.WEST);
        panel.add(txtPanel,  BorderLayout.CENTER);
        return panel;
    }

    private JButton taoBtnDangNhap() {
        JButton btn = new JButton("\u2192  \u0110\u0103ng nh\u1eadp") {
            private Color currentBg = CLR_TEAL;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e)  { currentBg = CLR_TEAL_HOVER;  repaint(); }
                    @Override public void mouseExited(MouseEvent e)   { currentBg = CLR_TEAL;        repaint(); }
                    @Override public void mousePressed(MouseEvent e)  { currentBg = CLR_TEAL_PRESS;  repaint(); }
                    @Override public void mouseReleased(MouseEvent e) { currentBg = CLR_TEAL_HOVER;  repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(currentBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setFont(getFont());
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth()  - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {}
        };
        btn.setFont(FONT_BTN);
        btn.setForeground(Color.WHITE);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        return btn;
    }

    // ── Sự kiện ──────────────────────────────────────────────────────────────

    private void ganSuKien() {
        ActionListener actLogin = e -> thucHienDangNhap();
        btnDangNhap.addActionListener(actLogin);
        txtTenDangNhap.addActionListener(actLogin);
        txtMatKhau.addActionListener(actLogin);

        btnToggleMatKhau.addActionListener(e -> {
            hienMatKhau = !hienMatKhau;
            txtMatKhau.setEchoChar(hienMatKhau ? (char) 0 : '\u25cf');
            btnToggleMatKhau.repaint();
        });

        KeyAdapter hideError = new KeyAdapter() {
            @Override public void keyTyped(KeyEvent e) { anErrorBanner(); }
        };
        txtTenDangNhap.addKeyListener(hideError);
        txtMatKhau.addKeyListener(hideError);
    }

    // ── Logic đăng nhập ──────────────────────────────────────────────────────

    private void thucHienDangNhap() {
        String ten = txtTenDangNhap.getText().trim();
        String mk  = new String(txtMatKhau.getPassword());

        if (ten.isEmpty() || mk.isEmpty()) {
            hienErrorBanner("Kh\u00f4ng th\u1ec3 \u0111\u0103ng nh\u1eadp",
                    "Vui l\u00f2ng nh\u1eadp \u0111\u1ea7y \u0111\u1ee7 t\u00ean \u0111\u0103ng nh\u1eadp v\u00e0 m\u1eadt kh\u1ea9u.");
            if (ten.isEmpty()) txtTenDangNhap.requestFocus();
            else               txtMatKhau.requestFocus();
            return;
        }

        btnDangNhap.setEnabled(false);
        btnDangNhap.setText("\u0110ang x\u1eed l\u00fd...");

        SwingWorker<TaiKhoan, Void> worker = new SwingWorker<>() {
            @Override protected TaiKhoan doInBackground() {
                return taiKhoanDAO.dangNhap(ten, mk);
            }
            @Override protected void done() {
                btnDangNhap.setEnabled(true);
                btnDangNhap.setText("\u2192  \u0110\u0103ng nh\u1eadp");
                try {
                    TaiKhoan tk = get();
                    SessionManager.getInstance().login(tk);
                    dangNhapThanhCong(tk);
                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof BusinessException) {
                        hienErrorBanner("Kh\u00f4ng th\u1ec3 \u0111\u0103ng nh\u1eadp", cause.getMessage());
                        txtMatKhau.setText("");
                        txtMatKhau.requestFocus();
                    } else if (cause instanceof DataAccessException) {
                        hienErrorBanner("L\u1ed7i h\u1ec7 th\u1ed1ng", cause.getMessage());
                    } else {
                        hienErrorBanner("L\u1ed7i kh\u00f4ng x\u00e1c \u0111\u1ecbnh",
                                "Vui l\u00f2ng th\u1eed l\u1ea1i ho\u1eb7c li\u00ean h\u1ec7 qu\u1ea3n tr\u1ecb vi\u00ean.");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        worker.execute();
    }

    private void dangNhapThanhCong(TaiKhoan tk) {
        dispose();
        SwingUtilities.invokeLater(() -> new FrmMain(tk).setVisible(true));
    }

    private void hienErrorBanner(String title, String sub) {
        lblErrorMsg.setText(title);
        lblErrorSub.setText("<html>" + sub + "</html>");
        pnlError.setVisible(true);
        pnlError.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        revalidate();
        repaint();
    }

    private void anErrorBanner() {
        if (pnlError.isVisible()) {
            pnlError.setVisible(false);
            revalidate();
            repaint();
        }
    }

    // ── Đồng hồ ──────────────────────────────────────────────────────────────

    private void batDauDongHo() {
        capNhatGio();
        new Timer(1000, e -> capNhatGio()).start();
    }

    private void capNhatGio() {
        String s = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy  \u2022  HH:mm",
                        new java.util.Locale("vi", "VN")));
        if (lblDateTime != null) lblDateTime.setText(s + "  ");
    }
}
