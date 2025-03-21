package com.soon_my_room.soon_my_room.service;

import com.soon_my_room.soon_my_room.dto.PostDTO;
import com.soon_my_room.soon_my_room.dto.ProfileDTO;
import com.soon_my_room.soon_my_room.exception.ResourceNotFoundException;
import com.soon_my_room.soon_my_room.model.Follow;
import com.soon_my_room.soon_my_room.model.Heart;
import com.soon_my_room.soon_my_room.model.HeartId;
import com.soon_my_room.soon_my_room.model.Post;
import com.soon_my_room.soon_my_room.model.User;
import com.soon_my_room.soon_my_room.repository.CommentRepository;
import com.soon_my_room.soon_my_room.repository.FollowRepository;
import com.soon_my_room.soon_my_room.repository.HeartRepository;
import com.soon_my_room.soon_my_room.repository.PostRepository;
import com.soon_my_room.soon_my_room.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HeartService {

  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final HeartRepository heartRepository;
  private final CommentRepository commentRepository;
  private final FollowRepository followRepository;

  /** 게시글 좋아요 */
  @Transactional
  public PostDTO.PostResponse addHeart(String postId, String currentUserEmail) {
    // 게시글 조회
    Post post =
        postRepository
            .findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 게시글입니다."));

    // 현재 사용자 조회
    User currentUser =
        userRepository
            .findByEmail(currentUserEmail)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

    // 이미 좋아요를 눌렀는지 확인
    boolean alreadyHearted = heartRepository.existsByUserIdAndPostId(currentUser.getId(), postId);

    // 아직 좋아요를 누르지 않았다면 추가
    if (!alreadyHearted) {
      Heart heart =
          Heart.builder()
              .userId(currentUser.getId())
              .postId(postId)
              .user(currentUser)
              .post(post)
              .createdAt(LocalDateTime.now())
              .build();

      heartRepository.save(heart);
    }

    // 게시글 작성자 프로필 정보
    User author = post.getAuthor();
    List<String> followerIds =
        followRepository.findByFollowingId(author.getId()).stream()
            .map(Follow::getFollowerId)
            .collect(Collectors.toList());

    List<String> followingIds =
        followRepository.findByFollowerId(author.getId()).stream()
            .map(Follow::getFollowingId)
            .collect(Collectors.toList());

    boolean isFollowing =
        followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), author.getId());

    ProfileDTO.Profile authorProfile =
        ProfileDTO.Profile.fromEntity(
            author,
            isFollowing,
            followingIds,
            followerIds,
            followingIds.size(),
            followerIds.size());

    // 좋아요 정보 업데이트
    boolean hearted = true; // 이제 좋아요를 누른 상태
    int heartCount = heartRepository.countByPostId(postId);
    int commentCount = commentRepository.countByPost(post);

    return PostDTO.PostResponse.fromEntity(post, hearted, heartCount, commentCount, authorProfile);
  }

  /** 게시글 좋아요 취소 */
  @Transactional
  public PostDTO.PostResponse removeHeart(String postId, String currentUserEmail) {
    // 게시글 조회
    Post post =
        postRepository
            .findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 게시글입니다."));

    // 현재 사용자 조회
    User currentUser =
        userRepository
            .findByEmail(currentUserEmail)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

    // 좋아요 조회 및 삭제
    HeartId heartId = new HeartId(currentUser.getId(), postId);
    heartRepository.findById(heartId).ifPresent(heartRepository::delete);

    // 게시글 작성자 프로필 정보
    User author = post.getAuthor();
    List<String> followerIds =
        followRepository.findByFollowingId(author.getId()).stream()
            .map(Follow::getFollowerId)
            .collect(Collectors.toList());

    List<String> followingIds =
        followRepository.findByFollowerId(author.getId()).stream()
            .map(Follow::getFollowingId)
            .collect(Collectors.toList());

    boolean isFollowing =
        followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), author.getId());

    ProfileDTO.Profile authorProfile =
        ProfileDTO.Profile.fromEntity(
            author,
            isFollowing,
            followingIds,
            followerIds,
            followingIds.size(),
            followerIds.size());

    // 좋아요 정보 업데이트
    boolean hearted = false; // 이제 좋아요가 취소된 상태
    int heartCount = heartRepository.countByPostId(postId);
    int commentCount = commentRepository.countByPost(post);

    return PostDTO.PostResponse.fromEntity(post, hearted, heartCount, commentCount, authorProfile);
  }
}
