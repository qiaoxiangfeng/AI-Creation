-- 数据库初始化脚本（整合所有 DDL 和初始化逻辑）
-- 说明：
-- 1. 本脚本用于在全新环境下初始化数据库结构
-- 2. 已将原来分散的建表、字段变更及约束脚本整合到一个文件中
-- 3. 历史迁移脚本（增删旧字段、数据迁移等）已移除

-- =====================================================================
-- 1. 文章表 article
-- =====================================================================

-- 删除已存在的表
DROP TABLE IF EXISTS article;

-- 创建文章表（已合并 response_id、字数预估和完结标识等字段）
CREATE TABLE article (
    id BIGSERIAL PRIMARY KEY,
    article_name VARCHAR(255) NOT NULL,
    article_outline TEXT,
    story_background TEXT,
    image_desc TEXT,
    theme TEXT,
    additional_characteristics TEXT,
    voice_tone VARCHAR(100),
    voice_link VARCHAR(500),
    voice_file_path VARCHAR(500),
    video_link VARCHAR(500),
    video_file_path VARCHAR(500),
    publish_status SMALLINT DEFAULT 1,
    total_word_count_estimate INTEGER,
    chapter_word_count_estimate INTEGER,
    story_complete BOOLEAN DEFAULT FALSE,
    response_id VARCHAR(255),
    res_state SMALLINT DEFAULT 1,
    create_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_article_name ON article(article_name);
CREATE INDEX IF NOT EXISTS idx_article_create_time ON article(create_time);
CREATE INDEX IF NOT EXISTS idx_article_publish_status ON article(publish_status);
CREATE INDEX IF NOT EXISTS idx_article_res_state ON article(res_state);
CREATE INDEX IF NOT EXISTS idx_article_response_id ON article(response_id);
CREATE INDEX IF NOT EXISTS idx_article_theme ON article(theme);
CREATE INDEX IF NOT EXISTS idx_article_create_user_id ON article(create_user_id);

-- 注释
COMMENT ON TABLE article IS '文章表';
COMMENT ON COLUMN article.id IS '主键ID';
COMMENT ON COLUMN article.article_name IS '文章名称';
COMMENT ON COLUMN article.article_outline IS '文章简介';
COMMENT ON COLUMN article.story_background IS '故事背景';
COMMENT ON COLUMN article.image_desc IS '形象描述';
COMMENT ON COLUMN article.theme IS '文章主题/分类（原 article_type）';
COMMENT ON COLUMN article.additional_characteristics IS '附加特点（生成配置的非主题字段值拼接，逗号分隔）';
COMMENT ON COLUMN article.voice_tone IS '音色';
COMMENT ON COLUMN article.voice_link IS '语音链接';
COMMENT ON COLUMN article.voice_file_path IS '语音文件地址';
COMMENT ON COLUMN article.video_link IS '视频链接';
COMMENT ON COLUMN article.video_file_path IS '视频文件地址';
COMMENT ON COLUMN article.publish_status IS '发布状态（1-未发布，2-已发布）';
COMMENT ON COLUMN article.total_word_count_estimate IS '总字数预估';
COMMENT ON COLUMN article.chapter_word_count_estimate IS '每章节字数预估';
COMMENT ON COLUMN article.story_complete IS '章节完结标识（true-章节已完结，false-章节未完结）';
COMMENT ON COLUMN article.response_id IS 'Responses API的response_id，用于小说上下文管理和多任务隔离';
COMMENT ON COLUMN article.res_state IS '删除标记（1-有效，0-无效）';
COMMENT ON COLUMN article.create_user_id IS '创建人用户ID（关联 user_info.id；由配置任务生成的文章与配置的创建人一致）';
COMMENT ON COLUMN article.create_time IS '创建时间';
COMMENT ON COLUMN article.update_time IS '更新时间';


-- =====================================================================
-- 2. 文章章节表 article_chapter
-- =====================================================================

-- 删除已存在的表
DROP TABLE IF EXISTS article_chapter;

-- 创建文章章节表（已合并核心剧情、字数预估和生成状态字段）
CREATE TABLE article_chapter (
    id BIGSERIAL PRIMARY KEY,
    chapter_no INTEGER NOT NULL,
    article_id BIGINT NOT NULL,
    chapter_title VARCHAR(255),
    chapter_content TEXT,
    core_plot TEXT,
    word_count_estimate INTEGER,
    response_id_plan VARCHAR(255),
    response_id_content VARCHAR(255),
    generation_status SMALLINT DEFAULT 0,
    chapter_voice_link VARCHAR(500),
    chapter_video_link VARCHAR(500),
    res_state SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_article_chapter_article_id ON article_chapter(article_id);
CREATE INDEX IF NOT EXISTS idx_article_chapter_chapter_no ON article_chapter(chapter_no);
CREATE INDEX IF NOT EXISTS idx_article_chapter_res_state ON article_chapter(res_state);
CREATE INDEX IF NOT EXISTS idx_article_chapter_generation_status ON article_chapter(generation_status);

-- 注释
COMMENT ON TABLE article_chapter IS '文章章节表';
COMMENT ON COLUMN article_chapter.id IS '主键ID';
COMMENT ON COLUMN article_chapter.chapter_no IS '章节序号（第一章填1）';
COMMENT ON COLUMN article_chapter.article_id IS '文章ID，关联 article.id';
COMMENT ON COLUMN article_chapter.chapter_title IS '章节标题';
COMMENT ON COLUMN article_chapter.chapter_content IS '章节内容';
COMMENT ON COLUMN article_chapter.core_plot IS '核心剧情';
COMMENT ON COLUMN article_chapter.word_count_estimate IS '字数预估';
COMMENT ON COLUMN article_chapter.response_id_plan IS '生成本章章节信息时的response_id，用于本章内容生成的上下文起点';
COMMENT ON COLUMN article_chapter.response_id_content IS '生成本章正文内容时的response_id，用于后续重新生成时的上下文起点';
COMMENT ON COLUMN article_chapter.generation_status IS '生成状态（0-未开始，1-生成中，2-已完成，3-失败）';
COMMENT ON COLUMN article_chapter.chapter_voice_link IS '章节语音链接地址';
COMMENT ON COLUMN article_chapter.chapter_video_link IS '章节视频链接地址';
COMMENT ON COLUMN article_chapter.res_state IS '删除标记（1-有效，0-无效）';
COMMENT ON COLUMN article_chapter.create_time IS '创建时间';
COMMENT ON COLUMN article_chapter.update_time IS '更新时间';


-- =====================================================================
-- 3. 文章生成配置表 article_generation_config
-- =====================================================================

-- 删除已存在的表
DROP TABLE IF EXISTS article_generation_config;

-- 创建文章生成配置表（包含字数预估等字段）
CREATE TABLE article_generation_config (
    id BIGSERIAL PRIMARY KEY,
    theme TEXT NOT NULL,
    gender VARCHAR(50),
    genre VARCHAR(100),
    plot VARCHAR(200),
    character_type VARCHAR(100),
    style VARCHAR(100),
    additional_characteristics TEXT,
    total_word_count_estimate INTEGER DEFAULT 100000,
    chapter_word_count_estimate INTEGER DEFAULT 5000,
    pending_count INTEGER DEFAULT 0,
    res_state SMALLINT DEFAULT 1,
    create_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_article_generation_config_theme ON article_generation_config(theme);
CREATE INDEX IF NOT EXISTS idx_article_generation_config_create_user_id ON article_generation_config(create_user_id);
CREATE INDEX IF NOT EXISTS idx_article_generation_config_create_time ON article_generation_config(create_time);
CREATE INDEX IF NOT EXISTS idx_article_generation_config_res_state ON article_generation_config(res_state);

-- 注释
COMMENT ON TABLE article_generation_config IS '文章生成配置表';
COMMENT ON COLUMN article_generation_config.id IS '主键ID';
COMMENT ON COLUMN article_generation_config.theme IS '文章主题（用户自定义输入）';
COMMENT ON COLUMN article_generation_config.gender IS '性别分类（男生小说、女生小说）';
COMMENT ON COLUMN article_generation_config.genre IS '题材分类（仙侠、玄幻、都市等）';
COMMENT ON COLUMN article_generation_config.plot IS '情节分类（升级、学院、人生赢家等）';
COMMENT ON COLUMN article_generation_config.character_type IS '角色分类';
COMMENT ON COLUMN article_generation_config.style IS '风格分类';
COMMENT ON COLUMN article_generation_config.additional_characteristics IS '附加特点（逗号分隔）';
COMMENT ON COLUMN article_generation_config.total_word_count_estimate IS '总字数预估（默认100000）';
COMMENT ON COLUMN article_generation_config.chapter_word_count_estimate IS '每章节字数预估（默认5000）';
COMMENT ON COLUMN article_generation_config.pending_count IS '待生成数量';
COMMENT ON COLUMN article_generation_config.res_state IS '删除标记（1-有效，0-无效）';
COMMENT ON COLUMN article_generation_config.create_user_id IS '创建人用户ID（关联 user_info.id）';
COMMENT ON COLUMN article_generation_config.create_time IS '创建时间';
COMMENT ON COLUMN article_generation_config.update_time IS '更新时间';


-- =====================================================================
-- 4. 字典表 dictionary
-- =====================================================================

-- 删除已存在的表
DROP TABLE IF EXISTS dictionary;

-- 创建字典表
CREATE TABLE dictionary (
    id BIGSERIAL PRIMARY KEY,
    dict_key VARCHAR(100) NOT NULL,
    dict_value VARCHAR(500) NOT NULL,
    sort_order INTEGER DEFAULT 0,
    res_state SMALLINT DEFAULT 1,
    create_user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_dictionary_key ON dictionary(dict_key);
CREATE INDEX IF NOT EXISTS idx_dictionary_create_time ON dictionary(create_time);
CREATE INDEX IF NOT EXISTS idx_dictionary_res_state ON dictionary(res_state);
CREATE INDEX IF NOT EXISTS idx_dictionary_create_user_id ON dictionary(create_user_id);

-- 注释
COMMENT ON TABLE dictionary IS '字典表';
COMMENT ON COLUMN dictionary.id IS '主键ID';
COMMENT ON COLUMN dictionary.dict_key IS '字典键';
COMMENT ON COLUMN dictionary.dict_value IS '字典值';
COMMENT ON COLUMN dictionary.sort_order IS '排序顺序';
COMMENT ON COLUMN dictionary.res_state IS '删除标记（1-有效，0-无效）';
COMMENT ON COLUMN dictionary.create_user_id IS '创建人用户ID（NULL 表示全局字典，所有用户可见）';
COMMENT ON COLUMN dictionary.create_time IS '创建时间';
COMMENT ON COLUMN dictionary.update_time IS '更新时间';


-- =====================================================================
-- 5. 伏笔表 plot
-- =====================================================================

-- 删除已存在的表
DROP TABLE IF EXISTS plot;

-- 创建伏笔表
CREATE TABLE plot (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL,
    chapter_id BIGINT NOT NULL,
    plot_name VARCHAR(255) NOT NULL,
    plot_content TEXT,
    recovery_chapter_id BIGINT,
    res_state SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_plot_article_id ON plot(article_id);
CREATE INDEX IF NOT EXISTS idx_plot_chapter_id ON plot(chapter_id);
CREATE INDEX IF NOT EXISTS idx_plot_recovery_chapter_id ON plot(recovery_chapter_id);
CREATE INDEX IF NOT EXISTS idx_plot_res_state ON plot(res_state);

-- 注释
COMMENT ON TABLE plot IS '伏笔表';
COMMENT ON COLUMN plot.id IS '主键ID';
COMMENT ON COLUMN plot.article_id IS '文章ID';
COMMENT ON COLUMN plot.chapter_id IS '埋设伏笔的章节ID';
COMMENT ON COLUMN plot.plot_name IS '伏笔名称';
COMMENT ON COLUMN plot.plot_content IS '伏笔内容';
COMMENT ON COLUMN plot.recovery_chapter_id IS '回收伏笔的章节ID';
COMMENT ON COLUMN plot.res_state IS '删除标记（1-有效，0-无效）';
COMMENT ON COLUMN plot.create_time IS '创建时间';
COMMENT ON COLUMN plot.update_time IS '更新时间';


-- =====================================================================
-- 6. 用户信息表 user_info
-- =====================================================================

-- 删除已存在的表
DROP TABLE IF EXISTS user_info;

-- 创建用户信息表
CREATE TABLE user_info (
    id BIGSERIAL PRIMARY KEY,
    user_name VARCHAR(100) NOT NULL,
    pen_name VARCHAR(100),
    user_password VARCHAR(255) NOT NULL,
    user_email VARCHAR(255),
    user_phone VARCHAR(20),
    user_status SMALLINT DEFAULT 1,
    is_admin BOOLEAN DEFAULT FALSE,
    last_login_time TIMESTAMP,
    membership_tier VARCHAR(32) NOT NULL DEFAULT 'NONE',
    membership_start_at TIMESTAMP,
    membership_end_at TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    res_state SMALLINT DEFAULT 1
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_user_info_user_name ON user_info(user_name);
CREATE INDEX IF NOT EXISTS idx_user_info_user_status ON user_info(user_status);
CREATE INDEX IF NOT EXISTS idx_user_info_res_state ON user_info(res_state);

-- 注释
COMMENT ON TABLE user_info IS '用户信息表';
COMMENT ON COLUMN user_info.id IS '主键ID';
COMMENT ON COLUMN user_info.user_name IS '用户名';
COMMENT ON COLUMN user_info.pen_name IS '笔名';
COMMENT ON COLUMN user_info.user_password IS '用户密码（加密存储）';
COMMENT ON COLUMN user_info.user_email IS '用户邮箱';
COMMENT ON COLUMN user_info.user_phone IS '用户手机号';
COMMENT ON COLUMN user_info.user_status IS '用户状态（1-启用，0-禁用）';
COMMENT ON COLUMN user_info.is_admin IS '是否管理员（true-可查看全部文章与配置）';
COMMENT ON COLUMN user_info.last_login_time IS '最后登录时间';
COMMENT ON COLUMN user_info.create_time IS '创建时间';
COMMENT ON COLUMN user_info.update_time IS '更新时间';
COMMENT ON COLUMN user_info.res_state IS '删除标记（1-有效，0-无效）';
COMMENT ON COLUMN user_info.membership_tier IS '会员等级：NONE/BASIC';
COMMENT ON COLUMN user_info.membership_start_at IS '首次成为会员时间';
COMMENT ON COLUMN user_info.membership_end_at IS '当前会员权益截止时间';

CREATE INDEX IF NOT EXISTS idx_user_info_membership_end ON user_info(membership_end_at);

-- =====================================================================
-- 6.1 会员定价配置 membership_pricing_config
-- =====================================================================

CREATE TABLE membership_pricing_config (
    id BIGSERIAL PRIMARY KEY,
    tier VARCHAR(32) NOT NULL,
    duration_months SMALLINT NOT NULL,
    base_month_price_cent BIGINT NOT NULL,
    discount_rate DECIMAL(5, 4) NOT NULL DEFAULT 1.0000,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_membership_pricing_tier_duration UNIQUE (tier, duration_months)
);

CREATE INDEX IF NOT EXISTS idx_membership_pricing_enabled ON membership_pricing_config(enabled);

COMMENT ON TABLE membership_pricing_config IS '会员定价配置（基础会员各档位）';

-- =====================================================================
-- 6.2 会员开通记录 membership_subscription
-- =====================================================================

CREATE TABLE membership_subscription (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    source VARCHAR(32) NOT NULL,
    payment_order_id BIGINT,
    tier VARCHAR(32) NOT NULL,
    duration_months SMALLINT,
    previous_end_at TIMESTAMP,
    new_end_at TIMESTAMP NOT NULL,
    amount_cent BIGINT NOT NULL DEFAULT 0,
    channel VARCHAR(16),
    external_trade_no VARCHAR(128),
    remark VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_membership_sub_user_time ON membership_subscription(user_id, create_time DESC);
CREATE UNIQUE INDEX uk_membership_sub_payment_order ON membership_subscription(payment_order_id) WHERE payment_order_id IS NOT NULL;

COMMENT ON TABLE membership_subscription IS '会员开通/变更/退款审计记录';

INSERT INTO membership_pricing_config (tier, duration_months, base_month_price_cent, discount_rate, enabled, sort_order) VALUES
('BASIC', 1, 9900, 1.0000, TRUE, 1),
('BASIC', 3, 9900, 0.9800, TRUE, 2),
('BASIC', 6, 9900, 0.9500, TRUE, 3),
('BASIC', 12, 9900, 0.9200, TRUE, 4);

-- =====================================================================
-- 7. 音频文件记录表 audio_file_record
-- =====================================================================

-- =====================================================================
-- 7.1 用户钱包表 user_wallet
-- =====================================================================

DROP TABLE IF EXISTS user_wallet;

CREATE TABLE user_wallet (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    total_balance_cent BIGINT NOT NULL DEFAULT 0,
    frozen_balance_cent BIGINT NOT NULL DEFAULT 0,
    available_balance_cent BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    res_state SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_wallet_user_id ON user_wallet(user_id);

COMMENT ON TABLE user_wallet IS '用户钱包表';
COMMENT ON COLUMN user_wallet.user_id IS '用户ID（关联 user_info.id）';
COMMENT ON COLUMN user_wallet.total_balance_cent IS '总余额（分）';
COMMENT ON COLUMN user_wallet.frozen_balance_cent IS '冻结余额（分）';
COMMENT ON COLUMN user_wallet.available_balance_cent IS '可用余额（分）';
COMMENT ON COLUMN user_wallet.version IS '乐观锁版本号';
COMMENT ON COLUMN user_wallet.res_state IS '删除标记（1-有效，0-无效）';
COMMENT ON COLUMN user_wallet.create_time IS '创建时间';
COMMENT ON COLUMN user_wallet.update_time IS '更新时间';

-- =====================================================================
-- 7.2 钱包流水表 wallet_ledger
-- =====================================================================

DROP TABLE IF EXISTS wallet_ledger;

CREATE TABLE wallet_ledger (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    biz_type VARCHAR(32) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    amount_cent BIGINT NOT NULL,
    balance_before_cent BIGINT,
    balance_after_cent BIGINT,
    related_biz_type VARCHAR(32),
    related_biz_id BIGINT,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    remark VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_wallet_ledger_user_id_create_time ON wallet_ledger(user_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_wallet_ledger_related_biz ON wallet_ledger(related_biz_type, related_biz_id);

COMMENT ON TABLE wallet_ledger IS '钱包流水表';
COMMENT ON COLUMN wallet_ledger.user_id IS '用户ID（关联 user_info.id）';
COMMENT ON COLUMN wallet_ledger.biz_type IS '业务类型（RECHARGE/AI_PRE_AUTH/AI_SETTLE_DEBIT/AI_SETTLE_REFUND/MANUAL_ADJUST）';
COMMENT ON COLUMN wallet_ledger.direction IS '方向（IN/OUT/FREEZE/UNFREEZE）';
COMMENT ON COLUMN wallet_ledger.amount_cent IS '金额（分）';
COMMENT ON COLUMN wallet_ledger.balance_before_cent IS '变更前余额（分）';
COMMENT ON COLUMN wallet_ledger.balance_after_cent IS '变更后余额（分）';
COMMENT ON COLUMN wallet_ledger.related_biz_type IS '关联业务类型（RECHARGE_ORDER/AI_USAGE）';
COMMENT ON COLUMN wallet_ledger.related_biz_id IS '关联业务ID';
COMMENT ON COLUMN wallet_ledger.idempotency_key IS '幂等键（唯一）';
COMMENT ON COLUMN wallet_ledger.remark IS '备注';
COMMENT ON COLUMN wallet_ledger.create_time IS '创建时间';

-- =====================================================================
-- 7.3 AI 使用计费表 ai_usage_billing
-- =====================================================================

DROP TABLE IF EXISTS ai_usage_billing;

CREATE TABLE ai_usage_billing (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    article_id BIGINT,
    chapter_id BIGINT,
    biz_scene VARCHAR(64) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    model_name VARCHAR(64),
    request_tokens INTEGER DEFAULT 0,
    response_tokens INTEGER DEFAULT 0,
    total_tokens INTEGER DEFAULT 0,
    estimated_cost_cent BIGINT DEFAULT 0,
    actual_cost_cent BIGINT DEFAULT 0,
    pre_auth_amount_cent BIGINT DEFAULT 0,
    settled_amount_cent BIGINT DEFAULT 0,
    refund_amount_cent BIGINT DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    trace_id VARCHAR(64),
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    error_message VARCHAR(1000),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_usage_user_id_create_time ON ai_usage_billing(user_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_ai_usage_article_id ON ai_usage_billing(article_id);
CREATE INDEX IF NOT EXISTS idx_ai_usage_status ON ai_usage_billing(status);

COMMENT ON TABLE ai_usage_billing IS 'AI 使用计费表';
COMMENT ON COLUMN ai_usage_billing.user_id IS '用户ID（关联 user_info.id）';
COMMENT ON COLUMN ai_usage_billing.article_id IS '文章ID（可空）';
COMMENT ON COLUMN ai_usage_billing.chapter_id IS '章节ID（可空）';
COMMENT ON COLUMN ai_usage_billing.biz_scene IS '业务场景（GENERATE_CHAPTERS/GENERATE_CHAPTER_CONTENT/REGENERATE_CHAPTER_CONTENT/REFINE_OUTLINE）';
COMMENT ON COLUMN ai_usage_billing.provider IS 'AI 提供方（如 VOLCENGINE）';
COMMENT ON COLUMN ai_usage_billing.model_name IS '模型名称';
COMMENT ON COLUMN ai_usage_billing.request_tokens IS '请求 token 数';
COMMENT ON COLUMN ai_usage_billing.response_tokens IS '响应 token 数';
COMMENT ON COLUMN ai_usage_billing.total_tokens IS '总 token 数';
COMMENT ON COLUMN ai_usage_billing.estimated_cost_cent IS '预估成本（分）';
COMMENT ON COLUMN ai_usage_billing.actual_cost_cent IS '实际成本（分）';
COMMENT ON COLUMN ai_usage_billing.pre_auth_amount_cent IS '预占金额（分）';
COMMENT ON COLUMN ai_usage_billing.settled_amount_cent IS '结算扣款（分）';
COMMENT ON COLUMN ai_usage_billing.refund_amount_cent IS '结算退回（分）';
COMMENT ON COLUMN ai_usage_billing.status IS '状态（INIT/PRE_AUTHED/SUCCESS/FAILED/SETTLED/CANCELED）';
COMMENT ON COLUMN ai_usage_billing.trace_id IS '链路追踪ID';
COMMENT ON COLUMN ai_usage_billing.idempotency_key IS '幂等键（唯一）';
COMMENT ON COLUMN ai_usage_billing.error_message IS '错误信息（脱敏）';
COMMENT ON COLUMN ai_usage_billing.create_time IS '创建时间';
COMMENT ON COLUMN ai_usage_billing.update_time IS '更新时间';

-- =====================================================================
-- 7.4 充值订单表 recharge_order
-- =====================================================================

DROP TABLE IF EXISTS recharge_order;

CREATE TABLE recharge_order (
    id BIGSERIAL PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    channel VARCHAR(16) NOT NULL,
    amount_cent BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    subject VARCHAR(128),
    pay_trade_no VARCHAR(128),
    pay_url TEXT,
    expire_time TIMESTAMP,
    paid_time TIMESTAMP,
    callback_time TIMESTAMP,
    callback_payload TEXT,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    biz_type VARCHAR(32) NOT NULL DEFAULT 'RECHARGE',
    membership_pricing_config_id BIGINT,
    membership_duration_months SMALLINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_recharge_order_user_id_create_time ON recharge_order(user_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_recharge_order_status ON recharge_order(status);
CREATE INDEX IF NOT EXISTS idx_recharge_order_biz_type ON recharge_order(biz_type);

COMMENT ON TABLE recharge_order IS '统一支付订单（充值、会员等）';
COMMENT ON COLUMN recharge_order.order_no IS '商户订单号';
COMMENT ON COLUMN recharge_order.user_id IS '用户ID（关联 user_info.id）';
COMMENT ON COLUMN recharge_order.channel IS '充值渠道（ALIPAY/WECHAT）';
COMMENT ON COLUMN recharge_order.amount_cent IS '订单金额（分）';
COMMENT ON COLUMN recharge_order.status IS '状态（CREATED/PAYING/PAID/CLOSED/FAILED）';
COMMENT ON COLUMN recharge_order.subject IS '订单标题';
COMMENT ON COLUMN recharge_order.pay_trade_no IS '平台交易号';
COMMENT ON COLUMN recharge_order.pay_url IS '支付URL（二维码/跳转）';
COMMENT ON COLUMN recharge_order.expire_time IS '过期时间';
COMMENT ON COLUMN recharge_order.paid_time IS '支付完成时间';
COMMENT ON COLUMN recharge_order.callback_time IS '回调接收时间';
COMMENT ON COLUMN recharge_order.callback_payload IS '回调原始数据（建议脱敏）';
COMMENT ON COLUMN recharge_order.idempotency_key IS '幂等键（唯一）';
COMMENT ON COLUMN recharge_order.biz_type IS '业务类型：RECHARGE=余额充值，MEMBERSHIP=会员购买';
COMMENT ON COLUMN recharge_order.membership_pricing_config_id IS '会员定价配置ID（biz_type=MEMBERSHIP）';
COMMENT ON COLUMN recharge_order.membership_duration_months IS '会员购买时长（月）';
COMMENT ON COLUMN recharge_order.create_time IS '创建时间';
COMMENT ON COLUMN recharge_order.update_time IS '更新时间';

-- =====================================================================
-- 7.5 用户消息通知表 user_notification
-- =====================================================================

DROP TABLE IF EXISTS user_notification;

CREATE TABLE user_notification (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(128) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_time TIMESTAMP,
    biz_ref_type VARCHAR(32),
    biz_ref_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_notification_user_id_create_time ON user_notification(user_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_user_notification_user_id_is_read ON user_notification(user_id, is_read);

COMMENT ON TABLE user_notification IS '用户消息通知表';
COMMENT ON COLUMN user_notification.user_id IS '用户ID（关联 user_info.id）';
COMMENT ON COLUMN user_notification.type IS '消息类型（BALANCE_LOW/RECHARGE_SUCCESS/AI_CHARGE/AI_REFUND/SYSTEM）';
COMMENT ON COLUMN user_notification.title IS '标题';
COMMENT ON COLUMN user_notification.content IS '内容';
COMMENT ON COLUMN user_notification.is_read IS '是否已读';
COMMENT ON COLUMN user_notification.read_time IS '阅读时间';
COMMENT ON COLUMN user_notification.biz_ref_type IS '关联业务类型（RECHARGE_ORDER/AI_USAGE）';
COMMENT ON COLUMN user_notification.biz_ref_id IS '关联业务ID';
COMMENT ON COLUMN user_notification.create_time IS '创建时间';
COMMENT ON COLUMN user_notification.update_time IS '更新时间';

-- 删除已存在的表
DROP TABLE IF EXISTS audio_file_record;

-- 创建音频文件记录表
CREATE TABLE audio_file_record (
    id BIGSERIAL PRIMARY KEY,
    req_id VARCHAR(64) NOT NULL UNIQUE,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    voice_type VARCHAR(100),
    text_content TEXT,
    synthesis_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    error_message TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64)
);

-- 索引
CREATE INDEX idx_audio_file_record_req_id ON audio_file_record(req_id);
CREATE INDEX idx_audio_file_record_create_time ON audio_file_record(create_time);
CREATE INDEX idx_audio_file_record_status ON audio_file_record(status);

-- 注释
COMMENT ON TABLE audio_file_record IS '音频文件记录表';
COMMENT ON COLUMN audio_file_record.id IS '主键ID';
COMMENT ON COLUMN audio_file_record.req_id IS '请求ID';
COMMENT ON COLUMN audio_file_record.file_name IS '文件名';
COMMENT ON COLUMN audio_file_record.file_path IS '文件存储路径';
COMMENT ON COLUMN audio_file_record.file_size IS '文件大小（字节）';
COMMENT ON COLUMN audio_file_record.file_type IS '文件类型（mp3/wav/pcm等）';
COMMENT ON COLUMN audio_file_record.voice_type IS '音色类型';
COMMENT ON COLUMN audio_file_record.text_content IS '合成文本内容';
COMMENT ON COLUMN audio_file_record.synthesis_type IS '合成类型（HTTP/ASYNC/STREAM）';
COMMENT ON COLUMN audio_file_record.status IS '状态（SUCCESS/FAILED/PROCESSING）';
COMMENT ON COLUMN audio_file_record.error_message IS '错误信息';
COMMENT ON COLUMN audio_file_record.create_time IS '创建时间';
COMMENT ON COLUMN audio_file_record.update_time IS '更新时间';
COMMENT ON COLUMN audio_file_record.created_by IS '创建人';
COMMENT ON COLUMN audio_file_record.updated_by IS '更新人';


--用户初始化
INSERT INTO user_info (id, user_name, pen_name, user_password, user_email, user_phone, user_status, is_admin, last_login_time, create_time, update_time, res_state) VALUES (1, 'admin', '管理员', '$2a$10$5YZZPtjmXHpvECf4fFQFNulMrOskCta0cbdp1/kTAkYvFT9n7FSIi', 'admin@aicreation.com', '13800138000', 1, TRUE, '2026-03-06 14:12:19.466373', '2025-08-17 07:09:51.962571', '2026-03-06 14:12:19.472975', 1);
INSERT INTO user_info (id, user_name, pen_name, user_password, user_email, user_phone, user_status, is_admin, last_login_time, create_time, update_time, res_state) VALUES (2, 'test_user', '测试用户', '$2a$10$5YZZPtjmXHpvECf4fFQFNulMrOskCta0cbdp1/kTAkYvFT9n7FSIi', 'test@aicreation.com', '13800138001', 1, FALSE, null, '2025-08-17 07:09:51.962571', '2025-08-20 10:08:02.381484', 1);
INSERT INTO user_info (id, user_name, pen_name, user_password, user_email, user_phone, user_status, is_admin, last_login_time, create_time, update_time, res_state) VALUES (3, 'demo_user', '演示用户', '$2a$10$5YZZPtjmXHpvECf4fFQFNulMrOskCta0cbdp1/kTAkYvFT9n7FSIi', 'demo@aicreation.com', '13800138002', 1, FALSE, '2025-08-18 09:42:17.969563', '2025-08-17 07:09:51.962571', '2025-08-20 10:08:02.381484', 1);
--字段初始化
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (1, '文章特点', '搞笑', 1, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (2, '文章特点', '失落', 2, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (3, '文章特点', '多女主', 3, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (4, '文章特点', '贴近生活', 4, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (5, '文章特点', '温馨', 5, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (6, '文章特点', '励志', 6, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (7, '文章特点', '悬疑', 7, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (8, '文章特点', '恐怖', 8, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (9, '文章特点', '科幻', 9, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (10, '文章特点', '奇幻', 10, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (11, '文章特点', '言情', 11, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (12, '文章特点', '武侠', 12, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (13, '文章特点', '历史', 13, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (14, '文章特点', '现实主义', 14, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (15, '文章特点', '魔幻现实主义', 15, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (16, '文章特点', '后现代', 16, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (17, '文章特点', '意识流', 17, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (18, '文章特点', '第一人称', 18, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (19, '文章特点', '第三人称', 19, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (20, '文章特点', '全知视角', 20, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (21, '文章特点', '限制视角', 21, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (22, '文章特点', '倒叙', 22, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (23, '文章特点', '插叙', 23, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (24, '文章特点', '预叙', 24, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (25, '文章特点', '讽刺', 25, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (26, '文章特点', '幽默', 26, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (27, '文章特点', '悲剧', 27, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (28, '文章特点', '喜剧', 28, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (29, '文章特点', '荒诞', 29, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (30, '文章特点', '象征主义', 30, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (31, '文章特点', '现实主义', 31, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (32, '文章特点', '浪漫主义', 32, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (33, '文章特点', '自然主义', 33, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (34, '文章特点', '存在主义', 34, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (35, '文章特点', '人文主义', 35, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (36, '文章特点', '女性主义', 36, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (37, '文章特点', '后殖民主义', 37, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (38, '文章特点', '生态文学', 38, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (39, '文章特点', '都市文学', 39, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (40, '文章特点', '乡村文学', 40, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (41, '文章特点', '校园文学', 41, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (42, '文章特点', '职场文学', 42, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (43, '文章特点', '家庭文学', 43, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (44, '文章特点', '青春文学', 44, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (45, '文章特点', '中老年文学', 45, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (46, '文章特点', '儿童文学', 46, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (47, '文章特点', '科普文学', 47, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (48, '文章特点', '传记文学', 48, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (49, '文章特点', '游记文学', 49, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (50, '文章特点', '散文', 50, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (51, '文章特点', '诗歌', 51, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (52, '文章特点', '戏剧', 52, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (53, '文章特点', '小说', 53, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (54, '文章特点', '短篇小说', 54, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (55, '文章特点', '中篇小说', 55, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (56, '文章特点', '长篇小说', 56, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (57, '文章特点', '系列小说', 57, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (58, '文章特点', '推理小说', 58, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (59, '文章特点', '惊悚小说', 59, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (60, '文章特点', '爱情小说', 60, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (61, '文章特点', '历史小说', 61, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (62, '文章特点', '科幻小说', 62, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (63, '文章特点', '奇幻小说', 63, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (64, '文章特点', '武侠小说', 64, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (65, '文章特点', '修仙小说', 65, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (66, '文章特点', '玄幻小说', 66, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (67, '文章特点', '都市修仙', 67, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (68, '文章特点', '系统流', 68, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (69, '文章特点', '重生流', 69, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (70, '文章特点', '穿越流', 70, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (71, '文章特点', '爽文', 71, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (72, '文章特点', '虐文', 72, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (73, '文章特点', '甜文', 73, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (74, '文章特点', '慢热', 74, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (75, '文章特点', '快节奏', 75, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (76, '文章特点', '高甜', 76, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (77, '文章特点', '狗血', 77, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (78, '文章特点', '玛丽苏', 78, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (79, '文章特点', '龙傲天', 79, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (80, '文章特点', '反套路', 80, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (81, '文章特点', 'HE', 81, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (82, '文章特点', 'BE', 82, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (83, '文章特点', '开放式结局', 83, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (84, '文章特点', '团圆结局', 84, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (85, '文章特点', '悲剧结局', 85, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (86, '文章特点', '温馨结局', 86, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (87, '文章特点', '治愈系', 87, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (88, '文章特点', '黑暗系', 88, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (89, '文章特点', '光明系', 89, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (90, '文章特点', '中二病', 90, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (91, '文章特点', '沙雕', 91, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (92, '文章特点', '无厘头', 92, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (93, '文章特点', '吐槽', 93, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (94, '文章特点', '自嘲', 94, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (95, '文章特点', '自黑', 95, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (96, '文章特点', '傲娇', 96, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (97, '文章特点', '傲慢', 97, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (98, '文章特点', '傲气', 98, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (99, '文章特点', '傲视群雄', 99, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (100, '文章特点', '傲视天下', 100, 1, '2026-02-28 10:33:18.961490', '2026-02-28 10:33:18.961490');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (301, '性别分类', '男生小说', 1, 1, '2026-03-02 10:48:50.764557', '2026-03-02 10:48:50.764557');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (302, '性别分类', '女生小说', 2, 1, '2026-03-02 10:48:50.764557', '2026-03-02 10:48:50.764557');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (303, '性别分类', '通用小说', 3, 1, '2026-03-02 10:48:50.764557', '2026-03-02 10:48:50.764557');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (304, '题材分类', '仙侠', 1, 1, '2026-03-02 10:48:50.765707', '2026-03-02 10:48:50.765707');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (305, '题材分类', '玄幻', 2, 1, '2026-03-02 10:48:50.765707', '2026-03-02 10:48:50.765707');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (306, '题材分类', '武侠', 3, 1, '2026-03-02 10:48:50.765707', '2026-03-02 10:48:50.765707');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (307, '题材分类', '都市', 4, 1, '2026-03-02 10:48:50.765707', '2026-03-02 10:48:50.765707');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (308, '题材分类', '历史', 5, 1, '2026-03-02 10:48:50.765707', '2026-03-02 10:48:50.765707');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (309, '题材分类', '科幻', 6, 1, '2026-03-02 10:48:50.765707', '2026-03-02 10:48:50.765707');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (310, '题材分类', '悬疑', 7, 1, '2026-03-02 10:48:50.765707', '2026-03-02 10:48:50.765707');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (311, '题材分类', '游戏', 8, 1, '2026-03-02 10:48:50.765707', '2026-03-02 10:48:50.765707');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (312, '题材分类', '奇幻', 9, 1, '2026-03-02 10:48:50.765707', '2026-03-02 10:48:50.765707');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (313, '题材分类', '军事', 10, 1, '2026-03-02 10:48:50.765707', '2026-03-02 10:48:50.765707');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (314, '题材分类', '体育', 11, 1, '2026-03-02 10:48:50.765707', '2026-03-02 10:48:50.765707');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (315, '题材分类', '竞技', 12, 1, '2026-03-02 10:48:50.765707', '2026-03-02 10:48:50.765707');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (316, '题材分类', '修仙', 13, 1, '2026-03-02 10:48:50.765707', '2026-03-02 10:48:50.765707');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (317, '题材分类', '修真', 14, 1, '2026-03-02 10:48:50.765707', '2026-03-02 10:48:50.765707');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (318, '题材分类', '灵异', 15, 1, '2026-03-02 10:48:50.765707', '2026-03-02 10:48:50.765707');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (319, '题材分类', '恐怖', 16, 1, '2026-03-02 10:48:50.765707', '2026-03-02 10:48:50.765707');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (320, '题材分类', '惊悚', 17, 1, '2026-03-02 10:48:50.765707', '2026-03-02 10:48:50.765707');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (321, '题材分类', '侦探', 18, 1, '2026-03-02 10:48:50.765707', '2026-03-02 10:48:50.765707');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (322, '题材分类', '推理', 19, 1, '2026-03-02 10:48:50.765707', '2026-03-02 10:48:50.765707');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (323, '题材分类', '冒险', 20, 1, '2026-03-02 10:48:50.765707', '2026-03-02 10:48:50.765707');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (324, '情节分类', '升级', 1, 1, '2026-03-02 10:48:50.767553', '2026-03-02 10:48:50.767553');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (325, '情节分类', '学院', 2, 1, '2026-03-02 10:48:50.767553', '2026-03-02 10:48:50.767553');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (326, '情节分类', '人生赢家', 3, 1, '2026-03-02 10:48:50.767553', '2026-03-02 10:48:50.767553');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (327, '情节分类', '剑道', 4, 1, '2026-03-02 10:48:50.767553', '2026-03-02 10:48:50.767553');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (328, '情节分类', '复仇', 5, 1, '2026-03-02 10:48:50.767553', '2026-03-02 10:48:50.767553');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (329, '情节分类', '重生', 6, 1, '2026-03-02 10:48:50.767553', '2026-03-02 10:48:50.767553');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (330, '情节分类', '穿越', 7, 1, '2026-03-02 10:48:50.767553', '2026-03-02 10:48:50.767553');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (331, '情节分类', '系统', 8, 1, '2026-03-02 10:48:50.767553', '2026-03-02 10:48:50.767553');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (332, '情节分类', '金手指', 9, 1, '2026-03-02 10:48:50.767553', '2026-03-02 10:48:50.767553');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (333, '情节分类', '开挂', 10, 1, '2026-03-02 10:48:50.767553', '2026-03-02 10:48:50.767553');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (334, '情节分类', '崛起', 11, 1, '2026-03-02 10:48:50.767553', '2026-03-02 10:48:50.767553');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (335, '情节分类', '逆袭', 12, 1, '2026-03-02 10:48:50.767553', '2026-03-02 10:48:50.767553');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (336, '情节分类', '奋斗', 13, 1, '2026-03-02 10:48:50.767553', '2026-03-02 10:48:50.767553');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (337, '情节分类', '创业', 14, 1, '2026-03-02 10:48:50.767553', '2026-03-02 10:48:50.767553');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (338, '情节分类', '恋爱', 15, 1, '2026-03-02 10:48:50.767553', '2026-03-02 10:48:50.767553');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (339, '情节分类', '修仙', 16, 1, '2026-03-02 10:48:50.767553', '2026-03-02 10:48:50.767553');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (340, '情节分类', '夺嫡', 17, 1, '2026-03-02 10:48:50.767553', '2026-03-02 10:48:50.767553');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (341, '情节分类', '争霸', 18, 1, '2026-03-02 10:48:50.767553', '2026-03-02 10:48:50.767553');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (342, '情节分类', '守护', 19, 1, '2026-03-02 10:48:50.767553', '2026-03-02 10:48:50.767553');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (343, '情节分类', '探险', 20, 1, '2026-03-02 10:48:50.767553', '2026-03-02 10:48:50.767553');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (344, '角色分类', '主角光环', 1, 1, '2026-03-02 10:48:50.770190', '2026-03-02 10:48:50.770190');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (345, '角色分类', '龙傲天', 2, 1, '2026-03-02 10:48:50.770190', '2026-03-02 10:48:50.770190');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (346, '角色分类', '玛丽苏', 3, 1, '2026-03-02 10:48:50.770190', '2026-03-02 10:48:50.770190');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (347, '角色分类', '反派', 4, 1, '2026-03-02 10:48:50.770190', '2026-03-02 10:48:50.770190');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (348, '角色分类', '配角', 5, 1, '2026-03-02 10:48:50.770190', '2026-03-02 10:48:50.770190');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (349, '角色分类', '搞笑角色', 6, 1, '2026-03-02 10:48:50.770190', '2026-03-02 10:48:50.770190');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (350, '角色分类', '智者', 7, 1, '2026-03-02 10:48:50.770190', '2026-03-02 10:48:50.770190');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (351, '角色分类', '勇者', 8, 1, '2026-03-02 10:48:50.770190', '2026-03-02 10:48:50.770190');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (352, '角色分类', '法师', 9, 1, '2026-03-02 10:48:50.770190', '2026-03-02 10:48:50.770190');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (353, '角色分类', '战士', 10, 1, '2026-03-02 10:48:50.770190', '2026-03-02 10:48:50.770190');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (354, '角色分类', '召唤师', 11, 1, '2026-03-02 10:48:50.770190', '2026-03-02 10:48:50.770190');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (355, '角色分类', '刺客', 12, 1, '2026-03-02 10:48:50.770190', '2026-03-02 10:48:50.770190');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (356, '角色分类', '商人', 13, 1, '2026-03-02 10:48:50.770190', '2026-03-02 10:48:50.770190');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (357, '角色分类', '贵族', 14, 1, '2026-03-02 10:48:50.770190', '2026-03-02 10:48:50.770190');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (358, '角色分类', '平民', 15, 1, '2026-03-02 10:48:50.770190', '2026-03-02 10:48:50.770190');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (359, '风格分类', '热血', 1, 1, '2026-03-02 10:48:50.771110', '2026-03-02 10:48:50.771110');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (360, '风格分类', '温馨', 2, 1, '2026-03-02 10:48:50.771110', '2026-03-02 10:48:50.771110');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (361, '风格分类', '黑暗', 3, 1, '2026-03-02 10:48:50.771110', '2026-03-02 10:48:50.771110');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (362, '风格分类', '轻松', 4, 1, '2026-03-02 10:48:50.771110', '2026-03-02 10:48:50.771110');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (363, '风格分类', '沉重', 5, 1, '2026-03-02 10:48:50.771110', '2026-03-02 10:48:50.771110');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (364, '风格分类', '幽默', 6, 1, '2026-03-02 10:48:50.771110', '2026-03-02 10:48:50.771110');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (365, '风格分类', '严肃', 7, 1, '2026-03-02 10:48:50.771110', '2026-03-02 10:48:50.771110');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (366, '风格分类', '浪漫', 8, 1, '2026-03-02 10:48:50.771110', '2026-03-02 10:48:50.771110');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (367, '风格分类', '现实', 9, 1, '2026-03-02 10:48:50.771110', '2026-03-02 10:48:50.771110');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (368, '风格分类', '奇幻', 10, 1, '2026-03-02 10:48:50.771110', '2026-03-02 10:48:50.771110');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (369, '风格分类', '史诗', 11, 1, '2026-03-02 10:48:50.771110', '2026-03-02 10:48:50.771110');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (370, '风格分类', '传奇', 12, 1, '2026-03-02 10:48:50.771110', '2026-03-02 10:48:50.771110');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (371, '风格分类', '励志', 13, 1, '2026-03-02 10:48:50.771110', '2026-03-02 10:48:50.771110');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (372, '风格分类', '治愈', 14, 1, '2026-03-02 10:48:50.771110', '2026-03-02 10:48:50.771110');
INSERT INTO dictionary (id, dict_key, dict_value, sort_order, res_state, create_time, update_time) VALUES (373, '风格分类', '爽文', 15, 1, '2026-03-02 10:48:50.771110', '2026-03-02 10:48:50.771110');

-- =====================================================================
-- 自增主键序列：下一值 >= 10000 且 > 当前表内最大 id（避免显式 INSERT 后序列滞后导致主键冲突）
-- =====================================================================
DO $$
DECLARE
  tbl text;
BEGIN
  FOREACH tbl IN ARRAY ARRAY[
    'article',
    'article_chapter',
    'article_generation_config',
    'dictionary',
    'plot',
    'user_info',
    'audio_file_record',
    'user_wallet',
    'wallet_ledger',
    'ai_usage_billing',
    'recharge_order',
    'user_notification',
    'membership_pricing_config',
    'membership_subscription'
  ]
  LOOP
    EXECUTE format(
      'SELECT setval(pg_get_serial_sequence(%L, ''id''), GREATEST(COALESCE((SELECT MAX(id) FROM %I), 0), 9999), true)',
      tbl,
      tbl
    );
  END LOOP;
END $$;
