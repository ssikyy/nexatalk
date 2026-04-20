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
- `server/server-api/Dockerfile`
- `web/Dockerfile`

## 首次部署前准备

1. 把服务器系统准备好 Docker 和 Docker Compose。
2. 确保 GCP 防火墙放行 `22`、`80`、`443`。
3. 确保域名 `sslovett.top` 已解析到服务器公网 IP。

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

## 验证点

- 首页是否能通过 `https://sslovett.top` 打开
- `https://sslovett.top/health` 是否返回后端健康响应
- 登录、发帖、图片上传、WebSocket 私信是否正常

## 下一步建议

完成首次手工部署后，再接一层自动更新：

- 方案 A：GitHub Actions 通过 SSH 调用 `deploy/update-on-server.sh`
- 方案 B：本地手工 SSH 到服务器执行这个脚本

如果你愿意，下一步我就可以继续带你做 GCP 服务器初始化和首发部署命令。
