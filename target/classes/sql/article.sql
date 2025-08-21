-- 文章表
-- PostgreSQL数据库规范：
-- 1. 使用BIGSERIAL作为自增主键
-- 2. 时间字段使用TIMESTAMP类型
-- 3. 使用COMMENT ON语句添加表和字段注释
-- 4. 索引命名规范：idx_表名_字段名
-- 5. 使用IF NOT EXISTS避免重复创建

CREATE TABLE IF NOT EXISTS article (
    id BIGSERIAL PRIMARY KEY,
    article_name VARCHAR(255) NOT NULL,
    article_outline TEXT,
    article_content TEXT,
    voice_tone VARCHAR(100),
    voice_link VARCHAR(500),
    voice_file_path VARCHAR(500),
    video_link VARCHAR(500),
    video_file_path VARCHAR(500),
    publish_status SMALLINT DEFAULT 1,
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
COMMENT ON COLUMN article.article_content IS '文章内容';
COMMENT ON COLUMN article.voice_tone IS '音色';
COMMENT ON COLUMN article.voice_link IS '语音链接';
COMMENT ON COLUMN article.voice_file_path IS '语音文件地址';
COMMENT ON COLUMN article.video_link IS '视频链接';
COMMENT ON COLUMN article.video_file_path IS '视频文件地址';
COMMENT ON COLUMN article.publish_status IS '发布状态（1-未发布，2-已发布）';
COMMENT ON COLUMN article.res_state IS '删除标记（1-有效，0-无效）';
COMMENT ON COLUMN article.create_time IS '创建时间';
COMMENT ON COLUMN article.update_time IS '更新时间';

-- 创建序列（可选，BIGSERIAL会自动创建）
-- CREATE SEQUENCE IF NOT EXISTS article_id_seq OWNED BY article.id;

-- 添加约束（可选）
-- ALTER TABLE article ADD CONSTRAINT chk_res_state CHECK (res_state IN (0, 1));
-- ALTER TABLE article ADD CONSTRAINT chk_article_name_length CHECK (LENGTH(article_name) >= 1);
