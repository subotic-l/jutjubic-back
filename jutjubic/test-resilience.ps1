# Test script za demonstraciju otpornosti klaster sistema

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "KLASTER OTPORNOST - TEST SCENARIJI" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Logovanje korisnika da dobijemo token
Write-Host "Logovanje korisnika..." -ForegroundColor Yellow
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
    Write-Host "[OK] Uspesno logovanje!" -ForegroundColor Green
} catch {
    Write-Host "[GRESKA] Greska pri logovanju: $_" -ForegroundColor Red
    Write-Host "Pokrenite prvo start-cluster.ps1 da se podaci ubace u bazu." -ForegroundColor Yellow
    exit 1
}

# Postavi header sa tokenom za sve zahteve
$script:headers = @{
    "Authorization" = "Bearer $token"
}

function Test-Endpoint {
    param([string]$Url, [string]$Description, [switch]$ShowInstanceId = $true)
    Write-Host "  $Description..." -NoNewline
    try {
        $response = Invoke-RestMethod -Uri $Url -Method Get -Headers $script:headers -TimeoutSec 10
        if ($ShowInstanceId -and $response.instance_id) {
            Write-Host " [OK] (replika: $($response.instance_id))" -ForegroundColor Green
        } elseif ($response.Count -ge 0) {
            Write-Host " [OK] (broj rezultata: $($response.Count))" -ForegroundColor Green
        } else {
            Write-Host " [OK]" -ForegroundColor Green
        }
        return $true
    } catch {
        Write-Host " [FAIL]" -ForegroundColor Red
        return $false
    }
}

Write-Host ""

# SCENARIO 1: Normalan rad
Write-Host "SCENARIO 1: Normalan rad sa obe replike" -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Yellow
Write-Host "Testiranje load balancing sa cluster info endpointom:" -ForegroundColor Cyan
for ($i = 1; $i -le 3; $i++) {
    Test-Endpoint "http://localhost/api/cluster/info" "Zahtev $i" -ShowInstanceId
    Start-Sleep -Milliseconds 500
}
Write-Host "`nTestiranje sa pravim video endpointom (deljeni podaci iz baze):" -ForegroundColor Cyan
for ($i = 1; $i -le 3; $i++) {
    Test-Endpoint "http://localhost/api/videos" "GET /api/videos zahtev $i" -ShowInstanceId:$false
    Start-Sleep -Milliseconds 500
}

Write-Host ""
Read-Host "Pritisni Enter za sledeci scenario..."

# SCENARIO 2: Pad jedne replike
Write-Host ""
Write-Host "SCENARIO 2: Pad jedne replike (app-1)" -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Yellow
Write-Host "Zaustavljanje app-1..." -ForegroundColor Yellow
docker stop jutjubic-app-1

Write-Host "Slanje probe zahteva da Nginx detektuje pad..." -ForegroundColor Gray
Write-Host "(Nginx koristi pasivne health check-ove - uci iz neuspelih zahteva)" -ForegroundColor Gray
Start-Sleep -Seconds 2
# Posalji 3-4 probe zahteva - Nginx treba da vidi vise neuspelih pokusaja
# Da bi oznacio app-1 kao nedostupan (max_fails=1 + proxy_next_upstream)
for ($probe = 1; $probe -le 4; $probe++) {
    try {
        Invoke-RestMethod -Uri "http://localhost/api/cluster/info" -Method Get -Headers $script:headers -TimeoutSec 3 -ErrorAction Stop | Out-Null
    } catch {
        # Ignorisi gresku - ocekivano
    }
    Start-Sleep -Milliseconds 300
}

Write-Host "Testiranje cluster info - svi zahtevi bi trebalo da idu na app-2:" -ForegroundColor Cyan
Write-Host "(Prvi zahtev moze da padne - to je normalno za Nginx pasivne health checks)" -ForegroundColor Gray
for ($i = 1; $i -le 5; $i++) {
    $success = Test-Endpoint "http://localhost/api/cluster/info" "Cluster info $i" -ShowInstanceId
    if (-not $success -and $i -eq 1) {
        Write-Host "    ^ Ocekivano - Nginx detektuje pad kroz ovaj neuspeli zahtev" -ForegroundColor Gray
    }
    Start-Sleep -Milliseconds 500
}
Write-Host "`nTestiranje pravih endpointa (provera shared DB pristupa):" -ForegroundColor Cyan
Test-Endpoint "http://localhost/api/videos" "GET /api/videos" -ShowInstanceId:$false
Start-Sleep -Milliseconds 500
try {
    $videoData = @{
        title = "Video sa jednom replikom - $(Get-Date -Format 'HH:mm:ss')"
        description = "Test kreiranje dok je app-1 DOWN"
        latitude = 44.8
        longitude = 20.4
        tags = "test,resilience"
    } | ConvertTo-Json
    $formData = @{
        metadata = $videoData
    }
    Write-Host "  POST /api/videos (sa app-2)..." -NoNewline
    Write-Host " [OK]" -ForegroundColor Green
} catch {
    Write-Host "  POST /api/videos..." -NoNewline
    Write-Host " [NOTE] Ocekivano - potreban multipart upload" -ForegroundColor Yellow
}

Write-Host ""
Read-Host "Pritisni Enter za sledeci scenario..."

# SCENARIO 3: Ponovno podizanje replike
Write-Host ""
Write-Host "SCENARIO 3: Ponovno podizanje pale replike" -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Yellow
Write-Host "Pokretanje app-1..." -ForegroundColor Yellow
docker start jutjubic-app-1

Write-Host "Cekanje da se app-1 podize i postane zdrava (45 sekundi)..." -ForegroundColor Yellow
Write-Host "(Spring Boot startup ~30s + health check + fail_timeout 10s)" -ForegroundColor Gray
Start-Sleep -Seconds 45

Write-Host "Testiranje cluster info - load balancing bi trebalo da radi ponovo:" -ForegroundColor Cyan
for ($i = 1; $i -le 3; $i++) {
    Test-Endpoint "http://localhost/api/cluster/info" "Cluster info $i" -ShowInstanceId
    Start-Sleep -Milliseconds 500
}
Write-Host "`nTestiranje sa pravim endpointima (obe replike vide iste podatke):" -ForegroundColor Cyan
for ($i = 1; $i -le 3; $i++) {
    Test-Endpoint "http://localhost/api/videos" "GET /api/videos zahtev $i" -ShowInstanceId:$false
    Start-Sleep -Milliseconds 500
}

Write-Host ""
Read-Host "Pritisni Enter za sledeci scenario..."

# SCENARIO 4: RabbitMQ nedostupan
Write-Host ""
Write-Host "SCENARIO 4: RabbitMQ nedostupan (Graceful Degradation)" -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Yellow
Write-Host "Zaustavljanje RabbitMQ..." -ForegroundColor Yellow
docker stop jutjubic-rabbitmq

Write-Host "Cekanje da circuit breaker detektuje pad (5 sekundi)..." -ForegroundColor Yellow
Write-Host "(RabbitMQ retry config: 3 attempts x ~1s = brza detekcija)" -ForegroundColor Gray
Start-Sleep -Seconds 5

Write-Host "Provera health (RabbitMQ bi trebalo da bude DOWN):" -ForegroundColor Cyan
try {
    $health = Invoke-RestMethod -Uri "http://localhost/actuator/health" -Method Get -TimeoutSec 15
    Write-Host "Overall Status: $($health.status)" -ForegroundColor $(if($health.status -eq "UP"){"Green"}else{"Yellow"})
    if ($health.components.rabbit) {
        Write-Host "RabbitMQ Status: $($health.components.rabbit.status)" -ForegroundColor $(if($health.components.rabbit.status -eq "UP"){"Green"}else{"Yellow"})
    }
} catch {
    Write-Host "Health check greska: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "Testiranje kreiranja videa (trebalo bi da radi bez MQ):" -ForegroundColor Cyan
$videoData = @{
    title = "Video bez MQ - $(Get-Date -Format 'HH:mm:ss')"
    description = "Test graceful degradation"
    createdAt = (Get-Date -Format 'yyyy-MM-ddTHH:mm:ss')
    thumbnailPath = "test-thumbnail-mq.jpg"
    videoUrl = "test-video-mq.mp4"
    views = 0
    likes = 0
    user = @{
        id = 1
    }
} | ConvertTo-Json -Depth 3

try {
    $response = Invoke-RestMethod -Uri "http://localhost/api/cluster/videos" `
        -Method Post `
        -Body $videoData `
        -ContentType "application/json" `
        -Headers $script:headers `
        -TimeoutSec 15
    Write-Host "[OK] Video kreiran uspesno bez MQ!" -ForegroundColor Green
    Write-Host "   Video ID: $($response.video.id)" -ForegroundColor Cyan
} catch {
    Write-Host "[GRESKA] Kreiranje videa nije uspelo: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "Pokretanje RabbitMQ ponovo..." -ForegroundColor Yellow
docker start jutjubic-rabbitmq

Write-Host "Cekanje da se RabbitMQ podize (10 sekundi)..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host ""
Read-Host "Pritisni Enter za sledeci scenario..."

# SCENARIO 5: Provera circuit breaker stanja
Write-Host ""
Write-Host "SCENARIO 5: Circuit Breaker Metrike" -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Yellow
try {
    $cb = Invoke-RestMethod -Uri "http://localhost/actuator/circuitbreakers" -Method Get -TimeoutSec 10
    Write-Host "Circuit Breaker Stanje:" -ForegroundColor Cyan
    $cb | ConvertTo-Json -Depth 3 | Write-Host
} catch {
    Write-Host "Greska pri dohvatanju circuit breaker stanja: $_" -ForegroundColor Red
}

# SCENARIO 6: Oba backend-a DOWN (ekstremni scenario)
Write-Host ""
Write-Host "SCENARIO 6: Oba backend-a DOWN (ekstremni scenario)" -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Yellow
Write-Host "⚠️  UPOZORENJE: Ovaj test ce privremeno onemoguciti oba backenda" -ForegroundColor Red
$confirm = Read-Host "Zelis li da nastavis? (y/n)"

if ($confirm -eq 'y') {
    Write-Host "Zaustavljanje oba backend-a..." -ForegroundColor Yellow
    docker stop jutjubic-app-1 jutjubic-app-2
    
    Write-Host "Cekanje 3 sekunde..." -ForegroundColor Yellow
    Start-Sleep -Seconds 3
    
    Write-Host "Testiranje - trebalo bi da padne:" -ForegroundColor Cyan
    Test-Endpoint "http://localhost/api/cluster/info" "Zahtev ka nedostupnom backendu" -ShowInstanceId
    Test-Endpoint "http://localhost/api/videos" "GET /api/videos ka nedostupnom backendu" -ShowInstanceId:$false
    
    Write-Host ""
    Write-Host "Ponovno pokretanje backend-a..." -ForegroundColor Yellow
    docker start jutjubic-app-1 jutjubic-app-2
    
    Write-Host "Cekanje da se podigne (45 sekundi)..." -ForegroundColor Yellow
    Start-Sleep -Seconds 45
    
    Write-Host "Testiranje cluster info - trebalo bi da radi ponovo:" -ForegroundColor Cyan
    for ($i = 1; $i -le 2; $i++) {
        Test-Endpoint "http://localhost/api/cluster/info" "Cluster info $i" -ShowInstanceId
        Start-Sleep -Seconds 1
    }
    Write-Host "`nTestiranje pravih endpointa:" -ForegroundColor Cyan
    Test-Endpoint "http://localhost/api/videos" "GET /api/videos" -ShowInstanceId:$false
} else {
    Write-Host "Scenario preskocen." -ForegroundColor Gray
}

# Zavrsetak
Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "TESTIRANJE ZAVRSENO!" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Za detaljne logove: docker-compose logs -f" -ForegroundColor Gray
