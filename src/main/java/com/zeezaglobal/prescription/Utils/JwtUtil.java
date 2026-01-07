package com.zeezaglobal.prescription.Utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private Long jwtExpirationMs;

    // Get the signing key
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    public String extractUserType(String token) {
        return extractRole(token);
    }
    // Extract username from token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extract email from token (same as username in most cases)
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extract user ID from token
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);



        Object userIdObj = claims.get("userId");
        if (userIdObj == null) {
            return null;
        }
        if (userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        }
        if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
        if (userIdObj instanceof String) {
            return Long.parseLong((String) userIdObj);
        }
        return null;
    }

    // Extract role from token
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        Object roleObj = claims.get("role");
        if (roleObj == null) {
            return null;
        }
        return roleObj.toString();
    }

    // Extract expiration date from token
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Extract a specific claim from token
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Extract all claims from token (For JJWT 0.12.x)
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Check if token is expired
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Generate token for user with userId claim
    public String generateToken(String email, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        return createToken(claims, email);
    }

    // Generate token for user with userId and role claims
    public String generateToken(String email, Long userId, String role) {


        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);

       

        return createToken(claims, email);
    }

    // Generate token for user
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }

    // Generate token with custom claims
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    // Create token with claims and subject
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Validate token against email
    public Boolean validateToken(String token, String email) {
        final String extractedEmail = extractEmail(token);
        return (extractedEmail.equals(email) && !isTokenExpired(token));
    }

    // Validate token against username
    public Boolean validateTokenByUsername(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    // Validate token against UserDetails
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // Additional helper method to check if token is valid (without username comparison)
    public Boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    // Get expiration time in milliseconds
    public Long getExpirationTime() {
        return jwtExpirationMs;
    }
}