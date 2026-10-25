    /* =====================================================================
    B-Clinic - Hệ thống quản lý phòng khám nha khoa
    Script tạo CSDL cho SQL Server
    Phiên bản: đã sửa các điểm bất hợp lý so với file gốc:
        [Fix 1] gioiTinh: NVARCHAR(10) → BIT (NhanVien, KhachHang)
        [Fix 2] Voucher: đổi giaTriToiDa → giaTriGiamToiDa (khớp entity Java)
        [Fix 3] Trigger trg_KhoaDuLieuPhieuKhamHoanTat: vá lỗ hổng kiểm tra
                cả trạng thái TRƯỚC và SAU update
        [Fix 4] CHK_CaTruc_KhopGio: bổ sung ràng buộc giờ cho CA_NGAY
        [Fix 5] Bổ sung indexes trên các cột FK còn thiếu
    ===================================================================== */
    IF DB_ID(N'BClinic') IS NULL CREATE DATABASE BClinic;
    GO
    USE BClinic;
    GO

    /* ---------------------------- 1. NHANVIEN ---------------------------- */
    CREATE TABLE NhanVien (
        maNV        INT IDENTITY(1,1) CONSTRAINT PK_NhanVien PRIMARY KEY,
        hoTenNV     NVARCHAR(100) NOT NULL,
        -- [Fix 1] gioiTinh: BIT thay vì NVARCHAR(10) để khớp boolean Java; 1 = Nam, 0 = Nữ
        gioiTinh    BIT           NOT NULL,
        sdt         VARCHAR(15)   NOT NULL,
        email       VARCHAR(100)  NOT NULL,
        chuyenMon   NVARCHAR(100) NULL,
        CONSTRAINT UQ_NhanVien_sdt   UNIQUE (sdt),
        CONSTRAINT UQ_NhanVien_email UNIQUE (email)
    );
    GO

    /* ---------------------------- 2. TAIKHOAN ---------------------------- */
    CREATE TABLE TaiKhoan (
        maTK        INT IDENTITY(1,1) CONSTRAINT PK_TaiKhoan PRIMARY KEY,
        maNV        INT           NOT NULL,
        tenTK       VARCHAR(50)   NOT NULL,
        matKhau     VARCHAR(255)  NOT NULL,           -- lưu mã băm (bcrypt/argon2)
        vaiTro      VARCHAR(10)   NOT NULL,
        trangThai   BIT           NOT NULL CONSTRAINT DF_TaiKhoan_trangThai DEFAULT 1,  -- 1: hoạt động, 0: đã khóa
        CONSTRAINT UQ_TaiKhoan_tenTK UNIQUE (tenTK),
        CONSTRAINT UQ_TaiKhoan_maNV  UNIQUE (maNV),  -- 1 nhân viên - 1 tài khoản
        CONSTRAINT FK_TaiKhoan_NhanVien FOREIGN KEY (maNV) REFERENCES NhanVien(maNV),
        CONSTRAINT CK_TaiKhoan_vaiTro CHECK (vaiTro IN ('QUANLY','NHASI','LETAN'))
    );
    GO

    /* --------------------------- 3. LICHLAMVIEC -------------------------- */
    CREATE TABLE LichLamViec (
        maLLV       INT IDENTITY(1,1) CONSTRAINT PK_LichLamViec PRIMARY KEY,
        maNV        INT           NOT NULL,
        ngayTruc    DATE          NOT NULL,
        caTruc      VARCHAR(10)   NOT NULL,
        gioBatDau   TIME(0)       NOT NULL,
        gioKetThuc  TIME(0)       NOT NULL,
        trangThai   VARCHAR(15)   NOT NULL CONSTRAINT DF_LLV_trangThai DEFAULT 'DA_PHAN_CONG',
        ghiChuNghi  NVARCHAR(255) NULL,
        CONSTRAINT FK_LLV_NhanVien FOREIGN KEY (maNV) REFERENCES NhanVien(maNV),
        CONSTRAINT UQ_LLV_NV_Ngay_Ca UNIQUE (maNV, ngayTruc, caTruc),
        CONSTRAINT CK_LLV_caTruc     CHECK (caTruc IN ('CA_SANG','CA_CHIEU','CA_NGAY')),
        CONSTRAINT CK_LLV_trangThai  CHECK (trangThai IN ('DA_PHAN_CONG','NGHI_PHEP')),
        CONSTRAINT CK_LLV_Gio        CHECK (gioKetThuc > gioBatDau),
        -- [Fix 4] Bổ sung range giờ cho CA_NGAY (07:00–17:00), giữ ràng buộc ca sáng/chiều
        CONSTRAINT CHK_CaTruc_KhopGio CHECK (
            (caTruc = 'CA_SANG'  AND gioBatDau  >= '07:00' AND gioKetThuc <= '12:00')
            OR (caTruc = 'CA_CHIEU' AND gioBatDau  >= '13:30' AND gioKetThuc <= '17:30')
            OR (caTruc = 'CA_NGAY'  AND gioBatDau  >= '07:00' AND gioKetThuc <= '17:30')),
        CONSTRAINT CHK_Nghi_CoLyDo CHECK (
            trangThai <> 'NGHI_PHEP' OR (ghiChuNghi IS NOT NULL AND LEN(LTRIM(RTRIM(ghiChuNghi))) > 0))
    );
    GO

    /* ---------------------------- 4. KHACHHANG --------------------------- */
    CREATE TABLE KhachHang (
        maKH        INT IDENTITY(1,1) CONSTRAINT PK_KhachHang PRIMARY KEY,
        tenKH       NVARCHAR(100) NOT NULL,
        -- [Fix 1] gioiTinh: BIT thay vì NVARCHAR(10); 1 = Nam, 0 = Nữ
        gioiTinh    BIT           NOT NULL,
        ngaySinh    DATE          NULL,
        sdt         VARCHAR(15)   NOT NULL,
        email       VARCHAR(100)  NULL,
        diaChi      NVARCHAR(255) NULL,
        CONSTRAINT UQ_KhachHang_sdt UNIQUE (sdt),
        CONSTRAINT CK_KhachHang_ngaySinh CHECK (ngaySinh IS NULL OR ngaySinh <= CAST(GETDATE() AS DATE))
    );
    GO

    /* --------------------------- 5. HOSOBENHAN --------------------------- */
    CREATE TABLE HoSoBenhAn (
        maHSBA      INT IDENTITY(1,1) CONSTRAINT PK_HoSoBenhAn PRIMARY KEY,
        maKH        INT           NOT NULL,
        tienSuBenh  NVARCHAR(MAX) NULL,
        CONSTRAINT UQ_HSBA_maKH UNIQUE (maKH),       -- 1 khách hàng - 1 hồ sơ
        CONSTRAINT FK_HSBA_KhachHang FOREIGN KEY (maKH) REFERENCES KhachHang(maKH)
    );
    GO

    /* --------------------------- 6. LOAIDICHVU --------------------------- */
    CREATE TABLE LoaiDichVu (
        maLoaiDV    INT IDENTITY(1,1) CONSTRAINT PK_LoaiDichVu PRIMARY KEY,
        tenLoaiDV   NVARCHAR(100) NOT NULL,
        mucThueGTGT DECIMAL(5,2)  NOT NULL,
        CONSTRAINT CK_LoaiDV_thue CHECK (mucThueGTGT IN (0.00, 5.00, 10.00))
    );
    GO

    /* ----------------------------- 7. DICHVU ----------------------------- */
    CREATE TABLE DichVu (
        maDV            INT IDENTITY(1,1) CONSTRAINT PK_DichVu PRIMARY KEY,
        maLoaiDV        INT            NOT NULL,
        tenDV           NVARCHAR(150)  NOT NULL,
        donGia          DECIMAL(18,2)  NOT NULL,
        thoiLuong       INT            NOT NULL,       -- phút
        loTrinhDichVu   NVARCHAR(MAX)  NULL,
        trangThai       BIT            NOT NULL CONSTRAINT DF_DichVu_trangThai DEFAULT 1,  -- 1: đang cung cấp, 0: ngừng
        CONSTRAINT FK_DichVu_Loai      FOREIGN KEY (maLoaiDV) REFERENCES LoaiDichVu(maLoaiDV),
        CONSTRAINT CK_DichVu_donGia    CHECK (donGia > 0),
        CONSTRAINT CK_DichVu_thoiLuong CHECK (thoiLuong > 0)
    );
    GO

    /* ----------------------------- 8. LICHHEN ---------------------------- */
    CREATE TABLE LichHen (
        maLH        INT IDENTITY(1,1) CONSTRAINT PK_LichHen PRIMARY KEY,
        maKH        INT           NOT NULL,
        maNV        INT           NOT NULL,            -- nha sĩ phụ trách
        maDV        INT           NOT NULL,            -- dịch vụ yêu cầu
        maPKNguon   INT           NULL,                -- phiếu khám gốc (lịch tái khám); FK thêm sau khi có PhieuKham
        ngayGioHen  DATETIME2(0)  NOT NULL,
        trangThai   VARCHAR(10)   NOT NULL CONSTRAINT DF_LichHen_trangThai DEFAULT 'DA_DAT',
        ghiChu      NVARCHAR(255) NULL,
        lyDoHuy     NVARCHAR(255) NULL,
        CONSTRAINT FK_LichHen_KhachHang FOREIGN KEY (maKH) REFERENCES KhachHang(maKH),
        CONSTRAINT FK_LichHen_NhanVien  FOREIGN KEY (maNV) REFERENCES NhanVien(maNV),
        CONSTRAINT FK_LichHen_DichVu    FOREIGN KEY (maDV) REFERENCES DichVu(maDV),
        CONSTRAINT CK_LichHen_trangThai CHECK (trangThai IN ('DA_DAT','DA_DEN','HOAN_TAT','DA_HUY','KHONG_DEN')),
        CONSTRAINT CHK_LichHen_Huy CHECK (
            trangThai <> 'DA_HUY' OR (lyDoHuy IS NOT NULL AND LEN(LTRIM(RTRIM(lyDoHuy))) > 0))
    );
    GO

    /* ---------------------------- 9. PHIEUKHAM --------------------------- */
    CREATE TABLE PhieuKham (
        maPK            INT IDENTITY(1,1) CONSTRAINT PK_PhieuKham PRIMARY KEY,
        maHSBA          INT           NOT NULL,
        maLH            INT           NULL,            -- NULL với khách không hẹn trước
        maNV_LapPhieu   INT           NOT NULL,        -- lễ tân lập phiếu
        ngayLap         DATETIME2(0)  NOT NULL CONSTRAINT DF_PK_ngayLap DEFAULT SYSDATETIME(),
        trangThai       VARCHAR(10)   NOT NULL CONSTRAINT DF_PK_trangThai DEFAULT 'CHO_KHAM',
        loiDan          NVARCHAR(500) NULL,
        CONSTRAINT FK_PK_HSBA     FOREIGN KEY (maHSBA)        REFERENCES HoSoBenhAn(maHSBA),
        CONSTRAINT FK_PK_LichHen  FOREIGN KEY (maLH)          REFERENCES LichHen(maLH),
        CONSTRAINT FK_PK_NhanVien FOREIGN KEY (maNV_LapPhieu) REFERENCES NhanVien(maNV),
        CONSTRAINT CK_PK_trangThai CHECK (trangThai IN ('CHO_KHAM','DANG_KHAM','HOAN_TAT'))
    );
    GO

    -- Mỗi lịch hẹn chỉ sinh tối đa 1 phiếu khám; cho phép nhiều phiếu của khách không hẹn trước
    CREATE UNIQUE INDEX UX_PhieuKham_maLH ON PhieuKham(maLH) WHERE maLH IS NOT NULL;
    GO

    -- Khóa ngoại vòng: lịch tái khám → phiếu khám gốc
    ALTER TABLE LichHen ADD CONSTRAINT FK_LichHen_PKNguon
        FOREIGN KEY (maPKNguon) REFERENCES PhieuKham(maPK);
    GO

    /* ------------------------ 10. CHITIETPHIEUKHAM ----------------------- */
    CREATE TABLE ChiTietPhieuKham (
        maCTPK          INT IDENTITY(1,1) CONSTRAINT PK_CTPK PRIMARY KEY,
        maPK            INT            NOT NULL,
        maDV            INT            NOT NULL,
        maNV_NhaSi      INT            NOT NULL,
        trieuChung      NVARCHAR(500)  NULL,
        chanDoan        NVARCHAR(500)  NULL,
        ketQuaDieuTri   NVARCHAR(500)  NULL,
        donViTinh       NVARCHAR(30)   NULL,
        soLuong         INT            NOT NULL,
        donGia          DECIMAL(18,2)  NOT NULL,       -- lưu vết đơn giá tại thời điểm chỉ định
        mucThueGTGT     DECIMAL(5,2)   NOT NULL,       -- lưu vết thuế suất tại thời điểm chỉ định
        thanhTien       AS (CONVERT(DECIMAL(18,2), soLuong * donGia)) PERSISTED,
        CONSTRAINT FK_CTPK_PhieuKham FOREIGN KEY (maPK)       REFERENCES PhieuKham(maPK) ON DELETE CASCADE,
        CONSTRAINT FK_CTPK_DichVu    FOREIGN KEY (maDV)       REFERENCES DichVu(maDV),
        CONSTRAINT FK_CTPK_NhaSi     FOREIGN KEY (maNV_NhaSi) REFERENCES NhanVien(maNV),
        CONSTRAINT CK_CTPK_soLuong CHECK (soLuong > 0),
        CONSTRAINT CK_CTPK_donGia  CHECK (donGia > 0),
        CONSTRAINT CK_CTPK_thue    CHECK (mucThueGTGT IN (0.00, 5.00, 10.00))
    );
    GO

    /* ----------------------------- 11. VOUCHER --------------------------- */
    CREATE TABLE Voucher (
        maVoucher           INT IDENTITY(1,1) CONSTRAINT PK_Voucher PRIMARY KEY,
        tenVoucher          NVARCHAR(100) NOT NULL,
        loaiGiam            TINYINT       NOT NULL,           -- 1: giảm theo %, 0: giảm số tiền cố định
        giaGiam             DECIMAL(18,2) NOT NULL,
        -- [Fix 2] Đổi tên giaTriToiDa → giaTriGiamToiDa cho khớp với entity Java
        giaTriGiamToiDa     DECIMAL(18,2) NULL,               -- mức giảm tối đa (áp dụng khi giảm theo %)
        ngayBatDau          DATE          NOT NULL,
        ngayKetThuc         DATE          NOT NULL,
        trangThai           BIT           NOT NULL CONSTRAINT DF_Voucher_trangThai DEFAULT 1,
        CONSTRAINT CK_Voucher_loaiGiam CHECK (loaiGiam IN (0, 1)),
        CONSTRAINT CK_Voucher_giaGiam  CHECK (giaGiam > 0),
        CONSTRAINT CK_Voucher_ngay     CHECK (ngayKetThuc > ngayBatDau),
        CONSTRAINT CK_Voucher_phanTram CHECK (loaiGiam <> 1 OR giaGiam <= 100)
    );
    GO

    /* ----------------------------- 12. HOADON ---------------------------- */
    CREATE TABLE HoaDon (
        maHD               INT IDENTITY(1,1) CONSTRAINT PK_HoaDon PRIMARY KEY,
        maPK               INT           NOT NULL,
        maVoucher          INT           NULL,
        maNV_ThuNgan       INT           NULL,         -- NULL khi chưa thanh toán
        ngayLap            DATETIME2(0)  NOT NULL CONSTRAINT DF_HD_ngayLap DEFAULT SYSDATETIME(),
        trangThai          BIT           NOT NULL CONSTRAINT DF_HD_trangThai DEFAULT 0,  -- 0: chưa thanh toán, 1: đã thanh toán
        hinhThucThanhToan  NVARCHAR(30)  NULL,
        ngayThanhToan      DATETIME2(0)  NULL,
        tongKhuyenMai      DECIMAL(18,2) NOT NULL CONSTRAINT DF_HD_km DEFAULT 0,
        CONSTRAINT UQ_HoaDon_maPK UNIQUE (maPK),       -- 1 phiếu khám - tối đa 1 hóa đơn
        CONSTRAINT FK_HD_PhieuKham FOREIGN KEY (maPK)         REFERENCES PhieuKham(maPK),
        CONSTRAINT FK_HD_Voucher   FOREIGN KEY (maVoucher)    REFERENCES Voucher(maVoucher),
        CONSTRAINT FK_HD_ThuNgan   FOREIGN KEY (maNV_ThuNgan) REFERENCES NhanVien(maNV),
        CONSTRAINT CK_HD_ngayTT    CHECK (ngayThanhToan IS NULL OR ngayThanhToan >= ngayLap),
        CONSTRAINT CK_HD_km        CHECK (tongKhuyenMai >= 0),
        CONSTRAINT CHK_HoaDon_Voucher CHECK (maVoucher IS NOT NULL OR tongKhuyenMai = 0),
        CONSTRAINT CHK_HoaDon_ThanhToan CHECK (
            (trangThai = 0 AND ngayThanhToan IS NULL     AND hinhThucThanhToan IS NULL     AND maNV_ThuNgan IS NULL)
        OR (trangThai = 1 AND ngayThanhToan IS NOT NULL AND hinhThucThanhToan IS NOT NULL AND maNV_ThuNgan IS NOT NULL))
    );
    GO

    /* ------------------------- 13. NHATKYHETHONG ------------------------- */
    CREATE TABLE NhatKyHeThong (
        maNhatKy         INT IDENTITY(1,1) CONSTRAINT PK_NhatKy PRIMARY KEY,
        maTK             INT           NOT NULL,
        thoiDiem         DATETIME2(0)  NOT NULL CONSTRAINT DF_NhatKy_thoiDiem DEFAULT SYSDATETIME(),
        hanhDong         NVARCHAR(100) NOT NULL,
        noiDungThayDoi   NVARCHAR(MAX) NULL,
        CONSTRAINT FK_NhatKy_TaiKhoan FOREIGN KEY (maTK) REFERENCES TaiKhoan(maTK)
    );
    GO

    /* =====================================================================
    [Fix 5] INDEXES bổ sung trên các cột FK còn thiếu
    (SQL Server tự tạo index cho PK/UNIQUE, nhưng không tự tạo cho FK)
    ===================================================================== */
    CREATE INDEX IX_TaiKhoan_maNV        ON TaiKhoan(maNV);
    CREATE INDEX IX_LichLamViec_maNV     ON LichLamViec(maNV);
    CREATE INDEX IX_HoSoBenhAn_maKH      ON HoSoBenhAn(maKH);
    CREATE INDEX IX_DichVu_maLoaiDV      ON DichVu(maLoaiDV);
    CREATE INDEX IX_LichHen_maKH         ON LichHen(maKH);
    CREATE INDEX IX_LichHen_maNV         ON LichHen(maNV);
    CREATE INDEX IX_LichHen_maDV         ON LichHen(maDV);
    CREATE INDEX IX_LichHen_maPKNguon    ON LichHen(maPKNguon) WHERE maPKNguon IS NOT NULL;
    CREATE INDEX IX_LichHen_ngayGioHen   ON LichHen(ngayGioHen);
    CREATE INDEX IX_PhieuKham_maHSBA     ON PhieuKham(maHSBA);
    CREATE INDEX IX_PhieuKham_maNV       ON PhieuKham(maNV_LapPhieu);
    CREATE INDEX IX_CTPK_maPK            ON ChiTietPhieuKham(maPK);
    CREATE INDEX IX_CTPK_maDV            ON ChiTietPhieuKham(maDV);
    CREATE INDEX IX_CTPK_maNV            ON ChiTietPhieuKham(maNV_NhaSi);
    CREATE INDEX IX_HoaDon_maVoucher     ON HoaDon(maVoucher) WHERE maVoucher IS NOT NULL;
    CREATE INDEX IX_HoaDon_maNV          ON HoaDon(maNV_ThuNgan) WHERE maNV_ThuNgan IS NOT NULL;
    CREATE INDEX IX_NhatKy_maTK          ON NhatKyHeThong(maTK);
    CREATE INDEX IX_NhatKy_thoiDiem      ON NhatKyHeThong(thoiDiem DESC);
    GO

    /* =====================================================================
    TRIGGERS
    ===================================================================== */

    /* Trigger 1: Chống chồng lịch hẹn — khách hàng & nha sĩ cách tối thiểu 15 phút */
    CREATE TRIGGER trg_KiemTraKhoangCachLichHen
    ON LichHen
    AFTER INSERT, UPDATE
    AS
    BEGIN
        SET NOCOUNT ON;

        -- Kiểm tra khách hàng trùng khung giờ
        IF EXISTS (
            SELECT 1
            FROM inserted i
            INNER JOIN LichHen lh ON lh.maKH = i.maKH AND lh.maLH <> i.maLH
            INNER JOIN DichVu dvi ON dvi.maDV = i.maDV
            INNER JOIN DichVu dvl ON dvl.maDV = lh.maDV
            WHERE i.trangThai  IN ('DA_DAT','DA_DEN')
            AND lh.trangThai IN ('DA_DAT','DA_DEN')
            AND i.ngayGioHen  < DATEADD(MINUTE, dvl.thoiLuong, lh.ngayGioHen)
            AND lh.ngayGioHen < DATEADD(MINUTE, dvi.thoiLuong, i.ngayGioHen)
        )
        BEGIN
            RAISERROR (N'Khách hàng đã có lịch hẹn khác trùng khung giờ này!', 16, 1);
            ROLLBACK TRANSACTION;
            RETURN;
        END

        -- Kiểm tra nha sĩ không đủ 15 phút chuẩn bị giữa hai ca
        IF EXISTS (
            SELECT 1
            FROM inserted i
            INNER JOIN LichHen lh ON lh.maNV = i.maNV AND lh.maLH <> i.maLH
            INNER JOIN DichVu dvi ON dvi.maDV = i.maDV
            INNER JOIN DichVu dvl ON dvl.maDV = lh.maDV
            WHERE i.trangThai  IN ('DA_DAT','DA_DEN')
            AND lh.trangThai IN ('DA_DAT','DA_DEN')
            AND i.ngayGioHen  < DATEADD(MINUTE, dvl.thoiLuong + 15, lh.ngayGioHen)
            AND lh.ngayGioHen < DATEADD(MINUTE, dvi.thoiLuong + 15, i.ngayGioHen)
        )
        BEGIN
            RAISERROR (N'Lịch hẹn xung đột hoặc không đủ 15 phút chuẩn bị cho nha sĩ!', 16, 1);
            ROLLBACK TRANSACTION;
            RETURN;
        END
    END;
    GO

    /* Trigger 2: Khóa phiếu khám đã hoàn tất
    [Fix 3] Kiểm tra cả deleted (trạng thái TRƯỚC) và inserted (trạng thái SAU)
    để chặn cả: UPDATE trên phiếu đang HOAN_TAT và bất kỳ UPDATE nào
    khi phiếu đã ở HOAN_TAT trước đó                                       */
    CREATE TRIGGER trg_KhoaDuLieuPhieuKhamHoanTat
    ON PhieuKham
    AFTER UPDATE, DELETE
    AS
    BEGIN
        SET NOCOUNT ON;

        -- Chặn xóa phiếu đã hoàn tất
        IF EXISTS (SELECT 1 FROM deleted d WHERE d.trangThai = 'HOAN_TAT')
        AND NOT EXISTS (SELECT 1 FROM inserted)   -- đây là lệnh DELETE
        BEGIN
            RAISERROR (N'Phiếu khám đã hoàn tất, không được phép xóa!', 16, 1);
            ROLLBACK TRANSACTION;
            RETURN;
        END

        -- [Fix 3] Chặn UPDATE khi phiếu đang hoặc đã ở trạng thái HOAN_TAT
        IF EXISTS (
            SELECT 1 FROM inserted i
            INNER JOIN deleted d ON i.maPK = d.maPK
            WHERE d.trangThai = 'HOAN_TAT'   -- phiếu đã hoàn tất trước khi update
        )
        BEGIN
            RAISERROR (N'Phiếu khám đã hoàn tất, không được phép chỉnh sửa!', 16, 1);
            ROLLBACK TRANSACTION;
            RETURN;
        END
    END;
    GO

    /* Trigger 3: Khóa chi tiết phiếu khi phiếu hoàn tất hoặc hóa đơn đã thanh toán */
    CREATE TRIGGER trg_KhoaChiTietPhieuKham
    ON ChiTietPhieuKham
    AFTER INSERT, UPDATE, DELETE
    AS
    BEGIN
        SET NOCOUNT ON;
        IF EXISTS (
            SELECT 1
            FROM PhieuKham pk
            LEFT JOIN HoaDon hd ON hd.maPK = pk.maPK
            WHERE pk.maPK IN (SELECT maPK FROM inserted UNION SELECT maPK FROM deleted)
            AND (pk.trangThai = 'HOAN_TAT' OR hd.trangThai = 1)
        )
        BEGIN
            RAISERROR (N'Không thể thay đổi dịch vụ của phiếu khám đã hoàn tất hoặc đã thanh toán!', 16, 1);
            ROLLBACK TRANSACTION;
            RETURN;
        END
    END;
    GO
