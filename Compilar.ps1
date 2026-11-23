$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
New-Item -ItemType Directory -Force bin,work | Out-Null
$fontes = Get-ChildItem -LiteralPath src -Filter '*.java' | Select-Object -ExpandProperty FullName
& javac -encoding UTF-8 -d bin $fontes
if ($LASTEXITCODE -ne 0) { throw 'Falha na compilação.' }
@'
Manifest-Version: 1.0
Main-Class: Main
Class-Path: lib/h2.jar

'@ | Set-Content -Encoding ASCII work\MANIFEST.MF
& jar cfm FreshStock.jar work\MANIFEST.MF -C bin .
if ($LASTEXITCODE -ne 0) { throw 'Falha ao gerar o aplicativo.' }
Write-Host 'Compilado. Abra Abrir-FreshStock.vbs.'
