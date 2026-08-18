package com.taskmanagement.service.auth;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.taskmanagement.dto.auth.AccessTokenClaims;
import com.taskmanagement.dto.auth.RefreshTokenClaims;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;

@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final String SID_CLAIM = "sid";

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public String generateAccessToken( String username,String sessionId) {
            return Jwts.builder()
                    .subject(username)
                    .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                    .claim(SID_CLAIM, sessionId)
                    .issuedAt(new Date())
                    .expiration(new Date(
                            System.currentTimeMillis() + expiration
                    ))
                    .signWith(getSignKey())
                    .compact();
        }

    public String generateRefreshToken(String username, String sessionId, String tokenId) {
        return Jwts.builder()
                .subject(username)
                .id(tokenId)
                .claim(SID_CLAIM, sessionId)
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSignKey())
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public AccessTokenClaims parseAccessToken(String token) {
        Claims claims = extractAllClaims(token);

        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        String sessionId = claims.get(SID_CLAIM, String.class);
        String username = claims.getSubject();

        if (!ACCESS_TOKEN_TYPE.equals(tokenType)) {
            throw new MalformedJwtException("Token is not an access token");
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new MalformedJwtException("Access token is missing sid");
        }
        if (username == null || username.isBlank()) {
            throw new MalformedJwtException("Access token is missing subject");
        }
        return new AccessTokenClaims(username, sessionId);
    }

    public RefreshTokenClaims parseRefreshToken(String token) {
        Claims claims = extractAllClaims(token);

        String tokenType =
                claims.get(TOKEN_TYPE_CLAIM, String.class);
        String sessionId =
                claims.get(SID_CLAIM, String.class);
        String tokenId =
                claims.getId();
        String username =
                claims.getSubject();

        if (!REFRESH_TOKEN_TYPE.equals(tokenType)) {
            throw new MalformedJwtException(
                    "Token is not a refresh token"
            );
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new MalformedJwtException(
                    "Refresh token is missing sid"
            );
        }
        if (tokenId == null || tokenId.isBlank()) {
            throw new MalformedJwtException(
                    "Refresh token is missing jti"
            );
        }
        if (username == null || username.isBlank()) {
            throw new MalformedJwtException(
                    "Refresh token is missing subject"
            );
        }
        return new RefreshTokenClaims(
                username,
                sessionId,
                tokenId
        );
    }

    public String extractSessionId(String token) {
        Claims claims = extractAllClaims(token);
        String sessionId = claims.get(SID_CLAIM, String.class);
        if (sessionId == null || sessionId.isBlank()) {
            throw new MalformedJwtException("Token is missing sid");
        }
        return sessionId;
    }

    public String extractUsername(String token) { //Read username from token
        return extractAllClaims(token).getSubject();
    }

    private Date extractExpiration(String token) { //Read token expiration
        return extractAllClaims(token).getExpiration();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        Claims claims = extractAllClaims(token);
        String username = claims.getSubject();
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);

        return username.equals(userDetails.getUsername())
                && !claims.getExpiration().before(new Date())
                && ACCESS_TOKEN_TYPE.equals(tokenType);
    }

    public boolean isAccessToken(String token) {
        return ACCESS_TOKEN_TYPE.equals(extractAllClaims(token).get(TOKEN_TYPE_CLAIM, String.class));
    }

    public boolean isRefreshToken(String token) {
        return REFRESH_TOKEN_TYPE.equals(extractAllClaims(token).get(TOKEN_TYPE_CLAIM, String.class));
    }
    private boolean isTokenExpired(String token) { //Read token expiration
        return extractExpiration(token).before(new Date());
    }

    private Key getSignKey() {
        String normalizedSecret = secretKey == null ? "" : secretKey.trim();

        try {
            byte[] decoded = Decoders.BASE64.decode(normalizedSecret);
            if (decoded.length >= 32) {
                return Keys.hmacShaKeyFor(decoded);
            }
        } catch (DecodingException ignored) {
            // Fall back to plain-text secret handling below
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalizedSecret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to initialize JWT signing key", ex);
        }
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
