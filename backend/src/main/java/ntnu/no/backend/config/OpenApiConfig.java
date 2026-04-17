package ntnu.no.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI configuration for the nettdetektivene API.
 * Configures API metadata and JWT bearer authentication scheme
 * so protected endpoints can be tested directly from Swagger UI.
 *
 * <p>Open <a href="http://localhost:8080/swagger-ui.html">http://localhost:8080/swagger-ui.html</a>
 * after starting the application.</p>
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Configuration
public class OpenApiConfig {

  /**
   * Configures the OpenAPI bean with API info and global JWT security scheme.
   *
   * @return the configured {@link OpenAPI} instance
   */
  @Bean
  public OpenAPI customOpenApi(){
    final String securitySchemaName = "bearerAuth";

    return new OpenAPI().info(new Info()
            .title("Nettdetektivene API")
            .version("0.1")
            .description("API for nettdetektivene. Backend and server-side access. Auth with JWT")
            .contact(new Contact()
                    .name("IDATT2106 Systemutvikling 2 gruppe 5")
                    .email("trile@stud.ntnu.no")))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemaName))
            .components(new Components()
                    .addSecuritySchemes(securitySchemaName, new SecurityScheme()
                            .name(securitySchemaName)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")));

  }
}
