package com.bt.main.config;

import com.bt.main.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
<<<<<<< HEAD
                        // Allow access to static resources and frontend routes
                        .requestMatchers(
                            "/",
                            "/index.html",
                            "/static/**",
                            "/assets/**",
                            "/favicon.ico",
                            "/vite.svg",
                            "/login",
                            "/register",
                            "/dashboard",
                            "/profile",
                            "/products/**",
                            "/fd-calculator",
                            "/accounts/**",
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/docs/**"
                        ).permitAll()
                        // Allow access to API endpoints (they will be handled by JWT filter)
                        .requestMatchers("/api/**").permitAll()
=======
>>>>>>> origin/master
                        .anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
