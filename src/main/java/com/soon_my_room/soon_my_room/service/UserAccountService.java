package com.soon_my_room.soon_my_room.service;

import com.soon_my_room.soon_my_room.dto.UserResponseDTO;
import com.soon_my_room.soon_my_room.exception.DuplicateResourceException;
import com.soon_my_room.soon_my_room.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자 계정 관련 서비스 계정명, 이메일 검증 등 계정 관련 기능을 담당합니다. */
@Service
@RequiredArgsConstructor
public class UserAccountService {

  private final UserRepository userRepository;

  /**
   * 계정명 중복 검증
   *
   * @param accountname 검증할 계정명
   * @return 검증 결과 응답 DTO
   */
  @Transactional(readOnly = true)
  public UserResponseDTO.AccountValidResponse validateAccountname(String accountname) {
    boolean exists = userRepository.existsByAccountname(accountname);
    String message = exists ? "이미 가입된 계정ID 입니다." : "사용 가능한 계정ID 입니다.";
    return UserResponseDTO.AccountValidResponse.builder().message(message).build();
  }

  /**
   * 이메일 중복 검증
   *
   * @param email 검증할 이메일
   * @return 검증 결과 응답 DTO
   */
  @Transactional(readOnly = true)
  public UserResponseDTO.EmailValidResponse validateEmail(String email) {
    boolean exists = userRepository.existsByEmail(email);
    String message = exists ? "이미 가입된 이메일 주소 입니다." : "사용 가능한 이메일 입니다.";
    return UserResponseDTO.EmailValidResponse.builder().message(message).build();
  }

  /**
   * 계정명 존재 여부 확인 (중복 검사)
   *
   * @param accountname 검증할 계정명
   * @throws DuplicateResourceException 이미 존재하는 계정명인 경우
   */
  @Transactional(readOnly = true)
  public void validateAccountnameNotExists(String accountname) {
    if (userRepository.existsByAccountname(accountname)) {
      throw new DuplicateResourceException("이미 사용중인 계정 ID입니다.");
    }
  }

  /**
   * 이메일 존재 여부 확인 (중복 검사)
   *
   * @param email 검증할 이메일
   * @throws DuplicateResourceException 이미 존재하는 이메일인 경우
   */
  @Transactional(readOnly = true)
  public void validateEmailNotExists(String email) {
    if (userRepository.existsByEmail(email)) {
      throw new DuplicateResourceException("이미 가입된 이메일 주소입니다.");
    }
  }
}
