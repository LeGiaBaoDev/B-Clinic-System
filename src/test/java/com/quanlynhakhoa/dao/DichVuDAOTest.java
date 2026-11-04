package com.quanlynhakhoa.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.entity.DichVu;
import com.quanlynhakhoa.entity.LoaiDichVu;
import com.quanlynhakhoa.entity.enums.MucThue;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/*
 * T201 – DichVuDAO (cần database test, xem DaoTestSupport)
 * UC006 xem danh mục dịch vụ:
 * [x] tìm theo một phần tên hoặc mã
 * [x] lọc trạng thái: đang cung cấp / ngừng cung cấp / tất cả
 * [x] lọc phân loại thuế (KCT, 5%, 10%)
 * [x] không có kết quả -> danh sách rỗng
 * [x] ký tự %, _ trong từ khóa được coi là chữ thường
 * [x] hàm cũ timKiem(ten, true) vẫn chỉ trả dịch vụ đang cung cấp (Long, Bảo đang dùng)
 * [x] Lễ tân, Nha sĩ được xem; chưa đăng nhập bị chặn
 * [x] layTheoMa trả đúng loại và mức thuế
 */
class DichVuDAOTest {

    private final DichVuDAO dao = new DichVuDAO();
    private final LoaiDichVuDAO loaiDao = new LoaiDichVuDAO();
    private final List<String> daTao = new ArrayList<>();
    /** Tiền tố riêng mỗi lần chạy để không lẫn dữ liệu khác trong DB test. */
    private String ma;

    @BeforeAll
    static void chuanBi() {
        DaoTestSupport.yeuCauDatabaseTest();
    }

    @BeforeEach
    void dangNhapQuanLy() {
        DaoTestSupport.dangNhap(VaiTro.QUANLY);
        ma = "KT" + ThreadLocalRandom.current().nextInt(100_000, 1_000_000);
    }

    @AfterEach
    void don() {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
                PreparedStatement ps = c.prepareStatement("DELETE FROM DichVu WHERE maDV=?")) {
            for (String maDV : daTao) {
                ps.setString(1, maDV);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không dọn được dịch vụ test " + daTao, e);
        } finally {
            daTao.clear();
            DaoTestSupport.dangXuat();
        }
    }

    /** Lấy loại theo mức thuế; DB test trống thì tạo (3 loại cố định theo Bảng 4.5). */
    private LoaiDichVu loai(MucThue mucThue) {
        for (LoaiDichVu l : loaiDao.layTatCa()) {
            if (l.getMucThueGTGT() == mucThue) {
                return l;
            }
        }
        String ten = switch (mucThue) {
            case THUE_0 -> LoaiDichVu.DIEU_TRI;
            case THUE_5 -> LoaiDichVu.VAT_TU;
            case THUE_10 -> LoaiDichVu.THAM_MY;
        };
        LoaiDichVu moi = new LoaiDichVu(null, ten, mucThue);
        assertTrue(loaiDao.them(moi));
        return moi;
    }

    private DichVu themDichVu(String ten, MucThue mucThue) {
        DichVu dv = new DichVu(null, ten, 300_000, 30, null, true, loai(mucThue));
        assertTrue(dao.them(dv));
        daTao.add(dv.getMaDV());
        return dv;
    }

    private static List<String> maCua(List<DichVu> ds) {
        return ds.stream().map(DichVu::getMaDV).toList();
    }

    // ---- UC006 xem danh mục dịch vụ ----

    @Test
    void timKiem_theoMotPhanTenHoacMa_traDungDichVu() {
        DichVu cao = themDichVu(ma + " Cạo vôi răng", MucThue.THUE_0);
        DichVu tay = themDichVu(ma + " Tẩy trắng răng", MucThue.THUE_10);

        assertEquals(List.of(cao.getMaDV(), tay.getMaDV()), maCua(dao.timKiem(ma, null, null)));
        assertEquals(List.of(tay.getMaDV()), maCua(dao.timKiem(ma + " tẩy", null, null)),
                "Tìm không phân biệt hoa thường");
        assertEquals(List.of(cao.getMaDV()), maCua(dao.timKiem(cao.getMaDV(), null, null)));
    }

    @Test
    void timKiem_locTrangThai_tachDangCungCapVaNgungCungCap() {
        DichVu dang = themDichVu(ma + " Trám răng", MucThue.THUE_0);
        DichVu ngung = themDichVu(ma + " Niềng răng cũ", MucThue.THUE_10);
        assertTrue(dao.ngungCungCap(ngung.getMaDV()));

        assertEquals(List.of(dang.getMaDV()), maCua(dao.timKiem(ma, true, null)));
        assertEquals(List.of(ngung.getMaDV()), maCua(dao.timKiem(ma, false, null)));
        assertEquals(List.of(dang.getMaDV(), ngung.getMaDV()), maCua(dao.timKiem(ma, null, null)),
                "Tất cả: dịch vụ đang cung cấp đứng trước");
    }

    @Test
    void timKiem_locPhanLoaiThue_chiTraDungMucThue() {
        DichVu kct = themDichVu(ma + " Nhổ răng", MucThue.THUE_0);
        DichVu vatTu = themDichVu(ma + " Bàn chải điện", MucThue.THUE_5);
        DichVu thamMy = themDichVu(ma + " Dán sứ", MucThue.THUE_10);

        assertEquals(List.of(kct.getMaDV()), maCua(dao.timKiem(ma, null, MucThue.THUE_0)));
        assertEquals(List.of(vatTu.getMaDV()), maCua(dao.timKiem(ma, null, MucThue.THUE_5)));
        assertEquals(List.of(thamMy.getMaDV()), maCua(dao.timKiem(ma, true, MucThue.THUE_10)));
    }

    @Test
    void timKiem_khongCoKetQua_traDanhSachRong() {
        themDichVu(ma + " Chụp X-quang", MucThue.THUE_0);

        assertTrue(dao.timKiem(ma + " không tồn tại", null, null).isEmpty());
        assertTrue(dao.timKiem(ma, false, null).isEmpty());
    }

    @Test
    void timKiem_kyTuDacBietTrongTuKhoa_khongThanhKyTuDaiDien() {
        DichVu giam = themDichVu(ma + " Gói giảm 50% cạo vôi", MucThue.THUE_0);
        themDichVu(ma + " Gói 500 lấy cao răng", MucThue.THUE_0);

        assertEquals(List.of(giam.getMaDV()), maCua(dao.timKiem(ma + " Gói giảm 50%", null, null)));
        assertTrue(dao.timKiem(ma + "_", null, null).isEmpty(), "'_' không được khớp mọi ký tự");
    }

    @Test
    void timKiemCu_chiDangHoatDong_vanChiTraDichVuDangCungCap() {
        DichVu dang = themDichVu(ma + " Khám tổng quát", MucThue.THUE_0);
        DichVu ngung = themDichVu(ma + " Gói cũ", MucThue.THUE_0);
        assertTrue(dao.ngungCungCap(ngung.getMaDV()));

        assertEquals(List.of(dang.getMaDV()), maCua(dao.timKiem(ma, true)));
        assertEquals(2, dao.timKiem(ma, false).size());
    }

    @Test
    void timKiem_leTanVaNhaSi_duocXemThamKhao() {
        DichVu dv = themDichVu(ma + " Điều trị tủy", MucThue.THUE_0);

        DaoTestSupport.dangNhap(VaiTro.LETAN);
        assertEquals(List.of(dv.getMaDV()), maCua(dao.timKiem(ma, true, null)));
        DaoTestSupport.dangNhap(VaiTro.NHASI);
        assertEquals(List.of(dv.getMaDV()), maCua(dao.timKiem(ma, true, null)));
    }

    @Test
    void timKiem_chuaDangNhap_biChan() {
        DaoTestSupport.dangXuat();
        BusinessException loi = assertThrows(BusinessException.class, () -> dao.timKiem("", null, null));
        assertEquals("Vui lòng đăng nhập.", loi.getMessage());
    }

    @Test
    void layTheoMa_traDungLoaiVaMucThue() {
        DichVu dv = themDichVu(ma + " Răng sứ", MucThue.THUE_10);

        DichVu doc = dao.layTheoMa(dv.getMaDV());
        assertNotNull(doc);
        assertEquals(dv.getTenDV(), doc.getTenDV());
        assertEquals(MucThue.THUE_10, doc.getLoaiDichVu().getMucThueGTGT());
        assertEquals(LoaiDichVu.THAM_MY, doc.getLoaiDichVu().getTenLoaiDV());
        assertEquals(300_000, doc.getDonGia());
        assertTrue(doc.isTrangThai());
        assertFalse(dv.getMaDV().isBlank());
    }
}
