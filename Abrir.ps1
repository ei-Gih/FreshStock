$ErrorActionPreference = 'Stop'
try {
    Set-Location -LiteralPath $PSScriptRoot
    if (Test-Path -LiteralPath '.gemini-key.xml') {
        $cred = Import-Clixml -LiteralPath '.gemini-key.xml'
        $env:GEMINI_API_KEY = $cred.GetNetworkCredential().Password
    }
    $java = (Get-Command javaw.exe -ErrorAction Stop).Source
    Start-Process -FilePath $java -ArgumentList '-Dfile.encoding=UTF-8 -jar FreshStock.jar' -WorkingDirectory $PSScriptRoot -WindowStyle Hidden
} catch {
    Add-Type -AssemblyName System.Windows.Forms
    [System.Windows.Forms.MessageBox]::Show('Não foi possível abrir. Verifique se o Java está instalado. ' + $_.Exception.Message, 'FreshStock')
}
