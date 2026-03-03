-- 删除已存在的表
DROP TABLE IF EXISTS article_generation_config;

-- 创建文章生成配置表
CREATE TABLE article_generation_config (
    id BIGSERIAL PRIMARY KEY,
    theme VARCHAR(100) NOT NULL,
    gender VARCHAR(50),
    genre VARCHAR(100),
    plot VARCHAR(200),
    character_type VARCHAR(100),
    style VARCHAR(100),
    additional_characteristics TEXT,
    total_word_count_estimate INTEGER DEFAULT 100000,
    chapter_word_count_estimate INTEGER DEFAULT 5000,
    pending_count INTEGER DEFAULT 0,
    res_state SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
-- 索引命名规范：idx_表名_字段名
CREATE INDEX IF NOT EXISTS idx_article_generation_config_theme ON article_generation_config(theme);
CREATE INDEX IF NOT EXISTS idx_article_generation_config_create_time ON article_generation_config(create_time);
CREATE INDEX IF NOT EXISTS idx_article_generation_config_res_state ON article_generation_config(res_state);

-- 添加表和字段注释
-- PostgreSQL使用COMMENT ON语句添加注释
COMMENT ON TABLE article_generation_config IS '文章生成配置表';
COMMENT ON COLUMN article_generation_config.id IS '主键ID';
COMMENT ON COLUMN article_generation_config.theme IS '文章主题（用户自定义输入）';
COMMENT ON COLUMN article_generation_config.gender IS '性别分类（男生小说、女生小说）';
COMMENT ON COLUMN article_generation_config.genre IS '题材分类（仙侠、玄幻、都市等）';
COMMENT ON COLUMN article_generation_config.plot IS '情节分类（升级、学院、人生赢家等）';
COMMENT ON COLUMN article_generation_config.character_type IS '角色分类';
COMMENT ON COLUMN article_generation_config.style IS '风格分类';
COMMENT ON COLUMN article_generation_config.additional_characteristics IS '附加特点（逗号分隔）';
COMMENT ON COLUMN article_generation_config.total_word_count_estimate IS '总字数预估（默认100000）';
COMMENT ON COLUMN article_generation_config.chapter_word_count_estimate IS '每章节字数预估（默认5000）';
COMMENT ON COLUMN article_generation_config.pending_count IS '待生成数量';
COMMENT ON COLUMN article_generation_config.res_state IS '删除标记（1-有效，0-无效）';
COMMENT ON COLUMN article_generation_config.create_time IS '创建时间';
COMMENT ON COLUMN article_generation_config.update_time IS '更新时间';