-- 移除文章内容生成状态字段
-- 日期: 2024-01-01
-- 描述: 删除article表的content_generated字段，使用generation_status字段替代

-- 注意：执行前请确保已运行数据迁移脚本 migrate_content_generated_to_generation_status.sql

ALTER TABLE article DROP COLUMN IF EXISTS content_generated;