package com.quanlynhakhoa.ui.dialog;

import com.quanlynhakhoa.entity.Voucher;
import com.quanlynhakhoa.dao.VoucherDAO;
import com.quanlynhakhoa.exception.BusinessException;
import static com.quanlynhakhoa.ui.component.UiConstants.*;
import com.toedter.calendar.JDateChooser;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.*;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** UC036/037: chỉnh trên bản sao, chỉ trả kết quả sau khi DAO lưu thành công. */
public class DlgVoucher extends JDialog {
    public DlgVoucher(Window owner, Voucher voucher, boolean daSuDung,
            Function<Voucher, Boolean> luu, Consumer<Voucher> daLuu) {
        super(owner, voucher == null ? "Thêm voucher" : "Chi tiết voucher", ModalityType.APPLICATION_MODAL);
        NoiDung form = new NoiDung(voucher, daSuDung, luu, v -> { dispose(); daLuu.accept(v); }, this::dispose);
        setContentPane(form);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { form.yeuCauDong(); }
        });
        getRootPane().setDefaultButton(form.btnLuu);
        getRootPane().registerKeyboardAction(e -> form.yeuCauDong(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setSize(580, 670);
        setMinimumSize(new Dimension(500, 530));
        setLocationRelativeTo(owner);
        if (voucher == null) form.taiMaDuKien(new VoucherDAO()::layMaDuKien);
    }

    static final class NoiDung extends JPanel {
        private static final Logger LOG = Logger.getLogger(DlgVoucher.class.getName());
        private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/uuuu")
                .withResolverStyle(ResolverStyle.STRICT);
        final JTextField txtMa = new JTextField(), txtTen = new JTextField(),
                txtGiaGiam = new JTextField(), txtToiDa = new JTextField();
        final JComboBox<String> cboLoai = new JComboBox<>(new String[]{"Giảm số tiền (₫)", "Giảm phần trăm (%)"});
        final JDateChooser dtpBatDau = new JDateChooser(), dtpKetThuc = new JDateChooser();
        final JCheckBox chkHoatDong = new JCheckBox("Cho phép sử dụng trong thời hạn áp dụng", true);
        final JButton btnLuu = new JButton("Lưu voucher"), btnDong = new JButton("Hủy");
        final JLabel lblLoi = new JLabel(" ");
        private final Voucher banDau;
        private final boolean chiDoc;
        private final Function<Voucher, Boolean> luu;
        private final Consumer<Voucher> daLuu;
        private final Runnable dong;
        private boolean dangLuu;
        private JComponent truongLoi;

        NoiDung(Voucher voucher, boolean chiDoc, Function<Voucher, Boolean> luu,
                Consumer<Voucher> daLuu, Runnable dong) {
            super(new BorderLayout(0, 16));
            this.banDau = voucher == null ? null : new Voucher(voucher);
            this.chiDoc = chiDoc;
            this.luu = luu; this.daLuu = daLuu; this.dong = dong;
            txtMa.setName("maVoucher"); txtTen.setName("tenVoucher");
            txtGiaGiam.setName("giaGiamVoucher"); txtToiDa.setName("giamToiDaVoucher");
            btnLuu.setName("luuVoucher"); btnDong.setName("dongVoucher");
            setBackground(SURFACE);
            setBorder(new EmptyBorder(22, 24, 20, 24));
            JLabel title = new JLabel(voucher == null ? "Thêm voucher mới" : "Voucher " + voucher.getMaVoucher());
            title.setFont(bold(22)); title.setForeground(TEXT);
            add(title, BorderLayout.NORTH);
            JPanel fields = new JPanel(new GridBagLayout()); fields.setBackground(SURFACE);
            txtMa.setEditable(false); txtMa.setText(voucher == null ? "Đang lấy mã voucher..." : voucher.getMaVoucher());
            if (voucher == null) txtMa.setToolTipText("Mã dự kiến, hệ thống kiểm tra lại khi lưu để tránh trùng mã.");
            txtTen.putClientProperty("JTextField.placeholderText", "Tên ưu đãi, từ 2 đến 100 ký tự");
            txtGiaGiam.putClientProperty("JTextField.placeholderText", "VD: 100000 hoặc 10,5 (không dấu ngăn nghìn)");
            txtToiDa.putClientProperty("JTextField.placeholderText", "Để trống hoặc 0: không giới hạn");
            themTruong(fields, 0, "Mã voucher", txtMa);
            themTruong(fields, 1, "Tên voucher *", txtTen);
            themTruong(fields, 2, "Loại giảm giá *", cboLoai);
            themTruong(fields, 3, "Giá trị giảm *", txtGiaGiam);
            themTruong(fields, 4, "Giảm tối đa (₫, chỉ áp dụng cho phần trăm)", txtToiDa);
            JPanel dates = new JPanel(new GridLayout(1, 2, 14, 0)); dates.setOpaque(false);
            for (JDateChooser date : new JDateChooser[]{dtpBatDau, dtpKetThuc}) {
                date.setLocale(Locale.forLanguageTag("vi-VN")); date.setDateFormatString("dd/MM/yyyy");
            }
            dates.add(dtpBatDau); dates.add(dtpKetThuc);
            JPanel dateLabels = new JPanel(new GridLayout(1, 2, 14, 0)); dateLabels.setOpaque(false);
            for (String name : new String[]{"Ngày bắt đầu *", "Ngày kết thúc *"}) {
                JLabel label = new JLabel(name); label.setFont(font(12)); label.setForeground(TEXT); dateLabels.add(label);
            }
            GridBagConstraints dateLayout = new GridBagConstraints();
            dateLayout.gridx = 0; dateLayout.gridy = 10; dateLayout.weightx = 1; dateLayout.fill = GridBagConstraints.HORIZONTAL;
            dateLayout.insets = new Insets(0, 0, 5, 0); fields.add(dateLabels, dateLayout);
            dateLayout.gridy = 11; dateLayout.insets = new Insets(0, 0, 13, 0);
            dates.setPreferredSize(new Dimension(0, 34)); fields.add(dates, dateLayout);
            themTruong(fields, 6, "Trạng thái", chkHoatDong);
            chkHoatDong.setOpaque(false);
            JScrollPane scroll = new JScrollPane(fields); scroll.setBorder(null);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            add(scroll, BorderLayout.CENTER);
            JPanel bottom = new JPanel(new BorderLayout(0, 10)); bottom.setOpaque(false);
            lblLoi.setForeground(new Color(0xBC4141)); lblLoi.setFont(font(12)); bottom.add(lblLoi, BorderLayout.NORTH);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); actions.setOpaque(false);
            btnLuu.setBackground(TEAL); btnLuu.setForeground(Color.WHITE);
            actions.add(btnDong); actions.add(btnLuu); bottom.add(actions, BorderLayout.SOUTH);
            add(bottom, BorderLayout.SOUTH);
            if (voucher != null) {
                txtTen.setText(voucher.getTenVoucher()); cboLoai.setSelectedIndex(voucher.isLoaiGiam() ? 1 : 0);
                txtGiaGiam.setText(so(voucher.getGiaGiam())); txtToiDa.setText(so(voucher.getGiaTriGiamToiDa()));
                dtpBatDau.setDate(date(voucher.getNgayBatDau())); dtpKetThuc.setDate(date(voucher.getNgayKetThuc()));
                chkHoatDong.setSelected(voucher.isTrangThai());
            } else {
                dtpBatDau.setDate(date(LocalDate.now())); dtpKetThuc.setDate(date(LocalDate.now().plusMonths(1)));
            }
            cboLoai.addActionListener(e -> txtToiDa.setEnabled(!chiDoc && !dangLuu && cboLoai.getSelectedIndex() == 1));
            for (JTextField input : new JTextField[]{txtTen, txtGiaGiam, txtToiDa}) {
                input.addFocusListener(new FocusAdapter() {
                    @Override public void focusLost(FocusEvent e) { if (!chiDoc && !dangLuu) kiemTra(); }
                });
            }
            for (JDateChooser input : new JDateChooser[]{dtpBatDau, dtpKetThuc}) {
                input.getDateEditor().getUiComponent().addFocusListener(new FocusAdapter() {
                    @Override public void focusLost(FocusEvent e) { if (!chiDoc && !dangLuu) kiemTra(); }
                });
            }
            btnLuu.addActionListener(e -> luu()); btnDong.addActionListener(e -> yeuCauDong());
            batNhap(true);
            if (chiDoc) {
                lblLoi.setText("Đã dùng trên hóa đơn: chỉ xem thông tin hoặc khóa từ danh sách.");
                btnLuu.setVisible(false); btnDong.setText("Đóng");
            }
        }

        void taiMaDuKien(Supplier<String> nguon) {
            if (banDau != null) return;
            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() { return nguon.get(); }
                @Override protected void done() {
                    try { txtMa.setText(get()); }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); txtMa.setText("Chưa lấy được mã");
                    } catch (ExecutionException e) {
                        txtMa.setText("Chưa lấy được mã");
                        LOG.log(Level.WARNING, "Không thể lấy mã voucher dự kiến", e.getCause());
                        if (!dangLuu) lblLoi.setText(e.getCause() instanceof BusinessException
                                ? e.getCause().getMessage() : "Không thể lấy mã voucher. Vui lòng đóng và mở lại form.");
                    }
                }
            }.execute();
        }

        private static void themTruong(JPanel form, int row, String label, JComponent input) {
            GridBagConstraints g = new GridBagConstraints();
            g.gridx = 0; g.gridy = row * 2; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
            g.insets = new Insets(0, 0, 5, 0);
            JLabel lbl = new JLabel(label); lbl.setFont(font(12)); lbl.setForeground(TEXT); form.add(lbl, g);
            g.gridy++; g.insets = new Insets(0, 0, 13, 0);
            input.setFont(font(13)); input.setPreferredSize(new Dimension(0, 34)); form.add(input, g);
        }

        private static String so(double value) { return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString().replace('.', ','); }
        private static Date date(LocalDate value) { return Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant()); }

        private BigDecimal docSo(JTextField field, String name, boolean optional) {
            String text = field.getText().trim();
            if (optional && text.isEmpty()) return BigDecimal.ZERO;
            if (!text.matches("[0-9]+([,.][0-9]{1,2})?")) {
                throw loi(field, name + ": nhập số không âm, tối đa 2 chữ số thập phân.");
            }
            BigDecimal value = new BigDecimal(text.replace(',', '.'));
            if (value.compareTo(new BigDecimal("9999999999999999.99")) > 0) {
                throw loi(field, name + " quá lớn.");
            }
            return value;
        }

        private LocalDate docNgay(JDateChooser field, String label) {
            try { return LocalDate.parse(((JTextField) field.getDateEditor().getUiComponent()).getText().trim(), NGAY); }
            catch (DateTimeException e) { throw loi((JComponent) field.getDateEditor().getUiComponent(), label + " phải là ngày hợp lệ (dd/MM/yyyy)."); }
        }

        private IllegalArgumentException loi(JComponent field, String message) {
            truongLoi = field;
            field.putClientProperty("JComponent.outline", "error");
            return new IllegalArgumentException(message);
        }

        Voucher docDuLieu() {
            for (JComponent field : new JComponent[]{txtTen, txtGiaGiam, txtToiDa,
                    (JComponent) dtpBatDau.getDateEditor().getUiComponent(), (JComponent) dtpKetThuc.getDateEditor().getUiComponent()}) {
                field.putClientProperty("JComponent.outline", null);
            }
            String name = txtTen.getText().trim();
            if (name.length() < 2 || name.length() > 100) throw loi(txtTen, "Tên voucher phải có từ 2 đến 100 ký tự.");
            boolean percent = cboLoai.getSelectedIndex() == 1;
            BigDecimal value = docSo(txtGiaGiam, "Giá trị giảm", false);
            if (value.signum() <= 0 || (percent && value.compareTo(BigDecimal.valueOf(100)) > 0)) {
                throw loi(txtGiaGiam, percent ? "Phần trăm giảm phải lớn hơn 0 và không quá 100." : "Giá trị giảm phải lớn hơn 0.");
            }
            BigDecimal cap = percent ? docSo(txtToiDa, "Giảm tối đa", true) : BigDecimal.ZERO;
            LocalDate start = docNgay(dtpBatDau, "Ngày bắt đầu"), end = docNgay(dtpKetThuc, "Ngày kết thúc");
            if (!end.isAfter(start)) throw loi((JComponent) dtpKetThuc.getDateEditor().getUiComponent(), "Ngày kết thúc phải sau ngày bắt đầu.");
            return new Voucher(banDau == null ? null : banDau.getMaVoucher(), name, percent,
                    value.doubleValue(), cap.doubleValue(), start, end, chkHoatDong.isSelected());
        }

        private boolean kiemTra() {
            try { docDuLieu(); lblLoi.setText(" "); return true; }
            catch (IllegalArgumentException ex) { lblLoi.setText(ex.getMessage()); return false; }
        }

        private void batNhap(boolean enabled) {
            boolean edit = enabled && !chiDoc;
            for (JComponent field : new JComponent[]{txtTen, txtGiaGiam, cboLoai, dtpBatDau, dtpKetThuc, chkHoatDong}) field.setEnabled(edit);
            txtToiDa.setEnabled(edit && cboLoai.getSelectedIndex() == 1);
            btnLuu.setEnabled(edit); btnDong.setEnabled(enabled);
        }

        void yeuCauDong() { if (!dangLuu) dong.run(); }

        private void luu() {
            if (chiDoc || dangLuu) return;
            if (!kiemTra()) { if (truongLoi != null) truongLoi.requestFocusInWindow(); return; }
            Voucher value = docDuLieu();
            dangLuu = true; batNhap(false); lblLoi.setText("Đang lưu voucher...");
            new SwingWorker<Boolean, Void>() {
                @Override protected Boolean doInBackground() { return luu.apply(value); }
                @Override protected void done() {
                    boolean success = false;
                    try {
                        if (!get()) throw new BusinessException("Voucher không còn tồn tại. Hãy tải lại danh sách.");
                        success = true; lblLoi.setText("Đã lưu voucher.");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); lblLoi.setText("Thao tác bị gián đoạn. Vui lòng tải lại để kiểm tra.");
                    } catch (ExecutionException | RuntimeException e) {
                        Throwable cause = e instanceof ExecutionException ? e.getCause() : e;
                        LOG.log(Level.WARNING, "Không thể lưu voucher", cause);
                        lblLoi.setText(cause instanceof BusinessException || cause instanceof IllegalArgumentException
                                ? cause.getMessage() : "Không thể lưu voucher. Vui lòng thử lại.");
                    } finally { dangLuu = false; batNhap(true); }
                    if (success) daLuu.accept(value);
                }
            }.execute();
        }
    }
}
