# 开发环境与闭环自测

本文说明如何在**本机**跑通前后端，供个人与 GitHub 团队成员复现。

## 必备软件

| 组件 | 版本建议 | 用途 |
|------|----------|------|
| JDK | 17 | Spring Boot 后端 |
| Maven | 3.8+ | `mvn spring-boot:run` |
| Node.js | 18 LTS+ | 前端 Vite |
| Git | 任意近期版本 | 克隆与协作 |

可选：Docker（与 `docker-compose.yml` 一键部署一致）。

## 本机最快闭环（H2，无需 MySQL）

1. **后端**  
   在 `backend` 目录执行 `run-dev-h2.bat`（Windows），或：

   ```bash
   mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=h2
   ```

   等待日志出现 `Started FinResearchApplication`，默认端口 **8080**。

2. **前端**  

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   浏览器打开 **http://localhost:5173** ，使用 **`demo` / `demo123456`** 登录。

3. **建议自测路径**  
   新建项目 → **文献与选题**（上传/解读、科研选题、时政选题）→ **数据与处理**（保存 EDC、伪数据模拟、上传 CSV、清洗/统计）→ **论文写作** → **投稿回修** → **合规与文档**。

## 云服务器与 Python/AkShare

- 统计分析可在云主机独立安装 Python、AkShare、Jupyter；详见前端 **数据与处理 → 统计分析环境（云服务器）** 文本框内注释。  
- 本平台**不**在仓库内执行远程 SSH 命令，仅提供文档化模板。

## AI 密钥（每人本机配置，勿提交仓库）

1. 在 [Anthropic Console](https://console.anthropic.com/) 创建 **API Key**。  
2. Windows：**系统环境变量** 新建 `ANTHROPIC_API_KEY` = 你的密钥，保存后**重启终端**，再启动后端。  
3. 或仅在本地使用 `backend/local-api.bat`（需自建，已列入 `.gitignore`，勿 push）：

```bat
@echo off
set "ANTHROPIC_API_KEY=你的密钥"
cd /d "%~dp0"
call mvn spring-boot:run "-Dspring-boot.run.profiles=h2"
```

未配置 Key 时仍可跑通界面，但论文生成等为**演示占位文案**。

---

## 放到 GitHub + 团队协作（操作清单）

### 负责人：第一次上传

1. 在 GitHub 网页 **New repository**，建空仓库（可先不勾选自动 README），记下 HTTPS 地址，如 `https://github.com/组织或用户名/finresearch.git`。  
2. 在本机进入**项目根目录**（含 `README.md`、`backend`、`frontend` 的那一层），执行：

```bash
git init
git add .
git status
```

确认列表里**没有** `node_modules`、`target`、`.env` 等敏感项。

```bash
git commit -m "chore: 初始提交"
git branch -M main
git remote add origin https://github.com/组织或用户名/finresearch.git
git push -u origin main
```

3. **私有仓库**：仓库 **Settings → Collaborators → Add people**，给对方 **Write**，对方在邮件/GitHub 里 **接受邀请**后才能 clone（私有）或 push。

### 成员：克隆并跑通一遍

1. 安装 **JDK 17、Maven、Node.js**（见上文「必备软件」）。  
2. 克隆并进入仓库根目录：

```bash
git clone https://github.com/组织或用户名/finresearch.git
cd finresearch
```

3. 后端：`cd backend` → 双击 **`run-dev-h2.bat`**（或 `mvn spring-boot:run "-Dspring-boot.run.profiles=h2"`），等到 **`Started FinResearchApplication`**。  
4. 新开终端：`cd frontend` → **`npm install`** → **`npm run dev`**。  
5. 浏览器打开 **http://localhost:5173**，登录 **demo / demo123456**。  
6. 每人自行配置 **`ANTHROPIC_API_KEY`**（见上一节），需要真实 AI 生成时再配即可。

### 日常协作（每人）

```bash
git pull
# 改代码…
git add .
git commit -m "feat: 说明改动"
git push
```

冲突时按 Git 提示改文件后再提交。

---

## 与 GitHub 协作（细则）

见 [GitHub团队协作.md](./GitHub团队协作.md)；勿提交 `node_modules`、`backend/target`、`.env`、`local-env.bat`、`local-api.bat` 等（见根目录 `.gitignore`）。
