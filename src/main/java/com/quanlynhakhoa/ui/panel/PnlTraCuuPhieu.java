package com.quanlynhakhoa.ui.panel;

import static com.quanlynhakhoa.ui.component.UiConstants.BACKGROUND;
import static com.quanlynhakhoa.ui.component.UiConstants.BORDER;
import static com.quanlynhakhoa.ui.component.UiConstants.GREEN;
import static com.quanlynhakhoa.ui.component.UiConstants.MUTED;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL;
import static com.quanlynhakhoa.ui.component.UiConstants.TEXT;
import static com.quanlynhakhoa.ui.component.UiConstants.bold;
import static com.quanlynhakhoa.ui.component.UiConstants.font;

import com.quanlynhakhoa.dao.ChiTietPhieuKhamDAO;
import com.quanlynhakhoa.dao.HoaDonDAO;
import com.quanlynhakhoa.dao.PhieuKhamDAO;
import com.quanlynhakhoa.entity.ChiTietPhieuKham;
import com.quanlynhakhoa.entity.HoaDon;
import com.quanlynhakhoa.entity.PhieuKham;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.TrangThaiPhieuKham;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import com.quanlynhakhoa.ui.dialog.DlgTaiKham;
import com.quanlynhakhoa.ui.dialog.DlgThanhToan;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

public class PnlTraCuuPhieu extends JPanel {

    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] COT = {"Mã phiếu", "Ngày lập", "Bệnh nhân", "Nha sĩ", "Trạng thái"};

    private final PhieuKhamDAO phieuKhamDAO = new PhieuKhamDAO();
    private final ChiTietPhieuKhamDAO chiTietDAO = new ChiTietPhieuKhamDAO();
    private final HoaDonDAO hoaDonDAO = new HoaDonDAO();
    private final boolean xemDayDu;
    private final List<PhieuKham> dsPhieu = new ArrayList<>();

    private final JTextField txtTimKiem = new JTextField();
    private final JButton btnTim = new JButton("Tìm kiếm");
    private final JComboBox<String> cboTrangThai = new JComboBox<>(
            new String[]{"Tất cả trạng thái", "Chờ khám", "Đang khám", "Hoàn tất"});
    private final DefaultTableModel modelPhieu = new DefaultTableModel(COT, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tblPhieu = new JTable(modelPhieu) {
        @Override
        public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            Component c = super.prepareRenderer(renderer, row, column);
            int modelRow = convertRowIndexToModel(row);
            boolean choKham = modelRow >= 0 && modelRow < dsPhieu.size()
                    && dsPhieu.get(modelRow).getTrangThai() == TrangThaiPhieuKham.CHO_KHAM;
            if (choKham) {
                c.setBackground(new Color(0xDDF2E9));
            } else if (!isRowSelected(row)) {
                c.setBackground(Color.WHITE);
            }
            return c;
        }
    };
    private final JLabel lblTen = new JLabel(" ");
    private final JLabel lblMa = new JLabel(" ");
    private final JLabel lblNgay = new JLabel(" ");
    private final JLabel lblNhaSi = new JLabel(" ");
    private final PillLabel lblTrangThai = new PillLabel();
    private final JLabel lblLoiDan = new JLabel(" ");
    private final JLabel lblTong = new JLabel("0 đ");
    private final JButton btnThanhToan = new JButton("Thanh toán");
    private final JButton btnTaiKham = new JButton("Đặt tái khám");
    private final DefaultTableModel modelDichVu = new DefaultTableModel();
    private final JTable tblDichVu = new JTable(modelDichVu);
    private final JLabel lblKhongCo = new JLabel("Chưa chọn phiếu khám", SwingConstants.CENTER);

    public PnlTraCuuPhieu(TaiKhoan taiKhoan) {
        super(new BorderLayout(0, 14));
        this.xemDayDu = taiKhoan != null && taiKhoan.getVaiTro() == VaiTro.NHASI;
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 20, 24));
        initComponents();
        addEvents();
        taiDanhSach("");
    }

    public void lamMoi() {
        taiDanhSach("");
    }

    private void initComponents() {
        JPanel dau = new JPanel(new BorderLayout(0, 12));
        dau.setOpaque(false);
        JPanel title = new JPanel();
        title.setOpaque(false);
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        JLabel tieuDe = new JLabel("Tra cứu phiếu khám");
        tieuDe.setFont(bold(21));
        tieuDe.setForeground(TEXT);
        JLabel phuDe = new JLabel(xemDayDu
                ? "Tìm phiếu khám để xem đầy đủ chẩn đoán, dịch vụ và lời dặn."
                : "Tìm phiếu khám để xem dịch vụ và chi phí; thông tin chuyên môn được ẩn.");
        phuDe.setFont(font(12));
        phuDe.setForeground(MUTED);
        title.add(tieuDe);
        title.add(Box.createVerticalStrut(4));
        title.add(phuDe);
        dau.add(title, BorderLayout.NORTH);

        SurfacePanel tim = new SurfacePanel(new BorderLayout(8, 0));
        tim.setBorder(new EmptyBorder(6, 12, 6, 10));
        tim.add(new JLabel(new LineIcon(LineIcon.Type.SEARCH, MUTED, 16)), BorderLayout.WEST);
        txtTimKiem.setFont(font(12));
        txtTimKiem.setBorder(null);
        txtTimKiem.setOpaque(false);
        txtTimKiem.putClientProperty("JTextField.placeholderText",
                "Nhập mã phiếu, tên bệnh nhân hoặc số điện thoại...");
        tim.add(txtTimKiem, BorderLayout.CENTER);
        btnTim.setFont(bold(12));
        btnTim.setBackground(TEAL);
        btnTim.setForeground(Color.WHITE);
        cboTrangThai.setFont(font(12));
        cboTrangThai.setPreferredSize(new Dimension(145, 30));
        JPanel loc = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        loc.setOpaque(false);
        loc.add(cboTrangThai);
        loc.add(btnTim);
        tim.add(loc, BorderLayout.EAST);
        dau.add(tim, BorderLayout.CENTER);
        add(dau, BorderLayout.NORTH);

        cauHinhBang();
        JScrollPane scr = new JScrollPane(tblPhieu);
        scr.setBorder(BorderFactory.createLineBorder(BORDER));
        scr.getViewport().setBackground(Color.WHITE);

        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        JLabel tieuDeBang = new JLabel("Danh sách phiếu khám");
        tieuDeBang.setFont(bold(13));
        tieuDeBang.setForeground(TEXT);
        card.add(tieuDeBang, BorderLayout.NORTH);
        card.add(scr, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
        add(taoChiTiet(), BorderLayout.EAST);
    }

    private void cauHinhBang() {
        tblPhieu.setRowHeight(42);
        tblPhieu.setFont(font(12));
        tblPhieu.getTableHeader().setFont(bold(10));
        tblPhieu.getTableHeader().setBackground(new Color(0xF7F9FB));
        tblPhieu.getTableHeader().setForeground(MUTED);
        tblPhieu.setShowVerticalLines(false);
        tblPhieu.setGridColor(new Color(0xEDF1F5));
        tblPhieu.setSelectionBackground(new Color(0xEFF6F5));
        tblPhieu.setSelectionForeground(TEXT);
        tblPhieu.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] rong = {110, 150, 190, 170, 110};
        for (int i = 0; i < rong.length; i++) {
            tblPhieu.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        }
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        tblPhieu.getColumnModel().getColumn(4).setCellRenderer(center);
    }

    private JPanel taoChiTiet() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.setPreferredSize(new Dimension(430, 0));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JPanel dong = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        dong.setOpaque(false);
        lblTen.setFont(bold(17));
        lblTen.setForeground(TEXT);
        lblMa.setFont(font(10));
        lblMa.setForeground(MUTED);
        dong.add(lblTen);
        dong.add(lblMa);
        JPanel dong2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        dong2.setOpaque(false);
        lblNgay.setFont(font(10));
        lblNgay.setForeground(MUTED);
        lblNhaSi.setFont(font(10));
        lblNhaSi.setForeground(MUTED);
        dong2.add(lblNgay);
        dong2.add(lblNhaSi);
        header.add(dong);
        header.add(Box.createVerticalStrut(4));
        header.add(dong2);
        header.add(Box.createVerticalStrut(6));
        header.add(lblTrangThai);
        card.add(header, BorderLayout.NORTH);

        JPanel giua = new JPanel(new BorderLayout(0, 8));
        giua.setOpaque(false);
        tblDichVu.setRowHeight(34);
        tblDichVu.setFont(font(11));
        tblDichVu.getTableHeader().setFont(bold(9));
        tblDichVu.getTableHeader().setBackground(new Color(0xF7F9FB));
        tblDichVu.getTableHeader().setForeground(MUTED);
        tblDichVu.setSelectionBackground(new Color(0xEFF6F5));
        tblDichVu.setSelectionForeground(TEXT);
        tblDichVu.setShowVerticalLines(false);
        tblDichVu.setGridColor(new Color(0xEDF1F5));
        JScrollPane scr = new JScrollPane(tblDichVu);
        scr.setBorder(BorderFactory.createLineBorder(BORDER));
        scr.getViewport().setBackground(Color.WHITE);
        giua.add(scr, BorderLayout.CENTER);
        JPanel tong = new JPanel(new BorderLayout());
        tong.setOpaque(false);
        JLabel nhan = new JLabel("Tổng cộng");
        nhan.setFont(font(10));
        nhan.setForeground(MUTED);
        lblTong.setFont(bold(17));
        lblTong.setForeground(TEAL);
        tong.add(nhan, BorderLayout.WEST);
        tong.add(lblTong, BorderLayout.EAST);
        giua.add(tong, BorderLayout.SOUTH);
        card.add(giua, BorderLayout.CENTER);

        lblLoiDan.setFont(font(11));
        lblLoiDan.setForeground(MUTED);
        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        footer.add(lblLoiDan, BorderLayout.CENTER);
        btnThanhToan.setFont(bold(12));
        btnThanhToan.setBackground(TEAL);
        btnThanhToan.setForeground(Color.WHITE);
        btnThanhToan.setPreferredSize(new Dimension(116, 34));
        btnThanhToan.setVisible(!xemDayDu);
        btnThanhToan.setEnabled(false);
        btnTaiKham.setFont(bold(12));
        btnTaiKham.setPreferredSize(new Dimension(116, 34));
        btnTaiKham.setVisible(!xemDayDu);
        btnTaiKham.setEnabled(false);
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        nut.setOpaque(false);
        nut.add(btnTaiKham);
        nut.add(btnThanhToan);
        footer.add(nut, BorderLayout.EAST);
        card.add(footer, BorderLayout.SOUTH);
        lblKhongCo.setFont(font(12));
        lblKhongCo.setForeground(MUTED);
        return card;
    }

    private void addEvents() {
        btnTim.addActionListener(e -> taiDanhSach(txtTimKiem.getText()));
        cboTrangThai.addActionListener(e -> taiDanhSach(txtTimKiem.getText()));
        txtTimKiem.addActionListener(e -> taiDanhSach(txtTimKiem.getText()));
        tblPhieu.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                hienChiTiet(phieuDangChon());
            }
        });
        btnThanhToan.addActionListener(e -> moThanhToan());
        btnTaiKham.addActionListener(e -> moDatTaiKham());
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "lamMoi");
        getActionMap().put("lamMoi", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                taiDanhSach(txtTimKiem.getText());
            }
        });
    }

    private void taiDanhSach(String tuKhoa) {
        new SwingWorker<List<PhieuKham>, Void>() {
            @Override
            protected List<PhieuKham> doInBackground() {
                return phieuKhamDAO.timKiem(tuKhoa);
            }

            @Override
            protected void done() {
                try {
                    List<PhieuKham> ds = new ArrayList<>(get());
                    ds.sort(Comparator.comparingInt((PhieuKham p) -> switch (p.getTrangThai()) {
                        case CHO_KHAM -> 0;
                        case DANG_KHAM -> 1;
                        default -> 2;
                    }).thenComparing(PhieuKham::getNgayLap, Comparator.reverseOrder()));
                    String locTrangThai = (String) cboTrangThai.getSelectedItem();
                    ds.removeIf(pk -> !khopTrangThai(pk, locTrangThai));
                    dsPhieu.clear();
                    dsPhieu.addAll(ds);
                    modelPhieu.setRowCount(0);
                    for (PhieuKham pk : dsPhieu) {
                        modelPhieu.addRow(new Object[]{
                            pk.getMaPK(),
                            pk.getNgayLap().format(NGAY_GIO),
                            pk.getLichHen() == null ? "—" : pk.getLichHen().getKhachHang().getTenKH(),
                            pk.getLichHen() == null ? "—" : pk.getLichHen().getNhaSi().getHoTenNV(),
                            nhanTrangThai(pk.getTrangThai())});
                    }
                    if (dsPhieu.isEmpty()) {
                        hienChiTiet(null);
                        lblKhongCo.setText("Không tìm thấy phiếu khám phù hợp");
                    } else {
                        tblPhieu.setRowSelectionInterval(0, 0);
                    }
                } catch (CancellationException ignored) {
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    baoLoi(e.getCause());
                }
            }
        }.execute();
    }

    private static boolean khopTrangThai(PhieuKham pk, String loc) {
        if (loc == null || loc.startsWith("Tất cả")) {
            return true;
        }
        return switch (loc) {
            case "Chờ khám" -> pk.getTrangThai() == TrangThaiPhieuKham.CHO_KHAM;
            case "Đang khám" -> pk.getTrangThai() == TrangThaiPhieuKham.DANG_KHAM;
            case "Hoàn tất" -> pk.getTrangThai() == TrangThaiPhieuKham.HOAN_TAT;
            default -> true;
        };
    }

    private PhieuKham phieuDangChon() {
        int row = tblPhieu.getSelectedRow();
        if (row < 0 || row >= dsPhieu.size()) {
            return null;
        }
        return dsPhieu.get(row);
    }

    private void hienChiTiet(PhieuKham pk) {
        if (pk == null) {
            lblTen.setText("Chưa chọn phiếu khám");
            lblMa.setText(" ");
            lblNgay.setText(" ");
            lblNhaSi.setText(" ");
            lblLoiDan.setText(" ");
            lblTong.setText("0 đ");
            modelDichVu.setRowCount(0);
            btnThanhToan.setEnabled(false);
            btnTaiKham.setEnabled(false);
            return;
        }
        lblTen.setText(pk.getLichHen() == null ? "—" : pk.getLichHen().getKhachHang().getTenKH());
        lblMa.setText(pk.getMaPK());
        lblNgay.setText("Lập: " + pk.getNgayLap().format(NGAY_GIO));
        lblNhaSi.setText("Nha sĩ: " + (pk.getLichHen() == null ? "—" : pk.getLichHen().getNhaSi().getHoTenNV()));
        lblTrangThai.set(nhanTrangThai(pk.getTrangThai()), TEAL, new Color(0xE7F4F3));
        lblLoiDan.setText(xemDayDu && pk.getLoiDan() != null ? "Lời dặn: " + pk.getLoiDan() : " ");
        taiDongDichVu(pk);
        capNhatNutThanhToan(pk);
        btnTaiKham.setEnabled(pk.getTrangThai() == TrangThaiPhieuKham.HOAN_TAT && pk.getLichHen() != null);
    }

    /** UC017: lễ tân đặt lịch tái khám từ phiếu đã hoàn tất. */
    private void moDatTaiKham() {
        PhieuKham pk = phieuDangChon();
        if (pk == null || pk.getTrangThai() != TrangThaiPhieuKham.HOAN_TAT) {
            return;
        }
        DlgTaiKham.datTaiKham(javax.swing.SwingUtilities.getWindowAncestor(this), pk);
    }

    private void capNhatNutThanhToan(PhieuKham pk) {
        if (xemDayDu || pk == null || pk.getTrangThai() != TrangThaiPhieuKham.HOAN_TAT) {
            btnThanhToan.setEnabled(false);
            btnThanhToan.setToolTipText(null);
            return;
        }
        btnThanhToan.setEnabled(false);
        btnThanhToan.setToolTipText("Đang kiểm tra hóa đơn...");
        String maPK = pk.getMaPK();
        new SwingWorker<HoaDon, Void>() {
            @Override
            protected HoaDon doInBackground() {
                return hoaDonDAO.layTheoPhieuKham(maPK);
            }

            @Override
            protected void done() {
                try {
                    HoaDon hd = get();
                    boolean chuaTra = hd != null && !hd.isTrangThai();
                    btnThanhToan.setEnabled(chuaTra);
                    btnThanhToan.setToolTipText(hd == null ? "Phiếu khám chưa có hóa đơn."
                            : chuaTra ? "Thanh toán hóa đơn " + hd.getMaHD()
                            : "Hóa đơn đã được thanh toán.");
                } catch (Exception e) {
                    btnThanhToan.setEnabled(false);
                    btnThanhToan.setToolTipText(null);
                }
            }
        }.execute();
    }

    private void moThanhToan() {
        PhieuKham pk = phieuDangChon();
        if (pk == null || pk.getTrangThai() != TrangThaiPhieuKham.HOAN_TAT) {
            return;
        }
        btnThanhToan.setEnabled(false);
        new SwingWorker<HoaDon, Void>() {
            @Override
            protected HoaDon doInBackground() {
                return hoaDonDAO.layTheoPhieuKham(pk.getMaPK());
            }

            @Override
            protected void done() {
                try {
                    HoaDon hd = get();
                    if (hd == null) {
                        JOptionPane.showMessageDialog(PnlTraCuuPhieu.this,
                                "Phiếu khám chưa có hóa đơn.", "Thông báo",
                                JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    DlgThanhToan.hien(javax.swing.SwingUtilities.getWindowAncestor(PnlTraCuuPhieu.this),
                            hd, () -> taiDanhSach(txtTimKiem.getText()));
                } catch (Exception e) {
                    baoLoi(e.getCause() == null ? e : e.getCause());
                } finally {
                    capNhatNutThanhToan(pk);
                }
            }
        }.execute();
    }

    private void taiDongDichVu(PhieuKham pk) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                if (xemDayDu) {
                    List<ChiTietPhieuKham> ds = chiTietDAO.layTheoPhieuKham(pk.getMaPK());
                    javax.swing.SwingUtilities.invokeLater(() -> hienDongDayDu(ds));
                } else {
                    List<ChiTietPhieuKhamDAO.DongThanhToan> ds = chiTietDAO.layDongThanhToan(pk.getMaPK());
                    javax.swing.SwingUtilities.invokeLater(() -> hienDongThanhToan(ds));
                }
                return null;
            }
        }.execute();
    }

    private void hienDongDayDu(List<ChiTietPhieuKham> ds) {
        modelDichVu.setColumnIdentifiers(new String[]{"Triệu chứng/Chẩn đoán", "Dịch vụ", "SL", "Đơn giá", "Thành tiền"});
        modelDichVu.setRowCount(0);
        double tong = 0;
        for (ChiTietPhieuKham ct : ds) {
            double thanhTien = ct.getDonGia() * ct.getSoLuong();
            tong += thanhTien;
            modelDichVu.addRow(new Object[]{ct.getTrieuChung() + " / " + ct.getChanDoan(),
                ct.getDichVu().getTenDV(), ct.getSoLuong(), money(ct.getDonGia()), money(thanhTien)});
        }
        lblTong.setText(money(tong));
    }

    private void hienDongThanhToan(List<ChiTietPhieuKhamDAO.DongThanhToan> ds) {
        modelDichVu.setColumnIdentifiers(new String[]{"Dịch vụ", "SL", "Đơn giá", "Thuế", "Thành tiền"});
        modelDichVu.setRowCount(0);
        double tong = 0;
        for (ChiTietPhieuKhamDAO.DongThanhToan d : ds) {
            double thanhTien = d.donGia().doubleValue() * d.soLuong();
            tong += thanhTien;
            modelDichVu.addRow(new Object[]{d.tenDV(), d.soLuong(), money(d.donGia().doubleValue()),
                money(d.tienThue().doubleValue()), money(thanhTien)});
        }
        lblTong.setText(money(tong));
    }

    private static String nhanTrangThai(TrangThaiPhieuKham tt) {
        return switch (tt) {
            case CHO_KHAM -> "Chờ khám";
            case DANG_KHAM -> "Đang khám";
            case HOAN_TAT -> "Hoàn tất";
        };
    }

    private static String money(double v) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.of("vi", "VN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(v) + " đ";
    }

    private void baoLoi(Throwable loi) {
        if (loi instanceof BusinessException) {
            JOptionPane.showMessageDialog(this, loi.getMessage(), "Cảnh báo", JOptionPane.WARNING_MESSAGE);
        } else if (loi instanceof DataAccessException) {
            JOptionPane.showMessageDialog(this, "Không thể tải phiếu khám.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Đã xảy ra lỗi, vui lòng thử lại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static final class PillLabel extends JLabel {

        PillLabel() {
            super("", SwingConstants.CENTER);
            setOpaque(false);
            setBorder(new EmptyBorder(4, 10, 4, 10));
        }

        void set(String text, Color fg, Color bg) {
            setText(text);
            setForeground(fg);
            putClientProperty("bg", bg);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Object bg = getClientProperty("bg");
            if (bg instanceof Color c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
