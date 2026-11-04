package com.quanlynhakhoa.dao;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.dao.support.MaSinh;
import com.quanlynhakhoa.dao.support.NhatKy;
import com.quanlynhakhoa.dao.support.PhanQuyen;
import com.quanlynhakhoa.entity.DichVu;
import com.quanlynhakhoa.entity.LoaiDichVu;
import com.quanlynhakhoa.entity.enums.MucThue;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DichVuDAO {

    private static final String SQL_SELECT = """
            SELECT dv.maDV, dv.tenDV, dv.donGia, dv.thoiLuong, dv.loTrinhDichVu, dv.trangThai,
                   ldv.maLoaiDV, ldv.tenLoaiDV, ldv.mucThueGTGT
            FROM DichVu dv
            LEFT JOIN LoaiDichVu ldv ON ldv.maLoaiDV = dv.maLoaiDV
            """;

    public List<DichVu> layTatCa() {
        return timKiem("", false);
    }

    
    public List<DichVu> timKiem(String ten, boolean chiDangHoatDong) {
        return timKiem(ten, chiDangHoatDong ? Boolean.TRUE : null, null);
    }

    /**
     * tra cứu danh mục theo tên hoặc mã, lọc trạng thái và phân loại thuế.
     *
     * @param tuKhoa một phần tên hoặc mã dịch vụ; rỗng/null = tất cả
     * @param trangThai true = đang cung cấp, false = ngừng cung cấp, null = tất
     * cả
     * @param mucThue mức thuế GTGT của loại dịch vụ; null = tất cả
     */
    public List<DichVu> timKiem(String tuKhoa, Boolean trangThai, MucThue mucThue) {
        StringBuilder sql = new StringBuilder(SQL_SELECT)
                .append(" WHERE (dv.tenDV LIKE ? ESCAPE '\\' OR dv.maDV LIKE ? ESCAPE '\\')");
        List<Object> thamSo = new ArrayList<>();
        String mau = mauTimKiem(tuKhoa);
        thamSo.add(mau);
        thamSo.add(mau);
        // Chỉ ghép đoạn SQL cố định; giá trị lọc luôn đi qua tham số.
        if (trangThai != null) {
            sql.append(" AND dv.trangThai=?");
            thamSo.add(trangThai);
        }
        if (mucThue != null) {
            sql.append(" AND ldv.mucThueGTGT=?");
            thamSo.add(LoaiDichVuDAO.thue(mucThue));
        }
        // Sắp theo mã để dịch vụ đã ngừng nằm đúng vị trí, không bị dồn xuống trang cuối khi xem "Tất cả".
        sql.append(" ORDER BY dv.maDV");
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c);
            return JdbcHelper.query(c, sql.toString(), this::mapRow, thamSo.toArray());
        });
    }

    /**
     * Mẫu LIKE coi %, _, [ người dùng gõ là ký tự thường.
     */
    private static String mauTimKiem(String tuKhoa) {
        String text = tuKhoa == null ? "" : tuKhoa.trim();
        return "%" + text.replace("\\", "\\\\").replace("%", "\\%")
                .replace("_", "\\_").replace("[", "\\[") + "%";
    }

    /**
     * Số lộ trình tối đa nạp lên để gợi ý; form lọc trên máy và chỉ hiện 5 cái
     * đầu.
     */
    static final int SO_LO_TRINH_GOI_Y_TOI_DA = 200;

    /**
     * @return danh sách lộ trình (không trùng), mới nhất trước, tối đa
     * {@value #SO_LO_TRINH_GOI_Y_TOI_DA}
     */
    public List<String> layLoTrinhDaLuu() {
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c);
            return JdbcHelper.query(c, "SELECT TOP (" + SO_LO_TRINH_GOI_Y_TOI_DA + ") loTrinhDichVu FROM DichVu"
                    + " WHERE loTrinhDichVu IS NOT NULL AND LEN(LTRIM(RTRIM(loTrinhDichVu))) > 0"
                    + " GROUP BY loTrinhDichVu ORDER BY MAX(maDV) DESC",
                    r -> r.getString(1));
        });
    }

    public DichVu layTheoMa(String ma) {
        return JdbcHelper.doc(c -> {
            PhanQuyen.quyen(c);
            return layTheoMa(c, ma);
        });
    }

    DichVu layTheoMa(Connection c, String ma) throws SQLException {
        return JdbcHelper.one(c, SQL_SELECT + " WHERE dv.maDV=?", this::mapRow, ma);
    }

    public boolean them(DichVu dv) {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            kiemTraDuLieu(dv);
            MaSinh.khoa(c, "BClinic:lich");
            MaSinh.khoa(c, "BClinic:dich-vu");

            kiemTraTenKhongTrung(c, dv.getTenDV(), null, "Dịch vụ đã tồn tại. Vui lòng đổi tên dịch vụ.");
            LoaiDichVu loai = new LoaiDichVuDAO().layHoacTao(c, dv.getLoaiDichVu());

            String ma = MaSinh.maMoi(c, MaSinh.Bang.DICH_VU, null);
            int rows = JdbcHelper.update(c, "INSERT INTO DichVu(maDV,tenDV,maLoaiDV,donGia,thoiLuong,"
                    + "loTrinhDichVu,trangThai) VALUES(?,?,?,?,?,?,1)",
                    ma, dv.getTenDV(), loai.getMaLoaiDV(), BigDecimal.valueOf(dv.getDonGia()),
                    dv.getThoiLuong(), dv.getLoTrinhDichVu());
            if (rows == 1) {
                dv.setMaDV(ma);
                dv.setTrangThai(true);
                dv.setLoaiDichVu(loai);
                NhatKy.ghi(c, "THEM_DICH_VU", ma);
            }
            return rows == 1;
        });
    }

    /**
     * @param dv dịch vụ đã sửa, phải có {@code maDV} của dịch vụ cần cập nhật
     * @return true nếu đã lưu
     * @throws BusinessException dịch vụ không tồn tại / đã ngừng, không có thay
     * đổi, tên trùng, đổi thời lượng khi còn lịch hẹn chưa xử lý, không có
     * quyền
     */
    public boolean capNhat(DichVu dv) {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            kiemTraDuLieu(dv);
            yeuCau(dv.getMaDV() != null, "Chưa chọn dịch vụ cần cập nhật.");
            MaSinh.khoa(c, "BClinic:lich");
            MaSinh.khoa(c, "BClinic:dich-vu");

            DichVu cu = layTheoMa(c, dv.getMaDV());
            yeuCau(cu != null, "Dịch vụ không tồn tại.");
            yeuCau(cu.isTrangThai(),
                    "Dịch vụ đã ngừng cung cấp. Hãy mở lại cung cấp trước khi cập nhật.");

            String thayDoi = moTaThayDoi(cu, dv);
            yeuCau(!thayDoi.isEmpty(), "Không có thay đổi để lưu.");

            if (cu.getThoiLuong() != dv.getThoiLuong()) {
                yeuCau(!conLichHenChuaXuLy(c, dv.getMaDV()),
                        "Dịch vụ còn lịch hẹn đã đặt hoặc đang khám. Cần xử lý các lịch hẹn đó trước khi đổi thời lượng.");
            }
            kiemTraTenKhongTrung(c, dv.getTenDV(), dv.getMaDV(), "Tên dịch vụ đã tồn tại.");
            LoaiDichVu loai = new LoaiDichVuDAO().layHoacTao(c, dv.getLoaiDichVu());

            int rows = JdbcHelper.update(c, "UPDATE DichVu SET tenDV=?,maLoaiDV=?,donGia=?,thoiLuong=?,"
                    + "loTrinhDichVu=? WHERE maDV=? AND trangThai=1",
                    dv.getTenDV(), loai.getMaLoaiDV(), BigDecimal.valueOf(dv.getDonGia()),
                    dv.getThoiLuong(), dv.getLoTrinhDichVu(), dv.getMaDV());
            if (rows == 1) {
                dv.setTrangThai(true);
                dv.setLoaiDichVu(loai);
                NhatKy.ghi(c, "CAP_NHAT_DICH_VU", dv.getMaDV() + ": " + thayDoi);
            }
            return rows == 1;
        });
    }

    /**
     * Chạy lại validate của entity (Bảng 4.6) phòng trường hợp bên gọi tạo
     * DichVu rỗng rồi set thiếu.
     */
    private static void kiemTraDuLieu(DichVu dv) {
        new DichVu(dv.getMaDV(), dv.getTenDV(), dv.getDonGia(), dv.getThoiLuong(),
                dv.getLoTrinhDichVu(), true, dv.getLoaiDichVu());
    }

    /**
     * Tên không trùng dịch vụ đang hoạt động khác. Collation Vietnamese_CI_AS
     * nên "Cạo vôi" và "cạo VÔI" bị coi là trùng. UPDLOCK,HOLDLOCK giữ khóa đến
     * hết transaction để hai người lưu cùng lúc không cùng lọt qua bước kiểm
     * tra.
     *
     * @param maBoQua mã dịch vụ đang sửa (null khi thêm mới)
     */
    private static void kiemTraTenKhongTrung(Connection c, String ten, String maBoQua, String thongBao)
            throws SQLException {
        yeuCau(!JdbcHelper.exists(c,
                "SELECT 1 FROM DichVu WITH(UPDLOCK,HOLDLOCK) WHERE trangThai=1 AND tenDV=? AND maDV<>?",
                ten, maBoQua == null ? "" : maBoQua), thongBao);
    }

    /**
     * Lịch hẹn đã đặt hoặc khách đã đến (đang chờ/đang khám) còn dùng dịch vụ
     * này.
     */
    private static boolean conLichHenChuaXuLy(Connection c, String maDV) throws SQLException {
        return JdbcHelper.exists(c, "SELECT 1 FROM LichHen WHERE maDV=? AND trangThai IN('DA_DAT','DA_DEN')",
                maDV);
    }

    /**
     * So sánh bản cũ và bản mới; trả mô tả các trường thay đổi (rỗng nếu không
     * đổi gì). Loại dịch vụ so theo tên vì form chỉ biết tên loại (mã có thể
     * null). Hàm thuần (không truy cập DB), DlgDichVu dùng lại để báo "Không có
     * thay đổi" ngay trên form.
     */
    public static String moTaThayDoi(DichVu cu, DichVu moi) {
        List<String> doi = new ArrayList<>();
        if (!cu.getTenDV().equals(moi.getTenDV())) {
            doi.add("tên '" + cu.getTenDV() + "' -> '" + moi.getTenDV() + "'");
        }
        if (Double.compare(cu.getDonGia(), moi.getDonGia()) != 0) {
            // stripTrailingZeros để ghi "300000" thay vì "300000.0".
            doi.add("đơn giá " + BigDecimal.valueOf(cu.getDonGia()).stripTrailingZeros().toPlainString()
                    + " -> " + BigDecimal.valueOf(moi.getDonGia()).stripTrailingZeros().toPlainString());
        }
        if (cu.getThoiLuong() != moi.getThoiLuong()) {
            doi.add("thời lượng " + cu.getThoiLuong() + " -> " + moi.getThoiLuong() + " phút");
        }
        if (!cu.getLoaiDichVu().getTenLoaiDV().equals(moi.getLoaiDichVu().getTenLoaiDV())) {
            doi.add("phân loại '" + cu.getLoaiDichVu().getTenLoaiDV() + "' -> '"
                    + moi.getLoaiDichVu().getTenLoaiDV() + "'");
        }
        if (!java.util.Objects.equals(cu.getLoTrinhDichVu(), moi.getLoTrinhDichVu())) {
            doi.add("lộ trình");
        }
        return String.join("; ", doi);
    }

    /**
     * @return true nếu đã ngừng
     * @throws BusinessException dịch vụ không tồn tại / đã ngừng sẵn, còn lịch
     * hẹn chưa xử lý, không có quyền
     */
    public boolean ngungCungCap(String ma) {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            // Khóa "lịch" trước để không ai đặt lịch hẹn mới cho dịch vụ này trong lúc đang ngừng.
            MaSinh.khoa(c, "BClinic:lich");
            MaSinh.khoa(c, "BClinic:dich-vu");
            DichVu dv = layTheoMa(c, ma);
            yeuCau(dv != null, "Dịch vụ không tồn tại.");
            yeuCau(dv.isTrangThai(), "Dịch vụ " + ma + " đã ngừng cung cấp từ trước.");
            // Ràng buộc nhóm giữ (Q2): còn lịch đã đặt / khách đã đến thì phải xử lý lịch trước,
            // tránh lịch hẹn treo với một dịch vụ không còn cung cấp.
            yeuCau(!conLichHenChuaXuLy(c, ma),
                    "Dịch vụ còn lịch hẹn đã đặt hoặc đang khám. Cần xử lý các lịch hẹn đó trước khi ngừng cung cấp.");
            boolean ok = JdbcHelper.update(c, "UPDATE DichVu SET trangThai=0 WHERE maDV=? AND trangThai=1", ma) == 1;
            if (ok) {
                NhatKy.ghi(c, "NGUNG_DICH_VU", ma + ": " + dv.getTenDV());
            }
            return ok;
        });
    }

    /**
     * @return true nếu đã mở lại
     * @throws BusinessException dịch vụ không tồn tại / đang cung cấp sẵn,
     * trùng tên, không có quyền
     */
    public boolean moLaiCungCap(String ma) {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            MaSinh.khoa(c, "BClinic:lich");
            MaSinh.khoa(c, "BClinic:dich-vu");
            DichVu dv = layTheoMa(c, ma);
            yeuCau(dv != null, "Dịch vụ không tồn tại.");
            yeuCau(!dv.isTrangThai(), "Dịch vụ " + ma + " đang cung cấp.");
            kiemTraTenKhongTrung(c, dv.getTenDV(), ma, "Đã có dịch vụ khác đang cung cấp tên \""
                    + dv.getTenDV() + "\". Đổi tên hoặc ngừng dịch vụ đó trước khi mở lại.");
            boolean ok = JdbcHelper.update(c, "UPDATE DichVu SET trangThai=1 WHERE maDV=? AND trangThai=0", ma) == 1;
            if (ok) {
                NhatKy.ghi(c, "MO_LAI_DICH_VU", ma + ": " + dv.getTenDV());
            }
            return ok;
        });
    }

    /**
     * Giữ tên chuẩn của DAO (NAMING_CONVENTIONS): "xóa" dịch vụ = ngừng cung
     * cấp, không xóa cứng.
     */
    public boolean xoa(String ma) {
        return ngungCungCap(ma);
    }

    private DichVu mapRow(ResultSet r) throws SQLException {
        LoaiDichVu loai = null;
        if (r.getString("maLoaiDV") != null) {
            loai = new LoaiDichVu(
                    r.getString("maLoaiDV"),
                    r.getString("tenLoaiDV"),
                    mucThue(r.getInt("mucThueGTGT")));
        }
        return new DichVu(
                r.getString("maDV"),
                r.getString("tenDV"),
                r.getDouble("donGia"),
                r.getInt("thoiLuong"),
                r.getString("loTrinhDichVu"),
                r.getBoolean("trangThai"),
                loai);
    }

    private static com.quanlynhakhoa.entity.enums.MucThue mucThue(int value) {
        return switch (value) {
            case 0 ->
                com.quanlynhakhoa.entity.enums.MucThue.THUE_0;
            case 5 ->
                com.quanlynhakhoa.entity.enums.MucThue.THUE_5;
            case 10 ->
                com.quanlynhakhoa.entity.enums.MucThue.THUE_10;
            default ->
                throw new BusinessException("Mức thuế GTGT không hợp lệ: " + value);
        };
    }

    private static void yeuCau(boolean dieuKien, String message) {
        if (!dieuKien) {
            throw new BusinessException(message);
        }
    }
}
