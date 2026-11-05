SET NOCOUNT ON;
IF DB_NAME()<>N'BClinic_Dev' THROW 51520,N'Chỉ kiểm tra BClinic_Dev.',1;
IF EXISTS(SELECT 1 FROM KhachHang WHERE maKH NOT LIKE 'KH[0-9][0-9][0-9][0-9][0-9]' OR LEN(maKH)<>7
    OR LEN(sdt)<>10 OR sdt NOT LIKE '0%' OR sdt LIKE '%[^0-9]%'
    OR ngaySinh>CAST(GETDATE() AS DATE) OR LTRIM(RTRIM(tenKH)) NOT LIKE N'% %'
    OR tenKH LIKE N'%[0-9]%' OR tenKH LIKE N'%Mẫu H4%' OR tenKH LIKE N'Khách %')
    THROW 51521,N'Còn khách hàng sai cú pháp hoặc dùng tên giữ chỗ.',1;
IF EXISTS(SELECT 1 FROM TaiKhoan WHERE tenTK LIKE 'demo.%' OR tenTK LIKE '%[^a-z0-9.]%'
    OR tenTK NOT LIKE '%.'+LOWER(vaiTro))
    THROW 51522,N'Còn tên đăng nhập không đúng hậu tố vai trò.',1;
IF EXISTS(SELECT 1 FROM NhanVien n JOIN TaiKhoan t ON t.maNV=n.maNV
    WHERE n.chuyenMon<>CASE t.vaiTro WHEN 'QUANLY' THEN N'Quản lý' WHEN 'NHASI' THEN N'Nha sĩ' ELSE N'Lễ tân' END)
    THROW 51523,N'Chuyên môn/vị trí không khớp vai trò.',1;
IF NOT EXISTS(SELECT 1 FROM TaiKhoan WHERE trangThai=0 AND tenTK='vy.nhasi')
    THROW 51524,N'Trạng thái tài khoản khóa không được giữ.',1;
SELECT (SELECT COUNT(*) FROM KhachHang) AS khach_hang,
       (SELECT COUNT(*) FROM HoSoBenhAn) AS ho_so,
       (SELECT COUNT(*) FROM TaiKhoan) AS tai_khoan,
       (SELECT COUNT(*) FROM LichHen) AS lich_hen,
       (SELECT COUNT(*) FROM PhieuKham) AS phieu_kham,
       (SELECT COUNT(*) FROM HoaDon) AS hoa_don;
SELECT N'NORMALIZED_DATA_VERIFIED' AS ketQua;
