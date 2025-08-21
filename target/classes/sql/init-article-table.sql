-- =============================================
-- 文章表完整初始化脚本
-- 执行时间：2025-08-20
-- 用途：重新创建文章表结构，包含文章内容字段
-- =============================================
--
-- 表结构说明：
-- 1. 主键：id (BIGSERIAL)
-- 2. 文章名称：article_name (VARCHAR(255), NOT NULL)
-- 3. 文章简介：article_outline (TEXT)
-- 4. 文章内容：article_content (TEXT) - 新增字段，支持长文本
-- 5. 音色：voice_tone (VARCHAR(100))
-- 6. 语音相关：voice_link, voice_file_path (VARCHAR(500))
-- 7. 视频相关：video_link, video_file_path (VARCHAR(500))
-- 8. 发布状态：publish_status (SMALLINT, DEFAULT 1) - 1未发布，2已发布
-- 9. 删除标记：res_state (SMALLINT, DEFAULT 1) - 1有效，0无效
-- 10. 时间：create_time, update_time (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)
--
-- PostgreSQL规范：
-- 1. 使用BIGSERIAL作为自增主键类型
-- 2. 发布状态字段publish_status使用SMALLINT类型，1-未发布，2-已发布
-- 3. 删除标记字段res_state统一使用SMALLINT类型
-- 4. 使用COMMENT ON语句添加表和字段注释
-- 5. 遵循PostgreSQL最佳实践
--
-- 注意事项：
-- 1. 此脚本会删除现有表和数据，请谨慎使用
-- 2. 建议在生产环境使用前先备份数据
-- 3. 执行后需要重启应用
-- =============================================

-- 如果表已存在，先删除
DROP TABLE IF EXISTS article CASCADE;

-- 创建文章表
CREATE TABLE article (
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
CREATE INDEX IF NOT EXISTS idx_article_name ON article(article_name);
CREATE INDEX IF NOT EXISTS idx_article_create_time ON article(create_time);
CREATE INDEX IF NOT EXISTS idx_article_publish_status ON article(publish_status);
CREATE INDEX IF NOT EXISTS idx_article_res_state ON article(res_state);

-- 添加注释
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

-- 插入示例数据
INSERT INTO article (article_name, article_outline, article_content, voice_tone, res_state) VALUES
('AI技术发展趋势', '本文主要介绍AI技术的发展趋势和未来展望', '人工智能（AI）技术正在快速发展，从机器学习到深度学习，从自然语言处理到计算机视觉，AI已经在各个领域展现出巨大的潜力。随着算力的提升和算法的优化，AI技术将继续推动社会进步和产业升级。', 'alex', 1),
('区块链技术应用', '探讨区块链技术在各个行业的应用场景', '区块链作为一种分布式账本技术，具有去中心化、不可篡改、透明可追溯等特性。在金融、供应链、医疗、教育等领域都有广泛的应用前景。通过智能合约，可以实现自动化的业务流程，提高效率降低成本。', 'anna', 1),
('云计算架构设计', '介绍现代云计算架构的设计原则和最佳实践', '云计算架构设计需要考虑可扩展性、高可用性、安全性等多个方面。通过微服务架构、容器化部署、自动化运维等手段，可以构建稳定可靠的云平台。同时要关注成本优化和性能调优，确保系统的高效运行。', 'alex', 1);

-- 验证表结构
SELECT column_name, data_type, is_nullable, column_default, character_maximum_length
FROM information_schema.columns 
WHERE table_name = 'article' 
ORDER BY ordinal_position;

-- 验证数据
SELECT id, article_name, LEFT(article_content, 50) as content_preview, voice_tone, res_state
FROM article;
