# performance-test.ps1
# Skripta za pokretanje izolovanog performance testiranja JSON vs Protobuf

param(
    [string]$ConsumerUrl = "http://localhost:8083",
    [string]$OutputFile = "performance-test-results.txt",
    [switch]$NoFile
)

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "  JSON vs Protobuf - Isolated Performance Benchmark" -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Test konfiguracija:" -ForegroundColor Yellow
Write-Host "  - Broj poruka: 50 razlicitih UploadEvent poruka" -ForegroundColor Gray
Write-Host "  - Iteracije po poruci: 100x serijalizacija + 100x deserijalizacija" -ForegroundColor Gray
Write-Host "  - Ukupno merenja: 5000 serijalizacija + 5000 deserijalizacija (po formatu)" -ForegroundColor Gray
Write-Host "  - JIT Warmup: Da (20 iteracija pre merenja)" -ForegroundColor Gray
Write-Host "  - I/O overhead: ISKLJUCEN (bez RabbitMQ, mreze, diska)" -ForegroundColor Gray
Write-Host ""

$benchmarkUrl = "$ConsumerUrl/benchmark-results/isolated-benchmark"

try {
    Write-Host "Pokretanje izolovanog benchmark testa..." -ForegroundColor Yellow
    Write-Host "URL: $benchmarkUrl" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Molimo sacekajte, test je u toku (moze trajati 10-30 sekundi)..." -ForegroundColor Yellow
    Write-Host ""
    
    # Pokreni POST request ka benchmark endpointu
    $response = Invoke-RestMethod -Uri $benchmarkUrl -Method Post -ErrorAction Stop
    
    Write-Host "Benchmark uspesno zavrsen!" -ForegroundColor Green
    Write-Host ""
    Write-Host "=================================================================" -ForegroundColor Cyan
    Write-Host "  REZULTATI IZOLOVANOG BENCHMARK TESTA" -ForegroundColor Cyan
    Write-Host "=================================================================" -ForegroundColor Cyan
    Write-Host ""
    
    # JSON statistika
    Write-Host "--- JSON ---" -ForegroundColor Magenta
    Write-Host "  Prosecno vreme serijalizacije:   $($response.json.avgSerializeNs) ns" -ForegroundColor White
    Write-Host "                                     ($([Math]::Round($response.json.avgSerializeNs / 1000, 2)) us)" -ForegroundColor Gray
    Write-Host "  Prosecno vreme deserijalizacije:  $($response.json.avgDeserializeNs) ns" -ForegroundColor White
    Write-Host "                                     ($([Math]::Round($response.json.avgDeserializeNs / 1000, 2)) us)" -ForegroundColor Gray
    Write-Host "  Prosecna velicina poruke:          $([Math]::Round($response.json.avgSizeBytes, 2)) bytes" -ForegroundColor White
    Write-Host ""
    
    # Protobuf statistika
    Write-Host "--- Protobuf ---" -ForegroundColor Magenta
    Write-Host "  Prosecno vreme serijalizacije:   $($response.protobuf.avgSerializeNs) ns" -ForegroundColor White
    Write-Host "                                     ($([Math]::Round($response.protobuf.avgSerializeNs / 1000, 2)) us)" -ForegroundColor Gray
    Write-Host "  Prosecno vreme deserijalizacije:  $($response.protobuf.avgDeserializeNs) ns" -ForegroundColor White
    Write-Host "                                     ($([Math]::Round($response.protobuf.avgDeserializeNs / 1000, 2)) us)" -ForegroundColor Gray
    Write-Host "  Prosecna velicina poruke:          $([Math]::Round($response.protobuf.avgSizeBytes, 2)) bytes" -ForegroundColor White
    Write-Host ""
    
    # Poredjenje
    Write-Host "--- Poredjenje (Protobuf vs JSON) ---" -ForegroundColor Green
    Write-Host "  Serijalizacija:" -ForegroundColor Yellow
    Write-Host "     Protobuf je $($response.comparison.serializeSpeedRatio)x brzi" -ForegroundColor White
    Write-Host "     Ubrzanje: $($response.comparison.serializeSpeedupPercent)%" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  Deserijalizacija:" -ForegroundColor Yellow
    Write-Host "     Protobuf je $($response.comparison.deserializeSpeedRatio)x brzi" -ForegroundColor White
    Write-Host "     Ubrzanje: $($response.comparison.deserializeSpeedupPercent)%" -ForegroundColor Cyan
    Write-Host ""
    Write-Host " Velicina poruke:" -ForegroundColor Yellow
    Write-Host "     Protobuf je $($response.comparison.sizeRatio)x manji" -ForegroundColor White
    Write-Host "     Smanjenje: $($response.comparison.compressionPercent)%" -ForegroundColor Cyan
    Write-Host ""
    
    # Dodatne metrike
    $totalJsonTimeUs = [Math]::Round(($response.json.avgSerializeNs + $response.json.avgDeserializeNs) / 1000, 2)
    $totalProtobufTimeUs = [Math]::Round(($response.protobuf.avgSerializeNs + $response.protobuf.avgDeserializeNs) / 1000, 2)
    
    Write-Host "--- Dodatne metrike ---" -ForegroundColor DarkCyan
    Write-Host "  JSON: Ukupno vreme (ser + deser):     $totalJsonTimeUs us" -ForegroundColor Gray
    Write-Host "  Protobuf: Ukupno vreme (ser + deser): $totalProtobufTimeUs us" -ForegroundColor Gray
    Write-Host ""
    
    Write-Host "=================================================================" -ForegroundColor Cyan
    Write-Host ""
    
    # Sacuvaj rezultate u fajl ako nije iskljuceno
    if (-not $NoFile) {
        $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
        
        $jsonSerUs = [Math]::Round($response.json.avgSerializeNs / 1000, 2)
        $jsonDeserUs = [Math]::Round($response.json.avgDeserializeNs / 1000, 2)
        $protoSerUs = [Math]::Round($response.protobuf.avgSerializeNs / 1000, 2)
        $protoDeserUs = [Math]::Round($response.protobuf.avgDeserializeNs / 1000, 2)
        $jsonSize = [Math]::Round($response.json.avgSizeBytes, 2)
        $protoSize = [Math]::Round($response.protobuf.avgSizeBytes, 2)
        
        $reportLines = @(
            "",
            "================================================================="
            "JSON vs Protobuf - Performance Benchmark Results"
            "================================================================="
            "Generated: $timestamp"
            ""
            "Test Configuration:"
            "  Number of messages: 50 diverse UploadEvent messages"
            "  Iterations per message: 100x serialize + 100x deserialize"
            "  Total measurements: 5000 serializations + 5000 deserializations per format"
            "  JIT Warmup: Yes"
            "  I/O overhead: EXCLUDED"
            ""
            "================================================================="
            "JSON Statistics"
            "================================================================="
            "Average Serialize Time:    $($response.json.avgSerializeNs) ns ($jsonSerUs us)"
            "Average Deserialize Time:  $($response.json.avgDeserializeNs) ns ($jsonDeserUs us)"
            "Average Message Size:      $jsonSize bytes"
            "Total Time:                $totalJsonTimeUs us"
            ""
            "================================================================="
            "Protobuf Statistics"
            "================================================================="
            "Average Serialize Time:    $($response.protobuf.avgSerializeNs) ns ($protoSerUs us)"
            "Average Deserialize Time:  $($response.protobuf.avgDeserializeNs) ns ($protoDeserUs us)"
            "Average Message Size:      $protoSize bytes"
            "Total Time:                $totalProtobufTimeUs us"
            ""
            "================================================================="
            "Performance Comparison"
            "================================================================="
            "Serialization:     Protobuf is $($response.comparison.serializeSpeedRatio)x faster ($($response.comparison.serializeSpeedupPercent)%)"
            "Deserialization:   Protobuf is $($response.comparison.deserializeSpeedRatio)x faster ($($response.comparison.deserializeSpeedupPercent)%)"
            "Message Size:      Protobuf is $($response.comparison.sizeRatio)x smaller ($($response.comparison.compressionPercent)%)"
            ""
            "================================================================="
            ""
        )
        
        $reportLines | Out-File -FilePath $OutputFile -Encoding UTF8
        Write-Host "Detaljni izvestaj sacuvan u: $OutputFile" -ForegroundColor Green
        Write-Host ""
    }
    
} catch {
    Write-Host ""
    Write-Host "Greska prilikom izvrsavanja benchmark testa!" -ForegroundColor Red
    Write-Host "Poruka greske: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Moguca resenja:" -ForegroundColor Yellow
    Write-Host "  1. Proverite da li je upload-event-consumer aplikacija pokrenuta" -ForegroundColor Gray
    Write-Host "  2. Proverite URL: $benchmarkUrl" -ForegroundColor Gray
    Write-Host "  3. Proverite logove aplikacije za detalje" -ForegroundColor Gray
    Write-Host ""
    exit 1
}
