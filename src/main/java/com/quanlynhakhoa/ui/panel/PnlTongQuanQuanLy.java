package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO;
import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.DoanhThuNgay;
import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.LichHenTrongNgay;
import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.NhaSiHienTai;
import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.TongQuanQuanLy;
import com.quanlynhakhoa.entity.enums.TrangThaiLichHen;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.ui.component.*;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import static com.quanlynhakhoa.ui.panel.PnlQuanLy.label;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;

/**
 * Tổng quan quản lý, tải dữ liệu DAO bằng SwingWorker; không chạy truy vấn trên
 * EDT.
 */
public class PnlTongQuanQuanLy extends JPanel {

    private static final Logger LOG = Logger.getLogger(PnlTongQuanQuanLy.class.getName());
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");
    private final Consumer<String> dieuHuong;
    private final String tenQuanLy;
    private final Supplier<TongQuanQuanLy> taiDuLieu;
    private final javax.swing.Timer tuLamMoi = new javax.swing.Timer(60_000, e -> lamMoiDuLieu());
    private SwingWorker<TongQuanQuanLy, Void> worker;
    private Consumer<TrangThaiTai> langNghe = state -> {
    };
    private TongQuanQuanLy duLieu;
    private final JLabel[] lblSoLieu = new JLabel[4], lblGhiChu = new JLabel[4];
    private final JLabel lblLoiChao = label("", bold(24), TEXT), lblNgay = label("", font(12), MUTED);
    private final JLabel lblCaLichHen = label("Đang tải lịch hẹn...", font(12), MUTED);
    private final JLabel lblLichTiep = label("Đang tải dữ liệu...", font(11), MUTED);
    private final JLabel lblTongBayNgay = new FitLabel("—", bold(27), 20);
    private final JLabel lblTangTruong = label(" — ", bold(10), MUTED);
    private final JLabel lblTieuDeNhaSi = label("Trạng thái nha sĩ", bold(15), TEXT);
    private final JLabel lblGhiChuNhaSi = label("Đang tải dữ liệu...", font(11), TEAL);
    private final JPanel pnlNhaSi = new JPanel();
    private final BieuDoDoanhThu bieuDo = new BieuDoDoanhThu();
    private List<LichHenTrongNgay> lichHen = List.of();
    private String maLichTiep;
    private DefaultTableModel modelLichHen;
    private JTable tblLichHen;
    private TableRowSorter<DefaultTableModel> sorter;
    private Boolean compactMode;

    public PnlTongQuanQuanLy(String tenQuanLy, Consumer<String> dieuHuong) {
        this(tenQuanLy, dieuHuong, () -> new ThongKeDoanhThuDAO().layTongQuanQuanLy());
    }

    public PnlTongQuanQuanLy(String tenQuanLy, Consumer<String> dieuHuong, Supplier<TongQuanQuanLy> taiDuLieu) {
        super(new GridBagLayout());
        this.dieuHuong = dieuHuong;
        this.tenQuanLy = tenQuanLy;
        this.taiDuLieu = java.util.Objects.requireNonNull(taiDuLieu);
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 30, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.BOTH;
        JPanel greeting = taoLoiChao(tenQuanLy), metrics = taoTheSoLieu();
        g.gridy = 0;
        g.weighty = 0;
        g.insets = new Insets(0, 0, 20, 0);
        add(greeting, g);
        g.gridy = 1;
        add(metrics, g);
        g.gridy = 2;
        g.weighty = 1;
        g.insets = new Insets(0, 0, 0, 0);
        add(taoNoiDung(), g);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                boolean compact = getHeight() < 700;
                if (compactMode != null && compactMode == compact) {
                    return;
                }
                compactMode = compact;
                setBorder(new EmptyBorder(compact ? 18 : 24, 24, compact ? 20 : 30, 24));
                greeting.setPreferredSize(new Dimension(0, compact ? 60 : 64));
                greeting.setMinimumSize(new Dimension(0, compact ? 60 : 64));
                metrics.setPreferredSize(new Dimension(0, compact ? 104 : 124));
                metrics.setMinimumSize(new Dimension(0, compact ? 104 : 124));
                GridBagLayout layout = (GridBagLayout) getLayout();
                for (JPanel panel : new JPanel[]{greeting, metrics}) {
                    GridBagConstraints constraints = layout.getConstraints(panel);
                    constraints.insets = new Insets(0, 0, compact ? 14 : 20, 0);
                    layout.setConstraints(panel, constraints);
                }
                revalidate();
            }
        });
        capNhatLoiChao(LocalDateTime.now(ZONE));
    }

    private JPanel taoLoiChao(String tenQuanLy) {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(0, 64));
        JPanel text = new JPanel(new BorderLayout(0, 4));
        text.setOpaque(false);
        text.add(lblLoiChao, BorderLayout.NORTH);
        text.add(lblNgay, BorderLayout.SOUTH);
        panel.add(text, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 5));
        actions.setOpaque(false);
        JButton ca = button("Phân ca làm việc", null, false);
        ca.addActionListener(e -> dieuHuong.accept("Lịch làm việc"));
        JButton baoCao = button("Xem báo cáo", LineIcon.Type.CHART, true);
        baoCao.addActionListener(e -> dieuHuong.accept("Báo cáo"));
        actions.add(ca);
        actions.add(baoCao);
        panel.add(actions, BorderLayout.EAST);
        return panel;
    }

    private JPanel taoTheSoLieu() {
        JPanel metrics = new JPanel(new GridLayout(1, 4, 16, 0));
        metrics.setOpaque(false);
        metrics.setPreferredSize(new Dimension(0, 124));
        metrics.add(the(0, "Lịch hẹn hôm nay", LineIcon.Type.CALENDAR));
        metrics.add(the(1, "Doanh thu dự kiến", LineIcon.Type.CHART));
        metrics.add(the(2, "Hóa đơn đã thanh toán", LineIcon.Type.CHECK));
        metrics.add(the(3, "Hóa đơn chưa thanh toán", LineIcon.Type.INVOICE));
        return metrics;
    }

    private JPanel the(int index, String title, LineIcon.Type icon) {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(16, 16, 15, 16));
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.add(label(title, font(12), MUTED), BorderLayout.CENTER);
        header.add(new JLabel(new LineIcon(icon, TEAL, 17)), BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);
        FitLabel number = new FitLabel("—", bold(29), 18);
        number.setForeground(TEXT);
        card.add(number, BorderLayout.CENTER);
        FitLabel subtitle = new FitLabel("Đang tải dữ liệu...", font(11), 9);
        subtitle.setForeground(MUTED);
        lblSoLieu[index] = number;
        lblGhiChu[index] = subtitle;
        subtitle.setPreferredSize(new Dimension(0, 17));
        card.add(subtitle, BorderLayout.SOUTH);
        card.setMinimumSize(new Dimension(0, 98));
        return card;
    }

    private JPanel taoNoiDung() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        JPanel left = new JPanel(new BorderLayout(0, 20));
        left.setOpaque(false);
        left.add(taoLichHen(), BorderLayout.CENTER);
        left.add(taoThaoTacNhanh(), BorderLayout.SOUTH);
        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        GridBagConstraints r = new GridBagConstraints();
        r.gridx = 0;
        r.weightx = 1;
        r.weighty = 0.54;
        r.fill = GridBagConstraints.BOTH;
        r.gridy = 0;
        r.insets = new Insets(0, 0, 20, 0);
        right.add(taoBieuDo(), r);
        r.gridy = 1;
        r.weighty = 0.46;
        r.insets = new Insets(0, 0, 0, 0);
        right.add(taoTrangThaiNhaSi(), r);
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1;
        g.gridx = 0;
        g.weightx = 0.625;
        g.insets = new Insets(0, 0, 0, 20);
        panel.add(left, g);
        g.gridx = 1;
        g.weightx = 0.375;
        g.insets = new Insets(0, 0, 0, 0);
        panel.add(right, g);
        left.setMinimumSize(new Dimension(0, 0));
        right.setMinimumSize(new Dimension(0, 0));
        return panel;
    }

    private JPanel taoLichHen() {
        SurfacePanel card = new SurfacePanel(new BorderLayout());
        card.setBorder(new EmptyBorder(19, 20, 16, 20));
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel title = label("Lịch hẹn trong ngày", bold(16), TEXT);
        title.setAlignmentX(LEFT_ALIGNMENT);
        heading.add(title);
        heading.add(Box.createVerticalStrut(15));
        JPanel sub = new JPanel(new BorderLayout());
        sub.setOpaque(false);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        sub.add(lblCaLichHen, BorderLayout.WEST);
        JButton link = textLink("Xem lịch làm việc →");
        link.addActionListener(e -> dieuHuong.accept("Lịch làm việc"));
        sub.add(link, BorderLayout.EAST);
        heading.add(sub);
        heading.add(Box.createVerticalStrut(16));
        card.add(heading, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(new Object[0][4], new String[]{"GIỜ HẸN", "BỆNH NHÂN", "BÁC SĨ", "TRẠNG THÁI"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        modelLichHen = model;
        tblLichHen = new JTable(model) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                if (getRowCount() == 0) {
                    String text = duLieu == null ? "Chưa có dữ liệu lịch hẹn" : "Không có lịch hẹn phù hợp";
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.setFont(font(12));
                    g.setColor(MUTED);
                    g.drawString(text, Math.max(10, (getWidth() - g.getFontMetrics().stringWidth(text)) / 2), Math.min(getHeight() - 8, 45));
                    g.dispose();
                }
            }
        };
        tblLichHen.setName("tblLichHen");
        tblLichHen.setFont(font(12));
        tblLichHen.setForeground(TEXT);
        tblLichHen.setBackground(SURFACE);
        tblLichHen.setRowHeight(46);
        tblLichHen.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblLichHen.setSelectionBackground(TEAL_LIGHT);
        tblLichHen.setSelectionForeground(TEXT);
        tblLichHen.setShowVerticalLines(false);
        tblLichHen.setShowHorizontalLines(true);
        tblLichHen.setGridColor(BORDER);
        tblLichHen.setIntercellSpacing(new Dimension(0, 1));
        tblLichHen.setFillsViewportHeight(true);
        tblLichHen.setFocusable(true);
        tblLichHen.setBorder(null);
        tblLichHen.getTableHeader().setReorderingAllowed(false);
        tblLichHen.getTableHeader().setPreferredSize(new Dimension(0, 39));
        tblLichHen.getTableHeader().setBackground(new Color(0xF2F4F7));
        tblLichHen.getTableHeader().setForeground(MUTED);
        tblLichHen.getTableHeader().setFont(bold(10));
        tblLichHen.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
                JLabel result = (JLabel) super.getTableCellRendererComponent(table, value, false, false, row, column);
                result.setFont(bold(10));
                result.setForeground(MUTED);
                result.setBackground(new Color(0xF2F4F7));
                result.setBorder(new EmptyBorder(0, 11, 0, 6));
                return result;
            }
        });
        DefaultTableCellRenderer textRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
                JLabel result = (JLabel) super.getTableCellRendererComponent(table, value, selected, false, row, column);
                result.setBorder(new EmptyBorder(0, 11, 0, 6));
                result.setFont(font(12));
                result.setBackground(selected || laLichTiep(table, row) ? TEAL_LIGHT : SURFACE);
                result.setForeground(TEXT);
                return result;
            }
        };
        tblLichHen.setDefaultRenderer(Object.class, textRenderer);
        tblLichHen.getColumnModel().getColumn(3).setCellRenderer(new BadgeRenderer());
        int[] widths = {79, 215, 175, 105};
        for (int i = 0; i < widths.length; i++) {
            tblLichHen.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        tblLichHen.getColumnModel().getColumn(0).setMinWidth(61);
        tblLichHen.getColumnModel().getColumn(3).setMinWidth(85);
        sorter = new TableRowSorter<>(model);
        tblLichHen.setRowSorter(sorter);
        JScrollPane scroll = new JScrollPane(tblLichHen);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.setColumnHeaderView(tblLichHen.getTableHeader());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(SURFACE);
        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int height = Math.max(23, Math.min(47, scroll.getViewport().getHeight() / 5));
                if (tblLichHen.getRowHeight() != height) {
                    tblLichHen.setRowHeight(height);
                }
            }
        });
        card.add(scroll, BorderLayout.CENTER);
        lblLichTiep.setBorder(new EmptyBorder(16, 0, 2, 0));
        card.add(lblLichTiep, BorderLayout.SOUTH);
        card.setMinimumSize(new Dimension(0, 0));
        return card;
    }

    private JPanel taoThaoTacNhanh() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(0, 36));
        panel.add(label("Thao tác nhanh", bold(12), TEXT));
        JButton nv = button("Thêm nhân viên", LineIcon.Type.PEOPLE, false);
        nv.addActionListener(e -> dieuHuong.accept("Nhân viên"));
        panel.add(nv);
        JButton dv = button("Thêm dịch vụ", LineIcon.Type.PLUS, false);
        dv.addActionListener(e -> dieuHuong.accept("Dịch vụ"));
        panel.add(dv);
        JButton vc = button("Tạo voucher", LineIcon.Type.VOUCHER, false);
        vc.addActionListener(e -> dieuHuong.accept("Voucher"));
        panel.add(vc);
        return panel;
    }

    private JPanel taoBieuDo() {
        SurfacePanel card = new SurfacePanel(new BorderLayout());
        card.setBorder(new EmptyBorder(16, 16, 10, 16));
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JLabel title = label("Doanh thu đã thu · 7 ngày gần nhất", bold(15), TEXT);
        title.setAlignmentX(LEFT_ALIGNMENT);
        top.add(title);
        top.add(Box.createVerticalStrut(12));
        JPanel value = new JPanel(new BorderLayout());
        value.setOpaque(false);
        value.setAlignmentX(LEFT_ALIGNMENT);
        lblTongBayNgay.setForeground(TEXT);
        value.add(lblTongBayNgay, BorderLayout.CENTER);
        lblTangTruong.setOpaque(true);
        lblTangTruong.setBackground(new Color(0xF1F3F6));
        lblTangTruong.setBorder(new EmptyBorder(3, 5, 3, 5));
        JPanel badge = new JPanel(new GridBagLayout());
        badge.setOpaque(false);
        badge.add(lblTangTruong);
        value.add(badge, BorderLayout.EAST);
        top.add(value);
        top.add(Box.createVerticalStrut(9));
        JLabel unit = label("So với 7 ngày trước · Đơn vị: triệu đồng", font(11), MUTED);
        unit.setAlignmentX(LEFT_ALIGNMENT);
        top.add(unit);
        top.add(Box.createVerticalStrut(7));
        card.add(top, BorderLayout.NORTH);
        card.add(bieuDo, BorderLayout.CENTER);
        card.setMinimumSize(new Dimension(0, 0));
        return card;
    }

    private JPanel taoTrangThaiNhaSi() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(14, 16, 12, 16));
        card.add(lblTieuDeNhaSi, BorderLayout.NORTH);
        pnlNhaSi.setOpaque(false);
        pnlNhaSi.setLayout(new BoxLayout(pnlNhaSi, BoxLayout.Y_AXIS));
        pnlNhaSi.add(label("Đang tải danh sách nha sĩ...", font(11), MUTED));
        JScrollPane scroll = new JScrollPane(pnlNhaSi);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        card.add(scroll, BorderLayout.CENTER);
        card.add(lblGhiChuNhaSi, BorderLayout.SOUTH);
        card.setMinimumSize(new Dimension(0, 0));
        return card;
    }

    private JPanel nhaSi(String name, String desc, String status, int color) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        JPanel top = new JPanel(new BorderLayout(4, 0));
        top.setOpaque(false);
        top.add(label(name, bold(12), TEXT), BorderLayout.WEST);
        Color foreground = color == 1 ? new Color(0xB67B13) : color == 0 ? GREEN : MUTED;
        JLabel badge = label(" " + status + " ", bold(10), foreground);
        badge.setOpaque(true);
        badge.setBackground(color == 1 ? new Color(0xFFF4DD) : color == 0 ? new Color(0xE4F5ED) : new Color(0xF1F3F6));
        badge.setBorder(new EmptyBorder(3, 3, 3, 3));
        top.add(badge, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);
        panel.add(label(desc, font(11), MUTED), BorderLayout.SOUTH);
        return panel;
    }

    public record TrangThaiTai(boolean dangTai, TongQuanQuanLy duLieu, String loi) {

    }

    public void langNgheTaiDuLieu(Consumer<TrangThaiTai> callback) {
        langNghe = java.util.Objects.requireNonNull(callback);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        tuLamMoi.start();
        SwingUtilities.invokeLater(this::lamMoiDuLieu);
    }

    @Override
    public void removeNotify() {
        tuLamMoi.stop();
        if (worker != null) {
            worker.cancel(true);
            worker = null;
        }
        super.removeNotify();
    }

    public void lamMoiDuLieu() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::lamMoiDuLieu);
            return;
        }
        if (worker != null) {
            return;
        }
        langNghe.accept(new TrangThaiTai(true, duLieu, null));
        worker = new SwingWorker<>() {
            @Override
            protected TongQuanQuanLy doInBackground() {
                return taiDuLieu.get();
            }

            @Override
            protected void done() {
                if (this != worker || isCancelled()) {
                    return;
                }
                try {
                    TongQuanQuanLy data = java.util.Objects.requireNonNull(get(), "DAO trả dữ liệu rỗng.");
                    hienThiDuLieu(data);
                    langNghe.accept(new TrangThaiTai(false, data, null));
                } catch (CancellationException ignored) {
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    LOG.log(Level.WARNING, "Không tải được tổng quan quản lý", cause);
                    String message = cause instanceof BusinessException ? cause.getMessage() : "Không tải được dữ liệu. Nhấn F5 để thử lại.";
                    if (duLieu == null) {
                        for (JLabel note : lblGhiChu) {
                            note.setText("Chưa tải được dữ liệu");
                        }
                        lblCaLichHen.setText("Chưa tải được lịch hẹn");
                        lblLichTiep.setText("Nhấn F5 để thử lại.");
                        pnlNhaSi.removeAll();
                        pnlNhaSi.add(label("Chưa tải được danh sách nha sĩ.", font(11), MUTED));
                        pnlNhaSi.revalidate();
                        pnlNhaSi.repaint();
                    }
                    langNghe.accept(new TrangThaiTai(false, duLieu, message));
                } finally {
                    if (this == worker) {
                        worker = null;
                    }
                }
            }
        };
        worker.execute();
    }

    public void hienThiDuLieu(TongQuanQuanLy data) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Cập nhật UI phải trên EDT.");
        }
        duLieu = java.util.Objects.requireNonNull(data);
        capNhatLoiChao(data.capNhatLuc());
        lblSoLieu[0].setText(Long.toString(data.tongLichHen()));
        lblGhiChu[0].setText(data.daDat() + " đã đặt · " + data.daDen() + " đã đến · " + data.daHuy() + " đã hủy");
        lblGhiChu[0].setToolTipText("Hoàn tất: " + data.hoanTat() + " · Không đến: " + data.khongDen());
        lblSoLieu[1].setText(tien(data.doanhThuDuKien()));
        lblGhiChu[1].setText("Đã thu hôm nay + còn phải thu");
        lblSoLieu[2].setText(Long.toString(data.hoaDonDaTra()));
        lblGhiChu[2].setText("Hôm nay: " + tien(data.daThu()));
        lblSoLieu[3].setText(Long.toString(data.hoaDonChuaTra()));
        lblGhiChu[3].setText("Còn phải thu: " + tien(data.conPhaiThu()));
        lblSoLieu[3].setToolTipText("Tất cả hóa đơn chưa thanh toán đến " + data.capNhatLuc().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
        lblTongBayNgay.setText(tien(data.thuBayNgay()));
        var change = data.tangTruong();
        boolean down = change.isPresent() && change.get().signum() < 0;
        boolean unchanged = change.isEmpty() || change.get().signum() == 0;
        lblTangTruong.setText(change.isEmpty() ? " Chưa có kỳ trước " : " " + (unchanged ? "→ " : down ? "↓ " : "↑ ") + number(change.get().abs(), "0.#") + "% ");
        lblTangTruong.setForeground(unchanged ? MUTED : down ? new Color(0xC94C48) : GREEN);
        lblTangTruong.setBackground(unchanged ? new Color(0xF1F3F6) : down ? new Color(0xFDEDEC) : new Color(0xE5F5ED));
        bieuDo.setPoints(data.bieuDo());
        lichHen = data.lichHen();
        LichHenTrongNgay next = lichTiepTheo();
        maLichTiep = next == null ? null : next.maLH();
        modelLichHen.setRowCount(0);
        for (LichHenTrongNgay lh : lichHen) {
            modelLichHen.addRow(new Object[]{lh.ngayGio().format(GIO), lh.benhNhan(), "BS. " + lh.nhaSi(), tenTrangThai(lh.trangThai())});
        }
        lblCaLichHen.setText("Hôm nay · " + data.tongLichHen() + " lịch hẹn");
        lblLichTiep.setText(next == null ? (lichHen.isEmpty() ? "Hôm nay chưa có lịch hẹn." : "Không còn lịch hẹn đang chờ hoặc sắp tới trong ngày.")
                : (next.trangThai() == TrangThaiLichHen.DA_DEN ? "Đang chờ: " : "Tiếp theo: ") + next.benhNhan() + " · " + next.ngayGio().format(GIO));
        lblLichTiep.setToolTipText(lblLichTiep.getText());
        lblTieuDeNhaSi.setText("Trạng thái nha sĩ · " + data.capNhatLuc().format(GIO));
        pnlNhaSi.removeAll();
        for (NhaSiHienTai ns : data.nhaSi()) {
            boolean busy = ns.benhNhanDangKham() != null;
            String status = busy ? "Đang khám" : ns.nghiPhep() ? "Nghỉ phép" : ns.trongCa() ? "Đang rảnh" : "Ngoài ca";
            String desc = busy ? ns.benhNhanDangKham() + " · bắt đầu " + ns.batDauKham().format(GIO)
                    : ns.lichTiep() != null ? "Tiếp: " + ns.benhNhanTiep() + " · " + ns.lichTiep().format(GIO)
                    : ns.trongCa() ? "Chưa có lịch tiếp theo trong ngày." : "Không có ca làm việc hiện tại.";
            JPanel row = nhaSi("BS. " + ns.hoTen(), desc, status, busy ? 1 : ns.trongCa() && !ns.nghiPhep() ? 0 : 2);
            row.setAlignmentX(LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
            row.setPreferredSize(new Dimension(0, 66));
            row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(6, 0, 8, 0)));
            row.setToolTipText(desc);
            pnlNhaSi.add(row);
        }
        if (data.nhaSi().isEmpty()) {
            pnlNhaSi.add(label("Chưa có nha sĩ hoạt động.", font(12), MUTED));
        }
        pnlNhaSi.add(Box.createVerticalGlue());
        long busy = data.nhaSi().stream().filter(ns -> ns.benhNhanDangKham() != null).count();
        lblGhiChuNhaSi.setText(data.nhaSi().size() + " nha sĩ hoạt động · " + busy + " đang khám");
        pnlNhaSi.revalidate();
        pnlNhaSi.repaint();
        revalidate();
        repaint();
    }

    private LichHenTrongNgay lichTiepTheo() {
        if (duLieu == null) {
            return null;
        }
        return lichHen.stream().filter(lh -> (lh.trangThai() == TrangThaiLichHen.DA_DAT && !lh.ngayGio().isBefore(duLieu.capNhatLuc()))
                || (lh.trangThai() == TrangThaiLichHen.DA_DEN && (lh.trangThaiPhieu() == null || "CHO_KHAM".equals(lh.trangThaiPhieu()))))
                .findFirst().orElse(null);
    }

    public String thongBaoLichTiep() {
        if (duLieu == null) {
            return "Chưa tải được dữ liệu lịch hẹn.";
        }
        LichHenTrongNgay next = lichTiepTheo();
        return next == null ? "Không có lịch hẹn đang chờ hoặc sắp tới trong ngày."
                : (next.trangThai() == TrangThaiLichHen.DA_DEN ? "Đang chờ: " : "Lịch tiếp theo: ") + next.benhNhan() + " · " + next.ngayGio().format(GIO);
    }

    public boolean coLichDangCho() {
        return lichTiepTheo() != null;
    }

    private boolean laLichTiep(JTable table, int row) {
        int index = table.convertRowIndexToModel(row);
        return index >= 0 && index < lichHen.size() && java.util.Objects.equals(maLichTiep, lichHen.get(index).maLH());
    }

    private void capNhatLoiChao(LocalDateTime now) {
        String greeting = now.getHour() < 12 ? "Chào buổi sáng" : now.getHour() < 18 ? "Chào buổi chiều" : "Chào buổi tối";
        lblLoiChao.setText(greeting + ", " + tenQuanLy);
        String date = now.format(DateTimeFormatter.ofPattern("EEEE, dd 'tháng' MM 'năm' yyyy", Locale.forLanguageTag("vi-VN")));
        lblNgay.setText(Character.toUpperCase(date.charAt(0)) + date.substring(1) + " · Tổng quan hoạt động phòng khám");
    }

    private static String tenTrangThai(TrangThaiLichHen state) {
        return switch (state) {
            case DA_DAT ->
                "Đã đặt";
            case DA_DEN ->
                "Đã đến";
            case HOAN_TAT ->
                "Hoàn tất";
            case DA_HUY ->
                "Đã hủy";
            case KHONG_DEN ->
                "Không đến";
        };
    }

    private static String number(BigDecimal value, String pattern) {
        return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN"))).format(value);
    }

    private static String tien(BigDecimal value) {
        return number(value, "#,##0") + " ₫";
    }

    public void timKiem(String keyword) {
        sorter.setRowFilter(keyword == null || keyword.isBlank() ? null : RowFilter.regexFilter("(?iu)" + Pattern.quote(keyword.trim())));
    }

    public static JButton button(String text, LineIcon.Type icon, boolean primary) {
        Color fg = primary ? Color.WHITE : TEXT;
        JButton button = new JButton(text, icon == null ? null : new LineIcon(icon, fg, 16));
        button.setFont(bold(12));
        button.setForeground(fg);
        button.setBackground(primary ? TEAL_DARK : SURFACE);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setIconTextGap(7);
        button.setMargin(new Insets(9, 13, 9, 13));
        button.putClientProperty("JButton.arc", 5);
        button.putClientProperty("JComponent.minimumHeight", 35);
        button.putClientProperty("JButton.minimumWidth", 0);
        if (!primary) {
            button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), new EmptyBorder(8, 12, 8, 12)));
        }
        return button;
    }

    private static JButton textLink(String text) {
        JButton button = new JButton(text);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setForeground(TEAL);
        button.setFont(font(11));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private final class BadgeRenderer extends JLabel implements TableCellRenderer {

        private Color rowBackground;

        BadgeRenderer() {
            setHorizontalAlignment(CENTER);
            setFont(bold(10));
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
            setText(value.toString());
            rowBackground = selected || laLichTiep(table, row) ? TEAL_LIGHT : SURFACE;
            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(rowBackground);
            g.fillRect(0, 0, getWidth(), getHeight());
            Color bg, fg;
            if ("Đã đến".equals(getText())) {
                bg = TEAL_LIGHT;
                fg = TEAL_DARK;
            } else if ("Đã đặt".equals(getText())) {
                bg = new Color(0xE6F5EC);
                fg = GREEN;
            } else if ("Đã hủy".equals(getText())) {
                bg = new Color(0xFDEDEC);
                fg = new Color(0xC94C48);
            } else if ("Không đến".equals(getText())) {
                bg = new Color(0xFFF4DD);
                fg = new Color(0xB67B13);
            } else {
                bg = new Color(0xF1F3F6);
                fg = new Color(0x697B90);
            }
            int width = g.getFontMetrics(getFont()).stringWidth(getText()) + 17, height = 23;
            g.setColor(bg);
            g.fillRoundRect((getWidth() - width) / 2, (getHeight() - height) / 2, width, height, 4, 4);
            g.dispose();
            setForeground(fg);
            super.paintComponent(graphics);
        }
    }

    private static final class BieuDoDoanhThu extends JPanel {

        private List<DoanhThuNgay> points = List.of();

        BieuDoDoanhThu() {
            setOpaque(false);
            setPreferredSize(new Dimension(300, 130));
            setMinimumSize(new Dimension(0, 45));
        }

        void setPoints(List<DoanhThuNgay> points) {
            this.points = List.copyOf(points);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int base = getHeight() - 23;
            int max = Math.max(0, base - 24);
            double step = getWidth() / 7.0;
            double highest = points.stream().mapToDouble(p -> p.soTien().doubleValue()).max().orElse(0);
            double scale = Math.max(1, highest * 1.15);
            g.setFont(font(9));
            FontMetrics fm = g.getFontMetrics();
            for (int i = 0; i < points.size(); i++) {
                DoanhThuNgay point = points.get(i);
                String amount = number(point.soTien().divide(BigDecimal.valueOf(1_000_000), 2, RoundingMode.HALF_UP), "0.##");
                int width = Math.max(12, Math.min(24, (int) step - 22));
                int height = (int) (point.soTien().doubleValue() / scale * max);
                int x = (int) (step * i + (step - width) / 2);
                int y = base - height;
                g.setColor(i == 6 ? TEAL_DARK : new Color(0xB6DEDC));
                g.fill(new RoundRectangle2D.Double(x, y, width, height, 2, 2));
                g.setColor(MUTED);
                g.drawString(amount, x + (width - fm.stringWidth(amount)) / 2, Math.max(10, y - 8));
                String date = point.ngay().format(DateTimeFormatter.ofPattern("dd/MM"));
                g.drawString(date, (int) (step * i + (step - fm.stringWidth(date)) / 2), base + 18);
            }
            if (!points.isEmpty() && highest == 0) {
                String empty = "Chưa phát sinh doanh thu";
                g.setFont(font(11));
                g.setColor(MUTED);
                g.drawString(empty, Math.max(2, (getWidth() - g.getFontMetrics().stringWidth(empty)) / 2), Math.max(14, base / 2));
            }
            g.dispose();
        }
    }
}
