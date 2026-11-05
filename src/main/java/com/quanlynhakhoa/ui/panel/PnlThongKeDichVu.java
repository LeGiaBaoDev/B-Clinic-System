package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.dao.ThongKeDichVuDAO;
import com.quanlynhakhoa.dao.ThongKeDichVuDAO.DichVuXepHang;
import com.quanlynhakhoa.dao.ThongKeDichVuDAO.TieuChi;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Thẻ "Dịch vụ được sử dụng nhiều" trên trang Báo cáo (chỉ Quản lý).
 *
 * <p>Bảng tối đa 10 dịch vụ gồm Hạng, Mã, Dịch vụ, Số lượt, Đơn giá, Doanh thu, luôn xếp theo số lượt
 * (bằng nhau thì xét doanh thu). Không có nút đổi tiêu chí vì cột Doanh thu đã hiện sẵn trong bảng.
 * Doanh thu = số tiền khách thực trả cho dịch vụ (gồm thuế, đã trừ voucher chia theo tỷ lệ), cộng các dịch vụ
 * của một hóa đơn ra đúng tổng tiền hóa đơn; chi tiết cách tính xem {@link ThongKeDichVuDAO}.</p>
 *
 * <p>Khoảng ngày do trang Báo cáo truyền vào qua {@link #taiDuLieu(LocalDate, LocalDate)}. Truy vấn chạy nền
 * bằng SwingWorker. Khi bảng trống (đang tải, không có dữ liệu, lỗi), thông báo hiện ngay giữa bảng.</p>
 */
public class PnlThongKeDichVu extends JPanel {

    /** Nguồn dữ liệu xếp hạng; tách ra để test không cần DB. */
    @FunctionalInterface
    interface XepHang {
        List<DichVuXepHang> xepHang(LocalDate tu, LocalDate den, TieuChi tieuChi);
    }

    static final int COT_HANG = 0, COT_MA = 1, COT_TEN = 2, COT_SO_LUOT = 3, COT_DON_GIA = 4, COT_DOANH_THU = 5;
    private static final Logger LOG = Logger.getLogger(PnlThongKeDichVu.class.getName());
    private static final Color DO = new Color(0xC94C48);
    private static final Color NEN_HEADER = new Color(0xF7F9FB);

    private final XepHang nguon;
    /** Thông báo tình trạng: vẽ giữa bảng khi bảng trống, hiện dưới bảng khi đã có dòng. */
    final JLabel lblThongBao = new JLabel(" ");
    final DefaultTableModel model = new DefaultTableModel(new String[]{
        "HẠNG", "MÃ", "DỊCH VỤ", "SỐ LƯỢT", "ĐƠN GIÁ", "DOANH THU"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    final JTable tblDichVu = new JTable(model) {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getRowCount() == 0) {
                veThongBaoGiuaBang(this, g);
            }
        }
    };

    private LocalDate tu;
    private LocalDate den;
    private SwingWorker<List<DichVuXepHang>, Void> worker;

    public PnlThongKeDichVu() {
        this(new ThongKeDichVuDAO()::xepHang);
    }

    PnlThongKeDichVu(XepHang nguon) {
        super(new BorderLayout());
        this.nguon = Objects.requireNonNull(nguon);
        setName("theThongKeDichVu");
        setOpaque(false);
        SurfacePanel the = new SurfacePanel(new BorderLayout(0, 12));
        the.setBorder(new EmptyBorder(16, 18, 16, 18));
        the.add(taoTieuDe(), BorderLayout.NORTH);
        the.add(taoBang(), BorderLayout.CENTER);
        lblThongBao.setName("thongBaoDichVu");
        lblThongBao.setFont(font(12));
        lblThongBao.setForeground(MUTED);
        lblThongBao.setVisible(false); // chỉ hiện dưới bảng khi bảng có dòng
        the.add(lblThongBao, BorderLayout.SOUTH);
        add(the, BorderLayout.CENTER);
    }

    private JLabel taoTieuDe() {
        JLabel tieuDe = new JLabel("Dịch vụ được sử dụng nhiều");
        tieuDe.setFont(bold(16));
        tieuDe.setForeground(TEXT);
        return tieuDe;
    }

    private JScrollPane taoBang() {
        tblDichVu.setName("bangThongKeDichVu");
        tblDichVu.setFont(font(13));
        tblDichVu.setRowHeight(32);
        tblDichVu.setShowVerticalLines(false);
        tblDichVu.setGridColor(BORDER);
        tblDichVu.setFillsViewportHeight(true);
        tblDichVu.setFocusable(false);
        tblDichVu.setRowSelectionAllowed(false);
        tblDichVu.getTableHeader().setReorderingAllowed(false);
        tblDichVu.getTableHeader().setPreferredSize(new Dimension(0, 36));
        tblDichVu.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                setFont(bold(11));
                setForeground(MUTED);
                setBackground(NEN_HEADER);
                setOpaque(true);
                setHorizontalAlignment(laCotSo(c) ? RIGHT : LEFT);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(0, 12, 0, 12)));
                return this;
            }
        });
        tblDichVu.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                setHorizontalAlignment(laCotSo(c) ? RIGHT : LEFT); // số căn phải cho dễ so sánh
                setBorder(new EmptyBorder(0, 12, 0, 12));
                setToolTipText(v == null ? null : v.toString());
                return this;
            }
        });
        int[] rong = {55, 75, 240, 80, 115, 125};
        for (int i = 0; i < rong.length; i++) {
            tblDichVu.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        }
        JScrollPane cuon = new JScrollPane(tblDichVu);
        cuon.setBorder(BorderFactory.createLineBorder(BORDER));
        cuon.getViewport().setBackground(SURFACE);
        // Đủ chỗ cho 10 dòng (tối đa của báo cáo) mà không phải cuộn.
        cuon.setPreferredSize(new Dimension(0, 36 + 32 * ThongKeDichVuDAO.GIOI_HAN_MAC_DINH + 4));
        return cuon;
    }

    private static boolean laCotSo(int cot) {
        return cot == COT_HANG || cot == COT_SO_LUOT || cot == COT_DON_GIA || cot == COT_DOANH_THU;
    }

    /**
     * Đặt thông báo tình trạng. Bảng trống thì thông báo được vẽ giữa bảng (xem {@link #veThongBaoGiuaBang});
     * bảng có dòng thì hiện ở dòng chữ nhỏ dưới bảng.
     */
    private void baoTinhTrang(String text, Color mau) {
        lblThongBao.setText(text);
        lblThongBao.setForeground(mau);
        lblThongBao.setVisible(model.getRowCount() > 0);
        tblDichVu.repaint();
    }

    /** Vẽ nội dung {@link #lblThongBao} vào giữa vùng bảng (bảng đang trống). */
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
        int x = (bang.getWidth() - fm.stringWidth(text)) / 2;
        // Canh giữa theo chiều ngang; theo chiều dọc đặt gần đầu bảng (khung cao đủ 10 dòng nên giữa thì quá thấp).
        int y = Math.min((bang.getHeight() - fm.getHeight()) / 2, 56) + fm.getAscent();
        g2.drawString(text, Math.max(8, x), y);
        g2.dispose();
    }

    /** Trang Báo cáo gọi khi đổi khoảng ngày. */
    public void taiDuLieu(LocalDate tu, LocalDate den) {
        this.tu = tu;
        this.den = den;
        taiLai();
    }

    /** Trang Báo cáo gọi khi ngày chọn sai (thiếu ngày, ngày tương lai...): xóa bảng, hiện thông báo đỏ. */
    public void baoLoi(String thongBao) {
        if (worker != null) {
            worker.cancel(true);
            worker = null;
        }
        tu = null;
        den = null;
        model.setRowCount(0);
        baoTinhTrang(thongBao, DO);
    }

    /** Tải lại với khoảng ngày hiện tại (chạy nền). */
    private void taiLai() {
        if (tu == null || den == null) {
            return; // trang chưa truyền khoảng ngày
        }
        if (worker != null) {
            worker.cancel(true); // bỏ kết quả lần trước nếu chưa xong
        }
        LocalDate tuNgay = tu;
        LocalDate denNgay = den;
        baoTinhTrang("Đang tải thống kê dịch vụ...", MUTED);
        worker = new SwingWorker<>() {
            @Override
            protected List<DichVuXepHang> doInBackground() {
                return List.copyOf(nguon.xepHang(tuNgay, denNgay, TieuChi.SO_LUOT));
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
                    model.setRowCount(0);
                    Throwable cause = ex.getCause();
                    if (cause instanceof BusinessException) {
                        baoTinhTrang(cause.getMessage(), DO); // vd khoảng thời gian không hợp lệ
                    } else {
                        LOG.log(Level.WARNING, "Không thể tải thống kê dịch vụ", cause);
                        baoTinhTrang("Không thể tải thống kê dịch vụ. Vui lòng thử lại.", DO);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        worker.execute();
    }

    private void hienThi(List<DichVuXepHang> ds) {
        model.setRowCount(0);
        for (DichVuXepHang d : ds) {
            model.addRow(new Object[]{d.hang(), d.maDV(), d.tenDV(), d.soLuot(), tien(d.donGia()), tien(d.doanhThu())});
        }
        baoTinhTrang(ds.isEmpty() ? "Không có dữ liệu."
                : "Top " + ds.size() + " dịch vụ · xếp theo số lượt sử dụng · doanh thu = tiền khách trả (gồm thuế, đã trừ voucher)",
                MUTED);
    }

    @Override
    public void removeNotify() {
        if (worker != null) {
            worker.cancel(true);
            worker = null;
        }
        super.removeNotify();
    }

    static String tien(BigDecimal soTien) {
        DecimalFormat f = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.of("vi", "VN")));
        return f.format(soTien == null ? BigDecimal.ZERO : soTien) + " ₫";
    }
}
