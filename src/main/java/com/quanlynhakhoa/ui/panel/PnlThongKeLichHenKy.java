package com.quanlynhakhoa.ui.panel;

import static com.quanlynhakhoa.ui.component.UiConstants.BORDER;
import static com.quanlynhakhoa.ui.component.UiConstants.GREEN;
import static com.quanlynhakhoa.ui.component.UiConstants.MUTED;
import static com.quanlynhakhoa.ui.component.UiConstants.SURFACE;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL;
import static com.quanlynhakhoa.ui.component.UiConstants.TEXT;
import static com.quanlynhakhoa.ui.component.UiConstants.bold;
import static com.quanlynhakhoa.ui.component.UiConstants.font;

import com.quanlynhakhoa.dao.NhanVienDAO;
import com.quanlynhakhoa.dao.ThongKeLichHenDAO;
import com.quanlynhakhoa.dao.ThongKeLichHenDAO.TongHop;
import com.quanlynhakhoa.entity.NhanVien;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import com.quanlynhakhoa.ui.panel.PnlBaoCao.KhoangNgay;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

/**
 * UC029 – phần "lịch hẹn" của trang Báo cáo hoạt động (Quản lý): ba thẻ gắn vào các ô
 * {@code oLichHoanTat}, {@code oHuyKhongDen}, {@code oKetQuaLichHen} của {@link PnlBaoCao}.
 *
 * <ul>
 *   <li>{@link #theHoanTat}: số lịch hoàn tất, tỷ lệ chuyển đổi (hoàn tất / tổng) và so với kỳ trước.</li>
 *   <li>{@link #theHuyKhongDen}: số lịch đã hủy + không đến, chi tiết từng loại và so với kỳ trước.</li>
 *   <li>{@link #theKetQua}: biểu đồ tròn phân bố trạng thái (JFreeChart), chú thích số lượng và tỷ lệ;
 *       có ô lọc theo nha sĩ.</li>
 * </ul>
 * Trang Báo cáo gọi {@link #taiDuLieu} mỗi khi đổi kỳ. Chỉ đọc, không thay đổi dữ liệu.
 */
public class PnlThongKeLichHenKy {

    /** Nguồn dữ liệu; tách ra để test không cần DB. */
    interface Nguon {

        TongHop tongHop(java.time.LocalDate tu, java.time.LocalDate den, String maNhaSi);

        List<NhanVien> danhSachNhaSi();
    }

    /** Nguồn rỗng cho bản dựng trang không có DB (test các phần khác của trang Báo cáo). */
    static final Nguon NGUON_RONG = new Nguon() {
        @Override
        public TongHop tongHop(java.time.LocalDate tu, java.time.LocalDate den, String maNhaSi) {
            return new TongHop(0, 0, 0, 0, 0, 0);
        }

        @Override
        public List<NhanVien> danhSachNhaSi() {
            return List.of();
        }
    };

    /** Mục trong ô lọc nha sĩ; {@code ma == null} là "Tất cả nha sĩ". */
    record NhaSiMuc(String ma, String ten) {

        @Override
        public String toString() {
            return ten;
        }
    }

    private record DuLieu(TongHop nay, TongHop truoc) {

    }

    static final String TAT_CA = "Tất cả nha sĩ";
    static final String KHONG_CO_DU_LIEU = "Không có dữ liệu trong kỳ này.";
    static final String[] TRANG_THAI = {"Hoàn tất", "Đã đặt", "Đã đến", "Đã hủy", "Không đến"};

    private static final Logger LOG = Logger.getLogger(PnlThongKeLichHenKy.class.getName());
    private static final Color VANG = new Color(0x9A6A12);
    private static final Color DO = new Color(0xC94C48);
    private static final Color XANH_DUONG = new Color(0x2F6FA8);
    private static final Color XANH_NHAT = new Color(0x8FB8DE);
    private static final Color[] MAU = {TEAL, XANH_DUONG, XANH_NHAT, VANG, DO};
    private static final Locale VI = Locale.forLanguageTag("vi-VN");

    private final Nguon nguon;
    final JPanel theHoanTat;
    final JPanel theHuyKhongDen;
    final JPanel theKetQua;

    final JLabel lblHoanTat = new JLabel("—");
    final JLabel lblChuyenDoi = new JLabel(" ");
    final JLabel lblHoanTatSoVoiTruoc = new JLabel(" ");
    final JLabel lblHuyKhongDen = new JLabel("—");
    final JLabel lblChiTietHuy = new JLabel(" ");
    final JLabel lblHuySoVoiTruoc = new JLabel(" ");
    final JLabel lblTongDaKetThuc = new JLabel("—");
    final JLabel lblThongBao = new JLabel(" ");
    final JLabel[] lblSoLuong = new JLabel[TRANG_THAI.length];
    final JComboBox<NhaSiMuc> cboNhaSi = new JComboBox<>();
    final DefaultPieDataset<String> duLieuBieuDo = new DefaultPieDataset<>();
    final ChartPanel bieuDo;

    private KhoangNgay khoangNay;
    private KhoangNgay khoangTruoc;
    private boolean daNapNhaSi;
    private boolean dangNapCombo;
    private SwingWorker<DuLieu, Void> worker;

    public PnlThongKeLichHenKy() {
        this(taoNguonDao());
    }

    PnlThongKeLichHenKy(Nguon nguon) {
        this.nguon = Objects.requireNonNull(nguon);
        cboNhaSi.addItem(new NhaSiMuc(null, TAT_CA));
        cboNhaSi.addActionListener(e -> {
            if (!dangNapCombo && khoangNay != null) {
                taiLai();
            }
        });
        bieuDo = taoBieuDo();
        theHoanTat = taoTheHoanTat();
        theHuyKhongDen = taoTheHuyKhongDen();
        theKetQua = taoTheKetQua();
    }

    private static Nguon taoNguonDao() {
        ThongKeLichHenDAO dao = new ThongKeLichHenDAO();
        NhanVienDAO nhanVienDAO = new NhanVienDAO();
        return new Nguon() {
            @Override
            public TongHop tongHop(java.time.LocalDate tu, java.time.LocalDate den, String maNhaSi) {
                return dao.tongHop(tu, den, maNhaSi);
            }

            @Override
            public List<NhanVien> danhSachNhaSi() {
                return nhanVienDAO.layNhaSiDangHoatDong();
            }
        };
    }

    // ===================== Dựng 3 thẻ =====================

    private JPanel taoTheHoanTat() {
        lblHoanTat.setName("soLichHoanTat");
        lblChuyenDoi.setName("tyLeChuyenDoi");
        return theChiSo("Lịch hẹn hoàn tất", LineIcon.Type.CHECK, lblHoanTat, lblChuyenDoi, lblHoanTatSoVoiTruoc);
    }

    private JPanel taoTheHuyKhongDen() {
        lblHuyKhongDen.setName("soHuyKhongDen");
        lblChiTietHuy.setName("chiTietHuyKhongDen");
        return theChiSo("Đã hủy / Không đến", LineIcon.Type.CALENDAR, lblHuyKhongDen, lblChiTietHuy,
                lblHuySoVoiTruoc);
    }

    private static JPanel theChiSo(String tieuDe, LineIcon.Type bieuTuong, JLabel giaTri, JLabel dong1,
            JLabel dong2) {
        SurfacePanel the = new SurfacePanel(new BorderLayout(0, 8));
        the.setBorder(new EmptyBorder(14, 16, 14, 16));
        JPanel dong = new JPanel(new BorderLayout());
        dong.setOpaque(false);
        dong.add(chu(tieuDe, font(12), MUTED), BorderLayout.WEST);
        dong.add(new JLabel(new LineIcon(bieuTuong, TEAL, 18)), BorderLayout.EAST);
        the.add(dong, BorderLayout.NORTH);
        giaTri.setFont(bold(26));
        giaTri.setForeground(TEXT);
        the.add(giaTri, BorderLayout.CENTER);
        JPanel duoi = new JPanel();
        duoi.setOpaque(false);
        duoi.setLayout(new BoxLayout(duoi, BoxLayout.Y_AXIS));
        for (JLabel l : new JLabel[]{dong1, dong2}) {
            l.setFont(font(11));
            l.setForeground(MUTED);
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            duoi.add(l);
        }
        the.add(duoi, BorderLayout.SOUTH);
        return the;
    }

    private JPanel taoTheKetQua() {
        SurfacePanel the = new SurfacePanel(new BorderLayout(0, 12));
        the.setName("theKetQuaLichHen");
        the.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel dau = new JPanel(new BorderLayout(12, 0));
        dau.setOpaque(false);
        dau.add(chu("Kết quả lịch hẹn", bold(16), TEXT), BorderLayout.WEST);
        cboNhaSi.setName("locNhaSiLichHen");
        cboNhaSi.setFont(font(12));
        cboNhaSi.setPreferredSize(new Dimension(150, 30));
        dau.add(cboNhaSi, BorderLayout.EAST);
        the.add(dau, BorderLayout.NORTH);

        JPanel giua = new JPanel();
        giua.setOpaque(false);
        giua.setLayout(new BoxLayout(giua, BoxLayout.Y_AXIS));
        lblTongDaKetThuc.setFont(font(12));
        lblTongDaKetThuc.setForeground(MUTED);
        lblTongDaKetThuc.setAlignmentX(Component.LEFT_ALIGNMENT);
        giua.add(lblTongDaKetThuc);
        giua.add(Box.createVerticalStrut(4));
        bieuDo.setAlignmentX(Component.LEFT_ALIGNMENT);
        giua.add(bieuDo);
        giua.add(Box.createVerticalStrut(8));
        for (int i = 0; i < TRANG_THAI.length; i++) {
            JPanel dong = new JPanel(new BorderLayout(8, 0));
            dong.setOpaque(false);
            dong.setAlignmentX(Component.LEFT_ALIGNMENT);
            dong.setBorder(new EmptyBorder(4, 0, 4, 0));
            dong.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            JLabel nhan = chu(TRANG_THAI[i], font(12), MUTED);
            nhan.setIcon(oVuong(MAU[i]));
            nhan.setIconTextGap(8);
            dong.add(nhan, BorderLayout.WEST);
            lblSoLuong[i] = chu("—", bold(12), TEXT);
            lblSoLuong[i].setName("soLuong" + TRANG_THAI[i]);
            dong.add(lblSoLuong[i], BorderLayout.EAST);
            giua.add(dong);
        }
        the.add(giua, BorderLayout.CENTER);
        lblThongBao.setName("thongBaoLichHenKy");
        lblThongBao.setFont(font(11));
        lblThongBao.setForeground(MUTED);
        the.add(lblThongBao, BorderLayout.SOUTH);
        return the;
    }

    /** Biểu đồ tròn phân bố trạng thái; không nhãn trên lát cắt vì đã có chú thích số lượng bên dưới. */
    private ChartPanel taoBieuDo() {
        JFreeChart chart = ChartFactory.createPieChart(null, duLieuBieuDo, false, true, false);
        chart.setBackgroundPaint(SURFACE);
        chart.setBorderVisible(false);
        PiePlot<?> plot = (PiePlot<?>) chart.getPlot();
        plot.setBackgroundPaint(SURFACE);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(null);
        plot.setLabelGenerator(null);
        plot.setInteriorGap(0.04);
        plot.setSectionOutlinesVisible(true);
        plot.setDefaultSectionOutlinePaint(SURFACE);
        plot.setNoDataMessage("Không có dữ liệu");
        plot.setNoDataMessageFont(font(13));
        plot.setNoDataMessagePaint(MUTED);
        for (int i = 0; i < TRANG_THAI.length; i++) {
            setSectionPaint(plot, TRANG_THAI[i], MAU[i]);
        }
        ChartPanel panel = new ChartPanel(chart, false);
        panel.setName("bieuDoTronLichHen");
        panel.setPreferredSize(new Dimension(220, 190));
        panel.setMinimumSize(new Dimension(120, 120));
        panel.setPopupMenu(null);
        panel.setMouseZoomable(false);
        panel.setMouseWheelEnabled(false);
        panel.setOpaque(false);
        return panel;
    }

    @SuppressWarnings("unchecked")
    private static void setSectionPaint(PiePlot<?> plot, String khoa, Color mau) {
        ((PiePlot<String>) plot).setSectionPaint(khoa, mau);
    }

    // ===================== Tải dữ liệu =====================

    /** Trang Báo cáo gọi khi đổi kỳ: tải kỳ này và kỳ trước (theo nha sĩ đang lọc) chạy nền. */
    public void taiDuLieu(KhoangNgay nay, KhoangNgay truoc) {
        khoangNay = nay;
        khoangTruoc = truoc;
        napNhaSi();
        taiLai();
    }

    private void taiLai() {
        if (worker != null) {
            worker.cancel(true); // bỏ kết quả lần trước nếu chưa xong
        }
        KhoangNgay nay = khoangNay;
        KhoangNgay truoc = khoangTruoc;
        NhaSiMuc muc = (NhaSiMuc) cboNhaSi.getSelectedItem();
        String maNhaSi = muc == null ? null : muc.ma();
        baoTinhTrang("Đang tải lịch hẹn...", MUTED);
        worker = new SwingWorker<>() {
            @Override
            protected DuLieu doInBackground() {
                return new DuLieu(nguon.tongHop(nay.tu(), nay.den(), maNhaSi),
                        nguon.tongHop(truoc.tu(), truoc.den(), maNhaSi));
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
                        baoLoi(cause.getMessage());
                    } else {
                        LOG.log(Level.WARNING, "Không thể tải thống kê lịch hẹn", cause);
                        baoLoi("Không thể tải lịch hẹn. Vui lòng thử lại.");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        worker.execute();
    }

    /** Nạp danh sách nha sĩ cho ô lọc một lần; lỗi thì chỉ còn "Tất cả nha sĩ". */
    private void napNhaSi() {
        if (daNapNhaSi) {
            return;
        }
        daNapNhaSi = true;
        new SwingWorker<List<NhanVien>, Void>() {
            @Override
            protected List<NhanVien> doInBackground() {
                return nguon.danhSachNhaSi();
            }

            @Override
            protected void done() {
                try {
                    dangNapCombo = true;
                    for (NhanVien nv : get()) {
                        cboNhaSi.addItem(new NhaSiMuc(nv.getMaNV(), nv.getHoTenNV()));
                    }
                } catch (ExecutionException ex) {
                    LOG.log(Level.WARNING, "Không thể tải danh sách nha sĩ", ex.getCause());
                    daNapNhaSi = false; // thử lại lần đổi kỳ sau
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    dangNapCombo = false;
                }
            }
        }.execute();
    }

    /** Khoảng ngày sai hoặc lỗi tải: xóa số liệu, hiện thông báo ở thẻ kết quả. */
    public void baoLoi(String thongBao) {
        if (worker != null) {
            worker.cancel(true);
            worker = null;
        }
        for (JLabel l : new JLabel[]{lblHoanTat, lblHuyKhongDen, lblTongDaKetThuc}) {
            l.setText("—");
        }
        for (JLabel l : new JLabel[]{lblChuyenDoi, lblHoanTatSoVoiTruoc, lblChiTietHuy, lblHuySoVoiTruoc}) {
            l.setText(" ");
            l.setForeground(MUTED);
        }
        for (JLabel l : lblSoLuong) {
            l.setText("—");
        }
        duLieuBieuDo.clear();
        baoTinhTrang(thongBao, DO);
    }

    private void hienThi(DuLieu d) {
        TongHop a = d.nay();
        TongHop b = d.truoc();

        lblHoanTat.setText(String.valueOf(a.hoanTat()));
        lblChuyenDoi.setText("Chuyển đổi " + phanTram(a.tyLeChuyenDoi()) + " tổng " + a.tong() + " lịch");
        datSoVoiKyTruoc(lblHoanTatSoVoiTruoc, b.hoanTat(), a.hoanTat(), true);

        long huyVaKhongDen = a.daHuy() + a.khongDen();
        lblHuyKhongDen.setText(String.valueOf(huyVaKhongDen));
        lblChiTietHuy.setText("Hủy " + a.daHuy() + " · Không đến " + a.khongDen());
        datSoVoiKyTruoc(lblHuySoVoiTruoc, b.daHuy() + b.khongDen(), huyVaKhongDen, false);

        long[] soLuong = {a.hoanTat(), a.daDat(), a.daDen(), a.daHuy(), a.khongDen()};
        double[] tyLe = {a.tyLeHoanTat(), a.tyLeDaDat(), a.tyLeDaDen(), a.tyLeDaHuy(), a.tyLeKhongDen()};
        duLieuBieuDo.clear();
        for (int i = 0; i < TRANG_THAI.length; i++) {
            lblSoLuong[i].setText(soLuong[i] + " · " + phanTram(tyLe[i]));
            if (soLuong[i] > 0) {
                duLieuBieuDo.setValue(TRANG_THAI[i], soLuong[i]);
            }
        }
        lblTongDaKetThuc.setText(a.tong() + " lịch hẹn trong kỳ");
        baoTinhTrang(a.coDuLieu() ? "Tỷ lệ chuyển đổi = lịch hoàn tất / tổng số lịch." : KHONG_CO_DU_LIEU, MUTED);
    }

    /**
     * Dòng "↑ 12,5% · Kỳ trước: N". {@code tangLaTot} = true thì tăng tô xanh (hoàn tất); false thì tăng tô đỏ
     * (hủy / không đến tăng là xấu).
     */
    private static void datSoVoiKyTruoc(JLabel nhan, long truoc, long nay, boolean tangLaTot) {
        String pt = PnlDoanhThuKy.thayDoi(BigDecimal.valueOf(truoc), BigDecimal.valueOf(nay));
        boolean tang = pt.startsWith("+");
        boolean giam = pt.startsWith("−");
        nhan.setText((pt.equals("—") ? "" : (tang ? "↑ " : giam ? "↓ " : "") + pt.replaceFirst("^[+−]", "") + " · ")
                + "Kỳ trước: " + truoc);
        nhan.setForeground(tang ? (tangLaTot ? GREEN : DO) : giam ? (tangLaTot ? DO : GREEN) : MUTED);
    }

    private void baoTinhTrang(String text, Color mau) {
        lblThongBao.setText(text);
        lblThongBao.setForeground(mau);
    }

    static String phanTram(double v) {
        return String.format(VI, "%.1f%%", v);
    }

    private static JLabel chu(String text, java.awt.Font f, Color mau) {
        JLabel l = new JLabel(text);
        l.setFont(f);
        l.setForeground(mau);
        return l;
    }

    /** Ô vuông màu 8px dùng làm chú thích. */
    private static Icon oVuong(Color mau) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(mau);
                g.fillRect(x, y, 8, 8);
            }

            @Override
            public int getIconWidth() {
                return 8;
            }

            @Override
            public int getIconHeight() {
                return 8;
            }
        };
    }
}
