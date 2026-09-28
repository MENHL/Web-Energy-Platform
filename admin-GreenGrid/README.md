# greengrid-admin-server

GreenGrid 新能源企业运营数据可视化平台 —— 后端服务（Spring Boot 3 + MyBatis-Plus + MySQL 8 + JWT）。

```
前端仓库地址：
Gitee：https://gitee.com/rainbow-under-the-sunshine/New-Energy-Platform
Githup：https://github.com/MENHL/New-Energy-Platform
```

## 技术栈与版本

| 组件 | 版本 | 说明 |
|---|---|---|
| Java | 17 | Microsoft OpenJDK 17（ms-17） |
| Spring Boot | 3.5.16 | 单体应用 |
| MyBatis-Plus | 3.5.17 | 含分页插件依赖 `mybatis-plus-jsqlparser` |
| MySQL | 8.x | 库名 `greengrid_admin`，utf8mb4 |
| JWT | jjwt 0.12.6 | 拦截器实现，未引入 Spring Security 全家桶 |
| 密码 | spring-security-crypto | 仅用 BCryptPasswordEncoder |
| 文档 | springdoc 3.1.1 | Swagger UI |


## 目录结构

```
backend/
├── pom.xml
└── src/main/
    ├── java/com/greengrid/admin/
    │   ├── GreenGridAdminApplication.java        # 启动类（@MapperScan）
    │   ├── common/                               # 全局基础设施
    │   │   ├── result/   Result / PageResult / ErrorCode
    │   │   ├── exception/BizException / GlobalExceptionHandler
    │   │   ├── query/PageQuery
    │   │   └── util/IpUtil
    │   ├── config/                               # 配置类
    │   │   ├── WebConfig / CorsConfig / MybatisPlusConfig
    │   │   ├── MetaObjectHandlerImpl / JacksonConfig
    │   │   ├── PasswordConfig / OpenApiConfig
    │   ├── security/                             # 鉴权
    │   │   ├── JwtProperties / JwtUtil
    │   │   ├── AuthInterceptor / RoleInterceptor
    │   │   ├── UserContext / LoginUser / RequireRole
    │   ├── modules/
    │   │   ├── auth/                             # 登录 / 当前用户 / 退出
    │   │   │   ├── AuthController / AuthService
    │   │   │   ├── dto/LoginRequest
    │   │   │   └── vo/LoginVO / UserInfoVO
    │   │   └── system/                           # 用户与日志
    │   │       ├── entity/SysUser / SysOperationLog
    │   │       ├── mapper/SysUserMapper / SysOperationLogMapper
    │   │       └── service/OperationLogService
    └── resources/
        ├── application.yml                       # 公共配置
        ├── application-dev.yml                   # 本地（root/root）
        ├── application-prod.yml                  # 生产（全部走环境变量）
        └── db/
            ├── schema.sql                        # 建表（幂等重建）
            └── data.sql                          # 初始化数据
```

## 一、建库 SQL

```sql
CREATE DATABASE IF NOT EXISTS greengrid_admin
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

> 建表与初始化数据由 `schema.sql` / `data.sql` 在启动时自动执行（`spring.sql.init.mode=always`），无需手工执行。如果你想手动执行，直接把这俩文件在 Navicat 里跑一遍即可。

## 二、启动命令

前置：本机已装 Java 17 + Maven 3.8+，MySQL 已启动且 root 密码为 root。

```bash
cd backend
mvn clean spring-boot:run -Dspring-boot.run.profiles=dev
```

或打包后运行：

```bash
mvn clean package -DskipTests
java -jar target/greengrid-admin-server.jar --spring.profiles.active=dev
```

启动成功标志：日志出现 `Tomcat started on port 8080 (http) with context path '/api'`。

Swagger 地址：<http://localhost:8080/api/swagger-ui.html>（右上角 Authorize 填 `Bearer <token>` 后可点测接口）

健康检查：<http://localhost:8080/api/actuator/health>

## 三、curl 登录测试

```bash
# 1) 登录，拿到 token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"123456\",\"remember\":true}"

# 期望返回：
# {"code":0,"message":"success","data":{"token":"eyJhbGciOiJIUzI1NiJ9...","user":{...}}}

# 2) 带 token 获取当前用户（把 <TOKEN> 换成上一步返回的 token）
curl http://localhost:8080/api/auth/me -H "Authorization: Bearer <TOKEN>"

# 3) 退出登录
curl -X POST http://localhost:8080/api/auth/logout -H "Authorization: Bearer <TOKEN>"

# 4) 错误密码
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"wrong\"}"
# 期望：{"code":1,"message":"账号或密码错误"}

# 5) 不带 token 访问受保护接口
curl -i http://localhost:8080/api/auth/me
# 期望：HTTP 401 + {"code":401,"message":"登录已过期，请重新登录"}
```

演示账号（密码统一 `123456`）：`admin` / `ops` / `warehouse` / `project` / `guest`。

## 四、常见报错与解决

| 报错 | 原因 | 解决 |
|---|---|---|
| `Communications link failure` | MySQL 没启动或端口不对 | 启动 MySQL；确认 3306 未被占用 |
| `Public Key Retrieval is not allowed` | MySQL 8 默认 caching_sha2_password | 连接串已带 `allowPublicKeyRetrieval=true`，检查是否被覆盖 |
| `Access denied for user 'root'@'localhost'` | 密码不是 root | 改 `application-dev.yml` 的 `spring.datasource.password` |
| `Unknown database 'greengrid_admin'` | 没建库 | 执行上面的建库 SQL |
| `Unsupported character encoding 'utf8mb4'` | JDBC 连接串把 characterEncoding 写成了 utf8mb4 | 改为 `characterEncoding=UTF-8`（本项目已用正确值） |
| 启动报 `ClassNotFoundException: net.sf.jsqlparser...` | 缺少分页插件依赖 | 确认 `mybatis-plus-jsqlparser` 依赖存在（3.5.9+ 必加） |
| `Could not resolve placeholder 'GG_JWT_SECRET'` | 误用 prod profile 但没设环境变量 | 本地用 `dev`，生产才用 `prod` 并设置环境变量 |
| `Port 8080 was already in use` | 端口占用 | `netstat -ano | findstr :8080` 找到 PID 结束，或改 `server.port` |
| Swagger 打开 404 | context-path 是 /api | 地址必须是 `/api/swagger-ui.html` |
| 所有接口 401（含登录） | 拦截器把登录也拦了 | 确认 `WebConfig` 里排除了 `/auth/login` |
| 登录成功但中文显示 `???` | 编码不统一 | 确认库/表/连接串都 utf8mb4，且 `server.servlet.encoding.force=true` |

## 五、设计约定（与前端契约一致）

- 成功：`{ code: 0, data: ... }`；业务失败：`{ code: 1, message: "..." }`
- 分页：请求 `page`(1 起)/`pageSize`/`keyword`；响应 `{ list, total }`
- 鉴权头：`Authorization: Bearer <token>`
- 时间格式：`LocalDateTime` 序列化为 `yyyy-MM-dd HH:mm:ss`，`LocalDate` 为 `yyyy-MM-dd`
- 状态枚举存中文字符串，与前端 1:1 对齐
