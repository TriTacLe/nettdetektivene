package ntnu.no.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Exposes uploaded task assets as static files.
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

  private final Path uploadsRoot;

  public StaticResourceConfig(@Value("${app.uploads.root:uploads}") String uploadsRoot) {
    this.uploadsRoot = Paths.get(uploadsRoot).toAbsolutePath().normalize();
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/uploads/**")
      .addResourceLocations(this.uploadsRoot.toUri().toString() + "/");
  }
}
