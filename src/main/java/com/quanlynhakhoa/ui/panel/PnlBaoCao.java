package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.ui.component.LineIcon;
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
 * Trang "Báo cáo hoạt động" (mục Báo cáo của Quản lý).
 *
 * <p>Hàng trên là 5 nút Ngày / Tuần / Tháng / Năm / Khoảng, chọn ngày giống trang thống kê lịch hẹn của Nha sĩ:</p>
 * <ul>
 *   <li>Ngày / Tuần / Tháng: chỉ chọn MỘT ngày; Tuần = tuần chứa ngày đó (thứ Hai → CN), Tháng = tháng chứa ngày đó.</li>
 *   <li>Năm: không có ô lịch, xem năm nay.</li>
 *   <li>Khoảng: chọn Từ ngày – Đến ngày.</li>
 * </ul>
 * <p>Mỗi lựa chọn so với ngày trước / tuần trước / tháng trước / năm trước / khoảng cùng số ngày ngay trước đó.
 * Bên dưới là lưới thẻ:</p>
 * <pre>
 *  [Doanh thu đã thu]      [Lịch hẹn hoàn tất]     [Đã hủy / Không đến]
 *  [Biểu đồ doanh thu ..............]  [Kết quả lịch hẹn  ]
 *  [Dịch vụ được sử dụng nhiều ...........................]
 * </pre>
 * Thẻ doanh thu: {@link PnlDoanhThuKy}; thẻ dịch vụ: {@link PnlThongKeDichVu}; 3 thẻ lịch hẹn của Long:
 * {@link PnlThongKeLichHenKy}.
 */
public class PnlBaoCao extends JPanel {

    /**
     * Các lựa chọn xem. {@code ten} dùng cho tiêu đề biểu đồ ("Doanh thu theo tuần"), {@link #truoc()} cho dòng
     * tăng/giảm ("so với tuần trước").
     */
    enum Ky {
        NGAY("ngày"), TUAN("tuần"), THANG("tháng"), NAM("năm"), KHOANG("khoảng");

        final String ten;

        Ky(String ten) {
            this.ten = ten;
        }

        String truoc() {
            return ten + " trước";
        }

        /** "trong tuần", "trong khoảng ngày"... dùng cho thẻ lịch hẹn ("10 lịch hẹn trong tuần"). */
        String trong() {
            return this == KHOANG ? "trong khoảng ngày" : "trong " + ten;
        }

        String tieuDeBieuDo() {
            return this == KHOANG ? "Doanh thu theo khoảng ngày" : "Doanh thu theo " + ten;
        }
    }

    /** Khoảng ngày [tu, den], tính cả 2 đầu. */
    record KhoangNgay(LocalDate tu, LocalDate den) {
        long soNgay() {
            return ChronoUnit.DAYS.between(tu, den) + 1;
        }
    }

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter THANG = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final DateTimeFormatter GIO_NGAY = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy");

    private final PnlThongKeDichVu theDichVu;
    private final PnlDoanhThuKy doanhThu;
    private final PnlThongKeLichHenKy lichHen;
    private final Supplier<LocalDateTime> bayGio;

    final JLabel lblPhuDe = new JLabel();
    final JToggleButton btnNgay = new JToggleButton("Ngày");
    final JToggleButton btnTuan = new JToggleButton("Tuần");
    final JToggleButton btnThang = new JToggleButton("Tháng");
    final JToggleButton btnNam = new JToggleButton("Năm");
    final JToggleButton btnKhoang = new JToggleButton("Khoảng");
    private final ButtonGroup nhomKy = new ButtonGroup();
    /** Ô chọn một ngày (Ngày / Tuần / Tháng). */
    final JDateChooser dcNgay = new JDateChooser();
    /** Hai ô Từ – Đến (Khoảng). */
    final JDateChooser dcTu = new JDateChooser();
    final JDateChooser dcDen = new JDateChooser();
    final JPanel pnlMotNgay = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    final JPanel pnlKhoang = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    // Nút "Xuất báo cáo": chưa làm, bạn khác phụ trách. Khi làm thì mở lại khai báo này và phần trong taoTieuDe().
    // final JButton btnXuatBaoCao = new JButton("Xuất báo cáo");
    /** Lưới chứa các thẻ báo cáo. */
    final JPanel pnlNoiDung = new JPanel(new GridBagLayout());

    // Các ô chứa thẻ lịch hẹn của Long.
    final JPanel oLichHoanTat = new JPanel(new BorderLayout());
    final JPanel oHuyKhongDen = new JPanel(new BorderLayout());
    final JPanel oKetQuaLichHen = new JPanel(new BorderLayout());

    private Ky ky = Ky.TUAN;
    /** true khi code đang tự đặt ngày, để sự kiện đổi ngày không tải lại nhiều lần. */
    private boolean dangDatNgay;

    public PnlBaoCao() {
        this(new PnlThongKeDichVu(), new PnlDoanhThuKy(), new PnlThongKeLichHenKy(), LocalDateTime::now);
    }

    PnlBaoCao(PnlThongKeDichVu theDichVu, PnlDoanhThuKy doanhThu, Supplier<LocalDateTime> bayGio) {
        this(theDichVu, doanhThu, new PnlThongKeLichHenKy(PnlThongKeLichHenKy.NGUON_RONG), bayGio);
    }

    PnlBaoCao(PnlThongKeDichVu theDichVu, PnlDoanhThuKy doanhThu, PnlThongKeLichHenKy lichHen,
            Supplier<LocalDateTime> bayGio) {
        super(new BorderLayout(0, 16));
        this.theDichVu = Objects.requireNonNull(theDichVu);
        this.doanhThu = Objects.requireNonNull(doanhThu);
        this.lichHen = Objects.requireNonNull(lichHen);
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
        datNgayVaKy(homNay(), Ky.TUAN); // mặc định xem tuần này
    }

    // ===================== Dựng giao diện =====================

    /** Tiêu đề + dòng mô tả đang xem gì. */
    private JPanel taoTieuDe() {
        JPanel dong = new JPanel(new BorderLayout(12, 0));
        dong.setOpaque(false);
        dong.add(taoChuTieuDe(), BorderLayout.CENTER);
        // Nút "Xuất báo cáo" (chưa làm):
        // btnXuatBaoCao.setName("xuatBaoCao");
        // btnXuatBaoCao.setFont(bold(13));
        // btnXuatBaoCao.setPreferredSize(new Dimension(150, 38));
        // JPanel phai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        // phai.setOpaque(false);
        // phai.add(btnXuatBaoCao);
        // dong.add(phai, BorderLayout.EAST);
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

    /** 5 nút chọn, rồi ô một ngày hoặc ô Từ – Đến tùy nút (Năm thì không có ô nào). */
    private JPanel taoHangChonKy() {
        JPanel hang = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        hang.setOpaque(false);
        JToggleButton[] nut = {btnNgay, btnTuan, btnThang, btnNam, btnKhoang};
        Ky[] kyCuaNut = {Ky.NGAY, Ky.TUAN, Ky.THANG, Ky.NAM, Ky.KHOANG};
        String[] ten = {"kyNgay", "kyTuan", "kyThang", "kyNam", "kyKhoang"};
        for (int i = 0; i < nut.length; i++) {
            Ky k = kyCuaNut[i];
            JToggleButton b = nut[i];
            b.setName(ten[i]);
            b.setFont(bold(13));
            b.setFocusPainted(false);
            b.setPreferredSize(new Dimension(80, 38));
            // FlatLaf tự tô nút đang chọn bằng màu riêng, nên phải đặt lại về teal của nhóm.
            b.putClientProperty("FlatLaf.style", "selectedBackground: #008991; selectedForeground: #FFFFFF");
            b.addActionListener(e -> chonKy(k));
            nhomKy.add(b);
            hang.add(b);
        }
        hang.add(Box.createHorizontalStrut(4));

        pnlMotNgay.setOpaque(false);
        pnlMotNgay.add(new JLabel(new LineIcon(LineIcon.Type.CALENDAR, TEXT, 16)));
        cauHinhNgay(dcNgay, "ngayBaoCao", "Chọn ngày");
        pnlMotNgay.add(dcNgay);
        hang.add(pnlMotNgay);

        pnlKhoang.setOpaque(false);
        pnlKhoang.add(new JLabel(new LineIcon(LineIcon.Type.CALENDAR, TEXT, 16)));
        cauHinhNgay(dcTu, "tuNgayBaoCao", "Từ ngày");
        pnlKhoang.add(dcTu);
        JLabel den = new JLabel("đến");
        den.setFont(font(13));
        den.setForeground(MUTED);
        pnlKhoang.add(den);
        cauHinhNgay(dcDen, "denNgayBaoCao", "Đến ngày");
        pnlKhoang.add(dcDen);
        hang.add(pnlKhoang);
        return hang;
    }

    private void cauHinhNgay(JDateChooser dc, String ten, String goiY) {
        dc.setName(ten);
        dc.setLocale(Locale.forLanguageTag("vi-VN"));
        dc.setDateFormatString("dd/MM/yyyy");
        dc.setPreferredSize(new Dimension(135, 38));
        dc.setFont(font(13));
        dc.setToolTipText(goiY);
        dc.addPropertyChangeListener("date", e -> {
            if (!dangDatNgay) {
                taiDuLieu(); // người dùng tự chọn ngày trên lịch
            }
        });
    }

    /**
     * Lưới thẻ: hàng 1 là 3 thẻ chỉ số chia đều; hàng 2 chia trái : phải = 2 : 1 (biểu đồ doanh thu và kết quả
     * lịch hẹn, cao bằng nhau); hàng 3 là thẻ dịch vụ trải hết chiều ngang.
     */
    private JScrollPane taoNoiDung() {
        pnlNoiDung.setName("noiDungBaoCao");
        pnlNoiDung.setOpaque(false);
        datO(oLichHoanTat, lichHen.theHoanTat);
        datO(oHuyKhongDen, lichHen.theHuyKhongDen);
        datO(oKetQuaLichHen, lichHen.theKetQua);

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

        g.gridx = 0;
        g.gridy = 2;
        g.gridwidth = 2;
        g.weightx = 1;
        g.insets = new Insets(0, 0, 16, 0);
        pnlNoiDung.add(boc(theDichVu), g);

        g.gridy = 3;
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

    /** Đặt thẻ vào ô (xóa thẻ cũ). */
    static void datO(JPanel o, JComponent the) {
        o.setOpaque(false);
        o.removeAll();
        o.add(the, BorderLayout.CENTER);
        o.revalidate();
        o.repaint();
    }

    /**
     * Thêm thẻ vào cột trái (x = 0) hoặc phải (x = 1) của hàng y, chia bề ngang theo tỷ lệ {@code tyLe}
     * (trái 2, phải 1), không phụ thuộc nội dung thẻ.
     */
    private void themCot(JComponent the, int x, int y, int tyLe, GridBagConstraints g) {
        g.gridx = x;
        g.gridy = y;
        g.weightx = tyLe;
        g.weighty = 0;
        g.insets = new Insets(0, x == 0 ? 0 : 8, 16, x == 0 ? 8 : 0); // khe giữa 2 cột 16px
        pnlNoiDung.add(boc(the), g);
    }

    /** Bọc thẻ trong khung có chiều rộng ưu tiên 0 để GridBagLayout chia bề ngang theo trọng số. */
    private static JPanel boc(JComponent the) {
        JPanel khung = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(0, super.getPreferredSize().height);
            }

            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
        };
        khung.setOpaque(false);
        khung.add(the, BorderLayout.CENTER);
        return khung;
    }

    // ===================== Chọn ngày / tuần / tháng / năm / khoảng =====================

    /** Đặt ngày và lựa chọn (không phát sự kiện tự chọn) rồi tải một lần. */
    private void datNgayVaKy(LocalDate ngay, Ky kyMoi) {
        dangDatNgay = true;
        try {
            dcNgay.setDate(sangDate(ngay));
            ky = kyMoi;
            veNutKy();
        } finally {
            dangDatNgay = false;
        }
        taiDuLieu();
    }

    /** Bấm nút: sang Khoảng thì điền sẵn khoảng đang xem; từ Khoảng quay lại thì lấy ngày bắt đầu. */
    void chonKy(Ky kyMoi) {
        KhoangNgay hienTai = khoangHienTai();
        dangDatNgay = true;
        try {
            if (kyMoi == Ky.KHOANG && ky != Ky.KHOANG && hienTai != null) {
                dcTu.setDate(sangDate(hienTai.tu()));
                dcDen.setDate(sangDate(hienTai.den()));
            } else if (ky == Ky.KHOANG && kyMoi != Ky.KHOANG && dcTu.getDate() != null) {
                dcNgay.setDate(dcTu.getDate());
            }
            ky = kyMoi;
            veNutKy();
        } finally {
            dangDatNgay = false;
        }
        taiDuLieu();
    }

    private void veNutKy() {
        JToggleButton[] nut = {btnNgay, btnTuan, btnThang, btnNam, btnKhoang};
        for (int i = 0; i < nut.length; i++) {
            nut[i].setSelected(ky == Ky.values()[i]);
            nut[i].setBackground(nut[i].isSelected() ? TEAL : SURFACE);
            nut[i].setForeground(nut[i].isSelected() ? Color.WHITE : TEXT);
        }
        pnlMotNgay.setVisible(ky == Ky.NGAY || ky == Ky.TUAN || ky == Ky.THANG);
        pnlKhoang.setVisible(ky == Ky.KHOANG);
        revalidate();
        repaint();
    }

    /** Khoảng đang xem theo nút và ô lịch; {@code null} nếu chưa chọn đủ ngày. */
    private KhoangNgay khoangHienTai() {
        if (ky == Ky.KHOANG) {
            LocalDate tu = sangLocalDate(dcTu.getDate());
            LocalDate den = sangLocalDate(dcDen.getDate());
            return tu == null || den == null ? null : new KhoangNgay(tu, den);
        }
        LocalDate ngay = ky == Ky.NAM ? homNay() : sangLocalDate(dcNgay.getDate());
        return ngay == null ? null : khoangCua(ky, ngay);
    }

    /**
     * Khoảng của Ngày / Tuần / Tháng / Năm chứa {@code ngay}: Ngày = đúng ngày đó; Tuần = thứ Hai → CN;
     * Tháng = ngày 1 → cuối tháng; Năm = 01/01 → 31/12.
     */
    static KhoangNgay khoangCua(Ky ky, LocalDate ngay) {
        return switch (ky) {
            case NGAY, KHOANG -> new KhoangNgay(ngay, ngay);
            case TUAN -> new KhoangNgay(ngay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    ngay.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
            case THANG -> new KhoangNgay(ngay.withDayOfMonth(1), ngay.with(TemporalAdjusters.lastDayOfMonth()));
            case NAM -> new KhoangNgay(ngay.withDayOfYear(1), ngay.with(TemporalAdjusters.lastDayOfYear()));
        };
    }

    /**
     * Khoảng để so sánh: Ngày -> ngày trước; Tuần -> cùng các thứ của tuần trước; Tháng -> cùng các ngày của
     * tháng trước (đang xem tới cuối tháng thì lấy tới cuối tháng trước); Năm -> cùng khoảng năm trước;
     * Khoảng -> khoảng cùng số ngày ngay trước đó.
     */
    static KhoangNgay kyTruoc(Ky ky, KhoangNgay k) {
        return switch (ky) {
            case NGAY -> new KhoangNgay(k.tu().minusDays(1), k.den().minusDays(1));
            case TUAN -> new KhoangNgay(k.tu().minusWeeks(1), k.den().minusWeeks(1));
            case THANG -> {
                LocalDate tu = k.tu().minusMonths(1);
                boolean cuoiThang = k.den().equals(k.den().with(TemporalAdjusters.lastDayOfMonth()));
                yield new KhoangNgay(tu, cuoiThang ? tu.with(TemporalAdjusters.lastDayOfMonth())
                        : k.den().minusMonths(1));
            }
            case NAM -> new KhoangNgay(k.tu().minusYears(1), k.den().minusYears(1));
            case KHOANG -> new KhoangNgay(k.tu().minusDays(k.soNgay()), k.tu().minusDays(1));
        };
    }

    /** Dòng dưới tiêu đề: "Báo cáo tuần 02/11 – 08/11/2026, so với tuần trước · Dữ liệu đến ...". */
    static String moTa(Ky ky, KhoangNgay k) {
        String dangXem = switch (ky) {
            case NGAY -> "Báo cáo ngày " + k.tu().format(NGAY);
            case TUAN -> "Báo cáo tuần " + PnlDoanhThuKy.nhanKhoang(k);
            case THANG -> "Báo cáo tháng " + k.tu().format(THANG);
            case NAM -> "Báo cáo năm " + k.tu().getYear();
            case KHOANG -> "Báo cáo từ " + k.tu().format(NGAY) + " đến " + k.den().format(NGAY);
        };
        return dangXem + ", so với " + ky.truoc();
    }

    // ===================== Tải dữ liệu =====================

    /** Mục Báo cáo trên thanh nav gọi mỗi lần mở trang: tải lại đúng lựa chọn đang xem. */
    public void lamMoi() {
        taiDuLieu();
    }

    /** Tính khoảng đang xem, cập nhật dòng mô tả rồi giao cho từng thẻ tải. */
    private void taiDuLieu() {
        KhoangNgay nay = khoangHienTai();
        String capNhat = "Dữ liệu đến " + bayGio.get().format(GIO_NGAY);
        String loi = null;
        if (nay == null) {
            loi = ky == Ky.KHOANG ? "Vui lòng chọn đủ ngày bắt đầu và ngày kết thúc." : "Vui lòng chọn ngày xem.";
        } else if (nay.tu().isAfter(nay.den())) {
            loi = "Khoảng thời gian không hợp lệ: ngày bắt đầu phải trước hoặc bằng ngày kết thúc.";
        }
        if (loi != null) {
            lblPhuDe.setText(capNhat);
            doanhThu.baoLoi(loi);
            lichHen.baoLoi(loi);
            if (nay != null) {
                theDichVu.taiDuLieu(nay.tu(), nay.den()); // thẻ tự báo khoảng ngày sai
            }
            return;
        }
        KhoangNgay truoc = kyTruoc(ky, nay);
        lblPhuDe.setText(moTa(ky, nay) + " · " + capNhat);
        doanhThu.taiDuLieu(nay, truoc, ky);
        lichHen.taiDuLieu(nay, truoc, ky);
        theDichVu.taiDuLieu(nay.tu(), nay.den());
    }

    private LocalDate homNay() {
        return bayGio.get().toLocalDate();
    }

    private static Date sangDate(LocalDate ngay) {
        return Date.from(ngay.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate sangLocalDate(Date ngay) {
        return ngay == null ? null : ngay.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
