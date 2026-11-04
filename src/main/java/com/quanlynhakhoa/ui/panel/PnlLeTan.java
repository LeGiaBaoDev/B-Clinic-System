package com.quanlynhakhoa.ui.panel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import static com.quanlynhakhoa.ui.component.UiConstants.BACKGROUND;
import static com.quanlynhakhoa.ui.component.UiConstants.BORDER;
import static com.quanlynhakhoa.ui.component.UiConstants.GREEN;
import static com.quanlynhakhoa.ui.component.UiConstants.MUTED;
import static com.quanlynhakhoa.ui.component.UiConstants.SURFACE;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL_LIGHT;
import static com.quanlynhakhoa.ui.component.UiConstants.TEXT;
import static com.quanlynhakhoa.ui.component.UiConstants.bold;
import static com.quanlynhakhoa.ui.component.UiConstants.font;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class PnlLeTan extends JPanel {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DONG_HO
            = DateTimeFormatter.ofPattern("HH:mm · EEEE, dd/MM/yyyy", Locale.of("vi", "VN"));

    private final TaiKhoan taiKhoan;
    private final Runnable dangXuat;
    private final PnlKhachHang pnlKhachHang;
    private final PnlTiepNhan pnlTiepNhan;
    private PnlLichCaNhan pnlLichCaNhan;
    private final CardLayout cards = new CardLayout();
    private final JPanel pnlCards = new JPanel(cards);
    private final Map<String, JButton> menu = new LinkedHashMap<>();
    private final JLabel lblTrang = new JLabel("Bệnh nhân");
    private final JTextField txtTimKiemNhanh = new JTextField();
    private final JLabel lblDongHo = new JLabel();
    private final javax.swing.Timer dongHo = new javax.swing.Timer(1000, e -> capNhatDongHo());

    public PnlLeTan(TaiKhoan taiKhoan, Runnable dangXuat) {
        super(new BorderLayout());
        this.taiKhoan = java.util.Objects.requireNonNull(taiKhoan);
        this.dangXuat = java.util.Objects.requireNonNull(dangXuat);
        if (taiKhoan.getVaiTro() != VaiTro.LETAN) {
            throw new IllegalArgumentException("Màn hình này chỉ dành cho lễ tân.");
        }
        setBackground(BACKGROUND);
        pnlKhachHang = new PnlKhachHang(VaiTro.LETAN);
        pnlTiepNhan = new PnlTiepNhan();
        pnlCards.setOpaque(false);
        pnlCards.add(pnlKhachHang, "Bệnh nhân");
        pnlCards.add(pnlTiepNhan, "Tiếp nhận");
        add(taoSidebar(), BorderLayout.WEST);
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BACKGROUND);
        main.add(taoHeader(), BorderLayout.NORTH);
        main.add(pnlCards, BorderLayout.CENTER);
        main.add(taoTrangThai(), BorderLayout.SOUTH);
        add(main, BorderLayout.CENTER);
        capNhatDongHo();
        dongHo.start();
        moTrang("Bệnh nhân");
    }

    private JPanel taoSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(SURFACE);
        sidebar.setPreferredSize(new Dimension(208, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(new EmptyBorder(24, 12, 0, 12));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(new EmptyBorder(2, 9, 21, 9));
        brand.setAlignmentX(LEFT_ALIGNMENT);
        var resource = getClass().getResource("/images/logo.png");
        if (resource != null) {
            ImageIcon original = new ImageIcon(resource);
            JLabel logo = new JLabel(new ImageIcon(original.getImage()
                    .getScaledInstance(164, 58, java.awt.Image.SCALE_SMOOTH)));
            logo.setAlignmentX(LEFT_ALIGNMENT);
            brand.add(logo);
        } else {
            brand.add(menuLabel("B-CLINIC", bold(26), TEAL));
        }
        brand.add(Box.createVerticalStrut(9));
        brand.add(menuLabel("PHÒNG KHÁM NHA KHOA", font(10), new Color(0x608480)));
        top.add(brand);

        top.add(menuItem("Tổng quan", LineIcon.Type.GRID, () -> thongBaoChuaCo("Tổng quan lễ tân")));
        top.add(Box.createVerticalStrut(5));
        top.add(menuItem("Lịch hẹn", LineIcon.Type.CALENDAR, () -> thongBaoChuaCo("Lịch hẹn (L2)")));
        top.add(Box.createVerticalStrut(5));
        top.add(menuItem("Lịch của tôi", LineIcon.Type.CALENDAR, () -> moTrang("Lịch của tôi")));
        top.add(Box.createVerticalStrut(5));
        top.add(menuItem("Bệnh nhân", LineIcon.Type.PEOPLE, () -> moTrang("Bệnh nhân")));
        top.add(Box.createVerticalStrut(5));
        top.add(menuItem("Tiếp nhận", LineIcon.Type.CLIPBOARD, () -> moTrang("Tiếp nhận")));
        top.add(Box.createVerticalStrut(5));
        top.add(menuItem("Hóa đơn", LineIcon.Type.INVOICE, () -> thongBaoChuaCo("Hóa đơn (UC025/UC026)")));
        top.add(Box.createVerticalStrut(5));
        top.add(menuItem("Báo cáo", LineIcon.Type.CHART, () -> thongBaoChuaCo("Báo cáo")));
        top.add(Box.createVerticalStrut(5));
        top.add(menuItem("Cài đặt", LineIcon.Type.SETTINGS, () -> thongBaoChuaCo("Cài đặt tài khoản")));
        JScrollPane scrollMenu=new JScrollPane(top);
        scrollMenu.setBorder(BorderFactory.createEmptyBorder());
        scrollMenu.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollMenu.getViewport().setBackground(SURFACE);
        scrollMenu.getVerticalScrollBar().setUnitIncrement(16);
        sidebar.add(scrollMenu,BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBorder(new EmptyBorder(12, 12, 10, 12));
        JPanel branch = new JPanel();
        branch.setBackground(TEAL_LIGHT);
        branch.setLayout(new BoxLayout(branch, BoxLayout.Y_AXIS));
        branch.setBorder(new EmptyBorder(13, 13, 13, 10));
        branch.setAlignmentX(LEFT_ALIGNMENT);
        branch.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));
        branch.add(menuLabel("Chi nhánh Quận 3", bold(12), TEXT));
        branch.add(Box.createVerticalStrut(7));
        branch.add(menuLabel("Ca sáng · 08:00–12:00", font(11), new Color(0x3C6E6A)));
        branch.add(Box.createVerticalStrut(6));
        branch.add(menuLabel("● Đã kết nối máy chủ", font(10), GREEN));
        bottom.add(branch);
        bottom.add(Box.createVerticalStrut(16));
        JButton help = new JButton("Trợ giúp & hướng dẫn");
        help.setFont(font(11));
        help.setForeground(MUTED);
        help.setContentAreaFilled(false);
        help.setBorderPainted(false);
        help.setFocusPainted(false);
        help.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        help.setAlignmentX(LEFT_ALIGNMENT);
        help.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Tra cứu, thêm và cập nhật khách hàng tại màn hình Khách hàng.\nF5: làm mới danh sách.",
                "Hướng dẫn", JOptionPane.INFORMATION_MESSAGE));
        bottom.add(help);
        sidebar.add(bottom, BorderLayout.SOUTH);
        return sidebar;
    }

    private JButton menuItem(String text, LineIcon.Type icon, Runnable action) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                if (Boolean.TRUE.equals(getClientProperty("menu.selected"))) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(TEAL_LIGHT);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 5, 5);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        button.setFont(font(14));
        button.setForeground(TEXT);
        button.setIcon(new LineIcon(icon, MUTED, 17));
        button.setIconTextGap(13);
        button.setBorder(new EmptyBorder(10, 14, 10, 12));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        button.setPreferredSize(new Dimension(184, 44));
        menu.put(text, button);
        if (action != null) {
            button.addActionListener(e -> action.run());
        }
        return button;
    }

    private void moTrang(String trang) {
        if("Lịch của tôi".equals(trang)) {
            if(pnlLichCaNhan==null) { pnlLichCaNhan=new PnlLichCaNhan(); pnlCards.add(pnlLichCaNhan,trang); }
            pnlLichCaNhan.lamMoiDuLieu();
        }
        cards.show(pnlCards, trang);
        lblTrang.setText(trang);
        txtTimKiemNhanh.setEnabled("Bệnh nhân".equals(trang));
        if ("Tiếp nhận".equals(trang)) {
            txtTimKiemNhanh.setText("");
            pnlTiepNhan.lamMoi();
        }
        menu.forEach((ten, nut) -> {
            boolean chon = ten.equals(trang);
            nut.putClientProperty("menu.selected", chon);
            nut.setFont(chon ? bold(14) : font(14));
            nut.setForeground(chon ? TEAL : TEXT);
            nut.setIcon(new LineIcon(iconCua(ten), chon ? TEAL : MUTED, 17));
            nut.repaint();
        });
    }

    private static LineIcon.Type iconCua(String ten) {
        return switch (ten) {
            case "Tổng quan" ->
                LineIcon.Type.GRID;
            case "Lịch hẹn", "Lịch của tôi" ->
                LineIcon.Type.CALENDAR;
            case "Bệnh nhân" ->
                LineIcon.Type.PEOPLE;
            case "Tiếp nhận" ->
                LineIcon.Type.CLIPBOARD;
            case "Hóa đơn" ->
                LineIcon.Type.INVOICE;
            case "Báo cáo" ->
                LineIcon.Type.CHART;
            default ->
                LineIcon.Type.SETTINGS;
        };
    }

    private JPanel taoHeader() {
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setBackground(SURFACE);
        header.setPreferredSize(new Dimension(0, 78));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(14, 24, 14, 24)));
        lblTrang.setFont(bold(18));
        lblTrang.setForeground(TEXT);
        header.add(lblTrang, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        right.setOpaque(false);
        SurfacePanel search = new SurfacePanel(new BorderLayout(10, 0));
        search.setPreferredSize(new Dimension(290, 38));
        search.setBorder(new EmptyBorder(4, 12, 4, 10));
        search.add(new JLabel(new LineIcon(LineIcon.Type.SEARCH, MUTED, 17)), BorderLayout.WEST);
        txtTimKiemNhanh.setBorder(BorderFactory.createEmptyBorder());
        txtTimKiemNhanh.setOpaque(false);
        txtTimKiemNhanh.setFont(font(12));
        txtTimKiemNhanh.putClientProperty("JTextField.placeholderText", "Tìm kiếm nhanh...");
        txtTimKiemNhanh.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                pnlKhachHang.timKiemNhanh(txtTimKiemNhanh.getText());
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                pnlKhachHang.timKiemNhanh(txtTimKiemNhanh.getText());
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                pnlKhachHang.timKiemNhanh(txtTimKiemNhanh.getText());
            }
        });
        search.add(txtTimKiemNhanh, BorderLayout.CENTER);
        right.add(search);
        JButton bell = new JButton(new LineIcon(LineIcon.Type.BELL, MUTED, 23)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(0xD93A3A));
                g.fillOval(getWidth() - 10, 4, 6, 6);
            }
        };
        bell.setContentAreaFilled(false);
        bell.setBorderPainted(false);
        bell.setFocusPainted(false);
        bell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bell.setPreferredSize(new Dimension(32, 38));
        bell.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Không có thông báo mới.", "Thông báo", JOptionPane.INFORMATION_MESSAGE));
        right.add(bell);
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 30));
        right.add(sep);
        right.add(taoHoSo());
        header.add(right, BorderLayout.EAST);

        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_K,
                        java.awt.event.InputEvent.CTRL_DOWN_MASK), "timKiemNhanh");
        getActionMap().put("timKiemNhanh", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                txtTimKiemNhanh.requestFocusInWindow();
            }
        });
        return header;
    }

    private JButton taoHoSo() {
        JButton profile = new JButton();
        profile.setLayout(new BorderLayout(12, 0));
        profile.setContentAreaFilled(false);
        profile.setBorderPainted(false);
        profile.setFocusPainted(false);
        profile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        profile.setPreferredSize(new Dimension(228, 45));

        JLabel avatar = new JLabel(vietTat(), SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TEAL_LIGHT);
                g2.fillOval(1, 1, getWidth() - 2, getHeight() - 2);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatar.setFont(bold(13));
        avatar.setForeground(TEAL);
        avatar.setPreferredSize(new Dimension(38, 38));

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.add(menuLabel(tenDayDu(), bold(12), TEXT));
        info.add(Box.createVerticalStrut(3));
        info.add(menuLabel("Lễ tân", font(10), MUTED));

        profile.add(avatar, BorderLayout.WEST);
        profile.add(info, BorderLayout.CENTER);
        profile.add(new JLabel(new LineIcon(LineIcon.Type.CHEVRON, MUTED, 14)), BorderLayout.EAST);
        profile.addActionListener(e -> {
            JPopupMenu popup = new JPopupMenu();
            JMenuItem user = new JMenuItem(tenDayDu() + " · Lễ tân");
            user.setEnabled(false);
            popup.add(user);
            popup.addSeparator();
            JMenuItem logout = new JMenuItem("Đăng xuất", new LineIcon(LineIcon.Type.LOGOUT, TEXT, 16));
            logout.addActionListener(a -> dangXuat.run());
            popup.add(logout);
            popup.show(profile, 0, profile.getHeight());
        });
        return profile;
    }

    private JPanel taoTrangThai() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(SURFACE);
        footer.setPreferredSize(new Dimension(0, 24));
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER), new EmptyBorder(2, 16, 2, 16)));
        footer.add(menuLabel("B-Clinic 1.0.0  |  Máy trạm  |  Dữ liệu đã đồng bộ", font(10), MUTED),
                BorderLayout.WEST);
        lblDongHo.setFont(font(10));
        lblDongHo.setForeground(MUTED);
        footer.add(lblDongHo, BorderLayout.EAST);
        return footer;
    }

    private void capNhatDongHo() {
        lblDongHo.setText(LocalDateTime.now(ZONE).format(DONG_HO));
    }

    private void thongBaoChuaCo(String tinhNang) {
        JOptionPane.showMessageDialog(this, tinhNang + " sẽ được bổ sung ở giai đoạn tiếp theo.",
                "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    private String tenDayDu() {
        return taiKhoan.getNhanVien() == null ? "Lễ tân" : taiKhoan.getNhanVien().getHoTenNV();
    }

    private String vietTat() {
        String[] phan = tenDayDu().trim().split("\\s+");
        if (phan.length == 1) {
            return phan[0].substring(0, Math.min(2, phan[0].length())).toUpperCase(Locale.ROOT);
        }
        return (phan[0].charAt(0) + "" + phan[phan.length - 1].charAt(0)).toUpperCase(Locale.ROOT);
    }

    private static JLabel menuLabel(String text, java.awt.Font font, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font);
        lbl.setForeground(color);
        return lbl;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        dongHo.start();
    }

    @Override
    public void removeNotify() {
        dongHo.stop();
        super.removeNotify();
    }
}
