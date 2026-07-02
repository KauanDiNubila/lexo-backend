# =====================================================================
#  Lexo — derruba todos os processos da stack (servicos Java + Vite).
#  Nao mexe na infra Docker (use 'docker compose down' se quiser parar os containers).
#  Uso:  powershell -ExecutionPolicy Bypass -File scripts\stop-all.ps1
# =====================================================================
$mortos = 0

# Servicos Java (jars do lexo-backend)
Get-CimInstance Win32_Process |
    Where-Object { $_.CommandLine -match 'lexo-backend.*target.*\.jar' } |
    ForEach-Object {
        Write-Host "  matando java PID $($_.ProcessId)" -ForegroundColor Yellow
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
        $mortos++
    }

# Frontend (Vite / node servindo o dev server)
Get-CimInstance Win32_Process |
    Where-Object { $_.CommandLine -match 'vite' -and $_.CommandLine -match 'lexo' } |
    ForEach-Object {
        Write-Host "  matando vite PID $($_.ProcessId)" -ForegroundColor Yellow
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
        $mortos++
    }

Write-Host "$mortos processo(s) encerrado(s)." -ForegroundColor Cyan
Write-Host "Infra Docker segue de pe (para parar: docker compose down)."
