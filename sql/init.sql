SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL COMMENT '用户ID',
    username VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt密码哈希',
    role VARCHAR(16) NOT NULL DEFAULT 'USER' COMMENT '用户角色',
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '账号状态',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    create_time DATETIME(3) NOT NULL COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    KEY idx_sys_user_status_deleted (status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户表';

INSERT IGNORE INTO sys_user
    (id, username, password_hash, role, status, deleted, create_time, update_time)
VALUES
    (348019002964054016, 'admin', '$2a$10$Na6IaPc7x3c.7lDVC5wfOuYdE8rOUij8HSbIYiOFLQfqgEc4RkZFO',
     'ADMIN', 'ENABLED', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3));

CREATE TABLE IF NOT EXISTS auth_session (
    id BIGINT NOT NULL COMMENT '认证会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    refresh_token_hash CHAR(64) NOT NULL COMMENT '刷新令牌哈希',
    refresh_expire_time DATETIME(3) NOT NULL COMMENT '刷新令牌过期时间',
    revoked TINYINT NOT NULL DEFAULT 0 COMMENT '撤销标记：0-未撤销，1-已撤销',
    create_time DATETIME(3) NOT NULL COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_session_refresh_hash (refresh_token_hash),
    KEY idx_auth_session_user_revoked (user_id, revoked)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户认证会话表';

CREATE TABLE IF NOT EXISTS ai_chat_session (
    id BIGINT NOT NULL COMMENT '对话会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(100) NOT NULL DEFAULT '新对话' COMMENT '会话标题',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    create_time DATETIME(3) NOT NULL COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_chat_session_user_update (user_id, deleted, update_time DESC, id DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI对话会话表';

CREATE TABLE IF NOT EXISTS ai_conversation_message (
    id BIGINT NOT NULL COMMENT '消息ID',
    session_id BIGINT NOT NULL COMMENT '对话会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    request_id BIGINT NOT NULL COMMENT '请求ID，用于关联同轮用户与助手消息',
    role VARCHAR(16) NOT NULL COMMENT '消息角色',
    content LONGTEXT NOT NULL COMMENT '消息内容',
    status VARCHAR(32) NULL COMMENT '消息状态',
    metadata JSON NULL COMMENT '助手消息元数据',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    create_time DATETIME(3) NOT NULL COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_message_request (request_id),
    KEY idx_message_session_id (session_id, deleted, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI对话消息表';

CREATE TABLE IF NOT EXISTS ai_user_quota (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    available_times INT NOT NULL DEFAULT 0 COMMENT '剩余可用对话次数',
    create_time DATETIME(3) NOT NULL COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL COMMENT '更新时间',
    PRIMARY KEY (user_id),
    CONSTRAINT chk_ai_user_quota_non_negative CHECK (available_times >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI用户额度表';

CREATE TABLE IF NOT EXISTS ai_meaningless_phrase (
    id BIGINT NOT NULL COMMENT '短语ID',
    phrase VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '无意义短语',
    category VARCHAR(64) NULL COMMENT '短语分类',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '启用标记：0-禁用，1-启用',
    priority INT NOT NULL DEFAULT 0 COMMENT '匹配优先级，数值越大优先级越高',
    remark VARCHAR(500) NULL COMMENT '备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    create_time DATETIME(3) NOT NULL COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_meaningless_phrase (phrase),
    KEY idx_meaningless_enabled_priority (enabled, deleted, priority DESC, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI无意义请求短语表';
