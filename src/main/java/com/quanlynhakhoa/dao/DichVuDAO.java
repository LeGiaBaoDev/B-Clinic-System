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

/**
 * UC005–008: danh mục, giá hiện tại; không sửa ảnh chụp giá ở chi tiết khám cũ.
 */
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

    /**
     * Danh sách chọn dịch vụ cho bên khác (đặt lịch, chỉ định): {@code timKiem("", true)}
     * chỉ trả dịch vụ đang cung cấp (UC008).
     */
    public List<DichVu> timKiem(String ten, boolean chiDangHoatDong) {
        return timKiem(ten, chiDangHoatDong ? Boolean.TRUE : null, null);
    }

    /**
     * UC006: tra cứu danh mục theo tên hoặc mã, lọc trạng thái và phân loại thuế.
     *
     * @param tuKhoa    một phần tên hoặc mã dịch vụ; rỗng/null = tất cả
     * @param trangThai true = đang cung cấp, false = ngừng cung cấp, null = tất cả
     * @param mucThue   mức thuế GTGT của loại dịch vụ; null = tất cả
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

    /** Mẫu LIKE coi %, _, [ người dùng gõ là ký tự thường. */
    private static String mauTimKiem(String tuKhoa) {
        String text = tuKhoa == null ? "" : tuKhoa.trim();
        return "%" + text.replace("\\", "\\\\").replace("%", "\\%")
                .replace("_", "\\_").replace("[", "\\[") + "%";
    }

    /** Số lộ trình tối đa nạp lên để gợi ý; form lọc trên máy và chỉ hiện 5 cái đầu. */
    static final int SO_LO_TRINH_GOI_Y_TOI_DA = 200;

    /**
     * UC005/UC007: các lộ trình dịch vụ khác nhau đã lưu (kể cả của dịch vụ đã ngừng), mới nhất trước,
     * để form gợi ý khi nhập "Lộ trình dịch vụ".
     *
     * <p>Bảng DichVu không có cột thời gian tạo. Mã DV### được sinh tăng dần khi thêm và dịch vụ không
     * bao giờ bị xóa cứng, nên mã lớn hơn = tạo sau (quyết định của Dương 04/11). Một lộ trình dùng cho
     * nhiều dịch vụ thì lấy mã lớn nhất của nó để xếp hạng. Lộ trình rỗng/toàn khoảng trắng bị bỏ qua.</p>
     *
     * @return danh sách lộ trình (không trùng), mới nhất trước, tối đa {@value #SO_LO_TRINH_GOI_Y_TOI_DA}
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

    /*
     * Thứ tự khóa ứng dụng khi ghi dịch vụ (giữ cố định để không deadlock với LichHenDAO):
     *   "BClinic:lich" -> "BClinic:dich-vu" -> (LoaiDichVuDAO) "BClinic:loai-dich-vu" -> MaSinh.
     * Khóa "BClinic:lich" chặn việc đặt lịch hẹn mới trong lúc đang đổi thời lượng dịch vụ.
     */

    /**
     * UC005: thêm dịch vụ mới, sinh mã DV###, trạng thái "Đang cung cấp". Loại chỉ cần đúng tên
     * (mã có thể null): DB chưa có loại thì tạo theo 3 loại cố định của Bảng 4.5. Chỉ Quản lý.
     *
     * @return true nếu đã lưu; sau khi lưu, {@code dv} được gán mã mới và loại đã lưu
     * @throws BusinessException tên trùng dịch vụ đang hoạt động, không có quyền, loại không tồn tại
     */
    public boolean them(DichVu dv) {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            kiemTraDuLieu(dv);
            MaSinh.khoa(c, "BClinic:lich");
            MaSinh.khoa(c, "BClinic:dich-vu");

            // UC005 bước 5 / luồng 5.1: tên không trùng dịch vụ đang hoạt động.
            kiemTraTenKhongTrung(c, dv.getTenDV(), null, "Dịch vụ đã tồn tại. Vui lòng đổi tên dịch vụ.");
            // UC005 bước 6: mức thuế đi theo phân loại đã chọn (lấy hoặc tạo loại trong cùng transaction).
            LoaiDichVu loai = new LoaiDichVuDAO().layHoacTao(c, dv.getLoaiDichVu());

            // UC005 bước 7: sinh mã, lưu với trạng thái "Đang cung cấp" (bỏ qua trangThai bên gọi truyền vào).
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
     * UC007: cập nhật tên, đơn giá, thời lượng, phân loại thuế, lộ trình của dịch vụ ĐANG cung cấp.
     * Chỉ Quản lý. Không đổi trạng thái ở đây (ngừng / mở lại thuộc UC008).
     *
     * <p>Dữ liệu lịch sử không bị ảnh hưởng: mỗi dòng ChiTietPhieuKham tự lưu donGia, mucThueGTGT
     * tại lúc chỉ định (ChiTietPhieuKhamDAO của Bảo), nên đổi giá ở đây chỉ áp dụng cho lượt mới.</p>
     *
     * @param dv dịch vụ đã sửa, phải có {@code maDV} của dịch vụ cần cập nhật
     * @return true nếu đã lưu
     * @throws BusinessException dịch vụ không tồn tại / đã ngừng, không có thay đổi, tên trùng,
     *                           đổi thời lượng khi còn lịch hẹn chưa xử lý, không có quyền
     */
    public boolean capNhat(DichVu dv) {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            kiemTraDuLieu(dv);
            yeuCau(dv.getMaDV() != null, "Chưa chọn dịch vụ cần cập nhật.");
            MaSinh.khoa(c, "BClinic:lich");
            MaSinh.khoa(c, "BClinic:dich-vu");

            // Đọc lại bản đang lưu trong DB (không tin dữ liệu cũ mà UI đang giữ).
            DichVu cu = layTheoMa(c, dv.getMaDV());
            yeuCau(cu != null, "Dịch vụ không tồn tại.");
            // UC007 tiền điều kiện: chỉ sửa dịch vụ đang cung cấp.
            yeuCau(cu.isTrangThai(),
                    "Dịch vụ đã ngừng cung cấp. Hãy mở lại cung cấp trước khi cập nhật.");

            // UC007 luồng 4.1: không thay đổi trường nào -> báo, không ghi DB.
            String thayDoi = moTaThayDoi(cu, dv);
            yeuCau(!thayDoi.isEmpty(), "Không có thay đổi để lưu.");

            // Ràng buộc của nhóm (giữ theo quyết định Q2): lịch hẹn đã đặt/đã đến được xếp theo
            // thời lượng cũ, đổi thời lượng lúc này có thể làm chồng lịch -> buộc xử lý lịch trước.
            if (cu.getThoiLuong() != dv.getThoiLuong()) {
                yeuCau(!conLichHenChuaXuLy(c, dv.getMaDV()),
                        "Dịch vụ còn lịch hẹn đã đặt hoặc đang khám. Cần xử lý các lịch hẹn đó trước khi đổi thời lượng.");
            }
            // UC007 bước 5 / luồng 5.2: tên không trùng dịch vụ KHÁC đang hoạt động.
            kiemTraTenKhongTrung(c, dv.getTenDV(), dv.getMaDV(), "Tên dịch vụ đã tồn tại.");
            LoaiDichVu loai = new LoaiDichVuDAO().layHoacTao(c, dv.getLoaiDichVu());

            // UC007 bước 6: chỉ cập nhật bảng DichVu, không đụng phiếu khám/hóa đơn cũ.
            int rows = JdbcHelper.update(c, "UPDATE DichVu SET tenDV=?,maLoaiDV=?,donGia=?,thoiLuong=?,"
                    + "loTrinhDichVu=? WHERE maDV=? AND trangThai=1",
                    dv.getTenDV(), loai.getMaLoaiDV(), BigDecimal.valueOf(dv.getDonGia()),
                    dv.getThoiLuong(), dv.getLoTrinhDichVu(), dv.getMaDV());
            if (rows == 1) {
                dv.setTrangThai(true);
                dv.setLoaiDichVu(loai);
                // Nhật ký ghi rõ trường nào đổi từ giá trị nào sang giá trị nào, để tra soát sau này.
                NhatKy.ghi(c, "CAP_NHAT_DICH_VU", dv.getMaDV() + ": " + thayDoi);
            }
            return rows == 1;
        });
    }

    /** Chạy lại validate của entity (Bảng 4.6) phòng trường hợp bên gọi tạo DichVu rỗng rồi set thiếu. */
    private static void kiemTraDuLieu(DichVu dv) {
        new DichVu(dv.getMaDV(), dv.getTenDV(), dv.getDonGia(), dv.getThoiLuong(),
                dv.getLoTrinhDichVu(), true, dv.getLoaiDichVu());
    }

    /**
     * Tên không trùng dịch vụ đang hoạt động khác. Collation Vietnamese_CI_AS nên "Cạo vôi" và
     * "cạo VÔI" bị coi là trùng. UPDLOCK,HOLDLOCK giữ khóa đến hết transaction để hai người lưu
     * cùng lúc không cùng lọt qua bước kiểm tra.
     *
     * @param maBoQua mã dịch vụ đang sửa (null khi thêm mới)
     */
    private static void kiemTraTenKhongTrung(Connection c, String ten, String maBoQua, String thongBao)
            throws SQLException {
        yeuCau(!JdbcHelper.exists(c,
                "SELECT 1 FROM DichVu WITH(UPDLOCK,HOLDLOCK) WHERE trangThai=1 AND tenDV=? AND maDV<>?",
                ten, maBoQua == null ? "" : maBoQua), thongBao);
    }

    /** Lịch hẹn đã đặt hoặc khách đã đến (đang chờ/đang khám) còn dùng dịch vụ này. */
    private static boolean conLichHenChuaXuLy(Connection c, String maDV) throws SQLException {
        return JdbcHelper.exists(c, "SELECT 1 FROM LichHen WHERE maDV=? AND trangThai IN('DA_DAT','DA_DEN')",
                maDV);
    }

    /**
     * So sánh bản cũ và bản mới; trả mô tả các trường thay đổi (rỗng nếu không đổi gì).
     * Loại dịch vụ so theo tên vì form chỉ biết tên loại (mã có thể null).
     * Hàm thuần (không truy cập DB), DlgDichVu dùng lại để báo "Không có thay đổi" ngay trên form.
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
     * UC008: ngừng cung cấp dịch vụ (không xóa cứng). Chỉ Quản lý.
     *
     * <p>Sau khi ngừng, dịch vụ không còn trong danh sách chọn khi đặt lịch / chỉ định
     * ({@code timKiem("", true)}); phiếu khám, hóa đơn cũ vẫn giữ dịch vụ và giá lúc phát sinh.
     * Người thực hiện + thời điểm được ghi vào nhật ký (UC008 bước 5).</p>
     *
     * @return true nếu đã ngừng
     * @throws BusinessException dịch vụ không tồn tại / đã ngừng sẵn, còn lịch hẹn chưa xử lý, không có quyền
     */
    public boolean ngungCungCap(String ma) {
        return JdbcHelper.transaction(c -> {
            PhanQuyen.quyen(c, VaiTro.QUANLY);
            // Khóa "lịch" trước để không ai đặt lịch hẹn mới cho dịch vụ này trong lúc đang ngừng.
            MaSinh.khoa(c, "BClinic:lich");
            MaSinh.khoa(c, "BClinic:dich-vu");
            DichVu dv = layTheoMa(c, ma);
            yeuCau(dv != null, "Dịch vụ không tồn tại.");
            // UC008 tiền điều kiện: chỉ ngừng dịch vụ đang cung cấp.
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
     * UC008 luồng 2.1–2.2: mở lại cung cấp dịch vụ đang ngừng. Chỉ Quản lý.
     *
     * <p>Vì UC005 cho phép thêm dịch vụ mới trùng tên với dịch vụ đã ngừng, khi mở lại phải kiểm tra
     * tên không trùng với dịch vụ KHÁC đang cung cấp (giữ đúng quy tắc tên của UC005/UC007).</p>
     *
     * @return true nếu đã mở lại
     * @throws BusinessException dịch vụ không tồn tại / đang cung cấp sẵn, trùng tên, không có quyền
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

    /** Giữ tên chuẩn của DAO (NAMING_CONVENTIONS): "xóa" dịch vụ = ngừng cung cấp, không xóa cứng. */
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
