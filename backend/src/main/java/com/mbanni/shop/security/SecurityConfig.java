package com.mbanni.shop.security;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final SecurityErrorResponseWriter securityErrorResponseWriter;
    private final String frontendUrl;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          SecurityErrorResponseWriter securityErrorResponseWriter,
                          @Value("${app.frontend-url}") String frontendUrl

    ) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.securityErrorResponseWriter = securityErrorResponseWriter;
        this.frontendUrl=frontendUrl;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return
                http
                        .cors(Customizer.withDefaults())
                        .csrf(csrf -> csrf.disable())
                        .authorizeHttpRequests(auth -> auth
                                // Do not hide controller/service failures behind a 403
                                // when Spring performs its secondary /error dispatch.
                                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                                .requestMatchers("/",
                                        "/products",
                                        "/products/{id}",
                                        "/auth/register",
                                        "/auth/login").permitAll()

                                .requestMatchers(HttpMethod.POST, "/payments/webhook").permitAll()

                                .anyRequest().authenticated()

                        )
                        .exceptionHandling(errors -> errors
                                .authenticationEntryPoint((request, response, exception) ->
                                        securityErrorResponseWriter.writeUnauthorized(response)
                                )
                                .accessDeniedHandler((request, response, exception) ->
                                        securityErrorResponseWriter.writeAccessDenied(response)
                                )
                        )
                        .sessionManagement(session -> session
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        )
                        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                        .build();

    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                frontendUrl
        ));

        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type"
        ));

        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}