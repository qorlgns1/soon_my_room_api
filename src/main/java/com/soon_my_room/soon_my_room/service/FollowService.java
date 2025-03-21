package com.soon_my_room.soon_my_room.service;

import com.soon_my_room.soon_my_room.dto.ProfileDTO;
import com.soon_my_room.soon_my_room.exception.ResourceNotFoundException;
import com.soon_my_room.soon_my_room.model.Follow;
import com.soon_my_room.soon_my_room.model.FollowId;
import com.soon_my_room.soon_my_room.model.User;
import com.soon_my_room.soon_my_room.repository.FollowRepository;
import com.soon_my_room.soon_my_room.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowService {

  private final UserRepository userRepository;
  private final FollowRepository followRepository;
  private final ProfileService profileService;

  @Transactional
  public ProfileDTO.ProfileResponse followUser(String followerId, String targetAccountname) {
    // 현재 사용자 조회
    User follower =
        userRepository
            .findById(followerId)
            .orElseThrow(() -> new ResourceNotFoundException("현재 사용자를 찾을 수 없습니다."));

    // 팔로우할 대상 사용자 조회
    User following =
        userRepository
            .findByAccountname(targetAccountname)
            .orElseThrow(() -> new ResourceNotFoundException("해당 계정이 존재하지 않습니다."));

    // 자기 자신을 팔로우하는지 확인
    if (follower.getId().equals(following.getId())) {
      throw new IllegalArgumentException("자기 자신을 팔로우 할 수 없습니다.");
    }

    // 이미 팔로우 중인지 확인 후 팔로우 관계 생성
    if (!followRepository.existsByFollowerIdAndFollowingId(follower.getId(), following.getId())) {
      Follow follow =
          Follow.builder().followerId(follower.getId()).followingId(following.getId()).build();
      followRepository.save(follow);
    }

    // 프로필 조회
    return profileService.getProfile(targetAccountname, followerId);
  }

  @Transactional
  public ProfileDTO.ProfileResponse unfollowUser(String followerId, String targetAccountname) {
    // 현재 사용자 조회
    User follower =
        userRepository
            .findById(followerId)
            .orElseThrow(() -> new ResourceNotFoundException("현재 사용자를 찾을 수 없습니다."));

    // 언팔로우할 대상 사용자 조회
    User following =
        userRepository
            .findByAccountname(targetAccountname)
            .orElseThrow(() -> new ResourceNotFoundException("해당 계정이 존재하지 않습니다."));

    // 팔로우 관계 삭제
    FollowId followId = new FollowId(follower.getId(), following.getId());
    followRepository.findById(followId).ifPresent(followRepository::delete);

    // 프로필 조회
    return profileService.getProfile(targetAccountname, followerId);
  }

  @Transactional(readOnly = true)
  public List<ProfileDTO.Profile> getFollowerProfiles(
      String accountname, String currentUserId, Integer limit, Integer skip) {
    User user =
        userRepository
            .findByAccountname(accountname)
            .orElseThrow(() -> new ResourceNotFoundException("해당 계정이 존재하지 않습니다."));

    List<String> followerIds =
        followRepository.findByFollowingId(user.getId()).stream()
            .map(Follow::getFollowerId)
            .collect(Collectors.toList());

    if (followerIds.isEmpty()) {
      return new ArrayList<>();
    }

    // 페이징 처리
    if (skip != null && limit != null) {
      int fromIndex = Math.min(skip, followerIds.size());
      int toIndex = Math.min(fromIndex + limit, followerIds.size());
      followerIds = followerIds.subList(fromIndex, toIndex);
    }

    List<User> followers = userRepository.findAllById(followerIds);

    return followers.stream()
        .map(
            follower -> {
              // 각 팔로워에 대한 프로필 정보 구성
              boolean isFollowing =
                  followRepository.existsByFollowerIdAndFollowingId(
                      currentUserId, follower.getId());

              List<String> followerFollowers =
                  followRepository.findByFollowingId(follower.getId()).stream()
                      .map(Follow::getFollowerId)
                      .collect(Collectors.toList());

              List<String> followerFollowing =
                  followRepository.findByFollowerId(follower.getId()).stream()
                      .map(Follow::getFollowingId)
                      .collect(Collectors.toList());

              return ProfileDTO.Profile.fromEntity(
                  follower,
                  isFollowing,
                  followerFollowing,
                  followerFollowers,
                  followerFollowing.size(),
                  followerFollowers.size());
            })
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<ProfileDTO.Profile> getFollowingProfiles(
      String accountname, String currentUserId, Integer limit, Integer skip) {
    User user =
        userRepository
            .findByAccountname(accountname)
            .orElseThrow(() -> new ResourceNotFoundException("해당 계정이 존재하지 않습니다."));

    List<String> followingIds =
        followRepository.findByFollowerId(user.getId()).stream()
            .map(Follow::getFollowingId)
            .collect(Collectors.toList());

    if (followingIds.isEmpty()) {
      return new ArrayList<>();
    }

    // 페이징 처리
    if (skip != null && limit != null) {
      int fromIndex = Math.min(skip, followingIds.size());
      int toIndex = Math.min(fromIndex + limit, followingIds.size());
      followingIds = followingIds.subList(fromIndex, toIndex);
    }

    List<User> followings = userRepository.findAllById(followingIds);

    return followings.stream()
        .map(
            following -> {
              // 각 팔로잉에 대한 프로필 정보 구성
              boolean isFollowing =
                  followRepository.existsByFollowerIdAndFollowingId(
                      currentUserId, following.getId());

              List<String> followingFollowers =
                  followRepository.findByFollowingId(following.getId()).stream()
                      .map(Follow::getFollowerId)
                      .collect(Collectors.toList());

              List<String> followingFollowing =
                  followRepository.findByFollowerId(following.getId()).stream()
                      .map(Follow::getFollowingId)
                      .collect(Collectors.toList());

              return ProfileDTO.Profile.fromEntity(
                  following,
                  isFollowing,
                  followingFollowing,
                  followingFollowers,
                  followingFollowing.size(),
                  followingFollowers.size());
            })
        .collect(Collectors.toList());
  }
}
