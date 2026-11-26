SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO
-- Mã vai trò là khóa nghiệp vụ; chỉ chuyển mã, không đổi mật khẩu hay xóa nhân viên.
IF EXISTS(SELECT 1 FROM NhanVien n WHERE NOT EXISTS(SELECT 1 FROM TaiKhoan t WHERE t.maNV=n.maNV))
    THROW 51700,N'Cần cấp vai trò/tài khoản cho mọi nhân viên trước khi chuyển mã.',1;
SELECT n.maNV AS maCu,
    CAST(CASE t.vaiTro WHEN 'QUANLY' THEN 'QL' WHEN 'NHASI' THEN 'NS' ELSE 'LT' END
    +RIGHT('000'+CAST(ROW_NUMBER() OVER(PARTITION BY t.vaiTro ORDER BY n.maNV) AS VARCHAR(10)),3) AS VARCHAR(15)) AS maMoi,
    t.vaiTro
INTO #DoiMa FROM NhanVien n JOIN TaiKhoan t ON t.maNV=n.maNV;
IF EXISTS(SELECT 1 FROM #DoiMa GROUP BY vaiTro HAVING COUNT(*)>999) THROW 51701,N'Vượt 999 nhân viên mỗi vai trò.',1;
CREATE TABLE #KhoaNgoai(id INT IDENTITY(1,1),parentTable NVARCHAR(260),parentColumn SYSNAME,ten SYSNAME,taoLai NVARCHAR(MAX));
INSERT #KhoaNgoai(parentTable,parentColumn,ten,taoLai)
SELECT QUOTENAME(s.name)+'.'+QUOTENAME(t.name),pc.name,f.name,
 N'ALTER TABLE '+QUOTENAME(s.name)+'.'+QUOTENAME(t.name)+N' WITH CHECK ADD CONSTRAINT '+QUOTENAME(f.name)
 +N' FOREIGN KEY('+QUOTENAME(pc.name)+N') REFERENCES dbo.NhanVien(maNV);'
FROM sys.foreign_keys f JOIN sys.foreign_key_columns fc ON fc.constraint_object_id=f.object_id
JOIN sys.tables t ON t.object_id=f.parent_object_id JOIN sys.schemas s ON s.schema_id=t.schema_id
JOIN sys.columns pc ON pc.object_id=fc.parent_object_id AND pc.column_id=fc.parent_column_id
WHERE f.referenced_object_id=OBJECT_ID('dbo.NhanVien');
SELECT QUOTENAME(OBJECT_SCHEMA_NAME(tr.parent_id))+'.'+QUOTENAME(OBJECT_NAME(tr.parent_id)) AS bang,
 QUOTENAME(tr.name) AS ten INTO #TriggerTam FROM sys.triggers tr WHERE tr.is_disabled=0
 AND tr.parent_id IN(SELECT OBJECT_ID(parentTable) FROM #KhoaNgoai);
DECLARE @sql NVARCHAR(MAX)=N'';
SELECT @sql=@sql+N'DISABLE TRIGGER '+ten+N' ON '+bang+N';' FROM #TriggerTam;
EXEC sys.sp_executesql @sql;
SET @sql=N'';
SELECT @sql=@sql+N'ALTER TABLE '+parentTable+N' DROP CONSTRAINT '+QUOTENAME(ten)+N';' FROM #KhoaNgoai;
EXEC sys.sp_executesql @sql;
ALTER TABLE NhanVien DROP CONSTRAINT CK_NhanVien_maNV;
UPDATE n SET maNV=m.maMoi FROM NhanVien n JOIN #DoiMa m ON m.maCu=n.maNV;
DECLARE @i INT=1,@count INT=(SELECT COUNT(*) FROM #KhoaNgoai),@table NVARCHAR(260),@column SYSNAME;
WHILE @i<=@count
BEGIN
 SELECT @table=parentTable,@column=parentColumn FROM #KhoaNgoai WHERE id=@i;
 SET @sql=N'UPDATE t SET '+QUOTENAME(@column)+N'=m.maMoi FROM '+@table+N' t JOIN #DoiMa m ON t.'+QUOTENAME(@column)+N'=m.maCu;';
 EXEC sys.sp_executesql @sql;
 SET @i+=1;
END;
UPDATE TaiKhoan SET tenTK=maNV;
ALTER TABLE NhanVien ADD CONSTRAINT CK_NhanVien_maNV CHECK(LEN(maNV)=5 AND RIGHT(maNV,3)<>'000'
 AND (maNV COLLATE Latin1_General_100_BIN2 LIKE 'QL[0-9][0-9][0-9]'
 OR maNV COLLATE Latin1_General_100_BIN2 LIKE 'NS[0-9][0-9][0-9]'
 OR maNV COLLATE Latin1_General_100_BIN2 LIKE 'LT[0-9][0-9][0-9]'));
ALTER TABLE TaiKhoan ADD CONSTRAINT CK_TaiKhoan_MaDangNhap CHECK(tenTK=maNV AND
 ((vaiTro='QUANLY' AND maNV LIKE 'QL%') OR (vaiTro='NHASI' AND maNV LIKE 'NS%') OR (vaiTro='LETAN' AND maNV LIKE 'LT%')));
SET @sql=N'';
SELECT @sql=@sql+taoLai FROM #KhoaNgoai;
EXEC sys.sp_executesql @sql;
SET @sql=N'';
SELECT @sql=@sql+N'ENABLE TRIGGER '+ten+N' ON '+bang+N';' FROM #TriggerTam;
EXEC sys.sp_executesql @sql;
CREATE TABLE BoDemMaNhanVien (
 vaiTro VARCHAR(10) NOT NULL,
 soCuoi SMALLINT NOT NULL,
 CONSTRAINT PK_BoDemMaNhanVien PRIMARY KEY(vaiTro),
 CONSTRAINT CK_BoDemNV_VaiTro CHECK(vaiTro IN('QUANLY','NHASI','LETAN')),
 CONSTRAINT CK_BoDemNV_So CHECK(soCuoi BETWEEN 0 AND 999)
);
INSERT BoDemMaNhanVien(vaiTro,soCuoi)
SELECT role.vaiTro,COALESCE(MAX(TRY_CONVERT(SMALLINT,RIGHT(m.maMoi,3))),0)
FROM (VALUES('QUANLY'),('NHASI'),('LETAN'))role(vaiTro) LEFT JOIN #DoiMa m ON m.vaiTro=role.vaiTro GROUP BY role.vaiTro;
DROP TABLE #TriggerTam;
DROP TABLE #KhoaNgoai;
DROP TABLE #DoiMa;
GO
CREATE OR ALTER TRIGGER trg_NV_KhongTaiSuDungMa ON NhanVien AFTER INSERT AS
BEGIN
 SET NOCOUNT ON;
 UPDATE b SET soCuoi=CASE WHEN n.so>b.soCuoi THEN n.so ELSE b.soCuoi END
 FROM BoDemMaNhanVien b JOIN(
 SELECT CASE LEFT(maNV,2) WHEN 'QL' THEN 'QUANLY' WHEN 'NS' THEN 'NHASI' ELSE 'LETAN' END AS vaiTro,
 MAX(CONVERT(SMALLINT,RIGHT(maNV,3))) AS so FROM inserted GROUP BY LEFT(maNV,2))n ON n.vaiTro=b.vaiTro;
END;
GO
