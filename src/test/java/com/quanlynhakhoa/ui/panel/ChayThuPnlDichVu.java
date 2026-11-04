package com.quanlynhakhoa.ui.panel;

import com.formdev.flatlaf.FlatLightLaf;
import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.config.FlywayMigration;
import com.quanlynhakhoa.dao.DichVuDAO;
import com.quanlynhakhoa.dao.LoaiDichVuDAO;
import com.quanlynhakhoa.entity.DichVu;
import com.quanlynhakhoa.entity.LoaiDichVu;
import com.quanlynhakhoa.entity.NhanVien;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.entity.enums.MucThue;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.util.SessionManager;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Chạy thử màn hình dịch vụ (UC005–UC008) khi chưa gắn vào FrmMain. Không phải test tự động.
 *
 * <p>Dùng {@code src/test/resources/database.properties} (database có tên chứa "_Test"), bỏ qua
 * màn hình đăng nhập: vào thẳng bằng tài khoản mẫu TK001 (Quản lý). Truyền tham số
 * {@code LETAN} hoặc {@code NHASI} để thử vai trò chỉ xem. DB test trống thì nạp vài dịch vụ mẫu.</p>
 */
public final class ChayThuPnlDichVu {

    private ChayThuPnlDichVu() {
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();
        UIManager.put("defaultFont", new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        String url = DatabaseConnection.getInstance().getDataSource().getJdbcUrl();
        if (!url.toLowerCase().contains("_test")) {
            throw new IllegalStateException("Chỉ chạy thử trên database test (tên chứa _Test): " + url);
        }
        FlywayMigration.migrate();

        VaiTro vaiTro = args.length > 0 ? VaiTro.valueOf(args[0].toUpperCase()) : VaiTro.QUANLY;
        dangNhapQuanLy();
        napDuLieuMauNeuTrong();
        dangNhap(vaiTro);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Chạy thử — Dịch vụ (" + vaiTro + ")");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new PnlDichVu());
            frame.setMinimumSize(new Dimension(1100, 680));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static void dangNhapQuanLy() {
        dangNhap(VaiTro.QUANLY);
    }

    /** Tài khoản mẫu V2: TK001 Quản lý, TK002 Nha sĩ, TK003 Lễ tân. */
    private static void dangNhap(VaiTro vaiTro) {
        String so = switch (vaiTro) {
            case QUANLY -> "1";
            case NHASI -> "2";
            case LETAN -> "3";
        };
        NhanVien nv = new NhanVien("NV00" + so, "Nhân viên chạy thử", true, "0398682230",
                "chaythu@example.com", "Chạy thử");
        SessionManager.getInstance().login(new TaiKhoan("TK00" + so, "chay.thu", "x", vaiTro, true, nv));
    }

    private static void napDuLieuMauNeuTrong() {
        DichVuDAO dichVuDAO = new DichVuDAO();
        if (!dichVuDAO.layTatCa().isEmpty()) {
            return;
        }
        LoaiDichVuDAO loaiDAO = new LoaiDichVuDAO();
        LoaiDichVu dieuTri = loai(loaiDAO, LoaiDichVu.DIEU_TRI, MucThue.THUE_0);
        LoaiDichVu vatTu = loai(loaiDAO, LoaiDichVu.VAT_TU, MucThue.THUE_5);
        LoaiDichVu thamMy = loai(loaiDAO, LoaiDichVu.THAM_MY, MucThue.THUE_10);
        dichVuDAO.them(new DichVu(null, "Khám tổng quát", 150_000, 20, null, true, dieuTri));
        dichVuDAO.them(new DichVu(null, "Cạo vôi răng", 300_000, 30, null, true, dieuTri));
        dichVuDAO.them(new DichVu(null, "Điều trị tủy", 1_500_000, 60,
                "Buổi 1: lấy tủy\nBuổi 2: trám bít ống tủy sau 7–14 ngày", true, dieuTri));
        dichVuDAO.them(new DichVu(null, "Bàn chải điện", 850_000, 5, null, true, vatTu));
        dichVuDAO.them(new DichVu(null, "Tẩy trắng răng", 2_500_000, 90, null, true, thamMy));
        DichVu cu = new DichVu(null, "Dán sứ Veneer (gói cũ)", 6_000_000, 120, null, true, thamMy);
        dichVuDAO.them(cu);
        dichVuDAO.ngungCungCap(cu.getMaDV());
    }

    private static LoaiDichVu loai(LoaiDichVuDAO dao, String ten, MucThue mucThue) {
        for (LoaiDichVu l : dao.layTatCa()) {
            if (l.getMucThueGTGT() == mucThue) {
                return l;
            }
        }
        LoaiDichVu moi = new LoaiDichVu(null, ten, mucThue);
        dao.them(moi);
        return moi;
    }
}
