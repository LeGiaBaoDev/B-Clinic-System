package com.quanlynhakhoa.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.entity.HoSoBenhAn;
import com.quanlynhakhoa.entity.KhachHang;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KhachHangDAOTest {

    private final KhachHangDAO dao = new KhachHangDAO();
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

    private static String sdtMoi() {
        return "09" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

    private KhachHang khachMoi(String ten, String sdt, LocalDate ngaySinh) {
        return new KhachHang(null, ten, ngaySinh, true, sdt, "Hà Nội", null);
    }

    private KhachHang themVaGhiNho(String ten, String sdt, LocalDate ngaySinh, String tienSu) {
        KhachHang kh = khachMoi(ten, sdt, ngaySinh);
        assertTrue(dao.them(kh, tienSu));
        daTao.add(kh.getMaKH());
        return kh;
    }

    @Test
    void them_khachMoi_sinhMaDungDinhDangVaGanVaoDoiTuong() {
        KhachHang kh = themVaGhiNho("Kiểm Thử Một", sdtMoi(), LocalDate.of(1990, 1, 15), "Dị ứng penicillin");

        assertNotNull(kh.getMaKH());
        assertTrue(kh.getMaKH().matches("KH\\d{5}"));
        KhachHang doc = dao.layTheoMa(kh.getMaKH());
        assertEquals(kh.getTenKH(), doc.getTenKH());
        assertEquals(kh.getSdt(), doc.getSdt());
        assertEquals(LocalDate.of(1990, 1, 15), doc.getNgaySinh());
    }

    @Test
    void them_khachMoi_taoDungMotHoSoVoiTienSuBenh() {
        KhachHang kh = themVaGhiNho("Kiểm Thử Hai", sdtMoi(), null, "Tiểu đường");

        assertEquals(1, DaoTestSupport.dem("SELECT COUNT(*) FROM HoSoBenhAn WHERE maKH=?", kh.getMaKH()));
        HoSoBenhAn hs = JdbcHelper.doc(c -> new HoSoBenhAnDAO().layTheoMaKhachHang(c, kh.getMaKH()));
        assertNotNull(hs);
        assertTrue(hs.getMaHSBA().matches("HS\\d{5}"));
        assertEquals("Tiểu đường", hs.getTienSuBenh());
    }

    @Test
    void them_khongNhapTienSu_hoSoCoTienSuNull() {
        KhachHang kh = themVaGhiNho("Kiểm Thử Ba", sdtMoi(), null, null);

        HoSoBenhAn hs = JdbcHelper.doc(c -> new HoSoBenhAnDAO().layTheoMaKhachHang(c, kh.getMaKH()));
        assertNotNull(hs);
        assertNull(hs.getTienSuBenh());
    }

    @Test
    void them_haiKhachLienTiep_maKhacNhau() {
        KhachHang a = themVaGhiNho("Kiểm Thử A", sdtMoi(), null, null);
        KhachHang b = themVaGhiNho("Kiểm Thử B", sdtMoi(), null, null);

        assertFalse(a.getMaKH().equals(b.getMaKH()));
    }

    @Test
    void them_sdtTrung_nemBusinessExceptionVaKhongGiuMa() {
        String sdt = sdtMoi();
        themVaGhiNho("Kiểm Thử Gốc", sdt, null, null);
        KhachHang trung = khachMoi("Kiểm Thử Trùng", sdt, null);

        assertThrows(BusinessException.class, () -> dao.them(trung, "x"));

        assertNull(trung.getMaKH());
        assertEquals(1, DaoTestSupport.dem("SELECT COUNT(*) FROM KhachHang WHERE sdt=?", sdt));
    }

    @Test
    void them_vaiTroNhaSi_biChan() {
        DaoTestSupport.dangNhap(VaiTro.NHASI);

        assertThrows(BusinessException.class, () -> dao.them(khachMoi("Không Được", sdtMoi(), null)));
    }

    @Test
    void them_chuaDangNhap_biChan() {
        DaoTestSupport.dangXuat();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dao.them(khachMoi("Chưa Đăng Nhập", sdtMoi(), null)));
        assertEquals("Vui lòng đăng nhập.", ex.getMessage());
    }

    @Test
    void them_quanLyDuocPhep() {
        DaoTestSupport.dangNhap(VaiTro.QUANLY);

        KhachHang kh = themVaGhiNho("Quản Lý Thêm", sdtMoi(), null, null);

        assertNotNull(kh.getMaKH());
    }

    // ---- R-KH-1 gợi ý trùng ----

    @Test
    void timTrung_cungSdt_traVeHoSoDaCo() {
        String sdt = sdtMoi();
        KhachHang goc = themVaGhiNho("Kiểm Thử Trùng Sdt", sdt, null, null);

        List<KhachHang> kq = dao.timTrung("Tên khác hẳn", null, sdt);

        assertTrue(kq.contains(goc));
    }

    @Test
    void timTrung_cungTenVaNgaySinh_traVeHoSoDaCo() {
        LocalDate ngaySinh = LocalDate.of(1988, 8, 8);
        KhachHang goc = themVaGhiNho("Nguyễn Kiểm Thử Giống", sdtMoi(), ngaySinh, null);

        List<KhachHang> kq = dao.timTrung("Nguyễn Kiểm Thử Giống", ngaySinh, sdtMoi());

        assertTrue(kq.contains(goc));
    }

    @Test
    void timTrung_cungTenKhacNgaySinh_khongTinhLaTrung() {
        KhachHang goc = themVaGhiNho("Lê Kiểm Thử Khác", sdtMoi(), LocalDate.of(1970, 1, 1), null);

        List<KhachHang> kq = dao.timTrung("Lê Kiểm Thử Khác", LocalDate.of(2000, 2, 2), sdtMoi());

        assertFalse(kq.contains(goc));
    }

    @Test
    void timTrung_vaiTroNhaSi_biChan() {
        DaoTestSupport.dangNhap(VaiTro.NHASI);

        assertThrows(BusinessException.class, () -> dao.timTrung("A", null, sdtMoi()));
    }

    @Test
    void timKiem_theoSdt_timThayKhach() {
        String sdt = sdtMoi();
        KhachHang kh = themVaGhiNho("Tra Cứu Sdt", sdt, null, null);

        assertTrue(dao.timKiem(sdt).contains(kh));
    }

    @Test
    void timKiem_theoMa_timThayKhach() {
        KhachHang kh = themVaGhiNho("Tra Cứu Mã", sdtMoi(), null, null);

        assertTrue(dao.timKiem(kh.getMaKH()).contains(kh));
    }

    @Test
    void timKiem_theoTen_timThayKhach() {
        KhachHang kh = themVaGhiNho("Zxqv Tra Cứu Tên", sdtMoi(), null, null);

        assertTrue(dao.timKiem("Zxqv Tra Cứu").contains(kh));
    }

    @Test
    void timKiem_tuKhoaKhongTonTai_traVeRong() {
        assertTrue(dao.timKiem("khong-ton-tai-" + System.nanoTime()).isEmpty());
    }

    @Test
    void layTatCa_chuaCacKhachDaThem() {
        KhachHang kh = themVaGhiNho("Có Trong Danh Sách", sdtMoi(), null, null);

        assertTrue(dao.layTatCa().contains(kh));
    }

    @Test
    void layTheoMa_khongTonTai_traVeNull() {
        assertNull(dao.layTheoMa("KH99999"));
    }

    @Test
    void nhaSi_chiThayBenhNhanDuocPhanCong_khachChuaCoLichKhongThay() {
        KhachHang kh = themVaGhiNho("Chưa Có Lịch Hẹn", sdtMoi(), null, null);
        DaoTestSupport.dangNhap(VaiTro.NHASI);

        assertFalse(dao.timKiem(kh.getSdt()).contains(kh));
        BusinessException ex = assertThrows(BusinessException.class, () -> dao.layTheoMa(kh.getMaKH()));
        assertEquals("Bệnh nhân chưa được phân công cho bạn.", ex.getMessage());
    }

    @Test
    void capNhat_doiSdtDiaChiEmail_luuDuocVaDocLaiDung() {
        KhachHang kh = themVaGhiNho("Cập Nhật Thông Tin", sdtMoi(), LocalDate.of(1992, 3, 3), null);
        String sdtMoi = sdtMoi();
        kh.setSdt(sdtMoi);
        kh.setDiaChi("Đà Nẵng");
        kh.setEmail("moi@example.com");

        assertTrue(dao.capNhat(kh));

        KhachHang doc = dao.layTheoMa(kh.getMaKH());
        assertEquals(sdtMoi, doc.getSdt());
        assertEquals("Đà Nẵng", doc.getDiaChi());
        assertEquals("moi@example.com", doc.getEmail());
        assertEquals(LocalDate.of(1992, 3, 3), doc.getNgaySinh());
    }

    @Test
    void capNhat_doiNgaySinh_biKhoa() {
        KhachHang kh = themVaGhiNho("Khóa Ngày Sinh", sdtMoi(), LocalDate.of(1992, 3, 3), null);
        kh.setNgaySinh(LocalDate.of(1993, 4, 4));

        BusinessException ex = assertThrows(BusinessException.class, () -> dao.capNhat(kh));

        assertEquals("Không được thay đổi ngày sinh đã lưu.", ex.getMessage());
        assertEquals(LocalDate.of(1992, 3, 3), dao.layTheoMa(kh.getMaKH()).getNgaySinh());
    }

    @Test
    void capNhat_khachKhongTonTai_nemBusinessException() {
        KhachHang khongCo = new KhachHang("KH99999", "Không Tồn Tại", null, true, sdtMoi(), null, null);

        BusinessException ex = assertThrows(BusinessException.class, () -> dao.capNhat(khongCo));

        assertEquals("Khách hàng không tồn tại.", ex.getMessage());
    }

    @Test
    void capNhat_sdtTrungKhachKhac_nemBusinessExceptionVaGiuSdtCu() {
        KhachHang a = themVaGhiNho("Khách A", sdtMoi(), null, null);
        KhachHang b = themVaGhiNho("Khách B", sdtMoi(), null, null);
        String sdtCuCuaB = b.getSdt();
        b.setSdt(a.getSdt());

        assertThrows(BusinessException.class, () -> dao.capNhat(b));

        assertEquals(sdtCuCuaB, dao.layTheoMa(b.getMaKH()).getSdt());
    }

    @Test
    void capNhat_vaiTroNhaSi_biChan() {
        KhachHang kh = themVaGhiNho("Nha Sĩ Không Được Sửa", sdtMoi(), null, null);
        DaoTestSupport.dangNhap(VaiTro.NHASI);

        assertThrows(BusinessException.class, () -> dao.capNhat(kh));
    }
}
