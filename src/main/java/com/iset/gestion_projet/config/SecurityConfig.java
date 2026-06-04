package com.iset.gestion_projet.config;

import com.iset.gestion_projet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserRepository userRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth

                        // ── Public endpoints ──────────────────────────────
                        .requestMatchers(
                                "/api/users/login",
                                "/api/users/demande",
                                "/api/users/demandes/**",
                                "/api/v1/analyze",
                                "/health",
                                "/error",
                                "/api/users/hash"
                        ).permitAll()

                        // ── ADMIN only ────────────────────────────────────
                        .requestMatchers("/api/users", "/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/equipes", "/api/equipes/**").hasRole("ADMIN")
                        .requestMatchers("/api/membres", "/api/membres/**").hasRole("ADMIN")

                        // ── Authenticated (any role) ───────────────────────
                        .requestMatchers("/api/taches/**").authenticated()
                        .requestMatchers("/api/tickets/**").authenticated()
                        .requestMatchers("/api/sous-tickets/**").authenticated()
                        .requestMatchers("/api/commentaires/**").authenticated()
                        .requestMatchers("/api/profile/**").authenticated()

                        // ── Everything else requires authentication ────────
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}