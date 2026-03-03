-- 删除已存在的表
DROP TABLE IF EXISTS article;

-- 创建文章表
CREATE TABLE article (
    id BIGSERIAL PRIMARY KEY,
    article_name VARCHAR(255) NOT NULL,
    article_outline TEXT,
    story_background TEXT,
    image_desc TEXT,
    article_type VARCHAR(100),
    article_content TEXT,
    voice_tone VARCHAR(100),
    voice_link VARCHAR(500),
    voice_file_path VARCHAR(500),
    video_link VARCHAR(500),
    video_file_path VARCHAR(500),
    publish_status SMALLINT DEFAULT 1,
    content_generated SMALLINT DEFAULT 0,
    res_state SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
-- 索引命名规范：idx_表名_字段名
CREATE INDEX IF NOT EXISTS idx_article_name ON article(article_name);
CREATE INDEX IF NOT EXISTS idx_article_create_time ON article(create_time);
CREATE INDEX IF NOT EXISTS idx_article_publish_status ON article(publish_status);
CREATE INDEX IF NOT EXISTS idx_article_res_state ON article(res_state);

-- 添加表和字段注释
-- PostgreSQL使用COMMENT ON语句添加注释
COMMENT ON TABLE article IS '文章表';
COMMENT ON COLUMN article.id IS '主键ID';
COMMENT ON COLUMN article.article_name IS '文章名称';
COMMENT ON COLUMN article.article_outline IS '文章简介';
COMMENT ON COLUMN article.story_background IS '故事背景';
COMMENT ON COLUMN article.image_desc IS '形象描述';
COMMENT ON COLUMN article.article_type IS '文章类型';
COMMENT ON COLUMN article.article_content IS '文章内容';
COMMENT ON COLUMN article.voice_tone IS '音色';
COMMENT ON COLUMN article.voice_link IS '语音链接';
COMMENT ON COLUMN article.voice_file_path IS '语音文件地址';
COMMENT ON COLUMN article.video_link IS '视频链接';
COMMENT ON COLUMN article.video_file_path IS '视频文件地址';
COMMENT ON COLUMN article.publish_status IS '发布状态（1-未发布，2-已发布）';
COMMENT ON COLUMN article.content_generated IS '内容生成状态（0-未生成，1-已生成）';
COMMENT ON COLUMN article.res_state IS '删除标记（1-有效，0-无效）';
COMMENT ON COLUMN article.create_time IS '创建时间';
COMMENT ON COLUMN article.update_time IS '更新时间';
