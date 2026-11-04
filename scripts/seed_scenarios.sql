-- Chay qua SeedScenarios.java: sau tham so hash la @ngay/@now/@xemTruoc.
-- Chi them du lieu mau, khong sua migration hoac cac ban ghi cu.
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME()<>N'BClinic_Dev' THROW 51200,N'Chỉ nạp tình huống mẫu vào BClinic_Dev.',1;
DECLARE @hash101 VARCHAR(255)=?,@hash102 VARCHAR(255)=?,@hash103 VARCHAR(255)=?,
        @hash104 VARCHAR(255)=?,@hash105 VARCHAR(255)=?,@hash106 VARCHAR(255)=?;
DECLARE @ngay DATE=?,@now DATETIME2(0)=?,@xemTruoc BIT=?;
IF CAST(@now AS DATE)<>@ngay THROW 51201,N'Mốc ngày/giờ không khớp.',1;
IF EXISTS(SELECT 1 FROM NhatKyHeThong WHERE maNhatKy='NK980099' AND hanhDong=N'DEMO_SCENARIOS_V2')
BEGIN
    PRINT N'Đợt dữ liệu nhiều tình huống đã tồn tại; không thêm trùng.';
    RETURN;
END;
IF NOT EXISTS(SELECT 1 FROM NhatKyHeThong WHERE maNhatKy='NK990099' AND hanhDong=N'DEMO_SEED_V1')
    THROW 51202,N'Cần bộ dữ liệu nền seed_demo.sql trước.',1;

BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @lock INT;
    EXEC @lock=sys.sp_getapplock @Resource=N'BClinic:seed-scenarios',@LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=10000;
    IF @lock<0 THROW 51203,N'Đang có lượt nạp dữ liệu khác.',1;
    IF EXISTS(SELECT 1 FROM NhatKyHeThong WHERE maNhatKy='NK980099' AND hanhDong=N'DEMO_SCENARIOS_V2')
    BEGIN COMMIT; RETURN; END;

    INSERT INTO NhanVien(maNV,hoTenNV,gioiTinh,sdt,email,chuyenMon) VALUES
    ('NV101',N'Lê Mai Hương',0,'0918100101','demo.huong@example.com',N'Nha sĩ điều trị tổng quát'),
    ('NV102',N'Trần Hoàng Nam',1,'0918100102','demo.nam@example.com',N'Nha sĩ phục hình và thẩm mỹ'),
    ('NV103',N'Nguyễn Ngọc Lan',0,'0918100103','demo.lan@example.com',N'Nha sĩ răng hàm mặt'),
    ('NV104',N'Phạm Quốc An',1,'0918100104','demo.an@example.com',N'Nha sĩ ca sáng'),
    ('NV105',N'Vũ Thảo Vy',0,'0918100105','demo.vy@example.com',N'Nha sĩ - tài khoản khóa kiểm thử'),
    ('NV106',N'Trần Thu Hà',0,'0918100106','demo.ha@example.com',N'Lễ tân và thu ngân');
    INSERT INTO TaiKhoan(maTK,maNV,tenTK,matKhau,vaiTro,trangThai) VALUES
    ('TK101','NV101','demo.huong',@hash101,'NHASI',1),
    ('TK102','NV102','demo.nam',@hash102,'NHASI',1),
    ('TK103','NV103','demo.lan',@hash103,'NHASI',1),
    ('TK104','NV104','demo.an',@hash104,'NHASI',1),
    ('TK105','NV105','demo.vy.locked',@hash105,'NHASI',0),
    ('TK106','NV106','demo.thuha',@hash106,'LETAN',1);

    INSERT INTO DichVu(maDV,maLoaiDV,tenDV,donGia,thoiLuong,loTrinhDichVu,trangThai) VALUES
    ('DV101','LDV01',N'Điều trị viêm nướu',300000,30,NULL,1),
    ('DV102','LDV03',N'Tẩy trắng răng với máng cá nhân',1800000,60,NULL,1),
    ('DV103','LDV02',N'Bộ vệ sinh răng miệng chuyên dụng',120000,15,NULL,1),
    ('DV104','LDV01',N'Khám kiểm tra sau điều trị',150000,30,NULL,1),
    ('DV105','LDV03',N'Dịch vụ mẫu đã ngừng cung cấp',500000,30,NULL,0);

    DECLARE @ten TABLE(stt INT PRIMARY KEY,ten NVARCHAR(100));
    INSERT INTO @ten VALUES
    (1,N'Phạm Ngọc Lan'),(2,N'Trần Đức Huy'),(3,N'Võ Thanh Tâm'),(4,N'Lê Quốc Bảo'),
    (5,N'Nguyễn Hoài Thương'),(6,N'Đỗ Minh Đức'),(7,N'Bùi Ngọc Ánh'),(8,N'Hồ Thanh Sơn'),
    (9,N'Vũ Phương Linh'),(10,N'Đặng Anh Tuấn'),(11,N'Trương Diệu Anh'),(12,N'Đinh Gia Hân'),
    (13,N'Ngô Quang Huy'),(14,N'Lý Thu Hiền'),(15,N'Tạ Hoàng Phúc'),(16,N'Đoàn Bảo Trâm'),
    (17,N'Mai Đức Anh'),(18,N'Phan Ngọc Hạ'),(19,N'Trần Minh Châu'),(20,N'Nguyễn Khánh Duy'),
    (21,N'Lê Thùy Dung'),(22,N'Võ Hoàng Long'),(23,N'Đặng Thanh Bình'),(24,N'Phạm Bảo Yến'),
    (25,N'Huỳnh Minh Tâm'),(26,N'Vũ Hải Đăng'),(27,N'Bùi Phương Thảo'),(28,N'Ngô Gia Bảo'),
    (29,N'Trần Nhật Minh'),(30,N'Nguyễn Mỹ Linh');
    INSERT INTO KhachHang(maKH,tenKH,gioiTinh,ngaySinh,sdt,email,diaChi)
    SELECT 'KH'+RIGHT('00000'+CAST(100+stt AS VARCHAR(5)),5),ten,stt%2,
        DATEFROMPARTS(1978+stt,1+stt%12,1+stt%27),'092810'+RIGHT('0000'+CAST(stt AS VARCHAR(4)),4),
        'scenario.kh'+CAST(stt AS VARCHAR(2))+'@example.com',N'[MẪU] TP. Hồ Chí Minh' FROM @ten;
    INSERT INTO HoSoBenhAn(maHSBA,maKH,tienSuBenh)
    SELECT 'HS'+RIGHT(maKH,5),maKH,CASE WHEN maKH='KH00101' THEN N'[MẪU] Dị ứng penicillin.'
        WHEN maKH='KH00102' THEN N'[MẪU] Tăng huyết áp, đang theo dõi.'
        WHEN maKH='KH00103' THEN N'[MẪU] Đái tháo đường, kiểm tra trước thủ thuật.'
        ELSE N'[MẪU] Chưa ghi nhận bệnh nền hoặc dị ứng.' END FROM KhachHang WHERE maKH BETWEEN 'KH00101' AND 'KH00130';

    INSERT INTO Voucher(maVoucher,tenVoucher,loaiGiam,giaGiam,giaTriGiamToiDa,ngayBatDau,ngayKetThuc,trangThai) VALUES
    ('VC101',N'[MẪU V2] Giảm 10%, tối đa 150.000đ',1,10,150000,DATEADD(DAY,-60,@ngay),DATEADD(DAY,60,@ngay),1),
    ('VC102',N'[MẪU V2] Giảm 300.000đ, tối đa bằng tổng tiền',0,300000,NULL,DATEADD(DAY,-60,@ngay),DATEADD(DAY,60,@ngay),1),
    ('VC103',N'[MẪU V2] Giảm 5% không trần',1,5,NULL,DATEADD(DAY,-60,@ngay),DATEADD(DAY,60,@ngay),1),
    ('VC104',N'[MẪU V2] Hết hạn',1,8,80000,DATEADD(DAY,-45,@ngay),DATEADD(DAY,-2,@ngay),1),
    ('VC105',N'[MẪU V2] Chưa có hiệu lực',1,12,120000,DATEADD(DAY,5,@ngay),DATEADD(DAY,30,@ngay),1),
    ('VC106',N'[MẪU V2] Bị vô hiệu',0,50000,NULL,DATEADD(DAY,-20,@ngay),DATEADD(DAY,20,@ngay),0);

    -- Ca sang/chieu tach biet; ca ngay cho nhan vien can ca hai buoi.
    DECLARE @day INT=-13,@shift INT=80000;
    WHILE @day<=7
    BEGIN
        DECLARE @date DATE=DATEADD(DAY,@day,@ngay);
        SET @shift+=1;
        INSERT INTO LichLamViec(maLLV,maNV,ngayTruc,caTruc,gioBatDau,gioKetThuc,trangThai)
        VALUES('LLV'+CAST(@shift AS VARCHAR(5)),'NV101',@date,CASE WHEN @day=0 THEN 'CA_NGAY' ELSE 'CA_SANG' END,
            '07:00',CASE WHEN @day=0 THEN '17:30' ELSE '12:00' END,'DA_PHAN_CONG');
        SET @shift+=1;
        INSERT INTO LichLamViec(maLLV,maNV,ngayTruc,caTruc,gioBatDau,gioKetThuc,trangThai)
        VALUES('LLV'+CAST(@shift AS VARCHAR(5)),'NV102',@date,'CA_CHIEU','13:30','17:30','DA_PHAN_CONG');
        SET @shift+=1;
        INSERT INTO LichLamViec(maLLV,maNV,ngayTruc,caTruc,gioBatDau,gioKetThuc,trangThai)
        VALUES('LLV'+CAST(@shift AS VARCHAR(5)),'NV106',@date,'CA_NGAY','07:00','17:30','DA_PHAN_CONG');
        SET @day+=1;
    END;
    INSERT INTO LichLamViec(maLLV,maNV,ngayTruc,caTruc,gioBatDau,gioKetThuc,trangThai,ghiChuNghi) VALUES
    ('LLV80101','NV103',@ngay,'CA_NGAY','07:00','17:30','NGHI_PHEP',N'[MẪU V2] Quản lý duyệt nghỉ việc gia đình.'),
    ('LLV80102','NV103',DATEADD(DAY,1,@ngay),'CA_NGAY','07:00','17:30','NGHI_PHEP',N'[MẪU V2] Nghỉ theo đơn đã duyệt.'),
    ('LLV80103','NV104',@ngay,'CA_SANG','07:00','12:00','DA_PHAN_CONG',NULL),
    ('LLV80104','NV104',DATEADD(DAY,1,@ngay),'CA_SANG','07:00','12:00','DA_PHAN_CONG',NULL);

    DECLARE @luot TABLE(stt INT PRIMARY KEY,ngayHen DATETIME2(0),kh VARCHAR(15),ns VARCHAR(15),dv VARCHAR(15),
        lhState VARCHAR(10),pkState VARCHAR(10) NULL,paid BIT,vc VARCHAR(15) NULL,sourceStt INT NULL);
    DECLARE @d INT=0,@slot INT,@stt INT=0;
    -- Ba luot moi ngay trong 14 ngay; doanh thu va dich vu thay doi theo ngay.
    WHILE @d<14
    BEGIN
        SET @slot=1;
        WHILE @slot<=3
        BEGIN
            SET @stt+=1;
            DECLARE @dt DATETIME2(0)=DATEADD(HOUR,CASE @slot WHEN 1 THEN 8 WHEN 2 THEN 10 ELSE 14 END,CAST(DATEADD(DAY,-@d,@ngay) AS DATETIME2));
            DECLARE @service VARCHAR(15)=CASE WHEN @slot=1 THEN CASE WHEN @d%2=0 THEN 'DV101' ELSE 'DV003' END
                WHEN @slot=2 THEN 'DV005' WHEN @d%2=0 THEN 'DV009' ELSE 'DV102' END;
            DECLARE @duration INT=(SELECT thoiLuong FROM DichVu WHERE maDV=@service);
            DECLARE @finished BIT=CASE WHEN DATEADD(MINUTE,@duration+20,@dt)<=@now THEN 1 ELSE 0 END;
            DECLARE @arrived BIT=CASE WHEN @dt<=@now THEN 1 ELSE 0 END;
            INSERT INTO @luot VALUES(@stt,@dt,'KH'+RIGHT('00000'+CAST(101+(@stt-1)%30 AS VARCHAR(5)),5),
                CASE WHEN @slot=3 THEN 'NV102' ELSE 'NV101' END,@service,
                CASE WHEN @finished=1 THEN 'HOAN_TAT' WHEN @arrived=1 THEN 'DA_DEN' ELSE 'DA_DAT' END,
                CASE WHEN @finished=1 THEN 'HOAN_TAT' WHEN @arrived=1 THEN 'DANG_KHAM' ELSE NULL END,@finished,
                CASE WHEN @finished=0 THEN NULL WHEN @stt%4=0 THEN 'VC101' WHEN @stt%4=1 THEN 'VC102' WHEN @stt%4=2 THEN 'VC103' ELSE NULL END,NULL);
            SET @slot+=1;
        END;
        SET @d+=1;
    END;

    -- Tinh huong trong ngay. Truoc gio hen thi van DA_DAT, khong gia lap da den o tuong lai.
    DECLARE @cho DATETIME2(0)=DATEADD(MINUTE,930,CAST(@ngay AS DATETIME2)); -- 15:30
    DECLARE @kham DATETIME2(0)=DATEADD(MINUTE,990,CAST(@ngay AS DATETIME2)); -- 16:30
    INSERT INTO @luot VALUES
    (51,@cho,'KH00125','NV101','DV001',CASE WHEN @cho<=@now THEN 'DA_DEN' ELSE 'DA_DAT' END,CASE WHEN @cho<=@now THEN 'CHO_KHAM' ELSE NULL END,0,NULL,NULL),
    (52,@kham,'KH00126','NV101','DV001',CASE WHEN @kham<=@now THEN 'DA_DEN' ELSE 'DA_DAT' END,CASE WHEN @kham<=@now THEN 'DANG_KHAM' ELSE NULL END,0,NULL,NULL),
    (53,@cho,'KH00127','NV102','DV001',CASE WHEN DATEADD(MINUTE,40,@cho)<=@now THEN 'DA_DEN' WHEN @cho<=@now THEN 'DA_DEN' ELSE 'DA_DAT' END,
        CASE WHEN DATEADD(MINUTE,40,@cho)<=@now THEN 'HOAN_TAT' WHEN @cho<=@now THEN 'DANG_KHAM' ELSE NULL END,0,NULL,NULL),
    (54,@kham,'KH00128','NV102','DV001',CASE WHEN DATEADD(MINUTE,50,@kham)<=@now THEN 'HOAN_TAT' WHEN @kham<=@now THEN 'DA_DEN' ELSE 'DA_DAT' END,
        CASE WHEN DATEADD(MINUTE,50,@kham)<=@now THEN 'HOAN_TAT' WHEN @kham<=@now THEN 'DANG_KHAM' ELSE NULL END,
        CASE WHEN DATEADD(MINUTE,50,@kham)<=@now THEN 1 ELSE 0 END,CASE WHEN DATEADD(MINUTE,50,@kham)<=@now THEN 'VC102' ELSE NULL END,NULL),
    (55,DATEADD(MINUTE,690,CAST(@ngay AS DATETIME2)),'KH00124','NV101','DV001','DA_HUY',NULL,0,NULL,NULL),
    (56,DATEADD(MINUTE,810,CAST(DATEADD(DAY,-1,@ngay) AS DATETIME2)),'KH00130','NV102','DV001','KHONG_DEN',NULL,0,NULL,NULL),
    (57,DATEADD(MINUTE,810,CAST(@ngay AS DATETIME2)),'KH00130','NV102','DV001',CASE WHEN DATEADD(MINUTE,825,CAST(@ngay AS DATETIME2))<@now THEN 'KHONG_DEN' ELSE 'DA_DAT' END,NULL,0,NULL,NULL),
    (58,DATEADD(HOUR,7,CAST(@ngay AS DATETIME2)),'KH00129','NV101','DV001','DA_HUY',NULL,0,NULL,NULL),
    (59,DATEADD(HOUR,8,CAST(@ngay AS DATETIME2)),'KH00129','NV101','DV001','DA_HUY',NULL,0,NULL,NULL),
    (60,DATEADD(HOUR,9,CAST(@ngay AS DATETIME2)),'KH00129','NV101','DV001','DA_HUY',NULL,0,NULL,NULL),
    (61,DATEADD(HOUR,7,CAST(DATEADD(DAY,1,@ngay) AS DATETIME2)),'KH00129','NV101','DV001','DA_DAT',NULL,0,NULL,NULL),
    (62,DATEADD(HOUR,9,CAST(DATEADD(DAY,1,@ngay) AS DATETIME2)),'KH00123','NV101','DV005','DA_DAT',NULL,0,NULL,23),
    (63,DATEADD(HOUR,14,CAST(DATEADD(DAY,1,@ngay) AS DATETIME2)),'KH00124','NV102','DV009','DA_DAT',NULL,0,NULL,NULL),
    (64,DATEADD(HOUR,10,CAST(DATEADD(DAY,2,@ngay) AS DATETIME2)),'KH00125','NV101','DV104','DA_DAT',NULL,0,NULL,NULL);

    DECLARE @i INT;
    DECLARE visits CURSOR LOCAL FAST_FORWARD FOR SELECT stt FROM @luot ORDER BY stt;
    OPEN visits; FETCH NEXT FROM visits INTO @i;
    WHILE @@FETCH_STATUS=0
    BEGIN
        DECLARE @dateHen DATETIME2(0),@kh VARCHAR(15),@ns VARCHAR(15),@dv VARCHAR(15),@tt VARCHAR(10),@pkState VARCHAR(10),@paid BIT,@vc VARCHAR(15),@src INT;
        SELECT @dateHen=ngayHen,@kh=kh,@ns=ns,@dv=dv,@tt=lhState,@pkState=pkState,@paid=paid,@vc=vc,@src=sourceStt FROM @luot WHERE stt=@i;
        DECLARE @suffix VARCHAR(9)=CONVERT(CHAR(6),@dateHen,12)+RIGHT('000'+CAST(500+@i AS VARCHAR(3)),3);
        DECLARE @lh VARCHAR(15)='LH'+@suffix,@pk VARCHAR(15)='PK'+@suffix,@source VARCHAR(15)=NULL;
        IF @src IS NOT NULL
            SELECT @source='PK'+CONVERT(CHAR(6),ngayHen,12)+RIGHT('000'+CAST(500+stt AS VARCHAR(3)),3) FROM @luot WHERE stt=@src;
        INSERT INTO LichHen(maLH,maKH,maNV,maDV,maPKNguon,ngayGioHen,trangThai,ghiChu,lyDoHuy)
        VALUES(@lh,@kh,@ns,@dv,@source,@dateHen,@tt,N'[MẪU V2] Tình huống kiểm thử mở rộng.',
            CASE WHEN @tt='DA_HUY' THEN N'[MẪU V2] Khách báo bận và yêu cầu hủy.' ELSE NULL END);
        IF @pkState IS NOT NULL
        BEGIN
            INSERT INTO PhieuKham(maPK,maHSBA,maLH,maNV_LapPhieu,ngayLap,trangThai,loiDan)
            VALUES(@pk,'HS'+RIGHT(@kh,5),@lh,'NV106',@dateHen,'CHO_KHAM',N'[MẪU V2] Chăm sóc sau điều trị theo hướng dẫn nha sĩ.');
            IF @pkState IN('DANG_KHAM','HOAN_TAT')
            BEGIN
                INSERT INTO ChiTietPhieuKham(maPK,maDV,maNV_NhaSi,trieuChung,chanDoan,ketQuaDieuTri,donViTinh,soLuong,donGia,mucThueGTGT)
                SELECT @pk,d.maDV,@ns,
                    CASE WHEN d.maLoaiDV='LDV03' THEN N'[MẪU V2] Mong muốn cải thiện màu răng.' ELSE N'[MẪU V2] Ê buốt, đau hoặc viêm nướu.' END,
                    CASE WHEN d.maLoaiDV='LDV03' THEN N'[MẪU V2] Nhiễm màu, chỉ định thẩm mỹ.' WHEN d.maDV='DV005' THEN N'[MẪU V2] Viêm tủy, điều trị theo buổi.' ELSE N'[MẪU V2] Chỉ định điều trị răng miệng.' END,
                    CASE WHEN @pkState='HOAN_TAT' THEN N'[MẪU V2] Hoàn tất buổi điều trị.' ELSE NULL END,
                    N'Lần',1,d.donGia,l.mucThueGTGT FROM DichVu d JOIN LoaiDichVu l ON l.maLoaiDV=d.maLoaiDV WHERE d.maDV=@dv;
                IF @i<=42 AND @i%3=0
                    INSERT INTO ChiTietPhieuKham(maPK,maDV,maNV_NhaSi,trieuChung,chanDoan,ketQuaDieuTri,donViTinh,soLuong,donGia,mucThueGTGT)
                    SELECT @pk,'DV103',@ns,N'[MẪU V2] Chăm sóc tại nhà.',N'[MẪU V2] Cấp dụng cụ vệ sinh.',N'[MẪU V2] Đã bàn giao.',N'Bộ',2,donGia,5 FROM DichVu WHERE maDV='DV103';
                UPDATE PhieuKham SET trangThai=@pkState WHERE maPK=@pk;
            END;
            IF @pkState='HOAN_TAT'
            BEGIN
                DECLARE @tong DECIMAL(18,2),@giam DECIMAL(18,2)=0;
                SELECT @tong=SUM(thanhTien+ROUND(thanhTien*mucThueGTGT/100,0)) FROM ChiTietPhieuKham WHERE maPK=@pk;
                IF @vc IS NOT NULL
                BEGIN
                    SELECT @giam=CASE WHEN loaiGiam=1 THEN ROUND(@tong*giaGiam/100,0) ELSE giaGiam END FROM Voucher WHERE maVoucher=@vc;
                    SELECT @giam=CASE WHEN loaiGiam=1 AND giaTriGiamToiDa>0 AND @giam>giaTriGiamToiDa THEN giaTriGiamToiDa ELSE @giam END FROM Voucher WHERE maVoucher=@vc;
                    IF @giam>@tong SET @giam=@tong;
                END;
                DECLARE @minutes INT=(SELECT thoiLuong FROM DichVu WHERE maDV=@dv);
                INSERT INTO HoaDon(maHD,maPK,maVoucher,maNV_ThuNgan,ngayLap,trangThai,hinhThucThanhToan,ngayThanhToan,tongKhuyenMai)
                VALUES('HD'+@suffix,@pk,@vc,CASE WHEN @paid=1 THEN 'NV106' ELSE NULL END,DATEADD(MINUTE,@minutes+10,@dateHen),@paid,
                    CASE WHEN @paid=0 THEN NULL WHEN @i%2=0 THEN N'Tiền mặt' ELSE N'Chuyển khoản' END,
                    CASE WHEN @paid=1 THEN DATEADD(MINUTE,@minutes+20,@dateHen) ELSE NULL END,@giam);
            END;
        END;
        FETCH NEXT FROM visits INTO @i;
    END;
    CLOSE visits; DEALLOCATE visits;

    INSERT INTO NhatKyHeThong(maNhatKy,maTK,thoiDiem,hanhDong,noiDungThayDoi) VALUES
    ('NK980001','TK101',@now,N'DANG_NHAP',N'[MẪU V2] Sự kiện giả lập nha sĩ đăng nhập.'),
    ('NK980002','TK106',@now,N'THANH_TOAN',N'[MẪU V2] Sự kiện giả lập thu ngân xác nhận thanh toán.'),
    ('NK980003','TK001',@now,N'DUYET_NGHI_PHEP',N'[MẪU V2] Quản lý duyệt nghỉ NV103.'),
    ('NK980099','TK001',@now,N'DEMO_SCENARIOS_V2',N'[MẪU V2] Hoàn tất bộ nhiều tình huống, ngày gốc '+CONVERT(NVARCHAR(10),@ngay,23));

    -- Kiem tra nghiep vu ngay trong transaction truoc khi commit.
    IF EXISTS(SELECT 1 FROM LichHen lh JOIN DichVu dv ON dv.maDV=lh.maDV
        WHERE lh.ghiChu=N'[MẪU V2] Tình huống kiểm thử mở rộng.' AND NOT EXISTS(
            SELECT 1 FROM LichLamViec ll WHERE ll.maNV=lh.maNV AND ll.ngayTruc=CAST(lh.ngayGioHen AS DATE)
            AND ll.trangThai='DA_PHAN_CONG' AND CAST(lh.ngayGioHen AS TIME)>=ll.gioBatDau
            AND CAST(DATEADD(MINUTE,dv.thoiLuong,lh.ngayGioHen) AS TIME)<=ll.gioKetThuc))
        THROW 51204,N'Lịch mới nằm ngoài ca trực.',1;
    IF EXISTS(SELECT 1 FROM HoaDon hd JOIN Voucher v ON v.maVoucher=hd.maVoucher
        WHERE hd.maNV_ThuNgan='NV106' AND (CAST(hd.ngayLap AS DATE) NOT BETWEEN v.ngayBatDau AND v.ngayKetThuc OR v.trangThai=0))
        THROW 51205,N'Hóa đơn mới dùng voucher không hiệu lực.',1;
    IF EXISTS(SELECT 1 FROM HoaDon hd WHERE hd.maNV_ThuNgan='NV106' AND hd.ngayThanhToan>@now)
        THROW 51206,N'Không được giả lập thanh toán ở tương lai.',1;

    SELECT (SELECT COUNT(*) FROM @luot) AS lichHenThem,
        (SELECT COUNT(*) FROM @luot WHERE pkState IS NOT NULL) AS phieuKhamThem,
        (SELECT COUNT(*) FROM @luot WHERE pkState='HOAN_TAT') AS hoaDonThem,
        (SELECT COUNT(*) FROM @luot WHERE paid=1) AS hoaDonDaTraThem;
    IF @xemTruoc=1
    BEGIN ROLLBACK; PRINT N'Xem trước hợp lệ; đã rollback toàn bộ.'; END
    ELSE
    BEGIN COMMIT; PRINT N'Đã thêm bộ nhiều tình huống, không thay dữ liệu cũ.'; END;
END TRY
BEGIN CATCH
    IF CURSOR_STATUS('local','visits')>=0 CLOSE visits;
    IF CURSOR_STATUS('local','visits')>=-1 DEALLOCATE visits;
    IF XACT_STATE()<>0 ROLLBACK;
    THROW;
END CATCH;
