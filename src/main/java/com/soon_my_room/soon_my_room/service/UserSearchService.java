package com.soon_my_room.soon_my_room.service;

import com.soon_my_room.soon_my_room.dto.UserResponseDTO;
import com.soon_my_room.soon_my_room.model.User;
import com.soon_my_room.soon_my_room.repository.FollowRepository;
import com.soon_my_room.soon_my_room.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자 검색 서비스 사용자 검색 기능을 담당합니다. */
@Service
@RequiredArgsConstructor
public class UserSearchService {

  private final UserRepository userRepository;
  private final FollowRepository followRepository;

  /**
   * 사용자 검색
   *
   * @param keyword 검색 키워드
   * @return 검색 결과 사용자 목록
   */
  @Transactional(readOnly = true)
  public List<UserResponseDTO.SearchUserResponse> searchUsers(String keyword) {
    List<User> users = userRepository.findByUsernameContainingOrAccountnameContaining(keyword);

    return users.stream().map(this::buildSearchUserResponse).collect(Collectors.toList());
  }

  /** 검색 결과 사용자 응답 구성 */
  private UserResponseDTO.SearchUserResponse buildSearchUserResponse(User user) {
    // 팔로워/팔로잉 정보 조회
    List<String> following = getFollowings(user);
    List<String> followers = getFollowers(user);

    return UserResponseDTO.SearchUserResponse.builder()
        .id(user.getId())
        .username(user.getUsername())
        .accountname(user.getAccountname())
        .following(following)
        .follower(followers)
        .followerCount(followers.size())
        .followingCount(following.size())
        .build();
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
