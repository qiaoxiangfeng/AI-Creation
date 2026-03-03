-- 删除已存在的表
DROP TABLE IF EXISTS article_chapter;

-- 创建文章章节表
CREATE TABLE article_chapter (
    id BIGSERIAL PRIMARY KEY,
    chapter_no INTEGER NOT NULL,
    article_id BIGINT NOT NULL,
    chapter_title VARCHAR(255),
    chapter_content TEXT,
    core_plot TEXT,
    word_count_estimate INTEGER,
    chapter_voice_link VARCHAR(500),
    chapter_video_link VARCHAR(500),
    res_state SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 外键（若不存在则添加）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_article_chapter_article_id'
          AND table_name = 'article_chapter'
    ) THEN
        ALTER TABLE article_chapter
            ADD CONSTRAINT fk_article_chapter_article_id
            FOREIGN KEY (article_id) REFERENCES article(id);
    END IF;
END $$;

-- 索引
CREATE INDEX IF NOT EXISTS idx_article_chapter_article_id ON article_chapter(article_id);
CREATE INDEX IF NOT EXISTS idx_article_chapter_chapter_no ON article_chapter(chapter_no);
CREATE INDEX IF NOT EXISTS idx_article_chapter_res_state ON article_chapter(res_state);

-- 注释
COMMENT ON TABLE article_chapter IS '文章章节表';
COMMENT ON COLUMN article_chapter.id IS '主键ID';
COMMENT ON COLUMN article_chapter.chapter_no IS '章节序号（第一章填1）';
COMMENT ON COLUMN article_chapter.article_id IS '文章ID，关联 article.id';
COMMENT ON COLUMN article_chapter.chapter_title IS '章节标题';
COMMENT ON COLUMN article_chapter.chapter_content IS '章节内容';
COMMENT ON COLUMN article_chapter.core_plot IS '核心剧情';
COMMENT ON COLUMN article_chapter.word_count_estimate IS '字数预估';
COMMENT ON COLUMN article_chapter.chapter_voice_link IS '章节语音链接地址';
COMMENT ON COLUMN article_chapter.chapter_video_link IS '章节视频链接地址';
COMMENT ON COLUMN article_chapter.res_state IS '删除标记（1-有效，0-无效）';
COMMENT ON COLUMN article_chapter.create_time IS '创建时间';
COMMENT ON COLUMN article_chapter.update_time IS '更新时间';


