SET NOCOUNT ON;
DECLARE @sql NVARCHAR(MAX)=N'';
SELECT @sql+=N'SELECT '+QUOTENAME(t.name,'''')+N' AS bang, STRING_AGG(CAST('+QUOTENAME(c.name)+N' AS nvarchar(max)), '','') WITHIN GROUP (ORDER BY '+QUOTENAME(c.name)+N') AS ma FROM dbo.'+QUOTENAME(t.name)+N';'
FROM sys.tables t JOIN sys.indexes i ON i.object_id=t.object_id AND i.is_primary_key=1
JOIN sys.index_columns ic ON ic.object_id=i.object_id AND ic.index_id=i.index_id
JOIN sys.columns c ON c.object_id=t.object_id AND c.column_id=ic.column_id
WHERE c.name LIKE 'ma%' AND c.system_type_id IN (167,231);
EXEC sys.sp_executesql @sql;
SELECT OBJECT_NAME(parent_id) bang,name FROM sys.triggers WHERE parent_class=1;

