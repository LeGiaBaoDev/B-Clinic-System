-- Chạy thủ công trên BClinic_Dev. Preview mặc định rollback; đổi @Preview=0 để lưu.
-- Dữ liệu mô phỏng cố định 05–12/11/2026, không phải giao dịch bệnh nhân thực.
SET NOCOUNT ON;
SET XACT_ABORT ON;
DECLARE @Preview BIT = 1;
DECLARE @Start DATE = '20261105';
IF DB_NAME() <> N'BClinic_Dev' THROW 51400,N'Chỉ dùng trên BClinic_Dev.',1;
BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @Lock INT;
    EXEC @Lock=sys.sp_getapplock @Resource=N'BClinic:seed-h4-dashboard',
         @LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=10000;
    IF @Lock<0 THROW 51401,N'Không lấy được khóa seed H4.',1;
    IF EXISTS(SELECT 1 FROM NhatKyHeThong WHERE hanhDong=N'H4_DEMO_20261105')
    BEGIN
        SELECT N'Đã có bộ H4 05–12/11/2026; không thêm trùng.' AS ketQua;
        COMMIT; RETURN;
    END;
    IF NOT EXISTS(SELECT 1 FROM TaiKhoan WHERE maNV='NV103' AND vaiTro='NHASI' AND trangThai=1)
       OR NOT EXISTS(SELECT 1 FROM TaiKhoan WHERE maNV='NV003' AND vaiTro='LETAN' AND trangThai=1)
       OR NOT EXISTS(SELECT 1 FROM TaiKhoan WHERE maTK='TK001' AND vaiTro='QUANLY')
       OR NOT EXISTS(SELECT 1 FROM DichVu WHERE maDV='DV001' AND trangThai=1)
        THROW 51402,N'Thiếu nha sĩ/lễ tân/quản lý/dịch vụ nền.',1;
    IF EXISTS(SELECT 1 FROM KhachHang WHERE maKH BETWEEN 'KH88001' AND 'KH88008')
       OR EXISTS(SELECT 1 FROM HoSoBenhAn WHERE maHSBA BETWEEN 'HS88001' AND 'HS88008')
       OR EXISTS(SELECT 1 FROM LichLamViec WHERE maLLV BETWEEN 'LLV88000' AND 'LLV88007')
       OR EXISTS(SELECT 1 FROM NhatKyHeThong WHERE maNhatKy='NK880005')
        THROW 51403,N'Có mã dành cho seed đã tồn tại; dừng, không ghi đè.',1;
    -- Không sửa/đè ca hiện có; cần khoảng ngày này trống cho nha sĩ demo.
    IF EXISTS(SELECT 1 FROM LichLamViec WHERE maNV='NV103' AND ngayTruc BETWEEN @Start AND DATEADD(DAY,7,@Start))
        THROW 51404,N'Nha sĩ NV103 đã có ca; kiểm tra lại trước khi seed.',1;

    DECLARE @TenKhach TABLE(stt INT,hoTen NVARCHAR(100),email VARCHAR(100),diaChi NVARCHAR(255));
    INSERT INTO @TenKhach VALUES
    (1,N'Nguyễn Minh Khang','nguyen.minh.khang@example.com',N'35 Đường Hoa Lan, TP. Hồ Chí Minh'),
    (2,N'Trần Ngọc Mai','tran.ngoc.mai@example.com',N'82 Đường Nguyễn Văn Đậu, TP. Hồ Chí Minh'),
    (3,N'Lê Hoàng Phúc','le.hoang.phuc@example.com',N'16 Đường Lê Văn Sỹ, TP. Hồ Chí Minh'),
    (4,N'Phạm Thảo Nguyên','pham.thao.nguyen@example.com',N'47 Đường Phan Văn Trị, TP. Hồ Chí Minh'),
    (5,N'Võ Đức Huy','vo.duc.huy@example.com',N'91 Đường Quang Trung, TP. Hồ Chí Minh'),
    (6,N'Đặng Minh Anh','dang.minh.anh@example.com',N'28 Đường Nguyễn Oanh, TP. Hồ Chí Minh'),
    (7,N'Bùi Thành Đạt','bui.thanh.dat@example.com',N'63 Đường Lê Đức Thọ, TP. Hồ Chí Minh'),
    (8,N'Đỗ Khánh Linh','do.khanh.linh@example.com',N'14 Đường Phạm Văn Đồng, TP. Hồ Chí Minh');

    DECLARE @i INT=1;
    WHILE @i<=8
    BEGIN
        INSERT INTO KhachHang(maKH,tenKH,gioiTinh,ngaySinh,sdt,email,diaChi)
        SELECT 'KH88'+RIGHT('000'+CAST(@i AS VARCHAR(3)),3),hoTen,
               @i%2,'19950101','09998800'+RIGHT('00'+CAST(@i AS VARCHAR(2)),2),email,diaChi
        FROM @TenKhach WHERE stt=@i;
        INSERT INTO HoSoBenhAn(maHSBA,maKH,tienSuBenh)
        VALUES('HS88'+RIGHT('000'+CAST(@i AS VARCHAR(3)),3),'KH88'+RIGHT('000'+CAST(@i AS VARCHAR(3)),3),N'H4_DEMO_20261105 - hồ sơ mô phỏng');
        SET @i+=1;
    END;

    DECLARE @d INT=0,@slot INT,@day DATE,@time TIME(0),@at DATETIME2(0),
            @code CHAR(6),@suffix CHAR(3),@lh VARCHAR(15),@pk VARCHAR(15),@hd VARCHAR(15),
            @kh VARCHAR(15),@status VARCHAR(10),@pkStatus VARCHAR(10),@paid BIT,@price DECIMAL(18,2);
    WHILE @d<=7
    BEGIN
        SET @day=DATEADD(DAY,@d,@Start);
        SET @code=CONVERT(CHAR(6),@day,12);
        INSERT INTO LichLamViec(maLLV,maNV,ngayTruc,caTruc,gioBatDau,gioKetThuc,trangThai)
        VALUES('LLV'+CAST(88000+@d AS VARCHAR(5)),'NV103',@day,'CA_NGAY','07:00','17:30','DA_PHAN_CONG');
        SET @slot=1;
        WHILE @slot<=CASE WHEN @d=0 THEN 8 ELSE 3 END
        BEGIN
            SET @suffix=RIGHT('000'+CAST(800+@slot AS VARCHAR(3)),3);
            SET @lh='LH'+@code+@suffix;
            SET @pk='PK'+@code+@suffix;
            SET @hd='HD'+@code+@suffix;
            SET @kh='KH88'+RIGHT('000'+CAST(@slot AS VARCHAR(3)),3);
            IF EXISTS(SELECT 1 FROM LichHen WHERE maLH=@lh)
               OR EXISTS(SELECT 1 FROM PhieuKham WHERE maPK=@pk)
               OR EXISTS(SELECT 1 FROM HoaDon WHERE maHD=@hd)
                THROW 51405,N'Mã lịch/phiếu/hóa đơn trùng; rollback toàn bộ.',1;
            SET @time=CASE WHEN @d>0 THEN CASE @slot WHEN 1 THEN '08:00' WHEN 2 THEN '10:00' ELSE '14:00' END
                ELSE CASE @slot WHEN 1 THEN '07:00' WHEN 2 THEN '08:00' WHEN 3 THEN '08:50'
                WHEN 4 THEN '10:00' WHEN 5 THEN '11:00' WHEN 6 THEN '14:00' WHEN 7 THEN '15:00' ELSE '16:00' END END;
            SET @at=DATEADD(SECOND,DATEDIFF(SECOND,CAST('00:00' AS TIME),@time),CAST(@day AS DATETIME2(0)));
            SET @status=CASE WHEN @d>0 THEN CASE WHEN @slot<=2 THEN 'DA_DEN' ELSE 'DA_DAT' END
                ELSE CASE WHEN @slot<=2 THEN 'HOAN_TAT' WHEN @slot<=5 THEN 'DA_DEN'
                WHEN @slot=6 THEN 'DA_DAT' WHEN @slot=7 THEN 'DA_HUY' ELSE 'KHONG_DEN' END END;
            INSERT INTO LichHen(maLH,maKH,maNV,maDV,ngayGioHen,trangThai,ghiChu,lyDoHuy)
            VALUES(@lh,@kh,'NV103','DV001',@at,@status,
                   N'H4_DEMO_20261105 - MÔ PHỎNG, gồm tình huống tương lai; không phải lượt khám thực',
                   CASE WHEN @status='DA_HUY' THEN N'Hủy mô phỏng để xem trạng thái H4' END);

            IF (@d=0 AND @slot<=5) OR (@d>0 AND @slot<=2)
            BEGIN
                SET @pkStatus=CASE WHEN (@d=0 AND @slot<=3) OR @d>0 THEN 'HOAN_TAT'
                                      WHEN @slot=4 THEN 'DANG_KHAM' ELSE 'CHO_KHAM' END;
                -- Thêm chi tiết trước khi khóa phiếu hoàn tất.
                INSERT INTO PhieuKham(maPK,maHSBA,maLH,maNV_LapPhieu,ngayLap,trangThai,loiDan)
                VALUES(@pk,'HS88'+RIGHT('000'+CAST(@slot AS VARCHAR(3)),3),@lh,'NV003',@at,'CHO_KHAM',
                       N'H4_DEMO_20261105 - Dữ liệu mô phỏng, không dùng điều trị thực');
                IF @pkStatus<>'CHO_KHAM'
                BEGIN
                    SET @price=100000+@d*20000+@slot*50000;
                    INSERT INTO ChiTietPhieuKham(maPK,maDV,maNV_NhaSi,trieuChung,chanDoan,ketQuaDieuTri,donViTinh,soLuong,donGia,mucThueGTGT)
                    VALUES(@pk,'DV001','NV103',N'Mô phỏng H4',N'Chẩn đoán mẫu H4',N'Kết quả mô phỏng',N'Lần',1,@price,0);
                    UPDATE PhieuKham SET trangThai=@pkStatus WHERE maPK=@pk;
                END;
                IF @pkStatus='HOAN_TAT'
                BEGIN
                    SET @paid=CASE WHEN @d=0 AND @slot<=2 THEN 1 ELSE 0 END;
                    INSERT INTO HoaDon(maHD,maPK,maNV_ThuNgan,ngayLap,trangThai,hinhThucThanhToan,ngayThanhToan,tongKhuyenMai)
                    VALUES(@hd,@pk,CASE WHEN @paid=1 THEN 'NV003' END,DATEADD(MINUTE,30,@at),@paid,
                           CASE WHEN @paid=1 THEN CASE WHEN @slot=1 THEN N'Tiền mặt' ELSE N'Chuyển khoản' END END,
                           CASE WHEN @paid=1 THEN DATEADD(MINUTE,35,@at) END,0);
                END;
            END;
            SET @slot+=1;
        END;
        SET @d+=1;
    END;
    INSERT INTO NhatKyHeThong(maNhatKy,maTK,thoiDiem,hanhDong,noiDungThayDoi)
    VALUES('NK880005','TK001',SYSDATETIME(),N'H4_DEMO_20261105',
           N'Bổ sung dữ liệu mô phỏng H4 05–12/11/2026: 8 KH/HS, 8 ca, 29 lịch, 19 phiếu, 17 hóa đơn; không phải giao dịch thực.');
    SELECT CAST(lh.ngayGioHen AS DATE) AS ngay,COUNT(*) AS lich_hen
    FROM LichHen lh WHERE lh.ghiChu LIKE N'H4_DEMO_20261105%' GROUP BY CAST(lh.ngayGioHen AS DATE) ORDER BY ngay;
    SELECT CAST(hd.ngayLap AS DATE) AS ngay,COUNT(*) AS hoa_don,
           SUM(CAST(hd.trangThai AS INT)) AS da_tra,
           SUM(ct.thanhTien) AS tam_tinh
    FROM HoaDon hd JOIN PhieuKham pk ON pk.maPK=hd.maPK
    JOIN ChiTietPhieuKham ct ON ct.maPK=pk.maPK
    WHERE pk.loiDan LIKE N'H4_DEMO_20261105%'
    GROUP BY CAST(hd.ngayLap AS DATE) ORDER BY ngay;
    IF @Preview=1 BEGIN ROLLBACK; SELECT N'PREVIEW_ROLLED_BACK' AS ketQua; END
    ELSE BEGIN COMMIT; SELECT N'H4_DEMO_COMMITTED' AS ketQua; END;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK;
    THROW;
END CATCH;
