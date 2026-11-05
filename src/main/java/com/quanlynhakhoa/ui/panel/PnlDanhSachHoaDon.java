package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.entity.HoaDon;
import com.quanlynhakhoa.entity.KhachHang;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import com.quanlynhakhoa.ui.dialog.DlgChiTietHoaDon;
import com.quanlynhakhoa.ui.dialog.DlgThanhToan;
import com.toedter.calendar.JDateChooser;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

/**
 * Khung "danh sách hóa đơn" (thẻ trắng giữa trang "Hóa đơn" trong ảnh thiết kế): hàng tab phân loại,
 * thanh tìm kiếm phía trên bảng, bảng hóa đơn có nhãn trạng thái và link "Xem / Thu tiền", dòng tổng tiền.
 *
 * <p>Trang "Hóa đơn" là của Bảo; Bảo chỉ cần đặt khung này vào trang ({@code new PnlDanhSachHoaDon()}),
 * gọi {@link #lamMoiDuLieu()} khi cần tải lại. Khung không viết SQL: dữ liệu lấy qua {@link TraCuuHoaDon}
 * (gọi API đọc của Bảo). "Xem" mở {@link DlgChiTietHoaDon}; "Thu tiền" mở hộp thanh toán của Bảo
 * ({@code DlgThanhToan.hien}), thu xong tự tải lại.</p>
 *
 * <p>Chỗ ảnh lệch báo cáo làm theo báo cáo (Dương chọn 05/11): không có tab "Đã hủy" (bảng HoaDon
 * không có trạng thái hủy), lọc theo khoảng ngày thay cho chọn 1 ngày, mã hiển thị HD261016042 / KH01284.</p>
 *
 * <p>Cách lấy dữ liệu: mỗi lần tìm chỉ gọi DAO một lần với "Tất cả trạng thái", rồi chia theo tab trên
 * máy — nhờ vậy số đếm trên 3 tab luôn đúng và đổi tab không phải chờ DB.</p>
 */
public class PnlDanhSachHoaDon extends JPanel {

    /** Hành động thu tiền một hóa đơn; {@code sauKhiThu} gọi lại để tải danh sách. */
    @FunctionalInterface
    interface ThuTien {
        void thu(Component cha, HoaDon hoaDon, Runnable sauKhiThu);
    }

    /** Thứ tự tab như ảnh: Chờ thanh toán (mặc định) → Đã thanh toán → Tất cả. */
    static final TraCuuHoaDon.TrangThai[] THU_TU_TAB = {TraCuuHoaDon.TrangThai.CHO_THANH_TOAN,
        TraCuuHoaDon.TrangThai.DA_THANH_TOAN, TraCuuHoaDon.TrangThai.TAT_CA};
    static final int COT_MA = 0, COT_KHACH = 1, COT_MA_KHACH = 2, COT_NGAY_LAP = 3, COT_TONG = 4,
            COT_TRANG_THAI = 5, COT_THAO_TAC = 6;
    static final String LINK_XEM = "linkXem";
    static final String LINK_THU_TIEN = "linkThuTien";
    static final String LINK_IN = "linkIn";

    private static final Logger LOG = Logger.getLogger(PnlDanhSachHoaDon.class.getName());
    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color DO = new Color(0xC94C48);
    private static final Color NEN_HEADER = new Color(0xF7F9FB);
    private static final Color XANH_NHAT = new Color(0xE3F4EA);
    private static final Color VANG_NEN = new Color(0xFFF4D6);
    private static final Color VANG_CHU = new Color(0x8A6100);

    private final TraCuuHoaDon traCuu;
    private final Consumer<HoaDon> xem;
    private final ThuTien thuTien;
    private final Supplier<LocalDate> homNay;

    // --- Tab, bộ lọc ---
    private final JButton[] btnTab = new JButton[THU_TU_TAB.length];
    final JTextField txtTimKiem = new JTextField();
    final JDateChooser dcTuNgay = new JDateChooser();
    final JDateChooser dcDenNgay = new JDateChooser();
    final JComboBox<TraCuuHoaDon.TrangThai> cboTrangThai = new JComboBox<>(THU_TU_TAB);
    final JButton btnLoc = PnlTongQuanQuanLy.button("Lọc", null, false);
    /** Gõ xong 300 ms mới tìm, tránh gọi DB theo từng phím. */
    private final Timer hoanTimKiem = new Timer(300, e -> lamMoiDuLieu());

    // --- Bảng + dòng cuối ---
    final DefaultTableModel model = new DefaultTableModel(new String[]{
        "MÃ HÓA ĐƠN", "KHÁCH HÀNG", "MÃ KHÁCH HÀNG", "NGÀY LẬP", "TỔNG TIỀN", "TRẠNG THÁI", "THAO TÁC"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    final JTable tblHoaDon = new JTable(model);
    final JLabel lblHienThi = new JLabel(" ");
    final JLabel lblTenTong = new JLabel("Tổng cần thanh toán");
    final JLabel lblTong = new JLabel("0 ₫");

    // --- Dữ liệu ---
    /** Kết quả lần tìm gần nhất (mọi trạng thái). */
    private List<TraCuuHoaDon.Dong> tatCa = List.of();
    /** Các dòng đang hiện trên bảng (theo tab), cùng thứ tự với các dòng của bảng. */
    private List<TraCuuHoaDon.Dong> dangHien = List.of();
    private TraCuuHoaDon.TrangThai tabDangChon = TraCuuHoaDon.TrangThai.CHO_THANH_TOAN;
    private SwingWorker<List<TraCuuHoaDon.Dong>, Void> worker;
    /** Đang đổi bộ lọc bằng code (đồng bộ tab ↔ dropdown) thì không kích hoạt sự kiện. */
    private boolean dangDongBo;

    /** Dùng DAO thật; "Xem" mở DlgChiTietHoaDon, "Thu tiền" mở hộp thanh toán của Bảo. */
    public PnlDanhSachHoaDon() {
        this(new TraCuuHoaDon(), null,
                (cha, hd, sau) -> DlgThanhToan.hien(SwingUtilities.getWindowAncestor(cha), hd, sau),
                LocalDate::now);
    }

    /**
     * @param xem     null = mở hộp thoại chi tiết thật; test truyền hàm giả
     * @param thuTien null = không cho thu tiền (chỉ tra cứu)
     */
    PnlDanhSachHoaDon(TraCuuHoaDon traCuu, Consumer<HoaDon> xem, ThuTien thuTien, Supplier<LocalDate> homNay) {
        super(new BorderLayout());
        this.traCuu = Objects.requireNonNull(traCuu);
        this.xem = xem;
        this.thuTien = thuTien; // null = không cho thu tiền (chỉ tra cứu)
        this.homNay = Objects.requireNonNull(homNay);
        setName("danhSachHoaDon");
        setOpaque(false);
        add(taoThe(), BorderLayout.CENTER);
        ganSuKien();
    }

    // ===================== Dựng giao diện =====================

    private JPanel taoThe() {
        SurfacePanel the = new SurfacePanel(new BorderLayout(0, 14));
        the.setBorder(new EmptyBorder(16, 20, 16, 20));
        JPanel tren = new JPanel();
        tren.setOpaque(false);
        tren.setLayout(new BoxLayout(tren, BoxLayout.Y_AXIS));
        tren.add(taoHangTab());
        tren.add(Box.createVerticalStrut(14));
        tren.add(taoThanhTimKiem());
        the.add(tren, BorderLayout.NORTH);
        the.add(taoBang(), BorderLayout.CENTER);
        the.add(taoDongCuoi(), BorderLayout.SOUTH);
        return the;
    }

    /** Hàng tab có đường kẻ mảnh bên dưới; tab đang chọn gạch chân teal như ảnh. */
    private JPanel taoHangTab() {
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabs.setOpaque(false);
        tabs.setAlignmentX(LEFT_ALIGNMENT);
        tabs.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        String[] ten = {"tabChoThanhToan", "tabDaThanhToan", "tabTatCa"};
        for (int i = 0; i < THU_TU_TAB.length; i++) {
            TraCuuHoaDon.TrangThai tab = THU_TU_TAB[i];
            JButton btn = new JButton(tab.ten());
            btn.setName(ten[i]);
            btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> chonTab(tab));
            btnTab[i] = btn;
            if (i > 0) {
                tabs.add(Box.createHorizontalStrut(24));
            }
            tabs.add(btn);
        }
        veTab();
        return tabs;
    }

    /** Thanh tìm kiếm phía trên bảng: ô từ khóa → khoảng ngày → trạng thái → Lọc. */
    private JPanel taoThanhTimKiem() {
        JPanel hang = new JPanel(new BorderLayout(10, 0));
        hang.setOpaque(false);
        hang.setAlignmentX(LEFT_ALIGNMENT);
        txtTimKiem.setName("timHoaDon");
        txtTimKiem.setFont(font(13));
        txtTimKiem.putClientProperty("JTextField.placeholderText", "Mã hóa đơn, tên hoặc số điện thoại khách hàng...");
        txtTimKiem.putClientProperty("JTextField.leadingIcon", new LineIcon(LineIcon.Type.SEARCH, MUTED, 15));
        txtTimKiem.setPreferredSize(new Dimension(240, 38));
        hang.add(txtTimKiem, BorderLayout.CENTER);

        JPanel phai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        phai.setOpaque(false);
        LocalDate hn = homNay.get();
        cauHinhNgay(dcTuNgay, "tuNgay", "Lập từ ngày", TraCuuHoaDon.tuNgayMacDinh(hn));
        cauHinhNgay(dcDenNgay, "denNgay", "Lập đến ngày", hn);
        JLabel gach = new JLabel("–");
        gach.setForeground(MUTED);
        cboTrangThai.setName("locTrangThai");
        cboTrangThai.setFont(font(13));
        cboTrangThai.setPreferredSize(new Dimension(170, 38));
        btnLoc.setName("locHoaDon");
        btnLoc.setPreferredSize(new Dimension(80, 38));
        btnLoc.setToolTipText("Tìm lại theo từ khóa và khoảng ngày (Enter trong ô tìm cũng được).");
        phai.add(dcTuNgay);
        phai.add(gach);
        phai.add(dcDenNgay);
        phai.add(cboTrangThai);
        phai.add(btnLoc);
        hang.add(phai, BorderLayout.EAST);
        return hang;
    }

    private void cauHinhNgay(JDateChooser dc, String ten, String goiY, LocalDate ngay) {
        dc.setName(ten);
        dc.setLocale(Locale.forLanguageTag("vi-VN"));
        dc.setDateFormatString("dd/MM/yyyy");
        dc.setPreferredSize(new Dimension(135, 38));
        dc.setFont(font(13));
        dc.setToolTipText(goiY);
        dc.setDate(sangDate(ngay));
    }

    private JScrollPane taoBang() {
        tblHoaDon.setName("bangHoaDon");
        tblHoaDon.setFont(font(13));
        tblHoaDon.setRowHeight(50);
        tblHoaDon.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblHoaDon.setSelectionBackground(TEAL_LIGHT);
        tblHoaDon.setSelectionForeground(TEXT);
        tblHoaDon.setGridColor(BORDER);
        tblHoaDon.setShowVerticalLines(false);
        tblHoaDon.setIntercellSpacing(new Dimension(0, 1));
        tblHoaDon.setFillsViewportHeight(true);
        tblHoaDon.getTableHeader().setReorderingAllowed(false);
        tblHoaDon.getTableHeader().setPreferredSize(new Dimension(0, 40));
        tblHoaDon.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                setFont(bold(11));
                setForeground(MUTED);
                setBackground(NEN_HEADER);
                setOpaque(true);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(0, 12, 0, 12)));
                return this;
            }
        });
        DefaultTableCellRenderer thuong = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, s, false, r, c);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                setToolTipText(v == null ? null : v.toString());
                return this;
            }
        };
        tblHoaDon.setDefaultRenderer(Object.class, thuong);
        tblHoaDon.getColumnModel().getColumn(COT_TRANG_THAI).setCellRenderer(new NhanTrangThai());
        tblHoaDon.getColumnModel().getColumn(COT_THAO_TAC).setCellRenderer(new ThaoTac());
        int[] rong = {130, 220, 120, 140, 130, 140, 140};
        for (int i = 0; i < rong.length; i++) {
            tblHoaDon.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        }
        JScrollPane cuon = new JScrollPane(tblHoaDon);
        cuon.setBorder(BorderFactory.createLineBorder(BORDER));
        cuon.getViewport().setBackground(SURFACE);
        return cuon;
    }

    private JPanel taoDongCuoi() {
        JPanel dong = new JPanel(new BorderLayout(12, 0));
        dong.setOpaque(false);
        lblHienThi.setName("hienThiHoaDon");
        lblHienThi.setFont(font(12));
        lblHienThi.setForeground(MUTED);
        dong.add(lblHienThi, BorderLayout.WEST);
        JPanel tong = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        tong.setOpaque(false);
        lblTenTong.setName("tenTong");
        lblTenTong.setFont(font(13));
        lblTenTong.setForeground(TEXT);
        lblTong.setName("tongHoaDon");
        lblTong.setFont(bold(20));
        lblTong.setForeground(TEAL);
        tong.add(lblTenTong);
        tong.add(lblTong);
        dong.add(tong, BorderLayout.EAST);
        return dong;
    }

    // ===================== Sự kiện =====================

    private void ganSuKien() {
        hoanTimKiem.setRepeats(false);
        txtTimKiem.addActionListener(e -> lamMoiDuLieu()); // Enter: tìm ngay
        txtTimKiem.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                hoanTimKiem.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                hoanTimKiem.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                hoanTimKiem.restart();
            }
        });
        btnLoc.addActionListener(e -> lamMoiDuLieu());
        // Đổi ngày cũng tìm lại (ô ngày phát sự kiện "date" mỗi khi chọn trên lịch).
        dcTuNgay.addPropertyChangeListener("date", e -> lamMoiDuLieu());
        dcDenNgay.addPropertyChangeListener("date", e -> lamMoiDuLieu());
        cboTrangThai.addActionListener(e -> {
            if (!dangDongBo && cboTrangThai.getSelectedItem() instanceof TraCuuHoaDon.TrangThai tt) {
                chonTab(tt);
            }
        });
        tblHoaDon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tblHoaDon.rowAtPoint(e.getPoint());
                if (!SwingUtilities.isLeftMouseButton(e) || row < 0) {
                    return;
                }
                tblHoaDon.setRowSelectionInterval(row, row);
                String link = linkTaiDiem(e.getPoint());
                if (link != null && e.getClickCount() == 1) {
                    chayThaoTac(link, dangHien.get(row).hoaDon());
                } else if (link == null && e.getClickCount() == 2) {
                    chayThaoTac(LINK_XEM, dangHien.get(row).hoaDon()); // nhấn đúp dòng = Xem
                }
            }
        });
        tblHoaDon.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                tblHoaDon.setCursor(linkTaiDiem(e.getPoint()) != null
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(() -> {
            if (isDisplayable()) {
                lamMoiDuLieu();
            }
        });
    }

    @Override
    public void removeNotify() {
        hoanTimKiem.stop();
        if (worker != null) {
            worker.cancel(true);
            worker = null;
        }
        super.removeNotify();
    }

    // ===================== Tải dữ liệu =====================

    /** Tìm lại theo từ khóa + khoảng ngày (chạy nền); Bảo gọi hàm này để làm mới sau khi thu tiền. */
    public void lamMoiDuLieu() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::lamMoiDuLieu);
            return;
        }
        hoanTimKiem.stop();
        if (worker != null) {
            worker.cancel(true); // bỏ kết quả lần tìm trước nếu chưa xong
        }
        String tuKhoa = txtTimKiem.getText();
        LocalDate tu = sangLocalDate(dcTuNgay.getDate());
        LocalDate den = sangLocalDate(dcDenNgay.getDate());
        try {
            TraCuuHoaDon.kiemTraKhoangNgay(tu, den); // báo ngay, không cần gọi DB
        } catch (BusinessException ex) {
            tatCa = List.of();
            hienThi();
            baoLoi(ex.getMessage());
            return;
        }
        btnLoc.setEnabled(false);
        lblHienThi.setForeground(MUTED);
        lblHienThi.setText("Đang tải danh sách hóa đơn...");
        worker = new SwingWorker<>() {
            @Override
            protected List<TraCuuHoaDon.Dong> doInBackground() {
                // Lấy mọi trạng thái một lần để đếm cả 3 tab.
                return List.copyOf(traCuu.tim(tuKhoa, TraCuuHoaDon.TrangThai.TAT_CA, tu, den));
            }

            @Override
            protected void done() {
                if (this != worker || isCancelled()) {
                    return;
                }
                try {
                    tatCa = get();
                    hienThi();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    tatCa = List.of();
                    hienThi();
                    if (cause instanceof BusinessException) {
                        baoLoi(cause.getMessage());
                    } else {
                        LOG.log(Level.WARNING, "Không thể tra cứu hóa đơn", cause);
                        baoLoi("Không thể tải danh sách hóa đơn. Vui lòng thử lại.");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    worker = null;
                    btnLoc.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    /** Lọc {@link #tatCa} theo tab đang chọn rồi đổ lên bảng, cập nhật số đếm và dòng tổng. */
    private void hienThi() {
        List<TraCuuHoaDon.Dong> theoTab = new ArrayList<>();
        BigDecimal tong = BigDecimal.ZERO;
        for (TraCuuHoaDon.Dong d : tatCa) {
            if (khopTab(tabDangChon, d.hoaDon())) {
                theoTab.add(d);
                tong = tong.add(d.tongThanhToan() == null ? BigDecimal.ZERO : d.tongThanhToan());
            }
        }
        dangHien = theoTab;
        tblHoaDon.clearSelection();
        model.setRowCount(0);
        for (TraCuuHoaDon.Dong d : dangHien) {
            HoaDon hd = d.hoaDon();
            KhachHang kh = d.khachHang();
            model.addRow(new Object[]{hd.getMaHD(), kh == null ? "—" : kh.getTenKH(), kh == null ? "—" : kh.getMaKH(),
                hd.getNgayLap() == null ? "—" : hd.getNgayLap().format(NGAY_GIO), tien(d.tongThanhToan()),
                TraCuuHoaDon.tenTrangThai(hd), hd.isTrangThai() ? "Xem · In hóa đơn" : "Xem · Thanh toán"});
        }
        veTab();
        lblHienThi.setForeground(MUTED);
        lblHienThi.setText(dangHien.isEmpty() ? "Không tìm thấy hóa đơn phù hợp."
                : "Hiển thị " + dangHien.size() + "/" + dangHien.size() + " hóa đơn · Đơn vị: VNĐ");
        lblTenTong.setText(switch (tabDangChon) {
            case CHO_THANH_TOAN -> "Tổng cần thanh toán";
            case DA_THANH_TOAN -> "Tổng đã thu";
            case TAT_CA -> "Tổng giá trị hóa đơn";
        });
        lblTong.setText(tien(tong));
    }

    private void baoLoi(String thongBao) {
        lblHienThi.setForeground(DO);
        lblHienThi.setText(thongBao);
    }

    private static boolean khopTab(TraCuuHoaDon.TrangThai tab, HoaDon hd) {
        return tab.daTra() == null || tab.daTra() == hd.isTrangThai();
    }

    // ===================== Tab ↔ dropdown trạng thái =====================

    private void chonTab(TraCuuHoaDon.TrangThai tab) {
        tabDangChon = tab;
        dangDongBo = true;
        try {
            cboTrangThai.setSelectedItem(tab); // dropdown luôn khớp tab
        } finally {
            dangDongBo = false;
        }
        hienThi(); // chỉ lọc lại trên máy
    }

    /** Chữ tab kèm số đếm; tab chọn chữ đậm teal và gạch chân 2px. */
    private void veTab() {
        for (int i = 0; i < THU_TU_TAB.length; i++) {
            JButton btn = btnTab[i];
            if (btn == null) {
                return; // đang dựng giao diện
            }
            TraCuuHoaDon.TrangThai tab = THU_TU_TAB[i];
            long dem = tatCa.stream().filter(d -> khopTab(tab, d.hoaDon())).count();
            boolean chon = tab == tabDangChon;
            btn.setText(tab.ten() + " (" + dem + ")");
            btn.setFont(chon ? bold(13) : font(13));
            btn.setForeground(chon ? TEAL : MUTED);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 0, chon ? TEAL : new Color(0, 0, 0, 0)),
                    new EmptyBorder(8, 2, 8, 2)));
        }
    }

    // ===================== Thao tác Xem / Thu tiền =====================

    /** Tên link dưới con trỏ ở cột THAO TÁC, null nếu không trúng link. */
    String linkTaiDiem(Point diem) {
        int row = tblHoaDon.rowAtPoint(diem);
        int cot = tblHoaDon.columnAtPoint(diem);
        if (row < 0 || cot < 0 || tblHoaDon.convertColumnIndexToModel(cot) != COT_THAO_TAC) {
            return null;
        }
        Rectangle o = tblHoaDon.getCellRect(row, cot, false);
        Component ve = tblHoaDon.prepareRenderer(tblHoaDon.getCellRenderer(row, cot), row, cot);
        ve.setBounds(0, 0, o.width, o.height);
        ve.doLayout();
        Component con = SwingUtilities.getDeepestComponentAt(ve, diem.x - o.x, diem.y - o.y);
        String ten = con == null || !con.isVisible() ? null : con.getName();
        return LINK_XEM.equals(ten) || LINK_THU_TIEN.equals(ten) || LINK_IN.equals(ten) ? ten : null;
    }

    /** "Xem" mở chi tiết; "Thu tiền" (chỉ hóa đơn chưa thanh toán) mở hộp thanh toán, thu xong tải lại. */
    void chayThaoTac(String link, HoaDon hd) {
        if (hd == null) {
            return;
        }
        if (LINK_XEM.equals(link)) {
            moChiTiet(hd);
        } else if (LINK_IN.equals(link) && hd.isTrangThai()) {
            moChiTiet(hd);
        } else if (LINK_THU_TIEN.equals(link) && !hd.isTrangThai() && thuTien != null) {
            thuTien.thu(this, hd, this::lamMoiDuLieu);
        }
    }

    /** Mở hộp thoại chi tiết hóa đơn (test thay bằng hàm giả qua tham số {@code xem}). */
    private void moChiTiet(HoaDon hd) {
        if (xem == null) {
            new DlgChiTietHoaDon(SwingUtilities.getWindowAncestor(this), hd.getMaHD()).setVisible(true);
        } else {
            xem.accept(hd);
        }
    }

    // ===================== Hiển thị =====================

    static String tien(BigDecimal soTien) {
        DecimalFormat f = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.of("vi", "VN")));
        return f.format(soTien == null ? BigDecimal.ZERO : soTien) + " ₫";
    }

    private static Date sangDate(LocalDate ngay) {
        return ngay == null ? null : Date.from(ngay.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate sangLocalDate(Date ngay) {
        return ngay == null ? null : ngay.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /** Nhãn trạng thái bo góc: vàng "Chờ thanh toán", xanh "Đã thanh toán" (như ảnh). */
    private static final class NhanTrangThai extends JPanel implements TableCellRenderer {
        private final JLabel nhan = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        NhanTrangThai() {
            super(new FlowLayout(FlowLayout.LEFT, 12, 13));
            nhan.setFont(bold(11));
            nhan.setBorder(new EmptyBorder(3, 8, 3, 8));
            add(nhan);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean focus, int row, int column) {
            boolean daTra = TraCuuHoaDon.TrangThai.DA_THANH_TOAN.ten().equals(value);
            nhan.setText(value == null ? "" : value.toString());
            nhan.setBackground(daTra ? XANH_NHAT : VANG_NEN);
            nhan.setForeground(daTra ? GREEN : VANG_CHU);
            setBackground(selected ? table.getSelectionBackground() : table.getBackground());
            return this;
        }
    }

    /** Ô thao tác: link "Xem" và (khi chưa thanh toán, có quyền thu) link "Thu tiền". */
    private final class ThaoTac extends JPanel implements TableCellRenderer {
        private final JLabel lblXem = new JLabel("Xem");
        private final JLabel lblIn = new JLabel("In hóa đơn");
        private final JLabel lblThu = new JLabel("Thanh toán");

        ThaoTac() {
            super(new FlowLayout(FlowLayout.LEFT, 10, 15));
            lblXem.setName(LINK_XEM);
            lblIn.setName(LINK_IN);
            lblThu.setName(LINK_THU_TIEN);
            for (JLabel l : new JLabel[]{lblXem, lblIn, lblThu}) {
                l.setFont(bold(12));
                l.setForeground(TEAL);
                add(l);
            }
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean focus, int row, int column) {
            HoaDon hd = row < dangHien.size() ? dangHien.get(row).hoaDon() : null;
            lblIn.setVisible(hd != null && hd.isTrangThai());
            lblThu.setVisible(hd != null && !hd.isTrangThai() && thuTien != null);
            setBackground(selected ? table.getSelectionBackground() : table.getBackground());
            return this;
        }
    }
}
