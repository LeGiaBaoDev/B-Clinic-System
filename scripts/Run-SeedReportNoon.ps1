param([switch]$Apply)
$ErrorActionPreference='Stop'
$mc=Join-Path ([Environment]::GetFolderPath('UserProfile')) '.m2/repository'
$cp=(@('target/classes',
 (Join-Path $mc 'com/microsoft/sqlserver/mssql-jdbc/12.8.1.jre11/mssql-jdbc-12.8.1.jre11.jar'),
 (Join-Path $mc 'com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar'),
 (Join-Path $mc 'org/slf4j/slf4j-api/2.0.16/slf4j-api-2.0.16.jar')))-join ';'
& javac -encoding UTF-8 -cp $cp -d target/seed-tools scripts/SeedFourDays.java scripts/SeedReportNoon.java
if($LASTEXITCODE -ne 0){throw 'Compile failed'}
if($Apply){ & java -cp "target/seed-tools;$cp" SeedReportNoon --apply }
else { & java -cp "target/seed-tools;$cp" SeedReportNoon }
if($LASTEXITCODE -ne 0){throw 'Seed failed'}
