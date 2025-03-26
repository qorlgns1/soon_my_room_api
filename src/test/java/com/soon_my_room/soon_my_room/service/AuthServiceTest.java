package com.soon_my_room.soon_my_room.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.soon_my_room.soon_my_room.dto.AuthResponseDTO;
import com.soon_my_room.soon_my_room.dto.LoginRequestDTO;
import com.soon_my_room.soon_my_room.dto.LoginResponseDTO;
import com.soon_my_room.soon_my_room.model.User;
import com.soon_my_room.soon_my_room.repository.UserRepository;
import com.soon_my_room.soon_my_room.security.JwtUtil;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private JwtUtil jwtUtil;

  @InjectMocks private AuthService authService;

  private User testUser;
  private LoginRequestDTO.LoginRequest.LoginUser loginRequest;
  private String jwtToken;

  @BeforeEach
  void setUp() {
    // 테스트 사용자 설정
    testUser =
        User.builder()
            .id("user-id-1234")
            .username("테스트유저")
            .email("test@example.com")
            .password("encoded_password")
            .accountname("testuser")
            .intro("안녕하세요!")
            .image("https://example.com/image.jpg")
            .build();

    // 로그인 요청 설정
    loginRequest =
        LoginRequestDTO.LoginRequest.LoginUser.builder()
            .email("test@example.com")
            .password("password123")
            .build();

    // JWT 토큰 설정
    jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.sample.token";
  }

  @Test
  @DisplayName("로그인 성공 테스트")
  void login_Success() {
    // Given
    when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword()))
        .thenReturn(true);
    when(jwtUtil.generateToken(testUser.getEmail())).thenReturn(jwtToken);

    // When
    LoginResponseDTO response = authService.login(loginRequest);

    // Then
    assertNotNull(response);
    assertEquals(testUser.getId(), response.getUser().getId());
    assertEquals(testUser.getUsername(), response.getUser().getUsername());
    assertEquals(testUser.getEmail(), response.getUser().getEmail());
    assertEquals(testUser.getAccountname(), response.getUser().getAccountname());
    assertEquals(jwtToken, response.getUser().getToken());

    // Verify
    verify(userRepository).findByEmail(loginRequest.getEmail());
    verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPassword());
    verify(jwtUtil).generateToken(testUser.getEmail());
  }

  @Test
  @DisplayName("로그인 실패 - 사용자가 없는 경우")
  void login_UserNotFound() {
    // Given
    when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

    // When & Then
    UsernameNotFoundException exception =
        assertThrows(UsernameNotFoundException.class, () -> authService.login(loginRequest));

    assertEquals("이메일 또는 비밀번호가 일치하지 않습니다.", exception.getMessage());

    // Verify
    verify(userRepository).findByEmail(loginRequest.getEmail());
    verify(passwordEncoder, never()).matches(anyString(), anyString());
    verify(jwtUtil, never()).generateToken(anyString());
  }

  @Test
  @DisplayName("로그인 실패 - 비밀번호가 일치하지 않는 경우")
  void login_PasswordMismatch() {
    // Given
    when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword()))
        .thenReturn(false);

    // When & Then
    BadCredentialsException exception =
        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));

    assertEquals("이메일 또는 비밀번호가 일치하지 않습니다.", exception.getMessage());

    // Verify
    verify(userRepository).findByEmail(loginRequest.getEmail());
    verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPassword());
    verify(jwtUtil, never()).generateToken(anyString());
  }

  @Test
  @DisplayName("토큰 검증 성공 테스트")
  void validateToken_Success() {
    // Given
    when(jwtUtil.extractEmail(jwtToken)).thenReturn(testUser.getEmail());
    when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
    when(jwtUtil.validateToken(eq(jwtToken), any(UserDetails.class))).thenReturn(true);

    // When
    AuthResponseDTO.TokenValidResponse response = authService.validateToken(jwtToken);

    // Then
    assertNotNull(response);
    assertTrue(response.isValid());

    // Verify
    verify(jwtUtil).extractEmail(jwtToken);
    verify(userRepository).findByEmail(testUser.getEmail());
    verify(jwtUtil).validateToken(eq(jwtToken), any(UserDetails.class));
  }

  @Test
  @DisplayName("토큰 검증 실패 - 잘못된 토큰")
  void validateToken_InvalidToken() {
    // Given
    when(jwtUtil.extractEmail(jwtToken)).thenThrow(new RuntimeException("Invalid token"));

    // When
    AuthResponseDTO.TokenValidResponse response = authService.validateToken(jwtToken);

    // Then
    assertNotNull(response);
    assertFalse(response.isValid());

    // Verify
    verify(jwtUtil).extractEmail(jwtToken);
    verify(userRepository, never()).findByEmail(anyString());
    verify(jwtUtil, never()).validateToken(anyString(), any(UserDetails.class));
  }

  @Test
  @DisplayName("토큰 검증 실패 - 사용자가 존재하지 않음")
  void validateToken_UserNotFound() {
    // Given
    when(jwtUtil.extractEmail(jwtToken)).thenReturn(testUser.getEmail());
    when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.empty());

    // When
    AuthResponseDTO.TokenValidResponse response = authService.validateToken(jwtToken);

    // Then
    assertNotNull(response);
    assertFalse(response.isValid());

    // Verify
    verify(jwtUtil).extractEmail(jwtToken);
    verify(userRepository).findByEmail(testUser.getEmail());
    verify(jwtUtil, never()).validateToken(anyString(), any(UserDetails.class));
  }

  @Test
  @DisplayName("토큰 검증 실패 - 만료된 토큰")
  void validateToken_ExpiredToken() {
    // Given
    when(jwtUtil.extractEmail(jwtToken)).thenReturn(testUser.getEmail());
    when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
    when(jwtUtil.validateToken(eq(jwtToken), any(UserDetails.class))).thenReturn(false);

    // When
    AuthResponseDTO.TokenValidResponse response = authService.validateToken(jwtToken);

    // Then
    assertNotNull(response);
    assertFalse(response.isValid());

    // Verify
    verify(jwtUtil).extractEmail(jwtToken);
    verify(userRepository).findByEmail(testUser.getEmail());
    verify(jwtUtil).validateToken(eq(jwtToken), any(UserDetails.class));
  }
}
