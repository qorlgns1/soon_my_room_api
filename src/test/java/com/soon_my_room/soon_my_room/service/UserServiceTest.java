package com.soon_my_room.soon_my_room.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.soon_my_room.soon_my_room.dto.UserRequestDTO;
import com.soon_my_room.soon_my_room.dto.UserResponseDTO;
import com.soon_my_room.soon_my_room.exception.DuplicateResourceException;
import com.soon_my_room.soon_my_room.model.User;
import com.soon_my_room.soon_my_room.repository.FollowRepository;
import com.soon_my_room.soon_my_room.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private FollowRepository followRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private UserService userService;

  private UserRequestDTO.RegisterRequest.User registerUserRequest;
  private User savedUser;
  private List<User> userList;

  @BeforeEach
  void setUp() {
    // 회원가입 요청 데이터 설정
    registerUserRequest =
        UserRequestDTO.RegisterRequest.User.builder()
            .username("테스트유저")
            .email("test@example.com")
            .password("password123")
            .accountname("testuser")
            .intro("안녕하세요!")
            .image("https://example.com/profile.jpg")
            .build();

    // 저장된 User 엔티티 설정
    savedUser =
        User.builder()
            .id("user-id-1234")
            .username("테스트유저")
            .email("test@example.com")
            .password("encoded_password")
            .accountname("testuser")
            .intro("안녕하세요!")
            .image("https://example.com/profile.jpg")
            .createdAt(LocalDateTime.now())
            .build();

    // 사용자 검색 테스트용 사용자 목록 설정
    userList =
        Arrays.asList(
            User.builder()
                .id("user-id-1")
                .username("김테스트")
                .email("kim@example.com")
                .accountname("kimtest")
                .intro("안녕하세요 김테스트입니다")
                .image("https://example.com/kim.jpg")
                .build(),
            User.builder()
                .id("user-id-2")
                .username("이테스트")
                .email("lee@example.com")
                .accountname("leetest")
                .intro("안녕하세요 이테스트입니다")
                .image("https://example.com/lee.jpg")
                .build());
  }

  @Test
  @DisplayName("회원가입 성공 테스트")
  void registerUser_Success() {
    // Given
    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(userRepository.existsByAccountname(anyString())).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    // When
    UserResponseDTO.RegisterResponse response = userService.registerUser(registerUserRequest);

    // Then
    assertNotNull(response);
    assertEquals("회원가입 성공", response.getMessage());
    assertEquals(savedUser.getId(), response.getUser().getId());
    assertEquals(savedUser.getUsername(), response.getUser().getUsername());
    assertEquals(savedUser.getEmail(), response.getUser().getEmail());
    assertEquals(savedUser.getAccountname(), response.getUser().getAccountname());

    // Verify
    verify(userRepository).existsByEmail(registerUserRequest.getEmail());
    verify(userRepository).existsByAccountname(registerUserRequest.getAccountname());
    verify(passwordEncoder).encode(registerUserRequest.getPassword());
    verify(userRepository).save(any(User.class));
  }

  @Test
  @DisplayName("회원가입 실패 - 이메일 중복")
  void registerUser_EmailDuplicate() {
    // Given
    when(userRepository.existsByEmail(anyString())).thenReturn(true);

    // When & Then
    DuplicateResourceException exception =
        assertThrows(
            DuplicateResourceException.class, () -> userService.registerUser(registerUserRequest));

    assertEquals("이미 가입된 이메일 주소입니다.", exception.getMessage());

    // Verify
    verify(userRepository).existsByEmail(registerUserRequest.getEmail());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("회원가입 실패 - 계정명 중복")
  void registerUser_AccountnameDuplicate() {
    // Given
    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(userRepository.existsByAccountname(anyString())).thenReturn(true);

    // When & Then
    DuplicateResourceException exception =
        assertThrows(
            DuplicateResourceException.class, () -> userService.registerUser(registerUserRequest));

    assertEquals("이미 사용중인 계정 ID입니다.", exception.getMessage());

    // Verify
    verify(userRepository).existsByEmail(registerUserRequest.getEmail());
    verify(userRepository).existsByAccountname(registerUserRequest.getAccountname());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("이메일 검증 - 사용 가능한 이메일")
  void validateEmail_Available() {
    // Given
    String email = "new@example.com";
    when(userRepository.existsByEmail(email)).thenReturn(false);

    // When
    UserResponseDTO.EmailValidResponse response = userService.validateEmail(email);

    // Then
    assertNotNull(response);
    assertEquals("사용 가능한 이메일 입니다.", response.getMessage());

    // Verify
    verify(userRepository).existsByEmail(email);
  }

  @Test
  @DisplayName("이메일 검증 - 이미 사용 중인 이메일")
  void validateEmail_AlreadyInUse() {
    // Given
    String email = "existing@example.com";
    when(userRepository.existsByEmail(email)).thenReturn(true);

    // When
    UserResponseDTO.EmailValidResponse response = userService.validateEmail(email);

    // Then
    assertNotNull(response);
    assertEquals("이미 가입된 이메일 주소 입니다.", response.getMessage());

    // Verify
    verify(userRepository).existsByEmail(email);
  }

  @Test
  @DisplayName("계정명 검증 - 사용 가능한 계정명")
  void validateAccountname_Available() {
    // Given
    String accountname = "newaccount";
    when(userRepository.existsByAccountname(accountname)).thenReturn(false);

    // When
    UserResponseDTO.AccountValidResponse response = userService.validateAccountname(accountname);

    // Then
    assertNotNull(response);
    assertEquals("사용 가능한 계정ID 입니다.", response.getMessage());

    // Verify
    verify(userRepository).existsByAccountname(accountname);
  }

  @Test
  @DisplayName("계정명 검증 - 이미 사용 중인 계정명")
  void validateAccountname_AlreadyInUse() {
    // Given
    String accountname = "existingaccount";
    when(userRepository.existsByAccountname(accountname)).thenReturn(true);

    // When
    UserResponseDTO.AccountValidResponse response = userService.validateAccountname(accountname);

    // Then
    assertNotNull(response);
    assertEquals("이미 가입된 계정ID 입니다.", response.getMessage());

    // Verify
    verify(userRepository).existsByAccountname(accountname);
  }
}
