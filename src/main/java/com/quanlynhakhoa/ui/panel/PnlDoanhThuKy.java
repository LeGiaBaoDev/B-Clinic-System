package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO;
import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.DoanhThuMotNgay;
import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.TongHopDoanhThu;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import com.quanlynhakhoa.ui.panel.PnlBaoCao.KhoangNgay;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Phần doanh thu theo thời gian trên trang Báo cáo của Quản lý: 3 thẻ dùng chung một lần tải dữ liệu.
 * <ul>
 *   <li>{@link #theChiSo} "Doanh thu đã thu": doanh thu thực nhận kỳ này, % tăng/giảm so với kỳ trước.</li>
 *   <li>{@link #theBieuDo} "Doanh thu theo ngày": biểu đồ cột kỳ này cạnh kỳ trước. Kỳ dài thì gộp cột
 *       theo tuần (trên 31 ngày) hoặc theo tháng (trên 120 ngày, vd kỳ Năm) để biểu đồ không quá dày.</li>
 *   <li>{@link #theChiTiet} "Chi tiết doanh thu": Số hóa đơn, Tiền hàng, Thuế GTGT, Giảm giá voucher,
 *       Doanh thu thực nhận, Tiền mặt, Chuyển khoản — kỳ trước, kỳ này và % tăng/giảm từng dòng.</li>
 * </ul>
 * Số liệu chỉ tính hóa đơn đã thanh toán, theo ngày thanh toán; cách tính xem
 * {@link ThongKeDoanhThuDAO#tongHopDoanhThu}. Trang Báo cáo gọi {@link #taiDuLieu} mỗi khi đổi kỳ.
 */
public class PnlDoanhThuKy {

    /** Nguồn dữ liệu; tách ra để test không cần DB. */
    interface Nguon {
        TongHopDoanhThu tongHop(LocalDate tu, LocalDate den);

        List<DoanhThuMotNgay> tungNgay(LocalDate tu, LocalDate den);
    }

    /** Kết quả một lần tải: tổng hợp và doanh thu từng ngày của 2 kỳ. */
    private record DuLieu(TongHopDoanhThu nay, TongHopDoanhThu truoc,
            List<DoanhThuMotNgay> ngayNay, List<DoanhThuMotNgay> ngayTruoc) {
    }

    /** Một nhóm cột trên biểu đồ: nhãn trục ngang, doanh thu kỳ này và kỳ trước. */
    record Cot(String nhan, BigDecimal nay, BigDecimal truoc) {
    }

    private static final Logger LOG = Logger.getLogger(PnlDoanhThuKy.class.getName());
    private static final Color XANH_NHAT = new Color(0xB7DEDB);
    private static final Color DO = new Color(0xC94C48);
    private static final Color NEN_HEADER = new Color(0xF7F9FB);
    private static final DateTimeFormatter NGAY_NGAN = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter THANG = DateTimeFormatter.ofPattern("MM/yyyy");
    /** Tên ngắn để vừa cột hẹp; "Thực nhận" = doanh thu thực nhận (tiền khách trả). */
    static final String[] CHI_TIEU = {"Số hóa đơn", "Tiền hàng", "Thuế GTGT", "Giảm voucher",
        "Thực nhận", "Tiền mặt", "Chuyển khoản"};
    static final int COT_KY_TRUOC = 1, COT_KY_NAY = 2, COT_THAY_DOI = 3;
    private static final int DONG_THUC_NHAN = 4;

    private final Nguon nguon;
    final JPanel theChiSo;
    final JPanel theBieuDo;
    final JPanel theChiTiet;
    final JLabel lblDoanhThu = new JLabel("—");
    final JLabel lblSoVoiKyTruoc = new JLabel(" ");
    final JLabel lblChuThichNay = new JLabel();
    final JLabel lblChuThichTruoc = new JLabel();
    final BieuDoCot bieuDo = new BieuDoCot();
    final DefaultTableModel model = new DefaultTableModel(new String[]{"CHỈ TIÊU", "KỲ TRƯỚC", "KỲ NÀY", "+/−"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    final JLabel lblThongBaoChiTiet = new JLabel(" ");

    private SwingWorker<DuLieu, Void> worker;

    public PnlDoanhThuKy() {
        this(taoNguonDao());
    }

    PnlDoanhThuKy(Nguon nguon) {
        this.nguon = Objects.requireNonNull(nguon);
        theChiSo = taoTheChiSo();
        theBieuDo = taoTheBieuDo();
        theChiTiet = taoTheChiTiet();
    }

    private static Nguon taoNguonDao() {
        ThongKeDoanhThuDAO dao = new ThongKeDoanhThuDAO();
        return new Nguon() {
            @Override
            public TongHopDoanhThu tongHop(LocalDate tu, LocalDate den) {
                return dao.tongHopDoanhThu(tu, den);
            }

            @Override
            public List<DoanhThuMotNgay> tungNgay(LocalDate tu, LocalDate den) {
                return dao.doanhThuTungNgay(tu, den);
            }
        };
    }

    // ===================== Dựng 3 thẻ =====================

    private JPanel taoTheChiSo() {
        SurfacePanel the = new SurfacePanel(new BorderLayout(0, 8));
        the.setName("theDoanhThuDaThu");
        the.setBorder(new EmptyBorder(14, 16, 14, 16));
        JPanel dong = new JPanel(new BorderLayout());
        dong.setOpaque(false);
        dong.add(chu("Doanh thu đã thu", font(12), MUTED), BorderLayout.WEST);
        dong.add(new JLabel(new LineIcon(LineIcon.Type.CHART, TEAL, 18)), BorderLayout.EAST);
        the.add(dong, BorderLayout.NORTH);
        lblDoanhThu.setFont(bold(26));
        lblDoanhThu.setForeground(TEXT);
        the.add(lblDoanhThu, BorderLayout.CENTER);
        lblSoVoiKyTruoc.setFont(font(11));
        lblSoVoiKyTruoc.setForeground(MUTED);
        the.add(lblSoVoiKyTruoc, BorderLayout.SOUTH);
        return the;
    }

    private JPanel taoTheBieuDo() {
        SurfacePanel the = new SurfacePanel(new BorderLayout(0, 8));
        the.setName("theDoanhThuTheoNgay");
        the.setBorder(new EmptyBorder(16, 18, 12, 18));
        JPanel dau = new JPanel(new BorderLayout());
        dau.setOpaque(false);
        dau.add(chu("Doanh thu theo ngày", bold(16), TEXT), BorderLayout.WEST);
        dau.add(chu("Đơn vị: triệu đồng", font(11), MUTED), BorderLayout.EAST);
        JPanel chuThich = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        chuThich.setOpaque(false);
        lblChuThichNay.setIcon(oVuong(TEAL));
        lblChuThichTruoc.setIcon(oVuong(XANH_NHAT));
        for (JLabel l : new JLabel[]{lblChuThichNay, lblChuThichTruoc}) {
            l.setFont(font(11));
            l.setForeground(MUTED);
            l.setIconTextGap(6);
            chuThich.add(l);
        }
        JPanel tren = new JPanel(new BorderLayout(0, 6));
        tren.setOpaque(false);
        tren.add(dau, BorderLayout.NORTH);
        tren.add(chuThich, BorderLayout.SOUTH);
        the.add(tren, BorderLayout.NORTH);
        bieuDo.setPreferredSize(new Dimension(0, 170));
        the.add(bieuDo, BorderLayout.CENTER);
        return the;
    }

    private JPanel taoTheChiTiet() {
        SurfacePanel the = new SurfacePanel(new BorderLayout(0, 12));
        the.setName("theChiTietDoanhThu");
        the.setBorder(new EmptyBorder(16, 18, 16, 18));
        JPanel dau = new JPanel(new BorderLayout());
        dau.setOpaque(false);
        dau.add(chu("Chi tiết doanh thu", bold(16), TEXT), BorderLayout.WEST);
        dau.add(chu("Đơn vị: đồng", font(11), MUTED), BorderLayout.EAST);
        the.add(dau, BorderLayout.NORTH);

        JTable bang = new JTable(model);
        bang.setName("bangChiTietDoanhThu");
        bang.setFont(font(12));
        bang.setRowHeight(34);
        bang.setShowVerticalLines(false);
        bang.setGridColor(BORDER);
        bang.setFocusable(false);
        bang.setRowSelectionAllowed(false);
        bang.getTableHeader().setReorderingAllowed(false);
        bang.getTableHeader().setResizingAllowed(false);
        bang.getTableHeader().setPreferredSize(new Dimension(0, 34));
        bang.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                setFont(bold(11));
                setForeground(MUTED);
                setBackground(NEN_HEADER);
                setOpaque(true);
                setHorizontalAlignment(c == 0 ? LEFT : RIGHT);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(0, 8, 0, 8)));
                return this;
            }
        });
        bang.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                setHorizontalAlignment(c == 0 ? LEFT : RIGHT);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                setFont(r == DONG_THUC_NHAN ? bold(12) : font(12)); // dòng thực nhận in đậm
                String text = v == null ? "" : v.toString();
                setForeground(c != COT_THAY_DOI ? TEXT
                        : text.startsWith("+") ? GREEN : text.startsWith("−") ? DO : MUTED);
                setToolTipText(text.isEmpty() ? null : text);
                return this;
            }
        });
        int[] rong = {96, 86, 86, 74};
        for (int i = 0; i < rong.length; i++) {
            bang.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        }
        for (String ct : CHI_TIEU) {
            model.addRow(new Object[]{ct, "—", "—", "—"});
        }
        JScrollPane cuon = new JScrollPane(bang);
        cuon.setBorder(BorderFactory.createLineBorder(BORDER));
        cuon.getViewport().setBackground(SURFACE);
        cuon.setPreferredSize(new Dimension(0, 34 + 34 * CHI_TIEU.length + 3));
        JPanel giua = new JPanel(new BorderLayout(0, 10));
        giua.setOpaque(false);
        giua.add(cuon, BorderLayout.NORTH);
        lblThongBaoChiTiet.setName("thongBaoChiTietDoanhThu");
        lblThongBaoChiTiet.setFont(font(11));
        lblThongBaoChiTiet.setForeground(MUTED);
        giua.add(lblThongBaoChiTiet, BorderLayout.CENTER);
        JPanel giuTren = new JPanel(new BorderLayout());
        giuTren.setOpaque(false);
        giuTren.add(giua, BorderLayout.NORTH); // thẻ cao hơn bảng thì bảng vẫn nằm trên
        the.add(giuTren, BorderLayout.CENTER);
        return the;
    }

    // ===================== Tải dữ liệu =====================

    /** Trang Báo cáo gọi khi đổi kỳ: tải 2 kỳ chạy nền rồi cập nhật cả 3 thẻ. */
    public void taiDuLieu(KhoangNgay nay, KhoangNgay truoc) {
        if (worker != null) {
            worker.cancel(true); // bỏ kết quả lần trước nếu chưa xong
        }
        lblChuThichNay.setText(nhanKhoang(nay));
        lblChuThichTruoc.setText(nhanKhoang(truoc));
        lblThongBaoChiTiet.setForeground(MUTED);
        lblThongBaoChiTiet.setText("Đang tải doanh thu...");
        worker = new SwingWorker<>() {
            @Override
            protected DuLieu doInBackground() {
                return new DuLieu(nguon.tongHop(nay.tu(), nay.den()), nguon.tongHop(truoc.tu(), truoc.den()),
                        List.copyOf(nguon.tungNgay(nay.tu(), nay.den())),
                        List.copyOf(nguon.tungNgay(truoc.tu(), truoc.den())));
            }

            @Override
            protected void done() {
                if (this != worker || isCancelled()) {
                    return;
                }
                worker = null;
                try {
                    hienThi(get(), nay);
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof BusinessException) {
                        baoLoi(cause.getMessage());
                    } else {
                        LOG.log(Level.WARNING, "Không thể tải doanh thu", cause);
                        baoLoi("Không thể tải doanh thu. Vui lòng thử lại.");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        worker.execute();
    }

    /** Khoảng ngày sai hoặc lỗi tải: xóa số liệu, hiện thông báo ở thẻ chi tiết và biểu đồ. */
    public void baoLoi(String thongBao) {
        if (worker != null) {
            worker.cancel(true);
            worker = null;
        }
        lblDoanhThu.setText("—");
        lblSoVoiKyTruoc.setForeground(MUTED);
        lblSoVoiKyTruoc.setText(" ");
        for (int r = 0; r < CHI_TIEU.length; r++) {
            for (int c = COT_KY_TRUOC; c <= COT_THAY_DOI; c++) {
                model.setValueAt("—", r, c);
            }
        }
        lblThongBaoChiTiet.setForeground(DO);
        lblThongBaoChiTiet.setText(thongBao);
        bieuDo.datDuLieu(List.of(), thongBao);
    }

    private void hienThi(DuLieu d, KhoangNgay nay) {
        TongHopDoanhThu a = d.nay();
        TongHopDoanhThu b = d.truoc();
        lblDoanhThu.setText(PnlThongKeDichVu.tien(a.thucNhan()));
        String phanTram = thayDoi(b.thucNhan(), a.thucNhan());
        lblSoVoiKyTruoc.setText((phanTram.equals("—") ? "" : muiTen(phanTram) + " " + phanTram.substring(1) + " · ")
                + "Kỳ trước: " + PnlThongKeDichVu.tien(b.thucNhan()));
        lblSoVoiKyTruoc.setForeground(phanTram.startsWith("+") ? GREEN : phanTram.startsWith("−") ? DO : MUTED);

        List<Function<TongHopDoanhThu, BigDecimal>> lay = List.of(
                t -> BigDecimal.valueOf(t.soHoaDon()), TongHopDoanhThu::tienHang, TongHopDoanhThu::thueGTGT,
                TongHopDoanhThu::giamGia, TongHopDoanhThu::thucNhan, TongHopDoanhThu::tienMat,
                TongHopDoanhThu::chuyenKhoan);
        for (int r = 0; r < lay.size(); r++) {
            BigDecimal truoc = lay.get(r).apply(b);
            BigDecimal sau = lay.get(r).apply(a);
            model.setValueAt(so(truoc), r, COT_KY_TRUOC);
            model.setValueAt(so(sau), r, COT_KY_NAY);
            model.setValueAt(thayDoi(truoc, sau), r, COT_THAY_DOI);
        }
        lblThongBaoChiTiet.setForeground(MUTED);
        lblThongBaoChiTiet.setText(a.soHoaDon() == 0 ? "Không có dữ liệu trong kỳ này."
                : "Thực nhận = Tiền hàng + Thuế − Giảm voucher = Tiền mặt + Chuyển khoản.");
        List<Cot> cot = gopCot(d.ngayNay(), d.ngayTruoc(), nay.soNgay());
        boolean coSo = cot.stream().anyMatch(c -> c.nay().signum() != 0 || c.truoc().signum() != 0);
        bieuDo.datDuLieu(cot, coSo ? null : "Không có dữ liệu.");
    }

    // ===================== Tính toán phụ (static để test) =====================

    /**
     * % thay đổi từ kỳ trước sang kỳ này, 1 chữ số thập phân: "+12,4%", "−3,0%", "0,0%".
     * Kỳ trước = 0 thì không tính được % -> "—".
     */
    static String thayDoi(BigDecimal truoc, BigDecimal sau) {
        if (truoc == null || sau == null || truoc.signum() == 0) {
            return "—";
        }
        BigDecimal pt = sau.subtract(truoc).multiply(BigDecimal.valueOf(100))
                .divide(truoc.abs(), 1, RoundingMode.HALF_UP);
        DecimalFormat f = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.of("vi", "VN")));
        String chu = f.format(pt.abs()) + "%";
        return pt.signum() > 0 ? "+" + chu : pt.signum() < 0 ? "−" + chu : chu;
    }

    private static String muiTen(String phanTram) {
        return phanTram.startsWith("+") ? "↑" : "↓";
    }

    /**
     * Gộp doanh thu từng ngày thành các cột biểu đồ. Kỳ này và kỳ trước ghép theo thứ tự (ngày thứ 1 với
     * ngày thứ 1...). Cách gộp theo độ dài kỳ này: tới 31 ngày -> mỗi ngày 1 cột; tới 120 ngày -> mỗi 7 ngày
     * 1 cột; dài hơn -> mỗi tháng dương lịch 1 cột. Thiếu ngày ở kỳ trước thì tính 0.
     */
    static List<Cot> gopCot(List<DoanhThuMotNgay> nay, List<DoanhThuMotNgay> truoc, long soNgay) {
        List<Cot> kq = new ArrayList<>();
        if (nay.isEmpty()) {
            return kq;
        }
        if (soNgay <= 31) {
            for (int i = 0; i < nay.size(); i++) {
                kq.add(new Cot(nay.get(i).ngay().format(NGAY_NGAN), nay.get(i).thucNhan(), lay(truoc, i)));
            }
        } else if (soNgay <= 120) {
            for (int i = 0; i < nay.size(); i += 7) {
                int het = Math.min(i + 7, nay.size());
                kq.add(new Cot(nay.get(i).ngay().format(NGAY_NGAN), cong(nay, i, het), cong(truoc, i, het)));
            }
        } else {
            List<int[]> thangNay = chiaThang(nay);
            List<int[]> thangTruoc = chiaThang(truoc);
            for (int k = 0; k < thangNay.size(); k++) {
                int[] t = thangNay.get(k);
                BigDecimal kyTruoc = k < thangTruoc.size()
                        ? cong(truoc, thangTruoc.get(k)[0], thangTruoc.get(k)[1]) : BigDecimal.ZERO;
                kq.add(new Cot(nay.get(t[0]).ngay().format(THANG), cong(nay, t[0], t[1]), kyTruoc));
            }
        }
        return kq;
    }

    /** Chia danh sách ngày thành các đoạn [đầu, cuối) theo tháng dương lịch. */
    private static List<int[]> chiaThang(List<DoanhThuMotNgay> ds) {
        List<int[]> doan = new ArrayList<>();
        int dau = 0;
        for (int i = 1; i <= ds.size(); i++) {
            if (i == ds.size() || ds.get(i).ngay().getMonthValue() != ds.get(dau).ngay().getMonthValue()) {
                doan.add(new int[]{dau, i});
                dau = i;
            }
        }
        return doan;
    }

    private static BigDecimal lay(List<DoanhThuMotNgay> ds, int i) {
        return i < ds.size() ? ds.get(i).thucNhan() : BigDecimal.ZERO;
    }

    private static BigDecimal cong(List<DoanhThuMotNgay> ds, int tu, int den) {
        BigDecimal tong = BigDecimal.ZERO;
        for (int i = tu; i < Math.min(den, ds.size()); i++) {
            tong = tong.add(ds.get(i).thucNhan());
        }
        return tong;
    }

    /** "12/10 – 16/10/2026"; một ngày thì "16/10/2026". */
    static String nhanKhoang(KhoangNgay k) {
        DateTimeFormatter day = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return k.tu().equals(k.den()) ? k.tu().format(day) : k.tu().format(NGAY_NGAN) + " – " + k.den().format(day);
    }

    /** Số có dấu chấm ngăn cách hàng nghìn, không kèm "₫" (bảng hẹp, đã ghi "Đơn vị: đồng"). */
    private static String so(BigDecimal giaTri) {
        DecimalFormat f = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.of("vi", "VN")));
        return f.format(giaTri == null ? BigDecimal.ZERO : giaTri);
    }

    private static JLabel chu(String text, Font f, Color mau) {
        JLabel l = new JLabel(text);
        l.setFont(f);
        l.setForeground(mau);
        return l;
    }

    private static Icon oVuong(Color mau) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(mau);
                g.fillRect(x, y + 1, 8, 8);
            }

            @Override
            public int getIconWidth() {
                return 8;
            }

            @Override
            public int getIconHeight() {
                return 10;
            }
        };
    }

    // ===================== Biểu đồ cột =====================

    /**
     * Biểu đồ cột đôi tự vẽ (dự án không dùng thư viện biểu đồ): mỗi nhóm có cột kỳ trước (xanh nhạt) và kỳ
     * này (teal), giá trị theo triệu đồng. Trục dọc 3 mốc 0 / giữa / đỉnh làm tròn lên số đẹp.
     */
    static class BieuDoCot extends JComponent {

        private List<Cot> cot = List.of();
        private String thongBao = "Đang tải...";

        void datDuLieu(List<Cot> cot, String thongBao) {
            this.cot = List.copyOf(cot);
            this.thongBao = thongBao;
            repaint();
        }

        List<Cot> cot() {
            return cot;
        }

        String thongBao() {
            return thongBao;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            if (thongBao != null || cot.isEmpty()) {
                g.setFont(font(12));
                g.setColor(MUTED);
                String t = thongBao == null ? "Không có dữ liệu." : thongBao;
                FontMetrics fm = g.getFontMetrics();
                g.drawString(t, (w - fm.stringWidth(t)) / 2, h / 2);
                g.dispose();
                return;
            }
            double max = 0;
            for (Cot c : cot) {
                max = Math.max(max, Math.max(trieu(c.nay()), trieu(c.truoc())));
            }
            double dinh = soDep(max);
            int trai = 30, duoi = 22, tren = 16;
            int caoVe = h - duoi - tren;
            g.setFont(font(10));
            FontMetrics fm = g.getFontMetrics();
            // Trục dọc: 0, giữa, đỉnh
            for (int i = 0; i <= 2; i++) {
                double v = dinh * i / 2;
                int y = tren + caoVe - (int) Math.round(caoVe * v / dinh);
                g.setColor(BORDER);
                g.drawLine(trai, y, w, y);
                g.setColor(MUTED);
                String nhan = soGon(v);
                g.drawString(nhan, trai - 6 - fm.stringWidth(nhan), y + fm.getAscent() / 2 - 1);
            }
            int n = cot.size();
            double rongNhom = (w - trai - 8) / (double) n;
            int rongCot = (int) Math.max(3, Math.min(22, rongNhom * 0.32));
            int buocNhan = Math.max(1, (int) Math.ceil(n / Math.max(1.0, (w - trai) / 46.0))); // nhãn không chồng
            boolean ghiGiaTri = n <= 14;
            for (int i = 0; i < n; i++) {
                Cot c = cot.get(i);
                int giua = trai + 4 + (int) Math.round(rongNhom * i + rongNhom / 2);
                int hTruoc = (int) Math.round(caoVe * trieu(c.truoc()) / dinh);
                int hNay = (int) Math.round(caoVe * trieu(c.nay()) / dinh);
                g.setColor(XANH_NHAT);
                g.fillRect(giua - rongCot - 1, tren + caoVe - hTruoc, rongCot, hTruoc);
                g.setColor(TEAL);
                g.fillRect(giua + 1, tren + caoVe - hNay, rongCot, hNay);
                if (ghiGiaTri && c.nay().signum() != 0) {
                    String gt = soGon(trieu(c.nay()));
                    g.setColor(TEAL_DARK);
                    g.setFont(bold(10));
                    g.drawString(gt, giua + 1 + rongCot / 2 - g.getFontMetrics().stringWidth(gt) / 2,
                            tren + caoVe - hNay - 4);
                    g.setFont(font(10));
                }
                if (i % buocNhan == 0) {
                    g.setColor(MUTED);
                    g.drawString(c.nhan(), giua - fm.stringWidth(c.nhan()) / 2, h - 6);
                }
            }
            g.dispose();
        }

        private static double trieu(BigDecimal dong) {
            return dong == null ? 0 : dong.doubleValue() / 1_000_000d;
        }

        /** Làm tròn lên 1, 2, 2,5, 5 × 10^k để trục dọc dễ đọc. */
        static double soDep(double v) {
            if (v <= 0) {
                return 1;
            }
            double mu = Math.pow(10, Math.floor(Math.log10(v)));
            for (double b : new double[]{1, 2, 2.5, 5, 10}) {
                if (v <= b * mu) {
                    return b * mu;
                }
            }
            return 10 * mu;
        }

        /** 13.8 -> "13,80"; 20 -> "20"; 0.15 -> "0,15". */
        private static String soGon(double v) {
            DecimalFormatSymbols vi = DecimalFormatSymbols.getInstance(Locale.of("vi", "VN"));
            return v == Math.rint(v) ? new DecimalFormat("#,##0", vi).format(v) : new DecimalFormat("#,##0.00", vi).format(v);
        }
    }
}
