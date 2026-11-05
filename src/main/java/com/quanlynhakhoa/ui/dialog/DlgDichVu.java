package com.quanlynhakhoa.ui.dialog;

import com.quanlynhakhoa.dao.DichVuDAO;
import com.quanlynhakhoa.entity.DichVu;
import com.quanlynhakhoa.entity.LoaiDichVu;
import com.quanlynhakhoa.entity.enums.MucThue;
import com.quanlynhakhoa.exception.BusinessException;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class DlgDichVu extends JDialog {

    /** Hành động lưu; tách ra để test form không cần DB. */
    @FunctionalInterface
    public interface LuuDichVu {
        boolean luu(DichVu dv);
    }

    public DlgDichVu(Window owner, Consumer<DichVu> daLuu) {
        this(owner, new DichVuDAO()::them, new DichVuDAO()::layLoTrinhDaLuu, daLuu);
    }

    DlgDichVu(Window owner, LuuDichVu luu, Supplier<List<String>> layLoTrinh, Consumer<DichVu> daLuu) {
        super(owner, "Thêm dịch vụ mới", ModalityType.APPLICATION_MODAL);
        NoiDung form = new NoiDung(luu, dv -> {
            dispose();
            daLuu.accept(dv);
        }, this::dispose, layLoTrinh);
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
        setSize(600, 690); // cao hơn bản đầu để có chỗ cho các khung gợi ý lộ trình
        setMinimumSize(new Dimension(520, 600));
        setLocationRelativeTo(owner);
    }

    /** Nội dung form, tách khỏi cửa sổ để test nhập liệu và luồng lưu. */
    static final class NoiDung extends JPanel {

        private static final Logger LOG = Logger.getLogger(DlgDichVu.class.getName());
        private static final Color DO = new Color(0xC94C48);
        
        private static final String[] TEN_PHAN_LOAI = {"-- Chọn phân loại thuế --",
            LoaiDichVu.DIEU_TRI + " (KCT)", LoaiDichVu.VAT_TU + " (5%)", LoaiDichVu.THAM_MY + " (10%)"};
        private static final String[] TEN_LOAI = {null, LoaiDichVu.DIEU_TRI, LoaiDichVu.VAT_TU, LoaiDichVu.THAM_MY};
        private static final MucThue[] MUC_THUE = {null, MucThue.THUE_0, MucThue.THUE_5, MucThue.THUE_10};
        /** Số khung gợi ý lộ trình tối đa hiện cùng lúc. */
        static final int SO_GOI_Y = DlgDichVu.SO_GOI_Y;

        final JTextField txtTenDV = new JTextField();
        final JTextField txtDonGia = new JTextField();
        final JSpinner spnThoiLuong = new JSpinner(new SpinnerNumberModel(30, 0, 600, 5));
        final JComboBox<String> cboPhanLoaiThue = new JComboBox<>(TEN_PHAN_LOAI);
        final JLabel lblMucThue = new JLabel(" ");
        final JTextArea txaLoTrinh = new JTextArea(4, 20);
        final JButton btnLuu = new JButton("Thêm dịch vụ");
        final JButton btnDong = new JButton("Đóng");
        final JLabel lblThongBao = new JLabel(" ");
        /** Hàng khung gợi ý lộ trình, nằm ngay dưới ô lộ trình; tự xuống dòng khi nhiều khung. */
        final JPanel pnlGoiY = new JPanel(new WrapLayout(FlowLayout.LEFT, 6, 6));
        /** Lộ trình đã lưu trong DB, mới nhất trước (nạp 1 lần khi mở form). */
        private List<String> loTrinhDaLuu = List.of();
        private final LuuDichVu luu;
        private final Consumer<DichVu> daLuu;
        private final Runnable dong;
        private boolean dangLuu;

        /** Form không có nguồn gợi ý (dùng trong test các luồng nhập liệu). */
        NoiDung(LuuDichVu luu, Consumer<DichVu> daLuu, Runnable dong) {
            this(luu, daLuu, dong, List::of);
        }

        NoiDung(LuuDichVu luu, Consumer<DichVu> daLuu, Runnable dong, Supplier<List<String>> layLoTrinh) {
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
            // Hàng gợi ý ngay dưới ô lộ trình (gridy 12 = sau 6 trường x 2 dòng nhãn/ô).
            pnlGoiY.setName("goiYLoTrinh");
            pnlGoiY.setOpaque(false);
            GridBagConstraints gbcGoiY = new GridBagConstraints();
            gbcGoiY.gridx = 0;
            gbcGoiY.gridy = 12;
            gbcGoiY.fill = GridBagConstraints.HORIZONTAL;
            gbcGoiY.weightx = 1;
            gbcGoiY.insets = new Insets(4, -6, 0, 0); // -6: bù lề trái mặc định của WrapLayout
            fields.add(pnlGoiY, gbcGoiY);
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
            // Gõ vào ô lộ trình -> lọc lại các khung gợi ý.
            txaLoTrinh.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    veGoiY();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    veGoiY();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    veGoiY();
                }
            });
            hienMucThue();
            napGoiY(Objects.requireNonNull(layLoTrinh));
        }

        // ===================== Gợi ý lộ trình =====================

        /** Nạp lộ trình đã lưu một lần (chạy nền). Lỗi thì chỉ không có gợi ý, form vẫn dùng bình thường. */
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
                        loTrinhDaLuu = List.of();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    veGoiY();
                }
            }.execute();
        }

        /** Giữ tên cũ cho test; logic nằm ở {@link DlgDichVu#locGoiY} để trang Sửa dùng chung. */
        static List<String> locGoiY(List<String> daLuu, String dangGo) {
            return DlgDichVu.locGoiY(daLuu, dangGo);
        }

        /** Vẽ lại các khung gợi ý dưới ô lộ trình theo nội dung hiện tại. */
        private void veGoiY() {
            DlgDichVu.veGoiY(pnlGoiY, loTrinhDaLuu, txaLoTrinh.getText(), loTrinh -> {
                if (!dangLuu) {
                    chonGoiY(loTrinh);
                }
            });
        }

        /** Bấm gợi ý: điền nguyên lộ trình vào ô (người dùng vẫn sửa tiếp được). */
        void chonGoiY(String loTrinh) {
            txaLoTrinh.setText(loTrinh);
            txaLoTrinh.requestFocusInWindow();
            txaLoTrinh.setCaretPosition(txaLoTrinh.getDocument().getLength());
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

        
        private void hienMucThue() {
            int i = cboPhanLoaiThue.getSelectedIndex();
            lblMucThue.setText(i <= 0 ? "Mức thuế GTGT sẽ được gán theo phân loại."
                    : "Mức thuế GTGT áp dụng: " + switch (MUC_THUE[i]) {
                        case THUE_0 -> "KCT (không chịu thuế)";
                        case THUE_5 -> "5%";
                        case THUE_10 -> "10%";
                    });
        }

        
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
            if (so.isEmpty() || !so.matches("\\d{1,15}")) {
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

        
        void yeuCauDong() {
            if (!dangLuu) {
                dong.run();
            }
        }
    }

    // ===================== Gợi ý lộ trình (dùng chung cho form Thêm và trang Sửa) =====================

    /** Số khung gợi ý lộ trình tối đa hiện cùng lúc. */
    public static final int SO_GOI_Y = 5;
    private static final Color NEN_GOI_Y = new Color(0xEEF1F4);
    private static final Color NEN_GOI_Y_HOVER = new Color(0xE2E8EE);

    /**
     * Lọc gợi ý theo nội dung đang gõ: chứa đoạn đã gõ (không phân biệt hoa thường), bỏ cái giống y hệt
     * nội dung hiện tại (đã điền rồi thì không gợi ý lại), giữ thứ tự mới nhất trước, lấy tối đa
     * {@value #SO_GOI_Y}. Ô trống thì gợi ý 5 lộ trình mới nhất.
     */
    public static List<String> locGoiY(List<String> daLuu, String dangGo) {
        String tim = dangGo == null ? "" : dangGo.trim().toLowerCase(Locale.ROOT);
        List<String> ketQua = new ArrayList<>();
        for (String loTrinh : daLuu) {
            if (ketQua.size() == SO_GOI_Y) {
                break;
            }
            String thuong = loTrinh.trim().toLowerCase(Locale.ROOT);
            if (thuong.contains(tim) && !thuong.equals(tim)) {
                ketQua.add(loTrinh);
            }
        }
        return ketQua;
    }

    /**
     * Vẽ lại các khung gợi ý vào {@code pnlGoiY} (nên dùng {@link WrapLayout}) theo nội dung đang gõ.
     *
     * @param chon gọi khi người dùng bấm một khung, nhận nguyên lộ trình để điền vào ô
     */
    public static void veGoiY(JPanel pnlGoiY, List<String> daLuu, String dangGo, Consumer<String> chon) {
        pnlGoiY.removeAll();
        List<String> goiY = locGoiY(daLuu, dangGo);
        if (!goiY.isEmpty()) {
            JLabel nhan = new JLabel("Gợi ý:");
            nhan.setFont(font(12));
            nhan.setForeground(MUTED);
            pnlGoiY.add(nhan);
        }
        for (int i = 0; i < goiY.size(); i++) {
            pnlGoiY.add(khungGoiY(goiY.get(i), i, chon));
        }
        pnlGoiY.revalidate();
        pnlGoiY.repaint();
    }

    /**
     * Một khung gợi ý: nền xám nhạt bo góc, nội dung rút về 1 dòng (các buổi nối bằng " · ", quá
     * 60 ký tự thì cắt "…"); rê chuột xem đủ từng buổi; bấm để điền.
     */
    private static JComponent khungGoiY(String loTrinh, int thuTu, Consumer<String> chon) {
        String motDong = loTrinh.trim().replaceAll("\\s*\\R\\s*", " · ");
        JLabel khung = new JLabel(motDong.length() > 60 ? motDong.substring(0, 57) + "…" : motDong) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        khung.setName("goiY" + thuTu);
        khung.setFont(font(12));
        khung.setForeground(TEXT);
        khung.setBackground(NEN_GOI_Y);
        khung.setBorder(new EmptyBorder(5, 10, 5, 10));
        khung.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        khung.setToolTipText("<html>" + loTrinh.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\n", "<br>") + "<br><i>Bấm để điền</i></html>");
        khung.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                chon.accept(loTrinh);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                khung.setBackground(NEN_GOI_Y_HOVER);
                khung.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                khung.setBackground(NEN_GOI_Y);
                khung.repaint();
            }
        });
        return khung;
    }

    /**
     * FlowLayout tự xuống hàng theo chiều rộng thật của khung chứa. FlowLayout gốc tính chiều cao như
     * chỉ có 1 hàng nên khi nhiều khung gợi ý, hàng thứ hai bị che mất.
     */
    public static final class WrapLayout extends FlowLayout {
        public WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return kichThuoc(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension d = kichThuoc(target, false);
            d.width -= getHgap() + 1;
            return d;
        }

        private Dimension kichThuoc(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                // Lúc chưa được xếp lần nào, khung chứa rộng 0 -> lấy chiều rộng của cha.
                int rongToiDa = target.getWidth();
                if (rongToiDa == 0 && target.getParent() != null) {
                    rongToiDa = target.getParent().getWidth();
                }
                if (rongToiDa <= 0) {
                    rongToiDa = Integer.MAX_VALUE;
                }
                Insets le = target.getInsets();
                rongToiDa -= le.left + le.right + getHgap() * 2;
                int rong = 0;
                int cao = 0;
                int rongHang = 0;
                int caoHang = 0;
                for (Component c : target.getComponents()) {
                    if (!c.isVisible()) {
                        continue;
                    }
                    Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                    if (rongHang + d.width > rongToiDa && rongHang > 0) { // hết chỗ -> sang hàng mới
                        rong = Math.max(rong, rongHang);
                        cao += caoHang + getVgap();
                        rongHang = 0;
                        caoHang = 0;
                    }
                    rongHang += d.width + getHgap();
                    caoHang = Math.max(caoHang, d.height);
                }
                rong = Math.max(rong, rongHang);
                cao += caoHang;
                return new Dimension(rong + le.left + le.right + getHgap() * 2,
                        cao + le.top + le.bottom + getVgap() * 2);
            }
        }
    }
}
