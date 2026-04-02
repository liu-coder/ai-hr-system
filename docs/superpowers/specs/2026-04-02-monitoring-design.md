# 监控方案（Prometheus + Grafana）设计

**目标：** 新增独立监控栈（Prometheus + Grafana），采集核心服务、考勤/薪酬、MySQL（exporter）与 Milvus 指标。

**架构：** 独立 `docker-compose.monitoring.yml` 挂到现有 `app-network`。Prometheus 抓取 Spring Boot Actuator 指标与 exporter 指标。Grafana 展示延迟分位与服务健康。`/actuator/prometheus` 不启用鉴权，仅供内网抓取。

**技术栈：** Docker Compose、Prometheus、Grafana、mysqld_exporter、Spring Boot Actuator。

---

## 1. 服务与端口

- Prometheus：`9090`
- Grafana：`3000`（admin/admin）
- MySQL exporter：`9104`

Milvus 指标从现有 `milvus-standalone` 的 `/metrics` 直接抓取。

## 2. 抓取目标

Prometheus 抓取目标：

- `ai-hr-gateway` `/actuator/prometheus`
- `ai-hr-ai-core` `/actuator/prometheus`
- `ai-hr-attendance` `/actuator/prometheus`
- `ai-hr-salary` `/actuator/prometheus`
- `mysqld_exporter`（MySQL 指标）
- `milvus-standalone` `/metrics`

## 3. 数据目录

监控数据统一落盘到 `E:\workspace\docker\data`：

- Prometheus 数据：`E:\workspace\docker\data\prometheus`
- Grafana 数据：`E:\workspace\docker\data\grafana`

## 4. 网络接入

监控编排文件挂到已有外部网络：

- `app-network`（external）

Prometheus 可直接访问网络内服务名（例如 `ai-hr-ai-core`、`standalone`、`mysql`）。

## 5. 指标暴露（Actuator）

以下服务启用 `/actuator/prometheus` 且不鉴权：

- `ai-hr-gateway`
- `ai-hr-ai-core`
- `ai-hr-attendance`
- `ai-hr-salary`

同时开启请求直方图与分位数统计：

- `management.metrics.distribution.percentiles-histogram.http.server.requests=true`
- `management.metrics.distribution.percentiles.http.server.requests=0.5,0.9,0.95,0.99`

## 6. MySQL Exporter

创建独立监控账号：

- 用户名：`prometheus`
- 密码：`prometheus`
- 权限：只读（用于指标采集）

Exporter 连接 `mysql` 容器并在 `9104` 暴露指标。

## 7. Grafana 初始看板

最低看板建议：

- gateway/ai-core/attendance/salary 的 p95/p99 延迟
- 按 `uri`、`method` 聚合的请求量
- MySQL 基础健康（连接数、查询量）
- Milvus 基础健康（若 metrics 可用）

## 8. 验证步骤

1. 启动监控栈：`docker compose -f docker-compose.monitoring.yml up -d`
2. 打开 Prometheus：`http://localhost:9090`，确认 targets 全部 `UP`
3. 打开 Grafana：`http://localhost:3000`，账号 `admin/admin`
4. 发送几次 `/v1/ai/chat`，确认延迟指标出现

---

## 风险与应对

- **服务指标未暴露：** 确认各服务配置已开启 `/actuator/prometheus`。
- **MySQL exporter 鉴权失败：** 校验监控账号与连接串。
- **Milvus metrics 不可用：** 允许目标为 `DOWN`，后续再补齐。
