-- 添加文章生成状态字段
-- 日期: 2024-01-01
-- 描述: 为article表添加generation_status字段，用于跟踪文章生成状态

ALTER TABLE article ADD COLUMN IF NOT EXISTS generation_status SMALLINT DEFAULT 0;

COMMENT ON COLUMN article.generation_status IS '生成状态（0-未开始，1-生成中，2-已完成，3-失败）';