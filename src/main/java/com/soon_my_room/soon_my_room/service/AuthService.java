package com.soon_my_room.soon_my_room.service;

import com.soon_my_room.soon_my_room.dto.AuthResponseDTO;
import com.soon_my_room.soon_my_room.dto.LoginRequestDTO;
import com.soon_my_room.soon_my_room.dto.LoginResponseDTO;
import com.soon_my_room.soon_my_room.exception.JwtAuthenticationException;
import com.soon_my_room.soon_my_room.exception.ResourceNotFoundException;
import com.soon_my_room.soon_my_room.model.User;
import com.soon_my_room.soon_my_room.repository.UserRepository;
import com.soon_my_room.soon_my_room.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final JwtUtil jwtUtil;
  private final AuthenticationManager authenticationManager;

  @Value("${app.jwt.refresh-token.expiration}")
  private long refreshTokenExpirationMs;

  @Transactional
  public LoginResponseDTO login(
      LoginRequestDTO.LoginRequest.LoginUser loginRequest, HttpServletResponse response) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            loginRequest.getEmail(), loginRequest.getPassword()));

    var user =
        userRepository
            .findByEmail(loginRequest.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
    String accessToken = jwtUtil.generateAccessToken(user);
    String refreshToken = jwtUtil.generateRefreshToken(user);

    // Refresh Token을 DB에 저장
    user.setRefreshToken(refreshToken);
    userRepository.save(user);

    // Refresh Token을 HttponLy 쿠키로 설정
    Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
    refreshTokenCookie.setHttpOnly(true);
    refreshTokenCookie.setSecure(true); // HTTPS에서만 전송
    refreshTokenCookie.setPath("/api/user/refresh"); // refresh 엔드포인트에서만 사용
    // 밀리초를 초로 변환하여 설정
    int refreshTokenMaxAgeInSeconds =
        (int) TimeUnit.MILLISECONDS.toSeconds(refreshTokenExpirationMs);
    refreshTokenCookie.setMaxAge(refreshTokenMaxAgeInSeconds);
    response.addCookie(refreshTokenCookie);

    // 응답에는 Access Token만 포함 (기존과 동일한 방식)
    return LoginResponseDTO.fromEntity(user, accessToken);
  }

  // 새 Access Token 발급 메소드 추가
  @Transactional
  public LoginResponseDTO refreshAccessToken(String refreshToken) {
    // Refresh Token 검증
    String email = jwtUtil.extractEmail(refreshToken);
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(
                () ->
                    new JwtAuthenticationException(
                        JwtAuthenticationException.ErrorType.USER_NOT_FOUND));

    // DB에 저장된 Refresh Token과 비교
    if (user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshToken)) {
      throw new JwtAuthenticationException(
          JwtAuthenticationException.ErrorType.TOKEN_INVALID_FORMAT);
    }

    // 새 Access Token 발급
    String newAccessToken = jwtUtil.generateAccessToken(user);

    // 응답에는 새 Access Token만 포함
    return LoginResponseDTO.fromEntity(user, newAccessToken);
  }

  @Transactional(readOnly = true)
  public AuthResponseDTO.TokenValidResponse validateToken(String token) {
    try {
      // 토큰에서 이메일 추출 시도
      String email = jwtUtil.extractEmail(token);

      // 사용자가 존재하는지 확인
      User user =
          userRepository
              .findByEmail(email)
              .orElseThrow(() -> new UsernameNotFoundException("User not found"));

      boolean isValid = jwtUtil.validateToken(token, user);
      return AuthResponseDTO.TokenValidResponse.builder().isValid(isValid).build();
    } catch (Exception e) {
      // 토큰 파싱 실패 등 모든 예외는 유효하지 않은 토큰으로 처리
      return AuthResponseDTO.TokenValidResponse.builder().isValid(false).build();
    }
  }

  // 로그아웃 메소드 추가
  @Transactional
  public void logout(String email, HttpServletResponse response) {
    // 사용자 찾기
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

    // DB에서 Refresh Token 제거
    user.setRefreshToken(null);
    userRepository.save(user);

    // 쿠키에서 Refresh Token 제거
    Cookie refreshTokenCookie = new Cookie("refresh_token", null);
    refreshTokenCookie.setHttpOnly(true);
    refreshTokenCookie.setSecure(true);
    refreshTokenCookie.setPath("/api/user/refresh");
    refreshTokenCookie.setMaxAge(0); // 즉시 만료
    response.addCookie(refreshTokenCookie);
  }
}
