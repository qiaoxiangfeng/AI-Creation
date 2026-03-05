-- 初始化文章生成状态，将NULL值设置为默认值0（未开始）
-- 解决启动时generationStatus为null导致的NullPointerException

DO $$
DECLARE
    updated_count INTEGER := 0;
BEGIN
    -- 更新所有generation_status为NULL的记录
    UPDATE article
    SET generation_status = 0,
        update_time = CURRENT_TIMESTAMP
    WHERE generation_status IS NULL;

    GET DIAGNOSTICS updated_count = ROW_COUNT;

    RAISE NOTICE '已初始化 % 篇文章的生成状态为0（未开始）', updated_count;
END $$;