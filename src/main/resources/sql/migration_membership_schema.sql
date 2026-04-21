-- 会员体系：用户字段、定价配置、开通记录
-- 依赖：已执行 migration_recharge_order_unified_payment.sql

-- 1) 用户表扩展
ALTER TABLE user_info
    ADD COLUMN IF NOT EXISTS membership_tier VARCHAR(32) NOT NULL DEFAULT 'NONE';
ALTER TABLE user_info
    ADD COLUMN IF NOT EXISTS membership_start_at TIMESTAMP;
ALTER TABLE user_info
    ADD COLUMN IF NOT EXISTS membership_end_at TIMESTAMP;

COMMENT ON COLUMN user_info.membership_tier IS '会员等级：NONE/BASIC';
COMMENT ON COLUMN user_info.membership_start_at IS '首次成为会员时间';
COMMENT ON COLUMN user_info.membership_end_at IS '当前会员权益截止时间';

CREATE INDEX IF NOT EXISTS idx_user_info_membership_end ON user_info(membership_end_at);

UPDATE user_info SET membership_tier = 'NONE' WHERE membership_tier IS NULL;

-- 2) 会员定价
CREATE TABLE IF NOT EXISTS membership_pricing_config (
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

-- 3) 开通记录
CREATE TABLE IF NOT EXISTS membership_subscription (
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
CREATE UNIQUE INDEX IF NOT EXISTS uk_membership_sub_payment_order
    ON membership_subscription(payment_order_id)
    WHERE payment_order_id IS NOT NULL;

COMMENT ON TABLE membership_subscription IS '会员开通/变更/退款审计记录';

-- 4) 默认定价（9900 分/月，各档位折扣示例）
INSERT INTO membership_pricing_config (tier, duration_months, base_month_price_cent, discount_rate, enabled, sort_order)
SELECT 'BASIC', v.m, 9900, v.d, TRUE, v.s
FROM (VALUES
    (1,  1.0000, 1),
    (3,  0.9800, 2),
    (6,  0.9500, 3),
    (12, 0.9200, 4)
) AS v(m, d, s)
WHERE NOT EXISTS (SELECT 1 FROM membership_pricing_config c WHERE c.tier = 'BASIC' AND c.duration_months = v.m);
