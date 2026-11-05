param([Parameter(Mandatory=$true)][string]$InputFile)
$ErrorActionPreference='Stop'
$cfg=@{}
Get-Content (Join-Path $PSScriptRoot '../src/main/resources/database.properties') | ForEach-Object {
    if ($_ -match '^\s*(db\.[^=]+)=(.*)$') { $cfg[$matches[1]]=$matches[2].Trim() }
}
if ($cfg['db.name'] -ne 'BClinic_Dev') { throw 'Only BClinic_Dev is allowed.' }
try {
    $env:SQLCMDPASSWORD=$cfg['db.password']
    & sqlcmd -S "tcp:$($cfg['db.host']),$($cfg['db.port'])" -U $cfg['db.username'] -C -d $cfg['db.name'] -i $InputFile -f 65001 -b -r 1 -w 300
    if ($LASTEXITCODE -ne 0) { throw "sqlcmd failed: $LASTEXITCODE" }
} finally { Remove-Item Env:SQLCMDPASSWORD -ErrorAction SilentlyContinue }
