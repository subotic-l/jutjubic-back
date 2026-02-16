# start-with-consumer.ps1
# Helper script to start all services including the upload-event-consumer

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Starting Jutjubic with Consumer" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$jutjubicPath = "C:\Users\Aleksa\Documents\Faks\Semestar7\ISA\projekat\jutjubic-back\jutjubic"

# Check if docker-compose.yml exists
if (-not (Test-Path "$jutjubicPath\docker-compose.yml")) {
    Write-Host "Error: docker-compose.yml not found in $jutjubicPath" -ForegroundColor Red
    exit 1
}

# Navigate to jutjubic directory
Set-Location $jutjubicPath

Write-Host "Building and starting all services..." -ForegroundColor Yellow
Write-Host ""

try {
    # Start all services
    docker-compose up -d --build
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "Waiting for database to be ready..." -ForegroundColor Yellow
        Start-Sleep -Seconds 5
        
        Write-Host "Clearing existing data from database..." -ForegroundColor Yellow
        docker exec jutjubic-postgres psql -U postgres -d jutjubic -c "TRUNCATE TABLE video_tags, user_liked_videos, watch_party_participants, video_comments, watch_parties, video_posts, users RESTART IDENTITY CASCADE;" 2>$null
        
        Write-Host "Importing new data from data.sql..." -ForegroundColor Yellow
        docker cp src/main/resources/data.sql jutjubic-postgres:/tmp/data.sql
        docker exec jutjubic-postgres psql -U postgres -d jutjubic -f /tmp/data.sql
        
        Write-Host "Database populated successfully!" -ForegroundColor Green
    }
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "All services started successfully!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Services running:" -ForegroundColor Cyan
        Write-Host "  - PostgreSQL:           http://localhost:5432" -ForegroundColor White
        Write-Host "  - RabbitMQ:             http://localhost:5672 (AMQP)" -ForegroundColor White
        Write-Host "  - RabbitMQ Management:  http://localhost:15672 (guest/guest)" -ForegroundColor White
        Write-Host "  - Upload Consumer:      http://localhost:8083" -ForegroundColor White
        Write-Host "  - App Instance 1:       http://localhost:8081" -ForegroundColor White
        Write-Host "  - App Instance 2:       http://localhost:8082" -ForegroundColor White
        Write-Host "  - Nginx Load Balancer:  http://localhost" -ForegroundColor White
        Write-Host "  - Prometheus:           http://localhost:9090" -ForegroundColor White
        Write-Host "  - Grafana:              http://localhost:3000 (admin/admin)" -ForegroundColor White
        Write-Host ""
        Write-Host "To view benchmark statistics, run:" -ForegroundColor Yellow
        Write-Host "  .\get-benchmark-stats.ps1" -ForegroundColor Gray
        Write-Host ""
        Write-Host "To view logs:" -ForegroundColor Yellow
        Write-Host "  docker logs jutjubic-upload-consumer" -ForegroundColor Gray
        Write-Host ""
        Write-Host "To stop all services:" -ForegroundColor Yellow
        Write-Host "  docker-compose down" -ForegroundColor Gray
        Write-Host ""
    }
    else {
        Write-Host ""
        Write-Host "Error starting services!" -ForegroundColor Red
        Write-Host "  Check docker-compose logs for details" -ForegroundColor Yellow
        exit 1
    }
}
catch {
    Write-Host ""
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
