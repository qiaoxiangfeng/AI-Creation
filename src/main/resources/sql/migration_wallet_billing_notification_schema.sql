-- Wallet / Billing / Notification schema for user balance & AI usage billing
-- Includes:
-- 1) user_wallet
-- 2) wallet_ledger
-- 3) ai_usage_billing
-- 4) recharge_order
-- 5) user_notification

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
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_recharge_order_user_id_create_time ON recharge_order(user_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_recharge_order_status ON recharge_order(status);

COMMENT ON TABLE recharge_order IS '充值订单表';
COMMENT ON COLUMN recharge_order.order_no IS '商户订单号';
COMMENT ON COLUMN recharge_order.user_id IS '用户ID（关联 user_info.id）';
COMMENT ON COLUMN recharge_order.channel IS '充值渠道（ALIPAY/WECHAT）';
COMMENT ON COLUMN recharge_order.amount_cent IS '充值金额（分）';
COMMENT ON COLUMN recharge_order.status IS '状态（CREATED/PAYING/PAID/CLOSED/FAILED）';
COMMENT ON COLUMN recharge_order.subject IS '订单标题';
COMMENT ON COLUMN recharge_order.pay_trade_no IS '平台交易号';
COMMENT ON COLUMN recharge_order.pay_url IS '支付URL（二维码/跳转）';
COMMENT ON COLUMN recharge_order.expire_time IS '过期时间';
COMMENT ON COLUMN recharge_order.paid_time IS '支付完成时间';
COMMENT ON COLUMN recharge_order.callback_time IS '回调接收时间';
COMMENT ON COLUMN recharge_order.callback_payload IS '回调原始数据（建议脱敏）';
COMMENT ON COLUMN recharge_order.idempotency_key IS '幂等键（唯一）';
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

-- =====================================================================
-- Sequence start (avoid id collision with explicit inserts)
-- Next value >= 10000 (9999 baseline in init.sql)
-- =====================================================================
DO $$
DECLARE
  tbl text;
BEGIN
  FOREACH tbl IN ARRAY ARRAY[
    'user_wallet',
    'wallet_ledger',
    'ai_usage_billing',
    'recharge_order',
    'user_notification'
  ]
  LOOP
    EXECUTE format(
      'SELECT setval(pg_get_serial_sequence(%L, ''id''), GREATEST(COALESCE((SELECT MAX(id) FROM %I), 0), 9999), true)',
      tbl,
      tbl
    );
  END LOOP;
END $$;

