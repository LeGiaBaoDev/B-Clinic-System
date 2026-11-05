-- Chỉ đọc; kiểm tra bộ mẫu H4 đã commit, không reset dữ liệu cũ.
SET NOCOUNT ON;
IF DB_NAME()<>N'BClinic_Dev' THROW 51410,N'Chỉ kiểm tra trên BClinic_Dev.',1;
IF (SELECT COUNT(*) FROM KhachHang WHERE maKH BETWEEN 'KH88001' AND 'KH88008')<>8
    THROW 51411,N'Sai số khách mẫu H4.',1;
IF (SELECT COUNT(*) FROM LichHen WHERE ghiChu LIKE N'H4_DEMO_20261105%')<>29
    THROW 51412,N'Sai số lịch mẫu H4.',1;
IF (SELECT COUNT(*) FROM PhieuKham WHERE loiDan LIKE N'H4_DEMO_20261105%')<>19
    THROW 51413,N'Sai số phiếu mẫu H4.',1;
IF (SELECT COUNT(*) FROM HoaDon h JOIN PhieuKham p ON p.maPK=h.maPK WHERE p.loiDan LIKE N'H4_DEMO_20261105%')<>17
    THROW 51414,N'Sai số hóa đơn mẫu H4.',1;
SELECT CAST(ngayGioHen AS DATE) AS ngay,COUNT(*) AS tong_lich,
       SUM(CASE WHEN trangThai='DA_DAT' THEN 1 ELSE 0 END) AS da_dat,
       SUM(CASE WHEN trangThai='DA_DEN' THEN 1 ELSE 0 END) AS da_den,
       SUM(CASE WHEN trangThai='HOAN_TAT' THEN 1 ELSE 0 END) AS hoan_tat,
       SUM(CASE WHEN trangThai='DA_HUY' THEN 1 ELSE 0 END) AS da_huy,
       SUM(CASE WHEN trangThai='KHONG_DEN' THEN 1 ELSE 0 END) AS khong_den
FROM LichHen WHERE ghiChu LIKE N'H4_DEMO_20261105%'
GROUP BY CAST(ngayGioHen AS DATE) ORDER BY ngay;
SELECT N'H4_PERSISTED_DATA_VERIFIED' AS ketQua;
