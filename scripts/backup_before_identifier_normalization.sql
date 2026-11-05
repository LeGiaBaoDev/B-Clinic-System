SET NOCOUNT ON;
IF DB_NAME()<>N'BClinic_Dev' THROW 51610,N'Chỉ cho phép BClinic_Dev.',1;
DECLARE @dir nvarchar(4000)=CAST(SERVERPROPERTY('InstanceDefaultBackupPath') AS nvarchar(4000));
IF @dir IS NULL THROW 51611,N'Không tìm thấy thư mục backup SQL Server.',1;
DECLARE @file nvarchar(4000)=@dir+CASE WHEN RIGHT(@dir,1) IN ('\','/') THEN '' ELSE '\' END
 +N'BClinic_Dev_before_identifiers_'+CONVERT(char(8),GETDATE(),112)+N'_'+REPLACE(CONVERT(char(8),GETDATE(),108),':','')+N'.bak';
BACKUP DATABASE BClinic_Dev TO DISK=@file WITH COPY_ONLY,CHECKSUM;
RESTORE VERIFYONLY FROM DISK=@file WITH CHECKSUM;
SELECT @file AS backup_file;
