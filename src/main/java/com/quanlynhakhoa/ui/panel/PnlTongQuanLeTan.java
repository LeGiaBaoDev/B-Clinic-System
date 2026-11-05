package com.quanlynhakhoa.ui.panel;

import static com.quanlynhakhoa.ui.component.UiConstants.*;
import com.quanlynhakhoa.dao.HoaDonDAO;
import com.quanlynhakhoa.dao.LichHenDAO;
import com.quanlynhakhoa.dao.PhieuKhamDAO;
import com.quanlynhakhoa.entity.HoaDon;
import com.quanlynhakhoa.entity.LichHen;
import com.quanlynhakhoa.entity.PhieuKham;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.TrangThaiLichHen;
import com.quanlynhakhoa.entity.enums.TrangThaiPhieuKham;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import com.quanlynhakhoa.ui.dialog.HopThoaiLoi;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** Tổng quan dành cho lễ tân: việc cần xử lý ngay khi mở ứng dụng. */
public class PnlTongQuanLeTan extends JPanel {

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy",
            Locale.of("vi", "VN"));
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final TaiKhoan taiKhoan;
    private final Consumer<String> moTrang;
    private final LichHenDAO lichHenDAO = new LichHenDAO();
    private final PhieuKhamDAO phieuKhamDAO = new PhieuKhamDAO();
    private final HoaDonDAO hoaDonDAO = new HoaDonDAO();

    private final JLabel lblNgay = new JLabel(" ");
    private final JLabel lblLoi = new JLabel(" ");
    private final JLabel lblLichHen = new JLabel("—");
    private final JLabel lblLichHenSub = new JLabel(" ");
    private final JLabel lblChoKham = new JLabel("—");
    private final JLabel lblChoKhamSub = new JLabel(" ");
    private final JLabel lblChuaThanhToan = new JLabel("—");
    private final JLabel lblChuaThanhToanSub = new JLabel(" ");
    private final JLabel lblDoanhThu = new JLabel("—");
    private final JLabel lblDoanhThuSub = new JLabel(" ");
    private final JLabel lblSapToi = new JLabel("—");
    private final JLabel lblSapToiSub = new JLabel(" ");

    private final DefaultTableModel modelLich = new DefaultTableModel(
            new String[]{"GIỜ", "BỆNH NHÂN", "NHA SĨ", "TRẠNG THÁI"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel modelHoaDon = new DefaultTableModel(
            new String[]{"MÃ HĐ", "BỆNH NHÂN", "SỐ TIỀN"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel modelSapToi = new DefaultTableModel(
            new String[]{"THỜI GIAN", "BỆNH NHÂN", "NHA SĨ"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public PnlTongQuanLeTan(TaiKhoan taiKhoan, Consumer<String> moTrang) {
        super(new BorderLayout(0, 14));
        this.taiKhoan = taiKhoan;
        this.moTrang = moTrang;
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 20, 24));
        add(taoDau(), BorderLayout.NORTH);
        add(taoNoiDung(), BorderLayout.CENTER);
        lamMoi();
    }

    public void lamMoi() {
        lblNgay.setText("Hôm nay, " + LocalDate.now().format(NGAY));
        lblLoi.setText(" ");
        new SwingWorker<DuLieu, Void>() {
            @Override
            protected DuLieu doInBackground() {
                LocalDate today = LocalDate.now();
                List<LichHen> lich = lichHenDAO.timKiem(today, today, null);
                List<PhieuKham> phieu = phieuKhamDAO.timKiem(today, today);
                List<PhieuKham> cho = phieu.stream()
                        .filter(pk -> pk.getTrangThai() == TrangThaiPhieuKham.CHO_KHAM
                                || pk.getTrangThai() == TrangThaiPhieuKham.DANG_KHAM)
                        .toList();
                List<HoaDon> hoaDon = hoaDonDAO.timKiem(today, today, null);
                Map<String, BigDecimal> tong = new HashMap<>();
                BigDecimal doanhThu = BigDecimal.ZERO;
                List<HoaDon> chuaTra = new ArrayList<>();
                for (HoaDon hd : hoaDon) {
                    BigDecimal tien = hoaDonDAO.tinhTongTien(hd.getMaHD());
                    tong.put(hd.getMaHD(), tien);
                    if (hd.isTrangThai()) {
                        doanhThu = doanhThu.add(tien);
                    } else {
                        chuaTra.add(hd);
                    }
                }
                List<LichHen> sapToi = lichHenDAO.timKiem(today.plusDays(1),
                        today.plusDays(7), TrangThaiLichHen.DA_DAT);
                return new DuLieu(lich, cho, chuaTra, tong, doanhThu, sapToi);
            }

            @Override
            protected void done() {
                try {
                    hienThi(get());
                } catch (CancellationException ignored) {
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    HopThoaiLoi.hien(PnlTongQuanLeTan.this, e.getCause(),
                            "Không thể tải tổng quan lễ tân.");
                    lblLoi.setText("Không tải được dữ liệu.");
                }
            }
        }.execute();
    }

    private JPanel taoDau() {
        JPanel dau = new JPanel(new BorderLayout(16, 0));
        dau.setOpaque(false);
        JPanel chu = new JPanel();
        chu.setOpaque(false);
        chu.setLayout(new BoxLayout(chu, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Tổng quan");
        title.setFont(bold(22));
        title.setForeground(TEXT);
        JLabel sub = new JLabel("Việc cần xử lý trong ngày của lễ tân.");
        sub.setFont(font(12));
        sub.setForeground(MUTED);
        lblNgay.setFont(font(11));
        lblNgay.setForeground(MUTED);
        chu.add(title);
        chu.add(Box.createVerticalStrut(4));
        chu.add(sub);
        chu.add(Box.createVerticalStrut(3));
        chu.add(lblNgay);
        dau.add(chu, BorderLayout.WEST);
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        nut.setOpaque(false);
        JButton lamMoi = PnlTongQuanQuanLy.button("Làm mới", LineIcon.Type.CHART, false);
        lamMoi.addActionListener(e -> lamMoi());
        nut.add(lamMoi);
        dau.add(nut, BorderLayout.EAST);
        return dau;
    }

    private JPanel taoNoiDung() {
        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        body.add(taoTheSoLieu(), BorderLayout.NORTH);
        body.add(taoHaiCot(), BorderLayout.CENTER);
        return body;
    }

    private JPanel taoTheSoLieu() {
        JPanel p = new JPanel(new GridLayout(1, 5, 12, 0));
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(0, 96));
        p.add(the("Lịch hẹn hôm nay", lblLichHen, lblLichHenSub, LineIcon.Type.CALENDAR));
        p.add(the("Bệnh nhân chờ khám", lblChoKham, lblChoKhamSub, LineIcon.Type.CLOCK));
        p.add(the("Hóa đơn chưa thanh toán", lblChuaThanhToan, lblChuaThanhToanSub, LineIcon.Type.INVOICE));
        p.add(the("Doanh thu trong ngày", lblDoanhThu, lblDoanhThuSub, LineIcon.Type.CHART));
        p.add(the("Lịch sắp tới (7 ngày)", lblSapToi, lblSapToiSub, LineIcon.Type.CALENDAR));
        return p;
    }

    private JPanel the(String title, JLabel value, JLabel sub, LineIcon.Type icon) {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 4));
        card.setBorder(new EmptyBorder(12, 14, 10, 14));
        JPanel header = new JPanel(new BorderLayout(6, 0));
        header.setOpaque(false);
        JLabel nhan = new JLabel(title);
        nhan.setFont(font(11));
        nhan.setForeground(MUTED);
        header.add(nhan, BorderLayout.CENTER);
        header.add(new JLabel(new LineIcon(icon, TEAL, 16)), BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);
        value.setFont(bold(26));
        value.setForeground(TEXT);
        card.add(value, BorderLayout.CENTER);
        sub.setFont(font(9));
        sub.setForeground(MUTED);
        card.add(sub, BorderLayout.SOUTH);
        return card;
    }

    private JPanel taoHaiCot() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1;
        g.gridx = 0;
        g.weightx = 0.6;
        g.insets = new Insets(0, 0, 0, 14);
        row.add(taoBang("Lịch hẹn hôm nay", modelLich), g);
        g.gridx = 1;
        g.weightx = 0.4;
        g.insets = new Insets(0, 0, 0, 0);
        JPanel phai = new JPanel(new GridLayout(2, 1, 0, 14));
        phai.setOpaque(false);
        phai.add(taoBang("Hóa đơn chưa thanh toán", modelHoaDon));
        phai.add(taoBang("Lịch sắp tới", modelSapToi));
        row.add(phai, g);
        return row;
    }

    private JPanel taoBang(String tieuDe, DefaultTableModel model) {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(14, 16, 12, 16));
        JLabel title = new JLabel(tieuDe);
        title.setFont(bold(14));
        title.setForeground(TEXT);
        card.add(title, BorderLayout.NORTH);
        JTable table = new JTable(model);
        table.setRowHeight(34);
        table.setFont(font(12));
        table.getTableHeader().setFont(bold(10));
        table.getTableHeader().setBackground(new Color(0xF7F9FB));
        table.getTableHeader().setForeground(MUTED);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(0xEDF1F5));
        table.setSelectionBackground(new Color(0xEFF6F5));
        table.setSelectionForeground(TEXT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        if (model == modelLich) {
            table.getColumnModel().getColumn(0).setPreferredWidth(55);
            table.getColumnModel().getColumn(0).setCellRenderer(center);
        }
        if (model == modelHoaDon) {
            table.getColumnModel().getColumn(2).setPreferredWidth(90);
        }
        if (model == modelSapToi) {
            table.getColumnModel().getColumn(0).setPreferredWidth(80);
        }
        JScrollPane scr = new JScrollPane(table);
        scr.setBorder(BorderFactory.createLineBorder(BORDER));
        scr.getViewport().setBackground(Color.WHITE);
        card.add(scr, BorderLayout.CENTER);
        return card;
    }

    private void hienThi(DuLieu d) {
        long daKham = d.lich().stream().filter(lh -> lh.getTrangThai() == TrangThaiLichHen.DA_DEN
                || lh.getTrangThai() == TrangThaiLichHen.HOAN_TAT).count();
        lblLichHen.setText(String.valueOf(d.lich().size()));
        lblLichHenSub.setText(daKham + " đã đến/hoàn tất");
        lblChoKham.setText(String.valueOf(d.choKham().size()));
        lblChoKhamSub.setText(d.choKham().isEmpty() ? "Phòng chờ đang trống" : "Đang chờ trong phòng khám");
        lblChuaThanhToan.setText(String.valueOf(d.chuaThanhToan().size()));
        lblChuaThanhToanSub.setText(d.chuaThanhToan().isEmpty() ? "Không còn công nợ"
                : tien(d.chuaThanhToan().stream().map(hd -> d.tong().getOrDefault(hd.getMaHD(),
                        BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add)));
        lblDoanhThu.setText(tien(d.doanhThu()));
        lblDoanhThuSub.setText("Đã thu trong ngày");
        lblSapToi.setText(String.valueOf(d.sapToi().size()));
        lblSapToiSub.setText("Lịch hẹn 7 ngày tới");

        modelLich.setRowCount(0);
        d.lich().stream().sorted((a, b) -> a.getNgayGioHen().compareTo(b.getNgayGioHen()))
                .forEach(lh -> modelLich.addRow(new Object[]{
                    lh.getNgayGioHen().format(GIO),
                    lh.getKhachHang().getTenKH(),
                    lh.getNhaSi() == null ? "—" : lh.getNhaSi().getHoTenNV(),
                    nhanTrangThai(lh.getTrangThai())}));

        modelHoaDon.setRowCount(0);
        d.chuaThanhToan().stream()
                .sorted((a, b) -> b.getNgayLap().compareTo(a.getNgayLap()))
                .forEach(hd -> modelHoaDon.addRow(new Object[]{
                    hd.getMaHD(),
                    tenKhach(hd),
                    tien(d.tong().getOrDefault(hd.getMaHD(), BigDecimal.ZERO))}));

        modelSapToi.setRowCount(0);
        d.sapToi().stream().sorted((a, b) -> a.getNgayGioHen().compareTo(b.getNgayGioHen()))
                .forEach(lh -> modelSapToi.addRow(new Object[]{
                    lh.getNgayGioHen().format(NGAY_GIO),
                    lh.getKhachHang().getTenKH(),
                    lh.getNhaSi() == null ? "—" : lh.getNhaSi().getHoTenNV()}));
    }

    private static String tenKhach(HoaDon hd) {
        if (hd.getPhieuKham() == null || hd.getPhieuKham().getLichHen() == null) {
            return "—";
        }
        return hd.getPhieuKham().getLichHen().getKhachHang().getTenKH();
    }

    private static String nhanTrangThai(TrangThaiLichHen tt) {
        return switch (tt) {
            case DA_DAT -> "Đã đặt";
            case DA_DEN -> "Đã đến";
            case HOAN_TAT -> "Hoàn tất";
            case DA_HUY -> "Đã hủy";
            case KHONG_DEN -> "Không đến";
        };
    }

    private static String tien(BigDecimal so) {
        DecimalFormat f = new DecimalFormat("#,##0",
                DecimalFormatSymbols.getInstance(Locale.of("vi", "VN")));
        return f.format(so == null ? BigDecimal.ZERO : so) + " ₫";
    }

    private record DuLieu(List<LichHen> lich, List<PhieuKham> choKham,
            List<HoaDon> chuaThanhToan, Map<String, BigDecimal> tong,
            BigDecimal doanhThu, List<LichHen> sapToi) {
    }
}
