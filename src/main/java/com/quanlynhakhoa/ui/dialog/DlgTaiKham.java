package com.quanlynhakhoa.ui.dialog;

import static com.quanlynhakhoa.ui.component.UiConstants.BACKGROUND;
import static com.quanlynhakhoa.ui.component.UiConstants.BORDER;
import static com.quanlynhakhoa.ui.component.UiConstants.MUTED;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL;
import static com.quanlynhakhoa.ui.component.UiConstants.TEXT;
import static com.quanlynhakhoa.ui.component.UiConstants.bold;
import static com.quanlynhakhoa.ui.component.UiConstants.font;

import com.quanlynhakhoa.dao.LichHenDAO;
import com.quanlynhakhoa.entity.LichHen;
import com.quanlynhakhoa.entity.PhieuKham;
import com.quanlynhakhoa.exception.BusinessException;
import com.toedter.calendar.JDateChooser;
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
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
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
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

/**
 * UC017 – đặt lịch tái khám từ một phiếu khám đã hoàn tất. Bệnh nhân, nha sĩ và dịch vụ lấy từ phiếu
 * nguồn nên không cho đổi; lễ tân chỉ chọn ngày trong khoảng cho phép và một khung giờ trống.
 */
public class DlgTaiKham extends JDialog {

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");
    private static final Color DO = new Color(0xC0392B);

    private final PhieuKham phieu;
    private final LichHenDAO lichHenDAO = new LichHenDAO();

    private final JDateChooser dpNgay = new JDateChooser();
    private final JTextField txtGhiChu = new JTextField();
    private final JButton btnTimKhung = new JButton("Tìm khung giờ trống");
    private final DefaultListModel<LocalDateTime> modelKhung = new DefaultListModel<>();
    private final JList<LocalDateTime> lstKhung = new JList<>(modelKhung);
    private final JLabel lblKhoang = new JLabel(" ");
    private final JLabel lblKhung = new JLabel(" ");
    private final JButton btnXacNhan = new JButton("Xác nhận");
    private final JButton btnDong = new JButton("Đóng");

    private LichHen ketQua;

    private DlgTaiKham(Window owner, PhieuKham phieu) {
        super(owner, "Đặt lịch tái khám", ModalityType.APPLICATION_MODAL);
        this.phieu = phieu;
        khoiTao();
        addEvents();
        napKhoangNgay();
        pack();
        setMinimumSize(new Dimension(560, getHeight()));
        setLocationRelativeTo(owner);
    }

    /** @return lịch tái khám vừa đặt, hoặc {@code null} nếu lễ tân đóng hộp thoại. */
    public static LichHen datTaiKham(Window owner, PhieuKham phieu) {
        Objects.requireNonNull(phieu);
        DlgTaiKham dlg = new DlgTaiKham(owner, phieu);
        dlg.setVisible(true);
        return dlg.ketQua;
    }

    // ---------------------------------------------------------------- giao diện

    private void khoiTao() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(BACKGROUND);
        root.setBorder(new EmptyBorder(20, 24, 16, 24));

        JLabel tieuDe = new JLabel(getTitle());
        tieuDe.setFont(bold(18));
        tieuDe.setForeground(TEXT);
        root.add(tieuDe, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        int dong = 0;
        LichHen goc = phieu.getLichHen();
        themDong(form, dong++, "Phiếu khám nguồn", chu(phieu.getMaPK() + " (lập "
                + phieu.getNgayLap().format(NGAY) + ")"));
        themDong(form, dong++, "Bệnh nhân", chu(goc.getKhachHang().getMaKH() + " - "
                + goc.getKhachHang().getTenKH()));
        themDong(form, dong++, "Dịch vụ", chu(goc.getDichVu().getTenDV()
                + " (" + goc.getDichVu().getThoiLuong() + " phút)"));
        themDong(form, dong++, "Nha sĩ (giữ nguyên)", chu(goc.getNhaSi().getHoTenNV()));

        JPanel pnlNgay = new JPanel(new BorderLayout(8, 0));
        pnlNgay.setOpaque(false);
        dpNgay.setDateFormatString("dd/MM/yyyy");
        dpNgay.setFont(font(13));
        dpNgay.setPreferredSize(new Dimension(190, 34));
        pnlNgay.add(dpNgay, BorderLayout.CENTER);
        pnlNgay.add(btnTimKhung, BorderLayout.EAST);
        JPanel cotNgay = new JPanel(new BorderLayout(0, 4));
        cotNgay.setOpaque(false);
        cotNgay.add(pnlNgay, BorderLayout.CENTER);
        lblKhoang.setFont(font(12));
        lblKhoang.setForeground(MUTED);
        cotNgay.add(lblKhoang, BorderLayout.SOUTH);
        themDong(form, dong++, "Ngày tái khám *", cotNgay);

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
        themDong(form, dong++, "Ghi chú", txtGhiChu);
        root.add(form, BorderLayout.CENTER);

        btnXacNhan.setBackground(TEAL);
        btnXacNhan.setForeground(Color.WHITE);
        btnXacNhan.setFont(bold(13));
        btnXacNhan.setPreferredSize(new Dimension(110, 36));
        btnDong.setPreferredSize(new Dimension(86, 36));
        btnTimKhung.setFont(bold(12));
        JPanel pnlNut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlNut.setOpaque(false);
        pnlNut.add(btnDong);
        pnlNut.add(btnXacNhan);
        root.add(pnlNut, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(btnXacNhan);
    }

    private static JLabel chu(String noiDung) {
        JLabel lbl = new JLabel(noiDung);
        lbl.setFont(bold(13));
        lbl.setForeground(TEXT);
        return lbl;
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

    private void addEvents() {
        btnTimKhung.addActionListener(e -> timKhung());
        btnXacNhan.addActionListener(e -> xacNhan());
        btnDong.addActionListener(e -> dispose());
        dpNgay.addPropertyChangeListener("date", e -> xoaKhung());
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // ---------------------------------------------------------------- khoảng ngày cho phép

    private void napKhoangNgay() {
        datDangXuLy(true);
        btnXacNhan.setEnabled(false);
        new SwingWorker<LichHenDAO.KhoangNgay, Void>() {
            @Override
            protected LichHenDAO.KhoangNgay doInBackground() {
                return lichHenDAO.khoangNgayTaiKham(phieu.getMaPK());
            }

            @Override
            protected void done() {
                try {
                    LichHenDAO.KhoangNgay k = get();
                    LocalDate tu = k.tu().isBefore(LocalDate.now()) ? LocalDate.now() : k.tu();
                    if (tu.isAfter(k.den())) {
                        lblKhoang.setForeground(DO);
                        lblKhoang.setText("Đã quá hạn tái khám (" + k.tu().format(NGAY) + " – "
                                + k.den().format(NGAY) + ").");
                        btnTimKhung.setEnabled(false);
                        dpNgay.setEnabled(false);
                        setCursor(Cursor.getDefaultCursor());
                        return;
                    }
                    dpNgay.setMinSelectableDate(batDauNgay(tu));
                    dpNgay.setMaxSelectableDate(batDauNgay(k.den().plusDays(1)));
                    dpNgay.setDate(batDauNgay(tu));
                    lblKhoang.setText("Chỉ được hẹn từ " + k.tu().format(NGAY) + " đến " + k.den().format(NGAY) + ".");
                    datDangXuLy(false);
                    btnXacNhan.setEnabled(true);
                } catch (Exception e) {
                    HopThoaiLoi.hien(DlgTaiKham.this, e, "Không thể tải thông tin phiếu khám.");
                    dispose();
                }
            }
        }.execute();
    }

    private static Date batDauNgay(LocalDate ngay) {
        return Date.from(ngay.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    // ---------------------------------------------------------------- khung giờ trống

    private void timKhung() {
        if (dpNgay.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn ngày tái khám.", "Dữ liệu chưa hợp lệ",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        LocalDate ngay = dpNgay.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        datDangXuLy(true);
        xoaKhung();
        new SwingWorker<List<LocalDateTime>, Void>() {
            @Override
            protected List<LocalDateTime> doInBackground() {
                return lichHenDAO.goiYKhungTrongTaiKham(phieu.getMaPK(), ngay);
            }

            @Override
            protected void done() {
                try {
                    List<LocalDateTime> khung = get();
                    khung.forEach(modelKhung::addElement);
                    if (khung.isEmpty()) {
                        lblKhung.setForeground(DO);
                        lblKhung.setText("Không có khung giờ trống. Hãy chọn ngày khác.");
                    } else {
                        lblKhung.setForeground(MUTED);
                        lblKhung.setText(khung.size() + " khung giờ trống ngày " + ngay.format(NGAY));
                        lstKhung.setSelectedIndex(0);
                    }
                } catch (Exception e) {
                    HopThoaiLoi.hien(DlgTaiKham.this, e, "Không thể tìm khung giờ trống.");
                } finally {
                    datDangXuLy(false);
                }
            }
        }.execute();
    }

    private void xoaKhung() {
        modelKhung.clear();
        lblKhung.setForeground(MUTED);
        lblKhung.setText(" ");
    }

    // ---------------------------------------------------------------- xác nhận

    private void xacNhan() {
        LocalDateTime gio = lstKhung.getSelectedValue();
        if (gio == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng tìm và chọn một khung giờ trống.",
                    "Dữ liệu chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String ghiChu = txtGhiChu.getText();
        datDangXuLy(true);
        new SwingWorker<LichHen, Void>() {
            @Override
            protected LichHen doInBackground() {
                return lichHenDAO.taoTaiKham(phieu.getMaPK(), gio, ghiChu);
            }

            @Override
            protected void done() {
                try {
                    LichHen lh = get();
                    ketQua = lh;
                    JOptionPane.showMessageDialog(DlgTaiKham.this,
                            "Đặt lịch tái khám thành công.\nMã lịch: " + lh.getMaLH() + " – "
                            + gio.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")),
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (Exception e) {
                    datDangXuLy(false);
                    Throwable goc = HopThoaiLoi.nguyenNhan(e);
                    if (goc instanceof BusinessException be && be.getMessage() != null
                            && be.getMessage().startsWith("Trùng lịch")) {
                        JOptionPane.showMessageDialog(DlgTaiKham.this, "Khung giờ không còn trống.", "Cảnh báo",
                                JOptionPane.WARNING_MESSAGE);
                        timKhung();
                    } else {
                        HopThoaiLoi.hien(DlgTaiKham.this, e, "Không thể đặt lịch tái khám, vui lòng thử lại.");
                    }
                }
            }
        }.execute();
    }

    private void datDangXuLy(boolean dang) {
        btnXacNhan.setEnabled(!dang);
        btnTimKhung.setEnabled(!dang);
        setCursor(dang ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }
}
