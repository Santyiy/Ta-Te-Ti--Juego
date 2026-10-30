$ErrorActionPreference = 'Stop'
$fuentes = Get-ChildItem -LiteralPath "$PSScriptRoot/src/redes", "$PSScriptRoot/pruebas/redes" -Filter '*.java'
& javac -encoding UTF-8 --release 11 -d "$PSScriptRoot/out" $fuentes.FullName
if ($LASTEXITCODE -ne 0) { throw 'No se pudo compilar el proyecto.' }
Write-Host 'Compilacion correcta. Ejecuta: java -cp out redes.Servidor'
