package com.quanlynhakhoa.ui.dialog;

import static com.quanlynhakhoa.ui.component.UiConstants.BACKGROUND;
import static com.quanlynhakhoa.ui.component.UiConstants.MUTED;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL;
import static com.quanlynhakhoa.ui.component.UiConstants.TEXT;
import static com.quanlynhakhoa.ui.component.UiConstants.bold;
import static com.quanlynhakhoa.ui.component.UiConstants.font;

import com.quanlynhakhoa.dao.DichVuDAO;
import com.quanlynhakhoa.dao.LichHenDAO;
import com.quanlynhakhoa.entity.DichVu;
import com.quanlynhakhoa.entity.LichHen;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

/**
 * UC018 cập nhật lịch hẹn: chỉ sửa dịch vụ và ghi chú của lịch đang ở trạng thái đã đặt.
 * Bệnh nhân, nha sĩ và ngày giờ chỉ hiển thị; muốn đổi thì dùng chức năng dời lịch (UC019).
 */
public class DlgCapNhatLichHen extends JDialog {

    private final LichHen lich;
    private final LichHenDAO lichHenDAO = new LichHenDAO();

    private final JComboBox<DichVu> cboDichVu = new JComboBox<>();
    private final JTextField txtGhiChu = new JTextField();
    private final JButton btnLuu = new JButton("Lưu");
    private final JButton btnHuy = new JButton("Hủy");

    private LichHen ketQua;

    private DlgCapNhatLichHen(Window owner, LichHen lich) {
        super(owner, "Cập nhật lịch hẹn", ModalityType.APPLICATION_MODAL);
        this.lich = lich;
        khoiTao();
        addEvents();
        taiDichVu();
        pack();
        setMinimumSize(new Dimension(520, getHeight()));
        setLocationRelativeTo(owner);
    }

    /** Trả về lịch sau khi cập nhật, hoặc null nếu không lưu. */
    public static LichHen hien(Window owner, LichHen lich) {
        DlgCapNhatLichHen dlg = new DlgCapNhatLichHen(owner, Objects.requireNonNull(lich));
        dlg.setVisible(true);
        return dlg.ketQua;
    }

    private void khoiTao() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(BACKGROUND);
        root.setBorder(new EmptyBorder(20, 24, 16, 24));

        JLabel lblTieuDe = new JLabel("Cập nhật lịch hẹn " + lich.getMaLH());
        lblTieuDe.setFont(bold(18));
        lblTieuDe.setForeground(TEXT);
        root.add(lblTieuDe, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        int dong = 0;
        themDong(form, dong++, "Bệnh nhân", chiDoc(lich.getKhachHang().getMaKH() + " - "
                + lich.getKhachHang().getTenKH()));
        themDong(form, dong++, "Nha sĩ", chiDoc(lich.getNhaSi().getHoTenNV()));
        themDong(form, dong++, "Ngày giờ", chiDoc(lich.getNgayGioHen()
                .format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))));
        themDong(form, dong++, "Dịch vụ *", cboDichVu);
        txtGhiChu.setText(lich.getGhiChu() == null ? "" : lich.getGhiChu());
        themDong(form, dong++, "Ghi chú", txtGhiChu);
        JLabel lblGoiY = new JLabel("Muốn đổi ngày giờ hoặc nha sĩ, hãy dùng chức năng Dời lịch.");
        lblGoiY.setFont(font(12));
        lblGoiY.setForeground(MUTED);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = dong;
        c.gridx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 0, 0, 0);
        form.add(lblGoiY, c);
        root.add(form, BorderLayout.CENTER);

        cboDichVu.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                String chu = value instanceof DichVu d ? d.getTenDV() + " (" + d.getThoiLuong() + " phút)" : "";
                return super.getListCellRendererComponent(list, chu, index, isSelected, cellHasFocus);
            }
        });

        JPanel pnlNut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlNut.setOpaque(false);
        btnLuu.setBackground(TEAL);
        btnLuu.setForeground(Color.WHITE);
        btnLuu.setFont(bold(13));
        btnLuu.setPreferredSize(new Dimension(96, 36));
        btnHuy.setPreferredSize(new Dimension(86, 36));
        pnlNut.add(btnHuy);
        pnlNut.add(btnLuu);
        root.add(pnlNut, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(btnLuu);
    }

    private static JTextField chiDoc(String noiDung) {
        JTextField tf = new JTextField(noiDung);
        tf.setEditable(false);
        tf.setEnabled(false);
        return tf;
    }

    private void themDong(JPanel form, int dong, String nhan, Component truong) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = dong;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 0, 5, 12);
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
            tf.setPreferredSize(new Dimension(300, 32));
        }
        form.add(truong, c);
    }

    private void addEvents() {
        btnHuy.addActionListener(e -> dispose());
        btnLuu.addActionListener(e -> luu());
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void taiDichVu() {
        datDangXuLy(true);
        new SwingWorker<List<DichVu>, Void>() {
            @Override
            protected List<DichVu> doInBackground() {
                return new DichVuDAO().timKiem("", true);
            }

            @Override
            protected void done() {
                try {
                    boolean thay = false;
                    for (DichVu d : get()) {
                        cboDichVu.addItem(d);
                        if (d.getMaDV().equals(lich.getDichVu().getMaDV())) {
                            cboDichVu.setSelectedItem(d);
                            thay = true;
                        }
                    }
                    if (!thay) {
                        cboDichVu.addItem(lich.getDichVu());
                        cboDichVu.setSelectedItem(lich.getDichVu());
                    }
                } catch (Exception e) {
                    HopThoaiLoi.hien(DlgCapNhatLichHen.this, e, "Không thể tải danh sách dịch vụ.");
                } finally {
                    datDangXuLy(false);
                }
            }
        }.execute();
    }

    private void luu() {
        DichVu dv = (DichVu) cboDichVu.getSelectedItem();
        if (dv == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn dịch vụ.", "Dữ liệu chưa hợp lệ",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        LichHen moi = new LichHen(lich);
        moi.setDichVu(dv);
        moi.setGhiChu(txtGhiChu.getText());
        boolean khongDoi = Objects.equals(moi.getDichVu().getMaDV(), lich.getDichVu().getMaDV())
                && Objects.equals(moi.getGhiChu(), lich.getGhiChu());
        if (khongDoi) {
            JOptionPane.showMessageDialog(this, "Không có thay đổi nào để lưu.", "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        datDangXuLy(true);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return lichHenDAO.capNhatThongTin(moi);
            }

            @Override
            protected void done() {
                try {
                    if (Boolean.TRUE.equals(get())) {
                        ketQua = moi;
                        JOptionPane.showMessageDialog(DlgCapNhatLichHen.this, "Cập nhật lịch hẹn thành công.",
                                "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } else {
                        datDangXuLy(false);
                        JOptionPane.showMessageDialog(DlgCapNhatLichHen.this,
                                "Không thể cập nhật, vui lòng thử lại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    datDangXuLy(false);
                    HopThoaiLoi.hien(DlgCapNhatLichHen.this, e, "Không thể lưu dữ liệu, vui lòng thử lại.");
                }
            }
        }.execute();
    }

    private void datDangXuLy(boolean dang) {
        btnLuu.setEnabled(!dang);
        btnHuy.setEnabled(!dang);
        setCursor(dang ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }
}
