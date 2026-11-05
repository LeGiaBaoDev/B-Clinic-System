package com.quanlynhakhoa.ui.dialog;

import static com.quanlynhakhoa.ui.component.UiConstants.BACKGROUND;
import static com.quanlynhakhoa.ui.component.UiConstants.BORDER;
import static com.quanlynhakhoa.ui.component.UiConstants.GREEN;
import static com.quanlynhakhoa.ui.component.UiConstants.MUTED;
import static com.quanlynhakhoa.ui.component.UiConstants.SURFACE;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL_DARK;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL_LIGHT;
import static com.quanlynhakhoa.ui.component.UiConstants.TEXT;
import static com.quanlynhakhoa.ui.component.UiConstants.bold;
import static com.quanlynhakhoa.ui.component.UiConstants.font;

import com.quanlynhakhoa.dao.ChiTietPhieuKhamDAO;
import com.quanlynhakhoa.dao.ChiTietPhieuKhamDAO.DongThanhToan;
import com.quanlynhakhoa.dao.HoaDonDAO;
import com.quanlynhakhoa.dao.VoucherDAO;
import com.quanlynhakhoa.entity.HoaDon;
import com.quanlynhakhoa.entity.KhachHang;
import com.quanlynhakhoa.entity.LichHen;
import com.quanlynhakhoa.entity.Voucher;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

public class DlgThanhToan extends JDialog {

    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color RED = new Color(0xC0392B);
    private static final Color AMBER = new Color(0xB26B00);

    private record LuaChonVoucher(String nhan, Voucher voucher) {
        @Override
        public String toString() {
            return nhan;
        }
    }

    private record DuLieu(List<DongThanhToan> dong, List<Voucher> voucher) {
    }

    private final HoaDon hoaDon;
    private final Runnable sauThanhToan;
    private final HoaDonDAO hoaDonDAO = new HoaDonDAO();
    private final ChiTietPhieuKhamDAO chiTietDAO = new ChiTietPhieuKhamDAO();
    private final VoucherDAO voucherDAO = new VoucherDAO();

    private final List<DongThanhToan> dsDong = new ArrayList<>();
    private final DefaultTableModel modelDong = new DefaultTableModel(
            new String[]{"DỊCH VỤ", "ĐVT", "SL", "ĐƠN GIÁ", "THUẾ", "THÀNH TIỀN"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tblDong = new JTable(modelDong);
    private final JComboBox<LuaChonVoucher> cboVoucher = new JComboBox<>();
    private final JRadioButton radTienMat = new JRadioButton("Tiền mặt", true);
    private final JRadioButton radChuyenKhoan = new JRadioButton("Chuyển khoản");
    private final JTextField txtKhachDua = new JTextField();
    private final JLabel lblTongHang = new JLabel("0 đ");
    private final JLabel lblTongThue = new JLabel("0 đ");
    private final JLabel lblKCT = new JLabel("KCT: 0 đ");
    private final JLabel lblThue5 = new JLabel("Thuế 5%: 0 đ");
    private final JLabel lblThue10 = new JLabel("Thuế 10%: 0 đ");
    private final JLabel lblGiam = new JLabel("0 đ");
    private final JLabel lblTong = new JLabel("0 đ");
    private final JLabel lblTienThoi = new JLabel("0 đ");
    private final JLabel lblThongBao = new JLabel(" ");
    private final JButton btnThanhToan = new JButton("Thanh toán");
    private final JButton btnIn = new JButton("In phiếu thu PDF");
    private final JButton btnDong = new JButton("Đóng");

    private BigDecimal tongHang = BigDecimal.ZERO;
    private BigDecimal tongThue = BigDecimal.ZERO;
    private BigDecimal tongTruocGiam = BigDecimal.ZERO;
    private BigDecimal tienGiam = BigDecimal.ZERO;
    private BigDecimal tongThanhToan = BigDecimal.ZERO;
    private boolean daThanhToan;
    private boolean dangTai;
    private boolean dangCapNhatTien;

    private DlgThanhToan(Window owner, HoaDon hoaDon, Runnable sauThanhToan) {
        super(owner, "Thanh toán hóa đơn", ModalityType.APPLICATION_MODAL);
        this.hoaDon = hoaDon;
        this.sauThanhToan = sauThanhToan;
        khoiTao();
        addEvents();
        taiDuLieu();
        pack();
        setMinimumSize(new Dimension(980, 680));
        setSize(1040, 740);
        setLocationRelativeTo(owner);
    }

    public static void hien(Window owner, HoaDon hoaDon, Runnable sauThanhToan) {
        new DlgThanhToan(owner, hoaDon, sauThanhToan).setVisible(true);
    }

    private void khoiTao() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(BACKGROUND);
        root.setBorder(new EmptyBorder(20, 24, 16, 24));
        root.add(taoHeader(), BorderLayout.NORTH);

        JPanel noiDung = new JPanel(new GridLayout(1, 2, 16, 0));
        noiDung.setOpaque(false);
        noiDung.add(taoHoaDon());
        noiDung.add(taoThanhToan());
        root.add(noiDung, BorderLayout.CENTER);
        root.add(taoChan(), BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(btnThanhToan);
        btnIn.setEnabled(false);
    }

    private JPanel taoHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel tieuDe = new JLabel("Thanh toán hóa đơn");
        tieuDe.setFont(bold(21));
        tieuDe.setForeground(TEXT);
        JLabel phuDe = new JLabel(hoaDon.getMaHD() + " · Lập lúc "
                + hoaDon.getNgayLap().format(NGAY_GIO));
        phuDe.setFont(font(12));
        phuDe.setForeground(MUTED);
        text.add(tieuDe);
        text.add(Box.createVerticalStrut(4));
        text.add(phuDe);
        header.add(text, BorderLayout.WEST);
        JLabel trangThai = new JLabel(hoaDon.isTrangThai() ? "Đã thanh toán" : "Chờ thanh toán");
        trangThai.setFont(bold(11));
        trangThai.setForeground(hoaDon.isTrangThai() ? GREEN : AMBER);
        trangThai.setBorder(new EmptyBorder(5, 12, 5, 12));
        trangThai.setOpaque(true);
        trangThai.setBackground(hoaDon.isTrangThai() ? new Color(0xE8F6EE) : new Color(0xFFF4DE));
        header.add(trangThai, BorderLayout.EAST);
        return header;
    }

    private JPanel taoHoaDon() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.add(taoThongTinBenhNhan(), BorderLayout.NORTH);

        JPanel giua = new JPanel(new BorderLayout(0, 12));
        giua.setOpaque(false);
        giua.add(taoBangDichVu(), BorderLayout.CENTER);
        giua.add(taoTongHop(), BorderLayout.SOUTH);
        card.add(giua, BorderLayout.CENTER);
        return card;
    }

    private JPanel taoThongTinBenhNhan() {
        JPanel p = new JPanel(new GridLayout(4, 1, 0, 5));
        p.setOpaque(false);
        LichHen lh = hoaDon.getPhieuKham().getLichHen();
        KhachHang kh = lh == null ? null : lh.getKhachHang();
        p.add(dongThongTin("Bệnh nhân", kh == null ? "—" : kh.getTenKH()));
        p.add(dongThongTin("Mã bệnh nhân", kh == null ? "—" : kh.getMaKH()));
        p.add(dongThongTin("Nha sĩ", lh == null ? "—" : lh.getNhaSi().getHoTenNV()));
        p.add(dongThongTin("Phiếu khám", hoaDon.getPhieuKham().getMaPK()));
        return p;
    }

    private JPanel dongThongTin(String nhan, String giaTri) {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setOpaque(false);
        JLabel l = new JLabel(nhan);
        l.setFont(font(10));
        l.setForeground(MUTED);
        JLabel v = new JLabel(giaTri == null ? "—" : giaTri, SwingConstants.RIGHT);
        v.setFont(bold(12));
        v.setForeground(TEXT);
        p.add(l, BorderLayout.WEST);
        p.add(v, BorderLayout.EAST);
        return p;
    }

    private JPanel taoBangDichVu() {
        tblDong.setRowHeight(34);
        tblDong.setFont(font(11));
        tblDong.getTableHeader().setFont(bold(9));
        tblDong.getTableHeader().setBackground(new Color(0xF7F9FB));
        tblDong.getTableHeader().setForeground(MUTED);
        tblDong.setSelectionBackground(new Color(0xEFF6F5));
        tblDong.setSelectionForeground(TEXT);
        tblDong.setShowVerticalLines(false);
        tblDong.setGridColor(new Color(0xEDF1F5));
        tblDong.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] rong = {170, 48, 36, 82, 62, 100};
        for (int i = 0; i < rong.length; i++) {
            tblDong.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        }
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 1; i <= 4; i++) {
            tblDong.getColumnModel().getColumn(i).setCellRenderer(center);
        }
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        tblDong.getColumnModel().getColumn(5).setCellRenderer(right);

        JScrollPane scr = new JScrollPane(tblDong);
        scr.setBorder(BorderFactory.createLineBorder(BORDER));
        scr.getViewport().setBackground(Color.WHITE);
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        JLabel tieuDe = new JLabel("Chi tiết dịch vụ");
        tieuDe.setFont(bold(13));
        tieuDe.setForeground(TEXT);
        p.add(tieuDe, BorderLayout.NORTH);
        p.add(scr, BorderLayout.CENTER);
        return p;
    }

    private JPanel taoTongHop() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(2, 0, 0, 0));
        int dong = 0;
        themDongTong(p, dong++, "Tổng tiền hàng", lblTongHang, false);
        themDongTong(p, dong++, "Tổng thuế GTGT", lblTongThue, false);
        JPanel thue = new JPanel();
        thue.setOpaque(false);
        thue.setLayout(new BoxLayout(thue, BoxLayout.Y_AXIS));
        for (JLabel l : new JLabel[]{lblKCT, lblThue5, lblThue10}) {
            l.setFont(font(10));
            l.setForeground(MUTED);
            l.setHorizontalAlignment(SwingConstants.RIGHT);
            l.setAlignmentX(Component.RIGHT_ALIGNMENT);
            thue.add(l);
        }
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = dong++;
        c.gridx = 1;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(1, 0, 3, 0);
        p.add(thue, c);
        themDongTong(p, dong++, "Giảm voucher", lblGiam, false);
        themDongTong(p, dong++, "Tổng thanh toán", lblTong, true);
        return p;
    }

    private void themDongTong(JPanel p, int dong, String nhan, JLabel giaTri, boolean lon) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = dong;
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(lon ? 8 : 2, 0, lon ? 0 : 2, 12);
        JLabel l = new JLabel(nhan);
        l.setFont(lon ? bold(13) : font(11));
        l.setForeground(lon ? TEXT : MUTED);
        p.add(l, c);
        if (giaTri != null) {
            giaTri.setFont(lon ? bold(19) : font(11));
            giaTri.setForeground(lon ? TEAL : TEXT);
            c.gridx = 1;
            c.anchor = GridBagConstraints.EAST;
            c.insets = new Insets(lon ? 8 : 2, 0, lon ? 0 : 2, 0);
            p.add(giaTri, c);
        }
    }

    private JPanel taoThanhToan() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 14));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        int dong = 0;
        themDongForm(form, dong++, "Hình thức thanh toán", taoHinhThuc());
        themDongForm(form, dong++, "Voucher", cboVoucher);
        cboVoucher.setPreferredSize(new Dimension(280, 36));
        cboVoucher.setFont(font(12));

        JLabel lblKhachDua = new JLabel("Tiền khách đưa");
        lblKhachDua.setFont(font(11));
        lblKhachDua.setForeground(MUTED);
        txtKhachDua.setFont(bold(15));
        txtKhachDua.setPreferredSize(new Dimension(280, 38));
        txtKhachDua.setHorizontalAlignment(SwingConstants.RIGHT);
        themDongForm(form, dong++, "Tiền khách đưa", txtKhachDua);

        JLabel lblThoi = new JLabel("Tiền thối");
        lblThoi.setFont(font(11));
        lblThoi.setForeground(MUTED);
        lblTienThoi.setFont(bold(16));
        lblTienThoi.setForeground(GREEN);
        JPanel pThoi = new JPanel(new BorderLayout());
        pThoi.setOpaque(false);
        pThoi.setPreferredSize(new Dimension(280, 34));
        pThoi.add(lblTienThoi, BorderLayout.EAST);
        themDongForm(form, dong++, "Tiền thối", pThoi);

        lblThongBao.setFont(font(10));
        lblThongBao.setForeground(MUTED);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = dong;
        c.gridx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 0, 0, 0);
        form.add(lblThongBao, c);

        card.add(form, BorderLayout.NORTH);
        card.add(taoHopDong(), BorderLayout.CENTER);
        return card;
    }

    private JPanel taoHinhThuc() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(280, 34));
        ButtonGroup nhom = new ButtonGroup();
        for (JRadioButton r : new JRadioButton[]{radTienMat, radChuyenKhoan}) {
            r.setFont(font(12));
            r.setOpaque(false);
            r.setFocusPainted(false);
            nhom.add(r);
            p.add(r);
        }
        return p;
    }

    private JPanel taoHopDong() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel tieuDe = new JLabel("Số tiền cần thu");
        tieuDe.setFont(font(11));
        tieuDe.setForeground(MUTED);
        tieuDe.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel hop = new JPanel(new BorderLayout());
        hop.setOpaque(true);
        hop.setBackground(TEAL_LIGHT);
        hop.setBorder(new EmptyBorder(14, 16, 14, 16));
        hop.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
        JLabel tong = new JLabel("0 đ", SwingConstants.LEFT);
        tong.setFont(bold(26));
        tong.setForeground(TEAL_DARK);
        hop.add(tong, BorderLayout.WEST);
        JLabel nhan = new JLabel("Tổng thanh toán", SwingConstants.RIGHT);
        nhan.setFont(bold(12));
        nhan.setForeground(TEAL_DARK);
        hop.add(nhan, BorderLayout.EAST);
        p.add(tieuDe);
        p.add(Box.createVerticalStrut(5));
        p.add(hop);
        tong.setText("0 đ");
        lblTong.putClientProperty("tongHop", tong);
        return p;
    }

    private JPanel taoChan() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);
        btnDong.setFont(font(13));
        btnDong.setPreferredSize(new Dimension(100, 38));
        btnIn.setFont(bold(13));
        btnIn.setPreferredSize(new Dimension(160, 38));
        btnThanhToan.setFont(bold(13));
        btnThanhToan.setBackground(TEAL);
        btnThanhToan.setForeground(Color.WHITE);
        btnThanhToan.setPreferredSize(new Dimension(190, 38));
        btnThanhToan.setIcon(new LineIcon(LineIcon.Type.INVOICE, Color.WHITE, 16));
        btnThanhToan.setIconTextGap(7);
        footer.add(btnDong);
        footer.add(btnIn);
        footer.add(btnThanhToan);
        return footer;
    }

    private void themDongForm(JPanel p, int dong, String nhan, Component field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = dong;
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(7, 0, 7, 16);
        JLabel l = new JLabel(nhan);
        l.setFont(font(11));
        l.setForeground(MUTED);
        p.add(l, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(7, 0, 7, 0);
        p.add(field, c);
    }

    private void addEvents() {
        btnDong.addActionListener(e -> dispose());
        btnThanhToan.addActionListener(e -> thanhToan());
        btnIn.addActionListener(e -> luuPhieuThuPdf());
        radTienMat.addActionListener(e -> capNhatHinhThuc());
        radChuyenKhoan.addActionListener(e -> capNhatHinhThuc());
        cboVoucher.addActionListener(e -> capNhatTong());
        ((AbstractDocument) txtKhachDua.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
                    throws BadLocationException {
                super.insertString(fb, offset, chiGiuSoTien(text), attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                super.replace(fb, offset, length, chiGiuSoTien(text), attrs);
            }
        });
        txtKhachDua.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                capNhatTienThoi();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                capNhatTienThoi();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                capNhatTienThoi();
            }
        });
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void taiDuLieu() {
        dangTai = true;
        btnThanhToan.setEnabled(false);
        cboVoucher.setEnabled(false);
        new SwingWorker<DuLieu, Void>() {
            @Override
            protected DuLieu doInBackground() {
                List<DongThanhToan> dong = chiTietDAO.layDongThanhToan(hoaDon.getPhieuKham().getMaPK());
                List<Voucher> voucher = hoaDon.getVoucher() != null
                        ? List.of(hoaDon.getVoucher())
                        : voucherDAO.layConHieuLuc(LocalDate.now());
                return new DuLieu(dong, voucher);
            }

            @Override
            protected void done() {
                try {
                    DuLieu duLieu = get();
                    dsDong.clear();
                    dsDong.addAll(duLieu.dong());
                    modelDong.setRowCount(0);
                    for (DongThanhToan d : dsDong) {
                        modelDong.addRow(new Object[]{
                            d.tenDV(), d.donViTinh(), d.soLuong(), money(d.donGia()),
                            nhanThue(d.mucThueGTGT()), money(d.thanhTien().add(d.tienThue()))});
                    }
                    cboVoucher.removeAllItems();
                    cboVoucher.addItem(new LuaChonVoucher("Không áp dụng", null));
                    for (Voucher v : duLieu.voucher()) {
                        cboVoucher.addItem(new LuaChonVoucher(v.getMaVoucher() + " · "
                                + v.getTenVoucher(), v));
                    }
                    if (hoaDon.getVoucher() != null) {
                        chonVoucher(hoaDon.getVoucher().getMaVoucher());
                        cboVoucher.setEnabled(false);
                    } else {
                        cboVoucher.setEnabled(true);
                    }
                    capNhatTong();
                    if (hoaDon.isTrangThai()) {
                        khoaDaThanhToan();
                    } else {
                        btnThanhToan.setEnabled(!dsDong.isEmpty());
                    }
                    lblThongBao.setText(" ");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    HopThoaiLoi.hien(DlgThanhToan.this, e, "Không thể tải chi tiết hóa đơn.");
                } finally {
                    dangTai = false;
                }
            }
        }.execute();
    }

    private void chonVoucher(String ma) {
        for (int i = 0; i < cboVoucher.getItemCount(); i++) {
            LuaChonVoucher lc = cboVoucher.getItemAt(i);
            if (lc.voucher() != null && lc.voucher().getMaVoucher().equals(ma)) {
                cboVoucher.setSelectedIndex(i);
                return;
            }
        }
    }

    private void capNhatTong() {
        if (dangTai || dsDong.isEmpty()) {
            return;
        }
        tongHang = BigDecimal.ZERO;
        tongThue = BigDecimal.ZERO;
        Map<Integer, BigDecimal> nhomThue = new TreeMap<>();
        for (DongThanhToan d : dsDong) {
            tongHang = tongHang.add(d.thanhTien());
            tongThue = tongThue.add(d.tienThue());
            int thue = d.mucThueGTGT().intValueExact();
            nhomThue.merge(thue, d.thanhTien(), BigDecimal::add);
        }
        tongTruocGiam = tongHang.add(tongThue);
        LuaChonVoucher lc = (LuaChonVoucher) cboVoucher.getSelectedItem();
        Voucher vc = lc == null ? null : lc.voucher();
        if (vc == null && hoaDon.getVoucher() != null) {
            vc = hoaDon.getVoucher();
        }
        tienGiam = vc == null ? BigDecimal.ZERO
                : HoaDonDAO.tinhTienGiam(vc, tongTruocGiam);
        tongThanhToan = tongTruocGiam.subtract(tienGiam).max(BigDecimal.ZERO);

        BigDecimal kct = nhomThue.getOrDefault(0, BigDecimal.ZERO);
        BigDecimal base5 = nhomThue.getOrDefault(5, BigDecimal.ZERO);
        BigDecimal base10 = nhomThue.getOrDefault(10, BigDecimal.ZERO);
        BigDecimal thue5 = base5.multiply(BigDecimal.valueOf(5))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        BigDecimal thue10 = base10.multiply(BigDecimal.valueOf(10))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        lblTongHang.setText(money(tongHang));
        lblTongThue.setText(money(tongThue));
        lblKCT.setText("KCT: " + money(kct));
        lblThue5.setText("Thuế 5%: " + money(thue5));
        lblThue10.setText("Thuế 10%: " + money(thue10));
        lblGiam.setText(tienGiam.signum() > 0 ? "-" + money(tienGiam) : "0 đ");
        lblGiam.setForeground(tienGiam.signum() > 0 ? GREEN : TEXT);
        lblTong.setText(money(tongThanhToan));
        JLabel tongHop = (JLabel) lblTong.getClientProperty("tongHop");
        if (tongHop != null) {
            tongHop.setText(money(tongThanhToan));
        }
        capNhatHinhThuc();
    }

    private void capNhatHinhThuc() {
        boolean tienMat = radTienMat.isSelected();
        dangCapNhatTien = true;
        txtKhachDua.setEditable(tienMat);
        txtKhachDua.setBackground(tienMat ? SURFACE : new Color(0xF4F6F8));
        txtKhachDua.setText(moneySo(tongThanhToan));
        dangCapNhatTien = false;
        lblThongBao.setForeground(MUTED);
        lblThongBao.setText(" ");
        capNhatTienThoi();
    }

    private void capNhatTienThoi() {
        if (dangCapNhatTien || daThanhToan) {
            return;
        }
        BigDecimal khachDua = soTienNhap();
        if (radChuyenKhoan.isSelected()) {
            lblTienThoi.setForeground(GREEN);
            lblTienThoi.setText("0 đ");
            return;
        }
        if (khachDua == null) {
            lblTienThoi.setForeground(MUTED);
            lblTienThoi.setText("—");
            return;
        }
        BigDecimal thoi = khachDua.subtract(tongThanhToan);
        if (thoi.signum() < 0) {
            lblTienThoi.setForeground(RED);
            lblTienThoi.setText("Thiếu " + money(thoi.abs()));
        } else {
            lblTienThoi.setForeground(GREEN);
            lblTienThoi.setText(money(thoi));
        }
    }

    private void thanhToan() {
        if (daThanhToan || dangTai) {
            return;
        }
        if (dsDong.isEmpty()) {
            lblThongBao.setForeground(RED);
            lblThongBao.setText("Hóa đơn chưa có dịch vụ để thanh toán.");
            return;
        }
        boolean tienMat = radTienMat.isSelected();
        BigDecimal khachDua = tienMat ? soTienNhap() : tongThanhToan;
        if (khachDua == null) {
            lblThongBao.setForeground(RED);
            lblThongBao.setText("Vui lòng nhập số tiền khách đưa.");
            txtKhachDua.requestFocusInWindow();
            return;
        }
        if (tienMat && khachDua.compareTo(tongThanhToan) < 0) {
            lblThongBao.setForeground(RED);
            lblThongBao.setText("Số tiền khách đưa chưa đủ.");
            txtKhachDua.requestFocusInWindow();
            return;
        }
        LuaChonVoucher lc = (LuaChonVoucher) cboVoucher.getSelectedItem();
        String maVoucher = lc == null || lc.voucher() == null ? null : lc.voucher().getMaVoucher();
        String hinhThuc = tienMat ? HoaDon.TIEN_MAT : HoaDon.CHUYEN_KHOAN;
        btnThanhToan.setEnabled(false);
        btnDong.setEnabled(false);
        lblThongBao.setForeground(MUTED);
        lblThongBao.setText("Đang xử lý thanh toán...");
        new SwingWorker<BigDecimal, Void>() {
            @Override
            protected BigDecimal doInBackground() {
                return hoaDonDAO.thanhToan(hoaDon.getMaHD(), hinhThuc, khachDua, maVoucher);
            }

            @Override
            protected void done() {
                try {
                    BigDecimal thoi = get();
                    daThanhToan = true;
                    khoaDaThanhToan();
                    lblTienThoi.setText(money(thoi));
                    lblThongBao.setForeground(GREEN);
                    lblThongBao.setText("Đã thanh toán thành công.");
                    if (sauThanhToan != null) {
                        sauThanhToan.run();
                    }
                    int chon = JOptionPane.showConfirmDialog(DlgThanhToan.this,
                            "Thanh toán thành công.\nTiền thối: " + money(thoi)
                            + "\n\nXuất phiếu thu PDF ngay bây giờ?",
                            "Thanh toán thành công", JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE);
                    if (chon == JOptionPane.YES_OPTION) {
                        luuPhieuThuPdf();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    btnThanhToan.setEnabled(true);
                    btnDong.setEnabled(true);
                    lblThongBao.setForeground(RED);
                    lblThongBao.setText("Thanh toán thất bại.");
                    HopThoaiLoi.hien(DlgThanhToan.this, e, "Không thể thanh toán hóa đơn.");
                }
            }
        }.execute();
    }

    private void khoaDaThanhToan() {
        btnThanhToan.setEnabled(false);
        btnIn.setEnabled(true);
        btnDong.setEnabled(true);
        txtKhachDua.setEditable(false);
        cboVoucher.setEnabled(false);
        radTienMat.setEnabled(false);
        radChuyenKhoan.setEnabled(false);
    }

    private void luuPhieuThuPdf() {
        JPanel phieu = taoPhieuThu();
        Dimension kichThuoc = phieu.getPreferredSize();
        phieu.setSize(new Dimension(720, kichThuoc.height));
        phieu.doLayout();
        JFileChooser chon = new JFileChooser();
        chon.setSelectedFile(new File("phieu-thu-" + hoaDon.getMaHD() + ".pdf"));
        if (chon.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chon.getSelectedFile();
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            file = new File(file.getParentFile(), file.getName() + ".pdf");
        }
        try {
            BufferedImage image = new BufferedImage(phieu.getWidth() * 4,
                    phieu.getHeight() * 4, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = image.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, image.getWidth(), image.getHeight());
            g2.scale(4, 4);
            phieu.printAll(g2);
            g2.dispose();

            try (PDDocument doc = new PDDocument()) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                PDImageXObject pdfImage = LosslessFactory.createFromImage(doc, image);
                float margin = 28;
                float rong = page.getMediaBox().getWidth() - margin * 2;
                float cao = page.getMediaBox().getHeight() - margin * 2;
                float tiLe = Math.min(rong / image.getWidth(), cao / image.getHeight());
                float veRong = image.getWidth() * tiLe;
                float veCao = image.getHeight() * tiLe;
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.drawImage(pdfImage, margin + (rong - veRong) / 2,
                            page.getMediaBox().getHeight() - margin - veCao, veRong, veCao);
                }
                doc.save(file);
            }
            JOptionPane.showMessageDialog(this, "Đã lưu phiếu thu:\n" + file.getAbsolutePath(),
                    "Lưu PDF thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | SecurityException e) {
            JOptionPane.showMessageDialog(this, "Không thể lưu file PDF: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel taoPhieuThu() {
        JPanel phieu = new JPanel();
        phieu.setBackground(Color.WHITE);
        phieu.setLayout(new BoxLayout(phieu, BoxLayout.Y_AXIS));
        phieu.setBorder(new EmptyBorder(24, 30, 24, 30));

        JPanel dau = new JPanel(new BorderLayout());
        dau.setOpaque(false);
        JPanel clinic = new JPanel();
        clinic.setOpaque(false);
        clinic.setLayout(new BoxLayout(clinic, BoxLayout.Y_AXIS));
        JLabel ten = new JLabel("NHA KHOA B-CLINIC");
        ten.setFont(bold(16));
        ten.setForeground(TEXT);
        JLabel diaChi = new JLabel("12 Nguyễn Văn Bảo, phường Hạnh Thông, TP. Hồ Chí Minh");
        diaChi.setFont(font(9));
        diaChi.setForeground(MUTED);
        JLabel dienThoai = new JLabel("Điện thoại: 0372415720");
        dienThoai.setFont(font(9));
        dienThoai.setForeground(MUTED);
        clinic.add(ten);
        clinic.add(diaChi);
        clinic.add(dienThoai);
        dau.add(clinic, BorderLayout.WEST);
        JPanel tieuDe = new JPanel();
        tieuDe.setOpaque(false);
        tieuDe.setLayout(new BoxLayout(tieuDe, BoxLayout.Y_AXIS));
        JLabel phieuThu = new JLabel("PHIẾU THU");
        phieuThu.setFont(bold(20));
        phieuThu.setForeground(TEXT);
        JLabel so = new JLabel("Số: " + hoaDon.getMaHD());
        so.setFont(font(10));
        so.setForeground(MUTED);
        tieuDe.add(phieuThu);
        tieuDe.add(so);
        dau.add(tieuDe, BorderLayout.EAST);
        phieu.add(dau);
        phieu.add(Box.createVerticalStrut(8));
        phieu.add(duong());
        phieu.add(Box.createVerticalStrut(10));

        LichHen lh = hoaDon.getPhieuKham().getLichHen();
        KhachHang kh = lh == null ? null : lh.getKhachHang();
        phieu.add(dongPhieu("Bệnh nhân", kh == null ? "—" : kh.getTenKH()));
        phieu.add(dongPhieu("Mã bệnh nhân", kh == null ? "—" : kh.getMaKH()));
        phieu.add(dongPhieu("Nha sĩ điều trị", lh == null ? "—" : lh.getNhaSi().getHoTenNV()));
        phieu.add(dongPhieu("Ngày lập", hoaDon.getNgayLap().format(NGAY_GIO)));
        phieu.add(dongPhieu("Hình thức", radTienMat.isSelected()
                ? HoaDon.TIEN_MAT : HoaDon.CHUYEN_KHOAN));
        phieu.add(Box.createVerticalStrut(10));
        phieu.add(duong());
        phieu.add(Box.createVerticalStrut(8));

        JPanel cot = new JPanel(new BorderLayout());
        cot.setOpaque(false);
        JLabel dv = new JLabel("DỊCH VỤ");
        dv.setFont(bold(9));
        dv.setForeground(MUTED);
        JLabel tt = new JLabel("THÀNH TIỀN");
        tt.setFont(bold(9));
        tt.setForeground(MUTED);
        cot.add(dv, BorderLayout.WEST);
        cot.add(tt, BorderLayout.EAST);
        phieu.add(cot);
        phieu.add(Box.createVerticalStrut(6));
        for (DongThanhToan d : dsDong) {
            JPanel dong = new JPanel(new BorderLayout(8, 0));
            dong.setOpaque(false);
            dong.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            JLabel trai = new JLabel("<html><b>" + escape(d.tenDV()) + "</b><br>"
                    + d.soLuong() + " × " + money(d.donGia()) + " · " + nhanThue(d.mucThueGTGT())
                    + " · " + escape(d.donViTinh()) + "</html>");
            trai.setFont(font(9));
            trai.setForeground(TEXT);
            JLabel phai = new JLabel(money(d.thanhTien().add(d.tienThue())));
            phai.setFont(font(10));
            phai.setForeground(TEXT);
            dong.add(trai, BorderLayout.CENTER);
            dong.add(phai, BorderLayout.EAST);
            phieu.add(dong);
        }
        phieu.add(Box.createVerticalStrut(8));
        phieu.add(duong());
        phieu.add(Box.createVerticalStrut(8));
        phieu.add(dongTien("Tổng tiền hàng", money(tongHang), false));
        phieu.add(dongTien("Thuế GTGT", money(tongThue), false));
        if (tienGiam.signum() > 0) {
            phieu.add(dongTien("Giảm voucher", "-" + money(tienGiam), false));
        }
        phieu.add(dongTien("Tổng thanh toán", money(tongThanhToan), true));
        BigDecimal khachDua = radTienMat.isSelected() ? soTienNhap() : tongThanhToan;
        if (khachDua == null) {
            khachDua = tongThanhToan;
        }
        phieu.add(dongTien("Tiền khách đưa", money(khachDua), false));
        phieu.add(dongTien("Tiền thối", money(khachDua.subtract(tongThanhToan).max(BigDecimal.ZERO)), false));
        phieu.add(Box.createVerticalStrut(18));

        JPanel ky = new JPanel(new GridLayout(1, 2, 30, 0));
        ky.setOpaque(false);
        ky.add(taoChuKy("Người nộp tiền"));
        ky.add(taoChuKy("Thu ngân"));
        phieu.add(ky);
        return phieu;
    }

    private JPanel dongPhieu(String nhan, String giaTri) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        JLabel l = new JLabel(nhan);
        l.setFont(font(9));
        l.setForeground(MUTED);
        JLabel v = new JLabel(giaTri == null ? "—" : giaTri);
        v.setFont(font(10));
        v.setForeground(TEXT);
        p.add(l, BorderLayout.WEST);
        p.add(v, BorderLayout.EAST);
        return p;
    }

    private JPanel dongTien(String nhan, String giaTri, boolean dam) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, dam ? 30 : 22));
        JLabel l = new JLabel(nhan);
        l.setFont(dam ? bold(12) : font(10));
        l.setForeground(dam ? TEXT : MUTED);
        JLabel v = new JLabel(giaTri);
        v.setFont(dam ? bold(15) : font(10));
        v.setForeground(dam ? TEAL : TEXT);
        p.add(l, BorderLayout.WEST);
        p.add(v, BorderLayout.EAST);
        return p;
    }

    private JPanel taoChuKy(String vaiTro) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = new JLabel(vaiTro);
        l.setFont(font(9));
        l.setForeground(MUTED);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel phu = new JLabel("(Ký và ghi rõ họ tên)");
        phu.setFont(font(8));
        phu.setForeground(MUTED);
        phu.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(l);
        p.add(phu);
        p.add(Box.createVerticalStrut(42));
        return p;
    }

    private JSeparator duong() {
        JSeparator s = new JSeparator();
        s.setForeground(BORDER);
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return s;
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String nhanThue(BigDecimal muc) {
        return muc == null || muc.signum() == 0 ? "KCT" : muc.stripTrailingZeros().toPlainString() + "%";
    }

    private static String money(BigDecimal v) {
        return moneySo(v) + " đ";
    }

    private static String moneySo(BigDecimal v) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.of("vi", "VN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(v == null ? BigDecimal.ZERO : v.setScale(0, RoundingMode.HALF_UP));
    }

    private static String chiGiuSoTien(String text) {
        return text == null ? "" : text.replaceAll("[^0-9.,]", "");
    }

    private BigDecimal soTienNhap() {
        String digits = txtKhachDua.getText() == null ? "" : txtKhachDua.getText().replaceAll("[^0-9]", "");
        return digits.isBlank() ? null : new BigDecimal(digits);
    }
}
