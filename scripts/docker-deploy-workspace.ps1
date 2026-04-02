# 使用 E:\workspace\docker\docker-compose.base.yml 中的基础栈 + 本仓库业务镜像部署。
# 前置：Docker Desktop 已启动。

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$WorkspaceDocker = if ($env:WORKSPACE_DOCKER) { $env:WORKSPACE_DOCKER } else { "E:/workspace/docker" }
$BaseCompose = Join-Path $WorkspaceDocker "docker-compose.base.yml"
$InfraRocket = Join-Path $RepoRoot "docker/infra-rocketmq.yml"
$EnvFile = Join-Path $RepoRoot "docker/base-images.env"

Set-Location $RepoRoot

if (-not (Test-Path $BaseCompose)) {
    Write-Error "未找到基础编排: $BaseCompose"
}

Write-Host "==> 启动 workspace 基础栈 (docker-compose.base.yml)..." -ForegroundColor Cyan
docker compose -f $BaseCompose up -d

Write-Host "==> 启动 RocketMQ (与 workspace docker-compose.yml 一致)..." -ForegroundColor Cyan
docker compose -f $InfraRocket up -d

Write-Host "==> Maven 打包..." -ForegroundColor Cyan
mvn -q -DskipTests package
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "==> 构建并启动业务后端 + 前端..." -ForegroundColor Cyan
docker compose --env-file $EnvFile -f docker-compose.backend.yml -f docker-compose.frontend.yml up -d --build

Write-Host "完成。前端: http://localhost:5173  网关: http://localhost:9100" -ForegroundColor Green
