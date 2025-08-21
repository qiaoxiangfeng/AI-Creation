package com.aicreation.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 配置类
 * 
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI aiCreationOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("AI-Creation API")
                .description("AI-Creation 后端接口文档")
                .version("v1.0.0")
                .contact(new Contact().name("AI-Creation Team").email("support@aicreation.com"))
                .license(new License().name("Apache 2.0")))
            .externalDocs(new ExternalDocumentation()
                .description("Project Home")
                .url("https://example.com"));
    }
}
