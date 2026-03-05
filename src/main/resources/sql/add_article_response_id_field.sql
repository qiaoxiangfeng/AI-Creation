-- 添加response_id字段到article表，用于Responses API上下文管理
-- 执行时间：2026-03-05

-- 添加response_id字段
ALTER TABLE article ADD COLUMN response_id VARCHAR(255);

-- 添加注释
COMMENT ON COLUMN article.response_id IS 'Responses API的response_id，用于小说上下文管理和多任务隔离';

-- 创建索引（可选，用于查询优化）
CREATE INDEX IF NOT EXISTS idx_article_response_id ON article(response_id);