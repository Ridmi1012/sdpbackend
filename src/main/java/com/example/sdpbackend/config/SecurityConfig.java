package com.example.sdpbackend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // Public endpoints that don't require authentication
                        .requestMatchers("/api/auth/**").permitAll() // Allow all auth endpoints including refresh
                        .requestMatchers("/api/customers").permitAll() // Allow registration
                        .requestMatchers("/api/customers/register").permitAll()
                        .requestMatchers("/api/designs/public/**").permitAll()

                        .requestMatchers("/api/password/forgot").permitAll()
                        .requestMatchers("/api/password/reset").permitAll()
                        .requestMatchers("/api/password/verify-code").permitAll()

                        // Allow notification subscription without authentication
                        .requestMatchers("/api/notifications/subscribe").permitAll()

                        // Allow GET requests to items endpoint without authentication
                        .requestMatchers(request ->
                                request.getMethod().equals("GET") &&
                                        request.getRequestURI().startsWith("/api/items")
                        ).permitAll()

                        // Allow GET requests to categories endpoint without authentication
                        .requestMatchers(request ->
                                request.getMethod().equals("GET") &&
                                        request.getRequestURI().startsWith("/api/categories")
                        ).permitAll()

                        // Allow GET requests to designs endpoint without authentication
                        .requestMatchers(request ->
                                request.getMethod().equals("GET") &&
                                        request.getRequestURI().startsWith("/api/designs")
                        ).permitAll()

                        // Allow GET requests to reviews endpoint without authentication
                        .requestMatchers(request ->
                                request.getMethod().equals("GET") &&
                                        request.getRequestURI().startsWith("/api/reviews")
                        ).permitAll()

                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/notifications/unread/count").hasRole("ADMIN")

                        // Only admin can modify items (POST, PUT, DELETE)
                        .requestMatchers(request ->
                                !request.getMethod().equals("GET") &&
                                        request.getRequestURI().startsWith("/api/items")
                        ).hasRole("ADMIN")

                        // Only admin can modify categories (POST, PUT, DELETE)
                        .requestMatchers(request ->
                                !request.getMethod().equals("GET") &&
                                        request.getRequestURI().startsWith("/api/categories")
                        ).hasRole("ADMIN")

                        // Only admin can modify designs (POST, PUT, DELETE)
                        .requestMatchers(request ->
                                !request.getMethod().equals("GET") &&
                                        request.getRequestURI().startsWith("/api/designs")
                        ).hasRole("ADMIN")

                        // Updated order endpoints permissions
                        .requestMatchers(HttpMethod.POST, "/api/orders").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/orders").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/orders/new").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/orders/ongoing").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/orders/customer/**").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/orders/payment").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/orders/payhere/initiate").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/orders/payhere/verify").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/orders/payhere/notify").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/orders/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/orders/payments").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/orders/verified/recent").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/orders/{orderId}/payment/{paymentId}/verify").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/orders/{orderId}/payment-slip").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/orders/{orderId}/confirm").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/orders/{orderId}/cancel").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/orders/{orderId}/event-details").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/orders/{orderId}/update").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}