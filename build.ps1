# build.ps1 — Clean, compile, stage assets, report.
$ErrorActionPreference = 'Stop'

# 1. Clean output
if (Test-Path out) { Remove-Item -Recurse -Force out }
New-Item -ItemType Directory -Path out | Out-Null

# 2. Gather all .java sources
$sources = Get-ChildItem -Recurse -Filter *.java -Path src | ForEach-Object { $_.FullName }
Write-Host "[build] Compiling $($sources.Count) source files..."

# 3. Compile
javac -d out @sources
if ($LASTEXITCODE -ne 0) {
    Write-Host "[build] COMPILATION FAILED" -ForegroundColor Red
    exit 1
}

# 4. Stage assets
Write-Host "[build] Copying assets..."
xcopy /E /I /Y src\unseen\assets out\unseen\assets | Out-Null

Write-Host "[build] Done. Run:  java -cp out unseen.game.Game" -ForegroundColor Green
