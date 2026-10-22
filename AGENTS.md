# AGENTS.md — 校园集（campus-market）协作说明

本文件是给任何接手本仓库的开发者或 AI 编码代理的第一份文档。先读这里，再读 `docs/PRD.md` 与 `docs/TECH_DESIGN.md`。

## 1. 项目速览

- 产品：校园二手交易与信用评价平台，学生买卖一体，审核员审核商品与处理举报，管理员看数据。
- 后端：`api/`，Java 17 + Spring Boot 3.5.16 + Spring Data JPA + Flyway + MySQL 8 + Spring Security/JWT。
- 前端：`web/`，Vue 3 + TypeScript + Vite + Pinia + Vue Router + Element Plus + ECharts。
- 文档：`docs/PROMPT.md`（冷启动提示词）、`docs/PRD.md`（需求）、`docs/TECH_DESIGN.md`（技术设计）。
- 数据库：本机独立 MySQL 实例，端口 **3307**，库 `campus_market`，账号 `campus / Campus@2026`。

## 2. 目录导航

| 路径 | 内容 |
| --- | --- |
| `api/src/main/java/com/campusmarket/common` | 统一响应、错误码、异常处理、分页、traceId |
| `api/src/main/java/com/campusmarket/config` | Security、JWT、CORS、OpenAPI、配置属性 |
| `api/src/main/java/com/campusmarket/auth` | 注册登录、令牌轮换、校园认证 |
| `api/src/main/java/com/campusmarket/user` | 资料、信用档案、通知 |
| `api/src/main/java/com/campusmarket/catalog` | 分类、商品、图片、收藏、检索 |
| `api/src/main/java/com/campusmarket/chat` | 会话与消息 |
| `api/src/main/java/com/campusmarket/trade` | 订单状态机、托管钱包、退款 |
| `api/src/main/java/com/campusmarket/review` | 双向评价 |
| `api/src/main/java/com/campusmarket/credit` | 信用分计算 |
| `api/src/main/java/com/campusmarket/moderation` | 审核、举报工单、审计日志 |
| `api/src/main/java/com/campusmarket/admin` | 后台接口与看板 |
| `api/src/main/java/com/campusmarket/web` | 健康检查、文件上传 |
| `api/src/main/resources/db/migration` | Flyway 迁移脚本，**只增不改** |
| `web/src/api` | 按模块封装的请求函数 |
| `web/src/views/student` | 学生端页面 |
| `web/src/views/admin` | 后台页面 |
| `scripts` | 数据库启停脚本 |

## 3. 命令清单

```powershell
# 数据库（独立实例，端口 3307）
.\scripts\start-db.ps1
.\scripts\stop-db.ps1
cmd /c '"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -h 127.0.0.1 -P 3307 --default-character-set=utf8mb4 < scripts\db-init.sql'

# 后端（首次下载依赖建议加 -s maven-settings.xml 走阿里云镜像）
cd api
.\mvnw.cmd -s maven-settings.xml spring-boot:run
.\mvnw.cmd -s maven-settings.xml test
.\mvnw.cmd -s maven-settings.xml -DskipTests package

# 前端
cd web
npm install
npm run dev
npm run build
```

- Swagger UI：http://localhost:8080/swagger-ui.html
- 健康检查：http://localhost:8080/api/v1/health
- 前端开发：http://localhost:5173

## 4. 编码规范

### 后端

1. 分层固定：`Controller` 只做参数校验与编排，业务规则全部在 `Service`，`Repository` 不写业务判断。
2. 请求/响应必须用 `dto/` 下的 record，禁止把实体当出入参；实体不出包。
3. 统一返回 `ApiResponse<T>`；分页返回 `PageResult<T>`；错误抛 `BusinessException(ErrorCode, message)`。
4. 状态变更只能通过状态机的 `assertTransition`，禁止在 Controller 里直接 `setStatus`。
5. 资金相关方法必须 `@Transactional(rollbackFor = Exception.class)`，并写 `wallet_ledger`。
6. 权限：接口级用 `@PreAuthorize`，数据级用 `assertOwner/assertParticipant`，二者都不可省。
7. 命名：包名小写，实体单数（`Item`），表名复数（`items`）；DTO 后缀 `Request/Response`。
8. 日志：关键动作 `log.info`，异常 `log.warn/error`，禁止打印密码与 token。

### 前端

1. 所有请求走 `src/api/*.ts`，组件内不直接写 axios。
2. 类型放 `src/types`，与后端 DTO 字段一一对应。
3. 页面结构固定：工具栏 → 筛选区 → 列表/表格 → 分页。
4. 状态展示用 `StatusTag`，金额格式化用 `formatMoney`，时间用 `formatDateTime`。
5. 每个异步操作都要有 loading 与失败提示，空数据用 `EmptyState`。

## 5. 数据库迁移规则

- 位置：`api/src/main/resources/db/migration/V{n}__{desc}.sql`。
- 已提交的迁移文件禁止修改，只能新增。
- 每个迁移必须可在空库上一次跑通；字段变更同步更新实体与 `docs/TECH_DESIGN.md` 表结构。
- 演示数据放 `V2__seed_demo_data.sql`，账号密码哈希固定（明文见 `README.md`）。
- 演示商品图集随仓库分发：`web/public/images/items/{slug}-{1..3}.svg`，由 `V3__seed_local_item_images.sql` 写入，每件商品 3 张，不依赖外网图床。

## 6. 里程碑进度

| 里程碑 | 内容 | 状态 |
| --- | --- | --- |
| M0 | 环境与脚手架、健康检查 | 完成 |
| M1 | PRD / TECH_DESIGN / AGENTS 文档 | 完成 |
| M2 | Flyway 建表 + 实体 + seed 数据 | 完成 |
| M3 | 注册登录、刷新令牌、校园认证、角色守卫 | 完成 |
| M4 | 商品发布、审核、检索、收藏、推荐 | 完成 |
| M5 | 聊天议价、订单托管、结算、退款、超时任务 | 完成 |
| M6 | 双向评价、信用分、发布限制 | 完成 |
| M7 | 举报工单、审计日志、站内通知 | 完成 |
| M8 | 后台审核队列、用户/类目管理、数据看板 | 完成 |
| M9 | 学生端与后台页面 | 完成 |
| M10 | 自动化测试、构建、README 与演示账号 | 完成 |

完成一个里程碑后更新本表状态，并写明当次变动要点。

## 7. 禁止事项

- 不引入 Redis、消息队列、Docker、真实支付 SDK。
- 不提交密钥、密码、`.env`、`uploads/`、`target/`、`node_modules/`。
- 不留 `TODO` 桩代码、不返回假数据、不跳过校验。
- 不修改已执行的 Flyway 迁移文件。
- 不绕过状态机、不在前端做权限判断替代后端校验。
