package com.aicreation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.aicreation.external.config.VolcengineProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * AI智造平台主启动类
 * 
 * @author AI-Creation Team
 * @since 1.0.0
 */
@SpringBootApplication
@EnableTransactionManagement
@EnableCaching
@EnableAsync
@EnableScheduling
@MapperScan("com.aicreation.mapper")
@EnableConfigurationProperties({VolcengineProperties.class})
public class AiCreationApplication implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public static void main(String[] args) {
        SpringApplication.run(AiCreationApplication.class, args);
    }

    /**
     * 配置BCryptPasswordEncoder Bean
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 检查并创建缺失的表
     */
    private void createTablesIfNotExist() throws Exception {
        // 检查并创建 article 表
        if (!tableExists("article")) {
            System.out.println("创建 article 表...");
            jdbcTemplate.execute("""
                CREATE TABLE article (
                    id BIGSERIAL PRIMARY KEY,
                    article_name VARCHAR(255) NOT NULL,
                    article_outline TEXT,
                    story_background TEXT,
                    image_desc TEXT,
                    article_type VARCHAR(100),
                    article_characteristics VARCHAR(500),
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
                """);
            createArticleIndexes();
            createArticleComments();
        }

        // 检查并创建 article_generation_config 表
        if (!tableExists("article_generation_config")) {
            System.out.println("创建 article_generation_config 表...");
            jdbcTemplate.execute("""
                CREATE TABLE article_generation_config (
                    id BIGSERIAL PRIMARY KEY,
                    theme VARCHAR(200),
                    gender VARCHAR(50),
                    genre VARCHAR(100),
                    plot VARCHAR(200),
                    character_type VARCHAR(100),
                    style VARCHAR(100),
                    additional_characteristics TEXT,
                    pending_count INTEGER DEFAULT 0,
                    res_state SMALLINT DEFAULT 1,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """);
            createArticleGenerationConfigComments();
        }

        // 检查并创建 article_chapter 表
        if (!tableExists("article_chapter")) {
            System.out.println("创建 article_chapter 表...");
            jdbcTemplate.execute("""
                CREATE TABLE article_chapter (
                    id BIGSERIAL PRIMARY KEY,
                    chapter_no INTEGER NOT NULL,
                    article_id BIGINT NOT NULL,
                    chapter_title VARCHAR(255),
                    chapter_content TEXT,
                    chapter_voice_link VARCHAR(500),
                    chapter_video_link VARCHAR(500),
                    res_state SMALLINT DEFAULT 1,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (article_id) REFERENCES article(id)
                );
                """);
            createArticleChapterComments();
        }

        // 检查并创建 dictionary 表
        if (!tableExists("dictionary")) {
            System.out.println("创建 dictionary 表...");
            jdbcTemplate.execute("""
                CREATE TABLE dictionary (
                    id BIGSERIAL PRIMARY KEY,
                    dict_key VARCHAR(100) NOT NULL,
                    dict_value VARCHAR(255) NOT NULL,
                    res_state SMALLINT DEFAULT 1,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """);
        }

        // 检查并创建 audio_file_record 表
        if (!tableExists("audio_file_record")) {
            System.out.println("创建 audio_file_record 表...");
            jdbcTemplate.execute("""
                CREATE TABLE audio_file_record (
                    id BIGSERIAL PRIMARY KEY,
                    file_name VARCHAR(255) NOT NULL,
                    file_path VARCHAR(500) NOT NULL,
                    file_size BIGINT,
                    duration INTEGER,
                    res_state SMALLINT DEFAULT 1,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """);
        }
    }

    /**
     * 检查表是否存在
     */
    private boolean tableExists(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
                Integer.class,
                tableName
            );
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 创建article表的索引
     */
    private void createArticleIndexes() {
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_article_name ON article(article_name);");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_article_create_time ON article(create_time);");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_article_publish_status ON article(publish_status);");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_article_res_state ON article(res_state);");
    }

    /**
     * 创建article表的注释
     */
    private void createArticleComments() {
        jdbcTemplate.execute("COMMENT ON TABLE article IS '文章表';");
        jdbcTemplate.execute("COMMENT ON COLUMN article.id IS '主键ID';");
        jdbcTemplate.execute("COMMENT ON COLUMN article.article_name IS '文章名称';");
        jdbcTemplate.execute("COMMENT ON COLUMN article.article_outline IS '文章简介';");
        jdbcTemplate.execute("COMMENT ON COLUMN article.story_background IS '故事背景';");
        jdbcTemplate.execute("COMMENT ON COLUMN article.image_desc IS '形象描述';");
        jdbcTemplate.execute("COMMENT ON COLUMN article.article_type IS '文章类型';");
        jdbcTemplate.execute("COMMENT ON COLUMN article.article_characteristics IS '文章特点';");
        jdbcTemplate.execute("COMMENT ON COLUMN article.article_content IS '文章内容';");
        jdbcTemplate.execute("COMMENT ON COLUMN article.voice_tone IS '音色';");
        jdbcTemplate.execute("COMMENT ON COLUMN article.voice_link IS '语音链接';");
        jdbcTemplate.execute("COMMENT ON COLUMN article.voice_file_path IS '语音文件地址';");
        jdbcTemplate.execute("COMMENT ON COLUMN article.video_link IS '视频链接';");
        jdbcTemplate.execute("COMMENT ON COLUMN article.video_file_path IS '视频文件地址';");
        jdbcTemplate.execute("COMMENT ON COLUMN article.publish_status IS '发布状态（1-未发布，2-已发布）';");
        jdbcTemplate.execute("COMMENT ON COLUMN article.content_generated IS '内容生成状态（0-未生成，1-已生成）';");
        jdbcTemplate.execute("COMMENT ON COLUMN article.res_state IS '删除标记（1-有效，0-无效）';");
        jdbcTemplate.execute("COMMENT ON COLUMN article.create_time IS '创建时间';");
        jdbcTemplate.execute("COMMENT ON COLUMN article.update_time IS '更新时间';");
    }

    /**
     * 创建article_generation_config表的注释
     */
    private void createArticleGenerationConfigComments() {
        jdbcTemplate.execute("COMMENT ON TABLE article_generation_config IS '文章生成配置表';");
        jdbcTemplate.execute("COMMENT ON COLUMN article_generation_config.id IS '主键ID';");
        jdbcTemplate.execute("COMMENT ON COLUMN article_generation_config.theme IS '文章主题（用户自定义输入）';");
        jdbcTemplate.execute("COMMENT ON COLUMN article_generation_config.gender IS '性别分类（男生小说、女生小说）';");
        jdbcTemplate.execute("COMMENT ON COLUMN article_generation_config.genre IS '题材分类（仙侠、玄幻、都市等）';");
        jdbcTemplate.execute("COMMENT ON COLUMN article_generation_config.plot IS '情节分类（升级、学院、人生赢家等）';");
        jdbcTemplate.execute("COMMENT ON COLUMN article_generation_config.character_type IS '角色分类';");
        jdbcTemplate.execute("COMMENT ON COLUMN article_generation_config.style IS '风格分类';");
        jdbcTemplate.execute("COMMENT ON COLUMN article_generation_config.additional_characteristics IS '附加特点';");
        jdbcTemplate.execute("COMMENT ON COLUMN article_generation_config.pending_count IS '待生成数量';");
        jdbcTemplate.execute("COMMENT ON COLUMN article_generation_config.res_state IS '删除标记（1-有效，0-无效）';");
        jdbcTemplate.execute("COMMENT ON COLUMN article_generation_config.create_time IS '创建时间';");
        jdbcTemplate.execute("COMMENT ON COLUMN article_generation_config.update_time IS '更新时间';");
    }

    /**
     * 创建article_chapter表的注释
     */
    private void createArticleChapterComments() {
        jdbcTemplate.execute("COMMENT ON TABLE article_chapter IS '文章章节表';");
        jdbcTemplate.execute("COMMENT ON COLUMN article_chapter.id IS '主键ID';");
        jdbcTemplate.execute("COMMENT ON COLUMN article_chapter.chapter_no IS '章节序号';");
        jdbcTemplate.execute("COMMENT ON COLUMN article_chapter.article_id IS '文章ID';");
        jdbcTemplate.execute("COMMENT ON COLUMN article_chapter.chapter_title IS '章节标题';");
        jdbcTemplate.execute("COMMENT ON COLUMN article_chapter.chapter_content IS '章节内容';");
        jdbcTemplate.execute("COMMENT ON COLUMN article_chapter.chapter_voice_link IS '章节语音链接';");
        jdbcTemplate.execute("COMMENT ON COLUMN article_chapter.chapter_video_link IS '章节视频链接';");
        jdbcTemplate.execute("COMMENT ON COLUMN article_chapter.res_state IS '删除标记（1-有效，0-无效）';");
        jdbcTemplate.execute("COMMENT ON COLUMN article_chapter.create_time IS '创建时间';");
        jdbcTemplate.execute("COMMENT ON COLUMN article_chapter.update_time IS '更新时间';");
    }

    @Override
    public void run(String... args) throws Exception {
        // 执行数据库迁移
        try {
            System.out.println("开始执行数据库迁移...");

            // 1. 检查并创建缺失的表
            createTablesIfNotExist();

            // 2. 重命名字段（如果存在）
            jdbcTemplate.execute("DO $$\n" +
                    "BEGIN\n" +
                    "    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'article_generation_config' AND column_name = 'category_name') THEN\n" +
                    "        ALTER TABLE article_generation_config RENAME COLUMN category_name TO theme;\n" +
                    "    END IF;\n" +
                    "END $$;");

            // 2. 添加新字段
            jdbcTemplate.execute("ALTER TABLE article_generation_config ADD COLUMN IF NOT EXISTS gender VARCHAR(50);");
            jdbcTemplate.execute("ALTER TABLE article_generation_config ADD COLUMN IF NOT EXISTS genre VARCHAR(100);");
            jdbcTemplate.execute("ALTER TABLE article_generation_config ADD COLUMN IF NOT EXISTS plot VARCHAR(200);");
            jdbcTemplate.execute("ALTER TABLE article_generation_config ADD COLUMN IF NOT EXISTS character_type VARCHAR(100);");
            jdbcTemplate.execute("ALTER TABLE article_generation_config ADD COLUMN IF NOT EXISTS style VARCHAR(100);");

            // 3. 为article表添加字段
            jdbcTemplate.execute("ALTER TABLE article ADD COLUMN IF NOT EXISTS image_desc TEXT;");
            jdbcTemplate.execute("ALTER TABLE article ADD COLUMN IF NOT EXISTS content_generated SMALLINT DEFAULT 0;");

            // 3. 更新字段注释
            jdbcTemplate.execute("COMMENT ON COLUMN article_generation_config.theme IS '文章主题（用户自定义输入）';");
            jdbcTemplate.execute("COMMENT ON COLUMN article_generation_config.gender IS '性别分类（男生小说、女生小说）';");
            jdbcTemplate.execute("COMMENT ON COLUMN article_generation_config.genre IS '题材分类（仙侠、玄幻、都市等）';");
            jdbcTemplate.execute("COMMENT ON COLUMN article_generation_config.plot IS '情节分类（升级、学院、人生赢家等）';");
            jdbcTemplate.execute("COMMENT ON COLUMN article_generation_config.character_type IS '角色分类';");
            jdbcTemplate.execute("COMMENT ON COLUMN article_generation_config.style IS '风格分类';");
            jdbcTemplate.execute("COMMENT ON COLUMN article.image_desc IS '形象描述';");
            jdbcTemplate.execute("COMMENT ON COLUMN article.content_generated IS '内容生成状态（0-未生成，1-已生成）';");

            System.out.println("数据库迁移完成！");
        } catch (Exception e) {
            System.err.println("数据库迁移失败: " + e.getMessage());
        }
    }

} 