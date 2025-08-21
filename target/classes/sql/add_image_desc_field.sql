-- 在文章表中添加image_desc字段（形象描述）
-- 在article_outline字段后添加

ALTER TABLE article ADD COLUMN image_desc TEXT;

-- 添加字段注释
COMMENT ON COLUMN article.image_desc IS '形象描述';

-- 为image_desc字段创建索引（可选，根据查询需求决定）
-- CREATE INDEX IF NOT EXISTS idx_article_image_desc ON article(image_desc);
