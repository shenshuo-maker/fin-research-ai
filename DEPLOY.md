# 部署说明（Docker + 云服务器）

**想直接上云、不再折腾本机 MySQL**：请按 **[docs/云服务器部署步骤.md](./docs/云服务器部署步骤.md)** 从零操作（含安全组、Docker 安装、`docker compose up`）。

---

## 前置条件

- 已安装 [Docker](https://docs.docker.com/get-docker/) 与 Docker Compose v2。
- 云服务器建议：**2 vCPU / 4GB RAM** 及以上；开放 **80**（或自定义）端口。
- 若需真实 AI 输出：准备 `ANTHROPIC_API_KEY`。

## 一键启动（本机或服务器）

在项目根目录执行：

```bash
cp .env.example .env   # 若已提供示例环境文件；否则可直接设置环境变量
docker compose up -d --build
```

首次启动 MySQL 需约 30 秒完成初始化；后端在 `mysql` 健康检查通过后才启动。

访问：

- **前端**：`http://<服务器IP>` 或 `http://localhost`
- **API 文档**（经 Nginx 转发）：`http://<服务器IP>/swagger-ui/index.html`
- **直连后端**（调试）：`http://<服务器IP>:8080/swagger-ui/index.html`

## 环境变量

可在 shell 或 `docker-compose.yml` 同目录的 `.env` 中设置：

| 变量 | 说明 |
|------|------|
| `JWT_SECRET` | JWT 签名密钥（须足够长且随机） |
| `ANTHROPIC_API_KEY` | Claude API Key；留空则走演示占位响应 |
| `ANTHROPIC_MODEL` | 模型名，默认 `claude-sonnet-4-20250514` |

数据持久化卷：`mysql_data`（数据库）、`backend_data`（上传加密文件）。

## HTTPS（推荐）

在服务器前放置 **Nginx / Caddy / 云负载均衡**，终止 TLS，并将流量反代到本 compose 的 `frontend:80`。证书可使用 Let’s Encrypt。

## 升级与备份

- **升级镜像**：`git pull` 后执行 `docker compose up -d --build`。
- **备份 MySQL**：`docker compose exec mysql mysqldump -uroot -proot finresearch > backup.sql`

## 故障排查

- 后端无法连库：确认 `mysql` 容器 `healthy`，检查 `MYSQL_*` 环境变量。
- 前端 502：确认 `backend` 已启动且 `frontend` 的 `nginx.conf` 中 `proxy_pass` 指向 `backend:8080`。
- AI 无真实内容：检查 `ANTHROPIC_API_KEY` 是否注入后端容器（`docker compose exec backend env | findstr ANTHROPIC`）。
