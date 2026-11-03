IF NOT EXISTS (SELECT 1
FROM NhanVien
WHERE maNV = 'NV001')
    INSERT INTO NhanVien
    (maNV, hoTenNV, gioiTinh, sdt, email, chuyenMon)
VALUES
    ('NV001', N'Nguyễn Trọng Hiếu', 1, N'0398682230', N'macvestdinet@gmail.com', N'Quản lý hệ thống');

IF NOT EXISTS (SELECT 1
FROM NhanVien
WHERE maNV = 'NV002')
    INSERT INTO NhanVien
    (maNV, hoTenNV, gioiTinh, sdt, email, chuyenMon)
VALUES
    ('NV002', N'Nguyễn Trọng Hiếu', 1, N'0398682231', N'macvestdinet+nhasi@gmail.com', N'Nha sĩ răng hàm mặt');

IF NOT EXISTS (SELECT 1
FROM NhanVien
WHERE maNV = 'NV003')
    INSERT INTO NhanVien
    (maNV, hoTenNV, gioiTinh, sdt, email, chuyenMon)
VALUES
    ('NV003', N'Nguyễn Trọng Hiếu', 1, N'0398682232', N'macvestdinet+letan@gmail.com', N'Lễ tân phòng khám');

IF NOT EXISTS (SELECT 1
FROM TaiKhoan
WHERE maTK = 'TK001')
    INSERT INTO TaiKhoan
    (maTK, maNV, tenTK, matKhau, vaiTro, trangThai)
VALUES
    ('TK001', 'NV001', N'hieu.quanly', N'Clinic@123', N'QUANLY', 1);

IF NOT EXISTS (SELECT 1
FROM TaiKhoan
WHERE maTK = 'TK002')
    INSERT INTO TaiKhoan
    (maTK, maNV, tenTK, matKhau, vaiTro, trangThai)
VALUES
    ('TK002', 'NV002', N'hieu.nhasi', N'Clinic@123', N'NHASI', 1);

IF NOT EXISTS (SELECT 1
FROM TaiKhoan
WHERE maTK = 'TK003')
    INSERT INTO TaiKhoan
    (maTK, maNV, tenTK, matKhau, vaiTro, trangThai)
VALUES
    ('TK003', 'NV003', N'hieu.letan', N'Clinic@123', N'LETAN', 1);
