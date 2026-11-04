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

import com.quanlynhakhoa.dao.LichHenDAO;
import com.quanlynhakhoa.entity.LichHen;
import com.quanlynhakhoa.entity.enums.TrangThaiLichHen;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import com.quanlynhakhoa.ui.dialog.DlgCapNhatLichHen;
import com.quanlynhakhoa.ui.dialog.DlgChiTietLichHen;
import com.quanlynhakhoa.ui.dialog.DlgDatLich;
import com.quanlynhakhoa.ui.dialog.HopThoaiLoi;
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
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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

public class PnlLichHen extends JPanel {

    private static final DateTimeFormatter NGAY =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String THE_BANG = "bang";
    private static final String THE_TRONG = "trong";
    private static final int DONG_MOI_TRANG = 9;
    private static final String[] COT = {"MÃ LỊCH", "NGÀY GIỜ", "BỆNH NHÂN", "SỐ ĐIỆN THOẠI", "DỊCH VỤ",
        "NHA SĨ", "TRẠNG THÁI"};

    /** Mục lọc trạng thái trong ô chọn (null = tất cả). */
    private record LocTrangThai(String nhan, TrangThaiLichHen trangThai) {

        @Override
        public String toString() {
            return nhan;
        }
    }

    private final LichHenDAO lichHenDAO = new LichHenDAO();

    private final JTextField txtTimKiem = new JTextField();
    private final JTextField txtTuNgay = new JTextField(8);
    private final JTextField txtDenNgay = new JTextField(8);
    private final JButton btnTimKiem = PnlTongQuanQuanLy.button("Tìm kiếm", LineIcon.Type.SEARCH, true);
    private final JButton btnHomNay = PnlTongQuanQuanLy.button("Hôm nay", null, false);
    private final JButton btnTuanNay = PnlTongQuanQuanLy.button("7 ngày tới", null, false);
    private final JComboBox<LocTrangThai> cboTrangThai = new JComboBox<>(new LocTrangThai[] {
        new LocTrangThai("Tất cả trạng thái", null),
        new LocTrangThai("Đã đặt", TrangThaiLichHen.DA_DAT),
        new LocTrangThai("Đã đến", TrangThaiLichHen.DA_DEN),
        new LocTrangThai("Hoàn tất", TrangThaiLichHen.HOAN_TAT),
        new LocTrangThai("Đã hủy", TrangThaiLichHen.DA_HUY),
        new LocTrangThai("Không đến", TrangThaiLichHen.KHONG_DEN)});
    private final JButton btnChiTiet = PnlTongQuanQuanLy.button("Xem chi tiết", LineIcon.Type.SEARCH, false);
    private final JButton btnCapNhat = PnlTongQuanQuanLy.button("Cập nhật", LineIcon.Type.CLIPBOARD, false);
    private final JButton btnDoiLich = PnlTongQuanQuanLy.button("Dời lịch", LineIcon.Type.CALENDAR, false);
    private final JButton btnHuyLich = PnlTongQuanQuanLy.button("Hủy lịch", null, false);
    private final JButton btnDat = PnlTongQuanQuanLy.button("Đặt lịch hẹn", LineIcon.Type.PLUS, true);
    private final JButton btnXuat = PnlTongQuanQuanLy.button("Xuất Excel", LineIcon.Type.INVOICE, false);
    private final JLabel lblTong = new JLabel("Đang tải lịch hẹn...");
    private final JLabel lblSapXep = new JLabel("Sắp xếp: Ngày giờ ↑");
    private final JLabel lblHienThi = new JLabel(" ");
    private final JLabel lblKhongCoKetQua = new JLabel(" ", SwingConstants.CENTER);
    private final JPanel pnlSoTrang = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
    private final CardLayout theNoiDung = new CardLayout();
    private final JPanel pnlNoiDung = new JPanel(theNoiDung);
    private final DefaultTableModel model = new DefaultTableModel(COT, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final Timer henGio;

    private List<LichHen> tatCa = new ArrayList<>();
    private List<LichHen> trangHienTai = new ArrayList<>();
    private SwingWorker<List<LichHen>, Void> worker;
    private String maCanChon;
    private int trang = 1;

    public PnlLichHen() {
        super(new BorderLayout(0, 14));
        henGio = new Timer(350, e -> taiDuLieu());
        henGio.setRepeats(false);
        initComponents();
        addEvents();
        datKhoang(LocalDate.now(), LocalDate.now().plusDays(6));
        taiDuLieu();
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
        JLabel tieuDe = new JLabel("Danh sách lịch hẹn");
        tieuDe.setFont(bold(21));
        tieuDe.setForeground(TEXT);
        lblTong.setFont(font(12));
        lblTong.setForeground(MUTED);
        text.add(tieuDe);
        text.add(Box.createVerticalStrut(4));
        text.add(lblTong);
        hang.add(text, BorderLayout.WEST);
        hang.add(btnDat, BorderLayout.EAST);
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
                "Tìm theo mã lịch, mã bệnh nhân, họ tên hoặc số điện thoại...");
        oTim.add(txtTimKiem, BorderLayout.CENTER);

        cboTrangThai.setFont(font(12));
        cboTrangThai.setPreferredSize(new Dimension(160, 36));
        for (JTextField tf : new JTextField[] {txtTuNgay, txtDenNgay}) {
            tf.setFont(font(12));
            tf.setPreferredSize(new Dimension(98, 36));
            tf.putClientProperty("JTextField.placeholderText", "dd/MM/yyyy");
        }
        btnXuat.setFont(bold(12));

        JPanel phai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        phai.setOpaque(false);
        phai.add(nhan("Từ"));
        phai.add(txtTuNgay);
        phai.add(nhan("đến"));
        phai.add(txtDenNgay);
        phai.add(cboTrangThai);
        phai.add(btnTimKiem);
        phai.add(btnHomNay);
        phai.add(btnTuanNay);
        phai.add(btnXuat);
        loc.add(oTim, BorderLayout.CENTER);
        loc.add(phai, BorderLayout.EAST);

        JPanel sapXep = new JPanel(new BorderLayout());
        sapXep.setOpaque(false);
        JLabel tatCaNhan = new JLabel("Tất cả lịch hẹn");
        tatCaNhan.setFont(bold(13));
        tatCaNhan.setForeground(TEXT);
        lblSapXep.setFont(font(11));
        lblSapXep.setForeground(MUTED);
        sapXep.add(tatCaNhan, BorderLayout.WEST);
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
        JLabel goiY = new JLabel("Hãy kiểm tra lại từ khóa, khoảng ngày hoặc trạng thái.", SwingConstants.CENTER);
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
        int[] rong = {105, 125, 180, 110, 180, 150, 110};
        for (int i = 0; i < rong.length; i++) {
            cot.getColumn(i).setPreferredWidth(rong[i]);
        }
        for (int i = 0; i <= 5; i++) {
            cot.getColumn(i).setCellRenderer(new PadRenderer(i == 2));
        }
        cot.getColumn(6).setCellRenderer(new TrangThaiRenderer());
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
        for (JButton nut : new JButton[] {btnChiTiet, btnCapNhat, btnDoiLich, btnHuyLich}) {
            nut.setFont(bold(12));
        }
        btnHuyLich.setForeground(new Color(0xC0392B));
        JPanel hanhDong = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        hanhDong.setOpaque(false);
        hanhDong.add(btnChiTiet);
        hanhDong.add(btnCapNhat);
        hanhDong.add(btnDoiLich);
        hanhDong.add(btnHuyLich);

        lblHienThi.setFont(font(10));
        lblHienThi.setForeground(MUTED);
        pnlSoTrang.setOpaque(false);
        JPanel phanTrang = new JPanel(new BorderLayout());
        phanTrang.setOpaque(false);
        phanTrang.add(lblHienThi, BorderLayout.WEST);
        phanTrang.add(pnlSoTrang, BorderLayout.EAST);

        JPanel chan = new JPanel(new BorderLayout(0, 10));
        chan.setOpaque(false);
        chan.add(hanhDong, BorderLayout.NORTH);
        chan.add(phanTrang, BorderLayout.SOUTH);
        capNhatNut();
        return chan;
    }

    private static JLabel nhan(String chu) {
        JLabel lbl = new JLabel(chu);
        lbl.setFont(font(11));
        lbl.setForeground(MUTED);
        return lbl;
    }

    // ---------------------------------------------------------------- sự kiện

    private void addEvents() {
        txtTimKiem.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                henGio.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                henGio.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                henGio.restart();
            }
        });
        txtTimKiem.addActionListener(e -> taiDuLieu());
        txtTuNgay.addActionListener(e -> taiDuLieu());
        txtDenNgay.addActionListener(e -> taiDuLieu());
        cboTrangThai.addActionListener(e -> taiDuLieu());
        btnTimKiem.addActionListener(e -> taiDuLieu());
        btnHomNay.addActionListener(e -> {
            datKhoang(LocalDate.now(), LocalDate.now());
            taiDuLieu();
        });
        btnTuanNay.addActionListener(e -> {
            datKhoang(LocalDate.now(), LocalDate.now().plusDays(6));
            taiDuLieu();
        });
        btnDat.addActionListener(e -> datLich());
        btnXuat.addActionListener(e -> xuatExcel());
        btnChiTiet.addActionListener(e -> xemChiTiet());
        btnCapNhat.addActionListener(e -> capNhat());
        btnDoiLich.addActionListener(e -> doiLich());
        btnHuyLich.addActionListener(e -> huyLich());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                capNhatNut();
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && lichDangChon() != null) {
                    xemChiTiet();
                }
            }
        });
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "lamMoi");
        getActionMap().put("lamMoi", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                taiDuLieu();
            }
        });
    }

    /** Cho bên ngoài (menu) làm mới danh sách mỗi khi mở màn hình. */
    public void lamMoi() {
        taiDuLieu();
    }

    private Window cuaSo() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private LichHen lichDangChon() {
        int hang = table.getSelectedRow();
        return hang >= 0 && hang < trangHienTai.size() ? trangHienTai.get(hang) : null;
    }

    private void capNhatNut() {
        LichHen lh = lichDangChon();
        boolean co = lh != null;
        boolean daDat = co && lh.getTrangThai() == TrangThaiLichHen.DA_DAT;
        btnChiTiet.setEnabled(co);
        btnCapNhat.setEnabled(daDat);
        btnDoiLich.setEnabled(daDat);
        btnHuyLich.setEnabled(daDat);
    }

    private void datLich() {
        LichHen lh = DlgDatLich.datMoi(cuaSo());
        if (lh != null) {
            maCanChon = lh.getMaLH();
            datKhoang(lh.getNgayGioHen().toLocalDate(), lh.getNgayGioHen().toLocalDate());
            txtTimKiem.setText("");
            cboTrangThai.setSelectedIndex(0);
            taiDuLieu();
        }
    }

    private void xemChiTiet() {
        LichHen lh = lichDangChon();
        if (lh != null) {
            DlgChiTietLichHen.hien(cuaSo(), lh);
        }
    }

    private void capNhat() {
        LichHen lh = lichDaDatDangChon("cập nhật");
        if (lh != null && DlgCapNhatLichHen.hien(cuaSo(), lh) != null) {
            maCanChon = lh.getMaLH();
            taiDuLieu();
        }
    }

    private void doiLich() {
        LichHen lh = lichDaDatDangChon("dời");
        if (lh == null) {
            return;
        }
        LichHen moi = DlgDatLich.doiLich(cuaSo(), lh);
        if (moi != null) {
            maCanChon = moi.getMaLH();
            datKhoang(moi.getNgayGioHen().toLocalDate(), moi.getNgayGioHen().toLocalDate());
            taiDuLieu();
        }
    }

    private void huyLich() {
        LichHen lh = lichDaDatDangChon("hủy");
        if (lh == null) {
            return;
        }
        JTextArea txaLyDo = new JTextArea(3, 30);
        txaLyDo.setLineWrap(true);
        txaLyDo.setWrapStyleWord(true);
        Object[] noiDung = {"Nhập lý do hủy lịch " + lh.getMaLH() + " của " + lh.getKhachHang().getTenKH() + ":",
            new JScrollPane(txaLyDo)};
        String lyDo;
        while (true) {
            int chon = JOptionPane.showConfirmDialog(this, noiDung, "Hủy lịch hẹn",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (chon != JOptionPane.OK_OPTION) {
                return;
            }
            lyDo = txaLyDo.getText().trim();
            if (!lyDo.isEmpty()) {
                break;
            }
            JOptionPane.showMessageDialog(this, "Vui lòng nhập lý do hủy.", "Dữ liệu chưa hợp lệ",
                    JOptionPane.WARNING_MESSAGE);
        }
        String ly = lyDo;
        btnHuyLich.setEnabled(false);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return lichHenDAO.huy(lh.getMaLH(), ly);
            }

            @Override
            protected void done() {
                try {
                    if (Boolean.TRUE.equals(get())) {
                        JOptionPane.showMessageDialog(PnlLichHen.this, "Hủy lịch hẹn thành công.", "Thành công",
                                JOptionPane.INFORMATION_MESSAGE);
                        maCanChon = lh.getMaLH();
                        taiDuLieu();
                    }
                } catch (Exception e) {
                    HopThoaiLoi.hien(PnlLichHen.this, e, "Không thể hủy lịch hẹn, vui lòng thử lại.");
                    capNhatNut();
                }
            }
        }.execute();
    }

    private LichHen lichDaDatDangChon(String viec) {
        LichHen lh = lichDangChon();
        if (lh == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn lịch hẹn cần " + viec + ".", "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        if (lh.getTrangThai() != TrangThaiLichHen.DA_DAT) {
            JOptionPane.showMessageDialog(this, "Chỉ " + viec + " được lịch hẹn đang ở trạng thái đã đặt.",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return lh;
    }

    // ---------------------------------------------------------------- dữ liệu

    private void datKhoang(LocalDate tu, LocalDate den) {
        txtTuNgay.setText(tu.format(NGAY));
        txtDenNgay.setText(den.format(NGAY));
    }

    private LocalDate[] khoangNgay() {
        try {
            return new LocalDate[] {LocalDate.parse(txtTuNgay.getText().trim(), NGAY),
                LocalDate.parse(txtDenNgay.getText().trim(), NGAY)};
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private void taiDuLieu() {
        LocalDate[] khoang = khoangNgay();
        if (khoang == null) {
            lblHienThi.setText("Ngày phải theo định dạng dd/MM/yyyy.");
            return;
        }
        if (khoang[1].isBefore(khoang[0])) {
            lblHienThi.setText("Khoảng ngày không hợp lệ: ngày kết thúc trước ngày bắt đầu.");
            return;
        }
        String tuKhoa = txtTimKiem.getText();
        TrangThaiLichHen trangThai = ((LocTrangThai) cboTrangThai.getSelectedItem()).trangThai();
        if (worker != null) {
            worker.cancel(true);
        }
        lblHienThi.setText("Đang tải dữ liệu...");
        worker = new SwingWorker<>() {
            @Override
            protected List<LichHen> doInBackground() {
                lichHenDAO.capNhatKhongDen();
                return lichHenDAO.timKiem(tuKhoa, khoang[0], khoang[1], trangThai);
            }

            @Override
            protected void done() {
                try {
                    tatCa = new ArrayList<>(get());
                    trang = 1;
                    capNhatBang();
                } catch (CancellationException ignored) {
                    return;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    lblHienThi.setText(" ");
                    HopThoaiLoi.hien(PnlLichHen.this, e, "Không thể tải danh sách lịch hẹn.");
                }
            }
        };
        worker.execute();
    }

    private void capNhatBang() {
        int tong = tatCa.size();
        if (maCanChon != null) {
            for (int i = 0; i < tong; i++) {
                if (maCanChon.equals(tatCa.get(i).getMaLH())) {
                    trang = i / DONG_MOI_TRANG + 1;
                    break;
                }
            }
        }
        int soTrang = Math.max(1, (tong + DONG_MOI_TRANG - 1) / DONG_MOI_TRANG);
        trang = Math.min(trang, soTrang);
        int tu = (trang - 1) * DONG_MOI_TRANG;
        int den = Math.min(tu + DONG_MOI_TRANG, tong);
        trangHienTai = new ArrayList<>(tatCa.subList(Math.min(tu, tong), den));

        model.setRowCount(0);
        for (LichHen lh : trangHienTai) {
            model.addRow(new Object[] {
                lh.getMaLH(),
                lh.getNgayGioHen().format(NGAY_GIO),
                lh.getKhachHang().getTenKH(),
                lh.getKhachHang().getSdt(),
                lh.getDichVu().getTenDV(),
                lh.getNhaSi().getHoTenNV(),
                nhanTrangThai(lh.getTrangThai())});
        }
        if (tong == 0) {
            boolean coTuKhoa = !txtTimKiem.getText().isBlank();
            lblKhongCoKetQua.setText(coTuKhoa ? "Không tìm thấy lịch hẹn" : "Chưa có lịch hẹn trong khoảng đã chọn");
            theNoiDung.show(pnlNoiDung, THE_TRONG);
        } else {
            theNoiDung.show(pnlNoiDung, THE_BANG);
            if (maCanChon != null) {
                for (int i = 0; i < trangHienTai.size(); i++) {
                    if (maCanChon.equals(trangHienTai.get(i).getMaLH())) {
                        table.setRowSelectionInterval(i, i);
                        break;
                    }
                }
                maCanChon = null;
            }
        }
        lblHienThi.setText(tong == 0 ? "Không có lịch hẹn"
                : "Hiển thị " + (tu + 1) + "–" + den + " trong tổng số " + tong + " lịch hẹn");
        long daDat = tatCa.stream().filter(l -> l.getTrangThai() == TrangThaiLichHen.DA_DAT).count();
        lblTong.setText(tong + " lịch hẹn · " + daDat + " lịch đang chờ");
        dungSoTrang(soTrang);
        capNhatNut();
    }

    private void dungSoTrang(int soTrang) {
        pnlSoTrang.removeAll();
        TreeSet<Integer> cacTrang = new TreeSet<>();
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

    // ---------------------------------------------------------------- xuất Excel

    private void xuatExcel() {
        JFileChooser chon = new JFileChooser();
        chon.setSelectedFile(new File("lich-hen.xlsx"));
        if (chon.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chon.getSelectedFile();
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            file = new File(file.getParentFile(), file.getName() + ".xlsx");
        }
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Lịch hẹn");
            String[] tieuDe = {"Mã lịch", "Ngày giờ", "Bệnh nhân", "Số điện thoại", "Dịch vụ", "Nha sĩ",
                "Trạng thái", "Ghi chú"};
            CellStyle header = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font fontHeader = wb.createFont();
            fontHeader.setBold(true);
            fontHeader.setColor(IndexedColors.WHITE.getIndex());
            header.setFont(fontHeader);
            header.setFillForegroundColor(IndexedColors.TEAL.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);
            vien(header);
            CellStyle body = wb.createCellStyle();
            vien(body);

            Row hangTieuDe = sheet.createRow(0);
            for (int i = 0; i < tieuDe.length; i++) {
                Cell o = hangTieuDe.createCell(i);
                o.setCellValue(tieuDe[i]);
                o.setCellStyle(header);
            }
            int dong = 1;
            for (LichHen lh : tatCa) {
                Row r = sheet.createRow(dong++);
                datO(r, 0, lh.getMaLH(), body);
                datO(r, 1, lh.getNgayGioHen().format(NGAY_GIO), body);
                datO(r, 2, lh.getKhachHang().getTenKH(), body);
                datO(r, 3, lh.getKhachHang().getSdt(), body);
                datO(r, 4, lh.getDichVu().getTenDV(), body);
                datO(r, 5, lh.getNhaSi().getHoTenNV(), body);
                datO(r, 6, nhanTrangThai(lh.getTrangThai()), body);
                datO(r, 7, lh.getGhiChu(), body);
            }
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, tieuDe.length - 1));
            int[] rong = {16, 18, 26, 16, 30, 24, 14, 34};
            for (int i = 0; i < rong.length; i++) {
                sheet.setColumnWidth(i, rong[i] * 256);
            }
            try (FileOutputStream out = new FileOutputStream(file)) {
                wb.write(out);
            }
            JOptionPane.showMessageDialog(this, "Đã xuất " + tatCa.size() + " lịch hẹn ra Excel.",
                    "Xuất danh sách", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Không thể xuất danh sách Excel.", "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void vien(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private static void datO(Row row, int cot, String giaTri, CellStyle style) {
        Cell o = row.createCell(cot);
        o.setCellValue(giaTri == null ? "" : giaTri);
        o.setCellStyle(style);
    }

    static String nhanTrangThai(TrangThaiLichHen trangThai) {
        return switch (trangThai) {
            case DA_DAT -> "Đã đặt";
            case DA_DEN -> "Đã đến";
            case HOAN_TAT -> "Hoàn tất";
            case DA_HUY -> "Đã hủy";
            case KHONG_DEN -> "Không đến";
        };
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

    private static final class TrangThaiRenderer extends JPanel implements TableCellRenderer {

        private final Pill pill = new Pill();

        TrangThaiRenderer() {
            super(new GridBagLayout());
            setOpaque(true);
            add(pill);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            String trangThai = String.valueOf(value);
            switch (trangThai) {
                case "Đã đặt" -> pill.set(trangThai, TEAL, TEAL_LIGHT);
                case "Đã đến" -> pill.set(trangThai, new Color(0xB26B00), new Color(0xFFF4E0));
                case "Hoàn tất" -> pill.set(trangThai, GREEN, new Color(0xE7F7EF));
                case "Đã hủy" -> pill.set(trangThai, new Color(0xC0392B), new Color(0xFDECEA));
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
}
