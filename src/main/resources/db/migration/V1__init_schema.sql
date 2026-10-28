    CREATE TABLE NhanVien (
        maNV        VARCHAR(15)   NOT NULL CONSTRAINT PK_NhanVien PRIMARY KEY
        CONSTRAINT CK_NhanVien_maNV CHECK (maNV LIKE 'NV[0-9][0-9][0-9]'),
        hoTenNV     NVARCHAR(100) NOT NULL,
        gioiTinh    BIT           NOT NULL,
        sdt         VARCHAR(15)   NOT NULL,
        email       VARCHAR(100)  NOT NULL,
        chuyenMon   NVARCHAR(100) NOT NULL,
        CONSTRAINT UQ_NhanVien_sdt   UNIQUE (sdt),
        CONSTRAINT UQ_NhanVien_email UNIQUE (email)
    );
    GO

    CREATE TABLE TaiKhoan (
        maTK        VARCHAR(15)   NOT NULL CONSTRAINT PK_TaiKhoan PRIMARY KEY
        CONSTRAINT CK_TaiKhoan_maTK CHECK (maTK LIKE 'TK[0-9][0-9][0-9]'),
        maNV        VARCHAR(15)   NOT NULL,
        tenTK       VARCHAR(50)   NOT NULL,
        matKhau     VARCHAR(255)  NOT NULL,
        vaiTro      VARCHAR(10)   NOT NULL,
        trangThai   BIT           NOT NULL CONSTRAINT DF_TaiKhoan_trangThai DEFAULT 1,
        CONSTRAINT UQ_TaiKhoan_tenTK UNIQUE (tenTK),
        CONSTRAINT UQ_TaiKhoan_maNV  UNIQUE (maNV),
        CONSTRAINT FK_TaiKhoan_NhanVien FOREIGN KEY (maNV) REFERENCES NhanVien(maNV),
        CONSTRAINT CK_TaiKhoan_vaiTro CHECK (vaiTro IN ('QUANLY','NHASI','LETAN'))
    );
    GO

    CREATE TABLE LichLamViec (
        maLLV       VARCHAR(15)   NOT NULL CONSTRAINT PK_LichLamViec PRIMARY KEY
        CONSTRAINT CK_LichLamViec_maLLV CHECK (maLLV LIKE 'LLV[0-9][0-9][0-9][0-9][0-9]'),
        maNV        VARCHAR(15)   NOT NULL,
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
        CONSTRAINT CHK_CaTruc_KhopGio CHECK (
            (caTruc = 'CA_SANG'  AND gioBatDau  >= '07:00' AND gioKetThuc <= '12:00')
            OR (caTruc = 'CA_CHIEU' AND gioBatDau  >= '13:30' AND gioKetThuc <= '17:30')
            OR (caTruc = 'CA_NGAY'  AND gioBatDau  >= '07:00' AND gioKetThuc <= '17:30')),
        CONSTRAINT CHK_Nghi_CoLyDo CHECK (
            trangThai <> 'NGHI_PHEP' OR (ghiChuNghi IS NOT NULL AND LEN(LTRIM(RTRIM(ghiChuNghi))) > 0))
    );
    GO

    CREATE TABLE KhachHang (
        maKH        VARCHAR(15)   NOT NULL CONSTRAINT PK_KhachHang PRIMARY KEY
        CONSTRAINT CK_KhachHang_maKH CHECK (maKH LIKE 'KH[0-9][0-9][0-9][0-9][0-9]'),
        tenKH       NVARCHAR(100) NOT NULL,
        gioiTinh    BIT           NOT NULL,
        ngaySinh    DATE          NULL,
        sdt         VARCHAR(15)   NOT NULL,
        email       VARCHAR(100)  NULL,
        diaChi      NVARCHAR(255) NULL,
        CONSTRAINT UQ_KhachHang_sdt UNIQUE (sdt),
        CONSTRAINT CK_KhachHang_ngaySinh CHECK (ngaySinh IS NULL OR ngaySinh <= CAST(GETDATE() AS DATE))
    );
    GO

    CREATE TABLE HoSoBenhAn (
        maHSBA      VARCHAR(15)   NOT NULL CONSTRAINT PK_HoSoBenhAn PRIMARY KEY
        CONSTRAINT CK_HoSoBenhAn_maHSBA CHECK (maHSBA LIKE 'HS[0-9][0-9][0-9][0-9][0-9]'),
        maKH        VARCHAR(15)   NOT NULL,
        tienSuBenh  NVARCHAR(MAX) NULL,
        CONSTRAINT UQ_HSBA_maKH UNIQUE (maKH),
        CONSTRAINT FK_HSBA_KhachHang FOREIGN KEY (maKH) REFERENCES KhachHang(maKH)
    );
    GO

    CREATE TABLE LoaiDichVu (
        maLoaiDV    VARCHAR(15)   NOT NULL CONSTRAINT PK_LoaiDichVu PRIMARY KEY
        CONSTRAINT CK_LoaiDichVu_maLoaiDV CHECK (maLoaiDV LIKE 'LDV[0-9][0-9]'),
        tenLoaiDV   NVARCHAR(100) NOT NULL,
        mucThueGTGT DECIMAL(5,2)  NOT NULL,
        CONSTRAINT CK_LoaiDV_thue CHECK (mucThueGTGT IN (0.00, 5.00, 10.00)),
        CONSTRAINT CK_LoaiDV_tenKhopThue CHECK (
               (tenLoaiDV = N'Điều trị bệnh lý'         AND mucThueGTGT = 0.00)
            OR (tenLoaiDV = N'Vật tư/thiết bị bán kèm'  AND mucThueGTGT = 5.00)
            OR (tenLoaiDV = N'Thẩm mỹ'                  AND mucThueGTGT = 10.00))
    );
    GO

    CREATE TABLE DichVu (
        maDV            VARCHAR(15)   NOT NULL CONSTRAINT PK_DichVu PRIMARY KEY
        CONSTRAINT CK_DichVu_maDV CHECK (maDV LIKE 'DV[0-9][0-9][0-9]'),
        maLoaiDV        VARCHAR(15)    NOT NULL,
        tenDV           NVARCHAR(150)  NOT NULL,
        donGia          DECIMAL(18,2)  NOT NULL,
        thoiLuong       INT            NOT NULL,
        loTrinhDichVu   NVARCHAR(MAX)  NULL,
        trangThai       BIT            NOT NULL CONSTRAINT DF_DichVu_trangThai DEFAULT 1,
        CONSTRAINT FK_DichVu_Loai      FOREIGN KEY (maLoaiDV) REFERENCES LoaiDichVu(maLoaiDV),
        CONSTRAINT CK_DichVu_donGia    CHECK (donGia > 0),
        CONSTRAINT CK_DichVu_thoiLuong CHECK (thoiLuong > 0)
    );
    GO

    CREATE TABLE LichHen (
        maLH        VARCHAR(15)   NOT NULL CONSTRAINT PK_LichHen PRIMARY KEY
        CONSTRAINT CK_LichHen_maLH CHECK (maLH LIKE 'LH[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]'),
        maKH        VARCHAR(15)   NOT NULL,
        maNV        VARCHAR(15)   NOT NULL,
        maDV        VARCHAR(15)   NOT NULL,
        maPKNguon   VARCHAR(15)   NULL,
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

    CREATE TABLE PhieuKham (
        maPK            VARCHAR(15)   NOT NULL CONSTRAINT PK_PhieuKham PRIMARY KEY
        CONSTRAINT CK_PhieuKham_maPK CHECK (maPK LIKE 'PK[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]'),
        maHSBA          VARCHAR(15)   NOT NULL,
        maLH            VARCHAR(15)   NULL,
        maNV_LapPhieu   VARCHAR(15)   NOT NULL,
        ngayLap         DATETIME2(0)  NOT NULL CONSTRAINT DF_PK_ngayLap DEFAULT SYSDATETIME(),
        trangThai       VARCHAR(10)   NOT NULL CONSTRAINT DF_PK_trangThai DEFAULT 'CHO_KHAM',
        loiDan          NVARCHAR(500) NULL,
        CONSTRAINT FK_PK_HSBA     FOREIGN KEY (maHSBA)        REFERENCES HoSoBenhAn(maHSBA),
        CONSTRAINT FK_PK_LichHen  FOREIGN KEY (maLH)          REFERENCES LichHen(maLH),
        CONSTRAINT FK_PK_NhanVien FOREIGN KEY (maNV_LapPhieu) REFERENCES NhanVien(maNV),
        CONSTRAINT CK_PK_trangThai CHECK (trangThai IN ('CHO_KHAM','DANG_KHAM','HOAN_TAT'))
    );
    GO

    CREATE UNIQUE INDEX UX_PhieuKham_maLH ON PhieuKham(maLH) WHERE maLH IS NOT NULL;
    GO

    ALTER TABLE LichHen ADD CONSTRAINT FK_LichHen_PKNguon
        FOREIGN KEY (maPKNguon) REFERENCES PhieuKham(maPK);
    GO

    CREATE TABLE ChiTietPhieuKham (
        maCTPK          INT IDENTITY(1,1) CONSTRAINT PK_CTPK PRIMARY KEY,
        maPK            VARCHAR(15)    NOT NULL,
        maDV            VARCHAR(15)    NOT NULL,
        maNV_NhaSi      VARCHAR(15)    NOT NULL,
        trieuChung      NVARCHAR(500)  NOT NULL,
        chanDoan        NVARCHAR(500)  NOT NULL,
        ketQuaDieuTri   NVARCHAR(500)  NULL,
        donViTinh       NVARCHAR(30)   NOT NULL,
        soLuong         INT            NOT NULL,
        donGia          DECIMAL(18,2)  NOT NULL,
        mucThueGTGT     DECIMAL(5,2)   NOT NULL,
        thanhTien       AS (CONVERT(DECIMAL(18,2), soLuong * donGia)) PERSISTED,
        CONSTRAINT FK_CTPK_PhieuKham FOREIGN KEY (maPK)       REFERENCES PhieuKham(maPK) ON DELETE CASCADE,
        CONSTRAINT FK_CTPK_DichVu    FOREIGN KEY (maDV)       REFERENCES DichVu(maDV),
        CONSTRAINT FK_CTPK_NhaSi     FOREIGN KEY (maNV_NhaSi) REFERENCES NhanVien(maNV),
        CONSTRAINT CK_CTPK_soLuong CHECK (soLuong > 0),
        CONSTRAINT CK_CTPK_donGia  CHECK (donGia > 0),
        CONSTRAINT CK_CTPK_thue    CHECK (mucThueGTGT IN (0.00, 5.00, 10.00))
    );
    GO

    CREATE TABLE Voucher (
        maVoucher           VARCHAR(15)   NOT NULL CONSTRAINT PK_Voucher PRIMARY KEY
        CONSTRAINT CK_Voucher_maVoucher CHECK (maVoucher LIKE 'VC[0-9][0-9][0-9]'),
        tenVoucher          NVARCHAR(100) NOT NULL,
        loaiGiam            TINYINT       NOT NULL,
        giaGiam             DECIMAL(18,2) NOT NULL,
        giaTriGiamToiDa     DECIMAL(18,2) NULL,
        ngayBatDau          DATE          NOT NULL,
        ngayKetThuc         DATE          NOT NULL,
        trangThai           BIT           NOT NULL CONSTRAINT DF_Voucher_trangThai DEFAULT 1,
        CONSTRAINT CK_Voucher_loaiGiam CHECK (loaiGiam IN (0, 1)),
        CONSTRAINT CK_Voucher_giaGiam  CHECK (giaGiam > 0),
        CONSTRAINT CK_Voucher_ngay     CHECK (ngayKetThuc > ngayBatDau),
        CONSTRAINT CK_Voucher_phanTram CHECK (loaiGiam <> 1 OR giaGiam <= 100)
    );
    GO

    CREATE TABLE HoaDon (
        maHD               VARCHAR(15)   NOT NULL CONSTRAINT PK_HoaDon PRIMARY KEY
        CONSTRAINT CK_HoaDon_maHD CHECK (maHD LIKE 'HD[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]'),
        maPK               VARCHAR(15)   NOT NULL,
        maVoucher          VARCHAR(15)   NULL,
        maNV_ThuNgan       VARCHAR(15)   NULL,
        ngayLap            DATETIME2(0)  NOT NULL CONSTRAINT DF_HD_ngayLap DEFAULT SYSDATETIME(),
        trangThai          BIT           NOT NULL CONSTRAINT DF_HD_trangThai DEFAULT 0,
        hinhThucThanhToan  NVARCHAR(30)  NULL,
        ngayThanhToan      DATETIME2(0)  NULL,
        tongKhuyenMai      DECIMAL(18,2) NOT NULL CONSTRAINT DF_HD_km DEFAULT 0,
        CONSTRAINT UQ_HoaDon_maPK UNIQUE (maPK),
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

    CREATE TABLE NhatKyHeThong (
        maNhatKy         VARCHAR(15)   NOT NULL CONSTRAINT PK_NhatKy PRIMARY KEY
        CONSTRAINT CK_NhatKyHeThong_maNhatKy CHECK (maNhatKy LIKE 'NK[0-9][0-9][0-9][0-9][0-9][0-9]'),
        maTK             VARCHAR(15)   NOT NULL,
        thoiDiem         DATETIME2(0)  NOT NULL CONSTRAINT DF_NhatKy_thoiDiem DEFAULT SYSDATETIME(),
        hanhDong         NVARCHAR(100) NOT NULL,
        noiDungThayDoi   NVARCHAR(MAX) NULL,
        CONSTRAINT FK_NhatKy_TaiKhoan FOREIGN KEY (maTK) REFERENCES TaiKhoan(maTK)
    );
    GO

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

    CREATE TRIGGER trg_KiemTraKhoangCachLichHen
    ON LichHen
    AFTER INSERT, UPDATE
    AS
    BEGIN
        SET NOCOUNT ON;

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

    CREATE TRIGGER trg_KhoaDuLieuPhieuKhamHoanTat
    ON PhieuKham
    AFTER UPDATE, DELETE
    AS
    BEGIN
        SET NOCOUNT ON;

        IF EXISTS (SELECT 1 FROM deleted d WHERE d.trangThai = 'HOAN_TAT')
        AND NOT EXISTS (SELECT 1 FROM inserted)
        BEGIN
            RAISERROR (N'Phiếu khám đã hoàn tất, không được phép xóa!', 16, 1);
            ROLLBACK TRANSACTION;
            RETURN;
        END

        IF EXISTS (
            SELECT 1 FROM inserted i
            INNER JOIN deleted d ON i.maPK = d.maPK
            WHERE d.trangThai = 'HOAN_TAT'
        )
        BEGIN
            RAISERROR (N'Phiếu khám đã hoàn tất, không được phép chỉnh sửa!', 16, 1);
            ROLLBACK TRANSACTION;
            RETURN;
        END
    END;
    GO

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
