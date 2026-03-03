-- 数据库升级脚本：从 v1.0 升级到 v2.0
-- 执行时间: 2024-01-01
-- 描述: 统一处理所有数据库结构变更，包括字段添加、删除和数据迁移

-- ===========================================
-- 1. 数据迁移：content_generated -> generation_status
-- ===========================================

DO $$
BEGIN
    RAISE NOTICE '开始数据迁移：content_generated -> generation_status';

    -- 检查content_generated字段是否存在
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'article' AND column_name = 'content_generated') THEN

        -- 迁移数据：content_generated = 1 -> generation_status = 2（已完成）
        UPDATE article
        SET generation_status = 2
        WHERE content_generated = 1
          AND (generation_status IS NULL OR generation_status = 0);

        -- 迁移数据：content_generated = 0 -> generation_status = 0（未开始）
        UPDATE article
        SET generation_status = 0
        WHERE content_generated = 0
          AND generation_status IS NULL;

        -- 设置默认值
        UPDATE article
        SET generation_status = 0
        WHERE generation_status IS NULL;

        RAISE NOTICE '数据迁移完成';
    ELSE
        RAISE NOTICE 'content_generated字段不存在，跳过数据迁移';
    END IF;
END $$;

-- ===========================================
-- 2. Article表结构变更
-- ===========================================

-- 添加generation_status字段
ALTER TABLE article ADD COLUMN IF NOT EXISTS generation_status SMALLINT DEFAULT 0;
COMMENT ON COLUMN article.generation_status IS '生成状态（0-未开始，1-生成中，2-已完成，3-失败）';

-- 添加字数预估字段
ALTER TABLE article ADD COLUMN IF NOT EXISTS total_word_count_estimate INTEGER;
ALTER TABLE article ADD COLUMN IF NOT EXISTS chapter_word_count_estimate INTEGER;
COMMENT ON COLUMN article.total_word_count_estimate IS '总字数预估';
COMMENT ON COLUMN article.chapter_word_count_estimate IS '每章节字数预估';

-- ===========================================
-- 3. Article_Chapter表结构变更
-- ===========================================

-- 添加核心剧情和字数预估字段
ALTER TABLE article_chapter ADD COLUMN IF NOT EXISTS core_plot TEXT;
ALTER TABLE article_chapter ADD COLUMN IF NOT EXISTS word_count_estimate INTEGER;
COMMENT ON COLUMN article_chapter.core_plot IS '核心剧情';
COMMENT ON COLUMN article_chapter.word_count_estimate IS '字数预估';

-- ===========================================
-- 4. Article_Generation_Config表结构变更
-- ===========================================

-- 添加字数预估字段（带默认值）
ALTER TABLE article_generation_config ADD COLUMN IF NOT EXISTS total_word_count_estimate INTEGER DEFAULT 100000;
ALTER TABLE article_generation_config ADD COLUMN IF NOT EXISTS chapter_word_count_estimate INTEGER DEFAULT 5000;
COMMENT ON COLUMN article_generation_config.total_word_count_estimate IS '总字数预估（默认100000）';
COMMENT ON COLUMN article_generation_config.chapter_word_count_estimate IS '每章节字数预估（默认5000）';

-- ===========================================
-- 5. 清理：删除废弃字段
-- ===========================================

-- 删除content_generated字段（如果存在）
ALTER TABLE article DROP COLUMN IF EXISTS content_generated;

-- ===========================================
-- 6. 验证升级结果
-- ===========================================

DO $$
DECLARE
    article_count INTEGER;
    article_chapter_count INTEGER;
    config_count INTEGER;
BEGIN
    RAISE NOTICE '数据库升级验证开始...';

    -- 检查Article表
    SELECT COUNT(*) INTO article_count FROM article;
    RAISE NOTICE 'Article表总记录数: %', article_count;

    -- 检查Article_Chapter表
    SELECT COUNT(*) INTO article_chapter_count FROM article_chapter;
    RAISE NOTICE 'Article_Chapter表总记录数: %', article_chapter_count;

    -- 检查Article_Generation_Config表
    SELECT COUNT(*) INTO config_count FROM article_generation_config;
    RAISE NOTICE 'Article_Generation_Config表总记录数: %', config_count;

    -- 检查字段完整性
    RAISE NOTICE '字段完整性检查:';
    RAISE NOTICE '- Article表 generation_status字段: %',
        CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns
                         WHERE table_name = 'article' AND column_name = 'generation_status')
             THEN '✓ 存在' ELSE '✗ 不存在' END;

    RAISE NOTICE '- Article表字数预估字段: %',
        CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns
                         WHERE table_name = 'article' AND column_name = 'total_word_count_estimate')
             AND EXISTS (SELECT 1 FROM information_schema.columns
                        WHERE table_name = 'article' AND column_name = 'chapter_word_count_estimate')
             THEN '✓ 存在' ELSE '✗ 不完整' END;

    RAISE NOTICE '- Article_Chapter表新字段: %',
        CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns
                         WHERE table_name = 'article_chapter' AND column_name = 'core_plot')
             AND EXISTS (SELECT 1 FROM information_schema.columns
                        WHERE table_name = 'article_chapter' AND column_name = 'word_count_estimate')
             THEN '✓ 存在' ELSE '✗ 不完整' END;

    RAISE NOTICE '- Article_Generation_Config表字数字段: %',
        CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns
                         WHERE table_name = 'article_generation_config' AND column_name = 'total_word_count_estimate')
             AND EXISTS (SELECT 1 FROM information_schema.columns
                        WHERE table_name = 'article_generation_config' AND column_name = 'chapter_word_count_estimate')
             THEN '✓ 存在' ELSE '✗ 不完整' END;

    RAISE NOTICE '数据库升级验证完成！';
END $$;