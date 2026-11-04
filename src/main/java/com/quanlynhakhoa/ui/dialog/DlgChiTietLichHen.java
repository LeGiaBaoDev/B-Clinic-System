package com.quanlynhakhoa.ui.dialog;

import com.quanlynhakhoa.entity.LichHen;
import com.quanlynhakhoa.entity.enums.TrangThaiLichHen;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import static com.quanlynhakhoa.ui.component.UiConstants.BACKGROUND;
import static com.quanlynhakhoa.ui.component.UiConstants.BORDER;
import static com.quanlynhakhoa.ui.component.UiConstants.GREEN;
import static com.quanlynhakhoa.ui.component.UiConstants.MUTED;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL;
import static com.quanlynhakhoa.ui.component.UiConstants.TEXT;
import static com.quanlynhakhoa.ui.component.UiConstants.bold;
import static com.quanlynhakhoa.ui.component.UiConstants.font;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class DlgChiTietLichHen extends JDialog {

    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("HH:mm · dd/MM/yyyy");
    private static final Color RED = new Color(0xC0392B);
    private static final Color AMBER = new Color(0xB26B00);
    private static final Color ROW_LE = new Color(0xF8FAFC);

    private DlgChiTietLichHen(Window owner, LichHen lh) {
        super(owner, "Chi tiết lịch hẹn", ModalityType.APPLICATION_MODAL);
        setContentPane(taoNoiDung(lh));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setMinimumSize(new Dimension(640, 520));
        setSize(680, 580);
        setLocationRelativeTo(owner);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public static void hien(Window owner, LichHen lh) {
        new DlgChiTietLichHen(owner, lh).setVisible(true);
    }

    private JPanel taoNoiDung(LichHen lh) {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(BACKGROUND);
        root.setBorder(new EmptyBorder(22, 26, 18, 26));

        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel tieuDe = new JLabel("Chi tiết lịch hẹn");
        tieuDe.setFont(bold(21));
        tieuDe.setForeground(TEXT);
        JLabel phuDe = new JLabel("Thông tin lịch hẹn của bệnh nhân tại phòng khám.");
        phuDe.setFont(font(12));
        phuDe.setForeground(MUTED);
        text.add(tieuDe);
        text.add(Box.createVerticalStrut(4));
        text.add(phuDe);
        header.add(text, BorderLayout.WEST);
        JPanel trangThai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        trangThai.setOpaque(false);
        trangThai.add(pill(lh.getTrangThai()));
        header.add(trangThai, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        SurfacePanel card = new SurfacePanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(BORDER));

        JPanel bang = new JPanel();
        bang.setOpaque(false);
        bang.setLayout(new BoxLayout(bang, BoxLayout.Y_AXIS));
        int dong = 0;
        bang.add(taoDong(dong++, "Bệnh nhân", lh.getKhachHang().getTenKH(), bold(15), TEXT, 400));
        bang.add(taoDong(dong++, "Số điện thoại", lh.getKhachHang().getSdt(), bold(12), TEXT, 400));
        bang.add(taoDong(dong++, "Dịch vụ", lh.getDichVu().getTenDV(), bold(12), TEXT, 400));
        bang.add(taoDong(dong++, "Thời gian", lh.getNgayGioHen().format(NGAY_GIO), bold(12), TEXT, 400));
        bang.add(taoDong(dong++, "Thời lượng", lh.getDichVu().getThoiLuong() + " phút", bold(12), TEXT, 400));
        bang.add(taoDong(dong++, "Trạng thái", moTaTrangThai(lh), bold(12),
                mauTrangThai(lh.getTrangThai()), 400));
        bang.add(taoDong(dong++, "Ghi chú", ghiChu(lh), bold(12), TEXT, 400));
        card.add(bang, BorderLayout.CENTER);
        root.add(card, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JLabel note = new JLabel("Chỉ xem thông tin, không chỉnh sửa lịch hẹn.");
        note.setFont(font(10));
        note.setForeground(MUTED);
        footer.add(note, BorderLayout.WEST);
        JButton btnDong = new JButton("Đóng");
        btnDong.setFont(bold(13));
        btnDong.setBackground(TEAL);
        btnDong.setForeground(Color.WHITE);
        btnDong.setPreferredSize(new Dimension(110, 38));
        btnDong.addActionListener(e -> dispose());
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        nut.setOpaque(false);
        nut.add(btnDong);
        footer.add(nut, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnDong);
        return root;
    }

    private static JPanel taoDong(int index, String nhan, String giaTri,
            java.awt.Font fontGiaTri, Color mauGiaTri, int rongGiaTri) {
        JPanel dong = new JPanel(new BorderLayout(18, 0));
        dong.setOpaque(true);
        dong.setBackground(index % 2 == 0 ? Color.WHITE : ROW_LE);
        dong.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xEDF1F5)),
                new EmptyBorder(11, 16, 11, 16)));
        dong.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));

        JLabel lbl = new JLabel(nhan);
        lbl.setFont(font(11));
        lbl.setForeground(MUTED);
        lbl.setPreferredSize(new Dimension(140, 20));
        dong.add(lbl, BorderLayout.WEST);

        JLabel value = new JLabel("<html><div style='width:" + rongGiaTri + "px'>"
                + escape(giaTri) + "</div></html>");
        value.setFont(fontGiaTri);
        value.setForeground(mauGiaTri);
        value.setToolTipText(giaTri);
        dong.add(value, BorderLayout.CENTER);
        return dong;
    }

    private static Color mauTrangThai(TrangThaiLichHen trangThai) {
        return switch (trangThai) {
            case DA_DAT ->
                AMBER;
            case DA_DEN, HOAN_TAT ->
                GREEN;
            case DA_HUY, KHONG_DEN ->
                RED;
        };
    }

    private static JLabel pill(TrangThaiLichHen trangThai) {
        Color fg = mauTrangThai(trangThai);
        Color bg = switch (trangThai) {
            case DA_DAT ->
                new Color(0xFFF4DE);
            case DA_DEN, HOAN_TAT ->
                new Color(0xE7F7EF);
            case DA_HUY, KHONG_DEN ->
                new Color(0xFDECEA);
        };
        JLabel lbl = new JLabel(nhanTrangThai(trangThai), SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            public boolean isOpaque() {
                return false;
            }
        };
        lbl.setFont(bold(11));
        lbl.setForeground(fg);
        lbl.setBorder(new EmptyBorder(6, 14, 6, 14));
        return lbl;
    }

    private static String ghiChu(LichHen lh) {
        if (lh.getTrangThai() == TrangThaiLichHen.DA_HUY) {
            return lh.getLyDoHuy() == null ? "Đã hủy, không ghi lý do." : "Lý do hủy: " + lh.getLyDoHuy();
        }
        return lh.getGhiChu() == null ? "Không có ghi chú." : lh.getGhiChu();
    }

    private static String moTaTrangThai(LichHen lh) {
        return switch (lh.getTrangThai()) {
            case DA_DAT ->
                "Đã đặt · Chờ lễ tân xác nhận";
            case DA_DEN ->
                "Đã đến · Đang chờ khám";
            case HOAN_TAT ->
                "Đã hoàn tất khám";
            case DA_HUY ->
                "Đã hủy";
            case KHONG_DEN ->
                "Không đến";
        };
    }

    private static String nhanTrangThai(TrangThaiLichHen trangThai) {
        return switch (trangThai) {
            case DA_DAT ->
                "Đã đặt";
            case DA_DEN ->
                "Đã đến";
            case HOAN_TAT ->
                "Hoàn tất";
            case DA_HUY ->
                "Đã hủy";
            case KHONG_DEN ->
                "Không đến";
        };
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
