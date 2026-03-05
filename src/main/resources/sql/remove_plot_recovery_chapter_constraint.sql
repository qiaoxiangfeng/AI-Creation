-- 移除plot表中recovery_chapter_id的外键约束
-- 因为AI可能生成指向不存在章节的ID，导致插入失败

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_plot_recovery_chapter_id'
          AND table_name = 'plot'
    ) THEN
        ALTER TABLE plot DROP CONSTRAINT fk_plot_recovery_chapter_id;
        RAISE NOTICE '已移除plot表的fk_plot_recovery_chapter_id外键约束';
    ELSE
        RAISE NOTICE 'fk_plot_recovery_chapter_id外键约束不存在，无需移除';
    END IF;
END $$;

-- 验证约束已移除
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_plot_recovery_chapter_id'
          AND table_name = 'plot'
    ) THEN
        RAISE NOTICE '验证成功：fk_plot_recovery_chapter_id外键约束已移除';
    ELSE
        RAISE EXCEPTION '验证失败：fk_plot_recovery_chapter_id外键约束仍存在';
    END IF;
END $$;