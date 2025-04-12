package com.soon_my_room.soon_my_room.service;

import com.soon_my_room.soon_my_room.dto.ProfileDTO;
import com.soon_my_room.soon_my_room.dto.UserRequestDTO;
import com.soon_my_room.soon_my_room.dto.UserResponseDTO;
import com.soon_my_room.soon_my_room.exception.ResourceNotFoundException;
import com.soon_my_room.soon_my_room.model.User;
import com.soon_my_room.soon_my_room.repository.FollowRepository;
import com.soon_my_room.soon_my_room.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자 핵심 서비스 사용자 등록, 정보 조회, 프로필 업데이트 기능을 담당합니다. */
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final FollowRepository followRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserAccountService userAccountService;

  /**
   * 회원가입 처리
   *
   * @param requestUser 회원가입 요청 데이터
   * @return 등록된 사용자 정보
   */
  @Transactional
  public UserResponseDTO.RegisterResponse registerUser(
      UserRequestDTO.RegisterRequest.User requestUser) {
    // 이메일 중복 검사
    userAccountService.validateEmailNotExists(requestUser.getEmail());

    // 계정명 중복 검사
    userAccountService.validateAccountnameNotExists(requestUser.getAccountname());

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

  /**
   * 사용자 ID로 조회
   *
   * @param id 사용자 ID
   * @return 사용자 객체
   */
  @Transactional(readOnly = true)
  public User getUserById(String id) {
    return userRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + id));
  }

  /**
   * 사용자 이메일로 조회
   *
   * @param email 사용자 이메일
   * @return 사용자 객체
   */
  @Transactional(readOnly = true)
  public User getUserByEmail(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
  }

  /**
   * 프로필 업데이트
   *
   * @param email 현재 사용자 이메일
   * @param profileUser 업데이트할 프로필 정보
   * @return 업데이트된 프로필 정보
   */
  @Transactional
  public ProfileDTO.ProfileResponse updateProfile(
      String email, UserRequestDTO.UpdateProfileRequest.ProfileUser profileUser) {
    // 현재 사용자 찾기
    User user = getUserByEmail(email);

    // 계정명이 변경되었고, 이미 다른 사용자가 사용 중인지 확인
    if (!user.getAccountname().equals(profileUser.getAccountname())) {
      userAccountService.validateAccountnameNotExists(profileUser.getAccountname());
    }

    // 사용자 정보 업데이트
    updateUserInfo(user, profileUser);

    // 팔로워/팔로잉 목록 조회
    ProfileDTO.Profile profile = buildUserProfile(user);

    return ProfileDTO.ProfileResponse.builder().profile(profile).build();
  }

  /** 사용자 정보 업데이트 */
  private void updateUserInfo(
      User user, UserRequestDTO.UpdateProfileRequest.ProfileUser profileUser) {
    user.setUsername(profileUser.getUsername());
    user.setAccountname(profileUser.getAccountname());
    user.setIntro(profileUser.getIntro());

    // 이미지가 제공되었을 경우에만 업데이트
    if (profileUser.getImage() != null && !profileUser.getImage().trim().isEmpty()) {
      user.setImage(profileUser.getImage());
    }

    // 저장
    userRepository.save(user);
  }

  /** 사용자 프로필 정보 구성 */
  private ProfileDTO.Profile buildUserProfile(User user) {
    // 팔로워/팔로잉 목록 조회
    List<String> followers = getFollowers(user);
    List<String> following = getFollowings(user);

    // 응답 생성
    return ProfileDTO.Profile.fromEntity(
        user,
        false, // 자신의 프로필이므로 isfollow는 false
        following,
        followers,
        following.size(),
        followers.size());
  }

  /** 팔로워 목록 조회 */
  private List<String> getFollowers(User user) {
    return followRepository.findByFollowingId(user.getId()).stream()
        .map(follow -> follow.getFollowerId())
        .collect(Collectors.toList());
  }

  /** 팔로잉 목록 조회 */
  private List<String> getFollowings(User user) {
    return followRepository.findByFollowerId(user.getId()).stream()
        .map(follow -> follow.getFollowingId())
        .collect(Collectors.toList());
  }
}
