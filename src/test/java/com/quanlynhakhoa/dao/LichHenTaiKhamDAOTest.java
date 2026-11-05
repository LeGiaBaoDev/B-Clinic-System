package com.quanlynhakhoa.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.entity.LichHen;
import com.quanlynhakhoa.entity.NhanVien;
import com.quanlynhakhoa.entity.PhieuKham;
import com.quanlynhakhoa.entity.enums.TrangThaiLichHen;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** T901/T902 – UC017 đặt lịch tái khám từ phiếu khám đã hoàn tất. */
class LichHenTaiKhamDAOTest {

    private static final String NV = "NV002";
    private static final LocalDate HOM_NAY = LocalDate.now();
    private static final DateTimeFormatter YYMMDD = DateTimeFormatter.ofPattern("yyMMdd");
    /** Mặc định tái khám 7–14 ngày kể từ ngày lập phiếu. */
    private static final LocalDate NGAY = HOM_NAY.plusDays(10);

    private final LichHenDAO dao = new LichHenDAO();
    private final String hauTo = String.valueOf(ThreadLocalRandom.current().nextInt(10_000, 100_000));
    private final List<String> maLLV = new ArrayList<>();
    private String maKH;
    private String maHS;
    private String maLHNguon;
    private String maPK;

    @BeforeAll
    static void chuanBi() {
        DaoTestSupport.yeuCauDatabaseTest();
    }

    @BeforeEach
    void taoDuLieu() throws SQLException {
        DaoTestSupport.dangNhap(VaiTro.LETAN);
        maKH = "KH" + hauTo;
        maHS = "HS" + hauTo;
        maPK = "PK9" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        maLHNguon = "LH" + HOM_NAY.minusDays(1).format(YYMMDD)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(800, 900));
        String sdt = "09" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        try (Connection c = ketNoi()) {
            JdbcHelper.update(c, "DELETE FROM LichLamViec WHERE maNV=? AND ngayTruc BETWEEN ? AND ?",
                    NV, HOM_NAY.plusDays(1), HOM_NAY.plusDays(40));
            JdbcHelper.update(c, "IF NOT EXISTS(SELECT 1 FROM LoaiDichVu WHERE maLoaiDV='LDV98')"
                    + " INSERT INTO LoaiDichVu(maLoaiDV,tenLoaiDV,mucThueGTGT)"
                    + " VALUES('LDV98',N'Điều trị bệnh lý',0.00)");
            JdbcHelper.update(c, "IF NOT EXISTS(SELECT 1 FROM DichVu WHERE maDV='DV998')"
                    + " INSERT INTO DichVu(maDV,maLoaiDV,tenDV,donGia,thoiLuong,loTrinhDichVu,trangThai)"
                    + " VALUES('DV998','LDV98',N'Dịch vụ kiểm thử tái khám A',100000,30,N'Tái khám sau 7–14 ngày',1)");
            JdbcHelper.update(c, "IF NOT EXISTS(SELECT 1 FROM DichVu WHERE maDV='DV997')"
                    + " INSERT INTO DichVu(maDV,maLoaiDV,tenDV,donGia,thoiLuong,loTrinhDichVu,trangThai)"
                    + " VALUES('DV997','LDV98',N'Dịch vụ kiểm thử tái khám B',200000,45,N'Tái khám sau 7–14 ngày',1)");
            JdbcHelper.update(c, "INSERT INTO KhachHang(maKH,tenKH,gioiTinh,sdt) VALUES(?,N'Zxqv Tái Khám',1,?)",
                    maKH, sdt);
            JdbcHelper.update(c, "INSERT INTO HoSoBenhAn(maHSBA,maKH) VALUES(?,?)", maHS, maKH);
            JdbcHelper.update(c, "INSERT INTO LichHen(maLH,maKH,maNV,maDV,ngayGioHen,trangThai)"
                    + " VALUES(?,?,?,'DV998',?,'HOAN_TAT')", maLHNguon, maKH, NV,
                    HOM_NAY.minusDays(1).atTime(9, 0));
            JdbcHelper.update(c, "INSERT INTO PhieuKham(maPK,maHSBA,maLH,maNV_LapPhieu,ngayLap,trangThai,loiDan)"
                    + " VALUES(?,?,?,?,?,'HOAN_TAT',N'Tái khám sau 10 ngày')", maPK, maHS, maLHNguon, NV,
                    LocalDateTime.now().withNano(0));
            for (int ngay : new int[]{3, 6, 7, 10, 14, 15, 30}) {
                themCa(c, HOM_NAY.plusDays(ngay));
            }
        }
    }

    @AfterEach
    void don() throws SQLException {
        try (Connection c = ketNoi()) {
            JdbcHelper.update(c, "DELETE FROM LichHen WHERE maKH=? AND maPKNguon IS NOT NULL", maKH);
            JdbcHelper.update(c, "DISABLE TRIGGER trg_KhoaDuLieuPhieuKhamHoanTat ON PhieuKham");
            try {
                JdbcHelper.update(c, "DELETE FROM PhieuKham WHERE maPK=?", maPK);
            } finally {
                JdbcHelper.update(c, "ENABLE TRIGGER trg_KhoaDuLieuPhieuKhamHoanTat ON PhieuKham");
            }
            JdbcHelper.update(c, "DELETE FROM LichHen WHERE maKH=?", maKH);
            for (String ma : maLLV) {
                JdbcHelper.update(c, "DELETE FROM LichLamViec WHERE maLLV=?", ma);
            }
            JdbcHelper.update(c, "DELETE FROM HoSoBenhAn WHERE maHSBA=?", maHS);
            JdbcHelper.update(c, "DELETE FROM KhachHang WHERE maKH=?", maKH);
            JdbcHelper.update(c, "DELETE FROM DichVu WHERE maDV IN ('DV998','DV997')");
            JdbcHelper.update(c, "DELETE FROM LoaiDichVu WHERE maLoaiDV='LDV98'");
        }
        maLLV.clear();
        DaoTestSupport.dangXuat();
    }

    // ------------------------------------------------------------------ taoTaiKham

    @Test
    void taoTaiKham_hopLe_giuKhachNhaSiDichVuVaGanPhieuNguon() throws SQLException {
        LichHen lh = dao.taoTaiKham(maPK, NGAY.atTime(9, 0), "Kiểm tra sau điều trị");

        assertNotNull(lh.getMaLH());
        assertEquals(TrangThaiLichHen.DA_DAT, lh.getTrangThai());
        assertEquals(maKH, lh.getKhachHang().getMaKH());
        assertEquals(NV, lh.getNhaSi().getMaNV());
        assertEquals("DV998", lh.getDichVu().getMaDV());
        assertEquals(maPK, lh.getPhieuKhamNguon().getMaPK());
        assertEquals(maPK, chuoi("SELECT maPKNguon FROM LichHen WHERE maLH=?", lh.getMaLH()));
        assertEquals("DA_DAT", chuoi("SELECT trangThai FROM LichHen WHERE maLH=?", lh.getMaLH()));
    }

    @Test
    void taoTaiKham_ngayDauKhoang_7Ngay_hopLe() {
        assertNotNull(dao.taoTaiKham(maPK, HOM_NAY.plusDays(7).atTime(9, 0), null).getMaLH());
    }

    @Test
    void taoTaiKham_ngayCuoiKhoang_14Ngay_hopLe() {
        assertNotNull(dao.taoTaiKham(maPK, HOM_NAY.plusDays(14).atTime(9, 0), null).getMaLH());
    }

    @Test
    void taoTaiKham_truocKhoang_6Ngay_nemLoi() throws SQLException {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> dao.taoTaiKham(maPK, HOM_NAY.plusDays(6).atTime(9, 0), null));

        assertEquals("Ngày tái khám ngoài khoảng cho phép.", ex.getMessage());
        assertEquals(0, soTaiKham());
    }

    @Test
    void taoTaiKham_sauKhoang_15Ngay_nemLoi() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> dao.taoTaiKham(maPK, HOM_NAY.plusDays(15).atTime(9, 0), null));

        assertEquals("Ngày tái khám ngoài khoảng cho phép.", ex.getMessage());
    }

    @Test
    void taoTaiKham_phieuChuaHoanTat_nemLoi() throws SQLException {
        try (Connection c = ketNoi()) {
            JdbcHelper.update(c, "DISABLE TRIGGER trg_KhoaDuLieuPhieuKhamHoanTat ON PhieuKham");
            try {
                // trigger chỉ chặn sửa phiếu đang HOAN_TAT; tắt tạm để dựng lại trạng thái đang khám
                JdbcHelper.update(c, "UPDATE PhieuKham SET trangThai='DANG_KHAM' WHERE maPK=?", maPK);
            } finally {
                JdbcHelper.update(c, "ENABLE TRIGGER trg_KhoaDuLieuPhieuKhamHoanTat ON PhieuKham");
            }
        }

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dao.taoTaiKham(maPK, NGAY.atTime(9, 0), null));

        assertEquals("Phiếu nguồn tái khám chưa hoàn tất.", ex.getMessage());
    }

    @Test
    void taoTaiKham_phieuKhongTonTai_nemLoi() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> dao.taoTaiKham("PK999999999", NGAY.atTime(9, 0), null));

        assertEquals("Phiếu nguồn tái khám chưa hoàn tất.", ex.getMessage());
    }

    @Test
    void taoTaiKham_dichVuChuaCoLoTrinh_nemLoi() throws SQLException {
        try (Connection c = ketNoi()) {
            JdbcHelper.update(c, "UPDATE DichVu SET loTrinhDichVu=NULL WHERE maDV='DV998'");
        }

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dao.taoTaiKham(maPK, NGAY.atTime(9, 0), null));

        assertEquals("Dịch vụ chưa có lộ trình tái khám.", ex.getMessage());
    }

    @Test
    void taoTaiKham_phieuDaCoLichTaiKham_nemLoi() throws SQLException {
        dao.taoTaiKham(maPK, NGAY.atTime(9, 0), null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dao.taoTaiKham(maPK, NGAY.atTime(14, 0), null));

        assertEquals("Phiếu khám này đã có lịch tái khám.", ex.getMessage());
        assertEquals(1, soTaiKham());
    }

    @Test
    void taoTaiKham_lichTaiKhamTruocDaHuy_datLaiDuoc() {
        LichHen cu = dao.taoTaiKham(maPK, NGAY.atTime(9, 0), null);
        assertTrue(dao.huy(cu.getMaLH(), "Khách bận"));

        assertNotNull(dao.taoTaiKham(maPK, NGAY.atTime(9, 0), null).getMaLH());
    }

    @Test
    void taoTaiKham_trungGioNhaSi_nemLoi() {
        dao.taoTaiKham(maPK, NGAY.atTime(9, 0), null);

        assertThrows(BusinessException.class, () -> chenLichKhac(NGAY.atTime(9, 10)));
    }

    @Test
    void taoTaiKham_vaiTroNhaSi_biChan() throws SQLException {
        DaoTestSupport.dangNhap(VaiTro.NHASI);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dao.taoTaiKham(maPK, NGAY.atTime(9, 0), null));

        assertEquals("Bạn không có quyền thực hiện thao tác này hoặc tài khoản đã bị khóa.", ex.getMessage());
        assertEquals(0, soTaiKham());
    }

    @Test
    void taoTaiKham_thieuNgayGio_nemLoi() {
        assertThrows(BusinessException.class, () -> dao.taoTaiKham(maPK, null, null));
    }

    // ------------------------------------------------------------------ giữ nguyên nguồn khi dùng them/capNhat

    @Test
    void them_taiKhamDoiDichVu_nemLoi() {
        PhieuKham nguon = new PhieuKhamDAO().layTheoMa(maPK);
        LichHen lh = new LichHen(null, NGAY.atTime(9, 0), TrangThaiLichHen.DA_DAT, null, null,
                new KhachHangDAO().layTheoMa(maKH), nguon.getLichHen().getNhaSi(),
                new DichVuDAO().layTheoMa("DV997"), nguon);

        BusinessException ex = assertThrows(BusinessException.class, () -> dao.them(lh));

        assertEquals("Tái khám phải giữ khách hàng, nha sĩ và dịch vụ nguồn.", ex.getMessage());
    }

    @Test
    void doiLichTaiKham_raNgoaiKhoang_nemLoi() {
        LichHen lh = dao.taoTaiKham(maPK, NGAY.atTime(9, 0), null);
        lh.setNgayGioHen(HOM_NAY.plusDays(30).atTime(9, 0));

        BusinessException ex = assertThrows(BusinessException.class, () -> dao.capNhat(lh));

        assertEquals("Ngày tái khám ngoài khoảng cho phép.", ex.getMessage());
    }

    @Test
    void doiLichTaiKham_trongKhoang_thanhCongVaVanGanPhieuNguon() throws SQLException {
        LichHen lh = dao.taoTaiKham(maPK, NGAY.atTime(9, 0), null);
        lh.setNgayGioHen(HOM_NAY.plusDays(7).atTime(10, 0));

        assertTrue(dao.capNhat(lh));
        assertEquals(maPK, chuoi("SELECT maPKNguon FROM LichHen WHERE maLH=?", lh.getMaLH()));
    }

    // ------------------------------------------------------------------ khoangNgayTaiKham

    @Test
    void khoangNgayTaiKham_tinhTuNgayLapPhieu() {
        LichHenDAO.KhoangNgay k = dao.khoangNgayTaiKham(maPK);

        assertEquals(HOM_NAY.plusDays(7), k.tu());
        assertEquals(HOM_NAY.plusDays(14), k.den());
    }

    @Test
    void khoangNgayTaiKham_phieuChuaHoanTat_nemLoi() {
        assertThrows(BusinessException.class, () -> dao.khoangNgayTaiKham("PK999999999"));
    }

    // ------------------------------------------------------------------ goiYKhungTrongTaiKham

    @Test
    void goiYKhungTrongTaiKham_trongKhoang_coKhungGioCuaNhaSiCu() {
        List<LocalDateTime> ds = dao.goiYKhungTrongTaiKham(maPK, NGAY);

        assertFalse(ds.isEmpty());
        assertTrue(ds.contains(NGAY.atTime(9, 0)));
    }

    @Test
    void goiYKhungTrongTaiKham_khungDaDatKhongConDuocGoiY() {
        dao.taoTaiKham(maPK, NGAY.atTime(9, 0), null);
        List<LocalDateTime> ds = dao.goiYKhungTrong(maKH, NV, "DV998", NGAY);

        assertFalse(ds.contains(NGAY.atTime(9, 0)));
    }

    @Test
    void goiYKhungTrongTaiKham_ngoaiKhoang_nemLoi() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> dao.goiYKhungTrongTaiKham(maPK, HOM_NAY.plusDays(30)));

        assertEquals("Ngày tái khám ngoài khoảng cho phép.", ex.getMessage());
    }

    // ------------------------------------------------------------------ hỗ trợ

    private void chenLichKhac(LocalDateTime gio) {
        NhanVien nhaSi = new NhanVien(NV, "Nha sĩ kiểm thử", true, "0398682231",
                "macvestdinet+nhasi@gmail.com", "Nha sĩ");
        String maKH2 = maKH;
        LichHen lh = new LichHen(null, gio, TrangThaiLichHen.DA_DAT, null, null,
                new KhachHangDAO().layTheoMa(maKH2), nhaSi, new DichVuDAO().layTheoMa("DV998"), null);
        dao.them(lh);
    }

    private int soTaiKham() throws SQLException {
        return Integer.parseInt(chuoi("SELECT COUNT(*) FROM LichHen WHERE maPKNguon=?", maPK));
    }

    private void themCa(Connection c, LocalDate ngay) throws SQLException {
        String ma = "LLV" + String.format("%05d", ThreadLocalRandom.current().nextInt(70_000, 99_000));
        maLLV.add(ma);
        JdbcHelper.update(c, "INSERT INTO LichLamViec(maLLV,maNV,ngayTruc,caTruc,gioBatDau,gioKetThuc,trangThai)"
                + " VALUES(?,?,?,'CA_NGAY','07:00','17:30','DA_PHAN_CONG')", ma, NV, ngay);
    }

    private static Connection ketNoi() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    private static String chuoi(String sql, String ma) throws SQLException {
        try (Connection c = ketNoi(); var ps = c.prepareStatement(sql)) {
            ps.setString(1, ma);
            try (var r = ps.executeQuery()) {
                return r.next() ? r.getString(1) : null;
            }
        }
    }
}
