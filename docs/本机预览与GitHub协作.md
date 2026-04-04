# 本机打开前端跑通 + 上传 GitHub 给团队部署

## 一、本机最快跑通（推荐：不依赖本机 MySQL）

### 1. 释放 8080（若之前启动过后端）

在 `backend` 目录双击 **`free-port-8080.bat`**（必要时右键管理员运行）。

### 2. 启动后端（H2，无需 MySQL）

在 `backend` 目录双击 **`run-dev-h2.bat`**，等到出现 **`Started FinResearchApplication`**，**窗口不要关**。

若启动后马上退出并出现 **`Table "users" not found`**：请更新到最新代码；已在 `application-h2.yml` 中覆盖为 **`H2Dialect`**（否则会沿用主配置里的 MySQL 方言，H2 上建表失败）。

### 3. 启动前端

新开一个终端：

```bash
cd frontend
npm install
npm run dev
```

### 4. 浏览器

打开 **http://localhost:5173** → 登录 **demo / demo123456** → 新建项目 → 各页签点一遍完成闭环。

---

## 二、推送到 GitHub 前检查（避免泄露密码）

1. **不要提交** `backend/local-env.bat`（已在 `.gitignore`）。  
2. 若你改过 **`run-dev.bat`** 并写入了真实 MySQL 密码，推前改回占位或改用 `local-env.bat`。  
3. 根目录 **`.env`** 若含密钥，已在 `.gitignore`，勿强行 `git add -f`。  
4. 执行 `git status`，确认没有 `*.log`、`node_modules`、`target` 等。

---

## 三、首次上传到 GitHub

在 **`bg` 项目根目录**（含 `README.md`、`docker-compose.yml` 的那一层）：

```bash
git init
git add .
git commit -m "feat: 金融科研 AI MVP 初版"
```

在 GitHub 网页新建 **空仓库**（不要勾选自动添加 README），然后：

```bash
git remote add origin https://github.com/你的组织或用户名/仓库名.git
git branch -M main
git push -u origin main
```

---

## 四、团队成员如何部署

| 场景 | 做法 |
|------|------|
| **本机快速看效果** | 同本文「第一节」：`run-dev-h2.bat` + `npm run dev` |
| **本机 + 完整 MySQL** | 见根目录 `README.md` 与 `backend/local-env.bat.example` |
| **服务器一键上线** | **[云服务器部署步骤.md](./云服务器部署步骤.md)** 或根目录 **DEPLOY.md**，执行 `docker compose up -d --build` |

团队克隆后（**仓库根目录**若已包含 `README.md` 与 `docker-compose.yml`，则 `cd` 进仓库名即可，**不要**多写一层 `bg`）：

```bash
git clone https://github.com/xxx/xxx.git
cd xxx
docker compose up -d --build
```

浏览器访问 **`http://服务器IP`** 即可（需云安全组放行 80）。

---

## 五、给团队的一句说明（可贴进 README）

> 克隆仓库后，本地演示：后端 `backend/run-dev-h2.bat`，前端 `frontend` 下 `npm run dev`，打开 http://localhost:5173 。  
> 服务器部署：`docker compose up -d --build`，详见 `docs/云服务器部署步骤.md`。
