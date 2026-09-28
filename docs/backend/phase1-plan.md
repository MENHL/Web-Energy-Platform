# GreenGrid 后端（Spring Boot 3）第一阶段方案

> 状态：**待你确认，未写任何 Java 代码**
> 前端仓库：`D:\New_xiangmu\New-Energy-Platform`（package.json name = `greengrid-admin`，Vue 3.5 + Vite 6 + TS 5.7 + Ant Design Vue 4）
> 本方案基于我实际读取的前端源码，不是基于你的口头描述，因此文中所有字段名都能在代码里找到出处。

---

## 0. 契约核对结论（读代码后发现的关键事实）

这 5 条直接决定后端能不能一次连通，请优先看：

| # | 发现 | 证据 | 影响 |
|---|---|---|---|
| 1 | **前端没有任何 `VITE_USE_MOCK` 开关**，Mock 是通过 Axios 自定义 adapter 强制拦截的 | `src/utils/request.ts:38-54`：`instance.defaults.adapter` 先 `matchMockHandler()`，命中就返回，未命中才走 `fallbackInstance` | 你只把 `VITE_USE_MOCK=false` 写进 `.env` **完全没用**。必须先改造 `request.ts`，否则后端一条请求都收不到（这是本次最大的坑，见第 9 节坑 1） |
| 2 | **响应拦截器没有 401 处理**，401 只会弹一句英文错误 | `src/utils/request.ts:31-34` 错误分支只有 `message.error(err?.message)`，没有清 token、没有跳登录 | 你说的"401 跳登录"当前不存在。需要补 5 行前端代码（见第 7 节），否则 token 过期后用户会卡在页面反复报错 |
| 3 | **报表状态枚举前后端不一致** | `src/types/index.ts:73` 只声明 `'已生成' \| '生成中'`，但 `src/api/report.ts:20` 取消时发送的是 `'已取消'` | 后端必须接受第三种状态 `已取消`，同时建议把 TS 类型补上 |
| 4 | **Mock 的字段过滤会跳过 falsy 值** | `src/mock/index.ts:41-44`：`if (!value \|\| [...].includes(key)) continue` | 用户管理页筛"已禁用"时 `status=0` 在 Mock 下筛不出结果。后端要**正确**支持 `status=0`（不要照抄 Mock 的 bug） |
| 5 | **前端列表页的筛选参数已固定** | `ProjectList.vue:145`（keyword/status/region）、`DeviceList.vue:97`（keyword/type/status）、`MaterialList.vue:153`（keyword/category/status）、`ReportList.vue:117`（keyword/type）、`UserManage.vue:103`（keyword/role/status）、`LogList.vue:75`（keyword/module/action） | 后端 Query 对象按这些字段名逐个对齐，**不要自创 `searchKey`、`state` 之类的名字** |

另外两条好消息（前端已经做对的地方，后端照做即可）：

- 前端 `DELETE` 请求把参数放在 query 里（`http.delete(url, params)`），但本项目所有删除都是**路径传 id**（`/projects/1`），所以后端统一用 `@PathVariable` 即可，不用处理 query。
- 前端 `router` 已做角色守卫（`router/index.ts:139-143`），但**接口层没有做**。后端必须自己校验角色，否则 guest 用户直接 curl 就能删数据。

---

## 1. 需要你补充 / 确认的问题清单

每条我都给了**默认建议**，你可以直接回复"全部按建议"，或只改你关心的几条。

| # | 问题 | 我的默认建议 |
|---|---|---|
| Q1 | 后端代码放哪里？ | 放在 `D:\New_xiangmu\New-Energy-Platform\backend\`（和前端同一个仓库，前后端一起提交，联调最省事）。如果你想让后端独立仓库，告诉我仓库名 |
| Q2 | Maven 构建坐标 | `groupId=com.greengrid`、`artifactId=greengrid-admin-server`、`basePackage=com.greengrid.admin` |
| Q3 | 看板数据（dashboard / board / analytics）要**真算**还是**返种子数据**？ | **真算**：KPI、趋势、Top5、库存结构全部从 `project/device/material/stock_order` 聚合出来（工作量只多一个 Service）；只有"今日发电量""设备综合利用率"这类没有数据源的前端装饰指标走固定值 + 注释说明 |
| Q4 | 出入库单只出现在"物料详情追溯"里，没有独立列表页。要不要顺手加 `GET /api/orders`？ | **加**（前端暂不调用，但 Swagger 里可调试，也给后续做仓储页留口子）。只加读接口，不加写接口 |
| Q5 | 设备详情的 `metrics`（12 个点的负载/温度）后端怎么来？ | 一期**不建监控表**，用"设备 ID + 日期"做种子的确定性伪随机生成（保证同一台设备每次刷新曲线一致，符合前端 Mock 的观感）。二期有真实采集再落 `device_metric` 表 |
| Q6 | `GET /api/auth/me`、`POST /api/auth/logout` 要不要加？ | **加**。现在前端只在勾选"记住登录"时才持久化用户信息，不勾选时刷新页面 `userStore.userInfo` 会变 null 但也还能用（token 在）。加 `/auth/me` 后可以修掉这个隐患 |
| Q7 | 用户"重置密码"前端目前是假的（`UserManage.vue:226-232` 只弹 message）。要不要做真接口？ | **加** `PUT /api/system/users/{id}/password`，重置为 `123456`。前端改动很小，一期可以只做后端 + Swagger 验证 |
| Q8 | 删除项目时有关联设备/单据，怎么处理？ | **拒绝删除**（返回 code 1："该项目下存在 N 台设备，请先解绑"），比级联删除安全，也符合 `ProjectList.vue:275` 的二次确认文案 |
| Q9 | 密码算法 / 演示账号 | BCrypt（`spring-security-crypto`，**不引入完整 Spring Security**）。5 个演示账号由启动时的 `DataSeeder` 初始化，密码统一 `123456` 的 BCrypt 值，避免在 SQL 里写死哈希 |
| Q10 | 建表方式 | 开发期用 `schema.sql` + `data.sql`（`spring.sql.init.mode=always`，配合 `DROP TABLE IF EXISTS` 幂等重建）；上生产前再换 Flyway。**不建议现在就用 Flyway**，对新手太重 |
| Q11 | 时间字段格式 | 日期类字段（`startDate/endDate/installDate`）用 `LocalDate` → 输出 `2026-03-01`；时间类（`updatedAt/lastLoginAt/createdAt/time`）用 `LocalDateTime` + Jackson 全局格式化成 `2026-09-14 09:12:00`，和 Mock 的观感一致 |
| Q12 | 状态字段存中文还是数字？ | **存中文**（`进行中/已暂停/已完成`、`在线/离线/故障/维护中`、`正常/低库存/缺货`、`已生成/生成中/已取消`），与前端枚举 1:1，`VARCHAR(16)` + 常量类约束。理由：本项目状态是业务语义而非计算值，存中文能省掉全部枚举转换代码，数据量 10 万以内无性能问题 |
| Q13 | Redis / 缓存 | **一期不用**。什么时候需要，见第 10 节 |
| Q14 | 端口与上下文路径 | 后端 `8080`，`server.servlet.context-path=/api`，Controller 写 `/auth/login`（与你给的一致） |

---

## 2. 前端接口 ↔ 后端接口对齐表

通用约定（**必须严格遵守，否则前端页面全白**）：

```
成功：HTTP 200 + { "code": 0, "data": <业务数据> }
业务失败：HTTP 200 + { "code": 1, "message": "错误原因" }
未登录/过期：HTTP 401 + { "code": 401, "message": "登录已过期，请重新登录" }
无权限：HTTP 403 + { "code": 403, "message": "暂无该操作权限" }
参数校验失败：HTTP 400 + { "code": 400, "message": "首个字段错误提示" }
服务异常：HTTP 500 + { "code": 500, "message": "服务繁忙，请稍后重试" }

分页响应固定：{ "code": 0, "data": { "list": [...], "total": 100 } }
分页请求固定：page（从 1 开始）、pageSize（默认 10）、keyword
```

### 2.1 认证

| # | 方法 | 路径（业务侧） | 完整 URL | 请求参数 | 响应 data | 失败 message |
|---|---|---|---|---|---|---|
| 1 | POST | `/auth/login` | `/api/auth/login` | Body：`username`(必填)、`password`(必填)、`remember`(bool，可选) | `{ token, user: UserInfo }` | `账号或密码错误`、`该账号已被禁用，请联系管理员`、`用户名和密码不能为空` |
| 2 | GET | `/auth/me` | `/api/auth/me` | Header：`Authorization` | `UserInfo` | `登录已过期，请重新登录` |
| 3 | POST | `/auth/logout` | `/api/auth/logout` | Header | `null` | — |

`UserInfo` 字段（前端 `types/index.ts:7-16` 逐字段对齐，**一个都不能少**）：`id, username, name, role, roleName, dept, status(1/0 数字), lastLoginAt`
`role` 取值：`super | ops | warehouse | project | guest`；`roleName`：`超级管理员 | 运营管理员 | 仓储管理员 | 项目管理员 | 普通用户`

> 演示账号（密码均 `123456`）：`admin` 张工 / `ops` 李敏 / `warehouse` 王强 / `project` 赵磊 / `guest` 访客。

### 2.2 总览 / 看板 / 分析

| # | 方法 | 路径 | 请求参数 | 响应 data 结构（字段名逐一取自 `mock/index.ts`） |
|---|---|---|---|---|
| 4 | GET | `/dashboard/overview` | 无 | `{ kpis: KpiItem[6], trend: { dates[], inbound[], outbound[] }, deviceStatus: { total, items: [{name, value}] }, topProjects: [{rank, name, amount}], warnings: [{id, name, spec, stock, safetyStock, level}], activities: [{id, content, time}] }` |
| 5 | GET | `/board/summary` | 无 | `{ kpis: KpiItem[4], hourly: { hours[], power[], orders[] }, warehouses: [{name, usage}], deviceTypes: [{name, usage}], todos: [{id, content, level}] }` |
| 6 | GET | `/analytics/summary` | `range`(7/30/90，默认 30)、`projectId`(可选)、`category`(可选) | `{ inboundRate, outboundRate, inboundTrend: { dates[], inbound[], outbound[] }, outboundTrend: {同上}, stockStructure: { total, items: [{name, value}] }, materialRank: [{rank, name, amount}] }` |

`KpiItem` = `{ label, value: number, unit, remark, trend: 1 | 0 | -1 }`（`api/dashboard.ts:8-14`）

注意三点：
- `kpis` 的 `value` 是**数字**（`128.6`、`91.8`），不是字符串，后端别拼成 `"128.6万kWh"`。
- `trend.dates` 格式是 `MM-DD`（`09-08`），不是 `YYYY-MM-DD`；`hourly.hours` 是 `HH:00`。
- Mock 里 `inboundTrend` 和 `outboundTrend` 是**同一个对象**（`mock/index.ts:136-137` 的偷懒），后端请返回两条独立曲线，前端渲染逻辑不受影响。

### 2.3 项目管理

| # | 方法 | 路径 | 请求参数 | 响应 data |
|---|---|---|---|---|
| 7 | GET | `/projects` | Query：`page, pageSize, keyword, status, region`（`ProjectList.vue:145`） | `{ list: ProjectItem[], total }` |
| 8 | GET | `/projects/{id}` | Path：`id` | `ProjectItem & { devices: DeviceItem[] }` |
| 9 | POST | `/projects` | Body：`{ name, projectCode, region, manager, status, progress, startDate, endDate }` | `null` |
| 10 | PUT | `/projects/{id}` | Body：同 POST（**部分字段更新**） | `null` |
| 11 | DELETE | `/projects/{id}` | Path：`id` | `null`，失败 `项目不存在` / `该项目下存在设备，无法删除` |

`ProjectItem`：`id, projectCode, name, region, manager, status('进行中'|'已暂停'|'已完成'), progress(0-100 整数), startDate, endDate, updatedAt`
`region` 取值固定 7 个：`华东 / 华北 / 华南 / 华中 / 西北 / 西南 / 东北`（`ProjectList.vue:153`）

⚠️ `keyword` 的语义：Mock 是"整条记录 JSON 转小写包含匹配"，项目页提示语是"项目名称 / 编号"。后端请实现为 **`name LIKE %kw% OR project_code LIKE %kw%`**（更专业，且效果一致）。

### 2.4 设备管理

| # | 方法 | 路径 | 请求参数 | 响应 data |
|---|---|---|---|---|
| 12 | GET | `/devices` | Query：`page, pageSize, keyword, type, status` | `{ list: DeviceItem[], total }` |
| 13 | GET | `/devices/{id}` | Path | `DeviceItem & { metrics: { times[12], load[12], temp[12] } }` |
| 14 | PUT | `/devices/{id}` | Body：`{ status }`（前端只传状态，`api/device.ts:26`） | `null` |
| 15 | DELETE | `/devices/{id}` | Path | `null`，失败 `设备不存在` |

`DeviceItem`：`id, deviceCode, name, type, projectName, status('在线'|'离线'|'故障'|'维护中'), installDate, lastUpdateAt`
`type` 取值 5 个：`逆变器 / 风电机组 / 储能系统 / 光伏阵列 / 输配电`（`DeviceList.vue:100`）
`metrics.times` 格式 `00:00, 02:00 ... 22:00`（12 点，每 2 小时）

⚠️ 前端设备列表页的"状态统计卡片"是**基于当前页数据统计**的（`DeviceList.vue:116-124`），不是全量。这不是 bug，但后端若能额外返回全量统计会更准——**一期先不动前端**。

### 2.5 物料管理

| # | 方法 | 路径 | 请求参数 | 响应 data |
|---|---|---|---|---|
| 16 | GET | `/materials` | Query：`page, pageSize, keyword, category, status` | `{ list: MaterialItem[], total }` |
| 17 | GET | `/materials/{id}` | Path | `MaterialItem & { inboundRecords: OrderItem[], outboundRecords: OrderItem[] }` |
| 18 | POST | `/materials` | Body：`{ materialCode, name, category, spec, unit, stock, safetyStock }` | `null` |
| 19 | PUT | `/materials/{id}` | Body：同上（部分更新） | `null` |
| 20 | DELETE | `/materials/{id}` | Path | `null`，失败 `物料不存在` |

`MaterialItem`：`id, materialCode, name, category, spec, unit, stock, safetyStock, status('正常'|'低库存'|'缺货')`
`category` 取值 4 个：`电芯电池 / 光伏组件 / 电气元件 / 结构件辅材`（取自 Mock 数据）
`OrderItem`：`id, orderNo, materialName, quantity, target, operator, time`

⚠️ **`status` 是派生字段，由后端在写入时统一重算**：`stock == 0 → 缺货`；`stock < safetyStock → 低库存`；否则 `正常`。前端调 PUT 时不会传 `status`，后端必须自己算（Mock 就是这么做的，见 `mock/index.ts:290`）。这是新手最容易漏的一条。

### 2.6 数据报表

| # | 方法 | 路径 | 请求参数 | 响应 data |
|---|---|---|---|---|
| 21 | GET | `/reports` | Query：`page, pageSize, keyword, type` | `{ list: ReportItem[], total }` |
| 22 | POST | `/reports` | Body：`{ type, name }` | `null`（后端补 `status='生成中'`、`createdAt=now`、`creator=当前登录用户`） |
| 23 | PUT | `/reports/{id}` | Body：`{ status }`，取值 `已取消` | `null`，失败 `报表不存在` |
| 24 | DELETE | `/reports/{id}` | Path | `null`，失败 `报表不存在` |

`ReportItem`：`id, type, name, status, createdAt, creator`
`type` 取值 5 个：`运营汇总 / 库存分析 / 设备分析 / 物料分析 / 报表导出`（前 4 个取自 Mock，第 5 个为兜底）
`status` 放开为 `'生成中' | '已生成' | '已取消'`（**后端必须接受"已取消"**，见第 0 节发现 3）

### 2.7 系统管理

| # | 方法 | 路径 | 请求参数 | 响应 data |
|---|---|---|---|---|
| 25 | GET | `/system/users` | Query：`page, pageSize, keyword, role, status` | `{ list: UserInfo[], total }` |
| 26 | POST | `/system/users` | Body：`{ username, name, role, roleName, dept }`（**无密码字段**） | `null`，后端密码默认 `123456` |
| 27 | PUT | `/system/users/{id}` | Body：整体对象**或**仅 `{ status: 0 | 1 }`（`UserManage.vue:220`） | `null` |
| 28 | DELETE | `/system/users/{id}` | Path | `null`，失败 `用户不存在` |
| 29 | PUT | `/system/users/{id}/password` | Path + Body：`{}` | `null`（重置为 `123456`，Q7 待确认） |
| 30 | GET | `/system/logs` | Query：`page, pageSize, keyword, module, action` | `{ list: LogItem[], total }` |

`LogItem`：`id, operator, module, action('新增'|'更新'|'删除'|'维护'|'登录'), content, ip, createdAt`
`module` 取值：`项目管理 / 设备管理 / 物料管理 / 仓储管理 / 系统管理`（取自 Mock 日志数据）

⚠️ 三条硬性要求：
1. 用户接口返回**绝对不能带 password 字段**（含 BCrypt 哈希）。
2. `PUT /system/users/{id}` 必须支持**只传 `status`** 的部分更新，且 `status` 是**数字 0/1**。
3. 删除自己 / 删除最后一个 super 账号要被拒绝（返回 code 1）。

### 2.8 建议新增（Q4 待确认）

| # | 方法 | 路径 | 参数 | 响应 |
|---|---|---|---|---|
| 31 | GET | `/orders` | `page, pageSize, keyword, orderType(INBOUND/OUTBOUND), materialId, startDate, endDate` | `{ list: OrderVO[], total }` |
| 32 | GET | `/health`（不带 /api 前缀也可） | 无 | `{ status: 'UP', db: 'UP', time }` |

### 2.9 错误码表

| HTTP | code | 触发场景 | 前端表现 |
|---|---|---|---|
| 200 | 0 | 成功 | 拦截器解包 `data` |
| 200 | 1 | 业务失败（账号密码错、资源不存在、关联数据阻止删除） | `message.error(后端 message)` |
| 400 | 400 | `@Valid` 参数校验失败、id 非数字、日期格式非法 | 弹后端 message（需配合第 7 节前端补丁） |
| 401 | 401 | 无 token / token 非法 / token 过期 | 清 token 跳 `/login` |
| 403 | 403 | 角色不匹配（如 guest 调 `/system/users`） | 弹"暂无该操作权限" |
| 404 | 404 | 路径写错（正常不该出现，出现即前后端路径不匹配） | 提示接口不存在 |
| 500 | 500 | 未捕获异常 | 弹"服务繁忙，请稍后重试"，后端日志记完整堆栈 |

**接口权限矩阵**（与 `router/index.ts:33-108` 的 `meta.roles` 完全一致）：

| 接口组 | super | ops | warehouse | project | guest |
|---|---|---|---|---|---|
| `/auth/*` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/dashboard/*`、`/board/*` | ✅ | ✅ | ❌ | ❌ | ✅ |
| `/projects/*` | ✅ | ✅ | ❌ | ✅ | ❌ |
| `/devices/*` | ✅ | ✅ | ❌ | ✅ | ❌ |
| `/materials/*`、`/analytics/*` | ✅ | ✅ | ✅ | ❌ | ❌ |
| `/reports/*` | ✅ | ✅ | ❌ | ❌ | ❌ |
| `/system/users/*`、`/system/logs` | ✅ | ❌ | ❌ | ❌ | ❌ |

读写可分权（可选增强）：`guest` 只允许 GET，任何写操作返回 403。

---

## 3. 数据库设计

数据库：`greengrid_admin`，字符集 `utf8mb4` / 排序规则 `utf8mb4_0900_ai_ci`，引擎 InnoDB。
命名：表名小写蛇形，业务表不加前缀，系统表加 `sys_`；**统一带审计字段** `created_at` / `updated_at` / `deleted`。
主键统一 `BIGINT UNSIGNED AUTO_INCREMENT`，MyBatis-Plus 用 `IdType.AUTO`。

### 3.1 `sys_user` 用户表

| 字段 | 类型 | 允许空 | 默认 | 说明 |
|---|---|---|---|---|
| id | bigint unsigned | 否 | auto | 主键 |
| username | varchar(50) | 否 | — | 登录账号（工号） |
| password | varchar(100) | 否 | — | BCrypt 哈希（60 字符），**永不出参** |
| name | varchar(50) | 否 | — | 姓名 |
| role | varchar(20) | 否 | — | super/ops/warehouse/project/guest |
| role_name | varchar(20) | 否 | — | 中文角色名（前端直接渲染） |
| dept | varchar(50) | 否 | — | 所属部门 |
| status | tinyint | 否 | 1 | 1 启用 / 0 禁用 |
| last_login_at | datetime | 是 | null | 最近登录时间，登录成功时更新 |
| created_at | datetime | 否 | now | — |
| updated_at | datetime | 否 | now on update | — |
| deleted | tinyint | 否 | 0 | 1 已删除（逻辑删除） |

索引：`uk_username(username, deleted)`（**复合唯一，不能用 `uk_username(username)`**，否则逻辑删除后无法重新创建同名账号——见坑 8）、`idx_role(role)`、`idx_status(status)`

初始化数据（5 条，密码均为 `123456` 的 BCrypt 值，由启动器写入，不写死在 SQL）：

| username | name | role | role_name | dept |
|---|---|---|---|---|
| admin | 张工 | super | 超级管理员 | 信息中心 |
| ops | 李敏 | ops | 运营管理员 | 运营部 |
| warehouse | 王强 | warehouse | 仓储管理员 | 仓储部 |
| project | 赵磊 | project | 项目管理员 | 工程部 |
| guest | 访客 | guest | 普通用户 | 外部 |

### 3.2 `sys_operation_log` 操作日志表

| 字段 | 类型 | 允许空 | 默认 | 说明 |
|---|---|---|---|---|
| id | bigint unsigned | 否 | auto | 主键 |
| user_id | bigint unsigned | 是 | null | 操作人 id（系统操作为 null） |
| operator | varchar(50) | 否 | — | 操作人姓名或"系统" |
| module | varchar(30) | 否 | — | 项目管理/设备管理/物料管理/仓储管理/系统管理 |
| action | varchar(16) | 否 | — | 新增/更新/删除/维护/登录 |
| content | varchar(500) | 否 | — | 操作描述（自然语言） |
| ip | varchar(45) | 是 | null | 客户端 IP（兼容 IPv6，**不要用 varchar(15)**） |
| created_at | datetime | 否 | now | 操作时间 |
| deleted | tinyint | 否 | 0 | 逻辑删除 |

索引：`idx_created_at(created_at DESC)`、`idx_module_action(module, action)`、`idx_operator(operator)`
分区/归档：单表 10 万以内无需处理；超过 50 万再按月归档（第 10 节）。

写入方式：**不用 AOP 全自动**（一期过于黑盒）。在 Service 层显式调用 `OperationLogService.record(module, action, content)`，登录成功也记一条。一期覆盖：登录、项目增删改、设备维护/删除、物料增删改、用户增删改。初始 8 条日志照抄 `mock/data.ts:124-133`。

### 3.3 `project` 项目表

| 字段 | 类型 | 允许空 | 默认 | 说明 |
|---|---|---|---|---|
| id | bigint unsigned | 否 | auto | 主键 |
| project_code | varchar(32) | 否 | — | 项目编号，如 PRJ-2026-011 |
| name | varchar(100) | 否 | — | 项目名称 |
| region | varchar(16) | 否 | — | 华东/华北/华南/华中/西北/西南/东北 |
| manager | varchar(50) | 否 | — | 负责人 |
| status | varchar(16) | 否 | 进行中 | 进行中/已暂停/已完成 |
| progress | tinyint unsigned | 否 | 0 | 0-100 |
| start_date | date | 是 | null | 计划开始 |
| end_date | date | 是 | null | 计划结束 |
| created_at | datetime | 否 | now | — |
| updated_at | datetime | 否 | now on update | 前端"更新时间"直接用这个 |
| deleted | tinyint | 否 | 0 | 逻辑删除 |

索引：`uk_project_code(project_code, deleted)`、`idx_status(status)`、`idx_region(region)`、`idx_updated_at(updated_at DESC)`

初始化数据：**12 条，照抄 `mock/data.ts:38-62`**（华东储能基地一期 PRJ-2026-011 等），保证前端首屏与设计稿完全一致。

### 3.4 `device` 设备表

| 字段 | 类型 | 允许空 | 默认 | 说明 |
|---|---|---|---|---|
| id | bigint unsigned | 否 | auto | 主键 |
| device_code | varchar(32) | 否 | — | 设备编号，如 DEV-0231 |
| name | varchar(100) | 否 | — | 设备名称 |
| type | varchar(20) | 否 | — | 逆变器/风电机组/储能系统/光伏阵列/输配电 |
| project_id | bigint unsigned | 是 | null | 所属项目（关联 project.id） |
| status | varchar(16) | 否 | 在线 | 在线/离线/故障/维护中 |
| install_date | date | 是 | null | 安装日期 |
| last_update_at | datetime | 是 | null | 最后上报/维护时间 |
| created_at | datetime | 否 | now | — |
| updated_at | datetime | 否 | now on update | — |
| deleted | tinyint | 否 | 0 | 逻辑删除 |

索引：`uk_device_code(device_code, deleted)`、`idx_project_id(project_id)`、`idx_status(status)`、`idx_type(type)`

> `projectName` **不落库**，通过 `LEFT JOIN project p ON d.project_id = p.id` 取名（保证改名后设备列表同步更新，比 Mock 按名称匹配更准）。实体上用 `@TableField(exist = false)` 标记。
> 初始化：12 条，照抄 `mock/data.ts:65-78`；`project_id` 按 `projectName` 反查填入。

### 3.5 `material` 物料表

| 字段 | 类型 | 允许空 | 默认 | 说明 |
|---|---|---|---|---|
| id | bigint unsigned | 否 | auto | 主键 |
| material_code | varchar(32) | 否 | — | 物料编码，如 MAT-0112 |
| name | varchar(100) | 否 | — | 物料名称 |
| category | varchar(20) | 否 | — | 电芯电池/光伏组件/电气元件/结构件辅材 |
| spec | varchar(100) | 是 | null | 规格型号 |
| unit | varchar(16) | 否 | 件 | 计量单位 |
| stock | int unsigned | 否 | 0 | 当前库存 |
| safety_stock | int unsigned | 否 | 0 | 安全库存 |
| status | varchar(16) | 否 | 正常 | 正常/低库存/缺货（**由 Service 重算**） |
| created_at | datetime | 否 | now | — |
| updated_at | datetime | 否 | now on update | — |
| deleted | tinyint | 否 | 0 | 逻辑删除 |

索引：`uk_material_code(material_code, deleted)`、`idx_category(category)`、`idx_status(status)`、`idx_name(name)`
初始化：10 条，照抄 `mock/data.ts:81-92`（含 2 条缺货、3 条低库存，保证首页"库存预警"有数据）。

### 3.6 `stock_order` 出入库单表

| 字段 | 类型 | 允许空 | 默认 | 说明 |
|---|---|---|---|---|
| id | bigint unsigned | 否 | auto | 主键 |
| order_no | varchar(32) | 否 | — | 单据号：入库 `RK2026...`，出库 `CK2026...` |
| order_type | varchar(10) | 否 | — | `INBOUND` 入库 / `OUTBOUND` 出库 |
| material_id | bigint unsigned | 否 | — | 物料 id |
| material_name | varchar(100) | 否 | — | 冗余物料名（物料详情追溯直接查，**避免 JOIN**） |
| quantity | int unsigned | 否 | — | 数量 |
| target | varchar(100) | 否 | — | 入库=仓库名；出库=去向/项目名 |
| operator | varchar(50) | 否 | — | 经手人 |
| order_time | datetime | 否 | — | 单据时间（趋势图按此聚合） |
| remark | varchar(255) | 是 | null | 备注 |
| created_at | datetime | 否 | now | — |
| deleted | tinyint | 否 | 0 | 逻辑删除 |

索引：`uk_order_no(order_no)`、`idx_material_id(material_id, order_time)`、`idx_type_time(order_type, order_time)`、`idx_order_time(order_time)`
初始化：Mock 的 6 条入库 + 6 条出库（`mock/data.ts:95-111`）**必须保留**（物料详情追溯要用），另外补 **90 天 × 每天 3-5 条**的批量数据（约 300 条），让 7/30/90 天的趋势图和 Top5 有真实数据可聚合。批量数据用一条 `WITH RECURSIVE` 递归 CTE 生成，不需要手写 300 行 INSERT。

### 3.7 `report` 报表表

| 字段 | 类型 | 允许空 | 默认 | 说明 |
|---|---|---|---|---|
| id | bigint unsigned | 否 | auto | 主键 |
| type | varchar(20) | 否 | — | 运营汇总/库存分析/设备分析/物料分析/报表导出 |
| name | varchar(100) | 否 | — | 报表名称 |
| status | varchar(16) | 否 | 生成中 | 生成中/已生成/已取消 |
| creator | varchar(50) | 否 | — | 创建人（新建时取当前登录用户姓名） |
| created_at | datetime | 否 | now | 前端"生成时间" |
| updated_at | datetime | 否 | now on update | — |
| deleted | tinyint | 否 | 0 | 逻辑删除 |

索引：`idx_type_status(type, status)`、`idx_created_at(created_at DESC)`
初始化：6 条，照抄 `mock/data.ts:114-121`。
说明：一期后端只做"记录状态流转"，不真跑 Excel 导出（前端导出按钮本来就是假的提示）。二期再接异步生成 + 文件下载。

### 3.8 `device_metric` 设备监控表（**一期建表但可不写数据**，Q5）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint unsigned | 主键 |
| device_id | bigint unsigned | 设备 id |
| collect_at | datetime | 采集时间 |
| load_rate | decimal(5,2) | 负载率 % |
| temperature | decimal(5,2) | 温度 ℃ |

索引：`idx_device_time(device_id, collect_at DESC)`
一期 `GET /devices/{id}` 的 metrics 用确定性伪随机生成（同 id 同日期结果固定）；二期采集接入后，改成"有真实数据就查表、无数据再兜底生成"，前端零改动。

### 3.9 建表顺序与外键策略

建表顺序：`sys_user → project → device → material → stock_order → report → sys_operation_log → device_metric`
**一律不建数据库外键**（`FOREIGN KEY`）。理由：MyBatis-Plus 逻辑删除与外键约束冲突多，应用层校验更可控，单机 2C4G 也不需要靠外键保一致性。关联正确性由 Service 层保证（删项目前查设备、删物料前查单据）。

---

## 4. Spring Boot 项目目录结构

采用**按功能组织 + 三层结构**：Controller 只做参数与响应，Service 承载业务，Mapper/Repository 只做数据访问。

```
backend/
├── pom.xml
├── README.md                              # 启动与联调说明
├── .gitignore                             # 忽略 target/、.env、*.iml
└── src/main/
    ├── java/com/greengrid/admin/
    │   ├── GreenGridAdminApplication.java          # 启动类
    │   │
    │   ├── common/                                 # 全局基础设施
    │   │   ├── result/Result.java                  # 统一响应 {code,data,message}，含 ok()/ok(data)/fail(msg)
    │   │   ├── result/PageResult.java              # {list,total}，含 from(IPage) 转换
    │   │   ├── result/ErrorCode.java               # 401/403/400/500 常量
    │   │   ├── exception/BizException.java         # 业务异常（code + message）
    │   │   ├── exception/GlobalExceptionHandler.java # @RestControllerAdvice，兜住所有异常
    │   │   ├── query/PageQuery.java                # page/pageSize/keyword，默认值 + 上限保护
    │   │   └── util/IpUtil.java                    # 取真实客户端 IP（代理头兼容）
    │   │
    │   ├── config/
    │   │   ├── WebConfig.java                      # 注册 JWT 拦截器、静态资源、异常页
    │   │   ├── CorsConfig.java                     # 允许 http://localhost:5180
    │   │   ├── MybatisPlusConfig.java              # 分页插件 + 逻辑删除 + 自动填充
    │   │   ├── MetaObjectHandlerImpl.java          # created_at/updated_at 自动填充
    │   │   ├── JacksonConfig.java                  # 时间格式 yyyy-MM-dd HH:mm:ss / Long 转 String
    │   │   ├── PasswordConfig.java                 # BCryptPasswordEncoder Bean（仅 crypto）
    │   │   └── OpenApiConfig.java                  # Swagger 信息 + JWT 鉴权按钮
    │   │
    │   ├── security/                               # 鉴权（拦截器方案，不引 Spring Security）
    │   │   ├── JwtProperties.java                  # secret / expire-minutes / header / prefix
    │   │   ├── JwtUtil.java                        # 签发、解析、校验
    │   │   ├── AuthInterceptor.java                # 校验 Bearer token，写入 UserContext
    │   │   ├── UserContext.java                    # ThreadLocal 保存当前登录用户
    │   │   ├── LoginUser.java                      # id/username/name/role/roleName/dept
    │   │   ├── RequireRole.java                    # 自定义注解 @RequireRole({"super"})
    │   │   └── RoleInterceptor.java                # 读注解做角色校验
    │   │
    │   ├── modules/                                # 业务模块（每个模块自成一包）
    │   │   ├── auth/
    │   │   │   ├── AuthController.java
    │   │   │   ├── AuthService.java
    │   │   │   └── dto/LoginRequest.java, vo/LoginVO.java
    │   │   ├── dashboard/
    │   │   │   ├── DashboardController.java        # /dashboard/overview、/board/summary、/analytics/summary
    │   │   │   ├── DashboardService.java
    │   │   │   └── vo/OverviewVO.java, BoardSummaryVO.java, AnalyticsVO.java, KpiVO.java, TrendVO.java
    │   │   ├── project/
    │   │   │   ├── ProjectController.java
    │   │   │   ├── ProjectService.java
    │   │   │   ├── ProjectMapper.java
    │   │   │   ├── entity/Project.java
    │   │   │   ├── dto/ProjectSaveRequest.java, ProjectQuery.java
    │   │   │   └── vo/ProjectDetailVO.java
    │   │   ├── device/   （同上：Controller/Service/Mapper/entity/dto/vo）
    │   │   ├── material/ （同上）
    │   │   ├── stock/    # 出入库
    │   │   │   ├── StockOrderController.java       # /orders
    │   │   │   ├── StockOrderService.java          # 含物料详情追溯查询
    │   │   │   ├── StockOrderMapper.java
    │   │   │   └── entity/StockOrder.java
    │   │   ├── report/   （同上）
    │   │   └── system/
    │   │       ├── UserController.java / UserService.java / UserMapper.java / entity/SysUser.java
    │   │       └── LogController.java  / OperationLogService.java / OperationLogMapper.java / entity/SysOperationLog.java
    │   │
    │   └── init/DataSeeder.java                     # 启动时幂等补齐演示账号（BCrypt）
    │
    └── resources/
        ├── application.yml                          # 公共配置
        ├── application-dev.yml                      # 本地：MySQL 连接、SQL 初始化、Swagger 开启
        ├── application-prod.yml                     # 生产：关闭 Swagger、收紧日志
        ├── db/schema.sql                            # 建库建表（DROP IF EXISTS 幂等）
        ├── db/data.sql                              # 初始化业务数据（含递归 CTE 生成 90 天单据）
        └── mapper/                                  # 复杂 SQL 的 XML（一期预计只用到 3 个）
            ├── DashboardMapper.xml                  # 趋势、Top5、库存结构聚合
            └── DeviceMapper.xml                     # 设备列表 JOIN 项目名
```

为什么不按 `controller/service/mapper` 顶层分包：本项目有 8 个业务域，按层分包会让每个包塞 8 个不相干的类，改一个模块要跳 4 个目录；按模块分包后，"删掉报表功能"只需删一个包。

---

## 5. Maven 依赖清单

Spring Boot `3.5.3`（Maven 中心当前可用的 3.5.x 稳定版），Java `17`，打包 `jar`。

| 依赖 | 版本 | 用途 / 选用理由 |
|---|---|---|
| `spring-boot-starter-web` | 由父 POM 管理 | MVC + 内嵌 Tomcat |
| `spring-boot-starter-validation` | 由父 POM 管理 | `@Valid`/`@NotBlank` 参数校验（不要手写 if 判空） |
| `mybatis-plus-spring-boot3-starter` | **3.5.7** | Boot 3 专用 starter。⚠️ 若你本地已能拉到 `3.5.9+`，**必须额外加 `mybatis-plus-jsqlparser` 依赖**（3.5.9 起 JSQLParser 被拆出，否则分页插件启动即报 ClassNotFound） |
| `mysql-connector-j` | 由父 POM 管理 | MySQL 8 驱动（scope `runtime`） |
| `lombok` | 由父 POM 管理 | `@Data`/`@Slf4j`（scope `provided`，需 IDEA 装 Lombok 插件） |
| `springdoc-openapi-starter-webmvc-ui` | **2.8.6** | Swagger UI，访问 `/api/swagger-ui/index.html` |
| `jjwt-api` / `jjwt-impl` / `jjwt-jackson` | **0.12.6** | JWT 签发与校验。⚠️ 0.12.x API 与网上大量 0.9.x 教程**完全不同**（`Jwts.builder().signWith(key)`，不再用 `setSigningKey(SignatureAlgorithm, String)`），照抄旧教程必编译失败 |
| `spring-security-crypto` | 由父 POM 管理 | 只用 `BCryptPasswordEncoder` 做密码哈希，**不引入 `spring-boot-starter-security`**（避免整条安全过滤链带来的复杂度） |
| `spring-boot-starter-actuator` | 由父 POM 管理 | 可选，提供 `/actuator/health` |
| `spring-boot-starter-test` | 由父 POM 管理 | 单测（scope `test`） |

**明确不引入**：Spring Security 全家桶、Redis、RabbitMQ/Kafka、ShardingSphere、Nacos、Feign、Flyway/Liquibase（一期）。理由见第 10 节。

`pom.xml` 关键片段（**不是完整文件，Phase 2 才生成**）：

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.5.3</version>
</parent>
<properties>
  <java.version>17</java.version>
  <mybatis-plus.version>3.5.7</mybatis-plus.version>
  <springdoc.version>2.8.6</springdoc.version>
  <jjwt.version>0.12.6</jjwt.version>
</properties>
```

### 核心配置（`application-dev.yml` 关键项，Phase 2 会给完整文件）

```yaml
server:
  port: 8080
  servlet:
    context-path: /api          # 前端请求 /api/auth/login → Controller 写 /auth/login
    encoding: { charset: UTF-8, force: true }   # 中文枚举值查询靠它
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/greengrid_admin?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari: { maximum-pool-size: 10, minimum-idle: 2, connection-timeout: 3000 }
  sql:
    init: { mode: always, schema-locations: classpath:db/schema.sql, data-locations: classpath:db/data.sql }
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
mybatis-plus:
  configuration: { map-underscore-to-camel-case: true, log-impl: org.apache.ibatis.logging.stdout.StdOutImpl }
  global-config:
    db-config: { id-type: auto, logic-delete-field: deleted, logic-delete-value: 1, logic-not-delete-value: 0 }
greengrid:
  jwt: { secret: <32字节以上随机串>, expire-minutes: 120, header: Authorization, prefix: "Bearer " }
  cors: { allowed-origins: http://localhost:5180 }
```

---

## 6. 本地运行步骤（零基础版）

**前置检查**（先跑一遍，90% 的"起不来"都在这）：
1. `java -version` → 必须是 `17.x`（Microsoft OpenJDK 17 = `ms-17`，可用）
2. `mvn -v` → 3.8+；没装就装 Maven 并配好 `JAVA_HOME`
3. MySQL 服务已启动，`mysql -uroot -proot -e "select version();"` 能出 `8.x`

**步骤 1 · 建库**（用 Navicat 或命令行都行）
```sql
CREATE DATABASE IF NOT EXISTS greengrid_admin
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

**步骤 2 · 建表 + 初始化数据**
两种方式，任选其一（默认用 A）：
- **A（推荐）**：什么都不用做。后端启动时 `spring.sql.init.mode=always` 会自动执行 `db/schema.sql` + `db/data.sql`。
- **B**：手动在 Navicat 里执行这两个 SQL 文件，然后把 `sql.init.mode` 改成 `never`。

**步骤 3 · 启动后端**
```bash
cd backend
mvn clean spring-boot:run -Dspring-boot.run.profiles=dev
# 或先打包再跑：mvn clean package -DskipTests && java -jar target/greengrid-admin-server.jar --spring.profiles.active=dev
```
看到 `Tomcat started on port 8080 (http) with context path '/api'` 即成功。

**步骤 4 · 冒烟验证（不用前端就能测）**
```bash
# 1) 登录，拿 token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"123456\",\"remember\":true}"
# 期望：{"code":0,"data":{"token":"eyJ...","user":{...}}}

# 2) 带 token 取项目列表（把 <TOKEN> 换成上面的值）
curl "http://localhost:8080/api/projects?page=1&pageSize=10" -H "Authorization: Bearer <TOKEN>"
# 期望：{"code":0,"data":{"list":[...12条...],"total":12}}

# 3) 中文筛选（验证 utf8mb4 与拦截器）
curl -G "http://localhost:8080/api/projects" --data-urlencode "status=进行中" -H "Authorization: Bearer <TOKEN>"
# 期望：total 为 8

# 4) 无 token 访问
curl -i http://localhost:8080/api/projects
# 期望：HTTP 401 + {"code":401,...}

# 5) 错误密码
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"wrong\"}"
# 期望：{"code":1,"message":"账号或密码错误"}
```
Swagger：浏览器打开 `http://localhost:8080/api/swagger-ui/index.html`，右上角 Authorize 填 `Bearer <token>` 后可逐个接口点测。

**常见报错速查**

| 报错 | 原因 | 解决 |
|---|---|---|
| `Communications link failure` | MySQL 没启动 / 端口不对 | 启动 MySQL；确认 3306 未被占用 |
| `Public Key Retrieval is not allowed` | MySQL 8 默认 `caching_sha2_password` | 连接串加 `allowPublicKeyRetrieval=true`（已含） |
| `Access denied for user 'root'@'localhost'` | 密码不是 root | 改 `application-dev.yml` 的 password |
| `Unknown database 'greengrid_admin'` | 没建库 | 执行步骤 1 |
| `Table 'greengrid_admin.sys_user' doesn't exist` | 没执行 SQL 初始化 | 确认 `sql.init.mode=always`，或手动执行 schema.sql |
| 启动即报 `ClassNotFoundException: net.sf.jsqlparser...` | MyBatis-Plus ≥3.5.9 缺分页依赖 | 加 `mybatis-plus-jsqlparser`，或降到 3.5.7 |
| `Port 8080 was already in use` | 端口占用 | `netstat -ano \| findstr :8080` 找 PID 后结束进程，或换 `server.port` |
| Swagger 打开 404 | context-path 搞错 | 地址必须是 `/api/swagger-ui/index.html` |
| 所有接口 401（含登录） | 拦截器把登录也拦了 | 放行 `/auth/login`、`/swagger-ui/**`、`/v3/api-docs/**`、`/actuator/**` |
| 中文变问号 `???` | 库/表/连接串编码不对 | 三者都确认 `utf8mb4`，且加 `server.servlet.encoding.force=true` |

---

## 7. 前后端联调步骤

### 步骤 1 · Vite 代理（`vite.config.ts` 的 `server` 段）

```ts
server: {
  port: 5180,
  open: false,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
      // ⚠️ 绝对不要加 rewrite！后端 context-path 已经是 /api，
      //    再 rewrite 去掉 /api 会变成 404（见坑 2）
    }
  }
}
```

### 步骤 2 · 给 Mock 装开关（**必须做，否则后端收不到任何请求**）

`src/utils/request.ts` 的 adapter 段改成：

```ts
/* 仅当 VITE_USE_MOCK === 'true' 时挂载 Mock 适配器 */
if (import.meta.env.VITE_USE_MOCK === 'true') {
  instance.defaults.adapter = async (config) => { /* 原有逻辑不变 */ }
}
```

新增 `.env.development`：
```
VITE_USE_MOCK=false
```
新增 `.env.development.mock`（想切回纯 Mock 演示时用）：
```
VITE_USE_MOCK=true
```
联调完成后建议把 `src/mock/` 保留但不再打包，或改由 `VITE_USE_MOCK` 控制动态 import。

### 步骤 3 · 给请求拦截器补错误映射（**当前缺口，见发现 2**）

```ts
instance.interceptors.response.use(
  (res) => { /* 原有 code === 0 解包逻辑不变 */ },
  (err) => {
    const status = err?.response?.status
    const msg = err?.response?.data?.message
      || (status === 401 ? '登录已过期，请重新登录'
        : status === 403 ? '暂无该操作权限'
        : status === 500 ? '服务异常，请稍后重试'
        : '网络异常，请稍后重试')
    message.error(msg)
    if (status === 401) {
      localStorage.removeItem('gg_token')
      localStorage.removeItem('gg_user')
      // 用 location 跳转，避免在 utils 里 import router 造成循环依赖
      if (!location.pathname.endsWith('/login')) {
        location.href = `/login?redirect=${encodeURIComponent(location.pathname)}`
      }
    }
    return Promise.reject(err)
  }
)
```

### 步骤 4 · 联调验证清单（按顺序做，每步一个页面）

| 顺序 | 页面 | 验证点 |
|---|---|---|
| 1 | `/login` | admin/123456 能进首页；错误密码弹"账号或密码错误"而不是"Request failed with status code 200" |
| 2 | 数据总览 | 6 个 KPI 数字正确；趋势图两条线（入/出库）不为空；"最近动态"4 条 |
| 3 | 项目管理 | 列表 12 条、分页 total 对；筛"进行中"+区域联动；新增后列表出现；详情抽屉"关联设备"有数据 |
| 4 | 设备管理 | 列表带"所属项目"名称（验证 JOIN）；详情抽屉两条曲线各 12 点；点"维护"后状态变"维护中" |
| 5 | 物料管理 | 列表缺货/低库存标签正确；详情 drawers 出入库追溯有记录；新增库存 0 的物料 → 状态自动为"缺货" |
| 6 | 数据报表 | range 切 7/30/90 趋势与 Top5 变化；"取消"后状态变"已取消"（验证发现 3） |
| 7 | 运营分析 | 环形图四项占比与 total 一致 |
| 8 | 用户与权限 | 筛"已禁用"能出结果（验证发现 4）；新增用户不带密码 → 用 123456 能登录 |
| 9 | 角色鉴权 | 用 guest 登录 → 访问 `/system/user` 被前端守卫拦；直接 curl 该接口 → 403 |
| 10 | 鉴权过期 | 手动改坏 localStorage 里的 token → 刷新页面 → 跳登录页 |

---

## 8. 开发顺序

按"能尽早跑通端到端"排序，每个阶段结束都有可验证的成果。

| 阶段 | 内容 | 交付物 | 完成标志（自测） |
|---|---|---|---|
| **P0** | 骨架 + 基础设施 | pom.xml、启动类、application*.yml、Result/PageResult/BizException/GlobalExceptionHandler、CORS、Jackson、MyBatis-Plus 分页与逻辑删除、Swagger、`/health` | `mvn spring-boot:run` 起得来；`/api/swagger-ui/index.html` 打得开；`/api/health` 返回 `{"code":0,...}` |
| **P1** | 数据库 + 建表 + 种子数据 | schema.sql、data.sql、DataSeeder | `sys_user` 5 条（密码 BCrypt）；project/device/material/report/log 条数与 Mock 一致；stock_order 约 300 条 |
| **P2** | 登录鉴权（**关键路径**） | JwtUtil、AuthInterceptor、RoleInterceptor、`@RequireRole`、AuthController（login/me/logout） | curl 登录拿到 token；无 token 401；错误密码 code 1；登录成功写一条 action='登录' 的日志 |
| **P3** | 项目管理 CRUD | Project 模块全套 + 详情 JOIN 设备 + 关联校验 | 5 个接口 Swagger 全通；`GET /projects/1` 带 devices；删有设备的项目被拒 |
| **P4** | 设备管理 | Device 模块 + 列表 JOIN 项目名 + metrics 生成 + 维护日志 | 列表 projectName 正确；详情 12 点曲线；PUT 状态后 lastUpdateAt 更新 |
| **P5** | 物料 + 出入库 | Material CRUD + status 重算 + 详情追溯 + `/orders` 读接口 | 库存 0 → 缺货；详情追溯按物料名命中；改库存后状态自动变 |
| **P6** | 系统管理 | 用户 CRUD + 重置密码 + 日志分页查询 + 关键操作落日志 | 筛 status=0 有效；新建用户可用 123456 登录；日志按 module/action 筛 |
| **P7** | 报表 | Report CRUD + status 放开"已取消" | 取消后状态为"已取消"，列表能查到 |
| **P8** | 三个看板接口（**最费脑、放最后**） | DashboardService + 聚合 SQL + VO | 首页/看板/分析三页数据全非空；range 切换结果变化；P95 均 < 200ms |
| **P9** | 联调收尾与加固 | 前端 request.ts/vite.config 改动、Swagger 完善、`mvn package`、Dockerfile + compose | 第 7 节 10 条清单全绿；`docker compose up` 一键起 |

依赖关系：P2 是 P3-P8 的硬前置（拦截器 + `UserContext` 后面每个模块都用）；P3 是 P4 的前置（设备挂项目）；P5 是 P8 的前置（趋势/结构图吃单据数据）。
如果你想**先看到成果**：P0→P1→P2→P8 也能跑通首页（看板先用种子聚合），之后再回头补 P3-P7。

---

## 9. 前端新手接后端最容易踩的 10 个坑

1. **Mock 永远拦在前面，后端一条请求都收不到。**
   `request.ts:38` 直接给 axios 实例换了 adapter，命中就返回。你在 `.env` 里写什么都没用。**必须先按第 7 节步骤 2 加开关**。表现为：后端日志干干净净，前端一切正常，你会以为是后端没部署。

2. **Vite 代理不能再 rewrite。**
   后端 `context-path=/api`，前端 baseURL 也是 `/api`。代理里加 `rewrite: path => path.replace(/^\/api/, '')` 就会变成 `localhost:8080/auth/login` → 404。**要么后端 context-path 为空、要么代理不 rewrite，二选一**，本项目选后者。

3. **响应结构差一个字段，前端就白屏。**
   成功必须 `{code:0,data:...}`，分页必须是 `{list,total}`。Spring Data 的 `Page` 序列化出来是 `content/totalElements`，MyBatis-Plus 的 `Page` 是 `records/total`——**两个前端都不认**。必须手动转成 `PageResult{list,total}`。前端拦截器只认 `code`，少写一层包装就会 `data === undefined`，然后页面报 `Cannot read properties of undefined (reading 'list')`。

4. **日期时间格式：Jackson 默认给你一个带 `T` 的字符串。**
   `LocalDateTime` 默认序列化成 `2026-09-14T09:12:00`，前端表格里就会显示那个 `T`（前端不格式化，直接渲染）。必须配 `spring.jackson.date-format` + `@JsonFormat`。

5. **空字符串日期反序列化直接把请求打成 400。**
   前端 `ProjectList.vue:247` 提交时 `startDate: dateRange?.[0] || ''`——**未选日期就是空串**。`LocalDate` 收到 `""` 会抛 `DateTimeParseException` → 400。后端字段必须写成"空串当 null"的自定义反序列化器或统一转成 String 再解析。这条不处理，新增项目永远失败。

6. **`status` 是 0/1 数字，且 0 最容易写错。**
   `UserManage.vue:220` 传 `{status: 0}`。如果你在 Service 里用无脑 `StringUtils.hasText` 或 MyBatis-Plus 的 `updateById`（忽略 null 但**不会忽略 0**，这里其实是对的）判断，容易写成 `if (status != null && status == 1)` 把禁用逻辑吞掉。**接收类型用 `Integer`，判断只用 `!= null`**。

7. **PUT 是"部分更新"，不是整体覆盖。**
   前端编辑项目传全字段，但设备/用户/报表只传一个字段（`{status}`）。后端如果用 `updateById(entity)` 且不加处理，非空字段被覆盖没问题（null 会被忽略），但**一旦你改用 `UpdateWrapper` 全字段 set，就会把没传的字段写成 null**。统一约定：PUT 一律"只更新非 null 字段"。

8. **逻辑删除 + 唯一索引 = 删完就再也加不进去。**
   `uk_username(username)` 配上 `deleted` 标记，删掉 `ops` 后再建 `ops` 会报唯一键冲突。正确做法：**复合唯一索引 `(username, deleted)`**，或删除时把 username 改成 `ops_deleted_1712345678`。7 张表全都要注意。

9. **拦截器会把 OPTIONS 预检和 Swagger 一起拦掉。**
   用 Spring MVC 拦截器做 JWT 时，忘记 `excludePathPatterns("/auth/login", "/swagger-ui/**", "/v3/api-docs/**", "/error", "/actuator/**")`，会出现"登录接口也返回 401"和"Swagger 页面空白"。另外 CORS 预检是 `OPTIONS` 请求且不带 token，拦截器要么放行 OPTIONS、要么用 `CorsFilter`/`WebMvcConfigurer.addCorsMappings` 让它先于拦截器生效。**直连 8080 调试时 CORS 必须配，走 Vite 代理时反而用不到——两种环境都要能跑。**

10. **字段名少一个就静默空白，最难查。**
    前端 `{{ record.lastLoginAt }}` 拿不到值时**不报错，只是显示空白**。后端 VO 少一个 `lastLoginAt`、或者写成 `lastLoginTime`，页面不会崩、控制台不报警，你只能靠肉眼比 Mock 数据。**做法：每个 VO 生成后，用 Mock 的字段清单逐字段对一遍**（本方案第 2 节的响应结构就是那份清单）。

补充两条容易忽视的：

- **别把 `password` 序列化出去。** 实体的 `password` 字段加 `@JsonIgnore`，或用独立 VO 返回。前端 `UserInfo` 没有这个字段，多送一个不会有提示，但这是安全问题。
- **中文枚举值全链路编码。** 库里 `utf8mb4`、连接串 `characterEncoding=utf8mb4`、`server.servlet.encoding.force=true`，三者缺一个，筛选中文状态就会查不到数据（表现为"总量有值但按状态筛永远是 0 条"）。

---

## 10. 什么时候才需要 Redis / 缓存 / 读写分离

现在**都不需要**，别提前上。触发条件如下（写在这里，等你真遇到再回来看）：

| 技术 | 触发信号 | 那时的做法 |
|---|---|---|
| 本地缓存（Caffeine） | 某个接口 QPS > 50，且数据 1 分钟内不变（如看板的设备类型字典） | 先加 Caffeine，10 行代码，别上 Redis |
| Redis | ① 多实例部署，本地缓存不一致；② token 需要主动失效/黑名单（现在是短时效 JWT，退出即丢，够用）；③ 接口 P95 > 200ms 且确认卡在 DB 重复查询 | 先用 Spring Cache + Redis 缓存看板聚合结果，TTL 60s |
| 读写分离 | MySQL 单表 > 500 万行，或写入 QPS > 300 | 先做归档（`sys_operation_log` 按月分区/归档），再考虑主从 |
| 消息队列 | 报表生成变成真任务（几分钟级）、或对接采集系统产生突发流量 | 先上"DB 任务表 + 定时轮询"，QPS < 200 时 MQ 是纯负担 |
| 分库分表 | 单表 > 1000 万且归档无效 | 本项目 10 万级，永远用不到 |
| 搜索引擎 | 需要**全文检索**日志内容 | 先用 `LIKE`，QPS 200 以内 `LIKE '%kw%'` 也够；日志 > 50 万行再上 ES |

判断标准只有一条：**先用最简单的方式做到 P95 < 200ms，做不到再逐级加**。你的预估是 QPS 200 / 单表 10 万，当前架构（单体 + 单库 + HikariCP 10 连接）在这套规模下是**过剩的**，不是不够的。

---

## 11. 确认后我会做什么（Phase 2 起）

你回复确认（或指出要改的点）后，我按 P0 → P9 顺序生成代码，每阶段交付：

1. 可直接 `mvn spring-boot:run` 的完整代码（**不省略任何 import，不写 `// TODO 自行补充`**）
2. 该阶段对应的 curl 冒烟命令 + 期望输出
3. 该阶段常见的 2-3 个报错与解法
4. 出问题时的排查路径（看哪个日志、看哪个表、看哪个字段）

需要你确认的最小集合（其他都按我的默认建议走也行）：

- **Q1** 后端目录（默认 `backend/` 子目录）
- **Q3** 看板是"真算"还是"返种子数据"（默认真算）
- **Q6 / Q7** 是否加 `/auth/me`、`/auth/logout`、重置密码接口（默认都加）
- **Q9** 演示账号密码默认 `123456` 由启动器写入（默认按此）
