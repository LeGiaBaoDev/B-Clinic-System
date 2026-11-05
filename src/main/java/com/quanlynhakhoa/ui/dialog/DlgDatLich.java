package com.quanlynhakhoa.ui.dialog;

import static com.quanlynhakhoa.ui.component.UiConstants.BACKGROUND;
import static com.quanlynhakhoa.ui.component.UiConstants.BORDER;
import static com.quanlynhakhoa.ui.component.UiConstants.MUTED;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL;
import static com.quanlynhakhoa.ui.component.UiConstants.TEXT;
import static com.quanlynhakhoa.ui.component.UiConstants.bold;
import static com.quanlynhakhoa.ui.component.UiConstants.font;

import com.quanlynhakhoa.dao.DichVuDAO;
import com.quanlynhakhoa.dao.KhachHangDAO;
import com.quanlynhakhoa.dao.LichHenDAO;
import com.quanlynhakhoa.dao.NhanVienDAO;
import com.quanlynhakhoa.entity.DichVu;
import com.quanlynhakhoa.entity.KhachHang;
import com.quanlynhakhoa.entity.LichHen;
import com.quanlynhakhoa.entity.NhanVien;
import com.quanlynhakhoa.entity.enums.TrangThaiLichHen;
import com.quanlynhakhoa.exception.BusinessException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Date;
import com.toedter.calendar.JDateChooser;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class DlgDatLich extends JDialog {

    private enum CheDo { DAT_MOI, DOI_LICH }

    private static final DateTimeFormatter NGAY =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");
    private static final Color DO = new Color(0xC0392B);
    private static final int SO_BENH_NHAN_TOI_DA = 200;

    private final CheDo cheDo;
    private final LichHen lichCu;
    private final KhachHang khachChonSan;
    private final LocalDate ngayMacDinh;
    private final LichHenDAO lichHenDAO = new LichHenDAO();
    private final KhachHangDAO khachHangDAO = new KhachHangDAO();

    private final JTextField txtTimBenhNhan = new JTextField();
    private final JButton btnTimBenhNhan = new JButton("Tìm");
    private final JButton btnThemKhach = new JButton("+ Thêm khách hàng mới");
    private final DefaultComboBoxModel<KhachHang> modelBenhNhan = new DefaultComboBoxModel<>();
    private final JComboBox<KhachHang> cboBenhNhan = new JComboBox<>(modelBenhNhan);
    private final JLabel lblThongBaoBenhNhan = new JLabel(" ");
    private final JComboBox<DichVu> cboDichVu = new JComboBox<>();
    private final JComboBox<NhanVien> cboNhaSi = new JComboBox<>();
    private final JDateChooser dpNgay = new JDateChooser();
    private final JTextField txtGhiChu = new JTextField();
    private final JButton btnTimKhung = new JButton("Tìm khung giờ trống");
    private final JButton btnLamMoi = new JButton("Làm mới");
    private final DefaultListModel<LocalDateTime> modelKhung = new DefaultListModel<>();
    private final JList<LocalDateTime> lstKhung = new JList<>(modelKhung);
    private final JLabel lblKhung = new JLabel(" ");
    private final JLabel lblCanhBao = new JLabel(" ");
    private final JButton btnXacNhan = new JButton("Xác nhận");
    private final JButton btnHuy = new JButton("Đóng");

    private final Map<LocalDateTime, List<NhanVien>> nhaSiTheoKhung = new HashMap<>();
    private final List<NhanVien> tatCaNhaSi = new ArrayList<>();
    private final Timer henGioTim = new Timer(300, e -> timBenhNhan());
    private SwingWorker<List<KhachHang>, Void> workerBenhNhan;
    private LichHen ketQua;

    private DlgDatLich(Window owner, CheDo cheDo, LichHen lichCu, KhachHang khachChonSan,
            LocalDate ngayMacDinh) {
        super(owner, cheDo == CheDo.DAT_MOI ? "Đặt lịch hẹn" : "Dời lịch hẹn", ModalityType.APPLICATION_MODAL);
        this.cheDo = cheDo;
        this.lichCu = lichCu;
        this.khachChonSan = khachChonSan;
        this.ngayMacDinh = ngayMacDinh;
        khoiTao();
        addEvents();
        napBanDau();
        pack();
        setMinimumSize(new Dimension(640, getHeight()));
        setLocationRelativeTo(owner);
    }

    
    public static LichHen datMoi(Window owner) {
        return datMoi(owner, null);
    }

    
    public static LichHen datMoi(Window owner, KhachHang khachChonSan) {
        DlgDatLich dlg = new DlgDatLich(owner, CheDo.DAT_MOI, null, khachChonSan, null);
        dlg.setVisible(true);
        return dlg.ketQua;
    }

    /** Tiếp nhận khách đến trực tiếp: mở form đặt lịch mặc định hôm nay. */
    public static LichHen datMoiHomNay(Window owner) {
        DlgDatLich dlg = new DlgDatLich(owner, CheDo.DAT_MOI, null, null, LocalDate.now());
        dlg.setVisible(true);
        return dlg.ketQua;
    }

    
    public static LichHen doiLich(Window owner, LichHen lich) {
        DlgDatLich dlg = new DlgDatLich(owner, CheDo.DOI_LICH, Objects.requireNonNull(lich), null, null);
        dlg.setVisible(true);
        return dlg.ketQua;
    }

    // ---------------------------------------------------------------- giao diện

    private void khoiTao() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(BACKGROUND);
        root.setBorder(new EmptyBorder(20, 24, 16, 24));

        JLabel lblTieuDe = new JLabel(getTitle());
        lblTieuDe.setFont(bold(18));
        lblTieuDe.setForeground(TEXT);
        root.add(lblTieuDe, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        int dong = 0;

        JPanel pnlTim = new JPanel(new BorderLayout(8, 0));
        pnlTim.setOpaque(false);
        txtTimBenhNhan.putClientProperty("JTextField.placeholderText",
                "Gõ mã, họ tên hoặc số điện thoại để lọc danh sách");
        pnlTim.add(txtTimBenhNhan, BorderLayout.CENTER);
        pnlTim.add(btnTimBenhNhan, BorderLayout.EAST);
        JPanel pnlBenhNhan = new JPanel(new BorderLayout(0, 6));
        pnlBenhNhan.setOpaque(false);
        pnlBenhNhan.add(pnlTim, BorderLayout.NORTH);
        pnlBenhNhan.add(cboBenhNhan, BorderLayout.CENTER);
        JPanel pnlDuoi = new JPanel(new BorderLayout());
        pnlDuoi.setOpaque(false);
        lblThongBaoBenhNhan.setFont(font(12));
        lblThongBaoBenhNhan.setForeground(DO);
        pnlDuoi.add(lblThongBaoBenhNhan, BorderLayout.CENTER);
        pnlDuoi.add(btnThemKhach, BorderLayout.EAST);
        pnlBenhNhan.add(pnlDuoi, BorderLayout.SOUTH);
        themDong(form, dong++, "Bệnh nhân *", pnlBenhNhan);

        themDong(form, dong++, "Triệu chứng", txtGhiChu);

        JPanel pnlNgay = new JPanel(new BorderLayout(8, 0));
        pnlNgay.setOpaque(false);
        dpNgay.setDateFormatString("dd/MM/yyyy");
        LocalDate homNay = LocalDate.now();
        dpNgay.setMinSelectableDate(Date.from(homNay.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        dpNgay.setMaxSelectableDate(Date.from(homNay.plusMonths(3).atTime(23, 59, 59)
                .atZone(ZoneId.systemDefault()).toInstant()));
        dpNgay.setFont(font(13));
        dpNgay.setPreferredSize(new Dimension(190, 34));
        pnlNgay.add(dpNgay, BorderLayout.CENTER);
        JPanel pnlNutNgay = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pnlNutNgay.setOpaque(false);
        pnlNutNgay.add(btnLamMoi);
        pnlNutNgay.add(btnTimKhung);
        pnlNgay.add(pnlNutNgay, BorderLayout.EAST);
        themDong(form, dong++, "Ngày hẹn * (dd/MM/yyyy)", pnlNgay);

        lstKhung.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstKhung.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        lstKhung.setVisibleRowCount(4);
        lstKhung.setFixedCellWidth(72);
        lstKhung.setFixedCellHeight(30);
        lstKhung.setFont(font(13));
        lstKhung.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list,
                        value instanceof LocalDateTime t ? t.format(GIO) : value, index, isSelected, cellHasFocus);
                lbl.setHorizontalAlignment(CENTER);
                return lbl;
            }
        });
        JScrollPane scr = new JScrollPane(lstKhung);
        scr.setPreferredSize(new Dimension(380, 130));
        scr.setBorder(BorderFactory.createLineBorder(BORDER));
        JPanel pnlKhung = new JPanel(new BorderLayout(0, 4));
        pnlKhung.setOpaque(false);
        pnlKhung.add(scr, BorderLayout.CENTER);
        lblKhung.setFont(font(12));
        lblKhung.setForeground(MUTED);
        pnlKhung.add(lblKhung, BorderLayout.SOUTH);
        themDong(form, dong++, "Khung giờ trống *", pnlKhung);
        themDong(form, dong++, "Nha sĩ phù hợp *", cboNhaSi);

        root.add(form, BorderLayout.CENTER);

        lblCanhBao.setFont(bold(12));
        lblCanhBao.setForeground(new Color(0xB26B00));
        JPanel pnlNut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlNut.setOpaque(false);
        btnXacNhan.setBackground(TEAL);
        btnXacNhan.setForeground(Color.WHITE);
        btnXacNhan.setFont(bold(13));
        btnXacNhan.setPreferredSize(new Dimension(110, 36));
        btnHuy.setPreferredSize(new Dimension(86, 36));
        btnTimKhung.setFont(bold(12));
        btnLamMoi.setFont(font(12));
        pnlNut.add(btnHuy);
        pnlNut.add(btnXacNhan);
        JPanel pnlSouth = new JPanel(new BorderLayout());
        pnlSouth.setOpaque(false);
        pnlSouth.add(lblCanhBao, BorderLayout.WEST);
        pnlSouth.add(pnlNut, BorderLayout.EAST);
        root.add(pnlSouth, BorderLayout.SOUTH);

        cboBenhNhan.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                String chu = value instanceof KhachHang k
                        ? k.getMaKH() + " - " + k.getTenKH() + " - " + k.getSdt() : "— Chọn bệnh nhân —";
                return super.getListCellRendererComponent(list, chu, index, isSelected, cellHasFocus);
            }
        });
        cboDichVu.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                String chu = value instanceof DichVu d ? d.getTenDV() + " (" + d.getThoiLuong() + " phút)" : "";
                return super.getListCellRendererComponent(list, chu, index, isSelected, cellHasFocus);
            }
        });
        cboNhaSi.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                String chu = value instanceof NhanVien n ? n.getHoTenNV() : "";
                return super.getListCellRendererComponent(list, chu, index, isSelected, cellHasFocus);
            }
        });
        setContentPane(root);
        getRootPane().setDefaultButton(btnXacNhan);
    }

    private void themDong(JPanel form, int dong, String nhan, Component truong) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = dong;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(6, 0, 6, 12);
        JLabel lbl = new JLabel(nhan);
        lbl.setFont(font(12));
        lbl.setForeground(MUTED);
        c.gridx = 0;
        form.add(lbl, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 6, 0);
        if (truong instanceof JTextField tf) {
            tf.setFont(font(13));
            tf.setPreferredSize(new Dimension(380, 32));
        }
        form.add(truong, c);
    }

    private void napBanDau() {
        if (cheDo == CheDo.DOI_LICH) {
            modelBenhNhan.addElement(lichCu.getKhachHang());
            cboBenhNhan.setEnabled(false);
            txtTimBenhNhan.setEnabled(false);
            btnTimBenhNhan.setEnabled(false);
            btnThemKhach.setVisible(false);
            dpNgay.setDate(Date.from(lichCu.getNgayGioHen().toLocalDate()
                    .atStartOfDay(ZoneId.systemDefault()).toInstant()));
            lblThongBaoBenhNhan.setForeground(MUTED);
            lblThongBaoBenhNhan.setText("Dời lịch " + lichCu.getMaLH() + " (hiện tại "
                    + lichCu.getNgayGioHen().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")) + ")");
        } else {
            LocalDate macDinh = ngayMacDinh == null ? LocalDate.now().plusDays(1) : ngayMacDinh;
            dpNgay.setDate(Date.from(macDinh.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            if (khachChonSan != null) {
                // lọc theo mã để chỉ còn đúng bệnh nhân này và được chọn sẵn
                txtTimBenhNhan.setText(khachChonSan.getMaKH());
            }
            timBenhNhan();
        }
        taiNhaSiVaDichVu();
    }

    private void taiNhaSiVaDichVu() {
        datDangXuLy(true);
        xoaKhung();
        cboDichVu.removeAllItems();
        cboNhaSi.removeAllItems();
        tatCaNhaSi.clear();
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() {
                return new Object[] {new DichVuDAO().timKiem("", true), new NhanVienDAO().layNhaSiDangHoatDong()};
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] kq = get();
                    for (DichVu d : (List<DichVu>) kq[0]) {
                        cboDichVu.addItem(d);
                    }
                    for (NhanVien n : (List<NhanVien>) kq[1]) {
                        tatCaNhaSi.add(n);
                        cboNhaSi.addItem(n);
                    }
                    if (cheDo == CheDo.DOI_LICH) {
                        chonDichVu(lichCu.getDichVu());
                        chonNhaSi(lichCu.getNhaSi());
                    } else {
                        chonDichVuMacDinh();
                    }
                    if (cboNhaSi.getItemCount() == 0) {
                        lblKhung.setForeground(DO);
                        lblKhung.setText("Chưa có nha sĩ đang hoạt động.");
                    }
                } catch (Exception e) {
                    HopThoaiLoi.hien(DlgDatLich.this, e, "Không thể tải danh sách dịch vụ và nha sĩ.");
                } finally {
                    datDangXuLy(false);
                }
            }
        }.execute();
    }

    /** Lịch hẹn không còn chọn dịch vụ; dùng dịch vụ khám mặc định để tính khung giờ. */
    private void chonDichVuMacDinh() {
        if (cboDichVu.getItemCount() == 0) {
            return;
        }
        for (int i = 0; i < cboDichVu.getItemCount(); i++) {
            if ("DV001".equals(cboDichVu.getItemAt(i).getMaDV())) {
                cboDichVu.setSelectedIndex(i);
                return;
            }
        }
        for (int i = 0; i < cboDichVu.getItemCount(); i++) {
            if (cboDichVu.getItemAt(i).getTenDV().toLowerCase().contains("khám")) {
                cboDichVu.setSelectedIndex(i);
                return;
            }
        }
        cboDichVu.setSelectedIndex(0);
    }

    private void chonDichVu(DichVu dv) {
        for (int i = 0; i < cboDichVu.getItemCount(); i++) {
            if (cboDichVu.getItemAt(i).getMaDV().equals(dv.getMaDV())) {
                cboDichVu.setSelectedIndex(i);
                return;
            }
        }
        cboDichVu.addItem(dv);
        cboDichVu.setSelectedItem(dv);
    }

    private void chonNhaSi(NhanVien nv) {
        for (int i = 0; i < cboNhaSi.getItemCount(); i++) {
            if (cboNhaSi.getItemAt(i).getMaNV().equals(nv.getMaNV())) {
                cboNhaSi.setSelectedIndex(i);
                return;
            }
        }
    }

    private void addEvents() {
        btnLamMoi.addActionListener(e -> taiNhaSiVaDichVu());
        henGioTim.setRepeats(false);
        txtTimBenhNhan.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                henGioTim.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                henGioTim.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                henGioTim.restart();
            }
        });
        cboBenhNhan.addActionListener(e -> xoaKhung());
        btnTimBenhNhan.addActionListener(e -> timBenhNhan());
        txtTimBenhNhan.addActionListener(e -> timBenhNhan());
        btnThemKhach.addActionListener(e -> themKhachMoi());
        btnTimKhung.addActionListener(e -> timKhung());
        lstKhung.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                capNhatNhaSiTheoKhung();
            }
        });
        btnXacNhan.addActionListener(e -> xacNhan());
        btnHuy.addActionListener(e -> dispose());
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // ---------------------------------------------------------------- bước 1: bệnh nhân

    /**
     * Tải danh sách bệnh nhân vào ô chọn: từ khóa rỗng thì hiện tất cả (tối đa 200), có từ khóa thì lọc.
     * Chạy ngay khi mở form và tự chạy lại khi gõ, nên không cần bấm "Tìm".
     */
    private void timBenhNhan() {
        if (cheDo != CheDo.DAT_MOI) {
            return;
        }
        String tuKhoa = txtTimBenhNhan.getText();
        if (workerBenhNhan != null) {
            workerBenhNhan.cancel(true);
        }
        workerBenhNhan = new SwingWorker<>() {
            @Override
            protected List<KhachHang> doInBackground() {
                return khachHangDAO.timKiem(tuKhoa);
            }

            @Override
            protected void done() {
                try {
                    List<KhachHang> ds = get();
                    modelBenhNhan.removeAllElements();
                    ds.stream().limit(SO_BENH_NHAN_TOI_DA).forEach(modelBenhNhan::addElement);
                    modelBenhNhan.setSelectedItem(ds.size() == 1 ? ds.get(0) : null);
                    lblThongBaoBenhNhan.setForeground(ds.isEmpty() ? DO : MUTED);
                    if (ds.isEmpty()) {
                        lblThongBaoBenhNhan.setText(tuKhoa.isBlank()
                                ? "Chưa có bệnh nhân. Hãy thêm khách hàng mới."
                                : "Không tìm thấy bệnh nhân. Hãy tìm lại hoặc thêm khách hàng mới.");
                    } else if (ds.size() > SO_BENH_NHAN_TOI_DA) {
                        lblThongBaoBenhNhan.setText("Có " + ds.size() + " bệnh nhân, hiện "
                                + SO_BENH_NHAN_TOI_DA + " người đầu. Gõ thêm để lọc.");
                    } else {
                        lblThongBaoBenhNhan.setText(ds.size() + " bệnh nhân");
                    }
                    xoaKhung();
                } catch (java.util.concurrent.CancellationException ignored) {
                    // đã có lần tìm mới hơn
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    HopThoaiLoi.hien(DlgDatLich.this, e, "Không thể tải danh sách bệnh nhân.");
                }
            }
        };
        workerBenhNhan.execute();
    }

    private void themKhachMoi() {
        KhachHang kh = DlgKhachHang.themMoi(this);
        if (kh != null) {
            modelBenhNhan.removeAllElements();
            modelBenhNhan.addElement(kh);
            modelBenhNhan.setSelectedItem(kh);
            lblThongBaoBenhNhan.setText(" ");
            xoaKhung();
        }
    }

    // ---------------------------------------------------------------- bước 2: khung giờ trống

    private void timKhung() {
        KhachHang kh = (KhachHang) cboBenhNhan.getSelectedItem();
        DichVu dv = (DichVu) cboDichVu.getSelectedItem();
        if (kh == null) {
            baoThieu("Vui lòng chọn bệnh nhân.", txtTimBenhNhan);
            return;
        }
        if (dv == null) {
            baoThieu("Chưa tải được dịch vụ mặc định. Bấm Làm mới rồi thử lại.", btnLamMoi);
            return;
        }
        if (dpNgay.getDate() == null) {
            baoThieu("Vui lòng chọn ngày hẹn.", dpNgay);
            return;
        }
        LocalDate ngay = dpNgay.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (ngay.isBefore(LocalDate.now())) {
            baoThieu("Ngày hẹn phải từ hôm nay trở đi.", dpNgay);
            return;
        }
        List<NhanVien> dsNhaSi = List.copyOf(tatCaNhaSi);
        if (dsNhaSi.isEmpty()) {
            baoThieu("Chưa có nha sĩ đang hoạt động.", btnLamMoi);
            return;
        }
        String boQua = cheDo == CheDo.DOI_LICH ? lichCu.getMaLH() : null;
        datDangXuLy(true);
        xoaKhung();
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() {
                Map<LocalDateTime, List<NhanVien>> theoKhung = new TreeMap<>();
                for (NhanVien ns : dsNhaSi) {
                    for (LocalDateTime gio : lichHenDAO.goiYKhungTrong(kh.getMaKH(), ns.getMaNV(),
                            dv.getMaDV(), ngay, boQua)) {
                        theoKhung.computeIfAbsent(gio, k -> new ArrayList<>()).add(ns);
                    }
                }
                int huy = lichHenDAO.demSoLanHuyTrongThang(kh.getMaKH(), ngay);
                return new Object[] {theoKhung, huy};
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] kq = get();
                    Map<LocalDateTime, List<NhanVien>> theoKhung =
                            (Map<LocalDateTime, List<NhanVien>>) kq[0];
                    int huy = (Integer) kq[1];
                    nhaSiTheoKhung.clear();
                    nhaSiTheoKhung.putAll(theoKhung);
                    modelKhung.clear();
                    theoKhung.keySet().forEach(modelKhung::addElement);
                    if (theoKhung.isEmpty()) {
                        lblKhung.setForeground(DO);
                        lblKhung.setText("Không có khung giờ trống. Hãy chọn ngày khác.");
                    } else {
                        lblKhung.setForeground(MUTED);
                        lblKhung.setText(theoKhung.size() + " khung giờ có nha sĩ trống ngày "
                                + ngay.format(NGAY));
                        lstKhung.setSelectedIndex(0);
                        capNhatNhaSiTheoKhung();
                    }
                    lblCanhBao.setText(huy >= 3
                            ? "Bệnh nhân đã hủy " + huy + " lần trong tháng: chỉ có khung đầu/cuối ngày."
                            : " ");
                } catch (Exception e) {
                    HopThoaiLoi.hien(DlgDatLich.this, e, "Không thể tìm khung giờ trống.");
                } finally {
                    datDangXuLy(false);
                }
            }
        }.execute();
    }

    /** Sau khi chọn khung giờ, chỉ hiện các nha sĩ còn trống đúng khung đó. */
    private void capNhatNhaSiTheoKhung() {
        LocalDateTime gio = lstKhung.getSelectedValue();
        NhanVien dangChon = (NhanVien) cboNhaSi.getSelectedItem();
        cboNhaSi.removeAllItems();
        if (gio == null) {
            return;
        }
        List<NhanVien> ds = nhaSiTheoKhung.get(gio);
        if (ds == null) {
            return;
        }
        for (NhanVien nv : ds) {
            cboNhaSi.addItem(nv);
        }
        if (dangChon != null) {
            for (int i = 0; i < cboNhaSi.getItemCount(); i++) {
                if (cboNhaSi.getItemAt(i).getMaNV().equals(dangChon.getMaNV())) {
                    cboNhaSi.setSelectedIndex(i);
                    return;
                }
            }
        }
        if (cboNhaSi.getItemCount() > 0) {
            cboNhaSi.setSelectedIndex(0);
        }
    }

    private void xoaKhung() {
        modelKhung.clear();
        nhaSiTheoKhung.clear();
        lblKhung.setForeground(MUTED);
        lblKhung.setText(" ");
        lblCanhBao.setText(" ");
    }

    // ---------------------------------------------------------------- bước 3: xác nhận

    private void xacNhan() {
        LocalDateTime gio = lstKhung.getSelectedValue();
        if (gio == null) {
            baoThieu("Vui lòng tìm và chọn một khung giờ trống.", btnTimKhung);
            return;
        }
        KhachHang kh = (KhachHang) cboBenhNhan.getSelectedItem();
        DichVu dv = (DichVu) cboDichVu.getSelectedItem();
        NhanVien ns = (NhanVien) cboNhaSi.getSelectedItem();
        if (kh == null || ns == null || dv == null) {
            baoThieu("Vui lòng chọn đủ bệnh nhân và nha sĩ.", txtTimBenhNhan);
            return;
        }
        LichHen lh;
        try {
            if (cheDo == CheDo.DAT_MOI) {
                lh = new LichHen(null, gio, TrangThaiLichHen.DA_DAT, txtGhiChu.getText(), null, kh, ns, dv, null);
            } else {
                lh = new LichHen(lichCu);
                lh.setNgayGioHen(gio);
                lh.setNhaSi(ns);
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Dữ liệu chưa hợp lệ",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean datMoi = cheDo == CheDo.DAT_MOI;
        datDangXuLy(true);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return datMoi ? lichHenDAO.them(lh) : lichHenDAO.capNhat(lh);
            }

            @Override
            protected void done() {
                try {
                    if (Boolean.TRUE.equals(get())) {
                        ketQua = lh;
                        JOptionPane.showMessageDialog(DlgDatLich.this,
                                datMoi ? "Đặt lịch hẹn thành công.\nMã lịch: " + lh.getMaLH()
                                        : "Dời lịch hẹn thành công.",
                                "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } else {
                        datDangXuLy(false);
                        JOptionPane.showMessageDialog(DlgDatLich.this, "Không thể lưu lịch hẹn, vui lòng thử lại.",
                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    datDangXuLy(false);
                    Throwable goc = HopThoaiLoi.nguyenNhan(e);
                    if (goc instanceof BusinessException be && be.getMessage() != null
                            && be.getMessage().startsWith("Trùng lịch")) {
                        JOptionPane.showMessageDialog(DlgDatLich.this, "Khung giờ không còn trống.", "Cảnh báo",
                                JOptionPane.WARNING_MESSAGE);
                        timKhung();
                    } else {
                        HopThoaiLoi.hien(DlgDatLich.this, e, "Không thể lưu lịch hẹn, vui lòng thử lại.");
                    }
                }
            }
        }.execute();
    }

    // ---------------------------------------------------------------- dùng chung

    private void baoThieu(String noiDung, JComponent truong) {
        JOptionPane.showMessageDialog(this, noiDung, "Dữ liệu chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
        SwingUtilities.invokeLater(truong::requestFocusInWindow);
    }

    private void datDangXuLy(boolean dang) {
        btnXacNhan.setEnabled(!dang);
        btnTimKhung.setEnabled(!dang);
        btnTimBenhNhan.setEnabled(!dang && cheDo == CheDo.DAT_MOI);
        setCursor(dang ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }
}
