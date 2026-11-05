package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO;
import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.DoanhThuTrongNgay;
import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.HoaDonTrongNgay;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import com.toedter.calendar.JDateChooser;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Trang "Doanh thu trong ngày" của Lễ tân (mục Báo cáo trên menu Lễ tân), bố cục theo kiểu trang Báo cáo.
 *
 * <p>Mặc định xem hôm nay; chọn ngày khác trên lịch thì tính lại. Theo báo cáo, trang tính MỌI hóa đơn lập
 * trong ngày, kể cả chưa thanh toán ("doanh thu tạm tính"), để Lễ tân nắm tình hình vận hành:</p>
 * <ul>
 *   <li>Hàng thẻ: Doanh thu tạm tính (kèm số hóa đơn, số chưa thu), Đã thu, Tiền mặt, Chuyển khoản.
 *       Tiền mặt dùng để đối chiếu với tiền đếm tại quầy cuối ngày.</li>
 *   <li>Bảng "Hóa đơn trong ngày": từng hóa đơn, mới nhất ở trên.</li>
 *   <li>Không có hóa đơn: các số = 0 và báo "Chưa có hóa đơn trong ngày này".</li>
 * </ul>
 * Chỉ đọc, không thay đổi dữ liệu. Tiền một hóa đơn = tiền hàng + thuế − giảm voucher
 * (xem {@link ThongKeDoanhThuDAO#doanhThuTrongNgay}).
 */
public class PnlDoanhThuNgay extends JPanel {

    static final int COT_MA = 0, COT_GIO = 1, COT_KHACH = 2, COT_TRANG_THAI = 3, COT_HINH_THUC = 4, COT_TIEN = 5;
    static final String DA_THANH_TOAN = "Đã thanh toán";
    static final String CHO_THANH_TOAN = "Chờ thanh toán";
    static final String KHONG_CO_HOA_DON = "Chưa có hóa đơn trong ngày này";

    private static final Logger LOG = Logger.getLogger(PnlDoanhThuNgay.class.getName());
    private static final Color DO = new Color(0xC94C48);
    private static final Color VANG = new Color(0x9A6A12);
    private static final Color NEN_HEADER = new Color(0xF7F9FB);
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter GIO_NGAY = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy");

    private final Function<LocalDate, DoanhThuTrongNgay> nguon;
    private final Supplier<LocalDateTime> bayGio;

    final JLabel lblPhuDe = new JLabel(" ");
    final JDateChooser dcNgay = new JDateChooser();
    final JButton btnHomNay = new JButton("Hôm nay");
    final JLabel lblTamTinh = new JLabel("—");
    final JLabel lblGhiChuTamTinh = new JLabel(" ");
    final JLabel lblDaThu = new JLabel("—");
    final JLabel lblTienMat = new JLabel("—");
    final JLabel lblChuyenKhoan = new JLabel("—");
    final JLabel lblThongBao = new JLabel(" ");
    final DefaultTableModel model = new DefaultTableModel(new String[]{
        "MÃ HĐ", "GIỜ LẬP", "KHÁCH HÀNG", "TRẠNG THÁI", "HÌNH THỨC", "TỔNG TIỀN"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    final JTable tblHoaDon = new JTable(model) {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getRowCount() == 0) {
                veThongBaoGiuaBang(this, g); // bảng trống: thông báo nằm ngay trong bảng
            }
        }
    };

    /** true khi code đang tự đặt ngày, để sự kiện đổi ngày không tải lại 2 lần. */
    private boolean dangDatNgay;
    private SwingWorker<DoanhThuTrongNgay, Void> worker;

    public PnlDoanhThuNgay() {
        this(new ThongKeDoanhThuDAO()::doanhThuTrongNgay, LocalDateTime::now);
    }

    PnlDoanhThuNgay(Function<LocalDate, DoanhThuTrongNgay> nguon, Supplier<LocalDateTime> bayGio) {
        super(new BorderLayout(0, 16));
        this.nguon = Objects.requireNonNull(nguon);
        this.bayGio = Objects.requireNonNull(bayGio);
        setName("Báo cáo");
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 24, 24));
        JPanel tren = new JPanel(new BorderLayout(0, 16));
        tren.setOpaque(false);
        tren.add(taoTieuDe(), BorderLayout.NORTH);
        tren.add(taoHangChonNgay(), BorderLayout.CENTER);
        tren.add(taoHangThe(), BorderLayout.SOUTH);
        add(tren, BorderLayout.NORTH);
        add(taoTheBang(), BorderLayout.CENTER);
        datNgay(bayGio.get().toLocalDate());
    }

    // ===================== Dựng giao diện =====================

    private JPanel taoTieuDe() {
        JPanel chu = new JPanel();
        chu.setOpaque(false);
        chu.setLayout(new BoxLayout(chu, BoxLayout.Y_AXIS));
        chu.add(nhan("Doanh thu trong ngày", bold(24), TEXT));
        chu.add(Box.createVerticalStrut(4));
        lblPhuDe.setName("phuDeDoanhThuNgay");
        lblPhuDe.setFont(font(13));
        lblPhuDe.setForeground(MUTED);
        chu.add(lblPhuDe);
        return chu;
    }

    private JPanel taoHangChonNgay() {
        JPanel hang = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        hang.setOpaque(false);
        hang.add(nhan("Ngày xem", font(13), MUTED));
        hang.add(new JLabel(new LineIcon(LineIcon.Type.CALENDAR, TEXT, 16)));
        dcNgay.setName("ngayXemDoanhThu");
        dcNgay.setLocale(Locale.forLanguageTag("vi-VN"));
        dcNgay.setDateFormatString("dd/MM/yyyy");
        dcNgay.setPreferredSize(new Dimension(140, 38));
        dcNgay.setFont(font(13));
        dcNgay.addPropertyChangeListener("date", e -> {
            if (!dangDatNgay) {
                taiDuLieu(); // Lễ tân tự chọn ngày khác trên lịch
            }
        });
        hang.add(dcNgay);
        btnHomNay.setName("xemHomNay");
        btnHomNay.setFont(bold(13));
        btnHomNay.setFocusPainted(false);
        btnHomNay.setPreferredSize(new Dimension(100, 38));
        btnHomNay.addActionListener(e -> datNgay(bayGio.get().toLocalDate()));
        hang.add(Box.createHorizontalStrut(4));
        hang.add(btnHomNay);
        return hang;
    }

    /** 4 thẻ số liệu chia đều một hàng. */
    private JPanel taoHangThe() {
        JPanel hang = new JPanel(new GridLayout(1, 4, 16, 0));
        hang.setOpaque(false);
        lblGhiChuTamTinh.setName("ghiChuTamTinh");
        hang.add(theSo("Doanh thu tạm tính", LineIcon.Type.CHART, lblTamTinh, lblGhiChuTamTinh));
        hang.add(theSo("Đã thu", LineIcon.Type.CHECK, lblDaThu, nhan("Hóa đơn đã thanh toán", font(11), MUTED)));
        hang.add(theSo("Tiền mặt", LineIcon.Type.INVOICE, lblTienMat,
                nhan("Đối chiếu tiền tại quầy cuối ngày", font(11), MUTED)));
        hang.add(theSo("Chuyển khoản", LineIcon.Type.INVOICE, lblChuyenKhoan,
                nhan("Đã thu qua chuyển khoản", font(11), MUTED)));
        return hang;
    }

    private static JPanel theSo(String tieuDe, LineIcon.Type bieuTuong, JLabel giaTri, JLabel ghiChu) {
        SurfacePanel the = new SurfacePanel(new BorderLayout(0, 8));
        the.setBorder(new EmptyBorder(14, 16, 14, 16));
        JPanel dong = new JPanel(new BorderLayout());
        dong.setOpaque(false);
        dong.add(nhan(tieuDe, font(12), MUTED), BorderLayout.WEST);
        dong.add(new JLabel(new LineIcon(bieuTuong, TEAL, 18)), BorderLayout.EAST);
        the.add(dong, BorderLayout.NORTH);
        giaTri.setFont(bold(24));
        giaTri.setForeground(TEXT);
        the.add(giaTri, BorderLayout.CENTER);
        ghiChu.setFont(font(11));
        ghiChu.setForeground(MUTED);
        the.add(ghiChu, BorderLayout.SOUTH);
        return the;
    }

    private JPanel taoTheBang() {
        SurfacePanel the = new SurfacePanel(new BorderLayout(0, 12));
        the.setBorder(new EmptyBorder(16, 18, 16, 18));
        the.add(nhan("Hóa đơn trong ngày", bold(16), TEXT), BorderLayout.NORTH);

        tblHoaDon.setName("bangHoaDonTrongNgay");
        tblHoaDon.setFont(font(13));
        tblHoaDon.setRowHeight(36);
        tblHoaDon.setShowVerticalLines(false);
        tblHoaDon.setGridColor(BORDER);
        tblHoaDon.setFillsViewportHeight(true);
        tblHoaDon.setFocusable(false);
        tblHoaDon.setRowSelectionAllowed(false);
        tblHoaDon.getTableHeader().setReorderingAllowed(false);
        tblHoaDon.getTableHeader().setPreferredSize(new Dimension(0, 36));
        tblHoaDon.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                setFont(bold(11));
                setForeground(MUTED);
                setBackground(NEN_HEADER);
                setOpaque(true);
                setHorizontalAlignment(c == COT_TIEN ? RIGHT : LEFT);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(0, 12, 0, 12)));
                return this;
            }
        });
        tblHoaDon.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                setHorizontalAlignment(c == COT_TIEN ? RIGHT : LEFT);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                setFont(c == COT_TIEN ? bold(13) : font(13));
                setForeground(c != COT_TRANG_THAI ? TEXT : DA_THANH_TOAN.equals(v) ? GREEN : VANG);
                setToolTipText(v == null ? null : v.toString());
                return this;
            }
        });
        int[] rong = {120, 80, 240, 130, 120, 130};
        for (int i = 0; i < rong.length; i++) {
            tblHoaDon.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        }
        JScrollPane cuon = new JScrollPane(tblHoaDon);
        cuon.setBorder(BorderFactory.createLineBorder(BORDER));
        cuon.getViewport().setBackground(SURFACE);
        cuon.getVerticalScrollBar().setUnitIncrement(16);
        the.add(cuon, BorderLayout.CENTER);
        lblThongBao.setName("thongBaoDoanhThuNgay");
        lblThongBao.setFont(font(12));
        lblThongBao.setForeground(MUTED);
        the.add(lblThongBao, BorderLayout.SOUTH);
        return the;
    }

    // ===================== Tải dữ liệu =====================

    /** Mục Báo cáo trên menu Lễ tân gọi mỗi lần mở trang: tải lại ngày đang xem. */
    public void lamMoi() {
        taiDuLieu();
    }

    /** Đặt ngày lên lịch (không phát sự kiện tự chọn) rồi tải. */
    private void datNgay(LocalDate ngay) {
        dangDatNgay = true;
        try {
            dcNgay.setDate(Date.from(ngay.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        } finally {
            dangDatNgay = false;
        }
        taiDuLieu();
    }

    private void taiDuLieu() {
        Date chon = dcNgay.getDate();
        lblPhuDe.setText("Gồm cả hóa đơn chưa thanh toán (doanh thu tạm tính) · Dữ liệu đến "
                + bayGio.get().format(GIO_NGAY));
        if (worker != null) {
            worker.cancel(true); // bỏ kết quả lần trước nếu chưa xong
        }
        if (chon == null) {
            hienLoi("Vui lòng chọn ngày xem.");
            return;
        }
        LocalDate ngay = chon.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        baoTinhTrang("Đang tải doanh thu...", MUTED);
        worker = new SwingWorker<>() {
            @Override
            protected DoanhThuTrongNgay doInBackground() {
                return nguon.apply(ngay);
            }

            @Override
            protected void done() {
                if (this != worker || isCancelled()) {
                    return;
                }
                worker = null;
                try {
                    hienThi(get());
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof BusinessException) {
                        hienLoi(cause.getMessage());
                    } else {
                        LOG.log(Level.WARNING, "Không thể tải doanh thu trong ngày", cause);
                        hienLoi("Không thể tải doanh thu. Vui lòng thử lại.");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        worker.execute();
    }

    private void hienThi(DoanhThuTrongNgay d) {
        model.setRowCount(0);
        for (HoaDonTrongNgay h : d.hoaDon()) {
            model.addRow(new Object[]{h.maHD(), h.ngayLap() == null ? "" : h.ngayLap().format(GIO), h.tenKH(),
                h.daThanhToan() ? DA_THANH_TOAN : CHO_THANH_TOAN, h.daThanhToan() ? h.hinhThuc() : "—",
                PnlThongKeDichVu.tien(h.tongTien())});
        }
        lblTamTinh.setText(PnlThongKeDichVu.tien(d.tamTinh()));
        lblGhiChuTamTinh.setText(d.soHoaDon() + " hóa đơn · Chưa thu: " + PnlThongKeDichVu.tien(d.chuaThu()));
        lblDaThu.setText(PnlThongKeDichVu.tien(d.daThu()));
        lblTienMat.setText(PnlThongKeDichVu.tien(d.tienMat()));
        lblChuyenKhoan.setText(PnlThongKeDichVu.tien(d.chuyenKhoan()));
        baoTinhTrang(d.soHoaDon() == 0 ? KHONG_CO_HOA_DON : d.soHoaDon() + " hóa đơn trong ngày", MUTED);
    }

    private void hienLoi(String thongBao) {
        model.setRowCount(0);
        for (JLabel l : new JLabel[]{lblTamTinh, lblDaThu, lblTienMat, lblChuyenKhoan}) {
            l.setText("—");
        }
        lblGhiChuTamTinh.setText(" ");
        baoTinhTrang(thongBao, DO);
    }

    /** Bảng trống thì thông báo vẽ giữa bảng; có dòng thì hiện dưới bảng. */
    private void baoTinhTrang(String text, Color mau) {
        lblThongBao.setText(text);
        lblThongBao.setForeground(mau);
        lblThongBao.setVisible(model.getRowCount() > 0);
        tblHoaDon.repaint();
    }

    private void veThongBaoGiuaBang(JTable bang, Graphics g) {
        String text = lblThongBao.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(font(13));
        g2.setColor(lblThongBao.getForeground());
        FontMetrics fm = g2.getFontMetrics();
        int y = Math.min((bang.getHeight() - fm.getHeight()) / 2, 56) + fm.getAscent();
        g2.drawString(text, Math.max(8, (bang.getWidth() - fm.stringWidth(text)) / 2), y);
        g2.dispose();
    }

    @Override
    public void removeNotify() {
        if (worker != null) {
            worker.cancel(true);
            worker = null;
        }
        super.removeNotify();
    }

    private static JLabel nhan(String text, Font f, Color mau) {
        JLabel l = new JLabel(text);
        l.setFont(f);
        l.setForeground(mau);
        return l;
    }
}
