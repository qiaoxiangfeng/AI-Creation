-- =============================================
-- 数据库表结构更新脚本
-- 更新日期：2025-08-20
-- 更新内容：为文章表（article）新增"文章内容"字段
-- =============================================
--
-- 变更详情：
-- 1. 新增字段：article_content TEXT COMMENT '文章内容'
-- 2. 字段位置：在 article_outline（文章简介）字段之后，voice_tone（音色）字段之前
-- 3. 数据类型：TEXT，支持长文本存储，最大长度无限制
-- 4. 约束：可为空，无默认值，不影响现有数据
--
-- 执行前注意事项：
-- 1. 请先备份现有数据：pg_dump -h localhost -U postgres ai_creation > backup_$(date +%Y%m%d).sql
-- 2. 确保PostgreSQL服务正在运行
-- 3. 确保有数据库ALTER权限
--
-- 执行后验证：
-- 1. 检查表结构：SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'article' ORDER BY ordinal_position;
-- 2. 测试插入：INSERT INTO article (article_name, article_content, voice_tone) VALUES ('测试', '内容', 'alex');
-- 3. 重启应用：./stop.sh && ./start.sh
-- =============================================

-- 为现有article表添加文章内容字段
ALTER TABLE article ADD COLUMN IF NOT EXISTS article_content TEXT COMMENT '文章内容';

-- 为现有记录设置默认值（可选）
UPDATE article SET article_content = '' WHERE article_content IS NULL;

-- 验证字段是否添加成功
SELECT column_name, data_type, is_nullable, column_default, character_maximum_length
FROM information_schema.columns 
WHERE table_name = 'article' AND column_name = 'article_content';
