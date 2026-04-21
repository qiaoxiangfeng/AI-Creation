# 钱包、AI计费与消息通知设计文档（支付宝单通道优先）

## 1. 背景与目标

### 1.1 背景
当前系统已具备用户、文章、文章生成配置、AI生成任务与基础权限能力，但尚未形成完整的“充值 -> 使用AI -> 扣费 -> 通知”的商业闭环。

### 1.2 目标
- 新增用户余额账户能力，支持余额查询、冻结、扣减、解冻。
- 新增 AI 使用计费能力，记录每次调用成本并进行结算。
- 新增用户消息通知能力，支持右上角未读提醒与消息列表。
- 支持支付充值（第一阶段优先对接支付宝，微信后续补充）。
- 当用户可用余额小于等于 0 时，禁止触发文章相关 AI 功能。

### 1.3 非目标（当前阶段不做）
- 不实现营销体系（优惠券、活动赠送、会员等级）。
- 不实现跨币种与多地区税务处理。
- 不实现复杂分账能力。

## 2. 术语定义

- `总余额(total_balance)`：用户累计可记账资产（单位：分）。
- `冻结余额(frozen_balance)`：已预占、未结算的金额（单位：分）。
- `可用余额(available_balance)`：可立即用于 AI 消费的金额，建议满足 `available = total - frozen`。
- `预占(pre-auth)`：AI 调用前先冻结一笔预计金额，避免超用。
- `结算(settlement)`：AI 调用结束后按实际成本扣款并退回差额。
- `流水(ledger)`：资金变动明细，作为审计与对账依据。

## 3. 总体方案

### 3.1 架构原则
- 资金类数据以“流水”为准，余额表作为高频查询快照。
- 所有扣费链路必须幂等，防止重复扣款。
- 支付以“异步回调成功”作为到账依据，不以前端跳转结果为准。
- 采用“实时结算 + 定时补偿任务”，避免纯定时扣费带来的风控风险。

### 3.2 业务闭环
1. 用户发起充值订单。
2. 用户完成支付，支付平台回调成功。
3. 系统入账并写充值流水，发送“充值成功”通知。
4. 用户发起 AI 功能时先进行余额门禁与预占。
5. AI 调用结束后进行结算：扣实际金额、退多余冻结、写消费流水。
6. 若余额不足，拒绝 AI 请求并发送“余额不足”通知。

## 4. 数据模型设计

> 统一约定：金额字段均使用 `BIGINT`（单位：分），避免浮点误差。

### 4.1 用户钱包表 `user_wallet`

用途：一用户一账户，存放当前余额快照。

建议字段：
- `id BIGSERIAL PRIMARY KEY`
- `user_id BIGINT NOT NULL UNIQUE`
- `total_balance_cent BIGINT NOT NULL DEFAULT 0`
- `frozen_balance_cent BIGINT NOT NULL DEFAULT 0`
- `available_balance_cent BIGINT NOT NULL DEFAULT 0`
- `version INTEGER NOT NULL DEFAULT 0`（乐观锁版本号）
- `res_state SMALLINT DEFAULT 1`
- `create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP`
- `update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP`

建议索引：
- `uk_user_wallet_user_id (user_id)`

### 4.2 钱包流水表 `wallet_ledger`

用途：记录所有资金变化，支持审计、追踪、对账。

建议字段：
- `id BIGSERIAL PRIMARY KEY`
- `user_id BIGINT NOT NULL`
- `biz_type VARCHAR(32) NOT NULL`
  - `RECHARGE`
  - `AI_PRE_AUTH`
  - `AI_SETTLE_DEBIT`
  - `AI_SETTLE_REFUND`
  - `MANUAL_ADJUST`
- `direction VARCHAR(16) NOT NULL`
  - `IN`
  - `OUT`
  - `FREEZE`
  - `UNFREEZE`
- `amount_cent BIGINT NOT NULL`（可定义为正值配合方向）
- `balance_before_cent BIGINT`
- `balance_after_cent BIGINT`
- `related_biz_type VARCHAR(32)`（如 `RECHARGE_ORDER` / `AI_USAGE`）
- `related_biz_id BIGINT`
- `idempotency_key VARCHAR(128) NOT NULL`
- `remark VARCHAR(500)`
- `create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP`

建议索引：
- `idx_wallet_ledger_user_id_create_time (user_id, create_time DESC)`
- `uk_wallet_ledger_idempotency_key (idempotency_key)`
- `idx_wallet_ledger_related_biz (related_biz_type, related_biz_id)`

### 4.3 AI 计费表 `ai_usage_billing`

用途：记录每次 AI 调用的消耗、状态、结算结果。

建议字段：
- `id BIGSERIAL PRIMARY KEY`
- `user_id BIGINT NOT NULL`
- `article_id BIGINT`
- `chapter_id BIGINT`
- `biz_scene VARCHAR(64) NOT NULL`
  - `GENERATE_CHAPTERS`
  - `GENERATE_CHAPTER_CONTENT`
  - `REGENERATE_CHAPTER_CONTENT`
  - `REFINE_OUTLINE`
- `provider VARCHAR(32) NOT NULL`（如 `VOLCENGINE`）
- `model_name VARCHAR(64)`
- `request_tokens INTEGER DEFAULT 0`
- `response_tokens INTEGER DEFAULT 0`
- `total_tokens INTEGER DEFAULT 0`
- `estimated_cost_cent BIGINT DEFAULT 0`
- `actual_cost_cent BIGINT DEFAULT 0`
- `pre_auth_amount_cent BIGINT DEFAULT 0`
- `settled_amount_cent BIGINT DEFAULT 0`
- `refund_amount_cent BIGINT DEFAULT 0`
- `status VARCHAR(32) NOT NULL`
  - `INIT`
  - `PRE_AUTHED`
  - `SUCCESS`
  - `FAILED`
  - `SETTLED`
  - `CANCELED`
- `trace_id VARCHAR(64)`
- `idempotency_key VARCHAR(128) NOT NULL`
- `error_message VARCHAR(1000)`
- `create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP`
- `update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP`

建议索引：
- `idx_ai_usage_user_id_create_time (user_id, create_time DESC)`
- `idx_ai_usage_article_id (article_id)`
- `uk_ai_usage_idempotency_key (idempotency_key)`
- `idx_ai_usage_status (status)`

### 4.4 充值订单表 `recharge_order`

用途：承载支付下单、回调、查单状态。

建议字段：
- `id BIGSERIAL PRIMARY KEY`
- `order_no VARCHAR(64) NOT NULL UNIQUE`（商户订单号）
- `user_id BIGINT NOT NULL`
- `channel VARCHAR(16) NOT NULL`（`ALIPAY` / `WECHAT`）
- `amount_cent BIGINT NOT NULL`
- `status VARCHAR(32) NOT NULL`
  - `CREATED`
  - `PAYING`
  - `PAID`
  - `CLOSED`
  - `FAILED`
- `subject VARCHAR(128)`（订单标题）
- `pay_trade_no VARCHAR(128)`（平台交易号）
- `pay_url TEXT`（二维码URL或跳转URL）
- `expire_time TIMESTAMP`
- `paid_time TIMESTAMP`
- `callback_time TIMESTAMP`
- `callback_payload TEXT`（建议脱敏后存储）
- `idempotency_key VARCHAR(128) NOT NULL`
- `create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP`
- `update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP`

建议索引：
- `uk_recharge_order_order_no (order_no)`
- `idx_recharge_order_user_id_create_time (user_id, create_time DESC)`
- `idx_recharge_order_status (status)`
- `uk_recharge_order_idempotency_key (idempotency_key)`

### 4.5 用户消息通知表 `user_notification`

用途：站内通知展示与已读状态管理。

建议字段：
- `id BIGSERIAL PRIMARY KEY`
- `user_id BIGINT NOT NULL`
- `type VARCHAR(32) NOT NULL`
  - `BALANCE_LOW`
  - `RECHARGE_SUCCESS`
  - `AI_CHARGE`
  - `AI_REFUND`
  - `SYSTEM`
- `title VARCHAR(128) NOT NULL`
- `content VARCHAR(1000) NOT NULL`
- `is_read BOOLEAN NOT NULL DEFAULT FALSE`
- `read_time TIMESTAMP`
- `biz_ref_type VARCHAR(32)`（`RECHARGE_ORDER` / `AI_USAGE`）
- `biz_ref_id BIGINT`
- `create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP`
- `update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP`

建议索引：
- `idx_user_notification_user_id_create_time (user_id, create_time DESC)`
- `idx_user_notification_user_id_is_read (user_id, is_read)`

## 5. 关键业务流程

### 5.1 充值流程（支付宝优先）
1. 前端调用“创建充值订单”接口，输入金额。
2. 后端校验金额阈值与频率限制，创建 `recharge_order`。
3. 后端调用支付宝下单接口，返回支付链接/二维码。
4. 用户支付成功后，支付宝异步回调业务接口。
5. 后端验签与幂等处理，更新订单为 `PAID`。
6. 后端钱包入账：更新 `user_wallet` + 写 `wallet_ledger(RECHARGE)`。
7. 创建 `user_notification(RECHARGE_SUCCESS)`。

### 5.2 AI 计费流程（预占 + 结算）
1. 用户发起 AI 请求（如生成章节、重写章节、大纲修订）。
2. 后端鉴权并校验可用余额 `available_balance > 0`。
3. 按模型和请求规模估算预占金额。
4. 钱包预占：`frozen +x`、`available -x`，写 `AI_PRE_AUTH` 流水。
5. 执行 AI 调用并记录 token 用量。
6. 计算实际成本并结算：
   - `实际 = 预占`：仅转为扣款。
   - `实际 < 预占`：扣实际并解冻差额。
   - `实际 > 预占`：可配置为补扣（若可用不足则记欠费/中断后续AI）。
7. 写 `ai_usage_billing` 最终状态与消费通知。

### 5.3 余额不足门禁
- 当 `available_balance_cent <= 0`：
  - 拒绝 AI 相关接口。
  - 返回明确错误码（建议新增 `BALANCE_INSUFFICIENT`）。
  - 可按频控创建 `BALANCE_LOW` 通知，避免刷屏。

### 5.4 定时补偿与对账
- `recharge-reconcile-task`（5~15分钟）：
  - 处理订单状态异常（创建后长期未回调，可主动查单）。
  - 处理重复回调与漏入账情况。
- `billing-settlement-repair-task`（5~15分钟）：
  - 修复 `PRE_AUTHED` 长时间未结算记录。
  - 失败场景自动解冻。

## 6. 接口设计（先定义，后实现）

### 6.0 域名、路径与回调地址规划（talex.top）

已备案业务域名：`http://www.talex.top/`（控制台/业务前端入口）。

为便于支付宝“当面付”接入与后续运维，建议在同一主域名下规划**固定的业务路径**（第一阶段不强制拆分子域名）：

- **充值/收银台页面（前端）**：`http(s)://www.talex.top/recharge`
  - 用途：展示充值金额选择、生成二维码、轮询订单状态等。
  - 备注：当面付通常以“二维码扫码”完成支付，前端无需跳转到支付宝收银台。

- **创建充值订单（后端接口，经网关转发）**：`POST http(s)://www.talex.top/api/wallet/recharge/create`
  - 返回 `payUrl`（二维码内容或用于生成二维码的 URL）

- **支付宝异步通知（后端回调，公网可访问）**：`POST http(s)://www.talex.top/api/wallet/recharge/callback/alipay`
  - 用途：接收支付宝支付结果通知并验签、入账。
  - 要求：必须在生产环境具备公网可访问性与稳定性；严禁使用本地 `localhost`。

建议（上线前）升级为 HTTPS 并统一使用 `https://www.talex.top/...`：
- 支付回调链路更稳定；也便于后续安全策略（HSTS、WAF 等）。

### 6.1 钱包与充值接口
- `POST /wallet/recharge/create`
  - 入参：`amountCent`、`channel`
  - 出参：`orderNo`、`payUrl`、`expireTime`
- `GET /wallet/recharge/{orderNo}`
  - 出参：订单状态与支付信息
- `POST /wallet/recharge/callback/alipay`
  - 支付异步回调（公网可访问）
- `GET /wallet/balance`
  - 出参：`totalBalanceCent`、`frozenBalanceCent`、`availableBalanceCent`
- `POST /wallet/ledger/list`
  - 分页查询余额流水

### 6.2 消息通知接口
- `POST /notifications/list`
  - 分页查询消息
- `GET /notifications/unread-count`
  - 查询未读数量（用于右上角红点）
- `POST /notifications/read`
  - 单条已读
- `POST /notifications/read-all`
  - 全部已读

### 6.3 AI 业务接口改造（门禁与计费）
- 文章相关 AI 接口在进入业务前，统一增加：
  - `assertAiUsable(userId)`
  - `preAuth()`
  - 执行后 `settle()`

## 7. 与现有系统集成方案

### 7.1 鉴权与权限
- 复用现有会话鉴权与 `CurrentUserHolder`。
- 继续复用 `AccessControlService` 的资源归属校验。
- 新增 `BillingAccessService` 处理余额门禁（只负责“能否用AI”）。

### 7.2 后端分层建议
- `controller`
  - `WalletController`
  - `PaymentController`
  - `NotificationController`
- `service`
  - `WalletService`
  - `BillingService`
  - `RechargeService`
  - `NotificationService`
- `external`
  - `AlipayClient`
- `task`
  - `RechargeReconcileTask`
  - `BillingSettlementRepairTask`

### 7.3 前端改造点（仅设计）
- 右上角增加余额展示与消息铃铛（未读红点）。
- 新增充值弹窗（金额、订单状态轮询）。
- AI 触发按钮处增加余额不足禁用态与提示文案。
- 新增“账单/流水”页面（可后置到第二阶段）。

## 8. 错误码与提示文案建议

在现有错误码体系基础上增加：
- `BALANCE_INSUFFICIENT`：余额不足，无法使用AI功能
- `RECHARGE_ORDER_NOT_FOUND`：充值订单不存在
- `PAY_CALLBACK_INVALID`：支付回调验签失败
- `PAY_AMOUNT_MISMATCH`：支付金额不匹配
- `BILLING_PREAUTH_FAILED`：预占失败
- `BILLING_SETTLEMENT_FAILED`：结算失败

前端提示建议：
- 余额不足：`当前可用余额不足，请先充值后继续使用 AI 功能`
- 充值成功：`充值成功，余额已更新`
- 扣费通知：`本次 AI 使用扣费 X 元（详情可在账单中查看）`

## 9. 幂等、并发与一致性设计

### 9.1 幂等键策略
- 充值下单：`userId + clientRequestId`
- 支付回调：`channel + payTradeNo + tradeStatus`
- AI计费：`scene + bizId + requestTraceId`

### 9.2 事务边界建议
- 钱包变更与流水写入必须在同一事务内。
- 订单支付成功更新与钱包入账在同一事务内（或采用本地消息保证最终一致）。
- AI 结算允许最终一致，但必须有补偿任务兜底。

### 9.3 并发控制
- 钱包更新采用“悲观锁（`FOR UPDATE`）或乐观锁（`version`）”二选一。
- 对同一用户高并发 AI 调用建议串行化或限流，避免频繁冲突。

## 10. 安全与合规

- 支付回调必须验签，拒绝伪造请求。
- 回调接口仅允许必要字段，原文留档需脱敏。
- 严禁记录支付敏感信息（私钥、完整证件信息）。
- 订单金额、订单号、用户ID做强一致校验。
- 异常场景全链路日志需可追踪（`trace_id`）。

## 11. 监控与运维

建议新增指标：
- 充值成功率、回调成功率、订单超时率
- AI 调用成功率、结算成功率、预占失败率
- 余额不足拦截次数
- 消息未读量与发送失败量

建议告警：
- 支付回调连续失败
- 结算失败积压超过阈值
- 钱包余额出现负值

## 12. 分阶段落地计划

### Phase 1（最小可用）
- 钱包表 + 流水表 + AI 计费表 + 消息通知表
- AI 入口余额门禁与预占/结算
- 右上角通知与未读数
- 手工充值（后台加款）替代真实支付

### Phase 2（支付接入）
- 支付宝下单与回调
- 充值订单全流程
- 支付对账与补偿任务

### Phase 3（扩展能力）
- 微信支付接入
- 套餐、赠送金、活动营销
- 账单明细与财务报表

## 13. 风险与应对

- 风险：重复回调导致重复入账  
  应对：回调幂等键 + 唯一约束 + 事务校验

- 风险：AI 失败未解冻导致“余额被锁死”  
  应对：结算兜底 + 定时解冻补偿任务

- 风险：高并发下余额超扣  
  应对：钱包行锁/版本锁 + 统一资金服务入口

- 风险：用户体验问题（频繁余额不足）  
  应对：低余额阈值提前提醒 + 一键充值入口

## 14. 验收标准（Definition of Done）

- 用户可查看余额、可用余额、冻结余额。
- 用户可完成支付宝充值并自动到账（以回调成功为准）。
- 每次 AI 调用都有可追溯计费记录与流水。
- 余额小于等于 0 时，所有 AI 入口被稳定拦截。
- 用户可在主界面右上角查看未读通知并标记已读。
- 关键异常（回调重复、AI失败、结算失败）具备自动补偿能力。

## 15. 附录：建议状态枚举

### 15.1 充值订单状态
- `CREATED`：已创建未支付
- `PAYING`：支付中
- `PAID`：支付成功已入账
- `CLOSED`：超时关闭
- `FAILED`：支付失败

### 15.2 AI 计费状态
- `INIT`：记录初始化
- `PRE_AUTHED`：已预占
- `SUCCESS`：AI调用成功
- `FAILED`：AI调用失败
- `SETTLED`：已结算
- `CANCELED`：取消

### 15.3 消息类型
- `BALANCE_LOW`
- `RECHARGE_SUCCESS`
- `AI_CHARGE`
- `AI_REFUND`
- `SYSTEM`
