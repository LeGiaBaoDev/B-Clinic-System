-- Dữ liệu test nhanh cho UC024: mở phiếu khám hôm nay của NV002.
-- Chạy thủ công trên BClinic_Dev sau khi app đã chạy Flyway V1/V2.
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() <> N'BClinic_Dev'
    THROW 50000, N'Chỉ chạy trên BClinic_Dev.', 1;

DECLARE @maNV     VARCHAR(15) = 'NV002';   -- nha sĩ đang đăng nhập (hieu.nhasi)
DECLARE @maLeTan  VARCHAR(15) = 'NV003';   -- lễ tân lập phiếu
DECLARE @maLH     VARCHAR(15) = 'LH999999901';
DECLARE @maPK     VARCHAR(15) = 'PK999999901';

DECLARE @maKH   VARCHAR(15) = (
    SELECT TOP 1 kh.maKH FROM KhachHang kh
    JOIN HoSoBenhAn hs ON hs.maKH = kh.maKH
    WHERE EXISTS (SELECT 1 FROM LichHen lh WHERE lh.maKH = kh.maKH AND lh.maNV = @maNV)
    ORDER BY kh.maKH);
DECLARE @maHSBA VARCHAR(15) = (SELECT maHSBA FROM HoSoBenhAn WHERE maKH = @maKH);
DECLARE @maDV   VARCHAR(15) = (SELECT TOP 1 maDV FROM DichVu WHERE trangThai = 1 ORDER BY maDV);
DECLARE @thoiLuong INT = (SELECT thoiLuong FROM DichVu WHERE maDV = @maDV);

IF @maKH IS NULL OR @maHSBA IS NULL OR @maDV IS NULL
    THROW 50001, N'Thiếu dữ liệu nền (bệnh nhân/hồ sơ/dịch vụ). Hãy chạy scripts/seed_demo.sql trước.', 1;
IF NOT EXISTS (SELECT 1 FROM NhanVien WHERE maNV = @maNV)
    THROW 50002, N'Không có nhân viên NV002.', 1;
IF NOT EXISTS (SELECT 1 FROM NhanVien WHERE maNV = @maLeTan)
    THROW 50003, N'Không có nhân viên NV003.', 1;

-- Tìm khung giờ trống cho cả bệnh nhân và nha sĩ trong hôm nay.
DECLARE @gio DATETIME2(0) = DATEADD(HOUR, 8, CAST(CAST(SYSDATETIME() AS DATE) AS DATETIME2(0)));
WHILE EXISTS (
    SELECT 1 FROM LichHen lh
    JOIN DichVu dv ON dv.maDV = lh.maDV
    WHERE lh.trangThai IN ('DA_DAT','DA_DEN')
      AND (lh.maNV = @maNV OR lh.maKH = @maKH)
      AND @gio < DATEADD(MINUTE, dv.thoiLuong + 15, lh.ngayGioHen)
      AND lh.ngayGioHen < DATEADD(MINUTE, @thoiLuong + 15, @gio)
)
BEGIN
    SET @gio = DATEADD(MINUTE, 30, @gio);
    IF @gio >= DATEADD(DAY, 1, CAST(CAST(SYSDATETIME() AS DATE) AS DATETIME2(0)))
        THROW 50004, N'Hết khung giờ trống trong hôm nay để tạo lịch test.', 1;
END

IF NOT EXISTS (SELECT 1 FROM LichHen WHERE maLH = @maLH)
    INSERT INTO LichHen (maLH, maKH, maNV, maDV, ngayGioHen, trangThai, ghiChu)
    VALUES (@maLH, @maKH, @maNV, @maDV, @gio, 'DA_DEN', N'[TEST] Lịch để mở phiếu khám hôm nay');

IF NOT EXISTS (SELECT 1 FROM PhieuKham WHERE maPK = @maPK)
    INSERT INTO PhieuKham (maPK, maHSBA, maLH, maNV_LapPhieu, ngayLap, trangThai)
    VALUES (@maPK, @maHSBA, @maLH, @maLeTan, SYSDATETIME(), 'CHO_KHAM');

SELECT @maKH AS BenhNhanTest, @maHSBA AS HoSo, @maLH AS LichHen, @maPK AS PhieuKham,
       FORMAT(@gio, 'HH:mm') AS GioTest;
