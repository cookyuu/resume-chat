package com.cookyuu.resume_chat.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String jwtSchemeName = "BearerAuth";

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(jwtSchemeName);

        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT Access Token (Authorization 헤더에 'Bearer {token}' 형식으로 전송)"));

        return new OpenAPI()
                .info(new Info()
                        .title("Resume Chat API")
                        .description("이력서 기반 채팅 서비스 API 문서")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("API Support")
                                .email("support@resumechat.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:7777")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.resumechat.com")
                                .description("Production Server")))
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}
