package com.quanlynhakhoa.ui.dialog;

import com.quanlynhakhoa.dao.DichVuDAO;
import com.quanlynhakhoa.entity.DichVu;
import com.quanlynhakhoa.entity.LoaiDichVu;
import com.quanlynhakhoa.entity.enums.MucThue;
import com.quanlynhakhoa.exception.BusinessException;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** UC005: form thêm dịch vụ mới (Quản lý). Lưu qua DichVuDAO trong SwingWorker. */
public class DlgDichVu extends JDialog {

    /** Hành động lưu; tách ra để test form không cần DB. */
    @FunctionalInterface
    public interface LuuDichVu {
        boolean luu(DichVu dv);
    }

    public DlgDichVu(Window owner, Consumer<DichVu> daLuu) {
        this(owner, new DichVuDAO()::them, daLuu);
    }

    DlgDichVu(Window owner, LuuDichVu luu, Consumer<DichVu> daLuu) {
        super(owner, "Thêm dịch vụ mới", ModalityType.APPLICATION_MODAL);
        NoiDung form = new NoiDung(luu, dv -> {
            dispose();
            daLuu.accept(dv);
        }, this::dispose);
        setContentPane(form);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                form.yeuCauDong();
            }
        });
        getRootPane().setDefaultButton(form.btnLuu);
        getRootPane().registerKeyboardAction(e -> form.yeuCauDong(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setSize(600, 620);
        setMinimumSize(new Dimension(520, 560));
        setLocationRelativeTo(owner);
    }

    /** Nội dung form, tách khỏi cửa sổ để test nhập liệu và luồng lưu. */
    static final class NoiDung extends JPanel {

        private static final Logger LOG = Logger.getLogger(DlgDichVu.class.getName());
        private static final Color DO = new Color(0xC94C48);
        /** Mục đầu là "chưa chọn" để bắt luồng 5.2 của UC005. */
        private static final String[] TEN_PHAN_LOAI = {"-- Chọn phân loại thuế --",
            LoaiDichVu.DIEU_TRI + " (KCT)", LoaiDichVu.VAT_TU + " (5%)", LoaiDichVu.THAM_MY + " (10%)"};
        private static final String[] TEN_LOAI = {null, LoaiDichVu.DIEU_TRI, LoaiDichVu.VAT_TU, LoaiDichVu.THAM_MY};
        private static final MucThue[] MUC_THUE = {null, MucThue.THUE_0, MucThue.THUE_5, MucThue.THUE_10};

        final JTextField txtTenDV = new JTextField();
        final JTextField txtDonGia = new JTextField();
        final JSpinner spnThoiLuong = new JSpinner(new SpinnerNumberModel(30, 0, 600, 5));
        final JComboBox<String> cboPhanLoaiThue = new JComboBox<>(TEN_PHAN_LOAI);
        final JLabel lblMucThue = new JLabel(" ");
        final JTextArea txaLoTrinh = new JTextArea(4, 20);
        final JButton btnLuu = new JButton("Thêm dịch vụ");
        final JButton btnDong = new JButton("Đóng");
        final JLabel lblThongBao = new JLabel(" ");
        private final LuuDichVu luu;
        private final Consumer<DichVu> daLuu;
        private final Runnable dong;
        private boolean dangLuu;

        NoiDung(LuuDichVu luu, Consumer<DichVu> daLuu, Runnable dong) {
            super(new BorderLayout(0, 16));
            this.luu = Objects.requireNonNull(luu);
            this.daLuu = Objects.requireNonNull(daLuu);
            this.dong = Objects.requireNonNull(dong);
            setBorder(new EmptyBorder(22, 24, 22, 24));
            setBackground(SURFACE);

            JLabel title = new JLabel("Thêm dịch vụ mới");
            title.setFont(bold(22));
            title.setForeground(TEXT);
            add(title, BorderLayout.NORTH);

            JPanel fields = new JPanel(new GridBagLayout());
            fields.setBackground(SURFACE);
            txtTenDV.setName("tenDichVu");
            txtDonGia.setName("donGia");
            spnThoiLuong.setName("thoiLuong");
            cboPhanLoaiThue.setName("phanLoaiThue");
            txaLoTrinh.setName("loTrinh");
            lblMucThue.setName("mucThue");
            txtDonGia.putClientProperty("JTextField.placeholderText", "VD: 300000 hoặc 300.000");
            txtTenDV.putClientProperty("JTextField.placeholderText", "VD: Cạo vôi răng");
            txaLoTrinh.setLineWrap(true);
            txaLoTrinh.setWrapStyleWord(true);
            txaLoTrinh.setFont(font(13));
            txaLoTrinh.setToolTipText("Mỗi buổi một dòng, ví dụ: Buổi 1: khám; Buổi 2: sau 7–14 ngày.");
            themTruong(fields, 0, "Tên dịch vụ *", txtTenDV);
            themTruong(fields, 1, "Đơn giá (VNĐ) *", txtDonGia);
            themTruong(fields, 2, "Thời lượng dự kiến (phút) *", spnThoiLuong);
            themTruong(fields, 3, "Phân loại thuế *", cboPhanLoaiThue);
            lblMucThue.setFont(font(12));
            lblMucThue.setForeground(MUTED);
            themTruong(fields, 4, "", lblMucThue);
            JScrollPane scrLoTrinh = new JScrollPane(txaLoTrinh);
            scrLoTrinh.setPreferredSize(new Dimension(0, 96));
            themTruong(fields, 5, "Lộ trình dịch vụ (nếu điều trị nhiều buổi)", scrLoTrinh);
            add(fields, BorderLayout.CENTER);

            JPanel south = new JPanel(new BorderLayout(0, 10));
            south.setOpaque(false);
            lblThongBao.setName("thongBao");
            lblThongBao.setFont(font(12));
            lblThongBao.setForeground(DO);
            south.add(lblThongBao, BorderLayout.NORTH);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            buttons.setOpaque(false);
            btnLuu.setName("luuDichVu");
            btnDong.setName("dongDichVu");
            for (JButton b : new JButton[]{btnDong, btnLuu}) {
                b.setFont(bold(12));
                b.setPreferredSize(new Dimension(130, 40));
                buttons.add(b);
            }
            btnLuu.setBackground(TEAL);
            btnLuu.setForeground(Color.WHITE);
            south.add(buttons, BorderLayout.SOUTH);
            add(south, BorderLayout.SOUTH);

            cboPhanLoaiThue.addActionListener(e -> hienMucThue());
            btnLuu.addActionListener(e -> luuDichVu());
            btnDong.addActionListener(e -> yeuCauDong());
            hienMucThue();
        }

        private void themTruong(JPanel parent, int row, String nhan, JComponent field) {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = row * 2;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.insets = new Insets(row == 0 ? 0 : 8, 0, 4, 0);
            if (!nhan.isEmpty()) {
                JLabel label = new JLabel(nhan);
                label.setFont(bold(12));
                label.setForeground(TEXT);
                parent.add(label, gbc);
            }
            gbc.gridy = row * 2 + 1;
            gbc.insets = new Insets(0, 0, 0, 0);
            if (field instanceof JTextField || field instanceof JSpinner || field instanceof JComboBox) {
                field.setPreferredSize(new Dimension(0, 38));
                field.setFont(font(13));
            }
            if (field instanceof JScrollPane) {
                gbc.fill = GridBagConstraints.BOTH;
                gbc.weighty = 1;
            }
            parent.add(field, gbc);
        }

        /** UC005 bước 6: mức thuế tự gán theo phân loại đã chọn. */
        private void hienMucThue() {
            int i = cboPhanLoaiThue.getSelectedIndex();
            lblMucThue.setText(i <= 0 ? "Mức thuế GTGT sẽ được gán theo phân loại."
                    : "Mức thuế GTGT áp dụng: " + switch (MUC_THUE[i]) {
                        case THUE_0 -> "KCT (không chịu thuế)";
                        case THUE_5 -> "5%";
                        case THUE_10 -> "10%";
                    });
        }

        /** UC005 bước 4–8. */
        void luuDichVu() {
            if (dangLuu) {
                return;
            }
            DichVu dv = docForm();
            if (dv == null) {
                return;
            }
            datDangLuu(true);
            lblThongBao.setForeground(MUTED);
            lblThongBao.setText("Đang lưu dịch vụ...");
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
                            daLuu.accept(dv);
                        } else {
                            baoLoi("Không thể thêm dịch vụ. Vui lòng thử lại.", txtTenDV);
                        }
                    } catch (ExecutionException ex) {
                        Throwable cause = ex.getCause();
                        if (cause instanceof BusinessException || cause instanceof IllegalArgumentException) {
                            // UC005 5.1: tên trùng -> đề nghị đổi tên, quay lại bước 3.
                            baoLoi(cause.getMessage(), txtTenDV);
                        } else {
                            LOG.log(Level.WARNING, "Không thể thêm dịch vụ", cause);
                            baoLoi("Không thể lưu dữ liệu, vui lòng thử lại.", txtTenDV);
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }.execute();
        }

        /** UC005 bước 5 / luồng 5.2: kiểm tra trên form trước khi gọi DAO; sai trả null. */
        DichVu docForm() {
            String ten = txtTenDV.getText().trim();
            if (ten.isEmpty()) {
                return baoLoi("Vui lòng nhập tên dịch vụ.", txtTenDV);
            }
            if (ten.length() > DichVu.TEN_TOI_DA) {
                return baoLoi("Tên dịch vụ tối đa " + DichVu.TEN_TOI_DA + " ký tự.", txtTenDV);
            }
            Double donGia = docDonGia(txtDonGia.getText());
            if (donGia == null) {
                return baoLoi("Đơn giá phải là số tiền hợp lệ (VNĐ).", txtDonGia);
            }
            if (donGia <= 0) {
                return baoLoi("Đơn giá phải lớn hơn 0.", txtDonGia);
            }
            int thoiLuong = ((Number) spnThoiLuong.getValue()).intValue();
            if (thoiLuong <= 0) {
                return baoLoi("Thời lượng dự kiến phải lớn hơn 0 phút.", spnThoiLuong);
            }
            int loai = cboPhanLoaiThue.getSelectedIndex();
            if (loai <= 0) {
                return baoLoi("Vui lòng chọn phân loại thuế.", cboPhanLoaiThue);
            }
            try {
                return new DichVu(null, ten, donGia, thoiLuong, txaLoTrinh.getText(), true,
                        new LoaiDichVu(null, TEN_LOAI[loai], MUC_THUE[loai]));
            } catch (IllegalArgumentException ex) {
                return baoLoi(ex.getMessage(), txtTenDV);
            }
        }

        /** Nhận "300000", "300.000", "300,000", "300.000 ₫"; không phải số nguyên dương thì null. */
        static Double docDonGia(String text) {
            String so = text == null ? "" : text.replace("₫", "").replace("đ", "").replaceAll("[\\s.,]", "");
            if (so.isEmpty() || !so.matches("-?\\d{1,15}")) {
                return null;
            }
            return Double.valueOf(so);
        }

        private DichVu baoLoi(String thongBao, JComponent field) {
            lblThongBao.setForeground(DO);
            lblThongBao.setText(thongBao);
            field.requestFocusInWindow();
            if (field instanceof JTextField txt) {
                txt.selectAll();
            }
            return null;
        }

        private void datDangLuu(boolean dangLuu) {
            this.dangLuu = dangLuu;
            for (JComponent c : new JComponent[]{txtTenDV, txtDonGia, spnThoiLuong, cboPhanLoaiThue,
                txaLoTrinh, btnLuu, btnDong}) {
                c.setEnabled(!dangLuu);
            }
        }

        /** UC005 luồng 3.1: "Đóng" để hủy; không đóng khi đang lưu. */
        void yeuCauDong() {
            if (!dangLuu) {
                dong.run();
            }
        }
    }
}
