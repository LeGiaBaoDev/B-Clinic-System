param([switch]$Apply)
$ErrorActionPreference='Stop'
$mavenCache=Join-Path ([Environment]::GetFolderPath('UserProfile')) '.m2/repository'
$jars=@(
 (Join-Path $mavenCache 'com/microsoft/sqlserver/mssql-jdbc/12.8.1.jre11/mssql-jdbc-12.8.1.jre11.jar'),
 (Join-Path $mavenCache 'com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar'),
 (Join-Path $mavenCache 'org/slf4j/slf4j-api/2.0.16/slf4j-api-2.0.16.jar')
)
$classpath=(@('target/classes')+$jars)-join ';'
if($Apply) { & java -cp $classpath scripts/SeedFourDays.java --apply }
else { & java -cp $classpath scripts/SeedFourDays.java }
if($LASTEXITCODE -ne 0) {throw 'SeedFourDays failed'}
