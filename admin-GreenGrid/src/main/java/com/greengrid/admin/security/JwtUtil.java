package com.greengrid.admin.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类（jjwt 0.12.x API）。
 * <p>注意：0.12.x 的写法与网上大量 0.9.x 旧教程完全不同——
 * 不再使用 {@code Jwts.parser().setSigningKey(String)}，
 * 而是 {@code Jwts.parser().verifyWith(SecretKey)}。</p>
 * <p>令牌里只放 userId（subject），用户角色等信息由拦截器实时查库获取，保证禁用/改角色立即生效。</p>
 */
@Component
public class JwtUtil {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtUtil(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发令牌。
     *
     * @param userId 用户主键
     */
    public String createToken(Long userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + properties.getExpireMinutes() * 60_000L);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    /**
     * 解析令牌并返回 userId；令牌非法或过期时抛出 {@link io.jsonwebtoken.JwtException}。
     */
    public Long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }
}
