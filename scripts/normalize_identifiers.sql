-- Bảo trì dữ liệu mẫu, mặc định preview rồi rollback.
SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
IF DB_NAME()<>N'BClinic_Dev' THROW 51600,N'Chỉ cho phép BClinic_Dev.',1;
DECLARE @Apply bit=0;
BEGIN TRY
BEGIN TRAN;
CREATE TABLE #Config(t sysname,c sysname,p varchar(8),digits int,dated bit);
INSERT #Config VALUES
('NhanVien','maNV','NV',3,0),('TaiKhoan','maTK','TK',3,0),
('KhachHang','maKH','KH',5,0),('HoSoBenhAn','maHSBA','HS',5,0),
('LoaiDichVu','maLoaiDV','LDV',2,0),('DichVu','maDV','DV',3,0),
('LichLamViec','maLLV','LLV',5,0),('DonNghiPhep','maDon','DNP',6,0),
('Voucher','maVoucher','VC',3,0),('LichHen','maLH','LH',3,1),
('PhieuKham','maPK','PK',3,1),('HoaDon','maHD','HD',3,1);
CREATE TABLE #Map(id int IDENTITY PRIMARY KEY,t sysname,c sysname,old varchar(15),new varchar(15),tmp varchar(15));
DECLARE @t sysname,@c sysname,@p varchar(8),@d int,@dated bit,@sql nvarchar(max),@count bigint;
DECLARE tables CURSOR LOCAL FAST_FORWARD FOR SELECT name FROM sys.tables WHERE name NOT IN ('flyway_schema_history','sysdiagrams') ORDER BY name;
OPEN tables; FETCH NEXT FROM tables INTO @t;
WHILE @@FETCH_STATUS=0 BEGIN
 SET @sql=N'SELECT @n=COUNT_BIG(*) FROM dbo.'+QUOTENAME(@t)+N' WITH(TABLOCKX,HOLDLOCK);';
 EXEC sys.sp_executesql @sql,N'@n bigint OUTPUT',@count OUTPUT;
 FETCH NEXT FROM tables INTO @t;
END CLOSE tables; DEALLOCATE tables;
DECLARE configs CURSOR LOCAL FAST_FORWARD FOR SELECT t,c,p,digits,dated FROM #Config;
OPEN configs; FETCH NEXT FROM configs INTO @t,@c,@p,@d,@dated;
WHILE @@FETCH_STATUS=0 BEGIN
 SET @sql=N'IF EXISTS(SELECT 1 FROM dbo.'+QUOTENAME(@t)+N' WHERE '+QUOTENAME(@c)+N' NOT LIKE @prefix+''%'' OR LEN('+QUOTENAME(@c)+N')<>LEN(@prefix)+@digits+CASE WHEN @dated=1 THEN 6 ELSE 0 END OR TRY_CONVERT(bigint,SUBSTRING('+QUOTENAME(@c)+N',LEN(@prefix)+1,15)) IS NULL) THROW 51601,N''Mã sai định dạng; dừng để rà soát.'',1;
 INSERT #Map(t,c,old,new)
 SELECT @table,@column,'+QUOTENAME(@c)+N',g+RIGHT(REPLICATE(''0'',@digits)+CAST(ROW_NUMBER() OVER(PARTITION BY g ORDER BY '+QUOTENAME(@c)+N') AS varchar(15)),@digits)
 FROM dbo.'+QUOTENAME(@t)+N' CROSS APPLY(SELECT @prefix+CASE WHEN @dated=1 THEN SUBSTRING('+QUOTENAME(@c)+N',LEN(@prefix)+1,6) ELSE '''' END g) x;';
 EXEC sys.sp_executesql @sql,N'@prefix varchar(8),@digits int,@dated bit,@table sysname,@column sysname',@p,@d,@dated,@t,@c;
 FETCH NEXT FROM configs INTO @t,@c,@p,@d,@dated;
END CLOSE configs; DEALLOCATE configs;
-- HD luôn lấy STT của phiếu tương ứng, không đánh số độc lập.
DELETE #Map WHERE t='HoaDon';
INSERT #Map(t,c,old,new)
SELECT 'HoaDon','maHD',h.maHD,'HD'+SUBSTRING(COALESCE(p.new,h.maPK),3,13)
FROM HoaDon h LEFT JOIN #Map p ON p.t='PhieuKham' AND p.c='maPK' AND p.old=h.maPK;
DELETE #Map WHERE old=new;
UPDATE #Map SET tmp='ZZ'+RIGHT('0000000000000'+CAST(id AS varchar(13)),13);
SELECT t AS bang,old AS ma_cu,new AS ma_moi FROM #Map ORDER BY t,old;
CREATE TABLE #Columns(t sysname,c sysname,mt sysname,mc sysname,PRIMARY KEY(t,c));
INSERT #Columns SELECT DISTINCT t,c,t,c FROM #Map;
INSERT #Columns
SELECT DISTINCT OBJECT_NAME(f.parent_object_id),COL_NAME(f.parent_object_id,f.parent_column_id),m.t,m.c
FROM sys.foreign_key_columns f JOIN (SELECT DISTINCT t,c FROM #Map) m
ON OBJECT_NAME(f.referenced_object_id)=m.t AND COL_NAME(f.referenced_object_id,f.referenced_column_id)=m.c
WHERE NOT EXISTS(SELECT 1 FROM #Columns x WHERE x.t=OBJECT_NAME(f.parent_object_id) AND x.c=COL_NAME(f.parent_object_id,f.parent_column_id));
-- Snapshot kỳ vọng của mọi cột/mọi dòng, chỉ chuyển giá trị mã đã lập bản đồ.
CREATE TABLE #Expected(t sysname,payload nvarchar(max));
CREATE TABLE #Actual(t sysname,payload nvarchar(max));
DECLARE @cols nvarchar(max);
DECLARE snapshots CURSOR LOCAL FAST_FORWARD FOR SELECT name FROM sys.tables WHERE name NOT IN ('flyway_schema_history','sysdiagrams');
OPEN snapshots; FETCH NEXT FROM snapshots INTO @t;
WHILE @@FETCH_STATUS=0 BEGIN
 SELECT @cols=STRING_AGG(CAST(CASE WHEN x.c IS NULL THEN N'a.'+QUOTENAME(sc.name) ELSE N'COALESCE((SELECT m.new FROM #Map m WHERE m.t='+QUOTENAME(x.mt,'''')+N' AND m.c='+QUOTENAME(x.mc,'''')+N' AND m.old=a.'+QUOTENAME(sc.name)+N'),a.'+QUOTENAME(sc.name)+N')' END+N' AS '+QUOTENAME(sc.name) AS nvarchar(max)),N',') WITHIN GROUP(ORDER BY sc.column_id)
 FROM sys.columns sc LEFT JOIN #Columns x ON x.t=@t AND x.c=sc.name WHERE sc.object_id=OBJECT_ID(N'dbo.'+QUOTENAME(@t));
 SET @sql=N'INSERT #Expected SELECT '+QUOTENAME(@t,'''')+N',CAST((SELECT '+@cols+N' FOR XML PATH(''row''),BINARY BASE64,TYPE) AS nvarchar(max)) FROM dbo.'+QUOTENAME(@t)+N' a;';
 EXEC sys.sp_executesql @sql;
 FETCH NEXT FROM snapshots INTO @t;
END CLOSE snapshots;
-- DDL có transaction: rollback cũng phục hồi trạng thái FK và trigger.
CREATE TABLE #FK(t sysname,n sysname); CREATE TABLE #Triggers(t sysname,n sysname);
INSERT #FK SELECT OBJECT_NAME(parent_object_id),name FROM sys.foreign_keys WHERE is_disabled=0;
INSERT #FK SELECT OBJECT_NAME(parent_object_id),name FROM sys.check_constraints WHERE is_disabled=0;
INSERT #Triggers SELECT OBJECT_NAME(parent_id),name FROM sys.triggers WHERE parent_class=1 AND is_disabled=0;
SELECT @sql=STRING_AGG(CAST(N'ALTER TABLE dbo.'+QUOTENAME(t)+N' NOCHECK CONSTRAINT '+QUOTENAME(n)+N';' AS nvarchar(max)),N'') FROM #FK;
EXEC sys.sp_executesql @sql;
SELECT @sql=STRING_AGG(CAST(N'DISABLE TRIGGER '+QUOTENAME(n)+N' ON dbo.'+QUOTENAME(t)+N';' AS nvarchar(max)),N'') FROM #Triggers;
EXEC sys.sp_executesql @sql;
DECLARE @phase int=1;
WHILE @phase<=2 BEGIN
 SELECT @sql=STRING_AGG(CAST(N'UPDATE a SET '+QUOTENAME(c)+N'=m.'+CASE WHEN @phase=1 THEN N'tmp' ELSE N'new' END+N' FROM dbo.'+QUOTENAME(t)+N' a JOIN #Map m ON m.t='+QUOTENAME(mt,'''')+N' AND m.c='+QUOTENAME(mc,'''')+N' AND a.'+QUOTENAME(c)+N'=m.'+CASE WHEN @phase=1 THEN N'old' ELSE N'tmp' END+N';' AS nvarchar(max)),N'') FROM #Columns;
 EXEC sys.sp_executesql @sql;
 SET @phase+=1;
END;
SELECT @sql=STRING_AGG(CAST(N'ALTER TABLE dbo.'+QUOTENAME(t)+N' WITH CHECK CHECK CONSTRAINT '+QUOTENAME(n)+N';' AS nvarchar(max)),N'') FROM #FK;
EXEC sys.sp_executesql @sql;
SELECT @sql=STRING_AGG(CAST(N'ENABLE TRIGGER '+QUOTENAME(n)+N' ON dbo.'+QUOTENAME(t)+N';' AS nvarchar(max)),N'') FROM #Triggers;
EXEC sys.sp_executesql @sql;
OPEN snapshots; FETCH NEXT FROM snapshots INTO @t;
WHILE @@FETCH_STATUS=0 BEGIN
 SELECT @cols=STRING_AGG(CAST(N'a.'+QUOTENAME(name)+N' AS '+QUOTENAME(name) AS nvarchar(max)),N',') WITHIN GROUP(ORDER BY column_id) FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.'+QUOTENAME(@t));
 SET @sql=N'INSERT #Actual SELECT '+QUOTENAME(@t,'''')+N',CAST((SELECT '+@cols+N' FOR XML PATH(''row''),BINARY BASE64,TYPE) AS nvarchar(max)) FROM dbo.'+QUOTENAME(@t)+N' a;';
 EXEC sys.sp_executesql @sql;
 FETCH NEXT FROM snapshots INTO @t;
END CLOSE snapshots; DEALLOCATE snapshots;
IF EXISTS(SELECT t,payload FROM #Expected EXCEPT SELECT t,payload FROM #Actual)
 OR EXISTS(SELECT t,payload FROM #Actual EXCEPT SELECT t,payload FROM #Expected)
 OR EXISTS(SELECT t,COUNT_BIG(*) n FROM #Expected GROUP BY t EXCEPT SELECT t,COUNT_BIG(*) FROM #Actual GROUP BY t)
 THROW 51602,N'Dữ liệu khác ngoài mã hoặc mất dòng; rollback.',1;
SELECT t AS bang,COUNT(*) AS so_ma_doi FROM #Map GROUP BY t ORDER BY t;
IF @Apply=1 BEGIN COMMIT; SELECT 'IDENTIFIERS_COMMITTED' ketQua; END
ELSE BEGIN ROLLBACK; SELECT 'IDENTIFIERS_PREVIEW_ROLLED_BACK' ketQua; END;
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK;
 THROW;
END CATCH;
