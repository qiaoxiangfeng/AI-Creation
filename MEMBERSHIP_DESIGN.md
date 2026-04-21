# 会员体系设计文档

> 版本：v0.1（草案）  
> 关联文档：`WALLET_BILLING_NOTIFICATION_DESIGN.md`（钱包与 AI 计费）  
> 本文约定与现有系统一致：**金额单位为分（BIGINT）**、**支付以异步回调成功为准**、**关键链路幂等**。

---

## 1. 背景与目标

### 1.1 背景

当前系统已实现「余额充值 → AI 预占/结算 → 流水与通知」。业务上需要增加 **会员权益**：仅会员可使用 AI，且仍按余额扣费（**会员门槛 + 余额扣费**，模式 A）。

### 1.2 目标

- 用户侧：支持 **支付宝 / 微信** 购买 **基础会员**，可选时长 **1 / 3 / 6 / 12 个月**；支持 **多次购买顺延** 会员结束时间。
- 定价：管理员配置 **基准月价** 与各时长 **折扣率**；成交价 = `round(月价 × 月数 × 折扣)`（分）。
- 门禁：**所有 AI 相关能力**在现有「余额判断」之前，先校验 **会员在有效期内**；校验逻辑 **集中封装**，便于维护。
- 到期：**会员到期后立即不可使用 AI**（与余额无关，先挡会员）。
- 退款：**退款成功后回滚**本次购买对应的会员结束时间（见规则章节）。
- 运营：管理员可 **手动赠送 / 延长** 会员。

### 1.3 非目标（当前阶段）

- 多档位会员（如专业版）；表中可预留 `tier`，实现上仅 **`BASIC`**。
- 会员包含免费 AI 额度（不与余额混用）；当前仍为 **余额扣费**。
- 复杂营销（优惠券叠加等）。

---

## 2. 术语

| 术语 | 含义 |
|------|------|
| 会员快照 | 用户表上当前会员等级与有效期，用于快速鉴权 |
| 顺延 | 新订单支付成功后，在 **当前结束时间之后** 累加时长；若已过期则从 **支付成功时刻** 起算时长 |
| 开通记录 | 每一次成功购买/赠送/管理员延长产生的审计明细（可选含退款冲正） |
| 统一支付订单 | 与充值共用同一回调地址，根据 **订单业务类型** 分发后续处理 |

---

## 3. 与现有系统的关系

### 3.1 门禁顺序（强制）

对 **所有需要扣余额的 AI 入口**（含 `AiBillingService.executeWithAiBilling` 调用链），顺序固定为：

1. **已登录**（已有）
2. **会员有效**：`now` 在 `[membership_start_at, membership_end_at)` 或约定闭开区间（实现时统一一种）
3. **余额充足**（现有预占逻辑）

建议新增统一方法（命名示例）：`MembershipAccessService.assertActiveMembership(userId)` 或并入 `AccessControlService`，由 **AI 入口唯一调用**，避免散落判断。

### 3.2 与充值、回调的关系

- **同一支付回调 URL**（如现有 `/wallet/recharge/callback/alipay`）：验签通过后，根据 **商户订单号** 查询统一订单表，按 **`biz_type`** 分支：
  - `RECHARGE`：走现有入账 + 流水 + 通知
  - `MEMBERSHIP`：走会员顺延 + 开通记录 + 通知（不写充值流水）

实现期可选：

- **方案 A（推荐）**：将现有 `recharge_order` **演进**为 **统一支付订单表**（可重命名为 `payment_order`，或保留表名增加字段），新增 `biz_type` 及会员相关可空字段。
- **方案 B**：新建 `membership_order`，回调内先查充值表再查会员表；**不推荐**（回调分发逻辑重复）。

下文数据模型按 **统一订单表 + biz_type** 描述；若保留表名 `recharge_order`，需在迁移脚本中增加字段并补充注释，避免语义混淆。

---

## 4. 核心业务规则

### 4.1 顺延算法

输入：支付成功时刻 `paid_at`，用户当前 `membership_end_at`（可为空表示从未开通），本次购买 `duration_months`。

- 若 `membership_end_at` 为空或 `membership_end_at <= paid_at`（已过期或未开始）：  
  `new_end = paid_at + duration_months`（按自然月或固定天数策略二选一，**实现阶段固定一种并写死文档**）。
- 若 `membership_end_at > paid_at`（仍在有效期内）：  
  `new_end = membership_end_at + duration_months`。

`membership_start_at`：仅在 **首次从「无会员」变为有会员** 时写入；后续顺延 **不重置** 开始时间（如需展示「当前周期开始」可另算，非必需）。

> 建议：**自然月**用数据库日期运算（如 PostgreSQL `+ interval '1 month'`）或 Java `Period`，避免 2 月天数问题；若产品要求「固定 30 天」需单独说明。

### 4.2 到期即失效

当 `now >= membership_end_at`（按约定边界）：**不允许** 使用任何依赖 `AiBillingService` 的 AI 能力；返回独立错误码（如 `MEMBERSHIP_EXPIRED`），前端提示开通/续费会员。

### 4.3 退款回滚结束时间

前提：支付平台支持退款且业务对接退款回调或主动查单。

- 每笔会员订单在 **开通记录** 中保存：`before_end_at`、`after_end_at`、关联 `order_id`。
- 退款成功且该订单 **曾将会员延长**：将用户 `membership_end_at` **恢复为 `before_end_at`**（若多笔顺延，仅回滚该笔对应区间，见下）。

**并发与顺序**：若用户存在多笔订单 A、B 依次延长，回滚中间一笔时，必须 **按订单维度** 存储「本单贡献的结束时间增量」或完整快照。推荐开通记录表存：

- `previous_end_at_snapshot`
- `new_end_at_snapshot`

退款时：若当前用户 `membership_end_at == new_end_at_snapshot`（一致才回滚，避免后续又买了 C 导致误伤），则置为 `previous_end_at_snapshot`；若不一致，需 **人工介入或标记异常**（文档级先写清 MVP：仅允许回滚「仍为该快照」的情况）。

### 4.4 管理员手动赠送 / 延长

- **赠送**（用户无付费订单）：等价于 `duration_months` 或固定 `new_end`，写 **开通记录** `source = ADMIN_GRANT`，不写支付订单或写 `amount_cent = 0` 的内部单（二选一，建议 **仅开通记录 + 审计日志**）。
- **延长**：与顺延类似，可视为 `paid_at = now` 的一次「0 元顺延」或管理员指定新结束时间（**指定结束时间**更直观，需校验 `new_end > old_end`）。

均需管理员权限，接口与操作日志审计。

---

## 5. 数据模型

### 5.1 用户表扩展 `user_info`（或现有用户表名）

| 字段 | 类型 | 说明 |
|------|------|------|
| `membership_tier` | VARCHAR(32) | `NONE` / `BASIC`（当前仅 BASIC） |
| `membership_start_at` | TIMESTAMP | 首次成为会员的起始（可选，可空） |
| `membership_end_at` | TIMESTAMP | **当前权益截止时间**；鉴权核心字段 |

索引：`(membership_end_at)` 可选，用于批量筛选将到期用户。

### 5.2 会员定价配置 `membership_pricing_config`

管理员可配置多条，按 **基础会员 + 时长档位** 唯一（当前仅一档会员）。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGSERIAL | 主键 |
| `tier` | VARCHAR(32) | 固定 `BASIC` |
| `duration_months` | SMALLINT | 1 / 3 / 6 / 12 |
| `base_month_price_cent` | BIGINT | 基准月价（分）；可与全局一致或按行覆盖 |
| `discount_rate` | DECIMAL(5,4) | 如 0.8500 表示 85 折；**成交价**见下 |
| `enabled` | BOOLEAN | 是否上架 |
| `sort_order` | INT | 展示排序 |
| `create_time` / `update_time` | TIMESTAMP | |

**成交价计算**（与产品一致）：

\[
\text{price\_cent} = \text{round}\bigl(\text{base\_month\_price\_cent} \times \text{duration\_months} \times \text{discount\_rate}\bigr)
\]

若全局仅一个「月价 99 元」，可简化为：所有行共用应用配置中的 `default_month_price_cent`，本表只存 `duration_months + discount_rate`；实现时二选一并在迁移中定稿。

唯一约束示例：`UNIQUE(tier, duration_months)`。

### 5.3 会员开通记录 `membership_subscription`（审计 / 退款依据）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGSERIAL | 主键 |
| `user_id` | BIGINT | 用户 |
| `source` | VARCHAR(32) | `PAYMENT` / `ADMIN_GRANT` / `ADMIN_EXTEND` / `REFUND_ROLLBACK`（可选） |
| `payment_order_id` | BIGINT | 可空；关联统一支付订单 |
| `tier` | VARCHAR(32) | `BASIC` |
| `duration_months` | SMALLINT | 本次增加月数（管理员直接指定结束时间时可空） |
| `previous_end_at` | TIMESTAMP | 变更前用户 `membership_end_at` 快照 |
| `new_end_at` | TIMESTAMP | 变更后 |
| `amount_cent` | BIGINT | 实付金额；赠送为 0 |
| `channel` | VARCHAR(16) | 可空；`ALIPAY` / `WECHAT` |
| `external_trade_no` | VARCHAR(128) | 第三方支付单号（冗余便于对账） |
| `remark` | VARCHAR(500) | 管理员备注等 |
| `create_time` | TIMESTAMP | |

索引：`user_id, create_time DESC`；`payment_order_id`。

### 5.4 统一支付订单表（演进自 `recharge_order`）

**物理表名保持 `recharge_order`**（与代码中 `RechargeOrder` 一致），不强制改名为 `payment_order`，避免大范围重命名。

已在库中增加字段（迁移脚本：`src/main/resources/sql/migration_recharge_order_unified_payment.sql`；全量建库见 `init.sql`）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `biz_type` | VARCHAR(32) | `RECHARGE` / `MEMBERSHIP`（存量默认 `RECHARGE`） |
| `membership_pricing_config_id` | BIGINT | 可空；`MEMBERSHIP` 时必填 |
| `membership_duration_months` | SMALLINT | 可空；冗余自定价行 |

常量类：`com.aicreation.constant.PaymentOrderBizType`。

充值订单：`biz_type = RECHARGE`，会员字段为空。  
会员订单：`biz_type = MEMBERSHIP`，`amount_cent` 与定价配置计算结果一致。

**订单号**：保持全局唯一；回调根据 `out_trade_no` 查单后分支。

---

## 6. 接口与模块划分（建议）

### 6.1 用户侧

| 能力 | 说明 |
|------|------|
| 定价列表 | GET 可售档位（含算好的 `price_cent`） |
| 创建会员订单 | 选中 `pricing_config_id`，创建 `MEMBERSHIP` 订单并返回支付内容（同充值） |
| 查询订单状态 | 复用现有订单查询（需返回 `biz_type` 或独立接口） |

### 6.2 支付回调

- 验签 → 查单 → `switch(biz_type)` → 充值入账 **或** 会员顺延 + 写 `membership_subscription`。
- 失败返回与现网一致（支付宝 `failure`、微信 FAIL XML），成功幂等。

### 6.3 管理员

| 能力 | 说明 |
|------|------|
| 定价 CRUD | `membership_pricing_config` |
| 赠送 / 延长 | 更新用户 `membership_end_at` + 写开通记录；需权限与审计日志 |

### 6.4 门禁封装

- `assertActiveMembership(userId)`：无效则抛 `MEMBERSHIP_REQUIRED` / `MEMBERSHIP_EXPIRED`（错误码在 `ErrorCodeEnum` 中新增）。
- 在 **AiBillingService 最前** 或 **各 AI 入口统一过滤器** 调用一次（避免遗漏定时任务线程：定时任务若以「系统身份」调用需单独策略，如仅允许配置创建人扣费且仍验该用户会员）。

---

## 7. 错误码（建议）

| 代码 | 场景 |
|------|------|
| `MEMBERSHIP_REQUIRED` | 从未开通或 tier 为 NONE |
| `MEMBERSHIP_EXPIRED` | `now >= end_at` |

前端：会员类错误优先展示，其次再提示充值余额。

---

## 8. 通知（可选）

- 会员购买成功 / 即将到期（定时任务扫描）  
与现有 `user_notification` 类型扩展：`MEMBERSHIP_PURCHASED`、`MEMBERSHIP_EXPIRING` 等。

---

## 9. 实现阶段建议（与先前「联调放最后」一致）

1. **库表迁移**：用户字段、`membership_pricing_config`、`membership_subscription`、统一订单表字段 `biz_type` 等。  
2. **门禁服务**：`assertActiveMembership` + 接入 `AiBillingService` 或统一切面。  
3. **后台定价配置**：CRUD。  
4. **创建会员订单 + 回调分发**：顺延逻辑 + 开通记录；充值路径回归测试。  
5. **退款回滚**（依赖支付侧能力）：开通记录 + 一致性校验。  
6. **管理员赠送/延长** + **前端**：会员开通页、错误提示、与余额按钮并存。  
7. **联调**：支付宝/微信真实支付、回调、顺延、到期拦截、余额扣费。

---

## 10. 风险与待确认项

- **自然月 vs 固定天数**：顺延必须统一一种算法。  
- **退款**：是否一期就做完整自动化；若否，文档中可标注「仅记录 + 人工改表」。  
- **时区**：一律使用服务器时区或 UTC 存库 + 展示转换，与现网一致。  
- **定时任务**：标题批量生成等若使用配置 `create_user_id` 扣费，该用户也必须 **会员有效**，否则任务失败或跳过并打日志。

---

## 11. 修订记录

| 日期 | 版本 | 说明 |
|------|------|------|
| （填日期） | v0.1 | 初稿，与产品确认后可冻结 v1.0 |

---

## 12. 实现说明（代码落地）

- **迁移脚本**：`src/main/resources/sql/migration_membership_schema.sql`（在已有库执行；含默认定价种子）。全量建库见 `init.sql` 中 `user_info` 扩展与 `membership_pricing_config` / `membership_subscription`。
- **核心服务**：`MembershipService` / `MembershipServiceImpl`（顺延、`assertActiveMembershipForAi`、下单、管理员赠送/指定结束、退款回滚）。
- **AI 门禁**：`AiBillingServiceImpl.executeWithAiBilling` 开头调用会员校验；**被计费用户**为管理员时跳过会员校验（与 `User.isAdmin` 一致）。
- **支付**：`WalletServiceImpl` 在 `MEMBERSHIP` 订单支付成功/对账成功后调用 `applyPaymentSuccess`。
- **API**：`/membership/pricing`、`/membership/order/create`；管理员 `/admin/membership/*`。
- **前端**：`auth.membershipActive`（`/users/me`）；Layout 会员入口与开通弹窗；文章 AI 按钮需 **会员且余额**；管理员菜单「会员定价」。
