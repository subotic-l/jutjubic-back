# Quick Start Script za Klaster Setup
# Pokreće sve servise i testira funkcionalnost

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "JUTJUBIC KLASTER - QUICK START" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# 1. Build i start
Write-Host "1. Building i pokretanje servisa..." -ForegroundColor Yellow
docker-compose up --build -d

Write-Host ""
Write-Host "Čekanje da se servisi podignu (60 sekundi)..." -ForegroundColor Yellow
Start-Sleep -Seconds 60

# 2. Provera statusa
Write-Host ""
Write-Host "2. Provera statusa servisa..." -ForegroundColor Yellow
docker-compose ps

# 3. Health check
Write-Host ""
Write-Host "3. Health check..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "http://localhost/actuator/health" -Method Get
    Write-Host "Health Status: $($health.status)" -ForegroundColor Green
    Write-Host "Components:" -ForegroundColor Green
    $health.components | Format-Table
} catch {
    Write-Host "Health check failed: $_" -ForegroundColor Red
}

# 4. Test load balancing
Write-Host ""
Write-Host "4. Testiranje load balancing (5 zahteva)..." -ForegroundColor Yellow
for ($i = 1; $i -le 5; $i++) {
    try {
        $response = Invoke-RestMethod -Uri "http://localhost/api/cluster/info" -Method Get
        Write-Host "Zahtev $i - Obradila replika: $($response.instance_id)" -ForegroundColor Cyan
    } catch {
        Write-Host "Zahtev $i - Greška: $_" -ForegroundColor Red
    }
    Start-Sleep -Seconds 1
}

# 5. Test CREATE video
Write-Host ""
Write-Host "5. Testiranje kreiranja videa..." -ForegroundColor Yellow
$videoData = @{
    title = "Test Video - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    description = "Automatski kreiran video za testiranje klaster funkcionalnosti"
} | ConvertTo-Json

try {
    $createResponse = Invoke-RestMethod -Uri "http://localhost/api/cluster/videos" `
        -Method Post `
        -Body $videoData `
        -ContentType "application/json"
    Write-Host "Video kreiran uspešno!" -ForegroundColor Green
    Write-Host "Replika: $($createResponse.instance_id)" -ForegroundColor Cyan
    Write-Host "Video ID: $($createResponse.video.id)" -ForegroundColor Cyan
} catch {
    Write-Host "Greška pri kreiranju videa: $_" -ForegroundColor Red
}

# 6. Test GET videos
Write-Host ""
Write-Host "6. Testiranje dohvatanja videa..." -ForegroundColor Yellow
try {
    $videosResponse = Invoke-RestMethod -Uri "http://localhost/api/cluster/videos" -Method Get
    Write-Host "Dohvaćeno $($videosResponse.count) videa" -ForegroundColor Green
    Write-Host "Replika: $($videosResponse.instance_id)" -ForegroundColor Cyan
} catch {
    Write-Host "Greška pri dohvatanju videa: $_" -ForegroundColor Red
}

# Završetak
Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "SETUP ZAVRŠEN!" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Pristup servisima:" -ForegroundColor Yellow
Write-Host "  - API: http://localhost/api/cluster/info" -ForegroundColor White
Write-Host "  - Health: http://localhost/actuator/health" -ForegroundColor White
Write-Host "  - RabbitMQ UI: http://localhost:15672 (guest/guest)" -ForegroundColor White
Write-Host "  - Prometheus: http://localhost:9090" -ForegroundColor White
Write-Host "  - Grafana: http://localhost:3000 (admin/admin)" -ForegroundColor White
Write-Host ""
Write-Host "Za testiranje otpornosti pogledaj CLUSTER_SETUP.md" -ForegroundColor Yellow
Write-Host ""
Write-Host "Logovi: docker-compose logs -f" -ForegroundColor Gray
Write-Host "Stop: docker-compose down" -ForegroundColor Gray
