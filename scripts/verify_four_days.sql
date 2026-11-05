SET NOCOUNT ON;
DECLARE @m nvarchar(100)=N'FOUR_DAYS_REALTIME_20261106';
IF EXISTS(SELECT 1 FROM PhieuKham WHERE ngayLap>='20261106' AND ngayLap<'20261110')
 OR EXISTS(SELECT 1 FROM HoaDon WHERE ngayLap>='20261106' AND ngayLap<'20261110')
 OR EXISTS(SELECT 1 FROM LichHen WHERE ngayGioHen>='20261106' AND ngayGioHen<'20261110' AND trangThai NOT IN ('DA_DAT','DA_HUY'))
 THROW 51699,N'Còn nghiệp vụ khám/thanh toán tương lai trong 4 ngày.',1;
IF EXISTS(SELECT 1 FROM LichHen WHERE ghiChu LIKE @m+'%' AND ngayGioHen>='20261106' AND trangThai NOT IN ('DA_DAT','DA_HUY'))
 THROW 51700,N'Lịch tương lai có trạng thái khám/không đến.',1;
IF EXISTS(SELECT 1 FROM PhieuKham WHERE loiDan LIKE @m+'%' AND ngayLap>='20261106')
 THROW 51701,N'Có phiếu khám mới trong tương lai.',1;
IF EXISTS(SELECT 1 FROM HoaDon hd JOIN PhieuKham p ON p.maPK=hd.maPK WHERE p.loiDan LIKE @m+'%' AND hd.maHD<>'HD'+SUBSTRING(p.maPK,3,13))
 THROW 51702,N'Mã hóa đơn không khớp phiếu.',1;
IF EXISTS(SELECT 1 FROM LichHen h JOIN DichVu d ON d.maDV=h.maDV WHERE h.ghiChu LIKE @m+'%' AND h.trangThai NOT IN ('DA_HUY','KHONG_DEN') AND
 NOT EXISTS(SELECT 1 FROM LichLamViec v JOIN TaiKhoan t ON t.maNV=v.maNV WHERE v.maNV=h.maNV AND v.ngayTruc=CAST(h.ngayGioHen AS date) AND v.trangThai='DA_PHAN_CONG' AND t.trangThai=1 AND t.vaiTro='NHASI' AND CAST(h.ngayGioHen AS time)>=v.gioBatDau AND CAST(DATEADD(minute,d.thoiLuong,h.ngayGioHen) AS time)<=v.gioKetThuc))
 THROW 51703,N'Lịch ngoài ca hoặc tài khoản bị khóa.',1;
IF EXISTS(SELECT 1 FROM LichHen h JOIN LichHen x ON h.maLH<>x.maLH JOIN DichVu d ON d.maDV=h.maDV JOIN DichVu dx ON dx.maDV=x.maDV
 WHERE h.ghiChu LIKE @m+'%' AND h.trangThai NOT IN ('DA_HUY','KHONG_DEN') AND x.trangThai NOT IN ('DA_HUY','KHONG_DEN')
 AND ((h.maNV=x.maNV AND h.ngayGioHen<DATEADD(minute,dx.thoiLuong+15,x.ngayGioHen) AND x.ngayGioHen<DATEADD(minute,d.thoiLuong+15,h.ngayGioHen))
 OR (h.maKH=x.maKH AND h.ngayGioHen<DATEADD(minute,dx.thoiLuong,x.ngayGioHen) AND x.ngayGioHen<DATEADD(minute,d.thoiLuong,h.ngayGioHen))))
 THROW 51704,N'Lịch trùng hoặc không đủ 15 phút.',1;
IF EXISTS(SELECT 1 FROM LichHen h JOIN PhieuKham p ON p.maPK=h.maPKNguon JOIN HoSoBenhAn hs ON hs.maHSBA=p.maHSBA
 WHERE h.ghiChu LIKE @m+'%' AND (p.trangThai<>'HOAN_TAT' OR hs.maKH<>h.maKH OR DATEDIFF(day,p.ngayLap,h.ngayGioHen) NOT BETWEEN 7 AND 14
 OR NOT EXISTS(SELECT 1 FROM ChiTietPhieuKham ct WHERE ct.maPK=p.maPK AND ct.maNV_NhaSi=h.maNV AND ct.maDV=h.maDV)))
 THROW 51705,N'Tái khám không đúng nguồn/khách/nha sĩ/dịch vụ/ngày.',1;
IF EXISTS(SELECT 1 FROM HoaDon hd JOIN PhieuKham p ON p.maPK=hd.maPK JOIN Voucher v ON v.maVoucher=hd.maVoucher
 CROSS APPLY(SELECT SUM(ROUND(ct.thanhTien*(1+ct.mucThueGTGT/100),0)) total FROM ChiTietPhieuKham ct WHERE ct.maPK=p.maPK) s
 CROSS APPLY(SELECT CASE WHEN v.loaiGiam=0 THEN v.giaGiam ELSE ROUND(s.total*v.giaGiam/100,0) END raw) a
 CROSS APPLY(SELECT CASE WHEN v.loaiGiam=1 AND v.giaTriGiamToiDa IS NOT NULL AND a.raw>v.giaTriGiamToiDa THEN v.giaTriGiamToiDa ELSE a.raw END capped) b
 WHERE p.loiDan LIKE @m+'%' AND (v.trangThai<>1 OR CAST(hd.ngayLap AS date) NOT BETWEEN v.ngayBatDau AND v.ngayKetThuc OR hd.tongKhuyenMai<>CASE WHEN b.capped>s.total THEN s.total ELSE b.capped END))
 THROW 51706,N'Voucher hoặc số tiền giảm không hợp lệ.',1;
IF EXISTS(SELECT 1 FROM ChiTietPhieuKham ct JOIN PhieuKham p ON p.maPK=ct.maPK JOIN DichVu d ON d.maDV=ct.maDV JOIN LoaiDichVu l ON l.maLoaiDV=d.maLoaiDV
 WHERE p.loiDan LIKE @m+'%' AND (ct.mucThueGTGT<>l.mucThueGTGT OR ct.donGia<>d.donGia OR d.trangThai<>1))
 THROW 51707,N'Sai giá/thuế/dịch vụ ngừng.',1;
IF (SELECT COUNT(*) FROM LichHen WHERE ghiChu LIKE @m+'%' AND ngayGioHen>='20261106' AND ngayGioHen<'20261110')<>48
 THROW 51708,N'Chưa đủ 12 lịch mỗi ngày tương lai.',1;
IF (SELECT COUNT(*) FROM DonNghiPhep WHERE maNV IN (SELECT maNV FROM TaiKhoan WHERE tenTK='tram.nhasi'))<>3
 THROW 51709,N'Chưa đủ ba tình huống nghỉ phép.',1;
IF EXISTS(SELECT 1 FROM sys.foreign_keys WHERE is_disabled=1 OR is_not_trusted=1)
 OR EXISTS(SELECT 1 FROM sys.triggers WHERE parent_class=1 AND is_disabled=1)
 THROW 51710,N'Ràng buộc/trigger không bật.',1;
IF EXISTS(SELECT 1 FROM LichHen h JOIN DichVu d ON d.maDV=h.maDV
 WHERE h.ghiChu LIKE @m+'%' AND h.trangThai='DA_DAT'
 AND (SELECT COUNT(*) FROM LichHen x WHERE x.maKH=h.maKH AND x.trangThai='DA_HUY' AND YEAR(x.ngayGioHen)=YEAR(h.ngayGioHen) AND MONTH(x.ngayGioHen)=MONTH(h.ngayGioHen))>=3
 AND NOT ((CAST(h.ngayGioHen AS time)>='07:00' AND CAST(DATEADD(minute,d.thoiLuong,h.ngayGioHen) AS time)<='09:30')
 OR (CAST(h.ngayGioHen AS time)>='16:00' AND CAST(DATEADD(minute,d.thoiLuong,h.ngayGioHen) AS time)<='17:30')))
 THROW 51711,N'Khách hủy nhiều lần chưa được giới hạn đầu/cuối ngày.',1;
SELECT 'FOUR_DAYS_VERIFIED' ketQua;
SELECT CAST(ngayGioHen AS date) ngay,trangThai,COUNT(*) so_lich FROM LichHen
WHERE ngayGioHen>='20261106' AND ngayGioHen<'20261110' GROUP BY CAST(ngayGioHen AS date),trangThai ORDER BY ngay,trangThai;
SELECT CAST(ngayLap AS date) ngay,trangThai,COUNT(*) so_phieu FROM PhieuKham
WHERE loiDan LIKE @m+'%' GROUP BY CAST(ngayLap AS date),trangThai ORDER BY ngay,trangThai;
SELECT hd.trangThai,hd.hinhThucThanhToan,COUNT(*) so_hoa_don,SUM(hd.tongKhuyenMai) tong_giam
FROM HoaDon hd JOIN PhieuKham p ON p.maPK=hd.maPK WHERE p.loiDan LIKE @m+'%' GROUP BY hd.trangThai,hd.hinhThucThanhToan;
SELECT ct.mucThueGTGT,COUNT(*) so_dong FROM ChiTietPhieuKham ct JOIN PhieuKham p ON p.maPK=ct.maPK WHERE p.loiDan LIKE @m+'%' GROUP BY ct.mucThueGTGT;
IF EXISTS(SELECT 1 FROM HoaDon WHERE maHD<>'HD'+SUBSTRING(maPK,3,13)) THROW 51712,N'Còn HD lệch phiếu.',1;
DBCC CHECKCONSTRAINTS WITH ALL_CONSTRAINTS;
