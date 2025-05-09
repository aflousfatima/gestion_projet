        package com.auth.authentification_service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class TestSecurityConfig {
    public TestSecurityConfig() {
        System.out.println("TestSecurityConfig chargé");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.disable())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/signup", "/api/login", "/api/refresh", "/api/logout", "/actuator/health", "/api/invitations/verify", "/api/project-members/by-user", "/api/update", "/api/change-password").permitAll()
                        .requestMatchers("/swagger.html", "/swagger-ui/**", "/swagger-ui.html", "/docs/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/invitations").hasRole("MANAGER")
                        .requestMatchers("/api/me", "/api/user-id", "/api/assign-manager-role", "/api/auth/decode-token", "/api/tasks_reponsibles/by-ids").authenticated()
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
