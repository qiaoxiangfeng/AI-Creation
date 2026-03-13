# 数据库初始化脚本说明

本文档说明当前项目的数据库初始化方式。

## 📋 初始化脚本

- **主脚本**: `init.sql`
- **用途**: 在全新数据库环境中一次性创建/重建所有业务相关表结构
- **包含内容**:
  - `article` 文章表（包含 `response_id`、`story_complete`、字数预估等字段）
  - `article_chapter` 文章章节表（包含核心剧情、字数预估、生成状态等字段）
  - `article_generation_config` 文章生成配置表
  - `dictionary` 字典表
  - `plot` 伏笔表
  - `audio_file_record` 音频文件记录表

## 🔄 执行方式

在目标数据库中执行以下命令即可完成初始化（以 `psql` 为例）：

```bash
psql -d your_database -f init.sql
```

> **说明**: `init.sql` 中包含 `DROP TABLE IF EXISTS ...` 语句，多次执行是幂等的（会重建相关表结构）。

## ⚠️ 注意事项

1. **备份数据**: 在生产环境执行前，请务必备份数据库
2. **测试环境**: 建议先在测试环境执行并验证
3. **回滚计划**: 准备好数据回滚方案，以防初始化或重建失败
4. **应用重启**: 初始化或结构变更完成后建议重启应用以确保新的表结构生效

## 🔍 验证方法

执行完初始化脚本后，可以通过以下SQL验证表结构是否正确：

```sql
-- 检查article表结构
\d article

-- 验证关键字段存在
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'article'
ORDER BY ordinal_position;
```
