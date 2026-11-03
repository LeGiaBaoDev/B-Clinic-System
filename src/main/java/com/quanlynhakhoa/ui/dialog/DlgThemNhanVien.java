package com.quanlynhakhoa.ui.dialog;

import com.quanlynhakhoa.dao.TaiKhoanDAO;
import com.quanlynhakhoa.entity.NhanVien;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import com.quanlynhakhoa.util.SessionManager;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** UC002: tạo nhân viên và tài khoản trong transaction của TaiKhoanDAO. */
public class DlgThemNhanVien extends JDialog {

    public DlgThemNhanVien(Window owner, Consumer<NhanVien> daTao) {
        super(owner, "Thêm nhân viên và tài khoản", ModalityType.APPLICATION_MODAL);
        NoiDung form = new NoiDung(new TaiKhoanDAO()::taoNhanVienVaTaiKhoan,
                nv -> { dispose(); daTao.accept(nv); }, this::dispose);
        setContentPane(form);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) { form.yeuCauDong(); }
        });
        getRootPane().setDefaultButton(form.btnLuu);
        getRootPane().registerKeyboardAction(e -> form.yeuCauDong(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(680, bounds.width - 32), Math.min(690, bounds.height - 40));
        setMinimumSize(new Dimension(560, 480));
        setLocationRelativeTo(owner);
    }

    /** Nội dung tách khỏi cửa sổ để kiểm tra nhập liệu và luồng lưu không cần mở dialog. */
    static final class NoiDung extends JPanel {
        private static final Logger LOG = Logger.getLogger(DlgThemNhanVien.class.getName());
        private final JTextField txtHoTen = new JTextField();
        private final JComboBox<String> cboGioiTinh = new JComboBox<>(new String[]{"Nam", "Nữ"});
        private final JTextField txtSdt = new JTextField();
        private final JTextField txtEmail = new JTextField();
        private final JTextField txtChuyenMon = new JTextField();
        private final JTextField txtTenTaiKhoan = new JTextField();
        private final JComboBox<VaiTro> cboVaiTro = new JComboBox<>(VaiTro.values());
        private final JPasswordField txtMatKhau = new JPasswordField();
        private final JPasswordField txtNhapLai = new JPasswordField();
        private final JCheckBox chkHienMatKhau = new JCheckBox("Hiện mật khẩu");
        private final JButton btnLuu = new JButton("Tạo nhân viên");
        private final JButton btnHuy = new JButton("Hủy");
        private final JLabel lblThongBao = new JLabel(" ");
        private final BiPredicate<NhanVien, TaiKhoan> luu;
        private final Consumer<NhanVien> daTao;
        private final Runnable dong;
        private boolean dangLuu;

        NoiDung(BiPredicate<NhanVien, TaiKhoan> luu, Consumer<NhanVien> daTao, Runnable dong) {
            super(new BorderLayout(0, 16));
            this.luu = java.util.Objects.requireNonNull(luu);
            this.daTao = java.util.Objects.requireNonNull(daTao);
            this.dong = java.util.Objects.requireNonNull(dong);
            setBorder(new EmptyBorder(22, 24, 22, 24));
            setBackground(SURFACE);
            JLabel title = new JLabel("Thêm nhân viên và tài khoản");
            title.setFont(bold(22));
            title.setForeground(TEXT);
            add(title, BorderLayout.NORTH);

            JPanel fields = new JPanel(new GridBagLayout());
            fields.setBackground(SURFACE);
            themMuc(fields, 0, "THÔNG TIN NHÂN VIÊN");
            themTruong(fields, 1, 0, "Họ tên *", txtHoTen, "hoTen");
            themTruong(fields, 1, 1, "Giới tính *", cboGioiTinh, "gioiTinh");
            themTruong(fields, 2, 0, "Số điện thoại *", txtSdt, "sdt");
            themTruong(fields, 2, 1, "Email *", txtEmail, "email");
            themTruong(fields, 3, 0, "Chuyên môn *", txtChuyenMon, "chuyenMon");
            themMuc(fields, 4, "TÀI KHOẢN ĐĂNG NHẬP");
            themTruong(fields, 5, 0, "Tên đăng nhập *", txtTenTaiKhoan, "tenTaiKhoan");
            themTruong(fields, 5, 1, "Vai trò *", cboVaiTro, "vaiTro");
            themTruong(fields, 6, 0, "Mật khẩu (ít nhất 6 ký tự) *", txtMatKhau, "matKhau");
            themTruong(fields, 6, 1, "Nhập lại mật khẩu *", txtNhapLai, "nhapLai");
            cboVaiTro.setSelectedItem(VaiTro.NHASI);
            cboVaiTro.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                        boolean selected, boolean focus) {
                    super.getListCellRendererComponent(list, value, index, selected, focus);
                    setText(value == null ? "" : switch ((VaiTro) value) {
                        case QUANLY -> "Quản lý";
                        case NHASI -> "Nha sĩ";
                        case LETAN -> "Lễ tân";
                    });
                    return this;
                }
            });
            JPanel body = new JPanel(new BorderLayout(0, 8));
            body.setBackground(SURFACE);
            body.add(fields, BorderLayout.NORTH);
            JPanel notes = new JPanel(new GridLayout(0, 1, 0, 6));
            notes.setOpaque(false);
            chkHienMatKhau.setOpaque(false);
            chkHienMatKhau.setFont(font(12));
            notes.add(chkHienMatKhau);
            JLabel note = new JLabel("Mã được tạo tự động. Tài khoản mới ở trạng thái Hoạt động.");
            note.setFont(font(12));
            note.setForeground(MUTED);
            notes.add(note);
            body.add(notes, BorderLayout.CENTER);
            JScrollPane scroll = new JScrollPane(body);
            scroll.setBorder(null);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            add(scroll, BorderLayout.CENTER);

            JPanel footer = new JPanel(new BorderLayout(0, 10));
            footer.setOpaque(false);
            lblThongBao.setName("thongBao");
            lblThongBao.setForeground(new Color(0xC94C48));
            lblThongBao.setFont(font(12));
            footer.add(lblThongBao, BorderLayout.NORTH);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            buttons.setOpaque(false);
            btnLuu.setName("luuNhanVien");
            btnHuy.setName("huyNhanVien");
            btnLuu.setBackground(TEAL);
            btnLuu.setForeground(Color.WHITE);
            for (JButton b : new JButton[]{btnHuy, btnLuu}) {
                b.setFont(bold(12));
                b.setPreferredSize(new Dimension(145, 38));
                buttons.add(b);
            }
            footer.add(buttons, BorderLayout.SOUTH);
            add(footer, BorderLayout.SOUTH);
            btnLuu.addActionListener(e -> taoNhanVien());
            btnHuy.addActionListener(e -> yeuCauDong());
            char echo = txtMatKhau.getEchoChar();
            chkHienMatKhau.addActionListener(e -> {
                txtMatKhau.setEchoChar(chkHienMatKhau.isSelected() ? (char) 0 : echo);
                txtNhapLai.setEchoChar(chkHienMatKhau.isSelected() ? (char) 0 : echo);
            });
            for (JTextField field : cacTruong()) {
                field.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusLost(FocusEvent e) {
                        if (!dangLuu) { field.putClientProperty("JComponent.outline", loiTruong(field) == null
                                ? null : "error"); }
                    }
                });
            }
        }

        private JTextField[] cacTruong() {
            return new JTextField[]{txtHoTen, txtSdt, txtEmail, txtChuyenMon,
                txtTenTaiKhoan, txtMatKhau, txtNhapLai};
        }

        private void themMuc(JPanel form, int row, String text) {
            JLabel label = new JLabel(text);
            label.setFont(bold(11));
            label.setForeground(TEAL);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0; c.gridy = row; c.gridwidth = 2; c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(row == 0 ? 0 : 12, 0, 10, 0);
            form.add(label, c);
        }

        private void themTruong(JPanel form, int row, int col, String text, JComponent input, String name) {
            JPanel group = new JPanel(new BorderLayout(0, 5));
            group.setOpaque(false);
            JLabel label = new JLabel(text);
            label.setFont(font(12));
            label.setForeground(TEXT);
            label.setLabelFor(input);
            input.setName(name);
            input.setFont(font(13));
            input.setPreferredSize(new Dimension(230, 36));
            group.add(label, BorderLayout.NORTH);
            group.add(input, BorderLayout.CENTER);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = col; c.gridy = row; c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, col == 0 ? 0 : 10, 12, col == 0 ? 10 : 0);
            form.add(group, c);
        }

        private String loiTruong(JTextField field) {
            try {
                NhanVien nv = new NhanVien();
                if (field == txtHoTen) { nv.setHoTenNV(field.getText()); }
                else if (field == txtSdt) { nv.setSdt(field.getText()); }
                else if (field == txtEmail) {
                    if (field.getText().trim().length() > 100) { return "Email tối đa 100 ký tự."; }
                    nv.setEmail(field.getText());
                } else if (field == txtChuyenMon) {
                    if (field.getText().trim().length() > 100) { return "Chuyên môn tối đa 100 ký tự."; }
                    nv.setChuyenMon(field.getText());
                } else if (field == txtTenTaiKhoan) {
                    if (field.getText().trim().length() > 50) { return "Tên đăng nhập tối đa 50 ký tự."; }
                    if (field.getText().trim().length() < TaiKhoan.DO_DAI_TEN_TOI_THIEU) {
                        return "Tên đăng nhập phải có ít nhất 4 ký tự.";
                    }
                    new TaiKhoan().setTenTK(field.getText());
                } else {
                    char[] password = txtMatKhau.getPassword();
                    char[] repeat = txtNhapLai.getPassword();
                    try {
                        if (field == txtMatKhau) {
                            String raw = new String(password);
                            TaiKhoan.kiemTraMatKhauGoc(raw);
                            new TaiKhoan().setMatKhau(raw);
                        } else if (!Arrays.equals(password, repeat)) {
                            return "Mật khẩu nhập lại không khớp.";
                        }
                    } finally {
                        Arrays.fill(password, '\0');
                        Arrays.fill(repeat, '\0');
                    }
                }
                return null;
            } catch (IllegalArgumentException ex) {
                return ex.getMessage();
            }
        }

        private void taoNhanVien() {
            if (dangLuu) { return; }
            TaiKhoan actor = SessionManager.getInstance().getCurrentUser();
            if (actor == null || actor.getVaiTro() != VaiTro.QUANLY) {
                lblThongBao.setText("Chỉ Quản lý được tạo nhân viên và tài khoản.");
                return;
            }
            for (JTextField field : cacTruong()) {
                String error = loiTruong(field);
                field.putClientProperty("JComponent.outline", error == null ? null : "error");
                if (error != null) {
                    lblThongBao.setText(error);
                    field.requestFocusInWindow();
                    return;
                }
            }
            NhanVien nv = new NhanVien(null, txtHoTen.getText(), cboGioiTinh.getSelectedIndex() == 0,
                    txtSdt.getText(), txtEmail.getText(), txtChuyenMon.getText());
            char[] password = txtMatKhau.getPassword();
            TaiKhoan tk;
            try {
                tk = new TaiKhoan(null, txtTenTaiKhoan.getText(), new String(password),
                        (VaiTro) cboVaiTro.getSelectedItem(), true, nv);
            } finally {
                Arrays.fill(password, '\0');
            }
            datDangLuu(true);
            lblThongBao.setText("Đang tạo nhân viên và tài khoản...");
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() { return luu.test(nv, tk); }

                @Override
                protected void done() {
                    boolean success = false;
                    try {
                        success = get();
                        if (!success) { lblThongBao.setText("Chưa tạo được nhân viên. Vui lòng thử lại."); }
                    } catch (ExecutionException ex) {
                        Throwable cause = ex.getCause();
                        lblThongBao.setText(cause instanceof BusinessException || cause instanceof IllegalArgumentException
                                ? cause.getMessage() : "Không thể lưu dữ liệu. Vui lòng thử lại.");
                        // Không ghi exception vì có thể chứa tham số nhạy cảm của thao tác tạo tài khoản.
                        LOG.log(Level.WARNING, "Tạo nhân viên thất bại: {0}", cause.getClass().getSimpleName());
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        lblThongBao.setText("Đã gián đoạn thao tác tạo nhân viên.");
                    } finally {
                        tk.setMatKhau("[Ẩn]");
                        txtMatKhau.setText("");
                        txtNhapLai.setText("");
                        datDangLuu(false);
                    }
                    if (success) { daTao.accept(nv); }
                }
            }.execute();
        }

        private void datDangLuu(boolean value) {
            dangLuu = value;
            for (JTextField field : cacTruong()) { field.setEnabled(!value); }
            cboGioiTinh.setEnabled(!value);
            cboVaiTro.setEnabled(!value);
            chkHienMatKhau.setEnabled(!value);
            btnLuu.setEnabled(!value);
            btnLuu.setText(value ? "Đang lưu..." : "Tạo nhân viên");
            btnHuy.setEnabled(!value);
        }

        void yeuCauDong() {
            if (!dangLuu) {
                txtMatKhau.setText("");
                txtNhapLai.setText("");
                dong.run();
            }
        }
    }
}
