package com.KEYSTONE.ServiceManagement.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.lang.NonNull;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import org.springframework.stereotype.Component;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    // =========================================================
    // SKIP CORS PREFLIGHT REQUESTS
    // =========================================================

    @Override
    protected boolean shouldNotFilter(
            @NonNull HttpServletRequest request) {

        return "OPTIONS".equalsIgnoreCase(
                request.getMethod()
        );
    }

    // =========================================================
    // JWT FILTER
    // =========================================================

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // -------------------------------------------------------
        // GET AUTHORIZATION HEADER
        // -------------------------------------------------------

        final String authHeader =
                request.getHeader("Authorization");

        // -------------------------------------------------------
        // NO JWT
        // -------------------------------------------------------

        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        // -------------------------------------------------------
        // EXTRACT JWT
        // -------------------------------------------------------

        final String jwt =
                authHeader.substring(7);

        try {

            // ---------------------------------------------------
            // EXTRACT USERNAME
            // ---------------------------------------------------

            final String username =
                    jwtService.extractUsername(jwt);

            // ---------------------------------------------------
            // AUTHENTICATE USER
            // ---------------------------------------------------

            if (username != null &&
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication() == null) {

                UserDetails userDetails =
                        userDetailsService
                                .loadUserByUsername(username);

                // ------------------------------------------------
                // VALIDATE TOKEN
                // ------------------------------------------------

                if (jwtService.isTokenValid(
                        jwt,
                        userDetails.getUsername())) {

                    UsernamePasswordAuthenticationToken
                            authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(authToken);
                }
            }

        } catch (Exception ex) {

            // ---------------------------------------------------
            // INVALID / EXPIRED TOKEN
            // ---------------------------------------------------

            SecurityContextHolder.clearContext();
        }

        // -------------------------------------------------------
        // CONTINUE REQUEST
        // -------------------------------------------------------

        filterChain.doFilter(request, response);
    }
}
