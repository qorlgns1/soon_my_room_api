package com.soon_my_room.soon_my_room.security;

import com.soon_my_room.soon_my_room.exception.JwtAuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  @Value("${app.jwt.secret}")
  private String secret;

  @Value("${app.jwt.expiration}")
  private long expirationTime;

  // 토큰 생성
  public String generateToken(String email) {
    return Jwts.builder()
        .subject(email)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + expirationTime))
        .signWith(getSigningKey())
        .compact();
  }

  // 클레임을 추가하여 토큰 생성
  public String generateToken(String email, Map<String, Object> claims) {
    return Jwts.builder()
        .subject(email)
        .claims(claims)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + expirationTime))
        .signWith(getSigningKey())
        .compact();
  }

  // 토큰에서 이메일 추출
  public String extractEmail(String token) {
    try {
      return extractClaim(token, Claims::getSubject);
    } catch (ExpiredJwtException e) {
      throw new JwtAuthenticationException(JwtAuthenticationException.ErrorType.TOKEN_EXPIRED, e);
    } catch (SignatureException e) {
      throw new JwtAuthenticationException(
          JwtAuthenticationException.ErrorType.TOKEN_SIGNATURE_INVALID, e);
    } catch (MalformedJwtException e) {
      throw new JwtAuthenticationException(JwtAuthenticationException.ErrorType.TOKEN_MALFORMED, e);
    } catch (UnsupportedJwtException e) {
      throw new JwtAuthenticationException(
          JwtAuthenticationException.ErrorType.TOKEN_INVALID_FORMAT, e);
    } catch (JwtException e) {
      throw new JwtAuthenticationException(JwtAuthenticationException.ErrorType.GENERAL_ERROR, e);
    }
  }

  // 토큰에서 만료 시간 추출
  public Date extractExpiration(String token) {
    try {
      return extractClaim(token, Claims::getExpiration);
    } catch (JwtException e) {
      throw new JwtAuthenticationException(
          JwtAuthenticationException.ErrorType.GENERAL_ERROR,
          "만료 시간 추출 중 오류 발생: " + e.getMessage());
    }
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    try {
      return Jwts.parser()
          .verifyWith(getSigningKey())
          .build()
          .parseSignedClaims(token)
          .getPayload();
    } catch (ExpiredJwtException e) {
      throw new JwtAuthenticationException(JwtAuthenticationException.ErrorType.TOKEN_EXPIRED, e);
    } catch (SignatureException e) {
      throw new JwtAuthenticationException(
          JwtAuthenticationException.ErrorType.TOKEN_SIGNATURE_INVALID, e);
    } catch (MalformedJwtException e) {
      throw new JwtAuthenticationException(JwtAuthenticationException.ErrorType.TOKEN_MALFORMED, e);
    } catch (UnsupportedJwtException e) {
      throw new JwtAuthenticationException(
          JwtAuthenticationException.ErrorType.TOKEN_INVALID_FORMAT, e);
    } catch (JwtException e) {
      throw new JwtAuthenticationException(JwtAuthenticationException.ErrorType.GENERAL_ERROR, e);
    }
  }

  private SecretKey getSigningKey() {
    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  // 토큰 유효성 검사
  public Boolean validateToken(String token, UserDetails userDetails) {
    try {
      final String email = extractEmail(token);
      return (email.equals(userDetails.getUsername()) && !isTokenExpired(token));
    } catch (JwtAuthenticationException e) {
      // 이미 특정 예외 유형으로 포장되어 있으므로 다시 던짐
      throw e;
    } catch (Exception e) {
      throw new JwtAuthenticationException(
          JwtAuthenticationException.ErrorType.GENERAL_ERROR,
          "토큰 검증 중 예상치 못한 오류 발생: " + e.getMessage());
    }
  }

  // 토큰 만료 확인
  private Boolean isTokenExpired(String token) {
    try {
      return extractExpiration(token).before(new Date());
    } catch (JwtAuthenticationException e) {
      // 이미 특정 예외 유형으로 포장되어 있으므로 다시 던짐
      throw e;
    } catch (Exception e) {
      throw new JwtAuthenticationException(
          JwtAuthenticationException.ErrorType.GENERAL_ERROR,
          "토큰 만료 확인 중 오류 발생: " + e.getMessage());
    }
  }
}
