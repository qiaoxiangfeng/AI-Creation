package com.aicreation.config;

import com.aicreation.interceptor.AuthContextInterceptor;
import com.aicreation.interceptor.TraceIdInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 *
 * @author AI-Creation Team
 * @since 1.0.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private TraceIdInterceptor traceIdInterceptor;

    @Autowired
    private AuthContextInterceptor authContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加链路ID拦截器，拦截所有请求
        registry.addInterceptor(traceIdInterceptor)
                .addPathPatterns("/**")  // 拦截所有路径
                .excludePathPatterns(
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/favicon.ico",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                ); // 排除静态资源

        // 解析 X-User-Id，写入当前用户（登录接口除外）
        registry.addInterceptor(authContextInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/users/login",
                        "/users/logout",
                        "/wallet/recharge/callback/alipay",
                        "/wallet/recharge/callback/wechat",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/favicon.ico",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );
    }
}