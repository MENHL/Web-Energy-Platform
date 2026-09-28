# GreenGrid 联调与部署指南

## 一、本地联调：从 Mock 切到真实后端

### 1. 启动后端（本机）

```bash
cd backend
mvn clean spring-boot:run -Dspring-boot.run.profiles=dev
# 成功标志：Tomcat started on port 8080 with context path '/api'
# 首次启动会自动建表 + 初始化数据（dev 环境 spring.sql.init.mode=always）
```

### 2. 前端切到真实后端

已配置好，无需手改：

- `.env.development` 里 `VITE_USE_MOCK=false` → `request.ts` 不再挂载 Mock 适配器，所有请求走真实网络。
- `vite.config.ts` 里 `server.proxy` 把 `/api` 转发到 `http://localhost:8080`，**未加 rewrite**（后端 context-path 已是 `/api`，加了反而 404）。

```bash
npm run dev   # http://localhost:5180
```

### 3. 想切回纯 Mock 演示

把 `.env.development` 里的 `VITE_USE_MOCK` 改成 `true`（或删掉该文件）后重启 `npm run dev`。

### 4. 验证

1. 登录页用 `admin/123456` 登录成功。
2. 打开浏览器 DevTools Network，请求 URL 是 `/api/...` 且返回 `{code:0,...}`。
3. 后端控制台能看到 `Preparing: SELECT ...` 之类的 SQL 日志。

---

## 二、Docker 部署（后端 + MySQL）

```bash
# 1. 准备环境变量（生产务必改密码和密钥）
cp .env.example .env
# 编辑 .env，改 MYSQL_ROOT_PASSWORD 和 GG_JWT_SECRET

# 2. 一键构建并启动（mysql + backend）
docker compose up -d --build

# 3. 查看状态与日志
docker compose ps
docker compose logs -f backend
```

- MySQL 首次启动会自动执行 `db/schema.sql` + `db/data.sql`（挂载到 `/docker-entrypoint-initdb.d/`，按 `01-`/`02-` 前缀顺序执行，建表 + 5 个演示账号 + 基础数据）。
- 后端激活 `prod` profile：数据库连接、密码、JWT 密钥全部来自容器环境变量（见 `docker-compose.yml` 的 `environment`），绝不硬编码。

### 验证

```bash
curl http://localhost:8080/api/actuator/health
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

> 注意：`application-prod.yml` 已关闭 Swagger（`springdoc`），生产不暴露接口文档。

---

## 三、Nginx 部署（前端静态资源 + /api 反向代理）

### 1. 构建前端

```bash
npm run build          # 产出 dist/，base 默认 /
```

### 2. 部署

`deploy/nginx.conf` 已配置：`/` 走 SPA History 回退到 index.html，`/api/` 反向代理到 `backend:8080`（不 rewrite）。

- **Nginx 与后端同处 compose 网络**：后端主机名写 `backend`。
- **Nginx 在宿主机**：把 `nginx.conf` 里的 `backend` 改成 `localhost`。

把 `dist/` 拷贝到 Nginx 容器的 `/usr/share/nginx/html`，`nginx.conf` 挂载到 `/etc/nginx/nginx.conf`。

```bash
# 单机手动部署示例（后端已在本机 8080 运行）
npm run build
sudo cp -r dist/* /usr/share/nginx/html/
sudo cp deploy/nginx.conf /etc/nginx/nginx.conf
sudo nginx -t && sudo nginx -s reload
```

---

## 四、启动 / 排查命令速查

| 操作 | 命令 |
|---|---|
| 本地起后端 | `cd backend && mvn clean spring-boot:run -Dspring-boot.run.profiles=dev` |
| 本地起前端 | `npm run dev` |
| 打包后端 jar | `cd backend && mvn clean package -DskipTests` |
| Docker 启动 | `docker compose up -d --build` |
| Docker 停止 | `docker compose down` |
| Docker 重置数据（删库重来） | `docker compose down -v && docker compose up -d --build` |
| 后端日志 | `docker compose logs -f backend` |
| 数据库日志 | `docker compose logs -f mysql` |
| 健康检查 | `curl http://localhost:8080/api/actuator/health` |
| 进 MySQL 容器 | `docker exec -it greengrid-mysql mysql -uroot -p greengrid_admin` |
| 前端构建 | `npm run build` |

### 常见报错

| 现象 | 排查 |
|---|---|
| 前端请求 404，URL 是 `/api/api/...` | 代理或 Nginx 加了 rewrite，去掉；后端 context-path 已是 `/api` |
| 前端报 `login` 页面反复跳 | 后端 401 返回码没对齐，确认拦截器返回 HTTP 401 + `{code:401}` |
| 后端启动报 `Connection refused` 连 MySQL | 后端容器连 `mysql:3306`（compose 内网），本机跑连 `localhost:3306` |
| MySQL 表不存在 | 检查 `schema.sql`/`data.sql` 是否首次执行成功；`docker compose down -v` 后重来 |
| 容器里 `GG_JWT_SECRET` 未解析 | 确认 `.env` 存在且变量名一致；`docker compose config` 查看生效值 |
| 端口占用 | `netstat -ano \| findstr :8080`（Windows）找 PID 结束 |

---

## 五、200 QPS 压测方案

目标：核心接口（如项目列表）在 200 QPS 下 P95 < 200ms，无错误。

### 方案 A：wrk（推荐，Linux/macOS）

```bash
# 1. 拿 token（用 jq 提取；无 jq 可手动复制）
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' | jq -r .data.token)

# 2. 压测列表接口：4 线程 100 连接，压 30 秒，输出延迟分布
wrk -t4 -c100 -d30s --latency \
  -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/projects?page=1&pageSize=10"

# 3. 压测聚合接口（逻辑最重，重点看 P95）
wrk -t4 -c100 -d30s --latency \
  -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/dashboard/overview"
```

**看结果**：`Requests/sec`（应 ≥ 200）、`Latency Distribution` 里的 `99%`（应 < 200ms）、`Non-2xx or 3xx responses`（应为 0）。

如果 QPS 上不去或延迟超标：
- 调 `-c` 连接数 / `-t` 线程数；
- 观察后端 `docker compose logs -f backend` 是否报连接池耗尽；
- 检查 MySQL 慢查询：`docker exec greengrid-mysql mysql -uroot -p -e "SHOW PROCESSLIST;"`。

### 方案 B：JMeter

1. 新建线程组：线程数 50、Ramp-Up 5s、循环次数 200（总请求 10000）。
2. 加「HTTP Header Manager」：`Authorization: Bearer <token>`。
3. 加「HTTP Request」：`GET http://localhost:8080/api/projects?page=1&pageSize=10`。
4. 加「Summary Report / Aggregate Report」监听器，看吞吐与 99% 响应时间。
5. 命令行非 GUI 跑：`jmeter -n -t plan.jmx -l result.jtl`，再用报告分析。

> 200 QPS 对当前架构（单体 + 单库 + HikariCP 10 连接）是轻松量级。若 P95 超标，先看是不是 MySQL 慢查询或连接池不够（调 `maximum-pool-size`），**不要**一上来加 Redis/ES（见 `docs/backend/phase1-plan.md` 第 10 节触发条件）。
