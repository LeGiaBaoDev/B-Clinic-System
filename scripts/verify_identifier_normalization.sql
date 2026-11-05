SET NOCOUNT ON;
IF DB_NAME()<>N'BClinic_Dev' THROW 51620,N'Chỉ cho phép BClinic_Dev.',1;
IF EXISTS(SELECT 1 FROM sys.foreign_keys WHERE is_disabled=1 OR is_not_trusted=1)
 OR EXISTS(SELECT 1 FROM sys.check_constraints WHERE is_disabled=1 OR is_not_trusted=1)
 OR EXISTS(SELECT 1 FROM sys.triggers WHERE parent_class=1 AND is_disabled=1)
 THROW 51621,N'Có ràng buộc/trigger chưa được bật hoặc chưa trusted.',1;
DBCC CHECKCONSTRAINTS WITH ALL_CONSTRAINTS;
SELECT maNV,hoTenNV FROM NhanVien ORDER BY maNV;
SELECT 'NhanVien' bang,COUNT(*) so_luong,MIN(maNV) ma_dau,MAX(maNV) ma_cuoi FROM NhanVien
UNION ALL SELECT 'TaiKhoan',COUNT(*),MIN(maTK),MAX(maTK) FROM TaiKhoan
UNION ALL SELECT 'KhachHang',COUNT(*),MIN(maKH),MAX(maKH) FROM KhachHang
UNION ALL SELECT 'HoSoBenhAn',COUNT(*),MIN(maHSBA),MAX(maHSBA) FROM HoSoBenhAn
UNION ALL SELECT 'DichVu',COUNT(*),MIN(maDV),MAX(maDV) FROM DichVu
UNION ALL SELECT 'Voucher',COUNT(*),MIN(maVoucher),MAX(maVoucher) FROM Voucher
UNION ALL SELECT 'LichLamViec',COUNT(*),MIN(maLLV),MAX(maLLV) FROM LichLamViec
UNION ALL SELECT 'LichHen',COUNT(*),MIN(maLH),MAX(maLH) FROM LichHen
UNION ALL SELECT 'PhieuKham',COUNT(*),MIN(maPK),MAX(maPK) FROM PhieuKham
UNION ALL SELECT 'HoaDon',COUNT(*),MIN(maHD),MAX(maHD) FROM HoaDon;
SELECT 'IDENTIFIER_CONSTRAINTS_VERIFIED' ketQua;
