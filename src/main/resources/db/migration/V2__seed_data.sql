-- ============================================================
-- V2 — Dữ liệu mẫu
-- 3 nhân viên Nguyễn Trọng Hiếu, mỗi người 1 tài khoản / vai trò
-- Mật khẩu mặc định: Clinic@123
-- ============================================================

-- Nhân viên 1 — vai trò Quản lý (maNV = 1)
INSERT INTO NhanVien (hoTenNV, gioiTinh, sdt, email, chuyenMon)
VALUES (N'Nguyễn Trọng Hiếu', 1, N'0398682230', N'macvestdinet@gmail.com', N'Quản lý hệ thống');

-- Nhân viên 2 — vai trò Nha sĩ (maNV = 2)
-- SĐT / email phải unique → thêm suffix phân biệt
INSERT INTO NhanVien (hoTenNV, gioiTinh, sdt, email, chuyenMon)
VALUES (N'Nguyễn Trọng Hiếu', 1, N'0398682231', N'macvestdinet+nhasi@gmail.com', N'Nha sĩ răng hàm mặt');

-- Nhân viên 3 — vai trò Lễ tân (maNV = 3)
INSERT INTO NhanVien (hoTenNV, gioiTinh, sdt, email, chuyenMon)
VALUES (N'Nguyễn Trọng Hiếu', 1, N'0398682232', N'macvestdinet+letan@gmail.com', N'Lễ tân phòng khám');

-- Tài khoản QUANLY → maNV = 1
INSERT INTO TaiKhoan (maNV, tenTK, matKhau, vaiTro, trangThai)
VALUES (1, N'hieu.quanly', N'Clinic@123', N'QUANLY', 1);

-- Tài khoản NHASI → maNV = 2
INSERT INTO TaiKhoan (maNV, tenTK, matKhau, vaiTro, trangThai)
VALUES (2, N'hieu.nhasi', N'Clinic@123', N'NHASI', 1);

-- Tài khoản LETAN → maNV = 3
INSERT INTO TaiKhoan (maNV, tenTK, matKhau, vaiTro, trangThai)
VALUES (3, N'hieu.letan', N'Clinic@123', N'LETAN', 1);
