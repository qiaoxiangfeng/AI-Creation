-- 为已有库增加创建人字段（PostgreSQL）
-- 在 article、article_generation_config 上执行一次即可

ALTER TABLE article ADD COLUMN IF NOT EXISTS create_user_id BIGINT;
COMMENT ON COLUMN article.create_user_id IS '创建人用户ID（关联 user_info.id；由配置任务生成的文章与配置的创建人一致）';
CREATE INDEX IF NOT EXISTS idx_article_create_user_id ON article(create_user_id);

ALTER TABLE article_generation_config ADD COLUMN IF NOT EXISTS create_user_id BIGINT;
COMMENT ON COLUMN article_generation_config.create_user_id IS '创建人用户ID（关联 user_info.id）';
CREATE INDEX IF NOT EXISTS idx_article_generation_config_create_user_id ON article_generation_config(create_user_id);
