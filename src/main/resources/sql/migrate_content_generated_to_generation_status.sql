-- 迁移脚本：将content_generated字段的数据迁移到generation_status字段
-- 执行顺序：在删除content_generated字段之前执行

-- 首先，将content_generated = 1的记录设置为generation_status = 2（已完成）
UPDATE article
SET generation_status = 2
WHERE content_generated = 1
  AND (generation_status IS NULL OR generation_status = 0);

-- 将content_generated = 0的记录设置为generation_status = 0（未开始）
-- 注意：这里不覆盖已经设置的generation_status值
UPDATE article
SET generation_status = 0
WHERE content_generated = 0
  AND generation_status IS NULL;

-- 设置默认值：如果generation_status仍为NULL，则设为0
UPDATE article
SET generation_status = 0
WHERE generation_status IS NULL;

-- 验证迁移结果
SELECT
    COUNT(*) as total_articles,
    SUM(CASE WHEN generation_status = 0 THEN 1 ELSE 0 END) as not_started,
    SUM(CASE WHEN generation_status = 1 THEN 1 ELSE 0 END) as in_progress,
    SUM(CASE WHEN generation_status = 2 THEN 1 ELSE 0 END) as completed,
    SUM(CASE WHEN generation_status = 3 THEN 1 ELSE 0 END) as failed
FROM article;

-- 最后可以删除content_generated字段（在确认迁移成功后）
-- ALTER TABLE article DROP COLUMN IF EXISTS content_generated;