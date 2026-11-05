package com.quanlynhakhoa.ui.panel;

import static com.quanlynhakhoa.ui.component.UiConstants.*;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Trang "Hóa đơn" (mục Hóa đơn trên thanh nav): tiêu đề như ảnh thiết kế, bên dưới là thẻ danh sách
 * hóa đơn {@link PnlDanhSachHoaDon} (tab phân loại, tìm kiếm, bảng, Xem / Thu tiền).
 * Hàng thẻ số liệu phía trên danh sách (hóa đơn chờ thu, tổng cần thu, đã thu hôm nay) sẽ thêm sau.
 *
 * <p>Thu tiền vẫn dùng hộp thanh toán của Bảo (gọi từ thẻ danh sách), trang này không viết SQL.</p>
 */
public class PnlHoaDon extends JPanel {

    private final PnlDanhSachHoaDon pnlDanhSach;

    public PnlHoaDon() {
        this(new PnlDanhSachHoaDon());
    }

    PnlHoaDon(PnlDanhSachHoaDon pnlDanhSach) {
        super(new BorderLayout(0, 18));
        this.pnlDanhSach = pnlDanhSach;
        setName("Hóa đơn");
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 24, 24));
        add(taoTieuDe(), BorderLayout.NORTH);
        add(pnlDanhSach, BorderLayout.CENTER);
    }

    private JPanel taoTieuDe() {
        JPanel chu = new JPanel();
        chu.setOpaque(false);
        chu.setLayout(new BoxLayout(chu, BoxLayout.Y_AXIS));
        JLabel tieuDe = new JLabel("Hóa đơn chờ thanh toán");
        tieuDe.setFont(bold(24));
        tieuDe.setForeground(TEXT);
        JLabel phuDe = new JLabel("Theo dõi công nợ trong ngày và thu tiền sau điều trị.");
        phuDe.setFont(font(13));
        phuDe.setForeground(MUTED);
        chu.add(tieuDe);
        chu.add(Box.createVerticalStrut(4));
        chu.add(phuDe);
        return chu;
    }

    /** Tải lại danh sách (thanh nav gọi mỗi khi mở trang). */
    public void lamMoi() {
        pnlDanhSach.lamMoiDuLieu();
    }
}
