package com.quanlynhakhoa.ui.panel;

import static com.quanlynhakhoa.ui.component.UiConstants.BACKGROUND;
import static com.quanlynhakhoa.ui.component.UiConstants.BORDER;
import static com.quanlynhakhoa.ui.component.UiConstants.GREEN;
import static com.quanlynhakhoa.ui.component.UiConstants.MUTED;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL_DARK;
import static com.quanlynhakhoa.ui.component.UiConstants.TEXT;
import static com.quanlynhakhoa.ui.component.UiConstants.bold;
import static com.quanlynhakhoa.ui.component.UiConstants.font;

import com.quanlynhakhoa.dao.LichHenDAO;
import com.quanlynhakhoa.dao.LichLamViecDAO;
import com.quanlynhakhoa.dao.PhieuKhamDAO;
import com.quanlynhakhoa.entity.LichHen;
import com.quanlynhakhoa.entity.LichLamViec;
import com.quanlynhakhoa.entity.PhieuKham;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.CaTruc;
import com.quanlynhakhoa.entity.enums.TrangThaiLichHen;
import com.quanlynhakhoa.entity.enums.TrangThaiLichLamViec;
import com.quanlynhakhoa.entity.enums.TrangThaiPhieuKham;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import com.quanlynhakhoa.ui.dialog.HopThoaiLoi;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

public class PnlTongQuanNhaSi extends JPanel {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy",
            Locale.of("vi", "VN"));

    private final TaiKhoan taiKhoan;
    private final Consumer<String> moPhieuKham;
    private final Runnable moLich;
    private final Runnable moBenhAn;
    private final transient Consumer<String> xemHoSo;
    private String maKHTiepTheo;
    private final LichHenDAO lichHenDAO = new LichHenDAO();
    private final PhieuKhamDAO phieuKhamDAO = new PhieuKhamDAO();
    private final LichLamViecDAO lichLamViecDAO = new LichLamViecDAO();

    private final JLabel lblLichHen = new JLabel("—");
    private final JLabel lblLichHenSub = new JLabel(" ");
    private final JLabel lblDaDen = new JLabel("—");
    private final JLabel lblDaDenSub = new JLabel(" ");
    private final JLabel lblSapToi = new JLabel("—");
    private final JLabel lblSapToiSub = new JLabel(" ");
    private final JLabel lblHoanTat = new JLabel("—");
    private final JLabel lblHoanTatSub = new JLabel(" ");
    private final JLabel lblNgay = new JLabel();
    private final JLabel lblLoi = new JLabel(" ");
    private final JLabel lblListSub = new JLabel(" ");
    private final JLabel lblBadge = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel lblTiepTheoFooter = new JLabel(" ");
    private final JLabel lblCa = new JLabel("Đang tải...");
    private final JLabel lblCaGhiChu = new JLabel(" ");
    private final JLabel lblCaPhong = new JLabel("—");
    private final JLabel lblCaPhuTa = new JLabel("—");
    private final JLabel lblLuuY = new JLabel(" ");
    private final JLabel lblTiepTheo = new JLabel("Chưa có bệnh nhân tiếp theo");
    private final JLabel lblTiepTheoMeta = new JLabel(" ");
    private final JLabel lblTiepTheoGio = new JLabel("—");
    private final JLabel lblTiepTheoLienHe = new JLabel(" ");
    private final JLabel lblTiepTheoGhiChu = new JLabel(" ");
    private final JButton btnMoPhieu = new JButton("Mở phiếu khám");
    private final JButton btnLamMoi = PnlTongQuanQuanLy.button("Làm mới", LineIcon.Type.CHART, false);
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"GIỜ HẸN", "BỆNH NHÂN", "DỊCH VỤ", "TRẠNG THÁI"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final List<LichHen> dsLich = new ArrayList<>();
    private final Map<String, PhieuKham> phieuTheoLich = new HashMap<>();
    private SwingWorker<DuLieu, Void> worker;

    public PnlTongQuanNhaSi(TaiKhoan taiKhoan, Consumer<String> moPhieuKham,
            Runnable moLich, Runnable moBenhAn, Consumer<String> xemHoSo) {
        super(new BorderLayout(0, 14));
        this.taiKhoan = taiKhoan;
        this.moPhieuKham = moPhieuKham;
        this.moLich = moLich;
        this.moBenhAn = moBenhAn;
        this.xemHoSo = xemHoSo;
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 20, 24));
        initComponents();
        addEvents();
        lamMoi();
    }

    public void lamMoi() {
        LocalDate homNay = LocalDate.now(ZONE);
        lblNgay.setText("Hôm nay, " + homNay.format(NGAY));
        lblLoi.setText(" ");
        btnLamMoi.setEnabled(false);
        new SwingWorker<DuLieu, Void>() {
            @Override
            protected DuLieu doInBackground() {
                String maNV = taiKhoan.getNhanVien() == null ? null : taiKhoan.getNhanVien().getMaNV();
                List<LichHen> lich = lichHenDAO.timKiem(homNay, homNay, null);
                List<PhieuKham> phieu = phieuKhamDAO.timKiem(homNay, homNay);
                List<LichLamViec> ca = maNV == null ? List.of()
                        : lichLamViecDAO.layTheoNhanVien(maNV, homNay, homNay);
                LichHen tiep = timTiepTheo(lich);
                int huy = tiep == null ? 0
                        : lichHenDAO.demSoLanHuyTrongThangChoNhaSi(tiep.getKhachHang().getMaKH(), homNay);
                return new DuLieu(lich, phieu, ca, huy);
            }

            @Override
            protected void done() {
                try {
                    hienThi(get(), homNay);
                } catch (CancellationException ignored) {
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    HopThoaiLoi.hien(PnlTongQuanNhaSi.this, e.getCause(),
                            "Không thể tải tổng quan nha sĩ.");
                    lblLoi.setText("Không tải được dữ liệu.");
                } finally {
                    btnLamMoi.setEnabled(true);
                }
            }
        }.execute();
    }

    private void initComponents() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        String ten = taiKhoan.getNhanVien() == null ? "" : taiKhoan.getNhanVien().getHoTenNV();
        int gio = LocalTime.now(ZONE).getHour();
        String chao = gio < 12 ? "Chào buổi sáng" : gio < 18 ? "Chào buổi chiều" : "Chào buổi tối";
        JLabel title = new JLabel(chao + ", BS. " + ten);
        title.setFont(bold(21));
        title.setForeground(TEXT);
        lblNgay.setFont(font(12));
        lblNgay.setForeground(MUTED);
        text.add(title);
        text.add(Box.createVerticalStrut(4));
        text.add(lblNgay);
        header.add(text, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actions.setOpaque(false);
        JButton xemLich = PnlTongQuanQuanLy.button("Xem lịch trực", LineIcon.Type.CALENDAR, false);
        xemLich.addActionListener(e -> moLich.run());
        JButton traCuu = PnlTongQuanQuanLy.button("Tra cứu bệnh án", LineIcon.Type.SEARCH, true);
        traCuu.addActionListener(e -> moBenhAn.run());
        actions.add(btnLamMoi);
        actions.add(xemLich);
        actions.add(traCuu);
        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        body.add(taoTheThongKe(), BorderLayout.NORTH);
        body.add(taoNoiDung(), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    private JPanel taoTheThongKe() {
        JPanel p = new JPanel(new GridLayout(1, 4, 14, 0));
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(0, 104));
        p.add(the("Lịch hẹn hôm nay", lblLichHen, lblLichHenSub, LineIcon.Type.CALENDAR, moLich));
        p.add(the("Bệnh nhân đã đến", lblDaDen, lblDaDenSub, LineIcon.Type.CHECK, null));
        p.add(the("Lịch hẹn sắp tới", lblSapToi, lblSapToiSub, LineIcon.Type.CLOCK, moLich));
        p.add(the("Đã hoàn tất", lblHoanTat, lblHoanTatSub, LineIcon.Type.CLOSE, null));
        return p;
    }

    private JPanel the(String title, JLabel value, JLabel sub, LineIcon.Type icon, Runnable khiBam) {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 4));
        card.setBorder(new EmptyBorder(13, 16, 12, 16));
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.add(label(title, font(11), MUTED), BorderLayout.CENTER);
        header.add(new JLabel(new LineIcon(icon, TEAL, 17)), BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);
        value.setFont(bold(30));
        value.setForeground(TEXT);
        card.add(value, BorderLayout.CENTER);
        sub.setFont(font(9));
        sub.setForeground(MUTED);
        card.add(sub, BorderLayout.SOUTH);
        if (khiBam != null) {
            card.setToolTipText("Xem lịch trực");
            ganBam(card, khiBam);
        }
        return card;
    }

    /** Gắn click cho cả thẻ lẫn các nhãn con vì Swing gửi sự kiện cho component sâu nhất. */
    private static void ganBam(Component c, Runnable khiBam) {
        c.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                khiBam.run();
            }
        });
        if (c instanceof java.awt.Container cont) {
            for (Component con : cont.getComponents()) {
                ganBam(con, khiBam);
            }
        }
    }

    private JPanel taoNoiDung() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;

        JPanel trai = new JPanel();
        trai.setOpaque(false);
        trai.setLayout(new BoxLayout(trai, BoxLayout.Y_AXIS));
        trai.add(taoDanhSach());
        trai.add(Box.createVerticalStrut(14));
        trai.add(taoCa());

        JPanel phai = new JPanel();
        phai.setOpaque(false);
        phai.setLayout(new BoxLayout(phai, BoxLayout.Y_AXIS));
        phai.add(taoTiepTheo());
        phai.add(Box.createVerticalStrut(14));
        phai.add(taoLuuY());

        c.gridx = 0;
        c.weightx = 0.64;
        c.insets = new Insets(0, 0, 0, 14);
        p.add(trai, c);

        c.gridx = 1;
        c.weightx = 0.36;
        c.insets = new Insets(0, 0, 0, 0);
        p.add(phai, c);
        return p;
    }

    private JPanel taoDanhSach() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 9));
        card.setBorder(new EmptyBorder(14, 16, 12, 16));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Danh sách bệnh nhân trong ngày");
        title.setFont(bold(13));
        title.setForeground(TEXT);
        lblListSub.setFont(font(9));
        lblListSub.setForeground(MUTED);
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(lblListSub);
        header.add(titleBox, BorderLayout.WEST);
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        lblBadge.setFont(bold(9));
        lblBadge.setForeground(TEAL);
        lblBadge.setOpaque(true);
        lblBadge.setBackground(new Color(0xE7F4F3));
        lblBadge.setBorder(new EmptyBorder(3, 9, 3, 9));
        lblBadge.setAlignmentX(Component.RIGHT_ALIGNMENT);
        right.add(lblBadge);
        right.add(Box.createVerticalStrut(4));
        JPanel dongLink = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        dongLink.setOpaque(false);
        lblLoi.setFont(font(9));
        lblLoi.setForeground(MUTED);
        btnLamMoi.setText("Xem tất cả →");
        btnLamMoi.setFont(font(10));
        btnLamMoi.setForeground(TEAL);
        btnLamMoi.setContentAreaFilled(false);
        btnLamMoi.setBorderPainted(false);
        btnLamMoi.setFocusPainted(false);
        btnLamMoi.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        btnLamMoi.addActionListener(e -> lamMoi());
        dongLink.add(lblLoi);
        dongLink.add(btnLamMoi);
        right.add(dongLink);
        header.add(right, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        table.setRowHeight(40);
        table.setFont(font(11));
        table.getTableHeader().setFont(bold(9));
        table.getTableHeader().setBackground(new Color(0xF7F9FB));
        table.getTableHeader().setForeground(MUTED);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(0xEDF1F5));
        table.setSelectionBackground(new Color(0xEFF6F5));
        table.setSelectionForeground(TEXT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] rong = {58, 170, 210, 110};
        for (int i = 0; i < rong.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        }
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(center);
        table.getColumnModel().getColumn(3).setCellRenderer(new PillRenderer());
        JScrollPane scr = new JScrollPane(table);
        scr.setBorder(BorderFactory.createLineBorder(BORDER));
        scr.getViewport().setBackground(Color.WHITE);
        card.add(scr, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        lblTiepTheoFooter.setFont(font(10));
        lblTiepTheoFooter.setForeground(MUTED);
        footer.add(lblTiepTheoFooter, BorderLayout.WEST);
        btnMoPhieu.setFont(bold(12));
        btnMoPhieu.setBackground(TEAL);
        btnMoPhieu.setForeground(Color.WHITE);
        btnMoPhieu.setPreferredSize(new Dimension(150, 36));
        batNut(false);
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        nut.setOpaque(false);
        nut.add(btnMoPhieu);
        footer.add(nut, BorderLayout.EAST);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private JPanel taoTheoDoi() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(taoTiepTheo());
        p.add(Box.createVerticalStrut(14));
        p.add(taoLuuY());
        return p;
    }

    private JPanel taoCa() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 9));
        card.setBorder(new EmptyBorder(13, 16, 13, 16));
        JLabel title = new JLabel("Ca trực hôm nay");
        title.setFont(bold(13));
        title.setForeground(TEXT);
        card.add(title, BorderLayout.NORTH);
        JPanel cot = new JPanel(new GridLayout(1, 3, 22, 0));
        cot.setOpaque(false);
        cot.add(cotCa("Thời gian", lblCa, lblCaGhiChu));
        cot.add(cotCa("Phòng làm việc", lblCaPhong, new JLabel(" ")));
        cot.add(cotCa("Phụ tá", lblCaPhuTa, new JLabel(" ")));
        card.add(cot, BorderLayout.CENTER);
        return card;
    }

    private JPanel cotCa(String nhan, JLabel chinh, JLabel phu) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = new JLabel(nhan);
        l.setFont(font(9));
        l.setForeground(MUTED);
        chinh.setFont(font(11));
        chinh.setForeground(TEXT);
        phu.setFont(font(9));
        phu.setForeground(MUTED);
        p.add(l);
        p.add(Box.createVerticalStrut(4));
        p.add(chinh);
        p.add(Box.createVerticalStrut(2));
        p.add(phu);
        return p;
    }

    private JPanel taoTiepTheo() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 9));
        card.setBorder(new EmptyBorder(13, 16, 13, 16));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Chuẩn bị lượt khám tiếp theo");
        title.setFont(bold(13));
        title.setForeground(TEXT);
        lblTiepTheoGio.setFont(bold(10));
        lblTiepTheoGio.setForeground(TEAL);
        lblTiepTheoGio.setOpaque(true);
        lblTiepTheoGio.setBackground(new Color(0xE7F4F3));
        lblTiepTheoGio.setBorder(new EmptyBorder(3, 8, 3, 8));
        header.add(title, BorderLayout.WEST);
        header.add(lblTiepTheoGio, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        lblTiepTheo.setFont(bold(15));
        lblTiepTheo.setForeground(TEXT);
        lblTiepTheoMeta.setFont(font(9));
        lblTiepTheoMeta.setForeground(MUTED);
        lblTiepTheoGhiChu.setFont(font(10));
        lblTiepTheoGhiChu.setForeground(MUTED);
        text.add(lblTiepTheo);
        text.add(Box.createVerticalStrut(3));
        text.add(lblTiepTheoMeta);
        text.add(Box.createVerticalStrut(8));
        text.add(lblTiepTheoGhiChu);
        text.add(Box.createVerticalStrut(6));
        lblTiepTheoLienHe.setFont(font(10));
        lblTiepTheoLienHe.setForeground(TEXT);
        text.add(lblTiepTheoLienHe);
        card.add(text, BorderLayout.CENTER);

        JButton xem = new JButton("Xem hồ sơ bệnh án");
        xem.setFont(font(11));
        xem.addActionListener(e -> {
            if (xemHoSo != null && maKHTiepTheo != null) {
                xemHoSo.accept(maKHTiepTheo);
            } else if (moBenhAn != null) {
                moBenhAn.run();
            }
        });
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        footer.add(xem);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private JPanel taoLuuY() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 7));
        card.setBackground(new Color(0xFFF8E8));
        card.setBorder(new EmptyBorder(13, 16, 13, 16));
        JLabel title = new JLabel("Lưu ý trong ca");
        title.setFont(bold(12));
        title.setForeground(new Color(0x9A6500));
        card.add(title, BorderLayout.NORTH);
        lblLuuY.setFont(font(10));
        lblLuuY.setForeground(new Color(0x7A5A1A));
        lblLuuY.setVerticalAlignment(SwingConstants.TOP);
        card.add(lblLuuY, BorderLayout.CENTER);
        JLabel source = new JLabel("Thông tin từ bộ phận lễ tân");
        source.setFont(font(9));
        source.setForeground(MUTED);
        card.add(source, BorderLayout.SOUTH);
        return card;
    }

    private void addEvents() {
        btnMoPhieu.addActionListener(e -> moPhieu());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                capNhatNut();
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    moPhieu();
                }
            }
        });
    }

    private void hienThi(DuLieu d, LocalDate homNay) {
        dsLich.clear();
        dsLich.addAll(d.lich());
        phieuTheoLich.clear();
        for (PhieuKham pk : d.phieu()) {
            if (pk.getLichHen() != null) {
                phieuTheoLich.put(pk.getLichHen().getMaLH(), pk);
            }
        }

        long daDen = d.lich().stream()
                .filter(lh -> lh.getTrangThai() == TrangThaiLichHen.DA_DEN
                || lh.getTrangThai() == TrangThaiLichHen.HOAN_TAT).count();
        long hoanTat = d.phieu().stream()
                .filter(pk -> pk.getTrangThai() == TrangThaiPhieuKham.HOAN_TAT).count();
        LocalDateTime bayGio = LocalDateTime.now(ZONE);
        long sapToi = d.lich().stream()
                .filter(lh -> lh.getTrangThai() == TrangThaiLichHen.DA_DAT
                && lh.getNgayGioHen().isAfter(bayGio)).count();
        LichHen tiep = timTiepTheo(d.lich());
        LichHen daDenDau = d.lich().stream()
                .filter(lh -> lh.getTrangThai() == TrangThaiLichHen.DA_DEN)
                .findFirst().orElse(null);
        lblLichHen.setText(String.valueOf(d.lich().size()));
        String tenBs = taiKhoan.getNhanVien() == null ? "" : taiKhoan.getNhanVien().getHoTenNV();
        lblListSub.setText(d.lich().size() + " lịch hẹn · BS. " + tenBs);
        lblBadge.setText(daDen > 0 ? String.format("%02d bệnh nhân đã đến", daDen) : " ");
        lblBadge.setVisible(daDen > 0);
        lblDaDen.setText(String.valueOf(daDen));
        lblSapToi.setText(String.valueOf(sapToi));
        lblHoanTat.setText(String.valueOf(hoanTat));
        lblDaDenSub.setText(daDenDau == null ? "Chưa có bệnh nhân đến"
                : daDenDau.getKhachHang().getTenKH() + " · Đã check-in");
        lblSapToiSub.setText(tiep != null && tiep.getTrangThai() == TrangThaiLichHen.DA_DAT
                ? "Lịch gần nhất " + tiep.getNgayGioHen().format(GIO)
                : "Không có lịch sắp tới");
        lblHoanTatSub.setText(hoanTat == 0 ? "Chưa có phiếu khám hoàn tất"
                : hoanTat + " phiếu đã hoàn tất");

        model.setRowCount(0);
        for (LichHen lh : d.lich()) {
            String batDau = lh.getNgayGioHen().format(GIO);
            String ketThuc = lh.getDichVu() == null ? null : lh.getNgayGioHen()
                    .plusMinutes(lh.getDichVu().getThoiLuong()).format(GIO);
            model.addRow(new Object[]{
                ketThuc == null ? batDau : batDau + "–" + ketThuc,
                lh.getKhachHang().getTenKH(),
                lh.getDichVu() == null ? "—" : lh.getDichVu().getTenDV(),
                trangThai(lh.getTrangThai())});
        }
        if (d.lich().isEmpty()) {
            lblLoi.setText("Không có lịch hẹn trong ngày.");
        } else {
            lblLoi.setText(" ");
        }
        hienCa(d.ca());
        hienTiepTheo(tiep, d.soLanHuy());
        if (!dsLich.isEmpty()) {
            int chon = 0;
            for (int i = 0; i < dsLich.size(); i++) {
                if (dsLich.get(i) == tiep) {
                    chon = i;
                    break;
                }
            }
            table.setRowSelectionInterval(chon, chon);
        } else {
            batNut(false);
        }
    }

    private void hienCa(List<LichLamViec> ds) {
        if (ds.isEmpty()) {
            lblCa.setText("Chưa có ca làm việc");
            lblCaGhiChu.setText(" ");
            lblCaPhong.setText("—");
            lblCaPhuTa.setText("—");
            lblLichHenSub.setText("Chưa có ca làm việc");
            return;
        }
        LichLamViec ca = ds.get(0);
        String thoiGian = ca.getGioBatDau().format(GIO) + "–" + ca.getGioKetThuc().format(GIO)
                + " · " + nhanCa(ca.getCaTruc());
        lblCa.setText(thoiGian);
        lblCaGhiChu.setText(" ");
        lblCaPhong.setText("Phòng khám B-Clinic");
        lblCaPhuTa.setText("Chưa phân công");
        lblLichHenSub.setText(thoiGian);
    }

    private static LichHen timTiepTheo(List<LichHen> ds) {
        LocalDateTime bayGio = LocalDateTime.now(ZONE);
        for (LichHen lh : ds) {
            if (lh.getTrangThai() == TrangThaiLichHen.DA_DAT
                    && lh.getNgayGioHen().isAfter(bayGio)) {
                return lh;
            }
        }
        return null;
    }

    private void hienTiepTheo(LichHen tiep, int soLanHuy) {
        if (tiep == null) {
            lblTiepTheoGio.setText("—");
            lblTiepTheo.setText("Chưa có bệnh nhân tiếp theo");
            lblTiepTheoMeta.setText(" ");
            lblTiepTheoGhiChu.setText(" ");
            lblTiepTheoLienHe.setText(" ");
            lblTiepTheoFooter.setText(" ");
            lblLuuY.setText("Kiểm tra tiền sử dị ứng và bệnh nền trước khi khám.");
            maKHTiepTheo = null;
            return;
        }
        maKHTiepTheo = tiep.getKhachHang().getMaKH();
        lblTiepTheoGio.setText(tiep.getNgayGioHen().format(GIO));
        lblTiepTheoFooter.setText("Tiếp theo: " + tiep.getKhachHang().getTenKH()
                + " · " + tiep.getNgayGioHen().format(GIO));
        lblTiepTheo.setText(tiep.getKhachHang().getTenKH());
        String gioiTinh = tiep.getKhachHang().isGioiTinh() ? "Nam" : "Nữ";
        String tuoi = "";
        if (tiep.getKhachHang().getNgaySinh() != null) {
            int nam = java.time.Period.between(tiep.getKhachHang().getNgaySinh(),
                    LocalDate.now(ZONE)).getYears();
            tuoi = " · " + nam + " tuổi";
        }
        lblTiepTheoMeta.setText(tiep.getKhachHang().getMaKH() + " · " + gioiTinh + tuoi);
        String lyDo = tiep.getGhiChu() == null || tiep.getGhiChu().isBlank()
                ? (tiep.getDichVu() == null ? "—" : tiep.getDichVu().getTenDV())
                : tiep.getGhiChu();
        lblTiepTheoGhiChu.setText("<html><div style='width:280px'>Lý do đến khám: "
                + escape(lyDo) + "<br><br>Chưa ghi nhận dị ứng, bệnh nền. "
                + "Cần xác minh tiền sử trước khi khám.</div></html>");
        lblTiepTheoLienHe.setText("Liên hệ: " + (tiep.getKhachHang().getSdt() == null
                ? "—" : tiep.getKhachHang().getSdt()));
        if (soLanHuy > 0) {
            lblLuuY.setText("<html><div style='width:260px'>" + escape(tiep.getKhachHang().getTenKH())
                    + " đã hủy " + soLanHuy + " lịch trong tháng "
                    + LocalDate.now(ZONE).getMonthValue() + ". Xác nhận bệnh nhân đến trước khi bắt đầu khám.</div></html>");
        } else {
            lblLuuY.setText("<html><div style='width:260px'>Kiểm tra tiền sử dị ứng và bệnh nền của "
                    + escape(tiep.getKhachHang().getTenKH())
                    + " trước khi bắt đầu khám.</div></html>");
        }
    }

    /** Bật/tắt nút mở phiếu kèm đổi màu để trạng thái disabled nhìn thấy rõ (xám). */
    private void batNut(boolean bat) {
        btnMoPhieu.setEnabled(bat);
        btnMoPhieu.setBackground(bat ? TEAL : new Color(0xDBE2EA));
        btnMoPhieu.setForeground(bat ? Color.WHITE : new Color(0x9AA6B2));
    }

    private void capNhatNut() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= dsLich.size()) {
            batNut(false);
            return;
        }
        LichHen lh = dsLich.get(row);
        PhieuKham pk = phieuTheoLich.get(lh.getMaLH());
        boolean moDuoc = pk != null && (pk.getTrangThai() == TrangThaiPhieuKham.CHO_KHAM
                || pk.getTrangThai() == TrangThaiPhieuKham.DANG_KHAM);
        batNut(moDuoc);
        if (!moDuoc) {
            btnMoPhieu.setToolTipText(lh.getTrangThai() == TrangThaiLichHen.DA_DEN
                    ? "Phiếu khám chưa sẵn sàng." : "Chỉ mở phiếu khám của lượt đã check-in.");
        } else {
            btnMoPhieu.setToolTipText(null);
        }
    }

    private void moPhieu() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= dsLich.size() || moPhieuKham == null) {
            return;
        }
        LichHen lh = dsLich.get(row);
        PhieuKham pk = phieuTheoLich.get(lh.getMaLH());
        if (pk == null || (pk.getTrangThai() != TrangThaiPhieuKham.CHO_KHAM
                && pk.getTrangThai() != TrangThaiPhieuKham.DANG_KHAM)) {
            JOptionPane.showMessageDialog(this, "Lượt khám này chưa có phiếu khám đang chờ.",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        moPhieuKham.accept(lh.getKhachHang().getMaKH());
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String nhanCa(CaTruc ca) {
        return switch (ca) {
            case CA_SANG -> "Ca sáng";
            case CA_CHIEU -> "Ca chiều";
            case CA_NGAY -> "Ca cả ngày";
        };
    }

    private static String trangThai(TrangThaiLichHen tt) {
        return switch (tt) {
            case DA_DAT -> "Đã đặt";
            case DA_DEN -> "Đã đến";
            case HOAN_TAT -> "Hoàn tất";
            case DA_HUY -> "Đã hủy";
            case KHONG_DEN -> "Không đến";
        };
    }

    private static JLabel label(String text, java.awt.Font f, Color c) {
        JLabel l = new JLabel(text);
        l.setFont(f);
        l.setForeground(c);
        return l;
    }

    private record DuLieu(List<LichHen> lich, List<PhieuKham> phieu, List<LichLamViec> ca,
            int soLanHuy) {
    }

    private static final class PillRenderer extends JLabel implements TableCellRenderer {

        PillRenderer() {
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(bold(9));
            setBorder(new EmptyBorder(4, 6, 4, 6));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean focus, int row, int column) {
            String text = value == null ? "" : value.toString();
            setText(text);
            putClientProperty("rowBg", selected ? table.getSelectionBackground() : table.getBackground());
            if ("Đã đến".equals(text)) {
                setForeground(TEAL_DARK);
                putClientProperty("bg", new Color(0xE7F4F3));
            } else if ("Đã đặt".equals(text)) {
                setForeground(GREEN);
                putClientProperty("bg", new Color(0xE6F5EC));
            } else if ("Hoàn tất".equals(text)) {
                setForeground(GREEN);
                putClientProperty("bg", new Color(0xE6F5EC));
            } else if ("Đã hủy".equals(text)) {
                setForeground(new Color(0xC94C48));
                putClientProperty("bg", new Color(0xFDECEA));
            } else {
                setForeground(new Color(0xB67B13));
                putClientProperty("bg", new Color(0xFFF4DD));
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Object rowBg = getClientProperty("rowBg");
            if (rowBg instanceof Color rc) {
                g.setColor(rc);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            Object bg = getClientProperty("bg");
            if (bg instanceof Color c) {
                g.setColor(c);
                g.fillRoundRect(2, 6, getWidth() - 4, getHeight() - 12, 12, 12);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
