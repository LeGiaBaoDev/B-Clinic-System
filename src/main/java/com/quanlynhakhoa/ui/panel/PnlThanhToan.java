package com.quanlynhakhoa.ui.panel;

import static com.quanlynhakhoa.ui.component.UiConstants.BACKGROUND;
import static com.quanlynhakhoa.ui.component.UiConstants.BORDER;
import static com.quanlynhakhoa.ui.component.UiConstants.GREEN;
import static com.quanlynhakhoa.ui.component.UiConstants.MUTED;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL;
import static com.quanlynhakhoa.ui.component.UiConstants.TEXT;
import static com.quanlynhakhoa.ui.component.UiConstants.bold;
import static com.quanlynhakhoa.ui.component.UiConstants.font;

import com.quanlynhakhoa.dao.HoaDonDAO;
import com.quanlynhakhoa.entity.HoaDon;
import com.quanlynhakhoa.entity.KhachHang;
import com.quanlynhakhoa.entity.LichHen;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import com.quanlynhakhoa.ui.dialog.DlgThanhToan;
import com.quanlynhakhoa.ui.dialog.HopThoaiLoi;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class PnlThanhToan extends JPanel {

    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] COT = {"MÃ HÓA ĐƠN", "BỆNH NHÂN", "SỐ ĐIỆN THOẠI",
        "NGÀY LẬP", "TỔNG TIỀN", "TRẠNG THÁI"};

    private record DongHoaDon(HoaDon hoaDon, BigDecimal tong) {
    }

    private final HoaDonDAO hoaDonDAO = new HoaDonDAO();
    private final List<HoaDon> dsHoaDon = new ArrayList<>();
    private final List<BigDecimal> dsTong = new ArrayList<>();
    private final JTextField txtTimKiem = new JTextField();
    private final JButton btnTim = new JButton("Tìm kiếm");
    private final JButton btnLamMoi = new JButton("Làm mới");
    private final JButton btnThanhToan = new JButton("Thanh toán");
    private final DefaultTableModel model = new DefaultTableModel(COT, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel lblTong = new JLabel(" ");
    private final JLabel lblChon = new JLabel("Chọn một hóa đơn để thanh toán");

    public PnlThanhToan() {
        super(new BorderLayout(0, 14));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 20, 24));
        initComponents();
        addEvents();
        lamMoi();
    }

    public void lamMoi() {
        taiDuLieu();
    }

    private void initComponents() {
        JPanel dau = new JPanel(new BorderLayout(0, 12));
        dau.setOpaque(false);
        JPanel title = new JPanel();
        title.setOpaque(false);
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        JLabel tieuDe = new JLabel("Hóa đơn chờ thanh toán");
        tieuDe.setFont(bold(21));
        tieuDe.setForeground(TEXT);
        JLabel phuDe = new JLabel("Chọn hóa đơn do bác sĩ vừa hoàn tất để thu tiền và in phiếu thu.");
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
                "Tìm theo mã hóa đơn, tên bệnh nhân hoặc số điện thoại...");
        tim.add(txtTimKiem, BorderLayout.CENTER);
        JPanel nutTim = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        nutTim.setOpaque(false);
        btnTim.setFont(bold(12));
        btnTim.setBackground(TEAL);
        btnTim.setForeground(Color.WHITE);
        btnLamMoi.setFont(font(12));
        nutTim.add(btnLamMoi);
        nutTim.add(btnTim);
        tim.add(nutTim, BorderLayout.EAST);
        dau.add(tim, BorderLayout.CENTER);
        add(dau, BorderLayout.NORTH);

        cauHinhBang();
        JScrollPane scr = new JScrollPane(table);
        scr.setBorder(BorderFactory.createLineBorder(BORDER));
        scr.getViewport().setBackground(Color.WHITE);

        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setOpaque(false);
        JLabel tieuDeBang = new JLabel("Danh sách hóa đơn");
        tieuDeBang.setFont(bold(13));
        tieuDeBang.setForeground(TEXT);
        lblTong.setFont(font(11));
        lblTong.setForeground(MUTED);
        cardHeader.add(tieuDeBang, BorderLayout.WEST);
        cardHeader.add(lblTong, BorderLayout.EAST);
        card.add(cardHeader, BorderLayout.NORTH);
        card.add(scr, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
        add(taoChan(), BorderLayout.SOUTH);
    }

    private void cauHinhBang() {
        table.setRowHeight(44);
        table.setFont(font(12));
        table.getTableHeader().setFont(bold(10));
        table.getTableHeader().setBackground(new Color(0xF7F9FB));
        table.getTableHeader().setForeground(MUTED);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(0xEDF1F5));
        table.setSelectionBackground(new Color(0xEFF6F5));
        table.setSelectionForeground(TEXT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] rong = {120, 190, 120, 140, 120, 110};
        for (int i = 0; i < rong.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        }
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(center);
        table.getColumnModel().getColumn(5).setCellRenderer(center);
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(4).setCellRenderer(right);
    }

    private JPanel taoChan() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        lblChon.setFont(font(11));
        lblChon.setForeground(MUTED);
        footer.add(lblChon, BorderLayout.WEST);
        btnThanhToan.setFont(bold(13));
        btnThanhToan.setBackground(TEAL);
        btnThanhToan.setForeground(Color.WHITE);
        btnThanhToan.setPreferredSize(new Dimension(150, 38));
        btnThanhToan.setIcon(new LineIcon(LineIcon.Type.INVOICE, Color.WHITE, 16));
        btnThanhToan.setIconTextGap(7);
        btnThanhToan.setEnabled(false);
        JPanel phai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        phai.setOpaque(false);
        phai.add(btnThanhToan);
        footer.add(phai, BorderLayout.EAST);
        return footer;
    }

    private void addEvents() {
        btnTim.addActionListener(e -> taiDuLieu());
        txtTimKiem.addActionListener(e -> taiDuLieu());
        btnLamMoi.addActionListener(e -> {
            txtTimKiem.setText("");
            taiDuLieu();
        });
        btnThanhToan.addActionListener(e -> moThanhToan());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                capNhatNut();
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    moThanhToan();
                }
            }
        });
    }

    private void taiDuLieu() {
        btnThanhToan.setEnabled(false);
        lblTong.setText("Đang tải...");
        String tuKhoa = txtTimKiem.getText() == null ? "" : txtTimKiem.getText().trim();
        new SwingWorker<List<DongHoaDon>, Void>() {
            @Override
            protected List<DongHoaDon> doInBackground() {
                List<DongHoaDon> ketQua = new ArrayList<>();
                for (HoaDon hd : hoaDonDAO.timKiem(LocalDate.of(2000, 1, 1),
                        LocalDate.of(2100, 1, 1), false)) {
                    if (khop(hd, tuKhoa)) {
                        ketQua.add(new DongHoaDon(hd, hoaDonDAO.tinhTongTien(hd.getMaHD())));
                    }
                }
                return ketQua;
            }

            @Override
            protected void done() {
                try {
                    List<DongHoaDon> tatCa = get();
                    dsHoaDon.clear();
                    dsTong.clear();
                    model.setRowCount(0);
                    for (DongHoaDon dong : tatCa) {
                        HoaDon hd = dong.hoaDon();
                        BigDecimal tong = dong.tong();
                        dsHoaDon.add(hd);
                        dsTong.add(tong);
                        LichHen lh = hd.getPhieuKham().getLichHen();
                        KhachHang kh = lh == null ? null : lh.getKhachHang();
                        model.addRow(new Object[]{
                            hd.getMaHD(),
                            kh == null ? "—" : kh.getTenKH(),
                            kh == null ? "—" : kh.getSdt(),
                            hd.getNgayLap().format(NGAY_GIO),
                            money(tong),
                            "Chờ thanh toán"});
                    }
                    lblTong.setText(dsHoaDon.size() + " hóa đơn");
                    if (!dsHoaDon.isEmpty()) {
                        table.setRowSelectionInterval(0, 0);
                    } else {
                        capNhatNut();
                    }
                } catch (CancellationException ignored) {
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    HopThoaiLoi.hien(PnlThanhToan.this, e, "Không thể tải danh sách hóa đơn.");
                }
            }
        }.execute();
    }

    private boolean khop(HoaDon hd, String tuKhoa) {
        if (tuKhoa.isBlank()) {
            return true;
        }
        String k = tuKhoa.toLowerCase(Locale.ROOT);
        LichHen lh = hd.getPhieuKham().getLichHen();
        KhachHang kh = lh == null ? null : lh.getKhachHang();
        String ten = kh == null || kh.getTenKH() == null ? "" : kh.getTenKH();
        String sdt = kh == null || kh.getSdt() == null ? "" : kh.getSdt();
        return hd.getMaHD().toLowerCase(Locale.ROOT).contains(k)
                || ten.toLowerCase(Locale.ROOT).contains(k)
                || sdt.toLowerCase(Locale.ROOT).contains(k);
    }

    private void capNhatNut() {
        int row = table.getSelectedRow();
        boolean chon = row >= 0 && row < dsHoaDon.size();
        btnThanhToan.setEnabled(chon);
        if (chon) {
            HoaDon hd = dsHoaDon.get(row);
            LichHen lh = hd.getPhieuKham().getLichHen();
            KhachHang kh = lh == null ? null : lh.getKhachHang();
            lblChon.setText("Đã chọn " + hd.getMaHD() + " · "
                    + (kh == null ? "—" : kh.getTenKH()) + " · " + money(dsTong.get(row)));
        } else {
            lblChon.setText("Chọn một hóa đơn để thanh toán");
        }
    }

    private void moThanhToan() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= dsHoaDon.size()) {
            return;
        }
        HoaDon hd = dsHoaDon.get(row);
        DlgThanhToan.hien(javax.swing.SwingUtilities.getWindowAncestor(this), hd, this::taiDuLieu);
    }

    private static String money(BigDecimal v) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.of("vi", "VN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(v == null ? BigDecimal.ZERO : v) + " đ";
    }
}
