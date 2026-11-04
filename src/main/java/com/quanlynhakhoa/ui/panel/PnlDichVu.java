package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.dao.DichVuDAO;
import com.quanlynhakhoa.entity.DichVu;
import com.quanlynhakhoa.entity.LoaiDichVu;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.MucThue;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import com.quanlynhakhoa.ui.dialog.DlgDichVu;
import com.quanlynhakhoa.util.SessionManager;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
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
 * UC006: xem và tra cứu danh mục dịch vụ kèm bảng giá, mức thuế GTGT. Mọi vai trò được xem.
 *
 * <p>Bố cục theo thiết kế Figma "Danh sách dịch vụ", riêng các chỗ lệch báo cáo thì theo báo cáo
 * (quyết định của Dương 04/11): mã DV001, "Danh mục" = 3 phân loại của báo cáo (mỗi loại một mức
 * thuế), thuế 0% hiện "KCT", chữ "Đang cung cấp / Ngừng cung cấp", mặc định "Đang cung cấp" (UC006 bước 2).
 * Thanh lọc: ô tìm kiếm -> Tất cả danh mục -> Tất cả trạng thái (đồng bộ với 3 tab) -> Xóa lọc.</p>
 *
 * <p>Cách lấy dữ liệu: mỗi lần tìm, panel hỏi DAO toàn bộ dịch vụ khớp từ khóa + phân loại (mọi
 * trạng thái), rồi tự chia theo tab và phân trang trên máy. Nhờ vậy số đếm trên 3 tab luôn đúng mà
 * chỉ cần một truy vấn. Danh mục dịch vụ của phòng khám nhỏ (vài chục dòng) nên cách này đủ nhanh.</p>
 *
 * <p>UC005: Quản lý bấm "Thêm dịch vụ" ở góc phải tiêu đề để mở DlgDichVu.</p>
 *
 * <p>Panel không viết SQL; dữ liệu chỉ đi qua DichVuDAO, DAO kiểm tra quyền lại từ DB.</p>
 */
public class PnlDichVu extends JPanel {

    /** Nguồn dữ liệu tra cứu; tách ra để test giao diện không cần DB. */
    @FunctionalInterface
    interface TimDichVu {
        List<DichVu> tim(String tuKhoa, Boolean trangThai, MucThue mucThue);
    }

    /** Ba tab trạng thái; giá trị lọc null = tất cả. */
    enum Tab {
        TAT_CA("Tất cả", null), DANG_CUNG_CAP("Đang cung cấp", Boolean.TRUE), NGUNG_CUNG_CAP("Ngừng cung cấp", Boolean.FALSE);

        final String ten;
        final Boolean trangThai;

        Tab(String ten, Boolean trangThai) {
            this.ten = ten;
            this.trangThai = trangThai;
        }

        boolean khop(DichVu dv) {
            return trangThai == null || trangThai == dv.isTrangThai();
        }
    }

    static final int SO_DONG_MOI_TRANG = 10; // tài liệu không quy định; nhóm chọn 10 (04/11)
    private static final Logger LOG = Logger.getLogger(PnlDichVu.class.getName());
    private static final Color DO = new Color(0xC94C48);
    private static final Color NEN_HEADER = new Color(0xF7F9FB);
    private static final Color XANH_NHAT = new Color(0xE3F4EA);
    private static final Color XAM_NHAT = new Color(0xEEF1F4);
    /*
     * "Danh mục" trên giao diện chính là 3 phân loại dịch vụ của báo cáo (Bảng 4.5); mỗi danh mục
     * gắn cố định một mức thuế, nên lọc theo danh mục = lọc theo mức thuế GTGT của loại.
     */
    private static final String[] TEN_DANH_MUC = {"Tất cả danh mục", LoaiDichVu.DIEU_TRI,
        LoaiDichVu.VAT_TU, LoaiDichVu.THAM_MY};
    private static final MucThue[] GIA_TRI_DANH_MUC = {null, MucThue.THUE_0, MucThue.THUE_5, MucThue.THUE_10};
    /** Cùng thứ tự với enum Tab, để chỉ số của combobox = ordinal của tab. */
    private static final String[] TEN_TRANG_THAI = {"Tất cả trạng thái", "Đang cung cấp", "Ngừng cung cấp"};
    // Thứ tự cột của bảng (dùng chung cho model, renderer, độ rộng).
    static final int COT_MA = 0, COT_TEN = 1, COT_DANH_MUC = 2, COT_THOI_LUONG = 3, COT_DON_GIA = 4,
            COT_THUE = 5, COT_TRANG_THAI = 6;

    private final TimDichVu timDichVu;

    // --- Thanh tiêu đề ---
    /** Chỗ đặt nút hành động ở góc phải tiêu đề. */
    final JPanel pnlHanhDong = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    /** UC005: chỉ Quản lý thấy; DAO vẫn kiểm tra quyền lại từ DB. */
    private final JButton btnThem = PnlTongQuanQuanLy.button("Thêm dịch vụ", LineIcon.Type.PLUS, true);

    // --- Tab, bộ lọc ---
    private final JButton[] btnTab = new JButton[Tab.values().length];
    private final JTextField txtTimKiem = new JTextField();
    private final JComboBox<String> cboDanhMuc = new JComboBox<>(TEN_DANH_MUC);
    /** Lọc trạng thái dạng dropdown; luôn đồng bộ với 3 tab phía trên (chọn bên nào bên kia đổi theo). */
    private final JComboBox<String> cboTrangThai = new JComboBox<>(TEN_TRANG_THAI);
    /** true khi đang đồng bộ tab <-> dropdown trạng thái bằng code, để hai bên không gọi lẫn nhau mãi. */
    private boolean dangDongBoTrangThai;
    private final JButton btnXoaLoc = PnlTongQuanQuanLy.button("Xóa lọc", null, false);
    /** Gõ xong 300 ms mới tìm, tránh gọi DB theo từng phím. */
    private final Timer hoanTimKiem = new Timer(300, e -> lamMoiDuLieu());
    private final JLabel lblKetQua = new JLabel("Chưa tải danh mục dịch vụ.");
    private final JLabel lblGhiChuGia = new JLabel("Đơn giá chưa bao gồm thuế GTGT");

    // --- Bảng + phân trang ---
    private final DefaultTableModel modelDichVu = new DefaultTableModel(new String[]{
        "MÃ", "TÊN DỊCH VỤ", "DANH MỤC", "THỜI LƯỢNG", "ĐƠN GIÁ", "THUẾ GTGT", "TRẠNG THÁI"
    }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tblDichVu = new JTable(modelDichVu);
    private final JLabel lblPhanTrang = new JLabel(" ");
    private final JPanel pnlSoTrang = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));

    // --- Khung chi tiết (chỉ hiện khi chọn một dòng) ---
    private final SurfacePanel pnlChiTiet = new SurfacePanel(new BorderLayout(12, 0));
    private final JLabel lblChiTiet = new JLabel();

    // --- Trạng thái dữ liệu ---
    /** Kết quả DAO trả về cho từ khóa + phân loại hiện tại (mọi trạng thái). */
    private List<DichVu> tatCa = List.of();
    /** Các dịch vụ đang hiển thị trên trang hiện tại, cùng thứ tự với các dòng của bảng. */
    private List<DichVu> trangHienTai = List.of();
    private Tab tabDangChon = Tab.DANG_CUNG_CAP; // UC006 bước 2: mặc định "Đang cung cấp"
    private int trang = 0;                      // trang hiện tại, đếm từ 0
    private SwingWorker<List<DichVu>, Void> worker;
    /** Đặt true khi panel tự đổi bộ lọc bằng code, để không kích hoạt tải lại nhiều lần. */
    private boolean dangDatLaiBoLoc;

    public PnlDichVu() {
        this(new DichVuDAO()::timKiem);
    }

    PnlDichVu(TimDichVu timDichVu) {
        super(new BorderLayout(0, 16));
        this.timDichVu = Objects.requireNonNull(timDichVu);
        setName("Dịch vụ");
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 24, 24));
        add(taoTieuDe(), BorderLayout.NORTH);
        add(taoTheDanhSach(), BorderLayout.CENTER);
        add(taoChiTiet(), BorderLayout.SOUTH);
        ganSuKien();
    }

    // ===================== Dựng giao diện =====================

    private JPanel taoTieuDe() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        JPanel chu = new JPanel();
        chu.setOpaque(false);
        chu.setLayout(new BoxLayout(chu, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Danh sách dịch vụ");
        title.setFont(bold(24));
        title.setForeground(TEXT);
        JLabel sub = new JLabel("Quản lý danh mục điều trị, thời lượng và bảng giá dịch vụ.");
        sub.setFont(font(13));
        sub.setForeground(MUTED);
        chu.add(title);
        chu.add(Box.createVerticalStrut(4));
        chu.add(sub);
        header.add(chu, BorderLayout.CENTER);
        pnlHanhDong.setOpaque(false);
        btnThem.setName("themDichVu");
        btnThem.setPreferredSize(new Dimension(150, 40));
        btnThem.setVisible(laQuanLy());
        pnlHanhDong.add(btnThem);
        // Bọc thêm 1 lớp để nút nằm giữa theo chiều dọc so với 2 dòng tiêu đề.
        JPanel phai = new JPanel(new GridBagLayout());
        phai.setOpaque(false);
        phai.add(pnlHanhDong);
        header.add(phai, BorderLayout.EAST);
        return header;
    }

    /** Thẻ trắng chứa: tab trạng thái, bộ lọc, dòng tóm tắt, bảng, phân trang. */
    private JPanel taoTheDanhSach() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel tren = new JPanel();
        tren.setOpaque(false);
        tren.setLayout(new BoxLayout(tren, BoxLayout.Y_AXIS));
        tren.add(taoThanhTab());
        tren.add(Box.createVerticalStrut(14));
        tren.add(taoThanhLoc());
        tren.add(Box.createVerticalStrut(12));
        tren.add(taoDongTomTat());
        card.add(tren, BorderLayout.NORTH);
        card.add(taoBang(), BorderLayout.CENTER);
        card.add(taoThanhPhanTrang(), BorderLayout.SOUTH);
        return card;
    }

    private JPanel taoThanhTab() {
        // Hàng tab có đường kẻ mảnh phía dưới; tab đang chọn có gạch chân màu teal.
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabs.setOpaque(false);
        tabs.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        tabs.setAlignmentX(LEFT_ALIGNMENT);
        String[] ten = {"tabTatCa", "tabDangCungCap", "tabNgungCungCap"};
        for (Tab tab : Tab.values()) {
            JButton btn = new JButton(tab.ten);
            btn.setName(ten[tab.ordinal()]);
            btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> chonTab(tab));
            btnTab[tab.ordinal()] = btn;
            if (tab.ordinal() > 0) {
                tabs.add(Box.createHorizontalStrut(24)); // khoảng cách giữa các tab
            }
            tabs.add(btn);
        }
        veTab();
        return tabs;
    }

    private JPanel taoThanhLoc() {
        JPanel loc = new JPanel(new BorderLayout(10, 0));
        loc.setOpaque(false);
        loc.setAlignmentX(LEFT_ALIGNMENT);
        txtTimKiem.setName("timDichVu");
        txtTimKiem.setFont(font(13));
        txtTimKiem.putClientProperty("JTextField.placeholderText", "Tìm theo mã hoặc tên dịch vụ...");
        txtTimKiem.putClientProperty("JTextField.leadingIcon", new LineIcon(LineIcon.Type.SEARCH, MUTED, 15));
        txtTimKiem.setPreferredSize(new Dimension(240, 38));
        loc.add(txtTimKiem, BorderLayout.CENTER);

        // Thứ tự theo thiết kế: ô tìm kiếm -> Tất cả danh mục -> Tất cả trạng thái -> Xóa lọc.
        JPanel phai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        phai.setOpaque(false);
        cboDanhMuc.setName("locDanhMuc");
        cboDanhMuc.setFont(font(13));
        cboDanhMuc.setPreferredSize(new Dimension(210, 38));
        cboDanhMuc.setToolTipText("Lọc theo danh mục; mỗi danh mục gắn một mức thuế GTGT (KCT / 5% / 10%).");
        cboTrangThai.setName("locTrangThai");
        cboTrangThai.setFont(font(13));
        cboTrangThai.setPreferredSize(new Dimension(180, 38));
        cboTrangThai.setSelectedIndex(tabDangChon.ordinal()); // mặc định "Đang cung cấp" như tab
        btnXoaLoc.setName("xoaLocDichVu");
        btnXoaLoc.setToolTipText("Xóa từ khóa, đưa bộ lọc về mặc định và tải lại (F5 chỉ tải lại).");
        btnXoaLoc.setPreferredSize(new Dimension(90, 38));
        phai.add(cboDanhMuc);
        phai.add(cboTrangThai);
        phai.add(btnXoaLoc);
        loc.add(phai, BorderLayout.EAST);
        return loc;
    }

    private JPanel taoDongTomTat() {
        JPanel dong = new JPanel(new BorderLayout());
        dong.setOpaque(false);
        dong.setAlignmentX(LEFT_ALIGNMENT);
        lblKetQua.setName("ketQuaDichVu");
        lblKetQua.setFont(font(12));
        lblKetQua.setForeground(MUTED);
        lblGhiChuGia.setFont(font(12));
        lblGhiChuGia.setForeground(MUTED);
        dong.add(lblKetQua, BorderLayout.WEST);
        dong.add(lblGhiChuGia, BorderLayout.EAST);
        return dong;
    }

    private JScrollPane taoBang() {
        tblDichVu.setName("danhSachDichVu");
        tblDichVu.setFont(font(13));
        tblDichVu.setRowHeight(45);
        tblDichVu.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblDichVu.setSelectionBackground(TEAL_LIGHT);
        tblDichVu.setSelectionForeground(TEXT);
        tblDichVu.setGridColor(BORDER);
        tblDichVu.setShowVerticalLines(false);
        tblDichVu.setIntercellSpacing(new Dimension(0, 1));
        tblDichVu.setFillsViewportHeight(true);
        tblDichVu.getTableHeader().setReorderingAllowed(false);
        tblDichVu.getTableHeader().setPreferredSize(new Dimension(0, 40));
        tblDichVu.getTableHeader().setDefaultRenderer(new HeaderRenderer());

        // Ô thường: chữ có lề trái; đơn giá định dạng tiền, thời lượng thêm "phút".
        DefaultTableCellRenderer thuong = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                    boolean focus, int row, int column) {
                Object hienThi = switch (table.convertColumnIndexToModel(column)) {
                    case COT_DON_GIA -> value instanceof Number n ? dinhDangTien(n.doubleValue()) : value;
                    case COT_THOI_LUONG -> value == null ? null : value + " phút";
                    default -> value;
                };
                super.getTableCellRendererComponent(table, hienThi, selected, false, row, column);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                setToolTipText(hienThi == null ? null : hienThi.toString());
                return this;
            }
        };
        tblDichVu.setDefaultRenderer(Object.class, thuong);
        tblDichVu.getColumnModel().getColumn(COT_TRANG_THAI).setCellRenderer(new BadgeRenderer());
        int[] widths = {80, 260, 190, 100, 120, 90, 140};
        for (int i = 0; i < widths.length; i++) {
            tblDichVu.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        JScrollPane scroll = new JScrollPane(tblDichVu);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(SURFACE);
        return scroll;
    }

    private JPanel taoThanhPhanTrang() {
        JPanel dong = new JPanel(new BorderLayout());
        dong.setOpaque(false);
        dong.setBorder(new EmptyBorder(4, 0, 0, 0));
        lblPhanTrang.setName("phanTrangDichVu");
        lblPhanTrang.setFont(font(12));
        lblPhanTrang.setForeground(MUTED);
        dong.add(lblPhanTrang, BorderLayout.WEST);
        pnlSoTrang.setName("soTrangDichVu");
        pnlSoTrang.setOpaque(false);
        dong.add(pnlSoTrang, BorderLayout.EAST);
        return dong;
    }

    /** Khung trắng bên dưới: ẩn khi chưa chọn dòng để bảng được kéo dài hết chiều cao. */
    private JPanel taoChiTiet() {
        pnlChiTiet.setName("khungChiTietDichVu");
        pnlChiTiet.setBorder(new EmptyBorder(16, 20, 16, 20));
        lblChiTiet.setName("chiTietDichVu");
        lblChiTiet.setFont(font(13));
        lblChiTiet.setForeground(TEXT);
        pnlChiTiet.add(lblChiTiet, BorderLayout.CENTER);
        pnlChiTiet.setVisible(false);
        return pnlChiTiet;
    }

    private void ganSuKien() {
        txtTimKiem.addActionListener(e -> lamMoiDuLieu()); // Enter: tìm ngay
        hoanTimKiem.setRepeats(false);
        txtTimKiem.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                hoanTimKiemNeuDuoc();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                hoanTimKiemNeuDuoc();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                hoanTimKiemNeuDuoc();
            }
        });
        // Đổi danh mục phải hỏi lại DB (lọc ở câu SQL); đổi trạng thái chỉ cần lọc lại trên máy.
        cboDanhMuc.addActionListener(e -> {
            if (!dangDatLaiBoLoc) {
                lamMoiDuLieu();
            }
        });
        cboTrangThai.addActionListener(e -> {
            if (!dangDongBoTrangThai && cboTrangThai.getSelectedIndex() >= 0) {
                chonTab(Tab.values()[cboTrangThai.getSelectedIndex()]);
            }
        });
        btnXoaLoc.addActionListener(e -> datLaiBoLoc());
        btnThem.addActionListener(e -> moThemDichVu());
        tblDichVu.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                hienChiTiet();
            }
        });
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "lamMoi");
        getActionMap().put("lamMoi", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                lamMoiDuLieu();
            }
        });
    }

    private void hoanTimKiemNeuDuoc() {
        if (!dangDatLaiBoLoc) {
            hoanTimKiem.restart();
        }
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

    /** UC006 bước 3–4: tải danh sách theo từ khóa + phân loại (chạy nền, không chặn giao diện). */
    public void lamMoiDuLieu() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::lamMoiDuLieu);
            return;
        }
        hoanTimKiem.stop();
        if (worker != null) {
            worker.cancel(true); // bỏ kết quả của lần tìm trước nếu chưa xong
        }
        String tuKhoa = txtTimKiem.getText().trim();
        MucThue mucThue = GIA_TRI_DANH_MUC[Math.max(0, cboDanhMuc.getSelectedIndex())];
        datDangTai(true);
        lblKetQua.setForeground(MUTED);
        lblKetQua.setText("Đang tải danh mục dịch vụ...");
        worker = new SwingWorker<>() {
            @Override
            protected List<DichVu> doInBackground() {
                // trangThai = null: lấy mọi trạng thái để đếm số trên cả 3 tab.
                return List.copyOf(timDichVu.tim(tuKhoa, null, mucThue));
            }

            @Override
            protected void done() {
                if (this != worker || isCancelled()) {
                    return;
                }
                try {
                    tatCa = get();
                    trang = 0;
                    hienThiTrang();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    tatCa = List.of();
                    hienThiTrang();
                    lblKetQua.setForeground(DO);
                    lblKetQua.setText(cause instanceof BusinessException ? cause.getMessage()
                            : "Không thể tải danh mục dịch vụ. Vui lòng thử lại.");
                    LOG.log(Level.WARNING, "Không thể tra cứu dịch vụ", cause);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    lblKetQua.setText("Đã dừng tải danh mục dịch vụ.");
                } finally {
                    worker = null;
                    datDangTai(false);
                }
            }
        };
        worker.execute();
    }

    /** Chia {@link #tatCa} theo tab đang chọn, cắt đúng trang hiện tại rồi đổ vào bảng. */
    private void hienThiTrang() {
        List<DichVu> theoTab = locTheoTab(tabDangChon);
        int soTrang = Math.max(1, (theoTab.size() + SO_DONG_MOI_TRANG - 1) / SO_DONG_MOI_TRANG);
        trang = Math.min(trang, soTrang - 1);
        int tu = trang * SO_DONG_MOI_TRANG;
        int den = Math.min(theoTab.size(), tu + SO_DONG_MOI_TRANG);

        tblDichVu.clearSelection();
        modelDichVu.setRowCount(0);
        trangHienTai = theoTab.subList(tu, den);
        for (DichVu dv : trangHienTai) {
            modelDichVu.addRow(new Object[]{dv.getMaDV(), dv.getTenDV(), dv.getLoaiDichVu().getTenLoaiDV(),
                dv.getThoiLuong(), dv.getDonGia(), tenMucThue(dv.getLoaiDichVu().getMucThueGTGT()),
                dv.isTrangThai() ? "Đang cung cấp" : "Ngừng cung cấp"});
        }
        veTab();

        lblKetQua.setForeground(MUTED);
        // UC006 luồng 4.1
        lblKetQua.setText(theoTab.isEmpty() ? "Không tìm thấy dịch vụ phù hợp."
                : theoTab.size() + " dịch vụ · Bảng giá tháng "
                + LocalDate.now().format(DateTimeFormatter.ofPattern("MM/yyyy")));
        lblPhanTrang.setText(theoTab.isEmpty() ? " "
                : "Hiển thị " + (tu + 1) + "–" + den + " trong tổng số " + theoTab.size() + " dịch vụ");
        veSoTrang(soTrang);
    }

    private List<DichVu> locTheoTab(Tab tab) {
        List<DichVu> ketQua = new ArrayList<>();
        for (DichVu dv : tatCa) {
            if (tab.khop(dv)) {
                ketQua.add(dv);
            }
        }
        return ketQua;
    }

    // ===================== Tab, phân trang, bộ lọc =====================

    /** Chọn tab (bấm tab hoặc đổi dropdown trạng thái): về trang 1 và lọc lại, không gọi DB. */
    private void chonTab(Tab tab) {
        datTab(tab);
        trang = 0;
        hienThiTrang();
    }

    /** Đổi tab đang chọn và kéo dropdown trạng thái theo cho khớp. */
    private void datTab(Tab tab) {
        tabDangChon = tab;
        dangDongBoTrangThai = true;
        try {
            cboTrangThai.setSelectedIndex(tab.ordinal());
        } finally {
            dangDongBoTrangThai = false;
        }
    }

    /** Cập nhật chữ (kèm số đếm) và kiểu của 3 tab. */
    private void veTab() {
        for (Tab tab : Tab.values()) {
            JButton btn = btnTab[tab.ordinal()];
            if (btn == null) {
                return; // đang dựng giao diện, tab chưa tạo xong
            }
            boolean chon = tab == tabDangChon;
            btn.setText(tab.ten + " (" + locTheoTab(tab).size() + ")");
            btn.setFont(chon ? bold(13) : font(13));
            btn.setForeground(chon ? TEAL : MUTED);
            // Tab chọn: gạch chân 2px teal; tab khác: chừa chỗ để chữ không bị nhảy.
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 0, chon ? TEAL : new Color(0, 0, 0, 0)),
                    new EmptyBorder(8, 2, 8, 2)));
        }
    }

    /** Vẽ lại nhóm nút trang: ‹ 1 2 3 › kèm chữ "10 dòng / trang". */
    private void veSoTrang(int soTrang) {
        pnlSoTrang.removeAll();
        JLabel soDong = new JLabel(SO_DONG_MOI_TRANG + " dòng / trang");
        soDong.setFont(font(12));
        soDong.setForeground(MUTED);
        pnlSoTrang.add(soDong);
        pnlSoTrang.add(nutTrang("‹", "trangTruoc", trang > 0, () -> denTrang(trang - 1), false));
        for (int i = 0; i < soTrang; i++) {
            int so = i;
            pnlSoTrang.add(nutTrang(String.valueOf(i + 1), "trang" + (i + 1), true, () -> denTrang(so), i == trang));
        }
        pnlSoTrang.add(nutTrang("›", "trangSau", trang < soTrang - 1, () -> denTrang(trang + 1), false));
        pnlSoTrang.revalidate();
        pnlSoTrang.repaint();
    }

    private JButton nutTrang(String chu, String ten, boolean bat, Runnable hanhDong, boolean dangChon) {
        JButton btn = PnlTongQuanQuanLy.button(chu, null, dangChon);
        btn.setName(ten);
        btn.setEnabled(bat);
        btn.setPreferredSize(new Dimension(34, 34));
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.addActionListener(e -> hanhDong.run());
        return btn;
    }

    private void denTrang(int trangMoi) {
        trang = trangMoi;
        hienThiTrang();
    }

    /** Nút "Xóa lọc": về điều kiện mặc định của UC006 bước 2 (Đang cung cấp, mọi danh mục). */
    private void datLaiBoLoc() {
        dangDatLaiBoLoc = true;
        try {
            txtTimKiem.setText("");
            cboDanhMuc.setSelectedIndex(0);
            datTab(Tab.DANG_CUNG_CAP);
        } finally {
            dangDatLaiBoLoc = false;
        }
        lamMoiDuLieu();
    }

    private void datDangTai(boolean dangTai) {
        btnXoaLoc.setEnabled(!dangTai);
        // Ẩn/hiện theo vai trò mỗi lần tải, phòng trường hợp đổi tài khoản mà panel vẫn giữ.
        btnThem.setVisible(laQuanLy());
    }

    private static boolean laQuanLy() {
        TaiKhoan actor = SessionManager.getInstance().getCurrentUser();
        return actor != null && actor.getVaiTro() == VaiTro.QUANLY;
    }

    // ===================== UC005 thêm dịch vụ =====================

    /** UC005 bước 1: mở form thêm dịch vụ. */
    private void moThemDichVu() {
        if (!laQuanLy()) {
            lblKetQua.setText("Chỉ Quản lý được thêm dịch vụ.");
            return;
        }
        new DlgDichVu(SwingUtilities.getWindowAncestor(this), this::daThemDichVu).setVisible(true);
    }

    /** UC005 bước 8: báo thành công và hiện dịch vụ mới trong danh sách. */
    void daThemDichVu(DichVu dv) {
        hienDichVuVuaLuu(dv);
        JOptionPane.showMessageDialog(this, "Thêm dịch vụ thành công: " + dv.getMaDV() + " – " + dv.getTenDV(),
                "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Về tab "Đang cung cấp", tìm theo mã dịch vụ vừa lưu để người dùng thấy ngay kết quả. */
    private void hienDichVuVuaLuu(DichVu dv) {
        dangDatLaiBoLoc = true; // chặn tải thừa do đổi ô tìm kiếm + combobox
        try {
            txtTimKiem.setText(dv.getMaDV());
            cboDanhMuc.setSelectedIndex(0);
            datTab(Tab.DANG_CUNG_CAP);
        } finally {
            dangDatLaiBoLoc = false;
        }
        lamMoiDuLieu();
    }

    // ===================== Chi tiết dòng chọn =====================

    /** Dịch vụ đang được chọn trên bảng, null nếu chưa chọn. */
    DichVu dichVuDangChon() {
        int row = tblDichVu.getSelectedRow();
        return row < 0 ? null : trangHienTai.get(tblDichVu.convertRowIndexToModel(row));
    }

    /** Chọn dòng: hiện khung trắng với tên + lộ trình; bỏ chọn: ẩn khung để bảng dài hết cỡ. */
    private void hienChiTiet() {
        DichVu dv = dichVuDangChon();
        pnlChiTiet.setVisible(dv != null);
        if (dv != null) {
            String loTrinh = dv.getLoTrinhDichVu();
            lblChiTiet.setText("<html><b>" + html(dv.getTenDV()) + " · " + html(dv.getMaDV()) + "</b><br>"
                    + (loTrinh == null ? "Lộ trình điều trị: Không có (thực hiện trong một buổi)"
                    : "Lộ trình điều trị:<br>" + html(loTrinh).replace("\n", "<br>")) + "</html>");
        }
        revalidate();
        repaint();
    }

    // ===================== Hiển thị dữ liệu =====================

    /** Thuế 0% hiển thị "KCT" (không chịu thuế) theo báo cáo. */
    static String tenMucThue(MucThue mucThue) {
        return switch (mucThue) {
            case THUE_0 -> "KCT";
            case THUE_5 -> "5%";
            case THUE_10 -> "10%";
        };
    }

    static String dinhDangTien(double soTien) {
        DecimalFormat format = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.of("vi", "VN")));
        return format.format(soTien) + " ₫";
    }

    private static String html(String text) {
        return text == null ? "—" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Tiêu đề cột: chữ in hoa nhỏ, màu xám, nền xám nhạt như thiết kế. */
    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean focus, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setFont(bold(11));
            setForeground(MUTED);
            setBackground(NEN_HEADER);
            setOpaque(true);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(0, 12, 0, 12)));
            return this;
        }
    }

    /** Ô trạng thái: nhãn bo tròn, xanh khi "Đang cung cấp", xám khi "Ngừng cung cấp". */
    private static final class BadgeRenderer extends JPanel implements TableCellRenderer {
        private final JLabel badge = new JLabel() {
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

        BadgeRenderer() {
            super(new FlowLayout(FlowLayout.LEFT, 12, 11));
            badge.setFont(bold(11));
            badge.setBorder(new EmptyBorder(3, 8, 3, 8));
            add(badge);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean focus, int row, int column) {
            boolean dang = "Đang cung cấp".equals(value);
            badge.setText(value == null ? "" : value.toString());
            badge.setBackground(dang ? XANH_NHAT : XAM_NHAT);
            badge.setForeground(dang ? GREEN : MUTED);
            setBackground(selected ? table.getSelectionBackground() : table.getBackground());
            return this;
        }
    }
}
