package com.hemolife.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session != null
                && SecurityContextHolder
                .getContext()
                .getAuthentication() == null) {

            Object usuarioId =
                    session.getAttribute("usuarioId");

            Object ongId =
                    session.getAttribute("ongId");

            Object perfil =
                    session.getAttribute("perfil");

            Object principalId =
                    usuarioId != null
                            ? usuarioId
                            : ongId;

            if (principalId != null && perfil != null) {

                String role =
                        "ROLE_" +
                                perfil.toString()
                                        .trim()
                                        .toUpperCase();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principalId.toString(),
                                null,
                                List.of(
                                        new SimpleGrantedAuthority(role)
                                )
                        );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
            }
        }

        filterChain.doFilter(
                request,
                response
        );
    }
}