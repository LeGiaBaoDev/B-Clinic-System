-- Đơn là dữ liệu riêng; chỉ khi duyệt mới đổi trạng thái ca làm việc.
CREATE TABLE DonNghiPhep (
    maDon VARCHAR(15) NOT NULL,
    maLLV VARCHAR(15) NOT NULL,
    maNV VARCHAR(15) NOT NULL,
    maTKGui VARCHAR(15) NOT NULL,
    ngayTruc DATE NOT NULL,
    caTruc VARCHAR(15) NOT NULL,
    gioBatDau TIME(0) NOT NULL,
    gioKetThuc TIME(0) NOT NULL,
    lyDo NVARCHAR(255) NOT NULL,
    ngayGui DATETIME2(0) NOT NULL CONSTRAINT DF_DonNghiPhep_ngayGui DEFAULT SYSDATETIME(),
    trangThai VARCHAR(15) NOT NULL CONSTRAINT DF_DonNghiPhep_trangThai DEFAULT 'CHO_DUYET',
    maTKXuLy VARCHAR(15) NULL,
    ngayXuLy DATETIME2(0) NULL,
    lyDoTuChoi NVARCHAR(255) NULL,
    CONSTRAINT PK_DonNghiPhep PRIMARY KEY (maDon),
    CONSTRAINT FK_DonNghiPhep_LichLamViec FOREIGN KEY (maLLV) REFERENCES LichLamViec(maLLV),
    CONSTRAINT FK_DonNghiPhep_NhanVien FOREIGN KEY (maNV) REFERENCES NhanVien(maNV),
    CONSTRAINT FK_DonNghiPhep_NguoiGui FOREIGN KEY (maTKGui) REFERENCES TaiKhoan(maTK),
    CONSTRAINT FK_DonNghiPhep_NguoiXuLy FOREIGN KEY (maTKXuLy) REFERENCES TaiKhoan(maTK),
    CONSTRAINT CK_DonNghiPhep_ca CHECK (caTruc IN ('CA_SANG','CA_CHIEU','CA_NGAY') AND gioBatDau<gioKetThuc),
    CONSTRAINT CK_DonNghiPhep_lyDo CHECK (LEN(LTRIM(RTRIM(lyDo)))>0),
    CONSTRAINT CK_DonNghiPhep_ketQua CHECK (
        (trangThai='CHO_DUYET' AND maTKXuLy IS NULL AND ngayXuLy IS NULL AND lyDoTuChoi IS NULL)
        OR (trangThai='DA_DUYET' AND maTKXuLy IS NOT NULL AND ngayXuLy IS NOT NULL AND lyDoTuChoi IS NULL)
        OR (trangThai='TU_CHOI' AND maTKXuLy IS NOT NULL AND ngayXuLy IS NOT NULL AND lyDoTuChoi IS NOT NULL AND LEN(LTRIM(RTRIM(lyDoTuChoi)))>0)
    )
);
CREATE UNIQUE INDEX UQ_DonNghiPhep_caChoDuyet ON DonNghiPhep(maLLV) WHERE trangThai='CHO_DUYET';
CREATE INDEX IX_DonNghiPhep_nhanVien ON DonNghiPhep(maNV,ngayGui);
