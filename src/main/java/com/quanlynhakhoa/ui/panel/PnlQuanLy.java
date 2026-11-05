package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.ui.component.*;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;

public class PnlQuanLy extends JPanel {

    private final TaiKhoan taiKhoan;
    private final Runnable dangXuat;
    private final CardLayout cards = new CardLayout();
    private final JPanel pnlCards = new JPanel(cards);
    private final Map<String, JButton> menu = new LinkedHashMap<>();
    private final JLabel lblTrang = label("Tổng quan", bold(18), TEXT);
    private final JTextField txtTimKiem = new JTextField();
    private final PnlTongQuanQuanLy pnlTongQuan;
    private PnlNhanVien pnlNhanVien;
    private PnlLichLamViec pnlLichLamViec;
    private PnlLichCaNhan pnlLichCaNhan;
    private PnlDichVu pnlDichVu;
    private PnlVoucher pnlVoucher;
    private PnlBaoCao pnlBaoCao;
    private final JLabel lblCaHienTai = label("Đang tải ca làm việc...", font(11), MUTED);
    private final JLabel lblKetNoi = label("● Đang tải dữ liệu", font(10), MUTED);
    private final JLabel lblDongBo = label("Đang tải dữ liệu...", font(10), MUTED);
    private final JLabel lblDongHo = label("", font(10), MUTED);
    private final javax.swing.Timer dongHo = new javax.swing.Timer(1000, e -> capNhatDongHo());
    private JButton btnLamMoi, btnThongBao;

    public PnlQuanLy(TaiKhoan taiKhoan, Runnable dangXuat) {
        super(new BorderLayout());
        this.taiKhoan = java.util.Objects.requireNonNull(taiKhoan);
        this.dangXuat = java.util.Objects.requireNonNull(dangXuat);
        setBackground(BACKGROUND);
        pnlTongQuan = new PnlTongQuanQuanLy(tenNgan(), this::moTrang);
        pnlCards.add(pnlTongQuan, "Tổng quan");
        add(taoSidebar(), BorderLayout.WEST);
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(taoHeader(), BorderLayout.NORTH);
        main.add(pnlCards, BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);
        add(taoTrangThai(), BorderLayout.SOUTH);
        pnlTongQuan.langNgheTaiDuLieu(this::capNhatTrangThaiTai);
        capNhatDongHo();
        moTrang("Tổng quan");
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK), "search");
        getActionMap().put("search", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                txtTimKiem.requestFocusInWindow();
            }
        });
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh");
        getActionMap().put("refresh", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                lamMoiTrangHienTai();
            }
        });
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
            JLabel logo = new JLabel(new ImageIcon(original.getImage().getScaledInstance(164, 58, Image.SCALE_SMOOTH)));
            logo.setAlignmentX(LEFT_ALIGNMENT);
            brand.add(logo);
        } else {
            brand.add(label("B-CLINIC", bold(26), TEAL));
        }
        brand.add(Box.createVerticalStrut(9));
        brand.add(label("PHÒNG KHÁM NHA KHOA", font(10), new Color(0x608480)));
        top.add(brand);
        addMenu(top, "Tổng quan", LineIcon.Type.GRID);
        addMenu(top, "Nhân viên", LineIcon.Type.PEOPLE);
        addMenu(top, "Dịch vụ", LineIcon.Type.CLIPBOARD);
        addMenu(top, "Lịch làm việc", LineIcon.Type.CALENDAR);
        addMenu(top, "Lịch của tôi", LineIcon.Type.CALENDAR);
        addMenu(top, "Báo cáo", LineIcon.Type.CHART);
        addMenu(top, "Voucher", LineIcon.Type.VOUCHER);
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
        branch.add(label("Phòng khám B-Clinic", bold(12), TEXT));
        branch.add(Box.createVerticalStrut(7));
        lblCaHienTai.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        lblKetNoi.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        branch.add(lblCaHienTai);
        branch.add(Box.createVerticalStrut(6));
        branch.add(lblKetNoi);
        bottom.add(branch);
        bottom.add(Box.createVerticalStrut(16));
        JButton help = plainButton("Trợ giúp & hướng dẫn", LineIcon.Type.HELP, MUTED);
        help.setFont(font(12));
        help.setAlignmentX(LEFT_ALIGNMENT);
        help.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Ctrl + K: tìm nhanh lịch hẹn trong ngày.\nF5: tải lại dữ liệu tổng quan. Dữ liệu tự làm mới mỗi phút.\nChọn tài khoản ở góc phải để đăng xuất.", "Hướng dẫn", JOptionPane.INFORMATION_MESSAGE));
        bottom.add(help);
        sidebar.add(bottom, BorderLayout.SOUTH);
        return sidebar;
    }

    private void addMenu(JPanel parent, String text, LineIcon.Type icon) {
        JButton button = plainButton(text, icon, MUTED);
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        button.setPreferredSize(new Dimension(184, 44));
        button.setBorder(new EmptyBorder(10, 14, 10, 12));
        button.setIconTextGap(13);
        button.addActionListener(e -> moTrang(text));
        menu.put(text, button);
        parent.add(button);
        parent.add(Box.createVerticalStrut(5));
    }

    private JPanel taoHeader() {
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setBackground(SURFACE);
        header.setPreferredSize(new Dimension(0, 78));
        header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(14, 24, 14, 24)));
        header.add(lblTrang, BorderLayout.WEST);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        controls.setOpaque(false);

        SurfacePanel search = new SurfacePanel(new BorderLayout(10, 0));
        search.setPreferredSize(new Dimension(290, 38));
        search.setBorder(new EmptyBorder(4, 12, 4, 10));
        search.add(new JLabel(new LineIcon(LineIcon.Type.SEARCH, MUTED, 17)), BorderLayout.WEST);
        txtTimKiem.setBorder(BorderFactory.createEmptyBorder());
        txtTimKiem.setOpaque(false);
        txtTimKiem.setFont(font(12));
        txtTimKiem.putClientProperty("JTextField.placeholderText", "Tìm kiếm nhanh...");
        txtTimKiem.setToolTipText("Tìm tên bệnh nhân hoặc bác sĩ trong lịch hẹn hôm nay.");
        search.add(txtTimKiem, BorderLayout.CENTER);
        controls.add(search);
        txtTimKiem.getDocument().addDocumentListener(new DocumentListener() {
            private void search() {
                moTrang("Tổng quan");
                pnlTongQuan.timKiem(txtTimKiem.getText());
            }

            public void insertUpdate(DocumentEvent e) {
                search();
            }

            public void removeUpdate(DocumentEvent e) {
                search();
            }

            public void changedUpdate(DocumentEvent e) {
                search();
            }
        });
        JButton bell = new JButton(new LineIcon(LineIcon.Type.BELL, MUTED, 23)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (pnlTongQuan.coLichDangCho()) {
                    g.setColor(new Color(0xDB414A));
                    g.fillOval(getWidth() - 11, 3, 6, 6);
                }
            }
        };
        btnThongBao = bell;
        stylePlain(bell);
        bell.setPreferredSize(new Dimension(32, 38));
        bell.setToolTipText("Lịch hẹn đang chờ và sắp tới");
        bell.addActionListener(e -> {
            JPopupMenu popup = new JPopupMenu();
            JMenuItem item = new JMenuItem(pnlTongQuan.thongBaoLichTiep());
            item.addActionListener(a -> moTrang("Tổng quan"));
            popup.add(item);
            popup.show(bell, 0, bell.getHeight());
        });
        controls.add(bell);
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(1, 30));
        controls.add(separator);
        JButton profile = taoTaiKhoan();
        controls.add(profile);
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(controls);
        header.add(center, BorderLayout.EAST);
        return header;
    }

    private JButton taoTaiKhoan() {
        JButton profile = new JButton();
        stylePlain(profile);
        profile.setLayout(new BorderLayout(12, 0));
        profile.setPreferredSize(new Dimension(228, 45));
        JLabel avatar = new JLabel(vietTat(), SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(TEAL_LIGHT);
                g.fillOval(1, 1, getWidth() - 2, getHeight() - 2);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        avatar.setFont(bold(13));
        avatar.setForeground(TEAL);
        avatar.setPreferredSize(new Dimension(38, 38));
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(label(tenDayDu(), bold(12), TEXT));
        text.add(Box.createVerticalStrut(3));
        text.add(label("Quản lý", font(10), MUTED));
        profile.add(avatar, BorderLayout.WEST);
        profile.add(text, BorderLayout.CENTER);
        profile.add(new JLabel(new LineIcon(LineIcon.Type.CHEVRON, MUTED, 14)), BorderLayout.EAST);
        profile.setToolTipText("Tài khoản " + taiKhoan.getTenTK());
        profile.addActionListener(e -> {
            JPopupMenu popup = new JPopupMenu();
            JMenuItem user = new JMenuItem(tenDayDu() + " · Quản lý");
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
        footer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER), new EmptyBorder(2, 16, 2, 16)));
        JPanel status = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0));
        status.setOpaque(false);
        status.add(label("B-Clinic   |", font(10), MUTED));
        status.add(lblDongBo);
        btnLamMoi = new JButton("Làm mới (F5)");
        stylePlain(btnLamMoi);
        btnLamMoi.setFont(font(10));
        btnLamMoi.setForeground(TEAL);
        btnLamMoi.setBorder(BorderFactory.createEmptyBorder());
        btnLamMoi.addActionListener(e -> lamMoiTrangHienTai());
        status.add(btnLamMoi);
        footer.add(status, BorderLayout.WEST);
        footer.add(lblDongHo, BorderLayout.EAST);
        return footer;
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

    private void capNhatDongHo() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        lblDongHo.setText(now.format(DateTimeFormatter.ofPattern("HH:mm · EEEE, dd/MM/yyyy", java.util.Locale.forLanguageTag("vi-VN"))));
    }

    private void capNhatTrangThaiTai(PnlTongQuanQuanLy.TrangThaiTai state) {
        btnLamMoi.setEnabled(!state.dangTai());
        if (state.dangTai()) {
            lblDongBo.setText("Đang cập nhật dữ liệu...");
            lblDongBo.setForeground(MUTED);
            return;
        }
        if (state.loi() != null) {
            lblDongBo.setText(state.duLieu() == null ? "Không tải được dữ liệu" : "Cập nhật lỗi · dữ liệu lúc " + state.duLieu().capNhatLuc().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            lblDongBo.setForeground(new Color(0xC94C48));
            lblDongBo.setToolTipText(state.loi());
            lblKetNoi.setText("● Chưa cập nhật được");
            lblKetNoi.setForeground(new Color(0xC94C48));
            return;
        }
        var data = state.duLieu();
        lblDongBo.setText("Đã cập nhật lúc " + data.capNhatLuc().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        lblDongBo.setForeground(MUTED);
        lblDongBo.setToolTipText(null);
        lblKetNoi.setText("● Đã cập nhật dữ liệu");
        lblKetNoi.setForeground(GREEN);
        var ca = data.caHienTai();
        String name = ca == null ? "" : switch (ca.caTruc()) {
            case "CA_SANG" ->
                "Ca sáng";
            case "CA_CHIEU" ->
                "Ca chiều";
            default ->
                "Ca cả ngày";
        };
        lblCaHienTai.setText(ca == null ? "Không có ca hiện tại" : name + " · " + ca.batDau().format(DateTimeFormatter.ofPattern("HH:mm")) + "–" + ca.ketThuc().format(DateTimeFormatter.ofPattern("HH:mm")));
        btnThongBao.repaint();
    }

    private void lamMoiTrangHienTai() {
        if ("Lịch của tôi".equals(lblTrang.getText()) && pnlLichCaNhan != null) {
            pnlLichCaNhan.lamMoiDuLieu();
        } else if ("Nhân viên".equals(lblTrang.getText()) && pnlNhanVien != null) {
            pnlNhanVien.lamMoiDuLieu();
        } else if ("Lịch làm việc".equals(lblTrang.getText()) && pnlLichLamViec != null) {
            pnlLichLamViec.lamMoiDuLieu();
        } else if ("Dịch vụ".equals(lblTrang.getText()) && pnlDichVu != null) {
            pnlDichVu.lamMoiDuLieu();
        } else if ("Báo cáo".equals(lblTrang.getText()) && pnlBaoCao != null) {
            pnlBaoCao.lamMoi();
        } else if ("Voucher".equals(lblTrang.getText()) && pnlVoucher != null) {
            pnlVoucher.lamMoiDuLieu();
        } else {
            pnlTongQuan.lamMoiDuLieu();
        }
    }

    public void moTrang(String name) {
        if (!"Tổng quan".equals(name) && !menu.containsKey(name)) {
            return;
        }
        if ("Lịch của tôi".equals(name)) {
            if(pnlLichCaNhan==null) { pnlLichCaNhan=new PnlLichCaNhan(); pnlCards.add(pnlLichCaNhan,name); }
            pnlLichCaNhan.lamMoiDuLieu();
        }
        if ("Nhân viên".equals(name) && pnlNhanVien == null) {
            pnlNhanVien = new PnlNhanVien();
            pnlCards.add(pnlNhanVien, name);
        }
        if ("Lịch làm việc".equals(name) && pnlLichLamViec == null) {
            pnlLichLamViec = new PnlLichLamViec();
            pnlCards.add(pnlLichLamViec, name);
        }
        if ("Lịch làm việc".equals(name)) {
            pnlLichLamViec.lamMoiDuLieu();
        }
        if ("Dịch vụ".equals(name) && pnlDichVu == null) {
            pnlDichVu = new PnlDichVu();
            pnlCards.add(pnlDichVu, name);
        }
        if ("Dịch vụ".equals(name)) {
            pnlDichVu.lamMoiDuLieu();
        }
        if ("Voucher".equals(name)) {
            if (pnlVoucher == null) { pnlVoucher = new PnlVoucher(); pnlCards.add(pnlVoucher, name); }
            pnlVoucher.lamMoiDuLieu();
        }
        if ("Báo cáo".equals(name)) { // trang Báo cáo hoạt động (doanh thu, dịch vụ, lịch hẹn)
            if (pnlBaoCao == null) {
                pnlBaoCao = new PnlBaoCao();
                pnlCards.add(pnlBaoCao, name);
            }
            pnlBaoCao.lamMoi();
        }
        if (!"Tổng quan".equals(name) && java.util.Arrays.stream(pnlCards.getComponents()).noneMatch(c -> name.equals(c.getName()))) {
            JPanel empty = new JPanel(new GridBagLayout());
            empty.setBackground(BACKGROUND);
            empty.setName(name);
            JPanel content = new JPanel();
            content.setOpaque(false);
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            JLabel heading = label(name, bold(26), TEXT);
            heading.setAlignmentX(CENTER_ALIGNMENT);
            content.add(heading);
            content.add(Box.createVerticalStrut(12));
            JLabel note = label("Màn hình này đang được hoàn thiện.", font(14), MUTED);
            note.setAlignmentX(CENTER_ALIGNMENT);
            content.add(note);
            content.add(Box.createVerticalStrut(24));
            JButton back = PnlTongQuanQuanLy.button("Quay lại tổng quan", LineIcon.Type.ARROW, false);
            back.setAlignmentX(CENTER_ALIGNMENT);
            back.addActionListener(e -> moTrang("Tổng quan"));
            content.add(back);
            empty.add(content);
            pnlCards.add(empty, name);
        }
        cards.show(pnlCards, name);
        lblTrang.setText(name);
        menu.forEach((title, button) -> {
            boolean selected = title.equals(name);
            button.setOpaque(false);
            button.putClientProperty("dashboard.selected", selected);
            button.repaint();
            button.setForeground(selected ? TEAL : TEXT);
            button.setFont(selected ? bold(14) : font(14));
            LineIcon.Type icon = switch (title) {
                case "Tổng quan" ->
                    LineIcon.Type.GRID;
                case "Nhân viên" ->
                    LineIcon.Type.PEOPLE;
                case "Dịch vụ" ->
                    LineIcon.Type.CLIPBOARD;
                case "Lịch làm việc", "Lịch của tôi" ->
                    LineIcon.Type.CALENDAR;
                case "Báo cáo" ->
                    LineIcon.Type.CHART;
                case "Voucher" ->
                    LineIcon.Type.VOUCHER;
                default ->
                    LineIcon.Type.SETTINGS;
            };
            button.setIcon(new LineIcon(icon, selected ? TEAL : MUTED, 17));
        });
    }

    private String tenDayDu() {
        return taiKhoan.getNhanVien() == null ? "Quản lý" : taiKhoan.getNhanVien().getHoTenNV();
    }

    private String tenNgan() {
        String[] words = tenDayDu().trim().split("\\s+");
        return words.length > 1 ? words[words.length - 2] + " " + words[words.length - 1] : words[0];
    }

    private String vietTat() {
        String[] words = tenNgan().split("\\s+");
        StringBuilder s = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                s.append(word.charAt(0));
            }
        }
        return s.toString().toUpperCase(java.util.Locale.ROOT);
    }

    private static JButton plainButton(String text, LineIcon.Type icon, Color color) {
        JButton button = new JButton(text, new LineIcon(icon, color, 17)) {
            @Override
            protected void paintComponent(Graphics graphics) {
                if (Boolean.TRUE.equals(getClientProperty("dashboard.selected"))) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(TEAL_LIGHT);
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 5, 5);
                    g.dispose();
                }
                super.paintComponent(graphics);
            }
        };
        stylePlain(button);
        button.setFont(font(14));
        button.setForeground(TEXT);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        return button;
    }

    private static void stylePlain(JButton button) {
        button.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("JButton.buttonType", "borderless");
    }

    static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }
}
