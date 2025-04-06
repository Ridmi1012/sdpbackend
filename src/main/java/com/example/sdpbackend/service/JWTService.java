package com.example.sdpbackend.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;


@Service
public class JWTService {
    @Value("${jwt.secret}")
    private String secret;
    private final long EXPIRATION_TIME = 864_000_000; // 10 days

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }


    public String generateToken(String username, String userType) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userType", userType)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey())
                .compact();
    }

//    public boolean validateToken(String token) {
//        try {
//            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
//            return true;
//        } catch (JwtException e) {
//            return false;
//        }
//    }
//
//    public Claims extractClaims(String token) {
//        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
//    }
}
