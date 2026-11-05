package com.quanlynhakhoa.ui.panel;

import static com.quanlynhakhoa.ui.component.UiConstants.BACKGROUND;
import static com.quanlynhakhoa.ui.component.UiConstants.BORDER;
import static com.quanlynhakhoa.ui.component.UiConstants.MUTED;
import static com.quanlynhakhoa.ui.component.UiConstants.SURFACE;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL;
import static com.quanlynhakhoa.ui.component.UiConstants.TEXT;
import static com.quanlynhakhoa.ui.component.UiConstants.bold;
import static com.quanlynhakhoa.ui.component.UiConstants.font;

import com.quanlynhakhoa.dao.LichHenDAO;
import com.quanlynhakhoa.dao.ThongKeLichHenDAO;
import com.quanlynhakhoa.dao.ThongKeLichHenDAO.TongHop;
import com.quanlynhakhoa.entity.LichHen;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import com.toedter.calendar.JDateChooser;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ButtonGroup;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * UC034 – Nha sĩ xem số lịch hẹn của chính mình trong một khoảng ngày, theo từng trạng thái (đã đặt, đã đến,
 * hoàn tất, đã hủy, không đến), kèm danh sách các lịch đó.
 *
 * <p>Chọn kỳ Ngày / Tuần / Tháng rồi chỉ cần chọn MỘT ngày trên lịch: Ngày = đúng ngày đó, Tuần = tuần chứa
 * ngày đó (thứ Hai đến Chủ nhật), Tháng = tháng chứa ngày đó. Kỳ Khoảng mới cần chọn Từ và Đến. Mặc định hiện
 * tuần này (gồm cả lịch sắp tới); nút Hôm nay xem riêng hôm nay. Nha sĩ chỉ thấy lịch của mình: quyền do {@link ThongKeLichHenDAO} và {@link LichHenDAO} kiểm.
 * Chỉ đọc, không thay đổi dữ liệu.</p>
 */
public class PnlThongKeLichHenNhaSi extends JPanel {

    static final String KHONG_CO_LICH = "Không có lịch hẹn trong kỳ này";
    static final int COT_GIO = 0, COT_MA = 1, COT_KHACH = 2, COT_DICH_VU = 3, COT_TRANG_THAI = 4;

    private static final Logger LOG = Logger.getLogger(PnlThongKeLichHenNhaSi.class.getName());
    private static final Color DO = new Color(0xC94C48);
    private static final Color XANH_DUONG = new Color(0x2F6FA8);
    private static final Color VANG = new Color(0x9A6A12);
    private static final Color XANH_LA = new Color(0x2E8B57);
    private static final Color XAM = new Color(0x6B7785);
    private static final Color NEN_HEADER = new Color(0xF7F9FB);
    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter GIO_NGAY = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy");
    private static final Locale VI = Locale.forLanguageTag("vi-VN");

    /** Kết quả một lần tải: số liệu tổng hợp và danh sách lịch hẹn của khoảng ngày. */
    record KetQua(TongHop tongHop, List<LichHen> lich) {

    }

    private final BiFunction<LocalDate, LocalDate, TongHop> nguon;
    private final BiFunction<LocalDate, LocalDate, List<LichHen>> nguonDanhSach;
    private final Supplier<LocalDateTime> bayGio;

    final JLabel lblPhuDe = new JLabel(" ");
    /** Kỳ xem; KHOANG là tự chọn Từ – Đến. */
    enum Ky { NGAY, TUAN, THANG, KHOANG }

    final JDateChooser dcNgay = new JDateChooser();
    final JDateChooser dcTu = new JDateChooser();
    final JDateChooser dcDen = new JDateChooser();
    final JButton btnHomNay = new JButton("Hôm nay");
    final JToggleButton btnNgay = new JToggleButton("Ngày");
    final JToggleButton btnTuan = new JToggleButton("Tuần");
    final JToggleButton btnThang = new JToggleButton("Tháng");
    final JToggleButton btnKhoang = new JToggleButton("Khoảng");
    private final ButtonGroup nhomKy = new ButtonGroup();
    private final JPanel pnlMotNgay = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JPanel pnlKhoang = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private Ky ky = Ky.TUAN;
    final JLabel lblTong = new JLabel("—");
    final JLabel lblDaDat = new JLabel("—");
    final JLabel lblDaDen = new JLabel("—");
    final JLabel lblHoanTat = new JLabel("—");
    final JLabel lblDaHuy = new JLabel("—");
    final JLabel lblKhongDen = new JLabel("—");
    final JLabel lblTyLeDaDat = new JLabel(" ");
    final JLabel lblTyLeDaDen = new JLabel(" ");
    final JLabel lblTyLeHoanTat = new JLabel(" ");
    final JLabel lblTyLeDaHuy = new JLabel(" ");
    final JLabel lblTyLeKhongDen = new JLabel(" ");
    final JLabel lblThongBao = new JLabel(" ");
    final DefaultTableModel model = new DefaultTableModel(new String[]{
        "NGÀY GIỜ", "MÃ LỊCH", "KHÁCH HÀNG", "DỊCH VỤ", "TRẠNG THÁI"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    final JTable tblLich = new JTable(model) {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getRowCount() == 0) {
                veThongBaoGiuaBang(this, g); // bảng trống: thông báo nằm ngay trong bảng
            }
        }
    };

    /** true khi code đang tự đặt ngày, để sự kiện đổi ngày không tải lại nhiều lần. */
    private boolean dangDatNgay;
    private SwingWorker<KetQua, Void> worker;

    public PnlThongKeLichHenNhaSi() {
        // nha sĩ truyền maNhaSi = null: DAO tự giới hạn về lịch của chính nha sĩ đang đăng nhập
        this((tu, den) -> new ThongKeLichHenDAO().tongHop(tu, den, null),
                (tu, den) -> new LichHenDAO().timKiem(tu, den, null), LocalDateTime::now);
    }

    PnlThongKeLichHenNhaSi(BiFunction<LocalDate, LocalDate, TongHop> nguon,
            BiFunction<LocalDate, LocalDate, List<LichHen>> nguonDanhSach, Supplier<LocalDateTime> bayGio) {
        super(new BorderLayout(0, 16));
        this.nguon = Objects.requireNonNull(nguon);
        this.nguonDanhSach = Objects.requireNonNull(nguonDanhSach);
        this.bayGio = Objects.requireNonNull(bayGio);
        setName("Thống kê lịch hẹn");
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 24, 24));
        JPanel tren = new JPanel(new BorderLayout(0, 16));
        tren.setOpaque(false);
        tren.add(taoTieuDe(), BorderLayout.NORTH);
        tren.add(taoHangChonNgay(), BorderLayout.CENTER);
        tren.add(taoHangThe(), BorderLayout.SOUTH);
        add(tren, BorderLayout.NORTH);
        add(taoTheBang(), BorderLayout.CENTER);
        datNgayVaKy(bayGio.get().toLocalDate(), Ky.TUAN);
    }

    // ===================== Dựng giao diện =====================

    private JPanel taoTieuDe() {
        JPanel chu = new JPanel();
        chu.setOpaque(false);
        chu.setLayout(new BoxLayout(chu, BoxLayout.Y_AXIS));
        chu.add(nhan("Thống kê lịch hẹn của tôi", bold(24), TEXT));
        chu.add(Box.createVerticalStrut(4));
        lblPhuDe.setName("phuDeThongKeLichNhaSi");
        lblPhuDe.setFont(font(13));
        lblPhuDe.setForeground(MUTED);
        chu.add(lblPhuDe);
        return chu;
    }

    private JPanel taoHangChonNgay() {
        JPanel hang = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        hang.setOpaque(false);
        JToggleButton[] nut = {btnNgay, btnTuan, btnThang, btnKhoang};
        Ky[] kyCuaNut = {Ky.NGAY, Ky.TUAN, Ky.THANG, Ky.KHOANG};
        String[] ten = {"kyNgayNhaSi", "kyTuanNhaSi", "kyThangNhaSi", "kyKhoangNhaSi"};
        for (int i = 0; i < nut.length; i++) {
            Ky k = kyCuaNut[i];
            JToggleButton b = nut[i];
            b.setName(ten[i]);
            b.setFont(bold(13));
            b.setFocusPainted(false);
            b.setPreferredSize(new Dimension(80, 38));
            // FlatLaf tự tô nút đang chọn bằng màu riêng, nên đặt lại về teal của nhóm (như trang Báo cáo).
            b.putClientProperty("FlatLaf.style", "selectedBackground: #008991; selectedForeground: #FFFFFF");
            b.addActionListener(e -> chonKy(k));
            nhomKy.add(b);
            hang.add(b);
        }
        hang.add(Box.createHorizontalStrut(4));

        pnlMotNgay.setOpaque(false);
        pnlMotNgay.add(new JLabel(new LineIcon(LineIcon.Type.CALENDAR, TEXT, 16)));
        cauHinhNgay(dcNgay, "ngayXemNhaSi");
        pnlMotNgay.add(dcNgay);
        hang.add(pnlMotNgay);

        pnlKhoang.setOpaque(false);
        pnlKhoang.add(new JLabel(new LineIcon(LineIcon.Type.CALENDAR, TEXT, 16)));
        cauHinhNgay(dcTu, "tuNgayThongKeNhaSi");
        pnlKhoang.add(dcTu);
        pnlKhoang.add(nhan("đến", font(13), MUTED));
        cauHinhNgay(dcDen, "denNgayThongKeNhaSi");
        pnlKhoang.add(dcDen);
        hang.add(pnlKhoang);

        btnHomNay.setName("xemHomNayNhaSi");
        btnHomNay.setFont(bold(13));
        btnHomNay.setFocusPainted(false);
        btnHomNay.setPreferredSize(new Dimension(100, 38));
        btnHomNay.addActionListener(e -> datNgayVaKy(bayGio.get().toLocalDate(), Ky.NGAY));
        hang.add(btnHomNay);
        return hang;
    }

    private void cauHinhNgay(JDateChooser dc, String ten) {
        dc.setName(ten);
        dc.setLocale(VI);
        dc.setDateFormatString("dd/MM/yyyy");
        dc.setPreferredSize(new Dimension(135, 38));
        dc.setFont(font(13));
        dc.addPropertyChangeListener("date", e -> {
            if (!dangDatNgay) {
                taiDuLieu(); // nha sĩ tự chọn ngày trên lịch
            }
        });
    }

    /** 6 thẻ chia đều một hàng: tổng số lịch rồi từng trạng thái. */
    private JPanel taoHangThe() {
        JPanel hang = new JPanel(new GridLayout(1, 6, 16, 0));
        hang.setOpaque(false);
        lblTong.setName("tongLichHenNhaSi");
        lblDaDat.setName("soDaDatNhaSi");
        lblDaDen.setName("soDaDenNhaSi");
        lblHoanTat.setName("soHoanTatNhaSi");
        lblDaHuy.setName("soDaHuyNhaSi");
        lblKhongDen.setName("soKhongDenNhaSi");
        hang.add(theSo("Tổng số lịch", LineIcon.Type.CALENDAR, lblTong, nhan("Trong kỳ đã chọn", font(11), MUTED),
                TEXT));
        hang.add(theSo("Đã đặt", LineIcon.Type.CLIPBOARD, lblDaDat, lblTyLeDaDat, XANH_DUONG));
        hang.add(theSo("Đã đến", LineIcon.Type.PEOPLE, lblDaDen, lblTyLeDaDen, VANG));
        hang.add(theSo("Hoàn tất", LineIcon.Type.CHECK, lblHoanTat, lblTyLeHoanTat, XANH_LA));
        hang.add(theSo("Đã hủy", LineIcon.Type.CLIPBOARD, lblDaHuy, lblTyLeDaHuy, DO));
        hang.add(theSo("Không đến", LineIcon.Type.CLIPBOARD, lblKhongDen, lblTyLeKhongDen, XAM));
        return hang;
    }

    private static JPanel theSo(String tieuDe, LineIcon.Type bieuTuong, JLabel giaTri, JLabel ghiChu, Color mau) {
        SurfacePanel the = new SurfacePanel(new BorderLayout(0, 8));
        the.setBorder(new EmptyBorder(14, 16, 14, 16));
        JPanel dong = new JPanel(new BorderLayout());
        dong.setOpaque(false);
        dong.add(nhan(tieuDe, font(12), MUTED), BorderLayout.WEST);
        dong.add(new JLabel(new LineIcon(bieuTuong, mau, 18)), BorderLayout.EAST);
        the.add(dong, BorderLayout.NORTH);
        giaTri.setFont(bold(28));
        giaTri.setForeground(mau);
        the.add(giaTri, BorderLayout.CENTER);
        ghiChu.setFont(font(11));
        ghiChu.setForeground(MUTED);
        the.add(ghiChu, BorderLayout.SOUTH);
        return the;
    }

    private JPanel taoTheBang() {
        SurfacePanel the = new SurfacePanel(new BorderLayout(0, 12));
        the.setBorder(new EmptyBorder(16, 18, 16, 18));
        the.add(nhan("Lịch hẹn của tôi", bold(16), TEXT), BorderLayout.NORTH);

        tblLich.setName("bangLichHenNhaSi");
        tblLich.setFont(font(13));
        tblLich.setRowHeight(36);
        tblLich.setShowVerticalLines(false);
        tblLich.setGridColor(BORDER);
        tblLich.setFillsViewportHeight(true);
        tblLich.setFocusable(false);
        tblLich.setRowSelectionAllowed(false);
        tblLich.getTableHeader().setReorderingAllowed(false);
        tblLich.getTableHeader().setPreferredSize(new Dimension(0, 36));
        tblLich.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                setFont(bold(11));
                setForeground(MUTED);
                setBackground(NEN_HEADER);
                setOpaque(true);
                setHorizontalAlignment(LEFT);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(0, 12, 0, 12)));
                return this;
            }
        });
        tblLich.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                setHorizontalAlignment(LEFT);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                setFont(c == COT_TRANG_THAI ? bold(13) : font(13));
                setForeground(c == COT_TRANG_THAI ? mauTrangThai(String.valueOf(v)) : TEXT);
                setToolTipText(v == null ? null : v.toString());
                return this;
            }
        });
        int[] rong = {120, 130, 230, 240, 120};
        for (int i = 0; i < rong.length; i++) {
            tblLich.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        }
        JScrollPane cuon = new JScrollPane(tblLich);
        cuon.setBorder(BorderFactory.createLineBorder(BORDER));
        cuon.getViewport().setBackground(SURFACE);
        cuon.getVerticalScrollBar().setUnitIncrement(16);
        the.add(cuon, BorderLayout.CENTER);
        lblThongBao.setName("thongBaoThongKeLichNhaSi");
        lblThongBao.setFont(font(12));
        lblThongBao.setForeground(MUTED);
        the.add(lblThongBao, BorderLayout.SOUTH);
        return the;
    }

    private static Color mauTrangThai(String nhan) {
        return switch (nhan) {
            case "Đã đặt" -> XANH_DUONG;
            case "Đã đến" -> VANG;
            case "Hoàn tất" -> XANH_LA;
            case "Đã hủy" -> DO;
            default -> XAM;
        };
    }

    // ===================== Khoảng ngày nhanh =====================

    /** Tuần chứa {@code ngay}: thứ Hai đến Chủ nhật. */
    static LocalDate[] tuanCua(LocalDate ngay) {
        return new LocalDate[]{ngay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            ngay.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))};
    }

    /** Tháng chứa {@code ngay}: ngày 1 đến ngày cuối tháng. */
    static LocalDate[] thangCua(LocalDate ngay) {
        return new LocalDate[]{ngay.withDayOfMonth(1), ngay.with(TemporalAdjusters.lastDayOfMonth())};
    }

    // ===================== Tải dữ liệu =====================

    /** Menu Nha sĩ gọi mỗi lần mở trang: tải lại khoảng ngày đang xem. */
    public void lamMoi() {
        taiDuLieu();
    }

    /** Đặt ngày và kỳ (không phát sự kiện tự chọn) rồi tải một lần. */
    private void datNgayVaKy(LocalDate ngay, Ky kyMoi) {
        dangDatNgay = true;
        try {
            dcNgay.setDate(sangDate(ngay));
            ky = kyMoi;
            capNhatNutKy();
        } finally {
            dangDatNgay = false;
        }
        taiDuLieu();
    }

    /** Bấm nút kỳ: giữ khoảng đang xem khi chuyển sang Khoảng; từ Khoảng quay lại thì lấy ngày bắt đầu. */
    private void chonKy(Ky kyMoi) {
        LocalDate[] hienTai = khoangHienTai();
        dangDatNgay = true;
        try {
            if (kyMoi == Ky.KHOANG && hienTai[0] != null && hienTai[1] != null) {
                dcTu.setDate(sangDate(hienTai[0]));
                dcDen.setDate(sangDate(hienTai[1]));
            } else if (ky == Ky.KHOANG && kyMoi != Ky.KHOANG && sangLocalDate(dcTu.getDate()) != null) {
                dcNgay.setDate(dcTu.getDate());
            }
            ky = kyMoi;
            capNhatNutKy();
        } finally {
            dangDatNgay = false;
        }
        taiDuLieu();
    }

    private void capNhatNutKy() {
        btnNgay.setSelected(ky == Ky.NGAY);
        btnTuan.setSelected(ky == Ky.TUAN);
        btnThang.setSelected(ky == Ky.THANG);
        btnKhoang.setSelected(ky == Ky.KHOANG);
        for (JToggleButton b : new JToggleButton[]{btnNgay, btnTuan, btnThang, btnKhoang}) {
            b.setBackground(b.isSelected() ? TEAL : SURFACE);
            b.setForeground(b.isSelected() ? Color.WHITE : TEXT);
        }
        pnlMotNgay.setVisible(ky != Ky.KHOANG);
        pnlKhoang.setVisible(ky == Ky.KHOANG);
    }

    /** Khoảng ngày [tu, den] theo kỳ và ngày đang chọn; phần tử {@code null} nếu chưa chọn ngày. */
    private LocalDate[] khoangHienTai() {
        if (ky == Ky.KHOANG) {
            return new LocalDate[]{sangLocalDate(dcTu.getDate()), sangLocalDate(dcDen.getDate())};
        }
        LocalDate ngay = sangLocalDate(dcNgay.getDate());
        if (ngay == null) {
            return new LocalDate[]{null, null};
        }
        return switch (ky) {
            case NGAY -> new LocalDate[]{ngay, ngay};
            case TUAN -> tuanCua(ngay);
            default -> thangCua(ngay);
        };
    }

    private void taiDuLieu() {
        LocalDate[] khoang = khoangHienTai();
        LocalDate tu = khoang[0];
        LocalDate den = khoang[1];
        lblPhuDe.setText((tu == null || den == null ? "Số lịch hẹn của bạn"
                : tu.equals(den) ? "Ngày " + tu.format(NGAY)
                : "Từ " + tu.format(NGAY) + " đến " + den.format(NGAY))
                + " · Dữ liệu đến " + bayGio.get().format(GIO_NGAY));
        if (worker != null) {
            worker.cancel(true); // bỏ kết quả lần trước nếu chưa xong
        }
        if (tu == null || den == null) {
            hienLoi(ky == Ky.KHOANG ? "Vui lòng chọn đủ ngày bắt đầu và ngày kết thúc." : "Vui lòng chọn ngày xem.");
            return;
        }
        if (tu.isAfter(den)) {
            hienLoi("Khoảng thời gian không hợp lệ: ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
            return;
        }
        baoTinhTrang("Đang tải thống kê...", MUTED);
        worker = new SwingWorker<>() {
            @Override
            protected KetQua doInBackground() {
                return new KetQua(nguon.apply(tu, den), nguonDanhSach.apply(tu, den));
            }

            @Override
            protected void done() {
                if (this != worker || isCancelled()) {
                    return;
                }
                worker = null;
                try {
                    hienThi(get());
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof BusinessException) {
                        hienLoi(cause.getMessage());
                    } else {
                        LOG.log(Level.WARNING, "Không thể tải thống kê lịch hẹn của nha sĩ", cause);
                        hienLoi("Không thể tải thống kê. Vui lòng thử lại.");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        worker.execute();
    }

    private void hienThi(KetQua kq) {
        TongHop t = kq.tongHop();
        model.setRowCount(0);
        for (LichHen lh : kq.lich()) {
            model.addRow(new Object[]{lh.getNgayGioHen().format(NGAY_GIO), lh.getMaLH(),
                lh.getKhachHang().getTenKH(), lh.getDichVu().getTenDV(),
                PnlLichHen.nhanTrangThai(lh.getTrangThai())});
        }
        lblTong.setText(String.valueOf(t.tong()));
        lblDaDat.setText(String.valueOf(t.daDat()));
        lblDaDen.setText(String.valueOf(t.daDen()));
        lblHoanTat.setText(String.valueOf(t.hoanTat()));
        lblDaHuy.setText(String.valueOf(t.daHuy()));
        lblKhongDen.setText(String.valueOf(t.khongDen()));
        lblTyLeDaDat.setText(PnlThongKeLichHenNgay.tyLe(t.tyLeDaDat()));
        lblTyLeDaDen.setText(PnlThongKeLichHenNgay.tyLe(t.tyLeDaDen()));
        lblTyLeHoanTat.setText(PnlThongKeLichHenNgay.tyLe(t.tyLeHoanTat()));
        lblTyLeDaHuy.setText(PnlThongKeLichHenNgay.tyLe(t.tyLeDaHuy()));
        lblTyLeKhongDen.setText(PnlThongKeLichHenNgay.tyLe(t.tyLeKhongDen()));
        baoTinhTrang(t.coDuLieu() ? t.tong() + " lịch hẹn trong kỳ đã chọn" : KHONG_CO_LICH, MUTED);
    }

    private void hienLoi(String thongBao) {
        model.setRowCount(0);
        for (JLabel l : new JLabel[]{lblTong, lblDaDat, lblDaDen, lblHoanTat, lblDaHuy, lblKhongDen}) {
            l.setText("—");
        }
        for (JLabel l : new JLabel[]{lblTyLeDaDat, lblTyLeDaDen, lblTyLeHoanTat, lblTyLeDaHuy, lblTyLeKhongDen}) {
            l.setText(" ");
        }
        baoTinhTrang(thongBao, DO);
    }

    /** Bảng trống thì thông báo vẽ giữa bảng; có dòng thì hiện dưới bảng. */
    private void baoTinhTrang(String text, Color mau) {
        lblThongBao.setText(text);
        lblThongBao.setForeground(mau);
        lblThongBao.setVisible(model.getRowCount() > 0);
        tblLich.repaint();
    }

    private void veThongBaoGiuaBang(JTable bang, Graphics g) {
        String text = lblThongBao.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(font(13));
        g2.setColor(lblThongBao.getForeground());
        FontMetrics fm = g2.getFontMetrics();
        int y = Math.min((bang.getHeight() - fm.getHeight()) / 2, 56) + fm.getAscent();
        g2.drawString(text, Math.max(8, (bang.getWidth() - fm.stringWidth(text)) / 2), y);
        g2.dispose();
    }

    @Override
    public void removeNotify() {
        if (worker != null) {
            worker.cancel(true);
            worker = null;
        }
        super.removeNotify();
    }

    private static Date sangDate(LocalDate ngay) {
        return Date.from(ngay.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate sangLocalDate(Date ngay) {
        return ngay == null ? null : ngay.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static JLabel nhan(String text, java.awt.Font f, Color mau) {
        JLabel l = new JLabel(text);
        l.setFont(f);
        l.setForeground(mau);
        return l;
    }
}
