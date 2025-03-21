package com.soon_my_room.soon_my_room.service;

import com.soon_my_room.soon_my_room.dto.AuthResponseDTO;
import com.soon_my_room.soon_my_room.dto.LoginRequestDTO;
import com.soon_my_room.soon_my_room.dto.LoginResponseDTO;
import com.soon_my_room.soon_my_room.model.User;
import com.soon_my_room.soon_my_room.repository.UserRepository;
import com.soon_my_room.soon_my_room.security.JwtUtil;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;

  @Transactional
  public LoginResponseDTO login(LoginRequestDTO.LoginRequest.LoginUser loginRequest) {
    // 1. 이메일로 사용자 찾기
    User user =
        userRepository
            .findByEmail(loginRequest.getEmail())
            .orElseThrow(() -> new UsernameNotFoundException("이메일 또는 비밀번호가 일치하지 않습니다."));

    // 2. 비밀번호 검증
    if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
      throw new BadCredentialsException("이메일 또는 비밀번호가 일치하지 않습니다.");
    }

    // 3. JWT 토큰 생성
    String token = jwtUtil.generateToken(user.getEmail());

    // 4. 응답 DTO 생성
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

      // 토큰 유효성 검사
      UserDetails userDetails =
          new org.springframework.security.core.userdetails.User(
              user.getEmail(), user.getPassword(), new ArrayList<>());

      boolean isValid = jwtUtil.validateToken(token, userDetails);
      return AuthResponseDTO.TokenValidResponse.builder().isValid(isValid).build();
    } catch (Exception e) {
      // 토큰 파싱 실패 등 모든 예외는 유효하지 않은 토큰으로 처리
      return AuthResponseDTO.TokenValidResponse.builder().isValid(false).build();
    }
  }
}
