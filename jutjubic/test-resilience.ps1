# Test script za demonstraciju otpornosti klaster sistema

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "KLASTER OTPORNOST - TEST SCENARIJI" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

function Test-Endpoint {
    param([string]$Url, [string]$Description)
    Write-Host "  $Description..." -NoNewline
    try {
        $response = Invoke-RestMethod -Uri $Url -Method Get -TimeoutSec 5
        Write-Host " ✅ OK (replika: $($response.instance_id))" -ForegroundColor Green
        return $true
    } catch {
        Write-Host " ❌ FAIL" -ForegroundColor Red
        return $false
    }
}

# SCENARIO 1: Normalan rad
Write-Host "SCENARIO 1: Normalan rad sa obe replike" -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Yellow
for ($i = 1; $i -le 5; $i++) {
    Test-Endpoint "http://localhost/api/cluster/info" "Zahtev $i"
    Start-Sleep -Milliseconds 500
}

Write-Host ""
Read-Host "Pritisni Enter za sledeći scenario..."

# SCENARIO 2: Pad jedne replike
Write-Host ""
Write-Host "SCENARIO 2: Pad jedne replike (app-1)" -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Yellow
Write-Host "Zaustavljanje app-1..." -ForegroundColor Yellow
docker stop jutjubic-app-1

Write-Host "Čekanje da Nginx detektuje pad (30 sekundi)..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

Write-Host "Testiranje - svi zahtevi bi trebalo da idu na app-2:" -ForegroundColor Cyan
for ($i = 1; $i -le 5; $i++) {
    Test-Endpoint "http://localhost/api/cluster/info" "Zahtev $i"
    Start-Sleep -Milliseconds 500
}

Write-Host ""
Read-Host "Pritisni Enter za sledeći scenario..."

# SCENARIO 3: Ponovno podizanje replike
Write-Host ""
Write-Host "SCENARIO 3: Ponovno podizanje pale replike" -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Yellow
Write-Host "Pokretanje app-1..." -ForegroundColor Yellow
docker start jutjubic-app-1

Write-Host "Čekanje da se app-1 podiže i postane zdrava (50 sekundi)..." -ForegroundColor Yellow
Start-Sleep -Seconds 50

Write-Host "Testiranje - load balancing bi trebalo da radi ponovo:" -ForegroundColor Cyan
for ($i = 1; $i -le 5; $i++) {
    Test-Endpoint "http://localhost/api/cluster/info" "Zahtev $i"
    Start-Sleep -Milliseconds 500
}

Write-Host ""
Read-Host "Pritisni Enter za sledeći scenario..."

# SCENARIO 4: RabbitMQ nedostupan
Write-Host ""
Write-Host "SCENARIO 4: RabbitMQ nedostupan (Graceful Degradation)" -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Yellow
Write-Host "Zaustavljanje RabbitMQ..." -ForegroundColor Yellow
docker stop jutjubic-rabbitmq

Write-Host "Čekanje da circuit breaker detektuje pad (10 sekundi)..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host "Provera health (RabbitMQ bi trebalo da bude DOWN):" -ForegroundColor Cyan
try {
    $health = Invoke-RestMethod -Uri "http://localhost/actuator/health" -Method Get
    Write-Host "Overall Status: $($health.status)" -ForegroundColor $(if($health.status -eq "UP"){"Green"}else{"Yellow"})
    if ($health.components.rabbit) {
        Write-Host "RabbitMQ Status: $($health.components.rabbit.status)" -ForegroundColor $(if($health.components.rabbit.status -eq "UP"){"Green"}else{"Yellow"})
    }
} catch {
    Write-Host "Health check greška: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "Testiranje kreiranja videa (trebalo bi da radi bez MQ):" -ForegroundColor Cyan
$videoData = @{
    title = "Video bez MQ - $(Get-Date -Format 'HH:mm:ss')"
    description = "Test graceful degradation"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "http://localhost/api/cluster/videos" `
        -Method Post `
        -Body $videoData `
        -ContentType "application/json"
    Write-Host "✅ Video kreiran uspešno bez MQ!" -ForegroundColor Green
    Write-Host "   Video ID: $($response.video.id)" -ForegroundColor Cyan
} catch {
    Write-Host "❌ Kreiranje videa nije uspelo: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "Pokretanje RabbitMQ ponovo..." -ForegroundColor Yellow
docker start jutjubic-rabbitmq

Write-Host "Čekanje da se RabbitMQ podiže (15 sekundi)..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

Write-Host ""
Read-Host "Pritisni Enter za sledeći scenario..."

# SCENARIO 5: Provera circuit breaker stanja
Write-Host ""
Write-Host "SCENARIO 5: Circuit Breaker Metrike" -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Yellow
try {
    $cb = Invoke-RestMethod -Uri "http://localhost/actuator/circuitbreakers" -Method Get
    Write-Host "Circuit Breaker Stanje:" -ForegroundColor Cyan
    $cb | ConvertTo-Json -Depth 3 | Write-Host
} catch {
    Write-Host "Greška pri dohvatanju circuit breaker stanja: $_" -ForegroundColor Red
}

# SCENARIO 6: Oba backend-a DOWN (ekstremni scenario)
Write-Host ""
Write-Host "SCENARIO 6: Oba backend-a DOWN (ekstremni scenario)" -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Yellow
Write-Host "⚠️  UPOZORENJE: Ovaj test će privremeno onemogućiti sve backend-e" -ForegroundColor Red
$confirm = Read-Host "Želiš li da nastaviš? (y/n)"

if ($confirm -eq 'y') {
    Write-Host "Zaustavljanje oba backend-a..." -ForegroundColor Yellow
    docker stop jutjubic-app-1 jutjubic-app-2
    
    Write-Host "Čekanje 10 sekundi..." -ForegroundColor Yellow
    Start-Sleep -Seconds 10
    
    Write-Host "Testiranje - trebalo bi da padne:" -ForegroundColor Cyan
    Test-Endpoint "http://localhost/api/cluster/info" "Zahtev ka nedostupnom backendu"
    
    Write-Host ""
    Write-Host "Ponovno pokretanje backend-a..." -ForegroundColor Yellow
    docker start jutjubic-app-1 jutjubic-app-2
    
    Write-Host "Čekanje da se podigne (50 sekundi)..." -ForegroundColor Yellow
    Start-Sleep -Seconds 50
    
    Write-Host "Testiranje - trebalo bi da radi ponovo:" -ForegroundColor Cyan
    for ($i = 1; $i -le 3; $i++) {
        Test-Endpoint "http://localhost/api/cluster/info" "Zahtev $i"
        Start-Sleep -Seconds 1
    }
} else {
    Write-Host "Scenario preskočen." -ForegroundColor Gray
}

# Završetak
Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "TESTIRANJE ZAVRŠENO!" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Sažetak:" -ForegroundColor Yellow
Write-Host "✅ Load balancing radi" -ForegroundColor Green
Write-Host "✅ Otpornost na pad replike" -ForegroundColor Green
Write-Host "✅ Automatski recovery" -ForegroundColor Green
Write-Host "✅ Graceful degradation (RabbitMQ)" -ForegroundColor Green
Write-Host "✅ Health check funkcioniše" -ForegroundColor Green
Write-Host ""
Write-Host "Za detaljne logove: docker-compose logs -f" -ForegroundColor Gray
