package com.walletwise.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import java.util.Map;
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
                        .bearerFormat("JWT"))
                .addSchemas(
                    "ProblemDetail",
                    new ObjectSchema()
                        .addProperty("type", new StringSchema().format("uri"))
                        .addProperty("title", new StringSchema())
                        .addProperty("status", new IntegerSchema())
                        .addProperty("detail", new StringSchema())
                        .addProperty("instance", new StringSchema().format("uri"))
                        .addProperty("code", new StringSchema())
                        .addProperty("timestamp", new StringSchema().format("date-time"))
                        .addProperty("correlationId", new StringSchema()))
                .addResponses(
                    "BadRequest",
                    problemResponse(
                        "Invalid request",
                        Map.of(
                            "type", "https://walletwise.app/problems/malformed_request",
                            "title", "Bad Request",
                            "status", 400,
                            "detail", "The request contains an invalid value",
                            "code", "malformed_request",
                            "correlationId", "demo-correlation-123")))
                .addResponses(
                    "Unauthorized",
                    problemResponse(
                        "Authentication required",
                        Map.of(
                            "type", "https://walletwise.app/problems/authentication_required",
                            "title", "Unauthorized",
                            "status", 401,
                            "detail", "Authentication is required or invalid",
                            "code", "authentication_required",
                            "correlationId", "demo-correlation-123")))
                .addResponses(
                    "Conflict",
                    problemResponse(
                        "State or idempotency conflict",
                        Map.of(
                            "type", "https://walletwise.app/problems/idempotency_key_reused",
                            "title", "Conflict",
                            "status", 409,
                            "detail", "The key was already used with another request",
                            "code", "idempotency_key_reused",
                            "correlationId", "demo-correlation-123"))))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .servers(
            List.of(
                new Server()
                    .url(properties.publicUrl())
                    .description("Configured application URL")));
  }

  private static ApiResponse problemResponse(String description, Map<String, Object> example) {
    return new ApiResponse()
        .description(description)
        .content(
            new Content()
                .addMediaType(
                    "application/problem+json",
                    new MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))
                        .example(example)));
  }
}
