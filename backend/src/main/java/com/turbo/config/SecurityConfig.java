package com.turbo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.turbo.user.model.enums.UserRole;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String AUTH_ENDPOINTS = "/api/auth/**";
    private static final String BOOKING_PHOTOS_ENDPOINTS = "/api/bookings/photos/**";
    private static final String DRIVER_ENDPOINTS = "/api/driver/**";
    private static final String OWNER_ENDPOINTS = "/api/owner/**";
    private static final String ADMIN_ENDPOINTS = "/api/admin/**";

    private final JwtAuthFilter jwtAuthFilter;

    /** Configures the HTTP security filter chain with JWT-based stateless authentication. */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(AUTH_ENDPOINTS).permitAll()
                .requestMatchers(BOOKING_PHOTOS_ENDPOINTS).authenticated()
                .requestMatchers(DRIVER_ENDPOINTS).hasAuthority(UserRole.DRIVER.name())
                .requestMatchers(OWNER_ENDPOINTS).hasAuthority(UserRole.CAR_OWNER.name())
                .requestMatchers(ADMIN_ENDPOINTS).hasAuthority(UserRole.ADMIN.name())
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Provides the BCrypt password encoder bean. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Exposes the authentication manager bean. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
