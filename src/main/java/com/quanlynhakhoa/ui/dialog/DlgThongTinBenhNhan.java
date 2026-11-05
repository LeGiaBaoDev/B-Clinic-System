package com.quanlynhakhoa.ui.dialog;

import static com.quanlynhakhoa.ui.component.UiConstants.BACKGROUND;
import static com.quanlynhakhoa.ui.component.UiConstants.BORDER;
import static com.quanlynhakhoa.ui.component.UiConstants.MUTED;
import static com.quanlynhakhoa.ui.component.UiConstants.TEAL;
import static com.quanlynhakhoa.ui.component.UiConstants.TEXT;
import static com.quanlynhakhoa.ui.component.UiConstants.bold;
import static com.quanlynhakhoa.ui.component.UiConstants.font;

import com.quanlynhakhoa.dao.ThongKeBenhNhanDAO.BenhNhanTrongKy;
import com.quanlynhakhoa.ui.component.SurfacePanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
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
import javax.swing.border.EmptyBorder;

public class DlgThongTinBenhNhan extends JDialog {

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private DlgThongTinBenhNhan(Window owner, BenhNhanTrongKy bn) {
        super(owner, "Thông tin bệnh nhân", ModalityType.APPLICATION_MODAL);
        setContentPane(taoNoiDung(bn));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setMinimumSize(new Dimension(560, 430));
        setSize(620, 500);
        setLocationRelativeTo(owner);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public static void hien(Window owner, BenhNhanTrongKy bn) {
        new DlgThongTinBenhNhan(owner, bn).setVisible(true);
    }

    private JPanel taoNoiDung(BenhNhanTrongKy bn) {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(BACKGROUND);
        root.setBorder(new EmptyBorder(20, 24, 16, 24));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Thông tin bệnh nhân");
        title.setFont(bold(21));
        title.setForeground(TEXT);
        JLabel sub = new JLabel("Lượt khám " + (bn.ngayKham() == null ? "—" : bn.ngayKham().format(NGAY))
                + " · Đã hoàn tất");
        sub.setFont(font(12));
        sub.setForeground(MUTED);
        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(sub);
        root.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        SurfacePanel info = new SurfacePanel(new GridLayout(4, 2, 18, 7));
        info.setBorder(new EmptyBorder(14, 16, 14, 16));
        info.add(dong("Họ và tên", bn.tenKH()));
        info.add(dong("Mã bệnh nhân", bn.maKH()));
        info.add(dong("Ngày sinh", bn.ngaySinh() == null ? "—" : bn.ngaySinh().format(NGAY)));
        info.add(dong("Giới tính", bn.gioiTinh() ? "Nam" : "Nữ"));
        info.add(dong("Số điện thoại", bn.sdt() == null || bn.sdt().isBlank() ? "—" : bn.sdt()));
        info.add(dong("Nha sĩ", bn.nhaSi() == null ? "—" : "BS. " + bn.nhaSi()));
        info.add(dong("Mã phiếu", bn.maPK()));
        info.add(dong("Ngày khám", bn.ngayKham() == null ? "—" : bn.ngayKham().format(NGAY)));
        body.add(info);
        body.add(Box.createVerticalStrut(12));
        body.add(dongDiaChi(bn.diaChi()));
        body.add(Box.createVerticalStrut(12));
        body.add(hopBenh(bn.chanDoan()));
        root.add(body, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        JButton dong = new JButton("Đóng");
        dong.setFont(font(13));
        dong.setPreferredSize(new Dimension(100, 38));
        dong.addActionListener(e -> dispose());
        footer.add(dong);
        root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    private JPanel dong(String nhan, String giaTri) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        JLabel l = new JLabel(nhan);
        l.setFont(font(10));
        l.setForeground(MUTED);
        JLabel v = new JLabel(giaTri == null || giaTri.isBlank() ? "—" : giaTri);
        v.setFont(bold(12));
        v.setForeground(TEXT);
        p.add(l, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    private JPanel dongDiaChi(String diaChi) {
        SurfacePanel p = new SurfacePanel(new BorderLayout(0, 5));
        p.setBorder(new EmptyBorder(12, 14, 12, 14));
        JLabel l = new JLabel("Địa chỉ");
        l.setFont(font(10));
        l.setForeground(MUTED);
        JLabel v = new JLabel(diaChi == null || diaChi.isBlank() ? "—" : diaChi);
        v.setFont(font(12));
        v.setForeground(TEXT);
        p.add(l, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    private JPanel hopBenh(String chanDoan) {
        SurfacePanel p = new SurfacePanel(new BorderLayout(0, 6));
        p.setBackground(new Color(0xF6F8FA));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(12, 14, 12, 14)));
        JLabel l = new JLabel("Bệnh / Chẩn đoán");
        l.setFont(bold(11));
        l.setForeground(TEAL);
        JLabel v = new JLabel("<html><div style='width:500px'>"
                + escape(chanDoan == null || chanDoan.isBlank() ? "Chưa ghi nhận chẩn đoán." : chanDoan)
                + "</div></html>");
        v.setFont(font(12));
        v.setForeground(TEXT);
        p.add(l, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\n", "<br>");
    }
}
