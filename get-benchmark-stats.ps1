# get-benchmark-stats.ps1
# Script to fetch and display benchmark statistics from upload-event-consumer

param(
    [string]$Url = "http://localhost:8083/benchmark-results",
    [string]$OutputFile = "benchmark-stats.csv",
    [switch]$JsonOutput,
    [switch]$NoFile
)

Write-Host "===============================================" -ForegroundColor Cyan
Write-Host "  Upload Event Consumer - Benchmark Statistics" -ForegroundColor Cyan
Write-Host "===============================================" -ForegroundColor Cyan
Write-Host ""

try {
    # Fetch benchmark results from the API
    Write-Host "Fetching benchmark results from: $Url" -ForegroundColor Yellow
    $response = Invoke-RestMethod -Uri $Url -Method Get -ErrorAction Stop
    
    Write-Host "Successfully retrieved benchmark data!" -ForegroundColor Green
    Write-Host ""
    
    # Display results in console
    Write-Host "--- JSON Deserialization Stats ---" -ForegroundColor Magenta
    Write-Host "  Average Deserialize Time: $($response.json.avgDeserializeNs) ns ($([Math]::Round($response.json.avgDeserializeNs / 1000, 2)) us)"
    Write-Host "  Average Message Size:     $($response.json.avgSizeBytes) bytes"
    Write-Host ""
    
    Write-Host "--- Protobuf Deserialization Stats ---" -ForegroundColor Magenta
    Write-Host "  Average Deserialize Time: $($response.protobuf.avgDeserializeNs) ns ($([Math]::Round($response.protobuf.avgDeserializeNs / 1000, 2)) us)"
    Write-Host "  Average Message Size:     $($response.protobuf.avgSizeBytes) bytes"
    Write-Host ""
    
    # Display comparison stats from backend
    if ($response.comparison) {
        Write-Host "--- Performance Comparison ---" -ForegroundColor Green
        Write-Host "  Protobuf is $($response.comparison.speedRatio)x faster in deserialization"
        Write-Host "  Protobuf messages are $($response.comparison.sizeRatio)x smaller in size"
        Write-Host "  Speed improvement: $($response.comparison.speedupPercent)%"
        Write-Host "  Size reduction: $($response.comparison.compressionPercent)%"
        Write-Host ""
    }
    
    # Save to file if not disabled
    if (-not $NoFile) {
        $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
        
        if ($JsonOutput) {
            # Save as JSON
            $jsonFile = $OutputFile -replace '\.csv$', '.json'
            $outputData = @{
                timestamp = $timestamp
                json = $response.json
                protobuf = $response.protobuf
                comparison = $response.comparison
            } | ConvertTo-Json -Depth 3
            
            $outputData | Out-File -FilePath $jsonFile -Encoding UTF8
            Write-Host "Results saved to: $jsonFile" -ForegroundColor Green
        }
        else {
            # Save as CSV
            $csvContent = @()
            $csvContent += "Timestamp,Format,AvgDeserializeNs,AvgDeserializeMicroseconds,AvgSizeBytes"
            $csvContent += "$timestamp,JSON,$($response.json.avgDeserializeNs),$([Math]::Round($response.json.avgDeserializeNs / 1000, 2)),$($response.json.avgSizeBytes)"
            $csvContent += "$timestamp,Protobuf,$($response.protobuf.avgDeserializeNs),$([Math]::Round($response.protobuf.avgDeserializeNs / 1000, 2)),$($response.protobuf.avgSizeBytes)"
            
            $csvContent | Out-File -FilePath $OutputFile -Encoding UTF8
            Write-Host "Results saved to: $OutputFile" -ForegroundColor Green
        }
    }
    
    # Also create a detailed text report
    if (-not $NoFile) {
        $txtFile = $OutputFile -replace '\.(csv|json)$', '.txt'
        $reportContent = @"
===============================================
Upload Event Consumer - Benchmark Report
===============================================
Generated: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

JSON Deserialization Statistics
--------------------------------
Average Deserialize Time: $($response.json.avgDeserializeNs) nanoseconds
                         ($([Math]::Round($response.json.avgDeserializeNs / 1000, 2)) microseconds)
                         ($([Math]::Round($response.json.avgDeserializeNs / 1000000, 4)) milliseconds)
Average Message Size:     $($response.json.avgSizeBytes) bytes

Protobuf Deserialization Statistics
------------------------------------
Average Deserialize Time: $($response.protobuf.avgDeserializeNs) nanoseconds
                         ($([Math]::Round($response.protobuf.avgDeserializeNs / 1000, 2)) microseconds)
                         ($([Math]::Round($response.protobuf.avgDeserializeNs / 1000000, 4)) milliseconds)
Average Message Size:     $($response.protobuf.avgSizeBytes) bytes

Performance Comparison
----------------------
"@
        
        if ($response.comparison -and $response.comparison.speedRatio -gt 0) {
            $reportContent += @"
Deserialization Speed: Protobuf is $($response.comparison.speedRatio)x faster ($($response.comparison.speedupPercent)% improvement)
Message Size:          Protobuf is $($response.comparison.sizeRatio)x smaller ($($response.comparison.compressionPercent)% reduction)

Conclusion
----------
Protobuf provides significant performance benefits:
- Faster deserialization by $($response.comparison.speedupPercent)%
- Smaller message size by $($response.comparison.compressionPercent)%
"@
        }
        else {
            $reportContent += "Insufficient data for comparison (no messages processed yet)"
        }
        
        $reportContent | Out-File -FilePath $txtFile -Encoding UTF8
        Write-Host "Detailed report saved to: $txtFile" -ForegroundColor Green
    }
    
    Write-Host ""
    Write-Host "===============================================" -ForegroundColor Cyan
    
}
catch {
    Write-Host ""
    Write-Host "Error fetching benchmark data!" -ForegroundColor Red
    Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "  Make sure the upload-event-consumer is running and accessible at: $Url" -ForegroundColor Yellow
    Write-Host "  You can start it with: docker-compose up -d upload-event-consumer" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}
