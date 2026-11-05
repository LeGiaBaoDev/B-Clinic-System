package com.quanlynhakhoa.ui.panel;

import static com.quanlynhakhoa.ui.component.UiConstants.BACKGROUND;
import static com.quanlynhakhoa.ui.component.UiConstants.BORDER;
import static com.quanlynhakhoa.ui.component.UiConstants.MUTED;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL_DARK;
import static com.quanlynhakhoa.ui.component.UiConstants.TEXT;
import static com.quanlynhakhoa.ui.component.UiConstants.bold;
import static com.quanlynhakhoa.ui.component.UiConstants.font;

import com.quanlynhakhoa.dao.ThongKeBenhNhanDAO;
import com.quanlynhakhoa.dao.ThongKeBenhNhanDAO.BenhNhanTrongKy;
import com.quanlynhakhoa.dao.ThongKeBenhNhanDAO.TheoNgay;
import com.quanlynhakhoa.dao.ThongKeBenhNhanDAO.TomTat;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import com.quanlynhakhoa.ui.dialog.DlgThongTinBenhNhan;
import com.quanlynhakhoa.ui.dialog.HopThoaiLoi;
import com.toedter.calendar.JDateChooser;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class PnlThongKeBenhNhan extends JPanel {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter THU_NGAY =
            DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale.of("vi", "VN"));

    private final ThongKeBenhNhanDAO dao = new ThongKeBenhNhanDAO();
    private final JDateChooser dpTu = new JDateChooser();
    private final JDateChooser dpDen = new JDateChooser();
    private final JButton btnXem = new JButton("Xem thống kê");
    private final JButton btnHomNay = new JButton("Hôm nay");
    private final JButton btnBayNgay = new JButton("7 ngày qua");
    private final JButton btnThangNay = new JButton("Tháng này");
    private final JLabel lblLuot = new JLabel("0");
    private final JLabel lblDoanhThu = new JLabel("0 đ");
    private final JLabel lblMoi = new JLabel("0");
    private final JLabel lblGhiChu = new JLabel("Chưa có dữ liệu");
    private final JLabel lblKhoang = new JLabel(" ");
    private final JLabel lblChartTrong = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel lblBangTrong = new JLabel(" ", SwingConstants.CENTER);
    private final BieuDoBenhNhan bieuDo = new BieuDoBenhNhan();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"BỆNH NHÂN", "BỆNH / CHẨN ĐOÁN"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JPanel pnlBang = new JPanel(new BorderLayout());
    private List<BenhNhanTrongKy> hienTai = List.of();
    private SwingWorker<?, ?> worker;

    public PnlThongKeBenhNhan() {
        super(new BorderLayout(0, 14));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 20, 24));
        initComponents();
        addEvents();
        datKhoang(LocalDate.now(ZONE), LocalDate.now(ZONE));
        taiDuLieu();
    }

    public void lamMoi() {
        taiDuLieu();
    }

    private void initComponents() {
        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(taoHeader());
        north.add(Box.createVerticalStrut(12));
        north.add(taoBoLoc());
        add(north, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        JPanel kpiWrap = new JPanel(new BorderLayout(0, 8));
        kpiWrap.setOpaque(false);
        kpiWrap.add(taoKpi(), BorderLayout.CENTER);
        lblGhiChu.setFont(font(11));
        lblGhiChu.setForeground(MUTED);
        kpiWrap.add(lblGhiChu, BorderLayout.SOUTH);
        body.add(kpiWrap, BorderLayout.NORTH);
        JPanel chia = new JPanel(new GridLayout(1, 2, 14, 0));
        chia.setOpaque(false);
        chia.add(taoChart());
        chia.add(taoBang());
        body.add(chia, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    private JPanel taoHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Thống kê số bệnh nhân");
        title.setFont(bold(21));
        title.setForeground(TEXT);
        JLabel sub = new JLabel("Chỉ tính các phiếu khám đã hoàn tất trong khoảng thời gian đã chọn.");
        sub.setFont(font(12));
        sub.setForeground(MUTED);
        text.add(title);
        text.add(Box.createVerticalStrut(4));
        text.add(sub);
        p.add(text, BorderLayout.WEST);
        lblKhoang.setFont(font(11));
        lblKhoang.setForeground(MUTED);
        p.add(lblKhoang, BorderLayout.EAST);
        return p;
    }

    private JPanel taoBoLoc() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(10, 0));
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        JPanel trai = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        trai.setOpaque(false);
        trai.add(nhan("Từ ngày"));
        cauHinhNgay(dpTu);
        trai.add(dpTu);
        trai.add(nhan("Đến ngày"));
        cauHinhNgay(dpDen);
        trai.add(dpDen);
        card.add(trai, BorderLayout.WEST);

        JPanel phai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        phai.setOpaque(false);
        for (JButton b : new JButton[]{btnHomNay, btnBayNgay, btnThangNay}) {
            b.setFont(font(12));
        }
        btnXem.setFont(bold(12));
        btnXem.setBackground(TEAL);
        btnXem.setForeground(Color.WHITE);
        phai.add(btnHomNay);
        phai.add(btnBayNgay);
        phai.add(btnThangNay);
        phai.add(btnXem);
        card.add(phai, BorderLayout.EAST);
        return card;
    }

    private void cauHinhNgay(JDateChooser chooser) {
        chooser.setDateFormatString("dd/MM/yyyy");
        chooser.setFont(font(12));
        chooser.setPreferredSize(new Dimension(138, 34));
        chooser.getDateEditor().getUiComponent().setFont(font(12));
        ((javax.swing.JTextField) chooser.getDateEditor().getUiComponent()).setEditable(false);
    }

    private JPanel taoKpi() {
        JPanel row = new JPanel(new GridLayout(1, 3, 14, 0));
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(0, 92));
        row.add(kpiCard("Lượt khám hoàn tất", lblLuot));
        row.add(kpiCard("Doanh thu cá nhân", lblDoanhThu));
        row.add(kpiCard("Bệnh nhân mới", lblMoi));
        return row;
    }

    private static JPanel kpiCard(String title, JLabel value) {
        value.setFont(bold(26));
        value.setForeground(TEAL_DARK);
        return kpiKhung(title, value);
    }

    private static JPanel kpiKhung(String title, JLabel value) {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        JLabel t = new JLabel(title);
        t.setFont(font(12));
        t.setForeground(MUTED);
        card.add(t, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel taoChart() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(14, 16, 12, 16));
        JLabel title = new JLabel("Bệnh nhân theo ngày");
        title.setFont(bold(13));
        title.setForeground(TEXT);
        card.add(title, BorderLayout.NORTH);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(bieuDo, BorderLayout.CENTER);
        lblChartTrong.setFont(font(11));
        lblChartTrong.setForeground(MUTED);
        lblChartTrong.setVisible(false);
        wrap.add(lblChartTrong, BorderLayout.SOUTH);
        card.add(wrap, BorderLayout.CENTER);
        return card;
    }

    private JPanel taoBang() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(14, 16, 12, 16));
        JLabel title = new JLabel("Bệnh nhân trong ngày");
        title.setFont(bold(13));
        title.setForeground(TEXT);
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title, BorderLayout.WEST);
        card.add(header, BorderLayout.NORTH);

        table.setRowHeight(36);
        table.setFont(font(11));
        table.getTableHeader().setFont(bold(9));
        table.getTableHeader().setBackground(new Color(0xF7F9FB));
        table.getTableHeader().setForeground(MUTED);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(0xEDF1F5));
        table.setSelectionBackground(new Color(0xEFF6F5));
        table.setSelectionForeground(TEXT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(1).setPreferredWidth(300);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    moThongTin(row);
                }
            }
        });
        JScrollPane scr = new JScrollPane(table);
        scr.setBorder(BorderFactory.createLineBorder(BORDER));
        scr.getViewport().setBackground(Color.WHITE);
        pnlBang.setOpaque(false);
        pnlBang.add(scr, BorderLayout.CENTER);
        lblBangTrong.setFont(font(11));
        lblBangTrong.setForeground(MUTED);
        lblBangTrong.setHorizontalAlignment(SwingConstants.CENTER);
        lblBangTrong.setVisible(false);
        pnlBang.add(lblBangTrong, BorderLayout.SOUTH);
        card.add(pnlBang, BorderLayout.CENTER);
        return card;
    }

    private void addEvents() {
        btnXem.addActionListener(e -> taiDuLieu());
        btnHomNay.addActionListener(e -> {
            datKhoang(LocalDate.now(ZONE), LocalDate.now(ZONE));
            taiDuLieu();
        });
        btnBayNgay.addActionListener(e -> {
            LocalDate homNay = LocalDate.now(ZONE);
            datKhoang(homNay.minusDays(6), homNay);
            taiDuLieu();
        });
        btnThangNay.addActionListener(e -> {
            LocalDate homNay = LocalDate.now(ZONE);
            datKhoang(homNay.withDayOfMonth(1), homNay);
            taiDuLieu();
        });
    }

    private void datKhoang(LocalDate tu, LocalDate den) {
        dpTu.setDate(Date.from(tu.atStartOfDay(ZONE).toInstant()));
        dpDen.setDate(Date.from(den.atStartOfDay(ZONE).toInstant()));
    }

    private void taiDuLieu() {
        LocalDate tu = ngay(dpTu);
        LocalDate den = ngay(dpDen);
        if (tu == null || den == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn đầy đủ từ ngày và đến ngày.",
                    "Thiếu dữ liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (den.isBefore(tu)) {
            JOptionPane.showMessageDialog(this, "Đến ngày phải sau hoặc bằng từ ngày.",
                    "Khoảng ngày không hợp lệ", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (worker != null) {
            worker.cancel(true);
        }
        btnXem.setEnabled(false);
        for (JLabel l : new JLabel[]{lblLuot, lblDoanhThu, lblMoi}) {
            l.setText("—");
        }
        lblGhiChu.setText("Đang tải...");
        lblChartTrong.setVisible(false);
        lblBangTrong.setVisible(false);
        worker = new SwingWorker<DuLieu, Void>() {
            @Override
            protected DuLieu doInBackground() {
                List<TheoNgay> ds = dao.thongKe(tu, den);
                TomTat tt = dao.tongHop(tu, den);
                List<BenhNhanTrongKy> benhNhan = dao.danhSachBenhNhan(tu, den);
                return new DuLieu(ds, tt, benhNhan);
            }

            @Override
            protected void done() {
                try {
                    DuLieu d = get();
                    hienThi(tu, den, d);
                } catch (CancellationException ignored) {
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    HopThoaiLoi.hien(PnlThongKeBenhNhan.this, e.getCause(), "Không thể tải thống kê bệnh nhân.");
                    for (JLabel l : new JLabel[]{lblLuot, lblDoanhThu, lblMoi}) {
                        l.setText("—");
                    }
                    lblGhiChu.setText("Không tải được dữ liệu");
                } finally {
                    btnXem.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void hienThi(LocalDate tu, LocalDate den, DuLieu d) {
        TomTat tt = d.tomTat();
        lblLuot.setText(String.valueOf(tt.soLuotKham()));
        lblDoanhThu.setText(money(tt.doanhThu()));
        lblMoi.setText(String.valueOf(tt.soBenhNhanMoi()));
        lblKhoang.setText(tu.format(NGAY) + " — " + den.format(NGAY));
        Map<LocalDate, Long> theoNgay = new LinkedHashMap<>();
        for (TheoNgay n : d.ds()) {
            theoNgay.put(n.ngay(), n.soBenhNhan());
        }
        List<TheoNgay> chart = new ArrayList<>();
        long soNgayCoDuLieu = 0;
        for (LocalDate ngay = tu; !ngay.isAfter(den); ngay = ngay.plusDays(1)) {
            long so = theoNgay.getOrDefault(ngay, 0L);
            chart.add(new TheoNgay(ngay, so));
            if (so > 0) {
                soNgayCoDuLieu++;
            }
        }
        hienTai = d.benhNhan();
        model.setRowCount(0);
        for (BenhNhanTrongKy bn : d.benhNhan()) {
            model.addRow(new Object[]{bn.tenKH(), chanDoanNgan(bn.chanDoan())});
        }
        boolean co = tt.soLuotKham() > 0 || tt.soBenhNhan() > 0;
        boolean coBenhNhan = !d.benhNhan().isEmpty();
        lblGhiChu.setText(co
                ? soNgayCoDuLieu + " ngày có khám · " + tt.soLuotKham() + " lượt · "
                  + tt.soBenhNhan() + " bệnh nhân (" + tt.soBenhNhanMoi() + " mới)"
                : "Không có dữ liệu để thống kê");
        bieuDo.setPoints(chart);
        lblChartTrong.setText(" ");
        lblChartTrong.setVisible(false);
        lblBangTrong.setText(coBenhNhan ? " " : "Chưa có bệnh nhân trong khoảng này");
        lblBangTrong.setVisible(!coBenhNhan);
    }

    private void moThongTin(int row) {
        if (row < 0 || row >= hienTai.size()) {
            return;
        }
        DlgThongTinBenhNhan.hien(javax.swing.SwingUtilities.getWindowAncestor(this), hienTai.get(row));
    }

    private static String chanDoanNgan(String s) {
        if (s == null || s.isBlank()) {
            return "—";
        }
        for (String d : s.split("\r?\n")) {
            String t = d.trim();
            if (t.startsWith("Chẩn đoán:")) {
                t = t.substring("Chẩn đoán:".length()).trim();
            }
            if (!t.isBlank()) {
                return t;
            }
        }
        return "—";
    }

    private static String money(double v) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.of("vi", "VN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(v) + " đ";
    }

    private static LocalDate ngay(JDateChooser chooser) {
        return chooser.getDate() == null ? null
                : chooser.getDate().toInstant().atZone(ZONE).toLocalDate();
    }

    private record DuLieu(List<TheoNgay> ds, TomTat tomTat,
            List<BenhNhanTrongKy> benhNhan) {
    }

    private static String escapeHtml(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static JLabel nhan(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(11));
        l.setForeground(MUTED);
        return l;
    }

    private static final class BieuDoBenhNhan extends JPanel {

        private List<TheoNgay> points = List.of();

        BieuDoBenhNhan() {
            setOpaque(false);
            setPreferredSize(new Dimension(280, 240));
            setMinimumSize(new Dimension(160, 160));
        }

        void setPoints(List<TheoNgay> points) {
            this.points = List.copyOf(points);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int left = 42, right = 10, top = 20, bottom = 34;
            int chartW = Math.max(1, getWidth() - left - right);
            int chartH = Math.max(1, getHeight() - top - bottom);
            long max = points.stream().mapToLong(TheoNgay::soBenhNhan).max().orElse(0);
            long scale = Math.max(1, max);

            g.setFont(font(8));
            FontMetrics fmY = g.getFontMetrics();
            g.setStroke(new BasicStroke(1));
            for (int i = 0; i <= 4; i++) {
                int y = top + chartH - chartH * i / 4;
                g.setColor(new Color(0xEDF2F5));
                g.drawLine(left, y, left + chartW, y);
                String nhan = String.valueOf(Math.round(max * i / 4.0));
                g.setColor(MUTED);
                g.drawString(nhan, left - 7 - fmY.stringWidth(nhan), y + 3);
            }
            g.setColor(new Color(0xB9C5D1));
            g.drawLine(left, top, left, top + chartH);
            g.drawLine(left, top + chartH, left + chartW, top + chartH);

            if (points.isEmpty() || max == 0) {
                g.setFont(font(11));
                g.setColor(MUTED);
                String empty = "Không có dữ liệu để thống kê";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(empty, Math.max(2, (getWidth() - fm.stringWidth(empty)) / 2),
                        Math.max(14, top + chartH / 2));
                g.dispose();
                return;
            }

            double step = chartW / (double) points.size();
            int barW = Math.max(4, Math.min(30, (int) (step * 0.62)));
            int skip = Math.max(1, (int) Math.ceil(points.size() / 8.0));
            g.setFont(font(8));
            FontMetrics fm = g.getFontMetrics();
            for (int i = 0; i < points.size(); i++) {
                TheoNgay p = points.get(i);
                int h = (int) Math.round(chartH * p.soBenhNhan() / (double) scale);
                int x = (int) Math.round(left + step * i + (step - barW) / 2);
                int y = top + chartH - h;
                g.setColor(p.soBenhNhan() > 0 ? new Color(0x7FC7C4) : new Color(0xE6EEF2));
                g.fill(new RoundRectangle2D.Double(x, y, barW, Math.max(2, h), 3, 3));
                if (p.soBenhNhan() > 0) {
                    String value = String.valueOf(p.soBenhNhan());
                    g.setColor(TEAL_DARK);
                    g.drawString(value, x + (barW - fm.stringWidth(value)) / 2, Math.max(10, y - 4));
                }
                if (i % skip == 0 || i == points.size() - 1) {
                    String date = p.ngay().format(DateTimeFormatter.ofPattern("dd/MM"));
                    g.setColor(MUTED);
                    g.drawString(date, (int) (left + step * i + (step - fm.stringWidth(date)) / 2),
                            getHeight() - 14);
                }
            }
            g.dispose();
        }
    }
}
