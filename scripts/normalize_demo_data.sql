-- Chuẩn hóa dữ liệu mẫu hiện hữu trên BClinic_Dev; không phải migration schema.
-- Mặc định preview rollback. Đổi @Preview=0 sau khi kiểm tra kết quả.
SET NOCOUNT ON;
SET XACT_ABORT ON;
DECLARE @Preview BIT = 1;
IF DB_NAME()<>N'BClinic_Dev' THROW 51500,N'Chỉ chuẩn hóa dữ liệu mẫu trên BClinic_Dev.',1;
BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @lock INT;
    EXEC @lock=sys.sp_getapplock @Resource=N'BClinic:normalize-demo-data',@LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=10000;
    IF @lock<0 THROW 51501,N'Có thao tác chuẩn hóa khác đang chạy.',1;
    IF EXISTS(SELECT 1 FROM NhatKyHeThong WHERE hanhDong=N'CHUAN_HOA_DU_LIEU_20261105')
    BEGIN SELECT N'Đã chuẩn hóa; không sửa lại dữ liệu người dùng.' AS ketQua; COMMIT; RETURN; END;

    DECLARE @Khach TABLE(maKH VARCHAR(15) PRIMARY KEY,tenCu NVARCHAR(100),tenMoi NVARCHAR(100),email VARCHAR(100),diaChi NVARCHAR(255));
    INSERT INTO @Khach VALUES
    ('KH88001',N'Mẫu H4 - Khách 1',N'Nguyễn Minh Khang','nguyen.minh.khang@example.com',N'35 Đường Hoa Lan, TP. Hồ Chí Minh'),
    ('KH88002',N'Mẫu H4 - Khách 2',N'Trần Ngọc Mai','tran.ngoc.mai@example.com',N'82 Đường Nguyễn Văn Đậu, TP. Hồ Chí Minh'),
    ('KH88003',N'Mẫu H4 - Khách 3',N'Lê Hoàng Phúc','le.hoang.phuc@example.com',N'16 Đường Lê Văn Sỹ, TP. Hồ Chí Minh'),
    ('KH88004',N'Mẫu H4 - Khách 4',N'Phạm Thảo Nguyên','pham.thao.nguyen@example.com',N'47 Đường Phan Văn Trị, TP. Hồ Chí Minh'),
    ('KH88005',N'Mẫu H4 - Khách 5',N'Võ Đức Huy','vo.duc.huy@example.com',N'91 Đường Quang Trung, TP. Hồ Chí Minh'),
    ('KH88006',N'Mẫu H4 - Khách 6',N'Đặng Minh Anh','dang.minh.anh@example.com',N'28 Đường Nguyễn Oanh, TP. Hồ Chí Minh'),
    ('KH88007',N'Mẫu H4 - Khách 7',N'Bùi Thành Đạt','bui.thanh.dat@example.com',N'63 Đường Lê Đức Thọ, TP. Hồ Chí Minh'),
    ('KH88008',N'Mẫu H4 - Khách 8',N'Đỗ Khánh Linh','do.khanh.linh@example.com',N'14 Đường Phạm Văn Đồng, TP. Hồ Chí Minh');
    DECLARE @TaiKhoan TABLE(maTK VARCHAR(15) PRIMARY KEY,tenCu VARCHAR(50),tenMoi VARCHAR(50));
    INSERT INTO @TaiKhoan VALUES
    ('TK101','demo.huong','huong.nhasi'),('TK102','demo.nam','nam.nhasi'),
    ('TK103','demo.lan','lan.nhasi'),('TK104','demo.an','an.nhasi'),
    ('TK105','demo.vy.locked','vy.nhasi'),('TK106','demo.thuha','ha.letan');
    IF EXISTS(SELECT 1 FROM @Khach m LEFT JOIN KhachHang k ON k.maKH=m.maKH WHERE k.maKH IS NULL OR k.tenKH<>m.tenCu)
        THROW 51502,N'Dữ liệu khách mẫu đã thay đổi; dừng để tránh ghi đè.',1;
    IF EXISTS(SELECT 1 FROM @TaiKhoan m LEFT JOIN TaiKhoan t ON t.maTK=m.maTK WHERE t.maTK IS NULL OR t.tenTK<>m.tenCu)
        THROW 51503,N'Tài khoản mẫu đã thay đổi; dừng để tránh ghi đè.',1;
    IF EXISTS(SELECT 1 FROM @TaiKhoan m JOIN TaiKhoan t ON t.tenTK=m.tenMoi AND t.maTK<>m.maTK)
        THROW 51504,N'Tên đăng nhập mới bị trùng; không tự ghi đè.',1;

    -- Snapshot trong transaction: không in password/hash ra kết quả hoặc log.
    DECLARE @AccountBefore TABLE(maTK VARCHAR(15),matKhau VARCHAR(255),vaiTro VARCHAR(10),trangThai BIT,maNV VARCHAR(15));
    INSERT INTO @AccountBefore SELECT maTK,matKhau,vaiTro,trangThai,maNV FROM TaiKhoan;
    DECLARE @CustomerBefore TABLE(maKH VARCHAR(15),ngaySinh DATE,sdt VARCHAR(15),gioiTinh BIT);
    INSERT INTO @CustomerBefore SELECT maKH,ngaySinh,sdt,gioiTinh FROM KhachHang;
    DECLARE @CountBefore TABLE(ten VARCHAR(30),soLuong BIGINT);
    INSERT INTO @CountBefore
    SELECT 'KhachHang',COUNT_BIG(*) FROM KhachHang UNION ALL SELECT 'HoSoBenhAn',COUNT_BIG(*) FROM HoSoBenhAn
    UNION ALL SELECT 'LichHen',COUNT_BIG(*) FROM LichHen UNION ALL SELECT 'PhieuKham',COUNT_BIG(*) FROM PhieuKham
    UNION ALL SELECT 'ChiTietPhieuKham',COUNT_BIG(*) FROM ChiTietPhieuKham UNION ALL SELECT 'HoaDon',COUNT_BIG(*) FROM HoaDon;

    UPDATE k SET tenKH=m.tenMoi,email=m.email,diaChi=m.diaChi FROM KhachHang k JOIN @Khach m ON m.maKH=k.maKH;
    UPDATE t SET tenTK=m.tenMoi FROM TaiKhoan t JOIN @TaiKhoan m ON m.maTK=t.maTK;
    -- Chuyên môn/vị trí theo lựa chọn hiện hành của form: chỉ 3 giá trị.
    UPDATE n SET chuyenMon=CASE t.vaiTro WHEN 'QUANLY' THEN N'Quản lý' WHEN 'NHASI' THEN N'Nha sĩ' ELSE N'Lễ tân' END
    FROM NhanVien n JOIN TaiKhoan t ON t.maNV=n.maNV
    WHERE n.chuyenMon<>CASE t.vaiTro WHEN 'QUANLY' THEN N'Quản lý' WHEN 'NHASI' THEN N'Nha sĩ' ELSE N'Lễ tân' END;

    IF EXISTS(SELECT maTK,matKhau,vaiTro,trangThai,maNV FROM TaiKhoan EXCEPT SELECT * FROM @AccountBefore)
       OR EXISTS(SELECT * FROM @AccountBefore EXCEPT SELECT maTK,matKhau,vaiTro,trangThai,maNV FROM TaiKhoan)
        THROW 51505,N'Mật khẩu/quyền/trạng thái bị thay đổi; rollback.',1;
    IF EXISTS(SELECT maKH,ngaySinh,sdt,gioiTinh FROM KhachHang EXCEPT SELECT * FROM @CustomerBefore)
       OR EXISTS(SELECT * FROM @CustomerBefore EXCEPT SELECT maKH,ngaySinh,sdt,gioiTinh FROM KhachHang)
        THROW 51506,N'Mã/ngày sinh/SĐT/giới tính bị thay đổi; rollback.',1;
    DECLARE @CountAfter TABLE(ten VARCHAR(30),soLuong BIGINT);
    INSERT INTO @CountAfter
    SELECT 'KhachHang',COUNT_BIG(*) FROM KhachHang UNION ALL SELECT 'HoSoBenhAn',COUNT_BIG(*) FROM HoSoBenhAn
    UNION ALL SELECT 'LichHen',COUNT_BIG(*) FROM LichHen UNION ALL SELECT 'PhieuKham',COUNT_BIG(*) FROM PhieuKham
    UNION ALL SELECT 'ChiTietPhieuKham',COUNT_BIG(*) FROM ChiTietPhieuKham UNION ALL SELECT 'HoaDon',COUNT_BIG(*) FROM HoaDon;
    IF EXISTS(SELECT * FROM @CountBefore EXCEPT SELECT * FROM @CountAfter)
        THROW 51507,N'Số lượng dữ liệu nghiệp vụ bị thay đổi; rollback.',1;
    IF EXISTS(SELECT 1 FROM KhachHang WHERE LEN(sdt)<>10 OR sdt LIKE '%[^0-9]%' OR sdt NOT LIKE '0%' OR ngaySinh>CAST(GETDATE() AS DATE))
        THROW 51508,N'Còn dữ liệu khách sai định dạng; rollback.',1;

    DECLARE @n INT=880006,@ma VARCHAR(15);
    SET @ma='NK'+CAST(@n AS VARCHAR(6));
    WHILE EXISTS(SELECT 1 FROM NhatKyHeThong WHERE maNhatKy=@ma)
    BEGIN SET @n+=1; IF @n>880099 THROW 51509,N'Hết mã nhật ký cho chuẩn hóa.',1; SET @ma='NK'+CAST(@n AS VARCHAR(6)); END;
    INSERT INTO NhatKyHeThong(maNhatKy,maTK,hanhDong,noiDungThayDoi)
    VALUES(@ma,'TK001',N'CHUAN_HOA_DU_LIEU_20261105',N'Chuẩn hóa 8 tên khách/email/địa chỉ mẫu; 6 tên đăng nhập theo tên.vai_trò; vị trí NV theo quyền. Giữ mã, ngày sinh, SĐT, mật khẩu, trạng thái và liên kết nghiệp vụ. Dữ liệu hồ sơ/điều trị và lịch sử cũ giữ nguyên.');
    SELECT m.maKH,m.tenCu,k.tenKH AS tenMoi,k.email,k.diaChi FROM @Khach m JOIN KhachHang k ON k.maKH=m.maKH ORDER BY m.maKH;
    SELECT m.maTK,m.tenCu,t.tenTK AS tenMoi,t.vaiTro,t.trangThai FROM @TaiKhoan m JOIN TaiKhoan t ON t.maTK=m.maTK ORDER BY m.maTK;
    SELECT * FROM @CountAfter;
    IF @Preview=1 BEGIN ROLLBACK; SELECT N'NORMALIZATION_PREVIEW_ROLLED_BACK' AS ketQua; END
    ELSE BEGIN COMMIT; SELECT N'NORMALIZATION_COMMITTED' AS ketQua; END;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK;
    THROW;
END CATCH;
