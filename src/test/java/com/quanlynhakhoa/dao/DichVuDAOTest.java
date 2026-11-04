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
import org.junit.jupiter.api.Assumptions;
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
 * UC005 thêm dịch vụ mới:
 * [x] sinh mã DV###, trạng thái đang cung cấp, loại gán theo tên, ghi nhật ký
 * [x] tên trùng dịch vụ đang hoạt động (không phân biệt hoa thường) -> chặn, không lưu
 * [x] tên trùng dịch vụ đã ngừng -> được thêm
 * [x] luôn lưu "Đang cung cấp" dù đối tượng truyền vào trangThai=false
 * [x] Lễ tân, Nha sĩ bị chặn; mã loại không tồn tại bị chặn
 * [x] gợi ý lộ trình: không trùng, bỏ lộ trình rỗng, lộ trình gắn với dịch vụ mới nhất đứng trước
 * UC007 cập nhật thông tin dịch vụ:
 * [x] đổi tên, giá, thời lượng, loại (thuế đi theo loại), lộ trình; nhật ký ghi giá cũ -> giá mới
 * [x] 4.1 không thay đổi -> "Không có thay đổi để lưu."
 * [x] 5.2 tên trùng dịch vụ khác đang hoạt động -> chặn; giữ tên của chính nó thì được
 * [x] dịch vụ đã ngừng / không tồn tại -> chặn (tiền điều kiện)
 * [x] không đổi trạng thái qua capNhat
 * [x] đổi thời lượng khi còn lịch hẹn DA_DAT -> chặn; đổi giá vẫn được (quyết định Q2)
 * [x] Lễ tân bị chặn
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
    void timKiem_locTrangThai_tachDangCungCapVaNgungCungCap_tatCaSapTheoMa() {
        // Dịch vụ bị ngừng được tạo TRƯỚC nên có mã nhỏ hơn.
        DichVu ngung = themDichVu(ma + " Niềng răng cũ", MucThue.THUE_10);
        DichVu dang = themDichVu(ma + " Trám răng", MucThue.THUE_0);
        assertTrue(dao.ngungCungCap(ngung.getMaDV()));

        assertEquals(List.of(dang.getMaDV()), maCua(dao.timKiem(ma, true, null)));
        assertEquals(List.of(ngung.getMaDV()), maCua(dao.timKiem(ma, false, null)));
        assertEquals(List.of(ngung.getMaDV(), dang.getMaDV()), maCua(dao.timKiem(ma, null, null)),
                "Tất cả: sắp theo mã, dịch vụ đã ngừng không bị dồn xuống cuối");
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

    // ---- UC005 thêm dịch vụ mới ----

    @Test
    void them_hopLe_sinhMaDV_dangCungCap_vaGanLoaiTheoTen() {
        // Form chỉ biết tên loại (mã null): DAO tự tìm hoặc tạo loại tương ứng.
        DichVu dv = new DichVu(null, ma + " Bọc răng sứ", 3_500_000, 90,
                "Buổi 1: mài răng\nBuổi 2: gắn sứ sau 5 ngày", true,
                new LoaiDichVu(null, LoaiDichVu.THAM_MY, MucThue.THUE_10));
        String demNhatKy = "SELECT COUNT(*) FROM NhatKyHeThong WHERE hanhDong=?";
        int nhatKyTruoc = DaoTestSupport.dem(demNhatKy, "THEM_DICH_VU");
        assertTrue(dao.them(dv));
        daTao.add(dv.getMaDV());

        assertTrue(dv.getMaDV().matches("DV\\d{3}"));
        assertTrue(dv.getLoaiDichVu().getMaLoaiDV().matches("LDV\\d{2}"));
        DichVu doc = dao.layTheoMa(dv.getMaDV());
        assertTrue(doc.isTrangThai());
        assertEquals(3_500_000, doc.getDonGia());
        assertEquals(90, doc.getThoiLuong());
        assertEquals(MucThue.THUE_10, doc.getLoaiDichVu().getMucThueGTGT());
        assertEquals("Buổi 1: mài răng\nBuổi 2: gắn sứ sau 5 ngày", doc.getLoTrinhDichVu());
        // Mã DV### được dùng lại sau khi test dọn dữ liệu, nên đếm chênh lệch thay vì theo mã.
        assertEquals(nhatKyTruoc + 1, DaoTestSupport.dem(demNhatKy, "THEM_DICH_VU"),
                "Ghi đúng một dòng nhật ký khi thêm");
    }

    @Test
    void them_tenTrungDichVuDangHoatDong_khongPhanBietHoaThuong_biChanVaKhongLuu() {
        themDichVu(ma + " Hàn răng", MucThue.THUE_0);
        DichVu trung = new DichVu(null, (ma + " HÀN RĂNG").toLowerCase(), 200_000, 30, null, true,
                loai(MucThue.THUE_0));

        BusinessException loi = assertThrows(BusinessException.class, () -> dao.them(trung));
        assertTrue(loi.getMessage().startsWith("Dịch vụ đã tồn tại"));
        assertEquals(null, trung.getMaDV());
        assertEquals(1, dao.timKiem(ma, null, null).size());
    }

    @Test
    void them_tenTrungDichVuDaNgung_vanDuocThem() {
        DichVu cu = themDichVu(ma + " Gói tẩy trắng", MucThue.THUE_10);
        assertTrue(dao.ngungCungCap(cu.getMaDV()));

        DichVu moi = themDichVu(ma + " Gói tẩy trắng", MucThue.THUE_10);
        assertTrue(moi.isTrangThai());
        assertEquals(List.of(moi.getMaDV()), maCua(dao.timKiem(ma, true, null)));
    }

    @Test
    void them_truyenTrangThaiNgung_vanLuuDangCungCap() {
        DichVu dv = new DichVu(null, ma + " Chỉnh khớp cắn", 500_000, 45, null, false, loai(MucThue.THUE_0));
        assertTrue(dao.them(dv));
        daTao.add(dv.getMaDV());

        assertTrue(dao.layTheoMa(dv.getMaDV()).isTrangThai());
    }

    @Test
    void them_leTanHoacNhaSi_biChan() {
        for (VaiTro vaiTro : new VaiTro[]{VaiTro.LETAN, VaiTro.NHASI}) {
            DaoTestSupport.dangNhap(vaiTro);
            DichVu dv = new DichVu(null, ma + " Không được thêm", 100_000, 15, null, true, loai(MucThue.THUE_0));
            assertThrows(BusinessException.class, () -> dao.them(dv));
        }
        DaoTestSupport.dangNhap(VaiTro.QUANLY);
        assertTrue(dao.timKiem(ma, null, null).isEmpty());
    }

    @Test
    void them_maLoaiKhongTonTai_biChan() {
        DichVu dv = new DichVu(null, ma + " Loại sai", 100_000, 15, null, true,
                new LoaiDichVu("LDV99", LoaiDichVu.DIEU_TRI, MucThue.THUE_0));
        BusinessException loi = assertThrows(BusinessException.class, () -> dao.them(dv));
        assertEquals("Loại dịch vụ không tồn tại.", loi.getMessage());
    }

    // ---- Gợi ý lộ trình (UC005) ----

    @Test
    void layLoTrinhDaLuu_khongTrung_boRong_moiNhatTruoc() {
        String a = ma + " Buổi 1: khám\nBuổi 2: điều trị";
        String b = ma + " Buổi 1: lấy dấu\nBuổi 2: gắn sứ";
        themDichVuCoLoTrinh(ma + " DV cũ dùng A", a);
        themDichVuCoLoTrinh(ma + " DV dùng B", b);
        themDichVuCoLoTrinh(ma + " DV không lộ trình", "   "); // toàn khoảng trắng -> entity lưu NULL
        themDichVuCoLoTrinh(ma + " DV mới dùng lại A", a);  // A được dùng lại ở dịch vụ mới nhất

        List<String> goiY = dao.layLoTrinhDaLuu();
        assertEquals(1, goiY.stream().filter(a::equals).count(), "Lộ trình trùng chỉ xuất hiện 1 lần");
        assertEquals(1, goiY.stream().filter(b::equals).count());
        assertTrue(goiY.indexOf(a) < goiY.indexOf(b), "A gắn với dịch vụ mới nhất nên đứng trước B");
        assertTrue(goiY.stream().noneMatch(s -> s == null || s.isBlank()), "Không gợi ý lộ trình rỗng");
    }

    private void themDichVuCoLoTrinh(String ten, String loTrinh) {
        DichVu dv = new DichVu(null, ten, 200_000, 30, loTrinh, true, loai(MucThue.THUE_0));
        assertTrue(dao.them(dv));
        daTao.add(dv.getMaDV());
    }

    // ---- UC007 cập nhật thông tin dịch vụ ----

    /** Bản sao của dịch vụ đã lưu để sửa từng trường, giống cách form gửi xuống DAO. */
    private DichVu banSua(DichVu goc) {
        return new DichVu(dao.layTheoMa(goc.getMaDV()));
    }

    @Test
    void capNhat_doiTenGiaThoiLuongLoaiLoTrinh_luuDung_vaGhiNhatKyChiTiet() {
        DichVu dv = themDichVu(ma + " Trám răng", MucThue.THUE_0);
        DichVu sua = banSua(dv);
        sua.setTenDV(ma + " Trám răng thẩm mỹ");
        sua.setDonGia(350_000);
        sua.setThoiLuong(45);
        sua.setLoaiDichVu(new LoaiDichVu(null, LoaiDichVu.THAM_MY, MucThue.THUE_10)); // form chỉ gửi tên loại
        sua.setLoTrinhDichVu("Buổi 1: trám\nBuổi 2: đánh bóng");

        assertTrue(dao.capNhat(sua));

        DichVu doc = dao.layTheoMa(dv.getMaDV());
        assertEquals(ma + " Trám răng thẩm mỹ", doc.getTenDV());
        assertEquals(350_000, doc.getDonGia());
        assertEquals(45, doc.getThoiLuong());
        assertEquals(MucThue.THUE_10, doc.getLoaiDichVu().getMucThueGTGT(), "Mức thuế đi theo loại mới");
        assertEquals("Buổi 1: trám\nBuổi 2: đánh bóng", doc.getLoTrinhDichVu());
        assertTrue(doc.isTrangThai());
        assertEquals(1, DaoTestSupport.dem("SELECT COUNT(*) FROM NhatKyHeThong WHERE hanhDong='CAP_NHAT_DICH_VU'"
                + " AND noiDungThayDoi LIKE ?", dv.getMaDV() + ": %" + ma + " Trám răng thẩm mỹ%đơn giá 300000 -> 350000%"),
                "Nhật ký ghi rõ giá cũ -> giá mới (lọc theo tên mới riêng của lần chạy vì mã DV được dùng lại)");
    }

    @Test
    void capNhat_khongThayDoiGi_baoKhongCoThayDoi() {
        DichVu dv = themDichVu(ma + " Khám định kỳ", MucThue.THUE_0);

        BusinessException loi = assertThrows(BusinessException.class, () -> dao.capNhat(banSua(dv)));
        assertEquals("Không có thay đổi để lưu.", loi.getMessage());
    }

    @Test
    void capNhat_tenTrungDichVuKhacDangHoatDong_biChan_giuTenCuThiDuoc() {
        themDichVu(ma + " Lấy cao răng", MucThue.THUE_0);
        DichVu dv = themDichVu(ma + " Đánh bóng", MucThue.THUE_0);

        DichVu trung = banSua(dv);
        trung.setTenDV(ma + " LẤY CAO RĂNG");
        assertEquals("Tên dịch vụ đã tồn tại.",
                assertThrows(BusinessException.class, () -> dao.capNhat(trung)).getMessage());

        // Giữ nguyên tên của chính nó, chỉ đổi giá: không bị coi là trùng với bản thân.
        DichVu doiGia = banSua(dv);
        doiGia.setDonGia(120_000);
        assertTrue(dao.capNhat(doiGia));
    }

    @Test
    void capNhat_dichVuDaNgungHoacKhongTonTai_biChan() {
        DichVu dv = themDichVu(ma + " Gói cũ", MucThue.THUE_0);
        DichVu sua = banSua(dv);
        assertTrue(dao.ngungCungCap(dv.getMaDV()));
        sua.setDonGia(999_000);

        assertTrue(assertThrows(BusinessException.class, () -> dao.capNhat(sua)).getMessage()
                .startsWith("Dịch vụ đã ngừng cung cấp"));
        assertEquals(300_000, dao.layTheoMa(dv.getMaDV()).getDonGia(), "Giá không bị đổi");

        DichVu khongCo = new DichVu("DV999", ma + " Không có", 100_000, 10, null, true, loai(MucThue.THUE_0));
        Assumptions.assumeTrue(dao.layTheoMa("DV999") == null, "DB test đã có DV999");
        assertEquals("Dịch vụ không tồn tại.",
                assertThrows(BusinessException.class, () -> dao.capNhat(khongCo)).getMessage());
    }

    @Test
    void capNhat_truyenTrangThaiNgung_khongLamNgungDichVu() {
        DichVu dv = themDichVu(ma + " Chụp phim", MucThue.THUE_0);
        DichVu sua = banSua(dv);
        sua.setTrangThai(false); // UC007 không đổi trạng thái; ngừng là việc của UC008
        sua.setDonGia(310_000);

        assertTrue(dao.capNhat(sua));
        assertTrue(dao.layTheoMa(dv.getMaDV()).isTrangThai());
    }

    @Test
    void capNhat_doiThoiLuongKhiConLichHen_biChan_doiGiaThiVanDuoc() {
        DichVu dv = themDichVu(ma + " Nhổ răng khôn", MucThue.THUE_0);
        String maKH = taoLichHenDaDat(dv.getMaDV());
        try {
            DichVu doiThoiLuong = banSua(dv);
            doiThoiLuong.setThoiLuong(60);
            assertTrue(assertThrows(BusinessException.class, () -> dao.capNhat(doiThoiLuong)).getMessage()
                    .contains("trước khi đổi thời lượng"));

            // Đổi giá không ảnh hưởng khung giờ đã xếp nên vẫn cho phép.
            DichVu doiGia = banSua(dv);
            doiGia.setDonGia(450_000);
            assertTrue(dao.capNhat(doiGia));
        } finally {
            xoaLichHenVaKhach(maKH);
        }
    }

    @Test
    void capNhat_leTan_biChan() {
        DichVu dv = themDichVu(ma + " Tư vấn", MucThue.THUE_0);
        DichVu sua = banSua(dv);
        sua.setDonGia(1);
        DaoTestSupport.dangNhap(VaiTro.LETAN);

        assertThrows(BusinessException.class, () -> dao.capNhat(sua));
        DaoTestSupport.dangNhap(VaiTro.QUANLY);
        assertEquals(300_000, dao.layTheoMa(dv.getMaDV()).getDonGia());
    }

    /**
     * Tạo trực tiếp 1 khách + 1 lịch hẹn DA_DAT dùng dịch vụ (không qua LichHenDAO của Long để test
     * không phụ thuộc module khác). Năm 2099 + phút ngẫu nhiên để không trùng lịch dữ liệu test khác.
     */
    private String taoLichHenDaDat(String maDV) {
        int so = ThreadLocalRandom.current().nextInt(90_000, 100_000);
        String maKH = "KH" + so;
        String maLH = "LH99" + String.format("%07d", ThreadLocalRandom.current().nextInt(10_000_000));
        java.time.LocalDateTime gio = java.time.LocalDateTime.of(2099, 1, 1, 8, 0)
                .plusMinutes(ThreadLocalRandom.current().nextInt(500_000) * 5L);
        try (Connection c = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO KhachHang(maKH,tenKH,gioiTinh,sdt) VALUES(?,?,1,?)")) {
                ps.setString(1, maKH);
                ps.setString(2, "Khách kiểm thử dịch vụ");
                ps.setString(3, "08" + so + "123");
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO LichHen(maLH,maKH,maNV,maDV,ngayGioHen,trangThai) VALUES(?,?,?,?,?,'DA_DAT')")) {
                ps.setString(1, maLH);
                ps.setString(2, maKH);
                ps.setString(3, DaoTestSupport.NV_NHA_SI);
                ps.setString(4, maDV);
                ps.setObject(5, gio);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không tạo được lịch hẹn test", e);
        }
        return maKH;
    }

    /** Lịch hẹn phải xóa trước khách và trước dịch vụ vì khóa ngoại. */
    private static void xoaLichHenVaKhach(String maKH) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
                PreparedStatement ps = c.prepareStatement("DELETE FROM LichHen WHERE maKH=?")) {
            ps.setString(1, maKH);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        DaoTestSupport.xoaKhachHang(maKH);
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
