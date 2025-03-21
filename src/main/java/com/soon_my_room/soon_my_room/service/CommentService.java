package com.soon_my_room.soon_my_room.service;

import com.soon_my_room.soon_my_room.dto.CommentDTO;
import com.soon_my_room.soon_my_room.dto.ProfileDTO;
import com.soon_my_room.soon_my_room.exception.ResourceNotFoundException;
import com.soon_my_room.soon_my_room.model.Comment;
import com.soon_my_room.soon_my_room.model.Follow;
import com.soon_my_room.soon_my_room.model.Post;
import com.soon_my_room.soon_my_room.model.User;
import com.soon_my_room.soon_my_room.repository.CommentRepository;
import com.soon_my_room.soon_my_room.repository.FollowRepository;
import com.soon_my_room.soon_my_room.repository.PostRepository;
import com.soon_my_room.soon_my_room.repository.UserRepository;
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
public class CommentService {

  private final CommentRepository commentRepository;
  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final FollowRepository followRepository;

  /** 댓글 작성 */
  @Transactional
  public CommentDTO.CommentResponse createComment(
      String postId, String currentUserEmail, String content) {
    // 유효성 검사
    if (content == null || content.trim().isEmpty()) {
      throw new IllegalArgumentException("댓글을 입력해주세요.");
    }

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

    // 댓글 생성
    Comment comment = Comment.builder().post(post).author(currentUser).content(content).build();

    Comment savedComment = commentRepository.save(comment);

    // 작성자 프로필 정보
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
            false, // 자신이 작성한 댓글이므로 팔로우 상태는 false
            followingIds,
            followerIds,
            followingIds.size(),
            followerIds.size());

    return CommentDTO.CommentResponse.fromEntity(savedComment, authorProfile);
  }

  /** 댓글 목록 조회 */
  @Transactional(readOnly = true)
  public CommentDTO.CommentListResponse getComments(
      String postId, String currentUserEmail, Integer limit, Integer skip) {
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

    // 페이징 설정
    int pageSize = limit != null ? limit : 10;
    int pageNumber = skip != null ? skip / pageSize : 0;
    Pageable pageable = PageRequest.of(pageNumber, pageSize);

    // 댓글 목록 조회
    List<Comment> comments = commentRepository.findByPostOrderByCreatedAtDesc(post, pageable);

    // 댓글 상세 정보 구성
    List<CommentDTO.CommentDetail> commentDetails =
        comments.stream()
            .map(
                comment -> {
                  // 댓글 작성자 프로필 정보
                  User author = comment.getAuthor();
                  List<String> followerIds =
                      followRepository.findByFollowingId(author.getId()).stream()
                          .map(Follow::getFollowerId)
                          .collect(Collectors.toList());

                  List<String> followingIds =
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
                          followingIds,
                          followerIds,
                          followingIds.size(),
                          followerIds.size());

                  return CommentDTO.CommentDetail.builder()
                      .id(comment.getId())
                      .content(comment.getContent())
                      .createdAt(comment.getCreatedAt())
                      .author(authorProfile)
                      .build();
                })
            .collect(Collectors.toList());

    return CommentDTO.CommentListResponse.builder().comment(commentDetails).build();
  }

  /** 댓글 삭제 */
  @Transactional
  public void deleteComment(String postId, String commentId, String currentUserEmail) {
    // 게시글 조회
    Post post =
        postRepository
            .findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 게시글입니다."));

    // 댓글 조회
    Comment comment =
        commentRepository
            .findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("댓글이 존재하지 않습니다."));

    // 현재 사용자 조회
    User currentUser =
        userRepository
            .findByEmail(currentUserEmail)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

    // 댓글 작성자 확인
    if (!comment.getAuthor().getId().equals(currentUser.getId())) {
      throw new AccessDeniedException("댓글 작성자만 댓글을 삭제할 수 있습니다.");
    }

    // 댓글 삭제
    commentRepository.delete(comment);
  }

  /** 댓글 신고 */
  @Transactional
  public CommentDTO.ReportResponse reportComment(
      String postId, String commentId, String currentUserEmail) {
    // 게시글 존재 확인
    postRepository
        .findById(postId)
        .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 게시글입니다."));

    // 댓글 존재 확인
    commentRepository
        .findById(commentId)
        .orElseThrow(() -> new ResourceNotFoundException("댓글이 존재하지 않습니다."));

    // 현재 사용자 조회 (신고자)
    userRepository
        .findByEmail(currentUserEmail)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

    // 실제 신고 로직은 요구사항에 구체적으로 명시되어 있지 않아
    // 단순히 신고된 댓글 ID를 반환하는 것으로 구현
    // 향후 신고 테이블 추가 등의 확장 가능

    return CommentDTO.ReportResponse.fromCommentId(commentId);
  }
}
