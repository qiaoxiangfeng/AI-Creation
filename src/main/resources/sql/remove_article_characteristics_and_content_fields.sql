-- 删除文章表的article_characteristics和article_content字段
-- 执行时间：2026-03-05

-- 删除字段（如果存在）
DO $$
BEGIN
    -- 删除article_characteristics字段
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'article' AND column_name = 'article_characteristics'
    ) THEN
        ALTER TABLE article DROP COLUMN article_characteristics;
    END IF;

    -- 删除article_content字段
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'article' AND column_name = 'article_content'
    ) THEN
        ALTER TABLE article DROP COLUMN article_content;
    END IF;
END $$;