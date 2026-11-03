package com.quanlynhakhoa.ui.frame;

import com.quanlynhakhoa.dao.TaiKhoanDAO;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;
import com.quanlynhakhoa.util.SessionManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class FrmDangNhap extends JFrame {

    // Màu sắc
    private static final Color CLR_LEFT_BG = new Color(0x1A3C40);
    private static final Color CLR_TEAL = new Color(0x1A7A7A);
    private static final Color CLR_TEAL_HOVER = new Color(0x15686A);
    private static final Color CLR_TEAL_PRESS = new Color(0x115658);
    private static final Color CLR_ERROR_BG = new Color(0xFFF0F0);
    private static final Color CLR_ERROR_BORDER = new Color(0xFFCCCC);
    private static final Color CLR_ERROR_TEXT = new Color(0xCC2222);
    private static final Color CLR_INPUT_BORDER = new Color(0xCCCCCC);
    private static final Color CLR_INPUT_FOCUS = new Color(0x1A7A7A);
    private static final Color CLR_LABEL = new Color(0x333333);
    private static final Color CLR_SUBLABEL = new Color(0x666666);
    private static final Color CLR_RIGHT_BG = Color.WHITE;
    private static final Color CLR_OUTER_BG = new Color(0xEDF3F4);

    // Font
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_SUB = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_INPUT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_BTN = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_LEFT_BIG = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_LEFT_SUB = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_LEFT_INFO = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font FONT_LEFT_TINY = new Font("Segoe UI", Font.PLAIN, 11);

    // Component
    private JTextField txtTenDangNhap;
    private JPasswordField txtMatKhau;
    private JButton btnDangNhap;
    private JButton btnToggleMatKhau;
    private JPanel pnlError;
    private JLabel lblErrorMsg;
    private JLabel lblErrorSub;
    private JLabel lblMatKhauError;
    private JLabel lblQuen;

    private final TaiKhoanDAO taiKhoanDAO = new TaiKhoanDAO();
    private boolean hienMatKhau = false;
    private boolean matKhauLoi = false;

    private final BufferedImage logoProcessed;

    public FrmDangNhap() {
        logoProcessed = loadLogo();
        khoiTaoFrame();
        khoiTaoComponents();
        ganSuKien();
    }

    private BufferedImage loadLogo() {
        java.net.URL url = getClass().getClassLoader().getResource("images/logo.png");
        if (url == null) {
            url = getClass().getClassLoader().getResource("images/logo.jpg");
        }
        if (url == null) {
            return null;
        }

        try {
            BufferedImage src = javax.imageio.ImageIO.read(url); // load đồng bộ
            if (src == null) {
                return null;
            }
            int targetW = 170;
            int targetH = (int) (src.getHeight() * (targetW / (double) src.getWidth()));
            BufferedImage bi = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = bi.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, targetW, targetH);
            g2.drawImage(src, 0, 0, targetW, targetH, null);
            g2.dispose();
            return bi;
        } catch (java.io.IOException e) {
            return null;
        }
    }

    // Khởi tạo frame
    private void khoiTaoFrame() {
        setTitle("B-Clinic — Quản lý phòng khám nha khoa");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(980, 640);
        setMinimumSize(new Dimension(920, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(CLR_OUTER_BG);
    }

    private void khoiTaoComponents() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(CLR_OUTER_BG);
        setContentPane(outer);

        JPanel card = new JPanel(new GridLayout(1, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CLR_RIGHT_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(840, 510));
        card.add(taoLeftPanel());
        card.add(taoRightPanel());

        JLabel lblFooter = new JLabel("© 2026 B-Clinic  •  Hệ thống quản lý phòng khám nha khoa");
        lblFooter.setFont(FONT_SMALL);
        lblFooter.setForeground(CLR_SUBLABEL);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setOpaque(false);
        footer.add(lblFooter);

        JLabel dot = new JLabel("●");
        dot.setForeground(new Color(0x22CC55));
        dot.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        JLabel lblStatus = new JLabel("Máy chủ sẵn sàng");
        lblStatus.setFont(FONT_SMALL);
        lblStatus.setForeground(CLR_SUBLABEL);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setOpaque(false);
        statusBar.setBorder(new EmptyBorder(6, 28, 8, 28));
        JPanel pill = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xE8F5E8));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
            }
        };
        pill.setOpaque(false);
        pill.setBorder(new EmptyBorder(2, 8, 2, 8));
        pill.add(dot);
        pill.add(lblStatus);

        JPanel leftStatus = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftStatus.setOpaque(false);
        leftStatus.add(pill);
        statusBar.add(leftStatus, BorderLayout.WEST);

        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setOpaque(false);
        wrapper.add(card, BorderLayout.CENTER);
        wrapper.add(footer, BorderLayout.SOUTH);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        outer.add(wrapper, gbc);
        gbc.gridy = 1;
        outer.add(statusBar, gbc);
    }

    // Panel trái
    private JPanel taoLeftPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CLR_LEFT_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
                g2.fillRect(getWidth() / 2, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(28, 28, 20, 28));

        // Logo
        JLabel logoHolder;
        if (logoProcessed != null) {
            logoHolder = new JLabel(new ImageIcon(logoProcessed));
        } else {
            logoHolder = new JLabel("B-CLINIC");
            logoHolder.setFont(new Font("Segoe UI", Font.BOLD, 16));
            logoHolder.setForeground(CLR_TEAL);
        }
        JPanel logoCard = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        logoCard.setOpaque(false);
        logoCard.add(logoHolder);

        JPanel pnlLogoWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlLogoWrap.setOpaque(false);
        pnlLogoWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlLogoWrap.add(logoCard);

        // Tiêu đề + subtitle
        JLabel lblBig = new JLabel("<html>Không gian làm việc<br>của phòng khám.</html>");
        lblBig.setFont(FONT_LEFT_BIG);
        lblBig.setForeground(Color.WHITE);
        lblBig.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSubLeft = new JLabel("<html>Quản lý lịch hẹn, tiếp nhận bệnh nhân và thanh<br>toán trên cùng một hệ thống.</html>");
        lblSubLeft.setFont(FONT_LEFT_SUB);
        lblSubLeft.setForeground(new Color(0xAAC8CA));
        lblSubLeft.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Cụm logo + tiêu đề căn giữa dọc trong panel
        JPanel pnlGroup = new JPanel();
        pnlGroup.setOpaque(false);
        pnlGroup.setLayout(new BoxLayout(pnlGroup, BoxLayout.Y_AXIS));
        pnlGroup.add(pnlLogoWrap);
        pnlGroup.add(Box.createVerticalStrut(24));
        pnlGroup.add(lblBig);
        pnlGroup.add(Box.createVerticalStrut(10));
        pnlGroup.add(lblSubLeft);

        JPanel pnlCenter = new JPanel(new GridBagLayout());
        pnlCenter.setOpaque(false);
        GridBagConstraints gbcCenter = new GridBagConstraints();
        gbcCenter.gridx = 0;
        gbcCenter.gridy = 0;
        gbcCenter.weightx = 1.0;
        gbcCenter.weighty = 1.0;
        gbcCenter.anchor = GridBagConstraints.WEST;
        gbcCenter.fill = GridBagConstraints.HORIZONTAL;
        pnlCenter.add(pnlGroup, gbcCenter);

        // Version ở cuối
        JLabel lblVer = new JLabel("B-Clinic · Phiên bản 2.6.0");
        lblVer.setFont(FONT_LEFT_TINY);
        lblVer.setForeground(new Color(0x6A9A9C));

        panel.add(pnlCenter, BorderLayout.CENTER);
        panel.add(lblVer, BorderLayout.SOUTH);
        return panel;
    }

    // Panel phải
    private JPanel taoRightPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(CLR_RIGHT_BG);

        JPanel form = new JPanel();
        form.setBackground(CLR_RIGHT_BG);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(0, 40, 0, 40));

        JLabel lblTitle = new JLabel("Đăng nhập hệ thống");
        lblTitle.setFont(FONT_TITLE);
        lblTitle.setForeground(CLR_LABEL);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("Chào mừng trở lại.");
        lblSub.setFont(FONT_SUB);
        lblSub.setForeground(CLR_SUBLABEL);
        lblSub.setHorizontalAlignment(SwingConstants.CENTER);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel pnlHeader = new JPanel();
        pnlHeader.setOpaque(false);
        pnlHeader.setLayout(new BoxLayout(pnlHeader, BoxLayout.Y_AXIS));
        pnlHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        pnlHeader.add(lblTitle);
        pnlHeader.add(Box.createVerticalStrut(4));
        pnlHeader.add(lblSub);

        pnlError = taoErrorBanner();
        pnlError.setVisible(false);

        JLabel lblTen = taoLabel("Tên đăng nhập *");
        JPanel pnlTen = taoUserNamePanel();

        JLabel lblMk = taoLabel("Mật khẩu *");
        JPanel pnlMk = taoPasswordPanel();

        // Lỗi inline mật khẩu
        lblMatKhauError = new JLabel("Vui lòng kiểm tra lại mật khẩu của bạn.");
        lblMatKhauError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblMatKhauError.setForeground(CLR_ERROR_TEXT);
        lblMatKhauError.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblMatKhauError.setVisible(false);

        // Quên mật khẩu
        JPanel pnlQuen = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pnlQuen.setOpaque(false);
        pnlQuen.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlQuen.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        lblQuen = new JLabel("Quên mật khẩu?");
        lblQuen.setFont(FONT_SMALL);
        lblQuen.setForeground(new Color(0xCC2222));
        lblQuen.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pnlQuen.add(lblQuen);

        btnDangNhap = taoBtnDangNhap();

        form.add(pnlHeader);
        form.add(Box.createVerticalStrut(22));
        form.add(pnlError);
        form.add(Box.createVerticalStrut(6));
        form.add(lblTen);
        form.add(Box.createVerticalStrut(6));
        form.add(pnlTen);
        form.add(Box.createVerticalStrut(16));
        form.add(lblMk);
        form.add(Box.createVerticalStrut(6));
        form.add(pnlMk);
        form.add(Box.createVerticalStrut(3));
        form.add(lblMatKhauError);
        form.add(Box.createVerticalStrut(2));
        form.add(pnlQuen);
        form.add(Box.createVerticalStrut(18));
        form.add(btnDangNhap);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        panel.add(form, gbc);
        return panel;
    }

    // Helper components
    private JLabel taoLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(CLR_LABEL);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JPanel taoUserNamePanel() {
        txtTenDangNhap = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setFont(getFont());
                    g2.setColor(new Color(0xAAAAAA));
                    Insets ins = getInsets();
                    g2.drawString("Nhập tên đăng nhập...", ins.left + 2,
                            getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 1);
                    g2.dispose();
                }
            }

            @Override
            protected void paintBorder(Graphics g) {
            }
        };
        txtTenDangNhap.setFont(FONT_INPUT);
        txtTenDangNhap.setForeground(CLR_LABEL);
        txtTenDangNhap.setBackground(Color.WHITE);
        txtTenDangNhap.setOpaque(false);
        txtTenDangNhap.setBorder(new EmptyBorder(13, 14, 13, 10));
        txtTenDangNhap.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                txtTenDangNhap.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                txtTenDangNhap.repaint();
            }
        });

        JLabel iconUser = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xBBBBBB));
                g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2, cy = getHeight() / 2;
                g2.drawOval(cx - 5, cy - 9, 10, 10);
                g2.drawArc(cx - 8, cy + 2, 16, 10, 0, 180);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(32, 32);
            }
        };

        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean focused = txtTenDangNhap.isFocusOwner();
                g2.setColor(focused ? CLR_INPUT_FOCUS : CLR_INPUT_BORDER);
                g2.setStroke(new BasicStroke(focused ? 2f : 1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        wrapper.setOpaque(false);
        wrapper.add(txtTenDangNhap, BorderLayout.CENTER);
        wrapper.add(iconUser, BorderLayout.EAST);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtTenDangNhap.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                wrapper.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                wrapper.repaint();
            }
        });

        return wrapper;
    }

    private JPanel taoPasswordPanel() {
        txtMatKhau = new JPasswordField() {
            @Override
            protected void paintBorder(Graphics g) {
            }
        };
        txtMatKhau.setFont(FONT_INPUT);
        txtMatKhau.setForeground(CLR_LABEL);
        txtMatKhau.setBackground(Color.WHITE);
        txtMatKhau.setOpaque(false);
        txtMatKhau.setBorder(new EmptyBorder(13, 14, 13, 10));
        txtMatKhau.setEchoChar('●');
        txtMatKhau.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                txtMatKhau.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                txtMatKhau.repaint();
            }
        });

        btnToggleMatKhau = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2, cy = getHeight() / 2;
                g2.setColor(new Color(0xBBBBBB));
                g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawArc(cx - 8, cy - 5, 16, 10, 0, 180);
                g2.drawArc(cx - 8, cy - 5, 16, 10, 180, 180);
                g2.fillOval(cx - 2, cy - 2, 5, 5);
                if (!hienMatKhau) {
                    g2.setColor(new Color(0xBBBBBB));
                    g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(cx - 7, cy + 5, cx + 7, cy - 6);
                }
                g2.dispose();
            }
        };
        btnToggleMatKhau.setBorderPainted(false);
        btnToggleMatKhau.setContentAreaFilled(false);
        btnToggleMatKhau.setFocusPainted(false);
        btnToggleMatKhau.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnToggleMatKhau.setPreferredSize(new Dimension(38, 38));
        btnToggleMatKhau.setToolTipText("Hiện / ẩn mật khẩu");

        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean focused = txtMatKhau.isFocusOwner();
                if (matKhauLoi) {
                    g2.setColor(CLR_ERROR_TEXT);
                    g2.setStroke(new BasicStroke(1.5f));
                } else {
                    g2.setColor(focused ? CLR_INPUT_FOCUS : CLR_INPUT_BORDER);
                    g2.setStroke(new BasicStroke(focused ? 2f : 1.2f));
                }
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        wrapper.setOpaque(false);
        wrapper.add(txtMatKhau, BorderLayout.CENTER);
        wrapper.add(btnToggleMatKhau, BorderLayout.EAST);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtMatKhau.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                wrapper.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                wrapper.repaint();
            }
        });

        return wrapper;
    }

    private JPanel taoErrorBanner() {
        JPanel panel = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
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

        JLabel lblIcon = new JLabel("⊗");
        lblIcon.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblIcon.setForeground(CLR_ERROR_TEXT);

        JPanel txtPanel = new JPanel();
        txtPanel.setOpaque(false);
        txtPanel.setLayout(new BoxLayout(txtPanel, BoxLayout.Y_AXIS));

        lblErrorMsg = new JLabel("Không thể đăng nhập");
        lblErrorMsg.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblErrorMsg.setForeground(CLR_ERROR_TEXT);

        lblErrorSub = new JLabel("Tên đăng nhập hoặc mật khẩu chưa chính xác.");
        lblErrorSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblErrorSub.setForeground(CLR_ERROR_TEXT);

        txtPanel.add(lblErrorMsg);
        txtPanel.add(lblErrorSub);
        panel.add(lblIcon, BorderLayout.WEST);
        panel.add(txtPanel, BorderLayout.CENTER);
        return panel;
    }

    private JButton taoBtnDangNhap() {
        JButton btn = new JButton("→  Đăng nhập") {
            private Color currentBg = CLR_TEAL;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        currentBg = CLR_TEAL_HOVER;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        currentBg = CLR_TEAL;
                        repaint();
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {
                        currentBg = CLR_TEAL_PRESS;
                        repaint();
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        currentBg = CLR_TEAL_HOVER;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(currentBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setFont(getFont());
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }

            @Override
            protected void paintBorder(Graphics g) {
            }
        };
        btn.setFont(FONT_BTN);
        btn.setForeground(Color.WHITE);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        return btn;
    }

    // Sự kiện
    private void ganSuKien() {
        btnDangNhap.addActionListener(this::xuLyDangNhap);
        txtTenDangNhap.addActionListener(this::xuLyDangNhap);
        txtMatKhau.addActionListener(this::xuLyDangNhap);

        btnToggleMatKhau.addActionListener(this::xuLyHienMatKhau);

        lblQuen.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                moQuenMatKhau();
            }
        });

        KeyAdapter hideError = new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                anErrorBanner();
            }
        };
        txtTenDangNhap.addKeyListener(hideError);
        txtMatKhau.addKeyListener(hideError);
    }

    private void xuLyDangNhap(ActionEvent e) {
        thucHienDangNhap();
    }

    private void xuLyHienMatKhau(ActionEvent e) {
        hienMatKhau = !hienMatKhau;
        txtMatKhau.setEchoChar(hienMatKhau ? (char) 0 : '●');
        btnToggleMatKhau.repaint();
    }

    private void moQuenMatKhau() {
        new com.quanlynhakhoa.ui.dialog.DlgQuenMatKhau(this).setVisible(true);
    }

    // Logic đăng nhập
    private void thucHienDangNhap() {
        String ten = txtTenDangNhap.getText().trim();
        String mk = new String(txtMatKhau.getPassword());

        String loi = com.quanlynhakhoa.util.KiemTraXacThuc.loiDangNhap(ten, mk);
        if (loi != null) {
            hienErrorBanner("Không thể đăng nhập", loi);
            if (ten.isEmpty()) {
                txtTenDangNhap.requestFocus();
            } else {
                txtMatKhau.requestFocus();
            }
            return;
        }

        btnDangNhap.setEnabled(false);
        btnDangNhap.setText("Đang xử lý...");

        SwingWorker<TaiKhoan, Void> worker = new SwingWorker<>() {
            @Override
            protected TaiKhoan doInBackground() {
                return taiKhoanDAO.dangNhap(ten, mk);
            }

            @Override
            protected void done() {
                btnDangNhap.setEnabled(true);
                btnDangNhap.setText("→  Đăng nhập");
                try {
                    TaiKhoan tk = get();
                    anLoi();
                    SessionManager.getInstance().login(tk);
                    dangNhapThanhCong(tk);
                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof BusinessException) {
                        // Sai mật khẩu — hiện lỗi inline ô mật khẩu
                        hienLoi("Không thể đăng nhập", cause.getMessage());
                        txtMatKhau.setText("");
                        txtMatKhau.requestFocus();
                    } else if (cause instanceof DataAccessException) {
                        hienErrorBanner("Lỗi hệ thống", cause.getMessage());
                    } else {
                        hienErrorBanner("Lỗi không xác định",
                                "Vui lòng thử lại hoặc liên hệ quản trị viên.");
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

    /**
     * Hiện cả banner lỗi trên và lỗi inline ô mật khẩu
     */
    private void hienLoi(String title, String sub) {
        hienErrorBanner(title, sub);
        matKhauLoi = true;
        lblMatKhauError.setVisible(true);
        revalidate();
        repaint();
    }

    /**
     * Ẩn tất cả lỗi
     */
    private void anLoi() {
        anErrorBanner();
        matKhauLoi = false;
        lblMatKhauError.setVisible(false);
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
            matKhauLoi = false;
            if (lblMatKhauError != null) {
                lblMatKhauError.setVisible(false);
            }
            revalidate();
            repaint();
        }
    }
}
