# NexaTalk Docker 部署到 GCP

这套部署文件面向单台 GCP 虚拟机，推荐用在你当前的 `e2-standard-2` 机器上。

## 当前部署结构

- `gateway`：Caddy，负责 `HTTPS`、静态前端、反代 `/api` `/ws` `/uploads` `/health`
- `backend`：Spring Boot 后端
- `mysql`：MySQL 8
- `redis`：Redis 7

## 仓库里新增的文件

- `docker-compose.prod.yml`
- `deploy/.env.prod.example`
- `deploy/Caddyfile`
- `deploy/update-on-server.sh`
- `deploy/ops-status.sh`
- `deploy/logs.sh`
- `deploy/backup-mysql.sh`
- `server/server-api/Dockerfile`
- `web/Dockerfile`
- `OPS_RUNBOOK.md`

## 首次部署前准备

1. 把服务器系统准备好 Docker 和 Docker Compose。
2. 确保 GCP 防火墙放行 `22`、`80`、`443`。
3. 确保你的域名已经解析到服务器公网 IP。

## 服务器上的部署目录

推荐把仓库放到：

```bash
/opt/nexatalk
```

## 首次部署命令

```bash
cd /opt/nexatalk
cp deploy/.env.prod.example deploy/.env.prod
```

然后编辑：

```bash
vim deploy/.env.prod
```

至少需要改这些值：

- `SITE_ADDRESS`
- `ACME_EMAIL`
- `MYSQL_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `REDIS_PASSWORD`
- `JWT_SECRET`
- `AI_API_KEY`（如果你要启用 AI）

启动：

```bash
docker compose --env-file deploy/.env.prod -f docker-compose.prod.yml up -d --build
```

查看状态：

```bash
docker compose --env-file deploy/.env.prod -f docker-compose.prod.yml ps
docker compose --env-file deploy/.env.prod -f docker-compose.prod.yml logs -f gateway
docker compose --env-file deploy/.env.prod -f docker-compose.prod.yml logs -f backend
```

## 数据持久化

下面这些数据会保留在 Docker volume 里：

- MySQL 数据
- Redis 持久化数据
- 上传文件
- 后端日志
- Caddy 证书与配置

## 更新发布

如果服务器上是这个仓库的 git 工作副本，可以直接执行：

```bash
sh deploy/update-on-server.sh
```

这个脚本会：

1. 拉取最新代码
2. 重新构建镜像
3. 重启生产容器
4. 清理悬空镜像

## 自动更新

仓库里已经提供 GitHub Actions 工作流：

- `.github/workflows/deploy-gcp.yml`

它会在以下情况自动部署到服务器：

- push 到 `main`
- push 到 `codex-gcp-docker-deploy`
- 手工触发并指定分支

### 你需要在 GitHub 仓库配置这些 Variables

- `DEPLOY_HOST`：服务器 IP 或域名
- `DEPLOY_USER`：服务器登录用户
- `DEPLOY_PORT`：SSH 端口，默认 `22`
- `DEPLOY_APP_DIR`：服务器部署目录，默认 `/opt/nexatalk`

### 你需要在 GitHub 仓库配置这个 Secret

- `DEPLOY_SSH_KEY`：用于登录服务器的私钥内容

### 服务器侧要做的一次性准备

1. 生成一对部署专用 SSH 密钥。
2. 把公钥追加到服务器用户的 `~/.ssh/authorized_keys`。
3. 把私钥内容保存到 GitHub Secret `DEPLOY_SSH_KEY`。

示例：

```bash
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/nexatalk_deploy
cat ~/.ssh/nexatalk_deploy.pub
```

把上面的公钥追加到服务器：

```bash
mkdir -p ~/.ssh
chmod 700 ~/.ssh
cat >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

工作流执行时，会在服务器上运行：

```bash
APP_DIR=/opt/nexatalk APP_BRANCH=<当前分支> sh /opt/nexatalk/deploy/update-on-server.sh
```

## 验证点

- 首页是否能通过 `https://你的域名` 打开
- `https://你的域名/health` 是否返回后端健康响应
- 登录、发帖、图片上传、WebSocket 私信是否正常

## 下一步建议

完成首次手工部署后，再接一层自动更新：

- 方案 A：GitHub Actions 通过 SSH 调用 `deploy/update-on-server.sh`
- 方案 B：本地手工 SSH 到服务器执行这个脚本

如果你愿意，下一步我就可以继续带你做 GCP 服务器初始化和首发部署命令。

补充：日常运维请优先查看 `OPS_RUNBOOK.md`。
