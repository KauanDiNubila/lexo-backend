$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$java = "C:\Program Files\Java\jdk-21\bin\java.exe"
$logs = Join-Path $root "scripts\logs"
New-Item -ItemType Directory -Force -Path $logs | Out-Null

if (-not (Test-Path $java)) {
    Write-Host "ERRO: Java 21 nao encontrado em $java" -ForegroundColor Red
    Write-Host "Ajuste a variavel \$java no topo deste script." -ForegroundColor Red
    exit 1
}

$envLocal = Join-Path $PSScriptRoot "env.local.ps1"
if (Test-Path $envLocal) {
    . $envLocal
    if ($env:GEMINI_API_KEY) { Write-Host "GEMINI_API_KEY carregada (Lexo IA usara o Gemini real)." -ForegroundColor Green }
}

function Start-Jar($name) {
    $jar = Join-Path $root "$name\target\$name-0.1.0.jar"
    if (-not (Test-Path $jar)) {
        Write-Host "  ! jar ausente: $name  (rode primeiro: mvn -DskipTests package)" -ForegroundColor Yellow
        return
    }
    Start-Process -FilePath $java -ArgumentList '-jar', $jar `
        -WorkingDirectory $root -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $logs "$name.log") `
        -RedirectStandardError  (Join-Path $logs "$name.err")
    Write-Host "  > $name" -ForegroundColor Green
}

$env:SPRING_PROFILES_ACTIVE = "dev"

Write-Host "== Infra (Docker) ==" -ForegroundColor Cyan
docker compose -f (Join-Path $root "docker-compose.yml") up -d | Out-Null

Write-Host "== Eureka (discovery) ==" -ForegroundColor Cyan
Start-Jar "discovery-server"
Write-Host "  aguardando o Eureka responder..."
$ok = $false
for ($i = 0; $i -lt 45; $i++) {
    try { Invoke-WebRequest -UseBasicParsing http://localhost:8761 -TimeoutSec 2 | Out-Null; $ok = $true; break }
    catch { Start-Sleep -Seconds 2 }
}
if ($ok) { Write-Host "  Eureka pronto." -ForegroundColor Green }
else { Write-Host "  Eureka demorou; seguindo mesmo assim." -ForegroundColor Yellow }

Write-Host "== Servicos ==" -ForegroundColor Cyan
foreach ($s in @(
    "auth-service", "cliente-service", "processo-service",
    "financeiro-service", "auditoria-service", "notificacao-service", "ia-service", "api-gateway")) {
    Start-Jar $s
}

Write-Host "== Frontend (Vite) ==" -ForegroundColor Cyan
Start-Process -FilePath "cmd.exe" -ArgumentList "/c npm run dev" `
    -WorkingDirectory (Join-Path $root "frontend") -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $logs "frontend.log") `
    -RedirectStandardError  (Join-Path $logs "frontend.err")
Write-Host "  > frontend" -ForegroundColor Green

Write-Host ""
Write-Host "Tudo iniciando. Aguarde ~30-40s (os servicos registram no Eureka) e acesse:" -ForegroundColor Cyan
Write-Host "   Frontend:  http://localhost:5173"
Write-Host "   Gateway:   http://localhost:8080/api/health"
Write-Host "   Eureka:    http://localhost:8761"
Write-Host "   Logs:      scripts\logs\"
