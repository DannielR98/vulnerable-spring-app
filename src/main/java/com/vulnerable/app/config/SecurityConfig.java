package com.vulnerable.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ⚠️ VULNERABILITY: Intentionally misconfigured Spring Security
 *
 * Issues:
 *  - CSRF disabled
 *  - Frameptions disabled (allows clickjacking)
 *  - All endpoints permitted without authentication
 *  - No HTTPS enforcement
 *  - H2 console exposed
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ⚠️ CSRF protection disabled
            .csrf(AbstractHttpConfigurer::disable)

            // ⚠️ Allow framing (clickjacking risk)
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
            )

            // ⚠️ All requests permitted — no authentication required
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
