package com.quanlynhakhoa.ui.panel;

import static com.quanlynhakhoa.ui.component.UiConstants.BACKGROUND;
import static com.quanlynhakhoa.ui.component.UiConstants.BORDER;
import static com.quanlynhakhoa.ui.component.UiConstants.GREEN;
import static com.quanlynhakhoa.ui.component.UiConstants.MUTED;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL_LIGHT;
import static com.quanlynhakhoa.ui.component.UiConstants.TEXT;
import static com.quanlynhakhoa.ui.component.UiConstants.bold;
import static com.quanlynhakhoa.ui.component.UiConstants.font;

import com.quanlynhakhoa.dao.KhachHangDAO;
import com.quanlynhakhoa.dao.KhachHangDAO.TomTat;
import com.quanlynhakhoa.entity.KhachHang;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import com.quanlynhakhoa.ui.dialog.DlgHoSoKhachHang;
import com.quanlynhakhoa.ui.dialog.DlgKhachHang;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * UC010 tra cứu bệnh nhân; mở hồ sơ, form thêm (UC009) và cập nhật (UC011).
 *
 * <p>Bảng hiển thị toàn chiều rộng theo figma; hồ sơ chi tiết mở trong dialog.
 * Nha sĩ chỉ tra cứu bệnh nhân được phân công (DAO tự lọc).</p>
 */
public class PnlKhachHang extends JPanel {

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String THE_BANG = "bang";
    private static final String THE_TRONG = "trong";
    private static final String[] COT = {"MÃ BỆNH NHÂN", "HỌ VÀ TÊN", "NGÀY SINH", "GIỚI TÍNH",
        "SỐ ĐIỆN THOẠI", "LỊCH GẦN NHẤT", "HỒ SƠ", "THAO TÁC"};
    private static final String[] TRANG_THAI = {"Tất cả trạng thái", "Mới", "Đang điều trị", "Theo dõi"};
    private static final Integer[] DONG_MOI_TRANG = {9, 15, 30};

    private final KhachHangDAO khachHangDAO = new KhachHangDAO();
    private final boolean duocSua;

    private final JTextField txtTimKiem = new JTextField();
    private final JComboBox<String> cboTrangThai = new JComboBox<>(TRANG_THAI);
    private final JCheckBox chkGanNhat = new JCheckBox("Làm khám gần nhất");
    private final JComboBox<Integer> cboSoDong = new JComboBox<>(DONG_MOI_TRANG);
    private final JButton btnThem = PnlTongQuanQuanLy.button("Thêm bệnh nhân", LineIcon.Type.PLUS, true);
    private final JButton btnXuat = PnlTongQuanQuanLy.button("Xuất Excel", LineIcon.Type.INVOICE, false);
    private final JLabel lblHienThi = new JLabel(" ");
    private final JLabel lblTong = new JLabel("Đang tải hồ sơ bệnh nhân...");
    private final JLabel lblSapXep = new JLabel("Sắp xếp: Mã bệnh nhân ↑");
    private final JPanel pnlSoTrang = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
    private final JLabel lblKhongCoKetQua = new JLabel(" ", SwingConstants.CENTER);
    private final CardLayout theNoiDung = new CardLayout();
    private final JPanel pnlNoiDung = new JPanel(theNoiDung);
    private final DefaultTableModel model = new DefaultTableModel(COT, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    private List<TomTat> tatCa = new ArrayList<>();
    private List<TomTat> daLoc = new ArrayList<>();
    private List<TomTat> trangHienTai = new ArrayList<>();
    private SwingWorker<List<TomTat>, Void> worker;
    private final Timer henGio;
    private int trang = 1;
    private int soDongMoiTrang = 9;

    public PnlKhachHang(VaiTro vaiTro) {
        super(new BorderLayout(0, 14));
        this.duocSua = vaiTro == VaiTro.LETAN || vaiTro == VaiTro.QUANLY;
        henGio = new Timer(350, e -> taiDuLieu(txtTimKiem.getText()));
        henGio.setRepeats(false);
        initComponents();
        addEvents();
        taiDuLieu("");
    }

    // ---------------------------------------------------------------- giao diện

    private void initComponents() {
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 20, 24));

        add(taoTieuDe(), BorderLayout.NORTH);
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(16, 18, 10, 18));
        card.add(taoBoLoc(), BorderLayout.NORTH);
        card.add(taoBang(), BorderLayout.CENTER);
        card.add(taoChan(), BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    private JPanel taoTieuDe() {
        JPanel hang = new JPanel(new BorderLayout());
        hang.setOpaque(false);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel tieuDe = new JLabel("Danh sách bệnh nhân");
        tieuDe.setFont(bold(21));
        tieuDe.setForeground(TEXT);
        lblTong.setFont(font(12));
        lblTong.setForeground(MUTED);
        text.add(tieuDe);
        text.add(Box.createVerticalStrut(4));
        text.add(lblTong);
        hang.add(text, BorderLayout.WEST);
        btnThem.setVisible(duocSua);
        hang.add(btnThem, BorderLayout.EAST);
        return hang;
    }

    private JPanel taoBoLoc() {
        JPanel dau = new JPanel(new BorderLayout(0, 12));
        dau.setOpaque(false);

        JPanel loc = new JPanel(new BorderLayout(10, 0));
        loc.setOpaque(false);

        SurfacePanel oTim = new SurfacePanel(new BorderLayout(9, 0));
        oTim.setPreferredSize(new Dimension(390, 36));
        oTim.setBorder(new EmptyBorder(4, 12, 4, 10));
        oTim.add(new JLabel(new LineIcon(LineIcon.Type.SEARCH, MUTED, 16)), BorderLayout.WEST);
        txtTimKiem.setBorder(BorderFactory.createEmptyBorder());
        txtTimKiem.setOpaque(false);
        txtTimKiem.setFont(font(12));
        txtTimKiem.putClientProperty("JTextField.placeholderText",
                "Tìm theo họ tên, số điện thoại hoặc mã bệnh nhân...");
        oTim.add(txtTimKiem, BorderLayout.CENTER);

        cboTrangThai.setFont(font(12));
        cboTrangThai.setPreferredSize(new Dimension(175, 36));
        chkGanNhat.setOpaque(false);
        chkGanNhat.setFont(font(12));
        chkGanNhat.setForeground(TEXT);
        btnXuat.setFont(bold(12));

        JPanel phai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        phai.setOpaque(false);
        phai.add(cboTrangThai);
        phai.add(chkGanNhat);
        phai.add(btnXuat);
        loc.add(oTim, BorderLayout.CENTER);
        loc.add(phai, BorderLayout.EAST);

        JPanel sapXep = new JPanel(new BorderLayout());
        sapXep.setOpaque(false);
        JLabel tatCa = new JLabel("Tất cả bệnh nhân");
        tatCa.setFont(bold(13));
        tatCa.setForeground(TEXT);
        lblSapXep.setFont(font(11));
        lblSapXep.setForeground(MUTED);
        sapXep.add(tatCa, BorderLayout.WEST);
        sapXep.add(lblSapXep, BorderLayout.EAST);

        dau.add(loc, BorderLayout.NORTH);
        dau.add(sapXep, BorderLayout.CENTER);
        return dau;
    }

    private JPanel taoBang() {
        cauHinhBang();
        JScrollPane scr = new JScrollPane(table);
        scr.setBorder(BorderFactory.createLineBorder(BORDER));
        scr.getViewport().setBackground(Color.WHITE);

        lblKhongCoKetQua.setFont(bold(16));
        lblKhongCoKetQua.setForeground(TEXT);
        JLabel goiY = new JLabel("Hãy kiểm tra lại mã, họ tên hoặc số điện thoại.", SwingConstants.CENTER);
        goiY.setFont(font(12));
        goiY.setForeground(MUTED);
        JPanel trong = new JPanel();
        trong.setBackground(Color.WHITE);
        trong.setBorder(BorderFactory.createLineBorder(BORDER));
        trong.setLayout(new BoxLayout(trong, BoxLayout.Y_AXIS));
        trong.add(Box.createVerticalGlue());
        lblKhongCoKetQua.setAlignmentX(CENTER_ALIGNMENT);
        goiY.setAlignmentX(CENTER_ALIGNMENT);
        trong.add(lblKhongCoKetQua);
        trong.add(Box.createVerticalStrut(5));
        trong.add(goiY);
        trong.add(Box.createVerticalGlue());

        pnlNoiDung.setOpaque(false);
        pnlNoiDung.add(scr, THE_BANG);
        pnlNoiDung.add(trong, THE_TRONG);
        return pnlNoiDung;
    }

    private void cauHinhBang() {
        table.setRowHeight(44);
        table.setFont(font(12));
        table.getTableHeader().setFont(bold(10));
        table.getTableHeader().setForeground(MUTED);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(0xEDF1F5));
        table.setSelectionBackground(new Color(0xEFF6F5));
        table.setSelectionForeground(TEXT);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        TableColumnModel cot = table.getColumnModel();
        int[] rong = {115, 190, 100, 80, 120, 130, 105, 140};
        for (int i = 0; i < rong.length; i++) {
            cot.getColumn(i).setPreferredWidth(rong[i]);
        }
        for (int i = 0; i <= 5; i++) {
            cot.getColumn(i).setCellRenderer(new PadRenderer(i == 1));
        }
        cot.getColumn(6).setCellRenderer(new HoSoRenderer());
        cot.getColumn(7).setCellRenderer(new ThaoTacRenderer());
        DefaultTableCellRenderer left = header(SwingConstants.LEFT, 20);
        DefaultTableCellRenderer center = header(SwingConstants.CENTER, 6);
        for (int i = 0; i < cot.getColumnCount(); i++) {
            cot.getColumn(i).setHeaderRenderer(i < 6 ? left : center);
        }
    }

    private static DefaultTableCellRenderer header(int align, int left) {
        DefaultTableCellRenderer h = new DefaultTableCellRenderer();
        h.setOpaque(true);
        h.setBackground(new Color(0xF7F9FB));
        h.setHorizontalAlignment(align);
        h.setFont(bold(10));
        h.setForeground(MUTED);
        h.setBorder(new EmptyBorder(8, left, 8, 6));
        return h;
    }

    private JPanel taoChan() {
        JPanel chan = new JPanel(new BorderLayout());
        chan.setOpaque(false);
        lblHienThi.setFont(font(10));
        lblHienThi.setForeground(MUTED);
        chan.add(lblHienThi, BorderLayout.WEST);

        cboSoDong.setFont(font(11));
        cboSoDong.setPreferredSize(new Dimension(70, 28));
        cboSoDong.addActionListener(e -> {
            soDongMoiTrang = (Integer) cboSoDong.getSelectedItem();
            trang = 1;
            capNhatBang();
        });
        pnlSoTrang.setOpaque(false);

        JPanel phai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        phai.setOpaque(false);
        JLabel nhanDong = new JLabel("Số dòng");
        nhanDong.setFont(font(10));
        nhanDong.setForeground(MUTED);
        phai.add(nhanDong);
        phai.add(cboSoDong);
        phai.add(pnlSoTrang);
        chan.add(phai, BorderLayout.EAST);
        return chan;
    }

    // ---------------------------------------------------------------- dữ liệu

    private void taiDuLieu(String tuKhoa) {
        if (worker != null) {
            worker.cancel(true);
        }
        lblHienThi.setText("Đang tải dữ liệu...");
        worker = new SwingWorker<>() {
            @Override
            protected List<TomTat> doInBackground() {
                return khachHangDAO.timKiemTomTat(tuKhoa);
            }

            @Override
            protected void done() {
                try {
                    tatCa = new ArrayList<>(get());
                    apDungLoc();
                } catch (CancellationException ignored) {
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    baoLoi(e.getCause());
                }
            }
        };
        worker.execute();
    }

    private void apDungLoc() {
        String trangThai = String.valueOf(cboTrangThai.getSelectedItem());
        daLoc = new ArrayList<>();
        for (TomTat t : tatCa) {
            if (!"Tất cả trạng thái".equals(trangThai) && !trangThai.equals(t.trangThaiHoSo())) {
                continue;
            }
            daLoc.add(t);
        }
        if (chkGanNhat.isSelected()) {
            daLoc.sort(Comparator.comparing(TomTat::lichGanNhat,
                    Comparator.nullsLast(Comparator.reverseOrder())));
        } else {
            daLoc.sort(Comparator.comparing(t -> t.khachHang().getMaKH()));
        }
        trang = 1;
        capNhatBang();
    }

    private void capNhatBang() {
        int tong = daLoc.size();
        int soTrang = Math.max(1, (tong + soDongMoiTrang - 1) / soDongMoiTrang);
        trang = Math.min(trang, soTrang);
        int tu = (trang - 1) * soDongMoiTrang;
        int den = Math.min(tu + soDongMoiTrang, tong);
        trangHienTai = new ArrayList<>(daLoc.subList(Math.min(tu, tong), den));

        model.setRowCount(0);
        for (TomTat t : trangHienTai) {
            KhachHang kh = t.khachHang();
            model.addRow(new Object[]{
                kh.getMaKH(),
                kh.getTenKH(),
                kh.getNgaySinh() == null ? "" : kh.getNgaySinh().format(NGAY),
                kh.isGioiTinh() ? "Nam" : "Nữ",
                kh.getSdt(),
                t.lichGanNhat() == null ? "—" : t.lichGanNhat().format(NGAY_GIO),
                t.trangThaiHoSo(),
                "Xem · Đặt lịch"});
        }
        if (tong == 0) {
            lblKhongCoKetQua.setText("Không tìm thấy bệnh nhân");
            theNoiDung.show(pnlNoiDung, THE_TRONG);
        } else {
            theNoiDung.show(pnlNoiDung, THE_BANG);
        }
        lblHienThi.setText(tong == 0 ? "Không có bệnh nhân"
                : "Hiển thị " + (tu + 1) + "–" + den + " trong tổng số " + tong + " bệnh nhân");
        lblTong.setText(tong + " hồ sơ bệnh nhân · Thông tin được bảo mật theo phân quyền nhân viên");
        lblSapXep.setText(chkGanNhat.isSelected()
                ? "Sắp xếp: Làm khám gần nhất ↓" : "Sắp xếp: Mã bệnh nhân ↑");
        dungSoTrang(soTrang);
    }

    private void dungSoTrang(int soTrang) {
        pnlSoTrang.removeAll();
        java.util.TreeSet<Integer> cacTrang = new java.util.TreeSet<>();
        cacTrang.add(1);
        cacTrang.add(soTrang);
        for (int p = Math.max(1, trang - 1); p <= Math.min(soTrang, trang + 1); p++) {
            cacTrang.add(p);
        }
        int truoc = 0;
        for (int p : cacTrang) {
            if (truoc > 0 && p > truoc + 1) {
                JLabel dots = new JLabel("...");
                dots.setFont(font(11));
                dots.setForeground(MUTED);
                pnlSoTrang.add(dots);
            }
            JButton nut = new JButton(String.valueOf(p));
            nut.setFont(bold(11));
            nut.setPreferredSize(new Dimension(34, 28));
            if (p == trang) {
                nut.setBackground(TEAL);
                nut.setForeground(Color.WHITE);
            } else {
                nut.setBackground(Color.WHITE);
                nut.setForeground(TEXT);
            }
            nut.setFocusPainted(false);
            nut.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            nut.addActionListener(e -> {
                trang = p;
                capNhatBang();
            });
            pnlSoTrang.add(nut);
            truoc = p;
        }
        pnlSoTrang.revalidate();
        pnlSoTrang.repaint();
    }

    // ---------------------------------------------------------------- sự kiện

    private void addEvents() {
        txtTimKiem.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                henGio.restart();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                henGio.restart();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                henGio.restart();
            }
        });
        txtTimKiem.addActionListener(e -> taiDuLieu(txtTimKiem.getText()));
        cboTrangThai.addActionListener(e -> apDungLoc());
        chkGanNhat.addActionListener(e -> apDungLoc());
        btnXuat.addActionListener(e -> xuatExcel());
        btnThem.addActionListener(e -> moThem());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int hang = table.rowAtPoint(e.getPoint());
                int cot = table.columnAtPoint(e.getPoint());
                if (hang < 0) {
                    return;
                }
                table.setRowSelectionInterval(hang, hang);
                if (table.convertColumnIndexToModel(cot) == 7) {
                    moThaoTac(hang, e);
                } else if (e.getClickCount() == 2) {
                    moHoSo(hang);
                }
            }
        });
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int cot = table.columnAtPoint(e.getPoint());
                table.setCursor(Cursor.getPredefinedCursor(
                        table.convertColumnIndexToModel(cot) == 7 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        });
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "lamMoi");
        getActionMap().put("lamMoi", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                taiDuLieu(txtTimKiem.getText());
            }
        });
    }

    private void moThaoTac(int hang, MouseEvent e) {
        TomTat t = trangHienTai.get(hang);
        JPopupMenu popup = new JPopupMenu();
        var xem = new javax.swing.JMenuItem("Xem hồ sơ");
        xem.addActionListener(a -> moHoSo(t));
        popup.add(xem);
        var datLich = new javax.swing.JMenuItem("Đặt lịch hẹn");
        datLich.addActionListener(a -> JOptionPane.showMessageDialog(this,
                "Đặt lịch hẹn thuộc L2 (UC016) và chưa được triển khai trong L1.",
                "Thông báo", JOptionPane.INFORMATION_MESSAGE));
        datLich.setEnabled(false);
        popup.add(datLich);
        popup.show(table, e.getX(), e.getY());
    }

    private void moHoSo(int hang) {
        if (hang < 0 || hang >= trangHienTai.size()) {
            return;
        }
        moHoSo(trangHienTai.get(hang));
    }

    private void moHoSo(TomTat t) {
        DlgHoSoKhachHang.hien(SwingUtilities.getWindowAncestor(this), t,
                () -> taiDuLieu(txtTimKiem.getText()));
    }

    public void timKiemNhanh(String tuKhoa) {
        txtTimKiem.setText(tuKhoa);
    }

    private void moThem() {
        java.awt.Window owner = SwingUtilities.getWindowAncestor(this);
        try {
            KhachHang kh = DlgKhachHang.themMoi(owner);
            if (kh != null) {
                txtTimKiem.setText(kh.getMaKH());
                taiDuLieu(kh.getMaKH());
            }
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Không mở được form thêm bệnh nhân: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void xuatExcel() {
        JFileChooser chon = new JFileChooser();
        chon.setSelectedFile(new File("benh-nhan.xlsx"));
        if (chon.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chon.getSelectedFile();
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            file = new File(file.getParentFile(), file.getName() + ".xlsx");
        }
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Bệnh nhân");
            String[] tieuDe = {"Mã bệnh nhân", "Họ và tên", "Ngày sinh", "Giới tính",
                "Số điện thoại", "Lịch gần nhất", "Hồ sơ"};
            CellStyle header = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font fontHeader = wb.createFont();
            fontHeader.setBold(true);
            fontHeader.setColor(IndexedColors.WHITE.getIndex());
            header.setFont(fontHeader);
            header.setFillForegroundColor(IndexedColors.TEAL.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setBorderBottom(BorderStyle.THIN);
            header.setBorderTop(BorderStyle.THIN);
            header.setBorderLeft(BorderStyle.THIN);
            header.setBorderRight(BorderStyle.THIN);

            CellStyle body = wb.createCellStyle();
            body.setBorderBottom(BorderStyle.THIN);
            body.setBorderTop(BorderStyle.THIN);
            body.setBorderLeft(BorderStyle.THIN);
            body.setBorderRight(BorderStyle.THIN);

            Row hangTieuDe = sheet.createRow(0);
            for (int i = 0; i < tieuDe.length; i++) {
                Cell o = hangTieuDe.createCell(i);
                o.setCellValue(tieuDe[i]);
                o.setCellStyle(header);
            }

            int dong = 1;
            for (TomTat t : daLoc) {
                KhachHang kh = t.khachHang();
                Row r = sheet.createRow(dong++);
                datO(r, 0, kh.getMaKH(), body);
                datO(r, 1, kh.getTenKH(), body);
                datO(r, 2, kh.getNgaySinh() == null ? "" : kh.getNgaySinh().format(NGAY), body);
                datO(r, 3, kh.isGioiTinh() ? "Nam" : "Nữ", body);
                datO(r, 4, kh.getSdt(), body);
                datO(r, 5, t.lichGanNhat() == null ? "" : t.lichGanNhat().format(NGAY_GIO), body);
                datO(r, 6, t.trangThaiHoSo(), body);
            }
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, tieuDe.length - 1));
            int[] rong = {15, 30, 14, 12, 18, 22, 18};
            for (int i = 0; i < rong.length; i++) {
                sheet.setColumnWidth(i, rong[i] * 256);
            }
            try (FileOutputStream out = new FileOutputStream(file)) {
                wb.write(out);
            }
            JOptionPane.showMessageDialog(this, "Đã xuất " + daLoc.size() + " bệnh nhân ra Excel.",
                    "Xuất danh sách", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Không thể xuất danh sách Excel.", "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void datO(Row row, int cot, String giaTri, CellStyle style) {
        Cell o = row.createCell(cot);
        o.setCellValue(giaTri == null ? "" : giaTri);
        o.setCellStyle(style);
    }

    private void baoLoi(Throwable loi) {
        lblHienThi.setText(" ");
        if (loi instanceof BusinessException) {
            JOptionPane.showMessageDialog(this, loi.getMessage(), "Cảnh báo", JOptionPane.WARNING_MESSAGE);
        } else if (loi instanceof DataAccessException) {
            JOptionPane.showMessageDialog(this, "Không thể tải danh sách bệnh nhân.", "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Đã xảy ra lỗi, vui lòng thử lại.", "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    static String cheSdt(String sdt) {
        if (sdt == null || sdt.length() < 8) {
            return sdt == null ? "" : sdt;
        }
        return sdt.substring(0, 4) + "***" + sdt.substring(sdt.length() - 3);
    }

    // ---------------------------------------------------------------- renderer

    private static final class PadRenderer extends DefaultTableCellRenderer {

        PadRenderer(boolean boldName) {
            setFont(boldName ? bold(12) : font(12));
            setForeground(TEXT);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new EmptyBorder(0, 20, 0, 8));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            c.setForeground(TEXT);
            return c;
        }
    }

    private static final class HoSoRenderer extends JPanel implements TableCellRenderer {

        private final Pill pill = new Pill();

        HoSoRenderer() {
            super(new java.awt.GridBagLayout());
            setOpaque(true);
            add(pill);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            String trangThai = String.valueOf(value);
            switch (trangThai) {
                case "Mới" -> pill.set(trangThai, GREEN, new Color(0xE7F7EF));
                case "Đang điều trị" -> pill.set(trangThai, TEAL, TEAL_LIGHT);
                default -> pill.set(trangThai, MUTED, new Color(0xF0F2F5));
            }
            return this;
        }
    }

    private static final class Pill extends JLabel {

        private Color bg = TEAL_LIGHT;

        Pill() {
            super("", SwingConstants.CENTER);
            setOpaque(false);
            setFont(bold(10));
            setBorder(new EmptyBorder(4, 10, 4, 10));
        }

        void set(String text, Color fg, Color bg) {
            setText(text);
            setForeground(fg);
            this.bg = bg;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            FontMetrics fm = g2.getFontMetrics(getFont());
            int rong = Math.min(getWidth() - 4, fm.stringWidth(getText()) + 20);
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Double((getWidth() - rong) / 2.0, (getHeight() - 20) / 2.0,
                    rong, 20, 20, 20));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class ThaoTacRenderer extends DefaultTableCellRenderer {

        ThaoTacRenderer() {
            setFont(bold(11));
            setForeground(TEAL);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(new EmptyBorder(0, 6, 0, 6));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setForeground(TEAL);
            c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return c;
        }
    }
}
