# 金融科研 AI 辅助平台（MVP）

中文界面、前后端分离的金融学科研全流程演示系统：选题与文献 → EDC/数据清洗与统计 → 论文写作 → 审稿回修，集成 **Claude API**（可配置）与 **数据脱敏 / 审计** 能力。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3、TypeScript、Vite、Element Plus、Pinia、ECharts、vuedraggable |
| 后端 | Spring Boot 3、Java 17、Spring Security + JWT、JPA、MySQL 8 |
| AI | Anthropic Messages API，提示词封装为可复用 **Skill**（`SkillId` + `SkillPrompts`） |
| 部署 | Docker Compose（MySQL + 后端 + Nginx 前端） |

## 本机先看页面跑通（给个人/演示）

1. 需 **JDK 17**、**Maven**、**Node.js**；8080 被占时运行 `backend/free-port-8080.bat`  
2. **`backend/run-dev-h2.bat`**（H2 内存库，**不要**本机 MySQL；窗口保持打开，等到 `Started FinResearchApplication`）  
3. **`frontend`**：`npm install` → **`npm run dev`**  
4. 浏览器 **http://localhost:5173**，登录 **demo / demo123456**  

> 若你曾遇到 H2 启动后立刻崩溃：请拉取最新代码；`application-h2.yml` 已强制使用 **H2Dialect**，避免误用主配置里的 MySQL 方言导致 **users 表不存在**。

更细说明与 **GitHub / 团队部署**：[docs/本机预览与GitHub协作.md](./docs/本机预览与GitHub协作.md)  
**GitHub 放哪些文件、怎么 push、同事怎么 clone**：[docs/GitHub团队协作.md](./docs/GitHub团队协作.md)  
**开发环境、GitHub 上传与成员跑通、API Key 配置（团队必读）**：[docs/DEV_ENV.md](./docs/DEV_ENV.md)

---

## 快速开始（本地开发）

### 1. MySQL

创建库 `finresearch`（utf8mb4），修改 `backend/src/main/resources/application.yml` 或使用环境变量覆盖数据源。

### 2. 后端

**若本机 MySQL 未装好或总连不上**，可直接用内置 H2（无需 MySQL，适合先跑通闭环）：

```bash
cd backend
# Windows：双击 run-dev-h2.bat
# 或命令行：
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

使用 **MySQL** 时：

```bash
cd backend
mvn spring-boot:run
```

默认端口 `8080`。Swagger UI：<http://localhost:8080/swagger-ui/index.html>

环境变量（可选）：

- `ANTHROPIC_API_KEY`：未配置时使用内置**演示占位**输出，仍可跑通闭环。
- `JWT_SECRET`、`ENCRYPTION_KEY_BASE64`（32 字节 Base64）：生产环境务必修改。

### 3. 前端

```bash
cd frontend
npm install
npm run dev
```

开发服务器 <http://localhost:5173>，通过 Vite 代理转发 `/api` 到后端。

### 4. 演示账号

首次启动会自动创建：`demo` / `demo123456`。

## Docker 一键部署（含云服务器）

- **本机 / 通用**： [DEPLOY.md](./DEPLOY.md)  
- **云服务器从零上线（推荐）**： [docs/云服务器部署步骤.md](./docs/云服务器部署步骤.md)

```bash
docker compose up -d --build
```

浏览器访问 <http://localhost>（前端），API 经同域 `/api` 反向代理。

## 文档索引

| 文档 | 说明 |
|------|------|
| [DEPLOY.md](./DEPLOY.md) | 容器与云服务器部署 |
| [DEMO.md](./DEMO.md) | 从选题到回修的演示脚本 |
| [prompt_library.md](./prompt_library.md) | Claude Skill 提示词库 |
| [docs/DATABASE_DESIGN.md](./docs/DATABASE_DESIGN.md) | 表结构、字段与 ER |
| [docs/](./docs/) | 需求/选型说明（md + Word 可打开的 .doc） |
| `sample_data/demo_panel.csv` | 示例面板数据（清洗/统计试用） |

## 合规说明（MVP）

- 用户上传文件以 **AES-GCM** 加密写入数据目录；送大模型前对文本做 **脱敏** 并写入 **脱敏日志**。
- 重要操作写入 **审计日志**（可在「文档与合规」页查看）。
- 生产环境请补充：KMS、细粒度 RBAC、数据出境评估、与 CSMAR/Akshare 正式合同接口等。

## 许可证

MIT（示例项目，商用前请自行审查第三方数据与模型服务条款）。
