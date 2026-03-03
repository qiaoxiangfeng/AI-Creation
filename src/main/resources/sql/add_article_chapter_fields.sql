-- 添加文章章节新字段
-- 日期: 2024-01-01
-- 描述: 为article_chapter表添加核心剧情和字数预估字段

ALTER TABLE article_chapter ADD COLUMN IF NOT EXISTS core_plot TEXT;
ALTER TABLE article_chapter ADD COLUMN IF NOT EXISTS word_count_estimate INTEGER;

COMMENT ON COLUMN article_chapter.core_plot IS '核心剧情';
COMMENT ON COLUMN article_chapter.word_count_estimate IS '字数预估';