package com.turnero.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
  private final SecretKey key;
  private final long minutes;

  public JwtService(@Value("${app.jwt.secret}") String secret,
                    @Value("${app.jwt.expiration-minutes}") long minutes) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.minutes = minutes;
  }

  public String issue(Long userId, Long tenantId, String role) {
    Instant now = Instant.now();
    return Jwts.builder()
      .subject(String.valueOf(userId))
      .claim("tenantId", tenantId)
      .claim("role", role)
      .issuedAt(Date.from(now))
      .expiration(Date.from(now.plusSeconds(minutes * 60)))
      .signWith(key)
      .compact();
  }

  public AuthPrincipal parse(String token) {
    Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    return new AuthPrincipal(Long.valueOf(c.getSubject()), c.get("tenantId", Long.class), c.get("role", String.class));
  }
}
