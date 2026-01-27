package swyp12.team9.server.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

        @Value("${swagger.server-url}")
        private String serverUrl;

        @Bean
        public OpenAPI openAPI() {
                String securitySchemeName = "AccessToken";

                return new OpenAPI()
                                .info(new Info()
                                                .title("Keepit API")
                                                .description("Keepit Backend Server API Documentation")
                                                .version("v1.0.0"))
                                .servers(List.of(
                                                new Server()
                                                                .url(serverUrl)
                                                                .description("Server")))
                                .components(new Components()
                                                .addSecuritySchemes(securitySchemeName,
                                                                new SecurityScheme()
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")
                                                                                .in(SecurityScheme.In.HEADER)
                                                                                .name("Authorization")))
                                .addSecurityItem(new SecurityRequirement()
                                                .addList(securitySchemeName));
        }
}
