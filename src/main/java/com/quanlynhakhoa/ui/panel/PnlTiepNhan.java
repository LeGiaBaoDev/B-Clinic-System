package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.dao.LichHenDAO;
import com.quanlynhakhoa.dao.PhieuKhamDAO;
import com.quanlynhakhoa.entity.LichHen;
import com.quanlynhakhoa.entity.PhieuKham;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.TrangThaiLichHen;
import com.quanlynhakhoa.entity.enums.TrangThaiPhieuKham;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import static com.quanlynhakhoa.ui.component.UiConstants.BACKGROUND;
import static com.quanlynhakhoa.ui.component.UiConstants.BORDER;
import static com.quanlynhakhoa.ui.component.UiConstants.GREEN;
import static com.quanlynhakhoa.ui.component.UiConstants.MUTED;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL;
import static com.quanlynhakhoa.ui.component.UiConstants.TEXT;
import static com.quanlynhakhoa.ui.component.UiConstants.bold;
import static com.quanlynhakhoa.ui.component.UiConstants.font;
import com.quanlynhakhoa.ui.dialog.DlgPhieuTiepNhan;
import com.quanlynhakhoa.util.SessionManager;
import com.toedter.calendar.JDateChooser;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
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
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.EmptyBorder;

public class PnlTiepNhan extends JPanel {

    private record DuLieu(List<LichHen> lichHen, List<PhieuKham> phieuKham) {

    }

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter NGAY_HIEN_THI = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");

    private final LichHenDAO lichHenDAO = new LichHenDAO();
    private final PhieuKhamDAO phieuKhamDAO = new PhieuKhamDAO();
    private final JDateChooser dpNgay = new JDateChooser();
    private final JButton btnInPhieu = PnlTongQuanQuanLy.button("In phiếu tiếp nhận",
            LineIcon.Type.PRINTER, false);
    private final JButton btnXacNhan = PnlTongQuanQuanLy.button("Xác nhận tiếp nhận",
            LineIcon.Type.CHECK, true);
    private final JCheckBox chkXacNhan = new JCheckBox(
            "Đã đối chiếu họ tên, ngày sinh và SĐT với bệnh nhân.");
    private final JComboBox<String> cboNguon = new JComboBox<>(new String[]{"Lịch hẹn trước"});

    private final JLabel lblBanner = new JLabel();
    private final JLabel lblBannerPhu = new JLabel();
    private final JPanel pnlBanner = new JPanel(new BorderLayout(10, 0));
    private final JLabel lblBadge = pill("Chờ tiếp nhận", MUTED, new Color(0xF0F2F5));
    private final JLabel lblTen = new JLabel("Chưa chọn lịch hẹn");
    private final JLabel lblMaHoSo = new JLabel(" ");
    private final JLabel lblNgaySinh = new JLabel(" ");
    private final JLabel lblSdt = new JLabel(" ");
    private final JLabel lblLichHen = new JLabel(" ");
    private final JLabel lblBacSi = new JLabel(" ");
    private final JLabel lblDichVu = new JLabel(" ");
    private final JLabel lblPhong = new JLabel("Phòng khám B-Clinic");
    private final JLabel lblGioDen = new JLabel(" ");
    private final JLabel lblNguoiTiepNhan = new JLabel(" ");
    private final JLabel lblSomTre = new JLabel(" ");
    private final JLabel lblSoCho = pill("0 bệnh nhân", new Color(0x9A6B00), new Color(0xFFF4DE));
    private final JPanel pnlCho = new JPanel();
    private final JPanel pnlMoi = new JPanel(new BorderLayout(10, 0));
    private final JLabel lblMoi = new JLabel(" ");
    private final JLabel lblMoiPhu = new JLabel(" ");

    private final DefaultTableModel modelLich = new DefaultTableModel(
            new String[]{"GIỜ", "BỆNH NHÂN", "NHA SĨ", "TRẠNG THÁI"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tblLich = new JTable(modelLich);
    private final JLabel lblTieuDeLich = new JLabel("Lịch hẹn");
    private boolean dangCapNhatLich;
    private List<LichHen> dsLich = new ArrayList<>();
    private List<LichHen> dsLoc = new ArrayList<>();
    private List<PhieuKham> dsCho = new ArrayList<>();
    private LichHen dangChon;
    private String maPKMoi;
    private LocalDate ngay = LocalDate.now(ZONE);
    private LocalDateTime gioDen = LocalDateTime.now(ZONE);
    private SwingWorker<DuLieu, Void> worker;
    private boolean dangXuLy;
    private boolean vuaTiepNhan;
    private String maLHCanChon;

    public PnlTiepNhan() {
        super(new BorderLayout(0, 14));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 20, 24));
        initComponents();
        addEvents();
        taiDuLieu();
    }

    /**
     * Tải lại lịch hẹn/phiếu của ngày đang chọn; dùng khi chuyển trang.
     */
    public void lamMoi() {
        taiDuLieu();
    }

    // ---------------------------------------------------------------- giao diện
    private void initComponents() {
        add(taoDau(), BorderLayout.NORTH);
        JScrollPane cuon = new JScrollPane(taoHaiCot());
        cuon.setBorder(null);
        cuon.getViewport().setBackground(BACKGROUND);
        cuon.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        cuon.getVerticalScrollBar().setUnitIncrement(16);
        add(cuon, BorderLayout.CENTER);
    }

    private JPanel taoDau() {
        JPanel dau = new JPanel(new BorderLayout(0, 14));
        dau.setOpaque(false);

        JPanel hang = new JPanel(new BorderLayout());
        hang.setOpaque(false);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel tieuDe = new JLabel("Tiếp nhận bệnh nhân");
        tieuDe.setFont(bold(21));
        tieuDe.setForeground(TEXT);
        JLabel phuDe = new JLabel("Xác nhận lịch hẹn và ghi nhận bệnh nhân đến phòng khám.");
        phuDe.setFont(font(12));
        phuDe.setForeground(MUTED);
        text.add(tieuDe);
        text.add(Box.createVerticalStrut(4));
        text.add(phuDe);
        hang.add(text, BorderLayout.WEST);
        dau.add(hang, BorderLayout.NORTH);

        pnlBanner.setOpaque(true);
        pnlBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xCDEBD8)),
                new EmptyBorder(10, 14, 10, 14)));
        pnlBanner.setBackground(new Color(0xEAF8EF));
        pnlBanner.setVisible(false);
        JLabel icon = new JLabel(new LineIcon(LineIcon.Type.CHECK, GREEN, 18));
        pnlBanner.add(icon, BorderLayout.WEST);
        JPanel bannerText = new JPanel();
        bannerText.setOpaque(false);
        bannerText.setLayout(new BoxLayout(bannerText, BoxLayout.Y_AXIS));
        lblBanner.setFont(bold(12));
        lblBanner.setForeground(GREEN);
        lblBannerPhu.setFont(font(11));
        lblBannerPhu.setForeground(new Color(0x3C6E6A));
        bannerText.add(lblBanner);
        bannerText.add(Box.createVerticalStrut(2));
        bannerText.add(lblBannerPhu);
        pnlBanner.add(bannerText, BorderLayout.CENTER);

        JPanel giua = new JPanel();
        giua.setOpaque(false);
        giua.setLayout(new BoxLayout(giua, BoxLayout.Y_AXIS));
        pnlBanner.setAlignmentX(LEFT_ALIGNMENT);
        pnlBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        giua.add(pnlBanner);
        giua.add(Box.createVerticalStrut(10));
        giua.add(taoThanhTim());
        dau.add(giua, BorderLayout.CENTER);
        return dau;
    }

    private JPanel taoThanhTim() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(12, 0));
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

        JLabel nhan = new JLabel("Ngày xem lịch");
        nhan.setFont(bold(12));
        nhan.setForeground(TEXT);
        nhan.setPreferredSize(new Dimension(110, 22));

        dpNgay.setDateFormatString("dd/MM/yyyy");
        LocalDate now = LocalDate.now(ZONE);
        LocalDate min = LocalDate.of(2000, 1, 1);
        LocalDate max = now.plusMonths(3);
        dpNgay.setMinSelectableDate(Date.from(min.atStartOfDay(ZONE).toInstant()));
        dpNgay.setMaxSelectableDate(Date.from(max.atTime(23, 59, 59).atZone(ZONE).toInstant()));
        dpNgay.setDate(Date.from(ngay.atStartOfDay(ZONE).toInstant()));
        dpNgay.setFont(font(13));
        dpNgay.setOpaque(false);
        dpNgay.setBorder(null);
        try {
            com.toedter.calendar.JCalendar jc = dpNgay.getJCalendar();
            jc.setLocale(Locale.of("vi", "VN"));
            jc.setPreferredSize(new Dimension(380, 310));
            jc.setFont(font(13));
            jc.setBorder(new EmptyBorder(8, 8, 8, 8));
            jc.setWeekOfYearVisible(false);
            jc.setDecorationBackgroundVisible(false);
            jc.setDecorationBordersVisible(false);
            jc.setTodayButtonVisible(true);
            jc.setTodayButtonText("Hôm nay");
            jc.setNullDateButtonVisible(false);
            jc.getDayChooser().setFont(font(13));
            jc.getDayChooser().setBackground(Color.WHITE);
            jc.getDayChooser().setWeekOfYearVisible(false);
            jc.getDayChooser().setDecorationBackgroundColor(new Color(0xE7F4F3));
            jc.getDayChooser().setWeekdayForeground(TEXT);
            jc.getDayChooser().setSundayForeground(new Color(0xC0392B));
            jc.getMonthChooser().setFont(bold(14));
            jc.getYearChooser().setFont(bold(14));
        } catch (RuntimeException ignored) {
        }
        if (dpNgay.getDateEditor().getUiComponent() instanceof javax.swing.JComponent c) {
            c.setFont(bold(13));
            c.setForeground(TEXT);
            c.setBackground(Color.WHITE);
            c.setOpaque(false);
            c.setBorder(null);
            c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            c.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    dpNgay.getCalendarButton().doClick();
                }
            });
        }
        JButton btnLich = dpNgay.getCalendarButton();
        btnLich.setIcon(null);
        btnLich.setText("");
        btnLich.setBorderPainted(false);
        btnLich.setContentAreaFilled(false);
        btnLich.setFocusPainted(false);
        btnLich.setPreferredSize(new Dimension(1, 36));
        JLabel iconLich = new JLabel(new LineIcon(LineIcon.Type.CALENDAR, MUTED, 16));
        iconLich.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        iconLich.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                btnLich.doClick();
            }
        });
        JPanel oNgay = new JPanel(new BorderLayout(8, 0));
        oNgay.setBackground(Color.WHITE);
        oNgay.setPreferredSize(new Dimension(190, 38));
        oNgay.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(0, 11, 0, 9)));
        oNgay.add(iconLich, BorderLayout.WEST);
        oNgay.add(dpNgay, BorderLayout.CENTER);

        JButton btnLamMoi = PnlTongQuanQuanLy.button("Làm mới", LineIcon.Type.CHART, false);
        btnLamMoi.addActionListener(e -> taiDuLieu());
        JPanel benPhai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        benPhai.setOpaque(false);
        benPhai.add(oNgay);
        benPhai.add(btnLamMoi);
        card.add(nhan, BorderLayout.WEST);
        card.add(benPhai, BorderLayout.EAST);
        return card;
    }

    private JPanel taoHaiCot() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1;
        g.anchor = GridBagConstraints.NORTH;
        g.gridx = 0;
        g.weightx = 0.58;
        g.insets = new Insets(0, 0, 0, 16);
        row.add(taoTheXacNhan(), g);
        g.gridx = 1;
        g.weightx = 0.42;
        g.insets = new Insets(0, 0, 0, 0);
        row.add(taoThePhongCho(), g);
        JPanel haiCot = new PanelCuon();
        haiCot.setLayout(new GridBagLayout());
        haiCot.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        c.insets = new Insets(0, 0, 12, 0);
        haiCot.add(taoDanhSachLich(), c);
        c.gridy = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 0);
        haiCot.add(row, c);
        return haiCot;
    }

    private JPanel taoDanhSachLich() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        card.setPreferredSize(new Dimension(0, 130));
        lblTieuDeLich.setFont(bold(13));
        lblTieuDeLich.setForeground(TEXT);
        card.add(lblTieuDeLich, BorderLayout.NORTH);
        tblLich.setRowHeight(28);
        tblLich.setFont(font(12));
        tblLich.getTableHeader().setFont(bold(10));
        tblLich.getTableHeader().setBackground(new Color(0xF7F9FB));
        tblLich.getTableHeader().setForeground(MUTED);
        tblLich.setShowVerticalLines(false);
        tblLich.setGridColor(new Color(0xEDF1F5));
        tblLich.setSelectionBackground(new Color(0xEFF6F5));
        tblLich.setSelectionForeground(TEXT);
        tblLich.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblLich.getColumnModel().getColumn(0).setPreferredWidth(60);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        tblLich.getColumnModel().getColumn(0).setCellRenderer(center);
        JScrollPane scr = new JScrollPane(tblLich);
        scr.setBorder(BorderFactory.createLineBorder(BORDER));
        scr.getViewport().setBackground(Color.WHITE);
        card.add(scr, BorderLayout.CENTER);
        return card;
    }

    private JPanel taoTheXacNhan() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel tieuDe = new JLabel("Xác nhận & thông tin đến khám");
        tieuDe.setFont(bold(14));
        tieuDe.setForeground(TEXT);
        heading.add(tieuDe, BorderLayout.WEST);
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        badge.setOpaque(false);
        badge.add(lblBadge);
        heading.add(badge, BorderLayout.EAST);
        card.add(heading, BorderLayout.NORTH);

        JPanel noiDung = new JPanel();
        noiDung.setOpaque(false);
        noiDung.setLayout(new BoxLayout(noiDung, BoxLayout.Y_AXIS));

        JPanel thongTin = new JPanel(new GridBagLayout());
        thongTin.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 10, 16);
        themCot(thongTin, c, 0, 0, "Bệnh nhân · Mã hồ sơ", lblTen, lblMaHoSo);
        themCot(thongTin, c, 2, 0, "Ngày sinh · Số điện thoại", lblNgaySinh, lblSdt);
        themCot(thongTin, c, 0, 1, "Lịch hẹn · Bác sĩ", lblLichHen, lblBacSi);
        themCot(thongTin, c, 2, 1, "Dịch vụ · Phòng khám", lblDichVu, lblPhong);
        noiDung.add(thongTin);
        noiDung.add(Box.createVerticalStrut(2));

        JPanel dongNhap = new JPanel(new GridLayout(1, 2, 14, 0));
        dongNhap.setOpaque(false);
        dongNhap.setAlignmentX(LEFT_ALIGNMENT);
        dongNhap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        JPanel oGio = oNhap("Giờ đến", taoOGioDen());
        oGio.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        JPanel oNguon = oNhap("Nguồn tiếp nhận", cboNguon);
        oNguon.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        dongNhap.add(oGio);
        dongNhap.add(oNguon);
        cboNguon.setFont(font(12));
        cboNguon.setEnabled(false);
        noiDung.add(dongNhap);
        noiDung.add(Box.createVerticalStrut(12));

        chkXacNhan.setOpaque(false);
        chkXacNhan.setFont(font(11));
        chkXacNhan.setForeground(TEXT);
        JPanel dongCheck = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        dongCheck.setOpaque(false);
        dongCheck.setAlignmentX(LEFT_ALIGNMENT);
        dongCheck.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        chkXacNhan.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        dongCheck.add(chkXacNhan);
        noiDung.add(dongCheck);
        noiDung.add(Box.createVerticalStrut(4));

        JPanel ghiChu = new JPanel(new BorderLayout());
        ghiChu.setOpaque(false);
        lblSomTre.setFont(font(10));
        lblSomTre.setForeground(MUTED);
        lblNguoiTiepNhan.setFont(font(10));
        lblNguoiTiepNhan.setForeground(MUTED);
        ghiChu.add(lblSomTre, BorderLayout.WEST);
        ghiChu.add(lblNguoiTiepNhan, BorderLayout.EAST);
        noiDung.add(ghiChu);
        noiDung.add(Box.createVerticalStrut(6));

        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        nut.setOpaque(false);
        nut.setAlignmentX(LEFT_ALIGNMENT);
        nut.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnInPhieu.setEnabled(false);
        btnXacNhan.setEnabled(false);
        nut.add(btnInPhieu);
        nut.add(btnXacNhan);
        noiDung.add(nut);
        card.add(noiDung, BorderLayout.CENTER);
        return card;
    }

    private JPanel taoOGioDen() {
        JPanel o = new FieldPanel(new BorderLayout(8, 0));
        o.setBorder(new EmptyBorder(8, 12, 8, 10));
        lblGioDen.setFont(font(12));
        lblGioDen.setForeground(TEXT);
        o.add(lblGioDen, BorderLayout.CENTER);
        o.add(new JLabel(new LineIcon(LineIcon.Type.CLOCK, MUTED, 16)), BorderLayout.EAST);
        return o;
    }

    private static JPanel oNhap(String nhan, java.awt.Component truong) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel lbl = new JLabel(nhan);
        lbl.setFont(font(10));
        lbl.setForeground(MUTED);
        truong.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        p.add(lbl, BorderLayout.NORTH);
        p.add(truong, BorderLayout.CENTER);
        return p;
    }

    private static void themCot(JPanel luoi, GridBagConstraints c, int x, int y,
            String nhan, JLabel dong1, JLabel dong2) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel lbl = new JLabel(nhan);
        lbl.setFont(font(10));
        lbl.setForeground(MUTED);
        dong1.setFont(bold(12));
        dong1.setForeground(TEXT);
        p.add(lbl);
        p.add(Box.createVerticalStrut(4));
        p.add(dong1);
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = 2;
        c.weightx = 0.5;
        luoi.add(p, c);
        c.gridwidth = 1;
    }

    private JPanel taoThePhongCho() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel tieuDe = new JLabel("Phòng chờ hiện tại");
        tieuDe.setFont(bold(14));
        tieuDe.setForeground(TEXT);
        heading.add(tieuDe, BorderLayout.WEST);
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        badge.setOpaque(false);
        badge.add(lblSoCho);
        heading.add(badge, BorderLayout.EAST);
        card.add(heading, BorderLayout.NORTH);

        JPanel giua = new JPanel(new BorderLayout(0, 6));
        giua.setOpaque(false);

        JPanel tieuDeBang = new JPanel(new BorderLayout());
        tieuDeBang.setOpaque(true);
        tieuDeBang.setBackground(new Color(0xF7F9FB));
        tieuDeBang.setBorder(new EmptyBorder(6, 8, 6, 8));
        JLabel stt = nhanBang("STT", 42);
        JLabel benhNhan = nhanBang("BỆNH NHÂN", 0);
        JLabel gio = nhanBang("GIỜ ĐẾN", 70);
        gio.setHorizontalAlignment(SwingConstants.RIGHT);
        tieuDeBang.add(stt, BorderLayout.WEST);
        tieuDeBang.add(benhNhan, BorderLayout.CENTER);
        tieuDeBang.add(gio, BorderLayout.EAST);
        giua.add(tieuDeBang, BorderLayout.NORTH);

        JScrollPane scr = new JScrollPane(pnlCho);
        scr.setBorder(BorderFactory.createLineBorder(BORDER));
        scr.getViewport().setBackground(Color.WHITE);
        pnlCho.setBackground(Color.WHITE);
        pnlCho.setLayout(new BoxLayout(pnlCho, BoxLayout.Y_AXIS));
        giua.add(scr, BorderLayout.CENTER);
        card.add(giua, BorderLayout.CENTER);

        pnlMoi.setOpaque(true);
        pnlMoi.setBorder(new EmptyBorder(12, 14, 12, 14));
        pnlMoi.setBackground(new Color(0xEFF6F5));
        pnlMoi.setVisible(false);
        JPanel moiText = new JPanel();
        moiText.setOpaque(false);
        moiText.setLayout(new BoxLayout(moiText, BoxLayout.Y_AXIS));
        lblMoi.setFont(bold(13));
        lblMoi.setForeground(TEXT);
        lblMoiPhu.setFont(font(10));
        lblMoiPhu.setForeground(MUTED);
        moiText.add(lblMoi);
        moiText.add(Box.createVerticalStrut(3));
        moiText.add(lblMoiPhu);
        pnlMoi.add(moiText, BorderLayout.CENTER);

        JLabel ghiChu = new JLabel("<html><div style='width:300px'>Thứ tự gọi khám phụ thuộc "
                + "lịch hẹn và tình trạng thực tế. Không thay đổi giờ hẹn đã xác nhận.</div></html>");
        ghiChu.setFont(font(9));
        ghiChu.setForeground(MUTED);
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        pnlMoi.setAlignmentX(LEFT_ALIGNMENT);
        pnlMoi.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        ghiChu.setAlignmentX(LEFT_ALIGNMENT);
        bottom.add(pnlMoi);
        bottom.add(Box.createVerticalStrut(8));
        bottom.add(ghiChu);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private static JLabel nhanBang(String text, int rong) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(bold(9));
        lbl.setForeground(MUTED);
        if (rong > 0) {
            lbl.setPreferredSize(new Dimension(rong, 18));
        }
        return lbl;
    }

    // ---------------------------------------------------------------- sự kiện
    private void addEvents() {
        dpNgay.addPropertyChangeListener("date", e -> {
            if (dpNgay.getDate() != null && !dpNgay.getDate().equals(e.getOldValue())) {
                taiDuLieu();
            }
        });
        chkXacNhan.addActionListener(e -> capNhatNutXacNhan());
        tblLich.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || dangCapNhatLich) {
                return;
            }
            int row = tblLich.getSelectedRow();
            if (row >= 0 && row < dsLoc.size()) {
                LichHen lh = dsLoc.get(row);
                if (dangChon == null || !dangChon.getMaLH().equals(lh.getMaLH())) {
                    chonLich(lh);
                }
            }
        });
        btnInPhieu.addActionListener(e -> inPhieuTiepNhan());
        btnXacNhan.addActionListener(e -> xacNhanTiepNhan());
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "lamMoi");
        getActionMap().put("lamMoi", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                taiDuLieu();
            }
        });
    }

    // ---------------------------------------------------------------- dữ liệu
    private void taiDuLieu() {
        if (dpNgay.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn ngày cần tiếp nhận.",
                    "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        LocalDate chon = dpNgay.getDate().toInstant().atZone(ZONE).toLocalDate();
        LocalDate homNay = LocalDate.now(ZONE);
        if (chon.isAfter(homNay.plusMonths(3))) {
            JOptionPane.showMessageDialog(this,
                    "Chỉ được chọn tối đa " + homNay.plusMonths(3).format(NGAY_HIEN_THI) + ".",
                    "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (worker != null) {
            worker.cancel(true);
        }
        ngay = chon;
        worker = new SwingWorker<>() {
            @Override
            protected DuLieu doInBackground() {
                TrangThaiLichHen loc = ngay.isBefore(LocalDate.now(ZONE))
                        ? null : TrangThaiLichHen.DA_DAT;
                return new DuLieu(lichHenDAO.timKiem(ngay, ngay, loc),
                        phieuKhamDAO.timKiem(ngay, ngay));
            }

            @Override
            protected void done() {
                try {
                    DuLieu d = get();
                    dsLich = new ArrayList<>(d.lichHen());
                    dsCho = d.phieuKham().stream()
                            .filter(pk -> pk.getTrangThai() == TrangThaiPhieuKham.CHO_KHAM
                                    || pk.getTrangThai() == TrangThaiPhieuKham.DANG_KHAM)
                            .sorted(Comparator.comparing(PhieuKham::getNgayLap))
                            .toList();
                    locLich();
                    capNhatPhongCho();
                    if (dangChon == null && !dsCho.isEmpty()) {
                        chonPhieu(dsCho.getLast());
                    }
                } catch (CancellationException ignored) {
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    baoLoi(e.getCause());
                }
            }
        };
        worker.execute();
    }

    private void locLich() {
        dsLoc = dsLich.stream()
                .sorted(Comparator.comparing(LichHen::getNgayGioHen))
                .toList();
        hienDanhSachLich();
        boolean boQuaChon = vuaTiepNhan;
        vuaTiepNhan = false;
        if (maLHCanChon != null) {
            for (LichHen lh : dsLoc) {
                if (maLHCanChon.equals(lh.getMaLH())) {
                    chonLich(lh);
                    maLHCanChon = null;
                    return;
                }
            }
        }
        if (!boQuaChon && (dangChon == null || !dsLoc.contains(dangChon))) {
            chonLich(dsLoc.isEmpty() ? null : dsLoc.getFirst());
        }
    }

    private void hienDanhSachLich() {
        dangCapNhatLich = true;
        modelLich.setRowCount(0);
        for (LichHen lh : dsLoc) {
            modelLich.addRow(new Object[]{
                lh.getNgayGioHen().format(GIO),
                lh.getKhachHang().getTenKH(),
                lh.getNhaSi() == null ? "—" : lh.getNhaSi().getHoTenNV(),
                nhanTrangThai(lh)});
        }
        lblTieuDeLich.setText("Lịch hẹn ngày " + ngay.format(NGAY_HIEN_THI)
                + " · " + dsLoc.size() + " lịch");
        if (!dsLoc.isEmpty()) {
            int chon = 0;
            String ma = maLHCanChon != null ? maLHCanChon
                    : (dangChon == null ? null : dangChon.getMaLH());
            if (ma != null) {
                for (int i = 0; i < dsLoc.size(); i++) {
                    if (ma.equals(dsLoc.get(i).getMaLH())) {
                        chon = i;
                        break;
                    }
                }
            }
            tblLich.setRowSelectionInterval(chon, chon);
        }
        dangCapNhatLich = false;
    }

    private void timKiem() {
        locLich();
    }

    static boolean khop(LichHen lh, String tuKhoa) {
        String khoa = tuKhoa == null ? "" : tuKhoa.trim().toLowerCase(Locale.ROOT);
        if (khoa.isBlank()) {
            return true;
        }
        return chua(lh.getMaLH(), khoa)
                || chua(lh.getKhachHang().getMaKH(), khoa)
                || chua(lh.getKhachHang().getTenKH(), khoa)
                || chua(lh.getKhachHang().getSdt(), khoa)
                || chua(lh.getNhaSi() == null ? null : lh.getNhaSi().getHoTenNV(), khoa)
                || chua(lh.getDichVu() == null ? null : lh.getDichVu().getTenDV(), khoa);
    }

    private static boolean chua(String value, String tuKhoa) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(tuKhoa);
    }

    private void chonLich(LichHen lh) {
        dangChon = lh;
        maPKMoi = null;
        gioDen = LocalDateTime.now(ZONE);
        chkXacNhan.setSelected(false);
        chkXacNhan.setEnabled(lh != null && lh.getTrangThai() == TrangThaiLichHen.DA_DAT
                && lh.getNgayGioHen().toLocalDate().equals(LocalDate.now(ZONE)));
        btnInPhieu.setEnabled(false);
        if (lh == null) {
            lblTen.setText("Chưa chọn lịch hẹn");
            lblMaHoSo.setText(" ");
            lblNgaySinh.setText(" ");
            lblSdt.setText(" ");
            lblLichHen.setText(" ");
            lblBacSi.setText(" ");
            lblDichVu.setText(" ");
            lblGioDen.setText(" ");
            lblSomTre.setText(" ");
            lblNguoiTiepNhan.setText(" ");
            datBadge("Chờ tiếp nhận", MUTED, new Color(0xF0F2F5));
            capNhatNutXacNhan();
            return;
        }
        dienThongTin(lh);
        if (lh.getTrangThai() == TrangThaiLichHen.DA_DAT) {
            datBadge("Chờ tiếp nhận", new Color(0x9A6B00), new Color(0xFFF4DE));
        } else {
            datBadge(nhanTrangThai(lh), lh.getTrangThai() == TrangThaiLichHen.DA_HUY
                    || lh.getTrangThai() == TrangThaiLichHen.KHONG_DEN ? new Color(0xC0392B) : GREEN,
                    lh.getTrangThai() == TrangThaiLichHen.DA_HUY
                    || lh.getTrangThai() == TrangThaiLichHen.KHONG_DEN ? new Color(0xFDECEA) : new Color(0xE7F7EF));
        }
        capNhatNutXacNhan();
    }

    private void chonPhieu(PhieuKham pk) {
        dangChon = pk.getLichHen();
        maPKMoi = pk.getMaPK();
        gioDen = pk.getNgayLap();
        chkXacNhan.setSelected(false);
        chkXacNhan.setEnabled(false);
        btnInPhieu.setEnabled(true);
        dienThongTin(dangChon);
        datBadge("Đã tiếp nhận", GREEN, new Color(0xE7F7EF));
        capNhatNutXacNhan();
        capNhatPhongCho();
    }

    private void dienThongTin(LichHen lh) {
        lblTen.setText(lh.getKhachHang().getTenKH() + "  -  " + lh.getKhachHang().getMaKH());
        lblMaHoSo.setText(" ");
        lblNgaySinh.setText((lh.getKhachHang().getNgaySinh() == null ? "—"
                : lh.getKhachHang().getNgaySinh().format(NGAY_HIEN_THI))
                + "  ·  " + lh.getKhachHang().getSdt());
        lblSdt.setText(" ");
        lblLichHen.setText(lh.getNgayGioHen().format(GIO) + "–"
                + lh.getGioKetThuc().format(GIO) + "  ·  BS. " + lh.getNhaSi().getHoTenNV());
        lblBacSi.setText(" ");
        lblDichVu.setText(lh.getDichVu().getTenDV() + "  ·  Phòng khám B-Clinic");
        lblGioDen.setText(gioDen.format(GIO));
        lblNguoiTiepNhan.setText("Tiếp nhận bởi " + tenNguoiDung());
        long phut = Duration.between(gioDen, lh.getNgayGioHen()).toMinutes();
        if (phut > 0) {
            lblSomTre.setText("Đến sớm " + phut + " phút · Đúng lịch");
            lblSomTre.setForeground(GREEN);
        } else if (phut < 0) {
            lblSomTre.setText("Đến trễ " + Math.abs(phut) + " phút");
            lblSomTre.setForeground(new Color(0xC0392B));
        } else {
            lblSomTre.setText("Đúng giờ hẹn");
            lblSomTre.setForeground(GREEN);
        }
    }

    private void capNhatNutXacNhan() {
        btnXacNhan.setEnabled(!dangXuLy && maPKMoi == null
                && dangChon != null && dangChon.getTrangThai() == TrangThaiLichHen.DA_DAT
                && dangChon.getNgayGioHen().toLocalDate().equals(LocalDate.now(ZONE))
                && chkXacNhan.isSelected());
    }

    private void xacNhanTiepNhan() {
        if (dangChon == null || !chkXacNhan.isSelected() || dangXuLy) {
            return;
        }
        dangXuLy = true;
        capNhatNutXacNhan();
        String maLH = dangChon.getMaLH();
        new SwingWorker<PhieuKham, Void>() {
            @Override
            protected PhieuKham doInBackground() {
                return phieuKhamDAO.taoTuLichHen(maLH);
            }

            @Override
            protected void done() {
                dangXuLy = false;
                try {
                    PhieuKham pk = get();
                    maPKMoi = pk.getMaPK();
                    btnInPhieu.setEnabled(true);
                    chkXacNhan.setSelected(false);
                    vuaTiepNhan = true;
                    hienBanner(pk);
                    taiDuLieu();
                } catch (CancellationException ignored) {
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    baoLoi(e.getCause());
                } finally {
                    capNhatNutXacNhan();
                }
            }
        }.execute();
    }

    private void hienBanner(PhieuKham pk) {
        pnlBanner.setVisible(true);
        lblBanner.setText("Tiếp nhận thành công");
        lblBannerPhu.setText(pk.getLichHen().getKhachHang().getTenKH()
                + " đã được đưa vào phòng chờ lúc " + pk.getNgayLap().format(GIO)
                + ". BS. " + pk.getLichHen().getNhaSi().getHoTenNV() + " đã được thông báo.");
        datBadge("Đã tiếp nhận", GREEN, new Color(0xE7F7EF));
    }

    private void capNhatPhongCho() {
        pnlCho.removeAll();
        lblSoCho.setText(dsCho.size() + " bệnh nhân");
        if (dsCho.isEmpty()) {
            JLabel trong = new JLabel("Phòng chờ đang trống.", SwingConstants.CENTER);
            trong.setFont(font(11));
            trong.setForeground(MUTED);
            trong.setBorder(new EmptyBorder(20, 0, 20, 0));
            trong.setAlignmentX(CENTER_ALIGNMENT);
            pnlCho.add(trong);
            pnlMoi.setVisible(false);
        } else {
            for (int i = 0; i < dsCho.size(); i++) {
                PhieuKham pk = dsCho.get(i);
                pnlCho.add(dongCho(i + 1, pk));
            }
            PhieuKham moi = dsCho.stream()
                    .filter(pk -> pk.getMaPK().equals(maPKMoi))
                    .findFirst().orElse(null);
            if (moi != null) {
                int stt = dsCho.indexOf(moi) + 1;
                lblMoi.setText(String.format("%02d", stt) + "   "
                        + moi.getLichHen().getKhachHang().getTenKH() + " · Đã đến");
                lblMoiPhu.setText("Phòng khám B-Clinic · BS. "
                        + moi.getLichHen().getNhaSi().getHoTenNV());
                pnlMoi.setVisible(true);
                lblBanner.setText("Tiếp nhận thành công · Số thứ tự " + String.format("%02d", stt));
            } else {
                pnlMoi.setVisible(false);
            }
        }
        pnlCho.revalidate();
        pnlCho.repaint();
    }

    private JPanel dongCho(int stt, PhieuKham pk) {
        boolean chon = pk.getMaPK().equals(maPKMoi);
        JPanel dong = new JPanel(new BorderLayout(8, 0));
        dong.setBackground(chon ? new Color(0xEFF6F5) : Color.WHITE);
        dong.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xEDF1F5)),
                new EmptyBorder(8, 8, 8, 8)));
        JLabel so = new JLabel(String.format("%02d", stt));
        so.setFont(bold(11));
        so.setForeground(chon ? TEAL : TEXT);
        so.setPreferredSize(new Dimension(42, 20));
        JLabel ten = new JLabel(pk.getLichHen().getKhachHang().getTenKH());
        ten.setFont(chon ? bold(11) : font(11));
        ten.setForeground(TEXT);
        JLabel gio = new JLabel(pk.getNgayLap().format(GIO));
        gio.setFont(font(10));
        gio.setForeground(MUTED);
        gio.setPreferredSize(new Dimension(70, 20));
        gio.setHorizontalAlignment(SwingConstants.RIGHT);
        dong.add(so, BorderLayout.WEST);
        dong.add(ten, BorderLayout.CENTER);
        dong.add(gio, BorderLayout.EAST);
        dong.setAlignmentX(LEFT_ALIGNMENT);
        dong.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        dong.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        dong.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                chonPhieu(pk);
            }
        });
        return dong;
    }

    private void inPhieuTiepNhan() {
        if (dangChon == null) {
            return;
        }
        if (maPKMoi == null) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng xác nhận tiếp nhận trước khi in/lưu phiếu.",
                    "Chưa tiếp nhận", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int stt = -1;
        for (int i = 0; i < dsCho.size(); i++) {
            if (dsCho.get(i).getMaPK().equals(maPKMoi)) {
                stt = i + 1;
                break;
            }
        }
        DlgPhieuTiepNhan.hien(javax.swing.SwingUtilities.getWindowAncestor(this),
                dangChon, maPKMoi, stt, gioDen, tenNguoiDung());
    }

    private void datBadge(String text, Color fg, Color bg) {
        lblBadge.setText(text);
        lblBadge.setForeground(fg);
        lblBadge.putClientProperty("pill.bg", bg);
        lblBadge.repaint();
    }

    private void baoLoi(Throwable loi) {
        if (loi instanceof BusinessException) {
            JOptionPane.showMessageDialog(this, loi.getMessage(), "Cảnh báo", JOptionPane.WARNING_MESSAGE);
        } else if (loi instanceof DataAccessException) {
            JOptionPane.showMessageDialog(this, "Không thể tải dữ liệu tiếp nhận.", "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Đã xảy ra lỗi, vui lòng thử lại.", "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Panel co giãn theo chiều ngang viewport để không bị cắt hai bên. */
    private static final class PanelCuon extends JPanel implements javax.swing.Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 120;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static String nhanTrangThai(LichHen lh) {
        return switch (lh.getTrangThai()) {
            case DA_DAT -> "Chờ tiếp nhận";
            case DA_DEN -> "Đã đến";
            case HOAN_TAT -> "Hoàn tất";
            case DA_HUY -> "Đã hủy";
            case KHONG_DEN -> "Không đến";
        };
    }

    private static String tenNguoiDung() {
        TaiKhoan tk = SessionManager.getInstance().getCurrentUser();
        return tk == null || tk.getNhanVien() == null ? "lễ tân" : tk.getNhanVien().getHoTenNV();
    }

    private static final class FieldPanel extends JPanel {

        FieldPanel(java.awt.LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0xF7F9FB));
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
            g2.setColor(new Color(0xE1E8EF));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 2, getHeight() - 2, 8, 8));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static JLabel pill(String text, Color fg, Color bg) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Object mauNen = getClientProperty("pill.bg");
                if (mauNen instanceof Color mau) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    FontMetrics fm = g2.getFontMetrics(getFont());
                    int rong = Math.min(getWidth() - 4, fm.stringWidth(getText()) + 20);
                    g2.setColor(mau);
                    g2.fill(new RoundRectangle2D.Double((getWidth() - rong) / 2.0,
                            (getHeight() - 20) / 2.0, rong, 20, 20, 20));
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        lbl.setOpaque(false);
        lbl.setFont(bold(10));
        lbl.setForeground(fg);
        lbl.putClientProperty("pill.bg", bg);
        lbl.setBorder(new EmptyBorder(4, 10, 4, 10));
        return lbl;
    }
}
