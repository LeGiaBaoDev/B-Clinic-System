-- Nâng cấp cấu trúc; KHÔNG xóa dữ liệu. Dọn dữ liệu là thao tác riêng có xác nhận của người dùng.
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO
ALTER TABLE LoaiDichVu ADD loaiGTGT NVARCHAR(20) NOT NULL
    CONSTRAINT DF_LoaiDV_loaiGTGT DEFAULT N'CHIU_THUE';
GO
UPDATE LoaiDichVu SET loaiGTGT=N'KHONG_CHIU_THUE' WHERE tenLoaiDV=N'Điều trị bệnh lý';
ALTER TABLE LoaiDichVu ADD CONSTRAINT CK_LoaiDV_loaiGTGT CHECK
    (loaiGTGT=N'CHIU_THUE' OR (loaiGTGT=N'KHONG_CHIU_THUE' AND mucThueGTGT=0));
ALTER TABLE ChiTietPhieuKham ADD loaiGTGT NVARCHAR(20) NOT NULL
    CONSTRAINT DF_CTPK_loaiGTGT DEFAULT N'CHIU_THUE';
GO
DISABLE TRIGGER trg_KhoaChiTietPhieuKham ON ChiTietPhieuKham;
UPDATE ct SET loaiGTGT=l.loaiGTGT FROM ChiTietPhieuKham ct
JOIN DichVu d ON d.maDV=ct.maDV JOIN LoaiDichVu l ON l.maLoaiDV=d.maLoaiDV;
ENABLE TRIGGER trg_KhoaChiTietPhieuKham ON ChiTietPhieuKham;
ALTER TABLE ChiTietPhieuKham ADD CONSTRAINT CK_CTPK_loaiGTGT CHECK
    (loaiGTGT=N'CHIU_THUE' OR (loaiGTGT=N'KHONG_CHIU_THUE' AND mucThueGTGT=0));
ALTER TABLE ChiTietPhieuKham ADD CONSTRAINT UQ_CTPK_Nguon UNIQUE(maCTPK,maPK,maDV);
ALTER TABLE ChiTietPhieuKham ADD CONSTRAINT UQ_CTPK_Phieu UNIQUE(maCTPK,maPK);
GO
CREATE TABLE VoucherDichVu (
    maVoucher VARCHAR(15) NOT NULL,
    maDV VARCHAR(15) NOT NULL,
    CONSTRAINT PK_VoucherDichVu PRIMARY KEY(maVoucher,maDV),
    CONSTRAINT FK_VoucherDV_Voucher FOREIGN KEY(maVoucher) REFERENCES Voucher(maVoucher),
    CONSTRAINT FK_VoucherDV_DichVu FOREIGN KEY(maDV) REFERENCES DichVu(maDV)
);
CREATE INDEX IX_VoucherDV_DichVu ON VoucherDichVu(maDV,maVoucher);
-- Chỉ ghi lại phạm vi đã thực sự xuất hiện của các hóa đơn cũ.
INSERT VoucherDichVu(maVoucher,maDV)
SELECT DISTINCT h.maVoucher,ct.maDV FROM HoaDon h JOIN ChiTietPhieuKham ct ON ct.maPK=h.maPK WHERE h.maVoucher IS NOT NULL;
ALTER TABLE HoaDon DROP CONSTRAINT CHK_HoaDon_Voucher;
ALTER TABLE HoaDon ADD
    maKH VARCHAR(15) NULL,
    tenKhachHang NVARCHAR(100) NULL,
    sdtKhachHang VARCHAR(15) NULL,
    diaChiKhachHang NVARCHAR(255) NULL,
    tongTienHang DECIMAL(18,2) NOT NULL CONSTRAINT DF_HD_tongHang DEFAULT 0,
    tongThueGTGT DECIMAL(18,2) NOT NULL CONSTRAINT DF_HD_tongThue DEFAULT 0,
    tongThanhToan DECIMAL(18,2) NOT NULL CONSTRAINT DF_HD_tongThanhToan DEFAULT 0,
    tongDaThu DECIMAL(18,2) NOT NULL CONSTRAINT DF_HD_tongDaThu DEFAULT 0,
    trangThaiThanhToan NVARCHAR(24) NOT NULL CONSTRAINT DF_HD_trangThaiThanhToan DEFAULT N'CHUA_THANH_TOAN';
GO
UPDATE h SET maKH=k.maKH,tenKhachHang=k.tenKH,sdtKhachHang=k.sdt,diaChiKhachHang=k.diaChi
FROM HoaDon h JOIN PhieuKham p ON p.maPK=h.maPK
JOIN HoSoBenhAn s ON s.maHSBA=p.maHSBA JOIN KhachHang k ON k.maKH=s.maKH;
ALTER TABLE HoaDon ALTER COLUMN maKH VARCHAR(15) NOT NULL;
ALTER TABLE HoaDon ALTER COLUMN tenKhachHang NVARCHAR(100) NOT NULL;
ALTER TABLE HoaDon ALTER COLUMN sdtKhachHang VARCHAR(15) NOT NULL;
ALTER TABLE HoaDon ADD CONSTRAINT FK_HD_KhachHang FOREIGN KEY(maKH) REFERENCES KhachHang(maKH);
ALTER TABLE HoaDon ADD CONSTRAINT UQ_HD_Phieu UNIQUE(maHD,maPK);
ALTER TABLE HoaDon ADD conNo AS (tongThanhToan-tongDaThu) PERSISTED;
CREATE TABLE ChiTietHoaDon (
    maCTHD INT IDENTITY(1,1) NOT NULL,
    maHD VARCHAR(15) NOT NULL,
    maPK VARCHAR(15) NOT NULL,
    maCTPK INT NOT NULL,
    maDV VARCHAR(15) NOT NULL,
    tenDichVu NVARCHAR(150) NOT NULL,
    donViTinh NVARCHAR(30) NOT NULL,
    soLuong INT NOT NULL,
    donGia DECIMAL(18,2) NOT NULL,
    loaiGTGT NVARCHAR(20) NOT NULL,
    thueSuatGTGT DECIMAL(5,2) NULL,
    tienHang DECIMAL(18,2) NOT NULL,
    tienGTGT DECIMAL(18,2) NOT NULL,
    tienGiam DECIMAL(18,2) NOT NULL CONSTRAINT DF_CTHD_tienGiam DEFAULT 0,
    thanhTien DECIMAL(18,2) NOT NULL,
    maVoucher VARCHAR(15) NULL,
    tenKhuyenMai NVARCHAR(100) NULL,
    loaiGiam BIT NULL,
    giaTriGiam DECIMAL(18,2) NULL,
    CONSTRAINT PK_ChiTietHoaDon PRIMARY KEY(maCTHD),
    CONSTRAINT UQ_CTHD_DieuTri UNIQUE(maCTPK),
    CONSTRAINT FK_CTHD_HoaDon FOREIGN KEY(maHD,maPK) REFERENCES HoaDon(maHD,maPK),
    CONSTRAINT FK_CTHD_DieuTri FOREIGN KEY(maCTPK,maPK,maDV) REFERENCES ChiTietPhieuKham(maCTPK,maPK,maDV),
    CONSTRAINT FK_CTHD_VoucherDV FOREIGN KEY(maVoucher,maDV) REFERENCES VoucherDichVu(maVoucher,maDV),
    CONSTRAINT CK_CTHD_SoLuongGia CHECK(soLuong>0 AND donGia>0),
    CONSTRAINT CK_CTHD_Thue CHECK(
        (loaiGTGT=N'KHONG_CHIU_THUE' AND thueSuatGTGT IS NULL AND tienGTGT=0)
        OR (loaiGTGT=N'CHIU_THUE' AND thueSuatGTGT IS NOT NULL AND thueSuatGTGT BETWEEN 0 AND 100
            AND tienGTGT=ROUND(tienHang*thueSuatGTGT/100,0))),
    CONSTRAINT CK_CTHD_Tien CHECK(tienHang=soLuong*donGia AND tienGiam>=0
        AND tienGiam<=tienHang+tienGTGT AND thanhTien=tienHang+tienGTGT-tienGiam),
    CONSTRAINT CK_CTHD_KhuyenMai CHECK(
        (maVoucher IS NULL AND tienGiam=0 AND tenKhuyenMai IS NULL AND loaiGiam IS NULL AND giaTriGiam IS NULL)
        OR (maVoucher IS NOT NULL AND tenKhuyenMai IS NOT NULL AND loaiGiam IS NOT NULL
            AND giaTriGiam>0 AND (loaiGiam=0 OR giaTriGiam<=100)))
);
CREATE INDEX IX_CTHD_HoaDon ON ChiTietHoaDon(maHD) INCLUDE(tienHang,tienGTGT,tienGiam,thanhTien);
CREATE INDEX IX_CTHD_DichVu ON ChiTietHoaDon(maDV,maVoucher);
GO
-- Phân bổ giảm cũ bằng chênh lệch tổng lũy kế, bảo toàn chính xác tổng giảm.
;WITH Dong AS (
 SELECT h.maHD,h.maPK,h.maVoucher,h.tongKhuyenMai,ct.maCTPK,ct.maDV,ct.donViTinh,ct.soLuong,ct.donGia,ct.mucThueGTGT,ct.thanhTien,ct.loaiGTGT,d.tenDV,
        CAST(ct.thanhTien+ROUND(ct.thanhTien*ct.mucThueGTGT/100,0) AS DECIMAL(18,2)) AS sauThue,
        v.tenVoucher,v.loaiGiam,v.giaGiam
 FROM HoaDon h JOIN ChiTietPhieuKham ct ON ct.maPK=h.maPK
 JOIN DichVu d ON d.maDV=ct.maDV LEFT JOIN Voucher v ON v.maVoucher=h.maVoucher
), Tong AS (
 SELECT *,SUM(sauThue) OVER(PARTITION BY maHD) AS tong,
     SUM(sauThue) OVER(PARTITION BY maHD ORDER BY maCTPK ROWS UNBOUNDED PRECEDING) AS luyKe FROM Dong
), Giam AS (
 SELECT *,CAST(CASE WHEN tong=0 THEN 0 ELSE
     ROUND(tongKhuyenMai*(luyKe/tong),2)-ROUND(tongKhuyenMai*((luyKe-sauThue)/tong),2) END AS DECIMAL(18,2)) AS giam FROM Tong
)
INSERT ChiTietHoaDon(maHD,maPK,maCTPK,maDV,tenDichVu,donViTinh,soLuong,donGia,
 loaiGTGT,thueSuatGTGT,tienHang,tienGTGT,tienGiam,thanhTien,maVoucher,tenKhuyenMai,loaiGiam,giaTriGiam)
SELECT maHD,maPK,maCTPK,maDV,tenDV,donViTinh,soLuong,donGia,loaiGTGT,
 CASE WHEN loaiGTGT=N'KHONG_CHIU_THUE' THEN NULL ELSE mucThueGTGT END,
 thanhTien,ROUND(thanhTien*mucThueGTGT/100,0),giam,sauThue-giam,
 maVoucher,tenVoucher,loaiGiam,giaGiam FROM Giam;
UPDATE h SET tongTienHang=COALESCE(t.hang,0),tongThueGTGT=COALESCE(t.thue,0),
 tongKhuyenMai=COALESCE(t.giam,0),tongThanhToan=COALESCE(t.tong,0)
FROM HoaDon h OUTER APPLY(SELECT SUM(tienHang) hang,SUM(tienGTGT) thue,SUM(tienGiam) giam,SUM(thanhTien) tong FROM ChiTietHoaDon d WHERE d.maHD=h.maHD)t;
GO
CREATE TABLE PhieuThu (
    maPT VARCHAR(15) NOT NULL,
    maHD VARCHAR(15) NOT NULL,
    ngayThu DATETIME2(0) NOT NULL CONSTRAINT DF_PT_ngayThu DEFAULT SYSDATETIME(),
    soTienThu DECIMAL(18,2) NOT NULL,
    hinhThucThanhToan NVARCHAR(30) NOT NULL,
    maNVThu VARCHAR(15) NOT NULL,
    maThamChieu NVARCHAR(100) NULL,
    khoaYeuCau VARCHAR(100) NOT NULL,
    ghiChu NVARCHAR(255) NULL,
    CONSTRAINT PK_PhieuThu PRIMARY KEY(maPT),
    CONSTRAINT FK_PT_HoaDon FOREIGN KEY(maHD) REFERENCES HoaDon(maHD),
    CONSTRAINT FK_PT_NhanVien FOREIGN KEY(maNVThu) REFERENCES NhanVien(maNV),
    CONSTRAINT UQ_PT_YeuCau UNIQUE(khoaYeuCau),
    CONSTRAINT CK_PT_Ma CHECK(maPT LIKE 'PT[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]' AND LEN(maPT)=11),
    CONSTRAINT CK_PT_Tien CHECK(soTienThu>0),
    CONSTRAINT CK_PT_HinhThuc CHECK(hinhThucThanhToan IN(N'Tiền mặt',N'Chuyển khoản')),
    CONSTRAINT CK_PT_YeuCau CHECK(LEN(LTRIM(RTRIM(khoaYeuCau)))>0)
);
CREATE INDEX IX_PT_HoaDon ON PhieuThu(maHD,ngayThu) INCLUDE(soTienThu);
CREATE INDEX IX_PT_NgayThu ON PhieuThu(ngayThu) INCLUDE(soTienThu,maNVThu,hinhThucThanhToan);
INSERT PhieuThu(maPT,maHD,ngayThu,soTienThu,hinhThucThanhToan,maNVThu,khoaYeuCau,ghiChu)
SELECT 'PT'+SUBSTRING(maHD,3,9),maHD,ngayThanhToan,tongThanhToan,hinhThucThanhToan,maNV_ThuNgan,
 'migration:'+maHD,N'Chuyển từ hóa đơn đã thanh toán trước V4.' FROM HoaDon WHERE trangThai=1 AND tongThanhToan>0;
UPDATE HoaDon SET tongDaThu=CASE WHEN trangThai=1 THEN tongThanhToan ELSE 0 END,
 trangThaiThanhToan=CASE WHEN trangThai=1 THEN N'DA_THANH_TOAN' ELSE N'CHUA_THANH_TOAN' END;
ALTER TABLE HoaDon ADD CONSTRAINT CK_HD_TongTien CHECK(tongTienHang>=0 AND tongThueGTGT>=0
 AND tongKhuyenMai>=0 AND tongKhuyenMai<=tongTienHang+tongThueGTGT
 AND tongThanhToan=tongTienHang+tongThueGTGT-tongKhuyenMai AND tongDaThu>=0 AND tongDaThu<=tongThanhToan);
ALTER TABLE HoaDon ADD CONSTRAINT CK_HD_TrangThaiThu CHECK(
 (trangThaiThanhToan=N'CHUA_THANH_TOAN' AND trangThai=0 AND tongDaThu=0)
 OR (trangThaiThanhToan=N'THANH_TOAN_MOT_PHAN' AND trangThai=0 AND tongDaThu>0 AND tongDaThu<tongThanhToan)
 OR (trangThaiThanhToan=N'DA_THANH_TOAN' AND trangThai=1 AND tongDaThu=tongThanhToan));
CREATE INDEX IX_HD_KhachNgay ON HoaDon(maKH,ngayLap DESC);
CREATE INDEX IX_HD_CongNo ON HoaDon(trangThaiThanhToan,ngayLap DESC) INCLUDE(tongThanhToan,tongDaThu,maKH);
GO
ALTER TABLE PhieuKham ADD maNV_NhaSiPhuTrach VARCHAR(15) NULL,ngayBatDauKham DATETIME2(0) NULL,ngayHoanTat DATETIME2(0) NULL;
ALTER TABLE PhieuKham ADD CONSTRAINT FK_PK_NhaSiPhuTrach FOREIGN KEY(maNV_NhaSiPhuTrach) REFERENCES NhanVien(maNV);
GO
DISABLE TRIGGER trg_KhoaDuLieuPhieuKhamHoanTat ON PhieuKham;
UPDATE p SET maNV_NhaSiPhuTrach=l.maNV FROM PhieuKham p JOIN LichHen l ON l.maLH=p.maLH;
ENABLE TRIGGER trg_KhoaDuLieuPhieuKhamHoanTat ON PhieuKham;
ALTER TABLE PhieuKham ADD CONSTRAINT CK_PK_ThoiGianKham CHECK(
 (ngayBatDauKham IS NULL OR ngayBatDauKham>=ngayLap)
 AND (ngayHoanTat IS NULL OR ngayHoanTat>=ngayLap)
 AND (ngayBatDauKham IS NULL OR ngayHoanTat IS NULL OR ngayHoanTat>=ngayBatDauKham));
ALTER TABLE LichHen ADD maCTPKNguon INT NULL,thoiLuongDuKien INT NULL;
GO
UPDATE l SET thoiLuongDuKien=d.thoiLuong FROM LichHen l JOIN DichVu d ON d.maDV=l.maDV;
ALTER TABLE LichHen ADD CONSTRAINT CK_LH_ThoiLuong CHECK(thoiLuongDuKien IS NULL OR thoiLuongDuKien>0);
ALTER TABLE LichHen ADD CONSTRAINT CK_LH_ChiTietNguon CHECK(maCTPKNguon IS NULL OR maPKNguon IS NOT NULL);
ALTER TABLE LichHen ADD CONSTRAINT FK_LH_ChiTietNguon FOREIGN KEY(maCTPKNguon,maPKNguon) REFERENCES ChiTietPhieuKham(maCTPK,maPK);
CREATE INDEX IX_LH_NhaSiNgay ON LichHen(maNV,ngayGioHen) INCLUDE(trangThai,maKH,maDV,thoiLuongDuKien);
CREATE INDEX IX_PK_NhaSiNgay ON PhieuKham(maNV_NhaSiPhuTrach,ngayLap) INCLUDE(trangThai,ngayBatDauKham,ngayHoanTat);
CREATE UNIQUE INDEX UX_LH_TaiKhamChiTiet ON LichHen(maCTPKNguon)
 WHERE maCTPKNguon IS NOT NULL AND trangThai<>'DA_HUY' AND trangThai<>'KHONG_DEN';
GO
CREATE OR ALTER TRIGGER trg_CTHD_DongBoTong ON ChiTietHoaDon AFTER INSERT,UPDATE,DELETE AS
BEGIN
 SET NOCOUNT ON;
 DECLARE @lock BIGINT;
 SELECT @lock=COUNT_BIG(*) FROM HoaDon h WITH(UPDLOCK,HOLDLOCK)
 WHERE h.maHD IN(SELECT maHD FROM inserted UNION SELECT maHD FROM deleted);
 IF EXISTS(SELECT 1 FROM HoaDon h WHERE (h.tongDaThu>0 OR h.trangThai=1)
     AND h.maHD IN(SELECT maHD FROM inserted UNION SELECT maHD FROM deleted))
     THROW 51601,N'Hóa đơn đã thu tiền, không được sửa dòng dịch vụ hoặc khuyến mãi.',1;
 UPDATE h SET tongTienHang=COALESCE(s.hang,0),tongThueGTGT=COALESCE(s.thue,0),
   tongKhuyenMai=COALESCE(s.giam,0),tongThanhToan=COALESCE(s.tong,0)
 FROM HoaDon h OUTER APPLY(SELECT SUM(d.tienHang) hang,SUM(d.tienGTGT) thue,
     SUM(d.tienGiam) giam,SUM(d.thanhTien) tong FROM ChiTietHoaDon d WHERE d.maHD=h.maHD)s
 WHERE h.maHD IN(SELECT maHD FROM inserted UNION SELECT maHD FROM deleted);
END;
GO
CREATE OR ALTER TRIGGER trg_PT_ThuNhieuLan ON PhieuThu AFTER INSERT,UPDATE,DELETE AS
BEGIN
 SET NOCOUNT ON;
 IF EXISTS(SELECT 1 FROM deleted) THROW 51602,N'Phiếu thu đã ghi nhận không được sửa/xóa; cần nghiệp vụ điều chỉnh.',1;
 DECLARE @lock BIGINT;
 SELECT @lock=COUNT_BIG(*) FROM HoaDon h WITH(UPDLOCK,HOLDLOCK) JOIN(SELECT DISTINCT maHD FROM inserted)i ON i.maHD=h.maHD;
 IF EXISTS(SELECT 1 FROM inserted i JOIN HoaDon h ON h.maHD=i.maHD JOIN PhieuKham p ON p.maPK=h.maPK
    WHERE i.ngayThu<h.ngayLap OR p.trangThai<>'HOAN_TAT') THROW 51603,N'Phiếu chưa hoàn tất hoặc ngày thu không hợp lệ.',1;
 IF EXISTS(SELECT 1 FROM inserted i WHERE NOT EXISTS(SELECT 1 FROM TaiKhoan t WHERE t.maNV=i.maNVThu AND t.vaiTro IN('QUANLY','LETAN') AND t.trangThai=1))
    THROW 51604,N'Nhân viên không có quyền thu tiền.',1;
 IF EXISTS(SELECT 1 FROM HoaDon h JOIN(SELECT maHD,SUM(soTienThu) tong FROM PhieuThu GROUP BY maHD)t ON t.maHD=h.maHD
    WHERE h.maHD IN(SELECT maHD FROM inserted) AND t.tong>h.tongThanhToan) THROW 51605,N'Số tiền thu vượt công nợ.',1;
 UPDATE h SET tongDaThu=t.tong,
    trangThaiThanhToan=CASE WHEN t.tong=h.tongThanhToan THEN N'DA_THANH_TOAN' ELSE N'THANH_TOAN_MOT_PHAN' END,
    trangThai=CASE WHEN t.tong=h.tongThanhToan THEN 1 ELSE 0 END,
    ngayThanhToan=CASE WHEN t.tong=h.tongThanhToan THEN lastThu.ngayThu ELSE NULL END,
    hinhThucThanhToan=CASE WHEN t.tong=h.tongThanhToan THEN lastThu.hinhThucThanhToan ELSE NULL END,
    maNV_ThuNgan=CASE WHEN t.tong=h.tongThanhToan THEN lastThu.maNVThu ELSE NULL END
 FROM HoaDon h CROSS APPLY(SELECT SUM(soTienThu) tong FROM PhieuThu WHERE maHD=h.maHD)t
 CROSS APPLY(SELECT TOP 1 ngayThu,hinhThucThanhToan,maNVThu FROM PhieuThu WHERE maHD=h.maHD ORDER BY ngayThu DESC,maPT DESC)lastThu
 WHERE h.maHD IN(SELECT maHD FROM inserted);
END;
GO
CREATE OR ALTER TRIGGER trg_HD_KiemTraBanChup ON HoaDon AFTER INSERT,UPDATE AS
BEGIN
 SET NOCOUNT ON;
 IF EXISTS(SELECT 1 FROM inserted i JOIN PhieuKham p ON p.maPK=i.maPK JOIN HoSoBenhAn s ON s.maHSBA=p.maHSBA WHERE i.maKH<>s.maKH)
    THROW 51606,N'Khách hàng hóa đơn không khớp phiếu khám.',1;
 IF EXISTS(SELECT 1 FROM inserted i OUTER APPLY(SELECT SUM(tienHang) hang,SUM(tienGTGT) thue,SUM(tienGiam) giam,SUM(thanhTien) tong FROM ChiTietHoaDon d WHERE d.maHD=i.maHD)s
    WHERE i.tongTienHang<>COALESCE(s.hang,0) OR i.tongThueGTGT<>COALESCE(s.thue,0) OR i.tongKhuyenMai<>COALESCE(s.giam,0) OR i.tongThanhToan<>COALESCE(s.tong,0))
    THROW 51607,N'Tổng hóa đơn không khớp các dòng dịch vụ.',1;
 IF EXISTS(SELECT 1 FROM inserted i OUTER APPLY(SELECT SUM(soTienThu) tong FROM PhieuThu p WHERE p.maHD=i.maHD)s WHERE i.tongDaThu<>COALESCE(s.tong,0))
    THROW 51608,N'Tổng đã thu không khớp phiếu thu.',1;
 IF EXISTS(SELECT 1 FROM inserted i JOIN deleted d ON d.maHD=i.maHD WHERE (d.tongDaThu>0 OR d.trangThai=1) AND EXISTS(
     SELECT i2.maPK,i2.maKH,i2.tenKhachHang,i2.sdtKhachHang,i2.diaChiKhachHang,i2.ngayLap,i2.tongTienHang,i2.tongThueGTGT,i2.tongKhuyenMai,i2.tongThanhToan FROM inserted i2 WHERE i2.maHD=i.maHD
     EXCEPT SELECT d2.maPK,d2.maKH,d2.tenKhachHang,d2.sdtKhachHang,d2.diaChiKhachHang,d2.ngayLap,d2.tongTienHang,d2.tongThueGTGT,d2.tongKhuyenMai,d2.tongThanhToan FROM deleted d2 WHERE d2.maHD=i.maHD))
    THROW 51609,N'Không được thay đổi nội dung hóa đơn đã thu tiền.',1;
END;
GO
CREATE OR ALTER TRIGGER trg_LH_NguonDieuTri ON LichHen AFTER INSERT,UPDATE AS
BEGIN
 SET NOCOUNT ON;
 IF EXISTS(SELECT 1 FROM inserted i JOIN PhieuKham p ON p.maPK=i.maPKNguon
   JOIN HoSoBenhAn s ON s.maHSBA=p.maHSBA LEFT JOIN LichHen l ON l.maLH=p.maLH
   LEFT JOIN ChiTietPhieuKham ct ON ct.maCTPK=i.maCTPKNguon
   WHERE p.trangThai<>'HOAN_TAT' OR s.maKH<>i.maKH
      OR (i.maCTPKNguon IS NOT NULL AND (ct.maNV_NhaSi<>i.maNV OR ct.maDV<>i.maDV))
      OR (i.maCTPKNguon IS NULL AND (COALESCE(p.maNV_NhaSiPhuTrach,l.maNV,'')<>i.maNV OR COALESCE(l.maDV,'')<>i.maDV)))
    THROW 51610,N'Tái khám phải giữ bệnh nhân, dịch vụ và bác sĩ đã điều trị.',1;
END;
GO
