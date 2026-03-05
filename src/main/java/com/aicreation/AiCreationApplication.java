package com.aicreation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.aicreation.external.config.VolcengineProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.beans.factory.annotation.Autowired;
import com.aicreation.service.TaskStatusCleanupService;

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
public class AiCreationApplication {

    @Autowired
    private TaskStatusCleanupService taskStatusCleanupService;

    public static void main(String[] args) {
        SpringApplication.run(AiCreationApplication.class, args);
    }

    /**
     * 应用启动时执行的任务状态清理
     */
    @org.springframework.context.annotation.Bean
    public org.springframework.boot.ApplicationRunner applicationRunner() {
        return args -> {
            try {
                System.out.println("🚀 应用启动中，正在清理卡住的任务状态...");
                taskStatusCleanupService.cleanupAllStuckStatuses();
                System.out.println("✅ 任务状态清理完成");
            } catch (Exception e) {
                System.err.println("❌ 任务状态清理失败: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }

    /**
     * 配置BCryptPasswordEncoder Bean
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


} 