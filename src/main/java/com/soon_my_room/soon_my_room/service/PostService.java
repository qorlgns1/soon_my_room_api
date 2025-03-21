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
    User currentUser = findUserByEmail(userEmail);

    // 게시글 생성
    Post post =
        Post.builder()
            .content(postContent.getContent())
            .image(postContent.getImage())
            .author(currentUser)
            .build();

    Post savedPost = postRepository.save(post);

    // 프로필 정보 구성
    ProfileDTO.Profile authorProfile = buildProfileInfo(currentUser, currentUser);

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
    User currentUser = findUserByEmail(userEmail);

    // 팔로잉 목록 조회
    List<String> followingIds = getFollowingIds(currentUser);

    if (followingIds.isEmpty()) {
      return PostDTO.PostListResponse.builder().posts(new ArrayList<>()).build();
    }

    // 팔로잉 사용자 목록 조회
    List<User> followingUsers = userRepository.findAllById(followingIds);

    // 페이징 설정
    Pageable pageable = createPageRequest(limit, skip);

    // 팔로잉 사용자들의 게시글 조회
    List<Post> feedPosts =
        postRepository.findByAuthorInOrderByCreatedAtDesc(followingUsers, pageable);

    // 게시글 상세 정보 구성
    List<PostDTO.PostDetail> postDetails = buildFeedPostDetails(feedPosts, currentUser);

    return PostDTO.PostListResponse.fromEntities(postDetails);
  }

  /** 사용자 게시글 목록 */
  @Transactional(readOnly = true)
  public PostDTO.PostResponse getUserPosts(
      String accountname, String currentUserEmail, Integer limit, Integer skip) {
    // 사용자 조회
    User targetUser = findUserByAccountname(accountname);
    User currentUser = findUserByEmail(currentUserEmail);

    // 페이징 처리된 사용자 게시글 조회
    List<Post> userPosts = getPagedUserPosts(targetUser, limit, skip);

    // 게시글이 없는 경우 빈 목록 반환
    if (userPosts.isEmpty()) {
      return PostDTO.PostResponse.builder().post(new ArrayList<>()).build();
    }

    // 작성자 프로필 정보 구성
    ProfileDTO.Profile authorProfile = buildProfileInfo(targetUser, currentUser);

    // 게시글 상세 정보 구성
    List<PostDTO.PostDetail> postDetails = buildPostDetails(userPosts, currentUser, authorProfile);

    return PostDTO.PostResponse.builder().post(postDetails).build();
  }

  /** 게시글 상세 조회 */
  @Transactional(readOnly = true)
  public PostDTO.PostResponse getPostDetail(String postId, String currentUserEmail) {
    // 게시글 조회
    Post post = findPostById(postId);

    // 현재 사용자 조회
    User currentUser = findUserByEmail(currentUserEmail);

    // 좋아요 여부
    boolean hearted = hasLiked(currentUser.getId(), post.getId());

    // 좋아요 수
    int heartCount = countLikes(post.getId());

    // 댓글 수
    int commentCount = countComments(post);

    // 작성자 프로필 정보
    User author = post.getAuthor();
    ProfileDTO.Profile authorProfile = buildProfileInfo(author, currentUser);

    return PostDTO.PostResponse.fromEntity(post, hearted, heartCount, commentCount, authorProfile);
  }

  /** 게시글 수정 */
  @Transactional
  public PostDTO.PostResponse updatePost(
      String postId, String currentUserEmail, PostDTO.PostRequest.PostContent postContent) {
    // 게시글 조회
    Post post = findPostById(postId);

    // 현재 사용자 조회
    User currentUser = findUserByEmail(currentUserEmail);

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
    boolean hearted = hasLiked(currentUser.getId(), updatedPost.getId());

    // 좋아요 수
    int heartCount = countLikes(updatedPost.getId());

    // 댓글 수
    int commentCount = countComments(updatedPost);

    // 작성자 프로필 정보
    User author = updatedPost.getAuthor();
    ProfileDTO.Profile authorProfile = buildProfileInfo(author, currentUser);

    return PostDTO.PostResponse.fromEntity(
        updatedPost, hearted, heartCount, commentCount, authorProfile);
  }

  /** 게시글 삭제 */
  @Transactional
  public void deletePost(String postId, String currentUserEmail) {
    // 게시글 조회
    Post post = findPostById(postId);

    // 현재 사용자 조회
    User currentUser = findUserByEmail(currentUserEmail);

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
    Post post = findPostById(postId);

    // 현재 사용자 조회 (신고자)
    findUserByEmail(currentUserEmail);

    // 실제 신고 로직은 요구사항에 구체적으로 명시되어 있지 않아
    // 단순히 신고된 게시글 ID를 반환하는 것으로 구현

    return PostDTO.ReportResponse.fromPostId(postId);
  }

  /** ID로 게시글 조회 */
  private Post findPostById(String postId) {
    return postRepository
        .findById(postId)
        .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 게시글입니다."));
  }

  /** 계정명으로 사용자 조회 */
  private User findUserByAccountname(String accountname) {
    return userRepository
        .findByAccountname(accountname)
        .orElseThrow(() -> new ResourceNotFoundException("해당 계정이 존재하지 않습니다."));
  }

  /** 이메일로 사용자 조회 */
  private User findUserByEmail(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
  }

  /** 페이징 요청 객체 생성 */
  private Pageable createPageRequest(Integer limit, Integer skip) {
    int pageSize = limit != null ? limit : 10;
    int pageNumber = skip != null ? skip / pageSize : 0;
    return PageRequest.of(pageNumber, pageSize);
  }

  /** 페이징 처리된 사용자 게시글 조회 */
  private List<Post> getPagedUserPosts(User user, Integer limit, Integer skip) {
    Pageable pageable = createPageRequest(limit, skip);
    return postRepository.findByAuthorOrderByCreatedAtDesc(user, pageable);
  }

  /** 프로필 정보 구성 */
  private ProfileDTO.Profile buildProfileInfo(User targetUser, User currentUser) {
    // 팔로워 목록 조회
    List<String> followerIds = getFollowerIds(targetUser);

    // 팔로잉 목록 조회
    List<String> followingIds = getFollowingIds(targetUser);

    // 팔로우 여부 확인
    boolean isFollowing = isFollowing(currentUser.getId(), targetUser.getId());

    return ProfileDTO.Profile.fromEntity(
        targetUser,
        isFollowing,
        followingIds,
        followerIds,
        followingIds.size(),
        followerIds.size());
  }

  /** 팔로워 ID 목록 조회 */
  private List<String> getFollowerIds(User user) {
    return followRepository.findByFollowingId(user.getId()).stream()
        .map(Follow::getFollowerId)
        .collect(Collectors.toList());
  }

  /** 팔로잉 ID 목록 조회 */
  private List<String> getFollowingIds(User user) {
    return followRepository.findByFollowerId(user.getId()).stream()
        .map(Follow::getFollowingId)
        .collect(Collectors.toList());
  }

  /** 팔로우 여부 확인 */
  private boolean isFollowing(String followerId, String followingId) {
    return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
  }

  /** 게시글 상세 정보 구성 */
  private List<PostDTO.PostDetail> buildPostDetails(
      List<Post> posts, User currentUser, ProfileDTO.Profile authorProfile) {
    return posts.stream()
        .map(post -> buildPostDetail(post, currentUser, authorProfile))
        .collect(Collectors.toList());
  }

  /** 피드 게시글 상세 정보 구성 */
  private List<PostDTO.PostDetail> buildFeedPostDetails(List<Post> posts, User currentUser) {
    return posts.stream()
        .map(
            post -> {
              // 각 게시글의 작성자 프로필 정보 구성
              ProfileDTO.Profile authorProfile = buildProfileInfo(post.getAuthor(), currentUser);
              return buildPostDetail(post, currentUser, authorProfile);
            })
        .collect(Collectors.toList());
  }

  /** 단일 게시글 상세 정보 구성 */
  private PostDTO.PostDetail buildPostDetail(
      Post post, User currentUser, ProfileDTO.Profile authorProfile) {
    // 좋아요 여부
    boolean hearted = hasLiked(currentUser.getId(), post.getId());

    // 좋아요 수
    int heartCount = countLikes(post.getId());

    // 댓글 수
    int commentCount = countComments(post);

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
  }

  /** 좋아요 여부 확인 */
  private boolean hasLiked(String userId, String postId) {
    return heartRepository.existsByUserIdAndPostId(userId, postId);
  }

  /** 좋아요 수 조회 */
  private int countLikes(String postId) {
    return heartRepository.countByPostId(postId);
  }

  /** 댓글 수 조회 */
  private int countComments(Post post) {
    return commentRepository.countByPost(post);
  }
}
