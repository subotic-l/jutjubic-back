# Quick Start Script za Klaster Setup
# Pokrece sve servise i testira funkcionalnost

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "JUTJUBIC KLASTER - QUICK START" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# 1. Build i start
Write-Host "1. Building i pokretanje servisa..." -ForegroundColor Yellow
docker-compose up --build -d

Write-Host ""
Write-Host "Cekanje da se servisi podignu (60 sekundi)..." -ForegroundColor Yellow
Start-Sleep -Seconds 60

# 2. Provera statusa
Write-Host ""
Write-Host "2. Provera statusa servisa..." -ForegroundColor Yellow
docker-compose ps

# 3. Ubacivanje test podataka u bazu
Write-Host ""
Write-Host "3. Ubacivanje test podataka u bazu..." -ForegroundColor Yellow
try {
    docker cp src/main/resources/data.sql jutjubic-postgres:/tmp/data.sql
    Write-Host "data.sql kopiran u kontejner" -ForegroundColor Green
    
    docker exec -i jutjubic-postgres psql -U postgres -d jutjubic -f /tmp/data.sql
    Write-Host "Test podaci uspesno ubaceni!" -ForegroundColor Green
} catch {
    Write-Host "Greska pri ubacivanju podataka: $_" -ForegroundColor Red
}

# 4. Health check
Write-Host ""
Write-Host "4. Health check..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "http://localhost/actuator/health" -Method Get
    Write-Host "Health Status: $($health.status)" -ForegroundColor Green
    Write-Host "Components:" -ForegroundColor Green
    $health.components | Format-Table
} catch {
    Write-Host "Health check failed: $_" -ForegroundColor Red
}

# 5. Logovanje korisnika
Write-Host ""
Write-Host "5. Logovanje korisnika..." -ForegroundColor Yellow
$loginData = @{
    email = "marko.petrovic@gmail.com"
    password = "12345678"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "http://localhost/api/auth/login" `
        -Method Post `
        -Body $loginData `
        -ContentType "application/json"
    
    $token = $loginResponse.accessToken
    Write-Host "Uspesno logovanje!" -ForegroundColor Green
    Write-Host "Token dobijen: $($token.Substring(0, 20))..." -ForegroundColor Cyan
} catch {
    Write-Host "Greska pri logovanju: $_" -ForegroundColor Red
    Write-Host "Proverite da li su podaci ubaceni u bazu." -ForegroundColor Yellow
    exit 1
}

# Postavi header sa tokenom za sve dalje zahteve
$headers = @{
    "Authorization" = "Bearer $token"
}

# 6. Test load balancing
Write-Host ""
Write-Host "6. Testiranje load balancing..." -ForegroundColor Yellow
Write-Host "6a. Cluster info endpoint (3 zahteva):" -ForegroundColor Cyan
for ($i = 1; $i -le 3; $i++) {
    try {
        $response = Invoke-RestMethod -Uri "http://localhost/api/cluster/info" -Method Get -Headers $headers
        Write-Host "  Zahtev $i - Obradila replika: $($response.instance_id)" -ForegroundColor Green
    } catch {
        Write-Host "  Zahtev $i - Greska: $_" -ForegroundColor Red
    }
    Start-Sleep -Milliseconds 500
}

Write-Host "6b. Pravi video endpoint (3 zahteva - deljeni podaci iz baze):" -ForegroundColor Cyan
for ($i = 1; $i -le 3; $i++) {
    try {
        $response = Invoke-RestMethod -Uri "http://localhost/api/videos" -Method Get -Headers $headers
        Write-Host "  Zahtev $i - Dobavljeno videa: $($response.Count)" -ForegroundColor Green
    } catch {
        Write-Host "  Zahtev $i - Greska: $_" -ForegroundColor Red
    }
    Start-Sleep -Milliseconds 500
}

# 7. Test CREATE video
Write-Host ""
Write-Host "7. Testiranje kreiranja videa..." -ForegroundColor Yellow
$videoData = @{
    title = "Test Video - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    description = "Automatski kreiran video za testiranje klaster funkcionalnosti"
    createdAt = (Get-Date -Format 'yyyy-MM-ddTHH:mm:ss')
    thumbnailPath = "test-thumbnail.jpg"
    videoUrl = "test-video.mp4"
    views = 0
    likes = 0
    user = @{
        id = 1
    }
} | ConvertTo-Json -Depth 3

try {
    $createResponse = Invoke-RestMethod -Uri "http://localhost/api/cluster/videos" `
        -Method Post `
        -Body $videoData `
        -ContentType "application/json" `
        -Headers $headers
    Write-Host "Video kreiran uspesno!" -ForegroundColor Green
    Write-Host "Replika: $($createResponse.instance_id)" -ForegroundColor Cyan
    Write-Host "Video ID: $($createResponse.video.id)" -ForegroundColor Cyan
} catch {
    Write-Host "Greska pri kreiranju videa: $_" -ForegroundColor Red
}

# 8. Test GET videos
Write-Host ""
Write-Host "8. Testiranje dobavljanja videa..." -ForegroundColor Yellow
try {
    $videosResponse = Invoke-RestMethod -Uri "http://localhost/api/cluster/videos" -Method Get -Headers $headers
    Write-Host "Dobavljeno $($videosResponse.count) videa" -ForegroundColor Green
    Write-Host "Replika: $($videosResponse.instance_id)" -ForegroundColor Cyan
} catch {
    Write-Host "Greska pri dobavljanju videa: $_" -ForegroundColor Red
}

# Zavrsetak
Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "SETUP ZAVRSEN!" -ForegroundColor Green
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
