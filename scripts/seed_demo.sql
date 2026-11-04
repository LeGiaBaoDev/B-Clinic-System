-- Du lieu gia lap, chi nap thu cong tren BClinic_Dev sau Flyway V1/V2.
-- Khong chay nhu migration, khong sua mat khau/tai khoan hien co.
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() <> N'BClinic_Dev'
    THROW 51000, N'Chỉ được nạp dữ liệu mẫu trên BClinic_Dev.', 1;
IF EXISTS (SELECT 1 FROM NhatKyHeThong WHERE maNhatKy = 'NK990099'
    AND hanhDong = N'DEMO_SEED_V1')
BEGIN
    PRINT N'Bộ dữ liệu mẫu đã được nạp; không thêm lần nữa.';
    RETURN;
END;
-- Dung lai thay vi ghi de/bo qua ma dang thuoc du lieu cua nguoi dung.
IF EXISTS (SELECT 1 FROM KhachHang) OR EXISTS (SELECT 1 FROM DichVu)
    OR EXISTS (SELECT 1 FROM LoaiDichVu) OR EXISTS (SELECT 1 FROM Voucher)
    OR EXISTS (SELECT 1 FROM LichHen) OR EXISTS (SELECT 1 FROM LichLamViec)
    OR EXISTS (SELECT 1 FROM HoSoBenhAn) OR EXISTS (SELECT 1 FROM PhieuKham)
    OR EXISTS (SELECT 1 FROM HoaDon) OR EXISTS (SELECT 1 FROM ChiTietPhieuKham)
    THROW 51001, N'Bảng nghiệp vụ đã có dữ liệu. Dừng để tránh trùng hoặc ghi đè.', 1;
IF (SELECT COUNT(*) FROM TaiKhoan WHERE
    (maTK='TK001' AND maNV='NV001' AND vaiTro='QUANLY' AND trangThai=1) OR
    (maTK='TK002' AND maNV='NV002' AND vaiTro='NHASI' AND trangThai=1) OR
    (maTK='TK003' AND maNV='NV003' AND vaiTro='LETAN' AND trangThai=1)) <> 3
    THROW 51002, N'Cần ba tài khoản mẫu từ V2 trước khi nạp.', 1;

BEGIN TRY
    BEGIN TRANSACTION;
    -- Moc ngay theo gio Viet Nam khi nap; khong dich chuyen du lieu sau moi lan mo app.
    DECLARE @ngay DATE = CONVERT(DATE, SYSDATETIMEOFFSET() AT TIME ZONE 'SE Asia Standard Time');
    DECLARE @nguon VARCHAR(15);

    INSERT INTO LoaiDichVu (maLoaiDV,tenLoaiDV,mucThueGTGT) VALUES
    ('LDV01',N'Điều trị bệnh lý',0),
    ('LDV02',N'Vật tư/thiết bị bán kèm',5),
    ('LDV03',N'Thẩm mỹ',10);

    INSERT INTO DichVu (maDV,maLoaiDV,tenDV,donGia,thoiLuong,loTrinhDichVu,trangThai) VALUES
    ('DV001','LDV01',N'Khám răng tổng quát',100000,30,NULL,1),
    ('DV002','LDV01',N'Cạo vôi răng',250000,45,NULL,1),
    ('DV003','LDV01',N'Trám răng sâu',400000,45,NULL,1),
    ('DV004','LDV01',N'Nhổ răng sữa',200000,30,NULL,1),
    ('DV005','LDV01',N'Điều trị tủy theo buổi',800000,60,N'2 buổi; tái khám cùng nha sĩ sau 7–14 ngày; tính phí từng buổi.',1),
    ('DV006','LDV01',N'Nhổ răng khôn',1500000,60,NULL,1),
    ('DV007','LDV02',N'Bàn chải chuyên dụng',80000,15,NULL,1),
    ('DV008','LDV02',N'Chỉ nha khoa',50000,15,NULL,1),
    ('DV009','LDV03',N'Tẩy trắng răng thẩm mỹ',2000000,60,NULL,1),
    ('DV010','LDV03',N'Gắn đá răng thẩm mỹ (ngừng cung cấp)',500000,30,NULL,0);

    DECLARE @ten TABLE (stt INT PRIMARY KEY, hoTen NVARCHAR(100));
    INSERT INTO @ten VALUES
    (1,N'Nguyễn Minh Anh'),(2,N'Trần Hoàng Nam'),(3,N'Lê Ngọc Mai'),(4,N'Phạm Gia Huy'),
    (5,N'Võ Thanh Hà'),(6,N'Đặng Quốc Bảo'),(7,N'Bùi Thảo Vy'),(8,N'Đỗ Tuấn Kiệt'),
    (9,N'Hồ Khánh Linh'),(10,N'Ngô Đức Minh'),(11,N'Dương Hải Yến'),(12,N'Lý Anh Khoa'),
    (13,N'Trương Bảo Ngọc'),(14,N'Đinh Thành Đạt'),(15,N'Mai Phương Anh'),
    (16,N'Vũ Nhật Long'),(17,N'Đoàn Thu Trang'),(18,N'Tạ Minh Quân'),
    (19,N'Nguyễn An Nhiên'),(20,N'Trần Gia Phúc');
    INSERT INTO KhachHang (maKH,tenKH,gioiTinh,ngaySinh,sdt,email,diaChi)
    SELECT 'KH'+RIGHT('00000'+CAST(stt AS VARCHAR(5)),5),hoTen,stt%2,
        DATEFROMPARTS(1980+stt,1+stt%12,1+stt%27),
        '090990'+RIGHT('0000'+CAST(stt AS VARCHAR(4)),4),
        'demo.kh'+CAST(stt AS VARCHAR(2))+'@example.com',N'Địa chỉ giả lập — TP. Hồ Chí Minh'
    FROM @ten;
    INSERT INTO HoSoBenhAn (maHSBA,maKH,tienSuBenh)
    SELECT 'HS'+RIGHT(maKH,5),maKH,
        CASE WHEN maKH='KH00001' THEN N'[MẪU] Dị ứng penicillin do bệnh nhân khai báo.'
             WHEN maKH='KH00002' THEN N'[MẪU] Tiền sử tăng huyết áp; kiểm tra trước điều trị.'
             ELSE N'[MẪU] Chưa ghi nhận bệnh nền hoặc dị ứng.' END
    FROM KhachHang;

    INSERT INTO Voucher (maVoucher,tenVoucher,loaiGiam,giaGiam,giaTriGiamToiDa,ngayBatDau,ngayKetThuc,trangThai) VALUES
    ('VC001',N'[MẪU] Giảm 10%, tối đa 100.000đ',1,10,100000,DATEADD(DAY,-120,@ngay),DATEADD(DAY,60,@ngay),1),
    ('VC002',N'[MẪU] Giảm 50.000đ',0,50000,NULL,DATEADD(DAY,-120,@ngay),DATEADD(DAY,60,@ngay),1),
    ('VC003',N'[MẪU] Đã hết hạn',1,5,50000,DATEADD(DAY,-60,@ngay),DATEADD(DAY,-1,@ngay),1),
    ('VC004',N'[MẪU] Chưa tới ngày áp dụng',1,15,150000,DATEADD(DAY,10,@ngay),DATEADD(DAY,40,@ngay),1),
    ('VC005',N'[MẪU] Đã vô hiệu hóa',0,100000,NULL,DATEADD(DAY,-30,@ngay),DATEADD(DAY,30,@ngay),0),
    ('VC006',N'[MẪU] Giảm 5% không giới hạn',1,5,NULL,DATEADD(DAY,-120,@ngay),DATEADD(DAY,60,@ngay),1);

    DECLARE @luot TABLE (stt INT PRIMARY KEY, ngayHen DATETIME2(0), maKH VARCHAR(15),
        maDV VARCHAR(15), trangThai VARCHAR(10), trangThaiPK VARCHAR(10) NULL, daTra BIT);
    DECLARE @i INT=1;
    -- 12 buoi kham lich su trong khoang 3 thang, 10 da tra va 2 cho thanh toan.
    WHILE @i<=12
    BEGIN
        INSERT INTO @luot VALUES (@i,DATEADD(HOUR,8,CAST(DATEADD(DAY,-7*@i,@ngay) AS DATETIME2)),
            'KH'+RIGHT('00000'+CAST(@i AS VARCHAR(5)),5),
            CASE WHEN @i=1 THEN 'DV005' WHEN @i%3=0 THEN 'DV009'
                 WHEN @i%3=1 THEN 'DV002' ELSE 'DV003' END,
            CASE WHEN @i<=2 THEN 'DA_DEN' ELSE 'HOAN_TAT' END,'HOAN_TAT',CASE WHEN @i<=2 THEN 0 ELSE 1 END);
        SET @i+=1;
    END;
    INSERT INTO @luot VALUES
    (13,DATEADD(HOUR,15,CAST(@ngay AS DATETIME2)),'KH00013','DV001','DA_DEN','CHO_KHAM',0),
    (14,DATEADD(HOUR,16,CAST(@ngay AS DATETIME2)),'KH00014','DV003','DA_DEN','DANG_KHAM',0),
    (15,DATEADD(HOUR,8,CAST(DATEADD(DAY,7,@ngay) AS DATETIME2)),'KH00001','DV005','DA_DAT',NULL,0),
    (16,DATEADD(HOUR,9,CAST(DATEADD(DAY,1,@ngay) AS DATETIME2)),'KH00015','DV002','DA_DAT',NULL,0),
    (17,DATEADD(HOUR,7,CAST(DATEADD(DAY,2,@ngay) AS DATETIME2)),'KH00020','DV001','DA_DAT',NULL,0),
    (18,DATEADD(HOUR,7,CAST(DATEADD(DAY,4,DATEADD(MONTH,-1,DATEFROMPARTS(YEAR(@ngay),MONTH(@ngay),1))) AS DATETIME2)),'KH00020','DV001','DA_HUY',NULL,0),
    (19,DATEADD(HOUR,7,CAST(DATEADD(DAY,9,DATEADD(MONTH,-1,DATEFROMPARTS(YEAR(@ngay),MONTH(@ngay),1))) AS DATETIME2)),'KH00020','DV001','DA_HUY',NULL,0),
    (20,DATEADD(HOUR,7,CAST(DATEADD(DAY,14,DATEADD(MONTH,-1,DATEFROMPARTS(YEAR(@ngay),MONTH(@ngay),1))) AS DATETIME2)),'KH00020','DV001','DA_HUY',NULL,0),
    (21,DATEADD(HOUR,10,CAST(DATEADD(DAY,-3,@ngay) AS DATETIME2)),'KH00019','DV002','KHONG_DEN',NULL,0);

    -- Ca truc cho ca 3 vai tro; khong tao lich vao ngay nha si nghi phep.
    ;WITH Ngay AS (SELECT DISTINCT CAST(ngayHen AS DATE) AS ngayTruc FROM @luot),
    Ca AS (SELECT n.ngayTruc,nv.maNV,ROW_NUMBER() OVER(ORDER BY n.ngayTruc,nv.maNV) AS stt
        FROM Ngay n CROSS JOIN NhanVien nv WHERE nv.maNV IN ('NV001','NV002','NV003'))
    INSERT INTO LichLamViec (maLLV,maNV,ngayTruc,caTruc,gioBatDau,gioKetThuc,trangThai)
    SELECT 'LLV'+RIGHT('00000'+CAST(stt AS VARCHAR(5)),5),maNV,ngayTruc,'CA_NGAY','07:00','17:30','DA_PHAN_CONG' FROM Ca;
    INSERT INTO LichLamViec (maLLV,maNV,ngayTruc,caTruc,gioBatDau,gioKetThuc,trangThai,ghiChuNghi)
    VALUES ('LLV99999','NV002',DATEADD(DAY,3,@ngay),'CA_NGAY','07:00','17:30','NGHI_PHEP',N'[MẪU] Nghỉ việc gia đình, quản lý đã duyệt.');

    SET @i=1;
    WHILE @i<=21
    BEGIN
        DECLARE @dt DATETIME2(0),@kh VARCHAR(15),@dv VARCHAR(15),@tt VARCHAR(10),@pktt VARCHAR(10),@paid BIT;
        SELECT @dt=ngayHen,@kh=maKH,@dv=maDV,@tt=trangThai,@pktt=trangThaiPK,@paid=daTra FROM @luot WHERE stt=@i;
        DECLARE @suffix VARCHAR(9)=CONVERT(CHAR(6),@dt,12)+RIGHT('000'+CAST(@i AS VARCHAR(3)),3);
        DECLARE @lh VARCHAR(15)='LH'+@suffix,@pk VARCHAR(15)='PK'+@suffix;
        INSERT INTO LichHen (maLH,maKH,maNV,maDV,maPKNguon,ngayGioHen,trangThai,ghiChu,lyDoHuy)
        VALUES (@lh,@kh,'NV002',@dv,CASE WHEN @i=15 THEN @nguon ELSE NULL END,@dt,@tt,
            CASE WHEN @i=15 THEN N'[MẪU] Tái khám điều trị tủy buổi 2, sau 14 ngày, cùng nha sĩ.' ELSE N'[MẪU] Dữ liệu giả lập kiểm thử.' END,
            CASE WHEN @tt='DA_HUY' THEN N'[MẪU] Khách bận việc cá nhân, thông báo hủy.' ELSE NULL END);
        IF @pktt IS NOT NULL
        BEGIN
            INSERT INTO PhieuKham (maPK,maHSBA,maLH,maNV_LapPhieu,ngayLap,trangThai,loiDan)
            VALUES (@pk,'HS'+RIGHT(@kh,5),@lh,'NV003',@dt,'CHO_KHAM',N'[MẪU] Vệ sinh răng miệng; tái khám theo chỉ định.');
            IF @pktt IN ('DANG_KHAM','HOAN_TAT')
            BEGIN
                -- Them chi tiet truoc khi khoa phieu; gia/thue la anh chup tai thoi diem kham.
                INSERT INTO ChiTietPhieuKham (maPK,maDV,maNV_NhaSi,trieuChung,chanDoan,ketQuaDieuTri,donViTinh,soLuong,donGia,mucThueGTGT)
                SELECT @pk,d.maDV,'NV002',
                    CASE d.maDV WHEN 'DV005' THEN N'[MẪU] Đau nhức răng kéo dài.' WHEN 'DV009' THEN N'[MẪU] Mong muốn cải thiện màu răng.'
                    WHEN 'DV002' THEN N'[MẪU] Mảng bám, chảy máu chân răng.' ELSE N'[MẪU] Ê buốt răng khi ăn.' END,
                    CASE d.maDV WHEN 'DV005' THEN N'[MẪU] Viêm tủy răng.' WHEN 'DV009' THEN N'[MẪU] Nhiễm màu răng, chỉ định thẩm mỹ.'
                    WHEN 'DV002' THEN N'[MẪU] Cao răng và viêm nướu nhẹ.' ELSE N'[MẪU] Sâu răng ngà.' END,
                    CASE WHEN @pktt='HOAN_TAT' THEN N'[MẪU] Hoàn tất điều trị của buổi này.' ELSE NULL END,
                    N'Lần',1,d.donGia,l.mucThueGTGT FROM DichVu d JOIN LoaiDichVu l ON l.maLoaiDV=d.maLoaiDV WHERE d.maDV=@dv;
                IF @i%3=0 AND @pktt='HOAN_TAT'
                    INSERT INTO ChiTietPhieuKham (maPK,maDV,maNV_NhaSi,trieuChung,chanDoan,ketQuaDieuTri,donViTinh,soLuong,donGia,mucThueGTGT)
                    SELECT @pk,'DV007','NV002',N'[MẪU] Cần dụng cụ vệ sinh sau điều trị.',N'[MẪU] Hướng dẫn chăm sóc răng tại nhà.',
                        N'[MẪU] Đã giao bàn chải.',N'Cái',2,donGia,5 FROM DichVu WHERE maDV='DV007';
                UPDATE PhieuKham SET trangThai=@pktt WHERE maPK=@pk;
            END;
            IF @pktt='HOAN_TAT'
            BEGIN
                DECLARE @tong DECIMAL(18,2),@giam DECIMAL(18,2)=0,@vc VARCHAR(15)=NULL;
                SELECT @tong=SUM(thanhTien+ROUND(thanhTien*mucThueGTGT/100,0)) FROM ChiTietPhieuKham WHERE maPK=@pk;
                IF @paid=1 AND @i%3=0
                BEGIN
                    SET @vc='VC001'; SET @giam=ROUND(@tong*0.1,0);
                    IF @giam>100000 SET @giam=100000;
                END;
                IF @paid=1 AND @i%3=1
                BEGIN
                    SET @vc='VC002'; SET @giam=CASE WHEN @tong<50000 THEN @tong ELSE 50000 END;
                END;
                INSERT INTO HoaDon (maHD,maPK,maVoucher,maNV_ThuNgan,ngayLap,trangThai,hinhThucThanhToan,ngayThanhToan,tongKhuyenMai)
                VALUES ('HD'+@suffix,@pk,@vc,CASE WHEN @paid=1 THEN 'NV003' ELSE NULL END,
                    DATEADD(MINUTE,70,@dt),@paid,
                    CASE WHEN @paid=0 THEN NULL WHEN @i%2=0 THEN N'Tiền mặt' ELSE N'Chuyển khoản' END,
                    CASE WHEN @paid=1 THEN DATEADD(MINUTE,80,@dt) ELSE NULL END,@giam);
            END;
            IF @i=1 SET @nguon=@pk;
        END;
        SET @i+=1;
    END;

    INSERT INTO NhatKyHeThong (maNhatKy,maTK,thoiDiem,hanhDong,noiDungThayDoi) VALUES
    ('NK990001','TK001',DATEADD(HOUR,7,CAST(@ngay AS DATETIME2)),N'DANG_NHAP',N'[MẪU] Sự kiện giả lập đăng nhập quản lý, không phải nhật ký thật.'),
    ('NK990002','TK002',DATEADD(HOUR,7,CAST(@ngay AS DATETIME2)),N'DANG_NHAP',N'[MẪU] Sự kiện giả lập đăng nhập nha sĩ.'),
    ('NK990003','TK003',DATEADD(HOUR,7,CAST(@ngay AS DATETIME2)),N'DANG_NHAP',N'[MẪU] Sự kiện giả lập đăng nhập lễ tân.'),
    ('NK990004','TK003',DATEADD(HOUR,9,CAST(DATEADD(DAY,-21,@ngay) AS DATETIME2)),N'THANH_TOAN',N'[MẪU] Sự kiện giả lập thanh toán, dùng kiểm thử tra cứu.'),
    ('NK990099','TK001',SYSDATETIMEOFFSET() AT TIME ZONE 'SE Asia Standard Time',N'DEMO_SEED_V1',N'[MẪU] Hoàn tất nạp bộ dữ liệu giả lập; mốc ngày theo Asia/Saigon.');
    COMMIT TRANSACTION;
    PRINT N'Đã nạp dữ liệu mẫu thành công; giữ nguyên tài khoản và mật khẩu hiện có.';
END TRY
BEGIN CATCH
    IF XACT_STATE()<>0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
