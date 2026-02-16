# Skripta za testiranje DB konekcija - simuldacija opterećenja
# Koristi nove test endpoint-e

Write-Host "`n=== DATABASE LOAD TEST SCRIPT ===" -ForegroundColor Cyan
Write-Host "Ova skripta simulira opterećenje baze podataka`n" -ForegroundColor White

$baseUrl = "http://localhost:8080"

# Funkcija za slanje zahteva
function Invoke-LoadTest {
    param(
        [string]$TestType,
        [hashtable]$Params
    )
    
    $queryString = ($Params.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" }) -join "&"
    $url = "$baseUrl/api/test/$TestType?$queryString"
    
    Write-Host "🚀 Pozivam: $url" -ForegroundColor Yellow
    
    try {
        $response = Invoke-RestMethod -Uri $url -Method Get
        Write-Host "✅ Odgovor:" -ForegroundColor Green
        $response | ConvertTo-Json -Depth 3 | Write-Host
    } catch {
        Write-Host "❌ Greška: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Prikaz dostupnih testova
Write-Host "`n📋 DOSTUPNI TESTOVI:`n" -ForegroundColor Cyan

Write-Host "1. JEDNOKRATNO OPTEREĆENJE (db-load)" -ForegroundColor Yellow
Write-Host "   - Pravi određen broj paralelnih query-ja" -ForegroundColor Gray
Write-Host "   - Svaki query 'radi' određeno vreme (sleep)" -ForegroundColor Gray
Write-Host "   - Parametri: threads (1-50), delayMs (0-5000)`n" -ForegroundColor Gray

Write-Host "2. KONTINUIRANO OPTEREĆENJE (continuous-load)" -ForegroundColor Yellow
Write-Host "   - Neprekidno šalje query-je u pozadini" -ForegroundColor Gray
Write-Host "   - Traje određeno vreme" -ForegroundColor Gray
Write-Host "   - Parametri: duration (1-300s), threads (1-20)`n" -ForegroundColor Gray

Write-Host "3. PARALELNI HTTP ZAHTEVI (iz PowerShell-a)" -ForegroundColor Yellow
Write-Host "   - Ova skripta paralelno zove endpoint više puta`n" -ForegroundColor Gray

# Meni
Write-Host "`n🎯 IZABERITE TEST:" -ForegroundColor Cyan
Write-Host "1 - Lagano opterećenje (5 threadova, 1s delay)" -ForegroundColor White
Write-Host "2 - Srednje opterećenje (10 threadova, 1s delay)" -ForegroundColor White
Write-Host "3 - Jako opterećenje (20 threadova, 2s delay)" -ForegroundColor White
Write-Host "4 - Kontinuirano 30s (5 threadova)" -ForegroundColor White
Write-Host "5 - Kontinuirano 60s (10 threadova)" -ForegroundColor White
Write-Host "6 - Paralelni HTTP zahtevi (10 zahteva odjednom)" -ForegroundColor White
Write-Host "7 - Custom test" -ForegroundColor White
Write-Host "0 - Izlaz" -ForegroundColor White

$choice = Read-Host "`nVaš izbor"

switch ($choice) {
    "1" {
        Write-Host "`n🔥 LAGANO OPTEREĆENJE" -ForegroundColor Cyan
        Invoke-LoadTest -TestType "db-load" -Params @{threads=5; delayMs=1000}
    }
    "2" {
        Write-Host "`n🔥 SREDNJE OPTEREĆENJE" -ForegroundColor Cyan
        Invoke-LoadTest -TestType "db-load" -Params @{threads=10; delayMs=1000}
    }
    "3" {
        Write-Host "`n🔥 JAKO OPTEREĆENJE" -ForegroundColor Cyan
        Invoke-LoadTest -TestType "db-load" -Params @{threads=20; delayMs=2000}
    }
    "4" {
        Write-Host "`n🔄 KONTINUIRANO OPTEREĆENJE - 30s" -ForegroundColor Cyan
        Invoke-LoadTest -TestType "continuous-load" -Params @{duration=30; threads=5}
        Write-Host "`n💡 Test radi u pozadini. Otvorite Grafana dashboard da vidite konekcije:" -ForegroundColor Yellow
        Write-Host "   http://localhost:3000/d/jutjubic-monitoring" -ForegroundColor Green
    }
    "5" {
        Write-Host "`n🔄 KONTINUIRANO OPTEREĆENJE - 60s" -ForegroundColor Cyan
        Invoke-LoadTest -TestType "continuous-load" -Params @{duration=60; threads=10}
        Write-Host "`n💡 Test radi u pozadini. Otvorite Grafana dashboard da vidite konekcije:" -ForegroundColor Yellow
        Write-Host "   http://localhost:3000/d/jutjubic-monitoring" -ForegroundColor Green
    }
    "6" {
        Write-Host "`n🚀 PARALELNI HTTP ZAHTEVI (10x)" -ForegroundColor Cyan
        Write-Host "Šaljem 10 paralelnih zahteva ka /api/health..." -ForegroundColor Yellow
        
        $jobs = 1..10 | ForEach-Object {
            Start-Job -ScriptBlock {
                param($url)
                Invoke-RestMethod -Uri $url -Method Get
            } -ArgumentList "$baseUrl/api/health"
        }
        
        Write-Host "✅ Započeto - čekam rezultate..." -ForegroundColor Green
        $jobs | Wait-Job | Receive-Job
        $jobs | Remove-Job
        
        Write-Host "`n✅ Svi zahtevi završeni!" -ForegroundColor Green
    }
    "7" {
        Write-Host "`n⚙️ CUSTOM TEST" -ForegroundColor Cyan
        $threads = Read-Host "Broj threadova (1-50)"
        $delayMs = Read-Host "Delay po query-ju u ms (0-5000)"
        
        Invoke-LoadTest -TestType "db-load" -Params @{threads=$threads; delayMs=$delayMs}
    }
    "0" {
        Write-Host "`nDo viđenja! 👋" -ForegroundColor Cyan
    }
    default {
        Write-Host "`n❌ Nevažeći izbor!" -ForegroundColor Red
    }
}

Write-Host "`n💡 SAVETI:" -ForegroundColor Cyan
Write-Host "- Otvorite Grafana dashboard: http://localhost:3000/d/jutjubic-monitoring" -ForegroundColor White
Write-Host "- Pogledajte panel 'Broj aktivnih i idle konekcija ka bazi'" -ForegroundColor White
Write-Host "- Aktivne konekcije će narasti dok query-i traju!" -ForegroundColor White
Write-Host ""
