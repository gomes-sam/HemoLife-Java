package com.hemolife.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    private final SessionAuthenticationFilter sessionAuthenticationFilter;

    public SecurityConfig(
            SessionAuthenticationFilter sessionAuthenticationFilter
    ) {
        this.sessionAuthenticationFilter = sessionAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        return http

                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource())
                )

                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                )

                .authorizeHttpRequests(authorize -> authorize

                        .requestMatchers("/error").permitAll()

                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/teste",
                                "/api/teste-postgres",
                                "/api/teste-mongo"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/unidades",
                                "/api/ongs"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/usuarios/cadastrar",
                                "/usuarios/login",
                                "/usuarios/logout"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/usuarios/session"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/ong/login",
                                "/ong/cadastrar",
                                "/ong/logout"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/ong/session"
                        ).permitAll()

                        .requestMatchers(
                                "/usuarios/admin/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/ong/membros"
                        ).hasRole("ONG")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/usuarios/ongs",
                                "/usuarios/minhas-ongs"
                        ).hasRole("DOADOR")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/usuarios/ongs/inscrever/**",
                                "/usuarios/ongs/cancelar/**"
                        ).hasRole("DOADOR")


                        .requestMatchers(
                                HttpMethod.GET,
                                "/usuarios/exames",
                                "/usuarios/exames/*/arquivo"
                        ).hasRole("DOADOR")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/usuarios/exames",
                                "/usuarios/exames/*/cancelar",
                                "/usuarios/exames/*/arquivo"
                        ).hasRole("DOADOR")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/usuarios/exames/*/arquivo"
                        ).hasRole("DOADOR")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/usuarios/home"
                        ).authenticated()

                        .anyRequest().denyAll()
                )

                .addFilterBefore(
                        sessionAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)

                .exceptionHandling(exception -> exception

                        .authenticationEntryPoint(
                                (request, response, authException) ->
                                        escreverErro(
                                                response,
                                                HttpServletResponse.SC_FORBIDDEN,
                                                "Acesso negado"
                                        )
                        )

                        .accessDeniedHandler(
                                (request, response, accessDeniedException) ->
                                        escreverErro(
                                                response,
                                                HttpServletResponse.SC_FORBIDDEN,
                                                "Acesso negado"
                                        )
                        )
                )

                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(
                List.of("*")
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    private static void escreverErro(
            HttpServletResponse response,
            int status,
            String mensagem
    ) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                """
                {
                  "success": false,
                  "message": "%s"
                }
                """.formatted(mensagem)
        );
    }
}