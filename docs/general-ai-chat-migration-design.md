# 通用 AI 多轮对话迁移与实现方案

## 1. 文档定位

本项目已经从灯效生成服务迁移为独立的通用多轮文本对话服务。本文件记录当前实现基线、不可破坏的业务契约和后续演进边界；代码、接口、数据库、配置或目录架构发生变化时，必须同步更新本文件与 `AGENTS.md`。

项目固定使用 Java 21、Spring Boot 4、Spring AI 2.0、OpenAI、MySQL、Redis Stack、MyBatis-Plus 和 MapStruct，当前按单实例部署设计。

## 2. 范围与非目标

### 2.1 当前能力

- 用户名注册、登录、刷新 Token 和退出，密码使用 BCrypt 保存。
- `USER`、`ADMIN` 两种角色，管理员通过数据库手工设置。
- 单账号单登录：新登录、退出和停用账号都会注销该账号的全部认证会话。
- 会话创建、查询、软删除和完整消息历史分页。
- 真正转发 OpenAI 增量文本的 SSE 对话接口。
- 全局 FIFO 队列、模型并发控制、JVM Registry、Redis 单用户互斥锁和超时清理。
- 注册额度初始化为 0；请求成功进入队列时立即扣减 1，失败、取消或断开不退款。
- 确定性无意义请求规则、MySQL 短语管理和 Redis 短语缓存。
- 静态 OpenAPI 契约、Knife4j、Actuator health/info 和 `dev/test/prod` 配置。

### 2.2 非目标

- 不实现知识库、RAG、向量检索、文档入库或答案引用。
- 不包含灯效提示词、灯效 Advisor、Lua 生成或 Lua 校验。
- 不实现多实例队列、跨实例取消、请求恢复或分布式 Registry。
- 不创建 `ai_chat_request` 表，不持久化请求运行状态。
- 不实现会话重命名、清空上下文、用户等级或额度自动重置。
- 不允许客户端选择模型或供应商。

## 3. 目标代码结构

项目规模和业务复杂度尚不需要按 `account`、`chat`、`admission` 重复建立整套子分层，统一按职责轻量分层。请求和响应模型严格分包，同时保留聚合容器控制类数量，例如 `AuthRequests`、`AuthResponses`、`ChatRequests`、`ChatResponses`。

```text
com.jason.ai.knowledgebase
├── controller
├── service
│   ├── converter
│   └── sse
├── repository
│   ├── mapper
│   ├── cache
│   └── projection
├── model
│   ├── entity
│   ├── request
│   ├── response
│   ├── enums
│   ├── event
│   └── internal
├── config
├── security
└── common
    ├── api
    ├── exception
    └── util
```

职责约束：

- `controller` 只负责协议适配、参数接收和响应封装，只能使用接口请求/响应模型、Service、Security 和 Common，不得直接依赖 Repository、实体或内部模型。
- `service` 负责业务规则、事务边界和跨资源编排。确定性的实体/查询投影到响应 DTO 映射集中在 `service.converter`；SSE 队列、连接、生成和终态生命周期集中在 `service.sse`，只在该包使用的任务和值对象保持包可见。
- `repository.mapper` 收敛 MySQL 访问，`repository.cache` 收敛 Redis 状态和缓存访问，`repository.projection` 保存查询投影。Repository 不得反向依赖 Controller、Service 或接口 DTO。
- `model.entity` 只保存数据库实体；`model.request` 和 `model.response` 分别保存入参与出参；`model.enums` 保存共享枚举；`model.event` 保存应用事件；`model.internal` 保存不对外暴露的跨服务内部数据。
- 请求和响应模型不得相互依赖。不得重新建立同时包含入参与出参的 `AuthDtos`、`ChatDtos` 等混合容器；`ChatSseEvent` 作为独立响应模型保留。
- `config` 负责框架配置和属性绑定，`security` 负责认证授权，`common` 只保存无业务归属的响应、异常和通用工具。
- Model 不得依赖 Controller、Service、Repository、Config 或 Security；顶层职责包不得形成循环依赖。
- 上述边界由 `PackageArchitectureTest` 持续校验。新增代码优先放入既有职责包，仅在出现稳定且独立的新职责时增加子包。

## 4. 通用 API 契约

- 所有业务接口统一使用 `/api` 前缀。
- Controller 的每个接口方法都在方法级映射注解中声明非空路径，不直接映射到类级根路径。
- 普通接口统一返回 `ApiResponse`；成功响应使用 `success` 工厂方法，成功码为 200。
- 除 SSE 流式接口及其内部调用链外，Controller 对外调用的 Service 方法只要有返回值，就直接返回 `ApiResponse`，Controller 不再重复包装。
- 分页 Service 先使用 `IPage.convert` 完成实体到响应 DTO 的转换，再调用 `ApiResponse.page(IPage)`；该方法只提取 `items` 和 `total` 组成 `PageResult`，不返回请求中的页码和每页数量。
- `ErrorCode` 使用全局唯一的三位业务码。全局异常处理器和 Spring Security 捕获的异常保持 HTTP 200，客户端只根据 `ApiResponse.code` 判断结果。
- 对外 Controller 数值参数、API DTO、`PageResult` 和 `ChatSseEvent` 的 64 位整数统一使用 `Long`，Jackson 将 Long 序列化为字符串，避免 JavaScript 精度损失。
- OpenAPI 契约固定为 `src/main/resources/openapi/ai-knowledge-base-openapi.json`，由自定义 Controller 提供；接口、DTO 或 SSE 事件变化时必须同步更新。
- Knife4j 使用静态契约，不依赖运行时扫描生成另一份定义。

## 5. 认证与授权

### 5.1 用户和密码

用户表固定为 `sys_user`。数据库仅保存 BCrypt 编码值，字段名为 `password_hash`。日志不得记录明文密码、哈希值、Access Token 或 Refresh Token。

注册成功后同时创建额度记录，初始额度为 0。首个管理员和额度调整当前均由管理员直接操作数据库。

### 5.2 认证会话

- Access Token 默认有效期 30 分钟，Refresh Token 默认有效期 30 天。
- Refresh Token 使用安全随机值，数据库仅保存 SHA-256 哈希。
- 登录前注销该用户的全部旧会话，再创建新会话并更新 Redis 当前会话指针。
- 刷新 Token 时轮换 Refresh Token。
- 退出时注销该账号全部数据库认证会话，并清除 Redis 当前会话指针。
- JWT 除签名和过期时间外，还必须校验 Redis 中当前 `authSessionId`；不得仅依赖 JWT 本身。

### 5.3 管理接口

- `/api/admin/**` 仅允许 `ADMIN`。
- 管理员可按用户 ID 查询用户，也可通过 POST 请求体传递分页参数和可选用户名，分页查询全部用户并执行用户名模糊匹配。
- 单用户视图返回用户名、角色、状态、创建时间和更新时间；分页列表项额外返回字符串形式的用户 ID。
- 状态更新接口只接受 `ENABLED` 或 `DISABLED`，并保持幂等。
- 停用账号时必须更新状态、注销全部认证会话并清除 Redis 当前会话指针，使已有 Token 立即失效。

## 6. 会话与消息

### 6.1 标识与会话

用户、会话、消息和请求 ID 均使用 Snowflake BIGINT。

新会话标题默认为“新对话”。第一次成功受理用户问题时，使用规范化文本并按 Unicode code point 截取前 30 个字符作为标题；后续请求不再修改标题，不提供重命名接口。

会话列表不接收分页或其他参数，一次返回当前用户全部未删除会话，并按 `update_time DESC, id DESC` 排序。

删除会话和消息只允许软删除。删除前检查会话归属和活动请求；事务提交后清除对应 Redis ChatMemory。

### 6.2 消息模型

MySQL 保存完整且可分页的历史。`ai_conversation_message` 包含 `request_id`，同一轮 User 和 Assistant 消息使用相同请求 ID 关联，不使用 `paired_message_id`。

消息表不包含独立 `model` 字段。Assistant 的模型、完成原因、Token、Tool 摘要和可选错误码写入 `metadata JSON`；metadata 只保存在 MySQL，不进入 Redis ChatMemory 或模型上下文。

模型失败、取消或客户端断开时，已生成的部分回答仍保存到 MySQL。默认 Advisor 已写入 Redis 的 User 消息允许作为孤立消息保留，部分 Assistant 回答不得手工写入 Redis。

## 7. Redis ChatMemory

直接使用 Spring AI 2.0 的 `RedisChatMemoryRepository`、`MessageWindowChatMemory` 和 `MessageChatMemoryAdvisor`，不实现第二套记忆仓库。

- 使用真实 `sessionId` 作为 conversation ID。
- Redis 模型上下文最多保留最近 5 条消息；MySQL 完整历史不受此限制。
- 默认 Advisor 在模型调用前写入用户问题，正常流式聚合完成后写入完整助手回答。
- 失败、取消或断开不回滚已经写入的 User 消息，部分 Assistant 回答仅保存 MySQL。
- `RedisChatMemoryConfig` 创建的 Jedis 客户端读取 `spring.data.redis` 的主机、端口、用户名、密码、SSL、超时和数据库配置，逻辑数据库固定为 3。
- `spring.ai.chat.memory.redis` 只保存 Key 前缀、索引、Schema 初始化和窗口配置。
- Redis 必须使用 Redis Stack 7+，因为该仓库依赖 RedisJSON 和 Query Engine；普通 Redis 镜像不满足要求。

建议 Key：

```text
akb:auth:session:{userId}
akb:chat:user-lock:{userId}
akb:chat:memory:*
akb:admission:meaningless-phrases
```

## 8. SSE、排队和额度

### 8.1 接口与事件

流式接口固定为：

```text
POST /api/chat/sessions/stream
```

`sessionId` 和 `message` 通过 JSON 请求体传递。SSE 真正转发 OpenAI 增量文本，正常状态顺序为：

```text
Queued -> Generating -> Delta... -> Generated
```

失败、取消和拒绝使用明确终态事件；心跳使用 SSE comment。所有业务事件都返回非空 `requestId`。

### 8.2 准入顺序

固定顺序为：

1. 认证和 JSON 请求体校验。
2. 生成 `requestId`。
3. 校验会话归属和输入规则。
4. 预留本地 Registry/进程容量。
5. 获取 Redis 单用户互斥锁。
6. 数据库事务扣减额度并写入 User 消息和 Assistant 占位消息。
7. 注册 `SseConnectionRegistry`。
8. 进入全局 FIFO 队列。

进入流式 Controller 后，第 3～8 步的任何业务拒绝或运行时异常都不得冒出接口，必须保持 HTTP 200，并只返回一条 `Failed` 终态事件。持久化前失败时 `assistantMessageId` 为 `null`；额度不足使用 `QUOTA_EXHAUSTED`，未知异常使用 `INTERNAL_ERROR`。

### 8.3 资源所有权

- Redis 用户锁必须用 compare-and-delete Lua 释放，禁止无条件删除可能已换主的锁。
- 队列容量、Registry 容量、模型并发、超时、心跳、锁 TTL、终态保留期和清理间隔保持配置化。
- 一个请求只能由一个路径取得终态处理权。终态处理统一负责保存 Assistant、发送事件、停止模型订阅、释放 Redis 锁、模型许可和本地容量。
- 请求进入队列时立即扣减额度；模型失败、主动取消和客户端断开均不退款。
- 数据库事务内不得调用 OpenAI，也不得执行不可控的外部网络操作。
- 服务关闭时先停止接收新请求，再取消或终结 Registry 中尚未完成的请求。

### 8.4 超时和重试

`spring.ai.openai.chat.timeout` 固定为 150 秒，高于 140 秒的业务 `max-lifetime`。OpenAI SDK cause 链中的 `TimeoutException`、`SocketTimeoutException`，以及消息明确包含 timeout/timed out 的 `InterruptedIOException` 映射为 `GENERATION_TIMEOUT`；其他 I/O 异常保持 `LLM_FAILED`。

应用层不得对流式模型调用添加自动 `retry()`，避免已经输出增量后重复生成或重复计费。

Spring MVC 流式响应完成时会产生 `ASYNC` 二次派发。安全链只允许 `POST /api/chat/sessions/stream` 的 ASYNC 续派跳过重复授权，初始 REQUEST 仍完整执行 JWT 和 Redis 当前会话校验。

## 9. 无意义请求

- 原始问题最大长度由 `app.chat.message-max-length` 配置，默认 256 个字符。
- 先执行确定性规则，再查询 Redis 启用短语集合。
- 短语管理数据以 MySQL 为准，Redis 仅作为缓存。
- 管理接口变更短语后，只在数据库事务提交后刷新缓存。
- 标题可以使用规范化文本；MySQL User 消息和模型输入保留通过校验后的原始文本。
- 无意义请求在扣额度和写消息之前拒绝。

## 10. 数据库基线

首次建库执行 `sql/init.sql`；之后的人工变更写入 `sql/YYYYMMDD.sql`。SQL 必须可以独立审查，不依赖当前工作目录的 `SOURCE` 命令。MySQL 使用 utf8mb4。

核心表：

```text
sys_user
├── id
├── username
├── password_hash
├── role
├── status
├── deleted
├── create_time
└── update_time

auth_session
├── id
├── user_id
├── refresh_token_hash
├── refresh_expire_time
├── revoked
├── create_time
└── update_time

user_quota
├── user_id
├── remaining_quota
├── create_time
└── update_time

ai_chat_session
├── id
├── user_id
├── title
├── deleted
├── create_time
└── update_time

ai_conversation_message
├── id
├── session_id
├── user_id
├── request_id
├── role
├── content
├── status
├── metadata JSON
├── deleted
├── create_time
└── update_time

meaningless_phrase
├── id
├── phrase
├── category
├── enabled
├── priority
├── remark
├── deleted
├── create_time
└── update_time
```

数据库不创建 `ai_chat_request`，消息表不包含 `paired_message_id` 或独立 `model` 字段。

## 11. 配置、安全和日志

- 仅保留 `dev`、`test`、`prod` 三个 Profile。
- 公共配置统一放在 `application.yml`；三个 Profile 文件只覆盖 `app.openapi.enabled`，由 `--spring.profiles.active=` 选择，不配置 `spring.config.activate.on-profile`。
- 数据库密码、Redis 密码、JWT 密钥和 OpenAI Key 全部通过环境变量注入，不提交真实凭据。
- `dev/test` 默认开放静态 Swagger/Knife4j，`prod` 默认关闭。
- 只保留必要业务日志和 Actuator health/info。
- 日志不得输出问题全文、模型完整回答、密码、Token、哈希值或密钥。

## 12. 代码规范

- 使用构造器注入，不使用字段注入。
- MapStruct 转换器统一使用 `service.converter.MapStructConfiguration`，以 Spring 组件和构造器方式注入，并对未映射目标字段直接编译失败。转换器只承担无副作用的字段映射；校验、规范化、默认业务状态、ID/Token 生成、状态机、资源访问、响应包装和分页边界保留在 Service 或既有工厂中，不使用 MapStruct `expression` 隐藏业务逻辑。
- Maven 编译必须同时配置 MapStruct Processor、Lombok 与 `lombok-mapstruct-binding`；本地 IDE 从 Maven 导入注解处理器路径并开启 annotation processing，避免 Lombok getter 尚未生成时 MapStruct 报出错误元素。
- Controller 只做协议适配，全局异常到 HTTP 响应的映射统一放在 `controller.GlobalExceptionHandler`；事务和业务规则放 Service；SQL 和 Redis 数据访问放 Repository。
- 公共方法和业务逻辑复杂的私有方法使用中文 JavaDoc，说明用途、参数、返回值和可能抛出的异常；简单 getter、构造器和一眼可见的私有辅助方法不强制注释。
- 配置按职责拆为 `AuthProperties`、`AdmissionProperties`、`ChatInputProperties`、`AiProperties`、`ChatProperties`、`SnowflakeProperties` 和 `OpenApiProperties`，同时保持现有 YAML Key。
- 共享枚举统一放在 `model.enums`，通用错误码放在 `common.exception`；不得散落魔法数字、魔法字符串或环境判断。
- 避免为单一实现创建无意义的接口与 `impl` 包；只有出现真实替换点时再抽象。

## 13. 测试与验收

新增行为至少覆盖正常、边界、拒绝和资源释放。单元测试不得依赖真实 OpenAI。集成测试可使用 Testcontainers 启动 MySQL 和 Redis Stack；Docker 不可用时明确跳过，禁止改用生产资源。

提交前至少执行：

```text
mvn clean verify
```

该命令必须通过：

- 单元测试和契约测试。
- `PackageArchitectureTest` 分层约束。
- 静态 OpenAPI JSON 可解析检查。
- SQL 与实体字段一致性检查。
- `dev/test/prod` 配置绑定检查。
- JaCoCo 报告生成和覆盖率门槛。

关键验收条件：

1. 注册后额度为 0；额度不足时流式接口保持 HTTP 200，只返回带 `requestId` 的 `Failed/QUOTA_EXHAUSTED`。
2. 正常 SSE 顺序为 `Queued -> Generating -> Delta... -> Generated`。
3. 同一用户跨会话同时只能存在一个活动请求，不同用户按准入顺序进入全局 FIFO。
4. 取消、失败和断开保存 MySQL 部分回答，额度不退，Redis 不写入部分 Assistant。
5. Redis 模型上下文最多 5 条，MySQL 历史完整分页。
6. 新登录、退出和停用账号使旧 Access/Refresh Token 立即失效。
7. 用户不能查询、删除或取消其他用户的资源。
8. 会话删除使用软删除，并在提交后清除 Redis ChatMemory。
9. 无意义请求在扣额度前拒绝。
10. 项目中不存在灯效、Lua、RAG、请求表或多实例协调代码。

## 14. 当前迁移结果

目录已经从按业务域重复分层调整为按职责轻量分层：

- 原 `account`、`chat`、`admission` 的 Controller 汇总到 `controller`。
- 业务服务汇总到 `service`，SSE 生命周期保留独立的 `service.sse` 子包。
- 实体和查询投影到响应 DTO 的确定性转换集中到 `service.converter`，使用 MapStruct 生成实现；MyBatis 数据访问接口继续使用 `repository.mapper`，两类 Mapper 不混用。
- MySQL Mapper、Redis Store/Cache 和查询投影统一进入 `repository` 的对应子包。
- 实体、请求、响应、枚举、事件和内部数据分别进入 `model` 子包。
- 原业务域 Config 与基础设施配置统一进入 `config`，认证授权统一进入 `security`。
- 通用响应、异常、JSON、分页、Snowflake 和 Unicode 工具进入 `common`。
- 原混合 DTO 容器拆分为方向明确的 `*Requests` 与 `*Responses` 容器，对外 JSON 契约保持不变。

该调整只改变 Java 包和内部类型组织，不改变 API 路径、JSON 字段、数据库表、YAML Key、SSE 事件或业务语义。

## 15. 风险与演进边界

- 默认 Advisor 在模型调用前写入 User 消息，失败、取消或断开会留下孤立 User；这是已接受语义，升级 Spring AI 时必须回归验证。
- Redis 故障时无法可靠保证单用户互斥和 ChatMemory，对话准入保持 fail-closed。
- 单实例 JVM Registry 和 FIFO 无法承受多实例部署。扩容前必须重新设计请求归属、分布式队列、取消路由和恢复策略。
- 进程被强杀时可能丢失尚未持久化的回答尾部。
- 静态 OpenAPI 不会自动跟随代码变化，接口或 DTO 修改必须同步更新契约和测试。
- 当前轻量职责分层适合现有规模；只有当业务域数量、团队边界或独立发布需求真实增长时，才重新评估领域模块化，不预先建立重复目录。