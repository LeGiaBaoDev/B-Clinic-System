package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.dao.DichVuDAO;
import com.quanlynhakhoa.entity.DichVu;
import com.quanlynhakhoa.entity.LoaiDichVu;
import com.quanlynhakhoa.entity.enums.MucThue;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import com.quanlynhakhoa.ui.dialog.DlgDichVu;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class PnlCapNhatDichVu extends JPanel {

    /** Hành động lưu (DichVuDAO.capNhat); tách ra để test không cần DB. */
    @FunctionalInterface
    public interface LuuDichVu {
        boolean luu(DichVu dv);
    }

    private static final Logger LOG = Logger.getLogger(PnlCapNhatDichVu.class.getName());
    private static final Color DO = new Color(0xC94C48);
    private static final Color NEN_O_KHOA = new Color(0xF1F4F7);
    private static final Color VANG_NEN = new Color(0xFFF4D6);
    private static final Color VANG_VIEN = new Color(0xF2D58A);
    private static final Color VANG_CHU = new Color(0x8A6100);
    private static final String[] TEN_DANH_MUC = {"-- Chọn danh mục --",
        LoaiDichVu.DIEU_TRI, LoaiDichVu.VAT_TU, LoaiDichVu.THAM_MY};
    private static final MucThue[] MUC_THUE = {null, MucThue.THUE_0, MucThue.THUE_5, MucThue.THUE_10};

    /** Bản đang lưu trong DB lúc mở trang (bản sao, không sửa đối tượng của bảng). */
    private final DichVu banGoc;
    private final LuuDichVu luu;
    private final Consumer<DichVu> daLuu;
    private final Runnable dong;

    // --- Ô nhập (package-private để test) ---
    final JTextField txtMaDV = new JTextField();
    final JComboBox<String> cboDanhMuc = new JComboBox<>(TEN_DANH_MUC);
    final JTextField txtTenDV = new JTextField();
    final JTextArea txaLoTrinh = new JTextArea(3, 20);
    final JTextField txtThoiLuong = new JTextField();
    final JTextField txtDonGia = new JTextField();
    final JTextField txtTrangThai = new JTextField("Đang cung cấp");
    final JLabel[] lblThue = {new JLabel("KCT"), new JLabel("5%"), new JLabel("10%")};
    final JPanel pnlGoiY = new JPanel(new DlgDichVu.WrapLayout(FlowLayout.LEFT, 6, 6));

    // --- Dòng lỗi dưới từng ô ---
    final JLabel lblLoiTen = loiLabel("loiTen");
    final JLabel lblLoiDanhMuc = loiLabel("loiDanhMuc");
    final JLabel lblLoiThoiLuong = loiLabel("loiThoiLuong");
    final JLabel lblLoiDonGia = loiLabel("loiDonGia");

    // --- Thanh nút, thông báo, breadcrumb, thẻ bên phải ---
    final JButton btnLuu = PnlTongQuanQuanLy.button("Lưu thay đổi", LineIcon.Type.CHECK, true);
    final JButton btnHuy = PnlTongQuanQuanLy.button("Hủy", null, false);
    final JButton btnQuayLai = PnlTongQuanQuanLy.button("←  Quay lại danh sách", null, false);
    final JLabel lblThongBao = new JLabel(" ");
    final JLabel lblDuongDan = new JLabel();
    final JLabel lblGiaDonGia = new JLabel();
    final JLabel lblGiaThueTen = new JLabel();
    final JLabel lblGiaThue = new JLabel();
    final JLabel lblGiaSauThue = new JLabel();
    final JPanel pnlCanhBao = new JPanel(new BorderLayout(8, 0));
    final JLabel lblCanhBao = new JLabel();

    /** Lộ trình đã lưu trong DB (mới nhất trước) để gợi ý. */
    private List<String> loTrinhDaLuu = List.of();
    /** Các ô người dùng đã sửa — chỉ báo lỗi cho những ô này (bấm Lưu thì coi như đã chạm hết). */
    private final Set<JComponent> daCham = new HashSet<>();
    private boolean dangLuu;
    /** true khi code đang điền dữ liệu ban đầu, để không coi là người dùng đã sửa. */
    private boolean dangDienBanDau;

    /** Mở trang dùng DAO thật. */
    public PnlCapNhatDichVu(DichVu dichVu, Consumer<DichVu> daLuu, Runnable dong) {
        this(dichVu, new DichVuDAO()::capNhat, new DichVuDAO()::layLoTrinhDaLuu, daLuu, dong);
    }

    PnlCapNhatDichVu(DichVu dichVu, LuuDichVu luu, Supplier<List<String>> layLoTrinh,
            Consumer<DichVu> daLuu, Runnable dong) {
        super(new BorderLayout(0, 16));
        this.banGoc = new DichVu(Objects.requireNonNull(dichVu));
        this.luu = Objects.requireNonNull(luu);
        this.daLuu = Objects.requireNonNull(daLuu);
        this.dong = Objects.requireNonNull(dong);
        setName("trangCapNhatDichVu");
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 24, 24));
        add(taoTieuDe(), BorderLayout.NORTH);

        JPanel than = new JPanel(new BorderLayout(16, 0));
        than.setOpaque(false);
        JPanel trai = new JPanel(new BorderLayout());
        trai.setOpaque(false);
        trai.add(taoTheThongTin(), BorderLayout.NORTH);
        trai.add(taoThanhNut(), BorderLayout.CENTER);
        than.add(trai, BorderLayout.CENTER);
        than.add(taoCotPhai(), BorderLayout.EAST);
        // Cuộn được để màn hình thấp (1366×768) vẫn xem hết.
        JScrollPane cuon = new JScrollPane(than);
        cuon.setBorder(null);
        cuon.getViewport().setBackground(BACKGROUND);
        cuon.getVerticalScrollBar().setUnitIncrement(16);
        add(cuon, BorderLayout.CENTER);

        dienDuLieuBanDau();
        ganSuKien();
        capNhatTatCa();
        napGoiY(Objects.requireNonNull(layLoTrinh));
    }

    // ===================== Dựng giao diện =====================

    private JPanel taoTieuDe() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        JPanel chu = new JPanel();
        chu.setOpaque(false);
        chu.setLayout(new BoxLayout(chu, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Chỉnh sửa dịch vụ");
        title.setFont(bold(24));
        title.setForeground(TEXT);
        lblDuongDan.setName("duongDan");
        lblDuongDan.setFont(font(12));
        lblDuongDan.setForeground(MUTED);
        chu.add(title);
        chu.add(Box.createVerticalStrut(4));
        chu.add(lblDuongDan);
        header.add(chu, BorderLayout.CENTER);
        btnQuayLai.setName("quayLaiDanhSach");
        JPanel phai = new JPanel(new GridBagLayout()); // căn giữa nút theo chiều dọc
        phai.setOpaque(false);
        phai.add(btnQuayLai);
        header.add(phai, BorderLayout.EAST);
        return header;
    }

    /** Thẻ "Thông tin dịch vụ": lưới 2 cột như ảnh thiết kế. */
    private JPanel taoTheThongTin() {
        SurfacePanel the = new SurfacePanel(new GridBagLayout());
        the.setBorder(new EmptyBorder(18, 20, 20, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.weightx = 1;

        JLabel tieuDe = new JLabel("Thông tin dịch vụ");
        tieuDe.setFont(bold(16));
        tieuDe.setForeground(TEXT);
        dat(the, g, tieuDe, 0, 0, 2, new Insets(0, 0, 14, 0));

        // Hàng 1: Mã dịch vụ (khóa) | Danh mục *
        dat(the, g, nhan("Mã dịch vụ"), 0, 1, 1, new Insets(0, 0, 6, 8));
        dat(the, g, nhan("Danh mục *"), 1, 1, 1, new Insets(0, 8, 6, 0));
        txtMaDV.setName("maDichVu");
        khoa(txtMaDV, "Mã dịch vụ không đổi được.");
        cboDanhMuc.setName("danhMuc");
        cboDanhMuc.setToolTipText("Đổi danh mục thì mức thuế GTGT đổi theo; hóa đơn cũ giữ mức thuế lúc phát sinh.");
        dat(the, g, o(txtMaDV), 0, 2, 1, new Insets(0, 0, 0, 8));
        dat(the, g, o(cboDanhMuc), 1, 2, 1, new Insets(0, 8, 0, 0));
        dat(the, g, new JLabel(" "), 0, 3, 1, new Insets(2, 0, 10, 8));
        dat(the, g, lblLoiDanhMuc, 1, 3, 1, new Insets(2, 8, 10, 0));

        // Hàng 2: Tên dịch vụ * (trải 2 cột)
        dat(the, g, nhan("Tên dịch vụ *"), 0, 4, 2, new Insets(0, 0, 6, 0));
        txtTenDV.setName("tenDichVu");
        dat(the, g, o(txtTenDV), 0, 5, 2, new Insets(0, 0, 0, 0));
        dat(the, g, lblLoiTen, 0, 6, 2, new Insets(2, 0, 10, 0));

        // Hàng 3: Lộ trình dịch vụ (vị trí ô "Mô tả" trong ảnh) + gợi ý ngay bên dưới
        dat(the, g, nhan("Lộ trình dịch vụ (nếu điều trị nhiều buổi)"), 0, 7, 2, new Insets(0, 0, 6, 0));
        txaLoTrinh.setName("loTrinh");
        txaLoTrinh.setFont(font(13));
        txaLoTrinh.setLineWrap(true);
        txaLoTrinh.setWrapStyleWord(true);
        txaLoTrinh.setBorder(new EmptyBorder(8, 10, 8, 10));
        txaLoTrinh.setToolTipText("Mỗi buổi một dòng. Để trống nếu dịch vụ chỉ thực hiện 1 buổi.");
        JScrollPane scrLoTrinh = new JScrollPane(txaLoTrinh);
        scrLoTrinh.setPreferredSize(new Dimension(0, 80));
        dat(the, g, scrLoTrinh, 0, 8, 2, new Insets(0, 0, 0, 0));
        pnlGoiY.setName("goiYLoTrinh");
        pnlGoiY.setOpaque(false);
        dat(the, g, pnlGoiY, 0, 9, 2, new Insets(4, -6, 6, 0)); // -6: bù lề trái mặc định của WrapLayout

        // Hàng 4: Thời lượng * | Đơn giá *
        dat(the, g, nhan("Thời lượng (phút) *"), 0, 10, 1, new Insets(0, 0, 6, 8));
        dat(the, g, nhan("Đơn giá (VNĐ) *"), 1, 10, 1, new Insets(0, 8, 6, 0));
        txtThoiLuong.setName("thoiLuong");
        txtDonGia.setName("donGia");
        dat(the, g, o(txtThoiLuong), 0, 11, 1, new Insets(0, 0, 0, 8));
        dat(the, g, o(txtDonGia), 1, 11, 1, new Insets(0, 8, 0, 0));
        dat(the, g, lblLoiThoiLuong, 0, 12, 1, new Insets(2, 0, 10, 8));
        dat(the, g, lblLoiDonGia, 1, 12, 1, new Insets(2, 8, 10, 0));

        // Hàng 5: Thuế GTGT (tự theo danh mục) | Trạng thái (chỉ hiển thị)
        dat(the, g, nhan("Thuế GTGT"), 0, 13, 1, new Insets(0, 0, 6, 8));
        dat(the, g, nhan("Trạng thái"), 1, 13, 1, new Insets(0, 8, 6, 0));
        JPanel thue = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        thue.setOpaque(false);
        thue.setName("thueGTGT");
        for (JLabel l : lblThue) {
            l.setFont(bold(12));
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setOpaque(true);
            l.setPreferredSize(new Dimension(52, 38));
            l.setToolTipText("Mức thuế đi theo danh mục, không chọn riêng (Bảng 4.5).");
            thue.add(l);
        }
        txtTrangThai.setName("trangThai");
        khoa(txtTrangThai, "Ngừng / mở lại cung cấp ở danh sách dịch vụ (UC008).");
        dat(the, g, thue, 0, 14, 1, new Insets(0, -8, 0, 8)); // -8: bù khoảng đầu hàng của FlowLayout
        dat(the, g, o(txtTrangThai), 1, 14, 1, new Insets(0, 8, 0, 0));
        dat(the, g, ghiChu("Tự động theo danh mục."), 0, 15, 1, new Insets(4, 0, 0, 8));
        dat(the, g, ghiChu("Đổi trạng thái bằng Ngừng / Mở lại cung cấp."), 1, 15, 1, new Insets(4, 8, 0, 0));
        return the;
    }

    private JPanel taoThanhNut() {
        JPanel dong = new JPanel(new BorderLayout(12, 0));
        dong.setOpaque(false);
        dong.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(16, 0, 0, 0), BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER), new EmptyBorder(14, 0, 0, 0))));
        lblThongBao.setName("thongBao");
        lblThongBao.setFont(font(12));
        lblThongBao.setForeground(DO);
        dong.add(lblThongBao, BorderLayout.CENTER);
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        nut.setOpaque(false);
        btnHuy.setName("huyCapNhat");
        btnLuu.setName("luuThayDoi");
        btnHuy.setPreferredSize(new Dimension(80, 40));
        btnLuu.setPreferredSize(new Dimension(150, 40));
        nut.add(btnHuy);
        nut.add(btnLuu);
        dong.add(nut, BorderLayout.EAST);
        JPanel boc = new JPanel(new BorderLayout());
        boc.setOpaque(false);
        boc.add(dong, BorderLayout.NORTH);
        return boc;
    }

    /** Cột phải: thẻ giá dự kiến, hộp cảnh báo bản nháp sai, thẻ quy tắc cập nhật. */
    private JPanel taoCotPhai() {
        JPanel cot = new JPanel();
        cot.setOpaque(false);
        cot.setLayout(new BoxLayout(cot, BoxLayout.Y_AXIS));
        cot.setPreferredSize(new Dimension(360, 0));

        SurfacePanel gia = new SurfacePanel(new GridBagLayout());
        gia.setBorder(new EmptyBorder(18, 20, 18, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        JLabel tieuDe = new JLabel("Giá thanh toán dự kiến");
        tieuDe.setFont(bold(16));
        tieuDe.setForeground(TEXT);
        dat(gia, g, tieuDe, 0, 0, 2, new Insets(0, 0, 10, 0));
        JLabel nhanBanNhap = new JLabel(banGoc.getMaDV() + " · Bản nháp chỉnh sửa");
        nhanBanNhap.setFont(bold(11));
        nhanBanNhap.setForeground(VANG_CHU);
        nhanBanNhap.setBackground(VANG_NEN);
        nhanBanNhap.setOpaque(true);
        nhanBanNhap.setBorder(new EmptyBorder(3, 8, 3, 8));
        JPanel bocNhan = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bocNhan.setOpaque(false);
        bocNhan.add(nhanBanNhap);
        dat(gia, g, bocNhan, 0, 1, 2, new Insets(0, 0, 14, 0));
        dongGia(gia, g, 2, new JLabel("Đơn giá"), lblGiaDonGia);
        dongGia(gia, g, 3, lblGiaThueTen, lblGiaThue);
        JSeparator gach = new JSeparator();
        gach.setForeground(BORDER);
        dat(gia, g, gach, 0, 4, 2, new Insets(10, 0, 10, 0));
        JLabel sauThue = new JLabel("Giá sau thuế");
        sauThue.setFont(bold(13));
        sauThue.setForeground(TEXT);
        lblGiaSauThue.setName("giaSauThue");
        lblGiaSauThue.setFont(bold(24));
        lblGiaSauThue.setForeground(TEAL);
        lblGiaSauThue.setHorizontalAlignment(SwingConstants.RIGHT);
        dat(gia, g, sauThue, 0, 5, 1, new Insets(0, 0, 0, 0));
        dat(gia, g, lblGiaSauThue, 1, 5, 1, new Insets(0, 0, 0, 0));
        gia.setAlignmentX(LEFT_ALIGNMENT);
        gia.setMaximumSize(new Dimension(Integer.MAX_VALUE, gia.getPreferredSize().height));
        cot.add(gia);
        cot.add(Box.createVerticalStrut(16));

        // Hộp vàng: chỉ hiện khi bản nháp có ô sai, nhắc rằng giá trị đã lưu chưa bị đổi.
        pnlCanhBao.setName("canhBaoBanNhap");
        pnlCanhBao.setBackground(VANG_NEN);
        pnlCanhBao.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(VANG_VIEN), new EmptyBorder(12, 14, 12, 14)));
        JLabel icon = new JLabel("⚠");
        icon.setForeground(VANG_CHU);
        icon.setVerticalAlignment(SwingConstants.TOP);
        lblCanhBao.setFont(font(12));
        lblCanhBao.setForeground(VANG_CHU);
        pnlCanhBao.add(icon, BorderLayout.WEST);
        pnlCanhBao.add(lblCanhBao, BorderLayout.CENTER);
        pnlCanhBao.setAlignmentX(LEFT_ALIGNMENT);
        pnlCanhBao.setVisible(false);
        cot.add(pnlCanhBao);
        cot.add(Box.createVerticalStrut(16));

        SurfacePanel quyTac = new SurfacePanel(new BorderLayout(0, 8));
        quyTac.setBorder(new EmptyBorder(18, 20, 18, 20));
        JLabel tdQuyTac = new JLabel("Quy tắc cập nhật");
        tdQuyTac.setFont(bold(16));
        tdQuyTac.setForeground(TEXT);
        JLabel noiDung = new JLabel("<html>Giá mới chỉ áp dụng cho các lượt chỉ định / lịch hẹn tạo sau khi lưu."
                + " Không thay đổi phiếu khám, hóa đơn đã phát sinh."
                + "<br><br>Tên không được trùng với dịch vụ khác đang cung cấp."
                + "<br><br>Không đổi được thời lượng khi dịch vụ còn lịch hẹn đã đặt hoặc đang khám.</html>");
        noiDung.setFont(font(12));
        noiDung.setForeground(MUTED);
        quyTac.add(tdQuyTac, BorderLayout.NORTH);
        quyTac.add(noiDung, BorderLayout.CENTER);
        quyTac.setAlignmentX(LEFT_ALIGNMENT);
        cot.add(quyTac);
        return cot;
    }

    private void dongGia(JPanel p, GridBagConstraints g, int hang, JLabel trai, JLabel phai) {
        trai.setFont(font(13));
        trai.setForeground(MUTED);
        phai.setFont(bold(13));
        phai.setForeground(TEXT);
        phai.setHorizontalAlignment(SwingConstants.RIGHT);
        dat(p, g, trai, 0, hang, 1, new Insets(0, 0, 8, 0));
        dat(p, g, phai, 1, hang, 1, new Insets(0, 0, 8, 0));
    }

    // ===================== Dữ liệu ban đầu, sự kiện =====================

    
    private void dienDuLieuBanDau() {
        dangDienBanDau = true;
        try {
            txtMaDV.setText(banGoc.getMaDV());
            txtTenDV.setText(banGoc.getTenDV());
            txaLoTrinh.setText(banGoc.getLoTrinhDichVu() == null ? "" : banGoc.getLoTrinhDichVu());
            txtThoiLuong.setText(String.valueOf(banGoc.getThoiLuong()));
            txtDonGia.setText(soTien(banGoc.getDonGia()));
            for (int i = 1; i < TEN_DANH_MUC.length; i++) {
                if (TEN_DANH_MUC[i].equals(banGoc.getLoaiDichVu().getTenLoaiDV())) {
                    cboDanhMuc.setSelectedIndex(i);
                }
            }
        } finally {
            dangDienBanDau = false;
        }
    }

    private void ganSuKien() {
        // Mọi thay đổi: đánh dấu ô đã chạm, kiểm tra lại, tính lại giá, bật/tắt nút lưu.
        nghe(txtTenDV);
        nghe(txtThoiLuong);
        nghe(txtDonGia);
        nghe(txaLoTrinh);
        cboDanhMuc.addActionListener(e -> {
            if (!dangDienBanDau) {
                daCham.add(cboDanhMuc);
            }
            capNhatTatCa();
        });
        btnLuu.addActionListener(e -> luuThayDoi());
        btnHuy.addActionListener(e -> yeuCauDong());
        btnQuayLai.addActionListener(e -> yeuCauDong());
    }

    private void nghe(javax.swing.text.JTextComponent o) {
        o.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                doi();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                doi();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                doi();
            }

            private void doi() {
                if (dangDienBanDau) {
                    return;
                }
                daCham.add(o);
                if (o == txaLoTrinh) {
                    veGoiY();
                }
                capNhatTatCa();
            }
        });
    }

    // ===================== Kiểm tra, tính giá, so sánh thay đổi =====================

    /** Kiểm tra lại toàn form, tô lỗi ô đã chạm, cảnh báo bản nháp, tính giá, bật/tắt nút lưu. */
    private void capNhatTatCa() {
        String loiTen = loiTen();
        String loiDanhMuc = cboDanhMuc.getSelectedIndex() <= 0 ? "Vui lòng chọn danh mục." : null;
        String loiThoiLuong = loiThoiLuong();
        String loiDonGia = loiDonGia();
        hienLoi(txtTenDV, lblLoiTen, loiTen, null);
        hienLoi(cboDanhMuc, lblLoiDanhMuc, loiDanhMuc, null);
        hienLoi(txtThoiLuong, lblLoiThoiLuong, loiThoiLuong, null);
        hienLoi(txtDonGia, lblLoiDonGia, loiDonGia, "Giá chưa bao gồm thuế GTGT.");

        boolean hopLe = loiTen == null && loiDanhMuc == null && loiThoiLuong == null && loiDonGia == null;
        boolean coThayDoi = hopLe && !DichVuDAO.moTaThayDoi(banGoc, docForm()).isEmpty();
        btnLuu.setEnabled(hopLe && coThayDoi && !dangLuu);

        if (!dangLuu) {
            lblThongBao.setForeground(DO);
            if (!hopLe && !daCham.isEmpty()) {
                lblThongBao.setText("Vui lòng kiểm tra các trường được đánh dấu.");
            } else if (hopLe && !coThayDoi && !daCham.isEmpty()) {
                lblThongBao.setForeground(MUTED);
                lblThongBao.setText("Không có thay đổi để lưu.");
            } else {
                lblThongBao.setText(" ");
            }
        }
        hienCanhBao(loiTen, loiThoiLuong, loiDonGia);
        veThue();
        tinhGia();
        lblDuongDan.setText("Dịch vụ / " + banGoc.getMaDV() + " · " + banGoc.getTenDV()
                + (coThayDoi || (!hopLe && !daCham.isEmpty()) ? " · Thay đổi chưa được lưu" : ""));
    }

    
    private String loiTen() {
        String ten = txtTenDV.getText().trim();
        if (ten.isEmpty()) {
            return "Vui lòng nhập tên dịch vụ.";
        }
        return ten.length() > DichVu.TEN_TOI_DA ? "Tên dịch vụ tối đa " + DichVu.TEN_TOI_DA + " ký tự." : null;
    }

    /** Thời lượng là số nguyên > 0 phút (báo cáo). */
    private String loiThoiLuong() {
        Integer phut = docSoNguyen(txtThoiLuong.getText());
        if (phut == null) {
            return "Thời lượng phải là số phút (số nguyên).";
        }
        return phut <= 0 ? "Thời lượng phải lớn hơn 0 phút." : null;
    }

    
    private String loiDonGia() {
        Double gia = docDonGia(txtDonGia.getText());
        if (gia == null) {
            return "Đơn giá phải là số tiền hợp lệ (VNĐ).";
        }
        return gia <= 0 ? "Đơn giá phải lớn hơn 0." : null;
    }

    /** Ô sai và đã chạm: viền đỏ + dòng lỗi; ô đúng: xóa viền, hiện ghi chú (nếu có). */
    private void hienLoi(JComponent o, JLabel lbl, String loi, String ghiChuKhiDung) {
        boolean hien = loi != null && daCham.contains(o);
        o.putClientProperty("JComponent.outline", hien ? "error" : null); // FlatLaf vẽ viền đỏ
        lbl.setForeground(hien ? DO : MUTED);
        lbl.setText(hien ? loi : ghiChuKhiDung == null ? " " : ghiChuKhiDung);
    }

    /**
     * Hộp vàng như ảnh: báo ô nào trong bản nháp đang sai và nhắc giá trị đã lưu vẫn giữ nguyên.
     * Ưu tiên thời lượng (ảnh thiết kế), rồi đơn giá, rồi tên.
     */
    private void hienCanhBao(String loiTen, String loiThoiLuong, String loiDonGia) {
        String noiDung = null;
        if (loiThoiLuong != null && daCham.contains(txtThoiLuong)) {
            noiDung = "Bản nháp có thời lượng không hợp lệ. Giá trị đã lưu vẫn là " + banGoc.getThoiLuong()
                    + " phút; lịch hẹn hiện tại chưa thay đổi.";
        } else if (loiDonGia != null && daCham.contains(txtDonGia)) {
            noiDung = "Bản nháp có đơn giá không hợp lệ. Giá đã lưu vẫn là "
                    + PnlDichVu.dinhDangTien(banGoc.getDonGia()) + ".";
        } else if (loiTen != null && daCham.contains(txtTenDV)) {
            noiDung = "Bản nháp có tên không hợp lệ. Tên đã lưu vẫn là \"" + html(banGoc.getTenDV()) + "\".";
        }
        pnlCanhBao.setVisible(noiDung != null);
        lblCanhBao.setText(noiDung == null ? "" : "<html>" + noiDung + "</html>");
    }

    /** Tô sáng mức thuế ứng với danh mục đang chọn (không bấm được). */
    private void veThue() {
        int chon = cboDanhMuc.getSelectedIndex() - 1;
        for (int i = 0; i < lblThue.length; i++) {
            boolean dang = i == chon;
            lblThue[i].setBackground(dang ? TEAL : SURFACE);
            lblThue[i].setForeground(dang ? Color.WHITE : MUTED);
            lblThue[i].setBorder(BorderFactory.createLineBorder(dang ? TEAL : BORDER));
        }
    }

    /**
     * Thẻ giá theo bản nháp. Thuế làm tròn tới đồng (HALF_UP) như cách tính thuế từng dòng hóa đơn.
     * Đơn giá đang sai thì hiện theo giá đã lưu để thẻ không nhảy về 0.
     */
    private void tinhGia() {
        Double gia = docDonGia(txtDonGia.getText());
        BigDecimal donGia = BigDecimal.valueOf(gia == null || gia <= 0 ? banGoc.getDonGia() : gia);
        MucThue mucThue = MUC_THUE[Math.max(0, cboDanhMuc.getSelectedIndex())];
        if (mucThue == null) {
            mucThue = banGoc.getLoaiDichVu().getMucThueGTGT();
        }
        int phanTram = switch (mucThue) {
            case THUE_0 -> 0;
            case THUE_5 -> 5;
            case THUE_10 -> 10;
        };
        BigDecimal thue = donGia.multiply(BigDecimal.valueOf(phanTram))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        lblGiaDonGia.setText(PnlDichVu.dinhDangTien(donGia.doubleValue()));
        lblGiaThueTen.setText("Thuế GTGT (" + PnlDichVu.tenMucThue(mucThue) + ")");
        lblGiaThue.setText(PnlDichVu.dinhDangTien(thue.doubleValue()));
        lblGiaSauThue.setText(PnlDichVu.dinhDangTien(donGia.add(thue).doubleValue()));
    }

    /**
     * Đọc form thành DichVu (giữ mã của bản gốc để DAO biết cập nhật dòng nào). Chỉ gọi khi form hợp lệ.
     * Lộ trình trống -> entity đổi thành null -> DB lưu NULL.
     */
    DichVu docForm() {
        int loai = cboDanhMuc.getSelectedIndex();
        return new DichVu(banGoc.getMaDV(), txtTenDV.getText().trim(),
                docDonGia(txtDonGia.getText()), docSoNguyen(txtThoiLuong.getText()),
                txaLoTrinh.getText(), true, new LoaiDichVu(null, TEN_DANH_MUC[loai], MUC_THUE[loai]));
    }

    // ===================== Gợi ý lộ trình (dùng chung logic với form Thêm) =====================

    private void napGoiY(Supplier<List<String>> layLoTrinh) {
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() {
                return List.copyOf(layLoTrinh.get());
            }

            @Override
            protected void done() {
                try {
                    loTrinhDaLuu = get();
                } catch (ExecutionException ex) {
                    LOG.log(Level.WARNING, "Không tải được gợi ý lộ trình", ex.getCause());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                veGoiY();
            }
        }.execute();
    }

    private void veGoiY() {
        DlgDichVu.veGoiY(pnlGoiY, loTrinhDaLuu, txaLoTrinh.getText(), loTrinh -> {
            if (!dangLuu) {
                txaLoTrinh.setText(loTrinh);
                txaLoTrinh.requestFocusInWindow();
                txaLoTrinh.setCaretPosition(txaLoTrinh.getDocument().getLength());
            }
        });
    }

    // ===================== Lưu / đóng =====================

    
    void luuThayDoi() {
        // Bấm lưu coi như đã chạm mọi ô, để nếu còn sai thì hiện hết lỗi.
        daCham.addAll(List.of(txtTenDV, cboDanhMuc, txtThoiLuong, txtDonGia));
        capNhatTatCa();
        if (dangLuu || !btnLuu.isEnabled()) {
            return;
        }
        DichVu dv = docForm();
        datDangLuu(true);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return luu.luu(dv);
            }

            @Override
            protected void done() {
                datDangLuu(false);
                try {
                    if (Boolean.TRUE.equals(get())) {
                        daLuu.accept(dv); // màn danh sách đóng trang này và báo thành công
                    } else {
                        // false: không dòng nào được ghi (vd dịch vụ vừa bị người khác ngừng).
                        baoLoiLuu("Không thể lưu. Dịch vụ có thể vừa bị ngừng cung cấp, hãy quay lại danh sách.");
                    }
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof BusinessException || cause instanceof IllegalArgumentException) {
                        // Tên trùng (5.2), không có thay đổi (4.1), còn lịch hẹn khi đổi thời lượng...
                        baoLoiLuu(cause.getMessage());
                        JComponent o = cause.getMessage().contains("thời lượng") ? txtThoiLuong : txtTenDV;
                        o.requestFocusInWindow();
                    } else {
                        // Lỗi kết nối/SQL: ghi log để tra cứu, chỉ hiện thông báo chung.
                        LOG.log(Level.WARNING, "Không thể cập nhật dịch vụ", cause);
                        baoLoiLuu("Không thể lưu dữ liệu, vui lòng thử lại.");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void baoLoiLuu(String thongBao) {
        lblThongBao.setForeground(DO);
        lblThongBao.setText(thongBao);
    }

    private void datDangLuu(boolean dangLuu) {
        this.dangLuu = dangLuu;
        for (JComponent c : new JComponent[]{cboDanhMuc, txtTenDV, txaLoTrinh, txtThoiLuong, txtDonGia,
            btnHuy, btnQuayLai}) {
            c.setEnabled(!dangLuu);
        }
        if (dangLuu) {
            btnLuu.setEnabled(false);
            lblThongBao.setForeground(MUTED);
            lblThongBao.setText("Đang lưu thay đổi...");
        } else {
            String giuThongBao = lblThongBao.getText();
            capNhatTatCa();
            lblThongBao.setText(giuThongBao); // giữ thông báo lỗi của DAO (nếu có) sau khi tính lại
        }
    }

    /** Hủy / Quay lại: còn thay đổi chưa lưu thì hỏi trước khi bỏ. */
    void yeuCauDong() {
        if (dangLuu) {
            return;
        }
        if (conThayDoiChuaLuu() && !xacNhanBoThayDoi()) {
            return;
        }
        dong.run();
    }

    /** Có ô nào khác bản gốc không (kể cả ô đang sai). */
    private boolean conThayDoiChuaLuu() {
        return !txtTenDV.getText().trim().equals(banGoc.getTenDV())
                || !txtThoiLuong.getText().trim().equals(String.valueOf(banGoc.getThoiLuong()))
                || !Objects.equals(docDonGia(txtDonGia.getText()), banGoc.getDonGia())
                || !Objects.equals(TEN_DANH_MUC[Math.max(0, cboDanhMuc.getSelectedIndex())],
                        banGoc.getLoaiDichVu().getTenLoaiDV())
                || !txaLoTrinh.getText().trim().equals(
                        banGoc.getLoTrinhDichVu() == null ? "" : banGoc.getLoTrinhDichVu());
    }

    /** Tách riêng để test thay được hộp thoại xác nhận. */
    boolean xacNhanBoThayDoi() {
        return JOptionPane.showConfirmDialog(this, "Bỏ các thay đổi chưa lưu và quay lại danh sách?",
                "Hủy chỉnh sửa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    // ===================== Tiện ích =====================

    static Integer docSoNguyen(String text) {
        String so = text == null ? "" : text.trim();
        return so.matches("-?\\d{1,6}") ? Integer.valueOf(so) : null;
    }

    /** 300000 -> "300.000" để hiện trong ô đơn giá. */
    private static String soTien(double soTien) {
        return new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.of("vi", "VN"))).format(soTien);
    }

    private static void khoa(JTextField o, String goiY) {
        o.setEditable(false);
        o.setFocusable(false);
        o.setBackground(NEN_O_KHOA);
        o.setForeground(MUTED);
        o.setToolTipText(goiY);
    }

    private static JLabel loiLabel(String ten) {
        JLabel l = new JLabel(" ");
        l.setName(ten);
        l.setFont(font(11));
        return l;
    }

    private static JLabel nhan(String chu) {
        JLabel l = new JLabel(chu);
        l.setFont(bold(12));
        l.setForeground(TEXT);
        return l;
    }

    private static JLabel ghiChu(String chu) {
        JLabel l = new JLabel(chu);
        l.setFont(font(11));
        l.setForeground(MUTED);
        return l;
    }

    private static JComponent o(JComponent c) {
        c.setFont(font(13));
        c.setPreferredSize(new Dimension(0, 38));
        return c;
    }

    private static void dat(JPanel p, GridBagConstraints g, Component c, int x, int y, int rong, Insets le) {
        g.gridx = x;
        g.gridy = y;
        g.gridwidth = rong;
        g.insets = le;
        p.add(c, g);
    }

    private static String html(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Đọc đơn giá giống form Thêm ("300000", "300.000", "300,000 ₫"); không phải số nguyên thì null. */
    static Double docDonGia(String text) {
        String so = text == null ? "" : text.replace("₫", "").replace("đ", "").replaceAll("[\\s.,]", "");
        return so.matches("-?\\d{1,15}") ? Double.valueOf(so) : null;
    }
}
