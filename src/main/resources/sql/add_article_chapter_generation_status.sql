-- 添加文章章节生成状态字段
-- 日期: 2026-02-28
-- 描述: 为article_chapter表添加generation_status字段，用于标识章节内容生成状态

ALTER TABLE article_chapter ADD COLUMN IF NOT EXISTS generation_status SMALLINT DEFAULT 0;

COMMENT ON COLUMN article_chapter.generation_status IS '生成状态（0-未开始，1-生成中，2-已完成，3-失败）';

-- 创建索引（可选）
CREATE INDEX IF NOT EXISTS idx_article_chapter_generation_status ON article_chapter(generation_status);