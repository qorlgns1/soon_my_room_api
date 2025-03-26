package com.soon_my_room.soon_my_room.service;

import com.soon_my_room.soon_my_room.dto.ProfileDTO;
import com.soon_my_room.soon_my_room.dto.UserRequestDTO;
import com.soon_my_room.soon_my_room.dto.UserResponseDTO;
import com.soon_my_room.soon_my_room.exception.DuplicateResourceException;
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

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final FollowRepository followRepository;
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

  /** 프로필 업데이트 */
  @Transactional
  public ProfileDTO.ProfileResponse updateProfile(
      String email, UserRequestDTO.UpdateProfileRequest.ProfileUser profileUser) {
    // 현재 사용자 찾기
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

    // 계정명이 변경되었고, 이미 다른 사용자가 사용 중인지 확인
    if (!user.getAccountname().equals(profileUser.getAccountname())
        && userRepository.existsByAccountname(profileUser.getAccountname())) {
      throw new DuplicateResourceException("이미 사용중이 계정 ID입니다.");
    }

    // 사용자 정보 업데이트
    user.setUsername(profileUser.getUsername());
    user.setAccountname(profileUser.getAccountname());
    user.setIntro(profileUser.getIntro());

    // 이미지가 제공되었을 경우에만 업데이트
    if (profileUser.getImage() != null && !profileUser.getImage().trim().isEmpty()) {
      user.setImage(profileUser.getImage());
    }

    // 저장
    userRepository.save(user);

    // 팔로워/팔로잉 목록 조회
    List<String> followers =
        followRepository.findByFollowingId(user.getId()).stream()
            .map(follow -> follow.getFollowerId())
            .collect(Collectors.toList());

    List<String> following =
        followRepository.findByFollowerId(user.getId()).stream()
            .map(follow -> follow.getFollowingId())
            .collect(Collectors.toList());

    // 응답 생성
    ProfileDTO.Profile profile =
        ProfileDTO.Profile.fromEntity(
            user,
            false, // 자신의 프로필이므로 isfollow는 false
            following,
            followers,
            following.size(),
            followers.size());

    return ProfileDTO.ProfileResponse.builder().profile(profile).build();
  }

  /** 사용자 검색 */
  @Transactional(readOnly = true)
  public List<UserResponseDTO.SearchUserResponse> searchUsers(String keyword) {
    List<User> users = userRepository.findByUsernameContainingOrAccountnameContaining(keyword);

    return users.stream()
        .map(
            user -> {
              // 팔로워/팔로잉 정보 조회
              List<String> following =
                  followRepository.findByFollowerId(user.getId()).stream()
                      .map(follow -> follow.getFollowingId())
                      .collect(Collectors.toList());

              List<String> followers =
                  followRepository.findByFollowingId(user.getId()).stream()
                      .map(follow -> follow.getFollowerId())
                      .collect(Collectors.toList());

              return UserResponseDTO.SearchUserResponse.builder()
                  .id(user.getId())
                  .username(user.getUsername())
                  .accountname(user.getAccountname())
                  .following(following)
                  .follower(followers)
                  .followerCount(followers.size())
                  .followingCount(following.size())
                  .build();
            })
        .collect(Collectors.toList());
  }
}
