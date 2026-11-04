package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.dao.support.MaSinh;
import com.quanlynhakhoa.dao.support.NhatKy;
import com.quanlynhakhoa.dao.support.PhanQuyen;
import com.quanlynhakhoa.dao.support.PhanQuyen.Actor;
import com.quanlynhakhoa.entity.*;
import com.quanlynhakhoa.entity.enums.*;
import com.quanlynhakhoa.exception.BusinessException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

/**
 * Chi tiết điều trị chỉ thêm khi đang khám; không cung cấp API sửa/xóa bệnh án.
 */
public class ChiTietPhieuKhamDAO {

    private static final String SQL_SELECT = """
            SELECT maCTPK, trieuChung, chanDoan, donGia, soLuong, donViTinh,
                   mucThueGTGT, ketQuaDieuTri, maPK, maDV, maNV_NhaSi
            FROM ChiTietPhieuKham
            """;

    public List<ChiTietPhieuKham> layTheoPhieuKham(String maPK) {
        return JdbcHelper.doc(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.NHASI, VaiTro.QUANLY);
            PhieuKham pk = new PhieuKhamDAO().layTheoMa(c, maPK, true);
            yeuCau(pk != null, "Phiếu khám không tồn tại.");
            PhieuKhamDAO.kiemTraQuyen(pk, actor);
            return JdbcHelper.query(c, SQL_SELECT + " WHERE maPK=? ORDER BY maCTPK",
                    r -> mapRow(c, r), maPK);
        });
    }

    /**
     * Dữ liệu tính tiền không chứa triệu chứng/chẩn đoán, dành cho lễ tân.
     */
    public record DongThanhToan(int maCTPK, String maDV, String tenDV, String donViTinh,
            int soLuong, BigDecimal donGia, BigDecimal mucThueGTGT,
            BigDecimal thanhTien, BigDecimal tienThue) {

    }

    public List<DongThanhToan> layDongThanhToan(String maPK) {
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c, VaiTro.LETAN, VaiTro.QUANLY);
            return JdbcHelper.query(c, "SELECT ct.maCTPK,ct.maDV,dv.tenDV,ct.donViTinh,"
                    + "ct.soLuong,ct.donGia,ct.mucThueGTGT,ct.thanhTien,"
                    + "ROUND(ct.thanhTien*ct.mucThueGTGT/100,0) AS tienThue"
                    + " FROM ChiTietPhieuKham ct JOIN DichVu dv ON dv.maDV=ct.maDV"
                    + " WHERE ct.maPK=? ORDER BY ct.maCTPK",
                    r -> new DongThanhToan(r.getInt(1), r.getString(2), r.getString(3),
                            r.getString(4), r.getInt(5), r.getBigDecimal(6), r.getBigDecimal(7),
                            r.getBigDecimal(8), r.getBigDecimal(9)), maPK);
        });
    }

    public boolean them(ChiTietPhieuKham ct) {
        yeuCau(ct != null && ct.getPhieuKham() != null, "Thiếu phiếu khám.");
        return JdbcHelper.transaction(c -> {
            Actor actor = PhanQuyen.quyen(c, VaiTro.NHASI);
            MaSinh.khoa(c, "BClinic:kham");
            PhieuKham pk = new PhieuKhamDAO().layTheoMa(c, ct.getPhieuKham().getMaPK(), true);
            yeuCau(pk != null, "Phiếu khám không tồn tại.");
            PhieuKhamDAO.kiemTraQuyen(pk, actor);
            them(c, pk, ct, actor);
            NhatKy.ghi(c, "THEM_CHI_TIET_KHAM", pk.getMaPK());
            return true;
        });
    }

    void them(Connection c, PhieuKham pk, ChiTietPhieuKham ct, Actor actor) throws SQLException {
        yeuCau(pk.getTrangThai() == TrangThaiPhieuKham.DANG_KHAM,
                "Chỉ thêm điều trị khi phiếu đang khám.");
        yeuCau(ct.getDichVu() != null && ct.getSoLuong() > 0
                && ct.getDonViTinh() != null && !ct.getDonViTinh().isBlank()
                && ct.getTrieuChung() != null && !ct.getTrieuChung().isBlank()
                && ct.getChanDoan() != null && !ct.getChanDoan().isBlank(),
                "Chi tiết khám thiếu dịch vụ, triệu chứng, chẩn đoán hoặc số lượng.");

        DichVu dv = new DichVuDAO().layTheoMa(c, ct.getDichVu().getMaDV());
        yeuCau(dv != null && dv.isTrangThai(), "Dịch vụ không hoạt động.");

        NhanVien nhaSi = ct.getNhaSi() == null
                ? new NhanVienDAO().layTheoMa(c, actor.maNV())
                : ct.getNhaSi();
        yeuCau(nhaSi != null && JdbcHelper.exists(c,
                "SELECT 1 FROM TaiKhoan WHERE maNV=? AND vaiTro='NHASI' AND trangThai=1",
                nhaSi.getMaNV()), "Nha sĩ thực hiện không hợp lệ hoặc đã ngừng hoạt động.");
        int thue = LoaiDichVuDAO.thue(dv.getLoaiDichVu().getMucThueGTGT());

        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO ChiTietPhieuKham(maPK,maDV,maNV_NhaSi,trieuChung,chanDoan,"
                + "ketQuaDieuTri,donViTinh,soLuong,donGia,mucThueGTGT)"
                + " VALUES(?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            JdbcHelper.bind(ps, pk.getMaPK(), dv.getMaDV(), nhaSi.getMaNV(), ct.getTrieuChung(),
                    ct.getChanDoan(), ct.getKetQuaDieuTri(), ct.getDonViTinh(), ct.getSoLuong(),
                    BigDecimal.valueOf(dv.getDonGia()), thue);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    ct.setMaCTPK(keys.getInt(1));
                }
            }
        }
        ct.setNhaSi(nhaSi);
        ct.setDonGia(dv.getDonGia());
        ct.setMucThueGTGT(thue);
    }

    private ChiTietPhieuKham mapRow(Connection c, ResultSet r) throws SQLException {
        return new ChiTietPhieuKham(
                r.getInt("maCTPK"),
                r.getString("trieuChung"),
                r.getString("chanDoan"),
                r.getDouble("donGia"),
                r.getInt("soLuong"),
                r.getString("donViTinh"),
                r.getDouble("mucThueGTGT"),
                r.getString("ketQuaDieuTri"),
                new PhieuKhamDAO().layTheoMa(c, r.getString("maPK"), false),
                new DichVuDAO().layTheoMa(c, r.getString("maDV")),
                new NhanVienDAO().layTheoMa(c, r.getString("maNV_NhaSi")));
    }

    private static void yeuCau(boolean dieuKien, String message) {
        if (!dieuKien) {
            throw new BusinessException(message);
        }
    }
}
