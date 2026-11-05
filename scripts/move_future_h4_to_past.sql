SET NOCOUNT ON;
SET XACT_ABORT ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
DECLARE @Apply bit=0;
IF DB_NAME()<>N'BClinic_Dev' THROW 51800,N'Chỉ BClinic_Dev.',1;
BEGIN TRY
BEGIN TRAN;
DECLARE @lock int;
EXEC @lock=sys.sp_getapplock @Resource=N'BClinic:h4-maintenance',@LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=10000;
IF @lock<0 THROW 51801,N'Không lấy được khóa bảo trì.',1;
CREATE TABLE #Move(oldPK varchar(15),newPK varchar(15),oldLH varchar(15),newLH varchar(15),oldHD varchar(15),newHD varchar(15),at datetime2(0),dentist varchar(15),patient varchar(15),dv varchar(15));
DECLARE @pk varchar(15),@lh varchar(15),@hd varchar(15),@at datetime2(0),@nv varchar(15),@kh varchar(15),@dv varchar(15),@n int,@code char(6),@suffix char(3),@llv varchar(15);
DECLARE rows CURSOR LOCAL FAST_FORWARD FOR
SELECT p.maPK,p.maLH,hd.maHD,DATEADD(day,-14,h.ngayGioHen),h.maNV,h.maKH,h.maDV
FROM PhieuKham p JOIN LichHen h ON h.maLH=p.maLH JOIN HoaDon hd ON hd.maPK=p.maPK
WHERE p.loiDan LIKE N'H4_DEMO_20261105%' AND p.ngayLap>='20261106' AND p.ngayLap<'20261110';
OPEN rows; FETCH NEXT FROM rows INTO @pk,@lh,@hd,@at,@nv,@kh,@dv;
WHILE @@FETCH_STATUS=0 BEGIN
 SET @code=CONVERT(char(6),@at,12);SET @n=1;
 WHILE @n<=999 BEGIN
 SET @suffix=RIGHT('000'+CAST(@n AS varchar(3)),3);
 IF NOT EXISTS(SELECT 1 FROM PhieuKham WHERE maPK='PK'+@code+@suffix)
 AND NOT EXISTS(SELECT 1 FROM HoaDon WHERE maHD='HD'+@code+@suffix)
 AND NOT EXISTS(SELECT 1 FROM LichHen WHERE maLH='LH'+@code+@suffix)
 AND NOT EXISTS(SELECT 1 FROM #Move WHERE newPK='PK'+@code+@suffix) BREAK;
 SET @n+=1;
 END;
 IF @n>999 THROW 51802,N'Hết mã trong ngày.',1;
 INSERT #Move VALUES(@pk,'PK'+@code+@suffix,@lh,'LH'+@code+@suffix,@hd,'HD'+@code+@suffix,@at,@nv,@kh,@dv);
 IF NOT EXISTS(SELECT 1 FROM LichLamViec WHERE maNV=@nv AND ngayTruc=CAST(@at AS date) AND trangThai='DA_PHAN_CONG' AND gioBatDau<=CAST(@at AS time) AND gioKetThuc>=CAST(DATEADD(minute,30,@at) AS time)) BEGIN
 IF EXISTS(SELECT 1 FROM LichLamViec WHERE maNV=@nv AND ngayTruc=CAST(@at AS date)) THROW 51803,N'Ca cũ không phù hợp.',1;
 SET @n=1;
 WHILE EXISTS(SELECT 1 FROM LichLamViec WHERE maLLV='LLV'+RIGHT('00000'+CAST(@n AS varchar(5)),5)) SET @n+=1;
 SET @llv='LLV'+RIGHT('00000'+CAST(@n AS varchar(5)),5);
 INSERT LichLamViec(maLLV,maNV,ngayTruc,caTruc,gioBatDau,gioKetThuc,trangThai) VALUES(@llv,@nv,CAST(@at AS date),'CA_NGAY','07:00','17:30','DA_PHAN_CONG');
 END;
 FETCH NEXT FROM rows INTO @pk,@lh,@hd,@at,@nv,@kh,@dv;
END CLOSE rows; DEALLOCATE rows;
IF EXISTS(SELECT 1 FROM #Move m JOIN LichHen h ON h.maNV=m.dentist OR h.maKH=m.patient
 JOIN DichVu d ON d.maDV=h.maDV JOIN DichVu dm ON dm.maDV=m.dv
 WHERE h.trangThai NOT IN ('DA_HUY','KHONG_DEN') AND h.maLH NOT IN (SELECT oldLH FROM #Move)
 AND m.at<DATEADD(minute,d.thoiLuong+15,h.ngayGioHen) AND h.ngayGioHen<DATEADD(minute,dm.thoiLuong+15,m.at))
 THROW 51804,N'Ngày đích có lịch trùng; rollback.',1;
SELECT * FROM #Move;
-- Chỉ bảo trì các mã/ngày của fixture đã được người dùng cho phép.
ALTER TABLE PhieuKham NOCHECK CONSTRAINT ALL;
ALTER TABLE LichHen NOCHECK CONSTRAINT ALL;
ALTER TABLE HoaDon NOCHECK CONSTRAINT ALL;
ALTER TABLE ChiTietPhieuKham NOCHECK CONSTRAINT ALL;
DISABLE TRIGGER trg_KhoaDuLieuPhieuKhamHoanTat ON PhieuKham;
DISABLE TRIGGER trg_KhoaChiTietPhieuKham ON ChiTietPhieuKham;
DISABLE TRIGGER trg_KiemTraKhoangCachLichHen ON LichHen;
UPDATE ct SET maPK=m.newPK FROM ChiTietPhieuKham ct JOIN #Move m ON m.oldPK=ct.maPK;
UPDATE h SET maPKNguon=m.newPK FROM LichHen h JOIN #Move m ON m.oldPK=h.maPKNguon;
UPDATE p SET maPK=m.newPK,maLH=m.newLH,ngayLap=m.at FROM PhieuKham p JOIN #Move m ON m.oldPK=p.maPK;
UPDATE h SET maLH=m.newLH,ngayGioHen=m.at,trangThai='HOAN_TAT' FROM LichHen h JOIN #Move m ON m.oldLH=h.maLH;
UPDATE h SET maHD=m.newHD,maPK=m.newPK,ngayLap=DATEADD(minute,30,m.at),ngayThanhToan=CASE WHEN h.ngayThanhToan IS NULL THEN NULL ELSE DATEADD(minute,35,m.at) END FROM HoaDon h JOIN #Move m ON m.oldHD=h.maHD;
-- Sửa 5 mã HD bị lệch STT phiếu sau lượt đánh số độc lập trước đó.
CREATE TABLE #HD(old varchar(15),new varchar(15),tmp varchar(15));
INSERT #HD SELECT maHD,'HD'+SUBSTRING(maPK,3,13),'HD991231'+RIGHT('000'+CAST(ROW_NUMBER() OVER(ORDER BY maHD) AS varchar(3)),3) FROM HoaDon WHERE maHD<>'HD'+SUBSTRING(maPK,3,13);
IF EXISTS(SELECT 1 FROM #HD m JOIN HoaDon h ON h.maHD=m.tmp) THROW 51805,N'Mã tạm HD đã tồn tại.',1;
UPDATE h SET maHD=m.tmp FROM HoaDon h JOIN #HD m ON m.old=h.maHD;
UPDATE h SET maHD=m.new FROM HoaDon h JOIN #HD m ON m.tmp=h.maHD;
ALTER TABLE PhieuKham WITH CHECK CHECK CONSTRAINT ALL;
ALTER TABLE LichHen WITH CHECK CHECK CONSTRAINT ALL;
ALTER TABLE HoaDon WITH CHECK CHECK CONSTRAINT ALL;
ALTER TABLE ChiTietPhieuKham WITH CHECK CHECK CONSTRAINT ALL;
ENABLE TRIGGER trg_KhoaDuLieuPhieuKhamHoanTat ON PhieuKham;
ENABLE TRIGGER trg_KhoaChiTietPhieuKham ON ChiTietPhieuKham;
ENABLE TRIGGER trg_KiemTraKhoangCachLichHen ON LichHen;
IF EXISTS(SELECT 1 FROM HoaDon WHERE maHD<>'HD'+SUBSTRING(maPK,3,13)) THROW 51806,N'HD chưa khớp phiếu.',1;
IF EXISTS(SELECT 1 FROM PhieuKham WHERE ngayLap>='20261106' AND ngayLap<'20261110') THROW 51807,N'Còn phiếu khám tương lai trong 4 ngày.',1;
SELECT COUNT(*) AS so_luot_chuyen FROM #Move;
IF @Apply=1 BEGIN COMMIT;SELECT 'H4_MOVED_COMMITTED' ketQua;END
ELSE BEGIN ROLLBACK;SELECT 'H4_MOVE_PREVIEW_ROLLED_BACK' ketQua;END;
END TRY
BEGIN CATCH
IF @@TRANCOUNT>0 ROLLBACK;
THROW;
END CATCH;
