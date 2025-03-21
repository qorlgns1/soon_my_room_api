package com.soon_my_room.soon_my_room.service;

import com.soon_my_room.soon_my_room.dto.ProfileDTO;
import com.soon_my_room.soon_my_room.exception.ResourceNotFoundException;
import com.soon_my_room.soon_my_room.model.User;
import com.soon_my_room.soon_my_room.repository.FollowRepository;
import com.soon_my_room.soon_my_room.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

  private final UserRepository userRepository;
  private final FollowRepository followRepository;

  /** 특정 사용자의 프로필 조회 */
  @Transactional(readOnly = true)
  public ProfileDTO.ProfileResponse getProfile(String accountname, String currentUserId) {
    User targetUser =
        userRepository
            .findByAccountname(accountname)
            .orElseThrow(() -> new ResourceNotFoundException("해당 계정이 존재하지 않습니다."));

    User currentUser =
        userRepository
            .findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("현재 사용자를 찾을 수 없습니다."));

    return buildProfileResponse(currentUser, targetUser);
  }

  /** 프로필 응답 구성 */
  private ProfileDTO.ProfileResponse buildProfileResponse(User currentUser, User targetUser) {
    // 팔로워 목록 조회
    List<String> followers =
        followRepository.findByFollowingId(targetUser.getId()).stream()
            .map(follow -> follow.getFollowerId())
            .collect(Collectors.toList());

    // 팔로잉 목록 조회
    List<String> followings =
        followRepository.findByFollowerId(targetUser.getId()).stream()
            .map(follow -> follow.getFollowingId())
            .collect(Collectors.toList());

    // 팔로워 및 팔로잉 수 계산
    int followerCount = followers.size();
    int followingCount = followings.size();

    // 현재 사용자가 대상 사용자를 팔로우하는지 확인
    boolean isFollowing =
        followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), targetUser.getId());

    // 프로필 응답 생성
    ProfileDTO.Profile profile =
        ProfileDTO.Profile.fromEntity(
            targetUser, isFollowing, followings, followers, followingCount, followerCount);

    return ProfileDTO.ProfileResponse.builder().profile(profile).build();
  }
}
