# 校园集（campus-market）

校园二手交易与信用评价平台。学生完成校园认证后发布闲置，平台审核上架；买家下单后资金进入托管，确认收货才结算给卖家（平台收取 2% 手续费）；交易完成双方互评，信用分决定发布权限与曝光。审核员处理商品审核与举报工单，管理员查看数据看板与做用户治理。

## 技术栈

| 层 | 选型 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.5、Spring Security + JWT、Spring Data JPA、Flyway、MySQL 8 |
| 前端 | Vue 3 + TypeScript、Vite、Pinia、Vue Router、Element Plus、ECharts |
| 测试 | JUnit 5 + MockMvc + H2（内存库，无需 MySQL） |

## 目录结构

```
final/
├─ api/                        # 后端（Spring Boot）
│  ├─ src/main/java/com/campusmarket/
│  │  ├─ auth/                 # 注册登录、令牌轮换、校园认证
│  │  ├─ user/                 # 资料、信用档案、站内通知
│  │  ├─ catalog/              # 分类、商品、收藏、检索
│  │  ├─ chat/                 # 会话与议价消息
│  │  ├─ trade/                # 订单状态机、托管钱包、退款、超时任务
│  │  ├─ review/ credit/       # 双向评价与信用分
│  │  ├─ moderation/           # 举报工单、审计日志
│  │  ├─ admin/                # 后台接口与看板
│  │  └─ common/ config/ web/  # 统一响应、异常、安全配置、健康检查、上传
│  └─ src/main/resources/db/migration/   # Flyway 脚本（启动时自动执行）
├─ web/                        # 前端（Vue 3）
│  └─ src/{api,stores,router,layouts,views,components,types}/
├─ docs/                       # PROMPT / PRD / TECH_DESIGN
├─ scripts/                    # 本地 MySQL 启停与建库脚本
├─ AGENTS.md                   # 协作约定与命令清单
└─ .env.example                # 环境变量示例
```

## 环境要求

- JDK 17
- Node.js 20.19+（当前验证版本 24.x）与 npm
- MySQL 8（本项目使用独立实例，端口 **3307**，避免与系统 `MySQL80` 服务冲突）

## 快速启动（Windows PowerShell）

### 1. 启动数据库

`scripts/start-db.ps1` 使用 `C:\Program Files\MySQL\MySQL Server 8.0\bin` 下的 `mysqld`，数据目录为 `C:\campus-market-mysql\data`，首次运行会自动初始化。

```powershell
cd <项目根目录>
.\scripts\start-db.ps1
cmd /c '"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -h 127.0.0.1 -P 3307 --default-character-set=utf8mb4 < scripts\db-init.sql'
```

第二条命令创建 `campus_market` 库与账号 `campus / Campus@2026`。表结构与演示数据由后端启动时的 Flyway 自动执行，无需手工建表。

停止数据库：

```powershell
.\scripts\stop-db.ps1
```

### 2. 启动后端

```powershell
cd api
.\mvnw.cmd -s maven-settings.xml spring-boot:run
```

`-s maven-settings.xml` 会把 Maven Central 镜像到阿里云，首次下载依赖更快；依赖已缓存后可省略，或加 `-o` 离线运行。

启动成功后：

- 健康检查：http://localhost:8080/api/v1/health
- Swagger UI：http://localhost:8080/swagger-ui.html

### 3. 启动前端

```powershell
cd web
npm install
npm run dev
```

打开 http://localhost:5173 。Vite 已将 `/api` 代理到 `http://localhost:8080`，无需额外配置跨域。

### 4. 生产构建

```powershell
cd web
npm run build          # 产物在 web/dist
cd ..\api
.\mvnw.cmd -s maven-settings.xml -DskipTests package   # 产物在 api/target/api-0.0.1-SNAPSHOT.jar
```

## 演示账号

密码统一为 `Campus@2026`（演示数据见 `api/src/main/resources/db/migration/V2__seed_demo_data.sql`）。

| 账号 | 昵称 | 角色 | 说明 |
| --- | --- | --- | --- |
| `admin` | 张明远 | ADMIN | 数据看板、用户治理、分类维护、审计日志 |
| `mod001` | 陈默 | MODERATOR | 商品审核、举报工单 |
| `2021001` | 林晓 | STUDENT | 已认证，有余额与在售商品 |
| `2021002` | 苏晴 | STUDENT | 已认证，有进行中的订单 |
| `2021003` | 李航 | STUDENT | 已认证 |
| `2021004` | 王梓 | STUDENT | **未完成校园认证**，用于演示发布前的认证拦截 |
| `2021005` | 赵昕 | STUDENT | 状态为 WARNED，用于演示信用/治理限制 |

登录页提供前三个账号的一键填充按钮。

## 演示商品图

16 件演示商品各配 3 张插画（封面 / 成色细节 / 配件一览），共 48 个 SVG，位于 `web/public/images/items/`，由 `api/src/main/resources/db/migration/V3__seed_local_item_images.sql` 写入 `item.cover_url` 与 `item_image`。图片随前端构建产物一起发布，不依赖外网图床；用户自行上传的图片仍走 `api/uploads` 与 `/api/v1/files/{filename}`。

## 核心业务流程

1. 注册（学号 + 校内邮箱 + 密码）→ 校园认证（演示环境直接在返回中给出 6 位验证码）。
2. 发布商品（草稿或直接提交）→ 审核员通过 → `ON_SALE` 出现在首页与检索结果。
3. 买家发起会话议价 → 下单 `PENDING_PAY` → 余额支付进入托管 `PAID`。
4. 卖家发货 `SHIPPED` → 买家确认收货 `COMPLETED` → 扣 2% 手续费后结算给卖家。
5. 双方互评 → 重算信用分（低于 60 禁止发布，高于 90 不限发布数量）。
6. 未支付订单 30 分钟超时自动取消；`PAID` 后买家可申请退款，卖家同意则原路退回托管金额。
7. 举报 → 审核员领取并裁定（下架商品 / 警告 / 封禁），全过程写审计日志并推送站内通知。

## 测试

```powershell
cd api
.\mvnw.cmd -s maven-settings.xml test
```

测试使用 H2 内存库（`src/test/resources/application-test.yml`，Flyway 关闭、JPA `create-drop`），覆盖注册与认证、商品审核与检索、托管交易结算、退款、评价与信用分、举报治理与低信用发布限制。当前 4 个用例全部通过。

## 配置

后端所有配置都有本地默认值，需要覆盖时参考 `.env.example`：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_HOST` / `DB_PORT` | `127.0.0.1` / `3307` | MySQL 地址 |
| `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` | `campus_market` / `campus` / `Campus@2026` | 数据库与账号 |
| `JWT_SECRET` | 开发用固定串 | 生产必须替换 |
| `UPLOAD_DIR` | `./uploads` | 图片上传目录（相对 `api/`） |
| `CORS_ORIGINS` | `http://localhost:5173` | 允许的前端来源 |
| `SERVER_PORT` | `8080` | 后端端口 |
| `DEMO_EXPOSE_CODE` | `true` | 是否在响应中返回校园认证验证码，生产应关闭 |

## 常见问题

- **`MySQL80` 服务无法启动**：该服务需要管理员权限且指向系统 `my.ini`。本项目改用 `scripts/start-db.ps1` 启动的独立实例，端口 3307，不影响系统服务。
- **首次 `mvnw` 下载依赖慢或失败**：加上 `-s maven-settings.xml` 使用阿里云镜像；仍失败时检查网络代理。
- **前端请求 401 后自动跳登录**：`web/src/api/http.ts` 会用 refresh token 自动续期，续期失败才清理会话并跳转。
- **图片上传后不显示**：确认后端已启动且 `api/uploads` 目录可写；开发环境通过 `/api/v1/files/{filename}` 访问。

## 文档

- `docs/PROMPT.md`：项目冷启动提示词与范围约定
- `docs/PRD.md`：需求、角色、功能清单与验收标准
- `docs/TECH_DESIGN.md`：ER 图、状态机、接口一览、信用分公式
- `AGENTS.md`：分层规范、编码约定、迁移规则
