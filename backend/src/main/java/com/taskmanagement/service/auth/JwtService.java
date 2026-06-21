package com.taskmanagement.service.auth;

import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public String generateToken(String username) { //Generate token
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String username) { //Generate refresh token have expiration time is double of access token
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractAllClaims(String token) { //Read all claims from token
        return Jwts.parser()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) { //Read username from token
        return extractAllClaims(token).getSubject();
    }

    private Date extractExpiration(String token) { //Read token expiration
        return extractAllClaims(token).getExpiration();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) { //Read token validity
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }
    private boolean isTokenExpired(String token) { //Read token expiration
        return extractExpiration(token).before(new Date());
    }

    private Key getSignKey() { //tạo khóa ký
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }
        // secretKey (Base64) → getSignKey() → Key object
        //                                         ↓
        // username → generateToken() → JWT string (header.payload.signature)
        //                                         ↓
        // JWT string → extractUsername() → read payload → get subject
        // JWT string → extractExpiration() → read payload → get expiration
        //                                         ↓
        // isTokenExpired() → comapre expiration with Date.now()
        // isTokenValid()   → username compare with UserDetails.getUsername() and isTokenExpired() check
}