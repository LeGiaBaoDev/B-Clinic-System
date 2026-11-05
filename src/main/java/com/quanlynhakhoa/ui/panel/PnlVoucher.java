package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.dao.VoucherDAO;
import com.quanlynhakhoa.entity.Voucher;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.ui.component.*;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import com.quanlynhakhoa.ui.dialog.DlgVoucher;
import com.quanlynhakhoa.util.SessionManager;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.text.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** UC036–038. Dữ liệu tải nền; lọc, phân trang và xuất cùng một bản chụp. */
public class PnlVoucher extends JPanel {
    private static final Logger LOG = Logger.getLogger(PnlVoucher.class.getName());
    private static final int SO_DONG = 8;
    private static final String[] TRANG_THAI = {"Tất cả trạng thái", "Hoạt động", "Chưa hiệu lực", "Hết hạn", "Đã khóa"};
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    record DuLieu(List<Voucher> vouchers, Set<String> daSuDung) {
        DuLieu { vouchers = vouchers.stream().map(Voucher::new).toList(); daSuDung = Set.copyOf(daSuDung); }
    }
    private final Supplier<DuLieu> tai;
    private final Function<Voucher, Boolean> them, sua;
    private final Function<String, Boolean> khoa;
    private final Clock clock;
    final JTextField txtTim = new JTextField();
    final JTextField txtTrang = new JTextField("1", 3);
    final JComboBox<String> cboLoai = new JComboBox<>(new String[]{"Tất cả loại giảm giá", "Giảm phần trăm", "Giảm số tiền"});
    private int trangThaiDangChon;
    final JButton btnThem = button("Thêm voucher", LineIcon.Type.PLUS, true);
    final JButton btnXuat = button("Xuất danh sách", LineIcon.Type.INVOICE, false);
    final JButton btnTruoc = button("‹", null, false), btnSau = button("›", null, false);

    final JLabel lblKetQua = label("Chưa tải danh sách voucher.", MUTED), lblPhanTrang = label(" ", MUTED), lblTrang = label("1", TEAL);
    final JButton[] btnTab = new JButton[TRANG_THAI.length];
    private final DefaultTableModel model = new DefaultTableModel(new String[]{
        "MÃ / TÊN VOUCHER", "GIẢM GIÁ", "THỜI GIAN ÁP DỤNG", "GIẢM TỐI ĐA", "TRẠNG THÁI", "THAO TÁC"
    }, 0) {
        @Override public boolean isCellEditable(int row, int col) { return col == 5 && sanSang(); }
        @Override public Class<?> getColumnClass(int col) { return col == 1 || col == 3 ? Double.class : Object.class; }
    };
    final JTable tblVoucher = new JTable(model);
    private List<Voucher> tatCa = List.of(), daLoc = List.of(), trangHienTai = List.of();
    private Set<String> daSuDung = Set.of();
    private int trang;
    private boolean dangTai, dangGhi, dangXuat, daTai;
    private SwingWorker<DuLieu, Void> worker;
    private final javax.swing.Timer timKiem = new javax.swing.Timer(180, e -> loc(true));

    public PnlVoucher() {
        this(() -> {
            VoucherDAO dao = new VoucherDAO();
            return new DuLieu(dao.layTatCa(), dao.layMaDaSuDung());
        }, new VoucherDAO()::them, new VoucherDAO()::capNhat, new VoucherDAO()::xoa, Clock.systemDefaultZone());
    }

    PnlVoucher(Supplier<DuLieu> tai, Function<Voucher, Boolean> them,
            Function<Voucher, Boolean> sua, Function<String, Boolean> khoa, Clock clock) {
        super(new BorderLayout(0, 14));
        this.tai = tai; this.them = them; this.sua = sua; this.khoa = khoa; this.clock = clock;
        setName("Voucher"); setBackground(BACKGROUND); setBorder(new EmptyBorder(20, 22, 18, 22));
        JPanel header = new JPanel(new BorderLayout(12, 0)); header.setOpaque(false);
        JPanel titles = new JPanel(); titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS)); titles.setOpaque(false);
        JLabel title = label("Danh sách voucher", TEXT); title.setFont(bold(24)); titles.add(title);
        titles.add(Box.createVerticalStrut(4)); titles.add(label("Theo dõi ưu đãi và thời gian áp dụng của phòng khám.", MUTED));
        header.add(titles, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5)); actions.setOpaque(false);
        actions.add(btnXuat); actions.add(btnThem); header.add(actions, BorderLayout.EAST); add(header, BorderLayout.NORTH);
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 12)); card.setBorder(new EmptyBorder(16, 18, 14, 18));
        JPanel filters = new JPanel(); filters.setOpaque(false); filters.setLayout(new BoxLayout(filters, BoxLayout.Y_AXIS));
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)); tabs.setOpaque(false);
        for (int i = 0; i < btnTab.length; i++) {
            final int index = i;
            btnTab[i] = button(i == 0 ? "Tất cả" : TRANG_THAI[i], null, false);
            btnTab[i].putClientProperty("JComponent.minimumHeight", 28);
            btnTab[i].setContentAreaFilled(false);
            btnTab[i].addActionListener(e -> { trangThaiDangChon = index; loc(true); }); tabs.add(btnTab[i]);
        }
        filters.add(tabs); filters.add(Box.createVerticalStrut(10));
        JPanel search = new JPanel(new BorderLayout(10, 0)); search.setOpaque(false);
        txtTim.putClientProperty("JTextField.placeholderText", "Tìm theo mã hoặc tên voucher...");
        txtTim.putClientProperty("JTextField.leadingIcon", new LineIcon(LineIcon.Type.SEARCH, MUTED, 16));
        txtTim.setPreferredSize(new Dimension(260, 36)); search.add(txtTim, BorderLayout.CENTER);
        JPanel selects = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); selects.setOpaque(false);
        cboLoai.setPreferredSize(new Dimension(175, 36));
        selects.add(cboLoai); search.add(selects, BorderLayout.EAST);
        filters.add(search); filters.add(Box.createVerticalStrut(10)); filters.add(lblKetQua);
        card.add(filters, BorderLayout.NORTH);
        taoBang();
        JScrollPane scroll = new JScrollPane(tblVoucher); scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(SURFACE); card.add(scroll, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout()); footer.setOpaque(false); footer.add(lblPhanTrang, BorderLayout.WEST);
        JPanel pages = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); pages.setOpaque(false);
        txtTrang.setName("trangVoucher"); txtTrang.setHorizontalAlignment(JTextField.CENTER);
        txtTrang.setPreferredSize(new Dimension(48, 35));
        txtTrang.setToolTipText("Nhập số trang rồi nhấn Enter; Esc trở về trang hiện tại");
        txtTrang.getAccessibleContext().setAccessibleName("Số trang voucher");
        pages.add(label("8 dòng / trang", MUTED)); pages.add(btnTruoc);
        pages.add(txtTrang); pages.add(lblTrang); pages.add(btnSau);
        footer.add(pages, BorderLayout.EAST); card.add(footer, BorderLayout.SOUTH); add(card, BorderLayout.CENTER);
        JLabel note = label("Lưu ý: Mỗi hóa đơn chỉ áp dụng 1 voucher. Voucher đã dùng giữ nguyên nội dung; khóa không làm mất lịch sử.", TEAL_DARK);
        note.setOpaque(true); note.setBackground(TEAL_LIGHT); note.setBorder(new EmptyBorder(12, 10, 12, 10)); add(note, BorderLayout.SOUTH);
        timKiem.setRepeats(false);
        txtTim.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { timKiem.restart(); }
            public void removeUpdate(DocumentEvent e) { timKiem.restart(); }
            public void changedUpdate(DocumentEvent e) { timKiem.restart(); }
        });
        txtTim.addActionListener(e -> { timKiem.stop(); loc(true); });
        cboLoai.addActionListener(e -> loc(true));

        btnThem.addActionListener(e -> { if (sanSang()) moForm(null); });
        btnXuat.addActionListener(e -> xuat());
        btnTruoc.addActionListener(e -> { trang--; hienTrang(); }); btnSau.addActionListener(e -> { trang++; hienTrang(); });
        txtTrang.addActionListener(e -> chuyenDenTrang());
        txtTrang.registerKeyboardAction(e -> hienTrang(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), WHEN_FOCUSED);
        registerKeyboardAction(e -> lamMoiDuLieu(), KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), WHEN_IN_FOCUSED_WINDOW);
        registerKeyboardAction(e -> txtTim.requestFocusInWindow(), KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), WHEN_IN_FOCUSED_WINDOW);
        capNhatNut();
    }

    private static JLabel label(String text, Color color) {
        JLabel label = new JLabel(text); label.setFont(font(12)); label.setForeground(color); return label;
    }
    private static JButton button(String text, LineIcon.Type icon, boolean primary) {
        return PnlTongQuanQuanLy.button(text, icon, primary);
    }
    private boolean laQuanLy() {
        var user = SessionManager.getInstance().getCurrentUser();
        return user != null && user.getVaiTro() == VaiTro.QUANLY;
    }
    private boolean sanSang() { return laQuanLy() && daTai && !dangTai && !dangGhi; }

    public void lamMoiDuLieu() {
        if (dangGhi) return;
        if (!laQuanLy()) {
            daTai = false; tatCa = List.of(); daSuDung = Set.of(); loc(true);
            lblKetQua.setText("Chỉ Quản lý được phép quản lý voucher."); capNhatNut(); return;
        }
        if (worker != null) worker.cancel(true);
        dangTai = true; lblKetQua.setText("Đang tải danh sách voucher..."); capNhatNut();
        worker = new SwingWorker<>() {
            @Override protected DuLieu doInBackground() { return tai.get(); }
            @Override protected void done() {
                if (this != worker || isCancelled()) return;
                try {
                    DuLieu value = get(); tatCa = value.vouchers(); daSuDung = value.daSuDung(); daTai = true;
                    dangTai = false; loc(false);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); thatBai("Tải dữ liệu bị gián đoạn.");
                } catch (ExecutionException | RuntimeException e) {
                    Throwable cause = e instanceof ExecutionException ? e.getCause() : e;
                    thatBai(thongDiep(cause, "Không thể tải danh sách voucher. Nhấn F5 để thử lại."));
                } finally { dangTai = false; capNhatNut(); }
            }
        };
        worker.execute();
    }

    private void thatBai(String message) {
        daTai = false; tatCa = List.of(); daSuDung = Set.of(); loc(true); lblKetQua.setText(message);
    }

    static int trangThai(Voucher v, LocalDate day) {
        if (!v.isTrangThai()) return 4;
        if (day.isBefore(v.getNgayBatDau())) return 2;
        if (day.isAfter(v.getNgayKetThuc())) return 3;
        return 1;
    }
    private static String khongDau(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd');
    }
    private void loc(boolean veDau) {
        if (veDau) trang = 0;
        String keyword = khongDau(txtTim.getText().trim()); LocalDate day = LocalDate.now(clock);
        int type = cboLoai.getSelectedIndex(), state = trangThaiDangChon;
        daLoc = tatCa.stream().filter(v -> khongDau(v.getMaVoucher() + " " + v.getTenVoucher()).contains(keyword))
                .filter(v -> type == 0 || v.isLoaiGiam() == (type == 1))
                .filter(v -> state == 0 || trangThai(v, day) == state).toList();
        for (int i = 0; i < btnTab.length; i++) {
            final int status = i;
            long count = tatCa.stream().filter(v -> status == 0 || trangThai(v, day) == status).count();
            btnTab[i].setText((i == 0 ? "Tất cả" : TRANG_THAI[i]) + " (" + count + ")");
            btnTab[i].setBackground(state == i ? TEAL_LIGHT : SURFACE);
            btnTab[i].setForeground(state == i ? TEAL : MUTED);
            btnTab[i].setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 0, state == i ? TEAL : BORDER), new EmptyBorder(6, 10, 6, 10)));
        }
        if (!dangTai && daTai) lblKetQua.setText(daLoc.size() + " voucher · "
                + tatCa.stream().filter(v -> trangThai(v, day) == 1).count() + " đang hoạt động"
                + (daLoc.isEmpty() ? " — Không có voucher phù hợp." : ""));
        hienTrang();
    }

    private void hienTrang() {
        if (tblVoucher.isEditing()) tblVoucher.getCellEditor().cancelCellEditing();
        trang = Math.max(0, Math.min(trang, Math.max(0, (daLoc.size() - 1) / SO_DONG)));
        int from = Math.min(trang * SO_DONG, daLoc.size()), to = Math.min(from + SO_DONG, daLoc.size());
        trangHienTai = daLoc.subList(from, to); model.setRowCount(0);
        for (Voucher v : trangHienTai) model.addRow(new Object[]{v, v.getGiaGiam(), v.getNgayBatDau(),
                v.getGiaTriGiamToiDa(), TRANG_THAI[trangThai(v, LocalDate.now(clock))], v});
        lblPhanTrang.setText(daLoc.isEmpty() ? "Không có voucher để hiển thị" : "Hiển thị " + (from + 1) + "–" + to + " trong tổng số " + daLoc.size() + " voucher");
        txtTrang.setText(String.valueOf(trang + 1)); txtTrang.putClientProperty("JComponent.outline", null);
        lblTrang.setText("/ " + Math.max(1, (daLoc.size() + SO_DONG - 1) / SO_DONG)); capNhatNut();
    }

    private void chuyenDenTrang() {
        if (!sanSang() || daLoc.isEmpty()) return;
        int total = (daLoc.size() + SO_DONG - 1) / SO_DONG;
        try {
            String text = txtTrang.getText().trim();
            if (!text.matches("[0-9]+")) throw new NumberFormatException();
            int page = Integer.parseInt(text);
            if (page < 1 || page > total) throw new NumberFormatException();
            trang = page - 1; hienTrang(); txtTrang.selectAll();
        } catch (NumberFormatException e) {
            txtTrang.putClientProperty("JComponent.outline", "error");
            lblPhanTrang.setText("Nhập số trang từ 1 đến " + total + ".");
            txtTrang.requestFocusInWindow(); txtTrang.selectAll();
        }
    }

    private void capNhatNut() {
        boolean ready = sanSang(); btnThem.setEnabled(ready); btnXuat.setEnabled(ready && !dangXuat && !daLoc.isEmpty());
        btnTruoc.setEnabled(ready && trang > 0); btnSau.setEnabled(ready && (trang + 1) * SO_DONG < daLoc.size());
        txtTrang.setEnabled(ready && !daLoc.isEmpty());
        tblVoucher.setEnabled(ready);
    }

    private void taoBang() {
        tblVoucher.setRowHeight(48); tblVoucher.setShowVerticalLines(false); tblVoucher.setGridColor(BORDER);
        tblVoucher.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); tblVoucher.setFillsViewportHeight(true);
        tblVoucher.setBackground(SURFACE); tblVoucher.setForeground(TEXT); tblVoucher.setSelectionBackground(TEAL_LIGHT);
        tblVoucher.getTableHeader().setFont(bold(11)); tblVoucher.getTableHeader().setForeground(MUTED);
        tblVoucher.getTableHeader().setPreferredSize(new Dimension(0, 38));
        tblVoucher.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {230, 115, 200, 135, 125, 175};
        for (int i = 0; i < widths.length; i++) tblVoucher.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        sorter.setComparator(0, Comparator.comparing(v -> ((Voucher) v).getMaVoucher())); sorter.setSortable(5, false);
        tblVoucher.setRowSorter(sorter);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                    boolean focus, int row, int col) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, col);
                Voucher v = trangHienTai.get(table.convertRowIndexToModel(row));
                setFont(font(12)); setForeground(TEXT); setBorder(new EmptyBorder(4, 12, 4, 8));
                setText(switch (col) {
                    case 0 -> "<html><b>" + html(v.getMaVoucher()) + "</b><br><font color='#7B8BA5'>" + html(v.getTenVoucher()) + "</font></html>";
                    case 1 -> giaGiam(v);
                    case 2 -> NGAY.format(v.getNgayBatDau()) + " – " + NGAY.format(v.getNgayKetThuc());
                    case 3 -> v.isLoaiGiam() && v.getGiaTriGiamToiDa() > 0 ? tien(v.getGiaTriGiamToiDa()) : "—";
                    default -> String.valueOf(value);
                });
                if (col == 4) setForeground(switch (trangThai(v, LocalDate.now(clock))) {
                    case 1 -> GREEN; case 4 -> new Color(0xBC4141); default -> MUTED;
                });
                return this;
            }
        };
        for (int i = 0; i < 5; i++) tblVoucher.getColumnModel().getColumn(i).setCellRenderer(renderer);
        tblVoucher.getColumnModel().getColumn(4).setCellRenderer(new TableCellRenderer() {
            private final JPanel background = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 11));
            private final JLabel chip = label("", GREEN);
            { chip.setOpaque(true); chip.setFont(font(11)); chip.setBorder(new EmptyBorder(3, 7, 3, 7)); background.add(chip); }
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int col) {
                Voucher v = trangHienTai.get(table.convertRowIndexToModel(row)); int status = trangThai(v, LocalDate.now(clock));
                chip.setText(TRANG_THAI[status]); chip.setForeground(switch (status) { case 1 -> GREEN; case 2 -> new Color(0x2563A6); case 3 -> new Color(0x956000); default -> new Color(0xBC4141); });
                chip.setBackground(switch (status) { case 1 -> new Color(0xE3F4EA); case 2 -> new Color(0xEAF2FF); case 3 -> new Color(0xFFF3D6); default -> new Color(0xFFF0F0); });
                background.setBackground(selected ? TEAL_LIGHT : SURFACE); return background;
            }
        });
        tblVoucher.getColumnModel().getColumn(5).setCellRenderer(new ActionCell());
        tblVoucher.getColumnModel().getColumn(5).setCellEditor(new ActionCell());
        tblVoucher.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = tblVoucher.rowAtPoint(e.getPoint()), col = tblVoucher.columnAtPoint(e.getPoint());
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2 && row >= 0 && col != 5 && sanSang()) {
                    moForm(trangHienTai.get(tblVoucher.convertRowIndexToModel(row)));
                }
            }
        });
    }

    static String html(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    private static String tien(double value) { return new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN"))).format(value) + " ₫"; }
    private static String giaGiam(Voucher v) { return v.isLoaiGiam() ? soThapPhan(v.getGiaGiam()) + "%" : tien(v.getGiaGiam()); }
    private static String soThapPhan(double value) { return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString().replace('.', ','); }

    private final class ActionCell extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 7));
        private final JButton edit = new JButton(), toggle = new JButton();
        private Voucher current;
        ActionCell() {
            for (JButton b : new JButton[]{edit, toggle}) {
                b.setFont(font(12)); b.setForeground(TEAL); b.setBorder(new EmptyBorder(7, 7, 7, 7));
                b.setContentAreaFilled(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); panel.add(b);
            }
            edit.addActionListener(e -> { Voucher v = current; fireEditingStopped(); if (sanSang()) moForm(v); });
            toggle.addActionListener(e -> { Voucher v = current; fireEditingStopped(); doiTrangThai(v); });
        }
        private Component render(Voucher v, boolean selected) {
            current = v; boolean used = daSuDung.contains(v.getMaVoucher());
            edit.setText(used ? "Chi tiết" : "Sửa"); toggle.setText(v.isTrangThai() ? "Khóa" : "Mở khóa");
            toggle.setVisible(v.isTrangThai() || !used); edit.setEnabled(sanSang()); toggle.setEnabled(sanSang());
            panel.setBackground(selected ? TEAL_LIGHT : SURFACE); return panel;
        }
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int row, int col) { return render((Voucher) v, s); }
        public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int row, int col) { return render((Voucher) v, true); }
        public Object getCellEditorValue() { return current; }
    }

    void moForm(Voucher value) {
        if (!sanSang()) return;
        new DlgVoucher(SwingUtilities.getWindowAncestor(this), value,
                value != null && daSuDung.contains(value.getMaVoucher()), value == null ? them : sua,
                v -> { txtTim.setText(""); cboLoai.setSelectedIndex(0); trangThaiDangChon = 0; trang = 0; lamMoiDuLieu(); }).setVisible(true);
    }

    boolean xacNhan(String message) {
        return JOptionPane.showConfirmDialog(this, message, "Xác nhận voucher", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }
    void doiTrangThai(Voucher value) {
        if (!sanSang() || value == null) return;
        if (!value.isTrangThai() && daSuDung.contains(value.getMaVoucher())) return;
        boolean lock = value.isTrangThai();
        if (!xacNhan((lock ? "Khóa" : "Mở khóa") + " voucher " + value.getMaVoucher()
                + "?" + (lock ? "\nVoucher sẽ không thể áp dụng cho thanh toán mới. Hóa đơn cũ được giữ nguyên." : ""))) return;
        dangGhi = true; capNhatNut(); lblKetQua.setText("Đang cập nhật voucher...");
        Voucher copy = new Voucher(value); copy.setTrangThai(!lock);
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() { return lock ? khoa.apply(value.getMaVoucher()) : sua.apply(copy); }
            @Override protected void done() {
                boolean success = false;
                try { if (!get()) throw new BusinessException("Voucher không còn tồn tại. Hãy tải lại danh sách."); success = true; }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); lblKetQua.setText("Thao tác bị gián đoạn. Nhấn F5 để kiểm tra."); }
                catch (ExecutionException | RuntimeException e) {
                    Throwable cause = e instanceof ExecutionException ? e.getCause() : e;
                    lblKetQua.setText(thongDiep(cause, "Không thể cập nhật voucher. Vui lòng thử lại."));
                } finally { dangGhi = false; capNhatNut(); }
                if (success) lamMoiDuLieu();
            }
        }.execute();
    }

    private static String thongDiep(Throwable cause, String fallback) {
        LOG.log(Level.WARNING, fallback, cause);
        return cause instanceof BusinessException ? cause.getMessage() : fallback;
    }

    private void xuat() {
        if (!sanSang() || dangXuat || daLoc.isEmpty()) return;
        JFileChooser chooser = new JFileChooser(); chooser.setDialogTitle("Xuất danh sách voucher đang lọc");
        chooser.setFileFilter(new FileNameExtensionFilter("Excel (*.xlsx)", "xlsx")); chooser.setSelectedFile(new java.io.File("voucher.xlsx"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path selected = chooser.getSelectedFile().toPath();
        Path file = selected.toString().toLowerCase(Locale.ROOT).endsWith(".xlsx") ? selected : Path.of(selected + ".xlsx");
        if (Files.exists(file) && !xacNhan("Ghi đè tệp " + file.getFileName() + "?")) return;
        List<Voucher> snapshot = daLoc.stream().map(Voucher::new).toList(); LocalDate day = LocalDate.now(clock);
        dangXuat = true; capNhatNut(); lblKetQua.setText("Đang xuất danh sách...");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception { ghiExcel(file, snapshot, day); return null; }
            @Override protected void done() {
                try { get(); lblKetQua.setText("Đã xuất " + snapshot.size() + " voucher vào " + file.getFileName()); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); lblKetQua.setText("Xuất danh sách bị gián đoạn."); }
                catch (ExecutionException e) { lblKetQua.setText(thongDiep(e.getCause(), "Không thể xuất danh sách. Kiểm tra tệp đang mở hoặc quyền ghi.")); }
                finally { dangXuat = false; capNhatNut(); }
            }
        }.execute();
    }

    static void ghiExcel(Path file, List<Voucher> values, LocalDate day) throws Exception {
        // Ghi file tạm cùng thư mục trước, tránh làm hỏng file cũ nếu xuất thất bại.
        Path temp = Files.createTempFile(file.toAbsolutePath().getParent(), "voucher-", ".xlsx");
        try {
            try (XSSFWorkbook book = new XSSFWorkbook()) {
                var sheet = book.createSheet("Voucher");
                String[] headers = {"Mã voucher", "Tên voucher", "Loại giảm", "Giá trị giảm", "Giảm tối đa (₫)", "Ngày bắt đầu", "Ngày kết thúc", "Trạng thái"};
                var header = sheet.createRow(0);
                for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
                for (int i = 0; i < values.size(); i++) {
                    Voucher v = values.get(i); var row = sheet.createRow(i + 1);
                    row.createCell(0).setCellValue(v.getMaVoucher()); row.createCell(1).setCellValue(v.getTenVoucher());
                    row.createCell(2).setCellValue(v.isLoaiGiam() ? "Phần trăm (%)" : "Số tiền (₫)");
                    row.createCell(3).setCellValue(v.getGiaGiam());
                    if (v.isLoaiGiam() && v.getGiaTriGiamToiDa() > 0) row.createCell(4).setCellValue(v.getGiaTriGiamToiDa());
                    row.createCell(5).setCellValue(NGAY.format(v.getNgayBatDau())); row.createCell(6).setCellValue(NGAY.format(v.getNgayKetThuc()));
                    row.createCell(7).setCellValue(TRANG_THAI[trangThai(v, day)]);
                }
                sheet.createFreezePane(0, 1); for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
                try (var output = Files.newOutputStream(temp)) { book.write(output); }
            }
            Files.move(temp, file.toAbsolutePath(), StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }

    @Override public void removeNotify() {
        timKiem.stop(); if (worker != null) { worker.cancel(true); worker = null; }
        dangTai = false; super.removeNotify();
    }
}
