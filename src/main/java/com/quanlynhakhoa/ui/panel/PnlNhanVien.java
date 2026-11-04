package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.dao.NhanVienDAO;
import com.quanlynhakhoa.dao.TaiKhoanDAO;
import com.quanlynhakhoa.entity.NhanVien;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import com.quanlynhakhoa.ui.dialog.DlgCapNhatNhanVien;
import com.quanlynhakhoa.ui.dialog.DlgThemNhanVien;
import com.quanlynhakhoa.util.SessionManager;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class PnlNhanVien extends JPanel {

    private static final Logger LOG = Logger.getLogger(PnlNhanVien.class.getName());
    private final Function<String, List<NhanVien>> timNhanVien;
    private final Supplier<List<TaiKhoan>> layTaiKhoan;
    private final JTextField txtTimKiem = new JTextField();
    private final JButton btnTimKiem = new JButton("Tìm kiếm");
    private final JButton btnLamMoi = new JButton("Làm mới");
    private final JButton btnThem = new JButton("Thêm nhân viên");
    private final JButton btnCapNhat = new JButton("Cập nhật / Khóa");
    private boolean dangTai;
    private final JLabel lblKetQua = new JLabel("Chưa tải danh sách nhân viên.");
    private final JLabel lblChiTiet = new JLabel("Chọn một nhân viên để xem thông tin chi tiết.");
    private final JLabel lblMaNV = new JLabel(" ");
    private final JLabel lblGioiTinh = new JLabel(" ");
    private final JLabel lblSdt = new JLabel(" ");
    private final JLabel lblEmail = new JLabel(" ");
    private final JLabel lblChuyenMon = new JLabel(" ");
    private final JLabel lblTaiKhoan = new JLabel(" ");
    private final JLabel lblVaiTro = new JLabel(" ");
    private final TrangThaiPill lblTrangThai = new TrangThaiPill();
    private final JLabel lblAvatar = new JLabel("?", SwingConstants.CENTER) {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(TEAL_LIGHT);
            int duongKinh = Math.min(getWidth(), getHeight());
            g2.fillOval((getWidth() - duongKinh) / 2, (getHeight() - duongKinh) / 2,
                    duongKinh, duongKinh);
            g2.dispose();
            super.paintComponent(g);
        }
    };
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

    private record DuLieu(List<NhanVien> nhanVien, List<TaiKhoan> taiKhoan) {

    }

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
        add(taoTheChiTiet(), BorderLayout.SOUTH);

        btnTimKiem.addActionListener(e -> lamMoiDuLieu());
        btnThem.addActionListener(e -> moThemNhanVien());
        btnCapNhat.addActionListener(e -> moCapNhatNhanVien());
        btnLamMoi.addActionListener(e -> lamMoiDuLieu());
        txtTimKiem.addActionListener(e -> lamMoiDuLieu());
        tblNhanVien.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                hienChiTiet();
                capNhatNutSua();
            }
        });
        tblNhanVien.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    int row = tblNhanVien.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        tblNhanVien.setRowSelectionInterval(row, row);
                        moCapNhatNhanVien();
                    }
                }
            }
        });
        tblNhanVien.setToolTipText("Nhấn đúp một dòng để cập nhật nhân viên và tài khoản.");
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "lamMoi");
        getActionMap().put("lamMoi", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                lamMoiDuLieu();
            }
        });
    }

    private JPanel taoTheChiTiet() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(14, 18, 16, 18));

        JPanel top = new JPanel(new BorderLayout(16, 0));
        top.setOpaque(false);
        lblAvatar.setFont(bold(16));
        lblAvatar.setForeground(TEAL);
        lblAvatar.setPreferredSize(new Dimension(54, 54));
        top.add(lblAvatar, BorderLayout.WEST);

        JPanel ten = new JPanel();
        ten.setOpaque(false);
        ten.setLayout(new BoxLayout(ten, BoxLayout.Y_AXIS));
        JPanel dongTen = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        dongTen.setOpaque(false);
        dongTen.setAlignmentX(LEFT_ALIGNMENT);
        lblChiTiet.setName("chiTietNhanVien");
        lblChiTiet.setFont(bold(17));
        lblChiTiet.setForeground(TEXT);
        lblMaNV.setFont(font(11));
        lblMaNV.setForeground(MUTED);
        dongTen.add(lblChiTiet);
        dongTen.add(Box.createHorizontalStrut(8));
        dongTen.add(lblMaNV);
        JLabel phu = new JLabel("Thông tin nhân viên và tài khoản");
        phu.setFont(font(10));
        phu.setForeground(MUTED);
        ten.add(dongTen);
        ten.add(Box.createVerticalStrut(4));
        ten.add(phu);
        top.add(ten, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        lblTrangThai.setName("chiTietTrangThaiNhanVien");
        lblTrangThai.setFont(bold(10));
        btnCapNhat.setName("capNhatNhanVien");
        btnCapNhat.setFont(bold(12));
        btnCapNhat.setBackground(Color.WHITE);
        btnCapNhat.setForeground(TEXT);
        btnCapNhat.setPreferredSize(new Dimension(140, 36));
        btnCapNhat.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCapNhat.setEnabled(false);
        actions.add(lblTrangThai);
        actions.add(btnCapNhat);
        top.add(actions, BorderLayout.EAST);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JPanel chinh = new JPanel(new GridLayout(1, 3, 16, 0));
        chinh.setOpaque(false);
        chinh.setAlignmentX(LEFT_ALIGNMENT);
        chinh.add(khoi("Giới tính", lblGioiTinh));
        chinh.add(khoi("Số điện thoại", lblSdt));
        chinh.add(khoi("Email", lblEmail));
        body.add(chinh);
        body.add(Box.createVerticalStrut(12));

        JPanel phuInfo = new JPanel();
        phuInfo.setOpaque(false);
        phuInfo.setLayout(new BoxLayout(phuInfo, BoxLayout.Y_AXIS));
        phuInfo.setAlignmentX(LEFT_ALIGNMENT);
        phuInfo.add(dongPhu("Chuyên môn", lblChuyenMon));
        phuInfo.add(Box.createVerticalStrut(4));
        phuInfo.add(dongPhu("Tài khoản", lblTaiKhoan));
        phuInfo.add(Box.createVerticalStrut(4));
        phuInfo.add(dongPhu("Vai trò", lblVaiTro));
        body.add(phuInfo);

        card.add(top, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private static JPanel khoi(String nhan, JLabel giaTri) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel lbl = new JLabel(nhan);
        lbl.setFont(font(10));
        lbl.setForeground(MUTED);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        giaTri.setFont(bold(12));
        giaTri.setForeground(TEXT);
        giaTri.setAlignmentX(LEFT_ALIGNMENT);
        p.add(lbl);
        p.add(Box.createVerticalStrut(4));
        p.add(giaTri);
        return p;
    }

    private static JPanel dongPhu(String nhan, JLabel giaTri) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel lbl = new JLabel(nhan);
        lbl.setFont(font(10));
        lbl.setForeground(MUTED);
        lbl.setPreferredSize(new Dimension(105, 18));
        giaTri.setFont(font(11));
        giaTri.setForeground(TEXT);
        p.add(lbl, BorderLayout.WEST);
        p.add(giaTri, BorderLayout.CENTER);
        return p;
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

    /**
     * Tải lại theo từ khóa; DAO kiểm tra quyền Quản lý từ DB trước khi trả dữ
     * liệu.
     */
    public void lamMoiDuLieu() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::lamMoiDuLieu);
            return;
        }
        TaiKhoan actor = SessionManager.getInstance().getCurrentUser();
        if (actor == null || actor.getVaiTro() != VaiTro.QUANLY) {
            if (worker != null) {
                worker.cancel(true);
                worker = null;
            }
            xoaKetQua();
            lblKetQua.setText("Chỉ Quản lý được tra cứu nhân viên và tài khoản.");
            datDangTai(false);
            return;
        }
        if (worker != null) {
            worker.cancel(true);
        }
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
                if (this != worker || isCancelled()) {
                    return;
                }
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
        this.dangTai = dangTai;
        btnTimKiem.setEnabled(!dangTai);
        btnLamMoi.setEnabled(!dangTai);
        TaiKhoan actor = SessionManager.getInstance().getCurrentUser();
        btnThem.setEnabled(!dangTai && actor != null && actor.getVaiTro() == VaiTro.QUANLY);
        capNhatNutSua();
    }

    private void capNhatNutSua() {
        TaiKhoan actor = SessionManager.getInstance().getCurrentUser();
        int row = tblNhanVien.getSelectedRow();
        btnCapNhat.setEnabled(!dangTai && actor != null && actor.getVaiTro() == VaiTro.QUANLY
                && row >= 0 && taiKhoanTheoNhanVien.containsKey(
                        nhanVien.get(tblNhanVien.convertRowIndexToModel(row)).getMaNV()));
    }

    private void moCapNhatNhanVien() {
        TaiKhoan actor = SessionManager.getInstance().getCurrentUser();
        if (actor == null || actor.getVaiTro() != VaiTro.QUANLY) {
            lblKetQua.setText("Chỉ Quản lý được cập nhật nhân viên và tài khoản.");
            return;
        }
        int row = tblNhanVien.getSelectedRow();
        if (dangTai || row < 0) {
            return;
        }
        NhanVien nv = nhanVien.get(tblNhanVien.convertRowIndexToModel(row));
        TaiKhoan tk = taiKhoanTheoNhanVien.get(nv.getMaNV());
        if (tk == null) {
            return;
        }
        new DlgCapNhatNhanVien(SwingUtilities.getWindowAncestor(this), tk, () -> {
            txtTimKiem.setText(nv.getMaNV());
            lamMoiDuLieu();
            JOptionPane.showMessageDialog(this, "Cập nhật tài khoản thành công.",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
        }).setVisible(true);
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
        lblMaNV.setText(" ");
        lblGioiTinh.setText(" ");
        lblSdt.setText(" ");
        lblEmail.setText(" ");
        lblChuyenMon.setText(" ");
        lblTaiKhoan.setText(" ");
        lblVaiTro.setText(" ");
        lblAvatar.setText("?");
        lblTrangThai.set("", MUTED, new Color(0xF0F2F5));
    }

    private void hienChiTiet() {
        int viewRow = tblNhanVien.getSelectedRow();
        if (viewRow < 0) {
            return;
        }
        NhanVien nv = nhanVien.get(tblNhanVien.convertRowIndexToModel(viewRow));
        TaiKhoan tk = taiKhoanTheoNhanVien.get(nv.getMaNV());
        lblChiTiet.setText(nv.getHoTenNV());
        lblMaNV.setText(nv.getMaNV());
        lblGioiTinh.setText(nv.isGioiTinh() ? "Nam" : "Nữ");
        lblSdt.setText(nv.getSdt());
        lblEmail.setName("chiTietMetaNhanVien");
        lblEmail.setText(nv.getEmail());
        lblChuyenMon.setText(nv.getChuyenMon());
        lblTaiKhoan.setText(tk == null ? "Chưa có" : tk.getTenTK());
        lblVaiTro.setText(tk == null ? "—" : tenVaiTro(tk.getVaiTro()));
        lblAvatar.setText(vietTat(nv.getHoTenNV()));
        if (tk == null) {
            lblTrangThai.set("Chưa có tài khoản", MUTED, new Color(0xF0F2F5));
        } else if (tk.isTrangThai()) {
            lblTrangThai.set("Hoạt động", GREEN, new Color(0xE7F7EF));
        } else {
            lblTrangThai.set("Đã khóa", new Color(0xC0392B), new Color(0xFDECEA));
        }
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

    private static String vietTat(String ten) {
        String[] phan = ten == null ? new String[0] : ten.trim().split("\\s+");
        if (phan.length == 0) {
            return "?";
        }
        if (phan.length == 1) {
            return phan[0].substring(0, Math.min(2, phan[0].length())).toUpperCase(java.util.Locale.ROOT);
        }
        return (phan[0].charAt(0) + "" + phan[phan.length - 1].charAt(0))
                .toUpperCase(java.util.Locale.ROOT);
    }

    private static final class TrangThaiPill extends JLabel {

        private Color nen = new Color(0xF0F2F5);

        TrangThaiPill() {
            super("", SwingConstants.CENTER);
            setOpaque(false);
            setBorder(new EmptyBorder(4, 10, 4, 10));
        }

        void set(String text, Color chu, Color nen) {
            setText(text);
            setForeground(chu);
            this.nen = nen;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(nen);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static String tenVaiTro(VaiTro vaiTro) {
        return switch (vaiTro) {
            case QUANLY ->
                "Quản lý";
            case NHASI ->
                "Nha sĩ";
            case LETAN ->
                "Lễ tân";
        };
    }
}
