package com.soon_my_room.soon_my_room.controller;

import com.soon_my_room.soon_my_room.dto.PostDTO;
import com.soon_my_room.soon_my_room.repository.UserRepository;
import com.soon_my_room.soon_my_room.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
@Tag(name = "Post", description = "게시글 관련 API")
public class PostController {

  private final PostService postService;
  private final UserRepository userRepository;

  @Operation(
      summary = "게시글 작성",
      description = "새로운 게시글을 작성합니다. 내용과 이미지 중 하나는 반드시 포함되어야 합니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "게시글 작성 성공"),
        @ApiResponse(responseCode = "400", description = "내용 또는 이미지를 입력하지 않음"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
      })
  @PostMapping
  public ResponseEntity<?> createPost(
      @Parameter(description = "게시글 정보", required = true) @Valid @RequestBody
          PostDTO.PostRequest requestDTO,
      Authentication authentication) {

    try {
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      String currentUserEmail = userDetails.getUsername();

      PostDTO.PostResponse response =
          postService.createPost(currentUserEmail, requestDTO.getPost());
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  @Operation(
      summary = "팔로잉 게시글 목록(피드)",
      description = "내가 팔로우하고 있는 유저의 게시글 목록을 조회합니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
      })
  @GetMapping("/feed")
  public ResponseEntity<?> getFeedPosts(
      @Parameter(description = "페이지당 게시글 수") @RequestParam(required = false) Integer limit,
      @Parameter(description = "건너뛸 게시글 수") @RequestParam(required = false) Integer skip,
      Authentication authentication) {

    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    String currentUserEmail = userDetails.getUsername();

    PostDTO.PostListResponse response = postService.getFeedPosts(currentUserEmail, limit, skip);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "사용자 게시글 목록",
      description = "특정 사용자의 게시글 목록을 조회합니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "계정이 존재하지 않음")
      })
  @GetMapping("/{accountname}/userpost")
  public ResponseEntity<?> getUserPosts(
      @PathVariable String accountname,
      @Parameter(description = "페이지당 게시글 수") @RequestParam(required = false) Integer limit,
      @Parameter(description = "건너뛸 게시글 수") @RequestParam(required = false) Integer skip,
      Authentication authentication) {
    System.out.println("[시작]");
    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    String currentUserEmail = userDetails.getUsername();

    PostDTO.PostResponse response =
        postService.getUserPosts(accountname, currentUserEmail, limit, skip);

    System.out.println("[종료]");
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "게시글 상세 조회",
      description = "특정 게시글의 상세 정보를 조회합니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "게시글이 존재하지 않음")
      })
  @GetMapping("/{post_id}")
  public ResponseEntity<?> getPostDetail(
      @Parameter(description = "게시글 ID", required = true) @PathVariable("post_id") String postId,
      Authentication authentication) {

    try {
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      String currentUserEmail = userDetails.getUsername();

      PostDTO.PostResponse response = postService.getPostDetail(postId, currentUserEmail);
      return ResponseEntity.ok(response);
    } catch (com.soon_my_room.soon_my_room.exception.ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }
  }

  @Operation(
      summary = "게시글 수정",
      description = "특정 게시글을 수정합니다. 내용과 이미지 중 하나는 반드시 포함되어야 합니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(responseCode = "400", description = "내용 또는 이미지를 입력하지 않음"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "수정 권한 없음"),
        @ApiResponse(responseCode = "404", description = "게시글이 존재하지 않음")
      })
  @PutMapping("/{post_id}")
  public ResponseEntity<?> updatePost(
      @Parameter(description = "게시글 ID", required = true) @PathVariable("post_id") String postId,
      @Parameter(description = "게시글 정보", required = true) @Valid @RequestBody
          PostDTO.PostRequest requestDTO,
      Authentication authentication) {

    try {
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      String currentUserEmail = userDetails.getUsername();

      PostDTO.PostResponse response =
          postService.updatePost(postId, currentUserEmail, requestDTO.getPost());
      return ResponseEntity.ok(response);
    } catch (com.soon_my_room.soon_my_room.exception.ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    } catch (AccessDeniedException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  @Operation(
      summary = "게시글 삭제",
      description = "특정 게시글을 삭제합니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
        @ApiResponse(responseCode = "404", description = "게시글이 존재하지 않음")
      })
  @DeleteMapping("/{post_id}")
  public ResponseEntity<?> deletePost(
      @Parameter(description = "게시글 ID", required = true) @PathVariable("post_id") String postId,
      Authentication authentication) {

    try {
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      String currentUserEmail = userDetails.getUsername();

      postService.deletePost(postId, currentUserEmail);
      return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    } catch (com.soon_my_room.soon_my_room.exception.ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    } catch (AccessDeniedException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
    }
  }

  @Operation(
      summary = "게시글 신고",
      description = "특정 게시글을 신고합니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "신고 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "게시글이 존재하지 않음")
      })
  @PostMapping("/{post_id}/report")
  public ResponseEntity<?> reportPost(
      @Parameter(description = "게시글 ID", required = true) @PathVariable("post_id") String postId,
      Authentication authentication) {

    try {
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      String currentUserEmail = userDetails.getUsername();

      PostDTO.ReportResponse response = postService.reportPost(postId, currentUserEmail);
      return ResponseEntity.ok(response);
    } catch (com.soon_my_room.soon_my_room.exception.ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }
  }
}
