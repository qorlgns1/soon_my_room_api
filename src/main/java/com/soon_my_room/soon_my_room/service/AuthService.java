package com.soon_my_room.soon_my_room.service;

import com.soon_my_room.soon_my_room.dto.LoginRequestDTO;
import com.soon_my_room.soon_my_room.dto.LoginResponseDTO;
import com.soon_my_room.soon_my_room.model.User;
import com.soon_my_room.soon_my_room.repository.UserRepository;
import com.soon_my_room.soon_my_room.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
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
}
