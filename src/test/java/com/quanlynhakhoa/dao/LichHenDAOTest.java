package com.quanlynhakhoa.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.entity.DichVu;
import com.quanlynhakhoa.entity.KhachHang;
import com.quanlynhakhoa.entity.LichHen;
import com.quanlynhakhoa.entity.NhanVien;
import com.quanlynhakhoa.entity.enums.TrangThaiLichHen;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LichHenDAOTest {

    private static final LocalDate NGAY = LocalDate.now().plusDays(10);
    private static final LocalDate NGAY_NGHI = NGAY.plusDays(1);
    private static final String NV = "NV002";
    private static final DateTimeFormatter YYMMDD = DateTimeFormatter.ofPattern("yyMMdd");
    private static final AtomicInteger DEM = new AtomicInteger(900);

    private final LichHenDAO dao = new LichHenDAO();
    private final String hauTo = String.valueOf(ThreadLocalRandom.current().nextInt(10_000, 100_000));
    private String maKH1;
    private String maKH2;
    private String sdt1;
    private final List<String> maLLV = new ArrayList<>();

    @BeforeAll
    static void chuanBi() {
        DaoTestSupport.yeuCauDatabaseTest();
    }

    @BeforeEach
    void taoDuLieu() throws SQLException {
        DaoTestSupport.dangNhap(VaiTro.LETAN);
        int so = Integer.parseInt(hauTo);
        maKH1 = "KH" + so;
        maKH2 = "KH" + (so == 99_999 ? 10_000 : so + 1);
        sdt1 = "09" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        String sdt2 = "09" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        try (Connection c = ketNoi()) {
            JdbcHelper.update(c, "DELETE FROM LichLamViec WHERE maNV=? AND ngayTruc IN (?,?)", NV, NGAY, NGAY_NGHI);
            JdbcHelper.update(c, "IF NOT EXISTS(SELECT 1 FROM LoaiDichVu WHERE maLoaiDV='LDV98')"
                    + " INSERT INTO LoaiDichVu(maLoaiDV,tenLoaiDV,mucThueGTGT)"
                    + " VALUES('LDV98',N'Điều trị bệnh lý',0.00)");
            JdbcHelper.update(c, "IF NOT EXISTS(SELECT 1 FROM DichVu WHERE maDV='DV998')"
                    + " INSERT INTO DichVu(maDV,maLoaiDV,tenDV,donGia,thoiLuong,trangThai)"
                    + " VALUES('DV998','LDV98',N'Dịch vụ kiểm thử lịch hẹn A',100000,30,1)");
            JdbcHelper.update(c, "IF NOT EXISTS(SELECT 1 FROM DichVu WHERE maDV='DV997')"
                    + " INSERT INTO DichVu(maDV,maLoaiDV,tenDV,donGia,thoiLuong,trangThai)"
                    + " VALUES('DV997','LDV98',N'Dịch vụ kiểm thử lịch hẹn B',200000,45,1)");
            JdbcHelper.update(c, "INSERT INTO KhachHang(maKH,tenKH,gioiTinh,sdt) VALUES(?,N'Zxqv Lịch Hẹn Một',1,?)",
                    maKH1, sdt1);
            JdbcHelper.update(c, "INSERT INTO KhachHang(maKH,tenKH,gioiTinh,sdt) VALUES(?,N'Zxqv Lịch Hẹn Hai',0,?)",
                    maKH2, sdt2);
            themCa(c, NGAY, "DA_PHAN_CONG", null);
            themCa(c, NGAY_NGHI, "NGHI_PHEP", "Nghỉ phép kiểm thử");
        }
    }

    @AfterEach
    void don() throws SQLException {
        try (Connection c = ketNoi()) {
            JdbcHelper.update(c, "DELETE FROM LichHen WHERE maKH IN (?,?)", maKH1, maKH2);
            for (String ma : maLLV) {
                JdbcHelper.update(c, "DELETE FROM LichLamViec WHERE maLLV=?", ma);
            }
            JdbcHelper.update(c, "DELETE FROM KhachHang WHERE maKH IN (?,?)", maKH1, maKH2);
            JdbcHelper.update(c, "DELETE FROM DichVu WHERE maDV IN ('DV998','DV997')");
            JdbcHelper.update(c, "DELETE FROM LoaiDichVu WHERE maLoaiDV='LDV98'");
        }
        maLLV.clear();
        DaoTestSupport.dangXuat();
    }

    @Test
    void them_hopLe_sinhMaDungDinhDangVaLuuDaDat() throws SQLException {
        LichHen lh = lich(maKH1, gio(9, 0), "DV998");

        assertTrue(dao.them(lh));

        assertTrue(lh.getMaLH().matches("LH" + NGAY.format(YYMMDD) + "\\d{3}"));
        assertEquals("DA_DAT", chuoi("SELECT trangThai FROM LichHen WHERE maLH=?", lh.getMaLH()));
    }

    @Test
    void them_haiLichCungNgay_maKhacNhau() throws SQLException {
        LichHen a = lich(maKH1, gio(9, 0), "DV998");
        LichHen b = lich(maKH2, gio(11, 0), "DV998");

        assertTrue(dao.them(a));
        assertTrue(dao.them(b));

        assertFalse(a.getMaLH().equals(b.getMaLH()));
    }

    @Test
    void them_ngayGioQuaKhu_nemLoi() {
        LichHen lh = lich(maKH1, LocalDateTime.now().minusDays(1), "DV998");

        BusinessException ex = assertThrows(BusinessException.class, () -> dao.them(lh));

        assertEquals("Ngày giờ hẹn phải ở tương lai.", ex.getMessage());
    }

    @Test
    void them_truocGioBatDauCa_nemLoi() {
        LichHen lh = lich(maKH1, gio(6, 0), "DV998");

        BusinessException ex = assertThrows(BusinessException.class, () -> dao.them(lh));

        assertEquals("Nha sĩ không có ca làm việc phù hợp hoặc đang nghỉ phép.", ex.getMessage());
    }

    @Test
    void them_vuotQuaGioKetThucCa_nemLoi() {
        LichHen lh = lich(maKH1, gio(17, 15), "DV998");

        assertThrows(BusinessException.class, () -> dao.them(lh));
    }

    @Test
    void them_nhaSiNghiPhep_nemLoi() {
        LichHen lh = lich(maKH1, NGAY_NGHI.atTime(9, 0), "DV998");

        BusinessException ex = assertThrows(BusinessException.class, () -> dao.them(lh));

        assertEquals("Nha sĩ không có ca làm việc phù hợp hoặc đang nghỉ phép.", ex.getMessage());
    }

    @Test
    void them_dichVuNgungCungCap_nemLoi() throws SQLException {
        try (Connection c = ketNoi()) {
            JdbcHelper.update(c, "UPDATE DichVu SET trangThai=0 WHERE maDV='DV998'");
        }
        LichHen lh = lich(maKH1, gio(9, 0), "DV998");

        BusinessException ex = assertThrows(BusinessException.class, () -> dao.them(lh));

        assertEquals("Dịch vụ không hoạt động.", ex.getMessage());
    }

    @Test
    void them_trungLichCuaBenhNhan_nemLoi() {
        assertTrue(dao.them(lich(maKH1, gio(9, 0), "DV998")));

        assertThrows(BusinessException.class, () -> dao.them(lich(maKH1, gio(9, 15), "DV998")));
    }

    @Test
    void them_nhaSiCachDuoi15Phut_nemLoi() {
        assertTrue(dao.them(lich(maKH1, gio(9, 0), "DV998")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dao.them(lich(maKH2, gio(9, 40), "DV998")));

        assertEquals("Trùng lịch khách hàng/nha sĩ hoặc thiếu khoảng cách chuẩn bị.", ex.getMessage());
    }

    @Test
    void them_nhaSiCachDung15Phut_hopLe() {
        assertTrue(dao.them(lich(maKH1, gio(9, 0), "DV998")));

        assertTrue(dao.them(lich(maKH2, gio(9, 45), "DV998")));
    }

    @Test
    void them_lichBiHuyKhongChanKhungGio() {
        LichHen cu = lich(maKH1, gio(9, 0), "DV998");
        assertTrue(dao.them(cu));
        assertTrue(dao.huy(cu.getMaLH(), "Khách bận"));

        assertTrue(dao.them(lich(maKH2, gio(9, 0), "DV998")));
    }

    @Test
    void them_datDongThoiCungKhung_chiMotNguoiThanhCong() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> a = () -> thu(() -> dao.them(lich(maKH1, gio(10, 0), "DV998")));
            Callable<Boolean> b = () -> thu(() -> dao.them(lich(maKH2, gio(10, 0), "DV998")));
            Future<Boolean> fa = pool.submit(a);
            Future<Boolean> fb = pool.submit(b);

            int thanhCong = (fa.get() ? 1 : 0) + (fb.get() ? 1 : 0);

            assertEquals(1, thanhCong);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void them_vaiTroNhaSi_biChan() throws SQLException {
        LichHen lh = lich(maKH1, gio(9, 0), "DV998");
        DaoTestSupport.dangNhap(VaiTro.NHASI);

        BusinessException ex = assertThrows(BusinessException.class, () -> dao.them(lh));

        assertEquals("Bạn không có quyền thực hiện thao tác này hoặc tài khoản đã bị khóa.", ex.getMessage());
        assertEquals(0, DaoTestSupport.dem("SELECT COUNT(*) FROM LichHen WHERE maKH=?", maKH1));
    }

    @Test
    void them_chuaDangNhap_biChan() {
        LichHen lh = lich(maKH1, gio(9, 0), "DV998");
        DaoTestSupport.dangXuat();

        BusinessException ex = assertThrows(BusinessException.class, () -> dao.them(lh));

        assertEquals("Vui lòng đăng nhập.", ex.getMessage());
    }

    // ------------------------------------------------------------------ R-LH-3 hủy nhiều lần

    @Test
    void them_khachDaHuy3LanTrongThang_khungGiuaNgayBiChan() throws SQLException {
        chenLichDaHuy(maKH1, gio(13, 0), 3);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dao.them(lich(maKH1, gio(11, 0), "DV998")));

        assertEquals("Khách hủy nhiều lần trong tháng; chỉ đặt khung đầu/cuối ngày.", ex.getMessage());
    }

    @Test
    void them_khachDaHuy3LanTrongThang_khungDauNgayVanDuocDat() throws SQLException {
        chenLichDaHuy(maKH1, gio(13, 0), 3);

        assertTrue(dao.them(lich(maKH1, gio(8, 0), "DV998")));
    }

    @Test
    void them_khachDaHuy3LanTrongThang_khungCuoiNgayVanDuocDat() throws SQLException {
        chenLichDaHuy(maKH1, gio(11, 0), 3);

        assertTrue(dao.them(lich(maKH1, gio(16, 30), "DV998")));
    }

    @Test
    void them_khachDaHuy2Lan_datKhungGiuaNgayBinhThuong() throws SQLException {
        chenLichDaHuy(maKH1, gio(13, 0), 2);

        assertTrue(dao.them(lich(maKH1, gio(11, 0), "DV998")));
    }

    @Test
    void demSoLanHuyTrongThang_chiDemDaHuy_khongDemKhongDen() throws SQLException {
        chenLichDaHuy(maKH1, gio(13, 0), 1);
        chenLich(maKH1, gio(15, 0), "KHONG_DEN", null);

        assertEquals(1, dao.demSoLanHuyTrongThang(maKH1, NGAY));
    }

    @Test
    void huy_coLyDo_chuyenDaHuyVaLuuLyDo() throws SQLException {
        LichHen lh = lich(maKH1, gio(9, 0), "DV998");
        assertTrue(dao.them(lh));

        assertTrue(dao.huy(lh.getMaLH(), "Khách có việc đột xuất"));

        assertEquals("DA_HUY", chuoi("SELECT trangThai FROM LichHen WHERE maLH=?", lh.getMaLH()));
        assertEquals("Khách có việc đột xuất", chuoi("SELECT lyDoHuy FROM LichHen WHERE maLH=?", lh.getMaLH()));
    }

    @Test
    void huy_khongCoLyDo_nemLoi() {
        LichHen lh = lich(maKH1, gio(9, 0), "DV998");
        assertTrue(dao.them(lh));

        BusinessException ex = assertThrows(BusinessException.class, () -> dao.huy(lh.getMaLH(), "  "));

        assertEquals("Vui lòng nhập lý do hủy.", ex.getMessage());
    }

    @Test
    void huy_lichDaDen_nemLoi() throws SQLException {
        String ma = chenLich(maKH1, gio(9, 0), "DA_DEN", null);

        BusinessException ex = assertThrows(BusinessException.class, () -> dao.huy(ma, "Muộn"));

        assertEquals("Chỉ được hủy lịch đang đặt.", ex.getMessage());
    }

    @Test
    void huy_vaiTroNhaSi_biChan() {
        LichHen lh = lich(maKH1, gio(9, 0), "DV998");
        assertTrue(dao.them(lh));
        DaoTestSupport.dangNhap(VaiTro.NHASI);

        assertThrows(BusinessException.class, () -> dao.huy(lh.getMaLH(), "Không được"));
    }

    // ------------------------------------------------------------------ R-LH-4 không đến

    @Test
    void capNhatKhongDen_lichQuaGio_thanhKhongDenVaLichTuongLaiGiuNguyen() throws SQLException {
        String qua = chenLichTuongDoi(maKH1, -180, "DA_DAT");
        String sau = chenLich(maKH1, gio(15, 0), "DA_DAT", null);

        assertTrue(dao.capNhatKhongDen() >= 1);

        assertEquals("KHONG_DEN", chuoi("SELECT trangThai FROM LichHen WHERE maLH=?", qua));
        assertEquals("DA_DAT", chuoi("SELECT trangThai FROM LichHen WHERE maLH=?", sau));
    }

    @Test
    void capNhatKhongDen_vaiTroNhaSi_biChan() {
        DaoTestSupport.dangNhap(VaiTro.NHASI);

        assertThrows(BusinessException.class, dao::capNhatKhongDen);
    }

    @Test
    void doiLich_sangKhungTrong_capNhatNgayGio() throws SQLException {
        LichHen lh = lich(maKH1, gio(9, 0), "DV998");
        assertTrue(dao.them(lh));
        LichHen cu = dao.layTheoMa(lh.getMaLH());
        cu.setNgayGioHen(gio(14, 0));

        assertTrue(dao.capNhat(cu));

        assertEquals(gio(14, 0), dao.layTheoMa(lh.getMaLH()).getNgayGioHen());
    }

    @Test
    void doiLich_sangKhungDaKin_nemLoiVaGiuLichCu() {
        LichHen a = lich(maKH1, gio(9, 0), "DV998");
        assertTrue(dao.them(a));
        assertTrue(dao.them(lich(maKH2, gio(11, 0), "DV998")));
        LichHen cu = dao.layTheoMa(a.getMaLH());
        cu.setNgayGioHen(gio(11, 10));

        assertThrows(BusinessException.class, () -> dao.capNhat(cu));

        assertEquals(gio(9, 0), dao.layTheoMa(a.getMaLH()).getNgayGioHen());
    }

    @Test
    void doiLich_chiTrongKhungCuaChinhNo_khongTuChanMinh() {
        LichHen a = lich(maKH1, gio(9, 0), "DV998");
        assertTrue(dao.them(a));
        LichHen cu = dao.layTheoMa(a.getMaLH());
        cu.setNgayGioHen(gio(9, 10));

        assertTrue(dao.capNhat(cu));
    }

    @Test
    void doiLich_lichDaDen_nemLoi() throws SQLException {
        String ma = chenLich(maKH1, gio(9, 0), "DA_DEN", null);
        LichHen cu = dao.layTheoMa(ma);
        cu.setNgayGioHen(gio(14, 0));

        BusinessException ex = assertThrows(BusinessException.class, () -> dao.capNhat(cu));

        assertEquals("Chỉ được dời lịch đang ở trạng thái đã đặt.", ex.getMessage());
    }

    @Test
    void doiLich_doiBenhNhan_biChan() {
        LichHen a = lich(maKH1, gio(9, 0), "DV998");
        assertTrue(dao.them(a));
        LichHen cu = dao.layTheoMa(a.getMaLH());
        cu.setKhachHang(khach(maKH2));

        BusinessException ex = assertThrows(BusinessException.class, () -> dao.capNhat(cu));

        assertEquals("Không được đổi bệnh nhân của lịch hẹn.", ex.getMessage());
    }

    @Test
    void capNhatThongTin_doiGhiChu_luuDuoc() {
        LichHen a = lich(maKH1, gio(9, 0), "DV998");
        assertTrue(dao.them(a));
        LichHen cu = dao.layTheoMa(a.getMaLH());
        cu.setGhiChu("Đau răng số 7");

        assertTrue(dao.capNhatThongTin(cu));

        assertEquals("Đau răng số 7", dao.layTheoMa(a.getMaLH()).getGhiChu());
    }

    @Test
    void capNhatThongTin_doiDichVu_luuDuocVaKiemTraLaiKhungGio() {
        LichHen a = lich(maKH1, gio(9, 0), "DV998");
        assertTrue(dao.them(a));
        LichHen cu = dao.layTheoMa(a.getMaLH());
        cu.setDichVu(dichVu("DV997"));

        assertTrue(dao.capNhatThongTin(cu));

        assertEquals("DV997", dao.layTheoMa(a.getMaLH()).getDichVu().getMaDV());
    }

    @Test
    void capNhatThongTin_doiDichVuLamDeLenLichKhac_nemLoi() {
        assertTrue(dao.them(lich(maKH2, gio(9, 50), "DV998")));
        LichHen a = lich(maKH1, gio(9, 0), "DV998");
        assertTrue(dao.them(a));
        LichHen cu = dao.layTheoMa(a.getMaLH());
        cu.setDichVu(dichVu("DV997"));

        assertThrows(BusinessException.class, () -> dao.capNhatThongTin(cu));

        assertEquals("DV998", dao.layTheoMa(a.getMaLH()).getDichVu().getMaDV());
    }

    @Test
    void capNhatThongTin_doiNgayGio_biTuChoi() {
        LichHen a = lich(maKH1, gio(9, 0), "DV998");
        assertTrue(dao.them(a));
        LichHen cu = dao.layTheoMa(a.getMaLH());
        cu.setNgayGioHen(gio(14, 0));

        BusinessException ex = assertThrows(BusinessException.class, () -> dao.capNhatThongTin(cu));

        assertEquals("Cập nhật thông tin không được đổi ngày giờ hoặc nha sĩ; hãy dùng chức năng dời lịch.",
                ex.getMessage());
        assertEquals(gio(9, 0), dao.layTheoMa(a.getMaLH()).getNgayGioHen());
    }

    @Test
    void capNhatThongTin_doiBenhNhan_biTuChoi() {
        LichHen a = lich(maKH1, gio(9, 0), "DV998");
        assertTrue(dao.them(a));
        LichHen cu = dao.layTheoMa(a.getMaLH());
        cu.setKhachHang(khach(maKH2));

        assertThrows(BusinessException.class, () -> dao.capNhatThongTin(cu));
    }

    @Test
    void capNhatThongTin_lichDaDen_nemLoi() throws SQLException {
        String ma = chenLich(maKH1, gio(9, 0), "DA_DEN", null);
        LichHen cu = dao.layTheoMa(ma);
        cu.setGhiChu("Sửa muộn");

        assertThrows(BusinessException.class, () -> dao.capNhatThongTin(cu));
    }

    @Test
    void timKiem_theoMaLich_timThay() {
        LichHen a = lich(maKH1, gio(9, 0), "DV998");
        assertTrue(dao.them(a));

        List<LichHen> kq = dao.timKiem(a.getMaLH(), NGAY, NGAY, null);

        assertEquals(1, kq.size());
        assertEquals(a, kq.get(0));
    }

    @Test
    void timKiem_theoMaKhachHang_timThay() {
        LichHen a = lich(maKH1, gio(9, 0), "DV998");
        assertTrue(dao.them(a));

        assertTrue(dao.timKiem(maKH1, NGAY, NGAY, null).contains(a));
    }

    @Test
    void timKiem_theoSoDienThoai_timThay() {
        LichHen a = lich(maKH1, gio(9, 0), "DV998");
        assertTrue(dao.them(a));

        assertTrue(dao.timKiem(sdt1, NGAY, NGAY, null).contains(a));
    }

    @Test
    void timKiem_theoHoTen_timThayDungNguoi() {
        LichHen a = lich(maKH1, gio(9, 0), "DV998");
        LichHen b = lich(maKH2, gio(11, 0), "DV998");
        assertTrue(dao.them(a));
        assertTrue(dao.them(b));

        List<LichHen> kq = dao.timKiem("Zxqv Lịch Hẹn Hai", NGAY, NGAY, null);

        assertTrue(kq.contains(b));
        assertFalse(kq.contains(a));
    }

    @Test
    void timKiem_locTheoTrangThai() {
        LichHen a = lich(maKH1, gio(9, 0), "DV998");
        LichHen b = lich(maKH2, gio(11, 0), "DV998");
        assertTrue(dao.them(a));
        assertTrue(dao.them(b));
        assertTrue(dao.huy(b.getMaLH(), "Hủy thử"));

        List<LichHen> daDat = dao.timKiem("Zxqv", NGAY, NGAY, TrangThaiLichHen.DA_DAT);

        assertTrue(daDat.contains(a));
        assertFalse(daDat.contains(b));
    }

    @Test
    void timKiem_tuKhoaRong_traVeTatCaTrongKhoang() {
        LichHen a = lich(maKH1, gio(9, 0), "DV998");
        assertTrue(dao.them(a));

        assertTrue(dao.timKiem("", NGAY, NGAY, null).contains(a));
        assertTrue(dao.timKiem(null, NGAY, NGAY, null).contains(a));
    }

    @Test
    void timKiem_khongKhopTuKhoa_traVeRong() {
        assertTrue(dao.timKiem("khong-ton-tai-" + System.nanoTime(), NGAY, NGAY, null).isEmpty());
    }

    @Test
    void timKiem_khoangNgayNguoc_nemLoi() {
        assertThrows(BusinessException.class, () -> dao.timKiem("", NGAY, NGAY.minusDays(1), null));
    }

    @Test
    void timKiem_nhaSiChiThayLichCuaMinh() {
        LichHen a = lich(maKH1, gio(9, 0), "DV998");
        assertTrue(dao.them(a));
        DaoTestSupport.dangNhap(VaiTro.NHASI);

        assertTrue(dao.timKiem(maKH1, NGAY, NGAY, null).contains(a));
        assertEquals(a, dao.layTheoMa(a.getMaLH()));
    }

    // ------------------------------------------------------------------ gợi ý khung trống

    @Test
    void goiYKhungTrong_loaiKhungSatLichDaCoVaGiuKhungDungCach15Phut() {
        assertTrue(dao.them(lich(maKH1, gio(9, 0), "DV998")));

        List<LocalDateTime> kq = dao.goiYKhungTrong(maKH2, NV, "DV998", NGAY);

        assertTrue(kq.contains(gio(8, 15)));
        assertTrue(kq.contains(gio(9, 45)));
        assertFalse(kq.contains(gio(8, 30)));
        assertFalse(kq.contains(gio(9, 0)));
        assertFalse(kq.contains(gio(9, 15)));
        assertFalse(kq.contains(gio(9, 30)));
    }

    @Test
    void goiYKhungTrong_chiNamTrongCaLamViec() {
        List<LocalDateTime> kq = dao.goiYKhungTrong(maKH1, NV, "DV998", NGAY);

        assertFalse(kq.isEmpty());
        for (LocalDateTime t : kq) {
            assertFalse(t.toLocalTime().isBefore(LocalTime.of(7, 0)));
            assertFalse(t.plusMinutes(30).toLocalTime().isAfter(LocalTime.of(17, 30)));
            assertEquals(NGAY, t.toLocalDate());
        }
    }

    @Test
    void goiYKhungTrong_ngayNghiPhep_khongCoKhung() {
        assertTrue(dao.goiYKhungTrong(maKH1, NV, "DV998", NGAY_NGHI).isEmpty());
    }

    @Test
    void goiYKhungTrong_khachHuy3Lan_chiGoiYKhungDauVaCuoiNgay() throws SQLException {
        chenLichDaHuy(maKH1, gio(13, 0), 3);

        List<LocalDateTime> kq = dao.goiYKhungTrong(maKH1, NV, "DV998", NGAY);

        assertFalse(kq.isEmpty());
        for (LocalDateTime t : kq) {
            LocalTime bd = t.toLocalTime();
            LocalTime kt = bd.plusMinutes(30);
            boolean dau = !bd.isBefore(LocalTime.of(7, 0)) && !kt.isAfter(LocalTime.of(9, 30));
            boolean cuoi = !bd.isBefore(LocalTime.of(16, 0)) && !kt.isAfter(LocalTime.of(17, 30));
            assertTrue(dau || cuoi, "Khung " + bd + " nằm giữa ngày");
        }
    }

    @Test
    void goiYKhungTrong_khiDoiLich_khungCuaChinhLichDangDoiDuocGoiY() {
        LichHen a = lich(maKH1, gio(9, 0), "DV998");
        assertTrue(dao.them(a));

        List<LocalDateTime> thuong = dao.goiYKhungTrong(maKH1, NV, "DV998", NGAY);
        List<LocalDateTime> khiDoi = dao.goiYKhungTrong(maKH1, NV, "DV998", NGAY, a.getMaLH());

        assertFalse(thuong.contains(gio(9, 0)));
        assertTrue(khiDoi.contains(gio(9, 0)));
    }

    @Test
    void goiYKhungTrong_dichVuNgung_nemLoi() throws SQLException {
        try (Connection c = ketNoi()) {
            JdbcHelper.update(c, "UPDATE DichVu SET trangThai=0 WHERE maDV='DV998'");
        }

        assertThrows(BusinessException.class, () -> dao.goiYKhungTrong(maKH1, NV, "DV998", NGAY));
    }

    @Test
    void goiYKhungTrong_thieuNgay_nemLoi() {
        assertThrows(BusinessException.class, () -> dao.goiYKhungTrong(maKH1, NV, "DV998", null));
    }

    // ------------------------------------------------------------------ T022 chuyển trạng thái dùng chung Connection

    @Test
    void danhDauDaDen_lichDaDat_chuyenDaDenVaChiMotLan() throws SQLException {
        String ma = chenLich(maKH1, gio(9, 0), "DA_DAT", null);

        boolean lan1 = daDen(ma);
        boolean lan2 = daDen(ma);

        assertTrue(lan1);
        assertFalse(lan2);
        assertEquals("DA_DEN", chuoi("SELECT trangThai FROM LichHen WHERE maLH=?", ma));
    }

    @Test
    void danhDauDaDen_lichDaHuy_traFalseVaGiuNguyen() throws SQLException {
        String ma = chenLich(maKH1, gio(9, 0), "DA_HUY", "Đã hủy");

        assertFalse(daDen(ma));

        assertEquals("DA_HUY", chuoi("SELECT trangThai FROM LichHen WHERE maLH=?", ma));
    }

    @Test
    void danhDauDaDen_khongTuCommit_rollbackKhiBenGoiLoi() throws SQLException {
        String ma = chenLich(maKH1, gio(9, 0), "DA_DAT", null);

        assertThrows(IllegalStateException.class, () -> JdbcHelper.transaction(c -> {
            assertTrue(dao.danhDauDaDen(c, ma));
            throw new IllegalStateException("Bên gọi lỗi sau khi đổi trạng thái");
        }));

        assertEquals("DA_DAT", chuoi("SELECT trangThai FROM LichHen WHERE maLH=?", ma));
    }

    @Test
    void danhDauHoanTat_chiTuDaDen() throws SQLException {
        String ma = chenLich(maKH1, gio(9, 0), "DA_DAT", null);

        assertFalse(hoanTat(ma));
        assertTrue(daDen(ma));
        assertTrue(hoanTat(ma));

        assertEquals("HOAN_TAT", chuoi("SELECT trangThai FROM LichHen WHERE maLH=?", ma));
    }

    @Test
    void danhDauHoanTat_khongTuCommit_rollbackKhiBenGoiLoi() throws SQLException {
        String ma = chenLich(maKH1, gio(9, 0), "DA_DEN", null);

        assertThrows(IllegalStateException.class, () -> JdbcHelper.transaction(c -> {
            assertTrue(dao.danhDauHoanTat(c, ma));
            throw new IllegalStateException("Bên gọi lỗi");
        }));

        assertEquals("DA_DEN", chuoi("SELECT trangThai FROM LichHen WHERE maLH=?", ma));
    }

    // ------------------------------------------------------------------ hỗ trợ

    private boolean daDen(String ma) {
        return JdbcHelper.transaction(c -> dao.danhDauDaDen(c, ma));
    }

    private boolean hoanTat(String ma) {
        return JdbcHelper.transaction(c -> dao.danhDauHoanTat(c, ma));
    }

    private static LocalDateTime gio(int gio, int phut) {
        return NGAY.atTime(gio, phut);
    }

    private KhachHang khach(String maKH) {
        return new KhachHangDAO().layTheoMa(maKH);
    }

    private DichVu dichVu(String maDV) {
        return new DichVuDAO().layTheoMa(maDV);
    }

    private LichHen lich(String maKH, LocalDateTime gio, String maDV) {
        NhanVien nhaSi = new NhanVien(NV, "Nha sĩ kiểm thử", true, "0398682231",
                "macvestdinet+nhasi@gmail.com", "Nha sĩ");
        return new LichHen(null, gio, TrangThaiLichHen.DA_DAT, null, null, khach(maKH), nhaSi, dichVu(maDV), null);
    }

    private void themCa(Connection c, LocalDate ngay, String trangThai, String lyDo) throws SQLException {
        String ma = "LLV" + String.format("%05d", ThreadLocalRandom.current().nextInt(70_000, 99_000));
        maLLV.add(ma);
        JdbcHelper.update(c, "INSERT INTO LichLamViec(maLLV,maNV,ngayTruc,caTruc,gioBatDau,gioKetThuc,trangThai,ghiChuNghi)"
                + " VALUES(?,?,?,'CA_NGAY','07:00','17:30',?,?)", ma, NV, ngay, trangThai, lyDo);
    }

    private String chenLich(String maKH, LocalDateTime gio, String trangThai, String lyDo) throws SQLException {
        String ma = "LH" + gio.format(YYMMDD) + String.format("%03d", DEM.getAndIncrement() % 100 + 900);
        try (Connection c = ketNoi()) {
            JdbcHelper.update(c, "INSERT INTO LichHen(maLH,maKH,maNV,maDV,ngayGioHen,trangThai,lyDoHuy)"
                    + " VALUES(?,?,?,'DV998',?,?,?)", ma, maKH, NV, gio, trangThai, lyDo);
        }
        return ma;
    }

    private String chenLichTuongDoi(String maKH, int phutLech, String trangThai) throws SQLException {
        LocalDateTime gio = LocalDateTime.now().plusMinutes(phutLech).withSecond(0).withNano(0);
        return chenLich(maKH, gio, trangThai, null);
    }

    private void chenLichDaHuy(String maKH, LocalDateTime batDau, int soLuong) throws SQLException {
        for (int i = 0; i < soLuong; i++) {
            chenLich(maKH, batDau.plusHours(i), "DA_HUY", "Hủy kiểm thử " + i);
        }
    }

    private static Connection ketNoi() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    private static String chuoi(String sql, String ma) throws SQLException {
        try (Connection c = ketNoi(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, ma);
            try (var r = ps.executeQuery()) {
                return r.next() ? r.getString(1) : null;
            }
        }
    }

    private static boolean thu(Callable<Boolean> viec) throws Exception {
        try {
            return viec.call();
        } catch (BusinessException e) {
            return false;
        }
    }
}
