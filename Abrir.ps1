$ErrorActionPreference = 'Stop'
try {
    $python = Join-Path $PSScriptRoot '.venv\Scripts\pythonw.exe'
    if (-not (Test-Path -LiteralPath $python -PathType Leaf)) {
        throw 'Prepare o ambiente conforme o README.md.'
    }
    $app = Join-Path $PSScriptRoot 'app.py'
    Start-Process -FilePath $python -ArgumentList ('"{0}"' -f $app) -WorkingDirectory $PSScriptRoot -WindowStyle Hidden
} catch {
    Add-Type -AssemblyName System.Windows.Forms
    [void][System.Windows.Forms.MessageBox]::Show(
        'Confira a instalacao do FreshStock. ' + $_.Exception.Message,
        'FreshStock')
}
