-- 将 recharge_order 演进为统一支付订单表：支持 RECHARGE（余额充值）与 MEMBERSHIP（会员）等类型。
-- 物理表名保持不变，便于兼容已有数据与代码命名。

ALTER TABLE recharge_order
    ADD COLUMN IF NOT EXISTS biz_type VARCHAR(32) NOT NULL DEFAULT 'RECHARGE';

ALTER TABLE recharge_order
    ADD COLUMN IF NOT EXISTS membership_pricing_config_id BIGINT;

ALTER TABLE recharge_order
    ADD COLUMN IF NOT EXISTS membership_duration_months SMALLINT;

COMMENT ON TABLE recharge_order IS '统一支付订单（充值、会员等）';
COMMENT ON COLUMN recharge_order.biz_type IS '业务类型：RECHARGE=余额充值，MEMBERSHIP=会员购买';
COMMENT ON COLUMN recharge_order.membership_pricing_config_id IS '会员订单关联的定价配置ID（biz_type=MEMBERSHIP 时使用）';
COMMENT ON COLUMN recharge_order.membership_duration_months IS '会员订单购买时长（月），冗余便于展示与对账';

CREATE INDEX IF NOT EXISTS idx_recharge_order_biz_type ON recharge_order(biz_type);
