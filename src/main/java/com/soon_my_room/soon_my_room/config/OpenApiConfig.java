package com.soon_my_room.soon_my_room.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI openAPI() {
    Info info =
        new Info()
            .title("Soon My Room API")
            .version("v1.0.0")
            .description("Soon My Room 서비스의 API 명세서입니다.")
            .contact(new Contact().name("Soon My Room Team").email("contact@soon-my-room.com"));

    // Access Token 용 보안 스키마
    SecurityScheme accessTokenScheme =
        new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .in(SecurityScheme.In.HEADER)
            .name("Authorization");

    // Refresh Token 용 보안 스키마 (쿠키)
    SecurityScheme refreshTokenScheme =
        new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .in(SecurityScheme.In.COOKIE)
            .name("refresh_token");

    // 두 가지 스키마를 Components에 추가
    Components components =
        new Components()
            .addSecuritySchemes("bearerAuth", accessTokenScheme)
            .addSecuritySchemes("refreshTokenCookie", refreshTokenScheme);

    // API 전체에 대한 기본 보안 요구 사항은 Access Token만 설정
    SecurityRequirement securityRequirement = new SecurityRequirement().addList("bearerAuth");

    return new OpenAPI()
        .info(info)
        .components(components)
        .addSecurityItem(securityRequirement)
        .addServersItem(new Server().url("/"));
  }
}
