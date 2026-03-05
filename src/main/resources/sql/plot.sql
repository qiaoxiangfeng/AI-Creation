-- 删除已存在的表
DROP TABLE IF EXISTS plot;

-- 创建伏笔表
CREATE TABLE plot (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL,
    chapter_id BIGINT NOT NULL,
    plot_name VARCHAR(255) NOT NULL,
    plot_content TEXT,
    recovery_chapter_id BIGINT,
    res_state SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 外键约束
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_plot_article_id'
          AND table_name = 'plot'
    ) THEN
        ALTER TABLE plot
            ADD CONSTRAINT fk_plot_article_id
            FOREIGN KEY (article_id) REFERENCES article(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_plot_chapter_id'
          AND table_name = 'plot'
    ) THEN
        ALTER TABLE plot
            ADD CONSTRAINT fk_plot_chapter_id
            FOREIGN KEY (chapter_id) REFERENCES article_chapter(id);
    END IF;
END $$;

-- 移除外键约束（如果存在）
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_plot_recovery_chapter_id'
          AND table_name = 'plot'
    ) THEN
        ALTER TABLE plot DROP CONSTRAINT fk_plot_recovery_chapter_id;
    END IF;
END $$;

-- 索引
CREATE INDEX IF NOT EXISTS idx_plot_article_id ON plot(article_id);
CREATE INDEX IF NOT EXISTS idx_plot_chapter_id ON plot(chapter_id);
CREATE INDEX IF NOT EXISTS idx_plot_recovery_chapter_id ON plot(recovery_chapter_id);
CREATE INDEX IF NOT EXISTS idx_plot_res_state ON plot(res_state);

-- 注释
COMMENT ON TABLE plot IS '伏笔表';
COMMENT ON COLUMN plot.id IS '主键ID';
COMMENT ON COLUMN plot.article_id IS '文章ID';
COMMENT ON COLUMN plot.chapter_id IS '埋设伏笔的章节ID';
COMMENT ON COLUMN plot.plot_name IS '伏笔名称';
COMMENT ON COLUMN plot.plot_content IS '伏笔内容';
COMMENT ON COLUMN plot.recovery_chapter_id IS '回收伏笔的章节ID';
COMMENT ON COLUMN plot.res_state IS '删除标记（1-有效，0-无效）';
COMMENT ON COLUMN plot.create_time IS '创建时间';
COMMENT ON COLUMN plot.update_time IS '更新时间';