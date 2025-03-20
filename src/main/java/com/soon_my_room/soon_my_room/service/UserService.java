package com.soon_my_room.soon_my_room.service;

import com.soon_my_room.soon_my_room.dto.UserRequestDTO;
import com.soon_my_room.soon_my_room.dto.UserResponseDTO;
import com.soon_my_room.soon_my_room.exception.DuplicateResourceException;
import com.soon_my_room.soon_my_room.exception.ResourceNotFoundException;
import com.soon_my_room.soon_my_room.model.User;
import com.soon_my_room.soon_my_room.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  /** 회원가입 처리 */
  @Transactional
  public UserResponseDTO.RegisterResponse registerUser(
      UserRequestDTO.RegisterRequest.User requestUser) {
    // 이메일 중복 검사
    if (userRepository.existsByEmail(requestUser.getEmail())) {
      throw new DuplicateResourceException("이미 가입된 이메일 주소입니다.");
    }

    // 계정명 중복 검사
    if (userRepository.existsByAccountname(requestUser.getAccountname())) {
      throw new DuplicateResourceException("이미 사용중인 계정 ID입니다.");
    }

    // 이미지가 없는 경우 기본 이미지 설정
    String imageUrl = requestUser.getImage();
    if (imageUrl == null || imageUrl.trim().isEmpty()) {
      imageUrl = "https://api.mandarin.weniv.co.kr/Ellipse.png";
    }

    // User 엔티티 생성
    User user =
        User.builder()
            .username(requestUser.getUsername())
            .email(requestUser.getEmail())
            .password(passwordEncoder.encode(requestUser.getPassword())) // 비밀번호 인코딩
            .accountname(requestUser.getAccountname())
            .intro(requestUser.getIntro())
            .image(imageUrl)
            .build();

    // 데이터베이스에 저장
    User savedUser = userRepository.save(user);

    // 응답 DTO 생성
    return UserResponseDTO.RegisterResponse.fromEntity(savedUser);
  }

  /** 사용자 ID로 조회 */
  @Transactional(readOnly = true)
  public User getUserById(String id) {
    return userRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + id));
  }

  @Transactional(readOnly = true)
  public UserResponseDTO.AccountValidResponse validateAccountname(String accountname) {
    boolean exists = userRepository.existsByAccountname(accountname);

    String message = exists ? "이미 가입된 계정ID 입니다." : "사용 가능한 계정ID 입니다.";

    return UserResponseDTO.AccountValidResponse.builder().message(message).build();
  }

  /** 이메일 중복 검증 */
  @Transactional(readOnly = true)
  public UserResponseDTO.EmailValidResponse validateEmail(String email) {
    boolean exists = userRepository.existsByEmail(email);

    String message = exists ? "이미 가입된 이메일 주소 입니다." : "사용 가능한 이메일 입니다.";

    return UserResponseDTO.EmailValidResponse.builder().message(message).build();
  }
}
