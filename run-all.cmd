@echo off
cd /d "%~dp0"
echo Ensure executable jars: run "mvn -q -DskipTests package" once if needed.
echo.

echo Starting ai-hr-auth (9101)...
start "ai-hr-auth" java -jar ai-hr-auth\target\ai-hr-auth-0.1.0-SNAPSHOT.jar
timeout /t 3 /nobreak >nul

echo Starting ai-hr-attendance (9010)...
start "ai-hr-attendance" java -jar ai-hr-attendance\target\ai-hr-attendance-0.1.0-SNAPSHOT.jar
timeout /t 2 /nobreak >nul

echo Starting ai-hr-salary (9020)...
start "ai-hr-salary" java -jar ai-hr-salary\target\ai-hr-salary-0.1.0-SNAPSHOT.jar
timeout /t 2 /nobreak >nul

echo Starting ai-hr-ai-core (9030)...
echo Stop existing ai-hr-ai-core on 9030 if any...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :9030 ^| findstr LISTENING') do taskkill /F /PID %%a
timeout /t 1 /nobreak >nul
start "ai-hr-ai-core" mvn -pl ai-hr-ai-core -DskipTests spring-boot:run
timeout /t 2 /nobreak >nul

echo Starting ai-hr-gateway (9100)...
start "ai-hr-gateway" java -jar ai-hr-gateway\target\ai-hr-gateway-0.1.0-SNAPSHOT.jar

echo.
echo All 5 services started in new windows. Wait ~30s then:
echo   Gateway: http://localhost:9100
echo   POST http://localhost:9100/v1/auth/token  (gateway forwards to auth:9101)
echo   Body: {"tenantId":"t-demo","username":"admin","password":"admin"}
echo.
echo Close this window when done. Service windows will keep running.
