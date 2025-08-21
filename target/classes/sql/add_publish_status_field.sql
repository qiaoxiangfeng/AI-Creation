-- 为文章表添加发布状态字段的迁移脚本
-- 执行时间：2025-08-20
-- 用途：为现有文章表添加发布状态字段
-- 注意：此脚本会修改现有表结构，请在生产环境执行前先备份数据

-- 连接到数据库
\c ai_creation;

-- 添加发布状态字段
ALTER TABLE article ADD COLUMN IF NOT EXISTS publish_status SMALLINT DEFAULT 1;

-- 添加字段注释
COMMENT ON COLUMN article.publish_status IS '发布状态（1-未发布，2-已发布）';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_article_publish_status ON article(publish_status);

-- 更新现有数据的发布状态（默认为未发布）
UPDATE article SET publish_status = 1 WHERE publish_status IS NULL;

-- 验证字段添加结果
SELECT column_name, data_type, is_nullable, column_default, character_maximum_length
FROM information_schema.columns 
WHERE table_name = 'article' AND column_name = 'publish_status';

-- 验证索引创建结果
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename = 'article' AND indexname = 'idx_article_publish_status';

-- 显示表结构
\d article;
