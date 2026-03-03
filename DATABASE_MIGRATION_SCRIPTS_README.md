# 数据库迁移脚本说明

## 📋 概述

本项目采用独立的SQL脚本来管理数据库结构变更，每个数据库变更都有对应的迁移脚本。这样可以：

- 更好的版本控制和变更追踪
- 更安全的数据迁移过程
- 更清晰的变更历史
- 更容易的回滚操作

## 📁 脚本分类

### 🔄 迁移脚本 (migrate_*.sql)
用于数据迁移，不涉及表结构变更

| 脚本名称 | 描述 | 执行顺序 |
|---------|------|---------|
| `migrate_content_generated_to_generation_status.sql` | 将content_generated字段数据迁移到generation_status字段 | 1 |

### ➕ 添加字段脚本 (add_*_fields.sql)
用于添加新字段到现有表

| 脚本名称 | 描述 | 执行顺序 | 依赖 |
|---------|------|---------|------|
| `add_article_generation_status.sql` | 为article表添加generation_status字段 | 2 | 无 |
| `add_article_word_estimate_fields.sql` | 为article表添加字数预估字段 | 3 | 无 |
| `add_article_chapter_fields.sql` | 为article_chapter表添加新字段 | 4 | 无 |
| `add_article_generation_config_word_estimate_fields.sql` | 为article_generation_config表添加字数预估字段 | 5 | 无 |

### ➖ 删除字段脚本 (remove_*_fields.sql)
用于删除不再需要的字段

| 脚本名称 | 描述 | 执行顺序 | 前置条件 |
|---------|------|---------|----------|
| `remove_article_content_generated.sql` | 删除article表的content_generated字段 | 最后 | 已执行数据迁移脚本 |

## 🚀 执行指南

### 推荐执行顺序

1. **数据迁移**：`migrate_content_generated_to_generation_status.sql`
2. **添加字段**：
   - `add_article_generation_status.sql`
   - `add_article_word_estimate_fields.sql`
   - `add_article_chapter_fields.sql`
   - `add_article_generation_config_word_estimate_fields.sql`
3. **清理字段**：`remove_article_content_generated.sql`

### 执行方法

#### 方法1：通过数据库客户端执行
```sql
-- 连接到数据库后执行
\i src/main/resources/sql/migrate_content_generated_to_generation_status.sql
\i src/main/resources/sql/add_article_generation_status.sql
-- 依次执行其他脚本...
```

#### 方法2：通过命令行执行
```bash
# PostgreSQL
psql -h localhost -U postgres -d ai_creation -f src/main/resources/sql/migrate_content_generated_to_generation_status.sql

# MySQL (如果使用MySQL)
mysql -u username -p database_name < src/main/resources/sql/migrate_content_generated_to_generation_status.sql
```

#### 方法3：通过应用自动执行
应用启动时会自动检测并添加缺失的字段（通过`AiCreationApplication.java`），但建议手动执行脚本以确保版本控制。

## 🔍 脚本内容说明

### migrate_content_generated_to_generation_status.sql
```sql
-- 数据迁移脚本示例
UPDATE article SET generation_status = 2 WHERE content_generated = 1;
UPDATE article SET generation_status = 0 WHERE content_generated = 0 AND generation_status IS NULL;
```

### add_article_generation_status.sql
```sql
-- 添加字段脚本示例
ALTER TABLE article ADD COLUMN IF NOT EXISTS generation_status SMALLINT DEFAULT 0;
COMMENT ON COLUMN article.generation_status IS '生成状态（0-未开始，1-生成中，2-已完成，3-失败）';
```

### remove_article_content_generated.sql
```sql
-- 删除字段脚本示例
ALTER TABLE article DROP COLUMN IF EXISTS content_generated;
```

## ⚠️ 注意事项

### 执行前检查
- [ ] 备份数据库
- [ ] 在测试环境验证脚本
- [ ] 确认脚本执行顺序
- [ ] 检查依赖关系

### 执行中注意
- [ ] 监控数据库性能
- [ ] 大表操作建议在低峰期执行
- [ ] 保留执行日志

### 执行后验证
- [ ] 验证数据完整性
- [ ] 确认应用功能正常
- [ ] 检查索引和约束

## 🔄 回滚策略

### 如果需要回滚某个变更

1. **添加字段的回滚**：
   ```sql
   ALTER TABLE table_name DROP COLUMN IF EXISTS column_name;
   ```

2. **删除字段的回滚**：
   ```sql
   ALTER TABLE table_name ADD COLUMN column_name data_type;
   -- 然后恢复数据
   ```

3. **数据迁移的回滚**：
   - 从备份恢复
   - 或编写逆向迁移脚本

## 📝 脚本命名规范

- `migrate_*.sql` - 数据迁移脚本
- `add_*_fields.sql` - 添加字段脚本
- `remove_*_fields.sql` - 删除字段脚本
- `create_*.sql` - 创建新表脚本
- `drop_*.sql` - 删除表脚本

## 🎯 最佳实践

1. **版本控制**：所有脚本纳入Git版本控制
2. **测试先行**：在测试环境完整测试后再在生产环境执行
3. **备份策略**：执行前务必备份数据库
4. **文档更新**：执行后更新相关文档
5. **审计日志**：保留执行记录和结果

## 📞 联系支持

如果在执行脚本过程中遇到问题，请：

1. 检查错误日志
2. 确认数据库权限
3. 联系开发团队
4. 提供详细的错误信息