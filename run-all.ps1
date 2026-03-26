# AI HR System - Start all services (PowerShell)
# Prerequisites: JDK 17+, local MySQL running (root/root), optional Milvus on 19530

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot

function Start-Service {
    param([string]$Name, [string]$JarPath, [int]$Port)
    $fullPath = Join-Path $ProjectRoot $JarPath
    if (-not (Test-Path $fullPath)) {
        Write-Host "ERROR: Jar not found: $fullPath. Run: mvn -q -DskipTests package" -ForegroundColor Red
        exit 1
    }
    Write-Host "Starting $Name on port $Port ..." -ForegroundColor Cyan
    Start-Process -FilePath "java" -ArgumentList "-jar", $fullPath -WorkingDirectory $ProjectRoot -WindowStyle Normal
    Start-Sleep -Seconds 2
}

# 1. Auth (port 9101 to avoid conflicts with local Docker)
Start-Service -Name "ai-hr-auth" -JarPath "ai-hr-auth\target\ai-hr-auth-0.1.0-SNAPSHOT.jar" -Port 9101
# 2. Attendance
Start-Service -Name "ai-hr-attendance" -JarPath "ai-hr-attendance\target\ai-hr-attendance-0.1.0-SNAPSHOT.jar" -Port 9010
# 3. Salary
Start-Service -Name "ai-hr-salary" -JarPath "ai-hr-salary\target\ai-hr-salary-0.1.0-SNAPSHOT.jar" -Port 9020
# 4. AI-Core
Start-Service -Name "ai-hr-ai-core" -JarPath "ai-hr-ai-core\target\ai-hr-ai-core-0.1.0-SNAPSHOT.jar" -Port 9030
# 5. Gateway
Start-Service -Name "ai-hr-gateway" -JarPath "ai-hr-gateway\target\ai-hr-gateway-0.1.0-SNAPSHOT.jar" -Port 9100

Write-Host ""
Write-Host "All 5 processes started in new windows. Wait ~30s for Spring Boot to finish startup." -ForegroundColor Green
Write-Host "Gateway: http://localhost:9100" -ForegroundColor Yellow
Write-Host "Auth:    http://localhost:9101" -ForegroundColor Yellow
Write-Host "Then: POST http://localhost:9100/v1/auth/token with body: {\"tenantId\":\"t-demo\",\"username\":\"admin\",\"password\":\"<your_password>\"}" -ForegroundColor Yellow
