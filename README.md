# NexaTalk

## 1. 项目简介

NexaTalk 是一个基于 `Spring Boot + Vue 3` 的信息交流平台，定位为一个集内容发布、互动讨论、私信沟通、系统通知与后台管理于一体的社区类项目。

项目采用前后端分离架构：

- 前端使用 `Vue 3 + Vite + Pinia + Vue Router + Element Plus`
- 后端使用 `Spring Boot + MyBatis-Plus + MySQL + Redis + JWT + WebSocket`
- 文件上传采用本地磁盘存储
- 数据库版本管理采用 `Flyway`

本项目不仅实现了社区平台的基础业务流程，也补充了面向演示与答辩更重要的内容，包括：

- 登录鉴权与管理员权限控制
- 实时私信与输入状态提示
- 通知系统与后台系统通知发布
- 举报、处罚、黑名单、操作日志等后台治理能力
- 图片上传、个人资料维护、关注关系、搜索功能
- AI 能力配置与相关业务入口

---

## 2. 项目目标

本项目的目标是完成一个具有完整业务闭环的社区交流平台，并在设计层面体现以下能力：

- 具备较完整的前后端工程化结构
- 支持真实的用户登录、发帖、评论、互动、消息和通知流程
- 具备一定的安全性、可维护性和可扩展性
- 具备管理员治理能力，便于系统运营和演示答辩
- 能够通过较规范的验收与测试流程证明系统可用性

---

## 3. 主要功能模块

### 3.1 用户与认证模块

- 用户注册、登录、退出登录
- JWT 身份认证
- Redis 登录态校验，支持主动失效 token
- 个人资料查看与编辑
- 头像、背景图上传
- 普通用户、管理员、超级管理员角色区分

### 3.2 社区内容模块

- 帖子发布、编辑、删除
- 帖子图片上传
- 帖子详情查看
- 评论与回复
- 点赞、收藏、分享计数
- 分区管理与帖子分类

### 3.3 社交关系模块

- 关注 / 取消关注
- 粉丝 / 关注列表查看
- 用户主页
- 用户发帖列表

### 3.4 通知模块

- 我的通知列表
- 未读通知统计
- 通知已读处理
- 通知详情查看
- 后台系统通知发布、编辑、删除、置顶

### 3.5 私信与实时通信模块

- 私信发送与会话管理
- WebSocket 实时消息推送
- 正在输入状态提示
- 会话已读处理
- 消息相关状态同步

### 3.6 搜索与辅助模块

- 用户、帖子等内容搜索
- Feed 流接口
- 健康检查、Ping 接口

### 3.7 后台管理模块

- 用户管理
- 举报审核
- 处罚管理
- 分区管理
- 系统通知管理
- 操作日志查看
- 系统配置管理
- AI 管理页

---

## 4. 技术栈

### 4.1 后端技术栈

- `Java 17`
- `Spring Boot 3.5.9`
- `MyBatis-Plus`
- `MySQL`
- `Redis`
- `JWT`
- `Spring WebSocket`
- `Flyway`
- `Spring AOP`
- `Knife4j / SpringDoc`

### 4.2 前端技术栈

- `Vue 3`
- `Vite 5`
- `Pinia`
- `Vue Router`
- `Axios`
- `Element Plus`
- `SockJS + STOMP`
- `Tiptap`
- `md-editor-v3`

### 4.3 工程特点

- 前后端分离
- 路由懒加载
- 统一响应结构
- 统一异常处理
- 基于注解的鉴权与参数注入
- 本地磁盘上传资源映射
- 支持数据库迁移脚本管理

---

## 5. 系统架构说明

### 5.1 总体架构

```text
+-------------------+        HTTP / WebSocket        +----------------------+
|   Vue 3 前端应用   |  <-------------------------->  |  Spring Boot 后端服务 |
+-------------------+                                 +----------------------+
         |                                                         |
         |                                                         |
         v                                                         v
  本地浏览器存储                                            MySQL / Redis / 本地上传目录
  Token / 用户信息                                           数据 / 登录态 / 图片资源
```

### 5.2 后端分层结构

```text
Controller  ->  Service  ->  Mapper  ->  MySQL
     |             |
     |             +---- Redis / WebSocket / 文件上传 / AI 配置
     |
     +---- DTO / VO / Exception / Security / Aspect / Config
```

### 5.3 前端结构

```text
views 页面
  -> api 接口层
  -> stores 状态管理
  -> utils 请求/WebSocket/会话工具
  -> components 公共组件
```

---

## 6. 项目目录结构

### 6.1 根目录

```text
NexaTalk/
├── server/                 后端工程
│   └── server-api/
├── web/                    前端工程
├── uploads/                上传文件目录
├── README.md               项目总说明文档
└── 项目目录结构.md          目录结构补充说明
```

### 6.2 后端目录

```text
server/server-api/
├── src/main/java/com/ttikss/nexatalk/
│   ├── aspect/             请求日志切面
│   ├── common/             统一响应与错误码
│   ├── config/             MVC / WebSocket / MyBatis / JWT 等配置
│   ├── controller/         控制器层
│   ├── dto/                请求对象
│   ├── entity/             实体类
│   ├── exception/          异常处理
│   ├── mapper/             数据访问层
│   ├── security/           JWT、用户上下文、权限注解
│   ├── service/            业务接口
│   ├── service/impl/       业务实现
│   ├── util/               文件与日志等工具类
│   └── vo/                 返回对象
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   ├── db/migration/       Flyway 迁移脚本
│   └── mapper/             MyBatis XML
└── src/test/java/          后端测试代码
```

### 6.3 前端目录

```text
web/
├── src/
│   ├── api/                接口封装
│   ├── components/         公共组件
│   ├── layouts/            页面布局
│   ├── router/             路由配置
│   ├── stores/             Pinia 状态管理
│   ├── styles/             全局样式
│   ├── utils/              request / websocket / session 工具
│   └── views/
│       ├── admin/          管理后台页面
│       ├── auth/           登录注册
│       ├── home/           首页
│       ├── messages/       私信页
│       ├── notifications/  通知页
│       ├── post/           帖子页
│       ├── search/         搜索页
│       ├── user/           用户主页与设置
│       └── error/          错误页
├── package.json
└── vite.config.js
```

---

## 7. 核心后端接口模块

当前后端控制器包括：

- `UserController`：用户注册、登录、资料、头像、背景图、管理员用户管理
- `PostController`：帖子相关业务
- `CommentController`：评论与回复
- `LikeController`：点赞
- `FavoriteController`：收藏
- `FollowController`：关注、粉丝、关注状态
- `MessageController`：私信与会话
- `NotificationController`：通知与系统通知
- `SearchController`：搜索
- `FeedController`：信息流
- `BlacklistController`：黑名单
- `ReportController`：举报
- `PunishmentController`：处罚
- `SectionController`：分区管理
- `OperationLogController`：操作日志
- `SystemConfigController`：系统配置
- `AiController`：AI 相关功能
- `WebSocketController`：实时消息与 typing 事件
- `HealthController / PingController`：健康检查

---

## 8. 核心前端页面

前端页面主要包括：

- 认证页：登录、注册
- 社区页：首页、帖子详情、发帖、编辑帖子
- 用户页：个人主页、关注列表、粉丝列表、用户帖子、编辑资料、设置
- 通知页：通知列表、通知详情
- 私信页：消息会话与实时聊天
- 搜索页
- 后台页：用户管理、举报管理、处罚管理、通知管理、日志管理、分区管理、系统管理、AI 管理

---

## 9. 数据库与迁移脚本

项目通过 `Flyway` 管理数据库版本，迁移脚本位于：

`server/server-api/src/main/resources/db/migration`

当前已经包含的迁移内容包括：

- 初始化核心表结构
- 用户字段补充
- 操作日志表
- 用户背景图字段
- 黑名单表
- 操作日志索引优化
- 默认超级管理员设置
- 帖子图片字段
- 默认分区插入
- 通知系统字段扩展
- 系统配置表
- 帖子标签与分享计数
- 消息类型与撤回支持
- AI 用户与置顶通知
- 通知广播 ID

核心业务表包括但不限于：

- `user`
- `post`
- `comment`
- `message`
- `conversation`
- `notification`
- `follow`
- `favorite`
- `like`
- `blacklist`
- `report`
- `punishment`
- `section`
- `operation_log`
- `system_config`

---

## 10. 环境要求

### 10.1 开发环境建议

- JDK `17`
- Node.js `18+`
- npm `9+`
- MySQL `8.x`
- Redis `6.x` 或以上
- Maven `3.9+` 或直接使用项目自带 `mvnw`

### 10.2 端口约定

- 前端开发服务：`3000`
- 后端服务：`8080`
- MySQL：`3306`
- Redis：`6379`

---

## 11. 配置说明

### 11.1 核心配置文件

- 公共配置：`server/server-api/src/main/resources/application.yml`
- 开发环境：`server/server-api/src/main/resources/application-dev.yml`
- 生产环境：`server/server-api/src/main/resources/application-prod.yml`

### 11.2 常用环境变量

| 变量名 | 说明 |
|------|------|
| `SPRING_PROFILES_ACTIVE` | 当前运行环境，开发建议 `dev`，生产使用 `prod` |
| `DB_URL` | MySQL 连接地址 |
| `DB_USERNAME` | MySQL 用户名 |
| `DB_PASSWORD` | MySQL 密码 |
| `REDIS_HOST` | Redis 主机 |
| `REDIS_PORT` | Redis 端口 |
| `REDIS_PASSWORD` | Redis 密码 |
| `JWT_SECRET` | JWT 密钥 |
| `APP_UPLOAD_PATH` | 上传文件存储目录 |
| `APP_UPLOAD_PREFIX` | 上传资源访问前缀 |
| `AI_ENABLED` | 是否启用 AI 功能 |
| `AI_BASE_URL` | AI 服务地址 |
| `AI_API_KEY` | AI 密钥 |
| `AI_MODEL` | AI 模型名 |

### 11.3 当前配置治理策略

- 开发环境数据库账号密码改为环境变量读取
- 生产环境默认关闭接口文档
- 生产环境必须显式配置 `JWT_SECRET`
- 上传目录默认使用相对路径 `./uploads`

---

## 12. 项目启动方式

### 12.1 启动后端

```bash
cd /Users/tuotuo/IdeaProjects/NexaTalk/server/server-api
./mvnw spring-boot:run
```

### 12.2 启动前端

```bash
cd /Users/tuotuo/IdeaProjects/NexaTalk/web
npm install
npm run dev
```

### 12.3 访问地址

- 前端首页：`http://localhost:3000`
- 后端接口：`http://localhost:8080`
- 开发环境接口文档：`http://localhost:8080/doc.html`

---

## 13. 已完成的安全与优化改进

本项目在原有基础上完成了以下重要改进：

### 13.1 鉴权与权限收口

- 管理员接口由拦截器统一识别 `@RequireAdmin`
- 未登录访问管理员接口返回 `401`
- 普通用户访问管理员接口返回 `403`
- token 除 JWT 校验外，还增加 Redis 登录态校验

### 13.2 WebSocket 安全加固

- WebSocket 建连时校验 `Bearer Token`
- 校验 Redis 中 token 是否仍有效
- typing 事件不再信任前端传入的 `senderId`
- typing 事件会校验会话参与者与接收方是否匹配
- 用户登出后，旧 WebSocket 会话会被拒绝继续使用

### 13.3 通知链路修复

- 新增普通用户可访问的通知详情接口
- 通知详情页不再错误调用管理员接口
- 打开通知详情时可自动完成已读更新

### 13.4 上传安全增强

- 上传目录改为读取配置
- 仅允许上传 `JPG / PNG / GIF / WEBP`
- 校验文件大小、MIME 类型、扩展名
- 非法上传统一返回可读的 4xx 错误

### 13.5 日志脱敏与异步化

- 操作日志对密码、token、密钥等敏感信息脱敏
- 日志持久化改为真正可生效的异步写入
- 修复模块提取逻辑，便于后台日志分类查看

### 13.6 前端状态一致性优化

- 抽离统一会话清理逻辑
- 401 时同步清理本地 token、用户状态和 WebSocket
- 登出后清空消息页内存状态，避免旧数据残留

### 13.7 性能优化

- 关注/粉丝列表由后端批量返回 `isFollowing`
- 去除前端逐条请求关注状态的 N+1 问题
- 富文本、Markdown、实时通信等依赖做了拆包优化

---

## 14. 验收方案

本项目验收建议从四个维度进行：

- 环境与构建验收
- 业务功能验收
- 安全性验收
- 性能与稳定性验收

### 14.1 环境与构建验收

#### 验收项

- 后端能够正常编译
- 前端能够正常构建
- 前后端能够正常启动
- MySQL、Redis 连接正常

#### 验收命令

```bash
cd /Users/tuotuo/IdeaProjects/NexaTalk/server/server-api
./mvnw -DskipTests test-compile
```

```bash
cd /Users/tuotuo/IdeaProjects/NexaTalk/web
npm run build
```

#### 预期结果

- 后端编译成功，无阻塞性报错
- 前端构建成功，页面资源正常生成到 `dist/`

### 14.2 业务功能验收

| 功能模块 | 验收内容 | 预期结果 |
|------|------|------|
| 用户模块 | 注册、登录、退出 | 功能正常，登录后可进入受限页面 |
| 用户资料 | 修改昵称、头像、背景图、简介 | 数据更新成功并正确显示 |
| 帖子模块 | 发帖、编辑、查看详情 | 页面与数据正常 |
| 评论模块 | 评论、回复、删除 | 评论关系正确 |
| 点赞收藏 | 点赞、取消点赞、收藏、取消收藏 | 状态切换正常 |
| 关注模块 | 关注、取关、查看粉丝/关注列表 | 按钮状态与列表数据一致 |
| 通知模块 | 查看通知列表、详情、已读 | 未读数与详情状态同步 |
| 私信模块 | 发送消息、查看会话 | 消息正常到达并展示 |
| 后台模块 | 用户、举报、处罚、通知、日志管理 | 管理员可正常访问与操作 |

### 14.3 安全性验收

| 验收项 | 验收步骤 | 预期结果 |
|------|------|------|
| 管理员接口未登录访问 | 直接请求 `/api/admin/notifications` | 返回 `401` |
| 普通用户访问管理员接口 | 普通用户 token 请求管理员接口 | 返回 `403` |
| 普通用户通知详情 | 普通用户访问 `/api/notifications/{id}` | 只能看到自己的通知 |
| 越权访问通知 | 访问其他用户通知 ID | 返回 `404` 或不可访问 |
| WebSocket 失效控制 | 用户登出后继续发送 typing | 不再生效 |
| 非法文件上传 | 上传 `.svg`、`.html` 等 | 返回 4xx，文件不落盘暴露 |
| 日志脱敏 | 登录、改密等接口后查看日志 | 不出现明文密码、token |

### 14.4 性能与稳定性验收

| 验收项 | 验收方法 | 预期结果 |
|------|------|------|
| 关注列表请求数量 | 打开关注/粉丝页观察浏览器 Network | 不再出现大量逐条 `isFollowing` 请求 |
| 前端构建包体 | 执行 `npm run build` | 关键页面已拆分为独立 chunk |
| WebSocket 稳定性 | 登录、聊天、退出重复操作 | 不出现明显的旧状态残留 |
| 上传目录管理 | 上传、替换、删除图片 | 文件路径与删除行为正常 |

---

## 15. 测试方案

本项目测试采用“自动化测试 + 手工回归测试”结合的方式。

### 15.1 后端自动化测试

已补充的关键测试包括：

- `JwtAuthInterceptorTest`
  - 验证管理员接口的 `401 / 403 / 200` 鉴权行为
- `WebSocketAuthInterceptorTest`
  - 验证 WebSocket 建连时 Redis 登录态校验
- `FileUtilsTest`
  - 验证非法图片类型拒绝上传、合法图片上传与删除
- `LogSanitizerTest`
  - 验证密码、token、apiKey 等敏感字段脱敏
- `NotificationServiceImplTest`
  - 验证通知详情越权保护与已读更新逻辑

测试文件位于：

```text
server/server-api/src/test/java/com/ttikss/nexatalk/
```

### 15.2 后端测试执行方式

首次联网环境下建议执行：

```bash
cd /Users/tuotuo/IdeaProjects/NexaTalk/server/server-api
./mvnw test
```

如果当前机器处于离线环境，至少执行：

```bash
cd /Users/tuotuo/IdeaProjects/NexaTalk/server/server-api
./mvnw -o -DskipTests test-compile
```

说明：

- `test` 用于执行单元测试和集成测试
- `test-compile` 用于验证测试代码是否可编译
- 首次运行 `mvn test` 需要联网下载 Maven 测试插件与依赖

### 15.3 前端测试方式

当前前端项目未单独引入 `Vitest` 等自动化测试框架，因此以手工回归测试为主，重点覆盖：

- 登录态跳转
- 通知详情访问
- 消息页会话切换
- 401 失效跳转
- 关注/粉丝列表展示与按钮状态
- 发帖与编辑帖子的主流程

### 15.4 手工回归测试建议步骤

#### 场景 1：登录与会话清理

1. 使用普通用户登录系统
2. 访问通知页、消息页、设置页
3. 让 token 失效或执行退出登录
4. 再次触发任意接口请求

预期结果：

- 页面跳回登录页
- 本地登录状态被清除
- WebSocket 断开
- 私信旧会话数据不再保留

#### 场景 2：通知详情链路

1. 管理员发布系统通知
2. 普通用户进入通知列表
3. 点击未读通知进入详情
4. 返回通知列表再次查看状态

预期结果：

- 详情页可正常打开
- 该通知被标记为已读
- 普通用户不会再请求管理员接口

#### 场景 3：WebSocket typing 安全

1. 用户 A 与用户 B 分别登录
2. 打开消息页建立对话
3. A 输入文字，B 观察 typing 提示
4. A 退出登录后不刷新原页面，再尝试发送 typing

预期结果：

- 登录状态下 typing 可正常接收
- 退出登录后 typing 不再生效

#### 场景 4：上传安全

1. 上传合法图片文件
2. 上传非法文件，如 `.svg`、`.html`
3. 检查页面提示与上传目录

预期结果：

- 合法图片上传成功
- 非法文件被拒绝
- 返回明确错误提示
- 非法文件不会以静态资源形式暴露

#### 场景 5：关注列表优化

1. 打开某个用户的关注列表或粉丝列表
2. 打开浏览器开发者工具的 Network 面板
3. 观察请求数量

预期结果：

- 列表接口一次返回关注状态
- 不再出现逐条 `isFollowing` 请求

---

## 16. 验收判定标准

若满足以下条件，可判定项目达到当前阶段的验收要求：

- 后端可正常编译并启动
- 前端可正常构建并启动
- 核心业务流程可正常运行
- 管理员与普通用户权限边界正确
- 非法上传被拒绝
- 登录失效后前端状态清理完整
- WebSocket 在登录态失效后无法继续滥用
- 通知详情链路、关注列表、消息页等关键页面回归正常

---

## 17. 项目亮点总结

与普通课程作业相比，本项目的亮点主要体现在：

- 不只是完成基础 CRUD，而是实现了社区平台的完整交互闭环
- 具备实时消息、通知系统、后台治理和 AI 能力入口
- 在原项目基础上进一步补足了鉴权、安全、上传、日志和状态一致性问题
- 引入了数据库迁移、统一异常处理、日志切面、构建拆包等工程化能力
- 可以通过较清晰的验收与测试方案证明系统质量

---

## 18. 后续可扩展方向

如果继续完善，本项目还可以从以下方向扩展：

- 引入前端自动化测试框架
- 为后端补充更多集成测试
- 将上传能力迁移到对象存储
- 增加消息已送达 / 已读回执
- 完善搜索能力与推荐能力
- 优化大体积依赖的进一步拆包
- 引入 Docker Compose 一键部署

---

