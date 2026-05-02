package com.lpu.skillservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * ✅ CORS Config — allows frontend (React/Angular) to call this service.
 * CSRF is already disabled in SecurityConfig (stateless JWT APIs don't need it).
 *
 * WHERE TO PASTE: skillservice/src/main/java/com/lpu/skillservice/config/CorsConfig.java
 * Do the same for: sessionservice, skillservice, reviewservice, groupservice, notificationservice
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // ✅ Allow all origins (for dev) — restrict in production
        config.setAllowedOriginPatterns(List.of("*"));

        // ✅ Allow common HTTP methods
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // ✅ Allow Authorization header (JWT)
        config.setAllowedHeaders(List.of("*"));

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
