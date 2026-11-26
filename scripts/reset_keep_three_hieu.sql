-- Chỉ chạy thủ công sau khi có backup. Không đặt file này trong thư mục Flyway.
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
IF DB_NAME()<>'BClinic_Dev' AND DB_NAME() NOT LIKE 'BClinic_Test_Finance[_]%'
    THROW 51800,N'Sai database đích, không được dọn dữ liệu.',1;
BEGIN TRAN;
SELECT t.* INTO #TaiKhoanGiuLai FROM TaiKhoan t WITH(UPDLOCK,HOLDLOCK)
JOIN NhanVien n ON n.maNV=t.maNV WHERE n.hoTenNV=N'Nguyễn Trọng Hiếu';
IF (SELECT COUNT(*) FROM #TaiKhoanGiuLai)<>3
 OR (SELECT COUNT(DISTINCT vaiTro) FROM #TaiKhoanGiuLai)<>3
 THROW 51801,N'Không xác định đúng ba tài khoản Hiếu ở ba vai trò.',1;
SELECT QUOTENAME(OBJECT_SCHEMA_NAME(parent_id))+'.'+QUOTENAME(OBJECT_NAME(parent_id)) AS bang,
 QUOTENAME(name) AS ten INTO #TriggerBat FROM sys.triggers WHERE parent_class=1 AND is_disabled=0;
DECLARE @sql NVARCHAR(MAX)=N'';
SELECT @sql=@sql+N'DISABLE TRIGGER '+ten+N' ON '+bang+N';' FROM #TriggerBat;
EXEC sys.sp_executesql @sql;
IF OBJECT_ID('dbo.PhieuThu') IS NOT NULL EXEC(N'DELETE FROM dbo.PhieuThu;');
IF OBJECT_ID('dbo.ChiTietHoaDon') IS NOT NULL EXEC(N'DELETE FROM dbo.ChiTietHoaDon;');
IF COL_LENGTH('dbo.LichHen','maCTPKNguon') IS NOT NULL EXEC(N'UPDATE dbo.LichHen SET maCTPKNguon=NULL,maPKNguon=NULL;');
ELSE UPDATE LichHen SET maPKNguon=NULL;
DELETE FROM HoaDon;
DELETE FROM ChiTietPhieuKham;
DELETE FROM PhieuKham;
DELETE FROM LichHen;
DELETE FROM HoSoBenhAn;
DELETE FROM KhachHang;
IF OBJECT_ID('dbo.VoucherDichVu') IS NOT NULL EXEC(N'DELETE FROM dbo.VoucherDichVu;');
DELETE FROM Voucher;
DELETE FROM DichVu;
DELETE FROM LoaiDichVu;
DELETE FROM DonNghiPhep;
DELETE FROM LichLamViec;
DELETE FROM NhatKyHeThong;
DELETE FROM TaiKhoan WHERE maTK NOT IN(SELECT maTK FROM #TaiKhoanGiuLai);
DELETE FROM NhanVien WHERE maNV NOT IN(SELECT maNV FROM #TaiKhoanGiuLai);
-- Các bảng tạm sao chép dữ liệu demo từ các phiên chuẩn bị trước, đã có trong backup đầy đủ.
IF OBJECT_ID('dbo.DemoBackup_HieuAppointments_20261106') IS NOT NULL DROP TABLE dbo.DemoBackup_HieuAppointments_20261106;
IF OBJECT_ID('dbo.DemoQueueBackup_20261106_PhieuKham') IS NOT NULL DROP TABLE dbo.DemoQueueBackup_20261106_PhieuKham;
IF OBJECT_ID('dbo.SyncAuditBackup_20261106_HoaDon') IS NOT NULL DROP TABLE dbo.SyncAuditBackup_20261106_HoaDon;
IF OBJECT_ID('dbo.SyncAuditBackup_20261106_LichHen') IS NOT NULL DROP TABLE dbo.SyncAuditBackup_20261106_LichHen;
IF OBJECT_ID('dbo.BoDemMaNhanVien') IS NOT NULL EXEC(N'UPDATE BoDemMaNhanVien SET soCuoi=1;');
IF EXISTS(SELECT maTK,maNV,tenTK,matKhau,vaiTro,trangThai FROM #TaiKhoanGiuLai
 EXCEPT SELECT maTK,maNV,tenTK,matKhau,vaiTro,trangThai FROM TaiKhoan)
 THROW 51802,N'Tài khoản giữ lại bị thay đổi ngoài ý muốn.',1;
IF (SELECT COUNT(*) FROM TaiKhoan)<>3 OR (SELECT COUNT(*) FROM NhanVien)<>3
 THROW 51803,N'Số lượng tài khoản/nhân viên sau dọn không đúng.',1;
SET @sql=N'';
SELECT @sql=@sql+N'ENABLE TRIGGER '+ten+N' ON '+bang+N';' FROM #TriggerBat;
EXEC sys.sp_executesql @sql;
COMMIT;
SELECT n.maNV,t.tenTK,t.vaiTro,n.hoTenNV,n.email FROM TaiKhoan t JOIN NhanVien n ON n.maNV=t.maNV ORDER BY t.vaiTro;
