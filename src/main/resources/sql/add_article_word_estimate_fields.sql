-- 添加文章字数预估字段
-- 日期: 2024-01-01
-- 描述: 为article表添加总字数预估和每章节字数预估字段

ALTER TABLE article ADD COLUMN IF NOT EXISTS total_word_count_estimate INTEGER;
ALTER TABLE article ADD COLUMN IF NOT EXISTS chapter_word_count_estimate INTEGER;

COMMENT ON COLUMN article.total_word_count_estimate IS '总字数预估';
COMMENT ON COLUMN article.chapter_word_count_estimate IS '每章节字数预估';