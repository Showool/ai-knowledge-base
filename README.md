# AI Knowledge Base

独立的通用多轮文本对话服务，基于 Spring Boot 4、Spring AI 2.0、OpenAI、MySQL 和 Redis Stack 构建。

## 技术栈

- **Java 21** + **Spring Boot 4.1**
- **Spring AI 2.0**（OpenAI 集成）
- **MySQL 8.0**（utf8mb4，MyBatis-Plus）
- **Redis Stack 7+**（RedisJSON + Query Engine）
- **Spring Security** + JWT 认证
- **Knife4j**（静态 OpenAPI 契约）

## 功能概览

- 用户名注册、登录、Token 刷新与退出（BCrypt 密码哈希）
- 单账号单登录：新登录、退出和停用均注销全部认证会话
- 会话创建、查询、软删除，完整消息历史分页
- 真正流式转发 OpenAI 增量文本的 SSE 对话接口
- 全局 FIFO 队列、模型并发控制、JVM 请求注册表、Redis 单用户互斥锁
- 请求准入序列：认证 → 参数校验 → 无意义请求拦截 → 容量预留 → 单用户锁 → 额度扣减 → 入队
- 注册额度初始为 0，请求进入队列即扣 1，失败/取消/断开不退款
- 确定性无意义请求规则 + MySQL 短语管理 + Redis 短语缓存
- USER / ADMIN 双角色，管理员可管理用户状态和额度

## 环境要求

| 组件 | 版本要求 |
|------|----------|
| JDK | 21+ |
| MySQL | 8.0+（utf8mb4） |
| Redis | Redis Stack 7+（需 RedisJSON） |
| Maven | 3.8+ |

## 快速开始

### 1. 初始化数据库

```sql
mysql -u root -p < sql/init.sql
```

### 2. 配置环境变量

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/ai_knowledge_base?useUnicode=true&characterEncoding=utf8mb4"
$env:DB_USER="root"
$env:DB_PASSWORD="your_password"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
$env:REDIS_PASSWORD="your_redis_password"
$env:JWT_SECRET="your-jwt-secret-at-least-256-bits"
$env:OPENAI_API_KEY="sk-your-openai-api-key"
$env:OPENAI_BASE_URL="https://api.openai.com/v1"
```

### 3. 编译与运行

```powershell
# 编译
mvn clean verify

# 启动（dev 环境）
mvn spring-boot-run -Dspring-boot.run.profiles=dev
```

服务默认运行在 `http://localhost:8080`。

## 配置说明

### Profile

- `dev` — 开发环境，Swagger/Knife4j 默认开启
- `test` — 测试环境
- `prod` — 生产环境，Swagger/Knife4j 默认关闭

### 关键配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `app.auth.access-token-ttl` | Access Token 有效期 | 30m |
| `app.auth.refresh-token-ttl` | Refresh Token 有效期 | 30d |
| `app.chat.message-max-length` | 用户消息最大长度（字符） | 256 |
| `app.chat.sse.max-active-connections` | 最大活跃 SSE 连接数 | 1000 |
| `app.chat.sse.llm-concurrency` | 模型最大并发数 | 100 |
| `app.chat.sse.max-lifetime` | 单次生成最大存活时间 | 140s |
| `spring.ai.openai.chat.timeout` | OpenAI 调用超时 | 150s |
| `spring.ai.chat.memory.redis.max-messages-per-conversation` | 上下文窗口大小 | 5 |

完整配置见 `application.yml`。

## API 文档

启动后访问 Knife4j UI（需 `app.openapi.enabled=true`）：

```
http://localhost:8080/api/doc.html
```

OpenAPI 静态契约文件：`src/main/resources/openapi/ai-knowledge-base-openapi.json`

### 核心接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/auth/register` | POST | 用户注册 |
| `/api/auth/login` | POST | 用户登录 |
| `/api/auth/refresh` | POST | 刷新 Token |
| `/api/auth/logout` | POST | 退出登录 |
| `/api/chat/sessions` | POST | 创建会话 |
| `/api/chat/sessions` | GET | 查询会话列表 |
| `/api/chat/sessions/{id}` | DELETE | 删除会话 |
| `/api/chat/sessions/{id}/messages` | GET | 分页查询消息历史 |
| `/api/chat/sessions/stream` | POST | SSE 流式对话 |
| `/api/admin/users` | POST | 分页查询用户（ADMIN） |
| `/api/admin/users/{id}` | GET | 按 ID 查询用户（ADMIN） |
| `/api/admin/users/{id}/status` | PUT | 更新用户状态（ADMIN） |

## 项目结构

```
com.jason.ai.knowledgebase
├── controller          # 控制器层（协议适配）
├── service
│   └── sse             # SSE 流式生成生命周期
├── repository
│   ├── mapper          # MyBatis Mapper
│   ├── cache           # Redis 缓存操作
│   └── projection      # 查询投影
├── model
│   ├── entity          # 数据库实体
│   ├── request         # 请求 DTO
│   ├── response        # 响应 DTO
│   ├── enums           # 枚举
│   ├── event           # 内部事件
│   └── internal        # 内部模型
├── config              # 配置类
├── security            # 安全认证
└── common              # 公共工具、异常、API 封装
```

## 开发指南

### 构建命令

```powershell
mvn clean verify       # 完整构建（含单元测试 + 架构测试 + JaCoCo）
mvn clean compile      # 快速编译
```

### 架构约束

- Controller 不直接依赖 Repository、Entity 或内部模型
- Repository 不反向依赖 Controller、Service 或接口 DTO
- Model 不依赖 Controller、Service、Repository、Config 或 Security
- 顶层职责包之间无循环依赖
- 以上约束由 ArchUnit 持续校验

### 数据库变更

- 首次建库执行 `sql/init.sql`
- 后续变更写入 `sql/YYYYMMDD.sql`，SQL 必须可独立审查

### 注意

- 所有 64 位 ID 在 JSON 接口中以字符串序列化，避免 JavaScript 精度损失
- 密码、JWT 密钥、OpenAI Key 等凭据必须通过环境变量注入，不得提交到代码仓库