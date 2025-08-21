package com.aicreation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.aicreation.external.config.VolcengineProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
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
@MapperScan("com.aicreation.mapper")
@EnableConfigurationProperties({VolcengineProperties.class})
public class AiCreationApplication {

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

} 