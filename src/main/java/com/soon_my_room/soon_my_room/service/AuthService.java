package com.soon_my_room.soon_my_room.service;

import com.soon_my_room.soon_my_room.dto.AuthResponseDTO;
import com.soon_my_room.soon_my_room.dto.LoginRequestDTO;
import com.soon_my_room.soon_my_room.dto.LoginResponseDTO;
import com.soon_my_room.soon_my_room.model.User;
import com.soon_my_room.soon_my_room.repository.UserRepository;
import com.soon_my_room.soon_my_room.security.JwtUtil;
import lombok.RequiredArgsConstructor;
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

  @Transactional
  public LoginResponseDTO login(LoginRequestDTO.LoginRequest.LoginUser loginRequest) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            loginRequest.getEmail(), loginRequest.getPassword()));

    var user =
        userRepository
            .findByEmail(loginRequest.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
    var token = jwtUtil.generateToken(user);

    return LoginResponseDTO.fromEntity(user, token);
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
}
