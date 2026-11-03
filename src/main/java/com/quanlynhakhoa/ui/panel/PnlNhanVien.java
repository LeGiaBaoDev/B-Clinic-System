package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.dao.NhanVienDAO;
import com.quanlynhakhoa.dao.TaiKhoanDAO;
import com.quanlynhakhoa.entity.NhanVien;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.ui.dialog.DlgThemNhanVien;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import com.quanlynhakhoa.util.SessionManager;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/** UC004: Quản lý tra cứu nhân viên; chỉ đọc dữ liệu qua DAO. */
public class PnlNhanVien extends JPanel {

    private static final Logger LOG = Logger.getLogger(PnlNhanVien.class.getName());
    private final Function<String, List<NhanVien>> timNhanVien;
    private final Supplier<List<TaiKhoan>> layTaiKhoan;
    private final JTextField txtTimKiem = new JTextField();
    private final JButton btnTimKiem = new JButton("Tìm kiếm");
    private final JButton btnLamMoi = new JButton("Làm mới");
    private final JButton btnThem = new JButton("Thêm nhân viên");
    private final JLabel lblKetQua = new JLabel("Chưa tải danh sách nhân viên.");
    private final JLabel lblChiTiet = new JLabel("Chọn một nhân viên để xem thông tin chi tiết.");
    private final DefaultTableModel modelNhanVien = new DefaultTableModel(new String[]{
        "Mã NV", "Họ tên", "Số điện thoại", "Chuyên môn", "Tài khoản", "Vai trò", "Trạng thái"
    }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tblNhanVien = new JTable(modelNhanVien);
    private List<NhanVien> nhanVien = List.of();
    private Map<String, TaiKhoan> taiKhoanTheoNhanVien = Map.of();
    private SwingWorker<DuLieu, Void> worker;

    private record DuLieu(List<NhanVien> nhanVien, List<TaiKhoan> taiKhoan) { }

    public PnlNhanVien() {
        this(new NhanVienDAO()::timKiem, new TaiKhoanDAO()::layTatCa);
    }

    PnlNhanVien(Function<String, List<NhanVien>> timNhanVien, Supplier<List<TaiKhoan>> layTaiKhoan) {
        super(new BorderLayout(0, 16));
        this.timNhanVien = java.util.Objects.requireNonNull(timNhanVien);
        this.layTaiKhoan = java.util.Objects.requireNonNull(layTaiKhoan);
        setName("Nhân viên");
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 24, 24));
        add(taoThanhTimKiem(), BorderLayout.NORTH);
        add(taoDanhSach(), BorderLayout.CENTER);
        SurfacePanel chiTiet = new SurfacePanel(new BorderLayout());
        chiTiet.setBorder(new EmptyBorder(16, 18, 16, 18));
        lblChiTiet.setName("chiTietNhanVien");
        lblChiTiet.setFont(font(13));
        lblChiTiet.setForeground(TEXT);
        chiTiet.add(lblChiTiet);
        add(chiTiet, BorderLayout.SOUTH);

        btnTimKiem.addActionListener(e -> lamMoiDuLieu());
        btnThem.addActionListener(e -> moThemNhanVien());
        btnLamMoi.addActionListener(e -> lamMoiDuLieu());
        txtTimKiem.addActionListener(e -> lamMoiDuLieu());
        tblNhanVien.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) { hienChiTiet(); }
        });
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "lamMoi");
        getActionMap().put("lamMoi", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) { lamMoiDuLieu(); }
        });
    }

    private JPanel taoThanhTimKiem() {
        JPanel header = new JPanel(new BorderLayout(0, 14));
        header.setOpaque(false);
        JLabel title = new JLabel("Tra cứu nhân viên");
        title.setFont(bold(24));
        title.setForeground(TEXT);
        header.add(title, BorderLayout.NORTH);
        JPanel search = new JPanel(new BorderLayout(12, 0));
        search.setOpaque(false);
        txtTimKiem.setName("timNhanVien");
        txtTimKiem.setFont(font(13));
        txtTimKiem.putClientProperty("JTextField.placeholderText", "Nhập mã, họ tên hoặc số điện thoại...");
        txtTimKiem.setToolTipText("Tìm theo mã nhân viên, họ tên hoặc số điện thoại; Enter để tìm.");
        txtTimKiem.setPreferredSize(new Dimension(250, 40));
        search.add(txtTimKiem, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        for (JButton button : new JButton[]{btnTimKiem, btnLamMoi, btnThem}) {
            button.setFont(bold(12));
            button.setPreferredSize(new Dimension(110, 40));
            buttons.add(button);
        }
        btnTimKiem.setBackground(TEAL);
        btnTimKiem.setForeground(Color.WHITE);
        btnLamMoi.setName("lamMoiNhanVien");
        btnThem.setName("themNhanVien");
        btnThem.setPreferredSize(new Dimension(145, 40));
        btnThem.setBackground(TEAL);
        btnThem.setForeground(Color.WHITE);
        btnLamMoi.setToolTipText("Tải lại kết quả theo từ khóa hiện tại (F5).");
        search.add(buttons, BorderLayout.EAST);
        header.add(search, BorderLayout.CENTER);
        return header;
    }

    private JPanel taoDanhSach() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        lblKetQua.setName("ketQuaNhanVien");
        lblKetQua.setFont(font(12));
        lblKetQua.setForeground(MUTED);
        card.add(lblKetQua, BorderLayout.NORTH);
        tblNhanVien.setName("danhSachNhanVien");
        tblNhanVien.setFont(font(12));
        tblNhanVien.setRowHeight(44);
        tblNhanVien.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblNhanVien.setSelectionBackground(TEAL_LIGHT);
        tblNhanVien.setSelectionForeground(TEXT);
        tblNhanVien.setGridColor(BORDER);
        tblNhanVien.setShowVerticalLines(false);
        tblNhanVien.setFillsViewportHeight(true);
        tblNhanVien.setRowSorter(new TableRowSorter<>(modelNhanVien));
        tblNhanVien.getTableHeader().setReorderingAllowed(false);
        tblNhanVien.getTableHeader().setFont(bold(12));
        tblNhanVien.getTableHeader().setPreferredSize(new Dimension(0, 40));
        tblNhanVien.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                    boolean focus, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                setToolTipText(value == null ? null : value.toString());
                return this;
            }
        });
        int[] widths = {75, 180, 120, 190, 140, 95, 110};
        for (int i = 0; i < widths.length; i++) {
            tblNhanVien.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            tblNhanVien.getColumnModel().getColumn(i).setMinWidth(i == 0 ? 65 : 90);
        }
        JScrollPane scroll = new JScrollPane(tblNhanVien);
        scroll.setColumnHeaderView(tblNhanVien.getTableHeader());
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(SURFACE);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(() -> {
            if (isDisplayable()) { lamMoiDuLieu(); }
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

    /** Tải lại theo từ khóa; DAO kiểm tra quyền Quản lý từ DB trước khi trả dữ liệu. */
    public void lamMoiDuLieu() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::lamMoiDuLieu);
            return;
        }
        TaiKhoan actor = SessionManager.getInstance().getCurrentUser();
        if (actor == null || actor.getVaiTro() != VaiTro.QUANLY) {
            if (worker != null) { worker.cancel(true); worker = null; }
            xoaKetQua();
            lblKetQua.setText("Chỉ Quản lý được tra cứu nhân viên và tài khoản.");
            datDangTai(false);
            return;
        }
        if (worker != null) { worker.cancel(true); }
        String tuKhoa = txtTimKiem.getText().trim();
        datDangTai(true);
        lblKetQua.setForeground(MUTED);
        lblKetQua.setText("Đang tải danh sách nhân viên...");
        worker = new SwingWorker<>() {
            @Override
            protected DuLieu doInBackground() {
                // Kiểm tra quyền Quản lý trong DAO trước khi đọc danh sách nhân viên.
                List<TaiKhoan> accounts = layTaiKhoan.get();
                return new DuLieu(List.copyOf(timNhanVien.apply(tuKhoa)), List.copyOf(accounts));
            }

            @Override
            protected void done() {
                if (this != worker || isCancelled()) { return; }
                try {
                    DuLieu data = get();
                    xoaKetQua();
                    nhanVien = data.nhanVien();
                    Map<String, TaiKhoan> accounts = new HashMap<>();
                    for (TaiKhoan tk : data.taiKhoan()) {
                        accounts.put(tk.getNhanVien().getMaNV(), tk);
                    }
                    taiKhoanTheoNhanVien = accounts;
                    for (NhanVien nv : nhanVien) {
                        TaiKhoan tk = accounts.get(nv.getMaNV());
                        modelNhanVien.addRow(new Object[]{nv.getMaNV(), nv.getHoTenNV(), cheSdt(nv.getSdt()),
                            nv.getChuyenMon(), tk == null ? "—" : tk.getTenTK(),
                            tk == null ? "—" : tenVaiTro(tk.getVaiTro()), trangThai(tk)});
                    }
                    lblKetQua.setText(nhanVien.isEmpty() ? "Không tìm thấy nhân viên phù hợp."
                            : "Tìm thấy " + nhanVien.size() + " nhân viên · Chọn dòng để xem chi tiết");
                } catch (ExecutionException ex) {
                    xoaKetQua();
                    Throwable cause = ex.getCause();
                    lblKetQua.setForeground(new Color(0xC94C48));
                    lblKetQua.setText(cause instanceof BusinessException ? cause.getMessage()
                            : "Không thể tải danh sách nhân viên. Vui lòng thử lại.");
                    LOG.log(Level.WARNING, "Không thể tra cứu nhân viên", cause);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    xoaKetQua();
                    lblKetQua.setText("Đã dừng tải danh sách nhân viên.");
                } finally {
                    worker = null;
                    datDangTai(false);
                }
            }
        };
        worker.execute();
    }

    private void datDangTai(boolean dangTai) {
        btnTimKiem.setEnabled(!dangTai);
        btnLamMoi.setEnabled(!dangTai);
        TaiKhoan actor = SessionManager.getInstance().getCurrentUser();
        btnThem.setEnabled(!dangTai && actor != null && actor.getVaiTro() == VaiTro.QUANLY);
    }

    private void moThemNhanVien() {
        TaiKhoan actor = SessionManager.getInstance().getCurrentUser();
        if (actor == null || actor.getVaiTro() != VaiTro.QUANLY) {
            lblKetQua.setText("Chỉ Quản lý được tạo nhân viên và tài khoản.");
            return;
        }
        new DlgThemNhanVien(SwingUtilities.getWindowAncestor(this), nv -> {
            txtTimKiem.setText(nv.getMaNV());
            lamMoiDuLieu();
            JOptionPane.showMessageDialog(this, "Đã tạo nhân viên " + nv.getMaNV() + " và tài khoản.",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
        }).setVisible(true);
    }

    private void xoaKetQua() {
        tblNhanVien.clearSelection();
        modelNhanVien.setRowCount(0);
        nhanVien = List.of();
        taiKhoanTheoNhanVien = Map.of();
        lblChiTiet.setText("Chọn một nhân viên để xem thông tin chi tiết.");
    }

    private void hienChiTiet() {
        int viewRow = tblNhanVien.getSelectedRow();
        if (viewRow < 0) { return; }
        NhanVien nv = nhanVien.get(tblNhanVien.convertRowIndexToModel(viewRow));
        TaiKhoan tk = taiKhoanTheoNhanVien.get(nv.getMaNV());
        lblChiTiet.setText("<html><b>" + html(nv.getHoTenNV()) + " · " + html(nv.getMaNV())
                + "</b><br>Giới tính: " + (nv.isGioiTinh() ? "Nam" : "Nữ")
                + " &nbsp; | &nbsp; SĐT: " + html(nv.getSdt()) + " &nbsp; | &nbsp; Email: " + html(nv.getEmail())
                + "<br>Chuyên môn: " + html(nv.getChuyenMon())
                + "<br>Tài khoản: " + (tk == null ? "Chưa có" : html(tk.getTenTK()))
                + " &nbsp; | &nbsp; Vai trò: " + (tk == null ? "—" : tenVaiTro(tk.getVaiTro()))
                + " &nbsp; | &nbsp; Trạng thái: " + trangThai(tk) + "</html>");
    }

    private static String cheSdt(String sdt) {
        return sdt == null || sdt.length() < 4 ? "—" : "******" + sdt.substring(sdt.length() - 4);
    }

    private static String html(String text) {
        return text == null ? "—" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String trangThai(TaiKhoan tk) {
        return tk == null ? "Chưa có tài khoản" : tk.isTrangThai() ? "Hoạt động" : "Đã khóa";
    }

    private static String tenVaiTro(VaiTro vaiTro) {
        return switch (vaiTro) {
            case QUANLY -> "Quản lý";
            case NHASI -> "Nha sĩ";
            case LETAN -> "Lễ tân";
        };
    }
}
