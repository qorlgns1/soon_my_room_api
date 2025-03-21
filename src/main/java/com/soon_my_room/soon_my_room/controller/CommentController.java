package com.soon_my_room.soon_my_room.controller;

import com.soon_my_room.soon_my_room.dto.CommentDTO;
import com.soon_my_room.soon_my_room.service.CommentService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
@Tag(name = "Comment", description = "댓글 관련 API")
public class CommentController {

  private final CommentService commentService;

  @Operation(
      summary = "댓글 작성",
      description = "특정 게시글에 댓글을 작성합니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "댓글 작성 성공"),
        @ApiResponse(responseCode = "400", description = "댓글을 입력하지 않음"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "게시글이 존재하지 않음")
      })
  @PostMapping("/{post_id}/comments")
  public ResponseEntity<?> createComment(
      @Parameter(description = "게시글 ID", required = true) @PathVariable("post_id") String postId,
      @Parameter(description = "댓글 정보", required = true) @Valid @RequestBody
          CommentDTO.CommentRequest requestDTO,
      Authentication authentication) {

    try {
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      String currentUserEmail = userDetails.getUsername();

      CommentDTO.CommentResponse response =
          commentService.createComment(
              postId, currentUserEmail, requestDTO.getComment().getContent());
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (com.soon_my_room.soon_my_room.exception.ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  @Operation(
      summary = "댓글 목록 조회",
      description = "특정 게시글의 댓글 목록을 조회합니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "게시글이 존재하지 않음")
      })
  @GetMapping("/{post_id}/comments")
  public ResponseEntity<?> getComments(
      @Parameter(description = "게시글 ID", required = true) @PathVariable("post_id") String postId,
      @Parameter(description = "페이지당 댓글 수") @RequestParam(required = false) Integer limit,
      @Parameter(description = "건너뛸 댓글 수") @RequestParam(required = false) Integer skip,
      Authentication authentication) {

    try {
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      String currentUserEmail = userDetails.getUsername();

      CommentDTO.CommentListResponse response =
          commentService.getComments(postId, currentUserEmail, limit, skip);
      return ResponseEntity.ok(response);
    } catch (com.soon_my_room.soon_my_room.exception.ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }
  }

  @Operation(
      summary = "댓글 삭제",
      description = "특정 게시글의 특정 댓글을 삭제합니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
        @ApiResponse(responseCode = "404", description = "게시글 또는 댓글이 존재하지 않음")
      })
  @DeleteMapping("/{post_id}/comments/{comment_id}")
  public ResponseEntity<?> deleteComment(
      @Parameter(description = "게시글 ID", required = true) @PathVariable("post_id") String postId,
      @Parameter(description = "댓글 ID", required = true) @PathVariable("comment_id")
          String commentId,
      Authentication authentication) {

    try {
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      String currentUserEmail = userDetails.getUsername();

      commentService.deleteComment(postId, commentId, currentUserEmail);
      return ResponseEntity.ok(Map.of("message", "댓글이 삭제되었습니다."));
    } catch (com.soon_my_room.soon_my_room.exception.ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    } catch (AccessDeniedException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
    }
  }

  @Operation(
      summary = "댓글 신고",
      description = "특정 게시글의 특정 댓글을 신고합니다.",
      security = {@SecurityRequirement(name = "bearerAuth")})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "신고 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "게시글 또는 댓글이 존재하지 않음")
      })
  @PostMapping("/{post_id}/comments/{comment_id}/report")
  public ResponseEntity<?> reportComment(
      @Parameter(description = "게시글 ID", required = true) @PathVariable("post_id") String postId,
      @Parameter(description = "댓글 ID", required = true) @PathVariable("comment_id")
          String commentId,
      Authentication authentication) {

    try {
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      String currentUserEmail = userDetails.getUsername();

      CommentDTO.ReportResponse response =
          commentService.reportComment(postId, commentId, currentUserEmail);
      return ResponseEntity.ok(response);
    } catch (com.soon_my_room.soon_my_room.exception.ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }
  }
}
