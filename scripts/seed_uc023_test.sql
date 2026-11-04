-- Dữ liệu test nhanh cho UC023: 1 phiếu khám hôm nay của NV002 (hieu.nhasi).
-- Chạy thủ công trên BClinic_Dev sau khi app đã chạy Flyway V1/V2.
-- Idempotent: chạy lại không tạo trùng.
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() <> N'BClinic_Dev'
    THROW 52000, N'Chỉ chạy trên BClinic_Dev.', 1;
IF NOT EXISTS (SELECT 1 FROM NhanVien WHERE maNV = 'NV002')
    THROW 52001, N'Thiếu nhân viên NV002. Hãy chạy seed_demo.sql trước.', 1;
IF NOT EXISTS (SELECT 1 FROM NhanVien WHERE maNV = 'NV003')
    THROW 52002, N'Thiếu nhân viên NV003. Hãy chạy seed_demo.sql trước.', 1;

DECLARE @ngay DATE = CAST(SYSDATETIME() AS DATE);
DECLARE @maKH  VARCHAR(15) = 'KH' + RIGHT('00000' + CAST(ABS(CHECKSUM(NEWID())) % 100000 AS VARCHAR(5)), 5);
DECLARE @maHS  VARCHAR(15) = 'HS' + RIGHT(@maKH, 5);
DECLARE @maLH  VARCHAR(15) = 'LH' + CONVERT(CHAR(6), @ngay, 12)
    + RIGHT('000' + CAST(ABS(CHECKSUM(NEWID())) % 1000 AS VARCHAR(3)), 3);
DECLARE @maPK  VARCHAR(15) = 'PK' + CONVERT(CHAR(6), @ngay, 12)
    + RIGHT('000' + CAST(ABS(CHECKSUM(NEWID())) % 1000 AS VARCHAR(3)), 3);
DECLARE @sdt   VARCHAR(15) = '09' + RIGHT('00000000'
    + CAST(ABS(CHECKSUM(NEWID())) % 100000000 AS VARCHAR(8)), 8);

IF NOT EXISTS (SELECT 1 FROM LoaiDichVu WHERE maLoaiDV = 'LDV01')
    INSERT INTO LoaiDichVu(maLoaiDV, tenLoaiDV, mucThueGTGT)
    VALUES ('LDV01', N'Điều trị bệnh lý', 0.00);

IF NOT EXISTS (SELECT 1 FROM DichVu WHERE maDV = 'DV002')
    INSERT INTO DichVu(maDV, maLoaiDV, tenDV, donGia, thoiLuong, trangThai)
    VALUES ('DV002', 'LDV01', N'Cạo vôi răng', 250000, 45, 1);

IF NOT EXISTS (SELECT 1 FROM KhachHang WHERE maKH = @maKH)
    INSERT INTO KhachHang(maKH, tenKH, gioiTinh, ngaySinh, sdt, email, diaChi)
    VALUES (@maKH, N'Bệnh nhân test UC023', 0, '1995-04-16',
            @sdt, 'test.uc023@example.com', N'Địa chỉ test');

IF NOT EXISTS (SELECT 1 FROM HoSoBenhAn WHERE maKH = @maKH)
    INSERT INTO HoSoBenhAn(maHSBA, maKH, tienSuBenh)
    VALUES (@maHS, @maKH, N'[TEST] Chưa ghi nhận bệnh nền hoặc dị ứng.');

IF NOT EXISTS (SELECT 1 FROM LichHen WHERE maLH = @maLH)
    INSERT INTO LichHen(maLH, maKH, maNV, maDV, ngayGioHen, trangThai, ghiChu)
    VALUES (@maLH, @maKH, 'NV002', 'DV002',
            DATEADD(HOUR, 15, CAST(@ngay AS DATETIME2)), 'DA_DEN',
            N'[TEST] Lịch hẹn kiểm thử UC023.');

IF NOT EXISTS (SELECT 1 FROM PhieuKham WHERE maPK = @maPK)
    INSERT INTO PhieuKham(maPK, maHSBA, maLH, maNV_LapPhieu, ngayLap, trangThai, loiDan)
    VALUES (@maPK, @maHS, @maLH, 'NV003', SYSDATETIME(), 'CHO_KHAM',
            N'[TEST] Phiếu chờ khám UC023.');

SELECT maPK, maLH, maHSBA, maNV_LapPhieu, trangThai
FROM PhieuKham WHERE maPK = @maPK;
