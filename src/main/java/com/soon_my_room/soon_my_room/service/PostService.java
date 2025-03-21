package com.soon_my_room.soon_my_room.service;

import com.soon_my_room.soon_my_room.dto.PostDTO;
import com.soon_my_room.soon_my_room.dto.ProfileDTO;
import com.soon_my_room.soon_my_room.exception.ResourceNotFoundException;
import com.soon_my_room.soon_my_room.model.Follow;
import com.soon_my_room.soon_my_room.model.Heart;
import com.soon_my_room.soon_my_room.model.Post;
import com.soon_my_room.soon_my_room.model.User;
import com.soon_my_room.soon_my_room.repository.CommentRepository;
import com.soon_my_room.soon_my_room.repository.FollowRepository;
import com.soon_my_room.soon_my_room.repository.HeartRepository;
import com.soon_my_room.soon_my_room.repository.PostRepository;
import com.soon_my_room.soon_my_room.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final HeartRepository heartRepository;
  private final CommentRepository commentRepository;
  private final FollowRepository followRepository;

  /** 게시글 작성 */
  @Transactional
  public PostDTO.PostResponse createPost(
      String userEmail, PostDTO.PostRequest.PostContent postContent) {
    // 유효성 검사
    if ((postContent.getContent() == null || postContent.getContent().trim().isEmpty())
        && (postContent.getImage() == null || postContent.getImage().trim().isEmpty())) {
      throw new IllegalArgumentException("내용 또는 이미지를 입력해주세요.");
    }

    // 현재 사용자 조회
    User currentUser =
        userRepository
            .findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

    // 게시글 생성
    Post post =
        Post.builder()
            .content(postContent.getContent())
            .image(postContent.getImage())
            .author(currentUser)
            .build();

    Post savedPost = postRepository.save(post);

    // 프로필 정보 구성
    List<String> followerIds =
        followRepository.findByFollowingId(currentUser.getId()).stream()
            .map(Follow::getFollowerId)
            .collect(Collectors.toList());

    List<String> followingIds =
        followRepository.findByFollowerId(currentUser.getId()).stream()
            .map(Follow::getFollowingId)
            .collect(Collectors.toList());

    ProfileDTO.Profile authorProfile =
        ProfileDTO.Profile.fromEntity(
            currentUser,
            false, // 자신의 게시글이므로 팔로우 상태는 false
            followingIds,
            followerIds,
            followingIds.size(),
            followerIds.size());

    // 응답 생성
    return PostDTO.PostResponse.fromEntity(
        savedPost,
        false, // 새 게시글이므로 좋아요 여부는 false
        0, // 새 게시글이므로 좋아요 수는 0
        0, // 새 게시글이므로 댓글 수는 0
        authorProfile);
  }

  /** 팔로잉 게시글 목록 (피드) */
  @Transactional(readOnly = true)
  public PostDTO.PostListResponse getFeedPosts(String userEmail, Integer limit, Integer skip) {
    // 현재 사용자 조회
    User currentUser =
        userRepository
            .findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

    // 팔로잉 목록 조회
    List<String> followingIds =
        followRepository.findByFollowerId(currentUser.getId()).stream()
            .map(Follow::getFollowingId)
            .collect(Collectors.toList());

    if (followingIds.isEmpty()) {
      return PostDTO.PostListResponse.builder().posts(new ArrayList<>()).build();
    }

    // 팔로잉 사용자 목록 조회
    List<User> followingUsers = userRepository.findAllById(followingIds);

    // 페이징 설정
    int pageSize = limit != null ? limit : 10;
    int pageNumber = skip != null ? skip / pageSize : 0;
    Pageable pageable = PageRequest.of(pageNumber, pageSize);

    // 팔로잉 사용자들의 게시글 조회
    List<Post> feedPosts =
        postRepository.findByAuthorInOrderByCreatedAtDesc(followingUsers, pageable);

    // 게시글 상세 정보 구성
    List<PostDTO.PostDetail> postDetails =
        feedPosts.stream()
            .map(
                post -> {
                  // 좋아요 여부
                  boolean hearted =
                      heartRepository.existsByUserIdAndPostId(currentUser.getId(), post.getId());
                  // 좋아요 수
                  int heartCount = heartRepository.countByPostId(post.getId());
                  // 댓글 수
                  int commentCount = commentRepository.countByPost(post);

                  // 작성자 프로필 정보
                  User author = post.getAuthor();
                  List<String> authorFollowerIds =
                      followRepository.findByFollowingId(author.getId()).stream()
                          .map(Follow::getFollowerId)
                          .collect(Collectors.toList());

                  List<String> authorFollowingIds =
                      followRepository.findByFollowerId(author.getId()).stream()
                          .map(Follow::getFollowingId)
                          .collect(Collectors.toList());

                  boolean isFollowing =
                      followRepository.existsByFollowerIdAndFollowingId(
                          currentUser.getId(), author.getId());

                  ProfileDTO.Profile authorProfile =
                      ProfileDTO.Profile.fromEntity(
                          author,
                          isFollowing,
                          authorFollowingIds,
                          authorFollowerIds,
                          authorFollowingIds.size(),
                          authorFollowerIds.size());

                  return PostDTO.PostDetail.builder()
                      .id(post.getId())
                      .content(post.getContent())
                      .image(post.getImage())
                      .createdAt(post.getCreatedAt())
                      .updatedAt(post.getUpdatedAt())
                      .hearted(hearted)
                      .heartCount(heartCount)
                      .commentCount(commentCount)
                      .author(authorProfile)
                      .build();
                })
            .collect(Collectors.toList());

    return PostDTO.PostListResponse.fromEntities(postDetails);
  }

  /** 사용자 게시글 목록 */
  @Transactional(readOnly = true)
  public PostDTO.PostResponse getUserPosts(
      String accountname, String currentUserEmail, Integer limit, Integer skip) {
    // 계정 소유자 조회
    User targetUser =
        userRepository
            .findByAccountname(accountname)
            .orElseThrow(() -> new ResourceNotFoundException("해당 계정이 존재하지 않습니다."));

    // 현재 사용자 조회
    User currentUser =
        userRepository
            .findByEmail(currentUserEmail)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

    // 페이징 설정
    int pageSize = limit != null ? limit : 10;
    int pageNumber = skip != null ? skip / pageSize : 0;
    Pageable pageable = PageRequest.of(pageNumber, pageSize);

    // 사용자 게시글 조회
    List<Post> userPosts = postRepository.findByAuthorOrderByCreatedAtDesc(targetUser, pageable);

    // 게시글이 없는 경우 빈 목록 반환
    if (userPosts.isEmpty()) {
      return PostDTO.PostResponse.builder().post(new ArrayList<>()).build();
    }

    // 프로필 정보 구성
    List<String> followerIds =
        followRepository.findByFollowingId(targetUser.getId()).stream()
            .map(Follow::getFollowerId)
            .collect(Collectors.toList());

    List<String> followingIds =
        followRepository.findByFollowerId(targetUser.getId()).stream()
            .map(Follow::getFollowingId)
            .collect(Collectors.toList());

    boolean isFollowing =
        followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), targetUser.getId());

    ProfileDTO.Profile authorProfile =
        ProfileDTO.Profile.fromEntity(
            targetUser,
            isFollowing,
            followingIds,
            followerIds,
            followingIds.size(),
            followerIds.size());

    // 게시글 상세 정보 구성
    List<PostDTO.PostDetail> postDetails =
        userPosts.stream()
            .map(
                post -> {
                  // 좋아요 여부
                  boolean hearted =
                      heartRepository.existsByUserIdAndPostId(currentUser.getId(), post.getId());
                  // 좋아요 수
                  int heartCount = heartRepository.countByPostId(post.getId());
                  // 댓글 수
                  int commentCount = commentRepository.countByPost(post);

                  return PostDTO.PostDetail.builder()
                      .id(post.getId())
                      .content(post.getContent())
                      .image(post.getImage())
                      .createdAt(post.getCreatedAt())
                      .updatedAt(post.getUpdatedAt())
                      .hearted(hearted)
                      .heartCount(heartCount)
                      .commentCount(commentCount)
                      .author(authorProfile)
                      .build();
                })
            .collect(Collectors.toList());

    return PostDTO.PostResponse.builder().post(postDetails).build();
  }

  /** 게시글 상세 조회 */
  @Transactional(readOnly = true)
  public PostDTO.PostResponse getPostDetail(String postId, String currentUserEmail) {
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

    // 좋아요 여부
    boolean hearted = heartRepository.existsByUserIdAndPostId(currentUser.getId(), post.getId());
    // 좋아요 수
    int heartCount = heartRepository.countByPostId(post.getId());
    // 댓글 수
    int commentCount = commentRepository.countByPost(post);

    // 작성자 프로필 정보
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

    return PostDTO.PostResponse.fromEntity(post, hearted, heartCount, commentCount, authorProfile);
  }

  /** 게시글 수정 */
  @Transactional
  public PostDTO.PostResponse updatePost(
      String postId, String currentUserEmail, PostDTO.PostRequest.PostContent postContent) {
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

    // 게시글 작성자 확인
    if (!post.getAuthor().getId().equals(currentUser.getId())) {
      throw new AccessDeniedException("잘못된 요청입니다. 로그인 정보를 확인하세요.");
    }

    // 유효성 검사
    if ((postContent.getContent() == null || postContent.getContent().trim().isEmpty())
        && (postContent.getImage() == null || postContent.getImage().trim().isEmpty())) {
      throw new IllegalArgumentException("내용 또는 이미지를 입력해주세요.");
    }

    // 게시글 수정
    post.setContent(postContent.getContent());
    post.setImage(postContent.getImage());

    Post updatedPost = postRepository.save(post);

    // 좋아요 여부
    boolean hearted =
        heartRepository.existsByUserIdAndPostId(currentUser.getId(), updatedPost.getId());
    // 좋아요 수
    int heartCount = heartRepository.countByPostId(updatedPost.getId());
    // 댓글 수
    int commentCount = commentRepository.countByPost(updatedPost);

    // 작성자 프로필 정보
    User author = updatedPost.getAuthor();
    List<String> followerIds =
        followRepository.findByFollowingId(author.getId()).stream()
            .map(Follow::getFollowerId)
            .collect(Collectors.toList());

    List<String> followingIds =
        followRepository.findByFollowerId(author.getId()).stream()
            .map(Follow::getFollowingId)
            .collect(Collectors.toList());

    ProfileDTO.Profile authorProfile =
        ProfileDTO.Profile.fromEntity(
            author,
            false, // 본인 게시글
            followingIds,
            followerIds,
            followingIds.size(),
            followerIds.size());

    return PostDTO.PostResponse.fromEntity(
        updatedPost, hearted, heartCount, commentCount, authorProfile);
  }

  /** 게시글 삭제 */
  @Transactional
  public void deletePost(String postId, String currentUserEmail) {
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

    // 게시글 작성자 확인
    if (!post.getAuthor().getId().equals(currentUser.getId())) {
      throw new AccessDeniedException("잘못된 요청입니다. 로그인 정보를 확인하세요.");
    }

    // 게시글 좋아요 삭제
    List<Heart> hearts = heartRepository.findByPostId(post.getId());
    heartRepository.deleteAll(hearts);

    // 게시글 삭제
    postRepository.delete(post);
  }

  /** 게시글 신고 */
  @Transactional
  public PostDTO.ReportResponse reportPost(String postId, String currentUserEmail) {
    // 게시글 존재 확인
    Post post =
        postRepository
            .findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 게시글입니다."));

    // 현재 사용자 조회 (신고자)
    userRepository
        .findByEmail(currentUserEmail)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

    // 실제 신고 로직은 요구사항에 구체적으로 명시되어 있지 않아
    // 단순히 신고된 게시글 ID를 반환하는 것으로 구현
    // 향후 신고 테이블 추가 등의 확장 가능

    return PostDTO.ReportResponse.fromPostId(postId);
  }
}
