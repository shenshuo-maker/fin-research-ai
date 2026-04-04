# GitHub 团队协作：放哪些文件、怎么操作

## 一、要上传到 GitHub 的是什么？

上传的是整个 **`bg` 项目目录里的源代码与配置**（通过 Git 管理），**不是**把 `C:\` 整个盘传上去。

团队需要的是：**别人 `git clone` 下来能按文档跑起来的那一套**。

---

## 二、应该提交哪些文件？（会进仓库的）

| 类型 | 路径示例 | 说明 |
|------|----------|------|
| 后端源码 | `backend/src/**` | Java 代码 |
| 后端构建描述 | `backend/pom.xml`、`backend/Dockerfile` | Maven / 镜像 |
| 前端源码 | `frontend/src/**`、`frontend/index.html`、`frontend/vite.config.ts` 等 | Vue 工程 |
| 前端依赖清单 | `frontend/package.json`、`frontend/package-lock.json` | 别人 `npm ci` 可复现 |
| 部署与文档 | `docker-compose.yml`、`README.md`、`DEPLOY.md`、`docs/**` | 含本文件 |
| 脚本 | `backend/run-dev-h2.bat`、`scripts/**` 等 | 便于本机/运维 |
| 示例数据 | `sample_data/**` | 可选演示用 |
| Git 忽略规则 | **`.gitignore`**（在 `bg` 根目录） | **必须提交**，避免把垃圾进库 |

**原则：** 没有敏感信息、能代表「项目长什么样」的，都提交。

---

## 三、不要提交哪些？（已在 `.gitignore` 里）

以下由根目录 **`.gitignore`** 忽略，**不要** `git add -f` 强行加入：

| 忽略项 | 原因 |
|--------|------|
| `**/node_modules/` | 体积大，用 `npm install` 本地生成 |
| `**/target/` | Maven 编译产物，可重建 |
| `**/dist/` | 前端构建产物，可 `npm run build` |
| `.env` | 常含密钥，易泄露 |
| `*.log` | 本地日志 |
| `backend/local-env.bat` | 可能含本机数据库密码 |
| `backend/last-backend-run.log` | 运行日志 |
| `**/data/` | 本机上传的加密文件等 |

**注意：** 若你在 **`run-dev.bat`** 里写过**真实 MySQL 密码**，提交前改回占位或只用 **`local-env.bat`**（且 `local-env.bat` 已被忽略）。

---

## 四、第一次把项目放到 GitHub（负责人操作）

### 1. 在 GitHub 网页上

1. 登录 [GitHub](https://github.com)  
2. 右上角 **New repository**  
3. 填仓库名（如 `finresearch-mvp`），选 **Public** 或 **Private**  
4. **不要**勾选「用 README 初始化」（你本地已有代码时更简单）  
5. 点 **Create repository**

页面会提示一段地址，形如：

`https://github.com/你的用户名/finresearch-mvp.git`

### 2. 在你电脑上（进入 `bg` 根目录）

用 **命令提示符** 或 **PowerShell**，`cd` 到 **`bg` 文件夹**（里面有 `README.md`、`docker-compose.yml` 的那一层）：

```bash
git init
git add .
git status
```

看一眼 **`git status`**：确认没有 `node_modules`、`target`、`.env`、`local-env.bat` 等。

```bash
git commit -m "chore: 初始提交 金融科研 AI MVP"
git branch -M main
git remote add origin https://github.com/你的用户名/finresearch-mvp.git
git push -u origin main
```

第一次 `git push` 会要求登录 GitHub（浏览器或 Token，按 GitHub 提示即可）。

### 3. 第一次 push 登不上去时

- Windows 上通常会用 **Git Credential Manager**，弹出浏览器登录 GitHub 即可。  
- 若要求输入密码：在 GitHub 网页 **Settings → Developer settings → Personal access tokens** 生成 **Token**，在命令行里把 Token **当作密码**粘贴（不要填登录密码）。

### 4. 让团队成员能打开仓库（网页 + clone）

| 仓库类型 | 你需要在 GitHub 上做的事 |
|----------|-------------------------|
| **Public** | 把仓库链接发给同事即可 **clone**；若要让他们 **push**，仍要到下面「邀请协作者」。 |
| **Private** | 必须邀请：打开仓库 → **Settings** → **Collaborators**（或 **Manage access**）→ **Add people** → 输入对方 GitHub 用户名 → 对方在通知里 **Accept**。 |

邀请时可选择权限：**Read**（只读代码）、**Write**（可 push）、**Admin**（管理设置）。一般开发给 **Write**。

---

## 五、团队成员怎么参与？

### 1. 克隆仓库

```bash
git clone https://github.com/你的用户名/finresearch-mvp.git
cd finresearch-mvp
```

`cd` 进去后应能直接看到 **`README.md`**、`backend`、`frontend`（若你是在 **`bg` 根目录** 执行的 `git init`，则克隆下来的**仓库根目录就是项目根**，**不要**再多套一层 `bg`）。

（若仓库在组织下，URL 换成组织给的地址。）

### 2. 日常改代码后提交（每人）

```bash
git pull
# 编辑代码…
git add .
git status
git commit -m "feat: 描述你改了什么"
git push
```

- **`git pull`**：先把远程最新代码拉下来，减少冲突。  
- 冲突时 Git 会提示，按提示改文件后再 `commit` / `push`。

### 3. 本机完整跑一遍（每人照做即可）

**先装环境（只做一次）**

- **JDK 17**、**Maven**（命令行执行 `java -version`、`mvn -v` 正常）  
- **Node.js** LTS（`node -v`、`npm -v` 正常）

**Windows（推荐 H2，不用本机装 MySQL）**

1. **8080 被占用时**：在 `backend` 目录运行 **`free-port-8080.bat`**（必要时管理员运行）。  
2. 在 **`backend`** 目录双击或运行 **`run-dev-h2.bat`**，等到控制台出现 **`Started FinResearchApplication`**，**窗口不要关**。  
3. **再开一个**终端：`cd frontend` → **`npm install`** → **`npm run dev`**。  
4. 浏览器打开 **http://localhost:5173**，登录 **demo / demo123456**。

**Mac / Linux（无 .bat 时）**：在 `backend` 目录执行  
`mvn spring-boot:run "-Dspring-boot.run.profiles=h2"`，前端步骤同上。

更细的说明见 **`README.md`**、**`docs/本机预览与GitHub协作.md`**。若要用本机 MySQL 或 Docker，见 **`README.md`** 与 **`docs/云服务器部署步骤.md`**。

### 4. 服务器部署（运维/负责人）

见 **`docs/云服务器部署步骤.md`**、`DEPLOY.md`：

```bash
docker compose up -d --build
```

---

## 六、协作小建议

1. **分支**：大功能可用 `git checkout -b feature/xxx`，做完再合并到 `main`（可用 GitHub Pull Request）。  
2. **密钥**：`ANTHROPIC_API_KEY`、`JWT_SECRET` 放在服务器环境变量或私有 `.env`，**不进仓库**；仓库里只保留 **`.env.example`**（无真实密钥）。  
3. **大文件**：不要把 PDF 数据集、整库备份塞进 Git；用网盘或对象存储链接说明即可。

---

## 七、对照清单（提交前 30 秒自查）

- [ ] `git status` 里没有 `node_modules`、`target`、`.env`  
- [ ] 没有 `backend/local-env.bat`  
- [ ] `run-dev.bat` 里没有真实数据库密码  
- [ ] 已包含 **`package-lock.json`**（前端锁版本，团队一致）

按上述操作即可在 **不泄露本机密码** 的前提下，把项目放到 GitHub 供团队克隆与部署。
