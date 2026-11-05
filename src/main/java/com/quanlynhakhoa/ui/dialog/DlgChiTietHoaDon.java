package com.quanlynhakhoa.ui.dialog;

import com.quanlynhakhoa.dao.ChiTietPhieuKhamDAO;
import com.quanlynhakhoa.dao.ChiTietPhieuKhamDAO.DongThanhToan;
import com.quanlynhakhoa.dao.HoaDonDAO;
import com.quanlynhakhoa.entity.HoaDon;
import com.quanlynhakhoa.entity.KhachHang;
import com.quanlynhakhoa.entity.Voucher;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import com.quanlynhakhoa.ui.panel.TraCuuHoaDon;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * Hộp thoại "Chi tiết hóa đơn" (chỉ đọc), mở khi bấm "Xem" trên danh sách hóa đơn.
 * Lễ tân và Quản lý dùng được (DAO của Bảo kiểm tra quyền).
 *
 * <p>Hiển thị: khách hàng, ngày lập, phiếu khám; từng dòng dịch vụ (đơn vị tính, số lượng, đơn giá,
 * thành tiền, thuế GTGT và tiền thuế của dòng); tổng tiền hàng, thuế, voucher áp dụng, số tiền giảm,
 * tổng cộng thanh toán; hình thức, thời điểm thanh toán và người thu.</p>
 *
 * <p>Dữ liệu chỉ đọc qua API của Bảo: {@link HoaDonDAO#layTheoMa}, {@link HoaDonDAO#tinhTongTien},
 * {@link ChiTietPhieuKhamDAO#layDongThanhToan} (bản không có chẩn đoán, phù hợp cho lễ tân).
 * Nút "Xuất hóa đơn" chưa có: sẽ gắn khi có hàm xuất PDF dùng chung của Bảo.</p>
 */
public class DlgChiTietHoaDon extends JDialog {

    /** Gói dữ liệu đọc một lần trong SwingWorker. */
    record DuLieu(HoaDon hoaDon, List<DongThanhToan> dong, BigDecimal tongThanhToan) {
    }

    /** Mở chi tiết hóa đơn {@code maHD}, dùng DAO thật. */
    public DlgChiTietHoaDon(Window owner, String maHD) {
        this(owner, maHD, new HoaDonDAO()::layTheoMa, new ChiTietPhieuKhamDAO()::layDongThanhToan,
                new HoaDonDAO()::tinhTongTien);
    }

    DlgChiTietHoaDon(Window owner, String maHD, Function<String, HoaDon> layHoaDon,
            Function<String, List<DongThanhToan>> layDong, Function<String, BigDecimal> tinhTong) {
        super(owner, "Chi tiết hóa đơn " + maHD, ModalityType.APPLICATION_MODAL);
        NoiDung noiDung = new NoiDung(maHD, layHoaDon, layDong, tinhTong, this::dispose);
        setContentPane(noiDung);
        getRootPane().setDefaultButton(noiDung.btnDong);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setSize(820, 660);
        setMinimumSize(new Dimension(700, 560));
        setLocationRelativeTo(owner);
        noiDung.taiDuLieu();
    }

    /** Nội dung hộp thoại, tách khỏi cửa sổ để test không cần mở dialog thật. */
    static final class NoiDung extends JPanel {

        private static final Logger LOG = Logger.getLogger(DlgChiTietHoaDon.class.getName());
        private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        private static final Color DO = new Color(0xC94C48);
        private static final Color XANH_NHAT = new Color(0xE3F4EA);
        private static final Color VANG_NEN = new Color(0xFFF4D6);
        private static final Color VANG_CHU = new Color(0x8A6100);
        private static final Color NEN_HEADER = new Color(0xF7F9FB);

        private final String maHD;
        private final Function<String, HoaDon> layHoaDon;
        private final Function<String, List<DongThanhToan>> layDong;
        private final Function<String, BigDecimal> tinhTong;

        // Package-private để test đọc lại nội dung đã hiển thị.
        final JLabel lblTieuDe = new JLabel();
        final JLabel lblPhuDe = new JLabel(" ");
        final JLabel lblTrangThai = new JLabel();
        final JLabel lblKhachHang = new JLabel("—");
        final JLabel lblThanhToan = new JLabel("—");
        final JLabel lblTongTienHang = giaTri();
        final JLabel lblTienThue = giaTri();
        final JLabel lblVoucher = giaTri();
        final JLabel lblTienGiam = giaTri();
        final JLabel lblTongCong = new JLabel("—");
        final JLabel lblThongBao = new JLabel(" ");
        final JButton btnIn = new JButton("In hóa đơn PDF");
        final JButton btnDong = new JButton("Đóng");
        final DefaultTableModel modelDong = new DefaultTableModel(new String[]{
            "DỊCH VỤ", "ĐVT", "SL", "ĐƠN GIÁ", "THÀNH TIỀN", "THUẾ GTGT", "TIỀN THUẾ"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        final JTable tblDong = new JTable(modelDong);

        NoiDung(String maHD, Function<String, HoaDon> layHoaDon, Function<String, List<DongThanhToan>> layDong,
                Function<String, BigDecimal> tinhTong, Runnable dong) {
            super(new BorderLayout(0, 16));
            this.maHD = Objects.requireNonNull(maHD);
            this.layHoaDon = Objects.requireNonNull(layHoaDon);
            this.layDong = Objects.requireNonNull(layDong);
            this.tinhTong = Objects.requireNonNull(tinhTong);
            setBackground(BACKGROUND);
            setBorder(new EmptyBorder(20, 22, 18, 22));
            add(taoTieuDe(), BorderLayout.NORTH);
            add(taoThan(), BorderLayout.CENTER);
            add(taoChan(dong), BorderLayout.SOUTH);
        }

        // ===================== Dựng giao diện =====================

        private JPanel taoTieuDe() {
            JPanel header = new JPanel(new BorderLayout(12, 0));
            header.setOpaque(false);
            JPanel chu = new JPanel();
            chu.setOpaque(false);
            chu.setLayout(new BoxLayout(chu, BoxLayout.Y_AXIS));
            lblTieuDe.setName("tieuDeHoaDon");
            lblTieuDe.setText("Hóa đơn " + maHD);
            lblTieuDe.setFont(bold(20));
            lblTieuDe.setForeground(TEXT);
            lblPhuDe.setName("phuDeHoaDon");
            lblPhuDe.setFont(font(12));
            lblPhuDe.setForeground(MUTED);
            chu.add(lblTieuDe);
            chu.add(Box.createVerticalStrut(4));
            chu.add(lblPhuDe);
            header.add(chu, BorderLayout.CENTER);
            lblTrangThai.setName("trangThaiHoaDon");
            lblTrangThai.setFont(bold(12));
            lblTrangThai.setOpaque(true);
            lblTrangThai.setBorder(new EmptyBorder(5, 10, 5, 10));
            lblTrangThai.setVisible(false); // hiện khi tải xong
            JPanel phai = new JPanel(new GridBagLayout()); // căn giữa nhãn trạng thái theo chiều dọc
            phai.setOpaque(false);
            phai.add(lblTrangThai);
            header.add(phai, BorderLayout.EAST);
            return header;
        }

        private JPanel taoThan() {
            JPanel than = new JPanel(new BorderLayout(0, 14));
            than.setOpaque(false);

            // Hàng trên: 2 thẻ Khách hàng | Thanh toán
            JPanel hangThe = new JPanel(new GridLayout(1, 2, 14, 0));
            hangThe.setOpaque(false);
            lblKhachHang.setName("khachHang");
            lblThanhToan.setName("thanhToan");
            hangThe.add(the("Khách hàng", lblKhachHang));
            hangThe.add(the("Thanh toán", lblThanhToan));
            than.add(hangThe, BorderLayout.NORTH);

            // Giữa: bảng dịch vụ
            tblDong.setName("bangDichVu");
            tblDong.setFont(font(12));
            tblDong.setRowHeight(36);
            tblDong.setShowVerticalLines(false);
            tblDong.setGridColor(BORDER);
            tblDong.setFillsViewportHeight(true);
            tblDong.setFocusable(false);
            tblDong.setRowSelectionAllowed(false);
            tblDong.getTableHeader().setReorderingAllowed(false);
            tblDong.getTableHeader().setPreferredSize(new Dimension(0, 34));
            tblDong.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                    super.getTableCellRendererComponent(t, v, false, false, r, c);
                    setFont(bold(11));
                    setForeground(MUTED);
                    setBackground(NEN_HEADER);
                    setOpaque(true);
                    setHorizontalAlignment(c >= 2 && c != 5 ? RIGHT : LEFT);
                    setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(0, 10, 0, 10)));
                    return this;
                }
            });
            DefaultTableCellRenderer o = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                    super.getTableCellRendererComponent(t, v, false, false, r, c);
                    setHorizontalAlignment(c >= 2 && c != 5 ? RIGHT : LEFT); // số căn phải, chữ căn trái
                    setBorder(new EmptyBorder(0, 10, 0, 10));
                    setToolTipText(v == null ? null : v.toString());
                    return this;
                }
            };
            tblDong.setDefaultRenderer(Object.class, o);
            int[] rong = {230, 60, 45, 110, 120, 80, 100};
            for (int i = 0; i < rong.length; i++) {
                tblDong.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
            }
            JScrollPane cuon = new JScrollPane(tblDong);
            cuon.setBorder(BorderFactory.createLineBorder(BORDER));
            cuon.getViewport().setBackground(SURFACE);
            than.add(cuon, BorderLayout.CENTER);

            // Dưới: bảng tổng tiền canh phải
            SurfacePanel tong = new SurfacePanel(new GridBagLayout());
            tong.setBorder(new EmptyBorder(14, 18, 14, 18));
            GridBagConstraints g = new GridBagConstraints();
            g.fill = GridBagConstraints.HORIZONTAL;
            dongTong(tong, g, 0, "Tổng tiền hàng (chưa thuế)", lblTongTienHang);
            dongTong(tong, g, 1, "Thuế GTGT", lblTienThue);
            dongTong(tong, g, 2, "Voucher áp dụng", lblVoucher);
            dongTong(tong, g, 3, "Số tiền giảm", lblTienGiam);
            JSeparator gach = new JSeparator();
            gach.setForeground(BORDER);
            g.gridx = 0;
            g.gridy = 4;
            g.gridwidth = 2;
            g.weightx = 1;
            g.insets = new Insets(6, 0, 8, 0);
            tong.add(gach, g);
            JLabel nhanTong = new JLabel("Tổng cộng thanh toán");
            nhanTong.setFont(bold(14));
            nhanTong.setForeground(TEXT);
            lblTongCong.setName("tongCong");
            lblTongCong.setFont(bold(22));
            lblTongCong.setForeground(TEAL);
            lblTongCong.setHorizontalAlignment(SwingConstants.RIGHT);
            g.gridwidth = 1;
            g.gridy = 5;
            g.insets = new Insets(0, 0, 0, 0);
            g.gridx = 0;
            g.weightx = 1;
            tong.add(nhanTong, g);
            g.gridx = 1;
            g.weightx = 0;
            tong.add(lblTongCong, g);
            JPanel bocTong = new JPanel(new BorderLayout());
            bocTong.setOpaque(false);
            bocTong.setBorder(new EmptyBorder(14, 0, 0, 0));
            bocTong.add(tong, BorderLayout.CENTER);
            than.add(bocTong, BorderLayout.SOUTH);
            return than;
        }

        private JPanel taoChan(Runnable dong) {
            JPanel chan = new JPanel(new BorderLayout(12, 0));
            chan.setOpaque(false);
            lblThongBao.setName("thongBao");
            lblThongBao.setFont(font(12));
            lblThongBao.setForeground(MUTED);
            chan.add(lblThongBao, BorderLayout.CENTER);
            btnIn.setName("inHoaDon");
            btnIn.setFont(bold(12));
            btnIn.setPreferredSize(new Dimension(150, 38));
            btnIn.setBackground(TEAL);
            btnIn.setForeground(Color.WHITE);
            btnIn.addActionListener(e -> xuatPdf());
            btnDong.setName("dongChiTiet");
            btnDong.setFont(bold(12));
            btnDong.setPreferredSize(new Dimension(100, 38));
            btnDong.addActionListener(e -> dong.run());
            JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            nut.setOpaque(false);
            nut.add(btnIn);
            nut.add(btnDong);
            chan.add(nut, BorderLayout.EAST);
            return chan;
        }

        private void xuatPdf() {
            JFileChooser chon = new JFileChooser();
            chon.setSelectedFile(new File("hoa-don-" + maHD + ".pdf"));
            if (chon.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            File file = chon.getSelectedFile();
            if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                file = new File(file.getParentFile(), file.getName() + ".pdf");
            }
            Dimension kichThuoc = getPreferredSize();
            setSize(new Dimension(720, kichThuoc.height));
            doLayout();
            try {
                BufferedImage image = new BufferedImage(getWidth() * 2, getHeight() * 2,
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = image.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, image.getWidth(), image.getHeight());
                g2.scale(2, 2);
                printAll(g2);
                g2.dispose();

                try (PDDocument doc = new PDDocument()) {
                    PDPage page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    PDImageXObject pdfImage = LosslessFactory.createFromImage(doc, image);
                    float margin = 28;
                    float rong = page.getMediaBox().getWidth() - margin * 2;
                    float cao = page.getMediaBox().getHeight() - margin * 2;
                    float tiLe = Math.min(rong / image.getWidth(), cao / image.getHeight());
                    float veRong = image.getWidth() * tiLe;
                    float veCao = image.getHeight() * tiLe;
                    try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                        cs.drawImage(pdfImage, margin + (rong - veRong) / 2,
                                page.getMediaBox().getHeight() - margin - veCao, veRong, veCao);
                    }
                    doc.save(file);
                }
                lblThongBao.setForeground(GREEN);
                lblThongBao.setText("Đã lưu hóa đơn: " + file.getAbsolutePath());
            } catch (IOException | SecurityException e) {
                lblThongBao.setForeground(DO);
                lblThongBao.setText("Không thể lưu PDF: " + e.getMessage());
            }
        }

        /** Thẻ trắng có tiêu đề nhỏ màu xám và nội dung bên dưới. */
        private static JPanel the(String tieuDe, JLabel noiDung) {
            SurfacePanel the = new SurfacePanel(new BorderLayout(0, 6));
            the.setBorder(new EmptyBorder(12, 16, 12, 16));
            JLabel nhan = new JLabel(tieuDe.toUpperCase(Locale.ROOT));
            nhan.setFont(bold(11));
            nhan.setForeground(MUTED);
            noiDung.setFont(font(13));
            noiDung.setForeground(TEXT);
            noiDung.setVerticalAlignment(SwingConstants.TOP);
            the.add(nhan, BorderLayout.NORTH);
            the.add(noiDung, BorderLayout.CENTER);
            return the;
        }

        private static void dongTong(JPanel p, GridBagConstraints g, int hang, String nhan, JLabel giaTri) {
            JLabel l = new JLabel(nhan);
            l.setFont(font(13));
            l.setForeground(MUTED);
            g.gridy = hang;
            g.gridwidth = 1;
            g.insets = new Insets(0, 0, 6, 24);
            g.gridx = 0;
            g.weightx = 1;
            p.add(l, g);
            g.gridx = 1;
            g.weightx = 0;
            g.insets = new Insets(0, 0, 6, 0);
            p.add(giaTri, g);
        }

        private static JLabel giaTri() {
            JLabel l = new JLabel("—");
            l.setFont(bold(13));
            l.setForeground(TEXT);
            l.setHorizontalAlignment(SwingConstants.RIGHT);
            return l;
        }

        // ===================== Tải và hiển thị dữ liệu =====================

        /** Đọc hóa đơn + các dòng dịch vụ + tổng tiền trong nền (không treo giao diện). */
        void taiDuLieu() {
            lblThongBao.setForeground(MUTED);
            lblThongBao.setText("Đang tải hóa đơn...");
            new SwingWorker<DuLieu, Void>() {
                @Override
                protected DuLieu doInBackground() {
                    HoaDon hd = layHoaDon.apply(maHD);
                    if (hd == null) {
                        return null;
                    }
                    return new DuLieu(hd, List.copyOf(layDong.apply(hd.getPhieuKham().getMaPK())),
                            tinhTong.apply(maHD));
                }

                @Override
                protected void done() {
                    try {
                        DuLieu duLieu = get();
                        if (duLieu == null) {
                            baoLoi("Không tìm thấy hóa đơn " + maHD + ".");
                            return;
                        }
                        hienThi(duLieu);
                        lblThongBao.setText(" ");
                    } catch (ExecutionException ex) {
                        Throwable cause = ex.getCause();
                        if (cause instanceof BusinessException) {
                            baoLoi(cause.getMessage()); // vd không có quyền xem hóa đơn
                        } else {
                            LOG.log(Level.WARNING, "Không thể tải hóa đơn " + maHD, cause);
                            baoLoi("Không thể tải hóa đơn. Vui lòng thử lại.");
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }.execute();
        }

        private void baoLoi(String thongBao) {
            lblThongBao.setForeground(DO);
            lblThongBao.setText(thongBao);
        }

        /** Đổ dữ liệu lên các phần của hộp thoại. */
        void hienThi(DuLieu d) {
            HoaDon hd = d.hoaDon();
            lblPhuDe.setText("Ngày lập " + ngayGio(hd.getNgayLap()) + " · Phiếu khám " + hd.getPhieuKham().getMaPK());

            // Nhãn trạng thái: xanh khi đã thanh toán, vàng khi còn chờ (cùng màu với danh sách).
            lblTrangThai.setText(TraCuuHoaDon.tenTrangThai(hd));
            lblTrangThai.setBackground(hd.isTrangThai() ? XANH_NHAT : VANG_NEN);
            lblTrangThai.setForeground(hd.isTrangThai() ? GREEN : VANG_CHU);
            lblTrangThai.setVisible(true);

            KhachHang kh = TraCuuHoaDon.khachHang(hd);
            lblKhachHang.setText(kh == null ? "—" : "<html><b>" + html(kh.getTenKH()) + "</b><br>"
                    + html(kh.getMaKH()) + " · " + html(kh.getSdt()) + "</html>");

            // Hóa đơn chưa thanh toán: chưa có hình thức, thời điểm, người thu (ràng buộc của bảng HoaDon).
            lblThanhToan.setText(!hd.isTrangThai() ? "Chưa thanh toán"
                    : "<html><b>" + html(hd.getHinhThucThanhToan()) + "</b><br>" + ngayGio(hd.getNgayThanhToan())
                    + (hd.getThuNgan() == null ? "" : " · Thu ngân: " + html(hd.getThuNgan().getHoTenNV())) + "</html>");

            modelDong.setRowCount(0);
            BigDecimal tienHang = BigDecimal.ZERO;
            BigDecimal tienThue = BigDecimal.ZERO;
            for (DongThanhToan dong : d.dong()) {
                modelDong.addRow(new Object[]{dong.tenDV(), dong.donViTinh(), dong.soLuong(),
                    tien(dong.donGia()), tien(dong.thanhTien()), tenThue(dong.mucThueGTGT()), tien(dong.tienThue())});
                tienHang = tienHang.add(soHoacKhong(dong.thanhTien()));
                tienThue = tienThue.add(soHoacKhong(dong.tienThue()));
            }
            lblTongTienHang.setText(tien(tienHang));
            lblTienThue.setText(tien(tienThue));
            Voucher vc = hd.getVoucher();
            lblVoucher.setText(vc == null ? "Không áp dụng" : vc.getMaVoucher() + " – " + vc.getTenVoucher());
            lblTienGiam.setText(hd.getTongKhuyenMai() > 0 ? "−" + tien(BigDecimal.valueOf(hd.getTongKhuyenMai())) : tien(BigDecimal.ZERO));
            lblTongCong.setText(tien(d.tongThanhToan()));
        }

        /** Thuế 0% hiển thị "KCT" (không chịu thuế) theo báo cáo. */
        static String tenThue(BigDecimal mucThue) {
            if (mucThue == null || mucThue.signum() == 0) {
                return "KCT";
            }
            return mucThue.stripTrailingZeros().toPlainString() + "%";
        }

        static String tien(BigDecimal soTien) {
            DecimalFormat f = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.of("vi", "VN")));
            return f.format(soHoacKhong(soTien)) + " ₫";
        }

        private static BigDecimal soHoacKhong(BigDecimal so) {
            return so == null ? BigDecimal.ZERO : so;
        }

        private static String ngayGio(java.time.LocalDateTime t) {
            return t == null ? "—" : t.format(NGAY_GIO);
        }

        private static String html(String text) {
            return text == null ? "—" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
