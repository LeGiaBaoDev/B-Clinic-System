package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.dao.DichVuDAO;
import com.quanlynhakhoa.entity.DichVu;
import com.quanlynhakhoa.entity.enums.MucThue;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * UC006: xem và tra cứu danh mục dịch vụ kèm bảng giá, mức thuế GTGT.
 * Mọi vai trò được xem; chỉ đọc dữ liệu qua DichVuDAO.
 */
public class PnlDichVu extends JPanel {

    /** Nguồn dữ liệu tra cứu; tách ra để test giao diện không cần DB. */
    @FunctionalInterface
    interface TimDichVu {
        List<DichVu> tim(String tuKhoa, Boolean trangThai, MucThue mucThue);
    }

    private static final Logger LOG = Logger.getLogger(PnlDichVu.class.getName());
    private static final Color DO = new Color(0xC94C48);
    private static final String[] TEN_TRANG_THAI = {"Đang cung cấp", "Ngừng cung cấp", "Tất cả trạng thái"};
    private static final Boolean[] GIA_TRI_TRANG_THAI = {Boolean.TRUE, Boolean.FALSE, null};
    private static final String[] TEN_PHAN_LOAI = {"Tất cả phân loại thuế", "Điều trị bệnh lý (KCT)",
        "Vật tư/thiết bị bán kèm (5%)", "Thẩm mỹ (10%)"};
    private static final MucThue[] GIA_TRI_PHAN_LOAI = {null, MucThue.THUE_0, MucThue.THUE_5, MucThue.THUE_10};
    private static final int COT_DON_GIA = 3;
    private static final int COT_THOI_LUONG = 5;

    private final TimDichVu timDichVu;
    private final JTextField txtTimKiem = new JTextField();
    private final JComboBox<String> cboTrangThai = new JComboBox<>(TEN_TRANG_THAI);
    private final JComboBox<String> cboPhanLoaiThue = new JComboBox<>(TEN_PHAN_LOAI);
    private final JButton btnTimKiem = new JButton("Tìm kiếm");
    private final JButton btnLamMoi = new JButton("Làm mới");
    private final JLabel lblKetQua = new JLabel("Chưa tải danh mục dịch vụ.");
    private final JLabel lblChiTiet = new JLabel("Chọn một dịch vụ để xem lộ trình điều trị.");
    private final DefaultTableModel modelDichVu = new DefaultTableModel(new String[]{
        "Mã DV", "Tên dịch vụ", "Phân loại", "Đơn giá", "Thuế GTGT", "Thời lượng", "Trạng thái"
    }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == COT_DON_GIA ? Double.class : column == COT_THOI_LUONG ? Integer.class : String.class;
        }
    };
    private final JTable tblDichVu = new JTable(modelDichVu);
    private List<DichVu> dichVu = List.of();
    private SwingWorker<List<DichVu>, Void> worker;
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
        add(taoThanhTimKiem(), BorderLayout.NORTH);
        add(taoDanhSach(), BorderLayout.CENTER);
        add(taoChiTiet(), BorderLayout.SOUTH);

        btnTimKiem.addActionListener(e -> lamMoiDuLieu());
        txtTimKiem.addActionListener(e -> lamMoiDuLieu());
        cboTrangThai.addActionListener(e -> locThayDoi());
        cboPhanLoaiThue.addActionListener(e -> locThayDoi());
        btnLamMoi.addActionListener(e -> datLaiBoLoc());
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

    private JPanel taoThanhTimKiem() {
        JPanel header = new JPanel(new BorderLayout(0, 14));
        header.setOpaque(false);
        JLabel title = new JLabel("Danh mục dịch vụ");
        title.setFont(bold(24));
        title.setForeground(TEXT);
        header.add(title, BorderLayout.NORTH);

        JPanel search = new JPanel(new BorderLayout(12, 0));
        search.setOpaque(false);
        txtTimKiem.setName("timDichVu");
        txtTimKiem.setFont(font(13));
        txtTimKiem.putClientProperty("JTextField.placeholderText", "Nhập tên hoặc mã dịch vụ...");
        txtTimKiem.setToolTipText("Tìm theo tên hoặc mã dịch vụ; Enter để tìm.");
        txtTimKiem.setPreferredSize(new Dimension(240, 40));
        search.add(txtTimKiem, BorderLayout.CENTER);

        JPanel boLoc = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        boLoc.setOpaque(false);
        cboTrangThai.setName("locTrangThai");
        cboPhanLoaiThue.setName("locPhanLoaiThue");
        for (JComboBox<String> cbo : List.of(cboTrangThai, cboPhanLoaiThue)) {
            cbo.setFont(font(13));
            cbo.setPreferredSize(new Dimension(cbo == cboTrangThai ? 160 : 230, 40));
            boLoc.add(cbo);
        }
        btnTimKiem.setName("timKiemDichVu");
        btnLamMoi.setName("lamMoiDichVu");
        btnLamMoi.setToolTipText("Xóa từ khóa, đưa bộ lọc về mặc định và tải lại (F5 chỉ tải lại).");
        for (JButton button : new JButton[]{btnTimKiem, btnLamMoi}) {
            button.setFont(bold(12));
            button.setPreferredSize(new Dimension(100, 40));
            boLoc.add(button);
        }
        btnTimKiem.setBackground(TEAL);
        btnTimKiem.setForeground(Color.WHITE);
        search.add(boLoc, BorderLayout.EAST);
        header.add(search, BorderLayout.CENTER);
        return header;
    }

    private JPanel taoDanhSach() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        lblKetQua.setName("ketQuaDichVu");
        lblKetQua.setFont(font(12));
        lblKetQua.setForeground(MUTED);
        card.add(lblKetQua, BorderLayout.NORTH);

        tblDichVu.setName("danhSachDichVu");
        tblDichVu.setFont(font(12));
        tblDichVu.setRowHeight(40);
        tblDichVu.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblDichVu.setSelectionBackground(TEAL_LIGHT);
        tblDichVu.setSelectionForeground(TEXT);
        tblDichVu.setGridColor(BORDER);
        tblDichVu.setShowVerticalLines(false);
        tblDichVu.setFillsViewportHeight(true);
        tblDichVu.setRowSorter(new TableRowSorter<>(modelDichVu));
        tblDichVu.getTableHeader().setReorderingAllowed(false);
        tblDichVu.getTableHeader().setFont(bold(12));
        tblDichVu.getTableHeader().setPreferredSize(new Dimension(0, 40));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                    boolean focus, int row, int column) {
                Object hienThi = switch (table.convertColumnIndexToModel(column)) {
                    case COT_DON_GIA -> value instanceof Number n ? dinhDangTien(n.doubleValue()) : value;
                    case COT_THOI_LUONG -> value == null ? null : value + " phút";
                    default -> value;
                };
                super.getTableCellRendererComponent(table, hienThi, selected, focus, row, column);
                int cot = table.convertColumnIndexToModel(column);
                setHorizontalAlignment(cot == COT_DON_GIA || cot == COT_THOI_LUONG ? RIGHT : LEFT);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                setToolTipText(hienThi == null ? null : hienThi.toString());
                return this;
            }
        };
        tblDichVu.setDefaultRenderer(Object.class, renderer);
        tblDichVu.setDefaultRenderer(Double.class, renderer);
        tblDichVu.setDefaultRenderer(Integer.class, renderer);
        int[] widths = {75, 230, 180, 120, 85, 95, 120};
        for (int i = 0; i < widths.length; i++) {
            tblDichVu.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            tblDichVu.getColumnModel().getColumn(i).setMinWidth(i == 0 ? 65 : 80);
        }
        JScrollPane scroll = new JScrollPane(tblDichVu);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(SURFACE);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel taoChiTiet() {
        SurfacePanel chiTiet = new SurfacePanel(new BorderLayout(12, 0));
        chiTiet.setBorder(new EmptyBorder(16, 18, 16, 18));
        lblChiTiet.setName("chiTietDichVu");
        lblChiTiet.setFont(font(13));
        lblChiTiet.setForeground(TEXT);
        chiTiet.add(lblChiTiet, BorderLayout.CENTER);
        return chiTiet;
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
        if (worker != null) {
            worker.cancel(true);
            worker = null;
        }
        super.removeNotify();
    }

    /** UC006 bước 3–4: tải danh sách theo từ khóa và bộ lọc hiện tại (chạy nền, không chặn EDT). */
    public void lamMoiDuLieu() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::lamMoiDuLieu);
            return;
        }
        if (worker != null) {
            worker.cancel(true);
        }
        String tuKhoa = txtTimKiem.getText().trim();
        Boolean trangThai = GIA_TRI_TRANG_THAI[Math.max(0, cboTrangThai.getSelectedIndex())];
        MucThue mucThue = GIA_TRI_PHAN_LOAI[Math.max(0, cboPhanLoaiThue.getSelectedIndex())];
        datDangTai(true);
        lblKetQua.setForeground(MUTED);
        lblKetQua.setText("Đang tải danh mục dịch vụ...");
        worker = new SwingWorker<>() {
            @Override
            protected List<DichVu> doInBackground() {
                return List.copyOf(timDichVu.tim(tuKhoa, trangThai, mucThue));
            }

            @Override
            protected void done() {
                if (this != worker || isCancelled()) {
                    return;
                }
                try {
                    hienThi(get());
                } catch (ExecutionException ex) {
                    xoaKetQua();
                    Throwable cause = ex.getCause();
                    lblKetQua.setForeground(DO);
                    lblKetQua.setText(cause instanceof BusinessException ? cause.getMessage()
                            : "Không thể tải danh mục dịch vụ. Vui lòng thử lại.");
                    LOG.log(Level.WARNING, "Không thể tra cứu dịch vụ", cause);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    xoaKetQua();
                    lblKetQua.setText("Đã dừng tải danh mục dịch vụ.");
                } finally {
                    worker = null;
                    datDangTai(false);
                }
            }
        };
        worker.execute();
    }

    private void hienThi(List<DichVu> ketQua) {
        xoaKetQua();
        dichVu = ketQua;
        for (DichVu dv : dichVu) {
            modelDichVu.addRow(new Object[]{dv.getMaDV(), dv.getTenDV(), dv.getLoaiDichVu().getTenLoaiDV(),
                dv.getDonGia(), tenMucThue(dv.getLoaiDichVu().getMucThueGTGT()), dv.getThoiLuong(),
                dv.isTrangThai() ? "Đang cung cấp" : "Ngừng cung cấp"});
        }
        lblKetQua.setForeground(MUTED);
        // UC006 luồng 4.1
        lblKetQua.setText(dichVu.isEmpty() ? "Không tìm thấy dịch vụ phù hợp."
                : "Tìm thấy " + dichVu.size() + " dịch vụ · Chọn dòng để xem lộ trình điều trị");
    }

    private void locThayDoi() {
        if (!dangDatLaiBoLoc) {
            lamMoiDuLieu();
        }
    }

    /** Nút "Làm mới": về điều kiện mặc định của UC006 bước 2 (đang cung cấp, mọi phân loại). */
    private void datLaiBoLoc() {
        dangDatLaiBoLoc = true;
        try {
            txtTimKiem.setText("");
            cboTrangThai.setSelectedIndex(0);
            cboPhanLoaiThue.setSelectedIndex(0);
        } finally {
            dangDatLaiBoLoc = false;
        }
        lamMoiDuLieu();
    }

    private void datDangTai(boolean dangTai) {
        btnTimKiem.setEnabled(!dangTai);
        btnLamMoi.setEnabled(!dangTai);
    }

    private void xoaKetQua() {
        tblDichVu.clearSelection();
        modelDichVu.setRowCount(0);
        dichVu = List.of();
        lblChiTiet.setText("Chọn một dịch vụ để xem lộ trình điều trị.");
    }

    private void hienChiTiet() {
        int viewRow = tblDichVu.getSelectedRow();
        if (viewRow < 0) {
            return;
        }
        DichVu dv = dichVu.get(tblDichVu.convertRowIndexToModel(viewRow));
        String loTrinh = dv.getLoTrinhDichVu();
        lblChiTiet.setText("<html><b>" + html(dv.getTenDV()) + " · " + html(dv.getMaDV()) + "</b><br>"
                + (loTrinh == null ? "Lộ trình điều trị: Không có (thực hiện trong một buổi)"
                : "Lộ trình điều trị:<br>" + html(loTrinh).replace("\n", "<br>")) + "</html>");
    }

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
}
