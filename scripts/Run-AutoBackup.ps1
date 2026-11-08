# Dùng làm Action của Windows Task Scheduler khi ứng dụng không mở.
$ErrorActionPreference='Stop'
Set-Location -LiteralPath (Join-Path $PSScriptRoot '..')
$mc=Join-Path ([Environment]::GetFolderPath('UserProfile')) '.m2/repository'
$cp=(@('target/classes',
 (Join-Path $mc 'com/microsoft/sqlserver/mssql-jdbc/12.8.1.jre11/mssql-jdbc-12.8.1.jre11.jar'),
 (Join-Path $mc 'com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar'),
 (Join-Path $mc 'org/slf4j/slf4j-api/2.0.16/slf4j-api-2.0.16.jar')))-join ';'
& java -cp $cp com.quanlynhakhoa.util.BackupScheduler
if($LASTEXITCODE -ne 0){throw 'Sao luu tu dong that bai; xem lich su va log SQL Server.'}
