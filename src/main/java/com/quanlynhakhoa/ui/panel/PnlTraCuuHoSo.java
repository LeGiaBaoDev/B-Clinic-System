package com.quanlynhakhoa.ui.panel;

import static com.quanlynhakhoa.ui.component.UiConstants.BACKGROUND;
import static com.quanlynhakhoa.ui.component.UiConstants.BORDER;
import static com.quanlynhakhoa.ui.component.UiConstants.MUTED;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL_LIGHT;
import static com.quanlynhakhoa.ui.component.UiConstants.TEXT;
import static com.quanlynhakhoa.ui.component.UiConstants.bold;
import static com.quanlynhakhoa.ui.component.UiConstants.font;

import com.quanlynhakhoa.dao.ChiTietPhieuKhamDAO;
import com.quanlynhakhoa.dao.HoSoBenhAnDAO;
import com.quanlynhakhoa.dao.KhachHangDAO;
import com.quanlynhakhoa.dao.LichHenDAO;
import com.quanlynhakhoa.dao.PhieuKhamDAO;
import com.quanlynhakhoa.entity.ChiTietPhieuKham;
import com.quanlynhakhoa.entity.HoSoBenhAn;
import com.quanlynhakhoa.entity.KhachHang;
import com.quanlynhakhoa.entity.LichHen;
import com.quanlynhakhoa.entity.PhieuKham;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.TrangThaiLichHen;
import com.quanlynhakhoa.entity.enums.TrangThaiPhieuKham;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import com.quanlynhakhoa.ui.dialog.DlgChiTietBenhAn;
import com.quanlynhakhoa.ui.dialog.DlgInHoSo;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

public class PnlTraCuuHoSo extends JPanel {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm");
    private static final Color AMBER = new Color(0x9A5A00);
    private static final Color AMBER_BG = new Color(0xFFF2DB);
    private static final Color GREEN = new Color(0x16875D);
    private static final Color GREEN_BG = new Color(0xE3F4EC);
    private static final String TAB_HOSO = "ho-so";
    private static final String TAB_LICH = "lich-su";
    private static final String TAB_GHICHU = "ghi-chu";

    private final transient TaiKhoan taiKhoan;
    private final transient Consumer<String> moPhieuKham;
    private final transient KhachHangDAO khachHangDAO = new KhachHangDAO();
    private final transient HoSoBenhAnDAO hoSoDAO = new HoSoBenhAnDAO();
    private final transient PhieuKhamDAO phieuKhamDAO = new PhieuKhamDAO();
    private final transient LichHenDAO lichHenDAO = new LichHenDAO();
    private final transient ChiTietPhieuKhamDAO chiTietDAO = new ChiTietPhieuKhamDAO();

    private final transient List<KhachHangDAO.TomTat> dsTomTat = new ArrayList<>();
    private final Map<String, CardBenhNhan> theCard = new LinkedHashMap<>();
    private String maChon;
    private transient ChiTiet chiTietHienTai;

    // trái
    private final JTextField txtTim = new JTextField();
    private final JComboBox<String> cboLoc = new JComboBox<>(new String[]{
        "Tất cả hồ sơ", "Mới", "Đang điều trị", "Theo dõi"});
    private final JPanel pnlDanhSach = new JPanel();
    private final JLabel lblDem = new JLabel("0 hồ sơ bệnh nhân");
    private final JLabel lblKetQua = new JLabel(" ");

    // phải
    private final JLabel lblTen = new JLabel(" ");
    private final PillLabel lblMaBadge = new PillLabel();
    private final JLabel lblNgaySinh = new JLabel(" ");
    private final JLabel lblSdt = new JLabel(" ");
    private final JLabel lblEmail = new JLabel(" ");
    private final JLabel lblDiaChi = new JLabel(" ");
    private final JLabel lblDiUng = new JLabel(" ");
    private final JLabel lblBenhNen = new JLabel(" ");
    private final JLabel lblMaHoSo = new JLabel(" ");
    private final JLabel lblTab1 = new JLabel("Hồ sơ bệnh án");
    private final JLabel lblTab2 = new JLabel("Lịch sử hẹn");
    private final JLabel lblTab3 = new JLabel("Ghi chú");
    private final CardLayout theTab = new CardLayout();
    private final JPanel pnlTab = new JPanel(theTab);
    private final JPanel pnlHoSo = boxPanel();
    private final JPanel pnlLichSu = boxPanel();
    private final JPanel pnlGhiChu = boxPanel();
    private final JButton btnMoPhieu = PnlTongQuanQuanLy.button("Mở phiếu khám", LineIcon.Type.CLIPBOARD, true);
    private final JPanel pnlPhai = new SurfacePanel(new BorderLayout(0, 12));

    public PnlTraCuuHoSo(TaiKhoan taiKhoan, Consumer<String> moPhieuKham) {
        super(new BorderLayout(0, 14));
        this.taiKhoan = java.util.Objects.requireNonNull(taiKhoan);
        this.moPhieuKham = moPhieuKham;
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 20, 24));
        add(taoHeader(), BorderLayout.NORTH);
        add(taoThanBody(), BorderLayout.CENTER);
        trangThaiRong();
    }

    /** Tải lại danh sách khi chuyển vào trang. */
    public void lamMoi() {
        taiDanhSach();
    }

    // ---------------------------------------------------------------- header

    private JPanel taoHeader() {
        JPanel p = new JPanel(new BorderLayout(16, 0));
        p.setOpaque(false);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Tra cứu hồ sơ bệnh án điện tử");
        title.setFont(bold(21));
        title.setForeground(TEXT);
        title.setAlignmentX(LEFT_ALIGNMENT);
        JLabel sub = new JLabel("Tìm kiếm bệnh nhân và xem thông tin khám, chẩn đoán, điều trị.");
        sub.setFont(font(12));
        sub.setForeground(MUTED);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        text.add(title);
        text.add(Box.createVerticalStrut(4));
        text.add(sub);
        p.add(text, BorderLayout.WEST);

        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        nut.setOpaque(false);
        JButton inHoSo = PnlTongQuanQuanLy.button("In hồ sơ", LineIcon.Type.PRINTER, false);
        inHoSo.addActionListener(e -> inHoSo());
        btnMoPhieu.addActionListener(e -> moPhieu());
        nut.add(inHoSo);
        nut.add(btnMoPhieu);
        p.add(nut, BorderLayout.EAST);
        return p;
    }

    private JPanel taoThanBody() {
        JPanel p = new JPanel(new BorderLayout(14, 0));
        p.setOpaque(false);
        p.add(taoTrai(), BorderLayout.WEST);
        p.add(taoPhai(), BorderLayout.CENTER);
        return p;
    }

    // ---------------------------------------------------------------- trái

    private JPanel taoTrai() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 10));
        card.setPreferredSize(new Dimension(340, 0));
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel tren = new JPanel();
        tren.setOpaque(false);
        tren.setLayout(new BoxLayout(tren, BoxLayout.Y_AXIS));
        txtTim.setFont(font(11));
        txtTim.putClientProperty("JTextField.placeholderText", "Tên, số điện thoại, mã bệnh nhân...");
        txtTim.putClientProperty("JTextField.leadingIcon", new LineIcon(LineIcon.Type.SEARCH, MUTED, 15));
        txtTim.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(7, 8, 7, 8)));
        txtTim.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        txtTim.setAlignmentX(LEFT_ALIGNMENT);
        txtTim.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { taiDanhSach(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { taiDanhSach(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { taiDanhSach(); }
        });
        tren.add(txtTim);
        tren.add(Box.createVerticalStrut(10));
        JPanel hangLoc = new JPanel(new BorderLayout(8, 0));
        hangLoc.setOpaque(false);
        hangLoc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        hangLoc.setAlignmentX(LEFT_ALIGNMENT);
        lblDem.setFont(font(11));
        lblDem.setForeground(MUTED);
        cboLoc.setFont(font(11));
        cboLoc.setPreferredSize(new Dimension(130, 28));
        cboLoc.addActionListener(e -> hienDanhSach());
        hangLoc.add(lblDem, BorderLayout.WEST);
        hangLoc.add(cboLoc, BorderLayout.EAST);
        tren.add(hangLoc);
        card.add(tren, BorderLayout.NORTH);

        pnlDanhSach.setOpaque(false);
        pnlDanhSach.setLayout(new BoxLayout(pnlDanhSach, BoxLayout.Y_AXIS));
        JScrollPane scr = new JScrollPane(pnlDanhSach);
        scr.setBorder(null);
        scr.setOpaque(false);
        scr.getViewport().setOpaque(false);
        scr.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scr.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scr, BorderLayout.CENTER);

        lblKetQua.setFont(font(10));
        lblKetQua.setForeground(MUTED);
        card.add(lblKetQua, BorderLayout.SOUTH);
        return card;
    }

    private void taiDanhSach() {
        final String khoa = txtTim.getText() == null ? "" : txtTim.getText().trim();
        new SwingWorker<List<KhachHangDAO.TomTat>, Void>() {
            @Override
            protected List<KhachHangDAO.TomTat> doInBackground() {
                return khachHangDAO.timKiemTomTat(khoa);
            }

            @Override
            protected void done() {
                try {
                    dsTomTat.clear();
                    dsTomTat.addAll(get());
                    hienDanhSach();
                } catch (CancellationException ignored) {
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    baoLoi(e.getCause());
                }
            }
        }.execute();
    }

    private void hienDanhSach() {
        pnlDanhSach.removeAll();
        theCard.clear();
        String loc = (String) cboLoc.getSelectedItem();
        List<KhachHangDAO.TomTat> hienThi = new ArrayList<>();
        for (KhachHangDAO.TomTat t : dsTomTat) {
            if (loc == null || "Tất cả hồ sơ".equals(loc) || loc.equals(t.trangThaiHoSo())) {
                hienThi.add(t);
            }
        }
        for (KhachHangDAO.TomTat t : hienThi) {
            CardBenhNhan card = new CardBenhNhan(t);
            theCard.put(t.khachHang().getMaKH(), card);
            pnlDanhSach.add(card);
            pnlDanhSach.add(Box.createVerticalStrut(8));
        }
        lblDem.setText(dsTomTat.size() + " hồ sơ bệnh nhân");
        lblKetQua.setText(hienThi.isEmpty() ? "Không có kết quả phù hợp."
                : "Hiển thị 1–" + hienThi.size() + " / " + hienThi.size() + " kết quả");
        pnlDanhSach.revalidate();
        pnlDanhSach.repaint();
        if (hienThi.isEmpty()) {
            maChon = null;
            trangThaiRong();
        } else {
            String giuChon = maChon != null && theCard.containsKey(maChon)
                    ? maChon : hienThi.get(0).khachHang().getMaKH();
            chonBenhNhan(giuChon);
        }
    }

    private void chonBenhNhan(String maKH) {
        maChon = maKH;
        theCard.forEach((ma, card) -> card.setChon(ma.equals(maKH)));
        taiChiTiet(maKH);
    }

    // ---------------------------------------------------------------- phải

    private JPanel taoPhai() {
        pnlPhai.setBorder(new EmptyBorder(16, 18, 14, 18));

        JPanel dau = new JPanel(new BorderLayout(10, 0));
        dau.setOpaque(false);
        dau.setAlignmentX(LEFT_ALIGNMENT);
        dau.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        lblTen.setFont(bold(18));
        lblTen.setForeground(TEXT);
        lblMaBadge.setFont(bold(10));
        dau.add(lblTen, BorderLayout.WEST);
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        badge.setOpaque(false);
        badge.add(lblMaBadge);
        dau.add(badge, BorderLayout.EAST);

        JPanel thongTin = new JPanel();
        thongTin.setOpaque(false);
        thongTin.setLayout(new BoxLayout(thongTin, BoxLayout.Y_AXIS));
        thongTin.add(dau);
        thongTin.add(Box.createVerticalStrut(12));
        JPanel g1 = new JPanel(new GridLayout(1, 3, 16, 0));
        g1.setOpaque(false);
        g1.setAlignmentX(LEFT_ALIGNMENT);
        g1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        g1.add(oInfo("Ngày sinh · Giới tính", lblNgaySinh));
        g1.add(oInfo("Số điện thoại", lblSdt));
        g1.add(oInfo("Email", lblEmail));
        thongTin.add(g1);
        thongTin.add(Box.createVerticalStrut(10));
        thongTin.add(oInfo("Địa chỉ liên hệ", lblDiaChi));
        thongTin.add(Box.createVerticalStrut(10));
        JPanel g2 = new JPanel(new GridLayout(1, 2, 16, 0));
        g2.setOpaque(false);
        g2.setAlignmentX(LEFT_ALIGNMENT);
        g2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        g2.add(oInfo("Dị ứng / Tiền sử", lblDiUng));
        g2.add(oInfo("Bệnh nền / Thuốc đang dùng", lblBenhNen));
        thongTin.add(g2);
        thongTin.add(Box.createVerticalStrut(8));
        lblMaHoSo.setFont(font(10));
        lblMaHoSo.setForeground(MUTED);
        lblMaHoSo.setAlignmentX(LEFT_ALIGNMENT);
        thongTin.add(lblMaHoSo);
        pnlPhai.add(thongTin, BorderLayout.NORTH);

        JPanel giua = new JPanel(new BorderLayout(0, 10));
        giua.setOpaque(false);
        giua.add(taoTabBar(), BorderLayout.NORTH);
        pnlTab.setOpaque(false);
        pnlTab.add(wrapScroll(pnlHoSo), TAB_HOSO);
        pnlTab.add(wrapScroll(pnlLichSu), TAB_LICH);
        pnlTab.add(wrapScroll(pnlGhiChu), TAB_GHICHU);
        giua.add(pnlTab, BorderLayout.CENTER);
        pnlPhai.add(giua, BorderLayout.CENTER);

        JPanel chan = new JPanel(new BorderLayout(10, 0));
        chan.setOpaque(false);
        chan.setBorder(new EmptyBorder(6, 0, 0, 0));
        JLabel note = new JLabel("Thông tin y tế · Chỉ sử dụng cho mục đích khám chữa bệnh");
        note.setFont(font(10));
        note.setForeground(MUTED);
        JLabel tiepTuc = linkLabel("Tiếp tục phiếu khám →");
        tiepTuc.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                moPhieu();
            }
        });
        chan.add(note, BorderLayout.WEST);
        chan.add(tiepTuc, BorderLayout.EAST);
        pnlPhai.add(chan, BorderLayout.SOUTH);
        return pnlPhai;
    }

    private JPanel taoTabBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        gan(lblTab1, TAB_HOSO);
        gan(lblTab2, TAB_LICH);
        gan(lblTab3, TAB_GHICHU);
        bar.add(lblTab1);
        bar.add(lblTab2);
        bar.add(lblTab3);
        return bar;
    }

    private void gan(JLabel tab, String the) {
        tab.setFont(font(12));
        tab.setForeground(MUTED);
        tab.setBorder(new EmptyBorder(6, 2, 8, 2));
        tab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tab.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                chonTab(the);
            }
        });
    }

    private void chonTab(String the) {
        theTab.show(pnlTab, the);
        napMauTab(lblTab1, TAB_HOSO.equals(the));
        napMauTab(lblTab2, TAB_LICH.equals(the));
        napMauTab(lblTab3, TAB_GHICHU.equals(the));
    }

    private static void napMauTab(JLabel tab, boolean active) {
        tab.setFont(active ? bold(12) : font(12));
        tab.setForeground(active ? TEAL : MUTED);
        tab.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, active ? 2 : 0, 0, TEAL),
                new EmptyBorder(6, 2, active ? 6 : 8, 2)));
    }

    // ---------------------------------------------------------------- chi tiết

    private void taiChiTiet(String maKH) {
        btnMoPhieu.setEnabled(false);
        new SwingWorker<ChiTiet, Void>() {
            @Override
            protected ChiTiet doInBackground() {
                KhachHang kh = khachHangDAO.layTheoMa(maKH);
                HoSoBenhAn hs = hoSoDAO.layTheoKhachHang(maKH);
                List<PhieuKham> phieu = phieuKhamDAO.timTheoKhachHang(maKH);
                List<LichHen> lich = lichHenDAO.timTheoKhachHang(maKH);
                Map<String, List<ChiTietPhieuKham>> ctTheoPhieu = new LinkedHashMap<>();
                for (PhieuKham pk : phieu) {
                    List<ChiTietPhieuKham> ct = chiTietDAO.layTheoPhieuKham(pk.getMaPK());
                    if (!ct.isEmpty()) {
                        ctTheoPhieu.put(pk.getMaPK(), ct);
                    }
                }
                return new ChiTiet(kh, hs, phieu, lich, ctTheoPhieu);
            }

            @Override
            protected void done() {
                try {
                    hienChiTiet(get());
                } catch (CancellationException ignored) {
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    baoLoi(e.getCause());
                }
            }
        }.execute();
    }

    private record ChiTiet(KhachHang kh, HoSoBenhAn hs, List<PhieuKham> phieu,
            List<LichHen> lich, Map<String, List<ChiTietPhieuKham>> ctTheoPhieu) {
    }

    private void hienChiTiet(ChiTiet d) {
        chiTietHienTai = d;
        KhachHang kh = d.kh();
        lblTen.setText(kh.getTenKH());
        lblMaBadge.set(kh.getMaKH(), TEAL, TEAL_LIGHT);
        lblNgaySinh.setText((kh.getNgaySinh() == null ? "—" : kh.getNgaySinh().format(NGAY))
                + " · " + (kh.isGioiTinh() ? "Nam" : "Nữ"));
        lblSdt.setText(nullThiGach(kh.getSdt()));
        lblEmail.setText(nullThiGach(kh.getEmail()));
        lblDiaChi.setText(nullThiGach(kh.getDiaChi()));
        String tienSu = d.hs() == null || d.hs().getTienSuBenh() == null
                || d.hs().getTienSuBenh().isBlank() ? null : d.hs().getTienSuBenh().trim();
        lblDiUng.setText(tienSu == null ? "Chưa ghi nhận · Cần xác minh" : tienSu);
        lblBenhNen.setText(tienSu == null ? "Chưa ghi nhận" : tienSu);
        lblMaHoSo.setText(d.hs() == null ? "Chưa lập hồ sơ bệnh án điện tử"
                : "Mã hồ sơ: " + d.hs().getMaHSBA());
        btnMoPhieu.setEnabled(coPhieuHomNay(d.phieu()));

        hienHoSo(d);
        hienLichSu(d.lich());
        hienGhiChu(d.phieu());
        lblTab2.setText("Lịch sử hẹn (" + d.lich().size() + ")");
        chonTab(TAB_HOSO);
        pnlPhai.revalidate();
        pnlPhai.repaint();
    }

    /** Có phiếu khám chờ/đang khám trong hôm nay để mở không. */
    private static boolean coPhieuHomNay(List<PhieuKham> phieu) {
        LocalDate today = LocalDate.now(ZONE);
        for (PhieuKham pk : phieu) {
            if (pk.getNgayLap().toLocalDate().equals(today)
                    && (pk.getTrangThai() == TrangThaiPhieuKham.CHO_KHAM
                    || pk.getTrangThai() == TrangThaiPhieuKham.DANG_KHAM)) {
                return true;
            }
        }
        return false;
    }

    private void hienHoSo(ChiTiet d) {
        pnlHoSo.removeAll();
        List<PhieuKham> hoanTat = chiPhieuHoanTat(d.phieu());
        if (hoanTat.isEmpty()) {
            pnlHoSo.add(trong("Chưa có phiếu khám nào đã hoàn tất."));
        }
        for (PhieuKham pk : hoanTat) {
            List<ChiTietPhieuKham> ct = d.ctTheoPhieu().get(pk.getMaPK());
            pnlHoSo.add(khoiPhieu(pk, ct == null || ct.isEmpty() ? null : ct.get(0)));
            pnlHoSo.add(Box.createVerticalStrut(10));
        }
        capNhat(pnlHoSo);
    }

    private JPanel khoiPhieu(PhieuKham pk, ChiTietPhieuKham ct) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xEDF1F5)),
                new EmptyBorder(8, 0, 9, 0)));

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel dong = new JLabel("Hồ sơ " + pk.getMaPK() + " · Ngày "
                + pk.getNgayLap().format(NGAY));
        dong.setFont(bold(12));
        dong.setForeground(TEXT);
        dong.setAlignmentX(LEFT_ALIGNMENT);
        String chanDoan = ct == null ? "—" : chanDoanNgan(ct.getChanDoan());
        JLabel benh = new JLabel("Bệnh: " + chanDoan);
        benh.setFont(font(11));
        benh.setForeground(MUTED);
        benh.setAlignmentX(LEFT_ALIGNMENT);
        info.add(dong);
        info.add(Box.createVerticalStrut(3));
        info.add(benh);
        row.add(info, BorderLayout.CENTER);

        JButton chiTiet = PnlTongQuanQuanLy.button("Chi tiết", null, false);
        chiTiet.setPreferredSize(new Dimension(112, 34));
        chiTiet.addActionListener(e -> moChiTiet(pk));
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        nut.setOpaque(false);
        nut.add(chiTiet);
        row.add(nut, BorderLayout.EAST);
        return row;
    }

    private void moChiTiet(PhieuKham pk) {
        if (pk.getTrangThai() != TrangThaiPhieuKham.HOAN_TAT) {
            return;
        }
        new SwingWorker<List<ChiTietPhieuKham>, Void>() {
            @Override
            protected List<ChiTietPhieuKham> doInBackground() {
                return chiTietDAO.layTheoPhieuKham(pk.getMaPK());
            }

            @Override
            protected void done() {
                try {
                    ChiTiet d = chiTietHienTai;
                    DlgChiTietBenhAn.hien(javax.swing.SwingUtilities.getWindowAncestor(PnlTraCuuHoSo.this),
                            d == null ? null : d.kh(), d == null ? null : d.hs(), pk, get());
                } catch (CancellationException ignored) {
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    baoLoi(e.getCause());
                }
            }
        }.execute();
    }

    private void hienLichSu(List<LichHen> ds) {
        pnlLichSu.removeAll();
        if (ds.isEmpty()) {
            pnlLichSu.add(trong("Chưa có lịch hẹn nào."));
        }
        for (LichHen lh : ds) {
            JPanel p = new JPanel(new BorderLayout(10, 0));
            p.setOpaque(false);
            p.setAlignmentX(LEFT_ALIGNMENT);
            p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
            p.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xEDF1F5)),
                    new EmptyBorder(6, 0, 8, 0)));
            JPanel trai = new JPanel();
            trai.setOpaque(false);
            trai.setLayout(new BoxLayout(trai, BoxLayout.Y_AXIS));
            JLabel l1 = new JLabel(lh.getNgayGioHen().format(NGAY_GIO)
                    + (lh.getDichVu() != null ? " · " + lh.getDichVu().getTenDV() : ""));
            l1.setFont(bold(11));
            l1.setForeground(TEXT);
            JLabel l2 = new JLabel(lh.getNhaSi() != null ? "BS. " + lh.getNhaSi().getHoTenNV() : "—");
            l2.setFont(font(10));
            l2.setForeground(MUTED);
            trai.add(l1);
            trai.add(Box.createVerticalStrut(2));
            trai.add(l2);
            p.add(trai, BorderLayout.WEST);
            JPanel bd = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            bd.setOpaque(false);
            bd.add(pillLich(lh.getTrangThai()));
            p.add(bd, BorderLayout.EAST);
            pnlLichSu.add(p);
        }
        capNhat(pnlLichSu);
    }

    private void hienGhiChu(List<PhieuKham> phieu) {
        pnlGhiChu.removeAll();
        boolean co = false;
        for (PhieuKham pk : chiPhieuHoanTat(phieu)) {
            if (pk.getLoiDan() != null && !pk.getLoiDan().isBlank()) {
                co = true;
                JLabel l = valueLabel(pk.getNgayLap().format(NGAY) + ": " + motDong(pk.getLoiDan()));
                l.setBorder(new EmptyBorder(0, 0, 8, 0));
                pnlGhiChu.add(l);
            }
        }
        if (!co) {
            pnlGhiChu.add(trong("Chưa có ghi chú/lời dặn."));
        }
        capNhat(pnlGhiChu);
    }

    private static List<PhieuKham> chiPhieuHoanTat(List<PhieuKham> ds) {
        return ds.stream().filter(pk -> pk.getTrangThai() == TrangThaiPhieuKham.HOAN_TAT).toList();
    }

    private void trangThaiRong() {
        chiTietHienTai = null;
        lblTen.setText("Chọn một bệnh nhân");
        lblMaBadge.set("", TEXT, BACKGROUND);
        for (JLabel l : new JLabel[]{lblNgaySinh, lblSdt, lblEmail, lblDiaChi, lblDiUng, lblBenhNen}) {
            l.setText("—");
        }
        lblMaHoSo.setText(" ");
        lblTab2.setText("Lịch sử hẹn");
        btnMoPhieu.setEnabled(false);
        for (JPanel pnl : new JPanel[]{pnlHoSo, pnlLichSu, pnlGhiChu}) {
            pnl.removeAll();
            pnl.add(trong("Chọn bệnh nhân ở danh sách bên trái để xem hồ sơ."));
            capNhat(pnl);
        }
    }

    // ---------------------------------------------------------------- hành động

    private void moPhieu() {
        if (maChon == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một bệnh nhân.",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (moPhieuKham != null) {
            moPhieuKham.accept(maChon);
        }
    }

    private void inHoSo() {
        ChiTiet d = chiTietHienTai;
        if (d == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một bệnh nhân.",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        DlgInHoSo.hien(javax.swing.SwingUtilities.getWindowAncestor(this),
                d.kh(), d.hs(), chiPhieuHoanTat(d.phieu()), d.ctTheoPhieu());
    }

    private void baoLoi(Throwable loi) {
        if (loi instanceof BusinessException) {
            JOptionPane.showMessageDialog(this, loi.getMessage(), "Cảnh báo", JOptionPane.WARNING_MESSAGE);
        } else if (loi instanceof DataAccessException) {
            JOptionPane.showMessageDialog(this, "Không thể tải dữ liệu hồ sơ.", "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Đã xảy ra lỗi, vui lòng thử lại.", "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------------------------------------------------------- helpers

    private static JPanel boxPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(8, 2, 8, 2));
        return p;
    }

    private static JScrollPane wrapScroll(JPanel noiDung) {
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(noiDung, BorderLayout.NORTH);
        JScrollPane scr = new JScrollPane(holder);
        scr.setBorder(null);
        scr.setOpaque(false);
        scr.getViewport().setOpaque(false);
        scr.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scr.getVerticalScrollBar().setUnitIncrement(16);
        return scr;
    }

    private static JPanel oInfo(String nhan, JLabel value) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(LEFT_ALIGNMENT);
        JLabel l = new JLabel(nhan);
        l.setFont(font(10));
        l.setForeground(MUTED);
        l.setAlignmentX(LEFT_ALIGNMENT);
        value.setFont(font(12));
        value.setForeground(TEXT);
        value.setAlignmentX(LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(3));
        p.add(value);
        return p;
    }

    private static JLabel valueLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(12));
        l.setForeground(TEXT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel dongChu(String text, java.awt.Font f, Color c) {
        JLabel l = new JLabel(text);
        l.setFont(f);
        l.setForeground(c);
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(3, 0, 0, 0));
        return l;
    }

    private static JPanel hopThongTin(String text, Color bg, Color fg) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(bg);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setBorder(new EmptyBorder(8, 10, 8, 10));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        JLabel l = new JLabel("<html><div style='width:460px'>" + text + "</div></html>");
        l.setFont(font(10));
        l.setForeground(fg);
        p.add(l, BorderLayout.CENTER);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setAlignmentX(LEFT_ALIGNMENT);
        wrap.setBorder(new EmptyBorder(6, 0, 0, 0));
        wrap.add(p, BorderLayout.CENTER);
        return wrap;
    }

    private static JLabel trong(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(11));
        l.setForeground(MUTED);
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(20, 2, 20, 2));
        return l;
    }

    private static JLabel linkLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(bold(11));
        l.setForeground(TEAL);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return l;
    }

    private static void capNhat(JPanel p) {
        p.revalidate();
        p.repaint();
    }

    private static String nullThiGach(String s) {
        return s == null || s.isBlank() ? "—" : s.trim();
    }

    private static String motDong(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }

    
    private static String dong(String blob, String nhan) {
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
        String v = dong(blob, "Triệu chứng:");
        return cut(v != null ? v : motDong(blob));
    }

    /** Gộp "ICD-10" + "Chẩn đoán" thành "K05.1 · Viêm nướu mạn tính"; không nhân đôi khi thiếu nhãn. */
    private static String chanDoanNgan(String blob) {
        String ma = dong(blob, "ICD-10:");
        String cd = dong(blob, "Chẩn đoán:");
        String s;
        if (ma != null && cd != null) {
            s = ma + " · " + cd;
        } else if (cd != null) {
            s = cd;
        } else if (ma != null) {
            s = ma;
        } else {
            s = motDong(blob);
        }
        return cut(s);
    }

    private static String cut(String s) {
        if (s == null || s.isBlank()) {
            return "—";
        }
        String t = s.trim();
        return t.length() <= 90 ? t : t.substring(0, 90) + "…";
    }

    private static PillLabel pillTrangThai(TrangThaiPhieuKham tt) {
        return switch (tt) {
            case HOAN_TAT -> pill("Hoàn tất", GREEN, GREEN_BG);
            case DANG_KHAM -> pill("Đang khám", TEAL, TEAL_LIGHT);
            default -> pill("Bản nháp", AMBER, AMBER_BG);
        };
    }

    private static PillLabel pillLich(TrangThaiLichHen tt) {
        return switch (tt) {
            case HOAN_TAT -> pill("Hoàn tất", GREEN, GREEN_BG);
            case DA_DAT -> pill("Đã đặt", TEAL, TEAL_LIGHT);
            case DA_DEN -> pill("Đã đến", TEAL, TEAL_LIGHT);
            case DA_HUY -> pill("Đã hủy", new Color(0xB42318), new Color(0xFDECEA));
            case KHONG_DEN -> pill("Không đến", AMBER, AMBER_BG);
        };
    }

    private static PillLabel pill(String text, Color fg, Color bg) {
        PillLabel l = new PillLabel();
        l.setFont(bold(10));
        l.set(text, fg, bg);
        return l;
    }

    // ---------------------------------------------------------------- thẻ bệnh nhân (trái)

    private final class CardBenhNhan extends JPanel {

        private boolean chon;

        CardBenhNhan(KhachHangDAO.TomTat t) {
            super(new BorderLayout(8, 0));
            KhachHang kh = t.khachHang();
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 12, 10, 12));

            JPanel info = new JPanel();
            info.setOpaque(false);
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
            JLabel ten = new JLabel(kh.getTenKH());
            ten.setFont(bold(13));
            ten.setForeground(TEXT);
            ten.setAlignmentX(LEFT_ALIGNMENT);
            JLabel meta = new JLabel(kh.getMaKH() + " · " + (kh.isGioiTinh() ? "Nam" : "Nữ")
                    + (kh.getNgaySinh() == null ? "" : " · " + kh.getNgaySinh().format(NGAY)));
            meta.setFont(font(10));
            meta.setForeground(MUTED);
            meta.setAlignmentX(LEFT_ALIGNMENT);
            JLabel sdt = new JLabel(nullThiGach(kh.getSdt()));
            sdt.setFont(font(10));
            sdt.setForeground(MUTED);
            sdt.setAlignmentX(LEFT_ALIGNMENT);
            JLabel tt = new JLabel(dongTrangThai(t));
            tt.setFont(font(10));
            tt.setForeground(t.coLichSapToi() ? TEAL : MUTED);
            tt.setAlignmentX(LEFT_ALIGNMENT);
            info.add(ten);
            info.add(Box.createVerticalStrut(3));
            info.add(meta);
            info.add(Box.createVerticalStrut(2));
            info.add(sdt);
            info.add(Box.createVerticalStrut(4));
            info.add(tt);
            add(info, BorderLayout.CENTER);

            JLabel chevron = new JLabel(new LineIcon(LineIcon.Type.ARROW, MUTED, 14));
            add(chevron, BorderLayout.EAST);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    chonBenhNhan(kh.getMaKH());
                }
            });
        }

        void setChon(boolean chon) {
            this.chon = chon;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(chon ? TEAL_LIGHT : Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.setColor(chon ? TEAL : BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.dispose();
            super.paintComponent(g);
        }

        private String dongTrangThai(KhachHangDAO.TomTat t) {
            if (t.lichGanNhat() != null && t.coLichSapToi()) {
                return "Lịch hẹn: " + t.lichGanNhat().format(NGAY_GIO);
            }
            return t.trangThaiHoSo() + " · " + t.soLanKham() + " lần khám";
        }
    }

    // ---------------------------------------------------------------- pill

    private static final class PillLabel extends JLabel {

        private transient Color bg = new Color(0xF0F2F5);

        PillLabel() {
            super("", SwingConstants.CENTER);
            setOpaque(false);
            setBorder(new EmptyBorder(3, 10, 3, 10));
        }

        void set(String text, Color fg, Color bg) {
            setText(text);
            setForeground(fg);
            this.bg = bg;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (getText() != null && !getText().isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
