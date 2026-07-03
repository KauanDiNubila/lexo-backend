$mortos = 0

$jarRegex = '(discovery-server|api-gateway|auth-service|cliente-service|processo-service|financeiro-service|auditoria-service|notificacao-service)-0\.1\.0\.jar'
Get-CimInstance Win32_Process |
    Where-Object { $_.CommandLine -match $jarRegex } |
    ForEach-Object {
        Write-Host "  matando java PID $($_.ProcessId)" -ForegroundColor Yellow
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
        $mortos++
    }

Get-CimInstance Win32_Process |
    Where-Object { $_.CommandLine -match 'vite' -and $_.CommandLine -match 'lexo' } |
    ForEach-Object {
        Write-Host "  matando vite PID $($_.ProcessId)" -ForegroundColor Yellow
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
        $mortos++
    }

Write-Host "$mortos processo(s) encerrado(s)." -ForegroundColor Cyan
Write-Host "Infra Docker segue de pe (para parar: docker compose down)."
