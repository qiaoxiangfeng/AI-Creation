-- 添加文章生成配置字数预估字段
-- 日期: 2024-01-01
-- 描述: 为article_generation_config表添加总字数预估和每章节字数预估字段

ALTER TABLE article_generation_config ADD COLUMN IF NOT EXISTS total_word_count_estimate INTEGER DEFAULT 100000;
ALTER TABLE article_generation_config ADD COLUMN IF NOT EXISTS chapter_word_count_estimate INTEGER DEFAULT 5000;

COMMENT ON COLUMN article_generation_config.total_word_count_estimate IS '总字数预估（默认100000）';
COMMENT ON COLUMN article_generation_config.chapter_word_count_estimate IS '每章节字数预估（默认5000）';