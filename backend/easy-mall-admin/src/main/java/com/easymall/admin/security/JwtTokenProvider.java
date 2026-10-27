package com.easymall.admin.security;

import com.easymall.admin.common.context.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKey;

/**
 * JWT 签发与解析。使用 HS256 对称签名。
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties properties;
    private SecretKey secretKey;

    @PostConstruct
    void init() {
        byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("easy-mall.jwt.secret 长度不足 32 字节，无法用于 HS256 签名");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(LoginUser loginUser) {
        return createToken(loginUser, properties.getAdminExpire());
    }

    public String createToken(LoginUser loginUser, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(loginUser.userId()))
                .issuer(properties.getIssuer())
                .claim("username", loginUser.username())
                .claim("nickname", loginUser.nickname())
                .claim("roles", String.join(",", loginUser.roles()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析令牌；签名错误、过期、格式非法均返回 Optional.empty()。
     */
    public Optional<LoginUser> parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(properties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new LoginUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get("username", String.class),
                    claims.get("nickname", String.class),
                    parseRoles(claims)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long getExpireSeconds() {
        return properties.getAdminExpire().toSeconds();
    }

    /**
     * 解析角色：新令牌为 roles（逗号分隔），兼容旧令牌的单值 role。
     */
    private List<String> parseRoles(Claims claims) {
        String roles = claims.get("roles", String.class);
        if (roles != null && !roles.isBlank()) {
            return Arrays.stream(roles.split(","))
                    .map(String::trim)
                    .filter(role -> !role.isEmpty())
                    .toList();
        }
        String legacyRole = claims.get("role", String.class);
        return legacyRole == null || legacyRole.isBlank() ? List.of() : List.of(legacyRole);
    }
}
