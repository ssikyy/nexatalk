# NexaTalk 运维手册

这份手册对应当前这套单机 Docker 部署，默认服务器目录为 `/opt/nexatalk`。

## 运行环境

- 代码目录：`/opt/nexatalk`
- 编排文件：`docker-compose.prod.yml`
- 生产环境变量：`deploy/.env.prod`
- 入口域名：`https://sslovett.top`
- 网关：Caddy
- 业务服务：Spring Boot + MySQL + Redis

## 日常巡检

先进入项目目录：

```bash
cd /opt/nexatalk
```

一条命令看核心状态：

```bash
sh deploy/ops-status.sh
```

它会检查：

- 当前 Git 提交和分支
- 容器运行状态
- `https://域名/health` 健康检查
- 服务器磁盘使用情况
- Docker 磁盘占用

如果只想手工看容器：

```bash
docker compose --env-file deploy/.env.prod -f docker-compose.prod.yml ps
```

## 日志查看

看后端实时日志：

```bash
sh deploy/logs.sh backend
```

看网关实时日志：

```bash
sh deploy/logs.sh gateway
```

看最近 50 行日志：

```bash
TAIL_LINES=50 sh deploy/logs.sh backend
```

## 发布与更新

当前仓库已经接入 GitHub Actions 自动部署。

正常流程是：

1. 本地把改动推到 GitHub 指定分支。
2. GitHub Actions 通过 SSH 登录服务器。
3. 服务器执行 `deploy/update-on-server.sh`。
4. 脚本拉取最新代码并重建容器。

如果要手工部署某个分支：

```bash
cd /opt/nexatalk
APP_BRANCH=codex-gcp-docker-deploy sh deploy/update-on-server.sh
```

如果只是更新当前分支：

```bash
cd /opt/nexatalk
sh deploy/update-on-server.sh
```

## 重启

重启全部服务：

```bash
docker compose --env-file deploy/.env.prod -f docker-compose.prod.yml up -d
```

重建并重启：

```bash
docker compose --env-file deploy/.env.prod -f docker-compose.prod.yml up -d --build
```

仅重启后端：

```bash
docker compose --env-file deploy/.env.prod -f docker-compose.prod.yml restart backend
```

## 数据库备份

执行一次 MySQL 备份：

```bash
cd /opt/nexatalk
sh deploy/backup-mysql.sh
```

默认行为：

- 备份目录：`/opt/nexatalk/backups/mysql`
- 文件名格式：`nexatalk-YYYYmmdd-HHMMSS.sql.gz`
- 自动生成一个 `latest.sql.gz` 软链接
- 默认保留 7 天

如果想保留 14 天：

```bash
BACKUP_RETENTION_DAYS=14 sh deploy/backup-mysql.sh
```

## 数据库恢复

恢复前建议先停后端，避免业务写入：

```bash
docker compose --env-file deploy/.env.prod -f docker-compose.prod.yml stop backend
```

恢复最近一次备份：

```bash
gunzip -c backups/mysql/latest.sql.gz | docker compose --env-file deploy/.env.prod -f docker-compose.prod.yml exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"'
```

恢复完成后再启动后端：

```bash
docker compose --env-file deploy/.env.prod -f docker-compose.prod.yml start backend
```

如果是覆盖恢复，操作前最好先额外备份一次当前库。

## 回滚

先看最近提交：

```bash
git log --oneline -n 10
```

切到要回滚的提交或分支后重新构建：

```bash
git checkout <目标提交或分支>
docker compose --env-file deploy/.env.prod -f docker-compose.prod.yml up -d --build
```

如果你仍然使用自动部署，回滚后记得处理好 GitHub 上对应分支，避免后续又被推回最新版本。

## 常见故障

### 浏览器打开首页看到 Welcome to nginx

这通常不是当前 Docker 服务返回的页面，而是本地浏览器缓存、DNS 缓存，或者之前访问过旧站点。

优先排查：

- 用无痕窗口重新打开
- 强制刷新页面
- 看命令行健康检查是否正常：`curl -i https://sslovett.top/health`

### 首页能开，但 `/health` 或 `/api` 返回 502

先看后端是否启动成功：

```bash
sh deploy/logs.sh backend
```

再看容器状态：

```bash
sh deploy/ops-status.sh
```

这类问题大多是：

- 后端启动失败
- 数据库迁移失败
- `.env.prod` 缺变量或变量不正确

### GitHub Actions 自动部署失败

优先检查：

- GitHub Secrets 里的 `DEPLOY_SSH_KEY` 是否是完整私钥
- GitHub Variables 里的 `DEPLOY_HOST` 是否只填主机名或 IP，不要带 `http://`、`https://`、空格
- 服务器上的 `authorized_keys` 是否包含对应公钥
- 服务器仓库目录是否仍是 `/opt/nexatalk`

## 建议的例行操作

建议你至少保留下面这个节奏：

- 每次发版后执行一次 `sh deploy/ops-status.sh`
- 每周至少做一次数据库备份
- 每次涉及数据库结构改动后，额外验证 `/health`、登录、发帖、上传
- 定期看一下 `docker system df`，避免磁盘被镜像和日志吃满
