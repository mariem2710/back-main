package com.iset.gestion_projet.config;

import com.iset.gestion_projet.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Autowired
    public JwtAuthFilter(JwtService jwtService,
                         @Lazy UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        System.out.println("=== JWT FILTER ===");
        System.out.println("URL: " + request.getRequestURI());
        System.out.println("Auth Header: " + authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("❌ Pas de token Bearer");
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);
        boolean valid = jwtService.isTokenValid(token);
        System.out.println("✅ Token valid: " + valid);

        if (!valid) {
            System.out.println("❌ Token invalide");
            filterChain.doFilter(request, response);
            return;
        }

        final String email = jwtService.extractEmail(token);
        System.out.println("📧 Email extrait: " + email);

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                System.out.println("👤 User trouvé: " + userDetails.getUsername());
                System.out.println("🔐 Authorities: " + userDetails.getAuthorities());

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                System.out.println("✅ Authentification réussie pour: " + email);
            } catch (Exception e) {
                System.out.println("❌ Erreur chargement user: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}