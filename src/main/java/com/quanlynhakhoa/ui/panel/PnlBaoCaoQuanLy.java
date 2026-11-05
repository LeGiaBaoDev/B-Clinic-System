package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.dao.BaoCaoQuanLyDAO;
import com.quanlynhakhoa.dao.BaoCaoQuanLyDAO.*;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.ui.component.*;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.nio.file.*;
import java.text.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.Function;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Báo cáo Quản lý: tải nền, dữ liệu thật, bộ lọc và snapshot Excel cùng kỳ. */
public class PnlBaoCaoQuanLy extends JPanel {
    private static final Logger LOG = Logger.getLogger(PnlBaoCaoQuanLy.class.getName());
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final Function<KyBaoCao, BaoCao> nguon;
    private final Clock dongHo;
    private final com.toedter.calendar.JDateChooser dpTu = datePicker("tuNgayBaoCao");
    private final com.toedter.calendar.JDateChooser dpDen = datePicker("denNgayBaoCao");
    private final Map<CheDo, JToggleButton> cheDoButtons = new EnumMap<>(CheDo.class);
    private CheDo cheDo = CheDo.TUAN;
    private boolean dangGanNgay;
    private boolean boLocDaDoi;
    private final JButton btnXuat = nut("Xuất báo cáo", LineIcon.Type.INVOICE, false);
    private final JLabel lblCapNhat = label("Doanh thu đã thu và hiệu quả vận hành", font(12), MUTED);
    private final JLabel lblSoSanh = label(" ", font(12), MUTED);
    private final JLabel lblTrangThai = label("Chọn kỳ và nhấn Xem báo cáo.", font(12), MUTED);
    private final JLabel[] lblGiaTri = new JLabel[3], lblGhiChu = new JLabel[3];
    private final JLabel lblKetQua = label("Chưa có dữ liệu", font(12), MUTED);
    private final JLabel lblTyLe = label("—", bold(12), GREEN);
    private final JLabel lblChiTietLich = label(" ", font(11), MUTED);
    private final JLabel lblDichVu = label(" ", font(11), MUTED);
    private final JLabel lblHoanTat = label("Hoàn tất: —", font(12), TEXT);
    private final JLabel lblHuy = label("Đã hủy: —", font(12), TEXT);
    private final JLabel lblKhongDen = label("Không đến: —", font(12), TEXT);
    private final JLabel lblChenhLech = label(" ", font(11), GREEN);
    private final BieuDoDoanhThu bieuDo = new BieuDoDoanhThu();
    private final ThanhKetQua thanhKetQua = new ThanhKetQua();
    private final DefaultTableModel modelDichVu = model(new String[]{"Mã", "Dịch vụ", "Số lượng", "Tiền trước giảm"},
            new Class<?>[]{String.class, String.class, Long.class, BigDecimal.class});
    private final DefaultTableModel modelSoSanh = model(new String[]{"Chỉ tiêu", "Kỳ trước", "Kỳ này"},
            new Class<?>[]{String.class, String.class, String.class});
    private final JTable tblDichVu = table(modelDichVu), tblSoSanh = table(modelSoSanh);
    private BaoCao duLieu;
    private SwingWorker<BaoCao, Void> worker;
    private SwingWorker<Void, Void> exportWorker;

    public PnlBaoCaoQuanLy() { this(new BaoCaoQuanLyDAO()::taiBaoCao, Clock.system(ZONE)); }
    public PnlBaoCaoQuanLy(Function<KyBaoCao, BaoCao> nguon, Clock dongHo) {
        super(new BorderLayout());
        this.nguon = Objects.requireNonNull(nguon);
        this.dongHo = Objects.requireNonNull(dongHo).withZone(ZONE);
        setName("Báo cáo"); setBackground(BACKGROUND);
        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(BACKGROUND); body.setBorder(new EmptyBorder(22, 24, 24, 24));
        addRow(body, taoTieuDe(), 0, 0);
        addRow(body, taoBoLoc(), 1, 0);
        addRow(body, taoTheSoLieu(), 2, 0);
        JPanel charts = new JPanel(new GridBagLayout()); charts.setOpaque(false);
        pair(charts, taoBieuDo(), taoKetQua()); charts.setPreferredSize(new Dimension(0, 226));
        addRow(body, charts, 3, 0.45);
        JPanel details = new JPanel(new GridBagLayout()); details.setOpaque(false);
        pair(details, taoDichVu(), taoSoSanh()); details.setPreferredSize(new Dimension(0, 240));
        addRow(body, details, 4, 0.55);
        addRow(body, lblTrangThai, 5, 0);
        body.setPreferredSize(new Dimension(1000, 900));
        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder()); scroll.getVerticalScrollBar().setUnitIncrement(20);
        add(scroll, BorderLayout.CENTER);
        ganKhoangNgay(KyBaoCao.tao(cheDo, LocalDate.now(this.dongHo), null).hienTai());
        dpDen.setEnabled(false); btnXuat.setEnabled(false);
        dpTu.addPropertyChangeListener("date", e -> danhDauBoLoc());
        dpDen.addPropertyChangeListener("date", e -> danhDauBoLoc());
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("F5"), "reload");
        getActionMap().put("reload", new AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e) { lamMoiDuLieu(); } });
    }

    private JPanel taoTieuDe() {
        JPanel panel = new JPanel(new BorderLayout(16, 4)); panel.setOpaque(false);
        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 6)); heading.setOpaque(false);
        heading.add(label("Báo cáo hoạt động", bold(24), TEXT)); heading.add(lblCapNhat);
        panel.add(heading, BorderLayout.CENTER);
        btnXuat.setName("xuatBaoCao"); btnXuat.addActionListener(e -> chonFileXuat());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); right.setOpaque(false); right.add(btnXuat);
        panel.add(right, BorderLayout.EAST); return panel;
    }

    private JPanel taoBoLoc() {
        JPanel panel = new JPanel(new BorderLayout(12, 10)); panel.setOpaque(false);
        JPanel modes = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)); modes.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        String[] names = {"Ngày", "Tuần", "Tháng", "Tùy chọn"};
        for (CheDo mode : CheDo.values()) {
            JToggleButton button = new JToggleButton(names[mode.ordinal()]);
            button.setName("kyBaoCao" + mode.name()); button.setFont(bold(12)); button.setFocusPainted(false);
            button.setMargin(new Insets(7, 14, 7, 14)); button.setSelected(mode == cheDo);
            button.setBackground(mode == cheDo ? TEAL_DARK : SURFACE);
            button.setForeground(mode == cheDo ? Color.WHITE : TEXT);
            group.add(button); modes.add(button); cheDoButtons.put(mode, button);
            button.addActionListener(e -> {
                cheDo = mode; dpDen.setEnabled(mode == CheDo.TUY_CHON);
                cheDoButtons.forEach((key, value) -> {
                    value.setBackground(key == mode ? TEAL_DARK : SURFACE);
                    value.setForeground(key == mode ? Color.WHITE : TEXT);
                });
                if (mode != CheDo.TUY_CHON) {
                    ganKhoangNgay(KyBaoCao.tao(mode, LocalDate.now(dongHo), null).hienTai());
                    lamMoiDuLieu();
                } else danhDauBoLoc();
            });
        }
        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false);
        top.add(modes, BorderLayout.WEST); top.add(lblSoSanh, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);
        JPanel dates = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); dates.setOpaque(false);
        dates.add(label("Từ ngày", font(12), MUTED)); dates.add(dpTu);
        dates.add(label("Đến ngày", font(12), MUTED)); dates.add(dpDen);
        JButton apply = nut("Xem báo cáo", LineIcon.Type.CHART, true);
        apply.setName("xemBaoCao"); apply.addActionListener(e -> lamMoiDuLieu()); dates.add(apply);
        panel.add(dates, BorderLayout.SOUTH); return panel;
    }

    private JPanel taoTheSoLieu() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 14, 0)); panel.setOpaque(false);
        String[] titles = {"Doanh thu đã thu", "Lịch hẹn hoàn tất", "Đã hủy / Không đến"};
        for (int i = 0; i < 3; i++) {
            JPanel card = card(); card.setLayout(new BorderLayout(0, 8));
            card.add(label(titles[i], font(12), MUTED), BorderLayout.NORTH);
            lblGiaTri[i] = label("—", bold(25), TEXT); lblGiaTri[i].setName("chiSoBaoCao" + i);
            lblGhiChu[i] = label("Chưa tải dữ liệu", font(11), MUTED);
            card.add(lblGiaTri[i], BorderLayout.CENTER); card.add(lblGhiChu[i], BorderLayout.SOUTH); panel.add(card);
        }
        panel.setPreferredSize(new Dimension(0, 108)); return panel;
    }

    private JPanel taoBieuDo() {
        JPanel panel = card(); panel.setLayout(new BorderLayout(0, 8));
        JPanel head = new JPanel(new BorderLayout()); head.setOpaque(false);
        head.add(label("Doanh thu theo ngày", bold(14), TEXT), BorderLayout.WEST);
        head.add(label("Đơn vị: triệu đồng", font(11), MUTED), BorderLayout.EAST);
        JPanel north = new JPanel(new GridLayout(2, 1, 0, 5)); north.setOpaque(false); north.add(head);
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); legend.setOpaque(false);
        legend.add(label("■ Kỳ này     ", font(11), TEAL_DARK));
        legend.add(label("■ Kỳ trước (cùng thứ tự ngày trong kỳ)", font(11), new Color(0x80BDBA)));
        north.add(legend);
        panel.add(north, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(bieuDo); scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        panel.add(scroll, BorderLayout.CENTER); return panel;
    }

    private JPanel taoKetQua() {
        JPanel panel = card(); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{label("Kết quả lịch hẹn", bold(14), TEXT), lblKetQua, lblTyLe,
                thanhKetQua, lblHoanTat, lblHuy, lblKhongDen, lblChiTietLich}) {
            c.setAlignmentX(LEFT_ALIGNMENT); panel.add(c); panel.add(Box.createVerticalStrut(6));
        }
        thanhKetQua.setPreferredSize(new Dimension(250, 15)); thanhKetQua.setMaximumSize(new Dimension(Integer.MAX_VALUE, 15));
        thanhKetQua.setMinimumSize(new Dimension(0, 12));
        return panel;
    }

    private JPanel taoDichVu() {
        JPanel panel = card(); panel.setLayout(new BorderLayout(0, 9));
        panel.add(label("Dịch vụ được sử dụng nhiều", bold(14), TEXT), BorderLayout.NORTH);
        tblDichVu.setName("dichVuBaoCao"); tblDichVu.getColumnModel().getColumn(0).setPreferredWidth(65);
        tblDichVu.getColumnModel().getColumn(1).setPreferredWidth(260);
        tblDichVu.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public void setValue(Object value) { setHorizontalAlignment(RIGHT); setText(value instanceof BigDecimal money ? tien(money) : ""); }
        });
        panel.add(tableScroll(tblDichVu), BorderLayout.CENTER);
        lblDichVu.setToolTipText("Số lượng và tiền sau thuế của dịch vụ trên hóa đơn đã trả trong kỳ; tiền dịch vụ trước giảm voucher cả hóa đơn.");
        panel.add(lblDichVu, BorderLayout.SOUTH); return panel;
    }

    private JPanel taoSoSanh() {
        JPanel panel = card(); panel.setLayout(new BorderLayout(0, 9));
        panel.add(label("So sánh với kỳ trước", bold(14), TEXT), BorderLayout.NORTH);
        tblSoSanh.setAutoCreateRowSorter(false); panel.add(tableScroll(tblSoSanh), BorderLayout.CENTER);
        panel.add(lblChenhLech, BorderLayout.SOUTH); return panel;
    }

    private void danhDauBoLoc() {
        if (dangGanNgay) return;
        boLocDaDoi = true;
        btnXuat.setEnabled(false); lblTrangThai.setForeground(MUTED);
        lblTrangThai.setText("Bộ lọc đã đổi. Nhấn Xem báo cáo để áp dụng.");
        if (worker != null) { worker.cancel(true); worker = null; }
    }

    public void lamMoiDuLieu() {
        if (!SwingUtilities.isEventDispatchThread()) { SwingUtilities.invokeLater(this::lamMoiDuLieu); return; }
        final KyBaoCao requested;
        try {
            requested = KyBaoCao.tao(cheDo, docNgay(dpTu), docNgay(dpDen));
        } catch (BusinessException e) {
            lblTrangThai.setForeground(new Color(0xC94C48)); lblTrangThai.setText(e.getMessage()); btnXuat.setEnabled(false); return;
        }
        ganKhoangNgay(requested.hienTai());
        boLocDaDoi = false;
        if (worker != null) worker.cancel(true);
        duLieu = null; btnXuat.setEnabled(false); clear();
        lblSoSanh.setText("So sánh: " + khoang(requested.kyTruoc()));
        lblCapNhat.setText("Đang tải " + khoang(requested.hienTai()) + "…");
        lblTrangThai.setForeground(MUTED); lblTrangThai.setText("Đang tải báo cáo…");
        worker = new SwingWorker<>() {
            @Override protected BaoCao doInBackground() { return nguon.apply(requested); }
            @Override protected void done() {
                if (this != worker || isCancelled()) return;
                try {
                    BaoCao data = Objects.requireNonNull(get());
                    if (!requested.equals(data.ky())) throw new IllegalStateException("Kỳ báo cáo không khớp.");
                    hienThi(data);
                } catch (CancellationException ignored) {
                } catch (InterruptedException e) { Thread.currentThread().interrupt();
                } catch (Exception e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause(); LOG.log(Level.WARNING, "Không tải được báo cáo", cause);
                    lblCapNhat.setText("Chưa tải được báo cáo " + khoang(requested.hienTai()));
                    lblTrangThai.setForeground(new Color(0xC94C48));
                    lblTrangThai.setText(cause instanceof BusinessException ? cause.getMessage() : "Không tải được báo cáo. Nhấn Xem báo cáo hoặc F5 để thử lại.");
                } finally { if (this == worker) worker = null; }
            }
        };
        worker.execute();
    }

    private void clear() {
        for (int i = 0; i < 3; i++) { lblGiaTri[i].setText("—"); lblGhiChu[i].setText("Đang tải…"); }
        modelDichVu.setRowCount(0); modelSoSanh.setRowCount(0); bieuDo.setData(List.of(), List.of());
        thanhKetQua.setData(KetQuaLich.TRONG); lblKetQua.setText("Đang tải…"); lblTyLe.setText("—");
        lblHoanTat.setText("Hoàn tất: —"); lblHuy.setText("Đã hủy: —"); lblKhongDen.setText("Không đến: —");
        lblChiTietLich.setText(" "); lblDichVu.setText(" "); lblChenhLech.setText(" ");
    }

    private void hienThi(BaoCao data) {
        duLieu = data;
        lblCapNhat.setText(khoang(data.ky().hienTai()) + " · Dữ liệu lúc " + data.capNhatLuc().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
        lblGiaTri[0].setText(tien(data.doanhThu()));
        String growth = data.tangTruong().map(n -> (n.signum() > 0 ? "+" : "") + so(n, "0.#") + "%").orElse("Chưa có cơ sở %");
        lblGhiChu[0].setText(growth + " · Kỳ trước: " + tien(data.doanhThuTruoc()));
        lblGhiChu[0].setToolTipText("Tiền mặt: " + tien(data.tienMat()) + " · Chuyển khoản: " + tien(data.chuyenKhoan()));
        var current = data.lichHienTai(); var old = data.lichKyTruoc();
        lblGiaTri[1].setText(Long.toString(current.hoanTat()));
        lblGhiChu[1].setText("Kỳ trước: " + old.hoanTat() + " · Chênh lệch: " + (current.hoanTat() - old.hoanTat()));
        lblGiaTri[2].setText(current.daHuy() + " / " + current.khongDen());
        lblGhiChu[2].setText("Kỳ trước: " + old.daHuy() + " hủy / " + old.khongDen() + " không đến");
        bieuDo.setData(data.hienTai(), data.kyTruoc()); thanhKetQua.setData(current);
        lblKetQua.setText(current.daKetThuc() + " lịch đã kết thúc / " + current.tong() + " tổng lịch");
        lblTyLe.setText(so(BigDecimal.valueOf(current.tyLeHoanTat()), "0.#") + "% hoàn tất");
        lblHoanTat.setText("Hoàn tất: " + current.hoanTat()); lblHuy.setText("Đã hủy: " + current.daHuy()); lblKhongDen.setText("Không đến: " + current.khongDen());
        lblChiTietLich.setText(current.daDat() + " đã đặt · " + current.daDen() + " đã đến");
        for (var service : data.dichVu()) modelDichVu.addRow(new Object[]{service.maDV(), service.tenDV(), service.soLuong(), service.tienTruocGiam()});
        lblDichVu.setText(data.dichVu().isEmpty() ? "Chưa có dịch vụ trên hóa đơn đã thanh toán." : "Hóa đơn đã trả · tiền dịch vụ sau thuế, trước giảm voucher");
        modelSoSanh.addRow(new Object[]{"Đã thu", tien(data.doanhThuTruoc()), tien(data.doanhThu())});
        modelSoSanh.addRow(new Object[]{"Tiền mặt", tien(tong(data.kyTruoc(), true)), tien(data.tienMat())});
        modelSoSanh.addRow(new Object[]{"Chuyển khoản", tien(tong(data.kyTruoc(), false)), tien(data.chuyenKhoan())});
        modelSoSanh.addRow(new Object[]{"Hoàn tất", Long.toString(old.hoanTat()), Long.toString(current.hoanTat())});
        modelSoSanh.addRow(new Object[]{"Đã hủy", Long.toString(old.daHuy()), Long.toString(current.daHuy())});
        modelSoSanh.addRow(new Object[]{"Không đến", Long.toString(old.khongDen()), Long.toString(current.khongDen())});
        double delta = current.tyLeHoanTat() - old.tyLeHoanTat();
        lblChenhLech.setText("Hoàn tất: " + (delta > 0 ? "+" : "") + so(BigDecimal.valueOf(delta), "0.#") + " điểm % so với kỳ trước");
        lblChenhLech.setForeground(delta < 0 ? new Color(0xC94C48) : GREEN);
        lblTrangThai.setForeground(MUTED);
        lblTrangThai.setText("Doanh thu theo ngày thanh toán · Lịch theo ngày hẹn · Tỷ lệ hoàn tất trên lịch đã kết thúc.");
        btnXuat.setEnabled(exportWorker == null && !boLocDaDoi);
    }

    @Override public void removeNotify() {
        if (worker != null) { worker.cancel(true); worker = null; }
        super.removeNotify();
    }

    public void timKiem(String keyword) {
        @SuppressWarnings("unchecked") TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) tblDichVu.getRowSorter();
        sorter.setRowFilter(keyword == null || keyword.isBlank() ? null
                : RowFilter.regexFilter("(?iu)" + java.util.regex.Pattern.quote(keyword.trim()), 0, 1));
    }

    private void chonFileXuat() {
        if (duLieu == null || exportWorker != null) return;
        BaoCao snapshot = duLieu;
        JFileChooser chooser = new JFileChooser(); chooser.setFileFilter(new FileNameExtensionFilter("Excel (*.xlsx)", "xlsx"));
        chooser.setSelectedFile(new java.io.File("BaoCao_" + snapshot.ky().hienTai().tu() + "_" + snapshot.ky().hienTai().den() + ".xlsx"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path file = chooser.getSelectedFile().toPath().toAbsolutePath();
        if (!file.toString().toLowerCase(Locale.ROOT).endsWith(".xlsx")) file = Path.of(file + ".xlsx");
        if (Files.exists(file) && JOptionPane.showConfirmDialog(this, "Tệp đã có. Bạn có muốn ghi đè?", "Xuất báo cáo", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        Path output = file; btnXuat.setEnabled(false);
        exportWorker = new SwingWorker<>() {
            protected Void doInBackground() throws Exception { xuatExcel(snapshot, output); return null; }
            protected void done() {
                try { get(); JOptionPane.showMessageDialog(PnlBaoCaoQuanLy.this, "Đã xuất báo cáo:\n" + output); }
                catch (Exception e) { LOG.log(Level.WARNING, "Không xuất được báo cáo", e); JOptionPane.showMessageDialog(PnlBaoCaoQuanLy.this, "Không xuất được tệp. Kiểm tra quyền ghi và đóng tệp Excel đang mở.", "Xuất báo cáo", JOptionPane.ERROR_MESSAGE); }
                finally { exportWorker = null; btnXuat.setEnabled(duLieu != null && worker == null && !boLocDaDoi); }
            }
        }; exportWorker.execute();
    }

    static void xuatExcel(BaoCao data, Path output) throws java.io.IOException {
        Path absolute = output.toAbsolutePath(); Path temporary = Files.createTempFile(absolute.getParent(), ".baocao-", ".xlsx");
        try {
            try (XSSFWorkbook book = new XSSFWorkbook()) {
                CellStyle heading = book.createCellStyle(); heading.setFillForegroundColor(IndexedColors.TEAL.getIndex()); heading.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                org.apache.poi.ss.usermodel.Font font = book.createFont(); font.setBold(true); font.setColor(IndexedColors.WHITE.getIndex()); heading.setFont(font);
                CellStyle number = book.createCellStyle(); number.setDataFormat(book.createDataFormat().getFormat("#,##0.00"));
                Sheet summary = book.createSheet("Tổng hợp");
                row(summary, 0, heading, number, "Chỉ tiêu", "Kỳ trước", "Kỳ này");
                row(summary, 1, null, number, "Khoảng ngày", khoang(data.ky().kyTruoc()), khoang(data.ky().hienTai()));
                row(summary, 2, null, number, "Dữ liệu lúc", "", data.capNhatLuc().toString());
                row(summary, 3, null, number, "Doanh thu đã thu", data.doanhThuTruoc(), data.doanhThu());
                row(summary, 4, null, number, "Tiền mặt", tong(data.kyTruoc(), true), data.tienMat());
                row(summary, 5, null, number, "Chuyển khoản", tong(data.kyTruoc(), false), data.chuyenKhoan());
                KetQuaLich old = data.lichKyTruoc(), current = data.lichHienTai();
                row(summary, 6, null, number, "Tổng lịch", old.tong(), current.tong());
                row(summary, 7, null, number, "Đã đặt", old.daDat(), current.daDat());
                row(summary, 8, null, number, "Đã đến", old.daDen(), current.daDen());
                row(summary, 9, null, number, "Hoàn tất", old.hoanTat(), current.hoanTat());
                row(summary, 10, null, number, "Đã hủy", old.daHuy(), current.daHuy());
                row(summary, 11, null, number, "Không đến", old.khongDen(), current.khongDen());
                row(summary, 12, null, number, "% hoàn tất trên lịch kết thúc", old.tyLeHoanTat(), current.tyLeHoanTat());
                row(summary, 14, null, number, "Quy tắc", "Doanh thu theo ngày thanh toán", "Lịch theo ngày hẹn");
                dailySheet(book.createSheet("Doanh thu kỳ này"), data.hienTai(), heading, number);
                dailySheet(book.createSheet("Doanh thu kỳ trước"), data.kyTruoc(), heading, number);
                Sheet services = book.createSheet("Dịch vụ"); row(services, 0, heading, number, "Mã", "Dịch vụ", "Số lượng", "Tiền sau thuế trước voucher");
                int n = 1; for (var service : data.dichVu()) row(services, n++, null, number, service.maDV(), service.tenDV(), service.soLuong(), service.tienTruocGiam());
                for (Sheet sheet : book) { sheet.createFreezePane(0, 1); for (int col = 0; col < sheet.getRow(0).getLastCellNum(); col++) { sheet.autoSizeColumn(col); sheet.setColumnWidth(col, Math.min(18000, Math.max(3600, sheet.getColumnWidth(col) + 600))); } }
                try (var out = Files.newOutputStream(temporary)) { book.write(out); }
            }
            try { Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
    private static void dailySheet(Sheet sheet, List<DoanhThuNgay> values, CellStyle head, CellStyle number) {
        row(sheet, 0, head, number, "Ngày thanh toán", "Số hóa đơn", "Đã thu sau voucher", "Tiền mặt", "Chuyển khoản");
        int i = 1; for (var value : values) row(sheet, i++, null, number, DATE.format(value.ngay()), value.soHoaDon(), value.doanhThu(), value.tienMat(), value.chuyenKhoan());
    }
    private static void row(Sheet sheet, int index, CellStyle heading, CellStyle number, Object... values) {
        Row row = sheet.createRow(index);
        for (int i = 0; i < values.length; i++) { Cell cell = row.createCell(i); if (values[i] instanceof Number value) { cell.setCellValue(value.doubleValue()); cell.setCellStyle(number); } else cell.setCellValue(Objects.toString(values[i], "")); if (heading != null) cell.setCellStyle(heading); }
    }

    private void ganKhoangNgay(KhoangNgay range) {
        dangGanNgay = true;
        try { dpTu.setDate(Date.from(range.tu().atStartOfDay(ZoneId.systemDefault()).toInstant())); dpDen.setDate(Date.from(range.den().atStartOfDay(ZoneId.systemDefault()).toInstant())); }
        finally { dangGanNgay = false; }
    }
    private static LocalDate docNgay(com.toedter.calendar.JDateChooser picker) { return picker.getDate() == null ? null : picker.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(); }
    private static com.toedter.calendar.JDateChooser datePicker(String name) {
        var picker = new com.toedter.calendar.JDateChooser(); picker.setName(name); picker.setLocale(Locale.forLanguageTag("vi-VN"));
        picker.setDateFormatString("dd/MM/yyyy"); picker.setPreferredSize(new Dimension(135, 32));
        ((JTextField) picker.getDateEditor().getUiComponent()).setEditable(false); return picker;
    }
    private static BigDecimal tong(List<DoanhThuNgay> days, boolean cash) { return days.stream().map(d -> cash ? d.tienMat() : d.chuyenKhoan()).reduce(BigDecimal.ZERO, BigDecimal::add); }
    private static String khoang(KhoangNgay range) { return DATE.format(range.tu()) + " – " + DATE.format(range.den()); }
    private static String so(BigDecimal value, String pattern) { return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN"))).format(value); }
    private static String tien(BigDecimal value) { return so(value, "#,##0") + " ₫"; }
    private static JLabel label(String text, java.awt.Font font, Color color) { JLabel label = new JLabel(text); label.setFont(font); label.setForeground(color); return label; }
    private static JButton nut(String title, LineIcon.Type icon, boolean primary) { return PnlTongQuanQuanLy.button(title, icon, primary); }
    private static JPanel card() { JPanel panel = new SurfacePanel(new BorderLayout()); panel.setBorder(new EmptyBorder(14, 15, 14, 15)); return panel; }
    private static DefaultTableModel model(String[] names, Class<?>[] types) { return new DefaultTableModel(names, 0) { public boolean isCellEditable(int r, int c) { return false; } public Class<?> getColumnClass(int col) { return types[col]; } }; }
    private static JTable table(DefaultTableModel model) {
        JTable table = new JTable(model); table.setRowHeight(30); table.setFont(font(12)); table.setForeground(TEXT);
        table.setShowVerticalLines(false); table.setGridColor(BORDER); table.setAutoCreateRowSorter(true);
        table.getTableHeader().setFont(bold(11)); table.getTableHeader().setBackground(BACKGROUND); table.setFillsViewportHeight(true); return table;
    }
    private static JScrollPane tableScroll(JTable table) { JScrollPane scroll = new JScrollPane(table); scroll.setColumnHeaderView(table.getTableHeader()); return scroll; }
    private static void addRow(JPanel panel, Component component, int row, double weight) { GridBagConstraints g = new GridBagConstraints(); g.gridx = 0; g.gridy = row; g.weightx = 1; g.weighty = weight; g.fill = GridBagConstraints.BOTH; g.insets = new Insets(0, 0, row == 5 ? 0 : 14, 0); panel.add(component, g); }
    private static void pair(JPanel panel, Component left, Component right) { left.setMinimumSize(new Dimension(0, 0)); right.setMinimumSize(new Dimension(0, 0)); GridBagConstraints g = new GridBagConstraints(); g.gridx = 0; g.gridy = 0; g.weightx = 0.66; g.weighty = 1; g.fill = GridBagConstraints.BOTH; g.insets = new Insets(0, 0, 0, 14); panel.add(left, g); g.gridx = 1; g.weightx = 0.34; g.insets = new Insets(0, 0, 0, 0); panel.add(right, g); }

    private static final class ThanhKetQua extends JComponent {
        private KetQuaLich data = KetQuaLich.TRONG;
        void setData(KetQuaLich value) { data = value; repaint(); }
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics); Graphics2D g = (Graphics2D) graphics.create();
            g.setColor(BORDER); g.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
            if (data.daKetThuc() > 0) { int x = 0; long[] values = {data.hoanTat(), data.daHuy(), data.khongDen()}; Color[] colors = {TEAL_DARK, new Color(0xA46A00), new Color(0xCC4248)}; for (int i = 0; i < 3; i++) { int width = i == 2 ? getWidth() - x : (int) Math.round(getWidth() * values[i] / (double) data.daKetThuc()); g.setColor(colors[i]); g.fillRect(x, 0, width, getHeight()); x += width; } } g.dispose();
        }
    }

    private static final class BieuDoDoanhThu extends JComponent implements Scrollable {
        private List<DoanhThuNgay> current = List.of(), old = List.of();
        BieuDoDoanhThu() { setOpaque(true); setBackground(SURFACE); setToolTipText(""); }
        void setData(List<DoanhThuNgay> current, List<DoanhThuNgay> old) { this.current = current; this.old = old; setPreferredSize(new Dimension(Math.max(420, Math.max(current.size(), old.size()) * 42), 140)); revalidate(); repaint(); }
        private int count() { return Math.max(1, Math.max(current.size(), old.size())); }
        public Dimension getPreferredScrollableViewportSize() { return new Dimension(420, 140); }
        public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return 42; }
        public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return Math.max(42, r.width - 42); }
        public boolean getScrollableTracksViewportWidth() { return getParent() instanceof JViewport viewport && viewport.getWidth() >= getPreferredSize().width; }
        public boolean getScrollableTracksViewportHeight() { return true; }
        @Override public String getToolTipText(MouseEvent e) {
            int index = Math.min(count() - 1, Math.max(0, (int) (e.getX() / (getWidth() / (double) count()))));
            String now = index < current.size() ? DATE.format(current.get(index).ngay()) + ": " + tien(current.get(index).doanhThu()) : "Kỳ này không có ngày tương ứng";
            String prior = index < old.size() ? DATE.format(old.get(index).ngay()) + ": " + tien(old.get(index).doanhThu()) : "Kỳ trước không có ngày tương ứng";
            return "<html>" + now + "<br>" + prior + "</html>";
        }
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics); Graphics2D g = (Graphics2D) graphics.create();
            g.setColor(SURFACE); g.fillRect(0, 0, getWidth(), getHeight());
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double max = java.util.stream.Stream.concat(current.stream(), old.stream()).mapToDouble(d -> d.doanhThu().doubleValue()).max().orElse(0);
            int base = getHeight() - 23, available = Math.max(1, base - 23); double step = getWidth() / (double) count();
            g.setFont(font(9));
            for (int i = 0; i < count(); i++) {
                int width = Math.max(2, Math.min(16, (int) step / 3)); int x = (int) (i * step + step / 2);
                double previous = i < old.size() ? old.get(i).doanhThu().doubleValue() : 0;
                double value = i < current.size() ? current.get(i).doanhThu().doubleValue() : 0;
                int priorHeight = max == 0 ? 0 : (int) (previous / max * available), height = max == 0 ? 0 : (int) (value / max * available);
                g.setColor(new Color(0xB6DEDC)); g.fillRect(x - width - 2, base - priorHeight, width, priorHeight);
                g.setColor(TEAL_DARK); g.fillRect(x + 2, base - height, width, height);
                g.setColor(MUTED);
                if (i < current.size()) { String date = current.get(i).ngay().format(DateTimeFormatter.ofPattern("dd/MM")); g.drawString(date, x - g.getFontMetrics().stringWidth(date) / 2, base + 17); }
                if (value > 0) { String amount = so(BigDecimal.valueOf(value / 1_000_000), "0.##"); g.drawString(amount, x + 2, Math.max(11, base - height - 5)); }
            }
            if (max == 0) { g.setFont(font(12)); g.setColor(MUTED); g.drawString("Chưa có hóa đơn đã thanh toán trong hai kỳ.", 16, Math.max(26, base / 2)); }
            g.dispose();
        }
    }
}
