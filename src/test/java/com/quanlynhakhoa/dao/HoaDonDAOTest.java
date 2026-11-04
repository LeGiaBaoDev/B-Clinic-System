package com.quanlynhakhoa.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quanlynhakhoa.config.DatabaseConnection;
import com.quanlynhakhoa.dao.support.JdbcHelper;
import com.quanlynhakhoa.entity.HoaDon;
import com.quanlynhakhoa.entity.enums.VaiTro;
import com.quanlynhakhoa.exception.BusinessException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HoaDonDAOTest {

    private final HoaDonDAO dao = new HoaDonDAO();
    private String maKH;
    private String maHS;
    private String maLLV;
    private String maLH;
    private String maPK;
    private String maHD;

    @BeforeAll
    static void chuanBi() {
        DaoTestSupport.yeuCauDatabaseTest();
    }

    @BeforeEach
    void taoDuLieu() throws SQLException {
        DaoTestSupport.dangNhap(VaiTro.LETAN);
        int so = ThreadLocalRandom.current().nextInt(100_000, 1_000_000);
        maKH = "KH" + String.format("%05d", so % 100_000);
        maHS = "HS" + String.format("%05d", (so + 1) % 100_000);
        maLLV = "LLV" + String.format("%05d", (so + 2) % 100_000);
        maLH = "LH" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                + String.format("%03d", so % 1000);
        maPK = "PK" + String.format("%09d", so);
        maHD = "HD" + String.format("%09d", so);
        try (Connection c = DatabaseConnection.getInstance().getConnection()) {
            JdbcHelper.update(c, "IF NOT EXISTS(SELECT 1 FROM LoaiDichVu WHERE maLoaiDV='LDV99')"
                    + " INSERT INTO LoaiDichVu(maLoaiDV,tenLoaiDV,mucThueGTGT)"
                    + " VALUES('LDV99',N'Điều trị bệnh lý',0.00)");
            JdbcHelper.update(c, "IF NOT EXISTS(SELECT 1 FROM DichVu WHERE maDV='DV999')"
                    + " INSERT INTO DichVu(maDV,maLoaiDV,tenDV,donGia,thoiLuong,trangThai)"
                    + " VALUES('DV999','LDV99',N'Dịch vụ kiểm thử UC025',100000,30,1)");
            JdbcHelper.update(c, "IF NOT EXISTS(SELECT 1 FROM DichVu WHERE maDV='DV998')"
                    + " INSERT INTO DichVu(maDV,maLoaiDV,tenDV,donGia,thoiLuong,trangThai)"
                    + " VALUES('DV998','LDV99',N'Vật tư kiểm thử UC025',200000,30,1)");
            JdbcHelper.update(c, "INSERT INTO KhachHang(maKH,tenKH,gioiTinh,ngaySinh,sdt,email,diaChi)"
                    + " VALUES(?,N'Khách kiểm thử UC025',0,'1990-01-01','0912345678',?,N'Test')",
                    maKH, maKH.toLowerCase() + "@example.com");
            JdbcHelper.update(c, "INSERT INTO HoSoBenhAn(maHSBA,maKH) VALUES(?,?)", maHS, maKH);
            JdbcHelper.update(c, "INSERT INTO LichLamViec(maLLV,maNV,ngayTruc,caTruc,gioBatDau,"
                    + "gioKetThuc,trangThai) VALUES(?,'NV002',CAST(SYSDATETIME() AS DATE),"
                    + "'CA_NGAY','07:00','17:30','DA_PHAN_CONG')", maLLV);
            JdbcHelper.update(c, "INSERT INTO LichHen(maLH,maKH,maNV,maDV,ngayGioHen,trangThai)"
                    + " VALUES(?,?,'NV002','DV999',DATEADD(HOUR,10,CAST(CAST(SYSDATETIME() AS DATE) AS DATETIME2)),'DA_DEN')",
                    maLH, maKH);
            JdbcHelper.update(c, "INSERT INTO PhieuKham(maPK,maHSBA,maLH,maNV_LapPhieu,ngayLap,trangThai)"
                    + " VALUES(?,?,?,'NV003',SYSDATETIME(),'DANG_KHAM')", maPK, maHS, maLH);
            JdbcHelper.update(c, "INSERT INTO ChiTietPhieuKham(maPK,maDV,maNV_NhaSi,trieuChung,chanDoan,"
                    + "donViTinh,soLuong,donGia,mucThueGTGT)"
                    + " VALUES(?,'DV999','NV002',N'Đau răng',N'Sâu răng',N'Lần',1,100000,0)", maPK);
            JdbcHelper.update(c, "INSERT INTO ChiTietPhieuKham(maPK,maDV,maNV_NhaSi,trieuChung,chanDoan,"
                    + "donViTinh,soLuong,donGia,mucThueGTGT)"
                    + " VALUES(?,'DV998','NV002',N'Vật tư',N'Phục hồi',N'Cái',1,200000,5)", maPK);
            JdbcHelper.update(c, "UPDATE PhieuKham SET trangThai='HOAN_TAT' WHERE maPK=?", maPK);
            JdbcHelper.update(c, "INSERT INTO HoaDon(maHD,maPK,ngayLap,trangThai) VALUES(?,?,?,0)",
                    maHD, maPK, LocalDateTime.now());
        }
    }

    @AfterEach
    void don() throws SQLException {
        try (Connection c = DatabaseConnection.getInstance().getConnection()) {
            JdbcHelper.update(c, "DISABLE TRIGGER trg_KhoaChiTietPhieuKham ON ChiTietPhieuKham");
            JdbcHelper.update(c, "DISABLE TRIGGER trg_KhoaDuLieuPhieuKhamHoanTat ON PhieuKham");
            JdbcHelper.update(c, "DELETE FROM HoaDon WHERE maPK=?", maPK);
            JdbcHelper.update(c, "DELETE FROM PhieuKham WHERE maPK=?", maPK);
            JdbcHelper.update(c, "ENABLE TRIGGER trg_KhoaChiTietPhieuKham ON ChiTietPhieuKham");
            JdbcHelper.update(c, "ENABLE TRIGGER trg_KhoaDuLieuPhieuKhamHoanTat ON PhieuKham");
            JdbcHelper.update(c, "DELETE FROM LichHen WHERE maLH=?", maLH);
            JdbcHelper.update(c, "DELETE FROM LichLamViec WHERE maLLV=?", maLLV);
            JdbcHelper.update(c, "DELETE FROM HoSoBenhAn WHERE maHSBA=?", maHS);
            JdbcHelper.update(c, "DELETE FROM KhachHang WHERE maKH=?", maKH);
            JdbcHelper.update(c, "DELETE FROM Voucher WHERE maVoucher='VC999'");
            JdbcHelper.update(c, "DELETE FROM DichVu WHERE maDV IN ('DV998','DV999')");
            JdbcHelper.update(c, "DELETE FROM LoaiDichVu WHERE maLoaiDV='LDV99'");
        }
        DaoTestSupport.dangXuat();
    }

    @Test
    void thanhToanTienMat_tinhTienThoiVaCapNhatHoaDonLichHen() throws SQLException {
        BigDecimal thoi = dao.thanhToan(maHD, HoaDon.TIEN_MAT, new BigDecimal("500000"));

        assertEquals(0, new BigDecimal("190000").compareTo(thoi));
        assertEquals(1, dem("SELECT COUNT(*) FROM HoaDon WHERE maHD=? AND trangThai=1"
                + " AND maNV_ThuNgan='NV003' AND hinhThucThanhToan=N'Tiền mặt'", maHD));
        assertEquals("HOAN_TAT", chuoi("SELECT trangThai FROM LichHen WHERE maLH=?", maLH));
        assertEquals(0, dao.tinhTongTien(maHD).compareTo(new BigDecimal("310000")));
    }

    @Test
    void thanhToanLanHai_nemLoi() {
        dao.thanhToan(maHD, HoaDon.TIEN_MAT, new BigDecimal("310000"));

        assertThrows(BusinessException.class,
                () -> dao.thanhToan(maHD, HoaDon.TIEN_MAT, new BigDecimal("310000")));
    }

    @Test
    void chuyenKhoan_phaiDungTongTien() {
        assertThrows(BusinessException.class,
                () -> dao.thanhToan(maHD, HoaDon.CHUYEN_KHOAN, new BigDecimal("300000")));

        assertNotNull(dao.thanhToan(maHD, HoaDon.CHUYEN_KHOAN, new BigDecimal("310000")));
    }

    @Test
    void voucher_apDungVaThanhToanTrongCungTransaction() throws SQLException {
        taoVoucher();
        BigDecimal thoi = dao.thanhToan(maHD, HoaDon.TIEN_MAT, new BigDecimal("300000"), "VC999");

        assertEquals(0, new BigDecimal("21000").compareTo(thoi));
        assertEquals(1, dem("SELECT COUNT(*) FROM HoaDon WHERE maHD=? AND maVoucher='VC999'"
                + " AND tongKhuyenMai=31000", maHD));
    }

    @Test
    void layTheoPhieuKham_traVeHoaDonChuaThanhToan() {
        HoaDon hd = dao.layTheoPhieuKham(maPK);

        assertNotNull(hd);
        assertEquals(maHD, hd.getMaHD());
        assertNull(hd.getHinhThucThanhToan());
        assertTrue(!hd.isTrangThai());
    }

    private void taoVoucher() throws SQLException {
        try (Connection c = DatabaseConnection.getInstance().getConnection()) {
            JdbcHelper.update(c, "DELETE FROM Voucher WHERE maVoucher='VC999'");
            JdbcHelper.update(c, "INSERT INTO Voucher(maVoucher,tenVoucher,loaiGiam,giaGiam,"
                    + "giaTriGiamToiDa,ngayBatDau,ngayKetThuc,trangThai)"
                    + " VALUES('VC999',N'Giảm 10%',1,10,50000,?,?,1)",
                    LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        }
    }

    private static int dem(String sql, String ma) throws SQLException {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
                var ps = c.prepareStatement(sql)) {
            ps.setString(1, ma);
            try (var r = ps.executeQuery()) {
                r.next();
                return r.getInt(1);
            }
        }
    }

    private static String chuoi(String sql, String ma) throws SQLException {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
                var ps = c.prepareStatement(sql)) {
            ps.setString(1, ma);
            try (var r = ps.executeQuery()) {
                return r.next() ? r.getString(1) : null;
            }
        }
    }
}
