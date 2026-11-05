package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import com.toedter.calendar.JDateChooser;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Trang "Báo cáo hoạt động" (mục Báo cáo của Quản lý), bố cục theo ảnh thiết kế của nhóm.
 *
 * <p>Hàng trên: chọn kỳ Ngày / Tuần / Tháng / Năm hoặc tự chọn khoảng ngày; bên phải ghi kỳ trước dùng để
 * so sánh. Bên dưới là lưới thẻ giống ảnh:</p>
 * <pre>
 *  [Doanh thu đã thu]      [Lịch hẹn hoàn tất]     [Đã hủy / Không đến]
 *  [Doanh thu theo ngày ............]  [Kết quả lịch hẹn  ]
 *  [Dịch vụ được sử dụng nhiều .....]  [Chi tiết doanh thu]
 * </pre>
 *
 * <ul>
 *   <li>Có dữ liệu: 3 thẻ doanh thu ({@link PnlDoanhThuKy}) và thẻ dịch vụ ({@link PnlThongKeDichVu}).</li>
 *   <li>Mới dựng khung (hiện "—"): thẻ lịch hẹn hoàn tất, đã hủy / không đến, kết quả lịch hẹn — phần của Long.
 *       Long thay nội dung các ô {@code oLichHoanTat}, {@code oHuyKhongDen}, {@code oKetQuaLichHen} bằng thẻ có
 *       dữ liệu (dùng {@link #datO}), rồi gọi thẻ đó trong {@link #taiDuLieu()}.</li>
 *   <li>Nút "Xuất báo cáo": bạn khác làm, hiện để tắt. Ô "Chi nhánh": 1 chi nhánh nên cố định.</li>
 * </ul>
 */
public class PnlBaoCao extends JPanel {

    /** Các kỳ có sẵn; TUY_CHINH khi người dùng tự đổi ngày trên lịch. */
    enum Ky { NGAY, TUAN, THANG, NAM, TUY_CHINH }

    /** Khoảng ngày [tu, den], tính cả 2 đầu. */
    record KhoangNgay(LocalDate tu, LocalDate den) {
        long soNgay() {
            return ChronoUnit.DAYS.between(tu, den) + 1;
        }
    }

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter NGAY_NGAN = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter GIO_NGAY = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy");

    private final PnlThongKeDichVu theDichVu;
    private final PnlDoanhThuKy doanhThu;
    private final Supplier<LocalDateTime> bayGio;

    final JLabel lblPhuDe = new JLabel();
    final JToggleButton btnNgay = new JToggleButton("Ngày");
    final JToggleButton btnTuan = new JToggleButton("Tuần");
    final JToggleButton btnThang = new JToggleButton("Tháng");
    final JToggleButton btnNam = new JToggleButton("Năm");
    private final ButtonGroup nhomKy = new ButtonGroup();
    final JDateChooser dcTu = new JDateChooser();
    final JDateChooser dcDen = new JDateChooser();
    final JLabel lblSoSanh = new JLabel(" ");
    final JButton btnXuatBaoCao = new JButton("Xuất báo cáo");
    /** Lưới chứa các thẻ báo cáo. */
    final JPanel pnlNoiDung = new JPanel(new GridBagLayout());

    // Các ô đang chứa thẻ khung của phần lịch hẹn (chưa có dữ liệu); Long thay nội dung khi làm xong.
    final JPanel oLichHoanTat = new JPanel(new BorderLayout());
    final JPanel oHuyKhongDen = new JPanel(new BorderLayout());
    final JPanel oKetQuaLichHen = new JPanel(new BorderLayout());

    private static final Color VANG = new Color(0x9A6A12);
    private static final Color DO = new Color(0xC94C48);
    private static final String CHUA_CO = "—";

    private Ky ky = Ky.TUAN;
    /** true khi code đang tự đặt ngày, để sự kiện đổi ngày không coi là người dùng tự chọn. */
    private boolean dangDatNgay;

    public PnlBaoCao() {
        this(new PnlThongKeDichVu(), new PnlDoanhThuKy(), LocalDateTime::now);
    }

    PnlBaoCao(PnlThongKeDichVu theDichVu, PnlDoanhThuKy doanhThu, Supplier<LocalDateTime> bayGio) {
        super(new BorderLayout(0, 16));
        this.theDichVu = Objects.requireNonNull(theDichVu);
        this.doanhThu = Objects.requireNonNull(doanhThu);
        this.bayGio = Objects.requireNonNull(bayGio);
        setName("Báo cáo");
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 24, 24));
        JPanel tren = new JPanel(new BorderLayout(0, 16));
        tren.setOpaque(false);
        tren.add(taoTieuDe(), BorderLayout.NORTH);
        tren.add(taoHangChonKy(), BorderLayout.CENTER);
        add(tren, BorderLayout.NORTH);
        add(taoNoiDung(), BorderLayout.CENTER);
        chonKy(Ky.TUAN); // mặc định "Tuần" như ảnh
    }

    // ===================== Dựng giao diện =====================

    /** Tiêu đề + phụ đề bên trái, nút "Xuất báo cáo" bên phải. */
    private JPanel taoTieuDe() {
        JPanel dong = new JPanel(new BorderLayout(12, 0));
        dong.setOpaque(false);
        dong.add(taoChuTieuDe(), BorderLayout.CENTER);
        btnXuatBaoCao.setName("xuatBaoCao");
        btnXuatBaoCao.setFont(bold(13));
        btnXuatBaoCao.setFocusPainted(false);
        btnXuatBaoCao.setIcon(bieuTuongTaiXuong());
        btnXuatBaoCao.setPreferredSize(new Dimension(150, 38));
        btnXuatBaoCao.setEnabled(false); // chưa có: làm cùng báo cáo doanh thu
        btnXuatBaoCao.setToolTipText("Chức năng đang được hoàn thiện");
        JPanel phai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        phai.setOpaque(false);
        phai.add(btnXuatBaoCao);
        dong.add(phai, BorderLayout.EAST);
        return dong;
    }

    private JPanel taoChuTieuDe() {
        JPanel chu = new JPanel();
        chu.setOpaque(false);
        chu.setLayout(new BoxLayout(chu, BoxLayout.Y_AXIS));
        JLabel tieuDe = new JLabel("Báo cáo hoạt động");
        tieuDe.setFont(bold(24));
        tieuDe.setForeground(TEXT);
        lblPhuDe.setName("phuDeBaoCao");
        lblPhuDe.setFont(font(13));
        lblPhuDe.setForeground(MUTED);
        chu.add(tieuDe);
        chu.add(Box.createVerticalStrut(4));
        chu.add(lblPhuDe);
        return chu;
    }

    /** Nút Ngày / Tuần / Tháng, ô khoảng ngày; bên phải là kỳ so sánh. */
    private JPanel taoHangChonKy() {
        JPanel hang = new JPanel(new BorderLayout(12, 0));
        hang.setOpaque(false);
        JPanel trai = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        trai.setOpaque(false);
        btnNgay.setName("kyNgay");
        btnTuan.setName("kyTuan");
        btnThang.setName("kyThang");
        btnNam.setName("kyNam");
        Ky[] kyCuaNut = {Ky.NGAY, Ky.TUAN, Ky.THANG, Ky.NAM};
        JToggleButton[] nut = {btnNgay, btnTuan, btnThang, btnNam};
        for (int i = 0; i < nut.length; i++) {
            Ky k = kyCuaNut[i];
            JToggleButton b = nut[i];
            b.setFont(bold(13));
            b.setFocusPainted(false);
            b.setPreferredSize(new Dimension(70, 38));
            // FlatLaf tự tô nút đang chọn bằng màu riêng, nên phải đặt lại về teal của nhóm.
            b.putClientProperty("FlatLaf.style", "selectedBackground: #008991; selectedForeground: #FFFFFF");
            b.addActionListener(e -> chonKy(k));
            nhomKy.add(b);
            trai.add(b);
        }
        cauHinhNgay(dcTu, "tuNgayBaoCao", "Từ ngày");
        cauHinhNgay(dcDen, "denNgayBaoCao", "Đến ngày");
        JLabel gach = new JLabel("–");
        gach.setForeground(MUTED);
        JLabel lich = new JLabel(new LineIcon(LineIcon.Type.CALENDAR, TEXT, 16));
        trai.add(Box.createHorizontalStrut(4));
        trai.add(lich);
        trai.add(dcTu);
        trai.add(gach);
        trai.add(dcDen);
        hang.add(trai, BorderLayout.CENTER);

        // Bên phải: "So sánh: ..." và ô chi nhánh (cố định, phòng khám chỉ có 1 chi nhánh).
        JPanel phai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        phai.setOpaque(false);
        lblSoSanh.setName("soSanhKy");
        lblSoSanh.setFont(font(12));
        lblSoSanh.setForeground(MUTED);
        JComboBox<String> cboChiNhanh = new JComboBox<>(new String[]{"Chi nhánh Quận 3"});
        cboChiNhanh.setName("chiNhanhBaoCao");
        cboChiNhanh.setFont(bold(13));
        cboChiNhanh.setPreferredSize(new Dimension(170, 38));
        cboChiNhanh.setEnabled(false);
        phai.add(lblSoSanh);
        phai.add(cboChiNhanh);
        hang.add(phai, BorderLayout.EAST);
        return hang;
    }

    private void cauHinhNgay(JDateChooser dc, String ten, String goiY) {
        dc.setName(ten);
        dc.setLocale(Locale.forLanguageTag("vi-VN"));
        dc.setDateFormatString("dd/MM/yyyy");
        dc.setPreferredSize(new Dimension(135, 38));
        dc.setFont(font(13));
        dc.setToolTipText(goiY);
        // Người dùng tự chọn ngày trên lịch -> chuyển sang kỳ "tùy chỉnh" và tải lại.
        dc.addPropertyChangeListener("date", e -> {
            if (!dangDatNgay) {
                ky = Ky.TUY_CHINH;
                nhomKy.clearSelection();
                veNutKy();
                taiDuLieu();
            }
        });
    }

    /**
     * Lưới thẻ như ảnh: hàng 1 là 3 thẻ chỉ số chia đều; hàng 2 và 3 chia cột trái : phải = 2 : 1,
     * hai thẻ cùng hàng cao bằng nhau.
     */
    private JScrollPane taoNoiDung() {
        pnlNoiDung.setName("noiDungBaoCao");
        pnlNoiDung.setOpaque(false);
        datO(oLichHoanTat, theChiSo("Lịch hẹn hoàn tất", LineIcon.Type.CHECK));
        datO(oHuyKhongDen, theChiSo("Đã hủy / Không đến", LineIcon.Type.CALENDAR));
        datO(oKetQuaLichHen, theKetQuaLichHen());

        JPanel hangChiSo = new JPanel(new GridLayout(1, 3, 16, 0));
        hangChiSo.setOpaque(false);
        hangChiSo.add(doanhThu.theChiSo);
        hangChiSo.add(oLichHoanTat);
        hangChiSo.add(oHuyKhongDen);

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.anchor = GridBagConstraints.NORTH;
        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;
        g.weightx = 1;
        g.insets = new Insets(0, 0, 16, 0);
        pnlNoiDung.add(hangChiSo, g);

        g.gridwidth = 1;
        themCot(doanhThu.theBieuDo, 0, 1, 2, g);
        themCot(oKetQuaLichHen, 1, 1, 1, g);
        themCot(theDichVu, 0, 2, 2, g);
        themCot(doanhThu.theChiTiet, 1, 2, 1, g);

        g.gridx = 0;
        g.gridy = 3;
        g.gridwidth = 2;
        g.weighty = 1; // đẩy các thẻ lên trên khi trang còn trống phía dưới
        g.insets = new Insets(0, 0, 0, 0);
        pnlNoiDung.add(Box.createGlue(), g);
        JScrollPane cuon = new JScrollPane(pnlNoiDung);
        cuon.setBorder(null);
        cuon.setOpaque(false);
        cuon.getViewport().setOpaque(false);
        cuon.getVerticalScrollBar().setUnitIncrement(16);
        return cuon;
    }

    /** Đặt thẻ vào ô (xóa thẻ cũ). Dùng cả khi thay thẻ khung bằng thẻ có dữ liệu. */
    static void datO(JPanel o, JComponent the) {
        o.setOpaque(false);
        o.removeAll();
        o.add(the, BorderLayout.CENTER);
        o.revalidate();
        o.repaint();
    }

    /**
     * Thêm thẻ vào cột trái (x = 0) hoặc phải (x = 1) của hàng y. Bọc thẻ trong khung có chiều rộng ưu tiên 0
     * để GridBagLayout chia toàn bộ bề ngang theo tỷ lệ {@code tyLe} (trái 2, phải 1), không phụ thuộc nội dung thẻ.
     */
    private void themCot(JComponent the, int x, int y, int tyLe, GridBagConstraints g) {
        JPanel cot = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(0, super.getPreferredSize().height);
            }

            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
        };
        cot.setOpaque(false);
        cot.add(the, BorderLayout.CENTER);
        g.gridx = x;
        g.gridy = y;
        g.weightx = tyLe;
        g.weighty = 0;
        g.insets = new Insets(0, x == 0 ? 0 : 8, 16, x == 0 ? 8 : 0); // khe giữa 2 cột 16px
        pnlNoiDung.add(cot, g);
    }

    /** Khung thẻ lớn: tiêu đề (và chữ nhỏ bên phải nếu có) ở trên, nội dung bên dưới. */
    private static JPanel theKhung(String tieuDe, String ghiChuGoc, JComponent noiDung) {
        SurfacePanel the = new SurfacePanel(new BorderLayout(0, 12));
        the.setBorder(new EmptyBorder(16, 18, 16, 18));
        JPanel dong = new JPanel(new BorderLayout(12, 0));
        dong.setOpaque(false);
        JLabel lblTieuDe = new JLabel(tieuDe);
        lblTieuDe.setFont(bold(16));
        lblTieuDe.setForeground(TEXT);
        dong.add(lblTieuDe, BorderLayout.WEST);
        if (ghiChuGoc != null) {
            dong.add(chu(ghiChuGoc, font(11), MUTED), BorderLayout.EAST);
        }
        the.add(dong, BorderLayout.NORTH);
        the.add(noiDung, BorderLayout.CENTER);
        return the;
    }

    /** Thẻ chỉ số nhỏ ở hàng đầu (tiêu đề + biểu tượng, số lớn, dòng so sánh). Chưa có dữ liệu nên hiện "—". */
    private static JPanel theChiSo(String tieuDe, LineIcon.Type bieuTuong) {
        SurfacePanel the = new SurfacePanel(new BorderLayout(0, 8));
        the.setBorder(new EmptyBorder(14, 16, 14, 16));
        JPanel dong = new JPanel(new BorderLayout());
        dong.setOpaque(false);
        dong.add(chu(tieuDe, font(12), MUTED), BorderLayout.WEST);
        dong.add(new JLabel(new LineIcon(bieuTuong, TEAL, 18)), BorderLayout.EAST);
        the.add(dong, BorderLayout.NORTH);
        the.add(chu(CHUA_CO, bold(26), TEXT), BorderLayout.CENTER);
        the.add(chu("Chưa có dữ liệu", font(11), MUTED), BorderLayout.SOUTH);
        return the;
    }

    /** "Kết quả lịch hẹn": thanh tỷ lệ để xám, 3 dòng chú thích hiện "—". */
    private static JPanel theKetQuaLichHen() {
        JPanel noiDung = new JPanel();
        noiDung.setOpaque(false);
        noiDung.setLayout(new BoxLayout(noiDung, BoxLayout.Y_AXIS));
        JLabel tong = chu(CHUA_CO + " lịch đã kết thúc", font(12), MUTED);
        tong.setAlignmentX(LEFT_ALIGNMENT);
        noiDung.add(tong);
        noiDung.add(Box.createVerticalStrut(12));
        JPanel thanh = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BORDER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                g2.dispose();
            }
        };
        thanh.setOpaque(false);
        thanh.setAlignmentX(LEFT_ALIGNMENT);
        thanh.setPreferredSize(new Dimension(0, 16));
        thanh.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        noiDung.add(thanh);
        noiDung.add(Box.createVerticalStrut(8));
        String[] ten = {"Hoàn tất", "Đã hủy", "Không đến"};
        Color[] mau = {TEAL, VANG, DO};
        for (int i = 0; i < ten.length; i++) {
            JPanel dong = new JPanel(new BorderLayout(8, 0));
            dong.setOpaque(false);
            dong.setAlignmentX(LEFT_ALIGNMENT);
            dong.setBorder(new EmptyBorder(6, 0, 6, 0));
            dong.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            JLabel nhan = chu(ten[i], font(12), MUTED);
            nhan.setIcon(oVuong(mau[i]));
            nhan.setIconTextGap(8);
            dong.add(nhan, BorderLayout.WEST);
            dong.add(chu(CHUA_CO, bold(12), TEXT), BorderLayout.EAST);
            noiDung.add(dong);
        }
        return theKhung("Kết quả lịch hẹn", null, noiDung);
    }


    private static JLabel chu(String text, Font f, Color mau) {
        JLabel l = new JLabel(text);
        l.setFont(f);
        l.setForeground(mau);
        return l;
    }

    /** Biểu tượng tải xuống 16px (mũi tên xuống + khay) cho nút "Xuất báo cáo". */
    private static Icon bieuTuongTaiXuong() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(c.isEnabled() ? TEXT : MUTED);
                g2.translate(x, y);
                g2.drawLine(8, 2, 8, 10);
                g2.drawLine(4, 7, 8, 11);
                g2.drawLine(12, 7, 8, 11);
                g2.drawLine(2, 11, 2, 14);
                g2.drawLine(2, 14, 14, 14);
                g2.drawLine(14, 14, 14, 11);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }
        };
    }

    /** Ô vuông màu 8px dùng làm chú thích. */
    private static Icon oVuong(Color mau) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(mau);
                g.fillRect(x, y, 8, 8);
            }

            @Override
            public int getIconWidth() {
                return 8;
            }

            @Override
            public int getIconHeight() {
                return 8;
            }
        };
    }

    // ===================== Chọn kỳ =====================

    /** Đặt kỳ, điền ngày tương ứng lên 2 ô lịch rồi tải lại các thẻ. */
    void chonKy(Ky kyMoi) {
        ky = kyMoi;
        KhoangNgay k = khoangCuaKy(kyMoi, bayGio.get().toLocalDate());
        dangDatNgay = true;
        try {
            dcTu.setDate(sangDate(k.tu()));
            dcDen.setDate(sangDate(k.den()));
        } finally {
            dangDatNgay = false;
        }
        veNutKy();
        taiDuLieu();
    }

    /**
     * Khoảng ngày của kỳ tính tới hôm nay: Ngày = hôm nay; Tuần = từ thứ Hai tuần này; Tháng = từ ngày 1;
     * Năm = từ 01/01. (Như ảnh: chọn Tuần ngày 16/10/2026 thứ Sáu -> 12/10 – 16/10/2026.)
     */
    static KhoangNgay khoangCuaKy(Ky ky, LocalDate homNay) {
        return switch (ky) {
            case NGAY, TUY_CHINH -> new KhoangNgay(homNay, homNay);
            case TUAN -> new KhoangNgay(homNay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), homNay);
            case THANG -> new KhoangNgay(homNay.withDayOfMonth(1), homNay);
            case NAM -> new KhoangNgay(homNay.withDayOfYear(1), homNay);
        };
    }

    /**
     * Kỳ trước để so sánh: Ngày -> hôm qua; Tuần -> cùng các thứ của tuần trước; Tháng -> cùng ngày của tháng
     * trước; Năm -> cùng khoảng của năm trước; tự chọn -> khoảng cùng số ngày ngay trước đó.
     * (Ảnh: 12/10–16/10 so với 05/10–09/10.)
     */
    static KhoangNgay kyTruoc(Ky ky, KhoangNgay k) {
        return switch (ky) {
            case NGAY -> new KhoangNgay(k.tu().minusDays(1), k.den().minusDays(1));
            case TUAN -> new KhoangNgay(k.tu().minusWeeks(1), k.den().minusWeeks(1));
            case THANG -> new KhoangNgay(k.tu().minusMonths(1), k.den().minusMonths(1));
            case NAM -> new KhoangNgay(k.tu().minusYears(1), k.den().minusYears(1));
            case TUY_CHINH -> new KhoangNgay(k.tu().minusDays(k.soNgay()), k.tu().minusDays(1));
        };
    }

    private void veNutKy() {
        btnNgay.setSelected(ky == Ky.NGAY);
        btnTuan.setSelected(ky == Ky.TUAN);
        btnThang.setSelected(ky == Ky.THANG);
        btnNam.setSelected(ky == Ky.NAM);
        for (JToggleButton b : new JToggleButton[]{btnNgay, btnTuan, btnThang, btnNam}) {
            b.setBackground(b.isSelected() ? TEAL : SURFACE);
            b.setForeground(b.isSelected() ? Color.WHITE : TEXT);
        }
    }

    // ===================== Tải dữ liệu =====================

    /** Mục Báo cáo trên thanh nav gọi mỗi lần mở trang. */
    public void lamMoi() {
        if (ky == Ky.TUY_CHINH) {
            taiDuLieu(); // giữ khoảng ngày người dùng đã chọn
        } else {
            chonKy(ky); // tính lại theo ngày hiện tại (qua nửa đêm thì kỳ đổi)
        }
    }

    /** Đọc khoảng ngày trên 2 ô lịch, cập nhật dòng phụ đề + so sánh, rồi giao cho từng thẻ tải. */
    private void taiDuLieu() {
        LocalDate tu = sangLocalDate(dcTu.getDate());
        LocalDate den = sangLocalDate(dcDen.getDate());
        lblPhuDe.setText("Doanh thu đã thu và hiệu quả vận hành · Dữ liệu đến " + bayGio.get().format(GIO_NGAY));
        if (tu == null || den == null || tu.isAfter(den)) {
            lblSoSanh.setText(" ");
            doanhThu.baoLoi(tu == null || den == null ? "Vui lòng chọn đủ ngày bắt đầu và ngày kết thúc."
                    : "Khoảng thời gian không hợp lệ: ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        } else {
            KhoangNgay nay = new KhoangNgay(tu, den);
            KhoangNgay truoc = kyTruoc(ky, nay);
            lblSoSanh.setText("So sánh: " + truoc.tu().format(NGAY_NGAN) + " – " + truoc.den().format(NGAY));
            doanhThu.taiDuLieu(nay, truoc);
        }
        theDichVu.taiDuLieu(tu, den); // thẻ tự báo lỗi nếu khoảng ngày sai
    }

    private static Date sangDate(LocalDate ngay) {
        return Date.from(ngay.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate sangLocalDate(Date ngay) {
        return ngay == null ? null : ngay.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
