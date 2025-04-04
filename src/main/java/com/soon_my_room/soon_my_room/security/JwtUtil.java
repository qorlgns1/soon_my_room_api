package com.soon_my_room.soon_my_room.security;

import com.soon_my_room.soon_my_room.exception.JwtAuthenticationException;
import com.soon_my_room.soon_my_room.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  @Value("${app.jwt.secret}")
  private String secret;

  @Value("${app.jwt.access-token.expiration}")
  private long accessTokenExpiration; // 짧게 설정 (예: 15분)

  @Value("${app.jwt.refresh-token.expiration}")
  private long refreshTokenExpiration; // 길게 설정 (예: 7일)

  @Getter private SecretKey signingKey;

  @PostConstruct
  public void init() {
    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    this.signingKey = Keys.hmacShaKeyFor(keyBytes);
  }

  public String generateAccessToken(User userDetails) {
    return generateToken(new HashMap<>(), userDetails, accessTokenExpiration);
  }

  public String generateRefreshToken(User userDetails) {
    return generateToken(new HashMap<>(), userDetails, refreshTokenExpiration);
  }

  // 토큰 생성 기능은 같으나 만료 시간을 파라미터로 받음
  private String generateToken(Map<String, Object> extraClaims, User userDetails, long expiration) {
    return Jwts.builder()
        .subject(userDetails.getUsername())
        .claims(extraClaims)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(getSigningKey(), Jwts.SIG.HS512)
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
    return extractClaim(token, Claims::getExpiration);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  // 토큰에서 모든 클레임 추출 및 예외 처리 통합
  private Claims extractAllClaims(String token) {
    try {
      return Jwts.parser()
          .verifyWith(getSigningKey()) // SecretKey 생성 메서드 사용
          .build()
          .parseSignedClaims(token)
          .getPayload();
    } catch (ExpiredJwtException e) {
      // 만료 예외 발생 시 JwtAuthenticationException으로 변환하여 던짐
      throw new JwtAuthenticationException(JwtAuthenticationException.ErrorType.TOKEN_EXPIRED, e);
    } catch (SignatureException e) {
      // 서명 검증 실패 예외 발생 시 JwtAuthenticationException으로 변환하여 던짐
      throw new JwtAuthenticationException(
          JwtAuthenticationException.ErrorType.TOKEN_SIGNATURE_INVALID, e);
    } catch (MalformedJwtException e) {
      // 토큰 형식 오류 예외 발생 시 JwtAuthenticationException으로 변환하여 던짐
      throw new JwtAuthenticationException(JwtAuthenticationException.ErrorType.TOKEN_MALFORMED, e);
    } catch (UnsupportedJwtException e) {
      // 지원되지 않는 토큰 형식 예외 발생 시 JwtAuthenticationException으로 변환하여 던짐
      throw new JwtAuthenticationException(
          JwtAuthenticationException.ErrorType.TOKEN_INVALID_FORMAT, e);
    } catch (IllegalArgumentException e) {
      // 잘못된 인자 예외 (예: 빈 토큰) 발생 시 JwtAuthenticationException으로 변환하여 던짐
      throw new JwtAuthenticationException(
          JwtAuthenticationException.ErrorType.TOKEN_INVALID_FORMAT,
          "Invalid token argument: " + e.getMessage());
    } catch (JwtException e) {
      // 기타 JWT 관련 예외 발생 시 JwtAuthenticationException으로 변환하여 던짐
      throw new JwtAuthenticationException(JwtAuthenticationException.ErrorType.GENERAL_ERROR, e);
    }
  }

  // 토큰 유효성 검사
  public Boolean validateToken(String token, User userDetails) {
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
    // 토큰 만료 시간 추출 후 현재 시간과 비교
    return extractExpiration(token).before(new Date());
  }
}
