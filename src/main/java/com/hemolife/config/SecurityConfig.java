package com.hemolife.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http

                // React roda em localhost:5173 e precisa acessar o Spring em localhost:5000
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Para o ambiente local/acadêmico.
                // Em produção, o ideal é configurar CSRF corretamente.
                .csrf(AbstractHttpConfigurer::disable)

                // O HemoLife usa autenticação por sessão/cookie.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                .authorizeHttpRequests(authorize -> authorize

                        // Importante: permite que erros reais como 404 apareçam
                        // em vez de serem transformados em "Login required".
                        .requestMatchers("/error").permitAll()

                        // Preflight do navegador
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ==========================================
                        // ROTAS DE TESTE
                        // ==========================================
                        .requestMatchers(HttpMethod.GET,
                                "/api/teste",
                                "/api/teste-postgres",
                                "/api/teste-mongo"
                        ).permitAll()

                        // ==========================================
                        // APIs PÚBLICAS
                        // ==========================================
                        .requestMatchers(HttpMethod.GET,
                                "/api/unidades",
                                "/api/ongs"
                        ).permitAll()

                        // ==========================================
                        // USUÁRIO - LOGIN / CADASTRO / SESSÃO
                        // ==========================================
                        .requestMatchers(HttpMethod.POST,
                                "/usuarios/login",
                                "/usuarios/cadastrar"
                        ).permitAll()

                        .requestMatchers(HttpMethod.GET,
                                "/usuarios/session"
                        ).permitAll()

                        .requestMatchers(HttpMethod.POST,
                                "/usuarios/logout"
                        ).authenticated()

                        // ==========================================
                        // ONG - LOGIN / CADASTRO / SESSÃO
                        // ==========================================
                        .requestMatchers(HttpMethod.POST,
                                "/ong/login",
                                "/ong/cadastrar"
                        ).permitAll()

                        .requestMatchers(HttpMethod.GET,
                                "/ong/session"
                        ).permitAll()

                        .requestMatchers(HttpMethod.POST,
                                "/ong/logout"
                        ).authenticated()

                        // ==========================================
                        // ADMIN
                        // ==========================================
                        .requestMatchers("/usuarios/admin/**")
                        .hasRole("ADMIN")

                        // ==========================================
                        // ONG AUTENTICADA
                        // ==========================================
                        .requestMatchers(HttpMethod.GET,
                                "/ong/membros"
                        ).hasRole("ONG")

                        // ==========================================
                        // DOADOR - ONGS
                        // ==========================================
                        .requestMatchers(HttpMethod.GET,
                                "/usuarios/ongs",
                                "/usuarios/minhas-ongs"
                        ).hasRole("DOADOR")

                        .requestMatchers(HttpMethod.POST,
                                "/usuarios/ongs/inscrever/**",
                                "/usuarios/ongs/cancelar/**"
                        ).hasRole("DOADOR")

                        // ==========================================
                        // DOADOR - EXAMES
                        // ==========================================
                        .requestMatchers(HttpMethod.GET,
                                "/usuarios/exames"
                        ).hasRole("DOADOR")

                        .requestMatchers(HttpMethod.POST,
                                "/usuarios/exames",
                                "/usuarios/exames/*/cancelar"
                        ).hasRole("DOADOR")

                        // ==========================================
                        // HOME
                        // ==========================================
                        .requestMatchers(HttpMethod.GET,
                                "/usuarios/home"
                        ).authenticated()

                        // Todo o restante permanece bloqueado.
                        .anyRequest().denyAll()
                )

                // Não queremos tela padrão de login do Spring
                .formLogin(AbstractHttpConfigurer::disable)

                // Não usar autenticação HTTP Basic
                .httpBasic(AbstractHttpConfigurer::disable)

                // Evita redirecionamentos automáticos do Spring Security
                .requestCache(AbstractHttpConfigurer::disable)

                // Respostas JSON para 401 e 403
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

        CorsConfiguration configuration = new CorsConfiguration();

        // Frontend React
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));

        // Métodos aceitos
        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "OPTIONS"
        ));

        // Headers aceitos
        configuration.setAllowedHeaders(List.of("*"));

        // Necessário para credentials: 'include'
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

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
