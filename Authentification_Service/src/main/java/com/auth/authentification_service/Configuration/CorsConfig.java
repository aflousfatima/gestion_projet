package com.auth.authentification_service.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of("https://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")); //All Méthodes autorisées
        config.setAllowedHeaders(List.of(
                "Accept", "Accept-Language", "Content-Language", "Content-Type",
                "Authorization", "Cookie", "X-Requested-With", "Origin", "Host"
        ));
        config.setAllowCredentials(true); // Autorise credentials (cookies, tokens, etc.)
        config.setExposedHeaders(List.of("Set-Cookie")); // Permet au client d’accéder au Set-Cookie

        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
