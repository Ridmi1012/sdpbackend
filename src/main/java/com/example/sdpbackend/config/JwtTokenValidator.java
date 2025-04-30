package com.example.sdpbackend.config;

import com.example.sdpbackend.service.JWTService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class JwtTokenValidator {
    private static final Logger logger = Logger.getLogger(JwtTokenValidator.class.getName());

    private final JWTService jwtService;

    @Autowired
    public JwtTokenValidator(JWTService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Extract user ID from JWT token in the request
     * @param request The HTTP request containing the token
     * @return User ID or null if not found/invalid
     */
    public Integer getUserIdFromToken(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null) {
            return null;
        }

        try {
            if (!jwtService.validateToken(token)) {
                return null;
            }

            Claims claims = jwtService.extractClaims(token);
            String username = claims.getSubject();

            // User ID would need to be stored in the token claims for this to work
            // For this example, we're just retrieving the subject (username)
            return null;
        } catch (Exception e) {
            logger.warning("Error extracting user ID from token: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extract username from JWT token in the request
     * @param request The HTTP request containing the token
     * @return Username or null if not found/invalid
     */
    public String getUsernameFromToken(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null) {
            return null;
        }

        try {
            if (!jwtService.validateToken(token)) {
                return null;
            }

            Claims claims = jwtService.extractClaims(token);
            return claims.getSubject();
        } catch (Exception e) {
            logger.warning("Error extracting username from token: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if the user has a specific role
     * @param request The HTTP request containing the token
     * @param role The role to check for
     * @return true if the user has the role, false otherwise
     */
    public boolean hasRole(HttpServletRequest request, String role) {
        String token = extractTokenFromRequest(request);
        if (token == null) {
            return false;
        }

        try {
            if (!jwtService.validateToken(token)) {
                return false;
            }

            Claims claims = jwtService.extractClaims(token);
            String userType = claims.get("userType", String.class);

            return role.equals(userType);
        } catch (Exception e) {
            logger.warning("Error checking role from token: " + e.getMessage());
            return false;
        }
    }

    /**
     * Extract the token from the Authorization header
     * @param request The HTTP request
     * @return The token or null if not found
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
