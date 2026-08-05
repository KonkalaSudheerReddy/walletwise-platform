package com.walletwise.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {
  @Bean
  OpenAPI walletWiseOpenApi(AppProperties properties) {
    return new OpenAPI()
        .info(
            new Info()
                .title("WalletWise API")
                .version("v1")
                .description(
                    "Portfolio and educational virtual-wallet API; it does not process real money."))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .servers(
            List.of(
                new Server()
                    .url(properties.publicUrl())
                    .description("Configured application URL")));
  }
}
