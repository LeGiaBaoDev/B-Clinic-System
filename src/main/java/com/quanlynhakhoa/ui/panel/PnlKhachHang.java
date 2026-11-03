package com.quanlynhakhoa.ui.panel;

import static com.quanlynhakhoa.ui.component.UiConstants.BACKGROUND;
import static com.quanlynhakhoa.ui.component.UiConstants.MUTED;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL_LIGHT;
import static com.quanlynhakhoa.ui.component.UiConstants.TEXT;
import static com.quanlynhakhoa.ui.component.UiConstants.bold;
import static com.quanlynhakhoa.ui.component.UiConstants.font;

import com.quanlynhakhoa.dao.KhachHangDAO;
import com.quanlynhakhoa.entity.KhachHang;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import com.quanlynhakhoa.exception.DataAccessException;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import com.quanlynhakhoa.ui.dialog.DlgKhachHang;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * UC010 tra cứu bệnh nhân; mở form UC009 thêm và UC011 cập nhật khách hàng.
 *
 * <p>Lễ tân và Quản lý thêm/sửa được; Nha sĩ chỉ tra cứu (DAO tự lọc theo bệnh nhân được phân công).
 * Số điện thoại trong danh sách được che bớt, chi tiết bên phải hiện đầy đủ.
 * Panel được bàn giao cho Hiếu gắn vào menu {@code FrmMain}: {@code new PnlKhachHang(vaiTro)}.</p>
 */
public class PnlKhachHang extends JPanel {

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] COT = {"Mã KH", "Họ tên", "Ngày sinh", "Giới tính", "SĐT", "Địa chỉ"};

    private final KhachHangDAO khachHangDAO = new KhachHangDAO();
    private final boolean duocSua;
    private final List<KhachHang> dsHienThi = new ArrayList<>();

    private final JTextField txtTimKiem = new JTextField();
    private final DefaultTableModel modelKhachHang = new DefaultTableModel(COT, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tblKhachHang = new JTable(modelKhachHang);
    private final JButton btnTimKiem = new JButton("Tìm kiếm");
    private final JButton btnLamMoi = new JButton("Làm mới");
    private final JButton btnThem = new JButton("+ Thêm khách hàng");
    private final JButton btnCapNhat = new JButton("Cập nhật thông tin");
    private final JLabel lblTrangThai = new JLabel(" ");

    private final JLabel lblMa = giaTri();
    private final JLabel lblTen = giaTri();
    private final JLabel lblNgaySinh = giaTri();
    private final JLabel lblGioiTinh = giaTri();
    private final JLabel lblSdt = giaTri();
    private final JLabel lblDiaChi = giaTri();
    private final JLabel lblEmail = giaTri();

    private SwingWorker<List<KhachHang>, Void> worker;
    private String maCanChon;

    public PnlKhachHang(VaiTro vaiTro) {
        super(new BorderLayout(0, 14));
        this.duocSua = vaiTro == VaiTro.LETAN || vaiTro == VaiTro.QUANLY;
        initComponents();
        addEvents();
        taiDuLieu("");
    }

    // ---------------------------------------------------------------- giao diện

    private void initComponents() {
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(22, 24, 18, 24));

        JLabel lblTieuDe = new JLabel("Khách hàng");
        lblTieuDe.setFont(bold(20));
        lblTieuDe.setForeground(TEXT);

        txtTimKiem.setFont(font(13));
        txtTimKiem.setPreferredSize(new Dimension(320, 36));
        txtTimKiem.putClientProperty("JTextField.placeholderText", "Nhập mã bệnh nhân, họ tên hoặc số điện thoại");
        btnTimKiem.setBackground(TEAL);
        btnTimKiem.setForeground(Color.WHITE);
        btnTimKiem.setFont(bold(13));
        btnThem.setBackground(TEAL);
        btnThem.setForeground(Color.WHITE);
        btnThem.setFont(bold(13));
        btnLamMoi.setFont(font(13));
        btnThem.setVisible(duocSua);

        JPanel pnlTimKiem = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlTimKiem.setOpaque(false);
        pnlTimKiem.add(txtTimKiem);
        pnlTimKiem.add(btnTimKiem);
        pnlTimKiem.add(btnLamMoi);

        JPanel pnlDau = new JPanel(new BorderLayout(0, 10));
        pnlDau.setOpaque(false);
        JPanel hang = new JPanel(new BorderLayout());
        hang.setOpaque(false);
        hang.add(lblTieuDe, BorderLayout.WEST);
        hang.add(btnThem, BorderLayout.EAST);
        pnlDau.add(hang, BorderLayout.NORTH);
        pnlDau.add(pnlTimKiem, BorderLayout.CENTER);
        add(pnlDau, BorderLayout.NORTH);

        tblKhachHang.setRowHeight(32);
        tblKhachHang.setFont(font(13));
        tblKhachHang.getTableHeader().setFont(bold(12));
        tblKhachHang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblKhachHang.setAutoCreateRowSorter(false);
        tblKhachHang.setRowSorter(new TableRowSorter<>(modelKhachHang));
        tblKhachHang.getColumnModel().getColumn(0).setPreferredWidth(80);
        tblKhachHang.getColumnModel().getColumn(1).setPreferredWidth(180);
        tblKhachHang.getColumnModel().getColumn(2).setPreferredWidth(90);
        tblKhachHang.getColumnModel().getColumn(3).setPreferredWidth(70);
        tblKhachHang.getColumnModel().getColumn(4).setPreferredWidth(110);
        tblKhachHang.getColumnModel().getColumn(5).setPreferredWidth(200);

        JScrollPane scr = new JScrollPane(tblKhachHang);
        scr.setBorder(BorderFactory.createLineBorder(new Color(0xDCE4EE)));

        lblTrangThai.setFont(font(12));
        lblTrangThai.setForeground(MUTED);
        JPanel pnlBang = new JPanel(new BorderLayout(0, 6));
        pnlBang.setOpaque(false);
        pnlBang.add(scr, BorderLayout.CENTER);
        pnlBang.add(lblTrangThai, BorderLayout.SOUTH);

        add(pnlBang, BorderLayout.CENTER);
        add(taoChiTiet(), BorderLayout.EAST);
        hienChiTiet(null);
    }

    private JPanel taoChiTiet() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 12), 10);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.setPreferredSize(new Dimension(300, 0));

        JLabel lblTieuDe = new JLabel("Hồ sơ khách hàng");
        lblTieuDe.setFont(bold(15));
        lblTieuDe.setForeground(TEXT);
        card.add(lblTieuDe, BorderLayout.NORTH);

        JPanel luoi = new JPanel(new GridBagLayout());
        luoi.setOpaque(false);
        themChiTiet(luoi, 0, "Mã bệnh nhân", lblMa);
        themChiTiet(luoi, 1, "Họ tên", lblTen);
        themChiTiet(luoi, 2, "Ngày sinh", lblNgaySinh);
        themChiTiet(luoi, 3, "Giới tính", lblGioiTinh);
        themChiTiet(luoi, 4, "Số điện thoại", lblSdt);
        themChiTiet(luoi, 5, "Địa chỉ", lblDiaChi);
        themChiTiet(luoi, 6, "Email", lblEmail);
        JPanel gio = new JPanel(new BorderLayout());
        gio.setOpaque(false);
        gio.add(luoi, BorderLayout.NORTH);
        card.add(gio, BorderLayout.CENTER);

        btnCapNhat.setBackground(TEAL_LIGHT);
        btnCapNhat.setForeground(TEAL);
        btnCapNhat.setFont(bold(13));
        btnCapNhat.setVisible(duocSua);
        card.add(btnCapNhat, BorderLayout.SOUTH);
        return card;
    }

    private static void themChiTiet(JPanel luoi, int dong, String nhan, JLabel gia) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = dong * 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(dong == 0 ? 0 : 10, 0, 2, 0);
        JLabel lbl = new JLabel(nhan);
        lbl.setFont(font(11));
        lbl.setForeground(MUTED);
        luoi.add(lbl, c);
        c.gridy = dong * 2 + 1;
        c.insets = new Insets(0, 0, 0, 0);
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        luoi.add(gia, c);
    }

    private static JLabel giaTri() {
        JLabel lbl = new JLabel(" ");
        lbl.setFont(bold(13));
        lbl.setForeground(TEXT);
        return lbl;
    }

    // ---------------------------------------------------------------- sự kiện

    private void addEvents() {
        btnTimKiem.addActionListener(e -> taiDuLieu(txtTimKiem.getText()));
        txtTimKiem.addActionListener(e -> taiDuLieu(txtTimKiem.getText()));
        btnLamMoi.addActionListener(e -> lamMoi());
        btnThem.addActionListener(e -> moFormThem());
        btnCapNhat.addActionListener(e -> moFormCapNhat());
        tblKhachHang.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                hienChiTiet(khachDangChon());
            }
        });
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "lamMoi");
        getActionMap().put("lamMoi", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                lamMoi();
            }
        });
    }

    private void lamMoi() {
        txtTimKiem.setText("");
        taiDuLieu("");
    }

    private void moFormThem() {
        KhachHang kh = DlgKhachHang.themMoi(SwingUtilities.getWindowAncestor(this));
        if (kh != null) {
            maCanChon = kh.getMaKH();
            txtTimKiem.setText(kh.getMaKH());
            taiDuLieu(kh.getMaKH());
        }
    }

    private void moFormCapNhat() {
        KhachHang kh = khachDangChon();
        if (kh == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn khách hàng cần cập nhật.", "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        KhachHang moi = DlgKhachHang.capNhat(SwingUtilities.getWindowAncestor(this), kh);
        if (moi != null) {
            maCanChon = moi.getMaKH();
            taiDuLieu(txtTimKiem.getText());
        }
    }

    // ---------------------------------------------------------------- dữ liệu

    private void taiDuLieu(String tuKhoa) {
        if (worker != null) {
            worker.cancel(true);
        }
        datDangTai(true);
        worker = new SwingWorker<>() {
            @Override
            protected List<KhachHang> doInBackground() {
                return khachHangDAO.timKiem(tuKhoa);
            }

            @Override
            protected void done() {
                try {
                    hienThi(get(), tuKhoa);
                } catch (CancellationException ignored) {
                    return;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    baoLoi(e.getCause());
                } finally {
                    datDangTai(false);
                }
            }
        };
        worker.execute();
    }

    private void hienThi(List<KhachHang> ds, String tuKhoa) {
        dsHienThi.clear();
        dsHienThi.addAll(ds);
        modelKhachHang.setRowCount(0);
        for (KhachHang kh : ds) {
            modelKhachHang.addRow(new Object[] {
                kh.getMaKH(), kh.getTenKH(),
                kh.getNgaySinh() == null ? "" : kh.getNgaySinh().format(NGAY),
                kh.isGioiTinh() ? "Nam" : "Nữ",
                cheSdt(kh.getSdt()),
                kh.getDiaChi() == null ? "" : kh.getDiaChi()});
        }
        boolean coTuKhoa = tuKhoa != null && !tuKhoa.isBlank();
        if (ds.isEmpty()) {
            lblTrangThai.setText(coTuKhoa ? "Không tìm thấy bệnh nhân." : "Chưa có khách hàng nào.");
            hienChiTiet(null);
            return;
        }
        lblTrangThai.setText(ds.size() + " khách hàng");
        int chon = 0;
        if (maCanChon != null) {
            for (int i = 0; i < ds.size(); i++) {
                if (maCanChon.equals(ds.get(i).getMaKH())) {
                    chon = i;
                    break;
                }
            }
            maCanChon = null;
        }
        int hang = tblKhachHang.convertRowIndexToView(chon);
        tblKhachHang.getSelectionModel().setSelectionInterval(hang, hang);
        hienChiTiet(ds.get(chon));
    }

    private KhachHang khachDangChon() {
        int hang = tblKhachHang.getSelectedRow();
        if (hang < 0) {
            return null;
        }
        int goc = tblKhachHang.convertRowIndexToModel(hang);
        return goc < dsHienThi.size() ? dsHienThi.get(goc) : null;
    }

    private void hienChiTiet(KhachHang kh) {
        boolean co = kh != null;
        lblMa.setText(co ? kh.getMaKH() : " ");
        lblTen.setText(co ? kh.getTenKH() : " ");
        lblNgaySinh.setText(co && kh.getNgaySinh() != null ? kh.getNgaySinh().format(NGAY) : " ");
        lblGioiTinh.setText(co ? (kh.isGioiTinh() ? "Nam" : "Nữ") : " ");
        lblSdt.setText(co ? kh.getSdt() : " ");
        lblDiaChi.setText(co && kh.getDiaChi() != null ? kh.getDiaChi() : " ");
        lblEmail.setText(co && kh.getEmail() != null ? kh.getEmail() : " ");
        btnCapNhat.setEnabled(co);
    }

    private void datDangTai(boolean dangTai) {
        btnTimKiem.setEnabled(!dangTai);
        btnLamMoi.setEnabled(!dangTai);
        if (dangTai) {
            lblTrangThai.setText("Đang tải dữ liệu...");
        }
    }

    private void baoLoi(Throwable loi) {
        lblTrangThai.setText(" ");
        if (loi instanceof BusinessException) {
            JOptionPane.showMessageDialog(this, loi.getMessage(), "Cảnh báo", JOptionPane.WARNING_MESSAGE);
        } else if (loi instanceof DataAccessException) {
            JOptionPane.showMessageDialog(this, "Không thể tải danh sách khách hàng.", "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Đã xảy ra lỗi, vui lòng thử lại.", "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Che số điện thoại trong danh sách: 0912***678. */
    static String cheSdt(String sdt) {
        if (sdt == null || sdt.length() < 8) {
            return sdt == null ? "" : sdt;
        }
        return sdt.substring(0, 4) + "***" + sdt.substring(sdt.length() - 3);
    }
}
