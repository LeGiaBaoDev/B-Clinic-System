-- Chuẩn hóa giới tính nhân viên vừa bổ sung, không đổi dữ liệu nhân viên cũ.
SET XACT_ABORT ON;
IF DB_NAME()<>N'BClinic_Dev' THROW 51720,N'Chỉ BClinic_Dev.',1;
BEGIN TRAN;
UPDATE n SET gioiTinh=0 FROM NhanVien n JOIN TaiKhoan t ON t.maNV=n.maNV
WHERE (n.maNV='NV013' AND n.hoTenNV=N'Phạm Ngọc Trâm' AND t.tenTK='tram.nhasi')
 OR (n.maNV='NV014' AND n.hoTenNV=N'Lê Ngọc Ánh' AND t.tenTK='anh.letan');
COMMIT;
