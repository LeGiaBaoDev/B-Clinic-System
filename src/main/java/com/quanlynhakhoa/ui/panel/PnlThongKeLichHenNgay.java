package com.quanlynhakhoa.ui.panel;

import static com.quanlynhakhoa.ui.component.UiConstants.BACKGROUND;
import static com.quanlynhakhoa.ui.component.UiConstants.BORDER;
import static com.quanlynhakhoa.ui.component.UiConstants.SURFACE;
import static com.quanlynhakhoa.ui.component.UiConstants.MUTED;
import static com.quanlynhakhoa.ui.component.UiConstants.TEXT;
import static com.quanlynhakhoa.ui.component.UiConstants.bold;
import static com.quanlynhakhoa.ui.component.UiConstants.font;

import com.quanlynhakhoa.dao.LichHenDAO;
import com.quanlynhakhoa.dao.ThongKeLichHenDAO;
import com.quanlynhakhoa.dao.ThongKeLichHenDAO.TongHop;
import com.quanlynhakhoa.entity.LichHen;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import com.toedter.calendar.JDateChooser;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * UC032 – Lễ tân xem số lượng lịch hẹn của một ngày theo từng trạng thái (đã đặt, đã đến, hoàn tất,
 * đã hủy, không đến).
 *
 * <p>Mặc định hiện hôm nay; chọn ngày khác trên lịch thì tính lại. Ngày không có lịch hẹn nào thì báo
 * "Không có lịch hẹn trong ngày này". Chỉ đọc, không thay đổi dữ liệu.</p>
 */
public class PnlThongKeLichHenNgay extends JPanel {

    static final String KHONG_CO_LICH = "Không có lịch hẹn trong ngày này";
    static final int COT_GIO = 0, COT_MA = 1, COT_KHACH = 2, COT_DICH_VU = 3, COT_NHA_SI = 4, COT_TRANG_THAI = 5;

    private static final Logger LOG = Logger.getLogger(PnlThongKeLichHenNgay.class.getName());
    private static final Color DO = new Color(0xC94C48);
    private static final Color XANH_DUONG = new Color(0x2F6FA8);
    private static final Color VANG = new Color(0x9A6A12);
    private static final Color XANH_LA = new Color(0x2E8B57);
    private static final Color XAM = new Color(0x6B7785);
    private static final Color NEN_HEADER = new Color(0xF7F9FB);
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");
    private static final Locale VI = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter GIO_NGAY = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy");

    /** Kết quả một lần tải: số liệu tổng hợp và danh sách lịch hẹn của ngày. */
    record KetQua(TongHop tongHop, List<LichHen> lich) {

    }

    private final Function<LocalDate, TongHop> nguon;
    private final Function<LocalDate, List<LichHen>> nguonDanhSach;
    private final Supplier<LocalDateTime> bayGio;

    final JLabel lblPhuDe = new JLabel(" ");
    final JDateChooser dcNgay = new JDateChooser();
    final JButton btnHomNay = new JButton("Hôm nay");
    final JLabel lblTong = new JLabel("—");
    final JLabel lblDaDat = new JLabel("—");
    final JLabel lblDaDen = new JLabel("—");
    final JLabel lblHoanTat = new JLabel("—");
    final JLabel lblDaHuy = new JLabel("—");
    final JLabel lblKhongDen = new JLabel("—");
    final JLabel lblTyLeDaDat = new JLabel(" ");
    final JLabel lblTyLeDaDen = new JLabel(" ");
    final JLabel lblTyLeHoanTat = new JLabel(" ");
    final JLabel lblTyLeDaHuy = new JLabel(" ");
    final JLabel lblTyLeKhongDen = new JLabel(" ");
    final JLabel lblThongBao = new JLabel(" ");
    final DefaultTableModel model = new DefaultTableModel(new String[]{
        "GIỜ", "MÃ LỊCH", "KHÁCH HÀNG", "DỊCH VỤ", "NHA SĨ", "TRẠNG THÁI"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    final JTable tblLich = new JTable(model) {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getRowCount() == 0) {
                veThongBaoGiuaBang(this, g); // bảng trống: thông báo nằm ngay trong bảng
            }
        }
    };

    /** true khi code đang tự đặt ngày, để sự kiện đổi ngày không tải lại hai lần. */
    private boolean dangDatNgay;
    private SwingWorker<KetQua, Void> worker;

    public PnlThongKeLichHenNgay() {
        this(new ThongKeLichHenDAO()::theoNgay, ngay -> new LichHenDAO().timKiem(ngay, ngay, null),
                LocalDateTime::now);
    }

    PnlThongKeLichHenNgay(Function<LocalDate, TongHop> nguon, Function<LocalDate, List<LichHen>> nguonDanhSach,
            Supplier<LocalDateTime> bayGio) {
        super(new BorderLayout(0, 16));
        this.nguon = Objects.requireNonNull(nguon);
        this.nguonDanhSach = Objects.requireNonNull(nguonDanhSach);
        this.bayGio = Objects.requireNonNull(bayGio);
        setName("Thống kê lịch hẹn");
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
        chu.add(nhan("Thống kê lịch hẹn theo trạng thái", bold(24), TEXT));
        chu.add(Box.createVerticalStrut(4));
        lblPhuDe.setName("phuDeThongKeLich");
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
        dcNgay.setName("ngayXemThongKeLich");
        dcNgay.setLocale(VI);
        dcNgay.setDateFormatString("dd/MM/yyyy");
        dcNgay.setPreferredSize(new Dimension(140, 38));
        dcNgay.setFont(font(13));
        dcNgay.addPropertyChangeListener("date", e -> {
            if (!dangDatNgay) {
                taiDuLieu(); // lễ tân tự chọn ngày khác trên lịch
            }
        });
        hang.add(dcNgay);
        btnHomNay.setName("xemHomNayThongKeLich");
        btnHomNay.setFont(bold(13));
        btnHomNay.setFocusPainted(false);
        btnHomNay.setPreferredSize(new Dimension(100, 38));
        btnHomNay.addActionListener(e -> datNgay(bayGio.get().toLocalDate()));
        hang.add(Box.createHorizontalStrut(4));
        hang.add(btnHomNay);
        return hang;
    }

    /** 6 thẻ chia đều một hàng: tổng số lịch rồi từng trạng thái. */
    private JPanel taoHangThe() {
        JPanel hang = new JPanel(new GridLayout(1, 6, 16, 0));
        hang.setOpaque(false);
        lblDaDat.setName("soDaDat");
        lblDaDen.setName("soDaDen");
        lblHoanTat.setName("soHoanTat");
        lblDaHuy.setName("soDaHuy");
        lblKhongDen.setName("soKhongDen");
        lblTong.setName("tongLichHen");
        hang.add(theSo("Tổng số lịch", LineIcon.Type.CALENDAR, lblTong, nhan("Cả ngày", font(11), MUTED), TEXT));
        hang.add(theSo("Đã đặt", LineIcon.Type.CLIPBOARD, lblDaDat, lblTyLeDaDat, XANH_DUONG));
        hang.add(theSo("Đã đến", LineIcon.Type.PEOPLE, lblDaDen, lblTyLeDaDen, VANG));
        hang.add(theSo("Hoàn tất", LineIcon.Type.CHECK, lblHoanTat, lblTyLeHoanTat, XANH_LA));
        hang.add(theSo("Đã hủy", LineIcon.Type.CLIPBOARD, lblDaHuy, lblTyLeDaHuy, DO));
        hang.add(theSo("Không đến", LineIcon.Type.CLIPBOARD, lblKhongDen, lblTyLeKhongDen, XAM));
        return hang;
    }

    private static JPanel theSo(String tieuDe, LineIcon.Type bieuTuong, JLabel giaTri, JLabel ghiChu, Color mau) {
        SurfacePanel the = new SurfacePanel(new BorderLayout(0, 8));
        the.setBorder(new EmptyBorder(14, 16, 14, 16));
        JPanel dong = new JPanel(new BorderLayout());
        dong.setOpaque(false);
        dong.add(nhan(tieuDe, font(12), MUTED), BorderLayout.WEST);
        dong.add(new JLabel(new LineIcon(bieuTuong, mau, 18)), BorderLayout.EAST);
        the.add(dong, BorderLayout.NORTH);
        giaTri.setFont(bold(28));
        giaTri.setForeground(mau);
        the.add(giaTri, BorderLayout.CENTER);
        ghiChu.setFont(font(11));
        ghiChu.setForeground(MUTED);
        the.add(ghiChu, BorderLayout.SOUTH);
        return the;
    }

    private JPanel taoTheBang() {
        SurfacePanel the = new SurfacePanel(new BorderLayout(0, 12));
        the.setBorder(new EmptyBorder(16, 18, 16, 18));
        the.add(nhan("Lịch hẹn trong ngày", bold(16), TEXT), BorderLayout.NORTH);

        tblLich.setName("bangLichHenTrongNgay");
        tblLich.setFont(font(13));
        tblLich.setRowHeight(36);
        tblLich.setShowVerticalLines(false);
        tblLich.setGridColor(BORDER);
        tblLich.setFillsViewportHeight(true);
        tblLich.setFocusable(false);
        tblLich.setRowSelectionAllowed(false);
        tblLich.getTableHeader().setReorderingAllowed(false);
        tblLich.getTableHeader().setPreferredSize(new Dimension(0, 36));
        tblLich.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                setFont(bold(11));
                setForeground(MUTED);
                setBackground(NEN_HEADER);
                setOpaque(true);
                setHorizontalAlignment(LEFT);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(0, 12, 0, 12)));
                return this;
            }
        });
        tblLich.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                setHorizontalAlignment(LEFT);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                setFont(c == COT_TRANG_THAI ? bold(13) : font(13));
                setForeground(c == COT_TRANG_THAI ? mauTrangThai(String.valueOf(v)) : TEXT);
                setToolTipText(v == null ? null : v.toString());
                return this;
            }
        });
        int[] rong = {70, 130, 230, 220, 180, 120};
        for (int i = 0; i < rong.length; i++) {
            tblLich.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        }
        JScrollPane cuon = new JScrollPane(tblLich);
        cuon.setBorder(BorderFactory.createLineBorder(BORDER));
        cuon.getViewport().setBackground(SURFACE);
        cuon.getVerticalScrollBar().setUnitIncrement(16);
        the.add(cuon, BorderLayout.CENTER);
        lblThongBao.setName("thongBaoThongKeLich");
        lblThongBao.setFont(font(12));
        lblThongBao.setForeground(MUTED);
        the.add(lblThongBao, BorderLayout.SOUTH);
        return the;
    }

    private static Color mauTrangThai(String nhan) {
        return switch (nhan) {
            case "Đã đặt" -> XANH_DUONG;
            case "Đã đến" -> VANG;
            case "Hoàn tất" -> XANH_LA;
            case "Đã hủy" -> DO;
            default -> XAM;
        };
    }

    // ===================== Tải dữ liệu =====================

    /** Menu Lễ tân gọi mỗi lần mở trang: tải lại ngày đang xem. */
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
        lblPhuDe.setText("Số lịch hẹn theo từng trạng thái trong ngày · Dữ liệu đến "
                + bayGio.get().format(GIO_NGAY));
        if (worker != null) {
            worker.cancel(true); // bỏ kết quả lần trước nếu chưa xong
        }
        if (chon == null) {
            hienLoi("Vui lòng chọn ngày xem.");
            return;
        }
        LocalDate ngay = chon.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        baoTinhTrang("Đang tải thống kê...", MUTED);
        worker = new SwingWorker<>() {
            @Override
            protected KetQua doInBackground() {
                return new KetQua(nguon.apply(ngay), nguonDanhSach.apply(ngay));
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
                        LOG.log(Level.WARNING, "Không thể tải thống kê lịch hẹn", cause);
                        hienLoi("Không thể tải thống kê. Vui lòng thử lại.");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        worker.execute();
    }

    private void hienThi(KetQua kq) {
        TongHop t = kq.tongHop();
        model.setRowCount(0);
        for (LichHen lh : kq.lich()) {
            model.addRow(new Object[]{lh.getNgayGioHen().format(GIO), lh.getMaLH(),
                lh.getKhachHang().getTenKH(), lh.getDichVu().getTenDV(), lh.getNhaSi().getHoTenNV(),
                PnlLichHen.nhanTrangThai(lh.getTrangThai())});
        }
        lblTong.setText(String.valueOf(t.tong()));
        lblDaDat.setText(String.valueOf(t.daDat()));
        lblDaDen.setText(String.valueOf(t.daDen()));
        lblHoanTat.setText(String.valueOf(t.hoanTat()));
        lblDaHuy.setText(String.valueOf(t.daHuy()));
        lblKhongDen.setText(String.valueOf(t.khongDen()));
        lblTyLeDaDat.setText(tyLe(t.tyLeDaDat()));
        lblTyLeDaDen.setText(tyLe(t.tyLeDaDen()));
        lblTyLeHoanTat.setText(tyLe(t.tyLeHoanTat()));
        lblTyLeDaHuy.setText(tyLe(t.tyLeDaHuy()));
        lblTyLeKhongDen.setText(tyLe(t.tyLeKhongDen()));
        baoTinhTrang(t.coDuLieu() ? t.tong() + " lịch hẹn trong ngày" : KHONG_CO_LICH, MUTED);
    }

    private void hienLoi(String thongBao) {
        model.setRowCount(0);
        for (JLabel l : new JLabel[]{lblTong, lblDaDat, lblDaDen, lblHoanTat, lblDaHuy, lblKhongDen}) {
            l.setText("—");
        }
        for (JLabel l : new JLabel[]{lblTyLeDaDat, lblTyLeDaDen, lblTyLeHoanTat, lblTyLeDaHuy, lblTyLeKhongDen}) {
            l.setText(" ");
        }
        baoTinhTrang(thongBao, DO);
    }

    /** Bảng trống thì thông báo vẽ giữa bảng; có dòng thì hiện dưới bảng. */
    private void baoTinhTrang(String text, Color mau) {
        lblThongBao.setText(text);
        lblThongBao.setForeground(mau);
        lblThongBao.setVisible(model.getRowCount() > 0);
        tblLich.repaint();
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

    static String tyLe(double phanTram) {
        return String.format(VI, "%.1f%% tổng số lịch", phanTram);
    }

    @Override
    public void removeNotify() {
        if (worker != null) {
            worker.cancel(true);
            worker = null;
        }
        super.removeNotify();
    }

    private static JLabel nhan(String text, java.awt.Font f, Color mau) {
        JLabel l = new JLabel(text);
        l.setFont(f);
        l.setForeground(mau);
        return l;
    }
}
