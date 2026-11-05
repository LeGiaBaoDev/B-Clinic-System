package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO;
import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.DoanhThuMotNgay;
import com.quanlynhakhoa.dao.ThongKeDoanhThuDAO.TongHopDoanhThu;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import com.quanlynhakhoa.ui.panel.PnlBaoCao.KhoangNgay;
import com.quanlynhakhoa.ui.panel.PnlBaoCao.Ky;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Phần doanh thu trên trang Báo cáo của Quản lý: 2 thẻ dùng chung một lần tải dữ liệu.
 * <ul>
 *   <li>{@link #theChiSo} "Doanh thu đã thu": doanh thu thực nhận của ngày / tuần / tháng / năm / khoảng đang xem,
 *       % tăng/giảm so với ngày trước / tuần trước...</li>
 *   <li>{@link #theBieuDo} biểu đồ cột, tiêu đề và cách vẽ đổi theo nút đang chọn:
 *     <ul>
 *       <li>Ngày: 2 cột đậm/nhạt — ngày đang xem cạnh ngày trước đó.</li>
 *       <li>Tuần: 1 màu, 7 cột Thứ 2 → CN của tuần đó.</li>
 *       <li>Tháng: 1 màu, 12 cột Th1 → Th12 của năm chứa tháng đó.</li>
 *       <li>Năm: 1 màu, mỗi năm 1 cột ({@link #SO_NAM_BIEU_DO} năm, tính tới năm đang xem).</li>
 *       <li>Khoảng: 1 màu, mỗi ngày 1 cột (khoảng dài thì gộp theo tuần / tháng).</li>
 *     </ul>
 *   </li>
 * </ul>
 * Số liệu chỉ tính hóa đơn đã thanh toán, theo ngày thanh toán; cách tính xem
 * {@link ThongKeDoanhThuDAO#tongHopDoanhThu}. Trang Báo cáo gọi {@link #taiDuLieu} mỗi khi đổi bộ lọc.
 */
public class PnlDoanhThuKy {

    /** Nguồn dữ liệu; tách ra để test không cần DB. */
    interface Nguon {
        TongHopDoanhThu tongHop(LocalDate tu, LocalDate den);

        List<DoanhThuMotNgay> tungNgay(LocalDate tu, LocalDate den);
    }

    /** Kết quả một lần tải: tổng của khoảng đang xem, của khoảng trước đó và các cột biểu đồ. */
    private record DuLieu(TongHopDoanhThu nay, TongHopDoanhThu truoc, List<Cot> cot) {
    }

    /** Một cột (cặp cột khi xem theo ngày) trên biểu đồ: nhãn trục ngang, doanh thu đang xem và trước đó. */
    record Cot(String nhan, BigDecimal nay, BigDecimal truoc) {
    }

    private static final Logger LOG = Logger.getLogger(PnlDoanhThuKy.class.getName());
    private static final Color XANH_NHAT = new Color(0xB7DEDB);
    private static final Color DO = new Color(0xC94C48);
    private static final DateTimeFormatter NGAY_NGAN = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter NGAY_DAY_DU = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter THANG = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final String[] THU = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN"};
    /** Số năm trên biểu đồ khi xem theo năm. */
    static final int SO_NAM_BIEU_DO = 5;

    private final Nguon nguon;
    final JPanel theChiSo;
    final JPanel theBieuDo;
    final JLabel lblDoanhThu = new JLabel("—");
    final JLabel lblSoVoiTruoc = new JLabel(" ");
    final JLabel lblTieuDeBieuDo = new JLabel("Doanh thu theo ngày");
    final JLabel lblChuThichNay = new JLabel();
    final JLabel lblChuThichTruoc = new JLabel();
    final BieuDoCot bieuDo = new BieuDoCot();

    private SwingWorker<DuLieu, Void> worker;

    public PnlDoanhThuKy() {
        this(taoNguonDao());
    }

    PnlDoanhThuKy(Nguon nguon) {
        this.nguon = Objects.requireNonNull(nguon);
        theChiSo = taoTheChiSo();
        theBieuDo = taoTheBieuDo();
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

    // ===================== Dựng 2 thẻ =====================

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
        lblSoVoiTruoc.setFont(font(11));
        lblSoVoiTruoc.setForeground(MUTED);
        the.add(lblSoVoiTruoc, BorderLayout.SOUTH);
        return the;
    }

    private JPanel taoTheBieuDo() {
        SurfacePanel the = new SurfacePanel(new BorderLayout(0, 8));
        the.setName("theBieuDoDoanhThu");
        the.setBorder(new EmptyBorder(16, 18, 12, 18));
        JPanel dau = new JPanel(new BorderLayout());
        dau.setOpaque(false);
        lblTieuDeBieuDo.setName("tieuDeBieuDoDoanhThu");
        lblTieuDeBieuDo.setFont(bold(16));
        lblTieuDeBieuDo.setForeground(TEXT);
        dau.add(lblTieuDeBieuDo, BorderLayout.WEST);
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
        bieuDo.setPreferredSize(new Dimension(0, 230));
        the.add(bieuDo, BorderLayout.CENTER);
        return the;
    }

    // ===================== Tải dữ liệu =====================

    /**
     * Trang Báo cáo gọi khi đổi bộ lọc: tải tổng của {@code nay} và {@code truoc} cùng dữ liệu biểu đồ chạy nền,
     * rồi cập nhật cả 2 thẻ. {@code ky} quyết định chữ "so với ..." và kiểu biểu đồ.
     */
    public void taiDuLieu(KhoangNgay nay, KhoangNgay truoc, Ky ky) {
        Objects.requireNonNull(ky);
        if (worker != null) {
            worker.cancel(true); // bỏ kết quả lần trước nếu chưa xong
        }
        datKhungBieuDo(ky, nay, truoc);
        bieuDo.datDuLieu(List.of(), ky == Ky.NGAY, "Đang tải...");
        worker = new SwingWorker<>() {
            @Override
            protected DuLieu doInBackground() {
                TongHopDoanhThu a = nguon.tongHop(nay.tu(), nay.den());
                TongHopDoanhThu b = nguon.tongHop(truoc.tu(), truoc.den());
                KhoangNgay kb = khoangBieuDo(ky, nay);
                List<DoanhThuMotNgay> ngay = kb == null ? List.of() : List.copyOf(nguon.tungNgay(kb.tu(), kb.den()));
                return new DuLieu(a, b, cotBieuDo(ky, nay, ngay, a.thucNhan(), b.thucNhan()));
            }

            @Override
            protected void done() {
                if (this != worker || isCancelled()) {
                    return;
                }
                worker = null;
                try {
                    hienThi(get(), ky);
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

    /** Tiêu đề và chú thích biểu đồ theo bộ lọc; chỉ xem theo ngày mới có chú thích màu nhạt. */
    private void datKhungBieuDo(Ky ky, KhoangNgay nay, KhoangNgay truoc) {
        lblTieuDeBieuDo.setText(ky.tieuDeBieuDo());
        lblChuThichNay.setText(chuThichBieuDo(ky, nay));
        lblChuThichTruoc.setText(nhanKhoang(truoc));
        lblChuThichTruoc.setVisible(ky == Ky.NGAY);
    }

    /** Ngày sai hoặc lỗi tải: xóa số liệu, hiện thông báo giữa biểu đồ. */
    public void baoLoi(String thongBao) {
        if (worker != null) {
            worker.cancel(true);
            worker = null;
        }
        lblDoanhThu.setText("—");
        lblSoVoiTruoc.setForeground(MUTED);
        lblSoVoiTruoc.setText(" ");
        bieuDo.datDuLieu(List.of(), false, thongBao);
    }

    private void hienThi(DuLieu d, Ky ky) {
        TongHopDoanhThu a = d.nay();
        TongHopDoanhThu b = d.truoc();
        lblDoanhThu.setText(PnlThongKeDichVu.tien(a.thucNhan()));
        String phanTram = thayDoi(b.thucNhan(), a.thucNhan());
        lblSoVoiTruoc.setText(soVoiTruoc(phanTram, ky.truoc(), b.thucNhan()));
        lblSoVoiTruoc.setForeground(phanTram.startsWith("+") ? GREEN : phanTram.startsWith("−") ? DO : MUTED);
        boolean coSo = d.cot().stream().anyMatch(c -> c.nay().signum() != 0 || c.truoc().signum() != 0);
        bieuDo.datDuLieu(d.cot(), ky == Ky.NGAY, coSo ? null : "Không có dữ liệu.");
    }

    // ===================== Tính toán phụ (static để test) =====================

    /**
     * % thay đổi so với trước đó, 1 chữ số thập phân: "+12,4%", "−3,0%", "0,0%".
     * Trước đó = 0 thì không tính được % -> "—".
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

    /**
     * Dòng dưới số doanh thu: "↑ 12,4% so với tuần trước (65.180.000 ₫)". Không tính được % (trước đó = 0)
     * thì chỉ ghi số của trước đó: "Tuần trước: 0 ₫".
     */
    static String soVoiTruoc(String phanTram, String tenTruoc, BigDecimal tienTruoc) {
        String tien = PnlThongKeDichVu.tien(tienTruoc);
        if (phanTram.equals("—")) {
            return Character.toUpperCase(tenTruoc.charAt(0)) + tenTruoc.substring(1) + ": " + tien;
        }
        String muiTen = phanTram.startsWith("+") ? "↑ " : phanTram.startsWith("−") ? "↓ " : "";
        return muiTen + phanTram.replaceFirst("^[+−]", "") + " so với " + tenTruoc + " (" + tien + ")";
    }

    /**
     * Khoảng ngày cần đọc doanh thu từng ngày để vẽ biểu đồ: Tuần -> cả tuần chứa ngày đầu (thứ Hai → CN);
     * Tháng -> cả năm chứa tháng đó; Năm -> {@link #SO_NAM_BIEU_DO} năm tới năm đang xem; Khoảng -> đúng khoảng.
     * Ngày không cần đọc thêm (dùng luôn 2 số tổng) nên trả {@code null}.
     */
    static KhoangNgay khoangBieuDo(Ky ky, KhoangNgay nay) {
        LocalDate dau = nay.tu();
        return switch (ky) {
            case NGAY -> null;
            case TUAN -> new KhoangNgay(dau.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    dau.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
            case THANG -> new KhoangNgay(dau.withDayOfYear(1), dau.withMonth(12).withDayOfMonth(31));
            case NAM -> new KhoangNgay(dau.minusYears(SO_NAM_BIEU_DO - 1L).withDayOfYear(1),
                    dau.withMonth(12).withDayOfMonth(31));
            case KHOANG -> nay;
        };
    }

    /**
     * Các cột biểu đồ từ doanh thu từng ngày của {@link #khoangBieuDo}. Ngày: 1 cặp cột (đang xem, ngày trước);
     * Tuần: 7 cột theo thứ; Tháng: 12 cột theo tháng; Năm: mỗi năm 1 cột; Khoảng: theo {@link #gopCot}.
     */
    static List<Cot> cotBieuDo(Ky ky, KhoangNgay nay, List<DoanhThuMotNgay> ngay, BigDecimal tongNay,
            BigDecimal tongTruoc) {
        List<Cot> kq = new ArrayList<>();
        switch (ky) {
            case NGAY -> kq.add(new Cot(nay.tu().format(NGAY_DAY_DU), tongNay, tongTruoc));
            case TUAN -> {
                BigDecimal[] theoThu = mangKhong(7);
                for (DoanhThuMotNgay d : ngay) {
                    int i = d.ngay().getDayOfWeek().getValue() - 1;
                    theoThu[i] = theoThu[i].add(d.thucNhan());
                }
                for (int i = 0; i < 7; i++) {
                    kq.add(new Cot(THU[i], theoThu[i], BigDecimal.ZERO));
                }
            }
            case THANG -> {
                BigDecimal[] theoThang = mangKhong(12);
                for (DoanhThuMotNgay d : ngay) {
                    int i = d.ngay().getMonthValue() - 1;
                    theoThang[i] = theoThang[i].add(d.thucNhan());
                }
                for (int i = 0; i < 12; i++) {
                    kq.add(new Cot("Th" + (i + 1), theoThang[i], BigDecimal.ZERO));
                }
            }
            case NAM -> {
                int namDau = nay.tu().getYear() - SO_NAM_BIEU_DO + 1;
                BigDecimal[] theoNam = mangKhong(SO_NAM_BIEU_DO);
                for (DoanhThuMotNgay d : ngay) {
                    int i = d.ngay().getYear() - namDau;
                    if (i >= 0 && i < SO_NAM_BIEU_DO) {
                        theoNam[i] = theoNam[i].add(d.thucNhan());
                    }
                }
                for (int i = 0; i < SO_NAM_BIEU_DO; i++) {
                    kq.add(new Cot(String.valueOf(namDau + i), theoNam[i], BigDecimal.ZERO));
                }
            }
            case KHOANG -> kq.addAll(gopCot(ngay, List.of(), nay.soNgay()));
        }
        return kq;
    }

    private static BigDecimal[] mangKhong(int n) {
        BigDecimal[] a = new BigDecimal[n];
        Arrays.fill(a, BigDecimal.ZERO);
        return a;
    }

    /** Chú thích màu đậm: ngày đang xem, tuần (thứ Hai → CN), năm, các năm hoặc khoảng ngày. */
    static String chuThichBieuDo(Ky ky, KhoangNgay nay) {
        int nam = nay.tu().getYear();
        return switch (ky) {
            case NGAY -> nay.tu().format(NGAY_DAY_DU);
            case TUAN -> "Tuần " + nhanKhoang(khoangBieuDo(ky, nay));
            case THANG -> "Năm " + nam;
            case NAM -> "Năm " + (nam - SO_NAM_BIEU_DO + 1) + " – " + nam;
            case KHOANG -> nhanKhoang(nay);
        };
    }

    /**
     * Gộp doanh thu từng ngày thành các cột biểu đồ, ghép 2 danh sách theo thứ tự (ngày thứ 1 với ngày thứ 1...).
     * Cách gộp theo số ngày: tới 31 ngày -> mỗi ngày 1 cột; tới 120 ngày -> mỗi 7 ngày 1 cột; dài hơn -> mỗi
     * tháng dương lịch 1 cột. Danh sách thứ 2 thiếu ngày thì tính 0.
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
                BigDecimal sau = k < thangTruoc.size()
                        ? cong(truoc, thangTruoc.get(k)[0], thangTruoc.get(k)[1]) : BigDecimal.ZERO;
                kq.add(new Cot(nay.get(t[0]).ngay().format(THANG), cong(nay, t[0], t[1]), sau));
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
        return k.tu().equals(k.den()) ? k.tu().format(NGAY_DAY_DU)
                : k.tu().format(NGAY_NGAN) + " – " + k.den().format(NGAY_DAY_DU);
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
     * Biểu đồ cột tự vẽ (dự án không dùng thư viện biểu đồ), giá trị theo triệu đồng. {@code haiMau} = true (xem
     * theo ngày) thì mỗi nhóm có cột trước đó (xanh nhạt) cạnh cột đang xem (teal); còn lại chỉ 1 cột teal.
     * Trục dọc 3 mốc 0 / giữa / đỉnh làm tròn lên số đẹp.
     */
    static class BieuDoCot extends JComponent {

        private List<Cot> cot = List.of();
        private boolean haiMau;
        private String thongBao = "Đang tải...";

        void datDuLieu(List<Cot> cot, boolean haiMau, String thongBao) {
            this.cot = List.copyOf(cot);
            this.haiMau = haiMau;
            this.thongBao = thongBao;
            repaint();
        }

        List<Cot> cot() {
            return cot;
        }

        boolean haiMau() {
            return haiMau;
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
                max = Math.max(max, Math.max(trieu(c.nay()), haiMau ? trieu(c.truoc()) : 0));
            }
            double dinh = soDep(max);
            int trai = 30, duoi = 22, tren = 18;
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
            int rongCot = haiMau ? (int) Math.max(3, Math.min(40, rongNhom * 0.3))
                    : (int) Math.max(3, Math.min(48, rongNhom * 0.55));
            int buocNhan = Math.max(1, (int) Math.ceil(n / Math.max(1.0, (w - trai) / 46.0))); // nhãn không chồng
            boolean ghiGiaTri = n <= 14;
            for (int i = 0; i < n; i++) {
                Cot c = cot.get(i);
                int giua = trai + 4 + (int) Math.round(rongNhom * i + rongNhom / 2);
                int hNay = (int) Math.round(caoVe * trieu(c.nay()) / dinh);
                int xNay;
                if (haiMau) {
                    int hTruoc = (int) Math.round(caoVe * trieu(c.truoc()) / dinh);
                    g.setColor(XANH_NHAT);
                    g.fillRect(giua - rongCot - 1, tren + caoVe - hTruoc, rongCot, hTruoc);
                    if (ghiGiaTri && c.truoc().signum() != 0) {
                        ghiSo(g, trieu(c.truoc()), giua - 1 - rongCot / 2, tren + caoVe - hTruoc - 4, MUTED);
                    }
                    xNay = giua + 1;
                } else {
                    xNay = giua - rongCot / 2;
                }
                g.setColor(TEAL);
                g.fillRect(xNay, tren + caoVe - hNay, rongCot, hNay);
                if (ghiGiaTri && c.nay().signum() != 0) {
                    ghiSo(g, trieu(c.nay()), xNay + rongCot / 2, tren + caoVe - hNay - 4, TEAL_DARK);
                }
                if (i % buocNhan == 0) {
                    g.setColor(MUTED);
                    g.drawString(c.nhan(), giua - fm.stringWidth(c.nhan()) / 2, h - 6);
                }
            }
            g.dispose();
        }

        /** Ghi giá trị (triệu đồng) căn giữa tại x, ngay trên đỉnh cột. */
        private static void ghiSo(Graphics2D g, double giaTri, int x, int y, Color mau) {
            String gt = soGon(giaTri);
            g.setColor(mau);
            g.setFont(bold(10));
            g.drawString(gt, x - g.getFontMetrics().stringWidth(gt) / 2, y);
            g.setFont(font(10));
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
