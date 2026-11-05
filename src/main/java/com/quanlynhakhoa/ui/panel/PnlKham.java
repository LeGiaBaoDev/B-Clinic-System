package com.quanlynhakhoa.ui.panel;

import com.quanlynhakhoa.config.QuyDinhLichHen;
import com.quanlynhakhoa.dao.DichVuDAO;
import com.quanlynhakhoa.dao.KhachHangDAO;
import com.quanlynhakhoa.dao.PhieuKhamDAO;
import com.quanlynhakhoa.entity.ChiTietPhieuKham;
import com.quanlynhakhoa.entity.DichVu;
import com.quanlynhakhoa.entity.KhachHang;
import com.quanlynhakhoa.entity.PhieuKham;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.TrangThaiPhieuKham;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import com.quanlynhakhoa.ui.dialog.DlgHoSoKhachHang;
import com.quanlynhakhoa.ui.dialog.HopThoaiLoi;
import static com.quanlynhakhoa.ui.component.UiConstants.BACKGROUND;
import static com.quanlynhakhoa.ui.component.UiConstants.BORDER;
import static com.quanlynhakhoa.ui.component.UiConstants.MUTED;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL_LIGHT;
import static com.quanlynhakhoa.ui.component.UiConstants.TEXT;
import static com.quanlynhakhoa.ui.component.UiConstants.bold;
import static com.quanlynhakhoa.ui.component.UiConstants.font;
import com.toedter.calendar.JDateChooser;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class PnlKham extends JPanel {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm");
    private static final String THE_DANH_SACH = "danh-sach";
    private static final String THE_KHAM = "kham";
    private static final String THE1 = "buoc1";
    private static final String THE2 = "buoc2";
    private static final Color AMBER = new Color(0x9A5A00);
    private static final Color AMBER_BG = new Color(0xFFF2DB);
    private static final String[] GIO_SLOT = taoGioSlot();

    private final TaiKhoan taiKhoan;
    private final PhieuKhamDAO phieuKhamDAO = new PhieuKhamDAO();
    private final DichVuDAO dichVuDAO = new DichVuDAO();
    private final CardLayout theTrang = new CardLayout();
    private final JPanel pnlTrang = new JPanel(theTrang);
    private final CardLayout theBuoc = new CardLayout();
    private final JPanel pnlBuoc = new JPanel(theBuoc);
    private final List<PhieuKham> dsPhieu = new ArrayList<>();
    private final List<DichVu> dsDichVu = new ArrayList<>();
    private final List<DichVu> dsHienThi = new ArrayList<>();
    private final Set<DichVu> daChon = new LinkedHashSet<>();
    private final DefaultTableModel modelPhieu = new DefaultTableModel(new String[]{
        "STT", "Mã phiếu", "Bệnh nhân", "Ngày sinh", "Lịch hẹn", "Dịch vụ", "Trạng thái", "Thao tác"
    }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tblPhieu = new JTable(modelPhieu);
    private final JButton btnBatDau = PnlTongQuanQuanLy.button("Bắt đầu khám", LineIcon.Type.ARROW, true);
    private PhieuKham dangKham;
    private boolean dangDungBang;
    private Runnable khiQuayLai;

    // --- wizard header ---
    private final JLabel lblTieuDe = new JLabel();
    private final JLabel lblPhuDe = new JLabel();
    private final JButton btnHeaderBack = PnlTongQuanQuanLy.button("Quay lại", null, false);

    // --- step indicator ---
    private final StepDot dot1 = new StepDot(1);
    private final StepDot dot2 = new StepDot(2);
    private final StepDot dot3 = new StepDot(3);
    private final JLabel lblStep1 = new JLabel("Thực hiện khám");
    private final JLabel lblStep2 = new JLabel("Chỉ định dịch vụ điều trị");
    private final JLabel lblStep3 = new JLabel("Lời dặn & hẹn tái khám");

    // --- patient card ---
    private final JLabel lblAvatar = new JLabel("BN", SwingConstants.CENTER);
    private final JLabel lblTen = new JLabel(" ");
    private final JLabel lblMeta = new JLabel(" ");
    private final JLabel lblDichVuTop = new JLabel(" ");
    private final JLabel lblLichHen = new JLabel(" ");
    private final PillLabel lblTrangThai = new PillLabel();

    // --- bước 1 ---
    private final JTextArea txtLyDo = area(3);
    private final JTextArea txtTrieuChung = area(3);
    private final JTextArea txtDauHieu = area(3);
    private final JTextField txtThoiGian = new JTextField();
    private final JComboBox<String> cboMucDo = new JComboBox<>(new String[]{
        "Bình thường", "Nhẹ", "Trung bình", "Nặng", "Rất nặng"
    });
    private final JCheckBox chkXacNhan = new JCheckBox(
            "Xác minh tiền sử, dị ứng và thuốc đang dùng trước khi điều trị.");
    private final JTextField txtChanDoan = new JTextField();
    private final JComboBox<String> cboViTri = new JComboBox<>(new String[]{
        "Không xác định", "Toàn hàm", "Cung hàm trên", "Cung hàm dưới",
        "Răng cửa", "Răng nanh", "Răng tiền hàm", "Răng hàm", "Răng khôn"});
    private final JTextField txtKemTheo = new JTextField();
    private final TruongChon tcLyDo = new TruongChon("Lý do khám *", txtLyDo, null);
    private final TruongChon tcTrieuChung = new TruongChon("Triệu chứng *", txtTrieuChung, null);
    private final TruongChon tcDauHieu = new TruongChon("Dấu hiệu lâm sàng *", txtDauHieu, null);
    private final TruongChon tcChanDoan = new TruongChon("Chẩn đoán *", txtChanDoan, null);

    // --- bước 2 ---
    private final JTextField txtTimDichVu = new JTextField();
    private final JComboBox<String> cboLoaiDichVu = new JComboBox<>();
    private final DefaultTableModel modelDichVu = new DefaultTableModel(
            new String[]{"Chọn", "Dịch vụ", "Lộ trình", "Đơn giá", "Đơn vị"}, 0) {
        @Override
        public Class<?> getColumnClass(int column) {
            return column == 0 ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 0;
        }
    };
    private final JTable tblDichVu = new JTable(modelDichVu);
    private final JLabel lblDaChon = new JLabel("Chưa chọn dịch vụ");
    private final JLabel lblChanDoanTomTat = new JLabel(" ");
    private final JPanel pnlDongTomTat = new JPanel();
    private final JLabel lblTong = new JLabel("0 đ");

    // --- bước cuối: lời dặn ---
    private final JTextArea txtLoiDan = area(3);
    private final JDateChooser dpTaiKham = new JDateChooser();
    private final JComboBox<String> cboGioTaiKham = new JComboBox<>(GIO_SLOT);
    private final JTextField txtNoiDungTaiKham = new JTextField();

    public PnlKham(TaiKhoan taiKhoan, Runnable dangXuat) {
        super(new BorderLayout());
        this.taiKhoan = java.util.Objects.requireNonNull(taiKhoan);
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 20, 24));
        pnlTrang.setOpaque(false);
        pnlTrang.add(taoDanhSach(), THE_DANH_SACH);
        pnlTrang.add(taoKham(), THE_KHAM);
        add(pnlTrang, BorderLayout.CENTER);
        taiDanhSach();
    }

    /**
     * Tải lại danh sách phiếu khi chuyển vào trang.
     */
    public void lamMoi() {
        taiDanhSach();
        theTrang.show(pnlTrang, THE_DANH_SACH);
    }

    // ---------------------------------------------------------------- danh sách
    private JPanel taoDanhSach() {
        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setOpaque(false);

        JPanel dau = new JPanel(new BorderLayout());
        dau.setOpaque(false);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Phiếu khám hôm nay");
        title.setFont(bold(21));
        title.setForeground(TEXT);
        JLabel sub = new JLabel("Chọn phiếu đang chờ/đang khám để bắt đầu.");
        sub.setFont(font(12));
        sub.setForeground(MUTED);
        text.add(title);
        text.add(Box.createVerticalStrut(4));
        text.add(sub);
        dau.add(text, BorderLayout.WEST);
        JButton refresh = PnlTongQuanQuanLy.button("Làm mới", LineIcon.Type.CHART, false);
        refresh.addActionListener(e -> taiDanhSach());
        dau.add(refresh, BorderLayout.EAST);

        cauHinhBang();
        JScrollPane scr = new JScrollPane(tblPhieu);
        scr.setBorder(BorderFactory.createLineBorder(BORDER));
        scr.getViewport().setBackground(Color.WHITE);

        JPanel body = new SurfacePanel(new BorderLayout());
        body.setBorder(new EmptyBorder(14, 16, 14, 16));
        body.add(scr, BorderLayout.CENTER);
        p.add(dau, BorderLayout.NORTH);
        p.add(body, BorderLayout.CENTER);

        JPanel chan = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        chan.setOpaque(false);
        btnBatDau.setEnabled(false);
        btnBatDau.addActionListener(e -> moPhieuDangChon());
        chan.add(btnBatDau);
        p.add(chan, BorderLayout.SOUTH);
        return p;
    }

    private void cauHinhBang() {
        tblPhieu.setRowHeight(42);
        tblPhieu.setFont(font(12));
        tblPhieu.getTableHeader().setFont(bold(10));
        tblPhieu.getTableHeader().setBackground(new Color(0xF7F9FB));
        tblPhieu.getTableHeader().setForeground(MUTED);
        tblPhieu.setShowVerticalLines(false);
        tblPhieu.setGridColor(new Color(0xEDF1F5));
        tblPhieu.setSelectionBackground(new Color(0xEFF6F5));
        tblPhieu.setSelectionForeground(TEXT);
        tblPhieu.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblPhieu.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                btnBatDau.setEnabled(tblPhieu.getSelectedRow() >= 0);
            }
        });
        tblPhieu.setRowSorter(new javax.swing.table.TableRowSorter<>(modelPhieu));
        int[] rong = {42, 110, 190, 95, 145, 190, 100, 80};
        for (int i = 0; i < rong.length; i++) {
            tblPhieu.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        }
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        tblPhieu.getColumnModel().getColumn(0).setCellRenderer(center);
        tblPhieu.getColumnModel().getColumn(6).setCellRenderer(center);
        tblPhieu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    moPhieuDangChon();
                }
            }
        });
    }

    private void taiDanhSach() {
        new SwingWorker<List<PhieuKham>, Void>() {
            @Override
            protected List<PhieuKham> doInBackground() {
                LocalDate today = LocalDate.now(ZONE);
                return phieuKhamDAO.timKiem(today, today);
            }

            @Override
            protected void done() {
                try {
                    dsPhieu.clear();
                    for (PhieuKham pk : get()) {
                        if (pk.getTrangThai() == TrangThaiPhieuKham.CHO_KHAM
                                || pk.getTrangThai() == TrangThaiPhieuKham.DANG_KHAM) {
                            dsPhieu.add(pk);
                        }
                    }
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
        dsPhieu.sort(java.util.Comparator.comparing(PhieuKham::getNgayLap)
                .thenComparing(PhieuKham::getMaPK));
        modelPhieu.setRowCount(0);
        for (int i = 0; i < dsPhieu.size(); i++) {
            PhieuKham pk = dsPhieu.get(i);
            modelPhieu.addRow(new Object[]{
                i + 1,
                pk.getMaPK(),
                pk.getLichHen().getKhachHang().getTenKH(),
                pk.getLichHen().getKhachHang().getNgaySinh() == null ? ""
                : pk.getLichHen().getKhachHang().getNgaySinh().format(NGAY),
                pk.getLichHen().getNgayGioHen().format(NGAY_GIO),
                pk.getLichHen().getDichVu().getTenDV(),
                pk.getTrangThai() == TrangThaiPhieuKham.CHO_KHAM ? "Chờ khám" : "Đang khám",
                "Khám"});
        }
    }

    private void moPhieuDangChon() {
        int row = tblPhieu.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một phiếu khám.", "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        khiQuayLai = null;
        PhieuKham pk = dsPhieu.get(tblPhieu.convertRowIndexToModel(row));
        moKham(pk);
    }

    
    public void moKhamTheoKhachHang(String maKH, Runnable truocKhiMo, Runnable khiQuayLai) {
        this.khiQuayLai = khiQuayLai;
        new SwingWorker<PhieuKham, Void>() {
            @Override
            protected PhieuKham doInBackground() {
                LocalDate today = LocalDate.now(ZONE);
                for (PhieuKham pk : phieuKhamDAO.timKiem(today, today)) {
                    if ((pk.getTrangThai() == TrangThaiPhieuKham.CHO_KHAM
                            || pk.getTrangThai() == TrangThaiPhieuKham.DANG_KHAM)
                            && pk.getLichHen().getKhachHang().getMaKH().equals(maKH)) {
                        return pk;
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    PhieuKham pk = get();
                    if (pk == null) {
                        JOptionPane.showMessageDialog(PnlKham.this,
                                "Bệnh nhân chưa có phiếu khám chờ/đang khám hôm nay.",
                                "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    if (truocKhiMo != null) {
                        truocKhiMo.run();
                    }
                    moKham(pk);
                } catch (CancellationException ignored) {
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    baoLoi(e.getCause());
                }
            }
        }.execute();
    }

    // ---------------------------------------------------------------- wizard
    private JPanel taoKham() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setOpaque(false);

        JPanel tren = new JPanel(new BorderLayout(0, 12));
        tren.setOpaque(false);
        tren.add(taoWizardHeader(), BorderLayout.NORTH);
        JPanel giua = new JPanel(new BorderLayout(0, 12));
        giua.setOpaque(false);
        giua.add(taoStepIndicator(), BorderLayout.NORTH);
        giua.add(taoBenhNhan(), BorderLayout.CENTER);
        tren.add(giua, BorderLayout.CENTER);
        p.add(tren, BorderLayout.NORTH);

        pnlBuoc.setOpaque(false);
        pnlBuoc.add(taoBuoc1(), THE1);
        pnlBuoc.add(taoBuoc2(), THE2);
        p.add(pnlBuoc, BorderLayout.CENTER);
        return p;
    }

    private JPanel taoWizardHeader() {
        JPanel p = new JPanel(new BorderLayout(16, 0));
        p.setOpaque(false);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        lblTieuDe.setFont(bold(21));
        lblTieuDe.setForeground(TEXT);
        lblTieuDe.setAlignmentX(LEFT_ALIGNMENT);
        lblPhuDe.setFont(font(12));
        lblPhuDe.setForeground(MUTED);
        lblPhuDe.setAlignmentX(LEFT_ALIGNMENT);
        text.add(lblTieuDe);
        text.add(Box.createVerticalStrut(4));
        text.add(lblPhuDe);
        p.add(text, BorderLayout.WEST);

        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        nut.setOpaque(false);
        nut.add(btnHeaderBack);
        p.add(nut, BorderLayout.EAST);
        return p;
    }

    private JPanel taoStepIndicator() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.add(buocItem(dot1, lblStep1));
        p.add(duongNoi());
        p.add(buocItem(dot2, lblStep2));
        p.add(duongNoi());
        p.add(buocItem(dot3, lblStep3));
        return p;
    }

    private static JPanel buocItem(StepDot dot, JLabel text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        text.setFont(font(11));
        text.setForeground(MUTED);
        p.add(dot);
        p.add(text);
        return p;
    }

    private static JPanel duongNoi() {
        JPanel line = new JPanel();
        line.setOpaque(false);
        line.setPreferredSize(new Dimension(34, 2));
        line.setBackground(BORDER);
        line.setOpaque(true);
        return line;
    }

    private JPanel taoBenhNhan() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(16, 0));
        card.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel trai = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        trai.setOpaque(false);
        lblAvatar.setFont(bold(14));
        lblAvatar.setForeground(TEAL);
        lblAvatar.setPreferredSize(new Dimension(48, 48));
        JPanel ten = new JPanel();
        ten.setOpaque(false);
        ten.setLayout(new BoxLayout(ten, BoxLayout.Y_AXIS));
        lblTen.setFont(bold(18));
        lblTen.setForeground(TEXT);
        lblMeta.setFont(font(11));
        lblMeta.setForeground(MUTED);
        ten.add(lblTen);
        ten.add(Box.createVerticalStrut(4));
        ten.add(lblMeta);
        trai.add(new AvatarLabel(lblAvatar));
        trai.add(ten);
        card.add(trai, BorderLayout.WEST);

        JPanel giua = new JPanel();
        giua.setOpaque(false);
        giua.setLayout(new BoxLayout(giua, BoxLayout.Y_AXIS));
        giua.setBorder(new EmptyBorder(2, 10, 0, 0));
        lblDichVuTop.setFont(font(12));
        lblDichVuTop.setForeground(TEXT);
        lblLichHen.setFont(font(11));
        lblLichHen.setForeground(MUTED);
        giua.add(lblDichVuTop);
        giua.add(Box.createVerticalStrut(4));
        giua.add(lblLichHen);
        card.add(giua, BorderLayout.CENTER);

        JPanel phai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        phai.setOpaque(false);
        lblTrangThai.setFont(bold(10));
        JLabel link = linkLabel("Xem hồ sơ →");
        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                moHoSoBenhNhan();
            }
        });
        phai.add(lblTrangThai);
        phai.add(link);
        card.add(phai, BorderLayout.EAST);
        return card;
    }

    private JPanel taoBuoc1() {
        JPanel cards = new JPanel(new GridLayout(1, 2, 14, 0));
        cards.setOpaque(false);

        SurfacePanel trai = new SurfacePanel(new BorderLayout(0, 10));
        trai.setBorder(new EmptyBorder(14, 16, 14, 16));
        trai.add(tieuDeThe("Triệu chứng & khám lâm sàng", null), BorderLayout.NORTH);
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(tcLyDo);
        form.add(tcTrieuChung);
        JPanel hang = new JPanel(new GridLayout(1, 2, 12, 0));
        hang.setOpaque(false);
        hang.add(oNhap("Thời gian xuất hiện", txtThoiGian));
        hang.add(oNhap("Mức độ đau", cboMucDo));
        form.add(hang);
        form.add(tcDauHieu);
        chkXacNhan.setOpaque(false);
        chkXacNhan.setFont(font(10));
        chkXacNhan.setForeground(MUTED);
        JPanel oChk = new JPanel(new BorderLayout());
        oChk.setBackground(new Color(0xF7F9FB));
        oChk.setBorder(new EmptyBorder(8, 10, 8, 10));
        oChk.add(chkXacNhan, BorderLayout.CENTER);
        form.add(oChk);
        JPanel holderTrai = new JPanel(new BorderLayout());
        holderTrai.setOpaque(false);
        holderTrai.add(form, BorderLayout.NORTH);
        trai.add(holderTrai, BorderLayout.CENTER);
        cards.add(trai);

        SurfacePanel phai = new SurfacePanel(new BorderLayout(0, 10));
        phai.setBorder(new EmptyBorder(14, 16, 14, 16));
        phai.add(tieuDeThe("Chẩn đoán",
                pill("Dự kiến", AMBER, AMBER_BG)), BorderLayout.NORTH);
        JPanel form2 = new JPanel();
        form2.setOpaque(false);
        form2.setLayout(new BoxLayout(form2, BoxLayout.Y_AXIS));
        form2.add(tcChanDoan);
        cboViTri.setEditable(true);
        cboViTri.setFont(font(11));
        form2.add(oNhap("Vị trí / Răng liên quan", cboViTri));
        form2.add(oNhap("Chẩn đoán kèm theo", txtKemTheo));
        JPanel holderPhai = new JPanel(new BorderLayout());
        holderPhai.setOpaque(false);
        holderPhai.add(form2, BorderLayout.NORTH);
        phai.add(holderPhai, BorderLayout.CENTER);
        cards.add(phai);

        JButton huy = PnlTongQuanQuanLy.button("Hủy thay đổi", null, false);
        huy.addActionListener(e -> {
            int chon = JOptionPane.showConfirmDialog(this,
                    "Hủy các thay đổi chưa lưu của phiếu khám?", "Xác nhận hủy",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (chon == JOptionPane.YES_OPTION) {
                xoaForm();
            }
        });
        JButton tiep = PnlTongQuanQuanLy.button("Tiếp tục: Chỉ định dịch vụ", LineIcon.Type.ARROW, true);
        tiep.addActionListener(e -> {
            if (kiemTraBuoc1()) {
                chonBuoc(THE2);
            }
        });
        return bocBuoc(cards,
                "* Thông tin bắt buộc · Phiếu khám đang ở trạng thái nháp, chưa ghi vào bệnh án.",
                huy, tiep);
    }

    private JPanel taoBuoc2() {
        JPanel tren = new JPanel(new BorderLayout(14, 0));
        tren.setOpaque(false);

        SurfacePanel trai = new SurfacePanel(new BorderLayout(0, 10));
        trai.setBorder(new EmptyBorder(14, 16, 14, 16));
        lblDaChon.setFont(font(11));
        lblDaChon.setForeground(TEAL);
        trai.add(tieuDeThe("Chọn dịch vụ điều trị", lblDaChon), BorderLayout.NORTH);

        JPanel giua = new JPanel(new BorderLayout(0, 10));
        giua.setOpaque(false);
        JPanel loc = new JPanel(new BorderLayout(8, 0));
        loc.setOpaque(false);
        txtTimDichVu.setFont(font(11));
        txtTimDichVu.putClientProperty("JTextField.placeholderText", "Tìm mã hoặc tên dịch vụ...");
        txtTimDichVu.putClientProperty("JTextField.leadingIcon", new LineIcon(LineIcon.Type.SEARCH, MUTED, 15));
        txtTimDichVu.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                locDichVu();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                locDichVu();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                locDichVu();
            }
        });
        cboLoaiDichVu.setFont(font(11));
        cboLoaiDichVu.setPreferredSize(new Dimension(170, 34));
        cboLoaiDichVu.addActionListener(e -> locDichVu());
        loc.add(txtTimDichVu, BorderLayout.CENTER);
        loc.add(cboLoaiDichVu, BorderLayout.EAST);
        giua.add(loc, BorderLayout.NORTH);

        cauHinhBangDichVu();
        JScrollPane scr = new JScrollPane(tblDichVu);
        scr.setBorder(BorderFactory.createLineBorder(BORDER));
        scr.getViewport().setBackground(Color.WHITE);
        scr.setPreferredSize(new Dimension(0, 200));
        giua.add(scr, BorderLayout.CENTER);
        JPanel chanTrai = new JPanel(new BorderLayout());
        chanTrai.setOpaque(false);
        JLabel ghiChu = new JLabel("Vị trí điều trị: Toàn hàm · Số lượng: 1 / dịch vụ");
        ghiChu.setFont(font(10));
        ghiChu.setForeground(MUTED);
        JLabel them = linkLabel("+ Dịch vụ khác");
        them.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                thongBaoChuaHoTro("Thêm dịch vụ ngoài danh mục");
            }
        });
        chanTrai.add(ghiChu, BorderLayout.WEST);
        chanTrai.add(them, BorderLayout.EAST);
        giua.add(chanTrai, BorderLayout.SOUTH);
        trai.add(giua, BorderLayout.CENTER);
        tren.add(trai, BorderLayout.CENTER);

        SurfacePanel phai = new SurfacePanel(new BorderLayout(0, 10));
        phai.setBorder(new EmptyBorder(14, 16, 14, 16));
        phai.setPreferredSize(new Dimension(360, 0));
        phai.add(tieuDeThe("Tóm tắt chỉ định", null), BorderLayout.NORTH);
        JPanel tomTat = new JPanel();
        tomTat.setOpaque(false);
        tomTat.setLayout(new BoxLayout(tomTat, BoxLayout.Y_AXIS));
        JPanel dongIcd = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        dongIcd.setOpaque(false);
        dongIcd.setAlignmentX(LEFT_ALIGNMENT);
        lblChanDoanTomTat.setFont(font(11));
        lblChanDoanTomTat.setForeground(TEXT);
        dongIcd.add(lblChanDoanTomTat);
        tomTat.add(dongIcd);
        tomTat.add(Box.createVerticalStrut(10));
        pnlDongTomTat.setOpaque(false);
        pnlDongTomTat.setLayout(new BoxLayout(pnlDongTomTat, BoxLayout.Y_AXIS));
        pnlDongTomTat.setAlignmentX(LEFT_ALIGNMENT);
        tomTat.add(pnlDongTomTat);
        tomTat.add(Box.createVerticalStrut(10));
        JPanel ngan = new JPanel();
        ngan.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        ngan.setBackground(BORDER);
        ngan.setAlignmentX(LEFT_ALIGNMENT);
        tomTat.add(ngan);
        tomTat.add(Box.createVerticalStrut(10));
        JPanel dongTong = new JPanel(new BorderLayout());
        dongTong.setOpaque(false);
        dongTong.setAlignmentX(LEFT_ALIGNMENT);
        dongTong.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JLabel nhanTong = new JLabel("Tổng chi phí dự kiến");
        nhanTong.setFont(bold(12));
        nhanTong.setForeground(TEXT);
        lblTong.setFont(bold(20));
        lblTong.setForeground(TEAL);
        dongTong.add(nhanTong, BorderLayout.WEST);
        dongTong.add(lblTong, BorderLayout.EAST);
        tomTat.add(dongTong);
        tomTat.add(Box.createVerticalStrut(8));
        JLabel note = new JLabel("<html><div style='width:300px'>Chỉ định sẽ chuyển đến bộ phận "
                + "lễ tân để lập hóa đơn sau khi hoàn tất. Chưa ghi nhận thanh toán.</div></html>");
        note.setFont(font(10));
        note.setForeground(MUTED);
        note.setAlignmentX(LEFT_ALIGNMENT);
        tomTat.add(note);
        phai.add(tomTat, BorderLayout.NORTH);
        tren.add(phai, BorderLayout.EAST);

        SurfacePanel cuoi = new SurfacePanel(new BorderLayout(0, 10));
        cuoi.setBorder(new EmptyBorder(14, 16, 14, 16));
        cuoi.add(tieuDeThe("Lời dặn & hẹn tái khám",
                pill("Bước cuối", AMBER, AMBER_BG)), BorderLayout.NORTH);
        JPanel formCuoi = new JPanel(new GridLayout(1, 2, 16, 0));
        formCuoi.setOpaque(false);
        JPanel coTrai = new JPanel(new BorderLayout(0, 6));
        coTrai.setOpaque(false);
        coTrai.add(oVanBan("Lời dặn sau điều trị *", txtLoiDan), BorderLayout.CENTER);
        formCuoi.add(coTrai);
        JPanel coPhai = new JPanel();
        coPhai.setOpaque(false);
        coPhai.setLayout(new BoxLayout(coPhai, BoxLayout.Y_AXIS));
        JPanel hangNgay = new JPanel(new GridLayout(1, 2, 12, 0));
        hangNgay.setOpaque(false);
        hangNgay.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        hangNgay.add(oNhap("Ngày tái khám (nếu cần)", taoONgay()));
        hangNgay.add(oNhap("Giờ hẹn", cboGioTaiKham));
        coPhai.add(hangNgay);
        coPhai.add(oNhap("Nội dung tái khám", txtNoiDungTaiKham));
        formCuoi.add(coPhai);
        cuoi.add(formCuoi, BorderLayout.CENTER);

        JPanel noiDung = new JPanel();
        noiDung.setOpaque(false);
        noiDung.setLayout(new BoxLayout(noiDung, BoxLayout.Y_AXIS));
        tren.setAlignmentX(LEFT_ALIGNMENT);
        cuoi.setAlignmentX(LEFT_ALIGNMENT);
        noiDung.add(tren);
        noiDung.add(Box.createVerticalStrut(12));
        noiDung.add(cuoi);

        JButton luu = PnlTongQuanQuanLy.button("Lưu & hoàn tất khám", LineIcon.Type.CHECK, true);
        luu.addActionListener(e -> hoanTat());
        return bocBuoc(noiDung,
                "* Thông tin bắt buộc · Kiểm tra nội dung trước khi lưu vào bệnh án.",
                null, luu);
    }

    private void cauHinhBangDichVu() {
        tblDichVu.setRowHeight(48);
        tblDichVu.setFont(font(11));
        tblDichVu.setShowVerticalLines(false);
        tblDichVu.setGridColor(new Color(0xEDF1F5));
        tblDichVu.setFillsViewportHeight(true);
        tblDichVu.setSelectionBackground(Color.WHITE);
        tblDichVu.setSelectionForeground(TEXT);
        tblDichVu.getTableHeader().setFont(bold(10));
        tblDichVu.getTableHeader().setBackground(new Color(0xF7F9FB));
        tblDichVu.getTableHeader().setForeground(MUTED);
        tblDichVu.getTableHeader().setReorderingAllowed(false);
        int[] rong = {48, 210, 250, 90, 60};
        for (int i = 0; i < rong.length; i++) {
            tblDichVu.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
            tblDichVu.getColumnModel().getColumn(i).setMaxWidth(
                    i == 1 || i == 2 ? Integer.MAX_VALUE : rong[i] + 20);
        }
        DefaultTableCellRenderer phai = new DefaultTableCellRenderer();
        phai.setHorizontalAlignment(SwingConstants.RIGHT);
        tblDichVu.getColumnModel().getColumn(3).setCellRenderer(phai);
        modelDichVu.addTableModelListener(e -> {
            if (!dangDungBang && e.getColumn() == 0) {
                dongBoChon();
                capNhatTomTat();
            }
        });
    }

    // ---------------------------------------------------------------- helpers UI
    private static JTextArea area(int rows) {
        JTextArea a = new JTextArea(rows, 20);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(font(11));
        a.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(6, 8, 6, 8)));
        return a;
    }

    private static JPanel oVanBan(String nhan, JTextArea area) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel l = new JLabel(nhan);
        l.setFont(font(10));
        l.setForeground(MUTED);
        p.add(l, BorderLayout.NORTH);
        p.add(new JScrollPane(area), BorderLayout.CENTER);
        p.setBorder(new EmptyBorder(0, 0, 8, 0));
        return p;
    }

    private static JPanel oNhap(String nhan, Component field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel l = new JLabel(nhan);
        l.setFont(font(10));
        l.setForeground(MUTED);
        if (field instanceof JTextField tf) {
            tf.setFont(font(11));
            tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER), new EmptyBorder(6, 8, 6, 8)));
        }
        if (field instanceof JComboBox<?> cb) {
            cb.setFont(font(11));
        }
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        p.setBorder(new EmptyBorder(0, 0, 8, 0));
        return p;
    }

    private static JPanel tieuDeThe(String tieuDe, Component phai) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel t = new JLabel(tieuDe);
        t.setFont(bold(14));
        t.setForeground(TEXT);
        p.add(t, BorderLayout.WEST);
        if (phai != null) {
            JPanel wrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            wrap.setOpaque(false);
            wrap.add(phai);
            p.add(wrap, BorderLayout.EAST);
        }
        p.setBorder(new EmptyBorder(0, 0, 2, 0));
        return p;
    }

    private static JPanel bocBuoc(Component noiDung, String ghiChu, JButton trai, JButton phai) {
        JPanel wrap = new JPanel(new BorderLayout(0, 12));
        wrap.setOpaque(false);
        wrap.add(noiDung, BorderLayout.CENTER);
        JPanel chan = new JPanel(new BorderLayout(10, 0));
        chan.setOpaque(false);
        JLabel note = new JLabel(ghiChu);
        note.setFont(font(10));
        note.setForeground(MUTED);
        chan.add(note, BorderLayout.WEST);
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        nut.setOpaque(false);
        if (trai != null) {
            nut.add(trai);
        }
        nut.add(phai);
        chan.add(nut, BorderLayout.EAST);
        wrap.add(chan, BorderLayout.SOUTH);
        return wrap;
    }

    private static JLabel linkLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(bold(11));
        l.setForeground(TEAL);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return l;
    }

    private static PillLabel pill(String text, Color fg, Color bg) {
        PillLabel l = new PillLabel();
        l.setFont(bold(10));
        l.set(text, fg, bg);
        return l;
    }

    // ---------------------------------------------------------------- logic
    private void moKham(PhieuKham pk) {
        dangKham = pk;
        var kh = pk.getLichHen().getKhachHang();
        lblAvatar.setText(initials(kh.getTenKH()));
        lblTen.setText(kh.getTenKH());
        String tuoi = kh.getNgaySinh() == null ? ""
                : " (" + Period.between(kh.getNgaySinh(), LocalDate.now(ZONE)).getYears() + " tuổi)";
        lblMeta.setText(kh.getMaKH() + " · " + (kh.isGioiTinh() ? "Nam" : "Nữ") + " · "
                + (kh.getNgaySinh() == null ? "—" : kh.getNgaySinh().format(NGAY)) + tuoi);
        lblDichVuTop.setText(pk.getLichHen().getDichVu().getTenDV());
        lblLichHen.setText(pk.getLichHen().getNgayGioHen().format(NGAY_GIO) + " · Phòng khám B-Clinic");
        lblTrangThai.set("Bản nháp", AMBER, AMBER_BG);
        xoaForm();
        theTrang.show(pnlTrang, THE_KHAM);
        chonBuoc(THE1);
        if (dsDichVu.isEmpty()) {
            taiDichVu();
        } else {
            chonDichVuGoc();
        }
    }

    /** Chọn sẵn dịch vụ ban đầu của lịch hẹn; nha sĩ vẫn có thể bỏ/chọn dịch vụ khác. */
    private void chonDichVuGoc() {
        if (dangKham == null || dangKham.getLichHen() == null
                || dangKham.getLichHen().getDichVu() == null) {
            return;
        }
        String maGoc = dangKham.getLichHen().getDichVu().getMaDV();
        for (DichVu dv : dsDichVu) {
            if (maGoc.equals(dv.getMaDV())) {
                daChon.add(dv);
                dungBangDichVu();
                capNhatTomTat();
                return;
            }
        }
    }

    private void moHoSoBenhNhan() {
        if (dangKham == null || dangKham.getLichHen() == null) {
            JOptionPane.showMessageDialog(this, "Chưa có phiếu khám để xem hồ sơ.",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        KhachHang kh = dangKham.getLichHen().getKhachHang();
        new javax.swing.SwingWorker<KhachHangDAO.TomTat, Void>() {
            @Override
            protected KhachHangDAO.TomTat doInBackground() {
                return new KhachHangDAO().timKiemTomTat(kh.getMaKH()).stream()
                        .filter(t -> t.khachHang().getMaKH().equals(kh.getMaKH()))
                        .findFirst().orElse(null);
            }

            @Override
            protected void done() {
                try {
                    KhachHangDAO.TomTat tt = get();
                    if (tt == null) {
                        JOptionPane.showMessageDialog(PnlKham.this,
                                "Không tìm thấy hồ sơ bệnh nhân.", "Thông báo",
                                JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    DlgHoSoKhachHang.hien(javax.swing.SwingUtilities.getWindowAncestor(PnlKham.this),
                            tt, () -> {
                            });
                } catch (java.util.concurrent.ExecutionException e) {
                    HopThoaiLoi.hien(PnlKham.this, e.getCause(), "Không thể mở hồ sơ bệnh nhân.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void quayLaiDanhSach() {
        if (khiQuayLai != null) {
            Runnable callback = khiQuayLai;
            khiQuayLai = null;
            callback.run();
            return;
        }
        theTrang.show(pnlTrang, THE_DANH_SACH);
        taiDanhSach();
    }

    private void xoaForm() {
        tcLyDo.reset();
        tcTrieuChung.reset();
        tcDauHieu.reset();
        tcChanDoan.reset();
        txtLoiDan.setText("");
        for (JTextField f : new JTextField[]{txtThoiGian, txtKemTheo, txtNoiDungTaiKham}) {
            f.setText("");
        }
        cboViTri.setSelectedIndex(0);
        dpTaiKham.setDate(null);
        cboMucDo.setSelectedIndex(0);
        cboGioTaiKham.setSelectedIndex(0);
        chkXacNhan.setSelected(false);
        daChon.clear();
        dungBangDichVu();
        capNhatTomTat();
    }

    private String giaTriViTri() {
        Object item = cboViTri.isEditable() ? cboViTri.getEditor().getItem() : cboViTri.getSelectedItem();
        String s = item == null ? "" : item.toString().trim();
        return "Không xác định".equals(s) ? "" : s;
    }

    private boolean kiemTraBuoc1() {
        if (tcLyDo.giaTri().isBlank() || tcTrieuChung.giaTri().isBlank()
                || tcDauHieu.giaTri().isBlank() || tcChanDoan.giaTri().isBlank()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đủ các trường bắt buộc (*).",
                    "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private void chonBuoc(String buoc) {
        theBuoc.show(pnlBuoc, buoc);
        boolean b1 = THE1.equals(buoc);
        lblTieuDe.setText(b1 ? "Thực hiện khám" : "Chỉ định dịch vụ điều trị");
        if (dangKham != null) {
            lblPhuDe.setText(b1
                    ? "BS. " + taiKhoan.getNhanVien().getHoTenNV() + " · Phòng khám B-Clinic"
                    : "Bước cuối: kiểm tra chỉ định, lời dặn và lịch tái khám");
        }
        btnHeaderBack.setText("Quay lại");
        setAction(btnHeaderBack, b1 ? this::quayLaiDanhSach : () -> chonBuoc(THE1));
        dot1.set(b1 ? StepDot.ACTIVE : StepDot.DONE);
        dot2.set(b1 ? StepDot.IDLE : StepDot.ACTIVE);
        dot3.set(b1 ? StepDot.IDLE : StepDot.ACTIVE);
        napMauStep(lblStep1, b1);
        napMauStep(lblStep2, !b1);
        napMauStep(lblStep3, !b1);
        if (!b1) {
            capNhatTomTat();
        }
    }

    private static void napMauStep(JLabel l, boolean active) {
        l.setFont(active ? bold(11) : font(11));
        l.setForeground(active ? TEAL : MUTED);
    }

    private static void setAction(JButton b, Runnable r) {
        for (var al : b.getActionListeners()) {
            b.removeActionListener(al);
        }
        b.addActionListener(e -> r.run());
    }

    private void taiDichVu() {
        new SwingWorker<List<DichVu>, Void>() {
            @Override
            protected List<DichVu> doInBackground() {
                return dichVuDAO.timKiem("", true);
            }

            @Override
            protected void done() {
                try {
                    dsDichVu.clear();
                    dsDichVu.addAll(get());
                    napLoaiDichVu();
                    chonDichVuGoc();
                    dungBangDichVu();
                } catch (CancellationException ignored) {
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    baoLoi(e.getCause());
                }
            }
        }.execute();
    }

    private void napLoaiDichVu() {
        Set<String> loai = new LinkedHashSet<>();
        for (DichVu dv : dsDichVu) {
            if (dv.getLoaiDichVu() != null && dv.getLoaiDichVu().getTenLoaiDV() != null) {
                loai.add(dv.getLoaiDichVu().getTenLoaiDV());
            }
        }
        cboLoaiDichVu.removeAllItems();
        cboLoaiDichVu.addItem("Tất cả danh mục");
        loai.forEach(cboLoaiDichVu::addItem);
    }

    private void dungBangDichVu() {
        String khoa = txtTimDichVu.getText() == null ? "" : txtTimDichVu.getText().trim().toLowerCase(Locale.ROOT);
        Object loaiSel = cboLoaiDichVu.getSelectedItem();
        String loai = loaiSel == null || cboLoaiDichVu.getSelectedIndex() <= 0 ? null : loaiSel.toString();
        dangDungBang = true;
        modelDichVu.setRowCount(0);
        dsHienThi.clear();
        for (DichVu dv : dsDichVu) {
            boolean hopKhoa = khoa.isBlank()
                    || dv.getTenDV().toLowerCase(Locale.ROOT).contains(khoa)
                    || (dv.getMaDV() != null && dv.getMaDV().toLowerCase(Locale.ROOT).contains(khoa));
            boolean hopLoai = loai == null || (dv.getLoaiDichVu() != null
                    && loai.equals(dv.getLoaiDichVu().getTenLoaiDV()));
            if (hopKhoa && hopLoai) {
                dsHienThi.add(dv);
                String loTrinh = dv.getLoTrinhDichVu();
                String ten = "<html><b>" + escapeHtml(dv.getTenDV()) + "</b>";
                if (loTrinh != null) {
                    ten += "<br><span style='color:#007B83;font-size:9px'>Nhiều buổi</span>";
                }
                ten += "</html>";
                String lo = loTrinh == null ? "—"
                        : "<html><div style='width:230px'>" + escapeHtml(loTrinh) + "</div></html>";
                modelDichVu.addRow(new Object[]{
                    daChon.contains(dv),
                    ten,
                    lo,
                    money(dv.getDonGia()),
                    "Lần"});
            }
        }
        dangDungBang = false;
    }

    private void locDichVu() {
        dungBangDichVu();
    }

    private void dongBoChon() {
        for (int i = 0; i < dsHienThi.size() && i < modelDichVu.getRowCount(); i++) {
            DichVu dv = dsHienThi.get(i);
            boolean sel = Boolean.TRUE.equals(modelDichVu.getValueAt(i, 0));
            if (sel) {
                daChon.add(dv);
            } else {
                daChon.remove(dv);
            }
        }
    }

    private void capNhatTomTat() {
        pnlDongTomTat.removeAll();
        double tong = 0;
        for (DichVu dv : daChon) {
            tong += dv.getDonGia();
            JPanel dong = new JPanel(new BorderLayout());
            dong.setOpaque(false);
            dong.setAlignmentX(LEFT_ALIGNMENT);
            dong.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
            JLabel ten = new JLabel(dv.getTenDV()
                    + (dv.getLoTrinhDichVu() == null ? "" : " (nhiều buổi)") + " × 1");
            ten.setFont(font(11));
            ten.setForeground(TEXT);
            JLabel gia = new JLabel(money(dv.getDonGia()));
            gia.setFont(font(11));
            gia.setForeground(TEXT);
            dong.add(ten, BorderLayout.WEST);
            dong.add(gia, BorderLayout.EAST);
            pnlDongTomTat.add(dong);
            pnlDongTomTat.add(Box.createVerticalStrut(6));
        }
        if (daChon.isEmpty()) {
            JLabel trong = new JLabel("Chưa chọn dịch vụ điều trị.");
            trong.setFont(font(11));
            trong.setForeground(MUTED);
            trong.setAlignmentX(LEFT_ALIGNMENT);
            pnlDongTomTat.add(trong);
        }
        boolean nhieuBuoi = coNhieuBuoi();
        if (nhieuBuoi && dpTaiKham.getDate() == null) {
            LocalDate goiY = LocalDate.now(ZONE).plusDays(QuyDinhLichHen.macDinh().taiKhamMinNgay());
            dpTaiKham.setDate(Date.from(goiY.atStartOfDay(ZONE).toInstant()));
        }
        lblDaChon.setText(daChon.isEmpty() ? "Chưa chọn dịch vụ"
                : "Đã chọn " + daChon.size() + " dịch vụ"
                + (nhieuBuoi ? " · cần hẹn buổi kế tiếp" : ""));
        lblTong.setText(money(tong));
        String cd = tcChanDoan.giaTri().isBlank() ? "Chưa có chẩn đoán" : tcChanDoan.giaTri().trim();
        if (!giaTriViTri().isBlank()) {
            cd += " · " + giaTriViTri();
        }
        lblChanDoanTomTat.setText(cd);
        pnlDongTomTat.revalidate();
        pnlDongTomTat.repaint();
    }

    private void thongBaoChuaHoTro(String ten) {
        JOptionPane.showMessageDialog(this, ten + " sẽ được bổ sung ở phiên bản sau.",
                "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    private boolean coNhieuBuoi() {
        return daChon.stream().anyMatch(dv -> dv.getLoTrinhDichVu() != null);
    }

    private void hoanTat() {
        if (daChon.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chưa chọn dịch vụ điều trị.",
                    "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (coNhieuBuoi()) {
            if (dpTaiKham.getDate() == null) {
                JOptionPane.showMessageDialog(this,
                        "Dịch vụ đã chọn điều trị nhiều buổi. Vui lòng chọn ngày tái khám.",
                        "Thiếu lịch tái khám", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Object gio = cboGioTaiKham.getSelectedItem();
            if (gio == null || "—".equals(gio.toString()) || gio.toString().isBlank()) {
                JOptionPane.showMessageDialog(this,
                        "Dịch vụ đã chọn điều trị nhiều buổi. Vui lòng chọn giờ hẹn tái khám.",
                        "Thiếu giờ tái khám", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        if (txtLoiDan.getText().isBlank()) {
            int chon = JOptionPane.showConfirmDialog(this,
                    "Bạn chưa nhập lời dặn, vẫn muốn hoàn tất?", "Xác nhận",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (chon != JOptionPane.YES_OPTION) {
                return;
            }
        }
        String trieuChung = cat(500, "Lý do: " + tcLyDo.giaTri()
                + "\nTriệu chứng: " + tcTrieuChung.giaTri()
                + "\nThời gian: " + txtThoiGian.getText()
                + "\nMức độ: " + cboMucDo.getSelectedItem()
                + "\nDấu hiệu: " + tcDauHieu.giaTri());
        String chanDoan = cat(500, "Chẩn đoán: " + tcChanDoan.giaTri()
                + "\nVị trí: " + giaTriViTri()
                + "\nKèm theo: " + txtKemTheo.getText());
        String ketQua = null;
        String ngayTK = ngayTaiKham();
        String loiDan = cat(500, txtLoiDan.getText()
                + (ngayTK.isBlank() ? "" : "\nTái khám: " + ngayTK + gioTaiKham())
                + (txtNoiDungTaiKham.getText().isBlank() ? "" : "\nNội dung: " + txtNoiDungTaiKham.getText()));
        List<ChiTietPhieuKham> chiTiet = new ArrayList<>();
        try {
            dangKham.setLoiDan(loiDan);
            for (DichVu dv : daChon) {
                chiTiet.add(new ChiTietPhieuKham(0, trieuChung, chanDoan, dv.getDonGia(), 1, "Lần", 0,
                        ketQua, dangKham, dv, taiKhoan.getNhanVien()));
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                if (dangKham.getTrangThai() == TrangThaiPhieuKham.CHO_KHAM) {
                    phieuKhamDAO.batDauKham(dangKham.getMaPK());
                }
                phieuKhamDAO.hoanTatPhieuKham(dangKham, chiTiet);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(PnlKham.this,
                            "Đã hoàn tất khám và tạo hóa đơn chờ thanh toán.",
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    quayLaiDanhSach();
                } catch (CancellationException ignored) {
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    baoLoi(e.getCause());
                }
            }
        }.execute();
    }

    private void baoLoi(Throwable loi) {
        if (loi instanceof BusinessException) {
            JOptionPane.showMessageDialog(this, loi.getMessage(), "Cảnh báo", JOptionPane.WARNING_MESSAGE);
        } else if (loi instanceof DataAccessException) {
            JOptionPane.showMessageDialog(this, "Không thể tải/ lưu dữ liệu khám.", "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Đã xảy ra lỗi, vui lòng thử lại.", "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String escapeHtml(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String cat(int max, String text) {
        if (text == null) {
            return "";
        }
        String s = text.trim();
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "BN";
        }
        String[] tu = name.trim().split("\\s+");
        if (tu.length == 1) {
            return tu[0].substring(0, Math.min(2, tu[0].length())).toUpperCase(Locale.ROOT);
        }
        String a = tu[tu.length - 2].substring(0, 1);
        String b = tu[tu.length - 1].substring(0, 1);
        return (a + b).toUpperCase(Locale.ROOT);
    }

    private static String money(double value) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.of("vi", "VN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(value) + " đ";
    }

    private static String[] taoGioSlot() {
        List<String> slot = new ArrayList<>();
        slot.add("—");
        for (int h = 8; h < 17; h++) {
            slot.add(String.format("%02d:00–%02d:30", h, h));
            slot.add(String.format("%02d:30–%02d:00", h, h + 1));
        }
        return slot.toArray(new String[0]);
    }

    /**
     * Ô chọn ngày tái khám dùng JDateChooser giống màn Tiếp nhận của lễ tân.
     */
    private JPanel taoONgay() {
        dpTaiKham.setDateFormatString("dd/MM/yyyy");
        QuyDinhLichHen quyDinh = QuyDinhLichHen.macDinh();
        LocalDate homNay = LocalDate.now(ZONE);
        LocalDate min = homNay.plusDays(quyDinh.taiKhamMinNgay());
        LocalDate max = homNay.plusDays(quyDinh.taiKhamMaxNgay());
        dpTaiKham.setMinSelectableDate(Date.from(min.atStartOfDay(ZONE).toInstant()));
        dpTaiKham.setMaxSelectableDate(Date.from(max.atTime(23, 59, 59).atZone(ZONE).toInstant()));
        dpTaiKham.setDate(null);
        dpTaiKham.setFont(font(11));
        dpTaiKham.setOpaque(false);
        dpTaiKham.setBorder(null);
        try {
            com.toedter.calendar.JCalendar jc = dpTaiKham.getJCalendar();
            jc.setLocale(Locale.of("vi", "VN"));
            jc.setPreferredSize(new Dimension(360, 300));
            jc.setWeekOfYearVisible(false);
            jc.setDecorationBackgroundVisible(false);
            jc.setDecorationBordersVisible(false);
            jc.setTodayButtonVisible(true);
            jc.setTodayButtonText("Hôm nay");
            jc.setNullDateButtonVisible(false);
            jc.getDayChooser().setBackground(Color.WHITE);
            jc.getDayChooser().setDecorationBackgroundColor(TEAL_LIGHT);
            jc.getDayChooser().setWeekdayForeground(TEXT);
            jc.getMonthChooser().setFont(bold(13));
            jc.getYearChooser().setFont(bold(13));
        } catch (RuntimeException ignored) {
        }
        if (dpTaiKham.getDateEditor().getUiComponent() instanceof JComponent c) {
            c.setFont(font(11));
            c.setForeground(TEXT);
            c.setOpaque(false);
            c.setBorder(null);
            c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            c.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    dpTaiKham.getCalendarButton().doClick();
                }
            });
        }
        JButton btn = dpTaiKham.getCalendarButton();
        btn.setText("");
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(1, 32));
        JLabel icon = new JLabel(new LineIcon(LineIcon.Type.CALENDAR, MUTED, 16));
        icon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        icon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                btn.doClick();
            }
        });
        JPanel oNgay = new JPanel(new BorderLayout(8, 0));
        oNgay.setBackground(Color.WHITE);
        oNgay.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(4, 9, 4, 9)));
        oNgay.add(dpTaiKham, BorderLayout.CENTER);
        oNgay.add(icon, BorderLayout.EAST);
        return oNgay;
    }

    private String ngayTaiKham() {
        Date d = dpTaiKham.getDate();
        return d == null ? "" : d.toInstant().atZone(ZONE).toLocalDate().format(NGAY);
    }

    private String gioTaiKham() {
        Object gio = cboGioTaiKham.getSelectedItem();
        return gio == null || "—".equals(gio) ? "" : " " + gio;
    }

    /**
     * Combo gợi ý các trường hợp phổ biến; chọn "Khác…" để tự nhập nội dung
     * khác.
     */
    /** Ô nhập có nhãn; không dùng danh mục gợi ý, cho phép nhập tự do. */
    private final class TruongChon extends JPanel {

        private final JComponent input;

        TruongChon(String nhan, JComponent input, Runnable ignored) {
            super(new BorderLayout(0, 4));
            setOpaque(false);
            setBorder(new EmptyBorder(0, 0, 8, 0));
            this.input = input;
            JLabel l = new JLabel(nhan);
            l.setFont(font(10));
            l.setForeground(MUTED);
            if (input instanceof JTextField tf) {
                tf.setFont(font(11));
                tf.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER), new EmptyBorder(6, 8, 6, 8)));
            }
            add(l, BorderLayout.NORTH);
            add(input instanceof JTextField ? input : new JScrollPane(input), BorderLayout.CENTER);
        }

        String giaTri() {
            return input instanceof JTextArea a ? a.getText().trim()
                    : ((JTextField) input).getText().trim();
        }

        void reset() {
            if (input instanceof JTextArea a) {
                a.setText("");
            } else {
                ((JTextField) input).setText("");
            }
        }
    }

    /**
     * Vòng tròn số thứ tự của bước trong thanh tiến trình.
     */
    private static final class StepDot extends JLabel {

        static final int IDLE = 0;
        static final int ACTIVE = 1;
        static final int DONE = 2;

        private final int so;
        private int trangThai = IDLE;

        StepDot(int so) {
            super(String.valueOf(so), SwingConstants.CENTER);
            this.so = so;
            setPreferredSize(new Dimension(26, 26));
            setFont(bold(11));
        }

        void set(int trangThai) {
            this.trangThai = trangThai;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int d = Math.min(getWidth(), getHeight());
            int x = (getWidth() - d) / 2;
            int y = (getHeight() - d) / 2;
            if (trangThai == IDLE) {
                g2.setColor(BORDER);
                g2.drawOval(x, y, d - 1, d - 1);
                g2.setColor(MUTED);
            } else {
                g2.setColor(TEAL);
                g2.fillOval(x, y, d, d);
                g2.setColor(Color.WHITE);
            }
            setForeground(trangThai == IDLE ? MUTED : Color.WHITE);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Nền tròn cho avatar viết tắt tên bệnh nhân.
     */
    private static final class AvatarLabel extends JPanel {

        AvatarLabel(JLabel noiDung) {
            super(new BorderLayout());
            setOpaque(false);
            setPreferredSize(new Dimension(48, 48));
            add(noiDung, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(TEAL_LIGHT);
            int d = Math.min(getWidth(), getHeight());
            g2.fillOval((getWidth() - d) / 2, (getHeight() - d) / 2, d, d);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class PillLabel extends JLabel {

        private Color bg = new Color(0xF0F2F5);

        PillLabel() {
            super("", SwingConstants.CENTER);
            setOpaque(false);
            setBorder(new EmptyBorder(4, 10, 4, 10));
        }

        void set(String text, Color fg, Color bg) {
            setText(text);
            setForeground(fg);
            this.bg = bg;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
