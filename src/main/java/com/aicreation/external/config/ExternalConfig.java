package com.aicreation.external.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 外部服务配置类
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
@Configuration
@EnableConfigurationProperties({
    VolcengineProperties.class,
    DouBaoTtsProperties.class
})
public class ExternalConfig {
}
