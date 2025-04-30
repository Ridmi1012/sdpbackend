package com.example.sdpbackend.utill;

import java.security.SecureRandom;
import java.util.Base64;

public class JwtUtil {
    /**
     * Generates a secure random string suitable for use as a JWT secret key
     * @param length Length of the random string to generate
     * @return Base64-encoded random string
     */
    public static String generateSecureSecret(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Main method to generate and print a secure JWT secret
     * This is for development/setup purposes only and should not be used in production
     */
    public static void main(String[] args) {
        // Generate a 64-byte (512-bit) secret key
        String secret = generateSecureSecret(64);
        System.out.println("Generated JWT Secret:");
        System.out.println(secret);
        System.out.println("Add this to your application.properties as jwt.secret=" + secret);
    }
}
