package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.dao.LichHenDAO;
import com.quanlynhakhoa.dao.LichLamViecDAO;
import com.quanlynhakhoa.entity.LichHen;
import com.quanlynhakhoa.entity.LichLamViec;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.CaTruc;
import com.quanlynhakhoa.entity.enums.TrangThaiLichHen;
import com.quanlynhakhoa.entity.enums.TrangThaiLichLamViec;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.PopupThongBao;
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
import com.quanlynhakhoa.ui.dialog.DlgChiTietLichHen;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

public class PnlLichNhaSi extends JPanel {

    record ThongKe(int homNay, int hoanTat, int choKham, int taiKham) {

    }

    private record DuLieu(List<LichHen> lichHen, List<LichLamViec> caTruc) {

    }

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter NGAY_DAY_DU = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale.of("vi", "VN"));
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DONG_HO = DateTimeFormatter.ofPattern("HH:mm · EEEE, dd/MM/yyyy", Locale.of("vi", "VN"));
    private static final Color ROW_SELECTED = new Color(0xF3F7FA);
    private static final Color RED = new Color(0xC0392B);
    private static final Color RED_LIGHT = new Color(0xFDECEA);
    private static final Color AMBER = new Color(0xB26B00);
    private static final Color AMBER_LIGHT = new Color(0xFFF4DE);
    private static final String THE_BANG = "bang";
    private static final String THE_TRONG = "trong";
    private static final String[] COT = {"THỜI GIAN", "BỆNH NHÂN", "DỊCH VỤ", "TRẠNG THÁI", "THAO TÁC"};
    private static final String[] NHAN_TRANG_THAI = {
        "Tất cả trạng thái", "Đã đặt", "Đã đến", "Hoàn tất", "Đã hủy", "Không đến"
    };
    private static final TrangThaiLichHen[] TRANG_THAI = {
        null, TrangThaiLichHen.DA_DAT, TrangThaiLichHen.DA_DEN,
        TrangThaiLichHen.HOAN_TAT, TrangThaiLichHen.DA_HUY, TrangThaiLichHen.KHONG_DEN
    };

    private final TaiKhoan taiKhoan;
    private final Runnable dangXuat;
    private final String maNV;
    private final LichHenDAO lichHenDAO = new LichHenDAO();
    private final LichLamViecDAO lichLamViecDAO = new LichLamViecDAO();

    private LocalDate homNay = LocalDate.now(ZONE);
    private LocalDate ngayChon = homNay;
    private LocalDate dauTuan = ngayChon.with(DayOfWeek.MONDAY);
    private List<LichHen> duLieuTuan = new ArrayList<>();
    private List<LichLamViec> caTuan = new ArrayList<>();
    private List<LichHen> dsNgay = new ArrayList<>();
    private LichHen dangChon;

    private final JTextField txtTimKiem = new JTextField();
    private final JComboBox<String> cboTrangThai = new JComboBox<>(NHAN_TRANG_THAI);
    private final JButton btnHomNay = PnlTongQuanQuanLy.button("Hôm nay", LineIcon.Type.CALENDAR, true);
    private final JPanel pnlTuan = new JPanel(new GridLayout(1, 7, 8, 0));
    private final JPanel pnlCaTruc = new JPanel();
    private final JPanel pnlTongHop = new JPanel();
    private final JLabel lblTieuDeNgay = new JLabel("Lịch hẹn");
    private final JLabel lblKhungTrong = new JLabel(" ");
    private final JLabel lblSoLuong = new JLabel(" ");
    private final JLabel lblKhongCoKetQua = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel lblCaSidebar = new JLabel("Đang tải ca làm việc...");
    private final JLabel lblDongHo = new JLabel();
    private final JPanel pnlChiTiet = new JPanel(new BorderLayout(12, 0));
    private final JLabel lblChiTietTen = new JLabel(" ");
    private final JLabel lblChiTietMeta = new JLabel(" ");
    private final JLabel lblChiTietTrangThai = new JLabel(" ");
    private final JButton btnXemBenhAn = new JButton("Xem bệnh án");
    private final CardLayout theTrang = new CardLayout();
    private final JPanel pnlTrang = new JPanel(theTrang);
    private final Map<String, JButton> menu = new LinkedHashMap<>();
    private final JLabel lblTrang = new JLabel("Lịch trực");
    private PnlKham pnlKham;
    private PnlTraCuuHoSo pnlTraCuu;
    private PnlLichCaNhan pnlLichCaNhan;
    private PnlThongKeBenhNhan pnlThongKe;
    private PnlTongQuanNhaSi pnlTongQuan;
    private final CardLayout theNoiDung = new CardLayout();
    private final JPanel pnlNoiDung = new JPanel(theNoiDung);
    private final DefaultTableModel modelLich = new DefaultTableModel(COT, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tblLich = new JTable(modelLich);

    private SwingWorker<DuLieu, Void> worker;
    private JButton btnThongBao;
    private final javax.swing.Timer dongHo = new javax.swing.Timer(1000, e -> capNhatDongHo());

    public PnlLichNhaSi(TaiKhoan taiKhoan, Runnable dangXuat) {
        super(new BorderLayout());
        this.taiKhoan = java.util.Objects.requireNonNull(taiKhoan);
        this.dangXuat = java.util.Objects.requireNonNull(dangXuat);
        if (taiKhoan.getVaiTro() != VaiTro.NHASI) {
            throw new IllegalArgumentException("Màn hình này chỉ dành cho nha sĩ.");
        }
        if (taiKhoan.getNhanVien() == null) {
            throw new IllegalArgumentException("Tài khoản nha sĩ chưa gắn nhân viên.");
        }
        this.maNV = taiKhoan.getNhanVien().getMaNV();
        setBackground(BACKGROUND);
        initComponents();
        addEvents();
        capNhatDongHo();
        dongHo.start();
        taiDuLieu();
    }

    private void initComponents() {
        pnlKham = new PnlKham(taiKhoan, dangXuat);
        pnlTraCuu = new PnlTraCuuHoSo(taiKhoan, this::moKhamTuTraCuu);
        pnlThongKe = new PnlThongKeBenhNhan();
        pnlTongQuan = new PnlTongQuanNhaSi(taiKhoan, this::moKhamTuTraCuu,
                () -> moTrang("Lịch trực"), () -> moTrang("Bệnh án"), this::moHoSoBenhAn);
        pnlTrang.setOpaque(false);
        pnlTrang.add(pnlTongQuan, "Tổng quan");
        pnlTrang.add(taoNoiDung(), "Lịch trực");
        pnlTrang.add(pnlTraCuu, "Bệnh án");
        pnlTrang.add(pnlKham, "Khám");
        pnlTrang.add(pnlThongKe, "Thống kê");
        add(taoSidebar(), BorderLayout.WEST);
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BACKGROUND);
        main.add(taoHeader(), BorderLayout.NORTH);
        main.add(pnlTrang, BorderLayout.CENTER);
        main.add(taoThanhTrangThai(), BorderLayout.SOUTH);
        add(main, BorderLayout.CENTER);
        moTrang("Tổng quan");
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
            JLabel logo = new JLabel(new ImageIcon(original.getImage().getScaledInstance(164, 58, java.awt.Image.SCALE_SMOOTH)));
            logo.setAlignmentX(LEFT_ALIGNMENT);
            brand.add(logo);
        } else {
            brand.add(label("B-CLINIC", bold(26), TEAL));
        }
        brand.add(Box.createVerticalStrut(9));
        brand.add(label("PHÒNG KHÁM NHA KHOA", font(10), new Color(0x608480)));
        top.add(brand);

        top.add(menuItem("Tổng quan", LineIcon.Type.GRID, () -> moTrang("Tổng quan")));
        top.add(Box.createVerticalStrut(4));
        top.add(menuItem("Lịch hẹn", LineIcon.Type.CALENDAR, () -> moTrang("Lịch trực")));
        top.add(Box.createVerticalStrut(4));
        top.add(menuItem("Lịch trực", LineIcon.Type.CALENDAR, () -> moTrang("Lịch của tôi")));
        top.add(Box.createVerticalStrut(4));
        top.add(menuItem("Khám", LineIcon.Type.CHECK, () -> moTrang("Khám")));
        top.add(Box.createVerticalStrut(4));
        top.add(menuItem("Bệnh án", LineIcon.Type.CLIPBOARD, () -> moTrang("Bệnh án")));
        top.add(Box.createVerticalStrut(4));
        top.add(menuItem("Thống kê", LineIcon.Type.CHART, () -> moTrang("Thống kê")));
        sidebar.add(top, BorderLayout.NORTH);

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
        lblCaSidebar.setFont(font(11));
        lblCaSidebar.setForeground(new Color(0x3C6E6A));
        lblCaSidebar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        branch.add(lblCaSidebar);
        branch.add(Box.createVerticalStrut(6));
        branch.add(label("● Đã kết nối máy chủ", font(10), GREEN));
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
        help.addActionListener(e -> hienHuongDan());
        bottom.add(help);
        bottom.add(Box.createVerticalStrut(6));
        bottom.add(taoNutDangXuat());
        sidebar.add(bottom, BorderLayout.SOUTH);
        return sidebar;
    }

    /** Nút Đăng xuất nằm dưới cùng thanh điều hướng dọc. */
    private JButton taoNutDangXuat() {
        JButton logout = new JButton("Đăng xuất");
        logout.setLayout(new BorderLayout());
        logout.setHorizontalAlignment(SwingConstants.LEFT);
        logout.setFont(bold(13));
        logout.setForeground(RED);
        logout.setIcon(new LineIcon(LineIcon.Type.LOGOUT, RED, 16));
        logout.setIconTextGap(11);
        logout.setBorder(new EmptyBorder(10, 13, 10, 12));
        logout.setBackground(RED_LIGHT);
        logout.setContentAreaFilled(true);
        logout.setOpaque(true);
        logout.setBorderPainted(false);
        logout.setFocusPainted(false);
        logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logout.setAlignmentX(LEFT_ALIGNMENT);
        logout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        logout.addActionListener(e -> dangXuat.run());
        return logout;
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
        button.setLayout(new BorderLayout());
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(font(14));
        button.setForeground(TEXT);
        button.setIcon(new LineIcon(icon, MUTED, 17));
        button.setIconTextGap(13);
        button.setBorder(new EmptyBorder(10, 14, 10, 12));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
        if ("Lịch của tôi".equals(trang)) {
            if (pnlLichCaNhan == null) {
                pnlLichCaNhan = new PnlLichCaNhan();
                pnlTrang.add(pnlLichCaNhan, trang);
            }
            pnlLichCaNhan.lamMoiDuLieu();
        }
        if ("Tổng quan".equals(trang) && pnlTongQuan != null) {
            pnlTongQuan.lamMoi();
        }
        if ("Thống kê".equals(trang) && pnlThongKe != null) {
            pnlThongKe.lamMoi();
        }
        theTrang.show(pnlTrang, trang);
        lblTrang.setText(trang);
        if ("Khám".equals(trang)) {
            pnlKham.lamMoi();
        }
        if ("Bệnh án".equals(trang)) {
            pnlTraCuu.lamMoi();
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

    /** Mở màn Bệnh án (UC024) và chọn sẵn một bệnh nhân. */
    private void moHoSoBenhAn(String maKH) {
        pnlTraCuu.chuanBiChon(maKH);
        moTrang("Bệnh án");
    }

    /** Từ Lịch trực mở màn Bệnh án của lịch đang chọn. */
    private void xemBenhAn() {
        if (dangChon != null) {
            moHoSoBenhAn(dangChon.getKhachHang().getMaKH());
        }
    }

    private void moKhamTuTraCuu(String maKH) {
        pnlKham.moKhamTheoKhachHang(maKH, () -> {
            theTrang.show(pnlTrang, "Khám");
            lblTrang.setText("Bệnh án");
        }, () -> moTrang("Bệnh án"));
    }

    private static LineIcon.Type iconCua(String ten) {
        return switch (ten) {
            case "Tổng quan" ->
                LineIcon.Type.GRID;
            case "Lịch trực", "Lịch của tôi" ->
                LineIcon.Type.CALENDAR;
            case "Khám" ->
                LineIcon.Type.CHECK;
            case "Bệnh án" ->
                LineIcon.Type.CLIPBOARD;
            case "Thống kê" ->
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

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        controls.setOpaque(false);

        JButton bell = new JButton(new LineIcon(LineIcon.Type.BELL, MUTED, 23)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (coLichChoHomNay()) {
                    g.setColor(new Color(0xD93A3A));
                    g.fillOval(getWidth() - 10, 4, 6, 6);
                }
            }
        };
        bell.setContentAreaFilled(false);
        bell.setBorderPainted(false);
        bell.setFocusPainted(false);
        bell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bell.setPreferredSize(new Dimension(32, 38));
        bell.addActionListener(e -> hienThongBaoLich());
        btnThongBao = bell;
        controls.add(bell);

        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 30));
        controls.add(sep);

        JButton profile = taoHoSo();
        controls.add(profile);
        header.add(controls, BorderLayout.EAST);
        return header;
    }

    private JButton taoHoSo() {
        JButton profile = new JButton();
        profile.setLayout(new BorderLayout(12, 0));
        profile.setContentAreaFilled(false);
        profile.setBorderPainted(false);
        profile.setFocusPainted(false);
        profile.setCursor(Cursor.getDefaultCursor());
        profile.setPreferredSize(new Dimension(258, 45));

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
        info.add(label(tenNhaSi(), bold(12), TEXT));
        info.add(Box.createVerticalStrut(3));
        info.add(label("Nha sĩ", font(10), MUTED));

        profile.add(avatar, BorderLayout.WEST);
        profile.add(info, BorderLayout.CENTER);
        return profile;
    }

    private JPanel taoNoiDung() {
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(BACKGROUND);
        content.setBorder(new EmptyBorder(24, 24, 30, 24));

        JPanel tieuDe = new JPanel(new BorderLayout());
        tieuDe.setOpaque(false);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel title = label("Lịch hẹn trong ngày", bold(22), TEXT);
        JLabel sub = label("Lịch làm việc của " + tenNhaSi() + " · Phòng khám B-Clinic", font(12), MUTED);
        text.add(title);
        text.add(Box.createVerticalStrut(4));
        text.add(sub);
        tieuDe.add(text, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 0));
        actions.setOpaque(false);
        actions.add(btnHomNay);
        tieuDe.add(actions, BorderLayout.EAST);
        content.add(tieuDe, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        pnlTuan.setOpaque(false);
        pnlTuan.setPreferredSize(new Dimension(0, 88));
        body.add(pnlTuan, BorderLayout.NORTH);
        body.add(taoHaiCot(), BorderLayout.CENTER);
        content.add(body, BorderLayout.CENTER);
        return content;
    }

    private JPanel taoHaiCot() {
        JPanel haiCot = new JPanel(new GridBagLayout());
        haiCot.setOpaque(false);

        JPanel trai = new JPanel();
        trai.setOpaque(false);
        trai.setLayout(new BoxLayout(trai, BoxLayout.Y_AXIS));
        trai.setPreferredSize(new Dimension(342, 0));
        trai.add(taoTheCaTruc());
        trai.add(Box.createVerticalStrut(14));
        trai.add(taoTheTongHop());
        trai.add(Box.createVerticalGlue());

        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1;
        g.gridx = 0;
        g.weightx = 0;
        g.insets = new Insets(0, 0, 0, 16);
        haiCot.add(trai, g);

        g.gridx = 1;
        g.weightx = 1;
        g.insets = new Insets(0, 0, 0, 0);
        haiCot.add(taoTheLichHen(), g);
        return haiCot;
    }

    private JPanel taoTheCaTruc() {
        SurfacePanel card = new SurfacePanel(new BorderLayout());
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        card.setPreferredSize(new Dimension(342, 260));
        pnlCaTruc.setOpaque(false);
        pnlCaTruc.setLayout(new BoxLayout(pnlCaTruc, BoxLayout.Y_AXIS));
        card.add(pnlCaTruc, BorderLayout.CENTER);
        return card;
    }

    private JPanel taoTheTongHop() {
        SurfacePanel card = new SurfacePanel(new BorderLayout());
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        card.setPreferredSize(new Dimension(342, 200));
        pnlTongHop.setOpaque(false);
        pnlTongHop.setLayout(new BoxLayout(pnlTongHop, BoxLayout.Y_AXIS));
        card.add(pnlTongHop, BorderLayout.CENTER);
        return card;
    }

    private JPanel taoTheLichHen() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(16, 18, 14, 18));

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        lblTieuDeNgay.setFont(bold(15));
        lblTieuDeNgay.setForeground(TEXT);
        heading.add(lblTieuDeNgay, BorderLayout.WEST);
        JPanel doiNgay = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        doiNgay.setOpaque(false);
        JButton truoc = nutMuiTen("<");
        JButton sau = nutMuiTen(">");
        truoc.addActionListener(e -> doiNgay(-1));
        sau.addActionListener(e -> doiNgay(1));
        doiNgay.add(truoc);
        doiNgay.add(sau);
        heading.add(doiNgay, BorderLayout.EAST);
        north.add(heading);
        north.add(Box.createVerticalStrut(10));
        north.add(taoBoLoc());
        card.add(north, BorderLayout.NORTH);

        cauHinhBang();
        JScrollPane scr = new JScrollPane(tblLich);
        scr.setBorder(BorderFactory.createLineBorder(BORDER));
        scr.getViewport().setBackground(SURFACE);

        lblKhongCoKetQua.setFont(bold(16));
        lblKhongCoKetQua.setForeground(TEXT);
        JLabel goiY = new JLabel("Không có lịch hẹn trong ngày đã chọn.", SwingConstants.CENTER);
        goiY.setFont(font(12));
        goiY.setForeground(MUTED);
        JPanel pnlTrong = new JPanel();
        pnlTrong.setBackground(SURFACE);
        pnlTrong.setBorder(BorderFactory.createLineBorder(BORDER));
        pnlTrong.setLayout(new BoxLayout(pnlTrong, BoxLayout.Y_AXIS));
        pnlTrong.add(Box.createVerticalGlue());
        lblKhongCoKetQua.setAlignmentX(Component.CENTER_ALIGNMENT);
        goiY.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlTrong.add(lblKhongCoKetQua);
        pnlTrong.add(Box.createVerticalStrut(5));
        pnlTrong.add(goiY);
        pnlTrong.add(Box.createVerticalGlue());
        pnlNoiDung.setOpaque(false);
        pnlNoiDung.add(scr, THE_BANG);
        pnlNoiDung.add(pnlTrong, THE_TRONG);
        card.add(pnlNoiDung, BorderLayout.CENTER);

        JPanel south = new JPanel();
        south.setOpaque(false);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        SurfacePanel khung = new SurfacePanel(new BorderLayout(8, 0), 8);
        khung.setBorder(new EmptyBorder(8, 12, 8, 12));
        khung.setBackground(new Color(0xF7F9FB));
        khung.add(new JLabel(new LineIcon(LineIcon.Type.CALENDAR, MUTED, 15)), BorderLayout.WEST);
        lblKhungTrong.setFont(font(11));
        lblKhungTrong.setForeground(MUTED);
        khung.add(lblKhungTrong, BorderLayout.CENTER);
        khung.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        south.add(khung);
        south.add(Box.createVerticalStrut(10));
        south.add(taoChiTiet());
        lblSoLuong.setFont(font(10));
        lblSoLuong.setForeground(MUTED);
        JPanel ghiChu = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        ghiChu.setOpaque(false);
        ghiChu.add(lblSoLuong);
        south.add(Box.createVerticalStrut(6));
        south.add(ghiChu);
        card.add(south, BorderLayout.SOUTH);
        return card;
    }

    private void cauHinhBang() {
        tblLich.setRowHeight(48);
        tblLich.setFont(font(12));
        tblLich.getTableHeader().setFont(bold(10));
        tblLich.getTableHeader().setForeground(MUTED);
        tblLich.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblLich.setAutoCreateRowSorter(false);
        tblLich.setRowSorter(new TableRowSorter<>(modelLich));
        tblLich.setSelectionBackground(ROW_SELECTED);
        tblLich.setSelectionForeground(TEXT);
        tblLich.setGridColor(new Color(0xEDF1F5));
        tblLich.setShowVerticalLines(false);
        tblLich.setFillsViewportHeight(true);
        TableColumnModel cot = tblLich.getColumnModel();
        cot.getColumn(0).setPreferredWidth(105);
        cot.getColumn(1).setPreferredWidth(240);
        cot.getColumn(2).setPreferredWidth(210);
        cot.getColumn(3).setPreferredWidth(105);
        cot.getColumn(4).setPreferredWidth(80);
        cot.getColumn(0).setCellRenderer(new PadRenderer(font(12), TEXT, false));
        cot.getColumn(1).setCellRenderer(new PadRenderer(bold(12), TEXT, true));
        cot.getColumn(2).setCellRenderer(new PadRenderer(font(12), TEXT, false));
        cot.getColumn(3).setCellRenderer(new TrangThaiRenderer());
        cot.getColumn(4).setCellRenderer(new ChiTietRenderer());
        DefaultTableCellRenderer headerLeft = new DefaultTableCellRenderer();
        headerLeft.setOpaque(true);
        headerLeft.setBackground(new Color(0xF7F9FB));
        headerLeft.setHorizontalAlignment(SwingConstants.LEFT);
        headerLeft.setFont(bold(10));
        headerLeft.setForeground(MUTED);
        headerLeft.setBorder(new EmptyBorder(8, 20, 8, 10));
        DefaultTableCellRenderer headerCenter = new DefaultTableCellRenderer();
        headerCenter.setOpaque(true);
        headerCenter.setBackground(new Color(0xF7F9FB));
        headerCenter.setHorizontalAlignment(SwingConstants.CENTER);
        headerCenter.setFont(bold(10));
        headerCenter.setForeground(MUTED);
        headerCenter.setBorder(new EmptyBorder(8, 6, 8, 6));
        for (int i = 0; i < cot.getColumnCount(); i++) {
            cot.getColumn(i).setHeaderRenderer(i < 3 ? headerLeft : headerCenter);
        }
    }

    private JPanel taoBoLoc() {
        JPanel boLoc = new JPanel(new BorderLayout(8, 0));
        boLoc.setOpaque(false);
        txtTimKiem.setFont(font(12));
        txtTimKiem.putClientProperty("JTextField.placeholderText", "Tên, mã bệnh nhân, mã lịch hẹn...");
        txtTimKiem.setPreferredSize(new Dimension(250, 34));
        cboTrangThai.setFont(font(12));
        cboTrangThai.setPreferredSize(new Dimension(180, 34));
        JPanel phai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        phai.setOpaque(false);
        phai.add(cboTrangThai);
        boLoc.add(txtTimKiem, BorderLayout.CENTER);
        boLoc.add(phai, BorderLayout.EAST);
        return boLoc;
    }

    private JPanel taoChiTiet() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(12, 0));
        card.setBorder(new EmptyBorder(10, 12, 10, 12));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        lblChiTietTen.setFont(bold(12));
        lblChiTietTen.setForeground(TEXT);
        lblChiTietMeta.setFont(font(11));
        lblChiTietMeta.setForeground(MUTED);
        lblChiTietTrangThai.setFont(font(11));
        lblChiTietTrangThai.setForeground(TEAL);
        text.add(lblChiTietTen);
        text.add(Box.createVerticalStrut(3));
        text.add(lblChiTietMeta);
        text.add(Box.createVerticalStrut(3));
        text.add(lblChiTietTrangThai);
        card.add(text, BorderLayout.CENTER);

        btnXemBenhAn.setFont(bold(11));
        btnXemBenhAn.setBackground(SURFACE);
        btnXemBenhAn.setForeground(TEXT);
        btnXemBenhAn.setIcon(new LineIcon(LineIcon.Type.CLIPBOARD, MUTED, 15));
        btnXemBenhAn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnXemBenhAn.addActionListener(e -> xemBenhAn());
        btnXemBenhAn.setEnabled(false);
        card.add(btnXemBenhAn, BorderLayout.EAST);
        return card;
    }

    private JPanel taoThanhTrangThai() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBackground(SURFACE);
        status.setPreferredSize(new Dimension(0, 24));
        status.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER), new EmptyBorder(0, 14, 0, 14)));
        status.add(label("B-Clinic 1.0.0  |  Máy trạm  |  Dữ liệu đã đồng bộ", font(10), MUTED), BorderLayout.WEST);
        lblDongHo.setFont(font(10));
        lblDongHo.setForeground(MUTED);
        status.add(lblDongHo, BorderLayout.EAST);
        return status;
    }

    private JButton nutMuiTen(String text) {
        JButton button = new JButton(text);
        button.setFont(bold(12));
        button.setForeground(MUTED);
        button.setBackground(SURFACE);
        button.setPreferredSize(new Dimension(30, 28));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    // ---------------------------------------------------------------- dựng dữ liệu giao diện
    private void dungThanhTuan() {
        pnlTuan.removeAll();
        LocalDate thuHai = ngayChon.with(DayOfWeek.MONDAY);
        for (int i = 0; i < 7; i++) {
            LocalDate ngay = thuHai.plusDays(i);
            pnlTuan.add(new TheNgay(ngay));
        }
        pnlTuan.revalidate();
        pnlTuan.repaint();
    }

    private final class TheNgay extends JPanel {

        private final LocalDate ngay;
        private final boolean dangChon;
        private final boolean laHomNay;

        TheNgay(LocalDate ngay) {
            this.ngay = ngay;
            this.dangChon = ngay.equals(ngayChon);
            this.laHomNay = ngay.equals(homNay);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(10, 12, 10, 12));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JPanel dong = new JPanel(new BorderLayout());
            dong.setOpaque(false);
            dong.setAlignmentX(Component.LEFT_ALIGNMENT);
            dong.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
            JLabel thu = label(tenThu(ngay.getDayOfWeek()), bold(10), MUTED);
            JLabel soNgay = label(ngay.format(DateTimeFormatter.ofPattern("dd/MM")), bold(12), TEXT);
            dong.add(thu, BorderLayout.WEST);
            dong.add(soNgay, BorderLayout.EAST);
            add(dong);
            add(Box.createVerticalStrut(7));

            List<LichLamViec> ca = caCuaNgay(ngay);
            boolean nghi = ca.stream().anyMatch(c -> c.getTrangThai() == TrangThaiLichLamViec.NGHI_PHEP);
            if (ca.isEmpty()) {
                if (nghi) {
                    add(trai(label("Nghỉ", bold(10), RED)));
                    add(Box.createVerticalStrut(4));
                    add(trai(label("Không có ca trực", font(9), MUTED)));
                } else {
                    add(trai(label("Không có ca trực", font(9), MUTED)));
                }
            } else {
                LichLamViec c = ca.getFirst();
                add(trai(label(tenCa(c.getCaTruc()), bold(10), TEAL)));
                add(Box.createVerticalStrut(4));
                add(trai(label(c.getGioBatDau().format(GIO) + "–" + c.getGioKetThuc().format(GIO), font(9), MUTED)));
            }
            if (laHomNay) {
                add(Box.createVerticalStrut(4));
                add(trai(label("Hôm nay", font(9), new Color(0x3C6E6A))));
            }
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    chonNgay(ngay);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(dangChon ? TEAL_LIGHT : (laHomNay ? new Color(0xF7FBFA) : SURFACE));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.setColor(dangChon ? TEAL : BORDER);
            g2.setStroke(new BasicStroke(dangChon ? 1.6f : 1f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        public boolean isOpaque() {
            return false;
        }
    }

    private void capNhatNgay() {
        dsNgay = duLieuTuan.stream()
                .filter(lh -> lh.getNgayGioHen().toLocalDate().equals(ngayChon))
                .sorted(Comparator.comparing(LichHen::getNgayGioHen))
                .toList();
        lblTieuDeNgay.setText("Lịch hẹn ngày " + ngayChon.format(NGAY));
        capNhatCaTruc();
        capNhatTongHop();
        capNhatKhungTrong();
        apDungLoc();
        dungThanhTuan();
    }

    private void capNhatCaTruc() {
        pnlCaTruc.removeAll();
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        heading.add(label(ngayChon.equals(homNay) ? "Ca trực hôm nay" : "Ca trực trong ngày", bold(14), TEXT), BorderLayout.WEST);
        List<LichLamViec> ca = caCuaNgay(ngayChon).stream()
                .filter(c -> c.getTrangThai() == TrangThaiLichLamViec.DA_PHAN_CONG)
                .toList();
        String nhanCa = ca.isEmpty() ? "Nghỉ" : tenCa(ca.getFirst().getCaTruc());
        boolean dangTrongCa = ca.stream().anyMatch(this::dangTrongCa);
        JLabel badge = pill(dangTrongCa ? "Đang trong ca" : nhanCa,
                dangTrongCa || !ca.isEmpty() ? GREEN : MUTED,
                dangTrongCa || !ca.isEmpty() ? new Color(0xE7F7EF) : new Color(0xF0F2F5));
        heading.add(badge, BorderLayout.EAST);
        pnlCaTruc.add(heading);
        pnlCaTruc.add(Box.createVerticalStrut(12));
        pnlCaTruc.add(trai(label(ngayChon.format(NGAY_DAY_DU), font(11), MUTED)));
        pnlCaTruc.add(Box.createVerticalStrut(7));

        if (ca.isEmpty()) {
            pnlCaTruc.add(trai(label("Không có ca trực", bold(12), TEXT)));
            pnlCaTruc.add(Box.createVerticalStrut(6));
            pnlCaTruc.add(trai(label("Ngày này bạn không được phân công ca làm việc.", font(11), MUTED)));
        } else {
            for (LichLamViec c : ca) {
                pnlCaTruc.add(trai(label(c.getGioBatDau().format(GIO) + "–" + c.getGioKetThuc().format(GIO)
                        + " · " + tenCa(c.getCaTruc()), bold(12), TEXT)));
                pnlCaTruc.add(Box.createVerticalStrut(8));
            }
            pnlCaTruc.add(trai(label("Vị trí", font(10), MUTED)));
            pnlCaTruc.add(Box.createVerticalStrut(3));
            pnlCaTruc.add(trai(label("Phòng khám · Ghế điều trị", bold(12), TEXT)));
        }
        pnlCaTruc.add(Box.createVerticalStrut(12));
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        pnlCaTruc.add(sep);
        pnlCaTruc.add(Box.createVerticalStrut(8));
        boolean coChieu = ca.stream().anyMatch(c -> c.getCaTruc() != CaTruc.CA_SANG);
        pnlCaTruc.add(trai(label("Ca chiều: " + (coChieu ? "Có lịch trực" : "Không có lịch trực"), font(10), MUTED)));
        pnlCaTruc.revalidate();
        pnlCaTruc.repaint();
    }

    private void capNhatTongHop() {
        pnlTongHop.removeAll();
        pnlTongHop.add(trai(label("Tổng hợp trong ngày", bold(14), TEXT)));
        pnlTongHop.add(Box.createVerticalStrut(12));
        int daDen = 0;
        int daDat = 0;
        int hoanTat = 0;
        int daHuy = 0;
        for (LichHen lh : dsNgay) {
            switch (lh.getTrangThai()) {
                case DA_DEN ->
                    daDen++;
                case DA_DAT ->
                    daDat++;
                case HOAN_TAT ->
                    hoanTat++;
                case DA_HUY, KHONG_DEN ->
                    daHuy++;
            }
        }
        dongTongHop("Tất cả lịch hẹn", String.format("%02d", dsNgay.size()));
        dongTongHop("Đã đến", String.format("%02d", daDen));
        dongTongHop("Đã đặt", String.format("%02d", daDat));
        dongTongHop("Hoàn tất / Đã hủy", String.format("%02d / %02d", hoanTat, daHuy));
        pnlTongHop.revalidate();
        pnlTongHop.repaint();
    }

    private void dongTongHop(String nhan, String giaTri) {
        JPanel dong = new JPanel(new BorderLayout());
        dong.setOpaque(false);
        dong.setAlignmentX(Component.LEFT_ALIGNMENT);
        dong.setMaximumSize(new Dimension(Integer.MAX_VALUE, 27));
        JLabel left = label(nhan, font(11), MUTED);
        JLabel right = label(giaTri, bold(12), TEXT);
        dong.add(left, BorderLayout.WEST);
        dong.add(right, BorderLayout.EAST);
        pnlTongHop.add(dong);
        pnlTongHop.add(Box.createVerticalStrut(7));
    }

    private void capNhatKhungTrong() {
        List<String> khung = tinhKhungTrong(dsNgay, caCuaNgay(ngayChon));
        lblKhungTrong.setText(khung.isEmpty()
                ? "Không còn khung giờ trống trong ca."
                : "Khung giờ trống: " + String.join(" · ", khung));
    }

    private void apDungLoc() {
        TrangThaiLichHen trangThai = TRANG_THAI[cboTrangThai.getSelectedIndex()];
        String tuKhoa = txtTimKiem.getText();
        List<LichHen> ketQua = locDanhSach(dsNgay, ngayChon, true, trangThai, tuKhoa);
        hienThi(ketQua);
    }

    private void hienThi(List<LichHen> ds) {
        modelLich.setRowCount(0);
        for (LichHen lh : ds) {
            modelLich.addRow(new Object[]{
                lh.getNgayGioHen().format(GIO) + "–" + lh.getGioKetThuc().format(GIO),
                lh.getKhachHang().getTenKH(),
                lh.getDichVu().getTenDV(),
                nhanTrangThai(lh.getTrangThai()),
                "Chi tiết"});
        }
        if (ds.isEmpty()) {
            lblKhongCoKetQua.setText("Không có lịch hẹn");
            lblSoLuong.setText(" ");
            theNoiDung.show(pnlNoiDung, THE_TRONG);
            hienChiTiet(null);
            return;
        }
        theNoiDung.show(pnlNoiDung, THE_BANG);
        lblSoLuong.setText("Hiển thị " + ds.size() + " trong tổng số " + dsNgay.size()
                + " lịch hẹn · Chỉ hiển thị lịch của bạn");
        int hang = tblLich.convertRowIndexToView(0);
        tblLich.getSelectionModel().setSelectionInterval(hang, hang);
        hienChiTiet(ds.getFirst());
    }

    private void hienChiTiet(LichHen lh) {
        dangChon = lh;
        if (lh == null) {
            lblChiTietTen.setText("Chọn một lịch hẹn để xem chi tiết");
            lblChiTietMeta.setText(" ");
            lblChiTietTrangThai.setText(" ");
            btnXemBenhAn.setEnabled(false);
            return;
        }
        lblChiTietTen.setText(lh.getKhachHang().getTenKH() + " · Lịch hẹn lúc " + lh.getNgayGioHen().format(GIO));
        lblChiTietMeta.setText(lh.getKhachHang().getSdt() + "  ·  Thời lượng "
                + lh.getDichVu().getThoiLuong() + " phút  ·  " + lh.getDichVu().getTenDV());
        lblChiTietTrangThai.setText(moTaTrangThai(lh));
        lblChiTietTrangThai.setForeground(switch (lh.getTrangThai()) {
            case DA_DAT ->
                AMBER;
            case DA_DEN, HOAN_TAT ->
                GREEN;
            case DA_HUY, KHONG_DEN ->
                RED;
        });
        btnXemBenhAn.setEnabled(true);
    }

    private static String moTaTrangThai(LichHen lh) {
        return switch (lh.getTrangThai()) {
            case DA_DAT ->
                "Chưa ghi nhận đến · Chờ lễ tân xác nhận";
            case DA_DEN ->
                "Đã đến · Đang chờ khám";
            case HOAN_TAT ->
                "Đã hoàn tất khám";
            case DA_HUY ->
                "Đã hủy" + (lh.getLyDoHuy() == null ? "" : " · " + lh.getLyDoHuy());
            case KHONG_DEN ->
                "Không đến";
        };
    }

    // ---------------------------------------------------------------- tải dữ liệu
    private void taiDuLieu() {
        if (worker != null) {
            worker.cancel(true);
        }
        LocalDate ngay = ngayChon;
        LocalDate thuHai = ngay.with(DayOfWeek.MONDAY);
        LocalDate chuNhat = thuHai.plusDays(6);
        lblSoLuong.setText("Đang tải lịch hẹn...");
        worker = new SwingWorker<>() {
            @Override
            protected DuLieu doInBackground() {
                return new DuLieu(
                        lichHenDAO.timKiem(thuHai, chuNhat, null),
                        lichLamViecDAO.layTheoNhanVien(maNV, thuHai, chuNhat));
            }

            @Override
            protected void done() {
                try {
                    DuLieu duLieu = get();
                    homNay = LocalDate.now(ZONE);
                    dauTuan = thuHai;
                    duLieuTuan = new ArrayList<>(duLieu.lichHen());
                    caTuan = new ArrayList<>(duLieu.caTruc());
                    capNhatNgay();
                    lblCaSidebar.setText(caHomNayTomTat());
                    btnThongBao.repaint();
                } catch (CancellationException ignored) {
                    return;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    baoLoi(e.getCause());
                }
            }
        };
        worker.execute();
    }

    private void chonNgay(LocalDate ngay) {
        if (ngay.equals(ngayChon)) {
            return;
        }
        boolean doiTuan = !ngay.with(DayOfWeek.MONDAY).equals(dauTuan);
        ngayChon = ngay;
        if (doiTuan) {
            taiDuLieu();
        } else {
            capNhatNgay();
        }
    }

    private void doiNgay(int delta) {
        chonNgay(ngayChon.plusDays(delta));
    }

    private void quayVeHomNay() {
        LocalDate now = LocalDate.now(ZONE);
        if (now.equals(ngayChon)) {
            return;
        }
        chonNgay(now);
    }

    private void addEvents() {
        btnHomNay.addActionListener(e -> quayVeHomNay());
        cboTrangThai.addActionListener(e -> apDungLoc());
        DocumentListener loc = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                apDungLoc();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                apDungLoc();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                apDungLoc();
            }
        };
        txtTimKiem.getDocument().addDocumentListener(loc);
        tblLich.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                hienChiTiet(lichDangChon());
            }
        });
        tblLich.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int hang = tblLich.rowAtPoint(e.getPoint());
                int cot = tblLich.columnAtPoint(e.getPoint());
                if (hang < 0) {
                    return;
                }
                if (tblLich.convertColumnIndexToModel(cot) == 4 || e.getClickCount() == 2) {
                    tblLich.getSelectionModel().setSelectionInterval(hang, hang);
                    LichHen lh = lichDangChon();
                    if (lh != null) {
                        hienChiTietDialog(lh);
                    }
                }
            }
        });
        tblLich.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int cot = tblLich.columnAtPoint(e.getPoint());
                tblLich.setCursor(Cursor.getPredefinedCursor(
                        tblLich.convertColumnIndexToModel(cot) == 4
                        ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        });
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "lamMoi");
        getActionMap().put("lamMoi", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if ("Lịch của tôi".equals(lblTrang.getText()) && pnlLichCaNhan != null) { pnlLichCaNhan.lamMoiDuLieu(); }
                else taiDuLieu();
            }
        });
    }

    private void hienChiTietDialog(LichHen lh) {
        DlgChiTietLichHen.hien(javax.swing.SwingUtilities.getWindowAncestor(this), lh);
    }

    private void hienHuongDan() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(SURFACE);
        p.setBorder(new EmptyBorder(4, 4, 4, 10));
        p.add(trai(label("Mẹo sử dụng nhanh", bold(14), TEXT)));
        p.add(Box.createVerticalStrut(10));
        p.add(dongHuongDan("Chọn một ngày trên thanh tuần để xem lịch hẹn của ngày đó."));
        p.add(dongHuongDan("Nhấn F5 để tải lại dữ liệu mới nhất từ máy chủ."));
        p.add(dongHuongDan("Chọn một lịch hẹn rồi bấm “Xem bệnh án” để mở hồ sơ bệnh nhân."));
        p.add(dongHuongDan("Nhấn vào chuông thông báo để xem các lịch đang chờ hôm nay."));
        JOptionPane.showMessageDialog(this, p, "Hướng dẫn sử dụng", JOptionPane.PLAIN_MESSAGE);
    }

    private JPanel dongHuongDan(String noiDung) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(360, 44));
        JLabel cham = new JLabel(new LineIcon(LineIcon.Type.CHECK, TEAL, 15));
        cham.setVerticalAlignment(SwingConstants.TOP);
        row.add(cham, BorderLayout.WEST);
        JLabel txt = new JLabel("<html><div style='width:300px'>" + noiDung + "</div></html>");
        txt.setFont(font(12));
        txt.setForeground(TEXT);
        row.add(txt, BorderLayout.CENTER);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(0, 0, 8, 0));
        wrap.add(row, BorderLayout.CENTER);
        return wrap;
    }

    private void hienThongBaoLich() {
        List<LichHen> homNayList = duLieuTuan.stream()
                .filter(lh -> lh.getNgayGioHen().toLocalDate().equals(homNay))
                .filter(lh -> lh.getTrangThai() == TrangThaiLichHen.DA_DAT
                        || lh.getTrangThai() == TrangThaiLichHen.DA_DEN)
                .toList();
        List<PopupThongBao.Dong> ds = homNayList.stream().map(lh -> {
            boolean choKham = lh.getTrangThai() == TrangThaiLichHen.DA_DEN;
            return new PopupThongBao.Dong(
                    lh.getNgayGioHen().format(GIO),
                    lh.getKhachHang().getTenKH(),
                    lh.getDichVu().getTenDV(),
                    choKham ? "Đã đến" : "Đã đặt",
                    choKham ? GREEN : AMBER,
                    choKham ? new Color(0xE7F7EF) : AMBER_LIGHT,
                    () -> chonLichTuThongBao(lh));
        }).toList();
        PopupThongBao.hien(btnThongBao, "Lịch hẹn đang chờ hôm nay", ds,
                "Không có lịch hẹn nào đang chờ.");
    }

    private void chonLichTuThongBao(LichHen lh) {
        ngayChon = lh.getNgayGioHen().toLocalDate();
        capNhatNgay();
        for (int i = 0; i < tblLich.getRowCount(); i++) {
            if (String.valueOf(modelLich.getValueAt(i, 1)).contains(lh.getKhachHang().getTenKH())) {
                tblLich.getSelectionModel().setSelectionInterval(i, i);
                break;
            }
        }
    }

    private void capNhatDongHo() {
        lblDongHo.setText(LocalDateTime.now(ZONE).format(DONG_HO));
    }

    private void baoLoi(Throwable loi) {
        lblSoLuong.setText(" ");
        if (loi instanceof BusinessException) {
            JOptionPane.showMessageDialog(this, loi.getMessage(), "Cảnh báo", JOptionPane.WARNING_MESSAGE);
        } else if (loi instanceof DataAccessException) {
            JOptionPane.showMessageDialog(this, "Không thể tải lịch trực.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Đã xảy ra lỗi, vui lòng thử lại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // tiện ích
    private List<LichLamViec> caCuaNgay(LocalDate ngay) {
        return caTuan.stream()
                .filter(c -> c.getNgayTruc().equals(ngay))
                .sorted(Comparator.comparing(LichLamViec::getGioBatDau))
                .toList();
    }

    private boolean dangTrongCa(LichLamViec c) {
        if (!c.getNgayTruc().equals(homNay) || c.getTrangThai() != TrangThaiLichLamViec.DA_PHAN_CONG) {
            return false;
        }
        LocalTime now = LocalTime.now(ZONE);
        return !now.isBefore(c.getGioBatDau()) && now.isBefore(c.getGioKetThuc());
    }

    private String caHomNayTomTat() {
        List<LichLamViec> ca = caCuaNgay(homNay).stream()
                .filter(c -> c.getTrangThai() == TrangThaiLichLamViec.DA_PHAN_CONG)
                .toList();
        if (ca.isEmpty()) {
            return "Hôm nay: Không có ca trực";
        }
        LichLamViec c = ca.getFirst();
        return "Ca " + (c.getCaTruc() == CaTruc.CA_SANG ? "sáng" : c.getCaTruc() == CaTruc.CA_CHIEU ? "chiều" : "ngày")
                + ": " + c.getGioBatDau().format(GIO) + "–" + c.getGioKetThuc().format(GIO);
    }

    private boolean coLichChoHomNay() {
        return duLieuTuan.stream().anyMatch(lh -> lh.getNgayGioHen().toLocalDate().equals(homNay)
                && (lh.getTrangThai() == TrangThaiLichHen.DA_DAT
                || lh.getTrangThai() == TrangThaiLichHen.DA_DEN));
    }

    private LichHen lichDangChon() {
        int hang = tblLich.getSelectedRow();
        if (hang < 0) {
            return null;
        }
        int goc = tblLich.convertRowIndexToModel(hang);
        TrangThaiLichHen trangThai = TRANG_THAI[cboTrangThai.getSelectedIndex()];
        List<LichHen> ds = locDanhSach(dsNgay, ngayChon, true, trangThai, txtTimKiem.getText());
        return goc < ds.size() ? ds.get(goc) : null;
    }

    private String tenNhaSi() {
        String ten = taiKhoan.getNhanVien().getHoTenNV();
        return ten.startsWith("BS.") ? ten : "BS. " + ten;
    }

    private String vietTat() {
        String[] phan = taiKhoan.getNhanVien().getHoTenNV().trim().split("\\s+");
        if (phan.length == 1) {
            return phan[0].substring(0, Math.min(2, phan[0].length())).toUpperCase(Locale.ROOT);
        }
        return (phan[0].charAt(0) + "" + phan[phan.length - 1].charAt(0)).toUpperCase(Locale.ROOT);
    }

    private static String tenThu(DayOfWeek thu) {
        return switch (thu) {
            case MONDAY ->
                "T2";
            case TUESDAY ->
                "T3";
            case WEDNESDAY ->
                "T4";
            case THURSDAY ->
                "T5";
            case FRIDAY ->
                "T6";
            case SATURDAY ->
                "T7";
            case SUNDAY ->
                "CN";
        };
    }

    private static String tenCa(CaTruc ca) {
        return switch (ca) {
            case CA_SANG ->
                "Ca sáng";
            case CA_CHIEU ->
                "Ca chiều";
            case CA_NGAY ->
                "Cả ngày";
        };
    }

    private static JLabel trai(JLabel lbl) {
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private static JLabel label(String text, java.awt.Font font, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font);
        lbl.setForeground(color);
        return lbl;
    }

    private static JLabel pill(String text, Color fg, Color bg) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            public boolean isOpaque() {
                return false;
            }
        };
        lbl.setFont(bold(10));
        lbl.setForeground(fg);
        lbl.setBorder(new EmptyBorder(4, 10, 4, 10));
        return lbl;
    }

    // logic
    static ThongKe tinhThongKe(List<LichHen> ds, LocalDate ngay) {
        int homNay = 0;
        int hoanTat = 0;
        int choKham = 0;
        int taiKham = 0;
        for (LichHen lh : ds) {
            LocalDate ngayHen = lh.getNgayGioHen().toLocalDate();
            boolean huy = lh.getTrangThai() == TrangThaiLichHen.DA_HUY
                    || lh.getTrangThai() == TrangThaiLichHen.KHONG_DEN;
            if (ngayHen.equals(ngay) && !huy) {
                homNay++;
            }
            if (ngayHen.equals(ngay) && lh.getTrangThai() == TrangThaiLichHen.HOAN_TAT) {
                hoanTat++;
            }
            if (ngayHen.equals(ngay) && lh.getTrangThai() == TrangThaiLichHen.DA_DEN) {
                choKham++;
            }
            if (laTaiKham(lh) && !huy) {
                taiKham++;
            }
        }
        return new ThongKe(homNay, hoanTat, choKham, taiKham);
    }

    static List<LichHen> locDanhSach(List<LichHen> ds, LocalDate ngay, boolean caTuan,
            TrangThaiLichHen trangThai, String tuKhoa) {
        List<LichHen> ketQua = new ArrayList<>();
        for (LichHen lh : ds) {
            if (!caTuan && !lh.getNgayGioHen().toLocalDate().equals(ngay)) {
                continue;
            }
            if (trangThai != null && lh.getTrangThai() != trangThai) {
                continue;
            }
            if (khopTuKhoa(lh, tuKhoa)) {
                ketQua.add(lh);
            }
        }
        ketQua.sort(Comparator.comparing(LichHen::getNgayGioHen));
        return ketQua;
    }

    static boolean khopTuKhoa(LichHen lh, String tuKhoa) {
        if (tuKhoa == null || tuKhoa.isBlank()) {
            return true;
        }
        String khoa = tuKhoa.trim().toLowerCase(Locale.ROOT);
        return chua(lh.getMaLH(), khoa)
                || chua(lh.getKhachHang().getMaKH(), khoa)
                || chua(lh.getKhachHang().getTenKH(), khoa)
                || chua(lh.getKhachHang().getSdt(), khoa)
                || chua(lh.getDichVu().getTenDV(), khoa);
    }

    static boolean laTaiKham(LichHen lh) {
        return lh.getPhieuKhamNguon() != null;
    }

    static String nhanTrangThai(TrangThaiLichHen trangThai) {
        return switch (trangThai) {
            case DA_DAT ->
                "Đã đặt";
            case DA_DEN ->
                "Đã đến";
            case HOAN_TAT ->
                "Hoàn tất";
            case DA_HUY ->
                "Đã hủy";
            case KHONG_DEN ->
                "Không đến";
        };
    }

    static List<String> tinhKhungTrong(List<LichHen> dsNgay, List<LichLamViec> caNgay) {
        List<String> ketQua = new ArrayList<>();
        List<LichHen> hopLe = dsNgay.stream()
                .filter(lh -> lh.getTrangThai() != TrangThaiLichHen.DA_HUY
                        && lh.getTrangThai() != TrangThaiLichHen.KHONG_DEN)
                .sorted(Comparator.comparing(LichHen::getNgayGioHen))
                .toList();
        for (LichLamViec ca : caNgay) {
            if (ca.getTrangThai() != TrangThaiLichLamViec.DA_PHAN_CONG) {
                continue;
            }
            LocalTime conTrong = ca.getGioBatDau();
            for (LichHen lh : hopLe) {
                LocalTime batDau = lh.getNgayGioHen().toLocalTime();
                LocalTime ketThuc = lh.getGioKetThuc().toLocalTime();
                if (!ketThuc.isAfter(ca.getGioBatDau()) || !batDau.isBefore(ca.getGioKetThuc())) {
                    continue;
                }
                if (batDau.isAfter(conTrong)) {
                    themKhung(ketQua, conTrong, batDau.isBefore(ca.getGioKetThuc()) ? batDau : ca.getGioKetThuc());
                }
                if (ketThuc.isAfter(conTrong)) {
                    conTrong = ketThuc;
                }
            }
            if (conTrong.isBefore(ca.getGioKetThuc())) {
                themKhung(ketQua, conTrong, ca.getGioKetThuc());
            }
        }
        return ketQua;
    }

    private static void themKhung(List<String> ketQua, LocalTime tu, LocalTime den) {
        if (tu.plusMinutes(15).isAfter(den)) {
            return;
        }
        String khung = tu.format(GIO) + "–" + den.format(GIO);
        if (!ketQua.contains(khung)) {
            ketQua.add(khung);
        }
    }

    private static boolean chua(String giaTri, String khoa) {
        return giaTri != null && giaTri.toLowerCase(Locale.ROOT).contains(khoa);
    }

    private static final class PadRenderer extends DefaultTableCellRenderer {

        PadRenderer(java.awt.Font font, Color color, boolean bold) {
            setFont(font);
            setForeground(color);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new EmptyBorder(0, 20, 0, 8));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setBackground(isSelected ? ROW_SELECTED : table.getBackground());
            return c;
        }
    }

    private static final class TrangThaiRenderer extends JPanel implements TableCellRenderer {

        private final PillLabel pill = new PillLabel();

        TrangThaiRenderer() {
            super(new GridBagLayout());
            setOpaque(true);
            add(pill);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            setBackground(isSelected ? ROW_SELECTED : table.getBackground());
            String trangThai = String.valueOf(value);
            switch (trangThai) {
                case "Hoàn tất", "Đã đến" ->
                    pill.setPill(trangThai, GREEN, new Color(0xE7F7EF));
                case "Đã hủy", "Không đến" ->
                    pill.setPill(trangThai, RED, RED_LIGHT);
                default ->
                    pill.setPill(trangThai, AMBER, AMBER_LIGHT);
            }
            return this;
        }
    }

    private static final class PillLabel extends JLabel {

        private Color pillBg = AMBER_LIGHT;

        PillLabel() {
            super("", SwingConstants.CENTER);
            setOpaque(false);
            setFont(bold(10));
            setBorder(new EmptyBorder(4, 10, 4, 10));
        }

        void setPill(String text, Color fg, Color bg) {
            setText(text);
            setForeground(fg);
            pillBg = bg;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            FontMetrics fm = g2.getFontMetrics(getFont());
            int rong = Math.min(getWidth() - 4, fm.stringWidth(getText()) + 20);
            g2.setColor(pillBg);
            g2.fill(new RoundRectangle2D.Double((getWidth() - rong) / 2.0, (getHeight() - 20) / 2.0,
                    rong, 20, 20, 20));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class ChiTietRenderer extends DefaultTableCellRenderer {

        ChiTietRenderer() {
            setFont(bold(11));
            setForeground(TEAL);
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
            setBorder(new EmptyBorder(0, 0, 0, 0));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setForeground(TEAL);
            c.setBackground(isSelected ? ROW_SELECTED : table.getBackground());
            return c;
        }
    }
}
