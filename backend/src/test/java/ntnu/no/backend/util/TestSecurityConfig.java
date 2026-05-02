package ntnu.no.backend.util;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enables {@code @PreAuthorize} enforcement in {@code @WebMvcTest} slices.
 * Import this in controller tests that verify role-based access.
 */
@TestConfiguration
@EnableMethodSecurity
public class TestSecurityConfig {}
