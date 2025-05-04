package com.example.sdpbackend.config;

import com.example.sdpbackend.service.JWTService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.logging.Logger;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = Logger.getLogger(JwtAuthenticationFilter.class.getName());

    @Autowired
    private JWTService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String userType = null;
        String jwt = null;

        // Check if the Authorization header is present and starts with "Bearer "
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                // Validate token and extract claims
                if (jwtService.validateToken(jwt)) {
                    Claims claims = jwtService.extractClaims(jwt);
                    username = claims.getSubject();
                    userType = claims.get("userType", String.class);
                    logger.info("Processing token for user: " + username + " with role: " + userType);

                    // Set up Spring Security authentication
                    if (username != null && userType != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        // Create an authentication token with the appropriate authority based on userType
                        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + userType.toUpperCase());

                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                username, null, Collections.singletonList(authority));

                        // Add request details to the authentication token
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        // Set the authentication in the SecurityContext
                        SecurityContextHolder.getContext().setAuthentication(authToken);

                        logger.info("Authenticated user: " + username + " with role: ROLE_" + userType.toUpperCase());
                    }
                }
            } catch (Exception e) {
                logger.warning("Invalid JWT token: " + e.getMessage());
                // Continue the chain without authentication
            }
        }

        // Continue the filter chain
        filterChain.doFilter(request, response);
    }
}
