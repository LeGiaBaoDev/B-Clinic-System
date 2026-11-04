package com.quanlynhakhoa.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.entity.HoSoBenhAn;
import com.quanlynhakhoa.entity.KhachHang;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/*
 * T401 – HoSoBenhAnDAO (hồ sơ tạo kèm khi thêm khách hàng, quan hệ 1–1) – cần database test
 * [x] hồ sơ được tạo cùng khách hàng, đọc lại theo mã hồ sơ và theo mã khách
 * [x] 1–1: không tạo được hồ sơ thứ hai cho cùng một khách
 * [x] phân quyền: Lễ tân không xem danh sách bệnh án; Quản lý xem được; nha sĩ chỉ xem bệnh nhân của mình
 */
class HoSoBenhAnDAOTest {

    private final KhachHangDAO khachHangDAO = new KhachHangDAO();
    private final HoSoBenhAnDAO dao = new HoSoBenhAnDAO();
    private final List<String> daTao = new ArrayList<>();

    @BeforeAll
    static void chuanBi() {
        DaoTestSupport.yeuCauDatabaseTest();
    }

    @BeforeEach
    void dangNhapLeTan() {
        DaoTestSupport.dangNhap(VaiTro.LETAN);
    }

    @AfterEach
    void don() {
        daTao.forEach(DaoTestSupport::xoaKhachHang);
        daTao.clear();
        DaoTestSupport.dangXuat();
    }

    private KhachHang themKhach(String tienSu) {
        String sdt = "09" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        KhachHang kh = new KhachHang(null, "Hồ Sơ Kiểm Thử", null, false, sdt, null, null);
        assertTrue(khachHangDAO.them(kh, tienSu));
        daTao.add(kh.getMaKH());
        return kh;
    }

    private HoSoBenhAn hoSoCua(KhachHang kh) {
        return JdbcHelper.doc(c -> dao.layTheoMaKhachHang(c, kh.getMaKH()));
    }

    @Test
    void hoSo_taoCungKhach_docLaiDuocTheoMaHoSoVaTheoMaKhach() {
        KhachHang kh = themKhach("Cao huyết áp");

        HoSoBenhAn theoKhach = hoSoCua(kh);
        assertNotNull(theoKhach);
        HoSoBenhAn theoMa = JdbcHelper.doc(c -> dao.layTheoMa(c, theoKhach.getMaHSBA()));

        assertEquals(theoKhach, theoMa);
        assertEquals("Cao huyết áp", theoMa.getTienSuBenh());
        assertEquals(kh, theoMa.getKhachHang());
    }

    @Test
    void them_khachDaCoHoSo_viPhamQuanHe1Den1_nemBusinessException() {
        KhachHang kh = themKhach(null);

        assertThrows(BusinessException.class, () -> dao.them(new HoSoBenhAn(null, "Hồ sơ thứ hai", kh)));

        assertEquals(1, DaoTestSupport.dem("SELECT COUNT(*) FROM HoSoBenhAn WHERE maKH=?", kh.getMaKH()));
    }

    @Test
    void layTatCa_leTan_biChan() {
        assertThrows(BusinessException.class, dao::layTatCa);
    }

    @Test
    void layTatCa_quanLy_thayHoSoVuaTao() {
        KhachHang kh = themKhach("Hen suyễn");
        HoSoBenhAn hs = hoSoCua(kh);
        DaoTestSupport.dangNhap(VaiTro.QUANLY);

        assertTrue(dao.layTatCa().contains(hs));
        assertEquals(hs, dao.layTheoMa(hs.getMaHSBA()));
    }

    @Test
    void layTheoMa_quanLy_maKhongTonTai_traVeNull() {
        DaoTestSupport.dangNhap(VaiTro.QUANLY);

        assertNull(dao.layTheoMa("HS99999"));
    }

    @Test
    void layTheoMa_nhaSi_benhNhanChuaPhanCong_biChan() {
        KhachHang kh = themKhach(null);
        HoSoBenhAn hs = hoSoCua(kh);
        DaoTestSupport.dangNhap(VaiTro.NHASI);

        BusinessException ex = assertThrows(BusinessException.class, () -> dao.layTheoMa(hs.getMaHSBA()));

        assertEquals("Bạn không được xem bệnh án này.", ex.getMessage());
    }
}
