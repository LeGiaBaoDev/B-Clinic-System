package com.quanlynhakhoa.ui.dialog;

import static com.quanlynhakhoa.ui.component.UiConstants.BACKGROUND;
import static com.quanlynhakhoa.ui.component.UiConstants.BORDER;
import static com.quanlynhakhoa.ui.component.UiConstants.MUTED;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL;
import static com.quanlynhakhoa.ui.component.UiConstants.TEXT;
import static com.quanlynhakhoa.ui.component.UiConstants.bold;
import static com.quanlynhakhoa.ui.component.UiConstants.font;

import com.quanlynhakhoa.entity.LichHen;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/** Xem trước và lưu phiếu khám bệnh ra PDF (UC021). */
public class DlgPhieuTiepNhan extends JDialog {

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("HH:mm · dd/MM/yyyy");
    private static final DateTimeFormatter NGAY_IN = DateTimeFormatter.ofPattern("'Ngày' dd 'tháng' MM 'năm' yyyy", Locale.of("vi", "VN"));
    private static final String[] COT = {"STT", "Triệu chứng / Chẩn đoán",
        "Tên dịch vụ điều trị", "Nha sĩ", "Đơn giá", "Số lượng", "Thành tiền"};

    private final LichHen lichHen;
    private final String maPK;
    private final int stt;
    private final LocalDateTime gioDen;
    private final String nguoiTiepNhan;
    private JPanel phieuPanel;

    private DlgPhieuTiepNhan(Window owner, LichHen lichHen, String maPK, int stt,
            LocalDateTime gioDen, String nguoiTiepNhan) {
        super(owner, "Phiếu khám bệnh", ModalityType.APPLICATION_MODAL);
        this.lichHen = lichHen;
        this.maPK = maPK;
        this.stt = stt;
        this.gioDen = gioDen;
        this.nguoiTiepNhan = nguoiTiepNhan;
        setContentPane(taoNoiDung());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setMinimumSize(new Dimension(760, 700));
        setSize(820, 800);
        setLocationRelativeTo(owner);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public static void hien(Window owner, LichHen lichHen, String maPK, int stt,
            LocalDateTime gioDen, String nguoiTiepNhan) {
        new DlgPhieuTiepNhan(owner, lichHen, maPK, stt, gioDen, nguoiTiepNhan).setVisible(true);
    }

    private JPanel taoNoiDung() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(BACKGROUND);
        root.setBorder(new EmptyBorder(20, 24, 16, 24));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel tieuDe = new JLabel("Phiếu khám bệnh");
        tieuDe.setFont(bold(21));
        tieuDe.setForeground(TEXT);
        JLabel phuDe = new JLabel("Xem trước phiếu khám và lưu ra PDF.");
        phuDe.setFont(font(12));
        phuDe.setForeground(MUTED);
        header.add(tieuDe);
        header.add(Box.createVerticalStrut(4));
        header.add(phuDe);
        root.add(header, BorderLayout.NORTH);

        phieuPanel = taoPhieu();
        JScrollPane scr = new JScrollPane(phieuPanel);
        scr.setBorder(BorderFactory.createLineBorder(BORDER));
        scr.getViewport().setBackground(BACKGROUND);
        scr.getVerticalScrollBar().setUnitIncrement(18);
        root.add(scr, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);
        JButton btnDong = new JButton("Đóng");
        btnDong.setFont(font(13));
        btnDong.setPreferredSize(new Dimension(100, 38));
        btnDong.addActionListener(e -> dispose());
        JButton btnLuu = new JButton("Lưu PDF", new LineIcon(LineIcon.Type.INVOICE, Color.WHITE, 16));
        btnLuu.setFont(bold(13));
        btnLuu.setBackground(TEAL);
        btnLuu.setForeground(Color.WHITE);
        btnLuu.setPreferredSize(new Dimension(120, 38));
        btnLuu.setIconTextGap(7);
        btnLuu.addActionListener(e -> luuPdf());
        footer.add(btnDong);
        footer.add(btnLuu);
        root.add(footer, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnLuu);
        return root;
    }

    private JPanel taoPhieu() {
        SurfacePanel phieu = new SurfacePanel(new BorderLayout(0, 12));
        phieu.setBackground(Color.WHITE);
        phieu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xB9C5D1)),
                new EmptyBorder(23, 29, 23, 29)));
        phieu.setPreferredSize(new Dimension(700, 900));

        phieu.add(taoDauPhieu(), BorderLayout.NORTH);
        phieu.add(taoThanPhieu(), BorderLayout.CENTER);
        phieu.add(taoCuoiPhieu(), BorderLayout.SOUTH);
        return phieu;
    }

    private JPanel taoDauPhieu() {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setOpaque(false);

        JPanel brand = new JPanel(new BorderLayout(10, 0));
        brand.setOpaque(false);
        java.net.URL logoUrl = getClass().getResource("/images/logo.png");
        if (logoUrl != null) {
            ImageIcon logo = new ImageIcon(logoUrl);
            JLabel anh = new JLabel(new ImageIcon(logo.getImage()
                    .getScaledInstance(68, 48, java.awt.Image.SCALE_SMOOTH)));
            brand.add(anh, BorderLayout.WEST);
        }
        JPanel clinic = new JPanel();
        clinic.setOpaque(false);
        clinic.setLayout(new BoxLayout(clinic, BoxLayout.Y_AXIS));
        JLabel ten = new JLabel("NHA KHOA B-CLINIC");
        ten.setFont(bold(15));
        ten.setForeground(TEXT);
        JLabel diaChi = new JLabel("12 Nguyễn Văn Bảo, phường Hạnh Thông, TP. Hồ Chí Minh");
        diaChi.setFont(font(9));
        diaChi.setForeground(MUTED);
        JLabel dienThoai = new JLabel("Điện thoại: 0372415720");
        dienThoai.setFont(font(9));
        dienThoai.setForeground(MUTED);
        clinic.add(ten);
        clinic.add(Box.createVerticalStrut(3));
        clinic.add(diaChi);
        clinic.add(Box.createVerticalStrut(2));
        clinic.add(dienThoai);
        brand.add(clinic, BorderLayout.CENTER);
        panel.add(brand, BorderLayout.WEST);

        JPanel meta = new JPanel();
        meta.setOpaque(false);
        meta.setLayout(new BoxLayout(meta, BoxLayout.Y_AXIS));
        JLabel ma = new JLabel("Mã phiếu: " + (maPK == null || maPK.isBlank() ? "—" : maPK));
        ma.setFont(bold(11));
        ma.setForeground(TEXT);
        JLabel so = new JLabel("Số thứ tự: " + (stt > 0 ? String.format("%02d", stt) : "—"));
        so.setFont(bold(11));
        so.setForeground(stt > 0 ? TEAL : MUTED);
        JLabel ngay = new JLabel(gioDen.format(NGAY_IN));
        ngay.setFont(font(10));
        ngay.setForeground(MUTED);
        ma.setAlignmentX(RIGHT_ALIGNMENT);
        so.setAlignmentX(RIGHT_ALIGNMENT);
        ngay.setAlignmentX(RIGHT_ALIGNMENT);
        meta.add(ma);
        meta.add(Box.createVerticalStrut(3));
        meta.add(so);
        meta.add(Box.createVerticalStrut(3));
        meta.add(ngay);
        panel.add(meta, BorderLayout.EAST);
        return panel;
    }

    private JPanel taoThanPhieu() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel tieuDe = new JLabel("PHIẾU KHÁM BỆNH", SwingConstants.CENTER);
        tieuDe.setFont(bold(18));
        tieuDe.setForeground(TEXT);
        tieuDe.setAlignmentX(CENTER_ALIGNMENT);
        panel.add(tieuDe);
        panel.add(Box.createVerticalStrut(16));

        JPanel thongTin = new JPanel(new GridBagLayout());
        thongTin.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 0, 5, 8);
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 0;
        themNhan(thongTin, c, "Họ tên khách hàng:");
        c.gridx = 1;
        c.weightx = 1;
        thongTin.add(duongKe(lichHen.getKhachHang().getTenKH()), c);
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        themNhan(thongTin, c, "Địa chỉ:");
        c.gridx = 1;
        c.weightx = 1;
        thongTin.add(duongKe(lichHen.getKhachHang().getDiaChi() == null ? ""
                : lichHen.getKhachHang().getDiaChi()), c);
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        themNhan(thongTin, c, "Điện thoại:");
        c.gridx = 1;
        c.weightx = 0.45;
        thongTin.add(duongKe(lichHen.getKhachHang().getSdt()), c);
        c.gridx = 2;
        c.weightx = 0;
        themNhan(thongTin, c, "Email:");
        c.gridx = 3;
        c.weightx = 0.55;
        thongTin.add(duongKe(lichHen.getKhachHang().getEmail() == null ? ""
                : lichHen.getKhachHang().getEmail()), c);
        panel.add(thongTin);
        panel.add(Box.createVerticalStrut(14));

        panel.add(taoBangDichVu());
        panel.add(Box.createVerticalStrut(12));

        JPanel dongLich = new JPanel(new BorderLayout());
        dongLich.setOpaque(false);
        dongLich.add(duongKe("Lịch hẹn: " + lichHen.getNgayGioHen().format(NGAY_GIO)), BorderLayout.WEST);
        dongLich.add(duongKe("Tổng chi phí: " + tien(lichHen.getDichVu().getDonGia())),
                BorderLayout.EAST);
        panel.add(dongLich);
        panel.add(Box.createVerticalStrut(10));

        JPanel loiDan = new JPanel(new BorderLayout());
        loiDan.setOpaque(false);
        loiDan.add(duongKe("Lời dặn: " + (lichHen.getGhiChu() == null ? "" : lichHen.getGhiChu())),
                BorderLayout.CENTER);
        panel.add(loiDan);
        return panel;
    }

    private JPanel taoBangDichVu() {
        DefaultTableModel model = new DefaultTableModel(COT, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        model.addRow(new Object[]{1, "Chưa ghi nhận", lichHen.getDichVu().getTenDV(),
            lichHen.getNhaSi().getHoTenNV(), tien(lichHen.getDichVu().getDonGia()), 1,
            tien(lichHen.getDichVu().getDonGia())});
        model.addRow(new Object[]{"", "", "", "", "", "", ""});
        model.addRow(new Object[]{"", "", "", "", "", "", ""});

        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setFont(font(10));
        table.getTableHeader().setFont(bold(9));
        table.getTableHeader().setBackground(new Color(0xF2F5F8));
        table.getTableHeader().setForeground(TEXT);
        table.setGridColor(new Color(0x9AA6B2));
        table.setShowGrid(true);
        table.setFillsViewportHeight(true);
        table.setRowSelectionAllowed(false);
        int[] rong = {38, 150, 140, 95, 80, 55, 95};
        for (int i = 0; i < rong.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        }
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(center);
        table.getColumnModel().getColumn(5).setCellRenderer(center);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createLineBorder(new Color(0x9AA6B2)));
        panel.add(table.getTableHeader(), BorderLayout.NORTH);
        panel.add(table, BorderLayout.CENTER);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 126));
        return panel;
    }

    private JPanel taoCuoiPhieu() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        JPanel chuKy = new JPanel(new java.awt.GridLayout(1, 2, 50, 0));
        chuKy.setOpaque(false);
        chuKy.add(oTenKy("KHÁCH HÀNG", lichHen.getKhachHang().getTenKH()));
        chuKy.add(oTenKy("NHA SĨ", lichHen.getNhaSi().getHoTenNV()));
        panel.add(chuKy, BorderLayout.CENTER);
        JLabel ghiChu = new JLabel("Vui lòng giữ phiếu và theo dõi số thứ tự tại phòng chờ. In lúc "
                + LocalDateTime.now().format(NGAY_GIO) + ".");
        ghiChu.setFont(font(9));
        ghiChu.setForeground(MUTED);
        panel.add(ghiChu, BorderLayout.SOUTH);
        return panel;
    }

    private static void themNhan(JPanel panel, GridBagConstraints c, String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font(11));
        lbl.setForeground(TEXT);
        panel.add(lbl, c);
    }

    private static JLabel duongKe(String text) {
        JLabel lbl = new JLabel(text == null ? "" : text);
        lbl.setFont(font(11));
        lbl.setForeground(TEXT);
        lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x8C98A4)));
        return lbl;
    }

    private static JPanel oTenKy(String vaiTro, String ten) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel lbl = new JLabel(vaiTro);
        lbl.setFont(bold(11));
        lbl.setForeground(TEXT);
        lbl.setAlignmentX(CENTER_ALIGNMENT);
        JLabel phu = new JLabel("(Ký và ghi rõ họ tên)");
        phu.setFont(font(9));
        phu.setForeground(MUTED);
        phu.setAlignmentX(CENTER_ALIGNMENT);
        JLabel nguoi = new JLabel(ten == null ? "" : ten);
        nguoi.setFont(font(11));
        nguoi.setForeground(TEXT);
        nguoi.setAlignmentX(CENTER_ALIGNMENT);
        p.add(lbl);
        p.add(phu);
        p.add(Box.createVerticalStrut(38));
        p.add(nguoi);
        return p;
    }

    private static String tien(double value) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.of("vi", "VN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(value) + " đ";
    }

    private void luuPdf() {
        if (phieuPanel == null || phieuPanel.getWidth() <= 0 || phieuPanel.getHeight() <= 0) {
            JOptionPane.showMessageDialog(this, "Phiếu chưa sẵn sàng để lưu.",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chon = new JFileChooser();
        chon.setSelectedFile(new File("phieu-kham-" + lichHen.getMaLH() + ".pdf"));
        if (chon.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chon.getSelectedFile();
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            file = new File(file.getParentFile(), file.getName() + ".pdf");
        }
        try {
            BufferedImage image = new BufferedImage(phieuPanel.getWidth() * 4,
                    phieuPanel.getHeight() * 4, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = image.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, image.getWidth(), image.getHeight());
            g2.scale(4, 4);
            phieuPanel.printAll(g2);
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
                            margin + (cao - veCao) / 2, veRong, veCao);
                }
                doc.save(file);
            }
            JOptionPane.showMessageDialog(this, "Đã lưu phiếu PDF:\n" + file.getAbsolutePath(),
                    "Lưu PDF thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | SecurityException e) {
            JOptionPane.showMessageDialog(this, "Không thể lưu file PDF: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
