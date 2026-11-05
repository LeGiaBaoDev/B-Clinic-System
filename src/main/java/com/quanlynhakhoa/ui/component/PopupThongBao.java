package com.quanlynhakhoa.ui.component;

import static com.quanlynhakhoa.ui.component.UiConstants.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Popup danh sách thông báo dùng chung cho chuông ở các màn nhân viên. */
public final class PopupThongBao {

    /** Một dòng thông báo; các trường phụ có thể null/rỗng để ẩn. */
    public record Dong(String gio, String ten, String phu, String nhan,
            Color mauNhan, Color nenNhan, Runnable hanhDong) {
    }

    private PopupThongBao() {
    }

    public static void hien(Component neo, String tieuDe, List<Dong> danhSach, String thongDiepTrong) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(BORDER));
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(SURFACE);
        panel.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel heading = new JPanel(new BorderLayout(8, 0));
        heading.setOpaque(false);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.setMaximumSize(new Dimension(340, 24));
        heading.add(label(tieuDe, bold(13), TEXT), BorderLayout.WEST);
        heading.add(pill(String.valueOf(danhSach.size()), TEAL, TEAL_LIGHT), BorderLayout.EAST);
        panel.add(heading);
        panel.add(Box.createVerticalStrut(10));

        if (danhSach.isEmpty()) {
            JLabel trong = label(thongDiepTrong, font(12), MUTED);
            trong.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(trong);
        } else {
            for (Dong dong : danhSach) {
                panel.add(taoDong(dong, popup));
                panel.add(Box.createVerticalStrut(6));
            }
        }
        popup.add(panel);
        popup.show(neo, neo.getWidth() - 344, neo.getHeight() + 6);
    }

    private static JPanel taoDong(Dong dong, JPopupMenu popup) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(new Color(0xF7F9FB));
        row.setBorder(new EmptyBorder(9, 11, 9, 11));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(340, 56));
        if (dong.hanhDong() != null) {
            row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        if (dong.gio() != null && !dong.gio().isBlank()) {
            JLabel gio = label(dong.gio(), bold(13), TEAL);
            gio.setBorder(new EmptyBorder(0, 0, 0, 4));
            row.add(gio, BorderLayout.WEST);
        }

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel ten = label(dong.ten(), bold(12), TEXT);
        ten.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(ten);
        if (dong.phu() != null && !dong.phu().isBlank()) {
            text.add(Box.createVerticalStrut(2));
            JLabel phu = label(dong.phu(), font(11), MUTED);
            phu.setAlignmentX(Component.LEFT_ALIGNMENT);
            text.add(phu);
        }
        row.add(text, BorderLayout.CENTER);

        if (dong.nhan() != null && !dong.nhan().isBlank()) {
            row.add(pill(dong.nhan(), dong.mauNhan(), dong.nenNhan()), BorderLayout.EAST);
        }

        if (dong.hanhDong() != null) {
            row.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    popup.setVisible(false);
                    dong.hanhDong().run();
                }
            });
        }
        return row;
    }

    private static JLabel label(String text, Font font, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font);
        lbl.setForeground(color);
        return lbl;
    }

    private static JLabel pill(String text, Color fg, Color bg) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            public boolean isOpaque() {
                return false;
            }
        };
        lbl.setFont(bold(10));
        lbl.setForeground(fg);
        lbl.setBorder(new EmptyBorder(4, 10, 4, 10));
        return lbl;
    }
}
