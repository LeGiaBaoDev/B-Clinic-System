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
import com.quanlynhakhoa.entity.LichHen;
import com.quanlynhakhoa.entity.PhieuKham;
import com.quanlynhakhoa.entity.enums.TrangThaiPhieuKham;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

public class DlgChiTietBenhAn extends JDialog {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter NGAY_IN =
            DateTimeFormatter.ofPattern("'Ngày' dd 'tháng' MM 'năm' yyyy", Locale.of("vi", "VN"));

    private final transient KhachHang kh;
    private final transient HoSoBenhAn hs;
    private final transient PhieuKham phieu;
    private final transient List<ChiTietPhieuKham> chiTiet;
    private JPanel phieuPanel;

    private DlgChiTietBenhAn(Window owner, KhachHang kh, HoSoBenhAn hs,
            PhieuKham phieu, List<ChiTietPhieuKham> chiTiet) {
        super(owner, "Hồ sơ bệnh án điện tử", ModalityType.APPLICATION_MODAL);
        this.kh = kh;
        this.hs = hs;
        this.phieu = phieu;
        this.chiTiet = chiTiet;
        setContentPane(taoNoiDung());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setMinimumSize(new Dimension(780, 680));
        setSize(840, 820);
        setLocationRelativeTo(owner);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public static void hien(Window owner, KhachHang kh, HoSoBenhAn hs,
            PhieuKham phieu, List<ChiTietPhieuKham> chiTiet) {
        new DlgChiTietBenhAn(owner, kh, hs, phieu, chiTiet).setVisible(true);
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
        JLabel phuDe = new JLabel("Chi tiết dịch vụ đã thực hiện.");
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
        scr.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
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
        SurfacePanel wrap = new SurfacePanel(new BorderLayout(0, 14)) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(660, d.height);
            }
        };
        wrap.setBackground(Color.WHITE);
        wrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xB9C5D1)),
                new EmptyBorder(23, 29, 23, 29)));
        wrap.add(taoDauPhieu(), BorderLayout.NORTH);
        wrap.add(taoThanPhieu(), BorderLayout.CENTER);
        wrap.add(taoCuoiPhieu(), BorderLayout.SOUTH);
        return wrap;
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
                    .getScaledInstance(-1, 46, java.awt.Image.SCALE_SMOOTH))), BorderLayout.WEST);
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
        JLabel maBn = new JLabel("Mã bệnh nhân: " + (kh == null ? "—" : kh.getMaKH()));
        maBn.setFont(bold(11));
        maBn.setForeground(TEXT);
        JLabel maHs = new JLabel("Mã hồ sơ: " + (hs == null ? "—" : hs.getMaHSBA()));
        maHs.setFont(font(10));
        maHs.setForeground(MUTED);
        JLabel ngay = new JLabel(phieu.getNgayLap().format(NGAY_IN));
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

        JLabel title = new JLabel("PHIẾU KHÁM & ĐIỀU TRỊ");
        title.setFont(bold(17));
        title.setForeground(TEXT);
        title.setAlignmentX(LEFT_ALIGNMENT);
        title.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        title.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        title.setBorder(new EmptyBorder(14, 0, 6, 0));
        panel.add(title);

        JPanel canhBao = canhBaoYTe();
        if (canhBao != null) {
            panel.add(canhBao);
            panel.add(Box.createVerticalStrut(2));
        }

        panel.add(mucPhu("I. THÔNG TIN BỆNH NHÂN"));
        panel.add(hang("Họ và tên", kh == null ? "—" : kh.getTenKH()));
        panel.add(luoi2(
                o("Ngày sinh", kh == null || kh.getNgaySinh() == null ? "—" : kh.getNgaySinh().format(NGAY)),
                o("Giới tính", kh != null && kh.isGioiTinh() ? "Nam" : "Nữ")));
        panel.add(luoi2(
                o("Số điện thoại", gach(kh == null ? null : kh.getSdt())),
                o("Email", gach(kh == null ? null : kh.getEmail()))));
        panel.add(hang("Địa chỉ liên hệ", gach(kh == null ? null : kh.getDiaChi())));
        String tienSu = hs == null || hs.getTienSuBenh() == null || hs.getTienSuBenh().isBlank()
                ? "Chưa ghi nhận" : hs.getTienSuBenh().trim();
        panel.add(hang("Dị ứng / Tiền sử · Bệnh nền", tienSu));

        panel.add(mucPhu("II. KHÁM & CHẨN ĐOÁN"));
        LichHen lh = phieu.getLichHen();
        String bs = lh == null || lh.getNhaSi() == null ? "—" : "BS. " + lh.getNhaSi().getHoTenNV();
        panel.add(dong("Lượt khám hoàn tất · " + phieu.getNgayLap().format(NGAY_GIO)
                + "  ·  " + bs, font(10), MUTED, 0));
        panel.add(Box.createVerticalStrut(4));
        ChiTietPhieuKham ct0 = chiTiet == null || chiTiet.isEmpty() ? null : chiTiet.get(0);
        panel.add(hang("Triệu chứng ghi nhận",
                ct0 == null ? "—" : clean(sauNhan(ct0.getTrieuChung(), "Triệu chứng:"), ct0.getTrieuChung())));
        panel.add(hang("Chẩn đoán", ct0 == null ? "—" : chanDoanText(ct0.getChanDoan())));
        if (ct0 != null && ct0.getKetQuaDieuTri() != null && !ct0.getKetQuaDieuTri().isBlank()) {
            panel.add(hang("Kết quả điều trị", motDong(ct0.getKetQuaDieuTri())));
        }

        panel.add(mucPhu("III. DỊCH VỤ ĐÃ THỰC HIỆN & CHI PHÍ"));
        panel.add(taoBangDichVu());
        if (phieu.getLoiDan() != null && !phieu.getLoiDan().isBlank()) {
            panel.add(Box.createVerticalStrut(10));
            panel.add(hang("Lời dặn sau điều trị", motDong(phieu.getLoiDan())));
        }
        return panel;
    }

    private JPanel taoBangDichVu() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(true);
        p.setBackground(Color.WHITE);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setBorder(BorderFactory.createMatteBorder(1, 1, 0, 0, new Color(0xB9C5D1)));
        double[] w = {0.08, 0.44, 0.18, 0.10, 0.20};
        String[] header = {"STT", "Dịch vụ điều trị", "Đơn giá", "SL", "Thành tiền"};
        themDongBang(p, 0, header, w, true);
        double tong = 0;
        int soDong = chiTiet == null ? 0 : chiTiet.size();
        if (soDong == 0) {
            themDongBang(p, 1, new String[]{"", "Chưa có dịch vụ điều trị.", "", "", ""}, w, false);
        } else {
            for (int i = 0; i < soDong; i++) {
                ChiTietPhieuKham ct = chiTiet.get(i);
                tong += ct.getThanhTien();
                themDongBang(p, i + 1, new String[]{
                    String.valueOf(i + 1),
                    ct.getDichVu() == null ? "—" : ct.getDichVu().getTenDV(),
                    money(ct.getDonGia()),
                    String.valueOf(ct.getSoLuong()),
                    money(ct.getThanhTien())}, w, false);
            }
        }
        themDongTong(p, soDong + 1, w, tong);
        return p;
    }

    /** Dòng TỔNG CỘNG: nhãn gộp 4 cột đầu, giá trị ở cột Thành tiền. */
    private void themDongTong(JPanel p, int y, double[] w, double tong) {
        JLabel nhan = new JLabel("TỔNG CỘNG");
        nhan.setFont(bold(10));
        nhan.setForeground(TEXT);
        nhan.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        JPanel cNhan = new JPanel(new BorderLayout());
        cNhan.setBackground(new Color(0xF1F6F7));
        cNhan.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(0xB9C5D1)),
                new EmptyBorder(6, 8, 6, 8)));
        cNhan.add(nhan, BorderLayout.CENTER);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = y;
        gc.gridwidth = 4;
        gc.weightx = w[0] + w[1] + w[2] + w[3];
        gc.fill = GridBagConstraints.BOTH;
        p.add(cNhan, gc);

        JLabel giaTri = new JLabel(money(tong));
        giaTri.setFont(bold(11));
        giaTri.setForeground(TEAL);
        giaTri.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        JPanel cGt = new JPanel(new BorderLayout());
        cGt.setBackground(new Color(0xF1F6F7));
        cGt.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(0xB9C5D1)),
                new EmptyBorder(6, 6, 6, 6)));
        cGt.add(giaTri, BorderLayout.CENTER);
        GridBagConstraints gg = new GridBagConstraints();
        gg.gridx = 4;
        gg.gridy = y;
        gg.weightx = w[4];
        gg.fill = GridBagConstraints.BOTH;
        p.add(cGt, gg);
    }

    private void themDongBang(JPanel p, int y, String[] giaTri, double[] w, boolean header) {
        for (int i = 0; i < giaTri.length; i++) {
            int rong = Math.max(34, (int) Math.round(500 * w[i]));
            JLabel l = new JLabel("<html><div style='width:" + rong + "px'>"
                    + escape(giaTri[i]) + "</div></html>");
            l.setFont(header ? bold(9) : font(9));
            l.setForeground(TEXT);
            l.setVerticalAlignment(javax.swing.SwingConstants.TOP);
            l.setHorizontalAlignment(i == 1 ? javax.swing.SwingConstants.LEFT
                    : (i == 2 || i == 4) ? javax.swing.SwingConstants.RIGHT
                    : javax.swing.SwingConstants.CENTER);
            JPanel cell = new JPanel(new BorderLayout());
            cell.setBackground(header ? new Color(0xF1F6F7) : Color.WHITE);
            cell.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(0xB9C5D1)),
                    new EmptyBorder(5, 3, 5, 3)));
            cell.add(l, BorderLayout.CENTER);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = i;
            c.gridy = y;
            c.weightx = w[i];
            c.fill = GridBagConstraints.BOTH;
            c.anchor = GridBagConstraints.NORTHWEST;
            p.add(cell, c);
        }
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br>");
    }

    private JPanel taoCuoiPhieu() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));
        JLabel note = new JLabel("Thông tin y tế · Chỉ gồm lượt khám đã hoàn tất · Bản in lúc "
                + LocalDate.now(ZONE).format(NGAY) + ".");
        note.setFont(font(9));
        note.setForeground(MUTED);
        panel.add(note, BorderLayout.WEST);

        JPanel ky = new JPanel();
        ky.setOpaque(false);
        ky.setLayout(new BoxLayout(ky, BoxLayout.Y_AXIS));
        JLabel ngay = new JLabel(phieu.getNgayLap().toLocalDate().format(NGAY_IN));
        ngay.setFont(font(10));
        ngay.setForeground(TEXT);
        ngay.setAlignmentX(RIGHT_ALIGNMENT);
        JLabel vai = new JLabel("Nha sĩ phụ trách");
        vai.setFont(bold(11));
        vai.setForeground(TEXT);
        vai.setAlignmentX(RIGHT_ALIGNMENT);
        JLabel ten = new JLabel(phieu.getLichHen() == null
                || phieu.getLichHen().getNhaSi() == null ? "—"
                : "BS. " + phieu.getLichHen().getNhaSi().getHoTenNV());
        ten.setFont(font(10));
        ten.setForeground(MUTED);
        ten.setAlignmentX(RIGHT_ALIGNMENT);
        ky.add(ngay);
        ky.add(Box.createVerticalStrut(30));
        ky.add(vai);
        ky.add(Box.createVerticalStrut(4));
        ky.add(ten);
        panel.add(ky, BorderLayout.EAST);
        return panel;
    }

    private static String trangThai(TrangThaiPhieuKham tt) {
        return switch (tt) {
            case CHO_KHAM -> "Chờ khám";
            case DANG_KHAM -> "Đang khám";
            case HOAN_TAT -> "Hoàn tất";
        };
    }

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
        JLabel v = new JLabel("<html><div style='width:580px'>" + escape(gach(giaTri)) + "</div></html>");
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
        JLabel v = new JLabel("<html><div style='width:270px'>" + escape(gach(giaTri)) + "</div></html>");
        v.setFont(font(12));
        v.setForeground(TEXT);
        v.setAlignmentX(LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(2));
        p.add(v);
        return p;
    }

    private static JLabel dong(String text, java.awt.Font f, Color c, int leftPad) {
        JLabel l = new JLabel("<html><div style='width:" + Math.max(120, 580 - leftPad)
                + "px'>" + escape(text) + "</div></html>");
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

    private static String money(double v) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.of("vi", "VN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(BigDecimal.valueOf(v).setScale(0, RoundingMode.HALF_UP)) + " đ";
    }

    /** Hộp cảnh báo y tế (dị ứng/bệnh nền) khi có; null nếu "chưa ghi nhận". */
    private JPanel canhBaoYTe() {
        String t = hs == null ? null : hs.getTienSuBenh();
        if (t == null || t.isBlank() || t.toLowerCase(Locale.ROOT).contains("chưa ghi nhận")) {
            return null;
        }
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(new Color(0xFFF2DB));
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, new Color(0xD9930A)),
                new EmptyBorder(7, 10, 7, 10)));
        JLabel l = new JLabel("<html><div style='width:560px'><b>LƯU Ý Y TẾ:</b> "
                + escape(t.trim()) + "</div></html>");
        l.setFont(font(10));
        l.setForeground(new Color(0x7A4A00));
        p.add(l, BorderLayout.CENTER);
        return p;
    }

    private static String sauNhan(String blob, String nhan) {
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

    private static String clean(String v, String blob) {
        String s = v != null ? v : motDong(blob);
        return s == null || s.isBlank() ? "—" : s;
    }

    private static String chanDoanText(String blob) {
        String ma = sauNhan(blob, "ICD-10:");
        String cd = sauNhan(blob, "Chẩn đoán:");
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

    private void luuPdf() {
        if (phieuPanel == null || phieuPanel.getWidth() <= 0 || phieuPanel.getHeight() <= 0) {
            JOptionPane.showMessageDialog(this, "Hồ sơ chưa sẵn sàng để lưu.",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chon = new JFileChooser();
        chon.setSelectedFile(new File("chi-tiet-" + phieu.getMaPK() + ".pdf"));
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
