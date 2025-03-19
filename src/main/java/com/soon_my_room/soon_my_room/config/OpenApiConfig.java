package com.soon_my_room.soon_my_room.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
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

    // JWT 인증을 위한 보안 스키마 정의
    SecurityScheme securityScheme =
        new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .in(SecurityScheme.In.HEADER)
            .name("Authorization");

    SecurityRequirement securityRequirement = new SecurityRequirement().addList("bearerAuth");

    return new OpenAPI()
        .info(info)
        .components(new Components().addSecuritySchemes("bearerAuth", securityScheme))
        .addSecurityItem(securityRequirement);
  }
}
