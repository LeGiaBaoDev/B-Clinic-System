package com.quanlynhakhoa.ui.frame;

import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.util.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Màn hình chính B-Clinic.
 * Hiện tại chỉ hiển thị panel xác nhận đăng nhập thành công.
 * Sẽ được mở rộng thêm các module theo lộ trình phát triển.
 */
public class FrmMain extends JFrame {

    private static final Color CLR_BG        = new Color(0xEDF3F4);
    private static final Color CLR_CARD      = Color.WHITE;
    private static final Color CLR_TEAL      = new Color(0x1A7A7A);
    private static final Color CLR_TEAL_DARK = new Color(0x1A3C40);
    private static final Color CLR_SUCCESS   = new Color(0x22AA55);
    private static final Color CLR_LABEL     = new Color(0x333333);
    private static final Color CLR_SUBLABEL  = new Color(0x666666);
    private static final Color CLR_BADGE_BG  = new Color(0xE8F5F5);

    private static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD,  22);
    private static final Font FONT_SUB    = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_INFO   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD   = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FONT_BADGE  = new Font("Segoe UI", Font.BOLD,  12);
    private static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_BTN    = new Font("Segoe UI", Font.BOLD,  13);

    private final TaiKhoan taiKhoan;

    // ─────────────────────────────────────────────────────────────────────────

    public FrmMain(TaiKhoan taiKhoan) {
        this.taiKhoan = taiKhoan;
        khoiTaoFrame();
        khoiTaoUI();
    }

    // ── Frame ────────────────────────────────────────────────────────────────

    private void khoiTaoFrame() {
        setTitle("B-Clinic \u2014 Qu\u1ea3n l\u00fd ph\u00f2ng kh\u00e1m nha khoa");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(780, 540);
        setMinimumSize(new Dimension(700, 480));
        setLocationRelativeTo(null);
        getContentPane().setBackground(CLR_BG);
    }

    // ── UI ───────────────────────────────────────────────────────────────────

    private void khoiTaoUI() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(CLR_BG);
        setContentPane(outer);

        JPanel card = taoCard();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        outer.add(card, gbc);
    }

    private JPanel taoCard() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Đổ bóng nhẹ
                g2.setColor(new Color(0, 0, 0, 18));
                g2.fillRoundRect(4, 6, getWidth() - 4, getHeight() - 4, 18, 18);
                // Nền card
                g2.setColor(CLR_CARD);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 18, 18);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(0, 0));
        card.setPreferredSize(new Dimension(560, 420));
        card.setBorder(new EmptyBorder(0, 0, 4, 4)); // offset cho bóng

        card.add(taoHeader(), BorderLayout.NORTH);
        card.add(taoBody(),   BorderLayout.CENTER);
        card.add(taoFooter(), BorderLayout.SOUTH);
        return card;
    }

    // ── Header — dải màu teal ─────────────────────────────────────────────────

    private JPanel taoHeader() {
        JPanel header = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Dải teal bo góc trên
                g2.setColor(CLR_TEAL_DARK);
                g2.fillRoundRect(0, 0, getWidth(), getHeight() + 20, 18, 18);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(0, 90));
        header.setLayout(new BorderLayout());
        header.setBorder(new EmptyBorder(20, 32, 20, 32));

        // Icon check thành công
        JLabel lblIcon = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Vòng tròn xanh lá
                g2.setColor(CLR_SUCCESS);
                g2.fillOval(0, 0, 40, 40);
                // Dấu tick trắng
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(9, 20, 16, 28);
                g2.drawLine(16, 28, 31, 12);
                g2.dispose();
            }
        };
        lblIcon.setPreferredSize(new Dimension(40, 40));

        JPanel txtPanel = new JPanel();
        txtPanel.setOpaque(false);
        txtPanel.setLayout(new BoxLayout(txtPanel, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel("\u0110\u0103ng nh\u1eadp th\u00e0nh c\u00f4ng!");
        lblTitle.setFont(FONT_TITLE);
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("Ch\u00e0o m\u1eebng b\u1ea1n \u0111\u00e3 quay tr\u1edf l\u1ea1i h\u1ec7 th\u1ed1ng B-Clinic.");
        lblSub.setFont(FONT_SUB);
        lblSub.setForeground(new Color(0xAAD4D4));

        txtPanel.add(lblTitle);
        txtPanel.add(Box.createVerticalStrut(4));
        txtPanel.add(lblSub);

        header.add(lblIcon,   BorderLayout.WEST);
        header.add(Box.createHorizontalStrut(16), BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(txtPanel, BorderLayout.WEST);
        header.add(right, BorderLayout.CENTER);

        return header;
    }

    // ── Body — thông tin tài khoản ────────────────────────────────────────────

    private JPanel taoBody() {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(28, 40, 16, 40));

        // Badge vai trò
        String tenVaiTro = tenVaiTro(taiKhoan.getVaiTro());
        Color  mauVaiTro = mauVaiTro(taiKhoan.getVaiTro());

        JLabel badge = new JLabel("  " + tenVaiTro + "  ") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(mauVaiTro.brighter().brighter());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(mauVaiTro.darker());
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(FONT_BADGE);
        badge.setForeground(mauVaiTro.darker().darker());
        badge.setOpaque(false);
        badge.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Thông tin
        String thoiGian = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("HH:mm, EEEE dd/MM/yyyy",
                        new java.util.Locale("vi", "VN")));

        JPanel pnlInfo = new JPanel(new GridBagLayout());
        pnlInfo.setOpaque(false);
        pnlInfo.setAlignmentX(Component.LEFT_ALIGNMENT);

        String tenNV = taiKhoan.getNhanVien() != null
                ? taiKhoan.getNhanVien().getHoTenNV() : "(kh\u00f4ng r\u00f5)";

        themInfoRow(pnlInfo, 0, "\ud83d\udc64  H\u1ecd t\u00ean",      tenNV);
        themInfoRow(pnlInfo, 1, "\ud83d\udd11  T\u00ean \u0111\u0103ng nh\u1eadp", taiKhoan.getTenTK());
        themInfoRow(pnlInfo, 2, "\u23f0  \u0110\u0103ng nh\u1eadp l\u00fac",  thoiGian);

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0xEEEEEE));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);

        body.add(badge);
        body.add(Box.createVerticalStrut(20));
        body.add(pnlInfo);
        body.add(Box.createVerticalStrut(20));
        body.add(sep);
        return body;
    }

    private void themInfoRow(JPanel parent, int row, String label, String value) {
        GridBagConstraints gbcL = new GridBagConstraints();
        gbcL.gridx = 0; gbcL.gridy = row;
        gbcL.anchor = GridBagConstraints.WEST;
        gbcL.insets = new Insets(5, 0, 5, 24);

        GridBagConstraints gbcV = new GridBagConstraints();
        gbcV.gridx = 1; gbcV.gridy = row;
        gbcV.anchor = GridBagConstraints.WEST;
        gbcV.weightx = 1.0;
        gbcV.insets = new Insets(5, 0, 5, 0);

        JLabel lblKey = new JLabel(label);
        lblKey.setFont(FONT_INFO);
        lblKey.setForeground(CLR_SUBLABEL);

        JLabel lblVal = new JLabel(value);
        lblVal.setFont(FONT_BOLD);
        lblVal.setForeground(CLR_LABEL);

        parent.add(lblKey, gbcL);
        parent.add(lblVal, gbcV);
    }

    // ── Footer — nút đăng xuất ────────────────────────────────────────────────

    private JPanel taoFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 32, 12));
        footer.setOpaque(false);

        JLabel lblNote = new JLabel("\ud83d\udd12  M\u1ecdi thao t\u00e1c \u0111\u01b0\u1ee3c ghi nh\u1eadt k\u00fd.");
        lblNote.setFont(FONT_SMALL);
        lblNote.setForeground(CLR_SUBLABEL);

        JButton btnDangXuat = taoBtnDangXuat();

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 32, 12));
        left.setOpaque(false);
        left.add(lblNote);

        JPanel footerWrap = new JPanel(new BorderLayout());
        footerWrap.setOpaque(false);
        footerWrap.add(left, BorderLayout.WEST);
        footerWrap.add(footer, BorderLayout.EAST);
        footerWrap.add(btnDangXuat, BorderLayout.EAST);
        footerWrap.setBorder(new EmptyBorder(0, 32, 20, 32));
        return footerWrap;
    }

    private JButton taoBtnDangXuat() {
        JButton btn = new JButton("\u21aa  \u0110\u0103ng xu\u1ea5t") {
            private Color bg = new Color(0xF0F0F0);
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseEntered(java.awt.event.MouseEvent e)  { bg = new Color(0xFFEEEE); repaint(); }
                    @Override public void mouseExited(java.awt.event.MouseEvent e)   { bg = new Color(0xF0F0F0); repaint(); }
                    @Override public void mousePressed(java.awt.event.MouseEvent e)  { bg = new Color(0xFFCCCC); repaint(); }
                    @Override public void mouseReleased(java.awt.event.MouseEvent e) { bg = new Color(0xFFEEEE); repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setFont(getFont());
                g2.setColor(new Color(0xCC2222));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xFFCCCC));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        btn.setFont(FONT_BTN);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(130, 38));

        btn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "B\u1ea1n c\u00f3 ch\u1eafc ch\u1eafn mu\u1ed1n \u0111\u0103ng xu\u1ea5t?",
                    "\u0110\u0103ng xu\u1ea5t",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                SessionManager.getInstance().logout();
                dispose();
                SwingUtilities.invokeLater(() -> new FrmDangNhap().setVisible(true));
            }
        });
        return btn;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String tenVaiTro(VaiTro vaiTro) {
        return switch (vaiTro) {
            case QUANLY -> "Qu\u1ea3n l\u00fd";
            case NHASI  -> "Nha s\u0129";
            case LETAN  -> "L\u1ec5 t\u00e2n";
        };
    }

    private Color mauVaiTro(VaiTro vaiTro) {
        return switch (vaiTro) {
            case QUANLY -> new Color(0x1A7A7A); // teal
            case NHASI  -> new Color(0x2266CC); // xanh dương
            case LETAN  -> new Color(0x8833AA); // tím
        };
    }
}
