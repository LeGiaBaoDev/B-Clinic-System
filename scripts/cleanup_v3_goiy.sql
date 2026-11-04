-- Xóa phần đã lỡ áp dụng của V3 (danh mục gợi ý khám) trên BClinic_Dev.
-- Chạy thủ công trong SSMS khi đang chọn database BClinic_Dev.
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() <> N'BClinic_Dev'
    THROW 53000, N'Chỉ chạy trên BClinic_Dev.', 1;

IF OBJECT_ID(N'dbo.GoiYKhamBenh', N'U') IS NOT NULL
    DROP TABLE dbo.GoiYKhamBenh;

DELETE FROM dbo.flyway_schema_history WHERE version = '3';
