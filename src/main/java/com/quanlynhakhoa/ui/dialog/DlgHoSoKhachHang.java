package com.quanlynhakhoa.ui.dialog;

import com.quanlynhakhoa.dao.KhachHangDAO;
import com.quanlynhakhoa.entity.KhachHang;
import com.quanlynhakhoa.ui.component.LineIcon;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import static com.quanlynhakhoa.ui.component.UiConstants.BACKGROUND;
import static com.quanlynhakhoa.ui.component.UiConstants.GREEN;
import static com.quanlynhakhoa.ui.component.UiConstants.MUTED;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL_LIGHT;
import static com.quanlynhakhoa.ui.component.UiConstants.TEXT;
import static com.quanlynhakhoa.ui.component.UiConstants.bold;
import static com.quanlynhakhoa.ui.component.UiConstants.font;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.time.format.DateTimeFormatter;
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

/**
 * Hồ sơ khách hàng chỉ đọc, mở từ danh sách bệnh nhân (UC010).
 */
public class DlgHoSoKhachHang extends JDialog {

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("HH:mm · dd/MM/yyyy");
    private static final String THE_LICH = "lich";
    private static final String THE_HOSO = "hoso";
    private static final String THE_GHICHU = "ghichu";

    private final KhachHangDAO.TomTat tomTat;
    private final Runnable khiCapNhat;
    private final JLabel lblTen = new JLabel();
    private final JLabel lblMeta = new JLabel();
    private final JLabel lblEmail = new JLabel();
    private final JLabel lblDiaChi = new JLabel();
    private final JLabel lblLichGanNhat = new JLabel();
    private final JLabel lblSoLanKham = new JLabel();
    private final JLabel lblTrangThai = new JLabel();
    private final JLabel lblTrangThaiPill = new JLabel();
    private final JLabel lblLichTab = new JLabel();
    private final JLabel lblHoSoTab = new JLabel();
    private final CardLayout theTab = new CardLayout();
    private final JPanel pnlTab = new JPanel(theTab);
    private final TabButton btnLich = new TabButton("Lịch sử hẹn", THE_LICH);
    private final TabButton btnHoSo = new TabButton("Hồ sơ bệnh án", THE_HOSO);
    private final TabButton btnGhiChu = new TabButton("Ghi chú", THE_GHICHU);

    private DlgHoSoKhachHang(Window owner, KhachHangDAO.TomTat tomTat, Runnable khiCapNhat) {
        super(owner, "Hồ sơ bệnh nhân", ModalityType.MODELESS);
        this.tomTat = tomTat;
        this.khiCapNhat = khiCapNhat;
        setContentPane(taoNoiDung());
        napDuLieu();
        chonTab(THE_LICH);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setMinimumSize(new Dimension(760, 580));
        setSize(820, 640);
        setLocationRelativeTo(owner);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /**
     * Mở hồ sơ; gọi {@code khiCapNhat} khi thông tin được sửa để danh sách tải
     * lại.
     */
    public static void hien(Window owner, KhachHangDAO.TomTat tomTat, Runnable khiCapNhat) {
        new DlgHoSoKhachHang(owner, tomTat, khiCapNhat).setVisible(true);
    }

    private JPanel taoNoiDung() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(BACKGROUND);
        root.setBorder(new EmptyBorder(22, 26, 18, 26));

        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel tieuDe = new JLabel("Hồ sơ bệnh nhân");
        tieuDe.setFont(bold(21));
        tieuDe.setForeground(TEXT);
        JLabel phuDe = new JLabel("Thông tin bệnh nhân và lịch sử khám gần nhất.");
        phuDe.setFont(font(12));
        phuDe.setForeground(MUTED);
        text.add(tieuDe);
        text.add(Box.createVerticalStrut(4));
        text.add(phuDe);
        header.add(text, BorderLayout.WEST);
        lblTrangThaiPill.setFont(bold(10));
        lblTrangThaiPill.setOpaque(true);
        lblTrangThaiPill.setBorder(new EmptyBorder(6, 14, 6, 14));
        JPanel trangThai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        trangThai.setOpaque(false);
        trangThai.add(lblTrangThaiPill);
        header.add(trangThai, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JPanel benhNhan = theBenhNhan();
        benhNhan.setAlignmentX(LEFT_ALIGNMENT);
        body.add(benhNhan);
        body.add(Box.createVerticalStrut(12));

        JPanel tongHop = new JPanel(new java.awt.GridLayout(1, 3, 12, 0));
        tongHop.setOpaque(false);
        tongHop.setAlignmentX(LEFT_ALIGNMENT);
        tongHop.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));
        tongHop.add(theTomTat("Lịch hẹn gần nhất", lblLichGanNhat, LineIcon.Type.CALENDAR, TEXT));
        tongHop.add(theTomTat("Số lần khám hoàn tất", lblSoLanKham, LineIcon.Type.CHECK, GREEN));
        tongHop.add(theTomTat("Trạng thái hồ sơ", lblTrangThai, LineIcon.Type.PEOPLE, TEAL));
        body.add(tongHop);
        body.add(Box.createVerticalStrut(14));

        JPanel thanhTab = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        thanhTab.setOpaque(false);
        thanhTab.setAlignmentX(LEFT_ALIGNMENT);
        thanhTab.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        thanhTab.add(btnLich);
        thanhTab.add(btnHoSo);
        thanhTab.add(btnGhiChu);
        body.add(thanhTab);
        body.add(Box.createVerticalStrut(6));

        pnlTab.setOpaque(false);
        pnlTab.setAlignmentX(LEFT_ALIGNMENT);
        pnlTab.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        pnlTab.add(taoTheLichSu(), THE_LICH);
        pnlTab.add(taoTheHoSo(), THE_HOSO);
        pnlTab.add(taoTheGhiChu(), THE_GHICHU);
        body.add(pnlTab);
        root.add(body, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JButton dong = new JButton("Đóng");
        dong.setFont(bold(13));
        dong.setBackground(TEAL);
        dong.setForeground(Color.WHITE);
        dong.setPreferredSize(new Dimension(110, 38));
        dong.addActionListener(e -> dispose());
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        nut.setOpaque(false);
        nut.add(dong);
        footer.add(nut, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(dong);
        return root;
    }

    private JPanel theBenhNhan() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(16, 0));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));

        JLabel avatar = new JLabel(vietTat(tomTat.khachHang().getTenKH()), SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TEAL_LIGHT);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatar.setFont(bold(16));
        avatar.setForeground(TEAL);
        avatar.setPreferredSize(new Dimension(56, 56));

        JPanel cot = new JPanel();
        cot.setOpaque(false);
        cot.setLayout(new BoxLayout(cot, BoxLayout.Y_AXIS));
        lblTen.setFont(bold(20));
        lblTen.setForeground(TEXT);
        lblMeta.setFont(font(12));
        lblMeta.setForeground(MUTED);
        lblEmail.setFont(font(12));
        lblEmail.setForeground(MUTED);
        lblDiaChi.setFont(font(12));
        lblDiaChi.setForeground(MUTED);
        cot.add(lblTen);
        cot.add(Box.createVerticalStrut(5));
        cot.add(lblMeta);
        cot.add(Box.createVerticalStrut(4));
        cot.add(lblEmail);
        cot.add(Box.createVerticalStrut(4));
        cot.add(lblDiaChi);

        JButton btnSua = new JButton("Sửa thông tin", new LineIcon(LineIcon.Type.PLUS, TEAL, 15));
        btnSua.setFont(bold(12));
        btnSua.setBackground(Color.WHITE);
        btnSua.setForeground(TEXT);
        btnSua.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSua.addActionListener(e -> moSua());
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        nut.setOpaque(false);
        nut.add(btnSua);

        card.add(avatar, BorderLayout.WEST);
        card.add(cot, BorderLayout.CENTER);
        card.add(nut, BorderLayout.EAST);
        return card;
    }

    private JPanel theTomTat(String nhan, JLabel giaTri, LineIcon.Type icon, Color mau) {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 5));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));
        JPanel dong = new JPanel(new BorderLayout(8, 0));
        dong.setOpaque(false);
        JLabel lbl = new JLabel(nhan);
        lbl.setFont(font(10));
        lbl.setForeground(MUTED);
        dong.add(lbl, BorderLayout.WEST);
        dong.add(new JLabel(new LineIcon(icon, new Color(0x9FB0C2), 15)), BorderLayout.EAST);
        card.add(dong, BorderLayout.NORTH);
        giaTri.setFont(bold(15));
        giaTri.setForeground(mau);
        card.add(giaTri, BorderLayout.CENTER);
        return card;
    }

    private JPanel taoTheLichSu() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        JLabel tieuDe = new JLabel("Lịch hẹn gần nhất");
        tieuDe.setFont(bold(14));
        tieuDe.setForeground(TEXT);
        card.add(tieuDe, BorderLayout.NORTH);
        SurfacePanel dong = new SurfacePanel(new BorderLayout(12, 0));
        dong.setBorder(new EmptyBorder(12, 14, 12, 14));
        JLabel nhan = new JLabel("Thời gian");
        nhan.setFont(font(11));
        nhan.setForeground(MUTED);
        lblLichTab.setFont(bold(13));
        lblLichTab.setForeground(TEXT);
        dong.add(nhan, BorderLayout.WEST);
        dong.add(lblLichTab, BorderLayout.CENTER);
        card.add(dong, BorderLayout.CENTER);
        return card;
    }

    private JPanel taoTheHoSo() {
        SurfacePanel card = new SurfacePanel(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        JLabel tieuDe = new JLabel("Hồ sơ bệnh án điện tử");
        tieuDe.setFont(bold(14));
        tieuDe.setForeground(TEXT);
        card.add(tieuDe, BorderLayout.NORTH);
        SurfacePanel dong = new SurfacePanel(new BorderLayout(12, 0));
        dong.setBorder(new EmptyBorder(12, 14, 12, 14));
        JLabel nhan = new JLabel("Số lần khám hoàn tất");
        nhan.setFont(font(11));
        nhan.setForeground(MUTED);
        lblHoSoTab.setFont(bold(13));
        lblHoSoTab.setForeground(GREEN);
        dong.add(nhan, BorderLayout.WEST);
        dong.add(lblHoSoTab, BorderLayout.CENTER);
        card.add(dong, BorderLayout.CENTER);
        return card;
    }

    private JPanel taoTheGhiChu() {
        SurfacePanel card = new SurfacePanel(new BorderLayout());
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        JLabel note = new JLabel("Chưa có ghi chú cho bệnh nhân này.");
        note.setFont(font(12));
        note.setForeground(MUTED);
        card.add(note, BorderLayout.NORTH);
        return card;
    }

    private void napDuLieu() {
        KhachHang kh = tomTat.khachHang();
        lblTen.setText(kh.getTenKH());
        lblMeta.setText(kh.getMaKH() + "  ·  " + (kh.getNgaySinh() == null ? "—" : kh.getNgaySinh().format(NGAY))
                + "  ·  " + (kh.isGioiTinh() ? "Nam" : "Nữ") + "  ·  " + kh.getSdt());
        lblEmail.setText(kh.getEmail() == null ? "Chưa có email" : kh.getEmail());
        lblDiaChi.setText(kh.getDiaChi() == null ? "Chưa có địa chỉ" : kh.getDiaChi());
        String lich = tomTat.lichGanNhat() == null ? "Chưa có lịch hẹn" : tomTat.lichGanNhat().format(NGAY_GIO);
        lblLichGanNhat.setText(lich);
        lblLichTab.setText(lich);
        lblSoLanKham.setText(tomTat.soLanKham() + " lần");
        lblHoSoTab.setText(tomTat.soLanKham() + " lần");
        String trangThai = tomTat.trangThaiHoSo();
        for (JLabel lbl : new JLabel[]{lblTrangThai, lblTrangThaiPill}) {
            lbl.setText(trangThai);
            lbl.setOpaque(true);
            switch (trangThai) {
                case "Mới" -> {
                    lbl.setForeground(GREEN);
                    lbl.setBackground(new Color(0xE7F7EF));
                }
                case "Đang điều trị" -> {
                    lbl.setForeground(TEAL);
                    lbl.setBackground(TEAL_LIGHT);
                }
                default -> {
                    lbl.setForeground(MUTED);
                    lbl.setBackground(new Color(0xF0F2F5));
                }
            }
        }
    }

    private void chonTab(String the) {
        theTab.show(pnlTab, the);
        btnLich.datChon(THE_LICH.equals(the));
        btnHoSo.datChon(THE_HOSO.equals(the));
        btnGhiChu.datChon(THE_GHICHU.equals(the));
    }

    private void moSua() {
        try {
            KhachHang ketQua = DlgKhachHang.capNhat(this, tomTat.khachHang());
            if (ketQua != null) {
                KhachHang kh = tomTat.khachHang();
                kh.setTenKH(ketQua.getTenKH());
                kh.setSdt(ketQua.getSdt());
                kh.setEmail(ketQua.getEmail());
                kh.setDiaChi(ketQua.getDiaChi());
                kh.setGioiTinh(ketQua.isGioiTinh());
                napDuLieu();
                if (khiCapNhat != null) {
                    khiCapNhat.run();
                }
            }
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Không mở được form sửa thông tin: " + ex.getMessage(),
                    "Lỗi", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String vietTat(String ten) {
        String[] phan = ten.trim().split("\\s+");
        if (phan.length == 1) {
            return phan[0].substring(0, Math.min(2, phan[0].length())).toUpperCase(java.util.Locale.ROOT);
        }
        return (phan[0].charAt(0) + "" + phan[phan.length - 1].charAt(0))
                .toUpperCase(java.util.Locale.ROOT);
    }

    private final class TabButton extends JButton {

        private final String the;
        private boolean chon;

        TabButton(String text, String the) {
            super(text);
            this.the = the;
            setFont(bold(12));
            setForeground(MUTED);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9, 14, 9, 14));
            addActionListener(e -> chonTab(the));
        }

        void datChon(boolean chon) {
            this.chon = chon;
            setForeground(chon ? TEAL : MUTED);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (chon) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TEAL_LIGHT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(TEAL);
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(10, getHeight() - 3, getWidth() - 10, getHeight() - 3);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
