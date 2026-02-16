# Test DB Metrics Script
# Pokreće slow queries i prati metrike u realnom vremenu

$connections = 30
$duration = 15

Write-Host "`n=== TESTIRANJE DB KONEKCIJA ===" -ForegroundColor Cyan
Write-Host "Parametri: $connections konekcija, svaka traje ${duration}s`n" -ForegroundColor Yellow

# Pokreni test
Write-Host "Pokrećem slow queries test..." -ForegroundColor Green
Invoke-RestMethod -Uri "http://localhost/api/test/slow-queries?connections=$connections&queryDuration=$duration" -Method Get | Out-Null

Write-Host "`nPraćenje metrika (osvežava se svakih 2s za $duration sekundi):" -ForegroundColor Yellow
Write-Host "Format: [Vreme] Active: X, Idle: Y, Max: Z`n" -ForegroundColor Gray

# Prati metrike
$endTime = (Get-Date).AddSeconds($duration + 3)
while ((Get-Date) -lt $endTime) {
    try {
        $metrics = Invoke-RestMethod -Uri "http://localhost:8081/actuator/prometheus" -ErrorAction Stop
        $lines = $metrics -split "`n"
        
        $activeLine = $lines | Where-Object { $_ -match 'hikaricp_connections_active.*HikariPool.*} (\d+\.?\d*)' }
        $idleLine = $lines | Where-Object { $_ -match 'hikaricp_connections_idle.*HikariPool.*} (\d+\.?\d*)' }
        $maxLine = $lines | Where-Object { $_ -match 'hikaricp_connections_max.*HikariPool.*} (\d+\.?\d*)' }
        
        if ($activeLine -match '} (\d+\.?\d*)') { $active = [int][double]$Matches[1] } else { $active = "?" }
        if ($idleLine -match '} (\d+\.?\d*)') { $idle = [int][double]$Matches[1] } else { $idle = "?" }
        if ($maxLine -match '} (\d+\.?\d*)') { $max = [int][double]$Matches[1] } else { $max = "?" }
        
        $timestamp = Get-Date -Format "HH:mm:ss"
        
        $color = if ($active -gt 0) { "Green" } else { "Gray" }
        Write-Host "[$timestamp] Active: " -NoNewline
        Write-Host "$active" -NoNewline -ForegroundColor $color
        Write-Host ", Idle: $idle, Max: $max"
        
    } catch {
        Write-Host "Greška pri čitanju metrika: $_" -ForegroundColor Red
    }
    
    Start-Sleep -Seconds 2
}

Write-Host "`n=== TEST ZAVRŠEN ===" -ForegroundColor Cyan
Write-Host "Otvori Grafana da vidiš grafik: http://localhost:3000/d/jutjubic-monitoring" -ForegroundColor Green
