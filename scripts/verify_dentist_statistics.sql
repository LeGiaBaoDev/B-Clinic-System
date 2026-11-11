SET NOCOUNT ON;
DECLARE @nv varchar(15)=(SELECT maNV FROM TaiKhoan WHERE tenTK='hieu.nhasi');
IF EXISTS(SELECT 1 FROM LichHen h JOIN LichHen x ON x.maLH<>h.maLH JOIN DichVu d ON d.maDV=h.maDV JOIN DichVu dx ON dx.maDV=x.maDV
WHERE h.ghiChu=N'DENTIST_STATS_20261105' AND x.trangThai NOT IN ('DA_HUY','KHONG_DEN') AND (h.maNV=x.maNV OR h.maKH=x.maKH)
AND h.ngayGioHen<DATEADD(minute,dx.thoiLuong+15,x.ngayGioHen) AND x.ngayGioHen<DATEADD(minute,d.thoiLuong+15,h.ngayGioHen)) THROW 51899,N'Lịch trùng hoặc thiếu khoảng chuẩn bị.',1;
IF (SELECT COUNT(*) FROM PhieuKham p JOIN LichHen h ON h.maLH=p.maLH WHERE p.loiDan LIKE N'DENTIST_STATS_20261105%' AND p.trangThai='HOAN_TAT' AND h.maNV=@nv)<>5 THROW 51900,N'Chưa đủ 5 phiếu đúng nha sĩ.',1;
IF (SELECT SUM(ct.thanhTien) FROM ChiTietPhieuKham ct JOIN PhieuKham p ON p.maPK=ct.maPK WHERE p.loiDan LIKE N'DENTIST_STATS_20261105%')<>2700000 THROW 51901,N'Sai tổng doanh thu mẫu.',1;
IF NOT EXISTS(SELECT 1 FROM LichLamViec WHERE maNV=@nv AND ngayTruc='20261105' AND trangThai='NGHI_PHEP') THROW 51902,N'Ca nghỉ phép không còn nguyên.',1;
IF EXISTS(SELECT 1 FROM HoaDon hd JOIN PhieuKham p ON p.maPK=hd.maPK WHERE p.loiDan LIKE N'DENTIST_STATS_20261105%' AND hd.maHD<>'HD'+SUBSTRING(p.maPK,3,13)) THROW 51903,N'Mã hóa đơn không khớp phiếu.',1;
SELECT CAST(p.ngayLap AS date) ngay,COUNT(DISTINCT p.maPK) luot,COUNT(DISTINCT hs.maKH) benh_nhan,SUM(ct.thanhTien) doanh_thu
FROM PhieuKham p JOIN LichHen h ON h.maLH=p.maLH JOIN HoSoBenhAn hs ON hs.maHSBA=p.maHSBA JOIN ChiTietPhieuKham ct ON ct.maPK=p.maPK
WHERE p.trangThai='HOAN_TAT' AND h.maNV=@nv AND p.ngayLap>='20261030' AND p.ngayLap<'20261106' GROUP BY CAST(p.ngayLap AS date);
DBCC CHECKCONSTRAINTS WITH ALL_CONSTRAINTS;
