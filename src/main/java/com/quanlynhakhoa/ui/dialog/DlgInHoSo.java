package com.quanlynhakhoa.ui.dialog;

import static com.quanlynhakhoa.ui.component.UiConstants.BACKGROUND;
import static com.quanlynhakhoa.ui.component.UiConstants.BORDER;
import static com.quanlynhakhoa.ui.component.UiConstants.MUTED;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL;
import static com.quanlynhakhoa.ui.component.UiConstants.TEXT;
import static com.quanlynhakhoa.ui.component.UiConstants.bold;
import static com.quanlynhakhoa.ui.component.UiConstants.font;

import com.quanlynhakhoa.entity.ChiTietPhieuKham;
import com.quanlynhakhoa.entity.HoSoBenhAn;
import com.quanlynhakhoa.entity.KhachHang;
import com.quanlynhakhoa.entity.PhieuKham;
import com.quanlynhakhoa.entity.enums.TrangThaiPhieuKham;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

public class DlgInHoSo extends JDialog {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter NGAY_IN =
            DateTimeFormatter.ofPattern("'Ngày' dd 'tháng' MM 'năm' yyyy", Locale.of("vi", "VN"));

    private final transient KhachHang kh;
    private final transient HoSoBenhAn hs;
    private final transient List<PhieuKham> phieu;
    private final transient Map<String, ChiTietPhieuKham> ctDau;
    private JPanel phieuPanel;

    private DlgInHoSo(Window owner, KhachHang kh, HoSoBenhAn hs,
            List<PhieuKham> phieu, Map<String, ChiTietPhieuKham> ctDau) {
        super(owner, "Hồ sơ bệnh án điện tử", ModalityType.APPLICATION_MODAL);
        this.kh = kh;
        this.hs = hs;
        this.phieu = phieu;
        this.ctDau = ctDau;
        setContentPane(taoNoiDung());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setMinimumSize(new Dimension(760, 700));
        setSize(820, 820);
        setLocationRelativeTo(owner);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public static void hien(Window owner, KhachHang kh, HoSoBenhAn hs,
            List<PhieuKham> phieu, Map<String, ChiTietPhieuKham> ctDau) {
        new DlgInHoSo(owner, kh, hs, phieu, ctDau).setVisible(true);
    }

    private JPanel taoNoiDung() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(BACKGROUND);
        root.setBorder(new EmptyBorder(20, 24, 16, 24));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel tieuDe = new JLabel("Hồ sơ bệnh án điện tử");
        tieuDe.setFont(bold(21));
        tieuDe.setForeground(TEXT);
        JLabel phuDe = new JLabel("Xem trước hồ sơ và lưu ra PDF.");
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
        SurfacePanel phieuWrap = new SurfacePanel(new BorderLayout(0, 14)) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(700, d.height);
            }
        };
        phieuWrap.setBackground(Color.WHITE);
        phieuWrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xB9C5D1)),
                new EmptyBorder(23, 29, 23, 29)));
        phieuWrap.add(taoDauPhieu(), BorderLayout.NORTH);
        phieuWrap.add(taoThanPhieu(), BorderLayout.CENTER);
        phieuWrap.add(taoCuoiPhieu(), BorderLayout.SOUTH);
        return phieuWrap;
    }

    private JPanel taoDauPhieu() {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setOpaque(false);

        JPanel brand = new JPanel(new BorderLayout(10, 0));
        brand.setOpaque(false);
        java.net.URL logoUrl = getClass().getResource("/images/logo.png");
        if (logoUrl != null) {
            ImageIcon logo = new ImageIcon(logoUrl);
            brand.add(new JLabel(new ImageIcon(logo.getImage()
                    .getScaledInstance(68, 48, java.awt.Image.SCALE_SMOOTH))), BorderLayout.WEST);
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
        JLabel maBn = new JLabel("Mã bệnh nhân: " + kh.getMaKH());
        maBn.setFont(bold(11));
        maBn.setForeground(TEXT);
        JLabel maHs = new JLabel("Mã hồ sơ: " + (hs == null ? "—" : hs.getMaHSBA()));
        maHs.setFont(font(10));
        maHs.setForeground(MUTED);
        JLabel ngay = new JLabel(LocalDate.now(ZONE).format(NGAY_IN));
        ngay.setFont(font(10));
        ngay.setForeground(MUTED);
        maBn.setAlignmentX(RIGHT_ALIGNMENT);
        maHs.setAlignmentX(RIGHT_ALIGNMENT);
        ngay.setAlignmentX(RIGHT_ALIGNMENT);
        meta.add(maBn);
        meta.add(Box.createVerticalStrut(3));
        meta.add(maHs);
        meta.add(Box.createVerticalStrut(3));
        meta.add(ngay);
        panel.add(meta, BorderLayout.EAST);
        return panel;
    }

    private JPanel taoThanPhieu() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("HỒ SƠ BỆNH ÁN ĐIỆN TỬ");
        title.setFont(bold(17));
        title.setForeground(TEXT);
        title.setAlignmentX(LEFT_ALIGNMENT);
        title.setBorder(new EmptyBorder(14, 0, 2, 0));
        panel.add(title);

        panel.add(mucPhu("I. THÔNG TIN BỆNH NHÂN"));
        panel.add(hang("Họ và tên", kh.getTenKH()));
        panel.add(luoi2(
                o("Ngày sinh", kh.getNgaySinh() == null ? "—" : kh.getNgaySinh().format(NGAY)),
                o("Giới tính", kh.isGioiTinh() ? "Nam" : "Nữ")));
        panel.add(luoi2(
                o("Số điện thoại", gach(kh.getSdt())),
                o("Email", gach(kh.getEmail()))));
        panel.add(hang("Địa chỉ liên hệ", gach(kh.getDiaChi())));
        String tienSu = hs == null || hs.getTienSuBenh() == null || hs.getTienSuBenh().isBlank()
                ? "Chưa ghi nhận" : hs.getTienSuBenh().trim();
        panel.add(hang("Dị ứng / Tiền sử · Bệnh nền", tienSu));

        panel.add(mucPhu("II. LỊCH SỬ KHÁM & ĐIỀU TRỊ"));
        if (phieu == null || phieu.isEmpty()) {
            panel.add(dong("Chưa có phiếu khám nào.", font(11), MUTED, 0));
        } else {
            for (PhieuKham pk : phieu) {
                panel.add(khoiPhieu(pk, ctDau == null ? null : ctDau.get(pk.getMaPK())));
            }
        }
        return panel;
    }

    private JPanel khoiPhieu(PhieuKham pk, ChiTietPhieuKham ct) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setBorder(new EmptyBorder(8, 0, 6, 0));

        String bs = pk.getLichHen() != null && pk.getLichHen().getNhaSi() != null
                ? "BS. " + pk.getLichHen().getNhaSi().getHoTenNV() : "—";
        String dv = pk.getLichHen() != null && pk.getLichHen().getDichVu() != null
                ? pk.getLichHen().getDichVu().getTenDV() : "—";
        p.add(dong("• Phiếu khám ngày " + pk.getNgayLap().format(NGAY)
                + "  —  " + trangThai(pk.getTrangThai()), bold(12), TEXT, 0));
        p.add(dong(bs + " · " + dv, font(10), MUTED, 12));
        if (ct != null) {
            p.add(dong("Triệu chứng: " + trieuChungNgan(ct.getTrieuChung()), font(11), TEXT, 12));
            p.add(dong("Chẩn đoán: " + chanDoanNgan(ct.getChanDoan()), font(11), TEXT, 12));
        }
        if (pk.getLoiDan() != null && !pk.getLoiDan().isBlank()) {
            p.add(dong("Lời dặn: " + motDong(pk.getLoiDan()), font(11), TEXT, 12));
        }
        return p;
    }

    private JPanel taoCuoiPhieu() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));
        JLabel note = new JLabel("Thông tin y tế · Chỉ sử dụng cho mục đích khám chữa bệnh.");
        note.setFont(font(9));
        note.setForeground(MUTED);
        panel.add(note, BorderLayout.WEST);

        JPanel ky = new JPanel();
        ky.setOpaque(false);
        ky.setLayout(new BoxLayout(ky, BoxLayout.Y_AXIS));
        JLabel ngay = new JLabel(LocalDate.now(ZONE).format(NGAY_IN));
        ngay.setFont(font(10));
        ngay.setForeground(TEXT);
        ngay.setAlignmentX(RIGHT_ALIGNMENT);
        JLabel vai = new JLabel("Nha sĩ phụ trách");
        vai.setFont(bold(11));
        vai.setForeground(TEXT);
        vai.setAlignmentX(RIGHT_ALIGNMENT);
        ky.add(ngay);
        ky.add(Box.createVerticalStrut(36));
        ky.add(vai);
        panel.add(ky, BorderLayout.EAST);
        return panel;
    }

    // ---------------------------------------------------------------- helpers

    private static JLabel mucPhu(String text) {
        JLabel l = new JLabel(text);
        l.setFont(bold(12));
        l.setForeground(TEAL);
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(12, 0, 4, 0));
        return l;
    }

    private static JPanel hang(String nhan, String giaTri) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        p.setBorder(new EmptyBorder(4, 0, 2, 0));
        JLabel l = new JLabel(nhan);
        l.setFont(font(10));
        l.setForeground(MUTED);
        l.setAlignmentX(LEFT_ALIGNMENT);
        JLabel v = new JLabel(giaTri);
        v.setFont(font(12));
        v.setForeground(TEXT);
        v.setAlignmentX(LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(2));
        p.add(v);
        return p;
    }

    private static JPanel luoi2(JPanel a, JPanel b) {
        JPanel p = new JPanel(new GridLayout(1, 2, 16, 0));
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        p.add(a);
        p.add(b);
        return p;
    }

    private static JPanel o(String nhan, String giaTri) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = new JLabel(nhan);
        l.setFont(font(10));
        l.setForeground(MUTED);
        l.setAlignmentX(LEFT_ALIGNMENT);
        JLabel v = new JLabel(giaTri);
        v.setFont(font(12));
        v.setForeground(TEXT);
        v.setAlignmentX(LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(2));
        p.add(v);
        return p;
    }

    private static JLabel dong(String text, java.awt.Font f, Color c, int leftPad) {
        JLabel l = new JLabel(text);
        l.setFont(f);
        l.setForeground(c);
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(2, leftPad, 2, 0));
        return l;
    }

    private static String gach(String s) {
        return s == null || s.isBlank() ? "—" : s.trim();
    }

    private static String motDong(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }

    private static String dongSauNhan(String blob, String nhan) {
        if (blob == null) {
            return null;
        }
        for (String d : blob.split("\\r?\\n")) {
            String t = d.trim();
            if (t.startsWith(nhan)) {
                String v = t.substring(nhan.length()).trim();
                return v.isBlank() ? null : v;
            }
        }
        return null;
    }

    private static String trieuChungNgan(String blob) {
        String v = dongSauNhan(blob, "Triệu chứng:");
        String s = v != null ? v : motDong(blob);
        return s.isBlank() ? "—" : s;
    }

    private static String chanDoanNgan(String blob) {
        String ma = dongSauNhan(blob, "ICD-10:");
        String cd = dongSauNhan(blob, "Chẩn đoán:");
        if (ma != null && cd != null) {
            return ma + " · " + cd;
        }
        if (cd != null) {
            return cd;
        }
        if (ma != null) {
            return ma;
        }
        String s = motDong(blob);
        return s.isBlank() ? "—" : s;
    }

    private static String trangThai(TrangThaiPhieuKham tt) {
        return switch (tt) {
            case HOAN_TAT -> "Hoàn tất";
            case DANG_KHAM -> "Đang khám";
            default -> "Chờ khám";
        };
    }

    private void luuPdf() {
        if (phieuPanel == null || phieuPanel.getWidth() <= 0 || phieuPanel.getHeight() <= 0) {
            JOptionPane.showMessageDialog(this, "Hồ sơ chưa sẵn sàng để lưu.",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chon = new JFileChooser();
        chon.setSelectedFile(new File("ho-so-" + kh.getMaKH() + ".pdf"));
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
                            page.getMediaBox().getHeight() - margin - veCao, veRong, veCao);
                }
                doc.save(file);
            }
            JOptionPane.showMessageDialog(this, "Đã lưu hồ sơ PDF:\n" + file.getAbsolutePath(),
                    "Lưu PDF thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | SecurityException e) {
            JOptionPane.showMessageDialog(this, "Không thể lưu file PDF: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
