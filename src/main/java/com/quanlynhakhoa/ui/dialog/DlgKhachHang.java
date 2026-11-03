package com.quanlynhakhoa.ui.dialog;

import static com.quanlynhakhoa.ui.component.UiConstants.BACKGROUND;
import static com.quanlynhakhoa.ui.component.UiConstants.BORDER;
import static com.quanlynhakhoa.ui.component.UiConstants.MUTED;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL;
import static com.quanlynhakhoa.ui.component.UiConstants.TEXT;
import static com.quanlynhakhoa.ui.component.UiConstants.bold;
import static com.quanlynhakhoa.ui.component.UiConstants.font;

import com.quanlynhakhoa.dao.KhachHangDAO;
import com.quanlynhakhoa.entity.KhachHang;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

/**
 * UC009 thêm khách hàng mới và UC011 cập nhật hồ sơ khách hàng.
 *
 * <p>Chỉ gọi {@link KhachHangDAO}; không viết SQL. Mọi truy vấn chạy trong {@link SwingWorker}.
 * Ở chế độ cập nhật, họ tên, ngày sinh, giới tính bị khóa; chỉ sửa được SĐT, địa chỉ, email.
 * Tiền sử bệnh chỉ nhập khi thêm mới (hồ sơ bệnh án đã lưu không UPDATE).</p>
 */
public class DlgKhachHang extends JDialog {

    public enum CheDo { THEM_MOI, CAP_NHAT }

    private static final DateTimeFormatter NGAY =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final CheDo cheDo;
    private final KhachHang goc;
    private final KhachHangDAO khachHangDAO = new KhachHangDAO();

    private final JTextField txtMaKH = new JTextField();
    private final JTextField txtHoTen = new JTextField();
    private final JTextField txtNgaySinh = new JTextField();
    private final JRadioButton radNam = new JRadioButton("Nam", true);
    private final JRadioButton radNu = new JRadioButton("Nữ");
    private final JTextField txtSdt = new JTextField();
    private final JTextField txtDiaChi = new JTextField();
    private final JTextField txtEmail = new JTextField();
    private final JTextArea txaTienSu = new JTextArea(4, 20);
    private final JButton btnLuu = new JButton("Lưu");
    private final JButton btnHuy = new JButton("Hủy");

    private KhachHang ketQua;

    private DlgKhachHang(Window owner, CheDo cheDo, KhachHang goc) {
        super(owner, cheDo == CheDo.THEM_MOI ? "Thêm khách hàng mới" : "Cập nhật hồ sơ khách hàng",
                ModalityType.APPLICATION_MODAL);
        this.cheDo = cheDo;
        this.goc = goc;
        khoiTao();
        napDuLieu();
        addEvents();
        pack();
        setMinimumSize(new Dimension(520, getHeight()));
        setLocationRelativeTo(owner);
    }

    /** UC009: mở form thêm khách hàng. */
    public static KhachHang themMoi(Window owner) {
        DlgKhachHang dlg = new DlgKhachHang(owner, CheDo.THEM_MOI, null);
        dlg.setVisible(true);
        return dlg.ketQua;
    }

    /** UC011: mở form cập nhật; trả về khách hàng sau khi lưu, hoặc null nếu không lưu. */
    public static KhachHang capNhat(Window owner, KhachHang khachHang) {
        DlgKhachHang dlg = new DlgKhachHang(owner, CheDo.CAP_NHAT, Objects.requireNonNull(khachHang));
        dlg.setVisible(true);
        return dlg.ketQua;
    }

    /** Khách hàng vừa tạo/cập nhật, hoặc hồ sơ đã có khi lễ tân chọn dùng lại; null nếu hủy. */
    public KhachHang getKetQua() {
        return ketQua;
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
        if (cheDo == CheDo.CAP_NHAT) {
            themDong(form, dong++, "Mã khách hàng", txtMaKH);
        }
        themDong(form, dong++, "Họ tên *", txtHoTen);
        themDong(form, dong++, "Ngày sinh (dd/MM/yyyy)", txtNgaySinh);
        ButtonGroup nhom = new ButtonGroup();
        nhom.add(radNam);
        nhom.add(radNu);
        JPanel pnlGioiTinh = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        pnlGioiTinh.setOpaque(false);
        radNam.setOpaque(false);
        radNu.setOpaque(false);
        pnlGioiTinh.add(radNam);
        pnlGioiTinh.add(radNu);
        themDong(form, dong++, "Giới tính", pnlGioiTinh);
        themDong(form, dong++, "Số điện thoại *", txtSdt);
        themDong(form, dong++, "Địa chỉ", txtDiaChi);
        themDong(form, dong++, "Email", txtEmail);
        if (cheDo == CheDo.THEM_MOI) {
            txaTienSu.setLineWrap(true);
            txaTienSu.setWrapStyleWord(true);
            JScrollPane scr = new JScrollPane(txaTienSu);
            scr.setBorder(BorderFactory.createLineBorder(BORDER));
            themDong(form, dong++, "Tiền sử bệnh / dị ứng", scr);
        }
        root.add(form, BorderLayout.CENTER);

        JPanel pnlNut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlNut.setOpaque(false);
        btnLuu.setFont(bold(13));
        btnLuu.setBackground(TEAL);
        btnLuu.setForeground(Color.WHITE);
        btnLuu.setPreferredSize(new Dimension(96, 36));
        btnHuy.setFont(font(13));
        btnHuy.setPreferredSize(new Dimension(86, 36));
        pnlNut.add(btnHuy);
        pnlNut.add(btnLuu);
        root.add(pnlNut, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(btnLuu);
    }

    private void themDong(JPanel form, int dong, String nhan, Component truong) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = dong;
        c.insets = new Insets(5, 0, 5, 12);
        c.anchor = GridBagConstraints.NORTHWEST;
        JLabel lbl = new JLabel(nhan);
        lbl.setFont(font(12));
        lbl.setForeground(MUTED);
        c.gridx = 0;
        form.add(lbl, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 0, 5, 0);
        if (truong instanceof JTextField tf) {
            tf.setFont(font(13));
            tf.setPreferredSize(new Dimension(280, 32));
        }
        form.add(truong, c);
    }

    private void napDuLieu() {
        if (cheDo != CheDo.CAP_NHAT) {
            return;
        }
        txtMaKH.setText(goc.getMaKH());
        txtHoTen.setText(goc.getTenKH());
        txtNgaySinh.setText(goc.getNgaySinh() == null ? "" : goc.getNgaySinh().format(NGAY));
        radNam.setSelected(goc.isGioiTinh());
        radNu.setSelected(!goc.isGioiTinh());
        txtSdt.setText(goc.getSdt());
        txtDiaChi.setText(goc.getDiaChi() == null ? "" : goc.getDiaChi());
        txtEmail.setText(goc.getEmail() == null ? "" : goc.getEmail());
        for (JComponent khoa : new JComponent[] {txtMaKH, txtHoTen, txtNgaySinh, radNam, radNu}) {
            khoa.setEnabled(false);
        }
    }

    private void addEvents() {
        btnHuy.addActionListener(e -> dispose());
        btnLuu.addActionListener(e -> {
            if (cheDo == CheDo.THEM_MOI) {
                luuThemMoi();
            } else {
                luuCapNhat();
            }
        });
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // ---------------------------------------------------------------- UC009

    private void luuThemMoi() {
        KhachHang kh;
        try {
            kh = docThemMoi();
        } catch (LoiNhap e) {
            baoLoiNhap(e);
            return;
        }
        String tienSu = txaTienSu.getText();
        datDangXuLy(true);
        new SwingWorker<List<KhachHang>, Void>() {
            @Override
            protected List<KhachHang> doInBackground() {
                return khachHangDAO.timTrung(kh.getTenKH(), kh.getNgaySinh(), kh.getSdt());
            }

            @Override
            protected void done() {
                try {
                    xuLyGoiYTrung(kh, tienSu, get());
                } catch (Exception e) {
                    xuLyLoi(e);
                }
            }
        }.execute();
    }

    private void xuLyGoiYTrung(KhachHang kh, String tienSu, List<KhachHang> trung) {
        KhachHang cungSdt = trung.stream().filter(k -> k.getSdt().equals(kh.getSdt())).findFirst().orElse(null);
        if (cungSdt != null) {
            Object[] lua = {"Dùng hồ sơ đã có", "Sửa lại số điện thoại"};
            int chon = JOptionPane.showOptionDialog(this,
                    "Số điện thoại đã được đăng ký.\nHồ sơ trùng: " + cungSdt.getMaKH() + " - "
                            + cungSdt.getTenKH() + (cungSdt.getNgaySinh() == null ? ""
                            : " (" + cungSdt.getNgaySinh().format(NGAY) + ")"),
                    "Trùng số điện thoại", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                    null, lua, lua[1]);
            if (chon == 0) {
                ketQua = cungSdt;
                dispose();
            } else {
                datDangXuLy(false);
                txtSdt.requestFocusInWindow();
            }
            return;
        }
        if (!trung.isEmpty()) {
            StringBuilder ds = new StringBuilder();
            trung.stream().limit(5).forEach(k -> ds.append("\n  - ").append(k.getMaKH()).append(" - ")
                    .append(k.getTenKH()));
            int chon = JOptionPane.showConfirmDialog(this,
                    "Có hồ sơ gần giống đã tồn tại:" + ds + "\n\nVẫn tạo hồ sơ mới?",
                    "Hồ sơ gần giống", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (chon != JOptionPane.YES_OPTION) {
                datDangXuLy(false);
                return;
            }
        }
        luuMoi(kh, tienSu);
    }

    private void luuMoi(KhachHang kh, String tienSu) {
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return khachHangDAO.them(kh, tienSu);
            }

            @Override
            protected void done() {
                try {
                    if (Boolean.TRUE.equals(get())) {
                        ketQua = kh;
                        JOptionPane.showMessageDialog(DlgKhachHang.this,
                                "Tạo hồ sơ thành công.\nMã bệnh nhân: " + kh.getMaKH(),
                                "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } else {
                        datDangXuLy(false);
                        JOptionPane.showMessageDialog(DlgKhachHang.this, "Không thể lưu hồ sơ, vui lòng thử lại.",
                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    xuLyLoi(e);
                }
            }
        }.execute();
    }

    private KhachHang docThemMoi() throws LoiNhap {
        String ten = txtHoTen.getText().trim();
        String sdt = txtSdt.getText().trim();
        if (ten.isEmpty()) {
            throw new LoiNhap("Vui lòng nhập đầy đủ thông tin bắt buộc", txtHoTen);
        }
        if (sdt.isEmpty()) {
            throw new LoiNhap("Vui lòng nhập đầy đủ thông tin bắt buộc", txtSdt);
        }
        LocalDate ngaySinh = null;
        String chuoiNgay = txtNgaySinh.getText().trim();
        if (!chuoiNgay.isEmpty()) {
            try {
                ngaySinh = LocalDate.parse(chuoiNgay, NGAY);
            } catch (DateTimeParseException e) {
                throw new LoiNhap("Ngày sinh phải theo định dạng dd/MM/yyyy", txtNgaySinh);
            }
        }
        try {
            return new KhachHang(null, ten, ngaySinh, radNam.isSelected(), sdt,
                    txtDiaChi.getText(), txtEmail.getText());
        } catch (IllegalArgumentException e) {
            throw new LoiNhap(e.getMessage(), truongLoi(e.getMessage()));
        }
    }

    // ---------------------------------------------------------------- UC011

    private void luuCapNhat() {
        KhachHang moi = new KhachHang(goc);
        try {
            moi.setSdt(txtSdt.getText());
        } catch (IllegalArgumentException e) {
            baoLoiNhap(new LoiNhap("Số điện thoại không hợp lệ", txtSdt));
            return;
        }
        try {
            moi.setEmail(txtEmail.getText());
        } catch (IllegalArgumentException e) {
            baoLoiNhap(new LoiNhap("Email không hợp lệ", txtEmail));
            return;
        }
        moi.setDiaChi(txtDiaChi.getText());

        boolean khongDoi = Objects.equals(moi.getSdt(), goc.getSdt())
                && Objects.equals(moi.getDiaChi(), goc.getDiaChi())
                && Objects.equals(moi.getEmail(), goc.getEmail());
        if (khongDoi) {
            JOptionPane.showMessageDialog(this, "Không có thay đổi nào để lưu.", "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        datDangXuLy(true);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return khachHangDAO.capNhat(moi);
            }

            @Override
            protected void done() {
                try {
                    if (Boolean.TRUE.equals(get())) {
                        ketQua = moi;
                        JOptionPane.showMessageDialog(DlgKhachHang.this, "Cập nhật hồ sơ thành công.",
                                "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } else {
                        datDangXuLy(false);
                        JOptionPane.showMessageDialog(DlgKhachHang.this, "Không thể cập nhật, vui lòng thử lại.",
                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    xuLyLoi(e);
                }
            }
        }.execute();
    }

    // ---------------------------------------------------------------- dùng chung

    private void datDangXuLy(boolean dangXuLy) {
        btnLuu.setEnabled(!dangXuLy);
        btnHuy.setEnabled(!dangXuLy);
        setCursor(dangXuLy ? java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR)
                : java.awt.Cursor.getDefaultCursor());
    }

    private void xuLyLoi(Exception loi) {
        datDangXuLy(false);
        Throwable goc = loi instanceof ExecutionException && loi.getCause() != null ? loi.getCause() : loi;
        if (goc instanceof BusinessException) {
            JOptionPane.showMessageDialog(this, goc.getMessage(), "Cảnh báo", JOptionPane.WARNING_MESSAGE);
        } else if (goc instanceof DataAccessException) {
            JOptionPane.showMessageDialog(this, "Không thể lưu dữ liệu, vui lòng thử lại.", "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Đã xảy ra lỗi, vui lòng thử lại.", "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void baoLoiNhap(LoiNhap loi) {
        JOptionPane.showMessageDialog(this, loi.getMessage(), "Dữ liệu chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
        if (loi.truong != null) {
            SwingUtilities.invokeLater(loi.truong::requestFocusInWindow);
        }
    }

    private JComponent truongLoi(String thongBao) {
        if (thongBao == null) {
            return txtHoTen;
        }
        if (thongBao.contains("điện thoại")) {
            return txtSdt;
        }
        if (thongBao.contains("Email")) {
            return txtEmail;
        }
        if (thongBao.contains("Ngày sinh")) {
            return txtNgaySinh;
        }
        return txtHoTen;
    }

    private static final class LoiNhap extends Exception {

        private final transient JComponent truong;

        LoiNhap(String thongBao, JComponent truong) {
            super(thongBao);
            this.truong = truong;
        }
    }
}
