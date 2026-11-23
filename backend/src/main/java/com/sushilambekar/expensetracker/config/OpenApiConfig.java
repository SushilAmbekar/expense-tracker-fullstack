package com.sushilambekar.expensetracker.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI at /swagger-ui.html, with an "Authorize" button that takes the JWT from /api/auth/login. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI expenseTrackerOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Expense Tracker API").version("1.0.0")
                        .description("Track expenses, organise them by category, set monthly budgets and see spending analytics."))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
